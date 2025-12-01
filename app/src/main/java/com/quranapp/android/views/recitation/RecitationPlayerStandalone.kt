package com.quranapp.android.views.recitation

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.LytRecitationPlayerStandaloneBinding
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.mediaplayer.PlaybackMode
import com.quranapp.android.utils.mediaplayer.PlaybackSettings
import com.quranapp.android.utils.mediaplayer.PlayerEvent
import com.quranapp.android.utils.mediaplayer.RecitationService
import com.quranapp.android.utils.sharedPrefs.SPReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * RecitationPlayerStandalone - A beautiful, self-contained recitation player UI.
 * 
 * Features:
 * - Full-height expandable player with beautiful artwork
 * - Collapsible mini player mode
 * - Full controls including play/pause, seek, verse navigation
 * - Settings for repeat, autoplay, playback speed
 * - Verse sync indication
 * - Can be added to any Activity/Fragment
 * 
 * Usage:
 * ```xml
 * <com.quranapp.android.views.recitation.RecitationPlayerStandalone
 *     android:id="@+id/recitationPlayer"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" />
 * ```
 * 
 * ```kotlin
 * recitationPlayer.setQuranMeta(quranMeta)
 * recitationPlayer.onVerseChangeListener = { chapter, verse -> ... }
 * ```
 */
