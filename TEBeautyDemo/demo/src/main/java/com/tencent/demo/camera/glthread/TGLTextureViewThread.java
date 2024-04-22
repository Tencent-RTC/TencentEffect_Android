package com.tencent.demo.camera.glthread;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import com.tencent.demo.gles.EglCore;
import com.tencent.demo.gles.WindowSurface;
import com.tencent.effect.beautykit.utils.LogUtils;

public class TGLTextureViewThread extends TGLBaseThread implements TextureView.SurfaceTextureListener {

    private String TAG = TGLTextureViewThread.class.getName();


    private SurfaceTexture inputSurfaceTexture = null;

    public TGLTextureViewThread(String name, TextureView textureView, TGLThreadListener listener) {
        super(name, listener);
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        LogUtils.d(TAG, "onSurfaceTextureAvailable  " + width + "   " + height + "   ");
        notifySurfaceSizeChanged(width,height);
        inputSurfaceTexture = surfaceTexture;
        initGl();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        LogUtils.d(TAG, "onSurfaceTextureSizeChanged  " + width + "   " + height + "   ");
        notifySurfaceSizeChanged(width,height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        pause();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public WindowSurface createWindowSurface(EglCore mEglCore) {
        WindowSurface windowSurface = new WindowSurface(mEglCore, inputSurfaceTexture);
        windowSurface.makeCurrent();
        return windowSurface;
    }

    @Override
    public void releaseInputData() {
        if (inputSurfaceTexture != null) {
            inputSurfaceTexture.release();
        }
    }

}
