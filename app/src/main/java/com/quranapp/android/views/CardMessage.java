/*
 * (c) Faisal Khan. Created on 13/2/2022.
 */

package com.quranapp.android.views;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import static com.quranapp.android.components.utility.CardMessageParams.STYLE_WARNING;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.components.utility.CardMessageParams;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.extensions.ViewPaddingKt;

public class CardMessage extends LinearLayout {
    private final CardMessageParams mParams = new CardMessageParams();

    private AppCompatImageView mIconView;
    private AppCompatTextView mMessageView;
    private AppCompatTextView mActionView;

    public static CardMessage warning(Context ctx, @StringRes int strResId) {
        CardMessage msgView = new CardMessage(ctx);
        msgView.setElevation(ContextKt.dp2px(ctx, 4));
        msgView.setMessageStyle(STYLE_WARNING);
        msgView.setMessage(ctx.getString(strResId));
        return msgView;
    }

    public CardMessage(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setId(View.generateViewId());
        ViewPaddingKt.updatePaddings(this, ContextKt.dp2px(context, 15));
        setOrientation(HORIZONTAL);
        setBackgroundResource(R.drawable.dr_bg_chapter_card);
        setGravity(Gravity.CENTER_VERTICAL);
    }

    private void createIconView() {
        if (mIconView != null) {
            return;
        }

        mIconView = new AppCompatImageView(getContext());
        int dimen = ContextKt.getDimenPx(getContext(), R.dimen.dmnActionButtonSmall);
        addView(mIconView, resolveIconViewIndex(), new ViewGroup.LayoutParams(dimen, dimen));
    }

    private void createMessageView() {
        if (mMessageView != null) {
            return;
        }

        mMessageView = new AppCompatTextView(getContext());
        mMessageView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            ContextKt.getDimenPx(getContext(), R.dimen.dmnCommonSize2));
        mMessageView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);

        LinearLayout.LayoutParams p = new LayoutParams(0, WRAP_CONTENT);
        p.weight = 1;
        LayoutParamsKt.updateMarginHorizontal(p, Dimen.dp2px(getContext(), 10));

        addView(mMessageView, resolveMsgViewIndex(), p);
    }

    private void createActionView() {
        if (mActionView != null) {
            return;
        }

        mActionView = new AppCompatTextView(new ContextThemeWrapper(getContext(), R.style.ButtonAction));
        mActionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, ContextKt.getDimenPx(getContext(), R.dimen.dmnCommonSize2));
        ViewPaddingKt.updatePaddingHorizontal(mActionView, ContextKt.dp2px(getContext(), 8));

        mActionView.setOnClickListener(v -> {
            if (mParams.getActionListener() != null) {
                mParams.getActionListener().run();
            }
        });

        addView(mActionView, resolveActionViewIndex());
    }

    private int resolveIconViewIndex() {
        return 0;
    }

    private int resolveMsgViewIndex() {
        int index = resolveIconViewIndex();
        if (mIconView == null) {
            return index;
        }
        return index + 1;
    }

    private int resolveActionViewIndex() {
        int index = resolveMsgViewIndex();
        if (mMessageView == null) {
            return index;
        }
        return index + 1;
    }

    public void setIcon(Drawable icon) {
        mParams.setIcon(icon);

        boolean hasIcon = icon != null;
        if (hasIcon) {
            if (mIconView == null) {
                createIconView();
            }
            mIconView.setImageDrawable(icon);
        } else {
            ViewKt.removeView(mIconView);
        }
    }

    public void setMessage(CharSequence message) {
        mParams.setMessage(message);

        boolean hasMessage = !TextUtils.isEmpty(message);
        if (hasMessage) {
            if (mMessageView == null) {
                createMessageView();
            }
            mMessageView.setText(message);
        } else {
            ViewKt.removeView(mMessageView);
        }
    }

    public void setActionText(CharSequence actionText, Runnable actionListener) {
        mParams.setActionText(actionText);
        mParams.setActionListener(actionListener);

        boolean hasText = !TextUtils.isEmpty(actionText);
        if (hasText) {
            if (mActionView == null) {
                createActionView();
            }
            mActionView.setText(actionText);
        } else {
            ViewKt.removeView(mActionView);
        }
    }

    public void setMessageStyle(int style) {
        mParams.setMessageStyle(style);

        if (style == STYLE_WARNING) {
            if (mIconView == null) {
                createIconView();
            }
            mIconView.setColorFilter(ColorUtils.WARNING);
            mIconView.setImageResource(R.drawable.dr_icon_info);
        }
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        if (params instanceof ViewGroup.MarginLayoutParams) {
            LayoutParamsKt.updateMargins((ViewGroup.MarginLayoutParams) params, Dimen.dp2px(getContext(), 5));
        }
    }
}
