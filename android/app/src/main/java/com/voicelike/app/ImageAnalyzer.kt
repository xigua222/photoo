package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.voicelike.app.smart.RankedMlLabel
import com.voicelike.app.smart.SmartAiLabelRanker
import com.voicelike.app.smart.translation.MlKitLabelTranslationExtensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Standalone Image Analyzer using ML Kit.
 * Handles on-device image labeling with simple localization mapping.
 */
object ImageAnalyzer {
    private const val MAX_LABEL_SIDE_PX = 2048
    private const val MAX_LABEL_PIXELS = 4_200_000L
    private const val LARGE_IMAGE_SIDE_PX = 4096
    private const val MIN_LABEL_SIDE_PX = 32
    private const val LARGE_IMAGE_PIXELS = 12_000_000L

    enum class AnalyzeFailureKind {
        URI_UNRESOLVED
    }

    data class AnalyzeResult(
        val labels: List<String>,
        val failureKind: AnalyzeFailureKind? = null
    )

    private data class LabelSourceInfo(
        val width: Int,
        val height: Int,
        val mimeType: String?,
        val rotationDegrees: Int
    ) {
        val pixels: Long
            get() = width.toLong() * height.toLong()
    }
    
    // Loaded from assets
    private var labelMap: Map<String, Map<String, String>>? = null
    private var reverseToEnglishMap: Map<String, String>? = null
    private val labelerMutex = Mutex()
    private val analysisPermit = Semaphore(2)

    @Volatile
    private var sharedLabeler: ImageLabeler? = null

