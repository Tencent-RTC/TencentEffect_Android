package com.tencent.effect.beautykit.config;


import android.text.TextUtils;

import androidx.annotation.ColorInt;

import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelViewResModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TEUIConfig {

    @ColorInt
    public int panelBackgroundColor = 0x66000000;  //Default background color
    @ColorInt
    public int panelDividerColor = 0x19FFFFFF;  //Divider color
    @ColorInt
    public int panelItemCheckedColor = 0xFF006EFF;  //Selected item color
    @ColorInt
    public int textColor = 0x99FFFFFF; //Text color
    @ColorInt
    public int textCheckedColor = 0xFFFFFFFF; //Text selected color
    @ColorInt
    public int seekBarProgressColor = 0xFF006EFF; //Progress bar color


    public boolean revertEffect2Json = false;   //如果设置为true,那么在点击面板还原按钮的时候就会还原到json配置的状态，否则还原到进入面板设置了 lastParam的状态

    public boolean cleanLightMakeup = false;   //此配置是当设置滤镜，或者单点美妆的时候是否清理轻美妆



    private Locale mLocale = Locale.getDefault();


    private List<TEPanelDataModel> defaultPanelDataList = new ArrayList<>();


    private static class ClassHolder {
        static final TEUIConfig TUI_CONFIG = new TEUIConfig();
    }

    public static TEUIConfig getInstance() {
        return TEUIConfig.ClassHolder.TUI_CONFIG;
    }


    public void setSystemLocal(Locale locale) {
        this.mLocale = locale;
    }


    /**
     Set the JSON file paths for the beauty panel.
     @param beauty The JSON file path for beauty attributes, set to null if not available.
     @param beautyBody The JSON file path for beauty body attributes, set to null if not available.
     @param lut The JSON file path for filter attributes, set to null if not available.
     @param motion The JSON file path for motion sticker attributes, set to null if not available.
     @param makeup The JSON file path for makeup attributes, set to null if not available.
     @param segmentation The JSON file path for segmentation attributes, set to null if not available.
     */
    public void setTEPanelViewRes(String beauty, String beautyBody, String lut, String motion, String makeup, String segmentation) {
        this.defaultPanelDataList.clear();
        this.addPanelViewRes(beauty, beautyBody, lut, motion, makeup, segmentation,null);
    }

    /**
     * 3.9.0新增接口，增加了设置轻美妆的能力
     */
    public void setTEPanelViewRes(TEPanelViewResModel resModel) {
        this.defaultPanelDataList.clear();
        this.addPanelViewRes(resModel.beauty, resModel.beautyBody, resModel.lut, resModel.motion, resModel.makeup, resModel.segmentation, resModel.lightMakeup);
    }

    private void addPanelViewRes(String beauty, String beautyBody, String lut, String motion, String makeup, String segmentation, String lightMakeup) {
        if (!TextUtils.isEmpty(beauty)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(beauty, TEUIProperty.UICategory.BEAUTY));
        }
        if (!TextUtils.isEmpty(beautyBody)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(beautyBody, TEUIProperty.UICategory.BODY_BEAUTY));
        }
        if (!TextUtils.isEmpty(lut)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(lut, TEUIProperty.UICategory.LUT));
        }
        if (!TextUtils.isEmpty(lightMakeup)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(lightMakeup, TEUIProperty.UICategory.LIGHT_MAKEUP));
        }
        if (!TextUtils.isEmpty(motion)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(motion, TEUIProperty.UICategory.MOTION));
        }
        if (!TextUtils.isEmpty(makeup)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(makeup, TEUIProperty.UICategory.MAKEUP));
        }
        if (!TextUtils.isEmpty(segmentation)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(segmentation, TEUIProperty.UICategory.SEGMENTATION));
        }
    }

    public List<TEPanelDataModel> getPanelDataList() {
        return this.defaultPanelDataList;
    }


    public boolean isDefaultLanguage() {
        return this.isZh();
    }

    public boolean isZh() {
        return "zh".equals(mLocale.getLanguage());
    }

    public boolean isJA() {
        return "ja".equals(mLocale.getLanguage());
    }


}
