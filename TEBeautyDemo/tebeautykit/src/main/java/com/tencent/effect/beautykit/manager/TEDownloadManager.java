package com.tencent.effect.beautykit.manager;

import android.text.TextUtils;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.download.TEDefaultDownloader;
import com.tencent.effect.beautykit.download.TEDownloadErrorCode;
import com.tencent.effect.beautykit.download.TEDownloadListener;
import com.tencent.effect.beautykit.download.TEDownloadRegistry;
import com.tencent.effect.beautykit.download.TEDownloadRequest;
import com.tencent.effect.beautykit.download.TEDownloadResult;
import com.tencent.effect.beautykit.download.TEDownloadTask;
import com.tencent.effect.beautykit.download.TEDownloadUrlHelper;
import com.tencent.effect.beautykit.download.TEDownloader;
import com.tencent.effect.beautykit.model.TEMotionDLModel;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.utils.TEZipUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * 下载管理器：只负责"下载"这件事，不负责解压。
 * <p>
 * 是否解压、解压到哪里由业务层（如 {@code TEDownloadHelper}）根据下载结果
 * （{@link TEDownloadResult} 中的文件名 / Content-Type）自行判断，并调用
 * {@link TEZipUtil} 解压。
 */
public class TEDownloadManager {

    private static final String TAG = TEDownloadManager.class.getName();

    private final Set<String> downloadingMotions = new HashSet<>();
    private final Set<String> downloadingRequests = new HashSet<>();

    private TEDownloader teDownloader = new TEDefaultDownloader();

    private TEDownloadManager() {
    }

    static class ClassHolder {
        static final TEDownloadManager DOWNLOAD_MANAGER = new TEDownloadManager();
    }

    public static TEDownloadManager getInstance() {
        return ClassHolder.DOWNLOAD_MANAGER;
    }

    public void setDownloader(TEDownloader teDownloader) {
        if (teDownloader != null) {
            this.teDownloader = teDownloader;
        }
    }

    /**
     * 通用下载接口：支持指定本地目录、断点续传、无扩展名/重定向地址等。
     *
     * @param request          下载请求
     * @param downloadListener 回调
     * @return 下载任务句柄（可用于取消）；若重复发起或参数非法则返回 null
     */
    public TEDownloadTask download(TEDownloadRequest request, TEDownloadListener downloadListener) {
        if (request == null) {
            safeFailed(downloadListener, TEDownloadErrorCode.INVALID_ARGUMENT, "request is null");
            return null;
        }
        final String key = request.getUrl() + "|" + request.getTargetDir() + "|" + request.getFileName();
        synchronized (downloadingRequests) {
            if (downloadingRequests.contains(key)) {
                LogUtils.d(TAG, "download: this request is downloading, ignore: " + key);
                safeFailed(downloadListener, TEDownloadErrorCode.DOWNLOADING, "already downloading");
                return null;
            }
            downloadingRequests.add(key);
        }

        return teDownloader.download(request, new TEDownloadListener() {
            @Override
            public void onDownloadSuccess(TEDownloadResult result) {
                removeRequestItem(key);
                if (downloadListener != null) {
                    downloadListener.onDownloadSuccess(result);
                }
            }

            @Override
            public void onDownloading(int progress) {
                if (downloadListener != null) {
                    downloadListener.onDownloading(progress);
                }
            }

            @Override
            public void onDownloadProgress(long downloadedBytes, long totalBytes) {
                if (downloadListener != null) {
                    downloadListener.onDownloadProgress(downloadedBytes, totalBytes);
                }
            }

            @Override
            public void onDownloadFailed(int errorCode) {
                removeRequestItem(key);
                if (downloadListener != null) {
                    downloadListener.onDownloadFailed(errorCode);
                }
            }

            @Override
            public void onDownloadFailed(int errorCode, String errorMsg) {
                removeRequestItem(key);
                if (downloadListener != null) {
                    downloadListener.onDownloadFailed(errorCode, errorMsg);
                }
            }

            @Override
            public void onDownloadCanceled() {
                removeRequestItem(key);
                if (downloadListener != null) {
                    downloadListener.onDownloadCanceled();
                }
            }
        });
    }

