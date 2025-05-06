package com.tencent.demo.opengl.view.process;

import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.view.CustomTextureProcessor;
import com.tencent.demo.opengl.view.FrameTexture;
import com.tencent.demo.opengl.view.display.Display;

public interface Processor {

    //set Display
    void setDisplay(Display display);

    // Sets the interface for custom texture processing.
    void setCustomTextureProcessor(CustomTextureProcessor customTextureProcessor);


    void process(EglCore eglCore, FrameTexture frameTexture);


    void onGLContextCreated(EglCore eglCore);


    void onGLContextDestroy();
}
