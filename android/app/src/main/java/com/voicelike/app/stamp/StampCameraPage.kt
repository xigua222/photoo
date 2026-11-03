package com.voicelike.app.stamp

import androidx.compose.runtime.CompositionLocalProvider
import com.voicelike.app.liquidglass.rememberLiquidGlassBackdrop
import com.voicelike.app.liquidglass.liquidGlassPanel
import com.voicelike.app.liquidglass.liquidGlassControl
import com.voicelike.app.liquidglass.LocalLiquidGlassBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import android.util.Rational
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.voicelike.app.AppLanguage
import com.voicelike.app.AppRuntimeLogStore
import com.voicelike.app.LocalAppLanguage
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.LocalHapticsEnabled
import com.voicelike.app.PreferencesManager
import com.voicelike.app.R
import com.voicelike.app.flipColorCardCameraBitmapHorizontally
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.roundToInt
import android.graphics.RectF
import android.util.Size as AndroidSize

private data class StampCameraCopy(
    val pageTitle: String,
    val pageSubtitle: String,
    val importPhoto: String,
    val importPlacementHint: String,
    val switchCamera: String,
    val save: String,
    val cancel: String,
    val saveToPhoneAlbum: String,
    val permissionTitle: String,
    val permissionBody: String,
    val permissionAction: String,
    val captureFailed: String,
    val renderFailed: String,
    val saveFailed: String,
    val loadFailed: String,
    val savedToCalendar: String,
    val savedToCalendarAndAlbum: String,
    val savedToCalendarAlbumFailed: String
)

private const val STAMP_PUNCH_OVERLAY_BASE_WIDTH_FRACTION = 1.08f
private const val STAMP_PUNCH_OVERLAY_MAX_WIDTH_FRACTION = 1.34f
private const val STAMP_PUNCH_OVERLAY_SE_MAX_WIDTH_FRACTION = 1.74f
private const val STAMP_PUNCH_OVERLAY_SE_CENTER_COMPENSATION_SCALE = 0.82f
private const val STAMP_PUNCH_LIFTED_STAMP_VISUAL_SCALE = 1.045f
private const val STAMP_PREVIEW_PHOTO_BLUR_ALPHA = 0.22f
private const val STAMP_PREVIEW_PAPER_ALPHA = 0.92f
private const val STAMP_PREVIEW_GRID_ALPHA = 0.96f
private const val STAMP_ORIENTATION_HYSTERESIS_DEGREES = 10
private const val STAMP_PREVIEW_LANDSCAPE_ASPECT_THRESHOLD = 1.07f
private const val STAMP_CAPTURE_PROCESSING_MAX_SIDE = 4096
private const val STAMP_FRAME_CUTOUT_CAPTURE_PROCESSING_MAX_SIDE = 1600
private const val STAMP_PREVIEW_MAX_LONG_SIDE = 2400
private const val STAMP_CAMERA_STARTUP_ENTRY_PROGRESS_THRESHOLD = 0.84f
private const val STAMP_AUTO_CUTOUT_VIEWPORT_MAX_LONG_SIDE = 1280
private const val STAMP_FRAME_CUTOUT_SCAN_MIN_DURATION_MS = 180L
private const val STAMP_CAMERA_PREFS_NAME = "stamp_camera_prefs"
private const val STAMP_CAMERA_PREF_USE_FRONT_CAMERA = "use_front_camera"
private const val STAMP_CAMERA_PREF_PUNCH_STYLE_KEY = "punch_style_key"
private val STAMP_PUNCH_OVERLAY_CENTER_Y_OFFSET = (-56).dp
private val STAMP_PUNCH_OVERLAY_SQUARE_EXTRA_DOWN_OFFSET = 10.dp
private val STAMP_PUNCH_OVERLAY_SE_UPWARD_BIAS = (-34).dp
private const val STAMP_STYLE_SELECTOR_WIDTH_FRACTION = 0.48f
private const val STAMP_PUNCH_RESULT_READY_GRACE_MS = 260L
private const val STAMP_PUNCH_CUT_TOTAL_DURATION_SLOW_MS = 1360L
private val stampSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private data class StampPunchCutoutEffectState(
    val token: Long,
    val fallbackBackdropBitmap: Bitmap,
    val holeRect: RectF,
    val holeMaskBitmap: Bitmap? = null,
    val targetPreviewWidthFraction: Float,
    val targetPreviewCenterYOffsetPx: Float,
    val punchedBackdropBitmap: Bitmap? = null,
    val liftedStampBitmap: Bitmap? = null
)

private data class StampPunchCutFrames(
    val punchedBackdrop: Bitmap,
    val liftedStamp: Bitmap
)

private data class FrameCutoutGuideGeometry(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val strokeWidth: Float,
    val cornerRadius: Float,
    val cornerLength: Float
) {
    val width: Float
        get() = (right - left).coerceAtLeast(1f)
    val height: Float
        get() = (bottom - top).coerceAtLeast(1f)
}

private data class StampImportBackdropSnapshot(
    val backdropBitmap: Bitmap,
    val imageRect: RectF
)

private data class StampCapturedSource(
    val processingBitmap: Bitmap,
    val sourceFile: File
)

private data class StampSourceContextDraft(
    val sourceBitmap: Bitmap,
    val sourceViewport: RectF,
    val recycleOnClear: Boolean = false,
    val sourceFile: File? = null,
    val deleteSourceFileOnClear: Boolean = false
)

private data class StampCaptureRenderResult(
    val renderedStamp: Bitmap?,
    val shouldLandscapeUi: Boolean,
    val sourceBitmap: Bitmap,
    val sourceViewport: RectF,
    val recycleSourceOnClear: Boolean,
    val sourceFile: File?,
    val deleteSourceFileOnClear: Boolean
)

