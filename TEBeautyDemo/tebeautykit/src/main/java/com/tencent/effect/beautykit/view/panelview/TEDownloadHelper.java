package com.tencent.effect.beautykit.view.panelview;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.effect.beautykit.R;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.download.TEDownloadErrorCode;
import com.tencent.effect.beautykit.download.TEDownloadListener;
import com.tencent.effect.beautykit.download.TEDownloadResult;
import com.tencent.effect.beautykit.download.TEDownloadUrlHelper;
import com.tencent.effect.beautykit.manager.TEDownloadManager;
import com.tencent.effect.beautykit.model.TEMotionDLModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.TEZipUtil;
import com.tencent.effect.beautykit.view.dialog.TEProgressDialog;

import java.io.File;

/**
 * 下载助手类，专门处理UI属性下载相关的逻辑。
 * <p>
 * 职责边界：
 * <ul>
 *     <li>下载：委托给 {@link TEDownloadManager}；</li>
 *     <li>是否解压：由本业务层根据下载结果（文件名 / Content-Type）判断；</li>
 *     <li>解压：调用独立的 {@link TEZipUtil}。</li>
 * </ul>
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
     * @param uiProperty       要下载的UI属性
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
            public void onDownloadSuccess(TEDownloadResult result) {
                LogUtils.d(TAG, "onDownloadSuccess(result)   " + Thread.currentThread().getName());
                // 业务层决策是否解压，解压过程在后台线程完成
                String finalPath = prepareResource(dlModel, result);
                postDownloadSuccess(uiProperty, downloadListener, finalPath);
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

        TEDownloadManager.getInstance().download(dlModel, internalDownloadListener);
    }

    /**
     * 业务层决策：根据下载结果判断是否需要解压，并返回最终资源路径。
     * 在下载回调线程（后台）执行。
     */
    private String prepareResource(TEMotionDLModel dlModel, TEDownloadResult result) {
        if (result == null) {
            return null;
        }
        String filePath = result.getFilePath();
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        String finalPath = filePath;
        if (TEDownloadManager.isZipResult(result)) {
            String dir = TEDownloadManager.resolveMaterialDirectory(dlModel);
            finalPath = TEZipUtil.unzipTo(new File(filePath), new File(dir), dlModel.getFileNameNoZip());
            if (TextUtils.isEmpty(finalPath)) {
                return null; // 解压失败
            }
        }
        // P0 优化：只有"无扩展名 / 无文件名"的 URL 才需要持久化最终路径；
        // 带扩展名的 URL（xxx.zip / xxx.png）配置期即可确定路径，无需记录，避免注册表膨胀。
        String urlFileName = TEDownloadUrlHelper.extractFileNameFromUrl(dlModel.getUrl());
        if (TextUtils.isEmpty(urlFileName) || !TEDownloadUrlHelper.hasExtension(urlFileName)) {
            TEDownloadManager.getInstance().recordResolvedPath(dlModel.getUrl(), finalPath);
        }
        return finalPath;
    }

    private void postDownloadSuccess(TEUIProperty uiProperty, TEDownloadListener downloadListener, String finalPath) {
        handler.post(() -> {
            dismissDialog();
            if (TextUtils.isEmpty(finalPath)) {
                Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show();
                if (downloadListener != null) {
                    downloadListener.onDownloadFailed(TEDownloadErrorCode.UNZIP_FAIL);
                }
                return;
            }
            syncResourcePath(uiProperty, finalPath);
            if (downloadListener != null) {
                downloadListener.onDownloadSuccess(new TEDownloadResult(finalPath, null, null));
            }
        });
    }

    /**
     * 下载成功后，用真实解析出的路径回填 {@code sdkParam.resourcePath}。
     * 因为无扩展名 / 带 query 的地址，其最终文件名（或 zip 解压后的目录名）只有下载时才能确定。
     */
    private void syncResourcePath(TEUIProperty uiProperty, String resolvedPath) {
        if (uiProperty == null || uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE) {
            return;
        }
        if (uiProperty.sdkParam == null || TextUtils.isEmpty(resolvedPath)) {
            return;
        }
        String resPath = TEBeautyKit.getResPath();
        if (!TextUtils.isEmpty(resPath) && resolvedPath.startsWith(resPath)) {
            // 存储为相对 getResPath() 的路径，与配置解析阶段的约定保持一致
            uiProperty.sdkParam.resourcePath = resolvedPath.substring(resPath.length());
        } else {
            uiProperty.sdkParam.resourcePath = resolvedPath;
        }
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
        public void onDownloadSuccess(TEDownloadResult result) {

        }

        @Override
        public void onDownloading(int progress) {

        }

        @Override
        public void onDownloadFailed(int errorCode) {

        }
    }
}