package com.quranapp.android.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import com.quranapp.android.utils.univ.MessageUtils

class NetworkStateReceiver : BroadcastReceiver() {
    private var prevNetworkStatus: String? = null
    private val listeners = HashSet<NetworkStateReceiverListener>()
    private var connected = false

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.extras == null) return

        if (isNetworkConnected(context)) {
            connected = true
        } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            connected = false
        }

        notifyStateToAll()
    }

    private fun notifyStateToAll() {
        for (listener in listeners) notifyState(listener)
    }

    private fun notifyState(listener: NetworkStateReceiverListener?) {
        if (listener == null) return

        if (connected) {
            val available = "AVAILABLE"
            if (available == prevNetworkStatus) return

            prevNetworkStatus = available
            listener.networkAvailable()
        } else {
            val unavailable = "UNAVAILABLE"
            if (unavailable == prevNetworkStatus) return

            prevNetworkStatus = unavailable
            listener.networkUnavailable()
        }
    }

    fun addListener(listener: NetworkStateReceiverListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NetworkStateReceiverListener) {
        listeners.remove(listener)
    }

    interface NetworkStateReceiverListener {
        fun networkAvailable()
        fun networkUnavailable()
    }

    companion object {
        @Suppress("DEPRECATION")
        @JvmStatic
        fun isNetworkConnected(context: Context): Boolean {
            val mgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = mgr.activeNetwork ?: return false
                val actNw = mgr.getNetworkCapabilities(nw) ?: return false
                return when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    //for other device how are able to connect with Ethernet
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    //for check internet over Bluetooth
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                    else -> false
                }
            } else {
                return mgr.activeNetworkInfo?.state == NetworkInfo.State.CONNECTED
            }
        }

        @JvmStatic
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        @JvmOverloads
        fun canProceed(context: Context, cancelable: Boolean = true, runOnDismissIfCantProceed: Runnable? = null): Boolean {
            if (!isNetworkConnected(context)) {
                MessageUtils.popNoInternetMessage(context, cancelable, runOnDismissIfCantProceed)
                return false
            }
            return true
        }
    }
}