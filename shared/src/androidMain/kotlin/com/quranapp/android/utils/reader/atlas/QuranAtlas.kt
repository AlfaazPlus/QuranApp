package com.quranapp.android.utils.reader.atlas

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.isQuranAtlasScript
import java.util.WeakHashMap

val LocalQuranAtlasBundle = staticCompositionLocalOf<QuranAtlasBundle?> { null }

/**
 * Redraw signal for one bundle. The decoded page stays in the texture store;
 * this only tells Compose that a page arrived, and holds the paint reused by every word.
 */
internal class AtlasDrawSignal : AtlasPageListener {
    private val epochState = mutableIntStateOf(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var posted = false

    val epoch: Int
        get() = epochState.intValue

    private val images = HashMap<Bitmap, ImageBitmap>()

    fun imageOf(bitmap: Bitmap): ImageBitmap? {
        if (bitmap.isRecycled) return null

        images[bitmap]?.let { return it }

        return bitmap.asImageBitmap().also { images[bitmap] = it }
    }

    override fun onDecoded(index: Int) {
        if (index >= 0) bump()
    }

    override fun onCleared() {
        images.clear()
        bump()
    }

    private fun bump() {
        synchronized(this) {
            if (posted) return
            posted = true
        }

        mainHandler.post {
            synchronized(this) { posted = false }
            epochState.intValue++
        }
    }
}

private val drawSignals = WeakHashMap<QuranAtlasBundle, AtlasDrawSignal>()

internal fun QuranAtlasBundle.drawSignal(): AtlasDrawSignal {
    synchronized(drawSignals) {
        drawSignals[this]?.let { return it }

        val created = AtlasDrawSignal()
        drawSignals[this] = created
        addPageListener(created, replay = false)
        return created
    }
}

@Composable
fun rememberQuranAtlasBundle(db: ExternalQuranDatabase): QuranAtlasBundle? {
    val context = LocalContext.current
    val script = ReaderPreferences.observeQuranScript()
    if (!script.isQuranAtlasScript()) return null

    val bundleState = produceState<QuranAtlasBundle?>(initialValue = null, script, context) {
        value = QuranAtlasLoader.getBundle(db, script)
    }
    return bundleState.value
}
