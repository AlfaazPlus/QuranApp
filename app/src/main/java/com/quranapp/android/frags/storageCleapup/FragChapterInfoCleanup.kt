package com.quranapp.android.frags.storageCleapup

import android.content.Context
import com.quranapp.android.R

class FragChapterInfoCleanup : FragStorageCleanupBase() {
    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.titleChapterInfo)

}