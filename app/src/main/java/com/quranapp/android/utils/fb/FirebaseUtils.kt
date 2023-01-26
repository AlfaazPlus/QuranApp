/*
 * (c) Faisal Khan. Created on 2/2/2022.
 */
/*
 * (c) Faisal Khan. Created on 30/1/2022.
 */
package com.quranapp.android.utils.fb

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.quranapp.android.BuildConfig

object FirebaseUtils {
    const val FIRESTORE_USERS_COLLECTION = "users"
    const val RTDB_VERSE_REPORTS = "verseReports"
    const val STORAGE_BUCKET = "gs://the-quranapp.appspot.com"

    private const val FIREBASE_EMULATOR_HOST = BuildConfig.DEVELOPER_MACHINE_IP
    private val FIREBASE_EMULATOR_ACTIVE = false && BuildConfig.DEBUG
    private const val FIREBASE_EMULATOR_PORT_AUTH = 9099
    private const val FIREBASE_EMULATOR_PORT_FIRESTORE = 8080
    private const val FIREBASE_EMULATOR_PORT_RTDB = 9000
    private const val FIREBASE_EMULATOR_PORT_FUNCTIONS = 5001
    private const val FIREBASE_EMULATOR_PORT_STORAGE = 9199

    @JvmStatic
    fun firestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    fun storage(): FirebaseStorage {
        val storage = FirebaseStorage.getInstance()
        if (FIREBASE_EMULATOR_ACTIVE) {
            try {
                storage.useEmulator(FIREBASE_EMULATOR_HOST, FIREBASE_EMULATOR_PORT_STORAGE)
            } catch (e: Exception) {
            }
        }

        return storage
    }

    @JvmStatic
    fun storageRef(): StorageReference {
        return storage().getReferenceFromUrl(STORAGE_BUCKET)
    }


    @JvmStatic
    fun remoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 60 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        return remoteConfig
    }
}