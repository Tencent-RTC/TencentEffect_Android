package com.tencent.effect.beautykit.provider;

import android.content.Context;
import android.util.Log;


import com.google.gson.Gson;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;

import java.util.Collections;
import java.util.List;


public class TEMotionPanelDataProvider extends TEAbstractPanelDataProvider {


    @Override
    public List<TEUIProperty> onItemClick(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return null;
        }
        if ((uiProperty.propertyList == null && uiProperty.sdkParam != null) || uiProperty.isNoneItem()) {
            ProviderUtils.revertUIState(allData, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty,TEUIProperty.UIState.CHECKED_AND_IN_USE);
        }
        return uiProperty.propertyList;
    }

    @Override
    public void selectPropertyItem(TEUIProperty uiProperty) {
        if (uiProperty != null) {
            ProviderUtils.revertUIState(allData, uiProperty);
            ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
        }
    }

    @Override
    public List<TEUIProperty.TESDKParam> getRevertData(Context context) {
        this.forceRefreshPanelData(context.getApplicationContext());
        return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_MOTION));
    }

    @Override
    public List<TEUIProperty.TESDKParam> getCloseEffectItems(TEUIProperty uiProperty) {
        if(uiProperty.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM){  //绿幕2 的关闭项按钮
           TEUIProperty gsv2uiProperty = uiProperty.parentUIProperty;
           gsv2uiProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH,"");
           return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_SEGMENTATION));
        }
        return Collections.singletonList(ProviderUtils.createNoneItem(XmagicConstant.EffectName.EFFECT_MOTION));
    }


    @Override
    public boolean isShowCompareBtn() {
        return false;
    }


    @Override
    public boolean isShowEntryBtn() {
        return true;
    }
}
