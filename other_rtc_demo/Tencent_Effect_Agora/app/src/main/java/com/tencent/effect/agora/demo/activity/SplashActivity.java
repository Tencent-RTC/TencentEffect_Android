package com.tencent.effect.agora.demo.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.xmagic.telicense.TELicenseCheck;
import com.tencent.effect.agora.demo.utils.AppUtils;
import com.tencent.effect.agora.demo.beauty.LicenseConstant;
import com.vcube.tencent.effect.R;


public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getName();

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS =
            {
                    android.Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE

            };

    private boolean isAuthSuccess = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_splash);
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
        this.findViewById(R.id.main_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAuthSuccess) {
                    Intent intent = new Intent(view.getContext(), MainActivity.class);
                    startActivity(intent);
                }
            }
        });

        this.findViewById(R.id.adapter_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAuthSuccess) {
                    Intent intent = new Intent(view.getContext(), AdapterActivity.class);
                    startActivity(intent);
                }
            }
        });
        copyXMagicRes();

    }


    private void copyXMagicRes() {
        if (isCopyRes()) {
            Log.e(TAG, "Has been copied");
            showMessage("Has been copied");
            auth();
        } else {
            new Thread(() -> {
                TEBeautyKit.copyRes(SplashActivity.this);
                Log.e(TAG, "copy res success");
                showMessage("copy res success");
                saveCopyData();
                auth();
            }).start();
        }
    }

    void showMessage(String message) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void auth() {
        TEBeautyKit.setTELicense(this, LicenseConstant.mXMagicLicenceUrl, LicenseConstant.mXMagicKey, (errorCode, msg) -> {
            if (errorCode == TELicenseCheck.ERROR_OK) {
                showMessage("Effect Auth Success");
                this.isAuthSuccess = true;
            } else {
                this.isAuthSuccess = false;
                showMessage("Effect Auth Error,code = " + errorCode);
            }
        });
    }


    private boolean checkSelfPermission() {
        if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[2]) != PackageManager.PERMISSION_GRANTED
        ) {
            return false;
        }
        return true;
    }


    private boolean isCopyRes() {
        String appVersionName = AppUtils.getAppVersionName(this);
        SharedPreferences sp = getSharedPreferences("demo_settings", Context.MODE_PRIVATE);
        String savedVersionName = sp.getString("resource_copied", "");
        return savedVersionName.equals(appVersionName);
    }

    private void saveCopyData() {
        String appVersionName = AppUtils.getAppVersionName(this);
        getSharedPreferences("demo_settings", Context.MODE_PRIVATE).edit()
                .putString("resource_copied", appVersionName).commit();
    }

}
