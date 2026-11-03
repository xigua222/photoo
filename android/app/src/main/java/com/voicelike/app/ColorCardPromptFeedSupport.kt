package com.voicelike.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

private const val COLOR_CARD_PROMPT_MANIFEST_ASSET = "color_card_prompt/prompts.json"
private const val COLOR_CARD_PROMPT_PREVIEW_DIR = "color_card_prompt/previews"
private const val COLOR_CARD_PROMPT_PREVIEW_HEIGHT_DP = 148
private const val COLOR_CARD_PROMPT_PREVIEW_WIDTH_DP = 112
private const val COLOR_CARD_PROMPT_DETAIL_IMAGE_MAX_WIDTH_DP = 212
private const val COLOR_CARD_PROMPT_DETAIL_ASPECT_RATIO =
    COLOR_CARD_PROMPT_PREVIEW_WIDTH_DP.toFloat() / COLOR_CARD_PROMPT_PREVIEW_HEIGHT_DP.toFloat()
private val colorCardPromptTemplateSlotRegex = Regex("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}")

internal data class ColorCardPromptCategory(
    val key: String,
    val order: Int,
    val zhTitle: String,
    val enTitle: String,
    val zhSubtitle: String,
    val enSubtitle: String
) {
    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhTitle
        else -> enTitle
    }

    fun subtitle(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhSubtitle
        else -> enSubtitle
    }
}

internal data class ColorCardPromptEntry(
    val id: String,
    val imageKey: String,
    val categoryKey: String,
    val zhTitle: String,
    val enTitle: String,
    val zhSummary: String,
    val enSummary: String,
    val platform: String,
    val exampleMode: ColorCardPromptExampleMode,
    val tags: List<String>,
    val prompt: String,
    val promptTemplate: String,
    val editableSlots: List<ColorCardPromptEditableSlot>
) {
    val previewImageAssetName: String
        get() = "$COLOR_CARD_PROMPT_PREVIEW_DIR/$imageKey.webp"

    val supportsEditablePrompt: Boolean
        get() = promptTemplate.isNotBlank() && editableSlots.isNotEmpty()

    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhTitle
        else -> enTitle
    }

    fun summary(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhSummary
        else -> enSummary
    }

    fun resolvedPrompt(slotValues: Map<String, String>): String {
        if (!supportsEditablePrompt) return prompt
        if (editableSlots.none { slotValues[it.key].orEmpty().trim().isNotBlank() }) {
            return prompt
        }
        var resolved = promptTemplate
        editableSlots.forEach { slot ->
            val replacement = slotValues[slot.key].orEmpty().trim().ifBlank { slot.defaultValue }
            resolved = resolved.replace("{{${slot.key}}}", replacement)
        }
        return resolved
    }
}

internal data class ColorCardPromptEditableSlot(
    val key: String,
    val zhLabel: String,
    val enLabel: String,
    val defaultValue: String
) {
    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhLabel.ifBlank { enLabel }
        else -> enLabel.ifBlank { zhLabel }
    }
}

internal enum class ColorCardPromptExampleMode {
    Required,
    Optional,
    None
}

internal data class ColorCardPromptSeries(
    val key: String,
    val category: ColorCardPromptCategory,
    val entries: List<ColorCardPromptEntry>
) {

    fun title(language: AppLanguage): String = category.title(language)

    fun subtitle(language: AppLanguage): String = category.subtitle(language)
}

internal data class ColorCardPromptFeedState(
    val series: List<ColorCardPromptSeries>,
    val previewBitmaps: Map<String, Bitmap>,
    val isLoading: Boolean
)

private data class ColorCardPromptManifest(
    val series: List<ColorCardPromptSeries>
)

private val colorCardPromptManifestLock = Any()
@Volatile
private var cachedColorCardPromptManifest: ColorCardPromptManifest? = null

private val colorCardPromptBitmapCacheLock = Any()
private val colorCardPromptBitmapCache = linkedMapOf<String, Bitmap>()

