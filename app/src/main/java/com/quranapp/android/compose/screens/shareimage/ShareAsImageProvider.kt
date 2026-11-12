package com.quranapp.android.compose.screens.shareimage

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.R
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.compose.utils.DataLoadError
import com.quranapp.android.compose.utils.formattedStringResource
import com.quranapp.android.compose.utils.preferences.AppPreferences
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.repository.TranslationRepository
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import com.quranapp.android.utils.reader.atlas.QuranAtlasLoader
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.isPrebuiltAtlas
import com.quranapp.android.utils.reader.isQuranAtlasScript
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.share.PreparedVerseScene
import com.quranapp.android.utils.share.ShareImageExporter
import com.quranapp.android.utils.share.VerseFitStage
import com.quranapp.android.utils.share.VerseRenderInput
import com.quranapp.android.utils.share.VerseTemplate
import com.quranapp.android.utils.share.VerseTemplateManager
import com.quranapp.android.utils.share.VerseTemplateRenderer
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private data class LoadedAtlasState(
    val scriptCode: String,
    val ayahId: Int,
    val bundle: QuranAtlasBundle,
    val placements: Map<Int, List<AtlasGlyphPlacement>>,
)

data class ShareAsImageSession(
    val verse: VerseWithDetails?,
    val templates: List<VerseTemplate>,
    val isTemplatesLoading: Boolean,
    val templatesError: DataLoadError?,
    val selectedTemplate: VerseTemplate?,
    val previewBgBitmap: Bitmap?,
    val isPreviewBgLoading: Boolean,
    val scene: PreparedVerseScene?,
    val canExport: Boolean,
    val isExporting: Boolean,
    val scriptCode: String,
    val contentOption: ShareContentOption,
    val selectedBook: TranslationBookInfoModel?,
    val onOpenContent: () -> Unit,
    val onOpenTranslation: () -> Unit,
    val onOpenScript: () -> Unit,
    val onSelectTemplate: (String) -> Unit,
    val selectedTabIndex: Int,
    val onSelectTab: (Int) -> Unit,
    val templatesGridState: LazyGridState,
    val customizeScrollState: ScrollState,
    val onPreviewWidthPx: (Int) -> Unit,
    val onRetryTemplates: () -> Unit,
    val onShare: () -> Unit,
    val onDownload: () -> Unit,
)

val LocalShareAsImage = staticCompositionLocalOf<ShareAsImageSession> {
    error("ShareAsImageSession not provided")
}

