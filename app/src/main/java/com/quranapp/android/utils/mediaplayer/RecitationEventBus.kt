package com.quranapp.android.utils.mediaplayer

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RecitationEventBus {
    private val _events = MutableSharedFlow<PlayerEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events = _events.asSharedFlow()

    suspend fun send(event: PlayerEvent) {
        _events.emit(event)
    }
}