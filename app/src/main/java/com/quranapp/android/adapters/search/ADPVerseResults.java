package com.quranapp.android.adapters.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import static com.quranapp.android.activities.ActivitySearch.SearchResultViewType.CHAPTER_JUMPER;
import static com.quranapp.android.activities.ActivitySearch.SearchResultViewType.JUZ_JUMPER;
import static com.quranapp.android.activities.ActivitySearch.SearchResultViewType.RESULT;
import static com.quranapp.android.activities.ActivitySearch.SearchResultViewType.RESULT_COUNT;
import static com.quranapp.android.activities.ActivitySearch.SearchResultViewType.TAFSIR_JUMPER;
import static com.quranapp.android.activities.ActivitySearch.SearchResultViewType.VERSE_JUMPER;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.span.TypefaceSpan2;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.ActivitySearch;
import com.quranapp.android.adapters.extended.PeaceBottomSheetMenuAdapter;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.search.ChapterJumpModel;
import com.quranapp.android.components.search.JuzJumpModel;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.components.search.TafsirJumpModel;
import com.quranapp.android.components.search.VerseJumpModel;
import com.quranapp.android.components.search.VerseResultCountModel;
import com.quranapp.android.components.search.VerseResultModel;
import com.quranapp.android.databinding.LytReaderJuzSpinnerItemBinding;
import com.quranapp.android.databinding.LytSearchResultItemBinding;
import com.quranapp.android.db.bookmark.BookmarkDBHelper;
import com.quranapp.android.frags.search.FragSearchResult;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.extensions.ViewPaddingKt;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.vh.search.VHChapterJump;
import com.quranapp.android.vh.search.VHJuzJump;
import com.quranapp.android.vh.search.VHSearchResultBase;
import com.quranapp.android.vh.search.VHTafsirJump;
import com.quranapp.android.vh.search.VHVerseJump;
import com.quranapp.android.widgets.IconedTextView;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetMenu;
import com.quranapp.android.widgets.chapterCard.ChapterCard;
import com.quranapp.android.widgets.list.base.BaseListItem;

import java.util.ArrayList;
import java.util.List;

public class ADPVerseResults extends RecyclerView.Adapter<VHSearchResultBase> implements Destroyable {
    private final FragmentManager mFm;
    private final int mTransTextSize;
    private final int mMoreBtnTextSize;
    private final int mAuthorTextSize;
    private final int mColorPrimary;
    private final int mColorSecondary;
    private final String mMoreTransText;
    private final Typeface fontUrdu;
    private final Typeface defaultTransFont;
    private final Typeface mQueryHighlightTypeface;
    private final LayoutInflater mInflater;
    private ArrayList<SearchResultModelBase> mResultModels = new ArrayList<>();
    private ActivitySearch mActivity;

    public ADPVerseResults(Context context, FragSearchResult fragSearchResult) {
        mFm = fragSearchResult.getParentFragmentManager();
        mInflater = LayoutInflater.from(context);
        mTransTextSize = ContextKt.getDimenPx(context, R.dimen.dmnCommonSize);
        mAuthorTextSize = ContextKt.getDimenPx(context, R.dimen.dmnCommonSize2);
        mMoreBtnTextSize = ContextKt.getDimenPx(context, R.dimen.dmnCommonSize2);
        mColorSecondary = ContextKt.color(context, R.color.colorText3);
        mColorPrimary = ContextKt.color(context, R.color.colorPrimary);
        mMoreTransText = context.getString(R.string.strLabelMoreTranslations);
        fontUrdu = ContextKt.getFont(context, R.font.font_urdu);
        defaultTransFont = Typeface.DEFAULT;
        mQueryHighlightTypeface = Typeface.create("sans-serif", Typeface.BOLD_ITALIC);

        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return mResultModels.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        SearchResultModelBase modelBase = mResultModels.get(position);
        if (modelBase instanceof VerseJumpModel) {
            return VERSE_JUMPER;
        } else if (modelBase instanceof ChapterJumpModel) {
            return CHAPTER_JUMPER;
        } else if (modelBase instanceof JuzJumpModel) {
            return JUZ_JUMPER;
        } else if (modelBase instanceof TafsirJumpModel) {
            return TAFSIR_JUMPER;
        } else if (modelBase instanceof VerseResultCountModel) {
            return RESULT_COUNT;
        } else if (modelBase instanceof VerseResultModel) {
            return RESULT;
        }
        return -1;
    }

