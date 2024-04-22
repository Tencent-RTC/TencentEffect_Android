package com.tencent.effect.beautykit;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.tencent.effect.beautykit.config.DeviceDirection;
import com.tencent.xmagic.GlUtil;
import com.tencent.xmagic.XmagicApi;
import com.tencent.effect.beautykit.enhance.TEParamEnhancingStrategy;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.enhance.DefaultEnhancingStrategy;
import com.tencent.effect.beautykit.manager.TEParamManager;
import com.tencent.effect.beautykit.utils.WorkThread;
import com.tencent.xmagic.bean.TEImageOrientation;
import com.tencent.xmagic.telicense.TELicenseCheck;
import com.tencent.xmagic.util.FileUtil;

import java.io.File;
import java.util.List;


public class TEBeautyKit implements SensorEventListener {

    private static final String TAG = TEBeautyKit.class.getName();
    private volatile XmagicApi mXMagicApi;
    private boolean isXMagicApiDestroyed = false;
    //判断当前手机旋转方向，用于手机在不同的旋转角度下都能正常的识别人脸
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private boolean isEnableEnhancedMode = false;
    //是否静音
    private boolean mIsMute = false;
    //美颜类型
    private int mBeautyStreamType = XmagicApi.PROCESS_TYPE_CAMERA_STREAM;
    private XmagicApi.XmagicAIDataListener mAIDataListener = null;
    private XmagicApi.XmagicTipsListener mTipsListener = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final TEParamManager mParamManager = new TEParamManager();
    private final Object mLock = new Object();
    //增强模式处理类
    private TEParamEnhancingStrategy mParamEnhancingStrategy = new DefaultEnhancingStrategy();
    private EffectState mEffectState = EffectState.ENABLED;
    private int mLogLevel = Log.WARN;
    private Context mApplicationContext = null;
    private EventListener mEventListener = null;

    private XmagicApi.ExportTextureCallback mTextureCallback = null;

    /**
     * 约定以 "/" 结尾, 方便拼接
     * xmagic resource local path
     */
    private static String mResPath = null;

    /**
     * 异步创建TEBeautyKit对象
     * @param context Android应用上下文
     * @param initListener  初始化回调接口
     */
    public static void create(@NonNull Context context, @NonNull OnInitListener initListener) {
        TEBeautyKit.create(context, false, initListener);
    }


    /**
     *
     * 异步创建TEBeautyKit对象
     * @param context Android应用上下文
     * @param isEnableHighPerformance 是否开启增强模式
     * @param initListener 初始化回调接口
     */
    public static void create(@NonNull Context context, boolean isEnableHighPerformance, @NonNull OnInitListener initListener) {
        new TEBeautyKit(context, isEnableHighPerformance, initListener);
    }


    /**
     * @param context      应用上下文
     * @param initListener 创建对象的回调，如果创建失败，错误信息回从此接口回调
     */
    private TEBeautyKit(Context context, @NonNull OnInitListener initListener) {
        this(context, false, initListener);
    }


    /**
     * @param context                 应用上下文
     * @param isEnableHighPerformance 是否开启高性能模式
     * @param initListener            创建对象的回调，如果创建失败，错误信息回从此接口回调
     */
    private TEBeautyKit(Context context, boolean isEnableHighPerformance, @NonNull OnInitListener initListener) {
        this.initSensor(context);
        WorkThread.getInstance().run(() -> {
            synchronized (this.mLock) {
                if (this.isXMagicApiDestroyed) {
                    return;
                }
                this.mXMagicApi = initXMagicApi(mApplicationContext, isEnableHighPerformance);
                this.mHandler.post(() -> {
                    initListener.onInitResult(this);
                });
            }
        }, this.hashCode());

    }


    /**
     * @param context 应用上下文
     */
    public TEBeautyKit(Context context) {
        this(context, false);
    }

    /**
     * @param context                 应用上下文
     * @param isEnableHighPerformance 是否开启高性能模式
     */
    public TEBeautyKit(Context context, boolean isEnableHighPerformance) {
        this.initSensor(context);
        this.mXMagicApi = this.initXMagicApi(mApplicationContext, isEnableHighPerformance);
    }


