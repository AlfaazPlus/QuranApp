package com.quranapp.android.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.span.LineHeightSpan2;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.readHistory.ReadHistoryModel;
import com.quranapp.android.databinding.LytBookmarkItemBinding;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.reader.factory.ReaderFactory;

import java.util.List;
import java.util.Locale;

public class ADPReadHistory extends RecyclerView.Adapter<ADPReadHistory.VHReadHistory> {
    private final QuranMeta mQuranMeta;
    @Dimension
    private final int mItemWidth;
    private final int mTxtSize;
    private final int mTxtSize2;
    private final ColorStateList mColorPrimary;
    private List<ReadHistoryModel> mHistories;
    private final String mVerseNoFormat;
    private final String mVersesFormat;
    private final String mTxtContinueReading;

    public ADPReadHistory(Context ctx, QuranMeta quranMeta, List<ReadHistoryModel> histories, int itemWidth) {
        mItemWidth = itemWidth;
        mQuranMeta = quranMeta;
        mHistories = histories;

        mTxtSize = ContextKt.getDimenPx(ctx, R.dimen.dmnCommonSize2);
        mTxtSize2 = ContextKt.getDimenPx(ctx, R.dimen.dmnCommonSize2_5);
        mColorPrimary = ContextCompat.getColorStateList(ctx, R.color.colorPrimary);

        mVerseNoFormat = ctx.getString(R.string.strLabelVerseNoWithColon);
        mVersesFormat = ctx.getString(R.string.strLabelVersesWithColon);
        mTxtContinueReading = ctx.getString(R.string.strLabelContinueReading);
    }

    public void updateModels(List<ReadHistoryModel> models) {
        mHistories = models;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mHistories.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public VHReadHistory onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LytBookmarkItemBinding binding = LytBookmarkItemBinding.inflate(LayoutInflater.from(parent.getContext()),
            parent, false);
        return new VHReadHistory(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VHReadHistory holder, int position) {
        holder.bind(mHistories.get(position));
    }

    private CharSequence prepareTexts(String title, CharSequence subTitle, String continueReading) {
        SpannableString titleSS = new SpannableString(title);
        TextAppearanceSpan titleTASpan = new TextAppearanceSpan("sans-serif", Typeface.BOLD, mTxtSize, null, null);
        titleSS.setSpan(titleTASpan, 0, titleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString subTitleSS = new SpannableString(subTitle);
        TextAppearanceSpan subTitleTASpan = new TextAppearanceSpan("sans-serif", Typeface.NORMAL, mTxtSize, null, null);
        subTitleSS.setSpan(subTitleTASpan, 0, subTitleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        subTitleSS.setSpan(new LineHeightSpan2(20, false, true), 0, subTitleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString continueReadingSS = new SpannableString(continueReading);
        TextAppearanceSpan continueReadingTASpan = new TextAppearanceSpan("sans-serif-medium", Typeface.NORMAL,
            mTxtSize2,
            mColorPrimary, null);
        continueReadingSS.setSpan(continueReadingTASpan, 0, continueReadingSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", continueReadingSS);
    }

    class VHReadHistory extends RecyclerView.ViewHolder {
        private final LytBookmarkItemBinding mBinding;

        public VHReadHistory(@NonNull LytBookmarkItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            View root = binding.getRoot();
            root.setElevation(Dimen.dp2px(root.getContext(), 4));

            ViewGroup.LayoutParams p = root.getLayoutParams();
            if (p != null) {
                p.width = mItemWidth;
            } else {
                p = new RecyclerView.LayoutParams(mItemWidth, WRAP_CONTENT);
                LayoutParamsKt.updateMargins((ViewGroup.MarginLayoutParams) p,
                    Dimen.dp2px(binding.getRoot().getContext(), 3));
            }
            root.setLayoutParams(p);

            if (mItemWidth > 0) {
                root.setBackgroundResource(R.drawable.dr_bg_chapter_card_bordered);
            } else {
                root.setBackgroundResource(R.drawable.dr_bg_chapter_card);
            }
        }

        public void bind(ReadHistoryModel history) {
            mBinding.chapterNo.setVisibility(View.VISIBLE);
            mBinding.menu.setVisibility(View.GONE);
            mBinding.check.setVisibility(View.GONE);

            mBinding.chapterNo.setText(String.format(Locale.getDefault(), "%d", history.getChapterNo()));

            String chapterName = mQuranMeta.getChapterName(itemView.getContext(), history.getChapterNo(), true);
            CharSequence txt = prepareTexts(chapterName,
                prepareSubtitleTitle(history.getFromVerseNo(), history.getToVerseNo()),
                mTxtContinueReading);
            mBinding.text.setText(txt);

            setupActions(history);
        }

        private void setupActions(ReadHistoryModel history) {
            mBinding.getRoot().setOnClickListener(v -> {
                Intent intent = ReaderFactory.prepareLastVersesIntent(mQuranMeta, history);

                if (intent != null) {
                    intent.setClass(itemView.getContext(), ActivityReader.class);
                    itemView.getContext().startActivity(intent);
                }
            });
        }

        public CharSequence prepareSubtitleTitle(int fromVerse, int toVerse) {
            if (QuranUtils.doesRangeDenoteSingle(fromVerse, toVerse)) {
                return String.format(mVerseNoFormat, fromVerse);
            } else {
                return String.format(mVersesFormat, fromVerse, toVerse);
            }
        }
    }
}
