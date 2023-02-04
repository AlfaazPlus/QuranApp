package com.quranapp.android.utils.reader.recitation;

import static com.quranapp.android.components.recitation.RecitationManifest.KEY_COMMON_HOST;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_PREMIUM;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_RECITERS;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_RECITER_NAME;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_SLUG;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_STYLE;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_URL_HOST;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_URL_INFO;
import static com.quranapp.android.components.recitation.RecitationManifest.KEY_URL_PATH;
import static com.quranapp.android.components.recitation.RecitationManifest.prepareInstance;
import static com.quranapp.android.utils.univ.ExceptionCause.RECITATIONS_LOAD_MAX_ATTEMPT;
import static com.quranapp.android.utils.univ.ExceptionCause.REQUIRE_RECITATION_FORCE_LOAD;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.quranapp.android.components.recitation.RecitationManifest;
import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.fb.FirebaseUtils;
import com.quranapp.android.utils.sp.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.SimpleDataLoaderTask;
import com.quranapp.android.utils.thread.tasks.recitation.LoadRecitationsManifestTask;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.URLBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecitationUtils {
    public static final String DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "recitations");
    public static final Pattern URL_CHAPTER_PATTERN = Pattern.compile("\\{chapNo:(.*?)\\}", Pattern.CASE_INSENSITIVE);
    public static final Pattern URL_VERSE_PATTERN = Pattern.compile("\\{verseNo:(.*?)\\}", Pattern.CASE_INSENSITIVE);

    public static final String KEY_RECITATION_RECITER = "key.recitation.reciter";
    public static final String KEY_RECITATION_REPEAT = "key.recitation.repeat";
    public static final String KEY_RECITATION_CONTINUE_CHAPTER = "key.recitation.continue_chapter";
    public static final String KEY_RECITATION_VERSE_SYNC = "key.recitation.verse_sync";
    public static final boolean RECITATION_DEFAULT_REPEAT = false;
    public static final boolean RECITATION_DEFAULT_CONTINUE_CHAPTER = true;
    public static final boolean RECITATION_DEFAULT_VERSE_SYNC = true;

    public static final String AVAILABLE_RECITATIONS_FILENAME = "available_recitations.json";
    public static final String RECITATION_AUDIO_NAME_FORMAT_LOCAL = "%03d-%03d.mp3";

    public static boolean isRecitationSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    @Nullable
    public static String getReciterName(String slug, RecitationManifest manifest) {
        if (slug == null || manifest == null) {
            return null;
        }

        RecitationModel model = manifest.getModel(slug);
        if (model == null) {
            return null;
        }
        return model.getReciter();
    }

    @Nullable
    public static String prepareAudioUrl(RecitationModel model, int chapterNo, int verseNo) {
        try {
            URLBuilder builder = new URLBuilder(model.getUrlHost());
            builder.setConnectionType(URLBuilder.CONNECTION_TYPE_HTTPS);

            String path = model.getUrlPath();
            Matcher matcher = URL_CHAPTER_PATTERN.matcher(path);
            while (matcher.find()) {
                String group = matcher.group(1);
                if (group != null) {
                    path = matcher.replaceFirst(String.format(group, chapterNo));
                    matcher.reset(path);
                }
            }
            matcher = URL_VERSE_PATTERN.matcher(path);
            while (matcher.find()) {
                String group = matcher.group(1);
                if (group != null) {
                    path = matcher.replaceFirst(String.format(group, verseNo));
                    matcher.reset(path);
                }
            }

            builder.addPath(path);
            return builder.getURLString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String prepareAudioPathForSpecificReciter(String reciterSlug, int chapterNo, int verseNo) {
        final String audioName = String.format(RECITATION_AUDIO_NAME_FORMAT_LOCAL, chapterNo, verseNo);
        return FileUtils.createPath(reciterSlug, audioName);
    }

    public static void prepareRecitationsManifestUrlFB(OnSuccessListener<Uri> successListener, OnFailureListener failureListener) {
        Task<Uri> downloadUrl = FirebaseUtils.storageRef()
                .child("recitations")
                .child("available_recitations_info.json")
                .getDownloadUrl();
        downloadUrl.addOnSuccessListener(successListener).addOnFailureListener(failureListener);
    }

    public static RecitationModel readRecitationInfo(String id, JSONObject reciterObj, String commonHost) throws Exception {
        String slug = reciterObj.optString(KEY_SLUG, "");
        RecitationModel recitationModel = new RecitationModel(Integer.parseInt(id), slug);

        recitationModel.setReciter(reciterObj.optString(KEY_RECITER_NAME, ""));

        if (reciterObj.has(KEY_STYLE)) {
            String style = reciterObj.getString(KEY_STYLE);
            if ("null".equalsIgnoreCase(style) || TextUtils.isEmpty(style)) {
                style = "";
            }
            recitationModel.setStyle(style);
        }
        if (reciterObj.has(KEY_URL_HOST)) {
            String host = reciterObj.optString(KEY_URL_HOST);
            if ("null".equalsIgnoreCase(host) || TextUtils.isEmpty(host)) {
                recitationModel.setUrlHost(commonHost);
            } else {
                recitationModel.setUrlHost(host);
            }
        } else {
            recitationModel.setUrlHost(commonHost);
        }

        recitationModel.setUrlPath(reciterObj.optString(KEY_URL_PATH, ""));

        return recitationModel;
    }

    public static synchronized void obtainRecitationSlug(Context ctx, boolean force, OnResultReadyCallback<String> callback,
                                                         OnResultReadyCallback<Exception> failCallback) {
        String savedSlug = SPReader.getSavedRecitationSlug(ctx);
        if (!force && RecitationManifest.getInstance() != null && !TextUtils.isEmpty(savedSlug)) {
            if (RecitationManifest.getInstance().getModel(savedSlug) != null) {
                callback.onReady(savedSlug);
                return;
            }
        }

        AtomicInteger reloadAttempt = new AtomicInteger(0);
        CallableTaskRunner<String> taskRunner = new CallableTaskRunner<>();
        LoadRecitationsManifestTask loadManifestTask = new LoadRecitationsManifestTask(ctx, null) {
            @Override
            public void onComplete(String manifest) {
                try {
                    if (RecitationManifest.getInstance() == null || RecitationManifest.getInstance().getModel(
                            savedSlug) == null || force) {
                        prepareInstance(ctx, manifest);
                    }
                    getASafeRecitationSlug(ctx, manifest, savedSlug, callback);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (e instanceof JSONException && reloadAttempt.getAndIncrement() <= 3) {
                        prepareRecitationsManifestUrlFBAndLoad(taskRunner, this, failCallback);
                    } else {
                        if (failCallback != null) {
                            failCallback.onReady(e);
                        }
                    }
                }
            }

            @Override
            public void onFailed(@NonNull Exception e) {
                super.onFailed(e);
                if (e instanceof CancellationException) {
                    if (failCallback != null) failCallback.onReady(e);
                    return;
                }

                if (e.getMessage() != null && e.getMessage().contains(REQUIRE_RECITATION_FORCE_LOAD)) {
                    if (reloadAttempt.getAndIncrement() >= 3) {
                        if (failCallback != null) failCallback.onReady(new Exception(RECITATIONS_LOAD_MAX_ATTEMPT));
                    } else {
                        prepareRecitationsManifestUrlFBAndLoad(taskRunner, this, failCallback);
                    }
                } else {
                    if (failCallback != null) failCallback.onReady(e);
                }
            }
        };

        if (force) {
            prepareRecitationsManifestUrlFBAndLoad(taskRunner, loadManifestTask, failCallback);
        } else {
            taskRunner.callAsync(loadManifestTask);
        }
    }

    public static void prepareRecitationsManifestUrlFBAndLoad(CallableTaskRunner<String> taskRunner, SimpleDataLoaderTask task, OnResultReadyCallback<Exception> failCallback) {
        prepareRecitationsManifestUrlFB(uri -> {
            task.setUrl(uri.toString());
            taskRunner.callAsync(task);
        }, e -> {
            if (failCallback != null) failCallback.onReady(e);
        });
    }

    private static void getASafeRecitationSlug(Context ctx, String manifest, String slug,
                                               OnResultReadyCallback<String> callback) throws Exception {
        JSONObject manifestObj = new JSONObject(manifest);
        String commonHost = manifestObj.getJSONObject(KEY_URL_INFO).getString(KEY_COMMON_HOST);
        JSONObject reciters = manifestObj.getJSONObject(KEY_RECITERS);

        if (TextUtils.isEmpty(slug)) {
            slug = findARecitationSlug(ctx, reciters, commonHost);
        }

        callback.onReady(slug);
    }

    private static String findARecitationSlug(Context ctx, JSONObject reciters, String commonHost) throws Exception {
        String slug = null;
        JSONArray ids = reciters.names();
        if (ids == null) {
            throw new NullPointerException();
        }


        for (int i = 0, l = ids.length(); i < l; i++) {
            if (slug != null) break;

            String id = ids.getString(i);

            JSONObject reciterObj = reciters.getJSONObject(id);

            slug = reciterObj.getString(KEY_SLUG);
        }

        SPReader.setSavedRecitationSlug(ctx, slug);
        return slug;
    }
}
