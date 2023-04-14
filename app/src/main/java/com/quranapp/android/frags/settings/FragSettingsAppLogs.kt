package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import com.quranapp.android.R

class FragSettingsAppLogs : FragSettingsBase() {
    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.appLogs)

    override val layoutResource = R.layout.frag_app_logs

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
    }
}