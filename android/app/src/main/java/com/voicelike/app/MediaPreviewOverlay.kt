package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MediaPreviewOverlay(
    item: MediaItem,
    sourceBoundsInRoot: Rect? = null,
    onSharedDismissStart: ((Long) -> Unit)? = null,
    sharedTargetCornerRadius: Dp = 24.dp,
    sharedScaleClampMin: Float = 0.12f,
    bottomOverlay: (@Composable BoxScope.(MediaItem) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (item.type == "video") {
            FullScreenVideoPlayer(
                item = item,
                isMuted = false,
                onMuteChange = {},
                onDismiss = onDismiss,
                dismissEnabled = true,
                enableOpenCloseTransition = true,
                showCloseButton = true,
                showTopBar = true
            )
        } else {
            MainFullScreenImageViewer(
                item = item,
                sourceBoundsInRoot = sourceBoundsInRoot,
                onSharedDismissStart = onSharedDismissStart,
                sharedTargetCornerRadius = sharedTargetCornerRadius,
                sharedScaleClampMin = sharedScaleClampMin,
                bottomOverlay = bottomOverlay,
                onDismiss = onDismiss
            )
        }
    }
}
