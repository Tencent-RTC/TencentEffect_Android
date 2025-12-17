package com.tencent.demo.beauty.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import android.widget.Toast;
import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;
import com.tencent.demo.AppConfig;
import com.tencent.demo.R;

import com.tencent.demo.beauty.view.TETitleBar;

import com.tencent.demo.opengl.view.CameraSize;
import com.tencent.demo.opengl.view.CustomTextureProcessor;
import com.tencent.demo.opengl.view.GLCameraXView;
import com.tencent.demo.utils.BitmapUtil;
import com.tencent.demo.utils.UriUtils;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelViewResModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam;
import com.tencent.effect.beautykit.utils.LogUtils;

import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.xmagic.XmagicConstant.FeatureName;
import com.tencent.effect.beautykit.view.panelview.TEPanelViewCallback;
import java.io.File;
import java.util.List;

public class TECameraBaseActivity extends AppCompatActivity implements TEPanelViewCallback, CustomTextureProcessor,TETitleBar.TETitleBarClickListener {

    private static final String TAG = TECameraBaseActivity.class.getName();

    //Temporary storage of custom segmentation TEUIProperty.
    private TEUIProperty mCustomProperty = null;
    private TEPanelView mTEPanelView;
    protected LinearLayout mPanelLayout = null;
    private GLCameraXView mCameraXView = null;
    protected TEBeautyKit mBeautyKit;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.te_beauty_activity_camera_base_layout);
        TETitleBar mTitleBar = findViewById(R.id.te_camera_layout_title_bar);
        mTitleBar.setTeTitleBarClickListener(this);
        mPanelLayout = findViewById(R.id.te_camera_layout_beauty_panel_layout);
        mCameraXView = findViewById(R.id.te_camera_layout_camerax_view);
        mCameraXView.setCameraSize(CameraSize.size1080);
        mCameraXView.setCustomTextureProcessor(this);
        findViewById(R.id.te_camera_layout_save_btn).setOnClickListener(view -> {
            saveCurrentBeautyParams();
        });
        TEBeautyKit.create(this.getApplicationContext(), XmagicConstant.EffectMode.PRO, beautyKit -> {
            mBeautyKit = beautyKit;
            initBeautyView(beautyKit);
        });
    }

    private void saveCurrentBeautyParams() {
        if (mBeautyKit != null) {
            String json = mBeautyKit.exportInUseSDKParam();
            Log.e("打印日志信息","美颜参数   "+json);
            if (json != null) {
                getSharedPreferences("demo_settings", Context.MODE_PRIVATE).edit()
                        .putString("current_beauty_params", json).commit();
                Toast.makeText(this, "Current beauty params saved.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Current beauty params save failed.", Toast.LENGTH_LONG).show();
            }
            Log.d(TAG, "saveCurrentBeautyParams: json="+json);
        }
    }

    public void initBeautyView(TEBeautyKit beautyKit){

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
        mTEPanelView.setTEPanelViewCallback(this);
        mTEPanelView.setupWithTEBeautyKit(beautyKit);

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
        mCustomProperty = uiProperty;
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, AppConfig.PICK_CONTENT_ALL);
        startActivityForResult(intentToPickPic, AppConfig.TE_CHOOSE_PHOTO_SEG_CUSTOM);
    }

    @Override
    public void onCameraClick() {
    }

    @Override
    public void onUpdateEffected(List<TESDKParam> sdkParams) {

    }

    @Override
    public void onEffectStateChange(TEBeautyKit.EffectState effectState) {

    }

    @Override
    public void onTitleClick(TEUIProperty uiProperty) {

    }


    @Override
    public void onGLContextCreated() {
    }

    @Override
    public int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight) {
        return mBeautyKit.process(textureId, textureWidth, textureHeight);
    }

    @Override
    public void onGLContextDestroy() {
        if (mBeautyKit != null) {
            mBeautyKit.onDestroy();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraXView != null) {
            mCameraXView.startPreview();
        }
        if (mBeautyKit != null) {
            mBeautyKit.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraXView != null) {
            mCameraXView.stopPreview();
        }
        if (mBeautyKit != null) {
            mBeautyKit.onPause();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraXView != null) {
            mCameraXView.release();
        }
    }

    @Override
    public void onCameraSwitch() {
        if (mCameraXView != null) {
            mCameraXView.switchCamera();
        }
    }

    @Override
    public void onPerformanceSwitchTurnOn(boolean isTurnOn) {
    }

    @Override
    public void onSwitchResolution(TETitleBar.RESOLUTION resolution) {
        LogUtils.i(TAG, "onSwitchResolution: " + resolution.name());
        Size size = null;
        switch (resolution) {
            case R540P:
                size = CameraSize.size540;
                break;
            case R720P:
                size = CameraSize.size720;
                break;
            case R1080P:
                size = CameraSize.size1080;
                break;
        }
        if (mCameraXView != null && size != null) {
            mCameraXView.setCameraSize(size);
            mCameraXView.previewAgain();
        }

    }

    @Override
    public void onClickPickBtn() {
    }

    @Override
    public void onOnlyBeautyWhiteOnSkin(boolean isTurnOn) {
        LogUtils.d(TAG, "onOnlyBeautyWhiteOnSkin" + isTurnOn);
        mBeautyKit.setFeatureEnableDisable(FeatureName.WHITEN_ONLY_SKIN_AREA, isTurnOn);
    }

    @Override
    public void onSmartBeautyTurnOn(boolean isTurnOn) {
        LogUtils.d(TAG, "onSmartBeautyTurnOn" + isTurnOn);
        mBeautyKit.setFeatureEnableDisable(FeatureName.SMART_BEAUTY, isTurnOn);
    }

    @Override
    public void onCropTextureSwitchTurnOn(boolean isCrop) {
        if (mCameraXView != null) {
            mCameraXView.setCropRatio(isCrop ? AppConfig.getInstance().cropRatio : 1f);
        }
    }

    @Override
    public void onFaceBlockSwitchTurnOn(boolean isTurnOn) {
        mBeautyKit.setFeatureEnableDisable(FeatureName.SEGMENTATION_FACE_BLOCK, isTurnOn);
    }

    @Override
    public void onEnhancedModeSwitchTurnOn(boolean isEnable) {
        if (mBeautyKit.enableEnhancedMode(isEnable)) {
            List<TESDKParam> usedProperties = mBeautyKit.getInUseSDKParamList();
            if (usedProperties == null) {
                return;
            }
            mBeautyKit.setEffectList(usedProperties);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String filePath = null;
            if (data != null) {
                filePath = UriUtils.getFilePathByUri(this, data.getData());
            } else {
                LogUtils.e(TAG, "the data and filePath is null ");
                return;
            }
            if (requestCode == AppConfig.TE_CHOOSE_PHOTO_SEG_CUSTOM) {  //custom segmentation
                setCustomSegParam(filePath);
            }
        } else {
            mCustomProperty = null;
        }
    }

    private void setCustomSegParam(String filePath) {
        if (mCustomProperty != null && mCustomProperty.sdkParam != null && mCustomProperty.sdkParam.extraInfo != null && (!TextUtils.isEmpty(filePath)) && new File(filePath).exists()) {
            if (filePath.endsWith("jpg") || filePath.endsWith("JPG") || filePath.endsWith("PNG") || filePath.endsWith("png") ||
                    filePath.endsWith("jpeg") || filePath.endsWith("JPEG")) {
                BitmapUtil.compressImage(getApplicationContext(), filePath, imgPath -> {
                    mCustomProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_TYPE, TEUIProperty.TESDKParam.EXTRA_INFO_BG_TYPE_IMG);
                    mCustomProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH, imgPath);
                    mBeautyKit.setEffect(mCustomProperty.sdkParam);
                    runOnUiThread(() -> {
                        mTEPanelView.checkPanelViewItem(mCustomProperty);
                        mCustomProperty = null;
                    });
                });
            } else {
                mCustomProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_TYPE, TEUIProperty.TESDKParam.EXTRA_INFO_BG_TYPE_VIDEO);
                mCustomProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH, filePath);
                mBeautyKit.setEffect(mCustomProperty.sdkParam);
                runOnUiThread(() -> {
                    mTEPanelView.checkPanelViewItem(mCustomProperty);
                    mCustomProperty = null;
                });
            }
        } else {
            mCustomProperty = null;
        }
    }
}
