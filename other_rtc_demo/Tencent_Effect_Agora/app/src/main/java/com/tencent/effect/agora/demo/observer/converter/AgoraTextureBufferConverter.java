package com.tencent.effect.agora.demo.observer.converter;

import android.graphics.Matrix;
import android.util.Log;

import com.tencent.effect.agora.demo.observer.AgoraVideoFrameObserverHelper;
import com.tencent.effect.agora.demo.utils.AgoraImageHelper;
import com.tencent.effect.agora.demo.utils.TimeComputer;

import io.agora.base.TextureBuffer;
import io.agora.base.TextureBufferHelper;
import io.agora.base.VideoFrame;
import io.agora.base.internal.video.RendererCommon;
import io.agora.rtc2.gl.EglBaseProvider;

/**
 * 用于将textureBuffer转换为texture id
 */
public class AgoraTextureBufferConverter implements IAgoraTextureConverter {
    private static final String TAG = AgoraTextureBufferConverter.class.getName();


    private AgoraImageHelper agoraImageHelper;
    private TextureBufferHelper textureBufferHelper = null;

    private AgoraVideoFrameConvertListener videoFrameListener;

    private TimeComputer timeComputer = new TimeComputer(TAG);

    public AgoraTextureBufferConverter(AgoraVideoFrameConvertListener listener) {
        this.videoFrameListener = listener;
        if (textureBufferHelper == null) {
            textureBufferHelper = TextureBufferHelper.create("tencentBeautyRender", EglBaseProvider.instance().getRootEglBase().getEglBaseContext());
            agoraImageHelper = new AgoraImageHelper();
            if (listener != null) {
                listener.onGLContextCreated();
            }
        }
    }


    @Override
    public int covert(VideoFrame videoFrame, boolean captureMirror) {
        return this.convertTextureBuffer(videoFrame, captureMirror);
    }

    @Override
    public void release() {
        if (textureBufferHelper != null) {
            //TODO 这个方法可能会阻塞调用线程，所以此方法需要在子线程调用
            long startTime = System.currentTimeMillis();
            textureBufferHelper.invoke(() -> {
                this.doDestroy();
                return null;
            });
            Log.d("打印日志信息","textureBufferHelper.invoke 耗时 = "+ (System.currentTimeMillis() -startTime));
        }
    }


    @Override
    public TextureBufferHelper getTextureBufferHelper() {
        return this.textureBufferHelper;
    }


    private int convertTextureBuffer(VideoFrame videoFrame, boolean captureMirror) {
        if (textureBufferHelper == null || agoraImageHelper == null) {
            return -1;
        }
        this.timeComputer.startTime();
        TextureBuffer buffer = (TextureBuffer) videoFrame.getBuffer();
        boolean isFront = videoFrame.getSourceType() == VideoFrame.SourceType.kFrontCamera;


        boolean isFrontCamera = videoFrame.getSourceType() == VideoFrame.SourceType.kFrontCamera;

        return this.textureBufferHelper.invoke(() -> {
            boolean mirror = isFront;
            if ((isFrontCamera && !captureMirror) || (!isFrontCamera && captureMirror)) {
                mirror = !mirror;
            }

            Matrix renderMatrix = new Matrix();
            renderMatrix.preTranslate(0.5f, 0.5f);
            renderMatrix.preRotate(videoFrame.getRotation());
            renderMatrix.preScale(mirror ? -1.0f : 1.0f, -1.0f);
            renderMatrix.preTranslate(-0.5f, -0.5f);
            Matrix finalMatrix = new Matrix(buffer.getTransformMatrix());
            finalMatrix.preConcat(renderMatrix);

            float[] transform = RendererCommon.convertMatrixFromAndroidGraphicsMatrix(finalMatrix);

            int width = 0;
            int height = 0;
            if (videoFrame.getRotation() == 180 || videoFrame.getRotation() == 0) {  //横屏
                width = buffer.getWidth();
                height = buffer.getHeight();
            } else {
                width = buffer.getHeight();
                height = buffer.getWidth();
            }

            int textureId = agoraImageHelper.transformTexture(
                    buffer.getTextureId(),
                    buffer.getType(),
                    width,
                    height,
                    transform
            );


            int resultId = textureId;
            if (this.videoFrameListener != null) {
                resultId = this.videoFrameListener.onProcessVideoFrame(textureId, width, height);
            }
            timeComputer.endTime();
            return resultId;
        });
    }


    private void doDestroy() {
        if (this.videoFrameListener != null) {
            this.videoFrameListener.onGLContextDestroy();
        }
        if (this.agoraImageHelper != null) {
            this.agoraImageHelper.release();
            this.agoraImageHelper = null;
        }
        if (this.textureBufferHelper != null) {
            this.textureBufferHelper.dispose();
            this.textureBufferHelper = null;
        }
    }


}
