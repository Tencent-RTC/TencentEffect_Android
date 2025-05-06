package com.tencent.demo.opengl.view.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Size;

import androidx.annotation.NonNull;

import com.tencent.demo.opengl.GlUtil;
import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.render.TextureFormat;
import com.tencent.demo.opengl.view.FrameTexture;
import com.tencent.demo.utils.TimerManager;

public class BitmapSource implements Source, GLThread.GLThreadEventListener, TimerManager.TimerManagerCallback {


    private SourceEventListener sourceEventListener = null;
    private Context context = null;
    private volatile Bitmap bitmap = null;
    private volatile boolean needBindBitmap = false;
    private GLThread glThread = null;
    private int surfaceTextureId = -1;

    private int textureWidth = 0;
    private int textureHeight = 0;
    private boolean isTextureSizeChange = false;

    private TimerManager timerManager = null;


    public BitmapSource(Context context) {
        this.context = context;
        this.glThread = new GLThread("bitmap_gl_thread", this);
        this.timerManager = new TimerManager(this);
    }

    /**
     * 开启GL线程，并初始化GL环境
     */
    @Override
    public void initGlContext() {
        this.glThread.start();
    }

    /**
     * 开启定时任务，让GL线程开始工作
     */
    @Override
    public void start() {
        this.timerManager.start();
    }


    @Override
    public void setSourceEventListener(SourceEventListener sourceEventListener) {
        this.sourceEventListener = sourceEventListener;
    }

    @Override
    public void surfaceRecreate() {
        this.glThread.surfaceRecreate();
    }

    @Override
    public void pause() {
        this.timerManager.pause();
        this.glThread.pause();
    }

    /**
     * 销毁GL线程，并销毁定时器
     */
    @Override
    public void release() {
        this.glThread.release();
        this.timerManager.release();
    }

    public void setData(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.needBindBitmap = true;
    }


    public void updateDisplaySize(@NonNull Size size) {
        this.textureWidth = size.getWidth();
        this.textureHeight = size.getHeight();
        this.isTextureSizeChange = true;
    }


    //当GL环境准备好的时候会回调此接口
    @Override
    public void onGLContextCreated(EglCore eglCore) {
        if (this.sourceEventListener != null) {
            this.sourceEventListener.onGLContextCreated(eglCore);
        }
    }

    @Override
    public void onReceiveEvent(int event, EglCore eglCore, Object parameter) {
        if (event == GLThread.GLThreadEvent.PROCESS.value) {
            int width = textureWidth;
            int height = textureHeight;
            if (surfaceTextureId == -1) {
                surfaceTextureId = GlUtil.createTexture(width, height, GLES20.GL_RGBA);
            }
            if (isTextureSizeChange) {
                //当显示宽高改变的时候重新创建纹理
                GlUtil.deleteTextureId(surfaceTextureId);
                surfaceTextureId = GlUtil.createTexture(width, height, GLES20.GL_RGBA);
                isTextureSizeChange = false;
                this.needBindBitmap = true;
            }
            bindBitmap(surfaceTextureId, this.bitmap, width, height);
            FrameTexture frameTexture = new FrameTexture();
            frameTexture.isFrontCamera = (boolean) parameter;
            frameTexture.textureFormat = TextureFormat.Texture_2D;
            frameTexture.textureId = surfaceTextureId;
            frameTexture.textureWidth = width;
            frameTexture.textureHeight = height;
            if (this.sourceEventListener != null) {
                this.sourceEventListener.onReceiveFrameTexture(eglCore, frameTexture);
            }
        } else if (event == GLThread.GLThreadEvent.SURFACE_RECREATED.value) {
            if (this.sourceEventListener != null) {
                this.sourceEventListener.onSurfaceRecreated(eglCore);
            }
        }
    }


    private void bindBitmap(int surfaceTextureId, Bitmap bitmap, int width, int height) {
        if (this.needBindBitmap) {
            this.needBindBitmap = false;
            if (bitmap != null) {
                GlUtil.bindTexture(bitmap, surfaceTextureId);
            } else {
                GlUtil.clearTexture(surfaceTextureId, width, height);
            }
        }
    }


    @Override
    public void onGLContextDestroy() {
        if (this.sourceEventListener != null) {
            this.sourceEventListener.onGLContextDestroy();
        }
    }


    @Override
    public void onCall() {
        if (this.glThread != null) {
            this.glThread.process(true);
        }
    }
}
