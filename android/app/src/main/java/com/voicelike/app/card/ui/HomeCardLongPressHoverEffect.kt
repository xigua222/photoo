package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect as AndroidRect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.PixelCopy
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import android.app.Activity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private val HOME_CARD_HOVER_IMAGE_LIFT = 36.dp
private val HOME_CARD_HOVER_IMAGE_SHADOW = 84.dp
private const val HOME_CARD_HOVER_IMAGE_SCALE = 1.065f
private const val HOME_CARD_HOVER_IMAGE_TILT_MAX_X = 0f
private const val HOME_CARD_HOVER_IMAGE_TILT_MAX_Y = 0f
private const val HOME_CARD_HOVER_IMAGE_TILT_MAX_Z = 0f
private const val HOME_CARD_HOVER_IMAGE_BASE_TILT_X = 0f
private const val HOME_CARD_HOVER_IMAGE_BASE_TILT_Y = 0f
private const val HOME_CARD_HOVER_IMAGE_BASE_TILT_Z = 0f
private const val HOME_CARD_HOVER_IMAGE_CAMERA_DISTANCE = 32f
private val HOME_CARD_HOVER_IMAGE_EDGE_INSET = 1.6.dp
private const val HOME_CARD_HOVER_IMAGE_SHELL_ALPHA = 0.11f
private val HOME_CARD_HOVER_BACKDROP_BLUR = 92.dp
private const val HOME_CARD_HOVER_BACKDROP_ALPHA = 1f
private const val HOME_CARD_HOVER_IMAGE_BORDER_ALPHA = 0.18f
private const val HOME_CARD_HOVER_TOUCH_SLOP_MULTIPLIER = 1.1f
private const val HOME_CARD_HOVER_SNAPSHOT_DOWNSAMPLE = 6
private const val HOME_CARD_HOVER_EXTRA_LONG_PRESS_DELAY_MS = 320L

internal data class HomeCardLongPressHoverVisuals(
    val active: Boolean,
    val imageLift: Dp,
    val imageShadowElevation: Dp,
    val imageScale: Float,
    val imageTiltProgress: Float,
    val imageOverlayAlpha: Float,
    val imageBorderAlpha: Float,
    val footerBlurRadius: Dp,
    val footerContentAlpha: Float,
    val footerScrimAlpha: Float,
    val backdropBlurRadius: Dp,
    val backdropAlpha: Float
)

