package com.tencent.effect.adapter.agora.impl;


import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

import android.util.Log;

import com.tencent.effect.adapter.agora.api.DeviceDirection;
import com.tencent.effect.adapter.agora.api.ITEBeautyAdapter;
import com.tencent.xmagic.GlUtil;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.bean.TEImageOrientation;

import org.light.utils.LightLogUtil;


public class TextureProcessor {


    private static final String TAG = TextureProcessor.class.getName();


    private XmagicApi xmagicApi = null;

    private boolean isFrontCamera = true;

    private int mScreenOrientation = SCREEN_ORIENTATION_PORTRAIT;

    private ITEBeautyAdapter.AdapterEffectState effectState = ITEBeautyAdapter.AdapterEffectState.ENABLED;
    private boolean additionalProcess = false;

    public void setScreenOrientation(@ITEBeautyAdapter.ScreenOrientation int screenOrientation) {
        this.mScreenOrientation = screenOrientation;
    }

    public void setBeautyApi(XmagicApi xmagicApi) {
        this.xmagicApi = xmagicApi;
    }

    public int rotateAndProcessTexture(int textureId, int width, int height, boolean isFrontCamera) {
        this.isFrontCamera = isFrontCamera;
        if (this.xmagicApi == null || this.effectState != ITEBeautyAdapter.AdapterEffectState.ENABLED) {
            additionalProcess = true;
            return textureId;
        }
        if (additionalProcess) {
            additionalProcess = false;
            this.xmagicApi.process(textureId, width, height);
        }
        return this.xmagicApi.process(textureId, width, height);
    }


    public void onDestroy() {

    }


    /**
     * @param screenOrientation      手机方向
     * @param isFrontCamera          是否是前置摄像头
     * @param currentDeviceDirection 当前手机的方向，此数据是由Sensor传感器传入
     */
    public void setImageOrientation(@ITEBeautyAdapter.ScreenOrientation int screenOrientation, boolean isFrontCamera, DeviceDirection currentDeviceDirection) {
        if (this.xmagicApi == null) {
            return;
        }
        if (currentDeviceDirection == DeviceDirection.HORIZONTAL_UP || currentDeviceDirection == DeviceDirection.HORIZONTAL_DOWN) {  //表示手机当前是水平朝上放置
//            xmagicApi.setImageOrientation(TEImageOrientation.ROTATION_0);
            return;
        }
        this.isFrontCamera = isFrontCamera;
        TEImageOrientation teImageOrientation = this.getOrientation(screenOrientation, currentDeviceDirection);
        Log.e(TAG, "teImageOrientation = " + teImageOrientation.name());
        xmagicApi.setImageOrientation(teImageOrientation);
    }

    public void notifyEffectStateChanged(ITEBeautyAdapter.AdapterEffectState effectState) {
        this.effectState = effectState;
        if (this.xmagicApi != null) {
            if (effectState == ITEBeautyAdapter.AdapterEffectState.ENABLED) {
                this.xmagicApi.onResume();
            } else {
                this.xmagicApi.onPause();
            }
        }
    }

    /**
     * @param screenOrientation      屏幕方向
     * @param currentDeviceDirection 当前手机的方向，此数据是由Sensor传感器传入
     * @return
     */
    private TEImageOrientation getOrientation(@ITEBeautyAdapter.ScreenOrientation int screenOrientation, DeviceDirection currentDeviceDirection) {
        LightLogUtil.d(TAG, "getOrientation =  " + currentDeviceDirection.angle);
        if (this.isFrontCamera) {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_0;
                    case 90:
                        return TEImageOrientation.ROTATION_90;
                    case 180:
                        return TEImageOrientation.ROTATION_180;
                    case 270:
                        return TEImageOrientation.ROTATION_270;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_180;
                    case 90:
                        return TEImageOrientation.ROTATION_270;
                    case 180:
                        return TEImageOrientation.ROTATION_0;
                    case 270:
                        return TEImageOrientation.ROTATION_90;
                }

            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_90;
                    case 90:
                        return TEImageOrientation.ROTATION_180;
                    case 180:
                        return TEImageOrientation.ROTATION_270;
                    case 270:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_270;
                    case 90:
                        return TEImageOrientation.ROTATION_0;
                    case 180:
                        return TEImageOrientation.ROTATION_90;
                    case 270:
                        return TEImageOrientation.ROTATION_180;
                }
            }
        } else {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_0;
                    case 90:
                        return TEImageOrientation.ROTATION_270;
                    case 180:
                        return TEImageOrientation.ROTATION_180;
                    case 270:
                        return TEImageOrientation.ROTATION_90;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_180;
                    case 90:
                        return TEImageOrientation.ROTATION_90;
                    case 180:
                        return TEImageOrientation.ROTATION_0;
                    case 270:
                        return TEImageOrientation.ROTATION_270;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_270;
                    case 90:
                        return TEImageOrientation.ROTATION_180;
                    case 180:
                        return TEImageOrientation.ROTATION_90;
                    case 270:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                switch (currentDeviceDirection.angle) {
                    case 0:
                        return TEImageOrientation.ROTATION_90;
                    case 90:
                        return TEImageOrientation.ROTATION_0;
                    case 180:
                        return TEImageOrientation.ROTATION_270;
                    case 270:
                        return TEImageOrientation.ROTATION_180;
                }
            }

        }
        return TEImageOrientation.ROTATION_0;
    }


}
