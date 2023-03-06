/*
 * (c) Faisal Khan. Created on 2/2/2022.
 */

package com.quranapp.android.adapters.quranIndex;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.frags.readerindex.BaseFragReaderIndex;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.widgets.chapterCard.ChapterCardJuz;

import kotlin.Pair;

public class ADPJuzListChapterList extends RecyclerView.Adapter<ADPJuzListChapterList.VHJuzChapter> {
    private final BaseFragReaderIndex mFragment;
    private final ImmutableList<QuranMeta.ChapterMeta> mChapterMetas;
    private final int mJuzNo;
    private final String mVersesStr;

    public ADPJuzListChapterList(BaseFragReaderIndex fragment, ImmutableList<QuranMeta.ChapterMeta> mChapterMetas, int juzNo) {
        mFragment = fragment;
        this.mChapterMetas = mChapterMetas;
        mJuzNo = juzNo;
        mVersesStr = fragment.getString(R.string.strLabelVersesText);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mChapterMetas.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public ADPJuzListChapterList.VHJuzChapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VHJuzChapter(new ChapterCardJuz(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull ADPJuzListChapterList.VHJuzChapter holder, int position) {
        holder.bind(mChapterMetas.get(position));
    }

    class VHJuzChapter extends RecyclerView.ViewHolder {
        public VHJuzChapter(@NonNull ChapterCardJuz chapterCard) {
            super(chapterCard);
            chapterCard.setBackgroundResource(R.drawable.dr_bg_hover);
        }

        public void bind(QuranMeta.ChapterMeta chapterMeta) {
            if (!(itemView instanceof ChapterCardJuz)) {
                return;
            }

            ChapterCardJuz chapterCard = (ChapterCardJuz) itemView;
            chapterCard.setChapterNumber(chapterMeta.chapterNo);
            chapterCard.setName(chapterMeta.getName(), chapterMeta.getNameTranslation());

            Pair<Integer, Integer> versesInJuz = mFragment.getQuranMeta().getVerseRangeOfChapterInJuz(mJuzNo,
                chapterMeta.chapterNo);
            if (versesInJuz != null) {
                chapterCard.setVersesCount(mVersesStr, versesInJuz.getFirst(), versesInJuz.getSecond());
                chapterCard.setOnClickListener(
                    v -> ReaderFactory.startVerseRange(v.getContext(), chapterMeta.chapterNo, versesInJuz));
            }
        }
    }
}
