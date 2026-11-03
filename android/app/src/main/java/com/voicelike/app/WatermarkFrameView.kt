package com.voicelike.app

import androidx.compose.runtime.CompositionLocalProvider
import com.voicelike.app.liquidglass.rememberLiquidGlassBackdrop
import com.voicelike.app.liquidglass.LocalLiquidGlassBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import android.app.Activity
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.location.Geocoder
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.os.SystemClock
import android.text.TextUtils
import android.util.Base64
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropOriginal
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.voicelike.app.stamp.generateStampAutoCutoutForegroundBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class WatermarkTemplatePreset(
    val title: String,
    val subtitle: String
) {
    WatermarkFrame("水印边框", "把时间与光，都留在边缘"),
    BlurBackground("背景模糊", "让喧嚣后退，让主角发声"),
    ThinWhiteBorder("边框", "在留白里定义照片气质"),
    Floating("悬浮", "照片离开纸面，像记忆浮起"),
    Polaroid("拍立得", "纸张质感，留住即时心情"),
    FilmRetro("胶片复古", "旧颗粒里，藏着新情绪"),
    PaperDots("波点", "把照片打成一张会呼吸的纸孔"),
    DirectWatermark("极简", "不多一笔，只写关键"),
    FullColor("半屏色彩", "半屏色彩"),
    ColorPalette("色彩", "从照片里，长出一组颜色")
}

internal enum class WatermarkFrameLayoutMode(val title: String) {
    Center("居中"),
    Left("居左"),
    Right("居右")
}

internal enum class BorderCornerMode(val title: String) {
    Straight("直角"),
    InnerRounded("内圆角")
}

private val watermarkTemplatePreviewBitmapMemoryCache = ConcurrentHashMap<WatermarkTemplatePreset, Bitmap>()

private fun snapshotWatermarkTemplatePreviewBitmapMemoryCache(): Map<WatermarkTemplatePreset, Bitmap> {
    return buildMap {
        WatermarkTemplatePreset.entries.forEach { preset ->
            watermarkTemplatePreviewBitmapMemoryCache[preset]
                ?.takeIf { !it.isRecycled }
                ?.let { put(preset, it) }
        }
    }
}

private fun cacheWatermarkTemplatePreviewBitmaps(
    previews: Map<WatermarkTemplatePreset, Bitmap>
) {
    if (previews.isEmpty()) return
    previews.forEach { (preset, bitmap) ->
        if (bitmap.isRecycled) return@forEach
        watermarkTemplatePreviewBitmapMemoryCache[preset]
            ?.takeIf { existing -> existing !== bitmap && !existing.isRecycled }
            ?.recycle()
        watermarkTemplatePreviewBitmapMemoryCache[preset] = bitmap
    }
}

private fun isWatermarkTemplatePreviewBitmapCached(bitmap: Bitmap): Boolean {
    return watermarkTemplatePreviewBitmapMemoryCache.values.any { cached ->
        cached === bitmap && !cached.isRecycled
    }
}

private data class WatermarkTemplatePreviewSourceBitmap(
    val bitmap: Bitmap,
    val identity: String,
    val preserveAfterRender: Boolean
)

private suspend fun loadWatermarkTemplatePreviewSourceBitmap(
    context: Context,
    demoAsset: ColorCardFilterFeedDemoAsset?,
    maxSidePx: Int
): WatermarkTemplatePreviewSourceBitmap? {
    if (demoAsset != null) {
        decodeColorCardFilterFeedDemoBitmap(
            context = context,
            assetName = demoAsset.assetName,
            maxSidePx = maxSidePx
        )?.takeIf { !it.isRecycled }?.let { bitmap ->
            return WatermarkTemplatePreviewSourceBitmap(
                bitmap = bitmap,
                identity = demoAsset.id,
                preserveAfterRender = true
            )
        }
    }

    return withContext(Dispatchers.IO) {
        val decoded = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.watermark_template_demo,
            BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: return@withContext null
        val argbBitmap = if (decoded.config == Bitmap.Config.ARGB_8888) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, false).also { copy ->
                if (copy !== decoded) recycleBitmapQuietly(decoded)
            }
        }
        WatermarkTemplatePreviewSourceBitmap(
            bitmap = scaleWatermarkTemplatePreviewSourceBitmap(argbBitmap, maxSidePx),
            identity = "fallback-resource",
            preserveAfterRender = false
        )
    }
}

private fun scaleWatermarkTemplatePreviewSourceBitmap(
    bitmap: Bitmap,
    maxSidePx: Int
): Bitmap {
    val safeMaxSide = maxSidePx.coerceAtLeast(1)
    val sourceMaxSide = max(bitmap.width, bitmap.height).coerceAtLeast(1)
    if (sourceMaxSide <= safeMaxSide) return bitmap

    val scale = safeMaxSide.toFloat() / sourceMaxSide.toFloat()
    val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also { scaled ->
        if (scaled !== bitmap) recycleBitmapQuietly(bitmap)
    }
}

private fun localizedWatermarkFullColorTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "半屏色彩"
    AppLanguage.TraditionalChinese -> "半屏色彩"
    AppLanguage.Japanese -> "ハーフカラー"
    AppLanguage.Korean -> "하프 컬러"
    AppLanguage.French -> "Demi-couleur"
    AppLanguage.German -> "Halbfarbe"
    AppLanguage.Italian -> "Mezzo colore"
    AppLanguage.Spanish -> "Medio color"
    AppLanguage.Portuguese -> "Meia cor"
    AppLanguage.Russian -> "Полуцвет"
    AppLanguage.Thai -> "สีครึ่งภาพ"
    AppLanguage.Vietnamese -> "Màu nửa khung"
    AppLanguage.Indonesian -> "Warna setengah"
    AppLanguage.Arabic -> "لون نصفي"
    AppLanguage.Ukrainian -> "Кольорова половина"
    AppLanguage.Polish -> "Pół w kolorze"
    AppLanguage.Czech -> "Barevná půlka"
    AppLanguage.Dutch -> "Kleurhelft"
    AppLanguage.Swedish -> "Färghalva"
    AppLanguage.Danish -> "Halv farve"
    AppLanguage.Norwegian -> "Fargehalvdel"
    AppLanguage.Finnish -> "Väripuolisko"
    AppLanguage.Greek -> "Μισό χρώμα"
    AppLanguage.Romanian -> "Jumătate color"
    AppLanguage.Hungarian -> "Félig színes"
    AppLanguage.Turkish -> "Yarım renk"
    AppLanguage.Malay -> "Separuh Warna"
    AppLanguage.Hebrew -> "חצי צבע"
    AppLanguage.English -> "Half Color"
}

private fun localizedWatermarkFullColorSubtitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "半屏色彩"
    AppLanguage.TraditionalChinese -> "半屏色彩"
    AppLanguage.Japanese -> "ハーフカラー"
    AppLanguage.Korean -> "하프 컬러"
    AppLanguage.French -> "Demi-couleur"
    AppLanguage.German -> "Halbfarbe"
    AppLanguage.Italian -> "Mezzo colore"
    AppLanguage.Spanish -> "Medio color"
    AppLanguage.Portuguese -> "Meia cor"
    AppLanguage.Russian -> "Полуцвет"
    AppLanguage.Thai -> "สีครึ่งภาพ"
    AppLanguage.Vietnamese -> "Màu nửa khung"
    AppLanguage.Indonesian -> "Warna setengah"
    AppLanguage.Arabic -> "لون نصفي"
    AppLanguage.Ukrainian -> "Кольорова половина"
    AppLanguage.Polish -> "Pół w kolorze"
    AppLanguage.Czech -> "Barevná půlka"
    AppLanguage.Dutch -> "Kleurhelft"
    AppLanguage.Swedish -> "Färghalva"
    AppLanguage.Danish -> "Halv farve"
    AppLanguage.Norwegian -> "Fargehalvdel"
    AppLanguage.Finnish -> "Väripuolisko"
    AppLanguage.Greek -> "Μισό χρώμα"
    AppLanguage.Romanian -> "Jumătate color"
    AppLanguage.Hungarian -> "Félig színes"
    AppLanguage.Turkish -> "Yarım renk"
    AppLanguage.Malay -> "Separuh Warna"
    AppLanguage.Hebrew -> "חצי צבע"
    AppLanguage.English -> "Half Color"
}

private fun WatermarkTemplatePreset.localizedTitle(language: AppLanguage): String {
    val extraCopy = getWatermarkExtraCopy(language)
    return when (this) {
        WatermarkTemplatePreset.WatermarkFrame -> extraCopy.templateWatermarkFrame
        WatermarkTemplatePreset.BlurBackground -> extraCopy.templateBlurBackground
        WatermarkTemplatePreset.ThinWhiteBorder -> extraCopy.templateThinWhiteBorder
        WatermarkTemplatePreset.Floating -> extraCopy.templateFloating
        WatermarkTemplatePreset.Polaroid -> extraCopy.templatePolaroid
        WatermarkTemplatePreset.FilmRetro -> extraCopy.templateFilmRetro
        WatermarkTemplatePreset.PaperDots -> extraCopy.templatePaperDots
        WatermarkTemplatePreset.DirectWatermark -> extraCopy.templateDirectWatermark
        WatermarkTemplatePreset.FullColor -> localizedWatermarkFullColorTitle(language)
        WatermarkTemplatePreset.ColorPalette -> extraCopy.templateColorPalette
    }
}

private fun WatermarkTemplatePreset.localizedSubtitle(language: AppLanguage): String {
    val extraCopy = getWatermarkExtraCopy(language)
    return when (this) {
        WatermarkTemplatePreset.WatermarkFrame -> extraCopy.templateWatermarkFrameDesc
        WatermarkTemplatePreset.BlurBackground -> extraCopy.templateBlurBackgroundDesc
        WatermarkTemplatePreset.ThinWhiteBorder -> extraCopy.templateThinWhiteBorderDesc
        WatermarkTemplatePreset.Floating -> extraCopy.templateFloatingDesc
        WatermarkTemplatePreset.Polaroid -> extraCopy.templatePolaroidDesc
        WatermarkTemplatePreset.FilmRetro -> extraCopy.templateFilmRetroDesc
        WatermarkTemplatePreset.PaperDots -> extraCopy.templatePaperDotsDesc
        WatermarkTemplatePreset.DirectWatermark -> extraCopy.templateDirectWatermarkDesc
        WatermarkTemplatePreset.FullColor -> localizedWatermarkFullColorSubtitle(language)
        WatermarkTemplatePreset.ColorPalette -> extraCopy.templateColorPaletteDesc
    }
}

private fun WatermarkFrameLayoutMode.localizedTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> title
    AppLanguage.TraditionalChinese -> when (this) {
        WatermarkFrameLayoutMode.Center -> "置中"
        WatermarkFrameLayoutMode.Left -> "靠左"
        WatermarkFrameLayoutMode.Right -> "靠右"
    }
    AppLanguage.Japanese -> when (this) {
        WatermarkFrameLayoutMode.Center -> "中央"
        WatermarkFrameLayoutMode.Left -> "左寄せ"
        WatermarkFrameLayoutMode.Right -> "右寄せ"
    }
    AppLanguage.Korean -> when (this) {
        WatermarkFrameLayoutMode.Center -> "중앙"
        WatermarkFrameLayoutMode.Left -> "왼쪽"
        WatermarkFrameLayoutMode.Right -> "오른쪽"
    }
    AppLanguage.Ukrainian, AppLanguage.Polish, AppLanguage.Czech, AppLanguage.Dutch, AppLanguage.Swedish, AppLanguage.Danish, AppLanguage.Norwegian, AppLanguage.Finnish, AppLanguage.Greek, AppLanguage.Romanian, AppLanguage.Hungarian, AppLanguage.Turkish, AppLanguage.Malay, AppLanguage.Hebrew -> getWatermarkExtraCopy(language).let { copy ->
        when (this) {
            WatermarkFrameLayoutMode.Center -> copy.layoutCenter
            WatermarkFrameLayoutMode.Left -> copy.layoutLeft
            WatermarkFrameLayoutMode.Right -> copy.layoutRight
        }
    }
    AppLanguage.French, AppLanguage.German, AppLanguage.Italian, AppLanguage.Spanish,
    AppLanguage.Portuguese, AppLanguage.Russian, AppLanguage.Thai, AppLanguage.Vietnamese,
    AppLanguage.Indonesian, AppLanguage.Arabic, AppLanguage.English -> when (this) {
        WatermarkFrameLayoutMode.Center -> "Center"
        WatermarkFrameLayoutMode.Left -> "Left"
        WatermarkFrameLayoutMode.Right -> "Right"
    }
}

internal fun BorderCornerMode.localizedTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> title
    AppLanguage.TraditionalChinese -> when (this) {
        BorderCornerMode.Straight -> "直角"
        BorderCornerMode.InnerRounded -> "內圓角"
    }
    AppLanguage.Japanese -> when (this) {
        BorderCornerMode.Straight -> "直角"
        BorderCornerMode.InnerRounded -> "内丸"
    }
    AppLanguage.Korean -> when (this) {
        BorderCornerMode.Straight -> "직각"
        BorderCornerMode.InnerRounded -> "내부 둥근 모서리"
    }
    AppLanguage.Ukrainian, AppLanguage.Polish, AppLanguage.Czech, AppLanguage.Dutch, AppLanguage.Swedish, AppLanguage.Danish, AppLanguage.Norwegian, AppLanguage.Finnish, AppLanguage.Greek, AppLanguage.Romanian, AppLanguage.Hungarian, AppLanguage.Turkish, AppLanguage.Malay, AppLanguage.Hebrew -> getWatermarkExtraCopy(language).let { copy ->
        when (this) {
            BorderCornerMode.Straight -> copy.cornerStraight
            BorderCornerMode.InnerRounded -> copy.cornerInnerRounded
        }
    }
    AppLanguage.French, AppLanguage.German, AppLanguage.Italian, AppLanguage.Spanish,
    AppLanguage.Portuguese, AppLanguage.Russian, AppLanguage.Thai, AppLanguage.Vietnamese,
    AppLanguage.Indonesian, AppLanguage.Arabic, AppLanguage.English -> when (this) {
        BorderCornerMode.Straight -> "Straight"
        BorderCornerMode.InnerRounded -> "Inner Rounded"
    }
}

private fun resolveWatermarkPanelColorPaletteVariantOptions(
    style: WatermarkColorPaletteStyle,
    language: AppLanguage
): List<WatermarkColorPaletteVariantOption> {
    fun options(vararg labels: String): List<WatermarkColorPaletteVariantOption> =
        labels.mapIndexed { index, label -> WatermarkColorPaletteVariantOption(index, label) }
    fun glassOptions(singleLabel: String, doubleLabel: String): List<WatermarkColorPaletteVariantOption> =
        WatermarkGlassDotsPanelTone.entries.flatMap { tone ->
            listOf(
                WatermarkColorPaletteVariantOption(
                    resolveWatermarkGlassDotsLayoutVariantIndex(false, tone),
                    "${resolveWatermarkGlassDotsToneLabel(tone, language)} $singleLabel"
                ),
                WatermarkColorPaletteVariantOption(
                    resolveWatermarkGlassDotsLayoutVariantIndex(true, tone),
                    "${resolveWatermarkGlassDotsToneLabel(tone, language)} $doubleLabel"
                )
            )
        }

    return when (style) {
        WatermarkColorPaletteStyle.HexCard -> when (language) {
            AppLanguage.SimplifiedChinese -> options("默认", "矩形", "爱心", "圆形", "四边形")
            AppLanguage.TraditionalChinese -> options("預設", "矩形", "愛心", "圓形", "四邊形")
            AppLanguage.Japanese -> options("デフォルト", "長方形", "ハート", "円形", "四角形")
            AppLanguage.Korean -> options("기본", "직사각형", "하트", "원형", "사각형")
            else -> options("Default", "Rectangle", "Heart", "Circle", "Square")
        }
        WatermarkColorPaletteStyle.EditorialStrip -> when (language) {
            AppLanguage.SimplifiedChinese -> options("默认", "无边框")
            AppLanguage.TraditionalChinese -> options("預設", "無邊框")
            AppLanguage.Japanese -> options("デフォルト", "枠なし")
            AppLanguage.Korean -> options("기본", "테두리 없음")
            else -> options("Default", "No Border")
        }
        WatermarkColorPaletteStyle.CircleBands,
        WatermarkColorPaletteStyle.CenterStripes -> when (language) {
            AppLanguage.SimplifiedChinese -> options("连体", "断开")
            AppLanguage.TraditionalChinese -> options("連體", "斷開")
            AppLanguage.Japanese -> options("連結", "分離")
            AppLanguage.Korean -> options("연결", "분리")
            else -> options("Connected", "Separated")
        }
        WatermarkColorPaletteStyle.CenterBlocks -> when (language) {
            AppLanguage.SimplifiedChinese -> options("方块", "矩形", "爱心", "圆形", "四边形")
            AppLanguage.TraditionalChinese -> options("方塊", "矩形", "愛心", "圓形", "四邊形")
            AppLanguage.Japanese -> options("ブロック", "長方形", "ハート", "円形", "四角形")
            AppLanguage.Korean -> options("블록", "직사각형", "하트", "원형", "사각형")
            else -> options("Block", "Rectangle", "Heart", "Circle", "Square")
        }
        WatermarkColorPaletteStyle.GlassDots -> when (language) {
            AppLanguage.SimplifiedChinese -> glassOptions("单行", "双行")
            AppLanguage.TraditionalChinese -> glassOptions("單行", "雙行")
            AppLanguage.Japanese -> glassOptions("1行", "2行")
            AppLanguage.Korean -> glassOptions("한 줄", "두 줄")
            else -> glassOptions("Single", "Double")
        }
        else -> emptyList()
    }
}
private fun resolveWatermarkPanelColorPaletteStyleTitle(
    style: WatermarkColorPaletteStyle,
    language: AppLanguage
): String = when (language) {
    AppLanguage.SimplifiedChinese -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "色卡"
        WatermarkColorPaletteStyle.EditorialStrip -> "刊页"
        WatermarkColorPaletteStyle.PaletteNote -> "注记"
        WatermarkColorPaletteStyle.ChinesePalette -> "中国色其一"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "中国色其二"
        WatermarkColorPaletteStyle.MosaicGrid -> "马赛克矩形"
        WatermarkColorPaletteStyle.CircleBands -> "圆形色带"
        WatermarkColorPaletteStyle.BottomGradientBar -> "底部渐变条"
        WatermarkColorPaletteStyle.CenterStripes -> "横向色条"
        WatermarkColorPaletteStyle.CenterBlocks -> "块面拼贴"
        WatermarkColorPaletteStyle.GlassDots -> "玻璃面板"
    }
    AppLanguage.TraditionalChinese -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "色卡"
        WatermarkColorPaletteStyle.EditorialStrip -> "刊頁"
        WatermarkColorPaletteStyle.PaletteNote -> "註記"
        WatermarkColorPaletteStyle.ChinesePalette -> "中國色其一"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "中國色其二"
        WatermarkColorPaletteStyle.MosaicGrid -> "馬賽克矩形"
        WatermarkColorPaletteStyle.CircleBands -> "圓形色帶"
        WatermarkColorPaletteStyle.BottomGradientBar -> "底部漸變條"
        WatermarkColorPaletteStyle.CenterStripes -> "橫向色條"
        WatermarkColorPaletteStyle.CenterBlocks -> "塊面拼貼"
        WatermarkColorPaletteStyle.GlassDots -> "玻璃面板"
    }
    AppLanguage.Japanese -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "カラーカード"
        WatermarkColorPaletteStyle.EditorialStrip -> "エディトリアル"
        WatermarkColorPaletteStyle.PaletteNote -> "ノート"
        WatermarkColorPaletteStyle.ChinesePalette -> "中国色1"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "中国色2"
        WatermarkColorPaletteStyle.MosaicGrid -> "モザイク長方形"
        WatermarkColorPaletteStyle.CircleBands -> "円形バンド"
        WatermarkColorPaletteStyle.BottomGradientBar -> "下部グラデーションバー"
        WatermarkColorPaletteStyle.CenterStripes -> "中央横ストライプ"
        WatermarkColorPaletteStyle.CenterBlocks -> "中央ブロック"
        WatermarkColorPaletteStyle.GlassDots -> "ガラスドット"
    }
    AppLanguage.Korean -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "색상 카드"
        WatermarkColorPaletteStyle.EditorialStrip -> "에디토리얼"
        WatermarkColorPaletteStyle.PaletteNote -> "노트"
        WatermarkColorPaletteStyle.ChinesePalette -> "중국색 1"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "중국색 2"
        WatermarkColorPaletteStyle.MosaicGrid -> "모자이크 직사각형"
        WatermarkColorPaletteStyle.CircleBands -> "원형 밴드"
        WatermarkColorPaletteStyle.BottomGradientBar -> "하단 그라데이션 바"
        WatermarkColorPaletteStyle.CenterStripes -> "중앙 가로 스트라이프"
        WatermarkColorPaletteStyle.CenterBlocks -> "중앙 블록"
        WatermarkColorPaletteStyle.GlassDots -> "글래스 도트"
    }
    else -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "Color Card"
        WatermarkColorPaletteStyle.EditorialStrip -> "Editorial Strip"
        WatermarkColorPaletteStyle.PaletteNote -> "Palette Note"
        WatermarkColorPaletteStyle.ChinesePalette -> "Chinese Colors 1"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "Chinese Colors 2"
        WatermarkColorPaletteStyle.MosaicGrid -> "Mosaic Grid"
        WatermarkColorPaletteStyle.CircleBands -> "Circle Bands"
        WatermarkColorPaletteStyle.BottomGradientBar -> "Bottom Gradient Bar"
        WatermarkColorPaletteStyle.CenterStripes -> "Center Stripes"
        WatermarkColorPaletteStyle.CenterBlocks -> "Center Blocks"
        WatermarkColorPaletteStyle.GlassDots -> "Glass Panel"
    }
}
private fun resolveWatermarkPanelColorPaletteStyleSubtitle(
    style: WatermarkColorPaletteStyle,
    language: AppLanguage
): String = when (language) {
    AppLanguage.SimplifiedChinese -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "底部展示色卡，可切换色块形状与数值显示"
        WatermarkColorPaletteStyle.EditorialStrip -> "右侧竖栏排版，可选无边框刊页"
        WatermarkColorPaletteStyle.PaletteNote -> "右侧色板与标题并置的注记感版式"
        WatermarkColorPaletteStyle.ChinesePalette -> "匹配传统色谱，保留中文气质"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "单色题签，适合更克制的中国色"
        WatermarkColorPaletteStyle.MosaicGrid -> "多色拼成紧密矩形马赛克"
        WatermarkColorPaletteStyle.CircleBands -> "所有颜色围成圆形色带，可调整位置和条数"
        WatermarkColorPaletteStyle.BottomGradientBar -> "底部独立延展的渐变色条"
        WatermarkColorPaletteStyle.CenterStripes -> "横向堆叠的色条，可增减条数"
        WatermarkColorPaletteStyle.CenterBlocks -> "一字排开的色块，可切换多种形状"
        WatermarkColorPaletteStyle.GlassDots -> "原图底部叠一块玻璃面板，可显示 6 到 10 个色点"
    }
    AppLanguage.TraditionalChinese -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "底部展示色卡，可切換色塊形狀與數值顯示"
        WatermarkColorPaletteStyle.EditorialStrip -> "右側豎欄排版，可選無邊框刊頁"
        WatermarkColorPaletteStyle.PaletteNote -> "右側色板與標題並置的註記感版式"
        WatermarkColorPaletteStyle.ChinesePalette -> "匹配傳統色譜，保留中文氣質"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "單色題簽，適合更克制的中國色"
        WatermarkColorPaletteStyle.MosaicGrid -> "多色拼成緊密矩形馬賽克"
        WatermarkColorPaletteStyle.CircleBands -> "所有顏色圍成圓形色帶，可調整位置和條數"
        WatermarkColorPaletteStyle.BottomGradientBar -> "底部獨立延展的漸變色條"
        WatermarkColorPaletteStyle.CenterStripes -> "橫向堆疊的色條，可增減條數"
        WatermarkColorPaletteStyle.CenterBlocks -> "一字排開的色塊，可切換多種形狀"
        WatermarkColorPaletteStyle.GlassDots -> "原圖底部疊一塊玻璃面板，可顯示 6 到 10 個色點"
    }
    AppLanguage.Japanese -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "下部にカラーカードを表示、色ブロックの形状と数値表示を切り替え可能"
        WatermarkColorPaletteStyle.EditorialStrip -> "右側縦バーのレイアウト、枠なしエディトリアルも選択可能"
        WatermarkColorPaletteStyle.PaletteNote -> "右側のカラーパレットとタイトルを並べたノート風レイアウト"
        WatermarkColorPaletteStyle.ChinesePalette -> "伝統的な中国色譜に合わせ、中国らしい雰囲気を保持"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "単色ラベルで、より控えめな中国色に向く"
        WatermarkColorPaletteStyle.MosaicGrid -> "複数の色を密な長方形モザイクにまとめる"
        WatermarkColorPaletteStyle.CircleBands -> "すべての色を円形バンドに配置し、位置と本数を調整可能"
        WatermarkColorPaletteStyle.BottomGradientBar -> "下部に独立して広がるグラデーションバー"
        WatermarkColorPaletteStyle.CenterStripes -> "画面中央に横方向の色条を重ね、数を調整可能"
        WatermarkColorPaletteStyle.CenterBlocks -> "中央に一列で並ぶ色ブロック、形状切替に対応"
        WatermarkColorPaletteStyle.GlassDots -> "写真下部にガラスパネルを重ね、6〜10個のカラードットを見せる"
    }
    AppLanguage.Korean -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "하단에 색상 카드를 표시하고 색 블록 모양과 수치 표시를 전환할 수 있습니다"
        WatermarkColorPaletteStyle.EditorialStrip -> "오른쪽 세로 레이아웃, 테두리 없는 에디토리얼 선택 가능"
        WatermarkColorPaletteStyle.PaletteNote -> "오른쪽 팔레트와 제목을 나란히 둔 노트형 레이아웃"
        WatermarkColorPaletteStyle.ChinesePalette -> "전통 중국 색감을 맞추고 동양적 분위기를 유지합니다"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "단색 라벨로 더 절제된 중국색에 어울립니다"
        WatermarkColorPaletteStyle.MosaicGrid -> "여러 색을 촘촘한 직사각형 모자이크로 구성합니다"
        WatermarkColorPaletteStyle.CircleBands -> "모든 색을 원형 밴드로 배치하고 위치와 개수를 조절할 수 있습니다"
        WatermarkColorPaletteStyle.BottomGradientBar -> "하단에 독립적으로 펼쳐지는 그라데이션 바"
        WatermarkColorPaletteStyle.CenterStripes -> "중앙에 가로 색 띠를 쌓고 개수를 조절할 수 있습니다"
        WatermarkColorPaletteStyle.CenterBlocks -> "중앙에 일렬로 배치된 색 블록, 여러 모양 전환 가능"
        WatermarkColorPaletteStyle.GlassDots -> "사진 하단에 글래스 패널을 얹고 6~10개의 색상 점을 보여줍니다"
    }
    else -> when (style) {
        WatermarkColorPaletteStyle.HexCard -> "Show color card at bottom with switchable shape and value display"
        WatermarkColorPaletteStyle.EditorialStrip -> "Right-side vertical layout with optional borderless editorial"
        WatermarkColorPaletteStyle.PaletteNote -> "Right-side palette alongside title in a note-style layout"
        WatermarkColorPaletteStyle.ChinesePalette -> "Match traditional Chinese color spectrum with authentic feel"
        WatermarkColorPaletteStyle.ChinesePaletteMono -> "Single-color label for a more restrained Chinese palette"
        WatermarkColorPaletteStyle.MosaicGrid -> "Multiple colors arranged into a tight rectangular mosaic"
        WatermarkColorPaletteStyle.CircleBands -> "All colors form circular bands with adjustable position and count"
        WatermarkColorPaletteStyle.BottomGradientBar -> "Independently extending gradient bar at the bottom"
        WatermarkColorPaletteStyle.CenterStripes -> "Colors stacked horizontally at center with adjustable count"
        WatermarkColorPaletteStyle.CenterBlocks -> "Small color blocks lined up at center with switchable shapes"
        WatermarkColorPaletteStyle.GlassDots -> "A glass panel at the bottom of the photo with 6 to 10 color dots"
    }
}
private fun resolveWatermarkPanelColorPaletteMinimumCount(
    style: WatermarkColorPaletteStyle,
    variantIndex: Int
): Int = when (style) {
    WatermarkColorPaletteStyle.CircleBands -> 6
    WatermarkColorPaletteStyle.CenterStripes -> 7
    WatermarkColorPaletteStyle.CenterBlocks -> if (variantIndex == 2) 6 else 7
    WatermarkColorPaletteStyle.GlassDots -> 6
    else -> 0
}
private fun supportsWatermarkPanelColorPaletteItemCount(
    style: WatermarkColorPaletteStyle
): Boolean = style == WatermarkColorPaletteStyle.CircleBands ||
    style == WatermarkColorPaletteStyle.CenterStripes ||
    style == WatermarkColorPaletteStyle.CenterBlocks ||
    style == WatermarkColorPaletteStyle.GlassDots

private fun normalizeWatermarkPanelColorPaletteItemCount(
    style: WatermarkColorPaletteStyle,
    variantIndex: Int,
    rawCount: Int
): Int {
    val minimum = resolveWatermarkPanelColorPaletteMinimumCount(style, variantIndex).coerceAtLeast(0)
    val clamped = rawCount.coerceIn(minimum, 10)
    return if (style == WatermarkColorPaletteStyle.GlassDots) {
        normalizeWatermarkGlassDotsItemCount(clamped)
    } else {
        clamped
    }
}

private fun resolveWatermarkGlassDotsLayoutLabel(twoRows: Boolean, language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> if (twoRows) "双行" else "单行"
    AppLanguage.TraditionalChinese -> if (twoRows) "雙行" else "單行"
    AppLanguage.Japanese -> if (twoRows) "2列" else "1列"
    AppLanguage.Korean -> if (twoRows) "2줄" else "1줄"
    else -> if (twoRows) "Two Rows" else "One Row"
}

private fun resolveWatermarkGlassDotsToneLabel(
    tone: WatermarkGlassDotsPanelTone,
    language: AppLanguage
): String = when (language) {
    AppLanguage.SimplifiedChinese -> when (tone) {
        WatermarkGlassDotsPanelTone.Clear -> "无色"
        WatermarkGlassDotsPanelTone.Pearl -> "珍珠"
        WatermarkGlassDotsPanelTone.Smoke -> "雾灰"
        WatermarkGlassDotsPanelTone.Black -> "黑色"
        WatermarkGlassDotsPanelTone.White -> "白色"
        WatermarkGlassDotsPanelTone.Ocean -> "海蓝"
        WatermarkGlassDotsPanelTone.Rose -> "玫粉"
    }
    AppLanguage.TraditionalChinese -> when (tone) {
        WatermarkGlassDotsPanelTone.Clear -> "無色"
        WatermarkGlassDotsPanelTone.Pearl -> "珍珠"
        WatermarkGlassDotsPanelTone.Smoke -> "霧灰"
        WatermarkGlassDotsPanelTone.Black -> "黑色"
        WatermarkGlassDotsPanelTone.White -> "白色"
        WatermarkGlassDotsPanelTone.Ocean -> "海藍"
        WatermarkGlassDotsPanelTone.Rose -> "玫粉"
    }
    AppLanguage.Japanese -> when (tone) {
        WatermarkGlassDotsPanelTone.Clear -> "クリア"
        WatermarkGlassDotsPanelTone.Pearl -> "パール"
        WatermarkGlassDotsPanelTone.Smoke -> "スモーク"
        WatermarkGlassDotsPanelTone.Black -> "ブラック"
        WatermarkGlassDotsPanelTone.White -> "ホワイト"
        WatermarkGlassDotsPanelTone.Ocean -> "オーシャン"
        WatermarkGlassDotsPanelTone.Rose -> "ローズ"
    }
    AppLanguage.Korean -> when (tone) {
        WatermarkGlassDotsPanelTone.Clear -> "투명"
        WatermarkGlassDotsPanelTone.Pearl -> "펄"
        WatermarkGlassDotsPanelTone.Smoke -> "스모크"
        WatermarkGlassDotsPanelTone.Black -> "블랙"
        WatermarkGlassDotsPanelTone.White -> "화이트"
        WatermarkGlassDotsPanelTone.Ocean -> "오션"
        WatermarkGlassDotsPanelTone.Rose -> "로즈"
    }
    else -> when (tone) {
        WatermarkGlassDotsPanelTone.Clear -> "Clear"
        WatermarkGlassDotsPanelTone.Pearl -> "Pearl"
        WatermarkGlassDotsPanelTone.Smoke -> "Smoke"
        WatermarkGlassDotsPanelTone.Black -> "Black"
        WatermarkGlassDotsPanelTone.White -> "White"
        WatermarkGlassDotsPanelTone.Ocean -> "Ocean"
        WatermarkGlassDotsPanelTone.Rose -> "Rose"
    }
}

private enum class WatermarkFlowStep {
    Template,
    Preview,
    Crop,
    CopyrightProfile
}

private enum class WatermarkCustomizePanel(val title: String) {
    Template("模板"),
    LayoutPreset("版式"),
    GlassDots("玻璃"),
    PaperDotsLayout("布局"),
    PaperDotsBackground("底图"),
    PaperDotsDots("波点"),
    Crop("裁切"),
    Border("边框"),
    Logo("标识"),
    Params("参数"),
    Signature("个性签名"),
    Copyright("版权")
}

private fun WatermarkCustomizePanel.localizedTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> title
    AppLanguage.TraditionalChinese -> when (this) {
        WatermarkCustomizePanel.Template -> "模板"
        WatermarkCustomizePanel.LayoutPreset -> "版式"
        WatermarkCustomizePanel.GlassDots -> "玻璃"
        WatermarkCustomizePanel.PaperDotsLayout -> "拼接"
        WatermarkCustomizePanel.PaperDotsBackground -> "背景"
        WatermarkCustomizePanel.PaperDotsDots -> "紙孔"
        WatermarkCustomizePanel.Crop -> "裁切"
        WatermarkCustomizePanel.Logo -> "標識"
        WatermarkCustomizePanel.Params -> "參數"
        WatermarkCustomizePanel.Border -> "邊框"
        WatermarkCustomizePanel.Signature -> "個性簽名"
        WatermarkCustomizePanel.Copyright -> "版權"
    }
    AppLanguage.Japanese -> when (this) {
        WatermarkCustomizePanel.Template -> "テンプレート"
        WatermarkCustomizePanel.LayoutPreset -> "レイアウト"
        WatermarkCustomizePanel.GlassDots -> "ガラス"
        WatermarkCustomizePanel.PaperDotsLayout -> "スティッチ"
        WatermarkCustomizePanel.PaperDotsBackground -> "背景"
        WatermarkCustomizePanel.PaperDotsDots -> "ドット"
        WatermarkCustomizePanel.Crop -> "クロップ"
        WatermarkCustomizePanel.Logo -> "ロゴ"
        WatermarkCustomizePanel.Params -> "パラメータ"
        WatermarkCustomizePanel.Border -> "枠線"
        WatermarkCustomizePanel.Signature -> "署名"
        WatermarkCustomizePanel.Copyright -> "著作権"
    }
    AppLanguage.Korean -> when (this) {
        WatermarkCustomizePanel.Template -> "템플릿"
        WatermarkCustomizePanel.LayoutPreset -> "레이아웃"
        WatermarkCustomizePanel.GlassDots -> "글래스"
        WatermarkCustomizePanel.PaperDotsLayout -> "스티치"
        WatermarkCustomizePanel.PaperDotsBackground -> "배경"
        WatermarkCustomizePanel.PaperDotsDots -> "도트"
        WatermarkCustomizePanel.Crop -> "자르기"
        WatermarkCustomizePanel.Logo -> "로고"
        WatermarkCustomizePanel.Params -> "매개변수"
        WatermarkCustomizePanel.Border -> "테두리"
        WatermarkCustomizePanel.Signature -> "서명"
        WatermarkCustomizePanel.Copyright -> "저작권"
    }
    AppLanguage.French -> when (this) {
        WatermarkCustomizePanel.Template -> "Modèle"
        WatermarkCustomizePanel.LayoutPreset -> "Mise en page"
        WatermarkCustomizePanel.GlassDots -> "Verre"
        WatermarkCustomizePanel.PaperDotsLayout -> "Assemblage"
        WatermarkCustomizePanel.PaperDotsBackground -> "Arrière-plan"
        WatermarkCustomizePanel.PaperDotsDots -> "Points"
        WatermarkCustomizePanel.Crop -> "Recadrage"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Paramètres"
        WatermarkCustomizePanel.Border -> "Bordure"
        WatermarkCustomizePanel.Signature -> "Signature"
        WatermarkCustomizePanel.Copyright -> "Copyright"
    }
    AppLanguage.German -> when (this) {
        WatermarkCustomizePanel.Template -> "Vorlage"
        WatermarkCustomizePanel.LayoutPreset -> "Layout"
        WatermarkCustomizePanel.GlassDots -> "Glas"
        WatermarkCustomizePanel.PaperDotsLayout -> "Zusammensetzung"
        WatermarkCustomizePanel.PaperDotsBackground -> "Hintergrund"
        WatermarkCustomizePanel.PaperDotsDots -> "Punkte"
        WatermarkCustomizePanel.Crop -> "Zuschneiden"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parameter"
        WatermarkCustomizePanel.Border -> "Rahmen"
        WatermarkCustomizePanel.Signature -> "Signatur"
        WatermarkCustomizePanel.Copyright -> "Urheberrecht"
    }
    AppLanguage.Italian -> when (this) {
        WatermarkCustomizePanel.Template -> "Modello"
        WatermarkCustomizePanel.LayoutPreset -> "Layout"
        WatermarkCustomizePanel.GlassDots -> "Vetro"
        WatermarkCustomizePanel.PaperDotsLayout -> "Assemblaggio"
        WatermarkCustomizePanel.PaperDotsBackground -> "Sfondo"
        WatermarkCustomizePanel.PaperDotsDots -> "Punti"
        WatermarkCustomizePanel.Crop -> "Ritaglio"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parametri"
        WatermarkCustomizePanel.Border -> "Bordo"
        WatermarkCustomizePanel.Signature -> "Firma"
        WatermarkCustomizePanel.Copyright -> "Copyright"
    }
    AppLanguage.Spanish -> when (this) {
        WatermarkCustomizePanel.Template -> "Plantilla"
        WatermarkCustomizePanel.LayoutPreset -> "Disposición"
        WatermarkCustomizePanel.GlassDots -> "Vidrio"
        WatermarkCustomizePanel.PaperDotsLayout -> "Ensamblaje"
        WatermarkCustomizePanel.PaperDotsBackground -> "Fondo"
        WatermarkCustomizePanel.PaperDotsDots -> "Puntos"
        WatermarkCustomizePanel.Crop -> "Recorte"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parámetros"
        WatermarkCustomizePanel.Border -> "Borde"
        WatermarkCustomizePanel.Signature -> "Firma"
        WatermarkCustomizePanel.Copyright -> "Derechos de autor"
    }
    AppLanguage.Portuguese -> when (this) {
        WatermarkCustomizePanel.Template -> "Modelo"
        WatermarkCustomizePanel.LayoutPreset -> "Layout"
        WatermarkCustomizePanel.GlassDots -> "Vidro"
        WatermarkCustomizePanel.PaperDotsLayout -> "Montagem"
        WatermarkCustomizePanel.PaperDotsBackground -> "Plano de fundo"
        WatermarkCustomizePanel.PaperDotsDots -> "Pontos"
        WatermarkCustomizePanel.Crop -> "Corte"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parâmetros"
        WatermarkCustomizePanel.Border -> "Borda"
        WatermarkCustomizePanel.Signature -> "Assinatura"
        WatermarkCustomizePanel.Copyright -> "Direitos autorais"
    }
    AppLanguage.Russian -> when (this) {
        WatermarkCustomizePanel.Template -> "Шаблон"
        WatermarkCustomizePanel.LayoutPreset -> "Макет"
        WatermarkCustomizePanel.GlassDots -> "Стекло"
        WatermarkCustomizePanel.PaperDotsLayout -> "Компоновка"
        WatermarkCustomizePanel.PaperDotsBackground -> "Фон"
        WatermarkCustomizePanel.PaperDotsDots -> "Точки"
        WatermarkCustomizePanel.Crop -> "Кадрирование"
        WatermarkCustomizePanel.Logo -> "Логотип"
        WatermarkCustomizePanel.Params -> "Параметры"
        WatermarkCustomizePanel.Border -> "Рамка"
        WatermarkCustomizePanel.Signature -> "Подпись"
        WatermarkCustomizePanel.Copyright -> "Авторское право"
    }
    AppLanguage.Thai -> when (this) {
        WatermarkCustomizePanel.Template -> "เทมเพลต"
        WatermarkCustomizePanel.LayoutPreset -> "เลย์เอาต์"
        WatermarkCustomizePanel.GlassDots -> "กระจก"
        WatermarkCustomizePanel.PaperDotsLayout -> "สติช"
        WatermarkCustomizePanel.PaperDotsBackground -> "พื้นหลัง"
        WatermarkCustomizePanel.PaperDotsDots -> "จุด"
        WatermarkCustomizePanel.Crop -> "ครอปตัด"
        WatermarkCustomizePanel.Logo -> "โลโก้"
        WatermarkCustomizePanel.Params -> "พารามิเตอร์"
        WatermarkCustomizePanel.Border -> "ขอบ"
        WatermarkCustomizePanel.Signature -> "ลายเซ็น"
        WatermarkCustomizePanel.Copyright -> "ลิขสิทธิ์"
    }
    AppLanguage.Vietnamese -> when (this) {
        WatermarkCustomizePanel.Template -> "Mẫu"
        WatermarkCustomizePanel.LayoutPreset -> "Bố cục"
        WatermarkCustomizePanel.GlassDots -> "Kính"
        WatermarkCustomizePanel.PaperDotsLayout -> "Ghép nối"
        WatermarkCustomizePanel.PaperDotsBackground -> "Nền"
        WatermarkCustomizePanel.PaperDotsDots -> "Chấm"
        WatermarkCustomizePanel.Crop -> "Cắt"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Tham số"
        WatermarkCustomizePanel.Border -> "Viền"
        WatermarkCustomizePanel.Signature -> "Chữ ký"
        WatermarkCustomizePanel.Copyright -> "Bản quyền"
    }
    AppLanguage.Indonesian -> when (this) {
        WatermarkCustomizePanel.Template -> "Template"
        WatermarkCustomizePanel.LayoutPreset -> "Tata Letak"
        WatermarkCustomizePanel.GlassDots -> "Kaca"
        WatermarkCustomizePanel.PaperDotsLayout -> "Stitching"
        WatermarkCustomizePanel.PaperDotsBackground -> "Latar Belakang"
        WatermarkCustomizePanel.PaperDotsDots -> "Titik"
        WatermarkCustomizePanel.Crop -> "Potong"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parameter"
        WatermarkCustomizePanel.Border -> "Batas"
        WatermarkCustomizePanel.Signature -> "Tanda Tangan"
        WatermarkCustomizePanel.Copyright -> "Hak Cipta"
    }
    AppLanguage.Arabic -> when (this) {
        WatermarkCustomizePanel.Template -> "قالب"
        WatermarkCustomizePanel.LayoutPreset -> "تخطيط"
        WatermarkCustomizePanel.GlassDots -> "زجاج"
        WatermarkCustomizePanel.PaperDotsLayout -> "تجميع"
        WatermarkCustomizePanel.PaperDotsBackground -> "خلفية"
        WatermarkCustomizePanel.PaperDotsDots -> "نقاط"
        WatermarkCustomizePanel.Crop -> "قص"
        WatermarkCustomizePanel.Logo -> "شعار"
        WatermarkCustomizePanel.Params -> "معلمات"
        WatermarkCustomizePanel.Border -> "حدود"
        WatermarkCustomizePanel.Signature -> "توقيع"
        WatermarkCustomizePanel.Copyright -> "حقوق النشر"
    }
    AppLanguage.Ukrainian -> when (this) {
        WatermarkCustomizePanel.Template -> "Шаблон"
        WatermarkCustomizePanel.LayoutPreset -> "Макет"
        WatermarkCustomizePanel.GlassDots -> "Скло"
        WatermarkCustomizePanel.PaperDotsLayout -> "Компонування"
        WatermarkCustomizePanel.PaperDotsBackground -> "Фон"
        WatermarkCustomizePanel.PaperDotsDots -> "Точки"
        WatermarkCustomizePanel.Crop -> "Кадрування"
        WatermarkCustomizePanel.Logo -> "Логотип"
        WatermarkCustomizePanel.Params -> "Параметри"
        WatermarkCustomizePanel.Border -> "Рамка"
        WatermarkCustomizePanel.Signature -> "Підпис"
        WatermarkCustomizePanel.Copyright -> "Авторські права"
    }
    AppLanguage.Polish -> when (this) {
        WatermarkCustomizePanel.Template -> "Szablon"
        WatermarkCustomizePanel.LayoutPreset -> "Układ"
        WatermarkCustomizePanel.GlassDots -> "Szkło"
        WatermarkCustomizePanel.PaperDotsLayout -> "Kompozycja"
        WatermarkCustomizePanel.PaperDotsBackground -> "Tło"
        WatermarkCustomizePanel.PaperDotsDots -> "Kropki"
        WatermarkCustomizePanel.Crop -> "Kadrowanie"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parametry"
        WatermarkCustomizePanel.Border -> "Obramowanie"
        WatermarkCustomizePanel.Signature -> "Podpis"
        WatermarkCustomizePanel.Copyright -> "Prawa autorskie"
    }
    AppLanguage.Czech -> when (this) {
        WatermarkCustomizePanel.Template -> "Šablona"
        WatermarkCustomizePanel.LayoutPreset -> "Rozvržení"
        WatermarkCustomizePanel.GlassDots -> "Sklo"
        WatermarkCustomizePanel.PaperDotsLayout -> "Skladba"
        WatermarkCustomizePanel.PaperDotsBackground -> "Pozadí"
        WatermarkCustomizePanel.PaperDotsDots -> "Tečky"
        WatermarkCustomizePanel.Crop -> "Oříznutí"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parametry"
        WatermarkCustomizePanel.Border -> "Okraj"
        WatermarkCustomizePanel.Signature -> "Podpis"
        WatermarkCustomizePanel.Copyright -> "Autorská práva"
    }
    AppLanguage.Dutch -> when (this) {
        WatermarkCustomizePanel.Template -> "Sjabloon"
        WatermarkCustomizePanel.LayoutPreset -> "Opmaak"
        WatermarkCustomizePanel.GlassDots -> "Glas"
        WatermarkCustomizePanel.PaperDotsLayout -> "Compositie"
        WatermarkCustomizePanel.PaperDotsBackground -> "Achtergrond"
        WatermarkCustomizePanel.PaperDotsDots -> "Stippen"
        WatermarkCustomizePanel.Crop -> "Uitsnijden"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parameters"
        WatermarkCustomizePanel.Border -> "Rand"
        WatermarkCustomizePanel.Signature -> "Handtekening"
        WatermarkCustomizePanel.Copyright -> "Auteursrecht"
    }
    AppLanguage.Swedish -> when (this) {
        WatermarkCustomizePanel.Template -> "Mall"
        WatermarkCustomizePanel.LayoutPreset -> "Layout"
        WatermarkCustomizePanel.GlassDots -> "Glas"
        WatermarkCustomizePanel.PaperDotsLayout -> "Sammansättning"
        WatermarkCustomizePanel.PaperDotsBackground -> "Bakgrund"
        WatermarkCustomizePanel.PaperDotsDots -> "Prickar"
        WatermarkCustomizePanel.Crop -> "Beskärning"
        WatermarkCustomizePanel.Logo -> "Logga"
        WatermarkCustomizePanel.Params -> "Parametrar"
        WatermarkCustomizePanel.Border -> "Kant"
        WatermarkCustomizePanel.Signature -> "Signatur"
        WatermarkCustomizePanel.Copyright -> "Upphovsrätt"
    }
    AppLanguage.Danish -> when (this) {
        WatermarkCustomizePanel.Template -> "Skabelon"
        WatermarkCustomizePanel.LayoutPreset -> "Layout"
        WatermarkCustomizePanel.GlassDots -> "Glas"
        WatermarkCustomizePanel.PaperDotsLayout -> "Sammensætning"
        WatermarkCustomizePanel.PaperDotsBackground -> "Baggrund"
        WatermarkCustomizePanel.PaperDotsDots -> "Prikker"
        WatermarkCustomizePanel.Crop -> "Beskæring"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parametre"
        WatermarkCustomizePanel.Border -> "Kant"
        WatermarkCustomizePanel.Signature -> "Signatur"
        WatermarkCustomizePanel.Copyright -> "Ophavsret"
    }
    AppLanguage.Norwegian -> when (this) {
        WatermarkCustomizePanel.Template -> "Mal"
        WatermarkCustomizePanel.LayoutPreset -> "Oppsett"
        WatermarkCustomizePanel.GlassDots -> "Glass"
        WatermarkCustomizePanel.PaperDotsLayout -> "Sammensetning"
        WatermarkCustomizePanel.PaperDotsBackground -> "Bakgrunn"
        WatermarkCustomizePanel.PaperDotsDots -> "Prikker"
        WatermarkCustomizePanel.Crop -> "Beskjæring"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parametere"
        WatermarkCustomizePanel.Border -> "Kant"
        WatermarkCustomizePanel.Signature -> "Signatur"
        WatermarkCustomizePanel.Copyright -> "Opphavsrett"
    }
    AppLanguage.Finnish -> when (this) {
        WatermarkCustomizePanel.Template -> "Malli"
        WatermarkCustomizePanel.LayoutPreset -> "Asettelu"
        WatermarkCustomizePanel.GlassDots -> "Lasi"
        WatermarkCustomizePanel.PaperDotsLayout -> "Kokoonpano"
        WatermarkCustomizePanel.PaperDotsBackground -> "Tausta"
        WatermarkCustomizePanel.PaperDotsDots -> "Pisteet"
        WatermarkCustomizePanel.Crop -> "Rajaus"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Kuvausarvot"
        WatermarkCustomizePanel.Border -> "Reunus"
        WatermarkCustomizePanel.Signature -> "Allekirjoitus"
        WatermarkCustomizePanel.Copyright -> "Tekijänoikeus"
    }
    AppLanguage.Greek -> when (this) {
        WatermarkCustomizePanel.Template -> "Πρότυπο"
        WatermarkCustomizePanel.LayoutPreset -> "Διάταξη"
        WatermarkCustomizePanel.GlassDots -> "Γυαλί"
        WatermarkCustomizePanel.PaperDotsLayout -> "Σύνθεση"
        WatermarkCustomizePanel.PaperDotsBackground -> "Φόντο"
        WatermarkCustomizePanel.PaperDotsDots -> "Τελείες"
        WatermarkCustomizePanel.Crop -> "Περικοπή"
        WatermarkCustomizePanel.Logo -> "Λογότυπο"
        WatermarkCustomizePanel.Params -> "Παράμετροι"
        WatermarkCustomizePanel.Border -> "Περίγραμμα"
        WatermarkCustomizePanel.Signature -> "Υπογραφή"
        WatermarkCustomizePanel.Copyright -> "Πνευματικά δικαιώματα"
    }
    AppLanguage.Romanian -> when (this) {
        WatermarkCustomizePanel.Template -> "Șablon"
        WatermarkCustomizePanel.LayoutPreset -> "Aranjare"
        WatermarkCustomizePanel.GlassDots -> "Sticlă"
        WatermarkCustomizePanel.PaperDotsLayout -> "Asamblare"
        WatermarkCustomizePanel.PaperDotsBackground -> "Fundal"
        WatermarkCustomizePanel.PaperDotsDots -> "Puncte"
        WatermarkCustomizePanel.Crop -> "Decupare"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parametri"
        WatermarkCustomizePanel.Border -> "Chenar"
        WatermarkCustomizePanel.Signature -> "Semnătură"
        WatermarkCustomizePanel.Copyright -> "Drepturi de autor"
    }
    AppLanguage.Hungarian -> when (this) {
        WatermarkCustomizePanel.Template -> "Sablon"
        WatermarkCustomizePanel.LayoutPreset -> "Elrendezés"
        WatermarkCustomizePanel.GlassDots -> "Üveg"
        WatermarkCustomizePanel.PaperDotsLayout -> "Összeállítás"
        WatermarkCustomizePanel.PaperDotsBackground -> "Háttér"
        WatermarkCustomizePanel.PaperDotsDots -> "Pontok"
        WatermarkCustomizePanel.Crop -> "Vágás"
        WatermarkCustomizePanel.Logo -> "Logó"
        WatermarkCustomizePanel.Params -> "Beállítások"
        WatermarkCustomizePanel.Border -> "Szegély"
        WatermarkCustomizePanel.Signature -> "Aláírás"
        WatermarkCustomizePanel.Copyright -> "Szerzői jog"
    }
    AppLanguage.Turkish -> when (this) {
        WatermarkCustomizePanel.Template -> "Şablon"
        WatermarkCustomizePanel.LayoutPreset -> "Yerleşim"
        WatermarkCustomizePanel.GlassDots -> "Cam"
        WatermarkCustomizePanel.PaperDotsLayout -> "Birleşim"
        WatermarkCustomizePanel.PaperDotsBackground -> "Arka plan"
        WatermarkCustomizePanel.PaperDotsDots -> "Noktalar"
        WatermarkCustomizePanel.Crop -> "Kırpma"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Parametreler"
        WatermarkCustomizePanel.Border -> "Kenarlık"
        WatermarkCustomizePanel.Signature -> "İmza"
        WatermarkCustomizePanel.Copyright -> "Telif hakkı"
    }
    AppLanguage.Malay -> when (this) {
        WatermarkCustomizePanel.Template -> "Templat"
        WatermarkCustomizePanel.LayoutPreset -> "Susun Atur"
        WatermarkCustomizePanel.GlassDots -> "Kaca"
        WatermarkCustomizePanel.PaperDotsLayout -> "Gabungan"
        WatermarkCustomizePanel.PaperDotsBackground -> "Latar"
        WatermarkCustomizePanel.PaperDotsDots -> "Titik"
        WatermarkCustomizePanel.Crop -> "Pangkas"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Seting"
        WatermarkCustomizePanel.Border -> "Sempadan"
        WatermarkCustomizePanel.Signature -> "Tandatangan"
        WatermarkCustomizePanel.Copyright -> "Hak Cipta"
    }
    AppLanguage.Hebrew -> when (this) {
        WatermarkCustomizePanel.Template -> "תבנית"
        WatermarkCustomizePanel.LayoutPreset -> "פריסה"
        WatermarkCustomizePanel.GlassDots -> "זכוכית"
        WatermarkCustomizePanel.PaperDotsLayout -> "הרכבה"
        WatermarkCustomizePanel.PaperDotsBackground -> "רקע"
        WatermarkCustomizePanel.PaperDotsDots -> "נקודות"
        WatermarkCustomizePanel.Crop -> "חיתוך"
        WatermarkCustomizePanel.Logo -> "לוגו"
        WatermarkCustomizePanel.Params -> "הגדרות"
        WatermarkCustomizePanel.Border -> "שוליים"
        WatermarkCustomizePanel.Signature -> "חתימה"
        WatermarkCustomizePanel.Copyright -> "זכויות יוצרים"
    }
    AppLanguage.English -> when (this) {
        WatermarkCustomizePanel.Template -> "Template"
        WatermarkCustomizePanel.LayoutPreset -> "Layout"
        WatermarkCustomizePanel.GlassDots -> "Glass"
        WatermarkCustomizePanel.PaperDotsLayout -> "Stitch"
        WatermarkCustomizePanel.PaperDotsBackground -> "Background"
        WatermarkCustomizePanel.PaperDotsDots -> "Dots"
        WatermarkCustomizePanel.Crop -> "Crop"
        WatermarkCustomizePanel.Logo -> "Logo"
        WatermarkCustomizePanel.Params -> "Params"
        WatermarkCustomizePanel.Border -> "Border"
        WatermarkCustomizePanel.Signature -> "Signature"
        WatermarkCustomizePanel.Copyright -> "Copyright"
    }
}

private fun preferredWatermarkCustomizePanel(
    template: WatermarkTemplatePreset,
    isCutoutBreakoutMode: Boolean = false,
    isSubjectOutlineMode: Boolean = false
): WatermarkCustomizePanel = when {
    isCutoutBreakoutMode || isSubjectOutlineMode -> WatermarkCustomizePanel.Params
    else -> when (template) {
    WatermarkTemplatePreset.FullColor -> WatermarkCustomizePanel.Params
    WatermarkTemplatePreset.ColorPalette -> WatermarkCustomizePanel.LayoutPreset
    WatermarkTemplatePreset.PaperDots -> WatermarkCustomizePanel.PaperDotsLayout
    WatermarkTemplatePreset.FilmRetro,
    WatermarkTemplatePreset.Polaroid -> WatermarkCustomizePanel.Border
    else -> WatermarkCustomizePanel.Crop
    }
}

private fun isWatermarkCutoutBreakoutMode(
    template: WatermarkTemplatePreset,
    cutoutOverlayStyle: WatermarkCutoutOverlayStyle
): Boolean = template == WatermarkTemplatePreset.ThinWhiteBorder &&
    cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None

private fun isWatermarkSubjectOutlineMode(
    template: WatermarkTemplatePreset,
    cutoutOverlayStyle: WatermarkCutoutOverlayStyle,
    subjectOutlineEnabled: Boolean
): Boolean = template == WatermarkTemplatePreset.ThinWhiteBorder &&
    subjectOutlineEnabled &&
    cutoutOverlayStyle == WatermarkCutoutOverlayStyle.None

private fun preferredWatermarkCustomizePanelForCurrentState(
    template: WatermarkTemplatePreset,
    cutoutOverlayStyle: WatermarkCutoutOverlayStyle,
    subjectOutlineEnabled: Boolean
): WatermarkCustomizePanel = preferredWatermarkCustomizePanel(
    template = template,
    isCutoutBreakoutMode = isWatermarkCutoutBreakoutMode(
        template = template,
        cutoutOverlayStyle = cutoutOverlayStyle
    ),
    isSubjectOutlineMode = isWatermarkSubjectOutlineMode(
        template = template,
        cutoutOverlayStyle = cutoutOverlayStyle,
        subjectOutlineEnabled = subjectOutlineEnabled
    )
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun resolveSocialCropLabel(language: AppLanguage, key: String): String = when (key) {
    "ratio_1x1" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "方图"
        AppLanguage.Japanese -> "正方形"
        AppLanguage.Korean -> "정사각형"
        AppLanguage.French -> "Carré"
        AppLanguage.German -> "Quadrat"
        AppLanguage.Italian -> "Quadrato"
        AppLanguage.Spanish -> "Cuadrado"
        AppLanguage.Portuguese -> "Quadrado"
        AppLanguage.Russian -> "Квадрат"
        AppLanguage.Thai -> "สี่เหลี่ยม"
        AppLanguage.Vietnamese -> "Vuông"
        AppLanguage.Indonesian -> "Persegi"
        AppLanguage.Arabic -> "مربع"
        AppLanguage.Ukrainian -> "Квадрат"
        AppLanguage.Polish -> "Kwadrat"
        AppLanguage.Czech -> "Čtverec"
        AppLanguage.Dutch -> "Vierkant"
        AppLanguage.Swedish -> "Kvadrat"
        AppLanguage.Danish -> "Kvadrat"
        AppLanguage.Norwegian -> "Kvadrat"
        AppLanguage.Finnish -> "Neliö"
        AppLanguage.Greek -> "Τετράγωνο"
        AppLanguage.Romanian -> "Pătrat"
        AppLanguage.Hungarian -> "Négyzet"
        AppLanguage.Turkish -> "Kare"
        AppLanguage.Malay -> "Segi Empat Sama"
        AppLanguage.Hebrew -> "ריבוע"
        else -> "Square"
    }
    "ratio_4x3" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "横图"
        AppLanguage.Japanese -> "横長"
        AppLanguage.Korean -> "가로형"
        AppLanguage.French -> "Paysage"
        AppLanguage.German -> "Querformat"
        AppLanguage.Italian -> "Orizzontale"
        AppLanguage.Spanish -> "Horizontal"
        AppLanguage.Portuguese -> "Paisagem"
        AppLanguage.Russian -> "Горизонталь"
        AppLanguage.Thai -> "แนวนอน"
        AppLanguage.Vietnamese -> "Ngang"
        AppLanguage.Indonesian -> "Lanskap"
        AppLanguage.Arabic -> "أفقي"
        AppLanguage.Ukrainian -> "Альбомна"
        AppLanguage.Polish -> "Poziomo"
        AppLanguage.Czech -> "Na šířku"
        AppLanguage.Dutch -> "Liggend"
        AppLanguage.Swedish -> "Liggande"
        AppLanguage.Danish -> "Liggende"
        AppLanguage.Norwegian -> "Liggende"
        AppLanguage.Finnish -> "Vaaka"
        AppLanguage.Greek -> "Οριζόντιο"
        AppLanguage.Romanian -> "Peisaj"
        AppLanguage.Hungarian -> "Fekvő"
        AppLanguage.Turkish -> "Yatay"
        AppLanguage.Malay -> "Landskap"
        AppLanguage.Hebrew -> "לרוחב"
        else -> "Landscape"
    }
    "ratio_3x4" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "竖图"
        AppLanguage.Japanese -> "縦長"
        AppLanguage.Korean -> "세로형"
        AppLanguage.French -> "Portrait"
        AppLanguage.German -> "Hochformat"
        AppLanguage.Italian -> "Verticale"
        AppLanguage.Spanish -> "Vertical"
        AppLanguage.Portuguese -> "Retrato"
        AppLanguage.Russian -> "Портрет"
        AppLanguage.Thai -> "แนวตั้ง"
        AppLanguage.Vietnamese -> "Dọc"
        AppLanguage.Indonesian -> "Potret"
        AppLanguage.Arabic -> "عمودي"
        AppLanguage.Ukrainian -> "Портретна"
        AppLanguage.Polish -> "Pionowo"
        AppLanguage.Czech -> "Na výšku"
        AppLanguage.Dutch -> "Staand"
        AppLanguage.Swedish -> "Stående"
        AppLanguage.Danish -> "Stående"
        AppLanguage.Norwegian -> "Stående"
        AppLanguage.Finnish -> "Pysty"
        AppLanguage.Greek -> "Κάθετο"
        AppLanguage.Romanian -> "Portret"
        AppLanguage.Hungarian -> "Álló"
        AppLanguage.Turkish -> "Dikey"
        AppLanguage.Malay -> "Potret"
        AppLanguage.Hebrew -> "לאורך"
        else -> "Portrait"
    }
    "ratio_4x5" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "图文"
        AppLanguage.Japanese -> "画像付き"
        AppLanguage.Korean -> "이미지형"
        AppLanguage.French -> "Image"
        AppLanguage.German -> "Bild"
        AppLanguage.Italian -> "Immagine"
        AppLanguage.Spanish -> "Imagen"
        AppLanguage.Portuguese -> "Imagem"
        AppLanguage.Russian -> "Изображение"
        AppLanguage.Thai -> "รูปภาพ"
        AppLanguage.Vietnamese -> "Hình ảnh"
        AppLanguage.Indonesian -> "Gambar"
        AppLanguage.Arabic -> "صورة"
        AppLanguage.Ukrainian -> "Зображення"
        AppLanguage.Polish -> "Obraz"
        AppLanguage.Czech -> "Obrázek"
        AppLanguage.Dutch -> "Afbeelding"
        AppLanguage.Swedish -> "Bild"
        AppLanguage.Danish -> "Billede"
        AppLanguage.Norwegian -> "Bilde"
        AppLanguage.Finnish -> "Kuva"
        AppLanguage.Greek -> "Εικόνα"
        AppLanguage.Romanian -> "Imagine"
        AppLanguage.Hungarian -> "Kép"
        AppLanguage.Turkish -> "Görsel"
        AppLanguage.Malay -> "Imej"
        AppLanguage.Hebrew -> "תמונה"
        else -> "Image"
    }
    "ratio_16x9" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "宽屏"
        AppLanguage.Japanese -> "ワイド"
        AppLanguage.Korean -> "와이드"
        AppLanguage.French -> "Large"
        AppLanguage.German -> "Breitbild"
        AppLanguage.Italian -> "Largo"
        AppLanguage.Spanish -> "Ancho"
        AppLanguage.Portuguese -> "Largo"
        AppLanguage.Russian -> "Широкоформатный"
        AppLanguage.Thai -> "กว้าง"
        AppLanguage.Vietnamese -> "Rộng"
        AppLanguage.Indonesian -> "Lebar"
        AppLanguage.Arabic -> "عريض"
        AppLanguage.Ukrainian -> "Широкоекранна"
        AppLanguage.Polish -> "Panoramiczny"
        AppLanguage.Czech -> "Širokoúhlý"
        AppLanguage.Dutch -> "Breedbeeld"
        AppLanguage.Swedish -> "Bredbild"
        AppLanguage.Danish -> "Bredformat"
        AppLanguage.Norwegian -> "Bredformat"
        AppLanguage.Finnish -> "Laajakuva"
        AppLanguage.Greek -> "Ευρεία οθόνη"
        AppLanguage.Romanian -> "Ecran lat"
        AppLanguage.Hungarian -> "Szélesvásznú"
        AppLanguage.Turkish -> "Geniş Ekran"
        AppLanguage.Malay -> "Skrin Lebar"
        AppLanguage.Hebrew -> "מסך רחב"
        else -> "Widescreen"
    }
    "ratio_9x16" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "竖屏"
        AppLanguage.Japanese -> "縦画面"
        AppLanguage.Korean -> "세로 화면"
        AppLanguage.French -> "Vertical Vidéo"
        AppLanguage.German -> "Hochformat Video"
        AppLanguage.Italian -> "Verticale Video"
        AppLanguage.Spanish -> "Vertical Video"
        AppLanguage.Portuguese -> "Vídeo Vertical"
        AppLanguage.Russian -> "Вертикальное видео"
        AppLanguage.Thai -> "แนวตั้งวิดีโอ"
        AppLanguage.Vietnamese -> "Video Dọc"
        AppLanguage.Indonesian -> "Video Vertikal"
        AppLanguage.Arabic -> "فيديو عمودي"
        AppLanguage.Ukrainian -> "Вертикальне відео"
        AppLanguage.Polish -> "Pionowe wideo"
        AppLanguage.Czech -> "Svislé video"
        AppLanguage.Dutch -> "Verticale video"
        AppLanguage.Swedish -> "Stående video"
        AppLanguage.Danish -> "Lodret video"
        AppLanguage.Norwegian -> "Stående video"
        AppLanguage.Finnish -> "Pystyvideo"
        AppLanguage.Greek -> "Κάθετο βίντεο"
        AppLanguage.Romanian -> "Video vertical"
        AppLanguage.Hungarian -> "Álló videó"
        AppLanguage.Turkish -> "Dikey Video"
        AppLanguage.Malay -> "Video Menegak"
        AppLanguage.Hebrew -> "סרטון אנכי"
        else -> "Vertical Video"
    }
    else -> key
}

private fun resolveWatermarkUILabel(language: AppLanguage, label: String): String = when (label) {
    "自由" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "自由"
        AppLanguage.Japanese -> "フリー"
        AppLanguage.Korean -> "자유"
        AppLanguage.French -> "Libre"
        AppLanguage.German -> "Frei"
        AppLanguage.Italian -> "Libero"
        AppLanguage.Spanish -> "Libre"
        AppLanguage.Portuguese -> "Livre"
        AppLanguage.Russian -> "Свободно"
        AppLanguage.Thai -> "อิสระ"
        AppLanguage.Vietnamese -> "Tự do"
        AppLanguage.Indonesian -> "Bebas"
        AppLanguage.Arabic -> "حر"
        AppLanguage.Ukrainian -> "Вільно"
        AppLanguage.Polish -> "Swobodnie"
        AppLanguage.Czech -> "Volně"
        AppLanguage.Dutch -> "Vrij"
        AppLanguage.Swedish -> "Fritt"
        AppLanguage.Danish -> "Frit"
        AppLanguage.Norwegian -> "Fritt"
        AppLanguage.Finnish -> "Vapaa"
        AppLanguage.Greek -> "Ελεύθερο"
        AppLanguage.Romanian -> "Liber"
        AppLanguage.Hungarian -> "Szabad"
        AppLanguage.Turkish -> "Serbest"
        AppLanguage.Malay -> "Bebas"
        AppLanguage.Hebrew -> "חופשי"
        else -> "Free"
    }
    "任意" -> when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "任意"
        AppLanguage.Japanese -> "任意"
        AppLanguage.Korean -> "임의"
        AppLanguage.French -> "Arbitraire"
        AppLanguage.German -> "Beliebig"
        AppLanguage.Italian -> "Arbitrario"
        AppLanguage.Spanish -> "Arbitrario"
        AppLanguage.Portuguese -> "Arbitrário"
        AppLanguage.Russian -> "Произвольно"
        AppLanguage.Thai -> "任意"
        AppLanguage.Vietnamese -> "Tùy ý"
        AppLanguage.Indonesian -> "Sembarang"
        AppLanguage.Arabic -> "تعسفي"
        AppLanguage.Ukrainian -> "Довільно"
        AppLanguage.Polish -> "Dowolnie"
        AppLanguage.Czech -> "Libovolně"
        AppLanguage.Dutch -> "Willekeurig"
        AppLanguage.Swedish -> "Valfritt"
        AppLanguage.Danish -> "Vilkårligt"
        AppLanguage.Norwegian -> "Valgfritt"
        AppLanguage.Finnish -> "Mielivaltainen"
        AppLanguage.Greek -> "Τυχαίο"
        AppLanguage.Romanian -> "Oarecare"
        AppLanguage.Hungarian -> "Tetszőleges"
        AppLanguage.Turkish -> "Rastgele"
        AppLanguage.Malay -> "Sebarang"
        AppLanguage.Hebrew -> "שרירותי"
        else -> "Any"
    }
    "识别有误？" -> when (language) {
        AppLanguage.SimplifiedChinese -> "识别有误？"
        AppLanguage.TraditionalChinese -> "辨識錯誤？"
        AppLanguage.Japanese -> "誤認識？"
        AppLanguage.Korean -> "인식 오류?"
        AppLanguage.French -> "Mauvaise détection ?"
        AppLanguage.German -> "Falsche Erkennung ?"
        AppLanguage.Italian -> "Rilevamento errato ?"
        AppLanguage.Spanish -> "¿Detección incorrecta ?"
        AppLanguage.Portuguese -> "Detecção incorreta ?"
        AppLanguage.Russian -> "Неверное определение ?"
        AppLanguage.Thai -> "ตรวจจับผิด?"
        AppLanguage.Vietnamese -> "Nhận diện sai ?"
        AppLanguage.Indonesian -> "Deteksi salah ?"
        AppLanguage.Arabic -> "كشف خاطئ؟"
        AppLanguage.Ukrainian -> "Неправильне розпізнавання?"
        AppLanguage.Polish -> "Błędne rozpoznanie?"
        AppLanguage.Czech -> "Chybné rozpoznání?"
        AppLanguage.Dutch -> "Verkeerde herkenning?"
        AppLanguage.Swedish -> "Fel igenkänning?"
        AppLanguage.Danish -> "Forkert genkendelse?"
        AppLanguage.Norwegian -> "Feil gjenkjenning?"
        AppLanguage.Finnish -> "Väärä tunnistus?"
        AppLanguage.Greek -> "Λάθος αναγνώριση;"
        AppLanguage.Romanian -> "Detecție greșită?"
        AppLanguage.Hungarian -> "Téves felismerés?"
        AppLanguage.Turkish -> "Yanlış algılama?"
        AppLanguage.Malay -> "Pengesanan salah?"
        AppLanguage.Hebrew -> "זיהוי שגוי?"
        else -> "Wrong detection ?"
    }
    "边框宽度" -> when (language) {
        AppLanguage.SimplifiedChinese -> "边框宽度"
        AppLanguage.TraditionalChinese -> "邊框寬度"
        AppLanguage.Japanese -> "枠線幅"
        AppLanguage.Korean -> "테두리 너비"
        AppLanguage.French -> "Largeur de bordure"
        AppLanguage.German -> "Rahmenbreite"
        AppLanguage.Italian -> "Larghezza bordo"
        AppLanguage.Spanish -> "Ancho del borde"
        AppLanguage.Portuguese -> "Largura da borda"
        AppLanguage.Russian -> "Ширина рамки"
        AppLanguage.Thai -> "ความกว้างขอบ"
        AppLanguage.Vietnamese -> "Độ rộng viền"
        AppLanguage.Indonesian -> "Lebar batas"
        AppLanguage.Arabic -> "عرض الحدود"
        AppLanguage.Ukrainian -> "Товщина рамки"
        AppLanguage.Polish -> "Szerokość obramowania"
        AppLanguage.Czech -> "Šířka okraje"
        AppLanguage.Dutch -> "Randbreedte"
        AppLanguage.Swedish -> "Kantbredd"
        AppLanguage.Danish -> "Kantbredde"
        AppLanguage.Norwegian -> "Kantbredde"
        AppLanguage.Finnish -> "Reunuksen leveys"
        AppLanguage.Greek -> "Πλάτος περιγράμματος"
        AppLanguage.Romanian -> "Lățimea chenarului"
        AppLanguage.Hungarian -> "Szegély szélessége"
        AppLanguage.Turkish -> "Kenarlık Genişliği"
        AppLanguage.Malay -> "Lebar Sempadan"
        AppLanguage.Hebrew -> "רוחב שוליים"
        else -> "Border Width"
    }
    "在面板内调整，预览会保持固定可见" -> when (language) {
        AppLanguage.SimplifiedChinese -> "在面板内调整，预览会保持固定可见"
        AppLanguage.TraditionalChinese -> "在面板內調整，預覽會保持固定可見"
        AppLanguage.Japanese -> "パネル内で調整、プレビューは固定表示を維持"
        AppLanguage.Korean -> "패널 내에서 조정, 미리보기는 고정 표시 유지"
        AppLanguage.French -> "Ajuster dans le panneau, l'aperçu reste visible"
        AppLanguage.German -> "Im Panel anpassen, Vorschau bleibt sichtbar"
        AppLanguage.Italian -> "Regola nel pannello, l'anteprima rimane visibile"
        AppLanguage.Spanish -> "Ajustar en el panel, la vista previa permanece visible"
        AppLanguage.Portuguese -> "Ajustar no painel, a visualização permanece visível"
        AppLanguage.Russian -> "Настройте в панели, предпросмотр остается видимым"
        AppLanguage.Thai -> "ปรับในแผงค ตัวอย่างยังคงมองเห็น"
        AppLanguage.Vietnamese -> "Chỉnh trong bảng, xem trước vẫn hiển thị"
        AppLanguage.Indonesian -> "Sesuaikan di panel, pratinjau tetap terlihat"
        AppLanguage.Arabic -> "اضبط في اللوحة، يبقى المعاينة مرئياً"
        AppLanguage.Ukrainian -> "Налаштовуйте на панелі, попередній перегляд лишається видимим"
        AppLanguage.Polish -> "Dostosuj w panelu, podgląd pozostaje widoczny"
        AppLanguage.Czech -> "Upravte v panelu, náhled zůstane viditelný"
        AppLanguage.Dutch -> "Pas aan in het paneel, de voorvertoning blijft zichtbaar"
        AppLanguage.Swedish -> "Justera i panelen, förhandsvisningen förblir synlig"
        AppLanguage.Danish -> "Juster i panelet, forhåndsvisningen forbliver synlig"
        AppLanguage.Norwegian -> "Juster i panelet, forhåndsvisningen forblir synlig"
        AppLanguage.Finnish -> "Säädä paneelissa, esikatselu pysyy näkyvissä"
        AppLanguage.Greek -> "Προσαρμόστε στον πίνακα, η προεπισκόπηση παραμένει ορατή"
        AppLanguage.Romanian -> "Ajustează în panou, previzualizarea rămâne vizibilă"
        AppLanguage.Hungarian -> "Állítsd be a panelben, az előnézet látható marad"
        AppLanguage.Turkish -> "Panelde ayarlayın, önizleme görünür kalır"
        AppLanguage.Malay -> "Laraskan dalam panel, pratonton kekal kelihatan"
        AppLanguage.Hebrew -> "התאימו בלוח, התצוגה המקדימה נשארת גלויה"
        else -> "Adjust in panel, preview stays visible"
    }
    "背景色" -> when (language) {
        AppLanguage.SimplifiedChinese -> "背景色"
        AppLanguage.TraditionalChinese -> "背景色"
        AppLanguage.Japanese -> "背景色"
        AppLanguage.Korean -> "배경색"
        AppLanguage.French -> "Couleur d'arrière-plan"
        AppLanguage.German -> "Hintergrundfarbe"
        AppLanguage.Italian -> "Colore di sfondo"
        AppLanguage.Spanish -> "Color de fondo"
        AppLanguage.Portuguese -> "Cor de fundo"
        AppLanguage.Russian -> "Цвет фона"
        AppLanguage.Thai -> "สีพื้นหลัง"
        AppLanguage.Vietnamese -> "Màu nền"
        AppLanguage.Indonesian -> "Warna latar belakang"
        AppLanguage.Arabic -> "لون الخلفية"
        AppLanguage.Ukrainian -> "Колір фону"
        AppLanguage.Polish -> "Kolor tła"
        AppLanguage.Czech -> "Barva pozadí"
        AppLanguage.Dutch -> "Achtergrondkleur"
        AppLanguage.Swedish -> "Bakgrundsfärg"
        AppLanguage.Danish -> "Baggrundsfarve"
        AppLanguage.Norwegian -> "Bakgrunnsfarge"
        AppLanguage.Finnish -> "Taustaväri"
        AppLanguage.Greek -> "Χρώμα φόντου"
        AppLanguage.Romanian -> "Culoare fundal"
        AppLanguage.Hungarian -> "Háttérszín"
        AppLanguage.Turkish -> "Arka Plan Rengi"
        AppLanguage.Malay -> "Warna Latar"
        AppLanguage.Hebrew -> "צבע רקע"
        else -> "Background Color"
    }
    "悬浮样式" -> when (language) {
        AppLanguage.SimplifiedChinese -> "悬浮样式"
        AppLanguage.TraditionalChinese -> "懸浮樣式"
        AppLanguage.Japanese -> "フローティングスタイル"
        AppLanguage.Korean -> "플로팅 스타일"
        AppLanguage.French -> "Style flottant"
        AppLanguage.German -> "Schwebender Stil"
        AppLanguage.Italian -> "Stile galleggiante"
        AppLanguage.Spanish -> "Estilo flotante"
        AppLanguage.Portuguese -> "Estilo flutuante"
        AppLanguage.Russian -> "Стиль плавания"
        AppLanguage.Thai -> "สไตล์ลอยตัว"
        AppLanguage.Vietnamese -> "Kiểu nổi"
        AppLanguage.Indonesian -> "Gaya mengambang"
        AppLanguage.Arabic -> "نمط العائم"
        AppLanguage.Ukrainian -> "Плаваючий стиль"
        AppLanguage.Polish -> "Styl unoszący się"
        AppLanguage.Czech -> "Plovoucí styl"
        AppLanguage.Dutch -> "Zwevende stijl"
        AppLanguage.Swedish -> "Svävande stil"
        AppLanguage.Danish -> "Svævende stil"
        AppLanguage.Norwegian -> "Svevende stil"
        AppLanguage.Finnish -> "Kelluva tyyli"
        AppLanguage.Greek -> "Αιωρούμενο στυλ"
        AppLanguage.Romanian -> "Stil plutitor"
        AppLanguage.Hungarian -> "Lebegő stílus"
        AppLanguage.Turkish -> "Yüzen Stil"
        AppLanguage.Malay -> "Gaya Terapung"
        AppLanguage.Hebrew -> "סגנון מרחף"
        else -> "Floating Style"
    }
    else -> label
}

data class SocialCropPreset(
    val key: String,
    val platformLabel: String,
    val ratioLabel: String,
    val width: Int,
    val height: Int,
    val isVideo: Boolean = false
) {
    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()
    val label: String
        get() = "$platformLabel $ratioLabel"
}

private data class WatermarkCropEditorHistoryEntry(
    val presetKey: String?,
    val aspectRatio: Float,
    val centerX: Float,
    val centerY: Float,
    val zoom: Float
)

internal data class WatermarkExifInfo(
    val make: String = "",
    val model: String = "",
    val iso: String = "--",
    val shutter: String = "--",
    val focalLength: String = "--",
    val aperture: String = "--",
    val dateTimeOriginal: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cityLocation: String = ""
) {
    val cameraLine: String
        get() {
            val text = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ")
            return text.ifBlank { "来自 Photoo" }
        }

    val detailLine: String
        get() {
            val parts = mutableListOf<String>()
            if (iso != "--") parts += "ISO $iso"
            if (shutter != "--") parts += formatShutterSpeed(shutter)
            if (aperture != "--") parts += "f/${formatAperture(aperture)}"
            if (focalLength != "--") parts += "${formatFocalLength(focalLength)}mm"
            val dateText = normalizeExifDate(dateTimeOriginal)
            if (dateText.isNotBlank()) parts += dateText
            return parts.joinToString("  |  ").ifBlank { "自动 EXIF 水印" }
        }
}

private data class FullColorUiCopy(
    val locationLabel: String,
    val noteLabel: String,
    val swapLayoutLabel: String,
    val gradientColorLabel: String,
    val timeHint: String,
    val locationHint: String,
    val noteHint: String,
    val dateDialogTitle: String,
    val locationDialogTitle: String,
    val noteDialogTitle: String,
    val fallbackHint: String
)

private fun resolveFullColorUiCopy(language: AppLanguage): FullColorUiCopy = when (language) {
    AppLanguage.SimplifiedChinese -> FullColorUiCopy(
        locationLabel = "地点",
        noteLabel = "随心记",
        swapLayoutLabel = "上下互换",
        gradientColorLabel = "渐变色彩",
        timeHint = "优先读取 EXIF，可手动改写",
        locationHint = "优先读取地理位置，可手动改写",
        noteHint = "写任何想留下的话，支持换行",
        dateDialogTitle = "编辑时间",
        locationDialogTitle = "编辑地点",
        noteDialogTitle = "编辑随心记",
        fallbackHint = "留空则回退到 EXIF"
    )
    AppLanguage.TraditionalChinese -> FullColorUiCopy(
        locationLabel = "地點",
        noteLabel = "隨心記",
        swapLayoutLabel = "上下互換",
        gradientColorLabel = "漸變色彩",
        timeHint = "優先讀取 EXIF，可手動改寫",
        locationHint = "優先讀取地理位置，可手動改寫",
        noteHint = "寫任何想留下的話，支援換行",
        dateDialogTitle = "編輯時間",
        locationDialogTitle = "編輯地點",
        noteDialogTitle = "編輯隨心記",
        fallbackHint = "留空則回退到 EXIF"
    )
    AppLanguage.Japanese -> FullColorUiCopy(
        locationLabel = "場所",
        noteLabel = "メモ",
        swapLayoutLabel = "上下を入れ替え",
        gradientColorLabel = "グラデーション",
        timeHint = "まず EXIF を読み込み、必要なら手動で上書き",
        locationHint = "まず位置情報を読み込み、必要なら手動で上書き",
        noteHint = "自由に書けます。改行も使えます",
        dateDialogTitle = "時間を編集",
        locationDialogTitle = "場所を編集",
        noteDialogTitle = "メモを編集",
        fallbackHint = "空欄なら EXIF を使用"
    )
    AppLanguage.Korean -> FullColorUiCopy(
        locationLabel = "위치",
        noteLabel = "메모",
        swapLayoutLabel = "위아래 바꾸기",
        gradientColorLabel = "그라데이션",
        timeHint = "우선 EXIF 를 읽고, 필요하면 수동으로 수정",
        locationHint = "우선 위치 정보를 읽고, 필요하면 수동으로 수정",
        noteHint = "원하는 내용을 자유롭게 쓰고 줄바꿈할 수 있어요",
        dateDialogTitle = "시간 편집",
        locationDialogTitle = "위치 편집",
        noteDialogTitle = "메모 편집",
        fallbackHint = "비워 두면 EXIF 를 사용"
    )
    else -> FullColorUiCopy(
        locationLabel = "Location",
        noteLabel = "Free Note",
        swapLayoutLabel = "Swap Top and Bottom",
        gradientColorLabel = "Gradient",
        timeHint = "Read from EXIF first, or override it manually",
        locationHint = "Read from location metadata first, or override it manually",
        noteHint = "Write anything you want. Line breaks are supported",
        dateDialogTitle = "Edit Time",
        locationDialogTitle = "Edit Location",
        noteDialogTitle = "Edit Free Note",
        fallbackHint = "Leave blank to fall back to EXIF"
    )
}

private fun resolveFullColorTextScaleLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "文字大小"
    AppLanguage.TraditionalChinese -> "文字大小"
    AppLanguage.Japanese -> "文字サイズ"
    AppLanguage.Korean -> "텍스트 크기"
    else -> "Text Size"
}

private fun resolveFullColorBackgroundColorLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u8272\u5f69\u989c\u8272"
    AppLanguage.TraditionalChinese -> "\u8272\u5f69\u984f\u8272"
    else -> "Color"
}

private data class FullColorGradientPreset(
    val key: String,
    val startColor: Int,
    val endColor: Int
)

private val FULL_COLOR_GRADIENT_PRESETS = listOf(
    FullColorGradientPreset("sunset_pop", AndroidColor.parseColor("#FF7E5F"), AndroidColor.parseColor("#FEB47B")),
    FullColorGradientPreset("berry_night", AndroidColor.parseColor("#C33764"), AndroidColor.parseColor("#1D2671")),
    FullColorGradientPreset("ocean_breeze", AndroidColor.parseColor("#43C6AC"), AndroidColor.parseColor("#191654")),
    FullColorGradientPreset("aurora", AndroidColor.parseColor("#00C9A7"), AndroidColor.parseColor("#845EC2")),
    FullColorGradientPreset("gold_pink", AndroidColor.parseColor("#F7971E"), AndroidColor.parseColor("#FFD200")),
    FullColorGradientPreset("neon_sky", AndroidColor.parseColor("#12C2E9"), AndroidColor.parseColor("#F64F59")),
    FullColorGradientPreset("lavender_blue", AndroidColor.parseColor("#654EA3"), AndroidColor.parseColor("#EAAFC8")),
    FullColorGradientPreset("lime_cyan", AndroidColor.parseColor("#A8FF78"), AndroidColor.parseColor("#00C6FF"))
)

internal fun resolveFullColorGradientColorsOverride(index: Int): Pair<Int, Int>? {
    val preset = FULL_COLOR_GRADIENT_PRESETS.getOrNull(index) ?: return null
    return preset.startColor to preset.endColor
}

private fun resolveFullColorAutoColorLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u81ea\u52a8"
    AppLanguage.TraditionalChinese -> "\u81ea\u52d5"
    else -> "Auto"
}

private fun resolveFullColorCustomColorLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u81ea\u5b9a\u4e49"
    AppLanguage.TraditionalChinese -> "\u81ea\u5b9a\u7fa9"
    else -> "Custom"
}

private fun resolveFullColorCustomColorDialogTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u81ea\u5b9a\u4e49\u8272\u5f69\u989c\u8272"
    AppLanguage.TraditionalChinese -> "\u81ea\u5b9a\u7fa9\u8272\u5f69\u984f\u8272"
    else -> "Custom Color"
}

private fun resolveFullColorInvalidHexLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u8bf7\u8f93\u5165 #RRGGBB"
    AppLanguage.TraditionalChinese -> "\u8acb\u8f38\u5165 #RRGGBB"
    else -> "Use #RRGGBB"
}

private fun formatFullColorHexColor(color: Int): String = "#%02X%02X%02X".format(
    AndroidColor.red(color),
    AndroidColor.green(color),
    AndroidColor.blue(color)
)

private fun sanitizeFullColorHexInput(raw: String): String {
    val keepHash = raw.trim().startsWith("#")
    val token = raw
        .filter { it == '#' || it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        .replace("#", "")
        .take(6)
        .uppercase(Locale.US)
    return if (keepHash || raw.contains('#')) "#$token" else token
}

private fun parseFullColorHexColor(raw: String): Int? {
    val token = raw.trim().removePrefix("#")
    if (token.length != 6 || token.any { it !in '0'..'9' && it !in 'A'..'F' && it !in 'a'..'f' }) {
        return null
    }
    return runCatching { AndroidColor.parseColor("#${token.uppercase(Locale.US)}") }.getOrNull()
}

private fun normalizeFullColorOpaqueColor(color: Int): Int = AndroidColor.rgb(
    AndroidColor.red(color),
    AndroidColor.green(color),
    AndroidColor.blue(color)
)

@Composable
private fun FullColorGradientChoice(
    startColor: Color,
    endColor: Color,
    selected: Boolean,
    outlineColor: Color,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 38.dp)
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    0f to startColor,
                    0.5f to startColor,
                    0.5f to endColor,
                    1f to endColor
                )
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) selectedColor else outlineColor,
                shape = shape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FullColorBackgroundColorPicker(
    language: AppLanguage,
    useGradientBackground: Boolean,
    selectedColorIndex: Int,
    selectedGradientPresetIndex: Int,
    customColor: Int,
    containerColor: Color,
    selectedContainerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onSelectAuto: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onSelectGradientPreset: (Int) -> Unit,
    onOpenCustomColor: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FullColorTextChoice(
            text = resolveFullColorAutoColorLabel(language),
            selected = if (useGradientBackground) {
                selectedGradientPresetIndex < 0
            } else {
                selectedColorIndex == FULL_COLOR_AUTO_BACKGROUND_INDEX
            },
            containerColor = containerColor,
            selectedContainerColor = selectedContainerColor,
            outlineColor = outlineColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            onClick = onSelectAuto
        )
        if (useGradientBackground) {
            FULL_COLOR_GRADIENT_PRESETS.forEachIndexed { index, preset ->
                FullColorGradientChoice(
                    startColor = Color(preset.startColor),
                    endColor = Color(preset.endColor),
                    selected = selectedGradientPresetIndex == index,
                    outlineColor = outlineColor,
                    selectedColor = primaryTextColor,
                    onClick = { onSelectGradientPreset(index) }
                )
            }
        } else {
            FULL_COLOR_BACKGROUND_OPTIONS.forEachIndexed { index, option ->
                FullColorSwatchChoice(
                    color = option.compose,
                    selected = selectedColorIndex == index,
                    outlineColor = outlineColor,
                    selectedColor = primaryTextColor,
                    onClick = { onSelectPreset(index) }
                )
            }
            FullColorCustomChoice(
                text = resolveFullColorCustomColorLabel(language),
                color = Color(customColor),
                selected = selectedColorIndex == FULL_COLOR_CUSTOM_BACKGROUND_INDEX,
                containerColor = containerColor,
                selectedContainerColor = selectedContainerColor,
                outlineColor = outlineColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                onClick = onOpenCustomColor
            )
        }
    }
}

@Composable
private fun FullColorTextChoice(
    text: String,
    selected: Boolean,
    containerColor: Color,
    selectedContainerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedContainerColor else containerColor,
        border = BorderStroke(1.dp, if (selected) primaryTextColor.copy(alpha = 0.34f) else outlineColor),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) primaryTextColor else secondaryTextColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FullColorSwatchChoice(
    color: Color,
    selected: Boolean,
    outlineColor: Color,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(shape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) selectedColor else outlineColor,
                shape = shape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FullColorCustomChoice(
    text: String,
    color: Color,
    selected: Boolean,
    containerColor: Color,
    selectedContainerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedContainerColor else containerColor,
        border = BorderStroke(1.dp, if (selected) primaryTextColor.copy(alpha = 0.34f) else outlineColor),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .border(1.dp, outlineColor, RoundedCornerShape(6.dp))
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) primaryTextColor else secondaryTextColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FullColorTimeWheelPicker(
    hour: String,
    minute: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val hourOptions = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val minuteOptions = remember { (0..59).map { it.toString().padStart(2, '0') } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FullColorTimeWheelColumn(
            options = hourOptions,
            selectedValue = hour.ifBlank { "12" },
            onValueChange = onHourChange,
            label = "HH",
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            modifier = Modifier.weight(1f)
        )
        FullColorTimeWheelColumn(
            options = minuteOptions,
            selectedValue = minute.ifBlank { "00" },
            onValueChange = onMinuteChange,
            label = "MM",
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FullColorTimeWheelColumn(
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    label: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestSelectedValue by rememberUpdatedState(selectedValue)
    val itemHeight = 42.dp
    val wheelViewportHeight = itemHeight * 3
    val columnShape = RoundedCornerShape(20.dp)
    val selectionShape = RoundedCornerShape(14.dp)
    val selectedIndex = options.indexOf(selectedValue).coerceAtLeast(0)
    val listState = remember(options) {
        LazyListState(firstVisibleItemIndex = selectedIndex)
    }
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centeredIndex by remember(listState, options, selectedIndex) {
        derivedStateOf {
            calculateFullColorTimeWheelCenteredIndex(
                state = listState,
                fallback = selectedIndex
            ).coerceIn(0, options.lastIndex.coerceAtLeast(0))
        }
    }
    var userInteracted by remember(options) { mutableStateOf(false) }
    LaunchedEffect(selectedIndex, options.size, userInteracted) {
        if (options.isEmpty() || listState.isScrollInProgress || userInteracted) return@LaunchedEffect
        if (
            listState.firstVisibleItemIndex != selectedIndex ||
            listState.firstVisibleItemScrollOffset != 0
        ) {
            listState.scrollToItem(selectedIndex)
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userInteracted = true
        }
    }
    LaunchedEffect(centeredIndex, options.size, userInteracted) {
        if (options.isEmpty() || !userInteracted) return@LaunchedEffect
        val centeredValue = options[centeredIndex]
        if (centeredValue != latestSelectedValue) {
            latestOnValueChange(centeredValue)
            if (hapticsAllowed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress, centeredIndex, options.size, userInteracted) {
        if (options.isEmpty() || !userInteracted || listState.isScrollInProgress) return@LaunchedEffect
        if (
            listState.firstVisibleItemIndex != centeredIndex ||
            listState.firstVisibleItemScrollOffset != 0
        ) {
            listState.scrollToItem(centeredIndex)
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = secondaryTextColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(wheelViewportHeight)
                .clip(columnShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = columnShape
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(itemHeight)
                    .clip(selectionShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = selectionShape
                    )
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight)
            ) {
                items(options.size) { index ->
                    val option = options[index]
                    val selected = option == selectedValue
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                userInteracted = true
                                scope.launch {
                                    listState.animateScrollToItem(index)
                                }
                                onValueChange(option)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            color = primaryTextColor.copy(alpha = if (selected) 1f else 0.48f),
                            fontSize = if (selected) 18.sp else 15.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun calculateFullColorTimeWheelCenteredIndex(
    state: LazyListState,
    fallback: Int
): Int {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return fallback
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return visibleItems.minByOrNull { item ->
        abs((item.offset + (item.size / 2)) - viewportCenter)
    }?.index ?: fallback
}

private data class WatermarkOutputFormat(
    val mimeType: String,
    val fileExtension: String,
    val compressFormat: Bitmap.CompressFormat,
    val quality: Int
)

private data class WatermarkRenderOutput(
    val outputFile: File,
    val mimeType: String
)

private data class BrandLogoVariant(
    val label: String,
    val resId: Int
)

private data class BrandLogoGroup(
    val key: String,
    val label: String,
    val aliases: List<String>,
    val variants: List<BrandLogoVariant>
)

private data class WatermarkUserPreset(
    val id: String,
    val name: String,
    val templateName: String,
    val selectedBrandKey: String?,
    val selectedVariantIndex: Int,
    val showLogoWatermark: Boolean,
    val showModelWatermark: Boolean,
    val showDateWatermark: Boolean,
    val showExifWatermark: Boolean,
    val showLocationWatermark: Boolean,
    val showCityLocationWatermark: Boolean,
    val customModelLabel: String,
    val customDateLabel: String,
    val customLocationLabel: String,
    val fullColorTextScaleFactor: Float,
    val fullColorBackgroundColorIndex: Int,
    val fullColorCustomBackgroundColor: Int,
    val fullColorUseGradientBackground: Boolean,
    val fullColorGradientPresetIndex: Int,
    val fullColorImageOnTop: Boolean,
    val frameSideTopBorderRatio: Float,
    val frameLayoutMode: String,
    val logoScaleFactor: Float,
    val frameBackgroundColorIndex: Int,
    val thinBorderColorIndex: Int,
    val roundedBorderColorIndex: Int,
    val borderCornerMode: String,
    val blurImageCornerRadiusRatio: Float,
    val borderImageScaleFactor: Float,
    val floatingBackgroundColorIndex: Int,
    val floatingStrokeColorIndex: Int,
    val floatingImageCornerRadiusRatio: Float,
    val filmFrameIndex: Int,
    val polaroidFrameIndex: Int,
    val colorPaletteStyleIndex: Int,
    val colorPaletteVariantIndex: Int,
    val colorPaletteHorizontalOffsetRatio: Float,
    val colorPaletteVerticalOffsetRatio: Float,
    val colorPaletteElementCount: Int,
    val colorPaletteShowHexValue: Boolean,
    val colorPaletteShowRgbValue: Boolean,
    val paperDotsLayoutDirection: String,
    val paperDotsSplitRatio: Float,
    val paperDotsBackgroundMode: String,
    val paperDotsBackgroundColorIndex: Int,
    val paperDotsBackgroundSecondaryColorIndex: Int,
    val paperDotsStripeSizeRatio: Float,
    val paperDotsShape: String,
    val paperDotsDotSize: Float,
    val paperDotsDotVariance: Float,
    val paperDotsDotCount: Int,
    val paperDotsRandomSeed: Int,
    val paperDotsStrokeMode: String,
    val paperDotsStrokeWidthRatio: Float,
    val paperDotsStrokeMixRatio: Float,
    val paperDotsPatternText: String,
    val signatureEnabled: Boolean,
    val signatureText: String,
    val signatureColorIndex: Int,
    val signatureHorizontalOffsetRatio: Float,
    val signatureVerticalOffsetRatio: Float,
    val signatureScaleFactor: Float,
    val watermarkFontKey: String,
    val signatureFontKey: String,
    val cutoutOverlayStyle: String,
    val cutoutSubjectScaleFactor: Float,
    val cutoutFrameAspectRatio: String,
    val subjectOutlineEnabled: Boolean,
    val subjectOutlineLineStyle: String,
    val subjectOutlineWidthRatio: Float,
    val subjectOutlineColorIndex: Int,
    val subjectOutlineOuterBorderEnabled: Boolean,
    val subjectOutlineOffsetRatio: Float,
    val subjectOutlineSymbolText: String,
    val subjectOutlineSymbolSizeRatio: Float
) {
    val template: WatermarkTemplatePreset?
        get() = runCatching { WatermarkTemplatePreset.valueOf(templateName) }.getOrNull()
}

private const val WATERMARK_USER_PRESET_PREF_KEY = "watermark_user_presets_v1"
private const val WATERMARK_USER_PRESET_LIMIT = 30
private const val WATERMARK_PREF_TEXT_FONT_KEY = "watermark_text_font_key"
private const val WATERMARK_PREF_SIGNATURE_FONT_KEY = "watermark_signature_font_key"
private const val WATERMARK_PREF_CUSTOM_LOGO_PATH = "watermark_custom_logo_path"
private const val WATERMARK_PREF_SIGNATURE_OFFSET_X = "watermark_signature_offset_x"
private const val WATERMARK_PREF_SIGNATURE_OFFSET_Y = "watermark_signature_offset_y"
private const val WATERMARK_PREF_SIGNATURE_SCALE = "watermark_signature_scale"
private const val DEFAULT_WATERMARK_TEXT_FONT_KEY = "inter_variable"
private const val DEFAULT_SIGNATURE_FONT_KEY = "harshita"
private const val WATERMARK_TEMP_SUBDIR = "watermark_temp"
private const val WATERMARK_CUSTOM_LOGO_SUBDIR = "watermark_custom_logo"
private const val WATERMARK_CUSTOM_LOGO_FILE = "logo.png"
private const val CUSTOM_LOGO_BRAND_KEY = "__custom_logo__"
private const val WATERMARK_TEMP_MAX_BYTES = 512L * 1024L * 1024L
private const val WATERMARK_TEMP_KEEP_NEWEST = 80
private const val WATERMARK_TEMP_MAX_AGE_MS = 2L * 24L * 60L * 60L * 1000L
private const val WATERMARK_LIVE_PREVIEW_MAX_SIDE = 1800
private const val WATERMARK_PAPER_DOTS_LIVE_PREVIEW_MAX_SIDE = 1400
private const val WATERMARK_TEMPLATE_PREVIEW_MAX_SIDE = 1200
private const val WATERMARK_SIGNATURE_MIN_RENDER_SOURCE_SIDE = 1800
private const val WATERMARK_TEMPLATE_PREVIEW_WIDTH = 480
private const val WATERMARK_TEMPLATE_PREVIEW_HEIGHT = 640
private const val WATERMARK_PAPER_DOTS_TEMPLATE_PREVIEW_WIDTH = 720
private const val WATERMARK_PAPER_DOTS_TEMPLATE_PREVIEW_HEIGHT = 480
private const val DEFAULT_BLUR_IMAGE_CORNER_RATIO = 0.055f
private const val DEFAULT_FLOATING_IMAGE_CORNER_RATIO = 0.010f
private const val DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR = 0.88f
private const val FULL_COLOR_TEXT_SCALE_MIN = 0.7f
private const val FULL_COLOR_TEXT_SCALE_MAX = 1.2f
private const val FULL_COLOR_AUTO_BACKGROUND_INDEX = -1
private const val FULL_COLOR_CUSTOM_BACKGROUND_INDEX = -2
private const val DEFAULT_FULL_COLOR_CUSTOM_BACKGROUND_COLOR = 0xFF365A7A.toInt()

private val cachedTypefaceByAssetPath = ConcurrentHashMap<String, Typeface>()

private val BRAND_LOGO_GROUPS = listOf(
    BrandLogoGroup("photoo", "Photoo", listOf("photoo"), listOf(
        BrandLogoVariant("默认", R.drawable.logokt)
    )),
    BrandLogoGroup("apple", "Apple 苹果", listOf("apple", "iphone", "ipad"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_apple_black),
        BrandLogoVariant("White", R.drawable.brand_logo_apple_white)
    )),
    BrandLogoGroup("canon", "Canon 佳能", listOf("canon"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_canon_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_canon_color),
        BrandLogoVariant("White", R.drawable.brand_logo_canon_white)
    )),
    BrandLogoGroup("dji", "DJI 大疆", listOf("dji"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_dji_black),
        BrandLogoVariant("White", R.drawable.brand_logo_dji_white)
    )),
    BrandLogoGroup("fujifilm", "Fujifilm 富士", listOf("fujifilm", "fuji"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_fujifilm_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_fujifilm_color),
        BrandLogoVariant("White", R.drawable.brand_logo_fujifilm_white)
    )),
    BrandLogoGroup("gopro", "GoPro", listOf("gopro"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_gopro_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_gopro_color),
        BrandLogoVariant("White", R.drawable.brand_logo_gopro_white)
    )),
    BrandLogoGroup("hasselblad", "HASSELBLAD 哈苏", listOf("hasselblad"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_hasselblad_black),
        BrandLogoVariant("White", R.drawable.brand_logo_hasselblad_white)
    )),
    BrandLogoGroup("honor", "HONOR 荣耀", listOf("honor"), listOf(
        BrandLogoVariant("Color", R.drawable.brand_logo_honor_color),
        BrandLogoVariant("White", R.drawable.brand_logo_honor_white)
    )),
    BrandLogoGroup("huawei", "HUAWEI 华为", listOf("huawei"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_huawei_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_huawei_color),
        BrandLogoVariant("White", R.drawable.brand_logo_huawei_white)
    )),
    BrandLogoGroup("insta360", "影石 Insta360", listOf("insta360"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_insta360_black),
        BrandLogoVariant("White", R.drawable.brand_logo_insta360_white)
    )),
    BrandLogoGroup("iqoo", "iQOO", listOf("iqoo"), listOf(
        BrandLogoVariant("Color 1", R.drawable.brand_logo_iqoo_color),
        BrandLogoVariant("Color 2", R.drawable.brand_logo_iqoo_color_alt),
        BrandLogoVariant("White", R.drawable.brand_logo_iqoo_white)
    )),
    BrandLogoGroup("leica", "Leica 徕卡", listOf("leica"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_leica_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_leica_color),
        BrandLogoVariant("White", R.drawable.brand_logo_leica_white)
    )),
    BrandLogoGroup("lumix", "LUMIX 松下", listOf("lumix", "panasonic"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_lumix_black),
        BrandLogoVariant("White", R.drawable.brand_logo_lumix_white)
    )),
    BrandLogoGroup("meizu", "meizu 魅族", listOf("meizu"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_meizu_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_meizu_color),
        BrandLogoVariant("White", R.drawable.brand_logo_meizu_white)
    )),
    BrandLogoGroup("nikon", "Nikon 尼康", listOf("nikon"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_nikon_black),
        BrandLogoVariant("Color 1", R.drawable.brand_logo_nikon_color),
        BrandLogoVariant("Color 2", R.drawable.brand_logo_nikon_color_alt),
        BrandLogoVariant("White", R.drawable.brand_logo_nikon_white)
    )),
    BrandLogoGroup("oneplus", "ONEPLUS 一加", listOf("oneplus", "one plus"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_oneplus_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_oneplus_color),
        BrandLogoVariant("White", R.drawable.brand_logo_oneplus_white)
    )),
    BrandLogoGroup("oppo", "OPPO", listOf("oppo"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_oppo_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_oppo_color),
        BrandLogoVariant("White", R.drawable.brand_logo_oppo_white)
    )),
    BrandLogoGroup("osmo_action", "OSMO ACTION", listOf("osmo", "action"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_osmo_action_black),
        BrandLogoVariant("White", R.drawable.brand_logo_osmo_action_white)
    )),
    BrandLogoGroup("phaseone", "PHASEONE 飞思", listOf("phaseone", "phase one"), listOf(
        BrandLogoVariant("Color 1", R.drawable.brand_logo_phaseone_color),
        BrandLogoVariant("Color 2", R.drawable.brand_logo_phaseone_color_alt),
        BrandLogoVariant("White", R.drawable.brand_logo_phaseone_white)
    )),
    BrandLogoGroup("realme", "realme 真我", listOf("realme"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_realme_black),
        BrandLogoVariant("White", R.drawable.brand_logo_realme_white)
    )),
    BrandLogoGroup("samsung", "Samsung 三星", listOf("samsung", "galaxy"), listOf(
        BrandLogoVariant("Black 1", R.drawable.brand_logo_samsung_black),
        BrandLogoVariant("Black 2", R.drawable.brand_logo_samsung_black_2),
        BrandLogoVariant("White", R.drawable.brand_logo_samsung_white)
    )),
    BrandLogoGroup("sony_alpha", "α 索尼阿尔法", listOf("sonyalpha", "sony alpha", " α"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_sony_alpha_black),
        BrandLogoVariant("White", R.drawable.brand_logo_sony_alpha_white)
    )),
    BrandLogoGroup("sony", "SONY 索尼", listOf("sony"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_sony_black),
        BrandLogoVariant("White", R.drawable.brand_logo_sony_white)
    )),
    BrandLogoGroup("vivo", "vivo", listOf("vivo"), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_vivo_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_vivo_color),
        BrandLogoVariant("White", R.drawable.brand_logo_vivo_white)
    )),
    BrandLogoGroup("xiaomi", "Xiaomi 小米", listOf("xiaomi", "redmi", " mi "), listOf(
        BrandLogoVariant("Black", R.drawable.brand_logo_xiaomi_black),
        BrandLogoVariant("Color", R.drawable.brand_logo_xiaomi_color),
        BrandLogoVariant("White", R.drawable.brand_logo_xiaomi_white)
    ))
)

private val BRAND_LOGO_GROUP_MAP by lazy { BRAND_LOGO_GROUPS.associateBy { it.key } }

private val FILM_FRAME_RES_IDS = listOf(
    R.drawable.film_frame_010_10_1,
    R.drawable.film_frame_010_10_4,
    R.drawable.film_frame_010_10_6,
    R.drawable.film_frame_010_10_9,
    R.drawable.film_frame_010_10_10
)
private val FILM_FRAME_LABELS = listOf("白色", "黑色", "竖黑", "大黑", "剥落")
private val POLAROID_FRAME_RES_IDS = listOf(
    R.drawable.polaroid_frame_3,
    R.drawable.polaroid_frame_5,
    R.drawable.polaroid_frame_6,
    R.drawable.polaroid_frame_7,
    R.drawable.polaroid_frame_8
)
private val POLAROID_FRAME_LABELS = listOf("经典宽底", "折角竖版", "横版银灰", "复古米白", "颗粒白边")
private data class PolaroidFrameSpec(
    val leftRatio: Float,
    val topRatio: Float,
    val rightRatio: Float,
    val bottomRatio: Float,
    val darkCenterOverlay: Boolean
)
private val POLAROID_FRAME_SPEC_MAP = mapOf(
    R.drawable.polaroid_frame_3 to PolaroidFrameSpec(0.052f, 0.062f, 0.944f, 0.783f, true),
    R.drawable.polaroid_frame_5 to PolaroidFrameSpec(0.067f, 0.055f, 0.938f, 0.955f, false),
    R.drawable.polaroid_frame_6 to PolaroidFrameSpec(0.067f, 0.099f, 0.936f, 0.891f, true),
    R.drawable.polaroid_frame_7 to PolaroidFrameSpec(0.062f, 0.071f, 0.939f, 0.949f, false),
    R.drawable.polaroid_frame_8 to PolaroidFrameSpec(0.055f, 0.060f, 0.944f, 0.922f, false)
)
private val LOGO_SIZE_EXCLUDED_BRANDS = setOf("canon", "insta360", "meizu", "osmo_action", "hasselblad")
private val FRAME_CENTERED_LOGO_ENLARGE_BRANDS = setOf("apple", "fujifilm", "leica", "lumix", "samsung", "xiaomi")
private val FRAME_CENTERED_LOGO_SHRINK_BRANDS = setOf("hasselblad", "insta360", "osmo_action")
private const val TEMPLATE_PREVIEW_BRAND_KEY = "canon"
private const val TEMPLATE_PREVIEW_MODEL_LINE = "EOS 6D Mark II"
internal data class PresetColorOption(
    val label: String,
    val compose: Color,
    val android: Int
)
private data class WatermarkFontOption(
    val key: String,
    val label: String,
    val assetPath: String
)
private val SHARED_WATERMARK_FONT_OPTIONS = listOf(
    WatermarkFontOption("inter_variable", "Inter", "InterVariable.ttf"),
    WatermarkFontOption("leiko", "Leiko", "Leiko-Regular.otf"),
    WatermarkFontOption("clockwise", "clockwise", "watermark_fonts/clockwise_light.otf"),
    WatermarkFontOption("harshita", "Harshita", "Harshita.ttf"),
    WatermarkFontOption("daddy", "Daddy", "watermark_fonts/daddy.ttf"),
    WatermarkFontOption("nicotype", "NicoType", "watermark_fonts/nicotype.ttf"),
    WatermarkFontOption("migha_regular", "Migha", "watermark_fonts/migha_regular.otf"),
    WatermarkFontOption("crooked_regular", "Crooked", "watermark_fonts/crooked_regular.ttf"),
    WatermarkFontOption("kallem", "Kallem", "watermark_fonts/kallem.otf"),
    WatermarkFontOption("ll_black_matrix", "LL Black Matrix", "watermark_fonts/ll_black_matrix.ttf"),
    WatermarkFontOption("vervelle_script", "Vervelle Script", "watermark_fonts/vervelle_script.otf"),
    WatermarkFontOption("dexotick", "Dexotick", "watermark_fonts/dexotick.otf")
)
private val WATERMARK_TEXT_FONT_OPTIONS = SHARED_WATERMARK_FONT_OPTIONS
private val SIGNATURE_FONT_OPTIONS = SHARED_WATERMARK_FONT_OPTIONS
private val SHARED_WATERMARK_FONT_OPTION_MAP = SHARED_WATERMARK_FONT_OPTIONS.associateBy { it.key }
private val WATERMARK_TEXT_FONT_OPTION_MAP = SHARED_WATERMARK_FONT_OPTION_MAP
private val SIGNATURE_FONT_OPTION_MAP = SHARED_WATERMARK_FONT_OPTION_MAP
internal val FLOATING_COLOR_OPTIONS = listOf(
    PresetColorOption("红", Color(0xFFE0B6B2), AndroidColor.parseColor("#E0B6B2")),
    PresetColorOption("橙", Color(0xFFE5C8AF), AndroidColor.parseColor("#E5C8AF")),
    PresetColorOption("黄", Color(0xFFE4D9BE), AndroidColor.parseColor("#E4D9BE")),
    PresetColorOption("绿", Color(0xFFC2D1BE), AndroidColor.parseColor("#C2D1BE")),
    PresetColorOption("青", Color(0xFFBCD3D4), AndroidColor.parseColor("#BCD3D4")),
    PresetColorOption("蓝", Color(0xFFC0CCE0), AndroidColor.parseColor("#C0CCE0")),
    PresetColorOption("紫", Color(0xFFD0C7E2), AndroidColor.parseColor("#D0C7E2")),
    PresetColorOption("黑", Color(0xFF111111), AndroidColor.parseColor("#111111")),
    PresetColorOption("白", Color(0xFFFFFFFF), AndroidColor.parseColor("#FFFFFF"))
)
private const val DEFAULT_WATERMARK_CUTOUT_BREAKOUT_BACKGROUND_COLOR_INDEX = 7
private val WATERMARK_CUTOUT_BREAKOUT_BACKGROUND_OPTIONS = FLOATING_COLOR_OPTIONS
    .mapIndexedNotNull { index, option ->
        if (option.android == AndroidColor.WHITE) null else index to option
    }

private fun resolveWatermarkCutoutBreakoutBackgroundColorIndex(colorIndex: Int): Int {
    val normalized = colorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
    return WATERMARK_CUTOUT_BREAKOUT_BACKGROUND_OPTIONS
        .firstOrNull { (index, _) -> index == normalized }
        ?.first
        ?: DEFAULT_WATERMARK_CUTOUT_BREAKOUT_BACKGROUND_COLOR_INDEX
}

private fun resolveWatermarkCutoutBreakoutBackgroundColor(colorIndex: Int): Int =
    FLOATING_COLOR_OPTIONS.getOrElse(
        resolveWatermarkCutoutBreakoutBackgroundColorIndex(colorIndex)
    ) {
        FLOATING_COLOR_OPTIONS[DEFAULT_WATERMARK_CUTOUT_BREAKOUT_BACKGROUND_COLOR_INDEX]
    }.android

private val FLOATING_STROKE_OPTIONS = listOf(
    PresetColorOption("无", Color.Transparent, AndroidColor.TRANSPARENT)
) + FLOATING_COLOR_OPTIONS
private val FULL_COLOR_BACKGROUND_OPTIONS = FLOATING_COLOR_OPTIONS + listOf(
    PresetColorOption("Crimson", Color(0xFFC94B4B), AndroidColor.parseColor("#C94B4B")),
    PresetColorOption("Amber", Color(0xFFD98B2B), AndroidColor.parseColor("#D98B2B")),
    PresetColorOption("Moss", Color(0xFF5F7D4E), AndroidColor.parseColor("#5F7D4E")),
    PresetColorOption("Forest", Color(0xFF2F7556), AndroidColor.parseColor("#2F7556")),
    PresetColorOption("Ocean", Color(0xFF2F6F9F), AndroidColor.parseColor("#2F6F9F")),
    PresetColorOption("Indigo", Color(0xFF4D5AA8), AndroidColor.parseColor("#4D5AA8")),
    PresetColorOption("Violet", Color(0xFF7A4FA1), AndroidColor.parseColor("#7A4FA1")),
    PresetColorOption("Graphite", Color(0xFF3A3E45), AndroidColor.parseColor("#3A3E45"))
)
private val SIGNATURE_COLOR_OPTIONS = listOf(
    PresetColorOption("自动", Color.Transparent, Int.MIN_VALUE)
) + FLOATING_COLOR_OPTIONS
private val SOCIAL_CROP_PRESETS = listOf(
    SocialCropPreset("ratio_1x1", "方图", "1:1", 1080, 1080),
    SocialCropPreset("ratio_4x3", "横图", "4:3", 1440, 1080),
    SocialCropPreset("ratio_3x4", "竖图", "3:4", 1080, 1440),
    SocialCropPreset("ratio_4x5", "图文", "4:5", 1080, 1350),
    SocialCropPreset("ratio_16x9", "宽屏", "16:9", 1920, 1080),
    SocialCropPreset("ratio_9x16", "竖屏", "9:16", 1080, 1920, isVideo = true)
)
private val SOCIAL_CROP_PRESET_MAP by lazy { SOCIAL_CROP_PRESETS.associateBy { it.key } }
private fun resolveOutputSizeOverride(
    template: WatermarkTemplatePreset,
    preset: SocialCropPreset?
): Pair<Int, Int>? = if (template in setOf(
        WatermarkTemplatePreset.FilmRetro,
        WatermarkTemplatePreset.Polaroid,
        WatermarkTemplatePreset.PaperDots
    )
) {
    null
} else {
    preset?.let { it.width to it.height }
}
private fun resolveCropAspectRatioForTemplate(
    template: WatermarkTemplatePreset,
    preset: SocialCropPreset,
    frameSideTopBorderRatio: Float,
    colorPaletteStyle: WatermarkColorPaletteStyle,
    colorPaletteVariantIndex: Int = 0,
    colorPaletteShowHexValue: Boolean = true,
    colorPaletteShowRgbValue: Boolean = true
): Float {
    val targetWidth = preset.width.toFloat().coerceAtLeast(1f)
    val targetHeight = preset.height.toFloat().coerceAtLeast(1f)
    val maxDim = max(targetWidth, targetHeight)
    val aspect = when (template) {
        WatermarkTemplatePreset.WatermarkFrame -> {
            val pad = maxDim * frameSideTopBorderRatio.coerceIn(0f, 0.06f)
            val bar = (maxDim * 0.168f).coerceAtLeast(132f)
            val photoW = (targetWidth - pad * 2f).coerceAtLeast(1f)
            val photoH = (targetHeight - pad - bar).coerceAtLeast(1f)
            photoW / photoH
        }
        WatermarkTemplatePreset.BlurBackground -> {
            val pad = (maxDim * 0.08f).coerceAtLeast(26f)
            val bar = (maxDim * 0.13f).coerceAtLeast(108f)
            val photoW = (targetWidth - pad * 2f).coerceAtLeast(1f)
            val photoH = (targetHeight - pad * 2f - bar).coerceAtLeast(1f)
            photoW / photoH
        }
        WatermarkTemplatePreset.ThinWhiteBorder -> {
            val border = (maxDim * 0.015f).coerceAtLeast(8f)
            val photoW = (targetWidth - border * 2f).coerceAtLeast(1f)
            val photoH = (targetHeight - border * 2f).coerceAtLeast(1f)
            photoW / photoH
        }
        WatermarkTemplatePreset.Floating -> {
            val pad = (maxDim * 0.115f).coerceAtLeast(42f)
            val photoW = (targetWidth - pad * 2f).coerceAtLeast(1f)
            val photoH = (targetHeight - pad * 2f).coerceAtLeast(1f)
            photoW / photoH
        }
        WatermarkTemplatePreset.DirectWatermark -> preset.aspectRatio
        WatermarkTemplatePreset.FullColor -> targetWidth / (targetHeight * 0.5f).coerceAtLeast(1f)
        WatermarkTemplatePreset.Polaroid -> preset.aspectRatio
        WatermarkTemplatePreset.FilmRetro -> preset.aspectRatio
        WatermarkTemplatePreset.PaperDots -> preset.aspectRatio
        WatermarkTemplatePreset.ColorPalette -> resolveWatermarkColorTemplateCropAspectRatio(
            style = colorPaletteStyle,
            outputWidth = preset.width,
            outputHeight = preset.height,
            variantIndex = colorPaletteVariantIndex,
            showHexValue = colorPaletteShowHexValue,
            showRgbValue = colorPaletteShowRgbValue
        )
    }
    return if (aspect.isFinite() && aspect > 0f) aspect else preset.aspectRatio
}
private val TEMPLATE_PREVIEW_EXIF_INFO = WatermarkExifInfo(
    make = "Canon",
    model = "EOS 6D Mark II",
    iso = "100",
    shutter = "1/250",
    focalLength = "35",
    aperture = "2.8",
    dateTimeOriginal = "2026:01:19 15:51:05",
    latitude = 31.2304,
    longitude = 121.4737,
    cityLocation = "Shanghai, China"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatermarkFrameView(
    onBack: () -> Unit,
    openSessionToken: Long = 0L,
    resetForLegacyEntryOnOpen: Boolean = false,
    initialModernPresetKey: String? = null,
    initialModernImageUriString: String? = null,
    initialBackdropBitmap: Bitmap? = null,
    autoLaunchPickerForInitialModernPreset: Boolean = false,
    onInitialModernPresetConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val watermarkStrings = remember(appLanguage) { getWatermarkUiStrings(appLanguage) }
    val watermarkExtraCopy = remember(appLanguage) { getWatermarkExtraCopy(appLanguage) }
    val prefs = remember(context) { context.getSharedPreferences("photoo_prefs", Context.MODE_PRIVATE) }
    val copyrightState = rememberWatermarkCopyrightState(prefs)
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val performHaptic = remember(haptic, hapticsAllowed) {
        {
            if (hapticsAllowed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    val initialModernPreset = remember(initialModernPresetKey) {
        resolveWatermarkModernPresetSpec(initialModernPresetKey)
    }
    val fallbackPageBackground = MaterialTheme.colorScheme.background
    val screenBackdropWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }.coerceAtLeast(720)
    val screenBackdropHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }.coerceAtLeast(1280)
    val topBarButtonBackground = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.68f else 0.78f)
    val topBarIconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val listItemBackground = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.34f else 0.62f)
    val previewCardBackground = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.28f else 0.54f)
    val panelSelectedBackground = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color(0xFFEAF2FF)
    val panelBackground = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.34f else 0.58f)
    val cardOutlineColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.26f) else Color.Black.copy(alpha = 0.08f)
    val loadingOverlayColor = if (isDark) MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.20f)
    val errorTextColor = if (isDark) MaterialTheme.colorScheme.error else Color(0xFFDC2626)
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val actionButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onBackground
    )
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = panelBackground,
        labelColor = MaterialTheme.colorScheme.onBackground,
        iconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
        selectedContainerColor = panelSelectedBackground,
        selectedLabelColor = MaterialTheme.colorScheme.onBackground,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = panelBackground.copy(alpha = 0.64f),
        disabledLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
        disabledLeadingIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f)
    )
    val dialogTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
    val dialogActionTextColor = MaterialTheme.colorScheme.onSurface
    val shouldOpenInitialModernPresetDirectly = initialModernPreset != null &&
        (autoLaunchPickerForInitialModernPreset || initialModernImageUriString != null)
    var selectedTemplate by remember { mutableStateOf(initialModernPreset?.template ?: WatermarkTemplatePreset.WatermarkFrame) }
    var selectedImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var exifSourceUriForSave by rememberSaveable { mutableStateOf<String?>(null) }
    var renderedFile by remember { mutableStateOf<File?>(null) }
    var renderedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewSourceBitmapKey by rememberSaveable { mutableStateOf<String?>(null) }
    var previewSpecialModeForegroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewSpecialModeForegroundKey by rememberSaveable { mutableStateOf<String?>(null) }
    var renderedMimeType by remember { mutableStateOf("image/jpeg") }
    var isWorking by remember { mutableStateOf(false) }
    var isSavingOutput by remember { mutableStateOf(false) }
    var isImportingSourceImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var currentExifInfo by remember { mutableStateOf<WatermarkExifInfo?>(null) }
    var activeSelectedSourceFile by remember { mutableStateOf<File?>(null) }
    var detectedBrandKey by remember { mutableStateOf<String?>(null) }
    var selectedBrandKey by remember { mutableStateOf<String?>(null) }
    var sessionManualBrandKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedVariantIndex by remember { mutableStateOf(0) }
    var selectedFilmFrameIndex by rememberSaveable { mutableStateOf(0) }
    var selectedPolaroidFrameIndex by rememberSaveable { mutableStateOf(0) }
    var selectedColorPaletteStyleIndex by rememberSaveable { mutableStateOf(0) }
    var selectedColorPaletteVariantIndex by rememberSaveable { mutableStateOf(0) }
    var colorPaletteRefreshNonce by rememberSaveable { mutableStateOf(0) }
    var colorPaletteHorizontalOffsetRatio by rememberSaveable { mutableStateOf(0f) }
    var colorPaletteHorizontalOffsetRatioDraft by rememberSaveable { mutableStateOf(0f) }
    var colorPaletteVerticalOffsetRatio by rememberSaveable { mutableStateOf(0f) }
    var colorPaletteVerticalOffsetRatioDraft by rememberSaveable { mutableStateOf(0f) }
    var colorPaletteElementCount by rememberSaveable { mutableStateOf(6) }
    var colorPaletteElementCountDraft by rememberSaveable { mutableStateOf(6f) }
    var colorPaletteShowHexValue by rememberSaveable { mutableStateOf(true) }
    var colorPaletteShowRgbValue by rememberSaveable { mutableStateOf(true) }
    var paperDotsLayoutDirection by rememberSaveable {
        mutableStateOf(WatermarkPaperDotsLayoutDirection.ImageTop)
    }
    var paperDotsSplitRatio by rememberSaveable { mutableStateOf(0.5f) }
    var paperDotsSplitRatioDraft by rememberSaveable { mutableStateOf(0.5f) }
    var paperDotsBackgroundMode by rememberSaveable {
        mutableStateOf(WatermarkPaperDotsBackgroundMode.Solid)
    }
    var paperDotsBackgroundColorIndex by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_COLOR_INDEX)
    }
    var paperDotsBackgroundSecondaryColorIndex by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_SECONDARY_COLOR_INDEX)
    }
    var paperDotsStripeSizeRatio by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_STRIPE_SIZE_RATIO)
    }
    var paperDotsStripeSizeRatioDraft by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_STRIPE_SIZE_RATIO)
    }
    var paperDotsShape by rememberSaveable { mutableStateOf(WatermarkPaperDotsDotShape.Circle) }
    var paperDotsDotSize by rememberSaveable { mutableStateOf(28f) }
    var paperDotsDotSizeDraft by rememberSaveable { mutableStateOf(28f) }
    var paperDotsDotVariance by rememberSaveable { mutableStateOf(32f) }
    var paperDotsDotVarianceDraft by rememberSaveable { mutableStateOf(32f) }
    var paperDotsDotCount by rememberSaveable { mutableStateOf(18) }
    var paperDotsDotCountDraft by rememberSaveable { mutableStateOf(18f) }
    var paperDotsRandomSeed by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_RANDOM_SEED)
    }
    var paperDotsStrokeMode by rememberSaveable {
        mutableStateOf(WatermarkPaperDotsStrokeMode.AllFill)
    }
    var paperDotsStrokeWidthRatio by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_STROKE_WIDTH_RATIO)
    }
    var paperDotsStrokeWidthRatioDraft by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_STROKE_WIDTH_RATIO)
    }
    var paperDotsStrokeMixRatio by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_STROKE_MIX_RATIO)
    }
    var paperDotsStrokeMixRatioDraft by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_STROKE_MIX_RATIO)
    }
    var paperDotsPatternText by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_PAPER_DOTS_PATTERN_TEXT)
    }
    var showLogoWatermark by rememberSaveable { mutableStateOf(true) }
    var showModelWatermark by rememberSaveable { mutableStateOf(false) }
    var showDateWatermark by rememberSaveable { mutableStateOf(true) }
    var showExifWatermark by rememberSaveable { mutableStateOf(true) }
    var showLocationWatermark by rememberSaveable { mutableStateOf(false) }
    var showCityLocationWatermark by rememberSaveable { mutableStateOf(false) }
    var showFullColorNoteWatermark by remember { mutableStateOf(false) }
    var signatureEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("watermark_signature_enabled", false)) }
    var likedTemplateKeys by rememberSaveable {
        mutableStateOf(
            prefs.getStringSet("watermark_liked_template_keys", emptySet())
                ?.toSet()
                ?: emptySet()
        )
    }
    var frameSideTopBorderRatio by rememberSaveable { mutableStateOf(0.022f) }
    var frameSideTopBorderRatioDraft by rememberSaveable { mutableStateOf(0.022f) }
    var frameLayoutMode by rememberSaveable { mutableStateOf(WatermarkFrameLayoutMode.Center) }
    var logoScaleFactor by rememberSaveable { mutableStateOf(1f) }
    var logoScaleFactorDraft by rememberSaveable { mutableStateOf(1f) }
    var frameBackgroundColorIndex by rememberSaveable { mutableStateOf(8) }
    var thinBorderColorIndex by rememberSaveable { mutableStateOf(8) }
    var roundedBorderColorIndex by rememberSaveable { mutableStateOf(8) }
    var borderCornerMode by rememberSaveable { mutableStateOf(BorderCornerMode.Straight) }
    var blurImageCornerRadiusRatio by rememberSaveable { mutableStateOf(DEFAULT_BLUR_IMAGE_CORNER_RATIO) }
    var blurImageCornerRadiusRatioDraft by rememberSaveable { mutableStateOf(DEFAULT_BLUR_IMAGE_CORNER_RATIO) }
    var borderImageScaleFactor by rememberSaveable { mutableStateOf(1f) }
    var borderImageScaleFactorDraft by rememberSaveable { mutableStateOf(1f) }
    var cutoutOverlayStyle by rememberSaveable { mutableStateOf(WatermarkCutoutOverlayStyle.None) }
    var cutoutSubjectScaleFactor by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR)
    }
    var cutoutSubjectScaleFactorDraft by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR)
    }
    var cutoutFrameAspectRatio by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO)
    }
    var subjectOutlineEnabled by rememberSaveable { mutableStateOf(false) }
    var subjectOutlineLineStyle by rememberSaveable {
        mutableStateOf(WatermarkSubjectOutlineLineStyle.Solid)
    }
    var subjectOutlineWidthRatio by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO)
    }
    var subjectOutlineWidthRatioDraft by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO)
    }
    var subjectOutlineColorIndex by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX)
    }
    var subjectOutlineOuterBorderEnabled by rememberSaveable { mutableStateOf(false) }
    var subjectOutlineOffsetRatio by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO)
    }
    var subjectOutlineOffsetRatioDraft by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO)
    }
    var subjectOutlineSymbolText by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT)
    }
    var subjectOutlineSymbolSizeRatio by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO)
    }
    var subjectOutlineSymbolSizeRatioDraft by rememberSaveable {
        mutableStateOf(DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO)
    }
    var floatingBackgroundColorIndex by rememberSaveable { mutableStateOf(8) }
    var floatingStrokeColorIndex by rememberSaveable { mutableStateOf(8) }
    var floatingImageCornerRadiusRatio by rememberSaveable { mutableStateOf(DEFAULT_FLOATING_IMAGE_CORNER_RATIO) }
    var floatingImageCornerRadiusRatioDraft by rememberSaveable { mutableStateOf(DEFAULT_FLOATING_IMAGE_CORNER_RATIO) }
    var resolvedModelLabel by remember { mutableStateOf("--") }
    var customModelLabel by rememberSaveable { mutableStateOf("") }
    var customDateLabel by rememberSaveable { mutableStateOf("") }
    var customLocationLabel by rememberSaveable { mutableStateOf("") }
    var customFullColorNoteText by remember { mutableStateOf("") }
    var fullColorTextScaleFactor by rememberSaveable { mutableStateOf(DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR) }
    var fullColorTextScaleFactorDraft by rememberSaveable { mutableStateOf(DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR) }
    var fullColorHorizontalOffsetRatio by rememberSaveable { mutableStateOf(0f) }
    var fullColorVerticalOffsetRatio by rememberSaveable { mutableStateOf(0f) }
    var fullColorBackgroundColorIndex by rememberSaveable { mutableStateOf(FULL_COLOR_AUTO_BACKGROUND_INDEX) }
    var fullColorUseGradientBackground by rememberSaveable { mutableStateOf(false) }
    var fullColorGradientPresetIndex by rememberSaveable { mutableStateOf(-1) }
    var fullColorImageOnTop by rememberSaveable { mutableStateOf(false) }
    var fullColorCustomBackgroundColor by rememberSaveable {
        mutableStateOf(DEFAULT_FULL_COLOR_CUSTOM_BACKGROUND_COLOR)
    }
    var customLogoPath by rememberSaveable {
        mutableStateOf(prefs.getString(WATERMARK_PREF_CUSTOM_LOGO_PATH, "").orEmpty())
    }
    var signatureText by rememberSaveable {
        mutableStateOf(prefs.getString("watermark_signature_text", "") ?: "")
    }
    var signatureColorIndex by rememberSaveable { mutableStateOf(0) }
    var signatureHorizontalOffsetRatio by rememberSaveable {
        mutableStateOf(prefs.getFloat(WATERMARK_PREF_SIGNATURE_OFFSET_X, 0f).coerceIn(-1f, 1f))
    }
    var signatureHorizontalOffsetRatioDraft by rememberSaveable { mutableStateOf(signatureHorizontalOffsetRatio) }
    var signatureVerticalOffsetRatio by rememberSaveable {
        mutableStateOf(prefs.getFloat(WATERMARK_PREF_SIGNATURE_OFFSET_Y, 0f).coerceIn(-1f, 1f))
    }
    var signatureVerticalOffsetRatioDraft by rememberSaveable { mutableStateOf(signatureVerticalOffsetRatio) }
    var signatureScaleFactor by rememberSaveable {
        mutableStateOf(prefs.getFloat(WATERMARK_PREF_SIGNATURE_SCALE, 1f).coerceIn(0.65f, 1.8f))
    }
    var signatureScaleFactorDraft by rememberSaveable { mutableStateOf(signatureScaleFactor) }
    var watermarkFontKey by rememberSaveable {
        mutableStateOf(
            normalizeWatermarkFontKey(
                prefs.getString(WATERMARK_PREF_TEXT_FONT_KEY, DEFAULT_WATERMARK_TEXT_FONT_KEY)
            )
        )
    }
    var signatureFontKey by rememberSaveable {
        mutableStateOf(
            normalizeSignatureFontKey(
                prefs.getString(WATERMARK_PREF_SIGNATURE_FONT_KEY, DEFAULT_SIGNATURE_FONT_KEY)
            )
        )
    }
    val signatureHistory = remember {
        androidx.compose.runtime.mutableStateListOf<String>().apply {
            addAll(decodeSignatureHistory(prefs.getString("watermark_signature_history", null)))
        }
    }
    val userPresets = remember {
        androidx.compose.runtime.mutableStateListOf<WatermarkUserPreset>().apply {
            addAll(decodeWatermarkUserPresets(prefs.getString(WATERMARK_USER_PRESET_PREF_KEY, null)))
        }
    }
    var isAddSignatureDialogVisible by rememberSaveable { mutableStateOf(false) }
    var addSignatureInput by rememberSaveable { mutableStateOf("") }
    var pendingDeleteSignature by rememberSaveable { mutableStateOf<String?>(null) }
    var isSavePresetDialogVisible by rememberSaveable { mutableStateOf(false) }
    var savePresetNameInput by rememberSaveable { mutableStateOf("") }
    var pendingDeletePresetId by rememberSaveable { mutableStateOf<String?>(null) }
    var customizePanelKey by rememberSaveable { mutableStateOf(WatermarkCustomizePanel.Crop.name) }
    var collapsedCustomizePanel by rememberSaveable { mutableStateOf(false) }
    var returnCustomizePanelKeyFromCopyright by rememberSaveable { mutableStateOf(WatermarkCustomizePanel.Copyright.name) }
    var isModelDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isCustomDateDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isCustomLocationDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isFullColorNoteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isFullColorCustomColorDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isBrandCorrectionDialogVisible by rememberSaveable { mutableStateOf(false) }
    var brandCorrectionDraftKey by rememberSaveable { mutableStateOf("photoo") }
    var currentStep by rememberSaveable {
        mutableStateOf(
            if (shouldOpenInitialModernPresetDirectly) {
                WatermarkFlowStep.Preview
            } else {
                WatermarkFlowStep.Template
            }
        )
    }
    var selectedPresetId by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedInitialModernPresetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedInitialModernImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSocialCropPresetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var cropEditorPresetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var cropEditorAspectRatio by rememberSaveable { mutableStateOf(1f) }
    var cropCenterX by remember { mutableStateOf(0f) }
    var cropCenterY by remember { mutableStateOf(0f) }
    var cropZoom by remember { mutableStateOf(1f) }
    val cropEditorUndoHistory = remember { mutableStateListOf<WatermarkCropEditorHistoryEntry>() }
    var cropSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeCropSourceFile by remember { mutableStateOf<File?>(null) }
    var templatePreviewBitmaps by remember {
        mutableStateOf(snapshotWatermarkTemplatePreviewBitmapMemoryCache())
    }
    var isTemplatePreviewWorking by remember { mutableStateOf(false) }
    var isRenderingPreview by remember { mutableStateOf(false) }
    var showLoadingOverlay by remember { mutableStateOf(false) }
    var loadingOverlayShownAt by remember { mutableStateOf(0L) }
    var pendingPreviewScrollRestore by remember { mutableStateOf<Int?>(null) }
    var shouldResetPreviewScrollOnNextRender by remember { mutableStateOf(false) }
    var previousFlowStep by remember { mutableStateOf(currentStep) }
    var renderRequestVersion by remember { mutableStateOf(0L) }
    var previewRenderJob by remember { mutableStateOf<Job?>(null) }
    var fullColorLastPreviewDragRenderAt by remember { mutableStateOf(0L) }
    var awaitingInitialModernImagePicker by rememberSaveable { mutableStateOf(false) }
    var screenBackdropBitmap by remember(initialBackdropBitmap) { mutableStateOf(initialBackdropBitmap) }
    var previewBackdropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val backdropVisuals = rememberHomeCardLongPressHoverVisuals(active = true)
    var suppressPreviewDockInputUntil by remember { mutableStateOf(0L) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val previewScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val templateListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val selectedUri = selectedImageUri?.let(Uri::parse)
    val selectedExifSourceUri = exifSourceUriForSave?.let(Uri::parse)
    val selectedSocialCropPreset = selectedSocialCropPresetKey?.let { SOCIAL_CROP_PRESET_MAP[it] }
    val forcedFullColorCropPreset = SOCIAL_CROP_PRESET_MAP["ratio_3x4"]
    val effectiveSelectedSocialCropPreset = if (
        selectedTemplate == WatermarkTemplatePreset.FullColor && forcedFullColorCropPreset != null
    ) {
        forcedFullColorCropPreset
    } else {
        selectedSocialCropPreset
    }
    val cropEditorPreset = cropEditorPresetKey?.let { SOCIAL_CROP_PRESET_MAP[it] }
    val currentCropEditorHistoryEntry: () -> WatermarkCropEditorHistoryEntry = {
        WatermarkCropEditorHistoryEntry(
            presetKey = cropEditorPresetKey,
            aspectRatio = cropEditorAspectRatio,
            centerX = cropCenterX,
            centerY = cropCenterY,
            zoom = cropZoom
        )
    }
    val applyCropEditorHistoryEntry: (WatermarkCropEditorHistoryEntry) -> Unit = { entry ->
        cropEditorPresetKey = entry.presetKey
        cropEditorAspectRatio = entry.aspectRatio
        cropCenterX = entry.centerX
        cropCenterY = entry.centerY
        cropZoom = entry.zoom
    }
    val updateCropEditorState: (WatermarkCropEditorHistoryEntry, Boolean) -> Unit = { nextEntry, recordUndo ->
        val currentEntry = currentCropEditorHistoryEntry()
        if (recordUndo && nextEntry != currentEntry && cropEditorUndoHistory.lastOrNull() != currentEntry) {
            cropEditorUndoHistory.add(currentEntry)
        }
        if (nextEntry != currentEntry) {
            applyCropEditorHistoryEntry(nextEntry)
        }
    }
    val undoCropEditorChange: () -> Unit = {
        if (cropEditorUndoHistory.isNotEmpty()) {
            val previousEntry = cropEditorUndoHistory.removeAt(cropEditorUndoHistory.lastIndex)
            applyCropEditorHistoryEntry(previousEntry)
        }
    }
    val resetCropEditorView: () -> Unit = {
        cropSourceBitmap?.let { source ->
            updateCropEditorState(
                WatermarkCropEditorHistoryEntry(
                    presetKey = cropEditorPresetKey,
                    aspectRatio = cropEditorAspectRatio,
                    centerX = source.width / 2f,
                    centerY = source.height / 2f,
                    zoom = 1f
                ),
                true
            )
        }
    }
    val selectedColorPaletteStyle = resolveWatermarkColorPaletteStyle(selectedColorPaletteStyleIndex)
    val selectedColorPaletteVariantOptions = resolveWatermarkPanelColorPaletteVariantOptions(selectedColorPaletteStyle, appLanguage)
    val selectedColorPaletteVariantLabel = selectedColorPaletteVariantOptions
        .firstOrNull { it.index == selectedColorPaletteVariantIndex }
        ?.label
    val selectedColorPaletteMinimumCount = resolveWatermarkPanelColorPaletteMinimumCount(
        selectedColorPaletteStyle,
        selectedColorPaletteVariantIndex
    )
    val resolveCurrentPaperDotsConfig = {
        WatermarkPaperDotsConfig(
            layoutDirection = paperDotsLayoutDirection,
            splitRatio = paperDotsSplitRatio,
            backgroundMode = paperDotsBackgroundMode,
            backgroundColorIndex = paperDotsBackgroundColorIndex,
            backgroundSecondaryColorIndex = paperDotsBackgroundSecondaryColorIndex,
            stripeSizeRatio = paperDotsStripeSizeRatio,
            dotShape = paperDotsShape,
            dotSize = paperDotsDotSize,
            dotVariance = paperDotsDotVariance,
            dotCount = paperDotsDotCount,
            randomSeed = paperDotsRandomSeed,
            strokeMode = paperDotsStrokeMode,
            strokeWidthRatio = paperDotsStrokeWidthRatio,
            strokeMixRatio = paperDotsStrokeMixRatio,
            patternText = paperDotsPatternText
        )
    }
    val resolveCurrentSubjectOutlineConfig = {
        WatermarkSubjectOutlineConfig(
            enabled = subjectOutlineEnabled,
            colorIndex = subjectOutlineColorIndex,
            lineStyle = subjectOutlineLineStyle,
            widthRatio = subjectOutlineWidthRatio,
            outerBorderEnabled = subjectOutlineOuterBorderEnabled,
            offsetRatio = subjectOutlineOffsetRatio,
            symbolText = subjectOutlineSymbolText,
            symbolSizeRatio = subjectOutlineSymbolSizeRatio
        ).normalized()
    }
    val outputSizeOverride = resolveOutputSizeOverride(selectedTemplate, effectiveSelectedSocialCropPreset)
    val previewOutputSizeOverride = resolveWatermarkPreviewOutputSizeOverride(outputSizeOverride)
    val orderedTemplates = remember(likedTemplateKeys) {
        WatermarkTemplatePreset.entries.sortedWith(
            compareBy<WatermarkTemplatePreset> { if (likedTemplateKeys.contains(it.name)) 0 else 1 }
                .thenBy { WatermarkTemplatePreset.entries.indexOf(it) }
        )
    }
    val templatePreviewDemoAssets = rememberColorCardFilterFeedDemoAssets()
    val templatePreviewDemoAssetsByPreset = remember(templatePreviewDemoAssets) {
        if (templatePreviewDemoAssets.isEmpty()) {
            emptyMap()
        } else {
            WatermarkTemplatePreset.entries.mapIndexed { index, preset ->
                preset to templatePreviewDemoAssets[index % templatePreviewDemoAssets.size]
            }.toMap()
        }
    }
    var templatePageOrderedTemplates by remember { mutableStateOf(orderedTemplates) }
    val selectedCustomizePanel = runCatching { WatermarkCustomizePanel.valueOf(customizePanelKey) }
        .getOrDefault(WatermarkCustomizePanel.Crop)
    val fullColorUiCopy = remember(appLanguage) { resolveFullColorUiCopy(appLanguage) }
    val effectiveModelLabel = customModelLabel.trim().ifBlank { resolvedModelLabel }.ifBlank { "--" }
    val applyFullColorExifOverrides: (WatermarkExifInfo?) -> WatermarkExifInfo? = { base ->
        if (selectedTemplate != WatermarkTemplatePreset.FullColor) {
            base
        } else {
            val dateOverride = customDateLabel.trim()
            val locationOverride = customLocationLabel.trim()
            val seed = base ?: WatermarkExifInfo()
            if (base == null && dateOverride.isBlank() && locationOverride.isBlank()) {
                null
            } else {
                seed.copy(
                    dateTimeOriginal = dateOverride.ifBlank { seed.dateTimeOriginal },
                    cityLocation = locationOverride.ifBlank { seed.cityLocation }
                )
            }
        }
    }
    val effectiveFullColorShowDate = if (selectedTemplate == WatermarkTemplatePreset.FullColor) {
        showDateWatermark && !showFullColorNoteWatermark && customDateLabel.trim().isNotBlank()
    } else {
        showDateWatermark
    }
    val effectiveFullColorShowCityLocation = if (selectedTemplate == WatermarkTemplatePreset.FullColor) {
        showCityLocationWatermark && !showFullColorNoteWatermark && customLocationLabel.trim().isNotBlank()
    } else {
        showCityLocationWatermark
    }
    val effectiveFullColorNoteText = if (
        selectedTemplate == WatermarkTemplatePreset.FullColor &&
        showFullColorNoteWatermark
    ) {
        customFullColorNoteText.trim()
    } else {
        ""
    }
    val effectiveExifInfo = applyFullColorExifOverrides(currentExifInfo)
    val resolveFullColorBackgroundColorOverride: () -> Int? = {
        if (selectedTemplate != WatermarkTemplatePreset.FullColor) {
            null
        } else {
            when (fullColorBackgroundColorIndex) {
                FULL_COLOR_AUTO_BACKGROUND_INDEX -> null
                FULL_COLOR_CUSTOM_BACKGROUND_INDEX -> fullColorCustomBackgroundColor
                else -> FULL_COLOR_BACKGROUND_OPTIONS.getOrNull(fullColorBackgroundColorIndex)?.android
            }
        }
    }
    val resolveSignatureColorOverride: () -> Int? = {
        SIGNATURE_COLOR_OPTIONS
            .getOrElse(signatureColorIndex.coerceIn(0, SIGNATURE_COLOR_OPTIONS.lastIndex)) { SIGNATURE_COLOR_OPTIONS.first() }
            .android
            .takeIf { it != Int.MIN_VALUE }
    }
    val persistSignatureHistory = {
        prefs.edit().putString("watermark_signature_history", encodeSignatureHistory(signatureHistory)).apply()
    }
    val persistUserPresets = {
        prefs.edit().putString(WATERMARK_USER_PRESET_PREF_KEY, encodeWatermarkUserPresets(userPresets)).apply()
    }
    val replaceRenderedPreviewBitmap: (Bitmap?) -> Unit = { bitmap ->
        val previous = renderedPreviewBitmap
        renderedPreviewBitmap = bitmap
        if (previous != null && previous !== bitmap) {
            recycleBitmapQuietly(previous)
        }
    }
    val replacePreviewSourceBitmap: (String?, Bitmap?) -> Unit = { key, bitmap ->
        val previous = previewSourceBitmap
        previewSourceBitmapKey = key
        previewSourceBitmap = bitmap
        if (previous != null && previous !== bitmap) {
            recycleBitmapQuietly(previous)
        }
    }
    val replacePreviewSpecialModeForegroundBitmap: (String?, Bitmap?) -> Unit = { key, bitmap ->
        val previous = previewSpecialModeForegroundBitmap
        previewSpecialModeForegroundKey = key
        previewSpecialModeForegroundBitmap = bitmap
        if (previous != null && previous !== bitmap) {
            recycleBitmapQuietly(previous)
        }
    }
    val replacePreviewBackdropBitmap: (Bitmap?) -> Unit = { bitmap ->
        val previous = previewBackdropBitmap
        previewBackdropBitmap = bitmap
        if (previous != null && previous !== bitmap) {
            recycleBitmapQuietly(previous)
        }
    }
    val replaceScreenBackdropBitmap: (Bitmap?) -> Unit = { bitmap ->
        val previous = screenBackdropBitmap
        screenBackdropBitmap = bitmap
        if (previous != null && previous !== bitmap) {
            recycleBitmapQuietly(previous)
        }
    }
    LaunchedEffect(screenBackdropBitmap, selectedImageUri) {
        if (screenBackdropBitmap != null || !selectedImageUri.isNullOrBlank()) return@LaunchedEffect
        val activity = context.findActivity() ?: return@LaunchedEffect
        val rootView = activity.window?.decorView?.rootView ?: return@LaunchedEffect
        repeat(3) {
            val captured = captureHomeCardLongPressBackdropBitmapFallback(rootView)
            if (captured != null) {
                replaceScreenBackdropBitmap(captured)
                return@LaunchedEffect
            }
            delay(32L)
        }
    }
    LaunchedEffect(selectedImageUri, selectedTemplate, cutoutOverlayStyle, subjectOutlineEnabled) {
        if (selectedImageUri.isNullOrBlank()) {
            replacePreviewSourceBitmap(null, null)
        }
        val stillNeedsForegroundCache =
            !selectedImageUri.isNullOrBlank() &&
                selectedTemplate == WatermarkTemplatePreset.ThinWhiteBorder &&
                (
                    cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None ||
                        subjectOutlineEnabled
                    )
        if (!stillNeedsForegroundCache) {
            replacePreviewSpecialModeForegroundBitmap(null, null)
        }
    }
    val replaceTemplatePreviewBitmaps: (Map<WatermarkTemplatePreset, Bitmap>) -> Unit = { nextMap ->
        val previous = templatePreviewBitmaps
        cacheWatermarkTemplatePreviewBitmaps(nextMap)
        templatePreviewBitmaps = nextMap
        previous.forEach { (template, bitmap) ->
            if (nextMap[template] !== bitmap && !isWatermarkTemplatePreviewBitmapCached(bitmap)) {
                recycleBitmapQuietly(bitmap)
            }
        }
    }
    val clearRenderedFileCache = {
        val previous = renderedFile
        renderedFile = null
        if (previous != null) {
            runCatching {
                if (previous.exists()) previous.delete()
            }
        }
    }
    val applySignatureTakeover = {
        showLogoWatermark = false
        showModelWatermark = false
        showDateWatermark = false
        showExifWatermark = false
        showLocationWatermark = false
        showCityLocationWatermark = false
        showFullColorNoteWatermark = false
    }
    val disableSignatureIfNeeded = {
        if (signatureEnabled) {
            signatureEnabled = false
            prefs.edit().putBoolean("watermark_signature_enabled", false).apply()
        }
    }
    val shouldAvoidWhiteLogo = {
        when (selectedTemplate) {
            WatermarkTemplatePreset.Floating -> FLOATING_COLOR_OPTIONS
                .getOrElse(floatingBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)) { FLOATING_COLOR_OPTIONS.last() }
                .android == AndroidColor.WHITE
            WatermarkTemplatePreset.WatermarkFrame -> FLOATING_COLOR_OPTIONS
                .getOrElse(frameBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)) { FLOATING_COLOR_OPTIONS.last() }
                .android == AndroidColor.WHITE
            WatermarkTemplatePreset.ThinWhiteBorder -> if (cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None) {
                false
            } else {
                FLOATING_COLOR_OPTIONS
                    .getOrElse(thinBorderColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)) { FLOATING_COLOR_OPTIONS.last() }
                    .android == AndroidColor.WHITE
            }
            else -> false
        }
    }
    val applyPreferredVariantForBrand: (String?) -> Unit = { brandKey ->
        val group = brandKey?.let { BRAND_LOGO_GROUP_MAP[it] }
        selectedVariantIndex = preferredLogoVariantIndex(group, avoidWhite = shouldAvoidWhiteLogo())
    }
    val resolveEffectiveBrandKey: () -> String? = {
        val autoBrandKey = selectedBrandKey ?: detectedBrandKey
        if (showLogoWatermark) autoBrandKey ?: "photoo" else autoBrandKey
    }
    val resolveNextPresetName = {
        nextWatermarkPresetName(userPresets)
    }
    val beginRenderRequest = {
        val next = renderRequestVersion + 1L
        renderRequestVersion = next
        next
    }
    val cancelPendingPreviewRender = {
        previewRenderJob?.cancel()
        previewRenderJob = null
    }
    LaunchedEffect(openSessionToken, resetForLegacyEntryOnOpen) {
        if (!resetForLegacyEntryOnOpen) return@LaunchedEffect
        cancelPendingPreviewRender()
        awaitingInitialModernImagePicker = false
        appliedInitialModernPresetKey = null
        appliedInitialModernImageUriString = null
        selectedPresetId = null
        selectedTemplate = WatermarkTemplatePreset.WatermarkFrame
        replacePreviewSourceBitmap(null, null)
        replacePreviewSpecialModeForegroundBitmap(null, null)
        cutoutOverlayStyle = WatermarkCutoutOverlayStyle.None
        cutoutSubjectScaleFactor = DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
        cutoutSubjectScaleFactorDraft = DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
        cutoutFrameAspectRatio = DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO
        subjectOutlineEnabled = false
        subjectOutlineLineStyle = WatermarkSubjectOutlineLineStyle.Solid
        subjectOutlineWidthRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
        subjectOutlineWidthRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
        subjectOutlineColorIndex = DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX
        subjectOutlineOuterBorderEnabled = false
        subjectOutlineOffsetRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
        subjectOutlineOffsetRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
        subjectOutlineSymbolText = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT
        subjectOutlineSymbolSizeRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
        subjectOutlineSymbolSizeRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
        selectedImageUri = null
        exifSourceUriForSave = null
        selectedSocialCropPresetKey = null
        cropEditorPresetKey = null
        cropEditorUndoHistory.clear()
        currentExifInfo = null
        customDateLabel = ""
        customLocationLabel = ""
        customFullColorNoteText = ""
        showFullColorNoteWatermark = false
        fullColorTextScaleFactor = DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR
        fullColorTextScaleFactorDraft = DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR
        fullColorHorizontalOffsetRatio = 0f
        fullColorVerticalOffsetRatio = 0f
        fullColorBackgroundColorIndex = FULL_COLOR_AUTO_BACKGROUND_INDEX
        fullColorUseGradientBackground = false
        fullColorGradientPresetIndex = -1
        fullColorImageOnTop = false
        fullColorCustomBackgroundColor = DEFAULT_FULL_COLOR_CUSTOM_BACKGROUND_COLOR
        detectedBrandKey = null
        selectedBrandKey = null
        sessionManualBrandKeys = emptyList()
        activeSelectedSourceFile?.let { file ->
            runCatching { if (file.exists()) file.delete() }
        }
        activeCropSourceFile?.let { file ->
            runCatching { if (file.exists()) file.delete() }
        }
        activeSelectedSourceFile = null
        activeCropSourceFile = null
        cropSourceBitmap?.let(::recycleBitmapQuietly)
        cropSourceBitmap = null
        replaceRenderedPreviewBitmap(null)
        clearRenderedFileCache()
        replacePreviewBackdropBitmap(null)
        replaceScreenBackdropBitmap(initialBackdropBitmap)
        customizePanelKey = WatermarkCustomizePanel.Template.name
        collapsedCustomizePanel = false
        errorMessage = null
        toastMessage = null
        runCatching { drawerState.close() }
        currentStep = WatermarkFlowStep.Template
    }
    val applyUserPreset: (WatermarkUserPreset) -> Unit = fun(preset: WatermarkUserPreset) {
        val template = preset.template ?: return
        selectedPresetId = preset.id
        selectedTemplate = template
        selectedBrandKey = preset.selectedBrandKey?.takeIf {
            it == CUSTOM_LOGO_BRAND_KEY || BRAND_LOGO_GROUP_MAP.containsKey(it)
        }
        selectedBrandKey?.let { key ->
            if (key != CUSTOM_LOGO_BRAND_KEY && key != "photoo" && BRAND_LOGO_GROUP_MAP.containsKey(key)) {
                sessionManualBrandKeys = (sessionManualBrandKeys + key).distinct()
            }
        }
        showLogoWatermark = preset.showLogoWatermark
        showModelWatermark = preset.showModelWatermark
        showDateWatermark = preset.showDateWatermark
        showExifWatermark = preset.showExifWatermark
        showLocationWatermark = preset.showLocationWatermark
        showCityLocationWatermark = preset.showCityLocationWatermark
        if (showLocationWatermark && showCityLocationWatermark) {
            showLocationWatermark = false
        }
        customModelLabel = preset.customModelLabel
        customDateLabel = preset.customDateLabel
        customLocationLabel = preset.customLocationLabel
        customFullColorNoteText = ""
        showFullColorNoteWatermark = false
        fullColorTextScaleFactor = preset.fullColorTextScaleFactor.coerceIn(
            FULL_COLOR_TEXT_SCALE_MIN,
            FULL_COLOR_TEXT_SCALE_MAX
        )
        fullColorTextScaleFactorDraft = fullColorTextScaleFactor
        fullColorHorizontalOffsetRatio = 0f
        fullColorVerticalOffsetRatio = 0f
        fullColorBackgroundColorIndex = when (preset.fullColorBackgroundColorIndex) {
            FULL_COLOR_AUTO_BACKGROUND_INDEX,
            FULL_COLOR_CUSTOM_BACKGROUND_INDEX -> preset.fullColorBackgroundColorIndex
            else -> preset.fullColorBackgroundColorIndex.coerceIn(0, FULL_COLOR_BACKGROUND_OPTIONS.lastIndex)
        }
        fullColorUseGradientBackground = preset.fullColorUseGradientBackground
        fullColorGradientPresetIndex = preset.fullColorGradientPresetIndex.coerceIn(-1, FULL_COLOR_GRADIENT_PRESETS.lastIndex)
        fullColorImageOnTop = preset.fullColorImageOnTop
        fullColorCustomBackgroundColor = normalizeFullColorOpaqueColor(preset.fullColorCustomBackgroundColor)
        frameSideTopBorderRatio = preset.frameSideTopBorderRatio.coerceIn(0f, 0.06f)
        frameSideTopBorderRatioDraft = frameSideTopBorderRatio
        frameLayoutMode = runCatching { WatermarkFrameLayoutMode.valueOf(preset.frameLayoutMode) }
            .getOrDefault(WatermarkFrameLayoutMode.Center)
        logoScaleFactor = preset.logoScaleFactor.coerceIn(0.72f, 1.85f)
        logoScaleFactorDraft = logoScaleFactor
        frameBackgroundColorIndex = preset.frameBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        thinBorderColorIndex = preset.thinBorderColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        roundedBorderColorIndex = preset.roundedBorderColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        borderCornerMode = runCatching { BorderCornerMode.valueOf(preset.borderCornerMode) }
            .getOrDefault(BorderCornerMode.Straight)
        blurImageCornerRadiusRatio = preset.blurImageCornerRadiusRatio.coerceIn(0f, 0.18f)
        blurImageCornerRadiusRatioDraft = blurImageCornerRadiusRatio
        borderImageScaleFactor = preset.borderImageScaleFactor.coerceIn(0.78f, 1f)
        borderImageScaleFactorDraft = borderImageScaleFactor
        cutoutOverlayStyle = runCatching {
            WatermarkCutoutOverlayStyle.valueOf(preset.cutoutOverlayStyle)
        }.getOrDefault(WatermarkCutoutOverlayStyle.None)
        if (cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None) {
            thinBorderColorIndex = resolveWatermarkCutoutBreakoutBackgroundColorIndex(thinBorderColorIndex)
        }
        cutoutSubjectScaleFactor = preset.cutoutSubjectScaleFactor.coerceIn(
            MIN_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
            MAX_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
        )
        cutoutSubjectScaleFactorDraft = cutoutSubjectScaleFactor
        cutoutFrameAspectRatio = runCatching {
            WatermarkCutoutFrameAspectRatio.valueOf(preset.cutoutFrameAspectRatio)
        }.getOrDefault(DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO)
        subjectOutlineEnabled = preset.subjectOutlineEnabled
        subjectOutlineLineStyle = runCatching {
            WatermarkSubjectOutlineLineStyle.valueOf(preset.subjectOutlineLineStyle)
        }.getOrDefault(WatermarkSubjectOutlineLineStyle.Solid)
        subjectOutlineWidthRatio = preset.subjectOutlineWidthRatio.coerceIn(
            MIN_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO,
            MAX_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
        )
        subjectOutlineWidthRatioDraft = subjectOutlineWidthRatio
        subjectOutlineColorIndex = preset.subjectOutlineColorIndex.coerceIn(
            0,
            FLOATING_COLOR_OPTIONS.lastIndex
        )
        subjectOutlineOuterBorderEnabled = preset.subjectOutlineOuterBorderEnabled
        subjectOutlineOffsetRatio = preset.subjectOutlineOffsetRatio.coerceIn(
            MIN_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO,
            MAX_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
        )
        subjectOutlineOffsetRatioDraft = subjectOutlineOffsetRatio
        subjectOutlineSymbolText = sanitizeWatermarkSubjectOutlineSymbolText(preset.subjectOutlineSymbolText)
        subjectOutlineSymbolSizeRatio = preset.subjectOutlineSymbolSizeRatio.coerceIn(
            MIN_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO,
            MAX_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
        )
        subjectOutlineSymbolSizeRatioDraft = subjectOutlineSymbolSizeRatio
        floatingBackgroundColorIndex = preset.floatingBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        floatingStrokeColorIndex = preset.floatingStrokeColorIndex.coerceIn(0, FLOATING_STROKE_OPTIONS.lastIndex)
        floatingImageCornerRadiusRatio = preset.floatingImageCornerRadiusRatio.coerceIn(0f, 0.12f)
        floatingImageCornerRadiusRatioDraft = floatingImageCornerRadiusRatio
        selectedFilmFrameIndex = preset.filmFrameIndex.coerceIn(0, FILM_FRAME_RES_IDS.lastIndex)
        selectedPolaroidFrameIndex = preset.polaroidFrameIndex.coerceIn(0, POLAROID_FRAME_RES_IDS.lastIndex)
        selectedColorPaletteStyleIndex = preset.colorPaletteStyleIndex.coerceIn(0, WatermarkColorPaletteStyle.entries.lastIndex)
        val colorPaletteStyle = resolveWatermarkColorPaletteStyle(selectedColorPaletteStyleIndex)
        val colorPaletteVariantOptions = resolveWatermarkPanelColorPaletteVariantOptions(colorPaletteStyle, appLanguage)
        selectedColorPaletteVariantIndex = if (colorPaletteVariantOptions.isEmpty()) {
            preset.colorPaletteVariantIndex.coerceAtLeast(0)
        } else {
            preset.colorPaletteVariantIndex.coerceIn(0, colorPaletteVariantOptions.maxOf { it.index })
        }
        colorPaletteHorizontalOffsetRatio = preset.colorPaletteHorizontalOffsetRatio.coerceIn(-1f, 1f)
        colorPaletteHorizontalOffsetRatioDraft = colorPaletteHorizontalOffsetRatio
        colorPaletteVerticalOffsetRatio = preset.colorPaletteVerticalOffsetRatio.coerceIn(-1f, 1f)
        colorPaletteVerticalOffsetRatioDraft = colorPaletteVerticalOffsetRatio
        colorPaletteElementCount = normalizeWatermarkPanelColorPaletteItemCount(
            style = colorPaletteStyle,
            variantIndex = selectedColorPaletteVariantIndex,
            rawCount = preset.colorPaletteElementCount
        )
        colorPaletteElementCountDraft = colorPaletteElementCount.toFloat()
        colorPaletteShowHexValue = preset.colorPaletteShowHexValue
        if (colorPaletteStyle == WatermarkColorPaletteStyle.GlassDots) {
            selectedColorPaletteVariantIndex = resolveEffectiveWatermarkGlassDotsVariantIndex(
                variantIndex = selectedColorPaletteVariantIndex,
                itemCount = colorPaletteElementCount,
                showHexValue = colorPaletteShowHexValue
            )
        }
        colorPaletteShowRgbValue = preset.colorPaletteShowRgbValue
        paperDotsLayoutDirection = runCatching {
            WatermarkPaperDotsLayoutDirection.valueOf(preset.paperDotsLayoutDirection)
        }.getOrDefault(WatermarkPaperDotsLayoutDirection.ImageTop)
        paperDotsSplitRatio = preset.paperDotsSplitRatio.coerceIn(0f, 1f)
        paperDotsSplitRatioDraft = paperDotsSplitRatio
        paperDotsBackgroundMode = runCatching {
            WatermarkPaperDotsBackgroundMode.valueOf(preset.paperDotsBackgroundMode)
        }.getOrDefault(WatermarkPaperDotsBackgroundMode.Solid)
        paperDotsBackgroundColorIndex = preset.paperDotsBackgroundColorIndex
            .coerceIn(0, WATERMARK_PAPER_DOTS_COLOR_OPTIONS.lastIndex)
        paperDotsBackgroundSecondaryColorIndex = preset.paperDotsBackgroundSecondaryColorIndex
            .coerceIn(0, WATERMARK_PAPER_DOTS_COLOR_OPTIONS.lastIndex)
        paperDotsStripeSizeRatio = preset.paperDotsStripeSizeRatio.coerceIn(0.015f, 0.12f)
        paperDotsStripeSizeRatioDraft = paperDotsStripeSizeRatio
        paperDotsShape = runCatching {
            WatermarkPaperDotsDotShape.valueOf(preset.paperDotsShape)
        }.getOrDefault(WatermarkPaperDotsDotShape.Circle)
        paperDotsDotSize = preset.paperDotsDotSize.coerceIn(12f, 72f)
        paperDotsDotSizeDraft = paperDotsDotSize
        paperDotsDotVariance = preset.paperDotsDotVariance.coerceIn(0f, 100f)
        paperDotsDotVarianceDraft = paperDotsDotVariance
        paperDotsDotCount = preset.paperDotsDotCount.coerceIn(6, 60)
        paperDotsDotCountDraft = paperDotsDotCount.toFloat()
        paperDotsRandomSeed = preset.paperDotsRandomSeed
        paperDotsStrokeMode = runCatching {
            WatermarkPaperDotsStrokeMode.valueOf(preset.paperDotsStrokeMode)
        }.getOrDefault(WatermarkPaperDotsStrokeMode.AllFill)
        paperDotsStrokeWidthRatio = preset.paperDotsStrokeWidthRatio.coerceIn(0.02f, 0.25f)
        paperDotsStrokeWidthRatioDraft = paperDotsStrokeWidthRatio
        paperDotsStrokeMixRatio = preset.paperDotsStrokeMixRatio.coerceIn(0f, 1f)
        paperDotsStrokeMixRatioDraft = paperDotsStrokeMixRatio
        paperDotsPatternText = sanitizeWatermarkPaperDotsPatternText(preset.paperDotsPatternText)
        signatureText = sanitizeEnglishSignature(preset.signatureText)
        signatureColorIndex = preset.signatureColorIndex.coerceIn(0, SIGNATURE_COLOR_OPTIONS.lastIndex)
        signatureHorizontalOffsetRatio = preset.signatureHorizontalOffsetRatio.coerceIn(-1f, 1f)
        signatureHorizontalOffsetRatioDraft = signatureHorizontalOffsetRatio
        signatureVerticalOffsetRatio = preset.signatureVerticalOffsetRatio.coerceIn(-1f, 1f)
        signatureVerticalOffsetRatioDraft = signatureVerticalOffsetRatio
        signatureScaleFactor = preset.signatureScaleFactor.coerceIn(0.65f, 1.8f)
        signatureScaleFactorDraft = signatureScaleFactor
        watermarkFontKey = normalizeWatermarkFontKey(preset.watermarkFontKey)
        signatureFontKey = normalizeSignatureFontKey(preset.signatureFontKey)
        signatureEnabled = preset.signatureEnabled && signatureText.isNotBlank()
        if (signatureEnabled) {
            applySignatureTakeover()
        }
        if (selectedBrandKey == null || selectedBrandKey == CUSTOM_LOGO_BRAND_KEY) {
            applyPreferredVariantForBrand(resolveEffectiveBrandKey())
        } else {
            val variantCount = BRAND_LOGO_GROUP_MAP[selectedBrandKey]?.variants?.size ?: 0
            selectedVariantIndex = if (variantCount <= 0) 0 else preset.selectedVariantIndex.coerceIn(0, variantCount - 1)
        }
        prefs.edit()
            .putString("watermark_signature_text", signatureText)
            .putBoolean("watermark_signature_enabled", signatureEnabled)
            .putFloat(WATERMARK_PREF_SIGNATURE_OFFSET_X, signatureHorizontalOffsetRatio)
            .putFloat(WATERMARK_PREF_SIGNATURE_OFFSET_Y, signatureVerticalOffsetRatio)
            .putFloat(WATERMARK_PREF_SIGNATURE_SCALE, signatureScaleFactor)
            .putString(WATERMARK_PREF_TEXT_FONT_KEY, watermarkFontKey)
            .putString(WATERMARK_PREF_SIGNATURE_FONT_KEY, signatureFontKey)
            .apply()
    }
    val applyModernPresetSpec: (WatermarkModernPresetSpec) -> Unit = fun(preset: WatermarkModernPresetSpec) {
        selectedPresetId = null
        selectedTemplate = preset.template
        selectedBrandKey = null
        selectedVariantIndex = 0
        sessionManualBrandKeys = emptyList()
        showLogoWatermark = preset.showLogoWatermark
        showModelWatermark = preset.showModelWatermark
        showDateWatermark = preset.showDateWatermark
        showExifWatermark = preset.showExifWatermark
        showLocationWatermark = preset.showLocationWatermark
        showCityLocationWatermark = preset.showCityLocationWatermark
        showFullColorNoteWatermark = false
        if (showLocationWatermark && showCityLocationWatermark) {
            showLocationWatermark = false
        }
        customModelLabel = ""
        customDateLabel = ""
        customLocationLabel = ""
        customFullColorNoteText = ""
        fullColorTextScaleFactor = DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR
        fullColorTextScaleFactorDraft = DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR
        fullColorHorizontalOffsetRatio = 0f
        fullColorVerticalOffsetRatio = 0f
        fullColorBackgroundColorIndex = FULL_COLOR_AUTO_BACKGROUND_INDEX
        fullColorUseGradientBackground = preset.fullColorUseGradientBackground
        fullColorGradientPresetIndex = preset.fullColorGradientPresetIndex.coerceIn(-1, FULL_COLOR_GRADIENT_PRESETS.lastIndex)
        fullColorImageOnTop = false
        fullColorCustomBackgroundColor = DEFAULT_FULL_COLOR_CUSTOM_BACKGROUND_COLOR
        frameSideTopBorderRatio = preset.frameSideTopBorderRatio.coerceIn(0f, 0.06f)
        frameSideTopBorderRatioDraft = frameSideTopBorderRatio
        frameLayoutMode = preset.frameLayoutMode
        logoScaleFactor = preset.logoScaleFactor.coerceIn(0.72f, 1.85f)
        logoScaleFactorDraft = logoScaleFactor
        frameBackgroundColorIndex = preset.frameBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        thinBorderColorIndex = preset.thinBorderColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        roundedBorderColorIndex = preset.roundedBorderColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        borderCornerMode = preset.borderCornerMode
        blurImageCornerRadiusRatio = preset.blurImageCornerRadiusRatio.coerceIn(0f, 0.18f)
        blurImageCornerRadiusRatioDraft = blurImageCornerRadiusRatio
        borderImageScaleFactor = preset.borderImageScaleFactor.coerceIn(0.78f, 1f)
        borderImageScaleFactorDraft = borderImageScaleFactor
        cutoutOverlayStyle = preset.cutoutOverlayStyle
        if (cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None) {
            thinBorderColorIndex = resolveWatermarkCutoutBreakoutBackgroundColorIndex(thinBorderColorIndex)
        }
        cutoutSubjectScaleFactor = preset.cutoutSubjectScaleFactor.coerceIn(
            MIN_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
            MAX_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
        )
        cutoutSubjectScaleFactorDraft = cutoutSubjectScaleFactor
        cutoutFrameAspectRatio = preset.cutoutFrameAspectRatio
        val subjectOutlineConfig = preset.subjectOutlineConfig.normalized()
        subjectOutlineEnabled = subjectOutlineConfig.enabled
        subjectOutlineLineStyle = subjectOutlineConfig.lineStyle
        subjectOutlineWidthRatio = subjectOutlineConfig.widthRatio
        subjectOutlineWidthRatioDraft = subjectOutlineWidthRatio
        subjectOutlineColorIndex = subjectOutlineConfig.colorIndex.coerceIn(
            0,
            FLOATING_COLOR_OPTIONS.lastIndex
        )
        subjectOutlineOuterBorderEnabled = subjectOutlineConfig.outerBorderEnabled
        subjectOutlineOffsetRatio = subjectOutlineConfig.offsetRatio
        subjectOutlineOffsetRatioDraft = subjectOutlineOffsetRatio
        subjectOutlineSymbolText = sanitizeWatermarkSubjectOutlineSymbolText(subjectOutlineConfig.symbolText)
        subjectOutlineSymbolSizeRatio = subjectOutlineConfig.symbolSizeRatio
        subjectOutlineSymbolSizeRatioDraft = subjectOutlineSymbolSizeRatio
        floatingBackgroundColorIndex = preset.floatingBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        floatingStrokeColorIndex = preset.floatingStrokeColorIndex.coerceIn(0, FLOATING_STROKE_OPTIONS.lastIndex)
        floatingImageCornerRadiusRatio = preset.floatingImageCornerRadiusRatio.coerceIn(0f, 0.12f)
        floatingImageCornerRadiusRatioDraft = floatingImageCornerRadiusRatio
        selectedFilmFrameIndex = preset.filmFrameIndex.coerceIn(0, FILM_FRAME_RES_IDS.lastIndex)
        selectedPolaroidFrameIndex = preset.polaroidFrameIndex.coerceIn(0, POLAROID_FRAME_RES_IDS.lastIndex)
        selectedColorPaletteStyleIndex = preset.colorPaletteStyleIndex.coerceIn(0, WatermarkColorPaletteStyle.entries.lastIndex)
        val colorPaletteStyle = resolveWatermarkColorPaletteStyle(selectedColorPaletteStyleIndex)
        val colorPaletteVariantOptions = resolveWatermarkPanelColorPaletteVariantOptions(colorPaletteStyle, appLanguage)
        selectedColorPaletteVariantIndex = if (colorPaletteVariantOptions.isEmpty()) {
            preset.colorPaletteVariantIndex.coerceAtLeast(0)
        } else {
            preset.colorPaletteVariantIndex.coerceIn(0, colorPaletteVariantOptions.maxOf { it.index })
        }
        colorPaletteHorizontalOffsetRatio = preset.colorPaletteHorizontalOffsetRatio.coerceIn(-1f, 1f)
        colorPaletteHorizontalOffsetRatioDraft = colorPaletteHorizontalOffsetRatio
        colorPaletteVerticalOffsetRatio = preset.colorPaletteVerticalOffsetRatio.coerceIn(-1f, 1f)
        colorPaletteVerticalOffsetRatioDraft = colorPaletteVerticalOffsetRatio
        colorPaletteElementCount = normalizeWatermarkPanelColorPaletteItemCount(
            style = colorPaletteStyle,
            variantIndex = selectedColorPaletteVariantIndex,
            rawCount = preset.colorPaletteElementCount
        )
        colorPaletteElementCountDraft = colorPaletteElementCount.toFloat()
        colorPaletteShowHexValue = preset.colorPaletteShowHexValue
        if (colorPaletteStyle == WatermarkColorPaletteStyle.GlassDots) {
            selectedColorPaletteVariantIndex = resolveEffectiveWatermarkGlassDotsVariantIndex(
                variantIndex = selectedColorPaletteVariantIndex,
                itemCount = colorPaletteElementCount,
                showHexValue = colorPaletteShowHexValue
            )
        }
        colorPaletteShowRgbValue = preset.colorPaletteShowRgbValue
        paperDotsLayoutDirection = preset.paperDotsConfig.layoutDirection
        paperDotsSplitRatio = preset.paperDotsConfig.splitRatio.coerceIn(0f, 1f)
        paperDotsSplitRatioDraft = paperDotsSplitRatio
        paperDotsBackgroundMode = preset.paperDotsConfig.backgroundMode
        paperDotsBackgroundColorIndex = preset.paperDotsConfig.backgroundColorIndex
            .coerceIn(0, WATERMARK_PAPER_DOTS_COLOR_OPTIONS.lastIndex)
        paperDotsBackgroundSecondaryColorIndex = preset.paperDotsConfig.backgroundSecondaryColorIndex
            .coerceIn(0, WATERMARK_PAPER_DOTS_COLOR_OPTIONS.lastIndex)
        paperDotsStripeSizeRatio = preset.paperDotsConfig.stripeSizeRatio.coerceIn(0.015f, 0.12f)
        paperDotsStripeSizeRatioDraft = paperDotsStripeSizeRatio
        paperDotsShape = preset.paperDotsConfig.dotShape
        paperDotsDotSize = preset.paperDotsConfig.dotSize.coerceIn(12f, 72f)
        paperDotsDotSizeDraft = paperDotsDotSize
        paperDotsDotVariance = preset.paperDotsConfig.dotVariance.coerceIn(0f, 100f)
        paperDotsDotVarianceDraft = paperDotsDotVariance
        paperDotsDotCount = preset.paperDotsConfig.dotCount.coerceIn(6, 60)
        paperDotsDotCountDraft = paperDotsDotCount.toFloat()
        paperDotsRandomSeed = preset.paperDotsConfig.randomSeed
        paperDotsStrokeMode = preset.paperDotsConfig.strokeMode
        paperDotsStrokeWidthRatio = preset.paperDotsConfig.strokeWidthRatio.coerceIn(0.02f, 0.25f)
        paperDotsStrokeWidthRatioDraft = paperDotsStrokeWidthRatio
        paperDotsStrokeMixRatio = preset.paperDotsConfig.strokeMixRatio.coerceIn(0f, 1f)
        paperDotsStrokeMixRatioDraft = paperDotsStrokeMixRatio
        paperDotsPatternText = sanitizeWatermarkPaperDotsPatternText(preset.paperDotsConfig.patternText)
        signatureText = sanitizeEnglishSignature(preset.signatureText)
        signatureColorIndex = 0
        signatureHorizontalOffsetRatio = preset.signatureHorizontalOffsetRatio.coerceIn(-1f, 1f)
        signatureHorizontalOffsetRatioDraft = signatureHorizontalOffsetRatio
        signatureVerticalOffsetRatio = preset.signatureVerticalOffsetRatio.coerceIn(-1f, 1f)
        signatureVerticalOffsetRatioDraft = signatureVerticalOffsetRatio
        signatureScaleFactor = preset.signatureScaleFactor.coerceIn(0.65f, 1.8f)
        signatureScaleFactorDraft = signatureScaleFactor
        watermarkFontKey = normalizeWatermarkFontKey(preset.watermarkFontKey)
        signatureFontKey = normalizeSignatureFontKey(preset.signatureFontKey)
        signatureEnabled = signatureText.isNotBlank()
        if (signatureEnabled) {
            applySignatureTakeover()
        }
        selectedSocialCropPresetKey = null
        cropEditorPresetKey = null
        colorPaletteRefreshNonce = 0
        customizePanelKey = preferredWatermarkCustomizePanelForCurrentState(
            template = preset.template,
            cutoutOverlayStyle = preset.cutoutOverlayStyle,
            subjectOutlineEnabled = preset.subjectOutlineConfig.enabled
        ).name
        collapsedCustomizePanel = false
    }
    val saveCurrentPreset: (String?) -> String = { rawName ->
        val fallbackName = resolveNextPresetName()
        val finalName = rawName?.trim().orEmpty().ifBlank { fallbackName }
        val preset = WatermarkUserPreset(
            id = UUID.randomUUID().toString(),
            name = finalName,
            templateName = selectedTemplate.name,
            selectedBrandKey = selectedBrandKey,
            selectedVariantIndex = selectedVariantIndex,
            showLogoWatermark = showLogoWatermark,
            showModelWatermark = showModelWatermark,
            showDateWatermark = effectiveFullColorShowDate,
            showExifWatermark = showExifWatermark,
            showLocationWatermark = showLocationWatermark,
            showCityLocationWatermark = effectiveFullColorShowCityLocation,
            customModelLabel = customModelLabel.trim(),
            customDateLabel = customDateLabel.trim(),
            customLocationLabel = customLocationLabel.trim(),
            fullColorTextScaleFactor = fullColorTextScaleFactor.coerceIn(
                FULL_COLOR_TEXT_SCALE_MIN,
                FULL_COLOR_TEXT_SCALE_MAX
            ),
            fullColorBackgroundColorIndex = fullColorBackgroundColorIndex,
            fullColorCustomBackgroundColor = normalizeFullColorOpaqueColor(fullColorCustomBackgroundColor),
            fullColorUseGradientBackground = fullColorUseGradientBackground,
            fullColorGradientPresetIndex = fullColorGradientPresetIndex,
            fullColorImageOnTop = fullColorImageOnTop,
            frameSideTopBorderRatio = frameSideTopBorderRatio,
            frameLayoutMode = frameLayoutMode.name,
            logoScaleFactor = logoScaleFactor,
            frameBackgroundColorIndex = frameBackgroundColorIndex,
            thinBorderColorIndex = thinBorderColorIndex,
            roundedBorderColorIndex = roundedBorderColorIndex,
            borderCornerMode = borderCornerMode.name,
            blurImageCornerRadiusRatio = blurImageCornerRadiusRatio,
            borderImageScaleFactor = borderImageScaleFactor,
            cutoutOverlayStyle = cutoutOverlayStyle.name,
            cutoutSubjectScaleFactor = cutoutSubjectScaleFactor,
            cutoutFrameAspectRatio = cutoutFrameAspectRatio.name,
            subjectOutlineEnabled = subjectOutlineEnabled,
            subjectOutlineLineStyle = subjectOutlineLineStyle.name,
            subjectOutlineWidthRatio = subjectOutlineWidthRatio,
            subjectOutlineColorIndex = subjectOutlineColorIndex,
            subjectOutlineOuterBorderEnabled = subjectOutlineOuterBorderEnabled,
            subjectOutlineOffsetRatio = subjectOutlineOffsetRatio,
            subjectOutlineSymbolText = sanitizeWatermarkSubjectOutlineSymbolText(subjectOutlineSymbolText),
            subjectOutlineSymbolSizeRatio = subjectOutlineSymbolSizeRatio,
            floatingBackgroundColorIndex = floatingBackgroundColorIndex,
            floatingStrokeColorIndex = floatingStrokeColorIndex,
            floatingImageCornerRadiusRatio = floatingImageCornerRadiusRatio,
            filmFrameIndex = selectedFilmFrameIndex,
            polaroidFrameIndex = selectedPolaroidFrameIndex,
            colorPaletteStyleIndex = selectedColorPaletteStyleIndex,
            colorPaletteVariantIndex = selectedColorPaletteVariantIndex,
            colorPaletteHorizontalOffsetRatio = colorPaletteHorizontalOffsetRatio,
            colorPaletteVerticalOffsetRatio = colorPaletteVerticalOffsetRatio,
            colorPaletteElementCount = colorPaletteElementCount,
            colorPaletteShowHexValue = colorPaletteShowHexValue,
            colorPaletteShowRgbValue = colorPaletteShowRgbValue,
            paperDotsLayoutDirection = paperDotsLayoutDirection.name,
            paperDotsSplitRatio = paperDotsSplitRatio,
            paperDotsBackgroundMode = paperDotsBackgroundMode.name,
            paperDotsBackgroundColorIndex = paperDotsBackgroundColorIndex,
            paperDotsBackgroundSecondaryColorIndex = paperDotsBackgroundSecondaryColorIndex,
            paperDotsStripeSizeRatio = paperDotsStripeSizeRatio,
            paperDotsShape = paperDotsShape.name,
            paperDotsDotSize = paperDotsDotSize,
            paperDotsDotVariance = paperDotsDotVariance,
            paperDotsDotCount = paperDotsDotCount,
            paperDotsRandomSeed = paperDotsRandomSeed,
            paperDotsStrokeMode = paperDotsStrokeMode.name,
            paperDotsStrokeWidthRatio = paperDotsStrokeWidthRatio,
            paperDotsStrokeMixRatio = paperDotsStrokeMixRatio,
            paperDotsPatternText = sanitizeWatermarkPaperDotsPatternText(paperDotsPatternText),
            signatureEnabled = signatureEnabled,
            signatureText = sanitizeEnglishSignature(signatureText),
            signatureColorIndex = signatureColorIndex,
            signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
            signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
            signatureScaleFactor = signatureScaleFactor,
            watermarkFontKey = watermarkFontKey,
            signatureFontKey = signatureFontKey
        )
        userPresets.add(0, preset)
        while (userPresets.size > WATERMARK_USER_PRESET_LIMIT) {
            userPresets.removeAt(userPresets.lastIndex)
        }
        persistUserPresets()
        finalName
    }
    val applySignatureSelection: (String) -> Unit = { raw ->
        val sanitized = sanitizeEnglishSignature(raw)
        if (sanitized.isNotBlank()) {
            signatureText = sanitized
            signatureEnabled = true
            applySignatureTakeover()
            val existingIndex = signatureHistory.indexOf(sanitized)
            if (existingIndex >= 0) {
                signatureHistory.removeAt(existingIndex)
            }
            signatureHistory.add(0, sanitized)
            prefs.edit()
                .putString("watermark_signature_text", sanitized)
                .putBoolean("watermark_signature_enabled", true)
                .apply()
            persistSignatureHistory()
        }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                trimWatermarkTempFiles(
                    cacheRoot = context.cacheDir,
                    protectedPaths = emptySet()
                )
            }
        }
        val sanitized = sanitizeEnglishSignature(signatureText)
        if (sanitized != signatureText) {
            signatureText = sanitized
            prefs.edit().putString("watermark_signature_text", sanitized).apply()
        }
        if (sanitized.isNotBlank() && !signatureHistory.contains(sanitized)) {
            signatureHistory.add(0, sanitized)
            persistSignatureHistory()
        }
        if (signatureEnabled) {
            applySignatureTakeover()
        }
    }
    LaunchedEffect(pendingPreviewScrollRestore) {
        val target = pendingPreviewScrollRestore ?: return@LaunchedEffect
        pendingPreviewScrollRestore = null
        if (!previewScrollState.isScrollInProgress) {
            runCatching { previewScrollState.scrollTo(target) }
        }
        if (target == 0) {
            shouldResetPreviewScrollOnNextRender = false
        }
    }
    LaunchedEffect(currentStep, selectedTemplate) {
        val enteringPreview = previousFlowStep != WatermarkFlowStep.Preview &&
            currentStep == WatermarkFlowStep.Preview
        if (enteringPreview && selectedTemplate == WatermarkTemplatePreset.ColorPalette) {
            shouldResetPreviewScrollOnNextRender = true
            pendingPreviewScrollRestore = 0
        }
        previousFlowStep = currentStep
    }
    LaunchedEffect(selectedColorPaletteStyleIndex, selectedColorPaletteVariantIndex) {
        if (selectedColorPaletteMinimumCount > 0 && colorPaletteElementCount < selectedColorPaletteMinimumCount) {
            colorPaletteElementCount = selectedColorPaletteMinimumCount
            colorPaletteElementCountDraft = selectedColorPaletteMinimumCount.toFloat()
        }
    }
    val toggleLikedTemplate: (String) -> Unit = { templateKey ->
        val next = if (likedTemplateKeys.contains(templateKey)) {
            likedTemplateKeys - templateKey
        } else {
            likedTemplateKeys + templateKey
        }
        likedTemplateKeys = next
        prefs.edit().putStringSet("watermark_liked_template_keys", next).apply()
    }
    suspend fun renderPreviewBitmapForCurrentState(
        sourceUri: Uri,
        exifInfoOverride: WatermarkExifInfo?,
        previewSizeOverride: Pair<Int, Int>? = previewOutputSizeOverride,
        previewMaxSideOverride: Int? = null
    ): Bitmap {
        val effectiveExifInfoOverride = applyFullColorExifOverrides(exifInfoOverride)
        val effectiveShowDate = if (selectedTemplate == WatermarkTemplatePreset.FullColor) {
            showDateWatermark && !showFullColorNoteWatermark && customDateLabel.trim().isNotBlank()
        } else {
            showDateWatermark
        }
        val effectiveShowCityLocation = if (selectedTemplate == WatermarkTemplatePreset.FullColor) {
            showCityLocationWatermark && !showFullColorNoteWatermark && customLocationLabel.trim().isNotBlank()
        } else {
            showCityLocationWatermark
        }
        val effectiveNoteText = if (
            selectedTemplate == WatermarkTemplatePreset.FullColor &&
            showFullColorNoteWatermark
        ) {
            customFullColorNoteText.trim()
        } else {
            ""
        }
        val previewMaxSidePx = previewMaxSideOverride ?: resolveWatermarkLivePreviewMaxSide(selectedTemplate)
        val previewSourceCacheKey = "preview-source:${sourceUri}#$previewMaxSidePx"
        val cachedPreviewSourceBitmap = previewSourceBitmap
            ?.takeIf { previewSourceBitmapKey == previewSourceCacheKey && !it.isRecycled }
            ?: withContext(Dispatchers.Default) {
                runCatching {
                    decodeSourceBitmap(
                        context = context,
                        uri = sourceUri,
                        requestedMaxSidePx = previewMaxSidePx
                    )
                }.getOrNull()
            }?.let { decodedPreviewSource ->
                val stillCurrentImage = selectedImageUri == sourceUri.toString()
                if (stillCurrentImage && !decodedPreviewSource.isRecycled) {
                    replacePreviewSourceBitmap(previewSourceCacheKey, decodedPreviewSource)
                    decodedPreviewSource
                } else {
                    recycleBitmapQuietly(decodedPreviewSource)
                    null
                }
            }
        val shouldUseSpecialModeForeground =
            selectedTemplate == WatermarkTemplatePreset.ThinWhiteBorder &&
                (
                    cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None ||
                        subjectOutlineEnabled
                    )
        val cutoutForegroundOverride = if (shouldUseSpecialModeForeground) {
            val cacheKey = "preview-cutout:${sourceUri}#$previewMaxSidePx"
            val cachedForeground = previewSpecialModeForegroundBitmap
                ?.takeIf { previewSpecialModeForegroundKey == cacheKey && !it.isRecycled }
            cachedForeground ?: cachedPreviewSourceBitmap?.let { previewSource ->
                withContext(Dispatchers.Default) {
                    runCatching {
                        generateStampAutoCutoutForegroundBitmap(
                            context = context,
                            source = previewSource
                        )
                    }.getOrNull()
                }?.let { generatedForeground ->
                    val stillCurrentImage = selectedImageUri == sourceUri.toString()
                    if (stillCurrentImage && !generatedForeground.isRecycled) {
                        replacePreviewSpecialModeForegroundBitmap(cacheKey, generatedForeground)
                        generatedForeground
                    } else {
                        recycleBitmapQuietly(generatedForeground)
                        null
                    }
                }
            }
        } else {
            null
        }
        return renderWatermarkPreviewBitmap(
            context = context,
            sourceUri = sourceUri,
            sourceBitmapOverride = cachedPreviewSourceBitmap,
            template = selectedTemplate,
            selectedBrandKey = resolveEffectiveBrandKey(),
            customLogoPath = customLogoPath.takeIf { it.isNotBlank() },
            selectedVariantIndex = selectedVariantIndex,
            showLogoWatermark = showLogoWatermark,
            showModelWatermark = showModelWatermark,
            showDateWatermark = effectiveShowDate,
            showExifWatermark = showExifWatermark,
            showLocationWatermark = showLocationWatermark,
            showCityLocationWatermark = effectiveShowCityLocation,
            fullColorNoteText = effectiveNoteText,
            modelLineOverride = effectiveModelLabel,
            frameSideTopBorderRatio = frameSideTopBorderRatio,
            frameLayoutMode = frameLayoutMode,
            logoScaleFactor = logoScaleFactor,
            frameBackgroundColorIndex = frameBackgroundColorIndex,
            thinBorderColorIndex = thinBorderColorIndex,
            roundedBorderColorIndex = roundedBorderColorIndex,
            borderCornerMode = borderCornerMode,
            blurImageCornerRadiusRatio = blurImageCornerRadiusRatio,
            borderImageScaleFactor = borderImageScaleFactor,
            cutoutOverlayStyle = cutoutOverlayStyle,
            cutoutSubjectScaleFactor = cutoutSubjectScaleFactor,
            cutoutFrameAspectRatio = cutoutFrameAspectRatio,
            subjectOutlineConfig = resolveCurrentSubjectOutlineConfig(),
            floatingBackgroundColorIndex = floatingBackgroundColorIndex,
            floatingStrokeColorIndex = floatingStrokeColorIndex,
            floatingImageCornerRadiusRatio = floatingImageCornerRadiusRatio,
            filmFrameIndex = selectedFilmFrameIndex,
            polaroidFrameIndex = selectedPolaroidFrameIndex,
            colorPaletteStyleIndex = selectedColorPaletteStyleIndex,
            colorPaletteVariantIndex = selectedColorPaletteVariantIndex,
            colorPaletteRefreshNonce = colorPaletteRefreshNonce,
            colorPaletteHorizontalOffsetRatio = colorPaletteHorizontalOffsetRatio,
            colorPaletteVerticalOffsetRatio = colorPaletteVerticalOffsetRatio,
            colorPaletteElementCount = colorPaletteElementCount,
            colorPaletteShowHexValue = colorPaletteShowHexValue,
            colorPaletteShowRgbValue = colorPaletteShowRgbValue,
            paperDotsConfig = resolveCurrentPaperDotsConfig(),
            watermarkFontKey = watermarkFontKey,
            signatureFontKey = signatureFontKey,
            signatureText = if (signatureEnabled) signatureText else "",
            signatureColor = if (signatureEnabled) resolveSignatureColorOverride() else null,
            signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
            signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
            signatureScaleFactor = signatureScaleFactor,
            fullColorTextScaleFactor = fullColorTextScaleFactor,
            fullColorBackgroundColorOverride = resolveFullColorBackgroundColorOverride(),
            fullColorUseGradientBackground = fullColorUseGradientBackground,
            fullColorGradientColorsOverride = resolveFullColorGradientColorsOverride(fullColorGradientPresetIndex),
            fullColorImageOnTop = fullColorImageOnTop,
            fullColorHorizontalOffsetRatio = fullColorHorizontalOffsetRatio,
            fullColorVerticalOffsetRatio = fullColorVerticalOffsetRatio,
            outputSizeOverride = previewSizeOverride,
            exifInfoOverride = effectiveExifInfoOverride,
            previewMaxSidePx = previewMaxSidePx,
            cutoutForegroundOverride = cutoutForegroundOverride,
            preserveSourceBitmapOverride = cachedPreviewSourceBitmap != null,
            enableColorPaletteCache = true
        )
    }
    suspend fun renderOutputFileForCurrentState(
        sourceUri: Uri,
        exifInfoOverride: WatermarkExifInfo?,
        requestedOutputSizeOverride: Pair<Int, Int>? = outputSizeOverride
    ): WatermarkRenderOutput {
        val effectiveExifInfoOverride = applyFullColorExifOverrides(exifInfoOverride)
        val effectiveShowDate = if (selectedTemplate == WatermarkTemplatePreset.FullColor) {
            showDateWatermark && !showFullColorNoteWatermark && customDateLabel.trim().isNotBlank()
        } else {
            showDateWatermark
        }
        val effectiveShowCityLocation = if (selectedTemplate == WatermarkTemplatePreset.FullColor) {
            showCityLocationWatermark && !showFullColorNoteWatermark && customLocationLabel.trim().isNotBlank()
        } else {
            showCityLocationWatermark
        }
        val effectiveNoteText = if (
            selectedTemplate == WatermarkTemplatePreset.FullColor &&
            showFullColorNoteWatermark
        ) {
            customFullColorNoteText.trim()
        } else {
            ""
        }
        return renderWatermarkImageToCache(
            context = context,
            sourceUri = sourceUri,
            template = selectedTemplate,
            selectedBrandKey = resolveEffectiveBrandKey(),
            customLogoPath = customLogoPath.takeIf { it.isNotBlank() },
            selectedVariantIndex = selectedVariantIndex,
            showLogoWatermark = showLogoWatermark,
            showModelWatermark = showModelWatermark,
            showDateWatermark = effectiveShowDate,
            showExifWatermark = showExifWatermark,
            showLocationWatermark = showLocationWatermark,
            showCityLocationWatermark = effectiveShowCityLocation,
            fullColorNoteText = effectiveNoteText,
            modelLineOverride = effectiveModelLabel,
            frameSideTopBorderRatio = frameSideTopBorderRatio,
            frameLayoutMode = frameLayoutMode,
            logoScaleFactor = logoScaleFactor,
            frameBackgroundColorIndex = frameBackgroundColorIndex,
            thinBorderColorIndex = thinBorderColorIndex,
            roundedBorderColorIndex = roundedBorderColorIndex,
            borderCornerMode = borderCornerMode,
            blurImageCornerRadiusRatio = blurImageCornerRadiusRatio,
            borderImageScaleFactor = borderImageScaleFactor,
            cutoutOverlayStyle = cutoutOverlayStyle,
            cutoutSubjectScaleFactor = cutoutSubjectScaleFactor,
            cutoutFrameAspectRatio = cutoutFrameAspectRatio,
            subjectOutlineConfig = resolveCurrentSubjectOutlineConfig(),
            floatingBackgroundColorIndex = floatingBackgroundColorIndex,
            floatingStrokeColorIndex = floatingStrokeColorIndex,
            floatingImageCornerRadiusRatio = floatingImageCornerRadiusRatio,
            filmFrameIndex = selectedFilmFrameIndex,
            polaroidFrameIndex = selectedPolaroidFrameIndex,
            colorPaletteStyleIndex = selectedColorPaletteStyleIndex,
            colorPaletteVariantIndex = selectedColorPaletteVariantIndex,
            colorPaletteHorizontalOffsetRatio = colorPaletteHorizontalOffsetRatio,
            colorPaletteVerticalOffsetRatio = colorPaletteVerticalOffsetRatio,
            colorPaletteElementCount = colorPaletteElementCount,
            colorPaletteShowHexValue = colorPaletteShowHexValue,
            colorPaletteShowRgbValue = colorPaletteShowRgbValue,
            paperDotsConfig = resolveCurrentPaperDotsConfig(),
            watermarkFontKey = watermarkFontKey,
            signatureFontKey = signatureFontKey,
            signatureText = if (signatureEnabled) signatureText else "",
            signatureColor = if (signatureEnabled) resolveSignatureColorOverride() else null,
            signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
            signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
            signatureScaleFactor = signatureScaleFactor,
            fullColorTextScaleFactor = fullColorTextScaleFactor,
            fullColorBackgroundColorOverride = resolveFullColorBackgroundColorOverride(),
            fullColorUseGradientBackground = fullColorUseGradientBackground,
            fullColorGradientColorsOverride = resolveFullColorGradientColorsOverride(fullColorGradientPresetIndex),
            fullColorImageOnTop = fullColorImageOnTop,
            fullColorHorizontalOffsetRatio = fullColorHorizontalOffsetRatio,
            fullColorVerticalOffsetRatio = fullColorVerticalOffsetRatio,
            outputSizeOverride = requestedOutputSizeOverride,
            exifInfoOverride = effectiveExifInfoOverride
        )
    }
    val rerenderForCurrentSelectionWithPreviewSide: (Uri, String, Int?) -> Unit = { uri, fallbackError, previewMaxSideOverride ->
        val scrollBeforeRender = previewScrollState.value
        val requestId = beginRenderRequest()
        cancelPendingPreviewRender()
        previewRenderJob = scope.launch {
            val currentJob = currentCoroutineContext()[Job]
            isWorking = true
            isRenderingPreview = true
            errorMessage = null
            runCatching {
                renderPreviewBitmapForCurrentState(
                    sourceUri = uri,
                    exifInfoOverride = currentExifInfo,
                    previewMaxSideOverride = previewMaxSideOverride
                )
            }.onSuccess { previewBitmap ->
                if (requestId != renderRequestVersion) {
                    recycleBitmapQuietly(previewBitmap)
                    return@onSuccess
                }
                replaceRenderedPreviewBitmap(previewBitmap)
                clearRenderedFileCache()
                if (currentStep == WatermarkFlowStep.Preview) {
                    pendingPreviewScrollRestore = if (shouldResetPreviewScrollOnNextRender) {
                        0
                    } else {
                        scrollBeforeRender
                    }
                }
                trimWatermarkTempFiles(
                    cacheRoot = context.cacheDir,
                    protectedPaths = buildSet {
                        activeSelectedSourceFile?.let { add(it.absolutePath) }
                        activeCropSourceFile?.let { add(it.absolutePath) }
                        renderedFile?.let { add(it.absolutePath) }
                    }
                )
            }.onFailure { error ->
                if (error is CancellationException) {
                    return@onFailure
                }
                if (requestId == renderRequestVersion) {
                    errorMessage = error.message ?: fallbackError
                }
            }
            if (requestId == renderRequestVersion) {
                isWorking = false
                isRenderingPreview = false
            }
            if (previewRenderJob == currentJob) {
                previewRenderJob = null
            }
        }
    }
    val performSaveOutput: () -> Unit = saveAction@{
        if (isSavingOutput) return@saveAction
        val origin = selectedUri ?: return@saveAction
        renderedPreviewBitmap ?: return@saveAction
        scope.launch {
            isWorking = true
            isSavingOutput = true
            runCatching {
                val outputFile = renderedFile ?: renderOutputFileForCurrentState(
                    sourceUri = origin,
                    exifInfoOverride = currentExifInfo
                ).also {
                    clearRenderedFileCache()
                    renderedFile = it.outputFile
                    renderedMimeType = it.mimeType
                }.outputFile
                val payload = buildWatermarkExportPayload(
                    context = context,
                    renderedFile = outputFile,
                    renderedMimeType = renderedMimeType,
                    originalSourceUri = origin,
                    copyrightSettings = copyrightState.currentSettings()
                )
                saveWatermarkOutput(
                    context = context,
                    outputFile = payload.outputFile,
                    outputMimeType = payload.outputMimeType,
                    sourceUri = payload.exifSourceUri ?: selectedExifSourceUri ?: origin,
                    template = selectedTemplate,
                    skipExifCopy = payload.preserveEmbeddedMetadata
                )
            }.onSuccess { uri ->
                if (uri != null) {
                    toastMessage = "已保存到相册"
                } else {
                    errorMessage = "保存失败"
                }
            }.onFailure { error ->
                errorMessage = error.message ?: "保存失败"
            }
            isSavingOutput = false
            isWorking = false
        }
    }
    val rerenderForCurrentSelection: (Uri, String) -> Unit = { uri, fallbackError ->
        rerenderForCurrentSelectionWithPreviewSide(uri, fallbackError, null)
    }
    val performShareOutput: () -> Unit = shareAction@{
        val origin = selectedUri
        renderedPreviewBitmap ?: return@shareAction
        scope.launch {
            isWorking = true
            runCatching {
                val outputFile = renderedFile ?: renderOutputFileForCurrentState(
                    sourceUri = origin ?: return@runCatching,
                    exifInfoOverride = currentExifInfo
                ).also {
                    clearRenderedFileCache()
                    renderedFile = it.outputFile
                    renderedMimeType = it.mimeType
                }.outputFile
                val payload = buildWatermarkExportPayload(
                    context = context,
                    renderedFile = outputFile,
                    renderedMimeType = renderedMimeType,
                    originalSourceUri = origin,
                    copyrightSettings = copyrightState.currentSettings()
                )
                shareWatermarkOutput(context, payload.outputFile, payload.outputMimeType)
            }.onFailure { error ->
                errorMessage = error.message ?: "分享失败"
            }
            isWorking = false
        }
    }
    val exitCopyrightProfileToPreview: () -> Unit = {
        customizePanelKey = WatermarkCustomizePanel.Copyright.name
        currentStep = WatermarkFlowStep.Preview
        scope.launch { drawerState.close() }
    }
    val restoreCropPreviewPanel: () -> Unit = {
        customizePanelKey = WatermarkCustomizePanel.Crop.name
        collapsedCustomizePanel = false
    }
    val exitCropToPreview: () -> Unit = {
        cropEditorUndoHistory.clear()
        suppressPreviewDockInputUntil = SystemClock.elapsedRealtime() + 240L
        restoreCropPreviewPanel()
        currentStep = WatermarkFlowStep.Preview
        scope.launch {
            runCatching { drawerState.close() }
            delay(32)
            if (currentStep == WatermarkFlowStep.Preview) {
                restoreCropPreviewPanel()
                runCatching { drawerState.close() }
            }
        }
    }
    val handleInnerBack: () -> Unit = {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            currentStep == WatermarkFlowStep.Crop -> {
                exitCropToPreview()
            }
            currentStep == WatermarkFlowStep.CopyrightProfile -> {
                exitCopyrightProfileToPreview()
            }
            currentStep == WatermarkFlowStep.Template && selectedUri != null -> {
                currentStep = WatermarkFlowStep.Preview
            }
            else -> onBack()
        }
    }
    val customLogoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                persistCustomWatermarkLogo(context, uri)
            }.onSuccess { path ->
                customLogoPath = path
                prefs.edit().putString(WATERMARK_PREF_CUSTOM_LOGO_PATH, path).apply()
                selectedBrandKey = CUSTOM_LOGO_BRAND_KEY
                selectedUri?.let { rerenderForCurrentSelection(it, "更新自定义 Logo 失败") }
            }.onFailure { error ->
                errorMessage = error.message ?: "保存自定义 Logo 失败"
            }
        }
    }
    suspend fun importPickedImageAndRender(uri: Uri) {
        val requestId = beginRenderRequest()
        isWorking = true
        isRenderingPreview = true
        isImportingSourceImage = true
        errorMessage = null
        val startedFromTemplateStep = currentStep == WatermarkFlowStep.Template
        runCatching {
            val imported = importPickedImageToWorkingFile(context, uri)
            sessionManualBrandKeys = emptyList()
            if (selectedBrandKey != null && selectedBrandKey != "photoo" && selectedBrandKey != CUSTOM_LOGO_BRAND_KEY) {
                selectedBrandKey = null
            }
            activeSelectedSourceFile?.let { old ->
                if (old.absolutePath != imported.file.absolutePath) {
                    runCatching { old.delete() }
                }
            }
            activeCropSourceFile?.let { old ->
                runCatching { old.delete() }
            }
            activeSelectedSourceFile = imported.file
            activeCropSourceFile = null
            selectedSocialCropPresetKey = null
            selectedColorPaletteVariantIndex = 0
            customFullColorNoteText = ""
            showFullColorNoteWatermark = false
            selectedImageUri = imported.uri.toString()
            exifSourceUriForSave = uri.toString()
            // New image should start with copyright toggles off.
            copyrightState.updateWriteCopyrightInfo(false)
            copyrightState.updateNoAiTrainingDeclaration(false)
            copyrightState.updateInvisibleWatermark(false)
            if (startedFromTemplateStep) {
                customizePanelKey = preferredWatermarkCustomizePanelForCurrentState(
                    template = selectedTemplate,
                    cutoutOverlayStyle = cutoutOverlayStyle,
                    subjectOutlineEnabled = subjectOutlineEnabled
                ).name
                collapsedCustomizePanel = false
            }
            runCatching { drawerState.close() }
            currentStep = WatermarkFlowStep.Preview

            val exifInfo = readExifInfo(context, imported.uri)
            currentExifInfo = exifInfo
            detectedBrandKey = detectBrandKey(exifInfo.make, exifInfo.model)
            if (selectedBrandKey == null) {
                applyPreferredVariantForBrand(resolveEffectiveBrandKey())
            }
            resolvedModelLabel = resolveDisplayModelLine(context, exifInfo)

            renderPreviewBitmapForCurrentState(
                sourceUri = imported.uri,
                exifInfoOverride = exifInfo
            )
        }.onSuccess { previewBitmap ->
            if (requestId != renderRequestVersion) {
                recycleBitmapQuietly(previewBitmap)
                return@onSuccess
            }
            replaceRenderedPreviewBitmap(previewBitmap)
            clearRenderedFileCache()
            trimWatermarkTempFiles(
                cacheRoot = context.cacheDir,
                protectedPaths = buildSet {
                    activeSelectedSourceFile?.let { add(it.absolutePath) }
                    activeCropSourceFile?.let { add(it.absolutePath) }
                    renderedFile?.let { add(it.absolutePath) }
                }
            )
        }.onFailure { error ->
            if (error is CancellationException) {
                throw error
            }
            if (requestId == renderRequestVersion) {
                errorMessage = error.message ?: "生成水印失败"
            }
        }
        if (requestId == renderRequestVersion) {
            isWorking = false
            isRenderingPreview = false
            isImportingSourceImage = false
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            if (awaitingInitialModernImagePicker) {
                awaitingInitialModernImagePicker = false
                onBack()
            }
            return@rememberLauncherForActivityResult
        }
        val uri = result.data?.data ?: result.data?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        if (uri == null) {
            if (awaitingInitialModernImagePicker) {
                awaitingInitialModernImagePicker = false
                onBack()
            }
            return@rememberLauncherForActivityResult
        }
        awaitingInitialModernImagePicker = false
        scope.launch {
            importPickedImageAndRender(uri)
        }
    }
    val launchImagePicker = {
        imagePickerLauncher.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        )
    }

    LaunchedEffect(initialModernPresetKey, initialModernPreset, initialModernImageUriString) {
        val preset = initialModernPreset ?: return@LaunchedEffect
        val initialImageUri = initialModernImageUriString
        val shouldApplyPreset = appliedInitialModernPresetKey != preset.key ||
            (initialImageUri != null && appliedInitialModernImageUriString != initialImageUri)
        if (shouldApplyPreset) {
            applyModernPresetSpec(preset)
            appliedInitialModernPresetKey = preset.key
        }
        if (initialImageUri != null && appliedInitialModernImageUriString != initialImageUri) {
            appliedInitialModernImageUriString = initialImageUri
            awaitingInitialModernImagePicker = false
            currentStep = WatermarkFlowStep.Preview
            importPickedImageAndRender(Uri.parse(initialImageUri))
            onInitialModernPresetConsumed()
            return@LaunchedEffect
        }
        if (autoLaunchPickerForInitialModernPreset && selectedUri == null) {
            awaitingInitialModernImagePicker = true
            currentStep = WatermarkFlowStep.Preview
            launchImagePicker()
            onInitialModernPresetConsumed()
        } else if (shouldApplyPreset) {
            onInitialModernPresetConsumed()
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        toastMessage = null
    }
    LaunchedEffect(frameSideTopBorderRatio) {
        frameSideTopBorderRatioDraft = frameSideTopBorderRatio
    }
    LaunchedEffect(logoScaleFactor) {
        logoScaleFactorDraft = logoScaleFactor
    }
    LaunchedEffect(signatureHorizontalOffsetRatio) {
        signatureHorizontalOffsetRatioDraft = signatureHorizontalOffsetRatio
    }
    LaunchedEffect(signatureVerticalOffsetRatio) {
        signatureVerticalOffsetRatioDraft = signatureVerticalOffsetRatio
    }
    LaunchedEffect(signatureScaleFactor) {
        signatureScaleFactorDraft = signatureScaleFactor
    }
    LaunchedEffect(borderImageScaleFactor) {
        borderImageScaleFactorDraft = borderImageScaleFactor
    }
    LaunchedEffect(blurImageCornerRadiusRatio) {
        blurImageCornerRadiusRatioDraft = blurImageCornerRadiusRatio
    }
    LaunchedEffect(floatingImageCornerRadiusRatio) {
        floatingImageCornerRadiusRatioDraft = floatingImageCornerRadiusRatio
    }
    LaunchedEffect(colorPaletteHorizontalOffsetRatio) {
        colorPaletteHorizontalOffsetRatioDraft = colorPaletteHorizontalOffsetRatio
    }
    LaunchedEffect(colorPaletteVerticalOffsetRatio) {
        colorPaletteVerticalOffsetRatioDraft = colorPaletteVerticalOffsetRatio
    }

    LaunchedEffect(currentStep) {
        if (currentStep != WatermarkFlowStep.Preview) {
            runCatching { drawerState.close() }
        }
        if (currentStep == WatermarkFlowStep.Template) {
            templatePageOrderedTemplates = orderedTemplates
        }
        if (currentStep == WatermarkFlowStep.Crop) {
            showLoadingOverlay = false
        }
    }

    LaunchedEffect(isWorking, currentStep, isRenderingPreview, isImportingSourceImage) {
        if (currentStep == WatermarkFlowStep.Crop) {
            showLoadingOverlay = false
            return@LaunchedEffect
        }
        if (currentStep == WatermarkFlowStep.Preview && !isImportingSourceImage) {
            showLoadingOverlay = false
            return@LaunchedEffect
        }
        if (isWorking) {
            delay(140)
            if (isWorking) {
                showLoadingOverlay = true
                loadingOverlayShownAt = SystemClock.elapsedRealtime()
            }
        } else if (showLoadingOverlay) {
            val elapsed = SystemClock.elapsedRealtime() - loadingOverlayShownAt
            val remain = (260L - elapsed).coerceAtLeast(0L)
            if (remain > 0L) delay(remain)
            showLoadingOverlay = false
        }
    }

    LaunchedEffect(currentStep, drawerState.isOpen, templatePreviewDemoAssetsByPreset) {
        if (currentStep != WatermarkFlowStep.Template && !drawerState.isOpen) return@LaunchedEffect
        if (templatePreviewBitmaps.size >= WatermarkTemplatePreset.entries.size) return@LaunchedEffect
        val appContext = context.applicationContext
        val previewVariantIndex = preferredLogoVariantIndex(
            group = BRAND_LOGO_GROUP_MAP[TEMPLATE_PREVIEW_BRAND_KEY],
            avoidWhite = false
        )
        isTemplatePreviewWorking = true
        val nextMap = linkedMapOf<WatermarkTemplatePreset, Bitmap>()
        WatermarkTemplatePreset.entries.forEach { preset ->
            val demoAsset = templatePreviewDemoAssetsByPreset[preset]
            runCatching {
                val templatePreviewOutputSize = if (preset == WatermarkTemplatePreset.PaperDots) {
                    WATERMARK_PAPER_DOTS_TEMPLATE_PREVIEW_WIDTH to WATERMARK_PAPER_DOTS_TEMPLATE_PREVIEW_HEIGHT
                } else {
                    WATERMARK_TEMPLATE_PREVIEW_WIDTH to WATERMARK_TEMPLATE_PREVIEW_HEIGHT
                }
                val templatePreviewPaperDotsConfig = if (preset == WatermarkTemplatePreset.PaperDots) {
                    WatermarkPaperDotsConfig(
                        layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                        splitRatio = 0.42f,
                        backgroundMode = WatermarkPaperDotsBackgroundMode.Solid,
                        backgroundColorIndex = DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_COLOR_INDEX,
                        dotShape = WatermarkPaperDotsDotShape.Circle,
                        dotSize = 32f,
                        dotCount = 20
                    )
                } else {
                    WatermarkPaperDotsConfig()
                }
                val sourceBitmap = loadWatermarkTemplatePreviewSourceBitmap(
                    context = appContext,
                    demoAsset = demoAsset,
                    maxSidePx = WATERMARK_TEMPLATE_PREVIEW_MAX_SIDE
                ) ?: throw IllegalStateException("无法加载模板预览图")
                renderWatermarkPreviewBitmap(
                    context = appContext,
                    sourceUri = null,
                    sourceBitmapOverride = sourceBitmap.bitmap,
                    preserveSourceBitmapOverride = sourceBitmap.preserveAfterRender,
                    sourceIdentity = "watermark-template-preview:${sourceBitmap.identity}",
                    template = preset,
                    selectedBrandKey = TEMPLATE_PREVIEW_BRAND_KEY,
                    customLogoPath = null,
                    selectedVariantIndex = previewVariantIndex,
                    showLogoWatermark = true,
                    showModelWatermark = true,
                    showDateWatermark = true,
                    showExifWatermark = true,
                    showLocationWatermark = false,
                    showCityLocationWatermark = true,
                    modelLineOverride = TEMPLATE_PREVIEW_MODEL_LINE,
                    frameSideTopBorderRatio = 0.018f,
                    frameLayoutMode = WatermarkFrameLayoutMode.Center,
                    logoScaleFactor = 1f,
                    frameBackgroundColorIndex = 8,
                    thinBorderColorIndex = 8,
                    roundedBorderColorIndex = 8,
                    borderCornerMode = BorderCornerMode.Straight,
                    blurImageCornerRadiusRatio = DEFAULT_BLUR_IMAGE_CORNER_RATIO,
                    borderImageScaleFactor = 1f,
                    floatingBackgroundColorIndex = 8,
                    floatingStrokeColorIndex = 0,
                    floatingImageCornerRadiusRatio = DEFAULT_FLOATING_IMAGE_CORNER_RATIO,
                    filmFrameIndex = 0,
                    polaroidFrameIndex = 0,
                    colorPaletteStyleIndex = 0,
                    colorPaletteVariantIndex = 0,
                    colorPaletteElementCount = 6,
                    colorPaletteShowHexValue = true,
                    colorPaletteShowRgbValue = true,
                    paperDotsConfig = templatePreviewPaperDotsConfig,
                    watermarkFontKey = DEFAULT_WATERMARK_TEXT_FONT_KEY,
                    signatureFontKey = DEFAULT_SIGNATURE_FONT_KEY,
                    exifInfoOverride = TEMPLATE_PREVIEW_EXIF_INFO,
                    signatureText = "",
                    signatureColor = null,
                    outputSizeOverride = templatePreviewOutputSize,
                    previewMaxSidePx = WATERMARK_TEMPLATE_PREVIEW_MAX_SIDE
                )
            }.onSuccess { previewBitmap ->
                nextMap[preset] = previewBitmap
            }
        }
        if (nextMap.isNotEmpty()) {
            replaceTemplatePreviewBitmaps(nextMap)
        }
        trimWatermarkTempFiles(
            cacheRoot = appContext.cacheDir,
            protectedPaths = buildSet {
                renderedFile?.let { add(it.absolutePath) }
                activeSelectedSourceFile?.let { add(it.absolutePath) }
                activeCropSourceFile?.let { add(it.absolutePath) }
            }
        )
        isTemplatePreviewWorking = false
    }

    DisposableEffect(Unit) {
        onDispose {
            cancelPendingPreviewRender()
            runCatching {
                recycleBitmapQuietly(renderedPreviewBitmap)
            }
            runCatching {
                recycleBitmapQuietly(previewSourceBitmap)
            }
            runCatching {
                recycleBitmapQuietly(previewSpecialModeForegroundBitmap)
            }
            runCatching {
                recycleBitmapQuietly(screenBackdropBitmap)
            }
            runCatching {
                recycleBitmapQuietly(previewBackdropBitmap)
            }
            runCatching {
                renderedFile?.let { file ->
                    if (file.exists()) file.delete()
                }
            }
            runCatching {
                templatePreviewBitmaps.values.forEach { bitmap ->
                    if (!isWatermarkTemplatePreviewBitmapCached(bitmap)) {
                        recycleBitmapQuietly(bitmap)
                    }
                }
            }
            runCatching {
                activeSelectedSourceFile?.let { file ->
                    if (file.exists()) file.delete()
                }
            }
            runCatching {
                activeCropSourceFile?.let { file ->
                    if (file.exists()) file.delete()
                }
            }
            runCatching {
                cropSourceBitmap?.let { bitmap ->
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }
    }
    LaunchedEffect(selectedUri, renderedPreviewBitmap, screenBackdropWidthPx, screenBackdropHeightPx) {
        val fallbackBitmap = renderedPreviewBitmap?.takeIf { !it.isRecycled }
        if (selectedUri == null && fallbackBitmap == null) {
            replacePreviewBackdropBitmap(null)
            return@LaunchedEffect
        }
        val backdropBitmap = withContext(Dispatchers.Default) {
            val decodedSource = if (fallbackBitmap == null && selectedUri != null) {
                runCatching {
                    decodeSourceBitmap(
                        context = context,
                        uri = selectedUri,
                        requestedMaxSidePx = max(screenBackdropWidthPx, screenBackdropHeightPx).coerceAtMost(1400)
                    )
                }.getOrNull()
            } else {
                null
            }
            val source = decodedSource ?: fallbackBitmap ?: return@withContext null
            try {
                createWatermarkEditorBackdropBitmap(
                    source = source,
                    outputWidth = screenBackdropWidthPx,
                    outputHeight = screenBackdropHeightPx
                )
            } finally {
                if (decodedSource != null && decodedSource !== fallbackBitmap) {
                    recycleBitmapQuietly(decodedSource)
                }
            }
        }
        if (backdropBitmap == null) {
            replacePreviewBackdropBitmap(null)
            return@LaunchedEffect
        }
        try {
            currentCoroutineContext().ensureActive()
            replacePreviewBackdropBitmap(backdropBitmap)
        } catch (cancelled: CancellationException) {
            recycleBitmapQuietly(backdropBitmap)
            throw cancelled
        }
    }
    BackHandler(
        enabled = drawerState.isOpen ||
            currentStep == WatermarkFlowStep.Crop ||
            currentStep == WatermarkFlowStep.CopyrightProfile ||
            (currentStep == WatermarkFlowStep.Template && selectedUri != null)
    ) {
        handleInnerBack()
    }

    val effectiveBackdropBitmap = screenBackdropBitmap?.takeIf { !it.isRecycled }
        ?: previewBackdropBitmap?.takeIf { !it.isRecycled }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fallbackPageBackground)
    ) {
            if (effectiveBackdropBitmap != null) {
            HomeCardLongPressHoverBackdrop(
                active = true,
                visuals = backdropVisuals,
                backdropBitmap = effectiveBackdropBitmap,
                isDark = isDark,
                dismissOnTap = false,
                onDismissRequest = {},
                modifier = Modifier.fillMaxSize()
            )
            }
            ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
            if (currentStep == WatermarkFlowStep.Preview) {
                ModalDrawerSheet {
                    Text(
                        text = watermarkStrings.changeTemplate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = primaryTextColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (userPresets.isNotEmpty()) {
                            items(userPresets, key = { it.id }) { preset ->
                                val presetTemplate = preset.template
                                if (presetTemplate != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(listItemBackground)
                                            .border(1.dp, cardOutlineColor, RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    performHaptic()
                                                    applyUserPreset(preset)
                                                    customizePanelKey = preferredWatermarkCustomizePanelForCurrentState(
                                                        template = selectedTemplate,
                                                        cutoutOverlayStyle = cutoutOverlayStyle,
                                                        subjectOutlineEnabled = subjectOutlineEnabled
                                                    ).name
                                                    collapsedCustomizePanel = false
                                                    selectedUri?.let { rerenderForCurrentSelection(it, "应用预设失败") }
                                                    scope.launch { drawerState.close() }
                                                },
                                                onLongClick = {
                                                    performHaptic()
                                                    pendingDeletePresetId = preset.id
                                                }
                                            )
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val previewBitmap = templatePreviewBitmaps[presetTemplate]
                                        val presetDisplayName = preset.displayName(appLanguage, presetTemplate)
                                        Box(
                                            modifier = Modifier
                                                .width(92.dp)
                                                .aspectRatio(3f / 4f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(previewCardBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (previewBitmap != null) {
                                                Image(
                                                    bitmap = previewBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(presetDisplayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = primaryTextColor)
                                                Icon(
                                                    imageVector = Icons.Outlined.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = secondaryTextColor
                                                )
                                            }
                                            Text("${watermarkStrings.savedPresetPrefix} · ${presetTemplate.localizedTitle(appLanguage)}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = secondaryTextColor)
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        items(orderedTemplates) { template ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(listItemBackground)
                                    .border(1.dp, cardOutlineColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (selectedTemplate != template) {
                                            selectedTemplate = template
                                            cutoutOverlayStyle = WatermarkCutoutOverlayStyle.None
                                            subjectOutlineEnabled = false
                                            subjectOutlineLineStyle = WatermarkSubjectOutlineLineStyle.Solid
                                            subjectOutlineWidthRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
                                            subjectOutlineWidthRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
                                            subjectOutlineColorIndex = DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX
                                            subjectOutlineOuterBorderEnabled = false
                                            subjectOutlineOffsetRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
                                            subjectOutlineOffsetRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
                                            subjectOutlineSymbolText = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT
                                            subjectOutlineSymbolSizeRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
                                            subjectOutlineSymbolSizeRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
                                            selectedPresetId = null
                                            customizePanelKey = preferredWatermarkCustomizePanelForCurrentState(
                                                template = template,
                                                cutoutOverlayStyle = cutoutOverlayStyle,
                                                subjectOutlineEnabled = subjectOutlineEnabled
                                            ).name
                                            collapsedCustomizePanel = false
                                            selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.switchTemplateFailed) }
                                        }
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val previewBitmap = templatePreviewBitmaps[template]
                                Box(
                                    modifier = Modifier
                                        .width(92.dp)
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(previewCardBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (previewBitmap != null) {
                                        Image(
                                            bitmap = previewBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(
                                                model = R.drawable.brand_logo_canon_color,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .width(38.dp)
                                                    .aspectRatio(1.6f),
                                                contentScale = ContentScale.Fit
                                            )
                                            Text("EOS 6D Mark II", fontSize = 9.sp, color = secondaryTextColor)
                                            Text("35mm f/2.8 1/250 ISO100", fontSize = 9.sp, color = secondaryTextColor)
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(template.localizedTitle(appLanguage), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = primaryTextColor)
                                        IconButton(
                                            onClick = {
                                                toggleLikedTemplate(template.name)
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (likedTemplateKeys.contains(template.name)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = watermarkStrings.likedTemplate,
                                                tint = if (likedTemplateKeys.contains(template.name)) Color(0xFFEF4444) else secondaryTextColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(template.localizedSubtitle(appLanguage), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = secondaryTextColor)
                                }
                            }
                        }
                    }
                }
            }
            }
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (currentStep == WatermarkFlowStep.Crop) Color.Black else Color.Transparent)
        ) {
            if (currentStep == WatermarkFlowStep.Crop) {
                WatermarkCropPage(
                    isDark = isDark,
                    preset = cropEditorPreset,
                    cropAspectRatio = cropEditorAspectRatio,
                    bitmap = cropSourceBitmap,
                    centerX = cropCenterX,
                    centerY = cropCenterY,
                    zoom = cropZoom,
                    isWorking = isWorking,
                    presets = SOCIAL_CROP_PRESETS,
                    canUndo = cropEditorUndoHistory.isNotEmpty(),
                    onUndo = undoCropEditorChange,
                    onTransform = { nextX, nextY, nextZoom ->
                        cropCenterX = nextX
                        cropCenterY = nextY
                        cropZoom = nextZoom
                    },
                    onFreeModeResize = { nextAspectRatio, nextCenterX, nextCenterY, nextZoom, recordUndo ->
                        updateCropEditorState(
                            WatermarkCropEditorHistoryEntry(
                                presetKey = null,
                                aspectRatio = nextAspectRatio,
                                centerX = nextCenterX,
                                centerY = nextCenterY,
                                zoom = nextZoom
                            ),
                            recordUndo
                        )
                    },
                    onReset = resetCropEditorView,
                    onPresetChange = { nextPreset ->
                        val nextAspectRatio = resolveCropAspectRatioForTemplate(
                            template = selectedTemplate,
                            preset = nextPreset,
                            frameSideTopBorderRatio = frameSideTopBorderRatio,
                            colorPaletteStyle = selectedColorPaletteStyle,
                            colorPaletteVariantIndex = selectedColorPaletteVariantIndex,
                            colorPaletteShowHexValue = colorPaletteShowHexValue,
                            colorPaletteShowRgbValue = colorPaletteShowRgbValue
                        )
                        cropSourceBitmap?.let { source ->
                            updateCropEditorState(
                                WatermarkCropEditorHistoryEntry(
                                    presetKey = nextPreset.key,
                                    aspectRatio = nextAspectRatio,
                                    centerX = source.width / 2f,
                                    centerY = source.height / 2f,
                                    zoom = 1f
                                ),
                                true
                            )
                        }
                    },
                    onFreeAspectSelected = {
                        cropSourceBitmap?.let { source ->
                            updateCropEditorState(
                                WatermarkCropEditorHistoryEntry(
                                    presetKey = null,
                                    aspectRatio = source.width.toFloat() / source.height.toFloat().coerceAtLeast(1f),
                                    centerX = source.width / 2f,
                                    centerY = source.height / 2f,
                                    zoom = 1f
                                ),
                                true
                            )
                        }
                    },
                    onCancel = exitCropToPreview,
                    onSave = {
                        val bitmap = cropSourceBitmap
                        val sourceForCrop = selectedUri
                        if (bitmap == null || sourceForCrop == null) {
                            errorMessage = watermarkExtraCopy.cropDataUnavailable
                            exitCropToPreview()
                            return@WatermarkCropPage
                        }
                        val preset = cropEditorPreset
                        val exifSourceForCrop = selectedExifSourceUri ?: sourceForCrop
                        scope.launch {
                            val requestId = beginRenderRequest()
                            isWorking = true
                            isRenderingPreview = true
                            errorMessage = null
                            runCatching {
                                val cropped = cropBitmapForAspect(
                                    source = bitmap,
                                    targetAspect = cropEditorAspectRatio,
                                    centerX = cropCenterX,
                                    centerY = cropCenterY,
                                    zoom = cropZoom
                                )
                                val previousCropFile = activeCropSourceFile
                                val tempDir = ensureWatermarkTempDir(context.cacheDir)
                                val tempFile = File(
                                    tempDir,
                                    "crop_${preset?.key ?: "free"}_${System.currentTimeMillis()}.jpg"
                                )
                                FileOutputStream(tempFile).use { out ->
                                    cropped.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                }
                                cropped.recycle()
                                val croppedUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                copyExifMetadataToFile(context.contentResolver, exifSourceForCrop, tempFile)
                                selectedImageUri = croppedUri.toString()
                                activeCropSourceFile = tempFile
                                if (previousCropFile != null && previousCropFile.absolutePath != tempFile.absolutePath) {
                                    runCatching { previousCropFile.delete() }
                                }
                                selectedSocialCropPresetKey = preset?.key
                                val exifInfo = readExifInfo(context, exifSourceForCrop)
                                currentExifInfo = exifInfo
                                detectedBrandKey = detectBrandKey(exifInfo.make, exifInfo.model)
                                if (selectedBrandKey == null) {
                                    applyPreferredVariantForBrand(resolveEffectiveBrandKey())
                                }
                                resolvedModelLabel = resolveDisplayModelLine(context, exifInfo)
                                val croppedOutputSizeOverride = resolveOutputSizeOverride(selectedTemplate, preset)
                                val previewBitmap = renderPreviewBitmapForCurrentState(
                                    sourceUri = croppedUri,
                                    exifInfoOverride = exifInfo,
                                    previewSizeOverride = resolveWatermarkPreviewOutputSizeOverride(croppedOutputSizeOverride)
                                )
                                if (requestId != renderRequestVersion) {
                                    recycleBitmapQuietly(previewBitmap)
                                    return@runCatching
                                }
                                replaceRenderedPreviewBitmap(previewBitmap)
                                clearRenderedFileCache()
                                trimWatermarkTempFiles(
                                    cacheRoot = context.cacheDir,
                                    protectedPaths = buildSet {
                                        add(tempFile.absolutePath)
                                        activeSelectedSourceFile?.let { add(it.absolutePath) }
                                        renderedFile?.let { add(it.absolutePath) }
                                    }
                                )
                                exitCropToPreview()
                                runCatching { drawerState.close() }
                            }.onFailure { error ->
                                errorMessage = error.message ?: watermarkExtraCopy.cropFailed
                                exitCropToPreview()
                            }
                            if (requestId == renderRequestVersion) {
                                isWorking = false
                                isRenderingPreview = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    UnifiedSubpageTopBar(
                        title = watermarkStrings.pageTitle,
                        onBack = {
                            performHaptic()
                            handleInnerBack()
                        },
                        titleColor = primaryTextColor,
                        backButtonContainerColor = topBarButtonBackground,
                        backButtonIconColor = topBarIconTint
                    ) {
                        if (currentStep == WatermarkFlowStep.Preview) {
                            UnifiedSubpageTopBarIconButton(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = watermarkStrings.save,
                                onClick = {
                                    performHaptic()
                                    performSaveOutput()
                                },
                                enabled = renderedPreviewBitmap != null && selectedUri != null && (!isWorking || isSavingOutput),
                                containerColor = topBarButtonBackground,
                                iconTint = topBarIconTint,
                                loading = isSavingOutput
                            )
                        }
                    }

                    val isPreviewStep = currentStep == WatermarkFlowStep.Preview
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = if (isPreviewStep) 8.dp else 16.dp)
                            .padding(
                                horizontal = if (isPreviewStep) 4.dp else 14.dp,
                                vertical = if (isPreviewStep) 10.dp else 14.dp
                            )
                    ) {
                        when (currentStep) {
                    WatermarkFlowStep.Template -> {
                        LazyColumn(
                            state = templateListState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (userPresets.isNotEmpty()) {
                                items(userPresets, key = { it.id }) { preset ->
                                    val presetTemplate = preset.template
                                    if (presetTemplate != null) {
                                        Row(
                                            modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(listItemBackground)
                                            .border(1.dp, cardOutlineColor, RoundedCornerShape(14.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    performHaptic()
                                                    applyUserPreset(preset)
                                                    val currentUri = selectedUri
                                                    if (currentUri != null) {
                                                        customizePanelKey = preferredWatermarkCustomizePanelForCurrentState(
                                                            template = selectedTemplate,
                                                            cutoutOverlayStyle = cutoutOverlayStyle,
                                                            subjectOutlineEnabled = subjectOutlineEnabled
                                                        ).name
                                                        collapsedCustomizePanel = false
                                                        currentStep = WatermarkFlowStep.Preview
                                                        scope.launch { drawerState.close() }
                                                        rerenderForCurrentSelection(currentUri, "应用预设失败")
                                                    } else {
                                                        launchImagePicker()
                                                    }
                                                },
                                                onLongClick = {
                                                    performHaptic()
                                                    pendingDeletePresetId = preset.id
                                                }
                                            )
                                            .padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val previewBitmap = templatePreviewBitmaps[presetTemplate]
                                            val presetDisplayName = preset.displayName(appLanguage, presetTemplate)
                                            Box(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .aspectRatio(3f / 4f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(previewCardBackground),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (previewBitmap != null) {
                                                    Image(
                                                        bitmap = previewBitmap.asImageBitmap(),
                                                        contentDescription = "$presetDisplayName ${watermarkStrings.previewContentDescription}",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = presetDisplayName,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = primaryTextColor
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Outlined.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = secondaryTextColor
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${watermarkStrings.savedPresetPrefix} · ${presetTemplate.localizedTitle(appLanguage)}",
                                                    fontSize = 12.sp,
                                                    lineHeight = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = secondaryTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            items(templatePageOrderedTemplates) { template ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(listItemBackground)
                                        .border(1.dp, cardOutlineColor, RoundedCornerShape(14.dp))
                                        .clickable {
                                            performHaptic()
                                            if (selectedTemplate != template) {
                                                selectedTemplate = template
                                                cutoutOverlayStyle = WatermarkCutoutOverlayStyle.None
                                                subjectOutlineEnabled = false
                                                subjectOutlineLineStyle = WatermarkSubjectOutlineLineStyle.Solid
                                                subjectOutlineWidthRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
                                                subjectOutlineWidthRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
                                                subjectOutlineColorIndex = DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX
                                                subjectOutlineOuterBorderEnabled = false
                                                subjectOutlineOffsetRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
                                                subjectOutlineOffsetRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
                                                subjectOutlineSymbolText = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT
                                                subjectOutlineSymbolSizeRatio = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
                                                subjectOutlineSymbolSizeRatioDraft = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
                                                selectedPresetId = null
                                            }
                                            val currentUri = selectedUri
                                            if (currentUri != null) {
                                                customizePanelKey = preferredWatermarkCustomizePanelForCurrentState(
                                                    template = template,
                                                    cutoutOverlayStyle = cutoutOverlayStyle,
                                                    subjectOutlineEnabled = subjectOutlineEnabled
                                                ).name
                                                collapsedCustomizePanel = false
                                                currentStep = WatermarkFlowStep.Preview
                                                scope.launch { drawerState.close() }
                                                rerenderForCurrentSelection(currentUri, watermarkExtraCopy.switchTemplateFailed)
                                            } else {
                                                launchImagePicker()
                                            }
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val previewBitmap = templatePreviewBitmaps[template]
                                    Box(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .aspectRatio(3f / 4f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(previewCardBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (previewBitmap != null) {
                                            Image(
                                                bitmap = previewBitmap.asImageBitmap(),
                                                contentDescription = "${template.localizedTitle(appLanguage)} ${watermarkStrings.previewContentDescription}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                AsyncImage(
                                                    model = R.drawable.brand_logo_canon_color,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .width(42.dp)
                                                        .aspectRatio(1.6f),
                                                    contentScale = ContentScale.Fit
                                                )
                                                Text("EOS 6D Mark II", fontSize = 9.sp, color = secondaryTextColor)
                                                Text("35mm f/2.8 1/250 ISO100", fontSize = 9.sp, color = secondaryTextColor)
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = template.localizedTitle(appLanguage),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = primaryTextColor
                                            )
                                            IconButton(
                                                onClick = {
                                                    performHaptic()
                                                    toggleLikedTemplate(template.name)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (likedTemplateKeys.contains(template.name)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                    contentDescription = watermarkStrings.likedTemplate,
                                                    tint = if (likedTemplateKeys.contains(template.name)) Color(0xFFEF4444) else secondaryTextColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = template.localizedSubtitle(appLanguage),
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp,
                                            color = secondaryTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    WatermarkFlowStep.Preview -> {
                        val canOutput = renderedPreviewBitmap != null && !isWorking
                        val sourceUri = selectedUri
                        val exifForUi = effectiveExifInfo
                        val isFullColorTemplate = selectedTemplate == WatermarkTemplatePreset.FullColor
                        val hasDateData = !exifForUi?.footerDateLine().isNullOrBlank()
                        val hasSpecData = !exifForUi?.footerSpecLine().isNullOrBlank()
                        val hasLocationData = !exifForUi?.footerLocationLine().isNullOrBlank()
                        val hasCityLocationData = !exifForUi?.footerCityLocationLine().isNullOrBlank()
                        val templateSupportsCustomControls = selectedTemplate in setOf(
                            WatermarkTemplatePreset.WatermarkFrame,
                            WatermarkTemplatePreset.BlurBackground,
                            WatermarkTemplatePreset.DirectWatermark,
                            WatermarkTemplatePreset.FullColor
                        )
                        val templateSupportsSignature = selectedTemplate in setOf(
                            WatermarkTemplatePreset.WatermarkFrame,
                            WatermarkTemplatePreset.BlurBackground,
                            WatermarkTemplatePreset.ThinWhiteBorder,
                            WatermarkTemplatePreset.DirectWatermark
                        )
                        val isCutoutBreakoutMode = isWatermarkCutoutBreakoutMode(
                            template = selectedTemplate,
                            cutoutOverlayStyle = cutoutOverlayStyle
                        )
                        val isSubjectOutlineMode = isWatermarkSubjectOutlineMode(
                            template = selectedTemplate,
                            cutoutOverlayStyle = cutoutOverlayStyle,
                            subjectOutlineEnabled = subjectOutlineEnabled
                        )
                        val supportsSignaturePanel = templateSupportsSignature || isCutoutBreakoutMode
                        val templateSupportsFloatingControls = selectedTemplate == WatermarkTemplatePreset.Floating
                        val templateSupportsBorderControls = selectedTemplate in setOf(
                            WatermarkTemplatePreset.WatermarkFrame,
                            WatermarkTemplatePreset.BlurBackground,
                            WatermarkTemplatePreset.ThinWhiteBorder,
                            WatermarkTemplatePreset.Floating,
                            WatermarkTemplatePreset.FilmRetro,
                            WatermarkTemplatePreset.Polaroid
                        )
                        val availableCustomizePanels = if (selectedTemplate == WatermarkTemplatePreset.PaperDots) {
                            listOf(
                                WatermarkCustomizePanel.Template,
                                WatermarkCustomizePanel.PaperDotsLayout,
                                WatermarkCustomizePanel.PaperDotsBackground,
                                WatermarkCustomizePanel.PaperDotsDots,
                                WatermarkCustomizePanel.Copyright
                            )
                        } else if (selectedTemplate == WatermarkTemplatePreset.ColorPalette) {
                            buildList {
                                add(WatermarkCustomizePanel.Template)
                                add(WatermarkCustomizePanel.LayoutPreset)
                                if (selectedColorPaletteStyle == WatermarkColorPaletteStyle.GlassDots) {
                                    add(WatermarkCustomizePanel.GlassDots)
                                }
                                add(WatermarkCustomizePanel.Copyright)
                            }
                        } else if (selectedTemplate == WatermarkTemplatePreset.FullColor) {
                            listOf(
                                WatermarkCustomizePanel.Template,
                                WatermarkCustomizePanel.Params,
                                WatermarkCustomizePanel.Copyright
                            )
                        } else if (isCutoutBreakoutMode || isSubjectOutlineMode) {
                            buildList {
                                add(WatermarkCustomizePanel.Template)
                                add(WatermarkCustomizePanel.Params)
                                if (isCutoutBreakoutMode) {
                                    add(WatermarkCustomizePanel.Signature)
                                }
                                if (templateSupportsBorderControls) {
                                    add(WatermarkCustomizePanel.Border)
                                }
                                add(WatermarkCustomizePanel.Copyright)
                            }
                        } else {
                            buildList {
                                add(WatermarkCustomizePanel.Template)
                                if (selectedTemplate !in setOf(WatermarkTemplatePreset.FilmRetro, WatermarkTemplatePreset.Polaroid)) {
                                    add(WatermarkCustomizePanel.Crop)
                                }
                                if (templateSupportsCustomControls) {
                                    add(WatermarkCustomizePanel.Logo)
                                    add(WatermarkCustomizePanel.Params)
                                }
                                if (templateSupportsSignature) {
                                    add(WatermarkCustomizePanel.Signature)
                                }
                                if (templateSupportsBorderControls) {
                                    add(WatermarkCustomizePanel.Border)
                                }
                                add(WatermarkCustomizePanel.Copyright)
                            }
                        }
                        val activeCustomizePanel = availableCustomizePanels.firstOrNull { it == selectedCustomizePanel }
                            ?: availableCustomizePanels.firstOrNull()
                            ?: WatermarkCustomizePanel.Border
                        LaunchedEffect(currentStep, selectedTemplate, activeCustomizePanel.name) {
                            if (currentStep == WatermarkFlowStep.Preview && selectedCustomizePanel != activeCustomizePanel) {
                                customizePanelKey = activeCustomizePanel.name
                            }
                        }
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val previewBitmap = renderedPreviewBitmap
                            val panelSurfaceMaxHeight = (maxHeight * 0.40f).coerceIn(220.dp, 312.dp)
                            val panelBodyMaxHeight = (panelSurfaceMaxHeight - 28.dp).coerceAtLeast(146.dp)
                            val dockContainerColor = if (isDark) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                            } else {
                                Color.White.copy(alpha = 0.84f)
                            }
                            val dockBorderColor = if (isDark) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            } else {
                                Color.Black.copy(alpha = 0.08f)
                            }
                            val dockActiveBackground = if (isDark) {
                                Color.White.copy(alpha = 0.10f)
                            } else {
                                Color.Black.copy(alpha = 0.08f)
                            }
                            val panelTabs = availableCustomizePanels.map { panel ->
                                WatermarkCompactPanelTabSpec(
                                    key = panel.name,
                                    title = when {
                                        panel == WatermarkCustomizePanel.Params && isCutoutBreakoutMode ->
                                            watermarkCutoutBreakoutPanelTitle(appLanguage)
                                        panel == WatermarkCustomizePanel.Params && isSubjectOutlineMode ->
                                            watermarkSubjectOutlineSectionTitle(appLanguage)
                                        else -> panel.localizedTitle(appLanguage)
                                    },
                                    iconResId = when (panel) {
                                        WatermarkCustomizePanel.LayoutPreset -> null
                                        WatermarkCustomizePanel.GlassDots -> null
                                        WatermarkCustomizePanel.PaperDotsLayout -> null
                                        WatermarkCustomizePanel.PaperDotsBackground -> null
                                        WatermarkCustomizePanel.PaperDotsDots -> null
                                        WatermarkCustomizePanel.Logo -> R.drawable.ic_wm_panel_logo
                                        WatermarkCustomizePanel.Params -> R.drawable.ic_wm_panel_params
                                        WatermarkCustomizePanel.Signature -> R.drawable.ic_wm_panel_signature
                                        WatermarkCustomizePanel.Border -> R.drawable.ic_wm_panel_border
                                        WatermarkCustomizePanel.Copyright -> R.drawable.ic_wm_panel_copyright
                                        else -> null
                                    }
                                )
                            }
                            val density = LocalDensity.current
                            val navigationBarBottomInset = with(density) {
                                WindowInsets.navigationBars.getBottom(this).toDp()
                            }
                            val previewBottomInset = 92.dp + navigationBarBottomInset
                            val previewAspectRatio = remember(previewBitmap, previewOutputSizeOverride) {
                                when {
                                    previewBitmap != null && previewBitmap.height > 0 ->
                                        previewBitmap.width.toFloat() / previewBitmap.height.toFloat()
                                    previewOutputSizeOverride != null &&
                                        previewOutputSizeOverride.second > 0 ->
                                        previewOutputSizeOverride.first.toFloat() / previewOutputSizeOverride.second.toFloat()
                                    else -> 1f
                                }.coerceIn(0.4f, 2.6f)
                            }
                            val previewGlassBackdrop = rememberLiquidGlassBackdrop()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent)
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (previewGlassBackdrop != null) {
                                                Modifier.layerBackdrop(previewGlassBackdrop)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = previewBottomInset),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    val availableWidth = maxWidth
                                    val availableHeight = maxHeight
                                    val previewMaxHeight = availableHeight * resolveWatermarkPreviewMaxHeightRatio(previewAspectRatio)
                                    val widthFromHeight = previewMaxHeight * previewAspectRatio
                                    val fittedWidth = minOf(availableWidth, widthFromHeight)
                                    val fittedHeight = minOf(previewMaxHeight, fittedWidth / previewAspectRatio)
                                    val previewFrameBorderColor = if (selectedTemplate == WatermarkTemplatePreset.WatermarkFrame) {
                                        if (isDark) {
                                            Color(0xFF8F98A3).copy(alpha = 0.34f)
                                        } else {
                                            Color(0xFFBFC6CE).copy(alpha = 0.92f)
                                        }
                                    } else {
                                        if (isDark) {
                                            Color.White.copy(alpha = 0.12f)
                                        } else {
                                            Color.White.copy(alpha = 0.42f)
                                        }
                                    }
                                    val previewImageModifier = Modifier
                                        .width(fittedWidth)
                                        .height(fittedHeight)
                                        .border(
                                            width = 0.8.dp,
                                            color = previewFrameBorderColor
                                        )
                                        .then(
                                            if (isFullColorTemplate && sourceUri != null) {
                                                Modifier.pointerInput(
                                                    sourceUri,
                                                    fittedWidth,
                                                    fittedHeight,
                                                    selectedTemplate
                                                ) {
                                                    val dragWidth = max(with(density) { fittedWidth.toPx() } * 0.42f, 1f)
                                                    val dragHeight = max(with(density) { fittedHeight.toPx() } * 0.24f, 1f)
                                                    detectDragGestures(
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            val nextHorizontal = (
                                                                fullColorHorizontalOffsetRatio -
                                                                    dragAmount.x / dragWidth
                                                                ).coerceIn(-1f, 1f)
                                                            val nextVertical = (
                                                                fullColorVerticalOffsetRatio -
                                                                    dragAmount.y / dragHeight
                                                                ).coerceIn(-1f, 1f)
                                                            if (
                                                                kotlin.math.abs(nextHorizontal - fullColorHorizontalOffsetRatio) > 0.0005f ||
                                                                    kotlin.math.abs(nextVertical - fullColorVerticalOffsetRatio) > 0.0005f
                                                            ) {
                                                                fullColorHorizontalOffsetRatio = nextHorizontal
                                                                fullColorVerticalOffsetRatio = nextVertical
                                                                val now = SystemClock.elapsedRealtime()
                                                                if (now - fullColorLastPreviewDragRenderAt >= 56L) {
                                                                    fullColorLastPreviewDragRenderAt = now
                                                                    rerenderForCurrentSelectionWithPreviewSide(
                                                                        sourceUri,
                                                                        watermarkExtraCopy.updateWatermarkFailed,
                                                                        960
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            rerenderForCurrentSelection(
                                                                sourceUri,
                                                                watermarkExtraCopy.updateWatermarkFailed
                                                            )
                                                        }
                                                    )
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                    if (previewBitmap == null && selectedUri == null) {
                                        Text(
                                            text = watermarkStrings.noImageSelected,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                                        )
                                    } else if (previewBitmap != null) {
                                        Image(
                                            bitmap = previewBitmap.asImageBitmap(),
                                            contentDescription = "水印预览",
                                            modifier = previewImageModifier,
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(selectedUri)
                                                .crossfade(false)
                                                .build(),
                                            contentDescription = "水印预览",
                                            modifier = previewImageModifier,
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }

                                CompositionLocalProvider(LocalLiquidGlassBackdrop provides previewGlassBackdrop) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(start = 12.dp, end = 12.dp)
                                            .bottomNavigationBarPadding(12.dp)
                                            .fillMaxWidth()
                                            .widthIn(max = 540.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (panelTabs.isNotEmpty()) {
                                            WatermarkCompactDockHeader(
                                                items = panelTabs,
                                                selectedKey = activeCustomizePanel.name,
                                                collapsed = collapsedCustomizePanel,
                                                onToggleCollapsed = { collapsedCustomizePanel = !collapsedCustomizePanel },
                                                onSelect = {
                                                    if (SystemClock.elapsedRealtime() < suppressPreviewDockInputUntil) {
                                                        return@WatermarkCompactDockHeader
                                                    }
                                                    customizePanelKey = it
                                                    collapsedCustomizePanel = false
                                                },
                                                containerColor = dockContainerColor,
                                                activeBackgroundColor = dockActiveBackground,
                                                outlineColor = dockBorderColor,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = !collapsedCustomizePanel,
                                            enter = expandVertically(
                                                expandFrom = Alignment.Top,
                                                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                                            ) + fadeIn(animationSpec = tween(durationMillis = 110)),
                                            exit = shrinkVertically(
                                                shrinkTowards = Alignment.Top,
                                                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                                            ) + fadeOut(animationSpec = tween(durationMillis = 100)),
                                            label = "watermarkEditorPanelBody"
                                        ) {
                                            WatermarkCompactPanelSurface(
                                                title = null,
                                                subtitle = null,
                                                containerColor = if (isDark) {
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
                                                } else {
                                                    Color.White.copy(alpha = 0.76f)
                                                },
                                                outlineColor = dockBorderColor,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = panelSurfaceMaxHeight)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = panelBodyMaxHeight)
                                                        .verticalScroll(previewScrollState)
                                                ) {

                                    if (activeCustomizePanel == WatermarkCustomizePanel.Template) {
                                        Text(
                                            text = watermarkStrings.templateActions,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = primaryTextColor
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    performHaptic()
                                                    launchImagePicker()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, dockBorderColor),
                                                colors = actionButtonColors,
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) { Text(watermarkStrings.reselectImage, fontSize = 12.sp, color = primaryTextColor) }
                                            Button(
                                                onClick = {
                                                    performHaptic()
                                                    scope.launch { drawerState.open() }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, dockBorderColor),
                                                colors = actionButtonColors,
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) { Text(watermarkStrings.changeTemplate, fontSize = 12.sp, color = primaryTextColor) }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                performHaptic()
                                                savePresetNameInput = resolveNextPresetName()
                                                isSavePresetDialogVisible = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, dockBorderColor),
                                            colors = actionButtonColors,
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) { Text(watermarkStrings.saveCurrentTemplate, fontSize = 12.sp, color = primaryTextColor) }
                                        if (selectedTemplate == WatermarkTemplatePreset.WatermarkFrame) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(watermarkStrings.layoutMode, fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                WatermarkFrameLayoutMode.entries.forEach { mode ->
                                                    FilterChip(
                                                        selected = frameLayoutMode == mode,
                                                        onClick = {
                                                            if (frameLayoutMode != mode) {
                                                                frameLayoutMode = mode
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "更新定位失败") }
                                                            }
                                                        },
                                                        label = { Text(mode.localizedTitle(appLanguage)) },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                        }
                                        if (selectedTemplate == WatermarkTemplatePreset.ThinWhiteBorder) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(watermarkStrings.borderCorner, fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                BorderCornerMode.entries.forEach { mode ->
                                                    FilterChip(
                                                        selected = borderCornerMode == mode,
                                                        onClick = {
                                                            if (borderCornerMode != mode) {
                                                                borderCornerMode = mode
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "更新边框形态失败") }
                                                            }
                                                        },
                                                        label = { Text(mode.localizedTitle(appLanguage)) },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(watermarkStrings.imageRatio, fontSize = 12.sp, color = primaryTextColor)
                                            Slider(
                                                value = borderImageScaleFactorDraft,
                                                onValueChange = { value -> borderImageScaleFactorDraft = value },
                                                onValueChangeFinished = {
                                                    if (borderImageScaleFactor != borderImageScaleFactorDraft) {
                                                        borderImageScaleFactor = borderImageScaleFactorDraft
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新图片占比失败") }
                                                    }
                                                },
                                                valueRange = 0.78f..1f
                                            )
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.LayoutPreset
                                        && selectedTemplate == WatermarkTemplatePreset.ColorPalette
                                    ) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = watermarkStrings.layoutPreset,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = primaryTextColor
                                                )
                                                Text(
                                                    text = buildString {
                                                        append("${watermarkStrings.currentStylePrefix} · ")
                                                        append(resolveWatermarkPanelColorPaletteStyleTitle(selectedColorPaletteStyle, appLanguage))
                                                        selectedColorPaletteVariantLabel?.let {
                                                            append(" · ")
                                                            append(it)
                                                        }
                                                    },
                                                    fontSize = 11.sp,
                                                    color = secondaryTextColor
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val refreshFailureMessage =
                                                        if (selectedColorPaletteStyle == WatermarkColorPaletteStyle.ChinesePaletteMono) {
                                                            "刷新中国色失败"
                                                        } else {
                                                            "刷新取色失败"
                                                        }
                                                    if (selectedColorPaletteStyle == WatermarkColorPaletteStyle.ChinesePaletteMono) {
                                                        selectedColorPaletteVariantIndex += 1
                                                    } else {
                                                        colorPaletteRefreshNonce += 1
                                                    }
                                                    selectedUri?.let {
                                                        rerenderForCurrentSelection(it, refreshFailureMessage)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Refresh,
                                                    contentDescription = watermarkStrings.refreshColors,
                                                    tint = secondaryTextColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WatermarkColorPaletteStyle.entries.toList().chunked(2).forEach { rowStyles ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowStyles.forEach { style ->
                                                        val styleIndex = WatermarkColorPaletteStyle.entries.indexOf(style)
                                                        WatermarkCompactPresetCard(
                                                            title = resolveWatermarkPanelColorPaletteStyleTitle(style, appLanguage),
                                                            subtitle = resolveWatermarkPanelColorPaletteStyleSubtitle(style, appLanguage),
                                                            selected = selectedColorPaletteStyleIndex == styleIndex,
                                                            onClick = {
                                                                if (selectedColorPaletteStyleIndex != styleIndex) {
                                                                    selectedColorPaletteStyleIndex = styleIndex
                                                                    selectedColorPaletteVariantIndex = 0
                                                                    val normalizedCount = normalizeWatermarkPanelColorPaletteItemCount(
                                                                        style = style,
                                                                        variantIndex = 0,
                                                                        rawCount = colorPaletteElementCount
                                                                    )
                                                                    if (normalizedCount > 0) {
                                                                        colorPaletteElementCount = normalizedCount
                                                                        colorPaletteElementCountDraft = normalizedCount.toFloat()
                                                                    }
                                                                    selectedUri?.let {
                                                                        rerenderForCurrentSelection(it, watermarkExtraCopy.switchColorTemplateFailed)
                                                                    }
                                                                }
                                                            },
                                                            containerColor = panelBackground,
                                                            selectedContainerColor = panelSelectedBackground,
                                                            outlineColor = cardOutlineColor,
                                                            primaryTextColor = primaryTextColor,
                                                            secondaryTextColor = secondaryTextColor,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    if (rowStyles.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                        if (
                                            selectedColorPaletteVariantOptions.isNotEmpty() &&
                                            selectedColorPaletteStyle != WatermarkColorPaletteStyle.GlassDots
                                        ) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(watermarkStrings.substyle, fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                selectedColorPaletteVariantOptions.forEach { option ->
                                                    FilterChip(
                                                        selected = selectedColorPaletteVariantIndex == option.index,
                                                        onClick = {
                                                            if (selectedColorPaletteVariantIndex != option.index) {
                                                                selectedColorPaletteVariantIndex = option.index
                                                                val minimumCount = resolveWatermarkPanelColorPaletteMinimumCount(
                                                                    selectedColorPaletteStyle,
                                                                    option.index
                                                                )
                                                                val normalizedCount = normalizeWatermarkPanelColorPaletteItemCount(
                                                                    style = selectedColorPaletteStyle,
                                                                    variantIndex = option.index,
                                                                    rawCount = colorPaletteElementCount.coerceAtLeast(minimumCount)
                                                                )
                                                                if (normalizedCount > 0 && colorPaletteElementCount != normalizedCount) {
                                                                    colorPaletteElementCount = normalizedCount
                                                                    colorPaletteElementCountDraft = normalizedCount.toFloat()
                                                                }
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, "切换色彩子样式失败")
                                                                }
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                option.label,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = primaryTextColor
                                                            )
                                                        },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                        }
                                        if (selectedColorPaletteStyle == WatermarkColorPaletteStyle.HexCard) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(watermarkStrings.colorValueToggles, fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                WatermarkCompactSwitchCard(
                                                    title = "HEX",
                                                    checked = colorPaletteShowHexValue,
                                                    onCheckedChange = { checked ->
                                                        if (colorPaletteShowHexValue != checked) {
                                                            colorPaletteShowHexValue = checked
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(it, "更新 HEX 显示失败")
                                                            }
                                                        }
                                                    },
                                                    containerColor = panelBackground,
                                                    selectedContainerColor = panelSelectedBackground,
                                                    outlineColor = cardOutlineColor,
                                                    primaryTextColor = primaryTextColor,
                                                    secondaryTextColor = secondaryTextColor,
                                                    modifier = Modifier.weight(1f),
                                                    supportingText = watermarkStrings.hexValueSubtitle
                                                )
                                                WatermarkCompactSwitchCard(
                                                    title = "RGB",
                                                    checked = colorPaletteShowRgbValue,
                                                    onCheckedChange = { checked ->
                                                        if (colorPaletteShowRgbValue != checked) {
                                                            colorPaletteShowRgbValue = checked
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(it, "更新 RGB 显示失败")
                                                            }
                                                        }
                                                    },
                                                    containerColor = panelBackground,
                                                    selectedContainerColor = panelSelectedBackground,
                                                    outlineColor = cardOutlineColor,
                                                    primaryTextColor = primaryTextColor,
                                                    secondaryTextColor = secondaryTextColor,
                                                    modifier = Modifier.weight(1f),
                                                    supportingText = watermarkStrings.rgbValueSubtitle
                                                )
                                            }
                                        }
                                        if (activeCustomizePanel == WatermarkCustomizePanel.GlassDots &&
                                            selectedTemplate == WatermarkTemplatePreset.ColorPalette &&
                                            selectedColorPaletteStyle == WatermarkColorPaletteStyle.GlassDots
                                        ) {
                                            val glassDotsTwoRows = resolveWatermarkGlassDotsTwoRows(selectedColorPaletteVariantIndex)
                                            val glassDotsTone = resolveWatermarkGlassDotsPanelTone(selectedColorPaletteVariantIndex)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "玻璃面板",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("布局", fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(false, true).forEach { twoRows ->
                                                    val targetVariant = resolveWatermarkGlassDotsLayoutVariantIndex(twoRows, glassDotsTone)
                                                    FilterChip(
                                                        selected = glassDotsTwoRows == twoRows,
                                                        onClick = {
                                                            if (selectedColorPaletteVariantIndex != targetVariant) {
                                                                selectedColorPaletteVariantIndex = targetVariant
                                                                colorPaletteElementCount = normalizeWatermarkPanelColorPaletteItemCount(
                                                                    style = WatermarkColorPaletteStyle.GlassDots,
                                                                    variantIndex = targetVariant,
                                                                    rawCount = colorPaletteElementCount
                                                                )
                                                                colorPaletteElementCountDraft = colorPaletteElementCount.toFloat()
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, "切换玻璃色点布局失败")
                                                                }
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                resolveWatermarkGlassDotsLayoutLabel(twoRows, appLanguage),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = primaryTextColor
                                                            )
                                                        },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("玻璃颜色", fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                WatermarkGlassDotsPanelTone.entries.forEach { tone ->
                                                    val targetVariant = resolveWatermarkGlassDotsLayoutVariantIndex(glassDotsTwoRows, tone)
                                                    FilterChip(
                                                        selected = glassDotsTone == tone,
                                                        onClick = {
                                                            if (selectedColorPaletteVariantIndex != targetVariant) {
                                                                selectedColorPaletteVariantIndex = targetVariant
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, "切换玻璃颜色失败")
                                                                }
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                resolveWatermarkGlassDotsToneLabel(tone, appLanguage),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = primaryTextColor
                                                            )
                                                        },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("底部色号", fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            WatermarkCompactSwitchCard(
                                                title = "HEX",
                                                checked = colorPaletteShowHexValue,
                                                onCheckedChange = { checked ->
                                                    if (colorPaletteShowHexValue != checked) {
                                                        colorPaletteShowHexValue = checked
                                                        selectedColorPaletteVariantIndex = resolveEffectiveWatermarkGlassDotsVariantIndex(
                                                            variantIndex = selectedColorPaletteVariantIndex,
                                                            itemCount = colorPaletteElementCount,
                                                            showHexValue = checked
                                                        )
                                                        selectedUri?.let {
                                                            rerenderForCurrentSelection(it, "更新玻璃色点色号失败")
                                                        }
                                                    }
                                                },
                                                containerColor = panelBackground,
                                                selectedContainerColor = panelSelectedBackground,
                                                outlineColor = cardOutlineColor,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor,
                                                supportingText = "显示底部色号"
                                            )
                                        }
                                        if (supportsWatermarkPanelColorPaletteItemCount(selectedColorPaletteStyle)) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = watermarkStrings.barCount(colorPaletteElementCount.coerceAtLeast(selectedColorPaletteMinimumCount)),
                                                fontSize = 12.sp,
                                                color = primaryTextColor
                                            )
                                            Slider(
                                                value = colorPaletteElementCountDraft,
                                                onValueChange = { value ->
                                                    colorPaletteElementCountDraft = value
                                                },
                                                onValueChangeFinished = {
                                                    val resolvedCount = normalizeWatermarkPanelColorPaletteItemCount(
                                                        style = selectedColorPaletteStyle,
                                                        variantIndex = selectedColorPaletteVariantIndex,
                                                        rawCount = colorPaletteElementCountDraft.roundToInt()
                                                    )
                                                    colorPaletteElementCountDraft = resolvedCount.toFloat()
                                                    if (colorPaletteElementCount != resolvedCount) {
                                                        colorPaletteElementCount = resolvedCount
                                                        if (selectedColorPaletteStyle == WatermarkColorPaletteStyle.GlassDots) {
                                                            selectedColorPaletteVariantIndex = resolveEffectiveWatermarkGlassDotsVariantIndex(
                                                                variantIndex = selectedColorPaletteVariantIndex,
                                                                itemCount = resolvedCount,
                                                                showHexValue = colorPaletteShowHexValue
                                                            )
                                                        }
                                                        selectedUri?.let {
                                                            rerenderForCurrentSelection(it, "更新色彩条数失败")
                                                        }
                                                    }
                                                },
                                                valueRange = selectedColorPaletteMinimumCount.toFloat()..10f,
                                                steps = if (selectedColorPaletteStyle == WatermarkColorPaletteStyle.GlassDots) {
                                                    1
                                                } else {
                                                    (10 - selectedColorPaletteMinimumCount - 1).coerceAtLeast(0)
                                                }
                                            )
                                        }
                                        if (selectedColorPaletteStyle == WatermarkColorPaletteStyle.CircleBands) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(watermarkStrings.leftRightPosition, fontSize = 12.sp, color = primaryTextColor)
                                            Slider(
                                                value = colorPaletteHorizontalOffsetRatioDraft,
                                                onValueChange = { value ->
                                                    colorPaletteHorizontalOffsetRatioDraft = value
                                                },
                                                onValueChangeFinished = {
                                                    if (colorPaletteHorizontalOffsetRatio != colorPaletteHorizontalOffsetRatioDraft) {
                                                        colorPaletteHorizontalOffsetRatio = colorPaletteHorizontalOffsetRatioDraft
                                                        selectedUri?.let {
                                                            rerenderForCurrentSelection(it, "更新圆形色带左右位置失败")
                                                        }
                                                    }
                                                },
                                                valueRange = -1f..1f
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(watermarkStrings.upDownPosition, fontSize = 12.sp, color = primaryTextColor)
                                            Slider(
                                                value = colorPaletteVerticalOffsetRatioDraft,
                                                onValueChange = { value ->
                                                    colorPaletteVerticalOffsetRatioDraft = value
                                                },
                                                onValueChangeFinished = {
                                                    if (colorPaletteVerticalOffsetRatio != colorPaletteVerticalOffsetRatioDraft) {
                                                        colorPaletteVerticalOffsetRatio = colorPaletteVerticalOffsetRatioDraft
                                                        selectedUri?.let {
                                                            rerenderForCurrentSelection(it, "更新圆形色带高低位置失败")
                                                        }
                                                    }
                                                },
                                                valueRange = -1f..1f
                                            )
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.GlassDots
                                        && selectedTemplate == WatermarkTemplatePreset.ColorPalette
                                        && selectedColorPaletteStyle == WatermarkColorPaletteStyle.GlassDots
                                    ) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "玻璃面板",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = primaryTextColor
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val glassDotsTwoRows = resolveWatermarkGlassDotsTwoRows(selectedColorPaletteVariantIndex)
                                        val glassDotsTone = resolveWatermarkGlassDotsPanelTone(selectedColorPaletteVariantIndex)
                                        Text("布局", fontSize = 12.sp, color = primaryTextColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(false, true).forEach { twoRows ->
                                                val targetVariant = resolveWatermarkGlassDotsLayoutVariantIndex(twoRows, glassDotsTone)
                                                FilterChip(
                                                    selected = glassDotsTwoRows == twoRows,
                                                    onClick = {
                                                        if (selectedColorPaletteVariantIndex != targetVariant) {
                                                            selectedColorPaletteVariantIndex = targetVariant
                                                            colorPaletteElementCount = normalizeWatermarkPanelColorPaletteItemCount(
                                                                style = WatermarkColorPaletteStyle.GlassDots,
                                                                variantIndex = targetVariant,
                                                                rawCount = colorPaletteElementCount
                                                            )
                                                            colorPaletteElementCountDraft = colorPaletteElementCount.toFloat()
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(it, "切换玻璃色点布局失败")
                                                            }
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            resolveWatermarkGlassDotsLayoutLabel(twoRows, appLanguage),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = primaryTextColor
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("玻璃颜色", fontSize = 12.sp, color = primaryTextColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WatermarkGlassDotsPanelTone.entries.forEach { tone ->
                                                val targetVariant = resolveWatermarkGlassDotsLayoutVariantIndex(glassDotsTwoRows, tone)
                                                FullColorSwatchChoice(
                                                    color = Color(resolveWatermarkGlassDotsPanelToneSwatchColor(tone)),
                                                    selected = glassDotsTone == tone,
                                                    outlineColor = cardOutlineColor,
                                                    selectedColor = primaryTextColor,
                                                    onClick = {
                                                        if (selectedColorPaletteVariantIndex != targetVariant) {
                                                            selectedColorPaletteVariantIndex = targetVariant
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(it, "切换玻璃颜色失败")
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("底部色号", fontSize = 12.sp, color = primaryTextColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        WatermarkCompactSwitchCard(
                                            title = "HEX",
                                            checked = colorPaletteShowHexValue,
                                            onCheckedChange = { checked ->
                                                if (colorPaletteShowHexValue != checked) {
                                                    colorPaletteShowHexValue = checked
                                                    selectedColorPaletteVariantIndex = resolveEffectiveWatermarkGlassDotsVariantIndex(
                                                        variantIndex = selectedColorPaletteVariantIndex,
                                                        itemCount = colorPaletteElementCount,
                                                        showHexValue = checked
                                                    )
                                                    selectedUri?.let {
                                                        rerenderForCurrentSelection(it, "更新玻璃色点色号失败")
                                                    }
                                                }
                                            },
                                            containerColor = panelBackground,
                                            selectedContainerColor = panelSelectedBackground,
                                            outlineColor = cardOutlineColor,
                                            primaryTextColor = primaryTextColor,
                                            secondaryTextColor = secondaryTextColor,
                                            supportingText = "显示底部色号"
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("色点数量", fontSize = 12.sp, color = primaryTextColor)
                                        Slider(
                                            value = colorPaletteElementCountDraft,
                                            onValueChange = { value -> colorPaletteElementCountDraft = value },
                                            onValueChangeFinished = {
                                                val resolvedCount = normalizeWatermarkPanelColorPaletteItemCount(
                                                    style = selectedColorPaletteStyle,
                                                    variantIndex = selectedColorPaletteVariantIndex,
                                                    rawCount = colorPaletteElementCountDraft.roundToInt()
                                                )
                                                colorPaletteElementCountDraft = resolvedCount.toFloat()
                                                if (colorPaletteElementCount != resolvedCount) {
                                                    colorPaletteElementCount = resolvedCount
                                                    selectedColorPaletteVariantIndex = resolveEffectiveWatermarkGlassDotsVariantIndex(
                                                        variantIndex = selectedColorPaletteVariantIndex,
                                                        itemCount = resolvedCount,
                                                        showHexValue = colorPaletteShowHexValue
                                                    )
                                                    selectedUri?.let {
                                                        rerenderForCurrentSelection(it, "更新玻璃色点数量失败")
                                                    }
                                                }
                                            },
                                            valueRange = selectedColorPaletteMinimumCount.toFloat()..10f,
                                            steps = 1
                                        )
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.PaperDotsLayout
                                        && selectedTemplate == WatermarkTemplatePreset.PaperDots
                                    ) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "布局",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = primaryTextColor
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("分屏方向", fontSize = 12.sp, color = primaryTextColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WatermarkPaperDotsLayoutDirection.entries.forEach { direction ->
                                                FilterChip(
                                                    selected = paperDotsLayoutDirection == direction,
                                                    onClick = {
                                                        if (paperDotsLayoutDirection != direction) {
                                                            paperDotsLayoutDirection = direction
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "切换波点方向失败") }
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            direction.localizedTitle(appLanguage),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = primaryTextColor
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        WatermarkCompactSliderCard(
                                            title = "分屏比例",
                                            subtitle = "和目标站一致，控制两块等尺寸面板各自显示多少",
                                            containerColor = panelBackground,
                                            outlineColor = cardOutlineColor,
                                            primaryTextColor = primaryTextColor,
                                            secondaryTextColor = secondaryTextColor
                                        ) {
                                            Slider(
                                                value = paperDotsSplitRatioDraft,
                                                onValueChange = { value -> paperDotsSplitRatioDraft = value },
                                                onValueChangeFinished = {
                                                    val resolved = paperDotsSplitRatioDraft.coerceIn(0f, 1f)
                                                    paperDotsSplitRatioDraft = resolved
                                                    if (paperDotsSplitRatio != resolved) {
                                                        paperDotsSplitRatio = resolved
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新波点分屏比例失败") }
                                                    }
                                                },
                                                valueRange = 0f..1f
                                            )
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.PaperDotsBackground
                                        && selectedTemplate == WatermarkTemplatePreset.PaperDots
                                    ) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "底图",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = primaryTextColor
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("背景样式", fontSize = 12.sp, color = primaryTextColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WatermarkPaperDotsBackgroundMode.entries.forEach { mode ->
                                                FilterChip(
                                                    selected = paperDotsBackgroundMode == mode,
                                                    onClick = {
                                                        if (paperDotsBackgroundMode != mode) {
                                                            paperDotsBackgroundMode = mode
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "切换波点背景失败") }
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            mode.localizedTitle(appLanguage),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = primaryTextColor
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (paperDotsBackgroundMode == WatermarkPaperDotsBackgroundMode.Photo) {
                                                "铺底色调"
                                            } else {
                                                "主色"
                                            },
                                            fontSize = 12.sp,
                                            color = primaryTextColor
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WATERMARK_PAPER_DOTS_COLOR_OPTIONS.forEachIndexed { index, option ->
                                                val showSwatchBorder = option.androidColor == AndroidColor.WHITE
                                                FilterChip(
                                                    selected = paperDotsBackgroundColorIndex == index,
                                                    onClick = {
                                                        if (paperDotsBackgroundColorIndex != index) {
                                                            paperDotsBackgroundColorIndex = index
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "更新波点主色失败") }
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            option.label,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = primaryTextColor
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .then(
                                                                    if (showSwatchBorder) {
                                                                        Modifier.border(
                                                                            width = 0.8.dp,
                                                                            color = if (isDark) {
                                                                                Color.White.copy(alpha = 0.56f)
                                                                            } else {
                                                                                Color.Black.copy(alpha = 0.36f)
                                                                            },
                                                                            shape = RoundedCornerShape(5.dp)
                                                                        )
                                                                    } else {
                                                                        Modifier
                                                                    }
                                                                )
                                                                .background(option.composeColor)
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                        if (paperDotsBackgroundMode == WatermarkPaperDotsBackgroundMode.DuoTone) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("副色", fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                WATERMARK_PAPER_DOTS_COLOR_OPTIONS.forEachIndexed { index, option ->
                                                    val showSwatchBorder = option.androidColor == AndroidColor.WHITE
                                                    FilterChip(
                                                        selected = paperDotsBackgroundSecondaryColorIndex == index,
                                                        onClick = {
                                                            if (paperDotsBackgroundSecondaryColorIndex != index) {
                                                                paperDotsBackgroundSecondaryColorIndex = index
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "更新波点副色失败") }
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                option.label,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = primaryTextColor
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(10.dp)
                                                                    .clip(RoundedCornerShape(5.dp))
                                                                    .then(
                                                                        if (showSwatchBorder) {
                                                                            Modifier.border(
                                                                                width = 0.8.dp,
                                                                                color = if (isDark) {
                                                                                    Color.White.copy(alpha = 0.56f)
                                                                                } else {
                                                                                    Color.Black.copy(alpha = 0.36f)
                                                                                },
                                                                                shape = RoundedCornerShape(5.dp)
                                                                            )
                                                                        } else {
                                                                            Modifier
                                                                        }
                                                                    )
                                                                    .background(option.composeColor)
                                                            )
                                                        },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            WatermarkCompactSliderCard(
                                                title = "条纹粗细",
                                                subtitle = "双色背景下控制纸面的条纹宽度",
                                                containerColor = panelBackground,
                                                outlineColor = cardOutlineColor,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor
                                            ) {
                                                Slider(
                                                    value = paperDotsStripeSizeRatioDraft,
                                                    onValueChange = { value -> paperDotsStripeSizeRatioDraft = value },
                                                    onValueChangeFinished = {
                                                        val resolved = paperDotsStripeSizeRatioDraft.coerceIn(0.015f, 0.12f)
                                                        paperDotsStripeSizeRatioDraft = resolved
                                                        if (paperDotsStripeSizeRatio != resolved) {
                                                            paperDotsStripeSizeRatio = resolved
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(it, "更新波点条纹粗细失败")
                                                            }
                                                        }
                                                    },
                                                    valueRange = 0.015f..0.12f
                                                )
                                            }
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.PaperDotsDots
                                        && selectedTemplate == WatermarkTemplatePreset.PaperDots
                                    ) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "波点",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                            IconButton(
                                                onClick = {
                                                    paperDotsRandomSeed = nextWatermarkPaperDotsSeed()
                                                    selectedUri?.let { rerenderForCurrentSelection(it, "重排波点失败") }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Refresh,
                                                    contentDescription = "重排波点",
                                                    tint = secondaryTextColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("波点形状", fontSize = 12.sp, color = primaryTextColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WatermarkPaperDotsDotShape.entries.forEach { shape ->
                                                FilterChip(
                                                    selected = paperDotsShape == shape,
                                                    onClick = {
                                                        if (paperDotsShape != shape) {
                                                            paperDotsShape = shape
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "切换波点形状失败") }
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            shape.localizedTitle(appLanguage),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = primaryTextColor
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                        if (paperDotsShape == WatermarkPaperDotsDotShape.Text) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedTextField(
                                                value = paperDotsPatternText,
                                                onValueChange = { value ->
                                                    val sanitized = sanitizeWatermarkPaperDotsPatternText(value)
                                                    if (paperDotsPatternText != sanitized) {
                                                        paperDotsPatternText = sanitized
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新文字图案失败") }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                label = { Text("图案文字", color = secondaryTextColor) },
                                                supportingText = {
                                                    Text("可输入 1-8 个字符，重复生成文字纸孔", color = secondaryTextColor)
                                                },
                                                colors = dialogTextFieldColors
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        WatermarkCompactSliderCard(
                                            title = "波点大小",
                                            subtitle = "控制纸孔与打孔区域的基础尺寸",
                                            containerColor = panelBackground,
                                            outlineColor = cardOutlineColor,
                                            primaryTextColor = primaryTextColor,
                                            secondaryTextColor = secondaryTextColor
                                        ) {
                                            Slider(
                                                value = paperDotsDotSizeDraft,
                                                onValueChange = { value -> paperDotsDotSizeDraft = value },
                                                onValueChangeFinished = {
                                                    val resolved = paperDotsDotSizeDraft.coerceIn(12f, 72f)
                                                    paperDotsDotSizeDraft = resolved
                                                    if (paperDotsDotSize != resolved) {
                                                        paperDotsDotSize = resolved
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新波点大小失败") }
                                                    }
                                                },
                                                valueRange = 12f..72f
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        WatermarkCompactSliderCard(
                                            title = "随机扰动",
                                            subtitle = "数值越高，波点大小起伏越明显",
                                            containerColor = panelBackground,
                                            outlineColor = cardOutlineColor,
                                            primaryTextColor = primaryTextColor,
                                            secondaryTextColor = secondaryTextColor
                                        ) {
                                            Slider(
                                                value = paperDotsDotVarianceDraft,
                                                onValueChange = { value -> paperDotsDotVarianceDraft = value },
                                                onValueChangeFinished = {
                                                    val resolved = paperDotsDotVarianceDraft.coerceIn(0f, 100f)
                                                    paperDotsDotVarianceDraft = resolved
                                                    if (paperDotsDotVariance != resolved) {
                                                        paperDotsDotVariance = resolved
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新波点扰动失败") }
                                                    }
                                                },
                                                valueRange = 0f..100f
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        WatermarkCompactSliderCard(
                                            title = "波点数量",
                                            subtitle = "控制纸面与图片里的打孔密度",
                                            containerColor = panelBackground,
                                            outlineColor = cardOutlineColor,
                                            primaryTextColor = primaryTextColor,
                                            secondaryTextColor = secondaryTextColor
                                        ) {
                                            Slider(
                                                value = paperDotsDotCountDraft,
                                                onValueChange = { value -> paperDotsDotCountDraft = value },
                                                onValueChangeFinished = {
                                                    val resolved = paperDotsDotCountDraft.roundToInt().coerceIn(6, 60)
                                                    paperDotsDotCountDraft = resolved.toFloat()
                                                    if (paperDotsDotCount != resolved) {
                                                        paperDotsDotCount = resolved
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新波点数量失败") }
                                                    }
                                                },
                                                valueRange = 6f..60f,
                                                steps = 53
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("边框样式", fontSize = 12.sp, color = primaryTextColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WatermarkPaperDotsStrokeMode.entries.forEach { mode ->
                                                FilterChip(
                                                    selected = paperDotsStrokeMode == mode,
                                                    onClick = {
                                                        if (paperDotsStrokeMode != mode) {
                                                            paperDotsStrokeMode = mode
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "切换边框样式失败") }
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            mode.localizedTitle(appLanguage),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = primaryTextColor
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                        if (paperDotsStrokeMode != WatermarkPaperDotsStrokeMode.AllFill) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            WatermarkCompactSliderCard(
                                                title = "边框粗细",
                                                subtitle = "控制边框波点的轮廓线条宽度",
                                                containerColor = panelBackground,
                                                outlineColor = cardOutlineColor,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor
                                            ) {
                                                Slider(
                                                    value = paperDotsStrokeWidthRatioDraft,
                                                    onValueChange = { value -> paperDotsStrokeWidthRatioDraft = value },
                                                    onValueChangeFinished = {
                                                        val resolved = paperDotsStrokeWidthRatioDraft.coerceIn(0.02f, 0.25f)
                                                        paperDotsStrokeWidthRatioDraft = resolved
                                                        if (paperDotsStrokeWidthRatio != resolved) {
                                                            paperDotsStrokeWidthRatio = resolved
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "更新边框粗细失败") }
                                                        }
                                                    },
                                                    valueRange = 0.02f..0.25f
                                                )
                                            }
                                        }
                                        if (paperDotsStrokeMode == WatermarkPaperDotsStrokeMode.Mix) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            WatermarkCompactSliderCard(
                                                title = "混合比例",
                                                subtitle = "数值越高，边框样式的波点占比越多",
                                                containerColor = panelBackground,
                                                outlineColor = cardOutlineColor,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor
                                            ) {
                                                Slider(
                                                    value = paperDotsStrokeMixRatioDraft,
                                                    onValueChange = { value -> paperDotsStrokeMixRatioDraft = value },
                                                    onValueChangeFinished = {
                                                        val resolved = paperDotsStrokeMixRatioDraft.coerceIn(0f, 1f)
                                                        paperDotsStrokeMixRatioDraft = resolved
                                                        if (paperDotsStrokeMixRatio != resolved) {
                                                            paperDotsStrokeMixRatio = resolved
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "更新混合比例失败") }
                                                        }
                                                    },
                                                    valueRange = 0f..1f
                                                )
                                            }
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.Crop) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        if (sourceUri == null) {
                                            Text(watermarkStrings.cropNeedImage, fontSize = 12.sp, color = secondaryTextColor)
                                        } else if (selectedTemplate in setOf(
                                                WatermarkTemplatePreset.FilmRetro,
                                                WatermarkTemplatePreset.Polaroid,
                                                WatermarkTemplatePreset.ColorPalette,
                                                WatermarkTemplatePreset.PaperDots
                                            )
                                        ) {
                                            Text(watermarkStrings.cropUnsupported, fontSize = 12.sp, color = secondaryTextColor)
                                        } else {
                                            val openCropEditor: (SocialCropPreset?) -> Unit = { preset ->
                                                scope.launch {
                                                    errorMessage = null
                                                    showLoadingOverlay = false
                                                    try {
                                                        drawerState.close()
                                                    } catch (_: Throwable) {
                                                    }
                                                    runCatching {
                                                        val cropDecodeMaxSide = computeRenderDecodeMaxSide(
                                                            resolveOutputSizeOverride(selectedTemplate, preset)
                                                        )
                                                        val bitmap = withContext(Dispatchers.Default) {
                                                            decodeSourceBitmap(
                                                                context = context,
                                                                uri = sourceUri,
                                                                requestedMaxSidePx = cropDecodeMaxSide
                                                            )
                                                        }
                                                        val old = cropSourceBitmap
                                                        if (old != null && old !== bitmap) {
                                                            runCatching { old.recycle() }
                                                        }
                                                        cropSourceBitmap = bitmap
                                                        cropEditorPresetKey = preset?.key
                                                        cropEditorAspectRatio = if (preset != null) {
                                                            resolveCropAspectRatioForTemplate(
                                                                template = selectedTemplate,
                                                                preset = preset,
                                                                frameSideTopBorderRatio = frameSideTopBorderRatio,
                                                                colorPaletteStyle = selectedColorPaletteStyle,
                                                                colorPaletteVariantIndex = selectedColorPaletteVariantIndex,
                                                                colorPaletteShowHexValue = colorPaletteShowHexValue,
                                                                colorPaletteShowRgbValue = colorPaletteShowRgbValue
                                                            )
                                                        } else {
                                                            bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                                                        }
                                                        cropCenterX = bitmap.width / 2f
                                                        cropCenterY = bitmap.height / 2f
                                                        cropZoom = 1f
                                                        cropEditorUndoHistory.clear()
                                                        currentStep = WatermarkFlowStep.Crop
                                                        showLoadingOverlay = false
                                                    }.onFailure { error ->
                                                        errorMessage = error.message ?: watermarkExtraCopy.openCropFailed
                                                    }
                                                }
                                            }
                                            Text(
                                                text = watermarkStrings.quickCrop,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                WatermarkQuickCropOptionCard(
                                                    title = resolveWatermarkUILabel(appLanguage, "自由"),
                                                    ratioLabel = resolveWatermarkUILabel(appLanguage, "任意"),
                                                    selected = activeCropSourceFile != null && selectedSocialCropPresetKey == null,
                                                    accentColor = if (isDark) Color(0xFFFFD85A) else Color(0xFFF5C400),
                                                    cardColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF3F2F0),
                                                    borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                                    titleColor = primaryTextColor,
                                                    subtitleColor = secondaryTextColor,
                                                    icon = Icons.Outlined.CropOriginal,
                                                    onClick = { openCropEditor(null) }
                                                )
                                                SOCIAL_CROP_PRESETS.forEach { preset ->
                                                    WatermarkQuickCropOptionCard(
                                                        title = resolveSocialCropLabel(appLanguage, preset.key),
                                                        ratioLabel = preset.ratioLabel,
                                                        selected = selectedSocialCropPresetKey == preset.key,
                                                        accentColor = if (isDark) Color(0xFFFFD85A) else Color(0xFFF5C400),
                                                        cardColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF3F2F0),
                                                        borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                                        titleColor = primaryTextColor,
                                                        subtitleColor = secondaryTextColor,
                                                        icon = watermarkQuickCropPresetIcon(preset.ratioLabel),
                                                        onClick = { openCropEditor(preset) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.Copyright) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        WatermarkCopyrightPanel(
                                            state = copyrightState,
                                            showIdentityFields = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                returnCustomizePanelKeyFromCopyright = activeCustomizePanel.name
                                                currentStep = WatermarkFlowStep.CopyrightProfile
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp),
                                            colors = actionButtonColors
                                        ) {
                                            Text(watermarkStrings.configureCopyright, fontSize = 12.sp, color = primaryTextColor)
                                        }
                                    }

                                if ((activeCustomizePanel == WatermarkCustomizePanel.Logo
                                        || activeCustomizePanel == WatermarkCustomizePanel.Params
                                        || activeCustomizePanel == WatermarkCustomizePanel.Signature
                                        || activeCustomizePanel == WatermarkCustomizePanel.Border)
                                    && (
                                        templateSupportsCustomControls ||
                                            supportsSignaturePanel ||
                                            templateSupportsBorderControls ||
                                            isCutoutBreakoutMode ||
                                            isSubjectOutlineMode
                                        )
                                ) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (templateSupportsCustomControls) {
                                        if (activeCustomizePanel == WatermarkCustomizePanel.Logo) {
                                            Text(
                                                text = watermarkStrings.logoSectionTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(watermarkStrings.brandLogo, fontSize = 12.sp, color = primaryTextColor)
                                                Switch(
                                                    checked = showLogoWatermark,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            disableSignatureIfNeeded()
                                                            if (selectedBrandKey == null && detectedBrandKey == null) {
                                                                applyPreferredVariantForBrand("photoo")
                                                            }
                                                        }
                                                        showLogoWatermark = checked
                                                        selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                    }
                                                )
                                            }
                                            if (showLogoWatermark) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                WatermarkCompactSliderCard(
                                                    title = watermarkStrings.logoSize,
                                                    subtitle = watermarkStrings.logoSizeSubtitle,
                                                    containerColor = panelBackground,
                                                    outlineColor = cardOutlineColor,
                                                    primaryTextColor = primaryTextColor,
                                                    secondaryTextColor = secondaryTextColor
                                                ) {
                                                    Slider(
                                                        value = logoScaleFactorDraft,
                                                        onValueChange = { value -> logoScaleFactorDraft = value },
                                                        onValueChangeFinished = {
                                                            if (logoScaleFactor != logoScaleFactorDraft) {
                                                                logoScaleFactor = logoScaleFactorDraft
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "更新 Logo 大小失败") }
                                                            }
                                                        },
                                                        valueRange = 0.72f..1.85f
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(watermarkStrings.brand, fontSize = 12.sp, color = primaryTextColor)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val selectableManualBrandKeys = sessionManualBrandKeys
                                                    .filter { key ->
                                                        key != "photoo" &&
                                                            key != CUSTOM_LOGO_BRAND_KEY &&
                                                            BRAND_LOGO_GROUP_MAP.containsKey(key)
                                                    }
                                                    .distinct()
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    FilterChip(
                                                        selected = selectedBrandKey == null,
                                                        onClick = {
                                                            selectedBrandKey = null
                                                            applyPreferredVariantForBrand(resolveEffectiveBrandKey())
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "应用品牌失败") }
                                                        },
                                                        label = { Text(watermarkStrings.automatic) },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                    FilterChip(
                                                        selected = selectedBrandKey == "photoo",
                                                        onClick = {
                                                            selectedBrandKey = "photoo"
                                                            applyPreferredVariantForBrand("photoo")
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "应用品牌失败") }
                                                        },
                                                        label = { Text("Photoo") },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                    FilterChip(
                                                        selected = selectedBrandKey == CUSTOM_LOGO_BRAND_KEY,
                                                        onClick = {
                                                            selectedBrandKey = CUSTOM_LOGO_BRAND_KEY
                                                            if (customLogoPath.isBlank() || !File(customLogoPath).exists()) {
                                                                customLogoPickerLauncher.launch("image/*")
                                                            } else {
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "应用品牌失败") }
                                                            }
                                                        },
                                                        label = { Text(watermarkStrings.custom) },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                    selectableManualBrandKeys.forEach { key ->
                                                        val label = BRAND_LOGO_GROUP_MAP[key]?.label ?: key
                                                        FilterChip(
                                                            selected = selectedBrandKey == key,
                                                            onClick = {
                                                                selectedBrandKey = key
                                                                applyPreferredVariantForBrand(key)
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "应用品牌失败") }
                                                            },
                                                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                            border = null,
                                                            colors = chipColors
                                                        )
                                                    }
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            brandCorrectionDraftKey = (detectedBrandKey
                                                                ?.takeIf { BRAND_LOGO_GROUP_MAP.containsKey(it) }
                                                                ?: "photoo")
                                                            isBrandCorrectionDialogVisible = true
                                                        },
                                                        label = { Text(resolveWatermarkUILabel(appLanguage, "识别有误？")) },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                                if (selectedBrandKey == CUSTOM_LOGO_BRAND_KEY) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    TextButton(onClick = { customLogoPickerLauncher.launch("image/*") }) {
                                                        Text("更换自定义 Logo", color = primaryTextColor)
                                                    }
                                                } else {
                                                    val currentLogoBrandGroup = resolveEffectiveBrandKey()
                                                        ?.let { BRAND_LOGO_GROUP_MAP[it] }
                                                    if (currentLogoBrandGroup != null && currentLogoBrandGroup.variants.size > 1) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text("Logo 样式", fontSize = 12.sp, color = primaryTextColor)
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Row(
                                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            currentLogoBrandGroup.variants.forEachIndexed { index, variant ->
                                                                FilterChip(
                                                                    selected = selectedVariantIndex == index,
                                                                    onClick = {
                                                                        if (selectedVariantIndex != index) {
                                                                            selectedVariantIndex = index
                                                                            selectedUri?.let {
                                                                                rerenderForCurrentSelection(it, "切换 Logo 样式失败")
                                                                            }
                                                                        }
                                                                    },
                                                                    label = {
                                                                        Text(
                                                                            localizeVariantLabel(variant.label),
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    },
                                                                    border = null,
                                                                    colors = chipColors
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                        // ThinWhiteBorder special modes still edit through Params
                                        // even when the template itself has no logo controls.
                                        if (activeCustomizePanel == WatermarkCustomizePanel.Params) {
                                            if (isCutoutBreakoutMode) {
                                                val selectedCutoutBreakoutBackgroundColorIndex =
                                                    resolveWatermarkCutoutBreakoutBackgroundColorIndex(thinBorderColorIndex)
                                                Text(
                                                    text = watermarkCutoutBreakoutPanelTitle(appLanguage),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = primaryTextColor
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = watermarkCutoutBreakoutBackgroundColorLabel(appLanguage),
                                                    fontSize = 12.sp,
                                                    color = primaryTextColor
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    WATERMARK_CUTOUT_BREAKOUT_BACKGROUND_OPTIONS.forEach { (index, option) ->
                                                        val showSwatchBorder = option.android == AndroidColor.WHITE
                                                        FilterChip(
                                                            selected = selectedCutoutBreakoutBackgroundColorIndex == index,
                                                            onClick = {
                                                                thinBorderColorIndex = index
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "鏇存柊杈规棰滆壊澶辫触"
                                                                    )
                                                                }
                                                            },
                                                            label = { Text(option.label) },
                                                            leadingIcon = {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(10.dp)
                                                                        .clip(RoundedCornerShape(5.dp))
                                                                        .then(
                                                                            if (showSwatchBorder) {
                                                                                Modifier.border(
                                                                                    width = 0.8.dp,
                                                                                    color = if (isDark) {
                                                                                        Color.White.copy(alpha = 0.56f)
                                                                                    } else {
                                                                                        Color.Black.copy(alpha = 0.36f)
                                                                                    },
                                                                                    shape = RoundedCornerShape(5.dp)
                                                                                )
                                                                            } else {
                                                                                Modifier
                                                                            }
                                                                        )
                                                                        .background(option.compose)
                                                                )
                                                            },
                                                            border = null,
                                                            colors = chipColors
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = watermarkCutoutBreakoutFrameRatioLabel(appLanguage),
                                                    fontSize = 12.sp,
                                                    color = primaryTextColor
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    WatermarkCutoutFrameAspectRatio.entries.forEach { ratio ->
                                                        FilterChip(
                                                            selected = cutoutFrameAspectRatio == ratio,
                                                            onClick = {
                                                                cutoutFrameAspectRatio = ratio
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "更新冲框外框比例"
                                                                    )
                                                                }
                                                            },
                                                            label = { Text(ratio.label) },
                                                            border = null,
                                                            colors = chipColors
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = watermarkCutoutBreakoutSubjectScaleLabel(appLanguage),
                                                    fontSize = 12.sp,
                                                    color = primaryTextColor
                                                )
                                                Slider(
                                                    value = cutoutSubjectScaleFactorDraft,
                                                    onValueChange = { value ->
                                                        cutoutSubjectScaleFactorDraft = value
                                                            .coerceIn(
                                                                MIN_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
                                                                MAX_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
                                                            )
                                                    },
                                                    onValueChangeFinished = {
                                                        if (kotlin.math.abs(cutoutSubjectScaleFactor - cutoutSubjectScaleFactorDraft) > 0.0005f) {
                                                            cutoutSubjectScaleFactor = cutoutSubjectScaleFactorDraft
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(
                                                                    it,
                                                                    "鏇存柊鍑虹暀鍔涘害澶辫触"
                                                                )
                                                            }
                                                        }
                                                    },
                                                    valueRange = MIN_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR..MAX_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
                                                )
                                            } else if (isSubjectOutlineMode) {
                                                WatermarkSubjectOutlinePanel(
                                                    language = appLanguage,
                                                    labels = WatermarkSubjectOutlinePanelLabels(
                                                        borderCorner = watermarkStrings.borderCorner,
                                                        imageRatio = watermarkStrings.imageRatio
                                                    ),
                                                    state = WatermarkSubjectOutlinePanelState(
                                                        outerBorderEnabled = subjectOutlineOuterBorderEnabled,
                                                        borderColorIndex = thinBorderColorIndex,
                                                        borderCornerMode = borderCornerMode,
                                                        borderImageScaleFactorDraft = borderImageScaleFactorDraft,
                                                        lineStyle = subjectOutlineLineStyle,
                                                        offsetRatioDraft = subjectOutlineOffsetRatioDraft,
                                                        widthRatioDraft = subjectOutlineWidthRatioDraft,
                                                        symbolText = subjectOutlineSymbolText,
                                                        symbolSizeRatioDraft = subjectOutlineSymbolSizeRatioDraft,
                                                        outlineColorIndex = subjectOutlineColorIndex
                                                    ),
                                                    colors = WatermarkSubjectOutlinePanelColors(
                                                        panelBackground = panelBackground,
                                                        panelSelectedBackground = panelSelectedBackground,
                                                        cardOutlineColor = cardOutlineColor,
                                                        primaryTextColor = primaryTextColor,
                                                        secondaryTextColor = secondaryTextColor,
                                                        isDark = isDark
                                                    ),
                                                    callbacks = WatermarkSubjectOutlinePanelCallbacks(
                                                        onOuterBorderEnabledChange = { checked ->
                                                            if (subjectOutlineOuterBorderEnabled != checked) {
                                                                subjectOutlineOuterBorderEnabled = checked
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline image border"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onBorderColorIndexChange = { index ->
                                                            thinBorderColorIndex = index
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(
                                                                    it,
                                                                    "Failed to update subject outline image border color"
                                                                )
                                                            }
                                                        },
                                                        onBorderCornerModeChange = { mode ->
                                                            if (borderCornerMode != mode) {
                                                                borderCornerMode = mode
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline image border corner"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onBorderImageScaleFactorDraftChange = { value ->
                                                            borderImageScaleFactorDraft = value
                                                        },
                                                        onBorderImageScaleFactorCommit = {
                                                            if (borderImageScaleFactor != borderImageScaleFactorDraft) {
                                                                borderImageScaleFactor = borderImageScaleFactorDraft
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline image scale"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onLineStyleChange = { lineStyle ->
                                                            if (subjectOutlineLineStyle != lineStyle) {
                                                                subjectOutlineLineStyle = lineStyle
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline style"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onOffsetRatioDraftChange = { value ->
                                                            subjectOutlineOffsetRatioDraft = value
                                                        },
                                                        onOffsetRatioCommit = {
                                                            if (subjectOutlineOffsetRatio != subjectOutlineOffsetRatioDraft) {
                                                                subjectOutlineOffsetRatio = subjectOutlineOffsetRatioDraft
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline distance"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onWidthRatioDraftChange = { value ->
                                                            subjectOutlineWidthRatioDraft = value
                                                        },
                                                        onWidthRatioCommit = {
                                                            if (subjectOutlineWidthRatio != subjectOutlineWidthRatioDraft) {
                                                                subjectOutlineWidthRatio = subjectOutlineWidthRatioDraft
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline thickness"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onSymbolTextChange = { value ->
                                                            val sanitized = sanitizeWatermarkSubjectOutlineSymbolText(value)
                                                            if (subjectOutlineSymbolText != sanitized) {
                                                                subjectOutlineSymbolText = sanitized
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline symbol"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onSymbolSizeRatioDraftChange = { value ->
                                                            subjectOutlineSymbolSizeRatioDraft = value
                                                        },
                                                        onSymbolSizeRatioCommit = {
                                                            if (subjectOutlineSymbolSizeRatio != subjectOutlineSymbolSizeRatioDraft) {
                                                                subjectOutlineSymbolSizeRatio = subjectOutlineSymbolSizeRatioDraft
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        "Failed to update subject outline symbol size"
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onOutlineColorIndexChange = { index ->
                                                            subjectOutlineColorIndex = index
                                                            selectedUri?.let {
                                                                rerenderForCurrentSelection(
                                                                    it,
                                                                    "Failed to update subject outline color"
                                                                )
                                                            }
                                                        }
                                                    )
                                                )
                                            } else {
                                            Text(
                                                text = watermarkExtraCopy.params,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (isFullColorTemplate) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = if (fullColorUseGradientBackground) {
                                                            fullColorUiCopy.gradientColorLabel
                                                        } else {
                                                            resolveFullColorBackgroundColorLabel(appLanguage)
                                                        },
                                                        fontSize = 12.sp,
                                                        color = primaryTextColor
                                                    )
                                                    FullColorBackgroundColorPicker(
                                                        language = appLanguage,
                                                        useGradientBackground = fullColorUseGradientBackground,
                                                        selectedColorIndex = fullColorBackgroundColorIndex,
                                                        selectedGradientPresetIndex = fullColorGradientPresetIndex,
                                                        customColor = fullColorCustomBackgroundColor,
                                                        containerColor = panelBackground,
                                                        selectedContainerColor = panelSelectedBackground,
                                                        outlineColor = cardOutlineColor,
                                                        primaryTextColor = primaryTextColor,
                                                        secondaryTextColor = secondaryTextColor,
                                                        onSelectAuto = {
                                                            val shouldRender = if (fullColorUseGradientBackground) {
                                                                fullColorGradientPresetIndex >= 0
                                                            } else {
                                                                fullColorBackgroundColorIndex != FULL_COLOR_AUTO_BACKGROUND_INDEX
                                                            }
                                                            if (fullColorUseGradientBackground) {
                                                                fullColorGradientPresetIndex = -1
                                                            } else {
                                                                fullColorBackgroundColorIndex = FULL_COLOR_AUTO_BACKGROUND_INDEX
                                                            }
                                                            if (shouldRender) {
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed)
                                                                }
                                                            }
                                                        },
                                                        onSelectPreset = { index ->
                                                            if (fullColorBackgroundColorIndex != index) {
                                                                fullColorBackgroundColorIndex = index
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed)
                                                                }
                                                            }
                                                        },
                                                        onSelectGradientPreset = { index ->
                                                            if (fullColorGradientPresetIndex != index) {
                                                                fullColorGradientPresetIndex = index
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed)
                                                                }
                                                            }
                                                        },
                                                        onOpenCustomColor = {
                                                            val shouldRender = fullColorBackgroundColorIndex != FULL_COLOR_CUSTOM_BACKGROUND_INDEX
                                                            fullColorBackgroundColorIndex = FULL_COLOR_CUSTOM_BACKGROUND_INDEX
                                                            isFullColorCustomColorDialogVisible = true
                                                            if (shouldRender) {
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed)
                                                                }
                                                            }
                                                        }
                                                    )
                                                    WatermarkCompactSwitchCard(
                                                        title = fullColorUiCopy.swapLayoutLabel,
                                                        checked = fullColorImageOnTop,
                                                        onCheckedChange = { checked ->
                                                            if (fullColorImageOnTop != checked) {
                                                                fullColorImageOnTop = checked
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed)
                                                                }
                                                            }
                                                        },
                                                        containerColor = panelBackground,
                                                        selectedContainerColor = panelSelectedBackground,
                                                        outlineColor = cardOutlineColor,
                                                        primaryTextColor = primaryTextColor,
                                                        secondaryTextColor = secondaryTextColor
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        WatermarkCompactSwitchCard(
                                                            title = watermarkExtraCopy.time,
                                                            checked = showDateWatermark && customDateLabel.trim().isNotBlank(),
                                                            onCheckedChange = { checked ->
                                                                if (checked) {
                                                                    disableSignatureIfNeeded()
                                                                    showFullColorNoteWatermark = false
                                                                    isCustomDateDialogVisible = true
                                                                } else {
                                                                    showDateWatermark = false
                                                                    selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                                }
                                                            },
                                                            containerColor = panelBackground,
                                                            selectedContainerColor = panelSelectedBackground,
                                                            outlineColor = cardOutlineColor,
                                                            primaryTextColor = primaryTextColor,
                                                            secondaryTextColor = secondaryTextColor,
                                                            modifier = Modifier.weight(1f),
                                                            trailingContent = {
                                                                IconButton(
                                                                    onClick = { isCustomDateDialogVisible = true },
                                                                    modifier = Modifier.size(18.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Edit,
                                                                        contentDescription = fullColorUiCopy.dateDialogTitle,
                                                                        tint = MaterialTheme.colorScheme.onBackground,
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                }
                                                            }
                                                        )
                                                        WatermarkCompactSwitchCard(
                                                            title = fullColorUiCopy.locationLabel,
                                                            checked = showCityLocationWatermark && customLocationLabel.trim().isNotBlank(),
                                                            onCheckedChange = { checked ->
                                                                if (checked) {
                                                                    disableSignatureIfNeeded()
                                                                    showFullColorNoteWatermark = false
                                                                    isCustomLocationDialogVisible = true
                                                                } else {
                                                                    showLocationWatermark = false
                                                                    showCityLocationWatermark = false
                                                                    selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                                }
                                                            },
                                                            containerColor = panelBackground,
                                                            selectedContainerColor = panelSelectedBackground,
                                                            outlineColor = cardOutlineColor,
                                                            primaryTextColor = primaryTextColor,
                                                            secondaryTextColor = secondaryTextColor,
                                                            modifier = Modifier.weight(1f),
                                                            trailingContent = {
                                                                IconButton(
                                                                    onClick = { isCustomLocationDialogVisible = true },
                                                                    modifier = Modifier.size(18.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Edit,
                                                                        contentDescription = fullColorUiCopy.locationDialogTitle,
                                                                        tint = MaterialTheme.colorScheme.onBackground,
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }
                                                    WatermarkCompactSwitchCard(
                                                        title = fullColorUiCopy.noteLabel,
                                                        checked = showFullColorNoteWatermark && customFullColorNoteText.trim().isNotBlank(),
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                disableSignatureIfNeeded()
                                                                isFullColorNoteDialogVisible = true
                                                            } else {
                                                                showFullColorNoteWatermark = false
                                                                selectedUri?.let {
                                                                    rerenderForCurrentSelection(
                                                                        it,
                                                                        watermarkExtraCopy.updateWatermarkFailed
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        containerColor = panelBackground,
                                                        selectedContainerColor = panelSelectedBackground,
                                                        outlineColor = cardOutlineColor,
                                                        primaryTextColor = primaryTextColor,
                                                        secondaryTextColor = secondaryTextColor,
                                                        supportingText = fullColorUiCopy.noteHint,
                                                        trailingContent = {
                                                            IconButton(
                                                                onClick = { isFullColorNoteDialogVisible = true },
                                                                modifier = Modifier.size(18.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Edit,
                                                                    contentDescription = fullColorUiCopy.noteDialogTitle,
                                                                    tint = MaterialTheme.colorScheme.onBackground,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    )
                                                    WatermarkCompactSliderCard(
                                                        title = resolveFullColorTextScaleLabel(appLanguage),
                                                        subtitle = "${(fullColorTextScaleFactorDraft * 100f).roundToInt()}%",
                                                        containerColor = panelBackground,
                                                        outlineColor = cardOutlineColor,
                                                        primaryTextColor = primaryTextColor,
                                                        secondaryTextColor = secondaryTextColor
                                                    ) {
                                                        Slider(
                                                            value = fullColorTextScaleFactorDraft,
                                                            onValueChange = { value ->
                                                                val resolved = value.coerceIn(
                                                                    FULL_COLOR_TEXT_SCALE_MIN,
                                                                    FULL_COLOR_TEXT_SCALE_MAX
                                                                )
                                                                fullColorTextScaleFactorDraft = resolved
                                                                if (kotlin.math.abs(fullColorTextScaleFactor - resolved) > 0.0005f) {
                                                                    fullColorTextScaleFactor = resolved
                                                                    selectedUri?.let {
                                                                        rerenderForCurrentSelection(
                                                                            it,
                                                                            watermarkExtraCopy.updateWatermarkFailed
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            onValueChangeFinished = {
                                                                if (kotlin.math.abs(fullColorTextScaleFactor - fullColorTextScaleFactorDraft) > 0.0005f) {
                                                                    fullColorTextScaleFactor = fullColorTextScaleFactorDraft
                                                                    selectedUri?.let {
                                                                        rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed)
                                                                    }
                                                                }
                                                            },
                                                            valueRange = FULL_COLOR_TEXT_SCALE_MIN..FULL_COLOR_TEXT_SCALE_MAX
                                                        )
                                                    }
                                                }
                                            } else {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    WatermarkCompactSwitchCard(
                                                        title = watermarkExtraCopy.model,

                                                        checked = showModelWatermark,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                disableSignatureIfNeeded()
                                                            }
                                                            showModelWatermark = checked
                                                            selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                        },
                                                        containerColor = panelBackground,
                                                        selectedContainerColor = panelSelectedBackground,
                                                        outlineColor = cardOutlineColor,
                                                        primaryTextColor = primaryTextColor,
                                                        secondaryTextColor = secondaryTextColor,
                                                        modifier = Modifier.weight(1f),
                                                        supportingText = watermarkExtraCopy.showCameraModel,
                                                        trailingContent = {
                                                            IconButton(
                                                                onClick = { isModelDialogVisible = true },
                                                                modifier = Modifier.size(18.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Edit,
                                                                    contentDescription = watermarkExtraCopy.editModelText,
                                                                    tint = MaterialTheme.colorScheme.onBackground,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    )
                                                    if (hasDateData) {
                                                        WatermarkCompactSwitchCard(
                                                            title = watermarkExtraCopy.time,
                                                            checked = showDateWatermark,
                                                            onCheckedChange = { checked ->
                                                                if (checked) {
                                                                    disableSignatureIfNeeded()
                                                                }
                                                                showDateWatermark = checked
                                                                selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                            },
                                                            containerColor = panelBackground,
                                                            selectedContainerColor = panelSelectedBackground,
                                                            outlineColor = cardOutlineColor,
                                                            primaryTextColor = primaryTextColor,
                                                            secondaryTextColor = secondaryTextColor,
                                                            modifier = Modifier.weight(1f),
                                                            supportingText = watermarkExtraCopy.showCaptureDate
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                                if (hasSpecData || (selectedTemplate == WatermarkTemplatePreset.WatermarkFrame && (hasLocationData || hasCityLocationData))) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (hasSpecData) {
                                                            WatermarkCompactSwitchCard(
                                                                title = watermarkExtraCopy.cameraInfo,
                                                                checked = showExifWatermark,
                                                                onCheckedChange = { checked ->
                                                                    if (checked) {
                                                                        disableSignatureIfNeeded()
                                                                    }
                                                                    showExifWatermark = checked
                                                                    selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                                },
                                                                containerColor = panelBackground,
                                                                selectedContainerColor = panelSelectedBackground,
                                                                outlineColor = cardOutlineColor,
                                                                primaryTextColor = primaryTextColor,
                                                                secondaryTextColor = secondaryTextColor,
                                                                modifier = Modifier.weight(1f),
                                                                supportingText = "ISO、快门、焦距等"
                                                            )
                                                        } else {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                        if (selectedTemplate == WatermarkTemplatePreset.WatermarkFrame && hasLocationData) {
                                                            WatermarkCompactSwitchCard(
                                                                title = "经纬度",
                                                                checked = showLocationWatermark,
                                                                onCheckedChange = { checked ->
                                                                    if (checked) {
                                                                        disableSignatureIfNeeded()
                                                                        showCityLocationWatermark = false
                                                                    }
                                                                    showLocationWatermark = checked
                                                                    selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                                },
                                                                containerColor = panelBackground,
                                                                selectedContainerColor = panelSelectedBackground,
                                                                outlineColor = cardOutlineColor,
                                                                primaryTextColor = primaryTextColor,
                                                                secondaryTextColor = secondaryTextColor,
                                                                modifier = Modifier.weight(1f),
                                                                supportingText = "显示坐标信息"
                                                            )
                                                        } else if (selectedTemplate == WatermarkTemplatePreset.WatermarkFrame && hasCityLocationData) {
                                                            WatermarkCompactSwitchCard(
                                                                title = "城市位置",
                                                                checked = showCityLocationWatermark,
                                                                onCheckedChange = { checked ->
                                                                    if (checked) {
                                                                        disableSignatureIfNeeded()
                                                                        showLocationWatermark = false
                                                                    }
                                                                    showCityLocationWatermark = checked
                                                                    selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                                },
                                                                containerColor = panelBackground,
                                                                selectedContainerColor = panelSelectedBackground,
                                                                outlineColor = cardOutlineColor,
                                                                primaryTextColor = primaryTextColor,
                                                                secondaryTextColor = secondaryTextColor,
                                                                modifier = Modifier.weight(1f),
                                                                supportingText = "显示城市或地点"
                                                            )
                                                        } else {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                                if (selectedTemplate == WatermarkTemplatePreset.WatermarkFrame && hasLocationData && hasCityLocationData) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        WatermarkCompactSwitchCard(
                                                            title = "城市位置",
                                                            checked = showCityLocationWatermark,
                                                            onCheckedChange = { checked ->
                                                                if (checked) {
                                                                    disableSignatureIfNeeded()
                                                                    showLocationWatermark = false
                                                                }
                                                                showCityLocationWatermark = checked
                                                                selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                                                            },
                                                            containerColor = panelBackground,
                                                            selectedContainerColor = panelSelectedBackground,
                                                            outlineColor = cardOutlineColor,
                                                            primaryTextColor = primaryTextColor,
                                                            secondaryTextColor = secondaryTextColor,
                                                            modifier = Modifier.weight(1f),
                                                            supportingText = "与经纬度二选一"
                                                        )
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(watermarkExtraCopy.paramFont, fontSize = 12.sp, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                WATERMARK_TEXT_FONT_OPTIONS.forEach { option ->
                                                    FilterChip(
                                                        selected = watermarkFontKey == option.key,
                                                        onClick = {
                                                            val normalized = normalizeWatermarkFontKey(option.key)
                                                            if (normalized != watermarkFontKey) {
                                                                watermarkFontKey = normalized
                                                                prefs.edit().putString(WATERMARK_PREF_TEXT_FONT_KEY, normalized).apply()
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "更新字体失败") }
                                                            }
                                                        },
                                                        label = {
                                                            FontPreviewLabel(
                                                                text = option.label,
                                                                assetPath = option.assetPath,
                                                                modifier = Modifier.widthIn(min = 36.dp, max = 72.dp)
                                                            )
                                                        },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                        }
                                        if (activeCustomizePanel == WatermarkCustomizePanel.Border
                                            && selectedTemplate == WatermarkTemplatePreset.WatermarkFrame
                                        ) {
                                            Text(
                                                text = "边框",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            WatermarkCompactSliderCard(
                                                title = resolveWatermarkUILabel(appLanguage, "边框宽度"),
                                                subtitle = resolveWatermarkUILabel(appLanguage, "在面板内调整，预览会保持固定可见"),
                                                containerColor = panelBackground,
                                                outlineColor = cardOutlineColor,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor
                                            ) {
                                                Slider(
                                                    value = frameSideTopBorderRatioDraft,
                                                    onValueChange = { value ->
                                                        frameSideTopBorderRatioDraft = value
                                                    },
                                                    onValueChangeFinished = {
                                                        if (frameSideTopBorderRatio != frameSideTopBorderRatioDraft) {
                                                            frameSideTopBorderRatio = frameSideTopBorderRatioDraft
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "更新边框失败") }
                                                        }
                                                    },
                                                    valueRange = 0f..0.06f
                                                )
                                            }
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.Border
                                        && selectedTemplate == WatermarkTemplatePreset.WatermarkFrame
                                    ) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "边框背景色",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FLOATING_COLOR_OPTIONS.forEachIndexed { index, option ->
                                                val showSwatchBorder = option.android == AndroidColor.WHITE
                                                FilterChip(
                                                    selected = frameBackgroundColorIndex == index,
                                                    onClick = {
                                                        frameBackgroundColorIndex = index
                                                        if (showLogoWatermark) {
                                                            applyPreferredVariantForBrand(resolveEffectiveBrandKey())
                                                        }
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新背景色失败") }
                                                    },
                                                    label = { Text(option.label) },
                                                    leadingIcon = {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .then(
                                                                    if (showSwatchBorder) {
                                                                        Modifier.border(
                                                                            width = 0.8.dp,
                                                                            color = if (isDark) Color.White.copy(alpha = 0.56f) else Color.Black.copy(alpha = 0.36f),
                                                                            shape = RoundedCornerShape(5.dp)
                                                                        )
                                                                    } else {
                                                                        Modifier
                                                                    }
                                                                )
                                                                .background(option.compose)
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                    }

                                    if (activeCustomizePanel == WatermarkCustomizePanel.Border
                                        && selectedTemplate == WatermarkTemplatePreset.ThinWhiteBorder
                                    ) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "边框颜色",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FLOATING_COLOR_OPTIONS.forEachIndexed { index, option ->
                                                val showSwatchBorder = option.android == AndroidColor.WHITE
                                                FilterChip(
                                                    selected = thinBorderColorIndex == index,
                                                    onClick = {
                                                        thinBorderColorIndex = index
                                                        if (showLogoWatermark) {
                                                            applyPreferredVariantForBrand(resolveEffectiveBrandKey())
                                                        }
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新边框颜色失败") }
                                                    },
                                                    label = { Text(option.label) },
                                                    leadingIcon = {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .then(
                                                                    if (showSwatchBorder) {
                                                                        Modifier.border(
                                                                            width = 0.8.dp,
                                                                            color = if (isDark) Color.White.copy(alpha = 0.56f) else Color.Black.copy(alpha = 0.36f),
                                                                            shape = RoundedCornerShape(5.dp)
                                                                        )
                                                                    } else {
                                                                        Modifier
                                                                    }
                                                                )
                                                                .background(option.compose)
                                                        )
                                                    },
                                                    border = null,
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                    }

                                    if (supportsSignaturePanel && activeCustomizePanel == WatermarkCustomizePanel.Signature) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = watermarkExtraCopy.signature,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryTextColor
                                            )
                                            Switch(
                                                checked = signatureEnabled,
                                                onCheckedChange = { checked ->
                                                    signatureEnabled = checked
                                                    if (checked) {
                                                        applySignatureTakeover()
                                                    }
                                                    prefs.edit().putBoolean("watermark_signature_enabled", checked).apply()
                                                    selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateSignatureFailed) }
                                                }
                                            )
                                        }
                                        if (signatureEnabled) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(watermarkExtraCopy.selectSignature, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = panelBackground,
                                                    contentColor = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .clickable {
                                                            addSignatureInput = ""
                                                            isAddSignatureDialogVisible = true
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Add,
                                                            contentDescription = "新增签名",
                                                            tint = MaterialTheme.colorScheme.onBackground,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("添加", fontSize = 12.sp, color = primaryTextColor)
                                                    }
                                                }
                                                signatureHistory.forEach { sign ->
                                                    val selected = signatureText == sign
                                                    Surface(
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = if (selected) {
                                                            panelSelectedBackground
                                                        } else {
                                                            panelBackground
                                                        },
                                                        contentColor = MaterialTheme.colorScheme.onBackground,
                                                        modifier = Modifier
                                                            .height(32.dp)
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .clickable {
                                                                applySignatureSelection(sign)
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "应用签名失败") }
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(start = 10.dp, end = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = sign,
                                                                fontSize = 12.sp,
                                                                color = primaryTextColor,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.widthIn(max = 180.dp)
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .clickable { pendingDeleteSignature = sign },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Close,
                                                                    contentDescription = watermarkExtraCopy.deleteSignatureTitle,
                                                                    tint = MaterialTheme.colorScheme.onBackground,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (signatureHistory.isEmpty() && signatureText.isBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "请创建签名",
                                                    fontSize = 12.sp,
                                                    color = errorTextColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("签名字体", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SIGNATURE_FONT_OPTIONS.forEach { option ->
                                                    FilterChip(
                                                        selected = signatureFontKey == option.key,
                                                        onClick = {
                                                            val normalized = normalizeSignatureFontKey(option.key)
                                                            if (normalized != signatureFontKey) {
                                                                signatureFontKey = normalized
                                                                prefs.edit().putString(WATERMARK_PREF_SIGNATURE_FONT_KEY, normalized).apply()
                                                                selectedUri?.let { rerenderForCurrentSelection(it, "更新签名字体失败") }
                                                            }
                                                        },
                                                        label = {
                                                            FontPreviewLabel(
                                                                text = option.label,
                                                                assetPath = option.assetPath,
                                                                modifier = Modifier.widthIn(min = 36.dp, max = 72.dp)
                                                            )
                                                        },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("签名颜色", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SIGNATURE_COLOR_OPTIONS.forEachIndexed { index, option ->
                                                    val isAuto = option.android == Int.MIN_VALUE
                                                    val showSwatchBorder = option.android == AndroidColor.WHITE || isAuto
                                                    FilterChip(
                                                        selected = signatureColorIndex == index,
                                                        onClick = {
                                                            signatureColorIndex = index
                                                            selectedUri?.let { rerenderForCurrentSelection(it, "更新签名颜色失败") }
                                                        },
                                                        label = { Text(option.label) },
                                                        leadingIcon = {
                                                            if (isAuto) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.AutoAwesome,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onBackground,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(10.dp)
                                                                        .clip(RoundedCornerShape(5.dp))
                                                                        .then(
                                                                            if (showSwatchBorder) {
                                                                                Modifier.border(
                                                                                    width = 0.8.dp,
                                                                                    color = if (isDark) Color.White.copy(alpha = 0.56f) else Color.Black.copy(alpha = 0.36f),
                                                                                    shape = RoundedCornerShape(5.dp)
                                                                                )
                                                                            } else {
                                                                                Modifier
                                                                            }
                                                                        )
                                                                        .background(option.compose)
                                                                )
                                                            }
                                                        },
                                                        border = null,
                                                        colors = chipColors
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("左右位置", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                            Slider(
                                                value = signatureHorizontalOffsetRatioDraft,
                                                onValueChange = { value ->
                                                    signatureHorizontalOffsetRatioDraft = value
                                                },
                                                onValueChangeFinished = {
                                                    if (signatureHorizontalOffsetRatio != signatureHorizontalOffsetRatioDraft) {
                                                        signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatioDraft
                                                        prefs.edit().putFloat(WATERMARK_PREF_SIGNATURE_OFFSET_X, signatureHorizontalOffsetRatio).apply()
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新签名位置失败") }
                                                    }
                                                },
                                                valueRange = -1f..1f
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("高度位置", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                            Slider(
                                                value = signatureVerticalOffsetRatioDraft,
                                                onValueChange = { value ->
                                                    signatureVerticalOffsetRatioDraft = value
                                                },
                                                onValueChangeFinished = {
                                                    if (signatureVerticalOffsetRatio != signatureVerticalOffsetRatioDraft) {
                                                        signatureVerticalOffsetRatio = signatureVerticalOffsetRatioDraft
                                                        prefs.edit().putFloat(WATERMARK_PREF_SIGNATURE_OFFSET_Y, signatureVerticalOffsetRatio).apply()
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新签名高度失败") }
                                                    }
                                                },
                                                valueRange = -1f..1f
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("签名大小", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                            Slider(
                                                value = signatureScaleFactorDraft,
                                                onValueChange = { value ->
                                                    signatureScaleFactorDraft = value
                                                },
                                                onValueChangeFinished = {
                                                    if (signatureScaleFactor != signatureScaleFactorDraft) {
                                                        signatureScaleFactor = signatureScaleFactorDraft
                                                        prefs.edit().putFloat(WATERMARK_PREF_SIGNATURE_SCALE, signatureScaleFactor).apply()
                                                        selectedUri?.let { rerenderForCurrentSelection(it, "更新签名大小失败") }
                                                    }
                                                },
                                                valueRange = 0.65f..1.8f
                                            )
                                        }
                                    }
                                }
                                if (activeCustomizePanel == WatermarkCustomizePanel.Border && templateSupportsFloatingControls) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = resolveWatermarkUILabel(appLanguage, "悬浮样式"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(resolveWatermarkUILabel(appLanguage, "背景色"), fontSize = 12.sp, color = primaryTextColor)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FLOATING_COLOR_OPTIONS.forEachIndexed { index, option ->
                                            val showSwatchBorder = option.android == AndroidColor.WHITE
                                            FilterChip(
                                                selected = floatingBackgroundColorIndex == index,
                                                onClick = {
                                                    floatingBackgroundColorIndex = index
                                                    selectedUri?.let { rerenderForCurrentSelection(it, "更新悬浮背景失败") }
                                                },
                                                label = { Text(option.label) },
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .then(
                                                                if (showSwatchBorder) {
                                                                    Modifier.border(
                                                                        width = 0.8.dp,
                                                                        color = if (isDark) Color.White.copy(alpha = 0.56f) else Color.Black.copy(alpha = 0.36f),
                                                                        shape = RoundedCornerShape(5.dp)
                                                                    )
                                                                } else {
                                                                    Modifier
                                                                }
                                                            )
                                                            .background(option.compose)
                                                    )
                                                },
                                                border = null,
                                                colors = chipColors
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("描边色", fontSize = 12.sp, color = primaryTextColor)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FLOATING_STROKE_OPTIONS.forEachIndexed { index, option ->
                                            val showSwatchBorder = option.android == AndroidColor.WHITE || option.android == AndroidColor.TRANSPARENT
                                            FilterChip(
                                                selected = floatingStrokeColorIndex == index,
                                                onClick = {
                                                    floatingStrokeColorIndex = index
                                                    selectedUri?.let { rerenderForCurrentSelection(it, "更新描边颜色失败") }
                                                },
                                                label = { Text(option.label) },
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .then(
                                                                if (showSwatchBorder) {
                                                                    Modifier.border(
                                                                        width = 0.8.dp,
                                                                        color = if (isDark) Color.White.copy(alpha = 0.56f) else Color.Black.copy(alpha = 0.36f),
                                                                        shape = RoundedCornerShape(5.dp)
                                                                    )
                                                                } else {
                                                                    Modifier
                                                                }
                                                            )
                                                            .background(if (option.android == AndroidColor.TRANSPARENT) Color.Transparent else option.compose)
                                                    )
                                                },
                                                border = null,
                                                colors = chipColors
                                            )
                                        }
                                    }
                                }

                                if (
                                    activeCustomizePanel == WatermarkCustomizePanel.Border &&
                                    selectedTemplate in setOf(
                                        WatermarkTemplatePreset.BlurBackground,
                                        WatermarkTemplatePreset.Floating
                                    )
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val isBlurTemplate = selectedTemplate == WatermarkTemplatePreset.BlurBackground
                                    WatermarkCompactSliderCard(
                                        title = "图片圆角",
                                        subtitle = if (isBlurTemplate) "背景模糊模板的内图圆角" else "悬浮模板的内图圆角",
                                        containerColor = panelBackground,
                                        outlineColor = cardOutlineColor,
                                        primaryTextColor = primaryTextColor,
                                        secondaryTextColor = secondaryTextColor
                                    ) {
                                        Slider(
                                            value = if (isBlurTemplate) {
                                                blurImageCornerRadiusRatioDraft
                                            } else {
                                                floatingImageCornerRadiusRatioDraft
                                            },
                                            onValueChange = { value ->
                                                if (isBlurTemplate) {
                                                    blurImageCornerRadiusRatioDraft = value
                                                } else {
                                                    floatingImageCornerRadiusRatioDraft = value
                                                }
                                            },
                                            onValueChangeFinished = {
                                                if (isBlurTemplate) {
                                                    if (blurImageCornerRadiusRatio != blurImageCornerRadiusRatioDraft) {
                                                        blurImageCornerRadiusRatio = blurImageCornerRadiusRatioDraft
                                                        selectedUri?.let {
                                                            rerenderForCurrentSelection(it, "更新背景模糊圆角失败")
                                                        }
                                                    }
                                                } else {
                                                    if (floatingImageCornerRadiusRatio != floatingImageCornerRadiusRatioDraft) {
                                                        floatingImageCornerRadiusRatio = floatingImageCornerRadiusRatioDraft
                                                        selectedUri?.let {
                                                            rerenderForCurrentSelection(it, "更新悬浮圆角失败")
                                                        }
                                                    }
                                                }
                                            },
                                            valueRange = if (isBlurTemplate) 0f..0.18f else 0f..0.12f
                                        )
                                    }
                                }

                                if (activeCustomizePanel == WatermarkCustomizePanel.Border && selectedTemplate == WatermarkTemplatePreset.FilmRetro) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "胶片边框",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FILM_FRAME_RES_IDS.indices.forEach { index ->
                                            FilterChip(
                                                selected = selectedFilmFrameIndex == index,
                                                onClick = {
                                                    selectedFilmFrameIndex = index
                                                    selectedUri?.let { rerenderForCurrentSelection(it, "切换胶片边框失败") }
                                                },
                                                label = { Text(FILM_FRAME_LABELS.getOrElse(index) { "框 ${index + 1}" }) },
                                                border = null,
                                                colors = chipColors
                                            )
                                        }
                                    }
                                }
                                if (activeCustomizePanel == WatermarkCustomizePanel.Border && selectedTemplate == WatermarkTemplatePreset.Polaroid) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "拍立得样式",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        POLAROID_FRAME_RES_IDS.indices.forEach { index ->
                                            FilterChip(
                                                selected = selectedPolaroidFrameIndex == index,
                                                onClick = {
                                                    selectedPolaroidFrameIndex = index
                                                    selectedUri?.let { rerenderForCurrentSelection(it, "切换拍立得样式失败") }
                                                },
                                                label = { Text(POLAROID_FRAME_LABELS.getOrElse(index) { "样式 ${index + 1}" }) },
                                                border = null,
                                                colors = chipColors
                                            )
                                        }
                                    }
                                }
                                if (activeCustomizePanel == WatermarkCustomizePanel.Border
                                    && selectedTemplate !in setOf(
                                        WatermarkTemplatePreset.WatermarkFrame,
                                        WatermarkTemplatePreset.ThinWhiteBorder,
                                        WatermarkTemplatePreset.Floating,
                                        WatermarkTemplatePreset.FilmRetro,
                                        WatermarkTemplatePreset.Polaroid
                                    )
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(watermarkExtraCopy.borderSettingsUnsupported, fontSize = 12.sp, color = secondaryTextColor)
                                }

                                if (!errorMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        fontSize = 12.sp,
                                        color = errorTextColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        }
                        }
                                }
                    }
                    }
                    }

                    WatermarkFlowStep.Crop -> Unit

                    WatermarkFlowStep.CopyrightProfile -> {
                        WatermarkCopyrightProfilePage(
                            authorName = copyrightState.authorName,
                            authorEmail = copyrightState.authorEmail,
                            yearText = copyrightState.yearText,
                            onCancel = exitCopyrightProfileToPreview,
                            onSave = { author, email, year ->
                                copyrightState.updateAuthorName(author)
                                copyrightState.updateAuthorEmail(email)
                                copyrightState.updateYearText(year.toString())
                                toastMessage = watermarkExtraCopy.saveCopyrightInfoSuccess
                                exitCopyrightProfileToPreview()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                        }
                    }
                }
            }

        }

        if (showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(loadingOverlayColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.6.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        if (awaitingInitialModernImagePicker && selectedUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color.Black.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }

        if (isSavePresetDialogVisible) {
            AlertDialog(
                onDismissRequest = { isSavePresetDialogVisible = false },
                title = { Text("保存预设", color = primaryTextColor) },
                text = {
                    OutlinedTextField(
                        value = savePresetNameInput,
                        onValueChange = { savePresetNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("预设名称", color = secondaryTextColor) },
                        placeholder = { Text(resolveNextPresetName(), color = secondaryTextColor) },
                        colors = dialogTextFieldColors
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            val savedName = saveCurrentPreset(savePresetNameInput)
                            toastMessage = "已保存预设：$savedName"
                            isSavePresetDialogVisible = false
                        }
                    ) {
                        Text("保存", color = dialogActionTextColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isSavePresetDialogVisible = false
                    }) {
                        Text("取消", color = dialogActionTextColor)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        val deletingPreset = pendingDeletePresetId?.let { id ->
            userPresets.firstOrNull { it.id == id }
        }
        if (deletingPreset != null) {
            val deletingTemplate = runCatching {
                WatermarkTemplatePreset.valueOf(deletingPreset.templateName)
            }.getOrNull()
            val deletingPresetDisplayName = deletingTemplate
                ?.let { deletingPreset.displayName(appLanguage, it) }
                ?: cleanWatermarkPresetDisplayName(deletingPreset.name).ifBlank { deletingPreset.name.trim() }
            AlertDialog(
                onDismissRequest = { pendingDeletePresetId = null },
                title = { Text("删除预设", color = primaryTextColor) },
                text = { Text("确定删除“$deletingPresetDisplayName”吗？", color = primaryTextColor) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            val removed = userPresets.firstOrNull { it.id == deletingPreset.id }
                            if (removed != null) {
                                userPresets.removeAll { it.id == removed.id }
                                if (selectedPresetId == removed.id) {
                                    selectedPresetId = null
                                }
                                persistUserPresets()
                                val removedTemplate = runCatching {
                                    WatermarkTemplatePreset.valueOf(removed.templateName)
                                }.getOrNull()
                                val removedDisplayName = removedTemplate
                                    ?.let { removed.displayName(appLanguage, it) }
                                    ?: cleanWatermarkPresetDisplayName(removed.name).ifBlank { removed.name.trim() }
                                toastMessage = "已删除预设：$removedDisplayName"
                            }
                            pendingDeletePresetId = null
                        }
                    ) { Text("删除", color = dialogActionTextColor) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        pendingDeletePresetId = null
                    }) {
                        Text("取消", color = dialogActionTextColor)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isModelDialogVisible) {
            AlertDialog(
                onDismissRequest = { isModelDialogVisible = false },
                title = { Text(watermarkExtraCopy.modelDialogTitle, color = primaryTextColor) },
                text = {
                    OutlinedTextField(
                        value = customModelLabel,
                        onValueChange = { customModelLabel = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(watermarkExtraCopy.modelDialogLabel, color = secondaryTextColor) },
                        placeholder = { Text(resolvedModelLabel, color = secondaryTextColor) },
                        colors = dialogTextFieldColors
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            isModelDialogVisible = false
                            selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                        }
                    ) {
                        Text(watermarkExtraCopy.confirm, color = dialogActionTextColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isModelDialogVisible = false
                    }) {
                        Text(watermarkExtraCopy.cancel, color = dialogActionTextColor)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isCustomDateDialogVisible) {
            val initialTimeDraft = remember(
                isCustomDateDialogVisible,
                customDateLabel,
                currentExifInfo?.dateTimeOriginal
            ) {
                resolveFullColorTimeDraft(
                    customValue = customDateLabel,
                    exifValue = currentExifInfo?.dateTimeOriginal
                )
            }
            var timeHourDraft by remember(isCustomDateDialogVisible) {
                mutableStateOf(initialTimeDraft.hour)
            }
            var timeMinuteDraft by remember(isCustomDateDialogVisible) {
                mutableStateOf(initialTimeDraft.minute)
            }
            var timeIsPmDraft by remember(isCustomDateDialogVisible) {
                mutableStateOf(initialTimeDraft.isPm)
            }
            AlertDialog(
                onDismissRequest = { isCustomDateDialogVisible = false },
                title = { Text(fullColorUiCopy.dateDialogTitle, color = primaryTextColor) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FullColorTimeWheelPicker(
                            hour = timeHourDraft,
                            minute = timeMinuteDraft,
                            onHourChange = { timeHourDraft = it },
                            onMinuteChange = { timeMinuteDraft = it },
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !timeIsPmDraft,
                                onClick = { timeIsPmDraft = false },
                                label = { Text("AM") }
                            )
                            FilterChip(
                                selected = timeIsPmDraft,
                                onClick = { timeIsPmDraft = true },
                                label = { Text("PM") }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            val nextTime = buildFullColorCustomTimeLabel(
                                hourInput = timeHourDraft,
                                minuteInput = timeMinuteDraft,
                                isPm = timeIsPmDraft
                            )
                            customDateLabel = nextTime
                            showDateWatermark = nextTime.isNotBlank()
                            if (showDateWatermark) {
                                showFullColorNoteWatermark = false
                            }
                            isCustomDateDialogVisible = false
                            selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                        }
                    ) {
                        Text(watermarkExtraCopy.confirm, color = dialogActionTextColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isCustomDateDialogVisible = false
                    }) {
                        Text(watermarkExtraCopy.cancel, color = dialogActionTextColor)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isCustomLocationDialogVisible) {
            var customLocationDraft by remember(isCustomLocationDialogVisible) {
                mutableStateOf(customLocationLabel)
            }
            AlertDialog(
                onDismissRequest = { isCustomLocationDialogVisible = false },
                title = { Text(fullColorUiCopy.locationDialogTitle, color = primaryTextColor) },
                text = {
                    OutlinedTextField(
                        value = customLocationDraft,
                        onValueChange = { customLocationDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            currentExifInfo
                                ?.cityLocation
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?.let { hint ->
                                    Text(
                                        text = hint,
                                        color = secondaryTextColor
                                    )
                                }
                        },
                        colors = dialogTextFieldColors
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            val nextLocation = customLocationDraft.trim()
                            customLocationLabel = nextLocation
                            showLocationWatermark = false
                            showCityLocationWatermark = nextLocation.isNotBlank()
                            if (showCityLocationWatermark) {
                                showFullColorNoteWatermark = false
                            }
                            isCustomLocationDialogVisible = false
                            selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                        }
                    ) {
                        Text(watermarkExtraCopy.confirm, color = dialogActionTextColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isCustomLocationDialogVisible = false
                    }) {
                        Text(watermarkExtraCopy.cancel, color = dialogActionTextColor)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isFullColorNoteDialogVisible) {
            var noteDraft by remember(isFullColorNoteDialogVisible) {
                mutableStateOf(customFullColorNoteText)
            }
            AlertDialog(
                onDismissRequest = { isFullColorNoteDialogVisible = false },
                title = { Text(fullColorUiCopy.noteDialogTitle, color = primaryTextColor) },
                text = {
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 132.dp),
                        singleLine = false,
                        minLines = 4,
                        maxLines = 8,
                        placeholder = {
                            Text(
                                text = fullColorUiCopy.noteHint,
                                color = secondaryTextColor
                            )
                        },
                        colors = dialogTextFieldColors
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            val nextNote = noteDraft.trim()
                            customFullColorNoteText = nextNote
                            showFullColorNoteWatermark = nextNote.isNotBlank()
                            if (showFullColorNoteWatermark) {
                                showDateWatermark = false
                                showLocationWatermark = false
                                showCityLocationWatermark = false
                            }
                            isFullColorNoteDialogVisible = false
                            selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed) }
                        }
                    ) {
                        Text(watermarkExtraCopy.confirm, color = dialogActionTextColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isFullColorNoteDialogVisible = false
                    }) {
                        Text(watermarkExtraCopy.cancel, color = dialogActionTextColor)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isFullColorCustomColorDialogVisible) {
            var fullColorHexDraft by remember(
                isFullColorCustomColorDialogVisible,
                fullColorCustomBackgroundColor
            ) {
                mutableStateOf(formatFullColorHexColor(fullColorCustomBackgroundColor))
            }
            val parsedCustomColor = parseFullColorHexColor(fullColorHexDraft)
            AlertDialog(
                onDismissRequest = { isFullColorCustomColorDialogVisible = false },
                title = { Text(resolveFullColorCustomColorDialogTitle(appLanguage), color = primaryTextColor) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fullColorHexDraft,
                            onValueChange = { fullColorHexDraft = sanitizeFullColorHexInput(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("#RRGGBB", color = secondaryTextColor) },
                            placeholder = { Text("#365A7A", color = secondaryTextColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            colors = dialogTextFieldColors
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(parsedCustomColor ?: fullColorCustomBackgroundColor))
                                    .border(1.dp, cardOutlineColor, RoundedCornerShape(10.dp))
                            )
                            Text(
                                text = parsedCustomColor?.let(::formatFullColorHexColor)
                                    ?: resolveFullColorInvalidHexLabel(appLanguage),
                                fontSize = 12.sp,
                                color = if (parsedCustomColor == null) errorTextColor else secondaryTextColor
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = parsedCustomColor != null,
                        onClick = {
                            val nextColor = parsedCustomColor ?: return@TextButton
                            performHaptic()
                            fullColorCustomBackgroundColor = normalizeFullColorOpaqueColor(nextColor)
                            fullColorBackgroundColorIndex = FULL_COLOR_CUSTOM_BACKGROUND_INDEX
                            isFullColorCustomColorDialogVisible = false
                            selectedUri?.let {
                                rerenderForCurrentSelection(it, watermarkExtraCopy.updateWatermarkFailed)
                            }
                        }
                    ) {
                        Text(
                            watermarkExtraCopy.confirm,
                            color = if (parsedCustomColor != null) {
                                dialogActionTextColor
                            } else {
                                dialogActionTextColor.copy(alpha = 0.38f)
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isFullColorCustomColorDialogVisible = false
                    }) {
                        Text(watermarkExtraCopy.cancel, color = dialogActionTextColor)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isBrandCorrectionDialogVisible) {
            AlertDialog(
                onDismissRequest = { isBrandCorrectionDialogVisible = false },
                title = { Text(watermarkExtraCopy.brandCorrectionTitle, color = primaryTextColor) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BRAND_LOGO_GROUPS.forEach { group ->
                            FilterChip(
                                selected = brandCorrectionDraftKey == group.key,
                                onClick = {
                                    performHaptic()
                                    brandCorrectionDraftKey = group.key
                                },
                                label = { Text(group.label) },
                                border = null,
                                colors = chipColors
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            val selected = brandCorrectionDraftKey.takeIf { BRAND_LOGO_GROUP_MAP.containsKey(it) }
                            if (selected != null) {
                                sessionManualBrandKeys = (sessionManualBrandKeys + selected).distinct()
                                selectedBrandKey = selected
                                applyPreferredVariantForBrand(selected)
                                selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.applyBrandFailed) }
                            }
                            isBrandCorrectionDialogVisible = false
                        }
                    ) { Text(watermarkExtraCopy.confirm, color = dialogActionTextColor) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isBrandCorrectionDialogVisible = false
                    }) { Text(watermarkExtraCopy.cancel, color = dialogActionTextColor) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isAddSignatureDialogVisible) {
            AlertDialog(
                onDismissRequest = { isAddSignatureDialogVisible = false },
                title = { Text(watermarkExtraCopy.addSignatureTitle, color = primaryTextColor) },
                text = {
                    OutlinedTextField(
                        value = addSignatureInput,
                        onValueChange = { addSignatureInput = sanitizeEnglishSignature(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(watermarkExtraCopy.englishOnly, color = secondaryTextColor) },
                        colors = dialogTextFieldColors
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            val sanitized = sanitizeEnglishSignature(addSignatureInput)
                            if (sanitized.isNotBlank()) {
                                applySignatureSelection(sanitized)
                                selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.addSignatureFailed) }
                            }
                            isAddSignatureDialogVisible = false
                        }
                    ) { Text(watermarkExtraCopy.confirm, color = dialogActionTextColor) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        isAddSignatureDialogVisible = false
                    }) { Text(watermarkExtraCopy.cancel, color = dialogActionTextColor) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        val deletingSignature = pendingDeleteSignature
        if (!deletingSignature.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = { pendingDeleteSignature = null },
                title = { Text(watermarkExtraCopy.deleteSignatureTitle, color = primaryTextColor) },
                text = { Text(watermarkExtraCopy.deleteSignatureConfirm(deletingSignature), color = primaryTextColor) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHaptic()
                            signatureHistory.remove(deletingSignature)
                            persistSignatureHistory()
                            if (signatureText == deletingSignature) {
                                signatureText = signatureHistory.firstOrNull().orEmpty()
                                prefs.edit().putString("watermark_signature_text", signatureText).apply()
                                selectedUri?.let { rerenderForCurrentSelection(it, watermarkExtraCopy.updateSignatureFailed) }
                            }
                            pendingDeleteSignature = null
                        }
                    ) { Text(watermarkExtraCopy.delete, color = dialogActionTextColor) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        performHaptic()
                        pendingDeleteSignature = null
                    }) { Text(watermarkExtraCopy.cancel, color = dialogActionTextColor) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
}

@Composable
private fun FontPreviewLabel(
    text: String,
    assetPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val typeface = remember(context, assetPath) { loadTypefaceFromAssetPath(context, assetPath) }
    val textColor = LocalContentColor.current.toArgb()
    AndroidView(
        factory = { viewContext ->
            TextView(viewContext).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            }
        },
        update = { textView ->
            textView.text = text
            textView.typeface = typeface ?: Typeface.SANS_SERIF
            textView.setTextColor(textColor)
        },
        modifier = modifier
    )
}

private suspend fun renderWatermarkImageToCache(
    context: Context,
    sourceUri: Uri,
    template: WatermarkTemplatePreset,
    selectedBrandKey: String?,
    customLogoPath: String?,
    selectedVariantIndex: Int,
    showLogoWatermark: Boolean,
    showModelWatermark: Boolean,
    showDateWatermark: Boolean,
    showExifWatermark: Boolean,
    showLocationWatermark: Boolean,
    showCityLocationWatermark: Boolean,
    fullColorNoteText: String = "",
    modelLineOverride: String?,
    frameSideTopBorderRatio: Float,
    frameLayoutMode: WatermarkFrameLayoutMode,
    logoScaleFactor: Float,
    frameBackgroundColorIndex: Int,
    thinBorderColorIndex: Int,
    roundedBorderColorIndex: Int,
    borderCornerMode: BorderCornerMode,
    blurImageCornerRadiusRatio: Float,
    borderImageScaleFactor: Float,
    cutoutOverlayStyle: WatermarkCutoutOverlayStyle = WatermarkCutoutOverlayStyle.None,
    cutoutSubjectScaleFactor: Float = DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
    cutoutFrameAspectRatio: WatermarkCutoutFrameAspectRatio = DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO,
    subjectOutlineConfig: WatermarkSubjectOutlineConfig = WatermarkSubjectOutlineConfig(),
    floatingBackgroundColorIndex: Int,
    floatingStrokeColorIndex: Int,
    floatingImageCornerRadiusRatio: Float,
    filmFrameIndex: Int,
    polaroidFrameIndex: Int,
    colorPaletteStyleIndex: Int,
    colorPaletteVariantIndex: Int,
    colorPaletteRefreshNonce: Int = 0,
    colorPaletteHorizontalOffsetRatio: Float = 0f,
    colorPaletteVerticalOffsetRatio: Float = 0f,
    colorPaletteElementCount: Int = 6,
    colorPaletteShowHexValue: Boolean = true,
    colorPaletteShowRgbValue: Boolean = true,
    paperDotsConfig: WatermarkPaperDotsConfig = WatermarkPaperDotsConfig(),
    watermarkFontKey: String,
    signatureFontKey: String,
    signatureText: String,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float = 0f,
    signatureVerticalOffsetRatio: Float = 0f,
    signatureScaleFactor: Float = 1f,
    fullColorTextScaleFactor: Float = DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR,
    fullColorBackgroundColorOverride: Int? = null,
    fullColorUseGradientBackground: Boolean = false,
    fullColorGradientColorsOverride: Pair<Int, Int>? = null,
    fullColorImageOnTop: Boolean = false,
    fullColorHorizontalOffsetRatio: Float = 0f,
    fullColorVerticalOffsetRatio: Float = 0f,
    outputSizeOverride: Pair<Int, Int>?,
    exifInfoOverride: WatermarkExifInfo? = null
): WatermarkRenderOutput = withContext(Dispatchers.IO) {
    val sourceMimeType = context.contentResolver.getType(sourceUri)
    val outputFormat = chooseOutputFormat(sourceMimeType)
    val renderedBitmap = renderWatermarkBitmap(
        context = context,
        sourceUri = sourceUri,
        template = template,
        selectedBrandKey = selectedBrandKey,
        customLogoPath = customLogoPath,
        selectedVariantIndex = selectedVariantIndex,
        showLogoWatermark = showLogoWatermark,
        showModelWatermark = showModelWatermark,
        showDateWatermark = showDateWatermark,
        showExifWatermark = showExifWatermark,
        showLocationWatermark = showLocationWatermark,
        showCityLocationWatermark = showCityLocationWatermark,
        fullColorNoteText = fullColorNoteText,
        modelLineOverride = modelLineOverride,
        frameSideTopBorderRatio = frameSideTopBorderRatio,
        frameLayoutMode = frameLayoutMode,
        logoScaleFactor = logoScaleFactor,
        frameBackgroundColorIndex = frameBackgroundColorIndex,
        thinBorderColorIndex = thinBorderColorIndex,
        roundedBorderColorIndex = roundedBorderColorIndex,
        borderCornerMode = borderCornerMode,
        blurImageCornerRadiusRatio = blurImageCornerRadiusRatio,
        borderImageScaleFactor = borderImageScaleFactor,
        cutoutOverlayStyle = cutoutOverlayStyle,
        cutoutSubjectScaleFactor = cutoutSubjectScaleFactor,
        cutoutFrameAspectRatio = cutoutFrameAspectRatio,
        subjectOutlineConfig = subjectOutlineConfig,
        floatingBackgroundColorIndex = floatingBackgroundColorIndex,
        floatingStrokeColorIndex = floatingStrokeColorIndex,
        floatingImageCornerRadiusRatio = floatingImageCornerRadiusRatio,
        filmFrameIndex = filmFrameIndex,
        polaroidFrameIndex = polaroidFrameIndex,
        colorPaletteStyleIndex = colorPaletteStyleIndex,
        colorPaletteVariantIndex = colorPaletteVariantIndex,
        colorPaletteHorizontalOffsetRatio = colorPaletteHorizontalOffsetRatio,
        colorPaletteVerticalOffsetRatio = colorPaletteVerticalOffsetRatio,
        colorPaletteElementCount = colorPaletteElementCount,
        colorPaletteShowHexValue = colorPaletteShowHexValue,
        colorPaletteShowRgbValue = colorPaletteShowRgbValue,
        paperDotsConfig = paperDotsConfig,
        watermarkFontKey = watermarkFontKey,
        signatureFontKey = signatureFontKey,
        signatureText = signatureText,
        signatureColor = signatureColor,
        signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
        signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
        signatureScaleFactor = signatureScaleFactor,
        fullColorTextScaleFactor = fullColorTextScaleFactor,
        fullColorBackgroundColorOverride = fullColorBackgroundColorOverride,
        fullColorUseGradientBackground = fullColorUseGradientBackground,
        fullColorGradientColorsOverride = fullColorGradientColorsOverride,
        fullColorImageOnTop = fullColorImageOnTop,
        fullColorHorizontalOffsetRatio = fullColorHorizontalOffsetRatio,
        fullColorVerticalOffsetRatio = fullColorVerticalOffsetRatio,
        outputSizeOverride = outputSizeOverride,
        exifInfoOverride = exifInfoOverride,
        requestedDecodeMaxSidePx = computeRenderDecodeMaxSide(outputSizeOverride)
    )

    val tempFile = File(
        ensureWatermarkTempDir(context.cacheDir),
        "watermark_${template.name.lowercase(Locale.ROOT)}_${System.currentTimeMillis()}.${outputFormat.fileExtension}"
    )
    FileOutputStream(tempFile).use { output ->
        renderedBitmap.compress(outputFormat.compressFormat, outputFormat.quality, output)
    }
    copyPreservedExifMetadataToFile(
        resolver = context.contentResolver,
        sourceUri = sourceUri,
        targetFile = tempFile
    )
    renderedBitmap.recycle()

    WatermarkRenderOutput(
        outputFile = tempFile,
        mimeType = outputFormat.mimeType
    )
}

internal suspend fun renderWatermarkPreviewBitmap(
    context: Context,
    sourceUri: Uri? = null,
    sourceBitmapOverride: Bitmap? = null,
    preserveSourceBitmapOverride: Boolean = false,
    sourceIdentity: String = sourceUri?.toString() ?: "bitmap",
    template: WatermarkTemplatePreset,
    selectedBrandKey: String?,
    customLogoPath: String?,
    selectedVariantIndex: Int,
    showLogoWatermark: Boolean,
    showModelWatermark: Boolean,
    showDateWatermark: Boolean,
    showExifWatermark: Boolean,
    showLocationWatermark: Boolean,
    showCityLocationWatermark: Boolean,
    fullColorNoteText: String = "",
    modelLineOverride: String?,
    frameSideTopBorderRatio: Float,
    frameLayoutMode: WatermarkFrameLayoutMode,
    logoScaleFactor: Float,
    frameBackgroundColorIndex: Int,
    thinBorderColorIndex: Int,
    roundedBorderColorIndex: Int,
    borderCornerMode: BorderCornerMode,
    blurImageCornerRadiusRatio: Float,
    borderImageScaleFactor: Float,
    cutoutOverlayStyle: WatermarkCutoutOverlayStyle = WatermarkCutoutOverlayStyle.None,
    cutoutSubjectScaleFactor: Float = DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
    cutoutFrameAspectRatio: WatermarkCutoutFrameAspectRatio = DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO,
    subjectOutlineConfig: WatermarkSubjectOutlineConfig = WatermarkSubjectOutlineConfig(),
    floatingBackgroundColorIndex: Int,
    floatingStrokeColorIndex: Int,
    floatingImageCornerRadiusRatio: Float,
    filmFrameIndex: Int,
    polaroidFrameIndex: Int,
    colorPaletteStyleIndex: Int,
    colorPaletteVariantIndex: Int,
    colorPaletteRefreshNonce: Int = 0,
    colorPaletteHorizontalOffsetRatio: Float = 0f,
    colorPaletteVerticalOffsetRatio: Float = 0f,
    colorPaletteElementCount: Int = 6,
    colorPaletteShowHexValue: Boolean = true,
    colorPaletteShowRgbValue: Boolean = true,
    paperDotsConfig: WatermarkPaperDotsConfig = WatermarkPaperDotsConfig(),
    watermarkFontKey: String,
    signatureFontKey: String,
    signatureText: String,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float = 0f,
    signatureVerticalOffsetRatio: Float = 0f,
    signatureScaleFactor: Float = 1f,
    fullColorTextScaleFactor: Float = DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR,
    fullColorBackgroundColorOverride: Int? = null,
    fullColorUseGradientBackground: Boolean = false,
    fullColorGradientColorsOverride: Pair<Int, Int>? = null,
    fullColorImageOnTop: Boolean = false,
    fullColorHorizontalOffsetRatio: Float = 0f,
    fullColorVerticalOffsetRatio: Float = 0f,
    outputSizeOverride: Pair<Int, Int>?,
    exifInfoOverride: WatermarkExifInfo? = null,
    previewMaxSidePx: Int = WATERMARK_LIVE_PREVIEW_MAX_SIDE,
    cutoutForegroundOverride: Bitmap? = null,
    suppressFrameSidePaddingWhenFixedOutput: Boolean = false,
    enableColorPaletteCache: Boolean = false
): Bitmap {
    val colorPaletteCacheKey = if (enableColorPaletteCache && template == WatermarkTemplatePreset.ColorPalette) {
        "preview:${sourceIdentity}:$previewMaxSidePx:$colorPaletteRefreshNonce"
    } else {
        null
    }
    return renderWatermarkBitmap(
        context = context,
        sourceUri = sourceUri,
        sourceBitmapOverride = sourceBitmapOverride,
        template = template,
        selectedBrandKey = selectedBrandKey,
        customLogoPath = customLogoPath,
        selectedVariantIndex = selectedVariantIndex,
        showLogoWatermark = showLogoWatermark,
        showModelWatermark = showModelWatermark,
        showDateWatermark = showDateWatermark,
        showExifWatermark = showExifWatermark,
        showLocationWatermark = showLocationWatermark,
        showCityLocationWatermark = showCityLocationWatermark,
        fullColorNoteText = fullColorNoteText,
        modelLineOverride = modelLineOverride,
        frameSideTopBorderRatio = frameSideTopBorderRatio,
        frameLayoutMode = frameLayoutMode,
        logoScaleFactor = logoScaleFactor,
        frameBackgroundColorIndex = frameBackgroundColorIndex,
        thinBorderColorIndex = thinBorderColorIndex,
        roundedBorderColorIndex = roundedBorderColorIndex,
        borderCornerMode = borderCornerMode,
        blurImageCornerRadiusRatio = blurImageCornerRadiusRatio,
        borderImageScaleFactor = borderImageScaleFactor,
        cutoutOverlayStyle = cutoutOverlayStyle,
        cutoutSubjectScaleFactor = cutoutSubjectScaleFactor,
        cutoutFrameAspectRatio = cutoutFrameAspectRatio,
        subjectOutlineConfig = subjectOutlineConfig,
        floatingBackgroundColorIndex = floatingBackgroundColorIndex,
        floatingStrokeColorIndex = floatingStrokeColorIndex,
        floatingImageCornerRadiusRatio = floatingImageCornerRadiusRatio,
        filmFrameIndex = filmFrameIndex,
        polaroidFrameIndex = polaroidFrameIndex,
        colorPaletteStyleIndex = colorPaletteStyleIndex,
        colorPaletteVariantIndex = colorPaletteVariantIndex,
        colorPaletteHorizontalOffsetRatio = colorPaletteHorizontalOffsetRatio,
        colorPaletteVerticalOffsetRatio = colorPaletteVerticalOffsetRatio,
        colorPaletteElementCount = colorPaletteElementCount,
        colorPaletteShowHexValue = colorPaletteShowHexValue,
        colorPaletteShowRgbValue = colorPaletteShowRgbValue,
        paperDotsConfig = paperDotsConfig,
        watermarkFontKey = watermarkFontKey,
        signatureFontKey = signatureFontKey,
        signatureText = signatureText,
        signatureColor = signatureColor,
        signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
        signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
        signatureScaleFactor = signatureScaleFactor,
        fullColorTextScaleFactor = fullColorTextScaleFactor,
        fullColorBackgroundColorOverride = fullColorBackgroundColorOverride,
        fullColorUseGradientBackground = fullColorUseGradientBackground,
        fullColorGradientColorsOverride = fullColorGradientColorsOverride,
        fullColorImageOnTop = fullColorImageOnTop,
        fullColorHorizontalOffsetRatio = fullColorHorizontalOffsetRatio,
        fullColorVerticalOffsetRatio = fullColorVerticalOffsetRatio,
        outputSizeOverride = outputSizeOverride,
        exifInfoOverride = exifInfoOverride,
        requestedDecodeMaxSidePx = previewMaxSidePx,
        preserveSourceBitmapOverride = preserveSourceBitmapOverride,
        cutoutForegroundOverride = cutoutForegroundOverride,
        suppressFrameSidePaddingWhenFixedOutput = suppressFrameSidePaddingWhenFixedOutput,
        colorPaletteCacheKey = colorPaletteCacheKey
    )
}

private suspend fun renderWatermarkBitmap(
    context: Context,
    sourceUri: Uri? = null,
    sourceBitmapOverride: Bitmap? = null,
    preserveSourceBitmapOverride: Boolean = false,
    template: WatermarkTemplatePreset,
    selectedBrandKey: String?,
    customLogoPath: String?,
    selectedVariantIndex: Int,
    showLogoWatermark: Boolean,
    showModelWatermark: Boolean,
    showDateWatermark: Boolean,
    showExifWatermark: Boolean,
    showLocationWatermark: Boolean,
    showCityLocationWatermark: Boolean,
    fullColorNoteText: String = "",
    modelLineOverride: String?,
    frameSideTopBorderRatio: Float,
    frameLayoutMode: WatermarkFrameLayoutMode,
    logoScaleFactor: Float,
    frameBackgroundColorIndex: Int,
    thinBorderColorIndex: Int,
    roundedBorderColorIndex: Int,
    borderCornerMode: BorderCornerMode,
    blurImageCornerRadiusRatio: Float,
    borderImageScaleFactor: Float,
    cutoutOverlayStyle: WatermarkCutoutOverlayStyle = WatermarkCutoutOverlayStyle.None,
    cutoutSubjectScaleFactor: Float = DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
    cutoutFrameAspectRatio: WatermarkCutoutFrameAspectRatio = DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO,
    subjectOutlineConfig: WatermarkSubjectOutlineConfig = WatermarkSubjectOutlineConfig(),
    floatingBackgroundColorIndex: Int,
    floatingStrokeColorIndex: Int,
    floatingImageCornerRadiusRatio: Float,
    filmFrameIndex: Int,
    polaroidFrameIndex: Int,
    colorPaletteStyleIndex: Int,
    colorPaletteVariantIndex: Int,
    colorPaletteRefreshNonce: Int = 0,
    colorPaletteHorizontalOffsetRatio: Float = 0f,
    colorPaletteVerticalOffsetRatio: Float = 0f,
    colorPaletteElementCount: Int = 6,
    colorPaletteShowHexValue: Boolean = true,
    colorPaletteShowRgbValue: Boolean = true,
    paperDotsConfig: WatermarkPaperDotsConfig = WatermarkPaperDotsConfig(),
    watermarkFontKey: String,
    signatureFontKey: String,
    signatureText: String,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float = 0f,
    signatureVerticalOffsetRatio: Float = 0f,
    signatureScaleFactor: Float = 1f,
    fullColorTextScaleFactor: Float = DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR,
    fullColorBackgroundColorOverride: Int? = null,
    fullColorUseGradientBackground: Boolean = false,
    fullColorGradientColorsOverride: Pair<Int, Int>? = null,
    fullColorImageOnTop: Boolean = false,
    fullColorHorizontalOffsetRatio: Float = 0f,
    fullColorVerticalOffsetRatio: Float = 0f,
    outputSizeOverride: Pair<Int, Int>?,
    exifInfoOverride: WatermarkExifInfo? = null,
    requestedDecodeMaxSidePx: Int? = computeRenderDecodeMaxSide(outputSizeOverride),
    cutoutForegroundOverride: Bitmap? = null,
    suppressFrameSidePaddingWhenFixedOutput: Boolean = false,
    colorPaletteCacheKey: String? = null
): Bitmap = withContext(Dispatchers.IO) {
    val sourceBitmap = sourceBitmapOverride ?: decodeSourceBitmap(
        context = context,
        uri = sourceUri ?: throw IllegalArgumentException("缺少原图"),
        requestedMaxSidePx = requestedDecodeMaxSidePx
    )
    currentCoroutineContext().ensureActive()
    val exifInfo = exifInfoOverride ?: sourceUri?.let { readExifInfo(context, it) } ?: WatermarkExifInfo()
    val watermarkTypeface = loadWatermarkTypeface(context, watermarkFontKey)
    val signatureTypeface = loadSignatureTypeface(context, signatureFontKey)
    val signature = sanitizeEnglishSignature(signatureText)
    val resolvedModelLine = modelLineOverride?.trim().orEmpty().ifBlank {
        resolveDisplayModelLine(context, exifInfo)
    }
    val resolvedBrandKey = selectedBrandKey ?: detectBrandKey(exifInfo.make, exifInfo.model)
    val logoReferenceHeight = outputSizeOverride?.second?.coerceAtLeast(1) ?: sourceBitmap.height
    val logoBitmap = if (!showLogoWatermark) {
        null
    } else if (resolvedBrandKey == CUSTOM_LOGO_BRAND_KEY) {
        decodeCustomLogo(
            filePath = customLogoPath,
            referenceHeight = logoReferenceHeight,
            template = template
        )
    } else {
        val logoResId = resolveBrandLogoResId(resolvedBrandKey, selectedVariantIndex)
        logoResId?.let {
            decodeBrandLogo(
                context = context,
                resId = it,
                referenceHeight = logoReferenceHeight,
                template = template,
                brandKey = resolvedBrandKey
            )
        }
    }
    currentCoroutineContext().ensureActive()
    val generatedCutoutForeground = if (
        cutoutForegroundOverride == null &&
        template == WatermarkTemplatePreset.ThinWhiteBorder &&
        (
            cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None ||
                subjectOutlineConfig.enabled
            )
    ) {
        runCatching {
            generateStampAutoCutoutForegroundBitmap(
                context = context,
                source = sourceBitmap
            )
        }.getOrNull()
    } else {
        null
    }
    val cutoutForeground = cutoutForegroundOverride
        ?.takeIf { !it.isRecycled }
        ?: generatedCutoutForeground
    currentCoroutineContext().ensureActive()

    try {
        when (template) {
            WatermarkTemplatePreset.WatermarkFrame -> drawWatermarkFrameTemplate(
                sourceBitmap,
                exifInfo,
                logoBitmap,
                showLogoWatermark,
                showModelWatermark,
                showDateWatermark,
                showExifWatermark,
                showLocationWatermark,
                showCityLocationWatermark,
                resolvedModelLine,
                frameSideTopBorderRatio,
                frameLayoutMode,
                logoScaleFactor,
                backgroundColor = FLOATING_COLOR_OPTIONS.getOrElse(frameBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)) {
                    FLOATING_COLOR_OPTIONS.last()
                }.android,
                watermarkTypeface,
                signature,
                signatureTypeface,
                signatureColor,
                signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
                signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
                signatureScaleFactor = signatureScaleFactor,
                suppressSidePaddingWhenFixedOutput = suppressFrameSidePaddingWhenFixedOutput,
                fixedOutputSize = outputSizeOverride
            )
            WatermarkTemplatePreset.BlurBackground -> drawBlurBackgroundTemplate(
                sourceBitmap,
                exifInfo,
                logoBitmap,
                showLogoWatermark,
                showModelWatermark,
                showDateWatermark,
                showExifWatermark,
                showLocationWatermark,
                showCityLocationWatermark,
                resolvedModelLine,
                logoScaleFactor,
                watermarkTypeface,
                signature,
                signatureTypeface,
                signatureColor,
                signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
                signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
                signatureScaleFactor = signatureScaleFactor,
                imageCornerRadiusRatio = blurImageCornerRadiusRatio,
                fixedOutputSize = outputSizeOverride
            )
            WatermarkTemplatePreset.ThinWhiteBorder -> drawThinWhiteBorderTemplate(
                sourceBitmap,
                exifInfo,
                logoBitmap,
                signature,
                signatureTypeface,
                signatureColor,
                signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
                signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
                signatureScaleFactor = signatureScaleFactor,
                borderColor = if (cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None) {
                    resolveWatermarkCutoutBreakoutBackgroundColor(thinBorderColorIndex)
                } else {
                    FLOATING_COLOR_OPTIONS.getOrElse(thinBorderColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)) {
                        FLOATING_COLOR_OPTIONS.last()
                    }.android
                },
                cornerMode = borderCornerMode,
                imageScaleFactor = borderImageScaleFactor,
                cutoutForeground = cutoutForeground,
                cutoutOverlayStyle = cutoutOverlayStyle,
                cutoutSubjectScaleFactor = cutoutSubjectScaleFactor,
                cutoutFrameAspectRatio = cutoutFrameAspectRatio,
                subjectOutlineConfig = subjectOutlineConfig,
                fixedOutputSize = outputSizeOverride
            )
            WatermarkTemplatePreset.Floating -> drawFloatingTemplate(
                source = sourceBitmap,
                backgroundColor = FLOATING_COLOR_OPTIONS.getOrElse(floatingBackgroundColorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)) {
                    FLOATING_COLOR_OPTIONS.last()
                }.android,
                strokeColor = FLOATING_STROKE_OPTIONS.getOrElse(floatingStrokeColorIndex.coerceIn(0, FLOATING_STROKE_OPTIONS.lastIndex)) {
                    FLOATING_STROKE_OPTIONS[8]
                }.android,
                imageCornerRadiusRatio = floatingImageCornerRadiusRatio,
                fixedOutputSize = outputSizeOverride
            )
            WatermarkTemplatePreset.FilmRetro -> drawFilmRetroTemplate(
                context = context,
                source = sourceBitmap,
                frameIndex = filmFrameIndex
            )
            WatermarkTemplatePreset.PaperDots -> drawWatermarkPaperDotsTemplate(
                source = sourceBitmap,
                config = paperDotsConfig,
                fixedOutputSize = outputSizeOverride
            )
            WatermarkTemplatePreset.FullColor -> drawWatermarkFullColorTemplate(
                source = sourceBitmap,
                exif = exifInfo,
                showTime = showDateWatermark,
                showLocation = showCityLocationWatermark,
                noteText = fullColorNoteText,
                watermarkTypeface = watermarkTypeface,
                textScaleFactor = fullColorTextScaleFactor,
                backgroundColorOverride = fullColorBackgroundColorOverride,
                useGradientBackground = fullColorUseGradientBackground,
                gradientColorsOverride = fullColorGradientColorsOverride,
                imageOnTop = fullColorImageOnTop,
                horizontalOffsetRatio = fullColorHorizontalOffsetRatio,
                verticalOffsetRatio = fullColorVerticalOffsetRatio,
                fixedOutputSize = outputSizeOverride
            )
            WatermarkTemplatePreset.Polaroid -> drawPolaroidTemplate(
                context = context,
                source = sourceBitmap,
                frameIndex = polaroidFrameIndex
            )
            WatermarkTemplatePreset.ColorPalette -> drawWatermarkColorPaletteTemplate(
                context = context,
                source = sourceBitmap,
                style = resolveWatermarkColorPaletteStyle(colorPaletteStyleIndex),
                fixedOutputSize = outputSizeOverride,
                variantIndex = colorPaletteVariantIndex,
                refreshNonce = colorPaletteRefreshNonce,
                horizontalOffsetRatio = colorPaletteHorizontalOffsetRatio,
                verticalOffsetRatio = colorPaletteVerticalOffsetRatio,
                itemCount = colorPaletteElementCount,
                showHexValue = colorPaletteShowHexValue,
                showRgbValue = colorPaletteShowRgbValue,
                paletteCacheKey = colorPaletteCacheKey
            )
            WatermarkTemplatePreset.DirectWatermark -> drawDirectWatermarkTemplate(
                sourceBitmap,
                exifInfo,
                logoBitmap,
                showLogoWatermark,
                showModelWatermark,
                showDateWatermark,
                showExifWatermark,
                showLocationWatermark,
                showCityLocationWatermark,
                resolvedModelLine,
                logoScaleFactor,
                watermarkTypeface,
                signature,
                signatureTypeface,
                signatureColor,
                signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
                signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
                signatureScaleFactor = signatureScaleFactor,
                fixedOutputSize = outputSizeOverride
            )
        }
    } finally {
        if (generatedCutoutForeground != null && generatedCutoutForeground !== cutoutForegroundOverride) {
            generatedCutoutForeground.takeIf { !it.isRecycled }?.recycle()
        }
        logoBitmap?.recycle()
        if (sourceBitmapOverride == null || !preserveSourceBitmapOverride) {
            recycleBitmapQuietly(sourceBitmap)
        }
    }
}

internal fun resolveSignatureRenderSourceSize(
    source: Bitmap,
    signatureText: String,
    fixedOutputSize: Pair<Int, Int>?
): Pair<Int, Int>? {
    if (fixedOutputSize != null || signatureText.isBlank()) return null
    val sourceMaxSide = max(source.width, source.height).coerceAtLeast(1)
    if (sourceMaxSide >= WATERMARK_SIGNATURE_MIN_RENDER_SOURCE_SIDE) return null
    val scale = WATERMARK_SIGNATURE_MIN_RENDER_SOURCE_SIDE.toFloat() / sourceMaxSide.toFloat()
    return (
        (source.width * scale).roundToInt().coerceAtLeast(1) to
            (source.height * scale).roundToInt().coerceAtLeast(1)
        )
}

private fun drawWatermarkFrameTemplate(
    source: Bitmap,
    exif: WatermarkExifInfo,
    logo: Bitmap?,
    showLogoWatermark: Boolean,
    showModelWatermark: Boolean,
    showDateWatermark: Boolean,
    showExifWatermark: Boolean,
    showLocationWatermark: Boolean,
    showCityLocationWatermark: Boolean,
    modelLine: String,
    frameSideTopBorderRatio: Float,
    frameLayoutMode: WatermarkFrameLayoutMode,
    logoScaleFactor: Float,
    backgroundColor: Int,
    watermarkTypeface: Typeface?,
    signatureText: String,
    signatureTypeface: Typeface?,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float,
    signatureVerticalOffsetRatio: Float,
    signatureScaleFactor: Float,
    suppressSidePaddingWhenFixedOutput: Boolean = false,
    fixedOutputSize: Pair<Int, Int>? = null
): Bitmap {
    val sideTopRatio = frameSideTopBorderRatio.coerceIn(0f, 0.06f)
    val outWidth = fixedOutputSize?.first?.coerceAtLeast(1)
    val outHeight = fixedOutputSize?.second?.coerceAtLeast(1)
    val renderSourceSize = resolveSignatureRenderSourceSize(
        source = source,
        signatureText = signatureText,
        fixedOutputSize = fixedOutputSize
    )
    val renderSourceWidth = renderSourceSize?.first ?: source.width
    val renderSourceHeight = renderSourceSize?.second ?: source.height
    val baseDim = if (outWidth != null && outHeight != null) {
        max(outWidth, outHeight)
    } else {
        max(renderSourceWidth, renderSourceHeight)
    }
    val pad = (baseDim * sideTopRatio).roundToInt().coerceAtLeast(0)
    val barHeight = (baseDim * 0.168f).roundToInt().coerceAtLeast(132)
    val finalWidth = outWidth ?: (renderSourceWidth + pad * 2)
    val finalHeight = outHeight ?: (renderSourceHeight + pad + barHeight)
    val horizontalPad = if (fixedOutputSize != null && suppressSidePaddingWhenFixedOutput) 0 else pad
    val output = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    canvas.drawColor(backgroundColor)
    val imageRect = if (fixedOutputSize == null) {
        RectF(
            horizontalPad.toFloat(),
            pad.toFloat(),
            (horizontalPad + renderSourceWidth).toFloat(),
            (pad + renderSourceHeight).toFloat()
        )
    } else {
        RectF(
            horizontalPad.toFloat(),
            pad.toFloat(),
            (finalWidth - horizontalPad).toFloat(),
            (finalHeight - barHeight).toFloat()
        )
    }
    drawBitmapCenterCrop(canvas, source, imageRect)

    val barTop = imageRect.bottom
    val barRect = RectF(
        horizontalPad.toFloat(),
        barTop,
        (finalWidth - horizontalPad).toFloat(),
        finalHeight.toFloat()
    )
    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    canvas.drawRect(barRect, barPaint)

    drawWatermarkFooterStyle(
        canvas = canvas,
        targetRect = barRect,
        logo = if (showLogoWatermark) logo else null,
        cameraLine = if (showModelWatermark) modelLine else "",
        dateLine = if (showDateWatermark) exif.footerDateLine() else "",
        specLine = if (showExifWatermark) exif.footerSpecLine() else "",
        locationLine = when {
            showCityLocationWatermark -> exif.footerCityLocationLine()
            showLocationWatermark -> exif.footerLocationLine()
            else -> ""
        },
        layoutMode = frameLayoutMode,
        logoScaleFactor = logoScaleFactor,
        watermarkTypeface = watermarkTypeface,
        signatureText = signatureText,
        signatureTypeface = signatureTypeface,
        signatureColor = signatureColor,
        signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
        signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
        signatureScaleFactor = signatureScaleFactor
    )
    return output
}

private fun drawBlurBackgroundTemplate(
    source: Bitmap,
    exif: WatermarkExifInfo,
    logo: Bitmap?,
    showLogoWatermark: Boolean,
    showModelWatermark: Boolean,
    showDateWatermark: Boolean,
    showExifWatermark: Boolean,
    showLocationWatermark: Boolean,
    showCityLocationWatermark: Boolean,
    modelLine: String,
    logoScaleFactor: Float,
    watermarkTypeface: Typeface?,
    signatureText: String,
    signatureTypeface: Typeface?,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float,
    signatureVerticalOffsetRatio: Float,
    signatureScaleFactor: Float,
    imageCornerRadiusRatio: Float,
    fixedOutputSize: Pair<Int, Int>? = null
): Bitmap {
    val outWidth = fixedOutputSize?.first?.coerceAtLeast(1)
    val outHeight = fixedOutputSize?.second?.coerceAtLeast(1)
    val renderSourceSize = resolveSignatureRenderSourceSize(
        source = source,
        signatureText = signatureText,
        fixedOutputSize = fixedOutputSize
    )
    val renderSourceWidth = renderSourceSize?.first ?: source.width
    val renderSourceHeight = renderSourceSize?.second ?: source.height
    val baseDim = if (outWidth != null && outHeight != null) {
        max(outWidth, outHeight)
    } else {
        max(renderSourceWidth, renderSourceHeight)
    }
    val pad = (baseDim * 0.08f).roundToInt().coerceAtLeast(26)
    val barHeight = (baseDim * 0.13f).roundToInt().coerceAtLeast(108)
    val finalWidth = outWidth ?: (renderSourceWidth + pad * 2)
    val finalHeight = outHeight ?: (renderSourceHeight + pad * 2 + barHeight)
    val output = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val background = createGaussianBlurBitmap(source, finalWidth, finalHeight)
    canvas.drawBitmap(background, 0f, 0f, null)
    background.recycle()

    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.argb(92, 68, 72, 78) }
    canvas.drawRect(0f, 0f, finalWidth.toFloat(), finalHeight.toFloat(), overlayPaint)

    val imageRect = if (fixedOutputSize == null) {
        RectF(
            pad.toFloat(),
            pad.toFloat(),
            (pad + renderSourceWidth).toFloat(),
            (pad + renderSourceHeight).toFloat()
        )
    } else {
        RectF(
            pad.toFloat(),
            pad.toFloat(),
            (finalWidth - pad).toFloat(),
            (finalHeight - pad - barHeight).toFloat()
        )
    }
    val imageRadius = (
        minOf(imageRect.width(), imageRect.height()) *
            imageCornerRadiusRatio.coerceIn(0f, 0.18f)
        ).coerceAtLeast(0f)
    if (imageRadius > 0.5f) {
        drawRoundedCenterCropBitmap(canvas, source, imageRect, imageRadius)
    } else {
        drawBitmapCenterCrop(canvas, source, imageRect)
    }

    val overlayRect = RectF(
        pad.toFloat(),
        (imageRect.bottom + pad * 0.10f),
        (finalWidth - pad).toFloat(),
        (finalHeight - pad * 0.22f).toFloat()
    )
    if (signatureText.isNotBlank()) {
        drawSignatureBottomRight(
            canvas = canvas,
            targetRect = overlayRect,
            signatureText = signatureText,
            textColor = signatureColor ?: AndroidColor.WHITE,
            signatureTypeface = signatureTypeface,
            horizontalOffsetRatio = signatureHorizontalOffsetRatio,
            verticalOffsetRatio = signatureVerticalOffsetRatio,
            sizeScaleFactor = signatureScaleFactor
        )
    } else {
        val detailLine = buildList {
            if (showDateWatermark) {
                exif.footerDateLine().takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (showExifWatermark) {
                exif.footerSpecLine().takeIf { it.isNotBlank() }?.let { add(it) }
            }
            when {
                showCityLocationWatermark -> exif.footerCityLocationLine()
                showLocationWatermark -> exif.footerLocationLine()
                else -> ""
            }.takeIf { it.isNotBlank() }?.let { add(it) }
        }.distinct().joinToString("  ")
        drawMinimalWatermarkOverlay(
            canvas = canvas,
            targetRect = overlayRect,
            logo = logo,
            modelLine = modelLine.uppercase(Locale.getDefault()),
            exifLine = detailLine,
            showLogo = showLogoWatermark,
            showModel = showModelWatermark,
            showExif = detailLine.isNotBlank(),
            logoScaleFactor = logoScaleFactor,
            textColor = AndroidColor.WHITE,
            bottomSafeRatio = 0.20f,
            watermarkTypeface = watermarkTypeface
        )
    }
    return output
}

private fun drawThinWhiteBorderTemplate(
    source: Bitmap,
    @Suppress("UNUSED_PARAMETER") exif: WatermarkExifInfo,
    @Suppress("UNUSED_PARAMETER") logo: Bitmap?,
    signatureText: String,
    signatureTypeface: Typeface?,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float,
    signatureVerticalOffsetRatio: Float,
    signatureScaleFactor: Float,
    borderColor: Int,
    cornerMode: BorderCornerMode,
    imageScaleFactor: Float,
    cutoutForeground: Bitmap? = null,
    cutoutOverlayStyle: WatermarkCutoutOverlayStyle = WatermarkCutoutOverlayStyle.None,
    cutoutSubjectScaleFactor: Float = DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
    cutoutFrameAspectRatio: WatermarkCutoutFrameAspectRatio = DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO,
    subjectOutlineConfig: WatermarkSubjectOutlineConfig = WatermarkSubjectOutlineConfig(),
    fixedOutputSize: Pair<Int, Int>? = null
): Bitmap {
    val isCutoutBreakoutMode = cutoutOverlayStyle != WatermarkCutoutOverlayStyle.None
    if (isCutoutBreakoutMode) {
        return drawWatermarkCutoutBreakoutTemplate(
            source = source,
            signatureText = signatureText,
            signatureTypeface = signatureTypeface,
            signatureColor = signatureColor,
            signatureHorizontalOffsetRatio = signatureHorizontalOffsetRatio,
            signatureVerticalOffsetRatio = signatureVerticalOffsetRatio,
            signatureScaleFactor = signatureScaleFactor,
            backgroundColor = borderColor,
            imageScaleFactor = imageScaleFactor,
            foreground = cutoutForeground,
            style = cutoutOverlayStyle,
            subjectScaleFactor = cutoutSubjectScaleFactor,
            frameAspectRatio = cutoutFrameAspectRatio,
            fixedOutputSize = fixedOutputSize
        )
    }

    val outWidth = fixedOutputSize?.first?.coerceAtLeast(1)
    val outHeight = fixedOutputSize?.second?.coerceAtLeast(1)
    val renderSourceSize = resolveSignatureRenderSourceSize(
        source = source,
        signatureText = signatureText,
        fixedOutputSize = fixedOutputSize
    )
    val renderSourceWidth = renderSourceSize?.first ?: source.width
    val renderSourceHeight = renderSourceSize?.second ?: source.height
    val baseDim = if (outWidth != null && outHeight != null) {
        max(outWidth, outHeight)
    } else {
        max(renderSourceWidth, renderSourceHeight)
    }
    val showImageOuterBorder = !subjectOutlineConfig.enabled ||
        subjectOutlineConfig.outerBorderEnabled
    val border = if (showImageOuterBorder) {
        (baseDim * 0.015f).roundToInt().coerceAtLeast(8)
    } else {
        0
    }
    val finalWidth = outWidth ?: (renderSourceWidth + border * 2)
    val finalHeight = outHeight ?: (renderSourceHeight + border * 2)
    val output = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    canvas.drawColor(borderColor)
    val baseRect = if (fixedOutputSize == null) {
        RectF(border.toFloat(), border.toFloat(), (border + renderSourceWidth).toFloat(), (border + renderSourceHeight).toFloat())
    } else {
        RectF(border.toFloat(), border.toFloat(), (finalWidth - border).toFloat(), (finalHeight - border).toFloat())
    }
    val scale = if (showImageOuterBorder) {
        imageScaleFactor.coerceIn(0.78f, 1f)
    } else {
        1f
    }
    val imageRect = if (scale < 0.999f) {
        val w = baseRect.width() * scale
        val h = baseRect.height() * scale
        RectF(
            baseRect.centerX() - w / 2f,
            baseRect.centerY() - h / 2f,
            baseRect.centerX() + w / 2f,
            baseRect.centerY() + h / 2f
        )
    } else {
        baseRect
    }
    if (showImageOuterBorder && cornerMode == BorderCornerMode.InnerRounded) {
        val radius = (minOf(imageRect.width(), imageRect.height()) * 0.06f).coerceAtLeast(14f)
        drawRoundedCenterCropBitmap(canvas, source, imageRect, radius)
    } else {
        drawBitmapCenterCrop(canvas, source, imageRect)
    }
    if (
        subjectOutlineConfig.enabled &&
        cutoutForeground != null &&
        !cutoutForeground.isRecycled
    ) {
        val outlineColor = FLOATING_COLOR_OPTIONS.getOrElse(
            subjectOutlineConfig.colorIndex.coerceIn(0, FLOATING_COLOR_OPTIONS.lastIndex)
        ) {
            FLOATING_COLOR_OPTIONS[DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX]
        }.android
        drawWatermarkSubjectOutline(
            canvas = canvas,
            foreground = cutoutForeground,
            anchorRect = imageRect,
            outlineColor = outlineColor,
            config = subjectOutlineConfig
        )
    }
    if (signatureText.isNotBlank()) {
        val area = RectF(
            border.toFloat(),
            (imageRect.top + imageRect.height() * 0.78f),
            imageRect.right,
            (imageRect.bottom - border * 0.25f)
        )
        drawSignatureBottomRight(
            canvas = canvas,
            targetRect = area,
            signatureText = signatureText,
            textColor = signatureColor ?: AndroidColor.argb(255, 38, 38, 38),
            signatureTypeface = signatureTypeface,
            horizontalOffsetRatio = signatureHorizontalOffsetRatio,
            verticalOffsetRatio = signatureVerticalOffsetRatio,
            sizeScaleFactor = signatureScaleFactor
        )
    }
    return output
}

private fun drawFloatingTemplate(
    source: Bitmap,
    backgroundColor: Int,
    strokeColor: Int,
    imageCornerRadiusRatio: Float,
    fixedOutputSize: Pair<Int, Int>? = null
): Bitmap {
    val outWidth = fixedOutputSize?.first?.coerceAtLeast(1)
    val outHeight = fixedOutputSize?.second?.coerceAtLeast(1)
    val baseDim = if (outWidth != null && outHeight != null) {
        max(outWidth, outHeight)
    } else {
        max(source.width, source.height)
    }
    val pad = (baseDim * 0.115f).roundToInt().coerceAtLeast(42)
    val shadowOffset = (baseDim * 0.018f).coerceAtLeast(6f)
    val shadowBlur = (baseDim * 0.03f).coerceAtLeast(12f)
    val finalWidth = outWidth ?: (source.width + pad * 2)
    val finalHeight = outHeight ?: (source.height + pad * 2)
    val imageCorner = (
        minOf(
            (finalWidth - pad * 2).toFloat().coerceAtLeast(1f),
            (finalHeight - pad * 2).toFloat().coerceAtLeast(1f)
        ) * imageCornerRadiusRatio.coerceIn(0f, 0.12f)
        ).coerceAtLeast(0f)
    val output = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    canvas.drawColor(backgroundColor)
    val photoRect = RectF(
        pad.toFloat(),
        pad.toFloat(),
        (finalWidth - pad).toFloat(),
        (finalHeight - pad).toFloat()
    )
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(34, 0, 0, 0)
        setShadowLayer(
            shadowBlur,
            0f,
            shadowOffset,
            AndroidColor.argb(105, 0, 0, 0)
        )
    }
    canvas.drawRoundRect(photoRect, imageCorner, imageCorner, shadowPaint)
    val strokeWidth = (baseDim * 0.0042f).coerceAtLeast(1.2f)
    val hasStroke = AndroidColor.alpha(strokeColor) > 0
    if (hasStroke) {
        val strokeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = strokeColor }
        canvas.drawRoundRect(photoRect, imageCorner, imageCorner, strokeFillPaint)
        val innerInset = strokeWidth
        val innerRect = RectF(
            photoRect.left + innerInset,
            photoRect.top + innerInset,
            photoRect.right - innerInset,
            photoRect.bottom - innerInset
        )
        val innerCorner = (imageCorner - innerInset * 0.65f).coerceAtLeast(0f)
        if (innerRect.width() > 2f && innerRect.height() > 2f) {
            if (innerCorner > 0.5f) {
                drawRoundedCenterCropBitmap(canvas, source, innerRect, innerCorner)
            } else {
                drawBitmapCenterCrop(canvas, source, innerRect)
            }
        } else {
            if (imageCorner > 0.5f) {
                drawRoundedCenterCropBitmap(canvas, source, photoRect, imageCorner)
            } else {
                drawBitmapCenterCrop(canvas, source, photoRect)
            }
        }
    } else {
        if (imageCorner > 0.5f) {
            drawRoundedCenterCropBitmap(canvas, source, photoRect, imageCorner)
        } else {
            drawBitmapCenterCrop(canvas, source, photoRect)
        }
    }
    return output
}

private fun drawFilmRetroTemplate(
    context: Context,
    source: Bitmap,
    frameIndex: Int
): Bitmap {
    val resId = FILM_FRAME_RES_IDS[frameIndex.coerceIn(0, FILM_FRAME_RES_IDS.lastIndex)]
    val overlayRaw = BitmapFactory.decodeResource(context.resources, resId) ?: return source.copy(Bitmap.Config.ARGB_8888, true)
    if (overlayRaw.width <= 0 || overlayRaw.height <= 0) {
        overlayRaw.recycle()
        return source.copy(Bitmap.Config.ARGB_8888, true)
    }

    // Keep frame aspect ratio fixed, then center-crop photo into this ratio (no stretching).
    val frameAspect = overlayRaw.width.toFloat() / overlayRaw.height.toFloat()
    val sourceAspect = source.width.toFloat() / source.height.toFloat()
    val outWidth: Int
    val outHeight: Int
    if (sourceAspect >= frameAspect) {
        outHeight = source.height
        outWidth = (outHeight * frameAspect).roundToInt().coerceAtLeast(1)
    } else {
        outWidth = source.width
        outHeight = (outWidth / frameAspect).roundToInt().coerceAtLeast(1)
    }

    val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val srcRect = if (sourceAspect > frameAspect) {
        val cropWidth = (source.height * frameAspect).roundToInt().coerceAtLeast(1).coerceAtMost(source.width)
        val left = ((source.width - cropWidth) / 2f).roundToInt().coerceAtLeast(0)
        android.graphics.Rect(left, 0, (left + cropWidth).coerceAtMost(source.width), source.height)
    } else {
        val cropHeight = (source.width / frameAspect).roundToInt().coerceAtLeast(1).coerceAtMost(source.height)
        val top = ((source.height - cropHeight) / 2f).roundToInt().coerceAtLeast(0)
        android.graphics.Rect(0, top, source.width, (top + cropHeight).coerceAtMost(source.height))
    }
    val dstRect = android.graphics.Rect(0, 0, outWidth, outHeight)
    canvas.drawBitmap(source, srcRect, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))

    val overlay = Bitmap.createScaledBitmap(overlayRaw, outWidth, outHeight, true)
    overlayRaw.recycle()
    canvas.drawBitmap(overlay, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
    overlay.recycle()
    return output
}

private fun drawPolaroidTemplate(
    context: Context,
    source: Bitmap,
    frameIndex: Int
): Bitmap {
    val resId = POLAROID_FRAME_RES_IDS[frameIndex.coerceIn(0, POLAROID_FRAME_RES_IDS.lastIndex)]
    val frame = BitmapFactory.decodeResource(context.resources, resId) ?: return source.copy(Bitmap.Config.ARGB_8888, true)
    if (frame.width <= 0 || frame.height <= 0) {
        frame.recycle()
        return source.copy(Bitmap.Config.ARGB_8888, true)
    }
    val spec = POLAROID_FRAME_SPEC_MAP[resId] ?: PolaroidFrameSpec(
        leftRatio = 0.06f,
        topRatio = 0.06f,
        rightRatio = 0.94f,
        bottomRatio = 0.92f,
        darkCenterOverlay = false
    )
    val output = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val fullRect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
    val photoRect = RectF(
        output.width * spec.leftRatio,
        output.height * spec.topRatio,
        output.width * spec.rightRatio,
        output.height * spec.bottomRatio
    )
    // Use slight bleed instead of inset to avoid visible "layer break" on torn/irregular inner edges.
    val bleedRatio = if (spec.darkCenterOverlay) 0.022f else 0.012f
    val bleed = minOf(photoRect.width(), photoRect.height()) * bleedRatio
    val safePhotoRect = RectF(
        (photoRect.left - bleed).coerceAtLeast(0f),
        (photoRect.top - bleed).coerceAtLeast(0f),
        (photoRect.right + bleed).coerceAtMost(output.width.toFloat()),
        (photoRect.bottom + bleed).coerceAtMost(output.height.toFloat())
    )
    canvas.drawBitmap(frame, null, fullRect, Paint(Paint.ANTI_ALIAS_FLAG))
    drawBitmapCenterCrop(canvas, source, safePhotoRect)
    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = if (spec.darkCenterOverlay) 255 else 208
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blendMode = if (spec.darkCenterOverlay) BlendMode.SCREEN else BlendMode.MULTIPLY
        } else {
            xfermode = PorterDuffXfermode(if (spec.darkCenterOverlay) PorterDuff.Mode.SCREEN else PorterDuff.Mode.MULTIPLY)
        }
    }
    canvas.drawBitmap(frame, null, fullRect, overlayPaint)
    frame.recycle()
    return output
}

private fun drawDirectWatermarkTemplate(
    source: Bitmap,
    exif: WatermarkExifInfo,
    logo: Bitmap?,
    showLogoWatermark: Boolean,
    showModelWatermark: Boolean,
    showDateWatermark: Boolean,
    showExifWatermark: Boolean,
    showLocationWatermark: Boolean,
    showCityLocationWatermark: Boolean,
    modelLine: String,
    logoScaleFactor: Float,
    watermarkTypeface: Typeface?,
    signatureText: String,
    signatureTypeface: Typeface?,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float,
    signatureVerticalOffsetRatio: Float,
    signatureScaleFactor: Float,
    fixedOutputSize: Pair<Int, Int>? = null
): Bitmap {
    val renderSourceSize = resolveSignatureRenderSourceSize(
        source = source,
        signatureText = signatureText,
        fixedOutputSize = fixedOutputSize
    )
    val output = when {
        fixedOutputSize != null -> Bitmap.createBitmap(
            fixedOutputSize.first.coerceAtLeast(1),
            fixedOutputSize.second.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        renderSourceSize != null -> Bitmap.createBitmap(
            renderSourceSize.first,
            renderSourceSize.second,
            Bitmap.Config.ARGB_8888
        ).apply {
            Canvas(this).drawBitmap(
                source,
                null,
                android.graphics.Rect(0, 0, width, height),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
        }
        else -> source.copy(Bitmap.Config.ARGB_8888, true)
    }
    val canvas = Canvas(output)
    if (fixedOutputSize != null) {
        drawBitmapCenterCrop(
            canvas = canvas,
            source = source,
            dstRect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
        )
    }
    if (signatureText.isNotBlank()) {
        drawSignatureBottomRight(
            canvas = canvas,
            targetRect = RectF(
                output.width * 0.06f,
                output.height * 0.78f,
                output.width * 0.94f,
                output.height * 0.98f
            ),
            signatureText = signatureText,
            textColor = signatureColor ?: AndroidColor.WHITE,
            signatureTypeface = signatureTypeface,
            horizontalOffsetRatio = signatureHorizontalOffsetRatio,
            verticalOffsetRatio = signatureVerticalOffsetRatio,
            sizeScaleFactor = signatureScaleFactor
        )
        return output
    }
    val textRect = RectF(
        output.width * 0.12f,
        output.height * 0.81f,
        output.width * 0.88f,
        output.height * 0.97f
    )
    val detailLine = buildList {
        if (showDateWatermark) {
            exif.footerDateLine().takeIf { it.isNotBlank() }?.let { add(it) }
        }
        if (showExifWatermark) {
            exif.footerSpecLine().takeIf { it.isNotBlank() }?.let { add(it) }
        }
        when {
            showCityLocationWatermark -> exif.footerCityLocationLine()
            showLocationWatermark -> exif.footerLocationLine()
            else -> ""
        }.takeIf { it.isNotBlank() }?.let { add(it) }
    }.distinct().joinToString("  ")
    drawMinimalWatermarkOverlay(
        canvas = canvas,
        targetRect = textRect,
        logo = logo,
        modelLine = modelLine.uppercase(Locale.getDefault()),
        exifLine = detailLine,
        showLogo = showLogoWatermark,
        showModel = showModelWatermark,
        showExif = detailLine.isNotBlank(),
        logoScaleFactor = logoScaleFactor,
        textColor = AndroidColor.WHITE,
        bottomSafeRatio = 0.14f,
        watermarkTypeface = watermarkTypeface
    )
    return output
}

private fun drawWatermarkPlate(
    canvas: Canvas,
    targetRect: RectF,
    logo: Bitmap?,
    cameraLine: String,
    detailLine: String,
    darkBackground: Boolean,
    textColor: Int
) {
    if (targetRect.width() <= 0 || targetRect.height() <= 0) return

    if (darkBackground) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(178, 0, 0, 0)
        }
        val radius = targetRect.height() * 0.22f
        canvas.drawRoundRect(targetRect, radius, radius, bgPaint)
    }

    val horizontalPadding = targetRect.width() * 0.045f
    val verticalPadding = targetRect.height() * 0.14f
    val logoHeight = targetRect.height() * 0.30f
    var textStartX = targetRect.left + horizontalPadding

    if (logo != null && logo.width > 0 && logo.height > 0) {
        val logoScale = logoHeight / logo.height.toFloat()
        val logoWidth = (logo.width * logoScale).roundToInt().coerceAtLeast(1)
        val scaledLogo = Bitmap.createScaledBitmap(
            logo,
            logoWidth,
            logoHeight.roundToInt().coerceAtLeast(1),
            true
        )
        val logoLeft = targetRect.left + horizontalPadding
        val logoTop = targetRect.top + verticalPadding
        canvas.drawBitmap(scaledLogo, logoLeft, logoTop, null)
        textStartX = logoLeft + scaledLogo.width + horizontalPadding * 0.75f
        scaledLogo.recycle()
    }

    val cameraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = (targetRect.height() * 0.22f).coerceAtLeast(12f)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        alpha = 230
        textSize = (targetRect.height() * 0.16f).coerceAtLeast(10f)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
    }

    val textMaxWidth = (targetRect.right - horizontalPadding - textStartX).coerceAtLeast(targetRect.width() * 0.20f)
    fitTextSizeToWidth(
        paint = cameraPaint,
        text = cameraLine,
        maxWidth = textMaxWidth,
        minTextSize = (targetRect.height() * 0.12f).coerceAtLeast(9f),
        maxTextSize = (targetRect.height() * 0.24f).coerceAtLeast(12f)
    )
    fitTextSizeToWidth(
        paint = detailPaint,
        text = detailLine,
        maxWidth = textMaxWidth,
        minTextSize = (targetRect.height() * 0.10f).coerceAtLeast(8f),
        maxTextSize = (targetRect.height() * 0.18f).coerceAtLeast(10f)
    )
    val cameraText = ellipsizeForPaint(cameraLine, cameraPaint, textMaxWidth)
    val detailText = ellipsizeForPaint(detailLine, detailPaint, textMaxWidth)
    val cameraBaseline = targetRect.top + verticalPadding - cameraPaint.ascent()
    val detailBaseline = targetRect.bottom - verticalPadding - detailPaint.descent()
    canvas.drawText(cameraText, textStartX, cameraBaseline, cameraPaint)
    canvas.drawText(detailText, textStartX, detailBaseline, detailPaint)
}

private fun drawWatermarkFooterStyle(
    canvas: Canvas,
    targetRect: RectF,
    logo: Bitmap?,
    cameraLine: String,
    dateLine: String,
    specLine: String,
    locationLine: String,
    layoutMode: WatermarkFrameLayoutMode,
    logoScaleFactor: Float,
    watermarkTypeface: Typeface?,
    signatureText: String,
    signatureTypeface: Typeface?,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float,
    signatureVerticalOffsetRatio: Float,
    signatureScaleFactor: Float
) {
    if (targetRect.width() <= 0f || targetRect.height() <= 0f) return

    val textColor = AndroidColor.argb(255, 28, 28, 28)
    val subTextColor = AndroidColor.argb(255, 122, 122, 122)
    val modelTextRaw = cameraLine.trim()
    val exifLines = listOf(dateLine, specLine, locationLine).filter { it.isNotBlank() }

    if (signatureText.isNotBlank()) {
        drawSignatureBottomRight(
            canvas = canvas,
            targetRect = RectF(
                targetRect.left + targetRect.width() * 0.06f,
                targetRect.top + targetRect.height() * 0.10f,
                targetRect.right - targetRect.width() * 0.04f,
                targetRect.bottom - targetRect.height() * 0.08f
            ),
            signatureText = signatureText,
            textColor = signatureColor ?: subTextColor,
            signatureTypeface = signatureTypeface,
            horizontalOffsetRatio = signatureHorizontalOffsetRatio,
            verticalOffsetRatio = signatureVerticalOffsetRatio,
            sizeScaleFactor = signatureScaleFactor
        )
        return
    }

    if (layoutMode == WatermarkFrameLayoutMode.Center) {
        drawCenteredFooterOverlay(
            canvas = canvas,
            targetRect = targetRect,
            logo = logo,
            modelLine = modelTextRaw,
            exifLine = exifLines.joinToString("  |  "),
            logoScaleFactor = logoScaleFactor,
            textColor = textColor,
            watermarkTypeface = watermarkTypeface
        )
        return
    }

    if (modelTextRaw.isBlank() && logo == null && exifLines.isEmpty()) return

    val edgeInset = if (targetRect.left <= 1f) targetRect.width() * 0.024f else 0f
    val contentLeft = targetRect.left + edgeInset
    val contentRight = targetRect.right - edgeInset
    val centerY = targetRect.centerY()
    val splitX = targetRect.left + targetRect.width() * 0.50f

    if (layoutMode == WatermarkFrameLayoutMode.Left) {
        val leftRect = RectF(contentLeft, targetRect.top, splitX, targetRect.bottom)
        val rightRect = RectF(splitX, targetRect.top, contentRight, targetRect.bottom)
        val blockPadding = leftRect.width() * 0.08f
        val modelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subTextColor
            textAlign = Paint.Align.LEFT
            textSize = (targetRect.height() * 0.105f).coerceAtLeast(6.8f)
            typeface = resolveWatermarkTextTypeface(watermarkTypeface, modelTextRaw)
        }
        val modelMaxW = (leftRect.width() * 0.84f).coerceAtLeast(targetRect.width() * 0.16f)
        val modelText = ellipsizeForPaint(modelTextRaw, modelPaint, modelMaxW)
        fitTextSizeToWidth(
            paint = modelPaint,
            text = modelTextRaw,
            maxWidth = modelMaxW,
            minTextSize = (targetRect.height() * 0.074f).coerceAtLeast(6.0f),
            maxTextSize = (targetRect.height() * 0.115f).coerceAtLeast(7.8f)
        )

        var logoBitmap: Bitmap? = null
        if (logo != null && logo.width > 0 && logo.height > 0) {
            val logoDesiredH = (targetRect.height() * 0.18f) * logoScaleFactor.coerceIn(0.72f, 1.85f)
            val scale = logoDesiredH.coerceAtLeast(8f) / logo.height.toFloat()
            val w = (logo.width * scale).roundToInt().coerceAtLeast(1)
            val h = logoDesiredH.roundToInt().coerceAtLeast(1)
            logoBitmap = Bitmap.createScaledBitmap(logo, w, h, true)
        }
        val modelH = if (modelText.isNotBlank()) modelPaint.descent() - modelPaint.ascent() else 0f
        val logoH = logoBitmap?.height?.toFloat() ?: 0f
        val stackGap = targetRect.height() * 0.03f
        val stackCount = listOf(logoBitmap != null, modelText.isNotBlank()).count { it }
        val stackHeight = logoH + modelH + if (stackCount >= 2) stackGap else 0f
        var stackTop = centerY - stackHeight / 2f
        val stackWidth = maxOf(
            (logoBitmap?.width ?: 0).toFloat(),
            if (modelText.isNotBlank()) modelPaint.measureText(modelText) else 0f
        )
        val blockX = if (exifLines.isEmpty()) {
            (contentRight - blockPadding - stackWidth).coerceAtLeast(contentLeft + blockPadding)
        } else {
            leftRect.left + blockPadding
        }
        if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, blockX, stackTop, null)
            stackTop += logoBitmap.height + stackGap
            logoBitmap.recycle()
        }
        if (modelText.isNotBlank()) {
            canvas.drawText(modelText, blockX, stackTop - modelPaint.ascent(), modelPaint)
        }

        if (exifLines.isNotEmpty()) {
            val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subTextColor
                textSize = (targetRect.height() * 0.102f).coerceAtLeast(6.8f)
                textAlign = Paint.Align.RIGHT
                typeface = resolveWatermarkTextTypeface(
                    preferredTypeface = watermarkTypeface,
                    text = exifLines.joinToString("  ")
                )
            }
            val textEndX = rightRect.right - rightRect.width() * 0.06f
            val maxTextW = (textEndX - (rightRect.left + rightRect.width() * 0.08f)).coerceAtLeast(targetRect.width() * 0.16f)
            fitTextSizeToWidth(
                paint = exifPaint,
                text = exifLines.maxByOrNull { it.length }.orEmpty(),
                maxWidth = maxTextW,
                minTextSize = (targetRect.height() * 0.074f).coerceAtLeast(6.0f),
                maxTextSize = (targetRect.height() * 0.115f).coerceAtLeast(7.8f)
            )
            val lineHeight = exifPaint.descent() - exifPaint.ascent()
            val lineGap = targetRect.height() * 0.026f
            val blockHeight = exifLines.size * lineHeight + (exifLines.size - 1) * lineGap
            var lineTop = centerY - blockHeight / 2f
            exifLines.forEach { line ->
                val text = ellipsizeForPaint(line, exifPaint, maxTextW)
                canvas.drawText(text, textEndX, lineTop - exifPaint.ascent(), exifPaint)
                lineTop += lineHeight + lineGap
            }
        }
        return
    }

    val cameraRect = RectF(contentLeft, targetRect.top, splitX, targetRect.bottom)
    val infoRect = RectF(splitX, targetRect.top, contentRight, targetRect.bottom)

    if (modelTextRaw.isNotBlank()) {
        val cameraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subTextColor
            textAlign = Paint.Align.LEFT
            textSize = (targetRect.height() * 0.102f).coerceAtLeast(6.8f)
            typeface = resolveWatermarkTextTypeface(watermarkTypeface, modelTextRaw)
        }
        val cameraInset = cameraRect.width() * 0.05f
        val cameraMaxW = (cameraRect.width() - cameraInset * 2f).coerceAtLeast(targetRect.width() * 0.16f)
        fitTextSizeToWidth(
            paint = cameraPaint,
            text = modelTextRaw,
            maxWidth = cameraMaxW,
            minTextSize = (targetRect.height() * 0.074f).coerceAtLeast(6.0f),
            maxTextSize = (targetRect.height() * 0.115f).coerceAtLeast(7.8f)
        )
        val cameraText = ellipsizeForPaint(modelTextRaw, cameraPaint, cameraMaxW)
        val cameraX = cameraRect.left + cameraInset
        val cameraY = centerY - (cameraPaint.ascent() + cameraPaint.descent()) / 2f
        canvas.drawText(cameraText, cameraX, cameraY, cameraPaint)
    }

    val blockPadding = infoRect.width() * 0.08f
    val logoDesiredHeight = (targetRect.height() * 0.19f) * logoScaleFactor.coerceIn(0.72f, 1.85f)
    var logoBitmap: Bitmap? = null
    if (logo != null && logo.width > 0 && logo.height > 0) {
        val scale = logoDesiredHeight.coerceAtLeast(8f) / logo.height.toFloat()
        val width = (logo.width * scale).roundToInt().coerceAtLeast(1)
        val height = logoDesiredHeight.roundToInt().coerceAtLeast(1)
        logoBitmap = Bitmap.createScaledBitmap(logo, width, height, true)
    }

    val logoLeft = infoRect.left + blockPadding
    val logoTop = centerY - (logoBitmap?.height ?: 0) / 2f
    if (logoBitmap != null) {
        canvas.drawBitmap(logoBitmap, logoLeft, logoTop, null)
    }

    val dividerGap = infoRect.width() * 0.04f
    val dividerX = (logoLeft + (logoBitmap?.width ?: 0).toFloat() + dividerGap).coerceAtMost(infoRect.right - blockPadding * 1.6f)

    if (exifLines.isNotEmpty()) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(52, 0, 0, 0)
            strokeWidth = (targetRect.height() * 0.010f).coerceAtLeast(1f)
        }
        canvas.drawLine(
            dividerX,
            targetRect.top + targetRect.height() * 0.24f,
            dividerX,
            targetRect.bottom - targetRect.height() * 0.24f,
            dividerPaint
        )
        val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subTextColor
            textSize = (targetRect.height() * 0.105f).coerceAtLeast(7.0f)
            textAlign = Paint.Align.LEFT
            typeface = resolveWatermarkTextTypeface(
                preferredTypeface = watermarkTypeface,
                text = exifLines.joinToString("  ")
            )
        }
        val textStartX = dividerX + dividerGap
        val maxTextW = (infoRect.right - blockPadding - textStartX).coerceAtLeast(targetRect.width() * 0.16f)
        fitTextSizeToWidth(
            paint = exifPaint,
            text = exifLines.maxByOrNull { it.length }.orEmpty(),
            maxWidth = maxTextW,
            minTextSize = (targetRect.height() * 0.076f).coerceAtLeast(6.2f),
            maxTextSize = (targetRect.height() * 0.118f).coerceAtLeast(8f)
        )
        val lineHeight = exifPaint.descent() - exifPaint.ascent()
        val lineGap = targetRect.height() * 0.026f
        val blockHeight = exifLines.size * lineHeight + (exifLines.size - 1) * lineGap
        var lineTop = centerY - blockHeight / 2f
        exifLines.forEach { line ->
            val text = ellipsizeForPaint(line, exifPaint, maxTextW)
            canvas.drawText(text, textStartX, lineTop - exifPaint.ascent(), exifPaint)
            lineTop += lineHeight + lineGap
        }
    }
    logoBitmap?.recycle()
}

private fun drawMinimalWatermarkOverlay(
    canvas: Canvas,
    targetRect: RectF,
    logo: Bitmap?,
    modelLine: String,
    exifLine: String,
    showLogo: Boolean,
    showModel: Boolean,
    showExif: Boolean,
    logoScaleFactor: Float,
    textColor: Int,
    bottomSafeRatio: Float
    ,
    watermarkTypeface: Typeface?
) {
    if (targetRect.width() <= 0f || targetRect.height() <= 0f) return
    val showModelFinal = showModel && modelLine.isNotBlank()
    val showExifFinal = showExif && exifLine.isNotBlank()
    if (!showLogo && !showModelFinal && !showExifFinal) return

    val centerX = targetRect.centerX()
    val maxTextWidth = targetRect.width() * 0.68f
    val gap = targetRect.height() * 0.034f
    val shadowColor = AndroidColor.argb(110, 0, 0, 0)

    val modelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = resolveWatermarkTextTypeface(
            preferredTypeface = watermarkTypeface,
            text = modelLine,
            bold = true
        )
        textSize = (targetRect.height() * 0.105f).coerceAtLeast(9f)
        setShadowLayer(2.2f, 0f, 1f, shadowColor)
    }
    val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = resolveWatermarkTextTypeface(watermarkTypeface, exifLine)
        textSize = (targetRect.height() * 0.092f).coerceAtLeast(8f)
        setShadowLayer(2f, 0f, 1f, shadowColor)
    }
    fitTextSizeToWidth(
        paint = modelPaint,
        text = modelLine,
        maxWidth = maxTextWidth,
        minTextSize = (targetRect.height() * 0.070f).coerceAtLeast(7.5f),
        maxTextSize = (targetRect.height() * 0.125f).coerceAtLeast(10f)
    )
    fitTextSizeToWidth(
        paint = exifPaint,
        text = exifLine,
        maxWidth = maxTextWidth,
        minTextSize = (targetRect.height() * 0.062f).coerceAtLeast(7.2f),
        maxTextSize = (targetRect.height() * 0.110f).coerceAtLeast(9.2f)
    )
    val modelText = ellipsizeForPaint(modelLine, modelPaint, maxTextWidth)
    val exifText = ellipsizeForPaint(exifLine, exifPaint, maxTextWidth)

    var logoBitmap: Bitmap? = null
    var logoHeight = 0f
    if (showLogo && logo != null && logo.width > 0 && logo.height > 0) {
        val desiredHeight = ((targetRect.height() * 0.13f) * logoScaleFactor.coerceIn(0.72f, 1.85f)).coerceAtLeast(10f)
        val scale = desiredHeight / logo.height.toFloat()
        val desiredWidth = (logo.width * scale).roundToInt().coerceAtLeast(1)
        val constrainedWidth = desiredWidth.coerceAtMost((targetRect.width() * 0.50f).roundToInt().coerceAtLeast(1))
        val finalHeight = ((constrainedWidth.toFloat() / logo.width.toFloat()) * logo.height).roundToInt().coerceAtLeast(1)
        logoBitmap = Bitmap.createScaledBitmap(logo, constrainedWidth, finalHeight, true)
        logoHeight = logoBitmap.height.toFloat()
    }

    val modelHeight = if (showModelFinal) modelPaint.descent() - modelPaint.ascent() else 0f
    val exifHeight = if (showExifFinal) exifPaint.descent() - exifPaint.ascent() else 0f
    val contentHeight = logoHeight + modelHeight + exifHeight + gap * listOf(showLogo && logoBitmap != null, showModelFinal, showExifFinal).count { it }.coerceAtLeast(1) - gap
    val safeBottom = targetRect.bottom - targetRect.height() * bottomSafeRatio
    var currentTop = (safeBottom - contentHeight).coerceAtLeast(targetRect.top + targetRect.height() * 0.06f)

    if (showLogo && logoBitmap != null) {
        val left = centerX - logoBitmap.width / 2f
        canvas.drawBitmap(logoBitmap, left, currentTop, null)
        currentTop += logoBitmap.height + gap
        logoBitmap.recycle()
    }
    if (showModelFinal) {
        val baseline = currentTop - modelPaint.ascent()
        canvas.drawText(modelText, centerX, baseline, modelPaint)
        currentTop += modelHeight + gap
    }
    if (showExifFinal) {
        val baseline = currentTop - exifPaint.ascent()
        canvas.drawText(exifText, centerX, baseline, exifPaint)
    }
}

private fun drawCenteredFooterOverlay(
    canvas: Canvas,
    targetRect: RectF,
    logo: Bitmap?,
    modelLine: String,
    exifLine: String,
    logoScaleFactor: Float,
    textColor: Int,
    watermarkTypeface: Typeface?
) {
    if (targetRect.width() <= 0f || targetRect.height() <= 0f) return
    val centerX = targetRect.centerX()
    val maxTextWidth = targetRect.width() * 0.74f
    val gap = targetRect.height() * 0.04f

    var logoBitmap: Bitmap? = null
    var logoH = 0f
    if (logo != null && logo.width > 0 && logo.height > 0) {
        val desiredHeight = ((targetRect.height() * 0.22f) * logoScaleFactor.coerceIn(0.72f, 1.85f)).coerceAtLeast(12f)
        val scale = desiredHeight / logo.height.toFloat()
        val w = (logo.width * scale).roundToInt().coerceAtLeast(1)
        val h = desiredHeight.roundToInt().coerceAtLeast(1)
        logoBitmap = Bitmap.createScaledBitmap(logo, w, h, true)
        logoH = logoBitmap.height.toFloat()
    }

    val modelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(255, 95, 95, 95)
        textAlign = Paint.Align.CENTER
        textSize = (targetRect.height() * 0.15f).coerceAtLeast(9f)
        typeface = resolveWatermarkTextTypeface(watermarkTypeface, modelLine)
    }
    val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(255, 95, 95, 95)
        textAlign = Paint.Align.CENTER
        textSize = (targetRect.height() * 0.15f).coerceAtLeast(9f)
        typeface = resolveWatermarkTextTypeface(watermarkTypeface, exifLine)
    }
    fitTextSizeToWidth(modelPaint, modelLine, maxTextWidth, 8f, (targetRect.height() * 0.16f).coerceAtLeast(9f))
    fitTextSizeToWidth(exifPaint, exifLine, maxTextWidth, 8f, (targetRect.height() * 0.16f).coerceAtLeast(9f))
    val mText = ellipsizeForPaint(modelLine, modelPaint, maxTextWidth)
    val eText = ellipsizeForPaint(exifLine, exifPaint, maxTextWidth)

    val modelH = if (modelLine.isNotBlank()) modelPaint.descent() - modelPaint.ascent() else 0f
    val exifH = if (exifLine.isNotBlank()) exifPaint.descent() - exifPaint.ascent() else 0f
    val parts = listOf(logoH > 0f, modelH > 0f, exifH > 0f).count { it }
    if (parts == 0) return
    val totalH = logoH + modelH + exifH + gap * (parts - 1)
    var top = targetRect.top + (targetRect.height() - totalH) / 2f

    if (logoBitmap != null) {
        canvas.drawBitmap(logoBitmap, centerX - logoBitmap.width / 2f, top, null)
        top += logoBitmap.height + gap
        logoBitmap.recycle()
    }
    if (modelH > 0f) {
        canvas.drawText(mText, centerX, top - modelPaint.ascent(), modelPaint)
        top += modelH + gap
    }
    if (exifH > 0f) {
        canvas.drawText(eText, centerX, top - exifPaint.ascent(), exifPaint)
    }
}

internal fun drawSignatureBottomRight(
    canvas: Canvas,
    targetRect: RectF,
    signatureText: String,
    textColor: Int,
    signatureTypeface: Typeface?,
    horizontalOffsetRatio: Float = 0f,
    verticalOffsetRatio: Float = 0f,
    sizeScaleFactor: Float = 1f
) {
    if (targetRect.width() <= 0f || targetRect.height() <= 0f || signatureText.isBlank()) return
    val safeText = sanitizeEnglishSignature(signatureText)
    if (safeText.isBlank()) return
    val safeScale = sizeScaleFactor.coerceIn(0.65f, 1.8f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.RIGHT
        textSize = (targetRect.height() * 0.38f * safeScale).coerceIn(9f, 58f)
        typeface = signatureTypeface ?: android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
    }
    val maxWidth = (targetRect.width() * 0.86f).coerceAtLeast(28f)
    fitTextSizeToWidth(
        paint = paint,
        text = safeText,
        maxWidth = maxWidth,
        minTextSize = (targetRect.height() * 0.22f * safeScale).coerceAtLeast(8f),
        maxTextSize = (targetRect.height() * 0.40f * safeScale).coerceAtLeast(12f)
    )
    val text = ellipsizeForPaint(safeText, paint, maxWidth)
    val textWidth = paint.measureText(text)
    val marginX = targetRect.width() * 0.02f
    val baseX = targetRect.right - marginX
    val xShift = targetRect.width() * 0.24f * horizontalOffsetRatio.coerceIn(-1f, 1f)
    val x = safeCoerceIn(
        value = baseX + xShift,
        min = targetRect.left + textWidth + marginX * 0.4f,
        max = targetRect.right - marginX * 0.35f
    )
    val baseY = targetRect.bottom - targetRect.height() * 0.08f - paint.descent()
    val yShift = targetRect.height() * 0.30f * verticalOffsetRatio.coerceIn(-1f, 1f)
    val minBaseline = targetRect.top - paint.ascent() + targetRect.height() * 0.04f
    val maxBaseline = targetRect.bottom - paint.descent() - targetRect.height() * 0.04f
    val y = safeCoerceIn(baseY + yShift, minBaseline, maxBaseline)
    canvas.drawText(text, x, y, paint)
}

private fun drawSignatureCentered(
    canvas: Canvas,
    targetRect: RectF,
    signatureText: String,
    textColor: Int,
    signatureTypeface: Typeface?,
    horizontalOffsetRatio: Float = 0f,
    verticalOffsetRatio: Float = 0f,
    sizeScaleFactor: Float = 1f
) {
    if (targetRect.width() <= 0f || targetRect.height() <= 0f || signatureText.isBlank()) return
    val safeText = sanitizeEnglishSignature(signatureText)
    if (safeText.isBlank()) return
    val safeScale = sizeScaleFactor.coerceIn(0.65f, 1.8f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        textSize = (targetRect.height() * 0.34f * safeScale).coerceIn(10f, 60f)
        typeface = signatureTypeface ?: android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
    }
    val maxWidth = targetRect.width() * 0.90f
    fitTextSizeToWidth(
        paint = paint,
        text = safeText,
        maxWidth = maxWidth,
        minTextSize = (targetRect.height() * 0.20f * safeScale).coerceAtLeast(9f),
        maxTextSize = (targetRect.height() * 0.38f * safeScale).coerceAtLeast(13f)
    )
    val text = ellipsizeForPaint(safeText, paint, maxWidth)
    val textWidth = paint.measureText(text)
    val centerMargin = targetRect.width() * 0.03f
    val baseX = targetRect.centerX()
    val xShift = targetRect.width() * 0.28f * horizontalOffsetRatio.coerceIn(-1f, 1f)
    val x = safeCoerceIn(
        value = baseX + xShift,
        min = targetRect.left + textWidth / 2f + centerMargin,
        max = targetRect.right - textWidth / 2f - centerMargin
    )
    val baseY = targetRect.centerY() - (paint.ascent() + paint.descent()) / 2f
    val yShift = targetRect.height() * 0.32f * verticalOffsetRatio.coerceIn(-1f, 1f)
    val minBaseline = targetRect.top - paint.ascent() + targetRect.height() * 0.05f
    val maxBaseline = targetRect.bottom - paint.descent() - targetRect.height() * 0.05f
    val y = safeCoerceIn(baseY + yShift, minBaseline, maxBaseline)
    canvas.drawText(text, x, y, paint)
}

private fun safeCoerceIn(value: Float, min: Float, max: Float): Float {
    if (min <= max) return value.coerceIn(min, max)
    val fallback = (min + max) * 0.5f
    return if (fallback.isFinite()) fallback else value
}

private fun fitTextSizeToWidth(
    paint: Paint,
    text: String,
    maxWidth: Float,
    minTextSize: Float,
    maxTextSize: Float
) {
    if (text.isBlank() || maxWidth <= 0f) return
    var low = minTextSize
    var high = maxTextSize
    var best = low
    repeat(12) {
        val mid = (low + high) / 2f
        paint.textSize = mid
        if (paint.measureText(text) <= maxWidth) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }
    paint.textSize = best.coerceIn(minTextSize, maxTextSize)
}

private fun ellipsizeForPaint(text: String, paint: Paint, maxWidth: Float): String {
    if (text.isBlank() || maxWidth <= 0f) return text
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "..."
    var low = 0
    var high = text.length
    var best = ""
    while (low <= high) {
        val mid = (low + high) ushr 1
        val candidate = text.take(mid) + ellipsis
        if (paint.measureText(candidate) <= maxWidth) {
            best = candidate
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return if (best.isNotBlank()) best else ellipsis
}

private fun computeCenterCropSrcRect(source: Bitmap, dstRect: RectF): android.graphics.Rect {
    return computeCenterCropSrcRect(
        source = source,
        dstRect = dstRect,
        horizontalOffsetRatio = 0f,
        verticalOffsetRatio = 0f
    )
}

private fun computeCenterCropSrcRect(
    source: Bitmap,
    dstRect: RectF,
    horizontalOffsetRatio: Float,
    verticalOffsetRatio: Float
): android.graphics.Rect {
    val dstW = dstRect.width().coerceAtLeast(1f)
    val dstH = dstRect.height().coerceAtLeast(1f)
    val dstAspect = dstW / dstH
    val srcAspect = source.width.toFloat() / source.height.toFloat().coerceAtLeast(1f)
    return if (srcAspect > dstAspect) {
        val cropWidth = (source.height * dstAspect).roundToInt().coerceAtLeast(1).coerceAtMost(source.width)
        val extraWidth = (source.width - cropWidth).coerceAtLeast(0)
        val horizontalBias = ((horizontalOffsetRatio.coerceIn(-1f, 1f) + 1f) * 0.5f)
        val left = (extraWidth * horizontalBias).roundToInt().coerceIn(0, extraWidth)
        android.graphics.Rect(left, 0, (left + cropWidth).coerceAtMost(source.width), source.height)
    } else {
        val cropHeight = (source.width / dstAspect).roundToInt().coerceAtLeast(1).coerceAtMost(source.height)
        val extraHeight = (source.height - cropHeight).coerceAtLeast(0)
        val verticalBias = ((verticalOffsetRatio.coerceIn(-1f, 1f) + 1f) * 0.5f)
        val top = (extraHeight * verticalBias).roundToInt().coerceIn(0, extraHeight)
        android.graphics.Rect(0, top, source.width, (top + cropHeight).coerceAtMost(source.height))
    }
}

internal fun drawBitmapCenterCrop(canvas: Canvas, source: Bitmap, dstRect: RectF, paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)) {
    drawBitmapCenterCrop(
        canvas = canvas,
        source = source,
        dstRect = dstRect,
        paint = paint,
        horizontalOffsetRatio = 0f,
        verticalOffsetRatio = 0f
    )
}

internal fun drawBitmapCenterCrop(
    canvas: Canvas,
    source: Bitmap,
    dstRect: RectF,
    paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG),
    horizontalOffsetRatio: Float = 0f,
    verticalOffsetRatio: Float = 0f
) {
    if (dstRect.width() <= 1f || dstRect.height() <= 1f) return
    val srcRect = computeCenterCropSrcRect(
        source = source,
        dstRect = dstRect,
        horizontalOffsetRatio = horizontalOffsetRatio,
        verticalOffsetRatio = verticalOffsetRatio
    )
    canvas.drawBitmap(source, srcRect, dstRect, paint)
}

private fun drawRoundedCenterCropBitmap(canvas: Canvas, source: Bitmap, rect: RectF, radius: Float) {
    if (rect.width() <= 1f || rect.height() <= 1f) return
    val path = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(path)
    drawBitmapCenterCrop(canvas, source, rect)
    canvas.restore()
}

private fun drawRoundedBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    rect: RectF,
    radius: Float
) {
    val path = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(path)
    canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG))
    canvas.restore()
}

private fun createSoftBlurBitmap(
    source: Bitmap,
    outputWidth: Int,
    outputHeight: Int
): Bitmap {
    val firstWidth = (source.width * 0.10f).roundToInt().coerceAtLeast(24)
    val firstHeight = (source.height * 0.10f).roundToInt().coerceAtLeast(24)
    val small = Bitmap.createScaledBitmap(source, firstWidth, firstHeight, true)
    val stretched = Bitmap.createScaledBitmap(small, outputWidth, outputHeight, true)
    small.recycle()

    val secondWidth = (outputWidth * 0.45f).roundToInt().coerceAtLeast(24)
    val secondHeight = (outputHeight * 0.45f).roundToInt().coerceAtLeast(24)
    val second = Bitmap.createScaledBitmap(stretched, secondWidth, secondHeight, true)
    stretched.recycle()

    val result = Bitmap.createScaledBitmap(second, outputWidth, outputHeight, true)
    second.recycle()
    return result
}

private fun createGaussianBlurBitmap(
    source: Bitmap,
    outputWidth: Int,
    outputHeight: Int,
    downsample: Int = 6,
    radius: Int = 8,
    passes: Int = 3
): Bitmap {
    val smallWidth = (outputWidth / downsample).coerceAtLeast(64)
    val smallHeight = (outputHeight / downsample).coerceAtLeast(64)
    val scaled = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
    val blurredSmall = fastGaussianLikeBlur(scaled, radius = radius, passes = passes)
    scaled.recycle()
    val result = Bitmap.createScaledBitmap(blurredSmall, outputWidth, outputHeight, true)
    blurredSmall.recycle()
    return result
}

private fun createWatermarkEditorBackdropBitmap(
    source: Bitmap,
    outputWidth: Int,
    outputHeight: Int
): Bitmap {
    val fitted = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val fittedCanvas = Canvas(fitted)
    fittedCanvas.drawColor(AndroidColor.argb(255, 104, 108, 114))
    val sourceWidth = source.width.toFloat().coerceAtLeast(1f)
    val sourceHeight = source.height.toFloat().coerceAtLeast(1f)
    val scale = minOf(outputWidth / sourceWidth, outputHeight / sourceHeight)
    val drawWidth = sourceWidth * scale
    val drawHeight = sourceHeight * scale
    val left = (outputWidth - drawWidth) / 2f
    val top = (outputHeight - drawHeight) / 2f
    fittedCanvas.drawBitmap(
        source,
        null,
        RectF(left, top, left + drawWidth, top + drawHeight),
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    )
    val blurred = createGaussianBlurBitmap(
        source = fitted,
        outputWidth = outputWidth,
        outputHeight = outputHeight,
        downsample = 12,
        radius = 14,
        passes = 4
    )
    fitted.recycle()
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val grayscalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply {
                setSaturation(0.12f)
            }
        )
    }
    canvas.drawBitmap(blurred, 0f, 0f, grayscalePaint)
    blurred.recycle()
    canvas.drawColor(AndroidColor.argb(108, 82, 86, 92))
    return output
}

private fun resolveWatermarkPreviewMaxHeightRatio(aspectRatio: Float): Float {
    val safeAspectRatio = aspectRatio.coerceAtLeast(0.01f)
    return when {
        safeAspectRatio < 0.58f -> 0.64f
        safeAspectRatio < 0.8f -> 0.72f
        safeAspectRatio < 1f -> 0.82f
        else -> 1f
    }
}

private fun fastGaussianLikeBlur(
    bitmap: Bitmap,
    radius: Int,
    passes: Int
): Bitmap {
    if (radius <= 0 || passes <= 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val width = bitmap.width
    val height = bitmap.height
    var src = IntArray(width * height)
    bitmap.getPixels(src, 0, width, 0, 0, width, height)
    var dst = IntArray(width * height)

    repeat(passes) {
        boxBlurHorizontal(src, dst, width, height, radius)
        boxBlurVertical(dst, src, width, height, radius)
    }

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(src, 0, width, 0, 0, width, height)
    }
}

private fun boxBlurHorizontal(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val windowSize = radius * 2 + 1
    for (y in 0 until height) {
        var aSum = 0
        var rSum = 0
        var gSum = 0
        var bSum = 0
        val rowStart = y * width
        for (i in -radius..radius) {
            val x = i.coerceIn(0, width - 1)
            val c = src[rowStart + x]
            aSum += (c ushr 24) and 0xFF
            rSum += (c ushr 16) and 0xFF
            gSum += (c ushr 8) and 0xFF
            bSum += c and 0xFF
        }
        for (x in 0 until width) {
            val out = ((aSum / windowSize) shl 24) or
                ((rSum / windowSize) shl 16) or
                ((gSum / windowSize) shl 8) or
                (bSum / windowSize)
            dst[rowStart + x] = out

            val removeX = (x - radius).coerceIn(0, width - 1)
            val addX = (x + radius + 1).coerceIn(0, width - 1)
            val removeColor = src[rowStart + removeX]
            val addColor = src[rowStart + addX]
            aSum += ((addColor ushr 24) and 0xFF) - ((removeColor ushr 24) and 0xFF)
            rSum += ((addColor ushr 16) and 0xFF) - ((removeColor ushr 16) and 0xFF)
            gSum += ((addColor ushr 8) and 0xFF) - ((removeColor ushr 8) and 0xFF)
            bSum += (addColor and 0xFF) - (removeColor and 0xFF)
        }
    }
}

private fun boxBlurVertical(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val windowSize = radius * 2 + 1
    for (x in 0 until width) {
        var aSum = 0
        var rSum = 0
        var gSum = 0
        var bSum = 0
        for (i in -radius..radius) {
            val y = i.coerceIn(0, height - 1)
            val c = src[y * width + x]
            aSum += (c ushr 24) and 0xFF
            rSum += (c ushr 16) and 0xFF
            gSum += (c ushr 8) and 0xFF
            bSum += c and 0xFF
        }
        for (y in 0 until height) {
            val out = ((aSum / windowSize) shl 24) or
                ((rSum / windowSize) shl 16) or
                ((gSum / windowSize) shl 8) or
                (bSum / windowSize)
            dst[y * width + x] = out

            val removeY = (y - radius).coerceIn(0, height - 1)
            val addY = (y + radius + 1).coerceIn(0, height - 1)
            val removeColor = src[removeY * width + x]
            val addColor = src[addY * width + x]
            aSum += ((addColor ushr 24) and 0xFF) - ((removeColor ushr 24) and 0xFF)
            rSum += ((addColor ushr 16) and 0xFF) - ((removeColor ushr 16) and 0xFF)
            gSum += ((addColor ushr 8) and 0xFF) - ((removeColor ushr 8) and 0xFF)
            bSum += (addColor and 0xFF) - (removeColor and 0xFF)
        }
    }
}

private fun computeBaseCropWindow(sourceWidth: Int, sourceHeight: Int, targetAspect: Float): Pair<Float, Float> {
    val safeAspect = targetAspect.coerceAtLeast(0.01f)
    val sourceAspect = sourceWidth.toFloat() / sourceHeight.toFloat().coerceAtLeast(1f)
    return if (sourceAspect >= safeAspect) {
        val h = sourceHeight.toFloat()
        val w = h * safeAspect
        w to h
    } else {
        val w = sourceWidth.toFloat()
        val h = w / safeAspect
        w to h
    }
}

private fun clampCropCenter(
    sourceWidth: Float,
    sourceHeight: Float,
    cropWidth: Float,
    cropHeight: Float,
    centerX: Float,
    centerY: Float
): Pair<Float, Float> {
    val halfW = cropWidth / 2f
    val halfH = cropHeight / 2f
    val clampedX = centerX.coerceIn(halfW, sourceWidth - halfW)
    val clampedY = centerY.coerceIn(halfH, sourceHeight - halfH)
    return clampedX to clampedY
}

private fun cropBitmapForAspect(
    source: Bitmap,
    targetAspect: Float,
    centerX: Float,
    centerY: Float,
    zoom: Float
): Bitmap {
    val safeZoom = zoom.coerceIn(1f, 8f)
    val (baseW, baseH) = computeBaseCropWindow(source.width, source.height, targetAspect.coerceAtLeast(0.01f))
    val cropW = (baseW / safeZoom).coerceAtLeast(1f)
    val cropH = (baseH / safeZoom).coerceAtLeast(1f)
    val clamped = clampCropCenter(source.width.toFloat(), source.height.toFloat(), cropW, cropH, centerX, centerY)
    val left = (clamped.first - cropW / 2f).roundToInt().coerceIn(0, (source.width - 1).coerceAtLeast(0))
    val top = (clamped.second - cropH / 2f).roundToInt().coerceIn(0, (source.height - 1).coerceAtLeast(0))
    val width = cropW.roundToInt().coerceAtLeast(1).coerceAtMost(source.width - left)
    val height = cropH.roundToInt().coerceAtLeast(1).coerceAtMost(source.height - top)
    return Bitmap.createBitmap(source, left, top, width, height)
}

private fun chooseOutputFormat(sourceMimeType: String?): WatermarkOutputFormat {
    val normalized = sourceMimeType?.lowercase(Locale.ROOT).orEmpty()
    return when (normalized) {
        "image/png" -> WatermarkOutputFormat(
            mimeType = "image/png",
            fileExtension = "png",
            compressFormat = Bitmap.CompressFormat.PNG,
            quality = 100
        )
        else -> WatermarkOutputFormat(
            mimeType = "image/jpeg",
            fileExtension = "jpg",
            compressFormat = Bitmap.CompressFormat.JPEG,
            quality = 100
        )
    }
}

private fun decodeSourceBitmap(
    context: Context,
    uri: Uri,
    requestedMaxSidePx: Int? = null
): Bitmap {
    val resolver = context.contentResolver
    val defaultMaxDecodeSide = 7000
    val pressureCap = if (AppPerformanceGovernor.isMemoryPressureHigh()) 3600 else defaultMaxDecodeSide
    val requestedCap = requestedMaxSidePx?.coerceIn(1600, defaultMaxDecodeSide) ?: defaultMaxDecodeSide
    val maxDecodeSide = minOf(pressureCap, requestedCap).coerceAtLeast(1600)
    runCatching {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, boundsOptions)
        } ?: throw IllegalStateException("无法打开原图")

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            throw IllegalStateException("图片尺寸无效")
        }

        val maxSide = max(srcWidth, srcHeight)
        var sample = 1
        while ((maxSide / sample) > maxDecodeSide) {
            sample *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, decodeOptions)
        } ?: throw IllegalStateException("BitmapFactory decode failed")

        val orientation = runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                ExifInterface(pfd.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        return applyExifOrientation(decoded, orientation)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return runCatching {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val srcWidth = info.size.width.coerceAtLeast(1)
                val srcHeight = info.size.height.coerceAtLeast(1)
                val maxSide = max(srcWidth, srcHeight).coerceAtLeast(1)
                if (maxSide > maxDecodeSide) {
                    val scale = maxDecodeSide.toFloat() / maxSide.toFloat()
                    decoder.setTargetSize(
                        (srcWidth * scale).roundToInt().coerceAtLeast(1),
                        (srcHeight * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        }.getOrElse {
            throw IllegalStateException("无法解码图片")
        }
    }
    throw IllegalStateException("无法解码图片")
}

private fun computeRenderDecodeMaxSide(outputSizeOverride: Pair<Int, Int>?): Int? {
    if (outputSizeOverride == null) return null
    val outMax = max(outputSizeOverride.first, outputSizeOverride.second).coerceAtLeast(1)
    // Decode a little larger than target to keep crop/detail quality while avoiding oversized source bitmaps.
    return (outMax * 2).coerceIn(1600, 6000)
}

private fun resolveWatermarkPreviewOutputSizeOverride(
    outputSizeOverride: Pair<Int, Int>?,
    previewMaxSidePx: Int = WATERMARK_LIVE_PREVIEW_MAX_SIDE
): Pair<Int, Int>? {
    if (outputSizeOverride == null) return null
    return scaleWatermarkOutputSizeToMaxSide(outputSizeOverride, previewMaxSidePx)
}

private fun resolveWatermarkLivePreviewMaxSide(template: WatermarkTemplatePreset): Int {
    return when (template) {
        WatermarkTemplatePreset.PaperDots -> WATERMARK_PAPER_DOTS_LIVE_PREVIEW_MAX_SIDE
        else -> WATERMARK_LIVE_PREVIEW_MAX_SIDE
    }
}

private fun scaleWatermarkOutputSizeToMaxSide(
    size: Pair<Int, Int>,
    maxSidePx: Int
): Pair<Int, Int> {
    val width = size.first.coerceAtLeast(1)
    val height = size.second.coerceAtLeast(1)
    val sourceMax = max(width, height).coerceAtLeast(1)
    if (sourceMax <= maxSidePx) return width to height
    val scale = maxSidePx.toFloat() / sourceMax.toFloat()
    return (
        (width * scale).roundToInt().coerceAtLeast(1) to
            (height * scale).roundToInt().coerceAtLeast(1)
        )
}

private fun recycleBitmapQuietly(bitmap: Bitmap?) {
    if (bitmap == null || bitmap.isRecycled) return
    runCatching { bitmap.recycle() }
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}

private suspend fun readExifInfo(context: Context, uri: Uri): WatermarkExifInfo = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val exif = runCatching {
        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            ExifInterface(pfd.fileDescriptor)
        }
    }.getOrNull() ?: runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream)
        }
    }.getOrNull() ?: return@withContext WatermarkExifInfo()

    val isoValue = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED)
        ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        ?: "--"
    val makeValue = exif.getAttribute(ExifInterface.TAG_MAKE).orEmpty().trim()
    val modelValue = exif.getAttribute(ExifInterface.TAG_MODEL).orEmpty().trim()
    val preferredFocalLength = resolvePreferredExifFocalLength(exif)
    val latLong = FloatArray(2)
    val hasLatLong = runCatching { exif.getLatLong(latLong) }.getOrDefault(false)
    val latitude = if (hasLatLong) latLong[0].toDouble() else null
    val longitude = if (hasLatLong) latLong[1].toDouble() else null
    val cityLocation = resolveWatermarkLocationLabel(context, latitude, longitude)

    WatermarkExifInfo(
        make = makeValue,
        model = modelValue,
        iso = isoValue,
        shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "--",
        focalLength = preferredFocalLength,
        aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "--",
        dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: "",
        latitude = latitude,
        longitude = longitude,
        cityLocation = cityLocation
    )
}

private fun resolvePreferredExifFocalLength(exif: ExifInterface): String {
    val focalLength35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)
        ?.trim()
        .orEmpty()
    if (focalLength35mm.isNotBlank() && focalLength35mm != "0") {
        return focalLength35mm
    }
    return exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        ?.trim()
        .takeUnless { it.isNullOrBlank() || it == "0" }
        ?: "--"
}

private fun decodeBrandLogo(
    context: Context,
    resId: Int,
    referenceHeight: Int,
    template: WatermarkTemplatePreset,
    brandKey: String?
): Bitmap? {
    return runCatching {
        val raw = BitmapFactory.decodeResource(context.resources, resId) ?: return@runCatching null
        val baseRatio = 0.065f
        val shouldBoost = template in setOf(
            WatermarkTemplatePreset.BlurBackground,
            WatermarkTemplatePreset.DirectWatermark
        ) && brandKey != null && !LOGO_SIZE_EXCLUDED_BRANDS.contains(brandKey)
        val aspect = raw.width.toFloat() / raw.height.toFloat().coerceAtLeast(1f)
        val boost = if (shouldBoost) {
            when {
                aspect >= 3.2f -> 1.16f
                aspect >= 2.2f -> 1.22f
                else -> 1.30f
            }
        } else {
            1f
        }
        val frameCenteredAdjust = if (template == WatermarkTemplatePreset.WatermarkFrame && brandKey != null) {
            when {
                FRAME_CENTERED_LOGO_ENLARGE_BRANDS.contains(brandKey) -> 1.18f
                FRAME_CENTERED_LOGO_SHRINK_BRANDS.contains(brandKey) -> 0.84f
                else -> 1f
            }
        } else {
            1f
        }
        val targetHeight = (referenceHeight * baseRatio * boost * frameCenteredAdjust).roundToInt().coerceIn(36, 280)
        val scale = targetHeight / raw.height.toFloat()
        val targetWidth = (raw.width * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(raw, targetWidth, targetHeight, true)
        raw.recycle()
        scaled
    }.getOrNull()
}

private fun decodeCustomLogo(
    filePath: String?,
    referenceHeight: Int,
    template: WatermarkTemplatePreset
): Bitmap? {
    val safePath = filePath?.trim().orEmpty()
    if (safePath.isBlank()) return null
    val file = File(safePath)
    if (!file.exists() || !file.isFile) return null
    return runCatching {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
        if (raw.width <= 0 || raw.height <= 0) {
            raw.recycle()
            return@runCatching null
        }
        val baseRatio = if (template == WatermarkTemplatePreset.WatermarkFrame) 0.07f else 0.065f
        val targetHeight = (referenceHeight * baseRatio).roundToInt().coerceIn(36, 320)
        val scale = targetHeight / raw.height.toFloat()
        val targetWidth = (raw.width * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(raw, targetWidth, targetHeight, true)
        raw.recycle()
        scaled
    }.getOrNull()
}

private fun persistCustomWatermarkLogo(context: Context, sourceUri: Uri): String {
    val targetDir = File(context.filesDir, WATERMARK_CUSTOM_LOGO_SUBDIR)
    if (!targetDir.exists() && !targetDir.mkdirs()) {
        error("无法创建 Logo 存储目录")
    }
    val targetFile = File(targetDir, WATERMARK_CUSTOM_LOGO_FILE)
    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
        }
    } ?: error("读取自定义 Logo 失败")
    return targetFile.absolutePath
}

private fun detectBrandKey(make: String, model: String): String? {
    val source = "$make $model".lowercase(Locale.ROOT)
    return BRAND_LOGO_GROUPS.firstOrNull { group ->
        group.aliases.any { alias -> source.contains(alias) }
    }?.key
}

private fun resolveBrandLogoResId(brandKey: String?, variantIndex: Int): Int? {
    val group = brandKey?.let { BRAND_LOGO_GROUP_MAP[it] } ?: return null
    val safeIndex = variantIndex.coerceIn(0, group.variants.lastIndex)
    return group.variants[safeIndex].resId
}

private fun preferredLogoVariantIndex(group: BrandLogoGroup?, avoidWhite: Boolean): Int {
    if (group == null || group.variants.isEmpty()) return 0
    val colorIndex = group.variants.indexOfFirst { it.label.contains("Color", ignoreCase = true) }
    if (colorIndex >= 0) return colorIndex
    if (!avoidWhite) {
        val whiteIndex = group.variants.indexOfFirst { it.label.contains("White", ignoreCase = true) }
        if (whiteIndex >= 0) return whiteIndex
    } else {
        val nonWhiteIndex = group.variants.indexOfFirst { !it.label.contains("White", ignoreCase = true) }
        if (nonWhiteIndex >= 0) return nonWhiteIndex
    }
    val nonBlackNonWhite = group.variants.indexOfFirst {
        !it.label.contains("Black", ignoreCase = true) && !it.label.contains("White", ignoreCase = true)
    }
    if (nonBlackNonWhite >= 0) return nonBlackNonWhite
    val nonWhiteIndex = group.variants.indexOfFirst { !it.label.contains("White", ignoreCase = true) }
    if (nonWhiteIndex >= 0) return nonWhiteIndex
    return 0
}

private fun resolveDisplayModelLine(context: Context, exif: WatermarkExifInfo): String {
    val raw = listOf(exif.make.trim(), exif.model.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { exif.cameraLine }
    val resolved = DeviceModelNameResolver.resolveCameraLabel(context, raw)
    val localeLanguage = Locale.getDefault().language.lowercase(Locale.US)
    val language = when (localeLanguage) {
        "zh" -> AppLanguage.SimplifiedChinese
        "ja" -> AppLanguage.Japanese
        "ko" -> AppLanguage.Korean
        "fr" -> AppLanguage.French
        "de" -> AppLanguage.German
        "it" -> AppLanguage.Italian
        "pt" -> AppLanguage.Portuguese
        "es" -> AppLanguage.Spanish
        "ru" -> AppLanguage.Russian
        "th" -> AppLanguage.Thai
        "vi" -> AppLanguage.Vietnamese
        "id" -> AppLanguage.Indonesian
        "ar" -> AppLanguage.Arabic
        "no", "nn" -> AppLanguage.Norwegian
        "iw" -> AppLanguage.Hebrew
        else -> AppLanguage.entries.firstOrNull { it.code == localeLanguage } ?: AppLanguage.English
    }
    return resolved.takeIf { it.isNotBlank() && it != "--" } ?: getWatermarkExtraCopy(language).unknownModel
}

private fun normalizeWatermarkFontKey(raw: String?): String {
    val key = raw?.trim().orEmpty()
    return if (WATERMARK_TEXT_FONT_OPTION_MAP.containsKey(key)) key else DEFAULT_WATERMARK_TEXT_FONT_KEY
}

private fun normalizeSignatureFontKey(raw: String?): String {
    val key = raw?.trim().orEmpty()
    return if (SIGNATURE_FONT_OPTION_MAP.containsKey(key)) key else DEFAULT_SIGNATURE_FONT_KEY
}

private fun loadTypefaceFromAssetPath(context: Context, assetPath: String): Typeface? {
    cachedTypefaceByAssetPath[assetPath]?.let { return it }
    val loaded = runCatching { Typeface.createFromAsset(context.assets, assetPath) }.getOrNull() ?: return null
    cachedTypefaceByAssetPath.putIfAbsent(assetPath, loaded)
    return cachedTypefaceByAssetPath[assetPath] ?: loaded
}

private fun loadWatermarkTypeface(context: Context, fontKey: String): Typeface? {
    val normalized = normalizeWatermarkFontKey(fontKey)
    val selected = WATERMARK_TEXT_FONT_OPTION_MAP[normalized] ?: WATERMARK_TEXT_FONT_OPTIONS.first()
    return loadTypefaceFromAssetPath(context, selected.assetPath)
        ?: loadTypefaceFromAssetPath(context, WATERMARK_TEXT_FONT_OPTIONS.first().assetPath)
}

private fun loadSignatureTypeface(context: Context, fontKey: String): Typeface? {
    val normalized = normalizeSignatureFontKey(fontKey)
    val selected = SIGNATURE_FONT_OPTION_MAP[normalized] ?: SIGNATURE_FONT_OPTIONS.first()
    return loadTypefaceFromAssetPath(context, selected.assetPath)
        ?: loadTypefaceFromAssetPath(context, SIGNATURE_FONT_OPTIONS.first().assetPath)
}

internal fun resolveWatermarkTextTypeface(
    preferredTypeface: Typeface?,
    text: String,
    bold: Boolean = false
): Typeface {
    val fallbackStyle = if (bold) Typeface.BOLD else Typeface.NORMAL
    val fallbackTypeface = Typeface.create(Typeface.SANS_SERIF, fallbackStyle)
    val normalizedText = text.trim()
    if (normalizedText.isBlank()) {
        return preferredTypeface ?: fallbackTypeface
    }
    val needsUnicodeFallback = normalizedText.any { char ->
        !char.isWhitespace() && char.code > 0x024F
    }
    return if (needsUnicodeFallback) {
        fallbackTypeface
    } else {
        preferredTypeface ?: fallbackTypeface
    }
}

private fun sanitizeEnglishSignature(input: String): String {
    if (input.isBlank()) return ""
    return input.filter { char ->
        char.code in 32..126
    }.trimStart()
}

private fun localizeVariantLabel(label: String): String {
    return label
        .replace("Black", "黑色")
        .replace("White", "白色")
        .replace("Color", "彩色")
}

private fun encodeSignatureHistory(items: List<String>): String {
    if (items.isEmpty()) return ""
    return items.joinToString("|") { text ->
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }
}

private fun decodeSignatureHistory(encoded: String?): List<String> {
    if (encoded.isNullOrBlank()) return emptyList()
    return encoded.split('|')
        .mapNotNull { token ->
            runCatching {
                String(Base64.decode(token, Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrNull()
        }
        .map { sanitizeEnglishSignature(it) }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun encodeWatermarkUserPresets(items: List<WatermarkUserPreset>): String {
    if (items.isEmpty()) return "[]"
    val array = JSONArray()
    items.forEach { preset ->
        val obj = JSONObject()
        obj.put("id", preset.id)
        obj.put("name", preset.name)
        obj.put("templateName", preset.templateName)
        obj.put("selectedBrandKey", preset.selectedBrandKey ?: "")
        obj.put("selectedVariantIndex", preset.selectedVariantIndex)
        obj.put("showLogoWatermark", preset.showLogoWatermark)
        obj.put("showModelWatermark", preset.showModelWatermark)
        obj.put("showDateWatermark", preset.showDateWatermark)
        obj.put("showExifWatermark", preset.showExifWatermark)
        obj.put("showLocationWatermark", preset.showLocationWatermark)
        obj.put("showCityLocationWatermark", preset.showCityLocationWatermark)
        obj.put("customModelLabel", preset.customModelLabel)
        obj.put("customDateLabel", preset.customDateLabel)
        obj.put("customLocationLabel", preset.customLocationLabel)
        obj.put("fullColorTextScaleFactor", preset.fullColorTextScaleFactor.toDouble())
        obj.put("fullColorBackgroundColorIndex", preset.fullColorBackgroundColorIndex)
        obj.put("fullColorCustomBackgroundColor", preset.fullColorCustomBackgroundColor)
        obj.put("fullColorUseGradientBackground", preset.fullColorUseGradientBackground)
        obj.put("fullColorGradientPresetIndex", preset.fullColorGradientPresetIndex)
        obj.put("fullColorImageOnTop", preset.fullColorImageOnTop)
        obj.put("frameSideTopBorderRatio", preset.frameSideTopBorderRatio.toDouble())
        obj.put("frameLayoutMode", preset.frameLayoutMode)
        obj.put("logoScaleFactor", preset.logoScaleFactor.toDouble())
        obj.put("frameBackgroundColorIndex", preset.frameBackgroundColorIndex)
        obj.put("thinBorderColorIndex", preset.thinBorderColorIndex)
        obj.put("roundedBorderColorIndex", preset.roundedBorderColorIndex)
        obj.put("borderCornerMode", preset.borderCornerMode)
        obj.put("blurImageCornerRadiusRatio", preset.blurImageCornerRadiusRatio.toDouble())
        obj.put("borderImageScaleFactor", preset.borderImageScaleFactor.toDouble())
        obj.put("subjectOutlineOffsetRatio", preset.subjectOutlineOffsetRatio.toDouble())
        obj.put("subjectOutlineSymbolText", preset.subjectOutlineSymbolText)
        obj.put("subjectOutlineSymbolSizeRatio", preset.subjectOutlineSymbolSizeRatio.toDouble())
        obj.put("floatingBackgroundColorIndex", preset.floatingBackgroundColorIndex)
        obj.put("floatingStrokeColorIndex", preset.floatingStrokeColorIndex)
        obj.put("floatingImageCornerRadiusRatio", preset.floatingImageCornerRadiusRatio.toDouble())
        obj.put("filmFrameIndex", preset.filmFrameIndex)
        obj.put("polaroidFrameIndex", preset.polaroidFrameIndex)
        obj.put("colorPaletteStyleIndex", preset.colorPaletteStyleIndex)
        obj.put("colorPaletteVariantIndex", preset.colorPaletteVariantIndex)
        obj.put("colorPaletteOffsetX", preset.colorPaletteHorizontalOffsetRatio.toDouble())
        obj.put("colorPaletteOffsetY", preset.colorPaletteVerticalOffsetRatio.toDouble())
        obj.put("colorPaletteElementCount", preset.colorPaletteElementCount)
        obj.put("colorPaletteShowHexValue", preset.colorPaletteShowHexValue)
        obj.put("colorPaletteShowRgbValue", preset.colorPaletteShowRgbValue)
        obj.put("paperDotsLayoutDirection", preset.paperDotsLayoutDirection)
        obj.put("paperDotsSplitRatio", preset.paperDotsSplitRatio.toDouble())
        obj.put("paperDotsBackgroundMode", preset.paperDotsBackgroundMode)
        obj.put("paperDotsBackgroundColorIndex", preset.paperDotsBackgroundColorIndex)
        obj.put("paperDotsBackgroundSecondaryColorIndex", preset.paperDotsBackgroundSecondaryColorIndex)
        obj.put("paperDotsStripeSizeRatio", preset.paperDotsStripeSizeRatio.toDouble())
        obj.put("paperDotsShape", preset.paperDotsShape)
        obj.put("paperDotsDotSize", preset.paperDotsDotSize.toDouble())
        obj.put("paperDotsDotVariance", preset.paperDotsDotVariance.toDouble())
        obj.put("paperDotsDotCount", preset.paperDotsDotCount)
        obj.put("paperDotsRandomSeed", preset.paperDotsRandomSeed)
        obj.put("paperDotsStrokeMode", preset.paperDotsStrokeMode)
        obj.put("paperDotsStrokeWidthRatio", preset.paperDotsStrokeWidthRatio.toDouble())
        obj.put("paperDotsStrokeMixRatio", preset.paperDotsStrokeMixRatio.toDouble())
        obj.put("paperDotsPatternText", preset.paperDotsPatternText)
        obj.put("signatureEnabled", preset.signatureEnabled)
        obj.put("signatureText", preset.signatureText)
        obj.put("signatureColorIndex", preset.signatureColorIndex)
        obj.put("signatureOffsetX", preset.signatureHorizontalOffsetRatio.toDouble())
        obj.put("signatureOffsetY", preset.signatureVerticalOffsetRatio.toDouble())
        obj.put("signatureScale", preset.signatureScaleFactor.toDouble())
        obj.put("watermarkFontKey", normalizeWatermarkFontKey(preset.watermarkFontKey))
        obj.put("signatureFontKey", normalizeSignatureFontKey(preset.signatureFontKey))
        obj.put("cutoutOverlayStyle", preset.cutoutOverlayStyle)
        obj.put("cutoutSubjectScaleFactor", preset.cutoutSubjectScaleFactor.toDouble())
        obj.put("cutoutFrameAspectRatio", preset.cutoutFrameAspectRatio)
        obj.put("subjectOutlineEnabled", preset.subjectOutlineEnabled)
        obj.put("subjectOutlineLineStyle", preset.subjectOutlineLineStyle)
        obj.put("subjectOutlineWidthRatio", preset.subjectOutlineWidthRatio.toDouble())
        obj.put("subjectOutlineColorIndex", preset.subjectOutlineColorIndex)
        obj.put("subjectOutlineOuterBorderEnabled", preset.subjectOutlineOuterBorderEnabled)
        array.put(obj)
    }
    return array.toString()
}

private fun decodeWatermarkUserPresets(encoded: String?): List<WatermarkUserPreset> {
    if (encoded.isNullOrBlank()) return emptyList()
    val array = runCatching { JSONArray(encoded) }.getOrElse { return emptyList() }
    val presets = mutableListOf<WatermarkUserPreset>()
    for (index in 0 until array.length()) {
        val obj = array.optJSONObject(index) ?: continue
        val templateNameRaw = obj.optString("templateName", "").trim()
        val template = runCatching { WatermarkTemplatePreset.valueOf(templateNameRaw) }.getOrNull() ?: continue
        val preset = WatermarkUserPreset(
            id = obj.optString("id", "").ifBlank { "${template.name}_$index" },
            name = obj.optString("name", "").trim().ifBlank { "预设${index + 1}" },
            templateName = template.name,
            selectedBrandKey = obj.optString("selectedBrandKey", "").trim().ifBlank { null },
            selectedVariantIndex = obj.optInt("selectedVariantIndex", 0),
            showLogoWatermark = obj.optBoolean("showLogoWatermark", true),
            showModelWatermark = obj.optBoolean("showModelWatermark", false),
            showDateWatermark = obj.optBoolean("showDateWatermark", true),
            showExifWatermark = obj.optBoolean("showExifWatermark", true),
            showLocationWatermark = obj.optBoolean("showLocationWatermark", false),
            showCityLocationWatermark = obj.optBoolean("showCityLocationWatermark", false),
            customModelLabel = obj.optString("customModelLabel", "").trim(),
            customDateLabel = obj.optString("customDateLabel", "").trim(),
            customLocationLabel = obj.optString("customLocationLabel", "").trim(),
            fullColorTextScaleFactor = obj.optDouble(
                "fullColorTextScaleFactor",
                DEFAULT_FULL_COLOR_TEXT_SCALE_FACTOR.toDouble()
            ).toFloat().coerceIn(FULL_COLOR_TEXT_SCALE_MIN, FULL_COLOR_TEXT_SCALE_MAX),
            fullColorBackgroundColorIndex = obj.optInt(
                "fullColorBackgroundColorIndex",
                FULL_COLOR_AUTO_BACKGROUND_INDEX
            ),
            fullColorCustomBackgroundColor = normalizeFullColorOpaqueColor(
                obj.optInt(
                    "fullColorCustomBackgroundColor",
                    DEFAULT_FULL_COLOR_CUSTOM_BACKGROUND_COLOR
                )
            ),
            fullColorUseGradientBackground = obj.optBoolean("fullColorUseGradientBackground", false),
            fullColorGradientPresetIndex = obj.optInt("fullColorGradientPresetIndex", -1),
            fullColorImageOnTop = obj.optBoolean("fullColorImageOnTop", false),
            frameSideTopBorderRatio = obj.optDouble("frameSideTopBorderRatio", 0.022).toFloat(),
            frameLayoutMode = obj.optString("frameLayoutMode", WatermarkFrameLayoutMode.Center.name),
            logoScaleFactor = obj.optDouble("logoScaleFactor", 1.0).toFloat(),
            frameBackgroundColorIndex = obj.optInt("frameBackgroundColorIndex", 8),
            thinBorderColorIndex = obj.optInt("thinBorderColorIndex", 8),
            roundedBorderColorIndex = obj.optInt("roundedBorderColorIndex", 8),
            borderCornerMode = obj.optString("borderCornerMode", BorderCornerMode.Straight.name),
            blurImageCornerRadiusRatio = obj.optDouble(
                "blurImageCornerRadiusRatio",
                DEFAULT_BLUR_IMAGE_CORNER_RATIO.toDouble()
            ).toFloat().coerceIn(0f, 0.18f),
            borderImageScaleFactor = obj.optDouble("borderImageScaleFactor", 1.0).toFloat(),
            floatingBackgroundColorIndex = obj.optInt("floatingBackgroundColorIndex", 8),
            floatingStrokeColorIndex = obj.optInt("floatingStrokeColorIndex", 8),
            floatingImageCornerRadiusRatio = obj.optDouble(
                "floatingImageCornerRadiusRatio",
                DEFAULT_FLOATING_IMAGE_CORNER_RATIO.toDouble()
            ).toFloat().coerceIn(0f, 0.12f),
            filmFrameIndex = obj.optInt("filmFrameIndex", 0),
            polaroidFrameIndex = obj.optInt("polaroidFrameIndex", 0),
            colorPaletteStyleIndex = obj.optInt("colorPaletteStyleIndex", 0),
            colorPaletteVariantIndex = obj.optInt("colorPaletteVariantIndex", 0),
            colorPaletteHorizontalOffsetRatio = obj.optDouble("colorPaletteOffsetX", 0.0).toFloat().coerceIn(-1f, 1f),
            colorPaletteVerticalOffsetRatio = obj.optDouble("colorPaletteOffsetY", 0.0).toFloat().coerceIn(-1f, 1f),
            colorPaletteElementCount = obj.optInt("colorPaletteElementCount", 6).coerceIn(0, 10),
            colorPaletteShowHexValue = obj.optBoolean("colorPaletteShowHexValue", true),
            colorPaletteShowRgbValue = obj.optBoolean("colorPaletteShowRgbValue", true),
            paperDotsLayoutDirection = obj.optString(
                "paperDotsLayoutDirection",
                WatermarkPaperDotsLayoutDirection.ImageTop.name
            ),
            paperDotsSplitRatio = obj.optDouble("paperDotsSplitRatio", 0.5).toFloat().coerceIn(0f, 1f),
            paperDotsBackgroundMode = obj.optString(
                "paperDotsBackgroundMode",
                WatermarkPaperDotsBackgroundMode.Solid.name
            ),
            paperDotsBackgroundColorIndex = obj.optInt(
                "paperDotsBackgroundColorIndex",
                DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_COLOR_INDEX
            ),
            paperDotsBackgroundSecondaryColorIndex = obj.optInt(
                "paperDotsBackgroundSecondaryColorIndex",
                DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_SECONDARY_COLOR_INDEX
            ),
            paperDotsStripeSizeRatio = obj.optDouble(
                "paperDotsStripeSizeRatio",
                DEFAULT_WATERMARK_PAPER_DOTS_STRIPE_SIZE_RATIO.toDouble()
            ).toFloat().coerceIn(0.015f, 0.12f),
            paperDotsShape = obj.optString("paperDotsShape", WatermarkPaperDotsDotShape.Circle.name),
            paperDotsDotSize = obj.optDouble("paperDotsDotSize", 28.0).toFloat().coerceIn(12f, 72f),
            paperDotsDotVariance = obj.optDouble("paperDotsDotVariance", 32.0).toFloat().coerceIn(0f, 100f),
            paperDotsDotCount = obj.optInt("paperDotsDotCount", 18).coerceIn(6, 60),
            paperDotsRandomSeed = obj.optInt("paperDotsRandomSeed", DEFAULT_WATERMARK_PAPER_DOTS_RANDOM_SEED),
            paperDotsStrokeMode = obj.optString("paperDotsStrokeMode", WatermarkPaperDotsStrokeMode.AllFill.name),
            paperDotsStrokeWidthRatio = obj.optDouble("paperDotsStrokeWidthRatio", DEFAULT_WATERMARK_PAPER_DOTS_STROKE_WIDTH_RATIO.toDouble()).toFloat().coerceIn(0.02f, 0.25f),
            paperDotsStrokeMixRatio = obj.optDouble("paperDotsStrokeMixRatio", DEFAULT_WATERMARK_PAPER_DOTS_STROKE_MIX_RATIO.toDouble()).toFloat().coerceIn(0f, 1f),
            paperDotsPatternText = sanitizeWatermarkPaperDotsPatternText(
                obj.optString("paperDotsPatternText", DEFAULT_WATERMARK_PAPER_DOTS_PATTERN_TEXT)
            ),
            signatureEnabled = obj.optBoolean("signatureEnabled", false),
            signatureText = sanitizeEnglishSignature(obj.optString("signatureText", "")),
            signatureColorIndex = obj.optInt("signatureColorIndex", 0),
            signatureHorizontalOffsetRatio = obj.optDouble("signatureOffsetX", 0.0).toFloat().coerceIn(-1f, 1f),
            signatureVerticalOffsetRatio = obj.optDouble("signatureOffsetY", 0.0).toFloat().coerceIn(-1f, 1f),
            signatureScaleFactor = obj.optDouble("signatureScale", 1.0).toFloat().coerceIn(0.65f, 1.8f),
            watermarkFontKey = normalizeWatermarkFontKey(
                obj.optString("watermarkFontKey", DEFAULT_WATERMARK_TEXT_FONT_KEY)
            ),
            signatureFontKey = normalizeSignatureFontKey(
                obj.optString("signatureFontKey", DEFAULT_SIGNATURE_FONT_KEY)
            ),
            cutoutOverlayStyle = obj.optString(
                "cutoutOverlayStyle",
                WatermarkCutoutOverlayStyle.None.name
            ),
            cutoutSubjectScaleFactor = obj.optDouble(
                "cutoutSubjectScaleFactor",
                DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR.toDouble()
            ).toFloat().coerceIn(
                MIN_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
                MAX_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
            ),
            cutoutFrameAspectRatio = obj.optString(
                "cutoutFrameAspectRatio",
                DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO.name
            ),
            subjectOutlineEnabled = obj.optBoolean("subjectOutlineEnabled", false),
            subjectOutlineLineStyle = obj.optString(
                "subjectOutlineLineStyle",
                WatermarkSubjectOutlineLineStyle.Solid.name
            ),
            subjectOutlineWidthRatio = obj.optDouble(
                "subjectOutlineWidthRatio",
                DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO.toDouble()
            ).toFloat().coerceIn(
                MIN_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO,
                MAX_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
            ),
            subjectOutlineColorIndex = obj.optInt(
                "subjectOutlineColorIndex",
                DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX
            ),
            subjectOutlineOuterBorderEnabled = obj.optBoolean("subjectOutlineOuterBorderEnabled", false),
            subjectOutlineOffsetRatio = obj.optDouble(
                "subjectOutlineOffsetRatio",
                DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO.toDouble()
            ).toFloat().coerceIn(
                MIN_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO,
                MAX_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
            ),
            subjectOutlineSymbolText = sanitizeWatermarkSubjectOutlineSymbolText(
                obj.optString("subjectOutlineSymbolText", DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT)
            ),
            subjectOutlineSymbolSizeRatio = obj.optDouble(
                "subjectOutlineSymbolSizeRatio",
                DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO.toDouble()
            ).toFloat().coerceIn(
                MIN_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO,
                MAX_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
            )
        )
        presets += preset
    }
    return presets.distinctBy { it.id }
}

private val WATERMARK_PRESET_SUFFIX_REGEX = Regex(
    pattern = "(?i)\\s*(?:[·•\\-–—]\\s*)?(core\\s*set|variations?)\\s*$"
)

private fun cleanWatermarkPresetDisplayName(rawName: String): String {
    val trimmed = rawName.trim()
    if (trimmed.isBlank()) return ""
    val cleaned = trimmed.replace(WATERMARK_PRESET_SUFFIX_REGEX, "").trim()
    return cleaned.ifBlank { trimmed }
}

private fun WatermarkUserPreset.displayName(
    language: AppLanguage,
    template: WatermarkTemplatePreset
): String {
    val cleaned = cleanWatermarkPresetDisplayName(name)
    if (cleaned.isBlank()) return template.localizedTitle(language)
    val templateAliases = listOf(
        template.name,
        template.localizedTitle(AppLanguage.English),
        template.localizedTitle(AppLanguage.SimplifiedChinese),
        template.localizedTitle(AppLanguage.TraditionalChinese)
    )
    return if (templateAliases.any { alias -> cleaned.equals(alias, ignoreCase = true) }) {
        template.localizedTitle(language)
    } else {
        cleaned
    }
}

private fun nextWatermarkPresetName(items: List<WatermarkUserPreset>): String {
    val used = items.mapNotNull { preset ->
        val name = preset.name.trim()
        if (!name.startsWith("预设")) return@mapNotNull null
        name.removePrefix("预设").toIntOrNull()
    }.toSet()
    var index = 1
    while (used.contains(index)) {
        index += 1
    }
    return "预设$index"
}

private fun ensureWatermarkTempDir(cacheRoot: File): File {
    val dir = File(cacheRoot, WATERMARK_TEMP_SUBDIR)
    if (!dir.exists()) {
        runCatching { dir.mkdirs() }
    }
    return dir
}

private fun isLegacyWatermarkTempFile(file: File): Boolean {
    if (!file.isFile) return false
    val name = file.name.lowercase(Locale.ROOT)
    return name.startsWith("watermark_") || name.startsWith("crop_")
}

private fun trimWatermarkTempFiles(
    cacheRoot: File,
    protectedPaths: Set<String>,
    maxTotalBytes: Long = WATERMARK_TEMP_MAX_BYTES,
    keepNewest: Int = WATERMARK_TEMP_KEEP_NEWEST,
    maxAgeMs: Long = WATERMARK_TEMP_MAX_AGE_MS
) {
    val tempDir = ensureWatermarkTempDir(cacheRoot)
    val protectedSet = protectedPaths.toSet()
    val candidates = mutableListOf<File>()
    tempDir.listFiles()?.forEach { file ->
        if (file.isFile) candidates += file
    }
    cacheRoot.listFiles()?.forEach { file ->
        if (isLegacyWatermarkTempFile(file)) candidates += file
    }
    if (candidates.isEmpty()) return

    val now = System.currentTimeMillis()
    candidates.forEach { file ->
        if (file.absolutePath !in protectedSet && now - file.lastModified() > maxAgeMs) {
            runCatching { file.delete() }
        }
    }

    val remain = candidates
        .filter { it.exists() && it.absolutePath !in protectedSet }
        .sortedByDescending { it.lastModified() }
    var totalBytes = remain.sumOf { it.length() }
    remain.forEachIndexed { index, file ->
        if (index >= keepNewest || totalBytes > maxTotalBytes) {
            val size = file.length()
            runCatching { file.delete() }
            totalBytes -= size
        }
    }
}

private fun WatermarkExifInfo.footerDateLine(): String {
    val normalized = normalizeExifDateForFooter(dateTimeOriginal)
    return normalized
}

private fun WatermarkExifInfo.footerSpecLine(): String {
    val focal = formatFocalLength(focalLength).takeIf { it != "--" }?.let { "${it}mm" }
    val apertureText = formatAperture(aperture).takeIf { it != "--" }?.let { "f/$it" }
    val shutterText = formatShutterSpeedForFooter(shutter).takeIf { it != "--" }
    val isoText = iso.takeIf { it != "--" && it.isNotBlank() }?.let { "ISO$it" }
    return listOfNotNull(focal, apertureText, shutterText, isoText).joinToString(" ")
}

private fun WatermarkExifInfo.footerLocationLine(): String {
    val lat = latitude
    val lon = longitude
    if (lat == null || lon == null) return ""
    val latPrefix = if (lat >= 0) "N" else "S"
    val lonPrefix = if (lon >= 0) "E" else "W"
    val latText = String.format(Locale.US, "%.4f", kotlin.math.abs(lat))
    val lonText = String.format(Locale.US, "%.4f", kotlin.math.abs(lon))
    return "$latPrefix $latText  $lonPrefix $lonText"
}

private fun WatermarkExifInfo.footerCityLocationLine(): String {
    return cityLocation.trim()
}

private fun resolveWatermarkLocationLabel(context: Context, lat: Double?, lon: Double?): String {
    if (lat == null || lon == null) return ""
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return ""
    val englishCity = resolveWatermarkCityLabel(
        context = context,
        lat = lat,
        lon = lon,
        locale = Locale.ENGLISH
    )
    if (englishCity.isNotBlank()) return englishCity
    return resolveWatermarkCityLabel(
        context = context,
        lat = lat,
        lon = lon,
        locale = Locale.getDefault()
    )
}

private fun resolveWatermarkCityLabel(
    context: Context,
    lat: Double,
    lon: Double,
    locale: Locale
): String {
    return runCatching {
        val geocoder = Geocoder(context, locale)
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        val address = addresses?.firstOrNull()
        if (address == null) {
            ""
        } else {
            listOf(
                address.locality,
                address.subAdminArea,
                address.adminArea,
                address.subLocality
            ).firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
        }
    }.getOrDefault("")
}

private fun formatAperture(raw: String): String {
    val value = raw.trim()
    return when {
        value.isBlank() || value == "--" -> "--"
        value.contains("/") -> {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].toDoubleOrNull()
                val denominator = parts[1].toDoubleOrNull()
                if (numerator != null && denominator != null && denominator != 0.0) {
                    val result = numerator / denominator
                    if (result % 1.0 == 0.0) result.toInt().toString() else String.format(Locale.US, "%.1f", result)
                } else {
                    value
                }
            } else {
                value
            }
        }
        else -> value
    }
}

private fun formatFocalLength(raw: String): String {
    val value = raw.trim()
    return when {
        value.isBlank() || value == "--" -> "--"
        value.contains("/") -> {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].toDoubleOrNull()
                val denominator = parts[1].toDoubleOrNull()
                if (numerator != null && denominator != null && denominator != 0.0) {
                    val result = numerator / denominator
                    result.roundToInt().toString()
                } else {
                    value
                }
            } else {
                value
            }
        }
        else -> {
            val asNumber = value.toDoubleOrNull()
            if (asNumber == null) value else asNumber.roundToInt().toString()
        }
    }
}

private fun formatShutterSpeedForFooter(raw: String): String {
    val value = raw.trim()
    if (value.isBlank() || value == "--") return "--"
    if (value.contains("/")) return value
    val numeric = value.toDoubleOrNull() ?: return value
    if (numeric <= 0.0) return "--"
    return if (numeric >= 1.0) {
        numeric.roundToInt().toString()
    } else {
        val denominator = (1.0 / numeric).roundToInt().coerceAtLeast(1)
        "1/$denominator"
    }
}

private data class FullColorTimeDraft(
    val hour: String,
    val minute: String,
    val isPm: Boolean
)

private fun parseFlexibleFullColorTime(raw: String): Date? {
    if (raw.isBlank()) return null
    val patterns = listOf(
        "yyyy:MM:dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy:MM:dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm",
        "HH:mm",
        "H:mm",
        "h:mm a",
        "hh:mm a"
    )
    patterns.forEach { pattern ->
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(raw)
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

private fun resolveFullColorTimeDraft(
    customValue: String,
    exifValue: String?
): FullColorTimeDraft {
    val seed = customValue.trim().ifBlank { exifValue?.trim().orEmpty() }
    val parsed = parseFlexibleFullColorTime(seed) ?: Date()
    val calendar = java.util.Calendar.getInstance(Locale.US).apply { time = parsed }
    val hour24 = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val hour12 = ((hour24 + 11) % 12) + 1
    return FullColorTimeDraft(
        hour = hour12.toString().padStart(2, '0'),
        minute = minute.toString().padStart(2, '0'),
        isPm = hour24 >= 12
    )
}

private fun buildFullColorCustomTimeLabel(
    hourInput: String,
    minuteInput: String,
    isPm: Boolean
): String {
    val hour = hourInput.trim().toIntOrNull() ?: return ""
    val minute = minuteInput.trim().toIntOrNull() ?: return ""
    if (hour !in 1..12 || minute !in 0..59) return ""
    return String.format(
        Locale.US,
        "%02d:%02d %s",
        hour,
        minute,
        if (isPm) "PM" else "AM"
    )
}

private fun normalizeExifDate(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        val input = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        val output = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = input.parse(raw)
        if (date != null) output.format(date) else raw
    }.getOrDefault(raw)
}

private fun normalizeExifDateForFooter(raw: String): String {
    if (raw.isBlank()) return ""
    val patterns = listOf(
        "yyyy:MM:dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy:MM:dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss.SSS"
    )
    patterns.forEach { pattern ->
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(raw)
        }.getOrNull()
        if (parsed != null) {
            return SimpleDateFormat("yyyy-MM-dd HH.mm.ss.SSS", Locale.getDefault()).format(parsed)
        }
    }
    return raw
}

private data class ImportedWatermarkSource(
    val uri: Uri,
    val file: File
)

private suspend fun importPickedImageToWorkingFile(
    context: Context,
    sourceUri: Uri
): ImportedWatermarkSource = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val tempDir = ensureWatermarkTempDir(context.cacheDir)
    val displayName = queryDisplayName(resolver, sourceUri)
    val extension = displayName.substringAfterLast('.', "").ifBlank {
        when (resolver.getType(sourceUri)?.lowercase(Locale.ROOT)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }
    val tempFile = File(tempDir, "source_${System.currentTimeMillis()}.$extension")
    resolver.openInputStream(sourceUri)?.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalStateException("读取图片失败")
    val importedUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    ImportedWatermarkSource(uri = importedUri, file = tempFile)
}

private suspend fun saveWatermarkOutput(
    context: Context,
    outputFile: File,
    outputMimeType: String,
    sourceUri: Uri,
    template: WatermarkTemplatePreset,
    skipExifCopy: Boolean = false
): Uri? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val sourceDisplayName = queryDisplayName(resolver, sourceUri).ifBlank {
        "image_${System.currentTimeMillis()}.jpg"
    }
    val baseName = sourceDisplayName.substringBeforeLast(".").ifBlank { "image_${System.currentTimeMillis()}" }
    val extension = if (outputMimeType == "image/png") "png" else "jpg"
    val fileName = "${baseName}_${template.name.lowercase(Locale.ROOT)}_watermark.$extension"
    val nowSeconds = System.currentTimeMillis() / 1000

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, outputMimeType)
        put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Photoo/Watermark")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null

    runCatching {
        resolver.openOutputStream(savedUri)?.use { output ->
            outputFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("输出流打开失败")

        if (!skipExifCopy) {
            copyPreservedExifMetadata(resolver, sourceUri, savedUri)
        }
    }.onFailure {
        runCatching { resolver.delete(savedUri, null, null) }
        return@withContext null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val pendingValues = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(savedUri, pendingValues, null, null)
    }
    savedUri
}

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String {
    return runCatching {
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index).orEmpty()
            } else {
                ""
            }
        }
    }.getOrNull().orEmpty()
}

private fun shareWatermarkOutput(
    context: Context,
    outputFile: File,
    outputMimeType: String
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = outputMimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, outputFile.name)
        clipData = ClipData.newUri(context.contentResolver, outputFile.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享"))
}

@Composable
private fun WatermarkQuickCropOptionCard(
    title: String,
    ratioLabel: String,
    selected: Boolean,
    accentColor: Color,
    cardColor: Color,
    borderColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    Surface(
        onClick = {
            if (hapticsAllowed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else cardColor,
        modifier = Modifier
            .widthIn(min = 76.dp)
            .heightIn(min = 58.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else titleColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = ratioLabel,
                fontSize = 11.sp,
                color = if (selected) accentColor else subtitleColor
            )
        }
    }
}

private fun watermarkQuickCropPresetIcon(ratioLabel: String) = when (ratioLabel) {
    "1:1" -> Icons.Outlined.CropSquare
    "3:4", "9:16" -> Icons.Outlined.CropPortrait
    else -> Icons.Outlined.CropOriginal
}

private val EXIF_COPY_EXCLUDED_TAGS = setOf(
    ExifInterface.TAG_ORIENTATION,
    ExifInterface.TAG_IMAGE_WIDTH,
    ExifInterface.TAG_IMAGE_LENGTH,
    ExifInterface.TAG_PIXEL_X_DIMENSION,
    ExifInterface.TAG_PIXEL_Y_DIMENSION,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
    ExifInterface.TAG_STRIP_OFFSETS,
    ExifInterface.TAG_STRIP_BYTE_COUNTS,
    ExifInterface.TAG_ROWS_PER_STRIP
)

private val EXIF_COPY_TAGS: List<String> by lazy {
    ExifInterface::class.java.fields
        .filter { field ->
            field.type == String::class.java && field.name.startsWith("TAG_")
        }
        .mapNotNull { field ->
            runCatching { field.get(null) as? String }.getOrNull()
        }
        .filterNot { it in EXIF_COPY_EXCLUDED_TAGS }
        .distinct()
}

private fun copyExifMetadata(
    resolver: ContentResolver,
    sourceUri: Uri,
    targetUri: Uri
) {
    runCatching {
        val sourceExif = openExifInterface(resolver, sourceUri) ?: return
        resolver.openFileDescriptor(targetUri, "rw")?.use { descriptor ->
            val targetExif = ExifInterface(descriptor.fileDescriptor)
            copyExifAttributes(sourceExif, targetExif)
        }
    }
}

private fun copyExifMetadataToFile(
    resolver: ContentResolver,
    sourceUri: Uri,
    targetFile: File
) {
    runCatching {
        val sourceExif = openExifInterface(resolver, sourceUri) ?: return
        val targetExif = ExifInterface(targetFile.absolutePath)
        copyExifAttributes(sourceExif, targetExif)
    }
}

private fun openExifInterface(
    resolver: ContentResolver,
    uri: Uri
): ExifInterface? {
    return runCatching {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor)
        }
    }.getOrNull() ?: runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream)
        }
    }.getOrNull()
}

private fun copyExifAttributes(sourceExif: ExifInterface, targetExif: ExifInterface) {
    EXIF_COPY_TAGS.forEach { tag ->
        val value = sourceExif.getAttribute(tag) ?: return@forEach
        runCatching { targetExif.setAttribute(tag, value) }
    }
    targetExif.setAttribute(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL.toString()
    )
    targetExif.saveAttributes()
}
