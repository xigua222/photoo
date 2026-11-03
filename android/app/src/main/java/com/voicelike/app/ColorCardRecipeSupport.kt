package com.voicelike.app

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Base64
import java.util.Locale
import java.util.UUID

private val colorCardRecipeGson = Gson()
private const val COLOR_CARD_RECIPE_TOKEN_PREFIX = "PHOTOO_RECIPE_V1:"

internal enum class ColorCardRecipeMode {
    Filter,
    Trace
}

internal data class SavedColorCardRecipe(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String,
    @SerializedName(value = "createdAt", alternate = ["b"])
    val createdAt: Long,
    @SerializedName(value = "mode", alternate = ["c"])
    val mode: ColorCardRecipeMode,
    @SerializedName(value = "title", alternate = ["d"])
    val title: String,
    @SerializedName(value = "paletteId", alternate = ["e"])
    val paletteId: String,
    @SerializedName(value = "candidateKind", alternate = ["f"])
    val candidateKind: String,
    @SerializedName(value = "candidateTitle", alternate = ["g"])
    val candidateTitle: String,
    @SerializedName(value = "sourceTag", alternate = ["h"])
    val sourceTag: String?,
    @SerializedName(value = "colors", alternate = ["i"])
    val colors: List<Int>,
    @SerializedName(value = "color", alternate = ["j"])
    val color: Float,
    @SerializedName(value = "tone", alternate = ["k"])
    val tone: Float,
    @SerializedName(value = "sharpen", alternate = ["l"])
    val sharpen: Float,
    @SerializedName(value = "saturation", alternate = ["m"])
    val saturation: Float,
    @SerializedName(value = "brightness", alternate = ["n"])
    val brightness: Float,
    @SerializedName(value = "paletteStrength", alternate = ["o"])
    val paletteStrength: Float,
    @SerializedName(value = "portraitVitality", alternate = ["p"])
    val portraitVitality: Float = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
    @SerializedName(value = "portraitSoftness", alternate = ["q"])
    val portraitSoftness: Float = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
    @SerializedName(value = "portraitDetailSuppression", alternate = ["r"])
    val portraitDetailSuppression: Float = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
    @SerializedName(value = "filmGrainEnabled", alternate = ["s"])
    val filmGrainEnabled: Boolean = false,
    @SerializedName(value = "filmGrainAssetId", alternate = ["t"])
    val filmGrainAssetId: String? = null
)

private data class ColorCardRecipeEnvelope(
    @SerializedName(value = "version", alternate = ["b"])
    val version: Int = 1,
    @SerializedName(value = "recipes", alternate = ["a"])
    val recipes: List<SavedColorCardRecipePayload>
)

private data class SavedColorCardRecipePayload(
    @SerializedName(value = "mode", alternate = ["a"])
    val mode: String,
    @SerializedName(value = "title", alternate = ["b"])
    val title: String?,
    @SerializedName(value = "paletteId", alternate = ["c"])
    val paletteId: String?,
    @SerializedName(value = "candidateKind", alternate = ["d"])
    val candidateKind: String?,
    @SerializedName(value = "candidateTitle", alternate = ["e"])
    val candidateTitle: String?,
    @SerializedName(value = "sourceTag", alternate = ["f"])
    val sourceTag: String?,
    @SerializedName(value = "colors", alternate = ["g"])
    val colors: List<Int>?,
    @SerializedName(value = "color", alternate = ["h"])
    val color: Float?,
    @SerializedName(value = "tone", alternate = ["i"])
    val tone: Float?,
    @SerializedName(value = "sharpen", alternate = ["j"])
    val sharpen: Float?,
    @SerializedName(value = "saturation", alternate = ["k"])
    val saturation: Float?,
    @SerializedName(value = "brightness", alternate = ["l"])
    val brightness: Float?,
    @SerializedName(value = "paletteStrength", alternate = ["m"])
    val paletteStrength: Float?,
    @SerializedName(value = "portraitVitality", alternate = ["n"])
    val portraitVitality: Float? = null,
    @SerializedName(value = "portraitSoftness", alternate = ["o"])
    val portraitSoftness: Float? = null,
    @SerializedName(value = "portraitDetailSuppression", alternate = ["p"])
    val portraitDetailSuppression: Float? = null,
    @SerializedName(value = "filmGrainEnabled", alternate = ["q"])
    val filmGrainEnabled: Boolean? = null,
    @SerializedName(value = "filmGrainAssetId", alternate = ["r"])
    val filmGrainAssetId: String? = null
)

