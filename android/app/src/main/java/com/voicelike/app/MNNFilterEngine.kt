package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.taobao.android.mnn.MNNForwardType
import com.taobao.android.mnn.MNNImageProcess
import com.taobao.android.mnn.MNNNetInstance
import com.taobao.android.mnn.MNNNetNative
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

private const val MNN_FILTER_MANIFEST_ASSET = "color_card_ai/mnn_filters.json"
private const val MNN_FILTER_CACHE_DIR = "mnn_filters"

private fun resolveMnnDisplayTitle(stem: String): String {
    return when (stem.trim().lowercase(Locale.US)) {
        "classic_neg" -> "NC"
        "v" -> "NV"
        else -> stem
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .split(Regex("\\s+"))
            .filter { token -> token.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { first ->
                    if (first.isLowerCase()) first.titlecase() else first.toString()
                }
            }
            .ifBlank { stem }
    }
}

internal enum class MNNColorTensorOrder {
    RGB,
    BGR
}

internal enum class MNNTensorLayout {
    NCHW,
    NHWC
}

internal data class MNNFilterAsset(
    val id: String,
    val title: String,
    val assetName: String,
    val inputTensorName: String? = null,
    val outputTensorName: String? = null,
    val inputColorOrder: MNNColorTensorOrder = MNNColorTensorOrder.RGB,
    val outputColorOrder: MNNColorTensorOrder = MNNColorTensorOrder.RGB,
    val inputLayout: MNNTensorLayout = MNNTensorLayout.NCHW,
    val outputLayout: MNNTensorLayout = MNNTensorLayout.NCHW,
    val threadCount: Int = 2
)

internal data class MNNFilterSeries(
    val id: String,
    val title: String,
    val filters: List<MNNFilterAsset>
)

internal suspend fun loadMNNFilterSeries(
    context: Context,
    manifestAssetName: String = MNN_FILTER_MANIFEST_ASSET
): MNNFilterSeries = withContext(Dispatchers.IO) {
    val fallback = MNNFilterSeries(
        id = "ai-mnn-series",
        title = "AI 调色",
        filters = emptyList()
    )

    runCatching {
        context.assets.open(manifestAssetName).bufferedReader().use { reader ->
            Gson().fromJson(reader, MNNFilterSeries::class.java)
        }
    }.getOrElse {
        fallback
    }
}

internal suspend fun loadMNNFilterSeriesSafe(
    context: Context,
    manifestAssetName: String = MNN_FILTER_MANIFEST_ASSET
): MNNFilterSeries = withContext(Dispatchers.IO) {
    val fallback = MNNFilterSeries(
        id = "ai-mnn-series",
        title = "AI 调色",
        filters = emptyList()
    )

    val fromManifest = runCatching {
        context.assets.open(manifestAssetName).bufferedReader().use { reader ->
            parseMnnFilterSeriesJson(JsonParser.parseReader(reader).asJsonObject)
        }
    }.getOrNull()

    if (fromManifest != null && fromManifest.filters.isNotEmpty()) {
        return@withContext fromManifest
    }

    val scannedFilters = runCatching {
        context.assets.list("color_card_ai/filters")
            ?.filter { assetName -> assetName.endsWith(".mnn", ignoreCase = true) }
            ?.sorted()
            ?.mapIndexed { index, fileName ->
                MNNFilterAsset(
                    id = "ai-mnn-${index + 1}",
                    title = resolveMnnDisplayTitle(fileName.substringBeforeLast('.')),
                    assetName = "color_card_ai/filters/$fileName"
                )
            }
            .orEmpty()
    }.getOrDefault(emptyList())

    if (scannedFilters.isNotEmpty()) {
        return@withContext fallback.copy(filters = scannedFilters)
    }

    fallback
}

