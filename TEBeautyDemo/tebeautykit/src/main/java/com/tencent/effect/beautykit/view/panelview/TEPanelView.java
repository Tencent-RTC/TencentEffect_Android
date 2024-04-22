package com.tencent.effect.beautykit.view.panelview;

import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.EXTRA_INFO_KEY_SEG_TYPE;

import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.provider.TEGeneralDataProvider;
import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.model.TEPanelMenuCategory;
import com.tencent.effect.beautykit.model.TEPanelMenuModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.provider.TEPanelDataProvider;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.view.dialog.TETipDialog;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * 美颜面板
 * 外部使用此view
 */
public class TEPanelView extends FrameLayout implements ITEPanelView {

    private static final String TAG = TEPanelView.class.getName();

    //美颜分类菜单页面
    private TEPanelMenuView mPanelMenuView;
    //美颜明细页面
    private TEDetailPanel mDetailPanel;
    //美颜明细页面的回调接口
    private InternalDetailPanelListenerImpl mDetailPanelListener = null;
    private TEPanelMenuCategory currentMenuCategory = TEPanelMenuCategory.BEAUTY;

    private final Map<TEPanelMenuCategory, TEPanelDataProvider> panelDataProviders = new ArrayMap<>();

    /**
     * 用于存放上一次美颜对应效果的数据
     */
    private List<TEUIProperty.TESDKParam> lastParamList = null;


    public TEPanelView(@NonNull Context context) {
        this(context, null);
    }

