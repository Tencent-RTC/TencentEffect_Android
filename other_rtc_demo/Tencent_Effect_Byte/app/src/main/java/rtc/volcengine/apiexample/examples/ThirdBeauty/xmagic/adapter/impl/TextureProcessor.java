package rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.impl;


import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

import com.ss.bytertc.engine.data.VideoOrientation;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.bean.TEImageOrientation;

import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.api.DeviceDirection;
import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.api.ITEBeautyAdapter;
import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.render.TextureConverter;


public class TextureProcessor {


    private static final String TAG = TextureProcessor.class.getName();


    private XmagicApi xmagicApi = null;

    private boolean isFrontCamera = true;


    private VideoOrientation videoOrientation = VideoOrientation.PORTRAIT;
    //用于旋转纹理方向
    private TextureConverter directionRotate = new TextureConverter();
    //用于还原纹理方向
    private TextureConverter directionRevert = new TextureConverter();

    private ITEBeautyAdapter.AdapterEffectState effectState = ITEBeautyAdapter.AdapterEffectState.ENABLED;
    private boolean additionalProcess = false;


    public void setVideoOrientation(VideoOrientation orientation) {
        this.videoOrientation = orientation;
    }

    public void setBeautyApi(XmagicApi xmagicApi) {
        this.xmagicApi = xmagicApi;
    }

    public int rotateAndProcessTexture(int textureId, int width, int height, boolean isFrontCamera, @ITEBeautyAdapter.ScreenOrientation int screenOrientation) {
        this.isFrontCamera = isFrontCamera;
        if (this.xmagicApi == null || this.effectState != ITEBeautyAdapter.AdapterEffectState.ENABLED) {
            additionalProcess = true;
            return textureId;
        }
        if (additionalProcess) {
            additionalProcess = false;
            this.processTexture(textureId, width, height, screenOrientation);
        }
        return this.processTexture(textureId, width, height, screenOrientation);
    }


    private int processTexture(int textureId, int width, int height, @ITEBeautyAdapter.ScreenOrientation int screenOrientation) {
        int rotation = this.getTextureRotationAngle(screenOrientation, isFrontCamera);
        int temporaryId = textureId;
        if (rotation != 0) {
            temporaryId = directionRotate.convert(temporaryId, width, height, rotation, false, false);
            if (rotation == 90 || rotation == 270) {
                int processId = this.xmagicApi.process(temporaryId, height, width);
                return directionRevert.convert(processId, height, width, 360 - rotation, false, false);
            } else {
                int processId = this.xmagicApi.process(temporaryId, width, height);
                return directionRevert.convert(processId, width, height, 360 - rotation, false, false);
            }
        } else {
            return this.xmagicApi.process(temporaryId, width, height);
        }

    }


    public void onDestroy() {
        if (this.directionRotate != null) {
            this.directionRotate.release();
        }
        if (this.directionRevert != null) {
            this.directionRevert.release();
        }
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
            return;
        }
        this.isFrontCamera = isFrontCamera;
        TEImageOrientation teImageOrientation = this.getOrientation(screenOrientation, currentDeviceDirection);
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


