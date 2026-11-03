package com.voicelike.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val COLOR_CARD_PROMPT_GENERATOR_MANIFEST_ASSET = "color_card_prompt/prompts.json"
private const val COLOR_CARD_PROMPT_GENERATOR_MAX_STYLE_TAGS = 12
private const val COLOR_CARD_PROMPT_GENERATOR_MAX_CONDITION_TAGS = 12
private const val COLOR_CARD_PROMPT_GENERATOR_MAX_MATCHED_SOURCES = 4

private enum class ColorCardPromptGeneratorEntrySource {
    Live,
    Draft
}

private enum class ColorCardPromptGeneratorPersonKind {
    Single,
    Group,
    NoPeople,
    Pet
}

private data class ColorCardPromptGeneratorCategory(
    val key: String,
    val order: Int,
    val zhTitle: String,
    val enTitle: String
) {
    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhTitle.ifBlank { enTitle.ifBlank { key } }
        else -> enTitle.ifBlank { zhTitle.ifBlank { key } }
    }
}

private data class ColorCardPromptGeneratorEntry(
    val id: String,
    val categoryKey: String,
    val zhTitle: String,
    val enTitle: String,
    val zhSummary: String,
    val enSummary: String,
    val tags: List<String>,
    val prompt: String,
    val source: ColorCardPromptGeneratorEntrySource
) {
    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhTitle.ifBlank { enTitle.ifBlank { id } }
        else -> enTitle.ifBlank { zhTitle.ifBlank { id } }
    }

    fun summary(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhSummary.ifBlank { enSummary }
        else -> enSummary.ifBlank { zhSummary }
    }

    fun searchText(): String = buildString {
        append(id.lowercase())
        append(' ')
        append(categoryKey.lowercase())
        append(' ')
        append(zhTitle.lowercase())
        append(' ')
        append(enTitle.lowercase())
        append(' ')
        append(zhSummary.lowercase())
        append(' ')
        append(enSummary.lowercase())
        append(' ')
        append(tags.joinToString(" ").lowercase())
        append(' ')
        append(prompt.lowercase())
    }
}

private data class ColorCardPromptGeneratorCorpus(
    val categories: List<ColorCardPromptGeneratorCategory>,
    val entries: List<ColorCardPromptGeneratorEntry>
)

private data class ColorCardPromptGeneratorUiState(
    val corpus: ColorCardPromptGeneratorCorpus?,
    val isLoading: Boolean
)

private data class ColorCardPromptGeneratorChoice(
    val key: String,
    val zhLabel: String,
    val enLabel: String,
    val zhPrompt: String,
    val enPrompt: String,
    val categoryKeys: Set<String> = emptySet()
) {
    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhLabel
        else -> enLabel
    }
}

private val colorCardPromptGeneratorCorpusLock = Any()

@Volatile
private var cachedColorCardPromptGeneratorCorpus: ColorCardPromptGeneratorCorpus? = null

private val colorCardPromptGeneratorPurposeChoices = listOf(
    ColorCardPromptGeneratorChoice(
        key = "portrait",
        zhLabel = "人像写真",
        enLabel = "Portrait",
        zhPrompt = "以人物写真或社交平台成片为目标",
        enPrompt = "Build the prompt around a polished portrait image.",
        categoryKeys = setOf(
            "light_portrait",
            "social_portrait",
            "mood_portrait",
            "practical_remix",
            "creative_remix"
        )
    ),
    ColorCardPromptGeneratorChoice(
        key = "avatar",
        zhLabel = "头像 / 角色",
        enLabel = "Avatar / Character",
        zhPrompt = "适合头像、角色卡、贴纸板或转绘方向",
        enPrompt = "Make it work for avatars, character sheets, stickers, or redraw outputs.",
        categoryKeys = setOf(
            "avatar_redraw",
            "playful_character"
        )
    ),
    ColorCardPromptGeneratorChoice(
        key = "illustration",
        zhLabel = "场景插画",
        enLabel = "Scene Illustration",
        zhPrompt = "更偏插画场景、绘本页面或风格化画面",
        enPrompt = "Lean toward illustration scenes, storybook pages, or stylized artwork.",
        categoryKeys = setOf(
            "narrative_scene",
            "handmade_texture",
            "playful_character",
            "oriental_aesthetics",
            "style_experiment"
        )
    ),
    ColorCardPromptGeneratorChoice(
        key = "poster",
        zhLabel = "海报 / 平面",
        enLabel = "Poster / Graphic",
        zhPrompt = "以海报、平面设计或叙事版式为目标",
        enPrompt = "Shape it as a poster, graphic layout, or narrative design piece.",
        categoryKeys = setOf(
            "graphic_layout",
            "cinematic_narrative",
            "editorial_panel",
            "oriental_aesthetics"
        )
    ),
    ColorCardPromptGeneratorChoice(
        key = "product",
        zhLabel = "商品视觉",
        enLabel = "Product Visual",
        zhPrompt = "突出商品、材质、展示感或微缩陈列效果",
        enPrompt = "Focus on product presentation, display quality, materials, or miniature setups.",
        categoryKeys = setOf(
            "object_display",
            "spatial_build",
            "graphic_layout"
        )
    ),
    ColorCardPromptGeneratorChoice(
        key = "concept",
        zhLabel = "概念艺术",
        enLabel = "Concept Art",
        zhPrompt = "强化世界观、概念表达和更明显的风格化处理",
        enPrompt = "Push toward concept art, worldbuilding, and stronger stylization.",
        categoryKeys = setOf(
            "cinematic_narrative",
            "editorial_panel",
            "style_experiment",
            "oriental_aesthetics",
            "spatial_build"
        )
    )
)

