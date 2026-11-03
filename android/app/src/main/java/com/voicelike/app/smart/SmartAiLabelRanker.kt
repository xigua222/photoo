package com.voicelike.app.smart

import java.util.Locale

data class RankedMlLabel(
    val text: String,
    val confidence: Float
)

object SmartAiLabelRanker {
    private const val DOMAIN_WEIGHT = 0.55f
    private const val GROUP_WEIGHT = 0.25f
    private const val GENERAL_GROUP_BONUS = 0.06f
    private const val POSITION_DECAY = 0.16f
    private const val MIN_POSITION_CONFIDENCE = 0.2f

    private data class Candidate(
        val text: String,
        val confidence: Float,
        val index: Int,
        val groupKey: String?,
        val domainKey: String
    )

    fun rankForRoast(labels: List<RankedMlLabel>, limit: Int): List<RankedMlLabel> {
        if (labels.isEmpty() || limit <= 0) return emptyList()
        val candidates = ArrayList<Candidate>(labels.size)
        val seen = HashSet<String>()

        labels.forEachIndexed { index, raw ->
            val text = raw.text.trim()
            if (text.isBlank()) return@forEachIndexed
            val normalizedKey = text.lowercase(Locale.ROOT)
            if (!seen.add(normalizedKey)) return@forEachIndexed

            val confidence = raw.confidence.coerceIn(0f, 1f)
            val groupKey = RoastLibrary.resolveGroupKey(text)
            val domainKey = groupKey?.substringBefore('_') ?: "__other_$index"
            candidates += Candidate(
                text = text,
                confidence = confidence,
                index = index,
                groupKey = groupKey,
                domainKey = domainKey
            )
        }

        if (candidates.isEmpty()) return emptyList()

        val domainScores = HashMap<String, Float>()
        val groupScores = HashMap<String, Float>()
        candidates.forEach { candidate ->
            domainScores[candidate.domainKey] =
                (domainScores[candidate.domainKey] ?: 0f) + candidate.confidence
            candidate.groupKey?.let { key ->
                groupScores[key] = (groupScores[key] ?: 0f) + candidate.confidence
            }
        }

        val ranked = candidates.sortedWith(
            compareByDescending<Candidate> { candidate ->
                val domainScore = domainScores[candidate.domainKey] ?: 0f
                val groupScore = candidate.groupKey?.let { groupScores[it] } ?: 0f
                val generalBonus = if (candidate.groupKey?.endsWith("_general") == true) {
                    GENERAL_GROUP_BONUS
                } else {
                    0f
                }
                candidate.confidence +
                    domainScore * DOMAIN_WEIGHT +
                    groupScore * GROUP_WEIGHT +
                    generalBonus
            }.thenByDescending { candidate ->
                candidate.confidence
            }.thenBy { candidate ->
                candidate.index
            }
        )

        return ranked
            .take(limit.coerceAtMost(ranked.size))
            .map { RankedMlLabel(text = it.text, confidence = it.confidence) }
    }

    fun rankTextsForRoast(labels: List<String>, limit: Int = labels.size): List<String> {
        if (labels.isEmpty() || limit <= 0) return emptyList()
        val scored = labels.mapIndexed { index, label ->
            val confidence = (1f - index * POSITION_DECAY).coerceAtLeast(MIN_POSITION_CONFIDENCE)
            RankedMlLabel(text = label, confidence = confidence)
        }
        return rankForRoast(scored, limit).map { it.text }
    }
}
