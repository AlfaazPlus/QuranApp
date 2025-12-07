package com.quranapp.android.views.reader

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.withTranslation

class VerseTextView : AppCompatTextView {
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        setSingleLine(false)
        setIncludeFontPadding(true)
    }

    override fun onDraw(canvas: Canvas) {
        val layout = getLayout()

        if (layout != null) {
            canvas.withTranslation(paddingLeft.toFloat(), paddingTop.toFloat()) {
                layout.draw(this)
            }
        } else {
            super.onDraw(canvas)
        }

    }
}