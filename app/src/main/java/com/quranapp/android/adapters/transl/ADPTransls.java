/*
 * Created by Faisal Khan on (c) 22/8/2021.
 */

package com.quranapp.android.adapters.transl;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.widget.checkbox.PeaceCheckBox;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.transls.TranslBaseModel;
import com.quranapp.android.components.transls.TranslModel;
import com.quranapp.android.components.transls.TranslTitleModel;
import com.quranapp.android.databinding.LytSettingsTranslItemBinding;
import com.quranapp.android.interfaceUtils.OnTranslSelectionChangeListener;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslFactory;

import java.util.List;

public class ADPTransls extends ADPTranslBase<ADPTransls.VHTransl> {
    private final boolean mDeleteAllowed;
    private final OnTranslSelectionChangeListener<TranslModel> mListener;

    public ADPTransls(Context context, List<TranslBaseModel> models, boolean deleteAllowed, OnTranslSelectionChangeListener<TranslModel> l) {
        super(context, models);
        mDeleteAllowed = deleteAllowed;
        mListener = l;
    }


    @NonNull
    @Override
    public VHTransl onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final VHTransl vh;

        if (viewType == 0) {
            vh = new VHTransl(createTitleView(parent.getContext()));
        } else {
            vh = new VHTransl(LytSettingsTranslItemBinding.inflate(mInflater, parent, false));
        }

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VHTransl holder, int position) {
        TranslBaseModel model = mModelsVisible.get(position);

        if (model instanceof TranslTitleModel && holder.itemView instanceof TextView) {
            ((TextView) holder.itemView).setText(((TranslTitleModel) model).getLangName());
        } else if (model instanceof TranslModel && holder.itemView instanceof PeaceCheckBox) {
            holder.bind((TranslModel) model);
        }
    }

    class VHTransl extends RecyclerView.ViewHolder {
        private LytSettingsTranslItemBinding mBinding;

        public VHTransl(@NonNull View itemView) {
            super(itemView);
        }

        public VHTransl(@NonNull LytSettingsTranslItemBinding binding) {
            this(binding.getRoot());
            mBinding = binding;
        }

        public void bind(TranslModel translModel) {
            if (mBinding == null) {
                return;
            }

            mBinding.checkbox.setTexts(translModel.getBookInfo().getBookName(), translModel.getBookInfo().getAuthorName());
            mBinding.checkbox.setChecked(translModel.isChecked());

            mBinding.checkbox.setOnBeforeCheckChangedListener((button, newState) -> {
                if (mListener.onSelectionChanged(button.getContext(), translModel, newState)) {
                    translModel.setChecked(newState);
                    return true;
                }

                return false;
            });

            mBinding.getRoot().setOnLongClickListener(v -> {
                if (mDeleteAllowed && !TranslUtils.isPrebuilt(translModel.getBookInfo().getSlug())) {
                    deleteTranslCheckPoint(v.getContext(), translModel);
                    return true;
                }
                return false;
            });
        }

        private void deleteTranslCheckPoint(Context ctx, TranslModel translModel) {
            QuranTranslBookInfo bookInfo = translModel.getBookInfo();
            PeaceDialog.Builder builder = PeaceDialog.newBuilder(ctx);
            builder.setTitle(R.string.strTitleTranslDelete);
            builder.setMessage(
                    bookInfo.getBookName() + "\n" + bookInfo.getAuthorName() + "\n\n" + "You will need to download it again.");
            builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            builder.setNeutralButton(R.string.strLabelCancel, null);
            builder.setNegativeButton(R.string.strLabelDelete, ColorUtils.DANGER, (dialog, which) -> {
                QuranTranslFactory factory = new QuranTranslFactory(ctx);
                factory.deleteTranslation(bookInfo.getSlug());
                factory.close();

                translModel.setChecked(false);
                mListener.onSelectionChanged(ctx, translModel, false);
                remove(bookInfo.getSlug());
            });
            builder.setFocusOnNegative(true);
            builder.show();
        }
    }
}