internal class ColorCardRecipeStore private constructor(
    private val appContext: Context
) {
    companion object {
        private const val TAG = "ColorCardRecipeStore"
        private const val MAX_RECIPES = 240

        @Volatile
        private var instance: ColorCardRecipeStore? = null

        fun get(context: Context): ColorCardRecipeStore {
            return instance ?: synchronized(this) {
                instance ?: ColorCardRecipeStore(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val storageFile = File(appContext.filesDir, "color_card_recipes.json")
    private val tempFile = File(appContext.filesDir, "color_card_recipes.json.tmp")
    private val backupFile = File(appContext.filesDir, "color_card_recipes.json.bak")
    private val _recipes = MutableStateFlow<List<SavedColorCardRecipe>>(emptyList())
    private var loaded = false

    val recipes: StateFlow<List<SavedColorCardRecipe>> = _recipes.asStateFlow()

    init {
        scope.launch {
            ensureLoaded()
        }
    }

    suspend fun ensureLoaded() {
        mutex.withLock {
            if (loaded) return
            _recipes.value = loadFromDisk()
            loaded = true
        }
    }

    suspend fun saveRecipe(
        recipe: SavedColorCardRecipe
    ): SavedColorCardRecipe? {
        val normalized = normalizeColorCardRecipe(recipe)
        return mutex.withLock {
            if (!loaded) {
                _recipes.value = loadFromDisk()
                loaded = true
            }
            val updated = mergeRecipes(_recipes.value, listOf(normalized))
            if (!persist(updated)) return@withLock null
            _recipes.value = updated
            normalized
        }
    }

    suspend fun saveImportedRecipes(
        recipes: List<SavedColorCardRecipe>
    ): Int {
        val normalized = recipes.map(::normalizeColorCardRecipe)
        if (normalized.isEmpty()) return 0
        return mutex.withLock {
            if (!loaded) {
                _recipes.value = loadFromDisk()
                loaded = true
            }
            val updated = mergeRecipes(_recipes.value, normalized)
            if (!persist(updated)) return@withLock 0
            _recipes.value = updated
            normalized.size
        }
    }

    suspend fun deleteRecipe(
        recipeId: String
    ): Boolean {
        val normalizedId = recipeId.trim()
        if (normalizedId.isBlank()) return false
        return mutex.withLock {
            if (!loaded) {
                _recipes.value = loadFromDisk()
                loaded = true
            }
            val updated = _recipes.value.filterNot { recipe -> recipe.id == normalizedId }
            if (updated.size == _recipes.value.size) return@withLock false
            if (!persist(updated)) return@withLock false
            _recipes.value = updated
            true
        }
    }

    private fun loadFromDisk(): List<SavedColorCardRecipe> {
        val candidates = listOf(storageFile, tempFile, backupFile)
        for (file in candidates) {
            if (!file.exists()) continue
            runCatching {
                parseColorCardRecipeSnapshot(file.readText(Charsets.UTF_8))
            }.onSuccess { recipes ->
                return recipes
            }.onFailure { error ->
                Log.e(TAG, "Failed to load recipes from ${file.name}", error)
            }
        }
        return emptyList()
    }

    private fun persist(
        recipes: List<SavedColorCardRecipe>
    ): Boolean {
        return runCatching {
            val json = serializeColorCardRecipeSnapshot(recipes)
            tempFile.writeText(json, Charsets.UTF_8)
            if (storageFile.exists()) {
                storageFile.copyTo(backupFile, overwrite = true)
            }
            tempFile.copyTo(storageFile, overwrite = true)
            storageFile.copyTo(backupFile, overwrite = true)
            tempFile.delete()
            true
        }.onFailure { error ->
            Log.e(TAG, "Failed to persist recipes", error)
        }.getOrDefault(false)
    }

    private fun mergeRecipes(
        existing: List<SavedColorCardRecipe>,
        incoming: List<SavedColorCardRecipe>
    ): List<SavedColorCardRecipe> {
        val existingBySignature = existing.associateBy(::colorCardRecipeSignature).toMutableMap()
        incoming.forEach { recipe ->
            existingBySignature[colorCardRecipeSignature(recipe)] = recipe
        }
        return existingBySignature.values
            .sortedByDescending(SavedColorCardRecipe::createdAt)
            .take(MAX_RECIPES)
    }
}

internal fun buildColorCardTraceRecipeFromCurrent(
    mode: ColorCardTracePanelMode,
    traceState: ColorCardTraceFilterState
): SavedColorCardRecipe? {
    if (mode != ColorCardTracePanelMode.Filter && mode != ColorCardTracePanelMode.Trace) return null
    val candidate = traceState.selectedCandidate ?: return null
    val recipeMode = if (mode == ColorCardTracePanelMode.Filter) {
        ColorCardRecipeMode.Filter
    } else {
        ColorCardRecipeMode.Trace
    }
    val baseTitle = candidate.title.ifBlank {
        candidate.sourceTag?.takeIf { it.isNotBlank() }
            ?: if (recipeMode == ColorCardRecipeMode.Filter) "滤镜配方" else "追色配方"
    }
    return normalizeColorCardRecipe(
        SavedColorCardRecipe(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            mode = recipeMode,
            title = "${if (recipeMode == ColorCardRecipeMode.Filter) "滤镜" else "追色"} · $baseTitle",
            paletteId = candidate.paletteId,
            candidateKind = candidate.kind.name,
            candidateTitle = candidate.title,
            sourceTag = candidate.sourceTag,
            colors = candidate.colors.take(ColorCardStore.MAX_PALETTE_COLOR_COUNT),
            color = traceState.color,
            tone = traceState.tone,
            sharpen = traceState.sharpen,
            saturation = traceState.saturation,
            brightness = traceState.brightness,
            paletteStrength = traceState.paletteStrength,
            portraitVitality = traceState.portraitAdjustments.vitality,
            portraitSoftness = traceState.portraitAdjustments.softness,
            portraitDetailSuppression = traceState.portraitAdjustments.detailSuppression,
            filmGrainEnabled = traceState.filmGrainEnabled,
            filmGrainAssetId = traceState.selectedFilmGrainAsset?.id
        )
    )
}

internal fun buildColorCardRecipeShareText(
    recipes: List<SavedColorCardRecipe>
): String {
    val normalized = recipes.map(::normalizeColorCardRecipe)
    val envelope = ColorCardRecipeEnvelope(
        recipes = normalized.map(::recipeToPayload)
    )
    val token = encodeColorCardRecipeEnvelopeToken(envelope)
    return buildString {
        append("PHOTOO 配方")
        append('\n')
        append("数量: ")
        append(normalized.size)
        normalized.take(6).forEach { recipe ->
            append('\n')
            append('\n')
            append(recipe.title)
            append('\n')
            append("模式: ")
            append(if (recipe.mode == ColorCardRecipeMode.Filter) "滤镜" else "追色")
            append('\n')
            append("目标: ")
            append(recipe.candidateTitle)
            recipe.sourceTag?.takeIf { it.isNotBlank() }?.let { sourceTag ->
                append(" · ")
                append(sourceTag)
            }
        }
        append('\n')
        append('\n')
        append(COLOR_CARD_RECIPE_TOKEN_PREFIX)
        append(token)
    }
}

internal fun parseSharedColorCardRecipes(
    rawText: String
): List<SavedColorCardRecipe> {
    val envelope = parseColorCardRecipeEnvelope(rawText)
    if (envelope.recipes.isEmpty()) {
        error("No recipes found")
    }
    return envelope.recipes.map(::payloadToRecipe)
}

internal suspend fun importSharedColorCardRecipes(
    rawText: String,
    recipeStore: ColorCardRecipeStore,
    paletteStore: ColorCardStore
): Int {
    val imported = parseSharedColorCardRecipes(rawText)
    paletteStore.ensureLoaded()
    val normalized = imported.map { recipe ->
        if (recipe.mode != ColorCardRecipeMode.Trace || recipe.colors.isEmpty()) {
            recipe
        } else {
            val existingPalette = paletteStore.palettes.value.firstOrNull { palette ->
                palette.colors == recipe.colors &&
                    palette.title == recipe.candidateTitle &&
                    palette.sourceTag == recipe.sourceTag
            }
            val savedPalette = existingPalette ?: paletteStore.savePalette(
                colors = recipe.colors,
                sourceMediaId = null,
                title = recipe.candidateTitle,
                sourceTag = recipe.sourceTag
            )
            recipe.copy(paletteId = savedPalette?.id ?: recipe.paletteId)
        }
    }
    return recipeStore.saveImportedRecipes(normalized)
}

internal fun colorCardRecipeModeToPanelMode(
    mode: ColorCardRecipeMode
): ColorCardTracePanelMode {
    return when (mode) {
        ColorCardRecipeMode.Filter -> ColorCardTracePanelMode.Filter
        ColorCardRecipeMode.Trace -> ColorCardTracePanelMode.Trace
    }
}

private fun recipeToPayload(
    recipe: SavedColorCardRecipe
): SavedColorCardRecipePayload {
    return SavedColorCardRecipePayload(
        mode = recipe.mode.name,
        title = recipe.title,
        paletteId = recipe.paletteId,
        candidateKind = recipe.candidateKind,
        candidateTitle = recipe.candidateTitle,
        sourceTag = recipe.sourceTag,
        colors = recipe.colors,
        color = recipe.color,
        tone = recipe.tone,
        sharpen = recipe.sharpen,
        saturation = recipe.saturation,
        brightness = recipe.brightness,
        paletteStrength = recipe.paletteStrength,
        portraitVitality = recipe.portraitVitality,
        portraitSoftness = recipe.portraitSoftness,
        portraitDetailSuppression = recipe.portraitDetailSuppression,
        filmGrainEnabled = recipe.filmGrainEnabled,
        filmGrainAssetId = recipe.filmGrainAssetId
    )
}

private fun payloadToRecipe(
    payload: SavedColorCardRecipePayload
): SavedColorCardRecipe {
    val mode = runCatching { ColorCardRecipeMode.valueOf(payload.mode) }
        .getOrDefault(ColorCardRecipeMode.Trace)
    return normalizeColorCardRecipe(
        SavedColorCardRecipe(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            mode = mode,
            title = payload.title?.trim().orEmpty().ifBlank {
                if (mode == ColorCardRecipeMode.Filter) "滤镜配方" else "追色配方"
            },
            paletteId = payload.paletteId?.trim().orEmpty(),
            candidateKind = payload.candidateKind?.trim().orEmpty()
                .ifBlank { ColorCardTraceFilterKind.Palette.name },
            candidateTitle = payload.candidateTitle?.trim().orEmpty().ifBlank { "未命名目标" },
            sourceTag = payload.sourceTag?.trim().orEmpty().ifBlank { null },
            colors = payload.colors.orEmpty(),
            color = payload.color ?: 0f,
            tone = payload.tone ?: 0f,
            sharpen = payload.sharpen ?: 0f,
            saturation = payload.saturation ?: 0f,
            brightness = payload.brightness ?: 0f,
            paletteStrength = payload.paletteStrength ?: 0.5f,
            portraitVitality = payload.portraitVitality ?: COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
            portraitSoftness = payload.portraitSoftness ?: COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
            portraitDetailSuppression = payload.portraitDetailSuppression ?: COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
            filmGrainEnabled = payload.filmGrainEnabled == true,
            filmGrainAssetId = payload.filmGrainAssetId?.trim().orEmpty().ifBlank { null }
        )
    )
}

private fun normalizeColorCardRecipe(
    recipe: SavedColorCardRecipe
): SavedColorCardRecipe {
    return recipe.copy(
        id = recipe.id.ifBlank { UUID.randomUUID().toString() },
        createdAt = recipe.createdAt.coerceAtLeast(1L),
        title = recipe.title.trim().ifBlank {
            if (recipe.mode == ColorCardRecipeMode.Filter) "滤镜配方" else "追色配方"
        },
        paletteId = recipe.paletteId.trim(),
        candidateKind = recipe.candidateKind.trim().ifBlank { ColorCardTraceFilterKind.Palette.name },
        candidateTitle = recipe.candidateTitle.trim().ifBlank { "未命名目标" },
        sourceTag = recipe.sourceTag?.trim().orEmpty().ifBlank { null },
        colors = recipe.colors.take(ColorCardStore.MAX_PALETTE_COLOR_COUNT),
        color = recipe.color.coerceIn(-1f, 1f),
        tone = recipe.tone.coerceIn(-1f, 1f),
        sharpen = recipe.sharpen.coerceIn(-1f, 1f),
        saturation = recipe.saturation.coerceIn(-1f, 1f),
        brightness = recipe.brightness.coerceIn(-1f, 1f),
        paletteStrength = recipe.paletteStrength.coerceIn(0f, 1f),
        portraitVitality = recipe.portraitVitality.coerceIn(0f, 1f),
        portraitSoftness = recipe.portraitSoftness.coerceIn(0f, 1f),
        portraitDetailSuppression = recipe.portraitDetailSuppression.coerceIn(0f, 1f),
        filmGrainEnabled = recipe.filmGrainEnabled,
        filmGrainAssetId = recipe.filmGrainAssetId?.trim().orEmpty().ifBlank { null }
    )
}

private fun colorCardRecipeSignature(
    recipe: SavedColorCardRecipe
): String {
    return buildString {
        append(recipe.mode.name)
        append('|')
        append(recipe.paletteId)
        append('|')
        append(recipe.candidateKind)
        append('|')
        append(recipe.colors.joinToString(","))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.color))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.tone))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.sharpen))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.saturation))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.brightness))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.paletteStrength))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.portraitVitality))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.portraitSoftness))
        append('|')
        append(String.format(Locale.US, "%.2f", recipe.portraitDetailSuppression))
        append('|')
        append(recipe.filmGrainEnabled)
        append('|')
        append(recipe.filmGrainAssetId.orEmpty())
    }
}

