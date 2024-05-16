package com.tencent.demo.beauty.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.demo.AppConfig;
import com.tencent.demo.R;
import com.tencent.demo.constant.LicenseConstant;
import com.tencent.demo.download.ResDownloadConfig;
import com.tencent.demo.download.ResDownloadUtil;
import com.tencent.demo.download.TEDownloadListener;
import com.tencent.demo.utils.AppUtils;
import com.tencent.demo.utils.PermissionHandler;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.telicense.TELicenseCheck;
import com.tencent.xmagic.util.FileUtil;
import java.io.File;

public class TEMenuActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = TEMenuActivity.class.getName();
    private TextView textViewState;
    private TextView mLoadingView = null;
    private Button btnDownloadLib, btnDownloadModels, btnAuth, btnStartCamera;
    private String sdkLibraryDirectory;

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
        mPermissionHandler.start();

        textViewState = findViewById(R.id.tv_state);
        btnDownloadLib = findViewById(R.id.btn_download_library);
        btnDownloadModels = findViewById(R.id.btn_download_models);
        btnAuth = findViewById(R.id.btn_auth);
        btnStartCamera = findViewById(R.id.btn_start_camera);

        btnDownloadLib.setOnClickListener(this);
        btnDownloadModels.setOnClickListener(this);
        btnAuth.setOnClickListener(this);
        btnStartCamera.setOnClickListener(this);

        String resPath = new File(getFilesDir(), AppConfig.SDK_RES_DIR).getAbsolutePath();
        if (!resPath.endsWith(File.separator)) {
            resPath = resPath + File.separator;
        }
        AppConfig.resPathForSDK = resPath;
        AppConfig.lutFilterPath = resPath + "light_material/lut";
        AppConfig.motionResPath = resPath + "MotionRes";
        copyRes();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_download_library:
                checkOrDownloadLibrary();
                break;
            case R.id.btn_download_models:
                checkOrDownloadModels();
                break;
            case R.id.btn_auth:
                auth();
                break;
            case R.id.btn_start_camera:
                startCamera();
                break;
            default:
                break;
        }

    }

    private void startCamera() {
        Intent intent = new Intent(this, TECameraBaseActivity.class);
        startActivity(intent);
    }

    private void auth() {
        boolean loadResult = XmagicApi.setLibPathAndLoad(sdkLibraryDirectory);
        if (!loadResult) {
            textViewState.setText("Library load failed.");
            return;
        }
        TELicenseCheck.getInstance().setTELicense(this,LicenseConstant.mXMagicLicenceUrl,LicenseConstant.mXMagicKey, (errorCode, msg) -> {
            if (errorCode == TELicenseCheck.ERROR_OK) {
                runOnUiThread(() -> {
                    textViewState.setText("Library ready!\nSDK models ready!\nAuth succeed.");
                    btnAuth.setTextColor(Color.GREEN);
                    btnStartCamera.setEnabled(true);
                });
            } else {
                runOnUiThread(() -> {
                    textViewState.setText("Library ready!\nSDK models ready!\nAuth failed:" + errorCode);
                });
            }
        });
    }

    private void checkOrDownloadModels() {
        String validAssetsDirectory = ResDownloadUtil.getValidAssetsDirectory(this, ResDownloadConfig.DOWNLOAD_MD5_ASSETS);
        if (TextUtils.isEmpty(validAssetsDirectory)) {
            ResDownloadUtil.checkOrDownloadFiles(this, ResDownloadUtil.FILE_TYPE_ASSETS,
                    ResDownloadConfig.DOWNLOAD_URL_ASSETS,
                    ResDownloadConfig.DOWNLOAD_MD5_ASSETS, new TEDownloadListener() {
                        @Override
                        public void onDownloadSuccess(String directory) {
                            runOnUiThread(() -> {
                                textViewState.setText("Library ready!\nSDK models ready!");
                                btnDownloadModels.setTextColor(Color.GREEN);
                                btnAuth.setEnabled(true);
                            });
                        }

                        @Override
                        public void onDownloading(int progress) {
                            runOnUiThread(() -> textViewState.setText("Library ready!\nSDK models downloading:" + progress + "%"));
                        }

                        @Override
                        public void onDownloadFailed(int errorCode) {
                            runOnUiThread(() -> textViewState.setText("Library ready!\nSDK models download failed:" + errorCode));
                        }
                    });
        } else {
            textViewState.setText("Library ready!\nSDK models ready!");
            btnDownloadModels.setTextColor(Color.GREEN);
            btnAuth.setEnabled(true);
        }
    }

    private void checkOrDownloadLibrary() {
        String libraryMD5 = ResDownloadConfig.DOWNLOAD_MD5_LIBS_V8A;
        String libraryURL = ResDownloadConfig.DOWNLOAD_URL_LIBS_V8A;
        if (!isCpuV8a()) {
            libraryMD5 = ResDownloadConfig.DOWNLOAD_MD5_LIBS_V7A;
            libraryURL = ResDownloadConfig.DOWNLOAD_URL_LIBS_V7A;
        }

        String validLibsDirectory = ResDownloadUtil.getValidLibsDirectory(this, libraryMD5);
        if (validLibsDirectory == null) {
            ResDownloadUtil.checkOrDownloadFiles(this, ResDownloadUtil.FILE_TYPE_LIBS, libraryURL, libraryMD5,
                new TEDownloadListener() {
                    @Override
                    public void onDownloadSuccess(String directory) {
                        runOnUiThread(() -> {
                            sdkLibraryDirectory = directory;
                            textViewState.setText("Library ready!");
                            btnDownloadLib.setTextColor(Color.GREEN);
                            btnDownloadModels.setEnabled(true);
                        });
                    }

                    @Override
                    public void onDownloading(int progress) {
                        runOnUiThread(() -> textViewState.setText("Library downloading:" + progress + "%"));
                    }

                    @Override
                    public void onDownloadFailed(int errorCode) {
                        runOnUiThread(() -> textViewState.setText("Library download failed:" + errorCode));
                    }
                });
        } else {
            textViewState.setText("Library ready!");
            btnDownloadLib.setTextColor(Color.GREEN);
            btnDownloadModels.setEnabled(true);
            sdkLibraryDirectory = validLibsDirectory;
        }
    }

    private static boolean isCpuV8a() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String[] abis = Build.SUPPORTED_ABIS;
            for (String abi : abis) {
                if (abi.equalsIgnoreCase("arm64-v8a")) {
                    return true;
                }
            }
        } else {
            String abi = Build.CPU_ABI;
            if (abi.equalsIgnoreCase("arm64-v8a")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionHandler.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
    }

    // Copy lut filters and motion resource from assets to private directory.
    // You do not need do it everytime,just do once
    private void copyRes() {
        if (!isNeedCopyRes()) {
            return;
        }
        mLoadingView.setVisibility(View.VISIBLE);
        new Thread(() -> {
            Context context = getApplicationContext();

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
