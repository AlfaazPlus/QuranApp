package com.peacedesign.android.widget.list.base;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.VERTICAL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.peacedesign.R;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ResUtils;

@SuppressLint("ViewConstructor")
public class BaseListItemView extends FrameLayout {
    private final BaseListItem mItem;
    public LinearLayout mContainerView;
    public AppCompatImageView mIconView;
    public LinearLayout mLabelCont;
    public AppCompatTextView mLabelView;
    public AppCompatTextView mMessageView;

    public BaseListItemView(@NonNull Context ctx, @NonNull BaseListItem item) {
        super(ctx);
        mItem = item;
        init();
    }

    private Context resolveItemStyle() {
        return new ContextThemeWrapper(getContext(), R.style.SimpleListItemStyle);
    }

    private Context resolveItemIconStyle() {
        return new ContextThemeWrapper(getContext(), R.style.SimpleListItemIconStyle);
    }

    private Context resolveItemLabelStyle() {
        return new ContextThemeWrapper(getContext(), R.style.SimpleListItemLabelStyle);
    }

    private Context resolveItemMassageStyle() {
        return new ContextThemeWrapper(getContext(), R.style.SimpleListItemMessageStyle);
    }

    private void init() {
        initThis();
        initContainer();
        initIconView();
        initLabelCont();

        measure(0, 0);
    }

    private void initThis() {
        updateDisability();
    }

    private void initContainer() {
        mContainerView = new LinearLayout(resolveItemStyle());
        LayoutParams params = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        mContainerView.setLayoutParams(params);

        addView(mContainerView);
    }

    private void initIconView() {
        Drawable iconDrawable = ResUtils.getDrawable(getContext(), mItem.getIcon());
        if (iconDrawable == null) return;

        mIconView = new AppCompatImageView(resolveItemIconStyle());

        int size = Dimen.dp2px(getContext(), 40);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.gravity = Gravity.TOP;
        mIconView.setLayoutParams(params);

        updateIcon();
        mContainerView.addView(mIconView);
    }

    private void initLabelCont() {
        mLabelCont = new LinearLayout(getContext());

        LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        int marg = Dimen.dp2px(getContext(), 10);
        params.setMargins(marg, 0, marg, 0);
        params.gravity = Gravity.CENTER_VERTICAL;
        mLabelCont.setLayoutParams(params);
        mLabelCont.setOrientation(VERTICAL);

        initLabelView();
        initMessageView();

        mContainerView.addView(mLabelCont);
    }

    private void initLabelView() {
        if (mLabelCont == null || mLabelView != null || TextUtils.isEmpty(mItem.getLabel())) return;

        mLabelView = new AppCompatTextView(resolveItemLabelStyle());

        updateLabel();
        mLabelCont.addView(mLabelView);
    }

    private void initMessageView() {
        if (mLabelCont == null || mMessageView != null || TextUtils.isEmpty(mItem.getMessage())) {
            return;
        }

        mMessageView = new AppCompatTextView(resolveItemMassageStyle());

        /*mMessageView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorBodyTer1Text));
        mMessageView.setTextSize(Dimen.getDimenSp(getContext(), R.dimen.dmnCommonSizeTer));
        mMessageView.setMaxLines(2);
        mMessageView.setEllipsize(TextUtils.TruncateAt.END);*/

        updateDescription();

        mLabelCont.addView(mMessageView);
    }

    public void updateIcon() {
        if (mIconView == null) {
            initIconView();
        } else {
            Drawable iconDrawable = ResUtils.getDrawable(getContext(), mItem.getIcon());
            if (iconDrawable != null) {
                mIconView.setImageDrawable(iconDrawable);
            }
        }
    }

    public void updateLabel() {
        if (mLabelView == null) {
            initMessageView();
        } else {
            if (TextUtils.isEmpty(mItem.getLabel())) {
                ((ViewGroup) mLabelView.getParent()).removeView(mLabelView);
            } else if (mLabelView.getText() != mItem.getLabel()) {
                mLabelView.setText(mItem.getLabel());
            }
        }
    }

    private void updateDescription() {
        if (mMessageView == null) {
            initMessageView();
        } else {
            if (TextUtils.isEmpty(mItem.getMessage())) {
                ((ViewGroup) mMessageView.getParent()).removeView(mMessageView);
            } else if (mMessageView.getText() != mItem.getMessage()) {
                mMessageView.setText(mItem.getMessage());
            }
        }
    }

    public void updateDisability() {
        setEnabled(mItem.isEnabled());
        setAlpha(mItem.isEnabled() ? 1f : 0.5f);
    }

    public void notifyForChange() {
        updateIcon();
        updateLabel();
        updateDescription();
        updateDisability();
        setSelected(mItem.isSelected());
    }

    public void setItemBackground(Drawable background) {
        mContainerView.setBackground(background);
    }

    public void setItemBackground(@DrawableRes int backgroundRes) {
        mContainerView.setBackgroundResource(backgroundRes);
    }
}

