package com.tencent.effect.beautykit.view.widget;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.config.TEUIConfig;


public class MarqueeTextView extends View {

    // 文本相关
    private String text = "";
    private TextPaint textPaint;
    private boolean lastRtl = false; // 记录上次布局方向，用于检测运行时方向切换

    // 文本宽度、偏移等
    private float textWidth = 0f;
    private float offsetX = 0f; // 当前文本左边缘在内容区域中的横向偏移
    private float baselineY = 0f;

    // 滚动控制
    private boolean marqueeRequested = false; // 外部请求开启
    private boolean marqueeActive = false;    // 实际在滚动

    private float speedPxPerSec = 60f; // px/s

    private long lastFrameNanos = 0L;
    private int paddingLeftPx = 0;
    private int paddingRightPx = 0;
    private int contentWidth = 0; // 可用宽度 = getWidth() - paddingLeft - paddingRight

    public MarqueeTextView(Context context) {
        super(context);
        init(null);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF000000); // 默认黑色
        textPaint.setTextSize(spToPx(16f));

        // 解析自定义属性
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.te_beauty_marquee_textview);
            String t = ta.getString(R.styleable.te_beauty_marquee_textview_mt_text);
            if (t != null)
                text = t;

            int color = ta.getColor(R.styleable.te_beauty_marquee_textview_mt_textColor, textPaint.getColor());
            textPaint.setColor(color);
            if (ta.hasValue(R.styleable.te_beauty_marquee_textview_mt_textSize)) {
                int sizePx = ta.getDimensionPixelSize(R.styleable.te_beauty_marquee_textview_mt_textSize,
                        (int) textPaint.getTextSize());
                textPaint.setTextSize(sizePx);
            }
            if (ta.hasValue(R.styleable.te_beauty_marquee_textview_mt_speed)) {
                speedPxPerSec = ta.getFloat(R.styleable.te_beauty_marquee_textview_mt_speed, speedPxPerSec);
            }
            ta.recycle();
        }

        // 注意：此处不调用 computeBaselineY()，因为构造阶段 getHeight() 尚为 0，
        // 基线会在 onSizeChanged() 中（尺寸确定后）被正确计算
        computeTextWidth();
    }

    // 外部调用：设置文本
    public void setText(String t) {
        this.text = (t == null) ? "" : t;
        computeTextWidth();
        // 文本变化时强制结束当前滚动，让 updateMarqueeState 从初始位置重新开始，
        // 避免 RecyclerView 复用场景下新文本从旧文本的滚动偏移继续滚动
        marqueeActive = false;
        lastFrameNanos = 0L;
        updateMarqueeState();
        invalidate();
    }

    public String getText() {
        return text;
    }

    // 外部调用：设置文本颜色
    public void setTextColor(int color) {
        textPaint.setColor(color);
        invalidate();
    }

    // 外部调用：设置字号（单位：sp）
    public void setTextSizeSp(float sp) {
        textPaint.setTextSize(spToPx(sp));
        computeBaselineY();
        updateMarqueeState();
        invalidate();
    }

    // 外部调用：设置滚动速度（px/s）
    public void setSpeedPxPerSec(float speed) {
        if (speed > 0)
            speedPxPerSec = speed;
    }

    // 外部调用：开启跑马灯
    public void startMarquee() {
        marqueeRequested = true;
        updateMarqueeState();
    }

    // 外部调用：关闭跑马灯
    public void stopMarquee() {
        marqueeRequested = false;
        marqueeActive = false;
        offsetX = getInitialOffset();
        invalidate();
    }

    // 内部：暂停滚动（保留 marqueeRequested，不清 offsetX 与否由恢复策略决定）
    // 这里采用"不保留 offsetX"策略：暂停时只停动画，恢复时归零重滚
    private void pauseMarquee() {
        marqueeActive = false;
        lastFrameNanos = 0L;
    }

    // 内部：可见后按用户意图恢复；本策略下 offsetX 归零，从头滚动
    private void resumeMarqueeIfNeeded() {
        if (marqueeRequested) {
            offsetX = getInitialOffset();
            updateMarqueeState();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // View 从窗口移除时，停止滚动重绘请求，避免持续占用 CPU / 造成泄漏
        pauseMarquee();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            resumeMarqueeIfNeeded();
        } else {
            pauseMarquee();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        // 应用切到后台 / Activity onStop 时窗口不可见，暂停以省电
        if (visibility == VISIBLE) {
            resumeMarqueeIfNeeded();
        } else {
            pauseMarquee();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // 计算文本的宽度和高度
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float textWidth = textPaint.measureText(text);

        // 处理宽度
        int desiredWidth;
        if (widthMode == MeasureSpec.EXACTLY) {
            desiredWidth = widthSize;
        } else {
            desiredWidth = (int) (textWidth + getPaddingLeft() + getPaddingRight());
            if (widthMode == MeasureSpec.AT_MOST) {
                desiredWidth = Math.min(desiredWidth, widthSize);
            }
        }

        // 处理高度
        int desiredHeight;
        if (heightMode == MeasureSpec.EXACTLY) {
            desiredHeight = heightSize;
        } else {
            desiredHeight = (int) (textHeight + getPaddingTop() + getPaddingBottom());
            if (heightMode == MeasureSpec.AT_MOST) {
                desiredHeight = Math.min(desiredHeight, heightSize);
            }
        }

        setMeasuredDimension(desiredWidth, desiredHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        paddingLeftPx = getPaddingStart();
        paddingRightPx = getPaddingEnd();
        contentWidth = Math.max(0, w - paddingLeftPx - paddingRightPx);
        computeBaselineY();
        updateMarqueeState();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (text == null || text.isEmpty())
            return;
        if (contentWidth <= 0)
            return;

        boolean rtl = isRtlLayout();
        // 方向发生变化时，重置到对应方向的初始偏移，保证运行时切换方向立即生效
        if (rtl != lastRtl) {
            lastRtl = rtl;
            offsetX = rtl ? (contentWidth - textWidth) : 0f;
            lastFrameNanos = 0L;
        }

        if (marqueeActive) {
            long nowNanos = System.nanoTime();
            if (lastFrameNanos == 0L)
                lastFrameNanos = nowNanos;
            float dt = (nowNanos - lastFrameNanos) / 1_000_000f; // 转换为毫秒

            // 只有时间差大于阈值才更新偏移
            if (dt > 30f) { // 30毫秒阈值
                lastFrameNanos = nowNanos;
                float deltaX = speedPxPerSec * dt / 1000;

                if (deltaX > 1f) {
                    if (rtl) {
                        // RTL：文本向右移动，移出右边界后从左侧重新进入
                        offsetX += deltaX;
                        if (offsetX > contentWidth) {
                            offsetX = -textWidth;
                        }
                    } else {
                        // LTR：文本向左移动，移出左边界后从右侧重新进入
                        offsetX -= deltaX;
                        if (offsetX < -textWidth) {
                            offsetX = contentWidth;
                        }
                    }
                }
            }

            // 始终绘制，避免首帧 dt 过小时出现文本空白
            canvas.save();
            canvas.translate(paddingLeftPx + offsetX, 0f);
            canvas.drawText(text, 0f, baselineY, textPaint);
            canvas.restore();

            // 使用固定帧率(25fps)的重绘
            postInvalidateDelayed(40);
        } else {
            // 不滚动：按布局方向对齐绘制
            canvas.save();
            canvas.translate(paddingLeftPx, 0f);
            if (textWidth > contentWidth) {
                // 文本宽度超过控件宽度，截断并添加省略号：
                // LTR 保留左侧、右侧省略；RTL 保留右侧、左侧省略
                TextUtils.TruncateAt where = rtl ? TextUtils.TruncateAt.START : TextUtils.TruncateAt.END;
                CharSequence truncatedText = TextUtils.ellipsize(text, textPaint, contentWidth, where);
                String str = truncatedText.toString();
                // RTL 右对齐：文本右边缘贴 contentWidth；LTR 左对齐：左边缘贴 0
                float x = rtl ? (contentWidth - textPaint.measureText(str)) : 0f;
                canvas.drawText(str, x, baselineY, textPaint);
            } else {
                // 文本宽度不超过控件宽度，水平居中显示
                float centerX = (contentWidth - textWidth) / 2f;
                canvas.drawText(text, centerX, baselineY, textPaint);
            }
            canvas.restore();
        }
    }

    // 计算文本宽度
    private void computeTextWidth() {
        textWidth = (text != null) ? textPaint.measureText(text) : 0f;
    }

    // 计算文本基线，使文本垂直居中
    private void computeBaselineY() {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        baselineY = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
    }

    // 根据当前状态判断是否需要滚动，以及初始化滚动位置
    private void updateMarqueeState() {
        if (!marqueeRequested) {
            marqueeActive = false;
            offsetX = getInitialOffset();
            lastFrameNanos = 0L;
            return;
        }

        if (contentWidth <= 0) {
            // 宽度尚未确定，稍后再评估
            return;
        }

        if (textWidth > contentWidth) {
            // 需要滚动
            if (!marqueeActive) {
                offsetX = getInitialOffset();  // 从初始对齐位置开始滚动
            }
            marqueeActive = true;
            lastFrameNanos = 0L;
            invalidate();
        } else {
            // 文本宽度不超过可用宽度，不滚动
            marqueeActive = false;
            offsetX = getInitialOffset();
            invalidate();
        }
    }

    // 实时读取当前布局方向，保证运行时修改 isLayoutRtl 后立即生效
    private boolean isRtlLayout() {
        return TEUIConfig.getInstance().isLayoutRtl;
    }

    // 计算初始文本偏移：LTR 左对齐(0)，RTL 右对齐(contentWidth - textWidth)
    private float getInitialOffset() {
        return isRtlLayout() ? (contentWidth - textWidth) : 0f;
    }


    // 辅助：单位换算
    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}