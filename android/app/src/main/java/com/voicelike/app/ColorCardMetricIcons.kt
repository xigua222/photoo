package com.voicelike.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object ColorCardMetricIcons {
    val Blend: ImageVector by lazy {
        ImageVector.Builder(
            name = "ColorCardMetricBlend",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlinePath {
                moveTo(16f, 9f)
                arcToRelative(7f, 7f, 0f, true, true, -14f, 0f)
                arcToRelative(7f, 7f, 0f, true, true, 14f, 0f)
            }
            outlinePath {
                moveTo(22f, 15f)
                arcToRelative(7f, 7f, 0f, true, true, -14f, 0f)
                arcToRelative(7f, 7f, 0f, true, true, 14f, 0f)
            }
        }.build()
    }

    val Contrast: ImageVector by lazy {
        ImageVector.Builder(
            name = "ColorCardMetricContrast",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlinePath {
                moveTo(22f, 12f)
                arcToRelative(10f, 10f, 0f, true, true, -20f, 0f)
                arcToRelative(10f, 10f, 0f, true, true, 20f, 0f)
            }
            outlinePath {
                moveTo(12f, 18f)
                arcToRelative(6f, 6f, 0f, false, false, 0f, -12f)
                verticalLineTo(18f)
                close()
            }
        }.build()
    }

    val Rainbow: ImageVector by lazy {
        ImageVector.Builder(
            name = "ColorCardMetricRainbow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlinePath {
                moveTo(22f, 17f)
                arcToRelative(10f, 10f, 0f, false, false, -20f, 0f)
            }
            outlinePath {
                moveTo(18f, 17f)
                arcToRelative(6f, 6f, 0f, false, false, -12f, 0f)
            }
            outlinePath {
                moveTo(14f, 17f)
                arcToRelative(2f, 2f, 0f, false, false, -4f, 0f)
            }
        }.build()
    }

    val VectorSquare: ImageVector by lazy {
        ImageVector.Builder(
            name = "ColorCardMetricVectorSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlinePath {
                moveTo(19.5f, 7f)
                arcToRelative(24f, 24f, 0f, false, true, 0f, 10f)
            }
            outlinePath {
                moveTo(4.5f, 7f)
                arcToRelative(24f, 24f, 0f, false, false, 0f, 10f)
            }
            outlinePath {
                moveTo(7f, 19.5f)
                arcToRelative(24f, 24f, 0f, false, false, 10f, 0f)
            }
            outlinePath {
                moveTo(7f, 4.5f)
                arcToRelative(24f, 24f, 0f, false, true, 10f, 0f)
            }
            outlinePath {
                roundedRect(left = 17f, top = 17f, right = 22f, bottom = 22f, radius = 1f)
            }
            outlinePath {
                roundedRect(left = 17f, top = 2f, right = 22f, bottom = 7f, radius = 1f)
            }
            outlinePath {
                roundedRect(left = 2f, top = 17f, right = 7f, bottom = 22f, radius = 1f)
            }
            outlinePath {
                roundedRect(left = 2f, top = 2f, right = 7f, bottom = 7f, radius = 1f)
            }
        }.build()
    }
}

private fun ImageVector.Builder.outlinePath(pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = pathBuilder
    )
}

private fun androidx.compose.ui.graphics.vector.PathBuilder.roundedRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float
) {
    moveTo(left + radius, top)
    horizontalLineTo(right - radius)
    arcToRelative(radius, radius, 0f, false, true, radius, radius)
    verticalLineTo(bottom - radius)
    arcToRelative(radius, radius, 0f, false, true, -radius, radius)
    horizontalLineTo(left + radius)
    arcToRelative(radius, radius, 0f, false, true, -radius, -radius)
    verticalLineTo(top + radius)
    arcToRelative(radius, radius, 0f, false, true, radius, -radius)
    close()
}
