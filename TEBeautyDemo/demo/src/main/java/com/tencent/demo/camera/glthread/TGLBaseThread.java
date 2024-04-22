package com.tencent.demo.camera.glthread;


import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.demo.camera.glrender.DirectRenderer;
import com.tencent.demo.camera.glthread.textureChain.AfterBeautyCropTextureFilter;
import com.tencent.demo.camera.glthread.textureChain.BeautyTextureFilter;
import com.tencent.demo.camera.glthread.textureChain.CameraXTexture;
import com.tencent.demo.camera.glthread.textureChain.CropTextureFilter;
import com.tencent.demo.camera.glthread.textureChain.OES2RGBATextureFilter;
import com.tencent.demo.camera.glthread.textureChain.RotateTextureFilter;
import com.tencent.demo.camera.glthread.textureChain.TextureFilter;
import com.tencent.demo.gles.EglCore;
import com.tencent.demo.gles.WindowSurface;
import com.tencent.demo.utils.GlUtil;
import com.tencent.effect.beautykit.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;


public abstract class TGLBaseThread extends HandlerThread {
    private String TAG = TGLBaseThread.class.getName();
    //消息类型。初始化gl环境类型
    public static final int INIT = 0;
    //处理纹理类型
    public static final int PROCESS = 1;
    //销毁环境类型
    public static final int RELEASE = 2;


    protected Handler handler = null;

    private EglCore mEglCore;
    private DirectRenderer mScreenRenderer;
    private WindowSurface windowSurface;
    private int mCameraOesTextureId = -1;

    private int mCameraWidth = -1;
    private int mCameraHeight = -1;

    private TGLThreadListener threadListener = null;

    //用于接收相机数据
    private SurfaceTexture mSurfaceTexture;

    private int mSurfaceWidth = -1;
    private int mSurfaceHeight = -1;


    private boolean isFrontCamera = true;  //是否是前置摄像头
    private boolean isCropOnAfterBeauty = false; //美颜之后是否进行边缘裁剪


    //用于将oes纹理转换为rgba纹理
    private OES2RGBATextureFilter oes2RGBATextureFilter = null;
    //将纹理进行旋转处理
    private RotateTextureFilter rotateTextureFilter = null;
    //对纹理进行裁剪操作
    private CropTextureFilter cropTextureFilter = null;
    //美颜处理
    private BeautyTextureFilter beautyTextureFilter = null;
    //美颜之后对纹理进行裁剪
    private AfterBeautyCropTextureFilter afterBeautyCropTextureFilter = null;
    private List<TextureFilter> textureFilterList = new ArrayList<>();


    public TGLBaseThread(String name, TGLThreadListener threadListener) {
        super(name);
        this.threadListener = threadListener;
    }

    public TGLBaseThread(String name, int priority, TGLThreadListener threadListener) {
        super(name, priority);
        this.threadListener = threadListener;
    }


    public abstract WindowSurface createWindowSurface(EglCore mEglCore);

    public abstract void releaseInputData();

    public void initGl() {
        if (handler != null) {
            handler.sendEmptyMessage(INIT);
        }
    }

    public void pause() {
        if (handler != null) {
            handler.removeMessages(PROCESS);
        }
    }

    public void process() {
        if (handler != null) {
            //有时消息处理的时间可能大于下个消息过来的时间，这样就导致队列中的消息会不断的拥挤，所以在添加后一个消息的时候，移除队列中未被处理的消息
            handler.removeMessages(PROCESS);
            handler.sendEmptyMessage(PROCESS);
        }
    }

    public void release() {
        LogUtils.d(TAG, "release " + Thread.currentThread().getName());
        if (handler != null) {
            //在发送销毁消息的时候先将处理任务的消息全部移除
            handler.removeMessages(PROCESS);
            handler.sendEmptyMessage(RELEASE);
        }
    }


    /**
     * 设置是否美颜之后再进行裁剪
     *
     * @param isCropTexture
     */
    public void notifyCropStateChange(boolean isCropTexture) {
        this.isCropOnAfterBeauty = isCropTexture;
        this.setCropState();
    }

    /**
     * 当前后摄像头切换的视乎调用此方法
     *
     * @param isFrontCamera
     */
    public void notifyCameraChanged(boolean isFrontCamera) {
        this.isFrontCamera = isFrontCamera;
        this.setCameraState();
    }

