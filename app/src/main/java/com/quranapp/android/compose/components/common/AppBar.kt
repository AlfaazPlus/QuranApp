package com.quranapp.android.compose.components.common

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.SimpleTooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    bgColor: Color = colorScheme.surfaceContainer,
    color: Color = colorScheme.onSurface,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String? = null,
    shadowElevation: Dp = 4.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var searchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val onSearch = onSearchQueryChange

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    val searchEnabled = onSearch != null
    if (searchEnabled) {
        BackHandler(enabled = searchExpanded) {
            searchExpanded = false
        }
    }

    TopAppBar(
        modifier = Modifier.shadow(shadowElevation),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bgColor,
            scrolledContainerColor = bgColor,
            navigationIconContentColor = color,
            titleContentColor = color,
            actionIconContentColor = color,
        ),
        title = {
            if (searchEnabled && searchExpanded) {
                SearchTextField(
                    value = searchQuery,
                    onValueChange = onSearch,
                    placeholder = searchPlaceholder ?: stringResource(R.string.strHintSearch),
                    modifier = Modifier.focusRequester(searchFocusRequester),
                )
            } else if (titleContent != null) {
                titleContent()
            } else if (title != null) {
                Text(text = title)
            }
        },
        navigationIcon = {
            BackButton() {
                if (searchEnabled && searchExpanded) {
                    searchExpanded = false
                } else {
                    backPressedDispatcher?.onBackPressed()
                }
            }
        },
        actions = {
            if (!searchExpanded) {
                if (searchEnabled) {
                    val searchLabel = stringResource(R.string.strLabelNavSearch)
                    SimpleTooltip(text = searchLabel) {
                        IconButton(
                            onClick = { searchExpanded = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_search),
                                contentDescription = searchLabel,
                            )
                        }
                    }
                }
                actions()
            }
        },
    )
}

@Composable
fun AppBarSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        interactionSource = interactionSource,
        singleLine = true,
        textStyle = typography.bodyMedium.copy(
            color = contentColor,
        ),
        cursorBrush = SolidColor(colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (isFocused) {
                            colorScheme.primary.copy(alpha = 0.5f)
                        } else {
                            colorScheme.outlineVariant.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = typography.bodyMedium.copy(
                                color = contentColor.copy(alpha = 0.5f),
                                lineHeight = 20.sp
                            )
                        )
                    }

                    innerTextField()
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_close),
                            contentDescription = stringResource(R.string.strLabelClose),
                            tint = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun BackButton(
    onClick: (() -> Unit)? = null
) {
    val backLabel = stringResource(R.string.strDescGoBack)
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    fun handleClick() {
        if (onClick != null) onClick()
        else backPressedDispatcher?.onBackPressed()
    }


    SimpleTooltip(text = backLabel) {
        IconButton(
            onClick = ::handleClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_arrow_left),
                contentDescription = backLabel,
            )
        }
    }
}
