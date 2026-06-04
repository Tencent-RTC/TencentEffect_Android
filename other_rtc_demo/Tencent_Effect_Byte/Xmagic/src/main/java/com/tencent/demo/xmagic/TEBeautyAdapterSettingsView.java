package com.tencent.demo.xmagic;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.ss.bytertc.engine.data.VideoOrientation;
import com.tencent.xmagic.demo.R;

public class TEBeautyAdapterSettingsView extends FrameLayout {

    private static final String TAG = TEBeautyAdapterSettingsView.class.getName();

    private final String[] videoOrientation = {"ADAPTIVE", "PORTRAIT", "LANDSCAPE"};
    private final String[] screenOrientationNames = {"朝上", "朝右", "朝下", "朝左"};


    private CallBack mCallBack;


    private SettingViewMode settingViewMode = new SettingViewMode();

    public TEBeautyAdapterSettingsView(Context context) {
        this(context, null);
    }

    public TEBeautyAdapterSettingsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TEBeautyAdapterSettingsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.initView(context);
    }

    public SettingViewMode getSettingViewMode() {
        return settingViewMode;
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.te_beauty_adapter_settings_view_layout, this, true);
        Switch cameraSwitcher = this.findViewById(R.id.camera_switcher);
        cameraSwitcher.setChecked(this.settingViewMode.isFront);
        cameraSwitcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.e(TAG, "onCameraChange " + isChecked);
            settingViewMode.isFront = isChecked;
            if (mCallBack != null) {
                mCallBack.onCameraChange(settingViewMode.isFront);
            }
        });

        Switch beautySwitcher = this.findViewById(R.id.beauty_switcher);
        beautySwitcher.setChecked(this.settingViewMode.isBindBeauty);
        beautySwitcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.e(TAG, "onBeautySwitcher " + isChecked);
            settingViewMode.isBindBeauty = isChecked;
            if (mCallBack != null) {
                mCallBack.onBindBeauty(settingViewMode.isBindBeauty);
            }
        });

        Spinner videoMirrorSpinner = this.findViewById(R.id.video_orientation_spinner);
        ArrayAdapter<CharSequence> videoMirrorSpinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, this.videoOrientation);
        videoMirrorSpinner.setAdapter(videoMirrorSpinnerAdapter);

        switch (settingViewMode.videoOrientation){
            case ADAPTIVE:
                videoMirrorSpinner.setSelection(0);
                break;
            case PORTRAIT:
                videoMirrorSpinner.setSelection(1);
                break;
            case LANDSCAPE:
                videoMirrorSpinner.setSelection(2);
                break;
        }

        videoMirrorSpinner.setOnItemSelectedListener(new ItemSelectedListenerImp() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        settingViewMode.videoOrientation = VideoOrientation.ADAPTIVE;
                        break;
                    case 1:
                        settingViewMode.videoOrientation = VideoOrientation.PORTRAIT;
                        break;
                    case 2:
                        settingViewMode.videoOrientation = VideoOrientation.LANDSCAPE;
                        break;
                }
                if (mCallBack != null) {
                    mCallBack.onVideoOrientationChange(settingViewMode.videoOrientation);
                }
            }
        });


        Spinner screenOrientationSpinner = this.findViewById(R.id.screen_orientation_spinner);
        ArrayAdapter<CharSequence> screenOrientationSpinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, this.screenOrientationNames);
        screenOrientationSpinner.setAdapter(screenOrientationSpinnerAdapter);
        screenOrientationSpinner.setSelection(this.settingViewMode.getPositionByOrientation());
        screenOrientationSpinner.setOnItemSelectedListener(new ItemSelectedListenerImp() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settingViewMode.screenOrientation = SettingViewMode.getOrientationByPosition(position);
                Log.e(TAG, "onScreenOrientationChange  " + position);
                if (mCallBack != null) {
                    mCallBack.onScreenOrientationChange(settingViewMode.screenOrientation);
                }
            }
        });


        Switch panelLayoutSwitch = this.findViewById(R.id.beauty_panel_layout_switch);
        panelLayoutSwitch.setChecked(this.settingViewMode.isShowPanel);
        panelLayoutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingViewMode.isShowPanel = isChecked;
            Log.e(TAG, "onPanelVisibleChange  " + isChecked);
            if (mCallBack != null) {
                mCallBack.onPanelVisibleChange(settingViewMode.isShowPanel);
            }
        });

    }


    public void setCallBack(CallBack mCallBack) {
        this.mCallBack = mCallBack;
    }

    public interface CallBack {
        void onCameraChange(boolean isFront);

        void onBindBeauty(boolean isBindBeauty);

        void onVideoOrientationChange(VideoOrientation videoOrientation);

        void onScreenOrientationChange(int orientation);


        void onPanelVisibleChange(boolean isVisible);

    }


    static class ItemSelectedListenerImp implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }


    public static void showDialog(@NonNull Context context, @NonNull TEBeautyAdapterSettingsView beautyAdapterTestView) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setView(beautyAdapterTestView);
        alertDialog.setOnDismissListener(dialog -> {
            ViewGroup viewGroup = (ViewGroup) beautyAdapterTestView.getParent();
            viewGroup.removeAllViews();
        });
        alertDialog.show();
    }

}
