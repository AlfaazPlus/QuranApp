package com.quranapp.android.utils.quran.parser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.univ.RegexPattern;
import com.quranapp.android.utils.univ.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

import kotlin.Pair;

public final class QuranMetaParserJSON {
    private static final String ATTR_INDEX = "index";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_CHAPTERS = "chapters";
    private static final String ATTR_CHAPTERS_VERSES = "verses";
    private static final String ATTR_PAGES = "pages";

    private static final String CHAPTERS_TAG_CHAPTERS = "suras";
    private static final String CHAPTERS_ATTR_VERSE_COUNT = "count";
    private static final String CHAPTERS_ATTR_RUKU_COUNT = "rukus";
    private static final String CHAPTERS_ATTR_VERSE_START = "start";
    private static final String CHAPTERS_ATTR_REVLTN_TYPE = "type";
    private static final String CHAPTERS_ATTR_REVLTN_ORDER = "order";
    private static final String CHAPTERS_ATTR_TRANSLATION = "translation";
    private static final String CHAPTERS_ATTR_TAGS = "tags";

    private static final String JUZS_TAG_JUZS = "juzs";
    private static final String PAGES_TAG_PAGES = "pages";

    public void parseMeta(Context ctx, AtomicReference<QuranMeta> quranMetaRef, Runnable postRunnable) {
        new Thread(() -> {
            try {
                initMetaParse(ctx, quranMetaRef);
            } catch (Exception e) {
                Log.saveError(e, "QuranMetaParserJSON.parseMeta");
            }

            new Handler(Looper.getMainLooper()).post(postRunnable);
        }).start();
    }

    private void initMetaParse(Context ctx, AtomicReference<QuranMeta> metaRef) throws Exception {
        final InputStream is = ctx.getAssets().open("quran_meta.json");
        JSONObject metaObject = new JSONObject(StringUtils.readInputStream(is));
        QuranMeta parsedMeta = parseMetaInternal(metaObject);
        metaRef.set(parsedMeta);
    }

    private QuranMeta parseMetaInternal(JSONObject metaObject) throws JSONException {
        SparseArray<QuranMeta.ChapterMeta> chapterMetaMap = new SparseArray<>();
        SparseArray<QuranMeta.JuzMeta> juzMetaMap = new SparseArray<>();
        SparseArray<QuranMeta.PageMeta> pageMetaMap = new SparseArray<>();

        JSONArray chapters = metaObject.getJSONArray(CHAPTERS_TAG_CHAPTERS);
        for (int i = 0, l = chapters.length(); i < l; i++) {
            QuranMeta.ChapterMeta chapterMeta = makeChapterMeta(chapters.getJSONObject(i));
            chapterMetaMap.put(chapterMeta.chapterNo, chapterMeta);
        }

        JSONArray juzs = metaObject.getJSONArray(JUZS_TAG_JUZS);
        for (int i = 0, l = juzs.length(); i < l; i++) {
            QuranMeta.JuzMeta juzMeta = makeJuzMeta(juzs.getJSONObject(i));
            juzMetaMap.put(juzMeta.juzNo, juzMeta);
        }

        JSONArray pages = metaObject.getJSONArray(PAGES_TAG_PAGES);
        for (int i = 0, l = pages.length(); i < l; i++) {
            QuranMeta.PageMeta pageMeta = makePageMeta(pages.getJSONObject(i));
            pageMetaMap.put(pageMeta.pageNo, pageMeta);
        }

        QuranMeta quranMeta = new QuranMeta();
        quranMeta.setChapterMetaMap(chapterMetaMap);
        quranMeta.setJuzMetaMap(juzMetaMap);
        quranMeta.setPageMetaMap(pageMetaMap);
        return quranMeta;
    }

