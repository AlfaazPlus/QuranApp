package com.peacedesign.android.utils;

import android.os.Handler;
import android.os.Looper;

public class TaskRunner {
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void runBackgroundTask(Runnable backgroundAction, Runnable uiAction) {
        new Thread(() -> {
            backgroundAction.run();
            handler.post(uiAction);
        }).start();
    }
}
