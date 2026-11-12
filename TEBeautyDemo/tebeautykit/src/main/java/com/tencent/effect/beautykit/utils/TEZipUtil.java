package com.tencent.effect.beautykit.utils;

import android.text.TextUtils;

import java.io.File;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 独立的解压工具，与下载逻辑解耦。
 * <p>
 * 只负责"解压"这一件事：是否解压、解压到哪里由业务层判断与调用。
 */
public final class TEZipUtil {

    private static final String TAG = "TEZipUtil";

    private TEZipUtil() {
    }

    /**
     * 判断文件是否真的是 zip（用 {@link ZipFile} 打开做魔数探测）。
     */
    public static boolean isZipFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            return zf.entries().hasMoreElements();
        } catch (Exception e) {
            return false;
        } finally {
            closeQuietly(zf);
        }
    }

    /**
     * 根据文件名 / Content-Type 判断是否应按 zip 处理（供业务层做解压决策）。
     */
    public static boolean isZipResult(String fileName, String contentType) {
        if (!TextUtils.isEmpty(fileName)
                && fileName.toLowerCase(Locale.US).endsWith(".zip")) {
            return true;
        }
        return !TextUtils.isEmpty(contentType)
                && contentType.toLowerCase(Locale.US).contains("zip");
    }

    /**
     * 读取 zip 内部的顶层目录名（解压后的目录名由压缩包内部结构决定，而非 zip 文件名）。
     */
    public static String detectTopFolder(File zipFile, String fallback) {
        ZipFile zf = null;
        String fromFile = null;
        try {
            zf = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (TextUtils.isEmpty(name)) {
                    continue;
                }
                int slash = name.indexOf('/');
                String top = slash >= 0 ? name.substring(0, slash) : null;
                if (TextUtils.isEmpty(top) || top.startsWith("__MACOSX") || ".".equals(top)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    return top;
                }
                if (fromFile == null) {
                    fromFile = top;
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "detectTopFolder failed: " + e);
        } finally {
            closeQuietly(zf);
        }
        return fromFile != null ? fromFile : fallback;
    }

    /**
     * 解压 zip 到 {@code targetDir} 下，采用"临时目录 + 原子改名"，避免解压中途被杀留下半成品最终目录。
     *
     * @param zipFile        zip 文件
     * @param targetDir      解压目标目录（最终目录的父目录）
     * @param fallbackFolder 无法探测顶层目录名时的兜底名
     * @return 解压后的最终目录绝对路径；失败返回 null
     */
    public static String unzipTo(File zipFile, File targetDir, String fallbackFolder) {
        if (zipFile == null || !zipFile.exists() || targetDir == null) {
            return null;
        }
        String folderName = detectTopFolder(zipFile, fallbackFolder);
        File targetFolder = new File(targetDir, folderName);

        File tempDir = new File(targetDir, ".te_unzip_" + hashOf(zipFile.getName()));
        FileUtil.deleteRecursive(tempDir);
        if (!tempDir.mkdirs()) {
            return null;
        }

        File tempZip = new File(tempDir, zipFile.getName());
        boolean moved = zipFile.renameTo(tempZip);
        boolean ok = moved && FileUtil.unzipFile(tempDir.getAbsolutePath(), tempZip.getName());
        FileUtil.deleteRecursive(tempZip);

        File extracted = new File(tempDir, folderName);
        if (ok && !extracted.exists()) {
            extracted = findSingleChildDir(tempDir);
        }
        if (!ok || extracted == null || !extracted.exists()) {
            FileUtil.deleteRecursive(tempDir);
            return null;
        }

        FileUtil.deleteRecursive(targetFolder);
        if (!extracted.renameTo(targetFolder)) {
            FileUtil.deleteRecursive(targetFolder);
            if (!extracted.renameTo(targetFolder)) {
                FileUtil.deleteRecursive(tempDir);
                return null;
            }
        }
        FileUtil.deleteRecursive(tempDir);
        return targetFolder.getAbsolutePath();
    }

    private static File findSingleChildDir(File parent) {
        File[] children = parent.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (child.isDirectory() && !child.getName().startsWith("__MACOSX")) {
                return child;
            }
        }
        return null;
    }

    private static String hashOf(String value) {
        String md5 = FileUtil.getMD5(value);
        return TextUtils.isEmpty(md5) ? String.valueOf(value.hashCode()) : md5;
    }

    private static void closeQuietly(ZipFile zf) {
        if (zf != null) {
            try {
                zf.close();
            } catch (Exception ignore) {
            }
        }
    }
}
