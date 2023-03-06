package com.quranapp.android.views.reader.spinner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quranapp.android.R;

import java.util.List;

public abstract class ReaderSpinnerAdapter<VH extends ReaderSpinnerAdapter.VHReaderSpinner> extends
    RecyclerView.Adapter<VH> {
    private final LayoutInflater mInflater;
    private List<ReaderSpinnerItem> mItems;
    private ReaderSpinner mSpinner;
    private ReaderSpinnerItem mSelectedItem;
    private List<ReaderSpinnerItem> mStoredItems;

    public ReaderSpinnerAdapter(Context ctx, List<ReaderSpinnerItem> items) {
        mInflater = LayoutInflater.from(ctx);
        mItems = items;

        setPositionToItems(items);

        storeItems(items);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    public ReaderSpinnerItem getItem(int position) {
        return mItems.get(position);
    }

    public ReaderSpinnerItem getItemFromStoredList(int position) {
        return mStoredItems.get(position);
    }

    public ReaderSpinnerItem getSelectedItem() {
        return mSelectedItem;
    }

    public void drainItems() {
        mItems.clear();
        mStoredItems.clear();
        notifyDataSetChanged();
    }

    protected void storeItems(List<ReaderSpinnerItem> items) {
        mStoredItems = items;
    }

    protected final void setPositionToItems(List<ReaderSpinnerItem> items) {
        for (int i = 0, l = items.size(); i < l; i++) {
            items.get(i).setPosition(i);
        }
    }

    public List<ReaderSpinnerItem> getStoredItems() {
        return mStoredItems;
    }

    public void setSpinner(ReaderSpinner spinner) {
        mSpinner = spinner;
    }

    public void onItemSelect(ReaderSpinnerItem item, boolean invokeListener) {
        if (mSpinner != null) {
            mSpinner.onItemSelect(item, invokeListener);
        }

        if (mSelectedItem != null) {
            mSelectedItem.setSelected(false);
            notifyItemChanged(mSelectedItem.getPosition());
        }

        item.setSelected(true);
        notifyItemChanged(item.getPosition());


        mSelectedItem = item;
    }

    public void selectSansInvocation(int position) {
        onItemSelect(getItem(position), false);
    }

    public void setSearchItems(List<ReaderSpinnerItem> items) {
        mItems = items;
        setPositionToItems(items);
        notifyDataSetChanged();
    }

    public LayoutInflater getInflater() {
        return mInflater;
    }

    public static class VHReaderSpinner extends RecyclerView.ViewHolder {
        private final ReaderSpinnerAdapter<?> mAdapter;

        public VHReaderSpinner(ReaderSpinnerAdapter<?> adapter, @NonNull View itemView) {
            super(itemView);
            mAdapter = adapter;
            itemView.setBackgroundResource(R.drawable.dr_bg_spinner_item);
        }

        @CallSuper
        public void bind(ReaderSpinnerItem item) {
            itemView.setSelected(item.getSelected());
            itemView.setOnClickListener(v -> {
                if (mAdapter != null) {
                    mAdapter.onItemSelect(item, true);
                }
            });
        }
    }
}
