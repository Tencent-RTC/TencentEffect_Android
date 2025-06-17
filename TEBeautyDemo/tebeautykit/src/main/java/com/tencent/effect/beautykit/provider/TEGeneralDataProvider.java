package com.tencent.effect.beautykit.provider;

import android.content.Context;
import android.util.ArrayMap;

import com.tencent.effect.beautykit.manager.TEParamManager;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;
import com.tencent.xmagic.XmagicConstant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;


public class TEGeneralDataProvider extends TEAbstractPanelDataProvider {

    private static final String TAG = TEGeneralDataProvider.class.getName();

    protected Map<TEUIProperty.UICategory, TEUIProperty> dataCategory = new ArrayMap<>();


    private List<TEUIProperty> pointMakeup = new ArrayList<>();
    private boolean hasLightMakeup = false;

    private boolean pointMakeupChecked = false;
    private boolean lightMakeupChecked = false;

    @Override
    public List<TEUIProperty> forceRefreshPanelData(Context context) {
        super.forceRefreshPanelData(context);
        for (TEUIProperty teuiProperty : allData) {
            if (teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY) {
                this.obtainPointMakeup(teuiProperty);
                //判断是否有默认设置
                pointMakeupChecked = ProviderUtils.getUsedProperties(pointMakeup).size() > 0 || pointMakeupChecked;
            }
            if (teuiProperty.uiCategory == TEUIProperty.UICategory.LUT) {
                pointMakeupChecked = ProviderUtils.getUsedProperties(pointMakeup).size() > 0 || pointMakeupChecked;
            }
            if (teuiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) {
                hasLightMakeup = true;
                //判断是否有默认设置
                lightMakeupChecked = ProviderUtils.getUsedProperties(Collections.singletonList(teuiProperty)).size() > 0;
            }
            dataCategory.put(teuiProperty.uiCategory, teuiProperty);
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
    public List<TEUIProperty> onItemClick(TEUIProperty uiProperty) {
       return this.onItemClickInternal(uiProperty,true);
    }

    private List<TEUIProperty> onItemClickInternal(TEUIProperty uiProperty, boolean isFromUI) {
        switch (uiProperty.uiCategory) {
            case BEAUTY:
            case BODY_BEAUTY:
            case LUT:
                this.onClickPointMakeup(uiProperty,isFromUI);
                break;
            case LIGHT_MAKEUP:  //此处需要将 单点美妆和滤镜全部设置为 未选中
                this.onClickLightMakeup(uiProperty,isFromUI);
                break;
            case MAKEUP:
            case MOTION:
            case SEGMENTATION:
                 this.handleMakeupMotionSegmentation(uiProperty,isFromUI);
                break;
        }
        return uiProperty.propertyList;
    }

    private void handleMakeupMotionSegmentation(TEUIProperty uiProperty, boolean isFromUI) {
        TEUIProperty makeUpProperty = dataCategory.get(TEUIProperty.UICategory.MAKEUP);
        TEUIProperty motionProperty = dataCategory.get(TEUIProperty.UICategory.MOTION);
        TEUIProperty segProperty = dataCategory.get(TEUIProperty.UICategory.SEGMENTATION);

        boolean shouldProcess = !isFromUI ||
                ((uiProperty.propertyList == null && uiProperty.sdkParam != null) ||
                        uiProperty.isNoneItem());

        if (shouldProcess) {
            if (makeUpProperty != null) {
                ProviderUtils.revertUIState(makeUpProperty.propertyList, uiProperty);
            }
            if (motionProperty != null) {
                ProviderUtils.revertUIState(motionProperty.propertyList, uiProperty);
            }
            if (segProperty != null) {
                ProviderUtils.revertUIState(segProperty.propertyList, uiProperty);
            }
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
        }
    }

    @Override
    public void selectPropertyItem(TEUIProperty uiProperty) {
        this.onItemClickInternal(uiProperty,false);
    }


    private void checkItem(TEUIProperty uiProperty , boolean isFromUI) {
        TEUIProperty currentProperty = dataCategory.get(uiProperty.uiCategory);
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
                TEUIProperty currentProperty = dataCategory.get(uiProperty.uiCategory);
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
            LogUtils.i(TAG, "revertUIState on lightMakeup item");
            this.lightMakeupChecked = false;
            this.uncheckLightMakeup();
        }
    }

    private void onClickLightMakeup(TEUIProperty property, boolean isFromUI) {
        if (property == null) {
            return;
        }
        this.lightMakeupChecked = true;
        this.checkItem(property, isFromUI);
        if (!this.pointMakeupChecked) {
            return;
        }
        LogUtils.i(TAG, "revertUIState on pointMakeup item");
        this.pointMakeupChecked = false;
        this.uncheckPointMakeup();
    }


    private void uncheckPointMakeup() {
        TEUIProperty lutData = this.dataCategory.get(TEUIProperty.UICategory.LUT);
        if (lutData != null) {
            ProviderUtils.revertUIStateToInit(Collections.singletonList(lutData));
        }
        if (this.pointMakeup != null) {
            ProviderUtils.revertUIStateToInit(this.pointMakeup);
        }
    }


    private void uncheckLightMakeup() {
        TEUIProperty lightMakeup = this.dataCategory.get(TEUIProperty.UICategory.LIGHT_MAKEUP);
        if (lightMakeup == null) {
            return;
        }
        ProviderUtils.revertUIStateToInit(Collections.singletonList(lightMakeup));
    }


    @Override
    public boolean isShowEntryBtn() {
        return true;
    }
}
