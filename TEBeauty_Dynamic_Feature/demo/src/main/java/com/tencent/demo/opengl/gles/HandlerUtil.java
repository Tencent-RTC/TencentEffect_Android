package com.tencent.demo.opengl.gles;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class HandlerUtil {

    public static void waitDone(Handler handler) {
        if (handler == null) {
            return;
        }

        final CountDownLatch waitDoneLock = new CountDownLatch(1);
        Runnable unlockRunnable = new Runnable() {
            public final void run() {
                waitDoneLock.countDown();
            }
        };

        //如果在FilterEngineFactory线程中调用getGPUInfo会导致死锁
        if (Looper.myLooper() == handler.getLooper()) {
            unlockRunnable.run();
        } else {
            handler.post(unlockRunnable);
        }
        try {
            //waitDoneLock.await(INIT_TIMEOUT_MILLS,TimeUnit.MILLISECONDS);
            waitDoneLock.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void waitDone(Handler handler, int timeoutMillis) {
        if (handler == null) {
            return;
        }

        final CountDownLatch waitDoneLock = new CountDownLatch(1);
        Runnable unlockRunnable = new Runnable() {
            public final void run() {
                waitDoneLock.countDown();
            }
        };

        //如果在FilterEngineFactory线程中调用getGPUInfo会导致死锁
        if (Looper.myLooper() == handler.getLooper()) {
            unlockRunnable.run();
        } else {
            handler.post(unlockRunnable);
        }
        try {
            waitDoneLock.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
