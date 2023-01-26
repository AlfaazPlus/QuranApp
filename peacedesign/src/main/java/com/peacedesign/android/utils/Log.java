package com.peacedesign.android.utils;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;

@SuppressLint("LogConditional")
public abstract class Log {
    public static final String TAG = "QuranAppLogs";

    public static void d(@Nullable Object... msgs) {
        StringBuilder sb = new StringBuilder();

        StackTraceElement trc = Thread.currentThread().getStackTrace()[3];
        String className = trc.getClassName();
        className = className.substring(className.lastIndexOf(".") + 1);
        sb.append("(").append(className).append("=>").append(trc.getMethodName()).append(":").append(trc.getLineNumber()).append(
                "): ");

        if (msgs != null) {
            int len = msgs.length;
            for (int i = 0; i < len; i++) {
                Object msg = msgs[i];
                if (msg != null) sb.append(msg.toString());
                else sb.append("null");
                if (i < len - 1) sb.append(", ");
            }
        } else sb.append("null");
        android.util.Log.d(TAG, sb.toString());
    }
}
