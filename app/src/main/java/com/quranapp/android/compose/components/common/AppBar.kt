package com.quranapp.android.compose.components.common

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.theme.alpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    bgColor: Color? = null,
    color: Color? = null,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String? = null,
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
        modifier = Modifier.shadow(4.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bgColor ?: Color.Unspecified,
            navigationIconContentColor = color ?: Color.Unspecified,
            titleContentColor = color ?: Color.Unspecified,
            actionIconContentColor = color ?: Color.Unspecified,
        ),
        title = {
            val contentColor = LocalContentColor.current
            if (searchEnabled && searchExpanded) {
                AppBarSearchField(
                    query = searchQuery,
                    onQueryChange = onSearch,
                    placeholder = searchPlaceholder ?: stringResource(R.string.strHintSearch),
                    contentColor = contentColor,
                    modifier = Modifier.focusRequester(searchFocusRequester),
                )
            } else {
                Text(text = title)
            }
        },
        navigationIcon = {
            val backLabel = stringResource(R.string.strLabelBack)
            SimpleTooltip(text = backLabel) {
                IconButton(
                    onClick = {
                        if (searchEnabled && searchExpanded) {
                            searchExpanded = false
                        } else {
                            backPressedDispatcher?.onBackPressed()
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_arrow_left),
                        contentDescription = backLabel,
                    )
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
private fun AppBarSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        placeholder = {
            Text(
                placeholder,
                style = typography.bodyMedium,
                color = contentColor.copy(alpha = 0.5f),
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                SimpleTooltip(text = stringResource(R.string.strLabelClose)) {
                    IconButton(
                        onClick = { onQueryChange("") },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_close),
                            contentDescription = stringResource(R.string.strLabelClose),
                            modifier = Modifier.size(20.dp),
                            tint = contentColor.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        } else null,
        singleLine = true,
        textStyle = typography.bodyMedium.copy(color = contentColor),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = contentColor,
            unfocusedTextColor = contentColor,
            cursorColor = colorScheme.primary,
            focusedBorderColor = colorScheme.primary.alpha(0.5f),
            unfocusedBorderColor = colorScheme.outlineVariant.alpha(0.5f),
            focusedContainerColor = colorScheme.surfaceContainerLow,
            unfocusedContainerColor = colorScheme.surfaceContainerLow,
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
    )
}
