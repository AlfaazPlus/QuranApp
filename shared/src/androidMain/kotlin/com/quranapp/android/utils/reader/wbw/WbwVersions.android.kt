package com.quranapp.android.utils.reader.wbw

import com.quranapp.android.db.DatabaseProvider

internal actual fun readWbwItemVersion(id: String): Int {
    return WbwVersionStore(DatabaseProvider.appContext()).getItemVersion(id)
}

internal actual fun writeWbwItemVersion(id: String, version: Int) {
    WbwVersionStore(DatabaseProvider.appContext()).setItemVersion(id, version)
}
