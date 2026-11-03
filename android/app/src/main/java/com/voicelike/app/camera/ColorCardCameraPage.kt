package com.voicelike.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.SharedPreferences
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.Rational
import android.util.Range
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.AspectRatio
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ripple
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.voicelike.app.LocalHapticsEnabled
import com.voicelike.app.keep.KeepGalleryPreviewOverlay

private val COLOR_CARD_CAMERA_PREVIEW_FRAME_RATE = Range(30, 60)
private const val COLOR_CARD_CAMERA_LIVE_PREVIEW_RENDER_FPS = 60
private val COLOR_CARD_CAMERA_FACE_TRACKING_TARGET_SIZE = Size(480, 640)
private val COLOR_CARD_CAMERA_PREVIEW_BITMAP_FACE_TRACKING_TARGET_SIZE = Size(360, 480)
private const val COLOR_CARD_CAMERA_PREVIEW_BITMAP_FACE_TRACKING_INTERVAL_MS = 150L
private const val COLOR_CARD_CAMERA_ENABLE_PREVIEW_BITMAP_FACE_TRACKING_FALLBACK = false
private const val COLOR_CARD_CAMERA_LOG_TAG = "ColorCardCamera"
private const val COLOR_CARD_CAMERA_SHOW_EXPERIMENTAL_YUV_ENTRY = false
private const val COLOR_CARD_CAMERA_PREVIEW_EFFECT_SWIPE_THRESHOLD_DP = 24
private const val COLOR_CARD_CAMERA_OUTPUT_ASPECT_RATIO = 3f / 4f
private const val COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM = 26f
private const val COLOR_CARD_CAMERA_EQ_ZOOM_MAX_FOCAL_MM = 50
private const val COLOR_CARD_CAMERA_EQ_ZOOM_SCALE_MIN = 0.92f
private const val COLOR_CARD_CAMERA_EQ_ZOOM_SCALE_MAX = 1.08f
private const val COLOR_CARD_CAMERA_FOCUS_DEFAULT_EXPOSURE_MIN_EV = -2f
private const val COLOR_CARD_CAMERA_FOCUS_DEFAULT_EXPOSURE_MAX_EV = 2f
private const val COLOR_CARD_CAMERA_TAP_FOCUS_AF_POINT_SIZE = 0.16f
private const val COLOR_CARD_CAMERA_TAP_FOCUS_AE_POINT_SIZE = 0.28f
private const val COLOR_CARD_CAMERA_TAP_FOCUS_AWB_POINT_SIZE = 0.28f
private const val COLOR_CARD_CAMERA_TAP_FOCUS_AUTO_CANCEL_SECONDS = 5L
private val COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR = Color(0xFFFFEA00)
private const val COLOR_CARD_CAMERA_UI_PREFS_NAME = "color_card_camera_ui_prefs"
private const val COLOR_CARD_CAMERA_PREF_FLASH_ENABLED = "flash_enabled"
private const val COLOR_CARD_CAMERA_PREF_GRID_ENABLED = "grid_enabled"
private const val COLOR_CARD_CAMERA_PREF_BEAUTY_ENABLED = "beauty_enabled"
private const val COLOR_CARD_CAMERA_PREF_EQ_ENHANCEMENT_ENABLED = "eq_enhancement_enabled"
private const val COLOR_CARD_CAMERA_PREF_EXPERIMENTAL_NOTICE_SHOWN = "experimental_notice_shown"
private const val COLOR_CARD_CAMERA_SHOW_DEBUG_OVERLAY = false
private const val COLOR_CARD_CAMERA_STARTUP_DELAY_MS = 220L

private data class ColorCardCameraOrientationState(
    val deviceRotation: Int,
    val controlsRotationDegrees: Float
)

internal enum class ColorCardCameraPreviewEffect(val label: String) {
    Dehaze("去雾"),
    None("无光效"),
    BlackMist("黑柔"),
    WhiteMist("白柔"),
    Glow("柔光"),
    Haze("薄雾")
}

private fun ColorCardCameraPreviewEffect.localizedLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> label
    AppLanguage.TraditionalChinese -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "去霧"
        ColorCardCameraPreviewEffect.None -> "無光效"
        ColorCardCameraPreviewEffect.BlackMist -> "黑柔"
        ColorCardCameraPreviewEffect.WhiteMist -> "白柔"
        ColorCardCameraPreviewEffect.Glow -> "柔光"
        ColorCardCameraPreviewEffect.Haze -> "薄霧"
    }
    AppLanguage.Korean -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "안개 제거"
        ColorCardCameraPreviewEffect.None -> "광효과 없음"
        ColorCardCameraPreviewEffect.BlackMist -> "블랙 미스트"
        ColorCardCameraPreviewEffect.WhiteMist -> "화이트 미스트"
        ColorCardCameraPreviewEffect.Glow -> "글로우"
        ColorCardCameraPreviewEffect.Haze -> "헤이즈"
    }
    AppLanguage.Japanese -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "かすみ除去"
        ColorCardCameraPreviewEffect.None -> "光効果なし"
        ColorCardCameraPreviewEffect.BlackMist -> "ブラックミスト"
        ColorCardCameraPreviewEffect.WhiteMist -> "ホワイトミスト"
        ColorCardCameraPreviewEffect.Glow -> "柔光"
        ColorCardCameraPreviewEffect.Haze -> "薄霧"
    }
    AppLanguage.French -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Anti-brume"
        ColorCardCameraPreviewEffect.None -> "Aucun effet lumineux"
        ColorCardCameraPreviewEffect.BlackMist -> "Brume noire"
        ColorCardCameraPreviewEffect.WhiteMist -> "Brume blanche"
        ColorCardCameraPreviewEffect.Glow -> "Lueur douce"
        ColorCardCameraPreviewEffect.Haze -> "Brume"
    }
    AppLanguage.German -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Dunst entfernen"
        ColorCardCameraPreviewEffect.None -> "Kein Lichteffekt"
        ColorCardCameraPreviewEffect.BlackMist -> "Schwarzer Mist"
        ColorCardCameraPreviewEffect.WhiteMist -> "Weißer Mist"
        ColorCardCameraPreviewEffect.Glow -> "Weiches Licht"
        ColorCardCameraPreviewEffect.Haze -> "Dunst"
    }
    AppLanguage.Italian -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Rimuovi foschia"
        ColorCardCameraPreviewEffect.None -> "Nessun effetto luce"
        ColorCardCameraPreviewEffect.BlackMist -> "Nebbia nera"
        ColorCardCameraPreviewEffect.WhiteMist -> "Nebbia bianca"
        ColorCardCameraPreviewEffect.Glow -> "Bagliore morbido"
        ColorCardCameraPreviewEffect.Haze -> "Foschia"
    }
    AppLanguage.Portuguese -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Remover névoa"
        ColorCardCameraPreviewEffect.None -> "Sem efeito de luz"
        ColorCardCameraPreviewEffect.BlackMist -> "Névoa preta"
        ColorCardCameraPreviewEffect.WhiteMist -> "Névoa branca"
        ColorCardCameraPreviewEffect.Glow -> "Brilho suave"
        ColorCardCameraPreviewEffect.Haze -> "Névoa"
    }
    AppLanguage.Spanish -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Quitar neblina"
        ColorCardCameraPreviewEffect.None -> "Sin efecto de luz"
        ColorCardCameraPreviewEffect.BlackMist -> "Niebla negra"
        ColorCardCameraPreviewEffect.WhiteMist -> "Niebla blanca"
        ColorCardCameraPreviewEffect.Glow -> "Luz suave"
        ColorCardCameraPreviewEffect.Haze -> "Neblina"
    }
    AppLanguage.Russian -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Убрать дымку"
        ColorCardCameraPreviewEffect.None -> "Без светового эффекта"
        ColorCardCameraPreviewEffect.BlackMist -> "Черный туман"
        ColorCardCameraPreviewEffect.WhiteMist -> "Белый туман"
        ColorCardCameraPreviewEffect.Glow -> "Мягкий свет"
        ColorCardCameraPreviewEffect.Haze -> "Дымка"
    }
    AppLanguage.Thai -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "ลบหมอก"
        ColorCardCameraPreviewEffect.None -> "ไม่มีเอฟเฟกต์แสง"
        ColorCardCameraPreviewEffect.BlackMist -> "หมอกดำ"
        ColorCardCameraPreviewEffect.WhiteMist -> "หมอกขาว"
        ColorCardCameraPreviewEffect.Glow -> "แสงนุ่ม"
        ColorCardCameraPreviewEffect.Haze -> "หมอกบาง"
    }
    AppLanguage.Vietnamese -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Khử sương mù"
        ColorCardCameraPreviewEffect.None -> "Không hiệu ứng sáng"
        ColorCardCameraPreviewEffect.BlackMist -> "Sương đen"
        ColorCardCameraPreviewEffect.WhiteMist -> "Sương trắng"
        ColorCardCameraPreviewEffect.Glow -> "Ánh sáng mềm"
        ColorCardCameraPreviewEffect.Haze -> "Sương mờ"
    }
    AppLanguage.Indonesian -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Hilangkan kabut"
        ColorCardCameraPreviewEffect.None -> "Tanpa efek cahaya"
        ColorCardCameraPreviewEffect.BlackMist -> "Kabut hitam"
        ColorCardCameraPreviewEffect.WhiteMist -> "Kabut putih"
        ColorCardCameraPreviewEffect.Glow -> "Cahaya lembut"
        ColorCardCameraPreviewEffect.Haze -> "Kabut"
    }
    AppLanguage.Arabic -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "إزالة الضباب"
        ColorCardCameraPreviewEffect.None -> "بدون تأثير ضوئي"
        ColorCardCameraPreviewEffect.BlackMist -> "ضباب أسود"
        ColorCardCameraPreviewEffect.WhiteMist -> "ضباب أبيض"
        ColorCardCameraPreviewEffect.Glow -> "توهج ناعم"
        ColorCardCameraPreviewEffect.Haze -> "ضباب خفيف"
    }
    AppLanguage.Ukrainian -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Прибрати серпанок"
        ColorCardCameraPreviewEffect.None -> "Без світлового ефекту"
        ColorCardCameraPreviewEffect.BlackMist -> "Чорний серпанок"
        ColorCardCameraPreviewEffect.WhiteMist -> "Білий серпанок"
        ColorCardCameraPreviewEffect.Glow -> "М'яке сяйво"
        ColorCardCameraPreviewEffect.Haze -> "Серпанок"
    }
    AppLanguage.Polish -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Usuń zamglenie"
        ColorCardCameraPreviewEffect.None -> "Bez efektu światła"
        ColorCardCameraPreviewEffect.BlackMist -> "Czarna mgiełka"
        ColorCardCameraPreviewEffect.WhiteMist -> "Biała mgiełka"
        ColorCardCameraPreviewEffect.Glow -> "Miękka poświata"
        ColorCardCameraPreviewEffect.Haze -> "Zamglenie"
    }
    AppLanguage.Czech -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Odstranit zamlžení"
        ColorCardCameraPreviewEffect.None -> "Bez světelného efektu"
        ColorCardCameraPreviewEffect.BlackMist -> "Černý opar"
        ColorCardCameraPreviewEffect.WhiteMist -> "Bílý opar"
        ColorCardCameraPreviewEffect.Glow -> "Měkká záře"
        ColorCardCameraPreviewEffect.Haze -> "Opar"
    }
    AppLanguage.Dutch -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Nevel verwijderen"
        ColorCardCameraPreviewEffect.None -> "Geen lichteffect"
        ColorCardCameraPreviewEffect.BlackMist -> "Zwarte nevel"
        ColorCardCameraPreviewEffect.WhiteMist -> "Witte nevel"
        ColorCardCameraPreviewEffect.Glow -> "Zachte gloed"
        ColorCardCameraPreviewEffect.Haze -> "Nevel"
    }
    AppLanguage.Swedish -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Ta bort dis"
        ColorCardCameraPreviewEffect.None -> "Ingen ljuseffekt"
        ColorCardCameraPreviewEffect.BlackMist -> "Svart dimma"
        ColorCardCameraPreviewEffect.WhiteMist -> "Vit dimma"
        ColorCardCameraPreviewEffect.Glow -> "Mjukt sken"
        ColorCardCameraPreviewEffect.Haze -> "Dis"
    }
    AppLanguage.Danish -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Fjern dis"
        ColorCardCameraPreviewEffect.None -> "Ingen lyseffekt"
        ColorCardCameraPreviewEffect.BlackMist -> "Sort tåge"
        ColorCardCameraPreviewEffect.WhiteMist -> "Hvid tåge"
        ColorCardCameraPreviewEffect.Glow -> "Blød glød"
        ColorCardCameraPreviewEffect.Haze -> "Dis"
    }
    AppLanguage.Norwegian -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Fjern dis"
        ColorCardCameraPreviewEffect.None -> "Ingen lyseffekt"
        ColorCardCameraPreviewEffect.BlackMist -> "Svart tåke"
        ColorCardCameraPreviewEffect.WhiteMist -> "Hvit tåke"
        ColorCardCameraPreviewEffect.Glow -> "Myk glød"
        ColorCardCameraPreviewEffect.Haze -> "Dis"
    }
    AppLanguage.Finnish -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Poista usva"
        ColorCardCameraPreviewEffect.None -> "Ei valotehostetta"
        ColorCardCameraPreviewEffect.BlackMist -> "Musta usva"
        ColorCardCameraPreviewEffect.WhiteMist -> "Valkoinen usva"
        ColorCardCameraPreviewEffect.Glow -> "Pehmeä hehku"
        ColorCardCameraPreviewEffect.Haze -> "Utu"
    }
    AppLanguage.Greek -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Αφαίρεση ομίχλης"
        ColorCardCameraPreviewEffect.None -> "Χωρίς εφέ φωτός"
        ColorCardCameraPreviewEffect.BlackMist -> "Μαύρη ομίχλη"
        ColorCardCameraPreviewEffect.WhiteMist -> "Λευκή ομίχλη"
        ColorCardCameraPreviewEffect.Glow -> "Απαλή λάμψη"
        ColorCardCameraPreviewEffect.Haze -> "Αχλή"
    }
    AppLanguage.Romanian -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Elimină ceața"
        ColorCardCameraPreviewEffect.None -> "Fără efect de lumină"
        ColorCardCameraPreviewEffect.BlackMist -> "Ceață neagră"
        ColorCardCameraPreviewEffect.WhiteMist -> "Ceață albă"
        ColorCardCameraPreviewEffect.Glow -> "Strălucire fină"
        ColorCardCameraPreviewEffect.Haze -> "Pâclă"
    }
    AppLanguage.Hungarian -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Pára eltávolítása"
        ColorCardCameraPreviewEffect.None -> "Nincs fényeffekt"
        ColorCardCameraPreviewEffect.BlackMist -> "Fekete köd"
        ColorCardCameraPreviewEffect.WhiteMist -> "Fehér köd"
        ColorCardCameraPreviewEffect.Glow -> "Lágy ragyogás"
        ColorCardCameraPreviewEffect.Haze -> "Homály"
    }
    AppLanguage.Turkish -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Pus giderme"
        ColorCardCameraPreviewEffect.None -> "Işık efekti yok"
        ColorCardCameraPreviewEffect.BlackMist -> "Siyah sis"
        ColorCardCameraPreviewEffect.WhiteMist -> "Beyaz sis"
        ColorCardCameraPreviewEffect.Glow -> "Yumuşak parlama"
        ColorCardCameraPreviewEffect.Haze -> "Pus"
    }
    AppLanguage.Malay -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Buang kabus"
        ColorCardCameraPreviewEffect.None -> "Tiada kesan cahaya"
        ColorCardCameraPreviewEffect.BlackMist -> "Kabus hitam"
        ColorCardCameraPreviewEffect.WhiteMist -> "Kabus putih"
        ColorCardCameraPreviewEffect.Glow -> "Cahaya lembut"
        ColorCardCameraPreviewEffect.Haze -> "Jerebu"
    }
    AppLanguage.Hebrew -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "הסרת אובך"
        ColorCardCameraPreviewEffect.None -> "ללא אפקט אור"
        ColorCardCameraPreviewEffect.BlackMist -> "ערפל שחור"
        ColorCardCameraPreviewEffect.WhiteMist -> "ערפל לבן"
        ColorCardCameraPreviewEffect.Glow -> "זוהר רך"
        ColorCardCameraPreviewEffect.Haze -> "אובך"
    }
    AppLanguage.English -> when (this) {
        ColorCardCameraPreviewEffect.Dehaze -> "Dehaze"
        ColorCardCameraPreviewEffect.None -> "No light effect"
        ColorCardCameraPreviewEffect.BlackMist -> "Black Mist"
        ColorCardCameraPreviewEffect.WhiteMist -> "White Mist"
        ColorCardCameraPreviewEffect.Glow -> "Glow"
        ColorCardCameraPreviewEffect.Haze -> "Haze"
    }
}

private enum class ColorCardCameraPreviewGestureMode {
    HorizontalSwipe,
    ExposureDrag,
    Cancelled
}

private data class ColorCardCameraStabilizationConfig(
    val videoMode: Int? = null,
    val opticalMode: Int? = null,
    val supportedVideoModes: List<Int> = emptyList(),
    val supportedOpticalModes: List<Int> = emptyList()
)

private data class ColorCardCameraStabilizationRuntimeState(
    val requestedVideoMode: Int? = null,
    val requestedOpticalMode: Int? = null,
    val activeVideoMode: Int? = null,
    val activeOpticalMode: Int? = null,
    val supportedVideoModes: List<Int> = emptyList(),
    val supportedOpticalModes: List<Int> = emptyList()
)

