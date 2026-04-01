package com.quranapp.android.compose.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.BuildConfig
import com.quranapp.android.R
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.ListItem
import com.quranapp.android.utils.app.InfoUtils
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.univ.MessageUtils

private data class AboutItem(
    val icon: Int,
    val title: Int,
    val onClick: (context: Context) -> Unit
)

private val items = listOf(
    AboutItem(
        R.drawable.dr_icon_info,
        R.string.strTitleAboutUs,
        onClick = { InfoUtils.openAbout(it) }
    ),
    AboutItem(
        R.drawable.dr_icon_bug,
        R.string.strTitleSendFeedback,
        onClick = { InfoUtils.openFeedbackPage(it) }
    ),
    AboutItem(
        R.drawable.dr_icon_help,
        R.string.strTitleHelpSupport,
        onClick = { InfoUtils.openHelp(it) }
    ),
    AboutItem(
        R.drawable.dr_icon_privacy_policy,
        R.string.strTitlePrivacyPolicy,
        onClick = { InfoUtils.openPrivacyPolicy(it) }
    ),
    AboutItem(
        R.drawable.icon_github_2,
        R.string.github,
        onClick = { AppBridge.newOpener(it).browseLink(ApiConfig.GITHUB_REPOSITORY_URL) }
    ),
    AboutItem(
        R.drawable.icon_discord,
        R.string.discord,
        onClick = { InfoUtils.openDiscord(it) }
    ),
)

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = { AppBar(stringResource(R.string.strTitleAboutUs)) }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VersionCard()
            items.forEach {
                ListItem(
                    title = it.title,
                    leading = {
                        Icon(
                            painter = painterResource(it.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp),
                            tint = colorScheme.onSurface
                        )
                    },
                    trailing = {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                ) { it.onClick(context) }
            }
        }
    }
}

@Composable
private fun VersionCard() {
    val context = LocalContext.current

    ListItem(
        title = R.string.strTitleAppVersion,
        subtitleStr = BuildConfig.VERSION_NAME,
        leading = {
            Icon(
                painter = painterResource(R.drawable.dr_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp),
                tint = Color.Unspecified
            )
        },
        trailing = {
            Icon(
                painter = painterResource(R.drawable.icon_copy),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    ) {
        context.copyToClipboard(BuildConfig.VERSION_NAME)
        MessageUtils.showClipboardMessage(
            context,
            "Version name copied",
        )
    }
}