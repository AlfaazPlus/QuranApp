package com.quranapp.android.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.noto_nastaliq_urdu_regular
import com.quranapp.android.generated.resources.quran_common
import com.quranapp.android.generated.resources.scheherazadenew_regular
import com.quranapp.android.generated.resources.suracon
import com.quranapp.android.resources.ComposeResourceFiles

private val urduFamily by lazy {
    FontFamily(
        Font(
            ComposeResourceFiles.fontFile(
                Res.font.noto_nastaliq_urdu_regular,
                "noto_nastaliq_urdu_regular.ttf"
            ),
            weight = FontWeight.Normal,
        ),
    )
}

private val arabicFamily by lazy {
    FontFamily(
        Font(
            ComposeResourceFiles.fontFile(
                Res.font.scheherazadenew_regular,
                "scheherazadenew_regular.ttf"
            ),
            weight = FontWeight.Normal,
        ),
    )
}

private val commonFamily by lazy {
    FontFamily(
        Font(
            ComposeResourceFiles.fontFile(Res.font.quran_common, "quran_common.ttf"),
            weight = FontWeight.Normal,
        ),
    )
}

private val surahFamily by lazy {
    FontFamily(
        Font(
            ComposeResourceFiles.fontFile(Res.font.suracon, "suracon.ttf"),
            weight = FontWeight.Normal,
        ),
    )
}

actual val fontUrdu: FontFamily get() = urduFamily
actual val fontArabic: FontFamily get() = arabicFamily
actual val fontCommon: FontFamily get() = commonFamily
actual val fontSurah: FontFamily get() = surahFamily
