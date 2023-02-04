/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */
/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */
package com.quranapp.android.utils.sp

import android.content.Context
import android.text.TextUtils

object SPAppActions {
    private const val SP_APP_ACTION = "sp_app_action"

    // Fetch translations forced in ActivityTranslationDownload.
    private const val KEY_APP_ACTION_SP_TRANSLS_FETCH_FORCE = "app.action.translations.fetch_force"
    private const val KEY_APP_ACTION_SP_RECITATIONS_FETCH_FORCE = "app.action.recitations.fetch_force"
    private const val KEY_APP_ACTION_SP_URLS_FETCH_FORCE = "app.action.urls.fetch_force"
    private const val KEY_APP_ACTION_SP_PENDING = "app.action.pending"
    private const val KEY_APP_ACTION_ONBOARDING_REQUIRED = "app.action.onboarding_required"

    @JvmStatic
    fun setFetchTranslationsForce(ctx: Context, fetchForce: Boolean) {
        val editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_APP_ACTION_SP_TRANSLS_FETCH_FORCE, fetchForce)
        editor.apply()
    }

    @JvmStatic
    fun getFetchTranslationsForce(ctx: Context): Boolean {
        val sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_APP_ACTION_SP_TRANSLS_FETCH_FORCE, false)
    }

    @JvmStatic
    fun setFetchRecitationsForce(ctx: Context, fetchForce: Boolean) {
        val editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_APP_ACTION_SP_RECITATIONS_FETCH_FORCE, fetchForce)
        editor.apply()
    }

    @JvmStatic
    fun getFetchRecitationsForce(ctx: Context): Boolean {
        val sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_APP_ACTION_SP_RECITATIONS_FETCH_FORCE, false)
    }

    @JvmStatic
    fun setFetchUrlsForce(ctx: Context, fetchForce: Boolean) {
        val editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_APP_ACTION_SP_URLS_FETCH_FORCE, fetchForce)
        editor.apply()
    }

    @JvmStatic
    fun getFetchUrlsForce(ctx: Context): Boolean {
        val sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_APP_ACTION_SP_URLS_FETCH_FORCE, false)
    }

    @JvmStatic
    fun addToPendingAction(ctx: Context, action: String, victim: String) {
        val sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)
        val pendingActionsSP = sp.getStringSet(KEY_APP_ACTION_SP_PENDING, null)
        val pendingActions = if (pendingActionsSP != null && pendingActionsSP.isNotEmpty()) {
            HashSet(pendingActionsSP)
        } else {
            HashSet()
        }
        pendingActions.add(makePendingActionKey(action, victim))
        val editor = sp.edit()
        editor.putStringSet(KEY_APP_ACTION_SP_PENDING, pendingActions)
        editor.apply()
    }

    @JvmStatic
    fun removeFromPendingAction(ctx: Context, action: String, victim: String) {
        val sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)
        val pendingActionsSP = sp.getStringSet(KEY_APP_ACTION_SP_PENDING, null)
        if (pendingActionsSP != null && pendingActionsSP.isNotEmpty()) {
            val pendingActions: MutableSet<String> = HashSet(pendingActionsSP)
            pendingActions.remove(makePendingActionKey(action, victim))
            val editor = sp.edit()
            editor.putStringSet(KEY_APP_ACTION_SP_PENDING, pendingActions)
            editor.apply()
        }
    }

    private fun makePendingActionKey(action: String, victim: String): String {
        return if (TextUtils.isEmpty(victim)) {
            action
        } else "$action:$victim"
    }

    fun getPendingActions(ctx: Context): Set<String> {
        val sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)
        return sp.getStringSet(KEY_APP_ACTION_SP_PENDING, HashSet()) ?: HashSet()
    }

    @JvmStatic
    fun getRequireOnboarding(ctx: Context): Boolean {
        val sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE)
        if (!sp.contains(KEY_APP_ACTION_ONBOARDING_REQUIRED)) {
            setRequireOnboarding(ctx, true)
            return true
        }
        return sp.getBoolean(KEY_APP_ACTION_ONBOARDING_REQUIRED, false)
    }

    @JvmStatic
    fun setRequireOnboarding(ctx: Context, require: Boolean) {
        val editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_APP_ACTION_ONBOARDING_REQUIRED, require)
        editor.apply()
    }

}