internal fun localizedColorCardPromptLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "提示词"
    AppLanguage.TraditionalChinese -> "提示詞"
    AppLanguage.Japanese -> "プロンプト"
    AppLanguage.Korean -> "프롬프트"
    AppLanguage.French -> "Prompt"
    AppLanguage.German -> "Prompt"
    AppLanguage.Italian -> "Prompt"
    AppLanguage.Spanish -> "Prompt"
    AppLanguage.Portuguese -> "Prompt"
    AppLanguage.Russian -> "Промпт"
    AppLanguage.Thai -> "พรอมต์"
    AppLanguage.Vietnamese -> "Prompt"
    AppLanguage.Indonesian -> "Prompt"
    AppLanguage.Arabic -> "Prompt"
    AppLanguage.Ukrainian -> "Промпт"
    AppLanguage.Polish -> "Prompt"
    AppLanguage.Czech -> "Prompt"
    AppLanguage.Dutch -> "Prompt"
    AppLanguage.Swedish -> "Prompt"
    AppLanguage.Danish -> "Prompt"
    AppLanguage.Norwegian -> "Prompt"
    AppLanguage.Finnish -> "Kehote"
    AppLanguage.Greek -> "Prompt"
    AppLanguage.Romanian -> "Prompt"
    AppLanguage.Hungarian -> "Prompt"
    AppLanguage.Turkish -> "İstem"
    AppLanguage.Malay -> "Prompt"
    AppLanguage.Hebrew -> "פרומפט"
    AppLanguage.English -> "Prompt"
}

private fun localizedColorCardPromptCopyLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "复制提示词"
    AppLanguage.TraditionalChinese -> "複製提示詞"
    AppLanguage.Japanese -> "プロンプトをコピー"
    AppLanguage.Korean -> "프롬프트 복사"
    AppLanguage.French -> "Copier le prompt"
    AppLanguage.German -> "Prompt kopieren"
    AppLanguage.Italian -> "Copia prompt"
    AppLanguage.Spanish -> "Copiar prompt"
    AppLanguage.Portuguese -> "Copiar prompt"
    AppLanguage.Russian -> "Скопировать промпт"
    AppLanguage.Thai -> "คัดลอกพรอมต์"
    AppLanguage.Vietnamese -> "Sao chép prompt"
    AppLanguage.Indonesian -> "Salin prompt"
    AppLanguage.Arabic -> "نسخ الـ prompt"
    AppLanguage.Ukrainian -> "Скопіювати промпт"
    AppLanguage.Polish -> "Kopiuj prompt"
    AppLanguage.Czech -> "Kopírovat prompt"
    AppLanguage.Dutch -> "Prompt kopiëren"
    AppLanguage.Swedish -> "Kopiera prompt"
    AppLanguage.Danish -> "Kopiér prompt"
    AppLanguage.Norwegian -> "Kopier prompt"
    AppLanguage.Finnish -> "Kopioi kehote"
    AppLanguage.Greek -> "Αντιγραφή prompt"
    AppLanguage.Romanian -> "Copiază promptul"
    AppLanguage.Hungarian -> "Prompt másolása"
    AppLanguage.Turkish -> "İstemi kopyala"
    AppLanguage.Malay -> "Salin prompt"
    AppLanguage.Hebrew -> "העתק פרומפט"
    AppLanguage.English -> "Copy Prompt"
}

private fun localizedColorCardPromptBodyLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "提示词正文"
    AppLanguage.TraditionalChinese -> "提示詞正文"
    AppLanguage.Ukrainian -> "Промпт"
    AppLanguage.Finnish -> "Kehote"
    AppLanguage.Turkish -> "İstem"
    AppLanguage.Hebrew -> "פרומפט"
    else -> "Prompt"
}

private fun localizedColorCardPromptEditConfirmLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "确认"
    AppLanguage.TraditionalChinese -> "確認"
    AppLanguage.Japanese -> "確認"
    AppLanguage.Korean -> "확인"
    AppLanguage.French -> "Confirmer"
    AppLanguage.German -> "Bestätigen"
    AppLanguage.Italian -> "Conferma"
    AppLanguage.Ukrainian -> "Підтвердити"
    AppLanguage.Polish -> "Potwierdź"
    AppLanguage.Czech -> "Potvrdit"
    AppLanguage.Dutch -> "Bevestigen"
    AppLanguage.Swedish -> "Bekräfta"
    AppLanguage.Danish -> "Bekræft"
    AppLanguage.Norwegian -> "Bekreft"
    AppLanguage.Finnish -> "Vahvista"
    AppLanguage.Greek -> "Επιβεβαίωση"
    AppLanguage.Romanian -> "Confirmă"
    AppLanguage.Hungarian -> "Megerősítés"
    AppLanguage.Turkish -> "Onayla"
    AppLanguage.Malay -> "Sahkan"
    AppLanguage.Hebrew -> "אישור"
    else -> "Confirm"
}

