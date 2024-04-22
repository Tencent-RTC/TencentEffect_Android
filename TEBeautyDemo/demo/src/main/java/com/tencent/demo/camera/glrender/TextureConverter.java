package com.tencent.demo.camera.glrender;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.microedition.khronos.opengles.GL10;

/**

 TextureConversion class
 Mainly used to convert oes texture to rgba texture,
 Convert landscape texture to portrait texture
 Perform mirror processing on textures


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
     * This method is used to rotate and mirror process rgba textures.
     * The process involves clockwise rotation by rotation degrees (can be 0, 90, 180, 270), followed by horizontal flipping (flipHorizontal) and vertical flipping (flipVertical).
     * Use case: Some streaming SDKs may return landscape textures or the orientation of the subject in the frame may be incorrect.
     * However, Tencent effects SDK requires the subject in the texture to be facing forward. This method can be used to transform the texture accordingly.
     *
     * @return After rotation, please note: if rotating by 90 or 270 degrees, the width needs to be swapped with the height.
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
