package com.tencent.effect.beautykit.download;

import android.text.TextUtils;

import com.tencent.effect.beautykit.utils.FileUtil;
import com.tencent.effect.beautykit.utils.LogUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 默认下载器实现，基于 OkHttp。
 * <p>
 * 核心能力：
 * <ul>
 *     <li>指定本地存放目录（{@link TEDownloadRequest#getTargetDir()}）；</li>
 *     <li>断点续传：通过 {@code Range} + {@code If-Range} 实现，能正确区分
 *         {@code 200}（服务端忽略 Range / 内容已变化，需重下）、{@code 206}（续传）、
 *         {@code 416}（已完整，直接改名）三种响应；</li>
 *     <li>文件名自动解析：显式指定 &gt; {@code Content-Disposition} &gt; URL 路径带扩展名 &gt;
 *         URL 路径 + {@code Content-Type} 扩展名，兼容无扩展名地址
 *         （如 {@code https://img01.mehiya.com/img/png/id/631616143864}）与重定向地址；</li>
 *     <li>下载过程先写临时文件，完成后原子改名到目标文件，避免半成品文件；</li>
 *     <li>可选的 MD5 完整性校验；</li>
 *     <li>支持取消（{@link TEDownloadTask#cancel()}）。</li>
 * </ul>
 */
public class TEDefaultDownloader implements TEDownloader {

    private static final String TAG = "TEDefaultDownloader";
    private static final int BUFFER_SIZE = 8 * 1024;
    private static final String TEMP_PREFIX = ".te_";
    private static final String TEMP_SUFFIX = ".download";
    private static final String META_SUFFIX = ".meta";

    private final OkHttpClient client;

    public TEDefaultDownloader() {
        this(new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build());
    }

    public TEDefaultDownloader(OkHttpClient client) {
        this.client = client;
    }

    /**
     * 旧接口兼容实现：将完整文件路径拆分为目录 + 文件名，转成新请求。
     */
    @Override
    public void download(String filePath, String url, TEDownloadListener listener) {
        if (TextUtils.isEmpty(filePath) || TextUtils.isEmpty(url)) {
            safeFailed(listener, TEDownloadErrorCode.INVALID_ARGUMENT, "filePath or url is empty");
            return;
        }
        File file = new File(filePath);
        TEDownloadRequest request = new TEDownloadRequest.Builder(url)
                .targetDir(file.getParent())
                .fileName(file.getName())
                .build();
        download(request, listener);
    }

    @Override
    public TEDownloadTask download(TEDownloadRequest request, TEDownloadListener listener) {
        if (request == null || TextUtils.isEmpty(request.getUrl()) || TextUtils.isEmpty(request.getTargetDir())) {
            safeFailed(listener, TEDownloadErrorCode.INVALID_ARGUMENT, "request/url/targetDir must not be empty");
            return null;
        }

        final File dir = new File(request.getTargetDir());
        if (!dir.exists() && !dir.mkdirs()) {
            safeFailed(listener, TEDownloadErrorCode.FILE_IO_ERROR, "can not create target dir: " + request.getTargetDir());
            return null;
        }

        final TEDownloadTask task = new TEDownloadTask(request.getUrl());
        final String urlKey = hashOf(request.getUrl());
        final File tempFile = new File(dir, TEMP_PREFIX + urlKey + TEMP_SUFFIX);
        final File metaFile = new File(dir, TEMP_PREFIX + urlKey + TEMP_SUFFIX + META_SUFFIX);

        final ResumeMeta meta = readMeta(metaFile);
        final long existingLength = request.isResumeEnabled() && tempFile.exists() ? tempFile.length() : 0L;

        Request.Builder builder = new Request.Builder().url(request.getUrl());
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        if (request.isResumeEnabled()) {
            // 上次已下载完整、只是没来得及改名 → 直接改名完成
            if (meta != null && meta.totalLength > 0 && existingLength > 0 && existingLength >= meta.totalLength) {
                finishFromTemp(task, request, listener, tempFile, metaFile,
                        meta.fileName, meta.contentType);
                return task;
            }
            if (existingLength > 0) {
                builder.header("Range", "bytes=" + existingLength + "-");
                if (meta != null) {
                    String validator = !TextUtils.isEmpty(meta.etag) ? meta.etag
                            : (!TextUtils.isEmpty(meta.lastModified) ? meta.lastModified : null);
                    if (validator != null) {
                        builder.header("If-Range", validator);
                    }
                }
            }
        }

        final Call call = client.newCall(builder.build());
        task.attachCancelAction(call::cancel);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (task.isCanceled()) {
                    task.markFinished();
                    safeCanceled(listener);
                } else {
                    safeFailed(listener, TEDownloadErrorCode.NETWORK_ERROR, "network error: " + e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                InputStream is = null;
                OutputStream os = null;
                try {
                    int code = response.code();
                    if (code == 416) {
                        // 服务端判定已完整，直接改名完成
                        finishFromTemp(task, request, listener, tempFile, metaFile,
                                meta != null ? meta.fileName : null,
                                meta != null ? meta.contentType : null);
                        return;
                    }
                    if (code != 200 && code != 206) {
                        safeFailed(listener, TEDownloadErrorCode.NETWORK_ERROR, "unexpected http code: " + code);
                        return;
                    }
                    if (response.body() == null || response.body().byteStream() == null) {
                        safeFailed(listener, TEDownloadErrorCode.NETWORK_ERROR, "empty response body");
                        return;
                    }

                    boolean append = (code == 206);
                    long base = append ? existingLength : 0L;
                    long contentLength = response.body().contentLength();
                    long total = contentLength >= 0 ? base + contentLength : -1L;

                    String contentType = response.header("Content-Type");
                    String finalName = resolveFinalName(request, response);
                    writeMeta(metaFile, finalName, response.header("ETag"), response.header("Last-Modified"),
                            total > 0 ? total : 0L, contentType);

                    is = response.body().byteStream();
                    os = new FileOutputStream(tempFile, append);

                    byte[] buf = new byte[BUFFER_SIZE];
                    long downloaded = base;
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        if (task.isCanceled()) {
                            throw new IOException("canceled");
                        }
                        os.write(buf, 0, len);
                        downloaded += len;
                        safeProgress(listener, downloaded, total);
                    }
                    os.flush();
                    closeQuietly(os);
                    os = null;
                    closeQuietly(is);
                    is = null;

                    writeMeta(metaFile, finalName, response.header("ETag"), response.header("Last-Modified"),
                            downloaded, contentType);
                    finishFromTemp(task, request, listener, tempFile, metaFile, finalName, contentType);
                } catch (IOException e) {
                    if (task.isCanceled()) {
                        task.markFinished();
                        safeCanceled(listener);
                    } else {
                        safeFailed(listener, TEDownloadErrorCode.NETWORK_FILE_ERROR, "io error: " + e);
                    }
                } finally {
                    closeQuietly(os);
                    closeQuietly(is);
                    response.close();
                }
            }
        });
        return task;
    }

    /**
     * 将已下载完整的临时文件改名为最终文件，并按需做 MD5 校验。
     */
    private void finishFromTemp(TEDownloadTask task, TEDownloadRequest request, TEDownloadListener listener,
                                File tempFile, File metaFile, String resolvedName, String contentType) {
        String finalName = resolvedName;
        if (TextUtils.isEmpty(finalName)) {
            finalName = resolveFinalNameWithoutResponse(request);
        }
        if (TextUtils.isEmpty(finalName)) {
            safeFailed(listener, TEDownloadErrorCode.FILE_IO_ERROR, "can not resolve final file name");
            task.markFinished();
            return;
        }
        if (!tempFile.exists()) {
            safeFailed(listener, TEDownloadErrorCode.FILE_IO_ERROR, "temp file missing: " + tempFile.getAbsolutePath());
            task.markFinished();
            return;
        }
        File finalFile = new File(tempFile.getParentFile(), finalName);
        if (finalFile.exists() && !finalFile.delete()) {
            LogUtils.e(TAG, "delete old final file failed: " + finalFile.getAbsolutePath());
        }
        if (!tempFile.renameTo(finalFile)) {
            safeFailed(listener, TEDownloadErrorCode.RENAME_FAIL, "rename failed: " + tempFile.getAbsolutePath());
            task.markFinished();
            return;
        }
        deleteQuietly(metaFile);

        if (!TextUtils.isEmpty(request.getMd5())) {
            String actual = FileUtil.getMd5(finalFile);
            if (actual == null || !actual.equalsIgnoreCase(request.getMd5())) {
                deleteQuietly(finalFile);
                safeFailed(listener, TEDownloadErrorCode.MD5_FAIL, "md5 mismatch: " + finalFile.getAbsolutePath());
                task.markFinished();
                return;
            }
        }

        task.markFinished();
        safeSuccess(listener, new TEDownloadResult(finalFile.getAbsolutePath(), finalName, contentType));
    }

    /**
     * 无响应信息（如 416 或已完整改名场景）时的文件名兜底解析。
     */
    private String resolveFinalNameWithoutResponse(TEDownloadRequest request) {
        if (!TextUtils.isEmpty(request.getFileName())) {
            return sanitizeFileName(request.getFileName());
        }
        String fromUrl = TEDownloadUrlHelper.extractFileNameFromUrl(request.getUrl());
        if (!TextUtils.isEmpty(fromUrl)) {
            return sanitizeFileName(fromUrl);
        }
        return "download_" + hashOf(request.getUrl());
    }

    /**
     * 基于响应头解析最终文件名。
     */
    private String resolveFinalName(TEDownloadRequest request, Response response) {
        if (!TextUtils.isEmpty(request.getFileName())) {
            return sanitizeFileName(request.getFileName());
        }
        String fromCd = TEDownloadUrlHelper.parseFileNameFromContentDisposition(response.header("Content-Disposition"));
        if (!TextUtils.isEmpty(fromCd)) {
            return sanitizeFileName(fromCd);
        }
        String fromUrl = TEDownloadUrlHelper.extractFileNameFromUrl(request.getUrl());
        if (!TextUtils.isEmpty(fromUrl) && TEDownloadUrlHelper.hasExtension(fromUrl)) {
            return sanitizeFileName(fromUrl);
        }
        // 无扩展名或 path 无文件名：优先从 query 中提取有意义的名字，否则用 URL 名/哈希兜底
        String base = TEDownloadUrlHelper.extractNameFromQuery(request.getUrl());
        if (TextUtils.isEmpty(base)) {
            base = TextUtils.isEmpty(fromUrl) ? ("download_" + hashOf(request.getUrl())) : fromUrl;
        }
        String ext = TEDownloadUrlHelper.extensionFromContentType(response.header("Content-Type"));
        if (!TextUtils.isEmpty(ext) && !base.toLowerCase(Locale.US).endsWith(ext.toLowerCase(Locale.US))) {
            base = base + ext;
        }
        return sanitizeFileName(base);
    }

    private static String sanitizeFileName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        String base = name;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replace('\n', '_').replace('\r', '_').replace('\0', '_').trim();
        while (base.startsWith(".")) {
            base = base.substring(1);
        }
        return TextUtils.isEmpty(base) ? null : base;
    }

    private static String hashOf(String url) {
        String md5 = FileUtil.getMD5(url);
        return TextUtils.isEmpty(md5) ? String.valueOf(url.hashCode()) : md5;
    }

    private static class ResumeMeta {
        String fileName;
        String etag;
        String lastModified;
        long totalLength;
        String contentType;
    }

    private static ResumeMeta readMeta(File metaFile) {
        if (metaFile == null || !metaFile.exists()) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(metaFile));
            ResumeMeta meta = new ResumeMeta();
            meta.fileName = readLineSafe(reader);
            meta.etag = readLineSafe(reader);
            meta.lastModified = readLineSafe(reader);
            meta.totalLength = parseLongSafe(readLineSafe(reader));
            meta.contentType = readLineSafe(reader);
            return meta;
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(reader);
        }
    }

    private void writeMeta(File metaFile, String fileName, String etag, String lastModified,
                           long totalLength, String contentType) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(metaFile, false));
            writer.write(fileName == null ? "" : fileName);
            writer.write("\n");
            writer.write(etag == null ? "" : etag);
            writer.write("\n");
            writer.write(lastModified == null ? "" : lastModified);
            writer.write("\n");
            writer.write(String.valueOf(totalLength));
            writer.write("\n");
            writer.write(contentType == null ? "" : contentType);
            writer.write("\n");
            writer.flush();
        } catch (Exception e) {
            LogUtils.e(TAG, "writeMeta failed: " + e);
        } finally {
            closeQuietly(writer);
        }
    }

    private static String readLineSafe(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        return line == null ? "" : line;
    }

    private static long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static void safeSuccess(TEDownloadListener l, TEDownloadResult result) {
        if (l != null) {
            l.onDownloadSuccess(result);
        }
    }

    private static void safeFailed(TEDownloadListener l, int code, String msg) {
        if (l != null) {
            l.onDownloadFailed(code, msg);
        }
    }

    private static void safeProgress(TEDownloadListener l, long downloaded, long total) {
        if (l != null) {
            l.onDownloadProgress(downloaded, total);
        }
    }

    private static void safeCanceled(TEDownloadListener l) {
        if (l != null) {
            l.onDownloadCanceled();
        }
    }
}
