package com.voicelike.app

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

private const val COLOR_CARD_CHINESE_TRADITIONAL_ASSET = "watermark_chinese_colors.json"
private const val COLOR_CARD_CHINESE_PIGMENTS_ASSET = "watermark_chinese_pigments.json"
internal const val COLOR_CARD_CHINESE_TRADITIONAL_SOURCE_TAG = "中国传统色"
internal const val COLOR_CARD_CHINESE_PIGMENT_SOURCE_TAG = "国画颜料"
private const val COLOR_CARD_CHINESE_FONT_ASSET = "watermark_fonts/zhengqingke_chinese_colors_subset.ttf"
private const val COLOR_CARD_LEIKO_FONT_ASSET = "Leiko-Regular.otf"

data class ColorCardChineseInspirationEntry(
    val name: String,
    val color: Int,
    val page: Int,
    val sourceTag: String
)

private var cachedColorCardChineseInspiration: List<ColorCardChineseInspirationEntry>? = null
private var cachedColorCardChineseTypeface: Typeface? = null
private var cachedColorCardLeikoTypeface: Typeface? = null
private val colorCardChineseInspirationLock = Any()

internal fun loadColorCardChineseInspiration(context: Context): List<ColorCardChineseInspirationEntry> {
    cachedColorCardChineseInspiration?.let { return it }
    synchronized(colorCardChineseInspirationLock) {
        cachedColorCardChineseInspiration?.let { return it }
        val loaded = loadColorCardChineseInspirationAsset(
            context = context,
            assetName = COLOR_CARD_CHINESE_TRADITIONAL_ASSET,
            defaultSourceTag = COLOR_CARD_CHINESE_TRADITIONAL_SOURCE_TAG
        ) + loadColorCardChineseInspirationAsset(
            context = context,
            assetName = COLOR_CARD_CHINESE_PIGMENTS_ASSET,
            defaultSourceTag = COLOR_CARD_CHINESE_PIGMENT_SOURCE_TAG,
            pageOffset = 10_000
        )
        cachedColorCardChineseInspiration = loaded
        return loaded
    }
}

internal fun loadColorCardChineseTypeface(context: Context): Typeface? {
    cachedColorCardChineseTypeface?.let { return it }
    synchronized(COLOR_CARD_CHINESE_FONT_ASSET) {
        cachedColorCardChineseTypeface?.let { return it }
        val loaded = runCatching { Typeface.createFromAsset(context.assets, COLOR_CARD_CHINESE_FONT_ASSET) }.getOrNull()
        cachedColorCardChineseTypeface = loaded
        return loaded
    }
}

internal fun loadColorCardLeikoTypeface(context: Context): Typeface? {
    cachedColorCardLeikoTypeface?.let { return it }
    synchronized(COLOR_CARD_LEIKO_FONT_ASSET) {
        cachedColorCardLeikoTypeface?.let { return it }
        val loaded = runCatching { Typeface.createFromAsset(context.assets, COLOR_CARD_LEIKO_FONT_ASSET) }.getOrNull()
        cachedColorCardLeikoTypeface = loaded
        return loaded
    }
}

internal fun shouldUseColorCardLeiko(text: String): Boolean {
    return text.any { char -> char.isLetter() && char.code < 128 }
}

@Composable
internal fun ChineseColorNameText(
    text: String,
    color: Color,
    textSizeSp: Float,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    lineHeightSp: Float? = null,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val context = LocalContext.current
    val typeface = remember(context) { loadColorCardChineseTypeface(context) }
    AssetTypefaceText(
        text = text,
        color = color,
        textSizeSp = textSizeSp,
        modifier = modifier,
        typeface = typeface,
        maxLines = maxLines,
        lineHeightSp = lineHeightSp,
        textAlign = textAlign,
        fontWeight = fontWeight
    )
}

@Composable
internal fun LeikoText(
    text: String,
    color: Color,
    textSizeSp: Float,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    lineHeightSp: Float? = null,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val context = LocalContext.current
    val typeface = remember(context) { loadColorCardLeikoTypeface(context) }
    AssetTypefaceText(
        text = text,
        color = color,
        textSizeSp = textSizeSp,
        modifier = modifier,
        typeface = typeface,
        maxLines = maxLines,
        lineHeightSp = lineHeightSp,
        textAlign = textAlign,
        fontWeight = fontWeight
    )
}

