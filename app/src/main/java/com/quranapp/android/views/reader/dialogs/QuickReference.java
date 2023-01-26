/*
 * Created by Faisal Khan on (c) 29/8/2021.
 */

package com.quranapp.android.views.reader.dialogs;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.quranapp.android.utils.univ.RegexPattern.VERSE_RANGE_PATTERN;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.sheet.SheetDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.adapters.ADPQuickReference;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.QuranTransl;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.LytSheetVerseReferenceBinding;
import com.quranapp.android.databinding.LytSheetVerseReferenceHeaderBinding;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.readerhandler.ActionController;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.views.CardMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

@Deprecated
public class QuickReference implements BookmarkCallbacks, Destroyable {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CallableTaskRunner<ArrayList<Verse>> mTaskRunner = new CallableTaskRunner<>(handler);
    private final SheetDialog mPopup = new SheetDialog();
    private final LytSheetVerseReferenceHeaderBinding mHeaderBinding;
    private final LytSheetVerseReferenceBinding mBinding;
    private final ReaderPossessingActivity mActivity;
    private final ActionController mActionController;

    public QuickReference(ReaderPossessingActivity activity, ActionController actionController) {
        mActivity = activity;
        mActionController = actionController;

        final LayoutInflater inflater = LayoutInflater.from(mActivity);
        mHeaderBinding = LytSheetVerseReferenceHeaderBinding.inflate(inflater);
        ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(MATCH_PARENT, ResUtils.getDimenPx(activity, R.dimen.dmnAppBarHeight));
        mHeaderBinding.getRoot().setLayoutParams(p);
        mBinding = LytSheetVerseReferenceBinding.inflate(inflater);

        init();
    }

    private void init() {
        SheetDialog.SheetDialogParams popupParams = mPopup.getDialogParams();
        popupParams.customHeader = mHeaderBinding.getRoot();
        popupParams.initialBehaviorState = BottomSheetBehavior.STATE_EXPANDED;
        popupParams.resetRoundedCornersOnFullHeight = false;
        popupParams.supportsRoundedCorners = false;
        mPopup.setOnDismissListener(this::destroy);

        mPopup.setContentView(mBinding.getRoot());

        setupContent();
    }

    @Override
    public void destroy() {
        mTaskRunner.cancel();
        mBinding.referenceVerses.setAdapter(null);
        mHeaderBinding.title.setText(null);
        mHeaderBinding.btnBookmark.setVisibility(GONE);
        mHeaderBinding.btnOpen.setVisibility(GONE);
    }

    private void setupContent() {
        mBinding.referenceVerses.setLayoutManager(new LinearLayoutManager(mActivity));
        mHeaderBinding.closeReference.setOnClickListener(v -> close());
    }

    private void initActions(int chapterNo, int[] verseRange, boolean isRange) {
        if (!isRange) {
            mHeaderBinding.btnBookmark.setVisibility(GONE);
            mHeaderBinding.btnOpen.setVisibility(GONE);
            return;
        }

        mHeaderBinding.btnBookmark.setVisibility(VISIBLE);
        mHeaderBinding.btnOpen.setVisibility(VISIBLE);

        mHeaderBinding.btnBookmark.setOnClickListener(v -> {
            if (chapterNo == -1 || verseRange == null) {
                return;
            }

            boolean isBookmarked = mActivity.isBookmarked(chapterNo, verseRange[0], verseRange[1]);

            if (isBookmarked) {
                mActivity.onBookmarkView(chapterNo, verseRange[0], verseRange[1], this);
            } else {
                mActivity.addVerseToBookmark(chapterNo, verseRange[0], verseRange[1], this);
            }
        });

        mHeaderBinding.btnOpen.setOnClickListener(v -> {
            if (chapterNo == -1 || verseRange == null) {
                return;
            }

            mActionController.openVerseReference(chapterNo, verseRange);
            close();
        });
    }

