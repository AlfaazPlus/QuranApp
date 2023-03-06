package com.quranapp.android.utils.reader.recitation;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.app.RecitationManager;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.URLBuilder;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.Unit;

public class RecitationUtils {
    public static final String DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
            "recitations");
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
    public static String getReciterName(String slug) {
        if (slug == null) {
            return null;
        }

        RecitationModel model = RecitationManager.getModel(slug);
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
        final String audioName = String.format(Locale.ENGLISH, RECITATION_AUDIO_NAME_FORMAT_LOCAL, chapterNo, verseNo);
        return FileUtils.createPath(reciterSlug, audioName);
    }

    public static synchronized void obtainRecitationModel(
            Context ctx,
            boolean force,
            OnResultReadyCallback<RecitationModel> callback
    ) {
        String savedSlug = SPReader.getSavedRecitationSlug(ctx);

        RecitationManager.prepare(ctx, force, () -> {
            String slug = savedSlug;

            if (TextUtils.isEmpty(slug)) {
                List<RecitationModel> models = RecitationManager.getModels();
                if (models != null && models.size() > 0) {
                    slug = models.get(0).getSlug();
                }
            }

            callback.onReady(RecitationManager.getModel(slug));

            return Unit.INSTANCE;
        });
    }
}
