package com.tencent.demo.beauty.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.play.core.splitcompat.SplitCompat;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.tencent.demo.FeatureManager;
import com.tencent.demo.R;

public class CheckFeatureActivity extends AppCompatActivity {
    private static final String TAG = CheckFeatureActivity.class.getName();

    private FeatureManager featureManager;
    public static final String MODULE_NAME = "TencentEffectDynamicFeature";
    private final int REQUEST_CODE = 1;

    private TextView tv_featureState;
    private Button btn_goToEffect;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        SplitCompat.installActivity(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.te_beauty_activity_feature_layout);
        tv_featureState = findViewById(R.id.textview_feature_state);
        btn_goToEffect = findViewById(R.id.btn_go_to_effect);
        btn_goToEffect.setEnabled(false);
        btn_goToEffect.setOnClickListener(v -> {
            startActivity(new Intent(this, TEMenuActivity.class));
        });
        tv_featureState.setText("Checking Dynamic Feature...");
        loadDFModule();
    }

    private void loadDFModule() {
        featureManager = new FeatureManager(getApplicationContext(), MODULE_NAME);
        featureManager.loadMoudle(new FeatureManager.FeatureStatusListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: ");
                updateFeatureState(true);
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "onError: e=" + e.getMessage());
                updateFeatureState(false);
            }

            @Override
            public void onUserConfirm(SplitInstallSessionState sessionState) {
                Log.d(TAG, "onUserConfirm: sessionState=" + sessionState.toString());
                featureManager.startConfirmationDialogForResult(sessionState, CheckFeatureActivity.this, REQUEST_CODE);
            }
        });

    }

    private void updateFeatureState(boolean success)  {
        runOnUiThread(() -> {
            if (success) {
                tv_featureState.setText("Dynamic Feature is installed!");
                btn_goToEffect.setEnabled(true);
            } else {
                tv_featureState.setText("Dynamic Feature install failed!");
                btn_goToEffect.setEnabled(false);
            }
        });

    }

}
