package com.tencent.effect.beautykit.view.widget.indicatorseekbar;




public interface OnSeekChangeListener {

    /**
     * 进度更改的通知。
     * <p>
     * 客户端可以使用fromUser参数来区分用户发起的更改
     * 那些以编程方式发生的，如果搜索栏类型是离散系列，
     * 客户端可以使用thumbPosition参数来检查刻度上的拇指位置
     * 勾选文本参数，获取当前大拇指下方的勾选文本。
     *
     * @param seekParams 有关搜索栏的参数信息
     */
    void onSeeking(SeekParams seekParams);

    /**
     * 用户已开始触摸手势的通知。 客户可能想使用这个
     * 禁用前进搜索栏。
     *
     * @param seekBar 触摸手势开始的SeekBar
     */
    void onStartTrackingTouch(IndicatorSeekBar seekBar);

    /**
     * 用户完成触摸手势的通知。 客户可能想使用这个
     * 重新启用前进搜索栏。
     *
     * @param seekBar 触摸手势开始的SeekBar
     */
    void onStopTrackingTouch(IndicatorSeekBar seekBar);
}