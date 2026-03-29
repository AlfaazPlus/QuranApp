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
    private TaskCallable<R> mCurrentTask;

    public CallableTaskRunner() {
        this(new Handler(Looper.getMainLooper()));
    }

    public CallableTaskRunner(Handler handler) {
        mHandler = handler;
    }

    public void callAsync(@NonNull TaskCallable<R> callable) {
        cancel();
        mCurrentTask = callable;
        callable.preExecute();

        mFuture = executor.submit(() -> {
            try {
                R result = callable.call();
                onComplete(callable, result);
                return result;
            } catch (Exception e) {
                onError(callable, e);
                throw e;
            }
        });
    }

    private void onComplete(TaskCallable<R> callable, R result) {
        mHandler.post(() -> {
            if (mCurrentTask != callable) return;
            callable.postExecute();
            callable.onComplete(result);
            callable.setDone(true);
        });
    }

    private void onError(TaskCallable<R> callable, Exception e) {
        mHandler.post(() -> {
            if (mCurrentTask != callable) return;
            callable.onFailed(e);
            callable.postExecute();
            callable.setDone(true);
        });
    }

    public boolean cancel() {
        mCurrentTask = null;
        if (mFuture != null) {
            boolean cancelled = mFuture.cancel(true);
            mFuture = null;
            return cancelled;
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
        return mCurrentTask == null || (mFuture != null && mFuture.isDone());
    }
}
