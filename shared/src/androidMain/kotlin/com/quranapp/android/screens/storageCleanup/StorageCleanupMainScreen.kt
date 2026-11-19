package com.quranapp.android.screens.storageCleanup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.Loader
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.labelFreeUpSpace
import com.quranapp.android.generated.resources.nothingToCleanup
import com.quranapp.android.generated.resources.recitationCanBeFreedUp
import com.quranapp.android.generated.resources.scriptCanBeFreedUp
import com.quranapp.android.generated.resources.storageCleanupMessage
import com.quranapp.android.generated.resources.strTitleRecitations
import com.quranapp.android.generated.resources.strTitleScripts
import com.quranapp.android.generated.resources.strTitleTafsir
import com.quranapp.android.generated.resources.strTitleTranslations
import com.quranapp.android.generated.resources.tafseerCanBeFreedUp
import com.quranapp.android.generated.resources.translationCanBeFreedUp
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.reader.QuranScriptUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private data class HubMetrics(
    val translationCount: Int = 0,
    val recitationFileCount: Int = 0,
    val recitationReciterCount: Int = 0,
    val scriptDirCount: Int = 0,
    val tafsirDownloadedCount: Int = 0,
)

@Composable
fun StorageCleanupMainScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onOpenSection: (StorageCleanupPane) -> Unit,
) {
    val context = LocalContext.current
    val recitationManager = remember(context) { RecitationModelManager }

    var metrics by remember { mutableStateOf(HubMetrics()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        metrics = withContext(Dispatchers.IO) {
            val translations = DatabaseProvider.getTranslationRepository().let {
                it.getDownloadedBooks().size
            }
            val (recFiles, recReciters) = recitationManager.getDownloadedAudioStats()
            val scripts =
                QuranScriptUtils.fontDir().toFile().listFiles()?.count { it.isDirectory } ?: 0
            val tafsirCount = run {
                val helper = QuranTafsirDBHelper()
                try {
                    helper.getDownloadedTafsirKeys().size
                } finally {
                    helper.close()
                }
            }
            HubMetrics(
                translationCount = translations,
                recitationFileCount = recFiles,
                recitationReciterCount = recReciters,
                scriptDirCount = scripts,
                tafsirDownloadedCount = tafsirCount,
            )
        }
        loading = false
    }

    val hasAnything = metrics.translationCount > 0 ||
            metrics.recitationFileCount > 0 ||
            metrics.scriptDirCount > 0 ||
            metrics.tafsirDownloadedCount > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) {
            Loader(true)
        } else {
            Text(
                text = stringResource(
                    if (hasAnything) Res.string.storageCleanupMessage
                    else Res.string.nothingToCleanup,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        if (!loading && metrics.translationCount > 0) {
            CleanupHubCard(
                title = stringResource(Res.string.strTitleTranslations),
                description = stringResource(
                    Res.string.translationCanBeFreedUp,
                    metrics.translationCount,
                ),
                onAction = { onOpenSection(StorageCleanupPane.Translations) },
            )
        }

        if (!loading && metrics.recitationFileCount > 0) {
            CleanupHubCard(
                title = stringResource(Res.string.strTitleRecitations),
                description = stringResource(
                    Res.string.recitationCanBeFreedUp,
                    metrics.recitationFileCount,
                    metrics.recitationReciterCount,
                ),
                onAction = { onOpenSection(StorageCleanupPane.Recitations) },
            )
        }

        if (!loading && metrics.scriptDirCount > 0) {
            CleanupHubCard(
                title = stringResource(Res.string.strTitleScripts),
                description = stringResource(
                    Res.string.scriptCanBeFreedUp,
                    metrics.scriptDirCount,
                ),
                onAction = { onOpenSection(StorageCleanupPane.Scripts) },
            )
        }

        if (!loading && metrics.tafsirDownloadedCount > 0) {
            CleanupHubCard(
                title = stringResource(Res.string.strTitleTafsir),
                description = stringResource(
                    Res.string.tafseerCanBeFreedUp,
                    metrics.tafsirDownloadedCount,
                ),
                onAction = { onOpenSection(StorageCleanupPane.Tafsir) },
            )
        }
    }
}

@Composable
private fun CleanupHubCard(
    title: String,
    description: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.alpha(0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            FilledTonalButton(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.labelFreeUpSpace))
            }
        }
    }
}
