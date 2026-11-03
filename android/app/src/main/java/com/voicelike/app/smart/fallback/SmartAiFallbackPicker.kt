package com.voicelike.app.smart.fallback

import kotlin.random.Random

object SmartAiFallbackPicker {
    private const val MEOW_PROBABILITY_PERCENT = 1
    private val pickerLock = Any()
    private val primaryDeck = ArrayDeque<String>()
    private val meowDeck = ArrayDeque<String>()

    fun pick(blocked: Collection<String>): String {
        val blockedSet = blocked.toHashSet()
        val chooseMeow = Random.nextInt(100) < MEOW_PROBABILITY_PERCENT

        val primaryPick = if (chooseMeow) {
            pickUniqueFromPool(
                pool = SmartAiFallbackPools.meow,
                blocked = blockedSet,
                deck = meowDeck
            ) ?: pickUniqueFromPool(
                pool = SmartAiFallbackPools.primary,
                blocked = blockedSet,
                deck = primaryDeck
            )
        } else {
            pickUniqueFromPool(
                pool = SmartAiFallbackPools.primary,
                blocked = blockedSet,
                deck = primaryDeck
            ) ?: pickUniqueFromPool(
                pool = SmartAiFallbackPools.meow,
                blocked = blockedSet,
                deck = meowDeck
            )
        }
        if (primaryPick != null) return primaryPick

        val merged = SmartAiFallbackPools.primary + SmartAiFallbackPools.meow
        return merged.random()
    }

    private fun pickUniqueFromPool(
        pool: List<String>,
        blocked: Set<String>,
        deck: ArrayDeque<String>
    ): String? {
        if (pool.isEmpty()) return null
        synchronized(pickerLock) {
            refillDeckIfNeeded(pool, deck)

            repeat(deck.size) {
                val candidate = deck.removeFirst()
                if (!blocked.contains(candidate)) {
                    return candidate
                }
                deck.addLast(candidate)
            }

            val unblocked = pool.filterNot { blocked.contains(it) }
            if (unblocked.isEmpty()) return null

            deck.clear()
            deck.addAll(unblocked.shuffled())
            return deck.removeFirst()
        }
    }

    private fun refillDeckIfNeeded(pool: List<String>, deck: ArrayDeque<String>) {
        if (deck.isNotEmpty()) return
        deck.addAll(pool.shuffled())
    }
}
