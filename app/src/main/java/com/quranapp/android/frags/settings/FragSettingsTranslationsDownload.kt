package com.quranapp.android.frags.settings

import android.app.Activity
import android.content.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.adapters.transl.ADPDownloadTranslations
import com.quranapp.android.adapters.transl.ADPDownloadTranslationsGroup
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.interfaceUtils.TranslDownloadExplorerImpl
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.receivers.TranslDownloadReceiver
import com.quranapp.android.utils.receivers.TranslDownloadReceiver.TranslDownloadStateListener
import com.quranapp.android.utils.services.TranslationDownloadService
import com.quranapp.android.utils.services.TranslationDownloadService.LocalBinder
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import com.quranapp.android.widgets.PageAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException
import java.util.*

class FragSettingsTranslationsDownload :
    FragSettingsBase(),
    TranslDownloadStateListener,
    ServiceConnection,
    TranslDownloadExplorerImpl {

    private lateinit var binding: FragSettingsTranslBinding
    private lateinit var fileUtils: FileUtils
    private var translFactory: QuranTranslationFactory? = null
    private var adapter: ADPDownloadTranslationsGroup? = null
    private var translDownloadReceiver: TranslDownloadReceiver? = null
    private var translDownloadService: TranslationDownloadService? = null
    private var newTranslations: Array<String>? = null
    private var pageAlert: PageAlert? = null
    private var isRefreshInProgress = false

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleDownloadTranslations)

    override val layoutResource = R.layout.frag_settings_transl

    override fun onStart() {
        super.onStart()
        if (activity == null) return

        translDownloadReceiver = TranslDownloadReceiver().apply {
            setDownloadStateListener(this@FragSettingsTranslationsDownload)
           ContextCompat.registerReceiver(
               requireActivity(),
                this,
                IntentFilter(TranslDownloadReceiver.ACTION_TRANSL_DOWNLOAD_STATUS).apply {
                    addAction(TranslDownloadReceiver.ACTION_NO_MORE_DOWNLOADS)
                },
               ContextCompat.RECEIVER_NOT_EXPORTED
            )
            bindTranslService(requireActivity())
        }
    }

    private fun bindTranslService(actvt: Activity) {
        actvt.bindService(
            Intent(actvt, TranslationDownloadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun unbindTranslationService(actvt: Activity) {
        // if mTranslDownloadService is null, it means the service is already unbound
        // or it was not bound in the first place.
        if (translDownloadService == null) {
            return
        }
        try {
            actvt.unbindService(this)
        } catch (ignored: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()

        if (translDownloadReceiver != null && activity != null) {
            translDownloadReceiver!!.removeListener()
            requireActivity().unregisterReceiver(translDownloadReceiver)
            unbindTranslationService(requireActivity())
        }

        translFactory?.close()
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.setCallback(object : BoldHeaderCallback {
            override fun onBackIconClick() {
                activity.onBackPressedDispatcher.onBackPressed()
            }

            override fun onRightIconClick() {
                refreshTranslations(header.context, true)
            }
        })

        header.setShowSearchIcon(false)
        header.setShowRightIcon(false)
        header.setSearchHint(R.string.strHintSearchTranslation)
        header.setRightIconRes(
            R.drawable.dr_icon_refresh,
            activity.getString(R.string.strLabelRefresh)
        )
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        fileUtils = FileUtils.newInstance(ctx)
        translFactory = QuranTranslationFactory(ctx)
        binding = FragSettingsTranslBinding.bind(view)

        newTranslations = getArgs().getStringArray(TranslUtils.KEY_NEW_TRANSLATIONS)

        view.post { init(ctx) }
    }

    private fun init(ctx: Context) {
        refreshTranslations(ctx, SPAppActions.getFetchTranslationsForce(ctx))
    }

    private fun initPageAlert(ctx: Context) {
        pageAlert = PageAlert(ctx)
    }

    private fun refreshTranslations(ctx: Context, force: Boolean) {
        isRefreshInProgress = true
        showLoader()

        val storedAvailableDownloads = fileUtils.translsManifestFile

        if (force) {
            loadAvailableTranslations(ctx, storedAvailableDownloads)
        } else {
            if (!storedAvailableDownloads.exists()) {
                refreshTranslations(ctx, true)
                return
            }
            try {
                val data = storedAvailableDownloads.readText()
                if (data.isEmpty()) {
                    refreshTranslations(ctx, true)
                    return
                }
                parseAvailableTranslationsData(ctx, data)
            } catch (e: IOException) {
                e.printStackTrace()
                refreshTranslations(ctx, true)
                return
            }
        }
    }

    private fun loadAvailableTranslations(ctx: Context, storedAvailableDownloadsFile: File) {
        if (!NetworkStateReceiver.isNetworkConnected(ctx)) {
            hideLoader()
            noInternet(ctx)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val responseBody = RetrofitInstance.github.getAvailableTranslations()
                responseBody.string().let { data ->
                    fileUtils.createFile(storedAvailableDownloadsFile)
                    storedAvailableDownloadsFile.writeText(data)

                    runOnUIThread {
                        parseAvailableTranslationsData(ctx, data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                runOnUIThread {
                    showAlert(
                        R.string.strTitleOops,
                        R.string.strMsgTranslLoadFailed,
                        R.string.strLabelRetry
                    ) { refreshTranslations(ctx, true) }
                }
            }
        }
    }

    private fun parseAvailableTranslationsData(ctx: Context, data: String) {
        val obj = JsonHelper.json.parseToJsonElement(data).jsonObject
        obj["translations"]?.jsonObject?.let { translations ->
            val translationGroups = LinkedList<TranslationGroupModel>()

            val isAnyDownloadInProgress = translDownloadService?.isAnyDownloading() ?: false

            for (langCode in translations.keys) {
                val translationsForLanguageCode = translations[langCode]?.jsonObject
                val slugs = translationsForLanguageCode?.keys ?: continue

                val groupModel = TranslationGroupModel(langCode)
                val translationItems = ArrayList<TranslModel>()

                for (slug in slugs) {
                    if (isTranslationDownloaded(slug)) continue

                    val model = readTranslInfo(langCode, slug, translationsForLanguageCode[slug]!!.jsonObject)

                    if (newTranslations?.contains(slug) == true) {
                        model.addMiniInfo(ctx.getString(R.string.strLabelNew))
                    }

                    if (isAnyDownloadInProgress) {
                        model.isDownloadingDisabled = true
                    }

                    model.isDownloading = isTranslationDownloading(slug)

                    groupModel.langName = model.bookInfo.langName
                    groupModel.isExpanded = groupModel.isExpanded || model.isDownloading

                    translationItems.add(model)
                }

                // If no translation was added in this language category, skip
                if (translationItems.isEmpty()) continue

                groupModel.translations = translationItems
                translationGroups.add(groupModel)
            }

            if (translationGroups.isNotEmpty()) {
                populateTranslations(ctx, translationGroups)
            } else {
                noDownloadsAvailable(ctx)
            }
            SPAppActions.setFetchTranslationsForce(ctx, false)
        }

        isRefreshInProgress = false
        hideLoader()
    }

    private fun readTranslInfo(langCode: String, slug: String, translObject: JsonObject): TranslModel {
        val bookInfo = QuranTranslBookInfo(slug)
        bookInfo.langCode = langCode
        bookInfo.bookName = translObject["book"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.authorName = translObject["author"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.displayName = translObject["displayName"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.langName = translObject["langName"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.lastUpdated = translObject["lastUpdated"]?.jsonPrimitive?.longOrNull ?: -1
        bookInfo.downloadPath = translObject["downloadPath"]?.jsonPrimitive?.contentOrNull ?: ""
        return TranslModel(bookInfo)
    }

    private fun populateTranslations(ctx: Context, models: List<TranslationGroupModel>) {
        adapter = ADPDownloadTranslationsGroup(models, this)

        binding.list.let {
            it.layoutManager = LinearLayoutManager(ctx)
            (it.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            binding.list.adapter = adapter
        }

        activity()?.header?.apply {
            setShowRightIcon(true)
        }
    }

    private fun noDownloadsAvailable(ctx: Context) {
        showAlert(
            0,
            R.string.strMsgTranslNoDownloadsAvailable,
            R.string.strLabelRefresh
        ) {
            refreshTranslations(ctx, true)
        }
    }

    private fun isTranslationDownloaded(slug: String): Boolean {
        return translFactory?.isTranslationDownloaded(slug) == true
    }

    private fun isTranslationDownloading(slug: String): Boolean {
        return translDownloadService?.isDownloading(slug) ?: false
    }

    override fun onDownloadAttempt(
        adapter: ADPDownloadTranslations,
        vhTransl: ADPDownloadTranslations.VHDownloadTransl,
        referencedView: View,
        model: TranslModel
    ) {
        if (model.isDownloading) return
        val bookInfo = model.bookInfo

        if (isTranslationDownloading(bookInfo.slug)) {
            model.isDownloading = true
            adapter.notifyItemChanged(vhTransl.bindingAdapterPosition)
            return
        }

        val ctx = referencedView.context

        if (isTranslationDownloaded(bookInfo.slug)) {
            onTranslDownloadStatus(bookInfo, TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED)
            return
        }

        if (!NetworkStateReceiver.canProceed(ctx)) {
            return
        }

        PeaceDialog.newBuilder(ctx)
            .setTitle(R.string.strTitleDownloadTranslations)
            .setMessage("${bookInfo.bookName}\n${bookInfo.authorName}")
            .setPositiveButton(R.string.labelDownload) { _, _ ->
                this.adapter?.onDownloadStatus(bookInfo.slug, true)

                TranslationDownloadService.startDownloadService(ctx as ContextWrapper, bookInfo)
                activity?.let { bindTranslService(requireActivity()) }
            }
            .setNegativeButton(R.string.strLabelCancel, null)
            .show()
    }

    override fun onTranslDownloadStatus(bookInfo: QuranTranslBookInfo, status: String) {
        adapter?.onDownloadStatus(bookInfo.slug, false)

        val ctx = binding.root.context
        var title: String? = null
        var msg: String? = null
        when (status) {
            TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_CANCELED -> {
                translDownloadService?.cancelDownload(bookInfo.slug)
            }
            TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_FAILED -> {
                title = ctx.getString(R.string.strTitleFailed)
                msg = (
                        ctx.getString(R.string.strMsgTranslFailedToDownload, bookInfo.bookName) +
                                " " + ctx.getString(R.string.strMsgTryLater)
                        )
            }

            TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED -> {
                title = ctx.getString(R.string.strTitleSuccess)
                msg = ctx.getString(R.string.strMsgTranslDownloaded, bookInfo.bookName)
                adapter!!.remove(bookInfo.slug)
            }
        }

        if (title != null && context != null) {
            MessageUtils.popMessage(ctx, title, msg, ctx.getString(R.string.strLabelClose), null)
        }
    }

    override fun onNoMoreDownloads() {
        if (activity != null) {
            unbindTranslationService(requireActivity())
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        translDownloadService = (service as LocalBinder).service
        if (!isRefreshInProgress) {
            refreshTranslations(binding.root.context, false)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        translDownloadService = null
    }

    private fun showLoader() {
        hideAlert()
        binding.loader.visibility = View.VISIBLE
        if (activity is ActivitySettings) {
            val header = (activity as ActivitySettings?)!!.header
            header.setShowRightIcon(false)
        }
    }

    private fun hideLoader() {
        binding.loader.visibility = View.GONE
        if (activity is ActivitySettings) {
            val header = (activity as ActivitySettings?)!!.header
            header.setShowRightIcon(true)
        }
    }

    private fun showAlert(titleRes: Int, msgRes: Int, btnRes: Int, action: Runnable) {
        hideLoader()
        val ctx = binding.root.context
        if (pageAlert == null) {
            initPageAlert(ctx)
        }
        pageAlert!!.let {
            it.setIcon(null as Drawable?)
            it.setMessage(if (titleRes > 0) ctx.getString(titleRes) else "", ctx.getString(msgRes))
            it.setActionButton(btnRes, action)
            it.show(binding.container)
            if (activity is ActivitySettings) {
                val header = (activity as ActivitySettings?)!!.header
                header.setShowRightIcon(true)
            }

            (activity as? ActivitySettings)?.header?.apply {
                setShowRightIcon(true)
            }
        }
    }

    private fun hideAlert() {
        pageAlert?.remove()
    }

    private fun noInternet(ctx: Context) {
        if (pageAlert == null) {
            initPageAlert(ctx)
        }

        pageAlert!!.let {
            it.setupForNoInternet { refreshTranslations(ctx, true) }
            it.show(binding.container)

            (activity as? ActivitySettings)?.header?.apply {
                setShowRightIcon(true)
            }
        }
    }
}
