package com.tencent.demo.opengl;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Size;
import android.widget.ImageView;

import com.tencent.demo.opengl.render.RendererManager;
import com.tencent.demo.opengl.render.TextureFormat;
import com.tencent.demo.utils.LogUtils;


public class TextureTransform {
    private static final String TAG = "TextureTransform";

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

    private RendererManager mRendererManager;


    /**
     * {zh}
     *
     * @param inputTexture        输入纹理
     * @param inputTextureFormat  输入纹理格式，2D/OES
     * @param outputTextureFormat 输出纹理格式，2D/OES
     * @param width               输入纹理的宽
     * @param height              输入纹理的高
     * @param transition          纹理变换方式
     * @return 输出纹理 int
     */
    public int transferTextureToTexture(int inputTexture, TextureFormat inputTextureFormat,
                                        TextureFormat outputTextureFormat,
                                        int width, int height, Transition transition) {
        if (outputTextureFormat != TextureFormat.Texture_2D) {
            LogUtils.e(TAG, "the inputTexture is not supported,please use Texure2D as output texture format");
            return GlUtil.NO_TEXTURE;
        }
        if (null == mRendererManager) {
            mRendererManager = new RendererManager();
        }

        boolean targetRotate = transition.getAngle() % 180 == 90;
        return mRendererManager.getRenderer(inputTextureFormat).renderOffScreen(inputTexture, targetRotate ? height : width, targetRotate ? width : height, transition.getMatrix());
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


    /**
     *   Render texture on screen
     *
     * @param textureId        纹理ID
     * @param srcTextureFormat 纹理格式
     * @param surfaceWidth     视口宽度
     * @param surfaceHeight    视口高度
     * @param mMVPMatrix       旋转矩阵
     */
    public void renderOnScreen(int textureId, TextureFormat srcTextureFormat, int surfaceWidth, int surfaceHeight, float[] mMVPMatrix) {
        if (null == mRendererManager) {
            mRendererManager = new RendererManager();
        }
        mRendererManager.getRenderer(srcTextureFormat).renderOnScreen(textureId, surfaceWidth, surfaceHeight, mMVPMatrix);
    }

    public void release() {
        destroyFrameBuffers();
        if (null != mRendererManager) {
            mRendererManager.release();
        }
    }



    /**
     * The type Transition.
     */
    public static class Transition {

        private float[] mMVPMatrix = new float[16];
        private int mAngle = 0;
        private Size croppedSize;

        /**
         * Instantiates a new Transition.
         */
        public Transition() {
            Matrix.setIdentityM(mMVPMatrix, 0);
        }


        /**
         * {zh}
         *
         * @param x the x
         * @param y the y
         * @return the transition
         */
        public Transition flip(boolean x, boolean y) {
            GlUtil.flip(mMVPMatrix, x, y);
            return this;

        }

        /**
         * Gets angle.
         *
         * @return the angle
         */
        public int getAngle() {
            return mAngle % 360;
        }

        /**
         * {zh}
         *
         * @param angle 旋转角度，仅支持 0/90/180/270
         * @return the transition
         */
        public Transition rotate(int angle) {
            mAngle = angle;
            GlUtil.rotate(mMVPMatrix, angle);
            return this;

        }

        /**
         * Scale transition.
         *
         * @param sx the sx
         * @param sy the sy
         * @return the transition
         */
        public Transition scale(float sx, float sy) {
            GlUtil.scale(mMVPMatrix, sx, sy);
            return this;
        }


        /**
         * Crop transition.
         *
         * @param scaleType     the scale type
         * @param rotation      the rotation
         * @param textureWidth  the texture width
         * @param textureHeight the texture height
         * @param surfaceWidth  the surface width
         * @param surfaceHeight the surface height
         * @return the transition
         */
        public Transition crop(ImageView.ScaleType scaleType, int rotation, int textureWidth, int textureHeight, int surfaceWidth, int surfaceHeight) {
            if (rotation % 180 == 90) {
                this.croppedSize = GlUtil.cropSize(mMVPMatrix, scaleType, textureHeight, textureWidth, surfaceWidth, surfaceHeight);
            } else {
                this.croppedSize = GlUtil.cropSize(mMVPMatrix, scaleType, textureWidth, textureHeight, surfaceWidth, surfaceHeight);
            }
            return this;
        }

        public Size getCroppedSize() {
            return croppedSize;
        }


        public float[] getMatrix() {
            return mMVPMatrix;
        }


    }
}