@Composable
private fun AssetTypefaceText(
    text: String,
    color: Color,
    textSizeSp: Float,
    modifier: Modifier,
    typeface: Typeface?,
    maxLines: Int,
    lineHeightSp: Float?,
    textAlign: TextAlign?,
    fontWeight: FontWeight
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                includeFontPadding = false
                ellipsize = TextUtils.TruncateAt.END
            }
        },
        update = { textView ->
            textView.text = text
            textView.maxLines = maxLines
            textView.textSize = textSizeSp
            textView.setTextColor(color.toArgb())
            textView.typeface = typeface ?: Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textView.paint.isFakeBoldText = fontWeight.weight >= FontWeight.SemiBold.weight
            val lineSpacingExtra = (lineHeightSp?.let { target ->
                (target - textSizeSp).coerceAtLeast(0f)
            } ?: 0f)
            textView.setLineSpacing(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    lineSpacingExtra,
                    textView.resources.displayMetrics
                ),
                1f
            )
            textView.gravity = when (textAlign) {
                TextAlign.Center -> Gravity.CENTER_HORIZONTAL
                TextAlign.End,
                TextAlign.Right -> Gravity.END
                else -> Gravity.START
            }
        }
    )
}

private fun loadColorCardChineseInspirationAsset(
    context: Context,
    assetName: String,
    defaultSourceTag: String,
    pageOffset: Int = 0
): List<ColorCardChineseInspirationEntry> {
    return runCatching {
        context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { reader ->
            parseColorCardChineseInspiration(
                root = JSONObject(reader.readText()),
                defaultSourceTag = defaultSourceTag,
                pageOffset = pageOffset
            )
        }
    }.getOrElse { emptyList() }
}

private fun parseColorCardChineseInspiration(
    root: JSONObject,
    defaultSourceTag: String,
    pageOffset: Int = 0
): List<ColorCardChineseInspirationEntry> {
    val array = root.optJSONArray("colors") ?: return emptyList()
    val sourceTag = root.optString("sourceTag").trim().ifBlank { defaultSourceTag }
    return parseColorCardChineseInspirationColors(
        array = array,
        sourceTag = sourceTag,
        pageOffset = pageOffset
    )
}

private fun parseColorCardChineseInspirationColors(
    array: JSONArray,
    sourceTag: String,
    pageOffset: Int = 0
): List<ColorCardChineseInspirationEntry> {
    val result = ArrayList<ColorCardChineseInspirationEntry>(array.length())
    for (index in 0 until array.length()) {
        val obj = array.optJSONObject(index) ?: continue
        val name = obj.optString("name").trim()
        val colorInt = parseColorCardChineseColor(obj) ?: continue
        if (name.isBlank()) continue
        result += ColorCardChineseInspirationEntry(
            name = name,
            color = colorInt,
            page = obj.optInt("page", pageOffset + index),
            sourceTag = obj.optString("sourceTag").trim().ifBlank { sourceTag }
        )
    }
    return result
}

private fun parseColorCardChineseColor(obj: JSONObject): Int? {
    val hex = obj.optString("hex").trim()
    if (hex.isNotBlank()) {
        return runCatching { AndroidColor.parseColor(hex) }.getOrNull()
    }
    val rgbObject = obj.optJSONObject("rgb") ?: return null
    val red = rgbObject.optInt("r", -1)
    val green = rgbObject.optInt("g", -1)
    val blue = rgbObject.optInt("b", -1)
    if (red !in 0..255 || green !in 0..255 || blue !in 0..255) {
        return null
    }
    return AndroidColor.rgb(red, green, blue)
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255f).toInt().coerceIn(0, 255),
        (red * 255f).toInt().coerceIn(0, 255),
        (green * 255f).toInt().coerceIn(0, 255),
        (blue * 255f).toInt().coerceIn(0, 255)
    )
}
