package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.widget.Toast
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import java.util.Locale

enum class SwipeAction {
    None,
    Keep,
    Like,
    Delete
}

fun parseSwipeAction(value: String): SwipeAction {
    return when (value.lowercase(Locale.getDefault())) {
        "like" -> SwipeAction.Like
        "delete" -> SwipeAction.Delete
        "none" -> SwipeAction.None
        else -> SwipeAction.Keep
    }
}

fun swipeActionKey(action: SwipeAction): String {
    return when (action) {
        SwipeAction.None -> "none"
        SwipeAction.Keep -> "keep"
        SwipeAction.Like -> "like"
        SwipeAction.Delete -> "delete"
    }
}

fun swipeActionLabel(action: SwipeAction, strings: LocalizedStrings): String {
    return when (action) {
        SwipeAction.None -> strings.swipeNone
        SwipeAction.Delete -> strings.delete
        SwipeAction.Like -> strings.likes
        SwipeAction.Keep -> strings.skip
    }
}

fun swipeActionColor(action: SwipeAction): Color {
    return when (action) {
        SwipeAction.None -> Color(0xFF9CA3AF)
        SwipeAction.Delete -> Color(0xFFEF4444)
        SwipeAction.Like -> Color(0xFFFF9800)
        SwipeAction.Keep -> Color(0xFF2196F3)
    }
}

fun swipeActionIcon(action: SwipeAction): ImageVector {
    return when (action) {
        SwipeAction.None -> Icons.Outlined.Close
        SwipeAction.Delete -> Icons.Outlined.Delete
        SwipeAction.Like -> Icons.Filled.Favorite
        SwipeAction.Keep -> Icons.Outlined.ArrowUpward
    }
}

fun nextSelectableSwipeAction(
    current: SwipeAction,
    taken: Set<SwipeAction>,
    actions: List<SwipeAction>
): SwipeAction {
    val available = actions.filterNot { taken.contains(it) }
    if (available.isEmpty()) return SwipeAction.None
    if (current == SwipeAction.None || !available.contains(current)) return available.first()
    val index = available.indexOf(current).coerceAtLeast(0)
    return available[(index + 1) % available.size]
}

fun resolveSwipeActionSelection(
    direction: SwipeDirection,
    action: SwipeAction,
    left: SwipeAction,
    right: SwipeAction,
    up: SwipeAction
): Triple<SwipeAction, SwipeAction, SwipeAction> {
    val resolved = mutableMapOf<SwipeDirection, SwipeAction>()
    resolved[SwipeDirection.Left] = if (direction == SwipeDirection.Left) {
        action
    } else if (left == action && action != SwipeAction.None) {
        SwipeAction.None
    } else {
        left
    }
    resolved[SwipeDirection.Right] = if (direction == SwipeDirection.Right) {
        action
    } else if (right == action && action != SwipeAction.None) {
        SwipeAction.None
    } else {
        right
    }
    resolved[SwipeDirection.Up] = if (direction == SwipeDirection.Up) {
        action
    } else if (up == action && action != SwipeAction.None) {
        SwipeAction.None
    } else {
        up
    }
    return Triple(
        resolved[SwipeDirection.Left] ?: left,
        resolved[SwipeDirection.Right] ?: right,
        resolved[SwipeDirection.Up] ?: up
    )
}

fun resolveUndoAction(item: MediaItem, action: SwipeAction, direction: SwipeDirection): UndoAction {
    return when (action) {
        SwipeAction.Delete -> UndoAction.Trash(item, direction = direction)
        SwipeAction.Like -> UndoAction.Like(item, direction = direction)
        SwipeAction.Keep -> UndoAction.Keep(item, direction = direction)
        SwipeAction.None -> UndoAction.Keep(item, direction = direction)
    }
}