    private QuranMeta.ChapterMeta makeChapterMeta(JSONObject chapterObj) throws JSONException {
        QuranMeta.ChapterMeta chapterMeta = new QuranMeta.ChapterMeta();
        chapterMeta.chapterNo = chapterObj.getInt(ATTR_INDEX);
        chapterMeta.verseCount = chapterObj.getInt(CHAPTERS_ATTR_VERSE_COUNT);
        chapterMeta.rukuCount = chapterObj.getInt(CHAPTERS_ATTR_RUKU_COUNT);
        chapterMeta.startsVerseId = chapterObj.getInt(CHAPTERS_ATTR_VERSE_START) + 1;
        chapterMeta.revelationOrder = chapterObj.getInt(CHAPTERS_ATTR_REVLTN_ORDER);
        chapterMeta.revelationType = chapterObj.getString(CHAPTERS_ATTR_REVLTN_TYPE);
        chapterMeta.pageRange = prepareRangeItem(chapterObj.getString(ATTR_PAGES));
        chapterMeta.tags = chapterObj.getString(CHAPTERS_ATTR_TAGS);

        StringBuilder nameTags = new StringBuilder();

        JSONObject names = chapterObj.getJSONObject(ATTR_NAME);
        Iterator<String> namesLangCodes = names.keys();
        while (namesLangCodes.hasNext()) {
            String langCode = namesLangCodes.next();
            String name = names.getString(langCode);
            chapterMeta.addName(langCode, name);

            nameTags.append(name);
        }

        JSONObject translations = chapterObj.getJSONObject(CHAPTERS_ATTR_TRANSLATION);
        Iterator<String> translsLangCodes = translations.keys();
        while (translsLangCodes.hasNext()) {
            String langCode = translsLangCodes.next();
            String translation = translations.getString(langCode);
            chapterMeta.addNameTranslation(langCode, translation);

            nameTags.append(translation);
        }

        chapterMeta.tags += nameTags.toString();
        return chapterMeta;
    }

    private QuranMeta.JuzMeta makeJuzMeta(JSONObject juzObj) throws JSONException {
        QuranMeta.JuzMeta juzMeta = new QuranMeta.JuzMeta();
        juzMeta.juzNo = juzObj.getInt(ATTR_INDEX);
        juzMeta.pages = prepareRangeItem(juzObj.getString(ATTR_PAGES));
        juzMeta.chapters = prepareRangeItem(juzObj.getString(ATTR_CHAPTERS));
        juzMeta.verseRangeOfChapter = prepareVersesOfChapters(juzObj.getString(ATTR_CHAPTERS_VERSES));
        juzMeta.verseCount = 0;

        JSONObject name = juzObj.getJSONObject(ATTR_NAME);

        juzMeta.nameAr = name.getString("ar");
        juzMeta.nameTrans = name.getString("en");

        IntStream.rangeClosed(juzMeta.chapters.getFirst(), juzMeta.chapters.getSecond()).forEach(chapterNo -> {
            Pair<Integer, Integer> verses = juzMeta.verseRangeOfChapter.get(chapterNo);
            if (verses != null) {
                juzMeta.verseCount += (verses.getSecond() - verses.getFirst()) + 1;
            }
        });

        return juzMeta;
    }

    /**
     * Prepare chapters in Juz or on page
     */
    private Pair<Integer, Integer> prepareRangeItem(String attrValue) {
        final String[] split = attrValue.split("-");
        final int from;
        final int to;
        if (split.length == 1) {
            from = Integer.parseInt(split[0]);
            to = from;
        } else {
            from = Integer.parseInt(split[0]);
            to = Integer.parseInt(split[1]);
        }
        return new Pair<>(from, to);
    }

    /**
     * Prepare verses of chapters in Juz or on page
     */
    private Map<Integer, Pair<Integer, Integer>> prepareVersesOfChapters(String attrValue) {
        // 9:93-123,10:1-109,11:1-5
        Map<Integer, Pair<Integer, Integer>> verseRangeOfChapter = new TreeMap<>();
        final Matcher matcher = RegexPattern.VERSE_RANGE_JUMP_PATTERN.matcher(attrValue);

        while (matcher.find()) {
            final MatchResult result = matcher.toMatchResult();
            int chapterNo = Integer.parseInt(result.group(1));
            int fromVerseNo = Integer.parseInt(result.group(2));
            int toVerseNo = Integer.parseInt(result.group(3));

            verseRangeOfChapter.put(chapterNo, new Pair<>(fromVerseNo, toVerseNo));
        }
        return verseRangeOfChapter;
    }

    private QuranMeta.PageMeta makePageMeta(JSONObject pageObj) throws JSONException {
        QuranMeta.PageMeta pageMeta = new QuranMeta.PageMeta();
        pageMeta.pageNo = pageObj.getInt(ATTR_INDEX);
        pageMeta.chapters = prepareRangeItem(pageObj.getString(ATTR_CHAPTERS));
        pageMeta.verseRangeOfChapter = prepareVersesOfChapters(pageObj.getString(ATTR_CHAPTERS_VERSES));
        return pageMeta;
    }
}
