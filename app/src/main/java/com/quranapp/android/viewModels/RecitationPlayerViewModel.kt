package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RecitationPlayerViewModel(application: Application) : AndroidViewModel(application) {
    val controller = RecitationController.getInstance(application)

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
        currentState.isResolving || isBuffering
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = controller.isLoading,
    )
}
