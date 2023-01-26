package com.peacedesign.android.utils.touchutils;

import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.annotation.NonNull;

public class HoverPushEffect implements OnTouchListener {
    private final float pressure;
    private float downScale;

    public HoverPushEffect() {
        this(Pressure.MEDIUM);
    }

    public HoverPushEffect(@NonNull Pressure pressure) {
        this.pressure = pressure == Pressure.LOW ? 0.98f : pressure == Pressure.HIGH ? 0.90f : 0.95f;
    }

    @Override
    public boolean onTouch(@NonNull View v, @NonNull MotionEvent e) {
        boolean defaultResult = v.onTouchEvent(e);
        float x;
        float y;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                down(v);
                break;
            case MotionEvent.ACTION_UP:
                up(v);
                x = e.getX();
                y = e.getY();
                if (!defaultResult && (x >= 0 && y >= 0 && x <= v.getWidth() && y <= v.getHeight())) v.performClick();
                break;
            case MotionEvent.ACTION_CANCEL:
                up(v); break;
            default:
                return defaultResult;
        }
        return true;
    }

    protected void down(@NonNull View v) {
        downScale = v.getScaleX() * pressure;
        animate(v, 1f, downScale);
    }

    protected void up(@NonNull View v) {
        animate(v, downScale, 1f);
    }

    private void animate(View v, float from, float to) {
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(100);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            v.setScaleX(value);
            v.setScaleY(value);
        });
        animator.start();
    }

    public void onLongPress() { }

    public enum Pressure {
        LOW, MEDIUM, HIGH,
    }
}