package com.tencent.effect.agora.demo.utils;

import android.text.TextUtils;

public class TimeComputer {

    private String TAG = TimeComputer.class.getName();
    private float timeCount = 0;
    private int count = 0;
    private long startTime = 0;

    public TimeComputer() {
    }

    public TimeComputer(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            this.TAG = tag;
        }
    }


    public void startTime() {
//        this.startTime = System.currentTimeMillis();
    }

    public void endTime() {
//        float currentTime = System.currentTimeMillis() - startTime;
//        timeCount = this.timeCount + currentTime;
//        count++;
//        float avgTime = timeCount / count;
//        Log.e(TAG, "computeTime time is " + currentTime + "  " + avgTime);
    }
}
