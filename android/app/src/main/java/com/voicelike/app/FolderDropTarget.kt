package com.voicelike.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FolderData(
    val id: String, // bucketId or "new_folder"
    val name: String,
    val path: String?, // null for new folder
    val count: Int
)

@Composable
fun FolderDropRow(
    visible: Boolean,
    folders: List<FolderData>,
    activeDropTargetId: String?, // The folder currently under the dragged card
    onFolderPositionsChanged: (Map<String, androidx.compose.ui.geometry.Rect>) -> Unit,
    modifier: Modifier = Modifier,
    pinnedFolderIds: Set<String> = emptySet(),
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    val rects = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    LaunchedEffect(visible) {
        if (!visible) {
            rects.clear()
            onFolderPositionsChanged(emptyMap())
        }
    }

    val updateRect = remember {
        { id: String, rect: androidx.compose.ui.geometry.Rect ->
            rects[id] = rect
            onFolderPositionsChanged(rects.toMap())
        }
    }

    val removeRect = remember {
        { id: String ->
            if (rects.remove(id) != null) {
                onFolderPositionsChanged(rects.toMap())
            }
        }
    }
    val dropRowFlingBehavior = rememberVelocityLimitedFlingBehavior(velocityScale = 0.34f)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = tween(200)) { it } + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(animationSpec = tween(200)) { it } + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        LazyRow(
            state = listState,
            flingBehavior = dropRowFlingBehavior,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Existing Folders
            items(folders) { folder ->
                DisposableEffect(folder.id) {
                    onDispose { removeRect(folder.id) }
                }
                FolderItem(
                    folder = folder,
                    isActive = activeDropTargetId == folder.id,
                    isPinned = folder.id in pinnedFolderIds,
                    onPositioned = { rect ->
                        updateRect(folder.id, rect)
                    }
                )
            }

            // New folder goes last, so the left edge of the row lands on the top folder instead.
            item {
                val strings = LocalAppStrings.current
                DisposableEffect(Unit) {
                    onDispose { removeRect("new_folder") }
                }
                FolderItem(
                    folder = FolderData("new_folder", strings.newFolder, null, 0),
                    isActive = activeDropTargetId == "new_folder",
                    onPositioned = { rect ->
                        updateRect("new_folder", rect)
                    }
                )
            }
        }
    }
}

@Composable
fun FolderItem(
    folder: FolderData,
    isActive: Boolean,
    isPinned: Boolean = false,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isActive -> 1.2f
            isPinned -> 0.95f
            else -> 0.82f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 320f)
    )
    val lift by animateDpAsState(
        targetValue = if (isActive) (-8).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 320f)
    )
    // Fixed per item: growing the active one would push every later target under the finger.
    val itemWidth = if (isPinned) 84.dp else 68.dp
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val activeColor = Color(0xFF9C27B0)
    val borderColor = if (isActive) activeColor else Color.Transparent
    val backgroundColor = if (isActive) Color(0xFFF3E8FF) else if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(itemWidth)
            // Measured before lift/scale: the hit rect is the item's slot, so an item growing as it
            // activates can't push itself out from under the finger and flicker.
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                onPositioned(
                    androidx.compose.ui.geometry.Rect(
                        left = position.x,
                        top = position.y,
                        right = position.x + size.width,
                        bottom = position.y + size.height
                    )
                )
            }
            .offset(y = lift)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (folder.id == "new_folder") {
                val strings = LocalAppStrings.current
                Icon(
                    imageVector = Icons.Outlined.CreateNewFolder,
                    contentDescription = strings.newFolder,
                    tint = if (isActive) activeColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = "Folder",
                    tint = if (isActive) activeColor else Color(0xFFFFC107), // Amber for folders
                    modifier = Modifier.size(32.dp)
                )
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = if (isActive) activeColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(13.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = folder.name,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) activeColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (folder.count > 0) {
            Text(
                text = "${folder.count}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}
