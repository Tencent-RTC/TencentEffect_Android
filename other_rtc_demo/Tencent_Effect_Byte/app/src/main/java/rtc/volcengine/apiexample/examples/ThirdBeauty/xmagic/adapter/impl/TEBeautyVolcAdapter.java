package rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.impl;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.EGL14;
import android.util.Log;


import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.VideoOrientation;
import com.ss.bytertc.engine.data.VideoPixelFormat;
import com.ss.bytertc.engine.video.IVideoProcessor;
import com.ss.bytertc.engine.video.VideoFrame;
import com.ss.bytertc.engine.video.VideoPreprocessorConfig;
import com.ss.bytertc.engine.video.builder.GLTextureVideoFrameBuilder;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.XmagicConstant;

import org.light.utils.LightLogUtil;

import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.api.ITEBeautyAdapter;

public class TEBeautyVolcAdapter implements ITEBeautyAdapter<RTCVideo> {

    private static final String TAG = "TEBeautyVolcAdapter";


    private Context mContext = null;
    private XmagicApi xmagicApi = null;

    private boolean isFrontCamera = true;

    private RTCVideo rtcVideo;


    private TextureProcessor mTextureProcessor = null;

    private CallBack mCallBack = null;

    private int mScreenOrientation = SCREEN_ORIENTATION_PORTRAIT;

    private XmagicConstant.EffectMode effectMode = XmagicConstant.EffectMode.PRO;

    private String xMagicResPath = null;

    private TESensorManager teSensorManager = null;
    private VideoOrientation mVideoOrientation = VideoOrientation.ADAPTIVE;

    private boolean isInitXmagicApi = false;


    public TEBeautyVolcAdapter(XmagicConstant.EffectMode effectMode, String xMagicResPath) {
        this.effectMode = effectMode;
        this.xMagicResPath = xMagicResPath;
    }


    private final IVideoProcessor customTextureListener = new IVideoProcessor() {
        @Override
        public VideoFrame processVideoFrame(VideoFrame frame) {
            initBeautyApi();
            if (mTextureProcessor != null) {
                int resultId = mTextureProcessor.rotateAndProcessTexture(frame.getTextureID(), frame.getWidth(), frame.getHeight(), isFrontCamera, mScreenOrientation);
                return processTextureFrame(frame, resultId);
            }
            return frame;
        }

        public void onGLEnvInitiated() {
            LogUtils.d(TAG, "customTextureListener   onGLEnvInitiated");
            initBeautyApi();
        }

        public void onGLEnvRelease() {
            LogUtils.d(TAG, "customTextureListener   onGLEnvRelease");
            destroyBeautyApi();
        }

        private synchronized VideoFrame processTextureFrame(VideoFrame frame, int textureID) {
            GLTextureVideoFrameBuilder builder = new GLTextureVideoFrameBuilder(VideoPixelFormat.TEXTURE_2D);
            builder.setEGLContext(EGL14.eglGetCurrentContext())
                    .setTextureID(textureID)
                    .setWidth(frame.getWidth())
                    .setHeight(frame.getHeight())
                    .setRotation(frame.getRotation())
                    .setTimeStampUs(frame.getTimeStampUs());
            return builder.build();
        }
    };

    @Override
    public void bind(Context context, RTCVideo rtcVideo, CallBack callBack) {
        this.mContext = context;
        if (this.teSensorManager == null) {
            this.teSensorManager = new TESensorManager(context);
        }
        this.teSensorManager.start();
        this.rtcVideo = rtcVideo;
        this.mCallBack = callBack;


        VideoPreprocessorConfig config = new VideoPreprocessorConfig();
        config.requiredPixelFormat = VideoPixelFormat.TEXTURE_2D;
        int result = this.rtcVideo.registerLocalVideoProcessor(customTextureListener, config);
        LogUtils.i(TAG, "bind result = " + result);

    }


    @Override
    public void unbind() {
        if (this.teSensorManager != null) {
            this.teSensorManager.stop();
        }
        if (this.rtcVideo == null) {
            LogUtils.e(TAG, "unbind method , mTextureSubject is null");
            return;
        }
        this.rtcVideo.registerLocalVideoProcessor(null, null);
    }

    @Override
    public void notifyVideoOrientationChanged(VideoOrientation orientation) {
        this.mVideoOrientation = orientation;
        if (mTextureProcessor != null) {
            mTextureProcessor.setVideoOrientation(orientation);
        }
    }

    @Override
    public void notifyCameraChanged(boolean isFrontCamera) {
        this.isFrontCamera = isFrontCamera;
    }


    @SuppressLint("WrongConstant")
    @Override
    public void notifyScreenOrientationChanged(@ScreenOrientation int screenOrientation) {
        this.mScreenOrientation = screenOrientation;
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
        //为什么 initBeautyApi 方法在 onGLEnvInitiated 和 processVideoFrame 方法中都有调用，是因为 registerLocalVideoProcessor方法多次设置之后
        // 除了第一次设置的customTextureListener 会回调onGLEnvInitiated，后边设置的不再回调 onGLEnvInitiated方法，所以在processVideoFrame 执行的时候也检测一次，如果没有初始化就再次初始化
        // 这种情况需要 问一下 火山的人员为什么后期设置的 customTextureListener 不会回调
        //Why the initBeautyApi method is called in both the onGLEnvInitiated and processVideoFrame methods is because after the registerLocalVideoProcessor method is set multiple times, except for the customTextureListener set for the first time, it will call back onGLEnvInitiated, and the subsequent settings will no longer call back the onGLEnvInitiated method, so it is also detected once when processVideoFrame is executed. , if it is not initialized, it needs to be initialized again. In this case, you need to ask. People from Huoshan why the customTextureListener set later does not call back
        if (isInitXmagicApi) {
            return;
        }
        isInitXmagicApi = true;
        this.mTextureProcessor = new TextureProcessor();
        this.xmagicApi = new XmagicApi(this.mContext, this.effectMode, xMagicResPath, (s, i) -> LightLogUtil.e(TAG, "onXmagicPropertyError,code = " + i + "  errorMsg = " + s));
        this.mTextureProcessor.setBeautyApi(this.xmagicApi);
        this.mTextureProcessor.setVideoOrientation(mVideoOrientation);
        if (this.teSensorManager != null) {
            this.teSensorManager.setEventListener((orientation, deviceDirection) -> {
                if (mTextureProcessor != null) {
                    mTextureProcessor.setImageOrientation(this.mScreenOrientation, isFrontCamera, deviceDirection);
                }
            });
        }
        if (this.mCallBack != null) {
            this.mCallBack.onCreatedTEBeautyApi(this.xmagicApi);
        }
    }


    private void destroyBeautyApi() {
        if (this.mTextureProcessor != null) {
            this.mTextureProcessor.onDestroy();
            this.mTextureProcessor = null;
        }
        if (this.mCallBack != null) {
            LogUtils.i(TAG, "destroyBeautyApi  onDestroyTEBeautyKit");
            this.mCallBack.onDestroyTEBeautyApi();
        }
        if (xmagicApi != null) {
            xmagicApi.onPause();
            xmagicApi.onDestroy();
        }
        this.isInitXmagicApi = false;
    }


}
