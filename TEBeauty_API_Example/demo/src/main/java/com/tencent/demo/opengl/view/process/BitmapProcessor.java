package com.tencent.demo.opengl.view.process;

import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.render.TextureFormat;
import com.tencent.demo.opengl.view.CustomTextureProcessor;
import com.tencent.demo.opengl.view.FrameTexture;
import com.tencent.demo.opengl.view.display.Display;

public class BitmapProcessor implements Processor {
    private CustomTextureProcessor customTextureProcessor = null;
    private Display display = null;

    public void setCustomTextureProcessor(CustomTextureProcessor customTextureProcessor) {
        this.customTextureProcessor = customTextureProcessor;
    }


    public void setDisplay(Display display) {
        this.display = display;
    }

    @Override
    public void onGLContextCreated(EglCore eglCore) {
        if (customTextureProcessor != null) {
            customTextureProcessor.onGLContextCreated();
        }
    }

    @Override
    public void process(EglCore eglCore, FrameTexture frameTexture) {
        int desTextureId = frameTexture.textureId;
        if (customTextureProcessor != null) {
            desTextureId = customTextureProcessor.onCustomProcessTexture(desTextureId, frameTexture.textureWidth, frameTexture.textureHeight);
        }
        FrameTexture outTexture = new FrameTexture(desTextureId, frameTexture.textureWidth, frameTexture.textureHeight);
        outTexture.textureFormat = TextureFormat.Texture_2D;
        if (this.display != null) {
            this.display.display(eglCore, outTexture);
        }
    }

    @Override
    public void onGLContextDestroy() {
        if (customTextureProcessor != null) {
            customTextureProcessor.onGLContextDestroy();
        }
    }
}
