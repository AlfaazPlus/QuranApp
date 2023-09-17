package com.quranapp.android.views;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.span.LineHeightSpan2;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityBookmark;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.LytVotdBinding;
import com.quranapp.android.databinding.LytVotdContentBinding;
import com.quranapp.android.db.bookmark.BookmarkDBHelper;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.reader_managers.ReaderVerseDecorator;
import com.quranapp.android.suppliments.BookmarkViewer;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.reader.QuranScriptUtilsKt;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.utils.verse.VerseUtils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.Pair;

/*
 * A very ugly implementation of verse of the day view
 */
public class VOTDView extends FrameLayout implements Destroyable, BookmarkCallbacks {
    private final CallableTaskRunner<Pair<QuranTranslBookInfo, Translation>> taskRunner = new CallableTaskRunner<>();
    private final BookmarkDBHelper mBookmarkDBHelper;
    private final BookmarkViewer mBookmarkViewer;
    private final LytVotdBinding mBinding;
    private final ReaderVerseDecorator mVerseDecorator;
    private final int mColorNormal;
    private final int mColorBookmarked;
    private LytVotdContentBinding mContent;
    private int mChapterNo = -1;
    private int mVerseNo = -1;
    private String mLastScript;
    private String mLastTranslationSlug;
    private boolean mLastArabicTextEnabled;
    private SpannableString mLastTranslationText;
    private SpannableString mLastAuthorText;
    private CharSequence mArText;

    public VOTDView(@NonNull Context context) {
        this(context, null);
    }

    public VOTDView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VOTDView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBookmarkDBHelper = new BookmarkDBHelper(context);
        mBookmarkViewer = new BookmarkViewer(context, new AtomicReference<>(null), mBookmarkDBHelper, this);
        mVerseDecorator = new ReaderVerseDecorator(context);
        mColorNormal = ContextCompat.getColor(context, R.color.white3);
        mColorBookmarked = ContextCompat.getColor(context, R.color.colorPrimary);

        mBinding = LytVotdBinding.inflate(LayoutInflater.from(context));
        mBinding.getRoot().setVisibility(GONE);

