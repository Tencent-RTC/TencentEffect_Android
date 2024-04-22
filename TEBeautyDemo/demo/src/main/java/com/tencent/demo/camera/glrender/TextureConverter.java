package com.tencent.demo.camera.glrender;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.microedition.khronos.opengles.GL10;

/**
 * 纹理转换类
 * 主要用于将oes纹理转换为rgba纹理，
 * 横屏纹理转换为竖屏纹理
 * 对纹理进行镜像处理
 */
public class TextureConverter {

    private OESRenderer mYUV2RGBAConverter;
    private RotateRenderer mRotateRender;

    private SurfaceTexture mSurfaceTexture;
    private int yuv2rRgbaID = -1;
    private int rotatedId = -1;

    private final float[] mOesMatrix = new float[16];


    private int lastRotation = -1;


    private int lastTextureWidth = 0;
    private int lastTextureHeight = 0;

    private int lastOesTextureWidth = 0;
    private int lastOesTextureHeight = 0;

    public TextureConverter() {

    }

    /**
     * 此方法用于将oes纹理转换为rgba纹理
     *
     * @param srcID  oes 纹理
     * @param width  纹理宽度
     * @param height 纹理高度
     * @return rgba纹理ID
     */
    public int oes2Rgba(int srcID, int width, int height) {
        if (this.lastOesTextureWidth != width || this.lastOesTextureHeight != height) {
            releaseOes();
        }
        this.lastOesTextureWidth = width;
        this.lastOesTextureHeight = height;
        if (mYUV2RGBAConverter == null) {
            mYUV2RGBAConverter = createOESRenderer();
        }
        if (mSurfaceTexture == null) {
            mSurfaceTexture = new SurfaceTexture(srcID);
            releaseTexture(yuv2rRgbaID);
            yuv2rRgbaID = createTexture(width, height, GLES20.GL_RGBA);
        }
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mOesMatrix);
        mYUV2RGBAConverter.doRender(srcID, yuv2rRgbaID, width, height, mOesMatrix, null, null);
        return yuv2rRgbaID;
    }


    /**
     * 此方法用于对rgba纹理进行旋转和镜像处理。处理过程为：先顺时针旋转rotation度（可取值0,90,180，270），再进行左右翻转(flipHorizontal）和 上下翻转（flipVertical）。
     * 使用场景：某些推流SDK返回的纹理是横屏纹理或者画面中人物朝向不对，而腾讯特效SDK要求纹理中的人物是正向的，所以可以通过此方法对纹理进行转换。
     *
     * @param srcID    rgba纹理
     * @param width    纹理宽度
     * @param height   纹理高度
     * @param rotation 需要进行旋转的角度。
     * @return 旋转后的纹理，注意：如果旋转90或者270度，那么宽度需要进行交换。
     */
    public int convert(int srcID, int width, int height, @RotationDegreesValue int rotation, boolean flipVertical, boolean flipHorizontal) {
        if (this.lastTextureWidth != width || this.lastTextureHeight != height) {
            releaseConvert();
        }
        this.lastTextureWidth = width;
        this.lastTextureHeight = height;
        int degree = rotation % 360;
        if (degree != lastRotation) {
            lastRotation = degree;
            releaseConvert();
        }

        if (mRotateRender == null) {
            mRotateRender = createRotateRender(degree, flipVertical, flipHorizontal);
        }
        if (degree == 270 || degree == 90) {
            //由于270和90转换会切换横竖屏，所以需要将宽高进行 互调
            int temp = width;
            width = height;
            height = temp;
        }
        if (rotatedId == -1) {
            rotatedId = createTexture(width, height, GLES20.GL_RGBA);
        }
        mRotateRender.doRender(srcID, rotatedId, width, height, null, null, null);
        return rotatedId;
    }


    private void releaseOes() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mYUV2RGBAConverter != null) {
            mYUV2RGBAConverter.release();
            mYUV2RGBAConverter = null;
        }

        releaseTexture(yuv2rRgbaID);
        yuv2rRgbaID = -1;
        this.lastOesTextureWidth = 0;
        this.lastOesTextureHeight = 0;
    }

    private void releaseConvert() {
        if (mRotateRender != null) {
            mRotateRender.release();
            mRotateRender = null;
        }
        releaseTexture(rotatedId);
        rotatedId = -1;
        lastRotation = -1;
        this.lastTextureWidth = 0;
        this.lastTextureHeight = 0;
    }


    public void release() {
        releaseOes();
        releaseConvert();
    }


    private int createTexture(int width, int height, int config) {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, config, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return texture[0];
    }


    private OESRenderer createOESRenderer() {
        OESRenderer oesRenderer = new OESRenderer();
        oesRenderer.initNoRotate();
        return oesRenderer;
    }


    private RotateRenderer createRotateRender(int degree, boolean flipVertical, boolean flipHorizontal) {
        RotateRenderer rotateRenderer = new RotateRenderer();
        rotateRenderer.initRotate(degree, flipVertical, flipHorizontal);
        return rotateRenderer;
    }

    private void releaseTexture(int id) {
        if (id >= 0) {
            GLES20.glDeleteTextures(1, new int[]{id}, 0);
        }
    }


    @IntDef({0, 90, 180, 270})
    @Retention(RetentionPolicy.SOURCE)
    @interface RotationDegreesValue {
    }
}
