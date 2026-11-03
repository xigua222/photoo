package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun resolveCardRoastLabels(
    context: Context,
    labels: List<String>
): List<String> = withContext(Dispatchers.Default) {
    val englishLabels = ImageAnalyzer.normalizeToEnglish(context, labels)
    if (englishLabels.isNotEmpty()) englishLabels else labels
}