    public void setResults(ArrayList<SearchResultModelBase> results) {
        mResultModels = results;
    }

    @NonNull
    @Override
    public VHSearchResultBase onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final VHSearchResultBase vh;
        switch (viewType) {
            case VERSE_JUMPER:
                vh = new VHVerseJump(new AppCompatTextView(parent.getContext()), false);
                break;
            case CHAPTER_JUMPER:
                vh = new VHChapterJump(new ChapterCard(parent.getContext()), false);
                break;
            case JUZ_JUMPER:
                vh = new VHJuzJump(LytReaderJuzSpinnerItemBinding.inflate(mInflater, parent, false), false);
                break;
            case TAFSIR_JUMPER:
                vh = new VHTafsirJump(new IconedTextView(parent.getContext()), false);
                break;
            case RESULT_COUNT:
                vh = new VHVerseResultCount(makeResultCountView(parent.getContext()));
                break;
            case RESULT:
                vh = new VHVerseResultVerse(LytSearchResultItemBinding.inflate(mInflater, parent, false));
                break;
            default:
                vh = new VHSearchResultBase(new View(parent.getContext()));
                break;
        }

        return vh;
    }

    private View makeResultCountView(Context context) {
        AppCompatTextView resultCountView = new AppCompatTextView(context);

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = mActivity.dp2px(5);
        resultCountView.setLayoutParams(params);

        ViewPaddingKt.updatePaddingHorizontal(resultCountView, mActivity.dp2px(15));
        ViewPaddingKt.updatePaddingVertical(resultCountView, mActivity.dp2px(2));

        resultCountView.setTextColor(mColorSecondary);
        resultCountView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mAuthorTextSize);

        return resultCountView;
    }

    @Override
    public void onBindViewHolder(@NonNull VHSearchResultBase holder, int position) {
        holder.bind(mResultModels.get(position), position);
    }

    public void setActivitySearch(ActivitySearch activitySearch) {
        mActivity = activitySearch;
    }

    @Override
    public void destroy() {
        mResultModels.clear();
    }

    static class VHVerseResultCount extends VHSearchResultBase {
        public VHVerseResultCount(@NonNull View itemView) {
            super(itemView);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public void bind(SearchResultModelBase baseModel, int pos) {
            VerseResultCountModel model = (VerseResultCountModel) baseModel;
            Context context = itemView.getContext();
            final String text;

            QuranTranslBookInfo translModel = model.getBookInfo();
            if (model.resultCount > 0) {
                if (model.resultCount == 1) {
                    text = context.getString(R.string.strMsgSearchOneResultFoundIn, translModel.getBookName());
                } else {
                    text = context.getString(R.string.strMsgSearchMultResultsFound, translModel.getBookName(),
                        model.resultCount);
                }
            } else {
                if (translModel != null) {
                    text = context.getString(R.string.strMsgSearchNoResultsFoundIn, translModel.getBookName());
                } else {
                    text = context.getString(R.string.strMsgSearchNoResultsFound);
                }
            }
            ((TextView) itemView).setText(text);
        }
    }

    class VHVerseResultVerse extends VHSearchResultBase {
        private final LytSearchResultItemBinding mBinding;

        public VHVerseResultVerse(@NonNull LytSearchResultItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            View root = binding.getRoot();
            root.setElevation(Dimen.dp2px(root.getContext(), 4));
        }

        @Override
        public void bind(SearchResultModelBase baseModel, int pos) {
            if (mBinding == null) {
                return;
            }

            VerseResultModel model = (VerseResultModel) baseModel;

            mBinding.verseSerial.setText(model.verseSerial);
            mBinding.menu.setOnClickListener(v -> openItemMenu(v.getContext(), model));

            mBinding.transPlaceholder.removeAllViews();
            if (model.translationsView == null) {
                model.translationsView = makeTranslations(model);
            } else {
                ViewKt.removeView(model.translationsView);
            }
            mBinding.transPlaceholder.addView(model.translationsView);
            itemView.setOnClickListener(v -> openItem(itemView.getContext(), model));
        }

        private View makeTranslations(VerseResultModel model) {
            LinearLayout container = new LinearLayout(itemView.getContext());
            container.setOrientation(LinearLayout.VERTICAL);

            int visibleTransCount = 1;

            List<Translation> translations = model.translations;
            for (int i = 0, l = Math.min(translations.size(), visibleTransCount); i < l; i++) {
                makeSingleTranslation(container, model.translDisplayNames.get(i), translations.get(i),
                    model.startIndices.get(i),
                    model.endIndices.get(i));
            }

            int hideableTransCount = model.translations.size() - visibleTransCount;
            if (hideableTransCount > 0) {
                makeMoreTransNavigator(model, container, visibleTransCount, hideableTransCount, model);
            }

            return container;
        }

        private void makeSingleTranslation(
            LinearLayout container, String translDisplayName,
            Translation translation, int startIndex, int endIndex
        ) {
            Context context = itemView.getContext();

            LinearLayout translRoot = new LinearLayout(context);
            translRoot.setOrientation(LinearLayout.VERTICAL);

            AppCompatTextView transTextView = new AppCompatTextView(context);
            ViewPaddingKt.updatePaddingHorizontal(transTextView, mActivity.dp2px(10));
            transTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTransTextSize);
            transTextView.setText(prepareTransText(translation.getText(), startIndex, endIndex, translation.isUrdu()));
            transTextView.setShadowLayer(mTransTextSize, 0f, 0f, Color.TRANSPARENT);
            transTextView.setLayerType(View.LAYER_TYPE_SOFTWARE, transTextView.getPaint());
            translRoot.addView(transTextView, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            AppCompatTextView authorTextView = new AppCompatTextView(context);
            ViewPaddingKt.updatePaddingHorizontal(authorTextView, mActivity.dp2px(10));
            authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mAuthorTextSize);
            authorTextView.setTextColor(mColorSecondary);
            authorTextView.setShadowLayer(mAuthorTextSize, 0f, 0f, Color.TRANSPARENT);
            authorTextView.setLayerType(View.LAYER_TYPE_SOFTWARE, authorTextView.getPaint());
            authorTextView.setText(translDisplayName);

            if (translation.isUrdu()) {
                transTextView.setTypeface(fontUrdu);
                authorTextView.setTypeface(fontUrdu);
                transTextView.setIncludeFontPadding(false);
            } else {
                transTextView.setTypeface(defaultTransFont);
                authorTextView.setTypeface(defaultTransFont);
                transTextView.setIncludeFontPadding(true);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.topMargin = mActivity.dp2px(5);
            translRoot.addView(authorTextView, params);

            LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            LayoutParamsKt.updateMarginVertical(rootParams, mActivity.dp2px(5));

            container.addView(translRoot, rootParams);
        }

        private void makeMoreTransNavigator(VerseResultModel verseResultModel, LinearLayout container, int visibleTransCount, int hideableCount, VerseResultModel model) {
            AppCompatTextView moreTransView = new AppCompatTextView(itemView.getContext());
            ViewPaddingKt.updatePaddings(moreTransView, mActivity.dp2px(5));

            moreTransView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMoreBtnTextSize);
            moreTransView.setText(String.format(mMoreTransText, hideableCount));
            moreTransView.setTextColor(mColorPrimary);
            moreTransView.setBackgroundResource(R.drawable.dr_bg_hover_cornered);
            moreTransView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            LayoutParamsKt.updateMarginHorizontal(params, mActivity.dp2px(10));
            LayoutParamsKt.updateMarginVertical(params, mActivity.dp2px(5));
            container.addView(moreTransView, params);

            moreTransView.setOnClickListener(v -> {
                ViewKt.removeView(moreTransView);
                List<Translation> translations = model.translations;
                for (int i = visibleTransCount, l = translations.size(); i < l; i++) {
                    Translation translation = translations.get(i);
                    makeSingleTranslation(container, model.translDisplayNames.get(i),
                        translation, model.startIndices.get(i), model.endIndices.get(i));
                }
            });
        }


        private CharSequence prepareTransText(String text, int startIndex, int endIndex, boolean isUrdu) {
            int starEndDiff = endIndex - startIndex;
            int textInitialLength = text.length();

            int subStrStart = startIndex - 50;
            if (subStrStart < 20) {
                subStrStart = 0;
            }

            int subStrEnd = Math.min(endIndex + 20, textInitialLength);

            if (!isUrdu) {
                char c = text.charAt(subStrStart);

                while (subStrStart > 0 && (StringUtils.isRTL(c) || c == ' ')) {
                    c = text.charAt(--subStrStart);
                }
            }

            CharSequence substring = text.substring(subStrStart, subStrEnd);

            startIndex -= subStrStart;
            endIndex = startIndex + starEndDiff;
            CharSequence highlighted = highlightQuery(substring, startIndex, endIndex, isUrdu);

            String startEllipsis = subStrStart == 0 ? "" : "...";
            String endEllipsis = (subStrEnd == textInitialLength ? "" : "...");
            return TextUtils.concat(startEllipsis, highlighted, endEllipsis);
        }


        private CharSequence highlightQuery(CharSequence textPart, int start, int end, boolean isUrdu) {
            SpannableString ss = new SpannableString(textPart);
            ss.setSpan(new ForegroundColorSpan(mColorPrimary), start, end, SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new TypefaceSpan2(isUrdu ? fontUrdu : mQueryHighlightTypeface), start, end,
                SPAN_EXCLUSIVE_EXCLUSIVE);
            return ss;
        }


        private void openItemMenu(Context context, VerseResultModel model) {
            BookmarkDBHelper dbHelper = mActivity.mBookmarkDBHelper;
            final boolean isBookmarked = dbHelper.isBookmarked(model.chapterNo, model.verseNo, model.verseNo);

            PeaceBottomSheetMenu dialog = new PeaceBottomSheetMenu();
            dialog.getParams().setHeaderTitle(model.chapterName + " " + model.verseSerial);

            int[] icons = {R.drawable.dr_icon_open, isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_add};
            int[] labels = {R.string.strLabelOpen, isBookmarked ? R.string.strLabelRemoveBookmark : R.string.strDescAddToBookmarks};
            int[] descs = {R.string.strLabelOpenInReader, 0};

            PeaceBottomSheetMenuAdapter adapter = new PeaceBottomSheetMenuAdapter(context);
            for (int i = 0, l = labels.length; i < l; i++) {
                BaseListItem item = new BaseListItem(icons[i], context.getString(labels[i]));
                if (descs[i] != 0) {
                    item.setMessage(context.getString(descs[i]));
                }
                item.setId(i);
                adapter.addItem(item);
            }

            dialog.setAdapter(adapter);
            dialog.setOnItemClickListener((menu, item) -> {
                switch (item.getId()) {
                    case 0: {
                        openItem(context, model);
                    } break;
                    case 1: {
                        if (isBookmarked) {
                            dbHelper.removeFromBookmark(model.chapterNo, model.verseNo, model.verseNo, null);
                        } else {
                            dbHelper.addToBookmark(model.chapterNo, model.verseNo, model.verseNo, null, null);
                        }
                        break;
                    }
                }
                menu.dismiss();
            });

            dialog.show(mFm);
        }


        private void openItem(Context context, VerseResultModel model) {
            Intent intent = ReaderFactory.prepareSingleVerseIntent(model.chapterNo, model.verseNo);
            intent.setClass(context, ActivityReader.class);
            String[] requestedSlugs = model.translSlugs.toArray(new String[0]);
            intent.putExtra(READER_KEY_TRANSL_SLUGS, requestedSlugs);
            intent.putExtra(READER_KEY_SAVE_TRANSL_CHANGES, false);
            context.startActivity(intent);
        }
    }
}
