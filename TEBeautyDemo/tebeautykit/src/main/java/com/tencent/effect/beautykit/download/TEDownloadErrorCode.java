package com.tencent.effect.beautykit.download;

public class TEDownloadErrorCode {
    public static final int NONE = 0;
    public static final int NETWORK_ERROR = -1;
    public static final int NETWORK_FILE_ERROR = -2;
    public static final int FILE_IO_ERROR = -3;
    public static final int UNZIP_FAIL = -4;
    public static final int MD5_FAIL = -5;
    public static final int DOWNLOADING = -6;

    public static final int RENAME_FAIL = -7;

    public static String getErrorMsg(int errorCode){
        switch (errorCode){
            case NONE:
                return "成功";
            case NETWORK_ERROR:
                return "网络错误";
            case NETWORK_FILE_ERROR:
                return "读写网络数据错误";
            case FILE_IO_ERROR:
                return "读写本地文件错误";
            case UNZIP_FAIL:
                return "解压错误";
            case MD5_FAIL:
                return "MD5计算错误";
            case DOWNLOADING:
                return "Downloading, please do not repeat";
            case RENAME_FAIL:
                return "file rename filed";
            default:
                return "其他错误";
        }
    }
}
