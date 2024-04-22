package com.tencent.effect.beautykit.provider;

import android.content.Context;


import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.List;

/**
 * 面板数据提供接口
 */
public interface TEPanelDataProvider {


    /**
     * 设置需要加载的数据
     * @param panelDataList
     */
    void setPanelDataList(List<TEPanelDataModel> panelDataList);

    /**
     * 设置用户选中的默认数据，一般只设置美颜数据
     * @param paramList
     */
    void setUsedParams(List<TEUIProperty.TESDKParam> paramList);

    /**
     * 获取数据
     *
     * @return
     */
    List<TEUIProperty> getPanelData(Context context);

    /**
     * 强制刷新数据
     *
     * @param context 应用上下文
     * @return
     */
    List<TEUIProperty> forceRefreshPanelData(Context context);

    /**
     * 当点击分类的时候调用此方法，此方法用于处理分类数据的选中状态更换
     * @param index
     */
    void onTabItemClick(int index);


    /**
     * 当列表item被点击时调用
     *
     * @param uiProperty
     * @return 如果有子项目则返回子项目，没有则直接返回null
     */
    List<TEUIProperty> onItemClick(TEUIProperty uiProperty);




    /**
     * 获取用于还原美颜效果的属性集合
     *
     * @return
     */
    List<TEUIProperty.TESDKParam> getRevertData(Context context);

    /**
     * 用于关闭当前分类效果的 属性列表
     *
     * @return
     */
    List<TEUIProperty.TESDKParam> getCloseEffectItems(TEUIProperty uiProperty);



    /**
     * 获取用户已使用的美颜数据
     * @return
     */
    List<TEUIProperty.TESDKParam> getUsedProperties();

    /**
     * 是否展示对比按钮
     *
     * @return
     */
    boolean isShowCompareBtn();

    /**
     * 获取美颜模板的原始数据
     * @return
     */
    String getOriginalParam();

    /**
     * 取消所有选中
     */
    void unCheckAll();


    /**
     * 用于设置和当前provider有互斥关系的provider
     * @param providerList
     */
    void putMutuallyExclusiveProvider(List<TEPanelDataProvider> providerList);

    /**
     * 获取用于更新美颜效果的数据
     * @param uiProperty
     * @return
     */
    List<TEUIProperty.TESDKParam> getBeautyTemplateData(TEUIProperty uiProperty);

    /**
     * 获取需要去美颜面板进行调整的美颜属性 数据
     * @return
     */
    List<TEUIProperty.TESDKParam> getBeautyTemplateData();


    /**
     * 用于更新美颜模版的数据
     * @param paramList
     */
    void updateBeautyTemplateData(List<TEUIProperty.TESDKParam> paramList);


}
