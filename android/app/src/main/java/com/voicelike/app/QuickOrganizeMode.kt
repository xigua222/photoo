package com.voicelike.app

import android.content.Context
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import com.voicelike.app.liquidglass.LocalLiquidGlassBackdrop
import com.voicelike.app.liquidglass.liquidGlassPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

fun buildFolderTargetsForOrganize(
    allMedia: List<MediaItem>,
    currentFolder: String?,
    hiddenFolders: Set<String>,
    prioritizedFolders: Set<String>,
    folderUsage: Map<String, Int>,
    folderLastUsed: Map<String, Long>,
    userCreatedFolders: Set<String> = emptySet()
): List<FolderData> {
    val grouped = allMedia.groupBy { it.bucketName }
    val existingTargets = grouped
        .filter { (bucketId, _) ->
            bucketId != currentFolder && !hiddenFolders.contains(bucketId)
        }
        .map { (bucketId, items) ->
            val firstPath = items.firstOrNull()?.path.orEmpty()
            val relFolder = if (firstPath.isNotEmpty()) {
                val root = Environment.getExternalStorageDirectory().absolutePath
                val normalized = if (firstPath.startsWith(root)) {
                    firstPath.substring(root.length).trimStart('/')
                } else {
                    firstPath
                }
                normalized.substringBeforeLast("/")
            } else {
                "Pictures"
            }
            val displayName = items.firstOrNull()?.bucketName?.takeIf { it.isNotBlank() }
                ?: relFolder.substringAfterLast("/")
            FolderData(
                id = bucketId,
                name = displayName,
                path = relFolder,
                count = items.size
            )
        }
    val existingIds = existingTargets.map { it.id }.toSet()
    val syntheticTargets = userCreatedFolders
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { it != currentFolder && !hiddenFolders.contains(it) && !existingIds.contains(it) }
        .map { folderName ->
            FolderData(
                id = folderName,
                name = folderName,
                path = null,
                count = 0
            )
        }
        .toList()
    return (existingTargets + syntheticTargets)
        .sortedWith(
            compareByDescending<FolderData> { prioritizedFolders.contains(it.id) }
                .thenByDescending {
                    val lastUsed = folderLastUsed[it.id] ?: 0L
                    (System.currentTimeMillis() - lastUsed) < 300_000L
                }
                .thenByDescending { folderUsage[it.id] ?: 0 }
                .thenByDescending { it.count }
        )
}

fun markQuickOrganizeFolderUsed(
    prefs: PreferencesManager,
    folderId: String,
    nowMs: Long = System.currentTimeMillis()
): MutableMap<String, Int> {
    val currentUsage = prefs.folderUsage.toMutableMap()
    currentUsage[folderId] = (currentUsage[folderId] ?: 0) + 1
    prefs.folderUsage = currentUsage

    val currentLastUsed = prefs.folderLastUsed.toMutableMap()
    currentLastUsed[folderId] = nowMs
    prefs.folderLastUsed = currentLastUsed
    // Pinning stays a manual choice: recency and usage already order recently used folders first.
    return currentUsage
}

fun registerQuickOrganizeFolderMetadata(
    prefs: PreferencesManager,
    folderName: String,
    nowMs: Long = System.currentTimeMillis()
): MutableMap<String, Int> {
    val normalized = folderName.trim()
    if (normalized.isEmpty()) return prefs.folderUsage

    val usage = prefs.folderUsage.toMutableMap()
    usage[normalized] = (usage[normalized] ?: 0) + 1
    prefs.folderUsage = usage

    val lastUsed = prefs.folderLastUsed.toMutableMap()
    lastUsed[normalized] = nowMs
    prefs.folderLastUsed = lastUsed

    val userCreated = prefs.userCreatedFolders
    userCreated.add(normalized)
    prefs.userCreatedFolders = userCreated

    return usage
}

