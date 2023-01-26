package com.quranapp.android.utils.quran.parser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.utils.reader.ScriptUtils;
import com.quranapp.android.utils.univ.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class QuranParserJSON {
    private static final String KEY_CHAPTER_LIST = "suras";
    private static final String KEY_VERSE_LIST = "ayas";
    private static final String KEY_ARABIC_TEXT = "text";
    private static final String KEY_ID = "id";
    private static final String KEY_NUMBER = "index";
    private final Context mContext;

    public QuranParserJSON(Context context) {
        mContext = context;
    }

    public void parseQuran(String script, @Nullable QuranMeta quranMeta, AtomicReference<Quran> quranRef, Runnable postRunnable) {
        new Thread(() -> {
            try {
                initQuranParse(script, quranMeta, quranRef);
            } catch (Exception e) {
                e.printStackTrace();
            }

            new Handler(Looper.getMainLooper()).post(postRunnable);
        }).start();
    }

    private void initQuranParse(String script, QuranMeta quranMeta, AtomicReference<Quran> quranRef) throws Exception {

        final InputStream is = mContext.getAssets().open(ScriptUtils.getScriptResPath(script));
        JSONObject quranObject = new JSONObject(StringUtils.readInputStream(is));
        Quran parsedQuran = parseQuranInternal(script, quranMeta, quranObject);

        quranRef.set(parsedQuran);
    }

    private Quran parseQuranInternal(String script, @Nullable QuranMeta quranMeta, JSONObject quranObject) throws Exception {
        HashMap<Integer, Chapter> chapters = new HashMap<>();

        readChapters(quranObject, chapters, quranMeta);

        Quran quran = new Quran(script);
        quran.setChapters(chapters);

        return quran;
    }

    private void readChapters(JSONObject quranObject, HashMap<Integer, Chapter> chapters, QuranMeta quranMeta) throws Exception {
        JSONArray chaptersJSON = quranObject.getJSONArray(KEY_CHAPTER_LIST);
        for (int i = 0, l = chaptersJSON.length(); i < l; i++) {
            JSONObject chapterObj = chaptersJSON.getJSONObject(i);

            int chapterNo = chapterObj.getInt(KEY_NUMBER);
            Chapter chapter = new Chapter();

            if (quranMeta != null) {
                chapter.setChapterNumber(chapterNo, quranMeta);
            }

            readSingleChapter(chapterObj, chapter);

            chapters.put(chapterNo, chapter);
        }
    }

    private void readSingleChapter(JSONObject chapterObj, Chapter chapter) throws Exception {
        ArrayList<Verse> verses = new ArrayList<>();

        JSONArray versesJSON = chapterObj.getJSONArray(KEY_VERSE_LIST);
        for (int i = 0, l = versesJSON.length(); i < l; i++) {
            JSONObject verseObj = versesJSON.getJSONObject(i);

            Verse verse = new Verse(chapter.getChapterNumber(), verseObj.getInt(KEY_NUMBER));
            verse.setId(verseObj.getInt(KEY_ID));
            verse.setArabicText(verseObj.getString(KEY_ARABIC_TEXT));

            verses.add(verse);
        }

        chapter.setVerses(verses);
    }
}