    public TEPanelView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TEPanelView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }


    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.te_beauty_panel_view_layout, this, true);
        this.mPanelMenuView = findViewById(R.id.te_panel_view_beauty_menu_view);
        this.mDetailPanelListener = new InternalDetailPanelListenerImpl(this);
        this.mPanelMenuView.setListener(new TEPanelMenuView.TEPanelMenuViewListener() {
            @Override
            public void onPanelMenuItemClick(TEPanelMenuCategory panelDataType) {
                if (panelDataType == TEPanelMenuCategory.CAMERA && mDetailPanelListener != null) {   //表示点击了相机按钮
                    mDetailPanelListener.onCameraClick();
                } else {
                    currentMenuCategory = panelDataType;
                    showPanelView();
                }
            }

            @Override
            public void onCloseEffect(boolean isClose) {
                if (mDetailPanelListener != null) {
                    mDetailPanelListener.setMenuEffectState(isClose ? TEBeautyKit.EffectState.DISABLED : TEBeautyKit.EffectState.ENABLED);
                }
            }
        });
        this.mDetailPanel = findViewById(R.id.te_panel_view_detail_panel);
    }


    /**
     * 用于设置美颜上次效果的数据，目的是将美颜面板还原到上次的状态，
     * 注意：此方法需要在 {@link #showView(TEPanelMenuModel, TEPanelMenuCategory, int)}} 方法之前使用
     *
     * @param lastParamList 美颜数据
     */
    @Override
    public void setLastParamList(String lastParamList) {
        if (!TextUtils.isEmpty(lastParamList)) {
            Type type = new TypeToken<List<TEUIProperty.TESDKParam>>() {
            }.getType();
            try {
                this.lastParamList = new Gson().fromJson(lastParamList, type);
            } catch (Exception e) {
                LogUtils.e(TAG, "JSON parsing failed, please check the json string");
                e.printStackTrace();
            }
        }

    }


    /**
     * 展示美颜面板
     */
    @Override
    public void showView(TEPanelViewCallback tePanelViewCallback) {
        this.showView(null, tePanelViewCallback);
    }

    /**
     * 展示美颜面板
     *
     * @param dataList
     */
    @Override
    public void showView(@Nullable List<TEPanelDataModel> dataList, TEPanelViewCallback tePanelViewCallback) {
        this.setTEPanelViewCallback(tePanelViewCallback);
        TEGeneralDataProvider tePanelDataProvider = new TEGeneralDataProvider();
        if (dataList != null) {   //优先使用传入的数据
            tePanelDataProvider.setPanelDataList(dataList);
        } else {
            if (TEUIConfig.getInstance().getPanelDataList() != null && TEUIConfig.getInstance().getPanelDataList().size() > 0) {
                tePanelDataProvider.setPanelDataList(TEUIConfig.getInstance().getPanelDataList());
            } else {
                throw new RuntimeException("please set panel data list");
            }
        }
        if (this.lastParamList != null && this.lastParamList.size() > 0) {
            tePanelDataProvider.setUsedParams(this.lastParamList);
        }
        tePanelDataProvider.getPanelData(getContext().getApplicationContext());
        this.mDetailPanelListener.onDefaultEffectList(tePanelDataProvider.getUsedProperties());

        this.mDetailPanel.show(tePanelDataProvider, this.mDetailPanelListener);
        this.mDetailPanel.setVisibility(VISIBLE);
        this.removeView(this.mPanelMenuView);
        this.showBottomLayout(false);
        this.showTopRightLayout(true);
        this.showMenu(false);
    }

    /**
     * 将TUIEffectApi 和 panelView进行绑定处理
     *
     * @param beautyKit
     */
    @Override
    public void setupWithTEBeautyKit(TEBeautyKit beautyKit) {
        this.mDetailPanelListener.setTEBeautyKit(beautyKit);
    }


    /**
     * 展示面板，此方法需要传入对应的数据
     *
     * @param beautyPanelMenuData 面板数据
     * @param menuCategory        当前展示的菜单数据，可以为null,如果为null,则表示进入之后，直接展示的是菜单，如果不为null则展示的是菜单中具体的数据
     * @param tabIndex            菜单中的数据因为是多个标签，通过此参数设置刚打开时的选中的是第几个标签，
     *                            比如在美颜美型菜单中，有四个标签（美颜、画面调整、高级美型、美体），
     *                            当从单点能力美颜进入的时候就需要选中，美颜，当从美体进入时就需要选中美体，所以可以通过这个参数控制选中对应的标签
     */
    @Override
    public void showView(TEPanelMenuModel beautyPanelMenuData, TEPanelMenuCategory menuCategory, int tabIndex) {
        this.currentMenuCategory = menuCategory;
        this.createProviders(beautyPanelMenuData);
        this.setDependencies();
        if (this.currentMenuCategory != null) {
            this.getCurrentProvider().onTabItemClick(tabIndex);
            this.showPanelView();
        } else {
            this.showMenuView();
        }
    }


    /**
     * 设置选中 自定义分割或者绿幕的item ，因为绿幕或者自定义分割按钮在点击之后是跳转到相册，
     * 只有用户选择了图片或者视频之后才会选中，如果在这个过程中用户取消了操作就不能选中对应的item
     *
     * @param uiProperty
     */
    @Override
    public void checkPanelViewItem(TEUIProperty uiProperty) {
        this.mDetailPanel.checkPanelViewItem(uiProperty);
    }

    /**
     * 设置事件回调接口
     * 注意：此方法需要在 {@link #showView(TEPanelMenuModel, TEPanelMenuCategory, int)}} 方法之前使用
     *
     * @param tePanelViewCallback 面板事件回调接口
     */
    @Override
    public void setTEPanelViewCallback(TEPanelViewCallback tePanelViewCallback) {
        if (this.mDetailPanelListener != null) {
            this.mDetailPanelListener.setBeautyPanelCallback(tePanelViewCallback);
        }
    }


    /**
     * 设置是否展示 美颜菜单View
     *
     * @param isShowMenu true 表示展示，false 表示隐藏，默认是展示的
     */
    @Override
    public void showMenu(boolean isShowMenu) {
        this.mDetailPanel.showBottomBtn(true, isShowMenu, TEDetailPanel.TE_PANEL_VIEW_EXPAND_TYPE);
    }


    /**
     * 用于显示右上角的 还原按钮
     *
     * @param isVisibility true 表示展示 false 表示隐藏  默认值为true
     */
    @Override
    public void showTopRightLayout(boolean isVisibility) {
        this.mDetailPanel.showTopRightLayout(isVisibility);
    }


    /**
     * 用于显示底部Layout
     *
     * @param isVisibility isVisibility true 表示展示 false 表示隐藏  默认值为false
     */
    @Override
    public void showBottomLayout(boolean isVisibility) {
        this.mDetailPanel.showBottomLayout(isVisibility);
    }

    @Override
    public void updateUIConfig(TEUIConfig uiConfig) {
        if (this.mDetailPanel != null) {
            this.mDetailPanel.updatePanelUIConfig(uiConfig);
        }
    }

    private void showPanelView() {
        TEPanelDataProvider panelDataProvider = this.getCurrentProvider();
        if (panelDataProvider != null) {
            this.mDetailPanel.show(panelDataProvider, this.mDetailPanelListener);
            this.mDetailPanel.setVisibility(VISIBLE);
            this.mPanelMenuView.setVisibility(GONE);
        } else {
            Toast.makeText(getContext(), R.string.te_beauty_panel_view_not_included_tips, Toast.LENGTH_LONG).show();
            LogUtils.e(TAG, "The edition does not include that capability");
        }
    }


    private void showMenuView() {
        mDetailPanel.setVisibility(GONE);
        mPanelMenuView.setVisibility(VISIBLE);
    }

    /**
     * 创建所有的provider对象
     */
    private void createProviders(TEPanelMenuModel beautyPanelMenuData) {
        List<TEPanelMenuCategory> TEPanelMenuCategoryList = beautyPanelMenuData.getPanelMenuCategories();
        List<TEUIProperty.TESDKParam> defaultList = new ArrayList<>();
        for (TEPanelMenuCategory panelMenuCategory : TEPanelMenuCategoryList) {
            if (TextUtils.isEmpty(panelMenuCategory.className)) {
                LogUtils.e(TAG, "panelMenuCategory className is null");
                continue;
            }
            try {
                TEPanelDataProvider provider = (TEPanelDataProvider) Class.forName(panelMenuCategory.className).newInstance();
                // 在此处设置 使用过的美颜属性，这样可以将面板恢复到上次的状态
                if (this.lastParamList != null && this.lastParamList.size() > 0) {
                    provider.setUsedParams(this.lastParamList);
                }
                //设置数据
                provider.setPanelDataList(beautyPanelMenuData.getDataByType(panelMenuCategory));
                provider.getPanelData(getContext().getApplicationContext());
                defaultList.addAll(provider.getUsedProperties());
                this.panelDataProviders.put(panelMenuCategory, provider);
                this.mPanelMenuView.setItemClickable(panelMenuCategory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.mDetailPanelListener != null && defaultList.size() > 0) {
            this.mDetailPanelListener.onDefaultEffectList(defaultList);
        }

    }

    /**
     * 设置依赖关系
     */
    private void setDependencies() {
        TEPanelDataProvider motionProvider = this.panelDataProviders.get(TEPanelMenuCategory.MOTION);
        TEPanelDataProvider makeupProvider = this.panelDataProviders.get(TEPanelMenuCategory.MAKEUP);
        if (motionProvider != null && makeupProvider != null) {
            motionProvider.putMutuallyExclusiveProvider(Collections.singletonList(makeupProvider));
            makeupProvider.putMutuallyExclusiveProvider(Collections.singletonList(motionProvider));
        }
    }


    private TEPanelDataProvider getCurrentProvider() {
        return this.panelDataProviders.get(this.currentMenuCategory);
    }

    private static class InternalDetailPanelListenerImpl implements TEDetailPanel.TEDetailPanelListener {
        private final Context context;
        private TEPanelViewCallback mBeautyPanelCallback;
        private final TEPanelView mPanelView;
        private long onCameraClickLastTime = 0;

        private TEBeautyKit beautyKit = null;
        //用于记录菜单上的美颜开关状态，
        private TEBeautyKit.EffectState menuEffectState = TEBeautyKit.EffectState.ENABLED;

        private List<TEUIProperty.TESDKParam> defaultParamList = null;


        public InternalDetailPanelListenerImpl(@NonNull TEPanelView panelView) {
            this.context = panelView.getContext();
            this.mPanelView = panelView;
        }

        /**
         * 设置TUIEffectApi对象，并建立关系
         *
         * @param beautyKit
         */
        public void setTEBeautyKit(TEBeautyKit beautyKit) {
            this.beautyKit = beautyKit;
            if (defaultParamList != null && defaultParamList.size() > 0 && this.beautyKit != null) {
                this.beautyKit.setEffectList(this.defaultParamList);
                this.notificationEffectChange();
            }
        }

        public void setBeautyPanelCallback(TEPanelViewCallback mBeautyPanelCallback) {
            this.mBeautyPanelCallback = mBeautyPanelCallback;
        }

        @Override
        public void onTopRightBtnClick() {
            TETipDialog.showRevertDialog(this.context, new TETipDialog.TipDialogClickListener() {
                @Override
                public void onLeftBtnClick(Button btn) {

                }

                @Override
                public void onRightBtnCLick(Button btn) {
                    mPanelView.mDetailPanel.revertEffect();
                }
            });
        }

        @Override
        public void onLeftBottomBtnClick(int type) {
            onTopRightBtnClick();
        }

        @Override
        public void onRightBottomBtnClick(int type) {
            if (mPanelView != null) {
                this.mPanelView.showMenuView();
            }
        }

        public void setMenuEffectState(TEBeautyKit.EffectState effectState) {
            this.menuEffectState = effectState;
            if (this.beautyKit != null) {
                this.beautyKit.setEffectState(this.menuEffectState);
                this.notificationEffectChange();
            }
        }

        @Override
        public void onCloseEffect(boolean isClose) {
            // 如果菜单上的美颜状态处于关闭状态，那么面板右上角的对比按钮将不再起作用，
            // 只有菜单上的开关处于开启状态，那么对比按钮才会生效
            if (this.beautyKit != null && this.menuEffectState == TEBeautyKit.EffectState.ENABLED) {
                this.beautyKit.setEffectState(isClose
                        ? TEBeautyKit.EffectState.DISABLED
                        : TEBeautyKit.EffectState.ENABLED);
                this.notificationEffectChange();
            }
        }

        @Override
        public void onRevertTE(List<TEUIProperty.TESDKParam> sdkParams) {
            if (this.beautyKit != null) {
                this.beautyKit.setEffectList(sdkParams);
                this.notificationEffectChange();
            }
        }


        @Override
        public void onUpdateEffect(TEUIProperty.TESDKParam sdkParam) {
            if (this.beautyKit != null) {
                this.beautyKit.setEffect(sdkParam);
                this.notificationEffectChange();
            }
        }

        @Override
        public void onUpdateEffectList(List<TEUIProperty.TESDKParam> sdkParams) {
            if (this.beautyKit != null) {
                this.beautyKit.setEffectList(sdkParams);
                this.notificationEffectChange();
            }
        }


        public void onDefaultEffectList(List<TEUIProperty.TESDKParam> paramList) {
            this.defaultParamList = paramList;
            if (this.beautyKit != null && this.defaultParamList != null && this.defaultParamList.size() > 0) {
                this.beautyKit.setEffectList(this.defaultParamList);
                this.notificationEffectChange();
            }
        }

        @Override
        public void onClickCustomSeg(TEUIProperty uiProperty) {
            if (mBeautyPanelCallback == null) {
                return;
            }
            if (uiProperty.sdkParam != null && uiProperty.sdkParam.extraInfo != null && TEUIProperty.TESDKParam.EXTRA_INFO_SEG_TYPE_GREEN.equals(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_SEG_TYPE))) {
                TETipDialog.showGreenScreenTipDialog(this.context, new TETipDialog.TipDialogClickListener() {
                    @Override
                    public void onLeftBtnClick(Button btn) {

                    }

                    @Override
                    public void onRightBtnCLick(Button btn) {
                        if (mBeautyPanelCallback != null) {
                            mBeautyPanelCallback.onClickCustomSeg(uiProperty);
                        }
                    }
                });
            } else {
                if (mBeautyPanelCallback != null) {
                    mBeautyPanelCallback.onClickCustomSeg(uiProperty);
                }
            }
        }


        @Override
        public void onCameraClick() {
            //对拍照功能进行限制，3秒钟之内只能使用一次
            long currentTime = System.currentTimeMillis();
            if (mBeautyPanelCallback != null && currentTime - onCameraClickLastTime >= 3 * 1000) {
                onCameraClickLastTime = currentTime;
                mBeautyPanelCallback.onCameraClick();
            }
        }

        private void notificationEffectChange() {
            if (this.mBeautyPanelCallback != null) {
                this.mBeautyPanelCallback.onUpdateEffected();
            }
        }
    }

}
