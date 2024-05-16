package com.tencent.demo.camera.glthread.textureChain;

import com.tencent.demo.camera.glrender.TextureCropper;


public class AfterBeautyCropTextureFilter implements TextureFilter {

    private boolean isCropTexture = false;
    private TextureCropper textureCropper = null;

    @Override
    public void init() {
        textureCropper = new TextureCropper();
    }


    public void setIsCropTexture(boolean isCrop) {
        this.isCropTexture = isCrop;
    }

    @Override
    public CameraXTexture processTexture(CameraXTexture cameraXTexture) {
        if (this.isCropTexture) {
            int resultId = textureCropper.cropTexture(cameraXTexture.textureId, cameraXTexture.width, cameraXTexture.height);
            return new CameraXTexture(resultId, cameraXTexture.width, cameraXTexture.height);
        } else {
            return cameraXTexture;
        }

    }

    @Override
    public void release() {
        this.textureCropper.release();
    }


}
