package com.tencent.effect.beautykit.view.panelview;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelMenuModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.provider.TEPanelDataProvider;

import java.util.List;

public interface ITEPanelView {

    /**
     * Used to set the data of the previous beauty effect. The purpose is to restore the beauty panel to its previous state.
     * Note: This method needs to be used before the {@link #showView(TEPanelMenuModel, TEPanelMenuCategory, int)}} method.
     *
     * @param lastParamList The beauty data, which can be obtained using the {@link TEBeautyKit#exportInUseSDKParam()} method and stored. Pass this string when starting the beauty effect next time.
     */
    void setLastParamList(String lastParamList);

    /**
     * Set the event callback interface.
     * Note: This method needs to be used before the {@link #showView(TEPanelMenuModel, TEPanelMenuCategory, int)}} method.
     *
     * @param tePanelViewCallback The panel event callback interface
     */
    void setTEPanelViewCallback(TEPanelViewCallback tePanelViewCallback);


    void showView(TEPanelViewCallback tePanelViewCallback);


    void showView(List<TEPanelDataModel> dataModelList, TEPanelViewCallback tePanelViewCallback);

    void showViewWithProvider(TEPanelDataProvider provider, TEPanelViewCallback tePanelViewCallback);

    void setupWithTEBeautyKit(TEBeautyKit beautyKit);



    void checkPanelViewItem(TEUIProperty uiProperty);




    void showTopRightLayout(boolean isVisibility);



    void revertEffect();

    void updateUIConfig(TEUIConfig uiConfig);

}
