package com.tencent.demo.camera.glthread;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;

import com.tencent.demo.camera.camerax.CustomTextureProcessor;

import java.util.concurrent.Executors;

public class TGLSurfaceProvider implements Preview.SurfaceProvider, TGLThreadListener {


    private static final String THREAD_NAME = "GL_THREAD";
    private SurfaceTexture mSurfaceTexture;

    private TGLBaseThread baseThread;
    private CustomTextureProcessor mCustomTextureProcessor = null;
    private SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = null;

    public TGLSurfaceProvider(TextureView textureView, CustomTextureProcessor customTextureProcessor) {
        this.mCustomTextureProcessor = customTextureProcessor;
        baseThread = new TGLTextureViewThread(THREAD_NAME, textureView, this);
        baseThread.start();
    }

    public TGLSurfaceProvider(SurfaceView surfaceView, CustomTextureProcessor customTextureProcessor) {
        this.mCustomTextureProcessor = customTextureProcessor;
        baseThread = new TGLSurfaceViewThread(THREAD_NAME, surfaceView, this);
        baseThread.start();
    }

    @Override
    public void onCreateSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        setOnFrameAvailableListener();
    }

    @Override
    public void onGLContextCreated() {
        if (mCustomTextureProcessor != null) {
            mCustomTextureProcessor.onGLContextCreated();
        }
    }

    @Override
    public int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight) {
        if (mCustomTextureProcessor != null) {
            return mCustomTextureProcessor.onCustomProcessTexture(textureId, textureWidth, textureHeight);
        }
        return textureId;
    }

    @Override
    public void onGLContextDestroy() {
        if (mCustomTextureProcessor != null) {
            mCustomTextureProcessor.onGLContextDestroy();
        }
    }

    public void setCustomTextureProcessor(CustomTextureProcessor customTextureProcessor) {
        this.mCustomTextureProcessor = customTextureProcessor;
    }

    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener) {
        this.mOnFrameAvailableListener = listener;
    }

    private void setOnFrameAvailableListener() {
        mSurfaceTexture.setOnFrameAvailableListener(surfaceTexture -> {
            if (baseThread != null) {
                baseThread.process();
            }
            if (mOnFrameAvailableListener != null) {
                mOnFrameAvailableListener.onFrameAvailable(surfaceTexture);
            }
        });
    }


    public void release() {
        if (baseThread != null) {
            baseThread.release();
        }
    }


    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        Size size = request.getResolution();
        request.setTransformationInfoListener(Executors.newSingleThreadExecutor(), transformationInfo -> {
                    if (baseThread != null) {
                        baseThread.notifyCameraSizeChanged(size.getWidth(), size.getHeight(), transformationInfo.getRotationDegrees());
                    }
                }
        );
        if (mSurfaceTexture == null) {
            return;
        } else {
            mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        }
        Log.e("onSurfaceRequested", size.toString());
        Surface surface = new Surface(mSurfaceTexture);
        request.provideSurface(surface, Executors.newSingleThreadExecutor(), result -> {
            if (result.getResultCode() == SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY) {
                result.getSurface().release();
            }
        });
    }


    /**
     * 设置是否在上屏之前进行裁剪处理
     * @param isCrop
     */
    public void notifyCropStateChange(boolean isCrop) {
        this.baseThread.notifyCropStateChange(isCrop);
    }

    public void notifyCameraChanged(boolean isFrontCamera) {
        this.baseThread.notifyCameraChanged(isFrontCamera);
    }
}
