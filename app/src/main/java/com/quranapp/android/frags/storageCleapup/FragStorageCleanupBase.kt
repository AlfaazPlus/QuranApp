package com.quranapp.android.frags.storageCleapup

import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityStorageCleanup
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.views.BoldHeader

abstract class FragStorageCleanupBase : BaseFragment() {
    abstract fun getFragTitle(ctx: Context): String?

    protected fun activity(): ActivityStorageCleanup? = activity as? ActivityStorageCleanup

    @CallSuper
    open fun setupHeader(activity: ActivityStorageCleanup, header: BoldHeader) {
        header.setCallback { activity.onBackPressedDispatcher.onBackPressed() }
        header.setTitleText(getFragTitle(activity))
    }

    fun launchFrag(cls: Class<out FragStorageCleanupBase?>, args: Bundle? = null) {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.frags_container, cls, args, cls.simpleName)
            setReorderingAllowed(true)
            addToBackStack(cls.simpleName)
            commit()
        }
    }
}
