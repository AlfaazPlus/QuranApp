package com.quranapp.android.compose.components.homepage

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.utils.app.UpdateManager
import kotlin.math.pow

@Composable
fun AppUpdateBanner() {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager.getInstance(context) }
    val bannerDecision by updateManager.bannerDecision.collectAsState()

    if (!bannerDecision.showInlineBanner) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface,
        ),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedUpdateAppIcon(
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.strMsgUpdateAvailable),
                modifier = Modifier.weight(1f),
                style = typography.bodyMedium,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            FilledTonalButton(onClick = { updateManager.openPlayStore() }) {
                Text(text = stringResource(R.string.strLabelUpdate))
            }
        }
    }
}

@Composable
private fun AnimatedUpdateAppIcon(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "appUpdateIcon")
    val cycle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 0
                1f at 1000
                1f at 2500
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "updateIconCycle",
    )

    val eased = 1f - (1f - cycle.value).toDouble().pow(2.0).toFloat()
    val transY = lerpKeyframes(floatArrayOf(0f, 8f, -10f, 10f, -3f, 0f), eased)
    var scaleX = lerpKeyframes(floatArrayOf(1f, 1.1f, .8f, 1.3f, 1.03f, 1f), eased)
    var scaleY = lerpKeyframes(floatArrayOf(1f, .8f, 1.1f, .9f, 1f, 1f), eased)

    Icon(
        painter = painterResource(R.drawable.dr_icon_update_app),
        contentDescription = null,
        tint = null,
        modifier = modifier.graphicsLayer {
            translationY = with(density) { transY.dp.toPx() }
            scaleX = scaleX
            scaleY = scaleY
        },
    )
}

private fun lerpKeyframes(values: FloatArray, t: Float): Float {
    if (values.isEmpty()) return 0f
    if (values.size == 1) return values[0]
    val n = values.size - 1
    val pos = t.coerceIn(0f, 1f) * n
    val i = pos.toInt().coerceIn(0, n - 1)
    val frac = pos - i
    return values[i] * (1f - frac) + values[i + 1] * frac
}
