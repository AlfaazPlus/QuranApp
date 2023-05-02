package com.quranapp.android.widgets.tablayout;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.core.content.ContextCompat.getColorStateList;
import static com.peacedesign.android.utils.Dimen.dp2px;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.gesture.HoverPushEffect;

@SuppressLint("ViewConstructor")
public class BottomTabView extends LinearLayout {
    private final BottomTabLayout mTabLayout;
    private final BottomTab mTab;
    private final boolean isKingTab;
    private final int primaryColor;
    private final int primaryColorDark;
    private final int kingTabTintColor;

    public BottomTabView(@NonNull BottomTabLayout tabLayout, @NonNull BottomTab tab) {
        super(tabLayout.getContext());
        mTabLayout = tabLayout;
        mTab = tab;
        isKingTab = tab.isKingTab();
        primaryColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        primaryColorDark = ContextCompat.getColor(getContext(), R.color.colorPrimaryDark);
        kingTabTintColor = ContextCompat.getColor(getContext(), R.color.white);
        init();
    }

    private void init() {
        initThis();
        initIconView(mTab);
        initLabelView(mTab);


        initThisParams(mTab);
    }

    private void initThis() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setId(View.generateViewId());


        if (isKingTab) {
            int padH = dp2px(getContext(), 10);
            int padV = dp2px(getContext(), 5);
            setPadding(padH, padV, padH, padV);

            Drawable background = DrawableUtils.createBackground(primaryColor, primaryColorDark,
                dp2px(getContext(), 100));
            setBackground(background);

            setOnTouchListener(new HoverPushEffect());
            setElevation(dp2px(getContext(), 5));
        } else {
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            setBackgroundResource(outValue.resourceId);
        }

        setOnClickListener(v -> {
            if (isKingTab) {
                mTabLayout.onKingTabClick(mTab);
            } else {
                mTabLayout.selectTab(mTab);
            }
        });

        mTab.setTabView(this);
    }

    private void initThisParams(BottomTab tab) {
        LinearLayout.LayoutParams params;

        int dimen = dp2px(getContext(), 50);

        final int width;
        if (tab.isKingTab()) {
            if (tab.getTabLabel() != null) {
                measure(0, 0);
                dimen = Math.max(getMeasuredWidth(), getMeasuredHeight());
            }
            width = dimen;
        } else {
            width = WRAP_CONTENT;
        }

        params = new LinearLayout.LayoutParams(width, dimen);
        LayoutParamsKt.updateMarginVertical(params, dp2px(getContext(), 5));
        params.gravity = Gravity.CENTER;

        setLayoutParams(params);
    }

    private void initIconView(BottomTab tab) {
        if (tab.getTabIcon() == 0) return;
        Drawable drawable = ContextCompat.getDrawable(getContext(), tab.getTabIcon());
        if (drawable == null) return;

        ImageView iconView = new ImageView(getContext());

        int dimen = dp2px(getContext(), isKingTab ? 40 : 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(isKingTab ? WRAP_CONTENT : dimen, dimen);
        params.gravity = Gravity.CENTER;
        iconView.setLayoutParams(params);


        if (isKingTab) {
            drawable.setTint(kingTabTintColor);
        } else {
            DrawableCompat.setTintList(drawable.mutate(),
                getColorStateList(getContext(), R.color.color_bottom_tablayout_icon));
        }
        iconView.setImageDrawable(drawable);

        addView(iconView);
    }

    private void initLabelView(BottomTab tab) {
        if (tab.getTabLabel() == null) return;

        AppCompatTextView labelView = new AppCompatTextView(getContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        labelView.setLayoutParams(params);

        labelView.setText(tab.getTabLabel());
        labelView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);

        labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, ContextKt.getDimenPx(getContext(), R.dimen.dmnCommonSize3_5));

        if (isKingTab) {
            labelView.setTextColor(kingTabTintColor);
        } else {
            labelView.setTextColor(ContextKt.colorStateList(getContext(), R.color.color_bottom_tablayout_label));
        }

        addView(labelView);
    }

    void unselectView() {
        mTab.setSelected(false);
        setSelected(false);
    }

    void selectView() {
        mTab.setSelected(true);
        setSelected(true);
    }
}