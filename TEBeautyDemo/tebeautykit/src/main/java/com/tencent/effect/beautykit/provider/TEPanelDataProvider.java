package com.tencent.effect.beautykit.provider;

import android.content.Context;


import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.List;

/**
 * Panel data processing interface.
 */
public interface TEPanelDataProvider {



    void setPanelDataList(List<TEPanelDataModel> panelDataList);


    void setUsedParams(List<TEUIProperty.TESDKParam> paramList);


    List<TEUIProperty> getPanelData(Context context);


    List<TEUIProperty> forceRefreshPanelData(Context context);

    /**
     * This method is called when a category is clicked. It is used to handle the change in selected state of category data.
     * @param index
     */
    void onTabItemClick(int index);


    /**

     Called when a list item is clicked.
     @param uiProperty The UI property associated with the clicked item.
     @return If there are sub-items, return the sub-items; otherwise, return null directly.
     */
    List<TEUIProperty> onItemClick(TEUIProperty uiProperty);


    void selectPropertyItem(TEUIProperty uiProperty);




    List<TEUIProperty.TESDKParam> getRevertData(Context context);

    /**
     * Property list used to close the current category effect.
     *
     * @return
     */
    List<TEUIProperty.TESDKParam> getCloseEffectItems(TEUIProperty uiProperty);




    List<TEUIProperty.TESDKParam> getUsedProperties();


    boolean isShowCompareBtn();


    String getOriginalParam();


    void unCheckAll();



    void putMutuallyExclusiveProvider(List<TEPanelDataProvider> providerList);


    List<TEUIProperty.TESDKParam> getBeautyTemplateData(TEUIProperty uiProperty);


    List<TEUIProperty.TESDKParam> getBeautyTemplateData();



    void updateBeautyTemplateData(List<TEUIProperty.TESDKParam> paramList);


    boolean isShowEntryBtn();

}
