package com.quranapp.android.screens.onboarding

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranapp.android.core.R
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.locale.formatString
import com.quranapp.android.theme.alpha
import com.quranapp.android.theme.type
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val onboardingIcons = listOf(
    R.drawable.dr_icon_language,
    R.drawable.dr_icon_theme,
    R.drawable.dr_icon_translations,
    R.drawable.ic_book_open_text,
)

private val onboardingItems = listOf(
    R.string.strTitleAppLanguage to R.string.onboardDescLanguage,
    R.string.strTitleTheme to R.string.onboardDescTheme,
    R.string.strLabelSelectTranslations to R.string.onboardDescTranslations,
    R.string.strTitleSelectTafsir to R.string.onboardDescTafsir,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val pageCount = onboardingItems.size

    var savedPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = savedPage,
        pageCount = { pageCount },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { savedPage = it }
    }

    BackHandler {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            activity?.finish()
        }
    }

    val onNext: () -> Unit = {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }
    val onBack: () -> Unit = {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Two-pane only in landscape (width > height).
            // Tablets in portrait use the same single-column layout as phones.
            val useTwoPane = maxWidth > maxHeight
            if (useTwoPane) {
                OnboardingTwoPaneLayout(pagerState, onComplete, onNext, onBack)
            } else {
                OnboardingPortraitLayout(pagerState, maxHeight < 700.dp, onComplete, onNext, onBack)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingPortraitLayout(
    pagerState: PagerState,
    isCompact: Boolean,
    onComplete: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val page = pagerState.currentPage
    val isLast = page == onboardingItems.lastIndex

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(colorScheme.surfaceContainer)
                .statusBarsPadding()
                .padding(
                    start = 24.dp, end = 24.dp,
                    top = 12.dp,
                    bottom = if (isCompact) 20.dp else 28.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepHeaderRow(
                page = page,
                total = onboardingItems.size,
                onSkip = onComplete,
            )

            Spacer(Modifier.height(if (isCompact) 16.dp else 24.dp))

            OnboardingHero(
                page = page,
                iconBoxSize = if (isCompact) 60.dp else 80.dp,
                iconSize = if (isCompact) 30.dp else 40.dp,
                iconCorner = if (isCompact) 18.dp else 24.dp,
                titleStyle = if (isCompact) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleLarge,
                descStyle = if (isCompact) MaterialTheme.typography.bodySmall
                else MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                spacerAfterIcon = if (isCompact) 12.dp else 20.dp,
                spacerAfterTitle = if (isCompact) 4.dp else 8.dp,
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = false,
            verticalAlignment = Alignment.Top,
        ) { OnboardingPage(it) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
        ) {
            OnboardingNavBar(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                pageCount = onboardingItems.size,
                currentPage = page,
                isLastPage = isLast,
                onBack = onBack,
                onNext = onNext,
                onComplete = onComplete,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingTwoPaneLayout(
    pagerState: PagerState,
    onComplete: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val page = pagerState.currentPage
    val isLast = page == onboardingItems.lastIndex

    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .background(colorScheme.surfaceContainer)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            StepHeaderRow(
                page = page,
                total = onboardingItems.size,
                onSkip = onComplete,
            )

            OnboardingHero(
                page = page,
                iconBoxSize = 68.dp,
                iconSize = 34.dp,
                iconCorner = 20.dp,
                titleStyle = MaterialTheme.typography.headlineSmall,
                descStyle = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                spacerAfterIcon = 20.dp,
                spacerAfterTitle = 8.dp,
            )

            OnboardingNavBar(
                modifier = Modifier.fillMaxWidth(),
                pageCount = onboardingItems.size,
                currentPage = page,
                isLastPage = isLast,
                onBack = onBack,
                onNext = onNext,
                onComplete = onComplete,
            )
        }

        Box(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 640.dp),
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceContainerLow,
                shadowElevation = 2.dp,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                    verticalAlignment = Alignment.Top,
                ) { OnboardingPage(it) }
            }
        }
    }
}

@Composable
private fun StepHeaderRow(
    page: Int,
    total: Int,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val appLocale = LocalAppLocale.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colorScheme.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                formatString(
                    context,
                    appLocale,
                    "%d / %d",
                    page + 1,
                    total,
                ),
                color = colorScheme.onPrimaryContainer,
                style = type.labelSmall,
            )
        }

        TextButton(onClick = onSkip, shape = RoundedCornerShape(50)) {
            Text(
                text = stringResource(R.string.strLabelSkip),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OnboardingHero(
    page: Int,
    iconBoxSize: Dp,
    iconSize: Dp,
    iconCorner: Dp,
    titleStyle: androidx.compose.ui.text.TextStyle,
    descStyle: androidx.compose.ui.text.TextStyle,
    textAlign: TextAlign,
    spacerAfterIcon: Dp,
    spacerAfterTitle: Dp,
) {
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 })
                .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 6 })
        },
        label = "onboardingHero",
    ) { p ->
        val item = onboardingItems[p]
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (textAlign == TextAlign.Center) Alignment.CenterHorizontally
            else Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .clip(RoundedCornerShape(iconCorner))
                    .background(colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(onboardingIcons[p]),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = colorScheme.primary,
                )
            }

            Spacer(Modifier.height(spacerAfterIcon))

            Text(
                text = stringResource(item.first),
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = textAlign,
            )
            Spacer(Modifier.height(spacerAfterTitle))
            Text(
                text = stringResource(item.second),
                style = descStyle,
                color = colorScheme.onSurfaceVariant,
                textAlign = textAlign,
            )
        }
    }
}

@Composable
private fun OnboardingPage(pageIndex: Int) {
    when (pageIndex) {
        0 -> OnboardingLanguagePage()
        1 -> OnboardingThemePage()
        2 -> OnboardingTranslationsPage()
        3 -> OnboardingTafsirPage()
    }
}

@Composable
private fun OnboardingNavBar(
    modifier: Modifier = Modifier,
    pageCount: Int,
    currentPage: Int,
    isLastPage: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Fixed-size slot keeps the row width stable; button is hidden on page 0.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (currentPage > 0) colorScheme.surfaceContainerHigh
                    else colorScheme.surfaceContainerHigh.copy(alpha = 0f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (currentPage > 0) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colorScheme.surfaceContainerHigh,
                        contentColor = colorScheme.onSurface,
                    ),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_arrow_left),
                        contentDescription = stringResource(R.string.strLabelBack),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { i ->
                val selected = i == currentPage
                val dotWidth by animateDpAsState(
                    targetValue = if (selected) 24.dp else 6.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dot$i",
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(
                            if (selected) colorScheme.primary
                            else colorScheme.outlineVariant.alpha(0.5f)
                        ),
                )
            }
        }

        Button(
            onClick = if (isLastPage) onComplete else onNext,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 4.dp,
            ),
        ) {
            Text(
                text = if (isLastPage) stringResource(R.string.strLabelStart)
                else stringResource(R.string.strLabelNext),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
