package com.voicelike.app.smart

import com.voicelike.app.smart.fallback.SmartAiFallbackPicker
import kotlin.random.Random

object SmartAiRoastProvider {
    private const val shortTermMemorySize = 24
    private val globalRecent = kotlin.collections.ArrayDeque<String>()
    private val recentLock = Any()

    fun pick(labels: List<String>, recent: kotlin.collections.ArrayDeque<String>): String {
        val rankedLabels = SmartAiLabelRanker.rankTextsForRoast(labels).ifEmpty { labels }
        val blocked = buildBlockedSet(recent)

        val hasMappedLabel = rankedLabels.any { label ->
            RoastLibrary.resolveGroupKey(label) != null
        }

        val picked = if (hasMappedLabel) {
            pickRoastWithoutBlocked(rankedLabels, blocked)
                ?: SmartAiFallbackPicker.pick(blocked)
        } else {
            SmartAiFallbackPicker.pick(blocked)
        }

        appendRecent(recent, picked)
        return picked
    }

    fun randomIdleDelayMs(): Long = Random.nextLong(from = 3_000L, until = 6_001L)

    private fun pickRoastWithoutBlocked(
        labels: List<String>,
        blocked: Set<String>
    ): String? {
        return RoastLibrary.get(labels = labels, blocked = blocked)
    }

    private fun buildBlockedSet(localRecent: kotlin.collections.ArrayDeque<String>): Set<String> {
        synchronized(recentLock) {
            val blocked = LinkedHashSet<String>(localRecent.size + globalRecent.size)
            localRecent.forEach { blocked += it }
            globalRecent.forEach { blocked += it }
            return blocked
        }
    }

    private fun appendRecent(localRecent: kotlin.collections.ArrayDeque<String>, picked: String) {
        localRecent.addLast(picked)
        while (localRecent.size > shortTermMemorySize) {
            localRecent.removeFirst()
        }
        synchronized(recentLock) {
            globalRecent.addLast(picked)
            while (globalRecent.size > shortTermMemorySize) {
                globalRecent.removeFirst()
            }
        }
    }
}