    private void preShow(int chapterNo, int[] verseRange) {
        if (verseRange == null) {
            mHeaderBinding.btnBookmark.setVisibility(GONE);
            return;
        }

        mHeaderBinding.btnBookmark.setVisibility(VISIBLE);
        setupBookmarkIcon(mHeaderBinding.btnBookmark, mActivity.isBookmarked(chapterNo, verseRange[0], verseRange[1]));
    }

    private void initializeShowVerses(Set<String> translSlugs, int chapterNo, int[] verses) {
        makeReferenceVerses(mBinding.referenceVerses, translSlugs, chapterNo, verses, false);
    }

    private void initializeShowSingleVerseOrRange(Set<String> translSlugs, int chapterNo, int[] verseRange) {
        makeReferenceVerses(mBinding.referenceVerses, translSlugs, chapterNo, verseRange, true);
    }

    private void setupBookmarkIcon(ImageView btnBookmark, boolean isBookmarked) {
        int filter = ContextCompat.getColor(mActivity, isBookmarked ? R.color.colorPrimary : R.color.colorIcon);
        btnBookmark.setColorFilter(filter);
        btnBookmark.setImageResource(isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined);
    }

    private void setupReferenceTitle(TextView titleView, int chapterNo, int[] verseInts, boolean isRange) {
        StringBuilder title = new StringBuilder("Quran ").append(chapterNo).append(":");
        if (isRange) {
            title.append(verseInts[0]);
            if (verseInts[0] != verseInts[1]) {
                title.append("-").append(verseInts[1]);
            }
        } else {
            for (int i = 0, l = verseInts.length; i < l; i++) {
                title.append(verseInts[i]);
                if (i < l - 1) {
                    title.append(", ");
                }
            }
        }
        titleView.setText(title.toString());
    }

