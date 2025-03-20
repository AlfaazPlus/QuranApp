package com.quranapp.android.utils.sharedPrefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import static com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_DEFAULT;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT;
import static com.quranapp.android.utils.reader.TranslUtils.KEY_TRANSLATIONS;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.AUDIO_OPTION_DEFAULT;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_RECITER;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_REPEAT;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_SPEED;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_TRANSLATION_RECITER;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.RECITATION_DEFAULT_VERSE_SYNC;

import com.quranapp.android.utils.reader.QuranScriptUtils;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.recitation.RecitationManager;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.reader.tafsir.TafsirManager;
import com.quranapp.android.utils.tafsir.TafsirUtils;
import com.quranapp.android.utils.univ.Keys;

import java.util.HashSet;
import java.util.Set;

/**
 * SharedPreferences utility class for Reader
 */
public abstract class SPReader {
    public static final String SP_READER = "sp_reader";

    public static final String SP_TEXT_STYLE = "sp_reader_text";
    public static final String SP_TRANSL = "sp_reader_translations";
    public static final String SP_RECITATION_OPTIONS = "sp_reader_recitation_options";
    public static final String SP_TAFSIR = "sp_reader_tafsir";
    public static final String SP_SCRIPT = "sp_reader_script";
    public static final String SP_READER_STYLE = "sp_reader_style";

