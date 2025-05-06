package com.tencent.demo;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;
import com.google.android.play.core.splitcompat.SplitCompat;


public class TEApplication extends Application implements CameraXConfig.Provider {

    public static Context sApplicationContext = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        SplitCompat.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplicationContext = this;
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig()).build();
    }
}