        addView(mBinding.getRoot());
    }

    private void initContent() {
        if (mContent == null) {
            mContent = LytVotdContentBinding.inflate(LayoutInflater.from(getContext()));
            mBinding.container.addView(mContent.getRoot());
        }
    }

    private void initActions(QuranMeta quranMeta) {
        mBinding.read.setVisibility(VISIBLE);

        mContent.btnTafsir.setOnClickListener(v -> ReaderFactory.startTafsir(getContext(), mChapterNo, mVerseNo));
        mContent.votdBookmark.setOnClickListener(v -> bookmark(mChapterNo, mVerseNo));
        mContent.votdBookmark.setOnLongClickListener(v -> {
            v.getContext().startActivity(new Intent(v.getContext(), ActivityBookmark.class));
            return true;
        });

        mBinding.read.setOnClickListener(v -> {
            if (QuranMeta.isChapterValid(mChapterNo) && quranMeta.isVerseValid4Chapter(mChapterNo, mVerseNo)) {
                ReaderFactory.startVerse(getContext(), mChapterNo, mVerseNo);
            }
        });
    }

    @Override
    public void destroy() {
        mBookmarkDBHelper.close();
    }

    public synchronized void refresh(QuranMeta quranMeta) {
        if (mBookmarkViewer != null) {
            mBookmarkViewer.setQuranMeta(quranMeta);
        }

        installVotdContents(quranMeta);
    }

    private void initVotd(QuranMeta quranMeta, Quran quran, Runnable runnable) {
        mBinding.getRoot().setVisibility(GONE);
        initActions(quranMeta);

        VerseUtils.getVOTD(getContext(), quranMeta, quran, (chapterNo, verseNo) -> {
            mChapterNo = chapterNo;
            mVerseNo = verseNo;
            if (runnable != null) runnable.run();
        });
    }

    public void installVotdContents(QuranMeta quranMeta) {
        initContent();

        if (!QuranMeta.isChapterValid(mChapterNo) || !quranMeta.isVerseValid4Chapter(mChapterNo, mVerseNo)) {
            initVotd(quranMeta, null, () -> installVotdContents(quranMeta));
            return;
        }

        setupVOTD(quranMeta);
        mBinding.getRoot().setVisibility(VISIBLE);
    }

    private void setupVOTD(QuranMeta quranMeta) {
        if (mChapterNo == -1 || mVerseNo == -1) {
            return;
        }

        prepareQuran(getContext(), quranMeta);
        setupVOTDBookmarkIcon(mBookmarkDBHelper.isBookmarked(mChapterNo, mVerseNo, mVerseNo));
    }

    private void prepareQuran(Context context, QuranMeta quranMeta) {
        if (!Objects.equals(mLastScript, SPReader.getSavedScript(context))) {
            Quran.prepareInstance(context, quranMeta, quran -> setupQuran(quranMeta, quran));
        } else {
            prepareTransl(getContext(), null);
        }
    }

    private void setupQuran(QuranMeta quranMeta, Quran quran) {
        if (!quran.getVerse(mChapterNo, mVerseNo).isIdealForVOTD()) {
            initVotd(quranMeta, quran, () -> installVotdContents(quranMeta));
            return;
        }

        Chapter chapter = quran.getChapter(mChapterNo);

        String info = getContext().getString(R.string.strLabelVerseWithChapNameAndNo, chapter.getName(), mChapterNo,
            mVerseNo);
        mContent.verseInfo.setText(info);

        Verse verse = quran.getChapter(mChapterNo).getVerse(mVerseNo);

        mVerseDecorator.refresh();
        mVerseDecorator.refreshQuranTextFonts(
            mVerseDecorator.isKFQPCScript()
                ? new Pair<>(verse.pageNo, verse.pageNo)
                : null
        );

        final int txtSizeRes = QuranScriptUtilsKt.getQuranScriptVerseTextSizeSmallRes(quran.getScript());
        int verseTextSize = ContextKt.getDimenPx(getContext(), txtSizeRes);

        mArText = mVerseDecorator.prepareArabicText(verse, verseTextSize);
        prepareTransl(getContext(), quran.getScript());

        mLastScript = quran.getScript();
    }

    private void prepareTransl(Context context, String scriptKey) {
        taskRunner.callAsync(new BaseCallableTask<>() {
            QuranTranslationFactory factory;

            @Override
            public void preExecute() {
                factory = new QuranTranslationFactory(context);
            }

            @Override
            public void postExecute() {
                if (factory != null) factory.close();
            }

            @Override
            public Pair<QuranTranslBookInfo, Translation> call() {
                QuranTranslBookInfo bookInfo = obtainOptimalSlug(context, factory);
                if (Objects.equals(mLastTranslationSlug, bookInfo.getSlug())) {
                    return null;
                }

                Translation translation = factory.getTranslationsSingleSlugVerse(
                    bookInfo.getSlug(),
                    mChapterNo,
                    mVerseNo
                );
                return new Pair<>(bookInfo, translation);
            }

            @Override
            public void onComplete(@Nullable Pair<QuranTranslBookInfo, Translation> result) {
                // if translation has changed, update with new translation
                if (result != null) {
                    setupTranslation(mArText, result.getFirst(), result.getSecond());
                }
                // Or if script has changed, update with old translation
                else if (scriptKey != null && !Objects.equals(mLastScript, scriptKey)) {
                    setupTranslation(mArText, null, null);
                }
                // Or if arabic text enable status has changed, update with old script and old translation
                else if (SPReader.getArabicTextEnabled(getContext()) != mLastArabicTextEnabled) {
                    showText(mArText, mLastTranslationText, mLastAuthorText);
                }
            }
        });
    }

    private QuranTranslBookInfo obtainOptimalSlug(Context ctx, QuranTranslationFactory factory) {
        Set<String> savedTranslations = SPReader.getSavedTranslations(ctx);

        QuranTranslBookInfo bookInfo = null;
        for (String savedSlug : savedTranslations) {
            if (!TranslUtils.isTransliteration(savedSlug)) {
                bookInfo = factory.getTranslationBookInfo(savedSlug);
                break;
            }
        }

        if (bookInfo == null) {
            bookInfo = factory.getTranslationBookInfo(TranslUtils.TRANSL_SLUG_DEFAULT);
        }

        return bookInfo;
    }

    private void setupTranslation(CharSequence arText, QuranTranslBookInfo bookInfo, Translation translation) {
        // If translation is null, it means translation was not changed, update with old translation
        if (bookInfo == null || translation == null || TextUtils.isEmpty(translation.getText())) {
            showText(arText, mLastTranslationText, mLastAuthorText);
            return;
        }

        mLastTranslationSlug = translation.getBookSlug();

        // Remove footnote markers etc
        String transl = StringUtils.removeHTML(translation.getText(), false);
        int txtSize = ContextKt.getDimenPx(getContext(), R.dimen.dmnCommonSize1_5);
        SpannableString translText = mVerseDecorator.setupTranslText(transl, -1, txtSize, translation.isUrdu());

        SpannableString authorText = null;
        String author = bookInfo.getDisplayName(true);
        if (!TextUtils.isEmpty(author)) {
            author = StringUtils.HYPHEN + " " + author;
            authorText = mVerseDecorator.setupAuthorText(author, translation.isUrdu());
        }

        mLastTranslationText = translText;
        mLastAuthorText = authorText;

        showText(arText, translText, authorText);
    }

    private void showText(CharSequence arText, SpannableString transl, SpannableString author) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        mLastArabicTextEnabled = SPReader.getArabicTextEnabled(getContext());

        boolean showArabicText = mLastArabicTextEnabled && !TextUtils.isEmpty(arText);
        boolean showTranslation = !TextUtils.isEmpty(transl);

        // Show arabic text if enabled
        if (showArabicText) {
            sb.append(arText);
        }

        // Show translation if exists
        if (showTranslation) {
            if (showArabicText) sb.append("\n\n");
            sb.append(transl);
        }

        // Show author if translation exists
        if (showTranslation) {
            sb.append("\n");
            author.setSpan(new LineHeightSpan2(15, true, false), 0, author.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(author);
        }

        mContent.text.setText(sb, TextView.BufferType.SPANNABLE);
        mContent.text.requestLayout();
        mContent.text.invalidate();

        ViewKt.removeView(findViewById(R.id.loader));
    }

    private void setupVOTDBookmarkIcon(boolean isBookmarked) {
        int iconRes = isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined;
        mContent.votdBookmark.setImageResource(iconRes);
        mContent.votdBookmark.setColorFilter(isBookmarked ? mColorBookmarked : mColorNormal);
    }

    private void bookmark(int chapterNo, int verseNo) {
        boolean isBookmarked = mBookmarkDBHelper.isBookmarked(chapterNo, verseNo, verseNo);

        if (isBookmarked) {
            mBookmarkViewer.view(chapterNo, verseNo, verseNo);
        } else {
            mBookmarkDBHelper.addToBookmark(chapterNo, verseNo, verseNo, null, model -> {
                setupVOTDBookmarkIcon(true);
                mBookmarkViewer.edit(model);
            });
        }
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        setupVOTDBookmarkIcon(false);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof MarginLayoutParams) {
            ((MarginLayoutParams) params).bottomMargin = Dimen.dp2px(getContext(), 2);
        }
        super.setLayoutParams(params);
    }
}
