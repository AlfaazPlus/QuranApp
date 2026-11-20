package com.quranapp.android.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.reader.toIntent

/**
 * Converts an [AppRoute] to an Android [Intent].
 *
 * - [AppRoute.Reader] delegates to [ReaderLaunchParams.toIntent] and sets the READER class name.
 * - [AppRoute.Screen] sets the class name from [screen] (resolved via [AppDestinations]) and writes [extras].
 */
fun AppRoute.toIntent(context: Context): Intent = when (this) {
    is AppRoute.Reader -> params.toIntent()
        .setClassName(context, "com.quranapp.android.activities.ActivityReader")

    is AppRoute.Screen -> Intent()
        .setClassName(context, resolveActivityClassname(screen))
        .putAppRouteExtras(extras)
}


fun resolveActivityClassname(screen: String): String = when (screen) {
    AppRoutes.MAIN -> "MainActivity"
    AppRoutes.SETTINGS -> "ActivitySettings"
    AppRoutes.SEARCH -> "ActivitySearch"
    AppRoutes.ABOUT -> "ActivityAbout"
    AppRoutes.BOOKMARK -> "ActivityBookmark"
    AppRoutes.COLLECTIONS -> "ActivityCollections"
    AppRoutes.EXPORT_IMPORT -> "ActivityExportImport"
    AppRoutes.STORAGE_CLEANUP -> "ActivityStorageCleanup"
    AppRoutes.READ_HISTORY -> "ActivityReadHistory"
    AppRoutes.READER_INDEX -> "ActivityReaderIndexPage"
    AppRoutes.CHAP_INFO -> "ActivityChapInfo"
    AppRoutes.TAFSIR -> "ActivityTafsir"
    AppRoutes.SHARE_AS_IMAGE -> "ActivityShareAsImage"
    AppRoutes.REFERENCE -> "reference.ActivityReference"
    AppRoutes.PROPHETIC_DUAS -> "reference.ActivityPropheticDuas"
    AppRoutes.PROPHETS -> "reference.ActivityProphets"
    AppRoutes.EXCLUSIVE_VERSES -> "reference.ActivityExclusiveVerses"
    AppRoutes.QURAN_SCIENCE -> "reference.ActivityQuranScience"
    AppRoutes.QURAN_SCIENCE_CONTENT -> "reference.ActivityQuranScienceContent"
    AppRoutes.QURANIC_TOPICS -> "reference.ActivityQuranicTopics"
    else -> null
}.let {
    if (it != null) "com.quranapp.android.activities.$it" else screen
}

class AndroidAppNavigator(private val context: Context) : AppNavigator {
    override fun navigate(route: AppRoute) {
        try {
            val intent = route.toIntent(context).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidAppNavigator.navigate")
        }
    }
}

private fun Intent.putAppRouteExtras(extras: Map<String, Any?>): Intent = apply {
    extras.forEach { (key, value) ->
        when (value) {
            null -> putExtra(key, null as String?)
            is String -> putExtra(key, value)
            is Int -> putExtra(key, value)
            is Long -> putExtra(key, value)
            is Boolean -> putExtra(key, value)
            is Float -> putExtra(key, value)
            is Double -> putExtra(key, value)
            is Array<*> -> if (value.isArrayOf<String>()) {
                @Suppress("UNCHECKED_CAST")
                putExtra(key, value as Array<String>)
            }

            is Collection<*> -> {
                val list = value.toList()
                when {
                    list.all { it is String } -> putStringArrayListExtra(
                        key,
                        ArrayList(list.filterIsInstance<String>())
                    )

                    list.all { it is Int } -> putIntegerArrayListExtra(
                        key,
                        ArrayList(list.filterIsInstance<Int>())
                    )
                }
            }

            is java.io.Serializable -> putExtra(key, value)
            is android.os.Bundle -> putExtra(key, value)
            is android.os.Parcelable -> putExtra(key, value)
        }
    }
}
