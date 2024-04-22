package com.tencent.demo.camera.camerax;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CaptureRequest;
import android.util.AttributeSet;
import android.util.Range;
import android.util.Size;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.Preview.Builder;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.google.common.util.concurrent.ListenableFuture;
import com.tencent.demo.R;
import com.tencent.demo.camera.glthread.TGLSurfaceProvider;
import com.tencent.effect.beautykit.utils.LogUtils;

import java.util.concurrent.ExecutionException;


public class GLCameraXView extends FrameLayout {

    private static final String TAG = GLCameraXView.class.getName();
    private TextureView textureView = null;
    private SurfaceView surfaceView = null;

    private TGLSurfaceProvider surfaceProvider = null;
    private CameraXLifecycleOwner cameraxLifecycle = null;

    private ProcessCameraProvider cameraProvider = null;
    private Preview preview = null;

    private boolean isSurfaceView = false;
    private boolean isBackCamera = true;
    private boolean isTransparent = false;

    private Size size = null;
    private GLCameraXViewErrorListener glCameraXViewErrorListener = null;

    public GLCameraXView(@NonNull Context context) {
        this(context, null);
    }

    public GLCameraXView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GLCameraXView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            getParameter(context, attrs);
        }
        this.init(context);
    }

    private void getParameter(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.te_beauty_GLCameraView);
        isBackCamera = typedArray.getBoolean(R.styleable.te_beauty_GLCameraView_back_camera, true);
        isSurfaceView = typedArray.getBoolean(R.styleable.te_beauty_GLCameraView_surface_view, true);
        isTransparent = typedArray.getBoolean(R.styleable.te_beauty_GLCameraView_transparent, false);
//        ratio = typedArray.getFloat(R.styleable.te_beauty_GLCameraView_ratio, 0f);
        typedArray.recycle();
    }

    private void init(Context context) {
        setBackgroundColor(Color.BLACK);
        if (isSurfaceView) {
            surfaceView = new SurfaceView(context);
            this.removeAllViews();
            this.addView(surfaceView);
            surfaceProvider = new TGLSurfaceProvider(surfaceView, null);
        } else {
            textureView = new TextureView(context);
            if (isTransparent) {
                this.textureView.setOpaque(false);
            }
            this.removeAllViews();
            this.addView(textureView);
            surfaceProvider = new TGLSurfaceProvider(textureView, null);
        }
        cameraxLifecycle = new CameraXLifecycleOwner();
        cameraxLifecycle.doOnCreate();
    }

    public void setCameraSize(Size size, boolean adjustPreviewViewPosition) {
        this.size = size;
        if (adjustPreviewViewPosition) {
            post(this::adjustPreviewViewPosition);
        }
    }

    private void adjustPreviewViewPosition() {

        if (size == null) {
            return;
        }
        float viewWidth = getMeasuredWidth();
        float viewHeight = getMeasuredHeight();
        if (viewWidth == 0 || viewHeight == 0) {
            return;
        }
        float viewAspectRadio = viewWidth / viewHeight;
        float cameraSizeAspectRadio = ((float) size.getWidth()) / size.getHeight();
        int resultWidth;
        int resultHeight;
        LogUtils.d(TAG, viewWidth + "  " + viewHeight + "  viewAspectRadio  " + viewAspectRadio + "   cameraSizeAspectRadio  " + cameraSizeAspectRadio);
        if (viewAspectRadio < cameraSizeAspectRadio) {
//            float cameraViewHeight = viewWidth * this.size.getHeight() / this.size.getWidth();
            float cameraViewHeight = viewWidth / cameraSizeAspectRadio;
            resultWidth = (int) viewWidth;
            resultHeight = (int) cameraViewHeight;
        } else {
//            float cameraViewWidth = viewHeight * this.size.getWidth() / this.size.getHeight();
            float cameraViewWidth = viewHeight * cameraSizeAspectRadio;
            resultWidth = (int) cameraViewWidth;
            resultHeight = (int) viewHeight;
        }
        LogUtils.d(TAG, "resultWidth " + resultWidth + "  " + resultHeight);
        View previewView = null;
        if (surfaceView != null) {
            previewView = surfaceView;
        } else {
            previewView = textureView;
        }
        LayoutParams layoutParams = (LayoutParams) previewView.getLayoutParams();
        layoutParams.width = resultWidth;
        layoutParams.height = resultHeight;
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        previewView.setLayoutParams(layoutParams);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void startPreview() {
        cameraxLifecycle.doOnStart();
        cameraxLifecycle.doOnResume();
        post(this::initCameraWhenCreated);
    }

    public void previewAgain() {
        post(this::initCameraWhenCreated);
    }

    public void stopPreview() {
        cameraxLifecycle.doOnPause();
        cameraxLifecycle.doOnStop();
    }

    public void release() {
        if (surfaceProvider != null) {
            surfaceProvider.release();
        }
        cameraxLifecycle.doOnDestroy();
        removeAllViews();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void initCameraWhenCreated() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext().getApplicationContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            Builder builder = new Builder();
            if (this.size != null) {
                builder.setTargetResolution(this.size);
            } else {
                builder.setTargetAspectRatio(AspectRatio.RATIO_16_9);
            }

            Camera2Interop.Extender ext = new Camera2Interop.Extender<>(builder);
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(30, 30));
            preview = builder.build();
            preview.setSurfaceProvider(surfaceProvider);
            setupCamera();
        }, ContextCompat.getMainExecutor(getContext()));
    }


    private void setupCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(isBackCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                    .build();
            if (this.surfaceProvider != null) {
                this.surfaceProvider.notifyCameraChanged(!isBackCamera);
            }
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
                if (this.glCameraXViewErrorListener != null) {
                    this.glCameraXViewErrorListener.onError();
                }
            }
        }
    }

    public void setCustomTextureProcessor(CustomTextureProcessor customTextureProcessor) {
        if (surfaceProvider != null) {
            surfaceProvider.setCustomTextureProcessor(customTextureProcessor);
        }
    }

    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener) {
        if (surfaceProvider != null) {
            surfaceProvider.setOnFrameAvailableListener(listener);
        }
    }

    public void switchCamera() {
        isBackCamera = !isBackCamera;
        setupCamera();
    }

    public void setIsBackCamera(boolean isBackCamera) {
        this.isBackCamera = isBackCamera;
    }

    public void setGlCameraXViewErrorListener(GLCameraXViewErrorListener glCameraXViewErrorListener) {
        this.glCameraXViewErrorListener = glCameraXViewErrorListener;
    }

    public interface GLCameraXViewErrorListener {
        void onError();
    }

    public void notifyCropStateChange(boolean isCropTexture) {
        this.surfaceProvider.notifyCropStateChange(isCropTexture);
    }
}
