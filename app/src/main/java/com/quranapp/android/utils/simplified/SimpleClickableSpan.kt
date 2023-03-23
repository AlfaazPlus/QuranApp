package com.quranapp.android.utils.simplified

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class SimpleClickableSpan @JvmOverloads constructor(
    private val txtColor: Int?,
    private val isUnderline: Boolean = false,
    private val callback: Runnable
) : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)

        ds.isUnderlineText = isUnderline
        if (txtColor != null) ds.color = txtColor
    }

    override fun onClick(widget: View) {
        callback.run()
    }
}
