package com.quranapp.android.frags.settings.recitations

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.quranapp.android.R
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.frags.settings.FragSettingsBase
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.widgets.PageAlert

abstract class FragSettingsRecitationsBase : BaseFragment() {
    companion object {
        const val KEY_IS_MANAGE_AUDIO = "key.is_manage_audio"
    }

    protected lateinit var binding: FragSettingsTranslBinding
    protected lateinit var fileUtils: FileUtils
    private var pageAlert: PageAlert? = null
    protected var isManageAudio = false
    var isRefreshing = false

    abstract fun getFinishingResult(ctx: Context): Bundle?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isManageAudio = getArgs().getBoolean(KEY_IS_MANAGE_AUDIO, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_settings_transl, container, false)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileUtils = FileUtils.newInstance(view.context)
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
        isRefreshing = true
        hideAlert()
        binding.loader.visibility = View.VISIBLE
    }

    protected fun hideLoader() {
        isRefreshing = false
        binding.loader.visibility = View.GONE
    }

    private fun showAlert(ctx: Context, msgRes: Int, btnRes: Int, action: Runnable) {
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
    }

    private fun hideAlert() {
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
    }

    fun launchFrag(cls: Class<out FragSettingsBase?>, args: Bundle?) {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.frags_container, cls, args, cls.simpleName)
            setReorderingAllowed(true)
            addToBackStack(cls.simpleName)
            commit()
        }
    }
}