@Composable
fun StampCameraPage(
    onBack: () -> Unit,
    onStampSaveStarted: ((Bitmap) -> String?)? = null,
    onStampSaveFailed: ((String?) -> Unit)? = null,
    onStampSaved: ((android.net.Uri, Bitmap, String?) -> Unit)? = null,
    entryProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appLanguage = LocalAppLanguage.current
    val appStrings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val copy = remember(appLanguage) { getStampCameraCopy(appLanguage) }
    val configuration = LocalConfiguration.current
    val isConfigurationLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val deviceRotation = rememberStampCameraDeviceRotation(context)
    val scope = rememberCoroutineScope()
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val startupProgressClamped = entryProgress.coerceIn(0f, 1f)
    val cameraStartupReady = startupProgressClamped >= STAMP_CAMERA_STARTUP_ENTRY_PROGRESS_THRESHOLD
    val cameraProvider by produceState<ProcessCameraProvider?>(
        initialValue = null,
        key1 = context,
        key2 = cameraStartupReady
    ) {
        value = if (cameraStartupReady) {
            awaitCameraProvider(context)
        } else {
            null
        }
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by rememberSaveable {
        mutableStateOf(loadPreferredStampCameraLens(context))
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var previewViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var isPunching by remember { mutableStateOf(false) }
    var isSavingResult by remember { mutableStateOf(false) }
    var stampResultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var stampResultDisplayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var presentedStampBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var stampResultBackdropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var presentedBackdropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var punchCutoutEffectState by remember { mutableStateOf<StampPunchCutoutEffectState?>(null) }
    var punchCutoutEffectToken by remember { mutableStateOf(0L) }
    var punchPressActive by remember { mutableStateOf(false) }
    var resultLandscapeUiMode by remember { mutableStateOf(false) }
    var pendingSourceContextDraft by remember { mutableStateOf<StampSourceContextDraft?>(null) }
    var importSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var importPunchOffset by remember { mutableStateOf(Offset.Zero) }
    var resultStampDisplayWidthFraction by remember { mutableStateOf<Float?>(null) }
    var preparedStampBytesDeferred by remember { mutableStateOf<Deferred<ByteArray?>?>(null) }
    var resultReadyForConfirm by remember { mutableStateOf(true) }
    val prefsManager = remember(context) { PreferencesManager(context) }
    var saveToPhoneAlbum by rememberSaveable { mutableStateOf(prefsManager.stampSaveToGalleryEnabled) }
    fun performInteractionHaptic() {
        if (hapticsAllowed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val soundPool = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.media.SoundPool.Builder().setMaxStreams(2).build()
        } else {
            @Suppress("DEPRECATION")
            android.media.SoundPool(2, android.media.AudioManager.STREAM_MUSIC, 0)
        }
    }
    var punchSoundId by remember { mutableIntStateOf(0) }

    val styleOptions = remember {
        listOf(
            StampPunchStyle.Se,
            StampPunchStyle.One,
            StampPunchStyle.Two,
            StampPunchStyle.Three,
            StampPunchStyle.FrameCutout
        )
    }
    var selectedStyleKey by rememberSaveable {
        mutableStateOf(loadPreferredStampCameraPunchStyle(context, styleOptions))
    }
    val selectedStyle = remember(selectedStyleKey) {
        StampPunchStyle.fromKey(selectedStyleKey)
            .takeIf { it in styleOptions }
            ?: StampPunchStyle.One
    }
    val selectedTemplate = remember(context, selectedStyle) {
        loadStampPunchTemplate(context, selectedStyle)
    }
    val overlayWidthFraction = remember(selectedStyle) {
        resolveCameraOverlayWidthFraction(selectedStyle)
    }
    val overlayAspectRatio = remember(selectedTemplate, selectedStyle) {
        resolveOverlayDisplayAspectRatio(
            template = selectedTemplate,
            fallbackAspect = selectedStyle.aspectRatio
        )
    }
    val captureInFlight = isPunching || stampResultBitmap != null || stampResultDisplayBitmap != null
    val cameraReadyForCutoutWarmup = hasCameraPermission &&
        cameraStartupReady &&
        imageCapture != null &&
        previewView != null
    fun discardPendingSourceContextDraft() {
        pendingSourceContextDraft?.let { draft ->
            draft.takeIf { it.recycleOnClear }?.sourceBitmap?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            draft.takeIf { it.deleteSourceFileOnClear }?.sourceFile?.let { sourceFile ->
                runCatching {
                    if (sourceFile.exists() && sourceFile.isFile) {
                        sourceFile.delete()
                    }
                }
            }
        }
        pendingSourceContextDraft = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null || captureInFlight) return@rememberLauncherForActivityResult
        performInteractionHaptic()
        scope.launch {
            preparedStampBytesDeferred?.cancel()
            preparedStampBytesDeferred = null
            val bitmap = loadStampSourceBitmap(context, uri, maxSide = 2400)
            if (bitmap == null) {
                Toast.makeText(context, copy.loadFailed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            stampResultBitmap = null
            stampResultDisplayBitmap = null
            stampResultBackdropBitmap = null
            resultLandscapeUiMode = false
            resultStampDisplayWidthFraction = null
            resultReadyForConfirm = true
            discardPendingSourceContextDraft()
            importSourceBitmap = bitmap
            importPunchOffset = Offset.Zero
        }
    }

    val latestOnBack by rememberUpdatedState(onBack)
    DisposableEffect(soundPool) {
        onDispose {
            runCatching { soundPool.release() }
        }
    }
    DisposableEffect(Unit) {
        val loadedId = soundPool.load(context, R.raw.stamp_punch, 1)
        punchSoundId = loadedId
        onDispose { }
    }
    LaunchedEffect(stampResultDisplayBitmap) {
        if (stampResultDisplayBitmap != null) {
            presentedStampBitmap = stampResultDisplayBitmap
        } else {
            delay(220)
            if (stampResultDisplayBitmap == null) {
                presentedStampBitmap = null
            }
        }
    }
    LaunchedEffect(stampResultBackdropBitmap) {
        if (stampResultBackdropBitmap != null) {
            presentedBackdropBitmap = stampResultBackdropBitmap
        } else {
            delay(220)
            if (stampResultBackdropBitmap == null) {
                presentedBackdropBitmap = null
            }
        }
    }
    LaunchedEffect(selectedStyleKey, styleOptions) {
        if (styleOptions.none { it.key == selectedStyleKey }) {
            val fallbackKey = styleOptions.firstOrNull()?.key ?: StampPunchStyle.One.key
            selectedStyleKey = fallbackKey
            savePreferredStampCameraPunchStyle(context, fallbackKey)
        }
    }
    LaunchedEffect(selectedStyle, cameraReadyForCutoutWarmup) {
        if (selectedStyle.isFrameCutoutMode && cameraReadyForCutoutWarmup) {
            warmUpStampAutoCutoutModel(context)
        }
    }

    BackHandler(enabled = punchCutoutEffectState != null || stampResultBitmap != null || stampResultDisplayBitmap != null) {
        preparedStampBytesDeferred?.cancel()
        preparedStampBytesDeferred = null
        punchPressActive = false
        stampResultBitmap = null
        stampResultDisplayBitmap = null
        stampResultBackdropBitmap = null
        resultLandscapeUiMode = false
        resultStampDisplayWidthFraction = null
        resultReadyForConfirm = true
        punchCutoutEffectState = null
        punchCutoutEffectToken = 0L
        discardPendingSourceContextDraft()
    }
    BackHandler(enabled = importSourceBitmap != null && stampResultBitmap == null) {
        punchPressActive = false
        importSourceBitmap = null
        importPunchOffset = Offset.Zero
        stampResultDisplayBitmap = null
        resultLandscapeUiMode = false
        resultStampDisplayWidthFraction = null
        resultReadyForConfirm = true
        punchCutoutEffectState = null
        punchCutoutEffectToken = 0L
        discardPendingSourceContextDraft()
    }
    BackHandler(enabled = stampResultBitmap == null && importSourceBitmap == null) {
        latestOnBack()
    }

    DisposableEffect(
        cameraProvider,
        previewView,
        previewViewportSize,
        lifecycleOwner,
        hasCameraPermission,
        useFrontCamera,
        selectedStyle,
        cameraStartupReady
    ) {
        val provider = cameraProvider
        val previewHost = previewView
        if (
            !cameraStartupReady ||
            provider == null ||
            previewHost == null ||
            !hasCameraPermission ||
            previewViewportSize.width <= 0 ||
            previewViewportSize.height <= 0
        ) {
            imageCapture = null
            boundCamera = null
            onDispose {
                imageCapture = null
                boundCamera = null
            }
        } else {
            val targetRotation = previewHost.display?.rotation ?: Surface.ROTATION_0
            val previewTargetSize = buildStampCameraPreviewTargetSize(previewViewportSize)
            val frameCaptureMode = selectedStyle.isFrameCutoutMode
            val selectorCandidates = buildStampCameraSelectorCandidates(
                provider = provider,
                preferFrontCamera = useFrontCamera
            )
            val primarySelection = selectorCandidates.firstOrNull()
            if (primarySelection == null) {
                imageCapture = null
                Toast.makeText(context, copy.captureFailed, Toast.LENGTH_SHORT).show()
            } else {
                fun bindStampUseCases(
                    selection: StampCameraSelectorCandidate,
                    strictResolutionSelector: Boolean,
                    captureMode: Int
                ): ImageCapture {
                    val previewBuilder = Preview.Builder()
                        .setTargetRotation(targetRotation)
                    if (strictResolutionSelector) {
                        previewBuilder.setResolutionSelector(
                            buildStampCameraPreviewResolutionSelector(previewTargetSize)
                        )
                    }
                    val preview = previewBuilder
                        .build()
                        .also { useCase -> useCase.setSurfaceProvider(previewHost.surfaceProvider) }
                    val captureBuilder = ImageCapture.Builder()
                        .setCaptureMode(captureMode)
                        .setTargetRotation(targetRotation)
                    if (strictResolutionSelector) {
                        captureBuilder.setResolutionSelector(
                            buildStampCameraCaptureResolutionSelector(
                                frameCutoutMode = frameCaptureMode
                            )
                        )
                    }
                    val capture = captureBuilder.build()
                    val viewPort = ViewPort.Builder(
                        Rational(
                            previewViewportSize.width.coerceAtLeast(1),
                            previewViewportSize.height.coerceAtLeast(1)
                        ),
                        targetRotation
                    )
                        .setScaleType(ViewPort.FILL_CENTER)
                        .build()
                    val useCaseGroup = UseCaseGroup.Builder()
                        .addUseCase(preview)
                        .addUseCase(capture)
                        .setViewPort(viewPort)
                        .build()
                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selection.selector,
                        useCaseGroup
                    )
                    imageCapture = capture
                    boundCamera = camera
                    if (selection.usingFrontCamera != useFrontCamera) {
                        useFrontCamera = selection.usingFrontCamera
                    }
                    return capture
                }

                runCatching {
                    bindStampUseCases(
                        selection = primarySelection,
                        strictResolutionSelector = true,
                        captureMode = if (frameCaptureMode) {
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        } else {
                            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                        }
                    )
                }.recoverCatching { primaryError ->
                    var recoveredCapture: ImageCapture? = null
                    selectorCandidates.forEach { candidate ->
                        if (recoveredCapture != null) return@forEach
                        runCatching {
                            recoveredCapture = bindStampUseCases(
                                selection = candidate,
                                strictResolutionSelector = false,
                                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                        }
                    }
                    recoveredCapture ?: throw primaryError
                }.onFailure {
                    imageCapture = null
                    boundCamera = null
                    Toast.makeText(context, copy.captureFailed, Toast.LENGTH_SHORT).show()
                }
            }
            onDispose {
                imageCapture = null
                boundCamera = null
                runCatching { provider.unbindAll() }
            }
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090909))
    ) {
        val density = LocalDensity.current
        val overlayDisplayWidth = remember(maxWidth, overlayWidthFraction) {
            maxWidth * overlayWidthFraction
        }
        val overlayCenterYOffset = remember(
            selectedStyle,
            selectedTemplate,
            overlayDisplayWidth,
            overlayAspectRatio,
            maxWidth
        ) {
            val baseOffset = resolveCameraOverlayCenterYOffset(selectedStyle)
            if (selectedStyle != StampPunchStyle.Se || selectedTemplate == null) {
                baseOffset
            } else {
                val holeCenterFraction = resolveTemplateHoleCenterYFraction(selectedTemplate)
                val estimatedOverlayHeightDp = (
                    overlayDisplayWidth /
                        overlayAspectRatio.coerceAtLeast(0.2f)
                    )
                val compensation = (
                    (estimatedOverlayHeightDp * (0.5f - holeCenterFraction) *
                        STAMP_PUNCH_OVERLAY_SE_CENTER_COMPENSATION_SCALE) +
                        STAMP_PUNCH_OVERLAY_SE_UPWARD_BIAS
                    ).coerceIn((-220).dp, 220.dp)
                baseOffset + compensation
            }
        }
        val overlayCenterYOffsetPx = remember(density, overlayCenterYOffset) {
            with(density) { overlayCenterYOffset.toPx() }
        }
        val resultOverlayCenterYOffset = remember(selectedStyle) {
            resolveCameraOverlayCenterYOffset(selectedStyle)
        }
        val resultOverlayCenterYOffsetPx = remember(density, resultOverlayCenterYOffset) {
            with(density) { resultOverlayCenterYOffset.toPx() }
        }
        val cameraSessionReady = hasCameraPermission &&
            cameraStartupReady &&
            imageCapture != null &&
            previewView != null
        // The live viewfinder is recorded so the camera chrome above it can refract it.
        val cameraGlassBackdrop = rememberLiquidGlassBackdrop()
        if (hasCameraPermission && cameraStartupReady) {
            AndroidView(
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (cameraGlassBackdrop != null) Modifier.layerBackdrop(cameraGlassBackdrop) else Modifier)
                    .onSizeChanged { nextSize ->
                        if (nextSize.width > 0 && nextSize.height > 0 && nextSize != previewViewportSize) {
                            previewViewportSize = nextSize
                        }
                    },
                update = { view ->
                    previewView = view
                    view.setOnTouchListener { _, _ -> true }
                    view.isClickable = false
                    view.isLongClickable = false
                    view.isFocusable = false
                    view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            )
        } else if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0C0C)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    strokeWidth = 2.6.dp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = copy.permissionTitle,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = copy.permissionBody,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        ) {
                            Text(
                                text = copy.permissionAction,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.62f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.74f)
                        )
                    )
                )
        )

        val interactionsBlocked = importSourceBitmap != null || stampResultBitmap != null
        val transitionOrPreviewActive = punchCutoutEffectState != null ||
            stampResultBitmap != null ||
            presentedStampBitmap != null
        val showPrimaryCameraChrome = importSourceBitmap == null && !transitionOrPreviewActive

        val overlayBitmap = selectedTemplate?.overlayBitmap?.takeUnless { it.isRecycled }
        val frameCutoutMode = selectedStyle.isFrameCutoutMode
        val punchOverlayAlpha by animateFloatAsState(
            targetValue = if (
                stampResultBitmap == null &&
                presentedStampBitmap == null &&
                importSourceBitmap == null
            ) 1f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "stamp_punch_overlay_alpha"
        )
        val punchPressScale by animateFloatAsState(
            targetValue = if (punchPressActive) 0.972f else 1f,
            animationSpec = if (punchPressActive) {
                tween(durationMillis = 95, easing = FastOutSlowInEasing)
            } else {
                spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessMedium
                )
            },
            label = "stamp_punch_press_scale"
        )
        if (hasCameraPermission && importSourceBitmap == null && (frameCutoutMode || overlayBitmap != null)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(if (punchCutoutEffectState != null) 3.1f else 2f)
                    .offset(y = overlayCenterYOffset)
                    .requiredWidth(overlayDisplayWidth)
                    .aspectRatio(overlayAspectRatio)
                    .graphicsLayer {
                        alpha = punchOverlayAlpha
                        scaleX = punchPressScale
                        scaleY = punchPressScale
                    }
                    .noRipplePressCapture(
                        enabled = !captureInFlight &&
                            cameraSessionReady &&
                            !interactionsBlocked,
                        onPressChanged = { pressed -> punchPressActive = pressed }
                    ) {
                        val activeCapture = imageCapture ?: run {
                            Toast.makeText(context, copy.captureFailed, Toast.LENGTH_SHORT).show()
                            return@noRipplePressCapture
                        }
                        val activeStyle = selectedStyle
                        val isFrameMode = activeStyle.isFrameCutoutMode
                        val template = if (isFrameMode) null else selectedTemplate
                        if (!isFrameMode && template == null) {
                            Toast.makeText(context, copy.renderFailed, Toast.LENGTH_SHORT).show()
                            return@noRipplePressCapture
                        }
                        if (!isFrameMode && prefsManager.soundEffectsEnabled && punchSoundId != 0) {
                            soundPool.play(punchSoundId, 1f, 1f, 0, 0, 1f)
                        }
                        preparedStampBytesDeferred?.cancel()
                        preparedStampBytesDeferred = null
                        isPunching = true
                        resultReadyForConfirm = false
                        stampResultBitmap = null
                        stampResultDisplayBitmap = null
                        stampResultBackdropBitmap = null
                        resultLandscapeUiMode = false
                        resultStampDisplayWidthFraction = null
                        punchCutoutEffectState = null
                        punchCutoutEffectToken = 0L
                        discardPendingSourceContextDraft()
                        scope.launch {
                            val capturedSource = withContext(Dispatchers.IO) {
                                captureStampSource(
                                    context = context,
                                    imageCapture = activeCapture,
                                    processingMaxSide = if (isFrameMode) {
                                        STAMP_FRAME_CUTOUT_CAPTURE_PROCESSING_MAX_SIDE
                                    } else {
                                        STAMP_CAPTURE_PROCESSING_MAX_SIDE
                                    }
                                )
                            }
                            if (capturedSource == null) {
                                isPunching = false
                                resultReadyForConfirm = true
                                punchCutoutEffectState = null
                                punchCutoutEffectToken = 0L
                                resultLandscapeUiMode = false
                                resultStampDisplayWidthFraction = null
                                Toast.makeText(context, copy.captureFailed, Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val capturedProcessingBitmap = withContext(Dispatchers.Default) {
                                if (useFrontCamera) {
                                    flipColorCardCameraBitmapHorizontally(capturedSource.processingBitmap)
                                } else {
                                    capturedSource.processingBitmap
                                }
                            }
                            if (capturedProcessingBitmap.isRecycled) {
                                runCatching {
                                    if (capturedSource.sourceFile.exists() && capturedSource.sourceFile.isFile) {
                                        capturedSource.sourceFile.delete()
                                    }
                                }
                                isPunching = false
                                resultReadyForConfirm = true
                                punchCutoutEffectState = null
                                punchCutoutEffectToken = 0L
                                resultLandscapeUiMode = false
                                resultStampDisplayWidthFraction = null
                                Toast.makeText(context, copy.captureFailed, Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val previewSnapshot = withContext(Dispatchers.Default) {
                                buildPreviewSnapshotFromCapturedSource(
                                    capturedSourceBitmap = capturedProcessingBitmap,
                                    previewViewportSize = previewViewportSize
                                )
                            }
                            if (previewSnapshot == null) {
                                if (!capturedProcessingBitmap.isRecycled) {
                                    capturedProcessingBitmap.recycle()
                                }
                                runCatching {
                                    if (capturedSource.sourceFile.exists() && capturedSource.sourceFile.isFile) {
                                        capturedSource.sourceFile.delete()
                                    }
                                }
                                isPunching = false
                                resultReadyForConfirm = true
                                punchCutoutEffectState = null
                                punchCutoutEffectToken = 0L
                                resultLandscapeUiMode = false
                                resultStampDisplayWidthFraction = null
                                Toast.makeText(context, copy.captureFailed, Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val previewViewport = if (isFrameMode) {
                                buildPreviewFrameViewport(
                                    previewBitmap = previewSnapshot,
                                    overlayWidthFraction = overlayWidthFraction,
                                    overlayAspectRatio = overlayAspectRatio,
                                    overlayCenterYOffsetPx = overlayCenterYOffsetPx
                                )
                            } else {
                                buildPreviewHoleViewport(
                                    previewBitmap = previewSnapshot,
                                    template = requireNotNull(template),
                                    overlayWidthFraction = overlayWidthFraction,
                                    overlayAspectRatio = overlayAspectRatio,
                                    overlayCenterYOffsetPx = overlayCenterYOffsetPx
                                )
                            }
                            val capturedSourceViewport = mapPreviewViewportToCapturedSource(
                                capturedSourceBitmap = capturedProcessingBitmap,
                                previewBitmap = previewSnapshot,
                                previewViewport = previewViewport
                            )
                            val punchedDisplayWidthFraction = (
                                previewViewport.width() /
                                    previewSnapshot.width.coerceAtLeast(1).toFloat()
                                ).coerceIn(0.2f, 0.96f)

                            if (isFrameMode) {
                                val frameDisplayWidthFraction =
                                    resolveFrameCutoutResultDisplayWidthFraction(
                                        punchedDisplayWidthFraction
                                    )
                                resultStampDisplayWidthFraction = frameDisplayWidthFraction
                                val frameTransitionStartedAt = SystemClock.uptimeMillis()
                                val effectToken = SystemClock.elapsedRealtimeNanos()
                                punchCutoutEffectToken = effectToken
                                punchCutoutEffectState = StampPunchCutoutEffectState(
                                    token = effectToken,
                                    fallbackBackdropBitmap = previewSnapshot,
                                    holeRect = RectF(previewViewport),
                                    holeMaskBitmap = null,
                                    targetPreviewWidthFraction = frameDisplayWidthFraction,
                                    targetPreviewCenterYOffsetPx = resultOverlayCenterYOffsetPx
                                )
                                val finalCutoutStamp = buildAutoCutoutStampFromViewport(
                                    sourceBitmap = capturedProcessingBitmap,
                                    sourceViewport = capturedSourceViewport,
                                    context = context
                                )
                                if (finalCutoutStamp != null) {
                                    val finalDisplayStamp =
                                        buildFrameCutoutDisplayBitmap(
                                            context = context,
                                            cutoutBitmap = finalCutoutStamp
                                        )
                                            ?: finalCutoutStamp
                                    val elapsed = SystemClock.uptimeMillis() - frameTransitionStartedAt
                                    if (elapsed < STAMP_FRAME_CUTOUT_SCAN_MIN_DURATION_MS) {
                                        delay(STAMP_FRAME_CUTOUT_SCAN_MIN_DURATION_MS - elapsed)
                                    }
                                    if (
                                        punchCutoutEffectToken == effectToken &&
                                        punchCutoutEffectState?.token == effectToken
                                    ) {
                                        punchCutoutEffectState = punchCutoutEffectState?.copy(
                                            liftedStampBitmap = finalDisplayStamp
                                        )
                                    }
                                    isPunching = false
                                    stampResultBackdropBitmap = previewSnapshot
                                    stampResultDisplayBitmap = finalDisplayStamp
                                    stampResultBitmap = finalCutoutStamp
                                    resultLandscapeUiMode = false
                                    pendingSourceContextDraft = StampSourceContextDraft(
                                        sourceBitmap = capturedProcessingBitmap,
                                        sourceViewport = RectF(capturedSourceViewport),
                                        recycleOnClear = true,
                                        sourceFile = capturedSource.sourceFile,
                                        deleteSourceFileOnClear = true
                                    )
                                    preparedStampBytesDeferred = stampSaveScope.async {
                                        encodeStampBitmapToPngBytes(finalCutoutStamp)
                                    }
                                    resultReadyForConfirm = true
                                    if (punchCutoutEffectToken == effectToken) {
                                        launch {
                                            delay(320)
                                            if (punchCutoutEffectToken == effectToken) {
                                                punchCutoutEffectState = null
                                                punchCutoutEffectToken = 0L
                                            }
                                        }
                                    }
                                    return@launch
                                }
                                if (!capturedProcessingBitmap.isRecycled) {
                                    capturedProcessingBitmap.recycle()
                                }
                                runCatching {
                                    if (capturedSource.sourceFile.exists() && capturedSource.sourceFile.isFile) {
                                        capturedSource.sourceFile.delete()
                                    }
                                }
                                isPunching = false
                                punchCutoutEffectState = null
                                punchCutoutEffectToken = 0L
                                stampResultBackdropBitmap = null
                                stampResultBitmap = null
                                stampResultDisplayBitmap = null
                                resultLandscapeUiMode = false
                                resultStampDisplayWidthFraction = null
                                resultReadyForConfirm = true
                                Toast.makeText(context, copy.renderFailed, Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            resultStampDisplayWidthFraction = punchedDisplayWidthFraction
                            val punchStartedAt = SystemClock.uptimeMillis()
                            val effectToken = SystemClock.elapsedRealtimeNanos()
                            punchCutoutEffectToken = effectToken
                            punchCutoutEffectState = StampPunchCutoutEffectState(
                                token = effectToken,
                                fallbackBackdropBitmap = previewSnapshot,
                                holeRect = RectF(previewViewport),
                                holeMaskBitmap = extractTemplateHoleMaskBitmap(requireNotNull(template)),
                                targetPreviewWidthFraction = punchedDisplayWidthFraction,
                                targetPreviewCenterYOffsetPx = resultOverlayCenterYOffsetPx
                            )
                            val previewCutFramesDeferred = async(Dispatchers.Default) {
                                buildStampPunchCutFrames(
                                    sourceBitmap = previewSnapshot,
                                    template = requireNotNull(template),
                                    sourceViewport = previewViewport
                                )
                            }
                            val finalStampFramesDeferred = async(Dispatchers.Default) {
                                buildStampPunchCutFrames(
                                    sourceBitmap = capturedProcessingBitmap,
                                    template = requireNotNull(template),
                                    sourceViewport = capturedSourceViewport
                                )
                            }
                            launch {
                                val cutFrames = runCatching { previewCutFramesDeferred.await() }.getOrNull()
                                if (
                                    cutFrames != null &&
                                    punchCutoutEffectToken == effectToken &&
                                    punchCutoutEffectState?.token == effectToken
                                ) {
                                    punchCutoutEffectState = punchCutoutEffectState?.copy(
                                        punchedBackdropBitmap = cutFrames.punchedBackdrop,
                                        liftedStampBitmap = cutFrames.liftedStamp
                                    )
                                }
                            }
                            val elapsed = SystemClock.uptimeMillis() - punchStartedAt
                            if (elapsed < STAMP_PUNCH_CUT_TOTAL_DURATION_SLOW_MS) {
                                delay(STAMP_PUNCH_CUT_TOTAL_DURATION_SLOW_MS - elapsed)
                            }
                            val previewCutFrames = runCatching { previewCutFramesDeferred.await() }.getOrNull()
                            val finalStampFrames = runCatching { finalStampFramesDeferred.await() }.getOrNull()
                            finalStampFrames?.punchedBackdrop
                                ?.takeUnless { it.isRecycled }
                                ?.recycle()
                            val finalLiftedStamp = finalStampFrames?.liftedStamp?.takeUnless { it.isRecycled }
                            if (finalLiftedStamp != null) {
                                isPunching = false
                                stampResultBackdropBitmap = previewCutFrames?.punchedBackdrop
                                    ?.takeUnless { it.isRecycled }
                                    ?: previewSnapshot
                                stampResultDisplayBitmap = finalLiftedStamp
                                stampResultBitmap = finalLiftedStamp
                                resultLandscapeUiMode = false
                                pendingSourceContextDraft = StampSourceContextDraft(
                                    sourceBitmap = capturedProcessingBitmap,
                                    sourceViewport = RectF(capturedSourceViewport),
                                    recycleOnClear = true,
                                    sourceFile = capturedSource.sourceFile,
                                    deleteSourceFileOnClear = true
                                )
                                preparedStampBytesDeferred = stampSaveScope.async {
                                    encodeStampBitmapToPngBytes(finalLiftedStamp)
                                }
                                resultReadyForConfirm = true
                                if (punchCutoutEffectToken == effectToken) {
                                    launch {
                                        delay(320)
                                        if (punchCutoutEffectToken == effectToken) {
                                            punchCutoutEffectState = null
                                            punchCutoutEffectToken = 0L
                                        }
                                    }
                                }
                                return@launch
                            }
                            if (!capturedProcessingBitmap.isRecycled) {
                                capturedProcessingBitmap.recycle()
                            }
                            runCatching {
                                if (capturedSource.sourceFile.exists() && capturedSource.sourceFile.isFile) {
                                    capturedSource.sourceFile.delete()
                                }
                            }
                            isPunching = false
                            punchCutoutEffectState = null
                            punchCutoutEffectToken = 0L
                            stampResultBackdropBitmap = null
                            stampResultBitmap = null
                            stampResultDisplayBitmap = null
                            resultLandscapeUiMode = false
                            resultStampDisplayWidthFraction = null
                            resultReadyForConfirm = true
                            Toast.makeText(context, copy.renderFailed, Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                if (frameCutoutMode) {
                    FrameCutoutGuideOverlay(
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White.copy(alpha = 0.95f)
                    )
                } else {
                    val bmp = overlayBitmap?.takeUnless { it.isRecycled }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (showPrimaryCameraChrome) {
            CompositionLocalProvider(LocalLiquidGlassBackdrop provides cameraGlassBackdrop) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(4f)
                        .statusBarsPadding()
                        .noRippleClickable(onClick = {})
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CameraChromeIconButton(
                        onClick = latestOnBack,
                        enabled = !isPunching
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = appStrings.back,
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = copy.pageTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = copy.pageSubtitle,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(4f)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .noRippleClickable(onClick = {})
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StampStyleSelector(
                        styles = styleOptions,
                        selectedStyle = selectedStyle,
                        enabled = !captureInFlight,
                        onStyleSelected = { style ->
                            if (selectedStyleKey != style.key) {
                                performInteractionHaptic()
                                selectedStyleKey = style.key
                                savePreferredStampCameraPunchStyle(context, style.key)
                            }
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 0.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        CameraUtilityButton(
                            enabled = !captureInFlight && !interactionsBlocked,
                            onClick = {
                                performInteractionHaptic()
                                imagePicker.launch("image/*")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = copy.importPhoto,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(80.dp))
                        CameraUtilityButton(
                            enabled = !captureInFlight && cameraSessionReady && !interactionsBlocked,
                            onClick = {
                                performInteractionHaptic()
                                useFrontCamera = (!useFrontCamera).also { preferred ->
                                    savePreferredStampCameraLens(context, preferred)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cameraswitch,
                                contentDescription = copy.switchCamera,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        importSourceBitmap?.let { sourceBitmap ->
            StampImportPlacementOverlay(
                sourceBitmap = sourceBitmap,
                styles = styleOptions,
                style = selectedStyle,
                template = selectedTemplate,
                overlayBitmap = overlayBitmap,
                punchOffset = importPunchOffset,
                isPunching = isPunching,
                copy = copy,
                onPunchOffsetChange = { importPunchOffset = it },
                onStyleSelected = { nextStyle ->
                    if (selectedStyleKey != nextStyle.key) {
                        performInteractionHaptic()
                        selectedStyleKey = nextStyle.key
                        savePreferredStampCameraPunchStyle(context, nextStyle.key)
                    }
                },
                onCancel = {
                    punchPressActive = false
                    importSourceBitmap = null
                    importPunchOffset = Offset.Zero
                    stampResultDisplayBitmap = null
                    resultLandscapeUiMode = false
                    resultStampDisplayWidthFraction = null
                    resultReadyForConfirm = true
                    discardPendingSourceContextDraft()
                },
                onPunch = { sourceViewport, importContainerSize, sourceDisplayRect ->
                    val activeStyle = selectedStyle
                    val isFrameMode = activeStyle.isFrameCutoutMode
                    val template = if (isFrameMode) null else selectedTemplate
                    if (!isFrameMode && template == null) {
                        Toast.makeText(context, copy.renderFailed, Toast.LENGTH_SHORT).show()
                        return@StampImportPlacementOverlay
                    }
                    if (!isFrameMode && prefsManager.soundEffectsEnabled && punchSoundId != 0) {
                        soundPool.play(punchSoundId, 1f, 1f, 0, 0, 1f)
                    }
                    preparedStampBytesDeferred?.cancel()
                    preparedStampBytesDeferred = null
                    punchPressActive = false
                    isPunching = true
                    resultReadyForConfirm = false
                    stampResultBitmap = null
                    stampResultDisplayBitmap = null
                    stampResultBackdropBitmap = null
                    resultLandscapeUiMode = false
                    resultStampDisplayWidthFraction = null
                    punchCutoutEffectState = null
                    punchCutoutEffectToken = 0L
                    discardPendingSourceContextDraft()
                    scope.launch {
                        val importBackdropSnapshot = withContext(Dispatchers.Default) {
                            buildImportBackdropSnapshot(
                                sourceBitmap = sourceBitmap,
                                containerSize = importContainerSize,
                                displayRect = sourceDisplayRect
                            )
                        }
                        if (importBackdropSnapshot == null) {
                            isPunching = false
                            resultReadyForConfirm = true
                            punchCutoutEffectState = null
                            punchCutoutEffectToken = 0L
                            stampResultBackdropBitmap = null
                            resultLandscapeUiMode = false
                            resultStampDisplayWidthFraction = null
                            Toast.makeText(context, copy.renderFailed, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val previewViewport = mapImportSourceViewportToBackdrop(
                            sourceViewport = sourceViewport,
                            sourceBitmap = sourceBitmap,
                            imageRect = importBackdropSnapshot.imageRect
                        )
                        val punchedDisplayWidthFraction = (
                            previewViewport.width() /
                                importBackdropSnapshot.backdropBitmap.width
                                    .coerceAtLeast(1).toFloat()
                            ).coerceIn(0.2f, 0.96f)

                        if (isFrameMode) {
                            val frameDisplayWidthFraction =
                                resolveFrameCutoutResultDisplayWidthFraction(
                                    punchedDisplayWidthFraction
                                )
                            resultStampDisplayWidthFraction = frameDisplayWidthFraction
                            val frameTransitionStartedAt = SystemClock.uptimeMillis()
                            val effectToken = SystemClock.elapsedRealtimeNanos()
                            punchCutoutEffectToken = effectToken
                            punchCutoutEffectState = StampPunchCutoutEffectState(
                                token = effectToken,
                                fallbackBackdropBitmap = importBackdropSnapshot.backdropBitmap,
                                holeRect = RectF(previewViewport),
                                holeMaskBitmap = null,
                                targetPreviewWidthFraction = frameDisplayWidthFraction,
                                targetPreviewCenterYOffsetPx = resultOverlayCenterYOffsetPx
                            )
                            val finalCutoutStamp = buildAutoCutoutStampFromViewport(
                                sourceBitmap = sourceBitmap,
                                sourceViewport = sourceViewport,
                                context = context
                            )
                            if (finalCutoutStamp != null) {
                                val finalDisplayStamp =
                                    buildFrameCutoutDisplayBitmap(
                                        context = context,
                                        cutoutBitmap = finalCutoutStamp
                                    )
                                        ?: finalCutoutStamp
                                val elapsed = SystemClock.uptimeMillis() - frameTransitionStartedAt
                                if (elapsed < STAMP_FRAME_CUTOUT_SCAN_MIN_DURATION_MS) {
                                    delay(STAMP_FRAME_CUTOUT_SCAN_MIN_DURATION_MS - elapsed)
                                }
                                if (
                                    punchCutoutEffectToken == effectToken &&
                                    punchCutoutEffectState?.token == effectToken
                                ) {
                                    punchCutoutEffectState = punchCutoutEffectState?.copy(
                                        liftedStampBitmap = finalDisplayStamp
                                    )
                                }
                                isPunching = false
                                stampResultBackdropBitmap = importBackdropSnapshot.backdropBitmap
                                stampResultDisplayBitmap = finalDisplayStamp
                                stampResultBitmap = finalCutoutStamp
                                resultLandscapeUiMode = false
                                pendingSourceContextDraft = StampSourceContextDraft(
                                    sourceBitmap = importBackdropSnapshot.backdropBitmap,
                                    sourceViewport = RectF(previewViewport),
                                    recycleOnClear = false
                                )
                                preparedStampBytesDeferred = stampSaveScope.async {
                                    encodeStampBitmapToPngBytes(finalCutoutStamp)
                                }
                                resultReadyForConfirm = true
                                if (punchCutoutEffectToken == effectToken) {
                                    launch {
                                        delay(320)
                                        if (punchCutoutEffectToken == effectToken) {
                                            punchCutoutEffectState = null
                                            punchCutoutEffectToken = 0L
                                        }
                                    }
                                }
                                return@launch
                            }
                            isPunching = false
                            punchCutoutEffectState = null
                            punchCutoutEffectToken = 0L
                            stampResultBackdropBitmap = null
                            stampResultBitmap = null
                            stampResultDisplayBitmap = null
                            resultLandscapeUiMode = false
                            resultStampDisplayWidthFraction = null
                            resultReadyForConfirm = true
                            Toast.makeText(context, copy.renderFailed, Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        resultStampDisplayWidthFraction = punchedDisplayWidthFraction
                        val punchStartedAt = SystemClock.uptimeMillis()
                        val effectToken = SystemClock.elapsedRealtimeNanos()
                        punchCutoutEffectToken = effectToken
                        punchCutoutEffectState = StampPunchCutoutEffectState(
                            token = effectToken,
                            fallbackBackdropBitmap = importBackdropSnapshot.backdropBitmap,
                            holeRect = RectF(previewViewport),
                            holeMaskBitmap = extractTemplateHoleMaskBitmap(requireNotNull(template)),
                            targetPreviewWidthFraction = punchedDisplayWidthFraction,
                            targetPreviewCenterYOffsetPx = resultOverlayCenterYOffsetPx
                        )
                        val cutFramesDeferred = async(Dispatchers.Default) {
                            buildStampPunchCutFrames(
                                sourceBitmap = importBackdropSnapshot.backdropBitmap,
                                template = requireNotNull(template),
                                sourceViewport = previewViewport
                            )
                        }
                        launch {
                            val cutFrames = runCatching { cutFramesDeferred.await() }.getOrNull()
                            if (
                                cutFrames != null &&
                                punchCutoutEffectToken == effectToken &&
                                punchCutoutEffectState?.token == effectToken
                            ) {
                                punchCutoutEffectState = punchCutoutEffectState?.copy(
                                    punchedBackdropBitmap = cutFrames.punchedBackdrop,
                                    liftedStampBitmap = cutFrames.liftedStamp
                                )
                            }
                        }
                        val elapsed = SystemClock.uptimeMillis() - punchStartedAt
                        if (elapsed < STAMP_PUNCH_CUT_TOTAL_DURATION_SLOW_MS) {
                            delay(STAMP_PUNCH_CUT_TOTAL_DURATION_SLOW_MS - elapsed)
                        }
                        val finalCutFrames = runCatching { cutFramesDeferred.await() }.getOrNull()
                        val finalLiftedStamp = finalCutFrames?.liftedStamp?.takeUnless { it.isRecycled }
                        if (finalLiftedStamp != null) {
                            isPunching = false
                            stampResultBackdropBitmap = finalCutFrames.punchedBackdrop
                                .takeUnless { it.isRecycled }
                                ?: importBackdropSnapshot.backdropBitmap
                            stampResultDisplayBitmap = finalLiftedStamp
                            stampResultBitmap = finalLiftedStamp
                            resultLandscapeUiMode = false
                            pendingSourceContextDraft = StampSourceContextDraft(
                                sourceBitmap = importBackdropSnapshot.backdropBitmap,
                                sourceViewport = RectF(previewViewport),
                                recycleOnClear = false
                            )
                            preparedStampBytesDeferred = stampSaveScope.async {
                                encodeStampBitmapToPngBytes(finalLiftedStamp)
                            }
                            resultReadyForConfirm = true
                            if (punchCutoutEffectToken == effectToken) {
                                launch {
                                    delay(320)
                                    if (punchCutoutEffectToken == effectToken) {
                                        punchCutoutEffectState = null
                                        punchCutoutEffectToken = 0L
                                    }
                                }
                            }
                            return@launch
                        }
                        isPunching = false
                        punchCutoutEffectState = null
                        punchCutoutEffectToken = 0L
                        stampResultBackdropBitmap = null
                        stampResultBitmap = null
                        stampResultDisplayBitmap = null
                        resultLandscapeUiMode = false
                        resultStampDisplayWidthFraction = null
                        resultReadyForConfirm = true
                        Toast.makeText(context, copy.renderFailed, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.zIndex(2.5f)
            )
        }

        if (punchCutoutEffectState != null || presentedStampBitmap != null) {
            AnimatedVisibility(
                visible = punchCutoutEffectState != null || stampResultBitmap != null,
                enter = fadeIn(tween(1)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.zIndex(3.3f)
            ) {
                StampResultOverlay(
                    stampBitmap = presentedStampBitmap,
                    backdropBitmap = presentedBackdropBitmap,
                    transitionState = punchCutoutEffectState,
                    style = selectedStyle,
                    landscapeMode = resultLandscapeUiMode,
                    overlayCenterYOffset = resultOverlayCenterYOffset,
                    stampDisplayWidthFraction = (
                        (
                            resultStampDisplayWidthFraction
                                ?: resolveStampDisplayWidthFraction(
                                    style = selectedStyle,
                                    template = selectedTemplate
                                )
                            ) * if (resultLandscapeUiMode && !selectedStyle.isFrameCutoutMode) 1.12f else 1f
                    ).coerceAtMost(
                        if (selectedStyle.isFrameCutoutMode) 0.78f else 0.92f
                    ),
                    isSaving = isSavingResult,
                    actionsEnabled = resultReadyForConfirm,
                    onConfirm = {
                        if (isSavingResult) {
                            return@StampResultOverlay
                        }
                        val sourceSnapshot = stampResultBitmap ?: return@StampResultOverlay
                        val snapshot = runCatching {
                            sourceSnapshot.copy(Bitmap.Config.ARGB_8888, false)
                        }.getOrDefault(sourceSnapshot)
                        if (snapshot.isRecycled) return@StampResultOverlay
                        val shouldSaveToGallery = saveToPhoneAlbum
                        val dayKey = stampCalendarTodayKey()
                        val selectedStyleSnapshot = selectedStyle
                        val appContext = context.applicationContext
                        val savedCallback = onStampSaved
                        val saveStartedCallback = onStampSaveStarted
                        val saveFailedCallback = onStampSaveFailed
                        val preparedBytesTask = preparedStampBytesDeferred
                        val sourceContextDraft = pendingSourceContextDraft?.let {
                            it.copy(sourceViewport = RectF(it.sourceViewport))
                        }
                        preparedStampBytesDeferred = null
                        pendingSourceContextDraft = null
                        isSavingResult = true
                        stampResultBitmap = null
                        stampResultDisplayBitmap = null
                        stampResultBackdropBitmap = null
                        resultLandscapeUiMode = false
                        resultStampDisplayWidthFraction = null
                        val pendingSaveKey = runCatching { saveStartedCallback?.invoke(snapshot) }.getOrNull()
                        stampSaveScope.launch {
                            try {
                                val encodedBytes = runCatching { preparedBytesTask?.await() }.getOrNull()
                                val localUri = if (encodedBytes != null) {
                                    saveStampBytesToLocalStore(appContext, encodedBytes)
                                } else {
                                    saveStampBitmapToLocalStore(appContext, snapshot)
                                }
                                if (localUri == null) {
                                    AppRuntimeLogStore.logError(
                                        appContext,
                                        "StampSave",
                                        "file save failed, style=${selectedStyleSnapshot.key}"
                                    )
                                    withContext(Dispatchers.Main) {
                                        isSavingResult = false
                                        saveFailedCallback?.invoke(pendingSaveKey)
                                        Toast.makeText(appContext, copy.renderFailed, Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }
                                val calendarPersisted = addStampToCalendarDay(
                                    context = appContext,
                                    dayKey = dayKey,
                                    uriString = localUri.toString(),
                                    punchStyleKey = selectedStyleSnapshot.key
                                )
                                if (!calendarPersisted) {
                                    AppRuntimeLogStore.logError(
                                        appContext,
                                        "StampSave",
                                        "calendar persist failed, dayKey=$dayKey"
                                    )
                                    withContext(Dispatchers.Main) {
                                        isSavingResult = false
                                        saveFailedCallback?.invoke(pendingSaveKey)
                                        Toast.makeText(
                                            appContext,
                                            copy.saveFailed,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    return@launch
                                }
                                AppRuntimeLogStore.logInfo(
                                    appContext,
                                    "StampSave",
                                    "persisted dayKey=$dayKey file=${localUri.lastPathSegment}"
                                )
                                if (selectedStyleSnapshot.isFrameCutoutMode) {
                                    val cutoutAssetPath = localUri.path.orEmpty()
                                    updateStampCalendarStampSkin(
                                        context = appContext,
                                        dayKey = dayKey,
                                        uriString = localUri.toString(),
                                        skin = StampCalendarStampSkin(
                                            style = StampCalendarSkinStyle.Cutout,
                                            cutoutAssetPath = cutoutAssetPath
                                        )
                                    )
                                    updateStampCalendarStampCutoutLock(
                                        context = appContext,
                                        dayKey = dayKey,
                                        uriString = localUri.toString(),
                                        locked = true
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    isSavingResult = false
                                    savedCallback?.invoke(localUri, snapshot, pendingSaveKey)
                                }
                                if (!shouldSaveToGallery) {
                                    return@launch
                                }
                                if (encodedBytes != null) {
                                    saveStampBytesToGallery(appContext, encodedBytes)
                                } else {
                                    saveStampFromLocalUriToGallery(appContext, localUri)
                                }
                            } finally {
                                sourceContextDraft?.takeIf { it.recycleOnClear }?.sourceBitmap?.let { bitmap ->
                                    if (!bitmap.isRecycled) {
                                        bitmap.recycle()
                                    }
                                }
                                sourceContextDraft?.takeIf { it.deleteSourceFileOnClear }?.sourceFile?.let { sourceFile ->
                                    runCatching {
                                        if (sourceFile.exists() && sourceFile.isFile) {
                                            sourceFile.delete()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onCancel = {
                        preparedStampBytesDeferred?.cancel()
                        preparedStampBytesDeferred = null
                        punchPressActive = false
                        stampResultBitmap = null
                        stampResultDisplayBitmap = null
                        stampResultBackdropBitmap = null
                        resultLandscapeUiMode = false
                        resultStampDisplayWidthFraction = null
                        resultReadyForConfirm = true
                        discardPendingSourceContextDraft()
                    },
                    saveToPhoneAlbum = saveToPhoneAlbum,
                    onSaveToPhoneAlbumChange = {
                        saveToPhoneAlbum = it
                        prefsManager.stampSaveToGalleryEnabled = it
                    },
                    copy = copy
                )
            }
        }
    }
}

@Composable
private fun StampStyleSelector(
    styles: List<StampPunchStyle>,
    selectedStyle: StampPunchStyle,
    enabled: Boolean,
    onStyleSelected: (StampPunchStyle) -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = STAMP_STYLE_SELECTOR_WIDTH_FRACTION
) {
    val glassBackdrop = LocalLiquidGlassBackdrop.current
    val containerShape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(44.dp)
            .clip(containerShape)
            .then(
                if (glassBackdrop != null) {
                    Modifier.liquidGlassPanel(glassBackdrop, containerShape, surfaceColor = Color.Black.copy(alpha = 0.16f))
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.24f))
                }
            )
            .padding(horizontal = 5.dp, vertical = 5.dp)
    ) {
        val itemWidth = maxWidth / styles.size
        val selectedIndex = styles.indexOf(selectedStyle).coerceAtLeast(0)
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "stamp_style_indicator"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.16f))
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            styles.forEach { style ->
                val selected = style == selectedStyle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .noRippleClickable(enabled = enabled && !selected) { onStyleSelected(style) },
                    contentAlignment = Alignment.Center
                ) {
                    StampStyleShapeIcon(
                        style = style,
                        selected = selected,
                        enabled = enabled
                    )
                }
            }
        }
    }
}

@Composable
private fun StampImportScaleSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbRadius = 7.dp
    val trackHorizontalPadding = 12.dp
    val trackStrokeWidth = 2.dp
    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
    ) {
        val minValue = valueRange.start
        val maxValue = valueRange.endInclusive
        val valueSpan = (maxValue - minValue).coerceAtLeast(0.0001f)
        val resolvedValue = value.coerceIn(minValue, maxValue)
        val trackPaddingPx = with(density) { trackHorizontalPadding.toPx() }
        val trackStartPx = trackPaddingPx
        val trackEndPx = (with(density) { maxWidth.toPx() } - trackPaddingPx)
            .coerceAtLeast(trackStartPx + 1f)
        val trackWidthPx = (trackEndPx - trackStartPx).coerceAtLeast(1f)
        val thumbRadiusPx = with(density) { thumbRadius.toPx() }
        val trackStrokePx = with(density) { trackStrokeWidth.toPx() }
        val thumbFraction = ((resolvedValue - minValue) / valueSpan).coerceIn(0f, 1f)
        val thumbCenterXPx = trackStartPx + (trackWidthPx * thumbFraction)

        fun updateSliderValue(positionX: Float) {
            val fraction = ((positionX - trackStartPx) / trackWidthPx).coerceIn(0f, 1f)
            onValueChange(minValue + (valueSpan * fraction))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, minValue, maxValue, trackStartPx, trackWidthPx) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        updateSliderValue(down.position.x)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break
                            updateSliderValue(change.position.x)
                            change.consume()
                            if (!change.pressed) {
                                break
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height * 0.5f
                drawLine(
                    color = Color.White.copy(alpha = if (enabled) 0.56f else 0.28f),
                    start = Offset(trackStartPx, centerY),
                    end = Offset(trackEndPx, centerY),
                    strokeWidth = trackStrokePx,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White.copy(alpha = if (enabled) 0.98f else 0.42f),
                    radius = thumbRadiusPx,
                    center = Offset(thumbCenterXPx, centerY)
                )
            }
        }
    }
}

@Composable
private fun StampStyleShapeIcon(
    style: StampPunchStyle,
    selected: Boolean,
    enabled: Boolean
) {
    if (style == StampPunchStyle.Se) {
        val starAlpha = when {
            !enabled -> 0.42f
            selected -> 1f
            else -> 0.88f
        }
        Text(
            text = "\uD83C\uDF1F",
            fontSize = if (selected) 16.sp else 14.sp,
            modifier = Modifier.graphicsLayer { alpha = starAlpha }
        )
        return
    }
    val tint = when {
        !enabled -> Color.White.copy(alpha = 0.4f)
        selected -> Color.White
        else -> Color.White.copy(alpha = 0.82f)
    }
    Canvas(
        modifier = Modifier.size(if (selected) 22.dp else 20.dp)
    ) {
        val width = size.width
        val height = size.height
        when (style) {
            StampPunchStyle.One -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(width * 0.33f, height * 0.16f),
                    size = Size(width * 0.34f, height * 0.68f)
                )
            }
            StampPunchStyle.Two -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(width * 0.22f, height * 0.22f),
                    size = Size(width * 0.56f, width * 0.56f)
                )
            }
            StampPunchStyle.Three -> {
                drawCircle(
                    color = tint,
                    radius = width * 0.28f,
                    center = Offset(width / 2f, height / 2f)
                )
            }
            StampPunchStyle.FrameCutout -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(width * 0.22f, height * 0.2f),
                    size = Size(width * 0.56f, height * 0.6f),
                    cornerRadius = CornerRadius(width * 0.09f, width * 0.09f),
                    style = Stroke(width = (if (selected) width * 0.11f else width * 0.09f).coerceAtLeast(1f))
                )
            }
            StampPunchStyle.Se -> Unit
            StampPunchStyle.Four -> {
                val path = Path().apply {
                    moveTo(width / 2f, height * 0.18f)
                    lineTo(width * 0.78f, height * 0.78f)
                    lineTo(width * 0.22f, height * 0.78f)
                    close()
                }
                drawPath(path = path, color = tint)
            }
        }
    }
}

@Composable
private fun StampImportPlacementOverlay(
    sourceBitmap: Bitmap,
    styles: List<StampPunchStyle>,
    style: StampPunchStyle,
    template: StampPunchTemplate?,
    overlayBitmap: Bitmap?,
    punchOffset: Offset,
    isPunching: Boolean,
    copy: StampCameraCopy,
    onPunchOffsetChange: (Offset) -> Unit,
    onStyleSelected: (StampPunchStyle) -> Unit,
    onCancel: () -> Unit,
    onPunch: (RectF, IntSize, RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sourceBitmap.isRecycled) return

    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    var containerSize by remember(sourceBitmap) { mutableStateOf(IntSize.Zero) }
    var sourceScale by remember(sourceBitmap) { mutableStateOf(1f) }
    val overlayAspectRatio = remember(template, style) {
        resolveOverlayDisplayAspectRatio(
            template = template,
            fallbackAspect = style.aspectRatio
        )
    }
    val baseImageRect = remember(sourceBitmap, containerSize) {
        if (containerSize.width <= 0 || containerSize.height <= 0) {
            RectF()
        } else {
            resolveImportImageDisplayRect(
                sourceWidth = sourceBitmap.width,
                sourceHeight = sourceBitmap.height,
                containerWidth = containerSize.width.toFloat(),
                containerHeight = containerSize.height.toFloat()
            )
        }
    }
    val isFrameMode = style.isFrameCutoutMode
    val overlayReady = (
        containerSize.width > 0 &&
            containerSize.height > 0 &&
            (isFrameMode || (
                template != null &&
                    overlayBitmap != null &&
                    !overlayBitmap.isRecycled
                )) &&
            baseImageRect.width() > 1f &&
            baseImageRect.height() > 1f
        )
    val overlayWidthPx = if (overlayReady) {
        resolveImportOverlayWidthPx(
            imageRect = baseImageRect,
            containerWidth = containerSize.width.toFloat(),
            style = style
        )
    } else {
        0f
    }
    val overlayHeightPx = if (overlayReady) {
        overlayWidthPx / overlayAspectRatio
    } else {
        0f
    }
    val scaleBounds = if (overlayReady) {
        if (isFrameMode) {
            resolveImportSourceScaleBoundsForFrame(
                imageRect = baseImageRect,
                overlayWidth = overlayWidthPx,
                overlayHeight = overlayHeightPx
            )
        } else {
            resolveImportSourceScaleBounds(
                imageRect = baseImageRect,
                overlayWidth = overlayWidthPx,
                overlayHeight = overlayHeightPx,
                template = requireNotNull(template)
            )
        }
    } else {
        null
    }
    LaunchedEffect(scaleBounds?.start, scaleBounds?.endInclusive, overlayReady) {
        val bounds = scaleBounds ?: return@LaunchedEffect
        val clampedScale = sourceScale.coerceIn(bounds.start, bounds.endInclusive)
        if (abs(clampedScale - sourceScale) > 0.0005f) {
            sourceScale = clampedScale
        }
    }
    val resolvedSourceScale = scaleBounds?.let { range ->
        sourceScale.coerceIn(range.start, range.endInclusive)
    } ?: 1f
    val displayedImageRect = if (baseImageRect.width() > 1f && baseImageRect.height() > 1f) {
        scaleImportImageDisplayRect(baseImageRect, resolvedSourceScale)
    } else {
        RectF()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090909))
            .noRippleClickable(onClick = {})
            .onSizeChanged { containerSize = it }
    ) {
        Image(
            bitmap = sourceBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = resolvedSourceScale
                    scaleY = resolvedSourceScale
                }
        )

        if (overlayReady) {
            val baseCenter = Offset(
                x = (displayedImageRect.left + displayedImageRect.right) * 0.5f,
                y = (displayedImageRect.top + displayedImageRect.bottom) * 0.5f
            )
            val clampedCenter = if (isFrameMode) {
                clampImportFrameCenter(
                    desiredCenter = Offset(baseCenter.x + punchOffset.x, baseCenter.y + punchOffset.y),
                    imageRect = displayedImageRect,
                    overlayWidth = overlayWidthPx,
                    overlayHeight = overlayHeightPx
                )
            } else {
                clampImportPunchCenter(
                    desiredCenter = Offset(baseCenter.x + punchOffset.x, baseCenter.y + punchOffset.y),
                    imageRect = displayedImageRect,
                    overlayWidth = overlayWidthPx,
                    overlayHeight = overlayHeightPx,
                    template = requireNotNull(template)
                )
            }
            LaunchedEffect(clampedCenter, baseCenter) {
                val clampedOffset = Offset(
                    x = clampedCenter.x - baseCenter.x,
                    y = clampedCenter.y - baseCenter.y
                )
                if (
                    abs(clampedOffset.x - punchOffset.x) > 0.5f ||
                    abs(clampedOffset.y - punchOffset.y) > 0.5f
                ) {
                    onPunchOffsetChange(clampedOffset)
                }
            }
            val overlayLeft = clampedCenter.x - overlayWidthPx * 0.5f
            val overlayTop = clampedCenter.y - overlayHeightPx * 0.5f
            val overlayRect = RectF(
                overlayLeft,
                overlayTop,
                overlayLeft + overlayWidthPx,
                overlayTop + overlayHeightPx
            )
            val latestOverlayRect by rememberUpdatedState(overlayRect)
            val latestCenter by rememberUpdatedState(clampedCenter)
            val latestBaseCenter by rememberUpdatedState(baseCenter)
            val latestImageRect by rememberUpdatedState(displayedImageRect)
            val latestOverlayWidthPx by rememberUpdatedState(overlayWidthPx)
            val latestOverlayHeightPx by rememberUpdatedState(overlayHeightPx)

            if (isFrameMode) {
                FrameCutoutGuideOverlay(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = overlayRect.left.roundToInt(),
                                y = overlayRect.top.roundToInt()
                            )
                        }
                        .width(with(density) { overlayWidthPx.toDp() })
                        .aspectRatio(overlayAspectRatio),
                    tint = Color.White.copy(alpha = 0.95f)
                )
            } else {
                val resolvedOverlayBitmap = overlayBitmap?.takeUnless { it.isRecycled }
                if (resolvedOverlayBitmap != null) {
                    Image(
                        bitmap = resolvedOverlayBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = overlayRect.left.roundToInt(),
                                    y = overlayRect.top.roundToInt()
                                )
                            }
                            .width(with(density) { overlayWidthPx.toDp() })
                            .aspectRatio(overlayAspectRatio)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        sourceBitmap,
                        style,
                        isFrameMode,
                        template,
                        containerSize,
                        isPunching
                    ) {
                        if (isPunching) return@pointerInput
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startOverlayRect = latestOverlayRect
                            if (!startOverlayRect.contains(down.position.x, down.position.y)) {
                                return@awaitEachGesture
                            }
                            down.consume()

                            var gestureCenter = latestCenter
                            var lastPosition = down.position
                            var totalDragX = 0f
                            var totalDragY = 0f
                            var isDragging = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                    ?: break
                                val delta = change.position - lastPosition

                                if (change.pressed) {
                                    totalDragX += delta.x
                                    totalDragY += delta.y

                                    if (!isDragging &&
                                        hypot(totalDragX.toDouble(), totalDragY.toDouble()) >= touchSlop
                                    ) {
                                        isDragging = true
                                    }

                                    if (isDragging && delta != Offset.Zero) {
                                        gestureCenter = if (isFrameMode) {
                                            clampImportFrameCenter(
                                                desiredCenter = Offset(
                                                    x = gestureCenter.x + delta.x,
                                                    y = gestureCenter.y + delta.y
                                                ),
                                                imageRect = latestImageRect,
                                                overlayWidth = latestOverlayWidthPx,
                                                overlayHeight = latestOverlayHeightPx
                                            )
                                        } else {
                                            clampImportPunchCenter(
                                                desiredCenter = Offset(
                                                    x = gestureCenter.x + delta.x,
                                                    y = gestureCenter.y + delta.y
                                                ),
                                                imageRect = latestImageRect,
                                                overlayWidth = latestOverlayWidthPx,
                                                overlayHeight = latestOverlayHeightPx,
                                                template = requireNotNull(template)
                                            )
                                        }
                                        onPunchOffsetChange(
                                            Offset(
                                                x = gestureCenter.x - latestBaseCenter.x,
                                                y = gestureCenter.y - latestBaseCenter.y
                                            )
                                        )
                                    }
                                    lastPosition = change.position
                                    change.consume()
                                } else {
                                    change.consume()
                                    if (!isDragging) {
                                        val overlayWidth = latestOverlayWidthPx
                                        val overlayHeight = latestOverlayHeightPx
                                        val selectionRect = RectF(
                                            gestureCenter.x - overlayWidth * 0.5f,
                                            gestureCenter.y - overlayHeight * 0.5f,
                                            gestureCenter.x + overlayWidth * 0.5f,
                                            gestureCenter.y + overlayHeight * 0.5f
                                        )
                                        onPunch(
                                            if (isFrameMode) {
                                                buildImportFrameViewport(
                                                    sourceBitmap = sourceBitmap,
                                                    imageRect = latestImageRect,
                                                    overlayRect = selectionRect
                                                )
                                            } else {
                                                buildImportPunchViewport(
                                                    sourceBitmap = sourceBitmap,
                                                    imageRect = latestImageRect,
                                                    overlayRect = selectionRect,
                                                    template = requireNotNull(template)
                                                )
                                            },
                                            containerSize,
                                            RectF(latestImageRect)
                                        )
                                    }
                                    break
                                }
                            }
                        }
                    }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraChromeIconButton(
                onClick = onCancel,
                enabled = !isPunching
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = copy.cancel,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = copy.importPhoto,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = copy.importPlacementHint,
                    color = Color.White.copy(alpha = 0.74f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (scaleBounds != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StampStyleSelector(
                        styles = styles,
                        selectedStyle = style,
                        enabled = !isPunching,
                        onStyleSelected = onStyleSelected,
                        modifier = Modifier.weight(1.08f),
                        widthFraction = 1f
                    )
                    StampImportScaleSlider(
                        value = resolvedSourceScale,
                        valueRange = scaleBounds,
                        enabled = !isPunching,
                        onValueChange = { nextScale ->
                            sourceScale = nextScale
                        },
                        modifier = Modifier.weight(0.92f)
                    )
                }
            } else {
                StampStyleSelector(
                    styles = styles,
                    selectedStyle = style,
                    enabled = !isPunching,
                    onStyleSelected = onStyleSelected
                )
            }
        }
    }
}

private fun resolveFrameCutoutGuideGeometry(
    left: Float,
    top: Float,
    width: Float,
    height: Float
): FrameCutoutGuideGeometry {
    val safeWidth = width.coerceAtLeast(1f)
    val safeHeight = height.coerceAtLeast(1f)
    val shortSide = min(safeWidth, safeHeight).coerceAtLeast(1f)
    val inset = (shortSide * 0.075f).coerceIn(12f, 28f)
    val guideLeft = left + inset
    val guideTop = top + inset
    val guideRight = (left + safeWidth - inset).coerceAtLeast(guideLeft + 1f)
    val guideBottom = (top + safeHeight - inset).coerceAtLeast(guideTop + 1f)
    val guideWidth = (guideRight - guideLeft).coerceAtLeast(1f)
    val guideHeight = (guideBottom - guideTop).coerceAtLeast(1f)
    val guideShortSide = min(guideWidth, guideHeight).coerceAtLeast(1f)
    val cornerLength = (guideShortSide * 0.2f)
        .coerceIn(26f, guideShortSide * 0.34f)
    val cornerRadius = (cornerLength * 0.58f)
        .coerceIn(12f, cornerLength * 0.72f)
    val strokeWidth = (guideShortSide * 0.016f).coerceIn(4f, 8f)
    return FrameCutoutGuideGeometry(
        left = guideLeft,
        top = guideTop,
        right = guideRight,
        bottom = guideBottom,
        strokeWidth = strokeWidth,
        cornerRadius = cornerRadius,
        cornerLength = cornerLength
    )
}

private fun DrawScope.drawFrameCutoutGuide(
    geometry: FrameCutoutGuideGeometry,
    color: Color
) {
    val arcSize = Size(
        width = geometry.cornerRadius * 2f,
        height = geometry.cornerRadius * 2f
    )
    val stroke = Stroke(
        width = geometry.strokeWidth,
        cap = StrokeCap.Round
    )
    val horizontalLength = max(
        0f,
        geometry.cornerLength - geometry.cornerRadius
    )
    val verticalLength = max(
        0f,
        geometry.cornerLength - geometry.cornerRadius
    )

    fun drawTopLeft() {
        drawLine(
            color = color,
            start = Offset(geometry.left + geometry.cornerRadius, geometry.top),
            end = Offset(
                geometry.left + geometry.cornerRadius + horizontalLength,
                geometry.top
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(geometry.left, geometry.top + geometry.cornerRadius),
            end = Offset(
                geometry.left,
                geometry.top + geometry.cornerRadius + verticalLength
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(geometry.left, geometry.top),
            size = arcSize,
            style = stroke
        )
    }

    fun drawTopRight() {
        drawLine(
            color = color,
            start = Offset(geometry.right - geometry.cornerRadius, geometry.top),
            end = Offset(
                geometry.right - geometry.cornerRadius - horizontalLength,
                geometry.top
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(geometry.right, geometry.top + geometry.cornerRadius),
            end = Offset(
                geometry.right,
                geometry.top + geometry.cornerRadius + verticalLength
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(geometry.right - arcSize.width, geometry.top),
            size = arcSize,
            style = stroke
        )
    }

    fun drawBottomRight() {
        drawLine(
            color = color,
            start = Offset(geometry.right - geometry.cornerRadius, geometry.bottom),
            end = Offset(
                geometry.right - geometry.cornerRadius - horizontalLength,
                geometry.bottom
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(geometry.right, geometry.bottom - geometry.cornerRadius),
            end = Offset(
                geometry.right,
                geometry.bottom - geometry.cornerRadius - verticalLength
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(
                geometry.right - arcSize.width,
                geometry.bottom - arcSize.height
            ),
            size = arcSize,
            style = stroke
        )
    }

    fun drawBottomLeft() {
        drawLine(
            color = color,
            start = Offset(geometry.left + geometry.cornerRadius, geometry.bottom),
            end = Offset(
                geometry.left + geometry.cornerRadius + horizontalLength,
                geometry.bottom
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(geometry.left, geometry.bottom - geometry.cornerRadius),
            end = Offset(
                geometry.left,
                geometry.bottom - geometry.cornerRadius - verticalLength
            ),
            strokeWidth = geometry.strokeWidth,
            cap = StrokeCap.Round
        )
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(geometry.left, geometry.bottom - arcSize.height),
            size = arcSize,
            style = stroke
        )
    }

    drawTopLeft()
    drawTopRight()
    drawBottomRight()
    drawBottomLeft()
}

@Composable
private fun StampResultOverlay(
    stampBitmap: Bitmap?,
    backdropBitmap: Bitmap?,
    transitionState: StampPunchCutoutEffectState?,
    style: StampPunchStyle,
    landscapeMode: Boolean,
    overlayCenterYOffset: Dp,
    stampDisplayWidthFraction: Float,
    isSaving: Boolean,
    actionsEnabled: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    saveToPhoneAlbum: Boolean,
    onSaveToPhoneAlbumChange: (Boolean) -> Unit,
    copy: StampCameraCopy
) {
    val settledStampBitmap = stampBitmap?.takeUnless { it.isRecycled }
    val transitionStampBitmap = transitionState?.liftedStampBitmap?.takeUnless { it.isRecycled }
    val isFrameTransition = transitionState != null && transitionState.holeMaskBitmap == null
    val displayStampBitmap = transitionStampBitmap ?: settledStampBitmap
    val settledBackdropBitmap = backdropBitmap?.takeUnless { it.isRecycled }
    val transitionBackdropBitmap = transitionState?.punchedBackdropBitmap?.takeUnless { it.isRecycled }
    val fallbackBackdropBitmap = transitionState?.fallbackBackdropBitmap?.takeUnless { it.isRecycled }
    val baseBackdropBitmap = transitionBackdropBitmap ?: fallbackBackdropBitmap ?: settledBackdropBitmap
    val paperBackdropBitmap = fallbackBackdropBitmap ?: settledBackdropBitmap ?: baseBackdropBitmap
    if (displayStampBitmap == null && baseBackdropBitmap == null && paperBackdropBitmap == null) {
        return
    }
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    fun performInteractionHaptic() {
        if (hapticsAllowed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    var sceneStarted by remember(settledStampBitmap) { mutableStateOf(false) }
    var transitionStarted by remember(transitionState?.token) { mutableStateOf(false) }
    var confirming by remember(settledStampBitmap) { mutableStateOf(false) }
    var controlsVisible by remember(settledStampBitmap) { mutableStateOf(false) }
    LaunchedEffect(settledStampBitmap) {
        sceneStarted = true
    }
    LaunchedEffect(transitionState?.token) {
        transitionStarted = transitionState != null
    }
    LaunchedEffect(transitionState?.token, settledStampBitmap) {
        if (settledStampBitmap == null) {
            controlsVisible = false
            return@LaunchedEffect
        }
        if (transitionState != null) {
            controlsVisible = false
        } else {
            controlsVisible = false
            delay(90)
            controlsVisible = true
        }
    }
    LaunchedEffect(confirming, isSaving, actionsEnabled, settledStampBitmap) {
        if (confirming && settledStampBitmap != null && !isSaving && actionsEnabled) {
            delay(220)
            if (confirming && !isSaving && actionsEnabled) {
                onConfirm()
            }
        }
    }
    LaunchedEffect(isSaving) {
        if (isSaving) {
            confirming = true
        }
    }
    val frameScanTransition = rememberInfiniteTransition(label = "stamp_frame_cutout_scan")
    val frameScanProgress by frameScanTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 960, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stamp_frame_cutout_scan_progress"
    )
    val settledBackdropProgress by animateFloatAsState(
        targetValue = if (sceneStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing),
        label = "stamp_result_backdrop"
    )
    val cutRevealProgressRaw by animateFloatAsState(
        targetValue = if (transitionState != null && transitionStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stamp_result_cut_reveal"
    )
    val liftProgressRaw by animateFloatAsState(
        targetValue = if (transitionState != null && transitionStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
        label = "stamp_result_lift_progress"
    )
    val travelProgressRaw by animateFloatAsState(
        targetValue = if (transitionState != null && transitionStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1160, easing = FastOutSlowInEasing),
        label = "stamp_result_travel_progress"
    )
    val cutRevealProgress = if (transitionState != null) cutRevealProgressRaw else 1f
    val liftProgress = if (transitionState != null) liftProgressRaw else 1f
    val travelProgress = if (transitionState != null) travelProgressRaw else 1f
    val confirmProgress by animateFloatAsState(
        targetValue = if (confirming) 1f else 0f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "stamp_result_confirm_progress"
    )
    val controlsAlpha by animateFloatAsState(
        targetValue = if (confirming) 0f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "stamp_result_controls_alpha"
    )
    val controlsRevealProgress by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
        label = "stamp_result_controls_reveal"
    )
    val backdropProgress = when {
        transitionState == null -> settledBackdropProgress
        isFrameTransition && transitionStampBitmap == null ->
            (0.1f + (cutRevealProgress * 0.18f)).coerceIn(0f, 0.34f)
        isFrameTransition -> ((travelProgress - 0.08f) / 0.92f).coerceIn(0f, 1f)
        else -> ((travelProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
    }
    val controlsRevealAlpha = (controlsAlpha * controlsRevealProgress).coerceIn(0f, 1f)
    val controlsTranslateY = with(LocalDensity.current) {
        (1f - controlsRevealProgress) * 26.dp.toPx()
    }
    val interactionEnabled = controlsVisible && settledStampBitmap != null && !isSaving && !confirming
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .noRippleClickable(onClick = {})
    ) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val resolvedAspect = displayStampBitmap?.let { bitmap ->
            (bitmap.width.toFloat() / bitmap.height.toFloat()).takeIf { it.isFinite() && it > 0f }
        } ?: style.aspectRatio
        val targetWidth = (maxWidthPx * stampDisplayWidthFraction).coerceAtLeast(1f)
        val targetHeight = (targetWidth / resolvedAspect).coerceAtLeast(1f)
        val targetLeft = (maxWidthPx - targetWidth) * 0.5f
        val targetTop = ((maxHeightPx - targetHeight) * 0.5f) + with(density) {
            overlayCenterYOffset.toPx()
        }
        val holeRect = transitionState?.holeRect
        val holeMaskBitmap = transitionState?.holeMaskBitmap?.takeUnless { it.isRecycled }
        val holeWidth = holeRect?.width()?.coerceAtLeast(1f) ?: targetWidth
        val holeHeight = holeRect?.height()?.coerceAtLeast(1f) ?: targetHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            StampPaperBackdropSurface(
                backdropBitmap = paperBackdropBitmap,
                baseBackdropBitmap = baseBackdropBitmap,
                photoBlurAlpha = STAMP_PREVIEW_PHOTO_BLUR_ALPHA * backdropProgress,
                paperAlpha = STAMP_PREVIEW_PAPER_ALPHA * backdropProgress,
                gridAlpha = STAMP_PREVIEW_GRID_ALPHA * backdropProgress,
                modifier = Modifier.fillMaxSize()
            )
            if (transitionState != null && holeRect != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isFrameTransition) {
                        val guideGeometry = resolveFrameCutoutGuideGeometry(
                            left = holeRect.left,
                            top = holeRect.top,
                            width = holeWidth,
                            height = holeHeight
                        )
                        val overlayAlpha = (
                            0.1f *
                                cutRevealProgress *
                                (1f - 0.24f * backdropProgress)
                            ).coerceIn(0f, 1f)
                        if (overlayAlpha > 0f) {
                            drawRect(Color.Black.copy(alpha = overlayAlpha))
                        }
                        drawFrameCutoutGuide(
                            geometry = guideGeometry,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        val scanBandHeight = (guideGeometry.height * 0.15f)
                            .coerceIn(20f, guideGeometry.height * 0.24f)
                        val scanTravel = (guideGeometry.height + scanBandHeight) * frameScanProgress
                        val scanTop = (guideGeometry.top - scanBandHeight) + scanTravel
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.34f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = scanTop,
                                endY = scanTop + scanBandHeight
                            ),
                            topLeft = Offset(guideGeometry.left, scanTop),
                            size = Size(guideGeometry.width, scanBandHeight),
                            cornerRadius = CornerRadius(
                                guideGeometry.cornerRadius,
                                guideGeometry.cornerRadius
                            )
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.52f),
                            start = Offset(
                                guideGeometry.left + guideGeometry.strokeWidth,
                                scanTop + scanBandHeight * 0.5f
                            ),
                            end = Offset(
                                guideGeometry.right - guideGeometry.strokeWidth,
                                scanTop + scanBandHeight * 0.5f
                            ),
                            strokeWidth = (guideGeometry.strokeWidth * 0.88f).coerceAtLeast(2f),
                            cap = StrokeCap.Round
                        )
                    } else {
                        drawRect(
                            Color.Black.copy(
                                alpha = (
                                    0.14f *
                                        cutRevealProgress *
                                        (1f - 0.42f * backdropProgress)
                                    ).coerceIn(0f, 1f)
                            )
                        )
                        val holeAlpha = (
                            0.94f * cutRevealProgress * (1f - backdropProgress)
                            ).coerceIn(0f, 1f)
                        if (transitionBackdropBitmap == null && holeAlpha > 0f && holeMaskBitmap != null) {
                            drawImage(
                                image = holeMaskBitmap.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(
                                    holeRect.left.roundToInt(),
                                    holeRect.top.roundToInt()
                                ),
                                dstSize = IntSize(
                                    holeWidth.roundToInt().coerceAtLeast(1),
                                    holeHeight.roundToInt().coerceAtLeast(1)
                                ),
                                colorFilter = ColorFilter.tint(
                                    Color.Black.copy(alpha = holeAlpha),
                                    blendMode = BlendMode.SrcIn
                                )
                            )
                        }
                    }
                }
            }
        }
        if (displayStampBitmap != null) {
            val liftedCenterX = holeRect?.centerX()?.plus(7f * liftProgress) ?: (maxWidthPx * 0.5f)
            val liftedCenterY = if (isFrameTransition) {
                holeRect?.centerY() ?: (maxHeightPx * 0.5f)
            } else {
                holeRect?.centerY()?.minus(132f * liftProgress) ?: (maxHeightPx * 0.5f)
            }
            val liftedWidth = if (isFrameTransition) {
                holeWidth
            } else {
                holeWidth * STAMP_PUNCH_LIFTED_STAMP_VISUAL_SCALE
            }
            val liftedHeight = if (isFrameTransition) {
                holeHeight
            } else {
                holeHeight * STAMP_PUNCH_LIFTED_STAMP_VISUAL_SCALE
            }
            val liftedLeft = liftedCenterX - (liftedWidth * 0.5f)
            val liftedTop = liftedCenterY - (liftedHeight * 0.5f)
            val cardLeft = if (transitionState != null) {
                lerpFloat(liftedLeft, targetLeft, travelProgress)
            } else {
                targetLeft
            }
            val cardTop = if (transitionState != null) {
                lerpFloat(liftedTop, targetTop, travelProgress)
            } else {
                targetTop
            }
            val cardWidth = if (transitionState != null) {
                lerpFloat(liftedWidth, targetWidth, travelProgress).coerceAtLeast(1f)
            } else {
                targetWidth
            }
            val cardHeight = if (transitionState != null) {
                lerpFloat(liftedHeight, targetHeight, travelProgress).coerceAtLeast(1f)
            } else {
                targetHeight
            }
            val cardRotationZ = if (transitionState != null) {
                if (isFrameTransition) {
                    lerpFloat(-3.2f * liftProgress, 0f, travelProgress)
                } else {
                    lerpFloat(-8.6f * liftProgress, 0f, travelProgress)
                }
            } else {
                0f
            }
            val cardRotationX = if (transitionState != null) {
                if (isFrameTransition) {
                    lerpFloat(5.6f * liftProgress, 0f, travelProgress)
                } else {
                    lerpFloat(10f * liftProgress, 0f, travelProgress)
                }
            } else {
                0f
            }
            val confirmShadowVisibility = (1f - (confirmProgress / 0.24f)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = cardLeft.roundToInt(),
                            y = cardTop.roundToInt()
                        )
                    }
                    .width(with(density) { cardWidth.toDp() })
                    .height(with(density) { cardHeight.toDp() })
                    .graphicsLayer {
                        translationY = 220f * confirmProgress
                        rotationZ = cardRotationZ
                        rotationX = cardRotationX
                        scaleX = 1f - (0.06f * confirmProgress)
                        scaleY = 1f - (0.06f * confirmProgress)
                        alpha = (1f - (0.24f * confirmProgress)).coerceIn(0f, 1f)
                        shadowElevation = 0f
                        cameraDistance = 18f * density.density
                    }
            ) {
                StampShadowedBitmap(
                    stampBitmap = displayStampBitmap,
                    shadowVisibility = confirmShadowVisibility,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        AnimatedVisibility(
            visible = settledStampBitmap != null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .align(if (landscapeMode) Alignment.CenterEnd else Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    horizontal = if (landscapeMode) 18.dp else 22.dp,
                    vertical = if (landscapeMode) 18.dp else 0.dp
                )
                .padding(
                    bottom = if (landscapeMode) 6.dp else 52.dp,
                    end = if (landscapeMode) 14.dp else 0.dp
                )
        ) {
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = controlsRevealAlpha
                    translationY = controlsTranslateY
                },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .noRippleClickable(enabled = interactionEnabled) {
                            performInteractionHaptic()
                            onSaveToPhoneAlbumChange(!saveToPhoneAlbum)
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = saveToPhoneAlbum,
                        onCheckedChange = {
                            if (interactionEnabled) {
                                performInteractionHaptic()
                                onSaveToPhoneAlbumChange(it)
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = copy.saveToPhoneAlbum,
                        color = Color(0xFF181512),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    horizontalArrangement = if (landscapeMode) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (landscapeMode) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CameraGlassCircleButton(
                                enabled = interactionEnabled,
                                onClick = onCancel,
                                size = 72.dp,
                                fallbackColor = Color.White.copy(alpha = 0.12f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = copy.cancel,
                                    tint = Color(0xFFE14545),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            CameraGlassCircleButton(
                                enabled = actionsEnabled && interactionEnabled,
                                onClick = {
                                    performInteractionHaptic()
                                    confirming = true
                                },
                                size = 80.dp,
                                fallbackColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = copy.save,
                                    tint = Color.Black,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    } else {
                        CameraGlassCircleButton(
                            enabled = interactionEnabled,
                            onClick = onCancel,
                            size = 72.dp,
                            fallbackColor = Color.White.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = copy.cancel,
                                tint = Color(0xFFE14545),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        CameraGlassCircleButton(
                            enabled = actionsEnabled && interactionEnabled,
                            onClick = {
                                performInteractionHaptic()
                                confirming = true
                            },
                            size = 80.dp,
                            fallbackColor = Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = copy.save,
                                tint = Color.Black,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
                if ((isSaving || !actionsEnabled) && controlsRevealAlpha > 0.01f) {
                    CircularProgressIndicator(
                        color = Color(0xFF181512),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameCutoutGuideOverlay(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.95f)
) {
    Canvas(modifier = modifier) {
        val geometry = resolveFrameCutoutGuideGeometry(
            left = 0f,
            top = 0f,
            width = size.width,
            height = size.height
        )
        drawFrameCutoutGuide(
            geometry = geometry,
            color = tint
        )
    }
}

@Composable
private fun StampPaperBackdropSurface(
    backdropBitmap: Bitmap?,
    baseBackdropBitmap: Bitmap? = null,
    photoBlurAlpha: Float,
    paperAlpha: Float,
    gridAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (
            baseBackdropBitmap != null &&
            !baseBackdropBitmap.isRecycled
        ) {
            Image(
                bitmap = baseBackdropBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (
            photoBlurAlpha > 0.001f &&
            backdropBitmap != null &&
            !backdropBitmap.isRecycled
        ) {
            Image(
                bitmap = backdropBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.16f
                        scaleY = 1.16f
                        alpha = photoBlurAlpha.coerceIn(0f, 1f)
                    }
                    .blur(40.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F2E7).copy(alpha = paperAlpha.coerceIn(0f, 1f)))
                .stampPaperGridBackground(alpha = gridAlpha.coerceIn(0f, 1f))
        )
    }
}

@Composable
private fun StampShadowedBitmap(
    stampBitmap: Bitmap,
    shadowVisibility: Float,
    modifier: Modifier = Modifier
) {
    val castShadowFilter = remember(shadowVisibility) {
        ColorFilter.tint(
            Color(0xFF2E261C).copy(alpha = 0.3f * shadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    val contactShadowFilter = remember(shadowVisibility) {
        ColorFilter.tint(
            Color(0xFF3A3125).copy(alpha = 0.2f * shadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    val edgeShadeFilter = remember(shadowVisibility) {
        ColorFilter.tint(
            Color(0xFF211A13).copy(alpha = 0.12f * shadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    val edgeHighlightFilter = remember(shadowVisibility) {
        ColorFilter.tint(
            Color.White.copy(alpha = 0.1f * shadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    Box(modifier = modifier) {
        if (shadowVisibility > 0.01f) {
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = castShadowFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 2.dp, y = 6.dp)
                    .blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = contactShadowFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 1.dp, y = 2.dp)
                    .blur(1.2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = edgeShadeFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 0.7.dp, y = 0.8.dp)
            )
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = edgeHighlightFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-0.55).dp, y = (-0.65).dp)
            )
        }
        Image(
            bitmap = stampBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.High,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StampResultShadowedCard(
    stampBitmap: Bitmap,
    styleAspect: Float,
    overlayCenterYOffset: Dp,
    widthFraction: Float,
    liftOffset: Float,
    stampScale: Float,
    contentAlpha: Float,
    confirmProgress: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val resolvedAspect = (stampBitmap.width.toFloat() / stampBitmap.height.toFloat())
        .takeIf { it.isFinite() && it > 0f }
        ?: styleAspect
    val confirmShadowVisibility = (1f - (confirmProgress / 0.24f)).coerceIn(0f, 1f)
    val castShadowFilter = remember(confirmShadowVisibility) {
        ColorFilter.tint(
            Color(0xFF2E261C).copy(alpha = 0.3f * confirmShadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    val contactShadowFilter = remember(confirmShadowVisibility) {
        ColorFilter.tint(
            Color(0xFF3A3125).copy(alpha = 0.2f * confirmShadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    val edgeShadeFilter = remember(confirmShadowVisibility) {
        ColorFilter.tint(
            Color(0xFF211A13).copy(alpha = 0.12f * confirmShadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    val edgeHighlightFilter = remember(confirmShadowVisibility) {
        ColorFilter.tint(
            Color.White.copy(alpha = 0.1f * confirmShadowVisibility),
            blendMode = BlendMode.SrcIn
        )
    }
    Box(
        modifier = modifier
            .offset(y = overlayCenterYOffset)
            .fillMaxWidth(widthFraction)
            .aspectRatio(resolvedAspect)
            .graphicsLayer {
                translationY = liftOffset + (220f * confirmProgress)
                scaleX = stampScale * (1f - (0.06f * confirmProgress))
                scaleY = stampScale * (1f - (0.06f * confirmProgress))
                alpha = contentAlpha.coerceIn(0f, 1f) * (1f - (0.24f * confirmProgress))
                // Avoid rectangular container shadow leaking behind non-rect stamp shapes.
                shadowElevation = 0f
                cameraDistance = 18f * density.density
            }
    ) {
        if (confirmShadowVisibility > 0.01f) {
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = castShadowFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 2.dp, y = 6.dp)
                    .blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = contactShadowFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 1.dp, y = 2.dp)
                    .blur(1.2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = edgeShadeFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 0.7.dp, y = 0.8.dp)
            )
            Image(
                bitmap = stampBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
                colorFilter = edgeHighlightFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-0.55).dp, y = (-0.65).dp)
            )
        }
        Image(
            bitmap = stampBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.High,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StampPunchCutoutTransitionOverlay(
    state: StampPunchCutoutEffectState,
    drawBackdrop: Boolean,
    drawLiftedStamp: Boolean,
    modifier: Modifier = Modifier
) {
    if (!drawBackdrop && !drawLiftedStamp) return
    val fallback = state.fallbackBackdropBitmap.takeUnless { it.isRecycled } ?: return
    var started by remember(state.token) { mutableStateOf(false) }
    LaunchedEffect(state.token) {
        started = true
    }
    val cutRevealProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stamp_punch_cut_reveal"
    )
    val liftProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
        label = "stamp_punch_lift_progress"
    )
    val travelProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 1160, easing = FastOutSlowInEasing),
        label = "stamp_punch_travel_progress"
    )
    val holeRect = state.holeRect
    val density = LocalDensity.current
    val liftedBitmap = state.liftedStampBitmap?.takeUnless { it.isRecycled }
    val backdropBitmap = state.punchedBackdropBitmap?.takeUnless { it.isRecycled }
    val holeMaskBitmap = state.holeMaskBitmap?.takeUnless { it.isRecycled }
    val holeWidth = holeRect.width().coerceAtLeast(1f)
    val holeHeight = holeRect.height().coerceAtLeast(1f)
    val stampAspect = liftedBitmap?.let { bmp ->
        (bmp.width.toFloat() / bmp.height.toFloat()).takeIf { it.isFinite() && it > 0f }
    } ?: (holeWidth / holeHeight).coerceAtLeast(0.3f)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (drawBackdrop) Modifier.noRippleClickable(onClick = {}) else Modifier
            )
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val targetWidth = (maxWidthPx * state.targetPreviewWidthFraction).coerceAtLeast(holeWidth)
        val targetHeight = (targetWidth / stampAspect).coerceAtLeast(1f)
        val targetLeft = (maxWidthPx - targetWidth) * 0.5f
        val targetTop = ((maxHeightPx - targetHeight) * 0.5f) + state.targetPreviewCenterYOffsetPx
        val backdropTransitionProgress = ((travelProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)

        if (drawBackdrop) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val activeBackdropBitmap = backdropBitmap ?: fallback
                Image(
                    bitmap = activeBackdropBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize()
                )
                StampPaperBackdropSurface(
                    backdropBitmap = fallback,
                    photoBlurAlpha = STAMP_PREVIEW_PHOTO_BLUR_ALPHA * backdropTransitionProgress,
                    paperAlpha = STAMP_PREVIEW_PAPER_ALPHA * backdropTransitionProgress,
                    gridAlpha = STAMP_PREVIEW_GRID_ALPHA * backdropTransitionProgress,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        Color.Black.copy(
                            alpha = (0.14f * cutRevealProgress * (1f - 0.42f * backdropTransitionProgress))
                                .coerceIn(0f, 1f)
                        )
                    )
                    val holeAlpha = (
                        0.94f * cutRevealProgress * (1f - backdropTransitionProgress)
                        ).coerceIn(0f, 1f)
                    if (backdropBitmap == null && holeAlpha > 0f) {
                        val holeLeft = holeRect.left.roundToInt()
                        val holeTop = holeRect.top.roundToInt()
                        val holeDstSize = IntSize(
                            holeWidth.roundToInt().coerceAtLeast(1),
                            holeHeight.roundToInt().coerceAtLeast(1)
                        )
                        if (holeMaskBitmap != null) {
                            drawImage(
                                image = holeMaskBitmap.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(holeLeft, holeTop),
                                dstSize = holeDstSize,
                                colorFilter = ColorFilter.tint(
                                    Color.Black.copy(alpha = holeAlpha),
                                    blendMode = BlendMode.SrcIn
                                )
                            )
                        }
                    }
                }
            }
        }
        if (drawLiftedStamp && liftedBitmap != null) {
            val liftedCenterX = holeRect.centerX() + (7f * liftProgress)
            val liftedCenterY = holeRect.centerY() - (132f * liftProgress)
            val liftedWidth = holeWidth * STAMP_PUNCH_LIFTED_STAMP_VISUAL_SCALE
            val liftedHeight = holeHeight * STAMP_PUNCH_LIFTED_STAMP_VISUAL_SCALE
            val liftedLeft = liftedCenterX - (liftedWidth * 0.5f)
            val liftedTop = liftedCenterY - (liftedHeight * 0.5f)
            val cardLeft = lerpFloat(liftedLeft, targetLeft, travelProgress)
            val cardTop = lerpFloat(liftedTop, targetTop, travelProgress)
            val cardWidth = lerpFloat(liftedWidth, targetWidth, travelProgress).coerceAtLeast(1f)
            val cardHeight = lerpFloat(liftedHeight, targetHeight, travelProgress).coerceAtLeast(1f)
            val cardRotationZ = lerpFloat(-8.6f * liftProgress, 0f, travelProgress)
            val cardRotationX = lerpFloat(10f * liftProgress, 0f, travelProgress)
            val shadowStrength = (1f - (0.08f * travelProgress)).coerceIn(0.9f, 1f)
            val castShadowFilter = remember(shadowStrength) {
                ColorFilter.tint(
                    Color(0xFF2E261C).copy(alpha = 0.3f * shadowStrength),
                    blendMode = BlendMode.SrcIn
                )
            }
            val contactShadowFilter = remember(shadowStrength) {
                ColorFilter.tint(
                    Color(0xFF3A3125).copy(alpha = 0.2f * shadowStrength),
                    blendMode = BlendMode.SrcIn
                )
            }
            val edgeShadeFilter = remember(shadowStrength) {
                ColorFilter.tint(
                    Color(0xFF211A13).copy(alpha = 0.12f * shadowStrength),
                    blendMode = BlendMode.SrcIn
                )
            }
            val edgeHighlightFilter = remember(shadowStrength) {
                ColorFilter.tint(
                    Color.White.copy(alpha = 0.1f * shadowStrength),
                    blendMode = BlendMode.SrcIn
                )
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = cardLeft.roundToInt(),
                            y = cardTop.roundToInt()
                        )
                    }
                    .width(with(density) { cardWidth.toDp() })
                    .height(with(density) { cardHeight.toDp() })
                    .graphicsLayer {
                        rotationZ = cardRotationZ
                        rotationX = cardRotationX
                        // Keep transition shadow shape from stamp bitmap only.
                        shadowElevation = 0f
                        cameraDistance = 18f * density.density
                    }
            ) {
                Image(
                    bitmap = liftedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.Medium,
                    colorFilter = castShadowFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = 2.dp, y = 6.dp)
                        .blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
                Image(
                    bitmap = liftedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.Medium,
                    colorFilter = contactShadowFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = 1.dp, y = 2.dp)
                        .blur(1.2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
                Image(
                    bitmap = liftedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.Medium,
                    colorFilter = edgeShadeFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = 0.7.dp, y = 0.8.dp)
                )
                Image(
                    bitmap = liftedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.Medium,
                    colorFilter = edgeHighlightFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = (-0.55).dp, y = (-0.65).dp)
                )
                Image(
                    bitmap = liftedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CameraGlassCircleButton(
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
    fallbackColor: Color,
    content: @Composable () -> Unit
) {
    val glassBackdrop = LocalLiquidGlassBackdrop.current
    Surface(
        shape = CircleShape,
        color = if (glassBackdrop != null) Color.Transparent else fallbackColor,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (glassBackdrop != null) {
                    Modifier.liquidGlassControl(glassBackdrop, CircleShape, surfaceColor = Color.Black.copy(alpha = 0.16f))
                } else {
                    Modifier
                }
            )
            .noRippleClickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun CameraChromeIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    CameraGlassCircleButton(
        enabled = enabled,
        onClick = onClick,
        size = 42.dp,
        fallbackColor = Color.Black.copy(alpha = 0.26f),
        content = content
    )
}

@Composable
private fun CameraUtilityButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    CameraGlassCircleButton(
        enabled = enabled,
        onClick = onClick,
        size = 60.dp,
        fallbackColor = if (enabled) {
            Color.Black.copy(alpha = 0.26f)
        } else {
            Color.Black.copy(alpha = 0.16f)
        },
        content = content
    )
}

private fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

private fun Modifier.noRipplePressCapture(
    enabled: Boolean = true,
    onPressChanged: (Boolean) -> Unit,
    onCapture: () -> Unit
): Modifier = composed {
    DisposableEffect(enabled) {
        if (!enabled) {
            onPressChanged(false)
        }
        onDispose {
            onPressChanged(false)
        }
    }
    if (!enabled) {
        this
    } else {
        pointerInput(enabled, onCapture) {
            detectTapGestures(
                onPress = {
                    onPressChanged(true)
                    val released = tryAwaitRelease()
                    onPressChanged(false)
                    if (released) {
                        onCapture()
                    }
                }
            )
        }
    }
}

private fun captureStampPreviewBitmap(previewView: PreviewView): Bitmap? {
    val rawBitmap = previewView.bitmap ?: return null
    val copiedBitmap = rawBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return rawBitmap
    val currentMaxSide = maxOf(copiedBitmap.width, copiedBitmap.height).coerceAtLeast(1)
    if (currentMaxSide <= STAMP_CAPTURE_PROCESSING_MAX_SIDE) {
        return copiedBitmap
    }
    val scale = STAMP_CAPTURE_PROCESSING_MAX_SIDE.toFloat() / currentMaxSide.toFloat()
    val targetWidth = (copiedBitmap.width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (copiedBitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(copiedBitmap, targetWidth, targetHeight, true).also { scaled ->
        if (scaled !== copiedBitmap && !copiedBitmap.isRecycled) {
            copiedBitmap.recycle()
        }
    }
}

private fun buildPreviewSnapshotFromCapturedSource(
    capturedSourceBitmap: Bitmap,
    previewViewportSize: IntSize
): Bitmap? {
    if (capturedSourceBitmap.isRecycled) return null
    val viewportWidth = previewViewportSize.width.coerceAtLeast(1)
    val viewportHeight = previewViewportSize.height.coerceAtLeast(1)
    val viewportLongSide = max(viewportWidth, viewportHeight).coerceAtLeast(1)
    val scale = (STAMP_PREVIEW_MAX_LONG_SIDE.toFloat() / viewportLongSide.toFloat())
        .coerceAtMost(1f)
    val targetWidth = (viewportWidth * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (viewportHeight * scale).roundToInt().coerceAtLeast(1)
    val visibleSourceRect = resolveCenterCropVisibleSourceRect(
        sourceWidth = capturedSourceBitmap.width.toFloat(),
        sourceHeight = capturedSourceBitmap.height.toFloat(),
        viewportWidth = targetWidth.toFloat(),
        viewportHeight = targetHeight.toFloat()
    )
    return runCatching {
        Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = android.graphics.Canvas(bitmap)
            drawSourceViewportToCanvas(
                canvas = canvas,
                sourceBitmap = capturedSourceBitmap,
                sourceViewport = visibleSourceRect,
                destination = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
            )
        }
    }.getOrNull()
}

private fun requestStampAutoFocus(
    camera: androidx.camera.core.Camera,
    previewView: PreviewView,
    template: StampPunchTemplate,
    overlayWidthFraction: Float,
    overlayAspectRatio: Float,
    overlayCenterYOffsetPx: Float
): Boolean {
    val previewWidth = previewView.width
    val previewHeight = previewView.height
    if (previewWidth <= 0 || previewHeight <= 0) return false
    val focusPoint = resolveStampHoleCenterPointInPreview(
        previewWidth = previewWidth,
        previewHeight = previewHeight,
        template = template,
        overlayWidthFraction = overlayWidthFraction,
        overlayAspectRatio = overlayAspectRatio,
        overlayCenterYOffsetPx = overlayCenterYOffsetPx
    )
    val meteringPointFactory = SurfaceOrientedMeteringPointFactory(
        previewWidth.toFloat(),
        previewHeight.toFloat()
    )
    val focusPointPrimary = meteringPointFactory.createPoint(focusPoint.x, focusPoint.y, 0.24f)
    val focusPointCenter = meteringPointFactory.createPoint(
        previewWidth * 0.5f,
        previewHeight * 0.5f,
        0.18f
    )
    val action = FocusMeteringAction.Builder(
        focusPointPrimary,
        FocusMeteringAction.FLAG_AF or
            FocusMeteringAction.FLAG_AE or
            FocusMeteringAction.FLAG_AWB
    )
        .addPoint(
            focusPointCenter,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )
        .setAutoCancelDuration(2, TimeUnit.SECONDS)
        .build()
    return runCatching {
        camera.cameraControl.startFocusAndMetering(action)
        true
    }.getOrDefault(false)
}

private fun resolveStampHoleCenterPointInPreview(
    previewWidth: Int,
    previewHeight: Int,
    template: StampPunchTemplate,
    overlayWidthFraction: Float,
    overlayAspectRatio: Float,
    overlayCenterYOffsetPx: Float
): Offset {
    val safePreviewWidth = previewWidth.coerceAtLeast(1).toFloat()
    val safePreviewHeight = previewHeight.coerceAtLeast(1).toFloat()
    val safeOverlayAspect = overlayAspectRatio.coerceAtLeast(0.3f)
    val overlayWidth = safePreviewWidth * overlayWidthFraction.coerceIn(0.2f, 1.6f)
    val overlayHeight = overlayWidth / safeOverlayAspect
    val overlayLeft = (safePreviewWidth - overlayWidth) * 0.5f
    val overlayTop = (safePreviewHeight - overlayHeight) * 0.5f + overlayCenterYOffsetPx
    val holeCenterXFraction = (
        (template.holeBounds.left + template.holeBounds.right) * 0.5f /
            template.overlayBitmap.width.toFloat().coerceAtLeast(1f)
        ).coerceIn(0f, 1f)
    val holeCenterYFraction = (
        (template.holeBounds.top + template.holeBounds.bottom) * 0.5f /
            template.overlayBitmap.height.toFloat().coerceAtLeast(1f)
        ).coerceIn(0f, 1f)
    val focusX = (overlayLeft + (overlayWidth * holeCenterXFraction))
        .coerceIn(0f, safePreviewWidth)
    val focusY = (overlayTop + (overlayHeight * holeCenterYFraction))
        .coerceIn(0f, safePreviewHeight)
    return Offset(focusX, focusY)
}

private suspend fun captureStampSource(
    context: Context,
    imageCapture: ImageCapture,
    processingMaxSide: Int = STAMP_CAPTURE_PROCESSING_MAX_SIDE
): StampCapturedSource? {
    val tempFile = File.createTempFile("stamp_capture_", ".jpg", context.cacheDir)
    val capturedFile = suspendCancellableCoroutine<File?> { continuation ->
        continuation.invokeOnCancellation {
            runCatching { tempFile.delete() }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (continuation.isActive) {
                        continuation.resume(tempFile)
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        )
    } ?: run {
        runCatching { tempFile.delete() }
        return null
    }
    val processingBitmap = decodeStampPunchSourceBitmap(
        sourceFile = capturedFile,
        maxSide = processingMaxSide
    ) ?: run {
        runCatching { capturedFile.delete() }
        return null
    }
    return StampCapturedSource(
        processingBitmap = processingBitmap,
        sourceFile = capturedFile
    )
}

private fun mapPreviewViewportToCapturedSource(
    capturedSourceBitmap: Bitmap,
    previewBitmap: Bitmap,
    previewViewport: RectF
): RectF {
    val visibleSourceRect = resolveCenterCropVisibleSourceRect(
        sourceWidth = capturedSourceBitmap.width.toFloat(),
        sourceHeight = capturedSourceBitmap.height.toFloat(),
        viewportWidth = previewBitmap.width.toFloat(),
        viewportHeight = previewBitmap.height.toFloat()
    )
    val scaleX = visibleSourceRect.width() / previewBitmap.width.coerceAtLeast(1).toFloat()
    val scaleY = visibleSourceRect.height() / previewBitmap.height.coerceAtLeast(1).toFloat()
    val left = visibleSourceRect.left + (previewViewport.left * scaleX)
    val top = visibleSourceRect.top + (previewViewport.top * scaleY)
    val right = visibleSourceRect.left + (previewViewport.right * scaleX)
    val bottom = visibleSourceRect.top + (previewViewport.bottom * scaleY)
    return RectF(
        left.coerceIn(0f, capturedSourceBitmap.width - 1f),
        top.coerceIn(0f, capturedSourceBitmap.height - 1f),
        right.coerceIn(left + 1f, capturedSourceBitmap.width.toFloat()),
        bottom.coerceIn(top + 1f, capturedSourceBitmap.height.toFloat())
    )
}

private fun resolveCenterCropVisibleSourceRect(
    sourceWidth: Float,
    sourceHeight: Float,
    viewportWidth: Float,
    viewportHeight: Float
): RectF {
    if (sourceWidth <= 0f || sourceHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) {
        return RectF(0f, 0f, sourceWidth.coerceAtLeast(1f), sourceHeight.coerceAtLeast(1f))
    }
    val sourceAspect = sourceWidth / sourceHeight
    val viewportAspect = viewportWidth / viewportHeight
    return if (sourceAspect > viewportAspect) {
        val visibleWidth = sourceHeight * viewportAspect
        val left = (sourceWidth - visibleWidth) * 0.5f
        RectF(left, 0f, left + visibleWidth, sourceHeight)
    } else {
        val visibleHeight = sourceWidth / viewportAspect
        val top = (sourceHeight - visibleHeight) * 0.5f
        RectF(0f, top, sourceWidth, top + visibleHeight)
    }
}

private fun Modifier.stampPaperGridBackground(
    alpha: Float,
    backgroundColor: Color = Color(0xFFF6F2E7),
    lineColor: Color = Color(0xFFE1DAC9)
): Modifier = this
    .background(backgroundColor.copy(alpha = alpha))
    .drawBehind {
        val resolvedLine = lineColor.copy(alpha = alpha * 0.44f)
        val step = 54.dp.toPx().coerceAtLeast(8f)
        var x = 0f
        while (x <= size.width) {
            drawLine(resolvedLine, Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(resolvedLine, Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
    }

private suspend fun renderStampOnly(
    sourceBitmap: Bitmap,
    template: StampPunchTemplate?,
    mirrorSource: Boolean = false,
    sourceViewport: RectF? = null
): Bitmap? {
    if (sourceBitmap.isRecycled) {
        return null
    }
    if (template == null) {
        sourceBitmap.takeIf { !it.isRecycled }?.recycle()
        return null
    }
    return try {
        val working = withContext(Dispatchers.Default) {
            if (mirrorSource) flipColorCardCameraBitmapHorizontally(sourceBitmap) else sourceBitmap
        }
        if (working.isRecycled) {
            return null
        }
        withContext(Dispatchers.Default) {
            renderStampPunchBitmap(
                source = working,
                template = template,
                sourceViewportOverride = sourceViewport
            )
        }
    } catch (e: Throwable) {
        android.util.Log.e("StampCamera", "renderStampOnly failed", e)
        sourceBitmap.takeIf { !it.isRecycled }?.recycle()
        null
    }
}

private fun extractTemplateHoleMaskBitmap(
    template: StampPunchTemplate
): Bitmap? {
    val holeWidth = template.holeBounds.width().roundToInt().coerceAtLeast(1)
    val holeHeight = template.holeBounds.height().roundToInt().coerceAtLeast(1)
    val mask = createScaledStampShapeMask(
        template = template,
        targetWidth = holeWidth,
        targetHeight = holeHeight
    ) ?: return null
    if (template.style == StampPunchStyle.Three) {
        hardenCircularStampMaskInPlace(mask)
    }
    return mask
}

private fun buildStampPunchCutFrames(
    sourceBitmap: Bitmap,
    template: StampPunchTemplate,
    sourceViewport: RectF
): StampPunchCutFrames? {
    if (sourceBitmap.isRecycled || template.holeMaskBitmap.isRecycled) {
        return null
    }
    val sourceWidth = sourceBitmap.width.coerceAtLeast(1)
    val sourceHeight = sourceBitmap.height.coerceAtLeast(1)
    val safeViewport = RectF(sourceViewport).apply {
        left = left.coerceIn(0f, sourceWidth.toFloat())
        top = top.coerceIn(0f, sourceHeight.toFloat())
        right = right.coerceIn(left + 1f, sourceWidth.toFloat())
        bottom = bottom.coerceIn(top + 1f, sourceHeight.toFloat())
    }
    val targetWidth = safeViewport.width().roundToInt().coerceAtLeast(1)
    val targetHeight = safeViewport.height().roundToInt().coerceAtLeast(1)
    val scaledMask = createScaledStampShapeMask(
        template = template,
        targetWidth = targetWidth,
        targetHeight = targetHeight
    ) ?: return null
    if (scaledMask.isRecycled) return null
    if (template.style == StampPunchStyle.Three) {
        hardenCircularStampMaskInPlace(scaledMask)
    }
    return try {
        val punchLeft = safeViewport.left.roundToInt()
            .coerceIn(0, (sourceWidth - targetWidth).coerceAtLeast(0))
        val punchTop = safeViewport.top.roundToInt()
            .coerceIn(0, (sourceHeight - targetHeight).coerceAtLeast(0))
        val punchedBackdrop = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        android.graphics.Canvas(punchedBackdrop).drawBitmap(
            scaledMask,
            punchLeft.toFloat(),
            punchTop.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                isFilterBitmap = true
            }
        )
        val liftedStamp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val liftedCanvas = android.graphics.Canvas(liftedStamp)
        drawSourceViewportToCanvas(
            canvas = liftedCanvas,
            sourceBitmap = sourceBitmap,
            sourceViewport = safeViewport,
            destination = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        )
        liftedCanvas.drawBitmap(
            scaledMask,
            0f,
            0f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                isFilterBitmap = true
            }
        )
        clearFullyTransparentPixelsInPlace(liftedStamp)
        StampPunchCutFrames(
            punchedBackdrop = punchedBackdrop,
            liftedStamp = liftedStamp
        )
    } finally {
        if (!scaledMask.isRecycled) {
            scaledMask.recycle()
        }
    }
}

private suspend fun buildAutoCutoutStampFromViewport(
    sourceBitmap: Bitmap,
    sourceViewport: RectF,
    context: Context
): Bitmap? = withContext(Dispatchers.Default) {
    if (sourceBitmap.isRecycled) return@withContext null
    val sourceWidth = sourceBitmap.width.coerceAtLeast(1)
    val sourceHeight = sourceBitmap.height.coerceAtLeast(1)
    val safeViewport = clampViewportToSource(
        viewport = RectF(sourceViewport),
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight
    )
    val viewportWidth = safeViewport.width().roundToInt().coerceAtLeast(1)
    val viewportHeight = safeViewport.height().roundToInt().coerceAtLeast(1)
    val viewportLongSide = max(viewportWidth, viewportHeight).coerceAtLeast(1)
    val viewportScale = (
        STAMP_AUTO_CUTOUT_VIEWPORT_MAX_LONG_SIDE.toFloat() / viewportLongSide.toFloat()
    ).coerceAtMost(1f)
    val targetWidth = (viewportWidth * viewportScale).roundToInt().coerceAtLeast(1)
    val targetHeight = (viewportHeight * viewportScale).roundToInt().coerceAtLeast(1)
    val viewportBitmap = runCatching {
        Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    }.getOrNull() ?: return@withContext null
    drawSourceViewportToCanvas(
        canvas = android.graphics.Canvas(viewportBitmap),
        sourceBitmap = sourceBitmap,
        sourceViewport = safeViewport,
        destination = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
    )
    val foregroundBitmap = generateStampAutoCutoutForegroundBitmap(
        context = context,
        source = viewportBitmap
    )
    if (foregroundBitmap == null || foregroundBitmap.isRecycled) {
        if (!viewportBitmap.isRecycled) {
            viewportBitmap.recycle()
        }
        return@withContext null
    }
    if (foregroundBitmap !== viewportBitmap && !viewportBitmap.isRecycled) {
        viewportBitmap.recycle()
    }
    clearFullyTransparentPixelsInPlace(foregroundBitmap)
    return@withContext foregroundBitmap
}

private suspend fun buildFrameCutoutDisplayBitmap(
    context: Context,
    cutoutBitmap: Bitmap
): Bitmap? = withContext(Dispatchers.Default) {
    renderStampCameraCutoutPreviewBitmap(
        context = context,
        foreground = cutoutBitmap
    )
}

private fun drawSourceViewportToCanvas(
    canvas: android.graphics.Canvas,
    sourceBitmap: Bitmap,
    sourceViewport: RectF,
    destination: RectF
) {
    val shader = BitmapShader(sourceBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val scale = max(
        destination.width() / sourceViewport.width().coerceAtLeast(1f),
        destination.height() / sourceViewport.height().coerceAtLeast(1f)
    )
    val dx = destination.left - (sourceViewport.left * scale) +
        ((destination.width() - sourceViewport.width() * scale) * 0.5f)
    val dy = destination.top - (sourceViewport.top * scale) +
        ((destination.height() - sourceViewport.height() * scale) * 0.5f)
    shader.setLocalMatrix(Matrix().apply {
        setScale(scale, scale)
        postTranslate(dx, dy)
    })
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
        isFilterBitmap = true
        isDither = true
    }
    canvas.drawRect(destination, paint)
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float {
    val clamped = fraction.coerceIn(0f, 1f)
    return start + (end - start) * clamped
}

private fun buildPreviewHoleViewport(
    previewBitmap: Bitmap,
    template: StampPunchTemplate,
    overlayWidthFraction: Float,
    overlayAspectRatio: Float,
    overlayCenterYOffsetPx: Float
): RectF {
    val overlayWidth = previewBitmap.width * overlayWidthFraction
    val overlayHeight = overlayWidth / overlayAspectRatio
    val overlayLeft = (previewBitmap.width - overlayWidth) * 0.5f
    val overlayTop = (previewBitmap.height - overlayHeight) * 0.5f + overlayCenterYOffsetPx
    val holeLeftFraction = template.holeBounds.left / template.overlayBitmap.width.toFloat()
    val holeTopFraction = template.holeBounds.top / template.overlayBitmap.height.toFloat()
    val holeRightFraction = template.holeBounds.right / template.overlayBitmap.width.toFloat()
    val holeBottomFraction = template.holeBounds.bottom / template.overlayBitmap.height.toFloat()
    val rawHoleViewport = RectF(
        overlayLeft + overlayWidth * holeLeftFraction,
        overlayTop + overlayHeight * holeTopFraction,
        overlayLeft + overlayWidth * holeRightFraction,
        overlayTop + overlayHeight * holeBottomFraction
    )
    return applyStampHoleViewportOpticalInset(
        viewport = rawHoleViewport,
        style = template.style,
        sourceWidth = previewBitmap.width,
        sourceHeight = previewBitmap.height
    )
}

private fun buildPreviewFrameViewport(
    previewBitmap: Bitmap,
    overlayWidthFraction: Float,
    overlayAspectRatio: Float,
    overlayCenterYOffsetPx: Float
): RectF {
    val overlayWidth = previewBitmap.width * overlayWidthFraction
    val overlayHeight = overlayWidth / overlayAspectRatio.coerceAtLeast(0.2f)
    val rawOverlayRect = RectF(
        (previewBitmap.width - overlayWidth) * 0.5f,
        (previewBitmap.height - overlayHeight) * 0.5f + overlayCenterYOffsetPx,
        (previewBitmap.width + overlayWidth) * 0.5f,
        (previewBitmap.height + overlayHeight) * 0.5f + overlayCenterYOffsetPx
    )
    return clampViewportToSource(
        viewport = rawOverlayRect,
        sourceWidth = previewBitmap.width,
        sourceHeight = previewBitmap.height
    )
}

private fun clampViewportToSource(
    viewport: RectF,
    sourceWidth: Int,
    sourceHeight: Int
): RectF {
    val safeWidth = sourceWidth.coerceAtLeast(1).toFloat()
    val safeHeight = sourceHeight.coerceAtLeast(1).toFloat()
    val left = viewport.left.coerceIn(0f, safeWidth - 1f)
    val top = viewport.top.coerceIn(0f, safeHeight - 1f)
    val right = viewport.right.coerceIn(left + 1f, safeWidth)
    val bottom = viewport.bottom.coerceIn(top + 1f, safeHeight)
    return RectF(left, top, right, bottom)
}

private fun applyStampHoleViewportOpticalInset(
    viewport: RectF,
    style: StampPunchStyle,
    sourceWidth: Int,
    sourceHeight: Int
): RectF {
    val insetFraction = resolveStampHoleViewportInsetFraction(style)
    if (insetFraction <= 0f) return RectF(viewport)
    val width = viewport.width().coerceAtLeast(1f)
    val height = viewport.height().coerceAtLeast(1f)
    val insetX = width * insetFraction
    val insetY = height * insetFraction
    val safeWidth = sourceWidth.coerceAtLeast(1).toFloat()
    val safeHeight = sourceHeight.coerceAtLeast(1).toFloat()
    val left = (viewport.left + insetX).coerceIn(0f, safeWidth - 1f)
    val top = (viewport.top + insetY).coerceIn(0f, safeHeight - 1f)
    val right = (viewport.right - insetX).coerceIn(left + 1f, safeWidth)
    val bottom = (viewport.bottom - insetY).coerceIn(top + 1f, safeHeight)
    return RectF(left, top, right, bottom)
}

private fun resolveStampHoleViewportInsetFraction(
    style: StampPunchStyle
): Float = when (style) {
    StampPunchStyle.Se -> 0.03f
    StampPunchStyle.One -> 0.012f
    StampPunchStyle.Two -> 0.014f
    StampPunchStyle.Three -> 0.0f
    StampPunchStyle.FrameCutout -> 0.0f
    StampPunchStyle.Four -> 0.010f
}

private fun resolveTemplateHoleCenterYFraction(template: StampPunchTemplate): Float {
    val overlayHeight = template.overlayBitmap.height.toFloat().coerceAtLeast(1f)
    return ((template.holeBounds.top + template.holeBounds.bottom) * 0.5f / overlayHeight)
        .coerceIn(0f, 1f)
}

private fun resolveOverlayDisplayAspectRatio(
    template: StampPunchTemplate?,
    fallbackAspect: Float
): Float {
    val overlay = template?.overlayBitmap?.takeUnless { it.isRecycled }
    val bitmapAspect = overlay?.let { bmp ->
        (bmp.width.toFloat() / bmp.height.toFloat()).takeIf { it.isFinite() && it > 0f }
    }
    return bitmapAspect ?: fallbackAspect
}

private fun buildImportBackdropSnapshot(
    sourceBitmap: Bitmap,
    containerSize: IntSize,
    displayRect: RectF
): StampImportBackdropSnapshot? {
    if (
        sourceBitmap.isRecycled ||
        containerSize.width <= 0 ||
        containerSize.height <= 0 ||
        displayRect.width() <= 1f ||
        displayRect.height() <= 1f
    ) {
        return null
    }
    val backdropBitmap = runCatching {
        Bitmap.createBitmap(
            containerSize.width.coerceAtLeast(1),
            containerSize.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
    }.getOrNull() ?: return null
    val canvas = android.graphics.Canvas(backdropBitmap)
    canvas.drawColor(android.graphics.Color.BLACK)
    canvas.drawBitmap(
        sourceBitmap,
        null,
        displayRect,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
    )
    return StampImportBackdropSnapshot(
        backdropBitmap = backdropBitmap,
        imageRect = RectF(displayRect)
    )
}

private fun mapImportSourceViewportToBackdrop(
    sourceViewport: RectF,
    sourceBitmap: Bitmap,
    imageRect: RectF
): RectF {
    if (sourceBitmap.isRecycled || imageRect.width() <= 0f || imageRect.height() <= 0f) {
        return RectF()
    }
    val sourceWidth = sourceBitmap.width.coerceAtLeast(1).toFloat()
    val sourceHeight = sourceBitmap.height.coerceAtLeast(1).toFloat()
    val safeViewport = RectF(sourceViewport).apply {
        left = left.coerceIn(0f, sourceWidth - 1f)
        top = top.coerceIn(0f, sourceHeight - 1f)
        right = right.coerceIn(left + 1f, sourceWidth)
        bottom = bottom.coerceIn(top + 1f, sourceHeight)
    }
    val scaleX = imageRect.width() / sourceWidth
    val scaleY = imageRect.height() / sourceHeight
    val left = (imageRect.left + safeViewport.left * scaleX).coerceIn(imageRect.left, imageRect.right - 1f)
    val top = (imageRect.top + safeViewport.top * scaleY).coerceIn(imageRect.top, imageRect.bottom - 1f)
    val right = (imageRect.left + safeViewport.right * scaleX).coerceIn(left + 1f, imageRect.right)
    val bottom = (imageRect.top + safeViewport.bottom * scaleY).coerceIn(top + 1f, imageRect.bottom)
    return RectF(left, top, right, bottom)
}

private fun resolveImportImageDisplayRect(
    sourceWidth: Int,
    sourceHeight: Int,
    containerWidth: Float,
    containerHeight: Float
): RectF {
    if (sourceWidth <= 0 || sourceHeight <= 0 || containerWidth <= 0f || containerHeight <= 0f) {
        return RectF()
    }
    val imageAspect = sourceWidth.toFloat() / sourceHeight.toFloat()
    val containerAspect = containerWidth / containerHeight
    return if (imageAspect > containerAspect) {
        val displayedWidth = containerWidth
        val displayedHeight = displayedWidth / imageAspect
        val top = (containerHeight - displayedHeight) * 0.5f
        RectF(0f, top, displayedWidth, top + displayedHeight)
    } else {
        val displayedHeight = containerHeight
        val displayedWidth = displayedHeight * imageAspect
        val left = (containerWidth - displayedWidth) * 0.5f
        RectF(left, 0f, left + displayedWidth, displayedHeight)
    }
}

private fun scaleImportImageDisplayRect(
    imageRect: RectF,
    scale: Float
): RectF {
    if (imageRect.width() <= 0f || imageRect.height() <= 0f) {
        return RectF()
    }
    val resolvedScale = scale.coerceAtLeast(0.01f)
    val centerX = (imageRect.left + imageRect.right) * 0.5f
    val centerY = (imageRect.top + imageRect.bottom) * 0.5f
    val scaledWidth = imageRect.width() * resolvedScale
    val scaledHeight = imageRect.height() * resolvedScale
    return RectF(
        centerX - (scaledWidth * 0.5f),
        centerY - (scaledHeight * 0.5f),
        centerX + (scaledWidth * 0.5f),
        centerY + (scaledHeight * 0.5f)
    )
}

private fun resolveImportOverlayWidthPx(
    imageRect: RectF,
    containerWidth: Float,
    style: StampPunchStyle
): Float {
    val referenceWidth = if (style == StampPunchStyle.Se) {
        minOf(imageRect.width(), containerWidth) * 0.96f
    } else {
        minOf(imageRect.width(), containerWidth) * 0.9f
    }
    val maxWidthCoverage = if (style == StampPunchStyle.Se) 1.18f else 0.985f
    return (referenceWidth * style.normalizedWidthScale * resolveStampPunchVisualWidthScale(style))
        .coerceAtLeast(180f)
        .coerceAtMost(imageRect.width() * maxWidthCoverage)
}

private fun resolveImportSourceScaleBounds(
    imageRect: RectF,
    overlayWidth: Float,
    overlayHeight: Float,
    template: StampPunchTemplate
): ClosedFloatingPointRange<Float> {
    val sourceWidth = imageRect.width().coerceAtLeast(1f)
    val sourceHeight = imageRect.height().coerceAtLeast(1f)
    val holeWidthFraction = template.holeBounds.width() /
        template.overlayBitmap.width.toFloat().coerceAtLeast(1f)
    val holeHeightFraction = template.holeBounds.height() /
        template.overlayBitmap.height.toFloat().coerceAtLeast(1f)
    val minScale = max(
        (overlayWidth * holeWidthFraction) / sourceWidth,
        (overlayHeight * holeHeightFraction) / sourceHeight
    ).coerceIn(0.46f, 1.35f)
    val maxScale = max(2.8f, minScale + 0.01f)
    return minScale..maxScale
}

private fun resolveImportSourceScaleBoundsForFrame(
    imageRect: RectF,
    overlayWidth: Float,
    overlayHeight: Float
): ClosedFloatingPointRange<Float> {
    val sourceWidth = imageRect.width().coerceAtLeast(1f)
    val sourceHeight = imageRect.height().coerceAtLeast(1f)
    val minScale = max(
        overlayWidth / sourceWidth,
        overlayHeight / sourceHeight
    ).coerceIn(0.46f, 1.35f)
    val maxScale = max(2.8f, minScale + 0.01f)
    return minScale..maxScale
}

private fun clampImportPunchCenter(
    desiredCenter: Offset,
    imageRect: RectF,
    overlayWidth: Float,
    overlayHeight: Float,
    template: StampPunchTemplate
): Offset {
    val holeLeftFraction = template.holeBounds.left / template.overlayBitmap.width.toFloat()
    val holeTopFraction = template.holeBounds.top / template.overlayBitmap.height.toFloat()
    val holeRightFraction = template.holeBounds.right / template.overlayBitmap.width.toFloat()
    val holeBottomFraction = template.holeBounds.bottom / template.overlayBitmap.height.toFloat()

    val minCenterX = imageRect.left + overlayWidth * (0.5f - holeLeftFraction)
    val maxCenterX = imageRect.right - overlayWidth * (holeRightFraction - 0.5f)
    val minCenterY = imageRect.top + overlayHeight * (0.5f - holeTopFraction)
    val maxCenterY = imageRect.bottom - overlayHeight * (holeBottomFraction - 0.5f)

    val resolvedCenterX = if (minCenterX <= maxCenterX) {
        desiredCenter.x.coerceIn(minCenterX, maxCenterX)
    } else {
        (imageRect.left + imageRect.right) * 0.5f
    }
    val resolvedCenterY = if (minCenterY <= maxCenterY) {
        desiredCenter.y.coerceIn(minCenterY, maxCenterY)
    } else {
        (imageRect.top + imageRect.bottom) * 0.5f
    }
    return Offset(resolvedCenterX, resolvedCenterY)
}

private fun clampImportFrameCenter(
    desiredCenter: Offset,
    imageRect: RectF,
    overlayWidth: Float,
    overlayHeight: Float
): Offset {
    val halfWidth = overlayWidth * 0.5f
    val halfHeight = overlayHeight * 0.5f
    val minCenterX = imageRect.left + halfWidth
    val maxCenterX = imageRect.right - halfWidth
    val minCenterY = imageRect.top + halfHeight
    val maxCenterY = imageRect.bottom - halfHeight
    val resolvedCenterX = if (minCenterX <= maxCenterX) {
        desiredCenter.x.coerceIn(minCenterX, maxCenterX)
    } else {
        (imageRect.left + imageRect.right) * 0.5f
    }
    val resolvedCenterY = if (minCenterY <= maxCenterY) {
        desiredCenter.y.coerceIn(minCenterY, maxCenterY)
    } else {
        (imageRect.top + imageRect.bottom) * 0.5f
    }
    return Offset(resolvedCenterX, resolvedCenterY)
}

private fun buildImportPunchViewport(
    sourceBitmap: Bitmap,
    imageRect: RectF,
    overlayRect: RectF,
    template: StampPunchTemplate
): RectF {
    val holeLeftFraction = template.holeBounds.left / template.overlayBitmap.width.toFloat()
    val holeTopFraction = template.holeBounds.top / template.overlayBitmap.height.toFloat()
    val holeRightFraction = template.holeBounds.right / template.overlayBitmap.width.toFloat()
    val holeBottomFraction = template.holeBounds.bottom / template.overlayBitmap.height.toFloat()
    val holeRect = RectF(
        overlayRect.left + overlayRect.width() * holeLeftFraction,
        overlayRect.top + overlayRect.height() * holeTopFraction,
        overlayRect.left + overlayRect.width() * holeRightFraction,
        overlayRect.top + overlayRect.height() * holeBottomFraction
    )
    val scaleX = sourceBitmap.width / imageRect.width().coerceAtLeast(1f)
    val scaleY = sourceBitmap.height / imageRect.height().coerceAtLeast(1f)
    val left = ((holeRect.left - imageRect.left) * scaleX).coerceIn(0f, sourceBitmap.width - 1f)
    val top = ((holeRect.top - imageRect.top) * scaleY).coerceIn(0f, sourceBitmap.height - 1f)
    val right = ((holeRect.right - imageRect.left) * scaleX).coerceIn(left + 1f, sourceBitmap.width.toFloat())
    val bottom = ((holeRect.bottom - imageRect.top) * scaleY).coerceIn(top + 1f, sourceBitmap.height.toFloat())
    return applyStampHoleViewportOpticalInset(
        viewport = RectF(left, top, right, bottom),
        style = template.style,
        sourceWidth = sourceBitmap.width,
        sourceHeight = sourceBitmap.height
    )
}

private fun buildImportFrameViewport(
    sourceBitmap: Bitmap,
    imageRect: RectF,
    overlayRect: RectF
): RectF {
    if (sourceBitmap.isRecycled || imageRect.width() <= 0f || imageRect.height() <= 0f) {
        return RectF()
    }
    val scaleX = sourceBitmap.width / imageRect.width().coerceAtLeast(1f)
    val scaleY = sourceBitmap.height / imageRect.height().coerceAtLeast(1f)
    val left = ((overlayRect.left - imageRect.left) * scaleX).coerceIn(0f, sourceBitmap.width - 1f)
    val top = ((overlayRect.top - imageRect.top) * scaleY).coerceIn(0f, sourceBitmap.height - 1f)
    val right = ((overlayRect.right - imageRect.left) * scaleX).coerceIn(left + 1f, sourceBitmap.width.toFloat())
    val bottom = ((overlayRect.bottom - imageRect.top) * scaleY).coerceIn(top + 1f, sourceBitmap.height.toFloat())
    return clampViewportToSource(
        viewport = RectF(left, top, right, bottom),
        sourceWidth = sourceBitmap.width,
        sourceHeight = sourceBitmap.height
    )
}

private fun resolveStampDisplayWidthFraction(
    style: StampPunchStyle,
    template: StampPunchTemplate?
): Float {
    val holeWidthFraction = template?.let {
        (it.holeBounds.width() / it.overlayBitmap.width.toFloat())
            .coerceIn(0.2f, 1f)
    } ?: 1f
    val insetScale = (1f - (resolveStampHoleViewportInsetFraction(style) * 2f))
        .coerceIn(0.62f, 1f)
    val resolvedFraction = (resolveCameraOverlayWidthFraction(style) * holeWidthFraction * insetScale)
        .coerceIn(0.2f, 0.96f)
    return if (style.isFrameCutoutMode) {
        resolveFrameCutoutResultDisplayWidthFraction(resolvedFraction)
    } else {
        resolvedFraction
    }
}

private fun resolveFrameCutoutResultDisplayWidthFraction(
    viewportWidthFraction: Float
): Float {
    return (viewportWidthFraction * 0.72f).coerceIn(0.42f, 0.66f)
}

private fun resolveCameraOverlayWidthFraction(style: StampPunchStyle): Float {
    val maxWidthFraction = if (style == StampPunchStyle.Se) {
        STAMP_PUNCH_OVERLAY_SE_MAX_WIDTH_FRACTION
    } else {
        STAMP_PUNCH_OVERLAY_MAX_WIDTH_FRACTION
    }
    return (
        STAMP_PUNCH_OVERLAY_BASE_WIDTH_FRACTION *
            style.normalizedWidthScale *
            resolveStampPunchVisualWidthScale(style)
        )
        .coerceAtMost(maxWidthFraction)
}

private fun resolveStampPunchVisualWidthScale(style: StampPunchStyle): Float = when (style) {
    StampPunchStyle.Se -> 0.72f
    StampPunchStyle.One -> 0.94f
    StampPunchStyle.Two -> 0.93f
    StampPunchStyle.Three -> 0.93f
    StampPunchStyle.FrameCutout -> 0.78f
    StampPunchStyle.Four -> 0.95f
}

private fun resolveCameraOverlayCenterYOffset(style: StampPunchStyle): Dp {
    val styleSpecificDownwardOffset = when (style) {
        StampPunchStyle.Se -> 18.dp
        StampPunchStyle.One -> 8.dp
        StampPunchStyle.Two -> 14.dp
        StampPunchStyle.Three -> 10.dp
        StampPunchStyle.FrameCutout -> 9.dp
        StampPunchStyle.Four -> 9.dp
    }
    return when (style) {
        StampPunchStyle.Two ->
            STAMP_PUNCH_OVERLAY_CENTER_Y_OFFSET +
                STAMP_PUNCH_OVERLAY_SQUARE_EXTRA_DOWN_OFFSET +
                styleSpecificDownwardOffset
        else -> STAMP_PUNCH_OVERLAY_CENTER_Y_OFFSET + styleSpecificDownwardOffset
    }
}

private data class StampCameraSelectorCandidate(
    val selector: CameraSelector,
    val usingFrontCamera: Boolean
)

private fun buildStampCameraSelectorCandidates(
    provider: ProcessCameraProvider,
    preferFrontCamera: Boolean
): List<StampCameraSelectorCandidate> {
    val resolved = mutableListOf<StampCameraSelectorCandidate>()

    fun appendCandidate(selector: CameraSelector, usingFrontCamera: Boolean) {
        val available = runCatching { provider.hasCamera(selector) }.getOrDefault(false)
        if (!available) return
        if (resolved.any { it.usingFrontCamera == usingFrontCamera }) return
        resolved += StampCameraSelectorCandidate(
            selector = selector,
            usingFrontCamera = usingFrontCamera
        )
    }

    if (preferFrontCamera) {
        appendCandidate(
            selector = CameraSelector.DEFAULT_FRONT_CAMERA,
            usingFrontCamera = true
        )
        appendCandidate(
            selector = CameraSelector.DEFAULT_BACK_CAMERA,
            usingFrontCamera = false
        )
    } else {
        appendCandidate(
            selector = CameraSelector.DEFAULT_BACK_CAMERA,
            usingFrontCamera = false
        )
        appendCandidate(
            selector = CameraSelector.DEFAULT_FRONT_CAMERA,
            usingFrontCamera = true
        )
    }

    if (resolved.isNotEmpty()) {
        return resolved
    }
    return listOf(
        StampCameraSelectorCandidate(
            selector = if (preferFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            },
            usingFrontCamera = preferFrontCamera
        )
    )
}

private fun normalizedStampCameraAspect(size: AndroidSize): Float {
    val longSide = max(size.width, size.height).toFloat().coerceAtLeast(1f)
    val shortSide = min(size.width, size.height).toFloat().coerceAtLeast(1f)
    return longSide / shortSide
}

private fun buildStampCameraPreviewTargetSize(viewportSize: IntSize): AndroidSize {
    val safeWidth = viewportSize.width.coerceAtLeast(1)
    val safeHeight = viewportSize.height.coerceAtLeast(1)
    val shortSide = min(safeWidth, safeHeight)
        .coerceAtLeast(720)
        .coerceAtMost(1600)
    val viewportAspect = max(safeWidth, safeHeight).toFloat() /
        min(safeWidth, safeHeight).coerceAtLeast(1).toFloat()
    val longSide = (shortSide * viewportAspect)
        .roundToInt()
        .coerceIn(shortSide + 1, STAMP_PREVIEW_MAX_LONG_SIDE)
    return if (safeWidth >= safeHeight) {
        AndroidSize(longSide, shortSide)
    } else {
        AndroidSize(shortSide, longSide)
    }
}

private fun buildStampCameraPreviewResolutionSelector(targetSize: AndroidSize): ResolutionSelector {
    val targetAspect = normalizedStampCameraAspect(targetSize)
    return ResolutionSelector.Builder()
        .setResolutionFilter { supportedSizes, _ ->
            val candidates = supportedSizes
                .filter { min(it.width, it.height) >= 480 }
                .ifEmpty { supportedSizes }
            candidates.sortedWith(
                compareBy<AndroidSize> { size ->
                    abs(normalizedStampCameraAspect(size) - targetAspect)
                }.thenByDescending { size ->
                    min(size.width, size.height)
                }.thenByDescending { size ->
                    max(size.width, size.height)
                }
            )
        }
        .setResolutionStrategy(
            ResolutionStrategy(
                targetSize,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()
}

private fun buildStampCameraCaptureResolutionSelector(
    frameCutoutMode: Boolean = false
): ResolutionSelector {
    if (frameCutoutMode) {
        val targetSize = AndroidSize(1440, 1920)
        return ResolutionSelector.Builder()
            .setAllowedResolutionMode(
                ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
            )
            .setResolutionFilter { supportedSizes, _ ->
                supportedSizes.sortedWith(
                    compareBy<AndroidSize> { size ->
                        abs(normalizedStampCameraAspect(size) - normalizedStampCameraAspect(targetSize))
                    }.thenBy { size ->
                        abs(max(size.width, size.height) - max(targetSize.width, targetSize.height))
                    }.thenBy { size ->
                        size.width.toLong() * size.height.toLong()
                    }
                )
            }
            .setResolutionStrategy(
                ResolutionStrategy(
                    targetSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
    }
    return ResolutionSelector.Builder()
        .setAllowedResolutionMode(
            ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
        )
        .setResolutionFilter { supportedSizes, _ ->
            supportedSizes.sortedByDescending { size ->
                size.width.toLong() * size.height.toLong()
            }
        }
        .build()
}

private fun loadPreferredStampCameraLens(context: Context): Boolean =
    context.applicationContext
        .getSharedPreferences(STAMP_CAMERA_PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(STAMP_CAMERA_PREF_USE_FRONT_CAMERA, false)

private fun loadPreferredStampCameraPunchStyle(
    context: Context,
    styleOptions: List<StampPunchStyle>
): String {
    val preferred = context.applicationContext
        .getSharedPreferences(STAMP_CAMERA_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(STAMP_CAMERA_PREF_PUNCH_STYLE_KEY, null)
        ?.trim()
        .orEmpty()
    return preferred
        .takeIf { key -> styleOptions.any { it.key == key } }
        ?: StampPunchStyle.One.key
}

private fun savePreferredStampCameraLens(context: Context, useFrontCamera: Boolean) {
    context.applicationContext
        .getSharedPreferences(STAMP_CAMERA_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(STAMP_CAMERA_PREF_USE_FRONT_CAMERA, useFrontCamera)
        .apply()
}

private fun savePreferredStampCameraPunchStyle(context: Context, styleKey: String) {
    val normalizedKey = StampPunchStyle.fromKey(styleKey).key
    context.applicationContext
        .getSharedPreferences(STAMP_CAMERA_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(STAMP_CAMERA_PREF_PUNCH_STYLE_KEY, normalizedKey)
        .apply()
}

@Composable
private fun rememberStampCameraDeviceRotation(context: Context): Int {
    val appContext = remember(context) { context.applicationContext }
    var deviceRotation by remember { mutableIntStateOf(context.display.rotation) }
    var quantizedOrientationDegrees by remember {
        mutableIntStateOf(stampSurfaceRotationToOrientationDegrees(deviceRotation))
    }
    DisposableEffect(appContext) {
        val listener = object : OrientationEventListener(appContext) {
            override fun onOrientationChanged(orientation: Int) {
                val nextQuantized = quantizeStampOrientationDegrees(
                    orientation = orientation,
                    previousQuantized = quantizedOrientationDegrees
                ) ?: return
                if (nextQuantized != quantizedOrientationDegrees) {
                    quantizedOrientationDegrees = nextQuantized
                }
                val nextRotation = stampOrientationDegreesToSurfaceRotation(nextQuantized)
                if (nextRotation != deviceRotation) {
                    deviceRotation = nextRotation
                }
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose {
            listener.disable()
        }
    }
    return deviceRotation
}

private fun quantizeStampOrientationDegrees(
    orientation: Int,
    previousQuantized: Int
): Int? {
    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return null
    val normalized = ((orientation % 360) + 360) % 360
    val nearest = ((normalized + 45) / 90 * 90) % 360
    val delta = abs(normalized - previousQuantized)
    val minDelta = min(delta, 360 - delta)
    return if (minDelta >= (45 + STAMP_ORIENTATION_HYSTERESIS_DEGREES)) {
        nearest
    } else {
        previousQuantized
    }
}

private fun stampOrientationDegreesToSurfaceRotation(quantizedOrientation: Int): Int {
    return when (((quantizedOrientation % 360) + 360) % 360) {
        90 -> Surface.ROTATION_270
        180 -> Surface.ROTATION_180
        270 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }
}

private fun stampSurfaceRotationToOrientationDegrees(rotation: Int): Int {
    return when (rotation) {
        Surface.ROTATION_90 -> 270
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 90
        else -> 0
    }
}

private fun isStampLandscapeRotation(rotation: Int): Boolean {
    return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
}

private fun shouldUseLandscapeStampResult(
    style: StampPunchStyle,
    deviceRotation: Int,
    configurationLandscape: Boolean,
    sourceWidth: Int,
    sourceHeight: Int
): Boolean {
    if (style != StampPunchStyle.One) return false
    val safeWidth = sourceWidth.coerceAtLeast(1).toFloat()
    val safeHeight = sourceHeight.coerceAtLeast(1).toFloat()
    val sourceLandscape = (safeWidth / safeHeight) >= STAMP_PREVIEW_LANDSCAPE_ASPECT_THRESHOLD
    val votes = (if (isStampLandscapeRotation(deviceRotation)) 1 else 0) +
        (if (configurationLandscape) 1 else 0) +
        (if (sourceLandscape) 1 else 0)
    return votes >= 2
}

private fun ensureStampBitmapLandscape(bitmap: Bitmap): Bitmap {
    if (bitmap.isRecycled || bitmap.width >= bitmap.height) return bitmap
    val rotated = runCatching {
        val matrix = Matrix().apply { postRotate(90f) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrNull() ?: return bitmap
    if (rotated !== bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
    }
    return rotated
}

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider? =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                val provider = runCatching { future.get() }.getOrNull()
                if (continuation.isActive) {
                    continuation.resume(provider)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

private fun getStampCameraCopy(language: AppLanguage): StampCameraCopy = when (language) {
    AppLanguage.SimplifiedChinese -> StampCameraCopy(
        pageTitle = "\u90ae\u7968\u6253\u5b54\u76f8\u673a",
        pageSubtitle = "\u70b9\u51fb\u6253\u5b54\u673a\uff0c\u751f\u6210\u90ae\u7968",
        importPhoto = "\u9009\u62e9\u56fe\u7247",
        importPlacementHint = "\u62d6\u52a8\u6253\u5b54\u5668\u5bf9\u51c6\u4f4d\u7f6e\uff0c\u70b9\u51fb\u5373\u53ef\u6253\u5b54",
        switchCamera = "\u5207\u6362\u6444\u50cf\u5934",
        save = "\u4fdd\u5b58",
        cancel = "\u53d6\u6d88",
        saveToPhoneAlbum = "\u4fdd\u5b58\u5230\u624b\u673a\u76f8\u518c",
        permissionTitle = "\u6253\u5f00\u76f8\u673a\u6743\u9650",
        permissionBody = "\u9700\u8981\u76f8\u673a\u6743\u9650\u624d\u80fd\u5728\u6253\u5b54\u673a\u91cc\u53d6\u666e\u5e76\u751f\u6210\u90ae\u7968\u56fe\u3002",
        permissionAction = "\u5141\u8bb8\u76f8\u673a",
        captureFailed = "\u62cd\u7167\u5931\u8d25",
        renderFailed = "\u90ae\u7968\u751f\u6210\u5931\u8d25",
        saveFailed = "\u90ae\u7968\u4fdd\u5b58\u5931\u8d25",
        loadFailed = "\u56fe\u7247\u8bfb\u53d6\u5931\u8d25",
        savedToCalendar = "\u5df2\u4fdd\u5b58\u5230\u65e5\u5386",
        savedToCalendarAndAlbum = "\u5df2\u4fdd\u5b58\u5230\u65e5\u5386\u548c\u624b\u673a\u76f8\u518c",
        savedToCalendarAlbumFailed = "\u5df2\u4fdd\u5b58\u5230\u65e5\u5386\uff0c\u4f46\u5199\u5165\u624b\u673a\u76f8\u518c\u5931\u8d25"
    )
    AppLanguage.TraditionalChinese -> StampCameraCopy(
        pageTitle = "\u90f5\u7968\u6253\u5b54\u76f8\u6a5f",
        pageSubtitle = "\u9ede\u64ca\u6253\u5b54\u6a5f\uff0c\u751f\u6210\u90f5\u7968",
        importPhoto = "\u9078\u64c7\u5716\u7247",
        importPlacementHint = "\u62d6\u52d5\u6253\u5b54\u5668\u5c0d\u6e96\u4f4d\u7f6e\uff0c\u9ede\u64ca\u5373\u53ef\u6253\u5b54",
        switchCamera = "\u5207\u63db\u651d\u5f71\u6a5f",
        save = "\u5132\u5b58",
        cancel = "\u53d6\u6d88",
        saveToPhoneAlbum = "\u5132\u5b58\u5230\u624b\u6a5f\u76f8\u7c3f",
        permissionTitle = "\u958b\u555f\u76f8\u6a5f\u6b0a\u9650",
        permissionBody = "\u9700\u8981\u76f8\u6a5f\u6b0a\u9650\u624d\u80fd\u5728\u6253\u5b54\u6a5f\u4e2d\u53d6\u666f\u4e26\u751f\u6210\u90f5\u7968\u5716\u3002",
        permissionAction = "\u5141\u8a31\u76f8\u6a5f",
        captureFailed = "\u62cd\u651d\u5931\u6557",
        renderFailed = "\u90f5\u7968\u751f\u6210\u5931\u6557",
        saveFailed = "\u90ae\u7968\u5132\u5b58\u5931\u6557",
        loadFailed = "\u5716\u7247\u8b80\u53d6\u5931\u6557",
        savedToCalendar = "\u5df2\u5132\u5b58\u5230\u65e5\u66c6",
        savedToCalendarAndAlbum = "\u5df2\u5132\u5b58\u5230\u65e5\u66c6\u8207\u624b\u6a5f\u76f8\u7c3f",
        savedToCalendarAlbumFailed = "\u5df2\u5132\u5b58\u5230\u65e5\u66c6\uff0c\u4f46\u5beb\u5165\u624b\u6a5f\u76f8\u7c3f\u5931\u6557"
    )
    AppLanguage.Japanese -> StampCameraCopy(
        pageTitle = "\u30b9\u30bf\u30f3\u30d7\u30ab\u30c3\u30c8\u30ab\u30e1\u30e9",
        pageSubtitle = "\u30bf\u30c3\u30d7\u3059\u308b\u3068\u90f5\u8c61\u304c\u4f5c\u6210\u3055\u308c\u307e\u3059",
        importPhoto = "\u753b\u50cf\u3092\u9078\u3076",
        importPlacementHint = "\u30ab\u30c3\u30c8\u30a2\u30a6\u30c8\u3092\u4f4d\u7f6e\u306b\u30c9\u30e9\u30c3\u30b0\u3057\u3066\u3001\u30bf\u30c3\u30d7\u3057\u3066\u304f\u3060\u3055\u3044",
        switchCamera = "\u30ab\u30e1\u30e9\u5207\u308a\u66ff\u3048",
        save = "\u4fdd\u5b58",
        cancel = "\u30ad\u30e3\u30f3\u30bb\u30eb",
        saveToPhoneAlbum = "\u30db\u30f3\u30b1\u30d5\u30a9\u30f3\u306b\u4fdd\u5b58",
        permissionTitle = "\u30ab\u30e1\u30e9\u30a2\u30af\u30bb\u30b9\u3092\u8a31\u53ef",
        permissionBody = "\u30ab\u30c3\u30c8\u30a2\u30a6\u30c8\u306e\u30d7\u30ec\u30d3\u30e5\u30fc\u3068\u90f5\u8c61\u751f\u6210\u306b\u30ab\u30e1\u30e9\u6a29\u9650\u304c\u5fc5\u8981\u3067\u3059",
        permissionAction = "\u30ab\u30e1\u30e9\u3092\u8a31\u53ef",
        captureFailed = "\u64ae\u5f71\u5931\u6557",
        renderFailed = "\u90f5\u8c61\u306e\u30ec\u30f3\u30c0\u30ea\u30f3\u30b0\u306b\u5931\u6557",
        saveFailed = "\u90f5\u8c61\u306e\u4fdd\u5b58\u306b\u5931\u6557\u3057\u307e\u3057\u305f",
        loadFailed = "\u753b\u50cf\u306e\u8aad\u307f\u8fbc\u307f\u306b\u5931\u6557",
        savedToCalendar = "\u30ab\u30ec\u30f3\u30c0\u30fc\u306b\u4fdd\u5b58\u3057\u307e\u3057\u305f",
        savedToCalendarAndAlbum = "\u30ab\u30ec\u30f3\u30c0\u30fc\u3068\u30db\u30f3\u30b1\u30d5\u30a9\u30f3\u306b\u4fdd\u5b58\u3057\u307e\u3057\u305f",
        savedToCalendarAlbumFailed = "\u30ab\u30ec\u30f3\u30c0\u30fc\u306b\u4fdd\u5b58\u3057\u307e\u3057\u305f\u304c\u3001\u30db\u30f3\u30b1\u30d5\u30a9\u30f3\u306b\u66f8\u304d\u8fbc\u307f\u306b\u5931\u6557\u3057\u307e\u3057\u305f"
    )
    AppLanguage.Korean -> StampCameraCopy(
        pageTitle = "\uc6d0\uc785 \ucef4\ud305 \ucea0\ub77c",
        pageSubtitle = "\ud074\ub9ad\ud55c \ub2f4\uc9c0\ub85c \uc6d0\uc785\uc744 \ub9cc\ub4e4\uc5b4 \ub4dc\ub9bd\ub2c8\ub2e4",
        importPhoto = "\uc774\ubbf8\uc9c0 \uc120\ud0dd",
        importPlacementHint = "\uc6d0\uc785 \ucef4\ud305\uc744 \uc704\uce58\uc5d0 \ub514\ub85c\uadf8\ud558\uace0 \ud074\ub9ad\ud558\uc138\uc694",
        switchCamera = "\ucea4\ub2c8\ub77c \uc0ac\uc7a5",
        save = "\uc800\uc7a5",
        cancel = "\ucde8\uc18c",
        saveToPhoneAlbum = "\ud3f4\ub354\uc5d0 \uc800\uc7a5",
        permissionTitle = "\ucea4\ub2c8\ub77c \uc561\uc138\uc5b4 \ud5c8\uc6a9",
        permissionBody = "\uc6d0\uc785 \ucef4\ud305 \ud504\ub9ac\ubca0\uc774\uc6a9\uc640 \uc6d0\uc785 \uc81c\uc0ac\uc5d0 \ucea4\ub2c8\ub77c \uad8c\ud55c\uc774 \ud544\uc694\ud569\ub2c8\ub2e4",
        permissionAction = "\ucea4\ub2c8\ub77c \ud5c8\uc6a9",
        captureFailed = "\uc0ac\uc9c4 \uc2e4\ud328",
        renderFailed = "\uc6d0\uc785 \ub80c\ub354\ub9c1 \uc2e4\ud328",
        saveFailed = "\uc6d0\uc785 \uc800\uc7a5\uc5d0 \uc2e4\ud328\ud558\uc5c8\uc2b5\ub2c8\ub2e4",
        loadFailed = "\uc774\ubbf8\uc9c0 \ubd88\ub3c4",
        savedToCalendar = "\uce0c\ub140\ub4dc\ub7ec\uc6cc\uc5d0 \uc800\uc7a5\ub418\uc5c8\uc2b5\ub2c8\ub2e4",
        savedToCalendarAndAlbum = "\uce0c\ub140\ub4dc\ub7ec\uc6cc\uc640 \ud3f4\ub354\uc5d0 \uc800\uc7a5\ub418\uc5c8\uc2b5\ub2c8\ub2e4",
        savedToCalendarAlbumFailed = "\uce0c\ub140\ub4dc\ub7ec\uc6cc\uc5d0 \uc800\uc7a5\ub418\uc5c8\uc9c0\ub9cc \ud3f4\ub354\uc5d0 \uc791\uc131\uc5d0 \uc2e4\ud328\ud558\uc5c8\uc2b5\ub2c8\ub2e4"
    )
    AppLanguage.French -> StampCameraCopy(
        pageTitle = "Appareil Photo \u00e0 Timbres",
        pageSubtitle = "Appuyez sur le massicot pour cr\u00e9er votre timbre.",
        importPhoto = "Importer",
        importPlacementHint = "Placez le massicot en place, puis appuyez dessus pour perforer.",
        switchCamera = "Changer",
        save = "Enregistrer",
        cancel = "Annuler",
        saveToPhoneAlbum = "Enregistrer dans la galerie",
        permissionTitle = "Autoriser l'acc\u00e8s \u00e0 la cam\u00e9ra",
        permissionBody = "L'autorisation de la cam\u00e9ra est n\u00e9cessaire pour l'aper\u00e7u et la g\u00e9n\u00e9ration du timbre.",
        permissionAction = "Autoriser la cam\u00e9ra",
        captureFailed = "Capture \u00e9chou\u00e9e",
        renderFailed = "\u00c9chec du rendu du timbre",
        saveFailed = "\u00c9chec de l'enregistrement du timbre",
        loadFailed = "\u00c9chec du chargement de l'image",
        savedToCalendar = "Enregistr\u00e9 dans le calendrier",
        savedToCalendarAndAlbum = "Enregistr\u00e9 dans le calendrier et la galerie",
        savedToCalendarAlbumFailed = "Enregistr\u00e9 dans le calendrier mais \u00e9chec de l'\u00e9criture dans la galerie"
    )
    AppLanguage.German -> StampCameraCopy(
        pageTitle = "Briefmarken-Stempelkamera",
        pageSubtitle = "Tippen Sie auf den Stempel, um Ihren Stempel zu erstellen.",
        importPhoto = "Importieren",
        importPlacementHint = "Positionieren Sie den Stempel, und tippen Sie dann zum Stanzen.",
        switchCamera = "Wechseln",
        save = "Speichern",
        cancel = "Abbrechen",
        saveToPhoneAlbum = "In Galerie speichern",
        permissionTitle = "Kamerazugriff erlauben",
        permissionBody = "Kameraberechtigung ist erforderlich, um in der Stempelvorschau anzuzeigen und einen Stempel zu erstellen.",
        permissionAction = "Kamera erlauben",
        captureFailed = "Aufnahme fehlgeschlagen",
        renderFailed = "Stempel konnte nicht gerendert werden",
        saveFailed = "Speichern der Briefmarke fehlgeschlagen",
        loadFailed = "Bild konnte nicht geladen werden",
        savedToCalendar = "Im Kalender gespeichert",
        savedToCalendarAndAlbum = "Im Kalender und der Galerie gespeichert",
        savedToCalendarAlbumFailed = "Im Kalender gespeichert, aber Schreiben in die Galerie fehlgeschlagen"
    )
    AppLanguage.Italian -> StampCameraCopy(
        pageTitle = "Macchina Tagliastampi",
        pageSubtitle = "Tocca il tagliastampi per creare il tuo francobollo.",
        importPhoto = "Importa",
        importPlacementHint = "Posiziona il tagliastampi e tocca per perforare.",
        switchCamera = "Cambia",
        save = "Salva",
        cancel = "Annulla",
        saveToPhoneAlbum = "Salva nella galleria",
        permissionTitle = "Consenti l'accesso alla fotocamera",
        permissionBody = "\u00c8 necessaria l'autorizzazione della fotocamera per l'anteprima e la generazione del francobollo.",
        permissionAction = "Consenti fotocamera",
        captureFailed = "Acquisizione fallita",
        renderFailed = "Rendering del francobollo fallito",
        saveFailed = "Salvataggio francobollo fallito",
        loadFailed = "Caricamento immagine fallito",
        savedToCalendar = "Salvato nel calendario",
        savedToCalendarAndAlbum = "Salvato nel calendario e nella galleria",
        savedToCalendarAlbumFailed = "Salvato nel calendario, ma scrittura nella galleria fallita"
    )
    AppLanguage.Spanish -> StampCameraCopy(
        pageTitle = "C\u00e1mara de Sellos",
        pageSubtitle = "Toca el cortador de sellos para crear tu sello.",
        importPhoto = "Importar",
        importPlacementHint = "Coloca el cortador de sellos en su lugar, luego toca para perforar.",
        switchCamera = "Cambiar",
        save = "Guardar",
        cancel = "Cancelar",
        saveToPhoneAlbum = "Guardar en la galer\u00eda",
        permissionTitle = "Permitir acceso a la c\u00e1mara",
        permissionBody = "Se requiere permiso de c\u00e1mara para previsualizar y generar un sello.",
        permissionAction = "Permitir c\u00e1mara",
        captureFailed = "Captura fallida",
        renderFailed = "Error al renderizar el sello",
        saveFailed = "Error al guardar el sello",
        loadFailed = "Error al cargar la imagen",
        savedToCalendar = "Guardado en el calendario",
        savedToCalendarAndAlbum = "Guardado en el calendario y la galer\u00eda",
        savedToCalendarAlbumFailed = "Guardado en el calendario, pero error al escribir en la galer\u00eda"
    )
    AppLanguage.Portuguese -> StampCameraCopy(
        pageTitle = "C\u00e2mera de Carimbos",
        pageSubtitle = "Toque no cortador para criar seu carimbo.",
        importPhoto = "Importar",
        importPlacementHint = "Posicione o cortador de carimbos e toque para perfurar.",
        switchCamera = "Trocar",
        save = "Salvar",
        cancel = "Cancelar",
        saveToPhoneAlbum = "Salvar na galeria",
        permissionTitle = "Permitir acesso \u00e0 c\u00e2mera",
        permissionBody = "\u00c9 necess\u00e1rio permiss\u00e3o da c\u00e2mera para visualizar e gerar um carimbo.",
        permissionAction = "Permitir c\u00e2mera",
        captureFailed = "Captura falhou",
        renderFailed = "Falha ao renderizar carimbo",
        saveFailed = "Falha ao salvar o selo",
        loadFailed = "Falha ao carregar imagem",
        savedToCalendar = "Salvo no calend\u00e1rio",
        savedToCalendarAndAlbum = "Salvo no calend\u00e1rio e na galeria",
        savedToCalendarAlbumFailed = "Salvo no calend\u00e1rio, mas falha ao escrever na galeria"
    )
    AppLanguage.Russian -> StampCameraCopy(
        pageTitle = "\u0428\u0442\u0430\u043c\u043f\u043a\u0430\u043c\u0435\u0440\u0430",
        pageSubtitle = "\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043d\u0430 \u0448\u0442\u0430\u043c\u043f, \u0447\u0442\u043e\u0431\u044b \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0441\u0432\u043e\u044e \u043c\u0430\u0440\u043a\u0443.",
        importPhoto = "\u0418\u043c\u043f\u043e\u0440\u0442",
        importPlacementHint = "\u041f\u0435\u0440\u0435\u0442\u0430\u0449\u0438\u0442\u0435 \u0448\u0442\u0430\u043c\u043f \u043d\u0430 \u043c\u0435\u0441\u0442\u043e \u0438 \u043d\u0430\u0436\u043c\u0438\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u043f\u0440\u043e\u0431\u0438\u0442\u044c.",
        switchCamera = "\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0438\u0442\u044c",
        save = "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c",
        cancel = "\u041e\u0442\u043c\u0435\u043d\u0430",
        saveToPhoneAlbum = "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432 \u0433\u0430\u043b\u0435\u0440\u0435\u044e",
        permissionTitle = "\u0420\u0430\u0437\u0440\u0435\u0448\u0438\u0442\u044c \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u043a\u0430\u043c\u0435\u0440\u0435",
        permissionBody = "\u0422\u0440\u0435\u0431\u0443\u0435\u0442\u0441\u044f \u0440\u0430\u0437\u0440\u0435\u0448\u0435\u043d\u0438\u0435 \u043d\u0430 \u043a\u0430\u043c\u0435\u0440\u0443 \u0434\u043b\u044f \u043f\u0440\u0435\u0434\u0432\u0430\u0440\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0433\u043e \u043f\u0440\u043e\u0441\u043c\u043e\u0442\u0440\u0430 \u0438 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u044f \u0448\u0442\u0430\u043c\u043f\u0430.",
        permissionAction = "\u0420\u0430\u0437\u0440\u0435\u0448\u0438\u0442\u044c \u043a\u0430\u043c\u0435\u0440\u0443",
        captureFailed = "\u0421\u044a\u0451\u043c\u043a\u0430 \u043d\u0435 \u0443\u0434\u0430\u043b\u0430\u0441\u044c",
        renderFailed = "\u041e\u0448\u0438\u0431\u043a\u0430 \u0440\u0435\u043d\u0434\u0435\u0440\u0438\u043d\u0433\u0430 \u0448\u0442\u0430\u043c\u043f\u0430",
        saveFailed = "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u043c\u0430\u0440\u043a\u0443",
        loadFailed = "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0435",
        savedToCalendar = "\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e \u0432 \u043a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u0435",
        savedToCalendarAndAlbum = "\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e \u0432 \u043a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u0435 \u0438 \u0433\u0430\u043b\u0435\u0440\u0435\u0435",
        savedToCalendarAlbumFailed = "\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e \u0432 \u043a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u0435, \u043d\u043e \u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u043f\u0438\u0441\u0430\u0442\u044c \u0432 \u0433\u0430\u043b\u0435\u0440\u0435\u044e"
    )
    AppLanguage.Thai -> StampCameraCopy(
        pageTitle = "\u0e01\u0e49\u0e32\u0e07\u0e07\u0e32\u0e19\u0e04\u0e2a\u0e48\u0e07\u0e02\u0e2d\u0e07",
        pageSubtitle = "\u0e41\u0e15\u0e48\u0e07\u0e40\u0e1e\u0e32\u0e30\u0e40\u0e1e\u0e37\u0e0d\u0e40\u0e1e\u0e48\u0e2d\u0e14\u0e49\u0e2d\u0e07\u0e40\u0e1e\u0e37\u0e0d\u0e02\u0e23\u0e49\u0e32\u0e07\u0e41\u0e1c\u0e48\u0e19\u0e4a\u0e01",
        importPhoto = "\u0e19\u0e33\u0e40\u0e02\u0e49\u0e32\u0e20\u0e32\u0e1e\u0e31\u0e1a",
        importPlacementHint = "\u0e25\u0e32\u0e14\u0e40\u0e1e\u0e48\u0e32\u0e15\u0e31\u0e14\u0e41\u0e1c\u0e48\u0e19\u0e4a\u0e01\u0e44\u0e1b\u0e17\u0e35\u0e48\u0e15\u0e33\u0e41\u0e2b\u0e19\u0e48\u0e41\u0e25\u0e49\u0e27\u0e41\u0e15\u0e48\u0e07\u0e40\u0e1e\u0e48\u0e32\u0e15\u0e31\u0e14\u0e40\u0e1e\u0e37\u0e0d\u0e02\u0e23\u0e49\u0e32\u0e07",
        switchCamera = "\u0e2a\u0e4c\u0e1f\u0e37\u0e1a\u0e01\u0e23\u0e30\u0e02\u0e38\u0e13\u0e30",
        save = "\u0e1a\u0e31\u0e15\u0e23\u0e41\u0e1c\u0e48\u0e19\u0e4a\u0e01",
        cancel = "\u0e22\u0e01\u0e40\u0e25\u0e34\u0e01",
        saveToPhoneAlbum = "\u0e1a\u0e31\u0e15\u0e23\u0e44\u0e1b\u0e41\u0e04\u0e25\u0e2d\u0e23\u0e4c\u0e35\u0e40\u0e23\u0e35\u0e22\u0e23\u0e35\u0e16\u0e18\u0e4c",
        permissionTitle = "\u0e2d\u0e19\u0e38\u0e0d\u0e32\u0e15\u0e23\u0e30\u0e02\u0e38\u0e13\u0e30\u0e01\u0e23\u0e30\u0e02\u0e38\u0e13\u0e30",
        permissionBody = "\u0e15\u0e49\u0e2d\u0e07\u0e01\u0e32\u0e23\u0e2d\u0e19\u0e38\u0e0d\u0e32\u0e15\u0e23\u0e30\u0e01\u0e23\u0e30\u0e02\u0e38\u0e13\u0e30\u0e40\u0e1e\u0e37\u0e0d\u0e40\u0e1e\u0e48\u0e2d\u0e14\u0e39\u0e49\u0e25\u0e48\u0e27\u0e07\u0e20\u0e32\u0e1e\u0e41\u0e25\u0e30\u0e2a\u0e23\u0e49\u0e32\u0e07\u0e41\u0e1c\u0e48\u0e19\u0e4a\u0e01",
        permissionAction = "\u0e2d\u0e19\u0e38\u0e0d\u0e32\u0e15\u0e23\u0e30\u0e01\u0e23\u0e30\u0e02\u0e38\u0e13\u0e30",
        captureFailed = "\u0e01\u0e32\u0e23\u0e04\u0e49\u0e19\u0e20\u0e32\u0e1e\u0e1e\u0e24\u0e15\u0e48\u0e32\u0e07",
        renderFailed = "\u0e01\u0e32\u0e23\u0e40\u0e1e\u0e23\u0e35\u0e2d\u0e2d\u0e1e\u0e4a\u0e01\u0e1e\u0e37\u0e48\u0e2d\u0e1e\u0e25\u0e32\u0e1e\u0e4c",
        saveFailed = "\u0e1a\u0e31\u0e15\u0e23\u0e41\u0e1c\u0e48\u0e19\u0e4a\u0e01\u0e25\u0e49\u0e2d\u0e21",
        loadFailed = "\u0e01\u0e32\u0e23\u0e42\u0e1f\u0e25\u0e94\u0e20\u0e32\u0e1e\u0e31\u0e1a\u0e1e\u0e24\u0e15\u0e48\u0e32\u0e07",
        savedToCalendar = "\u0e1a\u0e31\u0e15\u0e23\u0e44\u0e1b\u0e1e\u0e15\u0e34\u0e01\u0e31\u0e1a\u0e02\u0e2d\u0e07\u0e04\u0e38\u0e13",
        savedToCalendarAndAlbum = "\u0e1a\u0e31\u0e15\u0e23\u0e44\u0e1b\u0e1e\u0e15\u0e34\u0e01\u0e31\u0e1a\u0e41\u0e04\u0e25\u0e2d\u0e23\u0e4c\u0e35\u0e40\u0e23\u0e35\u0e22\u0e23\u0e35\u0e16\u0e18\u0e4c\u0e41\u0e25\u0e49\u0e27",
        savedToCalendarAlbumFailed = "\u0e1a\u0e31\u0e15\u0e23\u0e44\u0e1b\u0e1e\u0e15\u0e34\u0e01\u0e31\u0e1a\u0e02\u0e2d\u0e07\u0e04\u0e38\u0e13\u0e41\u0e25\u0e49\u0e27\u0e41\u0e15\u0e48\u0e1e\u0e15\u0e32\u0e21\u0e40\u0e1e\u0e25\u0e34\u0e04\u0e4c\u0e14\u0e32\u0e27\u0e01\u0e32\u0e23\u0e4c\u0e35\u0e40\u0e23\u0e35\u0e22\u0e23\u0e35\u0e16\u0e18\u0e4c"
    )
    AppLanguage.Vietnamese -> StampCameraCopy(
        pageTitle = "M\u00e1y \u1ea2nh D\u1eadp Khu\u00f4n",
        pageSubtitle = "Nh\u1ea5n v\u00e0o d\u1eadp \u0111\u1ec3 t\u1ea1o con d\u1ea5u c\u1ee7a b\u1ea1n.",
        importPhoto = "Nh\u1eadp",
        importPlacementHint = "K\u00e9o d\u1eadp \u0111\u1ebfn v\u1ecb tr\u00ed r\u1ed3i nh\u1ea5n \u0111\u1ec3 \u0111\u1ee5c.",
        switchCamera = "Chuy\u1ec3n",
        save = "L\u01b0u",
        cancel = "H\u1ee7y",
        saveToPhoneAlbum = "L\u01b0u v\u00e0o th\u01b0 vi\u1ec7n",
        permissionTitle = "Cho ph\u00e9p truy c\u1eadp m\u00e1y \u1ea3nh",
        permissionBody = "C\u1ea7n quy\u1ec1n m\u00e1y \u1ea3nh \u0111\u1ec3 xem tr\u01b0\u1edbc v\u00e0 t\u1ea1o d\u1eadp.",
        permissionAction = "Cho ph\u00e9p m\u00e1y \u1ea3nh",
        captureFailed = "Ch\u1ee5p th\u1ea5t b\u1ea1i",
        renderFailed = "K\u1eb3t xu\u1ea5t d\u1eadp th\u1ea5t b\u1ea1i",
        saveFailed = "L\u01b0u tem th\u1ea5t b\u1ea1i",
        loadFailed = "T\u1ea3i \u1ea3nh th\u1ea5t b\u1ea1i",
        savedToCalendar = "\u0110\u00e3 l\u01b0u v\u00e0o l\u1ecbch",
        savedToCalendarAndAlbum = "\u0110\u00e3 l\u01b0u v\u00e0o l\u1ecbch v\u00e0 th\u01b0 vi\u1ec7n",
        savedToCalendarAlbumFailed = "\u0110\u00e3 l\u01b0u v\u00e0o l\u1ecbch nh\u01b0ng kh\u00f4ng ghi \u0111\u01b0\u1ee3c v\u00e0o th\u01b0 vi\u1ec7n"
    )
    AppLanguage.Indonesian -> StampCameraCopy(
        pageTitle = "Kamera Potong Perangko",
        pageSubtitle = "Ketuk pemotong untuk membuat perangko Anda.",
        importPhoto = "Impor",
        importPlacementHint = "Seret pemotong perangko ke tempatnya, lalu ketuk untuk melubangi.",
        switchCamera = "Ganti",
        save = "Simpan",
        cancel = "Batal",
        saveToPhoneAlbum = "Simpan ke galeri",
        permissionTitle = "Izinkan Akses Kamera",
        permissionBody = "Izin kamera diperlukan untuk melihat pratinjau dan menghasilkan perangko.",
        permissionAction = "Izinkan Kamera",
        captureFailed = "Pengambilan gagal",
        renderFailed = "Gagal merender perangko",
        saveFailed = "Gagal menyimpan perangko",
        loadFailed = "Gagal memuat gambar",
        savedToCalendar = "Disimpan ke kalender",
        savedToCalendarAndAlbum = "Disimpan ke kalender dan galeri",
        savedToCalendarAlbumFailed = "Disimpan ke kalender, tetapi gagal menulis ke galeri"
    )
    AppLanguage.Arabic -> StampCameraCopy(
        pageTitle = "\u0643\u0627\u0645\u064a\u0631\u0627 \u0642\u0627\u0637\u0639 \u0627\u0644\u0637\u0648\u0627\u0628\u0639",
        pageSubtitle = "\u0627\u0646\u0642\u0631 \u0639\u0644\u0649 \u0627\u0644\u0642\u0627\u0637\u0639 \u0644\u0625\u0646\u0634\u0627\u0621 \u0637\u0627\u0628\u0639\u0643.",
        importPhoto = "\u0627\u0633\u062a\u064a\u0631\u0627\u062f",
        importPlacementHint = "\u0627\u0633\u062d\u0628 \u0627\u0644\u0642\u0627\u0637\u0639 \u0625\u0644\u0649 \u0627\u0644\u0645\u0643\u0627\u0646\u060c \u062b\u0645 \u0627\u0646\u0642\u0631 \u0644\u062b\u0642\u0628.",
        switchCamera = "\u062a\u063a\u064a\u064a\u0631",
        save = "\u062d\u0641\u0638",
        cancel = "\u0625\u0644\u063a\u0627\u0621",
        saveToPhoneAlbum = "\u062d\u0641\u0638 \u0641\u064a \u0627\u0644\u0645\u0639\u0631\u0636",
        permissionTitle = "\u0627\u0644\u0633\u0645\u0627\u062d \u0628\u0627\u0644\u0648\u0635\u0648\u0644 \u0644\u0644\u0643\u0627\u0645\u064a\u0631\u0627",
        permissionBody = "\u064a\u062c\u0628 \u0627\u0644\u0633\u0645\u0627\u062d \u0628\u0627\u0644\u0643\u0627\u0645\u064a\u0631\u0627 \u0644\u0644\u0645\u0639\u0627\u064a\u0646\u0629 \u0648\u0625\u0646\u0634\u0627\u0621 \u0627\u0644\u0637\u0627\u0628\u0639.",
        permissionAction = "\u0627\u0644\u0633\u0645\u0627\u062d \u0628\u0627\u0644\u0643\u0627\u0645\u064a\u0631\u0627",
        captureFailed = "\u0641\u0634\u0644\u062a \u0627\u0644\u0625\u062e\u0630\u0627\u0621",
        renderFailed = "\u0641\u0634\u0644 \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0637\u0627\u0628\u0639",
        saveFailed = "\u0641\u0634\u0644 \u062d\u0641\u0638 \u0627\u0644طاب\u0639",
        loadFailed = "\u0641\u0634\u0644 \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u0635\u0648\u0631\u0629",
        savedToCalendar = "\u062a\u0645 \u0627\u0644\u062d\u0641\u0638 \u0641\u064a \u0627\u0644\u062a\u0642\u0648\u064a\u0645",
        savedToCalendarAndAlbum = "\u062a\u0645 \u0627\u0644\u062d\u0641\u0638 \u0641\u064a \u0627\u0644\u062a\u0642\u0648\u064a\u0645 \u0648\u0627\u0644\u0645\u0639\u0631\u0636",
        savedToCalendarAlbumFailed = "\u062a\u0645 \u0627\u0644\u062d\u0641\u0638 \u0641\u064a \u0627\u0644\u062a\u0642\u0648\u064a\u0645 \u0648\u0644\u0643\u0646 \u0641\u0634\u0644 \u0627\u0644\u0643\u062a\u0627\u0628\u0629 \u0641\u064a \u0627\u0644\u0645\u0639\u0631\u0636"
    )
    AppLanguage.Ukrainian -> StampCameraCopy(
        pageTitle = "Камера-вирубка марок",
        pageSubtitle = "Торкніться вирубки, щоб створити марку.",
        importPhoto = "Імпорт",
        importPlacementHint = "Перетягніть вирубку на місце, потім торкніться її, щоб пробити.",
        switchCamera = "Змінити",
        save = "Зберегти",
        cancel = "Скасувати",
        saveToPhoneAlbum = "Зберегти в галерею телефона",
        permissionTitle = "Дозвольте доступ до камери",
        permissionBody = "Для попереднього перегляду всередині вирубки та створення марки потрібен дозвіл на камеру.",
        permissionAction = "Дозволити камеру",
        captureFailed = "Не вдалося зробити знімок",
        renderFailed = "Не вдалося створити марку",
        saveFailed = "Не вдалося зберегти марку",
        loadFailed = "Не вдалося завантажити зображення",
        savedToCalendar = "Збережено в календарі",
        savedToCalendarAndAlbum = "Збережено в календарі та галереї телефона",
        savedToCalendarAlbumFailed = "Збережено в календарі, але не вдалося записати в галерею телефона"
    )
    AppLanguage.Polish -> StampCameraCopy(
        pageTitle = "Aparat z wykrojnikiem znaczka",
        pageSubtitle = "Dotknij wykrojnika, aby utworzyć znaczek.",
        importPhoto = "Importuj",
        importPlacementHint = "Przeciągnij wykrojnik na miejsce, a potem dotknij go, aby wybić.",
        switchCamera = "Przełącz",
        save = "Zapisz",
        cancel = "Anuluj",
        saveToPhoneAlbum = "Zapisz w galerii telefonu",
        permissionTitle = "Zezwól na dostęp do aparatu",
        permissionBody = "Do podglądu w wykrojniku i tworzenia znaczka potrzebne jest uprawnienie do aparatu.",
        permissionAction = "Zezwól na aparat",
        captureFailed = "Nie udało się zrobić zdjęcia",
        renderFailed = "Nie udało się utworzyć znaczka",
        saveFailed = "Nie udało się zapisać znaczka",
        loadFailed = "Nie udało się wczytać obrazu",
        savedToCalendar = "Zapisano w kalendarzu",
        savedToCalendarAndAlbum = "Zapisano w kalendarzu i galerii telefonu",
        savedToCalendarAlbumFailed = "Zapisano w kalendarzu, ale nie udało się zapisać w galerii telefonu"
    )
    AppLanguage.Czech -> StampCameraCopy(
        pageTitle = "Fotoaparát s výsekem známky",
        pageSubtitle = "Klepnutím na výsek vytvoříte známku.",
        importPhoto = "Importovat",
        importPlacementHint = "Přetáhněte výsek na místo a klepnutím jej vyrazte.",
        switchCamera = "Přepnout",
        save = "Uložit",
        cancel = "Zrušit",
        saveToPhoneAlbum = "Uložit do galerie telefonu",
        permissionTitle = "Povolte přístup k fotoaparátu",
        permissionBody = "K náhledu ve výseku a vytvoření známky je potřeba oprávnění k fotoaparátu.",
        permissionAction = "Povolit fotoaparát",
        captureFailed = "Snímek se nepodařilo pořídit",
        renderFailed = "Známku se nepodařilo vykreslit",
        saveFailed = "Známku se nepodařilo uložit",
        loadFailed = "Obrázek se nepodařilo načíst",
        savedToCalendar = "Uloženo do kalendáře",
        savedToCalendarAndAlbum = "Uloženo do kalendáře a galerie telefonu",
        savedToCalendarAlbumFailed = "Uloženo do kalendáře, ale zápis do galerie telefonu se nezdařil"
    )
    AppLanguage.Dutch -> StampCameraCopy(
        pageTitle = "Postzegelstanscamera",
        pageSubtitle = "Tik op de stans om je postzegel te maken.",
        importPhoto = "Importeer",
        importPlacementHint = "Sleep de stans op zijn plek en tik erop om te ponsen.",
        switchCamera = "Wissel",
        save = "Bewaar",
        cancel = "Annuleer",
        saveToPhoneAlbum = "Bewaar in galerij van telefoon",
        permissionTitle = "Cameratoegang toestaan",
        permissionBody = "Cameratoestemming is nodig voor de voorvertoning in de stans en om een postzegel te maken.",
        permissionAction = "Camera toestaan",
        captureFailed = "Opname mislukt",
        renderFailed = "Postzegel renderen mislukt",
        saveFailed = "Postzegel bewaren mislukt",
        loadFailed = "Afbeelding laden mislukt",
        savedToCalendar = "Bewaard in kalender",
        savedToCalendarAndAlbum = "Bewaard in kalender en galerij van telefoon",
        savedToCalendarAlbumFailed = "Bewaard in kalender, maar schrijven naar galerij van telefoon mislukt"
    )
    AppLanguage.Swedish -> StampCameraCopy(
        pageTitle = "Frimärkskamera",
        pageSubtitle = "Tryck på stansen för att skapa ditt frimärke.",
        importPhoto = "Importera",
        importPlacementHint = "Dra stansen på plats och tryck på den för att stansa.",
        switchCamera = "Byt",
        save = "Spara",
        cancel = "Avbryt",
        saveToPhoneAlbum = "Spara i telefonens galleri",
        permissionTitle = "Tillåt kameraåtkomst",
        permissionBody = "Kamerabehörighet krävs för förhandsvisning i stansen och för att skapa ett frimärke.",
        permissionAction = "Tillåt kamera",
        captureFailed = "Fotograferingen misslyckades",
        renderFailed = "Det gick inte att rendera frimärket",
        saveFailed = "Det gick inte att spara frimärket",
        loadFailed = "Det gick inte att läsa in bilden",
        savedToCalendar = "Sparat i kalendern",
        savedToCalendarAndAlbum = "Sparat i kalendern och telefonens galleri",
        savedToCalendarAlbumFailed = "Sparat i kalendern, men det gick inte att skriva till telefonens galleri"
    )
    AppLanguage.Danish -> StampCameraCopy(
        pageTitle = "Frimærkekamera",
        pageSubtitle = "Tryk på stansen for at lave dit frimærke.",
        importPhoto = "Importér",
        importPlacementHint = "Træk stansen på plads, og tryk på den for at stanse.",
        switchCamera = "Skift",
        save = "Gem",
        cancel = "Annuller",
        saveToPhoneAlbum = "Gem i telefonens galleri",
        permissionTitle = "Tillad adgang til kamera",
        permissionBody = "Kameratilladelse kræves for at se forhåndsvisning i stansen og lave et frimærke.",
        permissionAction = "Tillad kamera",
        captureFailed = "Optagelsen mislykkedes",
        renderFailed = "Frimærket kunne ikke gengives",
        saveFailed = "Frimærket kunne ikke gemmes",
        loadFailed = "Billedet kunne ikke indlæses",
        savedToCalendar = "Gemt i kalenderen",
        savedToCalendarAndAlbum = "Gemt i kalenderen og telefonens galleri",
        savedToCalendarAlbumFailed = "Gemt i kalenderen, men kunne ikke skrives til telefonens galleri"
    )
    AppLanguage.Norwegian -> StampCameraCopy(
        pageTitle = "Frimerkekamera",
        pageSubtitle = "Trykk på stansen for å lage frimerket ditt.",
        importPhoto = "Importer",
        importPlacementHint = "Dra stansen på plass og trykk på den for å stanse.",
        switchCamera = "Bytt",
        save = "Lagre",
        cancel = "Avbryt",
        saveToPhoneAlbum = "Lagre i telefonens galleri",
        permissionTitle = "Tillat kameratilgang",
        permissionBody = "Kameratillatelse kreves for forhåndsvisning i stansen og for å lage et frimerke.",
        permissionAction = "Tillat kamera",
        captureFailed = "Opptaket mislyktes",
        renderFailed = "Kunne ikke gjengi frimerket",
        saveFailed = "Kunne ikke lagre frimerket",
        loadFailed = "Kunne ikke laste inn bildet",
        savedToCalendar = "Lagret i kalenderen",
        savedToCalendarAndAlbum = "Lagret i kalenderen og telefonens galleri",
        savedToCalendarAlbumFailed = "Lagret i kalenderen, men kunne ikke skrive til telefonens galleri"
    )
    AppLanguage.Finnish -> StampCameraCopy(
        pageTitle = "Postimerkkileikkurikamera",
        pageSubtitle = "Napauta leikkuria luodaksesi postimerkin.",
        importPhoto = "Tuo",
        importPlacementHint = "Vedä leikkuri paikalleen ja napauta sitä lävistääksesi.",
        switchCamera = "Vaihda",
        save = "Tallenna",
        cancel = "Kumoa",
        saveToPhoneAlbum = "Tallenna puhelimen galleriaan",
        permissionTitle = "Salli kameran käyttö",
        permissionBody = "Kameran lupa tarvitaan esikatseluun leikkurin sisällä ja postimerkin luomiseen.",
        permissionAction = "Salli kamera",
        captureFailed = "Kuvaus epäonnistui",
        renderFailed = "Postimerkin piirto epäonnistui",
        saveFailed = "Postimerkin tallennus epäonnistui",
        loadFailed = "Kuvan lataus epäonnistui",
        savedToCalendar = "Tallennettu kalenteriin",
        savedToCalendarAndAlbum = "Tallennettu kalenteriin ja puhelimen galleriaan",
        savedToCalendarAlbumFailed = "Tallennettu kalenteriin, mutta kirjoitus puhelimen galleriaan epäonnistui"
    )
    AppLanguage.Greek -> StampCameraCopy(
        pageTitle = "Κάμερα κοπτικού γραμματοσήμων",
        pageSubtitle = "Πατήστε το κοπτικό για να δημιουργήσετε το γραμματόσημό σας.",
        importPhoto = "Εισαγωγή",
        importPlacementHint = "Σύρετε το κοπτικό στη θέση του και πατήστε το για διάτρηση.",
        switchCamera = "Εναλλαγή",
        save = "Αποθήκευση",
        cancel = "Ακύρωση",
        saveToPhoneAlbum = "Αποθήκευση στη συλλογή του τηλεφώνου",
        permissionTitle = "Να επιτραπεί η πρόσβαση στην κάμερα",
        permissionBody = "Απαιτείται άδεια κάμερας για προεπισκόπηση μέσα στο κοπτικό και δημιουργία γραμματοσήμου.",
        permissionAction = "Να επιτραπεί η κάμερα",
        captureFailed = "Η λήψη απέτυχε",
        renderFailed = "Αποτυχία δημιουργίας γραμματοσήμου",
        saveFailed = "Αποτυχία αποθήκευσης γραμματοσήμου",
        loadFailed = "Αποτυχία φόρτωσης εικόνας",
        savedToCalendar = "Αποθηκεύτηκε στο ημερολόγιο",
        savedToCalendarAndAlbum = "Αποθηκεύτηκε στο ημερολόγιο και στη συλλογή του τηλεφώνου",
        savedToCalendarAlbumFailed = "Αποθηκεύτηκε στο ημερολόγιο, αλλά απέτυχε η εγγραφή στη συλλογή του τηλεφώνου"
    )
    AppLanguage.Romanian -> StampCameraCopy(
        pageTitle = "Cameră cu ștanță de timbre",
        pageSubtitle = "Atinge ștanța pentru a crea timbrul.",
        importPhoto = "Importă",
        importPlacementHint = "Trage ștanța la locul ei, apoi atinge-o pentru a perfora.",
        switchCamera = "Comută",
        save = "Salvează",
        cancel = "Anulează",
        saveToPhoneAlbum = "Salvează în galeria telefonului",
        permissionTitle = "Permite accesul la cameră",
        permissionBody = "Permisiunea pentru cameră este necesară pentru previzualizarea în ștanță și crearea timbrului.",
        permissionAction = "Permite camera",
        captureFailed = "Captura a eșuat",
        renderFailed = "Nu s-a putut genera timbrul",
        saveFailed = "Nu s-a putut salva timbrul",
        loadFailed = "Nu s-a putut încărca imaginea",
        savedToCalendar = "Salvat în calendar",
        savedToCalendarAndAlbum = "Salvat în calendar și în galeria telefonului",
        savedToCalendarAlbumFailed = "Salvat în calendar, dar scrierea în galeria telefonului a eșuat"
    )
    AppLanguage.Hungarian -> StampCameraCopy(
        pageTitle = "Bélyegkivágó kamera",
        pageSubtitle = "Koppints a kivágóra a bélyeg elkészítéséhez.",
        importPhoto = "Importálás",
        importPlacementHint = "Húzd a kivágót a helyére, majd koppints rá a lyukasztáshoz.",
        switchCamera = "Váltás",
        save = "Mentés",
        cancel = "Mégsem",
        saveToPhoneAlbum = "Mentés a telefon galériájába",
        permissionTitle = "Kamera-hozzáférés engedélyezése",
        permissionBody = "Kameraengedély szükséges a kivágóban látható előnézethez és a bélyeg elkészítéséhez.",
        permissionAction = "Kamera engedélyezése",
        captureFailed = "A felvétel sikertelen",
        renderFailed = "A bélyeg megjelenítése sikertelen",
        saveFailed = "A bélyeg mentése sikertelen",
        loadFailed = "A kép betöltése sikertelen",
        savedToCalendar = "Mentve a naptárba",
        savedToCalendarAndAlbum = "Mentve a naptárba és a telefon galériájába",
        savedToCalendarAlbumFailed = "Mentve a naptárba, de a telefon galériájába írás sikertelen"
    )
    AppLanguage.Turkish -> StampCameraCopy(
        pageTitle = "Pul Kesici Kamera",
        pageSubtitle = "Pulunu oluşturmak için kesiciye dokun.",
        importPhoto = "İçe aktar",
        importPlacementHint = "Kesiciyi yerine sürükle, sonra delmek için dokun.",
        switchCamera = "Çevir",
        save = "Kaydet",
        cancel = "İptal",
        saveToPhoneAlbum = "Telefon galerisine kaydet",
        permissionTitle = "Kamera erişimine izin ver",
        permissionBody = "Kesicinin içinde önizleme yapmak ve pul oluşturmak için kamera izni gerekir.",
        permissionAction = "Kameraya izin ver",
        captureFailed = "Çekim başarısız",
        renderFailed = "Pul oluşturulamadı",
        saveFailed = "Pul kaydedilemedi",
        loadFailed = "Görsel yüklenemedi",
        savedToCalendar = "Takvime kaydedildi",
        savedToCalendarAndAlbum = "Takvime ve telefon galerisine kaydedildi",
        savedToCalendarAlbumFailed = "Takvime kaydedildi, ancak telefon galerisine yazılamadı"
    )
    AppLanguage.Malay -> StampCameraCopy(
        pageTitle = "Kamera Pemotong Setem",
        pageSubtitle = "Ketik pemotong setem untuk mencipta setem anda.",
        importPhoto = "Import",
        importPlacementHint = "Seret pemotong setem ke tempatnya, kemudian ketik untuk menebuk.",
        switchCamera = "Tukar",
        save = "Simpan",
        cancel = "Batal",
        saveToPhoneAlbum = "Simpan ke galeri telefon",
        permissionTitle = "Benarkan Akses Kamera",
        permissionBody = "Kebenaran kamera diperlukan untuk pratonton dalam pemotong setem dan menjana setem.",
        permissionAction = "Benarkan Kamera",
        captureFailed = "Tangkapan gagal",
        renderFailed = "Gagal memaparkan setem",
        saveFailed = "Gagal menyimpan setem",
        loadFailed = "Gagal memuatkan imej",
        savedToCalendar = "Disimpan ke kalendar",
        savedToCalendarAndAlbum = "Disimpan ke kalendar dan galeri telefon",
        savedToCalendarAlbumFailed = "Disimpan ke kalendar, tetapi gagal menulis ke galeri telefon"
    )
    AppLanguage.Hebrew -> StampCameraCopy(
        pageTitle = "מצלמת חותך בולים",
        pageSubtitle = "הקישו על החותך כדי ליצור בול.",
        importPhoto = "ייבוא",
        importPlacementHint = "גררו את החותך למקומו והקישו עליו כדי לנקב.",
        switchCamera = "החלפה",
        save = "שמירה",
        cancel = "ביטול",
        saveToPhoneAlbum = "שמירה בגלריית הטלפון",
        permissionTitle = "אפשרו גישה למצלמה",
        permissionBody = "נדרשת הרשאת מצלמה לתצוגה מקדימה בתוך החותך וליצירת בול.",
        permissionAction = "אפשר מצלמה",
        captureFailed = "הצילום נכשל",
        renderFailed = "יצירת הבול נכשלה",
        saveFailed = "שמירת הבול נכשלה",
        loadFailed = "טעינת התמונה נכשלה",
        savedToCalendar = "נשמר בלוח השנה",
        savedToCalendarAndAlbum = "נשמר בלוח השנה ובגלריית הטלפון",
        savedToCalendarAlbumFailed = "נשמר בלוח השנה, אך הכתיבה לגלריית הטלפון נכשלה"
    )
    AppLanguage.English -> StampCameraCopy(
        pageTitle = "Stamp Cutter Camera",
        pageSubtitle = "Tap the stamp cutter to create your stamp.",
        importPhoto = "Import",
        importPlacementHint = "Drag the stamp cutter into place, then tap it to punch.",
        switchCamera = "Switch",
        save = "Save",
        cancel = "Cancel",
        saveToPhoneAlbum = "Save to phone gallery",
        permissionTitle = "Allow Camera Access",
        permissionBody = "Camera permission is required to preview inside the stamp cutter and generate a stamp.",
        permissionAction = "Allow Camera",
        captureFailed = "Capture failed",
        renderFailed = "Failed to render stamp",
        saveFailed = "Failed to save stamp",
        loadFailed = "Failed to load image",
        savedToCalendar = "Saved to calendar",
        savedToCalendarAndAlbum = "Saved to calendar and phone gallery",
        savedToCalendarAlbumFailed = "Saved to calendar, but failed to write into phone gallery"
    )
}
