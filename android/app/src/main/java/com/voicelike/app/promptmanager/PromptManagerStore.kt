package com.voicelike.app.promptmanager

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PromptManagerStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("prompt_manager_store", Context.MODE_PRIVATE)
    private val _prompts = MutableStateFlow(loadPrompts())

    val prompts: StateFlow<List<ManagedPrompt>> = _prompts

    fun upsert(
        existingId: String?,
        title: String,
        imageUriString: String?,
        platform: String,
        category: String,
        summary: String,
        prompt: String
    ): ManagedPrompt {
        val now = System.currentTimeMillis()
        val current = _prompts.value
        val existing = existingId?.let { id -> current.firstOrNull { it.id == id } }
        val item = ManagedPrompt(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            imageUriString = imageUriString?.trim()?.takeIf { it.isNotBlank() },
            platform = platform.trim(),
            category = category.trim(),
            summary = summary.trim(),
            prompt = prompt.trim(),
            createdAtMillis = existing?.createdAtMillis ?: now,
            updatedAtMillis = now
        )
        val next = if (existing == null) {
            listOf(item) + current
        } else {
            current.map { if (it.id == item.id) item else it }
        }.sortedByDescending { it.updatedAtMillis }
        persist(next)
        return item
    }

    fun delete(id: String) {
        persist(_prompts.value.filterNot { it.id == id })
    }

    private fun persist(items: List<ManagedPrompt>) {
        _prompts.value = items
        prefs.edit().putString(KEY_PROMPTS, items.toJsonArray().toString()).apply()
    }

    private fun loadPrompts(): List<ManagedPrompt> {
        val raw = prefs.getString(KEY_PROMPTS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(
                        ManagedPrompt(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            title = obj.optString("title"),
                            imageUriString = obj.optString("imageUriString").takeIf { it.isNotBlank() },
                            platform = obj.optString("platform"),
                            category = obj.optString("category"),
                            summary = obj.optString("summary"),
                            prompt = obj.optString("prompt"),
                            createdAtMillis = obj.optLong("createdAtMillis", 0L),
                            updatedAtMillis = obj.optLong("updatedAtMillis", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.updatedAtMillis }
        }.getOrElse { emptyList() }
    }

    companion object {
        const val DEFAULT_CATEGORY = "灵感"
        private const val KEY_PROMPTS = "prompts"

        @Volatile
        private var instance: PromptManagerStore? = null

        fun get(context: Context): PromptManagerStore =
            instance ?: synchronized(this) {
                instance ?: PromptManagerStore(context).also { instance = it }
            }
    }
}

private fun List<ManagedPrompt>.toJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("imageUriString", item.imageUriString.orEmpty())
                .put("platform", item.platform)
                .put("category", item.category)
                .put("summary", item.summary)
                .put("prompt", item.prompt)
                .put("createdAtMillis", item.createdAtMillis)
                .put("updatedAtMillis", item.updatedAtMillis)
        )
    }
    return array
}
