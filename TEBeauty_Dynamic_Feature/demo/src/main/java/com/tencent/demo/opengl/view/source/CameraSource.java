package com.tencent.demo.opengl.view.source;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.google.common.util.concurrent.ListenableFuture;
import com.tencent.demo.opengl.GlUtil;
import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.render.TextureFormat;
import com.tencent.demo.opengl.view.CameraXLifecycleOwner;
import com.tencent.demo.opengl.view.FrameTexture;
import com.tencent.demo.opengl.view.GLCameraXView;
import com.tencent.demo.utils.LogUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CameraSource implements Source, Preview.SurfaceProvider, GLThread.GLThreadEventListener {

    private static final String TAG = CameraSource.class.getName();
    private ProcessCameraProvider cameraProvider = null;
    private Preview preview = null;
    private CameraXLifecycleOwner cameraxLifecycle = null;
    private SourceEventListener sourceEventListener = null;
    private Size cameraInputSize = null;
    private Size cameraOutputSize = null;
    private Context context = null;

    private boolean isFrontCamera = true;

    private GLThread glThread = null;

    private int mCameraOesTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private SurfaceRequest mSurfaceRequest = null;

    private volatile CameraState mCameraState = CameraState.NOT_SWITCHING;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = null;
    private GLCameraXView.GLCameraXViewErrorListener cameraErrorListener = null;

    public CameraSource(Context context) {
        this.context = context;
        this.glThread = new GLThread("camera_gl_thread", this);
        cameraxLifecycle = new CameraXLifecycleOwner();
        cameraxLifecycle.doOnCreate();
    }


    public void setCameraSize(Size size) {
        this.cameraInputSize = size;
    }


    @Override
    public void initGlContext() {
        this.glThread.start();
    }


    @Override
    public void start() {
        cameraxLifecycle.doOnStart();
        cameraxLifecycle.doOnResume();
        mainHandler.post(() -> initCameraWhenCreated(isFrontCamera));
    }


    public void reStart() {
        mainHandler.post(() -> initCameraWhenCreated(isFrontCamera));
    }


    @Override
    public void setSourceEventListener(SourceEventListener sourceEventListener) {
        this.sourceEventListener = sourceEventListener;
    }

    @Override
    public void surfaceRecreate() {
        this.glThread.surfaceRecreate();
    }


    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener) {
        this.onFrameAvailableListener = onFrameAvailableListener;
    }


    public void setCameraErrorListener(GLCameraXView.GLCameraXViewErrorListener cameraErrorListener) {
        this.cameraErrorListener = cameraErrorListener;
    }


    @Override
    public void pause() {
        cameraxLifecycle.doOnPause();
        cameraxLifecycle.doOnStop();
    }


    @Override
    public void release() {
        this.glThread.release();
        cameraxLifecycle.doOnDestroy();
    }



    public void switchCamera() {
        if (this.mCameraState != CameraState.SWITCHING) {
            this.mCameraState = CameraState.SWITCHING;
            this.setupCamera(!this.isFrontCamera);
        }
    }


    public void setFrontCamera(boolean frontCamera) {
        this.isFrontCamera = frontCamera;
    }

    public boolean isFrontCamera() {
        return this.isFrontCamera;
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void initCameraWhenCreated(boolean isFrontCamera) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context.getApplicationContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            Preview.Builder builder = new Preview.Builder();
            if (this.cameraInputSize != null) {
                builder.setTargetResolution(this.cameraInputSize);
            } else {
                builder.setTargetAspectRatio(AspectRatio.RATIO_16_9);
            }

            Camera2Interop.Extender ext = new Camera2Interop.Extender<>(builder);
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(30, 30));
            preview = builder.build();
            preview.setSurfaceProvider(CameraSource.this);
            setupCamera(isFrontCamera);
        }, ContextCompat.getMainExecutor(context.getApplicationContext()));
    }


    @SuppressLint("RestrictedApi")
    private void setupCamera(boolean isFrontCamera) {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(isFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                    .build();
            boolean hasCamera = false;
            try {
                hasCamera = cameraProvider.hasCamera(cameraSelector);
            } catch (CameraInfoUnavailableException e) {
                e.printStackTrace();
            }
            if (hasCamera) {
                if (cameraxLifecycle.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                    return;
                }
                cameraProvider.bindToLifecycle(
                        cameraxLifecycle,
                        cameraSelector,
                        preview
                );
            } else {
                LogUtils.d(TAG, "camera unavailable ");
                if (this.cameraErrorListener != null) {
                    this.cameraErrorListener.onError();
                }
            }
        }
    }




    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        mSurfaceRequest = request;
        //send create SurfaceTexture event
        glThread.createSurfaceTexture();
    }


    @SuppressLint("RestrictedApi")
    private void setupSurfaceTexture(SurfaceTexture surfaceTexture, SurfaceRequest request) {
        if (surfaceTexture == null || request == null) {
            return;
        }
        cameraOutputSize = request.getResolution();
        request.setTransformationInfoListener(Executors.newSingleThreadExecutor(), transformationInfo -> {

                }
        );
        surfaceTexture.setDefaultBufferSize(cameraOutputSize.getWidth(), cameraOutputSize.getHeight());
        Surface surface = new Surface(surfaceTexture);
        request.provideSurface(surface, Executors.newSingleThreadExecutor(), result -> {
            if (result.getResultCode() == SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY) {
                result.getSurface().release();
            }
        });
        this.isFrontCamera = request.getCamera().getCameraInfo().getLensFacing() == CameraSelector.LENS_FACING_FRONT;
        this.mCameraState = CameraState.NOT_SWITCHING;
    }


    @Override
    public void onGLContextCreated(EglCore eglCore) {
        mCameraOesTextureId = GlUtil.createOESTexture();
        if (this.sourceEventListener != null) {
            this.sourceEventListener.onGLContextCreated(eglCore);
        }
    }

    @Override
    public void onReceiveEvent(int event, EglCore eglCore, Object parameter) {
        if (event == GLThread.GLThreadEvent.PROCESS.value) {
            if (mSurfaceTexture == null) {
                return;
            }
            mSurfaceTexture.updateTexImage();
            FrameTexture frameTexture = new FrameTexture();
            frameTexture.isFrontCamera = (boolean) parameter;
            frameTexture.textureFormat = TextureFormat.Texture_OES;
            frameTexture.textureId = mCameraOesTextureId;
            frameTexture.textureWidth = cameraOutputSize.getWidth();
            frameTexture.textureHeight = cameraOutputSize.getHeight();
            if (this.sourceEventListener != null && mCameraState != CameraState.SWITCHING) {
                this.sourceEventListener.onReceiveFrameTexture(eglCore, frameTexture);
            }
        } else if (event == GLThread.GLThreadEvent.SURFACE_RECREATED.value) {
            if (this.sourceEventListener != null) {
                this.sourceEventListener.onSurfaceRecreated(eglCore);
            }
        } else if (event == GLThread.GLThreadEvent.CREATED_SURFACE_TEXTURE.value) {
            this.createSurfaceTexture();
            this.setupSurfaceTexture(mSurfaceTexture, mSurfaceRequest);
        }
    }


    @Override
    public void onGLContextDestroy() {
        this.releaseSurfaceTexture();
        if (this.sourceEventListener != null) {
            this.sourceEventListener.onGLContextDestroy();
        }
    }

    /**
     * createSurfaceTexture
     */
    private void createSurfaceTexture() {
        releaseSurfaceTexture();
        mSurfaceTexture = new SurfaceTexture(mCameraOesTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(surfaceTexture -> {
            glThread.process(isFrontCamera);
            if (this.onFrameAvailableListener != null) {
                this.onFrameAvailableListener.onFrameAvailable(surfaceTexture);
            }
        });
    }


    /**
     * releaseSurfaceTexture
     */
    private void releaseSurfaceTexture() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }



    public enum CameraState {
        SWITCHING,
        NOT_SWITCHING;
    }


}
