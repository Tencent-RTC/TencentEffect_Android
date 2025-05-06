package com.tencent.demo.opengl.view.process;

import android.util.Size;
import android.widget.ImageView;

import com.tencent.demo.opengl.TextureTransform;
import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.render.TextureFormat;
import com.tencent.demo.opengl.view.CustomTextureProcessor;
import com.tencent.demo.opengl.view.FrameTexture;
import com.tencent.demo.opengl.view.display.Display;

public class CameraProcessor implements Processor {

    private int mSurfaceWidth = -1;
    private int mSurfaceHeight = -1;
    private float cropRatio = 1f;  //美颜之后的裁剪比例

    private CustomTextureProcessor customTextureProcessor = null;
    private TextureTransform textureTransform = null;

    private Display display = null;

    public void setCropRatio(float cropRatio) {
        this.cropRatio = cropRatio;
    }

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


    /**
     * Updates the size of the monitor. The size of the monitor is used in the process method to scale the texture size according to the monitor's dimensions.
     * @param size
     */
    public void updateSurfaceSize(Size size) {
        this.mSurfaceWidth = size.getWidth();
        this.mSurfaceHeight = size.getHeight();
    }

    @Override
    public void process(EglCore eglCore, FrameTexture frameTexture) {
        int mCameraWidth = frameTexture.textureWidth;
        int mCameraHeight = frameTexture.textureHeight;
        boolean isFrontCamera = frameTexture.isFrontCamera;

        int angle = isFrontCamera ? 270 : 90;
        //设置的顺序是 裁剪、镜像、旋转，执行的顺序为旋转、镜像、裁剪
        // Although the settings are applied in the order of cropping, mirroring, and rotation, the actual execution order is rotation, mirroring, then cropping.
        TextureTransform.Transition transition = new TextureTransform.Transition();
        transition.crop(ImageView.ScaleType.CENTER_CROP, angle, mCameraWidth, mCameraHeight, mSurfaceWidth, mSurfaceHeight);
        Size croppedSize = transition.getCroppedSize();
        int textureWidth = croppedSize.getWidth();
        int textureHeight = croppedSize.getHeight();
        if (isFrontCamera) {
            transition.flip(true, false);
        }
        transition.rotate(angle);
        if (textureTransform == null) {
            textureTransform = new TextureTransform();
        }
        int desTextureId = textureTransform.transferTextureToTexture(frameTexture.textureId, frameTexture.textureFormat, TextureFormat.Texture_2D,
                mCameraWidth, mCameraHeight, transition);
        if (customTextureProcessor != null) {
            desTextureId = customTextureProcessor.onCustomProcessTexture(desTextureId, textureWidth, textureHeight);
        }
        FrameTexture outTexture = new FrameTexture();
        outTexture.isFrontCamera = frameTexture.isFrontCamera;
        outTexture.textureWidth = croppedSize.getWidth();
        outTexture.textureHeight = croppedSize.getHeight();

        if (cropRatio < 1f && cropRatio > 0) {
            TextureTransform.Transition cropTransition = new TextureTransform.Transition();
            cropTransition.crop(ImageView.ScaleType.CENTER, 0, textureWidth, textureHeight, (int) (textureWidth * cropRatio), (int) (textureHeight * cropRatio));
            desTextureId = textureTransform.transferTextureToTexture(desTextureId, TextureFormat.Texture_2D, TextureFormat.Texture_2D, mSurfaceWidth, mSurfaceHeight, cropTransition);
            outTexture.textureWidth = (int) (textureWidth * cropRatio);
            outTexture.textureHeight = (int) (textureHeight * cropRatio);
        }

        outTexture.textureId = desTextureId;
        this.display.display(eglCore, outTexture);

    }

    @Override
    public void onGLContextDestroy() {
        if (customTextureProcessor != null) {
            customTextureProcessor.onGLContextDestroy();
        }
        if (textureTransform != null) {
            textureTransform.release();
        }
    }


}
