package com.voicelike.app

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Iso
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DraggableCardSwipeIndicatorSpec(
    val label: String,
    val color: Color,
    val icon: ImageVector
)

// Organize palette (iOS ChunUI parity): card surface, panel, foreground and swipe action inks.
internal fun organizeCardSurfaceColor(isDark: Boolean): Color =
    if (isDark) Color(0xFF27272A) else Color.White

internal fun organizePanelColor(isDark: Boolean): Color =
    if (isDark) Color(0xFF1F1F22) else Color(0xFFF7F7FA)

internal fun organizeForegroundColor(isDark: Boolean): Color =
    if (isDark) Color(0xFFFAFAFA) else Color(0xFF171717)

internal fun organizeBorderColor(isDark: Boolean): Color =
    if (isDark) Color(0xFF3F3F46) else Color(0xFFDCDCE3)

internal fun organizeSwipeActionInkColor(action: SwipeAction, isDark: Boolean): Color {
    return when (action) {
        SwipeAction.Delete -> if (isDark) Color(0xFFE07A94) else Color(0xFFC13B5F)
        SwipeAction.Like -> if (isDark) Color(0xFFFFCB3D) else Color(0xFFB98500)
        SwipeAction.Keep -> if (isDark) Color(0xFF5EC4B4) else Color(0xFF2F9E8F)
        SwipeAction.None -> swipeActionColor(SwipeAction.None)
    }
}

/** Screen-edge glow colour; Like glows in the bright like-yellow rather than its darker ink. */
internal fun organizeSwipeActionGlowColor(action: SwipeAction, isDark: Boolean): Color {
    return when (action) {
        SwipeAction.Like -> Color(0xFFFFC21A)
        else -> organizeSwipeActionInkColor(action, isDark)
    }
}

