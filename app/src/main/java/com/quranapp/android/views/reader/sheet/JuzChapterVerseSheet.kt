package com.quranapp.android.views.reader.sheet

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.peacedesign.android.widget.sheet.PeaceBottomSheet

class JuzChapterVerseSheet : PeaceBottomSheet() {
    override fun onCreate(savedInstanceState: Bundle?) {
        dialogParams.apply {
            initialBehaviorState = BottomSheetBehavior.STATE_EXPANDED
            disableDragging = true
            fullHeight = false
            windowDimAmount = .9F
        }

        super.onCreate(savedInstanceState)
    }


}