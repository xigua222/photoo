package com.voicelike.app.colorcard.lensy

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import kotlin.math.max
import kotlin.math.roundToInt

internal data class ColorCardLensyAssessment(
    val message: String
)

private data class ColorCardLensyIssue(
    val severity: Int,
    val priority: Int,
    val messageOptions: List<String>,
    val messageSeed: Int
)

internal data class ColorCardLensyFrameStats(
    val pixelCount: Int,
    val luminanceHistogram: IntArray,
    val redHistogram: IntArray,
    val greenHistogram: IntArray,
    val blueHistogram: IntArray,
    val highlightClipRatio: Float,
    val shadowClipRatio: Float,
    val redHighClipRatio: Float,
    val greenHighClipRatio: Float,
    val blueHighClipRatio: Float,
    val redLowClipRatio: Float,
    val greenLowClipRatio: Float,
    val blueLowClipRatio: Float,
    val saturationOverflowRatio: Float
)

internal fun analyzeColorCardLensyBitmap(
    bitmap: Bitmap
): ColorCardLensyAssessment? {
    if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
    val stats = computeColorCardLensyFrameStats(bitmap)
    return buildColorCardLensyAssessment(stats)
}

internal fun buildColorCardLensyAssessment(
    stats: ColorCardLensyFrameStats
): ColorCardLensyAssessment? {
    if (stats.pixelCount <= 0) return null

    val p1Luma = histogramPercentile(stats.luminanceHistogram, stats.pixelCount, 0.01f) / 255f
    val p5Luma = histogramPercentile(stats.luminanceHistogram, stats.pixelCount, 0.05f) / 255f
    val p95Luma = histogramPercentile(stats.luminanceHistogram, stats.pixelCount, 0.95f) / 255f
    val p99Luma = histogramPercentile(stats.luminanceHistogram, stats.pixelCount, 0.99f) / 255f
    val luminanceSpan = p99Luma - p1Luma
    val midtoneSpan = p95Luma - p5Luma

    val highlightSeverity = when {
        stats.highlightClipRatio >= 0.046f && p99Luma >= 0.997f -> 3
        stats.highlightClipRatio >= 0.028f && p99Luma >= 0.994f -> 2
        stats.highlightClipRatio >= 0.016f && p99Luma >= 0.990f -> 1
        else -> 0
    }
    val shadowSeverity = when {
        stats.shadowClipRatio >= 0.046f && p1Luma <= 0.005f -> 3
        stats.shadowClipRatio >= 0.028f && p1Luma <= 0.009f -> 2
        stats.shadowClipRatio >= 0.016f && p1Luma <= 0.015f -> 1
        else -> 0
    }
    val saturationSeverity = when {
        stats.saturationOverflowRatio >= 0.180f && p95Luma >= 0.50f -> 3
        stats.saturationOverflowRatio >= 0.125f && p95Luma >= 0.45f -> 2
        stats.saturationOverflowRatio >= 0.085f && p95Luma >= 0.40f -> 1
        else -> 0
    }
    val contrastSeverity = when {
        luminanceSpan >= 0.990f && midtoneSpan >= 0.89f && (highlightSeverity >= 2 || shadowSeverity >= 2) -> 3
        luminanceSpan >= 0.980f && midtoneSpan >= 0.85f && (highlightSeverity >= 2 || shadowSeverity >= 2) -> 2
        luminanceSpan >= 0.968f && midtoneSpan >= 0.81f &&
            (highlightSeverity >= 1 || shadowSeverity >= 1) &&
            stats.saturationOverflowRatio < 0.18f -> 1
        else -> 0
    }

    val combinedIssue = when {
        highlightSeverity >= 2 && shadowSeverity >= 2 -> {
            ColorCardLensyIssue(
                severity = max(highlightSeverity, shadowSeverity),
                priority = 0,
                messageOptions = listOf(
                    "明暗两头都已经比较紧了，先把强度收一点，再把亮度往中间带回去。",
                    "高光和阴影都在贴边，先降一点强度，再轻微回调亮度会更稳。",
                    "整体反差已经过硬，建议先减弱效果，再把亮部和暗部往中间收一收。"
                ),
                messageSeed = ((stats.highlightClipRatio + stats.shadowClipRatio) * 10000f).roundToInt()
            )
        }
        else -> null
    }

    val issues = buildList {
        combinedIssue?.let(::add)
        if (combinedIssue == null && highlightSeverity > 0) {
            add(
                ColorCardLensyIssue(
                    severity = highlightSeverity,
                    priority = 1,
                    messageOptions = listOf(
                        "亮部已经开始贴边了，试着把亮度或强度略微收一点。",
                        "高光空间有点紧，先降一点强度，白色层次通常会回来一些。",
                        "亮部细节开始变硬了，建议先把效果减弱一点再看。"
                    ),
                    messageSeed = (stats.highlightClipRatio * 10000f).roundToInt()
                )
            )
        }
        if (combinedIssue == null && shadowSeverity > 0) {
            add(
                ColorCardLensyIssue(
                    severity = shadowSeverity,
                    priority = 2,
                    messageOptions = listOf(
                        "暗部已经压得有点重了，试着抬一点亮度或减弱强度。",
                        "阴影空间开始变紧，先回一点亮度，暗部层次会更容易出来。",
                        "黑位有点闷，建议先把效果收一点，再轻轻提亮。"
                    ),
                    messageSeed = (stats.shadowClipRatio * 10000f).roundToInt()
                )
            )
        }
        strongestColorClipIssue(stats)?.let(::add)
        if (saturationSeverity > 0) {
            add(
                ColorCardLensyIssue(
                    severity = saturationSeverity,
                    priority = 4,
                    messageOptions = listOf(
                        "颜色已经有点过满了，试着先降一点饱和度或总强度。",
                        "鲜艳区域开始冒头，回一点饱和度通常会更耐看。",
                        "颜色有些溢出了，建议先收饱和度，再看是否还要减弱强度。"
                    ),
                    messageSeed = (stats.saturationOverflowRatio * 10000f).roundToInt()
                )
            )
        }
        if (contrastSeverity > 0) {
            add(
                ColorCardLensyIssue(
                    severity = contrastSeverity,
                    priority = 5,
                    messageOptions = listOf(
                        "整体反差已经偏硬了，试着先减弱强度，必要时把色调往回收一点。",
                        "中间层次开始变薄了，先降强度通常比继续硬推更稳。",
                        "对比已经有些顶，建议先把效果减一点，再看层次是否回来。"
                    ),
                    messageSeed = (luminanceSpan * 1000f).roundToInt() + (midtoneSpan * 1000f).roundToInt()
                )
            )
        }
    }

    val chosen = issues
        .sortedWith(compareByDescending<ColorCardLensyIssue> { it.severity }.thenBy { it.priority })
        .firstOrNull()
        ?: return null

    if (chosen.severity < 3) {
        return null
    }

    return ColorCardLensyAssessment(
        message = chooseColorCardLensyMessage(chosen.messageOptions, chosen.messageSeed)
    )
}

