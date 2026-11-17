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
import androidx.compose.ui.unit.dp
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ListItem
import com.quranapp.android.components.rememberInfoLinks
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.discord
import com.quranapp.android.generated.resources.dr_icon_bug
import com.quranapp.android.generated.resources.dr_icon_chevron_right
import com.quranapp.android.generated.resources.dr_icon_help
import com.quranapp.android.generated.resources.dr_icon_info
import com.quranapp.android.generated.resources.dr_icon_privacy_policy
import com.quranapp.android.generated.resources.dr_logo
import com.quranapp.android.generated.resources.github
import com.quranapp.android.generated.resources.icon_copy
import com.quranapp.android.generated.resources.icon_discord
import com.quranapp.android.generated.resources.icon_github_2
import com.quranapp.android.generated.resources.strTitleAboutUs
import com.quranapp.android.generated.resources.strTitleAppVersion
import com.quranapp.android.generated.resources.strTitleHelpSupport
import com.quranapp.android.generated.resources.strTitlePrivacyPolicy
import com.quranapp.android.generated.resources.strTitleSendFeedback
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.shared.BuildConfig
import com.quranapp.android.utils.app.UrlsManager
import com.quranapp.android.utils.extensions.copyToClipboard
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private data class AboutItem(
    val icon: DrawableResource,
    val title: StringResource,
    val onClick: () -> Unit,
)

@Composable
private fun aboutItems(openLink: (String) -> Unit): List<AboutItem> {
    val context = LocalContext.current
    return listOf(
        AboutItem(Res.drawable.dr_icon_info, Res.string.strTitleAboutUs) {
            openLink(UrlsManager.URL_KEY_ABOUT)
        },
        AboutItem(Res.drawable.dr_icon_bug, Res.string.strTitleSendFeedback) {
            openLink(UrlsManager.URL_KEY_FEEDBACK)
        },
        AboutItem(Res.drawable.dr_icon_help, Res.string.strTitleHelpSupport) {
            openLink(UrlsManager.URL_KEY_HELP)
        },
        AboutItem(Res.drawable.dr_icon_privacy_policy, Res.string.strTitlePrivacyPolicy) {
            openLink(UrlsManager.URL_KEY_PRIVACY_POLICY)
        },
        AboutItem(Res.drawable.icon_github_2, Res.string.github) {
            PlatformServices.links.browse(ApiConfig.GITHUB_REPOSITORY_URL)
        },
        AboutItem(Res.drawable.icon_discord, Res.string.discord) {
            openLink(UrlsManager.URL_KEY_DISCORD)
        },
    )
}

@Composable
fun AboutScreen() {
    val items = aboutItems(rememberInfoLinks())

    Scaffold(
        topBar = { AppBar(stringResource(Res.string.strTitleAboutUs)) }
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
                            painter = painterResource(Res.drawable.dr_icon_chevron_right),
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
        title = Res.string.strTitleAppVersion,
        subtitleStr = BuildConfig.VERSION_NAME,
        leading = {
            Icon(
                painter = painterResource(Res.drawable.dr_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp),
                tint = Color.Unspecified
            )
        },
        trailing = {
            Icon(
                painter = painterResource(Res.drawable.icon_copy),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    ) {
        context.copyToClipboard(BuildConfig.VERSION_NAME)
        PlatformServices.messages.showClipboard("Version name copied")
    }
}
