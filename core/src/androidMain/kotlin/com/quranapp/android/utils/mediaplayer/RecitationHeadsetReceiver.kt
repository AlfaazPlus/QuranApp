package com.quranapp.android.utils.mediaplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.quranapp.android.utils.Log

class RecitationHeadsetReceiver(private val service: RecitationService) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AudioManager.ACTION_HEADSET_PLUG == intent.action) {
            when (val state = intent.getIntExtra("state", -1)) {
                0 -> {
                    service.pauseMedia(PlayerInterationSource.HEADSET)
                }

                1 -> {
                    if (service.state.value.pausedByHeadset) {
                        service.playMedia()
                    }
                }

                else -> Log.d("Headset state: $state")
            }
        }
        // wireless headset
        else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
            service.pauseMedia(PlayerInterationSource.HEADSET)
        }
    }
}