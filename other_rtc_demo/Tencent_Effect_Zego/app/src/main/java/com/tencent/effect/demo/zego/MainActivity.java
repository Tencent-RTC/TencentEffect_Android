package com.tencent.effect.demo.zego;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tencent.effect.beautykit.TEBeautyKit;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getName();


    private Button actionBtn = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.requestPermission();
        this.actionBtn = this.findViewById(R.id.action_btn);
        this.actionBtn.setOnClickListener(v -> {
            onClick();
        });

    }


    private void onClick() {
        TEBeautyKit.setupSDK(this, LicenseConstant.mXMagicLicenceUrl, LicenseConstant.mXMagicKey, new TEBeautyKit.SetupSDKCallback() {
            @Override
            public void onResult(int errorCode, String msg) {
                if (errorCode == 0) {
                    Intent intent = new Intent(MainActivity.this, EffectActivity.class);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "auth failed " + msg + "  " + errorCode);
                }
            }
        });
    }


    private void requestPermission() {
        String[] permissionNeeded = {
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                //101 为 requestCode，可以是任何大于 0 的数字，会透传到权限请求结果回调 onRequestPermissionsResult
                requestPermissions(permissionNeeded, 101);
            } else {

            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {

        }
    }


}