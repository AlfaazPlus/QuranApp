/*
 * (c) Faisal Khan. Created on 20/9/2021.
 */

package com.quranapp.android.frags.editshare;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.editor.VerseEditor;
import com.quranapp.android.frags.BaseFragment;

public class FragEditorBase extends BaseFragment {
    protected VerseEditor mVerseEditor;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = view.getContext();

        view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorBGPage));
        ViewUtils.setPaddingTop(view, Dimen.dp2px(context, 10));
        ViewUtils.setPaddingBottom(view, ResUtils.getDimenPx(context, R.dimen.dmnPadBig));
    }

    public void setEditor(VerseEditor verseEditor) {
        mVerseEditor = verseEditor;
    }
}
