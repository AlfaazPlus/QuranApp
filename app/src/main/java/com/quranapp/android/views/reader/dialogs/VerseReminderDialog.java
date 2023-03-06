/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 9/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.views.reader.dialogs;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.LytBottomSheetActionBtn1Binding;
import com.quranapp.android.databinding.LytReaderVrdBinding;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetDialog;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class VerseReminderDialog extends PeaceBottomSheet {
    private int mChapterNo;
    private int mVerseNo;
    private LytReaderVrdBinding mBinding;
    private AsyncLayoutInflater mAsyncInflater;
    private LytBottomSheetActionBtn1Binding mActionBinding;

    public VerseReminderDialog() {
        PeaceBottomSheetParams P = getParams();
        P.setHeaderTitle("Verse Reminder");
        P.setInitialBehaviorState(BottomSheetBehavior.STATE_EXPANDED);
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
                Log.d("HERE");
                ViewGroup coordinator = dialog.findViewById(com.google.android.material.R.id.coordinator);
                ViewGroup containerLayout = dialog.findViewById(com.google.android.material.R.id.container);
                if (coordinator == null || containerLayout == null) {
                    return;
                }

                if (mActionBinding == null) {
                    getAsyncInflater().inflate(R.layout.lyt_bottom_sheet_action_btn_1, containerLayout,
                        (view, resid, parent) -> {
                            mActionBinding = LytBottomSheetActionBtn1Binding.bind(view);
                            setupActionButton(mActionBinding, coordinator, containerLayout);
                        });
                } else {
                    setupActionButton(mActionBinding, coordinator, containerLayout);
                }
                decorView.removeOnLayoutChangeListener(this);
            }
        });
    }

    private void setupActionButton(LytBottomSheetActionBtn1Binding binding, View coordinator, ViewGroup containerLayout) {
        binding.btn.setText("Set Reminder");
        binding.getRoot().setBackgroundColor(getParams().getSheetBGColor());

        ViewKt.disableView(binding.btn, true);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        ViewKt.removeView(binding.getRoot());
        containerLayout.addView(binding.getRoot(), lp);

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("chapterNo", mChapterNo);
        outState.putInt("verseNo", mVerseNo);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mChapterNo = savedInstanceState.getInt("chapterNo");
            mVerseNo = savedInstanceState.getInt("verseNo");
        }
        super.onCreate(savedInstanceState);
    }

    public void setVerse(int chapterNo, int verseNo) {
        mChapterNo = chapterNo;
        mVerseNo = verseNo;
    }

    @Override
    protected void setupContentView(@NonNull LinearLayout contentContainer, PeaceBottomSheetParams params) {
        QuranMeta.prepareInstance(contentContainer.getContext(), quranMeta -> {
            if (mBinding == null) {
                getAsyncInflater().inflate(R.layout.lyt_reader_vrd, contentContainer, (view, resid, parent) -> {
                    mBinding = LytReaderVrdBinding.bind(view);
                    contentContainer.addView(mBinding.getRoot());
                    setupContent(quranMeta, mBinding);
                });
            } else {
                ViewKt.removeView(mBinding.getRoot());
                contentContainer.addView(mBinding.getRoot());
                setupContent(quranMeta, mBinding);
            }
        });
    }

    private AsyncLayoutInflater getAsyncInflater() {
        if (mAsyncInflater == null) {
            mAsyncInflater = new AsyncLayoutInflater(getContext());
        }
        return mAsyncInflater;
    }

    private void setupContent(QuranMeta quranMeta, LytReaderVrdBinding binding) {
        Log.d(mChapterNo, mVerseNo);
        binding.message.setSaveEnabled(true);
        binding.duration.setSaveEnabled(true);

        binding.message.setText(prepareMessage(quranMeta));
        binding.timePicker.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        binding.duration.setVisibility(View.GONE);
        binding.timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            binding.duration.setVisibility(View.VISIBLE);
            binding.duration.setText(prepareDuration(hourOfDay, minute));

            if (mActionBinding != null) {
                ViewKt.disableView(mActionBinding.btn, false);
            }
        });

        if (mActionBinding != null) {
            mActionBinding.btn.setOnClickListener(v -> {});
            ViewKt.disableView(mActionBinding.btn, true);
        }
    }

    private String prepareDuration(int hourOfDay, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);

        long diff = cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diff);
        int hour = (int) (diffInSec / (60 * 60));
        int minRemaining = (int) (diffInSec % (60 * 60));
        int min = minRemaining / 60;

        return String.format("Reminds in %d hours and %d minutes.", hour, min);
    }

    private CharSequence prepareMessage(QuranMeta quranMeta) {
        Context ctx = getContext();

        String chapterName = quranMeta.getChapterName(ctx, mChapterNo, true);
        String reminderMsg = "Set reminder for:";
        SpannableString verseSS = new SpannableString(String.format("%s, Verse %d", chapterName, mVerseNo));

        AbsoluteSizeSpan txtSizeSpan = new AbsoluteSizeSpan(ContextKt.getDimenPx(ctx, R.dimen.dmnCommonSizeMedium));
        verseSS.setSpan(txtSizeSpan, 0, verseSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        verseSS.setSpan(new StyleSpan(Typeface.BOLD), 0, verseSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        verseSS.setSpan(new ForegroundColorSpan(ContextKt.obtainPrimaryColor(ctx)), 0, verseSS.length(),
            SPAN_EXCLUSIVE_EXCLUSIVE);

        return TextUtils.concat(reminderMsg, "\n", verseSS);
    }

    public int getChapterNo() {
        return mChapterNo;
    }

    public int getVerseNo() {
        return mVerseNo;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!isShowing()) {
            mBinding = null;
        }
    }
}