@Composable
internal fun rememberHomeCardLongPressHoverVisuals(
    active: Boolean
): HomeCardLongPressHoverVisuals {
    val imageLift by animateDpAsState(
        targetValue = if (active) HOME_CARD_HOVER_IMAGE_LIFT else 0.dp,
        animationSpec = if (active) {
            spring(stiffness = 360f, dampingRatio = 0.78f)
        } else {
            spring(stiffness = 250f, dampingRatio = 0.94f)
        },
        label = "homeCardHoverImageLift"
    )
    val imageShadowElevation by animateDpAsState(
        targetValue = if (active) HOME_CARD_HOVER_IMAGE_SHADOW else 0.dp,
        animationSpec = if (active) {
            spring(stiffness = 380f, dampingRatio = 0.8f)
        } else {
            spring(stiffness = 230f, dampingRatio = 0.96f)
        },
        label = "homeCardHoverImageShadow"
    )
    val imageScale by animateFloatAsState(
        targetValue = if (active) HOME_CARD_HOVER_IMAGE_SCALE else 1f,
        animationSpec = if (active) {
            spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.8f)
        } else {
            spring(stiffness = 220f, dampingRatio = 0.95f)
        },
        label = "homeCardHoverImageScale"
    )
    val imageTiltProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (active) {
            spring(stiffness = 340f, dampingRatio = 0.8f)
        } else {
            spring(stiffness = 210f, dampingRatio = 0.95f)
        },
        label = "homeCardHoverImageTiltProgress"
    )
    val imageOverlayAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (active) {
            spring(stiffness = 300f, dampingRatio = 0.86f)
        } else {
            spring(stiffness = 220f, dampingRatio = 0.98f)
        },
        label = "homeCardHoverImageAlpha"
    )
    val imageBorderAlpha by animateFloatAsState(
        targetValue = if (active) HOME_CARD_HOVER_IMAGE_BORDER_ALPHA else 0f,
        animationSpec = if (active) {
            spring(stiffness = 300f, dampingRatio = 0.88f)
        } else {
            spring(stiffness = 220f, dampingRatio = 0.98f)
        },
        label = "homeCardHoverImageBorderAlpha"
    )
    val footerBlurRadius by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = spring(stiffness = 540f, dampingRatio = 0.84f),
        label = "homeCardHoverFooterBlur"
    )
    val footerContentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = 560f, dampingRatio = 0.86f),
        label = "homeCardHoverFooterAlpha"
    )
    val footerScrimAlpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(stiffness = 560f, dampingRatio = 0.86f),
        label = "homeCardHoverFooterScrim"
    )
    val backdropBlurRadius by animateDpAsState(
        targetValue = if (active && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HOME_CARD_HOVER_BACKDROP_BLUR
        } else {
            0.dp
        },
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.84f),
        label = "homeCardHoverBackdropBlur"
    )
    val backdropAlpha by animateFloatAsState(
        targetValue = if (active) HOME_CARD_HOVER_BACKDROP_ALPHA else 0f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.88f),
        label = "homeCardHoverBackdropAlpha"
    )

    return HomeCardLongPressHoverVisuals(
        active = active,
        imageLift = imageLift,
        imageShadowElevation = imageShadowElevation,
        imageScale = imageScale,
        imageTiltProgress = imageTiltProgress,
        imageOverlayAlpha = imageOverlayAlpha,
        imageBorderAlpha = imageBorderAlpha,
        footerBlurRadius = footerBlurRadius,
        footerContentAlpha = footerContentAlpha,
        footerScrimAlpha = footerScrimAlpha,
        backdropBlurRadius = backdropBlurRadius,
        backdropAlpha = backdropAlpha
    )
}

internal fun Modifier.homeCardImageLongPressHoverGesture(
    enabled: Boolean,
    onHoverActiveChange: (Boolean) -> Unit
): Modifier {
    if (!enabled) return this
    return this.pointerInput(enabled) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial
            )
            val pointerId = down.id
            val touchSlop = viewConfiguration.touchSlop * HOME_CARD_HOVER_TOUCH_SLOP_MULTIPLIER
            val longPressTimeoutMillis =
                viewConfiguration.longPressTimeoutMillis + HOME_CARD_HOVER_EXTRA_LONG_PRESS_DELAY_MS
            val longPressTriggered = withTimeoutOrNull(longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: return@withTimeoutOrNull false
                    if (!change.pressed) {
                        return@withTimeoutOrNull false
                    }
                    if (hasExceededHoverTouchSlop(down.position.x, down.position.y, change.position.x, change.position.y, touchSlop)) {
                        return@withTimeoutOrNull false
                    }
                }
                false
            } == null

            if (!longPressTriggered) {
                return@awaitEachGesture
            }

            onHoverActiveChange(true)
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) {
                        break
                    }
                    change.consume()
                    if (hasExceededHoverTouchSlop(down.position.x, down.position.y, change.position.x, change.position.y, touchSlop * 1.15f)) {
                        break
                    }
                }
            } finally {
                onHoverActiveChange(false)
            }
        }
    }
}

