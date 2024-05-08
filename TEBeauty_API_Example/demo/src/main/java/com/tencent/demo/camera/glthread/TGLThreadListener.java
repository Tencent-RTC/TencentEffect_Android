package com.tencent.demo.camera.glthread;

import android.graphics.SurfaceTexture;

import com.tencent.demo.camera.camerax.CustomTextureProcessor;

public interface TGLThreadListener extends CustomTextureProcessor {
    void onCreateSurfaceTexture(SurfaceTexture surfaceTexture);
}
