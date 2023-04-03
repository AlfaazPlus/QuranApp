package com.quranapp.android.views.recitation

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.quranapp.android.activities.ActivityReader

class RecitationPlayerEventListener(private val player: RecitationPlayer, private val activity: ActivityReader) :
    Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                val params = player.params
                params.currentVerseCompleted = true

                val quranMeta = activity.mQuranMetaRef.get()
                val continueNext = params.continueRange && params.hasNextVerse(quranMeta)

                val verseValid = quranMeta.isVerseValid4Chapter(params.currentChapterNo, params.currentVerseNo + 1)
                val continueNextSingle = params.continueRange && activity.mReaderParams.isSingleVerse && verseValid

                params.currentRangeCompleted = !continueNext && !continueNextSingle

                if (continueNext || continueNextSingle) {
                    player.reciteNextVerse()
                } else {
                    player.pauseMedia()
                }
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
    }


}
