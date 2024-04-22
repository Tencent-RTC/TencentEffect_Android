package com.tencent.demo.beauty.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.demo.AppConfig;
import com.tencent.demo.R;
import com.tencent.demo.constant.LicenseConstant;
import com.tencent.demo.utils.AppUtils;
import com.tencent.demo.utils.PermissionHandler;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.xmagic.telicense.TELicenseCheck;
import java.io.File;
import java.util.Locale;

public class TEMenuActivity extends AppCompatActivity {

    private static final String TAG = TEMenuActivity.class.getName();
    private int authState = LicenseConstant.AUTH_STATE_FAILED;
    private TextView mLoadingView = null;
    private final PermissionHandler mPermissionHandler = new PermissionHandler(this) {
        @Override
        protected void onAllPermissionGranted() {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TEUIConfig.getInstance().setSystemLocal(Locale.getDefault());
        setContentView(R.layout.te_beauty_activity_menu_layout);
        mLoadingView = findViewById(R.id.te_menu_loading_view);
        findViewById(R.id.btn_start_camera).setOnClickListener(view -> {
            authAndStartCamera();
        });
        mPermissionHandler.start();

        String resPath = new File(getFilesDir(), AppConfig.getInstance().getBeautyFileDirName()).getAbsolutePath();
        TEBeautyKit.setResPath(resPath);
        copyRes();
    }

    private void authAndStartCamera() {
        if (authState == LicenseConstant.AUTH_STATE_SUCCEED) {
            Intent intent = new Intent(this, TECameraBaseActivity.class);
            startActivity(intent);
            return;
        }
        if (authState == LicenseConstant.AUTH_STATE_AUTHING) {
            Toast.makeText(this, "Authenticating，please try later", Toast.LENGTH_LONG).show();
            return;
        }
        authState = LicenseConstant.AUTH_STATE_AUTHING;
        TEBeautyKit.setTELicense(this.getApplicationContext(),LicenseConstant.mXMagicLicenceUrl,LicenseConstant.mXMagicKey, (errorCode, msg) -> {
            if (errorCode == TELicenseCheck.ERROR_OK) {
                authState = LicenseConstant.AUTH_STATE_SUCCEED;
                Intent intent = new Intent(this, TECameraBaseActivity.class);
                startActivity(intent);
            } else {
                authState = LicenseConstant.AUTH_STATE_FAILED;
                Toast.makeText(this, "Auth failed，errorCode " + errorCode, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionHandler.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
    }

    private void copyRes() {
        if (!isNeedCopyRes()) {
            return;
        }
        mLoadingView.setVisibility(View.VISIBLE);
        new Thread(() -> {
            TEBeautyKit.copyRes(getApplicationContext());
            runOnUiThread(() -> {
                mLoadingView.setVisibility(View.GONE);
                saveCopyData();
            });
        }).start();
    }

    private boolean isNeedCopyRes() {
        String appVersionName = AppUtils.getAppVersionName(this);
        SharedPreferences sp = getSharedPreferences("demo_settings", Context.MODE_PRIVATE);
        String savedVersionName = sp.getString("resource_copied", "");
        return !savedVersionName.equals(appVersionName);
    }

    private void saveCopyData() {
        String appVersionName = AppUtils.getAppVersionName(this);
        getSharedPreferences("demo_settings", Context.MODE_PRIVATE).edit()
                .putString("resource_copied", appVersionName).commit();
    }
}
