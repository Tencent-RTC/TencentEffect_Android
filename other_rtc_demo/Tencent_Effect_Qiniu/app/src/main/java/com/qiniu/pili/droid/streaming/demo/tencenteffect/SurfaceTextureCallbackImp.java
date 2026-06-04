package com.qiniu.pili.droid.streaming.demo.tencenteffect;

import android.content.Context;
import android.util.Log;

import com.qiniu.pili.droid.streaming.SurfaceTextureCallback;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.demo.xmagic.render.TextureConverter;

public class SurfaceTextureCallbackImp implements SurfaceTextureCallback {


    private static final String TAG = SurfaceTextureCallbackImp.class.getName();

    private Context context = null;
    private TEBeautyKit beautyKit = null;
    private TextureConverter textureConverter = null;

    private TextureConverter directionRevert = null;

    private boolean isFrontCamera = true;
    private CallBack callBack;


    public SurfaceTextureCallbackImp(Context context, CallBack callBack) {
        this.context = context;
        this.callBack = callBack;
    }

    public void setFrontCamera(boolean frontCamera) {
        isFrontCamera = frontCamera;
    }

    @Override
    public void onSurfaceCreated() {
        Log.e(TAG, "onSurfaceCreated   " + Thread.currentThread().getName());

        this.destroyBeautyApi();
        this.createBeautyApi();

    }

    private void createBeautyApi() {
        this.textureConverter = new TextureConverter();
        this.directionRevert = new TextureConverter();
        beautyKit = new TEBeautyKit(context, com.tencent.xmagic.XmagicConstant.EffectMode.PRO);
        if (callBack != null) {
            callBack.onCreatedTEBeautyKit(beautyKit);
        }
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }


    public void onResume() {
        if (beautyKit != null) {
            beautyKit.onResume();
        }
    }


    public void onPause() {
        if (beautyKit != null) {
            beautyKit.onPause();
        }
    }


    @Override
    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed   " + Thread.currentThread().getName());
        this.destroyBeautyApi();
    }

    private void destroyBeautyApi() {
        if (callBack != null) {
            callBack.onDestroyTEBeautyKit();
        }
        if (beautyKit != null) {
            beautyKit.onPause();
            beautyKit.onDestroy();
        }
        if (textureConverter != null) {
            textureConverter.release();
        }
        if (directionRevert != null) {
            directionRevert.release();
        }
    }


    @Override
    public int onDrawFrame(int textureId, int width, int height, float[] transformMatrix) {
        if (directionRevert == null || textureConverter == null || beautyKit == null) {
            return textureId;
        }
        //如果是oes 纹理需要调用oes2Rgba 方法，如果是 texture 2d 则不需要
        int rgbaId = textureConverter.oes2Rgba(textureId, width, height);
        int id = textureConverter.convert(rgbaId, width, height, this.isFrontCamera ? 270 : 90, false, false);
        int processed = beautyKit.process(id, height, width);
        return directionRevert.convert(processed, height, width, this.isFrontCamera ? 90 : 270, false, false);
    }


    public interface CallBack {
        void onCreatedTEBeautyKit(TEBeautyKit beautyKit);

        void onDestroyTEBeautyKit();
    }

}
