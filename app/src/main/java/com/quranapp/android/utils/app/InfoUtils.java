/*
 * (c) Faisal Khan. Created on 21/11/2021.
 */

package com.quranapp.android.utils.app;

import static com.quranapp.android.utils.app.UrlsManager.URL_KEY_ABOUT;
import static com.quranapp.android.utils.app.UrlsManager.URL_KEY_FEEDBACK;
import static com.quranapp.android.utils.app.UrlsManager.URL_KEY_HELP;
import static com.quranapp.android.utils.app.UrlsManager.URL_KEY_PRIVACY_POLICY;
import static com.quranapp.android.utils.app.UrlsManager.newInstance;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.widget.dialog.loader.ProgressDialog;
import com.quranapp.android.R;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.univ.NotifUtils;

import java.util.concurrent.CancellationException;

public class InfoUtils {

    public static void openFeedbackPage(Context context) {
        openTab(context, URL_KEY_FEEDBACK);
    }

    public static void openPrivacyPolicy(Context context) {
        openTab(context, URL_KEY_PRIVACY_POLICY);
    }

    public static void openAbout(Context context) {
        openTab(context, URL_KEY_ABOUT);
    }

    public static void openHelp(Context context) {
        openTab(context, URL_KEY_HELP);
    }

    private static void openTab(Context context, String urlKey) {
        UrlsManager urlsManager = newInstance(context);

        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(R.string.strTextPleaseWait);
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.strLabelCancel), (dialog1, which) -> {
            urlsManager.cancel();
            dialog.dismiss();
        });
        dialog.show();

        OnResultReadyCallback<Exception> failedCallback = (e) -> {
            e.printStackTrace();
            dialog.dismiss();

            if (!(e instanceof CancellationException)) {
                Logger.reportError(e);

                String title = context.getString(R.string.strMsgSomethingWrong);
                String msg = context.getString(R.string.strMsgCouldNotOpenPage) + " " + context.getString(R.string.strMsgTryLater);
                NotifUtils.popMsg(context, title, msg, context.getString(R.string.strLabelClose), null);
            }
        };

        urlsManager.getUrlsJson(context, jsonObject -> {
            try {
                String url = jsonObject.getString(urlKey);
                //                urlsManager.updateIfRightUrlMissing(url);
                prepareCustomTab(context).launchUrl(context, Uri.parse(url));
                dialog.dismiss();
            } catch (Exception e) {
                failedCallback.onReady(e);
            }
        }, failedCallback);
    }

    private static CustomTabsIntent prepareCustomTab(Context context) {
        return new CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.colorBGPage))
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build();
    }
}
