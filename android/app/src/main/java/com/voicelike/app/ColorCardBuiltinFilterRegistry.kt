package com.voicelike.app

import java.util.Locale
import kotlin.math.abs

internal data class ColorCardBuiltinHaldIdentity(
    val canonicalId: String,
    val displayCode: String,
    val sourceTag: String?,
    val familyLabel: String? = null,
    val variantLabel: String? = null,
    val variantValue: Int? = null
)

internal data class ColorCardTraceHaldVariant(
    val assetName: String,
    val haldLevel: Int,
    val previewColors: List<Int>,
    val position: Float,
    val variantLabel: String? = null
)

internal data class ColorCardTraceResolvedHaldBlend(
    val primaryAssetName: String?,
    val primaryHaldLevel: Int,
    val secondaryAssetName: String?,
    val secondaryHaldLevel: Int,
    val variantMix: Float
)

private data class ColorCardFilmSimFamily(
    val canonicalId: String,
    val familyLabel: String,
    val displayCode: String
)

private val COLOR_CARD_BUILTIN_HALD_DISPLAY_CODES = mapOf(
    "amy" to "#401A",
    "cms" to "#402A",
    "cinematic" to "#301C",
    "cinematicx-rec-709" to "#302C",
    "cinematicx-rec-709-2" to "#303C",
    "cinematicx-rec-709-3" to "#304C",
    "cinematicx-rec-709-4" to "#305C",
    "cinematicx-rec-709-5" to "#306C",
    "cinematicx-rec-709-6" to "#307C",
    "claire" to "#403A",
    "fg-cinebright" to "#308C",
    "fg-cinevibrant1" to "#309C",
    "fuj-07-velvia" to "#209F",
    "film-boost-10" to "#210F",
    "film-tone-3" to "#211F",
    "generate-by-resolve" to "#310C",
    "generate-by-resolve-2" to "#311C",
    "generate-by-resolve-3" to "#312C",
    "generated-by-3d-lut-creator-pro-c-oleg-sharonov" to "#404A",
    "hklike" to "#405A",
    "m-fahri-100c-negative" to "#212F",
    "moewarm" to "#406A",
    "ogs" to "#407A",
    "presetpro-rich-tines" to "#230F",
    "presetpro-santorini" to "#231F",
    "cc" to "#232F",
    "cc1" to "#233F",
    "cc2" to "#234F",
    "presetpro-sunset" to "#213F",
    "print-film-2" to "#214F",
    "converted-with-color" to "#408A",
    "j-fresh-bright-1" to "#501J",
    "j-fresh-bright-2" to "#502J",
    "j-fresh-bright-3" to "#503J",
    "j-fresh-bright-4" to "#504J",
    "j-fresh-bright-5" to "#505J",
    "j-fresh-bright-6" to "#506J",
    "j-fresh-bright-7" to "#507J",
    "j-fresh-bright-8" to "#508J",
    "astia" to "#215F",
    "bleach-bypass" to "#313C",
    "cinema3" to "#314C",
    "classic-chrome" to "#216F",
    "classic-neg" to "#217F",
    "eterna" to "#218F",
    "fj" to "#219F",
    "moeot" to "#409A",
    "nostalgic-neg" to "#220F",
    "polaroid-669" to "#221F",
    "polaroid-690" to "#222F",
    "polaroid-polachrome" to "#223F",
    "polaroid-px-680-warm" to "#224F",
    "pro-neg-hi" to "#225F",
    "pro-neg-std" to "#226F",
    "provia" to "#227F",
    "reala-ace" to "#228F",
    "velvia" to "#229F",
    "film-sim-5219" to "#201F",
    "film-sim-c200" to "#202F",
    "film-sim-ektar100" to "#203F",
    "film-sim-pro400h" to "#204F",
    "film-sim-spgold200" to "#205F",
    "film-sim-tx400" to "#206F",
    "film-sim-vivid250d" to "#207F",
    "film-sim-allround400" to "#208F"
)

private val COLOR_CARD_PIXEL_POSTER_DISPLAY_CODES = mapOf(
    "pixel-yellow" to "#101P",
    "pixel-red" to "#102P",
    "pixel-blue" to "#103P",
    "pixel-gray" to "#104P",
    "pixel-primary-yellow" to "#105P",
    "pixel-primary-blue" to "#106P",
    "pixel-primary-red" to "#107P",
    "pixel-original" to "#108P",
    "pixel-beads" to "#109P"
)

private val COLOR_CARD_FILM_SIM_FAMILIES = mapOf(
    "5219" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-5219",
        familyLabel = "5219",
        displayCode = "#201F"
    ),
    "c200" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-c200",
        familyLabel = "C200",
        displayCode = "#202F"
    ),
    "ektar100" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-ektar100",
        familyLabel = "Ektar100",
        displayCode = "#203F"
    ),
    "pro400h" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-pro400h",
        familyLabel = "PRO400H",
        displayCode = "#204F"
    ),
    "spgold200" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-spgold200",
        familyLabel = "SPGOLD200",
        displayCode = "#205F"
    ),
    "tx400" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-tx400",
        familyLabel = "Tx400",
        displayCode = "#206F"
    ),
    "vivid250d" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-vivid250d",
        familyLabel = "vivid250D",
        displayCode = "#207F"
    ),
    "全能400" to ColorCardFilmSimFamily(
        canonicalId = "film-sim-allround400",
        familyLabel = "全能400",
        displayCode = "#208F"
    )
)

