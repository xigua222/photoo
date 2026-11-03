package com.voicelike.app

import android.content.Context
import android.graphics.Color as AndroidColor
import org.json.JSONArray
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

private enum class ColorCardRootTopicFormat {
    YamlPalette,
    RPalette,
    JapaneseTraditionalTex,
    ChineseTraditionalXlsx,
    DictionaryOfColourCombinationsSwatches,
    ColorNamesBestOfJson
}

private data class ColorCardRootTopicSource(
    val sourceId: String,
    val assetName: String,
    val topicNameKey: String,
    val format: ColorCardRootTopicFormat,
    val feedCycleStride: Int = 1
) {
    val sourceTag: String
        get() = "#$topicNameKey"
    fun topicName(copy: ColorCardInspirationCopy): String = when (topicNameKey) {
        "ghibli" -> copy.topicGhibli
        "artists" -> copy.topicArtists
        "hermitage" -> copy.topicHermitage
        "mexican_art" -> copy.topicMexicanArt
        "met_museum" -> copy.topicMetMuseum
        "pacific" -> copy.topicPacific
        "japanese_traditional" -> copy.topicJapaneseTraditional
        "australia" -> copy.topicAustralia
        "dutch_masters" -> copy.topicDutchMasters
        "butterflies" -> copy.topicButterflies
        "chinese_traditional" -> copy.topicChineseTraditional
        "dictionary_of_colour_combinations" -> copy.topicDictionaryOfColourCombinations
        "color_names_bestof" -> copy.topicColorNamesBestOf
        "global_universities" -> copy.topicGlobalUniversities
        "game" -> copy.topicGame
        else -> topicNameKey
    }
}

private val colorCardRootTopicSources = listOf(
    ColorCardRootTopicSource(
        sourceId = "okcolors",
        assetName = "color_card_okcolors.R",
        topicNameKey = "like",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "ghibli",
        assetName = "color_card_ghibli.yml",
        topicNameKey = "ghibli",
        format = ColorCardRootTopicFormat.YamlPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "artists",
        assetName = "color_card_artists.yml",
        topicNameKey = "artists",
        format = ColorCardRootTopicFormat.YamlPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "hermitage",
        assetName = "color_card_hermitage.R",
        topicNameKey = "hermitage",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "mexican_art",
        assetName = "color_card_mexican_art.R",
        topicNameKey = "mexican_art",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "met_museum",
        assetName = "color_card_met_museum.R",
        topicNameKey = "met_museum",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "pacific",
        assetName = "color_card_pacific.R",
        topicNameKey = "pacific",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "japanese_traditional",
        assetName = "color_card_japanese_traditional.tex",
        topicNameKey = "japanese_traditional",
        format = ColorCardRootTopicFormat.JapaneseTraditionalTex
    ),
    ColorCardRootTopicSource(
        sourceId = "australia",
        assetName = "color_card_australia.R",
        topicNameKey = "australia",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "dutch_masters",
        assetName = "color_card_dutch_masters.R",
        topicNameKey = "dutch_masters",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "butterflies",
        assetName = "color_card_butterflies.R",
        topicNameKey = "butterflies",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "chinese_traditional",
        assetName = "color_card_chinese_traditional.xlsx",
        topicNameKey = "chinese_traditional",
        format = ColorCardRootTopicFormat.ChineseTraditionalXlsx
    ),
    ColorCardRootTopicSource(
        sourceId = "dictionary_of_colour_combinations",
        assetName = "",
        topicNameKey = "dictionary_of_colour_combinations",
        format = ColorCardRootTopicFormat.DictionaryOfColourCombinationsSwatches
    ),
    ColorCardRootTopicSource(
        sourceId = "color_names_bestof",
        assetName = "color_card_colornames_bestof_min.json",
        topicNameKey = "color_names_bestof",
        format = ColorCardRootTopicFormat.ColorNamesBestOfJson,
        feedCycleStride = 2
    ),
    ColorCardRootTopicSource(
        sourceId = "global_universities",
        assetName = "color_card_global_universities.yml",
        topicNameKey = "global_universities",
        format = ColorCardRootTopicFormat.YamlPalette,
        feedCycleStride = 4
    ),
    ColorCardRootTopicSource(
        sourceId = "gris",
        assetName = "color_card_gris.R",
        topicNameKey = "gris",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "game",
        assetName = "color_card_game.R",
        topicNameKey = "game",
        format = ColorCardRootTopicFormat.RPalette
    ),
    ColorCardRootTopicSource(
        sourceId = "moma",
        assetName = "color_card_moma.R",
        topicNameKey = "moma",
        format = ColorCardRootTopicFormat.RPalette
    )
)

