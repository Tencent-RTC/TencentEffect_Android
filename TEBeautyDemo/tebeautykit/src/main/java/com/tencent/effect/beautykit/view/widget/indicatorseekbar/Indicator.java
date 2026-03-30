package com.tencent.effect.beautykit.view.widget.indicatorseekbar;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.utils.ScreenUtils;


public class Indicator {
    
    // ==================== 常量定义 ====================
    
    /**
     * 指示器固定宽度（单位：dp）
     * 固定宽度可以避免文本内容变化时尺寸变化导致的卡顿
     */
    private static final int INDICATOR_FIXED_WIDTH_DP = 40;
    
    // ==================== 成员变量 ====================
    
    private final int mWindowWidth;
    private int[] mLocation = new int[2];
    private ArrowView mArrowView;
    private TextView mProgressTextView;
    private PopupWindow mIndicatorPopW;
    private LinearLayout mTopContentView;
    private int mGap;
    private int mIndicatorColor;
    private Context mContext;
    private int mIndicatorType;
    private IndicatorSeekBar mSeekBar;
    private View mIndicatorView;
    private View mIndicatorCustomView;
    private View mIndicatorCustomTopContentView;
    private float mIndicatorTextSize;
    private int mIndicatorTextColor;

    // 性能优化：缓存测量结果，避免每次滑动都重新测量
    private int mCachedContentWidth = -1;
    private int mCachedContentHeight = -1;
    // 固定的内容宽高（防止文本变化导致尺寸变化引起卡顿）
    private int mFixedContentWidth = -1;
    private int mFixedContentHeight = -1;
    private boolean mSizeFixed = false;
    // 缓存上一次箭头的偏移量，避免不必要的更新
    private float mLastArrowTranslationX = 0;

    // ==================== 构造方法 ====================
    
    public Indicator(Context context,
                     IndicatorSeekBar seekBar,
                     int indicatorColor,
                     int indicatorType,
                     int indicatorTextSize,
                     int indicatorTextColor,
                     View indicatorCustomView,
                     View indicatorCustomTopContentView) {
        this.mContext = context;
        this.mSeekBar = seekBar;
        this.mIndicatorColor = indicatorColor;
        this.mIndicatorType = indicatorType;
        this.mIndicatorCustomView = indicatorCustomView;
        this.mIndicatorCustomTopContentView = indicatorCustomTopContentView;
        this.mIndicatorTextSize = indicatorTextSize;
        this.mIndicatorTextColor = indicatorTextColor;

        mWindowWidth = getWindowWidth();
        mGap = ScreenUtils.dp2px(mContext, 2);
        initIndicator();
    }

    // ==================== 初始化方法 ====================
    
    private void initIndicator() {
        if (mIndicatorType == IndicatorType.CUSTOM) {
            initCustomIndicator();
        } else if (mIndicatorType == IndicatorType.CIRCULAR_BUBBLE) {
            initCircleBubbleIndicator();
        } else {
            initDefaultIndicator();
        }
    }
    
    /**
     * 初始化自定义指示器
     */
    private void initCustomIndicator() {
        if (mIndicatorCustomView == null) {
            throw new IllegalArgumentException("the attr：indicator_custom_layout must be set while you set the indicator type to CUSTOM.");
        }
        
        mIndicatorView = mIndicatorCustomView;
        // 查找自定义布局中的进度TextView
        int progressTextViewId = mContext.getResources().getIdentifier("isb_progress", "id", mContext.getApplicationContext().getPackageName());
        if (progressTextViewId > 0) {
            View view = mIndicatorView.findViewById(progressTextViewId);
            if (view != null) {
                if (view instanceof TextView) {
                    mProgressTextView = (TextView) view;
                    mProgressTextView.setText(mSeekBar.getIndicatorTextString());
                    mProgressTextView.setTextSize(ScreenUtils.px2sp(mContext, mIndicatorTextSize));
                    mProgressTextView.setTextColor(mIndicatorTextColor);
                } else {
                    throw new ClassCastException("the view identified by isb_progress in indicator custom layout can not be cast to TextView");
                }
            }
        }
    }
    
    /**
     * 初始化圆形气泡指示器
     */
    private void initCircleBubbleIndicator() {
        mIndicatorView = new CircleBubbleView(mContext, mIndicatorTextSize, mIndicatorTextColor, mIndicatorColor, "1000");
        ((CircleBubbleView) mIndicatorView).setProgress(mSeekBar.getIndicatorTextString());
    }
    
