package com.tencent.demo.opengl.view.display;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import com.tencent.demo.opengl.TextureTransform;
import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.gles.WindowSurface;
import com.tencent.demo.opengl.view.FrameTexture;
import com.tencent.demo.utils.LogUtils;

public class DisplayImp implements Display, TextureView.SurfaceTextureListener, SurfaceHolder.Callback {

    private static final String TAG = DisplayImp.class.getName();


    private DisplayStateChangeListener displayStateChangeListener = null;
    private TextureTransform textureTransform = null;
    private final float[] IdentityMatrix = new TextureTransform.Transition().getMatrix();
    private WindowSurface windowSurface = null;
    private Surface mSurface = null;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;
    private View displayView = null;


    public DisplayImp(@NonNull SurfaceView surfaceView) {
        this.displayView = surfaceView;
        surfaceView.getHolder().addCallback(this);
    }

    public DisplayImp(TextureView textureView) {
        this.displayView = textureView;
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public View getDisplayView() {
        return displayView;
    }

    @Override
    public void display(EglCore eglCore, FrameTexture frameTexture) {
        if (textureTransform == null) {
            textureTransform = new TextureTransform();
        }
        textureTransform.renderOnScreen(frameTexture.textureId, frameTexture.textureFormat, surfaceWidth, surfaceHeight, IdentityMatrix);
        windowSurface.swapBuffers();
    }

    @Override
    public void onGLContextCreated(EglCore eglCore) {
        this.releaseWindowSurface();
        windowSurface = new WindowSurface(eglCore, mSurface, false);
        windowSurface.makeCurrent();
    }

    @Override
    public void onGLContextDestroy() {
        this.releaseWindowSurface();
        if (textureTransform != null) {
            textureTransform.release();
        }
    }

    private void releaseWindowSurface() {
        if (windowSurface != null) {
            windowSurface.release();
            windowSurface = null;
        }
    }

    @Override
    public void setDisplayStateChangeListener(DisplayStateChangeListener listener) {
        this.displayStateChangeListener = listener;
    }


    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        this.mSurface = new Surface(surface);
        this.surfaceWidth = width;
        this.surfaceHeight = height;
        LogUtils.d(TAG, "onSurfaceTextureAvailable    " + width + " " + height);
        if (this.displayStateChangeListener != null) {
            this.displayStateChangeListener.onReady();
            this.displayStateChangeListener.onSizeChange(width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        this.surfaceWidth = width;
        this.surfaceHeight = height;
        LogUtils.d(TAG, "onSurfaceTextureSizeChanged    " + width + " " + height);
        if (this.displayStateChangeListener != null) {
            this.displayStateChangeListener.onSizeChange(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        LogUtils.d(TAG, "onSurfaceTextureDestroyed    ");
        if (this.displayStateChangeListener != null) {
            this.displayStateChangeListener.onDestroy();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        LogUtils.d(TAG, "onSurfaceTextureUpdated    ");
//        this.mSurface = new Surface(surface);
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        LogUtils.d(TAG, "surfaceCreated");
        if (this.displayStateChangeListener != null) {
            if (this.mSurface == null) {
                this.displayStateChangeListener.onReady();
            } else {
                this.displayStateChangeListener.onSurfaceRecreated();
            }
        }
        this.mSurface = holder.getSurface();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        LogUtils.d(TAG, "surfaceChanged    " + width + " " + height);
        this.surfaceWidth = width;
        this.surfaceHeight = height;
        if (this.displayStateChangeListener != null) {
            this.displayStateChangeListener.onSizeChange(width, height);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        LogUtils.d(TAG, "surfaceDestroyed    ");
        if (this.displayStateChangeListener != null) {
            this.displayStateChangeListener.onDestroy();
        }
    }
}
