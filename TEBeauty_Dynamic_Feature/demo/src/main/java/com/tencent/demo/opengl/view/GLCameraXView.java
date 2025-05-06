package com.tencent.demo.opengl.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.demo.R;
import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.view.display.Display;
import com.tencent.demo.opengl.view.display.DisplayImp;
import com.tencent.demo.opengl.view.display.DisplayStateChangeListener;
import com.tencent.demo.opengl.view.process.CameraProcessor;
import com.tencent.demo.opengl.view.process.Processor;
import com.tencent.demo.opengl.view.source.CameraSource;
import com.tencent.demo.opengl.view.source.SourceEventListener;




public class GLCameraXView extends FrameLayout {

    private static final String TAG = GLCameraXView.class.getName();


    private boolean isSurfaceView = false;
    private boolean isFrontCamera = true;
    private boolean isTransparent = false;

    private CameraSource cameraSource = null;
    private Processor processor;
    private Display display;


    public GLCameraXView(@NonNull Context context) {
        this(context, null);
    }

    public GLCameraXView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GLCameraXView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            getAttributes(context, attrs);
        }
        this.init(context);
    }


    private void getAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.te_beauty_GLCameraView);
        isFrontCamera = typedArray.getBoolean(R.styleable.te_beauty_GLCameraView_front_camera, true);
        isSurfaceView = typedArray.getBoolean(R.styleable.te_beauty_GLCameraView_surface_view, true);
        isTransparent = typedArray.getBoolean(R.styleable.te_beauty_GLCameraView_transparent, false);
        typedArray.recycle();
    }

    private void init(Context context) {
        setBackgroundColor(Color.BLACK);
        if (isSurfaceView) {
            display = new DisplayImp(new SurfaceView(context));
        } else {
            TextureView textureView = new TextureView(context);
            display = new DisplayImp(textureView);
            if (isTransparent) {
                textureView.setOpaque(false);
            }
        }
        processor = new CameraProcessor();
        this.display.setDisplayStateChangeListener(new DisplayStateChangeListener() {
            @Override
            public void onReady() {
                cameraSource.initGlContext();
            }

            @Override
            public void onSurfaceRecreated() {
                cameraSource.surfaceRecreate();
            }


            @Override
            public void onSizeChange(int width, int height) {
                ((CameraProcessor) processor).updateSurfaceSize(new Size(width, height));
            }

            @Override
            public void onDestroy() {

            }
        });
        processor.setDisplay(display);
        cameraSource = new CameraSource(context);
        cameraSource.setSourceEventListener(new SourceEventListener() {
            @Override
            public void onGLContextCreated(EglCore eglCore) {
                processor.onGLContextCreated(eglCore);
                display.onGLContextCreated(eglCore);
            }

            @Override
            public void onSurfaceRecreated(EglCore eglCore) {
                display.onGLContextCreated(eglCore);
            }

            @Override
            public void onReceiveFrameTexture(EglCore eglCore, FrameTexture frameTexture) {
                processor.process(eglCore, frameTexture);
            }

            @Override
            public void onGLContextDestroy() {
                processor.onGLContextDestroy();
                display.onGLContextDestroy();
            }
        });
        cameraSource.setFrontCamera(isFrontCamera);
        this.removeAllViews();
        this.addView(this.display.getDisplayView());

    }

    public void setCameraSize(Size size) {
        cameraSource.setCameraSize(size);
    }



    public void startPreview() {
        cameraSource.start();
    }

    public void previewAgain() {
        cameraSource.reStart();
    }


    public void stopPreview() {
        cameraSource.pause();
    }



    public void release() {
        cameraSource.release();
        removeAllViews();
    }


    /**
     * setCustomTextureProcessor
     *
     * @param customTextureProcessor
     */
    public void setCustomTextureProcessor(CustomTextureProcessor customTextureProcessor) {
        if (processor != null) {
            processor.setCustomTextureProcessor(customTextureProcessor);
        }
    }

    /**
     * setOnFrameAvailableListener
     */
    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener) {
        if (cameraSource != null) {
            cameraSource.setOnFrameAvailableListener(listener);
        }
    }


    public void switchCamera() {
        cameraSource.switchCamera();
    }


    public void setGlCameraXViewErrorListener(GLCameraXViewErrorListener glCameraXViewErrorListener) {
        this.cameraSource.setCameraErrorListener(glCameraXViewErrorListener);
    }

    public interface GLCameraXViewErrorListener {
        void onError();
    }



    public void setCropRatio(float ratio) {
        ((CameraProcessor) this.processor).setCropRatio(ratio);
    }


}
