package com.peacedesign.android.widget.sheet;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import androidx.annotation.FloatRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.State;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.peacedesign.R;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.WindowUtils;

import java.io.Serializable;

public class PeaceBottomSheet extends BottomSheetDialogFragment {
    private PeaceBottomSheetParams P;
    private OnPeaceBottomSheetShowListener mOnPeaceBottomSheetShowListener;
    private OnPeaceBottomSheetDismissListener mOnPeaceBottomSheetDismissListener;
    private LinearLayout mDialogLayout;

    public PeaceBottomSheet() {
        P = new PeaceBottomSheetParams();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("params", P);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.PeaceBottomSheetTheme);
        if (savedInstanceState != null) {
            P = (PeaceBottomSheetParams) savedInstanceState.getSerializable("params");
        }

        super.onCreate(savedInstanceState);
        if (P.sheetBGColor == -1) {
            P.sheetBGColor = ContextCompat.getColor(getContext(), R.color.colorBackgroundSheetDialog);
        }
    }

    @NonNull
    @Override
    public PeaceBottomSheetDialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new PeaceBottomSheetDialog(getContext(), getTheme(), P);
    }

    protected View prepareDialogLayout(Context context, PeaceBottomSheetParams params) {
        mDialogLayout = new LinearLayout(context);
        mDialogLayout.setOrientation(LinearLayout.VERTICAL);

        setupHeader(mDialogLayout, params);
        setupContentView(mDialogLayout, params);

        return mDialogLayout;
    }

    private void resolveTitle() {
        if (P.headerTitle == null) {
            P.headerTitle = getContext().getString(P.headerTitleResource);
        }
    }

    protected void setupHeader(ViewGroup dialogLayout, PeaceBottomSheetParams params) {
        if (!P.headerShown) {
            return;
        }

        resolveTitle();
        final boolean hasTitle = !TextUtils.isEmpty(params.headerTitle);
        if (!hasTitle && params.disableDragging) {
            return;
        }

        LinearLayout headerView = createHeaderView(dialogLayout);

        prepareDragIcon(headerView, params);
        prepareTitleView(headerView, params, false);

        if (hasTitle) {
            headerView.setBackground(getHeaderBG(dialogLayout.getContext()));
        }
    }

    private Drawable getHeaderBG(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.dr_bg_sheet_dialog_header);
    }

    protected void prepareDragIcon(LinearLayout container, PeaceBottomSheetParams params) {
        if (params.disableDragging) {
            return;
        }
        AppCompatImageView dragIcon = new AppCompatImageView(container.getContext());
        dragIcon.setId(R.id.dragIcon);
        dragIcon.setImageResource(R.drawable.dr_icon_drag);

        LayoutParams lp = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        lp.topMargin = Dimen.dp2px(container.getContext(), 10);
        container.addView(dragIcon, 0, lp);
    }

    protected void prepareTitleView(@NonNull LinearLayout container, PeaceBottomSheetParams params, boolean isUpdating) {
        boolean hasTitle = !TextUtils.isEmpty(params.headerTitle);
        if (!isUpdating && !hasTitle) {
            return;
        }

        AppCompatTextView titleView = container.findViewById(R.id.title);
        if (hasTitle && titleView == null) {
            titleView = new AppCompatTextView(new ContextThemeWrapper(container.getContext(), resolveTitleThemeId()));
            titleView.setId(R.id.title);
            container.addView(titleView);
        }

        if (titleView != null) {
            titleView.setText(params.headerTitle);
            titleView.setVisibility(hasTitle ? View.VISIBLE : View.GONE);
        }
    }

    public void updateHeaderTitle() {
        if (mDialogLayout == null) {
            return;
        }

        resolveTitle();
        final boolean hasTitle = !TextUtils.isEmpty(P.headerTitle);

        LinearLayout headerView = mDialogLayout.findViewById(R.id.peaceBottomSheetHeaderView);
        if (hasTitle && headerView == null) {
            headerView = createHeaderView(mDialogLayout);
        }

        if (headerView != null) {
            prepareTitleView(headerView, P, true);
            headerView.setBackground(hasTitle ? getHeaderBG(mDialogLayout.getContext()) : null);
        }
    }

    private LinearLayout createHeaderView(ViewGroup dialogLayout) {
        LinearLayout headerView = new LinearLayout(dialogLayout.getContext());
        headerView.setId(R.id.peaceBottomSheetHeaderView);
        headerView.setOrientation(LinearLayout.VERTICAL);
        headerView.setGravity(Gravity.CENTER);
        dialogLayout.addView(headerView, 0);
        return headerView;
    }

    protected int resolveTitleThemeId() {
        return R.style.PeaceBottomSheetTitleStyle;
    }

    protected void setupContentView(@NonNull LinearLayout dialogLayout, PeaceBottomSheetParams params) {
        if (params.mContentView == null) {
            if (params.mContentViewResId != 0) {
                params.mContentView = LayoutInflater.from(getContext()).inflate(params.mContentViewResId, dialogLayout, false);
            }
        }

        if (params.mContentView != null) {
            ViewUtils.removeView(params.mContentView);
            dialogLayout.addView(params.mContentView);
        }

        if (!params.headerShown && params.mContentView != null) {
            View closeBtn = params.mContentView.findViewById(R.id.close);
            if (closeBtn != null) {
                closeBtn.setOnClickListener(v -> dismiss());
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        setupDialogInternal(dialog, style, P);
    }

    protected void setupDialogInternal(Dialog dialog, int style, PeaceBottomSheetParams params) {
        View dialogLayout = prepareDialogLayout(getContext(), params);
        dialog.setContentView(dialogLayout);

        setupDialogStyles(dialog, dialogLayout, params);
    }

    protected void setupDialogStyles(Dialog dialog, View dialogLayout, PeaceBottomSheetParams P) {
        Window window = dialog.getWindow();
        window.getDecorView().setClipToOutline(true);

        dialogLayout.setClipToOutline(true);
        ((ViewGroup) dialogLayout).setClipChildren(true);

        ViewGroup dialogModal = (ViewGroup) dialogLayout.getParent();
        dialogModal.setClipToOutline(true);
        dialogModal.setClipChildren(true);

        setupFullHeight(dialogModal);
        if (!P.supportsRoundedCorners) {
            setupModalBackground(dialogModal, false);
        }

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(dialogModal);
        bottomSheetBehavior.setHideable(P.cancellable && !P.hideOnSwipe);
        bottomSheetBehavior.setDraggable(P.cancellable && !P.disableDragging);
        bottomSheetBehavior.setState(P.initialBehaviorState);
        if (WindowUtils.isLandscapeMode(getContext())) {
            bottomSheetBehavior.setSkipCollapsed(true);
        }
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                setupDialogOnStateChange(dialog, dialogModal, newState);

                View focus = dialog.getCurrentFocus();
                if (focus != null) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                /*if (slideOffset <= 0) {
                    float mult = 1 + slideOffset;
                    window.setDimAmount(P.windowDimAmount * mult);
                }*/
            }
        });

        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> onKey((BottomSheetDialog) dialogInterface, keyCode, event));
        dialog.setOnShowListener(dialogInterface -> {
            if (mOnPeaceBottomSheetShowListener != null) mOnPeaceBottomSheetShowListener.onShow();
            setupDialogOnStateChange(dialog, dialogModal, P.initialBehaviorState);
        });

        dialogModal.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            setupDialogOnStateChange(dialog, dialogModal, bottomSheetBehavior.getState());
        });
    }

    protected boolean onKey(@NonNull BottomSheetDialog dialog, int keyCode, @NonNull KeyEvent event) {
        return false;
    }

    private void setupFullHeight(View modal) {
        if (P.fullHeight) {
            ViewGroup.LayoutParams params = modal.getLayoutParams();
            params.height = MATCH_PARENT;
            modal.setLayoutParams(params);
        }
    }

    private void setupModalBackground(View modal, boolean isOnFullHeight) {
        float[] radii = null;

        if (P.supportsRoundedCorners) {
            boolean cornerFlag = P.resetRoundedCornersOnFullHeight && isOnFullHeight;
            if (!cornerFlag) {
                radii = Dimen.createRadiiForBGInDP(getContext(), 15, 15, 0, 0);
            }
        }

        Drawable background = DrawableUtils.createBackground(P.sheetBGColor, radii);
        modal.setBackground(background);
    }

    private void setupDialogOnStateChange(Dialog dialog, View dialogModal, int newState) {
        boolean isExpanded = newState == BottomSheetBehavior.STATE_EXPANDED;
        boolean isHeightFilled = dialogModal.getHeight() >= (Dimen.getWindowHeight(getContext()) - 10);
        boolean isOnFullHeight = isExpanded && isHeightFilled;

        setupModalBackground(dialogModal, isOnFullHeight);

        if (!P.resetRoundedCornersOnFullHeight) {
            return;
        }

        Window window = dialog.getWindow();
        window.setDimAmount(isOnFullHeight ? 0 : P.windowDimAmount);
        window.setStatusBarColor(isOnFullHeight ? P.sheetBGColor : 0);

        if (!WindowUtils.isNightMode(getContext())) {
            if (isOnFullHeight) {
                WindowUtils.setLightStatusBar(window);
            } else {
                WindowUtils.clearLightStatusBar(window);
            }
        }
    }

    public final void show(@NonNull FragmentManager fragmentManager) {
        show(fragmentManager, getClass().getSimpleName());
    }

    public void setContentView(@NonNull View contentView) {
        P.mContentView = contentView;
    }

    public void setContentView(@LayoutRes int layoutId) {
        P.mContentViewResId = layoutId;
    }

    public void setHeaderTitle(CharSequence title) {
        P.headerTitle = title;
    }

    public void setHeaderTitle(@StringRes int titleRes) {
        P.headerTitleResource = titleRes;
    }

    @Override
    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
        P.cancellable = cancelable;
    }

    public void disableDragging(boolean flag) {
        P.disableDragging = flag;
    }

    public void setHideOnSwipe(boolean flag) {
        P.hideOnSwipe = flag;
    }

    public void disableOutsideTouch(boolean flag) {
        P.disableOutsideTouch = flag;
    }

    public void disableBackKey(boolean flag) {
        P.disableBackKey = flag;
    }

    public void setInitialBehaviorState(@State int state) {
        P.initialBehaviorState = state;
    }

    public void setFullHeight(boolean flag) {
        P.fullHeight = flag;
    }

    public void setHeaderShown(boolean shown) {
        P.headerShown = shown;
    }

    @NonNull
    public PeaceBottomSheetParams getDialogParams() {
        return P;
    }

    public void applyParams(@NonNull PeaceBottomSheetParams params) {
        P = params;
    }

    public void setOnShowListener(@NonNull OnPeaceBottomSheetShowListener listener) {
        mOnPeaceBottomSheetShowListener = listener;
    }

    public void setOnDismissListener(@NonNull OnPeaceBottomSheetDismissListener listener) {
        mOnPeaceBottomSheetDismissListener = listener;
    }

    public LinearLayout getDialogLayout() {
        return mDialogLayout;
    }

    @NonNull
    @Override
    public Context getContext() {
        //noinspection ConstantConditions
        return super.getContext();
    }

    public int getSheetBGColor() {
        return P.sheetBGColor;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnPeaceBottomSheetDismissListener != null) mOnPeaceBottomSheetDismissListener.onDismissed();
    }

    public boolean isShowing() {
        return isAdded();
    }

    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (Exception ignored) {}
    }

    public interface OnPeaceBottomSheetShowListener {
        void onShow();
    }

    public interface OnPeaceBottomSheetDismissListener {
        void onDismissed();
    }

    public static class PeaceBottomSheetParams implements Serializable {
        public boolean disableDragging;
        public boolean hideOnSwipe;
        public boolean disableOutsideTouch;
        public boolean cancellable = true;
        public boolean disableBackKey;
        public boolean supportsRoundedCorners = true;
        public boolean resetRoundedCornersOnFullHeight = true;
        public boolean supportsOverlayBackground = true;
        public boolean supportsAnimations = true;
        public boolean supportsEnterAnimation = true;
        public boolean supportsExitAnimation = true;
        public boolean fullHeight;
        public boolean headerShown = true;
        public int sheetBGColor = -1;
        @FloatRange(from = 0f, to = 1f)
        public float windowDimAmount = 0.6f;
        @State
        public int initialBehaviorState = BottomSheetBehavior.STATE_EXPANDED;
        @Nullable
        public CharSequence headerTitle;
        @StringRes
        public int headerTitleResource;
        @Nullable
        protected transient View mContentView;
        @LayoutRes
        protected int mContentViewResId;

        public boolean supportsNoAnimation() {
            return !supportsAnimations || (!supportsEnterAnimation && !supportsExitAnimation);
        }

        public boolean supportsEnterAnimationOnly() {
            return supportsAnimations && supportsEnterAnimation && !supportsExitAnimation;
        }

        public boolean supportsExitAnimationOnly() {
            return supportsAnimations && !supportsEnterAnimation && supportsExitAnimation;
        }


        public void setContentView(@LayoutRes int viewResId) {
            mContentViewResId = viewResId;
        }

        public void setContentView(@NonNull View contentView) {
            mContentView = contentView;
        }
    }
}
