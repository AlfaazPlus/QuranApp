package com.peacedesign.android.widget.sheet;

import static android.view.View.GONE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.peacedesign.android.widget.list.base.BaseListAdapter;
import com.peacedesign.android.widget.list.base.BaseListItem;
import com.peacedesign.android.widget.list.base.BaseListView;
import com.peacedesign.android.widget.list.simple.SimpleListView;
import com.peacedesign.android.widget.list.singleChoice.SingleChoiceListAdapter;
import com.peacedesign.android.widget.list.singleChoice.SingleChoiceListView;

/**
 * @deprecated Use {@link PeaceBottomSheet} instead.
 */
@Deprecated
public class SheetDialog extends BottomSheetDialogFragment {
    @ColorInt
    private int sheetBGColor;
    private View mDialogLayout;
    private SheetDialogParams P;
    protected TextView mTitleView;
    private OnSheetDialogShowListener mOnSheetDialogShowListener;
    private OnSheetDialogDismissListener mOnSheetDialogDismissListener;

    public SheetDialog() {
        P = new SheetDialogParams();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, R.style.SheetDialogTheme);
        sheetBGColor = ContextCompat.getColor(getContext(), R.color.colorBackgroundSheetDialog);
        mDialogLayout = View.inflate(getContext(), R.layout.layout_sheet_dialog, null);