private data class ColorCardRootTopicParsedEntry(
    val rawTitle: String,
    val colors: List<Int>
)

private var cachedColorCardRootTopicEntries: List<ColorCardPaletteerPaletteEntry>? = null

internal fun loadColorCardRootTopicInspiration(
    context: Context,
    language: AppLanguage = AppLanguage.SimplifiedChinese
): List<ColorCardPaletteerPaletteEntry> {
    cachedColorCardRootTopicEntries?.let { return it }
    synchronized(colorCardRootTopicSources) {
        cachedColorCardRootTopicEntries?.let { return it }
        val inspirationCopy = getColorCardInspirationCopy(language)
        var nextIndex = 1
        val result = ArrayList<ColorCardPaletteerPaletteEntry>()
        colorCardRootTopicSources.forEach { source ->
            val parsedEntries = runCatching {
                when (source.format) {
                    ColorCardRootTopicFormat.YamlPalette -> {
                        context.assets.open(source.assetName)
                            .bufferedReader(Charsets.UTF_8)
                            .use { parseColorCardYamlPaletteEntries(it.readText()) }
                    }

                    ColorCardRootTopicFormat.RPalette -> {
                        context.assets.open(source.assetName)
                            .bufferedReader(Charsets.UTF_8)
                            .use { parseColorCardRPaletteEntries(it.readText()) }
                    }

                    ColorCardRootTopicFormat.JapaneseTraditionalTex -> {
                        context.assets.open(source.assetName)
                            .bufferedReader(Charsets.UTF_8)
                            .use { parseColorCardJapaneseTraditionalEntries(it.readText()) }
                    }

                    ColorCardRootTopicFormat.ChineseTraditionalXlsx -> {
                        context.assets.open(source.assetName).use(::parseColorCardChineseTraditionalEntries)
                    }

                    ColorCardRootTopicFormat.DictionaryOfColourCombinationsSwatches -> {
                        parseColorCardDictionaryOfColourCombinationsEntries(context)
                    }

                    ColorCardRootTopicFormat.ColorNamesBestOfJson -> {
                        parseColorCardColorNamesBestOfEntries(
                            context = context,
                            assetName = source.assetName
                        )
                    }
                }
            }.getOrElse { emptyList() }

            parsedEntries.forEach { parsed ->
                if (parsed.colors.isEmpty()) return@forEach
                result += ColorCardPaletteerPaletteEntry(
                    index = nextIndex++,
                    collectionName = source.topicName(inspirationCopy),
                    paletteName = translateColorCardRootTopicTitle(
                        sourceId = source.sourceId,
                        rawTitle = parsed.rawTitle,
                        inspirationCopy = inspirationCopy
                    ),
                    colors = parsed.colors,
                    sourceTag = source.sourceTag,
                    feedCycleStride = source.feedCycleStride
                )
            }
        }
        val loaded = result.sortedWith(
            compareBy<ColorCardPaletteerPaletteEntry> { it.collectionName }.thenBy { it.paletteName }
        )
        cachedColorCardRootTopicEntries = loaded
        return loaded
    }
}

internal fun clearColorCardRootTopicCache() {
    synchronized(colorCardRootTopicSources) {
        cachedColorCardRootTopicEntries = null
    }
}

private fun parseColorCardYamlPaletteEntries(rawText: String): List<ColorCardRootTopicParsedEntry> {
    val result = ArrayList<ColorCardRootTopicParsedEntry>()
    var currentTitle: String? = null
    val currentColors = ArrayList<Int>()

    fun flushCurrent() {
        val title = currentTitle ?: return
        if (currentColors.isNotEmpty()) {
            result += ColorCardRootTopicParsedEntry(
                rawTitle = title,
                colors = currentColors.toList()
            )
        }
        currentTitle = null
        currentColors.clear()
    }

    rawText.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) return@forEach
        if (!rawLine.startsWith(" ") && line.endsWith(":")) {
            flushCurrent()
            currentTitle = line.removeSuffix(":").trim()
            return@forEach
        }
        if (line.startsWith("-")) {
            parseColorCardHexToken(line.removePrefix("-").trim())?.let(currentColors::add)
        }
    }
    flushCurrent()
    return result
}

