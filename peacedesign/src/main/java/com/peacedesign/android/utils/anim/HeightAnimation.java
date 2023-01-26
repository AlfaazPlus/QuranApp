package com.peacedesign.android.utils.anim;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;

public class HeightAnimation extends Animation {
    private final View view;
    private int startHeight;
    private int deltaHeight; // distance between start and end height

    /**
     * constructor, do not forget to use the setParams(int, int) method before
     * starting the animation
     *
     * @param v The view to be animated
     */
    public HeightAnimation(@NonNull View v) {
        this.view = v;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, @NonNull Transformation t) {
        view.getLayoutParams().height = (int) (startHeight + deltaHeight * interpolatedTime);
        view.requestLayout();
    }

    /**
     * set the starting and ending height for the resize animation
     * starting height is usually the views current height, the end height is the height
     * we want to reach after the animation is completed
     *
     * @param startHeight height in pixels
     * @param endHeight   height in pixels
     */
    public void setParams(int startHeight, int endHeight) {
        this.startHeight = startHeight;
        deltaHeight = endHeight - this.startHeight;
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
