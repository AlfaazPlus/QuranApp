package com.quranapp.android.utils.univ

import android.content.Context
import android.widget.Toast
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import java.lang.ref.WeakReference

object MessageUtils {
    private var toast: WeakReference<Toast>? = null
    fun showRemovableToast(context: Context, msgRes: Int, duration: Int) {
        showRemovableToast(context, context.getString(msgRes), duration)
    }

    fun showRemovableToast(context: Context?, msg: CharSequence?, duration: Int) {
        try {
            toast?.get()?.cancel()
        } catch (ignored: Exception) {
        }
        toast = WeakReference(Toast.makeText(context, msg, duration))
        toast!!.get()!!.show()
    }

    fun popNoInternetMessage(ctx: Context, cancelable: Boolean, runOnDismiss: Runnable?) {
        PeaceDialog.newBuilder(ctx).apply {
            setTitle(R.string.strTitleNoInternet)
            setMessage(R.string.strMsgNoInternetLong)
            setNeutralButton(R.string.strLabelClose, null)
            if (runOnDismiss != null) {
                setOnDismissListener { runOnDismiss.run() }
            }
            setCancelable(cancelable)
            setFocusOnNeutral(true)
            show()
        }
    }

    @JvmStatic
    fun popMessage(context: Context, title: String, msg: String, btn: String, action: Runnable?) {
        val builder = PeaceDialog.newBuilder(context)
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setNeutralButton(btn) { _, _ -> action?.run() }
        builder.setFocusOnNeutral(true)
        builder.show()
    }
}