package com.tencent.effect.agora.demo.beauty;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tencent.effect.agora.demo.observer.AgoraVideoFrameObserverListener;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.DeviceDirection;
import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.xmagic.GlUtil;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.xmagic.bean.TEImageOrientation;
import com.tencent.effect.agora.demo.observer.converter.AgoraVideoFrameConvertListener;

public class TencentBeautyProcessor implements TEBeautyKit.EventListener, AgoraVideoFrameObserverListener {


    private static final String TAG = TencentBeautyProcessor.class.getName();
    private volatile TEBeautyKit beautyKit;
    private final Context context;

    private TEPanelView beautyPanel;
    private boolean isFrontCamera = true;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TencentBeautyProcessor(Context context) {
        this.context = context;
    }




    public void setBeautyPanel(TEPanelView beautyPanel) {
        this.beautyPanel = beautyPanel;
        if (this.beautyKit != null) {
            this.beautyPanel.setupWithTEBeautyKit(this.beautyKit);
        }
    }


    public TEBeautyKit getBeautyKit(){
        return this.beautyKit;
    }


    @Override
    public void onDeviceDirectionChanged(int orientation, DeviceDirection deviceDirection) {
        this.beautyKit.setImageOrientation(getOrientation(deviceDirection));
    }

    private TEImageOrientation getOrientation(DeviceDirection deviceDirection) {
        if (this.isFrontCamera) {
            switch (deviceDirection.angle) {
                case 0:
                    return TEImageOrientation.ROTATION_0;
                case 90:
                    return TEImageOrientation.ROTATION_90;
                case 180:
                    return TEImageOrientation.ROTATION_180;
                case 270:
                    return TEImageOrientation.ROTATION_270;
            }
        } else {
            switch (deviceDirection.angle) {
                case 0:
                    return TEImageOrientation.ROTATION_0;
                case 90:
                    return TEImageOrientation.ROTATION_270;
                case 180:
                    return TEImageOrientation.ROTATION_180;
                case 270:
                    return TEImageOrientation.ROTATION_90;
            }
        }
        return TEImageOrientation.ROTATION_0;
    }


    @Override
    public void onCameraChange(boolean isFrontCamera) {
        this.isFrontCamera = isFrontCamera;
    }

    @Override
    public void onGLContextCreated() {
        TEBeautyKit.create(context, XmagicConstant.EffectMode.PRO,kit -> mainHandler.post(() -> {
            beautyKit = kit;
            beautyKit.setEventListener(TencentBeautyProcessor.this);
            if (beautyPanel != null) {
                beautyPanel.setupWithTEBeautyKit(beautyKit);
            }
        }));
    }

    @Override
    public int onProcessVideoFrame(int textureId, int width, int height) {
        if (beautyKit == null) {
            return textureId;
        }
        return beautyKit.process(textureId, width, height);
    }

    @Override
    public void onGLContextDestroy() {
        Log.e(TAG, "onDestroy  " +Thread.currentThread().getName());
        if (beautyKit != null) {
            beautyKit.onDestroy();
            beautyKit = null;
        }
    }
}
