/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */
/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */
package com.quranapp.android.utils.sharedPrefs

import android.content.Context

object SPAppActions {
    private const val SP_APP_ACTION = "sp_app_action"

    // Fetch translations forced in ActivityTranslationDownload.
    private const val KEY_TRANSLATIONS_FETCH_FORCE = "app.action.translations.fetch_force"
    private const val KEY_RECITATIONS_FETCH_FORCE = "app.action.recitations.fetch_force"
    private const val KEY_RECITATION_TRANSLATIONS_FETCH_FORCE = "app.action.recitation_translations.fetch_force"
    private const val KEY_TAFSIRS_FETCH_FORCE = "app.action.tafsirs.fetch_force"
    private const val KEY_URLS_FETCH_FORCE = "app.action.urls.fetch_force"
    private const val KEY_APP_ACTION_SP_PENDING = "app.action.pending"
    private const val KEY_APP_ACTION_ONBOARDING_REQUIRED = "app.action.onboarding_required"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)

    @JvmStatic
    fun setFetchTranslationsForce(ctx: Context, fetchForce: Boolean) {
        sp(ctx).edit().apply {
            putBoolean(KEY_TRANSLATIONS_FETCH_FORCE, fetchForce)
            apply()
        }
    }

    @JvmStatic
    fun getFetchTranslationsForce(ctx: Context): Boolean = sp(ctx).getBoolean(
        KEY_TRANSLATIONS_FETCH_FORCE,
        false
    )

    @JvmStatic
    fun setFetchRecitationsForce(ctx: Context, fetchForce: Boolean) {
        sp(ctx).edit().apply {
            putBoolean(KEY_RECITATIONS_FETCH_FORCE, fetchForce)
            apply()
        }
    }

    @JvmStatic
    fun getFetchRecitationsForce(ctx: Context): Boolean = sp(ctx).getBoolean(
        KEY_RECITATIONS_FETCH_FORCE,
        false
    )

    @JvmStatic
    fun setFetchRecitationTranslationsForce(ctx: Context, fetchForce: Boolean) {
        sp(ctx).edit().apply {
            putBoolean(KEY_RECITATION_TRANSLATIONS_FETCH_FORCE, fetchForce)
            apply()
        }
    }

    @JvmStatic
    fun getFetchRecitationTranslationsForce(ctx: Context): Boolean = sp(ctx).getBoolean(
        KEY_RECITATION_TRANSLATIONS_FETCH_FORCE,
        false
    )

    @JvmStatic
    fun setFetchTafsirsForce(ctx: Context, fetchForce: Boolean) {
        sp(ctx).edit().apply {
            putBoolean(KEY_TAFSIRS_FETCH_FORCE, fetchForce)
            apply()
        }
    }

    @JvmStatic
    fun getFetchTafsirsForce(ctx: Context): Boolean = sp(ctx).getBoolean(
        KEY_TAFSIRS_FETCH_FORCE,
        false
    )

    @JvmStatic
    fun setFetchUrlsForce(ctx: Context, fetchForce: Boolean) {
        sp(ctx).edit().apply {
            putBoolean(KEY_URLS_FETCH_FORCE, fetchForce)
            apply()
        }
    }

    @JvmStatic
    fun getFetchUrlsForce(ctx: Context): Boolean = sp(ctx).getBoolean(
        KEY_URLS_FETCH_FORCE,
        false
    )

    @JvmStatic
    fun addToPendingAction(ctx: Context, action: String, victim: String?) {
        sp(ctx).apply {
            val pendingActionsSP = getStringSet(KEY_APP_ACTION_SP_PENDING, null)
            val pendingActions = if (!pendingActionsSP.isNullOrEmpty()) HashSet(pendingActionsSP) else HashSet()
            pendingActions.add(makePendingActionKey(action, victim))

            edit().apply {
                putStringSet(KEY_APP_ACTION_SP_PENDING, pendingActions)
                apply()
            }
        }
    }

    @JvmStatic
    fun removeFromPendingAction(ctx: Context, action: String, victim: String) {
        sp(ctx).apply {
            val pendingActionsSP = getStringSet(KEY_APP_ACTION_SP_PENDING, null)

            if (!pendingActionsSP.isNullOrEmpty()) {
                val pendingActions = HashSet<String>(pendingActionsSP)
                pendingActions.remove(makePendingActionKey(action, victim))

                edit().apply {
                    putStringSet(KEY_APP_ACTION_SP_PENDING, pendingActions)
                    apply()
                }
            }
        }
    }

    private fun makePendingActionKey(action: String, victim: String?): String {
        return if (victim.isNullOrEmpty()) action else "$action:$victim"
    }

    fun getPendingActions(ctx: Context): Set<String> {
        return sp(ctx).getStringSet(KEY_APP_ACTION_SP_PENDING, HashSet()) ?: HashSet()
    }

    @JvmStatic
    fun getRequireOnboarding(ctx: Context): Boolean {
        val sp = sp(ctx)

        if (!sp.contains(KEY_APP_ACTION_ONBOARDING_REQUIRED)) {
            setRequireOnboarding(ctx, true)
            return true
        }

        return sp.getBoolean(KEY_APP_ACTION_ONBOARDING_REQUIRED, false)
    }

    @JvmStatic
    fun setRequireOnboarding(ctx: Context, require: Boolean) {
        sp(ctx).edit().apply {
            putBoolean(KEY_APP_ACTION_ONBOARDING_REQUIRED, require)
            apply()
        }
    }
}
