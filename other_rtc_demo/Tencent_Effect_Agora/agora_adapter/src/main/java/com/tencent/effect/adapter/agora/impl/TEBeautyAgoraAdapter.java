package com.tencent.effect.adapter.agora.impl;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.tencent.effect.adapter.agora.api.AgoraCustomTextureListener;
import com.tencent.effect.adapter.agora.api.IAgoraTextureProvider;
import com.tencent.effect.adapter.agora.api.ITEBeautyAdapter;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.XmagicConstant;

import org.light.utils.LightLogUtil;

public class TEBeautyAgoraAdapter implements ITEBeautyAdapter<IAgoraTextureProvider> {

    private static String TAG = TEBeautyAgoraAdapter.class.getName();
    private Context mContext = null;
    private XmagicApi xmagicApi = null;

    private boolean isFrontCamera = true;

    private IAgoraTextureProvider textureProvider;


    private TextureProcessor mTextureProcessor = null;

    private CallBack mCallBack = null;

    private int mScreenOrientation = SCREEN_ORIENTATION_PORTRAIT;

    private XmagicConstant.EffectMode effectMode = XmagicConstant.EffectMode.PRO;

    private String xMagicResPath = null;

    private TESensorManager teSensorManager = null;

    /**
     * 使用此构造方法，还需调用 {@link #setResPath(String)}
     */
    public TEBeautyAgoraAdapter() {
    }

    /**
     * 使用此构造方法，还需调用 {@link #setResPath(String)}
     */
    public TEBeautyAgoraAdapter(XmagicConstant.EffectMode effectMode) {
        this.effectMode = effectMode;
    }

    public TEBeautyAgoraAdapter(XmagicConstant.EffectMode effectMode, String xMagicResPath) {
        this.effectMode = effectMode;
        this.xMagicResPath = xMagicResPath;
    }

    public void setResPath(String xMagicResPath) {
        this.xMagicResPath = xMagicResPath;
    }

    private AgoraCustomTextureListener customTextureListener = new AgoraCustomTextureListener() {
        @Override
        public void onCameraChange(boolean isFrontCamera) {
            TEBeautyAgoraAdapter.this.isFrontCamera = isFrontCamera;
        }

        @Override
        public void onGLContextCreated() { //2. GLContext 创建
            LightLogUtil.d(TAG, "onGLContextCreated");
            initBeautyApi();
        }

        @Override
        public int onProcessVideoFrame(int textureId, int width, int height) {
            if (mTextureProcessor != null) {
                return mTextureProcessor.rotateAndProcessTexture(textureId, width, height, isFrontCamera);
            }
            return textureId;
        }

        @Override
        public void onGLContextDestroy() {
            Log.d(TAG, "onGLContextDestroy  " + Thread.currentThread().getName());
            destroyBeautyApi();
        }
    };

    @Override
    public void bind(Context context, IAgoraTextureProvider iAgoraTextureProvider, CallBack callBack) {
        this.mContext = context;
        if (this.teSensorManager == null) {
            this.teSensorManager = new TESensorManager(context);
        }
        this.teSensorManager.start();
        this.textureProvider = iAgoraTextureProvider;
        this.mCallBack = callBack;
        this.textureProvider.registerCustomTextureListener(customTextureListener);
    }


    @Override
    public void unbind() {
        if (this.teSensorManager != null) {
            this.teSensorManager.stop();
        }
        if (this.textureProvider == null) {
            LightLogUtil.e(TAG, "unbind method , mTextureSubject is null");
            return;
        }
        new Thread(() -> {
            if (textureProvider != null) {
                textureProvider.removeCustomTextureListener();
            }
        }).start();

    }


    @Override
    public void notifyCameraChanged(boolean isFrontCamera, boolean isEncoderMirror) {
        this.isFrontCamera = isFrontCamera;
    }


    @SuppressLint("WrongConstant")
    @Override
    public void notifyScreenOrientationChanged(@ScreenOrientation int screenOrientation) {
        this.mScreenOrientation = screenOrientation;
        if (this.mTextureProcessor != null) {
            this.mTextureProcessor.setScreenOrientation(screenOrientation);
        }
    }


    @Override
    public void notifyEffectStateChanged(AdapterEffectState effectState) {
        if (mTextureProcessor != null) {
            mTextureProcessor.notifyEffectStateChanged(effectState);
        }
    }

    /**
     * 初始化美颜SDK
     */
    @SuppressLint("WrongConstant")
    private void initBeautyApi() {
        this.mTextureProcessor = new TextureProcessor();
        this.mTextureProcessor.setScreenOrientation(this.mScreenOrientation);
        this.xmagicApi = new XmagicApi(this.mContext, this.effectMode, xMagicResPath,
                (s, i) -> LightLogUtil.e(TAG, "onXmagicPropertyError,code = " + i + "  errorMsg = " + s));
        this.mTextureProcessor.setBeautyApi(this.xmagicApi);
        if (this.teSensorManager != null) {
            this.teSensorManager.setEventListener((orientation, deviceDirection) -> {
                if (mTextureProcessor != null) {
                    mTextureProcessor.setImageOrientation(this.mScreenOrientation, isFrontCamera, deviceDirection);
                }
            });
        }
        if (this.mCallBack != null) {
            LightLogUtil.d(TAG, "initBeautyApi  onCreatedTEBeautyKit");
            this.mCallBack.onCreatedTEBeautyApi(this.xmagicApi);
        }
    }


    private void destroyBeautyApi() {
        if (this.mTextureProcessor != null) {
            this.mTextureProcessor.onDestroy();
            this.mTextureProcessor = null;
        }
        if (this.mCallBack != null) {
            LightLogUtil.d(TAG, "destroyBeautyApi  onDestroyTEBeautyKit");
            this.mCallBack.onDestroyTEBeautyApi();
        }
        if (xmagicApi != null) {
            xmagicApi.onPause();
            xmagicApi.onDestroy();
        }
    }


}
