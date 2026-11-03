package com.voicelike.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
@Composable
fun FillLightPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findFillLightActivity() }
    var hue by rememberSaveable { mutableFloatStateOf(34f) }
    var saturation by rememberSaveable { mutableFloatStateOf(0.18f) }
    var screenBrightness by rememberSaveable { mutableFloatStateOf(1f) }
    var controlsCollapsed by rememberSaveable { mutableStateOf(false) }
    val lightColor = Color.hsv(hue, saturation, 1f)

    DisposableEffect(activity) {
        val window = activity?.window
        val originalAttributes = window?.attributes
        val originalBrightness = originalAttributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        val originalColorMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            originalAttributes?.colorMode ?: ActivityInfo.COLOR_MODE_DEFAULT
        } else {
            ActivityInfo.COLOR_MODE_DEFAULT
        }
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.let {
                val attrs = it.attributes
                attrs.screenBrightness = originalBrightness
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    attrs.colorMode = originalColorMode
                }
                it.attributes = attrs
            }
        }
    }

    SideEffect {
        applyFillLightWindowBrightness(activity, screenBrightness)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(lightColor)
            .pointerInput(Unit) {
                detectTapGestures {
                    controlsCollapsed = !controlsCollapsed
                }
            }
    ) {
        AnimatedVisibility(
            visible = !controlsCollapsed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 14.dp, top = 10.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.16f))
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "\u5173\u95ed", tint = Color.White)
            }
        }

        AnimatedVisibility(
            visible = controlsCollapsed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(18.dp)
        ) {
            IconButton(
                onClick = { controlsCollapsed = false },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.18f))
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = "\u5c55\u5f00\u8bbe\u7f6e", tint = Color.White)
            }
        }

        AnimatedVisibility(
            visible = !controlsCollapsed,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FillLightControlPanel(
                hue = hue,
                saturation = saturation,
                brightness = screenBrightness,
                onColorChange = { nextHue, nextSaturation ->
                    hue = nextHue
                    saturation = nextSaturation
                },
                onBrightnessChange = { nextBrightness ->
                    val appliedBrightness = nextBrightness.coerceIn(FillLightMinBrightness, FillLightMaxBrightness)
                    screenBrightness = appliedBrightness
                    applyFillLightWindowBrightness(activity, appliedBrightness)
                },
                onCollapse = { controlsCollapsed = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun FillLightControlPanel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onColorChange: (Float, Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelShape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .clip(panelShape)
            .background(Color.Black.copy(alpha = 0.20f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), panelShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\u8272\u5f69",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "\u6298\u53e0",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapse
                    )
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        FillLightSpectrumPicker(
            hue = hue,
            saturation = saturation,
            onColorChange = onColorChange
        )
        FillLightPresets(onColorChange)
        FillLightSlider("\u4eae\u5ea6", brightness, FillLightMinBrightness..FillLightMaxBrightness, onBrightnessChange)
    }
}

@Composable
private fun FillLightSpectrumPicker(
    hue: Float,
    saturation: Float,
    onColorChange: (Float, Float) -> Unit
) {
    var pickerSize by remember { mutableStateOf(IntSize.Zero) }
    val shape = RoundedCornerShape(20.dp)
    val updateFromPosition: (Offset) -> Unit = { offset ->
        val width = pickerSize.width.coerceAtLeast(1)
        val height = pickerSize.height.coerceAtLeast(1)
        val nextHue = (offset.x / width.toFloat()).coerceIn(0f, 1f) * 360f
        val nextSaturation = (1f - (offset.y / height.toFloat()).coerceIn(0f, 1f))
            .coerceIn(0f, 1f)
        onColorChange(nextHue, nextSaturation)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .onSizeChanged { pickerSize = it }
            .pointerInput(pickerSize) {
                detectTapGestures { updateFromPosition(it) }
            }
            .pointerInput(pickerSize) {
                detectDragGestures(
                    onDragStart = updateFromPosition,
                    onDrag = { change, _ -> updateFromPosition(change.position) }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSpectrum()
            val x = size.width * (hue / 360f).coerceIn(0f, 1f)
            val y = size.height * (1f - saturation.coerceIn(0f, 1f))
            drawCircle(
                color = Color.Black.copy(alpha = 0.38f),
                radius = 10.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.hsv(hue, saturation, 1f),
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawSpectrum() {
    val hueStops = (0..12).map { index ->
        val fraction = index / 12f
        fraction to Color.hsv(360f * fraction, 1f, 1f)
    }.toTypedArray()
    drawRect(
        brush = Brush.horizontalGradient(*hueStops),
        size = size
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0f),
            1f to Color.White
        ),
        size = size
    )
}

@Composable
private fun FillLightPresets(
    onColorChange: (Float, Float) -> Unit
) {
    val presets = listOf(
        34f to 0.18f,
        50f to 0.08f,
        8f to 0.32f,
        210f to 0.28f,
        285f to 0.22f,
        140f to 0.18f
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        presets.forEach { (presetHue, presetSaturation) ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(presetHue, presetSaturation, 1f))
                    .border(1.dp, Color.White.copy(alpha = 0.70f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onColorChange(presetHue, presetSaturation)
                    }
            )
        }
    }
}

@Composable
private fun FillLightSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
            )
        )
    }
}

private const val FillLightMinBrightness = 0.05f
private const val FillLightMaxBrightness = 1f

private fun applyFillLightWindowBrightness(activity: Activity?, brightness: Float) {
    val window = activity?.window ?: return
    val attrs = window.attributes
    attrs.screenBrightness = brightness.coerceIn(FillLightMinBrightness, FillLightMaxBrightness)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        attrs.colorMode = ActivityInfo.COLOR_MODE_HDR
    }
    window.attributes = attrs
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

private tailrec fun Context.findFillLightActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findFillLightActivity()
    else -> null
}
