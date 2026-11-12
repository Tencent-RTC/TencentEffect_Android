package com.tencent.effect.beautykit.download;

/**
 * 表示一个正在进行的下载任务，主要用于取消下载。
 * <p>
 * 通过 {@link TEDownloader#download(TEDownloadRequest, TEDownloadListener)} 返回。
 */
public class TEDownloadTask {

    private final String url;
    private volatile Runnable cancelAction;
    private volatile boolean canceled = false;
    private volatile boolean finished = false;

    public TEDownloadTask(String url) {
        this.url = url;
    }

    /**
     * 由下载器内部注入真正的取消动作，避免本类与具体网络库耦合。
     */
    void attachCancelAction(Runnable cancelAction) {
        this.cancelAction = cancelAction;
    }

    void markFinished() {
        this.finished = true;
    }

    /**
     * 取消当前下载任务。已完成的下载调用此方法无副作用。
     */
    public void cancel() {
        if (finished) {
            return;
        }
        canceled = true;
        Runnable action = cancelAction;
        if (action != null) {
            action.run();
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getUrl() {
        return url;
    }
}
