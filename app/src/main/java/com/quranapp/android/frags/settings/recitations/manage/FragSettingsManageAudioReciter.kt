package com.quranapp.android.frags.settings.recitations.manage

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.util.valueIterator
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.adapters.recitation.ADPManageAudioChapters
import com.quranapp.android.api.models.recitation.RecitationInfoBaseModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.recitation.ManageAudioChapterModel
import com.quranapp.android.frags.settings.FragSettingsBase
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_CANCELED
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_FAILED
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_PROGRESS
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_SUCCEED
import com.quranapp.android.utils.services.RecitationChapterDownloadService
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.helper.RecyclerView2
import com.quranapp.android.widgets.dialog.loader.PeaceProgressDialog

class FragSettingsManageAudioReciter :
    FragSettingsBase(),
    RecitationChapterDownloadReceiver.DownloadStateListener,
    ServiceConnection {

    companion object {
        const val KEY_RECITATION_INFO_MODEL = "key.reciter_slug"
    }

    private var adapter: ADPManageAudioChapters? = null
    private lateinit var quranMeta: QuranMeta
    private lateinit var fileUtils: FileUtils
    private lateinit var recyclerView: RecyclerView
    private var infoModel: RecitationInfoBaseModel? = null
    private val models = ArrayList<ManageAudioChapterModel>()
    private var downloadReceiver: RecitationChapterDownloadReceiver? = null
    private var downloadService: RecitationChapterDownloadService? = null

    override fun getFragTitle(ctx: Context): String? {
        return infoModel?.getReciterName()
    }

    override fun onStart() {
        super.onStart()
        if (activity == null) return

        downloadReceiver = RecitationChapterDownloadReceiver().apply {
            stateListener = this@FragSettingsManageAudioReciter
           ContextCompat.registerReceiver(
               requireActivity(),
                this,
                IntentFilter(RecitationChapterDownloadReceiver.ACTION_RECITATION_DOWNLOAD_STATUS),
               ContextCompat.RECEIVER_NOT_EXPORTED
            )
            bindService(requireActivity())
        }
    }

    private fun bindService(actvt: Activity) {
        actvt.bindService(
            Intent(actvt, RecitationChapterDownloadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun unbindService(actvt: Activity) {
        // if mTranslDownloadService is null, it means the service is already unbound
        // or it was not bound in the first place.
        if (downloadService == null) {
            return
        }
        try {
            actvt.unbindService(this)
        } catch (ignored: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()

        if (downloadReceiver != null && activity != null) {
            downloadReceiver!!.stateListener = null
            requireActivity().unregisterReceiver(downloadReceiver)
            unbindService(requireActivity())
        }
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)

        header.apply {
            setCallback(object : BoldHeader.BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                override fun onSearchRequest(searchBox: EditText, newText: CharSequence) {
                    search(newText)
                }
            })

            setShowSearchIcon(true)
            setShowRightIcon(false)
            setSearchHint(R.string.strHintSearchChapter)
        }
    }

    override val layoutResource get() = 0

    override fun getFragView(ctx: Context): View {
        return RecyclerView2(ctx).apply {
            updatePadding(
                bottom = ctx.dp2px(50f)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        infoModel = getArgs().serializableExtra(KEY_RECITATION_INFO_MODEL)!!
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        val progressDialog = PeaceProgressDialog(ctx).apply {
            setAllowOutsideTouches(false)
            show()
        }

        recyclerView = view as RecyclerView
        initList(recyclerView)

        fileUtils = FileUtils.newInstance(ctx)

        val modelInfo = infoModel!!

        QuranMeta.prepareInstance(ctx, object : OnResultReadyCallback<QuranMeta> {
            override fun onReady(r: QuranMeta) {
                quranMeta = r
                models.addAll(r.chapterMetas.valueIterator().asSequence().map {
                    ManageAudioChapterModel(it, modelInfo).apply {
                        downloaded = checkIfDownloaded(modelInfo.slug, it, fileUtils)
                        downloading = downloadService?.isDownloading(modelInfo.slug, it.chapterNo) ?: false
                        if (downloading) {
                            progress = downloadService!!.getDownloadProgress(modelInfo.slug, it.chapterNo)
                        }
                        prepareTitle(ctx)
                    }
                }.toList())

                resetAdapter(models)
                progressDialog.dismiss()
            }
        })
    }

    private fun checkIfDownloaded(slug: String, it: QuranMeta.ChapterMeta, fileUtils: FileUtils): Boolean {
        var allVersesDownloaded = true

        for (verseNo in 1..it.verseCount) {
            if (fileUtils.getRecitationAudioFile(slug, it.chapterNo, verseNo).length() == 0L) {
                allVersesDownloaded = false
                break
            }
        }

        return allVersesDownloaded
    }

    private fun initList(list: RecyclerView) {
        list.layoutManager = LinearLayoutManager(list.context)
        (list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    private fun resetAdapter(chapters: List<ManageAudioChapterModel>) {
        adapter = ADPManageAudioChapters(this, chapters)
        recyclerView.adapter = adapter
    }

    private fun search(newText: CharSequence) {
        if (newText.isEmpty()) {
            resetAdapter(models)
            return
        }

        resetAdapter(models.filter {
            val chapterMeta = it.chapterMeta
            chapterMeta.name.contains(newText, true) ||
                    chapterMeta.tags.contains(newText, true) ||
                    chapterMeta.nameTranslation.contains(newText, true) ||
                    chapterMeta.chapterNo.toString().contains(newText, true)
        })
    }

    override fun onDownloadStatus(chapterModel: ManageAudioChapterModel, status: String, progress: Int) {
        if (chapterModel.reciterModel.slug != infoModel?.slug) return
        val ctx = recyclerView.context

        val chapterNo = chapterModel.chapterMeta.chapterNo

        var title: String? = null
        var msg: String? = null

        when (status) {
            RECITATION_DOWNLOAD_STATUS_CANCELED -> {
                downloadService?.cancelDownload(
                    chapterModel.reciterModel.slug,
                    chapterModel.chapterMeta.chapterNo
                )
            }

            RECITATION_DOWNLOAD_STATUS_FAILED -> {
                title = ctx.getString(R.string.strTitleFailed)
                msg = "${
                    ctx.getString(
                        R.string.msgRecitaionFailedToDownload,
                        quranMeta.getChapterName(ctx, chapterNo, true),
                        chapterModel.reciterModel.getReciterName()
                    )
                } ${ctx.getString(R.string.strMsgTryLater)}"
            }

            RECITATION_DOWNLOAD_STATUS_SUCCEED -> {
                title = ctx.getString(R.string.strTitleSuccess)
                msg = ctx.getString(
                    R.string.msgRecitaionDownloaded,
                    quranMeta.getChapterName(ctx, chapterNo, true),
                    chapterModel.reciterModel.getReciterName()
                )
            }
        }

        val chapters = adapter?.chapters ?: return
        for ((index, chapter) in chapters.withIndex()) {
            if (chapter.chapterMeta.chapterNo == chapterNo) {
                chapter.downloading = status == RECITATION_DOWNLOAD_STATUS_PROGRESS
                chapter.progress = progress
                chapter.downloaded = status == RECITATION_DOWNLOAD_STATUS_SUCCEED
                chapter.prepareTitle(ctx)
                adapter?.notifyItemChanged(index)
                break
            }
        }

        if (title != null) {
            MessageUtils.popMessage(ctx, title, msg, ctx.getString(R.string.strLabelClose), null)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        downloadService = (service as RecitationChapterDownloadService.LocalBinder).service

        downloadService?.let {
            adapter?.chapters?.forEachIndexed { index, model ->
                model.downloading = it.isDownloading(model.reciterModel.slug, model.chapterMeta.chapterNo)
                if (model.downloading) {
                    model.progress = it.getDownloadProgress(model.reciterModel.slug, model.chapterMeta.chapterNo)
                }
                model.prepareTitle(it)
                adapter?.notifyItemChanged(index)
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        downloadService = null
    }

    fun initDownload(model: ManageAudioChapterModel, position: Int) {
        if (adapter == null || activity == null) return

        if (NetworkStateReceiver.canProceed(requireActivity()).not()) return

        model.downloading = true
        model.prepareTitle(recyclerView.context)
        adapter!!.notifyItemChanged(position)

        RecitationChapterDownloadService.startDownloadService(requireActivity(), model)
    }

    fun deleteDownloaded(model: ManageAudioChapterModel, position: Int) {
        if (adapter == null || activity == null) return

        val ctx = recyclerView.context
        val chapterNo = model.chapterMeta.chapterNo
        val chapterName = quranMeta.getChapterName(ctx, chapterNo, true)
        val reciterName = model.reciterModel.getReciterName()

        PeaceDialog.newBuilder(ctx).apply {
            setTitle(R.string.titleRecitationCleanup)
            setMessage(ctx.getString(R.string.msgRecitaionDeleleSurah, chapterName, reciterName))
            setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
            setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
            setNeutralButton(R.string.strLabelCancel, null)
            setDialogGravity(PeaceDialog.GRAVITY_TOP)
            setNegativeButton(R.string.strLabelDelete, ColorUtils.DANGER) { _, _ ->
                for (verseNo in 1..model.chapterMeta.verseCount) {
                    fileUtils.getRecitationAudioFile(model.reciterModel.slug, chapterNo, verseNo).delete()
                }

                model.downloading = false
                model.downloaded = false
                model.prepareTitle(recyclerView.context)
                adapter!!.notifyItemChanged(position)

                MessageUtils.popMessage(
                    ctx,
                    ctx.getString(R.string.strTitleSuccess),
                    ctx.getString(
                        R.string.msgRecitaionDeleletedSurah,
                        chapterName,
                        reciterName
                    ),
                    ctx.getString(R.string.strLabelClose),
                    null
                )
            }
            setFocusOnNegative(true)
        }.show()
    }
}