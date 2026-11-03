package com.voicelike.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.liquidglass.LocalLiquidGlassBackdrop
import com.voicelike.app.liquidglass.liquidGlassControl
import com.voicelike.app.liquidglass.liquidGlassSurfaceColor

private val MAIN_TOP_ACTION_RAIL_TOP_PADDING = 48.dp
private val MAIN_TOP_ACTION_RAIL_HORIZONTAL_PADDING = 24.dp
private val MAIN_STATS_ENTRY_ICON: ImageVector by lazy {
    ImageVector.Builder(
        name = "MainStatsEntryIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        outlinePath {
            moveTo(21f, 7.35304f)
            lineTo(21f, 16.647f)
            curveTo(21f, 16.8649f, 20.8819f, 17.0656f, 20.6914f, 17.1715f)
            lineTo(12.2914f, 21.8381f)
            curveTo(12.1102f, 21.9388f, 11.8898f, 21.9388f, 11.7086f, 21.8381f)
            lineTo(3.30861f, 17.1715f)
            curveTo(3.11814f, 17.0656f, 3f, 16.8649f, 3f, 16.647f)
            lineTo(2.99998f, 7.35304f)
            curveTo(2.99998f, 7.13514f, 3.11812f, 6.93437f, 3.3086f, 6.82855f)
            lineTo(11.7086f, 2.16188f)
            curveTo(11.8898f, 2.06121f, 12.1102f, 2.06121f, 12.2914f, 2.16188f)
            lineTo(20.6914f, 6.82855f)
            curveTo(20.8818f, 6.93437f, 21f, 7.13514f, 21f, 7.35304f)
            close()
        }
        outlinePath {
            moveTo(20.5f, 16.7222f)
            lineTo(12.2914f, 12.1619f)
            curveTo(12.1102f, 12.0612f, 11.8898f, 12.0612f, 11.7086f, 12.1619f)
            lineTo(3.5f, 16.7222f)
        }
        outlinePath {
            moveTo(3.52844f, 7.29357f)
            lineTo(11.7086f, 11.8381f)
            curveTo(11.8898f, 11.9388f, 12.1102f, 11.9388f, 12.2914f, 11.8381f)
            lineTo(20.5f, 7.27777f)
        }
        outlinePath {
            moveTo(12f, 21f)
            lineTo(12f, 3f)
        }
    }.build()
}

@Composable
fun MainTopActionRail(
    modifier: Modifier = Modifier,
    startContent: @Composable BoxScope.() -> Unit = {},
    centerContent: @Composable BoxScope.() -> Unit = {},
    endContent: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = MAIN_TOP_ACTION_RAIL_TOP_PADDING,
                start = MAIN_TOP_ACTION_RAIL_HORIZONTAL_PADDING,
                end = MAIN_TOP_ACTION_RAIL_HORIZONTAL_PADDING
            )
    ) {
        startContent()
        centerContent()
        endContent()
    }
}

@Composable
fun MainStatsEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .size(40.dp)
            .bounceClick(onClick = onClick)
            .mainTopCircleSurface(isDark),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MAIN_STATS_ENTRY_ICON,
            contentDescription = "Stats",
            tint = if (isDark) {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            } else {
                Color.Black.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MainHomeBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .size(40.dp)
            .bounceClick(onClick = onClick)
            .mainTopCircleSurface(isDark),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = if (isDark) {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            } else {
                Color.Black.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun Modifier.mainTopCircleSurface(isDark: Boolean): Modifier {
    val backdrop = LocalLiquidGlassBackdrop.current
    return if (backdrop != null) {
        clip(CircleShape).liquidGlassControl(
            backdrop = backdrop,
            shape = CircleShape,
            surfaceColor = liquidGlassSurfaceColor(isLightTheme = !isDark)
        )
    } else {
        clip(CircleShape).background(
            if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.White.copy(alpha = 0.5f)
            }
        )
    }
}

private fun ImageVector.Builder.outlinePath(
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
) {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = pathBuilder
    )
}

@Composable
fun MainDeleteQueueButton(
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pendingCount <= 0) return

    val isDark = isSystemInDarkTheme()
    val buttonShape = RoundedCornerShape(20.dp)
    val glassBackdrop = LocalLiquidGlassBackdrop.current
    Button(
        onClick = onClick,
        modifier = modifier
            .bounceClick()
            .then(
                if (glassBackdrop != null) {
                    Modifier.liquidGlassControl(
                        backdrop = glassBackdrop,
                        shape = buttonShape,
                        surfaceColor = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.32f) else Color(0xFFFEF2F2).copy(alpha = 0.6f)
                    )
                } else {
                    Modifier
                }
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                glassBackdrop != null -> Color.Transparent
                isDark -> Color(0xFF3A1717)
                else -> Color(0xFFFEF2F2)
            }
        ),
        shape = buttonShape,
        border = if (glassBackdrop != null) {
            null
        } else {
            BorderStroke(
                1.dp,
                if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA)
            )
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Delete Queue",
            tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        SlideText(
            targetValue = pendingCount,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
        )
    }
}
