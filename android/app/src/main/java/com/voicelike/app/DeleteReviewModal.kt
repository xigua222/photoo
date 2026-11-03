package com.voicelike.app

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DeleteReviewModal(
    pendingTrashIds: Set<Long>,
    trashItems: List<MediaItem>,
    onConfirmDelete: (Set<Long>) -> Unit,
    onRemoveFromQueue: (Set<Long>) -> Unit,
    onRemoveSingleFromQueue: (Long) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    currentDeletionMode: String = "recycle_bin",
    onSettingsClick: () -> Unit = {}
) {
    // Nothing is marked on entry; a tap marks a photo to keep. Marked photos leave the queue,
    // the rest can be deleted in one go.
    val keptStates = remember(pendingTrashIds) { mutableStateMapOf<Long, Boolean>() }
    var previewItem by remember { mutableStateOf<MediaItem?>(null) }
    var previewRemoveConfirmItem by remember { mutableStateOf<MediaItem?>(null) }
    val previewSourceBoundsById = remember { mutableStateMapOf<Long, androidx.compose.ui.geometry.Rect>() }
    var hiddenPreviewSourceId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    val strings = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val context = LocalContext.current
    val options = organizeOptionsStrings(appLanguage)
    val keptIds = keptStates.selectedIds()
    val keptCount = keptIds.size
    val queueIds = if (trashItems.isNotEmpty()) trashItems.mapTo(HashSet()) { it.id } else pendingTrashIds
    val restIds = queueIds - keptIds
    val allKept = queueIds.isNotEmpty() && restIds.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .bottomNavigationBarPadding(16.dp)
        ) {
            UnifiedSubpageTopBar(
                title = if (queueIds.isNotEmpty()) {
                    "${strings.deleteReviewTitle} (${queueIds.size})"
                } else {
                    strings.deleteReviewTitle
                },
                onBack = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDismiss()
                },
                backContentDescription = strings.exit,
                trailing = {
                    UnifiedSubpageTopBarIconButton(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = strings.settings,
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSettingsClick()
                        }
                    )
                }
            )

            if (currentDeletionMode != "direct_delete") {
                val period = when (appLanguage) {
                    AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "\u3002"
                    else -> "."
                }
                val annotatedString = buildAnnotatedString {
                    append(strings.deleteReviewWarningBody)
                    pushStringAnnotation(tag = "URL", annotation = "recycle_bin")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF2196F3),
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(strings.deleteReviewWarningLink)
                    }
                    pop()
                    append(period)
                }

                ClickableText(
                    text = annotatedString,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToRecycleBin()
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }

            if (trashItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = options.keepHint,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f)
                    )
                    // Keep everything, then tap the few that should go.
                    Text(
                        text = if (allKept) strings.recycleBinUnselectAll else strings.recycleBinSelectAll,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (allKept) {
                                    keptStates.clear()
                                } else {
                                    queueIds.forEach { keptStates[it] = true }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            if (pendingTrashIds.isNotEmpty() && trashItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(items = trashItems, key = { it.id }) { item ->
                        val isKept = keptStates[item.id] == true
                        val imageRequest = remember(item.id, item.uri, context) {
                            ImageRequest.Builder(context)
                                .data(item.uri)
                                .crossfade(false)
                                .placeholder(ColorDrawable(Color.LightGray.toArgb()))
                                .size(200, 200)
                                .dispatcher(Dispatchers.IO)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .graphicsLayer {
                                    alpha = if (hiddenPreviewSourceId == item.id) 0f else 1f
                                }
                                .onGloballyPositioned { coordinates ->
                                    previewSourceBoundsById[item.id] = coordinates.boundsInRoot()
                                }
                                .combinedClickable(
                                    onClick = {
                                        if (hapticsAllowed) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        if (isKept) {
                                            keptStates.remove(item.id)
                                        } else {
                                            keptStates[item.id] = true
                                        }
                                    },
                                    onLongClick = {
                                        if (hapticsAllowed) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        previewItem = item
                                        hiddenPreviewSourceId = null
                                    }
                                )
                        ) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(
                                        // Unmarked: a dark veil so the ring reads on white screenshots too.
                                        if (isKept) KEEP_MARK_COLOR else Color.Black.copy(alpha = 0.22f),
                                        CircleShape
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isKept) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (queueIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (keptCount > 0) {
                        // Marked photos leave the queue and go back to the album; the rest stay queued.
                        Button(
                            onClick = {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRemoveFromQueue(keptIds)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            ActionLabel(
                                text = options.keepOnlyFmt.format(keptCount),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    if (restIds.isNotEmpty()) {
                        Button(
                            onClick = {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Keep the marked ones first, then delete everything else in one request.
                                if (keptIds.isNotEmpty()) onRemoveFromQueue(keptIds)
                                onConfirmDelete(restIds)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Beside the keep button the label needs the room more than the icon does.
                            if (keptCount == 0) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            ActionLabel(
                                text = if (keptCount > 0) {
                                    options.deleteRestFmt.format(restIds.size)
                                } else {
                                    options.deleteAllFmt.format(restIds.size)
                                },
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        previewItem?.let { item ->
            MediaPreviewOverlay(
                item = item,
                sourceBoundsInRoot = previewSourceBoundsById[item.id],
                onSharedDismissStart = { sourceItemId ->
                    hiddenPreviewSourceId = sourceItemId
                },
                sharedTargetCornerRadius = 8.dp,
                sharedScaleClampMin = 0.04f,
                bottomOverlay = { activeItem ->
                    if (pendingTrashIds.contains(activeItem.id)) {
                        FilledTonalButton(
                            onClick = {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previewRemoveConfirmItem = activeItem
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.45f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp)
                                .bottomNavigationBarPadding(24.dp)
                        ) {
                            Text(strings.deleteReviewRemoveFromQueue)
                        }
                    }
                },
                onDismiss = {
                    if (hiddenPreviewSourceId != null) {
                        previewItem = null
                        scope.launch {
                            withFrameNanos { }
                            if (previewItem == null) {
                                hiddenPreviewSourceId = null
                            }
                        }
                    } else {
                        previewItem = null
                        hiddenPreviewSourceId = null
                    }
                }
            )
        }

        previewRemoveConfirmItem?.let { removeTarget ->
            AlertDialog(
                onDismissRequest = { previewRemoveConfirmItem = null },
                title = { Text(strings.deleteReviewRemoveFromQueue) },
                text = { Text(strings.deleteReviewRemoveFromQueueMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            previewRemoveConfirmItem = null
                            previewItem = null
                            hiddenPreviewSourceId = null
                            onRemoveSingleFromQueue(removeTarget.id)
                        }
                    ) {
                        Text(strings.remove, color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { previewRemoveConfirmItem = null }) {
                        Text(strings.cancel, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )
        }
    }
}

// One line that shrinks to fit half the bar in long languages.
@Composable
private fun ActionLabel(text: String, color: Color) {
    BasicText(
        text = text,
        style = TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp)
    )
}

// Green, not red: a mark means the photo stays.
private val KEEP_MARK_COLOR = Color(0xFF16A34A)

private fun SnapshotStateMap<Long, Boolean>.selectedIds(): Set<Long> {
    return entries.asSequence()
        .filter { it.value }
        .map { it.key }
        .toSet()
}