suspend fun createFolderOnlyForQuickMode(
    context: Context,
    baseDir: String,
    folderName: String
): Boolean {
    val normalized = folderName.trim()
    if (normalized.isEmpty()) return false
    return withContext(Dispatchers.IO) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(baseDir),
            normalized
        )
        val ok = dir.exists() || dir.mkdirs()
        if (ok) {
            runCatching { FileOperationManager.scanFile(context, dir.absolutePath) }
        }
        ok
    }
}

internal val QUICK_ORGANIZE_DRAWER_COLLAPSED_HEIGHT = 118.dp
internal val QUICK_ORGANIZE_DRAWER_BUTTON_CLEARANCE = 8.dp

private val QUICK_ORGANIZE_DRAWER_TOP_RADIUS = 28.dp
private val QUICK_ORGANIZE_DRAWER_HEADER_HEIGHT = 26.dp
private val QUICK_ORGANIZE_DRAWER_CHIP_HEIGHT = 40.dp
private const val QUICK_ORGANIZE_DRAWER_COLLAPSED_ROWS = 2
private val QUICK_ORGANIZE_DRAWER_HORIZONTAL_PADDING = 12.dp
private val QUICK_ORGANIZE_DRAWER_TOP_PADDING = 6.dp
private val QUICK_ORGANIZE_DRAWER_BOTTOM_PADDING = 6.dp
private val QUICK_ORGANIZE_DRAWER_CHIP_SPACING = 8.dp
private val QUICK_ORGANIZE_DRAWER_COLLAPSED_PEEK_REVEAL = 1.dp
private const val QUICK_ORGANIZE_DRAWER_EXPANDED_HEIGHT_FRACTION = 0.46f
private val QUICK_ORGANIZE_DRAWER_COLLAPSED_EXTRA_HEIGHT =
    QUICK_ORGANIZE_DRAWER_CHIP_SPACING + QUICK_ORGANIZE_DRAWER_COLLAPSED_PEEK_REVEAL
private val QUICK_ORGANIZE_DRAWER_MIN_VISUAL_BOTTOM_INSET = 8.dp
private val QUICK_ORGANIZE_DRAWER_MAX_VISUAL_BOTTOM_INSET = 12.dp
private val QUICK_ORGANIZE_DRAWER_COLLAPSED_CONTENT_HEIGHT =
    QUICK_ORGANIZE_DRAWER_HEADER_HEIGHT +
        QUICK_ORGANIZE_DRAWER_TOP_PADDING +
        QUICK_ORGANIZE_DRAWER_BOTTOM_PADDING +
        (QUICK_ORGANIZE_DRAWER_CHIP_HEIGHT * QUICK_ORGANIZE_DRAWER_COLLAPSED_ROWS) +
        (QUICK_ORGANIZE_DRAWER_CHIP_SPACING * (QUICK_ORGANIZE_DRAWER_COLLAPSED_ROWS - 1)) +
        QUICK_ORGANIZE_DRAWER_COLLAPSED_EXTRA_HEIGHT

private data class QuickOrganizeDrawerEntry(
    val id: String,
    val title: String,
    val isCreate: Boolean
)

@Composable
private fun rememberQuickOrganizeDrawerVisualBottomInset(): androidx.compose.ui.unit.Dp {
    val systemBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (systemBottomInset > 0.dp) {
        systemBottomInset.coerceIn(
            minimumValue = QUICK_ORGANIZE_DRAWER_MIN_VISUAL_BOTTOM_INSET,
            maximumValue = QUICK_ORGANIZE_DRAWER_MAX_VISUAL_BOTTOM_INSET
        )
    } else {
        0.dp
    }
}

