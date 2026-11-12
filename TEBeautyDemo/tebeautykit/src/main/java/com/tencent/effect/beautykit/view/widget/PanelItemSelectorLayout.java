package com.tencent.effect.beautykit.view.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.utils.ScreenUtils;



public class PanelItemSelectorLayout extends LinearLayout {

    private Paint borderPaint;
    private Path path;
    private RectF rectF;
    private float strokeWidth = 8;
    private float cornerRadius = -1;

    public PanelItemSelectorLayout(Context context) {
        this(context, null);
    }

    public PanelItemSelectorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanelItemSelectorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(attrs);
    }



    private void init(AttributeSet attrs) {
        // 初始化边框画笔

        this.cornerRadius = ScreenUtils.dip2px(getContext(), 10);
        this.strokeWidth = ScreenUtils.dip2px(getContext(), 5);
        if (attrs != null) {
            TypedArray attributes = getContext().obtainStyledAttributes(attrs,
                    R.styleable.te_beauty_panel_item_selector);
            this.cornerRadius = attributes.getDimension(R.styleable.te_beauty_panel_item_selector_corner_radius, cornerRadius);
            this.strokeWidth = attributes.getDimension(R.styleable.te_beauty_panel_item_selector_strokeWidth, strokeWidth);
            attributes.recycle();
        }
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokeWidth); // 边框宽度
        borderPaint.setAntiAlias(true);
        // 初始化路径
        path = new Path();
        rectF = new RectF();
        setBackgroundColor(Color.TRANSPARENT);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 在尺寸变化时重新设置渐变色
        int[] colors = {0xFFDCFC73, 0xFF74ECEE, 0xFF7BF38E};
        float[] positions = {0f, 0.6f, 1f}; // 颜色位置（0-1）
        Shader shader = new LinearGradient(
                0, 0, // 起点：左上角
                w, h, // 终点：右下角
                colors, positions, Shader.TileMode.CLAMP
        );
        borderPaint.setShader(shader);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (isChecked) {
            // 计算圆角矩形区域，考虑边框宽度
            float halfStroke = strokeWidth / 2f;
            rectF.set(halfStroke, halfStroke, getWidth() - halfStroke, getHeight() - halfStroke);
            path.reset(); // 重置路径
            path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
            // 绘制边框
            canvas.drawPath(path, borderPaint);
        }
    }

    private boolean isChecked = false;

    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
        this.postInvalidate();
    }

}
