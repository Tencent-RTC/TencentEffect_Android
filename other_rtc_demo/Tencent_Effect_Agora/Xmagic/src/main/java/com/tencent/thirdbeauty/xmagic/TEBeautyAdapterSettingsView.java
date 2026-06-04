package com.tencent.thirdbeauty.xmagic;

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

import com.tencent.xmagic.demo.R;

public class TEBeautyAdapterSettingsView extends FrameLayout {

    private static final String TAG = TEBeautyAdapterSettingsView.class.getName();

    private final String[] videoMirrorNames = {"AUTO", "ENABLE", "DISABLE"};
    private final String[] screenOrientationNames = {"朝上", "朝右", "朝下", "朝左"};
    private final String[] resolutionModeNames = {"LANDSCAPE_0", "PORTRAIT", "LANDSCAPE_2"};

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
            if (mCallBack.onCameraChange(isChecked)) {
                settingViewMode.isFront = isChecked;
            } else {
                cameraSwitcher.setChecked(!isChecked);
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

//        Spinner videoMirrorSpinner = this.findViewById(R.id.video_mirror_spinner);
//        ArrayAdapter<CharSequence> videoMirrorSpinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, this.videoMirrorNames);
//        videoMirrorSpinner.setAdapter(videoMirrorSpinnerAdapter);
//        videoMirrorSpinner.setSelection(this.settingViewMode.videoMirror);
//        videoMirrorSpinner.setOnItemSelectedListener(new ItemSelectedListenerImp() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                settingViewMode.videoMirror = position;
//                Log.e(TAG, "onVideoMirrorChange  " + position);
//                if (mCallBack != null) {
//                    mCallBack.onVideoMirrorChange(settingViewMode.videoMirror);
//                }
//            }
//        });


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


        Switch encoderMirrorSwitcher = this.findViewById(R.id.encoder_mirror_switcher);
        encoderMirrorSwitcher.setChecked(this.settingViewMode.isEncoderMirror);
        encoderMirrorSwitcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingViewMode.isEncoderMirror = isChecked;
            Log.e(TAG, "onEncoderMirrorChange  " + isChecked);
            if (mCallBack != null) {
                mCallBack.onEncoderMirrorChange(settingViewMode.isEncoderMirror);
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
        boolean onCameraChange(boolean isFront);

        void onBindBeauty(boolean isBindBeauty);

//        void onVideoMirrorChange(int mirrorType);

        void onScreenOrientationChange(int orientation);


        void onEncoderMirrorChange(boolean isMirror);

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