private fun computeColorCardLensyFrameStats(
    bitmap: Bitmap
): ColorCardLensyFrameStats {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val luminanceHistogram = IntArray(256)
    val redHistogram = IntArray(256)
    val greenHistogram = IntArray(256)
    val blueHistogram = IntArray(256)

    var highlightClipCount = 0
    var shadowClipCount = 0
    var redHighClipCount = 0
    var greenHighClipCount = 0
    var blueHighClipCount = 0
    var redLowClipCount = 0
    var greenLowClipCount = 0
    var blueLowClipCount = 0
    var saturationOverflowCount = 0

    val hsv = FloatArray(3)
    pixels.forEach { color ->
        val alpha = AndroidColor.alpha(color)
        if (alpha <= 0) return@forEach

        val red = AndroidColor.red(color)
        val green = AndroidColor.green(color)
        val blue = AndroidColor.blue(color)
        val luminance = (
            (red * 0.2126f) +
                (green * 0.7152f) +
                (blue * 0.0722f)
            ).toInt().coerceIn(0, 255)

        luminanceHistogram[luminance] += 1
        redHistogram[red] += 1
        greenHistogram[green] += 1
        blueHistogram[blue] += 1

        if (luminance >= 252) highlightClipCount += 1
        if (luminance <= 4) shadowClipCount += 1
        if (red >= 252) redHighClipCount += 1
        if (green >= 252) greenHighClipCount += 1
        if (blue >= 252) blueHighClipCount += 1
        if (red <= 3) redLowClipCount += 1
        if (green <= 3) greenLowClipCount += 1
        if (blue <= 3) blueLowClipCount += 1

        AndroidColor.RGBToHSV(red, green, blue, hsv)
        if (hsv[1] >= 0.94f && hsv[2] >= 0.55f) {
            saturationOverflowCount += 1
        }
    }

    val pixelCount = luminanceHistogram.sum().coerceAtLeast(1)
    return ColorCardLensyFrameStats(
        pixelCount = pixelCount,
        luminanceHistogram = luminanceHistogram,
        redHistogram = redHistogram,
        greenHistogram = greenHistogram,
        blueHistogram = blueHistogram,
        highlightClipRatio = highlightClipCount.toFloat() / pixelCount.toFloat(),
        shadowClipRatio = shadowClipCount.toFloat() / pixelCount.toFloat(),
        redHighClipRatio = redHighClipCount.toFloat() / pixelCount.toFloat(),
        greenHighClipRatio = greenHighClipCount.toFloat() / pixelCount.toFloat(),
        blueHighClipRatio = blueHighClipCount.toFloat() / pixelCount.toFloat(),
        redLowClipRatio = redLowClipCount.toFloat() / pixelCount.toFloat(),
        greenLowClipRatio = greenLowClipCount.toFloat() / pixelCount.toFloat(),
        blueLowClipRatio = blueLowClipCount.toFloat() / pixelCount.toFloat(),
        saturationOverflowRatio = saturationOverflowCount.toFloat() / pixelCount.toFloat()
    )
}

