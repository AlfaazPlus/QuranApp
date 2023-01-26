/*
 * (c) Faisal Khan. Created on 5/2/2022.
 */

package com.quranapp.android.interfaceUtils.editor;

import com.quranapp.android.components.editor.EditorBG;
import com.quranapp.android.components.editor.EditorFG;
import com.quranapp.android.components.quran.subcomponents.TranslationBook;

public interface OnEditorChangeListener {
    void onBGChange(EditorBG nBg);

    void onBGAlphaColorChange(int alphaColor);

    void onFGChange(EditorFG nFg);

    void onArSizeChange(float mult);

    void onTranslSizeChange(float mult);

    void onOptionChange(boolean showAr, boolean showTrans, boolean showRef);

    void onTranslChange(TranslationBook book);
}