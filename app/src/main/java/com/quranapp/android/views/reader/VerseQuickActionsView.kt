package com.quranapp.android.views.reader

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.updateMargins

class VerseQuickActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.lyt_reader_verse_quick_actions, this)
        setBackgroundResource(R.drawable.bg_verse_quick_actions)
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        if (params is MarginLayoutParams) {
            params.updateMargins(context.dp2px(10F))
        }

        super.setLayoutParams(params)
    }
}