private fun localizedColorCardPromptReferenceLabel(
    language: AppLanguage,
    mode: ColorCardPromptExampleMode
): String = when (mode) {
    ColorCardPromptExampleMode.Required -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u9700\u8981\u57ab\u56fe"
        AppLanguage.TraditionalChinese -> "\u9700\u8981\u588a\u5716"
        AppLanguage.Ukrainian -> "Потрібен референс"
        AppLanguage.Polish -> "Wymagany wzorzec"
        AppLanguage.Czech -> "Vyžadována reference"
        AppLanguage.Dutch -> "Referentie vereist"
        AppLanguage.Swedish -> "Referens krävs"
        AppLanguage.Danish -> "Reference påkrævet"
        AppLanguage.Norwegian -> "Referanse kreves"
        AppLanguage.Finnish -> "Viite vaaditaan"
        AppLanguage.Greek -> "Απαιτείται αναφορά"
        AppLanguage.Romanian -> "Referință necesară"
        AppLanguage.Hungarian -> "Referencia szükséges"
        AppLanguage.Turkish -> "Referans gerekli"
        AppLanguage.Malay -> "Rujukan diperlukan"
        AppLanguage.Hebrew -> "נדרשת תמונת ייחוס"
        else -> "Reference Required"
    }
    ColorCardPromptExampleMode.Optional -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u53ef\u9009\u57ab\u56fe"
        AppLanguage.TraditionalChinese -> "\u53ef\u9078\u588a\u5716"
        AppLanguage.Ukrainian -> "Референс необов'язковий"
        AppLanguage.Polish -> "Wzorzec opcjonalny"
        AppLanguage.Czech -> "Reference volitelná"
        AppLanguage.Dutch -> "Referentie optioneel"
        AppLanguage.Swedish -> "Referens valfri"
        AppLanguage.Danish -> "Reference valgfri"
        AppLanguage.Norwegian -> "Referanse valgfri"
        AppLanguage.Finnish -> "Viite valinnainen"
        AppLanguage.Greek -> "Προαιρετική αναφορά"
        AppLanguage.Romanian -> "Referință opțională"
        AppLanguage.Hungarian -> "Referencia nem kötelező"
        AppLanguage.Turkish -> "Referans isteğe bağlı"
        AppLanguage.Malay -> "Rujukan pilihan"
        AppLanguage.Hebrew -> "תמונת ייחוס אופציונלית"
        else -> "Reference Optional"
    }
    ColorCardPromptExampleMode.None -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u65e0\u9700\u57ab\u56fe"
        AppLanguage.TraditionalChinese -> "\u7121\u9700\u588a\u5716"
        AppLanguage.Ukrainian -> "Без референсу"
        AppLanguage.Polish -> "Bez wzorca"
        AppLanguage.Czech -> "Bez reference"
        AppLanguage.Dutch -> "Geen referentie"
        AppLanguage.Swedish -> "Ingen referens"
        AppLanguage.Danish -> "Ingen reference"
        AppLanguage.Norwegian -> "Ingen referanse"
        AppLanguage.Finnish -> "Ei viitettä"
        AppLanguage.Greek -> "Χωρίς αναφορά"
        AppLanguage.Romanian -> "Fără referință"
        AppLanguage.Hungarian -> "Referencia nélkül"
        AppLanguage.Turkish -> "Referans yok"
        AppLanguage.Malay -> "Tiada rujukan"
        AppLanguage.Hebrew -> "ללא תמונת ייחוס"
        else -> "No Reference"
    }
}

private fun parseColorCardPromptExampleMode(rawValue: String): ColorCardPromptExampleMode =
    when (rawValue.trim().lowercase()) {
        "yes",
        "required" -> ColorCardPromptExampleMode.Required
        "optional" -> ColorCardPromptExampleMode.Optional
        else -> ColorCardPromptExampleMode.None
    }

