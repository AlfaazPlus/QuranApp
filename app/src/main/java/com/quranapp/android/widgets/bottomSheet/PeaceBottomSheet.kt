package com.quranapp.android.widgets.bottomSheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.utils.DrawableUtils
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.*

open class PeaceBottomSheet : BottomSheetDialogFragment() {
    var params = PeaceBottomSheetParams()
    var onPeaceBottomSheetShowListener: OnPeaceBottomSheetShowListener? = null
    var onDismissListener: OnPeaceBottomSheetDismissListener? = null

    private var dialogLayout: LinearLayout? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("params", params)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.PeaceBottomSheetTheme)

        if (savedInstanceState != null && savedInstanceState.containsKey("params")) {
            params = savedInstanceState.serializableExtra("params")!!
        }

        super.onCreate(savedInstanceState)

        if (params.sheetBGColor == -1) {
            params.sheetBGColor = requireContext().color(R.color.colorBackgroundSheetDialog)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): PeaceBottomSheetDialog {
        return PeaceBottomSheetDialog(requireContext(), theme, params)
    }

    protected open fun prepareDialogLayout(context: Context, params: PeaceBottomSheetParams): View {
        dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        setupHeader(dialogLayout!!, params)
        setupContentView(dialogLayout!!, params)

        return dialogLayout!!
    }

    private fun resolveTitle(context: Context) {
        if (params.headerTitleResource == 0) return

        params.headerTitle = params.headerTitle ?: context.getString(params.headerTitleResource)
    }

    protected open fun setupHeader(dialogLayout: ViewGroup, params: PeaceBottomSheetParams) {
        if (!this.params.headerShown) return

        resolveTitle(dialogLayout.context)

        val hasTitle = !params.headerTitle.isNullOrEmpty()
        if (!hasTitle && params.disableDragging) return

        val headerView = createHeaderView(dialogLayout)

        prepareDragIcon(headerView, params)
        prepareTitleView(headerView, params, false)

        if (hasTitle) {
            headerView.background = getHeaderBG(dialogLayout.context)
        }
    }

    private fun getHeaderBG(context: Context): Drawable {
        return context.drawable(R.drawable.dr_bg_sheet_dialog_header)
    }

    protected open fun prepareDragIcon(container: LinearLayout, params: PeaceBottomSheetParams) {
        if (params.disableDragging) return

        val dragIcon = AppCompatImageView(container.context).apply {
            id = R.id.dragIcon
            setImageResource(R.drawable.dr_icon_drag)
        }

        container.addView(
            dragIcon,
            0,
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                topMargin = Dimen.dp2px(container.context, 10f)
            }
        )
    }

    protected open fun prepareTitleView(
        container: LinearLayout,
        params: PeaceBottomSheetParams,
        isUpdating: Boolean
    ) {
        val hasTitle = !TextUtils.isEmpty(params.headerTitle)
        if (!isUpdating && !hasTitle) return

        var titleView: AppCompatTextView? = container.findViewById(R.id.title)

        if (hasTitle && titleView == null) {
            titleView = AppCompatTextView(
                ContextThemeWrapper(container.context, resolveTitleThemeId())
            )
            titleView.id = R.id.title
            container.addView(titleView)
        }

        titleView?.let {
            it.text = params.headerTitle
            it.visibility = if (hasTitle) View.VISIBLE else View.GONE
        }
    }

    fun updateHeaderTitle() {
        val layout = dialogLayout ?: return

        resolveTitle(dialogLayout!!.context)

        val hasTitle = !params.headerTitle.isNullOrEmpty()

        var headerView: LinearLayout? = layout.findViewById(R.id.peaceBottomSheetHeaderView)

        if (hasTitle && headerView == null) {
            headerView = createHeaderView(layout)
        }

        headerView?.let {
            prepareTitleView(it, params, true)
            it.background = if (hasTitle) getHeaderBG(layout.context) else null
        }
    }

    private fun createHeaderView(dialogLayout: ViewGroup): LinearLayout {
        val headerView = LinearLayout(dialogLayout.context).apply {
            id = R.id.peaceBottomSheetHeaderView
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        dialogLayout.addView(headerView, 0)

        return headerView
    }

    protected open fun resolveTitleThemeId(): Int {
        return R.style.PeaceBottomSheetTitleStyle
    }

    protected open fun setupContentView(dialogLayout: LinearLayout, params: PeaceBottomSheetParams) {
        if (params.contentView == null) {
            if (params.contentViewResId != 0) {
                params.contentView = LayoutInflater.from(context).inflate(
                    params.contentViewResId,
                    dialogLayout,
                    false
                )
            }
        }

        if (params.contentView != null) {
            params.contentView.removeView()
            dialogLayout.addView(params.contentView)
        }

        if (!params.headerShown && params.contentView != null) {
            val closeBtn: View? = params.contentView!!.findViewById(R.id.close)
            closeBtn?.setOnClickListener { dismiss() }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        setupDialogInternal(dialog, style, params)
    }

    protected open fun setupDialogInternal(
        dialog: Dialog,
        style: Int,
        params: PeaceBottomSheetParams
    ) {
        val dialogLayout = prepareDialogLayout(dialog.context, params)
        dialog.setContentView(dialogLayout)
        setupDialogStyles(dialog, dialogLayout, params)
    }

    protected open fun setupDialogStyles(
        dialog: Dialog,
        dialogLayout: View,
        P: PeaceBottomSheetParams
    ) {
        dialog.window?.decorView?.clipToOutline = true

        dialogLayout.clipToOutline = true
        (dialogLayout as ViewGroup).clipChildren = true

        val dialogModal = (dialogLayout.getParent() as ViewGroup).apply {
            clipToOutline = true
            clipChildren = true
        }

        setupFullHeight(dialogModal)
        if (!P.supportsRoundedCorners) {
            setupModalBackground(dialogModal, false)
        }

        val bottomSheetBehavior = BottomSheetBehavior.from<View>(dialogModal).apply {
            isHideable = P.cancellable && !P.hideOnSwipe
            isDraggable = P.cancellable && !P.disableDragging

            state = P.initialBehaviorState

            if (WindowUtils.isLandscapeMode(dialogLayout.context)) {
                skipCollapsed = true
            }
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                setupDialogOnStateChange(dialog, dialogModal, newState)

                dialog.currentFocus?.let {
                    ContextCompat.getSystemService(dialog.context, InputMethodManager::class.java)
                        ?.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                /*if (slideOffset <= 0) {
                    float mult = 1 + slideOffset;
                    window.setDimAmount(P.windowDimAmount * mult);
                }*/
            }
        })

        dialog.setOnKeyListener { dialogInterface, keyCode, event ->
            onKey(
                dialogInterface as BottomSheetDialog,
                keyCode,
                event
            )
        }

        dialog.setOnShowListener {
            onPeaceBottomSheetShowListener?.onShow()
            setupDialogOnStateChange(dialog, dialogModal, P.initialBehaviorState)
        }

        dialogModal.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            setupDialogOnStateChange(
                dialog,
                dialogModal,
                bottomSheetBehavior.state
            )
        }
    }

    protected open fun onKey(dialog: BottomSheetDialog, keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    private fun setupFullHeight(modal: View) {
        if (params.fullHeight) {
            modal.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            modal.requestLayout()
        }
    }

    private fun setupModalBackground(modal: View, isOnFullHeight: Boolean) {
        var radii: FloatArray? = null

        if (params.supportsRoundedCorners && !(params.resetRoundedCornersOnFullHeight && isOnFullHeight)) {
            radii = Dimen.createRadiiForBGInDP(context, 15f, 15f, 0f, 0f)
        }

        modal.background = DrawableUtils.createBackground(params.sheetBGColor, radii)
    }

    private fun setupDialogOnStateChange(dialog: Dialog, dialogModal: View, newState: Int) {
        val isExpanded = newState == BottomSheetBehavior.STATE_EXPANDED
        val isHeightFilled = dialogModal.height >= (dialogModal.context.getWindowHeight() - 10)
        val isOnFullHeight = isExpanded && isHeightFilled

        setupModalBackground(dialogModal, isOnFullHeight)

        if (!params.resetRoundedCornersOnFullHeight) return

        val window = dialog.window?.apply {
            setDimAmount(if (isOnFullHeight) 0F else params.windowDimAmount)
            statusBarColor = if (isOnFullHeight) params.sheetBGColor else 0
        }

        if (!WindowUtils.isNightMode(dialogModal.context)) {
            if (isOnFullHeight) {
                WindowUtils.setLightStatusBar(window!!)
            } else {
                WindowUtils.clearLightStatusBar(window!!)
            }
        }
    }

    fun show(fragmentManager: FragmentManager) {
        if (isShowing()) return

        show(fragmentManager, javaClass.simpleName)
    }

    override fun setCancelable(cancelable: Boolean) {
        super.setCancelable(cancelable)
        params.cancellable = cancelable
    }

    fun getDialogLayout(): LinearLayout {
        return dialogLayout!!
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (onDismissListener != null) onDismissListener!!.onDismissed()
    }

    fun isShowing() = isAdded

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (ignored: Exception) {
        }
    }

    interface OnPeaceBottomSheetShowListener {
        fun onShow()
    }

    interface OnPeaceBottomSheetDismissListener {
        fun onDismissed()
    }
}
