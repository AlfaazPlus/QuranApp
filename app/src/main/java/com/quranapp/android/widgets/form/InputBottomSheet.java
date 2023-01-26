/*
 * (c) Faisal Khan. Created on 29/1/2022.
 */

package com.quranapp.android.widgets.form;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.sheet.PeaceBottomSheet;
import com.peacedesign.android.widget.sheet.PeaceBottomSheetDialog;
import com.quranapp.android.R;
import com.quranapp.android.databinding.LytBottomSheetActionBtn1Binding;
import com.peacedesign.android.utils.Log;

public class InputBottomSheet extends PeaceBottomSheet {
    private CharSequence mMsg;
    private CharSequence mInputHint;
    private int mInputType;
    private CharSequence mInputText;
    private CharSequence mBtnText;
    private CharSequence mInputWarning;
    private TextView mMessageView;
    private PeaceFormInputField mInputView;
    private InputBottomSheetBtnActionListener mBtnActionListener;
    private AsyncLayoutInflater mAsyncInflater;
    private LytBottomSheetActionBtn1Binding mActionBinding;
    private boolean mStateSaveEnabled;

    public InputBottomSheet() {
        disableDragging(true);
        disableOutsideTouch(true);
        setContentView(R.layout.lyt_authenticate);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("mStateSaveEnabled", mStateSaveEnabled);
        if (mStateSaveEnabled) {
            outState.putCharSequence("mMsg", mMsg);
            outState.putCharSequence("mInputHint", mInputHint);
            outState.putInt("mInputType", mInputType);
            outState.putCharSequence("mInputText", mInputText);
            outState.putCharSequence("mBtnText", mBtnText);
            outState.putCharSequence("mInputWarning", mInputWarning);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mStateSaveEnabled = savedInstanceState.getBoolean("mStateSaveEnabled");
            if (mStateSaveEnabled) {
                mMsg = savedInstanceState.getCharSequence("mMsg");
                mInputHint = savedInstanceState.getCharSequence("mInputHint");
                mInputType = savedInstanceState.getInt("mInputType");
                mInputText = savedInstanceState.getCharSequence("mInputText");
                mBtnText = savedInstanceState.getCharSequence("mBtnText");
                mInputWarning = savedInstanceState.getCharSequence("mInputWarning");
            }
        }

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && !mStateSaveEnabled) {
            try {
                dismiss();
            } catch (Exception ignored) {}
        }
    }

    @NonNull
    @Override
    public PeaceBottomSheetDialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        PeaceBottomSheetDialog dialog = super.onCreateDialog(savedInstanceState);
        prepareActionButton(dialog);
        return dialog;
    }

    private void prepareActionButton(PeaceBottomSheetDialog dialog) {
        final View decorView = dialog.getWindow().getDecorView();
        decorView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                ViewGroup coordinator = dialog.findViewById(com.google.android.material.R.id.coordinator);
                ViewGroup containerLayout = dialog.findViewById(com.google.android.material.R.id.container);
                if (coordinator == null || containerLayout == null) {
                    return;
                }

                getAsyncInflater().inflate(R.layout.lyt_bottom_sheet_action_btn_1, containerLayout, (view, resid, parent) -> {
                    mActionBinding = LytBottomSheetActionBtn1Binding.bind(view);
                    setupActionButton(mActionBinding, coordinator, containerLayout);
                });

                decorView.removeOnLayoutChangeListener(this);
            }
        });
    }

    private void setupActionButton(LytBottomSheetActionBtn1Binding binding, View coordinator, ViewGroup containerLayout) {
        binding.btn.setText(mBtnText);
        binding.getRoot().setBackgroundColor(getSheetBGColor());
        binding.btn.setOnClickListener(v -> {
            setInputWarning(null);
            if (mBtnActionListener != null) {
                mBtnActionListener.onButtonAction(this, mInputView.getText());
            }
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        containerLayout.addView(mActionBinding.getRoot(), lp);

        binding.getRoot().post(() -> {
            binding.getRoot().measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );

            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) coordinator.getLayoutParams();
            lp1.bottomMargin = binding.getRoot().getMeasuredHeight();
            coordinator.requestLayout();
        });
    }

    @Override
    protected void setupContentView(@NonNull LinearLayout dialogLayout, PeaceBottomSheetParams params) {
        getAsyncInflater().inflate(R.layout.lyt_authenticate, dialogLayout, (view, resid, parent) -> {
            dialogLayout.addView(view);

            setupView(view);
        });
    }

    private void setupView(View view) {
        mMessageView = view.findViewById(R.id.msg);
        mInputView = view.findViewById(R.id.input);

        setMessageInternal(mMsg);
        setInputHintInternal(mInputHint);
        setInputTypeInternal(mInputType);
        setInputTextInternal(mInputText);
        setButtonTextInternal(mBtnText);
        setInputWarningInternal(mInputWarning);
    }

    private void setMessageInternal(CharSequence msg) {
        if (mMessageView != null) {
            mMessageView.setText(msg);
            mMessageView.setVisibility(TextUtils.isEmpty(msg) ? GONE : VISIBLE);
        }
    }

    private void setButtonTextInternal(CharSequence text) {
        if (mActionBinding != null) {
            mActionBinding.btn.setText(text);
            mActionBinding.getRoot().setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
        }
    }

    private void setInputHintInternal(CharSequence hint) {
        if (mInputView != null) {
            mInputView.setFieldHint(hint);
        }
    }

    private void setInputTypeInternal(int inputType) {
        if (mInputView != null) {
            mInputView.setFieldInputType(inputType);
        }
    }

    private void setInputTextInternal(CharSequence inputText) {
        if (mInputView != null) {
            mInputView.setFieldText(inputText);
        }
    }

    private void setInputWarningInternal(CharSequence warning) {
        if (mInputView != null) {
            mInputView.setFieldWarning(warning);
        }
    }

    public void setMessage(CharSequence msg) {
        mMsg = msg;
        setMessageInternal(msg);
    }

    public void setInputHint(CharSequence hint) {
        mInputHint = hint;
        setInputHintInternal(hint);
    }

    public void setInputType(int inputType) {
        mInputType = inputType;
        setInputTypeInternal(inputType);
    }

    public void setInputText(CharSequence inputText) {
        mInputText = inputText;
        setInputTextInternal(inputText);
    }

    public void setInputWarning(CharSequence warning) {
        mInputWarning = warning;
        setInputWarningInternal(warning);
    }

    public void setButton(CharSequence btnText, InputBottomSheetBtnActionListener actionListener) {
        mBtnText = btnText;
        mBtnActionListener = actionListener;

        setButtonTextInternal(btnText);
    }

    public void setActionListener(InputBottomSheetBtnActionListener actionListener) {
        mBtnActionListener = actionListener;
    }

    public void setButtonText(CharSequence btnText) {
        mBtnText = btnText;
        setButtonTextInternal(btnText);
    }

    public void loader(boolean shown) {
        if (mActionBinding != null) {
            ViewUtils.disableView(mActionBinding.btn, shown);
        }

        if (mInputView != null) {
            ViewUtils.disableView(mInputView, shown);
        }
        if (mMessageView != null) {
            ViewUtils.disableView(mMessageView, shown);
        }

        disableBackKey(shown);
    }

    @Override
    protected boolean onKey(@NonNull BottomSheetDialog dialog, int keyCode, @NonNull KeyEvent event) {
        return getDialogParams().disableBackKey;
    }

    private AsyncLayoutInflater getAsyncInflater() {
        if (mAsyncInflater == null) {
            mAsyncInflater = new AsyncLayoutInflater(getContext());
        }
        return mAsyncInflater;
    }

    public PeaceFormInputField getInputField() {
        return mInputView;
    }

    public void setStateSaveEnabled(boolean stateSaveEnabled) {
        mStateSaveEnabled = stateSaveEnabled;
    }

    public interface InputBottomSheetBtnActionListener {
        void onButtonAction(InputBottomSheet dialog, CharSequence inputText);
    }
}