    public static boolean getArabicTextEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_READER, Context.MODE_PRIVATE);
        return sp.getBoolean(Keys.READER_KEY_ARABIC_TEXT_ENABLED, true);
    }

    @SuppressLint("ApplySharedPref")
    public static void setArabicTextEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(SP_READER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Keys.READER_KEY_ARABIC_TEXT_ENABLED, enabled);
        editor.commit();
    }

    public static float getSavedTextSizeMultArabic(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_TEXT_SIZE_MULT_ARABIC)) {
            setSavedTextSizeMultArabic(context, TEXT_SIZE_MULT_AR_DEFAULT);
        }

        return sp.getFloat(KEY_TEXT_SIZE_MULT_ARABIC, TEXT_SIZE_MULT_AR_DEFAULT);
    }

    public static void setSavedTextSizeMultArabic(Context context, float sizeMult) {
        SharedPreferences sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(KEY_TEXT_SIZE_MULT_ARABIC, sizeMult);
        editor.apply();
    }

    public static float getSavedTextSizeMultTransl(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_TEXT_SIZE_MULT_TRANSL)) {
            setSavedTextSizeMultTransl(context, TEXT_SIZE_MULT_TRANSL_DEFAULT);
        }

        return sp.getFloat(KEY_TEXT_SIZE_MULT_TRANSL, TEXT_SIZE_MULT_TRANSL_DEFAULT);
    }

    public static void setSavedTextSizeMultTransl(Context context, float sizeMult) {
        SharedPreferences sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(KEY_TEXT_SIZE_MULT_TRANSL, sizeMult);
        editor.apply();
    }


    public static float getSavedTextSizeMultTafsir(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_TEXT_SIZE_MULT_TAFSIR)) {
            setSavedTextSizeMultTafsir(context, TEXT_SIZE_MULT_TAFSIR_DEFAULT);
        }

        return sp.getFloat(KEY_TEXT_SIZE_MULT_TAFSIR, TEXT_SIZE_MULT_TAFSIR_DEFAULT);
    }

    public static void setSavedTextSizeMultTafsir(Context context, float sizeMult) {
        SharedPreferences sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(KEY_TEXT_SIZE_MULT_TAFSIR, sizeMult);
        editor.apply();
    }

    public static Set<String> getSavedTranslations(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_TRANSL, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_TRANSLATIONS)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet(KEY_TRANSLATIONS, TranslUtils.defaultTranslationSlugs());
            editor.apply();
        }

        if (sp.contains(KEY_TRANSLATIONS)) {
            return new HashSet<>(sp.getStringSet(KEY_TRANSLATIONS, new HashSet<>()));
        }

        return new HashSet<>();
    }

    public static void setSavedTranslations(Context context, Set<String> translSlugsSet) {
        SharedPreferences sp = context.getSharedPreferences(SP_TRANSL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_TRANSLATIONS, new HashSet<>(translSlugsSet));
        editor.apply();
    }

    public static Set<String> addToSavedTranslations(Context context, String translSlug) {
        Set<String> savedTranslations = new HashSet<>(getSavedTranslations(context));
        savedTranslations.add(translSlug);

        SharedPreferences sp = context.getSharedPreferences(SP_TRANSL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_TRANSLATIONS, savedTranslations);
        editor.apply();

        return savedTranslations;
    }

    public static Set<String> removeFromSavedTranslations(Context context, String translSlug) {
        Set<String> savedTranslations = new HashSet<>(getSavedTranslations(context));
        savedTranslations.remove(translSlug);

        SharedPreferences sp = context.getSharedPreferences(SP_TRANSL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_TRANSLATIONS, savedTranslations);
        editor.apply();

        return savedTranslations;
    }

    public static String getSavedRecitationSlug(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        return sp.getString(KEY_RECITATION_RECITER, null);
    }

    public static void setSavedRecitationSlug(Context context, String recitation) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_RECITATION_RECITER, recitation);
        editor.apply();

        RecitationManager.setSavedRecitationSlug(recitation);
    }

    public static String getSavedRecitationTranslationSlug(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        return sp.getString(KEY_RECITATION_TRANSLATION_RECITER, null);
    }

    public static void setSavedRecitationTranslationSlug(Context context, String slug) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_RECITATION_TRANSLATION_RECITER, slug);
        editor.apply();

        RecitationManager.setSavedRecitationTranslationSlug(slug);
    }

    public static float getRecitationSpeed(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_RECITATION_SPEED)) {
            setRecitationSpeed(context, RecitationUtils.RECITATION_DEFAULT_SPEED);
        }

        return sp.getFloat(KEY_RECITATION_SPEED, RecitationUtils.RECITATION_DEFAULT_SPEED);
    }

    public static void setRecitationSpeed(Context context, float speed) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(KEY_RECITATION_SPEED, speed);
        editor.apply();
    }

    public static boolean getRecitationRepeatVerse(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_RECITATION_REPEAT)) {
            setRecitationRepeatVerse(context, RecitationUtils.RECITATION_DEFAULT_REPEAT);
        }

        return sp.getBoolean(KEY_RECITATION_REPEAT, RecitationUtils.RECITATION_DEFAULT_REPEAT);
    }

    public static void setRecitationRepeatVerse(Context context, boolean repeatVerse) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_RECITATION_REPEAT, repeatVerse);
        editor.apply();
    }

    public static boolean getRecitationContinueChapter(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER)) {
            setRecitationContinueChapter(context, RecitationUtils.RECITATION_DEFAULT_CONTINUE_CHAPTER);
        }

        return sp.getBoolean(
            RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER,
            RecitationUtils.RECITATION_DEFAULT_CONTINUE_CHAPTER
        );
    }

    public static void setRecitationContinueChapter(Context context, boolean continueChapter) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER, continueChapter);
        editor.apply();
    }

    public static boolean getRecitationScrollSync(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(RecitationUtils.KEY_RECITATION_VERSE_SYNC)) {
            setRecitationVerseSync(context, RECITATION_DEFAULT_VERSE_SYNC);
        }

        return sp.getBoolean(RecitationUtils.KEY_RECITATION_VERSE_SYNC, RECITATION_DEFAULT_VERSE_SYNC);
    }

    public static void setRecitationVerseSync(Context context, boolean sync) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(RecitationUtils.KEY_RECITATION_VERSE_SYNC, sync);
        editor.apply();
    }

    public static int getRecitationAudioOption(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(RecitationUtils.KEY_RECITATION_AUDIO_OPTION)) {
            setRecitationAudioOption(context, AUDIO_OPTION_DEFAULT);
        }

        return sp.getInt(RecitationUtils.KEY_RECITATION_AUDIO_OPTION, AUDIO_OPTION_DEFAULT);
    }

    public static void setRecitationAudioOption(Context context, int option) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(RecitationUtils.KEY_RECITATION_AUDIO_OPTION, option);
        editor.apply();
    }

    public static String getSavedScript(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_SCRIPT, Context.MODE_PRIVATE);

        if (!sp.contains(QuranScriptUtils.KEY_SCRIPT)) {
            setSavedScript(context, QuranScriptUtils.SCRIPT_DEFAULT);
        }

        return sp.getString(QuranScriptUtils.KEY_SCRIPT, QuranScriptUtils.SCRIPT_DEFAULT);
    }

    public static void setSavedScript(Context context, String font) {
        SharedPreferences sp = context.getSharedPreferences(SP_SCRIPT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(QuranScriptUtils.KEY_SCRIPT, font);
        editor.apply();
    }

    public static int getSavedReaderStyle(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE);

        if (!sp.contains(Keys.READER_KEY_READER_STYLE)) {
            setSavedReaderStyle(context, READER_STYLE_DEFAULT);
        }

        sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE);
        return sp.getInt(Keys.READER_KEY_READER_STYLE, READER_STYLE_DEFAULT);
    }

    public static void setSavedReaderStyle(Context context, int readerStyle) {
        SharedPreferences sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(Keys.READER_KEY_READER_STYLE, readerStyle);
        editor.apply();
    }

    public static String getSavedTafsirKey(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_TAFSIR, Context.MODE_PRIVATE);
        return sp.getString(TafsirUtils.KEY_TAFSIR, null);
    }

    public static void setSavedTafsirKey(Context context, String tafsirKey) {
        SharedPreferences sp = context.getSharedPreferences(SP_TAFSIR, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(TafsirUtils.KEY_TAFSIR, tafsirKey);
        editor.apply();

        TafsirManager.setSavedTafsirKey(tafsirKey);
    }
}
