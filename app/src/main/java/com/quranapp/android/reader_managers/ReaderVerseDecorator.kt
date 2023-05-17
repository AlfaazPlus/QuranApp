package com.quranapp.android.reader_managers

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.util.TypedValue
import android.widget.TextView
import com.quranapp.android.R
import com.quranapp.android.components.quran.subcomponents.Verse
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
    val fileUtils by lazy { FileUtils.newInstance(ctx) }

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

    @JvmOverloads
    fun prepareArabicText(verse: Verse, verseTextSize: Int = -1): CharSequence {
        val isKFQPC = isKFQPCScript()

        return VerseUtils.decorateVerse(
            verse,
            if (isKFQPC) fontsArabicKFQPC[verse.pageNo] ?: Typeface.DEFAULT else fontQuranText,
            verseTextSize
        )
    }

    fun setupArabicTextQuranPage(
        txtColor: Int,
        verse: Verse,
        onClick: Runnable,
    ): CharSequence =
        VerseUtils.decorateQuranPageVerse(
            txtColor,
            verse,
            if (isKFQPCScript()) fontsArabicKFQPC[verse.pageNo] else fontQuranText,
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
        isUrdu: Boolean,
    ): SpannableString = VerseUtils.prepareTranslAuthorText(
        author,
        authorClr,
        txtSize,
        if (isUrdu) fontTranslationUrduAlike else Typeface.SANS_SERIF,
        false
    )

    @JvmOverloads
    fun setTextSizeArabic(txtView: TextView, multiplier: Float = savedTextSizeArabicMultiplier) {
        txtView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeArabic * multiplier)
    }

    @JvmOverloads
    fun setTextSizeTransl(txtView: TextView, multiplier: Float = savedTextSizeTranslMultiplier) {
        txtView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeTransl * multiplier)
    }

    fun setFontArabic(txtView: TextView, pageNo: Int) {
        txtView.typeface = if (isKFQPCScript()) {
            fontsArabicKFQPC[pageNo]
        } else {
            fontQuranText
        }
    }

    fun setTextColorArabic(txtView: TextView) {
        txtView.setTextColor(textColorArabic)
    }

    fun setTextColorNonArabic(txtView: TextView) {
        txtView.setTextColor(textColorNonArabic)
    }
}
