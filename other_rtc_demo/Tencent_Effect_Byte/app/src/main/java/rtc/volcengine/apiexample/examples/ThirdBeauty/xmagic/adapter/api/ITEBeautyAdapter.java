package rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.api;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

import android.content.Context;

import androidx.annotation.IntDef;

import com.ss.bytertc.engine.data.VideoOrientation;
import com.tencent.xmagic.XmagicApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 美颜对象与火山RTC 进行结合使用时的中间适配层接口
 *
 * @param <T>
 */
public interface ITEBeautyAdapter<T> {


    /**
     * 将美颜与火山RTC SDK进行绑定
     *
     * @param context  应用上下文
     * @param callBack 绑定成功之后的回调，会返回美颜对象
     */
    void bind(Context context, T t, CallBack callBack);

    /**
     * 解除绑定
     */
    void unbind();


    void notifyVideoOrientationChanged(VideoOrientation orientation);

    void notifyCameraChanged(boolean isFrontCamera);

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

    enum AdapterEffectState {
        ENABLED, DISABLED
    }


}
