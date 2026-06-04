package com.tencent.effect.adapter.agora.api;

public interface IAgoraTextureProvider {

    void registerCustomTextureListener(AgoraCustomTextureListener textureListener);


    void removeCustomTextureListener();


}
