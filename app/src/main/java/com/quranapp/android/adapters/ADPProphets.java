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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.span.LineHeightSpan2;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReference;
import com.quranapp.android.components.quran.QuranProphet;
import com.quranapp.android.databinding.LytQuranProphetItemBinding;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.reader.factory.ReaderFactory;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

public class ADPProphets extends RecyclerView.Adapter<ADPProphets.VHProphet> {
    private final int mItemWidth;
    private final int mTxtSize;
    private final ColorStateList mTxtColor2;
    private List<QuranProphet.Prophet> mTopics = new LinkedList<>();
    private final int mLimit;

    public ADPProphets(Context ctx, int itemWidth, int limit) {
        mItemWidth = itemWidth;
        mLimit = limit;

        mTxtSize = ContextKt.getDimenPx(ctx, R.dimen.dmnCommonSize2);
        mTxtColor2 = ContextCompat.getColorStateList(ctx, R.color.colorText2);
    }

    @Override
    public int getItemCount() {
        return mLimit > 0 ? Math.min(mTopics.size(), mLimit) : mTopics.size();
    }

    public void setProphets(List<QuranProphet.Prophet> topics) {
        mTopics = topics;
    }

    public List<QuranProphet.Prophet> getTopics() {
        return mTopics;
    }

    @NonNull
    @Override
    public VHProphet onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        LytQuranProphetItemBinding binding = LytQuranProphetItemBinding.inflate(inflater, parent, false);
        return new VHProphet(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VHProphet holder, int position) {
        holder.bind(mTopics.get(position));
    }

    private CharSequence prepareTexts(String title, CharSequence subTitle, String inChapters) {
        SpannableString titleSS = new SpannableString(title);
        TextAppearanceSpan titleTASpan = new TextAppearanceSpan("sans-serif", Typeface.BOLD, mTxtSize, null, null);
        titleSS.setSpan(titleTASpan, 0, titleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString subTitleSS = new SpannableString(subTitle);
        TextAppearanceSpan subTitleTASpan = new TextAppearanceSpan("sans-serif", Typeface.NORMAL, mTxtSize, null, null);
        subTitleSS.setSpan(subTitleTASpan, 0, subTitleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        subTitleSS.setSpan(new LineHeightSpan2(20, false, true), 0, subTitleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString chaptersSS = new SpannableString(inChapters);
        TextAppearanceSpan inChaptersTASpan = new TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, mTxtSize,
            mTxtColor2,
            null);
        chaptersSS.setSpan(inChaptersTASpan, 0, chaptersSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", chaptersSS);
    }

    class VHProphet extends RecyclerView.ViewHolder {
        private final LytQuranProphetItemBinding mBinding;

        public VHProphet(@NonNull LytQuranProphetItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            View root = binding.getRoot();
            root.setElevation(Dimen.dp2px(root.getContext(), 4));
            root.setLayoutParams(new ViewGroup.LayoutParams(mItemWidth, WRAP_CONTENT));

            if (mItemWidth > 0) {
                root.setMinimumHeight(Dimen.dp2px(root.getContext(), 110));
                root.setBackgroundResource(R.drawable.dr_bg_chapter_card_bordered);
            } else {
                root.setBackgroundResource(R.drawable.dr_bg_chapter_card);
            }
        }

        public void bind(QuranProphet.Prophet prophet) {
            setupActions(prophet);

           /* mBinding.name.setText(MessageFormat.format("{0} ({1})", prophet.nameTrans, prophet.honorific));
            mBinding.nameEng.setText(nameEng);
            mBinding.chapters.setText(prophet.inChapters);*/

            mBinding.icon.setImageDrawable(ContextKt.drawable(mBinding.getRoot().getContext(), prophet.iconRes));

            String name = MessageFormat.format("{0} ({1})", prophet.nameTrans, prophet.honorific);
            String nameEng = "English : " + (prophet.nameEn == null ? prophet.nameTrans : prophet.nameEn);
            mBinding.text.setText(prepareTexts(name, nameEng, prophet.inChapters));
        }

        private void setupActions(QuranProphet.Prophet prophet) {
            String title = prophet.nameTrans;
            if (prophet.order == 25) {
                title += " ﷺ";
            } else {
                title += " ؑ ";
            }

            Context ctx = mBinding.getRoot().getContext();
            String desc = ctx.getString(R.string.strMsgReferenceFoundPlaces, title, prophet.verses.size());

            title = ctx.getString(R.string.strMsgReferenceInQuran, title);

            Intent intent = ReaderFactory.prepareReferenceVerseIntent(true, title, desc, new String[]{},
                prophet.chapters, prophet.verses);
            intent.setClass(ctx, ActivityReference.class);
            mBinding.getRoot().setOnClickListener(v -> ctx.startActivity(intent));
        }
    }
}
