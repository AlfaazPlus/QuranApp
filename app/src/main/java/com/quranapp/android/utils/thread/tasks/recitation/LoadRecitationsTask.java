/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 23/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.utils.thread.tasks.recitation;

import static com.quranapp.android.components.recitation.RecitationManifest.KEY_COMMON_HOST;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_RECITERS;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_URL_INFO;
import static com.quranapp.android.utils.univ.ExceptionCause.RECITATIONS_INFO_NULL;
import static com.quranapp.android.utils.univ.ExceptionCause.REQUIRE_RECITATION_FORCE_LOAD;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quranapp.android.components.recitation.RecitationManifest;
import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.exc.NoInternetException;
import com.quranapp.android.interfaceUtils.RecitationExplorerImpl;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sp.SPReader;
import com.quranapp.android.utils.thread.tasks.SimpleDataLoaderTask;
import com.quranapp.android.utils.univ.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class LoadRecitationsTask extends SimpleDataLoaderTask {
    private final Context mCtx;
    private final RecitationExplorerImpl mExplorerImpl;
    private final FileUtils mFileUtils;
    private final List<RecitationModel> mItems = new ArrayList<>();
    private final boolean mForce;

    public LoadRecitationsTask(Context context, RecitationExplorerImpl explorerImpl, boolean force) {
        super(null);
        mCtx = context;
        mExplorerImpl = explorerImpl;
        mFileUtils = FileUtils.newInstance(context);
        mForce = force;
    }

    @Nullable
    @Override
    public String call() throws Exception {
        File storedAvailableRecitations = mFileUtils.getRecitationsManifestFile();

        String manifest;
        if (mForce) {
            manifest = loadUrl();
        } else {
            if (!storedAvailableRecitations.exists()) {
                throw new Exception(REQUIRE_RECITATION_FORCE_LOAD);
            }

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

        JSONObject object = new JSONObject(manifest);

        JSONObject urlInfo = object.getJSONObject(KEY_URL_INFO);
        String commonHost = urlInfo.getString(KEY_COMMON_HOST);
        JSONObject reciters = object.getJSONObject(KEY_RECITERS);
        JSONArray ids = reciters.names();
        if (ids == null) {
            throw new NullPointerException();
        }

        for (int i = 0, l = ids.length(); i < l; i++) {
            String id = ids.getString(i);
            JSONObject reciterObj = reciters.getJSONObject(id);
            RecitationModel model = RecitationUtils.readRecitationInfo(id, reciterObj, commonHost);
            mItems.add(model);

            if (mExplorerImpl != null) {
                if (TextUtils.isEmpty(mExplorerImpl.getSavedReciter())) {
                    mExplorerImpl.setSavedReciter(model.getSlug());
                    SPReader.setSavedRecitationSlug(mCtx, mExplorerImpl.getSavedReciter());
                    model.setChecked(true);
                } else {
                    model.setChecked(Objects.equals(mExplorerImpl.getSavedReciter(), model.getSlug()));
                }
            }
        }

        if (RecitationManifest.getInstance() != null) {
            RecitationManifest.prepareInstance(mCtx, manifest);
        }

        mFileUtils.createFile(storedAvailableRecitations);
        mFileUtils.writeToFile(storedAvailableRecitations, manifest);
        return manifest;
    }

    public String loadUrl() throws Exception {
        if (!NetworkStateReceiver.isNetworkConnected(mCtx)) {
            throw new NoInternetException();
        }
        return super.call();
    }

    @Override
    public void onComplete(String result) {
        onComplete(mItems);
    }

    protected void onComplete(@NonNull List<RecitationModel> items) {}

    public Context getContext() {
        return mCtx;
    }
}