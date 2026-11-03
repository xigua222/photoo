package com.voicelike.app.stamp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

private const val STAMP_SOURCE_CONTEXT_PREFS_NAME = "stamp_source_context_prefs"
private const val STAMP_SOURCE_CONTEXT_PREFS_KEY = "stamp_source_context_v1"
private const val STAMP_SOURCE_CONTEXT_FOLDER = "stamp_source_context"
private const val STAMP_SOURCE_CONTEXT_MAX_SIDE = 2048
private val stampSourceContextVersion = MutableStateFlow(0)
private val stampSourceContextSequence = AtomicInteger(0)
@Volatile
private var stampSourceContextCacheRaw: String? = null
@Volatile
private var stampSourceContextCache: Map<String, StampSourceContextEntry>? = null

internal data class StampSourceContextEntry(
    val stampUri: String,
    val sourceUri: String,
    val sourceAspectRatio: Float,
    val stampLeftFraction: Float,
    val stampTopFraction: Float,
    val stampRightFraction: Float,
    val stampBottomFraction: Float
)

internal fun observeStampSourceContextVersion() = stampSourceContextVersion.asStateFlow()

internal fun loadStampSourceContext(
    context: Context,
    stampUri: String?
): StampSourceContextEntry? {
    if (stampUri.isNullOrBlank()) return null
    return readStampSourceContextEntries(context)[stampUri]
}

internal fun upsertStampSourceContext(
    context: Context,
    stampUri: String,
    sourceUri: String,
    sourceWidth: Int,
    sourceHeight: Int,
    stampViewport: RectF
) {
    if (stampUri.isBlank() || sourceUri.isBlank()) return
    val safeWidth = sourceWidth.coerceAtLeast(1).toFloat()
    val safeHeight = sourceHeight.coerceAtLeast(1).toFloat()
    val left = (stampViewport.left / safeWidth).coerceIn(0f, 0.999f)
    val top = (stampViewport.top / safeHeight).coerceIn(0f, 0.999f)
    val right = (stampViewport.right / safeWidth).coerceIn((left + 0.0001f).coerceAtMost(1f), 1f)
    val bottom = (stampViewport.bottom / safeHeight).coerceIn((top + 0.0001f).coerceAtMost(1f), 1f)
    val aspect = (safeWidth / safeHeight).coerceIn(0.35f, 2.85f)
    val entry = StampSourceContextEntry(
        stampUri = stampUri,
        sourceUri = sourceUri,
        sourceAspectRatio = aspect,
        stampLeftFraction = left,
        stampTopFraction = top,
        stampRightFraction = right,
        stampBottomFraction = bottom
    )
    val allEntries = readStampSourceContextEntries(context).toMutableMap()
    allEntries[stampUri] = entry
    writeStampSourceContextEntries(context, allEntries)
}

internal fun removeStampSourceContext(
    context: Context,
    stampUri: String?,
    deleteSourceAsset: Boolean = true
) {
    if (stampUri.isNullOrBlank()) return
    val allEntries = readStampSourceContextEntries(context).toMutableMap()
    val removed = allEntries.remove(stampUri) ?: return
    writeStampSourceContextEntries(context, allEntries)
    if (deleteSourceAsset) {
        deleteSourceAssetByUri(context, removed.sourceUri)
    }
}

internal fun saveStampSourceBitmapToLocalStore(
    context: Context,
    bitmap: Bitmap,
    maxSide: Int = STAMP_SOURCE_CONTEXT_MAX_SIDE,
    quality: Int = 88
): Uri? {
    if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
    val outputDir = File(context.filesDir, STAMP_SOURCE_CONTEXT_FOLDER)
    if (!outputDir.exists() && !outputDir.mkdirs()) {
        return null
    }
    val outputFile = File(outputDir, buildStampSourceContextFileName())
    var scaled: Bitmap? = null
    return runCatching {
        val sourceBitmap = if (max(bitmap.width, bitmap.height) > maxSide) {
            val scale = maxSide.toFloat() / max(bitmap.width, bitmap.height).toFloat()
            val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
            val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                scaled = it
            }
        } else {
            bitmap
        }
        FileOutputStream(outputFile).use { output ->
            if (!sourceBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(76, 96), output)) {
                throw IllegalStateException("Failed to encode source bitmap")
            }
            output.flush()
        }
        Uri.fromFile(outputFile)
    }.onFailure {
        runCatching { outputFile.delete() }
    }.getOrNull().also {
        val scaledBitmap = scaled
        if (scaledBitmap != null && scaledBitmap !== bitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }
    }
}

