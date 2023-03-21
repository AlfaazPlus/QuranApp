package com.quranapp.android.reader_managers

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.util.TypedValue
import android.widget.TextView
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.getDimension
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.reader.getQuranScriptFontRes
import com.quranapp.android.utils.reader.getQuranScriptVerseTextSizeMediumRes
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.toKFQPCFontFilename
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.verse.VerseUtils
import java.io.File

class ReaderVerseDecorator(private val ctx: Context) {
    private val fileUtils by lazy { FileUtils.newInstance(ctx) }

    private var savedScript = SPReader.getSavedScript(ctx)
    private var savedFontScript = ""

    private val textColorArabic by lazy { ctx.color(R.color.colorTextArabic) }
    private val textColorNonArabic by lazy { ctx.color(R.color.colorTextNoArabic) }
    private val textColorAuthor by lazy { ctx.color(R.color.colorSecondary) }

    var textSizeArabic = 0f
    val textSizeTransl by lazy { ctx.getDimension(R.dimen.dmnReaderTextSizeTransl) }
    private val textSizeAuthor by lazy { ctx.getDimension(R.dimen.dmnCommonSize3) }

    private var fontQuranText: Typeface? = null

    /**
     * PageNo => Typeface
     */
    private var fontsArabicKFQPC = mutableMapOf<Int, Typeface>()

    private val fontVerseSerial by lazy { ctx.getFont(R.font.uthmanic_hafs1) }
    private val fontVerseSerialFallback by lazy { ctx.getFont(R.font.traditional_arabic) }
    private val fontTranslationUrduAlike by lazy { ctx.getFont(R.font.font_urdu) }

    private var savedTextSizeArabicMultiplier = 0f
    private var savedTextSizeTranslMultiplier = 0f

    init {
        refresh()
    }

    fun refresh() {
        savedTextSizeArabicMultiplier = SPReader.getSavedTextSizeMultArabic(ctx)
        savedTextSizeTranslMultiplier = SPReader.getSavedTextSizeMultTransl(ctx)

        savedScript = SPReader.getSavedScript(ctx)
        textSizeArabic = ctx.getDimension(savedScript.getQuranScriptVerseTextSizeMediumRes()).toFloat()
    }

    fun isKFQPCScript(): Boolean {
        return savedScript.isKFQPCScript()
    }

    /**
     * KFQPCPages must not be null if isKFQPCFont() is true
     */
    fun refreshQuranTextFonts(KFQPCPageRange: Pair<Int, Int>?) {
        if (KFQPCPageRange != null) {
            fontQuranText = null

            for (pageNo in KFQPCPageRange.first..KFQPCPageRange.second) {
                if (fontsArabicKFQPC[pageNo] != null) continue

                try {
                    fontsArabicKFQPC[pageNo] = Typeface.createFromFile(
                        File(
                            fileUtils.getKFQPCScriptFontDir(savedScript),
                            pageNo.toKFQPCFontFilename()
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            fontsArabicKFQPC.clear()

            if (savedScript != savedFontScript || fontQuranText == null) {
                fontQuranText = ctx.getFont(savedScript.getQuranScriptFontRes())
                savedFontScript = savedScript
            }
        }
    }

    private fun getFontSerial(): Typeface? {
        val isKFQPC = isKFQPCScript()

        return when {
            !isKFQPC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> fontVerseSerial
            !isKFQPC -> fontVerseSerialFallback
            else -> null
        }
    }

    @JvmOverloads
    fun setupArabicText(
        arabicText: String,
        verseNo: Int,
        pageNo: Int,
        verseTextSize: Int = -1,
        serialTextSize: Int = -1
    ): CharSequence {
        val isKFQPC = isKFQPCScript()

        return VerseUtils.decorateVerse(
            arabicText,
            if (!isKFQPC) verseNo else null,
            if (isKFQPC) fontsArabicKFQPC[pageNo] ?: Typeface.DEFAULT else fontQuranText,
            getFontSerial(),
            verseTextSize,
            serialTextSize
        )
    }

    fun setupArabicTextQuranPage(
        txtColor: Int,
        arabicText: String,
        verseNo: Int,
        pageNo: Int,
        onClick: Runnable
    ): CharSequence =
        VerseUtils.decorateQuranPageVerse(
            txtColor,
            arabicText,
            verseNo,
            if (isKFQPCScript()) fontsArabicKFQPC[pageNo] else fontQuranText,
            getFontSerial(),
            onClick
        )

    fun setupTranslText(translText: String, translClr: Int, txtSize: Int, isUrdu: Boolean): SpannableString {
        return VerseUtils.decorateSingleTranslSimple(
            translText,
            translClr,
            txtSize,
            if (isUrdu) fontTranslationUrduAlike else Typeface.SANS_SERIF
        )
    }

    @JvmOverloads
    fun setupAuthorText(
        author: String,
        authorClr: Int = textColorAuthor,
        txtSize: Int = textSizeAuthor,
        isUrdu: Boolean
    ): SpannableString = VerseUtils.prepareTranslAuthorText(
        author,
        authorClr,
        txtSize,
        if (isUrdu) fontTranslationUrduAlike else Typeface.SANS_SERIF,
        false
    )

    @JvmOverloads
    fun TextView.setTextSizeArabic(multiplier: Float = savedTextSizeArabicMultiplier) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeArabic * multiplier)
    }

    @JvmOverloads
    fun TextView.setTextSizeTransl(multiplier: Float = savedTextSizeTranslMultiplier) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeTransl * multiplier)
    }

    fun TextView.setFontArabic(pageNo: Int) {
        typeface = if (isKFQPCScript()) {
            fontsArabicKFQPC[pageNo]
        } else {
            fontQuranText
        }
    }

    fun TextView.setTextColorArabic() {
        setTextColor(textColorArabic)
    }

    fun TextView.setTextColorNonArabic() {
        setTextColor(textColorNonArabic)
    }
}
