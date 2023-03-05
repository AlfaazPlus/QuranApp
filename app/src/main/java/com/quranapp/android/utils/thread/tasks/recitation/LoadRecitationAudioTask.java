/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 23/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.utils.thread.tasks.recitation;

import android.os.Handler;
import android.os.Looper;

import com.quranapp.android.utils.exceptions.HttpNotFoundException;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class LoadRecitationAudioTask extends BaseCallableTask<Void> {
    private final File audioFile;
    private final String urlStr;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public LoadRecitationAudioTask(File audioFile, String urlStr) {
        this.audioFile = audioFile;
        this.urlStr = urlStr;
    }

    @Override
    public Void call() throws Exception {
        URL url = new URL(urlStr);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Content-Length", "0");
        conn.setRequestProperty("Connection", "close");
        conn.setConnectTimeout(180000);
        conn.setReadTimeout(180000);
        conn.setAllowUserInteraction(false);
        conn.connect();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new HttpNotFoundException();
        }

        final long totalLength = getContentLength(conn);

        InputStream input = new BufferedInputStream(conn.getInputStream());
        OutputStream output = new FileOutputStream(audioFile);
        BufferedOutputStream out = new BufferedOutputStream(output);

        byte[] data = new byte[1024];
        long currentLength = 0;

        int count;
        while ((count = input.read(data)) > 0) {
            currentLength += count;

            long finalCurrentLength = currentLength;
            handler.post(() -> onProgress(finalCurrentLength, totalLength));

            out.write(data, 0, count);
        }

        out.flush();
        out.close();
        input.close();

        return null;
    }
}
