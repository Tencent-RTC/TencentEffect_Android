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

import com.tencent.demo.R;
import com.tencent.demo.beauty.model.TEFeatureConfig;
import com.tencent.demo.beauty.view.TETitleBar.RESOLUTION;

/**
 *
 */

public class TEResolutionPopupWindow {

    private TETitleBar.TETitleBarClickListener teTitleBarClickListener = null;
    private PopupWindow popupWindow;

    public TEResolutionPopupWindow(TETitleBar.TETitleBarClickListener teTitleBarClickListener) {
        this.teTitleBarClickListener = teTitleBarClickListener;
    }

    @SuppressLint("NonConstantResourceId")
    private View createPopLayout(Context context) {
        View contentView = LayoutInflater.from(context).inflate(R.layout.te_beauty_titlebar_resolution_layout, null);

        contentView.findViewById(R.id.te_titlebar_layout_smart_beauty_switch_layout).setVisibility(View.VISIBLE);
        Switch smartBeautySwitch = contentView.findViewById(R.id.te_titlebar_layout_smart_beauty_switch);
        smartBeautySwitch.setChecked(TEFeatureConfig.getInstance().isOnSmartBeautySwitch);
        smartBeautySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onSmartBeautyTurnOn(isChecked);
            }
        });


        if (TEFeatureConfig.getInstance().isShowWhiteSkinOnlySwitch) {
            contentView.findViewById(R.id.te_titlebar_layout_only_white_skin_switch_layout).setVisibility(View.VISIBLE);
            Switch whiteSkinOnlySwitch = contentView.findViewById(R.id.te_titlebar_layout_only_white_skin_switch);
            whiteSkinOnlySwitch.setChecked(TEFeatureConfig.getInstance().isOnWhiteSkinOnlySwitch);
            whiteSkinOnlySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onOnlyBeautyWhiteOnSkin(isChecked);
                }
            });

        } else {
            contentView.findViewById(R.id.te_titlebar_layout_only_white_skin_switch_layout).setVisibility(View.GONE);
        }

        Switch cropTextureSwitch = contentView.findViewById(R.id.te_titlebar_layout_crop_texture_switch);
        cropTextureSwitch.setChecked(TEFeatureConfig.getInstance().isOnCropTextureSwitch);
        cropTextureSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onCropTextureSwitchTurnOn(isChecked);
            }
        });

        Switch performanceSwitch = contentView.findViewById(R.id.te_titlebar_layout_performance_switch);
        performanceSwitch.setChecked(TEFeatureConfig.getInstance().isOnPerformanceSwitch);
        performanceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onPerformanceSwitchTurnOn(isChecked);
            }
        });

        if (TEFeatureConfig.getInstance().isShowEnhancedModeSwitch) {
            contentView.findViewById(R.id.te_titlebar_layout_enhanced_mode_switch_layout).setVisibility(View.VISIBLE);
            Switch enhancedModeSwitch = contentView.findViewById(R.id.te_titlebar_layout_enhanced_mode_switch);
            enhancedModeSwitch.setChecked(TEFeatureConfig.getInstance().isOnEnhancedMode);
            enhancedModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (teTitleBarClickListener != null) {
                    teTitleBarClickListener.onEnhancedModeSwitchTurnOn(isChecked);
                }
            });
        } else {
            contentView.findViewById(R.id.te_titlebar_layout_enhanced_mode_switch_layout).setVisibility(View.GONE);
        }

        Switch faceBlockSwitch = contentView.findViewById(R.id.te_titlebar_layout_face_block_switch);
        faceBlockSwitch.setChecked(TEFeatureConfig.getInstance().isOnFaceBlockSwitch);
        faceBlockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onFaceBlockSwitchTurnOn(isChecked);
            }
        });

        RadioGroup resolutionRadioGroup = contentView.findViewById(R.id.te_titlebar_layout_resolution_group);
        if (TEFeatureConfig.getInstance().resolution == RESOLUTION.R540P) {
            resolutionRadioGroup.check(R.id.te_titlebar_layout_540p_btn);
        } else if (TEFeatureConfig.getInstance().resolution == RESOLUTION.R720P) {
            resolutionRadioGroup.check(R.id.te_titlebar_layout_720p_btn);
        } else if (TEFeatureConfig.getInstance().resolution == RESOLUTION.R1080P) {
            resolutionRadioGroup.check(R.id.te_titlebar_layout_1080p_btn);
        }
        resolutionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.te_titlebar_layout_540p_btn:
                    TEFeatureConfig.getInstance().resolution = RESOLUTION.R540P;
                    break;
                case R.id.te_titlebar_layout_1080p_btn:
                    TEFeatureConfig.getInstance().resolution = RESOLUTION.R1080P;
                    break;
                default:
                    TEFeatureConfig.getInstance().resolution = RESOLUTION.R720P;
                    break;
            }
            if (teTitleBarClickListener != null) {
                teTitleBarClickListener.onSwitchResolution(TEFeatureConfig.getInstance().resolution);
            }
        });
        return contentView;
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





}
