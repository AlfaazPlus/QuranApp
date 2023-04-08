package com.quranapp.android.utils.reader.recitation

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import com.peacedesign.android.utils.DrawableUtils
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.services.RecitationPlayerService

class RecitationNotificationHelper(private val service: RecitationPlayerService) {
    private val notifManager by lazy { NotificationManagerCompat.from(service) }
    private val notifActionPrev by lazy { createPreviousVerseAction() }
    private val notifActionNext by lazy { createNextVerseAction() }
    private var notifBuilder: NotificationCompat.Builder? = null
    private var notifTitle: String? = null
    private var notifDescription: String? = null
    private val albumArt by lazy {
        DrawableUtils.getBitmapFromDrawable(service.drawable(R.drawable.dr_quran_wallpaper), 256, 256)
    }

    fun prepareMetadata(quranMeta: QuranMeta?): MediaMetadataCompat {
        val recParams = service.recParams
        val chapterName = quranMeta?.getChapterName(service, recParams.currentChapterNo) ?: ""
        notifTitle = service.getString(
            R.string.strLabelVerseWithChapNameAndNo,
            chapterName,
            recParams.currentChapterNo,
            recParams.currentVerseNo
        )

        notifDescription = RecitationUtils.getReciterName(recParams.currentReciter)

        return MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, notifTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, notifDescription)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, service.duration.toLong())
            .build()
    }

    private fun createPreviousVerseAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.dr_icon_player_seek_left, "Previous verse",
            MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        )
    }

    private fun createNextVerseAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.dr_icon_player_seek_right, "Next verse",
            MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        )
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(service, service.getString(R.string.strNotifChannelIdRecitation))
            .setSmallIcon(R.drawable.dr_logo)
            .setLargeIcon(albumArt)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(service.session!!.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
    }

    fun getInitialNotification(): Notification {
        val builder = createNotificationBuilder()
            .addAction(notifActionPrev)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.dr_icon_play_verse,
                    "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_PLAY
                    )
                )
            )
            .addAction(notifActionNext)

        return builder.build()
    }

    fun showNotification(action: Long) {
        if (notifBuilder == null) {
            notifBuilder = createNotificationBuilder()
        }

        val isPlay = action == PlaybackStateCompat.ACTION_PLAY

        notifBuilder = notifBuilder!!.setContentTitle(notifTitle)
            .setContentText(notifDescription)
            .clearActions()
            .addAction(notifActionPrev)
            .addAction(
                NotificationCompat.Action(
                    if (isPlay) R.drawable.dr_icon_pause_verse
                    else R.drawable.dr_icon_play_verse,
                    if (isPlay) "Pause"
                    else "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        if (isPlay) PlaybackStateCompat.ACTION_PAUSE
                        else PlaybackStateCompat.ACTION_PLAY
                    )
                )
            )
            .addAction(notifActionNext)

        if (ActivityCompat.checkSelfPermission(
                service,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notifManager.notify(RecitationPlayerService.NOTIF_ID, notifBuilder!!.build())
    }
}