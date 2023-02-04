/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 26/7/2022.
 * All rights reserved.
 */
/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */
package com.quranapp.android.utils.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.BuildConfig
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.ActivityReference
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.quran.parser.ParserUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslFactory
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.services.TranslDownloadService
import com.quranapp.android.utils.sp.SPAppActions
import com.quranapp.android.utils.sp.SPAppConfigs
import com.quranapp.android.utils.sp.SPVerses
import com.quranapp.android.utils.univ.RegexPattern
import com.quranapp.android.utils.votd.VOTDUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppActions {
    const val APP_ACTION_KEY = "app.action.key"
    const val APP_ACTION_VICTIM_KEY = "app.action.victim_key"
    private const val APP_ACTION_UPDATE = "app.action.update"
    private const val APP_ACTION_PRIVACY_UPDATE = "app.action.privacy_update"
    private const val APP_ACTION_ABOUT_UPDATE = "app.action.about_update"
    private const val APP_ACTION_SUBS_REMINDER = "app.action.subscription_reminder"
    private const val APP_ACTION_TRANSL_DELETE = "app.action.translation.delete"
    const val APP_ACTION_TRANSL_UPDATE = "app.action.translation.update"
    private const val APP_ACTION_TRANSL_NEW = "app.action.translation.new"
    private const val APP_ACTION_TRANSL_INFO_UPDATE = "app.action.translation_info.update"
    private const val APP_ACTION_OPEN_LINK = "app.action.open_link"
    const val APP_ACTION_URLS_UPDATE = "app.action.urls_update"
    private const val APP_ACTION_OPEN_READER_JUZ = "app.action.open_reader_juz"
    private const val APP_ACTION_OPEN_READER_CHAPTER = "app.action.open_reader_chapter"
    private const val APP_ACTION_OPEN_READER_VERSES = "app.action.open_reader_verses"
    private const val APP_ACTION_OPEN_REFERENCES = "app.action.open_references"
    private const val APP_ACTION_REFERENCES_TITLE = "app.action.references_title"
    private const val APP_ACTION_REFERENCES_DESC = "app.action.references_desc"
    private const val APP_ACTION_REFERENCES_TRANSL_SLUGS = "app.action.references_transl_slugs"
    private const val APP_ACTION_REFERENCES_SHOW_CHAP_SUGG = "app.action.references_show_chap_sugg"

    /**
     * @param action     The action which is to be performed. Like [.APP_ACTION_TRANSL_UPDATE] etc.
     * @param victim     The victim may slug or null
     * @param fromNotif  If this action is being done from notification click.
     * @param wasPending If this action was pending.
     */
    @JvmStatic
    fun doAction(
        ctx: ContextWrapper, remoteMessage: RemoteMessage?, bundle: Bundle?, action: String, victim: String?,
        fromNotif: Boolean, wasPending: Boolean
    ) {
        if (!victim.isNullOrEmpty() && handleReaderOpener(ctx, remoteMessage, bundle, action, victim, fromNotif)) {
            return
        }
        if (APP_ACTION_UPDATE == action) {
            handleAppUpdate(ctx, remoteMessage, fromNotif)
        } else if (APP_ACTION_URLS_UPDATE == action) {
            UrlsManager.newInstance(ctx).updateUrls()
        } else if (APP_ACTION_TRANSL_NEW == action) {
            SPAppActions.setFetchTranslationsForce(ctx, true)
            if (fromNotif) {
                val intent = Intent(ctx, ActivitySettings::class.java)
                intent.putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, ActivitySettings.SETTINGS_TRANSL_DOWNLOAD)
                try {
                    intent.putExtra(TranslUtils.KEY_NEW_TRANSLATIONS, intArrayOf(victim!!.toInt()))
                } catch (ignored: NumberFormatException) {
                }
                ctx.startActivity(intent)
            }
        } else if (APP_ACTION_TRANSL_DELETE == action) {
            SPAppActions.setFetchTranslationsForce(ctx, true)
            if (victim != null) {
                val factory = QuranTranslFactory(ctx)
                factory.deleteTranslation(victim)
                factory.close()
            }
        } else if (APP_ACTION_TRANSL_UPDATE == action) {
            if (victim != null) {
                if (!wasPending) {
                    SPAppActions.addToPendingAction(ctx, action, victim)
                }
                updateTransl(ctx, victim)
            }
        } else if (APP_ACTION_TRANSL_INFO_UPDATE == action) {
            SPAppActions.setFetchTranslationsForce(ctx, true)
        } else if (APP_ACTION_OPEN_LINK == action) {
            if (victim != null) {
                handleOpenLink(ctx, remoteMessage, victim, fromNotif)
            }
        }
    }

    private fun handleReaderOpener(
        ctx: Context,
        remoteMessage: RemoteMessage?,
        bundle: Bundle?,
        action: String,
        victim: String,
        fromNotif: Boolean
    ): Boolean {
        try {
            var cls: Class<*> = ActivityReader::class.java
            var intent: Intent? = null
            if (APP_ACTION_OPEN_REFERENCES.equals(action, ignoreCase = true)) {
                intent = prepareReferenceIntent(bundle, victim)
                cls = ActivityReference::class.java
            } else if (APP_ACTION_OPEN_READER_JUZ.equals(action, ignoreCase = true)) {
                intent = ReaderFactory.prepareJuzIntent(victim.toInt())
            } else if (APP_ACTION_OPEN_READER_CHAPTER.equals(action, ignoreCase = true)) {
                intent = ReaderFactory.prepareChapterIntent(victim.toInt())
            } else if (APP_ACTION_OPEN_READER_VERSES.equals(action, ignoreCase = true)) {
                val mtchrVRangeJump = RegexPattern.VERSE_RANGE_JUMP_PATTERN.matcher(victim)
                val mtchrVJump = RegexPattern.VERSE_JUMP_PATTERN.matcher(victim)
                if (mtchrVRangeJump.find()) {
                    val r = mtchrVRangeJump.toMatchResult()
                    if (r.groupCount() >= 3) {
                        val chapNo = r.group(1).toInt()
                        var fromVerse = r.group(2).toInt()
                        var toVerse = r.group(3).toInt()

                        // swap
                        val tmpFrom = fromVerse
                        fromVerse = fromVerse.coerceAtMost(toVerse)
                        toVerse = tmpFrom.coerceAtLeast(toVerse)
                        intent = ReaderFactory.prepareVerseRangeIntent(chapNo, fromVerse, toVerse)
                    }
                } else if (mtchrVJump.find()) {
                    val r = mtchrVJump.toMatchResult()
                    if (r.groupCount() >= 2) {
                        val chapNo = r.group(1).toInt()
                        val verseNo = r.group(2).toInt()
                        intent = ReaderFactory.prepareSingleVerseIntent(chapNo, verseNo)
                    }
                }
            }
            if (intent != null) {
                intent.setClass(ctx, cls)
                if (fromNotif) {
                    ctx.startActivity(intent)
                } else {
                    val pendingIntent = PendingIntent.getActivity(ctx, 0, intent, notificationFlag)
                    showNotification(ctx, remoteMessage, pendingIntent)
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.reportError(e)
        }
        return false
    }

    private fun prepareReferenceIntent(bundle: Bundle?, versesStr: String): Intent {
        var title = ""
        var desc = ""
        var translSlugs = arrayOf<String>()
        var showChapSugg = false
        if (bundle != null && bundle.size() > 0) {
            title = bundle.getString(APP_ACTION_REFERENCES_TITLE, "")
            desc = bundle.getString(APP_ACTION_REFERENCES_DESC, "")
            val translSlugsStr = bundle.getString(APP_ACTION_REFERENCES_TRANSL_SLUGS, "")
            if (!TextUtils.isEmpty(translSlugsStr)) {
                translSlugs = translSlugsStr.split(",").toTypedArray()
            }
            showChapSugg = bundle.getBoolean(APP_ACTION_REFERENCES_SHOW_CHAP_SUGG, false)
        }
        val verses = ParserUtils.prepareVersesList(versesStr, true)
        val chapters = ParserUtils.prepareChaptersList(verses)
        return ReaderFactory.prepareReferenceVerseIntent(showChapSugg, title, desc, translSlugs, chapters, verses)
    }

    private fun handleAppUpdate(ctx: Context, remoteMessage: RemoteMessage?, fromNotif: Boolean) {
        if (fromNotif) {
            AppBridge.newOpener(ctx).openPlayStore()
            return
        }
        if (remoteMessage == null) {
            return
        }
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse(AppBridge.preparePlayStoreLink(ctx, false)))
        val pendingIntent = PendingIntent.getActivity(ctx, 0, playStoreIntent, notificationFlag)
        showNotification(ctx, remoteMessage, pendingIntent)
    }

    private fun handleOpenLink(ctx: Context, remoteMessage: RemoteMessage?, victim: String, fromNotif: Boolean) {
        if (fromNotif) {
            AppBridge.newOpener(ctx).browseLink(victim)
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(victim))
        val pendingIntent = PendingIntent.getActivity(ctx, 0, intent, notificationFlag)
        showNotification(ctx, remoteMessage, pendingIntent)
    }

    @JvmStatic
    fun doPendingActions(ctx: ContextWrapper) {
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance()
                .token
                .addOnCompleteListener { task: Task<String> ->
                    if (!task.isSuccessful) {
                        Logger.print("FCM token: FAILED", task.exception)
                        return@addOnCompleteListener
                    }
                    print("FCM token: " + task.result)
                }
        }
        val pendingActions = SPAppActions.getPendingActions(ctx)
        for (pendingAction in pendingActions) {
            val split = pendingAction.split(":").toTypedArray()
            val action = split[0]
            var victim: String? = null
            if (split.size > 1) {
                victim = split[1]
            }
            doAction(ctx, null, null, action, victim, false, true)
        }
        scheduleActions(ctx)
    }

    private fun updateTransl(ctx: ContextWrapper, slug: String) {
        val factory = QuranTranslFactory(ctx)
        val translationExists = factory.isTranslationDownloaded(slug)
        if (translationExists) {
            val bookInfo = factory.getTranslationBookInfo(slug)

            // The slug could be empty. Check factory.getTranslationBookInfo(slug) for more info.
            if (bookInfo.slug.isNotEmpty()) {
                TranslDownloadService.startDownloadService(ctx, bookInfo)
            }
        }
        factory.close()
    }

    private val notificationFlag: Int
        get() {
            var flag = PendingIntent.FLAG_ONE_SHOT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flag = flag or PendingIntent.FLAG_IMMUTABLE
            }
            return flag
        }

    private fun showNotification(ctx: Context, remoteMessage: RemoteMessage?, pendingIntent: PendingIntent) {
        if (remoteMessage == null) {
            return
        }
        val notification = remoteMessage.notification ?: return
        val channelId = ctx.getString(R.string.strNotifChannelIdDefault)
        val builder = NotificationCompat.Builder(ctx, channelId)
        builder.setSmallIcon(R.drawable.dr_logo)
        builder.setContentTitle(notification.title)
        builder.setContentText(notification.body)
        builder.setAutoCancel(true)
        builder.setContentIntent(pendingIntent)
        val manager = ContextCompat.getSystemService(ctx, NotificationManager::class.java)
        manager?.notify(0, builder.build())
    }

    private fun scheduleActions(ctx: Context) {
        if (SPVerses.getVOTDReminderEnabled(ctx)) {
            VOTDUtils.enableVOTDReminder(ctx)
        }
    }

    /**
     * Checks if there has been changes in the app resources on the server.
     * If there has been changes, then the upcoming versions from the remote config will be greater than that of locally saved.
     * */
    @JvmStatic
    fun checkForResourcesVersions(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (urlsVersion, translationsVersion, recitationsVersion) = RetrofitInstance.github.getResourcesVersions()

                val localUrlsVersion = SPAppConfigs.getUrlsVersion(ctx)
                val localTranslationsVersion = SPAppConfigs.getTranslationsVersion(ctx)
                val localRecitationsVersion = SPAppConfigs.getRecitationsVersion(ctx)

                Logger.print("RESOURCE VERSIONS: URLs: local: $localUrlsVersion, server: $urlsVersion")
                if (urlsVersion > localUrlsVersion) {
                    SPAppActions.setFetchUrlsForce(ctx, true)
                    SPAppConfigs.setUrlsVersion(ctx, urlsVersion)
                    Logger.print("Updated URLs version from $localUrlsVersion to $urlsVersion")
                }

                Logger.print("RESOURCE VERSIONS: TRANSLATIONS: local: $localTranslationsVersion, server: $translationsVersion")
                if (translationsVersion > localTranslationsVersion) {
                    SPAppActions.setFetchTranslationsForce(ctx, true)
                    SPAppConfigs.setTranslationsVersion(ctx, translationsVersion)
                    Logger.print("Updated translations version from $localTranslationsVersion to $translationsVersion")
                }

                Logger.print("RESOURCE VERSIONS: RECITATIONS: local: $localRecitationsVersion, server: $recitationsVersion")
                if (recitationsVersion > localRecitationsVersion) {
                    SPAppActions.setFetchRecitationsForce(ctx, true)
                    SPAppConfigs.setRecitationsVersion(ctx, recitationsVersion)
                    Logger.print("Updated recitations version from $localRecitationsVersion to $recitationsVersion")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}