package com.tencent.demo.opengl.render;

import android.graphics.Point;
import android.opengl.GLES20;

import com.tencent.demo.opengl.GlUtil;


/**
 * The type Program.
 */
public abstract class Renderer {

    private static final String TAG = GlUtil.TAG;

    /**
     * The M program handle.
     */
    protected int mProgramHandle;

    /**
     * The M drawable 2 d.
     */
    protected Drawable2d mDrawable2d;


    /**
     * The M frame buffers.
     */
    protected int[] mFrameBuffers;
    /**
     * The M frame buffer textures.
     */
    protected int[] mFrameBufferTextures;
    /**
     * The Frame buffer num.
     */
    protected int frameBufferNum = 1;
    /**
     * The M frame buffer shape.
     */
    protected Point mFrameBufferShape;

    /**
     * Prepares the program in the current EGL context.
     *
     * @param vertexShader     the vertex shader
     * @param fragmentShader2D the fragment shader 2 d
     */
    public Renderer(String vertexShader, String fragmentShader2D) {
        mProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader2D);
        mDrawable2d = new Drawable2d();
        getLocations();
    }


    /**
     * get locations of attributes and uniforms
     */
    protected abstract void getLocations();

    /**
     * render frame on screen.
     *
     * @param textureId the texture id
     * @param width     the width
     * @param height    the height
     * @param mvpMatrix the mvp matrix
     */
    public abstract void renderOnScreen(int textureId, int width, int height, float[] mvpMatrix);


    /**
     * render frame off screen.
     *
     * @param textureId the texture id
     * @param width     the width
     * @param height    the height
     * @param mvpMatrix the mvp matrix
     * @return the int
     */
    public abstract int renderOffScreen(int textureId, int width, int height, float[] mvpMatrix);


    /**
     * Init frame buffer if need.
     *
     * @param width  the width
     * @param height the height
     */
    protected void initFrameBufferIfNeed(int width, int height) {
        boolean need = false;
        if (null == mFrameBufferShape || mFrameBufferShape.x != width || mFrameBufferShape.y != height) {
            need = true;
        }
        if (mFrameBuffers == null || mFrameBufferTextures == null) {
            need = true;
        }
        if (need) {
            mFrameBuffers = new int[frameBufferNum];
            mFrameBufferTextures = new int[frameBufferNum];
            GLES20.glGenFramebuffers(frameBufferNum, mFrameBuffers, 0);
            GLES20.glGenTextures(frameBufferNum, mFrameBufferTextures, 0);
            for (int i = 0; i < frameBufferNum; i++) {
                bindFrameBuffer(mFrameBufferTextures[i], mFrameBuffers[i], width, height);
            }
            mFrameBufferShape = new Point(width, height);
        }
    }

    private void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(frameBufferNum, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(frameBufferNum, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    public void release() {
        destroyFrameBuffers();
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }
}
