/*
 * Created by Faisal Khan on (c) 22/8/2021.
 */

package com.quranapp.android.adapters.transl;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.Log;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.transls.TranslBaseModel;
import com.quranapp.android.components.transls.TranslModel;
import com.quranapp.android.components.transls.TranslTitleModel;
import com.quranapp.android.databinding.LytSettingsDownlTranslItemBinding;
import com.quranapp.android.interfaceUtils.TranslDownloadExplorerImpl;

import java.util.List;
import java.util.Objects;

public class ADPDownloadTransls extends ADPTranslBase<ADPDownloadTransls.VHDownloadTransl> {
    private final TranslDownloadExplorerImpl mExplorerImpl;

    public ADPDownloadTransls(Context ctx, TranslDownloadExplorerImpl explorerImpl, List<TranslBaseModel> models) {
        super(ctx, models);
        mExplorerImpl = explorerImpl;

        setHasStableIds(true);
    }

    @Override
    public TranslModel getItemVisible(int position) {
        return (TranslModel) super.getItemVisible(position);
    }

    @NonNull
    @Override
    public VHDownloadTransl onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final VHDownloadTransl vh;

        if (viewType == 0) {
            vh = new VHDownloadTransl(createTitleView(parent.getContext()));
        } else {
            vh = new VHDownloadTransl(LytSettingsDownlTranslItemBinding.inflate(mInflater, parent, false));
        }

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VHDownloadTransl holder, int position) {
        TranslBaseModel model = mModelsVisible.get(position);

        if (model instanceof TranslTitleModel) {
            ((TextView) holder.itemView).setText(((TranslTitleModel) model).getLangName());
        } else if (model instanceof TranslModel) {
            holder.bind((TranslModel) model);
        }
    }

    public void onDownloadStatus(String slug, boolean downloading) {
        boolean foundTheDownloadModel = false;
        for (TranslBaseModel visibleModel : mModelsStored) {
            if (visibleModel instanceof TranslModel) {
                final TranslModel model = (TranslModel) visibleModel;
                if (!foundTheDownloadModel && Objects.equals(slug, model.getBookInfo().getSlug())) {
                    foundTheDownloadModel = true;
                    model.setDownloading(downloading);
                } else {
                    model.setDownloadingDisabled(downloading);
                }
            }
        }

        foundTheDownloadModel = false;

        for (TranslBaseModel visibleModel : mModelsVisible) {
            if (visibleModel instanceof TranslModel) {
                final TranslModel model = (TranslModel) visibleModel;
                if (!foundTheDownloadModel && Objects.equals(slug, model.getBookInfo().getSlug())) {
                    foundTheDownloadModel = true;
                    model.setDownloading(downloading);
                } else {
                    model.setDownloadingDisabled(downloading);
                }
            }
        }

        notifyDataSetChanged();
    }

    public class VHDownloadTransl extends RecyclerView.ViewHolder {
        private LytSettingsDownlTranslItemBinding mBinding;

        public VHDownloadTransl(View itemView) {
            super(itemView);
        }

        public VHDownloadTransl(@NonNull LytSettingsDownlTranslItemBinding binding) {
            this(binding.getRoot());
            mBinding = binding;
        }

        public void bind(TranslModel translModel) {
            if (mBinding == null) {
                return;
            }
            QuranTranslBookInfo bookInfo = translModel.getBookInfo();
            mBinding.book.setText(bookInfo.getBookName());

            if (TextUtils.isEmpty(bookInfo.getAuthorName())) {
                mBinding.author.setVisibility(View.GONE);
            } else {
                mBinding.author.setVisibility(View.VISIBLE);
                mBinding.author.setText(bookInfo.getAuthorName());
            }

            mBinding.loader.setVisibility(translModel.isDownloading() ? View.VISIBLE : View.GONE);
            mBinding.iconDownload.setVisibility(translModel.isDownloading() ? View.GONE : View.VISIBLE);
            ViewUtils.disableView(mBinding.iconDownload, translModel.isDownloadingDisabled());

            createMiniInfos(mBinding.miniInfosCont, translModel.getMiniInfos());

            mBinding.getRoot().setClickable(!translModel.isDownloading() && !translModel.isDownloadingDisabled());
            mBinding.getRoot().setOnClickListener(v -> {
                if (translModel.isDownloadingDisabled()) {
                    return;
                }

                mExplorerImpl.onDownloadAttempt(this, v, translModel);
            });
        }

        private void createMiniInfos(LinearLayout container, List<String> miniInfos) {
            container.removeAllViews();
            Context ctx = container.getContext();

            for (String miniInfo : miniInfos) {
                if (TextUtils.isEmpty(miniInfo)) {
                    continue;
                }

                AppCompatTextView miniInfoView = new AppCompatTextView(container.getContext());
                miniInfoView.setText(miniInfo);
                miniInfoView.setBackgroundResource(R.drawable.dr_bg_primary_cornered);
                miniInfoView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                miniInfoView.setTextColor(ContextCompat.getColor(ctx, R.color.white));
                miniInfoView.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDimenPx(ctx, R.dimen.dmnCommonSize2_5));

                ViewUtils.setPaddingVertical(miniInfoView, Dimen.dp2px(ctx, 2));
                ViewUtils.setPaddingHorizontal(miniInfoView, Dimen.dp2px(ctx, 5));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                lp.setMarginEnd(Dimen.dp2px(ctx, 6));
                container.addView(miniInfoView, lp);
            }
        }
    }
}