private fun serializeColorCardRecipeSnapshot(
    recipes: List<SavedColorCardRecipe>
): String {
    return colorCardRecipeGson.toJson(
        recipes
            .map(::normalizeColorCardRecipe)
            .sortedByDescending(SavedColorCardRecipe::createdAt)
    )
}

private fun parseColorCardRecipeSnapshot(
    rawJson: String
): List<SavedColorCardRecipe> {
    val array = colorCardRecipeGson.fromJson(rawJson, Array<SavedColorCardRecipe>::class.java)
    return array.orEmpty()
        .map(::normalizeColorCardRecipe)
        .sortedByDescending(SavedColorCardRecipe::createdAt)
}

private fun encodeColorCardRecipeEnvelopeToken(
    envelope: ColorCardRecipeEnvelope
): String {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(colorCardRecipeGson.toJson(envelope).toByteArray(Charsets.UTF_8))
}

private fun parseColorCardRecipeEnvelope(
    rawText: String
): ColorCardRecipeEnvelope {
    extractColorCardRecipeTokenCandidates(rawText).forEach { token ->
        decodeColorCardRecipeEnvelopeTokenOrNull(token)?.let { envelope ->
            return envelope
        }
    }

    extractColorCardRecipeEnvelopeJson(rawText)?.let { json ->
        return colorCardRecipeGson.fromJson(json, ColorCardRecipeEnvelope::class.java)
    }

    error("No valid recipe token or JSON found")
}

