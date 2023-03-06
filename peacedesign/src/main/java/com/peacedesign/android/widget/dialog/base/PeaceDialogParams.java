package com.peacedesign.android.widget.dialog.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.content.DialogInterface.OnDismissListener;
import static android.content.DialogInterface.OnKeyListener;

class PeaceDialogParams {
    @SuppressLint("UnknownNullness")
    public Context context;
    public boolean focusOnPositive;
    public boolean focusOnNegative;
    public boolean focusOnNeutral;
    public boolean cancelable = true;
    public boolean cancelOnTouchOutside = true;
    public boolean dismissOnNegative = true;
    public boolean dismissOnPositive = true;
    public boolean dismissOnNeutral = true;
    public boolean dismissOnSwipe;
    public boolean fullscreen;
    @Nullable
    public View contentView;
    @LayoutRes
    public int contentViewResId = -1;
    @Nullable
    public View mCustomTitleView;
    @Nullable
    public CharSequence mTitle;
    @Nullable
    public CharSequence mMessage;
    public int mTitleTextAlignment = View.TEXT_ALIGNMENT_CENTER;
    public int mTitleTextAppearance;
    public int mMessageTextAlignment = View.TEXT_ALIGNMENT_CENTER;
    @Nullable
    public CharSequence positiveButtonText;
    @ColorInt
    public int positiveButtonTextColor;
    @Nullable
    public OnClickListener positiveButtonListener;
    @Nullable
    public CharSequence negativeButtonText;
    @ColorInt
    public int negativeButtonTextColor;
    @Nullable
    public OnClickListener negativeButtonListener;
    @Nullable
    public CharSequence neutralButtonText;
    @ColorInt
    public int neutralButtonTextColor;
    @Nullable
    public OnClickListener neutralButtonListener;
    @PeaceDialog.DialogButtonsDirection
    public int buttonsDirection = PeaceDialog.DIRECTION_AUTO;
    @PeaceDialog.DialogGravity
    public int dialogGravity = PeaceDialog.GRAVITY_CENTER;
    public int dialogWidth = ViewGroup.LayoutParams.MATCH_PARENT;
    @Nullable
    public OnShowListener onShowListener;
    @Nullable
    public OnCancelListener onCancelListener;
    @Nullable
    public OnDismissListener onDismissListener;
    @Nullable
    public OnKeyListener onKeyListener;

    public PeaceDialogParams(@NonNull Context context) {
        this.context = context;
    }

    public void apply(@NonNull PeaceDialogController controller) {
        if (mCustomTitleView != null) {
            controller.setCustomTitle(mCustomTitleView);
        } else {
            controller.setTitle(mTitle);
        }

        controller.setMessage(mMessage);
        controller.setView(contentView);
        controller.setView(contentViewResId);

        if (positiveButtonText != null) {
            controller.setButton(BUTTON_POSITIVE, positiveButtonText, positiveButtonTextColor, null,
                positiveButtonListener);
        }
        if (negativeButtonText != null) {
            controller.setButton(BUTTON_NEGATIVE, negativeButtonText, negativeButtonTextColor, null,
                negativeButtonListener);
        }
        if (neutralButtonText != null) {
            controller.setButton(BUTTON_NEUTRAL, neutralButtonText, neutralButtonTextColor, null,
                neutralButtonListener);
        }

        controller.setFullScreen(fullscreen);
        controller.setDialogGravity(dialogGravity);
        controller.setTitleTextAlignment(mTitleTextAlignment);
        controller.setTitleTextAppearance(mTitleTextAppearance);
        controller.setMessageTextAlignment(mMessageTextAlignment);
        controller.setButtonsDirection(buttonsDirection);
        controller.setFocusOnPositive(focusOnPositive);
        controller.setFocusOnNegative(focusOnNegative);
        controller.setFocusOnNeutral(focusOnNeutral);
        controller.setDismissOnPositive(dismissOnPositive);
        controller.setDismissOnNegative(dismissOnNegative);
        controller.setDismissOnNeutral(dismissOnNeutral);
    }
}
