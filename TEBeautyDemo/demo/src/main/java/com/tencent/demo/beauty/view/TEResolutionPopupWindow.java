package com.tencent.demo.beauty.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.Switch;

import com.tencent.demo.beauty.view.TETitleBar.RESOLUTION;
import com.tencent.demo.R;

public class TEResolutionPopupWindow {

    private TETitleBar.RESOLUTION resolution = TETitleBar.RESOLUTION.R720P;
    private TETitleBar.TETitleBarClickListener teTitleBarClickListener = null;
    private boolean isShowEnhancedModeSwitch = false;

    private boolean isShowSmartBeauty = false;
    private boolean isShowWhiteSkinOnly = false;
    private boolean isEnhancedModeIsTurnOn = false;

    private boolean isSmartBeauty = false;
    private boolean isWhiteSkinOnly = false;

    private boolean isCropTexture = false;

    private Switch smartBeautySwitch;
    private Switch whiteSkinOnlySwitch;

    private Switch cropTextureSwitch;

    private PopupWindow popupWindow;


    @SuppressLint("NonConstantResourceId")
    private View createPopLayout(Context context) {
        View contentView = LayoutInflater.from(context).inflate(R.layout.te_beauty_titlebar_resolution_layout, null);
        RadioGroup resolutionRadioGroup = contentView.findViewById(R.id.te_titlebar_layout_resolution_group);

        if (isShowSmartBeauty) {
            contentView.findViewById(R.id.te_titlebar_layout_smart_beauty_switch_layout).setVisibility(View.VISIBLE);
            smartBeautySwitch = contentView.findViewById(R.id.te_titlebar_layout_smart_beauty_switch);
            smartBeautySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isSmartBeauty = isChecked;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onSmartBeautyTurnOn(isSmartBeauty);
                }
            });
            smartBeautySwitch.setChecked(isSmartBeauty);
        }


        if (isShowWhiteSkinOnly) {
            contentView.findViewById(R.id.te_titlebar_layout_only_white_skin_switch_layout).setVisibility(View.VISIBLE);
            whiteSkinOnlySwitch = contentView.findViewById(R.id.te_titlebar_layout_only_white_skin_switch);
            whiteSkinOnlySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isWhiteSkinOnly = isChecked;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onOnlyBeautyWhiteOnSkin(isWhiteSkinOnly);
                }
            });
            whiteSkinOnlySwitch.setChecked(isWhiteSkinOnly);
        }

        cropTextureSwitch = contentView.findViewById(R.id.te_titlebar_layout_crop_texture_switch);
        cropTextureSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isCropTexture = isChecked;
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onCropTextureSwitchTurnOn(isCropTexture);
            }
        });
        cropTextureSwitch.setChecked(isCropTexture);

        if (isShowEnhancedModeSwitch) {
            contentView.findViewById(R.id.te_titlebar_layout_enhanced_mode_switch_layout).setVisibility(View.VISIBLE);
            Switch enhancedModeSwitch = contentView.findViewById(R.id.te_titlebar_layout_enhanced_mode_switch);
            enhancedModeSwitch.setChecked(isEnhancedModeIsTurnOn);
            enhancedModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isEnhancedModeIsTurnOn = isChecked;
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onEnhancedModeSwitchTurnOn(isEnhancedModeIsTurnOn);
                }
            });
        }
        if (resolution == RESOLUTION.R540P) {
            resolutionRadioGroup.check(R.id.te_titlebar_layout_540p_btn);
        } else if (resolution == RESOLUTION.R720P) {
            resolutionRadioGroup.check(R.id.te_titlebar_layout_720p_btn);
        } else if (resolution == RESOLUTION.R1080P) {
            resolutionRadioGroup.check(R.id.te_titlebar_layout_1080p_btn);
        }
        resolutionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.te_titlebar_layout_540p_btn:
                    resolution = RESOLUTION.R540P;
                    break;
                case R.id.te_titlebar_layout_1080p_btn:
                    resolution = RESOLUTION.R1080P;
                    break;
                default:
                    resolution = RESOLUTION.R720P;
                    break;
            }
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onSwitchResolution(resolution);
            }
        });
        return contentView;
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


    public void setDefaultParameter(TETitleBar.RESOLUTION resolution,
                                    boolean isEnhancedModeIsTurnOn, boolean isWhiteSkin, boolean isCropTexture,
                                    boolean isSmartBeauty) {
        this.resolution = resolution;
        this.isEnhancedModeIsTurnOn = isEnhancedModeIsTurnOn;
        this.isWhiteSkinOnly = isWhiteSkin;
        this.isCropTexture = isCropTexture;
        this.isSmartBeauty = isSmartBeauty;
    }

    public void showResolutionPop(View targetView) {
        View contentView = createPopLayout(targetView.getContext());
        popupWindow = new PopupWindow(contentView, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAsDropDown(targetView);
        popupWindow.setOnDismissListener(() -> {
            popupWindow = null;
        });
    }

    public void setTeTitleBarClickListener(TETitleBar.TETitleBarClickListener teTitleBarClickListener) {
        this.teTitleBarClickListener = teTitleBarClickListener;
    }
}
