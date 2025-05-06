package com.tencent.demo.opengl.view;


import com.tencent.demo.opengl.render.TextureFormat;

public class FrameTexture {
    public TextureFormat textureFormat = TextureFormat.Texture_2D;
    public int textureId = -1;
    public int textureWidth = 0;
    public int textureHeight = 0;
    public boolean isFrontCamera = true;

    public FrameTexture() {
    }

    public FrameTexture(int textureId, int textureWidth, int textureHeight) {
        this.textureId = textureId;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }
}
