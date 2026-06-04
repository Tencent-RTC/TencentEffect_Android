package com.tencent.effect.agora.demo.observer.converter;

import android.graphics.Bitmap;
import android.util.Log;

import com.tencent.effect.agora.demo.observer.AgoraVideoFrameObserverHelper;
import com.tencent.effect.agora.demo.utils.ImageTransform;
import com.tencent.effect.agora.demo.utils.TimeComputer;
import com.tencent.xmagic.GlUtil;

import java.nio.ByteBuffer;

import io.agora.base.TextureBufferHelper;
import io.agora.base.VideoFrame;
import io.agora.base.internal.video.YuvHelper;
import io.agora.rtc2.gl.EglBaseProvider;

/**
 * 将buffer 数据化转换为 texture id
 */
public class AgoraNV21BufferConverter implements IAgoraTextureConverter {

    private static final String TAG = AgoraNV21BufferConverter.class.getName();
    private TextureBufferHelper textureBufferHelper = null;
    private ImageTransform imageTransform;

    private ByteBuffer nv21ByteBuffer = null;
    private AgoraVideoFrameConvertListener videoFrameListener;
    private TimeComputer timeComputer = new TimeComputer(TAG);
    private final ImageTransform.Transition transition = new ImageTransform.Transition();

    public AgoraNV21BufferConverter(AgoraVideoFrameConvertListener listener) {
        this.videoFrameListener = listener;
        if (textureBufferHelper == null) {
            textureBufferHelper = TextureBufferHelper.create("tencentBeautyRender", EglBaseProvider.instance().getRootEglBase().getEglBaseContext());
            imageTransform = new ImageTransform();
            if (listener != null) {
                listener.onGLContextCreated();
            }
        }
    }



    @Override
    public TextureBufferHelper getTextureBufferHelper() {
        return this.textureBufferHelper;
    }


    @Override
    public int covert(VideoFrame videoFrame, boolean captureMirror) {

        return this.convertBuffer(videoFrame, captureMirror);
    }

    @Override
    public void release() {
        if (textureBufferHelper != null) {
            textureBufferHelper.invoke(() -> {
                this.doDestroy();
                return null;
            });
        }
    }


    private int convertBuffer(VideoFrame videoFrame, boolean captureMirror) {
        if (textureBufferHelper == null || this.imageTransform == null) {
            return -1;
        }
        timeComputer.startTime();
        byte[] nv21Buffer = this.getNV21Buffer(videoFrame);
        if (nv21Buffer == null) {
            return -1;
        }
        VideoFrame.Buffer buffer = videoFrame.getBuffer();
        boolean isFront = videoFrame.getSourceType() == VideoFrame.SourceType.kFrontCamera;


        return this.textureBufferHelper.invoke(() -> {

            int width = buffer.getHeight();
            int height = buffer.getWidth();

            int ySize = width * height;
            ByteBuffer yBuffer = ByteBuffer.allocateDirect(ySize);
            yBuffer.put(nv21Buffer, 0, ySize);
            yBuffer.position(0);
            ByteBuffer vuBuffer = ByteBuffer.allocateDirect(ySize / 2);
            vuBuffer.put(nv21Buffer, ySize, ySize / 2);
            vuBuffer.position(0);

            boolean mirror = isFront;
            if ((isFront && !captureMirror) || (!isFront && captureMirror)) {
                mirror = !mirror;
            }

            //转换为2d纹理
            int textureId = imageTransform.transferYUVToTexture(
                    yBuffer,
                    vuBuffer,
                    buffer.getWidth(),
                    buffer.getHeight(),
                    transition
            );


            boolean isHorizontal = videoFrame.getRotation() == 180 || videoFrame.getRotation() == 0;
            ImageTransform.Transition rotateTransition = new ImageTransform.Transition();
            if (isHorizontal && isFront) {
                rotateTransition.rotate(180 - videoFrame.getRotation());
            } else {
                rotateTransition.rotate(videoFrame.getRotation());
            }
            rotateTransition.flip(false, mirror);
            //进行角度和镜像变化
            int id = imageTransform.transferTextureToTexture(textureId, ImageTransform.TextureFormat.Texure2D, ImageTransform.TextureFormat.Texure2D, buffer.getWidth(), buffer.getHeight(), rotateTransition);
            timeComputer.endTime();

            int resultId = id;
            if (this.videoFrameListener != null) {
                if (isHorizontal) {
                    resultId = this.videoFrameListener.onProcessVideoFrame(id, height, width);
                } else {
                    resultId = this.videoFrameListener.onProcessVideoFrame(id, width, height);
                }
            }
            return resultId;
        });
    }

    private byte[] getNV21Buffer(VideoFrame videoFrame) {
        VideoFrame.Buffer buffer = videoFrame.getBuffer();
        VideoFrame.I420Buffer i420Buffer = null;
        if (buffer instanceof VideoFrame.I420Buffer) {
            i420Buffer = (VideoFrame.I420Buffer) buffer;
        } else {
            i420Buffer = buffer.toI420();
        }

        int width = i420Buffer.getWidth();
        int height = i420Buffer.getHeight();
        int nv21Size = (int) (width * height * 3.0f / 2.0f + 0.5f);
        if (nv21ByteBuffer == null || nv21ByteBuffer.capacity() != nv21Size) {
            if (nv21ByteBuffer != null) {
                nv21ByteBuffer.clear();
            }
            nv21ByteBuffer = ByteBuffer.allocateDirect(nv21Size);
            return null;
        }
        byte[] nv21ByteArray = new byte[nv21Size];

        YuvHelper.I420ToNV12(
                i420Buffer.getDataY(), i420Buffer.getStrideY(),
                i420Buffer.getDataV(), i420Buffer.getStrideV(),
                i420Buffer.getDataU(), i420Buffer.getStrideU(),
                nv21ByteBuffer, width, height
        );
        nv21ByteBuffer.position(0);
        nv21ByteBuffer.get(nv21ByteArray);
        if (!(buffer instanceof VideoFrame.I420Buffer)) {
            i420Buffer.release();
        }
        return nv21ByteArray;
    }


    private void doDestroy() {
        if (this.videoFrameListener != null) {
            this.videoFrameListener.onGLContextDestroy();
        }
        if (this.imageTransform != null) {
            this.imageTransform.release();
        }
        if (this.textureBufferHelper != null) {
            this.textureBufferHelper.dispose();
        }
        if (nv21ByteBuffer != null) {
            nv21ByteBuffer.clear();
            nv21ByteBuffer = null;
        }
    }
}
