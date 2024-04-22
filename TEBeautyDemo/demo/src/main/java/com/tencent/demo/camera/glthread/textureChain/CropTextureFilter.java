package com.tencent.demo.camera.glthread.textureChain;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.tencent.demo.camera.glrender.CropRenderer;
import com.tencent.demo.utils.GlUtil;


public class CropTextureFilter implements TextureFilter {

    private int screenWidth;
    private int screenHeight;

    private CropRenderer cropRenderer;
    private int mCropTextureId = -1;
    private final float[] mCropMatrix = new float[16];

    private boolean isScreenSizeChanged = true;

    private int lastCameraWidth;
    private int lastCameraHeight;

    @Override
    public void init() {
        this.cropRenderer = new CropRenderer();
        this.cropRenderer.init();
    }


    public void setWidthAndHeight(int width, int height) {
        if (this.screenWidth != width || this.screenHeight != height) {
            this.isScreenSizeChanged = true;
        }
        this.screenWidth = width;
        this.screenHeight = height;
    }

    @Override
    public CameraXTexture processTexture(CameraXTexture cameraXTexture) {
        if (this.screenWidth <= 0 || this.screenHeight <= 0) {
            return cameraXTexture;
        }

        if (lastCameraWidth != cameraXTexture.width || lastCameraHeight != cameraXTexture.height) {
            isScreenSizeChanged = true;
        }
        this.lastCameraWidth = cameraXTexture.width;
        this.lastCameraHeight = cameraXTexture.height;
        if (isScreenSizeChanged) {
            this.isScreenSizeChanged = false;
            GlUtil.releaseTexture(mCropTextureId);
            mCropTextureId = GlUtil.createTexture(screenWidth, screenHeight, GLES20.GL_RGBA);
            computeCropMatrix(this.lastCameraWidth, this.lastCameraHeight, screenWidth, screenHeight, mCropMatrix);
        }
        this.cropRenderer.doRender(cameraXTexture.textureId, mCropTextureId, screenWidth, screenHeight, mCropMatrix, null);
        return new CameraXTexture(mCropTextureId, screenWidth, screenHeight);
    }

    @Override
    public void release() {
        this.cropRenderer.release();
    }


    private static void computeCropMatrix(int cameraWidth, int cameraHeight, int screenWidth, int screenHeight, float[] matrix) {
        float scaleX = 1;
        float scaleY = 1;
        float translateX = 0;
        float translateY = 0;
        Matrix.setIdentityM(matrix, 0);
        float cameraNormalizedW = cameraWidth * 1f / cameraHeight;
        float screenNormalizedW = screenWidth * 1f / screenHeight;
        float cameraNormalizedH = cameraHeight * 1f / cameraWidth;
        float screenNormalizedH = screenHeight * 1f / screenWidth;
        if (cameraNormalizedW > screenNormalizedW) {
            scaleX = screenNormalizedW / cameraNormalizedW;
            translateX = (cameraNormalizedW - screenNormalizedW) / 2;
        } else if (cameraNormalizedW < screenNormalizedW) {
            scaleY = screenNormalizedH / cameraNormalizedH;
            translateY = (cameraNormalizedH - screenNormalizedH) / 2;
        }

        Matrix.translateM(matrix, 0, translateX, translateY, 0);
        Matrix.scaleM(matrix, 0, scaleX, scaleY, 1);
    }
}
