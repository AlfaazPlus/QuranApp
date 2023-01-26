package com.quranapp.android.components.quran;

import android.content.Context;

import androidx.annotation.Nullable;

import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.quran.parser.QuranParserJSON;
import com.quranapp.android.utils.sp.SPReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Quran {
    private static final AtomicReference<Quran> sQuranRef = new AtomicReference<>();
    private final String mScript;
    private Map<Integer, Chapter> chapters = new HashMap<>();

    public Quran(String script) {
        mScript = script;
    }

    private Quran(Quran quran) {
        mScript = quran.mScript;
        chapters = new HashMap<>();
        quran.chapters.forEach((chapterNo, chapter) -> chapters.put(chapterNo, chapter.copy()));
    }


    public static void prepareInstance(Context context, @Nullable QuranMeta quranMeta, OnResultReadyCallback<Quran> resultReadyCallback) {
        String savedScript = SPReader.getSavedScript(context);

        if (sQuranRef.get() == null || !Objects.equals(sQuranRef.get().mScript, savedScript)) {
            synchronized (Quran.class) {
                if (sQuranRef.get() == null || !Objects.equals(sQuranRef.get().mScript, savedScript)) {
                    prepare(context, savedScript, quranMeta, resultReadyCallback);
                } else {
                    resultReadyCallback.onReady(sQuranRef.get());
                }
            }
        } else {
            resultReadyCallback.onReady(sQuranRef.get());
        }
    }

    private static void prepare(Context context, String script, @Nullable QuranMeta quranMeta, OnResultReadyCallback<Quran> resultReadyCallback) {
        QuranParserJSON parser = new QuranParserJSON(context);
        parser.parseQuran(script, quranMeta, sQuranRef, () -> resultReadyCallback.onReady(sQuranRef.get()));
    }

    public String getScript() {
        return mScript;
    }

    public Map<Integer, Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(HashMap<Integer, Chapter> chapters) {
        this.chapters = chapters;
    }

    public Chapter getChapter(int chapterNo) {
        return chapters.get(chapterNo);
    }

    public Verse getVerse(int chapterNo, int verseNo) {
        Chapter chapter = getChapter(chapterNo);
        if (chapter == null) {
            return null;
        }
        return chapter.getVerse(verseNo);
    }

    public Quran copy() {
        return new Quran(this);
    }
}
