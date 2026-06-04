package com.tencent.effect.demo.zego;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.xmagic.bean.TEImageOrientation;
import java.nio.ByteBuffer;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoCustomVideoProcessHandler;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

public class CustomVideoProcessHandler extends IZegoCustomVideoProcessHandler {

    private static String TAG = CustomVideoProcessHandler.class.getName();


    private TEBeautyKit teBeautyKit = null;
    private Context context;
    private ZegoExpressEngine expressEngine;
    private CustomVideoProcessCallBack customVideoProcessCallBack = null;
    private boolean isFrontCamera = true;

    public CustomVideoProcessHandler(Context context, ZegoExpressEngine expressEngine) {
        this.context = context;
        this.expressEngine = expressEngine;
    }

    public void setCustomVideoProcessCallBack(CustomVideoProcessCallBack customVideoProcessCallBack) {
        this.customVideoProcessCallBack = customVideoProcessCallBack;
    }

    public void setFrontCamera(boolean frontCamera) {
        isFrontCamera = frontCamera;
    }

    @Override
    public void onStart(ZegoPublishChannel channel) {
        super.onStart(channel);
        this.teBeautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);
        this.teBeautyKit.setEventListener((orientation, deviceDirection) -> {
           if(isFrontCamera){
               switch (deviceDirection){
                   case PORTRAIT_UP:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_0);
                       break;
                   case PORTRAIT_DOWN:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_180);
                       break;

                   case LANDSCAPE_LEFT:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_270);
                       break;
                   case LANDSCAPE_RIGHT:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_90);
                       break;
               }
           }else {
               switch (deviceDirection){
                   case PORTRAIT_UP:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_0);
                       break;
                   case PORTRAIT_DOWN:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_180);
                       break;
                   case LANDSCAPE_LEFT:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_90);
                       break;
                   case LANDSCAPE_RIGHT:
                       teBeautyKit.setImageOrientation(TEImageOrientation.ROTATION_270);
                       break;
               }
           }
        });
        if (this.customVideoProcessCallBack != null) {
            this.customVideoProcessCallBack.onCreatedBeautyKit(this.teBeautyKit);
        }
        Log.e(TAG, "onStart   " + Thread.currentThread().getName());
    }

    @Override
    public void onStop(ZegoPublishChannel channel) {
        super.onStop(channel);
        if (teBeautyKit != null) {
            this.teBeautyKit.onPause();
            this.teBeautyKit.onDestroy();
        }
        Log.e(TAG, "onStop   " + Thread.currentThread().getName());
    }

    @Override
    public void onCapturedUnprocessedRawData(ByteBuffer data, int[] dataLength, ZegoVideoFrameParam param, long referenceTimeMillisecond, ZegoPublishChannel channel) {
        super.onCapturedUnprocessedRawData(data, dataLength, param, referenceTimeMillisecond, channel);
    }

    @Override
    public void onCapturedUnprocessedTextureData(int textureID, int width, int height, long referenceTimeMillisecond, ZegoPublishChannel channel) {
        super.onCapturedUnprocessedTextureData(textureID, width, height, referenceTimeMillisecond, channel);
        if (this.teBeautyKit == null) {
            expressEngine.sendCustomVideoProcessedTextureData(textureID, width, height, referenceTimeMillisecond, channel);
        } else {
            int resultId = this.teBeautyKit.process(textureID, width, height);
            expressEngine.sendCustomVideoProcessedTextureData(resultId, width, height, referenceTimeMillisecond, channel);
        }
    }

    @Override
    public SurfaceTexture getCustomVideoProcessInputSurfaceTexture(int width, int height, ZegoPublishChannel channel) {
        return super.getCustomVideoProcessInputSurfaceTexture(width, height, channel);
    }


    public interface CustomVideoProcessCallBack {
        void onCreatedBeautyKit(TEBeautyKit beautyKit);
    }
}
