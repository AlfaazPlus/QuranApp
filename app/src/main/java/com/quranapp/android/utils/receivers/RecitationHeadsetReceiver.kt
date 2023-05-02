package com.quranapp.android.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.services.RecitationService

class RecitationHeadsetReceiver(private val service: RecitationService) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AudioManager.ACTION_HEADSET_PLUG == intent.action) {
            when (val state = intent.getIntExtra("state", -1)) {
                0 -> {
                    service.pauseMedia()
                    service.p.pausedDueToHeadset = true
                }

                1 -> {
                    if (service.p.pausedDueToHeadset) {
                        service.p.pausedDueToHeadset = false
                        service.playMedia()
                    }
                }

                else -> Log.d("Headset state: $state")
            }
        }
        // wireless headset
        else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
            service.pauseMedia()
            service.p.pausedDueToHeadset = true
        }
    }
}