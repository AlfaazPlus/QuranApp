package com.peacedesign.android.utils.touchutils;

import static com.peacedesign.android.utils.touchutils.HoverPushEffect.Pressure.MEDIUM;

import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;

public class HoverPushOpacityEffect extends HoverPushEffect {
    private final float opacity = 0.6f;

    public HoverPushOpacityEffect() {
        this(MEDIUM);
    }

    public HoverPushOpacityEffect(@NonNull Pressure pressure) {
        super(pressure);
    }

    @Override
    protected void down(@NonNull View v) {
        super.down(v);
        animate(v, 1f, opacity);
    }

    @Override
    protected void up(@NonNull View v) {
        super.up(v);
        animate(v, opacity, 1f);
    }

    private void animate(View v, float from, float to) {
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(100);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            v.setAlpha(value);
        });
        animator.start();
    }
}