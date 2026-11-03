package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.CancellationSignal
import android.os.Build
import android.util.Size
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.math.max

private const val DEEP_ORGANIZE_SCREENSHOT_MODEL_ASSET = "converted_tflite_quantized/model.tflite"
private const val DEEP_ORGANIZE_SCREENSHOT_LABEL_ASSET = "converted_tflite_quantized/labels.txt"
private const val DEEP_ORGANIZE_SCREENSHOT_CACHE_FILE = "deep_organize_screenshot_classifier_v2.json"
private const val DEEP_ORGANIZE_SCREENSHOT_CACHE_VERSION = 2
private const val DEEP_ORGANIZE_SCREENSHOT_CONFIDENCE_THRESHOLD = 0.70f
private const val DEEP_ORGANIZE_SCREENSHOT_DECODE_EDGE_MULTIPLIER = 4
private const val DEEP_ORGANIZE_SCREENSHOT_FALLBACK_INPUT_SIZE = 224
private const val DEEP_ORGANIZE_SCREENSHOT_PIPELINE_BUFFER_SIZE = 2
private const val DEEP_ORGANIZE_SCREENSHOT_LOG_TAG = "ScreenshotClassifier"

enum class DeepOrganizeScreenshotCategory {
    Chat,
    Order,
    Pay,
    Other
}

data class DeepOrganizeScreenshotClassifiedItem(
    val item: MediaItem,
    val category: DeepOrganizeScreenshotCategory,
    val confidence: Float,
    val rawLabel: String
)

data class DeepOrganizeScreenshotClassificationSummary(
    val signature: Long,
    val totalCount: Int,
    val groupedItems: Map<DeepOrganizeScreenshotCategory, List<DeepOrganizeScreenshotClassifiedItem>>,
    val fromCache: Boolean,
    val modelWorked: Boolean = true,
    val diagnosticMessage: String? = null
)

private data class DeepOrganizeScreenshotCacheEntry(
    val itemId: Long,
    val category: String,
    val confidence: Float,
    val rawLabel: String
)

private data class DeepOrganizeScreenshotCacheSnapshot(
    val signature: Long,
    val version: Int,
    val entries: List<DeepOrganizeScreenshotCacheEntry>,
    val updatedAtMs: Long
)

private data class DeepOrganizeScreenshotModelSpec(
    val inputWidth: Int,
    val inputHeight: Int,
    val inputType: DataType,
    val outputType: DataType,
    val outputClassCount: Int
)

private data class DeepOrganizeScreenshotInferenceResult(
    val rawLabel: String,
    val confidence: Float
)

private data class DeepOrganizeScreenshotPreparedInput(
    val item: MediaItem,
    val bitmap: Bitmap
)

private data class DeepOrganizeScreenshotPreparedEnvelope(
    val item: MediaItem,
    val prepared: DeepOrganizeScreenshotPreparedInput?
)

private data class DeepOrganizeScreenshotPipelineResult(
    val entries: List<DeepOrganizeScreenshotCacheEntry>,
    val preparedCount: Int,
    val inferenceCount: Int
)

internal fun localizedDeepOrganizeScreenshotClassifierTitle(): String {
    return if (Locale.getDefault().language.equals("zh", ignoreCase = true)) "截图分类" else "Screenshot Sort"
}

internal fun localizedDeepOrganizeScreenshotClassifierSubtitle(): String {
    return if (Locale.getDefault().language.equals("zh", ignoreCase = true)) {
        "批量识别截图并分到聊天、订单、支付和其他"
    } else {
        "Classify screenshots into chat, order, payment, and other"
    }
}

internal fun localizedDeepOrganizeScreenshotCategoryName(category: DeepOrganizeScreenshotCategory): String {
    val isZh = Locale.getDefault().language.equals("zh", ignoreCase = true)
    return when (category) {
        DeepOrganizeScreenshotCategory.Chat -> if (isZh) "聊天记录" else "Chat"
        DeepOrganizeScreenshotCategory.Order -> if (isZh) "订单取件" else "Orders"
        DeepOrganizeScreenshotCategory.Pay -> if (isZh) "支付交易" else "Payments"
        DeepOrganizeScreenshotCategory.Other -> if (isZh) "其他" else "Other"
    }
}

