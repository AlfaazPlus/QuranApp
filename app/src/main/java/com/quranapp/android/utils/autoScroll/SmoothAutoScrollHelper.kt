package com.quranapp.android.utils.autoScroll

import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.interfaceUtils.Destroyable
import com.quranapp.android.utils.Logger

class SmoothAutoScrollHelper(
    private val delegate: SmoothAutoScrollHelperDelegate,
    private val recyclerView: RecyclerView,
    private var scrollSpeed: Int // 1 to 10
) : Destroyable {
    private val handler = Handler(Looper.getMainLooper())
    private var isScrolling = false


    private val scrollRunnable = object : Runnable {
        override fun run() {
            val duration = calculateDuration(scrollSpeed)
            val offset = calculateOffset(scrollSpeed)

            recyclerView.smoothScrollBy(0, offset, LinearInterpolator(), duration.toInt())

            // stop the auto scroll if the end of the list is reached
            if (!recyclerView.canScrollVertically(1)) {
                return
            }

            handler.postDelayed(this, duration)
        }
    }


    fun calculateOffset(value: Int): Int {
        val minOffset = 50
        val maxOffset = 300

        return minOffset + ((value - 1) * (maxOffset - minOffset) / 9)
    }

    fun calculateDuration(value: Int): Long {
        val fastestDurationMillis = 1000L
        val slowestDurationMillis = 10000L

        return slowestDurationMillis - (value * (slowestDurationMillis - fastestDurationMillis) / 10)
    }


    fun startAutoScroll() {
        isScrolling = true
        handler.post(scrollRunnable)
        delegate.onAutoScrollStarted()
    }

    fun stopAutoScroll(pause: Boolean = false) {
        if (!pause) {
            isScrolling = false
        }

        handler.removeCallbacks(scrollRunnable)
        recyclerView.stopScroll()
        delegate.onAutoScrollStopped()
    }

    fun pauseAutoScroll() {
        stopAutoScroll(true)
    }

    fun resumeAutoScroll() {
        if (!isScrolling) {
            return
        }

        startAutoScroll()
    }

    fun setScrollSpeed(speed: Int) {
        this.scrollSpeed = speed
    }

    override fun destroy() {
        stopAutoScroll()
    }
}
