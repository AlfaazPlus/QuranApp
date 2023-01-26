package com.quranapp.android.components

import com.google.firebase.remoteconfig.ktx.get
import com.quranapp.android.BuildConfig
import com.quranapp.android.utils.fb.FirebaseUtils
import org.json.JSONArray

class AppUpdateInfo {
    companion object {
        const val CRITICAL = 5
        const val MAJOR = 4
        const val MODERATE = 3
        const val MINOR = 2
        const val COSMETIC = 1
        const val NONE = 0


        private fun priorityIntToName(priorityInt: Int): String {
            return when (priorityInt) {
                CRITICAL -> "CRITICAL"
                MAJOR -> "MAJOR"
                MODERATE -> "MODERATE"
                MINOR -> "MINOR"
                COSMETIC -> "COSMETIC"
                else -> "NONE"
            }
        }
    }

    data class AppUpdate(val version: Long, val priority: Int) {
        val priorityName = priorityIntToName(priority)

        override fun toString(): String {
            return "AppUpdate(version=$version, priority=$priority, priorityName=$priorityName)"
        }
    }

    private val updates: List<AppUpdate> = ArrayList<AppUpdate>().apply {
        try {
            val arr = JSONArray(FirebaseUtils.remoteConfig()["updates"].asString())

            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { updateObj ->
                    add(
                        AppUpdate(
                            updateObj.optLong("version"),
                            updateObj.optInt("updatePriority")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMostImportantUpdate(): AppUpdate {
        val currentAppVersion = BuildConfig.VERSION_CODE

        val mostImportantUpdate = updates
            .filter { it.version > currentAppVersion }
            .sortedBy { it.priority }
        return mostImportantUpdate.takeIf { it.isNotEmpty() }?.get(0) ?: AppUpdate(0, NONE)
    }

}