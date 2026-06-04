package com.tencent.demo.xmagic;

import android.content.pm.ActivityInfo;

import com.ss.bytertc.engine.data.VideoOrientation;


public class SettingViewMode {
    public boolean isFront = true;
    public boolean isBindBeauty = true;

    public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    public boolean isShowPanel = true;

    public VideoOrientation videoOrientation = VideoOrientation.ADAPTIVE;


    public static int getOrientationByPosition(int position) {
        int orientation = 0;
        switch (position) {
            case 0:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case 1:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case 2:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case 3:
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
        }
        return orientation;
    }

    public int getPositionByOrientation() {
        switch (screenOrientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                return 0;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                return 1;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                return 2;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                return 3;
        }
        return 0;
    }


}
