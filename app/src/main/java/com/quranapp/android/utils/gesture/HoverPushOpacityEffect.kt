package com.quranapp.android.utils.gesture

import android.animation.ValueAnimator
import android.view.View

class HoverPushOpacityEffect(pressure: Pressure = Pressure.MEDIUM) : HoverPushEffect(pressure) {
    private val opacity = 0.6f

    override fun down(v: View) {
        super.down(v)
        animate(v, 1f, opacity)
    }

    override fun up(v: View) {
        super.up(v)
        animate(v, opacity, 1f)
    }

    private fun animate(v: View, from: Float, to: Float) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = 100
            addUpdateListener { animation: ValueAnimator ->
                val value = animation.animatedValue as Float
                v.alpha = value
            }
        }.start()
    }
}
