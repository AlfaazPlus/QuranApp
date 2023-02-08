/*
 * Created by Faisal Khan on (c) 22/8/2021.
 */

package com.quranapp.android.adapters.recitation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.databinding.LytSettingsRecitationItemBinding;
import com.quranapp.android.utils.sharedPrefs.SPReader;

import java.util.ArrayList;
import java.util.List;

public class ADPRecitations extends RecyclerView.Adapter<ADPRecitations.VHRecitation> {
    private List<RecitationModel> mModels;
    private int mSelectedPos = -1;

    public ADPRecitations() {
        setHasStableIds(true);
    }

    public void setModels(List<RecitationModel> models) {
        mModels = new ArrayList<>(models);
    }

    @NonNull
    @Override
    public VHRecitation onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new VHRecitation(LytSettingsRecitationItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VHRecitation holder, int position) {
        holder.bind(mModels.get(position));
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class VHRecitation extends RecyclerView.ViewHolder {
        private final LytSettingsRecitationItemBinding mBinding;

        public VHRecitation(@NonNull LytSettingsRecitationItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.radio.setClickable(false);
            mBinding.radio.setFocusable(false);
        }

        public void bind(RecitationModel model) {
            if (mBinding == null) {
                return;
            }

            mBinding.reciter.setText(model.getReciter());

            if (TextUtils.isEmpty(model.getStyle())) {
                mBinding.style.setVisibility(View.GONE);
            } else {
                mBinding.style.setVisibility(View.VISIBLE);
                mBinding.style.setText(model.getStyle());
            }

            mBinding.radio.setChecked(model.isChecked());
            if (model.isChecked()) {
                mSelectedPos = getAdapterPosition();
            }

            mBinding.radio.setVisibility(View.VISIBLE);

            mBinding.getRoot().setOnClickListener(v -> {
                select(getAdapterPosition());
                mBinding.radio.setChecked(true);
                SPReader.setSavedRecitationSlug(v.getContext(), model.getSlug());
            });
        }

        private void select(int position) {
            try {
                RecitationModel oldModel = mModels.get(mSelectedPos);
                if (oldModel != null) {
                    oldModel.setChecked(false);
                    notifyItemChanged(mSelectedPos);
                }
            } catch (Exception ignored) {}

            RecitationModel newModel = mModels.get(position);
            if (newModel != null) {
                newModel.setChecked(true);
                notifyItemChanged(position);
            }

            mSelectedPos = position;
        }
    }
}
