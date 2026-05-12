package com.quranapp.android.utils.mediaplayer

import androidx.annotation.OptIn
import androidx.compose.ui.util.fastCoerceIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.mediaplayer.RecitationService.Companion.ACTION_SEEK_LEFT
import com.quranapp.android.utils.mediaplayer.RecitationService.Companion.ACTION_SEEK_RIGHT

@OptIn(UnstableApi::class)
class SessionPlayer(
    private val service: RecitationService,
    val exoPlayer: ExoPlayer
) : ForwardingPlayer(exoPlayer) {

    private val clipPlan get() = service._verseClipPlan.value

    private var lastVerse: ChapterVersePair? = null
    private var cachedTimeline: Timeline? = null
    private var cachedTimelineDuration: Long = -1L
    private var cachedSessionMediaItem: MediaItem? = null

    override fun getDuration(): Long = clipPlan?.virtualDurationMs ?: exoPlayer.duration.let {
        if (it > 0 && it != C.TIME_UNSET) it else 0L
    }

    override fun getCurrentPosition(): Long =
        clipPlan?.virtualPosition(exoPlayer) ?: exoPlayer.currentPosition.let {
            if (it > 0 && it != C.TIME_UNSET) it else 0L
        }

    override fun getContentPosition(): Long = getCurrentPosition()

    override fun getBufferedPosition(): Long {
        val plan = clipPlan ?: return exoPlayer.bufferedPosition

        return plan.virtualPositionAt(
            exoPlayer.currentMediaItemIndex,
            exoPlayer.bufferedPosition
        )
    }

    override fun getBufferedPercentage(): Int {
        val duration = getDuration()

        return if (duration > 0) (getBufferedPosition() * 100 / duration).toInt()
            .fastCoerceIn(0, 100) else 0
    }

    override fun getTotalBufferedDuration(): Long {
        return (getBufferedPosition() - getCurrentPosition()).coerceAtLeast(0L)
    }

    override fun isCurrentMediaItemDynamic(): Boolean = false

    override fun isCurrentMediaItemSeekable(): Boolean = true

    override fun getCurrentMediaItemIndex(): Int = 0

    override fun getCurrentMediaItem(): MediaItem? {
        val realItem = exoPlayer.currentMediaItem ?: return null
        val currentVerse = service.state.value.currentVerse

        if (currentVerse == lastVerse &&
            realItem.mediaMetadata == cachedSessionMediaItem?.mediaMetadata &&
            cachedSessionMediaItem != null
        ) {
            return cachedSessionMediaItem
        }

        lastVerse = currentVerse

        val item = if (currentVerse.chapterNo <= 0) realItem else {
            MediaItem.Builder()
                .setMediaId("chapter_${currentVerse.chapterNo}")
                .setUri(realItem.requestMetadata.mediaUri ?: realItem.localConfiguration?.uri)
                .setMediaMetadata(realItem.mediaMetadata)
                .build()
        }

        cachedSessionMediaItem = item

        return item
    }

    override fun getMediaItemCount(): Int = 1

    override fun getMediaItemAt(index: Int): MediaItem =
        if (index == 0) getCurrentMediaItem()!! else super.getMediaItemAt(index)

    override fun getNextMediaItemIndex(): Int = C.INDEX_UNSET

    override fun getPreviousMediaItemIndex(): Int = C.INDEX_UNSET

    override fun hasNextMediaItem(): Boolean = false

    override fun hasPreviousMediaItem(): Boolean = false

    override fun getCurrentTimeline(): Timeline {
        val durationMs = getDuration()
        val currentMediaItem = getCurrentMediaItem()

        if (durationMs <= 0 || currentMediaItem == null) {
            return super.getCurrentTimeline()
        }

        if (durationMs == cachedTimelineDuration && currentMediaItem === cachedSessionMediaItem && cachedTimeline != null) {
            return cachedTimeline!!
        }

        val timeline = object : Timeline() {
            override fun getWindowCount(): Int = 1

            override fun getWindow(
                windowIndex: Int,
                window: Window,
                defaultPositionProjectionUs: Long
            ): Window {
                val durationUs = if (durationMs > 0) durationMs * 1000L else C.TIME_UNSET

                window.set(
                    Window.SINGLE_WINDOW_UID,
                    currentMediaItem,
                    null,
                    C.TIME_UNSET,
                    C.TIME_UNSET,
                    C.TIME_UNSET,
                    true,
                    false,
                    null,
                    0,
                    durationUs,
                    0,
                    0,
                    0
                )
                return window
            }

            override fun getPeriodCount(): Int = 1

            override fun getPeriod(
                periodIndex: Int,
                period: Period,
                setIdentifiers: Boolean
            ): Period {
                val durationUs = if (durationMs > 0) durationMs * 1000L else C.TIME_UNSET
                period.set(
                    Window.SINGLE_WINDOW_UID,
                    Window.SINGLE_WINDOW_UID,
                    0,
                    durationUs,
                    0
                )
                return period
            }

            override fun getIndexOfPeriod(uid: Any): Int =
                if (uid == Window.SINGLE_WINDOW_UID) 0 else C.INDEX_UNSET

            override fun getUidOfPeriod(periodIndex: Int): Any = Window.SINGLE_WINDOW_UID
        }

        cachedTimeline = timeline
        cachedTimelineDuration = durationMs
        cachedSessionMediaItem = currentMediaItem

        return timeline
    }

    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return command == Player.COMMAND_SEEK_TO_NEXT
                || command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                || command == Player.COMMAND_SEEK_TO_PREVIOUS
                || command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                || super.isCommandAvailable(command)
    }

    override fun seekTo(amountOrDirection: Long) {
        val state = service.state

        if (state.value.resolvingChapterNo != null) return

        service.invalidateRepeatSchedule()

        val SEEK_STEP_MS = 5000L
        val isRelative = amountOrDirection == ACTION_SEEK_LEFT ||
                amountOrDirection == ACTION_SEEK_RIGHT

        val plan = clipPlan

        if (plan != null && !plan.isEmpty) {
            val maxDuration = plan.virtualDurationMs.coerceAtLeast(0L)
            val target = if (isRelative) {
                val delta = if (amountOrDirection == ACTION_SEEK_RIGHT) SEEK_STEP_MS
                else -SEEK_STEP_MS

                (plan.virtualPosition(exoPlayer) + delta).coerceIn(0L, maxDuration)
            } else {
                amountOrDirection.coerceIn(0L, maxDuration)
            }

            plan.seekToVirtualPosition(exoPlayer, target)
        } else {
            val d = duration
            val upper = if (d == C.TIME_UNSET || d < 0) Long.MAX_VALUE else d

            val target = if (isRelative) {
                val delta =
                    if (amountOrDirection == ACTION_SEEK_RIGHT) SEEK_STEP_MS else -SEEK_STEP_MS
                (currentPosition + delta).coerceIn(0L, upper)
            } else {
                amountOrDirection.coerceIn(0L, upper)
            }

            exoPlayer.seekTo(target)
        }
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (mediaItemIndex == 0) {
            seekTo(positionMs)
        } else {
            super.seekTo(mediaItemIndex, positionMs)
        }
    }

    override fun seekToNext() {
        seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        seekToPreviousMediaItem()
    }

    override fun seekToNextMediaItem() {
        service.reciteNextVerse()
    }

    override fun seekToPreviousMediaItem() {
        service.recitePreviousVerse()
    }

    override fun getMediaMetadata(): MediaMetadata =
        getCurrentMediaItem()?.mediaMetadata ?: super.getMediaMetadata()
}