private fun parseColorCardRPaletteEntries(rawText: String): List<ColorCardRootTopicParsedEntry> {
    val body = extractColorCardRTopLevelListBody(rawText) ?: return emptyList()
    val result = ArrayList<ColorCardRootTopicParsedEntry>()
    val currentBlock = StringBuilder()
    var currentTitle: String? = null
    var depth = 0

    fun flushCurrent() {
        val title = currentTitle ?: return
        val colors = parseColorCardRPaletteColors(currentBlock.toString())
        if (colors.isNotEmpty()) {
            result += ColorCardRootTopicParsedEntry(
                rawTitle = title,
                colors = colors
            )
        }
        currentTitle = null
        currentBlock.clear()
    }

    body.lineSequence().forEach { line ->
        val entryMatch = if (depth == 0) colorCardRTopLevelEntryRegex.find(line) else null
        if (entryMatch != null) {
            flushCurrent()
            currentTitle = entryMatch.groupValues
                .drop(1)
                .firstOrNull { it.isNotBlank() }
        }
        if (currentTitle != null) {
            currentBlock.appendLine(line)
        }
        depth += countColorCardRParenthesisDelta(line)
    }
    flushCurrent()
    return result
}

private fun parseColorCardRPaletteColors(entryBlock: String): List<Int> {
    val cBlocks = extractColorCardRVectorBodies(entryBlock)
    if (cBlocks.isEmpty()) return emptyList()

    var paletteColors: List<Int>? = null
    cBlocks.forEach { block ->
        val colors = colorCardHexRegex.findAll(block)
            .mapNotNull { parseColorCardHexToken(it.value) }
            .toList()
        if (paletteColors == null && colors.isNotEmpty()) {
            paletteColors = colors
            return@forEach
        }
        val currentColors = paletteColors ?: return@forEach
        val order = parseColorCardRIndexOrder(block)
        if (order.isNotEmpty() && order.all { it in 1..currentColors.size }) {
            paletteColors = order.map { currentColors[it - 1] }
            return@forEach
        }
    }
    return paletteColors.orEmpty()
}

private fun extractColorCardRVectorBodies(rawText: String): List<String> {
    val result = ArrayList<String>()
    var index = 0
    var inString = false
    var quoteChar = '\u0000'
    var escaped = false

    while (index < rawText.length) {
        val char = rawText[index]
        if (inString) {
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == quoteChar) {
                inString = false
            }
            index += 1
            continue
        }

        if (char == '#') {
            while (index < rawText.length && rawText[index] != '\n' && rawText[index] != '\r') {
                index += 1
            }
            continue
        }

        if (char == '"' || char == '\'') {
            inString = true
            quoteChar = char
            index += 1
            continue
        }

        if (char == 'c') {
            val previous = rawText.getOrNull(index - 1)
            if (previous == null || (!previous.isLetterOrDigit() && previous != '_')) {
                var openParenIndex = index + 1
                while (openParenIndex < rawText.length && rawText[openParenIndex].isWhitespace()) {
                    openParenIndex += 1
                }
                if (openParenIndex < rawText.length && rawText[openParenIndex] == '(') {
                    val closeParenIndex = findColorCardRMatchingParen(rawText, openParenIndex) ?: break
                    result += rawText.substring(openParenIndex + 1, closeParenIndex)
                    index = closeParenIndex + 1
                    continue
                }
            }
        }

        index += 1
    }

    return result
}

private fun parseColorCardRIndexOrder(block: String): List<Int> {
    return block.split(',')
        .flatMap { token ->
            val trimmed = token.trim()
            val rangeMatch = colorCardRIntegerRangeRegex.matchEntire(trimmed)
            if (rangeMatch != null) {
                val start = rangeMatch.groupValues[1].toIntOrNull()
                val end = rangeMatch.groupValues[2].toIntOrNull()
                if (start != null && end != null) {
                    if (start <= end) {
                        (start..end).toList()
                    } else {
                        (start downTo end).toList()
                    }
                } else {
                    emptyList()
                }
            } else {
                trimmed.toIntOrNull()?.let(::listOf).orEmpty()
            }
        }
}

private fun findColorCardRMatchingParen(rawText: String, openingIndex: Int): Int? {
    var depth = 0
    var inString = false
    var quoteChar = '\u0000'
    var escaped = false
    var index = openingIndex

    while (index < rawText.length) {
        val char = rawText[index]
        if (inString) {
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == quoteChar) {
                inString = false
            }
            index += 1
            continue
        }

        if (char == '#') {
            while (index < rawText.length && rawText[index] != '\n' && rawText[index] != '\r') {
                index += 1
            }
            continue
        }

        if (char == '"' || char == '\'') {
            inString = true
            quoteChar = char
        } else when (char) {
            '(' -> depth += 1
            ')' -> {
                depth -= 1
                if (depth == 0) {
                    return index
                }
            }
        }
        index += 1
    }

    return null
}

