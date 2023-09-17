package com.quranapp.android.activities.reference;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static com.quranapp.android.adapters.reference.ADPReferenceVerses.VIEWTYPE_DESCRIPTION;
import static com.quranapp.android.adapters.reference.ADPReferenceVerses.VIEWTYPE_TITLE;
import static com.quranapp.android.adapters.reference.ADPReferenceVerses.VIEWTYPE_VERSE;
import static com.quranapp.android.utils.IntentUtils.INTENT_ACTION_OPEN_REFERENCE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.adapters.reference.ADPReferenceVerses;
import com.quranapp.android.components.ReferenceVerseItemModel;
import com.quranapp.android.components.ReferenceVerseModel;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.ActivityReferenceBinding;
import com.quranapp.android.databinding.LytChipgroupBinding;
import com.quranapp.android.utils.quran.parser.ParserUtils;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.thread.runner.RunnableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseRunnableTask;
import com.quranapp.android.utils.univ.Keys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import kotlin.Pair;

public class ActivityReference extends ReaderPossessingActivity {
    public Set<String> mSelectedTranslSlugs;
    private final RunnableTaskRunner mTaskRunner = new RunnableTaskRunner();
    private ActivityReferenceBinding mBinding;

    private QuranMeta mQuranMeta;
    private Quran mQuran;
    private ADPReferenceVerses mVersesAdapter;
    private int mLastScroll;
    private boolean mLastTitleShown;
    private int mSelectedSuggChapterNo;

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGPageVariableInverse);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_reference;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("initialSelectedChipId", mSelectedSuggChapterNo);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void preReaderReady(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedSuggChapterNo = savedInstanceState.getInt("initialSelectedChipId", 0);
        }

        mBinding = ActivityReferenceBinding.bind(activityView);
        mVerseUnhighlightedBGColor = color(R.color.colorBGCardVariable);
        initHeader();
    }

    @Override
    protected void onReaderReady(@NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        mQuranMeta = mQuranMetaRef.get();
        mQuran = mQuranRef.get().copy();

        init(intent);
    }

    /*private void prepareTestModel(Intent intent) {
        String title = "Ramadan Important Verses";
        String desc = "We have gathered some verses from the quran";

        List<String> verses = ParserUtils.prepareVersesList("51:12,16:8,15:12,6:12,14:12,55:5-8", true);
        List<Integer> chapters = ParserUtils.prepareChaptersList(verses);
        String[] slugs = {TranslUtils.TRANSL_SLUG_UR_JUNAGARHI};
        ReferenceVerseModel refVerseModel = new ReferenceVerseModel(true, title, desc, slugs, chapters, verses);
        intent.putExtra(Keys.KEY_REFERENCE_VERSE_MODEL, refVerseModel);
    }*/

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mSelectedSuggChapterNo = 0;
        init(intent);
    }

    private void initHeader() {
        int bgColor = color(R.color.colorBGPageVariableInverse);
        Drawable bg = DrawableUtils.createBackground(bgColor, null);
        mBinding.titleHeader.setBackground(bg);

        mBinding.back.setOnClickListener(v -> finish());
        mBinding.verses.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                mLastScroll += dy;
                boolean titleShown = mLastScroll > 150;

                if (titleShown && !mLastTitleShown) {
                    mLastTitleShown = mBinding.header.getElevation() >= 8;
                    resetAppBar(Math.min(mBinding.header.getElevation() + dy, 8), true);
                } else if (!titleShown && mLastTitleShown) {
                    mLastTitleShown = mBinding.header.getElevation() > 0;
                    resetAppBar(Math.max(mBinding.header.getElevation() + dy, 0), false);
                }
            }
        });
    }

    private void resetAppBar(float elevation, boolean titleShown) {
        mBinding.header.setElevation(elevation);
        mBinding.title.setVisibility(titleShown ? VISIBLE : GONE);
    }

    private void init(Intent intent) {
        ReferenceVerseModel refModel = null;

        try {
            refModel = validateIntent(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (refModel == null) {
            refModel = (ReferenceVerseModel) intent.getSerializableExtra(Keys.KEY_REFERENCE_VERSE_MODEL);
        }

        if (refModel == null) {
            return;
        }

        String[] requestedTransls = refModel.getTranslSlugs();

        if (requestedTransls.length == 0) {
            Set<String> savedTranslations = SPReader.getSavedTranslations(this);
            String first = savedTranslations.stream().findFirst().orElse("");
            mSelectedTranslSlugs = new HashSet<>();
            mSelectedTranslSlugs.add(first);
        } else {
            mSelectedTranslSlugs = new TreeSet<>(Arrays.asList(requestedTransls));
        }

        mSelectedTranslSlugs = mSelectedTranslSlugs.stream().filter(s -> !TextUtils.isEmpty(s)).collect(
            Collectors.toSet());

        if (mSelectedTranslSlugs.size() == 0) {
            mSelectedTranslSlugs = TranslUtils.defaultTranslationSlugs();
        }

        initContent(refModel);
    }

    private ReferenceVerseModel validateIntent(Intent intent) {
        if (!INTENT_ACTION_OPEN_REFERENCE.equalsIgnoreCase(intent.getAction())) {
            return null;
        }

        final String title = intent.getStringExtra("title");
        final String desc = intent.getStringExtra("description");
        final String[] translSlugs = intent.getStringArrayExtra("translations");
        final boolean showChapterSugg = intent.getBooleanExtra("showChapterSuggestions", true);
        final List<String> verses = ParserUtils.prepareVersesList(intent.getStringExtra("verses"), true);
        final List<Integer> chapters = ParserUtils.prepareChaptersList(verses);

        return new ReferenceVerseModel(showChapterSugg, title, desc, translSlugs, chapters, verses);
    }

    private void initContent(ReferenceVerseModel refModel) {
        if (mSelectedSuggChapterNo < 0) {
            mSelectedSuggChapterNo = 0;
        }

        mBinding.title.setText(refModel.getTitle());

        if (refModel.showChaptersSugg()) {
            initChaptersSugg(this, refModel);
        }

        mBinding.verses.setLayoutManager(new LinearLayoutManager(this));
        mVersesAdapter = new ADPReferenceVerses(this, refModel);

        resetVerses(mSelectedSuggChapterNo, refModel);
    }

    private void initChaptersSugg(Context context, ReferenceVerseModel verseModel) {
        final List<Integer> chapters = verseModel.getChapters();
        if (chapters.size() <= 1) {
            return;
        }

        LytChipgroupBinding chaptersGroupBinding = LytChipgroupBinding.inflate(LayoutInflater.from(context),
            mBinding.header, false);
        initChaptersSuggAsync(chaptersGroupBinding, chapters, verseModel);
    }

    private void initChaptersSuggAsync(
        LytChipgroupBinding chipGroupBinding,
        List<Integer> chapters, ReferenceVerseModel verseModel
    ) {
        ChipGroup chaptersGroup = chipGroupBinding.chipGroup;
        chaptersGroup.setChipSpacingHorizontal(dp2px(5));

        mTaskRunner.runAsync(new BaseRunnableTask() {
            @Override
            public void runTask() {
                makeChapterChip(0, chaptersGroup, str(R.string.strLabelAllChapters));
                for (int chapterNo : chapters) {
                    makeChapterChip(chapterNo, chaptersGroup,
                        mQuranMeta.getChapterName(ActivityReference.this, chapterNo));
                }
            }

            @Override
            public void onComplete() {
                // Check "mSelectedSuggChapterNo" id chip before setting listener to prevent adapter
                // to be invoked by the listener for the first time.
                chaptersGroup.check(mSelectedSuggChapterNo);
                chaptersGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == 0 || chapters.contains(checkedId)) {
                        mSelectedSuggChapterNo = checkedId;
                        resetVerses(checkedId, verseModel);
                    }

                    mBinding.header.setExpanded(true);
                });

                mBinding.header.addView(chipGroupBinding.getRoot(), 1);
            }
        });
    }

    private void makeChapterChip(int id, ChipGroup parent, String chapterName) {
        Chip chip = new Chip(this);
        chip.setId(id);
        chip.setText(chapterName);
        runOnUiThread(() -> parent.addView(chip));
    }

    private void resetVerses(int requestedChapterNo, ReferenceVerseModel verseModel) {
        mTaskRunner.runAsync(new BaseRunnableTask() {
            private List<ReferenceVerseItemModel> models;

            @Override
            public void preExecute() {
                mActionController.showLoader();
            }

            @Override
            public void postExecute() {
                mActionController.dismissLoader();
            }

            @Override
            public void runTask() throws NumberFormatException {
                if (!mVerseDecorator.isKFQPCScript()) {
                    mVerseDecorator.refreshQuranTextFonts(null);
                }

                models = prepareVerses(requestedChapterNo, verseModel, mVerseDecorator.isKFQPCScript());
            }

            @Override
            public void onComplete() {
                mVersesAdapter.setVerseModels(models);
                mBinding.verses.setAdapter(mVersesAdapter);
                mLastScroll = 0;
                resetAppBar(0, false);
            }

            @Override
            public void onFailed(@NonNull Exception e) {
                super.onFailed(e);
            }
        });
    }

    private List<ReferenceVerseItemModel> prepareVerses(int requestChapterNo, ReferenceVerseModel verseModel, boolean isKFQPCScript) {
        List<ReferenceVerseItemModel> models = new ArrayList<>();
        models.add(new ReferenceVerseItemModel(VIEWTYPE_DESCRIPTION, null, -1, -1, -1, null, false));

        Map<String, QuranTranslBookInfo> booksInfo = mTranslFactory.getTranslationBooksInfoValidated(
            mSelectedTranslSlugs);

        for (String verseStr : verseModel.getVerses()) {
            String[] chapterVerses = verseStr.split(":");

            int chapterNo = Integer.parseInt(chapterVerses[0].trim());

            if (requestChapterNo != 0 && chapterNo != requestChapterNo) {
                continue;
            }

            String[] split = chapterVerses[1].split("-");

            final int fromVerse = Integer.parseInt(split[0].trim());

            if (split.length == 1) {
                String titleText = String.format("%s %d:%d", mQuranMeta.getChapterName(this, chapterNo), chapterNo,
                    fromVerse);
                models.add(prepareTitleModel(chapterNo, fromVerse, fromVerse, titleText));
                models.add(prepareVerseModel(chapterNo, fromVerse, booksInfo, isKFQPCScript));
            } else {
                final int toVerse = Integer.parseInt(split[1].trim());
                String titleText = String.format("%s %d:%d-%d", mQuranMeta.getChapterName(this, chapterNo), chapterNo,
                    fromVerse,
                    toVerse);
                models.add(prepareTitleModel(chapterNo, fromVerse, toVerse, titleText));
                for (int verseNo = fromVerse; verseNo <= toVerse; verseNo++) {
                    models.add(prepareVerseModel(chapterNo, verseNo, booksInfo, isKFQPCScript));
                }
            }
        }
        return models;
    }

    private ReferenceVerseItemModel prepareTitleModel(Integer chapterNo, int fromVerse, int toVerse, String titleText) {
        return new ReferenceVerseItemModel(
            VIEWTYPE_TITLE,
            null,
            chapterNo,
            fromVerse,
            toVerse,
            titleText,
            isBookmarked(chapterNo, fromVerse, toVerse)
        );
    }

    private ReferenceVerseItemModel prepareVerseModel(
        int chapterNo,
        int verseNo,
        Map<String, QuranTranslBookInfo> booksInfo,
        boolean isKFQPCScript
    ) {
        Verse verse = mQuran.getVerse(chapterNo, verseNo);
        verse.setIncludeChapterNameInSerial(true);

        if (isKFQPCScript) {
            mVerseDecorator.refreshQuranTextFonts(
                new Pair<>(verse.pageNo, verse.pageNo)
            );
        }

        mTranslFactory.getTranslationsSingleVerse(mSelectedTranslSlugs, chapterNo, verseNo);

        List<Translation> translations = mTranslFactory.getTranslationsSingleVerse(
            mSelectedTranslSlugs,
            chapterNo,
            verseNo
        );
        verse.setTranslations(translations);
        verse.arabicTextSpannable = SPReader.getArabicTextEnabled(this) ? prepareVerseText(verse) : null;
        verse.translTextSpannable = prepareTranslSpannable(verse, translations, booksInfo);

        return new ReferenceVerseItemModel(VIEWTYPE_VERSE, verse, chapterNo, -1, -1, null, false);
    }
}
