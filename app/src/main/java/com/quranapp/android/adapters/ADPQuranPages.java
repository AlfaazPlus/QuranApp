package com.quranapp.android.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.CHAPTER_INFO;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.READER_FOOTER;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.READER_PAGE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.reader.QuranPageModel;
import com.quranapp.android.components.reader.QuranPageSectionModel;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.views.reader.ChapterInfoCardView;
import com.quranapp.android.views.reader.QuranPageView;
import com.quranapp.android.views.reader.ReaderFooter;

import java.util.ArrayList;

public class ADPQuranPages extends RecyclerView.Adapter<ADPQuranPages.VHQuranPage> {
    private final QuranMeta.ChapterMeta mChapterInfoMeta;
    private final ArrayList<QuranPageModel> mModels;
    private final ActivityReader mActivity;

    public ADPQuranPages(ActivityReader activity, QuranMeta.ChapterMeta chapterInfoMeta, ArrayList<QuranPageModel> models) {
        mActivity = activity;
        mChapterInfoMeta = chapterInfoMeta;
        mModels = models;

        if (chapterInfoMeta != null) {
            models.add(0, new QuranPageModel().setViewType(CHAPTER_INFO));
        }
        models.add(models.size(), new QuranPageModel().setViewType(READER_FOOTER));

        for (int i = 0; i < models.size(); i++) {
            QuranPageModel model = models.get(i);
            if (model.getViewType() == READER_PAGE) {
                for (QuranPageSectionModel section : model.getSections()) {
                    section.parentIndexInAdapter = i;
                }
            }
        }

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
        return position;
    }

    private int getViewType(int position) {
        return mModels.get(position).getViewType();
    }

    public void highlightVerseOnScroll(int position, int chapterNo, int verseNo) {
        QuranPageModel pageModel = getPageModel(position);
        pageModel.setScrollHighlightPendingChapterNo(chapterNo);
        pageModel.setScrollHighlightPendingVerseNo(verseNo);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public VHQuranPage onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        int viewType = getViewType(position);
        View view;
        if (viewType == CHAPTER_INFO) {
            view = new ChapterInfoCardView(mActivity);
        } else if (viewType == READER_PAGE) {
            final QuranPageView quranPageView = new QuranPageView(mActivity);
            final ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT);
            LayoutParamsKt.updateMargins(params, Dimen.dp2px(parent.getContext(), 3));
            quranPageView.setLayoutParams(params);
            view = quranPageView;
        } else if (viewType == READER_FOOTER) {
            final ReaderFooter footer = mActivity.mNavigator.readerFooter;
            footer.clearParent();
            view = footer;
        } else {
            view = new View(parent.getContext());
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        } else {
            params.width = MATCH_PARENT;
        }
        view.setLayoutParams(params);

        return new VHQuranPage(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VHQuranPage holder, int position) {
        holder.bind(mModels.get(position));
    }

    public QuranPageModel getPageModel(int position) {
        return mModels.get(position);
    }

    class VHQuranPage extends RecyclerView.ViewHolder {

        public VHQuranPage(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(QuranPageModel pageModel) {
            final int position = getBindingAdapterPosition();

            int viewType = getViewType(position);

            if (viewType == CHAPTER_INFO) {
                ((ChapterInfoCardView) itemView).setInfo(mChapterInfoMeta);
            } else if (viewType == READER_PAGE) {
                ((QuranPageView) itemView).setPageModel(pageModel);
            }
        }
    }
}