private fun parseColorCardJapaneseTraditionalEntries(rawText: String): List<ColorCardRootTopicParsedEntry> {
    return colorCardJapaneseTraditionalRegex.findAll(rawText).mapNotNull { match ->
        val rgb = match.groupValues[2]
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .takeIf { it.size == 3 }
            ?: return@mapNotNull null
        ColorCardRootTopicParsedEntry(
            rawTitle = match.groupValues[3].trim(),
            colors = listOf(
                AndroidColor.rgb(
                    rgb[0].coerceIn(0, 255),
                    rgb[1].coerceIn(0, 255),
                    rgb[2].coerceIn(0, 255)
                )
            )
        )
    }.toList()
}

private fun parseColorCardChineseTraditionalEntries(input: InputStream): List<ColorCardRootTopicParsedEntry> {
    val zipEntries = readColorCardZipEntries(input)
    val sharedStrings = zipEntries["xl/sharedStrings.xml"]?.let(::parseColorCardSharedStrings).orEmpty()
    val sheetBytes = zipEntries["xl/worksheets/sheet1.xml"] ?: return emptyList()
    return parseColorCardSheetRows(sheetBytes, sharedStrings)
        .drop(1)
        .mapNotNull { row ->
            val name = row[0].trim()
            val red = row.getOrNull(5)?.toIntOrNull()
            val green = row.getOrNull(6)?.toIntOrNull()
            val blue = row.getOrNull(7)?.toIntOrNull()
            if (name.isBlank() || red == null || green == null || blue == null) {
                return@mapNotNull null
            }
            ColorCardRootTopicParsedEntry(
                rawTitle = name,
                colors = listOf(
                    AndroidColor.rgb(
                        red.coerceIn(0, 255),
                        green.coerceIn(0, 255),
                        blue.coerceIn(0, 255)
                    )
                )
            )
        }
}

private val colorCardDictionarySwatchAssetNames = listOf(
    "swatches_a.json",
    "swatches_b.json",
    "swatches_c.json",
    "swatches_d.json",
    "swatches_e.json",
    "swatches_f.json"
)

private fun parseColorCardDictionaryOfColourCombinationsEntries(context: Context): List<ColorCardRootTopicParsedEntry> {
    val combinations = LinkedHashMap<Int, MutableList<Int>>()
    colorCardDictionarySwatchAssetNames.forEach { assetName ->
        val rootObject = runCatching {
            context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { reader ->
                org.json.JSONObject(reader.readText())
            }
        }.getOrNull() ?: return@forEach
        val rootArray = rootObject.keys().asSequence()
            .mapNotNull { key -> rootObject.optJSONArray(key) }
            .firstOrNull()
            ?: return@forEach
        for (index in 0 until rootArray.length()) {
            val colorObject = rootArray.optJSONObject(index) ?: continue
            val colorInt = parseColorCardCmykArray(colorObject.optJSONArray("cmyk"))
                ?: continue
            val combinationIds = colorObject.optJSONArray("combinations") ?: continue
            for (combinationIndex in 0 until combinationIds.length()) {
                val combinationId = combinationIds.optInt(combinationIndex, Int.MIN_VALUE)
                if (combinationId == Int.MIN_VALUE) continue
                combinations.getOrPut(combinationId) { ArrayList() }.add(colorInt)
            }
        }
    }
    val titleWidth = maxOf(3, combinations.keys.maxOrNull()?.toString()?.length ?: 1)
    return combinations.entries
        .sortedBy { it.key }
        .mapNotNull { (combinationId, colors) ->
            val normalizedColors = colors.distinct()
            if (normalizedColors.isEmpty()) return@mapNotNull null
            ColorCardRootTopicParsedEntry(
                rawTitle = "组合 ${combinationId.toString().padStart(titleWidth, '0')}",
                colors = normalizedColors
            )
    }
}

private fun parseColorCardColorNamesBestOfEntries(
    context: Context,
    assetName: String
): List<ColorCardRootTopicParsedEntry> {
    val rootObject = runCatching {
        context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { reader ->
            org.json.JSONObject(reader.readText())
        }
    }.getOrNull() ?: return emptyList()

    return rootObject.keys().asSequence()
        .mapNotNull { hexToken ->
            val title = rootObject.optString(hexToken).trim()
            val colorInt = parseColorCardHexToken(hexToken) ?: return@mapNotNull null
            if (title.isBlank()) return@mapNotNull null
            ColorCardRootTopicParsedEntry(
                rawTitle = title,
                colors = listOf(colorInt)
            )
        }
        .sortedBy { entry -> entry.rawTitle.lowercase() }
        .toList()
}

