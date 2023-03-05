package com.quranapp.android.frags.settings

import android.app.Activity
import android.content.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.adapters.transl.ADPDownloadTransls
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo
import com.quranapp.android.components.transls.TranslBaseModel
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslTitleModel
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.interfaceUtils.TranslDownloadExplorerImpl
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslFactory
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.receivers.TranslDownloadReceiver
import com.quranapp.android.utils.receivers.TranslDownloadReceiver.TranslDownloadStateListener
import com.quranapp.android.utils.services.TranslationDownloadService
import com.quranapp.android.utils.services.TranslationDownloadService.TranslationDownloadServiceBinder
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.utils.univ.StringUtils
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
import java.util.regex.Pattern

class FragSettingsTranslationsDownload : FragSettingsBase(), TranslDownloadStateListener,
    ServiceConnection, TranslDownloadExplorerImpl {

    private lateinit var mBinding: FragSettingsTranslBinding
    private lateinit var mFileUtils: FileUtils
    private var mTranslFactory: QuranTranslFactory? = null
    private var mAdapter: ADPDownloadTransls? = null
    private var mTranslDownloadReceiver: TranslDownloadReceiver? = null
    private var mTranslDownloadService: TranslationDownloadService? = null
    private var mNewTranslations: Array<String>? = null
    private var mPageAlert: PageAlert? = null

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleDownloadTranslations)

    override val layoutResource = R.layout.frag_settings_transl

    override fun onStart() {
        super.onStart()
        if (activity == null) return

        mTranslDownloadReceiver = TranslDownloadReceiver().apply {
            setDownloadStateListener(this@FragSettingsTranslationsDownload)
            requireActivity().registerReceiver(
                this,
                IntentFilter(TranslDownloadReceiver.ACTION_TRANSL_DOWNLOAD_STATUS).apply {
                    addAction(TranslDownloadReceiver.ACTION_NO_MORE_DOWNLOADS)
                }
            )
            bindTranslService(requireActivity())
        }
    }

    private fun bindTranslService(actvt: Activity) {
        actvt.bindService(Intent(actvt, TranslationDownloadService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    private fun unbindTranslationService(actvt: Activity) {
        // if mTranslDownloadService is null, it means the service is already unbound
        // or it was not bound in the first place.
        if (mTranslDownloadService == null) {
            return
        }
        try {
            actvt.unbindService(this)
        } catch (ignored: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()

        if (mTranslDownloadReceiver != null && activity != null) {
            mTranslDownloadReceiver!!.removeListener()
            requireActivity().unregisterReceiver(mTranslDownloadReceiver)
            unbindTranslationService(requireActivity())
        }

        mTranslFactory?.close()
    }


    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.setCallback(object : BoldHeaderCallback {
            override fun onBackIconClick() {
                activity.onBackPressed()
            }

            override fun onRightIconClick() {
                refreshTranslations(header.context, true)
            }

            override fun onSearchRequest(searchBox: EditText, newText: CharSequence) {
                search(newText)
            }
        })
        header.setShowSearchIcon(false)
        header.setShowRightIcon(false)
        header.setSearchHint(R.string.strHintSearchTranslation)
        header.setRightIconRes(R.drawable.dr_icon_refresh, activity.getString(R.string.strLabelRefresh))
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        mFileUtils = FileUtils.newInstance(ctx)
        mTranslFactory = QuranTranslFactory(ctx)
        mBinding = FragSettingsTranslBinding.bind(view)

        mNewTranslations = getArgs().getStringArray(TranslUtils.KEY_NEW_TRANSLATIONS)

        view.post { init(ctx) }
    }

    private fun init(ctx: Context) {
        refreshTranslations(ctx, SPAppActions.getFetchTranslationsForce(ctx))
    }

    private fun initPageAlert(ctx: Context) {
        mPageAlert = PageAlert(ctx)
    }


    private fun refreshTranslations(ctx: Context, force: Boolean) {
        showLoader()

        val storedAvailableDownloads = mFileUtils.translsManifestFile

        if (force) {
            loadAvailableTranslations(ctx, storedAvailableDownloads)
        } else {
            if (!storedAvailableDownloads.exists()) {
                refreshTranslations(ctx, true)
                return
            }
            try {
                val data = mFileUtils.readFile(storedAvailableDownloads)
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
                    mFileUtils.createFile(storedAvailableDownloadsFile)
                    mFileUtils.writeToFile(storedAvailableDownloadsFile, data)

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
            val translItems = LinkedList<TranslBaseModel>()

            for (langCode in translations.keys) {
                val translationsForLanguageCode = translations[langCode]?.jsonObject
                val slugs = translationsForLanguageCode?.keys ?: continue

                val translTitleModel = TranslTitleModel(langCode, null)
                translItems.add(translTitleModel)

                var traceAddedTranslCount = 0

                for (slug in slugs) {
                    val model = readTranslInfo(langCode, slug, translationsForLanguageCode[slug]!!.jsonObject)

                    if (mNewTranslations?.contains(slug) == true) {
                        model.addMiniInfo(ctx.getString(R.string.strLabelNew))
                    }

                    if (!isTranslationDownloaded(slug)) {
                        model.isDownloading = isTranslationDownloading(slug)
                        translTitleModel.langName = model.bookInfo.langName

                        translItems.add(model)
                        traceAddedTranslCount++
                    }
                }

                // If no translation was added in this language category, then remove the language title item
                if (traceAddedTranslCount == 0) {
                    translItems.removeLast()
                }
            }

            if (translItems.isNotEmpty()) {
                populateTranslations(ctx, translItems)
            } else {
                noDownloadsAvailable(ctx)
            }
            SPAppActions.setFetchTranslationsForce(ctx, false)
        }

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

    private fun search(query: CharSequence) {
        val adapter = mAdapter ?: return

        val storedModels = adapter.storedModels
        if (TextUtils.isEmpty(query)) {
            if (adapter.itemCount != adapter.storedItemCount) {
                adapter.setModels(storedModels)
                mBinding.list.adapter = adapter
            }
            return
        }
        val pattern = Pattern.compile(
            StringUtils.escapeRegex(query.toString()),
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        val found = ArrayList<TranslBaseModel>()
        for (model in storedModels) {
            if (model is TranslTitleModel) {
                found.add(model)
            }

            if (model !is TranslModel) continue

            val bookInfo = model.bookInfo
            val bookName = bookInfo.bookName
            val authorName = bookInfo.authorName
            val langName = bookInfo.langName

            if (bookName.isEmpty() && authorName.isEmpty() && langName.isEmpty()) {
                continue
            }

            if (pattern.matcher(bookName + authorName + langName).find()) {
                found.add(model)
            }
        }
        adapter.setModels(found)
        mBinding.list.adapter = adapter
    }

    private fun populateTranslations(ctx: Context, models: List<TranslBaseModel>) {
        mBinding.list.layoutManager = LinearLayoutManager(ctx)

        (mBinding.list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        mAdapter = ADPDownloadTransls(ctx, this, models)
        mBinding.list.adapter = mAdapter
        (activity as? ActivitySettings)?.header?.apply {
            setShowSearchIcon(true)
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
        return mTranslFactory?.isTranslationDownloaded(slug) == true
    }

    private fun isTranslationDownloading(slug: String): Boolean {
        return mTranslDownloadService?.isDownloading(slug) ?: false
    }

    override fun onDownloadAttempt(
        vhTransl: ADPDownloadTransls.VHDownloadTransl,
        referencedView: View,
        model: TranslModel
    ) {
        if (model.isDownloading) return

        val ctx = referencedView.context
        val bookInfo = model.bookInfo

        if (isTranslationDownloading(bookInfo.slug)) {
            model.isDownloading = true
            mAdapter?.notifyItemChanged(vhTransl.adapterPosition)
            return
        }

        if (isTranslationDownloaded(bookInfo.slug)) {
            onTranslDownloadStatus(bookInfo, TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED)
            return
        }

        if (!NetworkStateReceiver.canProceed(ctx)) {
            return
        }

        mAdapter?.onDownloadStatus(bookInfo.slug, true)

        TranslationDownloadService.startDownloadService(ctx as ContextWrapper, bookInfo)
        if (activity != null) {
            bindTranslService(requireActivity())
        }
    }

    override fun onTranslDownloadStatus(bookInfo: QuranTranslBookInfo, status: String) {
        mAdapter?.onDownloadStatus(bookInfo.slug, false)

        val ctx = mBinding.root.context
        var title: String? = null
        var msg: String? = null
        when (status) {
            TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_CANCELED -> {}
            TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_FAILED -> {
                title = ctx.getString(R.string.strTitleFailed)
                msg = (ctx.getString(R.string.strMsgTranslFailedToDownload, bookInfo.bookName)
                        + " " + ctx.getString(R.string.strMsgTryLater))
            }

            TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED -> {
                title = ctx.getString(R.string.strTitleSuccess)
                msg = ctx.getString(R.string.strMsgTranslDownloaded, bookInfo.bookName)
                mAdapter!!.remove(bookInfo.slug)
            }
        }

        if (title != null && context != null) {
            MessageUtils.popMessage(context, title, msg, ctx.getString(R.string.strLabelClose), null)
        }
    }

    override fun onNoMoreDownloads() {
        if (activity != null) {
            unbindTranslationService(requireActivity())
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        mTranslDownloadService = (service as TranslationDownloadServiceBinder).service
    }

    override fun onServiceDisconnected(name: ComponentName) {
        mTranslDownloadService = null
    }

    private fun showLoader() {
        hideAlert()
        mBinding.loader.visibility = View.VISIBLE
        if (activity is ActivitySettings) {
            val header = (activity as ActivitySettings?)!!.header
            header.setShowSearchIcon(false)
            header.setShowRightIcon(false)
        }
    }

    private fun hideLoader() {
        mBinding.loader.visibility = View.GONE
        if (activity is ActivitySettings) {
            val header = (activity as ActivitySettings?)!!.header
            header.setShowSearchIcon(true)
            header.setShowRightIcon(true)
        }
    }

    private fun showAlert(titleRes: Int, msgRes: Int, btnRes: Int, action: Runnable) {
        hideLoader()
        val ctx = mBinding.root.context
        if (mPageAlert == null) {
            initPageAlert(ctx)
        }
        mPageAlert!!.let {
            it.setIcon(null as Drawable?)
            it.setMessage(if (titleRes > 0) ctx.getString(titleRes) else "", ctx.getString(msgRes))
            it.setActionButton(btnRes, action)
            it.show(mBinding.container)
            if (activity is ActivitySettings) {
                val header = (activity as ActivitySettings?)!!.header
                header.setShowSearchIcon(false)
                header.setShowRightIcon(true)
            }

            (activity as? ActivitySettings)?.header?.apply {
                setShowSearchIcon(false)
                setShowRightIcon(true)
            }
        }
    }

    private fun hideAlert() {
        mPageAlert?.remove()
    }

    private fun noInternet(ctx: Context) {
        if (mPageAlert == null) {
            initPageAlert(ctx)
        }

        mPageAlert!!.let {
            it.setupForNoInternet { refreshTranslations(ctx, true) }
            it.show(mBinding.container)

            (activity as? ActivitySettings)?.header?.apply {
                setShowSearchIcon(false)
                setShowRightIcon(true)
            }
        }
    }
}