internal fun Modifier.homeCardFooterHoverEffect(
    visuals: HomeCardLongPressHoverVisuals
): Modifier {
    return this
        .graphicsLayer {
            alpha = visuals.footerContentAlpha
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .then(
            if (visuals.footerBlurRadius > 0.dp) {
                Modifier.blur(visuals.footerBlurRadius)
            } else {
                Modifier
            }
        )
}

internal fun captureHomeCardLongPressBackdropBitmapFallback(rootView: View): Bitmap? {
    val width = rootView.width
    val height = rootView.height
    if (width <= 0 || height <= 0) return null
    val targetWidth = max(1, width / HOME_CARD_HOVER_SNAPSHOT_DOWNSAMPLE)
    val targetHeight = max(1, height / HOME_CARD_HOVER_SNAPSHOT_DOWNSAMPLE)
    return runCatching {
        Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val inverseScale = 1f / HOME_CARD_HOVER_SNAPSHOT_DOWNSAMPLE.toFloat()
            canvas.scale(inverseScale, inverseScale)
            rootView.draw(canvas)
        }
    }.getOrNull()
}

@Composable
internal fun HomeCardLongPressHoverOverlay(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    backdropBitmap: Bitmap?,
    imageRequest: ImageRequest,
    imageBoundsInRoot: Rect?,
    windowSizePx: IntSize,
    isDark: Boolean,
    quote: HomeCardHoverQuotePresentation?,
    canRefreshQuote: Boolean,
    onQuoteRefreshRequest: () -> Unit,
    dismissOnBackdropTap: Boolean = true,
    showCloseButton: Boolean = false,
    imageOverlayContent: @Composable BoxScope.() -> Unit = {},
    footerOverlayContent: @Composable BoxScope.() -> Unit = {},
    onImageReady: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!active) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeCardLongPressHoverBackdrop(
                active = active,
                visuals = visuals,
                backdropBitmap = backdropBitmap,
                isDark = isDark,
                dismissOnTap = dismissOnBackdropTap,
                onDismissRequest = onDismissRequest,
                modifier = Modifier.fillMaxSize()
            )
            HomeCardLongPressHoverImageOverlay(
                active = active,
                visuals = visuals,
                imageRequest = imageRequest,
                imageBoundsInRoot = imageBoundsInRoot,
                windowSizePx = windowSizePx,
                overlayContent = imageOverlayContent,
                onImageReady = onImageReady,
                modifier = Modifier.fillMaxSize()
            )
            HomeCardLongPressHoverQuoteOverlay(
                active = active,
                visuals = visuals,
                quote = quote,
                isDark = isDark,
                canRefreshQuote = canRefreshQuote,
                onQuoteRefreshRequest = onQuoteRefreshRequest,
                modifier = Modifier.fillMaxSize()
            )
            footerOverlayContent()
            if (showCloseButton) {
                HomeCardLongPressHoverCloseButton(
                    active = active,
                    visuals = visuals,
                    isDark = isDark,
                    onDismissRequest = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
internal fun HomeCardLongPressHoverBackdrop(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    backdropBitmap: Bitmap?,
    isDark: Boolean,
    dismissOnTap: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!active) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (dismissOnTap) {
                    Modifier.pointerInput(onDismissRequest) {
                        detectTapGestures(
                            onTap = { onDismissRequest() }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val scrimColor = if (isDark) {
            Color.Black.copy(alpha = 0.24f)
        } else {
            Color.White.copy(alpha = 0.18f)
        }
        val bottomShade = if (isDark) {
            Color.Black.copy(alpha = 0.34f)
        } else {
            Color.White.copy(alpha = 0.22f)
        }

        backdropBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = visuals.backdropAlpha
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .then(
                        if (visuals.backdropBlurRadius > 0.dp) {
                            Modifier.blur(visuals.backdropBlurRadius)
                        } else {
                            Modifier
                        }
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = visuals.backdropAlpha
                }
                .background(scrimColor)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = visuals.backdropAlpha
                }
                .background(
                    Brush.verticalGradient(
                        0f to scrimColor.copy(alpha = scrimColor.alpha * 0.12f),
                        0.34f to scrimColor.copy(alpha = scrimColor.alpha * 0.34f),
                        1f to bottomShade
                    )
                )
        )
    }
}

@Composable
private fun HomeCardLongPressHoverCloseButton(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    isDark: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val commonUiCopy = getCommonGlobalUiCopy(language)
    if (!active) return

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
    } else {
        Color.White.copy(alpha = 0.82f)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val iconTint = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFF171717)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = visuals.footerContentAlpha
            }
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        IconButton(
            onClick = onDismissRequest,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = commonUiCopy.close,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

internal suspend fun captureHomeCardLongPressBackdropBitmap(
    activity: Activity?,
    rootView: View,
    fallbackBitmap: Bitmap? = null
): Bitmap? {
    val fallback = fallbackBitmap ?: captureHomeCardLongPressBackdropBitmapFallback(rootView)
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return fallback
    }
    val width = rootView.width
    val height = rootView.height
    if (width <= 0 || height <= 0) return fallback
    val targetWidth = max(1, width / HOME_CARD_HOVER_SNAPSHOT_DOWNSAMPLE)
    val targetHeight = max(1, height / HOME_CARD_HOVER_SNAPSHOT_DOWNSAMPLE)
    return withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val location = IntArray(2)
            rootView.getLocationInWindow(location)
            val requestRect = AndroidRect(
                location[0],
                location[1],
                location[0] + width,
                location[1] + height
            )
            PixelCopy.request(
                activity.window,
                requestRect,
                bitmap,
                { result ->
                    continuation.resume(
                        if (result == PixelCopy.SUCCESS) {
                            bitmap
                        } else {
                            fallback
                        }
                    )
                },
                Handler(Looper.getMainLooper())
            )
        }
    }
}

