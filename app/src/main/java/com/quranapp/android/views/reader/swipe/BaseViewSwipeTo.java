/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 21/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.views.reader.swipe;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.animation.Animation.RELATIVE_TO_SELF;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewPaddingKt;

import me.dkzwm.widget.srl.SmoothRefreshLayout;
import me.dkzwm.widget.srl.extra.IRefreshView;
import me.dkzwm.widget.srl.indicator.IIndicator;

public abstract class BaseViewSwipeTo extends LinearLayout implements IRefreshView<IIndicator> {
    private static final Interpolator sLinearInterpolator = new LinearInterpolator();
    private final int mTitleTxtSize;
    private final int mChapterTxtSize;
    private final int mArrowBGClrNormal;
    private final int mArrowBGClrCompleted;
    private final int mArrowTintNormal;
    private final int mArrowTintCompleted;
    final ValueAnimator mJumpAnimation;
    protected final RotateAnimation mRotateAnimation;
    protected final RotateAnimation mReverseRotateAnimation;
    private final int mRotAnimDuration = 200;
    AppCompatImageView mArrow;
    protected TextView mTitleView;
    boolean mSwipeDisabled;

    public BaseViewSwipeTo(Context context) {
        super(context);
        mTitleTxtSize = ContextKt.getDimenPx(getContext(), R.dimen.dmnCommonSize3);
        mChapterTxtSize = ContextKt.getDimenPx(context, R.dimen.dmnCommonSize1_5);
        mArrowBGClrNormal = ContextKt.color(getContext(), R.color.colorBGPage2);
        mArrowBGClrCompleted = ContextKt.color(getContext(), R.color.colorPrimary);
        mArrowTintNormal = ContextKt.color(getContext(), R.color.colorIcon);
        mArrowTintCompleted = ContextKt.color(getContext(), R.color.white);

        init();
        createViews(context);

        mJumpAnimation = ValueAnimator.ofFloat(-10, 0, -10);
        mJumpAnimation.addUpdateListener(animation -> mArrow.setTranslationY((Float) animation.getAnimatedValue()));
        mJumpAnimation.setDuration(700);
        mJumpAnimation.setInterpolator(sLinearInterpolator);
        mJumpAnimation.setRepeatCount(ValueAnimator.INFINITE);

        float center = 0.5f;
        int relative = RELATIVE_TO_SELF;

        mRotateAnimation = new RotateAnimation(0, -getRotationBeforeReach(), relative, center, relative, center);
        mRotateAnimation.setInterpolator(sLinearInterpolator);
        mRotateAnimation.setDuration(mRotAnimDuration);
        mRotateAnimation.setFillAfter(true);

        mReverseRotateAnimation = new RotateAnimation(-getRotationBeforeReach(), 0, relative, center, relative, center);
        mReverseRotateAnimation.setInterpolator(sLinearInterpolator);
        mReverseRotateAnimation.setDuration(mRotAnimDuration);
        mReverseRotateAnimation.setFillAfter(true);

        setVisibility(INVISIBLE);
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        ViewPaddingKt.updatePaddings(this, Dimen.dp2px(getContext(), 15));
    }

    private void createViews(Context context) {
        mArrow = createArrowView(context);
        mTitleView = createTitleView(context);
    }

    protected AppCompatImageView createArrowView(Context ctx) {
        AppCompatImageView arrow = new AppCompatImageView(ctx);

        arrow.setVisibility(GONE);
        arrow.setImageResource(getArrowResource());
        ViewPaddingKt.updatePaddings(arrow, Dimen.dp2px(ctx, 3));
        arrow.setRotation(getRotationBeforeReach());

        int dimen = Dimen.dp2px(ctx, 26);
        LayoutParams p = new LayoutParams(dimen, dimen);
        addView(arrow, Math.min(getChildCount(), getArrowIndex()), p);
        return arrow;
    }

    private void setArrowBG(boolean completed) {
        Drawable bg = DrawableUtils.createBackground(completed ? mArrowBGClrCompleted : mArrowBGClrNormal, 100);
        mArrow.setBackgroundDrawable(bg);

        mArrow.setColorFilter(completed ? mArrowTintCompleted : mArrowTintNormal);
    }

    protected TextView createTitleView(Context context) {
        TextView titleView = new TextView(context);
        titleView.setTextSize(14);
        titleView.setTextColor(ContextCompat.getColor(context, R.color.colorText2));
        titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        LayoutParams p = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        addView(titleView, Math.min(getChildCount(), getTitleIndex()), p);
        return titleView;
    }

