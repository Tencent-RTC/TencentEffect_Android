package com.tencent.demo;


public class AppConfig {

    public static final String SDK_COMBO_TYPE = "ALL";

    //custom segmentation
    public static final int TE_CHOOSE_PHOTO_SEG_CUSTOM = 2002;
    public static final int TE_CHOOSE_PHOTO_IMAGE_BEAUTY = 2003;


    public static String PICK_CONTENT_ALL = "image/*|video/*";
    public static String PICK_CONTENT_IMAGE = "image/*";
    public static String PICK_CONTENT_VIDEO = "video/*";

    private boolean isEnableHighPerformance = false;

    private AppConfig() {
    }

    private static class ClassHolder {
        static final AppConfig APP_CONFIG = new AppConfig();
    }

    public static AppConfig getInstance() {
        return ClassHolder.APP_CONFIG;
    }



    private String beautyFileDirName = "xmagic";




    public String getBeautyFileDirName() {
        return beautyFileDirName;
    }

    public void setBeautyFileDirName(String beautyFileDirName) {
        this.beautyFileDirName = beautyFileDirName;
    }

    public boolean isEnableHighPerformance() {
        return isEnableHighPerformance;
    }

    public void setEnableHighPerformance(boolean enableHighPerformance) {
        isEnableHighPerformance = enableHighPerformance;
    }
}
