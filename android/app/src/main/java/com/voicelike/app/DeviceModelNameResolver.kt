package com.voicelike.app

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.util.Locale

object DeviceModelNameResolver {
    private const val ASSET_NAME = "device_model_market_map.json"

    @Volatile
    private var loaded = false
    private val lock = Any()
    private val byCode = HashMap<String, String>()
    private val byCompactCode = HashMap<String, String>()

    private fun normalizeCode(code: String): String {
        return code.trim().uppercase(Locale.US)
    }

    private fun compactCode(code: String): String {
        return normalizeCode(code).replace(Regex("[^A-Z0-9]"), "")
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            runCatching {
                context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
                    val json = JSONObject(reader.readText())
                    json.keys().forEach { key ->
                        val marketName = json.optString(key).trim()
                        if (marketName.isBlank()) return@forEach
                        val normalized = normalizeCode(key)
                        val compact = compactCode(key)
                        byCode[normalized] = marketName
                        if (compact.isNotBlank()) {
                            byCompactCode[compact] = marketName
                        }
                    }
                }
            }.onFailure {
                Log.w("DeviceModelNameResolver", "Failed to load $ASSET_NAME", it)
            }
            loaded = true
        }
    }

    private fun resolveMarketNameInternal(raw: String, context: Context): String? {
        ensureLoaded(context)
        if (byCode.isEmpty() && byCompactCode.isEmpty()) return null
        val direct = byCode[normalizeCode(raw)] ?: byCompactCode[compactCode(raw)]
        if (direct != null) return direct

        val tokenRegex = Regex("""[A-Za-z0-9][A-Za-z0-9,._-]{1,31}""")
        val matches = tokenRegex.findAll(raw).map { it.value }.toList()
        if (matches.isEmpty()) return null
        val sortedCandidates = matches.sortedByDescending { it.length }
        sortedCandidates.forEach { candidate ->
            val mapped = byCode[normalizeCode(candidate)] ?: byCompactCode[compactCode(candidate)]
            if (mapped != null) return mapped
        }
        return null
    }

    private fun sanitizeDisplayName(name: String): String {
        // Remove parenthetical suffixes for cleaner UI labels.
        val noParen = name
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""（[^）]*）"""), " ")
        val compact = noParen
            .replace(Regex("""\s+"""), " ")
            .trim()
        return dedupeLeadingBrand(compact)
    }

    private fun dedupeLeadingBrand(text: String): String {
        if (text.isBlank()) return text
        var result = text
        val repeatedPrefix = Regex("""^\s*([A-Za-z][A-Za-z0-9+_-]{1,20})\s+\1(\s+|$)""", RegexOption.IGNORE_CASE)
        repeat(3) {
            val m = repeatedPrefix.find(result) ?: return@repeat
            val brand = m.groupValues[1]
            val suffix = result.substring(m.range.last + 1).trimStart()
            result = if (suffix.isBlank()) brand else "$brand $suffix"
        }
        return result
    }

    fun resolveCameraLabel(context: Context, raw: String?): String {
        val normalizedRaw = raw?.trim().orEmpty()
        if (normalizedRaw.isBlank() || normalizedRaw == "--") return "--"
        val mapped = resolveMarketNameInternal(normalizedRaw, context)
        return if (mapped != null) {
            sanitizeDisplayName(mapped)
        } else {
            sanitizeDisplayName(normalizedRaw)
        }
    }
}
