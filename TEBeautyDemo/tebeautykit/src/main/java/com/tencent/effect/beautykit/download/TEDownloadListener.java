package com.tencent.effect.beautykit.download;

/**
 * 下载回调。
 * <p>
 * 核心方法：{@link #onDownloadSuccess(TEDownloadResult)}、{@link #onDownloading(int)}、
 * {@link #onDownloadFailed(int)}；{@link #onDownloadProgress(long, long)}、
 * {@link #onDownloadFailed(int, String)}、{@link #onDownloadCanceled()} 提供更丰富信息并带默认实现。
 * <p>
 * 注意：回调默认运行在下载线程（后台线程），需要更新 UI 时请在实现中自行切换到主线程。
 */
public interface TEDownloadListener {

    /**
     * 下载成功，返回完整结果。
     *
     * @param result 包含最终文件路径、解析后的文件名、响应的 Content-Type
     */
    void onDownloadSuccess(TEDownloadResult result);

    /**
     * 下载进度（百分比 0~100）。
     *
     * @param progress 进度百分比
     */
    void onDownloading(int progress);

    /**
     * 下载失败。
     *
     * @param errorCode 错误码，见 {@link TEDownloadErrorCode}
     */
    void onDownloadFailed(int errorCode);

    /**
     * 下载进度（字节级）。默认实现会换算成百分比并转调 {@link #onDownloading(int)}。
     *
     * @param downloadedBytes 已下载字节数（含断点续传的已有部分）
     * @param totalBytes      总字节数，未知时为 -1
     */
    default void onDownloadProgress(long downloadedBytes, long totalBytes) {
        if (totalBytes > 0) {
            int progress = (int) (downloadedBytes * 100L / totalBytes);
            if (progress < 0) {
                progress = 0;
            }
            if (progress > 100) {
                progress = 100;
            }
            onDownloading(progress);
        }
    }

    /**
     * 下载失败（带错误描述）。默认实现转调 {@link #onDownloadFailed(int)}。
     */
    default void onDownloadFailed(int errorCode, String errorMsg) {
        onDownloadFailed(errorCode);
    }

    /**
     * 下载被取消。默认实现转调 {@link #onDownloadFailed(int)}，错误码为 {@link TEDownloadErrorCode#DOWNLOAD_CANCELED}。
     */
    default void onDownloadCanceled() {
        onDownloadFailed(TEDownloadErrorCode.DOWNLOAD_CANCELED);
    }
}