    private int getTextureRotationAngle(@ITEBeautyAdapter.ScreenOrientation int screenOrientation, boolean isFrontCamera) {
        if (videoOrientation == VideoOrientation.ADAPTIVE) {
            switch (screenOrientation) {
                case SCREEN_ORIENTATION_PORTRAIT:   //已处理
                    if (isFrontCamera) {
                        return 270;
                    } else {
                        return 90;
                    }
                case SCREEN_ORIENTATION_LANDSCAPE:   //已处理
                    return 180;
                case SCREEN_ORIENTATION_REVERSE_LANDSCAPE:  //已处理
                    return 0;
                case SCREEN_ORIENTATION_REVERSE_PORTRAIT:   //已处理
                    if (isFrontCamera) {
                        return 90;
                    } else {
                        return 270;
                    }
            }
        } else if (videoOrientation == VideoOrientation.PORTRAIT) {
            switch (screenOrientation) {
                case SCREEN_ORIENTATION_PORTRAIT:   //已处理
                    return 180;
                case SCREEN_ORIENTATION_LANDSCAPE:   //已处理
                    return 270;
                case SCREEN_ORIENTATION_REVERSE_LANDSCAPE:  //已处理
                    return 90;
                case SCREEN_ORIENTATION_REVERSE_PORTRAIT:  //已处理
                    return 0;
            }
        } else if (videoOrientation == VideoOrientation.LANDSCAPE) {
            switch (screenOrientation) {
                case SCREEN_ORIENTATION_PORTRAIT:
                    return 90;
                case SCREEN_ORIENTATION_LANDSCAPE:
                    return 180;
                case SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                    return 180;
                case SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                    return 270;
            }
        }
        return 0;
    }


    /**
     * @param screenOrientation      屏幕方向
     * @param currentDeviceDirection 当前手机的方向，此数据是由Sensor传感器传入
     * @return
     */
    private TEImageOrientation getOrientation(@ITEBeautyAdapter.ScreenOrientation int screenOrientation, DeviceDirection currentDeviceDirection) {
        if (videoOrientation == VideoOrientation.ADAPTIVE) {
            return this.getOrientationOnAdaptive(screenOrientation, currentDeviceDirection);
        } else if (videoOrientation == VideoOrientation.PORTRAIT) {
            return this.getOrientationOnPortrait(screenOrientation, currentDeviceDirection);
        } else if (videoOrientation == VideoOrientation.LANDSCAPE) {
            return this.getOrientationOnLandscape(screenOrientation, currentDeviceDirection);
        }
        return TEImageOrientation.ROTATION_0;
    }


    private TEImageOrientation getOrientationOnAdaptive(@ITEBeautyAdapter.ScreenOrientation int screenOrientation, DeviceDirection currentDeviceDirection) {

        if (this.isFrontCamera) {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_270;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_90;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_90;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_270;
                }

            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_180;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {  //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_0;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_180;
                }
            }
        } else {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_90;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_270;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_270;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_90;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {  //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_180;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_0;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_180;
                }
            }
        }
        return TEImageOrientation.ROTATION_0;
    }


    private TEImageOrientation getOrientationOnPortrait(@ITEBeautyAdapter.ScreenOrientation int screenOrientation, DeviceDirection currentDeviceDirection) {
        if (this.isFrontCamera) {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_270;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_90;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {  //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_90;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_270;
                }

            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {    //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_180;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) { //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_0;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_180;
                }
            }
        } else {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_90;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_270;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {  //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_270;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_90;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {  //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_180;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_0;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_180;
                }
            }
        }
        return TEImageOrientation.ROTATION_0;
    }


    private TEImageOrientation getOrientationOnLandscape(@ITEBeautyAdapter.ScreenOrientation int screenOrientation, DeviceDirection currentDeviceDirection) {
        if (this.isFrontCamera) {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {    //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_270;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_90;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {  //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_90;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_270;
                }

            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_180;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_0;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_180;
                }
            }
        } else {
            if (screenOrientation == SCREEN_ORIENTATION_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_90;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_270;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_180;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_270;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_0;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_90;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_180;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_0;
                }
            } else if (screenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {   //已处理
                switch (currentDeviceDirection) {
                    case PORTRAIT_UP:
                        return TEImageOrientation.ROTATION_270;
                    case LANDSCAPE_RIGHT:
                        return TEImageOrientation.ROTATION_0;
                    case PORTRAIT_DOWN:
                        return TEImageOrientation.ROTATION_90;
                    case LANDSCAPE_LEFT:
                        return TEImageOrientation.ROTATION_180;
                }
            }

        }
        return TEImageOrientation.ROTATION_0;
    }


}