internal fun saveStampSourceFileToLocalStore(
    context: Context,
    sourceFile: File
): Uri? {
    if (!sourceFile.exists() || !sourceFile.isFile) return null
    val outputDir = File(context.filesDir, STAMP_SOURCE_CONTEXT_FOLDER)
    if (!outputDir.exists() && !outputDir.mkdirs()) {
        return null
    }
    val extension = sanitizeStampSourceContextExtension(sourceFile.extension)
    val outputFile = File(outputDir, buildStampSourceContextFileName(extension))
    return runCatching {
        sourceFile.inputStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        Uri.fromFile(outputFile)
    }.onFailure {
        runCatching { outputFile.delete() }
    }.getOrNull()
}

@Composable
internal fun StampSourceContextRecallCard(
    stampUri: String,
    stampPreviewBitmap: Bitmap?,
    sourceContext: StampSourceContextEntry,
    revealProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = revealProgress.coerceIn(0f, 1f)
    val sourcePainter = rememberAsyncImagePainter(model = sourceContext.sourceUri)
    val sourceReady = sourcePainter.state is AsyncImagePainter.State.Success
    val sourceAlpha = if (sourceReady) progress else 0f
    val stampAlpha = (1f - progress).coerceIn(0f, 1f)
    val fallbackStampBitmap = stampPreviewBitmap?.takeUnless { it.isRecycled }
    val fallbackStampPainter = if (fallbackStampBitmap == null) {
        rememberAsyncImagePainter(model = stampUri)
    } else {
        null
    }
    val shadowPainter = if (sourceReady) sourcePainter else fallbackStampPainter
    val castShadowFilter = remember {
        ColorFilter.tint(Color(0xFF2E261C).copy(alpha = 0.28f), blendMode = BlendMode.SrcIn)
    }
    val contactShadowFilter = remember {
        ColorFilter.tint(Color(0xFF3A3125).copy(alpha = 0.21f), blendMode = BlendMode.SrcIn)
    }
    val edgeShadeFilter = remember {
        ColorFilter.tint(Color(0xFF211A13).copy(alpha = 0.14f), blendMode = BlendMode.SrcIn)
    }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier.stampSourceContextClickable(onClick = onClick)
    ) {
        if (fallbackStampBitmap != null) {
            StampSourceContextBitmapLayer(
                bitmap = fallbackStampBitmap,
                colorFilter = castShadowFilter,
                filterQuality = FilterQuality.Low,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 1.6.dp, y = 5.6.dp)
                    .blur(9.8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            StampSourceContextBitmapLayer(
                bitmap = fallbackStampBitmap,
                colorFilter = contactShadowFilter,
                filterQuality = FilterQuality.Low,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 0.8.dp, y = 1.8.dp)
                    .blur(1.3.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            StampSourceContextBitmapLayer(
                bitmap = fallbackStampBitmap,
                colorFilter = edgeShadeFilter,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 0.5.dp, y = 0.72.dp)
            )
        } else if (shadowPainter != null) {
            StampSourceContextPainterLayer(
                painter = shadowPainter,
                colorFilter = castShadowFilter,
                filterQuality = FilterQuality.Low,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 1.6.dp, y = 5.6.dp)
                    .blur(9.8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            StampSourceContextPainterLayer(
                painter = shadowPainter,
                colorFilter = contactShadowFilter,
                filterQuality = FilterQuality.Low,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 0.8.dp, y = 1.8.dp)
                    .blur(1.3.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            StampSourceContextPainterLayer(
                painter = shadowPainter,
                colorFilter = edgeShadeFilter,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 0.5.dp, y = 0.72.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFFF1EBDD))
        ) {
            if (fallbackStampBitmap != null) {
                StampSourceContextBitmapLayer(
                    bitmap = fallbackStampBitmap,
                    colorFilter = null,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = stampAlpha }
                )
            } else if (fallbackStampPainter != null) {
                StampSourceContextPainterLayer(
                    painter = fallbackStampPainter,
                    colorFilter = null,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = stampAlpha }
                )
            }

            StampSourceContextPainterLayer(
                painter = sourcePainter,
                colorFilter = null,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sourceAlpha }
            )

            if (sourceAlpha > 0.01f) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = sourceAlpha }
                ) {
                    val left = (sourceContext.stampLeftFraction * size.width).coerceIn(0f, size.width)
                    val top = (sourceContext.stampTopFraction * size.height).coerceIn(0f, size.height)
                    val right = (sourceContext.stampRightFraction * size.width).coerceIn(left + 1f, size.width)
                    val bottom = (sourceContext.stampBottomFraction * size.height).coerceIn(top + 1f, size.height)
                    val rectSize = Size(
                        width = (right - left).coerceAtLeast(1f),
                        height = (bottom - top).coerceAtLeast(1f)
                    )
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.22f),
                        topLeft = Offset(left + 0.5.dp.toPx(), top + 0.5.dp.toPx()),
                        size = rectSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.92f),
                        topLeft = Offset(left, top),
                        size = rectSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

