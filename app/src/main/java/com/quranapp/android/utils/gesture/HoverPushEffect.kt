package com.quranapp.android.utils.gesture

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View

open class HoverPushEffect(pressure: Pressure = Pressure.MEDIUM) : View.OnTouchListener {
    private var hoverPressure = 0f
    private var downScale = 0f

    init {
        when (pressure) {
            Pressure.LOW -> this.hoverPressure = 0.98f
            Pressure.MEDIUM -> this.hoverPressure = 0.95f
            Pressure.HIGH -> this.hoverPressure = 0.90f
        }
    }

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val superResult = v.onTouchEvent(e)
        when (e.action) {
            MotionEvent.ACTION_DOWN -> down(v)
            MotionEvent.ACTION_UP -> {
                up(v)
                val x = e.x
                val y = e.y
                if (!superResult && (x >= 0 && y >= 0 && x <= v.width && y <= v.height)) v.performClick()
            }
            MotionEvent.ACTION_CANCEL -> up(v)
            else -> return superResult
        }
        return true
    }

    protected open fun down(v: View) {
        downScale = v.scaleX * hoverPressure
        animate(v, 1f, downScale)
    }

    protected open fun up(v: View) {
        animate(v, downScale, 1f)
    }

    private fun animate(v: View, from: Float, to: Float) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = 100
            addUpdateListener { animation: ValueAnimator ->
                val value = animation.animatedValue as Float
                v.scaleX = value
                v.scaleY = value
            }
        }.start()
    }

    enum class Pressure {
        LOW, MEDIUM, HIGH
    }
}
