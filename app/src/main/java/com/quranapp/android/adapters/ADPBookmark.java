/*
 * (c) Faisal Khan. Created on 28/9/2021.
 */

package com.quranapp.android.adapters;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
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

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.span.LineHeightSpan2;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityBookmark;
import com.quranapp.android.adapters.extended.PeaceBottomSheetMenuAdapter;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.LytBookmarkItemBinding;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetMenu;
import com.quranapp.android.widgets.list.base.BaseListItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ADPBookmark extends RecyclerView.Adapter<ADPBookmark.VHBookmark> {
    public final Set<BookmarkModel> mSelectedModels = new LinkedHashSet<>();
    private final ActivityBookmark mActivity;
    private final AtomicReference<QuranMeta> mQuranMetaRef;
    private final int datetimeTxtSize;
    private final ColorStateList datetimeColor;
    public boolean mIsSelecting;
    private ArrayList<BookmarkModel> mBookmarkModels;

    public ADPBookmark(ActivityBookmark activity, ArrayList<BookmarkModel> bookmarkModels) {
        mActivity = activity;
        mQuranMetaRef = activity.quranMetaRef;
        mBookmarkModels = bookmarkModels;

        datetimeTxtSize = ContextKt.getDimenPx(mActivity, R.dimen.dmnCommonSize3);
        datetimeColor = ContextCompat.getColorStateList(mActivity, R.color.colorText3);
    }

    public void updateModels(ArrayList<BookmarkModel> models) {
        mBookmarkModels = models;
        clearSelection();
    }

    public void updateModel(BookmarkModel model, int position) {
        try {
            mBookmarkModels.set(position, model);
            notifyItemChanged(position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeItemFromAdapter(int position) {
        mBookmarkModels.remove(position);
        notifyItemRemoved(position);

        if (getItemCount() == 0) {
            mActivity.noSavedItems();
        }
    }

    public void onSelectionChanged(int position, BookmarkModel model, boolean selected) {
        if (mSelectedModels.isEmpty()) {
            mIsSelecting = true;
            notifyDataSetChanged();
        }

        notifyItemChanged(position);

        if (selected) {
            mSelectedModels.add(model);
        } else {
            mSelectedModels.remove(model);
        }
        mActivity.onSelection(mSelectedModels.size());

        if (mSelectedModels.isEmpty()) {
            mIsSelecting = false;
            notifyDataSetChanged();
        }
    }

    public void clearSelection() {
        mSelectedModels.clear();
        mIsSelecting = false;
        notifyDataSetChanged();
        mActivity.onSelection(0);
    }

    @Override
    public int getItemCount() {
        return mBookmarkModels.size();
    }

    @NonNull
    @Override
    public VHBookmark onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LytBookmarkItemBinding binding = LytBookmarkItemBinding.inflate(LayoutInflater.from(parent.getContext()),
            parent, false);
        return new VHBookmark(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VHBookmark holder, int position) {
        holder.bind(mBookmarkModels.get(position));
    }

    private CharSequence prepareTexts(String title, CharSequence subTitle, String datetime) {
        SpannableString titleSS = new SpannableString(title);
        TextAppearanceSpan titleTASpan = new TextAppearanceSpan("sans-serif", Typeface.BOLD, -1, null, null);
        titleSS.setSpan(titleTASpan, 0, titleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString subTitleSS = new SpannableString(subTitle);
        TextAppearanceSpan subTitleTASpan = new TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, -1, null, null);
        subTitleSS.setSpan(subTitleTASpan, 0, subTitleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        subTitleSS.setSpan(new LineHeightSpan2(20, false, true), 0, subTitleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString datetimeSS = new SpannableString(datetime);
        TextAppearanceSpan datetimeTASpan = new TextAppearanceSpan("sans-serif", Typeface.NORMAL, datetimeTxtSize,
            datetimeColor,
            null);
        datetimeSS.setSpan(datetimeTASpan, 0, datetimeSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", datetimeSS);
    }

    protected class VHBookmark extends RecyclerView.ViewHolder {
        private final LytBookmarkItemBinding mBinding;

        public VHBookmark(@NonNull LytBookmarkItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            binding.getRoot().setElevation(Dimen.dp2px(binding.getRoot().getContext(), 4));
            binding.getRoot().setBackgroundResource(R.drawable.dr_bg_chapter_card);
        }

        public void bind(BookmarkModel model) {
            boolean isSelected = mSelectedModels.contains(model);
            mBinding.chapterNo.setVisibility(isSelected ? View.GONE : View.VISIBLE);
            mBinding.menu.setVisibility(mIsSelecting ? View.GONE : View.VISIBLE);
            mBinding.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            mBinding.thumb.setSelected(isSelected);

            if (!isSelected) {
                mBinding.chapterNo.setText(String.format(Locale.getDefault(), "%d", model.getChapterNo()));
            }

            String chapterName = mQuranMetaRef.get().getChapterName(itemView.getContext(), model.getChapterNo(), true);
            CharSequence txt = prepareTexts(chapterName,
                mActivity.prepareSubtitleTitle(model.getFromVerseNo(), model.getToVerseNo()),
                model.getFormattedDate(mActivity));
            mBinding.text.setText(txt);

            mBinding.getRoot().setOnLongClickListener(v -> {
                onSelectionChanged(getAdapterPosition(), model, !mSelectedModels.contains(model));
                return true;
            });

            mBinding.getRoot().setOnClickListener(v -> {
                if (mIsSelecting) {
                    onSelectionChanged(getAdapterPosition(), model, !mSelectedModels.contains(model));
                } else {
                    mActivity.onView(model, getAdapterPosition());
                }
            });
            mBinding.menu.setOnClickListener(v -> {
                if (!mIsSelecting) {
                    final String title;
                    if (model.getFromVerseNo() == model.getToVerseNo()) {
                        title = String.format("%s %d:%d", chapterName, model.getChapterNo(), model.getFromVerseNo());
                    } else {
                        title = String.format("%s %d:%d-%d", chapterName, model.getChapterNo(), model.getFromVerseNo(),
                            model.getToVerseNo());
                    }
                    openItemMenu(title, model);
                }
            });
        }


        private void openItemMenu(String title, BookmarkModel model) {
            PeaceBottomSheetMenu dialog = new PeaceBottomSheetMenu();
            dialog.getParams().setHeaderTitle(title);

            TypedArray ta = mActivity.typedArray(R.array.arrBookmarkItemMenuIcons);
            String[] labels = mActivity.strArray(R.array.arrBookmarkItemMenuLabels);
            String[] descs = mActivity.strArray(R.array.arrBookmarkItemMenuDescs);

            PeaceBottomSheetMenuAdapter adapter = new PeaceBottomSheetMenuAdapter(mActivity);
            for (int i = 0, l = labels.length; i < l; i++) {
                String label = labels[i];
                BaseListItem item = new BaseListItem(ta.getResourceId(i, 0), label);
                item.setMessage(descs[i]);
                item.setPosition(i);
                adapter.addItem(item);
            }

            dialog.setAdapter(adapter);
            dialog.setOnItemClickListener((dialog1, item) -> {
                dialog1.dismiss();

                switch (item.getPosition()) {
                    case 0: mActivity.onView(model, getAdapterPosition()); break;
                    case 1: mActivity.onOpen(model); break;
                    case 2: mActivity.removeVerseFromBookmark(model, getAdapterPosition()); break;
                }
            });

            dialog.show(mActivity.getSupportFragmentManager());
        }
    }
}
