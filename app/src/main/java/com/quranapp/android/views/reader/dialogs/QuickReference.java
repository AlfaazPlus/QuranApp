/*
 * Created by Faisal Khan on (c) 29/8/2021.
 */

package com.quranapp.android.views.reader.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import static com.quranapp.android.utils.univ.RegexPattern.VERSE_RANGE_PATTERN;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.adapters.ADPQuickReference;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.LytSheetVerseReferenceBinding;
import com.quranapp.android.databinding.LytSheetVerseReferenceHeaderBinding;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.views.CardMessage;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import kotlin.Pair;

public class QuickReference extends PeaceBottomSheet implements BookmarkCallbacks, Destroyable {
    private final CallableTaskRunner<List<Verse>> mTaskRunner = new CallableTaskRunner<>();
    private ReaderPossessingActivity mActivity;
    private LytSheetVerseReferenceBinding mBinding;

    private Set<String> mTranslSlugs;
    private int mChapterNo;
    private int[] mVerses;
    private Pair<Integer, Integer> mVerseRange;
    private boolean mIsVerseRange;

    public QuickReference() {
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArray("translSlugs", mTranslSlugs.toArray(new String[0]));
        outState.putInt("chapterNo", mChapterNo);
        outState.putIntArray("verses", mVerses);
        outState.putSerializable("verseRange", mVerseRange);
        outState.putBoolean("isVerseRange", mIsVerseRange);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mTranslSlugs = Arrays.stream(savedInstanceState.getStringArray("translSlugs")).collect(Collectors.toSet());
            mChapterNo = savedInstanceState.getInt("chapterNo");
            mVerses = savedInstanceState.getIntArray("verses");
            mVerseRange = (Pair<Integer, Integer>) savedInstanceState.getSerializable("verseRange");
            mIsVerseRange = savedInstanceState.getBoolean("isVerseRange");
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ReaderPossessingActivity) {
            mActivity = (ReaderPossessingActivity) context;
        }
    }

    @Override
    public void destroy() {
        mTaskRunner.cancel();

        mTranslSlugs = null;
        mChapterNo = -1;
        mIsVerseRange = false;
        mVerseRange = null;
        mVerses = null;

        if (mBinding == null) {
            return;
        }

        ViewKt.removeView(mBinding.getRoot().findViewById(R.id.message));

        mBinding.referenceVerses.setAdapter(null);

        LytSheetVerseReferenceHeaderBinding header = mBinding.header;
        header.title.setText(null);
        header.btnBookmark.setVisibility(GONE);
        header.btnOpen.setVisibility(GONE);
    }

    @Override
    protected void setupDialogInternal(Dialog dialog, int style, PeaceBottomSheetParams params) {
        if (mActivity == null || (mIsVerseRange && mVerseRange == null) || (!mIsVerseRange && mVerses == null)) {
            return;
        }

        if (mBinding == null) {
            AsyncLayoutInflater inflater = new AsyncLayoutInflater(dialog.getContext());
            inflater.inflate(R.layout.lyt_sheet_verse_reference, null, (view, resid, parent) -> {
                mBinding = LytSheetVerseReferenceBinding.bind(view);
                setup(mActivity, mBinding);
                setupContent(mActivity, dialog, mBinding, params);
            });
        } else {
            setupContent(mActivity, dialog, mBinding, params);
        }
    }

    private void setupContent(
        ReaderPossessingActivity actvt, Dialog dialog,
        LytSheetVerseReferenceBinding binding, PeaceBottomSheetParams params
    ) {
        binding.referenceVerses.setVisibility(GONE);
        binding.loader.setVisibility(VISIBLE);

        ViewKt.removeView(binding.getRoot());
        dialog.setContentView(binding.getRoot());
        setupDialogStyles(dialog, binding.getRoot(), params);


        if (mIsVerseRange) {
            initializeShowSingleVerseOrRange(actvt, binding, mTranslSlugs, mChapterNo, mVerseRange);
        } else {
            initializeShowVerses(actvt, binding, mTranslSlugs, mChapterNo, mVerses);
        }
    }

    private void setup(Context ctx, LytSheetVerseReferenceBinding binding) {
        binding.referenceVerses.setLayoutManager(new LinearLayoutManager(ctx));
        binding.header.closeReference.setOnClickListener(v -> dismiss());
        binding.header.getRoot().setElevation(Dimen.dp2px(ctx, 4));
    }

    private void initActions(
        ReaderPossessingActivity actvt,
        LytSheetVerseReferenceBinding binding,
        int chapterNo,
        @Nullable Pair<Integer, Integer> verseRange
    ) {
        LytSheetVerseReferenceHeaderBinding header = binding.header;
        if (verseRange == null) {
            header.btnBookmark.setVisibility(GONE);
            header.btnOpen.setVisibility(GONE);
            return;
        }

        header.btnBookmark.setVisibility(VISIBLE);
        header.btnOpen.setVisibility(VISIBLE);

        header.btnBookmark.setOnClickListener(v -> {
            if (chapterNo == -1) {
                return;
            }

            boolean isBookmarked = actvt.isBookmarked(chapterNo, verseRange.getFirst(), verseRange.getSecond());

            if (isBookmarked) {
                actvt.onBookmarkView(chapterNo, verseRange.getFirst(), verseRange.getSecond(), this);
            } else {
                actvt.addVerseToBookmark(chapterNo, verseRange.getFirst(), verseRange.getSecond(), this);
            }
        });

        header.btnOpen.setOnClickListener(v -> {
            if (chapterNo == -1) {
                return;
            }

            actvt.mActionController.openVerseReference(chapterNo, verseRange);
            dismiss();
        });
    }

    private void preShow(
        ReaderPossessingActivity actvt,
        LytSheetVerseReferenceHeaderBinding header,
        int chapterNo,
        Pair<Integer, Integer> verseRange
    ) {
        if (verseRange == null) {
            header.btnBookmark.setVisibility(GONE);
            return;
        }

        header.btnBookmark.setVisibility(VISIBLE);
        setupBookmarkIcon(
            header.btnBookmark,
            actvt.isBookmarked(chapterNo, verseRange.getFirst(), verseRange.getSecond())
        );
    }

    private void initializeShowVerses(
        ReaderPossessingActivity actvt,
        LytSheetVerseReferenceBinding binding,
        Set<String> translSlugs,
        int chapterNo,
        int[] verses
    ) {
        preShow(actvt, binding.header, chapterNo, null);
        makeReferenceVerses(actvt, binding, translSlugs, chapterNo, null, verses);
    }

    private void initializeShowSingleVerseOrRange(
        ReaderPossessingActivity actvt,
        LytSheetVerseReferenceBinding binding,
        Set<String> translSlugs,
        int chapterNo,
        Pair<Integer, Integer> verseRange
    ) {
        preShow(actvt, binding.header, chapterNo, verseRange);
        makeReferenceVerses(actvt, binding, translSlugs, chapterNo, verseRange, null);
    }

    private void setupBookmarkIcon(ImageView btnBookmark, boolean isBookmarked) {
        int filter = ContextCompat.getColor(getContext(), isBookmarked ? R.color.colorPrimary : R.color.colorIcon);
        btnBookmark.setColorFilter(filter);
        btnBookmark.setImageResource(
            isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined);
    }

    private void setupReferenceTitle(
        TextView titleView,
        int chapterNo,
        @Nullable Pair<Integer, Integer> verseRange,
        @Nullable int[] versesInts
    ) {
        StringBuilder title = new StringBuilder("Quran ").append(chapterNo).append(":");
        if (verseRange != null) {
            title.append(verseRange.getFirst());
            if (!Objects.equals(verseRange.getFirst(), verseRange.getSecond())) {
                title.append("-").append(verseRange.getSecond());
            }
        } else {
            for (int i = 0, l = versesInts.length; i < l; i++) {
                title.append(versesInts[i]);
                if (i < l - 1) {
                    title.append(", ");
                }
            }
        }
        titleView.setText(title.toString());
    }

    private void makeReferenceVerses(
        ReaderPossessingActivity actvt,
        LytSheetVerseReferenceBinding binding,
        Set<String> translSlugs,
        int chapterNo,
        @Nullable Pair<Integer, Integer> verseRange,
        @Nullable int[] versesInts
    ) {
        QuranMeta.prepareInstance(actvt, quranMeta -> {
            if (!QuranMeta.isChapterValid(chapterNo)) {
                binding.loader.setVisibility(GONE);
                return;
            }
            Quran.prepareInstance(actvt, quranMeta, quran -> {
                // --
                makeReferenceVersesAsync(actvt,
                    binding,
                    quranMeta,
                    quran,
                    translSlugs,
                    chapterNo,
                    verseRange,
                    versesInts
                );
            });
        });
    }

    private void makeReferenceVersesAsync(
        ReaderPossessingActivity actvt,
        LytSheetVerseReferenceBinding binding,
        QuranMeta quranMeta,
        Quran quran,
        Set<String> translSlugs,
        int chapterNo,
        @Nullable Pair<Integer, Integer> verseRange,
        @Nullable int[] versesInts
    ) {
        mTaskRunner.cancel();

        mTaskRunner.callAsync(new BaseCallableTask<List<Verse>>() {
            @Override
            public void preExecute() {
                binding.referenceVerses.setVisibility(GONE);
                binding.loader.setVisibility(VISIBLE);
            }

            @Override
            public List<Verse> call() throws Exception {
                List<Verse> verses = new ArrayList<>();
                Chapter chapter = quran.getChapter(chapterNo);
                if (chapter == null) {
                    throw new Exception(
                        "could not get chapter object from quranMeta for the chapterNo. [QuickReference]");
                }

                Map<String, QuranTranslBookInfo> booksInfo = actvt.mTranslFactory.getTranslationBooksInfoValidated(
                    translSlugs
                );

                if (verseRange != null) {
                    List<List<Translation>> translations = actvt.mTranslFactory.getTranslationsVerseRange(
                        translSlugs,
                        chapterNo,
                        verseRange.getFirst(),
                        verseRange.getSecond()
                    );
                    QuranUtils.intRangeIterateWithIndex(verseRange, (index, verseNo) -> {
                        // --
                        prepareVerse(chapter, verseNo, verses, translations.get(index), booksInfo);
                    });
                } else {
                    assert versesInts != null;

                    List<List<Translation>> translations = actvt.mTranslFactory.getTranslationsDistinctVerses(
                        translSlugs,
                        chapterNo,
                        versesInts
                    );
                    for (int i = 0, l = versesInts.length; i < l; i++) {
                        int verseNo = versesInts[i];
                        if (quranMeta.isVerseValid4Chapter(chapterNo, verseNo)) {
                            prepareVerse(chapter, verseNo, verses, translations.get(i), booksInfo);
                        }
                    }
                }

                return verses;
            }

            private void prepareVerse(
                Chapter chapter, int verseNo, List<Verse> verses,
                List<Translation> translations, Map<String, QuranTranslBookInfo> booksInfo
            ) {
                Verse verse = chapter.getVerse(verseNo).copy();

                if (actvt.mVerseDecorator.isKFQPCScript()) {
                    actvt.mVerseDecorator.refreshQuranTextFonts(new Pair<>(verse.pageNo, verse.pageNo));
                } else {
                    actvt.mVerseDecorator.refreshQuranTextFonts(null);
                }

                verse.setIncludeChapterNameInSerial(true);
                verse.setTranslations(translations);
                verse.arabicTextSpannable = SPReader.getArabicTextEnabled(actvt) ? actvt.prepareVerseText(verse) : null;
                verse.translTextSpannable = actvt.prepareTranslSpannable(verse, translations, booksInfo);

                verses.add(verse);
            }

            @Override
            public void onComplete(List<Verse> verses) {
                Log.d(verses);
                ADPQuickReference adp = new ADPQuickReference(actvt);
                adp.setVerses(verses);
                binding.referenceVerses.setAdapter(adp);

                setupReferenceTitle(binding.header.title, chapterNo, verseRange, versesInts);
                initActions(actvt, binding, chapterNo, verseRange);


                ViewKt.removeView(mBinding.getRoot().findViewById(R.id.message));
                if (translSlugs == null || translSlugs.isEmpty()) {
                    if (!(binding.getRoot().getChildAt(0) instanceof CardMessage)) {
                        CardMessage msgView = CardMessage.warning(actvt, R.string.strMsgTranslNoneSelected);
                        msgView.setId(R.id.message);
                        if (actvt instanceof ActivityReader) {
                            msgView.setActionText(actvt.str(R.string.strTitleSettings), () -> {
                                // --
                                ((ActivityReader) actvt).mBinding.readerHeader.openReaderSetting(
                                    ActivitySettings.SETTINGS_TRANSLATION);
                            });
                        }

                        int headerPos = binding.getRoot().indexOfChild(binding.header.getRoot());
                        binding.getRoot().addView(msgView, headerPos + 1);
                    }
                }
            }

            @Override
            public void onFailed(@NonNull Exception e) {
                super.onFailed(e);
                Logger.reportError(e);
            }

            @Override
            public void postExecute() {
                binding.loader.setVisibility(GONE);
                binding.referenceVerses.setVisibility(VISIBLE);
            }
        });
    }

    /**
     * @param versesStr could be of different patterns.
     *                  eg., -
     *                  7,8 (Verses)
     *                  7-8 (Verse range)
     *                  7 (Single verse)
     */
    public void show(ReaderPossessingActivity actvt, Set<String> translSlugs, int chapterNo, String versesStr) {
        if (TextUtils.isEmpty(versesStr)) {
            showReferenceChapter(actvt, translSlugs, chapterNo);
            return;
        }

        final Pair<Integer, Integer> verseRange;

        String[] verses = versesStr.split(",");
        if (verses.length > 1) {
            int[] versesInts = Arrays.stream(verses).mapToInt(Integer::parseInt).sorted().toArray();
            showReferenceVerses(actvt, translSlugs, chapterNo, versesInts);
        } else {
            Matcher matcher = VERSE_RANGE_PATTERN.matcher(versesStr);
            MatchResult result;
            if (matcher.find() && (result = matcher.toMatchResult()).groupCount() >= 2) {
                final int fromVerse = Integer.parseInt(result.group(1));
                final int toVerse = Integer.parseInt(result.group(2));

                verseRange = new Pair<>(fromVerse, toVerse);
            } else {
                int verseNo = Integer.parseInt(versesStr);
                verseRange = new Pair<>(verseNo, verseNo);
            }

            showSingleVerseOrRange(actvt, translSlugs, chapterNo, verseRange);
        }
    }

    /**
     * Pattern <reference juz="\d+">()</reference>
     */
    private void showReferenceJuz(Set<String> translSlugs, int juzNo) {
    }

    /**
     * Pattern \<reference chapter="\d+">()</reference>
     */
    private void showReferenceChapter(ReaderPossessingActivity actvt, Set<String> translSlugs, int chapterNo) {
        QuranMeta quranMeta = actvt.mQuranMetaRef.get();

        if (!QuranMeta.isChapterValid(chapterNo)) {
            return;
        }

        PeaceDialog.Builder builder = PeaceDialog.newBuilder(actvt);
        builder.setTitle("Open chapter?");
        builder.setMessage("Surah " + quranMeta.getChapterName(actvt, chapterNo));
        builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setPositiveButton(R.string.strLabelOpen,
            (dialog, which) -> ReaderFactory.startChapter(actvt, translSlugs.toArray(new String[0]), false,
                chapterNo));
        builder.show();
    }

    /**
     * Call it to show verses of same chapter but with different numbers.
     *
     * @param verses comma separated verses from pattern <reference chapter="\d+" verses="\d+,\d+,...">()</reference>
     */
    private void showReferenceVerses(ReaderPossessingActivity actvt, Set<String> translSlugs, int chapterNo, int[] verses) {
        if (!QuranMeta.isChapterValid(chapterNo)) {
            return;
        }

        dismiss();

        mActivity = actvt;
        mTranslSlugs = translSlugs;
        mChapterNo = chapterNo;
        mVerses = verses;
        mIsVerseRange = false;

        show(actvt.getSupportFragmentManager());
    }

    /**
     * Call it show single verse of or a  verse range.
     *
     * @param verseRange contains two number. If both are same, means single verse otherwise a range.
     *                   Pattern <reference chapter="\d+" verses="\d+-\d+">()</reference>
     *                   or Pattern <reference chapter="\d+" verses="\d+">()</reference>
     */
    public void showSingleVerseOrRange(
        ReaderPossessingActivity actvt,
        Set<String> translSlugs,
        int chapterNo,
        Pair<Integer, Integer> verseRange
    ) {
        verseRange = QuranUtils.correctVerseInRange(actvt.mQuranMetaRef.get(), chapterNo, verseRange);
        if (!QuranMeta.isChapterValid(chapterNo) ||
            !actvt.mQuranMetaRef.get().isVerseRangeValid4Chapter(
                chapterNo,
                verseRange.getFirst(),
                verseRange.getSecond()
            )
        ) {
            return;
        }
        dismiss();

        mActivity = actvt;
        mTranslSlugs = translSlugs;
        mChapterNo = chapterNo;
        mVerseRange = verseRange;
        mIsVerseRange = true;

        show(actvt.getSupportFragmentManager());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!isShowing()) {
            if (mBinding != null) {
                mBinding.referenceVerses.setAdapter(null);
            }
            mBinding = null;
        }
    }

    @Override
    public void dismiss() {
        try {
            dismissAllowingStateLoss();
        } catch (Exception ignored) {}
        destroy();
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        if (mBinding == null) {
            return;
        }
        setupBookmarkIcon(mBinding.header.btnBookmark, false);
    }

    @Override
    public void onBookmarkAdded(BookmarkModel model) {
        if (mBinding == null) {
            return;
        }
        setupBookmarkIcon(mBinding.header.btnBookmark, true);
    }
}
