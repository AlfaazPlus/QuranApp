package com.quranapp.android.views.reader.juzSpinner;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quranapp.android.R;
import com.quranapp.android.views.reader.spinner.ReaderSpinner;
import com.quranapp.android.views.reader.spinner.ReaderSpinnerItem;

import java.util.regex.Pattern;

public class JuzSpinner extends ReaderSpinner {
    private TextView mJuzIconView;

    public JuzSpinner(@NonNull Context context) {
        super(context);
    }

    public JuzSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JuzSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean search(ReaderSpinnerItem item, Pattern pattern) {
        JuzSpinnerItem juzSpinnerItem = (JuzSpinnerItem) item;
        return pattern.matcher(String.valueOf(juzSpinnerItem.getJuzNumber())).find()
            || pattern.matcher(juzSpinnerItem.getLabel()).find();
    }

    @Override
    protected void setSpinnerTextInternal(TextView textView, ReaderSpinnerItem item) {
        super.setSpinnerTextInternal(textView, item);
        if (mJuzIconView != null) {
            mJuzIconView.setText(((JuzSpinnerItem) item).getNameArabic());
        }
    }

    @Override
    protected String getPopupTitle() {
        return getContext().getString(R.string.strTitleReaderJuz);
    }

    @Override
    protected String getPopupSearchHint() {
        return getContext().getString(R.string.strHintSearchJuz);
    }


    public void setJuzIconView(TextView juzIconView) {
        mJuzIconView = juzIconView;
    }

    public void setOnItemSelectListener(OnItemSelectedListener listener) {
        if (listener != null) {
            super.setOnItemSelectListener(item -> listener.onItemSelect((JuzSpinnerItem) item));
        }
    }

    public interface OnItemSelectedListener {
        void onItemSelect(JuzSpinnerItem item);
    }
}
