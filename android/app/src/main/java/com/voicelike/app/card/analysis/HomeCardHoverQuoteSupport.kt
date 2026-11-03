package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val HOME_CARD_HOVER_QUOTE_ASSET_DIR = "home_card_quotes"
private const val HOME_CARD_HOVER_QUOTE_CANDIDATE_COUNT = 15

private enum class HomeCardHoverQuoteLibraryKey(val assetName: String) {
    Film("h.json"),
    Poetry("i.json"),
    Thought("k.json")
}

internal data class HomeCardHoverQuotePresentation(
    val timeLabel: String,
    val quote: String
)

private data class HomeCardHoverQuoteEntry(
    val id: Long,
    val type: HomeCardHoverQuoteLibraryKey,
    val text: String,
    val from: String?,
    val fromWho: String?,
    val length: Int
)

private data class HomeCardHoverQuoteLibrary(
    val all: List<HomeCardHoverQuoteEntry>,
    val byType: Map<HomeCardHoverQuoteLibraryKey, List<HomeCardHoverQuoteEntry>>
)

private data class HomeCardHoverQuoteSignals(
    val labels: List<String>,
    val typeScores: Map<HomeCardHoverQuoteLibraryKey, Int>,
    val seed: Long
)

@Volatile
private var cachedHomeCardHoverQuoteLibrary: HomeCardHoverQuoteLibrary? = null

private val HOME_CARD_HOVER_POETRY_KEYWORDS = listOf(
    "sky", "cloud", "sun", "sunrise", "sunset", "moon", "star", "water", "lake", "river", "sea",
    "ocean", "shore", "beach", "mountain", "forest", "flower", "plant", "tree", "leaf", "grass",
    "snow", "rain", "fog", "mist", "bird", "butterfly", "fish", "lotus", "garden",
    "天空", "云", "日落", "日出", "月", "星", "水", "湖", "河", "海", "山", "森林", "花", "树",
    "叶", "草", "雪", "雨", "雾", "鱼", "荷", "园", "风景", "自然"
)

private val HOME_CARD_HOVER_FILM_KEYWORDS = listOf(
    "person", "people", "selfie", "portrait", "face", "smile", "crowd", "city", "street", "building",
    "architecture", "bridge", "tower", "skyscraper", "night", "concert", "music", "food", "drink",
    "restaurant", "cafe", "travel", "trip", "vacation", "dog", "cat", "pet", "car", "bus",
    "人物", "人像", "自拍", "面孔", "笑", "人群", "城市", "街道", "建筑", "桥", "塔", "夜", "音乐",
    "食物", "咖啡", "旅行", "度假", "猫", "狗", "宠物", "车"
)

private val HOME_CARD_HOVER_THOUGHT_KEYWORDS = listOf(
    "book", "paper", "text", "document", "screenshot", "computer", "laptop", "phone", "room", "interior",
    "wall", "chair", "clock", "screen", "shadow", "statue", "sculpture", "object", "still life",
    "书", "纸", "文字", "文档", "截图", "电脑", "手机", "房间", "室内", "墙", "椅", "钟", "屏幕",
    "影", "雕塑", "器物", "静物", "哲学"
)

internal suspend fun resolveHomeCardHoverQuoteCandidates(
    context: Context,
    item: MediaItem,
    labels: List<String>
): List<HomeCardHoverQuotePresentation> {
    val library = loadHomeCardHoverQuoteLibrary(context)
    if (library.all.isEmpty()) return emptyList()

    val normalizedLabels = runCatching {
        ImageAnalyzer.normalizeToEnglish(context, labels)
    }.getOrElse { emptyList() }

    return withContext(Dispatchers.Default) {
        val signals = buildHomeCardHoverQuoteSignals(
            item = item,
            rawLabels = labels,
            normalizedLabels = normalizedLabels
        )
        val preferredTypes = signals.typeScores.entries
            .sortedByDescending { it.value }
            .map { it.key }

        val candidates = mutableListOf<HomeCardHoverQuoteEntry>()
        val seenTexts = LinkedHashSet<String>()
        preferredTypes.forEachIndexed { index, type ->
            val target = when (index) {
                0 -> 8
                1 -> 4
                else -> 3
            }
            appendQuoteSamples(
                pool = library.byType[type].orEmpty(),
                seed = signals.seed + (index * 131L),
                targetCount = target,
                seenTexts = seenTexts,
                out = candidates
            )
        }
        if (candidates.size < HOME_CARD_HOVER_QUOTE_CANDIDATE_COUNT) {
            appendQuoteSamples(
                pool = library.all,
                seed = signals.seed + 911L,
                targetCount = HOME_CARD_HOVER_QUOTE_CANDIDATE_COUNT - candidates.size,
                seenTexts = seenTexts,
                out = candidates
            )
        }
        val timeLabel = formatHomeCardHoverQuoteTime(item.dateAdded)
        candidates.take(HOME_CARD_HOVER_QUOTE_CANDIDATE_COUNT).map {
            HomeCardHoverQuotePresentation(
                timeLabel = timeLabel,
                quote = it.text
            )
        }
    }
}