    private XmagicApi initXMagicApi(Context context, boolean isEnableHighPerformance) {
        XmagicApi api = new XmagicApi(context, mResPath, (errorMsg, code) -> {
            //加载特效异常
            LogUtils.e(TAG, "createXMagicApi  errorMsg = " + errorMsg + "  code = " + code);
        });
        if (isEnableHighPerformance) {
            api.setDowngradePerformance();
        }

        //是否开启animoji表情检测开关，默认关。
//        api.setFeatureEnableDisable(FeatureName.ANIMOJI_52_EXPRESSION, true);
        //是否开启3D身体点位开关，默认关。
//        api.setFeatureEnableDisable(FeatureName.BODY_3D_POINT, true);
//        api.setFeatureEnableDisable(FeatureName.HAND_DETECT, true);
        api.setXmagicLogLevel(this.mLogLevel);
        if (this.mIsMute) {   //如果为true 表示在外侧手动设置了，所以需要创建好 xmagicapi之后设置
            api.setAudioMute(true);
        }
        if (this.mBeautyStreamType != XmagicApi.PROCESS_TYPE_CAMERA_STREAM) {
            api.setXmagicStreamType(this.mBeautyStreamType);
        }
        if (this.mAIDataListener != null) {
            api.setAIDataListener(this.mAIDataListener);
        }
        if (this.mTipsListener != null) {
            api.setTipsListener(this.mTipsListener);
        }
        return api;
    }

    /**
     * 设置静音
     *
     * @param isMute true 表示静音
     */
    public void setMute(boolean isMute) {
        this.mIsMute = isMute;
        if (this.mXMagicApi != null) {
            this.mXMagicApi.setAudioMute(isMute);
        }
    }

    /**
     * 如果是对图片进行美颜处理，需要调用此方法设置数据源的类型，分别为相机数据源和图片数据源：
     * 相机数据源：XmagicApi.PROCESS_TYPE_CAMERA_STREAM  图片数据源：XmagicApi.PROCESS_TYPE_PICTURE_DATA。
     * @param type 默认是视频流类型
     */
    public void setBeautyStreamType(int type) {
        this.mBeautyStreamType = type;
        if (this.mXMagicApi != null) {
            this.mXMagicApi.setXmagicStreamType(type);
        }
    }

    /**
     * 设置某个特性的开或关
     *
     * @param featureName 取值见 XmagicConstant.FeatureName
     * @param enable      true表示开启，false表示关闭
     */
    public void setFeatureEnableDisable(String featureName, boolean enable) {
        if (this.mXMagicApi != null) {
            this.mXMagicApi.setFeatureEnableDisable(featureName, enable);
        }
    }

    /**
     * 用于处理美颜图片
     *
     * @param bitmap 等待美颜的图片
     * @param needReset true表示SDK内部调用两次
     * @return 美颜后的图片
     */
    public Bitmap process(Bitmap bitmap, boolean needReset) {
        if (this.mEffectState == EffectState.DISABLED) {
            return bitmap;
        }
        if (this.mXMagicApi != null) {
            return this.mXMagicApi.process(bitmap, needReset);
        }
        return bitmap;
    }

    /**
     * 处理 视频/摄像头 每一帧数据
     *
     * @param textureId 纹理id，此纹理需要的是纹理类型为GL_TEXTURE_2D，纹理像素格式为RGBA
     * @param width     纹理宽度
     * @param height    纹理高度
     * @return 处理后的纹理ID
     */
    public int process(int textureId, int width, int height) {
        if (this.mEffectState == EffectState.DISABLED) {
            if (this.mTextureCallback != null) {
                Bitmap bitmap = GlUtil.readTexture(textureId, width, height);
                this.mTextureCallback.onCallback(bitmap);
                this.mTextureCallback = null;
            }
            return textureId;
        }
        if (this.mXMagicApi != null) {
            return this.mXMagicApi.process(textureId, width, height);
        }
        return textureId;
    }

    /**
     * 更新美颜属性
     *
     * @param paramList
     */
    public void setEffectList(List<TEUIProperty.TESDKParam> paramList) {
        if (this.mXMagicApi == null || paramList == null || paramList.size() == 0) {
            return;
        }
        for (TEUIProperty.TESDKParam param : paramList) {
            this.setEffect(param);
        }
    }

