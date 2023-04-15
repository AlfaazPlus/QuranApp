package com.quranapp.android.frags.settings.appLogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.quranapp.android.R
import com.quranapp.android.adapters.appLogs.ADPAppLogs
import com.quranapp.android.components.appLogs.AppLogModel
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extended.GapedItemDecoration
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.updatePaddingHorizontal
import com.quranapp.android.utils.univ.DateUtils
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.widgets.PageAlert

class FragAppLogsCrash : BaseFragment() {
    private lateinit var binding: FragSettingsTranslBinding
    private lateinit var fileUtils: FileUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_settings_transl, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileUtils = FileUtils.newInstance(view.context)
        binding = FragSettingsTranslBinding.bind(view)

        init(view.context)
    }

    private fun init(context: Context) {
        val logs = ArrayList<AppLogModel>()

        val files = Log.CRASH_ERROR_DIR.listFiles()
        if (files.isNullOrEmpty()) {
            PageAlert(context).apply {
                setIcon(null)
                setMessage("", context.getString(R.string.textNoLogsFound))
                show(binding.container)
            }
            binding.loader.visibility = View.GONE
            return
        }

        files.sortedBy { it.lastModified() }.forEach { logFile ->
            val log = logFile.readText()
            val formattedDateTime = DateUtils.format(
                DateUtils.toDate(logFile.name, Log.FILE_NAME_DATE_FORMAT),
                DateUtils.DATETIME_FORMAT_USER
            )

            logs.add(
                AppLogModel(
                    formattedDateTime,
                    "Fatal Crash",
                    logFile,
                    log,
                    log.substring(0, 200) + if (log.length > 200) "..." else "",
                )
            )
        }

        binding.list.let {
            it.addItemDecoration(GapedItemDecoration(context.dp2px(10F)))
            it.updatePaddingHorizontal(context.dp2px(15F))
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = ADPAppLogs(logs)
        }
        binding.loader.visibility = View.GONE
    }
}