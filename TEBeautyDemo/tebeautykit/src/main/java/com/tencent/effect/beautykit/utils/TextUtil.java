package com.tencent.effect.beautykit.utils;

import android.graphics.Paint;

public class TextUtil {

    public static int getTextWidth(String text, int textSize) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);

        float textWidth = paint.measureText(text);
        int width = (int) Math.ceil(textWidth);
        return width;
    }
}