private val COLOR_CARD_FILM_SIM_VARIANT_REGEX = Regex(
    pattern = "^(5219|C200|Ektar100|PRO400H|SPGOLD200|Tx400|vivid250D|全能400)((?:[+-]\\d+)|(?:[+-]?[sS]))$",
    option = RegexOption.IGNORE_CASE
)

private val COLOR_CARD_REGISTRY_FILM_KEYWORDS = listOf(
    "fuji",
    "fuj",
    "velvia",
    "astia",
    "classic chrome",
    "classic neg",
    "eterna",
    "nostalgic neg",
    "pro neg",
    "provia",
    "reala ace",
    "polaroid",
    "negative",
    "polachrome",
    "film"
)

private val COLOR_CARD_REGISTRY_CINEMA_KEYWORDS = listOf(
    "cinematic",
    "cinema",
    "cine",
    "bleach bypass",
    "resolve"
)

internal fun resolveColorCardBuiltinHaldIdentity(
    rawId: String,
    title: String,
    sourceTag: String?
): ColorCardBuiltinHaldIdentity {
    if (rawId.startsWith("j-fresh-bright-")) {
        return ColorCardBuiltinHaldIdentity(
            canonicalId = rawId,
            displayCode = COLOR_CARD_BUILTIN_HALD_DISPLAY_CODES[rawId]
                ?: buildColorCardBuiltinFallbackDisplayCode(rawId, 'J'),
            sourceTag = "清新透亮"
        )
    }
    if (rawId.startsWith("classic-memory-")) {
        return ColorCardBuiltinHaldIdentity(
            canonicalId = rawId,
            displayCode = buildColorCardClassicMemoryDisplayCode(title),
            sourceTag = "经典记忆"
        )
    }
    if (rawId.startsWith("black-white-era-")) {
        return ColorCardBuiltinHaldIdentity(
            canonicalId = rawId,
            displayCode = buildColorCardBlackWhiteEraDisplayCode(rawId),
            sourceTag = "黑白时代"
        )
    }
    parseColorCardFilmSimVariant(title)?.let { parsed ->
        return ColorCardBuiltinHaldIdentity(
            canonicalId = parsed.family.canonicalId,
            displayCode = parsed.family.displayCode,
            sourceTag = "胶片模拟",
            familyLabel = parsed.family.familyLabel,
            variantLabel = parsed.variantLabel,
            variantValue = parsed.variantValue
        )
    }

    val canonicalId = rawId.trim()
    val displayCode = COLOR_CARD_BUILTIN_HALD_DISPLAY_CODES[canonicalId]
        ?: buildColorCardBuiltinFallbackDisplayCode(
            canonicalId = canonicalId,
            suffix = classifyColorCardBuiltinRegistrySuffix(canonicalId, title)
        )
    return ColorCardBuiltinHaldIdentity(
        canonicalId = canonicalId,
        displayCode = displayCode,
        sourceTag = sourceTag
    )
}

internal fun resolveColorCardTracePixelPosterDisplayCode(
    presetId: String
): String {
    return COLOR_CARD_PIXEL_POSTER_DISPLAY_CODES[presetId]
        ?: buildColorCardBuiltinFallbackDisplayCode(
            canonicalId = presetId,
            suffix = 'P'
        )
}

internal fun buildColorCardTraceHaldVariants(
    filters: List<ColorCardBuiltinHaldFilterAsset>
): List<ColorCardTraceHaldVariant> {
    if (filters.isEmpty()) return emptyList()
    val maxMagnitude = filters.maxOfOrNull { abs(it.variantValue ?: 0) }
        ?.coerceAtLeast(1)
        ?: 1
    return filters
        .sortedWith(
            compareBy<ColorCardBuiltinHaldFilterAsset> { normalizedColorCardVariantPosition(it, maxMagnitude) }
                .thenBy { abs(it.variantValue ?: 0) }
                .thenBy { it.sortOrder }
        )
        .map { filter ->
            ColorCardTraceHaldVariant(
                assetName = filter.assetName,
                haldLevel = filter.haldLevel,
                previewColors = filter.previewColors,
                position = normalizedColorCardVariantPosition(filter, maxMagnitude),
                variantLabel = filter.variantLabel
            )
        }
}

