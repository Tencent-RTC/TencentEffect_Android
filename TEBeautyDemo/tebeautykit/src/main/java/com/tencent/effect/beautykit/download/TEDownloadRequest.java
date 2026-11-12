package com.tencent.effect.beautykit.download;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 一次下载任务的完整描述。
 * <p>
 * 支持：
 * <ul>
 *     <li>{@link #targetDir} 指定本地存放目录（绝对路径）；</li>
 *     <li>{@link #fileName} 显式指定文件名（可选，为空时由下载器根据 URL / 响应头自动解析，
 *         可正确处理无扩展名、带 Content-Disposition、带重定向的下载地址）；</li>
 *     <li>{@link #resumeEnabled} 是否开启断点续传（默认开启）；</li>
 *     <li>{@link #md5} 下载完成后的完整性校验（可选，小写十六进制）；</li>
 *     <li>{@link #headers} 自定义请求头；</li>
 *     <li>{@link #tag} 业务自定义标识，便于在回调中区分不同任务。</li>
 * </ul>
 */
public class TEDownloadRequest {

    private String url;
    private String targetDir;
    private String fileName;
    private boolean resumeEnabled = true;
    private String md5;
    private Map<String, String> headers;
    private String tag;

    private TEDownloadRequest(Builder builder) {
        this.url = builder.url;
        this.targetDir = builder.targetDir;
        this.fileName = builder.fileName;
        this.resumeEnabled = builder.resumeEnabled;
        this.md5 = builder.md5;
        this.headers = builder.headers;
        this.tag = builder.tag;
    }

    public String getUrl() {
        return url;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isResumeEnabled() {
        return resumeEnabled;
    }

    public String getMd5() {
        return md5;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getTag() {
        return tag;
    }

    public static class Builder {
        private final String url;
        private String targetDir;
        private String fileName;
        private boolean resumeEnabled = true;
        private String md5;
        private Map<String, String> headers = new HashMap<>();
        private String tag;

        public Builder(String url) {
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url must not be empty");
            }
            this.url = url.trim();
        }

        /**
         * 本地保存目录（绝对路径）。
         */
        public Builder targetDir(String targetDir) {
            this.targetDir = targetDir;
            return this;
        }

        /**
         * 显式指定保存文件名；为空则自动解析。
         */
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * 是否开启断点续传，默认 true。
         */
        public Builder resumeEnabled(boolean resumeEnabled) {
            this.resumeEnabled = resumeEnabled;
            return this;
        }

        /**
         * 下载完成后的 MD5 校验值（可选）。
         */
        public Builder md5(String md5) {
            this.md5 = md5;
            return this;
        }

        public Builder addHeader(String name, String value) {
            if (!TextUtils.isEmpty(name) && value != null) {
                this.headers.put(name, value);
            }
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public TEDownloadRequest build() {
            return new TEDownloadRequest(this);
        }
    }
}
