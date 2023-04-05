package com.quranapp.android.utils.reader.recitation

import android.app.Notification
import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.quranapp.android.R

object RecitationNotificationHelper {
    fun createPreviousVerseAction(context: Context): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.dr_icon_player_seek_left, "Previous verse",
            MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        )
    }

    fun createNextVerseAction(context: Context): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.dr_icon_player_seek_right, "Next verse",
            MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        )
    }

    fun createNotificationBuilder(context: Context, session: MediaSessionCompat): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, context.getString(R.string.strNotifChannelIdRecitation))
            .setSmallIcon(R.drawable.dr_logo)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
    }
}