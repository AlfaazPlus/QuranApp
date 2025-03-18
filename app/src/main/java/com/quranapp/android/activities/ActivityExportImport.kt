package com.quranapp.android.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.databinding.ActivityExportImportBinding
import com.quranapp.android.databinding.LytExportItemCardBinding
import com.quranapp.android.db.bookmark.BookmarkDBHelper
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.views.BoldHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.InputStreamReader

class ActivityExportImport : BaseActivity() {
    private var bookmarkExportContent = ""
    private val bookmarkExportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportBookmarks(uri)
            }
        }
    }

    private val bookmarkImportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importBookmarks(uri)
            }
        }
    }

    override fun getLayoutResource() = R.layout.activity_export_import

    override fun shouldInflateAsynchronously() = true

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityExportImportBinding.bind(activityView)

        initHeader(binding.header)

        populateTexts(
            binding.container,
            R.string.strTitleBookmarks,
            getString(R.string.msgExportImportBookmarks),
            exportCallback = {
                val bookmarkDbHeader = BookmarkDBHelper(this)


                CoroutineScope(Dispatchers.IO).launch {
                    val bookmarks = bookmarkDbHeader.bookmarks

                    if (bookmarks.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            MessageUtils.showRemovableToast(
                                this@ActivityExportImport,
                                R.string.msgExportEmpty,
                                Toast.LENGTH_LONG
                            )
                        }
                        return@launch
                    }

                    val jsonString = BookmarkModel.toJson(bookmarks)


                    withContext(Dispatchers.Main) {
                        bookmarkDbHeader.close()
                        bookmarkExportContent = jsonString

                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                            putExtra(Intent.EXTRA_TITLE, "quranapp-bookmarks.json")
                        }
                        bookmarkExportLauncher.launch(intent)
                    }
                }
            },
            importCallback = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                bookmarkImportLauncher.launch(intent)
            },
        )
    }

    private fun initHeader(header: BoldHeader) {
        header.let {
            it.setCallback { onBackPressedDispatcher.onBackPressed() }
            it.setTitleText(R.string.titleExportData)
            it.setShowRightIcon(false)
            it.setShowSearchIcon(false)
            it.setBGColor(R.color.colorBGPage)
        }
    }


    private fun populateTexts(
        parent: ViewGroup,
        title: Int,
        description: String,
        rootId: Int = View.generateViewId(),
        exportCallback: () -> Unit,
        importCallback: (() -> Unit)? = null
    ) {
        LytExportItemCardBinding.inflate(layoutInflater, parent, true).apply {
            root.id = rootId

            this.title.setText(title)
            this.description.text = description

            buttonExport.setOnTouchListener(HoverPushOpacityEffect())
            buttonExport.setOnClickListener { exportCallback() }

            if (importCallback != null) {
                buttonImport.setOnTouchListener(HoverPushOpacityEffect())
                buttonImport.setOnClickListener { importCallback() }
            } else {
                buttonImport.visibility = View.GONE
            }
        }
    }


    private fun exportBookmarks(uri: Uri) {
        if (bookmarkExportContent.isEmpty()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bookmarkExportContent.toByteArray())
            }

            withContext(Dispatchers.Main) {
                MessageUtils.showRemovableToast(
                    this@ActivityExportImport,
                    R.string.msgExportSuccess,
                    Toast.LENGTH_LONG
                )
            }
        }
    }


    private fun importBookmarks(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val content = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            withContext(Dispatchers.Main) {
                parseBookmarksAndSave(content)
            }
        }
    }

    private fun parseBookmarksAndSave(content: String) {
        val bookmarkDbHeader = BookmarkDBHelper(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JsonHelper.json.parseToJsonElement(content).jsonObject
                val bookmarks = BookmarkModel.fromJson(jsonObject["bookmarks"]?.jsonArray)
                bookmarkDbHeader.addMultipleBookmarks(bookmarks)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    bookmarkDbHeader.close()

                    Logger.d(e)
                    Log.saveError(e, "parseBookmarksAndSave")
                    MessageUtils.showRemovableToast(
                        this@ActivityExportImport,
                        R.string.msgImportFailed,
                        Toast.LENGTH_LONG
                    )
                }

                return@launch
            }

            withContext(Dispatchers.Main) {
                bookmarkDbHeader.close()
                MessageUtils.showRemovableToast(
                    this@ActivityExportImport,
                    R.string.msgImportSuccess,
                    Toast.LENGTH_LONG
                )
            }
        }

    }
}
