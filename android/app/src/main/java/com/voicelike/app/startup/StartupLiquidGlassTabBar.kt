package com.voicelike.app.startup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.voicelike.app.AppLanguage
import com.voicelike.app.liquidglass.LocalReferenceLiquidBottomTabAccentLayer
import com.voicelike.app.liquidglass.ReferenceLiquidBottomTab
import com.voicelike.app.liquidglass.ReferenceLiquidBottomTabs
import com.voicelike.app.liquidglass.liquidGlassSurfaceColor

private val liquidGlassTabs = listOf(
    StartupWelcomeTab.Organize,
    StartupWelcomeTab.ColorCard,
    StartupWelcomeTab.Insights,
    StartupWelcomeTab.Mine
)

private fun liquidGlassTabIndex(tab: StartupWelcomeTab): Int = when (tab) {
    StartupWelcomeTab.Watermark -> liquidGlassTabs.indexOf(StartupWelcomeTab.ColorCard)
    else -> liquidGlassTabs.indexOf(tab).coerceAtLeast(0)
}

/**
 * Bottom tab bar built on the Kyant backdrop catalog's LiquidBottomTabs: the bar refracts
 * [backdrop] (the page content behind it) and the selection pill can be dragged between tabs.
 */
@Composable
internal fun StartupLiquidGlassTabBar(
    backdrop: Backdrop,
    selectedTab: StartupWelcomeTab,
    onTabSelected: (StartupWelcomeTab) -> Unit,
    onTabReselected: (StartupWelcomeTab) -> Unit,
    hidden: Boolean,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val iconColor = MaterialTheme.colorScheme.onSurface
    val selectedIndex = liquidGlassTabIndex(selectedTab)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val hiddenOffset by animateDpAsState(
        targetValue = if (hidden) 118.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_tab_bar_hidden_offset"
    )
    val barAlpha by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "liquid_tab_bar_alpha"
    )
    val containerColor = remember(isLightTheme) { liquidGlassSurfaceColor(isLightTheme) }

    ReferenceLiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        // Fires for drags and for external selection changes; only a drag lands on a new index.
        onTabSelected = { index ->
            if (index != currentSelectedIndex) onTabSelected(liquidGlassTabs[index])
        },
        backdrop = backdrop,
        tabsCount = liquidGlassTabs.size,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationY = hiddenOffset.toPx()
                alpha = barAlpha
            },
        containerShape = RoundedCornerShape(percent = 50),
        indicatorShape = RoundedCornerShape(percent = 50),
        containerColor = containerColor,
        accentColor = iconColor,
        isLightTheme = isLightTheme
    ) {
        val accentLayer = LocalReferenceLiquidBottomTabAccentLayer.current
        liquidGlassTabs.forEachIndexed { index, tab ->
            val tint by animateColorAsState(
                targetValue = iconColor.copy(alpha = if (index == selectedIndex || accentLayer) 1f else 0.44f),
                animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
                label = "liquid_tab_bar_icon"
            )
            ReferenceLiquidBottomTab(
                onClick = {
                    if (index == currentSelectedIndex) onTabReselected(selectedTab) else onTabSelected(tab)
                }
            ) {
                Icon(
                    imageVector = startupTabIcon(tab),
                    contentDescription = localizedStartupTabContentDescription(appLanguage, tab),
                    tint = tint,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}
