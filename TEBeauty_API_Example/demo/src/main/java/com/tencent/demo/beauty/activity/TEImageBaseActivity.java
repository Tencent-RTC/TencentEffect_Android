package com.tencent.demo.beauty.activity;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.demo.AppConfig;
import com.tencent.demo.R;
import com.tencent.demo.opengl.gles.SimpleGLThread;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.XmagicConstant;

/**
 * 图片美颜页面
 */
public class TEImageBaseActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    public static String INPUT_DATA_NAME = "input_data_name";
    private ImageView mImageView = null;
    private volatile Bitmap mProcessedBitmap;

    private SimpleGLThread mHandler;

    protected Bitmap mOriginalBitmap;
    private XmagicApi mXmagicApi = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.te_beauty_activity_image_base_layout);
        mImageView = findViewById(R.id.te_image_layout_image_view);
        ((CheckBox) findViewById(R.id.switch_whiten)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_smooth)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_filter)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.switch_thin_face_nature)).setOnCheckedChangeListener(this);
        initOpenGL();
        this.initXMagicAPI();
        mOriginalBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test);
        setImageBitmap(mOriginalBitmap);
    }

    private void initXMagicAPI() {
        mXmagicApi = new XmagicApi(this, AppConfig.resPathForSDK);
        if (AppConfig.isEnableDowngradePerformance) {
            mXmagicApi.enableHighPerformance();
        }
        mXmagicApi.setXmagicLogLevel(Log.ERROR);//the default log level is Log.WARN
    }


    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.destroy(() -> {
                if (mXmagicApi != null) {
                    mXmagicApi.onPause();
                    mXmagicApi.onDestroy();
                }
            });
        }
        super.onDestroy();
    }

    private void initOpenGL() {
        mHandler = new SimpleGLThread(null, "OffscreenRender", 720, 1280, null);
        mHandler.waitDone();
    }

    protected void processAndShowBitmap() {
        if (mOriginalBitmap == null) {
            return;
        }
        mHandler.postJob(() -> {
            mProcessedBitmap = mXmagicApi.process(mOriginalBitmap, true);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setImageBitmap(mProcessedBitmap);
            });
        });
    }

    private void setImageBitmap(Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
    }


    public void onCheckedChanged(CompoundButton buttonView, boolean checked){
        switch (buttonView.getId()){
            case R.id.switch_whiten:
                mXmagicApi.setEffect(XmagicConstant.EffectName.BEAUTY_WHITEN, checked? (int) (80) : 0, null, null);
                break;
            case R.id.switch_smooth:
                mXmagicApi.setEffect(XmagicConstant.EffectName.BEAUTY_SMOOTH, checked? (int) (80) : 0, null, null);
                break;
            case R.id.switch_filter:
                mXmagicApi.setEffect(XmagicConstant.EffectName.EFFECT_LUT, checked? (int) (80) : 0, checked? (AppConfig.lutFilterPath + "/baixi_lf.png") : null, null);
                break;
            case R.id.switch_thin_face_nature:
                mXmagicApi.setEffect(XmagicConstant.EffectName.BEAUTY_FACE_NATURE, checked? (int) (80) : 0, null, null);
                break;
        }
        processAndShowBitmap();
    }

}
