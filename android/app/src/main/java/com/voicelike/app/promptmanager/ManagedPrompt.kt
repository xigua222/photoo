package com.voicelike.app.promptmanager

data class ManagedPrompt(
    val id: String,
    val title: String,
    val imageUriString: String?,
    val platform: String,
    val category: String,
    val summary: String,
    val prompt: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    val displayTitle: String
        get() = title.ifBlank {
            prompt.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        }.ifBlank { "未命名提示词" }
}

