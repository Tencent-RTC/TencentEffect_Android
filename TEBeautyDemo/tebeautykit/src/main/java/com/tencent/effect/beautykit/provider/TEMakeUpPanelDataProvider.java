package com.tencent.effect.beautykit.provider;

import android.content.Context;


import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.manager.TEParamManager;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 美妆数据提供类
 * <p>
 * 单点美妆和风格整装
 */
public class TEMakeUpPanelDataProvider extends TEAbstractPanelDataProvider {

    private List<TEPanelDataProvider> dependentProviderList = null;

    @Override
    public List<TEUIProperty> onItemClick(TEUIProperty uiProperty) {
        List<TEUIProperty> processData = new ArrayList<>();
        for (TEUIProperty property : allData) {   //通过此方法将 单点妆容 和 整妆 区分开
            if (property.uiCategory == uiProperty.uiCategory) {
                processData.add(property);
            }
        }
        if ((uiProperty.propertyList == null && uiProperty.sdkParam != null) || uiProperty.isNoneItem()) {
            ProviderUtils.revertUIState(processData, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
            if (uiProperty.uiCategory == TEUIProperty.UICategory.MAKEUP && this.dependentProviderList != null) {  // 风格整装的情况
                for (TEPanelDataProvider provider : this.dependentProviderList) {
                    provider.unCheckAll();
                }
            }
        }
        return uiProperty.propertyList;
    }

    @Override
    public List<TEUIProperty.TESDKParam> getRevertData(Context context) {
        TEUIProperty beautyProperty = null;  //单点妆容数据
        TEUIProperty makeupProperty = null;   //整妆数据
        boolean hasForceFreshData = false;
        for (TEUIProperty uiProperty : allData) {
            if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY) {
                beautyProperty = uiProperty;
            } else if (uiProperty.uiCategory == TEUIProperty.UICategory.MAKEUP) {
                makeupProperty = uiProperty;
            }
        }
        TEParamManager paramManager = new TEParamManager();
        if (beautyProperty != null) {
            //获取使用的item
            List<TEUIProperty.TESDKParam> usedList = ProviderUtils.getUsedProperties(allData);
            //从新获取数据
            this.forceRefreshPanelData(context.getApplicationContext());
            hasForceFreshData = true;
            //获取默认的选中项
            List<TEUIProperty.TESDKParam> defaultUsedList = ProviderUtils.getUsedProperties(allData);
            paramManager.putTEParams(ProviderUtils.clone0ValuedParam(usedList));
            paramManager.putTEParams(defaultUsedList);
        }
        if (makeupProperty != null) {
            if (!hasForceFreshData) {
                this.forceRefreshPanelData(context.getApplicationContext());
            }
            paramManager.putTEParam(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_MAKEUP));
        }
        return paramManager.getParams();
    }

    @Override
    public List<TEUIProperty.TESDKParam> getCloseEffectItems(TEUIProperty uiProperty) {
        TEUIProperty teuiProperty = ProviderUtils.getUIPropertyByCategory(this.allData, uiProperty.uiCategory);
        if (teuiProperty == null) {
            return null;
        }
        if (teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY) {  //单点妆容
            List<TEUIProperty.TESDKParam> usedList = ProviderUtils.getUsedProperties(teuiProperty.propertyList);
            ProviderUtils.changParamValuedTo0(usedList);
            return usedList;
        } else if (teuiProperty.uiCategory == TEUIProperty.UICategory.MAKEUP) {  //风格整装
            return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_MAKEUP));
        }
        return null;
    }


    @Override
    public void putMutuallyExclusiveProvider(List<TEPanelDataProvider> providerList) {
        super.putMutuallyExclusiveProvider(providerList);
        this.dependentProviderList = providerList;
    }

    @Override
    public void unCheckAll() {
        super.unCheckAll();
        for (TEUIProperty property : allData) {
            if (property.uiCategory == TEUIProperty.UICategory.MAKEUP) {
                ProviderUtils.revertUIState(Collections.singletonList(property), null);
            }
        }
    }


}