@Composable
fun ColorCardCameraPage(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val appLanguage = LocalAppLanguage.current
    val commonUiCopy = remember(appLanguage) { getCommonGlobalUiCopy(appLanguage) }
    val strings = LocalAppStrings.current
    val cameraPermissionSubtitle = remember(appLanguage) {
        when (appLanguage) {
            AppLanguage.SimplifiedChinese -> "需要相机权限以开始取景和拍摄"
            AppLanguage.TraditionalChinese -> "需要相機權限以開始取景和拍攝"
            AppLanguage.Korean -> "미리보기와 촬영을 시작하려면 카메라 권한이 필요합니다"
            AppLanguage.Japanese -> "プレビューと撮影を開始するにはカメラ権限が必要です"
            AppLanguage.French -> "L'accès à la caméra est nécessaire pour démarrer l'aperçu et la prise de vue"
            AppLanguage.German -> "Für Vorschau und Aufnahme wird Kamerazugriff benötigt"
            AppLanguage.Italian -> "È necessaria l'autorizzazione della fotocamera per avviare anteprima e scatto"
            AppLanguage.Portuguese -> "É necessário acesso à câmera para iniciar a visualização e a captura"
            AppLanguage.Spanish -> "Se necesita acceso a la cámara para iniciar la vista previa y la captura"
            AppLanguage.Russian -> "Чтобы начать предпросмотр и съёмку, нужен доступ к камере"
            AppLanguage.Thai -> "ต้องใช้สิทธิ์กล้องเพื่อเริ่มพรีวิวและถ่ายภาพ"
            AppLanguage.Vietnamese -> "Cần quyền truy cập camera để bắt đầu xem trước và chụp ảnh"
            AppLanguage.Indonesian -> "Akses kamera diperlukan untuk memulai pratinjau dan pengambilan gambar"
            AppLanguage.Arabic -> "يلزم إذن الكاميرا لبدء المعاينة والالتقاط"
            AppLanguage.Ukrainian -> "Для перегляду та зйомки потрібен доступ до камери"
            AppLanguage.Polish -> "Do podglądu i robienia zdjęć wymagany jest dostęp do aparatu"
            AppLanguage.Czech -> "Pro zahájení náhledu a focení je potřeba přístup k fotoaparátu"
            AppLanguage.Dutch -> "Cameratoegang is nodig om voorbeeld en opname te starten"
            AppLanguage.Swedish -> "Kameraåtkomst krävs för att starta förhandsvisning och fotografering"
            AppLanguage.Danish -> "Der kræves adgang til kameraet for at starte forhåndsvisning og optagelse"
            AppLanguage.Norwegian -> "Kameratilgang kreves for å starte forhåndsvisning og opptak"
            AppLanguage.Finnish -> "Kameran käyttöoikeus tarvitaan esikatselun ja kuvaamisen aloittamiseen"
            AppLanguage.Greek -> "Απαιτείται πρόσβαση στην κάμερα για την έναρξη της προεπισκόπησης και της λήψης"
            AppLanguage.Romanian -> "Este necesar accesul la cameră pentru a porni previzualizarea și fotografierea"
            AppLanguage.Hungarian -> "Az előnézet és a fényképezés indításához kamera-hozzáférés szükséges"
            AppLanguage.Turkish -> "Önizlemeyi ve çekimi başlatmak için kamera erişimi gerekli"
            AppLanguage.Malay -> "Akses kamera diperlukan untuk memulakan pratonton dan penangkapan"
            AppLanguage.Hebrew -> "נדרשת גישה למצלמה כדי להתחיל תצוגה מקדימה וצילום"
            AppLanguage.English -> "Camera access is required to start preview and capture"
        }
    }
    fun performCameraControlHaptic() {
        if (hapticsAllowed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiPrefs = remember(context) { colorCardCameraUiPrefs(context) }
    val activity = remember(context) { context.findColorCardCameraActivity() }
    val orientationState = rememberColorCardCameraOrientationState(context)
    val targetRotation = orientationState.deviceRotation
    val controlsRotationDegrees = orientationState.controlsRotationDegrees
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraStartupReady by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            cameraStartupReady = false
            return@LaunchedEffect
        }
        cameraStartupReady = false
        withFrameNanos { }
        delay(COLOR_CARD_CAMERA_STARTUP_DELAY_MS)
        cameraStartupReady = true
    }
    val filterState = rememberColorCardCameraFilterState()
    val recentFilterUsageStore = remember(context) { ColorCardRecentFilterUsageStore.get(context) }
    val favoriteFilterStore = remember(context) { ColorCardFavoriteFilterStore.get(context) }
    val recentFilterIds by recentFilterUsageStore.topFilterIds.collectAsState()
    val favoriteFilterIds by favoriteFilterStore.favoriteFilterIds.collectAsState()
    var expandedFilterPicker by remember { mutableStateOf(false) }
    val browserPreviewBitmaps = rememberColorCardCameraExpandedPreviewBitmaps(
        enabled = filterState.browserCandidates.isNotEmpty(),
        candidates = filterState.browserCandidates
    )
    var lensMode by remember { mutableStateOf(ColorCardCameraLensMode.Native) }
    var eqZoomTargetFocalMm by remember { mutableIntStateOf(0) }
    var eqZoomTargetFocalContinuous by remember { mutableFloatStateOf(0f) }
    var capturePending by remember { mutableStateOf(false) }
    var queuePendingCount by remember { mutableIntStateOf(0) }
    var captureRequestNonce by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var flashEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean(COLOR_CARD_CAMERA_PREF_FLASH_ENABLED, false))
    }
    var meteringExposureBiasEv by remember { mutableStateOf(0f) }
    var exposureBiasCapabilities by remember { mutableStateOf(ColorCardCameraExposureBiasCapabilities()) }
    var cameraSessionBinding by remember { mutableStateOf(false) }
    val cameraUiReady = hasCameraPermission && cameraStartupReady
    val cameraOperationalReady = cameraUiReady && !cameraSessionBinding && !capturePending
    var previewHostView by remember { mutableStateOf<ColorCardCameraRealtimePreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var effectiveCropCoverage by remember { mutableStateOf(1f) }
    var previewRecoveryNonce by remember { mutableIntStateOf(0) }
    var previewRestartNonce by remember { mutableIntStateOf(0) }
    var lastPreviewRecoveryAtMs by remember { mutableStateOf(0L) }
    var previewTrackedFaces by remember { mutableStateOf(ColorCardCameraTrackedFacesFrame.Empty) }
    var previewDebugInfo by remember { mutableStateOf<ColorCardCameraDebugInfo?>(null) }
    var previewPipelineDebugInfo by remember { mutableStateOf(ColorCardCameraPreviewPipelineDebugInfo()) }
    val previewFaceRegions = remember(previewTrackedFaces) {
        previewTrackedFaces.protectionFaceRegions
    }
    var focusRequest by remember { mutableStateOf<ColorCardCameraFocusRequest?>(null) }
    var focusIndicator by remember { mutableStateOf<ColorCardCameraFocusIndicator?>(null) }
    var previewNeedsResumeRecovery by remember { mutableStateOf(false) }
    var previewEffect by remember { mutableStateOf(ColorCardCameraPreviewEffect.None) }
    var previewGridEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean(COLOR_CARD_CAMERA_PREF_GRID_ENABLED, false))
    }
    var previewBeautyEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean(COLOR_CARD_CAMERA_PREF_BEAUTY_ENABLED, false))
    }
    var eqEnhancementEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean(COLOR_CARD_CAMERA_PREF_EQ_ENHANCEMENT_ENABLED, false))
    }
    var showExperimentalNoticeDialog by remember {
        mutableStateOf(!uiPrefs.getBoolean(COLOR_CARD_CAMERA_PREF_EXPERIMENTAL_NOTICE_SHOWN, false))
    }
    var previewAspectRatio by remember { mutableStateOf(COLOR_CARD_CAMERA_OUTPUT_ASPECT_RATIO) }
    var previewEffectAnimating by remember { mutableStateOf(false) }
    var pendingCapturePortraitAdjustments by remember {
        mutableStateOf(resolveColorCardCameraPortraitAdjustments())
    }
    val previewBeautyAvailability = remember(previewTrackedFaces) {
        resolveColorCardCameraBeautyAvailability(previewTrackedFaces)
    }
    val previewBeautyActive = remember(previewBeautyEnabled, previewBeautyAvailability) {
        previewBeautyEnabled && previewBeautyAvailability.ready
    }
    val shouldSuppressEqEnhancement = remember(useFrontCamera, lensMode, eqEnhancementEnabled) {
        colorCardCameraShouldSuppressEqEnhancement(
            useFrontCamera = useFrontCamera,
            lensMode = lensMode,
            eqEnhancementEnabled = eqEnhancementEnabled
        )
    }
    val capturedItems = remember { mutableStateListOf<ColorCardCameraCapturedItem>() }
    var cameraGalleryRefreshToken by remember { mutableIntStateOf(0) }
    var previewAnchorUri by remember { mutableStateOf<String?>(null) }
    var hiddenPreviewSourceUri by remember { mutableStateOf<String?>(null) }
    var latestCaptureButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val savedCameraItems by produceState<List<MediaItem>>(
        initialValue = emptyList(),
        key1 = cameraGalleryRefreshToken
    ) {
        value = loadColorCardCameraSavedMedia(context)
    }
    val previewItems = buildColorCardCameraPreviewItems(
        capturedItems = capturedItems.toList(),
        savedItems = savedCameraItems
    )
    val previewStartIndex = remember(previewItems, previewAnchorUri) {
        findColorCardCameraPreviewIndex(previewItems, previewAnchorUri)
    }
    val performanceMode by produceState(initialValue = AppPerformanceGovernor.currentMode()) {
        while (true) {
            val nextMode = AppPerformanceGovernor.currentMode()
            if (value != nextMode) {
                value = nextMode
            }
            delay(1_500L)
        }
    }
    val lensSelection = remember(cameraProvider, lensMode, useFrontCamera, eqZoomTargetFocalMm) {
        resolveColorCardCameraLensSelection(
            context = context,
            provider = cameraProvider,
            lensMode = lensMode,
            useFrontCamera = useFrontCamera,
            eqZoomTargetFocalMm = if (
                lensMode == ColorCardCameraLensMode.Eq35 ||
                lensMode == ColorCardCameraLensMode.Eq50
            ) {
                eqZoomTargetFocalMm
            } else {
                null
            }
        )
    }
    val eqZoomRangeMinFocalMm = remember(lensSelection.nativeFocal35mmEq) {
        resolveColorCardCameraEqZoomMinFocalMm(lensSelection.nativeFocal35mmEq)
    }
    val eqZoomRangeMaxFocalMm = remember(eqZoomRangeMinFocalMm) {
        max(eqZoomRangeMinFocalMm, COLOR_CARD_CAMERA_EQ_ZOOM_MAX_FOCAL_MM)
    }
    val eqZoomAnchor35Mm = remember(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm) {
        35.coerceIn(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm)
    }
    val eqZoomAnchor50Mm = remember(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm) {
        50.coerceIn(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm)
    }
    val hasEq35Mode = ColorCardCameraLensMode.Eq35 in lensSelection.availableModes
    val hasEq50Mode = ColorCardCameraLensMode.Eq50 in lensSelection.availableModes
    val lensSelectorLabels = remember(
        lensSelection.displayLabels,
        lensSelection.availableModes,
        lensMode,
        eqZoomTargetFocalMm,
        eqZoomRangeMinFocalMm,
        eqZoomRangeMaxFocalMm,
        eqZoomAnchor35Mm,
        eqZoomAnchor50Mm
    ) {
        val labels = lensSelection.displayLabels.toMutableMap()
        val currentFocal = eqZoomTargetFocalMm.coerceIn(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm)
        if (ColorCardCameraLensMode.Eq35 in lensSelection.availableModes) {
            labels[ColorCardCameraLensMode.Eq35] = when (lensMode) {
                ColorCardCameraLensMode.Eq35 -> currentFocal.toString()
                else -> eqZoomAnchor35Mm.toString()
            }
        }
        if (ColorCardCameraLensMode.Eq50 in lensSelection.availableModes) {
            labels[ColorCardCameraLensMode.Eq50] = when (lensMode) {
                ColorCardCameraLensMode.Eq50 -> currentFocal.toString()
                else -> eqZoomAnchor50Mm.toString()
            }
        }
        labels
    }
    val eqPinchZoomEnabled = !useFrontCamera &&
        (hasEq35Mode || hasEq50Mode) &&
        (
            lensMode == ColorCardCameraLensMode.Native ||
                lensMode == ColorCardCameraLensMode.Eq35 ||
                lensMode == ColorCardCameraLensMode.Eq50
            )
    val handleEqPinchZoom: (Float) -> Unit = pinchZoom@{ rawScale ->
        if (!eqPinchZoomEnabled) {
            return@pinchZoom
        }
        val safeScale = rawScale.coerceIn(
            COLOR_CARD_CAMERA_EQ_ZOOM_SCALE_MIN,
            COLOR_CARD_CAMERA_EQ_ZOOM_SCALE_MAX
        )
        val currentFocal = if (eqZoomTargetFocalContinuous > 0f) {
            eqZoomTargetFocalContinuous
        } else if (lensMode == ColorCardCameraLensMode.Native) {
            eqZoomRangeMinFocalMm.toFloat()
        } else {
            eqZoomTargetFocalMm.toFloat().coerceAtLeast(eqZoomRangeMinFocalMm.toFloat())
        }
        // Reverse gesture direction:
        // pinch-in (scale < 1) pushes focal upward, pinch-out (scale > 1) pulls it downward.
        val nextFocal = (currentFocal / safeScale)
            .coerceIn(eqZoomRangeMinFocalMm.toFloat(), eqZoomRangeMaxFocalMm.toFloat())
        eqZoomTargetFocalContinuous = nextFocal
        val roundedFocal = nextFocal.roundToInt().coerceIn(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm)
        if (roundedFocal != eqZoomTargetFocalMm) {
            eqZoomTargetFocalMm = roundedFocal
        }
        when (lensMode) {
            ColorCardCameraLensMode.Native -> {
                when {
                    hasEq50Mode && roundedFocal >= eqZoomAnchor50Mm -> {
                        eqZoomTargetFocalMm = eqZoomAnchor50Mm
                        eqZoomTargetFocalContinuous = eqZoomAnchor50Mm.toFloat()
                        lensMode = ColorCardCameraLensMode.Eq50
                    }
                    hasEq35Mode && roundedFocal > eqZoomRangeMinFocalMm -> {
                        eqZoomTargetFocalMm = roundedFocal
                        eqZoomTargetFocalContinuous = roundedFocal.toFloat()
                        lensMode = ColorCardCameraLensMode.Eq35
                    }
                    hasEq50Mode && roundedFocal > eqZoomRangeMinFocalMm -> {
                        eqZoomTargetFocalMm = roundedFocal
                        eqZoomTargetFocalContinuous = roundedFocal.toFloat()
                        lensMode = ColorCardCameraLensMode.Eq50
                    }
                    else -> {
                        eqZoomTargetFocalMm = eqZoomRangeMinFocalMm
                        eqZoomTargetFocalContinuous = eqZoomRangeMinFocalMm.toFloat()
                    }
                }
            }
            ColorCardCameraLensMode.Eq35 -> {
                when {
                    roundedFocal >= eqZoomAnchor50Mm -> {
                        eqZoomTargetFocalMm = eqZoomAnchor50Mm
                        eqZoomTargetFocalContinuous = eqZoomAnchor50Mm.toFloat()
                        lensMode = ColorCardCameraLensMode.Eq50
                    }
                    roundedFocal <= eqZoomRangeMinFocalMm -> {
                        eqZoomTargetFocalMm = eqZoomRangeMinFocalMm
                        eqZoomTargetFocalContinuous = eqZoomRangeMinFocalMm.toFloat()
                        lensMode = ColorCardCameraLensMode.Native
                    }
                }
            }
            ColorCardCameraLensMode.Eq50 -> {
                when {
                    roundedFocal <= eqZoomAnchor35Mm -> {
                        eqZoomTargetFocalMm = eqZoomAnchor35Mm
                        eqZoomTargetFocalContinuous = eqZoomAnchor35Mm.toFloat()
                        lensMode = ColorCardCameraLensMode.Eq35
                    }
                    else -> {
                        eqZoomTargetFocalMm = roundedFocal
                        eqZoomTargetFocalContinuous = roundedFocal.toFloat()
                    }
                }
            }
            else -> Unit
        }
    }
    val useCaseProfile = remember(performanceMode, lensMode, useFrontCamera) {
        resolveColorCardCameraUseCaseProfile(
            perfMode = performanceMode,
            lensMode = lensMode,
            useFrontCamera = useFrontCamera,
            outputAspectRatio = COLOR_CARD_CAMERA_OUTPUT_ASPECT_RATIO
        )
    }
    val capturePortraitAdjustments = remember(previewBeautyEnabled) {
        resolveColorCardCameraPortraitAdjustments(previewBeautyEnabled)
    }
    val previewActivePortraitAdjustments = remember(previewBeautyActive) {
        resolveColorCardCameraPortraitAdjustments(previewBeautyActive)
    }
    val useTelePhotoPreviewBeautySafeMode = remember(
        useFrontCamera,
        lensSelection.cameraId,
        lensSelection.physicalCameraId,
        previewBeautyActive
    ) {
        previewBeautyActive &&
            !useFrontCamera &&
            isColorCardCameraPhysicalSubLens(
                cameraId = lensSelection.cameraId,
                physicalCameraId = lensSelection.physicalCameraId
            )
    }
    val previewPortraitAdjustments = remember(
        previewActivePortraitAdjustments,
        useTelePhotoPreviewBeautySafeMode
    ) {
        if (useTelePhotoPreviewBeautySafeMode) {
            ColorCardTracePortraitAdjustments()
        } else {
            previewActivePortraitAdjustments
        }
    }
    val previewRenderFaceRegions = remember(previewFaceRegions, useTelePhotoPreviewBeautySafeMode) {
        if (useTelePhotoPreviewBeautySafeMode) {
            emptyList()
        } else {
            previewFaceRegions
        }
    }
    val renderAdjustments = remember {
        ColorCardCameraRenderAdjustments(
            whiteBalanceShift = 0f,
            exposureCompensation = 0f
        )
    }
    val previewBaseParams = remember(
        filterState.renderSelectedCandidate?.paletteId,
        previewFaceRegions,
        filterState.color,
        filterState.tone,
        filterState.sharpen,
        filterState.saturation,
        filterState.brightness,
        filterState.strength,
        filterState.renderHaldBitmap,
        filterState.renderHaldSecondaryBitmap,
        filterState.renderHaldVariantMix,
        previewRenderFaceRegions,
        previewPortraitAdjustments,
        renderAdjustments,
        useCaseProfile.previewTargetSize
    ) {
        buildColorCardCameraRenderParams(
            filterState = filterState,
            sourceWidth = useCaseProfile.previewTargetSize.width,
            sourceHeight = useCaseProfile.previewTargetSize.height,
            faceRegions = previewRenderFaceRegions,
            portraitAdjustments = previewPortraitAdjustments,
            renderAdjustments = renderAdjustments
        )
    }
    val previewParams = remember(previewBaseParams, previewEffect, shouldSuppressEqEnhancement) {
        val effected = applyColorCardCameraPreviewEffect(
            params = previewBaseParams,
            effect = previewEffect
        )
        if (shouldSuppressEqEnhancement) {
            effected.copy(sharpen = 0f)
        } else {
            effected
        }
    }
    val effectiveCaptureFilterState = remember(filterState, shouldSuppressEqEnhancement) {
        if (shouldSuppressEqEnhancement) {
            filterState.copy(sharpen = 0f)
        } else {
            filterState
        }
    }
    LaunchedEffect(lensSelection.cropCoverage) {
        effectiveCropCoverage = lensSelection.cropCoverage
    }
    LaunchedEffect(Unit) {
        previewAspectRatio = COLOR_CARD_CAMERA_OUTPUT_ASPECT_RATIO
    }
    LaunchedEffect(lensSelection.availableModes) {
        if (lensMode !in lensSelection.availableModes) {
            lensMode = lensSelection.availableModes.firstOrNull() ?: ColorCardCameraLensMode.Native
        }
    }
    LaunchedEffect(
        lensMode,
        eqZoomRangeMinFocalMm,
        eqZoomRangeMaxFocalMm,
        eqZoomAnchor35Mm,
        eqZoomAnchor50Mm
    ) {
        when (lensMode) {
            ColorCardCameraLensMode.Eq35 -> {
                if (eqZoomTargetFocalMm <= 0) {
                    eqZoomTargetFocalMm = eqZoomAnchor35Mm
                }
                eqZoomTargetFocalMm = eqZoomTargetFocalMm
                    .coerceIn(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm)
                eqZoomTargetFocalContinuous = eqZoomTargetFocalMm.toFloat()
            }
            ColorCardCameraLensMode.Eq50 -> {
                if (eqZoomTargetFocalMm <= 0) {
                    eqZoomTargetFocalMm = eqZoomAnchor50Mm
                }
                eqZoomTargetFocalMm = eqZoomTargetFocalMm
                    .coerceIn(eqZoomRangeMinFocalMm, eqZoomRangeMaxFocalMm)
                eqZoomTargetFocalContinuous = eqZoomTargetFocalMm.toFloat()
            }
            else -> {
                eqZoomTargetFocalMm = eqZoomRangeMinFocalMm
                eqZoomTargetFocalContinuous = eqZoomRangeMinFocalMm.toFloat()
            }
        }
    }
    LaunchedEffect(useFrontCamera) {
        previewTrackedFaces = ColorCardCameraTrackedFacesFrame.Empty
        focusRequest = null
        focusIndicator = null
    }
    LaunchedEffect(flashEnabled) {
        uiPrefs.edit().putBoolean(COLOR_CARD_CAMERA_PREF_FLASH_ENABLED, flashEnabled).apply()
    }
    LaunchedEffect(previewGridEnabled) {
        uiPrefs.edit().putBoolean(COLOR_CARD_CAMERA_PREF_GRID_ENABLED, previewGridEnabled).apply()
    }
    LaunchedEffect(previewBeautyEnabled) {
        uiPrefs.edit().putBoolean(COLOR_CARD_CAMERA_PREF_BEAUTY_ENABLED, previewBeautyEnabled).apply()
    }
    LaunchedEffect(
        previewBeautyEnabled,
        previewBeautyActive,
        previewBeautyAvailability.reason,
        previewTrackedFaces.faces.size,
        previewTrackedFaces.stableFrameCount,
        previewTrackedFaces.isHeld,
        lensMode,
        useFrontCamera
    ) {
        Log.d(
            COLOR_CARD_CAMERA_LOG_TAG,
            "Beauty state enabled=$previewBeautyEnabled active=$previewBeautyActive " +
                "reason=${previewBeautyAvailability.reason} faces=${previewTrackedFaces.faces.size} " +
                "stable=${previewTrackedFaces.stableFrameCount} held=${previewTrackedFaces.isHeld} " +
                "front=$useFrontCamera lens=$lensMode telePreviewSafe=$useTelePhotoPreviewBeautySafeMode"
        )
    }
    LaunchedEffect(lensMode, useFrontCamera) {
        focusRequest = null
        focusIndicator = null
        previewTrackedFaces = ColorCardCameraTrackedFacesFrame.Empty
    }
    val renderQueue = remember(context) {
        ColorCardCameraRenderQueue(
            context = context.applicationContext,
            onRenderQueued = {
                queuePendingCount += 1
                capturedItems.add(
                    0,
                    ColorCardCameraCapturedItem(
                        id = it.id,
                        previewUri = it.previewUri,
                        pending = true
                    )
                )
            },
            onRenderFinished = { id, finalUri ->
                queuePendingCount = (queuePendingCount - 1).coerceAtLeast(0)
                val index = capturedItems.indexOfFirst { it.id == id }
                if (index >= 0) {
                    capturedItems[index] = capturedItems[index].copy(
                        finalUri = finalUri?.toString(),
                        pending = false
                    )
                }
                cameraGalleryRefreshToken += 1
            }
        )
    }
    BackHandler {
        if (expandedFilterPicker) {
            expandedFilterPicker = false
        } else {
            onBack()
        }
    }

    var lastObservedCameraPermission by remember { mutableStateOf(hasCameraPermission) }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !lastObservedCameraPermission) {
            previewNeedsResumeRecovery = false
            previewHostView?.resetPreviewPipeline()
            cameraProvider?.unbindAll()
            previewHostView = null
            focusRequest = null
            focusIndicator = null
            previewRestartNonce += 1
            previewRecoveryNonce += 1
        } else if (!hasCameraPermission && lastObservedCameraPermission) {
            cameraProvider?.unbindAll()
            previewHostView = null
            previewTrackedFaces = ColorCardCameraTrackedFacesFrame.Empty
            cameraSessionBinding = false
            focusRequest = null
            focusIndicator = null
        }
        lastObservedCameraPermission = hasCameraPermission
    }

    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        val decorView = window?.decorView
        val controller = if (window != null && decorView != null) {
            WindowInsetsControllerCompat(window, decorView)
        } else {
            null
        }
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            if (window != null) {
                controller?.show(WindowInsetsCompat.Type.statusBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    previewNeedsResumeRecovery = true
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (previewNeedsResumeRecovery && hasCameraPermission) {
                        previewNeedsResumeRecovery = false
                        previewHostView?.resetPreviewPipeline()
                        cameraProvider?.unbindAll()
                        previewHostView = null
                        focusRequest = null
                        focusIndicator = null
                        previewRestartNonce += 1
                        previewRecoveryNonce += 1
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val horizontalPadding = 2.dp
        val topSpacing = 28.dp
        val previewToPanelGap = 10.dp
        val panelToShutterGap = 0.dp
        val filterPanelFixedHeight = 208.dp
        val shutterRowHeight = 84.dp
        val shutterRowTopPadding = 0.dp
        val shutterControlsVerticalOffset = 6.dp
        val utilityButtonSize = 52.dp
        val topUtilityButtonWidth = 42.dp
        val topUtilityButtonHeight = 36.dp
        val shutterButtonSize = 68.dp
        val navigationBarBottomPadding = with(density) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        val bottomPadding = 8.dp + navigationBarBottomPadding
        val shutterRowContainerHeight = shutterRowHeight + bottomPadding
        val maxShutterButtonSize = (shutterRowHeight - shutterRowTopPadding).coerceAtLeast(64.dp)
        val resolvedShutterButtonSize = shutterButtonSize.coerceAtMost(maxShutterButtonSize)
        val maxShutterOffset = (
            ((shutterRowHeight - shutterRowTopPadding - resolvedShutterButtonSize) / 2f) +
                bottomPadding
            ).coerceAtLeast(0.dp)
        val resolvedShutterControlsVerticalOffset =
            shutterControlsVerticalOffset.coerceAtMost(maxShutterOffset)
        val availablePreviewHeight = (maxHeight -
            topSpacing -
            previewToPanelGap -
            filterPanelFixedHeight -
            panelToShutterGap -
            shutterRowHeight -
            bottomPadding).coerceAtLeast(220.dp)
        val previewOuterWidth = (maxWidth - (horizontalPadding * 2f)).coerceAtLeast(1.dp)
        val contentWidth = previewOuterWidth
        val previewHeight = availablePreviewHeight
        val safePreviewAspectRatio = previewAspectRatio.coerceIn(0.3f, 1.5f)
        val previewOuterRatio = (contentWidth.value / previewHeight.value).coerceAtLeast(0.0001f)
        val previewContentWidth: androidx.compose.ui.unit.Dp
        val previewContentHeight: androidx.compose.ui.unit.Dp
        if (previewOuterRatio > safePreviewAspectRatio) {
            previewContentHeight = previewHeight
            previewContentWidth = (previewHeight * safePreviewAspectRatio).coerceAtMost(contentWidth)
        } else {
            previewContentWidth = contentWidth
            previewContentHeight = (contentWidth / safePreviewAspectRatio).coerceAtMost(previewHeight)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(topSpacing))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF121314).copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (cameraUiReady) {
                    Box(
                        modifier = Modifier
                            .width(previewContentWidth)
                            .height(previewContentHeight)
                            .align(Alignment.Center)
                    ) {
                        ColorCardCameraPreviewSession(
                            provider = cameraProvider,
                            onProviderReady = { cameraProvider = it },
                            previewHostView = previewHostView,
                            cameraSelector = lensSelection.selector,
                            lensMode = lensMode,
                            requestedCameraId = lensSelection.cameraId,
                            requestedPhysicalCameraId = lensSelection.physicalCameraId,
                            requestedCapturePhysicalCameraId = lensSelection.capturePhysicalCameraId,
                            outputAspectRatio = COLOR_CARD_CAMERA_OUTPUT_ASPECT_RATIO,
                            baseCropCoverage = lensSelection.cropCoverage,
                            zoomRatio = lensSelection.zoomRatio,
                            useFrontCamera = useFrontCamera,
                            targetRotation = targetRotation,
                            flashEnabled = flashEnabled && !useFrontCamera,
                            multiFrameFusionEnabled = false,
                            captureRequestNonce = captureRequestNonce,
                            previewRecoveryNonce = previewRecoveryNonce,
                            focusRequest = focusRequest,
                            meteringExposureBiasEv = meteringExposureBiasEv,
                            useCaseProfile = useCaseProfile,
                            faceTrackingEnabled = cameraUiReady && !capturePending,
                            onBindingStateChange = {
                                cameraSessionBinding = it
                            },
                            onFaceTrackingFrameChange = { trackedFaces ->
                                previewTrackedFaces = trackedFaces
                            },
                            onEffectiveCropCoverageChange = { effectiveCropCoverage = it },
                            onExposureBiasCapabilitiesChange = { capabilities ->
                                exposureBiasCapabilities = capabilities
                                meteringExposureBiasEv = capabilities.coerce(meteringExposureBiasEv)
                                focusIndicator = focusIndicator?.let { indicator ->
                                    indicator.copy(
                                        exposureBiasEv = capabilities.coerce(indicator.exposureBiasEv)
                                    )
                                }
                            },
                            onDebugInfoChange = { debugInfo ->
                                previewDebugInfo = debugInfo
                            },
                            onCaptureStateChange = { pending ->
                                capturePending = pending
                            },
                            onCaptured = { captureResult ->
                                val itemId = colorCardCameraCaptureId.getAndIncrement()
                                val exportCropCoverage = if (!useFrontCamera && lensMode == ColorCardCameraLensMode.Native) {
                                    1f
                                } else {
                                    effectiveCropCoverage
                                }
                                when (captureResult) {
                                    is ColorCardCameraCaptureResult.LegacyJpeg -> {
                                        renderQueue.enqueue(
                                            ColorCardCameraPendingRender(
                                                id = itemId,
                                                rawFile = captureResult.rawFile,
                                                jpegFile = captureResult.jpegFile,
                                                burstJpegFiles = captureResult.burstJpegFiles,
                                                previewUri = Uri.fromFile(captureResult.jpegFile).toString(),
                                                useFrontCamera = useFrontCamera,
                                                cropCoverage = exportCropCoverage,
                                                enhancementCoverage = lensSelection.enhancementCoverage,
                                                enhancementEnabled = !shouldSuppressEqEnhancement,
                                                requestedFocal35mmEq = lensSelection.requestedFocal35mmEq,
                                                renderedFocal35mmEq = lensSelection.requestedFocal35mmEq,
                                                renderAdjustments = ColorCardCameraRenderAdjustments(
                                                    whiteBalanceShift = 0f,
                                                    exposureCompensation = 0f,
                                                    previewEffect = previewEffect,
                                                    portraitAdjustments = pendingCapturePortraitAdjustments
                                                )
                                            ),
                                            filterState = effectiveCaptureFilterState
                                        )
                                    }
                                    is ColorCardCameraCaptureResult.ExperimentalYuv -> {
                                        renderQueue.enqueue(
                                            ColorCardCameraPendingRender(
                                                id = itemId,
                                                rawFile = null,
                                                jpegFile = captureResult.previewFile,
                                                burstJpegFiles = listOf(captureResult.previewFile),
                                                previewUri = Uri.fromFile(captureResult.previewFile).toString(),
                                                useFrontCamera = useFrontCamera,
                                                cropCoverage = exportCropCoverage,
                                                enhancementCoverage = lensSelection.enhancementCoverage,
                                                enhancementEnabled = !shouldSuppressEqEnhancement,
                                                requestedFocal35mmEq = lensSelection.requestedFocal35mmEq,
                                                renderedFocal35mmEq = lensSelection.requestedFocal35mmEq,
                                                renderAdjustments = ColorCardCameraRenderAdjustments(
                                                    whiteBalanceShift = 0f,
                                                    exposureCompensation = 0f,
                                                    previewEffect = previewEffect,
                                                    portraitAdjustments = pendingCapturePortraitAdjustments
                                                )
                                            ),
                                            filterState = effectiveCaptureFilterState
                                        )
                                    }
                                }
                            }
                        )
                        androidx.compose.runtime.key("${useFrontCamera}:${previewRestartNonce}") {
                            ColorCardCameraPreviewRail(
                                contentWidth = previewContentWidth,
                                previewHeight = previewContentHeight,
                                previewParams = previewParams,
                                effectiveCropCoverage = effectiveCropCoverage,
                                framingGuideLabel = null,
                                previewEffect = previewEffect,
                                useCaseProfile = useCaseProfile,
                                previewHostView = previewHostView,
                                previewRestartNonce = previewRestartNonce,
                                cameraSessionBinding = cameraSessionBinding,
                                capturePending = capturePending,
                                previewEffectAnimating = previewEffectAnimating,
                                pinchZoomEnabled = eqPinchZoomEnabled,
                                onPreviewEffectAnimatingChange = { previewEffectAnimating = it },
                                onPreviewEffectChange = { previewEffect = it },
                                onPinchZoom = handleEqPinchZoom,
                                onPreviewAspectRatioChange = { previewAspectRatio = it },
                                onPreviewHostReset = {
                                    val now = SystemClock.uptimeMillis()
                                    if (!cameraSessionBinding && now - lastPreviewRecoveryAtMs >= 1500L) {
                                        lastPreviewRecoveryAtMs = now
                                        previewHostView?.resetPreviewPipeline()
                                        cameraProvider?.unbindAll()
                                        previewHostView = null
                                        previewRestartNonce += 1
                                        previewRecoveryNonce += 1
                                    }
                                },
                                focusIndicator = focusIndicator,
                                exposureBiasCapabilities = exposureBiasCapabilities,
                                onFocusRequest = { request, indicator, exposureBias ->
                                    meteringExposureBiasEv = exposureBias
                                    focusRequest = request
                                    focusIndicator = indicator
                                },
                                onExposureBiasChange = { bias, indicator ->
                                    meteringExposureBiasEv = bias
                                    focusIndicator = indicator
                                },
                                onViewReady = { previewHostView = it },
                                useFrontCamera = useFrontCamera,
                                showGridLines = previewGridEnabled,
                                trackedFacesFrame = previewTrackedFaces,
                                beautyEnabled = previewBeautyEnabled,
                                beautyActive = previewBeautyActive,
                                debugInfo = previewDebugInfo,
                                previewPipelineDebugInfo = previewPipelineDebugInfo,
                                onPreviewDebugInfoChange = { previewPipelineDebugInfo = it }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(previewContentWidth)
                            .height(previewContentHeight)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(28.dp))
                    ) {
                        ColorCardCameraFilterRevealOverlay(
                            revealKey = filterState.renderSelectedCandidate?.paletteId ?: "none",
                            revealTriggerNonce = filterState.revealAnimationNonce,
                            modifier = Modifier.matchParentSize()
                        )
                        if (
                            filterState.filmGrainEnabled &&
                            filterState.filmGrainEligible &&
                            filterState.selectedFilmGrainBitmap != null &&
                            !filterState.selectedFilmGrainBitmap.isRecycled
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = filterState.selectedFilmGrainBitmap.asImageBitmap(),
                                contentDescription = commonUiCopy.filmGrainOverlay,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alpha = COLOR_CARD_TRACE_FILM_GRAIN_BLEND_ALPHA,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                    if (!useFrontCamera) {
                        ColorCardCameraLensSelectorOverlay(
                            selected = lensMode,
                            modes = lensSelection.availableModes,
                            labels = lensSelectorLabels,
                            textRotationDegrees = controlsRotationDegrees,
                            enabled = cameraOperationalReady,
                            onSelected = {
                                if (it != lensMode) {
                                    performCameraControlHaptic()
                                    focusIndicator = null
                                    when (it) {
                                        ColorCardCameraLensMode.Eq35 -> {
                                            eqZoomTargetFocalMm = eqZoomAnchor35Mm
                                            eqZoomTargetFocalContinuous = eqZoomAnchor35Mm.toFloat()
                                        }
                                        ColorCardCameraLensMode.Eq50 -> {
                                            eqZoomTargetFocalMm = eqZoomAnchor50Mm
                                            eqZoomTargetFocalContinuous = eqZoomAnchor50Mm.toFloat()
                                        }
                                        ColorCardCameraLensMode.Native -> {
                                            eqZoomTargetFocalMm = eqZoomRangeMinFocalMm
                                            eqZoomTargetFocalContinuous = eqZoomRangeMinFocalMm.toFloat()
                                        }
                                        else -> Unit
                                    }
                                    lensMode = it
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        if (!useFrontCamera) {
                            Row(
                                modifier = Modifier.align(Alignment.TopStart),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ColorCardCameraUtilityBlock(
                                    modifier = Modifier
                                        .size(width = topUtilityButtonWidth, height = topUtilityButtonHeight)
                                        .graphicsLayer { rotationZ = controlsRotationDegrees },
                                    enabled = cameraOperationalReady,
                                    selected = flashEnabled,
                                    onClick = {
                                        performCameraControlHaptic()
                                        flashEnabled = !flashEnabled
                                    }
                                ) {
                                    ColorCardCameraUtilityIcon(
                                        selected = flashEnabled,
                                        tint = colorCardCameraUtilityIconTint(
                                            enabled = cameraOperationalReady,
                                            selected = flashEnabled
                                        ),
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FlashOn,
                                            contentDescription = commonUiCopy.flash,
                                            tint = colorCardCameraUtilityIconTint(
                                                enabled = cameraOperationalReady,
                                                selected = flashEnabled
                                            ),
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }
                                }
                                if (lensMode in setOf(ColorCardCameraLensMode.Eq35, ColorCardCameraLensMode.Eq50)) {
                                    ColorCardCameraUtilityBlock(
                                        modifier = Modifier.size(
                                            width = topUtilityButtonWidth,
                                            height = topUtilityButtonHeight
                                        ).graphicsLayer { rotationZ = controlsRotationDegrees },
                                        enabled = cameraOperationalReady,
                                        selected = eqEnhancementEnabled,
                                        onClick = {
                                            performCameraControlHaptic()
                                            val nextValue = !eqEnhancementEnabled
                                            eqEnhancementEnabled = nextValue
                                            uiPrefs.edit().putBoolean(
                                                COLOR_CARD_CAMERA_PREF_EQ_ENHANCEMENT_ENABLED,
                                                nextValue
                                            ).apply()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = when (appLanguage) {
                                                AppLanguage.SimplifiedChinese -> "35/50增强"
                                                AppLanguage.TraditionalChinese -> "35/50增強"
                                                AppLanguage.Korean -> "35/50 강화"
                                                AppLanguage.Japanese -> "35/50 強化"
                                                AppLanguage.French -> "Amélioration 35/50"
                                                AppLanguage.German -> "35/50-Verstärkung"
                                                AppLanguage.Italian -> "Miglioramento 35/50"
                                                AppLanguage.Portuguese -> "Realce 35/50"
                                                AppLanguage.Spanish -> "Mejora 35/50"
                                                AppLanguage.Russian -> "Усиление 35/50"
                                                AppLanguage.Thai -> "เพิ่มประสิทธิภาพ 35/50"
                                                AppLanguage.Vietnamese -> "Tăng cường 35/50"
                                                AppLanguage.Indonesian -> "Peningkatan 35/50"
                                                AppLanguage.Arabic -> "تعزيز 35/50"
                                                AppLanguage.Ukrainian -> "Посилення 35/50"
                                                AppLanguage.Polish -> "Wzmocnienie 35/50"
                                                AppLanguage.Czech -> "Vylepšení 35/50"
                                                AppLanguage.Dutch -> "35/50-verbetering"
                                                AppLanguage.Swedish -> "35/50-förbättring"
                                                AppLanguage.Danish -> "35/50-forbedring"
                                                AppLanguage.Norwegian -> "35/50-forbedring"
                                                AppLanguage.Finnish -> "35/50-tehostus"
                                                AppLanguage.Greek -> "Ενίσχυση 35/50"
                                                AppLanguage.Romanian -> "Îmbunătățire 35/50"
                                                AppLanguage.Hungarian -> "35/50 javítás"
                                                AppLanguage.Turkish -> "35/50 iyileştirme"
                                                AppLanguage.Malay -> "Peningkatan 35/50"
                                                AppLanguage.Hebrew -> "שיפור 35/50"
                                                AppLanguage.English -> "35/50 enhancement"
                                            },
                                            tint = colorCardCameraUtilityIconTint(
                                                enabled = cameraOperationalReady,
                                                selected = eqEnhancementEnabled
                                            ),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.align(Alignment.TopEnd),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            ColorCardCameraUtilityBlock(
                                modifier = Modifier.size(
                                    width = topUtilityButtonWidth,
                                    height = topUtilityButtonHeight
                                ).graphicsLayer { rotationZ = controlsRotationDegrees },
                                enabled = cameraOperationalReady,
                                selected = previewBeautyEnabled,
                                onClick = {
                                    performCameraControlHaptic()
                                    previewBeautyEnabled = !previewBeautyEnabled
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Face,
                                    contentDescription = commonUiCopy.portrait,
                                    tint = when {
                                        previewBeautyActive -> Color.White
                                        else -> colorCardCameraUtilityIconTint(
                                            enabled = cameraOperationalReady,
                                            selected = previewBeautyEnabled
                                        )
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            ColorCardCameraUtilityBlock(
                                modifier = Modifier.size(
                                    width = topUtilityButtonWidth,
                                    height = topUtilityButtonHeight
                                ).graphicsLayer { rotationZ = controlsRotationDegrees },
                                enabled = cameraOperationalReady,
                                selected = previewGridEnabled,
                                onClick = {
                                    performCameraControlHaptic()
                                    previewGridEnabled = !previewGridEnabled
                                }
                            ) {
                                ColorCardCameraGridIcon(
                                    enabled = previewGridEnabled,
                                    tint = colorCardCameraUtilityIconTint(
                                        enabled = cameraOperationalReady,
                                        selected = previewGridEnabled
                                    ),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            }
                        }
                        ColorCardCameraPreviewEffectDots(
                            selected = previewEffect,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 2.dp)
                        )
                    }
                } else if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .width(previewContentWidth)
                            .height(previewContentHeight)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            strokeWidth = 2.6.dp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .width(previewContentWidth)
                            .height(previewContentHeight)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = strings.camera,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cameraPermissionSubtitle,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        TextButton(
                            onClick = {
                                performCameraControlHaptic()
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                        ) {
                            Text(
                                text = strings.onboardingPermissionButton,
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(previewToPanelGap))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(filterPanelFixedHeight),
                contentAlignment = Alignment.BottomCenter
            ) {
                ColorCardCameraFilterPanel(
                    state = filterState,
                    onExpandBrowser = {
                        if (filterState.browserCandidates.isNotEmpty()) {
                            expandedFilterPicker = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(panelToShutterGap))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(shutterRowContainerHeight)
                    .padding(top = shutterRowTopPadding, bottom = bottomPadding),
                contentAlignment = Alignment.Center
            ) {
                val latestGalleryItem = previewItems.firstOrNull()
                val shutterEnabled = cameraOperationalReady
                val shutterInteractionSource = remember { MutableInteractionSource() }
                ColorCardCameraLatestCaptureButton(
                    latestItem = latestGalleryItem,
                    queuePendingCount = queuePendingCount,
                    buttonSize = utilityButtonSize,
                    onOpenLatest = {
                        performCameraControlHaptic()
                        previewAnchorUri = latestGalleryItem?.uri?.toString()
                    },
                    onBoundsMeasured = { coordinates ->
                        latestCaptureButtonBounds = layoutBoundsInRootUnclipped(coordinates)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(y = resolvedShutterControlsVerticalOffset)
                        .graphicsLayer { rotationZ = controlsRotationDegrees }
                        .padding(start = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = resolvedShutterControlsVerticalOffset)
                        .graphicsLayer { rotationZ = controlsRotationDegrees }
                        .requiredSize(resolvedShutterButtonSize)
                        .clip(CircleShape)
                        .background(
                            color = if (shutterEnabled) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.42f)
                            },
                            shape = CircleShape
                        )
                        .clickable(
                            enabled = shutterEnabled,
                            interactionSource = shutterInteractionSource,
                            indication = ripple(bounded = true)
                        ) {
                            if (hapticsAllowed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            pendingCapturePortraitAdjustments = capturePortraitAdjustments
                            captureRequestNonce += 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (capturePending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = commonUiCopy.cameraShutter,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                ColorCardCameraUtilityBlock(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = resolvedShutterControlsVerticalOffset)
                        .graphicsLayer { rotationZ = controlsRotationDegrees }
                        .padding(end = 12.dp)
                        .size(utilityButtonSize),
                    enabled = cameraOperationalReady,
                    onClick = {
                        performCameraControlHaptic()
                        useFrontCamera = !useFrontCamera
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cameraswitch,
                        contentDescription = commonUiCopy.switchCamera,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (expandedFilterPicker) {
            ColorCardTraceExpandedFilterBrowser(
                traceState = filterState.buildPanelState().copy(candidates = filterState.browserCandidates),
                panelMode = ColorCardTracePanelMode.Filter,
                recentFilterIds = recentFilterIds,
                favoriteFilterIds = favoriteFilterIds,
                previewBitmaps = browserPreviewBitmaps,
                isDark = true,
                backgroundColor = Color(0xFF121314).copy(alpha = 0.96f),
                borderColor = Color.White.copy(alpha = 0.10f),
                primaryText = Color.White.copy(alpha = 0.96f),
                secondaryText = Color.White.copy(alpha = 0.56f),
                footerAlpha = 1f,
                onDismiss = { expandedFilterPicker = false },
                onCandidateSelected = { paletteId ->
                    recentFilterUsageStore.recordExpandedPickerSelection(paletteId)
                    filterState.selectCandidate(paletteId)
                    expandedFilterPicker = false
                },
                onCandidateFavoriteToggle = { paletteId ->
                    favoriteFilterStore.toggle(paletteId)
                }
            )
        }

        previewAnchorUri?.let {
            if (previewItems.isNotEmpty()) {
                KeepGalleryPreviewOverlay(
                    items = previewItems,
                    startIndex = previewStartIndex,
                    sourceBoundsInRoot = latestCaptureButtonBounds,
                    sourceBoundsByItemId = previewItems.associate { item ->
                        item.id to (latestCaptureButtonBounds ?: androidx.compose.ui.geometry.Rect.Zero)
                    },
                    onActiveItemChanged = { activeItem ->
                        previewAnchorUri = activeItem.uri.toString()
                    },
                    onSharedDismissStart = { sourceItemId ->
                        hiddenPreviewSourceUri = previewItems
                            .firstOrNull { item -> item.id == sourceItemId }
                            ?.uri
                            ?.toString()
                    },
                    onShare = { item ->
                        shareMedia(context, item)
                    },
                    onTertiaryAction = { item ->
                        if (openColorCardCameraMediaInAlbum(context, item)) {
                            Toast.makeText(context, commonUiCopy.openedInAlbum, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, commonUiCopy.cannotOpenAlbum, Toast.LENGTH_SHORT).show()
                        }
                    },
                    tertiaryActionIcon = Icons.Outlined.PhotoLibrary,
                    tertiaryActionContentDescription = commonUiCopy.openInAlbum,
                    tertiaryActionLabel = commonUiCopy.openInAlbum,
                    tertiaryActionTint = Color(0xFF93C5FD),
                    onDelete = { item ->
                        val removedUri = item.uri.toString()
                        val removedIndex = previewItems.indexOfFirst { previewItem ->
                            previewItem.uri.toString() == removedUri
                        }
                        val nextAnchorUri = nextColorCardCameraPreviewAnchorUri(previewItems, removedUri)
                        if (deleteColorCardCameraMedia(context, item)) {
                            capturedItems.removeAll { captured ->
                                captured.previewUri == removedUri || captured.finalUri == removedUri
                            }
                            cameraGalleryRefreshToken += 1
                            if (removedIndex <= 0) {
                                previewAnchorUri = null
                                hiddenPreviewSourceUri = null
                            } else {
                                previewAnchorUri = nextAnchorUri
                                if (nextAnchorUri == null) {
                                    hiddenPreviewSourceUri = null
                                }
                            }
                            Toast.makeText(context, commonUiCopy.deleted, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, commonUiCopy.deleteFailed, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismissRequest = {
                        if (hiddenPreviewSourceUri != null) {
                            previewAnchorUri = null
                            if (previewAnchorUri == null) {
                                hiddenPreviewSourceUri = null
                            }
                        } else {
                            previewAnchorUri = null
                            hiddenPreviewSourceUri = null
                        }
                    }
                )
            }
        }
    }

    if (showExperimentalNoticeDialog) {
        AlertDialog(
            onDismissRequest = {
                uiPrefs.edit().putBoolean(COLOR_CARD_CAMERA_PREF_EXPERIMENTAL_NOTICE_SHOWN, true).apply()
                showExperimentalNoticeDialog = false
            },
            title = {
                Text(
                    text = colorCardCameraExperimentalNoticeTitle(appLanguage)
                )
            },
            text = {
                Text(
                    text = colorCardCameraExperimentalNoticeBody(appLanguage)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        uiPrefs.edit().putBoolean(COLOR_CARD_CAMERA_PREF_EXPERIMENTAL_NOTICE_SHOWN, true).apply()
                        showExperimentalNoticeDialog = false
                    }
                ) {
                    Text(text = strings.confirm)
                }
            }
        )
    }
}

@Composable
private fun ColorCardCameraPreviewEffectOverlay(
    effect: ColorCardCameraPreviewEffect,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val effectLabel = remember(effect, appLanguage) { effect.localizedLabel(appLanguage) }
    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = effectLabel,
                color = Color.White.copy(alpha = 0.96f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, bottom = 82.dp)
        ) {
            val guideColor = Color.White.copy(alpha = 0.14f)
            val lineCount = 4
            repeat(lineCount) { index ->
                val x = size.width * ((index + 1f) / (lineCount + 1f))
                drawLine(
                    color = guideColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = effectLabel,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorCardCameraPreviewEffect.values().forEach { candidate ->
                    Box(
                        modifier = Modifier
                            .size(if (candidate == effect) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (candidate == effect) {
                                    Color.White.copy(alpha = 0.96f)
                                } else {
                                    Color.White.copy(alpha = 0.34f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ColorCardCameraPreviewRail(
    contentWidth: androidx.compose.ui.unit.Dp,
    previewHeight: androidx.compose.ui.unit.Dp,
    previewParams: ColorCardTraceGpuParams,
    effectiveCropCoverage: Float,
    framingGuideLabel: String?,
    previewEffect: ColorCardCameraPreviewEffect,
    useCaseProfile: ColorCardCameraUseCaseProfile,
    previewHostView: ColorCardCameraRealtimePreviewView?,
    previewRestartNonce: Int,
    cameraSessionBinding: Boolean,
    capturePending: Boolean,
    previewEffectAnimating: Boolean,
    pinchZoomEnabled: Boolean,
    onPreviewEffectAnimatingChange: (Boolean) -> Unit,
    onPreviewEffectChange: (ColorCardCameraPreviewEffect) -> Unit,
    onPinchZoom: (Float) -> Unit,
    onPreviewAspectRatioChange: (Float) -> Unit,
    onPreviewHostReset: () -> Unit,
    focusIndicator: ColorCardCameraFocusIndicator?,
    exposureBiasCapabilities: ColorCardCameraExposureBiasCapabilities,
    onFocusRequest: (ColorCardCameraFocusRequest, ColorCardCameraFocusIndicator, Float) -> Unit,
    onExposureBiasChange: (Float, ColorCardCameraFocusIndicator) -> Unit,
    onViewReady: (ColorCardCameraRealtimePreviewView) -> Unit,
    useFrontCamera: Boolean,
    showGridLines: Boolean,
    trackedFacesFrame: ColorCardCameraTrackedFacesFrame,
    beautyEnabled: Boolean,
    beautyActive: Boolean,
    debugInfo: ColorCardCameraDebugInfo?,
    previewPipelineDebugInfo: ColorCardCameraPreviewPipelineDebugInfo,
    onPreviewDebugInfoChange: (ColorCardCameraPreviewPipelineDebugInfo) -> Unit
) {
    val localDensity = androidx.compose.ui.platform.LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val latestFocusIndicator by rememberUpdatedState(focusIndicator)
    val latestOnPinchZoom by rememberUpdatedState(onPinchZoom)
    val previewCardShape = RoundedCornerShape(28.dp)
    val centerPreviewCardModifier = Modifier
        .clip(previewCardShape)

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .width(contentWidth)
            .height(previewHeight)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .width(contentWidth)
                .height(previewHeight)
                .then(centerPreviewCardModifier)
                .align(Alignment.Center)
                .pointerInput(
                    previewEffect,
                    exposureBiasCapabilities,
                    cameraSessionBinding,
                    capturePending,
                    previewEffectAnimating,
                    pinchZoomEnabled
                ) {
                    val swipeThresholdPx = with(localDensity) {
                        COLOR_CARD_CAMERA_PREVIEW_EFFECT_SWIPE_THRESHOLD_DP.dp.toPx()
                    }
                    val touchSlop = viewConfiguration.touchSlop
                    val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (cameraSessionBinding || capturePending || previewEffectAnimating) {
                            return@awaitEachGesture
                        }

                        val focusIndicatorAtDown = latestFocusIndicator
                        val exposureBiasAtDown = exposureBiasCapabilities.coerce(
                            focusIndicatorAtDown?.exposureBiasEv ?: 0f
                        )
                        var gestureMode: ColorCardCameraPreviewGestureMode? = null
                        var totalDragX = 0f
                        var totalDragY = 0f
                        var longPressTriggered = false
                        var gestureFinished = false

                        while (!gestureFinished) {
                            var event = if (!longPressTriggered &&
                                gestureMode == null &&
                                abs(totalDragX) < touchSlop &&
                                abs(totalDragY) < touchSlop
                            ) {
                                val remainingLongPressMs = (
                                    longPressTimeoutMillis -
                                        (SystemClock.uptimeMillis() - down.uptimeMillis)
                                    ).coerceAtLeast(1L)
                                withTimeoutOrNull(remainingLongPressMs) {
                                    awaitPointerEvent()
                                }
                            } else {
                                awaitPointerEvent()
                            }

                            if (event == null) {
                                if (!longPressTriggered) {
                                    longPressTriggered = true
                                    onFocusRequest(
                                        ColorCardCameraFocusRequest(
                                            normalizedX = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f),
                                            normalizedY = (down.position.y / size.height.toFloat()).coerceIn(0f, 1f),
                                            lockFocus = true,
                                            nonce = SystemClock.uptimeMillis()
                                        ),
                                        ColorCardCameraFocusIndicator(
                                            offset = down.position,
                                            locked = true,
                                            exposureBiasEv = 0f,
                                            nonce = SystemClock.uptimeMillis()
                                        ),
                                        0f
                                    )
                                }
                                continue
                            }
                            if (pinchZoomEnabled && event.changes.size >= 2) {
                                val activeChanges = event.changes.filter { it.pressed }
                                if (activeChanges.size >= 2) {
                                    val first = activeChanges[0]
                                    val second = activeChanges[1]
                                    val previousDistance = hypot(
                                        (first.previousPosition.x - second.previousPosition.x).toDouble(),
                                        (first.previousPosition.y - second.previousPosition.y).toDouble()
                                    ).toFloat()
                                    val currentDistance = hypot(
                                        (first.position.x - second.position.x).toDouble(),
                                        (first.position.y - second.position.y).toDouble()
                                    ).toFloat()
                                    if (previousDistance > 1f && currentDistance > 1f) {
                                        val scale = currentDistance / previousDistance
                                        if (scale.isFinite() && abs(scale - 1f) > 0.0005f) {
                                            latestOnPinchZoom(scale)
                                        }
                                    }
                                    activeChanges.forEach { it.consumeAllChanges() }
                                    gestureMode = ColorCardCameraPreviewGestureMode.Cancelled
                                    continue
                                }
                            }
                            if (event.changes.size != 1) {
                                gestureFinished = true
                                continue
                            }
                            val change = event.changes.first()
                            if (change.id != down.id) {
                                gestureFinished = true
                                continue
                            }

                            if (change.changedToUpIgnoreConsumed()) {
                                when {
                                    gestureMode == ColorCardCameraPreviewGestureMode.HorizontalSwipe -> {
                                        val transitionDirection = when {
                                            totalDragX <= -swipeThresholdPx -> -1f
                                            totalDragX >= swipeThresholdPx -> 1f
                                            else -> 0f
                                        }
                                        if (transitionDirection != 0f) {
                                            onPreviewEffectAnimatingChange(true)
                                            onPreviewEffectChange(
                                                if (transitionDirection < 0f) {
                                                    previewEffect.next()
                                                } else {
                                                    previewEffect.previous()
                                                }
                                            )
                                            onPreviewEffectAnimatingChange(false)
                                        }
                                    }
                                    gestureMode == ColorCardCameraPreviewGestureMode.ExposureDrag -> Unit
                                    !longPressTriggered -> {
                                        onFocusRequest(
                                            ColorCardCameraFocusRequest(
                                                normalizedX = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f),
                                                normalizedY = (down.position.y / size.height.toFloat()).coerceIn(0f, 1f),
                                                lockFocus = false,
                                                nonce = SystemClock.uptimeMillis()
                                            ),
                                            ColorCardCameraFocusIndicator(
                                                offset = down.position,
                                                locked = false,
                                                exposureBiasEv = 0f,
                                                nonce = SystemClock.uptimeMillis()
                                            ),
                                            0f
                                        )
                                    }
                                }
                                gestureFinished = true
                                continue
                            }

                            val delta = change.positionChange()
                            totalDragX += delta.x
                            totalDragY += delta.y

                            if (gestureMode == null) {
                                when {
                                    focusIndicatorAtDown != null &&
                                        abs(totalDragY) > touchSlop &&
                                        abs(totalDragY) > abs(totalDragX) * 1.15f -> {
                                        gestureMode = ColorCardCameraPreviewGestureMode.ExposureDrag
                                    }
                                    abs(totalDragX) > touchSlop && abs(totalDragX) > abs(totalDragY) * 1.2f -> {
                                        gestureMode = ColorCardCameraPreviewGestureMode.HorizontalSwipe
                                    }
                                    focusIndicatorAtDown == null &&
                                        abs(totalDragY) > touchSlop * 1.45f &&
                                        abs(totalDragY) > abs(totalDragX) * 1.2f -> {
                                        gestureMode = ColorCardCameraPreviewGestureMode.Cancelled
                                    }
                                }
                            }
                            when (gestureMode) {
                                ColorCardCameraPreviewGestureMode.ExposureDrag -> {
                                    val activeIndicator = focusIndicatorAtDown ?: continue
                                    change.consumeAllChanges()
                                    val previewHeightPx = size.height.toFloat().coerceAtLeast(1f)
                                    val rawExposureBias = resolveColorCardCameraFocusExposureBiasFromDrag(
                                        startExposureBiasEv = exposureBiasAtDown,
                                        totalDragYPx = totalDragY,
                                        previewHeightPx = previewHeightPx,
                                        capabilities = exposureBiasCapabilities
                                    )
                                    onExposureBiasChange(
                                        exposureBiasCapabilities.snap(rawExposureBias),
                                        activeIndicator.copy(
                                            exposureBiasEv = rawExposureBias,
                                            nonce = SystemClock.uptimeMillis()
                                        )
                                    )
                                }
                                ColorCardCameraPreviewGestureMode.HorizontalSwipe -> {
                                    change.consumeAllChanges()
                                }
                                else -> Unit
                            }
                            if (longPressTriggered) {
                                change.consumeAllChanges()
                            }
                        }
                    }
                }
        ) {
            ColorCardCameraRealtimePreview(
                params = previewParams,
                cropCoverage = 1f,
                maxRenderFps = useCaseProfile.livePreviewRenderFps,
                onPreviewStalled = onPreviewHostReset,
                onFirstFrameRendered = { },
                onSourceAspectRatioChanged = onPreviewAspectRatioChange,
                onPreviewDebugInfoChanged = onPreviewDebugInfoChange,
                onViewReady = onViewReady,
                modifier = Modifier.matchParentSize()
            )
            ColorCardCameraCropFrameOverlay(
                cropCoverage = effectiveCropCoverage,
                label = framingGuideLabel,
                modifier = Modifier.matchParentSize()
            )
            if (showGridLines) {
                ColorCardCameraPreviewGridOverlay(
                    cropCoverage = effectiveCropCoverage,
                    modifier = Modifier.matchParentSize()
                )
            }
            ColorCardCameraFaceTrackingOverlay(
                trackedFacesFrame = trackedFacesFrame,
                beautyEnabled = beautyEnabled,
                beautyActive = beautyActive,
                modifier = Modifier.matchParentSize()
            )
            if (previewEffect != ColorCardCameraPreviewEffect.None) {
                ColorCardCameraPreviewEffectTag(
                    effect = previewEffect,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (!framingGuideLabel.isNullOrBlank()) 40.dp else 68.dp)
                )
            }
            ColorCardCameraFocusIndicatorOverlay(
                indicator = focusIndicator,
                exposureBiasCapabilities = exposureBiasCapabilities,
                modifier = Modifier.matchParentSize()
            )
            if (COLOR_CARD_CAMERA_SHOW_DEBUG_OVERLAY) {
                ColorCardCameraDebugOverlay(
                    sessionDebugInfo = debugInfo,
                    previewDebugInfo = previewPipelineDebugInfo,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorCardCameraFaceTrackingOverlay(
    trackedFacesFrame: ColorCardCameraTrackedFacesFrame,
    beautyEnabled: Boolean,
    beautyActive: Boolean,
    modifier: Modifier = Modifier
) {
    val trackedFaces = trackedFacesFrame.faces
    if (trackedFaces.isEmpty()) return

    val overlayAlpha = when {
        trackedFacesFrame.isHeld -> 0.46f
        trackedFacesFrame.stableFrameCount <= 1 -> 0.72f
        else -> 0.94f
    }
    val strokeColor = when {
        beautyActive -> COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR.copy(alpha = overlayAlpha)
        beautyEnabled -> COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR.copy(alpha = overlayAlpha * 0.92f)
        else -> COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR.copy(alpha = overlayAlpha * 0.84f)
    }
    val shadowColor = Color.Black.copy(alpha = 0.24f * overlayAlpha)

    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val shadowWidth = strokeWidth + 1.5.dp.toPx()
        val minCorner = 18.dp.toPx()
        val maxCorner = 34.dp.toPx()

        trackedFaces.forEach { face ->
            val bounds = face.bounds
            val faceWidth = bounds.width.coerceAtLeast(0.0001f)
            val faceHeight = bounds.height.coerceAtLeast(0.0001f)
            val horizontalPadding = (faceWidth * 0.08f).coerceIn(0.01f, 0.04f)
            val verticalPadding = (faceHeight * 0.10f).coerceIn(0.01f, 0.05f)
            val left = ((bounds.left - horizontalPadding).coerceIn(0f, 1f) * size.width)
            val top = ((bounds.top - verticalPadding).coerceIn(0f, 1f) * size.height)
            val right = ((bounds.right + horizontalPadding).coerceIn(0f, 1f) * size.width)
            val bottom = ((bounds.bottom + verticalPadding).coerceIn(0f, 1f) * size.height)
            val width = (right - left).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)
            val corner = min(max(min(width, height) * 0.22f, minCorner), maxCorner)

            drawColorCardCameraCornerFrame(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                corner = corner,
                strokeWidth = shadowWidth,
                color = shadowColor
            )
            drawColorCardCameraCornerFrame(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                corner = corner,
                strokeWidth = strokeWidth,
                color = strokeColor
            )
        }
    }

}

@Composable
private fun ColorCardCameraDebugOverlay(
    sessionDebugInfo: ColorCardCameraDebugInfo?,
    previewDebugInfo: ColorCardCameraPreviewPipelineDebugInfo,
    modifier: Modifier = Modifier
) {
    val lines = remember(sessionDebugInfo, previewDebugInfo) {
        buildList {
            add("camera-debug")
            addAll(sessionDebugInfo?.summaryLines().orEmpty())
            addAll(previewDebugInfo.summaryLines())
        }
    }
    Column(
        modifier = modifier
            .widthIn(max = 340.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                color = if (index == 0) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.82f),
                fontSize = if (index == 0) 10.sp else 9.sp,
                fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 12.sp
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawColorCardCameraCornerFrame(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    corner: Float,
    strokeWidth: Float,
    color: Color
) {
    fun line(start: Offset, end: Offset) {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }

    line(Offset(left, top), Offset(left + corner, top))
    line(Offset(left, top), Offset(left, top + corner))

    line(Offset(right, top), Offset(right - corner, top))
    line(Offset(right, top), Offset(right, top + corner))

    line(Offset(left, bottom), Offset(left + corner, bottom))
    line(Offset(left, bottom), Offset(left, bottom - corner))

    line(Offset(right, bottom), Offset(right - corner, bottom))
    line(Offset(right, bottom), Offset(right, bottom - corner))
}

@Composable
private fun ColorCardCameraPreviewEffectDots(
    selected: ColorCardCameraPreviewEffect,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorCardCameraPreviewEffect.values().forEach { candidate ->
            Box(
                modifier = Modifier
                    .size(if (candidate == selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (candidate == selected) {
                            Color.White.copy(alpha = 0.96f)
                        } else {
                            Color.White.copy(alpha = 0.32f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun ColorCardCameraGridIcon(
    enabled: Boolean,
    tint: Color = if (enabled) {
        Color.White.copy(alpha = 0.96f)
    } else {
        Color.White.copy(alpha = 0.72f)
    },
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 1.15.dp.toPx()
        val lineColor = tint
        drawLine(
            color = lineColor,
            start = Offset(size.width / 3f, 0f),
            end = Offset(size.width / 3f, size.height),
            strokeWidth = stroke
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width * (2f / 3f), 0f),
            end = Offset(size.width * (2f / 3f), size.height),
            strokeWidth = stroke
        )
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height / 3f),
            end = Offset(size.width, size.height / 3f),
            strokeWidth = stroke
        )
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height * (2f / 3f)),
            end = Offset(size.width, size.height * (2f / 3f)),
            strokeWidth = stroke
        )
    }
}

@Composable
private fun ColorCardCameraPreviewGridOverlay(
    cropCoverage: Float = 1f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(14.dp)) {
        val stroke = 1.dp.toPx()
        val lineColor = Color.White.copy(alpha = 0.22f)
        val safeCropCoverage = cropCoverage.coerceIn(0.2f, 1f)
        val insetX = (size.width * (1f - safeCropCoverage)) / 2f
        val insetY = (size.height * (1f - safeCropCoverage)) / 2f
        val left = insetX.coerceIn(0f, size.width)
        val top = insetY.coerceIn(0f, size.height)
        val right = (size.width - insetX).coerceIn(left, size.width)
        val bottom = (size.height - insetY).coerceIn(top, size.height)
        val width = (right - left).coerceAtLeast(1f)
        val height = (bottom - top).coerceAtLeast(1f)
        val firstThirdX = left + width / 3f
        val secondThirdX = left + width * (2f / 3f)
        val firstThirdY = top + height / 3f
        val secondThirdY = top + height * (2f / 3f)
        drawLine(lineColor, Offset(firstThirdX, top), Offset(firstThirdX, bottom), stroke)
        drawLine(lineColor, Offset(secondThirdX, top), Offset(secondThirdX, bottom), stroke)
        drawLine(lineColor, Offset(left, firstThirdY), Offset(right, firstThirdY), stroke)
        drawLine(lineColor, Offset(left, secondThirdY), Offset(right, secondThirdY), stroke)
    }
}

@Composable
private fun ColorCardCameraCropFrameOverlay(
    cropCoverage: Float,
    label: String?,
    modifier: Modifier = Modifier
) {
    val safeCropCoverage = cropCoverage.coerceIn(0.2f, 1f)
    if (safeCropCoverage >= 0.999f) return

    BoxWithConstraints(modifier = modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val labelTopOffsetPx = remember(maxWidth, maxHeight, safeCropCoverage, density) {
            with(density) {
                val safeFrameInsetPx = 14.dp.toPx()
                val widthPx = maxWidth.toPx()
                val heightPx = maxHeight.toPx()
                val availableHeight = (heightPx - safeFrameInsetPx * 2f).coerceAtLeast(1f)
                val insetY = safeFrameInsetPx + (availableHeight * (1f - safeCropCoverage)) / 2f
                (insetY - 24.dp.toPx()).coerceAtLeast(12.dp.toPx())
            }
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            val safeFrameInset = 14.dp.toPx()
            val availableWidth = (size.width - safeFrameInset * 2f).coerceAtLeast(1f)
            val availableHeight = (size.height - safeFrameInset * 2f).coerceAtLeast(1f)
            val insetX = safeFrameInset + (availableWidth * (1f - safeCropCoverage)) / 2f
            val insetY = safeFrameInset + (availableHeight * (1f - safeCropCoverage)) / 2f
            val frameLeft = insetX.coerceIn(0f, size.width)
            val frameTop = insetY.coerceIn(0f, size.height)
            val frameRight = (size.width - insetX).coerceIn(frameLeft, size.width)
            val frameBottom = (size.height - insetY).coerceIn(frameTop, size.height)
            val dimColor = Color.Black.copy(alpha = 0.34f)
            val strokeColor = Color.White.copy(alpha = 0.82f)
            val strokeWidth = 1.4.dp.toPx()
            val cornerRadius = 28.dp.toPx()
            val dimPath = androidx.compose.ui.graphics.Path().apply {
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                addRect(
                    androidx.compose.ui.geometry.Rect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height
                    )
                )
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = frameLeft,
                            top = frameTop,
                            right = frameRight,
                            bottom = frameBottom
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            cornerRadius,
                            cornerRadius
                        )
                    )
                )
            }
            drawPath(
                path = dimPath,
                color = dimColor
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(frameLeft, frameTop),
                size = androidx.compose.ui.geometry.Size(frameRight - frameLeft, frameBottom - frameTop),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            x = 0,
                            y = labelTopOffsetPx.roundToInt()
                        )
                    }
            )
        }
    }
}

@Composable
private fun ColorCardCameraPreviewEffectTag(
    effect: ColorCardCameraPreviewEffect,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val effectLabel = remember(effect, appLanguage) { effect.localizedLabel(appLanguage) }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.46f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = effectLabel,
            color = Color.White.copy(alpha = 0.96f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ColorCardCameraLatestCaptureButton(
    latestItem: MediaItem?,
    queuePendingCount: Int,
    buttonSize: androidx.compose.ui.unit.Dp = 58.dp,
    onOpenLatest: () -> Unit,
    onBoundsMeasured: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val commonUiCopy = remember(appLanguage) { getCommonGlobalUiCopy(appLanguage) }
    val latestUri = latestItem?.uri
    Box(
        modifier = modifier
            .size(buttonSize)
            .onGloballyPositioned(onBoundsMeasured)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .clickable(enabled = latestUri != null && queuePendingCount <= 0) {
                onOpenLatest()
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            queuePendingCount > 0 -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.4.dp
                )
            }
            latestUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(latestUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = commonUiCopy.latestCapture,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = commonUiCopy.captured,
                    tint = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun ColorCardCameraUtilityBlock(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
        if (enabled && !selected) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val stroke = 2.1.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.92f),
                    start = Offset(size.width * 0.24f, size.height * 0.78f),
                    end = Offset(size.width * 0.76f, size.height * 0.22f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ColorCardCameraUtilityIcon(
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun colorCardCameraUiPrefs(context: Context): SharedPreferences {
    return context.applicationContext.getSharedPreferences(
        COLOR_CARD_CAMERA_UI_PREFS_NAME,
        Context.MODE_PRIVATE
    )
}

private fun colorCardCameraExperimentalNoticeTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "photoo cam 测试提示"
    AppLanguage.TraditionalChinese -> "photoo cam 測試提示"
    else -> "Photoo Cam Beta Notice"
}

private fun colorCardCameraExperimentalNoticeBody(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> """
        photoo cam 目前属于测试功能，还不够完善。

        若您在使用过程中遇到发热、卡顿、掉帧、预览错误或失效、长焦丢失等问题，均属正常现象，恳请您第一时间反馈给我们。

        当然，您对于 photoo cam 的任何建议，也欢迎一并反馈给我们。
    """.trimIndent()
    AppLanguage.TraditionalChinese -> """
        photoo cam 目前屬於測試功能，還不夠完善。

        若您在使用過程中遇到發熱、卡頓、掉幀、預覽錯誤或失效、長焦丟失等問題，均屬正常現象，懇請您第一時間回饋給我們。

        當然，您對於 photoo cam 的任何建議，也歡迎一併回饋給我們。
    """.trimIndent()
    else -> """
        Photoo Cam is currently an experimental feature and is not fully polished yet.

        Heat, lag, frame drops, preview errors or failures, and telephoto issues may still occur during use. If you run into any of them, please send us feedback as soon as possible.

        Any suggestions for Photoo Cam are also very welcome.
    """.trimIndent()
}

private fun colorCardCameraShouldSuppressEqEnhancement(
    useFrontCamera: Boolean,
    lensMode: ColorCardCameraLensMode,
    eqEnhancementEnabled: Boolean
): Boolean {
    if (useFrontCamera) return false
    val isEqLens = lensMode == ColorCardCameraLensMode.Eq35 || lensMode == ColorCardCameraLensMode.Eq50
    return isEqLens && !eqEnhancementEnabled
}

private fun colorCardCameraUtilityIconTint(
    enabled: Boolean,
    selected: Boolean
): Color {
    return when {
        !enabled -> Color.White.copy(alpha = 0.24f)
        selected -> Color.White.copy(alpha = 0.98f)
        else -> Color.White.copy(alpha = 0.76f)
    }
}

@Composable
private fun rememberColorCardCameraOrientationState(context: Context): ColorCardCameraOrientationState {
    val appContext = remember(context) { context.applicationContext }
    var deviceRotation by remember { mutableIntStateOf(context.display.rotation) }
    DisposableEffect(appContext) {
        val listener = object : OrientationEventListener(appContext) {
            override fun onOrientationChanged(orientation: Int) {
                val nextRotation = orientationDegreesToSurfaceRotation(orientation) ?: return
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
    return remember(deviceRotation) {
        ColorCardCameraOrientationState(
            deviceRotation = deviceRotation,
            controlsRotationDegrees = surfaceRotationToControlsRotation(deviceRotation)
        )
    }
}

private fun orientationDegreesToSurfaceRotation(orientation: Int): Int? {
    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return null
    return when (orientation) {
        in 45..134 -> Surface.ROTATION_270
        in 135..224 -> Surface.ROTATION_180
        in 225..314 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }
}

private fun surfaceRotationToControlsRotation(rotation: Int): Float {
    return when (rotation) {
        Surface.ROTATION_90 -> 90f
        Surface.ROTATION_180 -> 180f
        Surface.ROTATION_270 -> -90f
        else -> 0f
    }
}

@Composable
private fun ColorCardCameraPreviewSession(
    provider: ProcessCameraProvider?,
    onProviderReady: (ProcessCameraProvider) -> Unit,
    previewHostView: ColorCardCameraRealtimePreviewView?,
    cameraSelector: CameraSelector,
    lensMode: ColorCardCameraLensMode,
    requestedCameraId: String?,
    requestedPhysicalCameraId: String?,
    requestedCapturePhysicalCameraId: String?,
    outputAspectRatio: Float,
    baseCropCoverage: Float,
    zoomRatio: Float,
    useFrontCamera: Boolean,
    targetRotation: Int,
    flashEnabled: Boolean,
    multiFrameFusionEnabled: Boolean,
    captureRequestNonce: Int,
    previewRecoveryNonce: Int,
    focusRequest: ColorCardCameraFocusRequest?,
    meteringExposureBiasEv: Float,
    useCaseProfile: ColorCardCameraUseCaseProfile,
    faceTrackingEnabled: Boolean,
    onBindingStateChange: (Boolean) -> Unit,
    onFaceTrackingFrameChange: (ColorCardCameraTrackedFacesFrame) -> Unit,
    onEffectiveCropCoverageChange: (Float) -> Unit,
    onExposureBiasCapabilitiesChange: (ColorCardCameraExposureBiasCapabilities) -> Unit,
    onDebugInfoChange: (ColorCardCameraDebugInfo?) -> Unit,
    onCaptureStateChange: (Boolean) -> Unit,
    onCaptured: (ColorCardCameraCaptureResult) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val commonUiCopy = remember(appLanguage) { getCommonGlobalUiCopy(appLanguage) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val disableRealtimeFaceTrackingForPhysicalTele = !useFrontCamera &&
        isColorCardCameraPhysicalSubLens(
            cameraId = requestedCameraId,
            physicalCameraId = requestedPhysicalCameraId
        )
    val attachFaceTrackingUseCase = faceTrackingEnabled && !disableRealtimeFaceTrackingForPhysicalTele
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceTrackingExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val onFaceTrackingFrameChangeState by rememberUpdatedState(onFaceTrackingFrameChange)
    val stabilizationConfig = remember(context, requestedCameraId, requestedPhysicalCameraId, useFrontCamera) {
        resolveColorCardCameraStabilizationConfig(
            context = context,
            cameraId = requestedCameraId,
            physicalCameraId = requestedPhysicalCameraId,
            frontCamera = useFrontCamera
        )
    }
    val captureStabilizationConfig = remember(
        context,
        requestedCameraId,
        requestedCapturePhysicalCameraId,
        useFrontCamera
    ) {
        resolveColorCardCameraStabilizationConfig(
            context = context,
            cameraId = requestedCameraId,
            physicalCameraId = requestedCapturePhysicalCameraId,
            frontCamera = useFrontCamera
        )
    }
    val faceTrackingPipeline = remember(context, mainExecutor) {
        ColorCardCameraFaceTrackingPipeline(context.applicationContext) { frame ->
            onFaceTrackingFrameChangeState(frame)
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var faceTrackingAnalysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var boundPhysicalCameraId by remember { mutableStateOf<String?>(null) }
    var activePhysicalCameraId by remember { mutableStateOf<String?>(null) }
    var boundPreviewResolution by remember { mutableStateOf<Size?>(null) }
    var boundCaptureResolution by remember { mutableStateOf<Size?>(null) }
    var currentEffectiveCropCoverage by remember { mutableStateOf(baseCropCoverage.coerceIn(0.2f, 1f)) }
    var stabilizationRuntime by remember(stabilizationConfig) {
        mutableStateOf(
            ColorCardCameraStabilizationRuntimeState(
                requestedVideoMode = stabilizationConfig.videoMode,
                requestedOpticalMode = stabilizationConfig.opticalMode,
                supportedVideoModes = stabilizationConfig.supportedVideoModes,
                supportedOpticalModes = stabilizationConfig.supportedOpticalModes
            )
        )
    }
    var usedFallbackSelector by remember { mutableStateOf(false) }
    val sessionCaptureCallback = remember(mainExecutor, requestedPhysicalCameraId, stabilizationConfig) {
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: android.hardware.camera2.CaptureRequest,
                result: TotalCaptureResult
            ) {
                val resolvedActivePhysicalId = resolveColorCardCameraActivePhysicalId(
                    result = result,
                    requestedPhysicalCameraId = requestedPhysicalCameraId
                )
                mainExecutor.execute {
                    activePhysicalCameraId = resolvedActivePhysicalId
                    stabilizationRuntime = stabilizationRuntime.copy(
                        activeVideoMode = result.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE),
                        activeOpticalMode = result.get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE)
                    )
                }
            }
        }
    }
    var boundFaceTracking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { onProviderReady(future.get()) },
            mainExecutor
        )
        onDispose {
            faceTrackingPipeline.close()
            faceTrackingExecutor.shutdown()
            captureExecutor.shutdown()
            provider?.unbindAll()
        }
    }

    LaunchedEffect(useFrontCamera) {
        faceTrackingPipeline.setMirrorHorizontally(useFrontCamera)
    }

    val usePreviewBitmapFaceTracking = attachFaceTrackingUseCase &&
        COLOR_CARD_CAMERA_ENABLE_PREVIEW_BITMAP_FACE_TRACKING_FALLBACK &&
        !boundFaceTracking &&
        !useFrontCamera &&
        !requestedPhysicalCameraId.isNullOrEmpty() &&
        previewHostView != null
    val effectiveFaceTrackingEnabled = attachFaceTrackingUseCase &&
        (boundFaceTracking || usePreviewBitmapFaceTracking)

    LaunchedEffect(attachFaceTrackingUseCase, boundFaceTracking, usePreviewBitmapFaceTracking) {
        val trackingSource = when {
            disableRealtimeFaceTrackingForPhysicalTele -> "disabled_physical_tele"
            boundFaceTracking -> "camera_analysis"
            usePreviewBitmapFaceTracking -> "preview_bitmap"
            else -> "none"
        }
        Log.d(
            COLOR_CARD_CAMERA_LOG_TAG,
            "Face tracking requested=$attachFaceTrackingUseCase bound=$boundFaceTracking " +
                "source=$trackingSource front=$useFrontCamera lens=$lensMode " +
                "physical=${requestedPhysicalCameraId ?: "-"}"
        )
        faceTrackingPipeline.setEnabled(effectiveFaceTrackingEnabled)
        if (!effectiveFaceTrackingEnabled) {
            onFaceTrackingFrameChangeState(ColorCardCameraTrackedFacesFrame.Empty)
        }
    }

    LaunchedEffect(usePreviewBitmapFaceTracking, previewHostView, requestedPhysicalCameraId, lensMode) {
        if (!usePreviewBitmapFaceTracking) return@LaunchedEffect
        val fallbackPreviewView = previewHostView
        val fallbackPhysicalCameraId = requestedPhysicalCameraId
        Log.d(
            COLOR_CARD_CAMERA_LOG_TAG,
            "Starting preview bitmap face tracking fallback lens=$lensMode physical=$fallbackPhysicalCameraId"
        )
        while (isActive) {
            val bitmap = withContext(Dispatchers.Main) {
                fallbackPreviewView.capturePreviewBitmap(
                    targetWidth = COLOR_CARD_CAMERA_PREVIEW_BITMAP_FACE_TRACKING_TARGET_SIZE.width,
                    targetHeight = COLOR_CARD_CAMERA_PREVIEW_BITMAP_FACE_TRACKING_TARGET_SIZE.height
                )
            }
            if (bitmap != null) {
                faceTrackingPipeline.submitBitmap(
                    bitmap = bitmap,
                    timestampMs = SystemClock.elapsedRealtime()
                )
            }
            delay(COLOR_CARD_CAMERA_PREVIEW_BITMAP_FACE_TRACKING_INTERVAL_MS)
        }
    }

    LaunchedEffect(
        requestedCameraId,
        requestedPhysicalCameraId,
        useFrontCamera,
        previewRecoveryNonce
    ) {
        faceTrackingPipeline.reset()
        onFaceTrackingFrameChangeState(ColorCardCameraTrackedFacesFrame.Empty)
    }

    DisposableEffect(
        provider,
        lifecycleOwner,
        previewHostView,
        requestedCameraId,
        requestedPhysicalCameraId,
        requestedCapturePhysicalCameraId,
        useFrontCamera,
        useCaseProfile,
        previewRecoveryNonce
    ) {
        val currentProvider = provider ?: return@DisposableEffect onDispose {}
        val hostView = previewHostView ?: return@DisposableEffect onDispose {}
        onBindingStateChange(true)
        usedFallbackSelector = false
        boundFaceTracking = false
        boundPhysicalCameraId = null
        activePhysicalCameraId = null
        val previewBuilder = Preview.Builder()
            .setResolutionSelector(buildColorCardCameraResolutionSelector(useCaseProfile.previewTargetSize))
            .setTargetFrameRate(useCaseProfile.previewFrameRate)
            .setTargetRotation(Surface.ROTATION_0)
        applyColorCardCameraInterop(
            builder = previewBuilder,
            physicalCameraId = requestedPhysicalCameraId,
            stabilizationConfig = stabilizationConfig,
            captureCallback = sessionCaptureCallback
        )
        val preview = previewBuilder.build()
        preview.surfaceProvider = hostView.surfaceProvider
        val faceTrackingAnalysis = if (attachFaceTrackingUseCase) {
            ImageAnalysis.Builder()
                .setResolutionSelector(buildColorCardCameraResolutionSelector(COLOR_CARD_CAMERA_FACE_TRACKING_TARGET_SIZE))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(targetRotation)
                .also { builder ->
                    applyColorCardCameraInterop(
                        builder = builder,
                        physicalCameraId = requestedPhysicalCameraId
                    )
                }
                .build()
                .apply {
                    setAnalyzer(faceTrackingExecutor, faceTrackingPipeline)
                }
        } else {
            null
        }
        faceTrackingAnalysisUseCase = faceTrackingAnalysis
        val capture = createColorCardCameraImageCapture(
            context = context,
            cameraId = requestedCameraId,
            targetRotation = targetRotation,
            useCaseProfile = useCaseProfile,
            physicalCameraId = requestedCapturePhysicalCameraId,
            experimentalYuvCapture = false,
            stabilizationConfig = captureStabilizationConfig,
            captureCallback = sessionCaptureCallback
        )
        imageCapture = capture
        val previewAndCaptureUseCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(capture)
            .build()
        val useCaseGroupWithFaceTracking = faceTrackingAnalysis?.let { analysis ->
            UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(capture)
                .addUseCase(analysis)
                .build()
        }

        fun bindUseCases(
            selector: CameraSelector,
            includeFaceTracking: Boolean,
            fallbackBinding: Boolean
        ): Camera {
            currentProvider.unbindAll()
            val useCaseGroup = if (includeFaceTracking) {
                useCaseGroupWithFaceTracking ?: previewAndCaptureUseCaseGroup
            } else {
                previewAndCaptureUseCaseGroup
            }
            return currentProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                useCaseGroup
            ).also {
                usedFallbackSelector = fallbackBinding
                boundPhysicalCameraId = selector.physicalCameraId
                boundFaceTracking = includeFaceTracking && faceTrackingAnalysis != null
                boundPreviewResolution = preview.resolutionInfo?.resolution
                boundCaptureResolution = capture.resolutionInfo?.resolution
                Log.d(
                    COLOR_CARD_CAMERA_LOG_TAG,
                    "UseCase bound preview=${formatColorCardCameraSize(preview.resolutionInfo?.resolution)} " +
                        "capture=${formatColorCardCameraSize(capture.resolutionInfo?.resolution)} " +
                        "selector=${selector.physicalCameraId ?: "-"} faceTracking=$boundFaceTracking " +
                        "fallback=$fallbackBinding viewport=none " +
                        "reqPreviewPhysical=${requestedPhysicalCameraId ?: "-"} " +
                        "reqCapturePhysical=${requestedCapturePhysicalCameraId ?: "-"}"
                )
            }
        }

        runCatching {
            boundCamera = bindUseCases(
                selector = cameraSelector,
                includeFaceTracking = faceTrackingAnalysis != null,
                fallbackBinding = false
            )
            onBindingStateChange(false)
        }.recoverCatching { bindError ->
            Log.w(
                COLOR_CARD_CAMERA_LOG_TAG,
                "Primary bind failed physical=${requestedPhysicalCameraId ?: "-"} " +
                    "faceTracking=${faceTrackingAnalysis != null} lens=$lensMode",
                bindError
            )
            if (!requestedPhysicalCameraId.isNullOrEmpty() && faceTrackingAnalysis != null) {
                boundCamera = bindUseCases(
                    selector = cameraSelector,
                    includeFaceTracking = false,
                    fallbackBinding = false
                )
                Log.w(
                    COLOR_CARD_CAMERA_LOG_TAG,
                    "Recovered physical lens binding by dropping face tracking for lens=$lensMode " +
                        "physical=${requestedPhysicalCameraId}"
                )
                onBindingStateChange(false)
            } else {
                throw bindError
            }
        }.onFailure {
            val fallbackSelector = when {
                useFrontCamera -> CameraSelector.DEFAULT_FRONT_CAMERA
                !requestedCameraId.isNullOrEmpty() -> buildColorCardCameraSelectorById(
                    bindCameraId = requestedCameraId,
                    physicalCameraId = null
                )
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }
            runCatching {
                boundCamera = bindUseCases(
                    selector = fallbackSelector,
                    includeFaceTracking = faceTrackingAnalysis != null,
                    fallbackBinding = true
                )
                onBindingStateChange(false)
            }.recoverCatching { fallbackError ->
                Log.w(
                    COLOR_CARD_CAMERA_LOG_TAG,
                    "Fallback bind failed faceTracking=${faceTrackingAnalysis != null} " +
                        "requestedCamera=${requestedCameraId ?: "-"}",
                    fallbackError
                )
                if (faceTrackingAnalysis != null) {
                    boundCamera = bindUseCases(
                        selector = fallbackSelector,
                        includeFaceTracking = false,
                        fallbackBinding = true
                    )
                    Log.w(
                        COLOR_CARD_CAMERA_LOG_TAG,
                        "Recovered fallback binding by dropping face tracking for selector=${fallbackSelector.physicalCameraId ?: "-"}"
                    )
                    onBindingStateChange(false)
                } else {
                    throw fallbackError
                }
            }.onFailure {
                onBindingStateChange(false)
                onDebugInfoChange(
                    ColorCardCameraDebugInfo(
                        lensMode = lensMode,
                        requestedCameraId = requestedCameraId,
                        requestedPhysicalCameraId = requestedPhysicalCameraId,
                        requestedCapturePhysicalCameraId = requestedCapturePhysicalCameraId,
                        boundCameraId = null,
                        boundPhysicalCameraId = null,
                        activePhysicalCameraId = null,
                        requestedZoomRatio = zoomRatio.coerceAtLeast(1f),
                        appliedZoomRatio = 1f,
                        minZoomRatio = 1f,
                        maxZoomRatio = 1f,
                        effectiveCropCoverage = baseCropCoverage.coerceIn(0.2f, 1f),
                        fallbackBinding = true,
                        frontCamera = useFrontCamera,
                        bindFailed = true,
                        requestedVideoStabilizationMode = stabilizationRuntime.requestedVideoMode,
                        requestedOpticalStabilizationMode = stabilizationRuntime.requestedOpticalMode,
                        activeVideoStabilizationMode = stabilizationRuntime.activeVideoMode,
                        activeOpticalStabilizationMode = stabilizationRuntime.activeOpticalMode,
                        supportedVideoStabilizationModes = stabilizationRuntime.supportedVideoModes,
                        supportedOpticalStabilizationModes = stabilizationRuntime.supportedOpticalModes,
                        previewResolution = boundPreviewResolution,
                        captureResolution = boundCaptureResolution
                    )
                )
                Toast.makeText(context, commonUiCopy.cameraFailedToStart, Toast.LENGTH_SHORT).show()
            }
        }
        onDispose {
            onBindingStateChange(false)
            faceTrackingAnalysis?.clearAnalyzer()
            faceTrackingPipeline.reset()
            boundFaceTracking = false
            faceTrackingAnalysisUseCase = null
            boundCamera = null
            boundPhysicalCameraId = null
            activePhysicalCameraId = null
            boundPreviewResolution = null
            boundCaptureResolution = null
            imageCapture = null
            if (faceTrackingAnalysis != null) {
                currentProvider.unbind(preview, faceTrackingAnalysis, capture)
            } else {
                currentProvider.unbind(preview, capture)
            }
        }
    }

    LaunchedEffect(imageCapture, flashEnabled, useFrontCamera) {
        imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
    }

    LaunchedEffect(targetRotation, imageCapture, faceTrackingAnalysisUseCase) {
        imageCapture?.targetRotation = targetRotation
        faceTrackingAnalysisUseCase?.targetRotation = targetRotation
    }

    LaunchedEffect(boundCamera) {
        val camera = boundCamera
        if (camera == null) {
            onExposureBiasCapabilitiesChange(ColorCardCameraExposureBiasCapabilities())
            return@LaunchedEffect
        }
        val exposureState = camera.cameraInfo.exposureState
        val stepValue = exposureState.exposureCompensationStep.toFloat().takeIf { it > 0f } ?: 0.1f
        val range = exposureState.exposureCompensationRange
        val minEv = (range.lower * stepValue)
        val maxEv = (range.upper * stepValue)
        val capabilities = if (maxEv >= minEv) {
            ColorCardCameraExposureBiasCapabilities(
                minEv = minEv,
                maxEv = maxEv,
                stepEv = stepValue
            )
        } else {
            ColorCardCameraExposureBiasCapabilities(stepEv = stepValue)
        }
        onExposureBiasCapabilitiesChange(capabilities)
    }

    LaunchedEffect(
        boundCamera,
        zoomRatio,
        useFrontCamera,
        baseCropCoverage,
        lensMode,
        requestedCameraId,
                requestedPhysicalCameraId,
                boundPhysicalCameraId,
                activePhysicalCameraId,
                usedFallbackSelector,
                stabilizationRuntime
    ) {
        val camera = boundCamera ?: return@LaunchedEffect
        val requestedZoom = zoomRatio.coerceAtLeast(1f)
        val zoomState = camera.cameraInfo.zoomState.value
        val minZoomRatio = (zoomState?.minZoomRatio ?: 1f).coerceAtLeast(1f)
        val maxZoomRatio = (zoomState?.maxZoomRatio ?: requestedZoom).coerceAtLeast(minZoomRatio)
        val appliedZoomRatio = requestedZoom.coerceIn(minZoomRatio, maxZoomRatio)
        val zoomCoverage = if (useFrontCamera || requestedZoom <= 1.0001f) {
            1f
        } else {
            (appliedZoomRatio / requestedZoom).coerceIn(0.2f, 1f)
        }
        val effectiveCoverage = (baseCropCoverage * zoomCoverage).coerceIn(0.2f, 1f)
        currentEffectiveCropCoverage = effectiveCoverage
        onEffectiveCropCoverageChange(effectiveCoverage)
        if (!useFrontCamera) {
            runCatching {
                camera.cameraControl.setZoomRatio(appliedZoomRatio)
            }
        }
        onDebugInfoChange(
            ColorCardCameraDebugInfo(
                lensMode = lensMode,
                requestedCameraId = requestedCameraId,
                requestedPhysicalCameraId = requestedPhysicalCameraId,
                requestedCapturePhysicalCameraId = requestedCapturePhysicalCameraId,
                boundCameraId = runCatching {
                    Camera2CameraInfo.from(camera.cameraInfo).cameraId
                }.getOrNull(),
                boundPhysicalCameraId = boundPhysicalCameraId,
                activePhysicalCameraId = activePhysicalCameraId,
                requestedZoomRatio = requestedZoom,
                appliedZoomRatio = appliedZoomRatio,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio,
                effectiveCropCoverage = effectiveCoverage,
                fallbackBinding = usedFallbackSelector,
                frontCamera = useFrontCamera,
                bindFailed = false,
                requestedVideoStabilizationMode = stabilizationRuntime.requestedVideoMode,
                requestedOpticalStabilizationMode = stabilizationRuntime.requestedOpticalMode,
                activeVideoStabilizationMode = stabilizationRuntime.activeVideoMode,
                activeOpticalStabilizationMode = stabilizationRuntime.activeOpticalMode,
                supportedVideoStabilizationModes = stabilizationRuntime.supportedVideoModes,
                supportedOpticalStabilizationModes = stabilizationRuntime.supportedOpticalModes,
                previewResolution = boundPreviewResolution,
                captureResolution = boundCaptureResolution
            )
        )
    }

    LaunchedEffect(boundCamera, previewHostView, focusRequest, currentEffectiveCropCoverage) {
        val request = focusRequest ?: return@LaunchedEffect
        val camera = boundCamera ?: return@LaunchedEffect
        val hostView = previewHostView ?: return@LaunchedEffect
        val display = hostView.display ?: context.display
        val meteringPointNormalized = hostView.mapTapToMeteringNormalized(
            normalizedX = request.normalizedX,
            normalizedY = request.normalizedY
        )
        val pointFactory: MeteringPointFactory = DisplayOrientedMeteringPointFactory(
            display,
            camera.cameraInfo,
            hostView.width.coerceAtLeast(1).toFloat(),
            hostView.height.coerceAtLeast(1).toFloat()
        )
        val fallbackMappedX = remapColorCardCameraTapToMeteringAxis(
            normalized = request.normalizedX,
            cropCoverage = currentEffectiveCropCoverage
        )
        val fallbackMappedY = remapColorCardCameraTapToMeteringAxis(
            normalized = request.normalizedY,
            cropCoverage = currentEffectiveCropCoverage
        )
        val mappedX = meteringPointNormalized.x.takeIf { it.isFinite() } ?: fallbackMappedX
        val mappedY = meteringPointNormalized.y.takeIf { it.isFinite() } ?: fallbackMappedY
        val hostWidth = hostView.width.coerceAtLeast(1).toFloat()
        val hostHeight = hostView.height.coerceAtLeast(1).toFloat()
        val focusX = mappedX * hostWidth
        val focusY = mappedY * hostHeight
        val afPoint = pointFactory.createPoint(
            focusX,
            focusY,
            COLOR_CARD_CAMERA_TAP_FOCUS_AF_POINT_SIZE
        )
        val aePoint = pointFactory.createPoint(
            focusX,
            focusY,
            COLOR_CARD_CAMERA_TAP_FOCUS_AE_POINT_SIZE
        )
        val awbPoint = pointFactory.createPoint(
            focusX,
            focusY,
            COLOR_CARD_CAMERA_TAP_FOCUS_AWB_POINT_SIZE
        )
        val actionBuilder = FocusMeteringAction.Builder(
            afPoint,
            FocusMeteringAction.FLAG_AF
        )
            .addPoint(aePoint, FocusMeteringAction.FLAG_AE)
            .addPoint(awbPoint, FocusMeteringAction.FLAG_AWB)
        if (!request.lockFocus) {
            actionBuilder.setAutoCancelDuration(
                COLOR_CARD_CAMERA_TAP_FOCUS_AUTO_CANCEL_SECONDS,
                java.util.concurrent.TimeUnit.SECONDS
            )
        }
        runCatching {
            camera.cameraControl.cancelFocusAndMetering()
        }
        runCatching {
            camera.cameraControl.startFocusAndMetering(actionBuilder.build())
        }
    }

    LaunchedEffect(boundCamera, meteringExposureBiasEv) {
        val camera = boundCamera ?: return@LaunchedEffect
        val exposureState = camera.cameraInfo.exposureState
        val stepValue = exposureState.exposureCompensationStep.toFloat().takeIf { it > 0f } ?: return@LaunchedEffect
        val targetIndex = (meteringExposureBiasEv / stepValue).roundToInt()
            .coerceIn(
                exposureState.exposureCompensationRange.lower,
                exposureState.exposureCompensationRange.upper
            )
        runCatching {
            camera.cameraControl.setExposureCompensationIndex(targetIndex)
        }
    }

    LaunchedEffect(captureRequestNonce) {
        if (captureRequestNonce <= 0) return@LaunchedEffect
        val capture = imageCapture ?: return@LaunchedEffect
        onCaptureStateChange(true)
        takeColorCardCameraBurstPicture(
            context = context,
            imageCapture = capture,
            camera = boundCamera,
            useFrontCamera = useFrontCamera,
            flashEnabled = flashEnabled,
            frameCount = if (multiFrameFusionEnabled) 3 else 1,
            executor = captureExecutor,
            onComplete = { burst ->
                mainExecutor.execute {
                    onCaptureStateChange(false)
                    onCaptured(
                        ColorCardCameraCaptureResult.LegacyJpeg(
                            rawFile = null,
                            jpegFile = burst.first(),
                            burstJpegFiles = burst
                        )
                    )
                }
            },
            onError = {
                mainExecutor.execute {
                    onCaptureStateChange(false)
                    Toast.makeText(context, commonUiCopy.captureFailed, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun ColorCardCameraFilterRevealOverlay(
    revealKey: String,
    revealTriggerNonce: Int,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val revealBandWidthPx = with(density) { 72.dp.toPx() }
    val revealProgress = remember { androidx.compose.animation.core.Animatable(1f) }
    var previewSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var skipInitialReveal by remember { mutableStateOf(true) }
    var lastRevealKey by remember { mutableStateOf(revealKey) }
    var consumedRevealTriggerNonce by remember { mutableStateOf(revealTriggerNonce) }

    LaunchedEffect(revealKey, revealTriggerNonce) {
        revealProgress.stop()
        if (skipInitialReveal) {
            skipInitialReveal = false
            lastRevealKey = revealKey
            consumedRevealTriggerNonce = revealTriggerNonce
            revealProgress.snapTo(1f)
        } else {
            val revealKeyChanged = revealKey != lastRevealKey
            val userTriggeredRevealPending = revealTriggerNonce != consumedRevealTriggerNonce
            if (revealKeyChanged && userTriggeredRevealPending) {
                revealProgress.snapTo(0f)
                revealProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                )
                consumedRevealTriggerNonce = revealTriggerNonce
            } else {
                revealProgress.snapTo(1f)
            }
            lastRevealKey = revealKey
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { previewSize = it }
    ) {
        if (revealProgress.value in 0.001f..0.999f && previewSize.width > 0) {
            val revealLeft = (previewSize.width * (1f - revealProgress.value))
                .coerceIn(0f, previewSize.width.toFloat())
            val bandLeft = (revealLeft - (revealBandWidthPx / 2f))
                .coerceIn(0f, previewSize.width.toFloat())
            val bandWidth = revealBandWidthPx
                .coerceAtMost(previewSize.width.toFloat() - bandLeft)
                .coerceAtLeast(0f)
            if (bandWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                x = bandLeft.toInt(),
                                y = 0
                            )
                        }
                        .width(with(density) { bandWidth.toDp() })
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

private data class ColorCardCameraFocusRequest(
    val normalizedX: Float,
    val normalizedY: Float,
    val lockFocus: Boolean,
    val nonce: Long
)

private fun remapColorCardCameraTapToMeteringAxis(
    normalized: Float,
    cropCoverage: Float
): Float {
    val safeNormalized = normalized.coerceIn(0f, 1f)
    val safeCropCoverage = cropCoverage.coerceIn(0.2f, 1f)
    val inset = (1f - safeCropCoverage) / 2f
    return (inset + (safeNormalized * safeCropCoverage)).coerceIn(0f, 1f)
}

private data class ColorCardCameraFocusIndicator(
    val offset: Offset,
    val locked: Boolean,
    val exposureBiasEv: Float,
    val nonce: Long
)

private data class ColorCardCameraExposureBiasCapabilities(
    val minEv: Float = COLOR_CARD_CAMERA_FOCUS_DEFAULT_EXPOSURE_MIN_EV,
    val maxEv: Float = COLOR_CARD_CAMERA_FOCUS_DEFAULT_EXPOSURE_MAX_EV,
    val stepEv: Float = 0.1f
) {
    val range: ClosedFloatingPointRange<Float>
        get() = minEv..maxEv

    fun coerce(ev: Float): Float = ev.coerceIn(minEv, maxEv)

    fun snap(ev: Float): Float {
        val safeStep = stepEv.takeIf { it > 0.0001f } ?: return coerce(ev)
        val snapped = minEv + (kotlin.math.round((coerce(ev) - minEv) / safeStep) * safeStep)
        return snapped.coerceIn(minEv, maxEv)
    }
}

private data class ColorCardCameraFocusOverlayMetrics(
    val frameSizePx: Float,
    val frameHalfSizePx: Float,
    val cornerLengthPx: Float,
    val strokeWidthPx: Float,
    val sliderCenterXPx: Float,
    val sliderTopPx: Float,
    val sliderBottomPx: Float,
    val sliderHeightPx: Float,
    val sliderKnobRadiusPx: Float
)

@Composable
private fun rememberColorCardCameraFocusOverlayMetrics(
    indicator: ColorCardCameraFocusIndicator
): ColorCardCameraFocusOverlayMetrics {
    val density = androidx.compose.ui.platform.LocalDensity.current
    return remember(indicator.offset, density) {
        with(density) {
            val frameSizePx = 72.dp.toPx()
            val sliderHeightPx = 92.dp.toPx()
            ColorCardCameraFocusOverlayMetrics(
                frameSizePx = frameSizePx,
                frameHalfSizePx = frameSizePx / 2f,
                cornerLengthPx = 16.dp.toPx(),
                strokeWidthPx = 3.dp.toPx(),
                sliderCenterXPx = indicator.offset.x + 54.dp.toPx(),
                sliderTopPx = indicator.offset.y - (sliderHeightPx / 2f),
                sliderBottomPx = indicator.offset.y + (sliderHeightPx / 2f),
                sliderHeightPx = sliderHeightPx,
                sliderKnobRadiusPx = 8.dp.toPx()
            )
        }
    }
}

private fun resolveColorCardCameraFocusExposureBiasFromDrag(
    startExposureBiasEv: Float,
    totalDragYPx: Float,
    previewHeightPx: Float,
    capabilities: ColorCardCameraExposureBiasCapabilities
): Float {
    val safeHeight = previewHeightPx.coerceAtLeast(1f)
    val fullRangeEv = (capabilities.maxEv - capabilities.minEv).coerceAtLeast(0f)
    if (fullRangeEv <= 0.0001f) {
        return capabilities.coerce(startExposureBiasEv)
    }
    val deltaEv = (-totalDragYPx / safeHeight) * fullRangeEv
    return capabilities.coerce(startExposureBiasEv + deltaEv)
}

@Composable
private fun ColorCardCameraFocusIndicatorOverlay(
    indicator: ColorCardCameraFocusIndicator?,
    exposureBiasCapabilities: ColorCardCameraExposureBiasCapabilities,
    modifier: Modifier = Modifier
) {
    val current = indicator ?: return
    val density = androidx.compose.ui.platform.LocalDensity.current
    var visible by remember(current.nonce) { mutableStateOf(true) }
    LaunchedEffect(current.nonce, current.locked) {
        visible = true
        if (current.locked) return@LaunchedEffect
        delay(1400L)
        visible = false
    }
    if (!visible) return
    val metrics = rememberColorCardCameraFocusOverlayMetrics(current)
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        x = (current.offset.x - metrics.frameHalfSizePx).roundToInt(),
                        y = (current.offset.y - metrics.frameHalfSizePx).roundToInt()
                    )
                }
                .size(with(density) { metrics.frameSizePx.toDp() })
        ) {
            val color = COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR
            val maxX = size.width
            val maxY = size.height
            val corner = metrics.cornerLengthPx
            val stroke = metrics.strokeWidthPx
            val inset = stroke / 2f
            drawColorCardCameraCornerFrame(
                left = inset,
                top = inset,
                right = maxX - inset,
                bottom = maxY - inset,
                corner = corner,
                strokeWidth = stroke,
                color = color
            )
        }
        Column(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        x = (metrics.sliderCenterXPx - with(density) { 14.dp.toPx() }).roundToInt(),
                        y = (metrics.sliderTopPx - with(density) { 28.dp.toPx() }).roundToInt()
                    )
                }
                .width(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (current.exposureBiasEv >= 0f) {
                    "+${String.format(java.util.Locale.US, "%.1f", current.exposureBiasEv)}"
                } else {
                    String.format(java.util.Locale.US, "%.1f", current.exposureBiasEv)
                },
                color = COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Canvas(
                modifier = Modifier
                    .width(24.dp)
                    .height(with(density) { metrics.sliderHeightPx.toDp() })
            ) {
                val centerX = size.width / 2f
                val top = 0f
                val bottom = size.height
                val minEv = exposureBiasCapabilities.minEv
                val maxEv = exposureBiasCapabilities.maxEv
                val rangeEv = (maxEv - minEv).takeIf { it > 0.0001f } ?: 1f
                val normalized =
                    ((current.exposureBiasEv - minEv) / rangeEv)
                        .coerceIn(0f, 1f)
                val knobY = top + ((1f - normalized) * (bottom - top))
                drawLine(
                    color = COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR.copy(alpha = 0.38f),
                    start = Offset(centerX, top),
                    end = Offset(centerX, bottom),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR.copy(alpha = 0.72f),
                    start = Offset(centerX - 5.dp.toPx(), size.height / 2f),
                    end = Offset(centerX + 5.dp.toPx(), size.height / 2f),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR,
                    radius = metrics.sliderKnobRadiusPx.coerceAtMost(size.width / 2f),
                    center = Offset(centerX, knobY)
                )
            }
            if (current.locked) {
                Text(
                    text = "LOCK",
                    color = COLOR_CARD_CAMERA_FOCUS_INDICATOR_COLOR.copy(alpha = 0.92f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun createColorCardCameraImageCapture(
    context: Context,
    cameraId: String?,
    targetRotation: Int,
    useCaseProfile: ColorCardCameraUseCaseProfile,
    physicalCameraId: String?,
    experimentalYuvCapture: Boolean,
    stabilizationConfig: ColorCardCameraStabilizationConfig,
    captureCallback: CameraCaptureSession.CaptureCallback
): ImageCapture {
    val captureFormat = if (experimentalYuvCapture) ImageFormat.YUV_420_888 else ImageFormat.JPEG
    val captureDiagnostics = resolveColorCardCameraCaptureDiagnostics(
        context = context,
        cameraId = cameraId,
        physicalCameraId = physicalCameraId,
        imageFormat = captureFormat,
        fallbackSize = useCaseProfile.captureTargetSize
    )
    val captureTargetSize = captureDiagnostics.selectedSize
    val builder = ImageCapture.Builder()
        .setCaptureMode(useCaseProfile.captureMode)
        .setResolutionSelector(
            buildColorCardCameraCaptureResolutionSelector(captureTargetSize)
        )
        .setTargetRotation(targetRotation)
    configureColorCardCameraExperimentalYuvCapture(
        builder = builder,
        enabled = experimentalYuvCapture
    )
    applyColorCardCameraInterop(
        builder = builder,
        physicalCameraId = physicalCameraId,
        stabilizationConfig = stabilizationConfig,
        captureCallback = captureCallback
    )
    Log.d(
        COLOR_CARD_CAMERA_LOG_TAG,
        buildString {
            append("ImageCapture plan fmt=")
            append(captureDiagnostics.imageFormatLabel)
            append(" req=")
            append(formatColorCardCameraSize(useCaseProfile.captureTargetSize))
            append(" sel=")
            append(formatColorCardCameraSize(captureDiagnostics.selectedSize))
            append(" max=")
            append(formatColorCardCameraSize(captureDiagnostics.maxAvailableSize))
            append(" top=")
            append(
                captureDiagnostics.availableSizes
                    .take(6)
                    .joinToString("/") { formatColorCardCameraSize(it) }
            )
            append(" camera=")
            append(cameraId ?: "-")
            append(" physical=")
            append(physicalCameraId ?: "-")
        }
    )
    return builder.build()
}

private data class ColorCardCameraUseCaseProfile(
    val previewTargetSize: Size,
    val captureTargetSize: Size,
    val previewFrameRate: Range<Int>,
    val livePreviewRenderFps: Int,
    val captureMode: Int
)

private fun resolveColorCardCameraUseCaseProfile(
    perfMode: AppPerformanceGovernor.PerfMode,
    lensMode: ColorCardCameraLensMode,
    useFrontCamera: Boolean,
    outputAspectRatio: Float
): ColorCardCameraUseCaseProfile {
    val previewTargetSize = resolveColorCardCameraPreviewTargetSize(
        perfMode = perfMode,
        lensMode = lensMode,
        useFrontCamera = useFrontCamera,
        outputAspectRatio = outputAspectRatio
    )
    val captureTargetSize = buildColorCardCameraTargetSizeForAspect(
        shortSide = 3072,
        outputAspectRatio = COLOR_CARD_CAMERA_OUTPUT_ASPECT_RATIO
    )
    return when (perfMode) {
        AppPerformanceGovernor.PerfMode.NORMAL -> ColorCardCameraUseCaseProfile(
            previewTargetSize = previewTargetSize,
            captureTargetSize = captureTargetSize,
            previewFrameRate = COLOR_CARD_CAMERA_PREVIEW_FRAME_RATE,
            livePreviewRenderFps = COLOR_CARD_CAMERA_LIVE_PREVIEW_RENDER_FPS,
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        )
        AppPerformanceGovernor.PerfMode.BALANCED -> ColorCardCameraUseCaseProfile(
            previewTargetSize = previewTargetSize,
            captureTargetSize = captureTargetSize,
            previewFrameRate = COLOR_CARD_CAMERA_PREVIEW_FRAME_RATE,
            livePreviewRenderFps = COLOR_CARD_CAMERA_LIVE_PREVIEW_RENDER_FPS,
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        )
        AppPerformanceGovernor.PerfMode.COOL -> ColorCardCameraUseCaseProfile(
            previewTargetSize = previewTargetSize,
            captureTargetSize = captureTargetSize,
            previewFrameRate = COLOR_CARD_CAMERA_PREVIEW_FRAME_RATE,
            livePreviewRenderFps = COLOR_CARD_CAMERA_LIVE_PREVIEW_RENDER_FPS,
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        )
    }
}

private fun resolveColorCardCameraPreviewTargetSize(
    perfMode: AppPerformanceGovernor.PerfMode,
    lensMode: ColorCardCameraLensMode,
    useFrontCamera: Boolean,
    outputAspectRatio: Float
): Size {
    if (
        useFrontCamera ||
        lensMode == ColorCardCameraLensMode.Native ||
        lensMode == ColorCardCameraLensMode.Eq35 ||
        lensMode == ColorCardCameraLensMode.Eq50
    ) {
        return buildColorCardCameraTargetSizeForAspect(
            shortSide = 960,
            outputAspectRatio = outputAspectRatio
        )
    }
    return when (perfMode) {
        AppPerformanceGovernor.PerfMode.COOL -> when (lensMode) {
            ColorCardCameraLensMode.Eq35,
            ColorCardCameraLensMode.Eq50 -> buildColorCardCameraTargetSizeForAspect(1200, outputAspectRatio)
            ColorCardCameraLensMode.Tele1,
            ColorCardCameraLensMode.Tele2 -> buildColorCardCameraTargetSizeForAspect(1280, outputAspectRatio)
            ColorCardCameraLensMode.Native -> buildColorCardCameraTargetSizeForAspect(960, outputAspectRatio)
        }
        AppPerformanceGovernor.PerfMode.NORMAL,
        AppPerformanceGovernor.PerfMode.BALANCED -> when (lensMode) {
            ColorCardCameraLensMode.Eq35,
            ColorCardCameraLensMode.Eq50 -> buildColorCardCameraTargetSizeForAspect(1440, outputAspectRatio)
            ColorCardCameraLensMode.Tele1,
            ColorCardCameraLensMode.Tele2 -> buildColorCardCameraTargetSizeForAspect(1600, outputAspectRatio)
            ColorCardCameraLensMode.Native -> buildColorCardCameraTargetSizeForAspect(960, outputAspectRatio)
        }
    }
}

private fun buildColorCardCameraResolutionSelector(targetSize: Size): ResolutionSelector {
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(
            AspectRatioStrategy(
                AspectRatio.RATIO_4_3,
                AspectRatioStrategy.FALLBACK_RULE_AUTO
            )
        )
        .setResolutionFilter { supportedSizes, _ ->
            supportedSizes.filterColorCardCameraPreferredFourThree()
        }
        .setResolutionStrategy(
            ResolutionStrategy(
                targetSize,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()
}

private fun buildColorCardCameraCaptureResolutionSelector(targetSize: Size): ResolutionSelector {
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(
            AspectRatioStrategy(
                AspectRatio.RATIO_4_3,
                AspectRatioStrategy.FALLBACK_RULE_AUTO
            )
        )
        .setResolutionFilter { supportedSizes, _ ->
            supportedSizes.filterColorCardCameraPreferredFourThree()
        }
        .setAllowedResolutionMode(
            ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
        )
        .setResolutionStrategy(
            ResolutionStrategy(
                targetSize,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()
}

private data class ColorCardCameraCaptureResolutionDiagnostics(
    val selectedSize: Size,
    val maxAvailableSize: Size,
    val availableSizes: List<Size>,
    val imageFormatLabel: String
)

private data class ColorCardCameraCaptureSizeSummary(
    val bestFourThree: Size?,
    val largest: Size?
)

private fun resolveColorCardCameraCaptureDiagnostics(
    context: Context,
    cameraId: String?,
    physicalCameraId: String?,
    imageFormat: Int,
    fallbackSize: Size
): ColorCardCameraCaptureResolutionDiagnostics {
    val imageFormatLabel = when (imageFormat) {
        ImageFormat.JPEG -> "JPEG"
        ImageFormat.YUV_420_888 -> "YUV_420_888"
        else -> imageFormat.toString()
    }
    val fallbackDiagnostics = ColorCardCameraCaptureResolutionDiagnostics(
        selectedSize = fallbackSize,
        maxAvailableSize = fallbackSize,
        availableSizes = listOf(fallbackSize),
        imageFormatLabel = imageFormatLabel
    )
    val cameraManager = context.getSystemService(CameraManager::class.java) ?: return fallbackDiagnostics
    val characteristics = runCatching {
        when {
            !physicalCameraId.isNullOrEmpty() -> cameraManager.getCameraCharacteristics(physicalCameraId)
            !cameraId.isNullOrEmpty() -> cameraManager.getCameraCharacteristics(cameraId)
            else -> null
        }
    }.getOrNull() ?: return fallbackDiagnostics
    val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        ?: return fallbackDiagnostics
    val outputSizes = streamConfigurationMap.getOutputSizes(imageFormat).orEmpty()
    if (outputSizes.isEmpty()) return fallbackDiagnostics

    val fallbackRatio = fallbackSize.height.toFloat() / fallbackSize.width.toFloat().coerceAtLeast(1f)
    val sortedSizes = outputSizes
        .map(::normalizeColorCardCameraSizeToPortrait)
        .sortedWith(
            compareBy<Size> { abs((it.height.toFloat() / it.width.toFloat().coerceAtLeast(1f)) - fallbackRatio) }
                .thenByDescending { isColorCardCameraFourThree(it) }
                .thenByDescending { it.width.toLong() * it.height.toLong() }
        )
    val selectedSize = sortedSizes.firstOrNull() ?: fallbackSize
    return ColorCardCameraCaptureResolutionDiagnostics(
        selectedSize = selectedSize,
        maxAvailableSize = selectedSize,
        availableSizes = sortedSizes,
        imageFormatLabel = imageFormatLabel
    )
}

private fun normalizeColorCardCameraSizeToPortrait(size: Size): Size {
    return if (size.width <= size.height) size else Size(size.height, size.width)
}

private fun summarizeColorCardCameraCaptureSizes(
    characteristics: CameraCharacteristics,
    imageFormat: Int = ImageFormat.JPEG
): ColorCardCameraCaptureSizeSummary {
    val outputSizes = characteristics
        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        ?.getOutputSizes(imageFormat)
        .orEmpty()
        .map(::normalizeColorCardCameraSizeToPortrait)
        .sortedByDescending { it.width.toLong() * it.height.toLong() }
    return ColorCardCameraCaptureSizeSummary(
        bestFourThree = outputSizes.firstOrNull(::isColorCardCameraFourThree),
        largest = outputSizes.firstOrNull()
    )
}

private fun resolveColorCardCameraPreferredStillCapturePhysicalId(
    context: Context,
    cameraId: String?,
    previewPhysicalCameraId: String?
): String? {
    if (!isColorCardCameraPhysicalSubLens(cameraId = cameraId, physicalCameraId = previewPhysicalCameraId)) {
        return previewPhysicalCameraId
    }
    val bindCameraId = cameraId ?: return previewPhysicalCameraId
    val physicalId = previewPhysicalCameraId ?: return null
    val cameraManager = context.getSystemService(CameraManager::class.java) ?: return previewPhysicalCameraId
    val logicalCharacteristics = runCatching {
        cameraManager.getCameraCharacteristics(bindCameraId)
    }.getOrNull() ?: return previewPhysicalCameraId
    val physicalCharacteristics = runCatching {
        cameraManager.getCameraCharacteristics(physicalId)
    }.getOrNull() ?: return previewPhysicalCameraId
    val logicalSummary = summarizeColorCardCameraCaptureSizes(logicalCharacteristics)
    val physicalSummary = summarizeColorCardCameraCaptureSizes(physicalCharacteristics)
    val logicalFourThree = logicalSummary.bestFourThree
    val physicalFourThree = physicalSummary.bestFourThree
    if (logicalFourThree == null) {
        return previewPhysicalCameraId
    }
    if (physicalFourThree == null) {
        return null
    }
    val logicalArea = logicalFourThree.width.toLong() * logicalFourThree.height.toLong()
    val physicalArea = physicalFourThree.width.toLong() * physicalFourThree.height.toLong()
    return if (physicalArea * 100L >= logicalArea * 82L) {
        previewPhysicalCameraId
    } else {
        null
    }
}

private fun formatColorCardCameraSize(size: Size?): String {
    if (size == null) return "-"
    return "${size.width}x${size.height}"
}

private fun formatColorCardCameraAspectRatio(value: Float): String {
    val safeValue = value.coerceAtLeast(0.0001f)
    return String.format(java.util.Locale.US, "%.4f", safeValue)
}

private fun formatColorCardCameraSavedJpegDimensions(file: File): String {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return "unknown"
    }
    return "${bounds.outWidth}x${bounds.outHeight}"
}

internal fun applyColorCardCameraPreviewEffect(
    params: ColorCardTraceGpuParams,
    effect: ColorCardCameraPreviewEffect
): ColorCardTraceGpuParams {
    return when (effect) {
        ColorCardCameraPreviewEffect.Dehaze -> params.copy(
            blackMistEnabled = false,
            blackMistStrength = 0f,
            sparkleStrength = 0f,
            dehazeStrength = 0.56f,
            blackMistSoftnessBias = 0f,
            blackMistBloomBias = 0f,
            blackMistVeilBias = 0f,
            blackMistHighlightBias = 0f,
            blackMistContrastBias = 0f,
            blackMistTintRed = 0.965f,
            blackMistTintGreen = 0.851f,
            blackMistTintBlue = 0.933f,
            sharpen = (params.sharpen + 0.08f).coerceIn(-1f, 1f),
            tone = (params.tone + 0.14f).coerceIn(-1f, 1f),
            brightness = (params.brightness - 0.04f).coerceIn(-1f, 1f),
            saturation = (params.saturation + 0.04f).coerceIn(-1f, 1f)
        )
        ColorCardCameraPreviewEffect.None -> params.copy(
            blackMistEnabled = false,
            blackMistStrength = 0f,
            sparkleStrength = 0f,
            dehazeStrength = 0f,
            blackMistSoftnessBias = 0f,
            blackMistBloomBias = 0f,
            blackMistVeilBias = 0f,
            blackMistHighlightBias = 0f,
            blackMistContrastBias = 0f,
            blackMistTintRed = 0.965f,
            blackMistTintGreen = 0.851f,
            blackMistTintBlue = 0.933f
        )
        ColorCardCameraPreviewEffect.BlackMist -> params.copy(
            blackMistEnabled = true,
            blackMistStrength = 0.34f,
            sparkleStrength = 0f,
            dehazeStrength = 0f,
            blackMistSoftnessBias = -0.05f,
            blackMistBloomBias = -0.08f,
            blackMistVeilBias = -0.012f,
            blackMistHighlightBias = 0.12f,
            blackMistContrastBias = 0.22f,
            blackMistRadiusBias = -0.22f,
            blackMistHaloBias = -0.12f,
            blackMistAtmosphereBias = -0.16f,
            blackMistTintRed = 0.92f,
            blackMistTintGreen = 0.87f,
            blackMistTintBlue = 0.90f,
            sharpen = (params.sharpen - 0.08f).coerceIn(-1f, 1f),
            tone = (params.tone - 0.14f).coerceIn(-1f, 1f),
            brightness = (params.brightness - 0.08f).coerceIn(-1f, 1f),
            saturation = (params.saturation - 0.06f).coerceIn(-1f, 1f)
        )
        ColorCardCameraPreviewEffect.WhiteMist -> params.copy(
            blackMistEnabled = true,
            blackMistStrength = 0.56f,
            sparkleStrength = 0f,
            dehazeStrength = 0f,
            blackMistSoftnessBias = 0.02f,
            blackMistBloomBias = 0.06f,
            blackMistVeilBias = 0.006f,
            blackMistHighlightBias = -0.03f,
            blackMistContrastBias = -0.10f,
            blackMistRadiusBias = 0.05f,
            blackMistHaloBias = 0.08f,
            blackMistAtmosphereBias = 0.06f,
            blackMistTintRed = 1.0f,
            blackMistTintGreen = 0.985f,
            blackMistTintBlue = 0.972f,
            sharpen = (params.sharpen - 0.20f).coerceIn(-1f, 1f),
            tone = (params.tone + 0.10f).coerceIn(-1f, 1f),
            brightness = (params.brightness + 0.12f).coerceIn(-1f, 1f),
            saturation = (params.saturation - 0.05f).coerceIn(-1f, 1f)
        )
        ColorCardCameraPreviewEffect.Glow -> params.copy(
            blackMistEnabled = true,
            blackMistStrength = 0.88f,
            sparkleStrength = 0.46f,
            dehazeStrength = 0f,
            blackMistSoftnessBias = 0.10f,
            blackMistBloomBias = 0.22f,
            blackMistVeilBias = 0.004f,
            blackMistHighlightBias = -0.12f,
            blackMistContrastBias = -0.10f,
            blackMistRadiusBias = 0.34f,
            blackMistHaloBias = 0.42f,
            blackMistAtmosphereBias = -0.04f,
            blackMistTintRed = 1.0f,
            blackMistTintGreen = 0.95f,
            blackMistTintBlue = 0.90f,
            sharpen = (params.sharpen - 0.22f).coerceIn(-1f, 1f),
            tone = (params.tone + 0.14f).coerceIn(-1f, 1f),
            brightness = (params.brightness + 0.24f).coerceIn(-1f, 1f),
            saturation = (params.saturation + 0.18f).coerceIn(-1f, 1f)
        )
        ColorCardCameraPreviewEffect.Haze -> params.copy(
            blackMistEnabled = true,
            blackMistStrength = 0.68f,
            sparkleStrength = 0f,
            dehazeStrength = 0f,
            blackMistSoftnessBias = 0.08f,
            blackMistBloomBias = -0.02f,
            blackMistVeilBias = 0.026f,
            blackMistHighlightBias = 0.06f,
            blackMistContrastBias = -0.24f,
            blackMistRadiusBias = 0.22f,
            blackMistHaloBias = -0.18f,
            blackMistAtmosphereBias = 0.38f,
            blackMistTintRed = 0.93f,
            blackMistTintGreen = 0.96f,
            blackMistTintBlue = 1.0f,
            sharpen = (params.sharpen - 0.34f).coerceIn(-1f, 1f),
            tone = (params.tone - 0.10f).coerceIn(-1f, 1f),
            brightness = (params.brightness + 0.08f).coerceIn(-1f, 1f),
            saturation = (params.saturation - 0.10f).coerceIn(-1f, 1f)
        )
    }
}

private fun ColorCardCameraPreviewEffect.next(): ColorCardCameraPreviewEffect {
    val values = ColorCardCameraPreviewEffect.values()
    return values[(ordinal + 1) % values.size]
}

private fun ColorCardCameraPreviewEffect.previous(): ColorCardCameraPreviewEffect {
    val values = ColorCardCameraPreviewEffect.values()
    return values[(ordinal - 1 + values.size) % values.size]
}

private fun isColorCardCameraFourThree(size: Size): Boolean {
    val ratio = size.height.toFloat() / size.width.toFloat().coerceAtLeast(1f)
    return abs(ratio - (4f / 3f)) <= 0.03f
}

private fun List<Size>.filterColorCardCameraPreferredFourThree(): List<Size> {
    val fourThree = filter { size ->
        isColorCardCameraFourThree(normalizeColorCardCameraSizeToPortrait(size))
    }
    return if (fourThree.isNotEmpty()) fourThree else this
}

private suspend fun takeColorCardCameraBurstPicture(
    context: Context,
    imageCapture: ImageCapture,
    camera: Camera?,
    useFrontCamera: Boolean,
    flashEnabled: Boolean,
    frameCount: Int,
    executor: ExecutorService,
    onComplete: (List<File>) -> Unit,
    onError: () -> Unit
) {
    val burstFiles = mutableListOf<File>()
    val useTorchAssist = flashEnabled &&
        !useFrontCamera &&
        runCatching { camera?.cameraInfo?.hasFlashUnit() == true }.getOrDefault(false)
    val metadata = ImageCapture.Metadata().apply {
        if (useFrontCamera) {
            isReversedHorizontal = false
        }
        isReversedVertical = false
    }
    if (useTorchAssist) {
        var torchWarmupCompleted = false
        try {
            runCatching { camera?.cameraControl?.enableTorch(true) }
            delay(140L)
            torchWarmupCompleted = true
        } finally {
            if (!torchWarmupCompleted) {
                runCatching { camera?.cameraControl?.enableTorch(false) }
            }
        }
    }
    fun captureSequential(index: Int) {
        if (index >= frameCount.coerceAtLeast(1)) {
            if (useTorchAssist) {
                runCatching { camera?.cameraControl?.enableTorch(false) }
            }
            onComplete(burstFiles.toList())
            return
        }
        val jpegFile = createCameraTempFile(
            context = context,
            prefix = "capture_",
            suffix = ".jpg"
        )
        val jpegOptions = ImageCapture.OutputFileOptions.Builder(jpegFile)
            .setMetadata(metadata)
            .build()
        imageCapture.takePicture(
            jpegOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(
                        COLOR_CARD_CAMERA_LOG_TAG,
                        "ImageCapture saved file=${formatColorCardCameraSavedJpegDimensions(jpegFile)} " +
                            "path=${jpegFile.name}"
                    )
                    burstFiles += jpegFile
                    captureSequential(index + 1)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (useTorchAssist) {
                        runCatching { camera?.cameraControl?.enableTorch(false) }
                    }
                    burstFiles.forEach { file -> file.takeIf(File::exists)?.delete() }
                    onError()
                }
            }
        )
    }
    captureSequential(0)
}

@Composable
private fun ColorCardCameraLensSelectorOverlay(
    selected: ColorCardCameraLensMode,
    modes: List<ColorCardCameraLensMode>,
    labels: Map<ColorCardCameraLensMode, String>,
    textRotationDegrees: Float,
    enabled: Boolean,
    onSelected: (ColorCardCameraLensMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        modes.forEach { mode ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = if (selected == mode) Color.White else Color.White.copy(alpha = 0.42f),
                        shape = CircleShape
                    )
                    .clickable(enabled = enabled) { onSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                if (selected == mode) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                }
                Text(
                    text = labels[mode] ?: mode.label,
                    color = if (selected == mode) Color.White else Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer { rotationZ = textRotationDegrees }
                )
            }
        }
    }
}

@Composable
private fun ColorCardCameraFilterPanel(
    state: ColorCardCameraFilterUiState,
    onExpandBrowser: () -> Unit
) {
    val traceState = remember(
        state.activeSeries,
        state.candidates,
        state.selectedCandidate?.paletteId,
        state.selectedStyleTemplate?.candidate?.paletteId,
        state.color,
        state.tone,
        state.sharpen,
        state.saturation,
        state.brightness,
        state.strength,
        state.filmGrainEligible,
        state.filmGrainEnabled,
        state.selectedFilmGrainAsset?.id,
        state.filmGrainAssets
    ) {
        state.buildPanelState()
    }
    val panelShape = RoundedCornerShape(24.dp)
    val backgroundColor = Color(0xFF121314).copy(alpha = 0.96f)
    val borderColor = Color.White.copy(alpha = 0.10f)
    val primaryText = Color.White.copy(alpha = 0.96f)
    val secondaryText = Color.White.copy(alpha = 0.56f)

    ColorCardTraceLutPanel(
        traceState = traceState,
        isDark = true,
        panelShape = panelShape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        primaryText = primaryText,
        secondaryText = secondaryText,
        saveImageState = ColorCardTraceSaveImageState.Idle,
        comparePressed = false,
        onSaveRecipe = {},
        onComparePressedChange = {},
        onExpandBrowser = onExpandBrowser,
        onToggleFilmGrain = state.setFilmGrainEnabled,
        onOpenFilmGrainBrowser = state.refreshFilmGrainAsset,
        onSaveImage = {},
        showSaveRecipeAction = false,
        showSaveImageAction = false,
        adjustmentPadContinuousInput = false,
        adjustmentPadUseFullArea = false,
        showTripleSliders = true,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    )
}

private data class ColorCardCameraLensCandidate(
    val bindCameraId: String,
    val physicalCameraId: String?,
    val focal35mmEq: Float,
    val logicalMultiCamera: Boolean,
    val outputAspectRatio: Float
) {
    val selectionKey: String
        get() = physicalCameraId?.let { "$bindCameraId:$it" } ?: bindCameraId

    val isDedicatedOpticalPath: Boolean
        get() = physicalCameraId != null || !logicalMultiCamera
}

private data class ColorCardCameraLensSelection(
    val cameraId: String?,
    val physicalCameraId: String?,
    val capturePhysicalCameraId: String?,
    val nativeFocal35mmEq: Float,
    val selector: CameraSelector,
    val zoomRatio: Float,
    val cropCoverage: Float,
    val enhancementCoverage: Float,
    val outputAspectRatio: Float,
    val requestedFocal35mmEq: Float?,
    val availableModes: List<ColorCardCameraLensMode>,
    val displayLabels: Map<ColorCardCameraLensMode, String>
)

private data class ColorCardCameraLensModeSpec(
    val mode: ColorCardCameraLensMode,
    val displayFocal35mmEq: Float,
    val targetFocal35mmEq: Float?,
    val priority: Int
)

private data class ColorCardCameraDebugInfo(
    val lensMode: ColorCardCameraLensMode,
    val requestedCameraId: String?,
    val requestedPhysicalCameraId: String?,
    val requestedCapturePhysicalCameraId: String?,
    val boundCameraId: String?,
    val boundPhysicalCameraId: String?,
    val activePhysicalCameraId: String?,
    val requestedZoomRatio: Float,
    val appliedZoomRatio: Float,
    val minZoomRatio: Float,
    val maxZoomRatio: Float,
    val effectiveCropCoverage: Float,
    val fallbackBinding: Boolean,
    val frontCamera: Boolean,
    val bindFailed: Boolean,
    val requestedVideoStabilizationMode: Int? = null,
    val requestedOpticalStabilizationMode: Int? = null,
    val activeVideoStabilizationMode: Int? = null,
    val activeOpticalStabilizationMode: Int? = null,
    val supportedVideoStabilizationModes: List<Int> = emptyList(),
    val supportedOpticalStabilizationModes: List<Int> = emptyList(),
    val bindingInProgress: Boolean = false,
    val previewResolution: Size? = null,
    val captureResolution: Size? = null
) {
    fun summaryLines(): List<String> {
        val lensLabel = if (frontCamera) "Front" else lensMode.label
        val status = when {
            bindFailed -> "bind_failed"
            bindingInProgress -> "binding"
            fallbackBinding -> "fallback"
            activePhysicalCameraId != null -> "physical"
            boundPhysicalCameraId != null -> "physical_req"
            else -> "logical"
        }
        return listOf(
            "lens $lensLabel  mode $status  crop ${formatColorCardCameraDebugFloat(effectiveCropCoverage)}",
            "req L:${requestedCameraId ?: "-"} P:${requestedPhysicalCameraId ?: "-"}  ${formatColorCardCameraDebugFloat(requestedZoomRatio)}x",
            "cap P:${requestedCapturePhysicalCameraId ?: "-"}  res pv:${formatColorCardCameraSize(previewResolution)} cap:${formatColorCardCameraSize(captureResolution)}",
            "sel L:${boundCameraId ?: "-"} P:${boundPhysicalCameraId ?: "-"}  ${formatColorCardCameraDebugFloat(appliedZoomRatio)}x/${formatColorCardCameraDebugFloat(maxZoomRatio)}x",
            "act P:${activePhysicalCameraId ?: "-"}",
            "stab req V:${formatColorCardCameraStabilizationMode(requestedVideoStabilizationMode, false)} O:${formatColorCardCameraStabilizationMode(requestedOpticalStabilizationMode, true)}",
            "stab act V:${formatColorCardCameraStabilizationMode(activeVideoStabilizationMode, false)} O:${formatColorCardCameraStabilizationMode(activeOpticalStabilizationMode, true)}",
            "stab sup V:${formatColorCardCameraSupportedModes(supportedVideoStabilizationModes, false)} O:${formatColorCardCameraSupportedModes(supportedOpticalStabilizationModes, true)}"
        )
    }
}

private fun resolveColorCardCameraStabilizationConfig(
    context: Context,
    cameraId: String?,
    physicalCameraId: String?,
    frontCamera: Boolean
): ColorCardCameraStabilizationConfig {
    if (frontCamera) {
        return ColorCardCameraStabilizationConfig()
    }
    val cameraManager = context.getSystemService(CameraManager::class.java) ?: return ColorCardCameraStabilizationConfig()
    val characteristics = runCatching {
        when {
            !physicalCameraId.isNullOrEmpty() -> cameraManager.getCameraCharacteristics(physicalCameraId)
            !cameraId.isNullOrEmpty() -> cameraManager.getCameraCharacteristics(cameraId)
            else -> null
        }
    }.getOrNull() ?: return ColorCardCameraStabilizationConfig()
    val supportedVideoModes = characteristics
        .get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
        ?.toList()
        .orEmpty()
    val supportedOpticalModes = characteristics
        .get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        ?.toList()
        .orEmpty()
    val videoMode: Int? = null
    val opticalMode = when {
        supportedOpticalModes.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) ->
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
        else -> null
    }
    return ColorCardCameraStabilizationConfig(
        videoMode = videoMode,
        opticalMode = opticalMode,
        supportedVideoModes = supportedVideoModes,
        supportedOpticalModes = supportedOpticalModes
    )
}

private fun formatColorCardCameraSupportedModes(
    modes: List<Int>,
    optical: Boolean
): String {
    if (modes.isEmpty()) return "-"
    return modes.joinToString("/") { mode ->
        formatColorCardCameraStabilizationMode(mode, optical)
    }
}

private fun formatColorCardCameraStabilizationMode(
    mode: Int?,
    optical: Boolean
): String {
    if (mode == null) return "-"
    return if (optical) {
        when (mode) {
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF -> "off"
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON -> "on"
            else -> mode.toString()
        }
    } else {
        when (mode) {
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF -> "off"
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON -> "on"
            else -> mode.toString()
        }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun applyColorCardCameraInterop(
    builder: Preview.Builder,
    physicalCameraId: String?,
    stabilizationConfig: ColorCardCameraStabilizationConfig,
    captureCallback: CameraCaptureSession.CaptureCallback
) {
    Camera2Interop.Extender(builder).apply {
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON
        )
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            CaptureRequest.CONTROL_AWB_MODE_AUTO
        )
        stabilizationConfig.videoMode?.let { mode ->
            setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, mode)
        }
        stabilizationConfig.opticalMode?.let { mode ->
            setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, mode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setCaptureRequestOption(
                CaptureRequest.SCALER_ROTATE_AND_CROP,
                CaptureRequest.SCALER_ROTATE_AND_CROP_NONE
            )
        }
        setSessionCaptureCallback(captureCallback)
        physicalCameraId?.let { setPhysicalCameraId(it) }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun applyColorCardCameraInterop(
    builder: ImageCapture.Builder,
    physicalCameraId: String?,
    stabilizationConfig: ColorCardCameraStabilizationConfig,
    captureCallback: CameraCaptureSession.CaptureCallback
) {
    Camera2Interop.Extender(builder).apply {
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            CaptureRequest.CONTROL_AWB_MODE_AUTO
        )
        stabilizationConfig.videoMode?.let { mode ->
            setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, mode)
        }
        stabilizationConfig.opticalMode?.let { mode ->
            setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, mode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setCaptureRequestOption(
                CaptureRequest.SCALER_ROTATE_AND_CROP,
                CaptureRequest.SCALER_ROTATE_AND_CROP_NONE
            )
        }
        setSessionCaptureCallback(captureCallback)
        physicalCameraId?.let { setPhysicalCameraId(it) }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun applyColorCardCameraInterop(
    builder: ImageAnalysis.Builder,
    physicalCameraId: String?
) {
    Camera2Interop.Extender(builder).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setCaptureRequestOption(
                CaptureRequest.SCALER_ROTATE_AND_CROP,
                CaptureRequest.SCALER_ROTATE_AND_CROP_NONE
            )
        }
        physicalCameraId?.let { setPhysicalCameraId(it) }
    }
}

private fun resolveColorCardCameraActivePhysicalId(
    result: TotalCaptureResult,
    requestedPhysicalCameraId: String?
): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val physicalResults = result.physicalCameraResults
        requestedPhysicalCameraId?.takeIf { physicalResults.containsKey(it) }?.let { return it }
        physicalResults.keys.firstOrNull()?.let { return it }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)?.let { return it }
    }
    return null
}

private fun resolveColorCardCameraLensSelection(
    context: android.content.Context,
    provider: ProcessCameraProvider?,
    lensMode: ColorCardCameraLensMode,
    useFrontCamera: Boolean,
    eqZoomTargetFocalMm: Int? = null
): ColorCardCameraLensSelection {
    if (useFrontCamera) {
        return ColorCardCameraLensSelection(
            cameraId = null,
            physicalCameraId = null,
            capturePhysicalCameraId = null,
            nativeFocal35mmEq = COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM,
            selector = CameraSelector.DEFAULT_FRONT_CAMERA,
            zoomRatio = 1f,
            cropCoverage = 1f,
            enhancementCoverage = 1f,
            outputAspectRatio = 3f / 4f,
            requestedFocal35mmEq = null,
            availableModes = listOf(ColorCardCameraLensMode.Native),
            displayLabels = mapOf(ColorCardCameraLensMode.Native to "1x")
        )
    }
    val candidates = provider?.let {
        loadColorCardCameraRearLensCandidates(context, it)
    }.orEmpty()
    if (candidates.isEmpty()) {
        val fallbackModes = listOf(
            ColorCardCameraLensMode.Native,
            ColorCardCameraLensMode.Eq35,
            ColorCardCameraLensMode.Eq50
        )
        val fallbackTargetFocal = when (lensMode) {
            ColorCardCameraLensMode.Eq35,
            ColorCardCameraLensMode.Eq50 -> {
                val requested = eqZoomTargetFocalMm?.toFloat() ?: lensMode.targetFocalMm ?: COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM
                requested.coerceIn(
                    COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM,
                    COLOR_CARD_CAMERA_EQ_ZOOM_MAX_FOCAL_MM.toFloat()
                )
            }
            else -> lensMode.targetFocalMm ?: COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM
        }
        return ColorCardCameraLensSelection(
            cameraId = null,
            physicalCameraId = null,
            capturePhysicalCameraId = null,
            nativeFocal35mmEq = COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM,
            selector = CameraSelector.DEFAULT_BACK_CAMERA,
            zoomRatio = 1f,
            cropCoverage = colorCardCameraTargetCoverage(
                actualFocal35mmEq = COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM,
                targetFocal35mmEq = fallbackTargetFocal
            ),
            enhancementCoverage = colorCardCameraTargetCoverage(
                actualFocal35mmEq = COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM,
                targetFocal35mmEq = fallbackTargetFocal
            ),
            outputAspectRatio = 3f / 4f,
            requestedFocal35mmEq = fallbackTargetFocal,
            availableModes = fallbackModes,
            displayLabels = buildColorCardCameraFallbackLabels(fallbackModes)
        )
    }
    val nativeCandidate = selectColorCardCameraNativeCandidate(candidates) ?: candidates.first()
    val modeSpecs = buildColorCardCameraLensModeSpecs(candidates, nativeCandidate)
    val labels = modeSpecs.associate { spec ->
        spec.mode to formatColorCardCameraFocalLabel(spec.displayFocal35mmEq)
    }
    val availableModes = modeSpecs.map(ColorCardCameraLensModeSpec::mode)
    val targetFocalByMode = modeSpecs.associate { spec ->
        spec.mode to (spec.targetFocal35mmEq ?: spec.displayFocal35mmEq)
    }
    val directNativeSelector = buildColorCardCameraSelectorById(
        bindCameraId = nativeCandidate.bindCameraId,
        physicalCameraId = nativeCandidate.physicalCameraId
    )
    val logicalSelector = buildColorCardCameraSelectorById(
        bindCameraId = nativeCandidate.bindCameraId,
        physicalCameraId = null
    )
    val preferredSelector = if (nativeCandidate.logicalMultiCamera) {
        logicalSelector
    } else {
        directNativeSelector
    }
    val preferredPhysicalCameraId = if (nativeCandidate.logicalMultiCamera) {
        null
    } else {
        nativeCandidate.physicalCameraId
    }
    val eqZoomMinFocal = resolveColorCardCameraEqZoomMinFocalMm(nativeCandidate.focal35mmEq).toFloat()
    val eqZoomMaxFocal = max(eqZoomMinFocal, COLOR_CARD_CAMERA_EQ_ZOOM_MAX_FOCAL_MM.toFloat())
    if (lensMode == ColorCardCameraLensMode.Native) {
        return ColorCardCameraLensSelection(
            cameraId = nativeCandidate.bindCameraId,
            physicalCameraId = preferredPhysicalCameraId,
            capturePhysicalCameraId = preferredPhysicalCameraId,
            nativeFocal35mmEq = nativeCandidate.focal35mmEq,
            selector = preferredSelector,
            zoomRatio = 1f,
            cropCoverage = 1f,
            enhancementCoverage = 1f,
            outputAspectRatio = nativeCandidate.outputAspectRatio,
            requestedFocal35mmEq = nativeCandidate.focal35mmEq,
            availableModes = availableModes,
            displayLabels = labels
        )
    }
    val target = when (lensMode) {
        ColorCardCameraLensMode.Eq35,
        ColorCardCameraLensMode.Eq50 -> {
            val defaultEqTarget = targetFocalByMode[lensMode]
                ?: lensMode.targetFocalMm
                ?: eqZoomMinFocal
            (eqZoomTargetFocalMm?.toFloat() ?: defaultEqTarget).coerceIn(eqZoomMinFocal, eqZoomMaxFocal)
        }
        else -> targetFocalByMode[lensMode]
    } ?: return ColorCardCameraLensSelection(
        cameraId = nativeCandidate.bindCameraId,
        physicalCameraId = preferredPhysicalCameraId,
        capturePhysicalCameraId = preferredPhysicalCameraId,
        nativeFocal35mmEq = nativeCandidate.focal35mmEq,
        selector = preferredSelector,
        zoomRatio = 1f,
        cropCoverage = 1f,
        enhancementCoverage = 1f,
        outputAspectRatio = nativeCandidate.outputAspectRatio,
        requestedFocal35mmEq = nativeCandidate.focal35mmEq,
        availableModes = availableModes,
        displayLabels = labels
    )
    val usesDerivedPreviewFraming = lensMode == ColorCardCameraLensMode.Eq35 ||
        lensMode == ColorCardCameraLensMode.Eq50
    val directOpticalCandidate = selectColorCardCameraDirectCandidate(
        targetFocalMm = target,
        candidates = candidates,
        nativeCandidate = nativeCandidate
    )
    if (!usesDerivedPreviewFraming && directOpticalCandidate != null) {
        val directZoomRatio = if (directOpticalCandidate.focal35mmEq >= target * 0.96f) {
            1f
        } else {
            (target / directOpticalCandidate.focal35mmEq).coerceAtLeast(1f)
        }
        val previewPhysicalCameraId = directOpticalCandidate.physicalCameraId
        val capturePhysicalCameraId = resolveColorCardCameraPreferredStillCapturePhysicalId(
            context = context,
            cameraId = directOpticalCandidate.bindCameraId,
            previewPhysicalCameraId = previewPhysicalCameraId
        )
        val selector = if (previewPhysicalCameraId != capturePhysicalCameraId) {
            buildColorCardCameraSelectorById(
                bindCameraId = directOpticalCandidate.bindCameraId,
                physicalCameraId = null
            )
        } else {
            buildColorCardCameraSelectorById(
                bindCameraId = directOpticalCandidate.bindCameraId,
                physicalCameraId = previewPhysicalCameraId
            )
        }
        return ColorCardCameraLensSelection(
            cameraId = directOpticalCandidate.bindCameraId,
            physicalCameraId = previewPhysicalCameraId,
            capturePhysicalCameraId = capturePhysicalCameraId,
            nativeFocal35mmEq = nativeCandidate.focal35mmEq,
            selector = selector,
            zoomRatio = directZoomRatio,
            cropCoverage = 1f,
            enhancementCoverage = colorCardCameraTargetCoverage(
                actualFocal35mmEq = directOpticalCandidate.focal35mmEq,
                targetFocal35mmEq = target
            ),
            outputAspectRatio = directOpticalCandidate.outputAspectRatio,
            requestedFocal35mmEq = target,
            availableModes = availableModes,
            displayLabels = labels
        )
    }
    val targetCoverage = colorCardCameraTargetCoverage(
        actualFocal35mmEq = nativeCandidate.focal35mmEq,
        targetFocal35mmEq = target
    )
    // Keep derived 35/50 modes on the same preview/capture session as 1x.
    // Re-pointing ImageCapture to another physical camera forces CameraX to
    // rebuild the whole session, which shows up as an audible hardware restart
    // when users are only asking for a crop-based focal preset.
    return ColorCardCameraLensSelection(
        cameraId = nativeCandidate.bindCameraId,
        physicalCameraId = preferredPhysicalCameraId,
        capturePhysicalCameraId = preferredPhysicalCameraId,
        nativeFocal35mmEq = nativeCandidate.focal35mmEq,
        selector = preferredSelector,
        zoomRatio = 1f,
        cropCoverage = targetCoverage,
        enhancementCoverage = targetCoverage,
        outputAspectRatio = nativeCandidate.outputAspectRatio,
        requestedFocal35mmEq = target,
        availableModes = availableModes,
        displayLabels = labels
    )
}

private fun buildColorCardCameraFallbackLabels(
    modes: List<ColorCardCameraLensMode>
): Map<ColorCardCameraLensMode, String> = modes.associateWith {
    when (it) {
        ColorCardCameraLensMode.Native -> "1x"
        ColorCardCameraLensMode.Eq35 -> "35"
        ColorCardCameraLensMode.Eq50 -> "50"
        ColorCardCameraLensMode.Tele1,
        ColorCardCameraLensMode.Tele2 -> it.label
    }
}

private fun buildColorCardCameraLensModeSpecs(
    candidates: List<ColorCardCameraLensCandidate>,
    nativeCandidate: ColorCardCameraLensCandidate
): List<ColorCardCameraLensModeSpec> {
    val opticalCandidates = candidates
        .asSequence()
        .filter { it.isDedicatedOpticalPath }
        .filter { it.selectionKey != nativeCandidate.selectionKey }
        .sortedBy { it.focal35mmEq }
        .toList()
    val teleCandidates = opticalCandidates.filter { it.focal35mmEq > 55f }
    val teleAssignments = when {
        opticalCandidates.size >= 3 && teleCandidates.size >= 2 -> listOf(
            ColorCardCameraLensMode.Tele1 to teleCandidates[0],
            ColorCardCameraLensMode.Tele2 to teleCandidates[1]
        )
        opticalCandidates.size >= 2 && teleCandidates.isNotEmpty() -> listOf(
            ColorCardCameraLensMode.Tele1 to teleCandidates.last()
        )
        else -> emptyList()
    }
    val rawSpecs = buildList {
        add(
            ColorCardCameraLensModeSpec(
                mode = ColorCardCameraLensMode.Native,
                displayFocal35mmEq = nativeCandidate.focal35mmEq,
                targetFocal35mmEq = nativeCandidate.focal35mmEq,
                priority = 0
            )
        )
        add(
            ColorCardCameraLensModeSpec(
                mode = ColorCardCameraLensMode.Eq35,
                displayFocal35mmEq = 35f,
                targetFocal35mmEq = 35f,
                priority = 1
            )
        )
        add(
            ColorCardCameraLensModeSpec(
                mode = ColorCardCameraLensMode.Eq50,
                displayFocal35mmEq = 50f,
                targetFocal35mmEq = 50f,
                priority = 1
            )
        )
        teleAssignments.forEach { (mode, candidate) ->
            add(
                ColorCardCameraLensModeSpec(
                    mode = mode,
                    displayFocal35mmEq = candidate.focal35mmEq,
                    targetFocal35mmEq = candidate.focal35mmEq,
                    priority = 0
                )
            )
        }
    }
    return dedupeColorCardCameraLensModeSpecs(rawSpecs)
}

private fun dedupeColorCardCameraLensModeSpecs(
    specs: List<ColorCardCameraLensModeSpec>
): List<ColorCardCameraLensModeSpec> {
    val deduped = mutableListOf<ColorCardCameraLensModeSpec>()
    specs.forEach { candidate ->
        val duplicateIndex = deduped.indexOfFirst {
            abs(it.displayFocal35mmEq - candidate.displayFocal35mmEq) <= 5f
        }
        if (duplicateIndex < 0) {
            deduped += candidate
        } else {
            val existing = deduped[duplicateIndex]
            val preferred = compareColorCardCameraLensModeSpecs(candidate, existing)
            deduped[duplicateIndex] = preferred
        }
    }
    return deduped.sortedBy {
        when (it.mode) {
            ColorCardCameraLensMode.Native -> 0
            ColorCardCameraLensMode.Eq35 -> 1
            ColorCardCameraLensMode.Eq50 -> 2
            ColorCardCameraLensMode.Tele1 -> 3
            ColorCardCameraLensMode.Tele2 -> 4
        }
    }
}

private fun compareColorCardCameraLensModeSpecs(
    left: ColorCardCameraLensModeSpec,
    right: ColorCardCameraLensModeSpec
): ColorCardCameraLensModeSpec {
    if (left.priority != right.priority) {
        return if (left.priority < right.priority) left else right
    }
    val leftTargetDelta = abs((left.targetFocal35mmEq ?: left.displayFocal35mmEq) - left.displayFocal35mmEq)
    val rightTargetDelta = abs((right.targetFocal35mmEq ?: right.displayFocal35mmEq) - right.displayFocal35mmEq)
    if (leftTargetDelta != rightTargetDelta) {
        return if (leftTargetDelta < rightTargetDelta) left else right
    }
    return if (left.displayFocal35mmEq <= right.displayFocal35mmEq) left else right
}

private fun colorCardCameraTargetCoverage(
    actualFocal35mmEq: Float,
    targetFocal35mmEq: Float
): Float {
    if (targetFocal35mmEq <= 0f) return 1f
    return (actualFocal35mmEq / targetFocal35mmEq).coerceIn(0.2f, 1f)
}

private fun resolveColorCardCameraEqZoomMinFocalMm(
    nativeFocal35mmEq: Float
): Int {
    val resolvedNative = when {
        nativeFocal35mmEq.isFinite() && nativeFocal35mmEq > 0f -> nativeFocal35mmEq
        else -> COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM
    }
    return resolvedNative
        .roundToInt()
        .coerceAtLeast(1)
        .coerceAtMost(COLOR_CARD_CAMERA_EQ_ZOOM_MAX_FOCAL_MM)
}

private fun formatColorCardCameraFocalLabel(value: Float): String {
    val safeValue = value.coerceAtLeast(0f)
    val displayValue = when {
        safeValue >= 30f -> (safeValue / 5f).roundToInt() * 5f
        safeValue >= 10f -> safeValue.roundToInt().toFloat()
        else -> kotlin.math.round(safeValue * 10f) / 10f
    }
    val wholeValue = displayValue.roundToInt()
    return if (abs(displayValue - wholeValue) < 0.05f) {
        wholeValue.toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", displayValue)
    }
}

private fun formatColorCardCameraDebugFloat(value: Float): String {
    val rounded = kotlin.math.round(value * 10f) / 10f
    return if (abs(rounded - rounded.toInt()) < 0.05f) {
        rounded.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", rounded)
    }
}

private fun loadColorCardCameraRearLensCandidates(
    context: android.content.Context,
    provider: ProcessCameraProvider
): List<ColorCardCameraLensCandidate> {
    val cameraManager = context.getSystemService(CameraManager::class.java) ?: return emptyList()
    return provider.availableCameraInfos.flatMap { info ->
        val bindCameraId = runCatching { Camera2CameraInfo.from(info).cameraId }.getOrNull()
            ?: return@flatMap emptyList()
        val characteristics = runCatching { cameraManager.getCameraCharacteristics(bindCameraId) }.getOrNull()
            ?: return@flatMap emptyList()
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
        if (facing != CameraCharacteristics.LENS_FACING_BACK) return@flatMap emptyList()
        if (!isColorCardCameraPublicStillCamera(characteristics)) return@flatMap emptyList()
        buildColorCardCameraLensCandidates(
            bindCameraId = bindCameraId,
            characteristics = characteristics,
            cameraManager = cameraManager
        )
    }.distinctBy { it.selectionKey }.sortedBy { it.focal35mmEq }
}

private data class ColorCardCameraProbeEntry(
    val title: String,
    val lines: List<String>
)

private fun loadColorCardCameraProbeEntries(
    context: Context,
    provider: ProcessCameraProvider?
): List<ColorCardCameraProbeEntry> {
    val cameraManager = context.getSystemService(CameraManager::class.java) ?: return emptyList()
    val bindableCameraIds = provider
        ?.availableCameraInfos
        ?.mapNotNull { info ->
            runCatching { Camera2CameraInfo.from(info).cameraId }.getOrNull()
        }
        ?.toSet()
        .orEmpty()
    return cameraManager.cameraIdList
        .sorted()
        .flatMap { cameraId ->
            val characteristics = runCatching {
                cameraManager.getCameraCharacteristics(cameraId)
            }.getOrNull() ?: return@flatMap emptyList()
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing != CameraCharacteristics.LENS_FACING_BACK) return@flatMap emptyList()
            val cameraEntries = mutableListOf<ColorCardCameraProbeEntry>()
            val logicalPhysicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                characteristics.physicalCameraIds.sorted()
            } else {
                emptyList()
            }
            cameraEntries += ColorCardCameraProbeEntry(
                title = "logical $cameraId${if (cameraId in bindableCameraIds) "  bindable" else "  not_bindable"}",
                lines = buildList {
                    add("35mm eq ${formatColorCardCameraDebugFloat(estimateColorCardCamera35mmEquivalent(characteristics) ?: 0f)}")
                    add("focal ${formatColorCardCameraFocalLengths(characteristics)}")
                    add(
                        "type " + if (logicalPhysicalIds.isNotEmpty()) {
                            "logical_multi (${logicalPhysicalIds.joinToString()})"
                        } else {
                            "single"
                        }
                    )
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                logicalPhysicalIds.forEach { physicalCameraId ->
                    val physicalCharacteristics = runCatching {
                        cameraManager.getCameraCharacteristics(physicalCameraId)
                    }.getOrNull() ?: return@forEach
                    cameraEntries += ColorCardCameraProbeEntry(
                        title = "physical $physicalCameraId of $cameraId",
                        lines = buildList {
                            add("35mm eq ${formatColorCardCameraDebugFloat(estimateColorCardCamera35mmEquivalent(physicalCharacteristics) ?: 0f)}")
                            add("focal ${formatColorCardCameraFocalLengths(physicalCharacteristics)}")
                            add(
                                "public_still " +
                                    if (isColorCardCameraPublicStillCamera(physicalCharacteristics)) "yes" else "no"
                            )
                        }
                    )
                }
            }
            cameraEntries
        }
}

private fun formatColorCardCameraFocalLengths(
    characteristics: CameraCharacteristics
): String {
    return characteristics
        .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        ?.joinToString { focal -> String.format(java.util.Locale.US, "%.2fmm", focal) }
        ?: "-"
}

private fun buildColorCardCameraSelectorById(
    bindCameraId: String,
    physicalCameraId: String? = null
): CameraSelector {
    return CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .addCameraFilter { cameraInfos ->
            cameraInfos.filter { info ->
                runCatching { Camera2CameraInfo.from(info).cameraId }.getOrNull() == bindCameraId
            }
        }
        .apply {
            physicalCameraId?.let { setPhysicalCameraId(it) }
        }
        .build()
}

private fun buildColorCardCameraLensCandidates(
    bindCameraId: String,
    characteristics: CameraCharacteristics,
    cameraManager: CameraManager
): List<ColorCardCameraLensCandidate> {
    val logicalMultiCamera = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        characteristics.physicalCameraIds.isNotEmpty()
    val candidates = mutableListOf<ColorCardCameraLensCandidate>()
    estimateColorCardCamera35mmEquivalent(characteristics)?.let { focal35mmEq ->
        candidates += ColorCardCameraLensCandidate(
            bindCameraId = bindCameraId,
            physicalCameraId = null,
            focal35mmEq = focal35mmEq,
            logicalMultiCamera = logicalMultiCamera,
            outputAspectRatio = resolveColorCardCameraSensorAspectRatio(characteristics)
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        characteristics.physicalCameraIds.forEach { physicalCameraId ->
            val physicalCharacteristics = runCatching {
                cameraManager.getCameraCharacteristics(physicalCameraId)
            }.getOrNull() ?: return@forEach
            if (!isColorCardCameraPublicStillCamera(physicalCharacteristics)) return@forEach
            estimateColorCardCamera35mmEquivalent(physicalCharacteristics)?.let { focal35mmEq ->
                candidates += ColorCardCameraLensCandidate(
                    bindCameraId = bindCameraId,
                    physicalCameraId = physicalCameraId,
                    focal35mmEq = focal35mmEq,
                    logicalMultiCamera = true,
                    outputAspectRatio = resolveColorCardCameraSensorAspectRatio(physicalCharacteristics)
                )
            }
        }
    }
    return candidates
}

private fun buildColorCardCameraTargetSizeForAspect(
    shortSide: Int,
    outputAspectRatio: Float
): Size {
    val safeAspectRatio = normalizeColorCardCameraPortraitAspectRatio(outputAspectRatio)
    val width = shortSide.coerceAtLeast(1)
    val height = (width / safeAspectRatio).roundToInt().coerceAtLeast(1)
    return Size(width, height)
}

private fun normalizeColorCardCameraPortraitAspectRatio(
    aspectRatio: Float
): Float {
    val safeAspectRatio = aspectRatio.coerceAtLeast(0.0001f)
    return if (safeAspectRatio > 1f) {
        1f / safeAspectRatio
    } else {
        safeAspectRatio
    }.coerceIn(0.3f, 1.5f)
}

private fun buildColorCardCameraViewPort(
    outputAspectRatio: Float,
    targetRotation: Int
): ViewPort {
    val safeAspectRatio = normalizeColorCardCameraPortraitAspectRatio(outputAspectRatio)
    val width = 10_000
    val height = (width / safeAspectRatio).roundToInt().coerceAtLeast(1)
    return ViewPort.Builder(Rational(width, height), targetRotation)
        .setScaleType(ViewPort.FIT)
        .build()
}

private fun isColorCardCameraPhysicalSubLens(
    cameraId: String?,
    physicalCameraId: String?
): Boolean {
    return !cameraId.isNullOrEmpty() &&
        !physicalCameraId.isNullOrEmpty() &&
        cameraId != physicalCameraId
}

private fun resolveColorCardCameraSensorAspectRatio(
    characteristics: CameraCharacteristics
): Float {
    val activeArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        characteristics.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)
    } else {
        null
    } ?: characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    if (activeArray != null && activeArray.width() > 0 && activeArray.height() > 0) {
        return normalizeColorCardCameraPortraitAspectRatio(
            activeArray.width().toFloat() / activeArray.height().toFloat()
        )
    }
    val pixelArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    if (pixelArray != null && pixelArray.width > 0 && pixelArray.height > 0) {
        return normalizeColorCardCameraPortraitAspectRatio(
            pixelArray.width.toFloat() / pixelArray.height.toFloat()
        )
    }
    return 3f / 4f
}

private fun estimateColorCardCamera35mmEquivalent(
    characteristics: CameraCharacteristics
): Float? {
    val focalLength = characteristics
        .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        ?.minOrNull()
        ?: return null
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        ?: return null
    val sensorDiagonal = hypot(sensorSize.width.toDouble(), sensorSize.height.toDouble()).toFloat()
    if (sensorDiagonal <= 0f) return null
    return focalLength * (43.2666f / sensorDiagonal)
}

private fun isColorCardCameraPublicStillCamera(
    characteristics: CameraCharacteristics
): Boolean {
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        ?: return true
    return capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
}

private fun selectColorCardCameraNativeCandidate(
    candidates: List<ColorCardCameraLensCandidate>
): ColorCardCameraLensCandidate? {
    val directCandidates = candidates.filter { it.isDedicatedOpticalPath }
    return directCandidates
        .ifEmpty { candidates }
        .minByOrNull { abs(it.focal35mmEq - COLOR_CARD_CAMERA_DEFAULT_NATIVE_FOCAL_MM) }
}

private fun selectColorCardCameraDirectCandidate(
    targetFocalMm: Float,
    candidates: List<ColorCardCameraLensCandidate>,
    nativeCandidate: ColorCardCameraLensCandidate
): ColorCardCameraLensCandidate? {
    val minimumOpticalFocal = max(nativeCandidate.focal35mmEq * 1.2f, targetFocalMm * 0.5f)
    val nativeRatioError = colorCardCameraFocalRatioError(
        actualFocal35mmEq = nativeCandidate.focal35mmEq,
        targetFocal35mmEq = targetFocalMm
    )
    return candidates
        .asSequence()
        .filter { it.isDedicatedOpticalPath }
        .filter { it.selectionKey != nativeCandidate.selectionKey }
        .filter { it.focal35mmEq >= minimumOpticalFocal }
        .filter { candidate ->
            val candidateRatioError = colorCardCameraFocalRatioError(
                actualFocal35mmEq = candidate.focal35mmEq,
                targetFocal35mmEq = targetFocalMm
            )
            candidateRatioError <= 1.35f &&
                candidateRatioError <= nativeRatioError * 0.8f
        }
        .sortedWith(
            compareBy<ColorCardCameraLensCandidate> {
                if (it.focal35mmEq <= targetFocalMm) {
                    ((targetFocalMm / it.focal35mmEq) - 1f) * 10f
                } else {
                    (it.focal35mmEq - targetFocalMm) * 0.55f
                }
            }.thenBy { abs(it.focal35mmEq - targetFocalMm) }
        )
        .firstOrNull()
}

private fun colorCardCameraFocalRatioError(
    actualFocal35mmEq: Float,
    targetFocal35mmEq: Float
): Float {
    val safeActual = actualFocal35mmEq.coerceAtLeast(0.0001f)
    val safeTarget = targetFocal35mmEq.coerceAtLeast(0.0001f)
    return max(safeActual, safeTarget) / min(safeActual, safeTarget)
}

private tailrec fun Context.findColorCardCameraActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findColorCardCameraActivity()
    else -> null
}

private fun layoutBoundsInRootUnclipped(
    coordinates: LayoutCoordinates
): androidx.compose.ui.geometry.Rect {
    val topLeft = coordinates.positionInRoot()
    val size = coordinates.size
    return androidx.compose.ui.geometry.Rect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height
    )
}

internal fun buildColorCardCameraPreviewMediaItem(uriString: String): MediaItem {
    val uri = Uri.parse(uriString)
    val displayName = uri.lastPathSegment.orEmpty()
    return MediaItem(
        id = uriString.hashCode().toLong(),
        uri = uri,
        type = "photo",
        dateAdded = System.currentTimeMillis() / 1000L,
        size = 0L,
        width = 0,
        height = 0,
        mimeType = "image/jpeg",
        displayName = displayName,
        path = if (uri.scheme == "file") uri.path else null
    )
}
