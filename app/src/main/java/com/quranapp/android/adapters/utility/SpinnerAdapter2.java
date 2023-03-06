package com.quranapp.android.adapters.utility;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.quranapp.android.R;
import com.quranapp.android.components.utility.SpinnerItem;
import com.quranapp.android.views.helper.Spinner2;

import java.util.List;

public class SpinnerAdapter2<T extends SpinnerItem> extends ArrayAdapter<T> {
    private final List<T> mSpinnerItems;
    private final int mTextViewId;
    private final int mColorPrimary;
    private final int mColorWhite;
    private final int mColorText;
    private Spinner2 mSpinner;

    public SpinnerAdapter2(@NonNull Context context, int itemLytRes, int textViewId, @NonNull List<T> objects) {
        super(context, itemLytRes, textViewId, objects);
        mTextViewId = textViewId;
        mSpinnerItems = objects;
        mColorPrimary = ContextCompat.getColor(context, R.color.colorPrimary);
        mColorWhite = ContextCompat.getColor(context, R.color.white);
        mColorText = ContextCompat.getColor(context, R.color.colorText);
    }

    @Override
    public int getCount() {
        return mSpinnerItems.size();
    }

    @NonNull
    @Override
    public T getItem(int position) {
        return mSpinnerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mSpinnerItems.get(position).getId();
    }

    @Override
    public boolean isEmpty() {
        return mSpinnerItems.isEmpty();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = view.findViewById(mTextViewId);
        textView.setText(getItem(position).getName());
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = view.findViewById(mTextViewId);
        textView.setText(getItem(position).getName());

        if (mSpinner != null) {
            boolean isSelected = mSpinner.getSelectedItemPosition() == position;
            textView.setBackgroundColor(isSelected ? mColorPrimary : Color.TRANSPARENT);
            textView.setTextColor(isSelected ? mColorWhite : mColorText);
        }

        return view;
    }

    public Spinner2 getSpinner() {
        return mSpinner;
    }

    public void setSpinner(Spinner2 spinner) {
        mSpinner = spinner;
    }
}