private fun parseMnnFilterSeriesJson(json: JsonObject): MNNFilterSeries {
    val filters = json.getAsJsonArray("filters")
        ?.mapNotNull { entry ->
            val item = entry?.asJsonObject ?: return@mapNotNull null
            val id = item.getAsJsonPrimitive("id")?.asString?.trim().orEmpty()
            val assetName = item.getAsJsonPrimitive("assetName")?.asString?.trim().orEmpty()
            if (id.isBlank() || assetName.isBlank()) return@mapNotNull null
            MNNFilterAsset(
                id = id,
                title = item.getAsJsonPrimitive("title")?.asString?.trim().takeUnless { it.isNullOrBlank() }
                    ?: formatMnnFilterTitle(assetName.substringAfterLast('/')),
                assetName = assetName,
                inputTensorName = item.getAsJsonPrimitive("inputTensorName")?.asString?.trim().takeUnless { it.isNullOrBlank() },
                outputTensorName = item.getAsJsonPrimitive("outputTensorName")?.asString?.trim().takeUnless { it.isNullOrBlank() },
                inputColorOrder = when (item.getAsJsonPrimitive("inputColorOrder")?.asString?.trim()?.uppercase()) {
                    "BGR" -> MNNColorTensorOrder.BGR
                    else -> MNNColorTensorOrder.RGB
                },
                outputColorOrder = when (item.getAsJsonPrimitive("outputColorOrder")?.asString?.trim()?.uppercase()) {
                    "BGR" -> MNNColorTensorOrder.BGR
                    else -> MNNColorTensorOrder.RGB
                },
                inputLayout = when (item.getAsJsonPrimitive("inputLayout")?.asString?.trim()?.uppercase()) {
                    "NHWC" -> MNNTensorLayout.NHWC
                    else -> MNNTensorLayout.NCHW
                },
                outputLayout = when (item.getAsJsonPrimitive("outputLayout")?.asString?.trim()?.uppercase()) {
                    "NHWC" -> MNNTensorLayout.NHWC
                    else -> MNNTensorLayout.NCHW
                },
                threadCount = item.getAsJsonPrimitive("threadCount")?.asInt?.coerceAtLeast(1) ?: 2
            )
        }
        .orEmpty()

    return MNNFilterSeries(
        id = json.getAsJsonPrimitive("id")?.asString?.trim().takeUnless { it.isNullOrBlank() } ?: "ai-mnn-series",
        title = json.getAsJsonPrimitive("title")?.asString?.trim().takeUnless { it.isNullOrBlank() } ?: "AI 调色",
        filters = filters
    )
}

private fun formatMnnFilterTitle(fileName: String): String {
    val stem = fileName.substringBeforeLast('.')
    return stem
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .split(Regex("\\s+"))
        .filter { token -> token.isNotBlank() }
        .joinToString(" ") { token ->
            token.replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase() else first.toString()
            }
        }
        .ifBlank { stem.ifBlank { "AI" } }
}

