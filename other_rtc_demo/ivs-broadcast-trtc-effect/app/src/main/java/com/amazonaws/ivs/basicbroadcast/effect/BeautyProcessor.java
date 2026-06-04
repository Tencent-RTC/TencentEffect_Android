package com.amazonaws.ivs.basicbroadcast.effect;

import android.content.Context;
import android.util.Log;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;
import com.tencent.xmagic.XmagicConstant;

public class BeautyProcessor implements TRTCCloudListener.TRTCVideoFrameListener {

    private static String TAG = BeautyProcessor.class.getName();
    private Context context = null;

    public TEBeautyKit teBeautyKit = null;
    private BeautyProcessorCallBack processorCallBack;

    public BeautyProcessor(Context context, BeautyProcessorCallBack processorCallBack) {
        this.context = context;
        this.processorCallBack = processorCallBack;
    }

    @Override
    public void onGLContextCreated() {
        Log.i(TAG, "onGLContextCreated");
        this.teBeautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);
        if (processorCallBack != null) {
            processorCallBack.onCreatedBeautyKit(this.teBeautyKit);
        }
    }

    @Override
    public int onProcessVideoFrame(TRTCCloudDef.TRTCVideoFrame trtcVideoFrame, TRTCCloudDef.TRTCVideoFrame trtcVideoFrame1) {
        trtcVideoFrame1.texture.textureId =  teBeautyKit.process(trtcVideoFrame.texture.textureId, trtcVideoFrame.width, trtcVideoFrame.height);
        Log.i(TAG, "textureWidth " + trtcVideoFrame.width + "   textureHeight " + trtcVideoFrame.height);
        return 0;
    }

    @Override
    public void onGLContextDestory() {
        Log.i(TAG, "onGLContextDestroy");
        if (this.teBeautyKit != null) {
            this.teBeautyKit.onDestroy();
            this.teBeautyKit = null;
        }
    }



    public interface BeautyProcessorCallBack {
        void onCreatedBeautyKit(TEBeautyKit beautyKit);
    }


}
