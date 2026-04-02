package com.quranapp.android.utils.univ

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


data class MessageEvent(val message: String?)
data class ErrorEvent(val message: String?)

object EventBus {
    private val _events = MutableSharedFlow<Any>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val events = _events.asSharedFlow()

    suspend fun send(event: Any) {
        _events.emit(event)
    }

    fun trySend(event: Any) {
        _events.tryEmit(event)
    }
}