private suspend fun loadHomeCardHoverQuoteLibrary(context: Context): HomeCardHoverQuoteLibrary {
    cachedHomeCardHoverQuoteLibrary?.let { return it }
    return withContext(Dispatchers.IO) {
        cachedHomeCardHoverQuoteLibrary?.let { return@withContext it }
        val loaded = buildList {
            HomeCardHoverQuoteLibraryKey.entries.forEach { key ->
                val assetPath = "$HOME_CARD_HOVER_QUOTE_ASSET_DIR/${key.assetName}"
                val entries = runCatching {
                    context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { reader ->
                        parseHomeCardHoverQuoteAsset(key, reader.readText())
                    }
                }.getOrElse { emptyList() }
                addAll(entries)
            }
        }
        val library = HomeCardHoverQuoteLibrary(
            all = loaded,
            byType = HomeCardHoverQuoteLibraryKey.entries.associateWith { key ->
                loaded.filter { it.type == key }
            }
        )
        cachedHomeCardHoverQuoteLibrary = library
        library
    }
}

private fun parseHomeCardHoverQuoteAsset(
    key: HomeCardHoverQuoteLibraryKey,
    rawJson: String
): List<HomeCardHoverQuoteEntry> {
    val array = JSONArray(rawJson)
    val result = ArrayList<HomeCardHoverQuoteEntry>(array.length())
    for (index in 0 until array.length()) {
        val obj = array.optJSONObject(index) ?: continue
        val quote = obj.optString("hitokoto").trim()
        if (quote.isBlank()) continue
        result += HomeCardHoverQuoteEntry(
            id = obj.optLong("id", index.toLong()),
            type = key,
            text = quote,
            from = obj.optString("from").trim().ifBlank { null },
            fromWho = obj.optString("from_who").trim().ifBlank { null },
            length = obj.optInt("length", quote.length).coerceAtLeast(quote.length)
        )
    }
    return result
}