private fun parseColorCardCmykArray(cmykArray: JSONArray?): Int? {
    val values = cmykArray ?: return null
    if (values.length() < 4) return null
    val cyan = values.optDouble(0, Double.NaN)
    val magenta = values.optDouble(1, Double.NaN)
    val yellow = values.optDouble(2, Double.NaN)
    val key = values.optDouble(3, Double.NaN)
    if (cyan.isNaN() || magenta.isNaN() || yellow.isNaN() || key.isNaN()) {
        return null
    }
    val c = (cyan / 100.0).coerceIn(0.0, 1.0)
    val m = (magenta / 100.0).coerceIn(0.0, 1.0)
    val y = (yellow / 100.0).coerceIn(0.0, 1.0)
    val k = (key / 100.0).coerceIn(0.0, 1.0)
    val red = ((1.0 - c) * (1.0 - k) * 255.0).toInt().coerceIn(0, 255)
    val green = ((1.0 - m) * (1.0 - k) * 255.0).toInt().coerceIn(0, 255)
    val blue = ((1.0 - y) * (1.0 - k) * 255.0).toInt().coerceIn(0, 255)
    return AndroidColor.rgb(red, green, blue)
}

private fun extractColorCardRTopLevelListBody(rawText: String): String? {
    val markerMatch = colorCardRListAssignmentRegex.find(rawText) ?: return null
    val openingIndex = rawText.indexOf('(', markerMatch.range.first)
    if (openingIndex < 0) return null

    var depth = 0
    var inString = false
    var quoteChar = '\u0000'
    var escaped = false
    var index = openingIndex

    while (index < rawText.length) {
        val char = rawText[index]
        if (inString) {
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == quoteChar) {
                inString = false
            }
            index += 1
            continue
        }

        if (char == '#') {
            while (index < rawText.length && rawText[index] != '\n' && rawText[index] != '\r') {
                index += 1
            }
            continue
        }

        if (char == '"' || char == '\'') {
            inString = true
            quoteChar = char
        } else when (char) {
            '(' -> depth += 1
            ')' -> {
                depth -= 1
                if (depth == 0) {
                    return rawText.substring(openingIndex + 1, index)
                }
            }
        }
        index += 1
    }
    return null
}

private fun countColorCardRParenthesisDelta(line: String): Int {
    var delta = 0
    var inString = false
    var quoteChar = '\u0000'
    var escaped = false
    var commentStart = false

    for (char in line) {
        if (commentStart) break
        if (inString) {
            if (escaped) {
                escaped = false
                continue
            }
            if (char == '\\') {
                escaped = true
                continue
            }
            if (char == quoteChar) {
                inString = false
            }
            continue
        }
        if (char == '#') {
            commentStart = true
            continue
        }
        if (char == '"' || char == '\'') {
            inString = true
            quoteChar = char
            continue
        }
        if (char == '(') delta += 1
        if (char == ')') delta -= 1
    }
    return delta
}

private fun readColorCardZipEntries(input: InputStream): Map<String, ByteArray> {
    val result = LinkedHashMap<String, ByteArray>()
    ZipInputStream(input).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val bytes = ByteArrayOutputStream()
            zip.copyTo(bytes)
            result[entry.name] = bytes.toByteArray()
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return result
}

private fun parseColorCardSharedStrings(bytes: ByteArray): List<String> {
    val document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(bytes))
    val sharedStringNodes = document.getElementsByTagName("si")
    val result = ArrayList<String>(sharedStringNodes.length)
    for (index in 0 until sharedStringNodes.length) {
        result += sharedStringNodes.item(index).textContent.orEmpty()
    }
    return result
}

private fun parseColorCardSheetRows(
    bytes: ByteArray,
    sharedStrings: List<String>
): List<List<String>> {
    val document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(bytes))
    val rowNodes = document.getElementsByTagName("row")
    val result = ArrayList<List<String>>(rowNodes.length)
    for (rowIndex in 0 until rowNodes.length) {
        val rowElement = rowNodes.item(rowIndex) as? Element ?: continue
        val cellNodes = rowElement.getElementsByTagName("c")
        val rowValues = ArrayList<String>()
        for (cellIndex in 0 until cellNodes.length) {
            val cellElement = cellNodes.item(cellIndex) as? Element ?: continue
            val reference = cellElement.getAttribute("r").orEmpty()
            val columnIndex = colorCardSpreadsheetColumnIndex(reference.takeWhile { it.isLetter() })
            while (rowValues.size <= columnIndex) {
                rowValues += ""
            }
            val rawValue = cellElement.getElementsByTagName("v")
                .item(0)
                ?.textContent
                .orEmpty()
            rowValues[columnIndex] = if (cellElement.getAttribute("t") == "s") {
                sharedStrings.getOrNull(rawValue.toIntOrNull() ?: -1).orEmpty()
            } else {
                rawValue
            }
        }
        result += rowValues
    }
    return result
}

