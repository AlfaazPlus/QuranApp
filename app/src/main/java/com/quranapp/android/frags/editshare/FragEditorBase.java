/*
 * (c) Faisal Khan. Created on 20/9/2021.
 */

package com.quranapp.android.frags.editshare;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quranapp.android.R;
import com.quranapp.android.components.editor.VerseEditor;
import com.quranapp.android.frags.BaseFragment;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewPaddingKt;

public class FragEditorBase extends BaseFragment {
    protected VerseEditor mVerseEditor;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = view.getContext();

        view.setBackgroundColor(ContextKt.color(context, R.color.colorBGPage));
        ViewPaddingKt.updatePaddingVertical(view, ContextKt.dp2px(context, 10),
            ContextKt.getDimenPx(context, R.dimen.dmnPadBig));
    }

    public void setEditor(VerseEditor verseEditor) {
        mVerseEditor = verseEditor;
    }
}
