package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.adapters.tafsir.ADPTafsirGroup
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.components.tafsir.TafsirGroupModel
import com.quranapp.android.databinding.FragSettingsTafsirBinding
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import com.quranapp.android.widgets.PageAlert
import java.util.*

class FragSettingsTafsirs : FragSettingsBase() {

    private lateinit var binding: FragSettingsTafsirBinding
    private lateinit var fileUtils: FileUtils
    private lateinit var pageAlert: PageAlert

    private var initialTafsirKey: String? = null

    override fun getFragTitle(ctx: Context): String = ctx.getString(R.string.strTitleSelectTafsir)

    override val layoutResource = R.layout.frag_settings_tafsir

    override fun getFinishingResult(ctx: Context): Bundle? {
        if (SPReader.getSavedTafsirKey(ctx) != initialTafsirKey) {
            return bundleOf(ActivityReader.KEY_TAFSIR_CHANGED to true)
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
            })

            disableRightBtn(false)
            setRightIconRes(
                R.drawable.dr_icon_refresh,
                activity.getString(R.string.strLabelRefresh)
            )
        }
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        fileUtils = FileUtils.newInstance(ctx)
        initialTafsirKey = SPReader.getSavedTafsirKey(ctx)
        pageAlert = PageAlert(ctx)
        binding = FragSettingsTafsirBinding.bind(view).apply {
            list.layoutManager = LinearLayoutManager(ctx)
        }

        refresh(ctx, SPAppActions.getFetchTafsirsForce(ctx))
    }

    private fun refresh(ctx: Context, force: Boolean) {
        if (force && !NetworkStateReceiver.isNetworkConnected(ctx)) {
            noInternet(ctx)
        }

        showLoader()

        TafsirManager.prepare(ctx, force) {
            val models = TafsirManager.getModels()

            if (!models.isNullOrEmpty()) {
                parseAvailableTafsir(ctx, models)
            } else {
                noTafsirsAvailable(ctx)
            }

            hideLoader()
        }
    }

    private fun parseAvailableTafsir(ctx: Context, tafsirs: Map<String, List<TafsirInfoModel>>) {
        val savedTafsirKey = SPReader.getSavedTafsirKey(ctx)
        val tafsirGroups = LinkedList<TafsirGroupModel>()

        for (langCode in tafsirs.keys) {
            val groupModel = TafsirGroupModel(langCode)
            val tafsirModels = tafsirs[langCode] ?: continue
            if (tafsirModels.isEmpty()) continue

            var groupHasItemSelected = false

            for (model in tafsirModels) {
                model.isChecked = model.key == savedTafsirKey

                if (model.isChecked) {
                    groupHasItemSelected = true
                }
            }

            groupModel.tafsirs = tafsirModels
            groupModel.langName = tafsirModels[0].langName
            groupModel.isExpanded = groupHasItemSelected

            tafsirGroups.add(groupModel)
        }

        populateTafsirs(ctx, tafsirGroups)
    }

    private fun populateTafsirs(ctx: Context, models: List<TafsirGroupModel>) {
        binding.list.let {
            it.adapter = ADPTafsirGroup(models)
            it.layoutManager = LinearLayoutManager(ctx)
            (it.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        activity()?.header?.apply {
            setShowRightIcon(true)
        }
    }

    private fun noTafsirsAvailable(ctx: Context) {
        showAlert(ctx, 0, R.string.strMsgTafsirsNoAvailable, R.string.strLabelRefresh) {
            refresh(ctx, true)
        }
    }

    private fun showLoader() {
        hideAlert()
        binding.loader.visibility = View.VISIBLE

        activity()?.header?.apply {
            setShowRightIcon(false)
        }
    }

    private fun hideLoader() {
        binding.loader.visibility = View.GONE

        activity()?.header?.apply {
            setShowRightIcon(true)
        }
    }

    private fun showAlert(ctx: Context, titleRes: Int, msgRes: Int, btnRes: Int, action: Runnable) {
        hideLoader()

        pageAlert.apply {
            setIcon(null)
            setMessage(if (titleRes > 0) ctx.getString(titleRes) else "", ctx.getString(msgRes))
            setActionButton(btnRes, action)
            show(binding.container)
        }

        activity()?.header?.apply {
            setShowRightIcon(true)
        }
    }

    private fun hideAlert() {
        pageAlert.remove()
    }

    private fun noInternet(ctx: Context) {
        pageAlert.apply {
            setupForNoInternet { refresh(ctx, true) }
            show(binding.container)
        }

        activity()?.header?.apply {
            setShowRightIcon(true)
        }
    }
}
