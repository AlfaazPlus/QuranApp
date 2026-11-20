package com.quranapp.android.platform.clipboard

import android.content.ClipData
import android.content.Context
import androidx.core.content.ContextCompat
import com.quranapp.android.platform.ClipboardManager

class AndroidClipboardManager(
    private val context: Context,
) : ClipboardManager {
    private val cm
        get() = ContextCompat.getSystemService(
            context,
            android.content.ClipboardManager::class.java,
        )

    override fun copy(text: String, label: String?): Boolean {
        return try {
            val manager = cm ?: return false
            val clip = ClipData.newPlainText(label ?: "QuranApp", text)
            manager.setPrimaryClip(clip)

            true
        } catch (_: Exception) {
            false
        }
    }
}
