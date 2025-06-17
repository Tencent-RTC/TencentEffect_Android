package com.tencent.effect.beautykit.view.panelview;

import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH;
import static com.tencent.effect.beautykit.model.TEUIProperty.TESDKParam.EXTRA_INFO_KEY_SEG_TYPE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.download.TEDownloadListener;
import com.tencent.effect.beautykit.manager.TEDownloadManager;
import com.tencent.effect.beautykit.model.TEMotionDLModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.provider.TEPanelDataProvider;
import com.tencent.effect.beautykit.utils.CustomDrawableUtils;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.PanelDisplay;
import com.tencent.effect.beautykit.utils.ScreenUtils;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;
import com.tencent.effect.beautykit.view.dialog.TEProgressDialog;
import com.tencent.effect.beautykit.view.widget.SwitchLayout;
import com.tencent.effect.beautykit.view.widget.indicatorseekbar.IndicatorSeekBar;
import com.tencent.effect.beautykit.view.widget.indicatorseekbar.OnSeekChangeListener;
import com.tencent.effect.beautykit.view.widget.indicatorseekbar.SeekParams;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;




public class TEDetailPanel extends FrameLayout implements View.OnClickListener,
        TEDetailPanelAdapter.ItemClickListener, RadioGroup.OnCheckedChangeListener, SwitchLayout.SwitchLayoutListener {

    public static final int TE_PANEL_VIEW_FOLDED_TYPE = 0;
    public static final int TE_PANEL_VIEW_EXPAND_TYPE = 1;


    private static final String TAG = TEDetailPanel.class.getName();

    private ConstraintLayout foldedLayout;
    private LinearLayout expandLayout;

    private LinearLayout expandViewCustomViewLayout;
    private ConstraintLayout expandViewCustomViewLayoutSeekbarLayout;
    private IndicatorSeekBar indicatorSeekBar;
    private SwitchLayout switchLayout;
    private ImageView expandViewCompareBtn;
    private RadioGroup expandViewRadioGroup;
    private HorizontalScrollView expandViewHorizontalScrollView;

    private View expandViewTitleDivider;
    private View expandViewTopRightDivider;
    private LinearLayout expandViewRadioGroupLayout;

    private LinearLayout expandViewTopRightRevertLayout;
    private TextView expandViewTopRightRevertText;
    private ConstraintLayout expandViewBottomLayout;
    private ImageView expandViewBackBtn;
    private TextView expandViewTitle;


    private TEPanelDataProvider panelDataProvider;
    private TEDetailPanelAdapter recycleViewAdapter;
    private TEDetailPanelListener panelViewListener;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TEProgressDialog progressDialog;

    private LinearLayout expandViewLeftBottomBtn;
    private ImageView expandViewLeftBottomBtnImg;
    private TextView expandViewLeftBottomBtnText;
    private LinearLayout foldedViewLeftBottomBtn;
    private ImageView foldedViewLeftBottomImg;
    private TextView foldedViewLeftBottomText;

    private RecyclerView recyclerView;
    private View panelBgView;
    private LinearLayout expandViewRightBottomBtn;
    private ImageView expandViewRightBottomImg;
    private TextView expandViewRightBottomText;
    private LinearLayout foldedViewRightBottomBtn;
    private ImageView foldedViewRightBottomImg;
    private TextView foldedViewRightBottomText;

    // Used to store the previous scroll position of the list. Once this position information is used, it should be deleted.
    private final List<Integer> itemPositions = new ArrayList<>();
    private final Map<String, Integer> typePropertyPositionMap = new ArrayMap<>();
    private TEUIProperty lastCheckedUIProperty = null;

    private LayoutType layoutType = LayoutType.LINEAR;
    private LinearLayout gridLayoutEntryBtn = null;

    private final int recycleViewPaddingRight = ScreenUtils.dip2px(getContext(), 72);
    private final int recycleViewPaddingLeft = ScreenUtils.dip2px(getContext(), 12);

    private int checkItemIndex = 0;
    private final int radioBtnLeftMargin = ScreenUtils.dip2px(getContext(), 20);
    private final int recycleViewHeightLinear = ScreenUtils.dip2px(getContext(), 120);

    private int customViewHeight = ScreenUtils.dip2px(getContext(), 75);
    private final GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 5);
    private final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

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
        initViews(context);
    }




    private void initViews(Context context) {
        setClickable(true);
        foldedLayout = (ConstraintLayout) LayoutInflater.from(context)
                .inflate(R.layout.te_beauty_panel_view_folded_layout, this, false);
        expandLayout = (LinearLayout) LayoutInflater.from(context)
                .inflate(R.layout.te_beauty_panel_view_expand_layout, this, false);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.BOTTOM;
        addView(foldedLayout, layoutParams);
        addView(expandLayout);
        initExpandViews();
        initFoldedViews();
    }

    @SuppressLint({"ClickableViewAccessibility", "ResourceAsColor"})
    private void initExpandViews() {
        expandViewCustomViewLayout = findViewById(R.id.te_panel_expand_view_custom_view_layout);
        expandViewCustomViewLayoutSeekbarLayout = findViewById(R.id.te_panel_expand_view_custom_view_seekbar_layout);
        indicatorSeekBar = findViewById(R.id.te_panel_expand_view_seekBar);
        indicatorSeekBar.setVisibility(GONE);
        TEUIConfig uiConfig = TEUIConfig.getInstance();
        this.indicatorSeekBar.setThumbDrawable(CustomDrawableUtils.createSeekBarThumbDrawable(getContext(), uiConfig.seekBarProgressColor));
        this.indicatorSeekBar.setProgressTrackColor(uiConfig.seekBarProgressColor);
        this.indicatorSeekBar.setIndicatorColor(uiConfig.seekBarProgressColor);
        switchLayout = findViewById(R.id.te_panel_expand_view_switch);
        expandViewCompareBtn = findViewById(R.id.te_panel_expand_view_compare_btn);
        expandViewHorizontalScrollView = findViewById(R.id.te_panel_expand_view_radio_group_scroll_view);
        expandViewRadioGroup = findViewById(R.id.te_panel_expand_view_radio_group);
        expandViewRadioGroup.setOnCheckedChangeListener(this);
        expandViewRadioGroupLayout = findViewById(R.id.te_panel_expand_view_radio_group_layout);
        expandViewTopRightRevertLayout = findViewById(R.id.te_panel_expand_view_top_right_layout);
        expandViewTopRightRevertLayout.setOnClickListener(this);
        expandViewTopRightRevertText = findViewById(R.id.te_panel_expand_view_top_right_txt);
        expandViewTopRightRevertText.setTextColor(uiConfig.textColor);
        expandViewTitleDivider = findViewById(R.id.te_panel_expand_view_title_divider);
        expandViewTitleDivider.setBackgroundColor(uiConfig.panelDividerColor);
        expandViewTopRightDivider = findViewById(R.id.te_panel_expand_view_top_right_layout_divider);
        expandViewTopRightDivider.setBackgroundColor(uiConfig.panelDividerColor);


        expandViewBottomLayout = findViewById(R.id.te_panel_view_bottom_layout);
        expandViewBackBtn = findViewById(R.id.te_panel_expand_view_back_btn);
        expandViewTitle = findViewById(R.id.te_panel_expand_view_title_text);
        gridLayoutEntryBtn = findViewById(R.id.te_panel_view_gridlayout_entry_btn);
        gridLayoutEntryBtn.setOnClickListener(v -> {
            if (panelViewListener != null) {
                panelViewListener.onMoreItemBtnClick();
            }
        });
        recyclerView = findViewById(R.id.te_panel_view_recycle_view);
        panelBgView = findViewById(R.id.te_panel_view_bg_view);
        panelBgView.setBackgroundColor(uiConfig.panelBackgroundColor);
        if (this.layoutType == LayoutType.GRID) {
            gridLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(gridLayoutManager);
            recycleViewAdapter = new TEDetailPanelAdapter(gridLayoutManager, this);
        } else {
            linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            recyclerView.setPadding(recycleViewPaddingLeft, 0, 0, 0);
            recyclerView.setLayoutManager(linearLayoutManager);
            recycleViewAdapter = new TEDetailPanelAdapter(linearLayoutManager, this);
        }
        recyclerView.setAdapter(recycleViewAdapter);
        expandViewLeftBottomBtn = findViewById(R.id.te_panel_expand_view_left_bottom_layout);
        expandViewRightBottomBtn = findViewById(R.id.te_panel_expand_view_right_bottom_layout);
        expandViewLeftBottomBtn.setOnClickListener(this);
        expandViewRightBottomBtn.setOnClickListener(this);
        Button expandViewCameraBtn = findViewById(R.id.te_panel_view_expand_camera_btn);
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
        expandViewCompareBtn.setOnTouchListener(new CompareBtnTouchListener());
        expandViewBackBtn.setOnClickListener(this);
        expandViewCameraBtn.setOnClickListener(this);
        expandViewLeftBottomBtnImg = findViewById(R.id.te_panel_expand_view_left_bottom_img);
        expandViewLeftBottomBtnText = findViewById(R.id.te_panel_expand_view_left_bottom_text);
        expandViewRightBottomImg = findViewById(R.id.te_panel_expand_view_right_bottom_img);
        expandViewRightBottomText = findViewById(R.id.te_panel_expand_view_right_bottom_text);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initFoldedViews() {
        foldedViewLeftBottomBtn = findViewById(R.id.te_panel_folded_view_left_bottom_layout);
        foldedViewLeftBottomBtn.setOnClickListener(this);
        foldedViewRightBottomBtn = findViewById(R.id.te_panel_folded_view_right_bottom_layout);
        foldedViewRightBottomBtn.setOnClickListener(this);
        findViewById(R.id.te_panel_folded_view_camera_btn).setOnClickListener(this);

        foldedViewLeftBottomImg = findViewById(R.id.te_panel_folded_view_left_bottom_img);
        foldedViewLeftBottomText = findViewById(R.id.te_panel_folded_view_left_bottom_text);

        foldedViewRightBottomImg = findViewById(R.id.te_panel_folded_view_right_bottom_img);
        foldedViewRightBottomText = findViewById(R.id.te_panel_folded_view_right_bottom_text);
        foldedLayout.setBackgroundColor(TEUIConfig.getInstance().panelBackgroundColor);
    }


    @SuppressLint({"ResourceType", "RtlHardcoded"})
    private void initRadioGroup(List<TEUIProperty> propertyList) {
        checkItemIndex = 0;
        expandViewRadioGroup.removeAllViews();
        if (propertyList == null || propertyList.isEmpty()) {
            return;
        }
        ((FrameLayout.LayoutParams) expandViewRadioGroup.getLayoutParams()).gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        TEUIProperty checkedItem = null;
        for (int i = 0; i < propertyList.size(); i++) {
            TEUIProperty uiProperty = propertyList.get(i);
            RadioButton btn = new RadioButton(getContext());
            btn.setTag(R.id.te_beauty_panel_view_radio_button_key, uiProperty);
            int uiID = View.generateViewId();
            btn.setId(uiID);
            btn.setButtonDrawable(null);
            btn.setTextSize(16);
            btn.setLines(1);
            btn.setTextColor(CustomDrawableUtils.createRadioGroupColorStateList(TEUIConfig.getInstance().textCheckedColor, TEUIConfig.getInstance().textColor));
            btn.setText(PanelDisplay.getDisplayName(uiProperty));
            RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = radioBtnLeftMargin;

            expandViewRadioGroup.addView(btn, layoutParams);
            boolean isHiddenThisRadioBtn = (this.layoutType == LayoutType.GRID) && TEUIConfig.getInstance().hiddenCategories.contains(uiProperty.uiCategory);
            if (isHiddenThisRadioBtn) {  //如果是网格布局，那么隐藏 美颜、美体、滤镜
                btn.setVisibility(GONE);
            }
            if (uiProperty.getUiState() == TEUIProperty.UIState.CHECKED_AND_IN_USE && !isHiddenThisRadioBtn) {
                checkItemIndex = i;
                checkedItem = uiProperty;
                btn.setChecked(true);
                recycleViewAdapter.setProperties(uiProperty.propertyList);
                setSeekBarState(getCheckedUIProperty(uiProperty.propertyList));
                this.showOrHideEntryBtn(uiProperty);
            }
        }
        if (checkedItem == null) {
            checkItemIndex = 0;
            checkedItem = propertyList.get(0);
            ((RadioButton) expandViewRadioGroup.getChildAt(0)).setChecked(true);
            recycleViewAdapter.setProperties(checkedItem.propertyList);
            setSeekBarState(getCheckedUIProperty(checkedItem.propertyList));
            this.showOrHideEntryBtn(checkedItem);
            expandViewHorizontalScrollView.scrollTo(0, 0);
        }
        recycleViewAdapter.scrollToPosition(0);
        expandViewRadioGroup.post(() -> {
            RadioButton radioButton = (RadioButton) expandViewRadioGroup.getChildAt(checkItemIndex);
            scrollToVisible(radioButton, expandViewHorizontalScrollView);
        });
    }


    private void scrollToVisible(RadioButton radioButton, HorizontalScrollView scrollView) {
        int left = radioButton.getLeft() + radioButton.getWidth();
        int width = scrollView.getWidth();
        scrollView.scrollTo(left - width, 0);
    }


    public void checkPanelViewItem(TEUIProperty uiProperty) {
        if (TEUIProperty.TESDKParam.EXTRA_INFO_SEG_TYPE_GREEN[1].equals(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_SEG_TYPE))) {
            this.checkGSV2Item(uiProperty);
            return;
        }
        if (panelDataProvider != null) {
            panelDataProvider.selectPropertyItem(uiProperty);
        }
        if (recycleViewAdapter != null) {
            recycleViewAdapter.notifyDataSetChanged();
        }
        setSeekBarState(uiProperty);
    }

    private void checkGSV2Item(TEUIProperty uiProperty){
        this.onClickItem(uiProperty);
        TEUIProperty teuiProperty = ProviderUtils.getImportTEUIPropertyItem(uiProperty);
        if (panelDataProvider != null) {
            panelDataProvider.selectPropertyItem(teuiProperty);
        }
        if (recycleViewAdapter != null) {
            recycleViewAdapter.notifyDataSetChanged();
        }
        setSeekBarState(teuiProperty);
    }



    public void setCustomView(View view, ViewGroup.LayoutParams layoutParams) {
        this.expandViewCustomViewLayout.removeAllViews();
        if (view == null) {
            // 75 = 滑竿区域高度35 + 标题区域高度 40
            customViewHeight = ScreenUtils.dip2px(getContext(), 75);
            this.expandViewCustomViewLayout.addView(this.expandViewCustomViewLayoutSeekbarLayout);
        } else {
            // customViewHeight = view height + 标题区域高度 40
            customViewHeight = layoutParams.height + ScreenUtils.dip2px(getContext(),40);
            this.expandViewCustomViewLayout.addView(view, layoutParams);
        }
    }


    @Override
    public void onItemClick(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return;
        }
        if (uiProperty.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE) {  //所以这里判断是不是绿幕2中的导入图片项
            TEUIProperty parentProperty = uiProperty.parentUIProperty;
            parentProperty.sdkParam.extraInfo.put("green_params_v2", ProviderUtils.getGreenParamsV2(parentProperty));
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
                uiProperty.sdkParam.extraInfo.put("green_params_v2", ProviderUtils.getGreenParamsV2(uiProperty));
                if (!TextUtils.isEmpty(uiProperty.sdkParam.extraInfo.get(EXTRA_INFO_KEY_BG_PATH))) {
                    this.onClickItem(uiProperty);
                    return;
                }
            }
            if (panelViewListener != null) {
                panelViewListener.onClickCustomSeg(uiProperty);
            }
        } else {
            this.onClickItem(uiProperty);
        }
    }


    private void onClickItem(TEUIProperty uiProperty) {
        List<TEUIProperty> uiPropertyList = panelDataProvider.onItemClick(uiProperty);
        if (uiPropertyList == null) {
            if (isNeedDownload(uiProperty)) {
                startDownload(uiProperty, new DefaultTEDownloadListenerImp() {
                    @Override
                    public void onDownloadSuccess(String directory) {
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
                if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE) {
                    panelViewListener.onUpdateEffectList(panelDataProvider.getBeautyTemplateData(uiProperty));
                } else if (uiProperty.isNoneItem()) {
                    List<TEUIProperty.TESDKParam> closeList = panelDataProvider.getCloseEffectItems(uiProperty);
                    if (closeList != null && !closeList.isEmpty()) {
                        panelViewListener.onUpdateEffectList(closeList);
                    }
                } else {
                    panelViewListener.onUpdateEffect(this.getSDKParam(uiProperty));
                }
                recycleViewAdapter.notifyDataSetChanged();
                setSeekBarState(uiProperty);
            }
        } else {
            this.showChildrenItem(uiProperty, uiPropertyList);
        }
    }



    private void showChildrenItem(TEUIProperty currentItem, List<TEUIProperty> uiPropertyList) {
        itemPositions.add(recycleViewAdapter.findFirstVisibleItemPosition());
        setSeekBarState(null);
        expandViewBackBtn.setVisibility(VISIBLE);
        expandViewTitle.setText(PanelDisplay.getDisplayName(currentItem));
        expandViewTitle.setVisibility(VISIBLE);
        expandViewTitle.setTag(currentItem);
        expandViewRadioGroupLayout.setVisibility(INVISIBLE);
        recycleViewAdapter.setProperties(uiPropertyList);
        recycleViewAdapter.scrollToPosition(0);
    }


    private void setSeekBarState(TEUIProperty uiProperty) {
        if (uiProperty == null || uiProperty.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE) {
            hideSeekBar();
            return;
        }
        if (uiProperty != null && uiProperty.sdkParam != null && uiProperty.propertyList == null
                && uiProperty.uiCategory != TEUIProperty.UICategory.MOTION
                && uiProperty.uiCategory != TEUIProperty.UICategory.SEGMENTATION
                && !uiProperty.isBeautyMakeupNoneItem()
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
            }else {
                indicatorSeekBar.setTickCount(0);
            }
            indicatorSeekBar.setProgress(uiProperty.sdkParam.effectValue);
            indicatorSeekBar.setVisibility(View.VISIBLE);
            indicatorSeekBar.setTag(uiProperty);
        } else {
           this.hideSeekBar();
        }
    }


    private void hideSeekBar(){
        indicatorSeekBar.setVisibility(View.GONE);
        switchLayout.setVisibility(GONE);
        indicatorSeekBar.setTag(null);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (lastCheckedUIProperty != null) {
            typePropertyPositionMap.put(getPositionKey(lastCheckedUIProperty), recycleViewAdapter.findFirstVisibleItemPosition());
        }
        RadioButton radioButton = group.findViewById(checkedId);
        if (radioButton == null) {
            return;
        }
        int count = group.getChildCount();
        int index = -1;
        for (int i = 0; i < count; i++) {
            RadioButton button = (RadioButton) group.getChildAt(i);
            button.setTypeface(null, button.isChecked() ? Typeface.BOLD : Typeface.NORMAL);
            if (radioButton == button) {
                index = i;
            }
        }
        if (index != -1) {
            panelDataProvider.onTabItemClick(index);
        }
        TEUIProperty uiProperty = (TEUIProperty) radioButton.getTag(R.id.te_beauty_panel_view_radio_button_key);
        this.showOrHideEntryBtn(uiProperty);
        this.onClickTitle(uiProperty);
        recycleViewAdapter.setProperties(uiProperty.propertyList);
        setSeekBarState(getCheckedUIProperty(uiProperty.propertyList));
        this.lastCheckedUIProperty = uiProperty;
        Integer position = typePropertyPositionMap.get(getPositionKey(uiProperty));
        if (position != null) {
            recycleViewAdapter.scrollToPosition(position);
        } else {
            recycleViewAdapter.scrollToPosition(0);
        }
    }


    private String getPositionKey(TEUIProperty teuiProperty) {
        if (teuiProperty != null) {
            return teuiProperty.displayName + teuiProperty.displayNameEn;
        }
        return null;
    }


    private void onSeekBarSeeking(SeekParams seekParams) {
        if (seekParams.fromUser && indicatorSeekBar.getTag() instanceof TEUIProperty) {
            TEUIProperty focusProperty = (TEUIProperty) indicatorSeekBar.getTag();
            if (focusProperty == null || focusProperty.sdkParam == null) {
                return;
            }
            if ((focusProperty.uiCategory == TEUIProperty.UICategory.MAKEUP || focusProperty.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP) && switchLayout.getVisibility() == VISIBLE) {
                onChangeMakeupItem(focusProperty.sdkParam, seekParams.progress);
            } else {
                focusProperty.sdkParam.effectValue = seekParams.progress;
            }
            if (panelViewListener != null) {
                panelViewListener.onUpdateEffect(this.getSDKParam(focusProperty));
            }
        }
    }


    /**
     * 从TEUIProperty 中获取 TESDKParam，主要用于处理 绿幕2 子项目的情况
     * @param teuiProperty
     * @return
     */
    private TEUIProperty.TESDKParam getSDKParam(TEUIProperty teuiProperty) {
        if (teuiProperty == null) {
            return null;
        }
        if (teuiProperty.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM) {
            TEUIProperty parentProperty = teuiProperty.parentUIProperty;
            if (TextUtils.isEmpty(parentProperty.sdkParam.extraInfo.get("bgPath")) && TextUtils.isEmpty(parentProperty.sdkParam.extraInfo.get("keyColor"))) {
                return null;
            }
            parentProperty.sdkParam.extraInfo.put("green_params_v2", ProviderUtils.getGreenParamsV2(parentProperty));
            return parentProperty.sdkParam;
        }
        return teuiProperty.sdkParam;
    }



    private void onChangeMakeupItem(TEUIProperty.TESDKParam teParam, int progress) {
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
        this.expandLayout.setVisibility(VISIBLE);
        this.foldedLayout.setVisibility(GONE);

        this.expandViewBackBtn.setVisibility(GONE);
        this.expandViewTitle.setVisibility(GONE);
        this.expandViewRadioGroupLayout.setVisibility(VISIBLE);
        initRadioGroup(this.panelDataProvider.getPanelData(getContext()));
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.te_panel_folded_view_camera_btn || id == R.id.te_panel_view_expand_camera_btn) {
            onCameraClick();
        } else if (id == R.id.te_panel_folded_view_left_bottom_layout) {
            if (panelViewListener != null) {
                panelViewListener.onLeftBottomBtnClick(TE_PANEL_VIEW_FOLDED_TYPE);
            }
        } else if (id == R.id.te_panel_expand_view_left_bottom_layout) {
            if (panelViewListener != null) {
                panelViewListener.onLeftBottomBtnClick(TE_PANEL_VIEW_EXPAND_TYPE);
            }
        } else if (id == R.id.te_panel_expand_view_back_btn) {
            onBackClick();
        } else if (id == R.id.te_panel_expand_view_right_bottom_layout) {
            if (panelViewListener != null) {
                panelViewListener.onRightBottomBtnClick(TE_PANEL_VIEW_EXPAND_TYPE);
            }
        } else if (id == R.id.te_panel_folded_view_right_bottom_layout) {
            if (panelViewListener != null) {
                panelViewListener.onRightBottomBtnClick(TE_PANEL_VIEW_FOLDED_TYPE);
            }
        } else if (id == R.id.te_panel_expand_view_top_right_layout) {
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
        this.expandViewCompareBtn.setVisibility(this.panelDataProvider.isShowCompareBtn() ? VISIBLE : GONE);
    }


    public void showAndForceRefresh(TEPanelDataProvider provider, TEDetailPanelListener panelViewListener) {
        if (provider == null) {
            return;
        }
        this.clearPositionData();
        this.panelDataProvider = provider;
        this.panelViewListener = panelViewListener;
        this.showView();
        this.expandViewCompareBtn.setVisibility(this.panelDataProvider.isShowCompareBtn() ? VISIBLE : GONE);
    }


    public void showBottomBtn(boolean isShowLeftBottom, boolean isShowRightBottom, int type) {
        if (type == TE_PANEL_VIEW_FOLDED_TYPE) {
            this.foldedViewLeftBottomBtn.setVisibility(isShowLeftBottom ? VISIBLE : GONE);
            this.foldedViewRightBottomBtn.setVisibility(isShowRightBottom ? VISIBLE : GONE);
        } else if (type == TE_PANEL_VIEW_EXPAND_TYPE) {
            this.expandViewRightBottomBtn.setVisibility(isShowRightBottom ? VISIBLE : GONE);
            this.expandViewLeftBottomBtn.setVisibility(isShowLeftBottom ? VISIBLE : GONE);
        }
    }


    public void showTopRightLayout(boolean isVisibility) {
        this.expandViewTopRightRevertLayout.setVisibility(isVisibility ? VISIBLE : GONE);
    }


    public void showBottomLayout(boolean isVisibility) {
        this.expandViewBottomLayout.setVisibility(isVisibility ? VISIBLE : GONE);
    }

    public boolean isVisibilityBottomLayout() {
        return this.expandViewBottomLayout.getVisibility() == VISIBLE;
    }


    public void showExpandLayout() {
        expandLayout.setVisibility(VISIBLE);
        foldedLayout.setVisibility(GONE);
    }



    public void showFoldLayout() {
        foldedLayout.setVisibility(VISIBLE);
        expandLayout.setVisibility(GONE);
    }


    @SuppressLint("ResourceType")
    public void setLeftBottomBtnInfo(@IntegerRes int icon, @IntegerRes int name, int type) {
        if (icon != 0) {
            ImageView imageView = type == TE_PANEL_VIEW_FOLDED_TYPE ? foldedViewLeftBottomImg : expandViewLeftBottomBtnImg;
            imageView.setImageResource(icon);
        }
        if (name != 0) {
            TextView textView = type == TE_PANEL_VIEW_FOLDED_TYPE ? foldedViewLeftBottomText : expandViewLeftBottomBtnText;
            textView.setText(name);
        }
    }


    @SuppressLint("ResourceType")
    public void setRightBottomBtnInfo(@IntegerRes int icon, @IntegerRes int name, int type) {
        if (icon != 0) {
            ImageView imageView = type == TE_PANEL_VIEW_FOLDED_TYPE ? foldedViewRightBottomImg : expandViewRightBottomImg;
            imageView.setImageResource(icon);
        }
        if (name != 0) {
            TextView textView = type == TE_PANEL_VIEW_FOLDED_TYPE ? foldedViewRightBottomText : expandViewRightBottomText;
            textView.setText(name);
        }
    }

    private void onCameraClick() {
        if (this.panelViewListener != null) {
            this.panelViewListener.onCameraClick();
        }
    }



    public void revertEffect() {
        if (panelDataProvider != null) {
            this.clearPositionData();
            setSeekBarState(null);
            if (panelViewListener != null) {
                panelViewListener.onRevertTE(panelDataProvider.getRevertData(getContext()));
            }
            expandViewBackBtn.setVisibility(GONE);
            expandViewTitle.setVisibility(GONE);
            expandViewRadioGroupLayout.setVisibility(VISIBLE);
            initRadioGroup(panelDataProvider.getPanelData(getContext()));
        }
    }


    private void onBackClick() {
        TEUIProperty titleProperty = (TEUIProperty) expandViewTitle.getTag();
        if (titleProperty == null) {
            return;
        }
        TEUIProperty parentUIProperty = titleProperty.parentUIProperty;
        if (parentUIProperty == null || parentUIProperty.parentUIProperty == null) {
            this.showView();
        } else {
            expandViewTitle.setText(PanelDisplay.getDisplayName(parentUIProperty));
            expandViewTitle.setTag(parentUIProperty);
            recycleViewAdapter.setProperties(parentUIProperty.propertyList);
        }
        if (itemPositions.size() > 0) {
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
        if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY
                || uiProperty.uiCategory == TEUIProperty.UICategory.BODY_BEAUTY) {
            return false;
        }
        return uiProperty.sdkParam != null && !TextUtils.isEmpty(uiProperty.resourceUri) && uiProperty.resourceUri.startsWith("http")
                && !new File(uiProperty.sdkParam.resourcePath).exists();
    }


    private void startDownload(TEUIProperty uiProperty, TEDownloadListener downloadListener) {
        TEMotionDLModel dlModel = uiProperty.dlModel;
        if (dlModel == null) {
            LogUtils.e(TAG, "please check this item : " + uiProperty.toString());
            Toast.makeText(getContext(), "please check if the local resource file exists", Toast.LENGTH_LONG).show();
            return;
        }
        if (progressDialog == null) {
            progressDialog = TEProgressDialog.createDialog(getContext());
        }
        progressDialog.show();
        String downloadTip = getResources().getString(R.string.te_beauty_panel_view_download_dialog_tip);
        TEDownloadListener TEDownloadListener = new TEDownloadListener() {
            private final String TAG = "startDownloadResource " + dlModel.getFileName();

            @Override
            public void onDownloadSuccess(String directory) {
                LogUtils.d(TAG, "onDownloadSuccess  " + directory + "   " + Thread.currentThread().getName());
                handler.post(() -> {
                    dismissDialog();
                    if (downloadListener != null) {
                        downloadListener.onDownloadSuccess(directory);
                    }
                });
            }

            @Override
            public void onDownloading(int progress) {
                LogUtils.d(TAG, "onDownloading  " + progress + "   " + Thread.currentThread().getName());
                StringBuilder builder = new StringBuilder();
                builder.append(downloadTip).append(progress).append("%");
                handler.post(() -> {
                    if (progressDialog != null) {
                        progressDialog.setMsg(builder.toString());
                    }
                    if (downloadListener != null) {
                        downloadListener.onDownloading(progress);
                    }
                });
            }

            @Override
            public void onDownloadFailed(int errorCode) {
                LogUtils.d(TAG, "onDownloadFailed  " + errorCode + "   " + Thread.currentThread().getName());
                handler.post(() -> {
                    dismissDialog();
                    Toast.makeText(getContext(), "Download failed", Toast.LENGTH_LONG).show();
                    if (downloadListener != null) {
                        downloadListener.onDownloadFailed(errorCode);
                    }
                });
            }
        };
        TEDownloadManager.getInstance().download(dlModel, dlModel.getUrl().endsWith(ProviderUtils.ZIP_NAME), TEDownloadListener);

    }


    private void dismissDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }



    private TEUIProperty getCheckedUIProperty(List<TEUIProperty> uiPropertyList) {
        if (uiPropertyList == null || uiPropertyList.size() == 0) {
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
                }
            }
        }
    }


    public void switchLayout(LayoutType layoutType) {
        if (this.recyclerView == null) {
            return;
        }
        if (this.layoutType == layoutType) {
            return;
        }
        this.layoutType = layoutType;
        if (layoutType == LayoutType.LINEAR) {
            this.recyclerView.setLayoutManager(this.linearLayoutManager);
        } else if (layoutType == LayoutType.GRID) {
            this.recyclerView.setLayoutManager(this.gridLayoutManager);
        }
        this.clearPositionData();
        this.showOrHideRadioBtn();
    }


    private void showOrHideRadioBtn() {
        int count = this.expandViewRadioGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            RadioButton radioButton = (RadioButton) expandViewRadioGroup.getChildAt(i);
            if (radioButton == null) {
                continue;
            }
            TEUIProperty uiProperty = (TEUIProperty) radioButton.getTag(R.id.te_beauty_panel_view_radio_button_key);
            if (radioButton.isChecked()) {
                this.showOrHideEntryBtn(uiProperty);
            }
            if (this.layoutType == LayoutType.GRID && TEUIConfig.getInstance().hiddenCategories.contains(uiProperty.uiCategory)) {  //如果是网格布局，那么不展示美颜、美体、滤镜
                radioButton.setVisibility(GONE);
            } else {
                radioButton.setVisibility(VISIBLE);
            }
        }
    }


    public enum LayoutType {
        GRID, LINEAR
    }

    private boolean isShowGridLayoutEntry(TEUIProperty teuiProperty) {
        if (this.layoutType == LayoutType.GRID) {
            return false;
        }
        return teuiProperty.isShowGridLayout;
    }

    private void showOrHideEntryBtn(TEUIProperty teuiProperty) {
        if (this.gridLayoutEntryBtn == null) {
            return;
        }
        boolean isShowEntryBtn = isShowGridLayoutEntry(teuiProperty) && this.panelDataProvider.isShowEntryBtn();
        boolean currentState = this.gridLayoutEntryBtn.getVisibility() == VISIBLE;
        if (isShowEntryBtn == currentState) {
            return;
        }
        this.gridLayoutEntryBtn.setVisibility(isShowEntryBtn ? VISIBLE : GONE);
        if (this.recyclerView == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = this.recyclerView.getLayoutParams();
        this.recyclerView.setPadding(recycleViewPaddingLeft, 0, isShowEntryBtn ? recycleViewPaddingRight : 0, 0);
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = (this.layoutType == LayoutType.GRID) ? ScreenUtils.dip2px(getContext(), TEUIConfig.getInstance().panelViewHeight) - customViewHeight : recycleViewHeightLinear;
        this.recyclerView.setLayoutParams(layoutParams);
    }




    public void updatePanelUIConfig(TEUIConfig uiConfig) {
        if (this.indicatorSeekBar != null) {
            this.indicatorSeekBar.setThumbDrawable(CustomDrawableUtils.createSeekBarThumbDrawable(getContext(), uiConfig.seekBarProgressColor));
            this.indicatorSeekBar.setProgressTrackColor(uiConfig.seekBarProgressColor);
            this.indicatorSeekBar.setIndicatorColor(uiConfig.seekBarProgressColor);
        }
        if (this.expandViewRadioGroup != null) {
            int count = this.expandViewRadioGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                RadioButton button = (RadioButton) this.expandViewRadioGroup.getChildAt(i);
                button.setTextColor(CustomDrawableUtils.createRadioGroupColorStateList(uiConfig.textCheckedColor, uiConfig.textColor));
            }
        }
        if (this.expandViewTopRightRevertText != null) {
            this.expandViewTopRightRevertText.setTextColor(uiConfig.textColor);
        }
        if (this.expandViewTitleDivider != null) {
            this.expandViewTitleDivider.setBackgroundColor(uiConfig.panelDividerColor);
        }
        if (this.expandViewTopRightDivider != null) {
            this.expandViewTopRightDivider.setBackgroundColor(uiConfig.panelDividerColor);
        }
        if (this.panelBgView != null) {
            this.panelBgView.setBackgroundColor(uiConfig.panelBackgroundColor);
        }
        if (this.foldedLayout != null) {
            this.foldedLayout.setBackgroundColor(uiConfig.panelBackgroundColor);
        }
        if (this.recycleViewAdapter != null) {
            this.recycleViewAdapter.updateUIConfig(uiConfig);
        }
    }


    private void clearPositionData() {
        this.itemPositions.clear();
        this.typePropertyPositionMap.clear();
        this.lastCheckedUIProperty = null;
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


    private static class DefaultTEDownloadListenerImp implements TEDownloadListener {

        @Override
        public void onDownloadSuccess(String directory) {

        }

        @Override
        public void onDownloading(int progress) {

        }

        @Override
        public void onDownloadFailed(int errorCode) {

        }
    }


    public interface TEDetailPanelListener {


        void onTopRightBtnClick();



        void onLeftBottomBtnClick(int type);


        void onRightBottomBtnClick(int type);


        void onCloseEffect(boolean isClose);


        void onRevertTE(List<TEUIProperty.TESDKParam> properties);



        void onUpdateEffect(TEUIProperty.TESDKParam param);


        void onUpdateEffectList(List<TEUIProperty.TESDKParam> paramList);



        void onClickCustomSeg(TEUIProperty uiProperty);


        void onCameraClick();

        void onMoreItemBtnClick();

        void onTitleClick(TEUIProperty uiProperty);
    }

    public static class DefaultTEDetailPanelListener implements TEDetailPanelListener {

        @Override
        public void onTopRightBtnClick() {

        }

        @Override
        public void onLeftBottomBtnClick(int type) {

        }

        @Override
        public void onRightBottomBtnClick(int type) {

        }

        @Override
        public void onCloseEffect(boolean isClose) {

        }

        @Override
        public void onRevertTE(List<TEUIProperty.TESDKParam> sdkParams) {

        }

        @Override
        public void onUpdateEffect(TEUIProperty.TESDKParam param) {

        }

        @Override
        public void onUpdateEffectList(List<TEUIProperty.TESDKParam> sdkParams) {

        }

        @Override
        public void onClickCustomSeg(TEUIProperty uiProperty) {

        }


        @Override
        public void onCameraClick() {

        }

        @Override
        public void onMoreItemBtnClick() {

        }

        @Override
        public void onTitleClick(TEUIProperty uiProperty) {

        }
    }

}
