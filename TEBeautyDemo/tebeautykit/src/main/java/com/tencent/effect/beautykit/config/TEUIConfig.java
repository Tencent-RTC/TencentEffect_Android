package com.tencent.effect.beautykit.config;


import android.text.TextUtils;

import androidx.annotation.ColorInt;

import com.google.android.material.tabs.TabLayout;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelViewResModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TEUIConfig {

    /**
     * 美颜面板整体背景色（作用于底部面板容器）
     */
    @ColorInt
    public int panelBackgroundColor = 0xAA000000;

    /**
     * 面板内部各分割线颜色（顶部标题分割线、item 之间的竖向分割线等）
     */
    @ColorInt
    public int panelDividerColor = 0x19FFFFFF;

    /**
     * 二级面板 item 选中态的文字颜色
     */
    @ColorInt
    public int panelItemCheckedColor = 0xFFFFFFFF;

    /**
     * 二级面板 item 默认（未选中）的文字颜色
     */
    @ColorInt
    public int panelItemColor = 0x99FFFFFF;

    /**
     * 顶部标题栏 Tab 的默认（未选中）文字颜色
     */
    @ColorInt
    public int textColor = 0x99FFFFFF;

    /**
     * 顶部标题栏 Tab 的选中态文字颜色
     */
    @ColorInt
    public int textCheckedColor = 0xFFFFFFFF;

    /**
     * SeekBar 的主色调，同时应用于：
     * 1) 进度条已选中部分（progress track）
     * 2) thumb 圆点
     * 3) 拖动时的气泡指示器（indicator）
     */
    @ColorInt
    public int seekBarProgressColor = 0xFFFFFFFF;


    /**
     * 顶部 Tab 栏的布局模式。
     * 可选值：{@link TabLayout#MODE_AUTO}（自动）、{@link TabLayout#MODE_FIXED}（固定等宽）、
     * {@link TabLayout#MODE_SCROLLABLE}（可滚动）。
     * 当 Tab 数量较多时建议使用 MODE_AUTO 或 MODE_SCROLLABLE。
     */
    public int tabMode = TabLayout.MODE_AUTO;


    /**
     * 面板"还原"按钮的行为策略：
     * true  —— 还原到 JSON 配置中定义的初始状态
     * false —— 还原到进入面板时通过 lastParam 记录的状态
     */
    public boolean revertEffect2Json = true;

    /**
     * 设置滤镜或单项美妆时，是否同时清理已应用的"轻美妆"效果。
     * true 表示会清理，false 表示保留。
     */
    public boolean cleanLightMakeup = false;

    /**
     * 解析 JSON 中 item 名称时使用的字段：
     * true  —— 使用 displayName（一般为本地化中文名）
     * false —— 使用 displayNameEn（英文名）
     * 默认根据系统语言自动切换（见 {@link #setSystemLocal(Locale)}）。
     */
    @Deprecated
    private boolean useDisplayName = true;

    // 是否启用从右向左（RTL）布局。默认 false，表示从左向右（LTR）展示。
    // 设置为 true 时，面板及对话框的布局方向与文字方向会切换为从右向左，
    // 用于适配阿拉伯语等 RTL 语言。
    public boolean isLayoutRtl = false;

    private Locale mLocale = Locale.getDefault();
    private EffectPlan effectPlan = null;


    private List<TEPanelDataModel> defaultPanelDataList = new ArrayList<>();

    public String panelViewIndexJsonFilePath = null;

    public String disPlayNameKey;
    public String defaultDisPlayNameKey;


    private static class ClassHolder {
        static final TEUIConfig TUI_CONFIG = new TEUIConfig();
    }

    public static TEUIConfig getInstance() {
        return TEUIConfig.ClassHolder.TUI_CONFIG;
    }


    public void setSystemLocal(Locale locale) {
        this.mLocale = locale;
        this.useDisplayName = this.isZh();
    }


    /**
     * Set the JSON file paths for the beauty panel.
     *
     * @param beauty       The JSON file path for beauty attributes, set to null if not available.
     * @param beautyBody   The JSON file path for beauty body attributes, set to null if not available.
     * @param lut          The JSON file path for filter attributes, set to null if not available.
     * @param motion       The JSON file path for motion sticker attributes, set to null if not available.
     * @param makeup       The JSON file path for makeup attributes, set to null if not available.
     * @param segmentation The JSON file path for segmentation attributes, set to null if not available.
     */
    public void setTEPanelViewRes(String beauty, String beautyBody, String lut, String motion, String makeup,
                                  String segmentation) {
        this.defaultPanelDataList.clear();
        this.addPanelViewRes(beauty, beautyBody, lut, motion, makeup, segmentation, null);
    }

    /**
     * 3.9.0新增接口，增加了设置轻美妆的能力
     */
    public void setTEPanelViewRes(TEPanelViewResModel resModel) {
        this.defaultPanelDataList.clear();
        this.addPanelViewRes(resModel.beauty, resModel.beautyBody, resModel.lut, resModel.motion, resModel.makeup,
                resModel.segmentation, resModel.lightMakeup);
    }

    private void addPanelViewRes(String beauty, String beautyBody, String lut, String motion, String makeup,
                                 String segmentation, String lightMakeup) {
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
            this.defaultPanelDataList.add(
                    new TEPanelDataModel(segmentation, TEUIProperty.UICategory.SEGMENTATION));
        }
    }

    public List<TEPanelDataModel> getPanelDataList() {
        return this.defaultPanelDataList;
    }


    public EffectPlan getEffectPlan() {
        return this.effectPlan;
    }


    /**
     * @param indexFilePath
     */
    public void setPanelViewResAndEffectPlanName(String indexFilePath, EffectPlan effectPlan) {
        this.defaultPanelDataList.clear();
        this.panelViewIndexJsonFilePath = indexFilePath;
        this.effectPlan = effectPlan;
    }


    public boolean isZh() {
        return "zh".equals(mLocale.getLanguage());
    }

    @Deprecated
    public boolean isUseDisplayName() {
        return this.useDisplayName;
    }

    @Deprecated
    public void setUseDisplayName(boolean useDisplayName) {
        this.useDisplayName = useDisplayName;
    }

    /**
     * 设置面板需要展示的文本的key
     * @param key
     */
    public void setDisPlayNameKey(String key) {
        this.disPlayNameKey = key;
    }


}
