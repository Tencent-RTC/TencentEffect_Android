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
     * 用于设置美颜上次效果的数据，目的是将美颜面板还原到上次的状态，
     * 注意：此方法需要在 {@link #showView(TEPanelMenuModel, TEPanelMenuCategory, int)}} 方法之前使用
     *
     * @param lastParamList 美颜数据,可以通过 {@link TEBeautyKit#exportInUseSDKParam()} ()}}方法获取，然后存储，在下次启动美颜的时候传入此字符串即可
     */
    void setLastParamList(String lastParamList);

    /**
     * 设置事件回调接口
     * 注意：此方法需要在 {@link #showView(TEPanelMenuModel, TEPanelMenuCategory, int)}} 方法之前使用
     *
     * @param tePanelViewCallback 面板事件回调接口
     */
    void setTEPanelViewCallback(TEPanelViewCallback tePanelViewCallback);

    /**
     * 展示美颜面板
     * @param tePanelViewCallback 面板事件回调接口
     */
    void showView(TEPanelViewCallback tePanelViewCallback);


    /**
     * 展示美颜面板
     * @param dataModelList  面板数据
     *        tePanelMenuModel 面板事件回调接口
     */
    void showView(List<TEPanelDataModel> dataModelList,TEPanelViewCallback tePanelViewCallback);


    /**
     * 绑定TEBeautyKit对象，当用户点击item的时候，面板会直接调用TEBeautyKit的方法进行属性设置
     * @param beautyKit TEBeautyKit对象
     */
    void setupWithTEBeautyKit(TEBeautyKit beautyKit);

    /**
     * 展示面板，此方法需要传入对应的数据
     *
     * @param beautyPanelMenuData 面板数据
     * @param menuCategory        当前展示的菜单数据，可以为null,如果为null,则表示进入之后，直接展示的是菜单，如果不为null则展示的是菜单中具体的数据
     * @param tabIndex            菜单中的数据因为是多个标签，通过此参数设置刚打开时的选中的是第几个标签，
     *                            比如在美颜美型菜单中，有四个标签（美颜、画面调整、高级美型、美体），
     *                            当从单点能力美颜进入的时候就需要选中，美颜，当从美体进入时就需要选中美体，所以可以通过这个参数控制选中对应的标签
     */
    void showView(TEPanelMenuModel beautyPanelMenuData, TEPanelMenuCategory menuCategory, int tabIndex);


    /**
     * 设置选中 自定义分割或者绿幕的item ，因为绿幕或者自定义分割按钮在点击之后是跳转到相册，
     * 只有用户选择了图片或者视频之后才会选中，如果在这个过程中用户取消了操作就不能选中对应的item
     *
     * @param uiProperty
     */
    void checkPanelViewItem(TEUIProperty uiProperty);



    /**
     * 设置是否展示 美颜菜单View
     *
     * @param isShowMenu true 表示展示，false 表示隐藏，默认是展示的
     */
    void showMenu(boolean isShowMenu);


    /**
     * 用于显示右上角的 还原按钮
     * @param isVisibility true 表示展示 false 表示隐藏  默认值为true
     */
     void showTopRightLayout(boolean isVisibility) ;

    /**
     * 用于显示底部Layout
     * @param isVisibility isVisibility true 表示展示 false 表示隐藏  默认值为false
     */
     void showBottomLayout(boolean isVisibility);


    /**
     * 设置当前面板的UI配置
     * @param uiConfig
     */
    void updateUIConfig(TEUIConfig uiConfig);

}
