package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.text.style.TextOverflow
import coil.decode.VideoFrameDecoder
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.itemsIndexed
import java.util.UUID
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.util.Calendar
import java.util.Locale

data class FilterFolderData(
    val name: String,
    val count: Int,
    val totalCount: Int = count,
    val previews: List<MediaItem>
)

private const val OTHER_FOLDERS_NAME = "其他文件夹"
private const val SMALL_FOLDER_THRESHOLD = 15

data class FeaturedDayEntry(
    val dayKey: String,
    val count: Int
)

private const val FILTER_TYPE_ALL = "All"
private const val FILTER_TYPE_LIVE = "Live"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterView(
    currentFolder: String,
    currentType: String,
    currentMonth: String = "All",
    currentDay: String = "All",
    typeCounts: Map<String, Int> = emptyMap(),
    folderList: List<FilterFolderData>,
    hiddenFolders: Set<String> = emptySet(),
    hiddenFoldersAffectMoveTargets: Boolean = true,
    compactSmallFoldersEnabled: Boolean = false,
    prioritizedFolders: Set<String> = emptySet(), // New
    isRenameRestricted: (String) -> Boolean = { false },
    onApply: (String, String, String, String) -> Unit,
    onBack: () -> Unit,
    featuredDays: List<FeaturedDayEntry> = emptyList(),
    onQuickApplyDay: (String, String) -> Unit = { _, _ -> },
    onSelectMonth: () -> Unit,
    onSelectDay: () -> Unit = {},
    onClearMonth: () -> Unit = {},
    onClearDay: () -> Unit = {},
    onHideFolder: (String) -> Unit = {},
    onUnhideFolder: (String) -> Unit = {},
    onHiddenFoldersAffectMoveTargetsChange: (Boolean) -> Unit = {},
    onCompactSmallFoldersEnabledChange: (Boolean) -> Unit = {},
    onPrioritizeFolder: (String) -> Unit = {}, // New
    onUnprioritizeFolder: (String) -> Unit = {}, // New
    onRenameFolder: (String, String) -> Unit = { _, _ -> }
) {
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(currentLanguage)
    val isChinese = currentLanguage == AppLanguage.SimplifiedChinese || currentLanguage == AppLanguage.TraditionalChinese
    val normalizedCurrentType = if (currentType == FILTER_TYPE_LIVE) FILTER_TYPE_ALL else currentType
    var selectedFolder by remember { mutableStateOf(currentFolder) }
    var selectedType by remember { mutableStateOf(normalizedCurrentType) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(currentDay) }
    var showHiddenList by remember { mutableStateOf(false) }
    var showHiddenSettings by remember { mutableStateOf(false) }
    var showOtherFoldersList by remember { mutableStateOf(false) }
    var folderToHide by remember { mutableStateOf<String?>(null) }
    var folderToUnhide by remember { mutableStateOf<String?>(null) }
    var folderWithOptions by remember { mutableStateOf<String?>(null) } // New: for long-press menu
    var folderToRename by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var localFolderList by remember { mutableStateOf(folderList) }
    var localHiddenFolders by remember { mutableStateOf(hiddenFolders) }
    var localHiddenFoldersAffectMoveTargets by remember { mutableStateOf(hiddenFoldersAffectMoveTargets) }
    var localCompactSmallFoldersEnabled by remember { mutableStateOf(compactSmallFoldersEnabled) }
    var localPrioritizedFolders by remember { mutableStateOf(prioritizedFolders) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentFolder) {
        selectedFolder = currentFolder
    }
    LaunchedEffect(normalizedCurrentType) {
        selectedType = normalizedCurrentType
    }

    LaunchedEffect(currentType) {
        selectedType = currentType
    }

    LaunchedEffect(currentMonth) {
        selectedMonth = currentMonth
    }
    
    LaunchedEffect(currentDay) {
        selectedDay = currentDay
    }

    LaunchedEffect(folderList) {
        localFolderList = folderList
    }

    LaunchedEffect(hiddenFolders) {
        localHiddenFolders = hiddenFolders
    }
    LaunchedEffect(hiddenFoldersAffectMoveTargets) {
        localHiddenFoldersAffectMoveTargets = hiddenFoldersAffectMoveTargets
    }
    LaunchedEffect(compactSmallFoldersEnabled) {
        localCompactSmallFoldersEnabled = compactSmallFoldersEnabled
    }

    LaunchedEffect(prioritizedFolders) {
        localPrioritizedFolders = prioritizedFolders
    }

    LaunchedEffect(typeCounts) {
        val count = typeCounts[selectedType] ?: 0
        if (selectedType == FILTER_TYPE_LIVE) {
            selectedType = FILTER_TYPE_ALL
        } else if (typeCounts.isNotEmpty() && selectedType != FILTER_TYPE_ALL && count == 0) {
            selectedType = FILTER_TYPE_ALL
        }
    }
    
    // Page ID for animation reset
    val pageId = remember { UUID.randomUUID().toString() }

    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val featuredLastYearToday = remember(featuredDays) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        val key = "%04d-%02d-%02d".format(
            Locale.US,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        featuredDays.firstOrNull { it.dayKey == key }
    }
    val featuredRandomDay = remember(featuredDays) {
        if (featuredDays.isEmpty()) {
            null
        } else {
            val preferred = featuredDays.filter { it.count >= 10 }
            val pool = if (preferred.isNotEmpty()) preferred else featuredDays
            pool[Random.nextInt(pool.size)]
        }
    }

    val visibleFolders = remember(localFolderList, localHiddenFolders) {
        localFolderList.filter { !localHiddenFolders.contains(it.name) }
    }
    val hiddenFolderList = remember(localFolderList, localHiddenFolders) {
        localFolderList.filter { localHiddenFolders.contains(it.name) }
    }
    val allFolder = remember(visibleFolders) {
        visibleFolders.firstOrNull { it.name == "All" }
    }
    val regularVisibleFolders = remember(visibleFolders) {
        visibleFolders.filter { it.name != "All" }
    }
    val smallFolders = remember(regularVisibleFolders) {
        regularVisibleFolders.filter { it.totalCount < SMALL_FOLDER_THRESHOLD }
    }
    val normalFolders = remember(regularVisibleFolders) {
        regularVisibleFolders.filter { it.totalCount >= SMALL_FOLDER_THRESHOLD }
    }
    val otherFoldersData = remember(smallFolders) {
        if (smallFolders.isNotEmpty()) {
            val mergedPreviews = smallFolders.asSequence()
                .flatMap { it.previews.asSequence() }
                .distinctBy { it.id }
                .take(7)
                .toList()
            FilterFolderData(
                name = OTHER_FOLDERS_NAME,
                count = smallFolders.sumOf { it.count },
                totalCount = smallFolders.sumOf { it.totalCount },
                previews = mergedPreviews
            )
        } else {
            null
        }
    }
    val renderedFolders = remember(allFolder, localCompactSmallFoldersEnabled, otherFoldersData, normalFolders, regularVisibleFolders) {
        buildList {
            if (allFolder != null) add(allFolder)
            if (localCompactSmallFoldersEnabled && otherFoldersData != null) {
                add(otherFoldersData)
                addAll(normalFolders)
            } else {
                addAll(regularVisibleFolders)
            }
        }
    }

    // Intercept Back Press when Hidden List is shown
    BackHandler(enabled = showHiddenList || showHiddenSettings || showOtherFoldersList) {
        if (showHiddenSettings) {
            showHiddenSettings = false
        } else if (showOtherFoldersList) {
            showOtherFoldersList = false
        } else {
            showHiddenList = false
        }
    }

    if (folderWithOptions != null) {
        val isPrioritized = localPrioritizedFolders.contains(folderWithOptions)
        val canRename = folderWithOptions?.let { !isRenameRestricted(it) } == true
        AlertDialog(
            onDismissRequest = { folderWithOptions = null },
            title = { Text(folderWithOptions ?: "") },
            text = { Text(newPageStrings.folderOptionsTitle) },
            confirmButton = {}, // No confirm button, using list items
            dismissButton = {
                TextButton(onClick = { folderWithOptions = null }) {
                    Text(strings.cancel ?: "Cancel")
                }
            },
            textContentColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surface,
            icon = null
        )
        
        // Custom Dialog Content isn't easily supported with basic AlertDialog params for list items
        // So we use a custom implementation or standard Dialog if needed, but here let's just use basic layout
        // actually AlertDialog text parameter can contain Column
        
        AlertDialog(
            onDismissRequest = { folderWithOptions = null },
            title = { Text(folderWithOptions ?: "") },
            text = {
                Column {
                    // Hide Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                folderWithOptions?.let { folderToHide = it }
                                folderWithOptions = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(16.dp))
                        Text(newPageStrings.hideFolderAction, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    // Prioritize Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                folderWithOptions?.let { folder ->
                                    if (isPrioritized) {
                                        onUnprioritizeFolder(folder)
                                    } else {
                                        onPrioritizeFolder(folder)
                                    }
                                }
                                folderWithOptions = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flag Icon
                        Icon(
                            imageVector = if (isPrioritized) Icons.Outlined.Close else Icons.Outlined.Flag, // Use Flag or similar
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = if (isPrioritized) newPageStrings.unprioritizeFolderAction else newPageStrings.prioritizeFolderAction,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (canRename) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    folderWithOptions?.let { folder ->
                                        folderToRename = folder
                                        renameInput = folder
                                    }
                                    folderWithOptions = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(newPageStrings.renameFolderAction, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { folderWithOptions = null }) {
                    Text(strings.cancel ?: "Cancel")
                }
            }
        )
    }
    
    if (folderToRename != null) {
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text(newPageStrings.renameFolderTitle) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text(strings.folderName) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = folderToRename
                        val trimmed = renameInput.trim()
                        if (target != null) {
                            val existingNames = localFolderList.map { it.name }.toSet()
                            when {
                                trimmed.isEmpty() -> {
                                    scope.launch { snackbarHostState.showSnackbar(newPageStrings.renameFolderEmptyName) }
                                }
                                trimmed == target -> {
                                    scope.launch { snackbarHostState.showSnackbar(newPageStrings.renameFolderUnchanged) }
                                }
                                existingNames.contains(trimmed) -> {
                                    scope.launch { snackbarHostState.showSnackbar(newPageStrings.renameFolderExists) }
                                }
                                else -> {
                                    onRenameFolder(target, trimmed)
                                    localFolderList = localFolderList.map { folder ->
                                        if (folder.name == target) {
                                            folder.copy(name = trimmed)
                                        } else {
                                            folder
                                        }
                                    }
                                    if (selectedFolder == target) {
                                        selectedFolder = trimmed
                                    }
                                    if (localHiddenFolders.contains(target)) {
                                        localHiddenFolders = (localHiddenFolders - target) + trimmed
                                    }
                                    if (localPrioritizedFolders.contains(target)) {
                                        localPrioritizedFolders = (localPrioritizedFolders - target) + trimmed
                                    }
                                    renameInput = trimmed
                                    scope.launch { snackbarHostState.showSnackbar(newPageStrings.renameFolderSuccess(trimmed)) }
                                }
                            }
                        }
                        folderToRename = null
                    }
                ) {
                    Text(strings.confirm ?: "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text(strings.cancel ?: "Cancel")
                }
            }
        )
    }

    if (folderToHide != null) {
        AlertDialog(
            onDismissRequest = { folderToHide = null },
            title = { Text(newPageStrings.hideFolderTitle) },
            text = { Text(newPageStrings.hideFolderBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        folderToHide?.let { onHideFolder(it) }
                        folderToHide = null
                    }
                ) {
                    Text(strings.confirm ?: "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToHide = null }) {
                    Text(strings.cancel ?: "Cancel")
                }
            }
        )
    }

    if (folderToUnhide != null) {
        AlertDialog(
            onDismissRequest = { folderToUnhide = null },
            title = { Text(newPageStrings.unhideFolderTitle) },
            text = { Text(newPageStrings.unhideFolderBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        folderToUnhide?.let { onUnhideFolder(it) }
                        folderToUnhide = null
                    }
                ) {
                    Text(strings.confirm ?: "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToUnhide = null }) {
                    Text(strings.cancel ?: "Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            UnifiedSubpageTopBar(
                title = strings.filterTitle,
                onBack = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }
            ) {
                TextButton(
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onApply(selectedFolder, selectedType, selectedMonth, selectedDay)
                    }
                ) {
                    Text(
                        text = strings.apply,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
            // Featured Section
            item {
                Text(
                    text = newPageStrings.filterFeaturedTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .then(staggeredRevealModifier(0, key = pageId + "featured_title"))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .then(staggeredRevealModifier(1, key = pageId + "featured_row")),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val quickApply: (FeaturedDayEntry?) -> Unit = { target ->
                        if (target != null) {
                            val month = target.dayKey.take(7)
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedFolder = "All"
                            selectedType = "All"
                            selectedMonth = month
                            selectedDay = target.dayKey
                            onQuickApplyDay(month, target.dayKey)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = featuredLastYearToday != null) { quickApply(featuredLastYearToday) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = newPageStrings.filterLastYearToday,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = featuredLastYearToday?.let { newPageStrings.filterPhotoCountLabel(it.count) } ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = featuredRandomDay != null) { quickApply(featuredRandomDay) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = newPageStrings.filterRandomDay,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = featuredRandomDay?.let { newPageStrings.filterPhotoCountLabel(it.count) } ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Month Section
            item {
                Text(
                    text = strings.filterMonth ?: "Month",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .then(staggeredRevealModifier(0, key = pageId + "month_title"))
                )

                // Month Selector Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(staggeredRevealModifier(1, key = pageId + "month_card"))
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .bounceClick(onClick = { 
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelectMonth() 
                        })
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedMonth == "All") strings.filterAll else selectedMonth,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (selectedMonth == "All") {
                                Text(
                                    text = strings.selectMonth ?: "Select a specific month",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                        if (selectedMonth != "All") {
                            IconButton(
                                onClick = { 
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedMonth = "All"
                                    onClearMonth()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear Month",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Select Month",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Day Section (Only if Month is selected)
            if (selectedMonth != "All") {
                item {
                    Text(
                        text = strings.filterDay,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .then(staggeredRevealModifier(1, key = pageId + "day_title"))
                    )

                    // Day Selector Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(staggeredRevealModifier(2, key = pageId + "day_card"))
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .bounceClick(onClick = { 
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectDay() 
                            })
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedDay == "All") {
                                        strings.filterAll
                                    } else {
                                        val parts = selectedDay.split("-")
                                        if (isChinese && parts.size == 3) {
                                            val month = parts[1].toIntOrNull()
                                            val day = parts[2].toIntOrNull()
                                            if (month != null && day != null) newPageStrings.monthDayLabel(month, day) else selectedDay
                                        } else {
                                            selectedDay
                                        }
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (selectedDay == "All") {
                                    Text(
                                        text = strings.selectDay,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            if (selectedDay != "All") {
                                IconButton(
                                    onClick = { 
                                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedDay = "All"
                                        onClearDay()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear Day",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Select Day",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Media Type Section
            item {
                Text(
                    text = strings.filterType,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .then(staggeredRevealModifier(2, key = pageId + "type_title"))
                )
                
                val types = listOf(
                    FILTER_TYPE_ALL to strings.filterAll,
                    "Image" to strings.filterImage,
                    "Video" to strings.filterVideo
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(staggeredRevealModifier(3, key = pageId + "type_row"))
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { (key, label) ->
                        val isSelected = selectedType == key
                        val count = typeCounts[key] ?: if (typeCounts.isNotEmpty() && key != "All") 0 else 1
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (count > 0 || key == "All") {
                                    selectedType = key
                                }
                            },
                            enabled = count > 0 || key == "All",
                            label = { Text(label, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                                selectedLabelColor = if (isDark) Color.Black else Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onBackground
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Folder Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .then(staggeredRevealModifier(4, key = pageId + "folder_title")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.filterFolder,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (smallFolders.isNotEmpty()) {
                        FilterChip(
                            selected = localCompactSmallFoldersEnabled,
                            onClick = {
                                localCompactSmallFoldersEnabled = !localCompactSmallFoldersEnabled
                                onCompactSmallFoldersEnabledChange(localCompactSmallFoldersEnabled)
                            },
                            label = {
                                Text(newPageStrings.filterSmartCollapse)
                            }
                        )
                    }
                }
            }
            
            itemsIndexed(
                items = renderedFolders,
                key = { _, folder -> folder.name }
            ) { index, folder ->
                // Optimize animation: Only stagger first few items, then show quickly
                val animIndex = if (index < 6) 5 + index else 1
                val isOtherEntry = folder.name == OTHER_FOLDERS_NAME
                val isOtherSelected = isOtherEntry && smallFolders.any { it.name == selectedFolder }
                
                FilterFolderItem(
                    data = folder,
                    isSelected = selectedFolder == folder.name || isOtherSelected,
                    isPrioritized = localPrioritizedFolders.contains(folder.name),
                    onClick = { 
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isOtherEntry) {
                            showOtherFoldersList = true
                        } else {
                            selectedFolder = folder.name
                        }
                    },
                    onLongClick = {
                        if (!isOtherEntry) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            folderWithOptions = folder.name
                        }
                    },
                    modifier = staggeredRevealModifier(
                        index = animIndex,
                        key = pageId + "folder_${folder.name}"
                    )
                )
            }

            // Hidden Folders Entry
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showHiddenList = true
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(16.dp))
                    Text(newPageStrings.hiddenFoldersTitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    Text("${hiddenFolderList.size}", color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
                }
            }
        }

        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    PageAnimatedVisibility(
        visible = showHiddenList,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        HiddenFoldersView(
            hiddenList = hiddenFolderList,
            onBack = { showHiddenList = false },
            onOpenSettings = { showHiddenSettings = true },
            onUnhide = { folderToUnhide = it },
            onSelect = { folder -> folderToUnhide = folder }
        )
    }

    PageAnimatedVisibility(
        visible = showHiddenSettings,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        HiddenFoldersMoveTargetSettingsView(
            enabled = localHiddenFoldersAffectMoveTargets,
            onBack = { showHiddenSettings = false },
            onEnabledChange = { enabled ->
                localHiddenFoldersAffectMoveTargets = enabled
                onHiddenFoldersAffectMoveTargetsChange(enabled)
            }
        )
    }

    PageAnimatedVisibility(
        visible = showOtherFoldersList,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        OtherFoldersView(
            folders = smallFolders,
            selectedFolder = selectedFolder,
            onBack = { showOtherFoldersList = false },
            onSelect = { folderName ->
                selectedFolder = folderName
                showOtherFoldersList = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterFolderItem(
    data: FilterFolderData,
    isSelected: Boolean,
    isPrioritized: Boolean = false, // New
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                }
            )
            .bounceClick(onClick = null)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp)
    ) {
        // Title Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${data.name} (${data.count}/${data.totalCount})",
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (isPrioritized) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = "Prioritized",
                    tint = Color(0xFFFFA000), // Amber/Orange
                    modifier = Modifier.size(16.dp)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Preview Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalSlots = 7
            val previewsToShow = minOf(data.previews.size, 6)
            val hasOverflow = data.count > 6
            
            for (i in 0 until previewsToShow) {
                 Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp))) {
                     AsyncImage(
                         model = ImageRequest.Builder(LocalContext.current)
                             .data(data.previews[i].uri)
                             .decoderFactory(VideoFrameDecoder.Factory())
                             .crossfade(true)
                             .size(200)
                             .build(),
                         contentDescription = null,
                         contentScale = ContentScale.Crop,
                         modifier = Modifier.fillMaxSize()
                     )
                 }
            }
            
            if (hasOverflow) {
                 Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp))) {
                     if (data.previews.size > 6) {
                         AsyncImage(
                             model = ImageRequest.Builder(LocalContext.current)
                                 .data(data.previews[6].uri)
                                 .decoderFactory(VideoFrameDecoder.Factory())
                                 .crossfade(true)
                                 .size(200)
                                 .build(),
                             contentDescription = null,
                             contentScale = ContentScale.Crop,
                             modifier = Modifier.fillMaxSize()
                         )
                     }
                     // Overlay
                     Box(
                         modifier = Modifier
                             .fillMaxSize()
                            .background(
                                if (isDark) MaterialTheme.colorScheme.background.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                            ),
                         contentAlignment = Alignment.Center
                     ) {
                         val remaining = data.count - 6
                         val fontSize = if (remaining.toString().length > 3) 10.sp else 12.sp
                         Text(
                             text = "+$remaining",
                            color = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                             fontWeight = FontWeight.Bold,
                             fontSize = fontSize,
                             maxLines = 1,
                             overflow = TextOverflow.Visible,
                             softWrap = false
                         )
                     }
                 }
            }
            
            val usedSlots = previewsToShow + (if (hasOverflow) 1 else 0)
            for (i in usedSlots until totalSlots) {
                 Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
            }
        }
    }
}

@Composable
fun HiddenFoldersView(
    hiddenList: List<FilterFolderData>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onUnhide: (String) -> Unit,
    onSelect: (String) -> Unit
) {
    val currentLanguage = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(currentLanguage)
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = newPageStrings.hiddenFoldersTitle,
            onBack = {
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBack()
            }
        ) {
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Hidden folder settings",
                onClick = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSettings()
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            if (hiddenList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                         Text(newPageStrings.hiddenFoldersEmpty, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
                    }
                }
            }
            
            itemsIndexed(hiddenList) { index, folder ->
                // Reuse FolderItem
                val isSelected = false
                FilterFolderItem(
                    data = folder,
                    isSelected = isSelected,
                    onClick = { 
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(folder.name)
                    },
                    onLongClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUnhide(folder.name)
                    }
                )
            }
        }
    }
}

@Composable
fun HiddenFoldersMoveTargetSettingsView(
    enabled: Boolean,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val currentLanguage = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(currentLanguage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = newPageStrings.hiddenFoldersSettingsTitle,
            onBack = {
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBack()
            }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = newPageStrings.hiddenFoldersApplyToMoveTargets,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEnabledChange(it)
                        }
                    )
                }
                Text(
                    text = if (enabled) newPageStrings.hiddenFoldersApplyToMoveTargetsDescOn else newPageStrings.hiddenFoldersApplyToMoveTargetsDescOff,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun OtherFoldersView(
    folders: List<FilterFolderData>,
    selectedFolder: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val currentLanguage = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(currentLanguage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = newPageStrings.filterOtherFolders,
            onBack = {
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            if (folders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = newPageStrings.filterNoCollapsibleFolders,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                itemsIndexed(folders) { _, folder ->
                    FilterFolderItem(
                        data = folder,
                        isSelected = selectedFolder == folder.name,
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(folder.name)
                        }
                    )
                }
            }
        }
    }
}
