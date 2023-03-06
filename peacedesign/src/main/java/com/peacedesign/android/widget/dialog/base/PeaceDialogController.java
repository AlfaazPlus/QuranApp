package com.peacedesign.android.widget.dialog.base;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;
import static com.peacedesign.android.widget.dialog.base.PeaceDialog.SIDE_BY_SIDE;
import static com.peacedesign.android.widget.dialog.base.PeaceDialog.STACKED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;

import com.peacedesign.R;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog.DialogButtonsDirection;
import com.peacedesign.android.widget.dialog.base.PeaceDialog.DialogGravity;

import java.lang.ref.WeakReference;

@SuppressLint("UnknownNullness")
public class PeaceDialogController {
    private final PeaceDialog mDialog;
    final Handler mHandler;
    private final Context mContext;
    public int mTitleTextAlignment = View.TEXT_ALIGNMENT_CENTER;
    public int mMessageTextAlignment = View.TEXT_ALIGNMENT_CENTER;
    @Nullable
    protected CharSequence mMessage;
    protected NestedScrollView mScrollView;
    protected TextView mMessageView;
    TextView mButtonPositive;
    Message mButtonPositiveMessage;
    TextView mButtonNegative;
    Message mButtonNegativeMessage;
    TextView mButtonNeutral;
    Message mButtonNeutralMessage;
    boolean dismissOnNegative = true;
    boolean dismissOnPositive = true;
    boolean dismissOnNeutral = true;
    private final View.OnClickListener mButtonClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Message m;
            final boolean dismiss;
            if (v == mButtonPositive && mButtonPositiveMessage != null) {
                m = Message.obtain(mButtonPositiveMessage);
                dismiss = dismissOnPositive;
            } else if (v == mButtonNegative && mButtonNegativeMessage != null) {
                m = Message.obtain(mButtonNegativeMessage);
                dismiss = dismissOnNegative;
            } else if (v == mButtonNeutral && mButtonNeutralMessage != null) {
                m = Message.obtain(mButtonNeutralMessage);
                dismiss = dismissOnNeutral;
            } else {
                m = null;
                dismiss = true;
            }

            if (m != null) {
                m.sendToTarget();
            }

