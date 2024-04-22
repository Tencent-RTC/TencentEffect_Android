package com.tencent.demo.camera.camerax;

public interface CustomTextureProcessor {


    void onGLContextCreated();
    /**
     * 重写此方法实现自定义纹理处理流程
     *
     * @param textureId     待处理的纹理,
     * @param textureWidth  待处理的纹理宽度
     * @param textureHeight 待处理的纹理高度
     * @return 处理后的纹理, 如果不处理, 可直接返回入参的 textureId
     */
    int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight);

    void onGLContextDestroy();
}
