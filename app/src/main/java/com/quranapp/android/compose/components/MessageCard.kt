package com.quranapp.android.compose.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.utils.DataLoadError

/**
 * Defines the visual style of a MessageCard
 */
enum class MessageCardStyle {
    /** For error states - uses error colors */
    Error,
    /** For informational messages - uses primary colors */
    Info,
    /** For warning messages - uses warning/tertiary colors */
    Warning,
    /** For success messages - uses success/primary colors */
    Success,
    /** Neutral style - uses surface variant colors */
    Neutral
}

/**
 * Data class representing button configuration for MessageCard
 */
data class MessageCardAction(
    @StringRes val textRes: Int,
    val onClick: () -> Unit,
    val isPrimary: Boolean = true
)

/**
 * A reusable message card component for displaying errors, empty states,
 * informational messages, and other status indicators.
 *
 * @param icon Resource ID for the icon to display
 * @param message The message text to display
 * @param modifier Modifier for the composable
 * @param title Optional title text
 * @param style The visual style of the card
 * @param primaryAction Optional primary action button
 * @param secondaryAction Optional secondary action button
 * @param fillMaxSize Whether the card should fill the maximum available size
 * @param showCard Whether to show as a card (with background) or just content
 */
@Composable
fun MessageCard(
    @DrawableRes icon: Int,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    style: MessageCardStyle = MessageCardStyle.Neutral,
    primaryAction: MessageCardAction? = null,
    secondaryAction: MessageCardAction? = null,
    fillMaxSize: Boolean = true,
    showCard: Boolean = false
) {
    val colors = getMessageCardColors(style)

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .then(if (fillMaxSize) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with background circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title (optional)
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Action buttons
            if (primaryAction != null || secondaryAction != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    primaryAction?.let { action ->
                        Button(
                            onClick = action.onClick,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.buttonColor
                            ),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(action.textRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    secondaryAction?.let { action ->
                        OutlinedButton(
                            onClick = action.onClick,
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(action.textRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCard) {
        Card(
            modifier = modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

/**
 * Convenience overload using string resource IDs
 */
@Composable
fun MessageCard(
    @DrawableRes icon: Int,
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int? = null,
    style: MessageCardStyle = MessageCardStyle.Neutral,
    primaryAction: MessageCardAction? = null,
    secondaryAction: MessageCardAction? = null,
    fillMaxSize: Boolean = true,
    showCard: Boolean = false
) {
    MessageCard(
        icon = icon,
        message = stringResource(messageRes),
        modifier = modifier,
        title = titleRes?.let { stringResource(it) },
        style = style,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        fillMaxSize = fillMaxSize,
        showCard = showCard
    )
}

/**
 * Creates a MessageCard from a DataLoadError
 */
@Composable
fun ErrorMessageCard(
    error: DataLoadError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, message) = when (error) {
        DataLoadError.NoConnection -> R.drawable.dr_icon_no_internet to R.string.strMsgNoInternet
        DataLoadError.NoData -> R.drawable.dr_icon_info to R.string.strMsgTafsirsNoAvailable
        DataLoadError.Failed -> R.drawable.dr_icon_info to R.string.strMsgSomethingWrong
    }

    val style = when (error) {
        DataLoadError.NoConnection -> MessageCardStyle.Warning
        DataLoadError.NoData -> MessageCardStyle.Info
        DataLoadError.Failed -> MessageCardStyle.Error
    }

    MessageCard(
        icon = icon,
        messageRes = message,
        modifier = modifier,
        style = style,
        primaryAction = MessageCardAction(
            textRes = R.string.strLabelRetry,
            onClick = onRetry
        )
    )
}

private data class MessageCardColors(
    val iconBackground: Color,
    val iconTint: Color,
    val buttonColor: Color
)

@Composable
private fun getMessageCardColors(style: MessageCardStyle): MessageCardColors {
    return when (style) {
        MessageCardStyle.Error -> MessageCardColors(
            iconBackground = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.onError,
            buttonColor = MaterialTheme.colorScheme.error
        )
        MessageCardStyle.Info -> MessageCardColors(
            iconBackground = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimary,
            buttonColor = MaterialTheme.colorScheme.primary
        )
        MessageCardStyle.Warning -> MessageCardColors(
            iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            buttonColor = MaterialTheme.colorScheme.tertiary
        )
        MessageCardStyle.Success -> MessageCardColors(
            iconBackground = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            buttonColor = MaterialTheme.colorScheme.primary
        )
        MessageCardStyle.Neutral -> MessageCardColors(
            iconBackground = MaterialTheme.colorScheme.surfaceVariant,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            buttonColor = MaterialTheme.colorScheme.primary
        )
    }
}