    private fun obtainSharedLabeler(): ImageLabeler {
        val existing = sharedLabeler
        if (existing != null) return existing
        return synchronized(this) {
            sharedLabeler ?: ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS).also {
                sharedLabeler = it
            }
        }
    }

    private fun ensureMapLoaded(context: Context) {
        if (labelMap != null) {
            if (reverseToEnglishMap == null) buildReverseMap()
            return
        }

        synchronized(this) {
            if (labelMap != null) {
                if (reverseToEnglishMap == null) buildReverseMap()
                return
            }

            try {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("ImageAnalyzer", "Loading imagenet_labels_localized.json...")
                }
                val json = context.assets.open("imagenet_labels_localized.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                val loaded: Map<String, Map<String, String>> = Gson().fromJson(json, type) ?: emptyMap()
                labelMap = MlKitLabelTranslationExtensions.merge(loaded)
                buildReverseMap()
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("ImageAnalyzer", "Loaded label map with ${labelMap?.size} entries")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("ImageAnalyzer", "Error loading label map", e)
                AppRuntimeLogStore.logError(context, "ImageAnalyzer", "Error loading label map", e)
                labelMap = MlKitLabelTranslationExtensions.merge(emptyMap())
                buildReverseMap()
            }
        }
    }

    private fun buildReverseMap() {
        val source = labelMap ?: emptyMap()
        val reverse = HashMap<String, String>(source.size * 4)
        source.forEach { (english, translations) ->
            val en = english.trim()
            if (en.isNotBlank()) {
                reverse.putIfAbsent(en.lowercase(Locale.ROOT), en)
            }
            translations.values.forEach { localized ->
                val value = localized.trim()
                if (value.isNotBlank()) {
                    reverse.putIfAbsent(value.lowercase(Locale.ROOT), en)
                }
            }
        }
        reverseToEnglishMap = reverse
    }

    suspend fun normalizeToEnglish(context: Context, labels: List<String>): List<String> {
        if (labels.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            ensureMapLoaded(context)
            val reverse = reverseToEnglishMap.orEmpty()
            labels
                .mapNotNull { raw ->
                    val value = raw.trim()
                    if (value.isBlank()) return@mapNotNull null
                    reverse[value.lowercase(Locale.ROOT)] ?: value
                }
                .distinct()
        }
    }

    suspend fun analyze(context: Context, uri: Uri, langCode: String = "en"): List<String> {
        return analyzeDetailed(context, uri, langCode).labels
    }

    suspend fun analyzeDetailed(context: Context, uri: Uri, langCode: String = "en"): AnalyzeResult {
        return withContext(Dispatchers.IO) {
            ensureMapLoaded(context)
            analysisPermit.withPermit {
                var bitmap: Bitmap? = null
                try {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("ImageAnalyzer", "Analyzing with langCode: $langCode")
                    }
                    val sourceInfo = readLabelSourceInfo(context, uri)
                    // ML Kit rejects images under 32px on either side; icons and stickers have
                    // nothing to label anyway, so they count as analysed with no labels.
                    if (sourceInfo.width in 1 until MIN_LABEL_SIDE_PX || sourceInfo.height in 1 until MIN_LABEL_SIDE_PX) {
                        return@withPermit AnalyzeResult(emptyList())
                    }
                    val image = if (shouldUseDecodedBitmapPath(context, sourceInfo)) {
                        bitmap = decodeBitmapForLabeling(context, uri, sourceInfo)
                            ?: return@withPermit AnalyzeResult(
                                labels = emptyList(),
                                failureKind = if (canOpenUri(context, uri)) null else AnalyzeFailureKind.URI_UNRESOLVED
                            )
                        if (bitmap.width < MIN_LABEL_SIDE_PX || bitmap.height < MIN_LABEL_SIDE_PX) {
                            return@withPermit AnalyzeResult(emptyList())
                        }
                        InputImage.fromBitmap(bitmap, sourceInfo.rotationDegrees)
                    } else {
                        InputImage.fromFilePath(context, uri)
                    }
                    val labels = labelerMutex.withLock {
                        obtainSharedLabeler().process(image).await()
                    }
                    val rankedForRoast = SmartAiLabelRanker.rankForRoast(
                        labels = labels
                            .map { RankedMlLabel(text = it.text, confidence = it.confidence) }
                            .sortedByDescending { it.confidence },
                        limit = 3
                    )
                    val selected = if (rankedForRoast.isNotEmpty()) {
                        rankedForRoast
                    } else {
                        labels
                            .sortedByDescending { it.confidence }
                            .take(3)
                            .map { RankedMlLabel(text = it.text, confidence = it.confidence) }
                    }

                    // Keep the selected order for downstream roast logic, then localize if needed.
                    AnalyzeResult(
                        labels = selected.map { label ->
                            val text = label.text
                            val lowerText = text.lowercase(Locale.ROOT)

                            // Try exact match, then lowercase match
                            // Default to English text if no mapping found
                            if (langCode == "en") {
                                text
                            } else {
                                // Try to find key in map (case insensitive)
                                val translations = labelMap?.get(lowerText)
                                val mapped = translations?.get(langCode)

                                if (BuildConfig.DEBUG) {
                                    android.util.Log.d("ImageAnalyzer", "Label: '$text' -> '$lowerText', Found in map: ${translations != null}, Translation for $langCode: $mapped")
                                }

                                mapped ?: text // Fallback to original English text
                            }
                        }
                    )
                } catch (_: CancellationException) {
                    AnalyzeResult(emptyList())
                } catch (_: OutOfMemoryError) {
                    AnalyzeResult(emptyList())
                } catch (e: Exception) {
                    e.printStackTrace()
                    AppRuntimeLogStore.logError(context, "ImageAnalyzer", "Analyze failed", e)
                    AnalyzeResult(
                        labels = emptyList(),
                        failureKind = classifyFailureKind(e)
                    )
                } finally {
                    bitmap?.recycle()
                }
            }
        }
    }

    private fun classifyFailureKind(error: Exception): AnalyzeFailureKind? {
        if (isUriResolutionFailure(error)) {
            return AnalyzeFailureKind.URI_UNRESOLVED
        }
        return null
    }

    private fun isUriResolutionFailure(error: Exception): Boolean {
        if (error is SecurityException) return true
        val message = error.message.orEmpty().lowercase(Locale.ROOT)
        return message.contains("uri could not be resolved") ||
            message.contains("no content provider") ||
            message.contains("open failed") ||
            message.contains("no such file") ||
            message.contains("not found")
    }

    private fun canOpenUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun readLabelSourceInfo(context: Context, uri: Uri): LabelSourceInfo {
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        return LabelSourceInfo(
            width = bounds.outWidth,
            height = bounds.outHeight,
            mimeType = mimeType,
            rotationDegrees = readRotationDegrees(context, uri)
        )
    }

    private fun shouldUseDecodedBitmapPath(context: Context, sourceInfo: LabelSourceInfo): Boolean {
        if (MemorySafetyMode.shouldReduceBackgroundWork(context)) return true
        if (sourceInfo.width <= 0 || sourceInfo.height <= 0) return false
        if (isRawLikeMime(sourceInfo.mimeType)) return true
        return maxOf(sourceInfo.width, sourceInfo.height) >= LARGE_IMAGE_SIDE_PX ||
            sourceInfo.pixels >= LARGE_IMAGE_PIXELS
    }

    private fun isRawLikeMime(mimeType: String?): Boolean {
        val value = mimeType?.lowercase(Locale.ROOT).orEmpty()
        return value.contains("dng") || value.contains("raw")
    }

    private fun readRotationDegrees(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
    }

    private fun decodeBitmapForLabeling(
        context: Context,
        uri: Uri,
        sourceInfo: LabelSourceInfo
    ): Bitmap? {
        val width = sourceInfo.width
        val height = sourceInfo.height
        if (width <= 0 || height <= 0) return null

        var inSampleSize = 1
        while (true) {
            val sampledWidth = (width / inSampleSize).coerceAtLeast(1)
            val sampledHeight = (height / inSampleSize).coerceAtLeast(1)
            val maxSide = maxOf(sampledWidth, sampledHeight)
            val pixels = sampledWidth.toLong() * sampledHeight.toLong()
            if (maxSide <= MAX_LABEL_SIDE_PX && pixels <= MAX_LABEL_PIXELS) {
                break
            }
            inSampleSize *= 2
            if (inSampleSize >= 64) break
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }
}
