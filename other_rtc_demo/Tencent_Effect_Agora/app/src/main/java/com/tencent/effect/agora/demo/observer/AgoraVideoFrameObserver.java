package com.tencent.effect.agora.demo.observer;


import android.graphics.Matrix;

import com.tencent.effect.agora.demo.observer.converter.AgoraNV21BufferConverter;
import com.tencent.effect.agora.demo.observer.converter.AgoraTextureBufferConverter;
import com.tencent.effect.agora.demo.observer.converter.AgoraVideoFrameConvertListener;
import com.tencent.effect.agora.demo.observer.converter.IAgoraTextureConverter;

import io.agora.base.TextureBuffer;
import io.agora.base.VideoFrame;
import io.agora.rtc2.video.IVideoFrameObserver;

public class AgoraVideoFrameObserver implements IVideoFrameObserver, AgoraVideoFrameConvertListener {

    private static final String TAG = AgoraVideoFrameObserver.class.getName();
    private IAgoraTextureConverter agoraTextureConverter = null;
    private AgoraVideoFrameObserverListener videoFrameListener = null;
    private LocalRenderMirrorChangeListener localRenderMirrorChangeListener = null;
    private boolean isFrontCamera = true;
    private boolean captureMirror = true;
    private boolean renderMirror = false;
    private int skipFrame = 0;

    private final AgoraVideoFrameObserverHelper.CameraConfig cameraConfig = new AgoraVideoFrameObserverHelper.CameraConfig();


    public AgoraVideoFrameObserver() {

    }

    public AgoraVideoFrameObserver(AgoraVideoFrameObserverListener listener) {
        this.videoFrameListener = listener;
    }


    public void setVideoFrameListener(AgoraVideoFrameObserverListener videoFrameListener) {
        this.videoFrameListener = videoFrameListener;
    }

    @Override
    public boolean onCaptureVideoFrame(int sourceType, VideoFrame videoFrame) {
        if (videoFrame == null) {
            return false;
        }
        return processBeauty(videoFrame);
    }

    private boolean processBeauty(VideoFrame videoFrame) {
        boolean[] mirrorArray = AgoraVideoFrameObserverHelper.getCaptureAndRenderMirrorState(isFrontCamera, cameraConfig);
        boolean cMirror = mirrorArray[0];
        boolean rMirror = mirrorArray[1];

        if (captureMirror != cMirror || renderMirror != rMirror) {
            captureMirror = cMirror;
            if (renderMirror != rMirror) {
                renderMirror = rMirror;
                if (this.localRenderMirrorChangeListener != null) {
                    this.localRenderMirrorChangeListener.onLocalRenderMirrorChange(renderMirror);
                }
            }
            skipFrame = 2;
            return false;
        }

        boolean oldIsFrontCamera = isFrontCamera;
        isFrontCamera = videoFrame.getSourceType() == VideoFrame.SourceType.kFrontCamera;
        if (oldIsFrontCamera != isFrontCamera) {
            this.onCameraChange(isFrontCamera);
            return false;
        }


        if (this.agoraTextureConverter == null) {
            if (videoFrame.getBuffer() instanceof TextureBuffer) {
                this.agoraTextureConverter = new AgoraTextureBufferConverter(this);
            } else {
                //只有宽高都小于256的时候才会走这里
                this.agoraTextureConverter = new AgoraNV21BufferConverter(this);
            }
        }
        int processTexId = this.agoraTextureConverter.covert(videoFrame, this.captureMirror);

        if (processTexId < 0) {
            return false;
        }

        if (skipFrame > 0) {
            skipFrame--;
            return false;
        }
        Matrix matrix = new Matrix();
        matrix.preTranslate(0.5f, 0.5f);
        matrix.preScale(1.0f, -1.0f);
        matrix.preTranslate(-0.5f, -0.5f);


        VideoFrame.TextureBuffer processBuffer = this.agoraTextureConverter.getTextureBufferHelper().wrapTextureBuffer(
                videoFrame.getRotatedWidth(),
                videoFrame.getRotatedHeight(),
                TextureBuffer.Type.RGB,
                processTexId,
                matrix
        );
        if (processBuffer == null) {
            return false;
        }
        videoFrame.replaceBuffer(processBuffer, 0, videoFrame.getTimestampNs());
        return true;
    }


    @Override
    public boolean onPreEncodeVideoFrame(int sourceType, VideoFrame videoFrame) {
        return false;
    }

    @Override
    public boolean onMediaPlayerVideoFrame(VideoFrame videoFrame, int mediaPlayerId) {
        return false;
    }

    @Override
    public boolean onRenderVideoFrame(String channelId, int uid, VideoFrame videoFrame) {
        return false;
    }

    @Override
    public int getVideoFrameProcessMode() {
        return IVideoFrameObserver.PROCESS_MODE_READ_WRITE;
    }

    @Override
    public int getVideoFormatPreference() {
        return IVideoFrameObserver.VIDEO_PIXEL_DEFAULT;
    }

    @Override
    public boolean getRotationApplied() {
        return false;
    }

    @Override
    public boolean getMirrorApplied() {
        return captureMirror;
    }

    @Override
    public int getObservedFramePosition() {
        return IVideoFrameObserver.POSITION_POST_CAPTURER;
    }

    @Override
    public void onCameraChange(boolean isFrontCamera) {
        if (this.videoFrameListener != null) {
            this.videoFrameListener.onCameraChange(isFrontCamera);
        }
    }

    @Override
    public void onGLContextCreated() {
        if (this.videoFrameListener != null) {
            this.videoFrameListener.onGLContextCreated();
        }
    }

    @Override
    public int onProcessVideoFrame(int textureId, int width, int height) {
        if (this.videoFrameListener != null) {
            return this.videoFrameListener.onProcessVideoFrame(textureId, width, height);
        }
        return textureId;
    }

    @Override
    public void onGLContextDestroy() {
        if (this.videoFrameListener != null) {
            this.videoFrameListener.onGLContextDestroy();
        }
    }


    public void setRenderMirrorChangeListener(LocalRenderMirrorChangeListener localRenderMirrorChangeListener) {
        this.localRenderMirrorChangeListener = localRenderMirrorChangeListener;
    }


    public void release() {
        if (this.agoraTextureConverter != null) {
            this.agoraTextureConverter.release();
            this.agoraTextureConverter = null;
            isFrontCamera = true;
            captureMirror = true;
            renderMirror = false;
            skipFrame = 0;
        }
    }

    public interface LocalRenderMirrorChangeListener {
        void onLocalRenderMirrorChange(boolean localRenderMirror);
    }


}
