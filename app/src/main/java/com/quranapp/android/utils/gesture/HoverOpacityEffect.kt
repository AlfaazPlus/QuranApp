package com.quranapp.android.utils.gesture

import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

class HoverOpacityEffect(
    private val fromOpacity: Float = 1f,
    private val toOpacity: Float = 0.7f
) : OnTouchListener {
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> down(v)
            MotionEvent.ACTION_UP -> {
                up(v)
                val x = e.x
                val y = e.y
                if (x >= 0 && y >= 0 && x <= v.width && y <= v.height) v.performClick()
            }
            MotionEvent.ACTION_CANCEL -> up(v)
        }
        return true
    }

    private fun down(v: View) {
        v.alpha = toOpacity
    }

    private fun up(v: View) {
        v.alpha = fromOpacity
    }
}
