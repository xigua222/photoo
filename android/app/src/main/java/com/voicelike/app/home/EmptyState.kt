package com.voicelike.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Reveal curve shared with iOS ccReveal: quick out, gentle settle, no overshoot.
private val StatusPanelRevealEasing = CubicBezierEasing(0.22f, 0.8f, 0.36f, 1f)
private const val STATUS_PANEL_REVEAL_MS = 320
private const val STATUS_PANEL_STAGGER_MS = 70L
private const val STATUS_PANEL_REVEAL_ROWS = 4
private const val STATUS_PANEL_CONFETTI_DELAY_MS = 300L

private enum class StatusPanelVariant { Access, RangeDone, BatchDone, AllDone }

private data class StatusPanelAction(
    val text: String,
    val primary: Boolean,
    val icon: ImageVector? = null,
    val flyUpFirst: Boolean = false,
    val onClick: () -> Unit
)

/**
 * The organize result panel (and the photo-access state), styled after iOS SortingStatusPanel:
 * a centred mark, title, message and at most two full-width buttons with exactly one primary.
 * Organize Again lives in Settings, so it is not offered here.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun EmptyState(
    onRestart: () -> Unit,
    hasMediaPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    pendingTrashCount: Int = 0,
    onViewTrash: () -> Unit = {},
    filterFolder: String = "All",
    filterType: String = "All",
    filterMonth: String = "All",
    filterDay: String = "All",
    onResetFilter: () -> Unit = {},
    onChangeFilter: () -> Unit = {},
    onReleaseKept: () -> Unit = {},
    onViewLikes: () -> Unit = {},
    remainingCount: Int = 0,
    isFullAlbumDone: Boolean = false,
    celebrationTriggerToken: Long = 1L,
    embeddedInCard: Boolean = false,
    burstImmediate: Boolean = false,
    quickOrganizeMode: Boolean = false,
    isRangeActive: Boolean =
        filterFolder != "All" || filterType != "All" || filterMonth != "All" || filterDay != "All",
    onBack: () -> Unit = {},
    onBackToAllPhotos: () -> Unit = onResetFilter
) {
    val copy = organizeResultStrings(LocalAppLanguage.current)
    val isDark = isSystemInDarkTheme()
    val isUnauthorized = !hasMediaPermission
    val trulyDone = remainingCount <= 0 || (!isRangeActive && isFullAlbumDone)
    val isFilterDone = isRangeActive && remainingCount <= 0
    val variant = when {
        isUnauthorized -> StatusPanelVariant.Access
        isFilterDone -> StatusPanelVariant.RangeDone
        !trulyDone -> StatusPanelVariant.BatchDone
        else -> StatusPanelVariant.AllDone
    }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val revealOffsetPx = with(density) { 14.dp.toPx() }

    // Fly-up before the next batch (Keep Organizing).
    val leaveOffsetY = remember { Animatable(0f) }
    val leaveAlpha = remember { Animatable(1f) }
    var isLeaving by remember { mutableStateOf(false) }

    LaunchedEffect(isLeaving, remainingCount) {
        if (isLeaving) {
            delay(400)
            isLeaving = false
            leaveAlpha.snapTo(1f)
            leaveOffsetY.snapTo(0f)
        }
    }

    // Entrance: the panel scales in from 0.92 and fades, rows follow with a 70ms stagger,
    // the mark pops from 0.4. Starts hidden so the first frame does not flash the final state.
    val panelScale = remember { Animatable(0.92f) }
    val panelAlpha = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.4f) }
    val rowReveal = remember { List(STATUS_PANEL_REVEAL_ROWS) { Animatable(0f) } }
    var contentOnScreen by remember { mutableStateOf(false) }

    LaunchedEffect(celebrationTriggerToken, isUnauthorized) {
        val markSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 273f)
        // Carousel settlement page seen before it settles: show it still, pop the mark later.
        if (embeddedInCard && celebrationTriggerToken == 0L && !isUnauthorized) {
            panelScale.snapTo(1f)
            panelAlpha.snapTo(1f)
            rowReveal.forEach { it.snapTo(1f) }
            markScale.snapTo(0.4f)
            contentOnScreen = true
            return@LaunchedEffect
        }
        if (contentOnScreen) {
            panelScale.snapTo(1f)
            panelAlpha.snapTo(1f)
            rowReveal.forEach { it.snapTo(1f) }
            if (isUnauthorized) {
                markScale.snapTo(1f)
            } else {
                markScale.snapTo(0.4f)
                markScale.animateTo(1f, markSpring)
            }
            return@LaunchedEffect
        }
        contentOnScreen = true
        panelScale.snapTo(0.92f)
        panelAlpha.snapTo(0f)
        rowReveal.forEach { it.snapTo(0f) }
        markScale.snapTo(if (isUnauthorized) 1f else 0.4f)
        launch { panelAlpha.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
        launch {
            panelScale.animateTo(
                1f,
                spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        rowReveal.forEachIndexed { index, progress ->
            launch {
                delay(STATUS_PANEL_STAGGER_MS * index)
                progress.animateTo(1f, tween(STATUS_PANEL_REVEAL_MS, easing = StatusPanelRevealEasing))
            }
        }
        if (!isUnauthorized) {
            launch { markScale.animateTo(1f, markSpring) }
        }
    }

    // Confetti starts once the panel has settled; composing it earlier would draw the
    // particles bunched at the burst centre.
    var confettiToken by remember { mutableLongStateOf(0L) }
    LaunchedEffect(celebrationTriggerToken, isUnauthorized) {
        confettiToken = 0L
        if (isUnauthorized || celebrationTriggerToken == 0L) return@LaunchedEffect
        delay(STATUS_PANEL_CONFETTI_DELAY_MS)
        confettiToken = celebrationTriggerToken
    }

    fun keepOrganizing() {
        if (isLeaving) return
        isLeaving = true
        scope.launch {
            launch { leaveAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
            leaveOffsetY.animateTo(-screenHeightPx, tween(260, easing = FastOutSlowInEasing))
            onRestart()
        }
    }

    val pendingDeleteText = copy.viewPendingDeleteFmt.format(pendingTrashCount)
    val (title, message, actions) = when (variant) {
        StatusPanelVariant.Access -> Triple(
            copy.accessTitle,
            copy.accessMessage,
            listOf(StatusPanelAction(copy.accessButton, primary = true, onClick = onRequestPermission))
        )
        StatusPanelVariant.RangeDone -> Triple(
            copy.rangeDoneTitle,
            copy.rangeDoneMessage,
            listOf(
                if (pendingTrashCount > 0) {
                    StatusPanelAction(pendingDeleteText, primary = true, icon = Icons.Outlined.Delete, onClick = onViewTrash)
                } else {
                    StatusPanelAction(copy.changeRange, primary = true, icon = Icons.Outlined.FilterList, onClick = onChangeFilter)
                },
                StatusPanelAction(copy.backToAllPhotos, primary = false, onClick = onBackToAllPhotos)
            )
        )
        StatusPanelVariant.BatchDone -> Triple(
            copy.batchDoneTitle,
            copy.batchDoneMessage,
            buildList<StatusPanelAction> {
                add(StatusPanelAction(copy.keepOrganizing, primary = true, flyUpFirst = true, onClick = ::keepOrganizing))
                if (pendingTrashCount > 0) {
                    add(StatusPanelAction(pendingDeleteText, primary = false, icon = Icons.Outlined.Delete, onClick = onViewTrash))
                }
            }
        )
        StatusPanelVariant.AllDone -> Triple(
            copy.allDoneTitle,
            copy.allDoneMessage,
            buildList<StatusPanelAction> {
                if (pendingTrashCount > 0) {
                    add(StatusPanelAction(pendingDeleteText, primary = true, icon = Icons.Outlined.Delete, onClick = onViewTrash))
                }
                add(StatusPanelAction(copy.back, primary = pendingTrashCount <= 0, onClick = onBack))
            }
        )
    }

    @Composable
    fun PanelContent(modifier: Modifier) {
        Column(
            modifier = modifier.graphicsLayer {
                scaleX = panelScale.value
                scaleY = panelScale.value
                alpha = panelAlpha.value
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.statusPanelReveal(rowReveal[0], revealOffsetPx)) {
                if (variant == StatusPanelVariant.Access) {
                    StatusPanelAccessMark(isDark = isDark)
                } else {
                    StatusPanelCelebrationMark(scale = { markScale.value })
                }
            }
            Text(
                text = title,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .statusPanelReveal(rowReveal[1], revealOffsetPx)
            )
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.62f else 0.55f),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .statusPanelReveal(rowReveal[2], revealOffsetPx)
            )
            Column(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth()
                    .statusPanelReveal(rowReveal[3], revealOffsetPx),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                actions.forEach { action ->
                    StatusPanelButton(
                        action = action,
                        isDark = isDark,
                        enabled = !(action.flyUpFirst && isLeaving)
                    )
                }
            }
        }
    }

    val confettiIntensity = if (trulyDone) 500 else 350

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val leaveModifier = Modifier.graphicsLayer {
            translationY = leaveOffsetY.value
            alpha = leaveAlpha.value
        }

        if (embeddedInCard) {
            Box(modifier = leaveModifier) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.34f else 0.14f),
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.48f else 0.18f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PanelContent(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .widthIn(max = 360.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                if (confettiToken != 0L) {
                    key(confettiToken) {
                        CelebrationConfetti(
                            intensity = confettiIntensity,
                            triggerToken = confettiToken,
                            initialBurstProgress = 0.08f,
                            burstCenterYFraction = 0.4f
                        )
                    }
                }
            }
        } else {
            // No card: the panel sits on the page background, centred where the deck would be.
            val homeCardFrameSpec = rememberHomeStackCardFrameSpec(quickOrganizeMode)
            val deckFrameSize = resolveHomeCardFrameSize(
                availableWidth = maxWidth,
                availableHeight = maxHeight,
                spec = homeCardFrameSpec
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = BiasAlignment(0f, homeCardFrameSpec.biasY)
            ) {
                Box(
                    modifier = leaveModifier
                        .fillMaxWidth()
                        .height(deckFrameSize.height),
                    contentAlignment = Alignment.Center
                ) {
                    PanelContent(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                    )
                }
            }
            if (confettiToken != 0L) {
                key(confettiToken) {
                    CelebrationConfetti(
                        intensity = confettiIntensity,
                        triggerToken = confettiToken,
                        initialBurstProgress = 0.08f,
                        burstCenterYFraction = 0.35f
                    )
                }
            }
        }
    }
}

private fun Modifier.statusPanelReveal(
    progress: Animatable<Float, AnimationVector1D>,
    offsetPx: Float
): Modifier = graphicsLayer {
    val value = progress.value
    alpha = value
    translationY = (1f - value) * offsetPx
}

/** Done mark: accent-filled circle with a check, popped in once. */
@Composable
private fun StatusPanelCelebrationMark(scale: () -> Float) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                val value = scale()
                scaleX = value
                scaleY = value
            }
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(34.dp)
        )
    }
}

/** Photo-access mark: neutral circle with a photo icon. */
@Composable
private fun StatusPanelAccessMark(isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun StatusPanelButton(
    action: StatusPanelAction,
    isDark: Boolean,
    enabled: Boolean
) {
    val containerColor = if (action.primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.12f else 0.06f)
    }
    val contentColor = if (action.primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    Button(
        onClick = action.onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            // Keep the look while the panel flies away after Keep Organizing.
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (action.icon != null) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = action.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
