package com.quranapp.android.components.quran.subcomponents;

import com.quranapp.android.components.transls.TranslModel;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.univ.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class TranslationBook implements Serializable {
    public static final boolean DISPLAY_NAME_DEFAULT_WITHOUT_HYPHEN = false;
    /**
     * Key is generated in {@link #generateKey}
     */
    private final Map<String, Translation> translations = new HashMap<>();
    private final TranslModel mTranslModel;
    private int id;
    private String bookName;
    private String authorName;
    private String displayName;
    private String langName = "English";
    private String langCode = "en";
    private String slug;

    public TranslationBook(TranslModel translModel) {
        mTranslModel = translModel;
    }

    private TranslationBook(TranslationBook translationBook) {
        mTranslModel = translationBook.mTranslModel;
        id = translationBook.id;
        bookName = translationBook.bookName;
        authorName = translationBook.authorName;
        displayName = translationBook.displayName;
        langName = translationBook.langName;
        langCode = translationBook.langCode;
        slug = translationBook.slug;
        translationBook.translations.forEach((s, translation) -> addTranslation(translation.copy()));
    }

    public TranslModel getTranslModel() {
        return mTranslModel;
    }

    public String getBookName() {
        return bookName;
    }

    private void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getLangName() {
        return langName;
    }

    public String getLangCode() {
        return langCode;
    }

    private void setLanguage(String langCode, String langName) {
        this.langCode = langCode;
        this.langName = langName;
    }

    public String getAuthorName() {
        return authorName;
    }

    private void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getDisplayName() {
        return getDisplayName(DISPLAY_NAME_DEFAULT_WITHOUT_HYPHEN);
    }

    private void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName(boolean withoutHyphen) {
        return withoutHyphen ? displayName : StringUtils.HYPHEN + " " + displayName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setBookInfo(TranslModel translModel) {
        setSlug(translModel.getBookInfo().getSlug());
        setAuthorName(translModel.getBookInfo().getAuthorName());
        setDisplayName(translModel.getBookInfo().getDisplayName());
        setBookName(translModel.getBookInfo().getBookName());
        setLanguage(translModel.getBookInfo().getLangCode(), translModel.getBookInfo().getLangName());
    }

    public Map<String, Translation> getAllTranslations() {
        return translations;
    }

    public Translation getTranslation(int chapterNo, int verseNo) {
        return translations.get(generateKey(chapterNo, verseNo));
    }

    public Footnote getFootnote(int chapNo, int verseNo, int footnoteNo) {
        Translation translation = getTranslation(chapNo, verseNo);
        return translation != null ? translation.getFootnote(footnoteNo) : null;
    }

    public void addTranslation(Translation translation) {
        final int chapterNo = translation.getChapterNo();
        final int verseNo = translation.getVerseNo();

        String key = generateKey(chapterNo, verseNo);
        translation.setBookSlug(slug);
        translation.setUrdu(TranslUtils.isUrdu(slug));
        translations.put(key, translation);
    }

    private String generateKey(int chapterNo, int verseNo) {
        return chapterNo + ":" + verseNo;
    }

    public TranslationBook copy() {
        return new TranslationBook(this);
    }
}
