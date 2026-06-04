package com.tencent.effect.agora.demo.observer.converter;


import com.tencent.effect.agora.demo.observer.AgoraVideoFrameObserverHelper;

import io.agora.base.TextureBufferHelper;
import io.agora.base.VideoFrame;

/**
 * 声网的VideoFrame转换者
 * 用于转换纹理，等到rgba纹理
 */
public interface IAgoraTextureConverter {


    int covert(VideoFrame videoFrame, boolean captureMirror);


    void release();


    TextureBufferHelper getTextureBufferHelper();
}
