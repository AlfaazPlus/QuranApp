package com.quranapp.android.components.quran

import com.quranapp.android.core.R

enum class ExclusiveVersesScreenKind(
    val titleRes: Int,
    val dataset: ExclusiveVersesDataset,
) {
    Dua(R.string.strTitleFeaturedDuas, dataset = ExclusiveVersesDataset.Dua),
    Etiquette(R.string.titleEtiquetteVerses, dataset = ExclusiveVersesDataset.Etiquette),
    MajorSins(R.string.strTitleMajorSins, dataset = ExclusiveVersesDataset.MajorSins),
    Solution(R.string.titleSolutionVerses, dataset = ExclusiveVersesDataset.Solution),
}
