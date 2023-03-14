package com.quranapp.android.widgets.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.view.Window
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.peacedesign.R
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getWindowWidth

open class PeaceBottomSheetDialog constructor(
    context: Context,
    theme: Int,
    private val params: PeaceBottomSheetParams
) : BottomSheetDialog(context, theme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window ?: return

        val targetWidth = context.dp2px(450f)

        window.setLayout(
            if (context.getWindowWidth() > targetWidth) {
                targetWidth
            } else {
                LayoutParams.MATCH_PARENT
            },
            LayoutParams.MATCH_PARENT
        )

        setupDialogInternal(window)
    }

    protected fun setupDialogInternal(window: Window) {
        if (params.supportsNoAnimation()) {
            window.setWindowAnimations(R.style.SheetDialogAnimations_NoAnimations)
        } else {
            if (params.supportsEnterAnimationOnly()) {
                window.setWindowAnimations(R.style.SheetDialogAnimations_EnterOnly)
            } else if (params.supportsExitAnimationOnly()) {
                window.setWindowAnimations(R.style.SheetDialogAnimations_ExitOnly)
            }
        }

        window.setDimAmount(if (!params.supportsOverlayBackground) 0F else params.windowDimAmount)

        setCanceledOnTouchOutside(params.cancellable && !params.disableOutsideTouch)
    }
}
