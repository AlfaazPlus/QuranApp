package com.quranapp.android.adapters.utility;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import com.quranapp.android.views.helper.Spinner2;

public class DynamicWidthSpinnerAdapter implements SpinnerAdapter {
    private final Spinner2 mSpinner;
    private final SpinnerAdapter mBaseAdapter;

    public DynamicWidthSpinnerAdapter(Spinner2 spinner, SpinnerAdapter baseAdapter) {
        mSpinner = spinner;
        mBaseAdapter = baseAdapter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return mBaseAdapter.getView(mSpinner.getSelectedItemPosition(), convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return mBaseAdapter.getDropDownView(position, convertView, parent);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mBaseAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mBaseAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public int getCount() {
        return mBaseAdapter.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mBaseAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mBaseAdapter.getItemId(position);
    }

    @Override
    public boolean hasStableIds() {
        return mBaseAdapter.hasStableIds();
    }

    @Override
    public int getItemViewType(int position) {
        return mBaseAdapter.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return mBaseAdapter.getViewTypeCount();
    }

    @Override
    public boolean isEmpty() {
        return mBaseAdapter.isEmpty();
    }
}
