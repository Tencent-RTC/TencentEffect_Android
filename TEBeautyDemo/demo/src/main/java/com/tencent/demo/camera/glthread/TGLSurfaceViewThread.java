package com.tencent.demo.camera.glthread;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.tencent.demo.gles.EglCore;
import com.tencent.demo.gles.WindowSurface;
import com.tencent.effect.beautykit.utils.LogUtils;

public class TGLSurfaceViewThread extends TGLBaseThread implements SurfaceHolder.Callback {

    private String TAG = TGLSurfaceViewThread.class.getName();
    private Surface mSurface = null;


    public TGLSurfaceViewThread(String name, SurfaceView surfaceView, TGLThreadListener listener) {
        super(name, listener);
        surfaceView.getHolder().addCallback(this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurface = holder.getSurface();
        initGl();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LogUtils.d(TAG, "surfaceChanged  " + width + "   " + height + "   ");
        notifySurfaceSizeChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    public WindowSurface createWindowSurface(EglCore mEglCore) {
        WindowSurface windowSurface = new WindowSurface(mEglCore, mSurface, false);
        windowSurface.makeCurrent();
        return windowSurface;
    }

    @Override
    public void releaseInputData() {
        if (mSurface != null) {
            mSurface.release();
        }
    }


}
