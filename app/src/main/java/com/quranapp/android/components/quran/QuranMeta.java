package com.quranapp.android.components.quran;

import android.content.Context;
import android.util.SparseArray;
import androidx.annotation.NonNull;

import com.quranapp.android.R;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.quran.parser.QuranMetaParserJSON;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.Pair;

public class QuranMeta implements Serializable {
    private static final AtomicReference<QuranMeta> sQuranMetaRef = new AtomicReference<>();
    private SparseArray<ChapterMeta> chapterMetaMap = new SparseArray<>();
    private SparseArray<JuzMeta> juzMetaMap = new SparseArray<>();
    private SparseArray<PageMeta> pageMetaMap = new SparseArray<>();

    public static void prepareInstance(@NonNull Context context, OnResultReadyCallback<QuranMeta> resultReadyCallback) {
        if (sQuranMetaRef.get() == null) {
            synchronized (QuranMeta.class) {
                if (sQuranMetaRef.get() == null) {
                    prepare(context, resultReadyCallback);
                } else {
                    resultReadyCallback.onReady(sQuranMetaRef.get());
                }
            }
        } else {
            resultReadyCallback.onReady(sQuranMetaRef.get());
        }
    }

    private static void prepare(Context context, OnResultReadyCallback<QuranMeta> resultReadyCallback) {
        QuranMetaParserJSON metaParser = new QuranMetaParserJSON();
        metaParser.parseMeta(context, sQuranMetaRef, () -> resultReadyCallback.onReady(sQuranMetaRef.get()));
    }

    public static int totalVerses() {
        return 6236;
    }

    public static int totalChapters() {
        return 114;
    }

    public static int totalJuzs() {
        return 30;
    }

    public static int totalPages() {
        return 604;
    }

    public static boolean canShowBismillah(int chapterNo) {
        return chapterNo != 1 && chapterNo != 9;
    }

    public static boolean isChapterValid(int chapterNo) {
        return chapterNo >= 1 && chapterNo <= totalChapters();
    }

    public static boolean isJuzValid(int juzNo) {
        return juzNo >= 1 && juzNo <= totalJuzs();
    }

    public void setChapterMetaMap(SparseArray<ChapterMeta> chapterMetaMap) {
        this.chapterMetaMap = chapterMetaMap;
    }

    public void setJuzMetaMap(SparseArray<JuzMeta> juzMetaMap) {
        this.juzMetaMap = juzMetaMap;
    }

    public void setPageMetaMap(SparseArray<PageMeta> pageMetaMap) {
        this.pageMetaMap = pageMetaMap;
    }

    public SparseArray<ChapterMeta> getChapterMetas() {
        return chapterMetaMap;
    }

    public ChapterMeta getChapterMeta(int chapterNo) {
        return chapterMetaMap.get(chapterNo);
    }

    public String getChapterName(Context ctx, int chapterNo) {
        return getChapterName(ctx, chapterNo, false);
    }

    public String getChapterName(Context ctx, int chapterNo, String langCode) {
        return getChapterName(ctx, chapterNo, langCode, false);
    }

    public String getChapterName(Context ctx, int chapterNo, boolean withPrefix) {
        ChapterMeta chapterMeta = getChapterMeta(chapterNo);
        if (chapterMeta == null) {
            return null;
        }

        String name = chapterMeta.getName();
        if (withPrefix) {
            name = ctx.getString(R.string.strLabelSurah, name);
        }
        return name;
    }

    public String getChapterName(Context ctx, int chapterNo, String langCode, boolean withPrefix) {
        String name = getChapterMeta(chapterNo).getName(langCode);
        if (withPrefix) {
            name = ctx.getString(R.string.strLabelSurah, name);
        }
        return name;
    }

    public String getChapterNameTranslation(int chapterNo) {
        return getChapterMeta(chapterNo).getNameTranslation();
    }

    public int getChapterVerseCount(int chapterNo) {
        return getChapterMeta(chapterNo).verseCount;
    }

    public int getChapterRukuCount(int chapterNo) {
        return getChapterMeta(chapterNo).rukuCount;
    }

    public int getChapterStartVerseId(int chapterNo) {
        return getChapterMeta(chapterNo).startsVerseId;
    }

    public int getChapterRevelationOrder(int chapterNo) {
        return getChapterMeta(chapterNo).revelationOrder;
    }

    public String getChapterRevelationType(int chapterNo) {
        return getChapterMeta(chapterNo).revelationType;
    }

