package com.quranapp.android.utils.mediaplayer

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.quranapp.android.platform.mediaplayer.AudioClip

fun VerseClipPlan.virtualPosition(player: ExoPlayer): Long =
    virtualPositionAt(player.currentMediaItemIndex, player.currentPosition)

fun VerseClipPlan.seekToVirtualPosition(player: ExoPlayer, targetMs: Long) {
    if (isEmpty) return
    val (index, offset) = findClipForVirtualPosition(targetMs)
    player.seekTo(index, offset)
}

fun AudioClip.toMediaItem(): MediaItem {
    val builder = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri.toUri())

    metadata?.let { meta ->
        builder.setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(meta.title)
                .setArtist(meta.artist)
                .build(),
        )
    }

    if (isClipped) {
        val clipBuilder = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
        if (endMs != AudioClip.UNSET_TIME_MS && endMs > startMs) {
            clipBuilder.setEndPositionMs(endMs)
        }

        builder.setClippingConfiguration(clipBuilder.build())
    }

    return builder.build()
}
