package com.quranapp.android.frags.storageCleapup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.quranapp.android.R
import com.quranapp.android.databinding.FragStorageCleanupMainBinding
import com.quranapp.android.databinding.LytStorageCleanupItemCardBinding
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.FileUtils

class FragStorageCleanupMain : FragStorageCleanupBase() {
    private lateinit var fileUtils: FileUtils

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.titleStorageCleanup)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_storage_cleanup_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileUtils = FileUtils.newInstance(view.context)
        init(FragStorageCleanupMainBinding.bind(view))
    }

    private fun init(binding: FragStorageCleanupMainBinding) {
        checkTranslations(binding)
        checkRecitations(binding)
        checkScripts(binding)
        checkChapterInfo(binding)
        // checkTafsir(binding)
    }

    private fun checkTranslations(parentBinding: FragStorageCleanupMainBinding) {
        QuranTranslationFactory(parentBinding.root.context).use {
            val downloadedTranslationsCount = it.getDownloadedTranslationBooksInfo().size

            if (downloadedTranslationsCount == 0) return

            populateTexts(
                parentBinding,
                R.string.strTitleTranslations,
                getString(R.string.translationCanBeFreedUp, downloadedTranslationsCount)
            ) {
                launchFrag(FragTranslationCleanup::class.java)
            }
        }
    }

    private fun checkRecitations(parentBinding: FragStorageCleanupMainBinding) {
        var recitersCount = 0
        var recitationsCount = 0

        fileUtils.recitationDir.listFiles()?.filter { it.isDirectory }?.forEach { reciterDir ->
            reciterDir.listFiles()?.filter { it.isFile }?.forEachIndexed { index, recitationFile ->
                if (recitationFile.length() > 0) {
                    if (index == 0) {
                        recitersCount++
                    }

                    recitationsCount++
                }
            }
        }

        if (recitationsCount == 0) return

        populateTexts(parentBinding, R.string.strTitleRecitations, getString(R.string.recitationCanBeFreedUp, recitationsCount, recitersCount)) {
            launchFrag(FragRecitationCleanup::class.java)
        }
    }

    private fun checkScripts(parentBinding: FragStorageCleanupMainBinding) {
        var scriptsCount = 0

        fileUtils.scriptDir.listFiles()?.filter { it.isFile }?.forEach { scriptFile ->
            if (scriptFile.length() > 0) {
                scriptsCount++
            }
        }

        if (scriptsCount == 0) return

        populateTexts(parentBinding, R.string.strTitleScripts, getString(R.string.scriptCanBeFreedUp, scriptsCount)) {
            launchFrag(FragScriptCleanup::class.java)
        }
    }

    private fun checkChapterInfo(parentBinding: FragStorageCleanupMainBinding) {
        var chapterInfoCount = 0

        fileUtils.chapterInfoDir.listFiles()?.forEach { chapterInfoDir ->
            if (!chapterInfoDir.isDirectory) return@forEach

            chapterInfoDir.listFiles()?.forEach { chapterInfoFile ->
                if (chapterInfoFile.isFile && chapterInfoFile.length() > 0) {
                    chapterInfoCount++
                }
            }
        }

        if (chapterInfoCount == 0) return

        populateTexts(parentBinding, R.string.titleChapterInfo, getString(R.string.chapterInfoCanBeFreedUp, chapterInfoCount)) {
            launchFrag(FragChapterInfoCleanup::class.java)
        }
    }

    private fun checkTafsir(parentBinding: FragStorageCleanupMainBinding) {
        /*val tafsirCount = 0

        if (tafsirCount == 0) return

        populateTexts(parentBinding, R.string.strTitleTafsir, getString(R.string.tafseerCanBeFreedUp, tafsirCount)) {

        }*/
    }

    private fun populateTexts(
        parentBinding: FragStorageCleanupMainBinding,
        title: Int,
        description: String,
        callback: () -> Unit
    ) {
        LytStorageCleanupItemCardBinding.inflate(layoutInflater, parentBinding.container, true).apply {
            this.title.setText(title)
            this.description.text = description

            buttonFreeUp.setOnTouchListener(HoverPushOpacityEffect())
            buttonFreeUp.setOnClickListener { callback() }
        }
    }

}