private fun colorCardSpreadsheetColumnIndex(columnToken: String): Int {
    var result = 0
    columnToken.forEach { char ->
        result = (result * 26) + (char.uppercaseChar() - 'A' + 1)
    }
    return (result - 1).coerceAtLeast(0)
}

private fun parseColorCardHexToken(token: String): Int? {
    val normalized = token.trim()
        .removePrefix("\"")
        .removeSuffix("\"")
        .removePrefix("'")
        .removeSuffix("'")
        .removePrefix("#")
        .uppercase()
    if (normalized.length != 6 || normalized.any { it !in '0'..'9' && it !in 'A'..'F' }) {
        return null
    }
    return runCatching { AndroidColor.parseColor("#$normalized") }.getOrNull()
}

private fun translateColorCardRootTopicTitle(
    sourceId: String,
    rawTitle: String,
    inspirationCopy: ColorCardInspirationCopy
): String {
    return when (sourceId) {
        "ghibli" -> translateColorCardGhibliTitle(rawTitle, inspirationCopy)
        "artists" -> translateColorCardArtistTitle(rawTitle)
        "game" -> translateColorCardGameTitle(rawTitle)
        "japanese_traditional",
        "chinese_traditional",
        "dictionary_of_colour_combinations",
        "color_names_bestof",
        "global_universities" -> rawTitle
        else -> translateColorCardGeneralTitle(rawTitle)
    }
}

private fun translateColorCardGhibliTitle(rawTitle: String, inspirationCopy: ColorCardInspirationCopy): String {
    val match = colorCardGhibliTitleRegex.matchEntire(rawTitle) ?: return translateColorCardGeneralTitle(rawTitle)
    val film = colorCardGhibliFilmTranslations[match.groupValues[1]] ?: match.groupValues[1]
    val tone = when (match.groupValues[2]) {
        "Light" -> inspirationCopy.toneLight
        "Medium" -> inspirationCopy.toneMedium
        "Dark" -> inspirationCopy.toneDark
        else -> match.groupValues[2]
    }
    val variant = match.groupValues[3].takeIf { it.isNotBlank() }?.let { " ${it.toInt()}" }.orEmpty()
    return "$film $tone$variant"
}

private fun translateColorCardArtistTitle(rawTitle: String): String {
    val variant = rawTitle.substringAfter('_', "").takeIf { it.isNotBlank() }
    val base = rawTitle.substringBefore('_')
    val translatedBase = colorCardArtistNameTranslations[base] ?: humanizeColorCardRawTitle(base)
    return if (variant != null) {
        "$translatedBase · ${variant.padStart(2, '0')}"
    } else {
        translatedBase
    }
}

private fun translateColorCardGameTitle(rawTitle: String): String {
    if (rawTitle.length <= 4 && rawTitle.any(Char::isDigit) && rawTitle.all { it.isLetterOrDigit() }) {
        return rawTitle.uppercase()
    }
    return rawTitle
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .replace(colorCardWhitespaceRegex, " ")
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            if (token.any(Char::isDigit)) {
                token.uppercase()
            } else {
                token.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
        }
}

private fun translateColorCardGeneralTitle(rawTitle: String): String {
    val variantMatch = colorCardTrailingVariantRegex.find(rawTitle)
    val variant = variantMatch?.groupValues?.get(1)
    val base = if (variantMatch != null) rawTitle.removeRange(variantMatch.range) else rawTitle
    colorCardKnownRawTitleTranslations[base]?.let { translated ->
        return if (variant != null) "$translated $variant" else translated
    }

    val humanized = humanizeColorCardRawTitle(base)
    colorCardKnownPhraseTranslations[humanized]?.let { translated ->
        return if (variant != null) "$translated $variant" else translated
    }

    val translated = if (!humanized.contains(' ')) {
        colorCardKnownWordTranslations[humanized] ?: humanized
    } else {
        humanized
    }
    return if (variant != null) "$translated $variant" else translated
}

