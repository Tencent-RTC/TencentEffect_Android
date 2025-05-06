package com.tencent.demo.opengl;

import androidx.annotation.IntDef;

import com.tencent.demo.opengl.render.TextureFormat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 纹理转换类
 * 主要用于将oes纹理转换为rgba纹理，
 * 横屏纹理转换为竖屏纹理
 * 对纹理进行镜像处理
 */
public class TextureConverter {

    private TextureTransform mYUV2RGBATransform;
    private final TextureTransform.Transition defaultTransition = new TextureTransform.Transition();

    private TextureTransform rotateTransform;


    /**
     * 此方法用于将oes纹理转换为rgba纹理
     *
     * @param srcID  oes 纹理
     * @param width  纹理宽度
     * @param height 纹理高度
     * @return rgba纹理ID
     */
    public int oes2Rgba(int srcID, int width, int height) {
        if (mYUV2RGBATransform == null) {
            mYUV2RGBATransform = new TextureTransform();
        }
        return mYUV2RGBATransform.transferTextureToTexture(srcID, TextureFormat.Texture_OES, TextureFormat.Texture_2D, width, height, defaultTransition);
    }


    /**
     * 此方法用于对rgba纹理进行旋转和镜像处理。处理过程为：先顺时针旋转rotation度（可取值0,90,180，270），再进行左右翻转(flipHorizontal）和 上下翻转（flipVertical）。
     * 使用场景：某些推流SDK返回的纹理是横屏纹理或者画面中人物朝向不对，而腾讯特效SDK要求纹理中的人物是正向的，所以可以通过此方法对纹理进行转换。
     *
     * @param srcID    rgba纹理
     * @param width    纹理宽度
     * @param height   纹理高度
     * @param rotation 需要进行旋转的角度。
     * @return 旋转后的纹理，注意：如果旋转90或者270度，那么宽度需要进行交换。
     */
    public int convert(int srcID, int width, int height, @RotationDegreesValue int rotation, boolean flipVertical, boolean flipHorizontal) {
        if (srcID < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("please check srcID ,width , height");
        }
        TextureTransform.Transition convertTransition = new TextureTransform.Transition();
        convertTransition.flip(flipHorizontal, flipVertical);
        convertTransition.rotate(rotation);
        if (this.rotateTransform == null) {
            this.rotateTransform = new TextureTransform();
        }
        return this.rotateTransform.transferTextureToTexture(srcID, TextureFormat.Texture_2D, TextureFormat.Texture_2D, width, height, convertTransition);
    }


    private void releaseOes() {
        if (mYUV2RGBATransform != null) {
            mYUV2RGBATransform.release();
            mYUV2RGBATransform = null;
        }
    }

    private void releaseConvert() {
        if (rotateTransform != null) {
            rotateTransform.release();
            rotateTransform = null;
        }
    }


    public void release() {
        this.releaseOes();
        this.releaseConvert();
    }


    @IntDef({0, 90, 180, 270})
    @Retention(RetentionPolicy.SOURCE)
    @interface RotationDegreesValue {
    }
}