@Composable
fun WelcomeSwipeStep(
    title: String,
    subtitle: String,
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    upAction: SwipeAction,
    onLeftActionChange: (SwipeAction) -> Unit,
    onRightActionChange: (SwipeAction) -> Unit,
    onUpActionChange: (SwipeAction) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    
    // Local state for actions to update instantly, but we rely on parent to update the source of truth
    // However, since parent hoists state, we just pass changes up.
    // Dialog state
    var dialogDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    val dialogActions = listOf(SwipeAction.Keep, SwipeAction.Like, SwipeAction.Delete, SwipeAction.None)
    
    // Helper to resolve conflicts when an action is selected
    val applySelection: (SwipeDirection, SwipeAction) -> Unit = { direction, action ->
        val (newLeft, newRight, newUp) = resolveSwipeActionSelection(
            direction = direction,
            action = action,
            left = leftAction,
            right = rightAction,
            up = upAction
        )
        if (newLeft != leftAction) onLeftActionChange(newLeft)
        if (newRight != rightAction) onRightActionChange(newRight)
        if (newUp != upAction) onUpActionChange(newUp)
    }

    // Generate a unique ID for this view session to control animation reset
    val pageId = remember { java.util.UUID.randomUUID().toString() }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxSize()
            .advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Start).then(staggeredRevealModifier(0, key = pageId + "title"))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start).then(staggeredRevealModifier(1, key = pageId + "subtitle"))
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(modifier = Modifier.then(staggeredRevealModifier(2, key = pageId + "stack"))) {
                    SwipePreferenceStack(
                        leftAction = leftAction,
                        rightAction = rightAction,
                        upAction = upAction,
                        onLeftClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dialogDirection = SwipeDirection.Left
                        },
                        onRightClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dialogDirection = SwipeDirection.Right
                        },
                        onUpClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dialogDirection = SwipeDirection.Up
                        }
                    )
                }
            }
            
            // Restore Defaults Button at the bottom inside the card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val restoreText = getNewPageStrings(LocalAppLanguage.current).restoreDefaultsText

                TextButton(
                    onClick = {
                        onLeftActionChange(SwipeAction.Delete)
                        onRightActionChange(SwipeAction.Like)
                        onUpActionChange(SwipeAction.Keep)
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                ) {
                    Text(
                        text = restoreText,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    dialogDirection?.let { direction ->
        val currentAction = when (direction) {
            SwipeDirection.Left -> leftAction
            SwipeDirection.Right -> rightAction
            SwipeDirection.Up -> upAction
            SwipeDirection.Down -> SwipeAction.None
        }
        SwipePreferenceActionDialog(
            direction = direction,
            currentAction = currentAction,
            actions = dialogActions,
            onSelect = { selected ->
                applySelection(direction, selected)
                dialogDirection = null
            },
            onDismiss = { dialogDirection = null }
        )
    }
}

@Composable
fun SwipePreferencesView(
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    upAction: SwipeAction,
    horizontalSpeedMultiplier: Float,
    upSpeedMultiplier: Float,
    onLeftActionChange: (SwipeAction) -> Unit,
    onRightActionChange: (SwipeAction) -> Unit,
    onUpActionChange: (SwipeAction) -> Unit,
    onHorizontalSpeedMultiplierChange: (Float) -> Unit,
    onUpSpeedMultiplierChange: (Float) -> Unit,
    onRestoreSwipeSpeedDefaults: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit = onBack
) {
    val strings = LocalAppStrings.current
    val preferenceCopy = onboardingPreferenceCopy(LocalAppLanguage.current)
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    var tempLeftAction by remember(leftAction) { mutableStateOf(leftAction) }
    var tempRightAction by remember(rightAction) { mutableStateOf(rightAction) }
    var tempUpAction by remember(upAction) { mutableStateOf(upAction) }
    var dialogDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var showSwipeSpeedPreferences by remember { mutableStateOf(false) }
    val dialogActions = listOf(SwipeAction.Keep, SwipeAction.Like, SwipeAction.Delete, SwipeAction.None)
    val applySelection: (SwipeDirection, SwipeAction) -> Unit = { direction, action ->
        val (newLeft, newRight, newUp) = resolveSwipeActionSelection(
            direction = direction,
            action = action,
            left = tempLeftAction,
            right = tempRightAction,
            up = tempUpAction
        )
        if (newLeft != tempLeftAction) tempLeftAction = newLeft
        if (newRight != tempRightAction) tempRightAction = newRight
        if (newUp != tempUpAction) tempUpAction = newUp
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showSwipeSpeedPreferences) {
            SwipeSpeedPreferencesView(
                horizontalSpeedMultiplier = horizontalSpeedMultiplier,
                upSpeedMultiplier = upSpeedMultiplier,
                onHorizontalSpeedMultiplierChange = onHorizontalSpeedMultiplierChange,
                onUpSpeedMultiplierChange = onUpSpeedMultiplierChange,
                onRestoreDefaults = onRestoreSwipeSpeedDefaults,
                onBack = { showSwipeSpeedPreferences = false }
            )
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, bottom = 24.dp)
            ) {
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = strings.back,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = {
                        if (tempLeftAction == SwipeAction.None || tempRightAction == SwipeAction.None || tempUpAction == SwipeAction.None) {
                            Toast.makeText(context, strings.swipeAssignAllToast, Toast.LENGTH_SHORT).show()
                        } else {
                            if (tempLeftAction != leftAction) onLeftActionChange(tempLeftAction)
                            if (tempRightAction != rightAction) onRightActionChange(tempRightAction)
                            if (tempUpAction != upAction) onUpActionChange(tempUpAction)
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                ) {
                    Text(text = strings.swipeSave, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = preferenceCopy.organizeSwipeTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .bounceClick(onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSwipeSpeedPreferences = true
                        })
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.swipeSpeed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${String.format("%.1f", horizontalSpeedMultiplier)}x / ${String.format("%.1f", upSpeedMultiplier)}x",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = if (layoutDirection == LayoutDirection.Rtl) {
                            Modifier.scale(scaleX = -1f, scaleY = 1f)
                        } else {
                            Modifier
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                SwipePreferenceStack(
                    leftAction = tempLeftAction,
                    rightAction = tempRightAction,
                    upAction = tempUpAction,
                    onLeftClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dialogDirection = SwipeDirection.Left
                    },
                    onRightClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dialogDirection = SwipeDirection.Right
                    },
                    onUpClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dialogDirection = SwipeDirection.Up
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                val restoreText = getNewPageStrings(LocalAppLanguage.current).restoreDefaultsText

                TextButton(
                    onClick = {
                        tempLeftAction = SwipeAction.Delete
                        tempRightAction = SwipeAction.Like
                        tempUpAction = SwipeAction.Keep
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                ) {
                    Text(
                        text = restoreText,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        dialogDirection?.let { direction ->
            val currentAction = when (direction) {
                SwipeDirection.Left -> tempLeftAction
                SwipeDirection.Right -> tempRightAction
                SwipeDirection.Up -> tempUpAction
                SwipeDirection.Down -> SwipeAction.None
            }
            SwipePreferenceActionDialog(
                direction = direction,
                currentAction = currentAction,
                actions = dialogActions,
                onSelect = { selected ->
                    applySelection(direction, selected)
                    dialogDirection = null
                },
                onDismiss = { dialogDirection = null },
                modifier = Modifier.zIndex(2f)
            )
        }
    }
}

@Composable
private fun SwipePreferenceStack(
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    upAction: SwipeAction,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onUpClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 12f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        val cardWidth = maxWidth * 0.72f
        val cardHeight = cardWidth * (14f / 9f)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SwipePreferenceCardStack(cardWidth, cardHeight)
            SwipePreferenceActionBlock(
                action = leftAction,
                direction = SwipeDirection.Left,
                onClick = onLeftClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 8.dp)
                    .zIndex(1f)
            )
            SwipePreferenceActionBlock(
                action = rightAction,
                direction = SwipeDirection.Right,
                onClick = onRightClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-8).dp)
                    .zIndex(1f)
            )
            SwipePreferenceActionBlock(
                action = upAction,
                direction = SwipeDirection.Up,
                onClick = onUpClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 12.dp)
                    .zIndex(1f)
            )
            SwipePreferenceActionBlock(
                action = SwipeAction.Keep,
                direction = SwipeDirection.Down,
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-12).dp)
                    .zIndex(1f),
                isEnabled = false,
                labelOverride = strings.move,
                iconOverride = Icons.Outlined.DriveFileMove,
                colorOverride = Color(0xFF8B5CF6)
            )
        }
    }
}

