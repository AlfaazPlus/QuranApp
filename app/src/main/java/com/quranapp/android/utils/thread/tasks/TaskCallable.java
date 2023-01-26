package com.quranapp.android.utils.thread.tasks;

import java.util.concurrent.Callable;

public interface TaskCallable<R> extends Callable<R> {
    @Override
    R call() throws Exception;

    void preExecute();

    void onProgress(long downloaded, long total);

    void onComplete(R result);

    void onFailed(Exception e);

    void postExecute();

    boolean isDone();

    void setDone(boolean done);
}
