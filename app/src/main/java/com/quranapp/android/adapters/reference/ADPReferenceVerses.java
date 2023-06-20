package com.quranapp.android.adapters.reference;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.peacedesign.android.utils.span.TypefaceSpan2;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.reference.ActivityReference;
import com.quranapp.android.components.ReferenceVerseItemModel;
import com.quranapp.android.components.ReferenceVerseModel;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.databinding.LytActivityReferenceDescriptionBinding;
import com.quranapp.android.databinding.LytReferenceVerseTitleBinding;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.views.reader.VerseView;

import java.util.ArrayList;
import java.util.List;

public class ADPReferenceVerses extends RecyclerView.Adapter<ADPReferenceVerses.VHReferenceVerse> implements
    Destroyable {
    public static final int VIEWTYPE_DESCRIPTION = 0x0;
    public static final int VIEWTYPE_TITLE = 0x1;
    public static final int VIEWTYPE_VERSE = 0x2;
    private ActivityReference mActivity;
    private List<ReferenceVerseItemModel> mVerseModels = new ArrayList<>();
    private LayoutInflater mInflater;
    private ReferenceVerseModel mRefModel;

    public ADPReferenceVerses(ActivityReference activity, ReferenceVerseModel refModel) {
        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        mRefModel = refModel;

        setHasStableIds(true);
    }

    public void setVerseModels(List<ReferenceVerseItemModel> verseModels) {
        mVerseModels = verseModels;
    }

    @Override
    public int getItemCount() {
        return mVerseModels.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mVerseModels.get(position).getViewType();
    }

    @NonNull
    @Override
    public VHReferenceVerse onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final VHReferenceVerse vh;
        if (viewType == VIEWTYPE_DESCRIPTION) {
            LytActivityReferenceDescriptionBinding binding = LytActivityReferenceDescriptionBinding.inflate(mInflater,
                parent, false);
            vh = new VHReferenceVerse(binding);
        } else if (viewType == VIEWTYPE_TITLE) {
            LytReferenceVerseTitleBinding binding = LytReferenceVerseTitleBinding.inflate(mInflater, parent, false);
            vh = new VHReferenceVerse(binding);
        } else if (viewType == VIEWTYPE_VERSE) {
            VerseView verseView = new VerseView(mActivity, parent, null, false);
            vh = new VHReferenceVerse(verseView);
        } else {
            vh = new VHReferenceVerse(new View(parent.getContext()));
        }

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VHReferenceVerse holder, int position) {
        holder.bind(mVerseModels.get(position));
    }

    @Override
    public void destroy() {
        mActivity = null;
        mVerseModels.clear();
        mVerseModels = null;
        mInflater = null;
        mRefModel = null;
    }

    class VHReferenceVerse extends RecyclerView.ViewHolder implements BookmarkCallbacks {
        private LytActivityReferenceDescriptionBinding mDescBinding;
        private LytReferenceVerseTitleBinding mTitleBinding;
        private VerseView mVerseView;

        public VHReferenceVerse(View itemView) {
            super(itemView);
        }

        public VHReferenceVerse(LytActivityReferenceDescriptionBinding binding) {
            super(binding.getRoot());
            mDescBinding = binding;

            int[] bgColors = {mActivity.color(R.color.colorBGPageVariableInverse), Color.TRANSPARENT};
            GradientDrawable bgGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, bgColors);
            mDescBinding.getRoot().setBackground(bgGradient);
        }

        public VHReferenceVerse(@NonNull VerseView verseView) {
            super(verseView);
            mVerseView = verseView;
            verseView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        }

        public VHReferenceVerse(LytReferenceVerseTitleBinding binding) {
            this(binding.getRoot());
            mTitleBinding = binding;
            binding.container.setBackground(prepareVerseTitleBG(itemView.getContext()));
        }

        public void bind(ReferenceVerseItemModel model) {
            if (model.getViewType() == VIEWTYPE_DESCRIPTION) {
                bindReferenceDesc(mDescBinding, mRefModel);
            } else if (model.getViewType() == VIEWTYPE_TITLE) {
                bindTitle(mTitleBinding, model);
            } else if (model.getViewType() == VIEWTYPE_VERSE && mVerseView != null) {
                mVerseView.setVerse(model.getVerse());
            }
        }

        private void bindReferenceDesc(LytActivityReferenceDescriptionBinding binding, ReferenceVerseModel model) {
            if (binding == null) {
                return;
            }

            SpannableStringBuilder sb = new SpannableStringBuilder();

            if (!TextUtils.isEmpty(model.getTitle())) {
                SpannableString titleSS = new SpannableString(model.getTitle());
                Typeface typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
                titleSS.setSpan(new TypefaceSpan2(typeface), 0, titleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(titleSS);
            }

            if (!TextUtils.isEmpty(model.getDesc())) {
                if (!TextUtils.isEmpty(model.getTitle())) {
                    SpannableString newLineSS = new SpannableString("\n\n");
                    newLineSS.setSpan(new AbsoluteSizeSpan(10), 0, newLineSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(newLineSS);
                }

                SpannableString descSS = new SpannableString(model.getDesc());
                Typeface typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
                descSS.setSpan(new TypefaceSpan2(typeface), 0, descSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(descSS);
            }


            binding.getRoot().setText(sb);
        }

        private void bindTitle(LytReferenceVerseTitleBinding binding, ReferenceVerseItemModel model) {
            if (binding == null) {
                return;
            }

            binding.titleText.setText(model.getTitleText());
            binding.btnBookmark.setImageResource(
                model.getBookmarked() ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined
            );
            binding.btnBookmark.setColorFilter(
                model.getBookmarked() ? mActivity.color(R.color.colorPrimary) : mActivity.color(R.color.colorIcon)
            );
            binding.btnBookmark.setOnClickListener(v -> {
                if (model.getBookmarked()) {
                    mActivity.onBookmarkView(model.getChapterNo(), model.getFromVerse(), model.getToVerse(), this);
                } else {
                    mActivity.addVerseToBookmark(model.getChapterNo(), model.getFromVerse(), model.getToVerse(), this);
                }
            });
            binding.openInReader.setOnTouchListener(new HoverPushOpacityEffect());
            binding.openInReader.setOnClickListener(v -> {
                Intent intent = ReaderFactory.prepareVerseRangeIntent(model.getChapterNo(), model.getFromVerse(),
                    model.getToVerse());
                intent.setClass(mActivity, ActivityReader.class);
                intent.putExtra(READER_KEY_TRANSL_SLUGS, mActivity.mSelectedTranslSlugs.toArray(new String[0]));
                intent.putExtra(READER_KEY_SAVE_TRANSL_CHANGES, false);
                mActivity.startActivity(intent);
            });
        }


        @Override
        public void onBookmarkRemoved(BookmarkModel model) {
            int adapterPosition = getBindingAdapterPosition();
            mVerseModels.get(adapterPosition).setBookmarked(false);
            notifyItemChanged(adapterPosition);
        }

        @Override
        public void onBookmarkAdded(BookmarkModel model) {
            int adapterPosition = getBindingAdapterPosition();
            mVerseModels.get(adapterPosition).setBookmarked(true);
            notifyItemChanged(adapterPosition);
        }
    }

    private Drawable prepareVerseTitleBG(Context context) {
        int bgColor = ContextCompat.getColor(context, R.color.colorBGCardVariable);
        int borderColor = ContextCompat.getColor(context, R.color.colorDividerVariable);

        int strokeWidth = Dimen.dp2px(mActivity, 1);
        int[] strokeWidths = {strokeWidth, strokeWidth, strokeWidth, 0};

        float[] radii = Dimen.createRadiiForBGInDP(mActivity, 5, 5, 0, 0);
        return DrawableUtils.createBackgroundStroked(bgColor, borderColor, strokeWidths, radii);
    }
}