private fun localizedColorCardPromptSourceModelLabel(
    language: AppLanguage,
    platform: String
): String {
    val normalizedPlatform = when (platform.trim().lowercase()) {
        "seed" -> "\u8c46\u5305/\u5373\u68a6"
        "yb" -> "\u5143\u5b9d"
        "qwen-image" -> "\u5343\u95ee"
        else -> platform.trim()
    }
    if (normalizedPlatform.isBlank()) return ""
    return when (language) {
        AppLanguage.SimplifiedChinese -> "\u6837\u56fe\u6765\u6e90\u6a21\u578b\uff1a$normalizedPlatform"
        AppLanguage.TraditionalChinese -> "\u6a23\u5716\u4f86\u6e90\u6a21\u578b\uff1a$normalizedPlatform"
        AppLanguage.Ukrainian -> "Модель зразка: $normalizedPlatform"
        AppLanguage.Polish -> "Model próbki: $normalizedPlatform"
        AppLanguage.Czech -> "Vzorový model: $normalizedPlatform"
        AppLanguage.Dutch -> "Voorbeeldmodel: $normalizedPlatform"
        AppLanguage.Swedish -> "Exempelmodell: $normalizedPlatform"
        AppLanguage.Danish -> "Eksempelmodel: $normalizedPlatform"
        AppLanguage.Norwegian -> "Eksempelmodell: $normalizedPlatform"
        AppLanguage.Finnish -> "Esimerkkimalli: $normalizedPlatform"
        AppLanguage.Greek -> "Μοντέλο δείγματος: $normalizedPlatform"
        AppLanguage.Romanian -> "Model eșantion: $normalizedPlatform"
        AppLanguage.Hungarian -> "Mintamodell: $normalizedPlatform"
        AppLanguage.Turkish -> "Örnek model: $normalizedPlatform"
        AppLanguage.Malay -> "Model contoh: $normalizedPlatform"
        AppLanguage.Hebrew -> "דגם לדוגמה: $normalizedPlatform"
        else -> "Sample Model: $normalizedPlatform"
    }
}

private fun colorCardPromptExampleContainerColor(
    mode: ColorCardPromptExampleMode,
    isDark: Boolean
): Color = when (mode) {
    ColorCardPromptExampleMode.Required -> if (isDark) Color(0xFF7D3A34) else Color(0xFFF7D8D5)
    ColorCardPromptExampleMode.Optional -> if (isDark) Color(0xFF5F4A1E) else Color(0xFFF6E8BF)
    ColorCardPromptExampleMode.None -> if (isDark) Color(0xFF1E5A4C) else Color(0xFFD8EFE4)
}

private fun colorCardPromptExampleContentColor(
    mode: ColorCardPromptExampleMode,
    isDark: Boolean
): Color = when (mode) {
    ColorCardPromptExampleMode.Required -> if (isDark) Color(0xFFFFD9D5) else Color(0xFF8F2F25)
    ColorCardPromptExampleMode.Optional -> if (isDark) Color(0xFFFFE39E) else Color(0xFF8E6510)
    ColorCardPromptExampleMode.None -> if (isDark) Color(0xFFC9F3E3) else Color(0xFF1C7C5C)
}

@Composable
internal fun rememberColorCardPromptFeedState(): ColorCardPromptFeedState {
    val context = LocalContext.current
    val shuffleSeed by rememberSaveable { mutableLongStateOf(Random.Default.nextLong()) }
    val manifest by produceState<ColorCardPromptManifest?>(initialValue = null, key1 = context) {
        value = withContext(Dispatchers.IO) {
            loadColorCardPromptManifest(context.applicationContext)
        }
    }
    val previewBitmaps by produceState(
        initialValue = emptyMap<String, Bitmap>(),
        key1 = context,
        key2 = manifest?.series?.sumOf { it.entries.size } ?: 0
    ) {
        val currentManifest = manifest ?: return@produceState
        val nextMap = linkedMapOf<String, Bitmap>()
        currentManifest.series
            .flatMap { it.entries }
            .forEachIndexed { index, entry ->
                loadColorCardPromptPreviewBitmap(context.applicationContext, entry)?.let { bitmap ->
                    nextMap[entry.id] = bitmap
                    value = nextMap.toMap()
                }
                if (index % 2 == 1) {
                    delay(6L)
                }
            }
    }
    val series = remember(manifest?.series, shuffleSeed) {
        organizeColorCardPromptSeries(
            source = manifest?.series.orEmpty(),
            shuffleSeed = shuffleSeed
        )
    }
    return ColorCardPromptFeedState(
        series = series,
        previewBitmaps = previewBitmaps,
        isLoading = manifest == null && series.isEmpty()
    )
}