@Composable
private fun SwipePreferenceCardStack(
    cardWidth: Dp,
    cardHeight: Dp
) {
    val layers = listOf(
        2 to 0.92f,
        1 to 0.96f,
        0 to 1f
    )
    layers.forEach { (index, scale) ->
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .offset(y = (index * 12).dp)
                .scale(scale)
                .shadow(10.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
private fun SwipePreferenceActionDialog(
    direction: SwipeDirection,
    currentAction: SwipeAction,
    actions: List<SwipeAction>,
    onSelect: (SwipeAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val downDirectionLabel = getNewPageStrings(LocalAppLanguage.current).swipeDownLabel
    val title = when (direction) {
        SwipeDirection.Left -> strings.swipeLeft
        SwipeDirection.Right -> strings.swipeRight
        SwipeDirection.Up -> strings.swipeUp
        SwipeDirection.Down -> downDirectionLabel
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                actions.forEach { action ->
                    val isSelected = action == currentAction
                    val rowColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(rowColor)
                            .bounceClick(onClick = { onSelect(action) })
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = swipeActionIcon(action),
                            contentDescription = null,
                            tint = swipeActionColor(action),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = swipeActionLabel(action, strings),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SwipePreferenceActionBlock(
    action: SwipeAction,
    direction: SwipeDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    labelOverride: String? = null,
    iconOverride: ImageVector? = null,
    colorOverride: Color? = null
) {
    val strings = LocalAppStrings.current
    val downDirectionLabel = getNewPageStrings(LocalAppLanguage.current).swipeDownLabel
    val color = colorOverride ?: swipeActionColor(action)
    val textColor = if (action == SwipeAction.None) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color.White
    val startAlpha = if (action == SwipeAction.None) 0.18f else 0.92f
    val endAlpha = if (action == SwipeAction.None) 0.06f else 0.35f
    val brush = when (direction) {
        SwipeDirection.Left -> Brush.horizontalGradient(
            colors = listOf(color.copy(alpha = startAlpha), color.copy(alpha = endAlpha))
        )
        SwipeDirection.Right -> Brush.horizontalGradient(
            colors = listOf(color.copy(alpha = endAlpha), color.copy(alpha = startAlpha))
        )
        SwipeDirection.Up -> Brush.verticalGradient(
            colors = listOf(color.copy(alpha = startAlpha), color.copy(alpha = endAlpha))
        )
        SwipeDirection.Down -> Brush.verticalGradient(
            colors = listOf(color.copy(alpha = endAlpha), color.copy(alpha = startAlpha))
        )
    }
    val directionLabel = when (direction) {
        SwipeDirection.Left -> strings.swipeLeft
        SwipeDirection.Right -> strings.swipeRight
        SwipeDirection.Up -> strings.swipeUp
        SwipeDirection.Down -> downDirectionLabel
    }
    val actionLabel = labelOverride ?: swipeActionLabel(action, strings)
    val actionIcon = iconOverride ?: swipeActionIcon(action)
    val clickModifier = if (isEnabled) Modifier.bounceClick(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = directionLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = actionLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}