    public ArrayList<Integer> getChapterJuzs(int chapterNo) {
        final ChapterMeta chapterMeta = getChapterMeta(chapterNo);
        if (chapterMeta.juzs != null) {
            return chapterMeta.juzs;
        }

        ArrayList<Integer> juzNos = new ArrayList<>();

        boolean lastContained = false;


        for (int i = 1; i <= totalJuzs(); i++) {
            Pair<Integer, Integer> fromToChapters = getChaptersInJuz(i);

            boolean contains = false;

            for (int chapterNumber = fromToChapters.getFirst(), toChapter = fromToChapters.getSecond(); chapterNumber <= toChapter; chapterNumber++) {
                if (chapterNumber == chapterNo) {
                    contains = true;
                    break;
                }
            }

            if (contains) {
                lastContained = true;
                juzNos.add(i);
            } else if (lastContained) {
                break;
            }
        }

        chapterMeta.juzs = juzNos;

        return juzNos;
    }

    public Pair<Integer, Integer> getChapterPageRange(int chapterNo) {
        return getChapterMeta(chapterNo).pageRange;
    }

    public String getChapterTags(int chapterNo) {
        return getChapterMeta(chapterNo).tags;
    }

    public boolean isVerseValid4Chapter(int chapterNo, int verseNo) {
        return verseNo >= 1 && verseNo <= getChapterVerseCount(chapterNo);
    }

    public boolean isVerseRangeValid4Chapter(int chapterNo, Pair<Integer, Integer> range) {
        return isVerseRangeValid4Chapter(chapterNo, range.getFirst(), range.getSecond());
    }

    public boolean isVerseRangeValid4Chapter(int chapterNo, int fromVerse, int toVerse) {
        return 1 <= fromVerse && fromVerse <= toVerse && fromVerse <= getChapterVerseCount(chapterNo)
            && toVerse <= getChapterVerseCount(chapterNo);
    }

    public JuzMeta getJuzMeta(int juzNo) {
        return juzMetaMap.get(juzNo);
    }

    public String getJuzNameArabic(int juzNo) {
        return getJuzMeta(juzNo).nameAr;
    }

    public String getJuzNameTransliterated(int juzNo) {
        return getJuzMeta(juzNo).nameTrans;
    }

    public Pair<Integer, Integer> getChaptersInJuz(int juzNo) {
        return getJuzMeta(juzNo).chapters;
    }

    public Pair<Integer, Integer> getVerseRangeOfChapterInJuz(int juzNo, int chapterNo) {
        return getJuzMeta(juzNo).verseRangeOfChapter.get(chapterNo);
    }

    public int getJuzVerseCount(int juzNo) {
        return getJuzMeta(juzNo).verseCount;
    }

    public Pair<Integer, Integer> getJuzPageRange(int juzNo) {
        return getJuzMeta(juzNo).pages;
    }

    public boolean isChapterValid4Juz(int juzNo, int chapterNo) {
        Pair<Integer, Integer> chaptersInJuz = getChaptersInJuz(juzNo);
        return chaptersInJuz.getFirst() <= chapterNo && chapterNo <= chaptersInJuz.getSecond();
    }

    public boolean isVerseValid4Juz(int juzNo, int chapterNo, int verseNo) {
        if (isChapterValid4Juz(juzNo, chapterNo)) {
            Pair<Integer, Integer> verseRangeOfChapterInJuz = getVerseRangeOfChapterInJuz(juzNo, chapterNo);
            return verseRangeOfChapterInJuz.getFirst() <= verseNo && verseNo <= verseRangeOfChapterInJuz.getSecond();
        }
        return false;
    }

    public PageMeta getPageMeta(int pageNo) {
        return pageMetaMap.get(pageNo);
    }

    public Pair<Integer, Integer> getChaptersOnPage(int pageNo) {
        return getPageMeta(pageNo).chapters;
    }

    public Pair<Integer, Integer> getVerseRangeOfChapterOnPage(int pageNo, int chapterNo) {
        return getPageMeta(pageNo).verseRangeOfChapter.get(chapterNo);
    }

