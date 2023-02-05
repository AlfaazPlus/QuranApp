/*
 * (c) Faisal Khan. Created on 2/2/2022.
 */
/*
 * (c) Faisal Khan. Created on 30/1/2022.
 */
package com.quranapp.android.utils.fb

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.quranapp.android.ApiConfig.FB_STORAGE_BUCKET
import com.quranapp.android.BuildConfig

object FirebaseUtils {
    const val RTDB_VERSE_REPORTS = "verseReports"
    const val STORAGE_BUCKET = FB_STORAGE_BUCKET

    private const val FIREBASE_EMULATOR_HOST = BuildConfig.DEVELOPER_MACHINE_IP
    private val FIREBASE_EMULATOR_ACTIVE = false && BuildConfig.DEBUG
    private const val FIREBASE_EMULATOR_PORT_STORAGE = 9199

    fun storage(): FirebaseStorage {
        val storage = FirebaseStorage.getInstance()
        if (FIREBASE_EMULATOR_ACTIVE) {
            try {
                storage.useEmulator(FIREBASE_EMULATOR_HOST, FIREBASE_EMULATOR_PORT_STORAGE)
            } catch (_: Exception) {
            }
        }

        return storage
    }

    @JvmStatic
    fun storageRef(): StorageReference {
        return storage().getReferenceFromUrl(STORAGE_BUCKET)
    }

}