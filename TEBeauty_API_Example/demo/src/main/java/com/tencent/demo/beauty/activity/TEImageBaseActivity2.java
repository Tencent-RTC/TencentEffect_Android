package com.tencent.demo.beauty.activity;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.demo.AppConfig;
import com.tencent.demo.R;
import com.tencent.demo.opengl.gles.SimpleGLThread;
import com.tencent.demo.opengl.view.CustomTextureProcessor;
import com.tencent.demo.opengl.view.GLImageView;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.XmagicConstant;

/**
 * 图片美颜页面
 */
public class TEImageBaseActivity2 extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    public static String INPUT_DATA_NAME = "input_data_name";
    private GLImageView mImageView = null;

    protected Bitmap mOriginalBitmap;
    private XmagicApi mXmagicApi = null;

    private SeekBar strengthSeekbar = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.te_beauty_activity_image_base_layout2);
        mImageView = findViewById(R.id.te_image_layout_image_view);
        ((CheckBox) findViewById(R.id.switch_whiten)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_smooth)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_filter)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_thin_face_nature)).setOnCheckedChangeListener(this);

        this.strengthSeekbar = findViewById(R.id.xmagic_strength_seekbar);
        this.strengthSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mXmagicApi.setEffect(XmagicConstant.EffectName.BEAUTY_SMOOTH, progress, null, null);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        mOriginalBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test,options);

        mImageView.setData(mOriginalBitmap);
        mImageView.setCustomTextureProcessor(new CustomTextureProcessor() {
            @Override
            public void onGLContextCreated() {
                initXMagicAPI();
            }

            @Override
            public int onCustomProcessTexture(int textureId, int textureWidth, int textureHeight) {
                if (mXmagicApi == null) {
                    return textureId;
                } else {
                    return mXmagicApi.process(textureId, textureWidth, textureHeight);
                }
            }

            @Override
            public void onGLContextDestroy() {
                if (mXmagicApi != null) {
                    mXmagicApi.onPause();
                    mXmagicApi.onDestroy();
                }
            }
        });
    }

    private void initXMagicAPI() {
        mXmagicApi = new XmagicApi(this, AppConfig.effectMode, AppConfig.resPathForSDK);
        mXmagicApi.setXmagicLogLevel(Log.ERROR);//the default log level is Log.WARN
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImageView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageView.pause();
    }

    @Override
    protected void onDestroy() {
        mImageView.release();
        super.onDestroy();
    }


    public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
        switch (buttonView.getId()) {
            case R.id.switch_whiten:
                mXmagicApi.setEffect(XmagicConstant.EffectName.BEAUTY_WHITEN, checked ? (int) (80) : 0, null, null);
                break;
            case R.id.switch_smooth:
                mXmagicApi.setEffect(XmagicConstant.EffectName.BEAUTY_SMOOTH, checked ? (int) (80) : 0, null, null);
                break;
            case R.id.switch_filter:
                mXmagicApi.setEffect(XmagicConstant.EffectName.EFFECT_LUT, checked ? (int) (80) : 0, checked ? (AppConfig.lutFilterPath + "/baixi_lf.png") : null, null);
                break;
            case R.id.switch_thin_face_nature:
                mXmagicApi.setEffect(XmagicConstant.EffectName.BEAUTY_FACE_NATURE, checked ? (int) (80) : 0, null, null);
                break;
        }

    }

}
