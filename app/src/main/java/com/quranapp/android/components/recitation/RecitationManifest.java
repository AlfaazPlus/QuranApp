/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 24/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.components.recitation;

import static com.quranapp.android.utils.univ.ExceptionCause.REQUIRE_RECITATION_FORCE_LOAD;

import android.content.Context;

import androidx.annotation.NonNull;

import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.recitation.LoadRecitationsManifestTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

public class RecitationManifest {
    public static final String KEY_URL_INFO = "url-info";
    public static final String KEY_COMMON_HOST = "common-host";
    public static final String KEY_RECITERS = "reciters";
    public static final String KEY_SLUG = "slug";
    public static final String KEY_RECITER_NAME = "reciter";
    public static final String KEY_STYLE = "style";
    public static final String KEY_URL_HOST = "url-host";
    public static final String KEY_URL_PATH = "url-path";

    private static RecitationManifest sInstance;
    private String commonHost;
    private final Map<String, RecitationModel> recitersMap = new TreeMap<>();

    public static void prepareInstance(Context ctx, boolean retryFromServer, OnResultReadyCallback<RecitationManifest> resultCallback) {
        if (sInstance != null) {
            resultCallback.onReady(sInstance);
            return;
        }

        AtomicInteger reloadAttempt = new AtomicInteger(0);
        CallableTaskRunner<String> taskRunner = new CallableTaskRunner<>();
        LoadRecitationsManifestTask loadManifestTask = new LoadRecitationsManifestTask(ctx, null) {
            @Override
            public void onComplete(String manifestData) {
                try {
                    RecitationManifest manifest = prepareInstance(ctx, manifestData);
                    resultCallback.onReady(manifest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(@NonNull Exception e) {
                super.onFailed(e);
                if (!retryFromServer || e instanceof CancellationException) {
                    return;
                }

                if (e.getMessage() != null && e.getMessage().contains(REQUIRE_RECITATION_FORCE_LOAD)) {
                    if (reloadAttempt.getAndIncrement() >= 3) {
                        return;
                    }
                    RecitationUtils.prepareRecitationsManifestUrlFBAndLoad(taskRunner, this, null);
                }
            }
        };
        taskRunner.callAsync(loadManifestTask);
    }

    public static RecitationManifest prepareInstance(Context ctx, String manifestData) throws Exception {
        RecitationManifest manifest = new RecitationManifest();
        JSONObject object = new JSONObject(manifestData);

        manifest.commonHost = object.getJSONObject("url-info").getString("common-host");

        JSONObject reciters = object.getJSONObject("reciters");
        JSONArray ids = reciters.names();
        if (ids == null) {
            throw new NullPointerException();
        }
        for (int i = 0, l = ids.length(); i < l; i++) {
            String id = ids.getString(i);
            JSONObject reciterObj = reciters.optJSONObject(id);
            if (reciterObj == null) continue;
            RecitationModel model = RecitationUtils.readRecitationInfo(id, reciterObj, manifest.commonHost);
            manifest.recitersMap.put(model.getSlug(), model);
        }

        sortReciters(manifest.recitersMap);

        sInstance = manifest;
        return sInstance;
    }

    public static void sortReciters(Map<String, RecitationModel> map) {
        List<Entry<String, RecitationModel>> list = new ArrayList<>(map.entrySet());

        for (Entry<String, RecitationModel> entry : list) {
            map.put(entry.getKey(), entry.getValue());
        }
    }

    public static RecitationManifest getInstance() {
        return sInstance;
    }

    public RecitationModel getModel(String slug) {
        return recitersMap.get(slug);
    }

    public Map<String, RecitationModel> getModels() {
        return recitersMap;
    }
}