/** A tag hanging from the photo's top edge: bottom corners rounded, card fill, 2dp action stroke. */
@Composable
private fun SwipeActionIndicatorBadge(
    label: String,
    color: Color,
    icon: ImageVector
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
    Row(
        modifier = Modifier
            .padding(end = 24.dp)
            .background(organizeCardSurfaceColor(isDark), shape)
            .border(2.dp, color, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 15.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun DraggableCardActionIndicators(
    isTopCard: Boolean,
    isExpanded: Boolean,
    suppressActionIndicators: Boolean,
    isFolderMode: Boolean,
    swipeIntentPreview: SwipeIntent,
    upAction: SwipeAction,
    rightAction: SwipeAction,
    leftAction: SwipeAction,
    upIndicatorSpec: DraggableCardSwipeIndicatorSpec? = null,
    rightIndicatorSpec: DraggableCardSwipeIndicatorSpec? = null,
    leftIndicatorSpec: DraggableCardSwipeIndicatorSpec? = null
) {
    if (!isTopCard || isExpanded || suppressActionIndicators) return
    val strings = LocalAppStrings.current
    val isDark = isSystemInDarkTheme()
    val moveColor = MaterialTheme.colorScheme.onBackground

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !isFolderMode && swipeIntentPreview == SwipeIntent.Up,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            val spec = upIndicatorSpec ?: DraggableCardSwipeIndicatorSpec(
                label = swipeActionLabel(upAction, strings),
                color = organizeSwipeActionInkColor(upAction, isDark),
                icon = swipeActionIconForDirection(upAction, SwipeDirection.Up)
            )
            SwipeActionIndicatorBadge(label = spec.label, color = spec.color, icon = spec.icon)
        }

        AnimatedVisibility(
            visible = !isFolderMode && swipeIntentPreview == SwipeIntent.Right,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            val spec = rightIndicatorSpec ?: DraggableCardSwipeIndicatorSpec(
                label = swipeActionLabel(rightAction, strings),
                color = organizeSwipeActionInkColor(rightAction, isDark),
                icon = swipeActionIconForDirection(rightAction, SwipeDirection.Right)
            )
            SwipeActionIndicatorBadge(label = spec.label, color = spec.color, icon = spec.icon)
        }

        AnimatedVisibility(
            visible = !isFolderMode && swipeIntentPreview == SwipeIntent.Left,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            val spec = leftIndicatorSpec ?: DraggableCardSwipeIndicatorSpec(
                label = swipeActionLabel(leftAction, strings),
                color = organizeSwipeActionInkColor(leftAction, isDark),
                icon = swipeActionIconForDirection(leftAction, SwipeDirection.Left)
            )
            SwipeActionIndicatorBadge(label = spec.label, color = spec.color, icon = spec.icon)
        }

        AnimatedVisibility(
            visible = isFolderMode,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            SwipeActionIndicatorBadge(
                label = strings.move,
                color = moveColor,
                icon = Icons.Outlined.DriveFileMove
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun DraggableCardDetailsSheet(
    isExpanded: Boolean,
    currentItem: MediaItem,
    onDismiss: () -> Unit,
    onJumpToDay: ((String) -> Unit)?
) {
    if (!isExpanded) return

    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)
    ) {
        DraggableCardDetailsContent(
            currentItem = currentItem,
            onJumpToDay = onJumpToDay,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        )
    }
}

// What the (i) sheet shows; also docked beside the deck on wide screens (OrganizeSplitLayout).
@Composable
internal fun DraggableCardDetailsContent(
    currentItem: MediaItem,
    onJumpToDay: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val commonUiCopy = remember(language) { getCommonGlobalUiCopy(language) }
    val isDark = isSystemInDarkTheme()
    val secondaryTextColor = if (isDark) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.Black.copy(alpha = 0.4f)
    }
    val primaryTextColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val metricIconTint = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    } else {
        Color.Black.copy(alpha = 0.8f)
    }
    val accentColor = Color(0xFF2F80ED)
    val dateFontSize = 15.sp
    val jumpIconSize = 21.dp
    val filenameMaxWidth = 240.dp
    val filenameFontSize = 12.sp
    val filenameLabelFontSize = 10.sp
    val headerGap = 12.dp
    val headerMetaGap = 4.dp
    val detailTopGap = 16.dp
    val gridSpacer = 12.dp
    val iconSize = 20.dp
    val valueFontSize = 13.sp
    val labelSpacer = 2.dp
    val valueWidth = 128.dp
    val valueHeight = 20.dp
    val metricLabelFontSize = 10.sp
    val metricLabelLetterSpacing = 1.sp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val date = Date(currentItem.dateAdded * 1000)
        val format = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayKey = dayKeyFormat.format(date)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onJumpToDay?.invoke(dayKey)
                }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                format.format(date),
                fontFamily = FontFamily.Monospace,
                fontSize = dateFontSize,
                color = accentColor,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            TimeMachineBenchIcon(
                modifier = Modifier
                    .size(jumpIconSize)
                    .semantics { contentDescription = commonUiCopy.jumpToDay },
                tint = accentColor
            )
        }
        Spacer(modifier = Modifier.height(headerGap))

        Text(
            currentItem.displayName,
            fontFamily = FontFamily.Serif,
            fontSize = filenameFontSize,
            color = primaryTextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = filenameMaxWidth)
        )
        Spacer(modifier = Modifier.height(headerMetaGap))
        Text(
            strings.filename,
            fontSize = filenameLabelFontSize,
            color = secondaryTextColor,
            letterSpacing = metricLabelLetterSpacing
        )

        Spacer(modifier = Modifier.height(detailTopGap))
        val isVideoDetail = currentItem.type == "video"
        val resolvedCameraForDisplay = DeviceModelNameResolver.resolveCameraLabel(context, currentItem.camera)
        val cameraLabel = if (isVideoDetail) newPageStrings.videoCodecLabel else strings.camera
        val metric1Label = if (isVideoDetail) newPageStrings.videoBitrateLabel else strings.iso
        val metric2Label = if (isVideoDetail) newPageStrings.videoFpsLabel else strings.shutter
        val metric3Label = if (isVideoDetail) newPageStrings.videoDurationLabel else strings.focalLabel
        val metric4Label = if (isVideoDetail) newPageStrings.videoAudioLabel else strings.apertureLabel

        Row(Modifier.fillMaxWidth()) {
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.Image, strings.resolution, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                value = currentItem.resolution.ifEmpty { "--" },
                label = strings.resolution,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
            )
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.Save, strings.size, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                value = if (currentItem.size > 1024 * 1024) {
                    String.format("%.1f MB", currentItem.size / 1024.0 / 1024.0)
                } else {
                    String.format("%.0f KB", currentItem.size / 1024.0)
                },
                label = strings.size,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
            )
        }

        Spacer(modifier = Modifier.height(gridSpacer))

        Row(Modifier.fillMaxWidth()) {
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.PhotoCamera, cameraLabel, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                label = cameraLabel,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
                valueContent = {
                    Box(
                        modifier = Modifier
                            .width(valueWidth)
                            .height(valueHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resolvedCameraForDisplay,
                            fontFamily = FontFamily.Serif,
                            fontSize = valueFontSize,
                            color = primaryTextColor,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                                spacing = MarqueeSpacing(24.dp),
                                velocity = 28.dp
                            )
                        )
                    }
                }
            )
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.LocationOn, strings.location, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                label = strings.location,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
                valueContent = {
                    Box(
                        modifier = Modifier
                            .width(valueWidth)
                            .height(valueHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentItem.location == "Unknown Location") strings.unknownLocation else currentItem.location,
                            fontFamily = FontFamily.Serif,
                            fontSize = valueFontSize,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(gridSpacer))

        Row(Modifier.fillMaxWidth()) {
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.Iso, metric1Label, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                value = currentItem.iso,
                label = metric1Label,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
            )
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.Timer, metric2Label, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                value = currentItem.shutter,
                label = metric2Label,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
            )
        }

        Spacer(modifier = Modifier.height(gridSpacer))

        Row(Modifier.fillMaxWidth()) {
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.Camera, metric3Label, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                value = currentItem.focalLength,
                label = metric3Label,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
            )
            CardDetailMetricCell(
                icon = {
                    Icon(Icons.Outlined.Lens, metric4Label, modifier = Modifier.size(iconSize), tint = metricIconTint)
                },
                value = currentItem.aperture,
                label = metric4Label,
                labelColor = secondaryTextColor,
                valueColor = primaryTextColor,
                valueFontSize = valueFontSize,
                labelSpacer = labelSpacer,
            )
        }
    }
}

@Composable
private fun RowScope.CardDetailMetricCell(
    icon: @Composable () -> Unit,
    label: String,
    labelColor: Color,
    valueColor: Color,
    valueFontSize: TextUnit,
    labelSpacer: Dp,
    value: String? = null,
    valueContent: (@Composable () -> Unit)? = null
) {
    val detailTextStyle = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.heightIn(min = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.height(22.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.height(labelSpacer))
            if (valueContent != null) {
                valueContent()
            } else {
                Text(
                    text = value.orEmpty(),
                    fontFamily = FontFamily.Serif,
                    fontSize = valueFontSize,
                    color = valueColor,
                    style = detailTextStyle
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                color = labelColor,
                letterSpacing = 1.sp,
                style = detailTextStyle
            )
        }
    }
}
