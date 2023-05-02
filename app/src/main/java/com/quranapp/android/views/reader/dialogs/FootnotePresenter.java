/*
 * Created by Faisal Khan on (c) 29/8/2021.
 */

package com.quranapp.android.views.reader.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import static com.quranapp.android.utils.univ.StringUtils.HYPHEN;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.peacedesign.android.utils.span.TypefaceSpan2;
import com.quranapp.android.R;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Footnote;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.LytReaderVerseFootnoteBinding;
import com.quranapp.android.reader_managers.ReaderVerseDecorator;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.parser.HtmlParser;
import com.quranapp.android.utils.reader.ReferenceTagHandler;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.univ.ResUtils;
import com.quranapp.android.utils.univ.SelectableLinkMovementMethod;
import com.quranapp.android.utils.univ.SpannableFactory;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FootnotePresenter extends PeaceBottomSheet {
    private LytReaderVerseFootnoteBinding mBinding;
    private ReaderPossessingActivity mActivity;
    private int mColorSecondary;
    private int mRefHighlightTxtColor;
    private int mRefHighlightBGColor;
    private int mRefHighlightBGColorPres;

    private Verse mVerse;
    private Footnote mFootnote;
    private String mLastSelectedSlug;
    private HashMap<Integer, Footnote> mLastSelectedFootnotes;
    private int mLastSelectedPos;
    private boolean mIsUrduSlug;

    public FootnotePresenter() {
        init();
    }

    private void init() {
        PeaceBottomSheetParams popupParams = getParams();
        popupParams.setHeaderShown(false);
        popupParams.setInitialBehaviorState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void initColors(Context context) {
        mColorSecondary = ContextCompat.getColor(context, R.color.colorSecondary);
        mRefHighlightTxtColor = ContextCompat.getColor(context, R.color.colorSecondary);
        mRefHighlightBGColor = ContextCompat.getColor(context, R.color.colorPrimaryAlpha10);
        mRefHighlightBGColorPres = ContextCompat.getColor(context, R.color.colorPrimaryAlpha50);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("verse", mVerse);
        outState.putSerializable("footnote", mFootnote);
        outState.putBoolean("isUrduSlug", mIsUrduSlug);

        if (mActivity == null || mBinding == null || mVerse == null) {
            return;
        }

        ChipGroup authorsGroup = mBinding.authors.chipGroup;


        View chip = authorsGroup.findViewById(authorsGroup.getCheckedChipId());
        if (chip == null) {
            return;
        }

        outState.putSerializable("lastSelectedSlug", mLastSelectedSlug);
        outState.putSerializable("lastSelectedPos", authorsGroup.indexOfChild(chip));

        mLastSelectedFootnotes = new HashMap<>(
            mActivity.mTranslFactory.getFootnotesSingleVerse((String) chip.getTag(), mVerse.chapterNo,
                mVerse.verseNo));
        outState.putSerializable("lastSelectedFootnotes", mLastSelectedFootnotes);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mVerse = (Verse) savedInstanceState.getSerializable("verse");
            mFootnote = (Footnote) savedInstanceState.getSerializable("footnote");
            mIsUrduSlug = savedInstanceState.getBoolean("isUrduSlug", false);
            mLastSelectedSlug = savedInstanceState.getString("lastSelectedSlug");
            mLastSelectedFootnotes = (HashMap<Integer, Footnote>) savedInstanceState.getSerializable(
                "lastSelectedFootnotes");
            mLastSelectedPos = savedInstanceState.getInt("lastSelectedPos", -1);
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
    protected void setupContentView(@NonNull LinearLayout dialogLayout, @NonNull PeaceBottomSheetParams params) {
        if (mActivity == null || mVerse == null) {
            return;
        }

        if (mBinding == null) {
            initColors(dialogLayout.getContext());

            AsyncLayoutInflater inflater = new AsyncLayoutInflater(dialogLayout.getContext());
            inflater.inflate(R.layout.lyt_reader_verse_footnote, dialogLayout, (view, resid, parent) -> {
                mBinding = LytReaderVerseFootnoteBinding.bind(view);
                setupContent(mActivity, mBinding, dialogLayout);
            });
        } else {
            setupContent(mActivity, mBinding, dialogLayout);
        }
    }

    private void setupContent(ReaderPossessingActivity actvt, LytReaderVerseFootnoteBinding binding, LinearLayout dialogLayout) {
        ViewKt.removeView(binding.getRoot());
        dialogLayout.addView(binding.getRoot());

        binding.getRoot().setSaveEnabled(true);
        binding.footnoteText.setShadowLayer(20, 0, 0, Color.TRANSPARENT);
        binding.title.setShadowLayer(20, 0, 0, Color.TRANSPARENT);

        setupWithDecorator(binding, actvt.mVerseDecorator);

        actvt.getQuranMetaSafely(quranMeta -> {
            setupTitle(actvt, quranMeta, binding, mVerse, mFootnote, mFootnote != null ? mFootnote.number : -1);
            binding.authors.getRoot().setVisibility(View.GONE);

            if (mFootnote != null) {
                Spanned footnoteText = prepareFootnoteText(actvt, binding, mFootnote,
                    TranslUtils.isUrdu(mFootnote.bookSlug));
                setText(binding, footnoteText);
                return;
            }

            List<Translation> translations = mVerse.getTranslations();
            if (translations.isEmpty()) {
                binding.authors.getRoot().setVisibility(View.GONE);
                return;
            }

            binding.authors.chipGroup.removeAllViews();
            setupChips(actvt, binding, mVerse, translations);
        });
    }

    private void setupTitle(ReaderPossessingActivity actvt, QuranMeta meta, LytReaderVerseFootnoteBinding binding, Verse verse, Footnote footnote, int number) {
        String title;
        QuranTranslBookInfo bookInfo = null;
        if (footnote != null) {
            bookInfo = actvt.mTranslFactory.getTranslationBookInfo(footnote.bookSlug);
            String langCode = bookInfo.getLangCode();
            final Locale locale = new Locale(langCode);
            title = ResUtils.getLocalizedString(actvt, R.string.strTitleFootnote, locale);

            if (title == null) {
                title = actvt.getString(R.string.strTitleFootnote);
            }
        } else {
            title = mBinding.getRoot().getContext().getString(R.string.strTitleFootnotes);
        }

        SpannableStringBuilder sb = new SpannableStringBuilder();
        final int flag = SPAN_EXCLUSIVE_EXCLUSIVE;

        SpannableString footnoteTitle = new SpannableString(title);
        footnoteTitle.setSpan(new ForegroundColorSpan(mColorSecondary), 0, title.length(), flag);

        sb.append(footnoteTitle);

        if (number != -1) {
            sb.append(" ");

            SpannableString numberSpannable = new SpannableString(String.valueOf(number));
            Typeface typefaceNumbering = Typeface.create("sans-serif-light", Typeface.NORMAL);
            numberSpannable.setSpan(new TypefaceSpan2(typefaceNumbering), 0, numberSpannable.length(), flag);
            numberSpannable.setSpan(new RelativeSizeSpan(0.95f), 0, numberSpannable.length(), flag);
            sb.append(numberSpannable);
        }

        binding.title.setText(sb);

        setupDesc(meta, bookInfo, verse, binding.desc, footnote);
    }

    private void setupDesc(QuranMeta meta, QuranTranslBookInfo bookInfo, Verse verse, TextView descView, Footnote footnote) {
        String chapterName = meta.getChapterName(descView.getContext(), verse.chapterNo);

        String descText = chapterName + " " + verse.chapterNo + ":" + verse.verseNo;

        if (footnote == null) {
            descView.setText(descText);
            return;
        }

        descText += " " + HYPHEN + " " + bookInfo.getDisplayName(true);
        descView.setText(descText);
    }

    private void setText(LytReaderVerseFootnoteBinding binding, CharSequence footnoteText) {
        binding.footnoteText.setText(footnoteText, TextView.BufferType.SPANNABLE);
        binding.footnoteText.setSpannableFactory(new SpannableFactory());
        binding.footnoteText.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        binding.scrollView.smoothScrollTo(0, 0);
    }

    private Spanned prepareFootnoteText(ReaderPossessingActivity actvt, LytReaderVerseFootnoteBinding binding, Footnote footnote, boolean isUrdu) {
        final Typeface typeface = isUrdu ? actvt.mUrduTypeface : Typeface.SANS_SERIF;
        binding.footnoteText.setTypeface(typeface);

        Set<String> translSlugs = Collections.singleton(footnote.bookSlug);

        ReferenceTagHandler tagHandler = new ReferenceTagHandler(translSlugs, mRefHighlightTxtColor,
            mRefHighlightBGColor,
            mRefHighlightBGColorPres, actvt::showVerseReference);

        String text = footnote.text;

        // to prevent clickable span to be invoked on empty space.
        text += " ";

        return HtmlParser.buildSpannedText(text, tagHandler);
    }

    private void prepareFootnotes(
        ReaderPossessingActivity actvt, LytReaderVerseFootnoteBinding binding,
        String slug, Map<Integer, Footnote> footnotes
    ) {
        mLastSelectedSlug = slug;

        SpannableStringBuilder sb = new SpannableStringBuilder();

        footnotes.forEach((number, footnote) -> {
            final Typeface typeface = TranslUtils.isUrdu(slug) ? actvt.mUrduTypeface : Typeface.SANS_SERIF;

            CharSequence footnoteText = prepareFootnoteText(actvt, binding, footnote, TranslUtils.isUrdu(slug));
            sb.append(prepareNumbering(number, typeface)).append(footnoteText);

            if (number < footnotes.size()) {
                sb.append("\n\n");
            }
        });

        setText(binding, sb);
    }

    private CharSequence prepareNumbering(int number, Typeface typeface) {
        String numberStr = number + ". ";

        SpannableString author = new SpannableString(numberStr);
        author.setSpan(new TypefaceSpan2(Typeface.create(typeface, Typeface.BOLD)), 0, author.length(),
            SPAN_EXCLUSIVE_EXCLUSIVE);
        author.setSpan(new ForegroundColorSpan(mColorSecondary), 0, author.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        return author;
    }

    public void setupWithDecorator(LytReaderVerseFootnoteBinding binding, ReaderVerseDecorator decorator) {
        decorator.setTextColorNonArabic(binding.footnoteText);
        decorator.setTextSizeTransl(binding.footnoteText);
    }

    private void setupChips(
        ReaderPossessingActivity actvt, LytReaderVerseFootnoteBinding binding,
        Verse verse, List<Translation> translations
    ) {
        binding.authors.getRoot().setVisibility(View.VISIBLE);
        binding.authors.getRoot().smoothScrollTo(0, 0);

        ChipGroup authorsGroup = binding.authors.chipGroup;
        authorsGroup.setSingleSelection(true);
        authorsGroup.setSelectionRequired(true);

        Set<String> slugs = translations.stream().map(Translation::getBookSlug).collect(Collectors.toSet());
        Map<String, QuranTranslBookInfo> booksInfo = actvt.mTranslFactory.getTranslationBooksInfoValidated(slugs);

        for (Translation translation : translations) {
            if (translation.getFootnotesCount() == 0) {
                continue;
            }

            QuranTranslBookInfo bookInfo = booksInfo.get(translation.getBookSlug());
            if (bookInfo != null) {
                String displayName = bookInfo.getDisplayName(true);
                Chip chip = makeAuthorChip(displayName, bookInfo.getSlug());
                authorsGroup.addView(chip);
            }
        }

        authorsGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            View chip = group.findViewById(checkedIds.get(0));
            if (chip == null) {
                return;
            }

            String slug = (String) chip.getTag();

            Map<Integer, Footnote> footnotes = actvt.mTranslFactory.getFootnotesSingleVerse(slug, verse.chapterNo,
                verse.verseNo);
            prepareFootnotes(actvt, binding, slug, footnotes);
        });

        if (mLastSelectedFootnotes != null) {
            prepareFootnotes(actvt, binding, mLastSelectedSlug, mLastSelectedFootnotes);
            mLastSelectedFootnotes = null;
        }

        View initialChild = null;
        if (authorsGroup.getChildCount() > 0 && mLastSelectedPos >= 0) {
            initialChild = authorsGroup.getChildAt(mLastSelectedPos);
        }

        if (initialChild == null) {
            initialChild = authorsGroup.getChildAt(0);
        }

        if (initialChild != null) {
            authorsGroup.check(initialChild.getId());
        }
    }

    private Chip makeAuthorChip(String authorName, String slug) {
        Chip chip = new Chip(getContext());
        chip.setId(View.generateViewId());
        chip.setText(authorName);
        chip.setTag(slug);
        return chip;
    }

    public void present(ReaderPossessingActivity activity, Verse verse, Footnote footnote, boolean isUrduSlug) {
        dismiss();

        mActivity = activity;
        mVerse = verse;
        mFootnote = footnote;
        mIsUrduSlug = isUrduSlug;

        show(activity.getSupportFragmentManager());
    }

    public void present(ReaderPossessingActivity activity, Verse verse) {
        present(activity, verse, null, false);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!isShowing()) {
            mBinding = null;
        }
    }

    private void onDismissed() {
        if (mBinding == null) {
            return;
        }

        mBinding.footnoteText.setText(null);
        mBinding.desc.setText(null);
        mBinding.authors.chipGroup.setOnCheckedStateChangeListener(null);
    }

    @Override
    public void dismiss() {
        try {
            dismissAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        onDismissed();
    }
}
