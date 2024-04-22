package com.tencent.effect.beautykit.config;


import android.text.TextUtils;

import androidx.annotation.ColorInt;

import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TEUIConfig {

    @ColorInt
    public int panelBackgroundColor = 0x66000000;  //默认背景色
    @ColorInt
    public int panelDividerColor = 0x19FFFFFF;  //分割线颜色
    @ColorInt
    public int panelItemCheckedColor = 0xFF006EFF;  //选中项颜色
    @ColorInt
    public int textColor = 0x99FFFFFF; //文本颜色
    @ColorInt
    public int textCheckedColor = 0xFFFFFFFF; //文本选中颜色
    @ColorInt
    public int seekBarProgressColor = 0xFF006EFF; //进度条颜色


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
     * 设置美颜面板的JSON文件路径
     * @param beauty 美颜属性的JSON文件路径，如果没有则设置为null
     * @param beautyBody 美体属性JSON文件路径，如果没有则设置为null
     * @param lut 滤镜属性JSON文件路径，如果没有则设置为null
     * @param motion 动效贴纸属性JSON文件路径，如果没有则设置为null
     * @param makeup 美妆属性JSON文件路径，如果没有则设置为null
     * @param segmentation 分割属性JSON文件路径，如果没有则设置为null
     */
    public void setTEPanelViewRes(String beauty, String beautyBody, String lut, String motion, String makeup, String segmentation) {
        this.defaultPanelDataList.clear();
        if (!TextUtils.isEmpty(beauty)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(beauty, TEUIProperty.UICategory.BEAUTY));
        }
        if (!TextUtils.isEmpty(beautyBody)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(beautyBody, TEUIProperty.UICategory.BODY_BEAUTY));
        }
        if (!TextUtils.isEmpty(lut)) {
            this.defaultPanelDataList.add(new TEPanelDataModel(lut, TEUIProperty.UICategory.LUT));
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

    /**
     * 判断是否是默认语言
     * @return
     */
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
