package com.tencent.effect.beautykit.view.widget.indicatorseekbar;


import androidx.annotation.ColorInt;


public interface ColorCollector {

    /**
     * 收集每个部分轨道的颜色
     *
     * @param colorIntArr ColorInt 每个部分轨道颜色的容器。
     * 该数组的长度将自动等于部分轨道的计数。
     * @return True 如果应用颜色，否则不改变
     */
    boolean collectSectionTrackColor(@ColorInt int[] colorIntArr);
}