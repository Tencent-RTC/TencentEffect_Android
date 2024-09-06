package com.tencent.effect.beautykit.config;


public enum DeviceDirection {

    PORTRAIT_UP(0),
    LANDSCAPE_RIGHT(90),
    PORTRAIT_DOWN(180),
    LANDSCAPE_LEFT(270),
    HORIZONTAL_UP(0),
    HORIZONTAL_DOWN(0);


    public final int angle;

    DeviceDirection(int angle) {
        this.angle = angle;
    }


}
