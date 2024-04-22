package com.tencent.demo.camera.glthread.textureChain;

public interface TextureFilter {


    public void init();
    public CameraXTexture processTexture(CameraXTexture cameraXTexture);

    public void release();

}
