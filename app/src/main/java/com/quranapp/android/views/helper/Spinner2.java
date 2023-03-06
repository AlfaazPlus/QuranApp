package com.quranapp.android.views.helper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.PopupWindow;
import android.widget.SpinnerAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;

import com.quranapp.android.adapters.utility.DynamicWidthSpinnerAdapter;
import com.quranapp.android.adapters.utility.SpinnerAdapter2;

import java.lang.reflect.Field;

public class Spinner2 extends AppCompatSpinner {
    public Spinner2(@NonNull Context context) {
        super(context);
        init();
    }

    public Spinner2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Spinner2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
    }

    @Override
    public boolean performClick() {
        boolean bool = super.performClick();
        clipPopupToOutline();
        return bool;
    }

    private void clipPopupToOutline() {
        try {
            Class<?> superclass = getClass().getSuperclass();
            if (superclass == null) {
                return;
            }
            Field mPopupInAppCompatSpinner = superclass.getDeclaredField("mPopup");
            mPopupInAppCompatSpinner.setAccessible(true);

            Object lpw = mPopupInAppCompatSpinner.get(this);
            if (lpw == null) {
                return;
            }

            Class<?> classListPopupWindow = lpw.getClass().getSuperclass();
            if (classListPopupWindow == null) {
                return;
            }

            Field mPopupInListPopupWindow = classListPopupWindow.getDeclaredField("mPopup");
            mPopupInListPopupWindow.setAccessible(true);

            PopupWindow popupWindow = (PopupWindow) mPopupInListPopupWindow.get(lpw);
            if (popupWindow == null) {
                return;
            }
            ((View) popupWindow.getContentView().getParent()).setClipToOutline(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAdapterWithDynamicWidth(SpinnerAdapter adapter) {
        if (adapter instanceof SpinnerAdapter2<?>) {
            ((SpinnerAdapter2<?>) adapter).setSpinner(this);
        }
        setAdapter(new DynamicWidthSpinnerAdapter(this, adapter));
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter);
        if (adapter instanceof SpinnerAdapter2<?>) {
            ((SpinnerAdapter2<?>) adapter).setSpinner(this);
        }
    }

    public abstract static class SimplerSpinnerItemSelectListener implements OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }
}
