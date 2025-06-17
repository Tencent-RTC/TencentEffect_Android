package com.tencent.effect.beautykit.view.panelview;

import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.EXTRA_INFO_KEY_SEG_TYPE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.PopupWindowCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelMenuCategory;
import com.tencent.effect.beautykit.model.TEPanelMenuModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.provider.TEGeneralDataProvider;
import com.tencent.effect.beautykit.provider.TEPanelDataProvider;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.ScreenUtils;
import com.tencent.effect.beautykit.view.dialog.TETipDialog;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class TEPanelView extends FrameLayout implements ITEPanelView {

    private static final String TAG = TEPanelView.class.getName();


    private TEPanelMenuView mPanelMenuView;

    private TEDetailPanel mDetailPanel;

    private InternalDetailPanelListenerImpl mDetailPanelListener = null;
    private TEPanelMenuCategory currentMenuCategory = TEPanelMenuCategory.BEAUTY;

    private final Map<TEPanelMenuCategory, TEPanelDataProvider> panelDataProviders = new ArrayMap<>();


    private List<TEUIProperty.TESDKParam> lastParamList = null;

    //用于关闭
    public boolean isShowGreenScreenTipDialog = true;


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
        this.mPanelMenuView = new TEPanelMenuView(getContext());
        this.mPanelMenuView.setVisibility(GONE);
        FrameLayout.LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.BOTTOM;
        this.addView(mPanelMenuView, layoutParams);
        this.mDetailPanelListener = new InternalDetailPanelListenerImpl(this);
        this.mPanelMenuView.setListener(new TEPanelMenuView.TEPanelMenuViewListener() {
            @Override
            public void onPanelMenuItemClick(TEPanelMenuCategory panelDataType) {
                if (panelDataType == TEPanelMenuCategory.CAMERA && mDetailPanelListener != null) {
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
        this.mDetailPanel = new TEDetailPanel(getContext());
        LayoutParams layoutParams1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams1.gravity = Gravity.BOTTOM;

        this.addView(mDetailPanel, layoutParams1);
    }


    /**
     * Used to set the last saved beauty effect data, in order to restore the beauty panel to its previous state.
     * Note: This method needs to be used before the {@link #showView(TEPanelMenuModel, TEPanelMenuCategory, int)} method.
     *
     * @param lastParamList Beauty data
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


    @Override
    public void showView(TEPanelViewCallback tePanelViewCallback) {
        this.showView(null, tePanelViewCallback);
    }


    @Override
    public void showView(@Nullable List<TEPanelDataModel> dataList, TEPanelViewCallback tePanelViewCallback) {
        this.setTEPanelViewCallback(tePanelViewCallback);
        TEGeneralDataProvider tePanelDataProvider = new TEGeneralDataProvider();
        if (dataList != null) {
            tePanelDataProvider.setPanelDataList(dataList);
        } else {
            if (TEUIConfig.getInstance().getPanelDataList() != null && !TEUIConfig.getInstance().getPanelDataList().isEmpty()) {
                tePanelDataProvider.setPanelDataList(TEUIConfig.getInstance().getPanelDataList());
            } else {
                throw new RuntimeException("please set panel data list");
            }
        }
        if (this.lastParamList != null && !this.lastParamList.isEmpty()) {
            tePanelDataProvider.setUsedParams(this.lastParamList);
        }
        this.showViewWithProvider(tePanelDataProvider, tePanelViewCallback);
    }


    @Override
    public void showViewWithProvider(TEPanelDataProvider provider, TEPanelViewCallback tePanelViewCallback) {
        if (provider == null) {
            return;
        }
        this.setTEPanelViewCallback(tePanelViewCallback);
        this.currentMenuCategory = TEPanelMenuCategory.ALL;
        this.panelDataProviders.put(this.currentMenuCategory, provider);


        provider.getPanelData(getContext().getApplicationContext());
        this.mDetailPanelListener.onDefaultEffectList(provider.getUsedProperties());

        this.mDetailPanel.show(provider, this.mDetailPanelListener);
        this.mDetailPanel.setVisibility(VISIBLE);
        this.removeView(this.mPanelMenuView);
        this.showBottomLayout(false);
        this.showTopRightLayout(true);
        this.showMenu(false);
    }


    @Override
    public void setupWithTEBeautyKit(TEBeautyKit beautyKit) {
        this.mDetailPanelListener.setTEBeautyKit(beautyKit);
    }


    /**
     * Show the panel, this method requires passing in the corresponding data.
     *
     * @param beautyPanelMenuData Panel data
     * @param menuCategory        The current displayed menu data, can be null. If null, it means that the menu is directly displayed after entering. If not null, it displays specific data within the menu.
     * @param tabIndex            Since the data in the menu has multiple tabs, use this parameter to set the initially selected tab.
     *                            For example, in the beauty and shaping menu, there are four tabs (beauty, image adjustment, advanced shaping, body shaping).
     *                            When entering from a single-point beauty capability, the beauty tab needs to be selected. When entering from body shaping, the body shaping tab needs to be selected. Therefore, this parameter can control the selection of the corresponding tab.
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


    @Override
    public void checkPanelViewItem(TEUIProperty uiProperty) {
        this.mDetailPanel.checkPanelViewItem(uiProperty);
    }


    @Override
    public void setTEPanelViewCallback(TEPanelViewCallback tePanelViewCallback) {
        if (this.mDetailPanelListener != null) {
            this.mDetailPanelListener.setBeautyPanelCallback(tePanelViewCallback);
        }
    }


    @Override
    public void showMenu(boolean isShowMenu) {
        this.mDetailPanel.showBottomBtn(true, isShowMenu, TEDetailPanel.TE_PANEL_VIEW_EXPAND_TYPE);
    }


    @Override
    public void showTopRightLayout(boolean isVisibility) {
        this.mDetailPanel.showTopRightLayout(isVisibility);
    }


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
                if (this.lastParamList != null && !this.lastParamList.isEmpty()) {
                    provider.setUsedParams(this.lastParamList);
                }
                provider.setPanelDataList(beautyPanelMenuData.getDataByType(panelMenuCategory));
                provider.getPanelData(getContext().getApplicationContext());
                defaultList.addAll(provider.getUsedProperties());
                this.panelDataProviders.put(panelMenuCategory, provider);
                this.mPanelMenuView.setItemClickable(panelMenuCategory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.mDetailPanelListener != null && !defaultList.isEmpty()) {
            this.mDetailPanelListener.onDefaultEffectList(defaultList);
        }

    }


    private void setDependencies() {
        TEPanelDataProvider lutProvider = this.panelDataProviders.get(TEPanelMenuCategory.LUT);
        TEPanelDataProvider makeupProvider = this.panelDataProviders.get(TEPanelMenuCategory.MAKEUP);
        if (lutProvider != null && makeupProvider != null) {
            lutProvider.putMutuallyExclusiveProvider(Collections.singletonList(makeupProvider));
            makeupProvider.putMutuallyExclusiveProvider(Collections.singletonList(lutProvider));
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
        private TEBeautyKit.EffectState menuEffectState = TEBeautyKit.EffectState.ENABLED;

        private List<TEUIProperty.TESDKParam> defaultParamList = null;


        public InternalDetailPanelListenerImpl(@NonNull TEPanelView panelView) {
            this.context = panelView.getContext();
            this.mPanelView = panelView;
        }

        public void setTEBeautyKit(TEBeautyKit beautyKit) {
            this.beautyKit = beautyKit;
            if (defaultParamList != null && !defaultParamList.isEmpty() && this.beautyKit != null) {
                this.beautyKit.setEffectList(this.defaultParamList);
                this.notificationEffectChange(this.defaultParamList);
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
                this.notificationEffectStateChange(this.menuEffectState);
            }
        }

        @Override
        public void onCloseEffect(boolean isClose) {
            if (this.beautyKit != null && this.menuEffectState == TEBeautyKit.EffectState.ENABLED) {
                TEBeautyKit.EffectState effectState = isClose
                        ? TEBeautyKit.EffectState.DISABLED
                        : TEBeautyKit.EffectState.ENABLED;
                this.beautyKit.setEffectState(effectState);
                this.notificationEffectStateChange(effectState);
            }
        }

        @Override
        public void onRevertTE(List<TEUIProperty.TESDKParam> sdkParams) {
            if (this.beautyKit != null) {
                this.beautyKit.setEffectList(sdkParams);
                this.notificationEffectChange(sdkParams);
            }
        }


        @Override
        public void onUpdateEffect(TEUIProperty.TESDKParam sdkParam) {
            if (sdkParam == null) {
                return;
            }
            if (this.beautyKit != null) {
                this.beautyKit.setEffect(sdkParam);
                this.notificationEffectChange(Collections.singletonList(sdkParam));
            }
        }

        @Override
        public void onUpdateEffectList(List<TEUIProperty.TESDKParam> sdkParams) {
            if (sdkParams == null || sdkParams.isEmpty()) {
                return;
            }
            if (this.beautyKit != null) {
                this.beautyKit.setEffectList(sdkParams);
                this.notificationEffectChange(sdkParams);
            }
        }


        private void onDefaultEffectList(List<TEUIProperty.TESDKParam> paramList) {
            this.defaultParamList = paramList;
            if (this.beautyKit != null && this.defaultParamList != null && !this.defaultParamList.isEmpty()) {
                this.beautyKit.setEffectList(this.defaultParamList);
                this.notificationEffectChange(paramList);
            }
        }

        @Override
        public void onClickCustomSeg(TEUIProperty uiProperty) {
            if (mBeautyPanelCallback == null) {
                return;
            }
            if (uiProperty.sdkParam != null && uiProperty.sdkParam.extraInfo != null && Arrays.asList(TEUIProperty.TESDKParam.EXTRA_INFO_SEG_TYPE_GREEN).contains(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_SEG_TYPE))) {
                if (mPanelView.isShowGreenScreenTipDialog) {
                    TETipDialog.showGreenScreenTipDialog(this.context, new TETipDialog.TipDialogClickListener() {
                        @Override
                        public void onLeftBtnClick(Button btn) {

                        }

                        @Override
                        public void onRightBtnCLick(Button btn) {
                            callBackCustomSeg(uiProperty);
                        }
                    });
                    return;
                }
            }
            this.callBackCustomSeg(uiProperty);
        }

        private void callBackCustomSeg(TEUIProperty uiProperty) {
            if (mBeautyPanelCallback != null) {
                mBeautyPanelCallback.onClickCustomSeg(uiProperty);
            }
        }


        @Override
        public void onCameraClick() {
            long currentTime = System.currentTimeMillis();
            if (mBeautyPanelCallback != null && currentTime - onCameraClickLastTime >= 3 * 1000) {
                onCameraClickLastTime = currentTime;
                mBeautyPanelCallback.onCameraClick();
            }
        }

        @Override
        public void onMoreItemBtnClick() {
            mPanelView.showGridPanelView();
        }

        @Override
        public void onTitleClick(TEUIProperty uiProperty) {
            if (mBeautyPanelCallback != null) {
                mBeautyPanelCallback.onTitleClick(uiProperty);
            }
        }

        private void notificationEffectChange(List<TEUIProperty.TESDKParam> sdkParams) {
            if (this.mBeautyPanelCallback != null) {
                this.mBeautyPanelCallback.onUpdateEffected(sdkParams);
            }
        }

        private void notificationEffectStateChange(TEBeautyKit.EffectState effectState) {
            if (this.mBeautyPanelCallback != null) {
                this.mBeautyPanelCallback.onEffectStateChange(effectState);
            }
        }
    }

    public void setCustomView(View view, ViewGroup.LayoutParams layoutParams) {
        if (mDetailPanel != null) {
            mDetailPanel.setCustomView(view, layoutParams);
        }
    }

    private void showGridPanelView() {
        final int height = mDetailPanel.getHeight();
        boolean isVisibility = this.mDetailPanel.isVisibilityBottomLayout();
        if (isVisibility) {  //只有在底部已经展示的时候才隐藏
            this.mDetailPanel.showBottomLayout(false);
        }
        this.removeView(this.mDetailPanel);
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = height;
        this.setLayoutParams(layoutParams);

        this.mDetailPanel.switchLayout(TEDetailPanel.LayoutType.GRID);
        PopupWindow popupWindow = new PopupWindow(getContext());
        popupWindow.setContentView(this.mDetailPanel);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(ScreenUtils.dip2px(getContext(), TEUIConfig.getInstance().panelViewHeight));

        popupWindow.setOnDismissListener(() -> {
            this.mDetailPanel.switchLayout(TEDetailPanel.LayoutType.LINEAR);
            if (isVisibility) {
                this.mDetailPanel.showBottomLayout(isVisibility);
            }
            this.addDetailPanel();
        });
        PopupWindowCompat.showAsDropDown(popupWindow, this, 0, -popupWindow.getHeight(), Gravity.BOTTOM | Gravity.LEFT);
    }


    private void addDetailPanel() {
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        this.setLayoutParams(layoutParams);
        this.addView(mDetailPanel, 0);
    }

}
