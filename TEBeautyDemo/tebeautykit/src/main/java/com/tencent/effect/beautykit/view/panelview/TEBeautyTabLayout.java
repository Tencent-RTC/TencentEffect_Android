package com.tencent.effect.beautykit.view.panelview;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;
import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.PanelDisplay;

import java.util.List;


public class TEBeautyTabLayout extends TabLayout {

    private static final String TAG = TEBeautyTabLayout.class.getName();
    private TEBeautyTabSelectedListener tabSelectedListener = null;

    public TEBeautyTabLayout(@NonNull Context context) {
        this(context, null);
    }

    public TEBeautyTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TEBeautyTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        this.setTabMode(TabLayout.MODE_AUTO);
        this.setSelectedTabIndicatorColor(Color.TRANSPARENT);
        this.setTabRippleColor(null);
        this.setOverScrollMode(OVER_SCROLL_NEVER);
        this.addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
                if (tab.getCustomView() instanceof TextView) {
                    if (tab.getCustomView() instanceof TextView) {
                        TextView textView = ((TextView) tab.getCustomView());
                        textView.setTextColor(getResources().getColor(R.color.te_beauty_color_FFFFFFFF));
                        textView.setTypeface(null, Typeface.BOLD);
                        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
                        layoutParams.rightMargin = 0;
                        layoutParams.leftMargin = 0;
                    }
                    if (tabSelectedListener != null) {
                        tabSelectedListener.onSelected((TEUIProperty) tab.getTag(), getSelectedTabPosition());
                    }
                }
            }

            @Override
            public void onTabUnselected(Tab tab) {
                if (tab.getCustomView() instanceof TextView) {
                    if (tab.getCustomView() instanceof TextView) {
                        TextView textView = ((TextView) tab.getCustomView());
                        textView.setTextColor(getResources().getColor(R.color.te_beauty_color_99FFFFFF));
                        textView.setTypeface(null, Typeface.NORMAL);
                        textView.setLayoutParams(textView.getLayoutParams());
                        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
                        layoutParams.rightMargin = 0;
                        layoutParams.leftMargin = 0;
                    }
                }
            }

            @Override
            public void onTabReselected(Tab tab) {

            }
        });
    }

    public void setData(List<TEUIProperty> data, int textSize) {
        this.removeAllTabs();
        if (data == null || data.isEmpty()) {
            return;
        }
        int position = -1;
        for (int index = 0; index < data.size(); index++) {
            TabLayout.Tab tab = null;
            TEUIProperty property = data.get(index);
            TextView textView = new TextView(getContext());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.rightMargin = 5;
            layoutParams.leftMargin = 5;
            textView.setLayoutParams(layoutParams);
            textView.setTextSize(textSize);
            textView.setLines(1);
            textView.setTextColor(getResources().getColor(R.color.te_beauty_color_99FFFFFF));
            textView.setText(PanelDisplay.getDisplayName(property));
            tab = newTab().setCustomView(textView);
            tab.setTag(property);
            textView.setLayoutParams(textView.getLayoutParams());
            if (property.getUiState() == TEUIProperty.UIState.CHECKED_AND_IN_USE) {
                position = index;
            }
            this.addTab(tab, property.getUiState() == TEUIProperty.UIState.CHECKED_AND_IN_USE);
        }
        if (position == -1) {  //表示没有默认选中项，所以这时手动选中第0项
            position = 0;
            checkTab(position);
            return;
        }
        int finalPosition = position;
        post(() -> setScrollPosition(finalPosition, 0, true));
    }

    public void checkTab(int index) {
        Tab tab = this.getTabAt(index);
        if (tab != null) {
            this.selectTab(tab);
        }
    }


    public void setTabSelectedListener(TEBeautyTabSelectedListener tabSelectedListener) {
        this.tabSelectedListener = tabSelectedListener;
    }

    public interface TEBeautyTabSelectedListener {
        void onSelected(TEUIProperty property, int selectedTabIndex);
    }


    public void updateItemTextColor(int checkedColor, int normalColor) {
        int tabCount = this.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Tab tab = getTabAt(i);
            if (tab != null && tab.getCustomView() instanceof TextView) {
                ((TextView) tab.getCustomView()).setTextColor(tab.isSelected() ? checkedColor : normalColor);
            }
        }
    }

}