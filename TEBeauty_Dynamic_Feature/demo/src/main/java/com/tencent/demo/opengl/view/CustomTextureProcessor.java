package com.tencent.demo.opengl.view;

public interface CustomTextureProcessor {


    void onGLContextCreated();

    int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight);

    void onGLContextDestroy();
}
