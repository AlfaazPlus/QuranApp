package com.quranapp.android.widgets.bottomSheet

import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.Serializable

class PeaceBottomSheetParams : Serializable {
    var disableDragging = false
    var hideOnSwipe = false
    var disableOutsideTouch = false
    var cancellable = true
    var disableBackKey = false
    var supportsRoundedCorners = true
    var resetRoundedCornersOnFullHeight = true
    var supportsOverlayBackground = true
    var supportsAnimations = true
    var supportsEnterAnimation = true
    var supportsExitAnimation = true
    var fullHeight = false

    /**
     * Whether to show the default header.
     */
    var headerShown = true
    var sheetBGColor = -1

    @FloatRange(from = 0.0, to = 1.0)
    var windowDimAmount = 0.6f

    @BottomSheetBehavior.State
    var initialBehaviorState = BottomSheetBehavior.STATE_EXPANDED
    var headerTitle: CharSequence? = null

    @StringRes
    var headerTitleResource = 0

    @Transient
    var contentView: View? = null

    @LayoutRes
    var contentViewResId = 0

    fun supportsNoAnimation(): Boolean {
        return !supportsAnimations || !supportsEnterAnimation && !supportsExitAnimation
    }

    fun supportsEnterAnimationOnly(): Boolean {
        return supportsAnimations && supportsEnterAnimation && !supportsExitAnimation
    }

    fun supportsExitAnimationOnly(): Boolean {
        return supportsAnimations && !supportsEnterAnimation && supportsExitAnimation
    }
}