    /**
     * 初始化默认指示器（圆角矩形或方角矩形）
     */
    private void initDefaultIndicator() {
        mIndicatorView = View.inflate(mContext, R.layout.te_beauty_isb_indicator, null);
        
        // 初始化容器
        mTopContentView = (LinearLayout) mIndicatorView.findViewById(R.id.indicator_container);
        
        // 初始化箭头
        mArrowView = (ArrowView) mIndicatorView.findViewById(R.id.indicator_arrow);
        mArrowView.setColor(mIndicatorColor);
        mArrowView.setVisibility(View.GONE);
        
        // 初始化进度文本
        mProgressTextView = (TextView) mIndicatorView.findViewById(R.id.isb_progress);
        mProgressTextView.setText(mSeekBar.getIndicatorTextString());
        mProgressTextView.setTextSize(ScreenUtils.px2sp(mContext, mIndicatorTextSize));
        mProgressTextView.setTextColor(mIndicatorTextColor);
        
        // 设置背景
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mTopContentView.setBackground(getGradientDrawable());
        } else {
            mTopContentView.setBackgroundDrawable(getGradientDrawable());
        }
        
        // 处理自定义顶部内容视图
        if (mIndicatorCustomTopContentView != null) {
            setupCustomTopContentView();
        }
    }
    
    /**
     * 设置自定义顶部内容视图
     */
    private void setupCustomTopContentView() {
        int progressTextViewId = mContext.getResources().getIdentifier("isb_progress", "id", mContext.getApplicationContext().getPackageName());
        View topContentView = mIndicatorCustomTopContentView;
        if (progressTextViewId > 0) {
            View tv = topContentView.findViewById(progressTextViewId);
            if (tv != null && tv instanceof TextView) {
                setTopContentView(topContentView, (TextView) tv);
            } else {
                setTopContentView(topContentView);
            }
        } else {
            setTopContentView(topContentView);
        }
    }

    @NonNull
    private GradientDrawable getGradientDrawable() {
        GradientDrawable tvDrawable;
        if (mIndicatorType == IndicatorType.ROUNDED_RECTANGLE) {
            tvDrawable =
                    (GradientDrawable) mContext.getResources().getDrawable(R.drawable.te_beauty_isb_indicator_rounded_corners);
        } else {
            tvDrawable =
                    (GradientDrawable) mContext.getResources().getDrawable(R.drawable.te_beauty_isb_indicator_square_corners);
        }
        tvDrawable.setColor(mIndicatorColor);
        return tvDrawable;
    }

    private int getWindowWidth() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            return wm.getDefaultDisplay().getWidth();
        }
        return 0;
    }

    // ==================== PopupWindow 管理 ====================
    
    /**
     * 初始化 PopupWindow
     */
    void iniPop() {
        if (mIndicatorPopW != null) {
            return;
        }
        if (mIndicatorType != IndicatorType.NONE && mIndicatorView != null) {
            // 预先计算固定尺寸
            calculateFixedSize();
            
            // 使用固定宽度创建 PopupWindow，避免尺寸变化导致卡顿
            int popWidth = ScreenUtils.dp2px(mContext, INDICATOR_FIXED_WIDTH_DP);
            int popHeight = mFixedContentHeight > 0 ? mFixedContentHeight : WindowManager.LayoutParams.WRAP_CONTENT;
            mIndicatorPopW = new PopupWindow(mIndicatorView, popWidth, popHeight, false);
            mCachedContentWidth = popWidth;
            
            // 性能优化：禁用裁剪可以减少系统计算开销
            mIndicatorPopW.setClippingEnabled(false);
        }
    }
    
    /**
     * 计算并固定内容尺寸，防止文本变化导致尺寸变化引起卡顿
     */
    private void calculateFixedSize() {
        if (mSizeFixed) {
            return;
        }
        
        // 使用固定宽度常量
        mFixedContentWidth = ScreenUtils.dp2px(mContext, INDICATOR_FIXED_WIDTH_DP);
        
        // 测量高度
        mIndicatorView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        mFixedContentHeight = mIndicatorView.getMeasuredHeight();
        
        // 固定 TextView 的宽度，防止文本变化时重新布局
        fixTextViewSize();
        
        // 缓存固定尺寸
        mCachedContentWidth = mFixedContentWidth;
        mCachedContentHeight = mFixedContentHeight;
        mSizeFixed = true;
    }
    
    /**
     * 固定 TextView 的尺寸，防止文本变化导致重新布局
     */
    private void fixTextViewSize() {
        int fixedWidth = ScreenUtils.dp2px(mContext, INDICATOR_FIXED_WIDTH_DP);
        
        // 固定 TextView 尺寸
        if (mProgressTextView != null) {
            mProgressTextView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int textViewHeight = mProgressTextView.getMeasuredHeight();
            
            ViewGroup.LayoutParams params = mProgressTextView.getLayoutParams();
            if (params != null) {
                params.width = fixedWidth;
                params.height = textViewHeight;
                mProgressTextView.setLayoutParams(params);
                mProgressTextView.setGravity(android.view.Gravity.CENTER);
            }
        }
        
        // 固定顶部容器尺寸
        if (mTopContentView != null) {
            mTopContentView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int containerHeight = mTopContentView.getMeasuredHeight();
            
            ViewGroup.LayoutParams params = mTopContentView.getLayoutParams();
            if (params != null) {
                params.width = fixedWidth;
                params.height = containerHeight;
                mTopContentView.setLayoutParams(params);
                mTopContentView.setGravity(android.view.Gravity.CENTER);
            }
        }
    }

    // ==================== 位置更新方法 ====================
    
    /**
     * 更新指示器位置
     *
     * @param touchX 触摸位置的 x 坐标（不含 padding left）
     */
    void update(float touchX) {
        if (!mSeekBar.isEnabled() || mSeekBar.getVisibility() != View.VISIBLE) {
            return;
        }
        if (mIndicatorPopW == null || !mIndicatorPopW.isShowing()) {
            return;
        }
        
        refreshProgressText();
        doUpdateDirect(touchX);
    }

    /**
     * 直接执行 PopupWindow 位置更新
     */
    private void doUpdateDirect(float touchX) {
        if (mIndicatorPopW == null || !mIndicatorPopW.isShowing()) {
            return;
        }
        
        // 确保缓存尺寸有效
        ensureCachedSize();
        
        // 计算偏移量并更新位置
        int xOffset = (int) (touchX - mCachedContentWidth / 2);
        int yOffset = -(mSeekBar.getMeasuredHeight() + mCachedContentHeight - mSeekBar.getPaddingTop() + mGap);
        mIndicatorPopW.update(mSeekBar, xOffset, yOffset, -1, -1);
        
        // 调整箭头位置
        adjustArrow(touchX);
    }
    
    /**
     * 确保缓存尺寸有效
     */
    private void ensureCachedSize() {
        if (mCachedContentWidth > 0) {
            return;
        }
        
        if (mFixedContentWidth > 0) {
            mCachedContentWidth = mFixedContentWidth;
            mCachedContentHeight = mFixedContentHeight;
        } else {
            mIndicatorPopW.getContentView().measure(0, 0);
            mCachedContentWidth = mIndicatorPopW.getContentView().getMeasuredWidth();
            mCachedContentHeight = mIndicatorPopW.getContentView().getMeasuredHeight();
        }
    }

    /**
     * 调整箭头位置（处理边缘情况）
     */
    private void adjustArrow(float touchX) {
        if (mIndicatorType == IndicatorType.CUSTOM || mIndicatorType == IndicatorType.CIRCULAR_BUBBLE) {
            return;
        }
        if (mArrowView == null || mCachedContentWidth <= 0) {
            return;
        }
        
        int halfWidth = mCachedContentWidth / 2;
        mSeekBar.getLocationInWindow(mLocation);
        int indicatorScreenX = mLocation[0] + mSeekBar.getPaddingLeft();
        
        float newTranslationX;
        if (indicatorScreenX + touchX < halfWidth) {
            // 左边缘：箭头向左偏移
            newTranslationX = -(halfWidth - indicatorScreenX - touchX);
        } else if (mWindowWidth - indicatorScreenX - touchX < halfWidth) {
            // 右边缘：箭头向右偏移
            newTranslationX = halfWidth - (mWindowWidth - indicatorScreenX - touchX);
        } else {
            // 中间区域：无偏移
            newTranslationX = 0;
        }
        
        // 仅当偏移量变化足够大时才更新
        if (Math.abs(newTranslationX - mLastArrowTranslationX) > 1.0f) {
            mArrowView.setTranslationX(newTranslationX);
            mLastArrowTranslationX = newTranslationX;
        }
    }

    /**
     * 显示指示器
     *
     * @param touchX 触摸位置的 x 坐标（不含 padding left）
     */
    void show(float touchX) {
        if (!mSeekBar.isEnabled() || mSeekBar.getVisibility() != View.VISIBLE) {
            return;
        }
        refreshProgressText();
        
        if (mIndicatorPopW != null) {
            ensureCachedSize();
            mIndicatorPopW.showAsDropDown(mSeekBar, 
                    (int) (touchX - mCachedContentWidth / 2f),
                    -(mSeekBar.getMeasuredHeight() + mCachedContentHeight - mSeekBar.getPaddingTop() + mGap));
            adjustArrow(touchX);
        }
    }

    /**
     * 隐藏指示器
     */
    void hide() {
        if (mIndicatorPopW != null) {
            mIndicatorPopW.dismiss();
        }
    }

    /**
     * 指示器是否正在显示
     */
    boolean isShowing() {
        return mIndicatorPopW != null && mIndicatorPopW.isShowing();
    }

    // ==================== 文本更新方法 ====================
    
    void setProgressTextView(String text) {
        if (mIndicatorView instanceof CircleBubbleView) {
            ((CircleBubbleView) mIndicatorView).setProgress(text);
        } else if (mProgressTextView != null) {
            mProgressTextView.setText(text);
        }
    }

    void refreshProgressText() {
        String tickTextString = mSeekBar.getIndicatorTextString();
        if (mIndicatorView instanceof CircleBubbleView) {
            ((CircleBubbleView) mIndicatorView).setProgress(tickTextString);
        } else if (mProgressTextView != null) {
            mProgressTextView.setText(tickTextString);
        }
    }

    // ==================== 缓存管理方法 ====================
    
    /**
     * 清除缓存的测量结果，下次更新时会重新测量
     * 当指示器内容发生较大变化（如字体大小改变）时调用
     */
    void invalidateMeasureCache() {
        mCachedContentWidth = -1;
        mCachedContentHeight = -1;
        mFixedContentWidth = -1;
        mFixedContentHeight = -1;
        mSizeFixed = false;
    }

    /**
     * 更新常驻指示器的位置（用于 IndicatorStayAlways 模式）
     * 
     * @param indicatorOffset 指示器的水平偏移量
     * @param arrowOffset 箭头的水平偏移量
     */
    void updateStayIndicatorLocation(int indicatorOffset, int arrowOffset) {
        if (mIndicatorView != null) {
            mIndicatorView.setTranslationX(indicatorOffset);
        }
        if (mArrowView != null && Math.abs(arrowOffset - mLastArrowTranslationX) > 0.5f) {
            mArrowView.setTranslationX(arrowOffset);
            mLastArrowTranslationX = arrowOffset;
        }
    }

    // ==================== 辅助方法 ====================
    
    private void setMargin(View view, int left, int top, int right, int bottom) {
        if (view == null) {
            return;
        }
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            layoutParams.setMargins(
                    left == -1 ? layoutParams.leftMargin : left,
                    top == -1 ? layoutParams.topMargin : top,
                    right == -1 ? layoutParams.rightMargin : right,
                    bottom == -1 ? layoutParams.bottomMargin : bottom);
            view.requestLayout();
        }
    }

    View getInsideContentView() {
        return mIndicatorView;
    }

    // ==================== 公开 API ====================

    /**
     * 获取指示器内容视图
     *
     * @return 指示器内部的视图
     */
    public View getContentView() {
        return mIndicatorView;
    }

    /**
     * 设置自定义指示器视图（会替换箭头）
     *
     * @param customIndicatorView 新的指示器内容视图
     */
    public void setContentView(@NonNull View customIndicatorView) {
        this.mIndicatorType = IndicatorType.CUSTOM;
        this.mIndicatorCustomView = customIndicatorView;
        initIndicator();
    }

    /**
     * 设置自定义指示器视图，并指定进度文本视图
     *
     * @param customIndicatorView 新的指示器内容视图
     * @param progressTextView    用于显示进度的 TextView，必须在 customIndicatorView 中
     */
    public void setContentView(@NonNull View customIndicatorView, TextView progressTextView) {
        this.mProgressTextView = progressTextView;
        this.mIndicatorType = IndicatorType.CUSTOM;
        this.mIndicatorCustomView = customIndicatorView;
        initIndicator();
    }

    /**
     * 获取指示器顶部内容视图
     * 如果指示器类型是 CUSTOM 或 CIRCULAR_BUBBLE，返回 null
     *
     * @return 指示器顶部内容视图（不含箭头）
     */
    public View getTopContentView() {
        return mTopContentView;
    }

    /**
     * 设置指示器顶部内容视图（不影响箭头）
     * 如果指示器类型是 CUSTOM 或 CIRCULAR_BUBBLE，此方法无效
     *
     * @param topContentView 顶部内容视图
     */
    public void setTopContentView(@NonNull View topContentView) {
        setTopContentView(topContentView, null);
    }

    /**
     * 设置指示器顶部内容视图，并指定进度文本视图
     *
     * @param topContentView   顶部内容视图
     * @param progressTextView 用于显示进度的 TextView，必须在 topContentView 中
     */
    public void setTopContentView(@NonNull View topContentView, @Nullable TextView progressTextView) {
        this.mProgressTextView = progressTextView;
        this.mTopContentView.removeAllViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            topContentView.setBackground(getGradientDrawable());
        } else {
            topContentView.setBackgroundDrawable(getGradientDrawable());
        }
        this.mTopContentView.addView(topContentView);
    }

}