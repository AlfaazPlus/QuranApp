package com.peacedesign.android.utils.touchutils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.annotation.NonNull;

public class HoverOpacityEffect implements OnTouchListener {
    private static final float DEFAULT_TARGET_OPACITY = 0.7f;
    private final float mFromOpacity;
    private final float mTargetOpacity;

    public HoverOpacityEffect() {
        this(DEFAULT_TARGET_OPACITY);
    }

    public HoverOpacityEffect(float targetOpacity) {
        this(1f, targetOpacity);
    }

    public HoverOpacityEffect(float fromOpacity, float targetOpacity) {
        mFromOpacity = fromOpacity;
        mTargetOpacity = targetOpacity;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(@NonNull View v, @NonNull MotionEvent e) {

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                down(v);
                break;
            case MotionEvent.ACTION_UP:
                up(v);
                float x = e.getX();
                float y = e.getY();
                if (x >= 0 && y >= 0 && x <= v.getWidth() && y <= v.getHeight()) v.performClick();
                break;
            case MotionEvent.ACTION_CANCEL: up(v); break;
        }
        return true;
    }

    private void down(View v) {
        v.setAlpha(mTargetOpacity);
    }

    private void up(View v) {
        v.setAlpha(mFromOpacity);
    }

    public void onLongPress() { }
}