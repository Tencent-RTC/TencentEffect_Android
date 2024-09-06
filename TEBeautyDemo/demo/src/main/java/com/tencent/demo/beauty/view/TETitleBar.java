package com.tencent.demo.beauty.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.tencent.demo.R;
import com.tencent.demo.beauty.model.TEFeatureConfig;

/**
 * 标题栏view
 */
public class TETitleBar extends LinearLayout implements View.OnClickListener {

    private ImageView backBtn;
    private ImageView resolutionBtn;
    private ImageView pickBtn;
    private ImageView cameraSwitchBtn;

    private TextView titleTxt;


    private TETitleBarClickListener teTitleBarClickListener = null;

    public TETitleBar(Context context) {
        this(context, null);
    }

    public TETitleBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TETitleBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    private void initViews() {
        LayoutInflater.from(getContext()).inflate(R.layout.te_beauty_titlebar_layout, this, true);
        backBtn = findViewById(R.id.te_title_layout_back_btn);
        backBtn.setOnClickListener(this);
        titleTxt = findViewById(R.id.te_title_layout_title_txt);

        resolutionBtn = findViewById(R.id.te_title_layout_resolution_btn);
        resolutionBtn.setOnClickListener(this);
        pickBtn = findViewById(R.id.te_title_layout_pick_btn);
        pickBtn.setOnClickListener(this);
        cameraSwitchBtn = findViewById(R.id.te_title_layout_camera_switch_btn);
        cameraSwitchBtn.setOnClickListener(this);
    }

    public ImageView getResolutionBtn() {
        return resolutionBtn;
    }

    public ImageView getPickBtn() {
        return pickBtn;
    }

    public ImageView getCameraSwitchBtn() {
        return cameraSwitchBtn;
    }

    public ImageView getBackBtn() {
        return backBtn;
    }

    public TextView getTitleTxt() {
        return titleTxt;
    }

    public void setTitleTxt(@StringRes int titleTxt, @ColorInt int textColor) {
        this.titleTxt.setText(titleTxt);
        this.titleTxt.setTextColor(textColor);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.te_title_layout_back_btn) {
            ((Activity) getContext()).finish();
        } else if (v.getId() == R.id.te_title_layout_resolution_btn) {
            showResolutionPop(v);
        } else if (v.getId() == R.id.te_title_layout_pick_btn) {
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onClickPickBtn();
            }
        } else if (v.getId() == R.id.te_title_layout_camera_switch_btn) {
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onCameraSwitch();
            }
        }
    }

    public void setTeTitleBarClickListener(TETitleBarClickListener titleBarClickListener) {
        teTitleBarClickListener = titleBarClickListener;
    }


    private void showResolutionPop(View targetView) {
        TEResolutionPopupWindow resolutionPopupWindow = new TEResolutionPopupWindow(new TETitleBarClickListener() {
            @Override
            public void onCameraSwitch() {

            }

            @Override
            public void onPerformanceSwitchTurnOn(boolean isTurnOn) {
                TEFeatureConfig.getInstance().isOnPerformanceSwitch = isTurnOn;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onPerformanceSwitchTurnOn(isTurnOn);
                }
            }

            @Override
            public void onSwitchResolution(RESOLUTION onSwitchResolution) {
                TEFeatureConfig.getInstance().resolution = onSwitchResolution;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onSwitchResolution(onSwitchResolution);
                }
            }

            @Override
            public void onClickPickBtn() {

            }

            @Override
            public void onEnhancedModeSwitchTurnOn(boolean isEnable) {
                TEFeatureConfig.getInstance().isOnEnhancedMode = isEnable;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onEnhancedModeSwitchTurnOn(isEnable);
                }
            }

            @Override
            public void onSmartBeautyTurnOn(boolean isTurnOn) {
                TEFeatureConfig.getInstance().isOnSmartBeautySwitch = isTurnOn;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onSmartBeautyTurnOn(isTurnOn);
                }
            }

            @Override
            public void onOnlyBeautyWhiteOnSkin(boolean isTurnOn) {
                TEFeatureConfig.getInstance().isOnWhiteSkinOnlySwitch = isTurnOn;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onOnlyBeautyWhiteOnSkin(isTurnOn);
                }
            }

            @Override
            public void onCropTextureSwitchTurnOn(boolean isCrop) {
                TEFeatureConfig.getInstance().isOnCropTextureSwitch = isCrop;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onCropTextureSwitchTurnOn(isCrop);
                }
            }

            @Override
            public void onFaceBlockSwitchTurnOn(boolean isTurnOn) {
                TEFeatureConfig.getInstance().isOnFaceBlockSwitch = isTurnOn;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onFaceBlockSwitchTurnOn(isTurnOn);
                }
            }
        });
        resolutionPopupWindow.showResolutionPop(targetView);
    }


    public interface TETitleBarClickListener {
        void onCameraSwitch();

        /**
         * 性能参数  开关回调
         *
         * @param isTurnOn
         */
        void onPerformanceSwitchTurnOn(boolean isTurnOn);

        void onSwitchResolution(RESOLUTION resolution);

        void onClickPickBtn();

        void onEnhancedModeSwitchTurnOn(boolean isEnable);

        void onSmartBeautyTurnOn(boolean isTurnOn);

        void onOnlyBeautyWhiteOnSkin(boolean isTurnOn);

        void onCropTextureSwitchTurnOn(boolean isCrop);

        void onFaceBlockSwitchTurnOn(boolean isTurnOn);
    }


    public enum RESOLUTION {
        R540P, R720P, R1080P
    }
}
