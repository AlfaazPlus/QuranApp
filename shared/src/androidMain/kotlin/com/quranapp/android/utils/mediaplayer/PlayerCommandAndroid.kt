package com.quranapp.android.utils.mediaplayer

import android.os.Bundle

private const val KEY_COMMAND = "command"

fun Bundle.putCommand(command: BasePlayerCommand) {
    putString(KEY_COMMAND, command.encode())
}

fun Bundle.toPlayerCommand(): BasePlayerCommand? =
    getString(KEY_COMMAND)?.let(::decodePlayerCommand)

val BasePlayerCommand.action: String
    get() = when (this) {
        is StartCommand -> StartCommand.ACTION
        is SetAudioOptionCommand -> SetAudioOptionCommand.ACTION
        is SetVerseGroupSizeCommand -> SetVerseGroupSizeCommand.ACTION
        is SetPlaybackSpeedCommand -> SetPlaybackSpeedCommand.ACTION
        is SetRepeatCommand -> SetRepeatCommand.ACTION
        is SetReciterCommand -> SetReciterCommand.ACTION
        is SeekToPositionCommand -> SeekToPositionCommand.ACTION
        is SetAudioEndBehaviourCommand -> SetAudioEndBehaviourCommand.ACTION
        StopCommand -> StopCommand.ACTION
        PreviousVerseCommand -> PreviousVerseCommand.ACTION
        NextVerseCommand -> NextVerseCommand.ACTION
        ClearModeCommand -> ClearModeCommand.ACTION
    }

val ALL_PLAYER_ACTIONS = arrayOf(
    StartCommand.ACTION,
    SetAudioOptionCommand.ACTION,
    SetPlaybackSpeedCommand.ACTION,
    SetVerseGroupSizeCommand.ACTION,
    SetRepeatCommand.ACTION,
    SetAudioEndBehaviourCommand.ACTION,
    SetReciterCommand.ACTION,
    SeekToPositionCommand.ACTION,
    StopCommand.ACTION,
    PreviousVerseCommand.ACTION,
    NextVerseCommand.ACTION,
    ClearModeCommand.ACTION,
)
