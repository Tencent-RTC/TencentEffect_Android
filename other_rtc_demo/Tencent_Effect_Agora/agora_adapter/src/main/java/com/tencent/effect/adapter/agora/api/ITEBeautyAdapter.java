package com.tencent.effect.adapter.agora.api;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

import android.content.Context;

import androidx.annotation.IntDef;

import com.tencent.xmagic.XmagicApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 美颜对象与声望RTC 进行结合使用时的中间适配层接口
 *
 * @param <T>
 */
public interface ITEBeautyAdapter<T> {


    /**
     * 将美颜与声网RTC SDK进行绑定
     *
     * @param context  应用上下文
     * @param t        声网 RTC 纹理提供者 IAgoraTextureProvider
     * @param callBack 绑定成功之后的回调，会返回美颜对象
     */
    void bind(Context context, T t, CallBack callBack);

    /**
     * 解除绑定
     */
    void unbind();

    /**
     * 当前后摄像头切换的时候调用此方法通知adapter
     *
     * @param isFrontCamera   是否是前置摄像头
     * @param isEncoderMirror 远程是否镜像
     */
    void notifyCameraChanged(boolean isFrontCamera, boolean isEncoderMirror);

    /**
     * 当页面activity 的方向改变的时候调用。
     *
     * @param screenOrientation
     */
    void notifyScreenOrientationChanged(@ScreenOrientation int screenOrientation);

    void notifyEffectStateChanged(AdapterEffectState effectState);


    interface CallBack {
        void onCreatedTEBeautyApi(XmagicApi xmagicApi);

        void onDestroyTEBeautyApi();
    }


    @IntDef(value = {
            SCREEN_ORIENTATION_LANDSCAPE,
            SCREEN_ORIENTATION_PORTRAIT,
            SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            SCREEN_ORIENTATION_REVERSE_PORTRAIT,
    })
    @Retention(RetentionPolicy.CLASS)
    @interface ScreenOrientation {
    }

    public enum AdapterEffectState {
        ENABLED, DISABLED
    }


}
