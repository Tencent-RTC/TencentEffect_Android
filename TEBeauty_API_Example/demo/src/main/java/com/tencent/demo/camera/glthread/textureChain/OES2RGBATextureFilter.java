package com.tencent.demo.camera.glthread.textureChain;


import com.tencent.demo.camera.glrender.TextureConverter;

public class OES2RGBATextureFilter implements TextureFilter {


    private TextureConverter textureConverter = null;


    @Override
    public void init() {
        this.textureConverter = new TextureConverter();
    }

    @Override
    public CameraXTexture processTexture(CameraXTexture cameraXTexture) {
        int rgbaId = this.textureConverter.oes2Rgba(cameraXTexture.textureId, cameraXTexture.width, cameraXTexture.height);
        return new CameraXTexture(rgbaId, cameraXTexture.width, cameraXTexture.height);
    }

    @Override
    public void release() {
        this.textureConverter.release();

    }




}
