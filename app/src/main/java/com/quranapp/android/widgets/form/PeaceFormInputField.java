/*
 * (c) Faisal Khan. Created on 27/1/2022.
 */

package com.quranapp.android.widgets.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.quranapp.android.R;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;

public class PeaceFormInputField extends FrameLayout {
    private CharSequence mFieldTitle;
    private CharSequence mFieldHint;
    private CharSequence mFieldText;
    private CharSequence mFieldWarningMsg;
    private boolean mFieldRequired;
    private final int mImeOptions;
    private int mInputType;
    private TextView mFieldTitleView;
    private EditText mFieldInputView;
    private TextView mFieldWarningView;

    public PeaceFormInputField(@NonNull Context context) {
        this(context, null);
    }

    public PeaceFormInputField(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeaceFormInputField(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PeaceFormInputField(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PeaceFormInputField, defStyleAttr,
            defStyleRes);
        mFieldTitle = a.getString(R.styleable.PeaceFormInputField_PeaceFFTitle);
        mFieldHint = a.getString(R.styleable.PeaceFormInputField_PeaceFFHint);
        mFieldText = a.getString(R.styleable.PeaceFormInputField_PeaceFFText);
        mFieldRequired = a.getBoolean(R.styleable.PeaceFormInputField_PeaceFFRequired, false);
        mImeOptions = a.getInt(R.styleable.PeaceFormInputField_android_imeOptions, EditorInfo.IME_ACTION_DONE);
        mInputType = a.getInt(R.styleable.PeaceFormInputField_android_inputType, InputType.TYPE_CLASS_TEXT);

        init(context);
        a.recycle();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("superState", super.onSaveInstanceState());
        state.putCharSequence("value", mFieldInputView.getText());
        state.putCharSequence("warning", mFieldWarningMsg);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle fieldState = (Bundle) state;

            mFieldText = fieldState.getCharSequence("value");
            mFieldWarningMsg = fieldState.getCharSequence("warning");

            setFieldText(mFieldText);
            setFieldWarning(mFieldWarningMsg);
            state = fieldState.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    private void init(Context context) {
        View layout = LayoutInflater.from(context).inflate(R.layout.lyt_form_input_field, this, false);
        mFieldTitleView = layout.findViewById(R.id.fieldTitle);
        mFieldInputView = layout.findViewById(R.id.fieldInput);
        mFieldWarningView = layout.findViewById(R.id.fieldMsg);
        mFieldInputView.setSaveEnabled(true);
        mFieldWarningView.setSaveEnabled(true);
        mFieldInputView.setId(View.generateViewId());
        mFieldWarningView.setId(View.generateViewId());
        addView(layout);

        setFieldTitle(mFieldTitle);
        setFieldHint(mFieldHint);
        setFieldText(mFieldText);

        mFieldInputView.setImeOptions(mImeOptions);
        mFieldInputView.setInputType(mInputType);
        mFieldInputView.setTypeface(Typeface.DEFAULT_BOLD);
        mFieldInputView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (isWarningShown()) {
                    setFieldWarning(null);
                }
            }
        });
    }

    public void setFieldTitle(CharSequence fieldTitle) {
        mFieldTitle = fieldTitle;
        mFieldTitleView.setText(fieldTitle);

        mFieldTitleView.setVisibility(TextUtils.isEmpty(fieldTitle) ? GONE : VISIBLE);
    }

    public void setFieldTitle(@StringRes int fieldTitleRes) {
        setFieldTitle(getContext().getString(fieldTitleRes));
    }

    public void setFieldHint(CharSequence fieldHint) {
        mFieldHint = fieldHint;
        mFieldInputView.setHint(fieldHint);
    }

    public void setFieldText(CharSequence fieldText) {
        mFieldText = fieldText;
        mFieldInputView.setText(fieldText);
    }

    public void setFieldInputType(int inputType) {
        mInputType = inputType;
        mFieldInputView.setInputType(inputType);
        mFieldInputView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setFieldWarning(CharSequence warningMsg) {
        mFieldWarningMsg = warningMsg;
        mFieldWarningView.setText(warningMsg);

        mFieldWarningView.setVisibility(TextUtils.isEmpty(warningMsg) ? GONE : VISIBLE);
    }

    public void setFieldWarning(@StringRes int warningMsgRes) {
        setFieldWarning(getContext().getString(warningMsgRes));
    }

    public void setFieldRequired(boolean required) {
        mFieldRequired = required;
    }

    public CharSequence getFieldTitle() {
        return mFieldTitle;
    }

    public EditText getFieldInputView() {
        return mFieldInputView;
    }

    public CharSequence getText() {
        return mFieldInputView.getText();
    }

    public boolean isWarningShown() {
        return mFieldWarningView.getVisibility() == VISIBLE;
    }

    public void setFocus() {
        mFieldInputView.requestFocus();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mFieldInputView.setEnabled(enabled);
    }
}
