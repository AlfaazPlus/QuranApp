/*
 * (c) Faisal Khan. Created on 2/2/2022.
 */

package com.quranapp.android.adapters.quranIndex;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;
import com.quranapp.android.R;
import com.quranapp.android.components.IndexJuzItemModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.LytReaderIndexJuzCardBinding;
import com.quranapp.android.frags.readerindex.BaseFragReaderIndex;
import com.quranapp.android.utils.reader.factory.ReaderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import kotlin.Pair;

public class ADPJuzList extends ADPReaderIndexBase<ADPJuzList.VHJuz> {
    // private final RecyclerView.RecycledViewPool mViewPool;
    private List<IndexJuzItemModel> mModels = new ArrayList<>();

    public ADPJuzList(BaseFragReaderIndex fragment, Context ctx, boolean reversed) {
        super(fragment, reversed);
        // mViewPool = new RecyclerView.RecycledViewPool();

        initADP(ctx);
    }

    @Override
    protected void prepareList(Context ctx, boolean reverse) {
        mModels = new ArrayList<>();

        int from = reverse ? QuranMeta.totalJuzs() : 1;
        int to = reverse ? 1 : QuranMeta.totalJuzs();
        int juzNo = from;

        QuranMeta quranMeta = mFragment.getQuranMeta();
        while (true) {
            IndexJuzItemModel model = prepareJuzItemModel(ctx, juzNo, quranMeta);

            mModels.add(model);

            if (reverse) {
                juzNo--;
                if (juzNo < to) break;
            } else {
                juzNo++;
                if (juzNo > to) break;
            }
        }
    }

    private IndexJuzItemModel prepareJuzItemModel(Context ctx, int juzNo, QuranMeta quranMeta) {
        IndexJuzItemModel model = new IndexJuzItemModel();
        model.juzNo = juzNo;
        model.juzTitle = ctx.getString(R.string.strLabelJuzNo, juzNo);

        List<QuranMeta.ChapterMeta> chapterMetas = new ArrayList<>();

        Pair<Integer, Integer> chaptersInJuz = quranMeta.getChaptersInJuz(juzNo);

        IntStream.rangeClosed(chaptersInJuz.getFirst(), chaptersInJuz.getSecond())
            .forEach(chapterNo -> chapterMetas.add(quranMeta.getChapterMeta(chapterNo)));

        model.chapters = ImmutableList.copyOf(chapterMetas);

        return model;
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public VHJuz onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LytReaderIndexJuzCardBinding binding = LytReaderIndexJuzCardBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        binding.getRoot().setClipChildren(true);
        binding.getRoot().setClipToOutline(true);
        binding.chapterList.setLayoutManager(new LinearLayoutManager(parent.getContext()));
        // binding.chapterList.setRecycledViewPool(mViewPool);
        return new VHJuz(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VHJuz holder, int position) {
        holder.bind(position);
    }


    class VHJuz extends RecyclerView.ViewHolder {
        private LytReaderIndexJuzCardBinding mBinding;

        public VHJuz(@NonNull View itemView) {
            super(itemView);
        }

        public VHJuz(@NonNull LytReaderIndexJuzCardBinding binding) {
            this(binding.getRoot());
            mBinding = binding;
        }

        public void bind(int position) {
            IndexJuzItemModel model = mModels.get(position);
            setupJuz(model);
        }

        private void setupJuz(IndexJuzItemModel model) {
            if (mBinding == null) {
                return;
            }

            mBinding.header.setText(model.juzTitle);
            mBinding.header.setOnClickListener(v -> ReaderFactory.startJuz(v.getContext(), model.juzNo));

            setupChapterList(mBinding.chapterList, model);
        }

        private void setupChapterList(RecyclerView innerList, IndexJuzItemModel model) {
            innerList.setAdapter(new ADPJuzListChapterList(mFragment, model.chapters, model.juzNo));
        }
    }
}