private fun organizeColorCardPromptSeries(
    source: List<ColorCardPromptSeries>,
    shuffleSeed: Long
): List<ColorCardPromptSeries> {
    if (source.isEmpty()) return emptyList()
    val random = Random(shuffleSeed)
    return source
        .map { series -> series.copy(entries = series.entries.shuffled(random)) }
        .shuffled(random)
}

@Composable
internal fun ColorCardPromptSeriesRow(
    series: ColorCardPromptSeries,
    previewBitmaps: Map<String, Bitmap>,
    onPromptSelected: (ColorCardPromptEntry) -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.title(appLanguage),
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = series.subtitle(appLanguage),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(series.entries, key = { it.id }) { entry ->
                ColorCardPromptFeedCard(
                    entry = entry,
                    previewBitmap = previewBitmaps[entry.id],
                    onClick = { onPromptSelected(entry) }
                )
            }
        }
    }
}

@Composable
internal fun ModernColorCardPromptOnlyFeed(
    listState: LazyListState,
    state: ColorCardPromptFeedState,
    onPromptSelected: (ColorCardPromptEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading || state.series.isEmpty()) {
        ColorCardFilterFeedLoadingState(modifier = modifier)
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(state.series, key = { it.key }) { series ->
            ColorCardPromptSeriesRow(
                series = series,
                previewBitmaps = state.previewBitmaps,
                onPromptSelected = onPromptSelected
            )
        }
    }
}

@Composable
internal fun ModernColorCardTaggedPromptSectionRow(
    series: ColorCardPromptSeries,
    previewBitmaps: Map<String, Bitmap>,
    onPromptSelected: (ColorCardPromptEntry) -> Unit
) {
    val appLanguage = LocalAppLanguage.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ColorCardPromptSectionHeading(
            title = series.title(appLanguage),
            tagText = localizedColorCardPromptLabel(appLanguage)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(series.entries, key = { it.id }) { entry ->
                ColorCardPromptFeedCard(
                    entry = entry,
                    previewBitmap = previewBitmaps[entry.id],
                    onClick = { onPromptSelected(entry) }
                )
            }
        }
    }
}