    /**
     * 素材下载（纯下载，不负责解压）：去重、创建目录、下载。
     * 结果通过 {@link TEDownloadListener#onDownloadSuccess(TEDownloadResult)} 返回，
     * 是否解压由业务层决定。
     */
    public void download(TEMotionDLModel model, TEDownloadListener downloadListener) {
        if (model == null) {
            safeFailed(downloadListener, TEDownloadErrorCode.INVALID_ARGUMENT, "model is null");
            return;
        }
        synchronized (downloadingMotions) {
            if (downloadingMotions.contains(motionKey(model))) {
                LogUtils.d(TAG, "download: this motion is downloading, ignore:" + motionKey(model));
                return;
            }
            this.addDownloadingItem(model);
        }

        String directory = resolveMaterialDirectory(model);
        File dir = new File(directory);
        if (!dir.exists() && !dir.mkdirs()) {
            safeFailed(downloadListener, TEDownloadErrorCode.FILE_IO_ERROR, "can not create dir: " + directory);
            removeDownloadingItem(model);
            return;
        }

        String fileName = model.getFileName();
        boolean nameResolved = TEDownloadUrlHelper.hasExtension(fileName);
        TEDownloadRequest request = new TEDownloadRequest.Builder(model.getUrl())
                .targetDir(directory)
                .fileName(nameResolved ? fileName : null)
                .resumeEnabled(true)
                .build();

        teDownloader.download(request, new TEDownloadListener() {
            @Override
            public void onDownloadSuccess(TEDownloadResult result) {
                removeDownloadingItem(model);
                if (result != null && !TextUtils.isEmpty(result.getFileName())) {
                    model.setFileName(result.getFileName());
                }
                if (downloadListener != null) {
                    downloadListener.onDownloadSuccess(result);
                }
            }

            @Override
            public void onDownloading(int progress) {
                if (downloadListener != null) {
                    downloadListener.onDownloading(progress);
                }
            }

            @Override
            public void onDownloadProgress(long downloadedBytes, long totalBytes) {
                if (downloadListener != null) {
                    downloadListener.onDownloadProgress(downloadedBytes, totalBytes);
                }
            }

            @Override
            public void onDownloadFailed(int errorCode) {
                onModelDownloadFailed(model, errorCode, null, downloadListener);
            }

            @Override
            public void onDownloadFailed(int errorCode, String errorMsg) {
                onModelDownloadFailed(model, errorCode, errorMsg, downloadListener);
            }

            @Override
            public void onDownloadCanceled() {
                removeDownloadingItem(model);
                if (downloadListener != null) {
                    downloadListener.onDownloadCanceled();
                }
            }
        });
    }

    /**
     * 业务层判断"下载结果是否应按 zip 解压"的辅助方法。
     */
    public static boolean isZipResult(TEDownloadResult result) {
        if (result == null) {
            return false;
        }
        if (TEZipUtil.isZipResult(result.getFileName(), result.getContentType())) {
            return true;
        }
        return TEZipUtil.isZipFile(new File(result.getFilePath()));
    }

    /**
     * 解析素材的本地存放目录（供业务层解压时使用）。
     */
    public static String resolveMaterialDirectory(TEMotionDLModel model) {
        String localDir = model.getLocalDir();
        if (model.isLocalDirAbsolute()) {
            return File.separator + localDir;
        }
        return TEBeautyKit.getResPath() + localDir;
    }

    /**
     * 记录下载结果到持久化注册表（供业务层在下载/解压完成后调用）。
     * <p>
     * 落盘到 {@code .te_download_records} 的实际内容为
     * {@code MD5(url) = 相对 getResPath() 的资源路径}：
     * 本方法内部会把 {@code absolutePath} 剥掉 {@code getResPath()} 前缀转成相对路径，
     * 再交给 {@link TEDownloadRegistry#record(String, String)} 以 {@code MD5(url)} 为 key 存储。
     *
     * @param url          下载地址（存储时取 MD5，非原文）
     * @param absolutePath 最终资源的绝对路径（存储时转为相对 getResPath() 的路径）
     */
    public void recordResolvedPath(String url, String absolutePath) {
        TEDownloadRegistry.record(url, toRelativePath(absolutePath));
    }

    /**
     * 把绝对路径转为相对 {@code getResPath()} 的路径；若不在其下则原样返回。
     */
    private String toRelativePath(String absolutePath) {
        String resPath = TEBeautyKit.getResPath();
        if (!TextUtils.isEmpty(resPath) && absolutePath != null && absolutePath.startsWith(resPath)) {
            return absolutePath.substring(resPath.length());
        }
        return absolutePath;
    }

    private void onModelDownloadFailed(TEMotionDLModel model, int errorCode, String errorMsg,
                                       TEDownloadListener downloadListener) {
        removeDownloadingItem(model);
        if (downloadListener != null) {
            if (errorMsg != null) {
                downloadListener.onDownloadFailed(errorCode, errorMsg);
            } else {
                downloadListener.onDownloadFailed(errorCode);
            }
        }
    }

    private String motionKey(TEMotionDLModel model) {
        // 注意：不能用 fileName 参与 key，下载完成后 model.fileName 会被回填为解析后的名字，
        // 若包含 fileName 会导致 add/remove 时 key 不一致而无法正确去重。
        return model.getUrl() + model.getLocalDir();
    }

    private void addDownloadingItem(TEMotionDLModel model) {
        synchronized (downloadingMotions) {
            downloadingMotions.add(motionKey(model));
        }
    }

    private void removeDownloadingItem(TEMotionDLModel model) {
        synchronized (downloadingMotions) {
            downloadingMotions.remove(motionKey(model));
        }
    }

    private void removeRequestItem(String key) {
        synchronized (downloadingRequests) {
            downloadingRequests.remove(key);
        }
    }

    private static void safeFailed(TEDownloadListener l, int code, String msg) {
        if (l != null) {
            l.onDownloadFailed(code, msg);
        }
    }
}
