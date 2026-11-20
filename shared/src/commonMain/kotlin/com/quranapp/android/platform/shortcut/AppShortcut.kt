package com.quranapp.android.platform.shortcut

data class AppShortcut(
    val id: String,
    val shortLabel: String,
    val longLabel: String? = null,
    val icon: ShortcutIcon? = null,
    val intent: ShortcutIntent? = null,
    val rank: Int = 0,
    val isEnabled: Boolean = true,
    val disabledMessage: String? = null,
)

sealed interface ShortcutIcon {
    data class Resource(
        val name: String? = null,
        val resId: Int? = null,
    ) : ShortcutIcon

    data class Platform(
        val nativePayload: Any,
    ) : ShortcutIcon
}

sealed interface ShortcutIntent {
    data class Activity(
        val destination: String? = null,
        val action: String? = null,
        val dataUri: String? = null,
        val extras: Map<String, Any?> = emptyMap(),
        val flags: Int = 0,
    ) : ShortcutIntent

    data class DeepLink(
        val uri: String,
        val extras: Map<String, Any?> = emptyMap(),
    ) : ShortcutIntent

    data class Platform(
        val nativePayload: Any,
    ) : ShortcutIntent
}
