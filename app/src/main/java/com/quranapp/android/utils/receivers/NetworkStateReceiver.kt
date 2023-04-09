package com.quranapp.android.utils.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.NonNull;

import com.quranapp.android.utils.univ.MessageUtils;

import java.util.HashSet;
import java.util.Set;

public class NetworkStateReceiver extends BroadcastReceiver {
    private String prevNetworkStatus;
    private final Set<NetworkStateReceiverListener> listeners;
    private boolean connected;

    public NetworkStateReceiver() {
        listeners = new HashSet<>();
        connected = false;
    }

    public static boolean isNetworkConnected(@NonNull Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();
        return ni != null && ni.getState() == NetworkInfo.State.CONNECTED;
    }

    public static IntentFilter getIntentFilter() {
        return new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    public static boolean canProceed(@NonNull Context context) {
        return canProceed(context, true, null);
    }

    public static boolean canProceed(@NonNull Context context, boolean cancelable, Runnable runOnDismissIfCantProceed) {
        if (!isNetworkConnected(context)) {
            MessageUtils.popNoInternetMessage(context, cancelable, runOnDismissIfCantProceed);
            return false;
        }
        return true;
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (intent.getExtras() == null) return;
        if (isNetworkConnected(context)) {
            connected = true;
        } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            connected = false;
        }

        notifyStateToAll();
    }

    private void notifyStateToAll() {
        for (NetworkStateReceiverListener listener : listeners) notifyState(listener);
    }

    private void notifyState(NetworkStateReceiverListener listener) {
        if (listener == null) return;

        if (connected) {
            String available = "AVAILABLE";
            if (available.equals(prevNetworkStatus)) return;
            prevNetworkStatus = available;

            listener.networkAvailable();
        } else {
            String unavailable = "UNAVAILABLE";
            if (unavailable.equals(prevNetworkStatus)) return;
            prevNetworkStatus = unavailable;

            listener.networkUnavailable();
        }
    }

    public void addListener(@NonNull NetworkStateReceiverListener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull NetworkStateReceiverListener listener) {
        listeners.remove(listener);
    }

    public interface NetworkStateReceiverListener {
        void networkAvailable();

        void networkUnavailable();
    }
}