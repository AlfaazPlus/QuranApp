package com.quranapp.android.utils.univ;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.core.content.ContextCompat;

import com.quranapp.android.utils.extensions.ViewKt;

public class PopupWindow2 extends RelativePopupWindow {
    private float mDimAmount;
    private boolean mClipBackground;

    public void setDimBehind(float dimAmount) {
        mDimAmount = dimAmount;
    }

    public void setClipBackground(boolean clipBackground) {
        mClipBackground = clipBackground;
    }

    @Override
    public void setOverlapAnchor(boolean overlapAnchor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            super.setOverlapAnchor(overlapAnchor);
        }
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        if (parent.getWindowToken() == null) {
            return;
        }
        super.showAtLocation(parent, gravity, x, y);
        afterShow();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        super.showAsDropDown(anchor, xoff, yoff, gravity);
        afterShow();
    }

    private void afterShow() {
        if (mDimAmount > 0) {
            try {
                dimBehind();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mClipBackground) {
            clipBackground();
        }
    }

    private void clipBackground() {
        // To avoid menu item views to spread out of its parent, set clipping on th parent.
        // Since this cannot be set before the window is visible, this has to be done immediately after show method is called.
        if (getContentView() == null) return;
        ViewKt.clipChildren(getContentView(), true);
    }

    private void dimBehind() {
        View contentView = getContentView();
        if (contentView == null) {
            return;
        }

        WindowManager wm = ContextCompat.getSystemService(contentView.getContext(), WindowManager.class);
        if (wm == null) {
            return;
        }

        View decorView;
        if (getBackground() == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView = (View) contentView.getParent();
            } else {
                decorView = contentView;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView = (View) contentView.getParent().getParent();
            } else {
                decorView = (View) contentView.getParent();
            }
        }

        ViewGroup.LayoutParams params = decorView.getLayoutParams();
        if (params instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams p = (WindowManager.LayoutParams) params;
            p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            p.dimAmount = mDimAmount;
            wm.updateViewLayout(decorView, p);
        }
    }
}