private fun humanizeColorCardRawTitle(rawTitle: String): String {
    return rawTitle
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(colorCardCamelBoundaryRegex, "$1 $2")
        .replace(colorCardDigitBoundaryRegex, "$1 $2")
        .replace(colorCardReverseDigitBoundaryRegex, "$1 $2")
        .trim()
        .replace(colorCardWhitespaceRegex, " ")
}

private val colorCardRListAssignmentRegex = Regex(
    """^[ \t]*[A-Za-z][A-Za-z0-9_.]*\s*<-\s*list\s*\(""",
    setOf(RegexOption.MULTILINE)
)
private val colorCardRTopLevelEntryRegex =
    Regex("""^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z][A-Za-z0-9_\-]*))\s*=""")
private val colorCardHexRegex = Regex("""#[0-9A-Fa-f]{6}""")
private val colorCardIntegerRegex = Regex("""\b\d+\b""")
private val colorCardRIntegerRangeRegex = Regex("""(\d+)\s*:\s*(\d+)""")
private val colorCardJapaneseTraditionalRegex =
    Regex("""\\definecolor\{([^}]+)\}\{RGB\}\{([^}]+)\}\s*%([^\r\n]+)""")
private val colorCardGhibliTitleRegex = Regex("""([A-Za-z]+)(Light|Medium|Dark)(\d+)?""")
private val colorCardTrailingVariantRegex = Regex("""(\d+)$""")
private val colorCardCamelBoundaryRegex = Regex("""([a-z])([A-Z])""")
private val colorCardDigitBoundaryRegex = Regex("""([A-Za-z])(\d)""")
private val colorCardReverseDigitBoundaryRegex = Regex("""(\d)([A-Za-z])""")
private val colorCardWhitespaceRegex = Regex("""\s+""")

private val colorCardGhibliFilmTranslations = mapOf(
    "Marnie" to "回忆中的玛妮",
    "Ponyo" to "悬崖上的金鱼姬",
    "Laputa" to "天空之城",
    "Mononoke" to "幽灵公主",
    "Spirited" to "千与千寻",
    "Yesterday" to "岁月的童话",
    "Kiki" to "魔女宅急便",
    "Totoro" to "龙猫"
)

private val colorCardArtistNameTranslations = mapOf(
    "JosefAlbers" to "约瑟夫·阿尔伯斯",
    "HilmaafKlint" to "希尔玛·阿夫·克林特",
    "Jean-MichelBasquiat" to "让-米歇尔·巴斯奎特",
    "FernandoBotero" to "费尔南多·波特罗",
    "SandroBotticelli" to "桑德罗·波提切利",
    "PieterBruegel" to "彼得·勃鲁盖尔",
    "MaryCassatt" to "玛丽·卡萨特",
    "MarcChagall" to "马克·夏加尔",
    "SalvadorDali" to "萨尔瓦多·达利",
    "LeonardodaVinci" to "列奥纳多·达·芬奇",
    "EdgarDegas" to "埃德加·德加",
    "PaulGauguin" to "保罗·高更",
    "KatsushikaHokusai" to "葛饰北斋",
    "EdwardHopper" to "爱德华·霍普",
    "FridaKahlo" to "弗里达·卡罗",
    "WassilyKandinsky" to "瓦西里·康定斯基",
    "PaulKlee" to "保罗·克利",
    "YvesKlein" to "伊夫·克莱因",
    "GustavKlimt" to "古斯塔夫·克里姆特",
    "LeeKrasner" to "李·克拉斯纳",
    "KazimirMalevich" to "卡济米尔·马列维奇",
    "ReneMagritte" to "勒内·马格里特",
    "Michelangelo" to "米开朗基罗",
    "AmedeoModigliani" to "阿梅代奥·莫迪利亚尼",
    "PietMondrian" to "皮特·蒙德里安",
    "ClaudeMonet" to "克洛德·莫奈",
    "EdvardMunch" to "爱德华·蒙克",
    "PabloPicasso" to "巴勃罗·毕加索",
    "JacksonPollock" to "杰克逊·波洛克",
    "OdilonRedon" to "奥迪隆·雷东",
    "Rembrandt" to "伦勃朗",
    "Pierre-AugusteRenoir" to "皮埃尔-奥古斯特·雷诺阿",
    "GeorgesSeurat" to "乔治·修拉",
    "CyTwombly" to "赛·托姆布雷",
    "TheovanDoesburg" to "特奥·凡·杜斯堡",
    "JanvanEyck" to "扬·凡·艾克",
    "VincentvanGogh" to "文森特·梵高",
    "RemediosVaro" to "蕾梅黛丝·瓦罗",
    "JohannesVermeer" to "约翰内斯·维米尔",
    "AndyWarhol" to "安迪·沃霍尔",
    "GrantWood" to "格兰特·伍德"
)

