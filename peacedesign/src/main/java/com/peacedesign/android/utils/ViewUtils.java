package com.peacedesign.android.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.touchutils.HoverOpacityEffect;
import com.peacedesign.android.utils.touchutils.HoverPushOpacityEffect;

public class ViewUtils {
    public static void setPaddings(@NonNull View view, int padding) {
        view.setPadding(padding, padding, padding, padding);
    }

    public static void setPaddings(@NonNull View view, int horizontalPadding, int verticalPadding) {
        setPaddingHorizontal(view, horizontalPadding);
        setPaddingVertical(view, verticalPadding);
    }

    public static void setPaddingHorizontal(@NonNull View view, int padding) {
        view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
    }

    public static void setPaddingHorizontal(@NonNull View view, int paddingStart, int paddingEnd) {
        view.setPadding(paddingStart, view.getPaddingTop(), paddingEnd, view.getPaddingBottom());
    }

    public static void setPaddingVertical(@NonNull View view, int padding) {
        view.setPadding(ViewCompat.getPaddingStart(view), padding, ViewCompat.getPaddingEnd(view), padding);
    }

    public static void setPaddingVertical(@NonNull View view, int paddingTop, int paddingBottom) {
        view.setPadding(ViewCompat.getPaddingStart(view), paddingTop, ViewCompat.getPaddingEnd(view), paddingBottom);
    }

    public static void setPaddingStart(@NonNull View view, int padding) {
        view.setPadding(padding, view.getPaddingTop(), ViewCompat.getPaddingEnd(view), view.getPaddingBottom());
    }

    public static void setPaddingTop(@NonNull View view, int padding) {
        view.setPadding(ViewCompat.getPaddingStart(view), padding, ViewCompat.getPaddingEnd(view), view.getPaddingBottom());
    }

    public static void setPaddingEnd(@NonNull View view, int padding) {
        view.setPadding(ViewCompat.getPaddingStart(view), view.getPaddingTop(), padding, view.getPaddingBottom());
    }

    public static void setPaddingBottom(@NonNull View view, int padding) {
        view.setPadding(ViewCompat.getPaddingStart(view), view.getPaddingTop(), ViewCompat.getPaddingEnd(view), padding);
    }

    public static void setMargins(@NonNull ViewGroup.MarginLayoutParams lp, int margin) {
        lp.setMargins(margin, margin, margin, margin);
    }

    public static void setMargins(@NonNull ViewGroup.MarginLayoutParams lp, int marginH, int marginV) {
        lp.setMargins(marginH, marginV, marginH, marginV);
    }

    public static void setMarginHorizontal(@NonNull ViewGroup.MarginLayoutParams lp, int margin) {
        MarginLayoutParamsCompat.setMarginStart(lp, margin);
        MarginLayoutParamsCompat.setMarginEnd(lp, margin);
    }

    public static void setMarginVertical(@NonNull ViewGroup.MarginLayoutParams lp, int margin) {
        lp.topMargin = margin;
        lp.bottomMargin = margin;
    }

    public static void alphaDisableView(View view, boolean disable) {
        view.setAlpha(disable ? 0.5f : 1f);
    }

    public static void disableView(View view, boolean disable) {
        view.setEnabled(!disable);
        alphaDisableView(view, disable);
    }

    public static void removeView(View view) {
        if (view == null) {
            return;
        }

        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }

    public static int getRelativeTop(View view, Class<?> till, boolean inclusiveTill) {
        if (view == null) {
            return 0;
        }

        if (till.getName().equalsIgnoreCase(view.getClass().getName())) {
            if (inclusiveTill) {
                return view.getTop();
            }
            return 0;
        } else {
            int top = view.getTop();
            ViewParent parent = view.getParent();
            if (!(parent instanceof View) || parent == view.getRootView()) {
                return top;
            }
            return top + getRelativeTop((View) parent, till, inclusiveTill);
        }
    }

    public static LinearSnapHelper enableSnappingOnRecyclerView(RecyclerView recyclerView) {
        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        return snapHelper;
    }

    public static void addHoverPushOpacityEffect(View... buttons) {
        for (View button : buttons) {
            button.setOnTouchListener(new HoverPushOpacityEffect());
        }
    }

    public static void addHoverOpacityEffect(View... buttons) {
        for (View button : buttons) {
            button.setOnTouchListener(new HoverOpacityEffect());
        }
    }

    public static void centerInHorizontalScrollView(HorizontalScrollView scrollView, View child) {
        int center = scrollView.getScrollX() + scrollView.getWidth() / 2;
        int left = child.getLeft();
        int childWidth = child.getWidth();
        if (center >= left && center <= left + childWidth) {
            scrollView.scrollBy((left + childWidth / 2) - center, 0);
        }
    }

    public static void addStrokedBGToHeader(View headerView, @ColorRes int bgColorRes, @ColorRes int borderColorRes) {
        Context ctx = headerView.getContext();
        int bgColor = ContextCompat.getColor(ctx, bgColorRes);
        int borderColor = ContextCompat.getColor(ctx, borderColorRes);

        int[] strokeWidths = Dimen.createBorderWidthsForBG(0, 0, 0, Dimen.dp2px(ctx, 1));
        Drawable bg = DrawableUtils.createBackgroundStroked(bgColor, borderColor, strokeWidths, null);
        headerView.setBackground(bg);
    }

    public static void setTextSizePx(TextView textView, @DimenRes int dimenResId) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDimenPx(textView.getContext(), dimenResId));
    }

    public static void setTextColor(TextView textView, @ColorRes int colorResId) {
        textView.setTextColor(ContextCompat.getColor(textView.getContext(), colorResId));
    }

    public static void clipChildren(View v, boolean clip) {
        if (v.getParent() == null) {
            return;
        }

        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(clip);
            v.setClipToOutline(clip);
        }

        if (v.getParent() instanceof View) {
            clipChildren((View) v.getParent(), clip);
        }
    }

    public static void setBounceOverScrollRV(RecyclerView rv) {
        //        rv.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        //        rv.setEdgeEffectFactory(new BounceEdgeEffectFactory());
    }
}
