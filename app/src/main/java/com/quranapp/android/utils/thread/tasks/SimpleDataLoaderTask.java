package com.quranapp.android.utils.thread.tasks;

import android.os.Handler;
import android.os.Looper;

import com.quranapp.android.utils.exceptions.HttpNotFoundException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class SimpleDataLoaderTask extends BaseCallableTask<String> {
    private final String mMethod;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String mUrl;

    public SimpleDataLoaderTask(String url) {
        this(url, "GET");
    }

    public SimpleDataLoaderTask(String url, String method) {
        mUrl = url;
        mMethod = method;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    @Override
    public String call() throws Exception {
        URL url = new URL(mUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(mMethod);
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setUseCaches(false);
        conn.setConnectTimeout(100000);
        conn.setReadTimeout(300000); // 5 minutes
        conn.setAllowUserInteraction(false);
        conn.connect();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new HttpNotFoundException();
        }

        final long totalLength = getContentLength(conn);

        InputStreamReader isr = new InputStreamReader(conn.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();

        char[] data = new char[1024];
        long currentLength = 0;

        int count;
        while ((count = br.read(data)) > 0) {
            currentLength += count;

            long finalCurrentLength = currentLength;
            handler.post(() -> onProgress(finalCurrentLength, totalLength));

            sb.append(data, 0, count);
        }

        br.close();
        conn.disconnect();

        return sb.toString();
    }
}