@Composable
fun ShareAsImageProvider(
    chapterNo: Int,
    verseNo: Int,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fontResolver = remember { FontResolver.getInstance(context) }
    val translRepo = TranslationRepository.remember(context)
    val downloadProxy = AppPreferences.observeResourceDownloadProxy()

    var templates by remember { mutableStateOf<List<VerseTemplate>>(emptyList()) }
    var isTemplatesLoading by remember { mutableStateOf(true) }
    var templatesError by remember { mutableStateOf<DataLoadError?>(null) }
    var templatesReloadToken by remember { mutableIntStateOf(0) }

    var selectedTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var contentOption by rememberSaveable(stateSaver = ShareContentOptionSaver) {
        mutableStateOf(ShareContentOption.DEFAULT)
    }
    var appliedReaderContentDefault by rememberSaveable { mutableStateOf(false) }

    var selectedSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedScript by rememberSaveable { mutableStateOf<String?>(null) }
    var showContentSheet by rememberSaveable { mutableStateOf(false) }
    var showTranslationSheet by rememberSaveable { mutableStateOf(false) }
    var showScriptSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var previewTargetWidthPx by remember {
        mutableIntStateOf(VerseTemplateRenderer.previewWidthCap(context))
    }
    val templatesGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val customizeScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    var isExporting by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            VerseTemplateManager.clearPreviewCache()
        }
    }

    LaunchedEffect(templatesReloadToken, downloadProxy) {
        isTemplatesLoading = true
        templatesError = null

        val force = templatesReloadToken > 0
        val catalog = VerseTemplateManager.getAvailable(context, forceRefresh = force)

        when {
            catalog == null -> {
                templatesError = if (!NetworkStateReceiver.isNetworkConnected(context)) {
                    DataLoadError.NoConnection
                } else {
                    DataLoadError.Failed
                }
            }

            catalog.templates.isEmpty() -> {
                templates = emptyList()
                templatesError = DataLoadError.NoData
            }

            else -> {
                templates = catalog.templates
                templatesError = null
                if (selectedTemplateId == null || catalog.templates.none { it.id == selectedTemplateId }) {
                    selectedTemplateId = catalog.templates.first().id
                }
            }
        }

        isTemplatesLoading = false
    }

    val availableBooks by produceState(emptyList<TranslationBookInfoModel>(), translRepo) {
        value = withContext(Dispatchers.IO) {
            translRepo.getBooks().values
                .sortedBy { it.displayName.ifBlank { it.bookName } }
        }
    }

    val availableScripts by produceState(emptyList<String>(), context) {
        value = withContext(Dispatchers.IO) {
            listAvailableScripts(context)
        }
    }

    LaunchedEffect(availableBooks) {
        if (selectedSlug == null && availableBooks.isNotEmpty()) {
            val preferred = withContext(Dispatchers.IO) {
                ReaderPreferences.primaryTranslationSlug()
            }
            selectedSlug = availableBooks.firstOrNull { it.slug == preferred }?.slug
                ?: availableBooks.first().slug
        }
    }

    LaunchedEffect(availableScripts) {
        if (selectedScript == null && availableScripts.isNotEmpty()) {
            val preferred = withContext(Dispatchers.IO) {
                QuranScriptUtils.validatePreferredScript(ReaderPreferences.getQuranScript())
            }
            selectedScript = availableScripts.firstOrNull { it == preferred }
                ?: availableScripts.first()
        }
    }

    LaunchedEffect(Unit) {
        if (appliedReaderContentDefault) return@LaunchedEffect

        val arabicEnabled = withContext(Dispatchers.IO) {
            ReaderPreferences.getArabicTextEnabled()
        }
        if (!arabicEnabled) {
            contentOption = ShareContentOption.TRANSLATION
        }
        appliedReaderContentDefault = true
    }

    val showArabic = contentOption.showArabic
    val showTranslation = contentOption.showTranslation

    val scriptCode = selectedScript ?: QuranScriptUtils.SCRIPT_DEFAULT
    val selectedTemplate = remember(templates, selectedTemplateId) {
        templates.firstOrNull { it.id == selectedTemplateId } ?: templates.firstOrNull()
    }

    val verse by produceState<VerseWithDetails?>(
        initialValue = null,
        chapterNo,
        verseNo,
        scriptCode,
        showArabic,
    ) {
        value = withContext(Dispatchers.IO) {
            DatabaseProvider.getQuranRepository(context).getVerseWithDetails(
                chapterNo = chapterNo,
                verseNo = verseNo,
                scriptCode = scriptCode,
                arabicEnabled = showArabic,
            )
        }
    }

    val translation by produceState<Translation?>(
        initialValue = null,
        selectedSlug,
        chapterNo,
        verseNo,
    ) {
        val slug = selectedSlug
        value = if (slug.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                translRepo.getTranslation(slug, chapterNo, verseNo)
            }
        }
    }

    val loadedAtlas by produceState<LoadedAtlasState?>(
        initialValue = null,
        scriptCode,
        verse,
        showArabic,
        context,
    ) {
        val v = verse
        if (!showArabic || v == null || v.words.isEmpty() || !scriptCode.isQuranAtlasScript()) {
            value = null
            return@produceState
        }

        value = withContext(Dispatchers.IO) {
            val bundle = QuranAtlasLoader.getBundle(
                context,
                DatabaseProvider.getExternalQuranDatabase(context),
                scriptCode,
            ) ?: return@withContext null

            val placements = bundle.getPlacementsForWords(v.words, v.pageNo)
            LoadedAtlasState(
                scriptCode = scriptCode,
                ayahId = v.verse.ayahId,
                bundle = bundle,
                placements = placements,
            )
        }
    }

    var previewBgPair by remember { mutableStateOf<Pair<String, Bitmap>?>(null) }
    var isPreviewBgLoading by remember { mutableStateOf(false) }

    val previewCanvasSize = selectedTemplate?.layout?.let { layout ->
        VerseTemplateRenderer.computePreviewDimensions(layout, previewTargetWidthPx)
    }

    LaunchedEffect(selectedTemplate?.id, selectedTemplate?.bgSrc, previewCanvasSize, downloadProxy) {
        val tpl = selectedTemplate
        val size = previewCanvasSize
        if (tpl == null || size == null) {
            previewBgPair = null
            isPreviewBgLoading = false
            return@LaunchedEffect
        }

        if (previewBgPair?.first != tpl.id || previewBgPair?.second?.isRecycled == true) {
            isPreviewBgLoading = true
        }
        val bmp = VerseTemplateManager.loadPreviewBitmap(
            context = context,
            template = tpl,
            reqWidth = size.first,
            reqHeight = size.second,
        )
        previewBgPair = if (bmp != null) tpl.id to bmp else null
        isPreviewBgLoading = false
    }

    val activePreviewBg = previewBgPair
        ?.takeIf { it.first == selectedTemplate?.id && !it.second.isRecycled }
        ?.second

    val cleanTranslationText = remember(translation) {
        VerseTemplateRenderer.cleanTranslationText(translation)
    }

    val citationText = if (verse != null) {
        formattedStringResource(
            R.string.strLabelVerseSerialWithChapter,
            verse!!.chapter.getCurrentName(),
            verse!!.chapterNo,
            verse!!.verseNo,
        )
    } else {
        ""
    }

    val renderInput = remember(
        selectedTemplate,
        verse,
        scriptCode,
        loadedAtlas,
        cleanTranslationText,
        selectedSlug,
        citationText,
        showArabic,
        showTranslation,
    ) {
        val tpl = selectedTemplate ?: return@remember null
        val v = verse ?: return@remember null

        val matchingAtlas = loadedAtlas?.takeIf {
            it.scriptCode == scriptCode && it.ayahId == v.verse.ayahId
        }

        if (showArabic && v.words.isNotEmpty() && scriptCode.isQuranAtlasScript() && matchingAtlas == null) {
            return@remember null
        }

        VerseRenderInput(
            template = tpl,
            arabicWords = v.words,
            pageNo = v.pageNo,
            scriptCode = scriptCode,
            atlasBundle = matchingAtlas?.bundle,
            atlasPlacements = matchingAtlas?.placements.orEmpty(),
            translationText = cleanTranslationText,
            translationSlug = selectedSlug.orEmpty(),
            referenceText = citationText,
            showArabic = showArabic,
            showTranslation = showTranslation,
            showReference = true,
            showLogo = true,
        )
    }

    var scene by remember { mutableStateOf<PreparedVerseScene?>(null) }

    LaunchedEffect(renderInput, previewCanvasSize) {
        val input = renderInput
        val size = previewCanvasSize
        if (input == null || size == null) {
            if (selectedTemplate == null || verse == null) {
                scene = null
            }
            return@LaunchedEffect
        }

        scene = VerseTemplateRenderer.prepareScene(
            context = context,
            fontResolver = fontResolver,
            input = input,
            canvasSize = size,
        )
    }

    val fitStage = scene?.fitResult?.stage ?: VerseFitStage.IDEAL
    val canExport = selectedTemplate != null &&
        verse != null &&
        scene != null &&
        fitStage != VerseFitStage.REJECTED
    val selectedBook = availableBooks.firstOrNull { it.slug == selectedSlug }

    ShareContentOptionSheet(
        isOpen = showContentSheet,
        selected = contentOption,
        onSelect = { contentOption = it },
        onClose = { showContentSheet = false },
    )

    TranslationSelectionSheet(
        isOpen = showTranslationSheet,
        books = availableBooks,
        selectedSlug = selectedSlug,
        onSelect = { selectedSlug = it },
        onClose = { showTranslationSheet = false },
    )

    ScriptSelectionSheet(
        isOpen = showScriptSheet,
        scripts = availableScripts,
        selectedScript = selectedScript,
        onSelect = { selectedScript = it },
        onClose = { showScriptSheet = false },
    )

    suspend fun buildExportBitmap(): Bitmap? {
        val input = renderInput ?: return null
        val latestScene = VerseTemplateRenderer.prepareScene(
            context = context,
            fontResolver = fontResolver,
            input = input,
            canvasSize = VerseTemplateRenderer.computeExportDimensions(input.template.layout),
        )
        if (latestScene.fitResult.stage == VerseFitStage.REJECTED) {
            return null
        }
        return VerseTemplateRenderer.renderHighQualityBitmap(
            context = context,
            scene = latestScene,
            template = input.template,
        )
    }

    fun onShare() {
        if (isExporting || !canExport) return
        isExporting = true
        scope.launch {
            val bitmap = buildExportBitmap()
            if (bitmap == null) {
                MessageUtils.showRemovableToast(
                    context,
                    R.string.image_share_failed,
                    Toast.LENGTH_SHORT,
                )
                isExporting = false
                return@launch
            }

            val ok = ShareImageExporter.share(
                context,
                bitmap,
                context.getString(R.string.strLabelShare),
            )
            if (!ok) {
                MessageUtils.showRemovableToast(
                    context,
                    R.string.image_share_failed,
                    Toast.LENGTH_SHORT,
                )
            }
            isExporting = false
        }
    }

    fun onDownload() {
        if (isExporting || !canExport) return
        isExporting = true
        scope.launch {
            val bitmap = buildExportBitmap()
            if (bitmap == null) {
                MessageUtils.showRemovableToast(
                    context,
                    R.string.image_save_failed,
                    Toast.LENGTH_SHORT,
                )
                isExporting = false
                return@launch
            }

            val ok = ShareImageExporter.saveToGallery(context, bitmap)
            MessageUtils.showRemovableToast(
                context,
                if (ok) R.string.image_saved else R.string.image_save_failed,
                Toast.LENGTH_SHORT,
            )
            isExporting = false
        }
    }

    val session = ShareAsImageSession(
        verse = verse,
        templates = templates,
        isTemplatesLoading = isTemplatesLoading,
        templatesError = templatesError,
        selectedTemplate = selectedTemplate,
        previewBgBitmap = activePreviewBg,
        isPreviewBgLoading = isPreviewBgLoading,
        scene = scene,
        canExport = canExport,
        isExporting = isExporting,
        scriptCode = scriptCode,
        contentOption = contentOption,
        selectedBook = selectedBook,
        onOpenContent = { showContentSheet = true },
        onOpenTranslation = { showTranslationSheet = true },
        onOpenScript = { showScriptSheet = true },
        onSelectTemplate = { selectedTemplateId = it },
        selectedTabIndex = selectedTabIndex,
        onSelectTab = { selectedTabIndex = it },
        templatesGridState = templatesGridState,
        customizeScrollState = customizeScrollState,
        onPreviewWidthPx = { widthPx ->
            val cap = VerseTemplateRenderer.previewWidthCap(context)
            val next = widthPx.coerceIn(280, cap)
            if (abs(next - previewTargetWidthPx) >= 32) {
                previewTargetWidthPx = next
            }
        },
        onRetryTemplates = { templatesReloadToken++ },
        onShare = ::onShare,
        onDownload = ::onDownload,
    )

    CompositionLocalProvider(LocalShareAsImage provides session, content = content)
}

private val ShareContentOptionSaver = Saver<ShareContentOption, String>(
    save = { it.value },
    restore = { ShareContentOption.fromValue(it) },
)

private suspend fun listAvailableScripts(context: Context): List<String> {
    val db = DatabaseProvider.getExternalQuranDatabase(context)

    return QuranScriptUtils.availableScripts().keys.filter { script ->
        when {
            script.isPrebuiltAtlas() -> true
            script.isQuranAtlasScript() -> {
                db.atlasWordShapeDao().getBundleByKey(script) != null
            }

            script.isKFQPCScript() -> {
                QuranScriptUtils.getKFQPCFontDownloadedCount(context, script).remaining == 0
            }

            else -> true
        }
    }
}