internal fun resolveColorCardTraceHaldBlend(
    candidate: ColorCardTraceFilterCandidate?,
    paletteStrength: Float
): ColorCardTraceResolvedHaldBlend {
    val variants = candidate
        ?.takeIf { it.kind == ColorCardTraceFilterKind.HaldClut }
        ?.haldVariants
        .orEmpty()
    if (variants.isEmpty()) {
        return ColorCardTraceResolvedHaldBlend(
            primaryAssetName = candidate?.haldAssetName,
            primaryHaldLevel = candidate?.haldLevel ?: 0,
            secondaryAssetName = null,
            secondaryHaldLevel = 0,
            variantMix = 0f
        )
    }
    if (variants.size == 1) {
        val variant = variants.first()
        return ColorCardTraceResolvedHaldBlend(
            primaryAssetName = variant.assetName,
            primaryHaldLevel = variant.haldLevel,
            secondaryAssetName = null,
            secondaryHaldLevel = 0,
            variantMix = 0f
        )
    }

    val targetPosition = ((paletteStrength.coerceIn(0f, 1f) * 2f) - 1f).coerceIn(-1f, 1f)
    val lowerIndex = variants.indexOfLast { it.position <= targetPosition }
    val upperIndex = variants.indexOfFirst { it.position >= targetPosition }
    val safeLower = when {
        lowerIndex >= 0 -> lowerIndex
        else -> 0
    }
    val safeUpper = when {
        upperIndex >= 0 -> upperIndex
        else -> variants.lastIndex
    }
    val lower = variants[safeLower]
    val upper = variants[safeUpper]
    if (safeLower == safeUpper || abs(upper.position - lower.position) <= 0.0001f) {
        return ColorCardTraceResolvedHaldBlend(
            primaryAssetName = lower.assetName,
            primaryHaldLevel = lower.haldLevel,
            secondaryAssetName = null,
            secondaryHaldLevel = 0,
            variantMix = 0f
        )
    }
    val variantMix = ((targetPosition - lower.position) / (upper.position - lower.position))
        .coerceIn(0f, 1f)
    return ColorCardTraceResolvedHaldBlend(
        primaryAssetName = lower.assetName,
        primaryHaldLevel = lower.haldLevel,
        secondaryAssetName = upper.assetName,
        secondaryHaldLevel = upper.haldLevel,
        variantMix = variantMix
    )
}

private data class ColorCardFilmSimVariantParseResult(
    val family: ColorCardFilmSimFamily,
    val variantLabel: String,
    val variantValue: Int
)

private fun parseColorCardFilmSimVariant(
    title: String
): ColorCardFilmSimVariantParseResult? {
    val match = COLOR_CARD_FILM_SIM_VARIANT_REGEX.matchEntire(title.trim()) ?: return null
    val familyToken = match.groupValues[1]
    val family = COLOR_CARD_FILM_SIM_FAMILIES[familyToken.lowercase(Locale.ROOT)]
        ?: COLOR_CARD_FILM_SIM_FAMILIES[familyToken]
        ?: return null
    val rawVariant = match.groupValues[2].trim()
    val normalizedVariant = rawVariant.lowercase(Locale.ROOT)
    return if ('s' in normalizedVariant) {
        ColorCardFilmSimVariantParseResult(
            family = family,
            variantLabel = "S",
            variantValue = 0
        )
    } else {
        val value = normalizedVariant.toIntOrNull() ?: return null
        ColorCardFilmSimVariantParseResult(
            family = family,
            variantLabel = if (value > 0) "+$value" else value.toString(),
            variantValue = value
        )
    }
}

private fun normalizedColorCardVariantPosition(
    filter: ColorCardBuiltinHaldFilterAsset,
    maxMagnitude: Int
): Float {
    val value = filter.variantValue ?: 0
    if (maxMagnitude <= 0) return 0f
    return (value.toFloat() / maxMagnitude.toFloat()).coerceIn(-1f, 1f)
}

private fun buildColorCardBuiltinFallbackDisplayCode(
    canonicalId: String,
    suffix: Char
): String {
    val hash = canonicalId.fold(17) { acc, char -> (acc * 31) + char.code }
    val number = 100 + abs(hash % 900)
    return "#$number$suffix"
}

private fun buildColorCardClassicMemoryDisplayCode(
    title: String
): String {
    val tokens = title
        .uppercase(Locale.ROOT)
        .split(Regex("[^A-Z0-9]+"))
        .filter { it.isNotBlank() }
    val letters = when {
        tokens.size >= 2 -> "${tokens.first().first()}${tokens.last().first()}"
        tokens.size == 1 -> tokens.first().padEnd(2, 'X').take(2)
        else -> "XM"
    }
    return "#${letters}M"
}

private fun buildColorCardBlackWhiteEraDisplayCode(
    canonicalId: String
): String {
    val hash = canonicalId.fold(23) { acc, char -> (acc * 37) + char.code }
    val number = 100 + abs(hash % 900)
    return "#${number}B"
}

private fun classifyColorCardBuiltinRegistrySuffix(
    canonicalId: String,
    title: String
): Char {
    val token = buildString {
        append(canonicalId)
        append(' ')
        append(title)
    }.lowercase(Locale.ROOT)
    return when {
        canonicalId.startsWith("j-") -> 'J'
        canonicalId.startsWith("black-white-era-") -> 'B'
        COLOR_CARD_REGISTRY_FILM_KEYWORDS.any(token::contains) -> 'F'
        COLOR_CARD_REGISTRY_CINEMA_KEYWORDS.any(token::contains) -> 'C'
        else -> 'A'
    }
}