private fun buildHomeCardHoverQuoteSignals(
    item: MediaItem,
    rawLabels: List<String>,
    normalizedLabels: List<String>
): HomeCardHoverQuoteSignals {
    val mergedLabels = buildList {
        addAll(normalizedLabels)
        addAll(rawLabels)
    }.mapNotNull { label ->
        label.trim().takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
    }.distinct()

    val scores = mutableMapOf(
        HomeCardHoverQuoteLibraryKey.Film to 2,
        HomeCardHoverQuoteLibraryKey.Poetry to 2,
        HomeCardHoverQuoteLibraryKey.Thought to 2
    )

    if (mergedLabels.isEmpty()) {
        scores[HomeCardHoverQuoteLibraryKey.Thought] = scores.getValue(HomeCardHoverQuoteLibraryKey.Thought) + 2
        scores[HomeCardHoverQuoteLibraryKey.Film] = scores.getValue(HomeCardHoverQuoteLibraryKey.Film) + 1
    }

    mergedLabels.forEach { label ->
        if (matchesQuoteKeyword(label, HOME_CARD_HOVER_POETRY_KEYWORDS)) {
            scores[HomeCardHoverQuoteLibraryKey.Poetry] = scores.getValue(HomeCardHoverQuoteLibraryKey.Poetry) + 3
        }
        if (matchesQuoteKeyword(label, HOME_CARD_HOVER_FILM_KEYWORDS)) {
            scores[HomeCardHoverQuoteLibraryKey.Film] = scores.getValue(HomeCardHoverQuoteLibraryKey.Film) + 3
        }
        if (matchesQuoteKeyword(label, HOME_CARD_HOVER_THOUGHT_KEYWORDS)) {
            scores[HomeCardHoverQuoteLibraryKey.Thought] = scores.getValue(HomeCardHoverQuoteLibraryKey.Thought) + 3
        }
        if (label.contains("night") || label.contains("city") || label.contains("neon")) {
            scores[HomeCardHoverQuoteLibraryKey.Film] = scores.getValue(HomeCardHoverQuoteLibraryKey.Film) + 1
        }
        if (label.contains("dark") || label.contains("shadow")) {
            scores[HomeCardHoverQuoteLibraryKey.Thought] = scores.getValue(HomeCardHoverQuoteLibraryKey.Thought) + 1
        }
        if (label.contains("sunset") || label.contains("sky") || label.contains("mountain")) {
            scores[HomeCardHoverQuoteLibraryKey.Poetry] = scores.getValue(HomeCardHoverQuoteLibraryKey.Poetry) + 1
        }
    }

    val hour = ((item.dateAdded % 86_400L) / 3_600L).toInt()
    if (hour in 5..8 || hour in 17..19) {
        scores[HomeCardHoverQuoteLibraryKey.Poetry] = scores.getValue(HomeCardHoverQuoteLibraryKey.Poetry) + 1
    } else if (hour >= 20 || hour in 0..3) {
        scores[HomeCardHoverQuoteLibraryKey.Film] = scores.getValue(HomeCardHoverQuoteLibraryKey.Film) + 1
        scores[HomeCardHoverQuoteLibraryKey.Thought] = scores.getValue(HomeCardHoverQuoteLibraryKey.Thought) + 1
    }

    val seed = item.id xor item.dateAdded xor mergedLabels.joinToString("|").hashCode().toLong()
    return HomeCardHoverQuoteSignals(
        labels = mergedLabels,
        typeScores = scores,
        seed = seed
    )
}

private fun matchesQuoteKeyword(
    label: String,
    keywords: List<String>
): Boolean {
    return keywords.any { keyword ->
        label == keyword || label.contains(keyword)
    }
}

private fun appendQuoteSamples(
    pool: List<HomeCardHoverQuoteEntry>,
    seed: Long,
    targetCount: Int,
    seenTexts: MutableSet<String>,
    out: MutableList<HomeCardHoverQuoteEntry>
) {
    if (pool.isEmpty() || targetCount <= 0) return

    val preferredLengthRange = 8..38
    val poolSize = pool.size
    val startIndex = positiveMod(seed, poolSize)
    val step = resolveSamplingStep(seed = seed, size = poolSize)
    val initialOutSize = out.size

    var index = startIndex
    var scanned = 0
    while (scanned < poolSize && (out.size - initialOutSize) < targetCount) {
        val entry = pool[index]
        if (entry.text !in seenTexts && entry.length in preferredLengthRange) {
            seenTexts += entry.text
            out += entry
        }
        index = (index + step) % poolSize
        scanned += 1
    }

    index = startIndex
    scanned = 0
    while (scanned < poolSize && (out.size - initialOutSize) < targetCount) {
        val entry = pool[index]
        if (entry.text !in seenTexts && entry.length in 6..64) {
            seenTexts += entry.text
            out += entry
        }
        index = (index + step) % poolSize
        scanned += 1
    }
}

private fun positiveMod(value: Long, size: Int): Int {
    return ((value % size) + size).toInt() % size
}

private fun resolveSamplingStep(
    seed: Long,
    size: Int
): Int {
    if (size <= 2) return 1
    var step = ((seed.absoluteValueMod(size - 1)) + 1).toInt()
    while (greatestCommonDivisor(step, size) != 1) {
        step += 1
        if (step >= size) step = 1
    }
    return step
}

private fun Long.absoluteValueMod(modulus: Int): Long {
    if (modulus <= 0) return 0L
    val normalized = this % modulus
    return if (normalized < 0L) normalized + modulus else normalized
}

private fun greatestCommonDivisor(a: Int, b: Int): Int {
    var left = kotlin.math.abs(a)
    var right = kotlin.math.abs(b)
    while (right != 0) {
        val next = left % right
        left = right
        right = next
    }
    return left.coerceAtLeast(1)
}

private fun formatHomeCardHoverQuoteTime(dateAddedSec: Long): String {
    if (dateAddedSec <= 0L) return ""
    val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    return formatter.format(Date(dateAddedSec * 1000L))
}
