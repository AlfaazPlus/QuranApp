package com.quranapp.android.extensions

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strMsgNoInternetLong
import com.quranapp.android.platform.NetworkMonitor
import com.quranapp.android.platform.Platform
import org.jetbrains.compose.resources.getString

suspend fun NetworkMonitor.canProceed(): Boolean {
    if (!isConnected) {
        Platform.messages.show(
            getString(Res.string.strMsgNoInternetLong),
            long = true,
        )

        return false
    }

    return true
}
