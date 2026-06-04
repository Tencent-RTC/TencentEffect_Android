package com.tencent.effect.agora.demo.observer.converter;

public interface AgoraVideoFrameConvertListener {

      void onCameraChange(boolean isFrontCamera);
      void onGLContextCreated();

      int onProcessVideoFrame(int textureId,int width,int height);

      void onGLContextDestroy();
}