            if (dismiss) {
                // Post a message so we dismiss after the above handlers are executed
                mHandler.obtainMessage(ButtonHandler.MSG_DISMISS_DIALOG, mDialog).sendToTarget();
            }
        }
    };
    @DialogButtonsDirection
    private int mButtonsDirection = PeaceDialog.DIRECTION_AUTO;
    @DialogGravity
    private int mDialogGravity = PeaceDialog.GRAVITY_CENTER;
    private boolean mFullscreen;
    private float mElevation = -1;
    private float mDimAmount = 0.6F;
    private boolean mAllowOutsideTouches;
    private CharSequence mTitle;
    private View mCustomTitleView;
    private View mView;
    @LayoutRes
    private int mViewResId = -1;
    private CharSequence mButtonPositiveText;
    private int mButtonPositiveTextColor = 0;
    private CharSequence mButtonNegativeText;
    private int mButtonNegativeTextColor = 0;
    private CharSequence mButtonNeutralText;
    private int mButtonNeutralTextColor = 0;
    private TextView mTitleView;
    private boolean focusOnPositive;
    private boolean focusOnNegative;
    private boolean focusOnNeutral;
    private View mAdapterView;
    private int mTitleTextAppearance;
    private LinearLayout mButtonsPanel;

    protected PeaceDialogController(@NonNull Context context, @NonNull PeaceDialog dialogInterface) {
        mContext = context;
        mDialog = dialogInterface;
        mHandler = new ButtonHandler(dialogInterface);

        mDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }

    public static PeaceDialogController create(Context context, PeaceDialog di) {
        return new PeaceDialogController(context, di);
    }

    protected void postCreate() {
    }

    public void installContent() {
        setupWindow();

        Window window = mDialog.getWindow();
        if (window != null) {
            window.setContentView(getDialogView());
        }
    }

    /**
     * Setup the basic configurations of the window.
     */
    protected void setupWindow() {
        Window window = mDialog.getWindow();
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams windowParams = window.getAttributes();
        // Get the resource id of rounded corner background if the dialog is to be displayed fullscreen.
        @DrawableRes int bgRes = mFullscreen ? R.drawable.dr_bg_peace_dialog : R.drawable.dr_bg_peace_dialog_cornered;
        Drawable bgDrawable = AppCompatResources.getDrawable(mContext, bgRes);
        window.setBackgroundDrawable(bgDrawable);
        // Set dialog to clip to its rounded cornered (if it is set) outline.
        window.getDecorView().setClipToOutline(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (mElevation != -1) {
                window.setElevation(mElevation);
            } else {
                window.setElevation(Dimen.dp2px(getContext(), 16));
            }
        }

        // Setup gravity (position on screen) and animations of the dialog.
        setupGravityAndAnimations(mDialogGravity, windowParams);

        window.setAttributes(windowParams);

        if (mFullscreen) {
            windowParams.dimAmount = 0;
            int bgColor = resolveDialogBGColor(mContext);
            window.clearFlags(FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(FLAG_TRANSLUCENT_NAVIGATION);

            window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            window.setStatusBarColor(bgColor);
            window.setNavigationBarColor(bgColor);
        } else {
            windowParams.dimAmount = mDimAmount;
        }

        if (mAllowOutsideTouches) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        }
    }

    private int resolveDialogBGColor(Context context) {
        TypedValue out = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.peaceDialogLayoutBackgroundColor, out, true);
        return out.data;
    }

    protected View getDialogView() {
        int contentViewRes = selectLayout();
        View contentView = mDialog.getWindow().getLayoutInflater().inflate(contentViewRes, null, false);

        final View parentPanel = contentView.findViewById(R.id.parentPanel);
        setupLayout(parentPanel);

        return contentView;
    }

    /**
     * Setup gravity (position on screen) and animations of the dialog.
     */
    private void setupGravityAndAnimations(@DialogGravity int dialogGravity, WindowManager.LayoutParams windowParams) {
        // if dialog is not to shown fullscreen and gravity is TOP.
        if (!mFullscreen && dialogGravity == PeaceDialog.GRAVITY_TOP) {
            windowParams.y = Dimen.dp2px(getContext(), 20);
            windowParams.gravity = Gravity.TOP;
            windowParams.windowAnimations = R.style.PeaceDialogAnimation_TOP;
        }
        // if dialog is not to shown fullscreen and gravity is BOTTOM.
        else if (!mFullscreen && dialogGravity == PeaceDialog.GRAVITY_BOTTOM) {
            windowParams.y = Dimen.dp2px(getContext(), 30);
            windowParams.gravity = Gravity.BOTTOM;
            windowParams.windowAnimations = R.style.PeaceDialogAnimation_BOTTOM;
        }
        // in any other case, show the dialog in center.
        else {
            windowParams.gravity = Gravity.CENTER;
            windowParams.y = 0;
            if (mFullscreen) {
                windowParams.windowAnimations = R.style.PeaceDialogAnimation_Fullscreen;
            } else {
                windowParams.windowAnimations = R.style.PeaceDialogAnimation;
            }
        }
    }

    private int selectLayout() {
        return R.layout.layout_peace_dialog;
    }

    protected void setupLayout(View parentPanel) {
        final ViewGroup topPanel = parentPanel.findViewById(R.id.topPanel);
        final ViewGroup contentPanel = parentPanel.findViewById(R.id.contentPanel);
        final LinearLayout buttonPanel = parentPanel.findViewById(R.id.buttonPanel);

        setupTitle(topPanel);
        setupContent(contentPanel);
        setupButtons(buttonPanel);

        if (!parentPanel.isInTouchMode()) {
            if (!requestFocusForContent(contentPanel)) {
                requestFocusForDefaultButton();
            }
        }

        if (mScrollView != null) {
            mScrollView.setVerticalFadingEdgeEnabled(true);
            mScrollView.setFadingEdgeLength(Dimen.dp2px(getContext(), 25));
            mScrollView.setVerticalScrollBarEnabled(false);
        }

        if (mAdapterView != null) {
            mAdapterView.setVerticalFadingEdgeEnabled(true);
            mAdapterView.setFadingEdgeLength(Dimen.dp2px(getContext(), 25));
            mAdapterView.setVerticalScrollBarEnabled(false);
        }
    }

    private boolean requestFocusForContent(View content) {
        return content != null && content.requestFocus();
    }

    private void requestFocusForDefaultButton() {
        if (mButtonPositive != null && mButtonPositive.getVisibility() == VISIBLE) {
            mButtonPositive.requestFocus();
        } else if (mButtonNegative != null && mButtonNegative.getVisibility() == VISIBLE) {
            mButtonNegative.requestFocus();
        } else if (mButtonNeutral != null && mButtonNeutral.getVisibility() == VISIBLE) {
            mButtonNeutral.requestFocus();
        }
    }

    protected void setupTitle(ViewGroup topPanel) {
        if (mCustomTitleView != null) {
            // Add the custom title view directly to the topPanel layout
            final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            topPanel.addView(mCustomTitleView, 0, lp);

            // remove the title template
            final View titleTemplate = topPanel.findViewById(R.id.title_template);
            topPanel.removeView(titleTemplate);
        } else {
            final ViewGroup titleTemplate = topPanel.findViewById(R.id.title_template);

            // Display the title if a title is supplied, else hide it.
            mTitleView = createTitleView(mContext);

            if (mTitleView != null) {
                titleTemplate.addView(mTitleView);

                if (TextUtils.isEmpty(mTitle)) {
                    // Hide the title view
                    mTitleView.setVisibility(GONE);
                }
            }
        }
    }

    protected TextView createTitleView(@NonNull Context context) {
        AppCompatTextView titleView = new AppCompatTextView(new ContextThemeWrapper(context, resolveTitleStyle(context)));
        titleView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        titleView.setText(mTitle);
        titleView.setTextAlignment(mTitleTextAlignment);
        if (mTitleTextAppearance != 0) {
            TextViewCompat.setTextAppearance(titleView, mTitleTextAppearance);
        }

        return titleView;
    }

    protected void setupContent(ViewGroup contentPanel) {
        mScrollView = contentPanel.findViewById(R.id.scrollView);
        mScrollView.setFocusable(false);


        View inflate = null;
        try {
            inflate = mDialog.getWindow().getLayoutInflater().inflate(mViewResId, mScrollView, false);
        } catch (Exception ignored) {}

        if (inflate != null) {
            mView = inflate;
        }

        final boolean hasCustomView = mView != null;

        if (hasCustomView) {
            ViewUtils.removeView(mView);
            mScrollView.addView(mView);
            return;
        }

        mMessageView = createMessageView(mContext);

        final boolean hasMessageText = mMessageView != null && !TextUtils.isEmpty(mMessage);
        final boolean hasAdapter = mAdapterView != null;

        if (hasMessageText) {
            mScrollView.addView(mMessageView);
        }
        if (hasAdapter) {
            contentPanel.addView(mAdapterView, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        }

        if (!hasMessageText && !hasAdapter) {
            contentPanel.setVisibility(GONE);
        }
    }

    @Nullable
    protected TextView createMessageView(@NonNull Context context) {
        AppCompatTextView messageView = new AppCompatTextView(new ContextThemeWrapper(context, R.style.PeaceDialogMessageStyle));
        messageView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        messageView.setTextAlignment(mMessageTextAlignment);
        messageView.setText(mMessage);

        messageView.measure(0, 0);
        int measuredWidth = messageView.getMeasuredWidth();
        int dialogMaxWidth = getDialogMaxWidth();

        if (mDialog.hasCustomWidth()) {
            messageView.getLayoutParams().width = mDialog.dialogWidth;
            messageView.measure(mDialog.dialogWidth, View.MeasureSpec.AT_MOST);
        } else if (measuredWidth > dialogMaxWidth) {
            messageView.getLayoutParams().width = dialogMaxWidth;
            messageView.measure(dialogMaxWidth, View.MeasureSpec.AT_MOST);
        }

        return messageView;
    }

    protected void setupButtons(LinearLayout buttonPanel) {
        mButtonsPanel = buttonPanel;
        buttonPanel.removeAllViews();

        int BIT_BUTTON_POSITIVE = 1;
        int BIT_BUTTON_NEGATIVE = 2;
        int BIT_BUTTON_NEUTRAL = 4;
        int whichButtons = 0;

        boolean hasNeutralText = !TextUtils.isEmpty(mButtonNeutralText);
        boolean hasNegativeText = !TextUtils.isEmpty(mButtonNegativeText);
        boolean hasPositiveText = !TextUtils.isEmpty(mButtonPositiveText);


        final boolean hasAllThreeButtons = hasNeutralText && hasNegativeText && hasPositiveText;
        setupButtonsOrientation(hasAllThreeButtons);

        final boolean isButtonDirectionStacked = mButtonsDirection == STACKED;
        buttonPanel.setOrientation(isButtonDirectionStacked ? VERTICAL : HORIZONTAL);
        buttonPanel.setMeasureWithLargestChildEnabled(!isButtonDirectionStacked);

        if (hasNeutralText) {
            mButtonNeutral = createButton(mContext, mButtonNeutralTextColor, focusOnNeutral);
            mButtonNeutral.setText(mButtonNeutralText);
            buttonPanel.addView(mButtonNeutral);

            whichButtons = whichButtons | BIT_BUTTON_NEUTRAL;

            if (hasNegativeText || hasPositiveText) makeSeparator(buttonPanel);
        }

        if (hasNegativeText) {
            mButtonNegative = createButton(mContext, mButtonNegativeTextColor, focusOnNegative);
            mButtonNegative.setText(mButtonNegativeText);
            buttonPanel.addView(mButtonNegative);

            whichButtons = whichButtons | BIT_BUTTON_NEGATIVE;

            if (hasPositiveText) makeSeparator(buttonPanel);
        }

        if (hasPositiveText) {
            mButtonPositive = createButton(mContext, mButtonPositiveTextColor, focusOnPositive);
            mButtonPositive.setText(mButtonPositiveText);
            buttonPanel.addView(mButtonPositive);

            whichButtons = whichButtons | BIT_BUTTON_POSITIVE;
        }

        final boolean hasButtons = whichButtons != 0;

        if (!hasButtons) {
            buttonPanel.setVisibility(GONE);
        }
    }

    private void setupButtonsOrientation(boolean hasAllThreeButtons) {
        if (mButtonsDirection != PeaceDialog.DIRECTION_AUTO) return;

        final boolean styleStacked = hasAllThreeButtons && !WindowUtils.isLandscapeMode(mContext);
        mButtonsDirection = styleStacked ? STACKED : SIDE_BY_SIDE;
    }

    private TextView createButton(Context context, @ColorInt int textColor, boolean focus) {
        AppCompatTextView button = new AppCompatTextView(new ContextThemeWrapper(context, R.style.PeaceDialogButtonStyle));

        int width = mButtonsDirection == SIDE_BY_SIDE ? WRAP_CONTENT : MATCH_PARENT;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, WRAP_CONTENT);
        params.weight = 1;
        button.setLayoutParams(params);

        //        Typeface font = ResourcesCompat.getFont(mContext, R.font.app_typeface_medium);
        button.setTypeface(button.getTypeface(), focus ? Typeface.BOLD : Typeface.NORMAL);

        if (textColor != 0) {
            button.setTextColor(textColor);
        } else if (focus) {
            button.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        button.setOnClickListener(mButtonClickHandler);
        return button;
    }

    /**
     * Create buttons separator based on the configurations and device orientation.
     */
    private void makeSeparator(ViewGroup buttonPanel) {
        View separator = new View(mContext);
        boolean isSideBySide = mButtonsDirection == SIDE_BY_SIDE;

        // Set separator dimension based on buttons direction.
        int width = isSideBySide ? Dimen.dp2px(getContext(), 1) : MATCH_PARENT;
        int height = isSideBySide ? MATCH_PARENT : Dimen.dp2px(getContext(), 1);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(width, height);
        // Set separator margin based on buttons direction.
        int margL = isSideBySide ? 0 : 30;
        int margT = isSideBySide ? 20 : 0;
        int margR = isSideBySide ? 0 : 30;
        int margB = isSideBySide ? 20 : 0;
        sepParams.setMargins(margL, margT, margR, margB);

        separator.setLayoutParams(sepParams);

        separator.setBackgroundColor(resolveButtonDividerColor(mContext));

        buttonPanel.addView(separator);
    }

    public int getDialogMaxWidth() {
        // Limit the width to the maximum of a fraction of screen width.
        // The multiplier fraction is coming from dimension resource based on device screen sizes and orientations.
        return (int) (Dimen.getWindowWidth(getContext()) * resolveDialogWidthMultiplier(mContext));
    }

    public int getDialogMaxHeight() {
        // Calculate a max height to limit dialog's height to.
        // Minus the height of the status bar and some offsets from the window height
        return Dimen.getWindowHeight(getContext()) - (Dimen.getStatusBarHeight(mContext) + Dimen.dp2px(getContext(), 10));
    }

    public int getDialogMinHeight() {
        return Dimen.dp2px(getContext(), 150);
    }

    /**
     * Get the title style resource.
     *
     * @return Returns the resolved title style resource.
     */
    @StyleRes
    protected int resolveTitleStyle(@NonNull Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowTitleStyle, outValue, true);
        return outValue.resourceId;
    }

    private int resolveButtonDividerColor(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.peaceDialogButtonsDividerColor, outValue, true);
        return outValue.data;
    }

    /**
     * Get the fraction based on device screen sizes and orientations to multiply with screen width.
     */
    protected float resolveDialogWidthMultiplier(@NonNull Context context) {
        int attr = WindowUtils.isLandscapeMode(context) ? android.R.attr.windowMinWidthMajor : android.R.attr.windowMinWidthMinor;
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue.getFraction(1, 1);
    }

    private ViewGroup resolvePanel(@Nullable View customPanel, @Nullable View defaultPanel) {
        if (customPanel == null) {
            // Inflate the default panel, if needed.
            if (defaultPanel instanceof ViewStub) {
                defaultPanel = ((ViewStub) defaultPanel).inflate();
            }
            return (ViewGroup) defaultPanel;
        }
        // Remove the default panel entirely.
        if (defaultPanel != null) {
            final ViewParent parent = defaultPanel.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(defaultPanel);
            }
        }
        // Inflate the custom panel, if needed.
        if (customPanel instanceof ViewStub) {
            customPanel = ((ViewStub) customPanel).inflate();
        }
        return (ViewGroup) customPanel;
    }

    public void setTitle(CharSequence title) {
        mTitle = title;

        if (mTitleView != null) {
            mTitleView.setText(title);
            mTitleView.setVisibility(TextUtils.isEmpty(title) ? GONE : VISIBLE);
        }

        mDialog.getWindow().setTitle(title);
    }

    /**
     * @see PeaceDialog.Builder#setCustomTitle(View)
     */
    public void setCustomTitle(View customTitleView) {
        mCustomTitleView = customTitleView;
    }

    public void setMessage(CharSequence message) {
        mMessage = message;
        if (mMessageView != null) {
            mMessageView.setText(message);
        }
    }

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        mView = view;
    }

    public void setView(@LayoutRes int viewId) {
        mViewResId = viewId;
    }

    public View getMessageView() {
        return mMessageView;
    }

    public void setAdapterView(View view) {
        mAdapterView = view;
    }

    public void setButton(int whichButton, CharSequence text, @ColorInt int textColor, @Nullable Message msg, @Nullable OnClickListener listener) {
        if (msg == null && listener != null) {
            msg = mHandler.obtainMessage(whichButton, listener);
        }
        switch (whichButton) {
            case BUTTON_POSITIVE:
                mButtonPositiveText = text;
                mButtonPositiveMessage = msg;
                mButtonPositiveTextColor = textColor;
                break;
            case BUTTON_NEGATIVE:
                mButtonNegativeText = text;
                mButtonNegativeMessage = msg;
                mButtonNegativeTextColor = textColor;
                break;
            case BUTTON_NEUTRAL:
                mButtonNeutralText = text;
                mButtonNeutralMessage = msg;
                mButtonNeutralTextColor = textColor;
                break;
            default:
                throw new IllegalArgumentException("Button does not exist");
        }

        if (mDialog.isShowing() && mButtonsPanel != null) {
            setupButtons(mButtonsPanel);
        }
    }

    public void setButtonEnabled(int whichButton, boolean enabled) {
        switch (whichButton) {
            case BUTTON_POSITIVE:
                setButtonEnabledInternal(mButtonPositive, enabled);
                break;
            case BUTTON_NEGATIVE:
                setButtonEnabledInternal(mButtonNegative, enabled);
                break;
            case BUTTON_NEUTRAL:
                setButtonEnabledInternal(mButtonNeutral, enabled);
                break;
            default:
                throw new IllegalArgumentException("Button does not exist");
        }
    }

    private void setButtonEnabledInternal(TextView button, boolean enabled) {
        if (button != null) {
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1f : 0.5f);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mScrollView != null && mScrollView.executeKeyEvent(event);
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mScrollView != null && mScrollView.executeKeyEvent(event);
    }

    /**
     * Set if the dialog should be display at whole screen, i.e., it will be of full width and full height.
     * Setting this value also changes corner radius to 0 of the default background.
     *
     * @param flag flag.
     */
    public void setFullScreen(boolean flag) {
        mFullscreen = flag;
    }

    public void setElevation(float elevation) {
        mElevation = elevation;
    }

    public void setDimAmount(float dimAmout) {
        mDimAmount = dimAmout;
    }

    /**
     * Set gravity of the dialog to show it on top, bottom or center.
     * This value is ignored if dialog is set to fullscreen via {@link #setFullScreen(boolean)}.
     * Setting this value preferred when the dialog's contents are not are big.
     *
     * @param gravity gravity of the dialog.
     */
    public void setDialogGravity(@DialogGravity int gravity) {
        mDialogGravity = gravity;
    }

    public void setTitleTextAlignment(int alignment) {
        mTitleTextAlignment = alignment;
    }

    public void setTitleTextAppearance(int appearance) {
        mTitleTextAppearance = appearance;
    }

    public void setMessageTextAlignment(int alignment) {
        mMessageTextAlignment = alignment;
    }

    /**
     * Set the orientation of the action buttons.
     *
     * @param orientation Orientation in which buttons should be shown.
     */
    public void setButtonsDirection(@DialogButtonsDirection int orientation) {
        mButtonsDirection = orientation;
    }

    /**
     * If it is set to true, the button text style will bold and its color will be different.
     *
     * @param flag flag
     */
    public void setFocusOnPositive(boolean flag) {
        focusOnPositive = flag;
    }

    /**
     * If it is set to true, the button text style will bold and its color will be different.
     *
     * @param flag flag
     */
    public void setFocusOnNegative(boolean flag) {
        focusOnNegative = flag;
    }

    /**
     * If it is set to true, the button text style will bold and its color will be different.
     *
     * @param flag flag
     */
    public void setFocusOnNeutral(boolean flag) {
        focusOnNeutral = flag;
    }

    /**
     * Set if dialog should be dismissed on Positive button click.
     *
     * @param flag flag
     */
    public void setDismissOnPositive(boolean flag) {
        dismissOnPositive = flag;
    }

    /**
     * Set if dialog should be dismissed on Negative button click.
     *
     * @param flag flag
     */
    public void setDismissOnNegative(boolean flag) {
        dismissOnNegative = flag;
    }

    /**
     * Set if dialog should be dismissed on Neutral button click.
     *
     * @param flag flag
     */
    public void setDismissOnNeutral(boolean flag) {
        dismissOnNeutral = flag;
    }

    /**
     * Set if touch events should be sent to the layouts behind the dialog.
     */
    public void setAllowOutsideTouches(Boolean allow) {
        mAllowOutsideTouches = allow;
    }

    public PeaceDialog getDialog() {
        return mDialog;
    }

    public final Context getContext() {
        return mContext;
    }


    private static final class ButtonHandler extends Handler {
        private static final int MSG_DISMISS_DIALOG = 1;
        private final WeakReference<DialogInterface> mDialog;

        public ButtonHandler(DialogInterface dialog) {
            super(Looper.getMainLooper());
            mDialog = new WeakReference<>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BUTTON_POSITIVE:
                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                    ((OnClickListener) msg.obj).onClick(mDialog.get(), msg.what);
                    break;
                case MSG_DISMISS_DIALOG:
                    ((DialogInterface) msg.obj).dismiss();
                    break;
            }
        }
    }
}
