/*
 * (c) Faisal Khan. Created on 4/10/2021.
 */

package com.quranapp.android.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.NotifModel;
import com.quranapp.android.databinding.LytNotifItemBinding;

import java.util.ArrayList;

public class ADPNotifs extends RecyclerView.Adapter<ADPNotifs.VHNotif> {
    private final ArrayList<NotifModel> mModels;

    public ADPNotifs(ArrayList<NotifModel> mModels) {
        this.mModels = mModels;
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }

    @NonNull
    @Override
    public VHNotif onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LytNotifItemBinding binding = LytNotifItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VHNotif(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VHNotif holder, int position) {
        holder.bind(mModels.get(position));
    }

    public static class VHNotif extends RecyclerView.ViewHolder {
        private final LytNotifItemBinding mBinding;

        public VHNotif(@NonNull LytNotifItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        public void bind(NotifModel model) {
            try {
                mBinding.thumb.setImageResource(model.getIconRes());
            } catch (Exception ignored) {
                mBinding.thumb.setImageResource(R.drawable.dr_icon_notifications);
            }

            mBinding.title.setText(model.getTitle());
            mBinding.message.setText(model.getMessage());
            mBinding.date.setText(model.getDate());

            itemView.setElevation(Dimen.dp2px(itemView.getContext(), 5));

            if (model.getAction() != null) {
                ViewUtils.addHoverOpacityEffect(itemView);
                itemView.setOnClickListener(v -> performAction(model.getAction()));
            }
        }

        private void performAction(String action) {

        }
    }
}