        installContents();
    }

    private void installContents() {
        LinearLayout contentPanel = mDialogLayout.findViewById(R.id.contentPanel);
        ViewGroup headerPanel = mDialogLayout.findViewById(R.id.headerPanel);
        View sheetHeader = mDialogLayout.findViewById(R.id.sheetHeader);

        setupHeader(headerPanel, sheetHeader);
        setupContentView(contentPanel);
    }

    protected void setupContentView(@NonNull LinearLayout contentContainer) {
        if (P.mContentView == null) {
            if (P.mContentViewResId != 0) {
                P.mContentView = LayoutInflater.from(getContext()).inflate(P.mContentViewResId, contentContainer, false);
            }
        }

        if (P.mContentView != null) {
            ViewUtils.removeView(P.mContentView);
            contentContainer.addView(P.mContentView);
        }

        if (!P.headerShown && P.mContentView != null) {
            View closeBtn = P.mContentView.findViewById(R.id.close);
            if (closeBtn != null) {
                closeBtn.setOnClickListener(v -> dismiss());
            }
        }

        if (P.listAdapter != null) {
            View adapterView = createAdapterView(P.listAdapter);
            appendAdapterView(contentContainer, adapterView);
        }
    }

    @NonNull
    protected View createAdapterView(@NonNull BaseListAdapter<BaseListItem> listAdapter) {
        final BaseListView listView;
        if (listAdapter instanceof SingleChoiceListAdapter) {
            listView = new SingleChoiceListView(getContext());
        } else {
            listView = new SimpleListView(getContext());
        }
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(item -> {
            if (P.itemClickListener != null) {
                P.itemClickListener.onItemClick(this, item);
            }
        });
        return listView;
    }

    protected void appendAdapterView(@NonNull ViewGroup container, @NonNull View adapterView) {
        container.addView(adapterView);
    }

    private void setupHeader(ViewGroup headerPanel, View sheetHeader) {
        if (P.customHeader != null) {
            headerPanel.removeView(sheetHeader);

            ViewUtils.removeView(P.customHeader);
            headerPanel.addView(P.customHeader);
            return;
        }

        setupTitle(sheetHeader);

        setupHeaderVisibility(headerPanel);
    }

    private void setupTitle(View sheetHeader) {
        mTitleView = sheetHeader.findViewById(R.id.title);
        if (P.headerTitle == null) {
            P.headerTitle = getContext().getString(P.headerTitleResource);
        }

        if (P.headerTitle == null) {
            mTitleView.setVisibility(GONE);
            return;
        }

        mTitleView.setText(P.headerTitle);
        //        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.dmnPageBigTitleSize));
    }

    private void setupHeaderVisibility(View headerPanel) {
        final boolean noHeaderContent = mTitleView.getVisibility() == GONE;
        if (!P.headerShown || noHeaderContent) {
            headerPanel.setVisibility(GONE);
        }

        headerPanel.findViewById(R.id.dragIcon).setVisibility(P.disableDragging ? GONE : View.VISIBLE);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        setupDialogInternal(dialog, style);
    }

    protected void setupDialogInternal(Dialog dialog, int style) {
        dialog.setContentView(getDialogLayout());

        Window window = dialog.getWindow();

        if (P.supportsNoAnimation()) {
            window.setWindowAnimations(R.style.SheetDialogAnimations_NoAnimations);
        } else {
            if (P.supportsEnterAnimationOnly()) {
                window.setWindowAnimations(R.style.SheetDialogAnimations_EnterOnly);
            } else if (P.supportsExitAnimationOnly()) {
                window.setWindowAnimations(R.style.SheetDialogAnimations_ExitOnly);
            }
        }

        window.setDimAmount(!P.supportsOverlayBackground ? 0 : P.windowDimAmount);

        View dialogModal = getDialogModal();

        getDialogLayout().setClipToOutline(true);
        dialogModal.setClipToOutline(true);
        window.getDecorView().setClipToOutline(true);

        setupFullHeight(dialogModal);
        setupModalBackground(getDialogModal(), false);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(dialogModal);
        bottomSheetBehavior.setHideable(P.cancellable && !P.hideOnSwipe);
        bottomSheetBehavior.setDraggable(P.cancellable && !P.disableDragging);
        bottomSheetBehavior.setState(P.initialBehaviorState);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                setupDialogOnStateChange(dialogModal, newState);

                View focus = dialog.getCurrentFocus();
                if (focus != null) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        dialog.setCanceledOnTouchOutside(P.cancellable && !P.disableOutsideTouch);
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            //            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            //                if (!P.disableBackKey) dismiss();
            //            }
            return onKey((BottomSheetDialog) dialogInterface, keyCode, event);
        });
        dialog.setOnShowListener(dialogInterface -> {
            if (mOnSheetDialogShowListener != null) mOnSheetDialogShowListener.onShow();
            setupDialogOnStateChange(dialogModal, P.initialBehaviorState);
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

        Drawable background = DrawableUtils.createBackground(sheetBGColor, radii);
        modal.setBackground(background);
    }

    void setupDialogOnStateChange(View dialogModal, int newState) {
        boolean isExpanded = newState == BottomSheetBehavior.STATE_EXPANDED;
        boolean isHeightFilled = dialogModal.getHeight() >= (Dimen.getWindowHeight(getContext()) - 10);
        boolean isOnFullHeight = isExpanded && isHeightFilled;

        if (P.supportsRoundedCorners) {
            setupModalBackground(getDialogModal(), isOnFullHeight);

            if (!P.resetRoundedCornersOnFullHeight) {
                return;
            }

            Dialog dialog = getDialog();
            if (dialog == null) {
                return;
            }

            Window window = dialog.getWindow();
            window.setDimAmount(isOnFullHeight ? 0 : P.windowDimAmount);

            if (!WindowUtils.isNightMode(getContext())) {
                if (isOnFullHeight) {
                    WindowUtils.setLightStatusBar(window);
                } else {
                    WindowUtils.clearLightStatusBar(window);
                }
            }
        }
    }

    public final void show(@NonNull FragmentManager fragmentManager) {
        show(fragmentManager, null);
    }

    public void setContentView(@NonNull View contentView) {
        P.mContentView = contentView;
    }

    public void setContentView(@LayoutRes int layoutId) {
        P.mContentViewResId = layoutId;
    }

    @NonNull
    public View getDialogLayout() {
        return mDialogLayout;
    }

    @NonNull
    public View getDialogModal() {
        return (View) mDialogLayout.getParent();
    }

    public void setHeaderLeftIcon(@DrawableRes int leftIcon) {
        P.headerLeftIconResource = leftIcon;
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
    public SheetDialogParams getDialogParams() {
        return P;
    }

    public void applyParams(@NonNull SheetDialogParams params) {
        P = params;
    }

    public void setOnShowListener(@NonNull OnSheetDialogShowListener listener) {
        mOnSheetDialogShowListener = listener;
    }

    public void setOnDismissListener(@NonNull OnSheetDialogDismissListener listener) {
        mOnSheetDialogDismissListener = listener;
    }

    @NonNull
    @Override
    public Context getContext() {
        //noinspection ConstantConditions
        return super.getContext();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnSheetDialogDismissListener != null) mOnSheetDialogDismissListener.onDismissed();
    }

    public void setAdapter(@NonNull BaseListAdapter<BaseListItem> listAdapter) {
        P.listAdapter = listAdapter;
    }

    public void setOnItemClickListener(@NonNull OnItemClickListener listener) {
        P.itemClickListener = listener;
    }

    public boolean isShowing() {
        return isVisible();
    }


    public interface OnSheetDialogShowListener {
        void onShow();
    }

    public interface OnSheetDialogDismissListener {
        void onDismissed();
    }

    public interface OnItemClickListener {
        void onItemClick(@NonNull SheetDialog dialog, @NonNull BaseListItem item);
    }

    public static class SheetDialogParams {
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
        public View customHeader;
        @FloatRange(from = 0f, to = 1f)
        public float windowDimAmount = 0.6f;
        @State
        public int initialBehaviorState = BottomSheetBehavior.STATE_EXPANDED;
        @Nullable
        public CharSequence headerTitle;
        @StringRes
        public int headerTitleResource;
        @DrawableRes
        public int headerLeftIconResource = R.drawable.dr_icon_close;
        @Nullable
        public BaseListAdapter<BaseListItem> listAdapter;
        @Nullable
        public OnItemClickListener itemClickListener;
        @Nullable
        protected View mContentView;
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

    public static class SheetDialogBehavior<V extends View> extends BottomSheetBehavior<V> {

    }
}
