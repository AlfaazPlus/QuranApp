package com.quranapp.android.frags.settings.recitations

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import com.quranapp.android.R
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.frags.settings.FragSettingsBase
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.widgets.PageAlert

abstract class FragSettingsRecitationsBase : FragSettingsBase() {
    protected lateinit var binding: FragSettingsTranslBinding
    protected lateinit var fileUtils: FileUtils
    private var pageAlert: PageAlert? = null

    override fun getFragTitle(ctx: Context): String? {
        return null
    }

    override val layoutResource = R.layout.frag_settings_transl

    @CallSuper
    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        fileUtils = FileUtils.newInstance(ctx)
        binding = FragSettingsTranslBinding.bind(view)
    }

    open fun refresh(context: Context, force: Boolean) {}

    open fun search(query: CharSequence) {}

    private fun initPageAlert(ctx: Context) {
        pageAlert = PageAlert(ctx)
    }

    protected fun noRecitersAvailable(ctx: Context) {
        showAlert(ctx, R.string.strMsgRecitationsNoAvailable, R.string.strLabelRefresh) {
            refresh(ctx, true)
        }
    }

    protected fun showLoader() {
        hideAlert()
        binding.loader.visibility = View.VISIBLE

        activity()?.header?.apply {
            setShowRightIcon(false)
            setShowSearchIcon(false)
        }
    }

    protected fun hideLoader() {
        binding.loader.visibility = View.GONE

        activity()?.header?.apply {
            setShowRightIcon(true)
            setShowSearchIcon(true)
        }
    }

    protected fun showAlert(ctx: Context, msgRes: Int, btnRes: Int, action: Runnable) {
        hideLoader()

        if (pageAlert == null) {
            initPageAlert(ctx)
        }

        pageAlert!!.apply {
            setIcon(null)
            setMessage("", ctx.getString(msgRes))
            setActionButton(btnRes, action)
            show(binding.container)
        }

        activity()?.header?.apply {
            setShowSearchIcon(false)
            setShowRightIcon(true)
        }
    }

    protected fun hideAlert() {
        pageAlert?.remove()
    }

    protected fun noInternet(ctx: Context) {
        if (pageAlert == null) {
            initPageAlert(ctx)
        }

        pageAlert!!.apply {
            setupForNoInternet { refresh(ctx, true) }
            show(binding.container)
        }

        activity()?.header?.apply {
            setShowSearchIcon(false)
            setShowRightIcon(true)
        }
    }
}