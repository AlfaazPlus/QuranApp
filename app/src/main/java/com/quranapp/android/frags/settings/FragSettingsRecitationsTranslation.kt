package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.adapters.recitation.ADPRecitationTranslations
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import com.quranapp.android.widgets.PageAlert
import java.util.regex.Pattern

class FragSettingsRecitationsTranslation : FragSettingsBase() {

    private lateinit var binding: FragSettingsTranslBinding
    private lateinit var fileUtils: FileUtils

    private val adapter = ADPRecitationTranslations()
    private var models: List<RecitationTranslationInfoModel>? = null
    private var pageAlert: PageAlert? = null

    private var initialRecitation: String? = null

    override fun getFragTitle(ctx: Context): String = ctx.getString(R.string.strTitleSelectReciter)

    override val layoutResource = R.layout.frag_settings_transl

    override fun getFinishingResult(ctx: Context): Bundle? {
        if (initialRecitation != null && SPReader.getSavedRecitationSlug(ctx) != initialRecitation) {
            return bundleOf(ActivityReader.KEY_RECITER_CHANGED to true)
        }
        return null
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback(object : BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                override fun onRightIconClick() {
                    refresh(activity, true)
                }

                override fun onSearchRequest(searchBox: EditText, newText: CharSequence) {
                    search(newText)
                }
            })

            disableRightBtn(false)
            setSearchHint(R.string.strHintSearchReciter)
            setRightIconRes(
                R.drawable.dr_icon_refresh,
                activity.getString(R.string.strLabelRefresh)
            )
        }
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        fileUtils = FileUtils.newInstance(ctx)
        initialRecitation = SPReader.getSavedRecitationSlug(ctx)
        binding = FragSettingsTranslBinding.bind(view)

        init(ctx)
    }

    private fun init(ctx: Context) {
        refresh(ctx, SPAppActions.getFetchRecitationTranslationsForce(ctx))
    }

    private fun initPageAlert(ctx: Context) {
        pageAlert = PageAlert(ctx)
    }

    private fun refresh(ctx: Context, force: Boolean) {
        if (force && !NetworkStateReceiver.isNetworkConnected(ctx)) {
            noInternet(ctx)
        }

        showLoader()

        RecitationManager.prepareTranslations(ctx, force) {
            val models = RecitationManager.getTranslationModels()

            if (!models.isNullOrEmpty()) {
                populateRecitations(ctx, models)
            } else {
                noRecitersAvailable(ctx)
            }

            hideLoader()
        }
    }

    private fun search(query: CharSequence) {
        val models = models ?: return

        if (query.isEmpty()) {
            if (adapter.itemCount != models.size) {
                resetAdapter(models)
            }
            return
        }
        val pattern = Pattern.compile(
            StringUtils.escapeRegex(query.toString()),
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )

        val found = ArrayList<RecitationTranslationInfoModel>()
        for (model in models) {
            if (pattern.matcher(model.reciter).find() || pattern.matcher(model.getReciterName()).find()) {
                found.add(model)
            }
        }

        resetAdapter(found)
    }

    private fun populateRecitations(ctx: Context, models: List<RecitationTranslationInfoModel>) {
        this.models = models

        binding.list.layoutManager = LinearLayoutManager(ctx)
        (binding.list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        resetAdapter(models)

        activity()?.header?.apply {
            setShowSearchIcon(true)
            setShowRightIcon(true)
        }
    }

    private fun resetAdapter(models: List<RecitationTranslationInfoModel>) {
        adapter.setModels(models)
        binding.list.adapter = adapter
    }

    private fun noRecitersAvailable(ctx: Context) {
        showAlert(ctx, R.string.strMsgRecitationsNoAvailable, R.string.strLabelRefresh) {
            refresh(ctx, true)
        }
    }

    private fun showLoader() {
        hideAlert()
        binding.loader.visibility = View.VISIBLE

        activity()?.header?.apply {
            setShowRightIcon(false)
            setShowSearchIcon(false)
        }
    }

    private fun hideLoader() {
        binding.loader.visibility = View.GONE

        activity()?.header?.apply {
            setShowRightIcon(true)
            setShowSearchIcon(true)
        }
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

        activity()?.header?.apply {
            setShowSearchIcon(false)
            setShowRightIcon(true)
        }
    }

    private fun hideAlert() {
        pageAlert?.remove()
    }

    private fun noInternet(ctx: Context) {
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
