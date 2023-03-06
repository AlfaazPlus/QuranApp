package com.quranapp.android.utils.thread.runner;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import com.quranapp.android.utils.thread.tasks.TaskCallable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableTaskRunner<R> {
    public static final String TIMEOUT = "timeout";
    private final ExecutorService executor = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
    private final Handler mHandler;
    private Future<R> mFuture;

    public CallableTaskRunner() {
        this(new Handler(Looper.getMainLooper()));
    }

    public CallableTaskRunner(Handler handler) {
        mHandler = handler;
    }

    public void callAsync(@NonNull TaskCallable<R> callable) {
        mFuture = null;
        callable.preExecute();

        new Thread(() -> {
            mFuture = executor.submit(callable);
            try {
                onComplete(callable, mFuture.get());
            } catch (Exception e) {
                onError(callable, e);
            }
        }).start();
    }

    private void onComplete(TaskCallable<R> callable, R result) {
        mHandler.post(() -> {
            callable.postExecute();
            callable.onComplete(result);
            callable.setDone(true);
        });
    }

    private void onError(TaskCallable<R> callable, Exception e) {
        mHandler.post(() -> {
            callable.onFailed(e);
            callable.postExecute();
            callable.setDone(true);
        });
    }

    public boolean cancel() {
        if (mFuture != null) return mFuture.cancel(true);
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
