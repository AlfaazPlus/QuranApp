/*
 * Created by Faisal Khan on (c) 23/8/2021.
 */

package com.quranapp.android.utils.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

public final class ReceiverUtils {
    public static void enableReceiver(Context context, Class<? extends BroadcastReceiver> receiverClass) {
        ComponentName receiver = new ComponentName(context, receiverClass);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP);
    }

    public static void disableReceiver(Context context, Class<? extends BroadcastReceiver> receiverClass) {
        ComponentName receiver = new ComponentName(context, receiverClass);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP);
    }
}
