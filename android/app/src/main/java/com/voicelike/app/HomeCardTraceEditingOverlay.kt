package com.voicelike.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voicelike.app.colorcard.lensy.rememberColorCardLensyAssessment
import com.voicelike.app.motionphoto.MotionPhotoExportCoordinator
import com.voicelike.app.motionphoto.MotionPhotoFileRepository
import com.voicelike.app.motionphoto.MotionPhotoLabPage
import com.voicelike.app.motionphoto.MotionPhotoRenderSpec
import com.voicelike.app.motionphoto.toDynamicPhotoExportToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val COLOR_CARD_TRACE_OVERLAY_DISMISS_DELAY_MS = 16L

@Composable
internal fun HomeCardTraceEditingOverlay(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    backdropBitmap: Bitmap?,
    item: MediaItem,
    imageBoundsInRoot: Rect?,
    windowSizePx: IntSize,
    isDark: Boolean,
    onImageReady: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val colorCardTraceCopy = remember(appLanguage) { getColorCardTraceCopy(appLanguage) }
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val adjustedImageBoundsInRoot = rememberColorCardTraceAdjustedImageBounds(bounds = imageBoundsInRoot)
    val store = remember(context) { ColorCardStore.get(context) }
    val savedPalettes by store.palettes.collectAsState()
    val recipeStore = remember(context) { ColorCardRecipeStore.get(context) }
    val savedRecipes by recipeStore.recipes.collectAsState()
    val originalSourceFile = remember(item.id, item.path) {
        item.path
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf(File::exists)
    }
    var importedImageSource by remember(item.id) { mutableStateOf<ColorCardImportedImageSource?>(null) }
    val latestImportedImageSource = rememberUpdatedState(importedImageSource)
    val editingItem = remember(item, importedImageSource?.sessionId) {
        importedImageSource?.toImportedMediaItem() ?: item
    }
    val currentSourceSessionId = importedImageSource?.sessionId ?: item.id
    val currentSourceFile = importedImageSource?.tempFile ?: originalSourceFile
    val currentSourceMediaId = if (importedImageSource != null) null else item.id
    var overlayVisible by remember(currentSourceSessionId) { mutableStateOf(active) }
    var dismissScheduled by remember(currentSourceSessionId) { mutableStateOf(false) }
    val latestOnDismissRequest = rememberUpdatedState(onDismissRequest)
    DisposableEffect(activity, currentSourceSessionId) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val colorPickerState = rememberHomeCardColorPickerState(
        active = active && overlayVisible,
        item = editingItem,
        sourceMediaId = currentSourceMediaId
    )
    val lensyEnabled = remember(context, appLanguage) {
        isLensySupported(appLanguage) && AiHomepageDisplayPreferences(context).lensyEnabled
    }
    var panelMode by remember(currentSourceSessionId) { mutableStateOf(ColorCardTracePanelMode.Picker) }
    var effectPanelMode by remember(currentSourceSessionId) { mutableStateOf(ColorCardTracePanelMode.Picker) }
    var pendingRecipeToApply by remember(currentSourceSessionId) { mutableStateOf<SavedColorCardRecipe?>(null) }
    var showDiscardEditDialog by remember(currentSourceSessionId) { mutableStateOf(false) }
    var showRecipeImportDialog by remember(currentSourceSessionId) { mutableStateOf(false) }
    var recipeImportText by remember(currentSourceSessionId) { mutableStateOf("") }
    var portraitPanelExpanded by remember(currentSourceSessionId) { mutableStateOf(false) }
    var lensyDismissedForSession by remember(currentSourceSessionId) { mutableStateOf(false) }
    var saveImageState by remember(currentSourceSessionId) {
        mutableStateOf(ColorCardTraceSaveImageState.Idle)
    }
    var comparePressed by remember(currentSourceSessionId) { mutableStateOf(false) }
    var exportSuccessPayload by remember(currentSourceSessionId) {
        mutableStateOf<ColorCardTraceExportSuccessPayload?>(null)
    }
    var showMotionPhotoLab by remember(currentSourceSessionId) { mutableStateOf(false) }
    var showDynamicExportMenu by remember(currentSourceSessionId) { mutableStateOf(false) }
    var imitateReferenceImage by remember(currentSourceSessionId) { mutableStateOf<ColorCardTemporaryImage?>(null) }
    var imitateReferenceLoading by remember(currentSourceSessionId) { mutableStateOf(false) }
    var imitateAnalysis by remember(currentSourceSessionId) { mutableStateOf<ColorCardImitateAnalysis?>(null) }
    val clearImitateReference: () -> Unit = {
        imitateReferenceImage?.let(::cleanupColorCardTemporaryImage)
        imitateReferenceImage = null
        imitateAnalysis = null
        imitateReferenceLoading = false
    }
    val imitateReferencePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val renderSourceFile = currentSourceFile
        if (renderSourceFile == null) {
            Toast.makeText(context, colorCardTraceCopy.cannotAnalyzeYet, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            imitateReferenceLoading = true
            var importedReference: ColorCardTemporaryImage? = null
            runCatching {
                val referenceImage = importColorCardTemporaryImage(
                    context = context.applicationContext,
                    sourceUri = uri,
                    tempPrefix = "trace_imitate_reference"
                )
                importedReference = referenceImage
                val analysis = withContext(Dispatchers.Default) {
                    analyzeColorCardImitateStyle(
                        sourceFile = renderSourceFile,
                        referenceFile = referenceImage.tempFile
                    )
                } ?: error("Reference analysis failed")
                referenceImage to analysis
            }.onSuccess { (referenceImage, analysis) ->
                imitateReferenceImage?.let(::cleanupColorCardTemporaryImage)
                imitateReferenceImage = referenceImage
                imitateAnalysis = analysis
            }.onFailure {
                importedReference?.let(::cleanupColorCardTemporaryImage)
                Toast.makeText(context, colorCardTraceCopy.referenceAnalysisFailed, Toast.LENGTH_SHORT).show()
            }
            imitateReferenceLoading = false
        }
    }
    val changeImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                importColorCardEditingImage(
                    context = context.applicationContext,
                    sourceUri = uri
                )
            }.onSuccess { imported ->
                exportSuccessPayload?.release()
                exportSuccessPayload = null
                importedImageSource?.let(::cleanupColorCardEditingImageSource)
                importedImageSource = imported
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: colorCardTraceCopy.openImageFailed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    DisposableEffect(item.id) {
        onDispose {
            cleanupColorCardEditingImageSource(latestImportedImageSource.value)
        }
    }
    DisposableEffect(imitateReferenceImage) {
        val imageToCleanup = imitateReferenceImage
        onDispose {
            cleanupColorCardTemporaryImage(imageToCleanup)
        }
    }
    DisposableEffect(exportSuccessPayload) {
        val payloadToRelease = exportSuccessPayload
        onDispose {
            payloadToRelease?.release()
        }
    }
    LaunchedEffect(active, currentSourceSessionId) {
        if (active) {
            overlayVisible = true
            dismissScheduled = false
        } else {
            overlayVisible = false
        }
    }
    val traceFilterState = rememberColorCardTraceFilterState(
        active = active && overlayVisible,
        panelMode = effectPanelMode,
        sourceUri = editingItem.uri,
        sourceFile = currentSourceFile,
        sourceSessionId = currentSourceSessionId,
        sourceColors = if (colorPickerState.isReady) colorPickerState.colors else emptyList(),
        savedPalettes = savedPalettes,
        imitateReferenceLabel = imitateReferenceImage?.displayName,
        imitateAnalysis = imitateAnalysis,
        imitateReferenceLoading = imitateReferenceLoading
    )
    val lensyAssessment = rememberColorCardLensyAssessment(
        enabled = lensyEnabled,
        effectPanelMode = effectPanelMode,
        traceState = traceFilterState
    )
    val isDynamicSource = editingItem.isLivePhoto || editingItem.isMotionPhoto
    val isColorAwakeningSelected =
        traceFilterState.selectedCandidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry
    val dynamicSourceItem = importedImageSource?.sourceMediaItem ?: item
    val renderSourceFile = currentSourceFile ?: File("")
    var renderedBitmap: Bitmap? = null
    val dynamicSaveActions = remember(
        isDynamicSource,
        isColorAwakeningSelected,
        editingItem.isLivePhoto,
        editingItem.isMotionPhoto
    ) {
        if (!isDynamicSource || isColorAwakeningSelected) {
            emptyList()
        } else {
            listOf(
                ColorCardTraceSaveMenuAction(
                    id = "image",
                    title = "导出图片",
                    subtitle = "仅保存当前调色后的静态图"
                ),
                ColorCardTraceSaveMenuAction(
                    id = "live",
                    title = "导出 Live",
                    subtitle = "保留动态内容，导出单文件 Motion Photo"
                )
            )
        }
    }
    val resolvedDynamicSaveActions = remember(dynamicSaveActions) {
        dynamicSaveActions.map { action ->
            if (action.id == "live") {
                action.copy(
                    title = "导出 Live",
                    subtitle = "对整个live图进行调色"
                )
            } else {
                action
            }
        }
    }
    val sessionLensyMessage = if (lensyDismissedForSession) null else lensyAssessment?.message
    val imageRequest = remember(context, currentSourceSessionId, editingItem.uri) {
        buildHomeCardHoverStillImageRequest(
            context = context,
            uri = editingItem.uri,
            itemId = currentSourceSessionId,
            crossfade = false
        )
    }
    val performDismiss: () -> Unit = {
        if (!dismissScheduled) {
            dismissScheduled = true
            overlayVisible = false
            scope.launch {
                delay(COLOR_CARD_TRACE_OVERLAY_DISMISS_DELAY_MS)
                latestOnDismissRequest.value()
            }
        }
    }
    val requestDismiss: () -> Unit = {
        if (traceFilterState.hasUserEdits) {
            showDiscardEditDialog = true
        } else {
            performDismiss()
        }
    }

    BackHandler(enabled = active && !dismissScheduled) {
        if (showDynamicExportMenu) {
            showDynamicExportMenu = false
            return@BackHandler
        }
        requestDismiss()
    }

    LaunchedEffect(isColorAwakeningSelected) {
        if (isColorAwakeningSelected) {
            showDynamicExportMenu = false
        }
    }

    LaunchedEffect(traceFilterState.mode, traceFilterState.candidates, pendingRecipeToApply?.id) {
        val recipe = pendingRecipeToApply ?: return@LaunchedEffect
        val expectedMode = colorCardRecipeModeToPanelMode(recipe.mode)
        if (traceFilterState.mode != expectedMode) return@LaunchedEffect
        if (traceFilterState.isAnalyzing) return@LaunchedEffect
        if (traceFilterState.candidates.none { it.paletteId == recipe.paletteId }) {
            Toast.makeText(context, colorCardTraceCopy.recipeTargetNotFound, Toast.LENGTH_SHORT).show()
            pendingRecipeToApply = null
            return@LaunchedEffect
        }
        val snappedColor = (((recipe.color.coerceIn(-1f, 1f) + 1f) / 2f) * 10f).roundToInt() / 10f
        val snappedTone = (((recipe.tone.coerceIn(-1f, 1f) + 1f) / 2f) * 10f).roundToInt() / 10f
        traceFilterState.selectCandidate(recipe.paletteId)
        traceFilterState.updateSharpen(recipe.sharpen)
        traceFilterState.updateSaturation(recipe.saturation)
        traceFilterState.updateBrightness(recipe.brightness)
        traceFilterState.updatePaletteStrength(recipe.paletteStrength)
        traceFilterState.updatePortraitVitality(recipe.portraitVitality)
        traceFilterState.updatePortraitSoftness(recipe.portraitSoftness)
        traceFilterState.updatePortraitDetailSuppression(recipe.portraitDetailSuppression)
        traceFilterState.updateAdjustment(
            snappedColor.coerceIn(0f, 1f),
            (1f - snappedTone).coerceIn(0f, 1f)
        )
        pendingRecipeToApply = null
    }

    val saveRenderedStill: () -> Unit = saveRenderedStill@{
        val renderSourceFile = currentSourceFile
        if (renderSourceFile == null) {
            Toast.makeText(context, colorCardTraceCopy.cannotExportYet, Toast.LENGTH_SHORT).show()
            return@saveRenderedStill
        }
        saveImageState = ColorCardTraceSaveImageState.Saving
        scope.launch {
            var renderedBitmap: Bitmap? = null
            runCatching {
                renderedBitmap = renderColorCardTraceModeBitmap(
                    context = context.applicationContext,
                    sourceFile = renderSourceFile,
                    traceState = traceFilterState
                )
            }.getOrElse {
                Toast.makeText(context, colorCardTraceCopy.renderFailed, Toast.LENGTH_SHORT).show()
                saveImageState = ColorCardTraceSaveImageState.Idle
                return@launch
            }
            if (renderedBitmap == null) {
                Toast.makeText(context, colorCardTraceCopy.nothingToSave, Toast.LENGTH_SHORT).show()
                saveImageState = ColorCardTraceSaveImageState.Idle
                return@launch
            }
            val safeRenderedBitmap = renderedBitmap
            try {
                val savedUri = saveColorCardRenderedBitmap(
                    context = context.applicationContext,
                    bitmap = safeRenderedBitmap,
                    baseName = editingItem.displayName,
                    sourceUri = editingItem.uri
                )
                saveImageState = if (savedUri != null) {
                    ColorCardTraceSaveImageState.Saved
                } else {
                    ColorCardTraceSaveImageState.Idle
                }
                if (savedUri != null) {
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = safeRenderedBitmap,
                        savedUri = savedUri,
                        displayName = editingItem.displayName,
                        recipe = null
                    )
                    delay(800)
                } else {
                    Toast.makeText(context, colorCardTraceCopy.saveFailed, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, colorCardTraceCopy.renderFailed, Toast.LENGTH_SHORT).show()
                saveImageState = ColorCardTraceSaveImageState.Idle
            } finally {
                safeRenderedBitmap.recycle()
                if (saveImageState == ColorCardTraceSaveImageState.Saved) {
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }
            }
        }
    }

    val saveRenderedLive: () -> Unit = saveRenderedLive@{
        val dynamicSourceItem = importedImageSource?.sourceMediaItem ?: item
        val renderSourceFile = currentSourceFile
        if (renderSourceFile == null) {
            Toast.makeText(context, colorCardTraceCopy.cannotExportYet, Toast.LENGTH_SHORT).show()
            return@saveRenderedLive
        }
        saveImageState = ColorCardTraceSaveImageState.Saving
        scope.launch {
            var renderedBitmap: Bitmap? = null
            val session = MotionPhotoFileRepository(context.applicationContext)
                .createSession(editingItem.displayName)
            if (isColorAwakeningSelected) {
                runCatching {
                    withContext(Dispatchers.Default) {
                        MotionPhotoExportCoordinator.export(
                            context = context.applicationContext,
                            sourceFile = renderSourceFile,
                            fallbackBitmap = traceFilterState.previewSourceBitmap,
                            displayName = editingItem.displayName,
                            session = session,
                            spec = MotionPhotoRenderSpec.dynamicSave(
                                revealPattern = traceFilterState.motionPhotoRevealPattern,
                                revealSeed = kotlin.random.Random.nextLong()
                            ),
                            onProgress = {}
                        )
                    }
                }.onSuccess { result ->
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = result.previewBitmap,
                        savedUri = result.savedUri,
                        displayName = result.displayName,
                        recipe = null
                    )
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }.onFailure { error ->
                    saveImageState = ColorCardTraceSaveImageState.Idle
                    Toast.makeText(
                        context,
                        error.toDynamicPhotoExportToastMessage("Live Photo"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (dynamicSourceItem.isLivePhoto || dynamicSourceItem.isMotionPhoto) {
                runCatching {
                    val safeRenderedBitmap = renderColorCardTraceModeBitmap(
                        context = context.applicationContext,
                        sourceFile = renderSourceFile,
                        traceState = traceFilterState
                    ) ?: error("Nothing to save in this mode")
                    renderedBitmap = safeRenderedBitmap
                    MotionPhotoExportCoordinator.exportRenderedDynamicPhoto(
                        context = context.applicationContext,
                        sourceItem = dynamicSourceItem,
                        renderedCoverBitmap = safeRenderedBitmap,
                        traceState = traceFilterState,
                        displayName = editingItem.displayName,
                        session = session,
                        spec = MotionPhotoRenderSpec.dynamicSave(
                            revealPattern = traceFilterState.motionPhotoRevealPattern,
                            revealSeed = kotlin.random.Random.nextLong()
                        ),
                        onProgress = {}
                    )
                }.onSuccess { result ->
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = result.previewBitmap,
                        savedUri = result.primarySavedUri,
                        displayName = result.displayName,
                        recipe = null
                    )
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }.onFailure { error ->
                    saveImageState = ColorCardTraceSaveImageState.Idle
                    Toast.makeText(
                        context,
                        error.toDynamicPhotoExportToastMessage("Motion Photo"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                runCatching {
                    MotionPhotoExportCoordinator.export(
                        context = context.applicationContext,
                        sourceFile = renderSourceFile,
                        fallbackBitmap = traceFilterState.previewSourceBitmap,
                        displayName = editingItem.displayName,
                        session = session,
                        spec = MotionPhotoRenderSpec(),
                        onProgress = {}
                    )
                }.onSuccess { result ->
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = result.previewBitmap,
                        savedUri = result.savedUri,
                        displayName = result.displayName,
                        recipe = null
                    )
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }.onFailure { error ->
                    saveImageState = ColorCardTraceSaveImageState.Idle
                    Toast.makeText(
                        context,
                        error.toDynamicPhotoExportToastMessage("Live Photo"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val handleSaveRequest: () -> Unit = handleSaveRequest@{
        comparePressed = false
        if (saveImageState == ColorCardTraceSaveImageState.Saving) {
            return@handleSaveRequest
        }
        if (isColorAwakeningSelected) {
            saveRenderedLive()
            return@handleSaveRequest
        }
        if (isDynamicSource) {
            showDynamicExportMenu = !showDynamicExportMenu
            return@handleSaveRequest
        }
        saveRenderedStill()
    }

    HomeCardLongPressHoverOverlay(
        active = overlayVisible && exportSuccessPayload == null,
        visuals = visuals,
        backdropBitmap = backdropBitmap,
        imageRequest = imageRequest,
        imageBoundsInRoot = adjustedImageBoundsInRoot,
        windowSizePx = windowSizePx,
        isDark = isDark,
        quote = null,
        canRefreshQuote = false,
        onQuoteRefreshRequest = {},
        dismissOnBackdropTap = false,
        showCloseButton = false,
        imageOverlayContent = {
            ColorCardTraceFilterImageOverlay(
                traceState = traceFilterState,
                colorPickerState = colorPickerState,
                panelMode = effectPanelMode,
                comparePressed = comparePressed,
                showPortraitFaces = portraitPanelExpanded,
                showLiveBadge = isDynamicSource
            )
        },
        footerOverlayContent = {
            ColorCardTraceEditingTopBar(
                isDark = isDark,
                panelMode = panelMode,
                saveState = saveImageState,
                saveEnabled = if (panelMode == ColorCardTracePanelMode.Picker) {
                    colorPickerState.isReady
                } else {
                    traceFilterState.canSaveRenderedImage
                },
                saveMenuExpanded = showDynamicExportMenu,
                saveMenuActions = resolvedDynamicSaveActions,
                onBack = requestDismiss,
                onSave = {
                    if (panelMode == ColorCardTracePanelMode.Picker) {
                        if (colorPickerState.isReady) {
                            colorPickerState.save()
                        }
                        return@ColorCardTraceEditingTopBar
                    }
                    handleSaveRequest()
                    return@ColorCardTraceEditingTopBar
                    comparePressed = false
                    if (saveImageState == ColorCardTraceSaveImageState.Saving) {
                        return@ColorCardTraceEditingTopBar
                    }
                    if (traceFilterState.selectedCandidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry) {
                        saveImageState = ColorCardTraceSaveImageState.Saving
                        scope.launch {
                            val session = MotionPhotoFileRepository(context.applicationContext)
                                .createSession(editingItem.displayName)
                runCatching {
                    renderedBitmap = renderColorCardTraceModeBitmap(
                        context = context.applicationContext,
                        sourceFile = renderSourceFile,
                        traceState = traceFilterState
                    ) ?: error("Nothing to save in this mode")
                    MotionPhotoExportCoordinator.exportRenderedDynamicPhoto(
                        context = context.applicationContext,
                        sourceItem = dynamicSourceItem,
                        renderedCoverBitmap = renderedBitmap!!,
                        traceState = traceFilterState,
                        displayName = editingItem.displayName,
                        session = session,
                        spec = MotionPhotoRenderSpec(),
                        onProgress = {}
                    )
                }.onSuccess { result ->
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = result.previewBitmap,
                        savedUri = result.primarySavedUri,
                        displayName = result.displayName,
                        recipe = null
                    )
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }.onFailure { error ->
                    renderedBitmap?.takeIf { bitmap -> !bitmap.isRecycled }?.recycle()
                    saveImageState = ColorCardTraceSaveImageState.Idle
                    Toast.makeText(
                        context,
                        error.toDynamicPhotoExportToastMessage("Live Photo"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                        }
                        return@ColorCardTraceEditingTopBar
                    }
                    val renderSourceFile = currentSourceFile ?: return@ColorCardTraceEditingTopBar
                    if (renderSourceFile == null) {
                        Toast.makeText(context, "Cannot export this image yet", Toast.LENGTH_SHORT).show()
                        return@ColorCardTraceEditingTopBar
                    }
                    saveImageState = ColorCardTraceSaveImageState.Saving
                    scope.launch {
                        var renderedBitmap: Bitmap? = null
                        runCatching {
                            renderedBitmap = renderColorCardTraceModeBitmap(
                                context = context.applicationContext,
                                sourceFile = renderSourceFile,
                                traceState = traceFilterState
                            )
                        }.getOrElse {
                            Toast.makeText(context, "Render failed", Toast.LENGTH_SHORT).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        if (renderedBitmap == null) {
                            Toast.makeText(context, "Nothing to save in this mode", Toast.LENGTH_SHORT).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        val safeRenderedBitmap = renderedBitmap ?: return@launch
                        try {
                            val savedUri = saveColorCardRenderedBitmap(
                                context = context.applicationContext,
                                bitmap = safeRenderedBitmap,
                                baseName = editingItem.displayName,
                                sourceUri = editingItem.uri
                            )
                            saveImageState = if (savedUri != null) {
                                ColorCardTraceSaveImageState.Saved
                            } else {
                                ColorCardTraceSaveImageState.Idle
                            }
                            if (savedUri != null) {
                                exportSuccessPayload?.release()
                                exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                                    beforeBitmap = traceFilterState.previewSourceBitmap,
                                    afterBitmap = safeRenderedBitmap,
                                    savedUri = savedUri,
                                    displayName = editingItem.displayName,
                                    recipe = null
                                )
                                delay(800)
                            } else {
                                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "Render failed", Toast.LENGTH_SHORT).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                        } finally {
                            safeRenderedBitmap.recycle()
                            if (saveImageState == ColorCardTraceSaveImageState.Saved) {
                                saveImageState = ColorCardTraceSaveImageState.Idle
                            }
                        }
                    }
                },
                onSaveMenuDismiss = { showDynamicExportMenu = false },
                onSaveMenuAction = { actionId ->
                    when (actionId) {
                        "live" -> saveRenderedLive()
                        else -> saveRenderedStill()
                    }
                }
            )
            ColorCardTraceLensyFloatingOverlay(
                message = sessionLensyMessage,
                imageBoundsInRoot = adjustedImageBoundsInRoot,
                windowSizePx = windowSizePx,
                onDismissRequest = { lensyDismissedForSession = true }
            )
            ColorCardTraceFilterDeckOverlay(
                active = overlayVisible,
                visuals = visuals,
                colorPickerState = colorPickerState,
                traceState = traceFilterState,
                isDark = isDark,
                panelMode = panelMode,
                effectPanelMode = effectPanelMode,
                recipes = savedRecipes,
                saveImageState = saveImageState,
                comparePressed = comparePressed,
                portraitPanelExpanded = portraitPanelExpanded,
                onPanelModeChange = { nextMode ->
                    panelMode = nextMode
                    if (nextMode != ColorCardTracePanelMode.Recipe) {
                        effectPanelMode = nextMode
                    }
                },
                onComparePressedChange = { comparePressed = it },
                onPortraitPanelExpandedChange = { portraitPanelExpanded = it },
                onSaveImage = {
                    handleSaveRequest()
                    return@ColorCardTraceFilterDeckOverlay
                    if (saveImageState == ColorCardTraceSaveImageState.Saving) {
                        return@ColorCardTraceFilterDeckOverlay
                    }
                    if (traceFilterState.selectedCandidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry) {
                        saveImageState = ColorCardTraceSaveImageState.Saving
                        scope.launch {
                            val session = MotionPhotoFileRepository(context.applicationContext)
                                .createSession(editingItem.displayName)
                            runCatching {
                                MotionPhotoExportCoordinator.export(
                                    context = context.applicationContext,
                                    sourceFile = currentSourceFile,
                                    fallbackBitmap = traceFilterState.previewSourceBitmap,
                                    displayName = editingItem.displayName,
                                    session = session,
                                    spec = MotionPhotoRenderSpec(),
                                    onProgress = {}
                                )
                            }.onSuccess { result ->
                                exportSuccessPayload?.release()
                                exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                                    beforeBitmap = traceFilterState.previewSourceBitmap,
                                    afterBitmap = result.previewBitmap,
                                    savedUri = result.savedUri,
                                    displayName = result.displayName,
                                    recipe = null
                                )
                                saveImageState = ColorCardTraceSaveImageState.Idle
                            }.onFailure { error ->
                                saveImageState = ColorCardTraceSaveImageState.Idle
                                Toast.makeText(
                                    context,
                                    error.toDynamicPhotoExportToastMessage("Motion Photo"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        return@ColorCardTraceFilterDeckOverlay
                    }
                    val renderSourceFile = currentSourceFile ?: return@ColorCardTraceFilterDeckOverlay
                    if (renderSourceFile == null) {
                        Toast.makeText(context, "Cannot export this image yet", Toast.LENGTH_SHORT).show()
                        return@ColorCardTraceFilterDeckOverlay
                    }
                    saveImageState = ColorCardTraceSaveImageState.Saving
                    scope.launch {
                        var renderedBitmap: Bitmap? = null
                        runCatching {
                            renderedBitmap = renderColorCardTraceModeBitmap(
                                context = context.applicationContext,
                                sourceFile = renderSourceFile,
                                traceState = traceFilterState
                            )
                        }.getOrElse {
                            Toast.makeText(context, "Render failed", Toast.LENGTH_SHORT).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        if (renderedBitmap == null) {
                            Toast.makeText(context, "Nothing to save in this mode", Toast.LENGTH_SHORT).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        val safeRenderedBitmap = renderedBitmap ?: return@launch
                        try {
                            val savedUri = saveColorCardRenderedBitmap(
                                context = context.applicationContext,
                                bitmap = safeRenderedBitmap,
                                baseName = editingItem.displayName,
                                sourceUri = editingItem.uri
                            )
                            saveImageState = if (savedUri != null) {
                                ColorCardTraceSaveImageState.Saved
                            } else {
                                ColorCardTraceSaveImageState.Idle
                            }
                            if (savedUri != null) {
                                exportSuccessPayload?.release()
                                exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                                    beforeBitmap = traceFilterState.previewSourceBitmap,
                                    afterBitmap = safeRenderedBitmap,
                                    savedUri = savedUri,
                                    displayName = editingItem.displayName,
                                    recipe = null
                                )
                                delay(800)
                            } else {
                                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "Render failed", Toast.LENGTH_SHORT).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                        } finally {
                            safeRenderedBitmap.recycle()
                            if (saveImageState == ColorCardTraceSaveImageState.Saved) {
                                saveImageState = ColorCardTraceSaveImageState.Idle
                            }
                        }
                    }
                },
                onSaveRecipe = {
                    val activeMode = effectPanelMode
                    val recipe = buildColorCardTraceRecipeFromCurrent(
                        mode = activeMode,
                        traceState = traceFilterState
                    )
                    if (recipe == null) {
                        Toast.makeText(context, "No recipe to save", Toast.LENGTH_SHORT).show()
                        return@ColorCardTraceFilterDeckOverlay
                    }
                    scope.launch {
                        val saved = recipeStore.saveRecipe(recipe)
                        Toast.makeText(
                            context,
                            if (saved != null) "Recipe saved" else "Recipe save failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onApplyRecipe = { recipe ->
                    effectPanelMode = colorCardRecipeModeToPanelMode(recipe.mode)
                    pendingRecipeToApply = recipe
                },
                onShareRecipe = { recipe ->
                    shareColorCardText(
                        context = context,
                        chooserTitle = "分享配方",
                        subject = recipe.title,
                        text = buildColorCardRecipeShareText(listOf(recipe))
                    )
                },
                onDeleteRecipe = { recipe ->
                    scope.launch {
                        val deleted = recipeStore.deleteRecipe(recipe.id)
                        Toast.makeText(
                            context,
                            if (deleted) "Recipe deleted" else "Recipe delete failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onImportRecipes = {
                    showRecipeImportDialog = true
                },
                onExportAllRecipes = {
                    if (savedRecipes.isEmpty()) {
                        Toast.makeText(context, "No recipes to export", Toast.LENGTH_SHORT).show()
                    } else {
                        shareColorCardText(
                            context = context,
                            chooserTitle = "Export recipes",
                            subject = "PHOTOO recipe library",
                            text = buildColorCardRecipeShareText(savedRecipes)
                        )
                    }
                },
                onMotionPhotoEntryClick = {
                    showMotionPhotoLab = true
                },
                imitateReferencePreviewUri = imitateReferenceImage?.uri,
                imitateReferenceDisplayName = imitateReferenceImage?.displayName,
                imitateReferenceColors = imitateAnalysis?.paletteColors.orEmpty(),
                imitateReferenceLoading = imitateReferenceLoading,
                onPickImitateReference = {
                    imitateReferencePicker.launch("image/*")
                },
                onClearImitateReference = clearImitateReference
            )
        },
        onImageReady = onImageReady,
        onDismissRequest = requestDismiss
    )

    exportSuccessPayload?.let { payload ->
        ColorCardTraceExportSuccessDialog(
            payload = payload,
            onBackToEditing = {
                exportSuccessPayload = null
            },
            onChangeImage = {
                changeImagePicker.launch("image/*")
            },
            onExitEditing = {
                exportSuccessPayload = null
                performDismiss()
            },
            onCopyRecipe = {
                val recipeText = payload.copyText ?: return@ColorCardTraceExportSuccessDialog
                copyColorCardText(
                    context = context,
                    label = payload.copyLabel ?: "color_card",
                    text = recipeText,
                    successMessage = "已复制"
                )
            },
            onShareImage = {
                shareColorCardRenderedImage(
                    context = context,
                    uri = payload.savedUri,
                    displayName = payload.displayName
                )
            }
        )
    }

    if (showMotionPhotoLab) {
        MotionPhotoLabPage(
            sourceBitmap = traceFilterState.previewSourceBitmap,
            sourceFile = currentSourceFile,
            displayName = editingItem.displayName,
            onBack = { showMotionPhotoLab = false }
        )
    }

    if (showRecipeImportDialog) {
        AlertDialog(
            onDismissRequest = { showRecipeImportDialog = false },
            title = { Text("Import recipe") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste shared recipe text or token. Imported trace recipes also bring their custom palettes.",
                    )
                    OutlinedTextField(
                        value = recipeImportText,
                        onValueChange = { recipeImportText = it },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rawText = recipeImportText
                        scope.launch {
                            val importedCount = runCatching {
                                importSharedColorCardRecipes(
                                    rawText = rawText,
                                    recipeStore = recipeStore,
                                    paletteStore = store
                                )
                            }.getOrElse {
                                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                                0
                            }
                            if (importedCount > 0) {
                                panelMode = ColorCardTracePanelMode.Recipe
                                Toast.makeText(
                                    context,
                                    "Imported $importedCount recipes",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showRecipeImportDialog = false
                                recipeImportText = ""
                            }
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipeImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDiscardEditDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardEditDialog = false },
            title = { Text(stringResource(id = R.string.color_card_discard_dialog_title)) },
            text = { Text(stringResource(id = R.string.color_card_discard_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardEditDialog = false
                        performDismiss()
                    }
                ) {
                    Text(stringResource(id = R.string.color_card_discard_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardEditDialog = false }) {
                    Text(stringResource(id = R.string.color_card_discard_dialog_dismiss))
                }
            }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
