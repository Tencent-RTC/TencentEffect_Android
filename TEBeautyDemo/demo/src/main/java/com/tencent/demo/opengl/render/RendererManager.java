package com.tencent.demo.opengl.render;



public class RendererManager {


    private Texture2DRenderer mProgramTexture2D;
    private TextureOesRenderer mProgramTextureOES;


    public Renderer getRenderer(TextureFormat srcTextureFormat) {
        switch (srcTextureFormat) {
            case Texture_2D:
                if (null == mProgramTexture2D) {
                    mProgramTexture2D = new Texture2DRenderer();
                }
                return mProgramTexture2D;
            case Texture_OES:
                if (null == mProgramTextureOES) {
                    mProgramTextureOES = new TextureOesRenderer();
                }
                return mProgramTextureOES;
            default:
                return null;
        }
    }


    public void release() {
        if (null != mProgramTexture2D) {
            mProgramTexture2D.release();
            mProgramTexture2D = null;
        }
        if (null != mProgramTextureOES) {
            mProgramTextureOES.release();
            mProgramTextureOES = null;
        }
    }
}
