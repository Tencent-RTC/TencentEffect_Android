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

public class TETitleBar extends LinearLayout implements View.OnClickListener {

    private ImageView backBtn;
    private ImageView resolutionBtn;
    private ImageView cameraSwitchBtn;

    private TextView titleTxt;
    private RESOLUTION resolution = RESOLUTION.R1080P;
    private boolean isEnhancedModeIsTurnOn = false;

    private boolean isSmartBeauty = false;
    private boolean isWhiteSkinOnly = false;

    private boolean isCropTexture = false;

    private boolean isShowEnhancedModeSwitch = true;

    private boolean isShowSmartBeauty = true;
    private boolean isShowWhiteSkinOnly = true;

    private TEResolutionPopupWindow resolutionPopupWindow;
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
        cameraSwitchBtn = findViewById(R.id.te_title_layout_camera_switch_btn);
        cameraSwitchBtn.setOnClickListener(this);
    }

    public ImageView getResolutionBtn() {
        return resolutionBtn;
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
        }else if (v.getId() == R.id.te_title_layout_camera_switch_btn) {
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onCameraSwitch();
            }
        }
    }

    public void setTeTitleBarClickListener(TETitleBarClickListener titleBarClickListener) {
        teTitleBarClickListener = titleBarClickListener;
    }

    public void setShowEnhancedModeSwitch(boolean isShowEnhancedModeSwitch) {
        this.isShowEnhancedModeSwitch = isShowEnhancedModeSwitch;
    }

    public void setShowSmartBeautySwitch(boolean isShow) {
        this.isShowSmartBeauty = isShow;
    }

    public void setShowOnlyWhiteOnSkySwitch(boolean isShow) {
        this.isShowWhiteSkinOnly = isShow;
    }


    public void setResolution(RESOLUTION resolution) {
        this.resolution = resolution;
    }



    private void showResolutionPop(View targetView) {
        resolutionPopupWindow = new TEResolutionPopupWindow();
        resolutionPopupWindow.setShowEnhancedModeSwitch(isShowEnhancedModeSwitch);
        resolutionPopupWindow.setShowOnlyWhiteOnSkySwitch(isShowWhiteSkinOnly);
        resolutionPopupWindow.setShowSmartBeautySwitch(isShowSmartBeauty);
        resolutionPopupWindow.setDefaultParameter(resolution, isEnhancedModeIsTurnOn, isWhiteSkinOnly,isCropTexture,isSmartBeauty);
        resolutionPopupWindow.setTeTitleBarClickListener(new TETitleBarClickListener() {
            @Override
            public void onCameraSwitch() {

            }

            @Override
            public void onSwitchResolution(RESOLUTION onSwitchResolution) {
                resolution = onSwitchResolution;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onSwitchResolution(onSwitchResolution);
                }
            }

            @Override
            public void onEnhancedModeSwitchTurnOn(boolean isEnable) {
                isEnhancedModeIsTurnOn = isEnable;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onEnhancedModeSwitchTurnOn(isEnhancedModeIsTurnOn);
                }
            }

            @Override
            public void onSmartBeautyTurnOn(boolean isTurnOn) {
                isSmartBeauty = isTurnOn;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onSmartBeautyTurnOn(isSmartBeauty);
                }
            }

            @Override
            public void onOnlyBeautyWhiteOnSkin(boolean isTurnOn) {
                isWhiteSkinOnly = isTurnOn;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onOnlyBeautyWhiteOnSkin(isWhiteSkinOnly);
                }
            }

            @Override
            public void onCropTextureSwitchTurnOn(boolean isCrop) {
                isCropTexture = isCrop;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onCropTextureSwitchTurnOn(isCropTexture);
                }
            }
        });
        resolutionPopupWindow.showResolutionPop(targetView);
    }


    public interface TETitleBarClickListener {
        void onCameraSwitch();

        void onSwitchResolution(RESOLUTION resolution);

        void onEnhancedModeSwitchTurnOn(boolean isEnable);

        void onSmartBeautyTurnOn(boolean isTurnOn);
        void onOnlyBeautyWhiteOnSkin(boolean isTurnOn);

        void onCropTextureSwitchTurnOn(boolean isCrop);
    }


    public enum RESOLUTION {
        R540P, R720P, R1080P
    }
}