    protected CharSequence prepareTitle(String title, String chapterName) {
        if (mSwipeDisabled) {
            setVisibility(GONE);
        }

        if (title == null) {
            title = "";
        }

        SpannableString titleSS = new SpannableString(title);
        AbsoluteSizeSpan titleSpanSize = new AbsoluteSizeSpan(mTitleTxtSize);
        titleSS.setSpan(titleSpanSize, 0, titleSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        if (TextUtils.isEmpty(chapterName)) {
            mArrow.setVisibility(GONE);
            return titleSS;
        }

        mArrow.setVisibility(VISIBLE);

        SpannableString chapterNameSS = new SpannableString(chapterName);
        AbsoluteSizeSpan chapterNameSpanSize = new AbsoluteSizeSpan(mChapterTxtSize);
        chapterNameSS.setSpan(chapterNameSpanSize, 0, chapterNameSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        chapterNameSS.setSpan(new StyleSpan(Typeface.BOLD), 0, chapterNameSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        return TextUtils.concat(chapterNameSS, "\n", titleSS);
    }

    public void setName(int readType, String fullChapterName) {
        if (mTitleView == null) {
            return;
        }

        mTitleView.setText(prepareTitle(getTitle(readType), fullChapterName));

        relayout();
    }

    public void setNoFurther(int readType) {
        mTitleView.setText(prepareTitle(getNoFurtherTitle(readType), null));
    }

    public void setSwipeDisabled(boolean disabled) {
        mSwipeDisabled = disabled;
    }

    void relayout() {
        mTitleView.measure(0, 0);
        measure(0, 0);
        requestLayout();
    }

    protected abstract String getTitle(int readType);

    protected abstract String getNoFurtherTitle(int readType);

    protected abstract int getArrowResource();

    protected abstract int getTitleIndex();

    protected abstract int getArrowIndex();

    protected abstract float getRotationBeforeReach();

    @Override
    public int getStyle() {
        return STYLE_DEFAULT;
    }

    @Override
    public int getCustomHeight() {
        return getMeasuredHeight();
    }

    @NonNull
    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onFingerUp(SmoothRefreshLayout layout, IIndicator indicator) {

    }

    @Override
    public void onReset(SmoothRefreshLayout layout) {
        mJumpAnimation.cancel();
        mArrow.clearAnimation();

        if (mSwipeDisabled) {
            setVisibility(GONE);
            return;
        }

        setArrowBG(false);
        setVisibility(VISIBLE);
        relayout();
    }

    @Override
    public void onRefreshPrepare(SmoothRefreshLayout layout) {
        mJumpAnimation.cancel();

        if (mSwipeDisabled) {
            setVisibility(GONE);
            return;
        }

        setArrowBG(false);
        mJumpAnimation.start();
        setVisibility(VISIBLE);
    }

    @Override
    public void onRefreshBegin(SmoothRefreshLayout layout, IIndicator indicator) {
        if (mSwipeDisabled) {
            setVisibility(GONE);
        }
    }

    @Override
    public void onRefreshComplete(SmoothRefreshLayout layout, boolean isSuccessful) {
        if (mSwipeDisabled) {
            setVisibility(GONE);
            return;
        }

        setArrowBG(true);
    }

    @Override
    public void onRefreshPositionChanged(SmoothRefreshLayout frame, byte status, IIndicator indicator) {
        if (mSwipeDisabled) {
            return;
        }

        if (!indicator.hasTouched() || status != SmoothRefreshLayout.SR_STATUS_PREPARE) {
            return;
        }

        final int offsetToRefresh = getCustomHeight();
        final int currentPos = indicator.getCurrentPos();
        final int lastPos = indicator.getLastPos();

        if (currentPos < offsetToRefresh && lastPos >= offsetToRefresh) {
            mArrow.clearAnimation();
            mArrow.startAnimation(mReverseRotateAnimation);

            mJumpAnimation.cancel();
            mJumpAnimation.setStartDelay(mRotAnimDuration);
            mJumpAnimation.start();
            mJumpAnimation.setStartDelay(0);
        } else if (currentPos > offsetToRefresh && lastPos <= offsetToRefresh) {
            mArrow.clearAnimation();
            mArrow.startAnimation(mRotateAnimation);
            mJumpAnimation.cancel();
        }
    }

    @Override
    public void onPureScrollPositionChanged(SmoothRefreshLayout layout, byte status, IIndicator indicator) {
        if (indicator.hasJustLeftStartPosition()) {
            mJumpAnimation.cancel();
            mArrow.clearAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mJumpAnimation.cancel();
        mRotateAnimation.cancel();
        mReverseRotateAnimation.cancel();
    }
}