internal class MNNFilterEngine(
    private val context: Context
) : Closeable {
    companion object {
        private const val TAG = "MNNFilterEngine"
        private val openClRejectedAssets = mutableSetOf<String>()

        private fun isOpenClRejected(asset: MNNFilterAsset): Boolean = synchronized(openClRejectedAssets) {
            asset.assetName in openClRejectedAssets
        }

        private fun rejectOpenCl(asset: MNNFilterAsset) {
            synchronized(openClRejectedAssets) {
                openClRejectedAssets += asset.assetName
            }
        }
    }

    private val appContext = context.applicationContext
    private val runtimeMutex = Mutex()
    private var loadedRuntime: LoadedRuntime? = null

    suspend fun switchFilter(filter: MNNFilterAsset?) {
        withContext(Dispatchers.Default) {
            runtimeMutex.withLock {
                releaseLocked()
                if (filter != null) {
                    loadedRuntime = loadRuntimeLocked(filter)
                }
            }
        }
    }

    suspend fun render(
        source: Bitmap,
        filter: MNNFilterAsset? = null
    ): Bitmap = runtimeMutex.withLock {
        val runtime = when {
            filter == null -> loadedRuntime ?: throw IllegalStateException("No active MNN filter")
            loadedRuntime?.asset == filter && !loadedRuntimeRequiresReplacement(loadedRuntime) ->
                loadedRuntime ?: throw IllegalStateException("No active MNN filter")
            else -> {
                releaseLocked()
                loadRuntimeLocked(filter).also { loadedRuntime = it }
            }
        }
        try {
            renderLocked(source, runtime, allowCpuFallback = true)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException || runtime.forwardType == MNNForwardType.FORWARD_CPU) {
                throw throwable
            }
            Log.w(
                TAG,
                "Fallback to CPU after ${runtime.forwardType} render exception for ${runtime.asset.id}",
                throwable
            )
            if (runtime.forwardType == MNNForwardType.FORWARD_OPENCL) {
                rejectOpenCl(runtime.asset)
            }
            renderWithCpuFallbackLocked(source, runtime.asset)
        }
    }

    suspend fun isRuntimeAvailable(): Boolean = withContext(Dispatchers.Default) {
        MNNNetNative.isRuntimeReady()
    }

    override fun close() {
        // Best-effort synchronous cleanup for call sites that do not already run in coroutines.
        loadedRuntime?.let { runtime ->
            runCatching { runtime.session.release() }
            runCatching { runtime.net.release() }
        }
        loadedRuntime = null
    }

    private suspend fun loadRuntimeLocked(filter: MNNFilterAsset): LoadedRuntime = withContext(Dispatchers.IO) {
        check(MNNNetNative.isRuntimeReady()) {
            val detail = MNNNetNative.getLoadError()?.message ?: "missing native runtime"
            "MNN runtime is not ready: $detail"
        }

        val modelFile = ensureModelFile(filter.assetName)
        val net = MNNNetInstance.createFromFile(modelFile.absolutePath)
            ?: throw IllegalStateException("Unable to create MNN net from ${filter.assetName}")
        try {
            val threadCount = filter.threadCount
                .coerceAtLeast(1)
                .coerceAtMost(Runtime.getRuntime().availableProcessors().coerceAtLeast(2))
            val sessionBundle = createPreferredSession(net, filter, threadCount)
                ?: throw IllegalStateException("Unable to create MNN session for ${filter.id}")
            val session = sessionBundle.session
            val inputTensor = session.getInput(filter.inputTensorName)
                ?: throw IllegalStateException("Missing input tensor for ${filter.id}")
            val outputTensor = session.getOutput(filter.outputTensorName)
                ?: throw IllegalStateException("Missing output tensor for ${filter.id}")
            LoadedRuntime(
                asset = filter,
                modelFile = modelFile,
                net = net,
                session = session,
                inputTensor = inputTensor,
                outputTensor = outputTensor,
                forwardType = sessionBundle.forwardType
            )
        } catch (throwable: Throwable) {
            runCatching { net.release() }
            throw throwable
        }
    }

    private fun createPreferredSession(
        net: MNNNetInstance,
        asset: MNNFilterAsset,
        threadCount: Int
    ): SessionBundle? {
        val forwardOrder = if (isOpenClRejected(asset)) {
            listOf(MNNForwardType.FORWARD_CPU)
        } else {
            listOf(
                MNNForwardType.FORWARD_OPENCL,
                MNNForwardType.FORWARD_CPU
            )
        }
        forwardOrder.forEach { forwardType ->
            val config = MNNNetInstance.Config().apply {
                this.forwardType = forwardType.type
                numThread = threadCount
            }
            val session = runCatching { net.createSession(config) }.getOrNull()
            if (session != null) {
                return SessionBundle(session = session, forwardType = forwardType)
            }
        }
        return null
    }

    private suspend fun renderLocked(
        source: Bitmap,
        runtime: LoadedRuntime,
        allowCpuFallback: Boolean
    ): Bitmap = withContext(Dispatchers.Default) {
        require(!source.isRecycled) { "Source bitmap is already recycled" }

        val safeSource = if (source.config == Bitmap.Config.ARGB_8888) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, false)
        }

        try {
            val width = safeSource.width.coerceAtLeast(1)
            val height = safeSource.height.coerceAtLeast(1)
            resizeSession(runtime, width, height)

            val processConfig = MNNImageProcess.Config().apply {
                this.source = MNNImageProcess.Format.RGBA
                this.dest = when (runtime.asset.inputColorOrder) {
                    MNNColorTensorOrder.RGB -> MNNImageProcess.Format.RGB
                    MNNColorTensorOrder.BGR -> MNNImageProcess.Format.BGR
                }
                this.filter = MNNImageProcess.Filter.BILINEAL
                this.mean = floatArrayOf(0f, 0f, 0f, 0f)
                this.normal = floatArrayOf(1f / 255f, 1f / 255f, 1f / 255f, 1f)
            }

            check(MNNImageProcess.convertBitmap(safeSource, runtime.inputTensor, processConfig, Matrix())) {
                "MNNImageProcess.convertBitmap failed for ${runtime.asset.id}"
            }

            runtime.session.run()

            val outputTensor = runtime.session.getOutput(runtime.asset.outputTensorName)
                ?: runtime.outputTensor
            val outputShape = outputTensor.getDimensions()
            val outputData = outputTensor.getFloatData()
            val outputInfo = resolveTensorShape(outputShape, width, height, runtime.asset.outputLayout)
            val hasInvalidOutput = hasInvalidTensorData(outputData)
            val shouldFallbackForIdentity = shouldFallbackBecauseOutputMatchesSource(
                sourceBitmap = safeSource,
                data = outputData,
                info = outputInfo,
                asset = runtime.asset
            )
            if (
                allowCpuFallback &&
                runtime.forwardType != MNNForwardType.FORWARD_CPU &&
                (
                    hasInvalidOutput ||
                        shouldFallbackToCpu(outputData, outputInfo, width, height) ||
                        shouldFallbackForIdentity
                    )
            ) {
                Log.w(
                    TAG,
                    "Fallback to CPU for ${runtime.asset.id}, forward=${runtime.forwardType}, shape=${outputShape.joinToString()}, source=${width}x$height, invalid=$hasInvalidOutput, identity=$shouldFallbackForIdentity"
                )
                if (runtime.forwardType == MNNForwardType.FORWARD_OPENCL) {
                    rejectOpenCl(runtime.asset)
                }
                return@withContext renderWithCpuFallbackLocked(source, runtime.asset)
            }
            decodeOutputBitmap(
                data = outputData,
                info = outputInfo,
                targetWidth = width,
                targetHeight = height,
                asset = runtime.asset
            )
        } finally {
            if (safeSource !== source && !safeSource.isRecycled) {
                safeSource.recycle()
            }
        }
    }

    private suspend fun renderWithCpuFallbackLocked(
        source: Bitmap,
        asset: MNNFilterAsset
    ): Bitmap {
        val modelFile = ensureModelFile(asset.assetName)
        val cpuRuntime = createRuntimeWithForward(asset, modelFile, MNNForwardType.FORWARD_CPU)
        return try {
            renderLocked(source, cpuRuntime, allowCpuFallback = false)
        } finally {
            runCatching { cpuRuntime.session.release() }
            runCatching { cpuRuntime.net.release() }
        }
    }

    private fun createRuntimeWithForward(
        asset: MNNFilterAsset,
        modelFile: File,
        forwardType: MNNForwardType
    ): LoadedRuntime {
        val net = MNNNetInstance.createFromFile(modelFile.absolutePath)
            ?: throw IllegalStateException("Unable to create MNN net from ${asset.assetName}")
        return try {
            val threadCount = asset.threadCount
                .coerceAtLeast(1)
                .coerceAtMost(Runtime.getRuntime().availableProcessors().coerceAtLeast(2))
            val config = MNNNetInstance.Config().apply {
                this.forwardType = forwardType.type
                numThread = threadCount
            }
            val session = net.createSession(config)
                ?: throw IllegalStateException("Unable to create ${forwardType.name} session for ${asset.id}")
            val inputTensor = session.getInput(asset.inputTensorName)
                ?: throw IllegalStateException("Missing input tensor for ${asset.id}")
            val outputTensor = session.getOutput(asset.outputTensorName)
                ?: throw IllegalStateException("Missing output tensor for ${asset.id}")
            LoadedRuntime(
                asset = asset,
                modelFile = modelFile,
                net = net,
                session = session,
                inputTensor = inputTensor,
                outputTensor = outputTensor,
                forwardType = forwardType
            )
        } catch (throwable: Throwable) {
            runCatching { net.release() }
            throw throwable
        }
    }

    private fun resizeSession(
        runtime: LoadedRuntime,
        width: Int,
        height: Int
    ) {
        val dims = when (runtime.asset.inputLayout) {
            MNNTensorLayout.NCHW -> intArrayOf(1, 3, height, width)
            MNNTensorLayout.NHWC -> intArrayOf(1, height, width, 3)
        }
        runtime.inputTensor.reshape(dims)
        runtime.session.reshape()
    }

    private fun decodeOutputBitmap(
        data: FloatArray,
        info: TensorShapeInfo,
        targetWidth: Int,
        targetHeight: Int,
        asset: MNNFilterAsset
    ): Bitmap {
        require(info.width > 0 && info.height > 0) { "Invalid tensor shape: ${info.width}x${info.height}" }

        val pixels = IntArray(info.width * info.height)
        for (y in 0 until info.height) {
            for (x in 0 until info.width) {
                val r = readChannel(data, info, x, y, 0, asset.outputColorOrder)
                val g = readChannel(data, info, x, y, 1, asset.outputColorOrder)
                val b = readChannel(data, info, x, y, 2, asset.outputColorOrder)
                val a = if (info.channels >= 4) readAlpha(data, info, x, y) else 255
                pixels[(y * info.width) + x] =
                    ((a and 0xFF) shl 24) or
                        ((r and 0xFF) shl 16) or
                        ((g and 0xFF) shl 8) or
                        (b and 0xFF)
            }
        }
        val decoded = Bitmap.createBitmap(pixels, info.width, info.height, Bitmap.Config.ARGB_8888)
        if (info.width == targetWidth && info.height == targetHeight) {
            return decoded
        }
        return Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also { scaled ->
            if (scaled !== decoded && !decoded.isRecycled) {
                decoded.recycle()
            }
        }
    }

    private fun shouldFallbackToCpu(
        data: FloatArray,
        info: TensorShapeInfo,
        targetWidth: Int,
        targetHeight: Int
    ): Boolean {
        if (data.isEmpty()) return true
        if (info.width <= 0 || info.height <= 0) return true
        if (info.width > targetWidth * 4 || info.height > targetHeight * 4) return true

        var sampleCount = 0
        var nonZeroCount = 0
        val step = (data.size / 4096).coerceAtLeast(1)
        var index = 0
        while (index < data.size) {
            val value = data[index]
            if (!value.isFinite()) {
                return true
            }
            if (kotlin.math.abs(value) > 1e-5f) {
                nonZeroCount++
            }
            sampleCount++
            index += step
        }
        return sampleCount == 0 || nonZeroCount == 0 || (sampleCount >= 32 && nonZeroCount <= 1)
    }

    private fun hasInvalidTensorData(data: FloatArray): Boolean {
        for (value in data) {
            if (!value.isFinite()) return true
        }
        return false
    }

    private fun shouldFallbackBecauseOutputMatchesSource(
        sourceBitmap: Bitmap,
        data: FloatArray,
        info: TensorShapeInfo,
        asset: MNNFilterAsset
    ): Boolean {
        if (info.width <= 0 || info.height <= 0) return false
        if (sourceBitmap.isRecycled) return false
        val sampleGrid = 12
        var diffSum = 0L
        var sampleCount = 0
        for (yIndex in 0 until sampleGrid) {
            val outY = ((yIndex + 0.5f) * info.height / sampleGrid).toInt().coerceIn(0, info.height - 1)
            val srcY = ((yIndex + 0.5f) * sourceBitmap.height / sampleGrid).toInt().coerceIn(0, sourceBitmap.height - 1)
            for (xIndex in 0 until sampleGrid) {
                val outX = ((xIndex + 0.5f) * info.width / sampleGrid).toInt().coerceIn(0, info.width - 1)
                val srcX = ((xIndex + 0.5f) * sourceBitmap.width / sampleGrid).toInt().coerceIn(0, sourceBitmap.width - 1)
                val outputR = readChannel(data, info, outX, outY, 0, asset.outputColorOrder)
                val outputG = readChannel(data, info, outX, outY, 1, asset.outputColorOrder)
                val outputB = readChannel(data, info, outX, outY, 2, asset.outputColorOrder)
                val sourceColor = sourceBitmap.getPixel(srcX, srcY)
                val sourceR = (sourceColor shr 16) and 0xFF
                val sourceG = (sourceColor shr 8) and 0xFF
                val sourceB = sourceColor and 0xFF
                diffSum += kotlin.math.abs(outputR - sourceR)
                diffSum += kotlin.math.abs(outputG - sourceG)
                diffSum += kotlin.math.abs(outputB - sourceB)
                sampleCount++
            }
        }
        if (sampleCount <= 0) return false
        val meanDiff = diffSum.toDouble() / (sampleCount.toDouble() * 3.0)
        return meanDiff < 2.0
    }

    private fun readChannel(
        data: FloatArray,
        info: TensorShapeInfo,
        x: Int,
        y: Int,
        rgbChannel: Int,
        colorOrder: MNNColorTensorOrder
    ): Int {
        val actualChannel = when (colorOrder) {
            MNNColorTensorOrder.RGB -> rgbChannel
            MNNColorTensorOrder.BGR -> when (rgbChannel) {
                0 -> 2
                2 -> 0
                else -> 1
            }
        }.coerceAtMost(info.channels - 1)
        val value = if (info.channels == 1) {
            readTensorValue(data, info, x, y, 0)
        } else {
            readTensorValue(data, info, x, y, actualChannel)
        }
        return normalizeColorChannel(value)
    }

    private fun readAlpha(
        data: FloatArray,
        info: TensorShapeInfo,
        x: Int,
        y: Int
    ): Int = normalizeColorChannel(readTensorValue(data, info, x, y, 3))

    private fun readTensorValue(
        data: FloatArray,
        info: TensorShapeInfo,
        x: Int,
        y: Int,
        channel: Int
    ): Float {
        val index = when (info.layout) {
            MNNTensorLayout.NCHW -> (channel * info.height * info.width) + (y * info.width) + x
            MNNTensorLayout.NHWC -> ((y * info.width) + x) * info.channels + channel
        }
        return data.getOrElse(index) { 0f }
    }

    private fun normalizeColorChannel(value: Float): Int {
        if (!value.isFinite()) return 0
        val scaled = if (value in -0.5f..1.5f) value * 255f else value
        if (!scaled.isFinite()) return 0
        return scaled.roundToInt().coerceIn(0, 255)
    }

    private fun resolveTensorShape(
        shape: IntArray,
        fallbackWidth: Int,
        fallbackHeight: Int,
        preferredLayout: MNNTensorLayout
    ): TensorShapeInfo {
        if (shape.size >= 4) {
            if (shape[1] in 1..4 && shape[2] > 0 && shape[3] > 0) {
                return TensorShapeInfo(shape[3], shape[2], shape[1], MNNTensorLayout.NCHW)
            }
            if (shape[3] in 1..4 && shape[1] > 0 && shape[2] > 0) {
                return TensorShapeInfo(shape[2], shape[1], shape[3], MNNTensorLayout.NHWC)
            }
        }
        if (shape.size == 3) {
            return when (preferredLayout) {
                MNNTensorLayout.NCHW -> TensorShapeInfo(shape[2], shape[1], shape[0], MNNTensorLayout.NCHW)
                MNNTensorLayout.NHWC -> TensorShapeInfo(shape[1], shape[0], shape[2], MNNTensorLayout.NHWC)
            }
        }
        val channels = when (preferredLayout) {
            MNNTensorLayout.NCHW -> 3
            MNNTensorLayout.NHWC -> 3
        }
        return TensorShapeInfo(fallbackWidth, fallbackHeight, channels, preferredLayout)
    }

    private suspend fun ensureModelFile(assetName: String): File = withContext(Dispatchers.IO) {
        val sourceName = assetName.substringAfterLast('/')
        val stem = sourceName.substringBeforeLast('.', sourceName)
        val extension = sourceName.substringAfterLast('.', "")
        val modelDir = File(appContext.cacheDir, MNN_FILTER_CACHE_DIR).apply {
            mkdirs()
        }
        val assetBytes = appContext.assets.open(assetName).use { input ->
            input.readBytes()
        }
        val assetDigest = MessageDigest.getInstance("SHA-256")
            .digest(assetBytes)
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(16)
        val versionedFileName = if (extension.isNotBlank()) {
            "${stem}_$assetDigest.$extension"
        } else {
            "${stem}_$assetDigest"
        }
        val target = File(modelDir, versionedFileName)
        if (!target.exists() || target.length() != assetBytes.size.toLong()) {
            FileOutputStream(target).use { output ->
                output.write(assetBytes)
            }
        }
        modelDir.listFiles()
            ?.filter { stale ->
                stale.name != target.name &&
                    stale.name.startsWith("${stem}_")
            }
            ?.forEach { stale ->
                runCatching { stale.delete() }
            }
        target
    }

    private fun releaseLocked() {
        loadedRuntime?.let { runtime ->
            runCatching { runtime.session.release() }
            runCatching { runtime.net.release() }
        }
        loadedRuntime = null
    }

    private fun loadedRuntimeRequiresReplacement(runtime: LoadedRuntime?): Boolean {
        return runtime?.forwardType == MNNForwardType.FORWARD_OPENCL &&
            isOpenClRejected(runtime.asset)
    }

    private data class LoadedRuntime(
        val asset: MNNFilterAsset,
        val modelFile: File,
        val net: MNNNetInstance,
        val session: MNNNetInstance.Session,
        val inputTensor: MNNNetInstance.Session.Tensor,
        val outputTensor: MNNNetInstance.Session.Tensor,
        val forwardType: MNNForwardType
    )

    private data class SessionBundle(
        val session: MNNNetInstance.Session,
        val forwardType: MNNForwardType
    )

    private data class TensorShapeInfo(
        val width: Int,
        val height: Int,
        val channels: Int,
        val layout: MNNTensorLayout
    )
}

internal class MNNFilterRenderPipeline(
    private val engine: MNNFilterEngine
) {
    suspend fun activate(filter: MNNFilterAsset?) {
        engine.switchFilter(filter)
    }

    suspend fun render(bitmap: Bitmap, filter: MNNFilterAsset? = null): Bitmap {
        return engine.render(bitmap, filter)
    }
}
