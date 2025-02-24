package com.tencent.effect.beautykit.view.panelview;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.List;


public interface TEPanelViewCallback {

    void onClickCustomSeg(TEUIProperty uiProperty);

    void onCameraClick();

    void onUpdateEffected(List<TEUIProperty.TESDKParam> sdkParams);

    void onEffectStateChange(TEBeautyKit.EffectState effectState);

    void onTitleClick(TEUIProperty uiProperty);
}
