package com.voicelike.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlin.math.min
import kotlin.math.roundToInt

fun Modifier.shadow(
    elevation: Dp,
    shape: Shape
) = this.graphicsLayer {
    this.shadowElevation = elevation.toPx()
    this.shape = shape
    this.clip = false
}

fun formatShutterSpeed(shutter: String): String {
    if (shutter == "--") return "--"
    return try {
        val value = shutter.toDouble()
        if (value < 1.0 && value > 0) {
            val reciprocal = (1.0 / value).roundToInt()
            "1/${reciprocal}s"
        } else {
            val text = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            "${text}s"
        }
    } catch (_: Exception) {
        shutter
    }
}

fun normalizePendingTrashIds(pendingIds: Set<Long>, allMedia: List<MediaItem>): Set<Long> {
    if (pendingIds.isEmpty()) return emptySet()
    val allIds = buildMediaIdSet(allMedia)
    return pendingIds.filterTo(HashSet()) { it in allIds }
}

fun countPendingTrash(pendingIds: Set<Long>, allMedia: List<MediaItem>): Int {
    if (pendingIds.isEmpty()) return 0
    val allIds = buildMediaIdSet(allMedia)
    return pendingIds.count { allIds.contains(it) }
}

@Composable
internal fun TimeMachineBenchIcon(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 64f
        val sy = size.height / 64f
        val strokeWidth = 2f * min(sx, sy)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val platformPath = Path().apply {
            moveTo(56f * sx, 36f * sy)
            lineTo(12f * sx, 36f * sy)
            lineTo(4f * sx, 40f * sy)
            lineTo(48f * sx, 40f * sy)
            close()
        }
        drawPath(path = platformPath, color = tint, style = stroke)

        drawRoundRect(
            color = tint,
            topLeft = Offset(32f * sx, 26f * sy),
            size = Size(12f * sx, 10f * sy),
            cornerRadius = CornerRadius(2f * sx, 2f * sy),
            style = stroke
        )

        val lampPath = Path().apply {
            moveTo(14f * sx, 36f * sy)
            lineTo(14f * sx, 20f * sy)
            quadraticBezierTo(14f * sx, 16f * sy, 18f * sx, 16f * sy)
            lineTo(21f * sx, 16f * sy)
        }
        drawPath(path = lampPath, color = tint, style = stroke)

        drawLine(
            color = tint,
            start = Offset(21f * sx, 16f * sy),
            end = Offset(23f * sx, 16f * sy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawOval(
            color = tint,
            topLeft = Offset(23f * sx, 14f * sy),
            size = Size(8f * sx, 4f * sy),
            style = stroke
        )
    }
}
