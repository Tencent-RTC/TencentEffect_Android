package com.tencent.demo.camera.glthread.textureChain;

import com.tencent.demo.camera.glthread.TGLThreadListener;

public class BeautyTextureFilter implements TextureFilter {

    private TGLThreadListener listener = null;

    public BeautyTextureFilter(TGLThreadListener listener) {
        this.listener = listener;
    }

    @Override
    public void init() {
        this.listener.onGLContextCreated();
    }

    @Override
    public CameraXTexture processTexture(CameraXTexture cameraXTexture) {
        int processedId = this.listener.onCustomProcessTexture(cameraXTexture.textureId, cameraXTexture.width, cameraXTexture.height);
        return new CameraXTexture(processedId, cameraXTexture.width, cameraXTexture.height);
    }

    @Override
    public void release() {
        this.listener.onGLContextDestroy();
    }


}
