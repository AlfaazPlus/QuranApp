package com.quranapp.android.utils.reader.atlas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.isQuranAtlasScript
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

val LocalQuranAtlasBundle = staticCompositionLocalOf<QuranAtlasBundle?> { null }

/**
 * Redraw signal for one bundle. The decoded page stays in the texture store;
 * this only tells Compose that a page arrived, and holds the paint reused by every word.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class AtlasDrawSignal : AtlasPageListener {
    private val epochState = mutableIntStateOf(0)
    private val mainPoster = MainPoster()
    private val posted = AtomicBoolean(false)

    val epoch: Int
        get() = epochState.intValue

    private val images = HashMap<AtlasBitmap, ImageBitmap>()

    fun imageOf(bitmap: AtlasBitmap): ImageBitmap? {
        if (bitmap.isRecycled()) return null

        images[bitmap]?.let { return it }

        return bitmap.toComposeImage().also { images[bitmap] = it }
    }

    override fun onDecoded(index: Int) {
        if (index >= 0) bump()
    }

    override fun onCleared() {
        images.clear()
        bump()
    }

    private fun bump() {
        if (!posted.compareAndSet(expectedValue = false, newValue = true)) return

        mainPoster.post {
            posted.store(false)
            epochState.intValue++
        }
    }
}

private val drawSignals = AtlasSignalMap()

internal fun QuranAtlasBundle.drawSignal(): AtlasDrawSignal {
    return drawSignals.getOrPut(this) {
        AtlasDrawSignal().also { addPageListener(it, replay = false) }
    }
}

@Composable
fun rememberQuranAtlasBundle(db: ExternalQuranDatabase): QuranAtlasBundle? {
    val script = ReaderPreferences.observeQuranScript()
    if (!script.isQuranAtlasScript()) return null

    val bundleState = produceState<QuranAtlasBundle?>(initialValue = null, script, db) {
        value = QuranAtlasLoader.getBundle(db, script)
    }

    return bundleState.value
}