private fun readStampSourceContextEntries(
    context: Context
): Map<String, StampSourceContextEntry> {
    val prefs = context.applicationContext.getSharedPreferences(
        STAMP_SOURCE_CONTEXT_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    val raw = prefs.getString(STAMP_SOURCE_CONTEXT_PREFS_KEY, null).orEmpty()
    if (raw.isBlank()) return emptyMap()
    val cachedRaw = stampSourceContextCacheRaw
    val cachedEntries = stampSourceContextCache
    if (cachedEntries != null && cachedRaw == raw) {
        return cachedEntries
    }
    val parsedEntries = runCatching {
        val root = JSONObject(raw)
        val items = root.optJSONObject("items") ?: JSONObject()
        buildMap {
            items.keys().forEach { stampUri ->
                val item = items.optJSONObject(stampUri) ?: return@forEach
                val sourceUri = normalizeStampSourceUri(item.optString("sourceUri"))
                    ?: return@forEach
                val sourceAspectRatio = item.optDouble("sourceAspect", 1.0).toFloat()
                    .coerceIn(0.35f, 2.85f)
                val rectArray = item.optJSONArray("stampRect") ?: JSONArray()
                if (rectArray.length() < 4) return@forEach
                val left = rectArray.optDouble(0, 0.0).toFloat().coerceIn(0f, 0.999f)
                val top = rectArray.optDouble(1, 0.0).toFloat().coerceIn(0f, 0.999f)
                val right = rectArray.optDouble(2, 1.0).toFloat()
                    .coerceIn((left + 0.0001f).coerceAtMost(1f), 1f)
                val bottom = rectArray.optDouble(3, 1.0).toFloat()
                    .coerceIn((top + 0.0001f).coerceAtMost(1f), 1f)
                put(
                    stampUri,
                    StampSourceContextEntry(
                        stampUri = stampUri,
                        sourceUri = sourceUri,
                        sourceAspectRatio = sourceAspectRatio,
                        stampLeftFraction = left,
                        stampTopFraction = top,
                        stampRightFraction = right,
                        stampBottomFraction = bottom
                    )
                )
            }
        }
    }.getOrDefault(emptyMap())
    stampSourceContextCacheRaw = raw
    stampSourceContextCache = parsedEntries
    return parsedEntries
}

private fun normalizeStampSourceUri(rawSourceUri: String?): String? {
    val trimmed = rawSourceUri?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val parsed = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
    val scheme = parsed.scheme?.lowercase(Locale.US)
    return when {
        scheme.isNullOrEmpty() -> {
            val file = File(trimmed)
            if (file.exists() && file.isFile) {
                Uri.fromFile(file).toString()
            } else {
                null
            }
        }

        scheme == "file" -> {
            val path = parsed.path.orEmpty()
            if (path.isEmpty()) return null
            val file = File(path)
            if (file.exists() && file.isFile) {
                parsed.toString()
            } else {
                null
            }
        }

        else -> parsed.toString()
    }
}

private fun writeStampSourceContextEntries(
    context: Context,
    entries: Map<String, StampSourceContextEntry>
) {
    val root = JSONObject()
    val items = JSONObject()
    entries
        .entries
        .sortedBy { it.key }
        .forEach { (stampUri, entry) ->
            val item = JSONObject()
            item.put("sourceUri", entry.sourceUri)
            item.put("sourceAspect", entry.sourceAspectRatio)
            item.put(
                "stampRect",
                JSONArray().apply {
                    put(entry.stampLeftFraction)
                    put(entry.stampTopFraction)
                    put(entry.stampRightFraction)
                    put(entry.stampBottomFraction)
                }
            )
            items.put(stampUri, item)
        }
    root.put("items", items)
    val serialized = root.toString()
    context.applicationContext
        .getSharedPreferences(STAMP_SOURCE_CONTEXT_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(STAMP_SOURCE_CONTEXT_PREFS_KEY, serialized)
        .commit()
    stampSourceContextCacheRaw = serialized
    stampSourceContextCache = entries
    stampSourceContextVersion.value += 1
}

private fun deleteSourceAssetByUri(context: Context, sourceUri: String) {
    if (sourceUri.isBlank()) return
    val parsed = runCatching { Uri.parse(sourceUri) }.getOrNull() ?: return
    when (parsed.scheme?.lowercase(Locale.US)) {
        "file" -> {
            runCatching {
                parsed.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
        }
        "content" -> {
            runCatching { context.contentResolver.delete(parsed, null, null) }
        }
        else -> {
            runCatching {
                val file = File(sourceUri)
                if (file.exists()) file.delete()
            }
        }
    }
}

private fun buildStampSourceContextFileName(extension: String = "jpg"): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    val sequence = stampSourceContextSequence.getAndIncrement()
        .let { value ->
            if (value == Int.MAX_VALUE) {
                stampSourceContextSequence.getAndSet(0)
            } else {
                value
            }
        }
        .and(0x0FFF)
    val resolvedExtension = sanitizeStampSourceContextExtension(extension)
    return "photoo_stamp_source_${timestamp}_${sequence}.${resolvedExtension}"
}

private fun sanitizeStampSourceContextExtension(extension: String?): String {
    val normalized = extension
        ?.trim()
        ?.lowercase(Locale.US)
        .orEmpty()
        .trimStart('.')
    val valid = normalized.length in 2..5 && normalized.all { char ->
        char in 'a'..'z' || char in '0'..'9'
    }
    return if (valid) normalized else "jpg"
}

private fun Modifier.stampSourceContextClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    this.clickable(
        enabled = enabled,
        interactionSource = interaction,
        indication = null,
        onClick = onClick
    )
}

@Composable
private fun StampSourceContextBitmapLayer(
    bitmap: Bitmap,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality,
    modifier: Modifier = Modifier
) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
        modifier = modifier
    )
}

@Composable
private fun StampSourceContextPainterLayer(
    painter: Painter,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality,
    modifier: Modifier = Modifier
) {
    var loaded by remember(painter) { mutableStateOf(false) }
    val state = painter
    if (state is AsyncImagePainter) {
        loaded = state.state is AsyncImagePainter.State.Success
    }
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        colorFilter = colorFilter,
        modifier = modifier.graphicsLayer { alpha = if (loaded || colorFilter != null) 1f else 0f }
    )
}
