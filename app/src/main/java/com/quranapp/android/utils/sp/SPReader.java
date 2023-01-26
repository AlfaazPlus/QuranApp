package com.quranapp.android.utils.sp;

import static com.quranapp.android.readerhandler.ReaderParams.READER_STYLE_DEFAULT;
import static com.quranapp.android.utils.reader.ScriptUtils.KEY_SCRIPT;
import static com.quranapp.android.utils.reader.ScriptUtils.SCRIPT_DEFAULT;
import static com.quranapp.android.utils.reader.TextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC;
import static com.quranapp.android.utils.reader.TextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL;
import static com.quranapp.android.utils.reader.TextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT;
import static com.quranapp.android.utils.reader.TextSizeUtils.TEXT_SIZE_MULT_TRANS_DEFAULT;
import static com.quranapp.android.utils.reader.TranslUtils.KEY_TRANSLATIONS;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_RECITER;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_REPEAT;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.KEY_RECITATION_VERSE_SYNC;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.RECITATION_DEFAULT_CONTINUE_CHAPTER;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.RECITATION_DEFAULT_REPEAT;
import static com.quranapp.android.utils.reader.recitation.RecitationUtils.RECITATION_DEFAULT_VERSE_SYNC;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_READER_STYLE;

import android.content.Context;
import android.content.SharedPreferences;

import com.quranapp.android.utils.reader.TranslUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * SharedPreferences utility class for Reader
 */
public abstract class SPReader {
    public static final String SP_TEXT_STYLE = "sp_reader_text";
    public static final String SP_TRANSL = "sp_reader_translations";
    public static final String SP_RECITATION_OPTIONS = "sp_reader_recitation_options";
    public static final String SP_SCRIPT = "sp_reader_script";
    public static final String SP_READER_STYLE = "sp_reader_style";

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
            setSavedTextSizeMultTransl(context, TEXT_SIZE_MULT_TRANS_DEFAULT);
        }

        return sp.getFloat(KEY_TEXT_SIZE_MULT_TRANSL, TEXT_SIZE_MULT_TRANS_DEFAULT);
    }

    public static void setSavedTextSizeMultTransl(Context context, float sizeMult) {
        SharedPreferences sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(KEY_TEXT_SIZE_MULT_TRANSL, sizeMult);
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
        return sp.getString(KEY_RECITATION_RECITER, "");
    }

    public static void setSavedRecitationSlug(Context context, String recitation) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_RECITATION_RECITER, recitation);
        editor.apply();
    }

    public static boolean getRecitationRepeatVerse(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_RECITATION_REPEAT)) {
            setRecitationRepeatVerse(context, RECITATION_DEFAULT_REPEAT);
        }

        return sp.getBoolean(KEY_RECITATION_REPEAT, RECITATION_DEFAULT_REPEAT);
    }

    public static void setRecitationRepeatVerse(Context context, boolean repeatVerse) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_RECITATION_REPEAT, repeatVerse);
        editor.apply();
    }

    public static boolean getRecitationContinueChapter(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_RECITATION_CONTINUE_CHAPTER)) {
            setRecitationContinueChapter(context, RECITATION_DEFAULT_CONTINUE_CHAPTER);
        }

        return sp.getBoolean(KEY_RECITATION_CONTINUE_CHAPTER, RECITATION_DEFAULT_CONTINUE_CHAPTER);
    }

    public static void setRecitationContinueChapter(Context context, boolean continueChapter) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_RECITATION_CONTINUE_CHAPTER, continueChapter);
        editor.apply();
    }

    public static boolean getRecitationScrollSync(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_RECITATION_VERSE_SYNC)) {
            setRecitationVerseSync(context, RECITATION_DEFAULT_VERSE_SYNC);
        }

        return sp.getBoolean(KEY_RECITATION_VERSE_SYNC, RECITATION_DEFAULT_VERSE_SYNC);
    }

    public static void setRecitationVerseSync(Context context, boolean sync) {
        SharedPreferences sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_RECITATION_VERSE_SYNC, sync);
        editor.apply();
    }

    public static String getSavedScript(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_SCRIPT, Context.MODE_PRIVATE);

        if (!sp.contains(KEY_SCRIPT)) {
            setSavedScript(context, SCRIPT_DEFAULT);
        }

        return sp.getString(KEY_SCRIPT, SCRIPT_DEFAULT);
    }

    public static void setSavedScript(Context context, String font) {
        SharedPreferences sp = context.getSharedPreferences(SP_SCRIPT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_SCRIPT, font);
        editor.apply();
    }

    public static int getSavedReaderStyle(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE);

        if (!sp.contains(READER_KEY_READER_STYLE)) {
            setSavedReaderStyle(context, READER_STYLE_DEFAULT);
        }

        sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE);
        return sp.getInt(READER_KEY_READER_STYLE, READER_STYLE_DEFAULT);
    }

    public static void setSavedReaderStyle(Context context, int readerStyle) {
        SharedPreferences sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(READER_KEY_READER_STYLE, readerStyle);
        editor.apply();
    }
}
