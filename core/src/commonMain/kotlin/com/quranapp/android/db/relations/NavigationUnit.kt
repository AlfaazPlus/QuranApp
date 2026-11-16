package com.quranapp.android.db.relations

import com.quranapp.android.db.entities.quran.NavigationType

data class NavigationUnitRange(
    val surah: SurahWithLocalizations,
    val startAyah: Int,
    val endAyah: Int
)

data class NavigationUnit(
    val type: NavigationType,
    val unitNo: Int,
    val ranges: List<NavigationUnitRange>
)