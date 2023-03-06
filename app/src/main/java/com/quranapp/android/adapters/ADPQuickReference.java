package com.quranapp.android.adapters;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.views.reader.VerseView;

import java.util.List;

public class ADPQuickReference extends RecyclerView.Adapter<ADPQuickReference.VHReader> {
    private final ReaderPossessingActivity mActivity;
    private List<Verse> mVerses;

    public ADPQuickReference(ReaderPossessingActivity activity) {
        mActivity = activity;

        setHasStableIds(true);
    }

    public void setVerses(List<Verse> verses) {
        mVerses = verses;
    }

    @Override
    public int getItemCount() {
        return mVerses.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public VHReader onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        return new VHReader(new VerseView(mActivity, parent, null, true));
    }

    @Override
    public void onBindViewHolder(@NonNull VHReader holder, int position) {
        holder.bind(mVerses.get(position));
    }


    public static class VHReader extends RecyclerView.ViewHolder {
        private final VerseView mVerseView;

        public VHReader(@NonNull VerseView verseView) {
            super(verseView);
            mVerseView = verseView;
        }

        public void bind(Verse verse) {
            if (mVerseView == null) {
                return;
            }
            mVerseView.setVerse(verse);
        }
    }
}
