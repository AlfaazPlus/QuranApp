/*
 * Created by Faisal Khan on (c) 27/8/2021.
 */

package com.quranapp.android.utils;

import java.io.IOException;
import java.io.InputStream;

public class SpeedInputStream extends InputStream {
    private static final long ONE_SECOND = 1000;
    private final int mMaxSpeed;
    private long downloadedWithinOneSecond;
    private long lastTime;

    private final InputStream inputStream;

    public SpeedInputStream(InputStream inputStream, int maxSpeed) {
        this.inputStream = inputStream;
        mMaxSpeed = maxSpeed;
        lastTime = System.currentTimeMillis();
    }

    @Override
    public int read() throws IOException {
        long currentTime;
        if (downloadedWithinOneSecond >= mMaxSpeed
            && (((currentTime = System.currentTimeMillis()) - lastTime) < ONE_SECOND)) {
            try {
                Thread.sleep(ONE_SECOND - (currentTime - lastTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            downloadedWithinOneSecond = 0;
            lastTime = System.currentTimeMillis();
        }

        int res = inputStream.read();
        if (res >= 0) {
            downloadedWithinOneSecond++;
        }
        return res;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
