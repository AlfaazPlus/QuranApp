/*
 * Copyright (c) 11/8/2021. Faisal Khan.
 */

package com.peacedesign.android.utils.anim

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import java.lang.ref.WeakReference

class DimensionAnimator private constructor(
    requestedDimen: Int,
    v: View,
    startWidth: Int,
    targetWidth: Int,
    startHeight: Int,
    targetHeight: Int
) {
    companion object {
        private const val DIMEN_BOTH = 0x0
        private const val DIMEN_HEIGHT = 0x1
        private const val DIMEN_WIDTH = 0x2
        private const val DURATION_DEFAULT: Long = 200

        fun ofBoth(v: View, startWidth: Int, targetWidth: Int, startHeight: Int, targetHeight: Int): DimensionAnimator {
            return DimensionAnimator(
                DIMEN_BOTH,
                v,
                startWidth,
                targetWidth,
                startHeight,
                targetHeight
            )
        }

        fun ofWidth(v: View, startWidth: Int, targetWidth: Int): DimensionAnimator {
            return DimensionAnimator(DIMEN_WIDTH, v, startWidth, targetWidth, -1, -1)
        }

        fun ofHeight(v: View, startHeight: Int, targetHeight: Int): DimensionAnimator {
            return DimensionAnimator(DIMEN_HEIGHT, v, -1, -1, startHeight, targetHeight)
        }
    }

    private var mRequestedDimen: Int = requestedDimen

    private var mView: WeakReference<View> = WeakReference(v)
    private var mStartWidth = startWidth
    private var mTargetWidth = targetWidth
    private var mStartHeight = startHeight
    private var mTargetHeight = targetHeight
    private var mDurationWidth: Long = DURATION_DEFAULT
    private var mDurationHeight: Long = DURATION_DEFAULT
    private var mAnimator: Animator? = null
    private var mWidthListener: AnimatorListener? = null
    private var mHeightListener: AnimatorListener? = null
    private var mWidthInterpolator: TimeInterpolator? = null
    private var mHeightInterpolator: TimeInterpolator? = null

    fun setDuration(duration: Long) {
        setDuration(duration, duration)
    }

    fun setDuration(widthDuration: Long, heightDuration: Long) {
        mDurationWidth = widthDuration
        mDurationHeight = heightDuration
    }

    fun setAnimatorListener(listener: AnimatorListener?) {
        setAnimatorListener(listener, listener)
    }

    fun setAnimatorListener(widthListener: AnimatorListener?, heightListener: AnimatorListener?) {
        mWidthListener = widthListener
        mHeightListener = heightListener
    }

    fun setInterpolator(interpolator: TimeInterpolator?) {
        setInterpolator(interpolator, interpolator)
    }

    fun setInterpolator(widthInterpolator: TimeInterpolator?, heightInterpolator: TimeInterpolator?) {
        mWidthInterpolator = widthInterpolator
        mHeightInterpolator = heightInterpolator
    }

    fun prepare(): Animator {
        return if (mRequestedDimen == DIMEN_BOTH) {
            prepareBothInternal()
        } else {
            prepareSingleInternal(mRequestedDimen == DIMEN_HEIGHT)
        }
    }

    private fun prepareBothInternal(): Animator {
        val animatorW = ValueAnimator.ofInt(mStartWidth, mTargetWidth)
        animatorW.duration = mDurationWidth
        if (mWidthInterpolator != null) {
            animatorW.interpolator = mWidthInterpolator
        }
        if (mWidthListener != null) {
            animatorW.addListener(mWidthListener)
        }
        animatorW.addUpdateListener { animation: ValueAnimator ->
            mView.get()!!.layoutParams.width = animation.animatedValue as Int
            mView.get()!!.requestLayout()
        }
        val animatorH = ValueAnimator.ofInt(mStartHeight, mTargetHeight)
        animatorH.duration = mDurationHeight
        if (mHeightInterpolator != null) {
            animatorH.interpolator = mHeightInterpolator
        }
        if (mHeightListener != null) {
            animatorH.addListener(mHeightListener)
        }
        animatorH.addUpdateListener { animation: ValueAnimator ->
            mView.get()!!.layoutParams.height = animation.animatedValue as Int
            mView.get()!!.requestLayout()
        }
        val set = AnimatorSet()
        set.play(animatorW).with(animatorH)
        return set
    }

    private fun prepareSingleInternal(isHeight: Boolean): Animator {
        val listener = if (isHeight) mHeightListener else mWidthListener
        val interpolator = if (isHeight) mHeightInterpolator else mWidthInterpolator
        val duration = if (isHeight) mDurationHeight else mDurationWidth

        val animator = ValueAnimator.ofInt(
            if (isHeight) mStartHeight else mStartWidth,
            if (isHeight) mTargetHeight else mTargetWidth
        )
        animator.duration = duration

        if (interpolator != null) {
            animator.interpolator = interpolator
        }

        if (listener != null) {
            animator.addListener(listener)
        }

        animator.addUpdateListener { animation: ValueAnimator ->
            val nDimen = animation.animatedValue as Int
            val params = mView.get()!!.layoutParams
            if (isHeight) {
                params.height = nDimen
            } else {
                params.width = nDimen
            }
            mView.get()!!.requestLayout()
        }
        mAnimator = animator
        return animator
    }

    fun start() {
        if (mAnimator == null) {
            prepare()
        }
        mAnimator!!.start()
    }
}
