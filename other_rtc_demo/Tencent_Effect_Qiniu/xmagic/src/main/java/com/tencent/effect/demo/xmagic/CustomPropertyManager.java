package com.tencent.effect.demo.xmagic;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.effect.demo.xmagic.utils.BitmapUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CustomPropertyManager {

    private static final String TAG = CustomPropertyManager.class.getName();

    public static final int TE_CHOOSE_PHOTO_SEG_CUSTOM = 2002;
    /**
     * 兼容旧调用方，这里保留这个常量，但内部改用 MIME 数组方式打开 SAF 文件选择器
     */
    public static String PICK_CONTENT_ALL = "image/*|video/*";

    /**
     * App 私有缓存目录下用于存放从系统相册拷贝过来的素材的子目录
     */
    private static final String CUSTOM_SEG_DIR = "custom_seg";

    /**
     * 写入中的临时文件后缀。只有不带 .tmp 的正式文件存在，才认为是完整可用的缓存。
     */
    private static final String TMP_SUFFIX = ".tmp";

    private Handler handler = new Handler(Looper.getMainLooper());
    private TEUIProperty customProperty = null;


    private TEPanelView tePanelView = null;

    public void setData(TEUIProperty customProperty, TEPanelView tePanelView) {
        this.customProperty = customProperty;
        this.tePanelView = tePanelView;
    }


    public void setBeautyKit(TEBeautyKit beautyKit) {
        setEffect(beautyKit);
    }


    /**
     * 使用 SAF（ACTION_OPEN_DOCUMENT）选择图片/视频，无需任何运行时权限，
     * 拿到的 Uri 我们会自己用 ContentResolver 读取并拷贝到 App 私有目录。
     */
    public void pickMedia(Activity activity, int requestCode, String pickContent) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        // 显式申请读权限（仅对该 Uri 生效，不需要 Manifest 权限）
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(intent, requestCode);
    }


    public void onActivityResult(Context context, int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            customProperty = null;
            return;
        }
        if (data == null || data.getData() == null) {
            LogUtils.e(TAG, "the data or uri is null ");
            customProperty = null;
            return;
        }
        if (requestCode != TE_CHOOSE_PHOTO_SEG_CUSTOM) {
            return;
        }

        final Uri uri = data.getData();
        // 拷贝到私有目录这一步可能涉及 IO，放到子线程执行，避免卡主线程
        new Thread(() -> {
            String localPath = copyUriToPrivateDir(context.getApplicationContext(), uri);
            if (TextUtils.isEmpty(localPath)) {
                LogUtils.e(TAG, "copy uri to private dir failed, uri=" + uri);
                handler.post(() -> customProperty = null);
                return;
            }
            handler.post(() -> setCustomSegParam(context, localPath));
        }).start();
    }


    private void setCustomSegParam(Context context, String filePath) {
        if (customProperty != null && customProperty.sdkParam != null &&
                customProperty.sdkParam.extraInfo != null && (!TextUtils.isEmpty(filePath)) &&
                new File(filePath).exists()) {
            String lower = filePath.toLowerCase();
            if (lower.endsWith("jpg") || lower.endsWith("png") || lower.endsWith("jpeg") ||
                    lower.endsWith("webp")) {
                BitmapUtil.compressImage(context.getApplicationContext(), filePath, imgPath -> {
                    customProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_TYPE,
                            TEUIProperty.TESDKParam.EXTRA_INFO_BG_TYPE_IMG);
                    customProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH, imgPath);

                });
            } else {
                customProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_TYPE,
                        TEUIProperty.TESDKParam.EXTRA_INFO_BG_TYPE_VIDEO);
                customProperty.sdkParam.extraInfo.put(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_BG_PATH, filePath);
            }
        } else {
            customProperty = null;
        }
    }


    private void setEffect(TEBeautyKit beautyKit) {
        if (beautyKit != null && customProperty!=null && customProperty.sdkParam!=null) {
            beautyKit.setEffect(customProperty.sdkParam);
            handler.post(() -> {
                tePanelView.checkPanelViewItem(customProperty);
                customProperty = null;
            });
        }
    }

    /**
     * 把通过 SAF 选到的 Uri 内容拷贝到 App 私有缓存目录下。
     * <p>
     * 缓存策略：
     * 1) 文件名 = "seg_<源流MD5>.<ext>"，同一文件多次选择只会保留一份；
     * 2) 写入采用 ".tmp" 临时文件 + rename 的方式，保证"要么没有文件、要么是完整文件"，
     * 防止"复制到一半被杀进程"导致下次启动误用半截文件。
     *
     * @return 拷贝后落到本地的绝对路径；失败返回 null
     */
    private String copyUriToPrivateDir(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();

        // 1) 通过 ContentResolver 拿 MIME 类型，再换算后缀
        String mime = resolver.getType(uri);
        String extension = null;
        if (!TextUtils.isEmpty(mime)) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        }
        boolean isImage = mime != null && mime.startsWith("image/");

        // 2) 准备私有目录，并清理上次可能残留的 .tmp 文件
        File dir = new File(context.getCacheDir(), CUSTOM_SEG_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            LogUtils.e(TAG, "mkdirs failed: " + dir.getAbsolutePath());
            return null;
        }
        cleanupTempFiles(dir);

        // 3) 计算源流 MD5，作为稳定文件名（与时间戳无关，能跨进程命中）
        String srcMd5 = md5OfUri(resolver, uri);
        if (TextUtils.isEmpty(srcMd5)) {
            LogUtils.e(TAG, "md5OfUri failed, uri=" + uri);
            return null;
        }

        // 4) 决定后缀
        String saveExt;
        Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
        if (isImage) {
            saveExt = extension != null ? extension : "jpg";
            if ("png".equalsIgnoreCase(saveExt)) {
                format = Bitmap.CompressFormat.PNG;
            } else if ("webp".equalsIgnoreCase(saveExt)) {
                format = Bitmap.CompressFormat.WEBP;
            } else {
                saveExt = "jpg";
                format = Bitmap.CompressFormat.JPEG;
            }
        } else {
            saveExt = TextUtils.isEmpty(extension) ? "mp4" : extension;
        }

        File out = new File(dir, "seg_" + srcMd5 + "." + saveExt);

        // 5) 命中缓存：正式文件存在且非空，直接复用
        if (out.exists() && out.length() > 0) {
            LogUtils.d(TAG, "cache hit: " + out.getAbsolutePath());
            return out.getAbsolutePath();
        }

        // 6) 未命中：先写 .tmp，写入成功后再 rename 到正式名
        File tmp = new File(dir, out.getName() + TMP_SUFFIX);
        // 防止上一次失败留下的同名 tmp 干扰
        if (tmp.exists()) {
            tmp.delete();
        }

        boolean ok;
        if (isImage) {
            ok = writeImageToFile(resolver, uri, tmp, format);
        } else {
            ok = writeStreamToFile(resolver, uri, tmp);
        }
        if (!ok) {
            tmp.delete();
            return null;
        }

        // 7) 原子地把 .tmp 改成正式文件名
        if (out.exists()) {
            // 极少数并发场景：另一路径已经写好了正式文件，丢弃我们这份即可
            tmp.delete();
            return out.getAbsolutePath();
        }
        if (tmp.renameTo(out)) {
            return out.getAbsolutePath();
        }
        // rename 失败兜底：复制 + 删除
        if (copyFile(tmp, out)) {
            tmp.delete();
            return out.getAbsolutePath();
        }
        tmp.delete();
        LogUtils.e(TAG, "rename and fallback copy both failed: " + tmp.getAbsolutePath());
        return null;
    }

    /**
     * 图片：从 Uri 解码出 Bitmap 后，按指定格式写入目标文件。
     */
    private boolean writeImageToFile(ContentResolver resolver, Uri uri, File target,
                                     Bitmap.CompressFormat format) {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = resolver.openInputStream(uri);
            if (is == null) {
                return false;
            }
            bitmap = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            LogUtils.e(TAG, "decodeStream failed: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(is);
        }
        if (bitmap == null) {
            return false;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(target);
            boolean compressed = bitmap.compress(format, 100, fos);
            fos.flush();
            return compressed;
        } catch (Exception e) {
            LogUtils.e(TAG, "save bitmap failed: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(fos);
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * 视频/其它：字节流拷贝到目标文件。
     */
    private boolean writeStreamToFile(ContentResolver resolver, Uri uri, File target) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = resolver.openInputStream(uri);
            if (is == null) {
                return false;
            }
            os = new FileOutputStream(target);
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            os.flush();
            return true;
        } catch (Exception e) {
            LogUtils.e(TAG, "copy stream failed: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    /**
     * 流式计算 Uri 内容的 MD5，用作缓存文件名的稳定指纹。
     */
    private String md5OfUri(ContentResolver resolver, Uri uri) {
        InputStream is = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = resolver.openInputStream(uri);
            if (is == null) {
                return null;
            }
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                digest.update(buf, 0, len);
            }
            byte[] md = digest.digest();
            StringBuilder sb = new StringBuilder(md.length * 2);
            for (byte b : md) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LogUtils.e(TAG, "MD5 not supported: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LogUtils.e(TAG, "md5OfUri failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(is);
        }
    }

    /**
     * 兜底拷贝：当 renameTo 失败时使用（极少发生）。
     */
    private boolean copyFile(File src, File dst) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new java.io.FileInputStream(src);
            os = new FileOutputStream(dst);
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            os.flush();
            return true;
        } catch (Exception e) {
            LogUtils.e(TAG, "copyFile failed: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    /**
     * 清理目录下所有 .tmp 残留文件（一般是上一次进程被杀时留下的半成品）。
     */
    private void cleanupTempFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(TMP_SUFFIX)) {
                boolean deleted = f.delete();
                LogUtils.d(TAG, "cleanup stale tmp: " + f.getName() + " deleted=" + deleted);
            }
        }
    }

    private static void closeQuietly(@Nullable AutoCloseable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }
}
