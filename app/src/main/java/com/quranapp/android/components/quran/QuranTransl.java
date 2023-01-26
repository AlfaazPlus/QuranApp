package com.quranapp.android.components.quran;

import android.content.Context;

import androidx.annotation.Nullable;

import com.peacedesign.android.utils.Log;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.TranslationBook;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.quran.parser.QuranTranslParserJSON;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.sp.SPReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

@Deprecated
public class QuranTransl {
    private final Map<String, TranslationBook> parsedTranslBooks = new HashMap<>();
    private Set<String> mOptedTranslations = new TreeSet<>();
    private static final AtomicReference<QuranTransl> sTranslRef = new AtomicReference<>();

    private QuranTransl(QuranTransl quranTransl) {
        quranTransl.parsedTranslBooks.forEach((s, translationBook) -> addToParsedTranslBook(translationBook.copy()));
        mOptedTranslations = new TreeSet<>(quranTransl.mOptedTranslations);
    }

    public QuranTransl() {}


    public static QuranTransl getInstance() {
        if (sTranslRef != null) {
            return sTranslRef.get();
        }
        return null;
    }

    public static void prepareInstance(Context context, OnResultReadyCallback<QuranTransl> resultReadyCallback) {
        prepareInstance(context, SPReader.getSavedTranslations(context), resultReadyCallback);
    }

    public static void prepareInstance(Context context, Set<String> translSlugs, OnResultReadyCallback<QuranTransl> resultReadyCallback) {
        if (sTranslRef.get() == null || !sTranslRef.get().mOptedTranslations.equals(translSlugs)) {
            synchronized (QuranTransl.class) {
                if (sTranslRef.get() == null || !sTranslRef.get().mOptedTranslations.equals(translSlugs)) {
                    prepare(context, translSlugs, resultReadyCallback);
                } else {
                    resultReadyCallback.onReady(sTranslRef.get());
                }
            }
        } else {
            resultReadyCallback.onReady(sTranslRef.get());
        }
    }

    public static void prepareInstanceForSingle(Context context, String slug, OnResultReadyCallback<TranslationBook> resultReadyCallback) {
        if (sTranslRef.get() == null || !sTranslRef.get().isTranslBookParsed(slug)) {
            synchronized (QuranTransl.class) {
                if (sTranslRef.get() == null || !sTranslRef.get().isTranslBookParsed(slug)) {
                    sTranslRef.set(new QuranTransl());
                    QuranTranslParserJSON translParser = new QuranTranslParserJSON(context);
                    translParser.parseTranslationSingle(slug, sTranslRef.get(),
                            () -> {
                                Log.d(sTranslRef.get().isTranslBookParsed(slug), sTranslRef.get().getParsedTranslBook(slug));
                                resultReadyCallback.onReady(sTranslRef.get().getParsedTranslBook(slug));
                            });
                } else {
                    Log.d(sTranslRef.get().getParsedTranslBook(slug));
                    resultReadyCallback.onReady(sTranslRef.get().getParsedTranslBook(slug));
                }
            }
        } else {
            Log.d(sTranslRef.get().getParsedTranslBook(slug));
            resultReadyCallback.onReady(sTranslRef.get().getParsedTranslBook(slug));
        }
    }

    private static void prepare(Context context, Set<String> translSlugs, OnResultReadyCallback<QuranTransl> resultReadyCallback) {
        QuranTranslParserJSON translParser = new QuranTranslParserJSON(context);
        translParser.parseTranslations(translSlugs, sTranslRef,
                () -> resultReadyCallback.onReady(sTranslRef.get()));
    }

    /**
     * Method to be used in {@link QuranTranslParserJSON}. Stores a parsed translation book.
     *
     * @param translationBook The parsed translation book.
     */
    public void addToParsedTranslBook(TranslationBook translationBook) {
        parsedTranslBooks.put(translationBook.getSlug(), translationBook);
        mOptedTranslations.add(translationBook.getSlug());
    }

    /**
     * Get specific translation book with slug as key.
     *
     * @param slug The slug for the translation.
     */
    public TranslationBook getParsedTranslBook(String slug) {
        return parsedTranslBooks.get(slug);
    }

    /**
     * Get all translation books.
     */
    public Map<String, TranslationBook> getAllParsedTranslBooks() {
        return parsedTranslBooks;
    }

    /**
     * @return Returns total number of books which is currently parsed
     */
    public int getParsedBooksCount() {
        return parsedTranslBooks.size();
    }

    /**
     * @param chapterNo Chapter No.
     * @param verseNo   Verse No.
     * @return Returns all parsed translations of the verse.
     */
    public ArrayList<Translation> getTranslations(int chapterNo, int verseNo) {
        return getTranslations(mOptedTranslations, chapterNo, verseNo);
    }

    /**
     * @return Returns all parsed translations of the verse for the {@param slugs}.
     */
    public ArrayList<Translation> getTranslations(Set<String> slugs, int chapterNo, int verseNo) {
        ArrayList<Translation> translations = new ArrayList<>();

        if (slugs == null || slugs.isEmpty()) return translations;

        for (String slug : slugs) {
            TranslationBook translBook = getParsedTranslBook(slug);
            if (translBook != null) {
                translations.add(translBook.getTranslation(chapterNo, verseNo));
            }
        }
        return translations;
    }

    /**
     * Get translation for a specific verse with slug as key.
     *
     * @param slug The slug for the translation.
     */
    @Nullable
    public Translation getTranslation(String slug, int chapterNo, int verseNo) {
        final TranslationBook translBook = getParsedTranslBook(slug);
        if (translBook != null) {
            return translBook.getTranslation(chapterNo, verseNo);
        }

        // return an empty translation for now if translationBook with the slug is not parse.
        return null;
    }

    /**
     * @param slug Slug for the translation book. It can be found in {@link TranslUtils}
     * @return Returns if the translation book xml is parsed.
     * Note: if {@link #parsedTranslBooks} contains the slug as key, it means that its parsed book was previously added.
     */
    public boolean isTranslBookParsed(String slug) {
        return parsedTranslBooks.containsKey(slug);
    }

    /**
     * Remove a parsed book. This method helpful after updating a translation because it need to be reparsed.
     *
     * @param slug The slug for the translation.
     */
    public void removeParsedBook(String slug) {
        parsedTranslBooks.remove(slug);
        mOptedTranslations.remove(slug);
    }

    /**
     * @return The slugs of all parsed books.
     * The opted translation slugs may be the slugs which are currently saved in SharedPreference. It can be get by {@link SPReader#getSavedTranslations}.
     * If we have to modify the returned set in a loop, then always make a copy first.
     */
    public Set<String> getOptedTranslations() {
        if (mOptedTranslations == null) {
            mOptedTranslations = new TreeSet<>();
        }
        return mOptedTranslations;
    }

    /**
     * @param optedTranslations The opted translation slugs which is currently saved in SharedPreference.
     *                          It can be get by {@link SPReader#getSavedTranslations}.
     */
    public void setOptedTranslations(Set<String> optedTranslations) {
        mOptedTranslations = new TreeSet<>(optedTranslations);
    }

    public QuranTransl copy() {
        return new QuranTransl(this);
    }
}
