package com.quranapp.android.adapters;

import android.content.Context;
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
import com.quranapp.android.components.quran.QuranTopic;
import com.quranapp.android.databinding.LytQuranTopicItemBinding;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.reader.factory.ReaderFactory;

import java.util.LinkedList;
import java.util.List;

public class ADPTopics extends RecyclerView.Adapter<ADPTopics.VHTopic> {
    @Dimension
    private final int mItemWidth;
    private final int mTxtSize;
    private final ColorStateList mTxtColor2;
    private List<QuranTopic.Topic> mTopics = new LinkedList<>();

    public ADPTopics(Context ctx, int itemWidth) {
        mItemWidth = itemWidth;

        mTxtSize = ContextKt.getDimenPx(ctx, R.dimen.dmnCommonSize2);
        mTxtColor2 = ContextCompat.getColorStateList(ctx, R.color.colorText2);
    }

    @Override
    public int getItemCount() {
        return mTopics.size();
    }

    public void setTopics(List<QuranTopic.Topic> topics) {
        mTopics = topics;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public VHTopic onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LytQuranTopicItemBinding binding = LytQuranTopicItemBinding.inflate(LayoutInflater.from(parent.getContext()),
            parent,
            false);
        return new VHTopic(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VHTopic holder, int position) {
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

    class VHTopic extends RecyclerView.ViewHolder {
        private final LytQuranTopicItemBinding mBinding;

        public VHTopic(@NonNull LytQuranTopicItemBinding binding) {
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

        public void bind(QuranTopic.Topic topic) {
            setupTopic(topic);
        }

        private void setupTopic(QuranTopic.Topic topic) {
            if (mBinding == null) {
                return;
            }

            setupActions(topic);


            String topicName = topic.name;
            if (!TextUtils.isEmpty(topic.otherTerms)) {
                topicName += " (" + topic.otherTerms + ")";
            }
            int count = topic.verses.size();

            String countText = count + (count == 1 ? " place" : " places");
            mBinding.text.setText(prepareTexts(topicName, countText, topic.inChapters));
        }

        private void setupActions(QuranTopic.Topic topic) {
            mBinding.getRoot().setOnClickListener(v -> {
                String title = "\"" + topic.name + "\"";

                Context ctx = v.getContext();
                String desc = ctx.getString(R.string.strMsgReferenceFoundPlaces, title, topic.verses.size());

                title = ctx.getString(R.string.strMsgReferenceInQuran, title);
                ReaderFactory.startReferenceVerse(ctx, true, title, desc, new String[]{}, topic.chapters, topic.verses);
            });
        }
    }
}
