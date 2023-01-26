/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 11/3/2022.
 * All rights reserved.
 */

package com.peacedesign.android.widget.sheet;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.peacedesign.R;
import com.peacedesign.android.utils.Dimen;

public class PeaceBottomSheetDialog extends BottomSheetDialog {
    private final PeaceBottomSheet.PeaceBottomSheetParams P;

    public PeaceBottomSheetDialog(@NonNull Context context, int theme, PeaceBottomSheet.PeaceBottomSheetParams p) {
        super(context, theme);
        P = p;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window == null) {
            return;
        }

        int targetWidth = Dimen.dp2px(getContext(), 450);
        if (Dimen.getWindowWidth(getContext()) > targetWidth) {
            window.setLayout(targetWidth, MATCH_PARENT);
        } else {
            window.setLayout(MATCH_PARENT, MATCH_PARENT);
        }

        setupDialogInternal(window);
    }


    protected void setupDialogInternal(Window window) {
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

        setCanceledOnTouchOutside(P.cancellable && !P.disableOutsideTouch);
    }
}
