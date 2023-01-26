/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 23/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.utils.thread.tasks.recitation;

import static com.quranapp.android.utils.univ.ExceptionCause.RECITATIONS_INFO_NULL;
import static com.quranapp.android.utils.univ.ExceptionCause.REQUIRE_RECITATION_FORCE_LOAD;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.quranapp.android.utils.thread.tasks.SimpleDataLoaderTask;
import com.quranapp.android.utils.univ.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class LoadRecitationsManifestTask extends SimpleDataLoaderTask {
    private final Context mCtx;
    private final FileUtils mFileUtils;

    public LoadRecitationsManifestTask(Context ctx, String url) {
        super(url);
        mCtx = ctx;
        mFileUtils = FileUtils.newInstance(ctx);
    }

    @Nullable
    @Override
    public String call() throws Exception {
        File storedAvailableRecitations = mFileUtils.getRecitationsManifestFile();

        String manifest;
        if (getUrl() != null) {
            manifest = super.call();
        } else {
            try {
                manifest = mFileUtils.readFile(storedAvailableRecitations);

                if (TextUtils.isEmpty(manifest)) {
                    throw new Exception(REQUIRE_RECITATION_FORCE_LOAD);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new Exception(REQUIRE_RECITATION_FORCE_LOAD);
            }
        }

        if (TextUtils.isEmpty(manifest)) {
            throw new NullPointerException(RECITATIONS_INFO_NULL);
        }

        mFileUtils.createFile(storedAvailableRecitations);
        mFileUtils.writeToFile(storedAvailableRecitations, manifest);
        return manifest;
    }
}