    /**
     * 更新美颜属性
     *
     * @param teParam
     */
    public void setEffect(TEUIProperty.TESDKParam teParam) {
        if (this.mXMagicApi == null || teParam == null) {
            return;
        }
        TEUIProperty.TESDKParam param = this.isEnableEnhancedMode ? this.mParamEnhancingStrategy.enhanceParam(teParam) : teParam;
        LogUtils.d(TAG, "setEffect " + this.isEnableEnhancedMode + "    " + param.toString());
        this.mXMagicApi.setEffect(param.effectName, param.effectValue, param.resourcePath, param.extraInfo);
        this.mParamManager.putTEParam(teParam);
    }

    /**
     * 获取当前是否开启增强模式
     *
     * @return
     */
    public boolean isEnableEnhancedMode() {
        return this.isEnableEnhancedMode;
    }

    /**
     * 开启或关闭增强模式
     *
     * @param enableEnhancedMode true 表示开启增强模式  false 表示关闭增强模式
     * @return 返回true表示状态发生改变了，false 表示状态没有改变
     */
    public boolean enableEnhancedMode(boolean enableEnhancedMode) {
        if (this.isEnableEnhancedMode != enableEnhancedMode) {
            this.isEnableEnhancedMode = enableEnhancedMode;
            return true;
        }
        return false;
    }

    /**
     * 获取当前生效的美颜属性列表
     *
     * @return
     */
    public List<TEUIProperty.TESDKParam> getInUseSDKParamList() {
        return this.mParamManager.getParams();
    }


    /**
     * 获取当前生效的美颜属性列表字符串。
     * 客户可以将导出的字符串进行本地保存，在下次创建TEPanelView对象后调用setLastParamList方法进行设置。
     * @return
     */
    public String exportInUseSDKParam() {
        List<TEUIProperty.TESDKParam> sdkParams = this.mParamManager.getParams();
        if (sdkParams != null && sdkParams.size() > 0) {
            return new Gson().toJson(sdkParams);
        } else {
            return null;
        }
    }


