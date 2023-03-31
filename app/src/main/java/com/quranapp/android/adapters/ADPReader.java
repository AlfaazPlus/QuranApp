package com.quranapp.android.adapters;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.BISMILLAH;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.CHAPTER_INFO;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.CHAPTER_TITLE;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.IS_VOTD;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.READER_FOOTER;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.VERSE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.components.reader.ReaderRecyclerItemModel;
import com.quranapp.android.components.utility.CardMessageParams;
import com.quranapp.android.databinding.LytReaderIsVotdBinding;
import com.quranapp.android.views.CardMessage;
import com.quranapp.android.views.reader.BismillahView;
import com.quranapp.android.views.reader.ChapterInfoCardView;
import com.quranapp.android.views.reader.ChapterTitleView;
import com.quranapp.android.views.reader.ReaderFooter;
import com.quranapp.android.views.reader.VerseView;

import java.util.ArrayList;

public class ADPReader extends RecyclerView.Adapter<ADPReader.VHReader> {
    private final QuranMeta.ChapterMeta mChapterInfoMeta;
    private final ActivityReader mActivity;
    private final ArrayList<ReaderRecyclerItemModel> mModels;

    public ADPReader(ActivityReader activity, QuranMeta.ChapterMeta chapterInfoMeta, ArrayList<ReaderRecyclerItemModel> models) {
        mActivity = activity;
        mChapterInfoMeta = chapterInfoMeta;
        mModels = models;

        if (chapterInfoMeta != null) {
            models.add(0, new ReaderRecyclerItemModel().setViewType(CHAPTER_INFO));
        }
        models.add(models.size(), new ReaderRecyclerItemModel().setViewType(READER_FOOTER));

        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return getViewType(position);
    }

    private int getViewType(int position) {
        return mModels.get(position).getViewType();
    }

    public ReaderRecyclerItemModel getItem(int position) {
        return mModels.get(position);
    }

    public void highlightVerseOnScroll(int position) {
        getItem(position).setScrollHighlightPending(true);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public VHReader onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view;
        if (viewType == IS_VOTD) {
            view = makeIsVotdView(mActivity, parent);
        } else if (viewType == NO_TRANSL_SELECTED) {
            view = prepareNoTranslMessageView(mActivity);
        } else if (viewType == CHAPTER_INFO) {
            view = new ChapterInfoCardView(mActivity);
        } else if (viewType == BISMILLAH) {
            view = new BismillahView(mActivity);
        } else if (viewType == VERSE) {
            view = new VerseView(mActivity, parent, null, false);
        } else if (viewType == CHAPTER_TITLE) {
            view = new ChapterTitleView(mActivity);
        } else if (viewType == READER_FOOTER) {
            final ReaderFooter footer = mActivity.mNavigator.readerFooter;
            footer.clearParent();
            view = footer;
        } else {
            view = new View(mActivity);
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        } else {
            params.width = MATCH_PARENT;
        }
        view.setLayoutParams(params);

        return new VHReader(view);
    }

    private View makeIsVotdView(ActivityReader activity, ViewGroup parent) {
        LytReaderIsVotdBinding binding = LytReaderIsVotdBinding.inflate(activity.getLayoutInflater(), parent, false);
        return binding.getRoot();
    }

    private View prepareNoTranslMessageView(ActivityReader activity) {
        CardMessage msgView = new CardMessage(activity);
        msgView.setMessage(activity.str(R.string.strMsgTranslNoneSelected));
        msgView.setElevation(activity.dp2px(4));
        msgView.setMessageStyle(CardMessageParams.STYLE_WARNING);
        msgView.setActionText(activity.str(R.string.strTitleSettings),
            () -> activity.mBinding.readerHeader.openReaderSetting(ActivitySettings.SETTINGS_TRANSLATION));
        return msgView;
    }

    @Override
    public void onBindViewHolder(@NonNull VHReader holder, int position) {
        final ReaderRecyclerItemModel model = mModels.get(position);
        holder.bind(model);
    }

    public class VHReader extends RecyclerView.ViewHolder {
        public VHReader(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(ReaderRecyclerItemModel model) {
            final int position = getAdapterPosition();

            int viewType = getViewType(position);

            switch (viewType) {
                case CHAPTER_INFO: ((ChapterInfoCardView) itemView).setInfo(mChapterInfoMeta);
                    break;
                case VERSE: setupVerseView(model);
                    break;
                case CHAPTER_TITLE: setupTitleView(model);
                    break;
            }
        }

        private void setupVerseView(ReaderRecyclerItemModel model) {
            if (!(itemView instanceof VerseView)) {
                return;
            }

            final Verse verse = model.getVerse();

            VerseView verseView = (VerseView) itemView;
            verseView.setVerse(verse);

            if (model.isScrollHighlightPending()) {
                verseView.highlightOnScroll();
                model.setScrollHighlightPending(false);
            }

            if (mActivity.mPlayer != null) {
                verseView.onRecite(mActivity.mPlayer.isReciting(verse.chapterNo, verse.verseNo));
            }
        }

        private void setupTitleView(ReaderRecyclerItemModel model) {
            if (!(itemView instanceof ChapterTitleView)) {
                return;
            }

            ChapterTitleView chapterTitleView = (ChapterTitleView) itemView;
            chapterTitleView.setChapterNumber(model.getChapterNo());
        }
    }
}
