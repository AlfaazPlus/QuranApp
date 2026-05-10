package com.quranapp.android.utils.reader

import android.content.Context
import android.content.Intent
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityPropheticDuas
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.ExclusiveVersesDataset
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.Keys

object ExclusiveVerseNavigator {
    fun open(context: Context, dataset: ExclusiveVersesDataset, verse: ExclusiveVerse) {
        when (dataset) {
            ExclusiveVersesDataset.Dua -> openDua(context, verse)
            ExclusiveVersesDataset.Etiquette -> openEtiquette(context, verse)
            ExclusiveVersesDataset.MajorSins -> openMajorSins(context, verse)
            ExclusiveVersesDataset.Solution -> openSolution(context, verse)
        }
    }

    private fun openDua(context: Context, verse: ExclusiveVerse) {
        if (verse.id == 1) {
            context.startActivity(
                Intent(context, ActivityPropheticDuas::class.java).apply {
                    putExtra(Keys.KEY_EXTRA_TITLE, verse.title)
                },
            )
            return
        }

        val excluded = verse.id in arrayOf(1, 2)
        val nameTitle = if (!excluded) {
            context.getString(R.string.strMsgDuaFor, verse.title)
        } else {
            context.getString(
                R.string.strMsgReferenceInQuran,
                "\"${verse.title}\"",
            )
        }
        val description = context.getString(
            R.string.strMsgReferenceFoundPlaces,
            if (excluded) nameTitle else "\"$nameTitle\"",
            verse.verses.size,
        )

        ReaderFactory.startReferenceVerse(
            context,
            ReferenceVerseModel(
                title = nameTitle,
                desc = description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
            ),
        )
    }

    private fun openEtiquette(context: Context, verse: ExclusiveVerse) {
        ReaderFactory.startReferenceVerse(
            context,
            ReferenceVerseModel(
                title = verse.title,
                desc = verse.description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
            ),
        )
    }

    private fun openMajorSins(context: Context, verse: ExclusiveVerse) {
        ReaderFactory.startReferenceVerse(
            context,
            ReferenceVerseModel(
                title = verse.title,
                desc = verse.description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
            )
        )
    }

    private fun openSolution(context: Context, verse: ExclusiveVerse) {
        val nameTitle = context.getString(
            R.string.strMsgReferenceInQuran,
            "\"${verse.title}\"",
        )
        val description = context.getString(
            R.string.strMsgReferenceFoundPlaces,
            "\"${verse.title}\"",
            verse.verses.size,
        )
        ReaderFactory.startReferenceVerse(
            context,
            ReferenceVerseModel(
                title = nameTitle,
                desc = description,
                chapters = verse.chapters,
                verses = verse.versesRaw,
            )
        )
    }
}
