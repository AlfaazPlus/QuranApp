package com.quranapp.android.adapters.appLogs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.R
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.appLogs.AppLogModel
import com.quranapp.android.databinding.LytLogItemBinding
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.univ.MessageUtils

class ADPAppLogs(
    private val logs: ArrayList<AppLogModel>,
    private val logColor: Int
) : RecyclerView.Adapter<ADPAppLogs.VHAppLog>() {
    inner class VHAppLog(private val binding: LytLogItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.clipToOutline = true

            binding.root.context.let {
                ViewCompat.setTooltipText(binding.btnDelete, it.getString(R.string.strLabelDelete))
                ViewCompat.setTooltipText(binding.btnCopy, it.getString(R.string.strLabelCopy))
                ViewCompat.setTooltipText(binding.btnGitHub, it.getString(R.string.createIssue))
            }
        }

        fun bind(logModel: AppLogModel) {
            binding.datetime.text = logModel.datetime
            binding.logText.text = logModel.logShort
            binding.place.text = logModel.place
            binding.logText.setTextColor(logColor)

            binding.btnDelete.setOnClickListener {
                if (logModel.file.delete()) {
                    MessageUtils.showRemovableToast(it.context, R.string.logRemoved, Toast.LENGTH_SHORT)
                    logs.removeAt(bindingAdapterPosition)
                    notifyItemRemoved(bindingAdapterPosition)
                }
            }
            binding.btnCopy.setOnClickListener {
                it.context.copyToClipboard(logModel.log)
                MessageUtils.showRemovableToast(it.context, R.string.copiedToClipboard, Toast.LENGTH_SHORT)
            }

            binding.btnGitHub.setOnClickListener {
                it.context.copyToClipboard(logModel.log)
                MessageUtils.showRemovableToast(it.context, R.string.pasteCrashLogGithubIssue, Toast.LENGTH_LONG)
                AppBridge.newOpener(it.context).browseLink(ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL)
            }
        }
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHAppLog {
        return VHAppLog(LytLogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }


    override fun onBindViewHolder(holder: VHAppLog, position: Int) {
        holder.bind(logs[position])
    }
}