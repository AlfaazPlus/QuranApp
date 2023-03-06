/*
 * (c) Faisal Khan. Created on 27/1/2022.
 */

package com.quranapp.android.widgets.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.quranapp.android.R;
import com.quranapp.android.adapters.utility.SpinnerAdapter2;
import com.quranapp.android.components.utility.SpinnerItem;
import com.quranapp.android.views.helper.Spinner2;
import com.quranapp.android.views.helper.Spinner2.SimplerSpinnerItemSelectListener;

import java.util.List;

public class PeaceFormSelectField extends FrameLayout {
    private CharSequence mFieldTitle;
    private CharSequence mFieldWarningMsg;
    private boolean mFieldRequired;
    private TextView mFieldTitleView;
    private Spinner2 mFieldSpinnerView;
    private TextView mFieldWarningView;

    public PeaceFormSelectField(@NonNull Context context) {
        this(context, null);
    }

    public PeaceFormSelectField(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeaceFormSelectField(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PeaceFormSelectField(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PeaceFormSelectField, defStyleAttr,
            defStyleRes);
        mFieldTitle = a.getString(R.styleable.PeaceFormSelectField_PeaceFFTitle);
        mFieldRequired = a.getBoolean(R.styleable.PeaceFormSelectField_PeaceFFRequired, false);

        init(context);
        a.recycle();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("superState", super.onSaveInstanceState());
        state.putInt("selectedPos", mFieldSpinnerView.getSelectedItemPosition());
        state.putCharSequence("warning", mFieldWarningMsg);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle fieldState = (Bundle) state;

            int selectedPos = fieldState.getInt("selectedPos");
            mFieldWarningMsg = fieldState.getCharSequence("warning");

            select(selectedPos);
            setFieldWarning(mFieldWarningMsg);
            state = fieldState.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    private void init(Context context) {
        View layout = LayoutInflater.from(context).inflate(R.layout.lyt_form_select_field, this, false);
        mFieldTitleView = layout.findViewById(R.id.fieldTitle);
        mFieldSpinnerView = layout.findViewById(R.id.fieldSpinner);
        mFieldWarningView = layout.findViewById(R.id.fieldMsg);
        mFieldSpinnerView.setSaveEnabled(true);
        mFieldWarningView.setSaveEnabled(true);
        mFieldSpinnerView.setId(View.generateViewId());
        mFieldWarningView.setId(View.generateViewId());
        addView(layout);

        setFieldTitle(mFieldTitle);
    }

    public void setFieldTitle(CharSequence fieldTitle) {
        mFieldTitle = fieldTitle;
        mFieldTitleView.setText(fieldTitle);
    }

    public void setFieldTitle(@StringRes int fieldTitleRes) {
        setFieldTitle(getContext().getString(fieldTitleRes));
    }

    public void setFieldWarning(CharSequence warningMsg) {
        mFieldWarningMsg = warningMsg;
        mFieldWarningView.setText(warningMsg);

        mFieldWarningView.setVisibility(TextUtils.isEmpty(warningMsg) ? GONE : VISIBLE);
    }

    public void setFieldRequired(boolean required) {
        mFieldRequired = required;
    }

    public CharSequence getFieldTitle() {
        return mFieldTitle;
    }

    public boolean isWarningShown() {
        return mFieldWarningView.getVisibility() == VISIBLE;
    }

    public void setOptions(List<SpinnerItem> options, SimplerSpinnerItemSelectListener selectListener) {
        mFieldSpinnerView.setOnItemSelectedListener(selectListener);
        SpinnerAdapter2<SpinnerItem> adapter = new SpinnerAdapter2<>(getContext(), R.layout.lyt_simple_spinner_item,
            R.id.text,
            options);
        mFieldSpinnerView.setAdapter(adapter);
    }

    public void select(int position) {
        mFieldSpinnerView.setSelection(position);
    }

    private static class FieldState extends BaseSavedState {
        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<FieldState> CREATOR = new Parcelable.Creator<FieldState>() {
            public FieldState createFromParcel(Parcel in) {
                return new FieldState(in);
            }

            public FieldState[] newArray(int size) {
                return new FieldState[size];
            }
        };

        int selectedPos;
        CharSequence warning;

        private FieldState(Parcelable superState) {
            super(superState);
        }

        private FieldState(Parcel in) {
            super(in);
            selectedPos = in.readInt();
            warning = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(selectedPos);
            TextUtils.writeToParcel(warning, out, flags);
        }
    }
}
