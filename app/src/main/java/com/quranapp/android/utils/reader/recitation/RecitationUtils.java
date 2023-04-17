package com.quranapp.android.utils.reader.recitation;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.quranapp.android.R;
import com.quranapp.android.api.models.recitation.RecitationInfoBaseModel;
import com.quranapp.android.api.models.recitation.RecitationInfoModel;
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.app.AppUtils;
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
    public static final String KEY_RECITATION_TRANSLATION_RECITER = "key.recitation_translation.reciter";
    public static final String KEY_RECITATION_SPEED = "key.recitation.speed";
    public static final String KEY_RECITATION_REPEAT = "key.recitation.repeat";
    public static final String KEY_RECITATION_CONTINUE_CHAPTER = "key.recitation.continue_chapter";
    public static final String KEY_RECITATION_VERSE_SYNC = "key.recitation.verse_sync";
    public static final String KEY_RECITATION_AUDIO_OPTION = "key.recitation.option_audio";
    public static final float RECITATION_DEFAULT_SPEED = 1.0f;
    public static final boolean RECITATION_DEFAULT_REPEAT = false;
    public static final boolean RECITATION_DEFAULT_CONTINUE_CHAPTER = true;
    public static final boolean RECITATION_DEFAULT_VERSE_SYNC = true;

    public static final String AVAILABLE_RECITATIONS_FILENAME = "available_recitations.json";
    public static final String AVAILABLE_RECITATION_TRANSLATIONS_FILENAME = "available_recitation_translations.json";
    public static final String RECITATION_AUDIO_NAME_FORMAT_LOCAL = "%03d-%03d.mp3";

    public static final int AUDIO_OPTION_ONLY_ARABIC = 0;
    public static final int AUDIO_OPTION_ONLY_TRANSLATION = 1;
    public static final int AUDIO_OPTION_BOTH = 2;
    public static final int AUDIO_OPTION_DEFAULT = AUDIO_OPTION_ONLY_ARABIC;

    public static boolean isRecitationSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static int resolveAudioOptionFromId(int id) {
        if (id == R.id.audioOnlyTranslation) {
            return AUDIO_OPTION_ONLY_TRANSLATION;
        } else if (id == R.id.audioBoth) {
            return AUDIO_OPTION_BOTH;
        }

        return AUDIO_OPTION_DEFAULT;
    }

    public static int resolveAudioOptionId(Context context) {
        switch (SPReader.getRecitationAudioOption(context)) {
            case AUDIO_OPTION_ONLY_TRANSLATION:
                return R.id.audioOnlyTranslation;
            case AUDIO_OPTION_BOTH:
                return R.id.audioBoth;
            default:
                return R.id.audioOnlyArabic;
        }
    }

    public static String resolveAudioOptionText(Context context) {
        switch (SPReader.getRecitationAudioOption(context)) {
            case AUDIO_OPTION_ONLY_TRANSLATION:
                return context.getString(R.string.audioOnlyTranslation);
            case AUDIO_OPTION_BOTH:
                return context.getString(R.string.audioBothArabicTranslation);
            default:
                return context.getString(R.string.audioOnlyArabic);
        }
    }

    @Nullable
    public static String prepareRecitationAudioUrl(RecitationInfoBaseModel model, int chapterNo, int verseNo) {
        return prepareAudioUrl(model.getUrlHost(), model.getUrlPath(), chapterNo, verseNo);
    }

    @Nullable
    private static String prepareAudioUrl(String host, String path, int chapterNo, int verseNo) {
        try {
            URLBuilder builder = new URLBuilder(host);
            builder.setConnectionType(URLBuilder.CONNECTION_TYPE_HTTPS);

            Matcher matcher = URL_CHAPTER_PATTERN.matcher(path);
            while (matcher.find()) {
                String group = matcher.group(1);
                if (group != null) {
                    path = matcher.replaceFirst(String.format(Locale.ENGLISH, group, chapterNo));
                    matcher.reset(path);
                }
            }
            matcher = URL_VERSE_PATTERN.matcher(path);
            while (matcher.find()) {
                String group = matcher.group(1);
                if (group != null) {
                    path = matcher.replaceFirst(String.format(Locale.ENGLISH, group, verseNo));
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

    public static synchronized void obtainRecitationModels(
        Context ctx,
        boolean force,
        boolean forceTranslation,
        OnResultReadyCallback<Pair<RecitationInfoModel, RecitationTranslationInfoModel>> callback
    ) {
        String savedSlug = SPReader.getSavedRecitationSlug(ctx);
        String savedTranslationSlug = SPReader.getSavedRecitationTranslationSlug(ctx);
        int audioOption = SPReader.getRecitationAudioOption(ctx);

        boolean isBoth = audioOption == AUDIO_OPTION_BOTH;
        boolean isOnlyTransl = audioOption == AUDIO_OPTION_ONLY_TRANSLATION;

        if (isBoth) {
            RecitationManager.prepare(ctx, force, () -> {
                RecitationManager.prepareTranslations(ctx, forceTranslation, () -> {
                    callback.onReady(obtainRecitationModels(
                        obtainArabicRecitationSlug(ctx, savedSlug),
                        obtainTranslationRecitationSlug(ctx, savedTranslationSlug)
                    ));
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
        } else if (isOnlyTransl) {
            RecitationManager.prepareTranslations(ctx, forceTranslation, () -> {
                callback.onReady(
                    obtainRecitationModels(null, obtainTranslationRecitationSlug(ctx, savedTranslationSlug)));
                return Unit.INSTANCE;
            });
        } else {
            RecitationManager.prepare(ctx, force, () -> {
                callback.onReady(obtainRecitationModels(obtainArabicRecitationSlug(ctx, savedSlug), null));
                return Unit.INSTANCE;
            });
        }
    }

    private static Pair<RecitationInfoModel, RecitationTranslationInfoModel> obtainRecitationModels(
        String savedSlug,
        String savedTranslationSlug
    ) {
        // AUDIO_OPTION_BOTH
        if (savedSlug != null && savedTranslationSlug != null) {
            return new Pair<>(
                RecitationManager.getModel(savedSlug),
                RecitationManager.getTranslationModel(savedTranslationSlug)
            );
        }
        // AUDIO_OPTION_ONLY_TRANSLATION
        else if (savedTranslationSlug != null) {
            return new Pair<>(
                null,
                RecitationManager.getTranslationModel(savedTranslationSlug)
            );
        }
        // AUDIO_OPTION_ONLY_ARABIC
        else {
            return new Pair<>(
                RecitationManager.getModel(savedSlug),
                null
            );
        }
    }

    private static String obtainArabicRecitationSlug(Context ctx, String savedSlug) {
        String slug = savedSlug;

        if (TextUtils.isEmpty(slug)) {
            List<RecitationInfoModel> models = RecitationManager.getModels();
            if (models != null && models.size() > 0) {
                slug = models.get(0).getSlug();

                if (!TextUtils.isEmpty(slug)) {
                    SPReader.setSavedRecitationSlug(ctx, slug);
                }
            }
        }

        return slug;
    }

    private static String obtainTranslationRecitationSlug(Context ctx, String savedSlug) {
        String translationSlug = savedSlug;

        if (TextUtils.isEmpty(translationSlug)) {
            List<RecitationTranslationInfoModel> models = RecitationManager.getTranslationModels();
            if (models != null && models.size() > 0) {
                translationSlug = models.get(0).getSlug();

                if (!TextUtils.isEmpty(translationSlug)) {
                    SPReader.setSavedRecitationTranslationSlug(ctx, translationSlug);
                }
            }
        }

        return translationSlug;
    }
}
