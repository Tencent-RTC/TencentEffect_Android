package com.tencent.effect.demo.zego;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelViewResModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.effect.beautykit.view.panelview.TEPanelViewCallback;
import com.tencent.thirdbeauty.xmagic.CustomPropertyManager;

import java.util.List;

import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomVideoProcessConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;

public class EffectActivity extends AppCompatActivity implements TEPanelViewCallback {

    private static final String TAG = EffectActivity.class.getName();

    private ZegoExpressEngine expressEngine;
    private ZegoCanvas zegoCanvas = null;

    private TEPanelView mTEPanelView = null;
    private LinearLayout mPanelLayout = null;
    private CustomVideoProcessHandler processHandler = null;
    private TEBeautyKit mBeautyKit = null;
    private final CustomPropertyManager customPropertyManager = new CustomPropertyManager();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.effect_layout);
        TextureView videoLayout = findViewById(R.id.video_view);
        this.mPanelLayout = findViewById(R.id.panel_layout);
        zegoCanvas = new ZegoCanvas(videoLayout);
        ((Switch) findViewById(R.id.camera_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (expressEngine != null && processHandler != null) {
                    expressEngine.useFrontCamera(isChecked);
                    processHandler.setFrontCamera(isChecked);
                }
            }
        });
        this.initBeautyView();
        this.createEngine();
    }


    @Override
    protected void onResume() {
        super.onResume();
        expressEngine.startPreview(this.zegoCanvas);
    }


    @Override
    protected void onPause() {
        super.onPause();
        expressEngine.stopPreview();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        enableEffect(false);
    }


    void createEngine() {
        ZegoEngineProfile profile = new ZegoEngineProfile();
        profile.appID = LicenseConstant.appID;  // 请通过官网注册获取，格式为：1234567890L
        profile.appSign = LicenseConstant.appSign; //请通过官网注册获取，格式为：@"0123456789012345678901234567890123456789012345678901234567890123"（共64个字符）
        profile.scenario = ZegoScenario.BROADCAST;  // 指定使用直播场景 (请根据实际情况填写适合你业务的场景)
        profile.application = getApplication();
        expressEngine = ZegoExpressEngine.createEngine(profile, null);
        expressEngine.useFrontCamera(true);
//        expressEngine.setAppOrientation(ZegoOrientation.ORIENTATION_90);

        enableEffect(true);


    }


    private void enableEffect(boolean enable) {
        if (expressEngine == null) {
            return;
        }
        ZegoCustomVideoProcessConfig config = new ZegoCustomVideoProcessConfig();
        config.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
        expressEngine.enableCustomVideoProcessing(enable, config);
        if (enable) {
            setProcessHandler();
        }
    }

    private void setProcessHandler() {
        processHandler = new CustomVideoProcessHandler(this, expressEngine);
        processHandler.setCustomVideoProcessCallBack(beautyKit -> new Handler(Looper.getMainLooper()).post(() -> {
            mBeautyKit = beautyKit;
            mTEPanelView.setupWithTEBeautyKit(beautyKit);
        }));
        expressEngine.setCustomVideoProcessHandler(processHandler);
    }


    public void initBeautyView() {
        List<TEPanelDataModel> panelDataModels = TEUIConfig.getInstance().getPanelDataList();
        panelDataModels.clear();
        //根据套餐功能添加对应的JSON
        TEPanelDataModel template = new TEPanelDataModel("beauty_panel/beauty_template.json", TEUIProperty.UICategory.BEAUTY_TEMPLATE);
        TEPanelDataModel beauty1 = new TEPanelDataModel("beauty_panel/beauty.json", TEUIProperty.UICategory.BEAUTY);
        TEPanelDataModel beauty2 = new TEPanelDataModel("beauty_panel/beauty_image.json", TEUIProperty.UICategory.BEAUTY);
        TEPanelDataModel beauty4 = new TEPanelDataModel("beauty_panel/beauty_shape.json", TEUIProperty.UICategory.BEAUTY);
        TEPanelDataModel beauty3 = new TEPanelDataModel("beauty_panel/beauty_makeup.json", TEUIProperty.UICategory.BEAUTY);


        TEPanelDataModel lut = new TEPanelDataModel("beauty_panel/lut.json", TEUIProperty.UICategory.LUT);
        TEPanelDataModel lightMakeup = new TEPanelDataModel("beauty_panel/light_makeup.json",
                TEUIProperty.UICategory.LIGHT_MAKEUP);
        TEPanelDataModel makeup = new TEPanelDataModel("beauty_panel/makeup.json", TEUIProperty.UICategory.MAKEUP);
        TEPanelDataModel motion = new TEPanelDataModel("beauty_panel/motion_2d.json", TEUIProperty.UICategory.MOTION);
        TEPanelDataModel motion2 = new TEPanelDataModel("beauty_panel/motion_3d.json", TEUIProperty.UICategory.MOTION);
        TEPanelDataModel motion_gesture = new TEPanelDataModel("beauty_panel/motion_gesture.json",
                TEUIProperty.UICategory.MOTION);
        TEPanelDataModel seg = new TEPanelDataModel("beauty_panel/segmentation.json", TEUIProperty.UICategory.SEGMENTATION);


        panelDataModels.add(template);
        panelDataModels.add(beauty1);
        panelDataModels.add(beauty2);
        panelDataModels.add(beauty4);
        panelDataModels.add(beauty3);
        panelDataModels.add(lut);

        panelDataModels.add(lightMakeup);
        panelDataModels.add(makeup);

        panelDataModels.add(motion);
        panelDataModels.add(motion2);
        panelDataModels.add(motion_gesture);
        panelDataModels.add(seg);

        mTEPanelView = new TEPanelView(this);

        SharedPreferences sp = getSharedPreferences("demo_settings", Context.MODE_PRIVATE);
        String savedParams = sp.getString("current_beauty_params", "");
        if (!savedParams.isEmpty()) {
            mTEPanelView.setLastParamList(savedParams);
        }

        mTEPanelView.showView(this);
        this.mPanelLayout.addView(mTEPanelView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onClickCustomSeg(TEUIProperty uiProperty) {
        if (mBeautyKit == null || customPropertyManager == null) {
            return;
        }
        customPropertyManager.setData(uiProperty, mBeautyKit, mTEPanelView);
        customPropertyManager.pickMedia(this, CustomPropertyManager.TE_CHOOSE_PHOTO_SEG_CUSTOM,
                CustomPropertyManager.PICK_CONTENT_ALL);
    }

    @Override
    public void onCameraClick() {

    }

    @Override
    public void onUpdateEffected(List<TEUIProperty.TESDKParam> sdkParams) {

    }

    @Override
    public void onEffectStateChange(TEBeautyKit.EffectState effectState) {

    }

    @Override
    public void onTitleClick(TEUIProperty uiProperty) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mBeautyKit == null || customPropertyManager == null) {
            return;
        }
        customPropertyManager.onActivityResult(this, requestCode, resultCode, data);
    }
}
