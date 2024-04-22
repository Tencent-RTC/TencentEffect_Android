package com.tencent.effect.beautykit.view.panelview;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelMenuCategory;
import com.tencent.effect.beautykit.model.TEPanelMenuModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

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


    void setupWithTEBeautyKit(TEBeautyKit beautyKit);

    /**
     * Show the panel by passing the corresponding data.
     *
     * @param beautyPanelMenuData The panel data
     * @param menuCategory        The current menu data to display. It can be null, which means that the menu will be displayed directly after entering. If it is not null, it will display the specific data in the menu.
     * @param tabIndex            The index of the selected tab when opening the menu. For example, in the beauty and shaping menu, there are four tabs (beauty, image adjustment, advanced shaping, body shaping). When entering from a single-point beauty capability, the beauty tab needs to be selected. When entering from body shaping, the body shaping tab needs to be selected. This parameter can be used to control the selection of the corresponding tab.
     */
    void showView(TEPanelMenuModel beautyPanelMenuData, TEPanelMenuCategory menuCategory, int tabIndex);


    void checkPanelViewItem(TEUIProperty uiProperty);


    void showMenu(boolean isShowMenu);


    void showTopRightLayout(boolean isVisibility);


    void showBottomLayout(boolean isVisibility);


    void updateUIConfig(TEUIConfig uiConfig);

}