    /**
     * 当相机分辨率改变的时候调用此方法
     *
     * @param cameraWidth
     * @param cameraHeight
     * @param rotationDegrees
     */
    public void notifyCameraSizeChanged(int cameraWidth, int cameraHeight, int rotationDegrees) {
        LogUtils.d(TAG, "onCameraSizeChange  " + cameraWidth + "   " + cameraHeight + "   " + rotationDegrees);
        this.mCameraWidth = cameraWidth;
        this.mCameraHeight = cameraHeight;
    }

    /**
     * 当画面view 尺寸发生改变时调用
     *
     * @param width
     * @param height
     */
    protected void notifySurfaceSizeChanged(int width, int height) {
        Log.e(TAG, "onSurfaceChanged:   " + width + "   " + height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        this.setScreenWH();
    }


    private synchronized void setScreenWH() {
        if (cropTextureFilter != null) {
            cropTextureFilter.setWidthAndHeight(this.mSurfaceWidth, this.mSurfaceHeight);
        }
    }

    private synchronized void setCameraState() {
        if (this.rotateTextureFilter != null) {
            this.rotateTextureFilter.setFrontCamera(this.isFrontCamera);
        }
    }

    private synchronized void setCropState() {
        if (this.afterBeautyCropTextureFilter != null) {
            this.afterBeautyCropTextureFilter.setIsCropTexture(this.isCropOnAfterBeauty);
        }
    }


    @Override
    public synchronized void start() {
        super.start();
        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case INIT:
                        onInitGl();
                        break;
                    case PROCESS:
                        onProcess();
                        break;
                    case RELEASE:
                        onRelease();
                        break;
                }
            }
        };
    }


    private void onInitGl() {
        if (mEglCore == null) {
            mEglCore = new EglCore(null, 0);
        }
        if (windowSurface != null) {
            windowSurface.release();
            windowSurface = null;
        }
        windowSurface = createWindowSurface(mEglCore);

        releaseTexture(mCameraOesTextureId);
        mCameraOesTextureId = GlUtil.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mCameraOesTextureId);
        if (threadListener != null) {
            threadListener.onCreateSurfaceTexture(mSurfaceTexture);
        }

        this.initTextureFilter();
        this.mScreenRenderer = new DirectRenderer();
        this.mScreenRenderer.init();
    }


    private void initTextureFilter() {
        this.oes2RGBATextureFilter = new OES2RGBATextureFilter();
        this.textureFilterList.add(this.oes2RGBATextureFilter);

        this.rotateTextureFilter = new RotateTextureFilter();
        this.setCameraState();
        this.textureFilterList.add(this.rotateTextureFilter);

        this.cropTextureFilter = new CropTextureFilter();
        this.setScreenWH();
        this.textureFilterList.add(cropTextureFilter);

        this.beautyTextureFilter = new BeautyTextureFilter(this.threadListener);
        this.textureFilterList.add(this.beautyTextureFilter);

        this.afterBeautyCropTextureFilter = new AfterBeautyCropTextureFilter();
        this.setCropState();
        this.textureFilterList.add(this.afterBeautyCropTextureFilter);

        for (TextureFilter textureFilter : this.textureFilterList) {
            textureFilter.init();
        }
    }

    private void onProcess() {
        if (windowSurface == null) {
            return;
        }
        if (mSurfaceTexture == null) {
            return;
        }
        mSurfaceTexture.updateTexImage();

        CameraXTexture processTexture = new CameraXTexture(mCameraOesTextureId, mCameraWidth, mCameraHeight);
        for (TextureFilter textureFilter : this.textureFilterList) {
            processTexture = textureFilter.processTexture(processTexture);
        }

        // 纹理上屏
        mScreenRenderer.doRender(processTexture.textureId,
                -1, processTexture.width, processTexture.height, null, null, null);
        // If the SurfaceTexture has been destroyed, this will throw an exception.
        windowSurface.swapBuffers();
    }


    private void onRelease() {
        LogUtils.d(TAG, "onRelease " + Thread.currentThread().getName());
        if (this.threadListener != null) {
            this.threadListener.onGLContextDestroy();
        }
        releaseInputData();
        if (windowSurface != null) {
            windowSurface.release();
            windowSurface = null;
        }
        this.releaseFilter();
        if (mScreenRenderer != null) {
            mScreenRenderer.release();
            mScreenRenderer = null;
        }

        releaseTexture(mCameraOesTextureId);
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (handler != null) {
            handler.getLooper().quit();
            handler = null;
        }
    }


    private void releaseFilter() {
        for (TextureFilter textureFilter : this.textureFilterList) {
            textureFilter.release();
        }
    }


    protected void releaseTexture(int id) {
        if (id >= 0) {
            GLES20.glDeleteTextures(1, new int[]{id}, 0);
        }
    }


}