private fun extractColorCardRecipeTokenCandidates(
    rawText: String
): List<String> {
    val candidates = linkedSetOf<String>()
    val prefixedPattern = Regex("${Regex.escape(COLOR_CARD_RECIPE_TOKEN_PREFIX)}\\s*([A-Za-z0-9_\\-=\\s]{16,})")
    prefixedPattern.findAll(rawText).forEach { match ->
        match.groupValues.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(candidates::add)
    }
    rawText.trim()
        .takeIf { it.startsWith(COLOR_CARD_RECIPE_TOKEN_PREFIX) }
        ?.removePrefix(COLOR_CARD_RECIPE_TOKEN_PREFIX)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let(candidates::add)
    val rawTokenPattern = Regex("(?<![A-Za-z0-9_\\-=])([A-Za-z0-9_\\-]{24,}={0,2})(?![A-Za-z0-9_\\-=])")
    rawTokenPattern.findAll(rawText).forEach { match ->
        match.groupValues.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(candidates::add)
    }
    return candidates.toList()
}

private fun decodeColorCardRecipeEnvelopeTokenOrNull(
    token: String
): ColorCardRecipeEnvelope? {
    val compact = token
        .replace(Regex("[\\u200B-\\u200D\\uFEFF\\s]"), "")
        .trim()
    if (compact.isBlank()) return null

    for (endExclusive in compact.length downTo 24) {
        val candidate = compact.substring(0, endExclusive)
        val normalizedToken = runCatching {
            normalizeColorCardRecipeToken(candidate)
        }.getOrNull() ?: continue
        val json = runCatching {
            String(Base64.getUrlDecoder().decode(normalizedToken), Charsets.UTF_8)
        }.getOrNull() ?: continue
        val envelope = runCatching {
            colorCardRecipeGson.fromJson(json, ColorCardRecipeEnvelope::class.java)
        }.getOrNull() ?: continue
        if (envelope.recipes.isNotEmpty()) {
            return envelope
        }
    }
    return null
}

private fun normalizeColorCardRecipeToken(
    token: String
): String {
    val compact = token
        .replace(Regex("[\\u200B-\\u200D\\uFEFF\\s]"), "")
        .trim()
    if (compact.isBlank()) error("Recipe token is empty")
    val paddingNeeded = (4 - (compact.length % 4)) % 4
    return compact + "=".repeat(paddingNeeded)
}

private fun extractColorCardRecipeEnvelopeJson(
    rawText: String
): String? {
    val startIndex = rawText.indexOf('{')
    val endIndex = rawText.lastIndexOf('}')
    if (startIndex < 0 || endIndex <= startIndex) return null
    val candidate = rawText.substring(startIndex, endIndex + 1).trim()
    return candidate.takeIf { json ->
        json.contains("\"recipes\"") && json.contains("\"version\"")
    }
}
