package com.tencent.demo.download;

import android.content.Context;
import android.text.TextUtils;
import com.tencent.demo.AppConfig;
import com.tencent.demo.utils.FileUtil;
import com.tencent.demo.utils.LogUtils;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.resource.XmagicResourceUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ResDownloadUtil {
    //After download succeed, the directories are as follows:

    //data/data/your_package_name/files
    //--xmagic
    //------light_assets
    //------------default
    //------------gan
    //------------images
    //------------js
    //------------material-prebuild
    //------------models
    //------------personface
    //------------shaders
    //------------sticker3d
    //------------template.json
    //------light_material
    //------------lut
    //------MotionRes
    //--xmagic_download_assets
    //------downloaded_zip_md5_assets
    //--xmagic_download_libs
    //------liblibpag.so
    //------liblight-sdk.so
    //------libv8jni.so
    //------downloaded_zip_md5_libs

    public static final int FILE_TYPE_LIBS = 0;
    public static final int FILE_TYPE_ASSETS = 1;
    private static final String TAG = "LibDownloadUtil";
    private static final String DL_DIRECTORY_LIBS = "xmagic_download_libs";            // data/data/package_name/files/xmagic_download_libs
    private static final String DL_DIRECTORY_ASSETS_PARENT = "xmagic_download_assets"; // data/data/package_name/files/xmagic_download_assets
    private static final String DL_ZIP_FILE = "download_zip_file"; //temporary zip file，will be deleted after unzipped
    private static final String MD5_VALUE_OF_LIBS = "md5_value_of_libs"; // this file is used to record the md5 value of sdk's library
    private static final String MD5_VALUE_OF_MODELS = "md5_value_of_models"; // this file is used to record the md5 value of sdk's models

    public static boolean ENABLE_RESUME_FROM_BREAKPOINT = true;

    public static String getValidLibsDirectory(Context context, String downloadMd5Libs) {
        String directory = context.getApplicationContext().getFilesDir().getAbsolutePath()
                + File.separator + DL_DIRECTORY_LIBS;
        int libsReady = checkValidLibsExist(directory + File.separator + MD5_VALUE_OF_LIBS, downloadMd5Libs);
        if (libsReady != ResDownloadUtil.CHECK_FILE_EXIST) {
            LogUtils.w(TAG, "getValidLibsDirectory: libs not ready");
            return null;
        }
        return directory;
    }

    public static String getValidAssetsDirectory(Context context, String downloadMd5Assets) {
        String directory = context.getApplicationContext().getFilesDir().getAbsolutePath()
                + File.separator + DL_DIRECTORY_ASSETS_PARENT;
        int assetsReady = checkValidLibsExist(directory + File.separator + MD5_VALUE_OF_MODELS, downloadMd5Assets);
        if (assetsReady != ResDownloadUtil.CHECK_FILE_EXIST) {
            LogUtils.w(TAG, "getValidAssetsDirectory: assets not ready");
            return null;
        }
        return directory;
    }

    public static void checkOrDownloadFiles(Context context, int fileType, String downloadUrl, String downloadMd5, TEDownloadListener listener) {
        String directory;
        String existMd5File;
        if (fileType == FILE_TYPE_LIBS) {
            directory = context.getApplicationContext().getFilesDir().getAbsolutePath()
                    + File.separator + DL_DIRECTORY_LIBS;
            existMd5File = MD5_VALUE_OF_LIBS;
        } else if (fileType == FILE_TYPE_ASSETS) {
            directory = context.getApplicationContext().getFilesDir().getAbsolutePath()
                    + File.separator + DL_DIRECTORY_ASSETS_PARENT;
            existMd5File = MD5_VALUE_OF_MODELS;
        } else {
            return;
        }
        doCheckOrDownloadFiles(fileType, directory, existMd5File, downloadUrl, downloadMd5, listener);
    }

    private static final Object downloadLock = new Object();
    private static boolean libsIsDownding = false;
    private static boolean assetsIsDownding = false;

    private static void doCheckOrDownloadFiles(int fileType, String directory, String existMd5File, String downloadUrl, String downloadMd5, TEDownloadListener listener) {
        int fileStatus = checkValidLibsExist(directory + File.separator + existMd5File, downloadMd5);
        if (fileStatus == CHECK_FILE_EXIST) {
            LogUtils.d(TAG, "checkDownload: file exists,valid md5");
            listener.onDownloadSuccess(directory);
            return;
        }

        synchronized (downloadLock) {
            if (fileType == FILE_TYPE_LIBS) {
                if (libsIsDownding) {
                    return;
                }
                libsIsDownding = true;
            } else if (fileType == FILE_TYPE_ASSETS) {
                if (assetsIsDownding) {
                    return;
                }
                assetsIsDownding = true;
            }
        }

        //The MD5 file does not exist, or the MD5 value does not match. The entire folder needs to be deleted and downloaded again.
        if (fileType == FILE_TYPE_LIBS) {
            File dir = new File(directory);
            if (fileStatus == CHECK_FILE_MD5_INVALID) {
                FileUtil.deleteRecursive(dir);
            } else if (fileStatus == CHECK_FILE_NOT_EXIST) {
                if (ENABLE_RESUME_FROM_BREAKPOINT) {
                    LogUtils.d(TAG, "doCheckOrDownloadFiles: CHECK_FILE_NOT_EXIST,can not delete");
                } else {
                    FileUtil.deleteRecursive(dir);
                }
            }
            dir.mkdirs();

        } else if (fileType == FILE_TYPE_ASSETS) {
            File downloadDir = new File(directory);
            if (fileStatus == CHECK_FILE_MD5_INVALID) {
                FileUtil.deleteRecursive(downloadDir);
            } else if (fileStatus == CHECK_FILE_NOT_EXIST) {
                if (ENABLE_RESUME_FROM_BREAKPOINT) {
                    LogUtils.d(TAG, "doCheckOrDownloadFiles: CHECK_FILE_NOT_EXIST");
                } else {
                    //delete the file that may have been half downloaded
                    FileUtil.deleteRecursive(downloadDir);
                }
            }
            downloadDir.mkdirs();
        }

        try {
            download(downloadUrl, directory, DL_ZIP_FILE, new TEDownloadListener() {
                @Override
                public void onDownloadSuccess(String downloadedDirectory) {
                    LogUtils.d(TAG, "onDownloadSuccess");
                    boolean unzipSuccess = FileUtil.unzipFile(downloadedDirectory, DL_ZIP_FILE);
                    LogUtils.d(TAG, "onDownloadSuccess: unzipSuccess=" + unzipSuccess);
                    if (unzipSuccess) {
                        String md5 = FileUtil.getMd5(new File(downloadedDirectory, DL_ZIP_FILE));
                        if (TextUtils.isEmpty(md5)) {
                            FileUtil.deleteRecursive(new File(downloadedDirectory));
                            setDownloadFinish(fileType);
                            listener.onDownloadFailed(TEDownloadErrorCode.MD5_FAIL);
                            return;
                        }
                        if (fileType == FILE_TYPE_ASSETS) {
                            if (organizeAssetsDirectory(downloadedDirectory)) {
                                FileUtil.writeContentIntoFile(downloadedDirectory, MD5_VALUE_OF_MODELS, md5);
                                FileUtil.deleteRecursive(new File(downloadedDirectory, DL_ZIP_FILE));
                                setDownloadFinish(fileType);
                                listener.onDownloadSuccess(downloadedDirectory);
                            } else {
                                //copy文件失败了，删除整个目录
                                FileUtil.deleteRecursive(new File(downloadedDirectory));
                                setDownloadFinish(fileType);
                                listener.onDownloadFailed(TEDownloadErrorCode.FILE_IO_ERROR);
                            }
                        } else if (fileType == FILE_TYPE_LIBS) {
                            FileUtil.writeContentIntoFile(downloadedDirectory, MD5_VALUE_OF_LIBS, md5);
                            //解压成功，删除zip包
                            FileUtil.deleteRecursive(new File(downloadedDirectory, DL_ZIP_FILE));
                            setDownloadFinish(fileType);
                            listener.onDownloadSuccess(downloadedDirectory);
                        }
                    } else {
                        //解压失败，删除整个目录
                        FileUtil.deleteRecursive(new File(downloadedDirectory));
                        setDownloadFinish(fileType);
                        listener.onDownloadFailed(TEDownloadErrorCode.UNZIP_FAIL);
                    }
                }

                @Override
                public void onDownloading(int progress) {
                    LogUtils.d(TAG, "onDownloading: progress=" + progress);
                    listener.onDownloading(progress);
                }

                @Override
                public void onDownloadFailed(int errorCode) {
                    LogUtils.d(TAG, "onDownloadFailed: ");
                    setDownloadFinish(fileType);
                    listener.onDownloadFailed(errorCode);
                }

            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LogUtils.d(TAG, "doCheckOrDownloadFiles: FileNotFoundException,e=" + e.toString());
            setDownloadFinish(fileType);
            listener.onDownloadFailed(TEDownloadErrorCode.FILE_IO_ERROR);
        }
    }


    private static void setDownloadFinish(int fileType) {
        synchronized (downloadLock) {
            if (fileType == FILE_TYPE_LIBS) {
                libsIsDownding = false;
            } else if (fileType == FILE_TYPE_ASSETS) {
                assetsIsDownding = false;
            }
        }
    }

    //downloadedDirectory = DL_DIRECTORY_ASSETS
    private static boolean organizeAssetsDirectory(String downloadedDirectory) {
        for (String path : XmagicResourceUtil.AI_MODE_DIR_NAMES) {
            if (XmagicApi.addAiModeFiles(downloadedDirectory + File.separator + path, AppConfig.resPathForSDK) == -2) {
                return false;
            }
            FileUtil.deleteRecursive(new File(downloadedDirectory + File.separator + path));
        }

        if (new File(downloadedDirectory + File.separator + "lut").exists()) {
            com.tencent.xmagic.util.FileUtil.copyDir(downloadedDirectory + File.separator + "lut", AppConfig.lutFilterPath);
            FileUtil.deleteRecursive(new File(downloadedDirectory + File.separator + "lut"));
        }
        return true;
    }


    public static final int CHECK_FILE_EXIST = 0;
    public static final int CHECK_FILE_MD5_INVALID = -1;
    public static final int CHECK_FILE_NOT_EXIST = -2;

    private static int checkValidLibsExist(String existMD5File, String downloadMd5) {
        File md5File = new File(existMD5File);
        if (md5File.exists()) {
            String historyMd5 = FileUtil.readOneLineFromFile(md5File);
            if (historyMd5 != null && historyMd5.equalsIgnoreCase(downloadMd5)) {
                LogUtils.d(TAG, "checkDownload: file exists,valid md5");
                return CHECK_FILE_EXIST;
            }
            return CHECK_FILE_MD5_INVALID;
        }
        return CHECK_FILE_NOT_EXIST;
    }

    private static void download(String downloadUrl, String directory, String dlZipFile, TEDownloadListener onDownloadSuccess) throws FileNotFoundException {
        if (ENABLE_RESUME_FROM_BREAKPOINT) {
            downloadWithResumeBreakPoint(downloadUrl, directory, dlZipFile, onDownloadSuccess);
        } else {
            downloadWithoutResumeBreakPoint(downloadUrl, directory, dlZipFile, onDownloadSuccess);
        }
    }

    private static void downloadWithoutResumeBreakPoint(final String url, final String directory, final String fileName, final TEDownloadListener listener) {
        Request request = new Request.Builder().url(url).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.e(TAG, "enqueue onFailure: e=" + e.toString());
                listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_ERROR);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response == null || response.body() == null || response.body().byteStream() == null) {
                    LogUtils.e(TAG, "onResponse: null or body null");
                    listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_ERROR);
                    return;
                }
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    readData(response, is, fos);
                } catch (Exception e) {
                    LogUtils.e(TAG, "onResponse: e=" + e.toString());
                    listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_FILE_ERROR);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        LogUtils.e(TAG, "onResponse: finally close is,e=" + e.toString());
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        LogUtils.e(TAG, "onResponse: finally close fos,e=" + e.toString());
                    }
                }
            }

            private void readData(Response response, InputStream is, FileOutputStream fos) throws IOException {
                byte[] buf = new byte[2048];
                is = response.body().byteStream();
                long total = response.body().contentLength();
                LogUtils.d(TAG, "onResponse: response.body().contentLength() = " + total);
                if (total <= 0) {
                    listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_ERROR);
                    return;
                }
                fos = new FileOutputStream(new File(directory + File.separator + fileName));
                long sum = 0;
                int len;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    sum += len;
                    int progress = (int) (sum * 1.0f / total * 100);
                    if (progress < 0) {
                        progress = 0;
                    }
                    if (progress > 100) {
                        progress = 100;
                    }
                    listener.onDownloading(progress);
                }
                fos.flush();
                LogUtils.d(TAG, "onResponse: onDownloadSuccess");
                listener.onDownloadSuccess(directory);
            }
        });
    }

    private static void downloadWithResumeBreakPoint(final String url, final String directory, final String fileName, final TEDownloadListener listener) throws FileNotFoundException {
        File file = new File(directory, fileName);
        RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
        final long existFileLength = file.exists() ? file.length() : 0;
        LogUtils.d(TAG, "download: file.length=" + existFileLength);
        String range = String.format(Locale.getDefault(), "bytes=%d-", existFileLength);

        Request request = new Request.Builder()
                .url(url)
                .header("range", range)
                .build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.e(TAG, "enqueue onFailure: e=" + e.toString());
                listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_ERROR);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response == null || response.body() == null || response.body().byteStream() == null) {
                    LogUtils.e(TAG, "onResponse: null or body null");
                    listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_ERROR);
                    return;
                }
                InputStream is = null;
                try {
                    readData(accessFile, existFileLength, response, is);
                } catch (Exception e) {
                    LogUtils.e(TAG, "onResponse: e=" + e.toString());
                    listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_FILE_ERROR);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        LogUtils.e(TAG, "onResponse: finally close is,e=" + e.toString());
                    }
                }
            }

            private void readData(RandomAccessFile accessFile, long existFileLength, Response response, InputStream is) throws IOException {
                byte[] buf = new byte[2048];
                is = response.body().byteStream();
                long total = response.body().contentLength();
                LogUtils.d(TAG, "onResponse: response.body().contentLength() = " + total);
                if (total <= 0) {
                    listener.onDownloadFailed(TEDownloadErrorCode.NETWORK_ERROR);
                    return;
                }
                accessFile.seek(existFileLength);
                long sum = existFileLength;
                int len;
                total += existFileLength;
                while ((len = is.read(buf)) != -1) {
                    accessFile.write(buf, 0, len);
                    sum += len;
                    int progress = (int) (sum * 1.0f / total * 100);
                    if (progress < 0) {
                        progress = 0;
                    }
                    if (progress > 100) {
                        progress = 100;
                    }
                    listener.onDownloading(progress);
                }
                LogUtils.d(TAG, "onResponse: onDownloadSuccess");
                listener.onDownloadSuccess(directory);
            }
        });
    }


}