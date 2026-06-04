package com.tencent.effect.agora.demo.utils;


import android.opengl.GLES20;
import io.agora.base.VideoFrame;
import io.agora.base.internal.video.GlRectDrawer;
import io.agora.base.internal.video.GlTextureFrameBuffer;
import io.agora.base.internal.video.RendererCommon.GlDrawer;

public class AgoraImageHelper {
    private GlTextureFrameBuffer glFrameBuffer;
    private GlDrawer drawer;

    public int transformTexture(int texId, VideoFrame.TextureBuffer.Type texType, int width, int height, float[] transform) {
        if (glFrameBuffer == null) {
            glFrameBuffer = new GlTextureFrameBuffer(GLES20.GL_RGBA);
        }
        GlTextureFrameBuffer frameBuffer = glFrameBuffer;
        if (drawer == null) {
            drawer = new GlRectDrawer();
        }

        frameBuffer.setSize(width, height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer.getFrameBufferId());
        if (texType == VideoFrame.TextureBuffer.Type.OES) {
            drawer.drawOes(texId, 0,transform, width, height, 0, 0, width, height);
        } else {
            drawer.drawRgb(texId, 0,transform, width, height, 0, 0, width, height);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glFinish();

        return frameBuffer.getTextureId();
    }

    public void release() {
        if (glFrameBuffer != null) {
            glFrameBuffer.release();
            glFrameBuffer = null;
        }
        if (drawer != null) {
            drawer.release();
            drawer = null;
        }
    }
}