private val colorCardPromptGeneratorPersonChoices = listOf(
    ColorCardPromptGeneratorChoice(
        key = "any",
        zhLabel = "不限",
        enLabel = "Any",
        zhPrompt = "",
        enPrompt = ""
    ),
    ColorCardPromptGeneratorChoice(
        key = "single",
        zhLabel = "单人主体",
        enLabel = "Single Person",
        zhPrompt = "画面以单一主体为核心，人物特征要清晰",
        enPrompt = "Keep one clear hero subject in frame."
    ),
    ColorCardPromptGeneratorChoice(
        key = "group",
        zhLabel = "多人 / 群像",
        enLabel = "Group",
        zhPrompt = "画面可包含多人关系或群像叙事，但层次要清楚",
        enPrompt = "Allow for multiple people or group storytelling while keeping hierarchy clear."
    ),
    ColorCardPromptGeneratorChoice(
        key = "none",
        zhLabel = "无人物",
        enLabel = "No People",
        zhPrompt = "不以人物为主，主体可以是物件、场景、建筑或抽象元素",
        enPrompt = "Do not center the image on people; focus on objects, scenes, architecture, or abstract forms."
    ),
    ColorCardPromptGeneratorChoice(
        key = "pet",
        zhLabel = "宠物 / 生物",
        enLabel = "Pet / Creature",
        zhPrompt = "主体可以是宠物、动物或拟人生物形象",
        enPrompt = "Use pets, animals, or creature-like characters as the main subject."
    )
)

private val colorCardPromptGeneratorConditionKeywords = listOf(
    "直闪",
    "自拍",
    "线稿",
    "半色调",
    "夜景",
    "阳光",
    "暗调",
    "自然光",
    "面部特写",
    "湿发",
    "留白",
    "花田",
    "浴室",
    "居家",
    "鱼缸",
    "水波",
    "白曝",
    "破壁",
    "蓝图",
    "建筑",
    "商务照",
    "简历",
    "游戏CG",
    "参考图",
    "教程"
)

private val colorCardPromptGeneratorDescriptorKeywords = listOf(
    "风格",
    "质感",
    "构图",
    "笔触",
    "纹理",
    "颗粒",
    "肌理",
    "留白",
    "色",
    "光",
    "氛围",
    "复古",
    "动画",
    "水墨",
    "胶片",
    "印刷",
    "手作",
    "极简",
    "对比",
    "光影",
    "剪影"
)

private val colorCardPromptGeneratorNoisePrefixes = listOf(
    "帮我生成",
    "生成图片",
    "生成写真",
    "请生成",
    "根据【",
    "根据[",
    "自动生成",
    "明确避免",
    "不要",
    "避免"
)

@Composable
private fun rememberColorCardPromptGeneratorUiState(): ColorCardPromptGeneratorUiState {
    val context = LocalContext.current
    val corpus by produceState<ColorCardPromptGeneratorCorpus?>(initialValue = null, key1 = context) {
        value = withContext(Dispatchers.IO) {
            loadColorCardPromptGeneratorCorpus(context.applicationContext)
        }
    }
    return ColorCardPromptGeneratorUiState(
        corpus = corpus,
        isLoading = corpus == null
    )
}

