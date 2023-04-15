package com.quranapp.android.adapters.appLogs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.components.appLogs.SuppressedLogModel
import com.quranapp.android.databinding.LytLogItemBinding
import com.quranapp.android.utils.extensions.copyToClipboard

class ADPSuppressedLogs(private val logs: ArrayList<SuppressedLogModel>) : RecyclerView.Adapter<ADPSuppressedLogs.VHSuppressedLog>() {
    inner class VHSuppressedLog(private val binding: LytLogItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(logModel: SuppressedLogModel) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHSuppressedLog {
        return VHSuppressedLog(LytLogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }


    override fun onBindViewHolder(holder: VHSuppressedLog, position: Int) {
        holder.bind(logs[position])
    }
}