package com.voicelike.app.keep

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.voicelike.app.FullScreenVideoPlayer
import com.voicelike.app.MediaItem
import com.voicelike.app.SharedPagedFullScreenImageViewer
import com.voicelike.app.SharedPagedViewerOrientation
import com.voicelike.app.SimilarImageViewerPage
import com.voicelike.app.fullScreenSwipeToDismiss
import com.voicelike.app.rememberFullScreenSwipeDismissState

@Composable
internal fun KeepGalleryPreviewOverlay(
    items: List<MediaItem>,
    startIndex: Int,
    sourceBoundsInRoot: Rect? = null,
    sourceBoundsByItemId: Map<Long, Rect> = emptyMap(),
    onActiveItemChanged: (MediaItem) -> Unit,
    onSharedDismissStart: ((Long) -> Unit)? = null,
    onShare: (MediaItem) -> Unit,
    onTertiaryAction: (MediaItem) -> Unit,
    tertiaryActionIcon: ImageVector = Icons.Outlined.FavoriteBorder,
    tertiaryActionContentDescription: String = "Like",
    tertiaryActionLabel: String = tertiaryActionContentDescription,
    tertiaryActionTint: Color = Color(0xFFF59E0B),
    onDelete: (MediaItem) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    var deleteConfirmItem by remember { mutableStateOf<MediaItem?>(null) }

    SharedPagedFullScreenImageViewer(
        items = items,
        initialIndex = startIndex.coerceIn(0, items.lastIndex),
        onDismiss = onDismissRequest,
        sourceBoundsInRoot = sourceBoundsInRoot,
        sourceBoundsByItemId = sourceBoundsByItemId,
        onPageChanged = { page ->
            items.getOrNull(page)?.let(onActiveItemChanged)
        },
        onSharedDismissStart = onSharedDismissStart,
        orientation = SharedPagedViewerOrientation.Horizontal,
        sharedTargetCornerRadius = 5.dp,
        sharedScaleClampMin = 0.04f,
        chrome = { currentItem, _, requestDismiss ->
            var metadataExpanded by rememberSaveable(currentItem?.id) { mutableStateOf(false) }

            BackHandler(enabled = metadataExpanded) {
                metadataExpanded = false
            }

            currentItem?.let { activeItem ->
                KeepGalleryMetadataDrawer(
                    item = activeItem,
                    expanded = metadataExpanded,
                    onExpandedChange = { metadataExpanded = it },
                    onShare = { onShare(activeItem) },
                    onTertiaryAction = { onTertiaryAction(activeItem) },
                    tertiaryActionIcon = tertiaryActionIcon,
                    tertiaryActionContentDescription = tertiaryActionContentDescription,
                    tertiaryActionLabel = tertiaryActionLabel,
                    tertiaryActionTint = tertiaryActionTint,
                    onDelete = { deleteConfirmItem = activeItem },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            IconButton(
                onClick = requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { _, item, requestDismiss, _, _, onZoomStateChanged ->
        if (item.type == "video") {
            KeepGalleryPreviewVideoPage(
                item = item,
                onDismiss = requestDismiss,
                onZoomStateChanged = onZoomStateChanged
            )
        } else {
            SimilarImageViewerPage(
                item = item,
                onDismiss = requestDismiss,
                isMuted = false,
                allowLivePlayback = true,
                compareOverrideItem = null,
                onZoomStateChanged = onZoomStateChanged
            )
        }
    }

    deleteConfirmItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = { Text("确认删除") },
            text = { Text("这张内容会被直接删除，确认继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmItem = null
                        onDelete(item)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmItem = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun KeepGalleryPreviewVideoPage(
    item: MediaItem,
    onDismiss: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit
) {
    var isMuted by rememberSaveable(item.id) { mutableStateOf(false) }
    val fullScreenSwipeDismissState = rememberFullScreenSwipeDismissState(onDismiss = onDismiss)

    LaunchedEffect(item.id) {
        onZoomStateChanged(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .fullScreenSwipeToDismiss(
                enabled = true,
                state = fullScreenSwipeDismissState
            )
            .graphicsLayer {
                translationY = fullScreenSwipeDismissState.offsetY
                scaleX = fullScreenSwipeDismissState.contentScale
                scaleY = fullScreenSwipeDismissState.contentScale
            }
    ) {
        FullScreenVideoPlayer(
            item = item,
            isMuted = isMuted,
            onMuteChange = { muted -> isMuted = muted },
            onDismiss = {},
            dismissEnabled = false,
            enableOpenCloseTransition = false,
            showCloseButton = false,
            showTopBar = false,
            controlsBottomPadding = 150.dp
        )
    }
}
