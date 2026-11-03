package com.voicelike.app.promptmanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.voicelike.app.ColorCardPromptDetailPage
import com.voicelike.app.ColorCardPromptEntry
import com.voicelike.app.ColorCardPromptFeedState
import com.voicelike.app.FluidTransitions
import com.voicelike.app.ModernColorCardPromptOnlyFeed
import com.voicelike.app.PageAnimatedVisibility
import com.voicelike.app.UnifiedSubpageTopBar
import com.voicelike.app.UnifiedSubpageTopBarIconButton
import com.voicelike.app.rememberColorCardPromptFeedState
import com.voicelike.app.copyColorCardText
import org.json.JSONObject

@Composable
fun PromptManagerPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember(context) { PromptManagerStore.get(context) }
    val prompts by store.prompts.collectAsState()
    val promptFeedState = rememberColorCardPromptFeedState()
    var selectedPrompt by remember { mutableStateOf<ManagedPrompt?>(null) }
    var selectedOfficialPrompt by remember { mutableStateOf<ColorCardPromptEntry?>(null) }
    var editingPrompt by remember { mutableStateOf<ManagedPrompt?>(null) }
    var creatingPrompt by rememberSaveable { mutableStateOf(false) }
    var displayedPrompt by remember { mutableStateOf<ManagedPrompt?>(null) }
    var displayedOfficialPrompt by remember { mutableStateOf<ColorCardPromptEntry?>(null) }

    androidx.compose.runtime.LaunchedEffect(selectedPrompt?.id) {
        selectedPrompt?.let { displayedPrompt = it }
    }
    androidx.compose.runtime.LaunchedEffect(selectedOfficialPrompt?.id) {
        selectedOfficialPrompt?.let { displayedOfficialPrompt = it }
    }

    BackHandler(enabled = selectedPrompt != null || selectedOfficialPrompt != null || editingPrompt != null || creatingPrompt) {
        when {
            editingPrompt != null -> editingPrompt = null
            creatingPrompt -> creatingPrompt = false
            selectedOfficialPrompt != null -> selectedOfficialPrompt = null
            selectedPrompt != null -> selectedPrompt = null
        }
    }

    when {
        editingPrompt != null || creatingPrompt -> {
            PromptEditPage(
                initialPrompt = editingPrompt,
                existingPlatforms = prompts.map { it.platform }.filter { it.isNotBlank() }.distinct(),
                onBack = {
                    editingPrompt = null
                    creatingPrompt = false
                },
                onSave = { draft ->
                    val saved = store.upsert(
                        existingId = editingPrompt?.id,
                        title = "",
                        imageUriString = draft.imageUriString,
                        platform = draft.platform,
                        category = "",
                        summary = "",
                        prompt = draft.prompt
                    )
                    editingPrompt = null
                    creatingPrompt = false
                    selectedPrompt = saved
                },
                modifier = modifier
            )
        }
        else -> {
            Box(modifier = modifier.fillMaxSize()) {
                PromptManagerHomePage(
                    prompts = prompts,
                    promptFeedState = promptFeedState,
                    onBack = onBack,
                    onAdd = { creatingPrompt = true },
                    onPromptClick = { selectedPrompt = it },
                    onOfficialPromptClick = { selectedOfficialPrompt = it },
                    modifier = Modifier.fillMaxSize()
                )
                PageAnimatedVisibility(
                    visible = selectedPrompt != null,
                    enter = FluidTransitions.SheetEnter,
                    exit = FluidTransitions.SheetExit,
                    restartContentOnOpen = false
                ) {
                    displayedPrompt?.let { prompt ->
                        PromptManagedDetailPage(
                            prompt = prompt,
                            onBack = { selectedPrompt = null },
                            onEdit = { editingPrompt = selectedPrompt },
                            onDelete = {
                                store.delete(prompt.id)
                                selectedPrompt = null
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                PageAnimatedVisibility(
                    visible = selectedOfficialPrompt != null,
                    enter = FluidTransitions.SheetEnter,
                    exit = FluidTransitions.SheetExit,
                    restartContentOnOpen = false
                ) {
                    displayedOfficialPrompt?.let { promptEntry ->
                        ColorCardPromptDetailPage(
                            entry = promptEntry,
                            onBack = { selectedOfficialPrompt = null },
                            initialPreviewBitmap = promptFeedState.previewBitmaps[promptEntry.id],
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromptManagerHomePage(
    prompts: List<ManagedPrompt>,
    promptFeedState: ColorCardPromptFeedState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onPromptClick: (ManagedPrompt) -> Unit,
    onOfficialPromptClick: (ColorCardPromptEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(PromptManagerTab.Inspiration) }
    val gridState = rememberLazyStaggeredGridState()
    val inspirationListState = rememberLazyListState()
    val filteredPrompts = remember(prompts, query) {
        val keyword = query.trim()
        if (keyword.isBlank()) {
            prompts
        } else {
            prompts.filter { item ->
                listOf(item.displayTitle, item.platform, promptManagerDisplayModelLabel(item.platform), item.summary, item.prompt)
                    .any { it.contains(keyword, ignoreCase = true) }
            }
        }
    }
    val filteredPromptFeedState = remember(promptFeedState, query) {
        filterPromptFeedState(promptFeedState, query)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = "提示词",
            onBack = onBack,
            trailing = {
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "添加提示词",
                    onClick = onAdd
                )
            }
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            },
            placeholder = { Text("搜索模型或提示词") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        PromptManagerTabBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
        when (selectedTab) {
            PromptManagerTab.Mine -> {
                if (filteredPrompts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (prompts.isEmpty()) "还没有添加提示词" else "没有找到匹配的提示词",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 12.dp,
                        contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 40.dp)
                    ) {
                        items(filteredPrompts, key = { it.id }) { item ->
                            PromptManagerGridCard(
                                prompt = item,
                                onClick = { onPromptClick(item) }
                            )
                        }
                    }
                }
            }
            PromptManagerTab.Inspiration -> {
                ModernColorCardPromptOnlyFeed(
                    listState = inspirationListState,
                    state = filteredPromptFeedState,
                    onPromptSelected = onOfficialPromptClick,
                    modifier = Modifier.fillMaxSize()
                )
                if (!promptFeedState.isLoading && filteredPromptFeedState.series.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "没有找到匹配的提示词",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private enum class PromptManagerTab(val label: String) {
    Mine("我的"),
    Inspiration("灵感")
}

@Composable
private fun PromptManagerTabBar(
    selectedTab: PromptManagerTab,
    onTabSelected: (PromptManagerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PromptManagerTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            val interactionSource = remember(tab) { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onTabSelected(tab) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private fun filterPromptFeedState(
    state: ColorCardPromptFeedState,
    query: String
): ColorCardPromptFeedState {
    val keyword = query.trim()
    if (keyword.isBlank()) return state
    return state.copy(
        series = state.series.mapNotNull { series ->
            val entries = series.entries.filter { entry ->
                listOf(
                    entry.zhTitle,
                    entry.enTitle,
                    entry.zhSummary,
                    entry.enSummary,
                    entry.platform,
                    entry.prompt,
                    entry.promptTemplate
                ).any { it.contains(keyword, ignoreCase = true) } ||
                    entry.tags.any { it.contains(keyword, ignoreCase = true) }
            }
            if (entries.isEmpty()) null else series.copy(entries = entries)
        }
    )
}

private fun promptManagerDisplayModelLabel(platform: String): String = when (platform.trim().lowercase()) {
    "seed" -> "豆包/即梦"
    "yb" -> "元宝"
    "qwen-image" -> "千问"
    else -> platform.trim()
}

@Composable
private fun PromptManagerGridCard(
    prompt: ManagedPrompt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (prompt.imageUriString.isNullOrBlank()) 1.18f else 0.78f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!prompt.imageUriString.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(prompt.imageUriString))
                        .crossfade(true)
                        .build(),
                    contentDescription = prompt.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (prompt.platform.isNotBlank()) {
                    PromptMetaPill(text = promptManagerDisplayModelLabel(prompt.platform))
                }
            }
            Text(
                text = prompt.summary.ifBlank { prompt.prompt.replace('\n', ' ') },
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class PromptDraft(
    val imageUriString: String?,
    val platform: String,
    val prompt: String
)

@Composable
private fun PromptEditPage(
    initialPrompt: ManagedPrompt?,
    existingPlatforms: List<String>,
    onBack: () -> Unit,
    onSave: (PromptDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val officialModels = remember(context) { loadPromptManagerOfficialModels(context) }
    var imageUriString by rememberSaveable(initialPrompt?.id) { mutableStateOf(initialPrompt?.imageUriString) }
    var platform by rememberSaveable(initialPrompt?.id) {
        mutableStateOf(initialPrompt?.platform ?: officialModels.firstOrNull() ?: "Seed")
    }
    var useCustomPlatform by rememberSaveable(initialPrompt?.id) { mutableStateOf(false) }
    var customPlatformInput by rememberSaveable(initialPrompt?.id) { mutableStateOf("") }
    var prompt by rememberSaveable(initialPrompt?.id) { mutableStateOf(initialPrompt?.prompt.orEmpty()) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        imageUriString = uri.toString()
    }
    val platforms = remember(officialModels, existingPlatforms, platform) {
        (officialModels + existingPlatforms + platform)
            .filter { it.isNotBlank() }
            .distinct()
    }
    val normalizedBuiltInPlatforms = remember(platforms) {
        platforms.map { it.lowercase() }.toSet()
    }
    val selectedPlatform = if (useCustomPlatform) customPlatformInput.trim() else platform.trim()
    val canSave = prompt.trim().isNotBlank() && selectedPlatform.isNotBlank()

    androidx.compose.runtime.LaunchedEffect(initialPrompt?.id, initialPrompt?.platform, platforms) {
        val initialPlatform = initialPrompt?.platform.orEmpty().trim()
        when {
            initialPlatform.isBlank() -> {
                useCustomPlatform = false
                customPlatformInput = ""
                platform = platforms.firstOrNull().orEmpty()
            }
            initialPlatform.lowercase() in normalizedBuiltInPlatforms -> {
                useCustomPlatform = false
                customPlatformInput = ""
                platform = initialPlatform
            }
            else -> {
                useCustomPlatform = true
                customPlatformInput = initialPlatform
                platform = platforms.firstOrNull().orEmpty()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = if (initialPrompt == null) "添加提示词" else "编辑提示词",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PromptImagePicker(
                    imageUriString = imageUriString,
                    onPick = { imagePicker.launch(arrayOf("image/*")) },
                    onClear = { imageUriString = null }
                )
            }
            item {
                PromptScrollableChipSection(title = "生成图片模型") {
                    platforms.forEach { option ->
                        FilterChip(
                            selected = !useCustomPlatform && platform == option,
                            onClick = {
                                useCustomPlatform = false
                                platform = option
                            },
                            label = { Text(promptManagerDisplayModelLabel(option)) },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                    FilterChip(
                        selected = useCustomPlatform,
                        onClick = { useCustomPlatform = true },
                        label = { Text("自定义") },
                        modifier = Modifier.height(42.dp)
                    )
                }
            }
            if (useCustomPlatform) {
                item {
                    OutlinedTextField(
                        value = customPlatformInput,
                        onValueChange = { customPlatformInput = it },
                        label = { Text("自定义模型名称") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("提示词内容") },
                    minLines = 8,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = {
                        if (!canSave) {
                            Toast.makeText(context, "请先填写模型和提示词内容", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSave(
                            PromptDraft(
                                imageUriString = imageUriString,
                                platform = selectedPlatform,
                                prompt = prompt
                            )
                        )
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun PromptImagePicker(
    imageUriString: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUriString.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(imageUriString),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("上传样图", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (!imageUriString.isNullOrBlank()) {
            TextButton(onClick = onClear) {
                Text("移除样图")
            }
        }
    }
}

@Composable
private fun PromptScrollableChipSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
private fun PromptManagedDetailPage(
    prompt: ManagedPrompt,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除提示词") },
            text = { Text("删除后将从提示词中移除，确认继续？") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = "",
            onBack = onBack,
            trailing = {
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "复制提示词",
                    onClick = {
                        copyColorCardText(
                            context = context,
                            label = "提示词",
                            text = prompt.prompt,
                            successMessage = "已复制"
                        )
                    }
                )
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "编辑",
                    onClick = onEdit
                )
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除",
                    onClick = { showDeleteConfirm = true }
                )
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!prompt.imageUriString.isNullOrBlank()) {
                item {
                    val imageShape = RoundedCornerShape(26.dp)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.58f)
                                .aspectRatio(112f / 148f)
                                .clip(imageShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = Uri.parse(prompt.imageUriString),
                                contentDescription = prompt.displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            item {
                if (prompt.summary.isNotBlank()) {
                    Text(
                        text = prompt.summary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (prompt.platform.isNotBlank()) {
                        PromptDetailMetaChip(promptManagerDisplayModelLabel(prompt.platform))
                    }
                }
            }
            item {
                PromptDetailCopyButton(
                    label = "复制提示词",
                    onClick = {
                        copyColorCardText(
                            context = context,
                            label = "提示词",
                            text = prompt.prompt,
                            successMessage = "已复制"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    text = "提示词正文",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = prompt.prompt,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptDetailCopyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(width = 8.dp, height = 0.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PromptDetailMetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PromptMetaPill(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSystemInDarkTheme()) 0.64f else 0.78f))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    )
}
private fun loadPromptManagerOfficialModels(context: Context): List<String> {
    return runCatching {
        context.assets.open("color_card_prompt/prompts.json").bufferedReader().use { reader ->
            val root = JSONObject(reader.readText())
            val platforms = linkedSetOf<String>()
            listOf(root.optJSONArray("entries"), root.optJSONArray("draft_entries")).forEach { entries ->
                for (index in 0 until (entries?.length() ?: 0)) {
                    val platform = entries?.optJSONObject(index)?.optString("platform").orEmpty().trim()
                    if (platform.isNotBlank()) platforms += platform
                }
            }
            platforms.toList().ifEmpty { listOf("Seed", "YB", "Qwen-Image", "GPT-Image", "General") }
        }
    }.getOrElse {
        listOf("Seed", "YB", "Qwen-Image", "GPT-Image", "General")
    }
}

