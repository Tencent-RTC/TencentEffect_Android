package com.tencent.demo.opengl.view.display;

import android.view.View;

import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.view.FrameTexture;

public interface Display {

    // Notifies the Display when the GL thread is fully created. The Display should create a WindowSurface object within this method.
    void onGLContextCreated(EglCore eglCore);

    // Used for displaying textures on the screen.
    void display(EglCore eglCore, FrameTexture frameTexture);

    // Sets the state listener for the Display.
    void setDisplayStateChangeListener(DisplayStateChangeListener listener);


    void onGLContextDestroy();

    //Get the view of the display screen, usually TextureView or SurfaceView
    View getDisplayView();

}
