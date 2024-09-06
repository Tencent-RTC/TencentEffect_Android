package com.tencent.demo.opengl.view.source;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.effect.beautykit.utils.LogUtils;


public class GLThread extends HandlerThread {
    private static final String TAG = GLThread.class.getName();

    private Handler handler = null;
    private EglCore mEglCore;
    private GLThreadEventListener threadEventListener = null;

    public GLThread(String name, GLThreadEventListener threadEventListener) {
        super(name);
        this.threadEventListener = threadEventListener;
    }

    @Override
    public synchronized void start() {
        super.start();
        this.handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == GLThreadEvent.INIT.value) {
                    onInitEGL();
                } else if (msg.what == GLThreadEvent.RELEASE.value) {
                    onRelease();
                } else {
                    if (threadEventListener != null) {
                        threadEventListener.onReceiveEvent(msg.what, mEglCore, msg.obj);
                    }
                }
            }
        };
        handler.sendEmptyMessage(GLThreadEvent.INIT.value);
    }

    // This method is called when the SurfaceHolder of the SurfaceView is created again.
    // Send this event to the GL thread.
    public void surfaceRecreate() {
        if (handler != null) {
            // Sometimes the processing time for a message may be longer than the time for the next message to arrive, causing a continuous backlog of messages in the queue. Therefore, when adding a subsequent message, remove any unprocessed messages from the queue.
            handler.removeMessages(GLThreadEvent.PROCESS.value);
            Message message = Message.obtain();
            message.what = GLThreadEvent.SURFACE_RECREATED.value;
            handler.sendMessage(message);
        }
    }

    // Send an event to the GL thread to create a SurfaceTexture.
    public void createSurfaceTexture() {
        if (handler != null) {
            // Sometimes the processing time for a message may be longer than the time for the next message to arrive, causing a continuous backlog of messages in the queue. Therefore, when adding a subsequent message, remove any unprocessed messages from the queue.
            handler.removeMessages(GLThreadEvent.PROCESS.value);
            Message message = Message.obtain();
            message.what = GLThreadEvent.CREATED_SURFACE_TEXTURE.value;
            handler.sendMessage(message);
        }
    }


    //向GL线程发送处理事件
    public void process(boolean isFrontCamera) {
        if (handler != null) {
            // Sometimes the processing time for a message may be longer than the time for the next message to arrive, causing a continuous backlog of messages in the queue. Therefore, when adding a subsequent message, remove any unprocessed messages from the queue.
            handler.removeMessages(GLThreadEvent.PROCESS.value);
            Message message = Message.obtain();
            message.what = GLThreadEvent.PROCESS.value;
            message.obj = isFrontCamera;
            handler.sendMessage(message);
        }
    }

    // Remove tasks that have not yet completed execution in the thread.
    public void pause() {
        if (handler != null) {
            handler.removeMessages(GLThreadEvent.PROCESS.value);
        }
    }

    // Send an event to destroy the GL environment.
    public void release() {
        LogUtils.d(TAG, "release " + Thread.currentThread().getName());
        if (handler != null) {
            handler.removeMessages(GLThreadEvent.PROCESS.value);
            handler.sendEmptyMessage(GLThreadEvent.RELEASE.value);
        }
    }

    /**
     * init EglCore
     */
    private void onInitEGL() {
        if (mEglCore == null) {
            mEglCore = new EglCore(null, 0);
        }
        if (this.threadEventListener != null) {
            this.threadEventListener.onGLContextCreated(mEglCore);
        }
    }


    /**
     * release
     */
    private void onRelease() {
        LogUtils.d(TAG, "onRelease " + Thread.currentThread().getName());
        if (this.threadEventListener != null) {
            this.threadEventListener.onGLContextDestroy();
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }

        if (handler != null) {
            handler.getLooper().quit();
            handler = null;
        }
    }


    public interface GLThreadEventListener {

        void onGLContextCreated(EglCore eglCore);

        void onReceiveEvent(int event, EglCore eglCore, Object parameter);


        void onGLContextDestroy();
    }


    public enum GLThreadEvent {
        // Message type. Initialize GL environment type.
        INIT(0),
        // Process texture type.
        PROCESS(1),
        // Destroy environment type.
        RELEASE(2),
        //recreate surfaceViewHolder type
        SURFACE_RECREATED(3),
        //create surfaceTexture type
        CREATED_SURFACE_TEXTURE(4);

        public int value;

        GLThreadEvent(int value) {
            this.value = value;
        }
    }

}
