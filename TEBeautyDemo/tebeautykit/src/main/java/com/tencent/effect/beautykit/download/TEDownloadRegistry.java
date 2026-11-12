package com.tencent.effect.beautykit.download;

import android.text.TextUtils;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.utils.FileUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 下载地址 → 最终本地资源路径 的持久化记录。
 * <p>
 * 对于"无法从 URL 解析出带扩展名文件名"的地址（如
 * {@code https://img01.mehiya.com/download/file?obj=...}），其最终文件名（或 zip 解压后的目录名）
 * 只有在下载时才能确定。为了让下次启动后 {@code isNeedDownload} 判断、以及 SDK 的资源路径
 * 都能拿到正确的路径，下载完成后把结果记录到本注册表，配置解析阶段再据此恢复。
 */
public final class TEDownloadRegistry {

    private static final String FILE_NAME = ".te_download_records";
    private static final Object LOCK = new Object();

    private TEDownloadRegistry() {
    }

    /**
     * 记录下载结果。落盘格式为 {@code MD5(url)=relativeResourcePath}：
     * <ul>
     *     <li>key 是 {@code url} 的 MD5 值，而非 url 原文；</li>
     *     <li>value 是相对 {@code TEBeautyKit.getResPath()} 的资源路径。</li>
     * </ul>
     */
    public static void record(String url, String relativeResourcePath) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(relativeResourcePath)) {
            return;
        }
        synchronized (LOCK) {
            File file = recordFile();
            if (file == null) {
                return;
            }
            Map<String, String> map = readAll(file);
            map.put(keyOf(url), relativeResourcePath);
            writeAll(file, map);
        }
    }

    /**
     * 查询 url 对应的已记录资源路径（相对 {@code TEBeautyKit.getResPath()}），无记录返回 null。
     * 内部同样以 {@code MD5(url)} 作为 key 查找。
     */
    public static String query(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        synchronized (LOCK) {
            File file = recordFile();
            if (file == null) {
                return null;
            }
            return readAll(file).get(keyOf(url));
        }
    }

    private static File recordFile() {
        String resPath = TEBeautyKit.getResPath();
        if (TextUtils.isEmpty(resPath)) {
            return null;
        }
        return new File(resPath, FILE_NAME);
    }

    /**
     * 用 url 的 MD5 作为存储 key：避免 url 中的 {@code = : # ? &} 等特殊字符破坏
     * {@code key=value} 的行格式，同时得到定长、无特殊字符的 key。
     */
    private static String keyOf(String url) {
        String md5 = FileUtil.getMD5(url);
        return TextUtils.isEmpty(md5) ? String.valueOf(url.hashCode()) : md5;
    }

    private static Map<String, String> readAll(File file) {
        Map<String, String> map = new LinkedHashMap<>();
        if (file == null || !file.exists()) {
            return map;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx > 0 && idx < line.length() - 1) {
                    map.put(line.substring(0, idx), line.substring(idx + 1));
                }
            }
        } catch (Exception ignore) {
        } finally {
            closeQuietly(reader);
        }
        return map;
    }

    private static void writeAll(File file, Map<String, String> map) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file, false));
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writer.write(entry.getKey());
                writer.write('=');
                writer.write(entry.getValue());
                writer.write('\n');
            }
            writer.flush();
        } catch (Exception ignore) {
        } finally {
            closeQuietly(writer);
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }
}
