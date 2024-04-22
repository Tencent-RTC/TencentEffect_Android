package com.tencent.effect.beautykit.view.panelview;

import com.tencent.effect.beautykit.model.TEUIProperty;

/**
 * TEPanelView 或 TETemplatePanelView 的点击事件回调
 */
public interface TEPanelViewCallback {

//    /**
//     * 当关闭或打开特效开关时回调此方法
//     *
//     * @param isClose
//     */
//    void onCloseEffect(boolean isClose);
//
//    /**
//     * 在此方法中接收用于还原效果的数据
//     *
//     * @param properties
//     */
//    void onRevertTE(List<TEUIProperty.TESDKParam> properties);
//
//
//    /**
//     * 当点击item或者滑动滑竿的时候
//     *
//     * @param param 需要更新的美颜属性
//     */
//    void onUpdateEffect(TEUIProperty.TESDKParam param);
//
//    /**
//     * 当点击item或者滑动滑竿的时候
//     *
//     * @param paramList 需要更新的美颜属性列表
//     */
//    void onUpdateEffectList(List<TEUIProperty.TESDKParam> paramList);
//
//
//    /**
//     * 此方法用于将默认的美颜效果列表 属性返回给客户
//     * @param paramList
//     */
//    void onDefaultEffectList(List<TEUIProperty.TESDKParam> paramList);
    /**
     * 由于绿幕和 自定义分割属性比较特殊，需要特殊处理，所以单独通过此方法返回对应的数据
     * 当点击了绿幕 或者 自定义分割的时候会回调此方法
     * @param uiProperty
     */
    void onClickCustomSeg(TEUIProperty uiProperty);
    /**
     * 当点击拍照按钮的时候回调
     */
    void onCameraClick();

    /**
     * 当面板设置美颜数据给TEBeautyKit时会执行此方法
     */
    void onUpdateEffected();

}
