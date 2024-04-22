package com.tencent.demo;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;

import com.tencent.demo.constant.LicenseConstant;
import com.tencent.effect.beautykit.TEBeautyKit;

import java.io.File;


public class TEApplication extends Application implements CameraXConfig.Provider {

    public static Context sApplicationContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplicationContext = this;
        TEBeautyKit.setTELicense(sApplicationContext, LicenseConstant.mXMagicLicenceUrl, LicenseConstant.mXMagicKey, null);
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig()).build();
    }
}
