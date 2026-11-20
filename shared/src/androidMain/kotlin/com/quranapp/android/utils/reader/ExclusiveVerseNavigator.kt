package com.quranapp.android.utils.reader

import android.content.Context
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strMsgDuaFor
import com.quranapp.android.generated.resources.strMsgReferenceFoundPlaces
import com.quranapp.android.generated.resources.strMsgReferenceInQuran
import com.quranapp.android.models.ExclusiveVerse
import com.quranapp.android.models.ExclusiveVersesDataset
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.utils.miscellenous.Keys
import org.jetbrains.compose.resources.getString
import com.quranapp.android.platform.PlatformServices

object ExclusiveVerseNavigator {
    suspend fun open(context: Context, dataset: ExclusiveVersesDataset, verse: ExclusiveVerse) {
        when (dataset) {
            ExclusiveVersesDataset.Dua -> openDua(context, verse)
            ExclusiveVersesDataset.Etiquette -> openEtiquette(context, verse)
            ExclusiveVersesDataset.MajorSins -> openMajorSins(context, verse)
            ExclusiveVersesDataset.Solution -> openSolution(context, verse)
        }
    }

    private suspend fun openDua(context: Context, verse: ExclusiveVerse) {
        if (verse.id == 1) {
            PlatformServices.navigator.navigate(
                AppRoute.Screen(
                    screen = AppRoutes.PROPHETIC_DUAS,
                    extras = mapOf(Keys.KEY_EXTRA_TITLE to verse.title)
                )
            )
            return
        }

        val excluded = verse.id in arrayOf(1, 2)
        val nameTitle = if (!excluded) {
            getString(Res.string.strMsgDuaFor, verse.title)
        } else {
            getString(
                Res.string.strMsgReferenceInQuran,
                "\"${verse.title}\"",
            )
        }
        val description = getString(
            Res.string.strMsgReferenceFoundPlaces,
            if (excluded) nameTitle else "\"$nameTitle\"",
            verse.verses.size,
        )

        PlatformServices.navigator.navigate(
            AppRoute.Screen(
                screen = AppRoutes.REFERENCE,
                extras = mapOf(
                    "title" to nameTitle,
                    "desc" to description,
                    "chapters" to verse.chapters,
                    "verses" to verse.versesRaw,
                ),
            )
        )
    }

    private fun openEtiquette(context: Context, verse: ExclusiveVerse) {
        PlatformServices.navigator.navigate(
            AppRoute.Screen(
                screen = AppRoutes.REFERENCE,
                extras = mapOf(
                    "title" to verse.title,
                    "desc" to verse.description,
                    "chapters" to verse.chapters,
                    "verses" to verse.versesRaw,
                ),
            )
        )
    }

    private fun openMajorSins(context: Context, verse: ExclusiveVerse) {
        PlatformServices.navigator.navigate(
            AppRoute.Screen(
                screen = AppRoutes.REFERENCE,
                extras = mapOf(
                    "title" to verse.title,
                    "desc" to verse.description,
                    "chapters" to verse.chapters,
                    "verses" to verse.versesRaw,
                ),
            )
        )
    }

    private suspend fun openSolution(context: Context, verse: ExclusiveVerse) {
        val nameTitle = getString(
            Res.string.strMsgReferenceInQuran,
            "\"${verse.title}\"",
        )
        val description = getString(
            Res.string.strMsgReferenceFoundPlaces,
            "\"${verse.title}\"",
            verse.verses.size,
        )
        PlatformServices.navigator.navigate(
            AppRoute.Screen(
                screen = AppRoutes.REFERENCE,
                extras = mapOf(
                    "title" to nameTitle,
                    "desc" to description,
                    "chapters" to verse.chapters,
                    "verses" to verse.versesRaw,
                ),
            )
        )
    }
}