private fun strongestColorClipIssue(
    stats: ColorCardLensyFrameStats
): ColorCardLensyIssue? {
    val issueCandidates = listOf(
        stats.redHighClipRatio to stats.redLowClipRatio,
        stats.greenHighClipRatio to stats.greenLowClipRatio,
        stats.blueHighClipRatio to stats.blueLowClipRatio
    ).mapNotNull { (highRatio, lowRatio) ->
        val highGap = highRatio - stats.highlightClipRatio
        val lowGap = lowRatio - stats.shadowClipRatio
        when {
            highRatio >= 0.040f && highGap >= 0.010f -> {
                ColorCardLensyIssue(
                    severity = 3,
                    priority = 3,
                    messageOptions = listOf(
                        "局部颜色已经明显贴边了，先把饱和度或强度收一点。",
                        "颜色层次开始断开了，先回一点饱和度通常最直接。",
                        "局部颜色有明显溢出，建议先减弱效果，再看细节是否回来。"
                    ),
                    messageSeed = ((highRatio + highGap) * 10000f).roundToInt()
                )
            }
            lowRatio >= 0.040f && lowGap >= 0.010f -> {
                ColorCardLensyIssue(
                    severity = 3,
                    priority = 3,
                    messageOptions = listOf(
                        "局部颜色已经压得太死了，试着先提一点亮度或减弱强度。",
                        "深色颜色有些堵，先把效果收一点会更容易恢复层次。",
                        "局部颜色已经压成一团了，建议先减弱效果，再轻微提亮。"
                    ),
                    messageSeed = ((lowRatio + lowGap) * 10000f).roundToInt()
                )
            }
            highRatio >= 0.026f && highGap >= 0.007f -> {
                ColorCardLensyIssue(
                    severity = 2,
                    priority = 3,
                    messageOptions = listOf(
                        "局部颜色已经开始偏硬了，可以考虑稍微收一点饱和度。",
                        "颜色边缘开始变紧，回一点强度会更稳。",
                        "局部颜色有点顶，先轻轻减一点效果更合适。"
                    ),
                    messageSeed = ((highRatio + highGap) * 10000f).roundToInt()
                )
            }
            lowRatio >= 0.026f && lowGap >= 0.007f -> {
                ColorCardLensyIssue(
                    severity = 2,
                    priority = 3,
                    messageOptions = listOf(
                        "局部颜色已经有点闷了，可以试着轻微提亮。",
                        "深色区域开始挤在一起，收一点强度会更自然。",
                        "颜色暗部开始发堵了，回一点亮度会更稳。"
                    ),
                    messageSeed = ((lowRatio + lowGap) * 10000f).roundToInt()
                )
            }
            else -> null
        }
    }

    return issueCandidates.maxWithOrNull(
        compareBy<ColorCardLensyIssue>({ it.severity }, { -it.priority })
    )
}

private fun histogramPercentile(
    histogram: IntArray,
    pixelCount: Int,
    percentile: Float
): Int {
    if (pixelCount <= 0) return 0
    val target = (pixelCount * percentile.coerceIn(0f, 1f)).toInt()
    var running = 0
    histogram.forEachIndexed { value, count ->
        running += count
        if (running >= target) {
            return value
        }
    }
    return histogram.lastIndex
}

private fun chooseColorCardLensyMessage(
    options: List<String>,
    seed: Int
): String {
    if (options.isEmpty()) return ""
    val index = seed.mod(options.size)
    return options[index]
}
