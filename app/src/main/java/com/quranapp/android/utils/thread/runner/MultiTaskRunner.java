package com.quranapp.android.utils.thread.runner;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import com.quranapp.android.utils.thread.tasks.TaskCallable;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiTaskRunner<KEY, TASK_RESULT, TASK extends TaskCallable<TASK_RESULT>> {
    private final ExecutorService executor = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
    private final Handler mHandler;
    private HashMap<KEY, Future<TASK_RESULT>> mFutures = new LinkedHashMap<>();

    public MultiTaskRunner() {
        this(new Handler(Looper.getMainLooper()));
    }

    public MultiTaskRunner(Handler handler) {
        mHandler = handler;
    }

    public void addTask(@NonNull KEY identifier, @NonNull TASK task) {
        executeAsync(identifier, task);
    }

    private void executeAsync(@NonNull KEY identifier, @NonNull TaskCallable<TASK_RESULT> callable) {
        callable.preExecute();

        new Thread(() -> {
            Future<TASK_RESULT> future = executor.submit(callable);
            mFutures.put(identifier, future);
            try {
                onComplete(identifier, callable, future.get());
            } catch (Exception e) {
                onError(identifier, callable, e);
            }
        }).start();
    }

    private void onComplete(@NonNull KEY identifier, TaskCallable<TASK_RESULT> callable, TASK_RESULT result) {
        mHandler.post(() -> {
            callable.onComplete(result);
            callable.postExecute();
            callable.setDone(true);
            cancel(identifier);
        });
    }

    private void onError(@NonNull KEY identifier, TaskCallable<TASK_RESULT> callable, Exception e) {
        mHandler.post(() -> {
            callable.onFailed(e);
            callable.postExecute();
            callable.setDone(true);
            cancel(identifier);
        });
    }

    public void reset() {
        cancelAll();
        mFutures = new HashMap<>();
    }

    /**
     * Destroy all requests (photos fetch request and thumbnails fetch request).
     * when the user immediately searches another query before the current request if accomplished.
     * <p>
     * Handling the {@link ConcurrentModificationException} which may be throw when the user immediately searches another query,
     * because after a new search, a new task may be added to {@link #mFutures}.
     */
    public void cancelAll() {
        try {
            mFutures.keySet().iterator().forEachRemaining(this::cancel);
        } catch (ConcurrentModificationException ignored) {}
    }

    public boolean cancel(@NonNull KEY identifier) {
        try {
            Future<?> future = mFutures.get(identifier);
            if (future != null) return future.cancel(true);
            mFutures.remove(identifier);
        } catch (ConcurrentModificationException ignored) {}
        return true;
    }

    public boolean isAllDone() {
        AtomicBoolean allDone = new AtomicBoolean(true);
        mFutures.keySet().iterator().forEachRemaining(key -> {
            Future<?> future = mFutures.get(key);
            if (future != null) {
                allDone.set(future.isDone());
            }
        });
        return allDone.get();
    }

    public boolean isDone(@NonNull KEY identifier) {
        Future<?> future = mFutures.get(identifier);
        if (future != null) return future.isDone();
        return true;
    }
}