@Composable
private fun ColorCardPromptSectionHeading(
    title: String,
    tagText: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dividerGap = 8.dp
    val tagColor = if (isDark) Color(0xFFE2B87A) else Color(0xFFA96818)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(dividerGap))
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(tagColor.copy(alpha = 0.92f))
        )
        Spacer(modifier = Modifier.width(dividerGap))
        Text(
            text = tagText,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = tagColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ColorCardPromptFeedCard(
    entry: ColorCardPromptEntry,
    previewBitmap: Bitmap?,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    val cardInteractionSource = remember { MutableInteractionSource() }
    val previewInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(COLOR_CARD_PROMPT_PREVIEW_WIDTH_DP.dp)
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(COLOR_CARD_PROMPT_PREVIEW_HEIGHT_DP.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = cardShape
                )
                .clickable(
                    interactionSource = previewInteractionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null && !previewBitmap.isRecycled) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = entry.zhTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.title(LocalAppLanguage.current),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
internal fun ColorCardPromptDetailPage(
    entry: ColorCardPromptEntry,
    onBack: () -> Unit,
    initialPreviewBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val colorCardPageCopy = remember(appLanguage) { getColorCardPageGlobalCopy(appLanguage) }
    val previewBitmap by produceState<Bitmap?>(initialValue = initialPreviewBitmap, key1 = context, key2 = entry.id) {
        val seededBitmap = initialPreviewBitmap
        if (seededBitmap != null && !seededBitmap.isRecycled) {
            value = seededBitmap
        } else {
            value = withContext(Dispatchers.IO) {
                loadColorCardPromptPreviewBitmap(context.applicationContext, entry)
            }
        }
    }
    val copyLabel = localizedColorCardPromptCopyLabel(appLanguage)
    val referenceLabel = localizedColorCardPromptReferenceLabel(appLanguage, entry.exampleMode)
    val editablePromptValues = remember(entry.id) {
        mutableStateMapOf<String, String>().apply {
            entry.editableSlots.forEach { slot ->
                this[slot.key] = ""
            }
        }
    }
    val editableValueSnapshot = entry.editableSlots.associate { slot ->
        slot.key to editablePromptValues[slot.key].orEmpty()
    }
    val promptToCopy = entry.resolvedPrompt(editableValueSnapshot)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = entry.title(appLanguage),
            onBack = onBack,
            trailing = {
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = copyLabel,
                    onClick = {
                        copyColorCardPromptText(
                            context = context,
                            label = copyLabel,
                            text = promptToCopy,
                            successMessage = colorCardPageCopy.copied
                        )
                    }
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (previewBitmap != null && !previewBitmap!!.isRecycled) {
                item {
                    val imageShape = RoundedCornerShape(26.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = entry.title(appLanguage),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.58f)
                                .widthIn(max = COLOR_CARD_PROMPT_DETAIL_IMAGE_MAX_WIDTH_DP.dp)
                                .aspectRatio(COLOR_CARD_PROMPT_DETAIL_ASPECT_RATIO)
                                .clip(imageShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = imageShape
                                )
                        )
                    }
                }
            }

            item {
                Text(
                    text = entry.summary(appLanguage),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorCardPromptMetaChip(
                        text = referenceLabel,
                        backgroundColor = colorCardPromptExampleContainerColor(entry.exampleMode, isSystemInDarkTheme()),
                        contentColor = colorCardPromptExampleContentColor(entry.exampleMode, isSystemInDarkTheme())
                    )
                    if (entry.platform.isNotBlank()) {
                        ColorCardPromptMetaChip(
                            text = localizedColorCardPromptSourceModelLabel(appLanguage, entry.platform)
                        )
                    }
                    entry.tags.filter { it.isNotBlank() }.forEach { tag ->
                        ColorCardPromptMetaChip(text = tag)
                    }
                }
            }

            item {
                ColorCardPromptCopyButton(
                    label = copyLabel,
                    onClick = {
                        copyColorCardPromptText(
                            context = context,
                            label = copyLabel,
                            text = promptToCopy,
                            successMessage = colorCardPageCopy.copied
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = localizedColorCardPromptBodyLabel(appLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                ColorCardPromptBodyCard(
                    entry = entry,
                    appLanguage = appLanguage,
                    editableValues = editableValueSnapshot,
                    onEditableValueChange = { key, value ->
                        editablePromptValues[key] = value
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorCardPromptBodyCard(
    entry: ColorCardPromptEntry,
    appLanguage: AppLanguage,
    editableValues: Map<String, String>,
    onEditableValueChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorCardPageCopy = remember(appLanguage) { getColorCardPageGlobalCopy(appLanguage) }
    var editingSlotKey by remember(entry.id) { mutableStateOf<String?>(null) }
    val editingSlot = remember(editingSlotKey, entry.editableSlots) {
        entry.editableSlots.firstOrNull { it.key == editingSlotKey }
    }
    val cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            shape = RoundedCornerShape(20.dp)
        )
        .padding(18.dp)
    if (!entry.supportsEditablePrompt) {
        SelectionContainer {
            Text(
                text = entry.prompt,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = cardModifier
            )
        }
        return
    }

    if (editingSlot != null) {
        var draftValue by remember(editingSlot.key, editableValues[editingSlot.key]) {
            mutableStateOf(editableValues[editingSlot.key].orEmpty())
        }
        AlertDialog(
            onDismissRequest = { editingSlotKey = null },
            title = {
                Text(text = editingSlot.label(appLanguage))
            },
            text = {
                ColorCardPromptSlotInputField(
                    value = draftValue,
                    placeholder = editingSlot.defaultValue.ifBlank { editingSlot.label(appLanguage) },
                    onValueChange = { draftValue = it }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditableValueChange(editingSlot.key, draftValue.trim())
                        editingSlotKey = null
                    }
                ) {
                    Text(text = localizedColorCardPromptEditConfirmLabel(appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSlotKey = null }) {
                    Text(text = colorCardPageCopy.cancel)
                }
            }
        )
    }

    Column(
        modifier = cardModifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ColorCardPromptEditablePreviewText(
            entry = entry,
            appLanguage = appLanguage,
            editableValues = editableValues,
            onSlotClick = { editingSlotKey = it }
        )
    }
}

private sealed interface ColorCardPromptEditablePreviewPart {
    data class TextSegment(val text: String) : ColorCardPromptEditablePreviewPart
    data class SlotSegment(val slot: ColorCardPromptEditableSlot) : ColorCardPromptEditablePreviewPart
}

@Composable
private fun ColorCardPromptEditablePreviewText(
    entry: ColorCardPromptEntry,
    appLanguage: AppLanguage,
    editableValues: Map<String, String>,
    onSlotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parts = remember(entry.promptTemplate, entry.editableSlots) {
        val slotLookup = entry.editableSlots.associateBy { it.key }
        buildList {
            var lastIndex = 0
            colorCardPromptTemplateSlotRegex.findAll(entry.promptTemplate).forEach { match ->
                val leadingText = entry.promptTemplate.substring(lastIndex, match.range.first)
                if (leadingText.isNotEmpty()) {
                    add(ColorCardPromptEditablePreviewPart.TextSegment(leadingText))
                }
                val slotKey = match.groupValues[1]
                val slot = slotLookup[slotKey]
                if (slot != null) {
                    add(ColorCardPromptEditablePreviewPart.SlotSegment(slot))
                } else {
                    add(ColorCardPromptEditablePreviewPart.TextSegment(match.value))
                }
                lastIndex = match.range.last + 1
            }
            val trailingText = entry.promptTemplate.substring(lastIndex)
            if (trailingText.isNotEmpty()) {
                add(ColorCardPromptEditablePreviewPart.TextSegment(trailingText))
            }
        }
    }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        parts.forEach { part ->
            when (part) {
                is ColorCardPromptEditablePreviewPart.TextSegment -> {
                    Text(
                        text = part.text,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is ColorCardPromptEditablePreviewPart.SlotSegment -> {
                    val slot = part.slot
                    val currentValue = editableValues[slot.key].orEmpty().trim()
                    val displayText = currentValue.ifBlank {
                        slot.defaultValue.ifBlank { slot.label(appLanguage) }
                    }
                    ColorCardPromptEditableToken(
                        text = displayText,
                        isCustomized = currentValue.isNotBlank(),
                        onClick = { onSlotClick(slot.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCardPromptEditableToken(
    text: String,
    isCustomized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isCustomized) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f)
    }
    val contentColor = if (isCustomized) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = if (isCustomized) 0.28f else 0.18f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = if (isCustomized) FontWeight.Medium else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
private fun ColorCardPromptSlotInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 26.sp,
            textAlign = TextAlign.Start
        ),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ColorCardPromptMetaChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 6.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = contentColor
        )
    }
}

@Composable
private fun ColorCardPromptCopyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.18f else 0.08f),
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
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun copyColorCardPromptText(
    context: Context,
    label: String,
    text: String,
    successMessage: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
}

private fun loadColorCardPromptManifest(context: Context): ColorCardPromptManifest {
    cachedColorCardPromptManifest?.let { return it }
    synchronized(colorCardPromptManifestLock) {
        cachedColorCardPromptManifest?.let { return it }
        val loaded = runCatching {
            context.assets.open(COLOR_CARD_PROMPT_MANIFEST_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { reader ->
                    parseColorCardPromptManifest(
                        JSONObject(reader.readText().removePrefix("\uFEFF"))
                    )
                }
        }.getOrElse { ColorCardPromptManifest(emptyList()) }
        cachedColorCardPromptManifest = loaded
        return loaded
    }
}

private fun parseColorCardPromptManifest(root: JSONObject): ColorCardPromptManifest {
    val categories = linkedMapOf<String, ColorCardPromptCategory>()
    val categoriesArray = root.optJSONArray("categories")
    for (index in 0 until (categoriesArray?.length() ?: 0)) {
        val categoryObject = categoriesArray?.optJSONObject(index) ?: continue
        val key = categoryObject.optString("key").trim()
        if (key.isBlank()) continue
        categories[key] = ColorCardPromptCategory(
            key = key,
            order = categoryObject.optInt("order", index + 1),
            zhTitle = categoryObject.optString("zh_title").trim(),
            enTitle = categoryObject.optString("en_title").trim(),
            zhSubtitle = categoryObject.optString("zh_subtitle").trim(),
            enSubtitle = categoryObject.optString("en_subtitle").trim()
        )
    }

    val entries = ArrayList<ColorCardPromptEntry>()
    entries += parseColorCardPromptManifestEntries(root.optJSONArray("entries"))
    entries += parseColorCardPromptManifestEntries(root.optJSONArray("draft_entries"))

    val series = categories.values
        .sortedBy { it.order }
        .mapNotNull { category ->
            val categoryEntries = entries
                .filter { it.categoryKey == category.key }
                .sortedBy { it.id }
            if (categoryEntries.isEmpty()) {
                null
            } else {
                ColorCardPromptSeries(
                    key = category.key,
                    category = category,
                    entries = categoryEntries
                )
            }
        }

    return ColorCardPromptManifest(series = series)
}

private fun parseColorCardPromptManifestEntries(entriesArray: JSONArray?): List<ColorCardPromptEntry> {
    val entries = ArrayList<ColorCardPromptEntry>()
    for (index in 0 until (entriesArray?.length() ?: 0)) {
        val entryObject = entriesArray?.optJSONObject(index) ?: continue
        val id = entryObject.optString("id").trim()
        val imageKey = entryObject.optString("image_key").trim()
        val categoryKey = entryObject.optString("category_key").trim()
        val prompt = entryObject.optString("prompt").trim()
        if (id.isBlank() || imageKey.isBlank() || categoryKey.isBlank() || prompt.isBlank()) continue
        val tagArray = entryObject.optJSONArray("tags")
        val tags = buildList {
            for (tagIndex in 0 until (tagArray?.length() ?: 0)) {
                val tag = tagArray?.optString(tagIndex).orEmpty().trim()
                if (tag.isNotBlank()) {
                    add(tag)
                }
            }
        }
        entries += ColorCardPromptEntry(
            id = id,
            imageKey = imageKey,
            categoryKey = categoryKey,
            zhTitle = entryObject.optString("zh_title").trim(),
            enTitle = entryObject.optString("en_title").trim(),
            zhSummary = entryObject.optString("zh_summary").trim(),
            enSummary = entryObject.optString("en_summary").trim(),
            platform = entryObject.optString("platform").trim(),
            exampleMode = parseColorCardPromptExampleMode(entryObject.optString("example")),
            tags = tags,
            prompt = prompt,
            promptTemplate = entryObject.optString("prompt_template").trim(),
            editableSlots = parseColorCardPromptEditableSlots(entryObject)
        )
    }
    return entries
}

private fun parseColorCardPromptEditableSlots(entryObject: JSONObject): List<ColorCardPromptEditableSlot> {
    val slotArray = entryObject.optJSONArray("editable_slots") ?: return emptyList()
    return buildList {
        for (slotIndex in 0 until slotArray.length()) {
            val slotObject = slotArray.optJSONObject(slotIndex) ?: continue
            val key = slotObject.optString("key").trim()
            val zhLabel = slotObject.optString("zh_label").trim()
            val enLabel = slotObject.optString("en_label").trim()
            if (key.isBlank() || (zhLabel.isBlank() && enLabel.isBlank())) continue
            add(
                ColorCardPromptEditableSlot(
                    key = key,
                    zhLabel = zhLabel,
                    enLabel = enLabel,
                    defaultValue = slotObject.optString("default_value").trim()
                )
            )
        }
    }
}

private fun loadColorCardPromptPreviewBitmap(
    context: Context,
    entry: ColorCardPromptEntry
): Bitmap? = loadColorCardPromptBitmap(context, entry.previewImageAssetName)

private fun loadColorCardPromptBitmap(
    context: Context,
    assetName: String
): Bitmap? {
    synchronized(colorCardPromptBitmapCacheLock) {
        colorCardPromptBitmapCache[assetName]?.let { return it }
    }
    val decoded = runCatching {
        context.assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            )
        }
    }.getOrNull() ?: return null
    synchronized(colorCardPromptBitmapCacheLock) {
        colorCardPromptBitmapCache[assetName] = decoded
    }
    return decoded
}
