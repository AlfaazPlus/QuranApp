package com.quranapp.android.platform.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.quranapp.android.shared.R
import com.quranapp.android.utils.app.Logger

class AndroidShortcutManager(
    private val appContext: Context,
) : BaseShortcutManager() {
    override fun pushShortcut(shortcut: AppShortcut) {
        try {
            val builder = ShortcutInfoCompat.Builder(appContext, shortcut.id)
                .setShortLabel(shortcut.shortLabel)

            val longLabel = shortcut.longLabel
            if (!longLabel.isNullOrBlank()) {
                builder.setLongLabel(longLabel)
            }

            if (shortcut.rank > 0) {
                builder.setRank(shortcut.rank)
            }

            resolveIcon(appContext, shortcut.icon)?.let { builder.setIcon(it) }

            val intent = resolveIntent(appContext, shortcut.intent)
            if (intent != null) {
                builder.setIntent(intent)
            }

            ShortcutManagerCompat.pushDynamicShortcut(appContext, builder.build())
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidShortcutManager")
        }
    }

    override fun removeShortcuts(ids: List<String>) {
        if (ids.isEmpty()) return
        try {
            ShortcutManagerCompat.removeDynamicShortcuts(appContext, ids)
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidShortcutManager")
        }
    }

    override fun removeAllShortcuts() {
        try {
            ShortcutManagerCompat.removeAllDynamicShortcuts(appContext)
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidShortcutManager")
        }
    }

    override fun isDynamicShortcutsSupported(): Boolean {
        return try {
            ShortcutManagerCompat.getMaxShortcutCountPerActivity(appContext) > 0
        } catch (_: Exception) {
            false
        }
    }

    override fun getDynamicShortcuts(): List<AppShortcut> {
        return try {
            ShortcutManagerCompat.getDynamicShortcuts(appContext).map { compat ->
                AppShortcut(
                    id = compat.id,
                    shortLabel = compat.shortLabel.toString().orEmpty(),
                    longLabel = compat.longLabel?.toString(),
                    rank = compat.rank,
                    isEnabled = compat.isEnabled,
                    disabledMessage = compat.disabledMessage?.toString(),
                )
            }
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidShortcutManager")
            emptyList()
        }
    }

    override fun disableShortcuts(ids: List<String>, disabledMessage: String?) {
        if (ids.isEmpty()) return
        try {
            ShortcutManagerCompat.disableShortcuts(appContext, ids, disabledMessage)
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidShortcutManager")
        }
    }

    override fun enableShortcuts(ids: List<String>) {
        if (ids.isEmpty()) return
        try {
            val infos = ids.map { ShortcutInfoCompat.Builder(appContext, it).build() }
            ShortcutManagerCompat.enableShortcuts(appContext, infos)
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidShortcutManager")
        }
    }

    private fun resolveIcon(context: Context, icon: ShortcutIcon?): IconCompat? {
        if (icon == null) return null

        return try {
            when (icon) {
                is ShortcutIcon.Platform -> icon.nativePayload as? IconCompat
                is ShortcutIcon.Resource -> {
                    if (icon.resId != null && icon.resId != 0) {
                        IconCompat.createWithResource(context, icon.resId)
                    } else when (icon.name) {
                        "dr_ic_shortcut_votd" -> IconCompat.createWithResource(
                            context,
                            R.drawable.dr_ic_shortcut_votd
                        )

                        "icon_launcher" -> IconCompat.createWithResource(
                            context,
                            R.mipmap.icon_launcher
                        )

                        else -> {
                            val name = icon.name
                            if (!name.isNullOrEmpty()) {
                                val resId = context.resources.getIdentifier(
                                    name,
                                    "drawable",
                                    context.packageName
                                )
                                    .takeIf { it != 0 }
                                    ?: context.resources.getIdentifier(
                                        name,
                                        "mipmap",
                                        context.packageName
                                    )
                                if (resId != 0) {
                                    IconCompat.createWithResource(context, resId)
                                } else null
                            } else null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.saveError(e, "AndroidShortcutManager")
            null
        }
    }

    private fun resolveIntent(context: Context, intent: ShortcutIntent?): Intent? {
        if (intent == null) return null

        return when (intent) {
            is ShortcutIntent.Platform -> intent.nativePayload as? Intent

            is ShortcutIntent.DeepLink -> {
                Intent(Intent.ACTION_VIEW, intent.uri.toUri()).apply {
                    intent.extras.forEach { (key, value) ->
                        putIntentExtra(this, key, value)
                    }
                }
            }

            is ShortcutIntent.Activity -> {
                Intent().apply {
                    if (!intent.destination.isNullOrEmpty()) {
                        if (intent.destination.contains('.')) {
                            setClassName(context, intent.destination)
                        } else {
                            setClassName(context.packageName, intent.destination)
                        }
                    }
                    action = intent.action ?: Intent.ACTION_VIEW
                    intent.dataUri?.let { data = it.toUri() }
                    if (intent.flags != 0) {
                        flags = intent.flags
                    }
                    intent.extras.forEach { (key, value) ->
                        putIntentExtra(this, key, value)
                    }
                }
            }
        }
    }

    private fun putIntentExtra(intent: Intent, key: String, value: Any?) {
        when (value) {
            null -> intent.putExtra(key, null as String?)
            is String -> intent.putExtra(key, value)
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is Boolean -> intent.putExtra(key, value)
            is Float -> intent.putExtra(key, value)
            is Double -> intent.putExtra(key, value)
            is ByteArray -> intent.putExtra(key, value)
            is IntArray -> intent.putExtra(key, value)
            is LongArray -> intent.putExtra(key, value)
            is Array<*> -> {
                if (value.isArrayOf<String>()) {
                    @Suppress("UNCHECKED_CAST")
                    intent.putExtra(key, value as Array<String>)
                }
            }
        }
    }
}
