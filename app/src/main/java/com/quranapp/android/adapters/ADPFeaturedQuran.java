package com.quranapp.android.adapters;

import static com.peacedesign.android.utils.touchutils.HoverPushEffect.Pressure.LOW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.touchutils.HoverPushOpacityEffect;
import com.quranapp.android.components.FeaturedQuranModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.LytFeaturedQuranItemBinding;
import com.quranapp.android.utils.reader.factory.ReaderFactory;

import java.util.List;

public class ADPFeaturedQuran extends RecyclerView.Adapter<ADPFeaturedQuran.VHFeaturedQuran> {
    private final List<FeaturedQuranModel> mModels;
    private final QuranMeta mQuranMeta;

    public ADPFeaturedQuran(QuranMeta quranMeta, List<FeaturedQuranModel> models) {
        mQuranMeta = quranMeta;
        mModels = models;
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }

    @NonNull
    @Override
    public VHFeaturedQuran onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        LytFeaturedQuranItemBinding binding = LytFeaturedQuranItemBinding.inflate(LayoutInflater.from(ctx));

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(Dimen.dp2px(ctx, 200), Dimen.dp2px(ctx, 150));
        binding.getRoot().setLayoutParams(params);
        return new VHFeaturedQuran(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VHFeaturedQuran holder, int position) {
        holder.bind(mModels.get(position));
    }

    class VHFeaturedQuran extends RecyclerView.ViewHolder {
        private final LytFeaturedQuranItemBinding binding;

        public VHFeaturedQuran(@NonNull LytFeaturedQuranItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(FeaturedQuranModel model) {
            binding.name.setText(model.getName());
            binding.miniInfo.setText(model.getMiniInfo());

            binding.getRoot().setOnClickListener(v -> {
                int chapterNo = model.getChapterNo();
                int[] verseRange = model.getVerseRange();
                if (QuranMeta.isChapterValid(chapterNo) &&
                        mQuranMeta.isVerseRangeValid4Chapter(chapterNo, verseRange[0], verseRange[1])) {
                    ReaderFactory.startVerseRange(itemView.getContext(), chapterNo, verseRange);
                }
            });

            binding.getRoot().setOnTouchListener(new HoverPushOpacityEffect(LOW));
        }
    }
}
