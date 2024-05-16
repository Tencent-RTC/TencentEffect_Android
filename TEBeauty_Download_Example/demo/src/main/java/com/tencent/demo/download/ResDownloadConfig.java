package com.tencent.demo.download;

public class ResDownloadConfig {
    //---download_libs.zip
    //------libace_zplan.so
    //------liblight-sdk.so
    //------libtecodec.so
    //------libv8jni.so
    //------libYTCommonXMagic.so
    //
    public static final String DOWNLOAD_URL_LIBS_V8A = "https://YOUR_SERVER/arm64-v8a.zip";
    public static final String DOWNLOAD_URL_LIBS_V7A = "https://YOUR_SERVER/libs-v7a.zip";

    public static final String DOWNLOAD_MD5_LIBS_V8A = "MD5 value of the library zip file";
    public static final String DOWNLOAD_MD5_LIBS_V7A = "MD5 value of the library zip file";


    //---download_assets.zip
    //------Light3DPlugin
    //------LightCore
    //------LightHandPlugin
    //------LightSegmentPlugin
    public static final String DOWNLOAD_URL_ASSETS = "https://your_server/download_assets.zip";
    public static final String DOWNLOAD_MD5_ASSETS = "MD5 value of the zip file";



}
