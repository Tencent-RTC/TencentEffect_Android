package com.tencent.effect.beautykit.provider;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.manager.TEParamManager;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;
import com.tencent.xmagic.XmagicConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class TEGeneralDataProvider extends TEAbstractPanelDataProvider {

    private static final String TAG = TEGeneralDataProvider.class.getName();

    protected Map<String, TEUIProperty> titleTypeData = new ArrayMap<>();   //根据一级标题名对数据进行分类


    private List<TEUIProperty> pointMakeup = new ArrayList<>();
    private boolean hasLightMakeup = false;

    private boolean pointMakeupChecked = false;
    private boolean lightMakeupChecked = false;


    private boolean hasBeautyTemplateChecked = true;

    @Override
    public List<TEUIProperty> forceRefreshPanelData(Context context) {
        super.forceRefreshPanelData(context);
        titleTypeData.clear();
        for (TEUIProperty teuiProperty : allData) {
            if (teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY) {
                this.obtainPointMakeup(teuiProperty);
                //判断是否有默认设置
                pointMakeupChecked = !ProviderUtils.getUsedProperties(pointMakeup).isEmpty() || pointMakeupChecked;
                // 轻美妆既可能作为独立一级标题存在（见下方 LIGHT_MAKEUP 分支），
                // 也可能被配置在"单点美妆"标题下作为二级标题；而单点美妆在解析后
                // uiCategory 会被归并成 BEAUTY（见 EffectPlan），导致顶层遍历时只能看到 BEAUTY，
                // 因此需继续深入遍历其二级标题（propertyList）才能发现嵌套的轻美妆。
                if (teuiProperty.propertyList != null && !teuiProperty.propertyList.isEmpty()) {
                    for (TEUIProperty property : teuiProperty.propertyList) {
                        if (property == null || property.uiCategory != TEUIProperty.UICategory.LIGHT_MAKEUP) {
                            continue;
                        }
                        hasLightMakeup = true;
                        //判断是否有默认设置
                        lightMakeupChecked =
                                !ProviderUtils.getUsedProperties(Collections.singletonList(property)).isEmpty();
                    }
                }
            }
            if (teuiProperty.uiCategory == TEUIProperty.UICategory.LUT) {
                pointMakeupChecked = !ProviderUtils.getUsedProperties(pointMakeup).isEmpty() || pointMakeupChecked;
            }
            if (teuiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) {
                hasLightMakeup = true;
                //判断是否有默认设置
                lightMakeupChecked = !ProviderUtils.getUsedProperties(Collections.singletonList(teuiProperty)).isEmpty();
            }
            titleTypeData.put(teuiProperty.titleType, teuiProperty);
        }
        LogUtils.i(TAG, "default clickPointMakeup = " + pointMakeupChecked + " default clickLightMakeup = " + lightMakeupChecked);
        return allData;
    }


    /**
     * 用于从美颜列表中解析出 单点妆容的 数据
     *
     * @param teuiProperty
     */
    private void obtainPointMakeup(TEUIProperty teuiProperty) {
        if (teuiProperty == null) {
            return;
        }
        if (teuiProperty.propertyList != null) {
            for (TEUIProperty uiProperty : teuiProperty.propertyList) {
                this.obtainPointMakeup(uiProperty);
            }
        } else if (ProviderUtils.isPointMakeup(teuiProperty.sdkParam)) {
            this.pointMakeup.add(teuiProperty);
        }
    }

    @Override
    public List<TEUIProperty> onHandRecycleViewItemClick(TEUIProperty uiProperty) {
        return this.onHandRecycleViewItemClick(uiProperty, true);
    }

    private List<TEUIProperty> onHandRecycleViewItemClick(TEUIProperty uiProperty, boolean isFromUI) {
        if (uiProperty == null) {
            return null;
        }
        switch (uiProperty.uiCategory) {
            case BEAUTY_TEMPLATE:
                this.hasBeautyTemplateChecked = true;
                this.uncheckBeautyAndLut();
                this.uncheckLightMakeup();
                this.checkItem(uiProperty, isFromUI);
                break;
            case BEAUTY:
            case LUT:
                this.uncheckBeautyTemplate();
                this.onClickPointMakeup(uiProperty, isFromUI);
                break;
            case BODY_BEAUTY:
                this.onClickPointMakeup(uiProperty, isFromUI);
                break;
            case LIGHT_MAKEUP:  //此处需要将 单点美妆和滤镜全部设置为 未选中
                this.onClickLightMakeup(uiProperty, isFromUI);
                break;
            case LIGHT_MAKEUP_ITEM:
                // 下钻态下点击某个单项（口红/腮红/眼影/滤镜等）：仅切换子列表内的选中状态，
                // 兄弟项统一回到 INIT；不下发 effect（真正下发由滑竿拖动时触发）。
                if (uiProperty.parentUIProperty != null && uiProperty.parentUIProperty.propertyList != null) {
                    ProviderUtils.revertUIStateToInit(uiProperty.parentUIProperty.propertyList);
                }
                ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
                break;
            case LIGHT_MAKEUP_BACK:
                // 返回项由 UI 层拦截处理，不走 provider 的选中逻辑。
                break;
            case MAKEUP:
            case MOTION:
            case SEGMENTATION:
                this.handleMakeupMotionSegmentation(uiProperty, isFromUI);
                break;
            case GREEN_BACKGROUND_V2_ITEM:
            case GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE:
                this.handleMakeupMotionSegmentation(uiProperty, isFromUI);
                break;

        }
        return uiProperty.propertyList;
    }

    private void handleMakeupMotionSegmentation(TEUIProperty uiProperty, boolean isFromUI) {
        List<TEUIProperty> makeUpProperty = getDataByUICategory(TEUIProperty.UICategory.MAKEUP);
        List<TEUIProperty> motionProperty = getDataByUICategory(TEUIProperty.UICategory.MOTION);
        List<TEUIProperty> segProperty = getDataByUICategory(TEUIProperty.UICategory.SEGMENTATION);

        boolean shouldProcess = !isFromUI ||
                ((uiProperty.propertyList == null && uiProperty.sdkParam != null) ||
                        uiProperty.isNoneItem());

        if (shouldProcess) {
            ProviderUtils.revertUIState(makeUpProperty, uiProperty);
            ProviderUtils.revertUIState(motionProperty, uiProperty);
            ProviderUtils.revertUIState(segProperty, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
        }
    }




    private void checkItem(TEUIProperty uiProperty, boolean isFromUI) {
        TEUIProperty currentProperty = titleTypeData.get(uiProperty.titleType);
        if (!isFromUI) {
            if (currentProperty == null) {
                return;
            }
            ProviderUtils.revertUIState(currentProperty.propertyList, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
            return;
        }
        if ((uiProperty.propertyList == null && uiProperty.sdkParam != null) || uiProperty.isNoneItem()) {
            if (currentProperty == null) {
                return;
            }
            ProviderUtils.revertUIState(currentProperty.propertyList, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
        } else if (uiProperty.paramList != null || uiProperty.isNoneItem()) {
            List<TEUIProperty> processData = new ArrayList<>();
            for (TEUIProperty property : allData) {
                if (property.uiCategory == uiProperty.uiCategory) {   //找到所有的模板item
                    processData.add(property);
                }
            }
            ProviderUtils.revertUIState(processData, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
        }
    }


    @Override
    public List<TEUIProperty.TESDKParam> getRevertData(Context context) {
        List<TEUIProperty.TESDKParam> usedList = ProviderUtils.getUsedProperties(allData);
        boolean hasLut = false;
        boolean hasMotion = false;
        for (TEUIProperty.TESDKParam param : usedList) {
            if (param.effectName.equals(XmagicConstant.EffectName.EFFECT_LUT)) {
                hasLut = true;
            }
            if (param.effectName.equals(XmagicConstant.EffectName.EFFECT_MAKEUP)
                    || param.effectName.equals(XmagicConstant.EffectName.EFFECT_MOTION)
                    || param.effectName.equals(XmagicConstant.EffectName.EFFECT_SEGMENTATION)) {
                hasMotion = true;
            }
        }

        if (TEUIConfig.getInstance().revertEffect2Json) {
            this.originalParamList = null;
        }
        this.forceRefreshPanelData(context.getApplicationContext());

        List<TEUIProperty.TESDKParam> defaultUsedList = ProviderUtils.getUsedProperties(allData);

        for (TEUIProperty.TESDKParam param : defaultUsedList) {
            if (param.effectName.equals(XmagicConstant.EffectName.EFFECT_LUT)) {
                hasLut = false;
            }
            if (param.effectName.equals(XmagicConstant.EffectName.EFFECT_MAKEUP)
                    || param.effectName.equals(XmagicConstant.EffectName.EFFECT_MOTION)
                    || param.effectName.equals(XmagicConstant.EffectName.EFFECT_SEGMENTATION)) {
                hasMotion = false;
            }
        }

        TEParamManager paramManager = new TEParamManager();
        paramManager.putTEParams(ProviderUtils.clone0ValuedParam(usedList));
        paramManager.putTEParams(defaultUsedList);
        if (hasLut) {
            paramManager.putTEParam(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_LUT));
        }
        if (hasMotion) {
            paramManager.putTEParam(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_MOTION));
        }
        return paramManager.getParams();
    }

    @Override
    public List<TEUIProperty.TESDKParam> getCloseEffectItems(TEUIProperty uiProperty) {

        switch (uiProperty.uiCategory) {
            case BEAUTY:
            case BODY_BEAUTY:
                TEUIProperty currentProperty = titleTypeData.get(uiProperty.titleType);
                if (currentProperty != null) {
                    List<TEUIProperty.TESDKParam> usedList = ProviderUtils.getUsedProperties(currentProperty.propertyList);
                    ProviderUtils.changParamValuedTo0(usedList);
                    return usedList;
                }
            case LUT:
                return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_LUT));
            case MAKEUP:
            case MOTION:
            case SEGMENTATION:
                return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_MOTION));
            case LIGHT_MAKEUP:
                return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_LIGHT_MAKEUP));
            case GREEN_BACKGROUND_V2_ITEM:
                //绿幕2 的关闭项按钮
                TEUIProperty gsv2uiProperty = uiProperty.parentUIProperty;
                gsv2uiProperty.sdkParam.extraInfo.remove(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH);
                return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_MOTION));
        }
        return null;
    }


    private void onClickPointMakeup(TEUIProperty property, boolean isFromUI) {
        if (property == null) {
            return;
        }
        this.checkItem(property, isFromUI);

        if (hasLightMakeup && ProviderUtils.isPointMakeup(property.sdkParam)) {  //只有在有轻美妆的情况下才会继续判断点击的是否是 单点妆容
            this.pointMakeupChecked = true;
            if (!this.lightMakeupChecked) {
                return;
            }
            if (!TEUIConfig.getInstance().cleanLightMakeup) {
                return;
            }
            LogUtils.i(TAG, "revertUIState on lightMakeup item");
            this.uncheckLightMakeup();
        }
    }

    private void onClickLightMakeup(TEUIProperty property, boolean isFromUI) {
        if (property == null) {
            return;
        }
        this.uncheckBeautyTemplate();
        this.lightMakeupChecked = true;
        this.checkItem(property, isFromUI);
        if (!this.pointMakeupChecked) {
            return;
        }
        LogUtils.i(TAG, "revertUIState on pointMakeup item");
        this.uncheckPointMakeup();
    }


    private void uncheckPointMakeup() {
        this.pointMakeupChecked = false;
        List<TEUIProperty> lutData = this.getDataByUICategory(TEUIProperty.UICategory.LUT);
        for (TEUIProperty teuiProperty : lutData) {
            unCheckItem(teuiProperty);
        }
        if (this.pointMakeup != null) {
            ProviderUtils.revertUIStateToInit(this.pointMakeup);
        }
    }


    @Override
    protected void uncheckLightMakeup() {
        this.lightMakeupChecked = false;
        List<TEUIProperty> lightMakeup = new ArrayList<>();
        for (TEUIProperty teuiProperty : allData) {
            this.collectByUICategory(teuiProperty, TEUIProperty.UICategory.LIGHT_MAKEUP, lightMakeup);
        }
        ProviderUtils.revertUIStateToInit(lightMakeup);
    }


    private void uncheckBeautyTemplate() {
        if (!this.hasBeautyTemplateChecked) {
            return;
        }
        this.hasBeautyTemplateChecked = false;
        List<TEUIProperty> beautyTemplate = this.getDataByUICategory(TEUIProperty.UICategory.BEAUTY_TEMPLATE);
        for (TEUIProperty teuiProperty : beautyTemplate) {
            unCheckItem(teuiProperty);
        }
    }


    protected void uncheckBeautyAndLut() {
        List<TEUIProperty> beautyData = this.getDataByUICategory(TEUIProperty.UICategory.BEAUTY);
        for (TEUIProperty teuiProperty : beautyData) {
            unCheckItem(teuiProperty);
        }
        List<TEUIProperty> lutData = this.getDataByUICategory(TEUIProperty.UICategory.LUT);
        for (TEUIProperty teuiProperty : lutData) {
            unCheckItem(teuiProperty);
        }
    }


    private void unCheckItem(TEUIProperty teuiProperty) {
        if (teuiProperty == null) {
            return;
        }
        ProviderUtils.revertUIStateToInit(Collections.singletonList(teuiProperty));
    }


    private List<TEUIProperty> getDataByUICategory(TEUIProperty.UICategory uiCategory) {
        List<TEUIProperty> result = new ArrayList<>();
        for (TEUIProperty teuiProperty : allData) {
            if (teuiProperty.uiCategory == uiCategory) {
                result.add(teuiProperty);
            }
        }
        return result;
    }


    /**
     * 递归收集指定 uiCategory 的所有项（含一级标题与任意层级子项）。
     * 轻美妆既可能是一级标题，也可能配置在单点美妆（BEAUTY）下的二级标题，
     * 因此清理/查找轻美妆时不能只遍历一级标题，需要递归深入。
     *
     * @param property   当前节点，可为 null
     * @param uiCategory 目标分类
     * @param result     收集结果
     */
    private void collectByUICategory(TEUIProperty property, TEUIProperty.UICategory uiCategory,
                                     List<TEUIProperty> result) {
        if (property == null) {
            return;
        }
        if (property.uiCategory == uiCategory) {
            result.add(property);
        }
        if (property.propertyList != null) {
            for (TEUIProperty child : property.propertyList) {
                this.collectByUICategory(child, uiCategory, result);
            }
        }
    }


}
