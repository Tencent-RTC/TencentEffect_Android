package com.tencent.demo.utils;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import androidx.annotation.Keep;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PermissionInfo;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 动态权限申请工具
 */
public abstract class PermissionHandler {

    private static final String TAG = "PermissionHandler";

    private Activity mActivity;
    private int mRequestCode;

    /** 记得重写 Activity 的 onRequestPermissionsResult 方法, 把结果传入这个类的 {@link #onRequestPermissionsResult(int, String[], int[])} */
    public PermissionHandler(Activity activity) {
        mActivity = activity;
    }

    /**
     * 自动检测 App 所需权限, 并申请未获得的权限
     * 1. 重写 {@link PermissionHandler#onGetPermissions()} 方法, 手动声明需要的权限, 避免抛出异常<br>
     * 2. catch 异常后调用 {@link PermissionHandler#start(String[])} 方法以继续业务逻辑
     */
    public void start() {
        String[] permissions = onGetPermissions();
        List<String> checkPermission = new ArrayList<>();
        for (String permission:permissions){
            if(Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)){
                continue;
            }
            checkPermission.add(permission);
        }
        start(checkPermission.toArray(new String[0]));
    }

    /**
     * 申请指定的权限
     * @param permissions 需要申请的权限
     */
    public void start(String[] permissions) {
        String[] no = getNoGrantPermissions(permissions);
        if (no == null || no.length == 0) {
            onAllPermissionGranted();
        } else {
            doRequestPermissions(no);
        }
    }

    /** 把 Activity 的回调交到这里处理 */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        List<String> declinedPermissions = new ArrayList<>();
        if (requestCode == mRequestCode) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (!shouldIgnore(permissions[i])) {
                        declinedPermissions.add(permissions[i]);
                    }
                }
            }
            int declinedPermissionsCount = declinedPermissions.size();
            if (declinedPermissionsCount == 0) {
                onAllPermissionGranted();
            } else {
                onPermissionsDecline(declinedPermissions.toArray(new String[declinedPermissionsCount]));
            }
        }
    }

    protected boolean shouldIgnore(String permission) {
        return false;
    };

    /**
     * 收集需要申请的权限, 例如 {@link permission#READ_EXTERNAL_STORAGE}
     */
    @Keep
    protected String[] onGetPermissions() {
        PackageInfo info = null;
        try {
            info = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            e.printStackTrace();// not possible
        }
        if (info.requestedPermissions == null) {
            Log.w(TAG, "android.content.pm.PackageInfo.requestedPermissions == null, this app does not require any permissions?");
        }
        return info.requestedPermissions;
    }

    /** 通知用户: 被拒绝的权限, 此时可以调用 {@link #start(String[])} 再次尝试 */
    @Keep
    protected void onPermissionsDecline(String[] permissions){
        String msg = "获取权限失败: " + Arrays.toString(permissions);
        Log.w(TAG, msg);
        Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
    }

    /** 通知用户: 已经获得全部权限 */
    @Keep
    protected abstract void onAllPermissionGranted();

    /** 检查权限, 返回尚未获得的权限 */
    private String[] getNoGrantPermissions(String[] permissions) {
        if (permissions == null) {
            return new String[0];
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String p : permissions) {
            if (!hasPermission(p)) {
                missingPermissions.add(p);
            }
        }
        return missingPermissions.toArray(new String[missingPermissions.size()]);
    }

    /** 判断是否具有权限 */
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /** 发起权限申请 */
    private void doRequestPermissions(String[] permissions) {
        mRequestCode = 1024;//随便定的
        ActivityCompat.requestPermissions(mActivity, permissions, mRequestCode);
    }

}
