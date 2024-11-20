package com.tencent.demo.beauty.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.demo.AppConfig;
import com.tencent.demo.R;
import com.tencent.demo.constant.LicenseConstant;
import com.tencent.demo.utils.AppUtils;
import com.tencent.demo.utils.PermissionHandler;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.telicense.TELicenseCheck;
import com.tencent.xmagic.util.FileUtil;
import java.io.File;

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
        setContentView(R.layout.te_beauty_activity_menu_layout);
        mLoadingView = findViewById(R.id.te_menu_loading_view);
        findViewById(R.id.btn_start_camera).setOnClickListener(view -> {
            authAndStartCamera(TECameraBaseActivity.class);
        });
        findViewById(R.id.btn_start_img).setOnClickListener(view -> {
            authAndStartCamera(TEImageBaseActivity.class);
        });
        ((RadioButton) findViewById(R.id.radio_normal_mode)).setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                AppConfig.isEnableDowngradePerformance = false;
            }
        });
        ((RadioButton) findViewById(R.id.radio_downgrade_mode)).setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                AppConfig.isEnableDowngradePerformance = true;
            }
        });

        mPermissionHandler.start();

        String resPath = new File(getFilesDir(), AppConfig.getInstance().getBeautyFileDirName()).getAbsolutePath();
        if (!resPath.endsWith(File.separator)) {
            resPath = resPath + File.separator;
        }
        AppConfig.resPathForSDK = resPath;
        AppConfig.lutFilterPath = resPath + "light_material/lut";
        AppConfig.motionResPath = resPath + "MotionRes";
        copyRes();
    }

    private void authAndStartCamera(Class<?> cls) {
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
        TELicenseCheck.getInstance().setTELicense(this.getApplicationContext(),LicenseConstant.mXMagicLicenceUrl,LicenseConstant.mXMagicKey, (errorCode, msg) -> {
           runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   if (errorCode == TELicenseCheck.ERROR_OK) {
                       authState = LicenseConstant.AUTH_STATE_SUCCEED;
                       Intent intent = new Intent(TEMenuActivity.this, cls);
                       startActivity(intent);
                   } else {
                       authState = LicenseConstant.AUTH_STATE_FAILED;
                       Toast.makeText(TEMenuActivity.this, "Auth failed，errorCode " + errorCode, Toast.LENGTH_LONG).show();
                   }
               }
           });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionHandler.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
    }

    // Copy model files from assets to private directory.
    // You do not need do it everytime,just do once
    private void copyRes() {
        if (!isNeedCopyRes()) {
            return;
        }
        mLoadingView.setVisibility(View.VISIBLE);
        new Thread(() -> {
            Context context = getApplicationContext();
            int addResult = XmagicApi.addAiModeFilesFromAssets(context, AppConfig.resPathForSDK);
            Log.d(TAG, "copyRes, add ai model files result = " + addResult);

            String lutDirNameInAsset = "lut";
            boolean result = FileUtil.copyAssets(context, lutDirNameInAsset, AppConfig.lutFilterPath);
            Log.d(TAG, "copyRes, copy lut, result = " + result);

            String motionResDirNameInAsset = "MotionRes";
            boolean result2 = FileUtil.copyAssets(context, motionResDirNameInAsset, AppConfig.motionResPath);
            Log.d(TAG, "copyRes, copy motion res, result = " + result2);

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
