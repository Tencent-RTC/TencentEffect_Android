package com.tencent.demo.beauty.model;

import android.util.Size;

import com.tencent.demo.beauty.view.TETitleBar;
import com.tencent.demo.opengl.view.CameraSize;

public class TEFeatureConfig {
    //当前分辨率
    public TETitleBar.RESOLUTION resolution = TETitleBar.RESOLUTION.R1080P;
    //人脸遮挡检测开关
    public boolean isOnFaceBlockSwitch = true;
    //性能参数开关
    public boolean isOnPerformanceSwitch = false;
    //增强模式开关
    public boolean isOnEnhancedMode = false;
    //智能美颜开关
    public boolean isOnSmartBeautySwitch = false;
    //美白仅对皮肤生效开关
    public boolean isOnWhiteSkinOnlySwitch = false;
    //画面裁剪开关
    public boolean isOnCropTextureSwitch = false;


    //是否展示增强模式开关
    public boolean isShowEnhancedModeSwitch = true;

    //是否展示美白仅对皮肤生效开关
    public boolean isShowWhiteSkinOnlySwitch = true;

    static class ClassHolder {
        static final TEFeatureConfig TE_FEATURE_CONFIG = new TEFeatureConfig();
    }

    public static TEFeatureConfig getInstance() {
        return ClassHolder.TE_FEATURE_CONFIG;
    }


    public void reset() {
        resolution = TETitleBar.RESOLUTION.R1080P;
        //是否展示增强模式开关
        isShowEnhancedModeSwitch = true;
        //是否展示美白仅对皮肤生效开关
        isShowWhiteSkinOnlySwitch = true;

        isOnFaceBlockSwitch = true;
        //性能参数开关
        isOnPerformanceSwitch = false;
        //增强模式开关
        isOnEnhancedMode = false;
        //智能美颜开关
        isOnSmartBeautySwitch = false;
        //美白仅对皮肤生效开关
        isOnWhiteSkinOnlySwitch = false;
        //画面裁剪开关
        isOnCropTextureSwitch = false;
    }


    /**
     * 隐藏部分按钮的开关，
     */
    public void hidePartFeatureSwitch() {
        resolution = TETitleBar.RESOLUTION.R1080P;
        //是否展示增强模式开关
        isShowEnhancedModeSwitch = false;
        //是否展示美白仅对皮肤生效开关
        isShowWhiteSkinOnlySwitch = false;

        isOnFaceBlockSwitch = true;
        //性能参数开关
        isOnPerformanceSwitch = false;
        //增强模式开关
        isOnEnhancedMode = false;
        //智能美颜开关
        isOnSmartBeautySwitch = false;
        //美白仅对皮肤生效开关
        isOnWhiteSkinOnlySwitch = false;
        //画面裁剪开关
        isOnCropTextureSwitch = false;
    }


    public Size getCameraSize() {
        switch (resolution) {
            case R540P:
                return CameraSize.size540;
            case R720P:
                return CameraSize.size720;
            default:
                return CameraSize.size1080;
        }
    }



}
