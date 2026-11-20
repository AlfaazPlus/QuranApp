package com.quranapp.android.viewmodel

import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RecitationPlayerViewModel() : AppViewModel() {
    val controller = Platform.audio.controller
    val repository get() = DatabaseProvider.getQuranRepository()

    init {
        controller.connect()
    }

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }

    val state: StateFlow<RecitationServiceState> = controller.state

    val isPlaying: StateFlow<Boolean> = controller.isPlayingState

    val isLoading: StateFlow<Boolean> = combine(
        controller.state,
        controller.isBufferingState,
    ) { currentState, isBuffering ->
        currentState.resolvingChapterNo != null || isBuffering
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = controller.isLoading,
    )
}
