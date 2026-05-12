package com.quranapp.android.views.player

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.Bitmap
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.mediaplayer.RecitationChapterArtwork
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.RecitationService
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import com.quranapp.android.utils.reader.factory.ReaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RecitationWidgetRefreshTrigger(
    val service: RecitationServiceState,
    val controllerPlaying: Boolean,
    val controllerBuffering: Boolean,
    val controllerConnected: Boolean,
    val appearance: Triple<String, String, Boolean>,
)

private data class RecitationPlayerWidgetUiState(
    val artwork: Bitmap,
    val title: String,
    val subtitle: String,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val colors: ColorScheme,
    val openReaderIntent: Intent,
)

class RecitationPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecitationPlayerGlanceWidget()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        val manager = GlanceAppWidgetManager(context)
        val widget = RecitationPlayerGlanceWidget()
        val glanceId = manager.getGlanceIdBy(appWidgetId)

        CoroutineScope(Dispatchers.Default).launch {
            widget.update(context, glanceId)
        }
    }
}

private class RecitationPlayerGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val sizes = LocalSize.current
            val glanceState = currentState<Preferences>()

            val state by produceState<RecitationPlayerWidgetUiState?>(null, sizes, glanceState) {
                try {
                    value = buildRecitationPlayerWidgetState(
                        context = context,
                        widgetWidthDp = sizes.width.value,
                        widgetHeightDp = sizes.height.value,
                    )
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    e.printStackTrace()
                }
            }

            RecitationPlayerGlanceContent(context, state)
        }
    }
}

@Composable
private fun RecitationPlayerGlanceContent(
    context: Context,
    state: RecitationPlayerWidgetUiState?,
) {
    if (state == null) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black))
                .cornerRadius(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        return
    }

    val colors = state.colors

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(18.dp)
            .background(state.colors.surfaceContainer)
            .clickable(actionStartActivity(state.openReaderIntent)),
    ) {

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(state.artwork),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(56.dp)
                        .background(colors.surfaceContainer)
                        .cornerRadius(16.dp),
                    contentScale = ContentScale.FillBounds
                )

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        text = state.title,
                        style = TextStyle(
                            color = ColorProvider(colors.onSurface),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        maxLines = 2,

                    )

                    Spacer(modifier = GlanceModifier.height(3.dp))

                    Text(
                        text = state.subtitle,
                        style = TextStyle(
                            color = ColorProvider(colors.onSurface.alpha(0.72f)),
                            fontSize = 14.sp,
                        ),
                        maxLines = 1
                    )
                }

                WidgetIconButton(
                    colors = state.colors,
                    icon = R.drawable.ic_skip_back,
                    contentDescription = context.getString(R.string.strLabelPrevious),
                    onClick = actionRunCallback<RecitationPlayerPreviousAction>(),
                    sizeDp = 38,
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                WidgetPlayPauseButton(
                    colors = state.colors,
                    isLoading = state.isLoading,
                    isPlaying = state.isPlaying,
                    contentDescription = context.getString(
                        when {
                            state.isLoading -> R.string.textPreparingAudio
                            state.isPlaying -> R.string.strLabelPause
                            else -> R.string.strLabelPlay
                        }
                    ),
                    onClick = actionRunCallback<RecitationPlayerToggleAction>(),
                    sizeDp = 44,
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                WidgetIconButton(
                    colors = state.colors,
                    icon = R.drawable.ic_skip_forward,
                    contentDescription = context.getString(R.string.strLabelNext),
                    onClick = actionRunCallback<RecitationPlayerNextAction>(),
                    sizeDp = 38,
                )
            }
        }
    }
}

@Composable
private fun WidgetPlayPauseButton(
    colors: ColorScheme,
    isLoading: Boolean,
    isPlaying: Boolean,
    contentDescription: String,
    onClick: Action,
    sizeDp: Int,
) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .background(colors.primary)
            .cornerRadius(99.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size((sizeDp - 14).coerceAtLeast(22).dp),
                color = ColorProvider(colors.onPrimary),
            )
        } else {
            Image(
                provider = ImageProvider(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                ),
                contentDescription = contentDescription,
                modifier = GlanceModifier.size((sizeDp - 20).coerceAtLeast(16).dp),
                colorFilter = ColorFilter.tint(ColorProvider(colors.onPrimary)),
            )
        }
    }
}

