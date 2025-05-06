package com.tencent.demo.opengl.gles;

import android.annotation.TargetApi;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

@TargetApi(18)
public class SimpleGLThread {
    private Handler mHandler;
    private EglCore mCore;
    private OffscreenSurface mOffscreenSurface;
    private String mThreadName;

    public interface OnSurfaceCreatedListener {
        void onSurfaceCreated(OffscreenSurface offscreenSurface);
    }

    public SimpleGLThread(final EGLContext shareContext, String name, int offscreenWidth, int offscreenHeight) {
        this(shareContext, name, offscreenWidth, offscreenHeight, null);
    }

    public SimpleGLThread(final EGLContext shareContext, String name, int offscreenWidth, int offscreenHeight, final OnSurfaceCreatedListener listener) {
        mThreadName = name;
        HandlerThread ht = new HandlerThread(mThreadName);
        ht.start();
        mHandler = new Handler(ht.getLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCore = new EglCore(shareContext, 0);
                } catch (RuntimeException e) {
                    Log.e("SimpleGLThread", "new EglCore crash : " + e.getMessage());

                    mCore = null;
                    return;
                }
                mOffscreenSurface = new OffscreenSurface(mCore, offscreenWidth, offscreenHeight);
                mOffscreenSurface.makeCurrent();
                if (listener != null) {
                    listener.onSurfaceCreated(mOffscreenSurface);
                }
            }
        });
    }

    public void sendEmptyMessage(int what) {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(what);
        }
    }

    public void removeCallbacksAndMessages(Object token) {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(token);
        }
    }

    public void postJob(Runnable runnable) {
        if (mHandler != null) {
            mHandler.removeMessages(0);
            mHandler.post(runnable);
        }
    }

    public void postJobSync(Runnable runnable) {
        if (mHandler != null) {
            mHandler.post(runnable);
            HandlerUtil.waitDone(mHandler);
        }
    }

    public void makeCurrent() {
        mOffscreenSurface.makeCurrent();
    }

    public void destroy() {
        destroy(null);
    }

    /**
     * destroy
     *
     * @param clearRunable 销毁时的callback，用于在该线程上销毁资源
     */
    public void destroy(final Runnable clearRunable) {
        if (mHandler != null) {
            mHandler.removeMessages(0);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (clearRunable != null) {
                        clearRunable.run();
                    }

                    if (mCore != null) {
                        mOffscreenSurface.release();
                        mCore.release();
                        mHandler.getLooper().quitSafely();
                    }
                }
            });
        }
    }

    public Looper getLooper() {
        return mHandler == null ? null : mHandler.getLooper();
    }

    public void waitDone() {
        HandlerUtil.waitDone(mHandler);
    }

    public void waitDone(int timeoutMillis) {
        HandlerUtil.waitDone(mHandler, timeoutMillis);
    }
}
