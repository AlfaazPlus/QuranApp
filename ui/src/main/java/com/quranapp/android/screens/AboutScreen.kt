package com.quranapp.android.screens

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
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ListItem
import com.quranapp.android.components.rememberInfoLinks
import com.quranapp.android.core.BuildConfig
import com.quranapp.android.core.R
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.app.UrlsManager
import com.quranapp.android.utils.extensions.copyToClipboard

private data class AboutItem(
    val icon: Int,
    val title: Int,
    val onClick: () -> Unit,
)

@Composable
private fun aboutItems(openLink: (String) -> Unit): List<AboutItem> {
    val context = LocalContext.current
    return listOf(
        AboutItem(R.drawable.dr_icon_info, R.string.strTitleAboutUs) {
            openLink(UrlsManager.URL_KEY_ABOUT)
        },
        AboutItem(R.drawable.dr_icon_bug, R.string.strTitleSendFeedback) {
            openLink(UrlsManager.URL_KEY_FEEDBACK)
        },
        AboutItem(R.drawable.dr_icon_help, R.string.strTitleHelpSupport) {
            openLink(UrlsManager.URL_KEY_HELP)
        },
        AboutItem(R.drawable.dr_icon_privacy_policy, R.string.strTitlePrivacyPolicy) {
            openLink(UrlsManager.URL_KEY_PRIVACY_POLICY)
        },
        AboutItem(R.drawable.icon_github_2, R.string.github) {
            PlatformServices.links.browse(ApiConfig.GITHUB_REPOSITORY_URL)
        },
        AboutItem(R.drawable.icon_discord, R.string.discord) {
            openLink(UrlsManager.URL_KEY_DISCORD)
        },
    )
}

@Composable
fun AboutScreen() {
    val items = aboutItems(rememberInfoLinks())

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
            items.forEach { item ->
                ListItem(
                    title = item.title,
                    leading = {
                        Icon(
                            painter = painterResource(item.icon),
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
                ) { item.onClick() }
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
        PlatformServices.messages.showClipboard("Version name copied")
    }
}