    /**
     * 用于恢复贴纸中的声音
     * 恢复陀螺仪传感器
     */
    public void onResume() {
        if (this.mXMagicApi != null) {
            this.mXMagicApi.onResume();
        }
        //注册传感器
        if (this.mSensorManager != null) {
            this.mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * 用于暂停贴纸中的声音
     * 暂停陀螺仪传感器
     */
    public void onPause() {
        if (this.mXMagicApi != null) {
            this.mXMagicApi.onPause();
        }
        //取消传感器
        if (this.mSensorManager != null) {
            this.mSensorManager.unregisterListener(this);
        }
    }

    /**
     * 当页面效果时调用，销毁美颜
     * 注意：必须在gl线程调用此方法
     */
    public void onDestroy() {
        synchronized (mLock) {
            this.isXMagicApiDestroyed = true;
            WorkThread.getInstance().cancel(this.hashCode());
            if (this.mXMagicApi != null) {
                this.mXMagicApi.onDestroy();
                this.mXMagicApi = null;
            }
        }
        this.mParamManager.clear();
    }


    private void initSensor(Context context) {
        this.mApplicationContext = context.getApplicationContext();
        this.mSensorManager = (SensorManager) mApplicationContext.getSystemService(Context.SENSOR_SERVICE);
        this.mAccelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * 设置事件监听，用于监听 手机方向事件，用于adapter
     * @param listener 事件监听回调
     */
    public void setEventListener(EventListener listener) {
        this.mEventListener = listener;
    }

    /**
     * 设置setAIDataListener 回调
     *
     * @param aiDataListener
     */
    public void setAIDataListener(XmagicApi.XmagicAIDataListener aiDataListener) {
        this.mAIDataListener = aiDataListener;
        if (this.mXMagicApi != null) {
            this.mXMagicApi.setAIDataListener(mAIDataListener);
        }
    }


    /**
     * 设置动效提示语回调函数，用于将提示语展示到前端页面上。
     *
     * @param tipsListener
     */
    public void setTipsListener(XmagicApi.XmagicTipsListener tipsListener) {
        this.mTipsListener = tipsListener;
        if (this.mXMagicApi != null) {
            this.mXMagicApi.setTipsListener(this.mTipsListener);
        }
    }

    /**
     * 设置日志级别
     *
     * @param logLevel
     */
    public void setLogLevel(int logLevel) {
        this.mLogLevel = logLevel;
        if (this.mXMagicApi != null) {
            this.mXMagicApi.setXmagicLogLevel(this.mLogLevel);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (this.mXMagicApi != null) {
            this.mXMagicApi.sensorChanged(event, mAccelerometer);
            this.dispatchOrientation(event, mAccelerometer);
        }
    }


    protected void dispatchOrientation(SensorEvent event, Sensor accelerometer) {
        int orientation = this.mApplicationContext.getResources().getConfiguration().orientation;
        DeviceDirection deviceDirection = DeviceDirection.PORTRAIT_UP;
        if (event.sensor == accelerometer) {
            float currentXAxis = event.values[0];
            float currentYAxis = event.values[1];
            if (Math.abs(currentYAxis) > Math.abs(currentXAxis)) {
                if (currentYAxis > 1) {
                    deviceDirection = DeviceDirection.PORTRAIT_UP;
                } else if (currentYAxis < -1) {
                    deviceDirection = DeviceDirection.PORTRAIT_DOWN;
                }
            } else {
                if (currentXAxis > 1) {
                    deviceDirection = DeviceDirection.LANDSCAPE_LEFT;
                } else if (currentXAxis < -1) {
                    deviceDirection = DeviceDirection.LANDSCAPE_RIGHT;
                }
            }
        }
        if (this.mEventListener != null) {
            this.mEventListener.onDeviceDirectionChanged(orientation, deviceDirection);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * 截取当前纹理上的画面
     *
     * @param callback
     */
    public void exportCurrentTexture(XmagicApi.ExportTextureCallback callback) {
        if (this.mEffectState == EffectState.DISABLED) {
            this.mTextureCallback = callback;
            return;
        }
        if (this.mXMagicApi != null) {
            this.mXMagicApi.exportCurrentTexture(callback);
        }
    }


    /**
     * 设置纹理逆时针旋转的度数。
     * 主要作用：用于SDK内部对纹理进行旋转，旋转对应角度之后，让人头朝上，这样SDK内部就可以识别人脸
     * 默认情况下SDK内部会使用sensor传感器获取需要旋转的角度
     *
     * @param orientation 取值只有0、90、180、270
     */
    public void setImageOrientation(TEImageOrientation orientation) {
        if (this.mXMagicApi == null) {
            LogUtils.e(TAG, "setImageOrientation: xmagicApi is null ");
            return;
        }
        this.mXMagicApi.setImageOrientation(orientation);
    }


    /**
     * 检测当前设备是否支持此素材
     *
     * @param motionResPath 素材文件的路径
     * @return
     */
    public boolean isDeviceSupport(String motionResPath) {
        if (TextUtils.isEmpty(motionResPath) || (!new File(motionResPath).exists())) {
            LogUtils.e(TAG, "isDeviceSupport: motionResPath is null or file not exists ");
            return false;
        } else if (this.mXMagicApi == null) {
            LogUtils.e(TAG, "isDeviceSupport: xmagicApi is null ");
            return false;
        } else {
            return this.mXMagicApi.isDeviceSupport(motionResPath);
        }
    }

    public boolean isSupportBeauty() {
        if (this.mXMagicApi != null) {
            return this.mXMagicApi.isSupportBeauty();
        }
        return true;
    }

    /**
     * 获取美颜特效的开关状态
     *
     * @return EffectState
     */
    public EffectState getEffectState() {
        return mEffectState;
    }

    /**
     * 设置是否开启美颜
     *
     * @param effectState ENABLED, 表示开启  DISABLED 表示关闭
     */
    public void setEffectState(EffectState effectState) {
        this.mEffectState = effectState;
        if (this.mXMagicApi != null) {
            if (mEffectState == EffectState.ENABLED) {
                this.mXMagicApi.onResume();
            } else {
                this.mXMagicApi.onPause();
            }
        }
    }

    /**
     * 获取当前增强模式使用的策略
     *
     * @return
     */
    public TEParamEnhancingStrategy getParamEnhancingStrategy() {
        return mParamEnhancingStrategy;
    }

    /**
     * 设置增强模式的策略实现类，如果不设置，则使用默认的实现
     *
     * @param teParamEnhancingStrategy 增强模式处理类
     */
    public void setParamEnhancingStrategy(TEParamEnhancingStrategy teParamEnhancingStrategy) {
        this.mParamEnhancingStrategy = teParamEnhancingStrategy;
    }


    //静态方法开始

    /**
     * 设置美颜资源存放的路径
     *
     * @param resPath
     */
    public static void setResPath(String resPath) {
        if (!TextUtils.isEmpty(resPath)) {
            if (resPath.endsWith(File.separator)) {
                mResPath = resPath;
            } else {
                mResPath = resPath + File.separator;
            }
        }
    }

    public static String getResPath() {
        return mResPath;
    }

    /**
     * 从 apk 的 assets 解压资源文件到指定路径, 需要先设置路径: {@link #setResPath(String)} <br>
     * 首次安装 App, 或 App 升级后调用一次即可.
     * copy xmagic resource from assets to local path
     */
    public static boolean copyRes(Context context) {
        if (TextUtils.isEmpty(mResPath)) {
            throw new IllegalStateException("resource path not set, call XmagicResParser.setResPath() first.");
        }
        int addResult = XmagicApi.addAiModeFilesFromAssets(context, mResPath);
        LogUtils.e(TAG, "add ai model files result = " + addResult);
        String lutDirName = "lut";
        boolean result = FileUtil.copyAssets(context, lutDirName, mResPath + "light_material" + File.separator + lutDirName);
        String motionResDirName = "MotionRes";
        boolean result2 = FileUtil.copyAssets(context, motionResDirName, mResPath + motionResDirName);
        return result && result2;
    }

    /**
     * 进行美颜授权检验
     * 注意：在使用此方法时，如果不设置回调接口，将不进行鉴权（只会从网络下载鉴权信息），
     * 所以可以参考demo  在application中调用时不设置回调，但是在使用xMagicApi对象之前再次调用此方法（设置回调接口）鉴权
     * <p>
     * Perform beauty authorization verification.
     * Note: When using this method, if the callback interface is not set,
     * no authentication will be performed (only the authentication information will be downloaded from the network),
     * so you can refer to the demo to not set the callback when calling in the application,
     * but call this method again (set the callback interface) before using the xMagicApi object to authenticate.
     *
     * @param context                应用上下文
     * @param licenseUrl             在平台申请的licenseUrl
     * @param licenseKey             在平台申请的licenseKey
     * @param teLicenseCheckListener 鉴权回调接口
     */
    public static void setTELicense(Context context, String licenseUrl, String licenseKey, TELicenseCheck.TELicenseCheckListener teLicenseCheckListener) {                         //
        if (teLicenseCheckListener == null) {
            TELicenseCheck.getInstance().setTELicense(context, licenseUrl, licenseKey, null);
        } else {
            TELicenseCheck.getInstance().setTELicense(context, licenseUrl, licenseKey, (i, s) -> new Handler(Looper.getMainLooper()).post(() -> {
                teLicenseCheckListener.onLicenseCheckFinish(i, s);
            }));
        }
    }
    //静态方法结束


    /**
     * TEBeautyKit中 异步创建TEBeautyKit的回调事件
     * 当异步创建成功之后通过此接口回调给使用者
     */
    public interface OnInitListener {
        void onInitResult(TEBeautyKit beautyKit);
    }

    /**
     * TEBeautyKit中的事件回调接口
     * 主要用于回调手机方向改变事件
     */
    public interface EventListener {

        /**
         * @param orientation     Overall orientation of the screen.  May be one of
         *                        * {@link # Configuration.ORIENTATION_LANDSCAPE}, {@link # Configuration.ORIENTATION_PORTRAIT}.
         * @param deviceDirection 屏幕方向
         */
        void onDeviceDirectionChanged(int orientation, DeviceDirection deviceDirection);
    }


    public enum EffectState {
        ENABLED, DISABLED
    }


}
