package com.tencent.demo.beauty.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.demo.AppConfig;
import com.tencent.demo.R;
import com.tencent.demo.opengl.view.CustomTextureProcessor;
import com.tencent.demo.opengl.view.GLImageView;
import com.tencent.demo.utils.BitmapUtil;
import com.tencent.demo.utils.UriUtils;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelViewResModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.effect.beautykit.view.panelview.TEPanelViewCallback;
import com.tencent.xmagic.XmagicConstant;

import java.io.File;
import java.util.List;

/**
 * 图片美颜页面
 */
public class TEImageBaseActivity2 extends AppCompatActivity implements TEPanelViewCallback, CustomTextureProcessor {

    private static final String TAG = TEImageBaseActivity2.class.getName();
    private GLImageView mImageView = null;

    protected Bitmap mOriginalBitmap;
    private TEUIProperty mCustomProperty = null;
    private TEPanelView mTEPanelView;
    protected LinearLayout mPanelLayout = null;
    protected TEBeautyKit mBeautyKit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.te_beauty_activity_image_base_layout2);
        mImageView = findViewById(R.id.te_image_layout_image_view);
        mPanelLayout = findViewById(R.id.te_camera_layout_beauty_panel_layout);


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        mOriginalBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test, options);

        mImageView.setData(mOriginalBitmap);
        mImageView.setCustomTextureProcessor(this);


        TEBeautyKit.create(this.getApplicationContext(), XmagicConstant.EffectMode.PRO, beautyKit -> {
            mBeautyKit = beautyKit;
            initBeautyView(beautyKit);
        });
    }

    public void initBeautyView(TEBeautyKit beautyKit) {
        TEPanelViewResModel resModel = new TEPanelViewResModel();
        String combo = "S1_07";
        resModel.beauty = "beauty_panel/" + combo + "/beauty.json";
        resModel.lut = "beauty_panel/" + combo + "/lut.json";
        resModel.beautyBody = "beauty_panel/" + combo + "/beauty_body.json";
        resModel.motion = "beauty_panel/" + combo + "/motions.json";
        resModel.lightMakeup = "beauty_panel/" + combo + "/light_makeup.json";
        resModel.segmentation = "beauty_panel/" + combo + "/segmentation.json";
        TEUIConfig.getInstance().setTEPanelViewRes(resModel);

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
    public void onGLContextCreated() {
    }

    @Override
    public int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight) {
        if (mBeautyKit == null) {
            return textureId;
        }
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
        if (mImageView != null) {
            mImageView.start();
        }
        if (mBeautyKit != null) {
            mBeautyKit.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mImageView != null) {
            mImageView.pause();
        }
        if (mBeautyKit != null) {
            mBeautyKit.onPause();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            mImageView.release();
        }
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
    public void onUpdateEffected(List<TEUIProperty.TESDKParam> sdkParams) {

    }

    @Override
    public void onEffectStateChange(TEBeautyKit.EffectState effectState) {

    }

    @Override
    public void onTitleClick(TEUIProperty uiProperty) {

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
