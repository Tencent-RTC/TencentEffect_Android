package com.tencent.demo.camera.camerax;

public interface CustomTextureProcessor {


    void onGLContextCreated();
    /**
     * Override this method to implement custom texture processing.
     * If not processed, the input textureId can be directly returned.
     */
    int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight);

    void onGLContextDestroy();
}
