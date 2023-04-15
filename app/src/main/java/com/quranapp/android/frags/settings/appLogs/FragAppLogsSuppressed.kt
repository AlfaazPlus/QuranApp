package com.quranapp.android.frags.settings.appLogs

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.quranapp.android.adapters.appLogs.ADPSuppressedLogs
import com.quranapp.android.components.appLogs.SuppressedLogModel
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.updatePaddingHorizontal
import com.quranapp.android.utils.univ.DateUtils
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.widgets.PageAlert

class FragAppLogsSuppressed : BaseFragment() {
    private lateinit var binding: FragSettingsTranslBinding
    private lateinit var fileUtils: FileUtils

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileUtils = FileUtils.newInstance(view.context)
        binding = FragSettingsTranslBinding.bind(view)

        init(view.context)
    }

    private fun init(context: Context) {
        val logs = ArrayList<SuppressedLogModel>()

        val files = Log.SUPPRESSED_ERROR_DIR.listFiles()
        if (files?.isNotEmpty() != true) {
            PageAlert(context).apply {
                setIcon(null)
                setMessage("", context.getString(msgRes))
                show(binding.container)
            }
            return
        }

        files.forEach { logFile ->
            val (datetimeStr, place) = logFile.name.split("@")
            val log = logFile.readText()
            val formattedDateTime = DateUtils.format(DateUtils.toDate(datetimeStr, Log.FILE_NAME_DATE_FORMAT), DateUtils.DATETIME_FORMAT_USER)

            logs.add(
                SuppressedLogModel(
                    formattedDateTime,
                    place,
                    logFile,
                    log,
                    log.substring(0, 100) + if (log.length > 100) "..." else "",
                )
            )
        }

        binding.list.updatePaddingHorizontal(context.dp2px(15F))
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = ADPSuppressedLogs(logs)
    }
}