private val colorCardKnownRawTitleTranslations = mapOf(
    "OKeeffe" to "欧姬芙",
    "VanGogh" to "梵高",
    "Cassatt" to "卡萨特",
    "Gauguin" to "高更",
    "Klimt" to "克里姆特",
    "Kandinsky" to "康定斯基",
    "Hokusai" to "葛饰北斋",
    "Hiroshige" to "歌川广重",
    "Renoir" to "雷诺阿",
    "Redon" to "雷东",
    "Manet" to "马奈",
    "Monet" to "莫奈",
    "Signac" to "西涅克",
    "Demuth" to "查尔斯·德穆斯",
    "Derain" to "安德烈·德兰",
    "Tiepolo" to "提埃波罗",
    "Veronese" to "委罗内塞",
    "Ingres" to "安格尔",
    "Archambault" to "阿尚博",
    "Navajo" to "纳瓦霍",
    "Lakota" to "拉科塔",
    "Nizami" to "尼扎米",
    "Pissaro" to "毕沙罗",
    "Tam" to "谭",
    "Tara" to "塔拉",
    "Thomas" to "托马斯",
    "Cross" to "克罗斯",
    "Degas" to "德加",
    "collioure_matisse" to "科利乌尔·马蒂斯",
    "dance_matisse" to "舞蹈·马蒂斯",
    "magdalene_titian" to "抹大拉的玛利亚",
    "flight_gauguin" to "飞翔·高更",
    "cottages_vincent" to "乡舍·梵高",
    "madonna_litta" to "利塔圣母",
    "prodigal_son" to "浪子回头",
    "peacock_clock" to "孔雀钟",
    "danae" to "达那厄",
    "judith" to "朱迪斯",
    "delft_ware" to "代尔夫特陶器",
    "du_barry" to "杜巴里",
    "milkmaid" to "倒牛奶的女仆",
    "pearl_earring" to "戴珍珠耳环的少女",
    "view_of_Delft" to "代尔夫特风景",
    "little_street" to "小巷",
    "anatomy" to "杜尔普博士的解剖课",
    "staalmeesters" to "布商公会理事",
    "namatjira_qual" to "纳马吉拉",
    "namatjira_div" to "纳马吉拉·分歧",
    "nolan_ned" to "内德·凯利",
    "healthy_reef" to "健康珊瑚礁",
    "dead_reef" to "白化珊瑚礁",
    "jumping_frog" to "跳蛙",
    "emu_woman_paired" to "鸸鹋女人"
)

private val colorCardKnownPhraseTranslations = mapOf(
    "Egypt" to "埃及",
    "Greek" to "希腊",
    "Austria" to "奥地利",
    "Java" to "爪哇",
    "New Kingdom" to "新王国",
    "Peru" to "秘鲁",
    "Winter" to "冬日",
    "Spring" to "春日",
    "Sunset" to "日落",
    "Mushroom" to "蘑菇",
    "Starfish" to "海星",
    "Bay" to "海湾",
    "Lake" to "湖泊",
    "Anemone" to "海葵",
    "Parliament" to "议会",
    "Tasmania" to "塔斯马尼亚",
    "Lorikeet" to "吸蜜鹦鹉",
    "Galah" to "凤头鹦鹉",
    "Revolucion" to "革命",
    "Naturaleza" to "自然",
    "Tierra" to "大地",
    "Aurora" to "极光",
    "Concha" to "海螺",
    "Frida" to "弗里达",
    "Maiz" to "玉米",
    "Ofrenda" to "供奉",
    "Ronda" to "回旋",
    "Alacena" to "餐柜",
    "Atentado" to "突袭",
    "Vendedora" to "女贩"
)

private val colorCardKnownWordTranslations = mapOf(
    "Albers" to "阿尔伯斯",
    "Klee" to "克利",
    "Monet" to "莫奈",
    "Manet" to "马奈",
    "Redon" to "雷东",
    "Renoir" to "雷诺阿",
    "Gauguin" to "高更",
    "Klimt" to "克里姆特",
    "Kandinsky" to "康定斯基",
    "Hokusai" to "葛饰北斋",
    "Hiroshige" to "歌川广重",
    "Veronese" to "委罗内塞",
    "Ingres" to "安格尔",
    "Navajo" to "纳瓦霍",
    "Lakota" to "拉科塔"
)
