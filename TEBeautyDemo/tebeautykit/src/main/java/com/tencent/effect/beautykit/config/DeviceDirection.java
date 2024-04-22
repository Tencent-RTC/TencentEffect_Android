package com.tencent.effect.beautykit.config;

/**
 * 手机方向
 */
public enum DeviceDirection {

    PORTRAIT_UP(0),     //规定：手机正常竖直放置的情况下，与重力方向相反的方向为上方向，
    LANDSCAPE_RIGHT(90),  //规定：上方向顺时针旋转90度
    PORTRAIT_DOWN(180),   //规定：手机正常竖直放置的情况下，与重力方向相同的方向为上方向，
    LANDSCAPE_LEFT(270);   //规定：上方向逆时针旋转90度


    public final int angle;

    DeviceDirection(int angle) {
        this.angle = angle;
    }


}
