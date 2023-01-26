package com.quranapp.android.utils.quran.parser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.quranapp.android.components.quran.QuranTransl;
import com.quranapp.android.components.quran.subcomponents.Footnote;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.TranslationBook;
import com.quranapp.android.components.transls.TranslModel;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Deprecated
public final class QuranTranslParserJSON {
    public static final String KEY_CHAPTER_LIST = "suras";
    public static final String KEY_VERSE_LIST = "ayas";
    public static final String KEY_TRANSLATION_TEXT = "translation";
    public static final String KEY_FOOTNOTE_LIST = "footnotes";
    public static final String KEY_FOOTNOTE_SINGLE_TEXT = "text";
    public static final String KEY_NUMBER = "index";

    public static final String FOOTNOTE_SINGLE_ATTR_NUMBER = "index";
    public static final String FOOTNOTE_REF_ATTR_INDEX = FOOTNOTE_SINGLE_ATTR_NUMBER;
    public static final String REFERENCE_TAG = "reference";
    public static final String REFERENCE_ATTR_CHAPTER_NO = "chapter";
    public static final String REFERENCE_ATTR_VERSES = "verses";
    public static final String FOOTNOTE_REF_TAG = "fn";

    private final Handler mHandle = new Handler(Looper.getMainLooper());
    private final Context mContext;
    private final FileUtils mFileUtils;

    public QuranTranslParserJSON(Context context) {
        mContext = context;
        mFileUtils = FileUtils.newInstance(context);
    }

    public void parseTranslations(Set<String> translSlugs, AtomicReference<QuranTransl> quranTranslRef, Runnable runnable) {
        final QuranTransl quranTransl;

        if (quranTranslRef.get() == null) {
            quranTransl = new QuranTransl();
            quranTranslRef.set(quranTransl);
        } else {
            quranTransl = quranTranslRef.get();
        }


        new Thread(() -> {
            for (String slug : new HashSet<>(translSlugs)) {
                try {
                    initTranslationParse(mContext, slug, quranTransl, translSlugs);
                } catch (Exception e) {e.printStackTrace();}
            }

            mHandle.post(runnable);
        }).start();
    }

    public void parseTranslationSingle(String slug, QuranTransl quranTransl, Runnable runnable) {
        new Thread(() -> {
            try {
                initTranslationParse(mContext, slug, quranTransl, null);
            } catch (Exception e) {e.printStackTrace();}

            mHandle.post(runnable);
        }).start();
    }

    private void initTranslationParse(Context context, String slug, QuranTransl quranTransl, Set<String> optedSlugs) throws Exception {
        String[] slugSplit = slug.split("_");

        // Word in the slug before the first underscore is always the language code.
        String langCode = slugSplit[0];
        // Word in the slug between the first and second underscores is always the translation id.
        int translId = Integer.parseInt(slugSplit[1]);

        final TranslModel translModel;
        if (TranslUtils.isPrebuilt(slug)) {
            InputStream is = mContext.getAssets().open(TranslUtils.getPrebuiltTranslInfoPath(slug));
            translModel = TranslUtils.readTranslInfo(new JSONObject(StringUtils.readInputStream(is)));
        } else {
            translModel = TranslUtils.readTranslInfo(mFileUtils, mFileUtils.getSingleTranslationInfoFile(langCode, slug));
        }

        // Make sure we are not parsing any illegal-premium-slug.
        if (translModel == null) {
            quranTransl.removeParsedBook(slug);
            if (optedSlugs != null) {
                optedSlugs.remove(slug);
            }
            return;
        }

        if (quranTransl.isTranslBookParsed(slug)) {
            return;
        }

        final String translStrData;
        final String prebuiltTranslPath = TranslUtils.getPrebuiltTranslPath(slug);
        if (prebuiltTranslPath != null) {
            InputStream is = context.getAssets().open(prebuiltTranslPath);
            translStrData = StringUtils.readInputStream(is);
        } else {
            File translationFile = mFileUtils.getSingleTranslationFile(translId, langCode, slug);
            if (!translationFile.exists() || translationFile.length() == 0) {
                if (optedSlugs != null) {
                    optedSlugs.remove(slug);
                }
                return;
            }
            translStrData = mFileUtils.readFile(translationFile);
        }

        TranslationBook nParsedBook = parseTranslationInternal(translStrData, translId, slug, translModel);
        quranTransl.addToParsedTranslBook(nParsedBook);
    }

    private TranslationBook parseTranslationInternal(String translStrData, int id, String slug, TranslModel translModel) throws Exception {
        TranslationBook book = new TranslationBook(translModel);
        book.setId(id);
        book.setSlug(slug);
        book.setBookInfo(translModel);

        JSONObject root = new JSONObject(translStrData);
        readChapters(root, book);

        return book;
    }

    private void readChapters(JSONObject root, TranslationBook book) throws Exception {
        JSONArray chapters = root.getJSONArray(KEY_CHAPTER_LIST);
        for (int i = 0, l = chapters.length(); i < l; i++) {
            JSONObject chapterObj = chapters.getJSONObject(i);
            readSingleChapter(chapterObj.getInt(KEY_NUMBER), chapterObj, book);
        }
    }

    private void readSingleChapter(int chapterNo, JSONObject chapterObj, TranslationBook book) throws Exception {
        JSONArray verses = chapterObj.getJSONArray(KEY_VERSE_LIST);
        for (int i = 0, l = verses.length(); i < l; i++) {
            JSONObject verseObj = verses.getJSONObject(i);
            Translation translation = new Translation();
            translation.setChapterNo(chapterNo);
            translation.setVerseNo(verseObj.getInt(KEY_NUMBER));
            translation.setText(verseObj.getString(KEY_TRANSLATION_TEXT));

            book.addTranslation(translation);
            // set footnotes after adding this translation to the translation book,
            // so that translation book can be passed to all the footnotes.
            translation.setFootnotes(readFootnotes(verseObj, chapterNo, translation.getVerseNo()));
        }
    }

    private HashMap<Integer, Footnote> readFootnotes(JSONObject verseObj, int chapterNo, int verseNo) throws Exception {
        HashMap<Integer, Footnote> footnotesMap = new HashMap<>();

        if (!verseObj.has(KEY_FOOTNOTE_LIST)) {
            return footnotesMap;
        }

        JSONArray footnotes = verseObj.getJSONArray(KEY_FOOTNOTE_LIST);
        for (int i = 0, l = footnotes.length(); i < l; i++) {
            JSONObject footnoteObj = footnotes.getJSONObject(i);

            Footnote footnote = new Footnote();
            footnote.chapterNo = chapterNo;
            footnote.verseNo = verseNo;
            footnote.number = footnoteObj.getInt(KEY_NUMBER);
            footnote.text = footnoteObj.getString(KEY_FOOTNOTE_SINGLE_TEXT);

            footnotesMap.put(footnote.number, footnote);
        }

        return footnotesMap;
    }
}
