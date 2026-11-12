package com.tencent.effect.beautykit.download;

/**
 * 下载器抽象。
 * <p>
 * 旧接口 {@link #download(String, String, TEDownloadListener)} 保留以兼容历史调用；
 * 推荐使用 {@link #download(TEDownloadRequest, TEDownloadListener)}，它支持：
 * 指定本地目录、断点续传、无扩展名/重定向地址、MD5 校验以及取消。
 */
public interface TEDownloader {

    /**
     * 旧版下载接口（兼容保留）。
     *
     * @param filePath         下载文件完整路径（目录 + 文件名）
     * @param url              下载地址
     * @param downloadListener 回调
     * @deprecated 请使用 {@link #download(TEDownloadRequest, TEDownloadListener)}
     */
    @Deprecated
    void download(String filePath, String url, TEDownloadListener downloadListener);

    /**
     * 发起一次下载。
     *
     * @param request          下载请求
     * @param downloadListener 回调
     * @return 下载任务句柄，可用于取消；实现若不支持取消可返回 null
     */
    default TEDownloadTask download(TEDownloadRequest request, TEDownloadListener downloadListener) {
        if (request == null) {
            return null;
        }
        String filePath = request.getTargetDir() + java.io.File.separator + request.getFileName();
        download(filePath, request.getUrl(), downloadListener);
        return null;
    }
}
