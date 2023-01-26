package com.quranapp.android;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.app.NotificationUtils;
import com.quranapp.android.utils.fb.FirebaseUtils;

import java.util.Objects;

public class QuranApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        initBeforeBaseAttach(base);
        super.attachBaseContext(base);
    }

    private void initBeforeBaseAttach(Context base) {
        updateTheme(base);
    }

    private void updateTheme(Context base) {
        AppCompatDelegate.setDefaultNightMode(AppUtils.resolveThemeModeFromSP(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createNotificationChannels(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String process = getProcessName();
            if (!Objects.equals(getPackageName(), process)) WebView.setDataDirectorySuffix(process);
        }

        FirebaseApp.initializeApp(this);
        FirebaseUtils.remoteConfig().fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Logger.print("Fetched and activated remoteConfig params");
            } else {
                Logger.print("Failed to fetch remoteConfig params");

                Exception exception = task.getException();
                if (exception != null) {
                    exception.printStackTrace();
                }
            }
        });

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                BuildConfig.DEBUG
                        ? DebugAppCheckProviderFactory.getInstance()
                        : PlayIntegrityAppCheckProviderFactory.getInstance()
        );
    }
}