package com.quranapp.android.widgets.dialog.loader

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.peacedesign.android.widget.dialog.base.PeaceDialogController
import com.quranapp.android.R

class PeaceProgressDialog(context: Context) : PeaceDialog(context) {
    init {
        setCancelable(false)
    }

    override fun getController(context: Context, dialog: PeaceDialog): PeaceDialogController {
        return ProgressDialogController(context, dialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        view = View.inflate(context, R.layout.lyt_progress_dialog, null)
        setDialogWidth(ViewGroup.LayoutParams.WRAP_CONTENT)
        setFullScreen(false)
        setCanceledOnTouchOutside(false)
        setDialogGravity(GRAVITY_CENTER)
        super.onCreate(savedInstanceState)
    }

    fun setAllowOutsideTouches(allow: Boolean?) {
        controller.setAllowOutsideTouches(allow)
    }

    internal class ProgressDialogController(
        context: Context,
        dialog: PeaceDialog
    ) : PeaceDialogController(context, dialog) {
        override fun setupContent(contentPanel: ViewGroup) {
            super.setupContent(contentPanel)
            val messageView = contentPanel.findViewById<TextView>(R.id.message)
            messageView.text = mMessage
            messageView.visibility = if (!mMessage.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        override fun createTitleView(context: Context): TextView {
            val titleView = super.createTitleView(context)
            titleView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            return titleView
        }

        override fun createMessageView(context: Context): TextView? {
            val messageView = super.createMessageView(context)
            if (messageView != null) {
                messageView.setPadding(0, 0, 0, Dimen.dp2px(getContext(), 10f))
                messageView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            return messageView
        }

        override fun getDialogMaxWidth(): Int {
            return Dimen.dp2px(context, 200f)
        }

        override fun getDialogMinHeight(): Int {
            return 0
        }
    }
}