@Composable
internal fun rememberQuickOrganizeBottomControlPadding() =
    QUICK_ORGANIZE_DRAWER_COLLAPSED_CONTENT_HEIGHT +
        QUICK_ORGANIZE_DRAWER_BUTTON_CLEARANCE

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickOrganizeBar(
    visible: Boolean,
    entranceProgress: Float = 1f,
    folders: List<FolderData>,
    onCreateFolderClick: () -> Unit,
    onFolderClick: (FolderData) -> Unit,
    interactive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val visualBottomPadding = rememberQuickOrganizeDrawerVisualBottomInset()
    val navigationBarBottomPadding = visualBottomPadding
    val advancedTextureEnabled = LocalAdvancedTextureEnabled.current
    val collapsedBlockedHeight =
        QUICK_ORGANIZE_DRAWER_BOTTOM_PADDING +
            visualBottomPadding +
            QUICK_ORGANIZE_DRAWER_CHIP_SPACING +
            QUICK_ORGANIZE_DRAWER_COLLAPSED_PEEK_REVEAL
    var acknowledgedFolderId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var keepMounted by remember { mutableStateOf(visible) }

    LaunchedEffect(acknowledgedFolderId) {
        if (acknowledgedFolderId != null) {
            delay(520)
            acknowledgedFolderId = null
        }
    }
    LaunchedEffect(expanded) {
        if (!expanded) {
            scrollState.scrollTo(0)
        }
    }
    LaunchedEffect(visible) {
        if (visible) {
            keepMounted = true
        } else {
            expanded = false
            delay(180)
            keepMounted = false
        }
    }
    if (!keepMounted) return
    val safeEntranceProgress = entranceProgress.coerceIn(0f, 1f)

    val entries = remember(folders) {
        buildList {
            add(
                QuickOrganizeDrawerEntry(
                    id = "new_folder_capsule",
                    title = "",
                    isCreate = true
                )
            )
            folders.forEach { folder ->
                add(
                    QuickOrganizeDrawerEntry(
                        id = folder.id,
                        title = folder.name,
                        isCreate = false
                    )
                )
            }
        }
    }
    val collapsedHeight = QUICK_ORGANIZE_DRAWER_COLLAPSED_CONTENT_HEIGHT + navigationBarBottomPadding
    val expandedHeight = (configuration.screenHeightDp.dp * QUICK_ORGANIZE_DRAWER_EXPANDED_HEIGHT_FRACTION)
        .coerceAtLeast(collapsedHeight)
    val currentHeight by animateDpAsState(
        targetValue = if (expanded) expandedHeight else collapsedHeight,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "quick_organize_drawer_height"
    )
    val drawerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "quick_organize_drawer_alpha"
    )
    val drawerOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 18.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "quick_organize_drawer_offset"
    )
    val glassBackdrop = LocalLiquidGlassBackdrop.current
    val drawerShape = RoundedCornerShape(
        topStart = QUICK_ORGANIZE_DRAWER_TOP_RADIUS,
        topEnd = QUICK_ORGANIZE_DRAWER_TOP_RADIUS
    )
    val drawerSurfaceBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.74f),
                Color.White.copy(alpha = 0.66f),
                Color.White.copy(alpha = 0.78f)
            )
        )
    }
    val drawerGlassTint = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }
    var drawerDragAmount by remember(expanded) { mutableStateOf(0f) }
    val collapseDrawer = {
        drawerDragAmount = 0f
        expanded = false
    }

    BackHandler(enabled = visible && interactive && expanded) {
        collapseDrawer()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .blur(18.dp * (1f - safeEntranceProgress))
            .graphicsLayer {
                alpha = drawerAlpha * safeEntranceProgress
                translationY = drawerOffsetY.toPx() + with(density) { (28.dp * (1f - safeEntranceProgress)).toPx() }
            }
    ) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        collapseDrawer()
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(currentHeight)
                .pointerInput(expanded, visible, interactive) {
                    if (expanded || !visible || !interactive) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { drawerDragAmount = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            drawerDragAmount += dragAmount
                        },
                        onDragCancel = { drawerDragAmount = 0f },
                        onDragEnd = {
                            if (drawerDragAmount <= -14f) {
                                expanded = true
                            }
                            drawerDragAmount = 0f
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(drawerShape)
                    .then(
                        if (glassBackdrop != null) {
                            Modifier.liquidGlassPanel(
                                backdrop = glassBackdrop,
                                shape = drawerShape,
                                surfaceColor = if (isDark) Color(0xFF121212).copy(alpha = 0.62f) else Color(0xFFFAFAFA).copy(alpha = 0.62f)
                            )
                        } else {
                            Modifier
                                .background(drawerSurfaceBrush)
                                .advancedTextureCard(
                                    shape = drawerShape,
                                    tint = drawerGlassTint,
                                    borderWidth = 0.8.dp
                                )
                                .then(
                                    if (!advancedTextureEnabled) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = if (isDark) {
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                                            } else {
                                                Color.Black.copy(alpha = 0.06f)
                                            },
                                            shape = drawerShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(QUICK_ORGANIZE_DRAWER_HEADER_HEIGHT)
                        .pointerInput(expanded, visible, interactive) {
                            if (!visible || !interactive) return@pointerInput
                            detectVerticalDragGestures(
                                onDragStart = { drawerDragAmount = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    drawerDragAmount += dragAmount
                                },
                                onDragCancel = { drawerDragAmount = 0f },
                                onDragEnd = {
                                    when {
                                        drawerDragAmount <= -14f -> expanded = true
                                        drawerDragAmount >= 14f -> collapseDrawer()
                                    }
                                    drawerDragAmount = 0f
                                }
                            )
                        }
                        .clickable(
                            enabled = visible && interactive,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { expanded = !expanded }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isDark) {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
                                } else {
                                    Color.Black.copy(alpha = 0.14f)
                                }
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = QUICK_ORGANIZE_DRAWER_HEADER_HEIGHT)
                        .height(currentHeight - QUICK_ORGANIZE_DRAWER_HEADER_HEIGHT)
                        .clipToBounds()
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState, enabled = expanded)
                            .padding(
                                start = QUICK_ORGANIZE_DRAWER_HORIZONTAL_PADDING,
                                end = QUICK_ORGANIZE_DRAWER_HORIZONTAL_PADDING,
                                top = QUICK_ORGANIZE_DRAWER_TOP_PADDING,
                                bottom = QUICK_ORGANIZE_DRAWER_BOTTOM_PADDING + visualBottomPadding
                            ),
                        horizontalArrangement = Arrangement.spacedBy(QUICK_ORGANIZE_DRAWER_CHIP_SPACING),
                        verticalArrangement = Arrangement.spacedBy(QUICK_ORGANIZE_DRAWER_CHIP_SPACING)
                    ) {
                        entries.forEach { entry ->
                            QuickOrganizeDrawerItem(
                                entry = entry,
                                isAcknowledged = acknowledgedFolderId == entry.id,
                                interactive = interactive,
                                onClick = {
                                    if (!interactive) return@QuickOrganizeDrawerItem
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (entry.isCreate) {
                                        onCreateFolderClick()
                                    } else {
                                        acknowledgedFolderId = entry.id
                                        folders.find { it.id == entry.id }?.let { folder ->
                                            onFolderClick(folder)
                                            if (expanded) {
                                                collapseDrawer()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (!expanded) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(collapsedBlockedHeight)
                                .clickable(
                                    enabled = visible && interactive,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickOrganizeDrawerItem(
    entry: QuickOrganizeDrawerEntry,
    isAcknowledged: Boolean,
    interactive: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val capsuleColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.60f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val contentTint = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
    } else {
        Color.Black.copy(alpha = 0.66f)
    }
    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = Modifier
            .clickable(
                enabled = interactive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .bounceClick()
            .height(QUICK_ORGANIZE_DRAWER_CHIP_HEIGHT)
            .clip(shape)
            .background(capsuleColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isAcknowledged -> {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            entry.isCreate -> {
                Icon(
                    imageVector = Icons.Outlined.CreateNewFolder,
                    contentDescription = "New Folder",
                    tint = contentTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            else -> {
                Text(
                    text = entry.title,
                    color = contentTint,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
