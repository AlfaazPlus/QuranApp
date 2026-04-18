package com.quranapp.android.utils.quran;

import com.quranapp.android.components.quran.QuranMeta;

import java.util.stream.IntStream;

import kotlin.Pair;

public abstract class QuranUtils {
    public static int getAyahId(int chapterNo, int verseNo) {
        return chapterNo * 1000 + verseNo;
    }

    public static Pair<Integer, Integer> getVerseNoFromAyahId(int ayahId) {
        int chapterNo = ayahId / 1000;
        int ayahNo = ayahId % 1000;

        return new Pair(chapterNo, ayahNo);
    }

    public static boolean doesVerseRangeEqualWhole(QuranMeta quranMeta, int chapterNo, int fromVerse, int toVerse) {
        return fromVerse == 1 && toVerse == quranMeta.getChapterVerseCount(chapterNo);
    }

    public static Pair<Integer, Integer> correctVerseInRange(QuranMeta quranMeta, int chapterNo, Pair<Integer, Integer> range) {
        int count = quranMeta.getChapterVerseCount(chapterNo);

        return new Pair<>(Math.max(range.getFirst(), 1), Math.min(range.getSecond(), count));
    }

    public static Pair<Integer, Integer> swapVerseRangeIfNeeded(Pair<Integer, Integer> range) {
        if (range.getFirst() > range.getSecond()) {
            return new Pair<>(range.getSecond(), range.getFirst());
        }
        return range;
    }

    public static boolean isVerseInRange(int verseNo, Pair<Integer, Integer> range) {
        return verseNo >= range.getFirst() && verseNo <= range.getSecond();
    }

    public static boolean doesRangeDenoteSingle(Pair<Integer, Integer> range) {
        return doesRangeDenoteSingle(range.getFirst(), range.getSecond());
    }

    public static boolean doesRangeDenoteSingle(int fromVerse, int toVerse) {
        return fromVerse == toVerse;
    }


    public static void intRangeIterateWithIndex(Pair<Integer, Integer> range, IterationItemCatcherWithIndex itemCatcher) {
        intRangeIterateWithIndex(range.getFirst(), range.getSecond(), itemCatcher);
    }

    public static void intRangeIterateWithIndex(int from, int to, IterationItemCatcherWithIndex itemCatcher) {
        for (int currentItem = from, i = 0; currentItem <= to; currentItem++) {
            itemCatcher.currentItem(i, currentItem);
            i++;
        }
    }

    public static void iterateChapterNo(IterationItemCatcher itemCatcher) {
        IntStream.rangeClosed(1, QuranMeta.totalChapters()).forEach(itemCatcher::currentItem);
    }

    public static void iterateJuzNo(IterationItemCatcher itemCatcher) {
        IntStream.rangeClosed(1, QuranMeta.totalJuzs()).forEach(itemCatcher::currentItem);
    }

    public interface IterationItemCatcher {
        void currentItem(int currentInt);
    }

    public interface IterationItemCatcherWithIndex {
        void currentItem(int index, int currentInt);
    }
}