    public Pair<Integer, Integer> getPageRangeOfVerseRangeOfChapter(int chapterNo, int fromVerse, int toVerse) {
        final Pair<Integer, Integer> chapterPageRange = getChapterPageRange(chapterNo);

        PageMeta fromPageMeta = getPageMeta(chapterPageRange.getFirst());
        PageMeta toPageMeta = getPageMeta(chapterPageRange.getSecond());

        int fromPage = -1;
        int toPage = -1;

        while (fromPageMeta.pageNo <= toPageMeta.pageNo) {
            if (QuranUtils.isVerseInRange(
                fromVerse,
                Objects.requireNonNull(fromPageMeta.verseRangeOfChapter.get(chapterNo))
            )) {
                fromPage = fromPageMeta.pageNo;
                break;
            }

            fromPageMeta = getPageMeta(fromPageMeta.pageNo + 1);
        }

        if (!QuranUtils.doesRangeDenoteSingle(fromVerse, toVerse)) {
            while (fromPageMeta.pageNo >= toPageMeta.pageNo) {
                if (QuranUtils.isVerseInRange(
                    toVerse,
                    Objects.requireNonNull(toPageMeta.verseRangeOfChapter.get(chapterNo))
                )) {
                    toPage = toPageMeta.pageNo;
                    break;
                }

                toPageMeta = getPageMeta(toPageMeta.pageNo - 1);
            }
        } else {
            toPage = fromPage;
        }

        return new Pair<>(fromPage, toPage);
    }

    public int getJuzForPage(int pageNo) {
        int juz = -1;
        for (int i = 0, l = juzMetaMap.size(); i < l; i++) {
            JuzMeta juzMeta = juzMetaMap.valueAt(i);
            Pair<Integer, Integer> pages = juzMeta.pages;
            if (pages.getFirst() <= pageNo && pageNo <= pages.getSecond()) {
                juz = juzMeta.juzNo;
                break;
            }
        }
        return juz;
    }

    public static class ChapterMeta implements Serializable {
        public int chapterNo;

        /**
         * Translations of names.
         * Key is lang code.
         */
        private final HashMap<String, String> translationMap = new HashMap<>();
        private final HashMap<String, String> nameMap = new HashMap<>();

        public int verseCount;
        public int rukuCount;
        public int startsVerseId;
        public int revelationOrder;
        public String revelationType;
        /**
         * from page to page, both equal if this chapter is expanded to only one page.
         */
        public Pair<Integer, Integer> pageRange;
        /**
         * from juz to juz, both equal if this chapter is expanded to only one juz.
         */
        private ArrayList<Integer> juzs;
        public String tags = "";

        public void addName(String langCode, String name) {
            nameMap.put(langCode, name);
        }

        public String getName() {
            return getName(Locale.getDefault().getLanguage());
        }

        public String getName(String langCode) {
            String name = nameMap.get(langCode);
            if (name == null) {
                name = nameMap.get("en");
            }
            return name != null ? name : "";
        }

        public HashMap<String, String> getNames() {
            return nameMap;
        }

        public void addNameTranslation(String langCode, String translation) {
            translationMap.put(langCode, translation);
        }

        public String getNameTranslation() {
            return getNameTranslation(Locale.getDefault().getLanguage());
        }

        public String getNameTranslation(String langCode) {
            String transl = translationMap.get(langCode);
            if (transl == null) {
                transl = translationMap.get("en");
            }
            return transl != null ? transl : "";
        }

        public HashMap<String, String> getNameTranslations() {
            return translationMap;
        }
    }

    public static class JuzMeta implements Serializable {
        public int juzNo;
        public String nameAr, nameTrans;
        /**
         * from page to page.
         */
        public Pair<Integer, Integer> pages;
        /**
         * from chapter to chapter, both equal if juz contains only one chapter.
         */
        public Pair<Integer, Integer> chapters;
        /**
         * Integer -> chapterNo, Pair<Integer, Integer> -> from verse to verse (contained in this juz for the particular chapter).
         */
        public Map<Integer, Pair<Integer, Integer>> verseRangeOfChapter = new TreeMap<>();
        public int verseCount;
    }

    public static class PageMeta implements Serializable {
        public int pageNo;
        /**
         * from chapter to chapter, both equal if page contains only one chapter.
         */
        public Pair<Integer, Integer> chapters;
        /**
         * Integer -> chapterNo, int[] -> from verse to verse (contained on this page for the particular chapter).
         */
        public Map<Integer, Pair<Integer, Integer>> verseRangeOfChapter = new TreeMap<>();

        public boolean isPageNoForChapter(int chapterNo) {
            return chapters.getFirst() <= chapterNo && chapterNo <= chapters.getSecond();
        }

        public boolean isPageNoForVerse(int chapterNo, int verseNo) {
            if (verseRangeOfChapter.containsKey(chapterNo)) {
                final Pair<Integer, Integer> verseRange = verseRangeOfChapter.get(chapterNo);
                if (verseRange != null) {
                    return verseRange.getFirst() <= verseNo && verseNo <= verseRange.getSecond();
                }
            }
            return false;
        }
    }
}





























































































