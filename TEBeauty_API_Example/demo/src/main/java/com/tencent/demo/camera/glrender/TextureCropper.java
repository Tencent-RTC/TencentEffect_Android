package com.tencent.demo.camera.glrender;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.tencent.demo.utils.GlUtil;

public class TextureCropper {

    public static final float cropRatio = 0.9f;  //To resize the texture to 90% of its original size
    private CropRenderer mCropEdgeRenderer;
    private int croppedTextureWidth = -1, croppedTextureHeight = -1;
    private final int[] croppedTextureId = new int[]{-1};
    private final float[] mCropMatrix = new float[16];

    public int cropTexture(int srcTextureId, int srcTextureWidth, int srcTextureHeight) {
        if (mCropEdgeRenderer == null) {
            mCropEdgeRenderer = new CropRenderer();
            mCropEdgeRenderer.init();
            croppedTextureId[0] = GlUtil.createTexture(srcTextureWidth, srcTextureHeight, GLES20.GL_RGBA);

            Matrix.setIdentityM(mCropMatrix, 0);
            float cut = 1.0f - cropRatio;

            Matrix.translateM(mCropMatrix, 0, cut / 2f, cut / 2, 0);
            Matrix.scaleM(mCropMatrix, 0, cropRatio, cropRatio, 1);
        } else if (srcTextureWidth != croppedTextureWidth || srcTextureHeight != croppedTextureHeight) {
            GLES20.glFinish();
            release();

            mCropEdgeRenderer = new CropRenderer();
            mCropEdgeRenderer.init();
            croppedTextureId[0] = GlUtil.createTexture(srcTextureWidth, srcTextureHeight, GLES20.GL_RGBA);
        }
        croppedTextureWidth = srcTextureWidth;
        croppedTextureHeight = srcTextureHeight;
        mCropEdgeRenderer.doRender(srcTextureId, croppedTextureId[0], srcTextureWidth, srcTextureHeight, mCropMatrix, null);
        return croppedTextureId[0];
    }


    public void release() {
        if (croppedTextureId[0] != -1) {
            GLES20.glDeleteTextures(croppedTextureId.length, croppedTextureId, 0);
            croppedTextureId[0] = -1;
        }
        if (mCropEdgeRenderer != null) {
            mCropEdgeRenderer.release();
            mCropEdgeRenderer = null;
        }
    }

}
