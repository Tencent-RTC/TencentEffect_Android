package com.tencent.effect.agora.demo.observer;


import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.effect.adapter.agora.api.AgoraCustomTextureListener;
import com.tencent.effect.adapter.agora.api.IAgoraTextureProvider;

import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngine;

public class AgoraTextureProvider implements IAgoraTextureProvider,
        AgoraVideoFrameObserver.LocalRenderMirrorChangeListener {

    private static final String TAG = AgoraTextureProvider.class.getName();
    private AgoraVideoFrameObserver agoraVideoFrameObserver = null;
    private RtcEngine agoraEngine;
    private AgoraCustomTextureListener lastListener = null;


    public void setAgoraEngine(RtcEngine agoraEngine) {
        this.agoraEngine = agoraEngine;
    }


    @Override
    public void registerCustomTextureListener(@NonNull AgoraCustomTextureListener textureListener) {
        if (agoraEngine == null) {
            return;
        }
        if (agoraVideoFrameObserver != null) {
            Log.e(TAG, "agoraVideoFrameObserver is not null, you need ");
            return;
        }
        lastListener = textureListener;
        agoraVideoFrameObserver = new AgoraVideoFrameObserver();
        agoraVideoFrameObserver.setRenderMirrorChangeListener(this);
        this.agoraEngine.registerVideoFrameObserver(agoraVideoFrameObserver);
        agoraVideoFrameObserver.setVideoFrameListener(new AgoraVideoFrameObserverListener() {
            @Override
            public void onCameraChange(boolean isFrontCamera) {
                lastListener.onCameraChange(isFrontCamera);
            }

            @Override
            public void onGLContextCreated() {
                lastListener.onGLContextCreated();
            }

            @Override
            public int onProcessVideoFrame(int textureId, int width, int height) {
                if (lastListener != null) {
                    return lastListener.onProcessVideoFrame(textureId, width, height);
                }
                return textureId;
            }

            @Override
            public void onGLContextDestroy() {
                lastListener.onGLContextDestroy();
            }
        });
    }


    @Override
    public void removeCustomTextureListener() {
        agoraVideoFrameObserver.release();
        agoraVideoFrameObserver = null;
    }


    @Override
    public void onLocalRenderMirrorChange(boolean localRenderMirror) {
        if (this.agoraEngine == null) {
            return;
        }
        this.agoraEngine.setLocalRenderMode(Constants.RENDER_MODE_HIDDEN,
                localRenderMirror ? Constants.VIDEO_MIRROR_MODE_ENABLED : Constants.VIDEO_MIRROR_MODE_DISABLED);
    }
}
