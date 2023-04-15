package com.quranapp.android.adapters.appLogs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.components.appLogs.AppLogModel
import com.quranapp.android.databinding.LytLogItemBinding
import com.quranapp.android.utils.extensions.copyToClipboard

class ADPAppLogs(private val logs: ArrayList<AppLogModel>) : RecyclerView.Adapter<ADPAppLogs.VHAppLog>() {
    inner class VHAppLog(private val binding: LytLogItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.clipToOutline = true
        }

        fun bind(logModel: AppLogModel) {
            binding.datetime.text = logModel.datetime
            binding.logText.text = logModel.logShort
            binding.place.text = logModel.place

            binding.btnDelete.setOnClickListener {
                if (logModel.file.delete()) {
                    Toast.makeText(it.context, R.string.logRemoved, Toast.LENGTH_SHORT).show()
                    logs.removeAt(bindingAdapterPosition)
                    notifyItemRemoved(bindingAdapterPosition)
                }
            }
            binding.btnCopy.setOnClickListener {
                it.context.copyToClipboard(logModel.log)
                Toast.makeText(it.context, R.string.copiedToClipboard, Toast.LENGTH_SHORT).show()
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