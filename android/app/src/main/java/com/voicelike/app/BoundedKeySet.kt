package com.voicelike.app

/**
 * Small access-ordered set for session-only dedupe keys.
 * Keeps recent entries hot without feeding Compose snapshot state.
 */
class BoundedKeySet<K>(
    private val maxSize: Int
) {
    private val lock = Any()
    private val values = object : LinkedHashMap<K, Unit>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Unit>?): Boolean {
            return size > maxSize
        }
    }

    fun contains(key: K): Boolean = synchronized(lock) {
        values.containsKey(key)
    }

    fun add(key: K) {
        synchronized(lock) {
            values[key] = Unit
        }
    }

    fun addAll(keys: Iterable<K>) {
        synchronized(lock) {
            keys.forEach { key ->
                values[key] = Unit
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            values.clear()
        }
    }
}
