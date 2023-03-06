package com.peacedesign.android.widget.dialog.base;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.peacedesign.R;
import com.peacedesign.android.utils.Dimen;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("UnusedReturnValue")
public class PeaceDialog extends Dialog {
    public static final int DIRECTION_AUTO = 0x0, SIDE_BY_SIDE = 0x1, STACKED = 0x2;
    public static final int GRAVITY_CENTER = 0x4, GRAVITY_TOP = 0x5, GRAVITY_BOTTOM = 0x6;

    protected PeaceDialogController controller;
    int dialogWidth = MATCH_PARENT;
    private boolean fullscreen;

    protected PeaceDialog(@NonNull Context context) {
        this(context, R.style.PeaceDialogTheme);
    }

    protected PeaceDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        controller = getController(getContext(), this);
    }

    @NonNull
    public static Builder newBuilder(@NonNull Context context) {
        return new Builder(context);
    }

    @NonNull
    public static Builder newBuilder(@NonNull Context context, int themeResId) {
        return new Builder(context, themeResId);
    }

    @NonNull
    protected PeaceDialogController getController(@NonNull Context context, @NonNull PeaceDialog dialog) {
        return PeaceDialogController.create(context, dialog);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeContents();
        setupDimension();
    }

    protected void initializeContents() {
        controller.installContent();
        controller.postCreate();
    }

    /**
     * Setup dimensions of the dialog.
     */
    public void setupDimension() {
        Window window = getWindow();
        if (window == null) {
            return;
        }

        View decorView = window.getDecorView();

        // resolve width and height for different scenarios and set it to dialog window's layout.
        decorView.measure(0, 0);
        int resolveDialogWidth = resolveDialogWidth(decorView);

        int measureWidth = resolveDialogWidth;
        if (measureWidth == MATCH_PARENT) {
            measureWidth = Dimen.getWindowWidth(getContext());
        }

        decorView.measure(makeMeasureSpec(measureWidth, EXACTLY), 0);

        int resolveDialogHeight = resolveDialogHeight(decorView);
        window.setLayout(resolveDialogWidth, resolveDialogHeight);
    }

    /**
     * Get width to set to dialog for different configurations.
     */
    protected int resolveDialogWidth(@NonNull View decorView) {
        // if dialog is to be show fullscreen then just make it of full width.
        // Otherwise if dialog width style is set to WRAP_CONTENT, return the minimum of its measured with (width for wrap_content)
        // and custom max width.
        // Otherwise return the custom max width.
        if (fullscreen) {
            return MATCH_PARENT;
        } else {
            int maxWidth = controller.getDialogMaxWidth();

            if (dialogWidth == WRAP_CONTENT) {
                int measuredWidth = decorView.getMeasuredWidth();
                return Math.min(maxWidth, measuredWidth);
            } else if (dialogWidth == MATCH_PARENT) {
                return maxWidth;
            } else {
                return dialogWidth;
            }
        }
    }

    boolean hasCustomWidth() {
        return dialogWidth != MATCH_PARENT && dialogWidth != WRAP_CONTENT;
    }

    /**
     * Get height to set to dialog for different configurations.
     */
    protected int resolveDialogHeight(@NonNull View decorView) {
        // if dialog is to be show fullscreen then just make it of full height.
        // Otherwise limit the height to a custom max height.
        if (fullscreen) {
            return MATCH_PARENT;
        } else {
            int maxHeight = controller.getDialogMaxHeight();
            int minHeight = controller.getDialogMinHeight();

            // get dialog's measured height.
            int height = decorView.getMeasuredHeight();
            // adjust dialog's min & max heights.
            height = Math.max(height, minHeight);
            height = Math.min(height, maxHeight);
            return height;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (controller.onKeyDown(keyCode, event)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (controller.onKeyUp(keyCode, event)) return true;
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Set if the dialog should be display at whole screen, i.e., it will be of full width and full height.
     * Setting this value also changes corner radius to 0 of the default background.
     *
     * @param flag flag.
     */
    public void setFullScreen(boolean flag) {
        fullscreen = flag;
    }

    public void setElevation(float elevation) {
        controller.setElevation(elevation);
        controller.setupWindow();
    }

    public void setDimAmount(float dimAmount) {
        controller.setDimAmount(dimAmount);
        controller.setupWindow();
    }

    @Override
    public void setTitle(@NonNull CharSequence title) {
        super.setTitle(title);
        controller.setTitle(title);
        setupDimension();
    }

    @Override
    public void setTitle(@StringRes int titleRes) {
        setTitle(getContext().getString(titleRes));
    }

    @Override
    public void show() {
        super.show();
        setupDimension();
        controller.setupWindow();
    }

    /**
     * @see Builder#setCustomTitle(View)
     */
    public void setCustomTitle(@NonNull View customTitleView) {
        controller.setCustomTitle(customTitleView);
    }

    public void setMessage(@NonNull CharSequence message) {
        controller.setMessage(message);
    }

    public void setMessage(@StringRes int messageRes) {
        setMessage(getContext().getString(messageRes));
    }

    /**
     * Set the view to display in that dialog.
     */
    public void setView(@NonNull View view) {
        controller.setView(view);
    }

    /**
     * Set a message to be sent when a button is pressed.
     *
     * @param whichButton Which button to set the message for, can be one of
     *                    {@link DialogInterface#BUTTON_POSITIVE},
     *                    {@link DialogInterface#BUTTON_NEGATIVE}, or
     *                    {@link DialogInterface#BUTTON_NEUTRAL}
     * @param text        The text to display in positive button.
     * @param msg         The {@link Message} to be sent when clicked.
     */
    public void setButton(int whichButton, CharSequence text, Message msg) {
        controller.setButton(whichButton, text, 0, msg, null);
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param whichButton Which button to set the listener on, can be one of
     *                    {@link DialogInterface#BUTTON_POSITIVE},
     *                    {@link DialogInterface#BUTTON_NEGATIVE}, or
     *                    {@link DialogInterface#BUTTON_NEUTRAL}
     * @param text        The text to display in positive button.
     * @param listener    The {@link OnClickListener} to use.
     */
    public void setButton(int whichButton, CharSequence text, @Nullable OnClickListener listener) {
        controller.setButton(whichButton, text, 0, null, listener);
    }

    public void setButtonEnabled(int whichButton, boolean enabled) {
        controller.setButtonEnabled(whichButton, enabled);
    }

    /**
     * Set if the dialog should have width wrapping its content or it should be full width.
     * However in both cases the width will always have some offsets horizontally.
     *
     * @param width Width style of the dialog.
     */
    public void setDialogWidth(int width) {
        dialogWidth = width;
    }

    /**
     * Set gravity of the dialog to show it on top, bottom or center.
     * This value is ignored if dialog is set to fullscreen via {@link #setFullScreen(boolean)}.
     * Setting this value preferred when the dialog's contents are not are big.
     *
     * @param gravity gravity of the dialog.
     */
    public void setDialogGravity(@DialogGravity int gravity) {
        controller.setDialogGravity(gravity);
    }

    public View getView() {
        return controller.getView();
    }

    @NonNull
    public View getMessageView() {
        return controller.getMessageView();
    }

    @IntDef({GRAVITY_TOP, GRAVITY_CENTER, GRAVITY_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogGravity {}

    @IntDef({DIRECTION_AUTO, SIDE_BY_SIDE, STACKED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogButtonsDirection {}

    public static class Builder {
        private final PeaceDialogParams P;
        private final PeaceDialog dialog;

        private Builder(@NonNull Context context) {
            P = new PeaceDialogParams(context);
            dialog = new PeaceDialog(context);
        }

        private Builder(@NonNull Context context, int themeResId) {
            P = new PeaceDialogParams(context);
            dialog = new PeaceDialog(context, themeResId);
        }

        @NonNull
        public Context getContext() {
            return P.context;
        }

        @NonNull
        public Builder setTitle(@StringRes int titleId) {
            return setTitle(P.context.getText(titleId));
        }

        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            P.mTitle = title;
            return this;
        }

        @NonNull
        public Builder setCustomTitle(@NonNull View customTitleView) {
            P.mCustomTitleView = customTitleView;
            return this;
        }

        @NonNull
        public Builder setMessage(@StringRes int messageId) {
            return setMessage(P.context.getText(messageId));
        }

        @NonNull
        public Builder setMessage(@NonNull CharSequence message) {
            P.mMessage = message;
            return this;
        }

        @NonNull
        public Builder setPositiveButton(@StringRes int textId, @Nullable OnClickListener listener) {
            return setPositiveButton(textId, 0, listener);
        }

        @NonNull
        public Builder setPositiveButton(@NonNull CharSequence text, @Nullable OnClickListener listener) {
            return setPositiveButton(text, 0, listener);
        }

        @NonNull
        public Builder setPositiveButton(@StringRes int textId, @ColorInt int textColor, @Nullable OnClickListener listener) {
            return setPositiveButton(P.context.getText(textId), textColor, listener);
        }

        @NonNull
        public Builder setPositiveButton(@NonNull CharSequence text, @ColorInt int textColor, @Nullable OnClickListener listener) {
            P.positiveButtonText = text;
            P.positiveButtonTextColor = textColor;
            P.positiveButtonListener = listener;
            return this;
        }

        @NonNull
        public Builder setNegativeButton(@StringRes int textId, @Nullable OnClickListener listener) {
            return setNegativeButton(textId, 0, listener);
        }

        @NonNull
        public Builder setNegativeButton(@NonNull CharSequence text, @Nullable OnClickListener listener) {
            return setNegativeButton(text, 0, listener);
        }

        @NonNull
        public Builder setNegativeButton(@StringRes int textId, @ColorInt int textColor, @Nullable OnClickListener listener) {
            return setNegativeButton(P.context.getText(textId), textColor, listener);
        }

        @NonNull
        public Builder setNegativeButton(@NonNull CharSequence text, @ColorInt int textColor, @Nullable OnClickListener listener) {
            P.negativeButtonText = text;
            P.negativeButtonTextColor = textColor;
            P.negativeButtonListener = listener;
            return this;
        }

        @NonNull
        public Builder setNeutralButton(@StringRes int textId, @Nullable OnClickListener listener) {
            return setNeutralButton(textId, 0, listener);
        }

        @NonNull
        public Builder setNeutralButton(@NonNull CharSequence text, @Nullable OnClickListener listener) {
            return setNeutralButton(text, 0, listener);
        }

        @NonNull
        public Builder setNeutralButton(@StringRes int textId, @ColorInt int textColor, @Nullable OnClickListener listener) {
            return setNeutralButton(P.context.getText(textId), textColor, listener);
        }

        @NonNull
        public Builder setNeutralButton(@NonNull CharSequence text, @ColorInt int textColor, @Nullable OnClickListener listener) {
            P.neutralButtonText = text;
            P.negativeButtonTextColor = textColor;
            P.neutralButtonListener = listener;
            return this;
        }

        /**
         * If it is set to true, the button text style will bold and its color will be different.
         *
         * @param flag flag
         */
        @NonNull
        public Builder setFocusOnPositive(boolean flag) {
            P.focusOnPositive = flag;
            return this;
        }

        /**
         * If it is set to true, the button text style will bold and its color will be different.
         *
         * @param flag flag
         */
        @NonNull
        public Builder setFocusOnNegative(boolean flag) {
            P.focusOnNegative = flag;
            return this;
        }

        /**
         * If it is set to true, the button text style will bold and its color will be different.
         *
         * @param flag flag
         */
        @NonNull
        public Builder setFocusOnNeutral(boolean flag) {
            P.focusOnNeutral = flag;
            return this;
        }

        /**
         * Set if dialog should be dismissed on Positive button click.
         *
         * @param flag flag
         */
        @NonNull
        public Builder setDismissOnPositive(boolean flag) {
            P.dismissOnPositive = flag;
            return this;
        }

        /**
         * Set if dialog should be dismissed on Negative button click.
         *
         * @param flag flag
         */
        @NonNull
        public Builder setDismissOnNegative(boolean flag) {
            P.dismissOnNegative = flag;
            return this;
        }

        /**
         * Set if dialog should be dismissed on Neutral button click.
         *
         * @param flag flag
         */
        @NonNull
        public Builder setDismissOnNeutral(boolean flag) {
            P.dismissOnNeutral = flag;
            return this;
        }

        @NonNull
        public Builder setFullscreen(boolean flag) {
            P.fullscreen = flag;
            return this;
        }

        /**
         * Set if the dialog should have width wrapping its content or it should be full width.
         * However in both cases the width will always have some offsets horizontally.
         *
         * @param width Width style of the dialog.
         */
        public void setDialogWidth(int width) {
            P.dialogWidth = width;
        }

        /**
         * Set gravity of the dialog to show it on top, bottom or center.
         * This value is ignored if dialog is set to fullscreen via {@link #setFullScreen(boolean)}.
         * Setting this value preferred when the dialog's contents are not are big.
         *
         * @param gravity gravity of the dialog.
         */
        @NonNull
        public Builder setDialogGravity(@DialogGravity int gravity) {
            P.dialogGravity = gravity;
            return this;
        }

        @NonNull
        public Builder setTitleTextAlignment(int alignment) {
            P.mTitleTextAlignment = alignment;
            return this;
        }

        @NonNull
        public Builder setTitleTextAppearance(int appearance) {
            P.mTitleTextAppearance = appearance;
            return this;
        }

        @NonNull
        public Builder setMessageTextAlignment(int alignment) {
            P.mMessageTextAlignment = alignment;
            return this;
        }

        /**
         * Set the direction of the action buttons.
         *
         * @param direction Direction in which buttons should be shown.
         */
        @NonNull
        public Builder setButtonsDirection(@DialogButtonsDirection int direction) {
            P.buttonsDirection = direction;
            return this;
        }

        @NonNull
        public Builder setCancelable(boolean cancelable) {
            P.cancelable = cancelable;
            return this;
        }

        @NonNull
        public Builder setCanceledOnTouchOutside(boolean flag) {
            P.cancelOnTouchOutside = flag;
            return this;
        }

        @NonNull
        public Builder setOnShowListener(@Nullable OnShowListener onShowListener) {
            P.onShowListener = onShowListener;
            return this;
        }

        @NonNull
        public Builder setOnCancelListener(@Nullable OnCancelListener onCancelListener) {
            P.onCancelListener = onCancelListener;
            return this;
        }


        @NonNull
        public Builder setOnDismissListener(@Nullable OnDismissListener onDismissListener) {
            P.onDismissListener = onDismissListener;
            return this;
        }


        @NonNull
        public Builder setOnKeyListener(@Nullable OnKeyListener onKeyListener) {
            P.onKeyListener = onKeyListener;
            return this;
        }

        /**
         * Add an additional custom view to the dialog.
         * It is recommended not to add a custom view and a long dialog message text together.
         *
         * @param layoutResId Resource id of the custom view.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        @NonNull
        public Builder setView(@LayoutRes int layoutResId) {
            P.contentViewResId = layoutResId;
            return this;
        }

        /**
         * Add an additional custom view to the dialog.
         * It is recommended not to add a custom view and a long dialog message text together.
         *
         * @param view The custom view.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        @NonNull
        public Builder setView(@NonNull View view) {
            P.contentView = view;
            return this;
        }

        /**
         * Creates an {@link PeaceDialog} with the arguments supplied to this
         * builder.
         * <p>
         * Calling this method does not display the dialog. If no additional
         * processing is needed, {@link #show()} may be called instead to both
         * create and display the dialog.
         */
        @NonNull
        public PeaceDialog create() {
            P.apply(dialog.controller);

            dialog.setOnShowListener(P.onShowListener);
            dialog.setOnCancelListener(P.onCancelListener);
            dialog.setOnDismissListener(P.onDismissListener);
            dialog.setFullScreen(P.fullscreen);
            dialog.setDialogWidth(P.dialogWidth);


            // Set if dialog should be dismissed on outside touch.
            dialog.setCanceledOnTouchOutside(P.cancelable && P.cancelOnTouchOutside);
            // Set if dialog is cancelable.
            dialog.setCancelable(P.cancelable);

            if (P.onKeyListener != null) {
                dialog.setOnKeyListener(P.onKeyListener);
            }

            return dialog;
        }

        public void show() {
            final PeaceDialog dialog = create();

            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
        }
    }
}