@Composable
private fun HomeCardLongPressHoverQuoteOverlay(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    quote: HomeCardHoverQuotePresentation?,
    isDark: Boolean,
    canRefreshQuote: Boolean,
    onQuoteRefreshRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!active || quote == null) return
    val language = LocalAppLanguage.current
    val commonUiCopy = getCommonGlobalUiCopy(language)

    val panelShape = RoundedCornerShape(22.dp)
    val backgroundColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.70f)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val primaryTextColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFF1C1C1C)
    }
    val secondaryTextColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    } else {
        Color(0xFF555555)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 38.dp)
                .widthIn(max = 380.dp)
                .clip(panelShape)
                .background(backgroundColor)
                .border(1.dp, borderColor, panelShape)
                .pointerInput(active) {
                    if (!active) return@pointerInput
                    detectTapGestures(onTap = {})
                }
                .graphicsLayer {
                    alpha = visuals.footerContentAlpha
                }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            if (quote.timeLabel.isNotBlank()) {
                Text(
                    text = quote.timeLabel,
                    color = secondaryTextColor,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = quote.quote,
                    modifier = Modifier.weight(1f),
                    color = primaryTextColor,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (canRefreshQuote) {
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = onQuoteRefreshRequest,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDark) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                } else {
                                    Color.Black.copy(alpha = 0.04f)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = commonUiCopy.refreshQuote,
                            tint = secondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeCardLongPressHoverImageOverlay(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    imageRequest: ImageRequest,
    imageBoundsInRoot: Rect?,
    windowSizePx: IntSize,
    overlayContent: @Composable BoxScope.() -> Unit = {},
    onImageReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bounds = imageBoundsInRoot
    if (bounds == null || bounds.width <= 1f || bounds.height <= 1f) return
    if (!active) return

    val density = LocalDensity.current
    val liftPx = with(density) { visuals.imageLift.toPx() }
    val overlayWidth = with(density) { bounds.width.toDp() }
    val overlayHeight = with(density) { bounds.height.toDp() }
    val overlayShape = RoundedCornerShape(24.dp)
    val safeWindowWidth = windowSizePx.width.coerceAtLeast(1).toFloat()
    val safeWindowHeight = windowSizePx.height.coerceAtLeast(1).toFloat()
    val normalizedCenterX = (((bounds.left + bounds.right) / 2f) / safeWindowWidth - 0.5f) * 2f
    val normalizedCenterY = (((bounds.top + bounds.bottom) / 2f) / safeWindowHeight - 0.5f) * 2f
    val lowCornerOnRight = normalizedCenterX <= 0f
    val diagonalBias = if (lowCornerOnRight) 1f else -1f
    val rotationYTarget = (
        (-normalizedCenterX * HOME_CARD_HOVER_IMAGE_TILT_MAX_Y) +
            (diagonalBias * HOME_CARD_HOVER_IMAGE_BASE_TILT_Y)
        ).coerceIn(-HOME_CARD_HOVER_IMAGE_TILT_MAX_Y, HOME_CARD_HOVER_IMAGE_TILT_MAX_Y)
    val rotationXTarget = (
        (normalizedCenterY * HOME_CARD_HOVER_IMAGE_TILT_MAX_X) +
            HOME_CARD_HOVER_IMAGE_BASE_TILT_X
        ).coerceIn(-HOME_CARD_HOVER_IMAGE_TILT_MAX_X, HOME_CARD_HOVER_IMAGE_TILT_MAX_X)
    val rotationZTarget = (
        (normalizedCenterX * -HOME_CARD_HOVER_IMAGE_TILT_MAX_Z) +
            (normalizedCenterY * 1.2f) +
            (diagonalBias * -HOME_CARD_HOVER_IMAGE_BASE_TILT_Z)
        ).coerceIn(-HOME_CARD_HOVER_IMAGE_TILT_MAX_Z, HOME_CARD_HOVER_IMAGE_TILT_MAX_Z)
    val tiltProgress = visuals.imageTiltProgress
    val rotationY = rotationYTarget * tiltProgress
    val rotationX = rotationXTarget * tiltProgress
    val rotationZ = rotationZTarget * tiltProgress
    val transformOrigin = TransformOrigin(
        pivotFractionX = if (lowCornerOnRight) 0.82f else 0.18f,
        pivotFractionY = 0.88f
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = bounds.left.roundToInt(),
                        y = (bounds.top - liftPx).roundToInt()
                    )
                }
                .size(width = overlayWidth, height = overlayHeight)
                .graphicsLayer {
                    alpha = visuals.imageOverlayAlpha
                    scaleX = visuals.imageScale
                    scaleY = visuals.imageScale
                    this.rotationX = rotationX
                    this.rotationY = rotationY
                    this.rotationZ = rotationZ
                    shadowElevation = with(density) { visuals.imageShadowElevation.toPx() }
                    ambientShadowColor = Color.Black.copy(alpha = 0.34f)
                    spotShadowColor = Color.Black.copy(alpha = 0.52f)
                    cameraDistance = HOME_CARD_HOVER_IMAGE_CAMERA_DISTANCE * density.density
                    shape = overlayShape
                    clip = false
                    this.transformOrigin = transformOrigin
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .pointerInput(active) {
                    if (!active) return@pointerInput
                    detectTapGestures(
                        onTap = {}
                    )
                }
                .background(
                    color = Color.White.copy(alpha = visuals.imageBorderAlpha * HOME_CARD_HOVER_IMAGE_SHELL_ALPHA),
                    shape = overlayShape
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(HOME_CARD_HOVER_IMAGE_EDGE_INSET)
                    .graphicsLayer {
                        shape = overlayShape
                        clip = true
                    }
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    alignment = Alignment.Center,
                    modifier = Modifier.matchParentSize(),
                    onSuccess = { onImageReady() }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = visuals.imageBorderAlpha * 0.34f),
                                0.12f to Color.Transparent,
                                0.72f to Color.Transparent,
                                1f to Color.Black.copy(alpha = visuals.imageBorderAlpha * 0.16f)
                            )
                        )
                )
                overlayContent()
            }
        }
    }
}

private fun hasExceededHoverTouchSlop(
    downX: Float,
    downY: Float,
    currentX: Float,
    currentY: Float,
    touchSlop: Float
): Boolean {
    return hypot((currentX - downX).toDouble(), (currentY - downY).toDouble()) > touchSlop
}
