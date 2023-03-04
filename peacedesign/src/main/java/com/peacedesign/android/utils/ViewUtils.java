package com.peacedesign.android.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class ViewUtils {
    public static void removeView(View view) {
        if (view == null) {
            return;
        }

        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }
}