internal fun selectDeepOrganizeScreenshotCandidates(allMedia: List<MediaItem>): List<MediaItem> {
    return allMedia
        .asSequence()
        .filter(::shouldAnalyzeAsStandardPhoto)
        .filter(::isLikelyDeepOrganizeScreenshot)
        .sortedWith(
            compareByDescending<MediaItem> { it.dateAdded }
                .thenByDescending { it.id }
        )
        .toList()
}

internal suspend fun loadDeepOrganizeScreenshotClassificationSummary(
    context: Context,
    allMedia: List<MediaItem>
): DeepOrganizeScreenshotClassificationSummary? = withContext(Dispatchers.IO) {
    val candidates = selectDeepOrganizeScreenshotCandidates(allMedia)
    val signature = buildMediaScanSignature(candidates, 0)
    if (signature == 0L) {
        return@withContext DeepOrganizeScreenshotClassificationSummary(
            signature = 0L,
            totalCount = 0,
            groupedItems = emptyDeepOrganizeScreenshotGroups(),
            fromCache = true,
            modelWorked = true,
            diagnosticMessage = DeepOrganizeScreenshotClassifierEngine.diagnosticMessage()
        )
    }
    val snapshot = loadDeepOrganizeScreenshotCacheSnapshot(context) ?: return@withContext null
    if (snapshot.version != DEEP_ORGANIZE_SCREENSHOT_CACHE_VERSION || snapshot.signature != signature) {
        return@withContext null
    }
    buildDeepOrganizeScreenshotSummary(candidates, signature, snapshot.entries, fromCache = true)
}

