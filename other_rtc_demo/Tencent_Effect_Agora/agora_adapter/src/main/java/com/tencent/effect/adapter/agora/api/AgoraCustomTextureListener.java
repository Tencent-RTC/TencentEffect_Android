package com.tencent.effect.adapter.agora.api;

public interface AgoraCustomTextureListener {
    void onCameraChange(boolean isFrontCamera);

    void onGLContextCreated();

    int onProcessVideoFrame(int textureId, int width, int height);

    void onGLContextDestroy();
}
