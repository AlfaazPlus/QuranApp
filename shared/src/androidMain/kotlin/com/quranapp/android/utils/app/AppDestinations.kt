package com.quranapp.android.utils.app

import android.content.Context
import android.content.Intent

/**
 * Fully-qualified activity class names living in `:androidApp`.
 * `:shared` must not depend on `:androidApp`, so intents use [Intent.setClassName].
 */
object AppDestinations {
    const val MAIN = "com.quranapp.android.activities.MainActivity"
    const val READER = "com.quranapp.android.activities.ActivityReader"
    const val SETTINGS = "com.quranapp.android.activities.ActivitySettings"
    const val SEARCH = "com.quranapp.android.activities.ActivitySearch"
    const val ABOUT = "com.quranapp.android.activities.ActivityAbout"
    const val BOOKMARK = "com.quranapp.android.activities.ActivityBookmark"
    const val COLLECTIONS = "com.quranapp.android.activities.ActivityCollections"
    const val EXPORT_IMPORT = "com.quranapp.android.activities.ActivityExportImport"
    const val STORAGE_CLEANUP = "com.quranapp.android.activities.ActivityStorageCleanup"
    const val READ_HISTORY = "com.quranapp.android.activities.ActivityReadHistory"
    const val READER_INDEX = "com.quranapp.android.activities.ActivityReaderIndexPage"
    const val CHAP_INFO = "com.quranapp.android.activities.ActivityChapInfo"
    const val TAFSIR = "com.quranapp.android.activities.ActivityTafsir"
    const val SHARE_AS_IMAGE = "com.quranapp.android.activities.ActivityShareAsImage"
    const val REFERENCE = "com.quranapp.android.activities.reference.ActivityReference"
    const val PROPHETIC_DUAS =
        "com.quranapp.android.activities.reference.ActivityPropheticDuas"
    const val PROPHETS = "com.quranapp.android.activities.reference.ActivityProphets"
    const val EXCLUSIVE_VERSES =
        "com.quranapp.android.activities.reference.ActivityExclusiveVerses"
    const val QURAN_SCIENCE =
        "com.quranapp.android.activities.reference.ActivityQuranScience"
    const val QURAN_SCIENCE_CONTENT =
        "com.quranapp.android.activities.reference.ActivityQuranScienceContent"
    const val QURANIC_TOPICS =
        "com.quranapp.android.activities.reference.ActivityQuranicTopics"

    fun intent(context: Context, className: String): Intent =
        Intent().setClassName(context, className)
}
