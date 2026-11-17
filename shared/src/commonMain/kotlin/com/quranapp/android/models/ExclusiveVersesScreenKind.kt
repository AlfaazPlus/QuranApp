package com.quranapp.android.models

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strTitleFeaturedDuas
import com.quranapp.android.generated.resources.strTitleMajorSins
import com.quranapp.android.generated.resources.titleEtiquetteVerses
import com.quranapp.android.generated.resources.titleSolutionVerses
import org.jetbrains.compose.resources.StringResource

enum class ExclusiveVersesScreenKind(
    val titleRes: StringResource,
    val dataset: ExclusiveVersesDataset,
) {
    Dua(Res.string.strTitleFeaturedDuas, dataset = ExclusiveVersesDataset.Dua),
    Etiquette(Res.string.titleEtiquetteVerses, dataset = ExclusiveVersesDataset.Etiquette),
    MajorSins(Res.string.strTitleMajorSins, dataset = ExclusiveVersesDataset.MajorSins),
    Solution(Res.string.titleSolutionVerses, dataset = ExclusiveVersesDataset.Solution),
}
