import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.SwitchItem
import com.quranapp.android.compose.components.settings.ResourceDownloadSrcSheet
import com.quranapp.android.compose.components.settings.SettingsItem
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.compose.screens.settings.LocalSettingsNavController
import com.quranapp.android.compose.utils.preferences.AppPreferences
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.app.DownloadSourceUtils
import kotlinx.coroutines.launch

@Composable
fun AdvancedSettingsScreen() {
    val navController = LocalSettingsNavController.current
    val coroutineScope = rememberCoroutineScope()

    var showResourceDownloadSrcSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { AppBar(title = stringResource(R.string.advanced_settings)) }
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 150.dp),
        ) {
            SettingsItem(
                title = R.string.titleResourceDownloadSource,
                icon = R.drawable.dr_icon_download,
                subtitleStr = DownloadSourceUtils.observeCurrentSourceName(),
            ) {
                showResourceDownloadSrcSheet = true
            }

            SettingsItem(
                title = R.string.appLogs,
                icon = R.drawable.dr_icon_bug,
                subtitle = R.string.crashLogs,
            ) {
                navController.navigate(SettingRoutes.APP_LOGS)
            }

            SwitchItem(
                title = "Developer mode",
                subtitle = "Enables additional developer capabilities, e.g., custom translation import.",
                checked = AppPreferences.observeDeveloperModeEnabled(),
            ) {
                coroutineScope.launch {
                    AppPreferences.setDeveloperModeEnabled(it)
                }
            }
        }
    }

    ResourceDownloadSrcSheet(isOpen = showResourceDownloadSrcSheet) {
        showResourceDownloadSrcSheet = false
    }
}
