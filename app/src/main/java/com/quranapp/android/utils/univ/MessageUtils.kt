package com.quranapp.android.utils.univ

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.Toast
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.peacedesign.android.widget.dialog.base.PeaceDialog.DialogGravity
import com.quranapp.android.R
import com.quranapp.android.components.bookmark.BookmarkModel
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
    fun popMessage(context: Context, title: String, msg: String?, btn: String, action: Runnable?) {
        val builder = PeaceDialog.newBuilder(context)
        builder.setTitle(title)
        if (msg != null) builder.setMessage(msg)
        builder.setNeutralButton(btn) { _, _ -> action?.run() }
        builder.setFocusOnNeutral(true)
        builder.show()
    }

    @JvmStatic
    @JvmOverloads
    fun showConfirmationDialog(
        context: Context,
        title: Int,
        msg: Int? = null,
        btn: Int,
        btnColor: Int? = null,
        @DialogGravity gravity: Int = PeaceDialog.GRAVITY_CENTER,
        action: Runnable?
    ) {
        showConfirmationDialog(
            context,
            context.getString(title),
            msg?.let { context.getString(it) },
            context.getString(btn),
            btnColor,
            gravity,
            action
        )
    }

    @JvmStatic
    fun showConfirmationDialog(
        context: Context,
        title: CharSequence,
        msg: CharSequence? = null,
        btn: String,
        btnColor: Int? = null,
        @DialogGravity gravity: Int = PeaceDialog.GRAVITY_CENTER,
        action: Runnable?
    ) {
        PeaceDialog.newBuilder(context).apply {
            setTitle(title)
            setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
            setMessage(msg ?: "\n")
            setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
            setDialogGravity(gravity)
            setNeutralButton(R.string.strLabelCancel, null)
            setNegativeButton(btn, btnColor ?: 0) { _, _ -> action?.run() }
        }.show()
    }
}