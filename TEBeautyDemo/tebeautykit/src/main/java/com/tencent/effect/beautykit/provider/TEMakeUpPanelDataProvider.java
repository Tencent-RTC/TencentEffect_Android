package com.tencent.effect.beautykit.provider;

import android.content.Context;
import android.util.ArrayMap;

import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.manager.TEParamManager;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用于单点妆容和轻美妆
 */
public class TEMakeUpPanelDataProvider extends TEAbstractPanelDataProvider {

    private List<TEPanelDataProvider> dependentProviderList = null;
    private Map<TEUIProperty.UICategory, TEUIProperty> dataCategory = new ArrayMap<>();

    private boolean pointMakeupChecked = false;
    private boolean lightMakeupChecked = false;


    @Override
    public List<TEUIProperty> forceRefreshPanelData(Context context) {
        super.forceRefreshPanelData(context);
        for (TEUIProperty teuiProperty : allData) {
            if(teuiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP){
                lightMakeupChecked = ProviderUtils.getUsedProperties(Collections.singletonList(teuiProperty)).size() > 0;
            }
            if(teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY){
                pointMakeupChecked = ProviderUtils.getUsedProperties(Collections.singletonList(teuiProperty)).size() > 0;
            }
            dataCategory.put(teuiProperty.uiCategory, teuiProperty);
        }
        return allData;
    }

    @Override
    public List<TEUIProperty> onItemClick(TEUIProperty uiProperty) {
        return this.onItemClickInternal(uiProperty,true);
    }

    public List<TEUIProperty> onItemClickInternal(TEUIProperty uiProperty, boolean isFromUI) {
        this.onclickItem(uiProperty);
        if ((!isFromUI) || ((uiProperty.propertyList == null && uiProperty.sdkParam != null) || uiProperty.isNoneItem())) {
            List<TEUIProperty> processData = Collections.singletonList(dataCategory.get(uiProperty.uiCategory));
            ProviderUtils.revertUIState(processData, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
            if (uiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP && this.dependentProviderList != null) {
                for (TEPanelDataProvider provider : this.dependentProviderList) {
                    provider.unCheckAll();
                }
            }
        }
        return uiProperty.propertyList;
    }

    @Override
    public void selectPropertyItem(TEUIProperty uiProperty) {
        this.onItemClickInternal(uiProperty,false);
    }


    private void onclickItem(TEUIProperty uiProperty) {
        if (uiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) {
            this.lightMakeupChecked = true;
            if (this.pointMakeupChecked) {
                this.pointMakeupChecked = false;
                ProviderUtils.revertUIStateToInit(Collections.singletonList(dataCategory.get(TEUIProperty.UICategory.BEAUTY)));
            }
        } else if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY) {
            this.pointMakeupChecked = true;
            if (this.lightMakeupChecked) {
                this.lightMakeupChecked = false;
                ProviderUtils.revertUIStateToInit(Collections.singletonList(dataCategory.get(TEUIProperty.UICategory.LIGHT_MAKEUP)));
            }
        }
    }



    @Override
    public List<TEUIProperty.TESDKParam> getRevertData(Context context) {
        TEUIProperty beautyProperty = null;
        TEUIProperty makeupProperty = null;
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
            List<TEUIProperty.TESDKParam> usedList = ProviderUtils.getUsedProperties(allData);
            this.forceRefreshPanelData(context.getApplicationContext());
            hasForceFreshData = true;
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
        if (teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY) {
            List<TEUIProperty.TESDKParam> usedList = ProviderUtils.getUsedProperties(teuiProperty.propertyList);
            ProviderUtils.changParamValuedTo0(usedList);
            return usedList;
        } else if (teuiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) {
            return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_LIGHT_MAKEUP));
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
            if (property.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) {
                ProviderUtils.revertUIState(Collections.singletonList(property), null);
            }
        }
    }


}
