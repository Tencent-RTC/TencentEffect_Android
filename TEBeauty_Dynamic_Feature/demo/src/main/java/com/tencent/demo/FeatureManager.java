package com.tencent.demo;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender.SendIntentException;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

public class FeatureManager {

    private static final String TAG = "FeatureSupport";

    private SplitInstallManager splitInstallManager;
    private String moduleName;

    private int installSession;

    private Context context;

    public FeatureManager(Context context, String moduleName) {
        this.context = context;
        this.moduleName = moduleName;

    }

    public boolean isInstalled() {
        return splitInstallManager.getInstalledModules().contains(moduleName);
    }

    public void startConfirmationDialogForResult(SplitInstallSessionState splitInstallSessionState,
            Activity currentActivity, int requestCode) {
        if (null != currentActivity && !currentActivity.isFinishing()) {
            try {
                splitInstallManager.startConfirmationDialogForResult(splitInstallSessionState, currentActivity, requestCode);
            } catch (SendIntentException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void loadMoudle(FeatureStatusListener listener) {
        splitInstallManager = SplitInstallManagerFactory.create(context);
        boolean installed = splitInstallManager.getInstalledModules().contains(moduleName);
        Log.d(TAG, "loadMoudle: " + installed);

        if (!installed) {
            splitInstallManager.registerListener(new SplitInstallStateUpdatedListener() {
                @Override
                public void onStateUpdate(@NonNull SplitInstallSessionState splitInstallSessionState) {
                    Log.d(TAG, "onStateUpdate: " + splitInstallSessionState.sessionId() + ","
                            + splitInstallSessionState.status() + "," + splitInstallSessionState.errorCode());
                    if (splitInstallSessionState.sessionId() == installSession) {
                        if (splitInstallSessionState.status() == SplitInstallSessionStatus.INSTALLED) {
                            //安装成功
                            if (listener != null) {
                                listener.onSuccess();
                            }
                        } else if (splitInstallSessionState.status()
                                == SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION) {
                            //need user confirm
                            if (listener != null) {
                                listener.onUserConfirm(splitInstallSessionState);
                            }

                        }

                    }

                }
            });

            SplitInstallRequest installRequest = SplitInstallRequest.newBuilder().addModule(moduleName).build();
            splitInstallManager.startInstall(installRequest).addOnSuccessListener(new OnSuccessListener<Integer>() {
                @Override
                public void onSuccess(Integer session) {
                    Log.d(TAG, "onSuccess: " + session);
                    installSession = session;
                }
            }).addOnCompleteListener(new OnCompleteListener<Integer>() {
                @Override
                public void onComplete(@NonNull Task<Integer> task) {
                    Log.d(TAG, "onComplete: ");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: ");
                    if (listener != null) {
                        listener.onError(e);
                    }
                }
            });
        } else {
            if (listener != null) {
                listener.onSuccess();
            }
        }

    }

    public interface FeatureStatusListener {
        void onSuccess();

        void onError(Exception e);

        void onUserConfirm(SplitInstallSessionState sessionState);
    }

}
