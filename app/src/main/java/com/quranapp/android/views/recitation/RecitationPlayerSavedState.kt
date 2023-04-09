package com.quranapp.android.views.recitation

import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState
import androidx.core.os.ParcelCompat
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerParams


class RecitationPlayerSavedState : BaseSavedState {
    var params = RecitationPlayerParams()

    constructor(superState: Parcelable?) : super(superState)

    private constructor(parcel: Parcel) : super(parcel) {
        params = ParcelCompat.readParcelable(
            parcel,
            RecitationPlayerParams::class.java.classLoader,
            RecitationPlayerParams::class.java
        )!!
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeParcelable(params, flags)
    }


    companion object CREATOR : Parcelable.Creator<RecitationPlayerSavedState> {
        override fun createFromParcel(parcel: Parcel): RecitationPlayerSavedState {
            return RecitationPlayerSavedState(parcel)
        }

        override fun newArray(size: Int): Array<RecitationPlayerSavedState?> {
            return arrayOfNulls(size)
        }
    }
}