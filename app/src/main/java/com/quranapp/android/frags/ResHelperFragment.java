/*
 * (c) Faisal Khan. Created on 4/2/2022.
 */

/*
 * (c) Faisal Khan. Created on 30/10/2021.
 */

package com.quranapp.android.frags;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.ArrayRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.FontRes;
import androidx.fragment.app.Fragment;

import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.ContextKt;

public class ResHelperFragment extends Fragment {
    private int mPrimaryClr = -213;

    public Configuration getConfiguration(Context ctx) {
        return ctx.getResources().getConfiguration();
    }

    @ColorInt
    public int primaryClr(Context ctx) {
        if (mPrimaryClr == -213) {
            mPrimaryClr = color(ctx, R.color.colorPrimary);
        }

        return mPrimaryClr;
    }

    public String[] strArray(Context ctx, @ArrayRes int arrayResId) {
        return ContextKt.getStringArray(ctx, arrayResId);
    }

    public int[] intArray(Context ctx, @ArrayRes int arrayResId) {
        return ContextKt.getIntArray(ctx, arrayResId);
    }

    @ColorInt
    public int color(Context ctx, @ColorRes int colorResId) {
        return ContextKt.color(ctx, colorResId);
    }

    public ColorStateList colorStateList(Context ctx, @ColorRes int colorResId) {
        return ContextKt.colorStateList(ctx, colorResId);
    }

    public Typeface font(Context ctx, @FontRes int fontResId) {
        return ContextKt.getFont(ctx, fontResId);
    }

    public Drawable drawable(Context ctx, @DrawableRes int drawableResId) {
        return ContextKt.drawable(ctx, drawableResId);
    }

    public int dimen(Context ctx, @DimenRes int dimenResId) {
        return ContextKt.getDimenPx(ctx, dimenResId);
    }

    public int dp2px(Context ctx, @Dimension(unit = Dimension.DP) float dpValue) {
        return ContextKt.dp2px(ctx, dpValue);
    }
}