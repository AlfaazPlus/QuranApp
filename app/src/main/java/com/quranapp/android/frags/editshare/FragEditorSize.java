/*
 * (c) Faisal Khan. Created on 20/9/2021.
 */

package com.quranapp.android.frags.editshare;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.databinding.LytSeekbarBinding;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;

public class FragEditorSize extends FragEditorBase {
    private LytSeekbarBinding mArTxtSizeSlider;
    private LytSeekbarBinding mTranslTxtSizeSlider;
    private boolean mDisableArSeekbar;
    private boolean mDisableTranslSeekbar;

    public static FragEditorSize newInstance() {
        return new FragEditorSize();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        NestedScrollView scrollView = new NestedScrollView(inflater.getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        LinearLayout layout = new LinearLayout(inflater.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(layout, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return scrollView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init((LinearLayout) ((ViewGroup) view).getChildAt(0));
    }

    private void init(LinearLayout layout) {
        mArTxtSizeSlider = makeSlider(layout, R.string.strTitleReaderTextSizeArabic, progress -> {
            if (mVerseEditor != null) {
                progress = Math.max(progress, 40);
                mVerseEditor.getListener().onArSizeChange((float) progress / 75);
            }
        });
        mTranslTxtSizeSlider = makeSlider(layout, R.string.strTitleReaderTextSizeTransl, progress -> {
            if (mVerseEditor != null) {
                progress = Math.max(progress, 40);
                mVerseEditor.getListener().onTranslSizeChange((float) progress / 75);
            }
        });

        mArTxtSizeSlider.seekbar.setProgress(50);
        mTranslTxtSizeSlider.seekbar.setProgress(65);

        disableArSeekbar(mDisableArSeekbar);
        disableTranslSeekbar(mDisableTranslSeekbar);
    }

    private LytSeekbarBinding makeSlider(LinearLayout layout, int txtRes, OnResultReadyCallback<Integer> callback) {
        Context ctx = layout.getContext();
        LytSeekbarBinding binding = LytSeekbarBinding.inflate(LayoutInflater.from(ctx), layout, false);
        binding.title.setText(txtRes);
        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setProgressText(progress, binding);
                callback.onReady(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        layout.addView(binding.getRoot());
        return binding;
    }

    private void setProgressText(int progress, LytSeekbarBinding binding) {
        String progressText = progress + "%";
        binding.progressText.setText(progressText);
    }

    public void disableArSeekbar(boolean disable) {
        mDisableArSeekbar = disable;

        if (mArTxtSizeSlider != null) {
            ViewUtils.disableView(mArTxtSizeSlider.seekbar, disable);
        }
    }

    public void disableTranslSeekbar(boolean disable) {
        mDisableTranslSeekbar = disable;
        if (mTranslTxtSizeSlider != null) {
            ViewUtils.disableView(mTranslTxtSizeSlider.seekbar, disable);
        }
    }
}