@SuppressLint("ViewConstructor")
class RecitationPlayerStandalone @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = LytRecitationPlayerStandaloneBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    
    private val controller = RecitationController.getInstance(context)
    private var uiScope: CoroutineScope? = null
    
    private var quranMetaRef: WeakReference<QuranMeta>? = null
    private var isExpanded = false
    private var isAnimating = false
    
    private val miniPlayerHeight = context.resources.getDimensionPixelSize(R.dimen.dmnMiniPlayerHeight)
    
    // Back press handler for collapsing full player
    private val backPressCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            collapse()
        }
    }
    
    /**
     * Callback when verse changes during playback.
     * Useful for highlighting verses in the reader.
     */
    var onVerseChangeListener: ((chapter: Int, verse: Int, isPlaying: Boolean) -> Unit)? = null
    
    /**
     * Callback when player expansion state changes.
     */
    var onExpansionChangeListener: ((isExpanded: Boolean) -> Unit)? = null

    init {
        id = R.id.recitationPlayerStandalone
        isSaveEnabled = true
        
        // Consume touch events to prevent click-through
        setOnTouchListener { _, _ -> true }
        
        setupControls()
        updateTimelineText(0, 0)
        updateProgressBar(0)
        
        // Handle window insets for navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            
            // Add padding for status bar at top of full player
            binding.fullPlayer.updatePadding(top = statusBarInsets.top)
            
            // Add padding for navigation bar at bottom of controls
            binding.controlsContainer.updatePadding(bottom = navBarInsets.bottom + 24.dpToPx())
            
            insets
        }
        
        // Start in collapsed state
        setExpandedState(false, animate = false)
    }
    
    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startObservingState()
        controller.connect()
        
        // Register back press callback
        getActivity()?.onBackPressedDispatcher?.addCallback(backPressCallback)
    }

    override fun onDetachedFromWindow() {
        stopObservingState()
        backPressCallback.remove()
        super.onDetachedFromWindow()
    }
    
    private fun getActivity(): AppCompatActivity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is AppCompatActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    // ==================== Public API ====================

    /**
     * Set QuranMeta reference for chapter name lookup.
     */
    fun setQuranMeta(quranMeta: QuranMeta?) {
        quranMetaRef = quranMeta?.let { WeakReference(it) }
    }

    /**
     * Expand to full-height player view.
     */
    fun expand() {
        if (!isExpanded && !isAnimating) {
            setExpandedState(true, animate = true)
        }
    }

    /**
     * Collapse to mini player view.
     */
    fun collapse() {
        if (isExpanded && !isAnimating) {
            setExpandedState(false, animate = true)
        }
    }

    /**
     * Toggle between expanded and collapsed states.
     */
    fun toggle() {
        if (isExpanded) collapse() else expand()
    }

    /**
     * Check if player is currently expanded.
     */
    fun isPlayerExpanded(): Boolean = isExpanded

    // ==================== State Observation ====================

    private fun startObservingState() {
        uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Observe player status
        uiScope?.launch {
            controller.playerStatus.collect { status ->
                updateLoadingState(status.isLoading)
                updatePlayButton(status.isPlaying)
                updatePlaybackModeUI(status.playbackMode, status.hasVerseTiming)
            }
        }

        // Observe playback progress
        uiScope?.launch {
            controller.playbackProgress.collect { progress ->
                updateProgressUI(progress.positionMs, progress.durationMs)
            }
        }

        // Observe playback settings
        uiScope?.launch {
            controller.playbackSettings.collect { settings ->
                updateSettingsUI(settings)
            }
        }

        // Observe current verse
        uiScope?.launch {
            controller.currentVerse.collect { verse ->
                val status = controller.playerStatus.value
                updateVerseInfo(verse.chapter, verse.verse)
                onVerseChangeListener?.invoke(verse.chapter, verse.verse, status.isPlaying)
            }
        }

        // Observe reciter info
        uiScope?.launch {
            controller.reciterInfo.collect { info ->
                updateReciterInfo(info.reciter)
            }
        }

        // Observe events
        uiScope?.launch {
            controller.events.collect { event ->
                when (event) {
                    is PlayerEvent.Error -> {
                        val msg = event.message ?: context.getString(event.messageResId)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    is PlayerEvent.Message -> {
                        val msg = event.message ?: context.getString(event.messageResId)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun stopObservingState() {
        uiScope?.cancel()
        uiScope = null
    }

    // ==================== Controls Setup ====================

    private fun setupControls() {
        binding.apply {
            // Expand/Collapse - mini player opens full player
            expandBtn.setOnClickListener { expand() }
            miniVerseInfo.setOnClickListener { expand() }
            collapseBtn.setOnClickListener { collapse() }
            
            // Play/Pause controls
            playControl.setOnClickListener {
                if (!controller.isLoading) {
                    controller.playPause()
                }
            }
            miniPlayControl.setOnClickListener {
                if (!controller.isLoading) {
                    controller.playPause()
                }
            }
            
            // Seek controls
            seekLeft.setOnClickListener {
                if (!controller.isLoading) controller.seekLeft()
            }
            seekRight.setOnClickListener {
                if (!controller.isLoading) controller.seekRight()
            }
            
            // Verse navigation
            prevVerse.setOnClickListener { controller.previousVerse() }
            nextVerse.setOnClickListener { controller.nextVerse() }
            
            // Verse sync
            verseSync.setOnClickListener {
                val currentSync = controller.playbackSettings.value.verseSync
                controller.setVerseSync(!currentSync, fromUser = true)
            }
            
            // Repeat button
            repeatBtn.setOnClickListener {
                val currentRepeat = controller.playbackSettings.value.repeatVerse
                controller.setRepeat(!currentRepeat)
            }
            
            // Speed button
            speedBtn.setOnClickListener {
                openPlaybackSpeedDialog()
            }
            
            // Autoplay button
            autoplayBtn.setOnClickListener {
                val currentAutoplay = controller.playbackSettings.value.continueRange
                controller.setContinue(!currentAutoplay)
            }
            
            // Menu
            menu.setOnClickListener {
                openOptionsMenu()
            }
            
            // Seekbar
            progress.isSaveEnabled = false
            progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                private var wasPlaying = false
                private var lastProgress = 0

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        lastProgress = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    wasPlaying = controller.isPlaying
                    controller.pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val seekPosition = lastProgress * RecitationService.MILLIS_MULTIPLIER
                    controller.seekTo(seekPosition)
                    if (wasPlaying) {
                        controller.resume()
                    }
                }
            })
        }
    }

    // ==================== UI Updates ====================

    private fun updateLoadingState(isLoading: Boolean) {
        binding.apply {
            // Full player
            if (isLoading) {
                loader.visibility = VISIBLE
                playControl.visibility = GONE
            } else {
                loader.visibility = GONE
                playControl.visibility = VISIBLE
            }
            
            // Mini player
            if (isLoading) {
                miniLoader.visibility = VISIBLE
                miniPlayControl.visibility = GONE
            } else {
                miniLoader.visibility = GONE
                miniPlayControl.visibility = VISIBLE
            }
            
            // Disable actions during loading
            val alpha = if (isLoading) 0.5f else 1f
            arrayOf(progress, seekLeft, seekRight, prevVerse, nextVerse).forEach {
                it.alpha = alpha
                it.isEnabled = !isLoading
            }
        }
    }

    private fun updatePlayButton(isPlaying: Boolean) {
        binding.apply {
            val icon = if (isPlaying) R.drawable.dr_icon_pause_verse else R.drawable.dr_icon_play_verse
            playControl.setImageResource(icon)
            miniPlayControl.setImageResource(icon)
        }
    }

    private fun updateProgressUI(positionMs: Long, durationMs: Long) {
        val maxProgress = (durationMs / RecitationService.MILLIS_MULTIPLIER).coerceAtLeast(0).toInt()
        val currentProgress = (positionMs / RecitationService.MILLIS_MULTIPLIER).toInt()
        
        binding.progress.max = maxProgress
        updateProgressBar(currentProgress)
        updateTimelineText(positionMs, durationMs)
        
        // Update mini progress bar
        val miniMax = 1000
        val miniProgress = if (durationMs > 0) {
            ((positionMs.toFloat() / durationMs) * miniMax).toInt()
        } else 0
        binding.miniProgress.max = miniMax
        binding.miniProgress.progress = miniProgress
    }

    private fun updateProgressBar(progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.progress.setProgress(progress, true)
        } else {
            binding.progress.progress = progress
        }
    }

    private fun updateTimelineText(positionMs: Long, durationMs: Long) {
        binding.currentTime.text = formatTime(positionMs)
        binding.totalTime.text = formatTime(durationMs)
    }

    private fun updateVerseInfo(chapter: Int, verse: Int) {
        val quranMeta = quranMetaRef?.get()
        
        if (chapter > 0 && verse > 0) {
            val chapterName = quranMeta?.getChapterName(context, chapter) ?: "Chapter $chapter"
            val verseCount = quranMeta?.getChapterVerseCount(chapter) ?: verse
            
            binding.apply {
                // Full player
                this.chapterName.text = chapterName
                verseInfo.text = context.getString(R.string.strLabelVerseOf, verse, verseCount)
                
                // Mini player
                miniTitle.text = "$chapterName • ${context.getString(R.string.strLabelVerseWithNum, verse)}"
            }
        }
    }

    private fun updateReciterInfo(reciterName: String?) {
        val displayName = reciterName 
            ?: RecitationManager.getCurrentReciterNameForAudioOption(context)
        
        binding.apply {
            this.reciterName.text = displayName
            miniSubtitle.text = displayName
        }
    }

    private fun updateSettingsUI(settings: PlaybackSettings) {
        val goldColor = 0xFFD4AF37.toInt()
        val inactiveColor = 0x80FFFFFF.toInt()
        
        binding.apply {
            // Verse sync
            verseSync.isSelected = settings.verseSync
            verseSync.setImageResource(
                if (settings.verseSync) R.drawable.dr_icon_locked 
                else R.drawable.dr_icon_unlocked
            )
            verseSync.setColorFilter(if (settings.verseSync) goldColor else inactiveColor)
            
            // Repeat button
            repeatBtn.setColorFilter(if (settings.repeatVerse) goldColor else inactiveColor)
            
            // Autoplay
            autoplayIcon.setColorFilter(if (settings.continueRange) goldColor else inactiveColor)
            autoplayLabel.setTextColor(if (settings.continueRange) goldColor else inactiveColor)
            
            // Speed
            speedLabel.text = String.format(Locale.getDefault(), "%.1fx", settings.speed)
        }
    }

    private fun updatePlaybackModeUI(playbackMode: PlaybackMode, hasVerseTiming: Boolean) {
        val isFullChapter = playbackMode == PlaybackMode.FULL_CHAPTER
        val canNavigateVerses = !isFullChapter || hasVerseTiming
        
        binding.apply {
            listOf(prevVerse, nextVerse, seekLeft, seekRight).forEach { btn ->
                btn.isEnabled = canNavigateVerses
                btn.alpha = if (canNavigateVerses) 1f else 0.3f
            }
            
            verseSync.apply {
                val enabled = !isFullChapter || hasVerseTiming
                isEnabled = enabled
                alpha = if (enabled) 1f else 0.3f
            }
        }
    }

    // ==================== Expand/Collapse Animation ====================

    private fun setExpandedState(expanded: Boolean, animate: Boolean) {
        if (isAnimating) return
        isExpanded = expanded
        
        // Update back press callback - enabled only when expanded
        backPressCallback.isEnabled = expanded
        
        // Update layout params for proper positioning
        updateLayoutForState(expanded)
        
        if (animate) {
            animateTransition(expanded)
        } else {
            binding.apply {
                fullPlayer.isVisible = expanded
                miniPlayer.isVisible = !expanded
            }
        }
        
        onExpansionChangeListener?.invoke(expanded)
    }
    
    private fun updateLayoutForState(expanded: Boolean) {
        val params = layoutParams
        
        when (params) {
            is CoordinatorLayout.LayoutParams -> {
                if (expanded) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    params.gravity = Gravity.NO_GRAVITY
                    params.behavior = null // Remove scroll behavior when expanded
                } else {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    params.gravity = Gravity.BOTTOM
                }
            }
            is FrameLayout.LayoutParams -> {
                if (expanded) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    params.gravity = Gravity.NO_GRAVITY
                } else {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    params.gravity = Gravity.BOTTOM
                }
            }
            else -> {
                params?.height = if (expanded) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        
        layoutParams = params
    }

    private fun animateTransition(toExpanded: Boolean) {
        isAnimating = true
        
        val duration = 300L
        val interpolator = DecelerateInterpolator()
        
        binding.apply {
            if (toExpanded) {
                // Collapse -> Expand (full height)
                fullPlayer.visibility = VISIBLE
                fullPlayer.alpha = 0f
                miniPlayer.alpha = 1f
                
                ValueAnimator.ofFloat(0f, 1f).apply {
                    this.duration = duration
                    this.interpolator = interpolator
                    addUpdateListener { animator ->
                        val value = animator.animatedValue as Float
                        fullPlayer.alpha = value
                        miniPlayer.alpha = 1f - value
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            miniPlayer.visibility = GONE
                            isAnimating = false
                        }
                    })
                    start()
                }
            } else {
                // Expand -> Collapse (mini player)
                miniPlayer.visibility = VISIBLE
                miniPlayer.alpha = 0f
                fullPlayer.alpha = 1f
                
                ValueAnimator.ofFloat(0f, 1f).apply {
                    this.duration = duration
                    this.interpolator = interpolator
                    addUpdateListener { animator ->
                        val value = animator.animatedValue as Float
                        miniPlayer.alpha = value
                        fullPlayer.alpha = 1f - value
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            fullPlayer.visibility = GONE
                            isAnimating = false
                        }
                    })
                    start()
                }
            }
        }
    }

    // ==================== Dialogs ====================

    private fun openPlaybackSpeedDialog() {
        val speeds = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val speedLabels = speeds.map { String.format(Locale.getDefault(), "%.2fx", it) }.toTypedArray()
        val currentSpeed = controller.playbackSettings.value.speed
        val selectedIndex = speeds.indexOfFirst { it == currentSpeed }.coerceAtLeast(0)
        
        android.app.AlertDialog.Builder(context)
            .setTitle(R.string.playbackSpeed)
            .setSingleChoiceItems(speedLabels, selectedIndex) { dialog, which ->
                controller.setSpeed(speeds[which])
                SPReader.setRecitationSpeed(context, speeds[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun openOptionsMenu() {
        Toast.makeText(context, "Options menu", Toast.LENGTH_SHORT).show()
    }

    // ==================== Utilities ====================

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
