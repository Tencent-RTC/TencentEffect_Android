package com.tencent.effect.beautykit.download;

import android.text.TextUtils;

import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 下载地址解析工具，用于从 URL / 响应头中提取文件名与扩展名。
 * <p>
 * 重点解决两类现有逻辑处理不了的问题：
 * <ol>
 *     <li>无扩展名地址，例如 {@code https://img01.mehiya.com/img/png/id/631616143864}：
 *         根据 {@code Content-Type} 补全扩展名（如 {@code .png}）；</li>
 *     <li>带 {@code ?query} / {@code #fragment} / URL 编码的地址，以及
 *         {@code Content-Disposition} 指定文件名的响应。</li>
 * </ol>
 */
public final class TEDownloadUrlHelper {

    private TEDownloadUrlHelper() {
    }

    private static final Map<String, String> MIME_TO_EXT = new HashMap<>();

    static {
        MIME_TO_EXT.put("image/png", ".png");
        MIME_TO_EXT.put("image/jpeg", ".jpg");
        MIME_TO_EXT.put("image/jpg", ".jpg");
        MIME_TO_EXT.put("image/gif", ".gif");
        MIME_TO_EXT.put("image/webp", ".webp");
        MIME_TO_EXT.put("image/bmp", ".bmp");
        MIME_TO_EXT.put("image/svg+xml", ".svg");
        MIME_TO_EXT.put("application/zip", ".zip");
        MIME_TO_EXT.put("application/x-zip-compressed", ".zip");
        MIME_TO_EXT.put("application/x-zip", ".zip");
        MIME_TO_EXT.put("application/json", ".json");
        MIME_TO_EXT.put("text/plain", ".txt");
        MIME_TO_EXT.put("video/mp4", ".mp4");
        MIME_TO_EXT.put("audio/mpeg", ".mp3");
        MIME_TO_EXT.put("application/pdf", ".pdf");
    }

    /**
     * 从 URL 的 path 部分提取文件名（解码并去掉 query/fragment）。
     *
     * @return 文件名；无法解析时返回 null
     */
    public static String extractFileNameFromUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        String path;
        try {
            path = new URL(url.trim()).getPath();
        } catch (Exception e) {
            path = stripQueryAndFragment(url);
        }
        if (TextUtils.isEmpty(path) || "/".equals(path)) {
            return null;
        }
        int idx = path.lastIndexOf('/');
        String name = idx >= 0 ? path.substring(idx + 1) : path;
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (Exception ignore) {
            // 解码失败时保留原始值
        }
        return name;
    }

    /**
     * 判断文件名是否带有合法扩展名（排除 {@code .gitignore} 这类点开头、以及以点结尾的情况）。
     */
    public static boolean hasExtension(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1;
    }

    /**
     * 当 URL path 没有可用的文件名时（如 {@code /download/file?obj=a/b/c.zip}），
     * 尝试从 query 中提取最后一个"路径型"片段作为候选名。
     * 优先返回带扩展名的片段，否则返回最后出现的路径型片段。
     *
     * @return 候选名；无则返回 null
     */
    public static String extractNameFromQuery(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        String query;
        try {
            query = new URL(url.trim()).getQuery();
        } catch (Exception e) {
            query = null;
        }
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        String best = null;
        String[] params = query.split("&");
        for (String param : params) {
            int eq = param.indexOf('=');
            String value = eq >= 0 ? param.substring(eq + 1) : param;
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            value = trimQuotes(value.trim());
            int slash = value.lastIndexOf('/');
            if (slash < 0 || slash >= value.length() - 1) {
                continue;
            }
            String seg = value.substring(slash + 1);
            try {
                seg = URLDecoder.decode(seg, "UTF-8");
            } catch (Exception ignore) {
            }
            seg = seg.trim();
            if (TextUtils.isEmpty(seg)) {
                continue;
            }
            if (hasExtension(seg)) {
                return seg;
            }
            if (best == null) {
                best = seg;
            }
        }
        return best;
    }

    /**
     * 根据 Content-Type 返回对应的扩展名（含点号），未知类型返回 null。
     */
    public static String extensionFromContentType(String contentType) {
        if (TextUtils.isEmpty(contentType)) {
            return null;
        }
        String mime = contentType.trim();
        int semi = mime.indexOf(';');
        if (semi >= 0) {
            mime = mime.substring(0, semi);
        }
        return MIME_TO_EXT.get(mime.trim().toLowerCase(Locale.US));
    }

    /**
     * 从 {@code Content-Disposition} 头中解析文件名，支持 {@code filename=} 与
     * {@code filename*=UTF-8''...} 两种形式。
     *
     * @return 文件名；无则返回 null
     */
    public static String parseFileNameFromContentDisposition(String contentDisposition) {
        if (TextUtils.isEmpty(contentDisposition)) {
            return null;
        }
        String result = parseRfc5987(contentDisposition);
        if (result != null) {
            return result;
        }
        return parsePlainFileName(contentDisposition);
    }

    private static String parseRfc5987(String value) {
        int idx = value.indexOf("filename*=");
        if (idx < 0) {
            return null;
        }
        String sub = value.substring(idx + "filename*=".length()).trim();
        int semi = sub.indexOf(';');
        if (semi >= 0) {
            sub = sub.substring(0, semi);
        }
        sub = trimQuotes(sub.trim());
        int enc = sub.indexOf("''");
        if (enc >= 0) {
            String raw = sub.substring(enc + 2);
            try {
                return URLDecoder.decode(raw, "UTF-8");
            } catch (Exception ignore) {
                return raw;
            }
        }
        return sub;
    }

    private static String parsePlainFileName(String value) {
        int idx = value.indexOf("filename=");
        if (idx < 0) {
            return null;
        }
        String sub = value.substring(idx + "filename=".length()).trim();
        int semi = sub.indexOf(';');
        if (semi >= 0) {
            sub = sub.substring(0, semi);
        }
        return trimQuotes(sub.trim());
    }

    private static String trimQuotes(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String stripQueryAndFragment(String url) {
        if (url == null) {
            return null;
        }
        int end = url.length();
        int frag = url.indexOf('#');
        if (frag >= 0) {
            end = frag;
        }
        int query = url.indexOf('?');
        if (query >= 0 && query < end) {
            end = query;
        }
        return url.substring(0, end);
    }
}