@Composable
internal fun ColorCardPromptGeneratorEntryBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedColorCardPromptGeneratorBarTitle(language),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = localizedColorCardPromptGeneratorBarSubtitle(language),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun ColorCardPromptGeneratorPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val globalCopy = remember(language) { getColorCardPageGlobalCopy(language) }
    val uiState = rememberColorCardPromptGeneratorUiState()
    val corpus = uiState.corpus
    if (uiState.isLoading || corpus == null) {
        ColorCardFilterFeedLoadingState(modifier = modifier)
        return
    }

    var selectedPurposeKey by rememberSaveable { mutableStateOf("") }
    var selectedPersonKey by rememberSaveable { mutableStateOf("any") }
    var selectedStyleTags by remember { mutableStateOf(emptyList<String>()) }
    var selectedConditionTags by remember { mutableStateOf(emptyList<String>()) }
    var subjectText by rememberSaveable { mutableStateOf("") }
    var sceneText by rememberSaveable { mutableStateOf("") }
    var extraText by rememberSaveable { mutableStateOf("") }
    var avoidText by rememberSaveable { mutableStateOf("") }

    val selectedPurpose = remember(selectedPurposeKey) {
        colorCardPromptGeneratorPurposeChoices.firstOrNull { it.key == selectedPurposeKey }
    }
    val selectedPerson = remember(selectedPersonKey) {
        colorCardPromptGeneratorPersonChoices.firstOrNull { it.key == selectedPersonKey }
            ?: colorCardPromptGeneratorPersonChoices.first()
    }
    val categoryByKey = remember(corpus.categories) { corpus.categories.associateBy { it.key } }
    val scopedEntries = remember(corpus.entries, selectedPurpose, selectedPersonKey) {
        buildColorCardPromptGeneratorScopedEntries(
            entries = corpus.entries,
            purpose = selectedPurpose,
            personKey = selectedPersonKey
        )
    }
    val styleTagSuggestions = remember(scopedEntries, selectedStyleTags, selectedConditionTags) {
        buildColorCardPromptGeneratorSuggestedTags(
            entries = scopedEntries,
            selectedTags = selectedStyleTags + selectedConditionTags,
            conditionLike = false,
            maxCount = COLOR_CARD_PROMPT_GENERATOR_MAX_STYLE_TAGS
        )
    }
    val conditionTagSuggestions = remember(scopedEntries, selectedStyleTags, selectedConditionTags) {
        buildColorCardPromptGeneratorSuggestedTags(
            entries = scopedEntries,
            selectedTags = selectedStyleTags + selectedConditionTags,
            conditionLike = true,
            maxCount = COLOR_CARD_PROMPT_GENERATOR_MAX_CONDITION_TAGS
        )
    }
    val selectedAllTags = remember(selectedStyleTags, selectedConditionTags) {
        (selectedStyleTags + selectedConditionTags).distinct()
    }
    val matchedEntries = remember(
        corpus.entries,
        selectedPurposeKey,
        selectedPersonKey,
        selectedStyleTags,
        selectedConditionTags,
        subjectText,
        sceneText,
        extraText,
        avoidText
    ) {
        buildColorCardPromptGeneratorMatchedEntries(
            entries = corpus.entries,
            purpose = selectedPurpose,
            personKey = selectedPersonKey,
            selectedTags = selectedAllTags,
            freeText = listOf(subjectText, sceneText, extraText, avoidText).joinToString(" ")
        )
    }
    val generatedPrompt = remember(
        language,
        selectedPurpose,
        selectedPerson,
        selectedStyleTags,
        selectedConditionTags,
        subjectText,
        sceneText,
        extraText,
        avoidText,
        matchedEntries,
        categoryByKey
    ) {
        buildColorCardPromptGeneratorResult(
            language = language,
            purpose = selectedPurpose,
            person = selectedPerson,
            styleTags = selectedStyleTags,
            conditionTags = selectedConditionTags,
            subjectText = subjectText,
            sceneText = sceneText,
            extraText = extraText,
            avoidText = avoidText,
            matchedEntries = matchedEntries,
            categoryByKey = categoryByKey
        )
    }
    val sourceDigest = remember(language, matchedEntries) {
        matchedEntries.take(COLOR_CARD_PROMPT_GENERATOR_MAX_MATCHED_SOURCES)
            .joinToString(" · ") { it.title(language) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = localizedColorCardPromptGeneratorPageTitle(language),
            onBack = onBack,
            trailing = {
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.ClearAll,
                    contentDescription = localizedColorCardPromptGeneratorResetLabel(language),
                    onClick = {
                        selectedPurposeKey = ""
                        selectedPersonKey = "any"
                        selectedStyleTags = emptyList()
                        selectedConditionTags = emptyList()
                        subjectText = ""
                        sceneText = ""
                        extraText = ""
                        avoidText = ""
                    }
                )
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                ColorCardPromptGeneratorSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = localizedColorCardPromptGeneratorPageSubtitle(language),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = localizedColorCardPromptGeneratorPageHint(language),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ColorCardPromptGeneratorSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = localizedColorCardPromptGeneratorPurposeTitle(language),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorCardPromptGeneratorPurposeChoices.forEach { choice ->
                                ColorCardPromptGeneratorChoiceChip(
                                    text = choice.label(language),
                                    selected = choice.key == selectedPurposeKey,
                                    onClick = {
                                        selectedPurposeKey = if (selectedPurposeKey == choice.key) "" else choice.key
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ColorCardPromptGeneratorSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = localizedColorCardPromptGeneratorPersonTitle(language),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorCardPromptGeneratorPersonChoices.forEach { choice ->
                                ColorCardPromptGeneratorChoiceChip(
                                    text = choice.label(language),
                                    selected = choice.key == selectedPersonKey,
                                    onClick = { selectedPersonKey = choice.key }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ColorCardPromptGeneratorSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = localizedColorCardPromptGeneratorStyleTagTitle(language),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = localizedColorCardPromptGeneratorStyleTagHint(language),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            styleTagSuggestions.forEach { tag ->
                                ColorCardPromptGeneratorChoiceChip(
                                    text = tag,
                                    selected = tag in selectedStyleTags,
                                    onClick = {
                                        selectedStyleTags = toggleColorCardPromptGeneratorSelection(selectedStyleTags, tag)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ColorCardPromptGeneratorSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = localizedColorCardPromptGeneratorConditionTagTitle(language),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = localizedColorCardPromptGeneratorConditionTagHint(language),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            conditionTagSuggestions.forEach { tag ->
                                ColorCardPromptGeneratorChoiceChip(
                                    text = tag,
                                    selected = tag in selectedConditionTags,
                                    onClick = {
                                        selectedConditionTags = toggleColorCardPromptGeneratorSelection(selectedConditionTags, tag)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ColorCardPromptGeneratorSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = localizedColorCardPromptGeneratorInputTitle(language),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = localizedColorCardPromptGeneratorInputHint(language),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = subjectText,
                            onValueChange = { subjectText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(localizedColorCardPromptGeneratorSubjectLabel(language)) },
                            placeholder = { Text(localizedColorCardPromptGeneratorSubjectPlaceholder(language)) },
                            minLines = 1,
                            maxLines = 2
                        )
                        OutlinedTextField(
                            value = sceneText,
                            onValueChange = { sceneText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(localizedColorCardPromptGeneratorSceneLabel(language)) },
                            placeholder = { Text(localizedColorCardPromptGeneratorScenePlaceholder(language)) },
                            minLines = 1,
                            maxLines = 2
                        )
                        OutlinedTextField(
                            value = extraText,
                            onValueChange = { extraText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(localizedColorCardPromptGeneratorExtraLabel(language)) },
                            placeholder = { Text(localizedColorCardPromptGeneratorExtraPlaceholder(language)) },
                            minLines = 2,
                            maxLines = 4
                        )
                        OutlinedTextField(
                            value = avoidText,
                            onValueChange = { avoidText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(localizedColorCardPromptGeneratorAvoidLabel(language)) },
                            placeholder = { Text(localizedColorCardPromptGeneratorAvoidPlaceholder(language)) },
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }
            }

            if (matchedEntries.isNotEmpty()) {
                item {
                    ColorCardPromptGeneratorSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = localizedColorCardPromptGeneratorSourcesTitle(language),
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = localizedColorCardPromptGeneratorSourcesHint(language),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                matchedEntries.take(COLOR_CARD_PROMPT_GENERATOR_MAX_MATCHED_SOURCES).forEach { entry ->
                                    ColorCardPromptGeneratorInfoChip(
                                        text = entry.title(language)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ColorCardPromptGeneratorSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = localizedColorCardPromptGeneratorResultTitle(language),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (sourceDigest.isNotBlank()) {
                            Text(
                                text = localizedColorCardPromptGeneratorSourceDigest(language, sourceDigest),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SelectionContainer {
                            Text(
                                text = generatedPrompt,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(18.dp)
                            )
                        }
                        val copyButtonShape = RoundedCornerShape(18.dp)
                        val copyButtonBorder = if (isSystemInDarkTheme()) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        }
                        FilledTonalButton(
                            onClick = {
                                copyColorCardPromptGeneratorText(
                                    context = context,
                                    label = localizedColorCardPromptGeneratorCopyLabel(language),
                                    text = generatedPrompt,
                                    successMessage = globalCopy.copied
                                )
                            },
                            shape = copyButtonShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, copyButtonBorder, copyButtonShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = localizedColorCardPromptGeneratorCopyLabel(language))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorCardPromptGeneratorSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun ColorCardPromptGeneratorChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val interactionSource = remember(text) { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = if (selected) 0.22f else 0.08f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ColorCardPromptGeneratorInfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
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

private fun loadColorCardPromptGeneratorCorpus(context: Context): ColorCardPromptGeneratorCorpus {
    cachedColorCardPromptGeneratorCorpus?.let { return it }
    synchronized(colorCardPromptGeneratorCorpusLock) {
        cachedColorCardPromptGeneratorCorpus?.let { return it }
        val loaded = runCatching {
            context.assets.open(COLOR_CARD_PROMPT_GENERATOR_MANIFEST_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { reader ->
                    parseColorCardPromptGeneratorCorpus(
                        JSONObject(reader.readText().removePrefix("\uFEFF"))
                    )
                }
        }.getOrElse {
            ColorCardPromptGeneratorCorpus(
                categories = emptyList(),
                entries = emptyList()
            )
        }
        cachedColorCardPromptGeneratorCorpus = loaded
        return loaded
    }
}

private fun parseColorCardPromptGeneratorCorpus(root: JSONObject): ColorCardPromptGeneratorCorpus {
    val categories = ArrayList<ColorCardPromptGeneratorCategory>()
    val categoryArray = root.optJSONArray("categories")
    for (index in 0 until (categoryArray?.length() ?: 0)) {
        val categoryObject = categoryArray?.optJSONObject(index) ?: continue
        val key = categoryObject.optString("key").trim()
        if (key.isBlank()) continue
        categories += ColorCardPromptGeneratorCategory(
            key = key,
            order = categoryObject.optInt("order", index + 1),
            zhTitle = categoryObject.optString("zh_title").trim(),
            enTitle = categoryObject.optString("en_title").trim()
        )
    }
    val categoryKeys = categories.map { it.key }.toSet()
    val liveEntries = parseColorCardPromptGeneratorEntries(
        root = root,
        arrayName = "entries",
        allowedCategoryKeys = categoryKeys,
        source = ColorCardPromptGeneratorEntrySource.Live
    )
    val draftEntries = parseColorCardPromptGeneratorEntries(
        root = root,
        arrayName = "draft_entries",
        allowedCategoryKeys = categoryKeys,
        source = ColorCardPromptGeneratorEntrySource.Draft
    )
    return ColorCardPromptGeneratorCorpus(
        categories = categories.sortedBy { it.order },
        entries = (liveEntries + draftEntries).sortedWith(
            compareBy<ColorCardPromptGeneratorEntry> { it.source != ColorCardPromptGeneratorEntrySource.Live }
                .thenBy { it.id }
        )
    )
}

private fun parseColorCardPromptGeneratorEntries(
    root: JSONObject,
    arrayName: String,
    allowedCategoryKeys: Set<String>,
    source: ColorCardPromptGeneratorEntrySource
): List<ColorCardPromptGeneratorEntry> {
    val entries = ArrayList<ColorCardPromptGeneratorEntry>()
    val array = root.optJSONArray(arrayName)
    for (index in 0 until (array?.length() ?: 0)) {
        val entryObject = array?.optJSONObject(index) ?: continue
        val id = entryObject.optString("id").trim()
        val categoryKey = entryObject.optString("category_key").trim()
        val promptBody = entryObject.optString("prompt_template").trim()
            .ifBlank { entryObject.optString("prompt").trim() }
        if (id.isBlank() || categoryKey.isBlank() || promptBody.isBlank() || categoryKey !in allowedCategoryKeys) {
            continue
        }
        val tags = buildList {
            val tagArray = entryObject.optJSONArray("tags")
            for (tagIndex in 0 until (tagArray?.length() ?: 0)) {
                val tag = tagArray?.optString(tagIndex).orEmpty().trim()
                if (tag.isNotBlank()) add(tag)
            }
        }
        entries += ColorCardPromptGeneratorEntry(
            id = id,
            categoryKey = categoryKey,
            zhTitle = entryObject.optString("zh_title").trim(),
            enTitle = entryObject.optString("en_title").trim(),
            zhSummary = entryObject.optString("zh_summary").trim(),
            enSummary = entryObject.optString("en_summary").trim(),
            tags = tags,
            prompt = promptBody,
            source = source
        )
    }
    return entries
}

private fun buildColorCardPromptGeneratorScopedEntries(
    entries: List<ColorCardPromptGeneratorEntry>,
    purpose: ColorCardPromptGeneratorChoice?,
    personKey: String
): List<ColorCardPromptGeneratorEntry> {
    val purposeScoped = if (purpose == null) {
        entries
    } else {
        entries.filter { it.categoryKey in purpose.categoryKeys }
    }
    val personScoped = purposeScoped.filter { entry ->
        colorCardPromptGeneratorPersonMatches(entry, personKey)
    }
    return when {
        personScoped.isNotEmpty() -> personScoped
        purposeScoped.isNotEmpty() -> purposeScoped
        else -> entries
    }
}

private fun buildColorCardPromptGeneratorSuggestedTags(
    entries: List<ColorCardPromptGeneratorEntry>,
    selectedTags: List<String>,
    conditionLike: Boolean,
    maxCount: Int
): List<String> {
    val ordered = linkedSetOf<String>()
    ordered.addAll(selectedTags.filter { isColorCardPromptGeneratorConditionLikeTag(it) == conditionLike })
    val frequencies = linkedMapOf<String, Int>()
    entries.forEach { entry ->
        entry.tags.forEach { tag ->
            if (isColorCardPromptGeneratorConditionLikeTag(tag) == conditionLike) {
                frequencies[tag] = (frequencies[tag] ?: 0) + 1
            }
        }
    }
    frequencies.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(maxCount)
        .forEach { ordered.add(it.key) }
    return ordered.toList().take(maxCount)
}

private fun buildColorCardPromptGeneratorMatchedEntries(
    entries: List<ColorCardPromptGeneratorEntry>,
    purpose: ColorCardPromptGeneratorChoice?,
    personKey: String,
    selectedTags: List<String>,
    freeText: String
): List<ColorCardPromptGeneratorEntry> {
    val userTokens = extractColorCardPromptGeneratorTokens(freeText)
    val scored = entries.map { entry ->
        val searchable = entry.searchText()
        var score = if (entry.source == ColorCardPromptGeneratorEntrySource.Live) 2 else 0
        if (purpose != null && entry.categoryKey in purpose.categoryKeys) {
            score += 8
        }
        if (colorCardPromptGeneratorPersonMatches(entry, personKey)) {
            score += if (personKey == "any") 0 else 4
        } else if (personKey != "any") {
            score -= 3
        }
        selectedTags.forEach { tag ->
            if (entry.tags.any { it.equals(tag, ignoreCase = true) }) {
                score += 7
            } else if (searchable.contains(tag.lowercase())) {
                score += 2
            }
        }
        userTokens.forEach { token ->
            if (searchable.contains(token.lowercase())) {
                score += 1
            }
        }
        entry to score
    }.sortedWith(
        compareByDescending<Pair<ColorCardPromptGeneratorEntry, Int>> { it.second }
            .thenBy { it.first.id }
    )
    return scored.map { it.first }.take(8)
}

private fun buildColorCardPromptGeneratorResult(
    language: AppLanguage,
    purpose: ColorCardPromptGeneratorChoice?,
    person: ColorCardPromptGeneratorChoice,
    styleTags: List<String>,
    conditionTags: List<String>,
    subjectText: String,
    sceneText: String,
    extraText: String,
    avoidText: String,
    matchedEntries: List<ColorCardPromptGeneratorEntry>,
    categoryByKey: Map<String, ColorCardPromptGeneratorCategory>
): String = when (language) {
    AppLanguage.SimplifiedChinese,
    AppLanguage.TraditionalChinese -> buildColorCardPromptGeneratorResultZh(
        purpose = purpose,
        person = person,
        styleTags = styleTags,
        conditionTags = conditionTags,
        subjectText = subjectText,
        sceneText = sceneText,
        extraText = extraText,
        avoidText = avoidText,
        matchedEntries = matchedEntries,
        categoryByKey = categoryByKey
    )
    else -> buildColorCardPromptGeneratorResultEn(
        purpose = purpose,
        person = person,
        styleTags = styleTags,
        conditionTags = conditionTags,
        subjectText = subjectText,
        sceneText = sceneText,
        extraText = extraText,
        avoidText = avoidText,
        matchedEntries = matchedEntries,
        categoryByKey = categoryByKey
    )
}

private fun buildColorCardPromptGeneratorResultZh(
    purpose: ColorCardPromptGeneratorChoice?,
    person: ColorCardPromptGeneratorChoice,
    styleTags: List<String>,
    conditionTags: List<String>,
    subjectText: String,
    sceneText: String,
    extraText: String,
    avoidText: String,
    matchedEntries: List<ColorCardPromptGeneratorEntry>,
    categoryByKey: Map<String, ColorCardPromptGeneratorCategory>
): String {
    val parts = ArrayList<String>()
    parts += purpose?.zhPrompt ?: "生成一段可直接用于 AI 生图的高完成度提示词"
    if (person.zhPrompt.isNotBlank()) {
        parts += person.zhPrompt
    }
    if (subjectText.isNotBlank()) {
        parts += "主题或主体为${subjectText.trim()}"
    }
    if (sceneText.isNotBlank()) {
        parts += "场景或内容设定为${sceneText.trim()}"
    }
    if (styleTags.isNotEmpty()) {
        parts += "风格关键词可参考${styleTags.joinToString("、")}"
    }
    if (conditionTags.isNotEmpty()) {
        parts += "可复用条件包括${conditionTags.joinToString("、")}"
    }
    buildColorCardPromptGeneratorCorpusHintZh(matchedEntries, categoryByKey)?.let { parts += it }
    val reusableFragments = buildColorCardPromptGeneratorReusableFragments(matchedEntries, styleTags + conditionTags)
    if (reusableFragments.isNotEmpty()) {
        parts += reusableFragments.joinToString("，")
    }
    if (extraText.isNotBlank()) {
        parts += extraText.trim()
    }
    if (avoidText.isNotBlank()) {
        parts += "避免${avoidText.trim()}"
    }
    parts += "保证主体明确，构图完整，层次清晰，细节丰富，风格统一，避免廉价模板感"
    return cleanupColorCardPromptGeneratorResult(parts.joinToString("，"))
}

private fun buildColorCardPromptGeneratorResultEn(
    purpose: ColorCardPromptGeneratorChoice?,
    person: ColorCardPromptGeneratorChoice,
    styleTags: List<String>,
    conditionTags: List<String>,
    subjectText: String,
    sceneText: String,
    extraText: String,
    avoidText: String,
    matchedEntries: List<ColorCardPromptGeneratorEntry>,
    categoryByKey: Map<String, ColorCardPromptGeneratorCategory>
): String {
    val parts = ArrayList<String>()
    parts += purpose?.enPrompt ?: "Generate a polished prompt for image creation."
    if (person.enPrompt.isNotBlank()) {
        parts += person.enPrompt
    }
    if (subjectText.isNotBlank()) {
        parts += "The main subject or theme is ${subjectText.trim()}."
    }
    if (sceneText.isNotBlank()) {
        parts += "The scene or content should be ${sceneText.trim()}."
    }
    if (styleTags.isNotEmpty()) {
        parts += "Style keywords can borrow from ${styleTags.joinToString(", ")}."
    }
    if (conditionTags.isNotEmpty()) {
        parts += "Reusable conditions include ${conditionTags.joinToString(", ")}."
    }
    buildColorCardPromptGeneratorCorpusHintEn(matchedEntries, categoryByKey)?.let { parts += it }
    val reusableFragments = buildColorCardPromptGeneratorReusableFragments(matchedEntries, styleTags + conditionTags)
    if (reusableFragments.isNotEmpty()) {
        parts += reusableFragments.joinToString(", ")
    }
    if (extraText.isNotBlank()) {
        parts += extraText.trim()
    }
    if (avoidText.isNotBlank()) {
        parts += "Avoid ${avoidText.trim()}."
    }
    parts += "Keep the subject clear, the composition coherent, the layers readable, the details rich, and the overall style consistent."
    return parts.joinToString(" ").replace("  ", " ").trim()
}

private fun buildColorCardPromptGeneratorCorpusHintZh(
    matchedEntries: List<ColorCardPromptGeneratorEntry>,
    categoryByKey: Map<String, ColorCardPromptGeneratorCategory>
): String? {
    val categories = matchedEntries.mapNotNull { categoryByKey[it.categoryKey]?.zhTitle }
        .distinct()
        .take(2)
    val tags = matchedEntries.flatMap { it.tags }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
    val summaries = matchedEntries.mapNotNull { it.zhSummary.takeIf(String::isNotBlank) }
        .distinct()
        .take(2)
    val parts = ArrayList<String>()
    if (categories.isNotEmpty()) {
        parts += "可参考${categories.joinToString("、")}这类语料方向"
    }
    if (tags.isNotEmpty()) {
        parts += "相关词可以吸收${tags.joinToString("、")}"
    }
    if (summaries.isNotEmpty()) {
        parts += summaries.joinToString("，")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("，")
}

private fun buildColorCardPromptGeneratorCorpusHintEn(
    matchedEntries: List<ColorCardPromptGeneratorEntry>,
    categoryByKey: Map<String, ColorCardPromptGeneratorCategory>
): String? {
    val categories = matchedEntries.mapNotNull { entry ->
        val category = categoryByKey[entry.categoryKey] ?: return@mapNotNull null
        category.enTitle.ifBlank { category.zhTitle }
    }
        .distinct()
        .take(2)
    val tags = matchedEntries.flatMap { it.tags }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
    val summaries = matchedEntries.mapNotNull { entry ->
        entry.enSummary.takeIf(String::isNotBlank) ?: entry.zhSummary.takeIf(String::isNotBlank)
    }
        .distinct()
        .take(2)
    val parts = ArrayList<String>()
    if (categories.isNotEmpty()) {
        parts += "Take direction from ${categories.joinToString(", ")}."
    }
    if (tags.isNotEmpty()) {
        parts += "Useful corpus tags include ${tags.joinToString(", ")}."
    }
    if (summaries.isNotEmpty()) {
        parts += summaries.joinToString(" ")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
}

private fun buildColorCardPromptGeneratorReusableFragments(
    matchedEntries: List<ColorCardPromptGeneratorEntry>,
    selectedTags: List<String>
): List<String> {
    val ordered = linkedSetOf<String>()
    matchedEntries.take(COLOR_CARD_PROMPT_GENERATOR_MAX_MATCHED_SOURCES).forEach { entry ->
        splitColorCardPromptGeneratorPrompt(entry.prompt)
            .map { fragment -> fragment to scoreColorCardPromptGeneratorFragment(fragment, selectedTags) }
            .sortedByDescending { it.second }
            .take(3)
            .forEach { (fragment, score) ->
                if (score > 0) {
                    ordered += fragment
                }
            }
    }
    return ordered.toList().take(6)
}

private fun splitColorCardPromptGeneratorPrompt(prompt: String): List<String> =
    prompt.replace("{{", "")
        .replace("}}", "")
        .replace("（", "")
        .replace("）", "")
        .split('，', '。', '；', '\n', ',', ';', ':')
        .map { it.trim() }
        .filter { fragment ->
            fragment.length in 4..28 &&
                colorCardPromptGeneratorNoisePrefixes.none { prefix -> fragment.startsWith(prefix) } &&
                !fragment.contains("比例") &&
                !fragment.contains("prompt", ignoreCase = true) &&
                !fragment.contains("best quality", ignoreCase = true)
        }

private fun scoreColorCardPromptGeneratorFragment(
    fragment: String,
    selectedTags: List<String>
): Int {
    var score = 0
    if (selectedTags.any { tag -> fragment.contains(tag, ignoreCase = true) }) {
        score += 6
    }
    if (colorCardPromptGeneratorDescriptorKeywords.any { keyword -> fragment.contains(keyword, ignoreCase = true) }) {
        score += 4
    }
    if (fragment.length in 6..18) {
        score += 2
    }
    if (fragment.contains("主体视觉") || fragment.contains("人物为主体")) {
        score += 1
    }
    if (fragment.any { it.isDigit() } && !fragment.contains("风格") && !fragment.contains("年代")) {
        score -= 2
    }
    return score
}

private fun colorCardPromptGeneratorPersonMatches(
    entry: ColorCardPromptGeneratorEntry,
    personKey: String
): Boolean {
    if (personKey == "any") return true
    val personKind = colorCardPromptGeneratorPersonKind(entry)
    return when (personKey) {
        "single" -> personKind == ColorCardPromptGeneratorPersonKind.Single
        "group" -> personKind == ColorCardPromptGeneratorPersonKind.Group
        "none" -> personKind == ColorCardPromptGeneratorPersonKind.NoPeople
        "pet" -> personKind == ColorCardPromptGeneratorPersonKind.Pet
        else -> true
    }
}

private fun colorCardPromptGeneratorPersonKind(entry: ColorCardPromptGeneratorEntry): ColorCardPromptGeneratorPersonKind {
    val searchable = entry.searchText()
    return when {
        listOf(
            "全家福",
            "family",
            "group",
            "多人",
            "群像",
            "couple",
            "少年少女",
            "两个孩子",
            "friends",
            "children"
        ).any {
            searchable.contains(it.lowercase())
        } ->
            ColorCardPromptGeneratorPersonKind.Group
        listOf(
            "宠物",
            "萌宠",
            "cat",
            "dog",
            "pet",
            "小猫",
            "猫",
            "动物",
            "熊猫",
            "狐狸",
            "独角兽",
            "橘猫",
            "金鱼",
            "panda",
            "fox",
            "unicorn",
            "fish",
            "goldfish"
        ).any { searchable.contains(it.lowercase()) } ->
            ColorCardPromptGeneratorPersonKind.Pet
        entry.categoryKey in setOf(
            "narrative_scene",
            "handmade_texture",
            "playful_character",
            "graphic_layout",
            "cinematic_narrative",
            "editorial_panel",
            "oriental_aesthetics",
            "style_experiment",
            "object_display",
            "spatial_build"
        ) && listOf(
            "人物",
            "少女",
            "肖像",
            "自拍",
            "角色",
            "女性",
            "女人",
            "男孩",
            "女孩",
            "小孩",
            "孩子",
            "儿童",
            "少年",
            "男生",
            "女生",
            "woman",
            "man",
            "girl",
            "boy",
            "person",
            "people",
            "portrait",
            "selfie",
            "female",
            "male",
            "kid",
            "child"
        ).none { searchable.contains(it.lowercase()) } ->
            ColorCardPromptGeneratorPersonKind.NoPeople
        else -> ColorCardPromptGeneratorPersonKind.Single
    }
}

private fun isColorCardPromptGeneratorConditionLikeTag(tag: String): Boolean =
    colorCardPromptGeneratorConditionKeywords.any { keyword ->
        tag.contains(keyword, ignoreCase = true)
    }

private fun toggleColorCardPromptGeneratorSelection(current: List<String>, value: String): List<String> =
    if (value in current) {
        current.filterNot { it == value }
    } else {
        current + value
    }

private fun extractColorCardPromptGeneratorTokens(text: String): List<String> =
    text.split(' ', '\n', '\t', ',', '，', '。', ';', '；', '/', '|')
        .map { it.trim() }
        .filter { it.length >= 2 }
        .distinct()
        .take(8)

private fun cleanupColorCardPromptGeneratorResult(text: String): String =
    text.replace("，，", "，")
        .replace("。。", "。")
        .replace("，。", "。")
        .trim('，', '。', ' ')

private fun copyColorCardPromptGeneratorText(
    context: Context,
    label: String,
    text: String,
    successMessage: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
}

private fun localizedColorCardPromptGeneratorBarTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "提示词生成器"
    AppLanguage.TraditionalChinese -> "提示詞生成器"
    else -> "Prompt Builder"
}

private fun localizedColorCardPromptGeneratorBarSubtitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "选用途、人物和库内 tag，把语料里的可复用文案自动拼进结果。"
    AppLanguage.TraditionalChinese -> "選用途、人物和庫內 tag，把語料裡可複用的文案自動拼進結果。"
    else -> "Choose intent, subject type, and corpus tags, then blend reusable copy into one prompt."
}

private fun localizedColorCardPromptGeneratorPageTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "提示词生成器"
    AppLanguage.TraditionalChinese -> "提示詞生成器"
    else -> "Prompt Generator"
}

private fun localizedColorCardPromptGeneratorPageSubtitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "这是一个独立工具，不改原有提示词库，只复用库里的分类、tag 和文案片段。"
    AppLanguage.TraditionalChinese -> "這是一個獨立工具，不改原有提示詞庫，只複用庫裡的分類、tag 和文案片段。"
    else -> "This tool stays separate from the existing prompt library and only reuses its corpus."
}

private fun localizedColorCardPromptGeneratorPageHint(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "先定用途和人物，再挑库内 tag。像“上美影”这种 tag，会把对应语料里的可复用描述自动吸收进结果。"
    AppLanguage.TraditionalChinese -> "先定用途和人物，再挑庫內 tag。像「上美影」這種 tag，會把對應語料裡可複用的描述自動吸收進結果。"
    else -> "Pick the use case and subject type first, then choose corpus tags. Tags like 'Shanghai Animation' will pull reusable copy from the matched source prompt."
}

private fun localizedColorCardPromptGeneratorPurposeTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "用途"
    AppLanguage.TraditionalChinese -> "用途"
    else -> "Use Case"
}

private fun localizedColorCardPromptGeneratorPersonTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "是否有人物"
    AppLanguage.TraditionalChinese -> "是否有人物"
    else -> "Subject Type"
}

private fun localizedColorCardPromptGeneratorStyleTagTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "库内画风词"
    AppLanguage.TraditionalChinese -> "庫內畫風詞"
    else -> "Corpus Style Tags"
}

private fun localizedColorCardPromptGeneratorStyleTagHint(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "直接用库里的 tag 做方向词，例如上美影、梦核、韩系、RISO。"
    AppLanguage.TraditionalChinese -> "直接用庫裡的 tag 做方向詞，例如上美影、夢核、韓系、RISO。"
    else -> "These are direct tags from the prompt corpus, such as Shanghai Animation, dreamcore, Korean style, or RISO."
}

private fun localizedColorCardPromptGeneratorConditionTagTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "可复用条件"
    AppLanguage.TraditionalChinese -> "可複用條件"
    else -> "Reusable Conditions"
}

private fun localizedColorCardPromptGeneratorConditionTagHint(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "这里优先放打光、构图、场景、材质这类更像“条件”的库内 tag。"
    AppLanguage.TraditionalChinese -> "這裡優先放打光、構圖、場景、材質這類更像「條件」的庫內 tag。"
    else -> "These tags lean more toward lighting, composition, scene setup, and reusable technical conditions."
}

private fun localizedColorCardPromptGeneratorInputTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "少量补充"
    AppLanguage.TraditionalChinese -> "少量補充"
    else -> "Small Custom Input"
}

private fun localizedColorCardPromptGeneratorInputHint(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "不用写很长，只补几个关键词或一句意图就够了。"
    AppLanguage.TraditionalChinese -> "不用寫很長，只補幾個關鍵詞或一句意圖就夠了。"
    else -> "Keep the text short. A few keywords or one sentence is enough."
}

private fun localizedColorCardPromptGeneratorSubjectLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "主体 / 主题"
    AppLanguage.TraditionalChinese -> "主體 / 主題"
    else -> "Subject / Theme"
}

private fun localizedColorCardPromptGeneratorSubjectPlaceholder(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "例如：唐僧西天取经、短发女孩、香水瓶、未来城市。"
    AppLanguage.TraditionalChinese -> "例如：唐僧西天取經、短髮女孩、香水瓶、未來城市。"
    else -> "For example: monk on a journey, short-haired girl, perfume bottle, future city."
}

private fun localizedColorCardPromptGeneratorSceneLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "场景 / 内容"
    AppLanguage.TraditionalChinese -> "場景 / 內容"
    else -> "Scene / Content"
}

private fun localizedColorCardPromptGeneratorScenePlaceholder(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "例如：浴室自拍、海边黄昏、宣纸绘本页面、展览海报版式。"
    AppLanguage.TraditionalChinese -> "例如：浴室自拍、海邊黃昏、宣紙繪本頁面、展覽海報版式。"
    else -> "For example: bathroom selfie, seaside dusk, rice-paper storybook page, exhibition poster layout."
}

private fun localizedColorCardPromptGeneratorExtraLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "补充要求"
    AppLanguage.TraditionalChinese -> "補充要求"
    else -> "Extra Direction"
}

private fun localizedColorCardPromptGeneratorExtraPlaceholder(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "例如：希望更克制一点，不要太花，保留老动画气质。"
    AppLanguage.TraditionalChinese -> "例如：希望更克制一點，不要太花，保留老動畫氣質。"
    else -> "For example: keep it restrained, not too busy, and retain an old-animation feeling."
}

private fun localizedColorCardPromptGeneratorAvoidLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "避免项"
    AppLanguage.TraditionalChinese -> "避免項"
    else -> "Avoid"
}

private fun localizedColorCardPromptGeneratorAvoidPlaceholder(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "例如：廉价奇幻素材、模板化背景、脏乱排版。"
    AppLanguage.TraditionalChinese -> "例如：廉價奇幻素材、模板化背景、髒亂排版。"
    else -> "For example: cheap fantasy assets, templated backgrounds, messy layout."
}

private fun localizedColorCardPromptGeneratorSourcesTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "当前参考语料"
    AppLanguage.TraditionalChinese -> "當前參考語料"
    else -> "Matched Corpus"
}

private fun localizedColorCardPromptGeneratorSourcesHint(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "这些条目会参与 tag 匹配和可复用文案的抽取。"
    AppLanguage.TraditionalChinese -> "這些條目會參與 tag 匹配和可複用文案的抽取。"
    else -> "These matched prompts drive both the tag match and the reusable copy extraction."
}

private fun localizedColorCardPromptGeneratorResultTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "生成结果"
    AppLanguage.TraditionalChinese -> "生成結果"
    else -> "Generated Prompt"
}

private fun localizedColorCardPromptGeneratorSourceDigest(language: AppLanguage, digest: String): String = when (language) {
    AppLanguage.SimplifiedChinese -> "当前拼接来源：$digest"
    AppLanguage.TraditionalChinese -> "當前拼接來源：$digest"
    else -> "Current blend: $digest"
}

private fun localizedColorCardPromptGeneratorCopyLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "复制提示词"
    AppLanguage.TraditionalChinese -> "複製提示詞"
    else -> "Copy Prompt"
}

private fun localizedColorCardPromptGeneratorResetLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "清空"
    AppLanguage.TraditionalChinese -> "清空"
    else -> "Reset"
}
