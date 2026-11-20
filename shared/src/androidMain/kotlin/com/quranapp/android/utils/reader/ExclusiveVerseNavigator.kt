package com.quranapp.android.utils.reader

import android.content.Context
import com.quranapp.android.models.ReferenceVerseModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strMsgDuaFor
import com.quranapp.android.generated.resources.strMsgReferenceFoundPlaces
import com.quranapp.android.generated.resources.strMsgReferenceInQuran
import com.quranapp.android.models.ExclusiveVerse
import com.quranapp.android.models.ExclusiveVersesDataset
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.miscellenous.Keys
import org.jetbrains.compose.resources.getString

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
            Platform.navigator.navigate(
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

        openReference(
            ReferenceVerseModel(
                title = nameTitle,
                desc = description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
            )
        )
    }

    private fun openEtiquette(context: Context, verse: ExclusiveVerse) {
        openReference(
            ReferenceVerseModel(
                title = verse.title,
                desc = verse.description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
            )
        )
    }

    private fun openMajorSins(context: Context, verse: ExclusiveVerse) {
        openReference(
            ReferenceVerseModel(
                title = verse.title,
                desc = verse.description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
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

        openReference(
            ReferenceVerseModel(
                title = nameTitle,
                desc = description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
            )
        )
    }

    private fun openReference(model: ReferenceVerseModel) {
        Platform.navigator.navigate(
            AppRoute.Screen(
                screen = AppRoutes.REFERENCE,
                extras = model.toExtras(),
            )
        )
    }
}
