package com.tencent.effect.beautykit.view.panelview;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.download.TEDownloadListener;
import com.tencent.effect.beautykit.manager.TEDownloadManager;
import com.tencent.effect.beautykit.model.TEMotionDLModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;
import com.tencent.effect.beautykit.view.dialog.TEProgressDialog;

/**
 * 下载助手类，专门处理UI属性下载相关的逻辑
 * 负责管理下载进度对话框、下载监听器回调等
 */
public class TEDownloadHelper {
    
    private static final String TAG = TEDownloadHelper.class.getName();
    
    private final Context context;
    private final Handler handler;
    private TEProgressDialog progressDialog;
    
    public TEDownloadHelper(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 启动UI属性下载
     * 
     * @param uiProperty 要下载的UI属性
     * @param downloadListener 下载监听器
     */
    public void startDownload(TEUIProperty uiProperty, TEDownloadListener downloadListener) {
        TEMotionDLModel dlModel = uiProperty.dlModel;
        if (dlModel == null) {
            LogUtils.e(TAG, "please check this item : " + uiProperty.toString());
            Toast.makeText(context, "please check if the local resource file exists", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (progressDialog == null) {
            progressDialog = TEProgressDialog.createDialog(context);
        }
        progressDialog.show();
        
        String downloadTip = context.getResources().getString(R.string.te_beauty_panel_view_download_dialog_tip);
        
        TEDownloadListener internalDownloadListener = new TEDownloadListener() {
            private final String TAG = "startDownloadResource " + dlModel.getFileName();

            @Override
            public void onDownloadSuccess(String directory) {
                LogUtils.d(TAG, "onDownloadSuccess  " + directory + "   " + Thread.currentThread().getName());
                handler.post(() -> {
                    dismissDialog();
                    if (downloadListener != null) {
                        downloadListener.onDownloadSuccess(directory);
                    }
                });
            }

            @Override
            public void onDownloading(int progress) {
                LogUtils.d(TAG, "onDownloading  " + progress + "   " + Thread.currentThread().getName());
                StringBuilder builder = new StringBuilder();
                builder.append(downloadTip).append(progress).append("%");
                handler.post(() -> {
                    if (progressDialog != null) {
                        progressDialog.setMsg(builder.toString());
                    }
                    if (downloadListener != null) {
                        downloadListener.onDownloading(progress);
                    }
                });
            }

            @Override
            public void onDownloadFailed(int errorCode) {
                LogUtils.d(TAG, "onDownloadFailed  " + errorCode + "   " + Thread.currentThread().getName());
                handler.post(() -> {
                    dismissDialog();
                    Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show();
                    if (downloadListener != null) {
                        downloadListener.onDownloadFailed(errorCode);
                    }
                });
            }
        };
        
        TEDownloadManager.getInstance().download(dlModel, dlModel.getUrl().endsWith(ProviderUtils.ZIP_NAME), internalDownloadListener);
    }
    
    /**
     * 关闭进度对话框
     */
    private void dismissDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
    
    /**
     * 释放资源，避免内存泄漏
     */
    public void release() {
        dismissDialog();
    }



    public static class DefaultTEDownloadListenerImp implements TEDownloadListener {

        @Override
        public void onDownloadSuccess(String directory) {

        }

        @Override
        public void onDownloading(int progress) {

        }

        @Override
        public void onDownloadFailed(int errorCode) {

        }
    }
}