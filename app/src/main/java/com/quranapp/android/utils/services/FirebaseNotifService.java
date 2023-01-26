/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */

package com.quranapp.android.utils.services;

import static com.quranapp.android.utils.app.AppActions.APP_ACTION_KEY;
import static com.quranapp.android.utils.app.AppActions.APP_ACTION_VICTIM_KEY;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.quranapp.android.utils.app.AppActions;
import com.peacedesign.android.utils.Log;

import java.util.Map;

public class FirebaseNotifService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d(s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Bundle bundle = new Bundle();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }

            String action = data.get(APP_ACTION_KEY);
            if (!TextUtils.isEmpty(action)) {
                String victim = data.get(APP_ACTION_VICTIM_KEY);
                AppActions.doAction(this, remoteMessage, bundle, action, victim, false, false);
            }
        }
    }
}