internal suspend fun classifyDeepOrganizeScreenshots(
    context: Context,
    allMedia: List<MediaItem>,
    onProgress: (processed: Int, total: Int) -> Unit
): DeepOrganizeScreenshotClassificationSummary = withContext(Dispatchers.Default) {
    val candidates = selectDeepOrganizeScreenshotCandidates(allMedia)
    val signature = buildMediaScanSignature(candidates, 0)
    if (candidates.isEmpty()) {
        return@withContext DeepOrganizeScreenshotClassificationSummary(
            signature = 0L,
            totalCount = 0,
            groupedItems = emptyDeepOrganizeScreenshotGroups(),
            fromCache = false,
            modelWorked = true,
            diagnosticMessage = DeepOrganizeScreenshotClassifierEngine.diagnosticMessage()
        )
    }

    val total = candidates.size
    onProgress(0, total)
    val pipelineResult = runDeepOrganizeScreenshotPipeline(
        context = context,
        candidates = candidates,
        onProgress = onProgress
    )
    val entries = ArrayList<DeepOrganizeScreenshotCacheEntry>(pipelineResult.entries.size).apply {
        addAll(pipelineResult.entries)
    }
    val modelWorked = pipelineResult.inferenceCount > 0
    Log.d(
        DEEP_ORGANIZE_SCREENSHOT_LOG_TAG,
        "classification finished total=$total prepared=${pipelineResult.preparedCount} inferred=${pipelineResult.inferenceCount}"
    )
    val diagnosticMessage = DeepOrganizeScreenshotClassifierEngine.diagnosticMessage(
        preparedCount = pipelineResult.preparedCount,
        inferenceCount = pipelineResult.inferenceCount
    )

    withContext(Dispatchers.IO) {
        saveDeepOrganizeScreenshotCacheSnapshot(
            context = context,
            snapshot = DeepOrganizeScreenshotCacheSnapshot(
                signature = signature,
                version = DEEP_ORGANIZE_SCREENSHOT_CACHE_VERSION,
                entries = entries,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }
    buildDeepOrganizeScreenshotSummary(
        candidates,
        signature,
        entries,
        fromCache = false,
        modelWorked = modelWorked,
        diagnosticMessage = diagnosticMessage
    )
        ?: DeepOrganizeScreenshotClassificationSummary(
            signature = signature,
            totalCount = total,
            groupedItems = emptyDeepOrganizeScreenshotGroups(),
            fromCache = false,
            modelWorked = modelWorked,
            diagnosticMessage = diagnosticMessage
        )
}

private fun buildDeepOrganizeScreenshotSummary(
    candidates: List<MediaItem>,
    signature: Long,
    entries: List<DeepOrganizeScreenshotCacheEntry>,
    fromCache: Boolean,
    modelWorked: Boolean = true,
    diagnosticMessage: String? = null
): DeepOrganizeScreenshotClassificationSummary? {
    val itemsById = candidates.associateBy { it.id }
    if (itemsById.isEmpty()) {
        return DeepOrganizeScreenshotClassificationSummary(
            signature = signature,
            totalCount = 0,
            groupedItems = emptyDeepOrganizeScreenshotGroups(),
            fromCache = fromCache,
            modelWorked = modelWorked,
            diagnosticMessage = diagnosticMessage
        )
    }
    val grouped = emptyDeepOrganizeScreenshotGroups().toMutableMap()
    entries.forEach { entry ->
        val item = itemsById[entry.itemId] ?: return@forEach
        val category = runCatching { DeepOrganizeScreenshotCategory.valueOf(entry.category) }
            .getOrDefault(DeepOrganizeScreenshotCategory.Other)
        grouped[category] = grouped.getValue(category) + DeepOrganizeScreenshotClassifiedItem(
            item = item,
            category = category,
            confidence = entry.confidence,
            rawLabel = entry.rawLabel
        )
    }
    val groupedCount = grouped.values.sumOf { it.size }
    if (groupedCount == 0 && candidates.isNotEmpty()) return null
    return DeepOrganizeScreenshotClassificationSummary(
        signature = signature,
        totalCount = candidates.size,
        groupedItems = grouped.mapValues { (_, value) -> value.sortedByDescending { it.item.dateAdded } },
        fromCache = fromCache,
        modelWorked = modelWorked,
        diagnosticMessage = diagnosticMessage
    )
}

private fun emptyDeepOrganizeScreenshotGroups():
    LinkedHashMap<DeepOrganizeScreenshotCategory, List<DeepOrganizeScreenshotClassifiedItem>> {
    return linkedMapOf(
        DeepOrganizeScreenshotCategory.Chat to emptyList<DeepOrganizeScreenshotClassifiedItem>(),
        DeepOrganizeScreenshotCategory.Order to emptyList<DeepOrganizeScreenshotClassifiedItem>(),
        DeepOrganizeScreenshotCategory.Pay to emptyList<DeepOrganizeScreenshotClassifiedItem>(),
        DeepOrganizeScreenshotCategory.Other to emptyList<DeepOrganizeScreenshotClassifiedItem>()
    )
}

private fun resolveDeepOrganizeScreenshotCategory(
    inference: DeepOrganizeScreenshotInferenceResult?
): DeepOrganizeScreenshotCategory {
    if (inference == null || inference.confidence < DEEP_ORGANIZE_SCREENSHOT_CONFIDENCE_THRESHOLD) {
        return DeepOrganizeScreenshotCategory.Other
    }
    return when (inference.rawLabel.lowercase(Locale.ROOT)) {
        "chat" -> DeepOrganizeScreenshotCategory.Chat
        "order" -> DeepOrganizeScreenshotCategory.Order
        "pay" -> DeepOrganizeScreenshotCategory.Pay
        else -> DeepOrganizeScreenshotCategory.Other
    }
}

private fun loadDeepOrganizeScreenshotCacheSnapshot(context: Context): DeepOrganizeScreenshotCacheSnapshot? {
    val file = File(context.filesDir, DEEP_ORGANIZE_SCREENSHOT_CACHE_FILE)
    if (!file.exists()) return null
    return runCatching {
        val json = file.readText()
        if (json.isBlank()) return null
        parseDeepOrganizeScreenshotCacheSnapshot(json)
    }.onFailure {
        Log.w(DEEP_ORGANIZE_SCREENSHOT_LOG_TAG, "Unable to parse screenshot cache snapshot, clearing corrupted cache", it)
        runCatching { file.delete() }
    }.getOrNull()
}

private fun saveDeepOrganizeScreenshotCacheSnapshot(
    context: Context,
    snapshot: DeepOrganizeScreenshotCacheSnapshot
) {
    runCatching {
        val target = File(context.filesDir, DEEP_ORGANIZE_SCREENSHOT_CACHE_FILE)
        val tmp = File(context.filesDir, "$DEEP_ORGANIZE_SCREENSHOT_CACHE_FILE.tmp")
        val json = Gson().toJson(snapshot)
        tmp.writeText(json)
        if (!tmp.renameTo(target)) {
            target.writeText(json)
            runCatching { tmp.delete() }
        }
    }
}

private fun parseDeepOrganizeScreenshotCacheSnapshot(json: String): DeepOrganizeScreenshotCacheSnapshot? {
    val root = JsonParser.parseString(json)?.asJsonObject ?: return null
    val signature = root.get("signature")?.asLong ?: return null
    val version = root.get("version")?.asInt ?: return null
    val updatedAtMs = root.get("updatedAtMs")?.asLong ?: 0L
    val entriesArray = root.getAsJsonArray("entries") ?: return null
    val entries = ArrayList<DeepOrganizeScreenshotCacheEntry>(entriesArray.size())
    entriesArray.forEach { element ->
        val obj = element?.asJsonObject ?: return@forEach
        val itemId = obj.get("itemId")?.asLong ?: return@forEach
        val category = obj.get("category")?.asString ?: return@forEach
        val confidence = obj.get("confidence")?.asFloat ?: 0f
        val rawLabel = obj.get("rawLabel")?.asString.orEmpty()
        entries += DeepOrganizeScreenshotCacheEntry(
            itemId = itemId,
            category = category,
            confidence = confidence,
            rawLabel = rawLabel
        )
    }
    return DeepOrganizeScreenshotCacheSnapshot(
        signature = signature,
        version = version,
        entries = entries,
        updatedAtMs = updatedAtMs
    )
}

private fun normalizeDeepOrganizeScreenshotLabelLine(raw: String): String {
    val compact = raw.trim()
    if (compact.isBlank()) return compact
    val parts = compact.split(Regex("\\s+"))
    if (parts.size >= 2 && parts.first().all(Char::isDigit)) {
        return parts.drop(1).joinToString(" ").trim()
    }
    return compact
}

private fun isLikelyDeepOrganizeScreenshot(item: MediaItem): Boolean {
    val metadata = buildString {
        append(item.displayName)
        append(' ')
        append(item.bucketName)
        append(' ')
        append(item.path.orEmpty())
    }.lowercase(Locale.ROOT)
    return DEEP_ORGANIZE_SCREENSHOT_KEYWORDS.any(metadata::contains)
}

private val DEEP_ORGANIZE_SCREENSHOT_KEYWORDS = listOf(
    "screenshot", "screenshots", "screen_shot", "screen-shot", "screen shot",
    "screen capture", "screen_capture", "screencap", "截屏", "截图", "屏幕截图", "螢幕截圖", "スクリーンショット"
)

private object DeepOrganizeScreenshotClassifierEngine {
    private val lock = Any()
    @Volatile private var interpreter: Interpreter? = null
    @Volatile private var mappedModel: MappedByteBuffer? = null
    @Volatile private var labels: List<String>? = null
    @Volatile private var lastErrorMessage: String? = null

    suspend fun classify(context: Context, item: MediaItem): DeepOrganizeScreenshotInferenceResult? {
        val prepared = prepareInput(context, item) ?: return null
        return runInference(prepared)
    }

    fun diagnosticMessage(
        preparedCount: Int? = null,
        inferenceCount: Int? = null
    ): String? {
        val explicit = lastErrorMessage
        if (!explicit.isNullOrBlank()) return explicit
        if (preparedCount == 0) return "No inputs prepared. Interpreter or bitmap decode failed."
        if (inferenceCount == 0) return "Inputs were prepared but inference returned no result."
        return null
    }

    suspend fun prepareInput(
        context: Context,
        item: MediaItem
    ): DeepOrganizeScreenshotPreparedInput? = withContext(Dispatchers.IO) {
        lastErrorMessage = null
        val interpreter = getInterpreter(context) ?: return@withContext null
        val modelSpec = getModelSpec(interpreter)
        val bitmap = decodeBitmapForModel(context, item, modelSpec) ?: return@withContext null
        DeepOrganizeScreenshotPreparedInput(
            item = item,
            bitmap = bitmap
        )
    }

    suspend fun runInference(
        prepared: DeepOrganizeScreenshotPreparedInput
    ): DeepOrganizeScreenshotInferenceResult? = withContext(Dispatchers.Default) {
        val interpreter = interpreter ?: return@withContext null
        val labels = loadLabelsFromCache()
        try {
            runModel(interpreter, getModelSpec(interpreter), labels, prepared.bitmap)
        } catch (throwable: Throwable) {
            lastErrorMessage = throwable.message ?: throwable.javaClass.simpleName
            Log.w(DEEP_ORGANIZE_SCREENSHOT_LOG_TAG, "Inference failed", throwable)
            null
        } finally {
            if (!prepared.bitmap.isRecycled) prepared.bitmap.recycle()
        }
    }

    private fun getInterpreter(context: Context): Interpreter? {
        interpreter?.let { return it }
        synchronized(lock) {
            interpreter?.let { return it }
            val modelBuffer = mappedModel
                ?: mapModelFile(context, DEEP_ORGANIZE_SCREENSHOT_MODEL_ASSET)?.also { mappedModel = it }
                ?: loadModelBufferFallback(context, DEEP_ORGANIZE_SCREENSHOT_MODEL_ASSET)
                ?: return null
            return runCatching {
                Interpreter(
                    modelBuffer,
                    Interpreter.Options().apply {
                        setUseXNNPACK(true)
                        setNumThreads(1)
                    }
                )
            }.onFailure {
                lastErrorMessage = it.message ?: it.javaClass.simpleName
                Log.w(DEEP_ORGANIZE_SCREENSHOT_LOG_TAG, "Unable to create TFLite interpreter", it)
            }.getOrNull()?.also { interpreter = it }
        }
    }

    private fun getModelSpec(interpreter: Interpreter): DeepOrganizeScreenshotModelSpec {
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        return DeepOrganizeScreenshotModelSpec(
            inputWidth = inputShape.getOrNull(2) ?: DEEP_ORGANIZE_SCREENSHOT_FALLBACK_INPUT_SIZE,
            inputHeight = inputShape.getOrNull(1) ?: DEEP_ORGANIZE_SCREENSHOT_FALLBACK_INPUT_SIZE,
            inputType = interpreter.getInputTensor(0).dataType(),
            outputType = interpreter.getOutputTensor(0).dataType(),
            outputClassCount = outputShape.lastOrNull() ?: 4
        )
    }

    private fun loadLabels(context: Context): List<String> {
        labels?.let { return it }
        synchronized(lock) {
            labels?.let { return it }
            val loaded = runCatching {
                context.assets.open(DEEP_ORGANIZE_SCREENSHOT_LABEL_ASSET).bufferedReader().useLines { lines ->
                    lines
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map(::normalizeDeepOrganizeScreenshotLabelLine)
                        .toList()
                }
            }.getOrDefault(listOf("chat", "order", "pay", "others"))
            labels = loaded
            return loaded
        }
    }

    private fun loadLabelsFromCache(): List<String> {
        return labels ?: listOf("chat", "order", "pay", "others")
    }

    private fun runModel(
        interpreter: Interpreter,
        spec: DeepOrganizeScreenshotModelSpec,
        labels: List<String>,
        bitmap: Bitmap
    ): DeepOrganizeScreenshotInferenceResult? {
        val input = createInputBuffer(spec, bitmap)
        val outputCount = max(1, spec.outputClassCount)
        val scores = when (spec.outputType) {
            DataType.UINT8 -> {
                val output = Array(1) { ByteArray(outputCount) }
                interpreter.run(input, output)
                val quant = interpreter.getOutputTensor(0).quantizationParams()
                val scale = quant.scale.takeIf { it > 0f } ?: (1f / 255f)
                val zeroPoint = quant.zeroPoint
                FloatArray(outputCount) { index ->
                    ((output[0][index].toInt() and 0xFF) - zeroPoint) * scale
                }
            }
            else -> {
                val output = Array(1) { FloatArray(outputCount) }
                interpreter.run(input, output)
                output[0]
            }
        }
        val topIndex = scores.indices.maxByOrNull { scores[it] } ?: return null
        return DeepOrganizeScreenshotInferenceResult(
            rawLabel = labels.getOrElse(topIndex) { "others" },
            confidence = scores[topIndex].coerceIn(0f, 1f)
        )
    }

    private fun createInputBuffer(spec: DeepOrganizeScreenshotModelSpec, bitmap: Bitmap): Any {
        val pixels = IntArray(spec.inputWidth * spec.inputHeight)
        bitmap.getPixels(pixels, 0, spec.inputWidth, 0, 0, spec.inputWidth, spec.inputHeight)
        return when (spec.inputType) {
            DataType.UINT8 -> ByteBuffer.allocateDirect(spec.inputWidth * spec.inputHeight * 3).apply {
                order(ByteOrder.nativeOrder())
                pixels.forEach { pixel ->
                    put(((pixel shr 16) and 0xFF).toByte())
                    put(((pixel shr 8) and 0xFF).toByte())
                    put((pixel and 0xFF).toByte())
                }
                rewind()
            }
            else -> ByteBuffer.allocateDirect(spec.inputWidth * spec.inputHeight * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
                pixels.forEach { pixel ->
                    putFloat(((pixel shr 16) and 0xFF) / 255f)
                    putFloat(((pixel shr 8) and 0xFF) / 255f)
                    putFloat((pixel and 0xFF) / 255f)
                }
                rewind()
            }
        }
    }

    private fun decodeBitmapForModel(
        context: Context,
        item: MediaItem,
        spec: DeepOrganizeScreenshotModelSpec
    ): Bitmap? {
        val decodeEdge = max(spec.inputWidth, spec.inputHeight) * DEEP_ORGANIZE_SCREENSHOT_DECODE_EDGE_MULTIPLIER
        val sampled = decodeSampledBitmap(context, item, max(decodeEdge, DEEP_ORGANIZE_SCREENSHOT_FALLBACK_INPUT_SIZE))
            ?: return null
        val scaled = if (sampled.width == spec.inputWidth && sampled.height == spec.inputHeight) {
            sampled
        } else {
            buildCenterCropBitmap(sampled, spec.inputWidth, spec.inputHeight).also {
                if (it != sampled && !sampled.isRecycled) sampled.recycle()
            }
        }
        return if (scaled.config == Bitmap.Config.ARGB_8888) scaled else scaled.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun decodeSampledBitmap(context: Context, item: MediaItem, targetMaxEdge: Int): Bitmap? {
        val resolver = context.contentResolver
        loadThumbnailBitmap(resolver = resolver, item = item, targetMaxEdge = targetMaxEdge)?.let { thumb ->
            return thumb
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsDecoded = resolver.openInputStream(item.uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } != null
        if (!boundsDecoded) {
            lastErrorMessage = "Unable to decode bitmap bounds from screenshot uri."
            return null
        }
        val longEdge = max(bounds.outWidth.coerceAtLeast(1), bounds.outHeight.coerceAtLeast(1))
        var sampleSize = 1
        while (longEdge / sampleSize > targetMaxEdge.coerceAtLeast(1)) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                inPreferredColorSpace = null
            }
        }
        return resolver.openInputStream(item.uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }?.also {
            if (it == null) {
                lastErrorMessage = "Bitmap decode returned null."
            }
        } ?: run {
            lastErrorMessage = "Unable to open screenshot uri for decoding."
            null
        }
    }

    private fun loadThumbnailBitmap(
        resolver: android.content.ContentResolver,
        item: MediaItem,
        targetMaxEdge: Int
    ): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            resolver.loadThumbnail(
                item.uri,
                Size(targetMaxEdge.coerceAtLeast(1), targetMaxEdge.coerceAtLeast(1)),
                CancellationSignal()
            )
        }.onFailure {
            lastErrorMessage = it.message ?: it.javaClass.simpleName
            Log.w(DEEP_ORGANIZE_SCREENSHOT_LOG_TAG, "loadThumbnail failed for ${item.id}", it)
        }.getOrNull()
    }

    private fun mapModelFile(context: Context, assetName: String): MappedByteBuffer? {
        return runCatching {
            val descriptor = context.assets.openFd(assetName)
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
            }
        }.onFailure {
            lastErrorMessage = it.message ?: it.javaClass.simpleName
            Log.w(DEEP_ORGANIZE_SCREENSHOT_LOG_TAG, "Unable to mmap model asset, will fall back to direct buffer", it)
        }.getOrNull()
    }

    private fun loadModelBufferFallback(context: Context, assetName: String): ByteBuffer? {
        return runCatching {
            context.assets.open(assetName).use { input ->
                readAllToDirectBuffer(input)
            }
        }.onFailure {
            lastErrorMessage = it.message ?: it.javaClass.simpleName
            Log.w(DEEP_ORGANIZE_SCREENSHOT_LOG_TAG, "Unable to load model asset into direct buffer", it)
        }.getOrNull()
    }

    private fun readAllToDirectBuffer(input: InputStream): ByteBuffer {
        val bytes = input.readBytes()
        return ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            rewind()
        }
    }

    private fun buildCenterCropBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val safeTargetWidth = targetWidth.coerceAtLeast(1)
        val safeTargetHeight = targetHeight.coerceAtLeast(1)
        val scale = max(
            safeTargetWidth.toFloat() / source.width.coerceAtLeast(1).toFloat(),
            safeTargetHeight.toFloat() / source.height.coerceAtLeast(1).toFloat()
        )
        val scaledWidth = (source.width * scale).toInt().coerceAtLeast(safeTargetWidth)
        val scaledHeight = (source.height * scale).toInt().coerceAtLeast(safeTargetHeight)
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val cropX = ((scaled.width - safeTargetWidth) / 2).coerceAtLeast(0)
        val cropY = ((scaled.height - safeTargetHeight) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(
            scaled,
            cropX,
            cropY,
            safeTargetWidth.coerceAtMost(scaled.width - cropX),
            safeTargetHeight.coerceAtMost(scaled.height - cropY)
        ).also {
            if (scaled != source && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }
}

private suspend fun runDeepOrganizeScreenshotPipeline(
    context: Context,
    candidates: List<MediaItem>,
    onProgress: (processed: Int, total: Int) -> Unit
): DeepOrganizeScreenshotPipelineResult = supervisorScope {
    val preparedChannel = Channel<DeepOrganizeScreenshotPreparedEnvelope>(DEEP_ORGANIZE_SCREENSHOT_PIPELINE_BUFFER_SIZE)
    val resultChannel = Channel<DeepOrganizeScreenshotCacheEntry>(DEEP_ORGANIZE_SCREENSHOT_PIPELINE_BUFFER_SIZE)
    val total = candidates.size
    val preparedCounter = IntArray(1)
    val inferenceCounter = IntArray(1)

    val producer = launch(Dispatchers.IO) {
        candidates.forEach { item ->
            currentCoroutineContext().ensureActive()
            val prepared = DeepOrganizeScreenshotClassifierEngine.prepareInput(context, item)
            if (prepared != null) {
                preparedCounter[0] += 1
            }
            preparedChannel.send(
                DeepOrganizeScreenshotPreparedEnvelope(
                    item = item,
                    prepared = prepared
                )
            )
        }
        Log.d(
            DEEP_ORGANIZE_SCREENSHOT_LOG_TAG,
            "producer finished total=$total prepared=${preparedCounter[0]}"
        )
        preparedChannel.close()
    }

    val consumer = launch(Dispatchers.Default) {
        for (envelope in preparedChannel) {
            currentCoroutineContext().ensureActive()
            val prepared = envelope.prepared
            val inference = if (prepared != null) {
                DeepOrganizeScreenshotClassifierEngine.runInference(prepared)
            } else {
                null
            }
            val category = resolveDeepOrganizeScreenshotCategory(inference)
            if (inference != null) {
                inferenceCounter[0] += 1
            }
            resultChannel.send(
                DeepOrganizeScreenshotCacheEntry(
                    itemId = envelope.item.id,
                    category = category.name,
                    confidence = inference?.confidence ?: 0f,
                    rawLabel = inference?.rawLabel.orEmpty()
                )
            )
        }
        Log.d(
            DEEP_ORGANIZE_SCREENSHOT_LOG_TAG,
            "consumer finished total=$total inferred=${inferenceCounter[0]}"
        )
        resultChannel.close()
    }

    val collector = async(Dispatchers.Default) {
        val collected = ArrayList<DeepOrganizeScreenshotCacheEntry>(total)
        var processed = 0
        for (entry in resultChannel) {
            collected += entry
            processed += 1
            onProgress(processed, total)
        }
        DeepOrganizeScreenshotPipelineResult(
            entries = collected,
            preparedCount = preparedCounter[0],
            inferenceCount = inferenceCounter[0]
        )
    }

    joinAll(producer, consumer)
    collector.await()
}
