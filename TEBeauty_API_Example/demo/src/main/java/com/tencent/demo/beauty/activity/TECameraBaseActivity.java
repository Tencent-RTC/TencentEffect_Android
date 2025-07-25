package com.tencent.demo.beauty.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.demo.AppConfig;
import com.tencent.demo.R;
import com.tencent.demo.opengl.view.CameraSize;
import com.tencent.demo.opengl.view.CustomTextureProcessor;
import com.tencent.demo.opengl.view.GLCameraXView;
import com.tencent.demo.utils.BitmapUtil;
import com.tencent.demo.utils.LogUtils;
import com.tencent.demo.utils.UriUtils;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.XmagicApi.XmagicAIDataListener;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.xmagic.XmagicConstant.EffectName;
import com.tencent.xmagic.XmagicConstant.FeatureName;
import com.tencent.xmagic.bean.TEBodyData;
import com.tencent.xmagic.bean.TEFaceData;
import com.tencent.xmagic.bean.TEHandData;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TECameraBaseActivity extends AppCompatActivity implements CustomTextureProcessor, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = TECameraBaseActivity.class.getName();

    protected LinearLayout mPanelLayout = null;
    private GLCameraXView mCameraXView = null;
    private XmagicApi mXmagicApi;
    private TextView textViewFaceCount;
    private int currentFaceCount = 0;
    private SeekBar seekBarSegmentation = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.te_beauty_activity_camera_base_layout);
        mPanelLayout = findViewById(R.id.te_camera_layout_beauty_panel_layout);
        mCameraXView = findViewById(R.id.te_camera_layout_camerax_view);
        mCameraXView.setCameraSize(CameraSize.size1080);
        mCameraXView.setCustomTextureProcessor(this);
        textViewFaceCount = findViewById(R.id.textview_face_count);
        ((CheckBox) findViewById(R.id.switch_face_detect)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_whiten)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_smooth)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_filter)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_thin_face_nature)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_lipstick_0)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_lipstick_1)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_lipstick_none)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_light_makeup_0)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_light_makeup_none)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_makeup)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_motion)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_motion_merge)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_segmentation)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_custom_segmentation)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.radio_motion_none)).setOnCheckedChangeListener(this);
        ((RadioButton)findViewById(R.id.radio_green_screen_v1)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_face_occlusion_detect)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_smart_beauty_for_men_and_baby)).setOnCheckedChangeListener(this);
        this.seekBarSegmentation = ((SeekBar)findViewById(R.id.seekbar_segmentation));
        //用于测试 背景模糊强度
        this.seekBarSegmentation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Map<String,String> extrainfo = new ArrayMap<>();
                extrainfo.put("segType","blur_background");
                mXmagicApi.setEffect(EffectName.EFFECT_SEGMENTATION, progress, AppConfig.motionResPath + "/segmentMotionRes/video_segmentation_blur_45", extrainfo);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ((Switch) findViewById(R.id.te_camera_layout_switch_camera)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mCameraXView != null) {
                mCameraXView.switchCamera();
            }
        });
        initXMagicAPI();
    }

    private void initXMagicAPI() {
        mXmagicApi = new XmagicApi(this, AppConfig.effectMode, AppConfig.resPathForSDK);
        mXmagicApi.setXmagicLogLevel(Log.ERROR);//the default log level is Log.WARN
        mXmagicApi.setAIDataListener(new XmagicAIDataListener() {
            @Override
            public void onFaceDataUpdated(List<TEFaceData> list) {
                if (list != null) {
                    currentFaceCount = list.size();
                } else {
                    currentFaceCount = 0;
                }
                runOnUiThread(() -> {
                    textViewFaceCount.setText("Face Count: " + currentFaceCount);
                });
            }

            @Override
            public void onHandDataUpdated(List<TEHandData> list) {

            }

            @Override
            public void onBodyDataUpdated(List<TEBodyData> list) {

            }

            @Override
            public void onAIDataUpdated(String s) {

            }
        });

    }

    @Override
    public void onGLContextCreated() {
    }

    @Override
    public int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight) {
        return mXmagicApi.process(textureId, textureWidth, textureHeight);
    }

    @Override
    public void onGLContextDestroy() {
        mXmagicApi.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mCameraXView.startPreview();
        mXmagicApi.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraXView.stopPreview();
        mXmagicApi.onPause();
    }


    @Override
    protected void onDestroy() {
        mCameraXView.release();
        //do not call "mXmagicApi.onDestroy()" on UI thread, call it on OpenGL thread："onGLContextDestroy()"
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        switch (compoundButton.getId()){
            case R.id.switch_face_occlusion_detect:
                mXmagicApi.setFeatureEnableDisable(FeatureName.SEGMENTATION_FACE_BLOCK, checked);
                break;
            case R.id.switch_smart_beauty_for_men_and_baby:
                mXmagicApi.setFeatureEnableDisable(FeatureName.SMART_BEAUTY, checked);
                break;
            case R.id.switch_face_detect:
                textViewFaceCount.setVisibility(checked? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.switch_whiten:
                mXmagicApi.setEffect(EffectName.BEAUTY_WHITEN, checked? 80 : 0, null, null);
                break;
            case R.id.switch_smooth:
                mXmagicApi.setEffect(EffectName.BEAUTY_SMOOTH, checked? 80 : 0, null, null);
                break;
            case R.id.switch_filter:
                mXmagicApi.setEffect(EffectName.EFFECT_LUT, checked? 80 : 0, checked? (AppConfig.lutFilterPath + "/baixi_lf.png") : null, null);
                break;
            case R.id.switch_thin_face_nature:
                mXmagicApi.setEffect(EffectName.BEAUTY_FACE_NATURE, checked? 80 : 0, null, null);
                break;
            case R.id.radio_lipstick_0:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.BEAUTY_MOUTH_LIPSTICK, 80, "/images/beauty/lips_fuguhong.png", null);
                }
                break;
            case R.id.radio_lipstick_1:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.BEAUTY_MOUTH_LIPSTICK, 80, "/images/beauty/lips_shanhuju.png", null);
                }
                break;
            case R.id.radio_lipstick_none:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.BEAUTY_MOUTH_LIPSTICK, 0, null, null);
                }
                break;
            case R.id.radio_light_makeup_0:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.EFFECT_LIGHT_MAKEUP, 80, AppConfig.motionResPath + "/light_makeup/light_baixi", null);
                }
                break;
            case R.id.radio_light_makeup_none:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.EFFECT_LIGHT_MAKEUP, 0, null, null);
                }
                break;
            case R.id.radio_makeup:
                if (checked) {
                    //extraInfo is optional
                    Map<String, String> extraInfo = new HashMap<>();
                    extraInfo.put("makeupLutStrength", "60");
                    mXmagicApi.setEffect(EffectName.EFFECT_MAKEUP, 100, AppConfig.motionResPath + "/makeupRes/video_makeup_xuemei", extraInfo);
                }
                break;
            case R.id.radio_motion:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.EFFECT_MOTION, 0, AppConfig.motionResPath + "/2dMotionRes/video_keaituya", null);
                }
                break;
            case R.id.radio_green_screen_v1:
                if (checked) {
                    Intent intentToPickPic = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, AppConfig.PICK_CONTENT_ALL);
                    startActivityForResult(intentToPickPic, AppConfig.TE_CHOOSE_PHOTO_SEG_GREEN_SCREEN);
                }
                break;
            case R.id.radio_segmentation:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.EFFECT_SEGMENTATION, 45, AppConfig.motionResPath + "/segmentMotionRes/video_segmentation_blur_45", null);
                    this.seekBarSegmentation.setVisibility(View.VISIBLE);
                    this.seekBarSegmentation.setProgress(45);
                }else {
                    this.seekBarSegmentation.setVisibility(View.GONE);
                }
                break;
            case R.id.radio_custom_segmentation:
                if (checked) {
                    Intent intentToPickPic = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, AppConfig.PICK_CONTENT_ALL);
                    startActivityForResult(intentToPickPic, AppConfig.TE_CHOOSE_PHOTO_SEG_CUSTOM);
                }
                break;
            case R.id.radio_motion_merge:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.EFFECT_SEGMENTATION, 0, AppConfig.motionResPath + "/segmentMotionRes/video_segmentation_blur_75", null);
                    Map<String, String> extraInfo = new HashMap<>();
                    extraInfo.put("mergeWithCurrentMotion", "true");
                    mXmagicApi.setEffect(EffectName.EFFECT_MOTION, 0, AppConfig.motionResPath + "/2dMotionRes/video_keaituya", extraInfo);
                }
                break;
            case R.id.radio_motion_none:
                if (checked) {
                    mXmagicApi.setEffect(EffectName.EFFECT_MOTION, 0, null, null);
                }
                break;
            default:
                break;
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
            } else if(requestCode == AppConfig.TE_CHOOSE_PHOTO_SEG_GREEN_SCREEN){
                setGreenScreenParam(filePath);
            }
        }
    }

    private void setCustomSegParam(String filePath) {
        if (!TextUtils.isEmpty(filePath) && new File(filePath).exists()) {
            if (filePath.endsWith("jpg") || filePath.endsWith("JPG") || filePath.endsWith("PNG") || filePath.endsWith("png") ||
                    filePath.endsWith("jpeg") || filePath.endsWith("JPEG")) {
                BitmapUtil.compressImage(getApplicationContext(), filePath, imgPath -> {
                    Map<String, String> extraInfo = new HashMap<>();
                    extraInfo.put("segType", "custom_background");
                    extraInfo.put("bgType", "0");
                    extraInfo.put("bgPath", imgPath);
                    mXmagicApi.setEffect(EffectName.EFFECT_SEGMENTATION, 0, AppConfig.motionResPath + "/segmentMotionRes/video_empty_segmentation", extraInfo);
                });
            }
        }
    }


    private void setGreenScreenParam(String filePath) {
        if (!TextUtils.isEmpty(filePath) && new File(filePath).exists()) {
            if (filePath.endsWith("jpg") || filePath.endsWith("JPG") || filePath.endsWith("PNG") || filePath.endsWith("png") ||
                    filePath.endsWith("jpeg") || filePath.endsWith("JPEG")) {
                BitmapUtil.compressImage(getApplicationContext(), filePath, imgPath -> {
                    Map<String, String> extraInfo = new HashMap<>();
                    extraInfo.put("keyColor","#ff0000");   //默认是绿色
                    extraInfo.put("tex_protect_rect","[0.0,0.0,0.3,0.3]");   //0-1.0
//                    extraInfo.put("green_params", "[0.413,0.5, 1.0, 1.0]");    //分别对应 抠图强度，羽化，去绿幕，增亮
                    extraInfo.put("segType", "green_background");
                    extraInfo.put("bgType", "0");
                    extraInfo.put("bgPath", imgPath);
                    mXmagicApi.setEffect(EffectName.EFFECT_SEGMENTATION, 0, AppConfig.motionResPath + "/segmentMotionRes/video_greenscreen", extraInfo);
                });
            }
        }
    }
}