    private void makeReferenceVerses(RecyclerView container, Set<String> translSlugs,
                                     int chapterNo, int[] versesInts, boolean isRange) {
        mTaskRunner.cancel();

        mTaskRunner.callAsync(new BaseCallableTask<ArrayList<Verse>>() {
            @Override
            public void preExecute() {
                //                mBinding.loader.setVisibility(VISIBLE);
            }

            @Override
            public ArrayList<Verse> call() {
                ArrayList<Verse> verses = new ArrayList<>();

                /*Quran quran = mActivity.mQuranRef.get();
                QuranTransl quranTransl = mActivity.mQuranTranslRef.get();

                Chapter chapter = quran.getChapter(chapterNo);

                if (isRange) {
                    QuranUtils.intRangeIterate(versesInts, verseNo -> prepareVerse(chapter, verseNo, verses, quranTransl));
                } else {
                    for (int verseNo : versesInts) {
                        prepareVerse(chapter, verseNo, verses, quranTransl);
                    }
                }*/

                return verses;
            }

            private void prepareVerse(Chapter chapter, int verseNo, ArrayList<Verse> verses, QuranTransl quranTransl) {
                /*Verse verse = chapter.getVerse(verseNo).copy();
                verse.setIncludeChapterNameInSerial(true);

                ArrayList<Translation> transls = quranTransl.getTranslations(translSlugs, verse.getChapterNo(), verseNo);
                verse.setTranslations(transls);

                verse.setTranslTextSpannable(mActivity.prepareTranslSpannable(verse, transls));

                verses.add(verse);*/
            }

            @Override
            public void onComplete(ArrayList<Verse> verses) {
                ADPQuickReference adp = new ADPQuickReference(mActivity);
                adp.setVerses(verses);
                container.setAdapter(adp);

                setupReferenceTitle(mHeaderBinding.title, chapterNo, versesInts, isRange);
                initActions(chapterNo, versesInts, isRange);

                if (translSlugs == null || translSlugs.isEmpty()) {
                    if (!(mBinding.getRoot().getChildAt(0) instanceof CardMessage)) {
                        CardMessage warning = CardMessage.warning(mActivity, R.string.strMsgTranslNoneSelected);
                        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                        p.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                        mBinding.getRoot().addView(warning, 0, p);

                        ViewGroup.LayoutParams p2 = mBinding.referenceVerses.getLayoutParams();
                        if (p2 instanceof RelativeLayout.LayoutParams) {
                            ((RelativeLayout.LayoutParams) p2).addRule(RelativeLayout.BELOW, warning.getId());
                            mBinding.referenceVerses.setLayoutParams(p2);
                        }
                    }
                }
            }

            @Override
            public void postExecute() {
                //                mBinding.loader.setVisibility(GONE);
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
    public void show(Set<String> translSlugs, int chapterNo, String versesStr) {
        if (TextUtils.isEmpty(versesStr)) {
            showReferenceChapter(translSlugs, chapterNo);
            return;
        }

        final int[] verseRange;

        String[] verses = versesStr.split(",");
        if (verses.length > 1) {
            int[] verseInts = Arrays.stream(verses).mapToInt(Integer::parseInt).toArray();
            showReferenceVerses(translSlugs, chapterNo, verseInts);
        } else {
            Matcher matcher = VERSE_RANGE_PATTERN.matcher(versesStr);
            MatchResult result;
            if (matcher.find() && (result = matcher.toMatchResult()).groupCount() >= 2) {
                final int fromVerse = Integer.parseInt(result.group(1));
                final int toVerse = Integer.parseInt(result.group(2));

                verseRange = new int[]{fromVerse, toVerse};
            } else {
                int verseNo = Integer.parseInt(versesStr);
                verseRange = new int[]{verseNo, verseNo};
            }

            showSingleVerseOrRange(translSlugs, chapterNo, verseRange);
        }
    }

    /**
     * Pattern <reference juz="\d+">()</reference>
     */
    private void showReferenceJuz(Set<String> translSlugs, int juzNo) {
    }

    /**
     * Pattern <reference chapter="\d+">()</reference>
     */
    private void showReferenceChapter(Set<String> translSlugs, int chapterNo) {
        QuranMeta quranMeta = mActivity.mQuranMetaRef.get();

        if (!QuranMeta.isChapterValid(chapterNo)) {
            return;
        }

        PeaceDialog.Builder builder = PeaceDialog.newBuilder(mActivity);
        builder.setTitle("Open chapter?");
        builder.setMessage("Surah " + quranMeta.getChapterName(mActivity, chapterNo));
        builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setPositiveButton(R.string.strLabelOpen,
                (dialog, which) -> ReaderFactory.startChapter(mActivity, translSlugs.toArray(new String[0]), false, chapterNo));
        builder.show();
    }

    /**
     * Call it to show verses of same chapter but with different numbers.
     *
     * @param verses comma separated verses from pattern <reference chapter="\d+" verses="\d+,\d+,...">()</reference>
     */
    private void showReferenceVerses(Set<String> translSlugs, int chapterNo, int[] verses) {
        if (!QuranMeta.isChapterValid(chapterNo)) {
            return;
        }

        close();

        mPopup.show(mActivity.getSupportFragmentManager());

        preShow(chapterNo, null);
        initializeShowVerses(translSlugs, chapterNo, verses);
    }

    /**
     * Call it show single verse of or a  verse range.
     *
     * @param verseRange contains two number. If both are same, means single verse otherwise a range.
     *                   Pattern <reference chapter="\d+" verses="\d+-\d+">()</reference>
     *                   or Pattern <reference chapter="\d+" verses="\d+">()</reference>
     */
    public void showSingleVerseOrRange(Set<String> translSlugs, int chapterNo, int[] verseRange) {
        if (!QuranMeta.isChapterValid(chapterNo) ||
                !mActivity.mQuranMetaRef.get().isVerseRangeValid4Chapter(chapterNo, verseRange[0], verseRange[1])) {
            return;
        }

        close();

        mPopup.show(mActivity.getSupportFragmentManager());

        preShow(chapterNo, verseRange);
        initializeShowSingleVerseOrRange(translSlugs, chapterNo, verseRange);
    }

    public void close() {
        try {
            mPopup.dismissAllowingStateLoss();
        } catch (Exception ignored) {}
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        setupBookmarkIcon(mHeaderBinding.btnBookmark, false);
    }

    @Override
    public void onBookmarkAdded(BookmarkModel model) {
        setupBookmarkIcon(mHeaderBinding.btnBookmark, true);
    }
}
