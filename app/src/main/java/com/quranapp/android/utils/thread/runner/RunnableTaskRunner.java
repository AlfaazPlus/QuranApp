package com.quranapp.android.utils.thread.runner;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.os.HandlerCompat;

import com.quranapp.android.utils.thread.tasks.TaskRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RunnableTaskRunner {
    public static final String TIMEOUT = "timeout";
    private final ExecutorService executorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
    private final Handler mHandler;

    private Future<?> mFuture;

    public RunnableTaskRunner() {
        this(null);
    }

    public RunnableTaskRunner(Handler handler) {
        if (handler == null) {
            mHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        } else {
            mHandler = handler;
        }
    }

    private void onComplete(TaskRunnable runnable) {
        mHandler.post(() -> {
            runnable.postExecute();
            runnable.onComplete();
            runnable.setDone(true);
        });
    }

    private void onError(TaskRunnable runnable, Exception e) {
        mHandler.post(() -> {
            runnable.postExecute();
            runnable.onFailed(e);
            runnable.setDone(true);
        });
    }


    public void runAsync(@NonNull TaskRunnable runnable) {
        mFuture = null;
        runnable.preExecute();

        new Thread(() -> {
            mFuture = executorService.submit(() -> {
                runnable.runTask();
                return null;
            });

            try {
                mFuture.get();
                onComplete(runnable);
            } catch (Exception e) {
                onError(runnable, e);
            }
        }).start();
    }

    public boolean cancel() {
        mHandler.removeCallbacksAndMessages(null);
        if (mFuture != null) {
            return mFuture.cancel(true);
        }
        return true;
    }

    /**
     * Returns {@code true} if this task completed.
     * <p>
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    public boolean isDone() {
        return mFuture == null || mFuture.isDone();
    }
}
