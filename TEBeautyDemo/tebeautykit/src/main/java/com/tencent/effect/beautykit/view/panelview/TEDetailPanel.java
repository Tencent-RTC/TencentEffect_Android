package com.tencent.effect.beautykit.view.panelview;

import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH;
import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.EXTRA_INFO_KEY_KEY_COLOR;
import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.EXTRA_INFO_KEY_SEG_TYPE;
import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.GREEN_PARAMS_V2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.download.TEDownloadResult;
import com.tencent.effect.beautykit.model.TEMotionDLModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.provider.TEGeneralDataProvider;
import com.tencent.effect.beautykit.provider.TEPanelDataProvider;
import com.tencent.effect.beautykit.utils.CustomDrawableUtils;
import com.tencent.effect.beautykit.utils.FileUtil;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.PanelDisplay;
import com.tencent.effect.beautykit.utils.ScreenUtils;
import com.tencent.effect.beautykit.utils.provider.LightMakeupItemBuilder;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;
import com.tencent.effect.beautykit.view.widget.SwitchLayout;
import com.tencent.effect.beautykit.view.widget.indicatorseekbar.IndicatorSeekBar;
import com.tencent.effect.beautykit.view.widget.indicatorseekbar.OnSeekChangeListener;
import com.tencent.effect.beautykit.view.widget.indicatorseekbar.SeekParams;
import com.tencent.xmagic.XmagicConstant;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class TEDetailPanel extends FrameLayout implements View.OnClickListener,
        TEDetailPanelAdapter.ItemClickListener, SwitchLayout.SwitchLayoutListener {


    private static final String TAG = TEDetailPanel.class.getName();


    private IndicatorSeekBar indicatorSeekBar;
    private SwitchLayout switchLayout;
    private ImageView compareBtn;
    private TEBeautyTabLayout mainTabLayout;
    private TEBeautyTabLayout subTabLayout;

    private View titleDivider;
    private View topRightDivider;
    private RelativeLayout mainTitleLayout;

    private LinearLayout rightRevertLayout;
    private TextView rightRevertText;
    private ImageView backBtn;
    private TextView titleText;


    private TEPanelDataProvider panelDataProvider;
    private TEDetailPanelAdapter recycleViewAdapter;
    private TEDetailPanelListener panelViewListener;

    private TEDownloadHelper downloadHelper = null;


    private RecyclerView recyclerView;
    private View panelBgView;

    // Used to store the previous scroll position of the list. Once this position information is used, it should be deleted.
    private final List<Integer> itemPositions = new ArrayList<>();


    private LayoutType layoutType = LayoutType.LINEAR;

    private final int recycleViewPaddingLeft = ScreenUtils.dip2px(getContext(), 12);

    private final GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 5);
    private final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

    private final int recycleViewHeightGrid = ScreenUtils.dip2px(getContext(), 126);
    private final int recycleViewHeightLinear = ScreenUtils.dip2px(getContext(), 100);


    public TEDetailPanel(@NonNull Context context) {
        this(context, null);
    }

    public TEDetailPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TEDetailPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TE_DetailPanel);
            int type = typedArray.getInt(R.styleable.TE_DetailPanel_layout_type, 1);
            if (type == 0) {
                this.layoutType = LayoutType.GRID;
            } else if (type == 1) {
                this.layoutType = LayoutType.LINEAR;
            }
            typedArray.recycle();
        }
        this.downloadHelper = new TEDownloadHelper(context);
        initViews(context);
    }


    @SuppressLint("ClickableViewAccessibility")
    private void initViews(Context context) {
        setClickable(true);
        LayoutInflater.from(context).inflate(R.layout.te_beauty_panel_view_layout, this, true);
        indicatorSeekBar = findViewById(R.id.te_panel_view_seekBar);
        indicatorSeekBar.setVisibility(GONE);
        TEUIConfig uiConfig = TEUIConfig.getInstance();
        this.indicatorSeekBar.setThumbDrawable(CustomDrawableUtils.createSeekBarThumbDrawable(getContext(), uiConfig.seekBarProgressColor));
        this.indicatorSeekBar.setProgressTrackColor(uiConfig.seekBarProgressColor);
        this.indicatorSeekBar.setIndicatorColor(uiConfig.seekBarProgressColor);
        switchLayout = findViewById(R.id.te_panel_view_switch);
        compareBtn = findViewById(R.id.te_panel_view_compare_btn);

        mainTabLayout = findViewById(R.id.te_panel_view_main_title_tab_layout);
        mainTabLayout.setTabMode(uiConfig.tabMode);
        mainTabLayout.updateItemTextColor(uiConfig.textCheckedColor, uiConfig.textColor);
        mainTabLayout.setTabSelectedListener((property, selectedTabIndex) -> {
            if (selectedTabIndex != -1) {
                panelDataProvider.onTabSelected(selectedTabIndex);
            }
            if (property.hasSubTitle) {
                subTabLayout.setVisibility(VISIBLE);
                subTabLayout.setData(property.propertyList, 13);
            } else {
                subTabLayout.setVisibility(GONE);
                subTabLayout.setData(null, 0);
                onSelectedInternal(property, selectedTabIndex);
            }
        });
        subTabLayout = findViewById(R.id.te_panel_view_sub_title_tab_layout);
        subTabLayout.setTabMode(uiConfig.tabMode);
        subTabLayout.updateItemTextColor(uiConfig.textCheckedColor, uiConfig.textColor);
        subTabLayout.setTabSelectedListener(this::onSelectedInternal);


        mainTitleLayout = findViewById(R.id.te_panel_view_main_tab_layout);
        rightRevertLayout = findViewById(R.id.te_panel_view_top_right_layout);
        rightRevertLayout.setOnClickListener(this);
        rightRevertText = findViewById(R.id.te_panel_view_top_right_txt);
        rightRevertText.setTextColor(uiConfig.textColor);
        titleDivider = findViewById(R.id.te_panel_view_title_divider);
        titleDivider.setBackgroundColor(uiConfig.panelDividerColor);
        topRightDivider = findViewById(R.id.te_panel_view_top_right_layout_divider);
        topRightDivider.setBackgroundColor(uiConfig.panelDividerColor);


        backBtn = findViewById(R.id.te_panel_view_back_btn);
        titleText = findViewById(R.id.te_panel_view_title_text);

        recyclerView = findViewById(R.id.te_panel_view_recycle_view);
        panelBgView = findViewById(R.id.te_panel_view_bg_view);
        panelBgView.setBackgroundColor(uiConfig.panelBackgroundColor);
        if (this.layoutType == LayoutType.GRID) {
            gridLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(gridLayoutManager);
            recycleViewAdapter = new TEDetailPanelAdapter(gridLayoutManager, this);
        } else {
            linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            recyclerView.setPaddingRelative(recycleViewPaddingLeft, 0, 0, 0);
            recyclerView.setLayoutManager(linearLayoutManager);
            recycleViewAdapter = new TEDetailPanelAdapter(linearLayoutManager, this);
        }
        recyclerView.setAdapter(recycleViewAdapter);
        indicatorSeekBar.setOnSeekChangeListener(new OnSeekChangeListenerImp() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                onSeekBarSeeking(seekParams);
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                super.onStopTrackingTouch(seekBar);
                onSeekBarStopTrackingTouch();
            }
        });
        compareBtn.setOnTouchListener(new CompareBtnTouchListener());
        backBtn.setOnClickListener(this);
        setLayoutDirect(uiConfig);
    }


    /**
     * 根据 UI 配置设置面板的布局方向与文字方向。
     * <p>
     * 当 {@link TEUIConfig#isLayoutRtl} 为 {@code true} 时，面板整体切换为从右向左（RTL）展示，
     * 用于适配阿拉伯语等 RTL 语言；否则保持默认的从左向右（LTR）布局。
     * <p>
     * 该方法会依次处理三部分方向：
     * <ul>
     *     <li>布局方向（{@link #setLayoutDirection(int)}）：镜像 start/end 属性并反转水平排列顺序；</li>
     *     <li>文字方向（{@link #setTextDirection(int)}）：注意需使用 {@link View#TEXT_DIRECTION_RTL}，
     *         与布局方向的 {@link View#LAYOUT_DIRECTION_RTL} 不是同一套常量，不能直接复用；</li>
     *     <li>{@code IndicatorSeekBar} 的方向：由于其为纯自定义绘制控件、不读取系统布局方向，
     *         需通过其自身的 {@code setR2L()} 单独同步。</li>
     * </ul>
     *
     * @param teuiConfig UI 配置，用于读取 {@code isLayoutRtl} 开关
     */
    public void setLayoutDirect(TEUIConfig teuiConfig) {
        // 根据配置决定面板布局方向：默认从左向右(LTR)，仅当 isLayoutRtl=true 时从右向左(RTL)
        int direct = teuiConfig.isLayoutRtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;

        // 对整个面板视图树设置布局方向，自动镜像 start/end 属性并反转水平方向
        setLayoutDirection(direct);
        // 同步文字方向：
        // 注意 setTextDirection 使用的是 TEXT_DIRECTION_* 常量体系（TEXT_DIRECTION_RTL = 4），
        // 与 setLayoutDirection 的 LAYOUT_DIRECTION_*（LAYOUT_DIRECTION_RTL = 1）不是同一套，
        // 不能直接复用 direct，否则会被误解析为 TEXT_DIRECTION_FIRST_STRONG，导致文字仍从左向右。
        setTextDirection(teuiConfig.isLayoutRtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);

        // IndicatorSeekBar 是纯自定义绘制 View，不读取系统 layoutDirection，
        // 需通过其自身的 setR2L() 单独同步从右向左的状态
        boolean isRtl = direct == View.LAYOUT_DIRECTION_RTL;
        if (indicatorSeekBar != null) {
            indicatorSeekBar.setR2L(isRtl);
        }
        // 返回按钮图标为 png，不会随布局方向自动镜像，
        // RTL 下「返回」箭头应指向右，需手动做水平镜像
        if (backBtn != null) {
            backBtn.setScaleX(isRtl ? -1f : 1f);
        }
    }



    public void onSelectedInternal(TEUIProperty uiProperty, int selectedTabIndex) {
        onClickTitle(uiProperty);
        this.initRecycleView(uiProperty);
        recycleViewAdapter.setProperties(uiProperty.propertyList);
        setSeekBarState(getCheckedUIProperty(uiProperty.propertyList));
        recycleViewAdapter.scrollToPosition(0);
    }


    private void initRecycleView(TEUIProperty uiProperty) {
        RecyclerView.LayoutManager currentLayoutmanager = recyclerView.getLayoutManager();
        boolean isLinearLayout = !(uiProperty.parentUIProperty != null ? uiProperty.parentUIProperty.verticalLayout : uiProperty.verticalLayout);
        if (currentLayoutmanager == null) {
            if (isLinearLayout) {
                linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                recyclerView.setLayoutManager(linearLayoutManager);
            } else {
                gridLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
                recyclerView.setLayoutManager(gridLayoutManager);
            }
        } else {
            if (currentLayoutmanager instanceof GridLayoutManager && isLinearLayout) {
                linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                recyclerView.setLayoutManager(linearLayoutManager);
            } else if (currentLayoutmanager instanceof LinearLayoutManager && !isLinearLayout) {
                gridLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
                recyclerView.setLayoutManager(gridLayoutManager);
            }
        }
        if (isLinearLayout) {
            recyclerView.getLayoutParams().height = recycleViewHeightLinear;
        } else {
            recyclerView.getLayoutParams().height = recycleViewHeightGrid;
        }
        if (recycleViewAdapter != null) {
            recycleViewAdapter.updateLayoutManager((LinearLayoutManager) recyclerView.getLayoutManager());
        } else {
            recycleViewAdapter = new TEDetailPanelAdapter((LinearLayoutManager) recyclerView.getLayoutManager(), this);
            recyclerView.setAdapter(recycleViewAdapter);
        }
    }


    /**
     * 手动选中item 的时候调用此方法
     *
     * @param uiProperty
     */
    public void checkPanelViewItem(TEUIProperty uiProperty) {
        if (TEUIProperty.TESDKParam.EXTRA_INFO_SEG_TYPE_GREEN[1].equals(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_SEG_TYPE)) && uiProperty.uiCategory == TEUIProperty.UICategory.SEGMENTATION) {
            List<TEUIProperty> uiPropertyList = panelDataProvider.onHandRecycleViewItemClick(uiProperty);
            if (uiPropertyList != null && !uiPropertyList.isEmpty()) {
                this.showChildrenItems(uiProperty, uiPropertyList);
            }
            this.handleRecyclerViewItemClick(ProviderUtils.getImportTEUIPropertyItem(uiProperty));
            return;
        }
        this.handleRecyclerViewItemClick(uiProperty);
    }


    @Override
    public void onRecycleViewItemClick(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return;
        }
        // 轻美妆主态下，用户再次点击已选中的套装：主动触发下钻到单项列表。
        if (uiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP
                && uiProperty.getUiState() == TEUIProperty.UIState.CHECKED_AND_IN_USE
                && uiProperty.sdkParam != null
                && !TextUtils.isEmpty(uiProperty.sdkParam.resourcePath)) {
            List<TEUIProperty> itemList = LightMakeupItemBuilder.buildItemList(uiProperty);
            if (itemList != null && !itemList.isEmpty()) {
                uiProperty.propertyList = itemList;
                showChildrenItems(uiProperty, itemList);
                return;
            }
        }
        if (uiProperty.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE) {  //所以这里判断是不是绿幕2中的导入图片项
            TEUIProperty parentProperty = uiProperty.parentUIProperty;
            parentProperty.sdkParam.extraInfo.put(GREEN_PARAMS_V2, ProviderUtils.getGreenParamsV2(parentProperty));
            uiProperty.sdkParam = parentProperty.sdkParam;
            panelViewListener.onClickCustomSeg(uiProperty);
            return;
        }
        if (uiProperty.sdkParam != null && uiProperty.sdkParam.extraInfo != null &&
                (Arrays.asList(TEUIProperty.TESDKParam.EXTRA_INFO_SEG_TYPE_GREEN).contains(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_SEG_TYPE))
                        || TEUIProperty.TESDKParam.EXTRA_INFO_SEG_TYPE_CUSTOM.equals(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_SEG_TYPE)))
        ) {
            if (TEUIProperty.TESDKParam.EXTRA_INFO_SEG_TYPE_GREEN[1].equals(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_SEG_TYPE))) {  //这个是绿幕2的情况 需要特殊处理，判断是否有背景图，如果有的话直接进入子目录
                // 将子项的值填写进去
                uiProperty.sdkParam.extraInfo.put(GREEN_PARAMS_V2, ProviderUtils.getGreenParamsV2(uiProperty));
                if (!TextUtils.isEmpty(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_BG_PATH))) {
                    this.handleRecyclerViewItemClick(uiProperty);
                    return;
                }
            }
            if (panelViewListener != null) {
                panelViewListener.onClickCustomSeg(uiProperty);
            }
        } else {
            this.handleRecyclerViewItemClick(uiProperty);
        }
    }


    /**
     * 处理RecyclerView项的点击事件
     * 根据数据提供者的返回结果决定是显示子项列表还是执行具体的点击逻辑
     *
     * @param uiProperty 被点击的UI属性对象
     *                   逻辑流程：
     *                   1. 检查UI属性是否为null
     *                   2. 调用数据提供者处理点击事件，获取子项列表
     *                   3. 如果存在子项列表，则显示子项界面
     *                   4. 如果没有子项列表，则执行具体的点击处理逻辑
     */
    private void handleRecyclerViewItemClick(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return;
        }
        // 轻美妆下钻态单项（口红/腮红/眼影/滤镜等）：只刷新子列表选中态和滑竿，
        // 不下发 effect（真正下发由滑竿拖动时触发），也不下钻。
        if (uiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP_ITEM) {
            panelDataProvider.onHandRecycleViewItemClick(uiProperty);
            if (recycleViewAdapter != null) {
                recycleViewAdapter.notifyDataSetChanged();
            }
            setSeekBarState(uiProperty);
            return;
        }
        List<TEUIProperty> uiPropertyList = panelDataProvider.onHandRecycleViewItemClick(uiProperty);

        if (uiPropertyList != null) {
            this.showChildrenItems(uiProperty, uiPropertyList);
        } else {
            handleUIPropertyClickLogic(uiProperty);
        }
    }

    /**
     * 处理UI属性点击事件的逻辑
     * 包括下载检查、不同UI分类的处理（美颜模板、无效果项、普通项等）
     *
     * @param uiProperty 被点击的UI属性对象
     */
    private void handleUIPropertyClickLogic(TEUIProperty uiProperty) {
        if (isNeedDownload(uiProperty)) {
            this.downloadHelper.startDownload(uiProperty, new TEDownloadHelper.DefaultTEDownloadListenerImp() {
                @Override
                public void onDownloadSuccess(TEDownloadResult result) {
                    recycleViewAdapter.notifyDataSetChanged();
                    setSeekBarState(uiProperty);
                    if (panelViewListener != null) {
                        panelViewListener.onUpdateEffect(uiProperty.sdkParam);
                    }
                }
            });
        } else {
            if (panelViewListener == null) {
                return;
            }
            if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE) {     //美颜模板需要特殊处理
                panelViewListener.onUpdateEffect(uiProperty.sdkParam);
                ((TEGeneralDataProvider) this.panelDataProvider).restoreUIStateFromParams(uiProperty.paramList, "点击的时候同步");
                //同步美颜数据
            } else if (uiProperty.isNoneItem()) {
                List<TEUIProperty.TESDKParam> closeList = panelDataProvider.getCloseEffectItems(uiProperty);
                if (closeList != null && !closeList.isEmpty()) {
                    panelViewListener.onUpdateEffectList(closeList);
                }
            } else {
                panelViewListener.onUpdateEffect(this.getSDKParamFromTEUIProperty(uiProperty));
            }
            recycleViewAdapter.notifyDataSetChanged();
            setSeekBarState(uiProperty);
        }
    }


    private void showChildrenItems(TEUIProperty currentItem, List<TEUIProperty> uiPropertyList) {
        itemPositions.add(recycleViewAdapter.findFirstVisibleItemPosition());
        setSeekBarState(null);
        backBtn.setVisibility(VISIBLE);
        titleText.setText(PanelDisplay.getDisplayName(currentItem));
        titleText.setVisibility(VISIBLE);
        titleText.setTag(currentItem);
        mainTitleLayout.setVisibility(INVISIBLE);
        if (subTabLayout != null && subTabLayout.getVisibility() == VISIBLE) {
            subTabLayout.setVisibility(GONE);
        }
        recycleViewAdapter.setProperties(uiPropertyList);
        recycleViewAdapter.scrollToPosition(0);
    }


    private void setSeekBarState(TEUIProperty uiProperty) {
        if (uiProperty == null || uiProperty.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE) {
            hideSeekBar();
            return;
        }
        if (uiProperty.sdkParam != null && uiProperty.propertyList == null
                && uiProperty.uiCategory != TEUIProperty.UICategory.MOTION
                && uiProperty.uiCategory != TEUIProperty.UICategory.SEGMENTATION
                && !uiProperty.isBeautyMakeupNoneItem()
                && uiProperty.uiCategory != TEUIProperty.UICategory.BEAUTY_TEMPLATE
        ) {
            TEUIProperty.EffectValueType type = TEUIProperty.getEffectValueType(uiProperty.sdkParam);
            if ((uiProperty.uiCategory == TEUIProperty.UICategory.MAKEUP || uiProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) && uiProperty.sdkParam.extraInfo != null
                    && uiProperty.sdkParam.extraInfo.get(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_LUT_STRENGTH) != null) {
                switchLayout.setText(getResources().getString(R.string.te_beauty_panel_menu_view_layout_makeup),
                        getResources().getString(R.string.te_beauty_panel_menu_view_layout_lut));
                switchLayout.check(SwitchLayout.SWITCH_LEFT_CHECKED);
                switchLayout.setSwitchLayoutListener(this);
                switchLayout.setVisibility(VISIBLE);
            } else {
                switchLayout.setVisibility(GONE);
            }
            indicatorSeekBar.setMin(type.getMin());
            indicatorSeekBar.setMax(type.getMax());
            if (type.getMax() - type.getMin() <= 10) {
                indicatorSeekBar.setTickCount(type.getMax() - type.getMin() + 1);
            } else {
                indicatorSeekBar.setTickCount(0);
            }
            indicatorSeekBar.setProgress(uiProperty.sdkParam.effectValue);
            indicatorSeekBar.setVisibility(View.VISIBLE);
            indicatorSeekBar.setTag(uiProperty);
        } else {
            this.hideSeekBar();
        }
    }


    private void hideSeekBar() {
        indicatorSeekBar.setVisibility(View.GONE);
        switchLayout.setVisibility(GONE);
        indicatorSeekBar.setTag(null);
    }


    private void onSeekBarSeeking(SeekParams seekParams) {
        LogUtils.i(TAG, "=== onSeekBarSeeking: fromUser=" + seekParams.fromUser
                + ", progress=" + seekParams.progress
                + ", tagIsProperty=" + (indicatorSeekBar.getTag() instanceof TEUIProperty)
                + ", tag=" + indicatorSeekBar.getTag());
        if (seekParams.fromUser && indicatorSeekBar.getTag() instanceof TEUIProperty) {
            TEUIProperty focusProperty = (TEUIProperty) indicatorSeekBar.getTag();
            if (focusProperty == null || focusProperty.sdkParam == null) {
                LogUtils.i(TAG, "=== onSeekBarSeeking: focusProperty null or sdkParam null");
                return;
            }
            // 轻美妆下钻态单项：将滑竿值写入父套装的 extraInfo[effectName]，并整套重发
            if (focusProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP_ITEM) {
                TEUIProperty parent = focusProperty.parentUIProperty;
                if (parent == null || parent.sdkParam == null) {
                    LogUtils.i(TAG, "=== LIGHT_MAKEUP_ITEM: parent null or parent.sdkParam null");
                    return;
                }
                LogUtils.i(TAG, "=== LIGHT_MAKEUP_ITEM drag: effectName=" + focusProperty.sdkParam.effectName
                        + ", progress=" + seekParams.progress
                        + ", parent.effectName=" + parent.sdkParam.effectName
                        + ", parent.resourcePath=" + parent.sdkParam.resourcePath);
                if (parent.sdkParam.extraInfo == null) {
                    parent.sdkParam.extraInfo = new android.util.ArrayMap<>();
                }
                parent.sdkParam.extraInfo.put(focusProperty.sdkParam.effectName, String.valueOf(seekParams.progress));
                // 同步 item 的 effectValue 快照，保证通用 setSeekBarState 读取到最新强度
                focusProperty.sdkParam.effectValue = seekParams.progress;
                if (panelViewListener != null) {
                    panelViewListener.onUpdateEffect(parent.sdkParam);
                } else {
                    LogUtils.i(TAG, "=== LIGHT_MAKEUP_ITEM: panelViewListener is null");
                }
                return;
            }
            if ((focusProperty.uiCategory == TEUIProperty.UICategory.MAKEUP || focusProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) && switchLayout.getVisibility() == VISIBLE) {
                onChangeMakeupItemEffectValue(focusProperty.sdkParam, seekParams.progress);
            } else {
                focusProperty.sdkParam.effectValue = seekParams.progress;
            }
            if (panelViewListener != null) {
                panelViewListener.onUpdateEffect(this.getSDKParamFromTEUIProperty(focusProperty));
            }
        }
    }


    /**
     * 从UI属性对象中获取对应的SDK参数
     * 对于绿幕背景V2项，需要特殊处理：检查父属性的背景路径和关键颜色，并添加GREEN_PARAMS_V2参数
     *
     * @param teuiProperty UI属性对象
     * @return 处理后的SDK参数，如果UI属性为null或绿幕背景V2项的条件不满足则返回null
     */
    private TEUIProperty.TESDKParam getSDKParamFromTEUIProperty(TEUIProperty teuiProperty) {
        if (teuiProperty == null) {
            return null;
        }
        if (teuiProperty.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM) {
            TEUIProperty parentProperty = teuiProperty.parentUIProperty;
            if (TextUtils.isEmpty(parentProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_BG_PATH)) && TextUtils.isEmpty(parentProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_KEY_COLOR))) {
                return null;
            }
            parentProperty.sdkParam.extraInfo.put(GREEN_PARAMS_V2, ProviderUtils.getGreenParamsV2(parentProperty));
            return parentProperty.sdkParam;
        }
        return teuiProperty.sdkParam;
    }


    private void onChangeMakeupItemEffectValue(TEUIProperty.TESDKParam teParam, int progress) {
        int checkedId = switchLayout.getCurrentCheckedItem();
        if (checkedId == SwitchLayout.SWITCH_LEFT_CHECKED) {
            teParam.effectValue = progress;
        } else if (checkedId == SwitchLayout.SWITCH_RIGHT_CHECKED) {
            Map<String, String> extraInfo = teParam.extraInfo;
            if (extraInfo == null) {
                return;
            }
            extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_LUT_STRENGTH, String.valueOf(progress));
        }
    }

    private void onSeekBarStopTrackingTouch() {
        if (indicatorSeekBar.getTag() instanceof TEUIProperty) {
            TEUIProperty focusProperty = (TEUIProperty) indicatorSeekBar.getTag();
            if (focusProperty == null
                    || focusProperty.uiCategory != TEUIProperty.UICategory.BEAUTY
                    || focusProperty.sdkParam == null
            ) {
                return;
            }
            recycleViewAdapter.refreshCurrentItemState();
        }
    }

    private void showView() {
        this.backBtn.setVisibility(GONE);
        this.titleText.setVisibility(GONE);
        this.mainTitleLayout.setVisibility(VISIBLE);
        initMainTitleLayout(this.panelDataProvider.getPanelData(getContext()));
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.te_panel_view_back_btn) {
            onBackClick();
        } else if (id == R.id.te_panel_view_top_right_layout) {
            if (panelViewListener != null) {
                panelViewListener.onTopRightBtnClick();
            }
        }
    }


    public void show(TEPanelDataProvider provider, TEDetailPanelListener panelViewListener) {
        if (provider == null) {
            return;
        }
        if (this.panelDataProvider == provider) {
            return;
        }
        this.clearPositionData();
        this.panelDataProvider = provider;
        this.panelViewListener = panelViewListener;
        this.showView();
        this.compareBtn.setVisibility(this.panelDataProvider.isShowCompareBtn() ? VISIBLE : GONE);
    }


    public void showAndForceRefresh(TEPanelDataProvider provider, TEDetailPanelListener panelViewListener) {
        if (provider == null) {
            return;
        }
        this.clearPositionData();
        this.panelDataProvider = provider;
        this.panelViewListener = panelViewListener;
        this.showView();
        this.compareBtn.setVisibility(this.panelDataProvider.isShowCompareBtn() ? VISIBLE : GONE);
    }


    public void showTopRightLayout(boolean isVisibility) {
        this.rightRevertLayout.setVisibility(isVisibility ? VISIBLE : GONE);
    }


    public void revertEffect() {
        if (panelDataProvider != null) {
            this.clearPositionData();
            setSeekBarState(null);
            if (panelViewListener != null) {
                panelViewListener.onRevertTE(panelDataProvider.getRevertData(getContext()));
            }
            backBtn.setVisibility(GONE);
            titleText.setVisibility(GONE);
            mainTitleLayout.setVisibility(VISIBLE);
            initMainTitleLayout(panelDataProvider.getPanelData(getContext()));
        }
    }

    private void initMainTitleLayout(List<TEUIProperty> propertyList) {
        if (propertyList == null || propertyList.isEmpty()) {
            return;
        }
        mainTabLayout.setData(propertyList, 15);
    }


    private void onBackClick() {
        TEUIProperty titleProperty = (TEUIProperty) titleText.getTag();
        if (titleProperty == null) {
            return;
        }
        // 轻美妆套装是叶子项，propertyList 正常为 null，仅在「已选中后再次点击」时由
        // onRecycleViewItemClick 临时挂载 LightMakeupItemBuilder 构建的单项列表用于下钻。
        // 而 onHandRecycleViewItemClick 统一返回 uiProperty.propertyList 作为下钻判据（非 null 即下钻），
        // 若不在此清理，残留列表会让后续「首次点击未选中套装」也直接下钻，
        // 破坏「首次点击选中、二次点击才下钻」的交互，故返回时需置空。
        // 也就是propertyList是 LIGHT_MAKEUP_ITEM 的时候才会将propertyList设置为null
        if (titleProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP
                && titleProperty.propertyList != null
                && !titleProperty.propertyList.isEmpty()
                && titleProperty.propertyList.get(0).uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP_ITEM) {
            titleProperty.propertyList = null;
        }
        TEUIProperty parentUIProperty = titleProperty.parentUIProperty;
        // 回退一级需要展示 parentUIProperty.propertyList，这里判断该列表由谁承载：
        // 若 parentUIProperty 是一级主 tab 或二级标题，其列表本就由 mainTabLayout / subTabLayout 渲染，
        // 直接恢复主面板根状态即可；否则它是中间下钻节点，继续用 titleText + RecyclerView 承载。
        if (parentUIProperty == null
                || parentUIProperty.isMainTabNode()
                || parentUIProperty.isSubTitleNode()) {
            this.showView();
        } else {
            titleText.setText(PanelDisplay.getDisplayName(parentUIProperty));
            titleText.setTag(parentUIProperty);
            recycleViewAdapter.setProperties(parentUIProperty.propertyList);
        }
        if (!itemPositions.isEmpty()) {
            Integer position = itemPositions.remove(itemPositions.size() - 1);
            LogUtils.d(TAG, "onBackClick postion = " + position);
            recycleViewAdapter.scrollToPosition(position);
        } else {
            recycleViewAdapter.scrollToPosition(0);
        }
    }


    public boolean isCheckedBeautyCloseItem() {
        return recycleViewAdapter.isCheckedBeautyCloseItem();
    }


    private boolean isNeedDownload(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return false;
        }
        if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE && uiProperty.dlModel != null) {
            for (TEUIProperty.TESDKParam tesdkParam : uiProperty.paramList) {
                if (XmagicConstant.EffectName.EFFECT_LUT.equals(tesdkParam.effectName)) {
                    if (!isLocalResourceExists(tesdkParam.resourcePath)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY || uiProperty.uiCategory == TEUIProperty.UICategory.BODY_BEAUTY) {
            return false;
        }
        return uiProperty.sdkParam != null && !TextUtils.isEmpty(uiProperty.resourceUri) && uiProperty.resourceUri.startsWith("http")
                && !isLocalResourceExists(uiProperty.sdkParam.resourcePath);
    }

    /**
     * 判断本地资源是否已存在。
     * <p>
     * {@code resourcePath} 既可能是相对 {@code TEBeautyKit.getResPath()} 的路径（配置解析阶段约定），
     * 也可能是绝对路径（自定义 downloadPath）。这里统一容错处理。
     */
    private boolean isLocalResourceExists(String resourcePath) {
        if (TextUtils.isEmpty(resourcePath)) {
            return false;
        }
        String resPath = TEBeautyKit.getResPath();
        if (TextUtils.isEmpty(resPath)) {
            return false;
        }
        if (resourcePath.startsWith(File.separator)) {
            return new File(resourcePath).exists();
        }
        return new File(resPath + resourcePath).exists();
    }


    private TEUIProperty getCheckedUIProperty(List<TEUIProperty> uiPropertyList) {
        if (uiPropertyList == null || uiPropertyList.isEmpty()) {
            return null;
        }
        for (TEUIProperty teuiProperty : uiPropertyList) {
            if (teuiProperty.getUiState() == TEUIProperty.UIState.CHECKED_AND_IN_USE) {
                return teuiProperty;
            }
        }
        return null;
    }


    @Override
    public void onSwitchChange(int checkedId) {
        if (indicatorSeekBar.getTag() instanceof TEUIProperty) {
            TEUIProperty uiProperty = (TEUIProperty) indicatorSeekBar.getTag();
            if (uiProperty.uiCategory != TEUIProperty.UICategory.MAKEUP && uiProperty.uiCategory != TEUIProperty.UICategory.LIGHT_MAKEUP) {
                return;
            }
            TEUIProperty.EffectValueType type = checkedId == SwitchLayout.SWITCH_LEFT_CHECKED ? TEUIProperty.getEffectValueType(uiProperty.sdkParam) : TEUIProperty.EffectValueType.RANGE_0_POS100;
            indicatorSeekBar.setMin(type.getMin());
            indicatorSeekBar.setMax(type.getMax());
            if (checkedId == SwitchLayout.SWITCH_LEFT_CHECKED) {
                indicatorSeekBar.setProgress(uiProperty.sdkParam.effectValue);
            } else if (checkedId == SwitchLayout.SWITCH_RIGHT_CHECKED) {
                Map<String, String> extraInfo = uiProperty.sdkParam.extraInfo;
                if (extraInfo == null) {
                    return;
                }
                String makeupLutStrength = extraInfo.get(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_LUT_STRENGTH);
                if (TextUtils.isEmpty(makeupLutStrength)) {
                    return;
                }
                try {
                    indicatorSeekBar.setProgress(Integer.parseInt(makeupLutStrength));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public enum LayoutType {
        GRID, LINEAR
    }


    public void updatePanelUIConfig(TEUIConfig uiConfig) {
        if (this.indicatorSeekBar != null) {
            this.indicatorSeekBar.setThumbDrawable(CustomDrawableUtils.createSeekBarThumbDrawable(getContext(), uiConfig.seekBarProgressColor));
            this.indicatorSeekBar.setProgressTrackColor(uiConfig.seekBarProgressColor);
            this.indicatorSeekBar.setIndicatorColor(uiConfig.seekBarProgressColor);
        }

        mainTabLayout.updateItemTextColor(uiConfig.textCheckedColor, uiConfig.textColor);
        subTabLayout.updateItemTextColor(uiConfig.textCheckedColor, uiConfig.textColor);

        if (this.rightRevertText != null) {
            this.rightRevertText.setTextColor(uiConfig.textColor);
        }
        if (this.titleDivider != null) {
            this.titleDivider.setBackgroundColor(uiConfig.panelDividerColor);
        }
        if (this.topRightDivider != null) {
            this.topRightDivider.setBackgroundColor(uiConfig.panelDividerColor);
        }
        if (this.panelBgView != null) {
            this.panelBgView.setBackgroundColor(uiConfig.panelBackgroundColor);
        }
        if (this.recycleViewAdapter != null) {
            this.recycleViewAdapter.updateUIConfig(uiConfig);
        }

        setLayoutDirect(uiConfig);
    }


    private void clearPositionData() {
        this.itemPositions.clear();
    }


    private class CompareBtnTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean isDown = false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDown = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDown = false;
                    break;
                default:
                    return true;
            }
            if (panelViewListener != null) {
                panelViewListener.onCloseEffect(isDown);
            }
            return true;
        }
    }


    private void onClickTitle(TEUIProperty teuiProperty) {
        if (panelViewListener != null) {
            panelViewListener.onTitleClick(teuiProperty);
        }
    }


    private static class OnSeekChangeListenerImp implements OnSeekChangeListener {

        @Override
        public void onSeeking(SeekParams seekParams) {

        }

        @Override
        public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(IndicatorSeekBar seekBar) {

        }
    }


    public interface TEDetailPanelListener {


        void onTopRightBtnClick();


        void onCloseEffect(boolean isClose);


        void onRevertTE(List<TEUIProperty.TESDKParam> properties);


        void onUpdateEffect(TEUIProperty.TESDKParam param);


        void onUpdateEffectList(List<TEUIProperty.TESDKParam> paramList);


        void onClickCustomSeg(TEUIProperty uiProperty);


        void onTitleClick(TEUIProperty uiProperty);
    }


}