@Composable
private fun WidgetIconButton(
    colors: ColorScheme,
    icon: Int,
    contentDescription: String,
    onClick: Action,
    sizeDp: Int,
) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .background(colors.surfaceContainerHigh)
            .cornerRadius(99.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size((sizeDp - 20).coerceAtLeast(16).dp),
            colorFilter = ColorFilter.tint(ColorProvider(colors.onSurface))
        )
    }
}

private suspend fun buildRecitationPlayerWidgetState(
    context: Context,
    widgetWidthDp: Float,
    widgetHeightDp: Float,
): RecitationPlayerWidgetUiState {
    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val systemDarkMode = uiMode == Configuration.UI_MODE_NIGHT_YES
    val colorScheme = ThemeUtils.colorSchemeFromPreferences(context, systemDarkMode)

    val playbackState = RecitationService.sharedState.value
    val lastVerse = RecitationPreferences.getLastPlayedVerse()
    val verse = if (playbackState == RecitationServiceState.EMPTY && lastVerse != null) {
        lastVerse
    } else {
        playbackState.currentVerse
    }.takeIf { it.isValid } ?: ChapterVersePair(1, 1)

    val repository = DatabaseProvider.getQuranRepository(context)
    val controller = RecitationController.getInstance(context)
    val connected = controller.isConnectedState.value
    val isPlaying = if (connected) controller.isPlaying else playbackState.isPlaying
    val isLoading = if (connected) {
        controller.isLoading
    } else {
        playbackState.resolvingChapterNo != null || playbackState.isBuffering
    }

    val (chapterName, reciterName) = withContext(Dispatchers.IO) {
        coroutineScope {
            val chapterDeferred = async {
                repository.getChapterName(verse.chapterNo).ifBlank { verse.chapterNo.toString() }
            }

            val reciterDeferred = async {
                RecitationModelManager.get(context)
                    .getCurrentReciterNameForAudioOption()
                    .ifBlank { context.getString(R.string.strTitleVerseRecitation) }
            }

            chapterDeferred.await() to reciterDeferred.await()
        }
    }

    val openReaderIntent = ReaderFactory.prepareSingleVerseIntent(verse.chapterNo, verse.verseNo).apply {
        setClass(context, ActivityReader::class.java)
    }

    return RecitationPlayerWidgetUiState(
        artwork = RecitationChapterArtwork.getChapterArtworkBitmap(
            context,
            verse.chapterNo,
            context.dp2px(56f)
        ),
        title = context.getString(R.string.strLabelSurah, chapterName),
        subtitle = reciterName,
        isPlaying = isPlaying,
        isLoading = isLoading,
        colors = colorScheme,
        openReaderIntent = openReaderIntent,
    )
}

fun updateAllRecitationPlayerWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val widget = RecitationPlayerGlanceWidget()

    CoroutineScope(Dispatchers.Default).launch {
        val glanceIds = manager.getGlanceIds(widget.javaClass)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[longPreferencesKey("recitation_player_last_update")] =
                        System.currentTimeMillis()
                }
            }

            widget.update(context, glanceId)
        }
    }
}

private val recitationPlayerWidgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun startRecitationPlayerWidgetObserver(context: Context) {
    val app = context.applicationContext

    recitationPlayerWidgetScope.launch {
        val controller = RecitationController.getInstance(app)
        combine(
            RecitationService.sharedState,
            controller.isPlayingState,
            controller.isBufferingState,
            controller.isConnectedState,
            ThemeUtils.widgetAppearancePreferencesFlow(),
        ) { service, playing, buffering, connected, appearance ->
            RecitationWidgetRefreshTrigger(service, playing, buffering, connected, appearance)
        }
            .distinctUntilChanged()
            .collect {
                updateAllRecitationPlayerWidgets(app)
            }
    }
}

private suspend fun runRecitationControls(
    context: Context,
    block: suspend RecitationController.() -> Unit,
) {
    withContext(Dispatchers.Main) {
        RecitationController.getInstance(context).block()
    }
}

class RecitationPlayerToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        runRecitationControls(context) { playPause() }
        updateAllRecitationPlayerWidgets(context)
    }
}

class RecitationPlayerPreviousAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        runRecitationControls(context) { previousVerse() }
        updateAllRecitationPlayerWidgets(context)
    }
}

class RecitationPlayerNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        runRecitationControls(context) { nextVerse() }
        updateAllRecitationPlayerWidgets(context)
    }
}
