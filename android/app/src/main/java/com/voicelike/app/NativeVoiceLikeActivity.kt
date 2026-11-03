package com.voicelike.app

import android.view.Display
import android.view.WindowManager
import androidx.core.view.WindowCompat
import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.VideoSize
import androidx.media3.common.MediaItem as ExoMediaItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kyant.backdrop.backdrops.layerBackdrop
import com.voicelike.app.liquidglass.LocalLiquidGlassBackdrop
import com.voicelike.app.liquidglass.liquidGlassControl
import com.voicelike.app.liquidglass.liquidGlassSurfaceColor
import com.voicelike.app.liquidglass.rememberLiquidGlassBackdrop
import com.voicelike.app.liquidglass.LocalLiquidGlassEnabled
import com.voicelike.app.liquidglass.isLiquidGlassSupported
import com.voicelike.app.smart.SmartAiRoastProvider
import com.voicelike.app.smart.SmartAiTypingOverlay
import com.voicelike.app.stamp.StampCalendarPage
import com.voicelike.app.startup.StartupGreetingContext
import com.voicelike.app.startup.StartupWelcomeTab
import com.voicelike.app.startup.StartupWelcomeCard
import com.voicelike.app.startup.StartupOrganizeScope
import com.voicelike.app.startup.StartupOrganizeHome
import com.voicelike.app.startup.StartupWelcomeCopy
import com.voicelike.app.startup.buildStartupWelcomeCopy
import com.voicelike.app.widgetlikes.LikesPhotoWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.Locale
import java.io.File
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.voicelike.app.helpcenter.HelpCenterPage
import com.voicelike.app.keep.KeepGalleryStore
import com.voicelike.app.keep.KeepGalleryView
import com.voicelike.app.keep.buildKeepIdSnapshot
import com.voicelike.app.keep.resolveMediaItemsByIds
import com.voicelike.app.promptmanager.PromptManagerPage

// --- Data Models ---
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: String, // "video" or "photo"
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val displayName: String = "",
    val resolution: String = "",
    val camera: String = "--",
    val iso: String = "--",
    val shutter: String = "--",
    val focalLength: String = "--",
    val aperture: String = "--",
    val location: String = "Unknown Location",
    val isLivePhoto: Boolean = false,
    val livePhotoVideoUri: Uri? = null,
    val livePhotoVideoId: Long? = null,
    val path: String?, // Added path to support undo move
    val bucketName: String = "", // Added folder name
    val duration: Long = 0L, // Duration in ms
    val isMotionPhoto: Boolean = false,
    val motionPhotoVideoOffset: Long = 0L,
    val motionPhotoVideoLength: Long = 0L
)

data class CompressionResumeState(
    val queueIds: List<Long> = emptyList(),
    val doneIds: List<Long> = emptyList()
)

// --- Persistence Helper ---
class PreferencesManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("photoo_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_TAG = "PreferencesManager"
        private const val MAX_APP_TRASH_INFO_ENTRIES = 24000
        private const val MAX_LOW_QUALITY_RESULT_ENTRIES = 20000
        private const val MAX_SCANNED_LOW_QUALITY_IDS_ENTRIES = 80000
        private const val OOM_FALLBACK_PROCESSED_IDS = 160000
        private const val OOM_FALLBACK_PENDING_TRASH_IDS = 80000
        private const val OOM_FALLBACK_LIKED_IDS = 120000
        private const val OOM_FALLBACK_MOVED_IDS = 120000
        private const val OOM_FALLBACK_FOLDER_USAGE_ENTRIES = 8000
        private const val OOM_FALLBACK_FOLDER_LAST_USED_ENTRIES = 8000
        private const val OOM_FALLBACK_LIKES_FOLDER_SELECTION_ENTRIES = 4000
    }

    // In-memory cache to avoid expensive JSON parsing on every access
    private var _stats: AppStats? = null
    private var _processedIds: MutableSet<Long>? = null
    private var _pendingTrashIds: MutableSet<Long>? = null
    private var _appTrashInfo: MutableMap<Long, Long>? = null
    private var _likedIds: MutableSet<Long>? = null
    private var _comments: MutableMap<Long, List<String>>? = null // Comments cache
    private var _language: AppLanguage? = null
    private var _muteVideos: Boolean? = null
    private var _hapticsEnabled: Boolean? = null
    private var _autoCheckUpdatesEnabled: Boolean? = null
    private var _themeMode: AppThemeMode? = null
    private var _danmakuEnabled: Boolean? = null
    private var _videoCompressBitrateKey: String? = null
    private var _imageCompressThresholdMb: Int? = null
    private var _imageCompressQualityKey: String? = null
    private var _imageCompressResume: CompressionResumeState? = null
    private var _videoCompressResume: CompressionResumeState? = null
    private var _onboardingCompleted: Boolean? = null
    private var _swipeLeftAction: String? = null
    private var _swipeRightAction: String? = null
    private var _swipeUpAction: String? = null
    private var _swipeHorizontalSpeedTenths: Int? = null
    private var _swipeUpSpeedTenths: Int? = null

    private fun <K, V> boundedMapSnapshot(source: Map<K, V>, maxEntries: Int): Map<K, V> {
        if (source.isEmpty()) return emptyMap()
        if (maxEntries <= 0) return emptyMap()
        if (source.size <= maxEntries) return HashMap(source)
        val bounded = LinkedHashMap<K, V>(maxEntries)
        var added = 0
        for ((key, value) in source) {
            bounded[key] = value
            added += 1
            if (added >= maxEntries) break
        }
        return bounded
    }

    private fun boundedLongSetSnapshot(source: Iterable<Long>, maxEntries: Int): Set<Long> {
        if (maxEntries <= 0) return emptySet()
        val bounded = LinkedHashSet<Long>(maxEntries)
        var added = 0
        for (id in source) {
            bounded.add(id)
            added += 1
            if (added >= maxEntries) break
        }
        return bounded
    }

    private fun snapshotLongSetForPersist(source: Set<Long>, maxEntries: Int): Set<Long> {
        if (source.isEmpty() || maxEntries <= 0) return emptySet()
        val resolvedMaxEntries = minOf(source.size, maxEntries)
        return boundedLongSetSnapshot(source, resolvedMaxEntries)
    }

    private fun <K, V> snapshotMapForPersist(source: Map<K, V>, maxEntries: Int): Map<K, V> {
        if (source.isEmpty() || maxEntries <= 0) return emptyMap()
        return boundedMapSnapshot(source, minOf(source.size, maxEntries))
    }

    private fun concurrentLongSet(source: Iterable<Long>): MutableSet<Long> =
        java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
            source.forEach { add(it) }
        }

    private fun safeToJson(key: String, payload: Any): String? {
        return try {
            gson.toJson(payload)
        } catch (oom: OutOfMemoryError) {
            Log.e(PREFS_TAG, "OOM while serializing key=$key", oom)
            MemorySafetyMode.markOomCrash(context)
            AppRuntimeLogStore.logCrashLowOverhead(context, "prefs_write:$key", oom)
            runCatching { System.gc() }
            null
        } catch (e: Exception) {
            Log.e(PREFS_TAG, "Failed to serialize key=$key", e)
            AppRuntimeLogStore.logError(context, PREFS_TAG, "Failed to serialize key=$key", e)
            null
        } catch (e: StackOverflowError) {
            // A payload with a reference cycle (e.g. a platform object) must not crash the app.
            Log.e(PREFS_TAG, "Cyclic payload while serializing key=$key", e)
            AppRuntimeLogStore.logError(context, PREFS_TAG, "Cyclic payload while serializing key=$key", e)
            null
        }
    }

    private fun putJsonSafely(key: String, payload: Any): Boolean {
        val json = safeToJson(key, payload) ?: return false
        prefs.edit().putString(key, json).apply()
        return true
    }

    private fun putJsonSafely(
        editor: android.content.SharedPreferences.Editor,
        key: String,
        payload: Any
    ): Boolean {
        val json = safeToJson(key, payload) ?: return false
        editor.putString(key, json)
        return true
    }

    private fun putJsonWithFallback(
        key: String,
        primaryPayload: Any,
        fallbackPayload: (() -> Any)?
    ): Boolean {
        if (putJsonSafely(key, primaryPayload)) return true
        val fallback = fallbackPayload?.invoke() ?: return false
        runCatching { System.gc() }
        return putJsonSafely(key, fallback)
    }

    private fun putJsonWithFallback(
        editor: android.content.SharedPreferences.Editor,
        key: String,
        primaryPayload: Any,
        fallbackPayload: (() -> Any)?
    ): Boolean {
        if (putJsonSafely(editor, key, primaryPayload)) return true
        val fallback = fallbackPayload?.invoke() ?: return false
        runCatching { System.gc() }
        return putJsonSafely(editor, key, fallback)
    }

    private fun handlePrefsReadFailure(key: String, error: Throwable) {
        if (error is OutOfMemoryError) {
            MemorySafetyMode.markOomCrash(context)
            AppRuntimeLogStore.logCrashLowOverhead(context, "prefs:$key", error)
            runCatching { System.gc() }
        } else if (error is Exception) {
            Log.e(PREFS_TAG, "Failed to deserialize key=$key", error)
            AppRuntimeLogStore.logError(context, PREFS_TAG, "Failed to deserialize key=$key", error)
        }
        runCatching { prefs.edit().remove(key).apply() }
    }

    private inline fun <T> readJsonOrDefault(
        key: String,
        defaultValue: () -> T,
        parser: (String) -> T
    ): T {
        val json = prefs.getString(key, null) ?: return defaultValue()
        return try {
            parser(json)
        } catch (oom: OutOfMemoryError) {
            handlePrefsReadFailure(key, oom)
            defaultValue()
        } catch (e: Exception) {
            handlePrefsReadFailure(key, e)
            defaultValue()
        }
    }

    var stats: AppStats
        get() {
            if (_stats == null) {
                _stats = readJsonOrDefault(
                    key = "stats",
                    defaultValue = { AppStats() }
                ) { json ->
                    gson.fromJson(json, AppStats::class.java) ?: AppStats()
                }
            }
            return _stats!!
        }
        set(value) {
            _stats = value
            putJsonSafely("stats", value)
        }

    fun reloadStats() {
        _stats = null
    }

    var processedIds: MutableSet<Long>
        get() {
            if (_processedIds == null) {
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _processedIds = readJsonOrDefault(
                    key = "processed_ids",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap.newKeySet() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet() 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣閻ｉ箖鎮峰鍐ч柕鍡楀€块幖褰掝敃閿涘嫮鐣炬俊鐐€栭崝锕傚磻閸曨垱鍊堕柨鏇炲€归悡娆愩亜閺冨洤浜圭紒澶屽劋椤ㄣ儵鎮欓幍顔剧崲濡ょ姷鍋炵敮锟犵嵁鐎ｎ亶鐔嗘い蹇撳€告慨鍌涙叏婵犲啯銇濈€规洦鍋婂畷鐔碱敋閸涱喛鏅ч梻鍌欒兌椤牓鏁冮妷褎宕查柛顐犲劤瀹撲線鏌涢幇鈺佸闁哄啫鐗嗗婵囥亜閺冨牊锛熼柣銏狀煼濮婂宕掑▎鎴ｇ獥闂佸憡鐟ョ换妯侯潖娴犲绀嬫い鎾跺У閸ｎ噣姊婚崒姘偓鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸愯弓鐢婚梻渚€娼ч敍蹇涘礋椤愩垻鍝庡┑鐘愁問閸犳濡靛☉銏犵；闁规儳鐏堥崑?Set
                    val loaded: HashSet<Long> = gson.fromJson(json, type) ?: HashSet()
                    concurrentLongSet(loaded)
                }
            }
            return _processedIds!!
        }
        set(value) {
            _processedIds = concurrentLongSet(value)
            val snapshot = snapshotLongSetForPersist(value, OOM_FALLBACK_PROCESSED_IDS)
            putJsonWithFallback("processed_ids", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_PROCESSED_IDS / 2).coerceAtLeast(1))
            }
        }

    var pendingTrashIds: MutableSet<Long>
        get() {
            if (_pendingTrashIds == null) {
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _pendingTrashIds = readJsonOrDefault(
                    key = "pending_trash",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap.newKeySet() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet() 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣閻ｉ箖鎮峰鍐ч柕鍡楀€块幖褰掝敃閿涘嫮鐣炬俊鐐€栭崝锕傚磻閸曨垱鍊堕柨鏇炲€归悡娆愩亜閺冨洤浜圭紒澶屽劋椤ㄣ儵鎮欓幍顔剧崲濡ょ姷鍋炵敮锟犵嵁鐎ｎ亶鐔嗘い蹇撳€告慨鍌涙叏婵犲啯銇濈€规洦鍋婂畷鐔碱敋閸涱喛鏅ч梻鍌欒兌椤牓鏁冮妷褎宕查柛顐犲劤瀹撲線鏌涢幇鈺佸闁哄啫鐗嗗婵囥亜閺冨牊锛熼柣銏狀煼濮婂宕掑▎鎴ｇ獥闂佸憡鐟ョ换妯侯潖娴犲绀嬫い鎾跺У閸ｎ噣姊婚崒姘偓鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸愯弓鐢婚梻渚€娼ч敍蹇涘礋椤愩垻鍝庡┑鐘愁問閸犳濡靛☉銏犵；闁规儳鐏堥崑?Set
                    val loaded: HashSet<Long> = gson.fromJson(json, type) ?: HashSet()
                    concurrentLongSet(loaded)
                }
            }
            return _pendingTrashIds!!
        }
        set(value) {
            _pendingTrashIds = concurrentLongSet(value)
            val snapshot = snapshotLongSetForPersist(value, OOM_FALLBACK_PENDING_TRASH_IDS)
            putJsonWithFallback("pending_trash", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_PENDING_TRASH_IDS / 2).coerceAtLeast(1))
            }
        }

    var appTrashInfo: MutableMap<Long, Long>
        get() {
            if (_appTrashInfo == null) {
                val type = object : TypeToken<HashMap<Long, Long>>() {}.type
                _appTrashInfo = readJsonOrDefault(
                    key = "app_trash_info",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁炬儳缍婇弻鐔兼⒒鐎靛壊妲梺姹囧€ら崰妤呭Φ閸曨垰绠涢柍杞拌兌閸旀挳姊洪幖鐐测偓鏍洪悢鐓庤摕婵炴垯鍩勯弫鍐煥濠靛棙顥滄い锔规櫊濮婄儤娼幍顕呮М濠碘槅鍋呴悷褔骞戦姀鐘闁靛繒濮烽鍝勨攽閻愬弶顥滅紒缁樺笚鐎靛ジ宕熼娑樷偓鐢告偡濞嗗繐顏悘蹇ｅ亞缁辨帡鎳滈棃娑樻懙濡ょ姷鍋涢崯鎶藉Φ閹版澘绠抽柟瀛樼矌閻愬﹪姊绘担鍛婂暈婵炶绠撳畷婊冾潩鐠鸿櫣锛熼梺瑙勫劤婢у海澹曢挊澹濆綊鏁愰崶銊ユ畬缂備焦褰冮…鐑藉蓟閻斿搫鏋堥柛妤冨仒閸戜粙鎮峰鍫殥缂佽埖鑹鹃～蹇撁洪鍕啇闂佺粯鍔栬ぐ鍐╂叏閵堝應鏀介柣鎰▕濡插綊鏌ｉ埡濠傜仩妞ゆ洩缍侀獮鏍ㄦ媴閻熼绨婚梻浣侯焾缁绘宕戦幇鏉垮偍闁告稑锕︾粻楣冨级閸繂鈷旈柛鎺嶅嵆閺岀喓鍠婇崡鐐扮敖闂佽桨绶￠崳锝夊箠濠婂牊鍊烽梺顐ｇ〒閻╁酣姊绘担鍛婃儓婵炴潙鍊圭粋宥夋倷閻㈢數鐓嬪銈嗘閺侇噣宕戦幘鑸靛枂闁告洦鍓涢ˇ銊╂煟閵忊晛鐏￠悽顖ょ節閺佹劙鎮欏ù瀣杸闁诲函缍嗘禍鐐恒€侀崨瀛樷拺缂備焦锚婵秵绻涙担鍐叉川瀹撲線鏌?
                    val loaded: HashMap<Long, Long> =
                        gson.fromJson(json, type) ?: HashMap()
                    val bounded = boundedMapSnapshot(loaded, MAX_APP_TRASH_INFO_ENTRIES)
                    if (bounded.size != loaded.size) {
                        putJsonSafely("app_trash_info", bounded)
                    }
                    java.util.concurrent.ConcurrentHashMap(bounded)
                }
            }
            return _appTrashInfo!!
        }
        set(value) {
            val bounded = boundedMapSnapshot(value, MAX_APP_TRASH_INFO_ENTRIES)
            _appTrashInfo = java.util.concurrent.ConcurrentHashMap(bounded)
            putJsonWithFallback("app_trash_info", bounded) {
                boundedMapSnapshot(bounded, (MAX_APP_TRASH_INFO_ENTRIES / 2).coerceAtLeast(1))
            }
        }

    var likedIds: MutableSet<Long>
        get() {
            if (_likedIds == null) {
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _likedIds = readJsonOrDefault(
                    key = "liked_ids",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap.newKeySet() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet() 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣閻ｉ箖鎮峰鍐ч柕鍡楀€块幖褰掝敃閿涘嫮鐣炬俊鐐€栭崝锕傚磻閸曨垱鍊堕柨鏇炲€归悡娆愩亜閺冨洤浜圭紒澶屽劋椤ㄣ儵鎮欓幍顔剧崲濡ょ姷鍋炵敮锟犵嵁鐎ｎ亶鐔嗘い蹇撳€告慨鍌涙叏婵犲啯銇濈€规洦鍋婂畷鐔碱敋閸涱喛鏅ч梻鍌欒兌椤牓鏁冮妷褎宕查柛顐犲劤瀹撲線鏌涢幇鈺佸闁哄啫鐗嗗婵囥亜閺冨牊锛熼柣銏狀煼濮婂宕掑▎鎴ｇ獥闂佸憡鐟ョ换妯侯潖娴犲绀嬫い鎾跺У閸ｎ噣姊婚崒姘偓鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸愯弓鐢婚梻渚€娼ч敍蹇涘礋椤愩垻鍝庡┑鐘愁問閸犳濡靛☉銏犵；闁规儳鐏堥崑?Set
                    val loaded: HashSet<Long> = gson.fromJson(json, type) ?: HashSet()
                    concurrentLongSet(loaded)
                }
            }
            return _likedIds!!
        }
        set(value) {
            _likedIds = concurrentLongSet(value)
            val snapshot = snapshotLongSetForPersist(value, OOM_FALLBACK_LIKED_IDS)
            putJsonWithFallback("liked_ids", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_LIKED_IDS / 2).coerceAtLeast(1))
            }
            LikesPhotoWidgetProvider.updateAllWidgets(context)
        }

    // New: Track moved items to prevent them from reappearing on Reset Filter
    private var _movedIds: MutableSet<Long>? = null
    var movedIds: MutableSet<Long>
        get() {
            if (_movedIds == null) {
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _movedIds = readJsonOrDefault(
                    key = "moved_ids",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap.newKeySet() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet() 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣閻ｉ箖鎮峰鍐ч柕鍡楀€块幖褰掝敃閿涘嫮鐣炬俊鐐€栭崝锕傚磻閸曨垱鍊堕柨鏇炲€归悡娆愩亜閺冨洤浜圭紒澶屽劋椤ㄣ儵鎮欓幍顔剧崲濡ょ姷鍋炵敮锟犵嵁鐎ｎ亶鐔嗘い蹇撳€告慨鍌涙叏婵犲啯銇濈€规洦鍋婂畷鐔碱敋閸涱喛鏅ч梻鍌欒兌椤牓鏁冮妷褎宕查柛顐犲劤瀹撲線鏌涢幇鈺佸闁哄啫鐗嗗婵囥亜閺冨牊锛熼柣銏狀煼濮婂宕掑▎鎴ｇ獥闂佸憡鐟ョ换妯侯潖娴犲绀嬫い鎾跺У閸ｎ噣姊婚崒姘偓鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸愯弓鐢婚梻渚€娼ч敍蹇涘礋椤愩垻鍝庡┑鐘愁問閸犳濡靛☉銏犵；闁规儳鐏堥崑?Set
                    val loaded: HashSet<Long> = gson.fromJson(json, type) ?: HashSet()
                    concurrentLongSet(loaded)
                }
            }
            return _movedIds!!
        }
        set(value) {
            _movedIds = concurrentLongSet(value)
            val snapshot = snapshotLongSetForPersist(value, OOM_FALLBACK_MOVED_IDS)
            putJsonWithFallback("moved_ids", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_MOVED_IDS / 2).coerceAtLeast(1))
            }
        }

    // New: Track user created folders
    private var _userCreatedFolders: MutableSet<String>? = null
    var userCreatedFolders: MutableSet<String>
        get() {
            if (_userCreatedFolders == null) {
                val type = object : TypeToken<HashSet<String>>() {}.type
                _userCreatedFolders = readJsonOrDefault(
                    key = "user_created_folders",
                    defaultValue = { HashSet() }
                ) { json ->
                    gson.fromJson(json, type) ?: HashSet()
                }
            }
            return _userCreatedFolders!!
        }
        set(value) {
            _userCreatedFolders = HashSet(value)
            putJsonSafely("user_created_folders", _userCreatedFolders!!)
        }

    // New: Folder Usage Tracking
    private var _folderUsage: MutableMap<String, Int>? = null // Cache for folder usage
    var folderUsage: MutableMap<String, Int>
        get() {
            if (_folderUsage == null) {
                val type = object : TypeToken<HashMap<String, Int>>() {}.type
                _folderUsage = readJsonOrDefault(
                    key = "folder_usage",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁炬儳缍婇弻鐔兼⒒鐎靛壊妲梺姹囧€ら崰妤呭Φ閸曨垰绠涢柍杞拌兌閸旀挳姊洪幖鐐测偓鏍洪悢鐓庤摕婵炴垯鍩勯弫鍐煥濠靛棙顥滄い锔规櫊濮婄儤娼幍顕呮М濠碘槅鍋呴悷褔骞戦姀鐘闁靛繒濮烽鍝勨攽閻愬弶顥滅紒缁樺笚鐎靛ジ宕熼娑樷偓鐢告偡濞嗗繐顏悘蹇ｅ亞缁辨帡鎳滈棃娑樻懙濡ょ姷鍋涢崯鎶藉Φ閹版澘绠抽柟瀛樼矌閻愬﹪姊绘担鍛婂暈婵炶绠撳畷婊冾潩鐠鸿櫣锛熼梺瑙勫劤婢у海澹曢挊澹濆綊鏁愰崶銊ユ畬缂備焦褰冮…鐑藉蓟閻斿搫鏋堥柛妤冨仒閸戜粙鎮峰鍫殥缂佽埖鑹鹃～蹇撁洪鍕啇闂佺粯鍔栬ぐ鍐╂叏閵堝應鏀介柣鎰▕濡插綊鏌ｉ埡濠傜仩妞ゆ洩缍侀獮鏍ㄦ媴閻熼绨婚梻浣侯焾缁绘宕戦幇鏉垮偍闁告稑锕︾粻楣冨级閸繂鈷旈柛鎺嶅嵆閺岀喓鍠婇崡鐐扮敖闂佽桨绶￠崳锝夊箠濠婂牊鍊烽梺顐ｇ〒閻╁酣姊绘担鍛婃儓婵炴潙鍊圭粋宥夋倷閻㈢數鐓嬪銈嗘閺侇噣宕戦幘鑸靛枂闁告洦鍓涢ˇ銊╂煟閵忊晛鐏￠悽顖ょ節閺佹劙鎮欏ù瀣杸闁诲函缍嗘禍鐐恒€侀崨瀛樷拺缂備焦锚婵秵绻涙担鍐叉川瀹撲線鏌?
                    val loaded: HashMap<String, Int> = gson.fromJson(json, type) ?: HashMap()
                    val bounded = snapshotMapForPersist(loaded, OOM_FALLBACK_FOLDER_USAGE_ENTRIES)
                    if (bounded.size != loaded.size) {
                        putJsonSafely("folder_usage", bounded)
                    }
                    java.util.concurrent.ConcurrentHashMap(bounded)
                }
            }
            return _folderUsage!!
        }
        set(value) {
            val bounded = snapshotMapForPersist(value, OOM_FALLBACK_FOLDER_USAGE_ENTRIES)
            _folderUsage = java.util.concurrent.ConcurrentHashMap(bounded)
            putJsonWithFallback("folder_usage", bounded) {
                boundedMapSnapshot(bounded, (OOM_FALLBACK_FOLDER_USAGE_ENTRIES / 2).coerceAtLeast(1))
            }
        }

    // New: Folder Last Used Timestamp (for scientific sorting)
    private var _folderLastUsed: MutableMap<String, Long>? = null
    var folderLastUsed: MutableMap<String, Long>
        get() {
            if (_folderLastUsed == null) {
                val type = object : TypeToken<HashMap<String, Long>>() {}.type
                _folderLastUsed = readJsonOrDefault(
                    key = "folder_last_used",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁炬儳缍婇弻鐔兼⒒鐎靛壊妲梺姹囧€ら崰妤呭Φ閸曨垰绠涢柍杞拌兌閸旀挳姊洪幖鐐测偓鏍洪悢鐓庤摕婵炴垯鍩勯弫鍐煥濠靛棙顥滄い锔规櫊濮婄儤娼幍顕呮М濠碘槅鍋呴悷褔骞戦姀鐘闁靛繒濮烽鍝勨攽閻愬弶顥滅紒缁樺笚鐎靛ジ宕熼娑樷偓鐢告偡濞嗗繐顏悘蹇ｅ亞缁辨帡鎳滈棃娑樻懙濡ょ姷鍋涢崯鎶藉Φ閹版澘绠抽柟瀛樼矌閻愬﹪姊绘担鍛婂暈婵炶绠撳畷婊冾潩鐠鸿櫣锛熼梺瑙勫劤婢у海澹曢挊澹濆綊鏁愰崶銊ユ畬缂備焦褰冮…鐑藉蓟閻斿搫鏋堥柛妤冨仒閸戜粙鎮峰鍫殥缂佽埖鑹鹃～蹇撁洪鍕啇闂佺粯鍔栬ぐ鍐╂叏閵堝應鏀介柣鎰▕濡插綊鏌ｉ埡濠傜仩妞ゆ洩缍侀獮鏍ㄦ媴閻熼绨婚梻浣侯焾缁绘宕戦幇鏉垮偍闁告稑锕︾粻楣冨级閸繂鈷旈柛鎺嶅嵆閺岀喓鍠婇崡鐐扮敖闂佽桨绶￠崳锝夊箠濠婂牊鍊烽梺顐ｇ〒閻╁酣姊绘担鍛婃儓婵炴潙鍊圭粋宥夋倷閻㈢數鐓嬪銈嗘閺侇噣宕戦幘鑸靛枂闁告洦鍓涢ˇ銊╂煟閵忊晛鐏￠悽顖ょ節閺佹劙鎮欏ù瀣杸闁诲函缍嗘禍鐐恒€侀崨瀛樷拺缂備焦锚婵秵绻涙担鍐叉川瀹撲線鏌?
                    val loaded: HashMap<String, Long> = gson.fromJson(json, type) ?: HashMap()
                    val bounded = snapshotMapForPersist(loaded, OOM_FALLBACK_FOLDER_LAST_USED_ENTRIES)
                    if (bounded.size != loaded.size) {
                        putJsonSafely("folder_last_used", bounded)
                    }
                    java.util.concurrent.ConcurrentHashMap(bounded)
                }
            }
            return _folderLastUsed!!
        }
        set(value) {
            val bounded = snapshotMapForPersist(value, OOM_FALLBACK_FOLDER_LAST_USED_ENTRIES)
            _folderLastUsed = java.util.concurrent.ConcurrentHashMap(bounded)
            putJsonWithFallback("folder_last_used", bounded) {
                boundedMapSnapshot(bounded, (OOM_FALLBACK_FOLDER_LAST_USED_ENTRIES / 2).coerceAtLeast(1))
            }
        }

    // New: Likes View Folder Selection Counts
    private var _likesFolderSelectionCounts: MutableMap<String, Int>? = null
    var likesFolderSelectionCounts: MutableMap<String, Int>
        get() {
            if (_likesFolderSelectionCounts == null) {
                val type = object : TypeToken<HashMap<String, Int>>() {}.type
                _likesFolderSelectionCounts = readJsonOrDefault(
                    key = "likes_folder_selection_counts",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁炬儳缍婇弻鐔兼⒒鐎靛壊妲梺姹囧€ら崰妤呭Φ閸曨垰绠涢柍杞拌兌閸旀挳姊洪幖鐐测偓鏍洪悢鐓庤摕婵炴垯鍩勯弫鍐煥濠靛棙顥滄い锔规櫊濮婄儤娼幍顕呮М濠碘槅鍋呴悷褔骞戦姀鐘闁靛繒濮烽鍝勨攽閻愬弶顥滅紒缁樺笚鐎靛ジ宕熼娑樷偓鐢告偡濞嗗繐顏悘蹇ｅ亞缁辨帡鎳滈棃娑樻懙濡ょ姷鍋涢崯鎶藉Φ閹版澘绠抽柟瀛樼矌閻愬﹪姊绘担鍛婂暈婵炶绠撳畷婊冾潩鐠鸿櫣锛熼梺瑙勫劤婢у海澹曢挊澹濆綊鏁愰崶銊ユ畬缂備焦褰冮…鐑藉蓟閻斿搫鏋堥柛妤冨仒閸戜粙鎮峰鍫殥缂佽埖鑹鹃～蹇撁洪鍕啇闂佺粯鍔栬ぐ鍐╂叏閵堝應鏀介柣鎰▕濡插綊鏌ｉ埡濠傜仩妞ゆ洩缍侀獮鏍ㄦ媴閻熼绨婚梻浣侯焾缁绘宕戦幇鏉垮偍闁告稑锕︾粻楣冨级閸繂鈷旈柛鎺嶅嵆閺岀喓鍠婇崡鐐扮敖闂佽桨绶￠崳锝夊箠濠婂牊鍊烽梺顐ｇ〒閻╁酣姊绘担鍛婃儓婵炴潙鍊圭粋宥夋倷閻㈢數鐓嬪銈嗘閺侇噣宕戦幘鑸靛枂闁告洦鍓涢ˇ銊╂煟閵忊晛鐏￠悽顖ょ節閺佹劙鎮欏ù瀣杸闁诲函缍嗘禍鐐恒€侀崨瀛樷拺缂備焦锚婵秵绻涙担鍐叉川瀹撲線鏌?
                    val loaded: HashMap<String, Int> = gson.fromJson(json, type) ?: HashMap()
                    val bounded = snapshotMapForPersist(loaded, OOM_FALLBACK_LIKES_FOLDER_SELECTION_ENTRIES)
                    if (bounded.size != loaded.size) {
                        putJsonSafely("likes_folder_selection_counts", bounded)
                    }
                    java.util.concurrent.ConcurrentHashMap(bounded)
                }
            }
            return _likesFolderSelectionCounts!!
        }
        set(value) {
            val bounded = snapshotMapForPersist(value, OOM_FALLBACK_LIKES_FOLDER_SELECTION_ENTRIES)
            _likesFolderSelectionCounts = java.util.concurrent.ConcurrentHashMap(bounded)
            putJsonWithFallback("likes_folder_selection_counts", bounded) {
                boundedMapSnapshot(bounded, (OOM_FALLBACK_LIKES_FOLDER_SELECTION_ENTRIES / 2).coerceAtLeast(1))
            }
        }

    // New: Smart Albums Persistence
    data class SmartAlbumDefinition(
        val id: String,
        val name: String,
        val rules: AlbumRules
    )
    
    data class DateRange(
        val start: Long,
        val end: Long
    )
    
    data class AlbumRules(
        val dateRange: DateRange? = null, // Start, End timestamp
        val location: String? = null,
        val keywords: List<String> = emptyList() // e.g. "Christmas", "Portrait"
    )

    private var _smartAlbums: MutableList<SmartAlbumDefinition>? = null
    var smartAlbums: MutableList<SmartAlbumDefinition>
        get() {
            if (_smartAlbums == null) {
                val json = prefs.getString("smart_albums", null)
                val type = object : TypeToken<ArrayList<SmartAlbumDefinition>>() {}.type
                _smartAlbums = try {
                    if (json != null) gson.fromJson(json, type) else ArrayList()
                } catch (e: Exception) {
                    ArrayList()
                }
            }
            return _smartAlbums!!
        }
        set(value) {
            _smartAlbums = value
            prefs.edit().putString("smart_albums", gson.toJson(value)).apply()
        }

    var danmakuEnabled: Boolean
        get() {
            if (_danmakuEnabled == null) {
                _danmakuEnabled = prefs.getBoolean("danmaku_enabled", false)
            }
            return _danmakuEnabled!!
        }
        set(value) {
            _danmakuEnabled = value
            prefs.edit().putBoolean("danmaku_enabled", value).apply()
        }

    var videoCompressBitrateKey: String
        get() {
            if (_videoCompressBitrateKey == null) {
                _videoCompressBitrateKey = prefs.getString("video_compress_bitrate_key", "balanced") ?: "balanced"
            }
            return _videoCompressBitrateKey!!
        }
        set(value) {
            _videoCompressBitrateKey = value
            prefs.edit().putString("video_compress_bitrate_key", value).apply()
        }

    var imageCompressThresholdMb: Int
        get() {
            if (_imageCompressThresholdMb == null) {
                _imageCompressThresholdMb = prefs.getInt("image_compress_threshold_mb", 3)
            }
            return _imageCompressThresholdMb!!
        }
        set(value) {
            _imageCompressThresholdMb = value
            prefs.edit().putInt("image_compress_threshold_mb", value).apply()
        }

    var imageCompressQualityKey: String
        get() {
            if (_imageCompressQualityKey == null) {
                _imageCompressQualityKey = prefs.getString("image_compress_quality_key", "balanced") ?: "balanced"
            }
            return _imageCompressQualityKey!!
        }
        set(value) {
            _imageCompressQualityKey = value
            prefs.edit().putString("image_compress_quality_key", value).apply()
        }

    var imageCompressResume: CompressionResumeState
        get() {
            if (_imageCompressResume == null) {
                val json = prefs.getString("image_compress_resume_v2", null)
                val type = object : TypeToken<CompressionResumeState>() {}.type
                _imageCompressResume = try {
                    if (json != null) gson.fromJson(json, type) else CompressionResumeState()
                } catch (e: Exception) {
                    CompressionResumeState()
                }
            }
            return _imageCompressResume!!
        }
        set(value) {
            _imageCompressResume = value
            prefs.edit().putString("image_compress_resume_v2", gson.toJson(value)).apply()
        }

    var videoCompressResume: CompressionResumeState
        get() {
            if (_videoCompressResume == null) {
                val json = prefs.getString("video_compress_resume_v2", null)
                val type = object : TypeToken<CompressionResumeState>() {}.type
                _videoCompressResume = try {
                    if (json != null) gson.fromJson(json, type) else CompressionResumeState()
                } catch (e: Exception) {
                    CompressionResumeState()
                }
            }
            return _videoCompressResume!!
        }
        set(value) {
            _videoCompressResume = value
            prefs.edit().putString("video_compress_resume_v2", gson.toJson(value)).apply()
        }

    var onboardingCompleted: Boolean
        get() {
            if (_onboardingCompleted == null) {
                _onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
            }
            return _onboardingCompleted!!
        }
        set(value) {
            _onboardingCompleted = value
            prefs.edit().putBoolean("onboarding_completed", value).apply()
        }

    var language: AppLanguage
        get() {
            if (_language == null) {
                val code = prefs.getString("language_code", null)
                _language = if (code != null) {
                    // Handle migration from old codes (zh-CN -> zh-SC, zh-TW -> zh-TC)
                    val migratedCode = when (code) {
                        "zh-CN" -> "zh-SC"
                        "zh-TW" -> "zh-TC"
                        else -> code
                    }
                    AppLanguage.values().find { it.code == migratedCode } ?: AppLanguage.English
                } else {
                    // Auto-detect system language
                    val sysLang = Locale.getDefault().language
                    when (sysLang) {
                        "zh" -> {
                            val country = Locale.getDefault().country
                            if (country == "TW" || country == "HK" || country == "MO") AppLanguage.TraditionalChinese else AppLanguage.SimplifiedChinese
                        }
                        "ko" -> AppLanguage.Korean
                        "ja" -> AppLanguage.Japanese
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
                        "uk" -> AppLanguage.Ukrainian
                        "pl" -> AppLanguage.Polish
                        "cs" -> AppLanguage.Czech
                        "nl" -> AppLanguage.Dutch
                        "sv" -> AppLanguage.Swedish
                        "da" -> AppLanguage.Danish
                        "nb", "no", "nn" -> AppLanguage.Norwegian
                        "fi" -> AppLanguage.Finnish
                        "el" -> AppLanguage.Greek
                        "ro" -> AppLanguage.Romanian
                        "hu" -> AppLanguage.Hungarian
                        "tr" -> AppLanguage.Turkish
                        "ms" -> AppLanguage.Malay
                        // Java still reports Hebrew under its legacy code.
                        "he", "iw" -> AppLanguage.Hebrew
                        else -> AppLanguage.English
                    }
                }
            }
            return _language!!
        }
        set(value) {
            _language = value
            prefs.edit().putString("language_code", value.code).apply()
        }

    var muteVideos: Boolean
        get() {
            if (_muteVideos == null) {
                _muteVideos = prefs.getBoolean("mute_videos", true)
            }
            return _muteVideos!!
        }
        set(value) {
            _muteVideos = value
            prefs.edit().putBoolean("mute_videos", value).apply()
        }

    var soundEffectsEnabled: Boolean
        get() = prefs.getBoolean("sound_effects_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_effects_enabled", value).apply()

    var stampSaveToGalleryEnabled: Boolean
        get() = prefs.getBoolean("stamp_save_to_gallery_enabled", false)
        set(value) = prefs.edit().putBoolean("stamp_save_to_gallery_enabled", value).apply()

    var hapticsEnabled: Boolean
        get() {
            if (_hapticsEnabled == null) {
                _hapticsEnabled = prefs.getBoolean("haptics_enabled", true)
            }
            return _hapticsEnabled!!
        }
        set(value) {
            _hapticsEnabled = value
            prefs.edit().putBoolean("haptics_enabled", value).apply()
        }

    var autoCheckUpdatesEnabled: Boolean
        get() {
            if (_autoCheckUpdatesEnabled == null) {
                _autoCheckUpdatesEnabled = prefs.getBoolean("auto_check_updates_enabled", false)
            }
            return _autoCheckUpdatesEnabled!!
        }
        set(value) {
            _autoCheckUpdatesEnabled = value
            prefs.edit().putBoolean("auto_check_updates_enabled", value).apply()
        }

    var campfireSelfParticipationChecked: Boolean
        get() = prefs.getBoolean("campfire_self_participation_checked", false)
        set(value) = prefs.edit().putBoolean("campfire_self_participation_checked", value).apply()

    var quickOrganizeMode: Boolean
        get() = prefs.getBoolean("quick_organize_mode", false)
        set(value) = prefs.edit().putBoolean("quick_organize_mode", value).apply()

    // Classic same-size cards with the info footer instead of cards cut to the photo (4.0 default).
    var fullCardMode: Boolean
        get() = prefs.getBoolean("organize_full_card_mode", false)
        set(value) = prefs.edit().putBoolean("organize_full_card_mode", value).apply()

    var modernHomeNavigationEnabled: Boolean
        get() = prefs.getBoolean("modern_home_navigation_enabled", true)
        set(value) = prefs.edit().putBoolean("modern_home_navigation_enabled", value).apply()

    var liquidGlassEnabled: Boolean
        get() = prefs.getBoolean("liquid_glass_enabled", false)
        set(value) = prefs.edit().putBoolean("liquid_glass_enabled", value).apply()

    var startupDefaultTab: String
        get() = when (val storedValue = prefs.getString("startup_default_tab", StartupWelcomeTab.Organize.name)) {
            StartupWelcomeTab.Organize.name,
            StartupWelcomeTab.ColorCard.name -> storedValue
            StartupWelcomeTab.Watermark.name -> StartupWelcomeTab.ColorCard.name
            else -> StartupWelcomeTab.Organize.name
        }
        set(value) {
            val normalized = when (value) {
                StartupWelcomeTab.Organize.name,
                StartupWelcomeTab.ColorCard.name -> value
                StartupWelcomeTab.Watermark.name -> StartupWelcomeTab.ColorCard.name
                else -> StartupWelcomeTab.Organize.name
            }
            prefs.edit().putString("startup_default_tab", normalized).apply()
        }

    var hiddenFoldersAffectMoveTargets: Boolean
        get() = prefs.getBoolean("hidden_folders_affect_move_targets", true)
        set(value) = prefs.edit().putBoolean("hidden_folders_affect_move_targets", value).apply()

    var compactSmallFoldersEnabled: Boolean
        get() = prefs.getBoolean("compact_small_folders_enabled", false)
        set(value) = prefs.edit().putBoolean("compact_small_folders_enabled", value).apply()

    var themeMode: AppThemeMode
        get() {
            if (_themeMode == null) {
                val key = prefs.getString("theme_mode", AppThemeMode.System.key) ?: AppThemeMode.System.key
                _themeMode = AppThemeMode.fromKey(key)
            }
            return _themeMode!!
        }
        set(value) {
            _themeMode = value
            prefs.edit().putString("theme_mode", value.key).apply()
        }

    var avatarType: String
        get() = prefs.getString("avatar_type", USER_AVATAR_TYPE_GIRL) ?: USER_AVATAR_TYPE_GIRL
        set(value) = prefs.edit().putString("avatar_type", value).apply()

    var customAvatarPath: String?
        get() = prefs.getString("custom_avatar_path", null)
            ?.takeIf { path -> File(path).exists() }
        set(value) = prefs.edit().putString("custom_avatar_path", value).apply()

    var batchSize: Int
        get() = prefs.getInt("batch_size", ORGANIZE_BATCH_SIZE_DEFAULT)
            .coerceIn(ORGANIZE_BATCH_SIZE_MIN, ORGANIZE_BATCH_SIZE_MAX)
        set(value) {
            prefs.edit()
                .putInt("batch_size", value.coerceIn(ORGANIZE_BATCH_SIZE_MIN, ORGANIZE_BATCH_SIZE_MAX))
                .commit()
        }

    var queueOrder: String
        get() = when (val storedValue = prefs.getString("queue_order", "random") ?: "random") {
            "time" -> "time_desc"
            "random", "time_asc", "time_desc", "size_desc", "size_asc" -> storedValue
            else -> "random"
        }
        set(value) = prefs.edit().putString("queue_order", value).apply()

    var labSlideshowSource: String
        get() = prefs.getString("lab_slideshow_source", "favorites") ?: "favorites"
        set(value) = prefs.edit().putString("lab_slideshow_source", value).apply()

    var labSlideshowFolder: String
        get() = prefs.getString("lab_slideshow_folder", "") ?: ""
        set(value) = prefs.edit().putString("lab_slideshow_folder", value).apply()

    var labPlaybackMode: String
        get() = prefs.getString("lab_playback_mode", "card") ?: "card"
        set(value) = prefs.edit().putString("lab_playback_mode", value).apply()

    var likesDisplayMode: String
        get() = prefs.getString("likes_display_mode", "tiktok") ?: "tiktok"
        set(value) = prefs.edit().putString("likes_display_mode", value).apply()

    var labSlideshowIntervalSec: Int
        get() = prefs.getInt("lab_slideshow_interval_sec", 3).coerceIn(2, 600)
        set(value) = prefs.edit().putInt("lab_slideshow_interval_sec", value.coerceIn(2, 600)).apply()

    var labSlideshowDurationEnabled: Boolean
        get() = prefs.getBoolean("lab_slideshow_duration_enabled", false)
        set(value) = prefs.edit().putBoolean("lab_slideshow_duration_enabled", value).apply()

    var labSlideshowShuffleEnabled: Boolean
        get() = prefs.getBoolean("lab_slideshow_shuffle_enabled", false)
        set(value) = prefs.edit().putBoolean("lab_slideshow_shuffle_enabled", value).apply()

    var labFullscreenLandscape: Boolean
        get() = prefs.getBoolean("lab_fullscreen_landscape", false)
        set(value) = prefs.edit().putBoolean("lab_fullscreen_landscape", value).apply()

    var labFullscreenDisplayMode: String
        get() = prefs.getString("lab_fullscreen_display_mode", "smart") ?: "smart"
        set(value) = prefs.edit().putString("lab_fullscreen_display_mode", value).apply()

    var labWholeViewBlurBackgroundEnabled: Boolean
        get() = prefs.getBoolean("lab_whole_view_blur_background_enabled", false)
        set(value) = prefs.edit().putBoolean("lab_whole_view_blur_background_enabled", value).apply()

    var labSlideshowDurationMin: Int
        get() = prefs.getInt("lab_slideshow_duration_min", 10).coerceIn(1, 180)
        set(value) = prefs.edit().putInt("lab_slideshow_duration_min", value.coerceIn(1, 180)).apply()

    var swipeLeftAction: String
        get() {
            if (_swipeLeftAction == null) {
                _swipeLeftAction = prefs.getString("swipe_left_action", "delete") ?: "delete"
            }
            return _swipeLeftAction!!
        }
        set(value) {
            _swipeLeftAction = value
            prefs.edit().putString("swipe_left_action", value).apply()
        }

    var swipeRightAction: String
        get() {
            if (_swipeRightAction == null) {
                _swipeRightAction = prefs.getString("swipe_right_action", "like") ?: "like"
            }
            return _swipeRightAction!!
        }
        set(value) {
            _swipeRightAction = value
            prefs.edit().putString("swipe_right_action", value).apply()
        }

    var swipeUpAction: String
        get() {
            if (_swipeUpAction == null) {
                _swipeUpAction = prefs.getString("swipe_up_action", "keep") ?: "keep"
            }
            return _swipeUpAction!!
        }
        set(value) {
            _swipeUpAction = value
            prefs.edit().putString("swipe_up_action", value).apply()
        }

    var swipeHorizontalSpeedMultiplier: Float
        get() {
            if (_swipeHorizontalSpeedTenths == null) {
                _swipeHorizontalSpeedTenths = prefs.getInt(
                    "swipe_horizontal_speed_tenths",
                    swipeSpeedMultiplierToTenths(DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER)
                )
            }
            return swipeSpeedTenthsToMultiplier(
                value = _swipeHorizontalSpeedTenths!!,
                defaultValue = DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER
            )
        }
        set(value) {
            val normalizedTenths = swipeSpeedMultiplierToTenths(value)
            _swipeHorizontalSpeedTenths = normalizedTenths
            prefs.edit().putInt("swipe_horizontal_speed_tenths", normalizedTenths).apply()
        }

    var swipeUpSpeedMultiplier: Float
        get() {
            if (_swipeUpSpeedTenths == null) {
                _swipeUpSpeedTenths = prefs.getInt(
                    "swipe_up_speed_tenths",
                    swipeSpeedMultiplierToTenths(DEFAULT_UP_SWIPE_SPEED_MULTIPLIER)
                )
            }
            return swipeSpeedTenthsToMultiplier(
                value = _swipeUpSpeedTenths!!,
                defaultValue = DEFAULT_UP_SWIPE_SPEED_MULTIPLIER
            )
        }
        set(value) {
            val normalizedTenths = swipeSpeedMultiplierToTenths(value)
            _swipeUpSpeedTenths = normalizedTenths
            prefs.edit().putInt("swipe_up_speed_tenths", normalizedTenths).apply()
        }

    var deletionMode: String
        get() = prefs.getString("deletion_mode", "recycle_bin") ?: "recycle_bin"
        set(value) = prefs.edit().putString("deletion_mode", value).apply()

    var deletionQueueReminderAckMilestone: Int
        get() = prefs.getInt("deletion_queue_reminder_ack_milestone", 0)
        set(value) = prefs.edit().putInt("deletion_queue_reminder_ack_milestone", value.coerceAtLeast(0)).apply()

    var filterFolder: String
        get() = prefs.getString("filter_folder", "All") ?: "All"
        set(value) = prefs.edit().putString("filter_folder", value).apply()

    var filterType: String
        get() = prefs.getString("filter_type", "All") ?: "All"
        set(value) = prefs.edit().putString("filter_type", value).apply()

    // New: Filter by Month (e.g., "2023-10") or "All"
    var filterMonth: String
        get() = prefs.getString("filter_month", "All") ?: "All"
        set(value) = prefs.edit().putString("filter_month", value).apply()

    // New: Filter by Day (e.g., "2023-10-15") or "All"
    var filterDay: String
        get() = prefs.getString("filter_day", "All") ?: "All"
        set(value) = prefs.edit().putString("filter_day", value).apply()

    // Photos organized today, for the home "N sorted today" line; resets when the day changes.
    var organizedTodayCount: Int
        get() = if (prefs.getInt("organized_today_day", 0) == todayDayValue()) {
            prefs.getInt("organized_today_count", 0)
        } else {
            0
        }
        set(value) = prefs.edit()
            .putInt("organized_today_day", todayDayValue())
            .putInt("organized_today_count", value.coerceAtLeast(0))
            .apply()

    // Organize range time preset: "All", "Today", "OnThisDay" or "RandomDay" (date kept in filter_day).
    var filterTimePreset: String
        get() = prefs.getString("filter_time_preset", OrganizeTimePreset.ALL) ?: OrganizeTimePreset.ALL
        set(value) = prefs.edit().putString("filter_time_preset", value).apply()

    var monthlyProgressEnabled: Boolean
        get() = prefs.getBoolean("monthly_progress_enabled", false)
        set(value) = prefs.edit().putBoolean("monthly_progress_enabled", value).apply()

    // New: Hidden Folders
    private var _hiddenFolders: MutableSet<String>? = null
    var hiddenFolders: MutableSet<String>
        get() {
            if (_hiddenFolders == null) {
                val type = object : TypeToken<HashSet<String>>() {}.type
                _hiddenFolders = readJsonOrDefault(
                    key = "hidden_folders",
                    defaultValue = { HashSet() }
                ) { json ->
                    gson.fromJson(json, type) ?: HashSet()
                }
            }
            return _hiddenFolders!!
        }
        set(value) {
            _hiddenFolders = HashSet(value)
            putJsonSafely("hidden_folders", _hiddenFolders!!)
        }

    // New: Prioritized Folders (for Move Queue)
    private val PRIORITIZED_FOLDERS_MANUAL_ONLY_KEY = "prioritized_folders_manual_only_v1"
    private var _prioritizedFolders: MutableSet<String>? = null
    var prioritizedFolders: MutableSet<String>
        get() {
            if (_prioritizedFolders == null) {
                // Older builds pinned every folder a photo was moved into, so the stored set can't
                // tell manual pins apart: start over once now that pinning is manual-only.
                if (!prefs.getBoolean(PRIORITIZED_FOLDERS_MANUAL_ONLY_KEY, false)) {
                    prefs.edit()
                        .remove("prioritized_folders")
                        .putBoolean(PRIORITIZED_FOLDERS_MANUAL_ONLY_KEY, true)
                        .apply()
                }
                val type = object : TypeToken<HashSet<String>>() {}.type
                _prioritizedFolders = readJsonOrDefault(
                    key = "prioritized_folders",
                    defaultValue = { HashSet() }
                ) { json ->
                    gson.fromJson(json, type) ?: HashSet()
                }
            }
            return _prioritizedFolders!!
        }
        set(value) {
            _prioritizedFolders = HashSet(value)
            putJsonSafely("prioritized_folders", _prioritizedFolders!!)
        }

    // Comments Storage
    var comments: MutableMap<Long, List<String>>
        get() {
            if (_comments == null) {
                val type = object : TypeToken<HashMap<Long, List<String>>>() {}.type
                _comments = readJsonOrDefault(
                    key = "comments",
                    defaultValue = { java.util.concurrent.ConcurrentHashMap() }
                ) { json ->
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁炬儳缍婇弻鐔兼⒒鐎靛壊妲梺姹囧€ら崰妤呭Φ閸曨垰绠涢柍杞拌兌閸旀挳姊洪幖鐐测偓鏍洪悢鐓庤摕婵炴垯鍩勯弫鍐煥濠靛棙顥滄い锔规櫊濮婄儤娼幍顕呮М濠碘槅鍋呴悷褔骞戦姀鐘闁靛繒濮烽鍝勨攽閻愬弶顥滅紒缁樺笚鐎靛ジ宕熼娑樷偓鐢告偡濞嗗繐顏悘蹇ｅ亞缁辨帡鎳滈棃娑樻懙濡ょ姷鍋涢崯鎶藉Φ閹版澘绠抽柟瀛樼矌閻愬﹪姊绘担鍛婂暈婵炶绠撳畷婊冾潩鐠鸿櫣锛熼梺瑙勫劤婢у海澹曢挊澹濆綊鏁愰崶銊ユ畬缂備焦褰冮…鐑藉蓟閻斿搫鏋堥柛妤冨仒閸戜粙鎮峰鍫殥缂佽埖鑹鹃～蹇撁洪鍕啇闂佺粯鍔栬ぐ鍐╂叏閵堝應鏀介柣鎰▕濡插綊鏌ｉ埡濠傜仩妞ゆ洩缍侀獮鏍ㄦ媴閻熼绨婚梻浣侯焾缁绘宕戦幇鏉垮偍闁告稑锕︾粻楣冨级閸繂鈷旈柛鎺嶅嵆閺岀喓鍠婇崡鐐扮敖闂佽桨绶￠崳锝夊箠濠婂牊鍊烽梺顐ｇ〒閻╁酣姊绘担鍛婃儓婵炴潙鍊圭粋宥夋倷閻㈢數鐓嬪銈嗘閺侇噣宕戦幘鑸靛枂闁告洦鍓涢ˇ銊╂煟閵忊晛鐏￠悽顖ょ節閺佹劙鎮欏ù瀣杸闁诲函缍嗘禍鐐恒€侀崨瀛樷拺缂備焦锚婵秵绻涙担鍐叉川瀹撲線鏌?
                    val loaded: HashMap<Long, List<String>> = gson.fromJson(json, type) ?: HashMap()
                    java.util.concurrent.ConcurrentHashMap(loaded)
                }
            }
            return _comments!!
        }
        set(value) {
            _comments = java.util.concurrent.ConcurrentHashMap(value)
            putJsonSafely("comments", _comments!!)
        }



    // --- Async Save Helpers ---
    fun saveProcessedIdsToDisk(ids: Set<Long>) {
        _processedIds = concurrentLongSet(ids)
        val snapshot = snapshotLongSetForPersist(ids, OOM_FALLBACK_PROCESSED_IDS)
        putJsonWithFallback("processed_ids", snapshot) {
            boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_PROCESSED_IDS / 2).coerceAtLeast(1))
        }
    }

    fun savePendingTrashIdsToDisk(ids: Set<Long>) {
        _pendingTrashIds = concurrentLongSet(ids)
        val snapshot = snapshotLongSetForPersist(ids, OOM_FALLBACK_PENDING_TRASH_IDS)
        putJsonWithFallback("pending_trash", snapshot) {
            boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_PENDING_TRASH_IDS / 2).coerceAtLeast(1))
        }
    }

    fun saveLikedIdsToDisk(ids: Set<Long>) {
        _likedIds = concurrentLongSet(ids)
        val snapshot = snapshotLongSetForPersist(ids, OOM_FALLBACK_LIKED_IDS)
        putJsonWithFallback("liked_ids", snapshot) {
            boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_LIKED_IDS / 2).coerceAtLeast(1))
        }
        LikesPhotoWidgetProvider.updateAllWidgets(context)
    }

    fun saveAppTrashInfoToDisk(info: Map<Long, Long>) {
        val bounded = boundedMapSnapshot(info, MAX_APP_TRASH_INFO_ENTRIES)
        _appTrashInfo = java.util.concurrent.ConcurrentHashMap(bounded)
        putJsonWithFallback("app_trash_info", bounded) {
            boundedMapSnapshot(bounded, (MAX_APP_TRASH_INFO_ENTRIES / 2).coerceAtLeast(1))
        }
    }

    fun saveHotStateSnapshot(
        processedIds: Set<Long>? = null,
        pendingTrashIds: Set<Long>? = null,
        likedIds: Set<Long>? = null,
        movedIds: Set<Long>? = null,
        folderUsage: Map<String, Int>? = null,
        stats: AppStats? = null
    ) {
        var shouldUpdateWidgets = false
        val editor = prefs.edit()
        var hasChanges = false

        processedIds?.let { ids ->
            if (_processedIds !== ids) {
                _processedIds = concurrentLongSet(ids)
            }
            val snapshot = snapshotLongSetForPersist(ids, OOM_FALLBACK_PROCESSED_IDS)
            val persisted = putJsonWithFallback(editor, "processed_ids", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_PROCESSED_IDS / 2).coerceAtLeast(1))
            }
            hasChanges = hasChanges || persisted
        }
        pendingTrashIds?.let { ids ->
            if (_pendingTrashIds !== ids) {
                _pendingTrashIds = concurrentLongSet(ids)
            }
            val snapshot = snapshotLongSetForPersist(ids, OOM_FALLBACK_PENDING_TRASH_IDS)
            val persisted = putJsonWithFallback(editor, "pending_trash", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_PENDING_TRASH_IDS / 2).coerceAtLeast(1))
            }
            hasChanges = hasChanges || persisted
        }
        likedIds?.let { ids ->
            if (_likedIds !== ids) {
                _likedIds = concurrentLongSet(ids)
            }
            val snapshot = snapshotLongSetForPersist(ids, OOM_FALLBACK_LIKED_IDS)
            val persisted = putJsonWithFallback(editor, "liked_ids", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_LIKED_IDS / 2).coerceAtLeast(1))
            }
            hasChanges = hasChanges || persisted
            shouldUpdateWidgets = true
        }
        movedIds?.let { ids ->
            if (_movedIds !== ids) {
                _movedIds = concurrentLongSet(ids)
            }
            val snapshot = snapshotLongSetForPersist(ids, OOM_FALLBACK_MOVED_IDS)
            val persisted = putJsonWithFallback(editor, "moved_ids", snapshot) {
                boundedLongSetSnapshot(snapshot, (OOM_FALLBACK_MOVED_IDS / 2).coerceAtLeast(1))
            }
            hasChanges = hasChanges || persisted
        }
        folderUsage?.let { usage ->
            _folderUsage = java.util.concurrent.ConcurrentHashMap(usage)
            val snapshot = snapshotMapForPersist(usage, OOM_FALLBACK_FOLDER_USAGE_ENTRIES)
            val persisted = putJsonWithFallback(editor, "folder_usage", snapshot) {
                boundedMapSnapshot(snapshot, (OOM_FALLBACK_FOLDER_USAGE_ENTRIES / 2).coerceAtLeast(1))
            }
            hasChanges = hasChanges || persisted
        }
        stats?.let { updated ->
            _stats = updated
            val persisted = putJsonSafely(editor, "stats", updated)
            hasChanges = hasChanges || persisted
        }

        if (!hasChanges) return
        editor.apply()
        if (shouldUpdateWidgets) {
            LikesPhotoWidgetProvider.updateAllWidgets(context)
        }
    }
}

fun shareMedia(context: Context, item: MediaItem) {
    val displayName = getShareDisplayName(context, item)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (item.mimeType.isNotEmpty()) item.mimeType else if (item.type == "video") "video/*" else "image/*"
        putExtra(Intent.EXTRA_STREAM, item.uri)
        putExtra(Intent.EXTRA_TITLE, displayName)
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        clipData = ClipData.newUri(context.contentResolver, displayName, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Share")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    }
}

private fun getShareDisplayName(context: Context, item: MediaItem): String {
    val fromItem = item.displayName.trim()
    if (fromItem.isNotEmpty()) return fromItem
    val fromResolver = runCatching {
        context.contentResolver.query(
            item.uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }.getOrNull()?.trim().orEmpty()
    if (fromResolver.isNotEmpty()) return fromResolver
    return if (item.type == "video") "video_${item.id}.mp4" else "image_${item.id}.jpg"
}

internal fun isHdrMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) return false
    val value = mimeType.lowercase()
    return value.contains("hdr") ||
        value.contains("hlg") ||
        value.contains("pq") ||
        value.contains("dolby") ||
        value.contains("dvh1") ||
        value.contains("dvhe")
}

internal fun calculateEdgeScrollSpeed(
    cardCenterX: Float,
    screenWidthPx: Float,
    edgeThreshold: Float,
    minSpeed: Float,
    maxSpeed: Float
): Float {
    val rightOverflow = cardCenterX - (screenWidthPx - edgeThreshold)
    if (rightOverflow > 0f) {
        val factor = (rightOverflow / edgeThreshold).coerceIn(0f, 1f)
        val easedFactor = kotlin.math.sqrt(factor)
        return minSpeed + (maxSpeed - minSpeed) * easedFactor
    }
    val leftOverflow = edgeThreshold - cardCenterX
    if (leftOverflow > 0f) {
        val factor = (leftOverflow / edgeThreshold).coerceIn(0f, 1f)
        val easedFactor = kotlin.math.sqrt(factor)
        return -(minSpeed + (maxSpeed - minSpeed) * easedFactor)
    }
    return 0f
}

// --- Data Models ---
enum class SwipeDirection {
    Left,
    Right,
    Up,
    Down
}

enum class AppThemeMode(val key: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromKey(key: String): AppThemeMode {
            return values().find { it.key == key } ?: System
        }
    }
}

data class UndoAnimationTarget(
    val itemId: Long,
    val direction: SwipeDirection,
    val token: Long,
    val restoreOffsetY: Float = 0f
)

internal const val UNDO_RESTORE_ANIMATION_DURATION_MS = 280L
private const val HOME_UNDO_ANIMATION_TARGET_RETENTION_MS = 1800L
private const val UNDO_CATALOG_SYNC_DEBOUNCE_MS = 96L
private const val HOME_UP_SWIPE_EXTEND_TRIGGER_DIVISOR = 4
private const val HOME_UP_SWIPE_EXTEND_MIN_REMAINING = 6
private const val HOME_UP_SWIPE_EXTEND_MAX_REMAINING = 12

private data class HomeUndoCatalogSyncRequest(
    val token: Long,
    val item: MediaItem,
    val criteria: MediaFilterCriteria,
    val batchSize: Int,
    val allMediaSnapshot: List<MediaItem>,
    val pendingTrashIds: Set<Long>,
    val hiddenFolders: Set<String>,
    val snappingCarouselInsertIndex: Int? = null
)

internal const val DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX = 300f
private const val MEDIASTORE_URI_BATCH_LIMIT = 1800

internal fun swipeActionIconForDirection(action: SwipeAction, direction: SwipeDirection): ImageVector {
    if (action != SwipeAction.Keep) return swipeActionIcon(action)
    return when (direction) {
        SwipeDirection.Left -> Icons.Outlined.ArrowBack
        SwipeDirection.Right -> Icons.Outlined.ArrowForward
        SwipeDirection.Up -> Icons.Outlined.ArrowUpward
        SwipeDirection.Down -> Icons.Outlined.ArrowDownward
    }
}

private fun mergeLowQualityDisplayKeepingVisible(
    currentDisplay: List<Pair<MediaItem, String>>,
    targetDisplay: List<Pair<MediaItem, String>>
): List<Pair<MediaItem, String>> {
    if (currentDisplay.isEmpty()) return unionLowQualityDisplayEntries(emptyList(), targetDisplay)
    if (targetDisplay.isEmpty()) return currentDisplay

    val targetById = targetDisplay.associateBy { it.first.id }
    val merged = ArrayList<Pair<MediaItem, String>>(maxOf(currentDisplay.size, targetDisplay.size))
    val seen = HashSet<Long>(currentDisplay.size + targetDisplay.size)

    currentDisplay.forEach { entry ->
        val latest = targetById[entry.first.id] ?: entry
        if (seen.add(latest.first.id)) merged.add(latest)
    }
    targetDisplay.forEach { entry ->
        if (seen.add(entry.first.id)) merged.add(entry)
    }
    return merged
}

internal fun unionLowQualityDisplayEntries(
    baseDisplay: List<Pair<MediaItem, String>>,
    overlayDisplay: List<Pair<MediaItem, String>>
): List<Pair<MediaItem, String>> {
    val merged = ArrayList<Pair<MediaItem, String>>(baseDisplay.size + overlayDisplay.size)
    val indexById = HashMap<Long, Int>(baseDisplay.size + overlayDisplay.size)

    fun upsert(entry: Pair<MediaItem, String>) {
        val existingIndex = indexById[entry.first.id]
        if (existingIndex == null) {
            indexById[entry.first.id] = merged.size
            merged.add(entry)
        } else {
            merged[existingIndex] = entry
        }
    }

    baseDisplay.forEach(::upsert)
    overlayDisplay.forEach(::upsert)
    return merged
}

sealed class UndoAction {
    abstract val item: MediaItem
    abstract val direction: SwipeDirection
    
    data class Trash(
        override val item: MediaItem,
        override val direction: SwipeDirection = SwipeDirection.Left,
        val restoreOffsetY: Float = 0f
    ) : UndoAction()
    data class Like(
        override val item: MediaItem,
        override val direction: SwipeDirection = SwipeDirection.Right
    ) : UndoAction()
    data class Keep(
        override val item: MediaItem,
        override val direction: SwipeDirection = SwipeDirection.Up
    ) : UndoAction()
    data class Move(
        override val item: MediaItem, 
        val sourceParentPath: String, 
        val targetFolder: String,
        val isNewFolder: Boolean = false,
        override val direction: SwipeDirection = SwipeDirection.Down
    ) : UndoAction()
}

data class BatchProgressSnapshot(
    val total: Int,
    val consumed: Int
)


// --- Main Activity ---
class NativeVoiceLikeActivity : AppCompatActivity() {
    override fun onDestroy() {
        super.onDestroy()
        VideoPlayerCache.clear()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏犖ч柛銉㈡櫇閸樼娀姊绘担铏瑰笡闁搞劌鐖煎鎼佹嚒閵堝洨顦梺鍝勮癁瀹€鈧崬鐢告煟閻樼儤顏犻悘蹇嬪姂瀹曟繈鎮㈤崗鑲╁幐闂佺硶鍓濋崝鏇㈠焵椤掍焦绀嬪┑鈩冩尦楠炴帡骞嬮鐔峰厞闂備焦瀵х换鍌炲箠瀹ュ棛鐝堕柡鍥ュ灪閻撶喖骞栧ǎ顒€鈧倕顭囬幇顓犵闁告瑥顧€閼板潡鏌℃担绋挎殻濠殿喒鍋撻梺缁樏悘姘枔妤ｅ啯鈷戦梻鍫熷崟閸儱鐤鹃柍鍝勬噹閺嬩線鏌涢妷顔煎闁绘挾鍠愭穱濠囶敍濞戞﹩鍤嬮梺缁樼箥娴滎亪寮诲☉銏″亹鐎规洖娲ら埛灞解攽椤旂》鍔熺紒顕呭灦楠炲繘宕ㄩ弶鎴濈獩婵犵數濮撮崐鐟扳枔濮椻偓濮婄粯鎷呴悷閭﹀殝缂佺偓婢樼粔瑙勪繆閻㈢绀嬫い鏍ㄦ皑椤撳搫鈹戦悩缁樻锭婵炲眰鍊曢埢宥夊Χ婢跺鎷洪梻渚囧亝缁嬫捇鍩為幒妤佺厱闁哄倽娉曠粻宕囩磼椤旀鍤欓柍钘夘樀婵偓闁绘ɑ顔栭崯瀣⒒娴ｇ儤鍤€闁告埃鍋撶紓浣插亾濞达絽婀遍々鎻捨旈敐鍛殲闁绘挻鐟╅弻锝夊箣閻戝洣绶靛┑鐐茬墛濮婂鍩€椤掑倹鍤€闁硅绱曢幑銏ゅ礃椤斿槈锕傛煕閺囥劌鐏犻柛鎰ㄥ亾闂備線娼ц噹闁逞屽墮鍗遍柛顐犲劜閳?PhotoCard 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁炬儳缍婇弻鐔兼⒒鐎靛壊妲紒鐐劤濠€閬嶅箟閹间焦鍋嬮柛顐ｇ箘閻熴劎绱撴担鍝勑￠柛妤佸▕瀵鎮㈤崗灏栨嫽闁诲海鏁搁…鍫熶繆娴犲鈷?
        if (AppPerformanceGovernor.isRunningLowOrCriticalTrimLevel(level)) {
            PhotoCardDataCache.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.CAMPFIRE_PAY_ENABLED) {
            CampfirePaymentReturnCenter.handleIntent(intent)
        }
        if (savedInstanceState == null) {
            PhotooDeepLinkCenter.handleIntent(intent)
        }
        val prefs = PreferencesManager(this)
        applyAppThemeMode(prefs.themeMode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
        }
        
        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Keep system-managed refresh rate for better battery and thermal balance.
        // Forcing max refresh globally causes significant power draw on some devices.
        
        supportActionBar?.hide()
        setContent {
            VoiceLikeTheme {
                VoiceLikeApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        CrashDebugProbe.markForeground(this, isForeground = true)
    }

    override fun onStop() {
        CrashDebugProbe.markForeground(this, isForeground = false)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (BuildConfig.CAMPFIRE_PAY_ENABLED) {
            CampfirePaymentReturnCenter.handleIntent(intent)
        }
        PhotooDeepLinkCenter.handleIntent(intent)
    }

    private fun enableHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: Use preferredDisplayModeId
                this.display?.let { d ->
                    val modes = d.supportedModes
                    // Find max refresh rate
                    val maxMode = modes.maxByOrNull { it.refreshRate }
                    maxMode?.let { mode ->
                        val lp = window.attributes
                        lp.preferredDisplayModeId = mode.modeId
                        window.attributes = lp
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23-29: Use preferredRefreshRate (deprecated but effective)
                val mode = windowManager.defaultDisplay.supportedModes.maxByOrNull { it.refreshRate }
                mode?.let {
                    val lp = window.attributes
                    lp.preferredRefreshRate = it.refreshRate
                    window.attributes = lp
                }
            }
        } catch (e: Exception) {
            // Fallback to system default
            Log.e("HighRefreshRate", "Failed to set high refresh rate: ${e.message}")
            AppRuntimeLogStore.logError(this, "HighRefreshRate", "Failed to set high refresh rate", e)
        }
    }
}

private fun applyAppThemeMode(mode: AppThemeMode) {
    val nightMode = when (mode) {
        AppThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
        AppThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
    }
    AppCompatDelegate.setDefaultNightMode(nightMode)
}

private fun restartForThemeModeChange(activity: Activity?) {
    activity ?: return
    val restartIntent = Intent(activity, NativeVoiceLikeActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    activity.startActivity(restartIntent)
    activity.finishAffinity()
}

@Composable
fun VoiceLikeTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val baseDensity = LocalDensity.current
    val fixedFontDensity = remember(baseDensity.density) {
        Density(density = baseDensity.density, fontScale = 1f)
    }
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Color(0xFF0B0B0B),
            onBackground = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1F1F1F),
            onSurfaceVariant = Color.White,
            secondaryContainer = Color(0xFF1A1A1A),
            outline = Color.White.copy(alpha = 0.08f)
	            )
	        } else {
        lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFF3F4F6),
            onSurfaceVariant = Color.Black,
            secondaryContainer = Color(0xFFE5E5E5),
            outline = Color.Black.copy(alpha = 0.08f)
        )
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }
    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
fun VoiceLikeApp() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var onboardingCompleted by remember { mutableStateOf(prefs.onboardingCompleted) }
    var pendingPostOnboardingStartupTab by rememberSaveable { mutableStateOf<String?>(null) }
    var currentLanguage by remember { mutableStateOf(prefs.language) }
    var hapticsEnabled by remember { mutableStateOf(prefs.hapticsEnabled) }
    var swipeLeftAction by remember { mutableStateOf(parseSwipeAction(prefs.swipeLeftAction)) }
    var swipeRightAction by remember { mutableStateOf(parseSwipeAction(prefs.swipeRightAction)) }
    var swipeUpAction by remember { mutableStateOf(parseSwipeAction(prefs.swipeUpAction)) }
    var modernStartupWelcomeTab by rememberSaveable { mutableStateOf(prefs.startupDefaultTab) }
    var deletionMode by remember { mutableStateOf(prefs.deletionMode) }
    val strings = getStrings(currentLanguage)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            hasPermission = perms.values.all { it }
        }
    )

    val requestPermissions = {
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.ACCESS_MEDIA_LOCATION))
            } else {
                launcher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION))
            }
        }
    }

    // Check for MANAGE_EXTERNAL_STORAGE (Android 11+)
    var hasManageStorage by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                true // Not needed below R
            }
        )
    }

    // Monitor lifecycle to update permission status when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    hasManageStorage = android.os.Environment.isExternalStorageManager()
                }
                // Also update standard permissions just in case
                hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    var showDeferredPermissionNotice by rememberSaveable(onboardingCompleted, hasPermission) {
        mutableStateOf(onboardingCompleted && !hasPermission)
    }
    LaunchedEffect(onboardingCompleted, hasPermission) {
        if (!onboardingCompleted || hasPermission) {
            showDeferredPermissionNotice = false
        }
    }

    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
        LocalHapticsEnabled provides hapticsEnabled
    ) {
        if (!onboardingCompleted) {
            WelcomeView(
                currentLanguage = currentLanguage,
                onLanguageSelected = { lang ->
                    currentLanguage = lang
                    prefs.language = lang
                },
                hasPermission = hasPermission,
                onRequestPermission = requestPermissions,
                hasManageStorage = hasManageStorage,
                onRequestManageStorage = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    }
                },
                leftAction = swipeLeftAction,
                rightAction = swipeRightAction,
                upAction = swipeUpAction,
                onLeftActionChange = {
                    swipeLeftAction = it
                    prefs.swipeLeftAction = swipeActionKey(it)
                },
                onRightActionChange = {
                    swipeRightAction = it
                    prefs.swipeRightAction = swipeActionKey(it)
                },
                onUpActionChange = {
                    swipeUpAction = it
                    prefs.swipeUpAction = swipeActionKey(it)
                },
                startupDefaultTab = modernStartupWelcomeTab,
                onStartupDefaultTabChange = { tab ->
                    modernStartupWelcomeTab = tab
                    prefs.startupDefaultTab = tab
                },
                deletionMode = deletionMode,
                onDeletionModeChange = {
                    deletionMode = it
                    prefs.deletionMode = it
                },
                onFinish = {
                    prefs.modernHomeNavigationEnabled = true
                    pendingPostOnboardingStartupTab = modernStartupWelcomeTab
                    prefs.onboardingCompleted = true
                    onboardingCompleted = true
                }
            )
	        } else {
            if (showDeferredPermissionNotice) {
                AlertDialog(
                    onDismissRequest = { showDeferredPermissionNotice = false },
                    title = { Text(deferredPermissionDialogTitle(currentLanguage)) },
                    text = { Text(deferredPermissionDialogMessage(currentLanguage)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeferredPermissionNotice = false
                                requestPermissions()
                            }
                        ) {
                            Text(deferredPermissionAuthorizeLabel(currentLanguage))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeferredPermissionNotice = false }) {
                            Text(deferredPermissionContinueLabel(currentLanguage))
                        }
                    }
                )
            }
            MainScreen(
                hasMediaPermission = hasPermission,
                onRequestMediaPermission = {
                    showDeferredPermissionNotice = true
                },
                initialStartupTabOverride = pendingPostOnboardingStartupTab,
                onInitialStartupTabConsumed = {
                    pendingPostOnboardingStartupTab = null
                }
            )
        }
    }
}

private fun deferredPermissionDialogTitle(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SimplifiedChinese -> "暂未授权"
        AppLanguage.TraditionalChinese -> "暫未授權"
        else -> "Permission not granted"
    }
}

private fun deferredPermissionDialogMessage(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SimplifiedChinese ->
            "暂未授权，基础功能可能无法正常使用，依然继续。后续当您实际使用照片整理、压缩、移动等需要访问媒体的功能时，我们会同步说明用途，并再向您申请所需权限。"
        AppLanguage.TraditionalChinese ->
            "暫未授權，基礎功能可能無法正常使用，依然繼續。後續當您實際使用照片整理、壓縮、移動等需要存取媒體的功能時，我們會同步說明用途，並再向您申請所需權限。"
        else ->
            "Permission is not granted yet. Some core media features may be unavailable, but you can continue. When you actually use a feature like organizing, compressing, or moving photos, we'll explain why the access is needed and ask again then."
    }
}

private fun deferredPermissionAuthorizeLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SimplifiedChinese -> "去授权"
        AppLanguage.TraditionalChinese -> "去授權"
        else -> "Grant access"
    }
}

private fun deferredPermissionContinueLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SimplifiedChinese -> "依然继续"
        AppLanguage.TraditionalChinese -> "依然繼續"
        else -> "Continue"
    }
}

@Composable
fun MainScreen(
    hasMediaPermission: Boolean = true,
    onRequestMediaPermission: () -> Unit = {},
    initialStartupTabOverride: String? = null,
    onInitialStartupTabConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val aiHomepageDisplayPrefs = remember { AiHomepageDisplayPreferences(context) }
    val homeSnappingCarouselLabPrefs = remember { HomeSnappingCarouselLabPreferences(context) }
    val colorCardFeaturePrefs = remember { ColorCardFeaturePreferences(context) }
    val haptic = LocalHapticFeedback.current
    
    // Localization
    var currentLanguage by remember { mutableStateOf(prefs.language) }
    val strings = getStrings(currentLanguage)

    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    var hapticsEnabled by remember { mutableStateOf(prefs.hapticsEnabled) }
    var liquidGlassEnabled by remember { mutableStateOf(prefs.liquidGlassEnabled) }
    var autoCheckUpdatesEnabled by remember { mutableStateOf(prefs.autoCheckUpdatesEnabled) }
    var colorCardFeatureEnabled by remember { mutableStateOf(colorCardFeaturePrefs.enabled) }
    val updateUiState = rememberAppUpdateUiState(context, currentLanguage, strings, scope)
    LaunchedEffect(colorCardFeaturePrefs) {
        colorCardFeaturePrefs.enabled = true
        colorCardFeatureEnabled = true
    }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
        LocalHapticsEnabled provides hapticsEnabled,
        LocalLiquidGlassEnabled provides (liquidGlassEnabled && isLiquidGlassSupported())
    ) {
        AppUpdateDialogHost(updateUiState = updateUiState)
        val isDark = isSystemInDarkTheme()
        val hapticsAllowed = LocalHapticsEnabled.current
        // Core State
        var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
        var feedLoadedRows by remember { mutableIntStateOf(0) }
        val globalCatalog = remember { GlobalMediaCatalog() }
        val swipeWindow = remember { SwipeWindow(globalCatalog) }
        val homeFeedCoordinator = remember { HomeFeedCoordinator(globalCatalog, swipeWindow) }
        LaunchedEffect(Unit) {
            // Keep a rollback tail during rapid swipes; can be tuned dynamically later by memory headroom.
            homeFeedCoordinator.configureRollbackBuffer(36)
        }
        LaunchedEffect(autoCheckUpdatesEnabled, updateUiState) {
            if (autoCheckUpdatesEnabled) {
                updateUiState.checkForUpdates(manual = false)
            }
        }
    var displayMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) } // Filtered list
    var homeSnappingCarouselCurrentPage by rememberSaveable { mutableIntStateOf(0) }
    var homeSnappingCarouselPendingUndoPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var homeSnappingCarouselBatchOrderIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var homeSnappingCarouselDeleteReturnPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var homeSnappingCarouselDeleteReturnItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var homeSnappingCarouselDeleteReturnToSettlement by rememberSaveable { mutableStateOf(false) }
    var homeSnappingCarouselDeleteReturnArmed by rememberSaveable { mutableStateOf(false) }
    var homeSnappingCarouselUndoInFlightToken by remember { mutableStateOf<Long?>(null) }
    var homeSnappingCarouselUndoReleaseJob by remember { mutableStateOf<Job?>(null) }
    var isFullAlbumDone by remember { mutableStateOf(false) } // Track if all items are processed
    var loading by remember { mutableStateOf(true) }
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var hasVerifiedCatalog by remember { mutableStateOf(false) }
    var showStartupWelcomeCard by remember { mutableStateOf(true) }
    val coldStartAtMs = remember { SystemClock.elapsedRealtime() }
    var lastBackgroundAt by remember { mutableLongStateOf(0L) }
    var skipNextCatalogSync by remember { mutableStateOf(false) }
    var batchSize by remember { mutableIntStateOf(prefs.batchSize) }
    var currentBatchTotal by remember { mutableIntStateOf(batchSize) }
    var currentBatchConsumed by remember { mutableIntStateOf(0) }
    var topCardPlaybackArmed by remember { mutableStateOf(true) }
    var nextPlaybackDelayMs by remember { mutableLongStateOf(170L) }
    var startupEwmaMs by remember { mutableFloatStateOf(140f) }
    var lastSwipeCommittedAtMs by remember { mutableLongStateOf(0L) }
    var rapidSwipeStreak by remember { mutableIntStateOf(0) }
    var predictedSwipeIntent by remember { mutableStateOf(SwipeIntent.None) }
    var playbackGateJob by remember { mutableStateOf<Job?>(null) }
    var undoTokenSequence by remember { mutableLongStateOf(0L) }
    var pendingHomeUndoCatalogSync by remember { mutableStateOf<HomeUndoCatalogSyncRequest?>(null) }
    var queueOrder by remember { mutableStateOf(prefs.queueOrder) }
    var queueRandomOrderSeed by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtimeNanos()) }
    var swipeLeftAction by remember { mutableStateOf(parseSwipeAction(prefs.swipeLeftAction)) }
    var swipeRightAction by remember { mutableStateOf(parseSwipeAction(prefs.swipeRightAction)) }
    var swipeUpAction by remember { mutableStateOf(parseSwipeAction(prefs.swipeUpAction)) }
    var swipeHorizontalSpeedMultiplier by remember {
        mutableFloatStateOf(prefs.swipeHorizontalSpeedMultiplier)
    }
    var swipeUpSpeedMultiplier by remember {
        mutableFloatStateOf(prefs.swipeUpSpeedMultiplier)
    }
    LaunchedEffect(swipeHorizontalSpeedMultiplier, swipeUpSpeedMultiplier) {
        SwipeMotionPreferences.update(
            horizontal = swipeHorizontalSpeedMultiplier,
            up = swipeUpSpeedMultiplier
        )
    }
    var showDeletionMode by remember { mutableStateOf(false) } // New state for deletion mode page
    var deletionMode by remember { mutableStateOf(prefs.deletionMode) }
    var filterFolder by remember { mutableStateOf(prefs.filterFolder) }
    var filterType by remember { mutableStateOf(prefs.filterType) }
    var filterMonth by remember { mutableStateOf(prefs.filterMonth) } // New
    var filterDay by remember { mutableStateOf(prefs.filterDay) } // New
    var filterTimePreset by remember { mutableStateOf(prefs.filterTimePreset) }
    // Insights collection: temporary, so it lives only here and never reaches prefs.
    var filterCollection by remember { mutableStateOf<Pair<String, Set<Long>>?>(null) }
    
    // Pending Filter State (for coherent filter logic)
    var pendingFilterFolder by remember { mutableStateOf(filterFolder) }
    var pendingFilterType by remember { mutableStateOf(filterType) }
    var pendingFilterMonth by remember { mutableStateOf(filterMonth) }
    var pendingFilterDay by remember { mutableStateOf(filterDay) }
    var monthlyProgressEnabled by remember { mutableStateOf(prefs.monthlyProgressEnabled) }
    val buildFilterCriteria = {
        MediaFilterCriteria(
            folder = filterFolder,
            type = filterType,
            month = filterMonth,
            day = filterDay,
            timePreset = filterTimePreset,
            collectionTitle = filterCollection?.first,
            collectionIds = filterCollection?.second
        )
    }
    // The single place that changes the organize range; callers reload the deck.
    fun setOrganizeRange(range: MediaFilterCriteria) {
        val collectionIds = range.collectionIds
        if (collectionIds != null) {
            // Keep the saved range in prefs untouched; a relaunch goes back to it.
            filterCollection = range.collectionTitle.orEmpty() to collectionIds
            filterFolder = "All"
            filterType = "All"
            filterMonth = "All"
            filterDay = "All"
            filterTimePreset = OrganizeTimePreset.ALL
            return
        }
        filterCollection = null
        filterFolder = range.folder
        prefs.filterFolder = range.folder
        filterType = range.type
        prefs.filterType = range.type
        filterMonth = range.month
        prefs.filterMonth = range.month
        filterDay = range.day
        prefs.filterDay = range.day
        filterTimePreset = range.timePreset
        prefs.filterTimePreset = range.timePreset
        pendingFilterFolder = range.folder
        pendingFilterType = range.type
        pendingFilterMonth = range.month
        pendingFilterDay = range.day
    }

    var hiddenFolders by remember { mutableStateOf(prefs.hiddenFolders) } // New
    // Hidden albums for the organize deck: a selected hidden album still counts.
    fun deckHiddenFolders(): Set<String> = effectiveHiddenFolders(hiddenFolders, buildFilterCriteria())
    var hiddenFoldersAffectMoveTargets by remember { mutableStateOf(prefs.hiddenFoldersAffectMoveTargets) }
    var compactSmallFoldersEnabled by remember { mutableStateOf(prefs.compactSmallFoldersEnabled) }
    var folderUsageState by remember { mutableStateOf(prefs.folderUsage) }
    var backtrackModeEnabled by remember { mutableStateOf(homeSnappingCarouselLabPrefs.enabled) }
    var quickOrganizeMode by remember { mutableStateOf(prefs.quickOrganizeMode) }
    var fullCardMode by remember { mutableStateOf(prefs.fullCardMode) }
    var modernHomeNavigationEnabled by remember { mutableStateOf(prefs.modernHomeNavigationEnabled) }
    var modernStartupWelcomeTab by rememberSaveable { mutableStateOf(prefs.startupDefaultTab) }
    var pendingQuickOrganizeRefresh by remember { mutableStateOf(false) }
    var muteVideos by remember { mutableStateOf(prefs.muteVideos) }
    var soundEffectsEnabled by remember { mutableStateOf(prefs.soundEffectsEnabled) }
    var lensyEnabled by remember { mutableStateOf(aiHomepageDisplayPrefs.lensyEnabled) }
    var aiImageResultsEnabled by remember { mutableStateOf(aiHomepageDisplayPrefs.imageResultsEnabled) }
    var appThemeMode by remember { mutableStateOf(prefs.themeMode) }
    val lensyAvailable = remember(currentLanguage) { isLensySupported(currentLanguage) }
    val effectiveLensyEnabled = lensyAvailable && lensyEnabled
    val lensyInfoCopy = remember(currentLanguage) { getLensyInfoCopy(currentLanguage) }

    // Entrance Animation State
    var isInitialLoad by rememberSaveable { mutableStateOf(true) }
    var entryAnimationToken by remember { mutableLongStateOf(0L) }
    var homeSnappingCarouselSettlementBatchToken by rememberSaveable { mutableLongStateOf(0L) }
    val entryRotationSeeds = listOf(-16f, 12f, -10f, 18f, -14f, 9f)
    val entryScreenConfig = LocalConfiguration.current
    val entryScreenWidthPx = with(LocalDensity.current) { entryScreenConfig.screenWidthDp.dp.toPx() }
    val entryScreenHeightPx = with(LocalDensity.current) { entryScreenConfig.screenHeightDp.dp.toPx() }
    val entryCardHeightPx = entryScreenWidthPx * 0.85f * (14f / 9f)
    val entryOffscreenY = entryScreenHeightPx + entryCardHeightPx + with(LocalDensity.current) { 44.dp.toPx() }
    val cardEntryOffsets = remember(entryAnimationToken, entryOffscreenY) { List(6) { Animatable(entryOffscreenY) } }
    val cardEntryRotations = remember(entryAnimationToken) {
        entryRotationSeeds.map { Animatable(it) }
    }
    val entryStaggerMs = 120L
    val entryInitialDelayMs = 120L
    val entryFirstCardExtraDelayMs = 0L
    val entryCompletionGuardMs = 1180L

    // Sound Pool (Moved up to be available for initial animation)
    val soundPool = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.media.SoundPool.Builder().setMaxStreams(5).build()
        } else {
            @Suppress("DEPRECATION")
            android.media.SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0)
        }
    }
    val swipeSoundId = remember { mutableIntStateOf(0) }
    val deleteSoundId = remember { mutableIntStateOf(0) }
    val magicSoundId = remember { mutableIntStateOf(0) }
    val startSoundId = remember { mutableIntStateOf(0) } // New start sound
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val cardsResId = context.resources.getIdentifier("cards", "raw", context.packageName)
                if (cardsResId != 0) swipeSoundId.intValue = soundPool.load(context, cardsResId, 1)
                
                val deleteResId = context.resources.getIdentifier("delete_sound", "raw", context.packageName)
                if (deleteResId != 0) deleteSoundId.intValue = soundPool.load(context, deleteResId, 1)

                val magicResId = context.resources.getIdentifier("magic", "raw", context.packageName)
                if (magicResId != 0) magicSoundId.intValue = soundPool.load(context, magicResId, 1)

                val startResId = context.resources.getIdentifier("start", "raw", context.packageName)
                if (startResId != 0) startSoundId.intValue = soundPool.load(context, startResId, 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val playSwipeSound = {
        if (soundEffectsEnabled && swipeSoundId.intValue != 0) {
            soundPool.play(swipeSoundId.intValue, 1f, 1f, 0, 0, 1f)
        }
    }
    
    val playDeleteSound = {
        if (soundEffectsEnabled) {
            if (deleteSoundId.intValue != 0) {
                soundPool.play(deleteSoundId.intValue, 0.7f, 0.7f, 0, 0, 1f)
            } else {
                playSwipeSound()
            }
        }
    }

    val playLikeSound = {
        if (soundEffectsEnabled) {
            if (magicSoundId.intValue != 0) {
                soundPool.play(magicSoundId.intValue, 1f, 1f, 0, 0, 1f)
            } else {
                playSwipeSound()
            }
        }
    }

    fun resolveHomeCarouselPage(items: List<MediaItem> = displayMedia): Int {
        if (!backtrackModeEnabled) return 0
        val maxPage = if (items.isEmpty()) 0 else items.size
        return homeSnappingCarouselCurrentPage.coerceIn(0, maxPage)
    }

    fun resolveHomeFocusIndex(items: List<MediaItem> = displayMedia): Int {
        if (items.isEmpty()) return 0
        return if (backtrackModeEnabled) {
            resolveHomeCarouselPage(items).coerceIn(0, items.lastIndex)
        } else {
            0
        }
    }

    fun resolveHomeFocusedItem(items: List<MediaItem> = displayMedia): MediaItem? {
        return items.getOrNull(resolveHomeFocusIndex(items))
    }

    fun resolveHomePlaybackItem(items: List<MediaItem> = displayMedia): MediaItem? {
        return if (backtrackModeEnabled) {
            resolveHomeFocusedItem(items)
        } else {
            items.firstOrNull()
        }
    }

    LaunchedEffect(displayMedia.size, displayMedia.firstOrNull()?.id, displayMedia.lastOrNull()?.id) {
        if (displayMedia.isEmpty()) {
            if (homeSnappingCarouselCurrentPage != 0) {
                homeSnappingCarouselCurrentPage = 0
            }
        } else {
            val clampedPage = homeSnappingCarouselCurrentPage.coerceIn(0, displayMedia.size)
            if (homeSnappingCarouselCurrentPage != clampedPage) {
                homeSnappingCarouselCurrentPage = clampedPage
            }
        }
    }

    // Track pending sound play if ID is not yet loaded
    var pendingStartSoundPlay by remember { mutableStateOf(false) }

    val playStartSound = {
        if (soundEffectsEnabled) {
            if (startSoundId.intValue != 0) {
                soundPool.play(startSoundId.intValue, 1f, 1f, 0, 0, 1f)
                pendingStartSoundPlay = false
            } else {
                // Mark as pending if not loaded yet
                pendingStartSoundPlay = true
            }
        }
    }

    // Monitor sound load completion for pending plays
    LaunchedEffect(startSoundId.intValue) {
        if (startSoundId.intValue != 0 && pendingStartSoundPlay && soundEffectsEnabled) {
            soundPool.play(startSoundId.intValue, 1f, 1f, 0, 0, 1f)
            pendingStartSoundPlay = false
        }
    }

    var suppressStartupHomeEntryOnce by remember { mutableStateOf(false) }
    var startupHomeTransitionRunning by remember { mutableStateOf(false) }
    val startupHomeTransitionProgress by animateFloatAsState(
        targetValue = if (startupHomeTransitionRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "startup_home_transition_progress"
    )

    LaunchedEffect(initialStartupTabOverride) {
        val overrideTab = initialStartupTabOverride ?: return@LaunchedEffect
        val normalizedTab = when (overrideTab) {
            StartupWelcomeTab.ColorCard.name -> StartupWelcomeTab.ColorCard.name
            StartupWelcomeTab.Watermark.name -> StartupWelcomeTab.ColorCard.name
            else -> StartupWelcomeTab.Organize.name
        }
        if (!modernHomeNavigationEnabled) {
            modernHomeNavigationEnabled = true
            prefs.modernHomeNavigationEnabled = true
        }
        modernStartupWelcomeTab = normalizedTab
        showStartupWelcomeCard = true
        startupHomeTransitionRunning = false
        onInitialStartupTabConsumed()
    }

    LaunchedEffect(
        displayMedia,
        entryAnimationToken,
        loading,
        showStartupWelcomeCard,
        suppressStartupHomeEntryOnce,
        backtrackModeEnabled
    ) {
        if (isInitialLoad && !showStartupWelcomeCard && !loading && displayMedia.isNotEmpty()) {
            if (suppressStartupHomeEntryOnce) {
                suppressStartupHomeEntryOnce = false
                isInitialLoad = false
                return@LaunchedEffect
            }
            if (backtrackModeEnabled) {
                return@LaunchedEffect
            }
            playStartSound()
            delay(entryInitialDelayMs) // Let first frame settle before card entrance.
            // Trigger animation
            cardEntryOffsets.forEachIndexed { index, anim ->
                launch {
                    val itemDelay = index * entryStaggerMs + if (index == 0) entryFirstCardExtraDelayMs else 0L
                    delay(itemDelay)
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 600f
                        )
                    )
                }
            }
            cardEntryRotations.forEachIndexed { index, anim ->
                launch {
                    val itemDelay = index * entryStaggerMs + if (index == 0) entryFirstCardExtraDelayMs else 0L
                    delay(itemDelay)
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = 380f
                        )
                    )
                }
            }
            // Mark as loaded so it doesn't replay on recomposition or new batches
            // We delay slightly to ensure the animation logic has kicked off
            delay(entryCompletionGuardMs)
            isInitialLoad = false
        }
    }
    var avatarType by remember { mutableStateOf(prefs.avatarType) }
    var customAvatarPath by remember { mutableStateOf(prefs.customAvatarPath) }
    
    // Stats State
    var stats by remember { mutableStateOf(prefs.stats) }
    var pendingTrashCount by remember { mutableIntStateOf(prefs.pendingTrashIds.size) }
    var mediaTotalCount by remember { mutableIntStateOf(0) }
    var firstInteractiveAtMs by remember { mutableLongStateOf(0L) }
    var stagedHeavyWorkCards by remember { mutableIntStateOf(2) }
    var mediaTotalSizeBytes by remember { mutableLongStateOf(0L) }
    var pendingStartupCatalogHydration by remember { mutableStateOf(false) }
    var startupCatalogHydrationRunning by remember { mutableStateOf(false) }
    var fullCatalogLoaded by remember { mutableStateOf(false) }
    var fullCatalogLoading by remember { mutableStateOf(false) }
    var suppressStatsHapticsOnce by remember { mutableStateOf(false) }
    var showDeletionQueueReminder by remember { mutableStateOf(false) }
    var deletionQueueReminderCount by remember { mutableIntStateOf(0) }
    var lastDeletionQueueMilestoneShown by remember { mutableIntStateOf(prefs.deletionQueueReminderAckMilestone) }
    var hasShownReminderThisSession by remember { mutableStateOf(false) }
    var startupGreetingSequence by remember { mutableIntStateOf(context.getSharedPreferences("voicelike_prefs", Context.MODE_PRIVATE).getInt("startup_greeting_sequence", 0)) }
    var startupLastGreetingKey by remember { mutableStateOf(context.getSharedPreferences("voicelike_prefs", Context.MODE_PRIVATE).getString("startup_last_greeting_key", "") ?: "") }
    
    // 闂?闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柤纰卞墻濡茬兘姊绘担鍛婃儓婵炲眰鍨藉畷鍦喆閸曨剙鍔呮繝鐢靛Т濞诧箓鎮″☉銏″€堕柣鎰邦杺閸ゆ瑩鏌涢弮鈧銊ф閹烘鍋愰柛鎰絻閹介潧螖閻橀潧浠﹂柣妤佹礉瑜颁礁顪冮妶鍡楀潑闁稿鎸搁埞鎴︽闁稿鍔楀Σ鎰板箳濡も偓閻掑灚銇勯幒宥囶槮缂佸墎鍋ら弻鐔兼焽閿曗偓楠炴﹢鏌涘鍡曠凹缂佺粯绻堥幃浠嬫濞磋翰鍨荤槐鎺旂磼濡浚浜垾鏍ㄧ節閸ヨ埖鏅╂繝娈垮枟閸旀帞鑺辨繝姘叆婵犲﹤鍠氶悡鑲┾偓瑙勬磸閸旀垿銆佸☉姗嗙叆闁归偊鍠栫花銉╂⒒閸屾艾鈧绮堟笟鈧獮鏍敃閿曗偓绾惧湱鎲搁悧鍫濅刊闁轰礁瀚换娑㈠幢濡纰嶇紒鐐劤濞硷繝鎮￠锕€鐐婇柕濠忓閿涙洟姊虹粙娆惧剱闁规悂绠栭獮澶愬箻椤旇偐顦板銈嗗坊閸嬫挾鐥弶璺ㄐ㈤柍瑙勫灴閸┿儵宕卞鍓у嚬缂傚倷娴囬褔宕愰崸妤€绠栭柟顖嗗懏娈濋梻渚囧弿闂勫嫰宕撻棃娑辨富闁靛牆妫楅悘鍗炩攽椤斿搫鈧繂顕ｉ鍕劦妞ゆ帒瀚埛鎺懨归敐鍫燁仩闁靛棗锕ユ穱濠囨偑濞嗗繑鎼愰柣銈囧亾缁绘繃绻濋崒娑樻缂備讲鍋撳┑鐘叉川閸欐捇鏌涢妷锝呭闁愁垱娲熼弻宥夋煥鐎ｎ亞浼岄梺鍝勬湰缁嬫垿鍩ユ径濠庢建闁割偅绻傞～鐘测攽閻橆偅濯伴悘鐐舵閳峰姊洪崫鍕効缂佹彃鈧喓浜欏┑鐐存尰閼规儳煤閿曞倹鍋ù锝呭濞撳鏌曢崼婵嗘殭闁诲浚浜濈换娑㈡嚑椤掆偓閺嬫稒顨ラ悙璇ц含闁诡喗鐟╁畷顐﹀礋鎼存稑濮傞柡灞诲姂瀵噣鍩€椤掑倵鍋撶粭娑樻搐閼哥懓顭块懜闈涘闁抽攱鍨块弻锝夋偄閸涘﹦鍑￠梺璇″枟閸ㄥ潡寮婚敐澶嬫櫜閹煎瓨绻堥崑妤呮倵鐟欏嫭绀堥柟铏尵閸欏懎顪冮妶鍛闁瑰啿閰ｉ崺鈧い鎺嗗亾闁诲繑宀告俊鐢稿礋椤栨氨鐫勯梺閫炲苯澧寸€规洜鎳撻埞鎴﹀醇閻斿嘲绨ラ梻浣虹《閸撴繄婀佺紓浣筋嚙濡繈寮婚弴銏犻唶婵犻潧妫岄崑鎾诲川婵犲嫷娲告俊銈忕到閸燁垶鍩涢幋鐘电＝濞达絿娅㈡笟娑欑箾閸噦鑰块柡灞剧〒閳ь剨缍嗛崜娆撳几鎼淬劌纭€闂侇剙绉甸悡娆撴⒑椤撱劎鐣遍柛妯绘尦閺岋繝鍩€椤掍胶顩烽悗锝庡亞閸樺崬顪冮妶鍡楀闁稿﹥娲熷鍛婃償閵婏妇鍘卞┑鈽嗗灠閸氬宕甸埀顒€顪冮妶搴濈盎闁哥喎鐡ㄦ穱濠囧醇閺囩偛鑰垮┑顔筋殔濡瑩鍩涢弽顓熺厽閹兼番鍊ゅ鎰箾閸欏鐭掔€殿噮鍋婂畷顐﹀Ψ閵夈儳浜板┑鐘垫暩婵數鍠婂澶婂惞鐎广儱顦伴悡蹇擃熆鐠団€崇仩闁瑰嘲顑夊畷顖涙償閵婏腹鎷绘繛杈剧秬濞咃綁濡存繝鍥ㄧ厱闁规儳顕粻鐐碘偓瑙勬礃缁诲牆顕ｉ幘顔碱潊闁炽儲鏋奸崑鎾诲醇閺囩偟楠囬梺鍓插亝绾板秴鈻嶉崶鈺冩／闁诡垎浣镐划闂佽鍠栫紞濠傜暦閹偊妲奸梺宕囩帛濞茬喖寮诲☉娆愬劅闁靛繆妾ч幏濠氭⒑闁偛鑻晶鍙夈亜椤愩埄妲搁悡銈夋煃閸濆嫬鏆炵€规洖寮剁换婵嬫濞戝崬鍓伴梺鍝勵儎閼冲爼鍩€椤掆偓缁犲秹宕曢柆宓ュ洭顢涘鍛劶闁诲函缍嗛崑浣圭濠婂牊鐓欏ù鐓庣摠濞懷兠瑰鍕畺闁逛究鍔戦弫鎰板川椤栨埃鍙烘繝娈垮枛閿曪妇鍒掗鐐茬闁告稒娼欐导鐘绘煕閺囥劌浜楃紒顕呭灣缁辨挻鎷呯粙搴撴寖闂佹儳绻愰柊锝呯暦閿濆绀冩い顓熷灥濞堛劑姊洪崫鍕潶闁稿孩妞藉顐﹀礃椤旂晫鍙嗛梺鍝勫暙濞诧箓藟婢跺⊕褰掓偐濞嗗繐鏆婇柡鈧禒瀣厽闁归偊鍘界紞鎴︽煟韫囨洖鏋涢柡灞剧洴婵℃悂濡烽敃浣侯攨婵犵數鍋為崹褰掑极婵犳艾鏄ラ柍褜鍓氶妵鍕箳閹存繍浠鹃梺鎶芥敱閸ㄥ潡寮婚悢纰辨晬闁糕剝顨呴顐ｃ亜閳衡偓閸楀啿顫忛搹鍦煓婵炲棙鍎抽崜浼存⒑绾懏鐝柨鏇樺灩閻ｅ嘲煤椤忓嫷娼婇梺闈涚墕閹虫劙鎯堥崟顖涒拺闁告挻褰冩禍鐐烘煕閻樿櫕宕岀€?
    var startupWelcomeCopy by remember { 
        val sharedPrefs = context.getSharedPreferences("voicelike_prefs", Context.MODE_PRIVATE)
        val nextSeq = sharedPrefs.getInt("startup_greeting_sequence", 0) + 1
        val lastKey = sharedPrefs.getString("startup_last_greeting_key", "") ?: ""
        val copy = buildStartupWelcomeCopy(
            context = StartupGreetingContext(
                language = currentLanguage,
                recentPhotoCount14d = 0,
                consecutiveShootingDays = 0,
                pendingPhotoCount = 0,
                processedCount = prefs.stats.processed,
                savedSizeBytes = prefs.stats.savedSize
            ),
            variationIndex = nextSeq,
            lastGreetingKey = lastKey,
            strings = strings
        )
        mutableStateOf(copy)
    }

    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧潡鏌熺€电孝缂佽翰鍊濋弻锕€螣娓氼垱锛嗗┑鐐叉▕娴滄繈寮插鍫熺厽闁逛即娼ф晶顕€骞栭弶鎴含婵﹨娅ｇ划娆戞崉閵娧傜礃闂備胶顭堥鍥磻閵堝懐鏆﹂柟杈剧畱鍥存繝銏ｆ硾椤戝洭宕㈤鍛瘈闁汇垽娼ф禒婊堟煟韫囨梻绠炵€规洘绻傝灃闁告侗鍠氶崢鎼佹煟鎼搭垳绉靛ù婊庝邯瀹曪綁宕熼娑氬幐闁诲繒鍋熼崑鎾剁矆婢跺瞼纾奸柣妯虹－濞叉潙鈹戦敍鍕効妞わ附濞婇弻娑氣偓锝嗘煥閹虫劗澹曢挊澹濆綊鏁愰崨顔藉創濡炪倕瀛╅幐鍓ф閹炬剚鍚嬮柛鏇ㄥ幘閻涖垽鎮楀▓鍨灈妞ゎ厾鍏樺顐﹀箻缂佹ê浜归梺鑲┾拡閸撱劎妲愰崣澶岀瘈婵炲牆鐏濋弸鐔兼煥閺囨娅婄€规洏鍨虹缓浠嬪级閸℃鍠橀柛鈹惧墲缁楃喖宕堕妸銉фВ闂傚倷绶氬褔鎮ч崱娑樼柈妞ゆ劧闄勯崐鍫曟煃閸濆嫬鏆熺痪鍓у帶椤法鎹勯悮鏉戝缂備礁顦遍崕銈夈€冮妷鈺傚€烽柛娆忣樈濡箓鎮楃憴鍕闁挎洏鍨介妴浣糕枎閹炬潙娈愰梺鍐叉惈閿曘儲鏅ュ┑鐘殿暜缁辨洟宕戦幋锕€纾归柡宥庡幖缁€澶愭煙鏉堝墽鐣辩痪鎹愵潐娣囧﹪濡堕崨顔兼闂侀€炲苯澧柛濠傜仢椤曪絾绻濆顑┿劑鏌ㄩ弮鍥舵綈閻庢艾銈稿缁樻媴閸涘﹤鏆堢紓浣割儐閸ㄥ潡寮崘顔嘉ч柛銉㈡櫇閻ｈ泛鈹戦鏂や緵闁告顫夌粋宥咁煥閸喓鍘繝鐢靛仜閻忔繈宕濆鍫熺厱闁靛牆鍊告禍楣冩⒒閸屾瑧绐旀繛浣冲厾娲晝閸屾氨顦┑鐐叉缁绘帡寮抽敂鍓х＜閻庯綆鍙庨崵锕傛煛閸愶絽浜鹃梺鐟板槻閹虫劙骞夐幘顔肩妞ゆ挾鍋涚紓鎾绘⒒閸屾艾鈧悂宕愭搴ｇ焼濞达絽婀遍悵璺衡攽閻樺弶鎼愮痪鎯ь煼閺屾稑鈽夐崡鐐典画閻庤娲橀悡锟犲蓟濞戙垹绠涢梻鍫熺⊕閻忔捇姊洪崨濠勬噭婵炲弶锕㈡俊鐢稿礋椤栨氨顔婇梺鐟扮摠缁诲秵绂掓ィ鍐┾拺缂備焦蓱鐏忕敻鏌涢悩鍐插濠碉紕鏁诲畷鐔碱敍閿濆棙娅囨俊鐐€栭崝鎴﹀垂瑜版帪缍栭柍鍝勫€荤弧鈧紒鍓у鑿ら柛瀣崌閹崇娀顢楅崒婊冨箚闂傚倷绀侀幖顐﹀嫉椤掑嫭鍎庢い鏍仜閺勩儵鏌ㄩ悢鍝勑㈢痪鎯у悑缁绘繃绻濋崒姘间患闂佹悶鍔戞禍璺侯潖缂佹ɑ濯寸紒娑橆儏濞堟劙姊洪崫銉バｆい銊ワ躬楠炲啴鎮滈挊澶屽幐闂佸憡渚楅崢楣冩偩濞差亝鈷戠紓浣姑悘杈ㄤ繆椤愩垹顏柣銉海椤﹀綊鏌″畝鈧崰鎾跺垝濞嗘挸绠伴幖娣灩闂傤垱绻?闂傚倸鍊搁崐宄懊归崶顒夋晪鐟滃繘鍩€椤掍胶鈻撻柡鍛█閵嗕礁鈻庨幘鍐插敤濡炪倖鎸鹃崑鐔兼偘閵夆晜鈷戦柛锔诲幖閸斿銇勯妸銉﹀櫧濠㈣娲樼换婵嗩潩椤撶姴骞嶉梻浣告啞閹稿棝宕ㄩ鐙€鍋ч梻?闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫇闁逞屽墴閹潡顢欐慨鎰盎闂佸搫绉查崝搴ㄥ疮閺屻儲鐓犻柡澶嬪閸嬨儵鏌＄仦鍓ф创闁轰礁鍊婚幏鐘诲箵閹烘棏鍟堥梺璇叉唉椤煤閺嵮屽殨闁割偅娲栫粻鐐烘煏婵炲灝鍓婚柣鏃傚劋鐎氭氨鈧懓澹婇崰妤€鈻撳ú顏呪拻濞达絼璀﹂悞鐐亜閹存繃顥㈡鐐村姍楠炴牗鎷呴崫銉ュ绩濠电姰鍨煎▔娑滃闂佸搫顑呯€涒晝鎹㈠☉銏犲耿婵°倕鍟伴鍥偡濠婂嫭绶叉俊顐ｇ〒濡?
    LaunchedEffect(hasLoadedOnce) {
        if (hasLoadedOnce && startupWelcomeCopy == null) {
            val sharedPrefs = context.getSharedPreferences("voicelike_prefs", Context.MODE_PRIVATE)
            val nextSeq = startupGreetingSequence + 1
            val copy = buildStartupWelcomeCopy(
                context = StartupGreetingContext(
                    language = currentLanguage,
                    recentPhotoCount14d = 0,
                    consecutiveShootingDays = 0,
                    pendingPhotoCount = 0,
                    processedCount = stats.processed,
                    savedSizeBytes = stats.savedSize
                ),
                variationIndex = nextSeq,
                lastGreetingKey = startupLastGreetingKey,
                strings = strings
            )
            startupWelcomeCopy = copy
            startupGreetingSequence = nextSeq
            startupLastGreetingKey = copy.key
            sharedPrefs.edit().putInt("startup_greeting_sequence", nextSeq).putString("startup_last_greeting_key", copy.key).apply()
        }
    }
    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆缂備焦蓱濞呭牓姊绘担鍛婂暈濞撴碍顨婂畷褰掝敂閸繄锛涢梺鍦亾閸撴艾顭囬埡鍌樹簻闁规壋鏅涢悘鈺冪磼娓氬洤娅嶆慨濠呮閸栨牠寮撮悢鍛婄番闂備胶顭堥鍡涘箰閹间讲鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔村姂濮婅櫣绮欓崠鈥冲濡炪値鍘奸悧鎾诲箖濞差亶鏁囬柕蹇婃閹风粯绻涙潏鍓хК妞ゎ偄顦靛畷鎴︽偐缂佹鍘辨繝鐢靛Т閸熸壆绮婚弽顐ょ＜闁稿本姘ㄨ倴闂佸湱鎳撶€氼厾绮悢纰辨晬婵﹩鍙庨崥娆忊攽閿涘嫬浜奸柛濠冪墵瀹曟繆顦寸紒顔碱煼楠炲鎮╅崹顐础闁荤喐绮嶅Λ鍐嚕鐠囨祴妲堥柍鍨涙杹閸嬫捇鏁冮崒姘鳖吅濠电娀娼уú銊︾閹€鏀介柣鎰▕閸ょ喎鈹戦锝呭籍鐎规洩缍佸畷姗€顢欓懖鈺冩毇婵犲痉鏉库偓鏇㈠箠韫囨稑纾归柣鎴ｅГ閻撶喖鏌ｉ弮鍌ょ劸妞ゃ儲鍔欓弻娑氣偓锝庡亝瀹曞瞼鈧鍠楅幐铏叏閳ь剟鏌ｅΟ娲诲晱闁哥喎娲︽穱濠囨倷椤忓嫧鍋撻弽顓炵鐟滃繒鍒掓繝姘闁绘ê鍚€缁楀姊哄Ч鍥х仼闁硅绻濋幃锟犲礃椤旂晫鍘卞┑鐘绘涧濡顢旈鍫熺厱闁挎繂妫欓妵婵嬫煛鐏炲墽銆掗柍褜鍓ㄧ紞鍡樼濠婂牆鐒垫い鎺嗗亾妞ゎ厾鍏橀悰顔藉緞瀹€鈧惌娆撳箹鐎涙ɑ灏伴柡鍌楀亾闂傚倷绀佹竟濠囧磻閸℃稑闂柕澶堝労閻掍粙鏌ｉ弬璺ㄦ闁衡偓娴犲鐓熸俊顖濆亹鐢稒绻涢幊宄板娴滄粓鏌熺€涙绠栨い銉ｅ灲閺岀喖顢欓幆褌娌紓浣芥〃缁瑥鐣烽妸锔剧瘈闁告洦鍋勭粻鐐测攽閿涘嫬浜奸柛濞垮€濆畷銏＄鐎ｎ亜鐎梺姹囧灮椤牓宕欓悩宕囩闁瑰瓨鐟ラ悘顏堟煕鐎ｃ劌濡介柕鍥у瀵粙濡歌濡插牓姊哄畷鍥╁笡婵炲弶绮撻崺鈧い鎺嶇贰閸熷繘鏌涢悩宕囧⒌闁诡喓鍎茬缓鐣岀矙閹稿孩袣闂備焦鏋奸弲娑㈠疮閹殿喖濮柍褜鍓熷铏圭矙閹稿孩鎷卞┑顔角滈崝鎴︺€佸▎鎰瘈闁搞儯鍔夐幏缁樼箾鏉堝墽鍒伴柟璇х節楠炲骞栨担鍦幈闁瑰吋鐣崹瑙勬叏瀹ュ洠鍋撶憴鍕缂侇喗鎹囬獮鍐閿涘嫰妾繝銏ｆ硾閿曘儵顢橀幐搴濈箚闁绘劦浜滈埀顒佺墪鐓ょ€广儱顦壕濠氭煙閹呬邯闁稿鎸鹃幉鎾礋椤掑偆妲扮紓鍌欑贰閸犳牜绮旇ぐ鎺斿祦闊洦绋戝婵嬫煛婢跺鐏︾€规挸绉撮—鍐Χ閸℃ê鏆楅梺鍝ュУ閸旀瑩銆佸▎鎺旂杸闁圭虎鍨遍弬鈧梺鍦劋婵炲﹤鐣烽幇顑╂梹鎷呴悷鏉夸紟闂備胶绮崹鐓幬涢崟顓烆棜濠电姵纰嶉悡鐔镐繆椤栨繂鍚归柕鍫熸尦閺岋紕鈧綆鍋嗘晶顒傜磼缂佹娲存鐐差儔閹瑩宕ｆ径灞剧€繝鐢靛仜閻°劎鍒掑畝鈧槐鐐寸節閸パ嗘憰濠电偞鍨堕惌顔尖柦椤忓牊鐓曢柟鐐仦鐠佹煡宕戦幘璇茬婵°倓鑳堕崢鎾绘⒑閼姐倕鏋戝鐟版閻楀酣姊绘笟鈧褍煤閵堝洠鍋撳顓熺凡妞ゆ洩缍侀、姘跺焵椤掆偓閻ｇ兘宕奸弴妞诲亾閺嶎収鏁婇柤鎭掑劚绾炬娊姊?
    val pendingTrashIdsSnapshot = remember(pendingTrashCount) { 
        java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
            addAll(prefs.pendingTrashIds)
        }
    }
    val insightsMedia = allMedia
    val organizeRangeCriteria = buildFilterCriteria()
    // Photos left in the current range, whether the range holds any photo at all, and the newest few
    // of them for the photo wall entry; null while unknown.
    val organizeRangeSummary by produceState<Triple<Int, Boolean, List<MediaItem>>?>(
        initialValue = null,
        organizeRangeCriteria,
        insightsMedia,
        stats.processed,
        pendingTrashCount,
        hiddenFolders
    ) {
        if (insightsMedia.isEmpty()) return@produceState
        val criteriaSnapshot = organizeRangeCriteria
        val pendingTrashSnapshot = HashSet(prefs.pendingTrashIds)
        val processedSnapshot = HashSet(prefs.processedIds)
        val hiddenSnapshot = effectiveHiddenFolders(hiddenFolders.toSet(), criteriaSnapshot)
        value = withContext(Dispatchers.Default) {
            val active = selectActiveCatalogMedia(insightsMedia, pendingTrashSnapshot, hiddenSnapshot)
            val inRange = applyMediaFilter(active, criteriaSnapshot)
            Triple(
                inRange.count { it.id !in processedSnapshot },
                inRange.isNotEmpty(),
                newestPhotoWallPreview(inRange, processedSnapshot)
            )
        }
    }
    val organizeRangeRemainingCount = organizeRangeSummary?.first
    // Startup loads only a cache or preview page of the library, so pending ids missing from it
    // may still exist: the queue is only pruned against the full catalog, never a partial one.
    fun pendingTrashForCurrentCatalog(): Set<Long> = if (fullCatalogLoaded) {
        normalizePendingTrashIds(prefs.pendingTrashIds, insightsMedia)
    } else {
        HashSet<Long>().apply { addAll(prefs.pendingTrashIds) }
    }
    val effectivePendingTrashIds = remember(fullCatalogLoaded, pendingTrashCount, insightsMedia) {
        pendingTrashForCurrentCatalog()
    }
    LaunchedEffect(hasLoadedOnce, fullCatalogLoaded, insightsMedia, effectivePendingTrashIds) {
        if (!hasLoadedOnce || !fullCatalogLoaded) {
            val persistedPendingCount = prefs.pendingTrashIds.size
            if (pendingTrashCount != persistedPendingCount) {
                pendingTrashCount = persistedPendingCount
            }
            return@LaunchedEffect
        }
        if (effectivePendingTrashIds.size != prefs.pendingTrashIds.size) {
            prefs.pendingTrashIds = effectivePendingTrashIds.toMutableSet()
        }
        val accuratePendingTrashCount = effectivePendingTrashIds.size
        if (pendingTrashCount != accuratePendingTrashCount) {
            pendingTrashCount = accuratePendingTrashCount
        }
    }
    // 闂傚倸鍊搁崐鎼佸磹閹间礁纾圭€瑰嫭鍣磋ぐ鎺戠倞妞ゆ帒锕︾粙蹇旂節閵忥絽鐓愰柛鏃撶畵瀵偅绻濋崶銊у幐閻庡箍鍎辨鎼佺嵁閺嶎偆妫柟顖嗗瞼鍚嬮梺鍝勬湰缁嬫垿鍩ユ径鎰闁绘劖褰冮婊堟⒒娴ｈ鍋犻柛濠冪墪鐓ら柣鏃傚帶閽冪喐绻涢幋鐐电煠婵¤尙鍘ч—鍐Χ閸愩劌顬堝┑鈽嗗亝缁诲倿鎮鹃悜钘夌闁挎洍鍋撻幆鐔兼⒑閸愬弶鎯堥柛濠傤煼瀹曘垽顢旈崨顖滅槇闂佹眹鍨藉褔鍩€椤掑鐏︾€规洘锕㈤垾锕傚箣濠婂懐锛涢梻鍌氬€风粈渚€骞栭鈶芥稑鈽夊▎鎰伎闂佹眹鍨归悘姘暦閸欏绡€闂傚牊绋掗ˉ鎴︽煛鐎ｎ偅顥堥柡灞剧洴閳ワ箓骞嬪┑鍥╀壕濠电偛顕刊顓㈠矗閸愵喖钃熼柨婵嗩槸椤懘鏌嶆潪鎷屽厡濞寸姭鏅犲娲箰鎼淬垻鈹涙繝纰樷偓铏悙閸楅亶鏌熼幍顔碱暭闁稿﹪顥撻惀顏堟偋閸╊偒浜濈粋宥嗐偅閸愨晝鍙嗗┑鐘绘涧濡繈顢撳Δ鍛厸閻庯綆鍋嗛妴鎺旂磼鏉堛劌娴柛鈹惧亾濡炪倖甯掔€氼剟鐛姀鈥茬箚妞ゆ牗纰嶇拹锛勭磼閻樺啿鍝烘慨濠冩そ濡啫鈽夊顒夋毇闂備胶鎳撻崯鍧楁煀閿濆鈧線寮崼婢劑鏌嶉崫鍕偓濠氬储閹剧粯鍋℃繝濠傚閻帞鈧娲樼划宀勫煝鎼淬劌绠荤€规洖娲﹂悞楣冩⒒娴ｅ憡璐￠柧蹇撻叄瀹曚即寮介鐐插殤婵炶揪缍€閵嗏偓闁稿鎹囬悰顕€宕归鐓庮潛闂備胶鎳撻崲鏌ヮ敄閸涚补鈧箓宕堕鈧悡娑㈡煕鐏炰箙鑲╄姳婵犳碍鈷戦悷娆忓椤ユ劙鏌￠崨顔炬创闁糕斁鍋撳銈嗗笂缁垛€斥枔濠婂嫷娈介柣鎰▕濡插ジ鏌曢崶銊ュ妤犵偞甯￠獮姗€顢涘☉姗嗕紪闂傚倸鍊风欢姘焽閼姐倐鍋撻棃娑氱劯鐎规洏鍨虹缓鐣岀矙鐠侯煈妲烽梺璇插嚱缂嶅棝宕板Δ鍛；闁靛璐熸禍婊堟煛瀹ュ啫濡稿ù鐘崇矒閺屾稓鈧綆鍋呭畷宀€鈧鍣崜鐔镐繆閻戣姤鏅滈梺娆惧灠娴滅偓绻涘顔荤凹闁绘挻鐟╅弻銈吤圭€ｎ偅鐝楅柦妯煎枎閳规垿鍩勯崘銊хシ闂佺娅曢敋闁伙絿鍏樺Λ鍐归搹顐ｎ棃鐎规洏鍔戦、娆撳箚瑜嶇粻鐐测攽閻樺灚鏆╅柛瀣仱瀹曞綊妫冨☉姘濡炪倖甯掔€氼喖鐣垫笟鈧弻鈥愁吋鎼粹€崇闂佺粯鎸鹃崰鏍偂椤愶箑鐐婄憸婵嬪绩缂佹绠鹃柛娑卞弿閺€濠氭煙娓氬灝濡界紒缁樼箞瀹曟帡濡堕崶褎鍊梺璇叉唉椤煤濠婂牆绐楅柡宥庡幖閺勩儵鏌嶉埡浣告殲濠殿垱鎸抽弻鐔虹磼濡櫣鐟愭繝銏ｎ潐濞叉牠鍩為幋锔藉亹缂備焦蓱闁款厼顪冮妶鍡楃仴闁硅櫕鎸鹃幑銏狀潩閼哥數鍘介柟鑹版彧缁辨洟鎮鹃銏＄厱閹兼番鍔嬮幉楣冩寠濠靛枹褰掑礂閸忚偐绋囬梺姹囧€楅崑鎾诲Φ閸曨垰绠抽柟瀛樼妇閸嬫捇鎮界粙璺紱闂佽宕橀褏绮堥崒姘肩唵閻犺櫣灏ㄥ妤€顭跨憴鍕婵﹦绮幏鍛村川婵犲倹娈樻繝鐢靛仩椤曟粓姊介崟顓犵焿鐎广儱顦伴崐鐑芥煕椤垵浜芥繛鑲╁枎閳规垿顢欑粵瀣姼闂佺硶鏅滈悧鐘诲春濞戙垹绠ｉ柣妯兼暩閿涙粓鏌ｆ惔顖滅У闁告鏅☉鐢稿礈娴ｈ櫣锛滈梺缁樏崯鍧椝夌€ｎ剛纾奸弶鍫涘妼缁楁氨鈧灚婢樼€氼喗绂掗敃鍌涘€锋い鎺嶈兌濡叉姊婚崒娆愮グ妞ゆ洘鐗犲畷褰掝敍濠婂嫷鍋ㄥ┑顔筋焾濞夋盯宕归崒姣兼棃鏁愰崨顓熸濠碘槅鍋呴敃銏ゅ蓟閺囩喎绶炴繛鎴烇供濞差參姊洪柅鐐茶嫰婢ь喗銇勯鐐叉Щ妞ゎ偄绻愮叅妞ゅ繐瀚ˉ婵嬫⒑闂堟侗妾ч梻鍕閹敻寮崼鐔叉嫼闂佸憡绺块崕閬嶅几瀹ュ鐓曟俊銈勭閳绘洟鏌ｅ☉鍗炴灕缂佺姵绋戦埥澶娢熷┃鐐珚闁哄苯绉瑰畷顐﹀礋椤愮喎浜惧┑鐘宠壘閸屻劑姊洪鈧粔鐢稿煕閹达附鐓曟繝闈涙椤忊晠鏌嶈閸忔稓鍒掑▎鎾扁偓浣割潩閹颁焦歇闂備線鈧稓鈹掗柛鏃€鍨甸悾鐑藉箳濡や礁鈧兘鎮楅悽娈跨劸妤犵偛鐗撳缁樼節鎼粹€茬盎濠电偠顕滅粻鎴ｇ亱闂佺鎻梽鍕吹閳ь剟姊虹粔鍡楀濞堟洟鏌ｉ幘瀵告创闁诡喗锕㈤幃娆徝圭€ｎ亙澹曢梺缁樻煥椤ㄥ孩瀵奸崘鈺€绻嗛柣鎰典簻閳ь剚鐗犻獮鎰板箹娴ｈ倽褏绱掔€ｎ厽纭堕柡鍡畵閺屾稑螖閸愩劉鍋撻敐澶嬪亜闁稿繒鍘ф禍妤呮煙閼圭増褰х紒杈ㄦ礋閹兘濮€閵堝棌鎷婚梺绋挎湰閼归箖鍩€椤掆偓閸㈡煡婀侀梺鎼炲労閸撱劎绱為弽褜鐔嗛柤鎼佹涧婵箓鏌℃担闈╄含闁哄备鈧剚鍚嬮幖绮光偓宕囶啇缂備胶鍋撻崕鎶解€﹂悜钘夎摕婵炴垶菤濡插牓鏌涘Δ鍐ㄤ粶閻庢冻缍佸?
    // UI State
    var showStatsOverlay by remember { mutableStateOf(false) }
    var showShootingStats by remember { mutableStateOf(false) }
    var shootingStatsMediaSnapshot by remember { mutableStateOf<List<MediaItem>?>(null) }
    var shootingStatsLikedSnapshot by remember { mutableStateOf<Set<Long>?>(null) }
    var shootingStatsWarmupSignature by remember { mutableLongStateOf(-1L) }
    var shootingStatsWarmupCandidates by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showDeleteModal by remember { mutableStateOf(false) }
    var showRecycleBin by remember { mutableStateOf(false) }

    fun openDeleteQueue() {
        showDeletionQueueReminder = false
        if (pendingTrashCount <= 0) {
            showDeleteModal = false
            return
        }
        scope.launch {
            showDeleteModal = false
            androidx.compose.runtime.withFrameNanos { }
            showDeleteModal = true
        }
    }

    var showDeepOrganize by remember { mutableStateOf(false) } // New state
    var showSimilarPhotos by remember { mutableStateOf(false) }
    var showLowQuality by remember { mutableStateOf(false) } // New state
    var showPhotoWall by remember { mutableStateOf(false) }
    // A photo was thrown or brought back on the wall: the home deck is rebuilt when it closes.
    var photoWallChanged by remember { mutableStateOf(false) }
    var showLikes by remember { mutableStateOf(false) } // New state for Likes view
    var showKeepGallery by remember { mutableStateOf(false) }
    var showWatermarkFrame by remember { mutableStateOf(false) }
    var pendingWatermarkBackdropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var watermarkFrameOpenSessionToken by rememberSaveable { mutableStateOf(0L) }
    var resetWatermarkEditorForLegacyEntry by rememberSaveable { mutableStateOf(false) }
    var pendingModernWatermarkPresetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingModernWatermarkInitialImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingModernWatermarkPickerPresetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorCards by remember { mutableStateOf(false) }
    var pendingModernColorCardFilterPaletteId by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorCardCamera by remember { mutableStateOf(false) }
    var showStampCamera by remember { mutableStateOf(false) }
    var showPromptManager by remember { mutableStateOf(false) }
    var showFillLight by remember { mutableStateOf(false) }
    var colorCardPageHandlesBackInternally by remember { mutableStateOf(false) }
    var showMonthly by remember { mutableStateOf(false) } // New state for Monthly view
    var showDailyCalendar by remember { mutableStateOf(false) } // New state for Daily Calendar view
    var showLanguage by remember { mutableStateOf(false) } // New state for Language view
    var showSwipePreferences by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) } // New state for Settings view
    var showReorganize by remember { mutableStateOf(false) }

    fun replacePendingWatermarkBackdropBitmap(next: Bitmap?) {
        val previous = pendingWatermarkBackdropBitmap
        pendingWatermarkBackdropBitmap = next
        if (previous != null && previous !== next && !previous.isRecycled) {
            previous.recycle()
        }
    }

    fun openWatermarkFramePage(
        presetKey: String? = null,
        initialImageUriString: String? = null,
        pickerPresetKey: String? = null
    ) {
        showLikes = false
        watermarkFrameOpenSessionToken += 1L
        resetWatermarkEditorForLegacyEntry = presetKey == null &&
            initialImageUriString == null &&
            pickerPresetKey == null
        pendingModernWatermarkPresetKey = presetKey
        pendingModernWatermarkInitialImageUri = initialImageUriString
        pendingModernWatermarkPickerPresetKey = pickerPresetKey
        val backdropRootView = context.findActivity()?.window?.decorView?.rootView
        replacePendingWatermarkBackdropBitmap(
            backdropRootView?.let(::captureHomeCardLongPressBackdropBitmapFallback)
        )
        showWatermarkFrame = true
    }
    var settingsDestination by remember { mutableStateOf(SettingsDestination.Home) }
    var returnToModernStartupWelcomeAfterSubpage by rememberSaveable { mutableStateOf(false) }
    var showLab by remember { mutableStateOf(false) }
    var showLabGlobe by remember { mutableStateOf(false) }
    var showLabAlbumSettings by remember { mutableStateOf(false) }
    var showLabCopyrightVerify by remember { mutableStateOf(false) }
    var showLabLiveSyncTool by remember { mutableStateOf(false) }
    var showCampfirePlan by remember { mutableStateOf(false) }
    var campfireOpenVersion by remember { mutableStateOf(0) }
    var showHelpCenter by remember { mutableStateOf(false) }
    var showElectronicAlbumPlayback by remember { mutableStateOf(false) }
    var electronicAlbumStandaloneEntry by rememberSaveable { mutableStateOf(false) }
    var electronicAlbumOpenedFromColorCards by rememberSaveable { mutableStateOf(false) }
    var pendingQueueOrderChange by remember { mutableStateOf(false) } // Pending queue order change to apply when returning to main
    if (BuildConfig.CAMPFIRE_PAY_ENABLED) {
        LaunchedEffect(Unit) {
            CampfirePaymentReturnCenter.events.collect {
                campfireOpenVersion += 1
                showCampfirePlan = true
            }
        }
    }
    var pendingBatchSizeChange by remember { mutableStateOf(false) } // Pending batch size change to apply when returning to main
    var showOpenSourceLicenses by remember { mutableStateOf(false) } // New state for Open Source Licenses
    var showLensyInfo by remember { mutableStateOf(false) } // Lensy info dialog
    LaunchedEffect(lensyAvailable) {
        if (!lensyAvailable) {
            showLensyInfo = false
        }
    }
    var showFilter by remember { mutableStateOf(false) } // New state for Filter view
    var organizedToday by remember { mutableIntStateOf(prefs.organizedTodayCount) }
    var startOrganizeFromHomeAfterRangeSignal by remember { mutableIntStateOf(0) }
    var moveFeedbackMessage by remember { mutableStateOf<String?>(null) } // Custom feedback state
    var showVideoCompress by remember { mutableStateOf(false) }
    var showImageCompress by remember { mutableStateOf(false) }
    fun closeElectronicAlbumFlow() {
        val closingColorCardAlbum = electronicAlbumOpenedFromColorCards
        showElectronicAlbumPlayback = false
        showLabAlbumSettings = false
        electronicAlbumStandaloneEntry = false
        if (closingColorCardAlbum) {
            scope.launch {
                withFrameNanos { }
                electronicAlbumOpenedFromColorCards = false
            }
        } else {
            electronicAlbumOpenedFromColorCards = false
        }
    }
    fun openStandaloneElectronicAlbum(preserveColorCards: Boolean = false) {
        electronicAlbumStandaloneEntry = true
        electronicAlbumOpenedFromColorCards = preserveColorCards
        if (!preserveColorCards) {
            colorCardPageHandlesBackInternally = false
            showColorCards = false
            pendingModernColorCardFilterPaletteId = null
        }
        showElectronicAlbumPlayback = false
        showLab = false
        showLabGlobe = false
        showLabCopyrightVerify = false
        showLabLiveSyncTool = false
        showLabAlbumSettings = true
    }
    val requiresActiveCatalogMedia =
        showDeepOrganize ||
            showSimilarPhotos ||
            showLowQuality ||
            showVideoCompress ||
            showImageCompress ||
            showLab ||
            showLabGlobe ||
            showLabAlbumSettings ||
            showLabCopyrightVerify ||
            showLabLiveSyncTool ||
            showElectronicAlbumPlayback
    val activeCatalogMedia = remember(insightsMedia, hiddenFolders, pendingTrashIdsSnapshot, requiresActiveCatalogMedia) {
        if (!requiresActiveCatalogMedia) {
            emptyList()
        } else {
            selectActiveCatalogMedia(insightsMedia, pendingTrashIdsSnapshot, hiddenFolders)
        }
    }
    val compressionVideos = remember(activeCatalogMedia, showVideoCompress) {
        if (!showVideoCompress) emptyList() else selectVideoCompressionCandidates(activeCatalogMedia)
    }
    val compressionPhotos = remember(activeCatalogMedia, showImageCompress) {
        if (!showImageCompress) emptyList() else selectImageCompressionCandidates(activeCatalogMedia)
    }
    val deepOrganizeVideoWarmTargets = remember(activeCatalogMedia, showDeepOrganize, showVideoCompress) {
        if (!showDeepOrganize && !showVideoCompress) {
            emptyList()
        } else {
            selectVideoCompressionCandidates(activeCatalogMedia).take(32)
        }
    }
    LaunchedEffect(deepOrganizeVideoWarmTargets) {
        if (deepOrganizeVideoWarmTargets.isEmpty()) return@LaunchedEffect
        launch {
            MediaAnalysisManager.warmVideoThumbs(context, deepOrganizeVideoWarmTargets)
        }
        launch {
            VideoCompressionSessionCache.prewarm(context, deepOrganizeVideoWarmTargets, maxCount = 12)
        }
    }
    val isGlobeProgressPageVisible = showLabGlobe
    val insightsUiState = rememberInsightsUiState()
    // The Insights tab (or one of its pages) is on screen: it needs the catalog and drives the scan.
    val insightsVisible = insightsUiState.hasOverlay ||
        (showStartupWelcomeCard && modernStartupWelcomeTab == StartupWelcomeTab.Insights.name)
    val insightsCatalogMedia = remember(insightsMedia, hiddenFolders, pendingTrashIdsSnapshot, insightsVisible) {
        if (!insightsVisible) {
            emptyList()
        } else {
            selectActiveCatalogMedia(insightsMedia, pendingTrashIdsSnapshot, hiddenFolders)
        }
    }
    val insightsLikedIds = remember(insightsVisible, stats.processed) {
        if (insightsVisible) prefs.likedIds.map { it.toLong() }.toSet() else emptySet()
    }
    val catalogAnalysisDemandActive =
        insightsVisible ||
        showDeepOrganize ||
            isGlobeProgressPageVisible ||
            showShootingStats
    val catalogScanState by CatalogScanCoordinator.state.collectAsState()
    val isDeepOrganizeScanPaused = catalogScanState.isPaused
    val hasCatalogAnalysisSession =
        catalogScanState.isRunning ||
            (catalogScanState.overallProgress > 0f && catalogScanState.overallProgress < 0.999f)
    val keepCatalogAnalysisAlive = catalogAnalysisDemandActive || hasCatalogAnalysisSession
    val restoreCatalogMedia = remember(insightsMedia, hiddenFolders, hasVerifiedCatalog, keepCatalogAnalysisAlive) {
        if (!hasVerifiedCatalog || !keepCatalogAnalysisAlive) {
            emptyList()
        } else {
            selectActiveCatalogMedia(insightsMedia, emptySet(), hiddenFolders)
        }
    }
    val scanCatalogMedia = remember(insightsMedia, hiddenFolders, fullCatalogLoaded, keepCatalogAnalysisAlive) {
        if (!fullCatalogLoaded || !keepCatalogAnalysisAlive) {
            emptyList()
        } else {
            selectActiveCatalogMedia(insightsMedia, emptySet(), hiddenFolders)
        }
    }
    val coordinatorCatalogMedia = remember(scanCatalogMedia, restoreCatalogMedia) {
        if (scanCatalogMedia.isNotEmpty()) scanCatalogMedia else restoreCatalogMedia
    }
    val catalogScanSignature by produceState(
        initialValue = 0L,
        coordinatorCatalogMedia,
        hasVerifiedCatalog,
        fullCatalogLoaded,
        keepCatalogAnalysisAlive
    ) {
        if (!hasVerifiedCatalog || !keepCatalogAnalysisAlive || coordinatorCatalogMedia.isEmpty()) {
            value = 0L
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            buildMediaScanSignature(coordinatorCatalogMedia, 0)
        }
    }
    val deepOrganizeCatalogMedia = remember(
        activeCatalogMedia,
        scanCatalogMedia,
        fullCatalogLoaded,
        pendingTrashIdsSnapshot,
        requiresActiveCatalogMedia
    ) {
        if (!requiresActiveCatalogMedia) {
            emptyList()
        } else {
            val baseMedia = if (fullCatalogLoaded && scanCatalogMedia.isNotEmpty()) {
                scanCatalogMedia
            } else {
                activeCatalogMedia
            }
            baseMedia.filterNot { pendingTrashIdsSnapshot.contains(it.id) }
        }
    }
    val allowCatalogBootstrap =
        fullCatalogLoaded &&
            catalogAnalysisDemandActive &&
            !pendingStartupCatalogHydration &&
            !startupCatalogHydrationRunning
    val catalogBootstrapDeferMs = resolveCatalogBootstrapDelayMs(
        demandActive = catalogAnalysisDemandActive
    )
    // StatsOverlay stays mounted behind some destinations. Those keep-alive pages should not inherit its pause.
    val isStatsOverlayBlockingAnalysis =
        showStatsOverlay &&
            !showShootingStats &&
            !keepCatalogAnalysisAlive
    val allowCatalogBackgroundWork = keepCatalogAnalysisAlive
    var rescanVisibleProcessedBaselineIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val processedIdsForCatalogDisplay = remember(stats.processed) {
        HashSet<Long>().apply { addAll(prefs.processedIds) }
    }
    val catalogDisplayIgnoredIds = remember(
        pendingTrashCount,
        processedIdsForCatalogDisplay,
        rescanVisibleProcessedBaselineIds
    ) {
        HashSet<Long>().apply {
            processedIdsForCatalogDisplay.forEach { id ->
                if (!rescanVisibleProcessedBaselineIds.contains(id)) {
                    add(id)
                }
            }
            addAll(prefs.pendingTrashIds)
        }
    }
    var labSlideshowSource by remember {
        mutableStateOf(
            if (prefs.labSlideshowSource == "folder") LabSlideshowSource.Folder else LabSlideshowSource.Favorites
        )
    }
    var labPlaybackMode by remember {
        mutableStateOf(
            if (prefs.labPlaybackMode == "fullscreen") LabPlaybackMode.FullscreenSlideshow else LabPlaybackMode.HomeCard
        )
    }
    var labSlideshowFolder by remember { mutableStateOf(prefs.labSlideshowFolder) }
    var labSlideshowIntervalSec by remember { mutableIntStateOf(prefs.labSlideshowIntervalSec) }
    var labSlideshowDurationEnabled by remember { mutableStateOf(prefs.labSlideshowDurationEnabled) }
    var labSlideshowShuffleEnabled by remember { mutableStateOf(prefs.labSlideshowShuffleEnabled) }
    var labFullscreenLandscape by remember { mutableStateOf(prefs.labFullscreenLandscape) }
    var labFullscreenDisplayMode by remember {
        mutableStateOf(electronicAlbumDisplayModeFromPreference(prefs.labFullscreenDisplayMode))
    }
    var labWholeViewBlurBackgroundEnabled by remember {
        mutableStateOf(prefs.labWholeViewBlurBackgroundEnabled)
    }
    var labSlideshowDurationMin by remember { mutableIntStateOf(prefs.labSlideshowDurationMin) }
    var labAdvancedTextureEnabled by remember { mutableStateOf(false) }
    var playbackQueue by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var playbackMode by remember { mutableStateOf(labPlaybackMode) }
    var playbackForceLandscapeInFullscreen by remember { mutableStateOf(labFullscreenLandscape) }
    var playbackDisplayMode by remember { mutableStateOf(labFullscreenDisplayMode) }
    var playbackWholeViewBlurBackgroundEnabled by remember {
        mutableStateOf(labWholeViewBlurBackgroundEnabled)
    }
    var playbackIntervalMs by remember { mutableLongStateOf(3000L) }
    var playbackDurationMs by remember { mutableStateOf<Long?>(null) }
    var recycleBinItems by remember { mutableStateOf<List<RecycleBinItem>>(emptyList()) }
    var pendingRestoreIds by remember { mutableStateOf<Set<Long>?>(null) }
    var pendingRestoreRemainingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingPermanentDeleteIds by remember { mutableStateOf<Set<Long>?>(null) }
    var pendingPermanentDeleteRemainingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var restoreLaunchToken by remember { mutableIntStateOf(0) }
    var permanentDeleteLaunchToken by remember { mutableIntStateOf(0) }

    fun doSyncCatalogAfterMediaRemoval(removedIds: Set<Long>) {
        if (removedIds.isEmpty()) return
        mediaTotalCount = (mediaTotalCount - removedIds.size).coerceAtLeast(0)
        MediaCatalogSnapshotCache.schedulePersist(context, allMedia)
    }

    fun doExtendFeedWindowIfNeeded(
        baseSnapshot: HomeFeedSnapshot,
        criteria: MediaFilterCriteria,
        processedIds: Set<Long>,
        swipeDirection: SwipeDirection
    ): HomeFeedSnapshot {
        // Keep the faster up-swipe handoff, but do not leak future-batch cards into the
        // visible stack. Batch completion still needs to hit the settlement card exactly
        // at the user-configured batch size.
        return baseSnapshot
    }

    fun resetBatchProgress(total: Int) {
        currentBatchTotal = total.coerceAtLeast(0)
        currentBatchConsumed = 0
    }

    fun advanceBatchProgressAfterSwipe(remainingPendingCount: Int) {
        val activeBatchTotal = currentBatchTotal.coerceAtLeast(0)
        if (activeBatchTotal <= 0) {
            resetBatchProgress(remainingPendingCount.coerceAtMost(organizeQueueLimit(batchSize, buildFilterCriteria())))
            return
        }

        val consumedAfterSwipe = (currentBatchConsumed + 1).coerceAtMost(activeBatchTotal)
        if (consumedAfterSwipe >= activeBatchTotal) {
            resetBatchProgress(remainingPendingCount.coerceAtMost(organizeQueueLimit(batchSize, buildFilterCriteria())))
        } else {
            currentBatchConsumed = consumedAfterSwipe
        }
    }

    // Combo System
    var comboCount by remember { mutableIntStateOf(0) }
    var lastActionTime by remember { mutableLongStateOf(0L) }
    
    // Reset combo if idle (3 seconds)
    LaunchedEffect(comboCount, lastActionTime) {
        if (comboCount > 0) {
            delay(3000)
            if (System.currentTimeMillis() - lastActionTime >= 3000) {
                comboCount = 0
            }
        }
    }

    // Sync stats when returning from compression views which update prefs directly
    LaunchedEffect(showVideoCompress, showImageCompress) {
        if (!showVideoCompress && !showImageCompress) {
            prefs.reloadStats()
            stats = prefs.stats
        }
    }

    LaunchedEffect(suppressStatsHapticsOnce) {
        if (suppressStatsHapticsOnce) {
            delay(1600)
            suppressStatsHapticsOnce = false
        }
    }

    LaunchedEffect(showRecycleBin) {
        if (showRecycleBin) {
            // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆缂備焦蓱濞呭牓姊绘担鍛婂暈濞撴碍顨婂畷褰掝敂閸繄锛涢梺鍦亾閸撴艾顭囬埡鍌樹簻闁规壋鏅涢悘鈺冪磼娓氬洤娅嶆慨濠呮閸栨牠寮撮悢鍛婄番闂備胶顭堥鍡涘箰閹间讲鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔村姂濮婅櫣绮欓崠鈥冲濡炪値鍘奸悧鎾诲箖濞差亶鏁囬柕蹇婃閹风粯绻涙潏鍓хК妞ゎ偄顦靛畷鎴︽偐缂佹鍘辨繝鐢靛Т閸熸壆绮婚弽顐ょ＜闁稿本姘ㄨ倴闂佸湱鎳撶€氼厾绮悢纰辨晬婵﹩鍙庨崥娆忊攽閿涘嫬浜奸柛濠冪墵瀹曟繆顦寸紒顔碱煼楠炲鎮╅崹顐础闁荤喐绮嶅Λ鍐嚕鐠囨祴妲堥柍鍨涙杹閸嬫捇鏁冮崒姘鳖吅濠电娀娼уú銊︾閹€鏀介柣鎰▕閸ょ喎鈹戦锝呭籍鐎规洩缍佸畷姗€顢欓懖鈺冩毇婵犲痉鏉库偓鏇㈠箠韫囨稑纾归柣鎴ｅГ閻撶喖鏌ｉ弮鍌ょ劸妞ゃ儲鍔欓弻娑氣偓锝庡亝瀹曞瞼鈧鍠楅幐铏叏閳ь剟鏌ｅΟ娲诲晱闁哥喎娲︽穱濠囨倷椤忓嫧鍋撻弽顓炵鐟滃繒鍒掓繝姘闁绘ê鍚€缁楀姊哄Ч鍥х仼闁硅绻濋幃锟犲礃椤旂晫鍘卞┑鐘绘涧濡顢旈鍫熺厱闁挎繂妫欓妵婵嬫煛鐏炲墽銆掗柍褜鍓ㄧ紞鍡樼濠婂牆鐒垫い鎺嗗亾妞ゎ厾鍏橀悰顔藉緞瀹€鈧惌娆撳箹鐎涙ɑ灏伴柡鍌楀亾闂傚倷绀佹竟濠囧磻閸℃稑闂柕澶堝労閻掍粙鏌ｉ弬璺ㄦ闁衡偓娴犲鐓熸俊顖濆亹鐢稒绻涢幊宄板娴滄粓鏌熺€涙绠栨い銉ｅ灲閺岀喖顢欓幆褌娌紓浣芥〃缁瑥鐣烽妸锔剧瘈闁告洦鍋勭粻鐐测攽閿涘嫬浜奸柛濞垮€濆畷銏＄鐎ｎ亜鐎梺姹囧灮椤牓宕欓悩宕囩闁瑰瓨鐟ラ悘顏堟煕鐎ｃ劌濡介柕鍥у瀵粙濡歌濡插牓姊哄畷鍥╁笡婵炲弶绮撻崺鈧い鎺嶇贰閸熷繘鏌涢悩宕囧⒌闁诡喓鍎茬缓鐣岀矙閹稿孩袣闂備焦鏋奸弲娑㈠疮閹殿喖濮柍褜鍓熷铏圭矙閹稿孩鎷卞┑顔角滈崝鎴︺€佸▎鎰瘈闁搞儯鍔夐幏缁樼箾鏉堝墽鍒伴柟璇х節楠炲骞栨担鍦幈闁瑰吋鐣崹瑙勬叏瀹ュ洠鍋撶憴鍕缂侇喗鎹囬獮鍐閿涘嫰妾繝銏ｆ硾閿曘儵顢橀幐搴濈箚闁绘劦浜滈埀顒佺墪鐓ょ€广儱顦壕濠氭煙閹呬邯闁稿鎸鹃幉鎾礋椤掑偆妲扮紓鍌欑贰閸犳牜绮旇ぐ鎺斿祦闊洦绋戝婵嬫煛婢跺鐏︾€规挸绉撮—鍐Χ閸℃ê鏆楅梺鍝ュУ閸旀瑩銆佸▎鎺旂杸闁圭虎鍨遍弬鈧梺鍦劋婵炲﹤鐣烽幇顑╂梹鎷呴悷鏉夸紟闂備胶绮崹鐓幬涢崟顓烆棜濠电姵纰嶉悡鐔镐繆椤栨繂鍚归柕鍫熸尦閺岋紕鈧綆鍋嗘晶顒傜磼缂佹娲存鐐差儔閹瑩宕ｆ径灞剧€繝鐢靛仜閻°劎鍒掑畝鈧槐鐐寸節閸パ嗘憰濠电偞鍨堕惌顔尖柦椤忓牊鐓曢柟鐐仦鐠佹煡宕戦幘璇茬婵°倓鑳堕崢鎾绘⒑閼姐倕鏋戝鐟版閻楀酣姊绘笟鈧褍煤閵堝洠鍋撳顓熺凡妞ゆ洩缍侀、姘跺焵椤掆偓閻ｇ兘宕奸弴妞诲亾閺嶎収鏁婇柤鎭掑劚绾炬娊姊?
            val infoSnapshot = java.util.concurrent.ConcurrentHashMap(prefs.appTrashInfo)
            val result = withContext(Dispatchers.IO) {
                loadRecycleBinItems(context, infoSnapshot)
            }
            val items = result.first
            val staleIds = result.second
            if (staleIds.isNotEmpty()) {
                val updated = prefs.appTrashInfo
                // 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾剧懓顪冪€ｎ亜顒㈡い鎰矙閺屻劑鎮㈤崫鍕戙垻绱掗埀顒勫醇閻旇櫣顔曢梺绯曞墲钃遍悘蹇庡嵆閺屾稒绻濋崨顓犲帿缂備胶绮换鍫澪涢崘銊㈡闁告鍋涙竟鍫ユ⒒娴ｄ警鐒鹃悶姘煎亰瀹曟繈骞嬮敃鈧拑鐔兼煛閸ャ儱鐏╅梺鍗炴处缁绘繈宕归銏狀潔缂佹鍨垮缁樻媴閸涘﹨纭€闂佺娅曢幑鍥箖閵夛妇闄勭紒瀣劵閹芥洟姊洪崫鍕窛闁哥姵鎸剧划缁樸偅閸愨晝鍘卞銈庡幗閸ㄧ敻寮稿☉娆嶄簻妞ゆ巻鍋撻柣妤佹尭椤繐煤椤忓嫮顔愰梺缁樺姈缁佹挳宕戦幘璇茬疀闁哄娉曢崝锕€顪冮妶鍡楃瑨闁哥喐濯介埅鐢告煟閻斿摜鐭屽褎顨堥弫顔嘉旈崨顓狅紱闂佺鎻梽鍕偂閺囩喓绠鹃柟瀛樼箓閼稿綊鏌ｈ箛銉хМ闁归攱鍨跺蹇涘Ω瑜忛惁鍫㈢磼閻愵剚绶叉い锕佷含婢规洟宕稿Δ浣哄幈闁诲函缍嗛崑鍡椕虹€电硶鍋撶憴鍕闁告梹鐟ラ悾鐑芥倻缁涘鏅ｉ梺缁橆焽閺佹悂宕㈡导瀛樷拻闁稿本鐟х拹浼存煕閹惧鎳呯紒顔界懄缁楃喖鍩€椤掆偓椤曪綁顢曢敐鍥╃槇闂佹悶鍎崝搴ㄥ储妤ｅ啯鈷戦悹鍥ｂ偓鍐茬闁汇埄鍨辩敮鈥崇暦濠靛棭鐓ラ柛鏇ㄥ墮瀵?- 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙５闁逞屽墾缁犳挸鐣锋總绋款潊闁炽儱鍟跨花銉╂⒒娴ｇ瓔娼愬鐟版閺呰泛螖閸涱厾锛涢柣搴秵娴滄牠寮ㄦ禒瀣厽婵☆垰鎼痪褔鏌熼崗鐓庡闁哄矉绱曢埀顒婄岛閺呯偤宕楀畝鍕厵闁告瑥顦伴崐鎰版煙椤斻劌娲ら柋鍥ㄧ節闂堟稓澧㈤柟铏墵濮婄粯鎷呴悷鎵虫灆闂佽　鍋撻梺顒€绉甸崑瀣煟濡鍤欓柦鍐枑缁绘盯骞嬪▎蹇曚患闂佺粯甯楀浠嬪蓟濞戙垹绠涙い鏃堟？婢规洟姊烘导娆戝埌闁绘牜鍘ч～蹇撁洪鍕炊闂佸憡娲﹂崜娆忊枍閵堝鈷戦柤濮愬€曢弳閬嶆煛閸涱垰孝妞ゆ洩绲块幏鐘裁圭€ｎ偒娼旈梻渚€娼х换鎺撴叏閻戠瓔鏁婂┑鐘叉处閳锋垿鏌涘┑鍡楊仼闁逞屽墮閹芥粓鎳為柆宥嗙劶鐎广儱鎳庨崵鎴︽⒑閸涘﹣绶遍柛妯绘倐瀵?
                staleIds.forEach { id -> updated.remove(id) }
                prefs.appTrashInfo = updated
            }
            recycleBinItems = items
        }
    }

    var monthlyReviewList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    // Track if stats animation has played once in this session
    var hasPlayedStatsAnimation by rememberSaveable { mutableStateOf(false) }
    fun toggleDeepOrganizeScanPause() {
        if (isDeepOrganizeScanPaused) {
            CatalogScanCoordinator.resume()
        } else {
            CatalogScanCoordinator.pause()
        }
    }

    fun restartDeepOrganizeScan() {
        val historicalProcessedBaseline = HashSet<Long>().apply {
            addAll(prefs.processedIds)
        }
        rescanVisibleProcessedBaselineIds = historicalProcessedBaseline
        val rescanIgnoredIds = HashSet<Long>().apply {
            addAll(prefs.pendingTrashIds)
        }
        CatalogScanCoordinator.rescan(displayIgnoredIdsOverride = rescanIgnoredIds)
    }

    fun openShootingStatsPage() {
        if (showShootingStats) return
        val pendingIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
            addAll(prefs.pendingTrashIds)
        }
        val activeMediaSnapshot = insightsMedia.filter { !pendingIds.contains(it.id) }
        val likedSnapshot = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
            addAll(prefs.likedIds)
        }
        shootingStatsMediaSnapshot = activeMediaSnapshot
        shootingStatsLikedSnapshot = likedSnapshot
        showShootingStats = true
        if (!hasVerifiedCatalog) return
        val activeMediaWarmupSignature = buildMediaScanSignature(activeMediaSnapshot.take(1400), 0)
        scope.launch {
            val candidates = if (shootingStatsWarmupSignature == activeMediaWarmupSignature) {
                shootingStatsWarmupCandidates
            } else {
                val resolved = withContext(Dispatchers.Default) {
                    collectStatsWarmupCandidates(context, activeMediaSnapshot)
                }
                shootingStatsWarmupSignature = activeMediaWarmupSignature
                shootingStatsWarmupCandidates = resolved
                resolved
            }
            if (candidates.isNotEmpty()) {
                HomeScanScheduler.run(
                    taskKey = HomeScanScheduler.TASK_STATS_WARMUP,
                    minIntervalMs = 1200L
                ) {
                    MediaAnalysisManager.prefetchUnifiedInsights(
                        context = context,
                        items = candidates,
                        languageCode = currentLanguage.code,
                        includeKeywords = true,
                        maxCount = candidates.size,
                        timeBudgetMs = AppPerformanceGovernor.scaledDelay(2800L)
                    )
                }
            }
        }
    }

    LaunchedEffect(catalogScanSignature, coordinatorCatalogMedia, catalogDisplayIgnoredIds, hasVerifiedCatalog, keepCatalogAnalysisAlive) {
        if (!hasVerifiedCatalog || !keepCatalogAnalysisAlive) return@LaunchedEffect
        if (catalogScanSignature == 0L || coordinatorCatalogMedia.isEmpty()) return@LaunchedEffect
        CatalogScanCoordinator.bindCatalog(
            context = context,
            signature = catalogScanSignature,
            media = coordinatorCatalogMedia,
            displayIgnoredIds = catalogDisplayIgnoredIds
        )
    }
    LaunchedEffect(catalogAnalysisDemandActive, catalogScanSignature, showDeepOrganize, showLabGlobe, showShootingStats) {
        if (!catalogAnalysisDemandActive) return@LaunchedEffect
        if (catalogScanSignature == 0L || coordinatorCatalogMedia.isEmpty()) return@LaunchedEffect
        CatalogScanCoordinator.ensureForeground()
    }
    CatalogBackgroundAnalysisEffects(
        context = context,
        prefs = prefs,
        restoreCatalogMedia = restoreCatalogMedia,
        scanCatalogMedia = scanCatalogMedia,
        displayIgnoredIds = catalogDisplayIgnoredIds,
        catalogVerified = hasVerifiedCatalog,
        hasLoadedOnce = hasLoadedOnce,
        loading = loading,
        isInitialLoad = isInitialLoad,
        allowCatalogBackgroundWork = allowCatalogBackgroundWork,
        allowCatalogBootstrap = allowCatalogBootstrap,
        firstInteractiveAtMs = firstInteractiveAtMs,
        catalogBootstrapDeferMs = catalogBootstrapDeferMs
    )

    // Local state for Low Quality Display (Critical for Swipe Performance)
    var lowQualityDisplayItems by remember { mutableStateOf<List<Pair<MediaItem, String>>>(emptyList()) }
    /*
    
    // Sync Manager -> Local (Only Append)
    // When manager finds new items, we append them if they are not already processed/displayed.
    // But we must NOT overwrite local state if we just deleted something locally.
    // Strategy: 
    // 1. Initial Load: Copy all.
    // 2. Updates: If Manager list grows, append. If Manager list shrinks (due to our removal), ignore?
    // Actually, simpler: Initialize local state when entering view.
    // But scan is ongoing.
    
    // Correct Strategy:
    // Maintain a set of "Locally Removed IDs".
    // lowQualityDisplayItems = lowQualityItemsFromManager.filter { !locallyRemovedIds.contains(it.first.id) }
    // No, that's computed state, simpler.
    // But swiping needs to update UI immediately.
    // So:
    // lowQualityDisplayItems IS the source of truth for the UI.
    // We update it when:
    // 1. Swipe -> Remove item locally.
    // 2. Manager finds NEW items -> Append them.
    
    LaunchedEffect(lowQualityItemsFromManager) {
        // Simple merge: Add items from manager that are not in current display list AND not processed
        // But since we persist processed IDs, manager shouldn't return them anyway (except during race).
        // Let's just use the Manager list but filter out things we *just* swiped if Manager hasn't updated yet?
        // Actually, easiest way to fix "Ghosting":
        // Use a local MutableList.
        // When Manager emits, we replace the local list? No, that causes the jump back.
        
        // Let's try:
        // UI uses `lowQualityDisplayItems`.
        // `lowQualityDisplayItems` is initialized from `lowQualityItemsFromManager`.
        // But we need to keep them in sync.
        
        // Alternative: The issue is likely that `handleLowQualitySwipe` calls `LowQualityManager.removeResult`.
        // This triggers `_foundItems.value = ...`.
        // This triggers Flow emission.
        // This triggers Recomposition.
        // If this loop is slow, the card might flicker.
        
        // But "Card swiped then comes back" implies the Flow emitted the OLD list again?
        // Or the UI recomposed with the old list before the new one arrived.
        
        // Fix: Use a local state that we modify *immediately* on swipe.
        // And when Manager emits, we update our local state *carefully*.
        
        val currentIds = lowQualityDisplayItems.map { it.first.id }.toSet()
        val newItems = lowQualityItemsFromManager.filter {
            shouldAnalyzeAsStandardPhoto(it.first) && !currentIds.contains(it.first.id)
        }
        
        if (newItems.isNotEmpty()) {
             // Only append NEW items. Do not restore items that are missing from manager (because we deleted them).
             // But wait, if we delete locally, they are still in Manager for a split second.
             // Manager emits. It might still contain the item.
             // So we must filter out items that we know we processed.
             val processed = prefs.processedIds // This is source of truth
             val validNewItems = newItems.filter { !processed.contains(it.first.id) }
             
             if (validNewItems.isNotEmpty()) {
                 lowQualityDisplayItems = lowQualityDisplayItems + validNewItems
             }
        }
        
        // Also handle case where Manager was cleared (e.g. stopScan)
        if (lowQualityItemsFromManager.isEmpty() && !isScanningLowQuality) {
            // Maybe we should clear?
             // lowQualityDisplayItems = emptyList() 
             // Keep it if we are viewing results?
        }
    }
    
    // Initial Load / Reset
    LaunchedEffect(showLowQuality, hasVerifiedCatalog) {
        if (showLowQuality) {
            if (!hasVerifiedCatalog) {
                lowQualityDisplayItems = emptyList()
                return@LaunchedEffect
            }
            lowQualityDisplayItems = emptyList()
            
        } else {
            // Do NOT stop scan on close. Let it run in background.
            // LowQualityManager.stopScan() 
            
            // lowQualityDisplayItems = emptyList() // Keep for transition or clear?
            // If we clear, next open is blank until restore.
            // Let's clear to save memory if needed, but persistence handles restore.
            lowQualityDisplayItems = emptyList()
        }
    }
    
    // Persistence Debouncer
    LaunchedEffect(lowQualityItemsFromManager) {
        if (lowQualityItemsFromManager.isNotEmpty()) {
            // Legacy low-quality prefs cache path removed.
            
            delay(1000) // Debounce 1s
            
            // Save Snapshot - 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆缂備焦蓱濞呭牓姊绘担鍛婂暈濞撴碍顨婂畷褰掝敂閸繄锛涢梺鍦亾閸撴艾顭囬埡鍌樹簻闁规壋鏅涢悘鈺冪磼娓氬洤娅嶆慨濠呮閸栨牠寮撮悢鍛婄番闂備胶顭堥鍡涘箰閹间讲鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔村姂濮婅櫣绮欓崠鈥冲濡炪値鍘奸悧鎾诲箖濞差亶鏁囬柕蹇婃閹风粯绻涙潏鍓хК妞ゎ偄顦靛畷鎴︽偐缂佹鍘辨繝鐢靛Т閸熸壆绮婚弽顐ょ＜闁稿本姘ㄨ倴闂佸湱鎳撶€氼厾绮悢纰辨晬婵﹩鍙庨崥娆忊攽閿涘嫬浜奸柛濠冪墵瀹曟繆顦寸€垫澘锕ョ粋鎺斺偓锝呯仛閺咁亪鏌℃径濠勫闁哄懏绻勬竟鏇熺鐎ｎ偆鍘卞銈嗗姉婵挳鎮橀埡鍛柈闁告縿鍎崇壕钘壝归敐鍕煓闁告繆娅ｉ埀顒€鍘滈崑鎾剁磼鐎ｎ偒鍎ユ繛鍏肩墵閺屟嗙疀閹剧纭€闂佹椿鍘介悷鈺呭箖瑜版帒绠掗柟鐑樺灥椤顪冮妶鍡樼叆婵炲眰鍔庡Σ鎰板箻鐎涙ê顎撻梺鍛婃尭瀵埖鏅ラ梻鍌欒兌閹虫捇骞夐埄鍐濠电姴娲ㄥ畵浣糕攽閻樺弶澶勯柛瀣閺屾稖绠涢弴鐐蹭粯婵炲瓨绮嶇划鎾愁潖閾忓湱纾兼俊顖濐嚙绾板秴鈹戦悙宸殶闁稿繑锕㈤獮鍐晸閻樿尙顓洪梺鎸庢琚欓柟閿嬫そ濮婃椽鎳￠妶鍛€炬繝銏㈡嚀濡繈鐛Δ鍛亹闁汇垻鏁搁敍婊冾渻閵堝棙顥嗛柛瀣姈閺呫儵鏌ｆ惔銈庢綈婵炴祴鏅濈槐鐐哄焵椤掑倻纾奸柣妯虹－閳藉鏌嶉挊澶樻Ц闁宠閰ｉ獮瀣攽閸℃瑯鍞堕梻鍌氬€搁崐椋庢濮樿泛鐒垫い鎺戝€告禒婊堟煠濞茶鐏￠柡鍛埣楠炴﹢顢欓悾灞藉箥婵＄偑鍊栧褰掑磿閹惰棄鍌ㄩ悗娑欙供濞堜粙鏌ｉ幇顖氱毢閺佸牆顪冮妶鍐ㄧ仾闁荤啿鏅犻悰顔碱潨閳ь剙鐣烽妸鈺婃晬闁绘劘鎻槐顒勬⒒?
            // Final snapshot persistence now lives in LowQualityManager.
        }
    }
    
    */
    var undoStack by remember { mutableStateOf(listOf<UndoAction>()) } // Item + Action Type
    var batchProgressUndoStack by remember { mutableStateOf(listOf<BatchProgressSnapshot>()) }
    var homeCardStackUndoAnimationTargets by remember { mutableStateOf(listOf<UndoAnimationTarget>()) }
    var homeSnappingCarouselUndoAnimationTargets by remember { mutableStateOf(listOf<UndoAnimationTarget>()) }
    val isUndoAnimating = homeCardStackUndoAnimationTargets.isNotEmpty()
    data class LowQualityUndoEntry(
        val item: MediaItem,
        val reason: String,
        val action: SwipeAction,
        val likeAdded: Boolean,
        val direction: SwipeDirection
    )
    var lowQualityUndoStack by remember { mutableStateOf(listOf<LowQualityUndoEntry>()) }
    var lowQualityUndoAnimationTarget by remember { mutableStateOf<UndoAnimationTarget?>(null) }
    var similarUndoStack by remember { mutableStateOf(listOf<Triple<Set<Long>, Set<Long>, Long>>()) } // deletedIds, processedIds, deletedSize
    var similarUndoMediaStack by remember { mutableStateOf(listOf<List<MediaItem>>()) }
    var viewingItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var viewingInitialIndex by remember { mutableIntStateOf(0) }
    var viewingSourceBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var viewingSourceCardBoundsAtOpen by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var viewingSourceItemId by remember { mutableStateOf<Long?>(null) }
    var sharedDismissHiddenSourceItemId by remember { mutableStateOf<Long?>(null) }
    var fullScreenVideoItem by remember { mutableStateOf<MediaItem?>(null) }
    var fullScreenPlayer by remember { mutableStateOf<ExoPlayer?>(null) } // Player transferred from preview
    val homeInteractionPerfController = remember {
        HomeInteractionPerfController(
            scope = scope,
            imageLoader = context.imageLoader,
            isUndoAnimating = { isUndoAnimating },
            hasUndoAvailable = { undoStack.isNotEmpty() },
            isFullScreenVideoOpen = { fullScreenVideoItem != null }
        )
    }
    val statePersistenceWriter = remember { HomeStatePersistenceWriter(prefs) }
    val keepGalleryStore = remember(context) { KeepGalleryStore.get(context) }

    fun openKeepGallery() {
        showKeepGallery = true
        scope.launch {
            val keepIds = withContext(Dispatchers.Default) {
                buildKeepIdSnapshot(
                    processedIds = prefs.processedIds.toSet(),
                    likedIds = prefs.likedIds.toSet(),
                    pendingTrashIds = prefs.pendingTrashIds.toSet(),
                    movedIds = prefs.movedIds.toSet()
                )
            }
            keepGalleryStore.ensureSynced(keepIds)
        }
    }

    fun addKeepItem(item: MediaItem) {
        keepGalleryStore.upsert(item)
    }

    fun addKeepItems(
        ids: Set<Long>,
        mediaSource: List<MediaItem> = allMedia
    ) {
        if (ids.isEmpty()) return
        val resolvedItems = resolveMediaItemsByIds(mediaSource, ids)
        if (resolvedItems.isNotEmpty()) {
            keepGalleryStore.upsertAll(resolvedItems)
        }
    }

    fun removeKeepIds(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        keepGalleryStore.removeIds(ids)
    }

    val pauseMainVideos = showStatsOverlay ||
        showShootingStats ||
        showDeleteModal ||
        showDeepOrganize ||
        showSimilarPhotos ||
        showLowQuality ||
        showPhotoWall ||
        showLikes ||
        showKeepGallery ||
        showWatermarkFrame ||
        showColorCards ||
        showColorCardCamera ||
        showStampCamera ||
        showPromptManager ||
        showFillLight ||
        showMonthly ||
        showLanguage ||
        showSwipePreferences ||
        showSettings ||
        showFilter ||
        showVideoCompress ||
        showImageCompress ||
        showCampfirePlan ||
        showHelpCenter ||
        monthlyReviewList.isNotEmpty() ||
        viewingItems != null ||
        fullScreenVideoItem != null

    fun nextUndoAnimationToken(): Long {
        undoTokenSequence += 1L
        return undoTokenSequence
    }

    fun clearHomeUndoAnimationTargets() {
        homeCardStackUndoAnimationTargets = emptyList()
        homeSnappingCarouselUndoAnimationTargets = emptyList()
    }

    fun clearHomeSnappingCarouselUndoPage() {
        homeSnappingCarouselPendingUndoPage = null
        homeSnappingCarouselUndoReleaseJob?.cancel()
        homeSnappingCarouselUndoReleaseJob = null
        homeSnappingCarouselUndoInFlightToken = null
    }

    fun clearHomeSnappingCarouselDeleteReturnState() {
        homeSnappingCarouselDeleteReturnPage = null
        homeSnappingCarouselDeleteReturnItemId = null
        homeSnappingCarouselDeleteReturnToSettlement = false
        homeSnappingCarouselDeleteReturnArmed = false
    }

    fun resetHomeSnappingCarouselViewportToStart() {
        if (!backtrackModeEnabled) return
        homeSnappingCarouselCurrentPage = 0
        clearHomeSnappingCarouselUndoPage()
    }

    fun resetHomeSnappingCarouselBatchOrder(items: List<MediaItem>) {
        homeSnappingCarouselBatchOrderIds = items.map { it.id }
    }

    data class SnappingCarouselUndoPlacement(
        val display: List<MediaItem>,
        val restoredIndex: Int
    )

    fun placeSnappingCarouselUndoAtCurrentPosition(
        items: List<MediaItem>,
        restoredItemId: Long,
        preferredInsertIndex: Int
    ): SnappingCarouselUndoPlacement? {
        val restoredItem = items.find { it.id == restoredItemId } ?: return null
        val withoutRestored = items.filter { it.id != restoredItemId }
        val insertIndex = preferredInsertIndex.coerceIn(0, withoutRestored.size)
        val adjustedDisplay = buildList(withoutRestored.size + 1) {
            addAll(withoutRestored.take(insertIndex))
            add(restoredItem)
            addAll(withoutRestored.drop(insertIndex))
        }
        val restoredIndex = adjustedDisplay.indexOfFirst { it.id == restoredItemId }
        if (restoredIndex < 0) return null
        return SnappingCarouselUndoPlacement(
            display = adjustedDisplay,
            restoredIndex = restoredIndex
        )
    }

    fun enqueueHomeUndoAnimation(
        itemId: Long,
        direction: SwipeDirection,
        restoreOffsetY: Float = 0f,
        forSnappingCarousel: Boolean = false
    ): Long {
        val target = UndoAnimationTarget(
            itemId = itemId,
            direction = direction,
            token = nextUndoAnimationToken(),
            restoreOffsetY = restoreOffsetY
        )
        if (forSnappingCarousel) {
            homeSnappingCarouselUndoAnimationTargets =
                (homeSnappingCarouselUndoAnimationTargets.filterNot { it.itemId == itemId } + target).takeLast(4)
        } else {
            homeCardStackUndoAnimationTargets =
                (homeCardStackUndoAnimationTargets.filterNot { it.itemId == itemId } + target).takeLast(4)
        }
        scope.launch {
            delay(
                if (forSnappingCarousel) {
                    HOME_UNDO_ANIMATION_TARGET_RETENTION_MS
                } else {
                    UNDO_RESTORE_ANIMATION_DURATION_MS
                }
            )
            if (forSnappingCarousel) {
                homeSnappingCarouselUndoAnimationTargets =
                    homeSnappingCarouselUndoAnimationTargets.filterNot { it.token == target.token }
            } else {
                homeCardStackUndoAnimationTargets =
                    homeCardStackUndoAnimationTargets.filterNot { it.token == target.token }
            }
        }
        return target.token
    }

    // Back Handler
    val isRootState = viewingItems == null && 
                      fullScreenVideoItem == null &&
                      monthlyReviewList.isEmpty() && 
                      !showMonthly && 
                      !showLanguage && 
                      !showSwipePreferences &&
                      !showSettings && 
                      !showLab &&
                      !showLabGlobe &&
                      !showLabAlbumSettings &&
                      !showLabCopyrightVerify &&
                      !showLabLiveSyncTool &&
                      !showCampfirePlan &&
                      !showHelpCenter &&
                      !showElectronicAlbumPlayback &&
                      !showFilter && 
                      !showLikes &&
                      !showKeepGallery &&
                      !showWatermarkFrame &&
                      !showColorCards &&
                      !showColorCardCamera &&
                      !showStampCamera &&
                      !showFillLight &&
                      !showSimilarPhotos && 
                      !showLowQuality && 
                      !showPhotoWall && 
                      !showVideoCompress && 
                      !showImageCompress && 
                      !showDeepOrganize && 
                      !showStatsOverlay && 
                      !showShootingStats &&
                      !showDeleteModal &&
                      !showRecycleBin
    val shouldRestoreModernStartupWelcome = returnToModernStartupWelcomeAfterSubpage &&
        modernHomeNavigationEnabled &&
        !showStartupWelcomeCard &&
        isRootState &&
        !showDailyCalendar &&
        !showDeletionMode &&
        !showOpenSourceLicenses
    LaunchedEffect(
        shouldRestoreModernStartupWelcome,
        returnToModernStartupWelcomeAfterSubpage,
        modernHomeNavigationEnabled
    ) {
        if (!returnToModernStartupWelcomeAfterSubpage) return@LaunchedEffect
        if (!modernHomeNavigationEnabled) {
            returnToModernStartupWelcomeAfterSubpage = false
            return@LaunchedEffect
        }
        if (shouldRestoreModernStartupWelcome) {
            returnToModernStartupWelcomeAfterSubpage = false
            showStartupWelcomeCard = true
        }
    }
    val allowHomeWarmups = isRootState && !showOpenSourceLicenses
    val shouldPauseMediaAnalysis = viewingItems != null ||
        fullScreenVideoItem != null ||
        monthlyReviewList.isNotEmpty() ||
        isStatsOverlayBlockingAnalysis ||
        showDeleteModal ||
        showRecycleBin ||
        showLikes ||
        showKeepGallery ||
        showWatermarkFrame ||
        showColorCards ||
        showColorCardCamera ||
        showStampCamera ||
        showFillLight ||
        showMonthly ||
        showDailyCalendar ||
        showVideoCompress ||
        showImageCompress ||
        showCampfirePlan ||
        showHelpCenter ||
        showOpenSourceLicenses ||
        showElectronicAlbumPlayback

    DisposableEffect(shouldPauseMediaAnalysis) {
        if (shouldPauseMediaAnalysis) {
            MediaAnalysisManager.requestPause()
        }
        onDispose {
            if (shouldPauseMediaAnalysis) {
                MediaAnalysisManager.releasePause()
            }
        }
    }

    // We only need the global BackHandler for states NOT wrapped in PredictiveBackWrapper
    // Currently, all major sub-pages are wrapped.
    // Remaining states that might need handling:
    // - viewingItems (FullScreenImageViewer) -> Not wrapped yet
    // - showStatsOverlay -> Not wrapped yet
    // - showDeleteModal -> Not wrapped yet
    
    BackHandler(enabled = !isRootState) {
        when {
            showDeleteModal -> showDeleteModal = false
            fullScreenVideoItem != null -> {
                fullScreenVideoItem = null
                fullScreenPlayer = null
                sharedDismissHiddenSourceItemId = null
            }
            viewingItems != null -> {
                viewingItems = null
                viewingSourceBounds = null
                viewingSourceCardBoundsAtOpen = null
                viewingSourceItemId = null
                sharedDismissHiddenSourceItemId = null
            }
            // monthlyReviewList & showMonthly are handled by wrapper
            // showLanguage, showSettings, showFilter handled by wrapper
            // showLikes handled by wrapper
            // showDeepOrganize & sub-features handled by wrapper
            showElectronicAlbumPlayback -> {
                if (electronicAlbumStandaloneEntry) closeElectronicAlbumFlow()
                else showElectronicAlbumPlayback = false
            }
            showPromptManager -> showPromptManager = false
            showFillLight -> showFillLight = false
            showLabLiveSyncTool -> showLabLiveSyncTool = false
            showLabCopyrightVerify -> showLabCopyrightVerify = false
            showLabAlbumSettings -> {
                if (electronicAlbumStandaloneEntry) closeElectronicAlbumFlow()
                else showLabAlbumSettings = false
            }
            showLabGlobe -> showLabGlobe = false
            showLab -> showLab = false
            showCampfirePlan -> showCampfirePlan = false
            showHelpCenter -> showHelpCenter = false
            showStatsOverlay -> showStatsOverlay = false
            showRecycleBin -> showRecycleBin = false
        }
    }

    LaunchedEffect(firstInteractiveAtMs, isInitialLoad, loading) {
        if (firstInteractiveAtMs == 0L || isInitialLoad || loading) return@LaunchedEffect
        // Keep the visual stack deep enough for smooth promotion during swipes.
        // Home-card heavy analysis is now limited separately inside the stack renderer.
        stagedHeavyWorkCards = 2
        delay(2000L)
        stagedHeavyWorkCards = 3
        delay(3000L)
        stagedHeavyWorkCards = 4
    }

    LaunchedEffect(pendingTrashCount, isRootState, showDeleteModal, showRecycleBin) {
        val currentMilestone = deletionQueueMilestone(pendingTrashCount)
        if (currentMilestone < lastDeletionQueueMilestoneShown) {
            // Queue shrank; allow future reminders when it grows again.
            lastDeletionQueueMilestoneShown = currentMilestone
            prefs.deletionQueueReminderAckMilestone = currentMilestone
            hasShownReminderThisSession = false
        }
        if (!isRootState || showDeleteModal || showRecycleBin) return@LaunchedEffect
        if (currentMilestone > 0 && currentMilestone > lastDeletionQueueMilestoneShown && !hasShownReminderThisSession) {
            deletionQueueReminderCount = currentMilestone
            showDeletionQueueReminder = true
            hasShownReminderThisSession = true
        }
    }

    if (showDeletionQueueReminder) {
        val newPageStrings = getNewPageStrings(currentLanguage)
        val reminderTitle = newPageStrings.deleteQueueReminderTitle
        val reminderText = newPageStrings.deleteQueueReminderBody(deletionQueueReminderCount)
        fun dismissDeletionQueueReminder(openDeleteModal: Boolean) {
            val ackMilestone = deletionQueueMilestone(pendingTrashCount)
            if (ackMilestone > lastDeletionQueueMilestoneShown) {
                lastDeletionQueueMilestoneShown = ackMilestone
                prefs.deletionQueueReminderAckMilestone = ackMilestone
            }
            showDeletionQueueReminder = false
            hasShownReminderThisSession = false
            if (openDeleteModal) {
                openDeleteQueue()
            }
        }

        AlertDialog(
            onDismissRequest = { dismissDeletionQueueReminder(openDeleteModal = false) },
            title = { Text(reminderTitle) },
            text = {
                Text(reminderText)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dismissDeletionQueueReminder(openDeleteModal = true)
                    }
                ) {
                    Text(strings.viewTrash)
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissDeletionQueueReminder(openDeleteModal = false) }) {
                    Text(strings.cancel)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    // Pending Move Args for Retry
    var pendingMoveArgs by remember { mutableStateOf<Triple<List<MediaItem>, String, String?>?>(null) }
    var pendingMoveUiAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingMoveCommitOnGrant by remember { mutableStateOf(false) }

    // Intent Sender for File Operations (Android 10/11+)
    val intentSenderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        fun clearPendingMoveState() {
            pendingMoveArgs = null
            pendingMoveUiAction = null
            pendingMoveCommitOnGrant = false
        }

        if (result.resultCode == Activity.RESULT_OK) {
             // User granted permission. Retry operation if pending.
             pendingMoveArgs?.let { (items, folder, existingAlbumId) ->
                 val commitOnGrant = pendingMoveCommitOnGrant
                 scope.launch {
                     // Retry move
                     val retryResult = runCatching { FileOperationManager.moveMedia(context, items, folder) }
                     val retrySender = retryResult.getOrNull()
                     val retryError = retryResult.exceptionOrNull()
                     val shouldCommitUi = (retrySender == null) || commitOnGrant

                     if (shouldCommitUi) {
                         pendingMoveUiAction?.invoke()
                         clearPendingMoveState()
                         Toast.makeText(context, "Moved ${items.size} items to $folder", Toast.LENGTH_SHORT).show()
                     } else {
                         clearPendingMoveState()
                         if (retryError != null) {
                             AppRuntimeLogStore.logError(
                                 context,
                                 "MovePermission",
                                 "Retry move failed for ${items.size} item(s) to $folder",
                                 retryError
                             )
                         }
                         // Still needs extra permission or the move could not be completed.
                         Toast.makeText(context, "Permission still required or operation failed.", Toast.LENGTH_SHORT).show()
                     }
                 }
             } ?: run {
                 Toast.makeText(context, "Permission granted.", Toast.LENGTH_SHORT).show()
             }
        } else {
            // Permission denied or cancelled
            clearPendingMoveState()
            Toast.makeText(context, "Permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    // Batch processing state
    var trashBatchIds by remember { mutableStateOf<Set<Long>?>(null) }
    var trashProcessingIds by remember { mutableStateOf<Set<Long>?>(null) }
    var onTrashBatchComplete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var trashBatchMode by remember { mutableStateOf("recycle_bin") }

    fun pruneHomeUndoStateForRemoved(ids: Set<Long>) {
        if (ids.isEmpty()) return
        val retainedUndoEntries = undoStack.mapIndexed { index, undoAction ->
            undoAction to batchProgressUndoStack.getOrElse(index) {
                BatchProgressSnapshot(
                    total = currentBatchTotal,
                    consumed = currentBatchConsumed
                )
            }
        }.filterNot { (undoAction, _) ->
            ids.contains(undoAction.item.id)
        }
        undoStack = retainedUndoEntries.map { it.first }
        batchProgressUndoStack = retainedUndoEntries.map { it.second }
        homeCardStackUndoAnimationTargets =
            homeCardStackUndoAnimationTargets.filterNot { target -> ids.contains(target.itemId) }
        homeSnappingCarouselUndoAnimationTargets =
            homeSnappingCarouselUndoAnimationTargets.filterNot { target -> ids.contains(target.itemId) }
    }

    fun pruneLowQualityUndoForRemoved(ids: Set<Long>) {
        if (ids.isEmpty()) return
        lowQualityUndoStack = lowQualityUndoStack.filterNot { entry ->
            ids.contains(entry.item.id)
        }
        lowQualityDisplayItems = lowQualityDisplayItems.filterNot { entry ->
            ids.contains(entry.first.id)
        }
        ids.forEach { removedId -> LowQualityManager.removeResult(removedId) }
    }

    fun hasLowQualityUndoCandidate(itemId: Long): Boolean {
        return allMedia.any { it.id == itemId }
    }

    // Delete Launcher
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Confirmed delete
            val batch = trashBatchIds
            val currentPending = prefs.pendingTrashIds
            // After process death the batch state is gone; process nothing rather than the whole queue.
            // Rows the system already removed drop out of the queue on the next confirm.
            val activeProcessingIds = trashProcessingIds.orEmpty()
            val now = System.currentTimeMillis()
            val expiresAt = now + 30L * 24L * 60L * 60L * 1000L
            val appTrashInfo = prefs.appTrashInfo
            
            // If batch is active, only process batch items. Otherwise process the active confirmation set.
            val itemsToProcess = (batch ?: activeProcessingIds).toSet()
            
            if (trashBatchMode == "recycle_bin") {
                itemsToProcess.forEach { id ->
                    appTrashInfo[id] = expiresAt
                    val item = allMedia.find { it.id == id }
                    val liveVideoId = item?.livePhotoVideoId
                    if (liveVideoId != null) {
                        appTrashInfo[liveVideoId] = expiresAt
                    }
                }
                prefs.appTrashInfo = appTrashInfo
            } else {
                itemsToProcess.forEach { id ->
                    appTrashInfo.remove(id)
                    val item = allMedia.find { it.id == id }
                    val liveVideoId = item?.livePhotoVideoId
                    if (liveVideoId != null) {
                        appTrashInfo.remove(liveVideoId)
                    }
                }
                prefs.appTrashInfo = appTrashInfo
            }
            
            // Remove from allMedia (Local Update) to prevent them from reappearing on Reset Filter
            val processingSet = itemsToProcess.toSet()
            if (processingSet.isNotEmpty()) {
                skipNextCatalogSync = true
                allMedia = removeMediaByIds(allMedia, processingSet)
                doSyncCatalogAfterMediaRemoval(processingSet)
                pruneLowQualityUndoForRemoved(processingSet)
                displayMedia = removeMediaByIds(displayMedia, processingSet)
                homeFeedCoordinator.alignCurrentDisplay(displayMedia)
            }
            // Remove processed items from pending list
            currentPending.removeAll(processingSet)
            prefs.pendingTrashIds = currentPending
            pendingTrashCount = currentPending.size

            if (currentPending.isEmpty()) {
                undoStack = emptyList()
                batchProgressUndoStack = emptyList()
                clearHomeUndoAnimationTargets()
                clearHomeSnappingCarouselUndoPage()
            } else {
                pruneHomeUndoStateForRemoved(processingSet)
            }

            val remainingProcessing = activeProcessingIds.filterNot { processingSet.contains(it) }.toSet()
            trashProcessingIds = remainingProcessing.takeIf { it.isNotEmpty() }

            // If the active confirmation still has more items, continue the next batch.
            if (remainingProcessing.isNotEmpty()) {
                // Next batch
                onTrashBatchComplete?.invoke()
            } else {
                // All done or single mode done
                trashBatchIds = null
                onTrashBatchComplete = null
                trashProcessingIds = null
            }
        } else {
            // Cancelled
            trashBatchIds = null
            onTrashBatchComplete = null
            trashProcessingIds = null
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val currentPending = pendingRestoreIds
        if (result.resultCode == Activity.RESULT_OK && currentPending != null) {
            suppressStatsHapticsOnce = true
            val appTrashInfo = prefs.appTrashInfo
            currentPending.forEach { appTrashInfo.remove(it) }
            prefs.appTrashInfo = appTrashInfo

            val processed = prefs.processedIds
            processed.removeAll(currentPending)
            prefs.processedIds = processed
            removeKeepIds(currentPending)

            val restoredItems = recycleBinItems.filter { currentPending.contains(it.media.id) }
            if (restoredItems.isNotEmpty()) {
                val restoredMedia = restoredItems.map { it.media }
                allMedia = mergeMediaIntoQueue(
                    currentMedia = allMedia,
                    additions = restoredMedia,
                    queueOrder = queueOrder,
                    randomOrderSeed = queueRandomOrderSeed
                )

                val restoredSize = sumMediaSize(restoredMedia)
                val newStats = stats.copy(
                    processed = (stats.processed - restoredItems.size).coerceAtLeast(0),
                    trashed = (stats.trashed - restoredItems.size).coerceAtLeast(0),
                    savedSize = (stats.savedSize - restoredSize).coerceAtLeast(0)
                )
                stats = newStats
                prefs.stats = newStats
            }

            recycleBinItems = recycleBinItems.filter { !currentPending.contains(it.media.id) }
        }
        if (result.resultCode == Activity.RESULT_OK && pendingRestoreRemainingIds.isNotEmpty()) {
            val nextBatchIds = pendingRestoreRemainingIds.take(MEDIASTORE_URI_BATCH_LIMIT).toSet()
            val nextBatchItems = recycleBinItems.filter { nextBatchIds.contains(it.media.id) }
            if (nextBatchItems.isNotEmpty()) {
                val nextIds = nextBatchItems.map { it.media.id }.toSet()
                pendingRestoreIds = nextIds
                pendingRestoreRemainingIds = pendingRestoreRemainingIds - nextIds
                restoreLaunchToken++
                return@rememberLauncherForActivityResult
            } else {
                pendingRestoreRemainingIds = pendingRestoreRemainingIds - nextBatchIds
            }
        }
        pendingRestoreIds = null
        pendingRestoreRemainingIds = emptySet()
    }

    val permanentDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val currentPending = pendingPermanentDeleteIds
        if (result.resultCode == Activity.RESULT_OK && currentPending != null) {
            val appTrashInfo = prefs.appTrashInfo
            currentPending.forEach { appTrashInfo.remove(it) }
            prefs.appTrashInfo = appTrashInfo

            val processed = prefs.processedIds
            processed.removeAll(currentPending)
            prefs.processedIds = processed
            removeKeepIds(currentPending)

            if (currentPending.isNotEmpty()) {
                skipNextCatalogSync = true
                allMedia = removeMediaByIds(allMedia, currentPending)
                doSyncCatalogAfterMediaRemoval(currentPending)
                pruneLowQualityUndoForRemoved(currentPending)
                displayMedia = removeMediaByIds(displayMedia, currentPending)
                homeFeedCoordinator.alignCurrentDisplay(displayMedia)
            }

            recycleBinItems = recycleBinItems.filter { !currentPending.contains(it.media.id) }
        }
        if (result.resultCode == Activity.RESULT_OK && pendingPermanentDeleteRemainingIds.isNotEmpty()) {
            val nextBatchIds = pendingPermanentDeleteRemainingIds.take(MEDIASTORE_URI_BATCH_LIMIT).toSet()
            val nextBatchItems = recycleBinItems.filter { nextBatchIds.contains(it.media.id) }
            if (nextBatchItems.isNotEmpty()) {
                val nextIds = nextBatchItems.map { it.media.id }.toSet()
                pendingPermanentDeleteIds = nextIds
                pendingPermanentDeleteRemainingIds = pendingPermanentDeleteRemainingIds - nextIds
                permanentDeleteLaunchToken++
                return@rememberLauncherForActivityResult
            } else {
                pendingPermanentDeleteRemainingIds = pendingPermanentDeleteRemainingIds - nextBatchIds
            }
        }
        pendingPermanentDeleteIds = null
        pendingPermanentDeleteRemainingIds = emptySet()
    }
    LaunchedEffect(restoreLaunchToken, pendingRestoreIds, recycleBinItems) {
        val current = pendingRestoreIds ?: return@LaunchedEffect
        if (current.isEmpty()) return@LaunchedEffect
        val uris = recycleBinItems.filter { current.contains(it.media.id) }.map { it.media.uri }
        if (uris.isEmpty()) {
            pendingRestoreIds = null
            pendingRestoreRemainingIds = emptySet()
            return@LaunchedEffect
        }
        try {
            val intentSender = MediaStore.createTrashRequest(context.contentResolver, uris, false).intentSender
            restoreLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        } catch (e: Exception) {
            e.printStackTrace()
            pendingRestoreIds = null
            pendingRestoreRemainingIds = emptySet()
        }
    }
    LaunchedEffect(permanentDeleteLaunchToken, pendingPermanentDeleteIds, recycleBinItems) {
        val current = pendingPermanentDeleteIds ?: return@LaunchedEffect
        if (current.isEmpty()) return@LaunchedEffect
        val uris = recycleBinItems.filter { current.contains(it.media.id) }.map { it.media.uri }
        if (uris.isEmpty()) {
            pendingPermanentDeleteIds = null
            pendingPermanentDeleteRemainingIds = emptySet()
            return@LaunchedEffect
        }
        try {
            val intentSender = MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            permanentDeleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        } catch (e: Exception) {
            e.printStackTrace()
            pendingPermanentDeleteIds = null
            pendingPermanentDeleteRemainingIds = emptySet()
        }
    }

    // Load Media
    LaunchedEffect(Unit) {
        val criteriaSnapshot = buildFilterCriteria()
        withContext(Dispatchers.IO) {
            var hasWarmCache = false
            val cached = MediaCatalogSnapshotCache.load(context)
                if (cached.isNotEmpty()) {
                    hasWarmCache = true
                    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆缂備焦蓱濞呭牓姊绘担鍛婂暈濞撴碍顨婂畷褰掝敂閸繄锛涢梺鍦亾閸撴艾顭囬埡鍌樹簻闁规壋鏅涢悘鈺冪磼娓氬洤娅嶆慨濠呮閸栨牠寮撮悢鍛婄番闂備胶顭堥鍡涘箰閹间讲鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔村姂濮婅櫣绮欓崠鈥冲濡炪値鍘奸悧鎾诲箖濞差亶鏁囬柕蹇婃閹风粯绻涙潏鍓хК妞ゎ偄顦靛畷鎴︽偐缂佹鍘辨繝鐢靛Т閸熸壆绮婚弽顐ょ＜闁稿本姘ㄨ倴闂佸湱鎳撶€氼厾绮悢纰辨晬婵﹩鍙庨崥娆忊攽閿涘嫬浜奸柛濠冪墵瀹曟繆顦寸紒顔碱煼楠炲鎮╅崹顐础闁荤喐绮嶅Λ鍐嚕鐠囨祴妲堥柍鍨涙杹閸嬫捇鏁冮崒姘鳖吅濠电娀娼уú銊︾閹€鏀介柣鎰▕閸ょ喎鈹戦锝呭籍鐎规洩缍佸畷姗€顢欓懖鈺冩毇婵犲痉鏉库偓鏇㈠箠韫囨稑纾归柣鎴ｅГ閻撶喖鏌ｉ弮鍌ょ劸妞ゃ儲鍔欓弻娑氣偓锝庡亝瀹曞瞼鈧鍠楅幐铏叏閳ь剟鏌ｅΟ娲诲晱闁哥喎娲︽穱濠囨倷椤忓嫧鍋撻弽顓炵鐟滃繒鍒掓繝姘闁绘ê鍚€缁楀姊哄Ч鍥х仼闁硅绻濋幃锟犲礃椤旂晫鍘卞┑鐘绘涧濡顢旈鍫熺厱闁挎繂妫欓妵婵嬫煛?
                    val processedSnapshot = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
                        addAll(prefs.processedIds)
                    }
                    val orderedCached = orderMediaForQueue(
                        media = cached,
                        queueOrder = prefs.queueOrder,
                        randomOrderSeed = queueRandomOrderSeed
                    )
                    val visibleCached = selectActiveCatalogMedia(orderedCached, java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
                        addAll(prefs.pendingTrashIds)
                    }, deckHiddenFolders())
                val cachedSnapshot = homeFeedCoordinator.rebuildFromAllMedia(
                    visibleCached,
                    criteriaSnapshot,
                    batchSize,
                    processedSnapshot
                )
                val cachedDisplayDistinct = cachedSnapshot.displayMedia.distinctBy { it.id }
                    withContext(Dispatchers.Main) {
                        allMedia = orderedCached
                        feedLoadedRows = orderedCached.size
                        mediaTotalCount = maxOf(mediaTotalCount, orderedCached.size)
                        displayMedia = cachedDisplayDistinct
                    playbackGateJob?.cancel()
                    playbackGateJob = null
                        topCardPlaybackArmed = cachedDisplayDistinct.isNotEmpty()
                        resetBatchProgress(cachedDisplayDistinct.size)
                        resetHomeSnappingCarouselBatchOrder(cachedDisplayDistinct)
                        batchProgressUndoStack = emptyList()
                        clearHomeUndoAnimationTargets()
                        clearHomeSnappingCarouselUndoPage()
                        pendingTrashCount = prefs.pendingTrashIds.size
                        // 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾剧懓顪冪€ｎ亝鎹ｉ柣顓炴閵嗘帒顫濋敐鍛闂佽姤蓱缁诲牓寮婚悢灏佹灁闁割煈鍠楅悘宥夋⒑閸濆嫭顥炵紒顔芥崌瀵鎮㈤崗鐓庘偓缁樹繆椤栨粌鍔﹂柟椋庢嚀椤啴濡舵惔鈥茬凹濠电偠灏欓崰鏍ь嚕婵犳碍鍋勯柛娑橈功缁夎埖淇婇妶蹇曞埌闁哥噥鍨堕、鎾愁吋婢跺鎷洪梺鍛婄箓鐎氼厽鍒婃總鍛婄厱闁圭偓娼欓崫鐑橆殽閻愯尙澧﹀┑鈩冩倐婵＄兘顢欓挊澶岀处闂傚倷鑳剁涵鍫曞礈濠靛鈧啴宕ㄦ潏鍓х◤濡炪倕绻愰悧濠囧煕閹达附鐓欓柤娴嬫櫅娴犳粓鏌嶈閸撴岸鎮у鍫濇瀬妞ゆ洍鍋撴鐐村笒铻栭柍褜鍓熼幃锟犲灳閹颁胶鍞甸梺鍏兼倐濞佳勬叏閸モ晝纾奸柣妯虹湴閸嬨垽鏌＄仦鍓ф创妤犵偛娲崺鈩冩媴閸撹尙鈧磭绱撻崒娆掑厡濠殿喗鍎抽悾鐑芥偨閸偒娼熼梺瑙勫劤閻°劍鍒婇幘顔界厽闁绘柨鎼。濂告煛閸℃瑥顣肩紒缁樼箘閸犲﹤螣瀹勯澹曢梺鎯ф禋閸嬪嫰寮搁悩缁樷拺缂侇垱娲橀弶褰掓煕鐎ｎ偅灏い顏勫暣婵″爼宕卞Δ鍐啰闂備胶鍘ч崯鍧楁偉婵傜鏄ュ┑鐘叉搐閸楁娊鏌曡箛銉х？闁告瑥妫楅埞鎴︻敊閺傘倓绶甸梺鍛娒肩划娆忕暦閿濆牏鐤€闁哄倸澧界粻姘渻閵堝棛澧紒顔惧█閹偤骞嗚閺€浠嬫⒔閸パ€鏋栭柡鍥╁У椤洟鏌熼悜姗嗘闁轰礁锕幃宄扳枎韫囨搩浠鹃梺缁樼箖閻楃姴顫忓ú顏呯劵闁绘劘灏€氭澘顭胯婢х晫妲愰幘鎰佸悑闁告侗鍣Λ锛勭磽娴ｇ鈧摜绮旈幘顔光偓锕傚Ω閳轰線鍞堕梺缁樻濞撳湱鑺遍崹顐ょ瘈闁汇垽娼ч埢鍫熺箾娴ｅ啿娴傞弫鍕煕濞戝崬鐏ｆ俊顐灣閹叉悂寮崼婵婃憰閻庡箍鍎遍ˇ顖涘閻樼粯鐓曢柡鍥ュ妼娴滄劙鏌涚€ｎ偅灏扮紒缁樼箓椤繈顢楅崒姘哗婵犵數濮伴崹濂稿春閺嶎厼绀夐柡宥庡幗閸嬪倿鏌￠崶鈺佹灁缂佺娀绠栭弻娑㈠焺閸愮偓鐣肩紓浣哄Т瀵爼濡甸崟顖毼ㄩ柨婵嗘噽娴煎嫰姊洪柅鐐茶嫰婢у弶銇勯銏╂Ц閻撱倖鎱ㄥ璇蹭壕闂佽鍠氶崗姗€宕洪埄鍐╁闁绘垶蓱椤旀洘淇婇悙顏勨偓鏍礉瑜忕划濠氬箣閻樼數鐒奸柣搴秵閸嬩焦绂嶅鍫熺厵闁逛絻娅曞▍鍛存煟韫囨稐鎲鹃柡灞界Х椤т線鏌涢幘瀵告噧妞ゎ厼娲崹楣冨箛娴ｅ湱绋佹繝鐢靛仜濡﹥绂嶅鍛笉濞村吋娼欑粻褰掑级閸繂鈷旂€光偓濞戞瑤绻嗛柟缁樺笧婢ф稓绱掔紒妯肩疄婵☆偄鍟埥澶娢熷畡棰佸闂佽崵鍠栭崑濠傤焽閺嶎厽鐓犲┑顔藉姇閳ь剚娲熷畷鐟扳攽閸ャ儰绨婚梺瑙勫礃濞夋盯寮告惔锝囩＜闁告挷绀佹禒褔鏌嶈閸撴瑩宕㈠鍫濈；闁瑰墽绮悡鍐喐濠婂牆绀堟慨妯挎硾妗呭┑鐐叉閹稿摜绮堥崘顏佸亾閻熸澘顥忛柛鐘冲哺瀹曨剟鎮介崨濠勫幗闂佺粯顭囬崕銈夘敂閻樼粯鍊堕煫鍥ь儏婵倻鈧娲樼换鍫ュ箖閵忋倖鎯為柛锔诲弿缁辨煡姊绘担绛嬫綈婵犮垺蓱閺呰埖鎯旈敐鍛氶梺閫炲苯澧存慨濠呮閹风娀鍨鹃搹顐や憾婵＄偑鍊戦崝宀勬晝椤忓牏宓侀柛鎰靛枟閸婂鏌ら幁鎺戝姎濞存粓浜跺娲礈閹绘帊绨介梺鍝ュУ閸旀鍩€椤掍胶顣插┑鐐诧工椤繒绱掑Ο璇差€撻梺鍛婄☉閿曘儵宕曢幘鏂ユ斀闁宠棄妫楁禍婊勩亜閺囥劌骞樻俊鍙夊姍閹瑧鈧稒锚椤庢捇姊洪崨濠勭細闁稿孩褰冮埢鎾诲箚瑜夐弨浠嬫煃閽樺顥滈柣蹇嬪劜缁绘盯宕崘顏喰滈悗娈垮枛绾绢參鍩€椤掑﹦绉甸柛鐘愁殜閹繝鎮㈤梹鎰畾闂侀潧鐗嗛崐鍛婄妤ｅ啯鈷戠紓浣癸供濞堟梹淇婇锝囩畵闁伙絽鍢查…銊╁礋椤撶姷妲囨繝娈垮枟閿曗晠宕㈤幖浣哥婵炲樊浜濋埛鎴︽煕濞戞﹫鍔熼柍钘夘樀閺屻劑寮村Ο鍝勫Е闂佽鍣换婵嬪箖閵忋倕绀傞柤娴嬫櫅楠炲牓姊绘担鍛婃儓闁哥喓濞€瀹曟垿骞樼紒妯煎幈闂侀潧顭堥崕鏌ュ磻閵夆晜鐓熸繛鎴濆船閺嬫盯鎽堕弽顓熺厱婵炴垵宕弸娑欑箾閸喎濮嶆慨濠冩そ瀹曟﹢宕ｆ径瀣壍闂備胶顭堥敃銈夊箺濠婂嫮鐝堕柡鍥ュ焺閺佸秵鎱ㄥΟ鍝勬倯閻庨潧鐭傚娲濞戞艾顣哄┑鈽嗗亝缁嬫帡寮查崼鏇熸櫇闁稿本绋撻崢杈ㄧ節閻㈤潧孝闁哥喓澧楅弲鍫曟焼瀹ュ棛鍘?
                        if (loading) {
                            loading = false
                        }
                        if (!hasLoadedOnce) {
                            hasLoadedOnce = true
                        }
                        if (firstInteractiveAtMs == 0L) {
                            firstInteractiveAtMs = SystemClock.elapsedRealtime()
                        }
                    }
                }

            if (hasWarmCache) {
                val totalCountHint = queryMediaTotalCount(context)
                val cachedNeedsHydration = when {
                    totalCountHint > 0 -> totalCountHint > cached.size
                    else -> MediaCatalogSnapshotCache.isLikelyTruncated(cached.size)
                }
                withContext(Dispatchers.Main) {
                    if (totalCountHint > 0) {
                        mediaTotalCount = maxOf(mediaTotalCount, totalCountHint)
                    }
                    pendingStartupCatalogHydration = cachedNeedsHydration
                    fullCatalogLoaded = !cachedNeedsHydration
                    fullCatalogLoading = false
                    hasVerifiedCatalog = true
                }
                AppRuntimeLogStore.logInfo(
                    context,
                    "CatalogStartup",
                    "Warm catalog cache restored count=${cached.size} pendingHydration=$cachedNeedsHydration"
                )
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "ColdStartPerf",
                        "defer startup catalog load count=$totalCountHint cached=${cached.size} pending=$cachedNeedsHydration"
                    )
                }
                return@withContext
            }

            val t0 = SystemClock.elapsedRealtime()
            val totalCount = queryMediaTotalCount(context)
            val previewLoadLimit = if (prefs.queueOrder == "time" || prefs.queueOrder == "time_desc" || prefs.queueOrder == "time_asc") {
                (batchSize.coerceAtLeast(1) * 6).coerceIn(180, 480)
            } else {
                (batchSize.coerceAtLeast(1) * 12).coerceIn(480, 1200)
            }
            val raw = loadMediaPageAfterCursor(
                context = context,
                beforeDateAddedSec = null,
                beforeId = null,
                limit = previewLoadLimit
            )
            val t1 = SystemClock.elapsedRealtime()
            val processed = prefs.processedIds
            // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆缂備焦蓱濞呭牓姊绘担鍛婂暈濞撴碍顨婂畷褰掝敂閸繄锛涢梺鍦亾閸撴艾顭囬埡鍌樹簻闁规壋鏅涢悘鈺冪磼娓氬洤娅嶆慨濠呮閸栨牠寮撮悢鍛婄番闂備胶顭堥鍡涘箰閹间讲鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔村姂濮婅櫣绮欓崠鈥冲濡炪値鍘奸悧鎾诲箖濞差亶鏁囬柕蹇婃閹风粯绻涙潏鍓хК妞ゎ偄顦靛畷鎴︽偐缂佹鍘辨繝鐢靛Т閸熸壆绮婚弽顐ょ＜闁稿本姘ㄨ倴闂佸湱鎳撶€氼厾绮悢纰辨晬婵﹩鍙庨崥娆忊攽閿涘嫬浜奸柛濠冪墵瀹曟繆顦寸紒顔碱煼楠炲鎮╅崹顐础闁荤喐绮嶅Λ鍐嚕鐠囨祴妲堥柍鍨涙杹閸嬫捇鏁冮崒姘鳖吅濠电娀娼уú銊︾閹€鏀介柣鎰▕閸ょ喎鈹戦锝呭籍鐎规洩缍佸畷姗€顢欓懖鈺冩毇婵犲痉鏉库偓鏇㈠箠韫囨稑纾归柣鎴ｅГ閻撶喖鏌ｉ弮鍌ょ劸妞ゃ儲鍔欓弻娑氣偓锝庡亝瀹曞瞼鈧鍠楅幐铏叏閳ь剟鏌ｅΟ娲诲晱闁哥喎娲︽穱濠囨倷椤忓嫧鍋撻弽顓炵鐟滃繒鍒掓繝姘闁绘ê鍚€缁楀姊哄Ч鍥х仼闁硅绻濋幃锟犲礃椤旂晫鍘卞┑鐘绘涧濡顢旈鍫熺厱闁挎繂妫欓妵婵嬫煛?
            val processedSnapshot = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
                addAll(processed)
            }
            val orderedRaw = orderMediaForQueue(
                media = raw,
                queueOrder = prefs.queueOrder,
                randomOrderSeed = queueRandomOrderSeed
            )
            val visibleRaw = selectActiveCatalogMedia(orderedRaw, java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
                addAll(prefs.pendingTrashIds)
            }, deckHiddenFolders())
            val t2 = SystemClock.elapsedRealtime()
            val initialSnapshot = homeFeedCoordinator.rebuildFromAllMedia(
                visibleRaw,
                criteriaSnapshot,
                batchSize,
                processedSnapshot
            )
            val initialDisplayDistinct = initialSnapshot.displayMedia.distinctBy { it.id }
            val t3 = SystemClock.elapsedRealtime()
            if (BuildConfig.DEBUG) {
                Log.d(
                    "ColdStartPerf",
                    "cache=$hasWarmCache preview=${raw.size}/$previewLoadLimit count=$totalCount load=${t1 - t0}ms order=${t2 - t1}ms feed=${t3 - t2}ms total=${t3 - t0}ms"
                )
            }
            val previewNeedsHydration = when {
                totalCount > 0 -> totalCount > orderedRaw.size
                else -> orderedRaw.size >= previewLoadLimit
            }
            val previewTotalSizeBytes = orderedRaw.sumOf { it.size }
            
            withContext(Dispatchers.Main) {
                val shouldStabilizeVisible = hasLoadedOnce && !loading && displayMedia.isNotEmpty()
                val mergedDisplay = if (shouldStabilizeVisible) {
                    mergeDisplayKeepingVisible(
                        currentDisplay = displayMedia,
                        targetDisplay = initialDisplayDistinct,
                        limit = organizeQueueLimit(batchSize, criteriaSnapshot).coerceAtLeast(1)
                    )
                } else {
                    initialDisplayDistinct
                }
                allMedia = orderedRaw
                feedLoadedRows = orderedRaw.size
                mediaTotalCount = maxOf(totalCount, orderedRaw.size)
                mediaTotalSizeBytes = maxOf(mediaTotalSizeBytes, previewTotalSizeBytes)
                displayMedia = mergedDisplay
                playbackGateJob?.cancel()
                playbackGateJob = null
                topCardPlaybackArmed = mergedDisplay.isNotEmpty()
                resetBatchProgress(mergedDisplay.size)
                resetHomeSnappingCarouselBatchOrder(mergedDisplay)
                batchProgressUndoStack = emptyList()
                clearHomeUndoAnimationTargets()
                clearHomeSnappingCarouselUndoPage()
                pendingTrashCount = prefs.pendingTrashIds.size
                if (BuildConfig.DEBUG) {
                    val unlockElapsed = SystemClock.elapsedRealtime() - coldStartAtMs
                    Log.d(
                        "ColdStartPerf",
                        "unlock cache=$hasWarmCache elapsed=${unlockElapsed}ms"
                    )
                }
                if (firstInteractiveAtMs == 0L) {
                    firstInteractiveAtMs = SystemClock.elapsedRealtime()
                }
                pendingStartupCatalogHydration = previewNeedsHydration
                startupCatalogHydrationRunning = false
                fullCatalogLoaded = !previewNeedsHydration
                fullCatalogLoading = false
                hasVerifiedCatalog = true
                loading = false
                hasLoadedOnce = true
            }
            MediaCatalogSnapshotCache.persist(context, orderedRaw)
        }
    }

    fun loadNextBatch(playEntryAnimation: Boolean = false) {
        val currentAllMedia = selectActiveCatalogMedia(
            allMedia,
            java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
                addAll(prefs.pendingTrashIds)
            },
            deckHiddenFolders()
        )
        val processedSnapshot = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
            addAll(prefs.processedIds)
        }
        val criteria = buildFilterCriteria()
        val currentBatchSize = batchSize

        scope.launch(Dispatchers.Default) {
            if (playEntryAnimation) {
                withContext(Dispatchers.Main) {
                    isInitialLoad = true
                    entryAnimationToken++
                }
            }

            val snapshot = homeFeedCoordinator.rebuildFromAllMedia(currentAllMedia, criteria, currentBatchSize, processedSnapshot)

            withContext(Dispatchers.Main) {
                isFullAlbumDone = snapshot.isFullAlbumDone
                resetHomeSnappingCarouselViewportToStart()
                homeSnappingCarouselSettlementBatchToken += 1L
                displayMedia = snapshot.displayMedia.distinctBy { it.id }
                playbackGateJob?.cancel()
                playbackGateJob = null
                topCardPlaybackArmed = snapshot.displayMedia.isNotEmpty()
                resetBatchProgress(snapshot.displayMedia.size)
                resetHomeSnappingCarouselBatchOrder(snapshot.displayMedia.distinctBy { it.id })
                undoStack = emptyList()
                batchProgressUndoStack = emptyList()
                clearHomeUndoAnimationTargets()
            }
        }
    }

    fun loadNextSnappingCarouselBatch() {
        queueRandomOrderSeed = SystemClock.elapsedRealtimeNanos()
        homeSnappingCarouselCurrentPage = 0
        clearHomeSnappingCarouselUndoPage()
        loadNextBatch(playEntryAnimation = true)
    }

    fun continueSnappingCarouselBatchByKeepingRemaining() {
        val processed = prefs.processedIds
        val pendingTrash = prefs.pendingTrashIds
        val keepCandidates = displayMedia.filter { item ->
            !processed.contains(item.id) && !pendingTrash.contains(item.id)
        }
        if (keepCandidates.isNotEmpty()) {
            processed.addAll(keepCandidates.map { it.id })
            prefs.processedIds = processed
            addKeepItems(
                ids = buildMediaIdSet(keepCandidates),
                mediaSource = keepCandidates
            )
            val newStats = stats.copy(
                processed = stats.processed + keepCandidates.size,
                skipped = stats.skipped + keepCandidates.size
            )
            stats = newStats
            statePersistenceWriter.schedule(
                HomeUndoPersistenceSnapshot(
                    persistProcessedIds = true,
                    persistPendingTrashIds = false,
                    persistLikedIds = false,
                    persistMovedIds = false,
                    folderUsage = null,
                    stats = newStats
                )
            )
        }
        loadNextSnappingCarouselBatch()
    }

    fun refreshAllMedia(
        playEntryAnimation: Boolean = false,
        preserveVisibleWindow: Boolean = false
    ) {
        if (startupCatalogHydrationRunning) return
        pendingStartupCatalogHydration = false
        startupCatalogHydrationRunning = true
        fullCatalogLoading = true
        val keepVerifiedCatalogDuringRefresh = preserveVisibleWindow && allMedia.isNotEmpty()
        if (!keepVerifiedCatalogDuringRefresh) {
            hasVerifiedCatalog = false
        } else {
            AppRuntimeLogStore.logInfo(
                context,
                "CatalogStartup",
                "Keeping verified catalog during full refresh items=${allMedia.size}"
            )
        }
        val previousDisplaySnapshot = if (preserveVisibleWindow) displayMedia else emptyList()
        val previousBatchTotalSnapshot = if (preserveVisibleWindow) currentBatchTotal else 0
        val previousBatchConsumedSnapshot = if (preserveVisibleWindow) currentBatchConsumed else 0
        scope.launch(Dispatchers.IO) {
            if (playEntryAnimation) {
                withContext(Dispatchers.Main) {
                    isInitialLoad = true
                    entryAnimationToken++
                }
            }
            val totalCount = queryMediaTotalCount(context)
            val raw = loadMedia(context)
            val orderedRaw = orderMediaForQueue(
                media = raw,
                queueOrder = prefs.queueOrder,
                randomOrderSeed = queueRandomOrderSeed
            )
            val totalSizeBytes = orderedRaw.sumOf { it.size }
            MediaAnalysisManager.clearKeywordFailureBackoff()
            val pendingTrashSnapshot = HashSet(prefs.pendingTrashIds)
            val processedSnapshot = HashSet(prefs.processedIds)
            val hiddenFoldersSnapshot = deckHiddenFolders()
            val criteria = buildFilterCriteria()
            val activeMedia = selectActiveCatalogMedia(
                orderedRaw,
                pendingTrashSnapshot,
                hiddenFoldersSnapshot
            )
            val refreshResolution = if (preserveVisibleWindow && previousDisplaySnapshot.isNotEmpty()) {
                computeCatalogSyncResolution(
                    homeFeedCoordinator = homeFeedCoordinator,
                    allMedia = orderedRaw,
                    pendingTrashIds = pendingTrashSnapshot,
                    hiddenFolders = hiddenFoldersSnapshot,
                    criteria = criteria,
                    batchSize = batchSize,
                    processedIds = processedSnapshot,
                    previousDisplay = previousDisplaySnapshot,
                    previousBatchTotal = previousBatchTotalSnapshot,
                    previousBatchConsumed = previousBatchConsumedSnapshot,
                    allowPartialWindowPreservation = true
                )
            } else {
                null
            }
            val rebuiltSnapshot = if (refreshResolution == null) {
                homeFeedCoordinator.rebuildFromAllMedia(activeMedia, criteria, batchSize, processedSnapshot)
            } else {
                null
            }
            withContext(Dispatchers.Main) {
                skipNextCatalogSync = true
                mediaTotalCount = totalCount
                mediaTotalSizeBytes = maxOf(mediaTotalSizeBytes, totalSizeBytes)
                allMedia = orderedRaw
                fullCatalogLoaded = true
                fullCatalogLoading = false
                feedLoadedRows = orderedRaw.size
                hasVerifiedCatalog = true
                startupCatalogHydrationRunning = false
                val resolvedDisplay = refreshResolution?.displayMedia ?: rebuiltSnapshot!!.displayMedia.distinctBy { it.id }
                val preservesBatchProgress = refreshResolution?.preservesBatchProgress == true
                val keepsCurrentSession =
                    preserveVisibleWindow &&
                        preservesBatchProgress &&
                        resolvedDisplay.isNotEmpty()
                isFullAlbumDone = refreshResolution?.isFullAlbumDone ?: rebuiltSnapshot!!.isFullAlbumDone
                resetHomeSnappingCarouselViewportToStart()
                if (!keepsCurrentSession) {
                    homeSnappingCarouselSettlementBatchToken += 1L
                }
                displayMedia = resolvedDisplay
                if (refreshResolution?.shouldAlignCurrentDisplay == true) {
                    homeFeedCoordinator.alignCurrentDisplay(resolvedDisplay)
                }
                playbackGateJob?.cancel()
                playbackGateJob = null
                topCardPlaybackArmed = resolvedDisplay.isNotEmpty()
                currentBatchTotal = refreshResolution?.currentBatchTotal ?: resolvedDisplay.size
                currentBatchConsumed = if (preservesBatchProgress) {
                    previousBatchConsumedSnapshot.coerceIn(0, currentBatchTotal)
                } else {
                    0
                }
                if (!keepsCurrentSession) {
                    resetBatchProgress(resolvedDisplay.size)
                    resetHomeSnappingCarouselBatchOrder(resolvedDisplay)
                    undoStack = emptyList()
                    batchProgressUndoStack = emptyList()
                    clearHomeUndoAnimationTargets()
                    clearHomeSnappingCarouselUndoPage()
                }
            }
            MediaCatalogSnapshotCache.schedulePersist(context, orderedRaw)
        }
    }

    val needsFullCatalogForInsights = showDeepOrganize ||
        showFilter ||
        showShootingStats ||
        showMonthly ||
        showDailyCalendar ||
        showLikes ||
        showDeleteModal ||
        showVideoCompress ||
        showImageCompress
    LaunchedEffect(needsFullCatalogForInsights, hasLoadedOnce, hasVerifiedCatalog) {
        if (!hasLoadedOnce || !hasVerifiedCatalog) return@LaunchedEffect
        if (pendingStartupCatalogHydration || startupCatalogHydrationRunning) {
            if (needsFullCatalogForInsights) {
                fullCatalogLoading = true
            }
            return@LaunchedEffect
        }
        if (needsFullCatalogForInsights) {
            fullCatalogLoading = true
            refreshAllMedia(
                playEntryAnimation = false,
                preserveVisibleWindow = true
            )
        }
    }

    val catalogSyncSignature by produceState(initialValue = 0L, key1 = allMedia) {
        value = withContext(Dispatchers.Default) {
            buildCatalogSyncSignature(allMedia)
        }
    }
    LaunchedEffect(catalogSyncSignature, batchSize, filterFolder, filterType, filterMonth, filterDay, filterTimePreset, filterCollection, hasLoadedOnce, isInitialLoad, loading) {
        if (!hasLoadedOnce) return@LaunchedEffect
        if (isInitialLoad || loading) return@LaunchedEffect
        if (skipNextCatalogSync) {
            skipNextCatalogSync = false
            return@LaunchedEffect
        }
        val previousDisplay = displayMedia
        val previousBatchTotal = currentBatchTotal
        val previousBatchConsumed = currentBatchConsumed
        // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap.newKeySet 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆缂備焦蓱濞呭牓姊绘担鍛婂暈濞撴碍顨婂畷褰掝敂閸繄锛涢梺鍦亾閸撴艾顭囬埡鍌樹簻闁规壋鏅涢悘鈺冪磼娓氬洤娅嶆慨濠呮閸栨牠寮撮悢鍛婄番闂備胶顭堥鍡涘箰閹间讲鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔村姂濮婅櫣绮欓崠鈥冲濡炪値鍘奸悧鎾诲箖濞差亶鏁囬柕蹇婃閹风粯绻涙潏鍓хК妞ゎ偄顦靛畷鎴︽偐缂佹鍘辨繝鐢靛Т閸熸壆绮婚弽顐ょ＜闁稿本姘ㄨ倴闂佸湱鎳撶€氼厾绮悢纰辨晬婵﹩鍙庨崥娆忊攽閿涘嫬浜奸柛濠冪墵瀹曟繆顦寸紒顔碱煼楠炲鎮╅崹顐础闁荤喐绮嶅Λ鍐嚕鐠囨祴妲堥柍鍨涙杹閸嬫捇鏁冮崒姘鳖吅濠电娀娼уú銊︾閹€鏀介柣鎰▕閸ょ喎鈹戦锝呭籍鐎规洩缍佸畷姗€顢欓懖鈺冩毇婵犲痉鏉库偓鏇㈠箠韫囨稑纾归柣鎴ｅГ閻撶喖鏌ｉ弮鍌ょ劸妞ゃ儲鍔欓弻娑氣偓锝庡亝瀹曞瞼鈧鍠楅幐铏叏閳ь剟鏌ｅΟ娲诲晱闁哥喎娲︽穱濠囨倷椤忓嫧鍋撻弽顓炵鐟滃繒鍒掓繝姘闁绘ê鍚€缁楀姊哄Ч鍥х仼闁硅绻濋幃锟犲礃椤旂晫鍘卞┑鐘绘涧濡顢旈鍫熺厱闁挎繂妫欓妵婵嬫煛?
        val criteria = buildFilterCriteria()
        val hiddenFoldersSnapshot = deckHiddenFolders()
        val syncResolution = withContext(Dispatchers.Default) {
            computeCatalogSyncResolution(
                homeFeedCoordinator = homeFeedCoordinator,
                allMedia = allMedia,
                pendingTrashIds = HashSet(prefs.pendingTrashIds),
                hiddenFolders = hiddenFoldersSnapshot,
                criteria = criteria,
                batchSize = batchSize,
                processedIds = HashSet(prefs.processedIds),
                previousDisplay = previousDisplay,
                previousBatchTotal = previousBatchTotal,
                previousBatchConsumed = previousBatchConsumed
            )
        }
        displayMedia = syncResolution.displayMedia
        if (syncResolution.shouldAlignCurrentDisplay) {
            // Keep coordinator window in sync with the UI-preserved window.
            // Otherwise next swipe is calculated against a refilled internal window and progress gets stuck.
            homeFeedCoordinator.alignCurrentDisplay(syncResolution.displayMedia)
        }
        isFullAlbumDone = syncResolution.isFullAlbumDone
        currentBatchTotal = syncResolution.currentBatchTotal
        currentBatchConsumed = if (syncResolution.preservesBatchProgress) {
            previousBatchConsumed.coerceIn(0, syncResolution.currentBatchTotal)
        } else {
            0
        }
    }

    // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｉ幇顒佲枙闁绘帟濮ょ换娑㈠幢濡纰嶉柣鐔哥懕缁犳捇骞冨Δ鍛櫜閹肩补鍓濋悘宥夋⒑閸濆嫭濯煎ù婊庝簻椤繘鎼圭憴鍕彴闂佸搫琚崕娲焵椤掍緡娈滈柡宀嬬秬缁犳盯寮崹顔芥嚈闂備浇顕栭崳顖滄崲濠靛鏋侀柟鍓х帛閸嬫劙鏌涘▎蹇ｆШ闁逞屽墻閸撶喖骞冨Δ鍛祦闁割煈鍠栨慨搴ㄦ⒑閻熸澘妲绘い鎴濇嚇閳ワ箓宕堕鈧悡娑㈡煕鐏炰箙鑲╄姳婵犳碍鈷戠痪顓炴噹閻掍粙鏌涢弬璺ㄐх€规洝娅曠€佃偐鈧稒顭囬崢鎼佹煟鎼搭垳绉靛ù婊呭仧缁崵绱掑Ο璇插伎?derivedStateOf 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柟缁樺笧閳ь剦鍙冨鍝勑ч崶褏浠奸梺璇茬箲閼归箖鎮鹃悜钘夎摕闁靛濡囬崢鐢告⒑鐟欏嫷鍟忛柛鐘崇墵閵嗗倹绺介崨濠勫幈闁硅壈鎻槐鏇熺墡闂備線娼уú銈団偓姘緲椤曪絾绂掔€ｎ€晝鎲哥€ｎ喖鐒垫い鎺嶇濞搭噣鏌＄仦鐣屝ょ紒杈ㄥ笒铻ｉ柣鎴旀櫆鐎氫粙姊绘笟鈧埀顒傚仜閼活垱鏅堕鍓х＜闁绘灏欐晥閻庤娲樺ú鐔煎蓟閸℃鍚嬮柛娑卞灣閺嬪啴姊绘担鍝ョШ闁稿锕畷鏉款潩椤撶姭鏋栭悗骞垮劚椤︿即鎮″▎鎾崇骇闁割偁鍎抽悾鐑樹繆缂併垹娅嶉柡灞剧〒閳ь剨绲芥晶搴ｅ姬閳ь剟姊洪崫鍕効缂佺粯绻傞悾鐑藉醇閺囩倣鈺呮煏閸繂顏╂い鎺嬪灲濮婄粯鎷呮笟顖滃姼缂備胶绮崝娆忣嚕閻㈠壊鏁嗛柛灞剧☉鎼村﹤鈹戦悙鏉戠仧闁搞劌缍婇弻瀣炊椤掍胶鍘介梺鍝勫暙閻楁粌螞閹存緷褰掓偐鐏炲倸浠┑顔硷攻濡炰粙骞冮悜钘夌骇閻犳亽鍔戦悰鎾绘⒒娴ｇ儤鍤€闁规祴鍓濈换娑欑節閸屻倕娈ㄥ銈嗗姧缁犳垹绮堥崘鈹夸簻闁哄啫娲ゆ禍褰掓煕閺冩挾鐣辨い顏勫暣婵″爼宕卞Δ鍐噯闂備礁鎽滈崰鍡涘礉瀹ュ洨鐭?
    HomeWarmupEffects(
        context = context,
        displayMedia = displayMedia,
        currentLanguageCode = currentLanguage.code,
        catalogVerified = hasVerifiedCatalog,
        hasLoadedOnce = hasLoadedOnce,
        isInitialLoad = isInitialLoad,
        loading = loading,
        allowHomeWarmups = allowHomeWarmups,
        homeInteractionPerfController = homeInteractionPerfController
    )

    fun isRestrictedFolderName(name: String): Boolean {
        if (name == "All") return true
        val lower = name.lowercase(Locale.getDefault())
        val blockedLatin = listOf("weixin", "wechat", "wx", "qq", "tencent", "camera", "camara", "screenshots", "screenshot", "dcim")
        if (blockedLatin.any { lower.contains(it) }) return true
        val blockedLocal = listOf("\u76F8\u673A", "\u622A\u56FE", "\u622A\u5C4F")
        return blockedLocal.any { name.contains(it) }
    }
    
    fun applyFolderRenameToPrefs(oldName: String, newName: String) {
        val usage = prefs.folderUsage
        if (usage.containsKey(oldName)) {
            usage[newName] = usage.remove(oldName) ?: 0
            prefs.folderUsage = usage
            folderUsageState = usage
        }
        val lastUsed = prefs.folderLastUsed
        if (lastUsed.containsKey(oldName)) {
            lastUsed[newName] = lastUsed.remove(oldName) ?: 0L
            prefs.folderLastUsed = lastUsed
        }
        val likesCounts = prefs.likesFolderSelectionCounts
        if (likesCounts.containsKey(oldName)) {
            likesCounts[newName] = likesCounts.remove(oldName) ?: 0
            prefs.likesFolderSelectionCounts = likesCounts
        }
        val hidden = prefs.hiddenFolders
        if (hidden.remove(oldName)) {
            hidden.add(newName)
            prefs.hiddenFolders = hidden
            hiddenFolders = hidden
        }
        val prioritized = prefs.prioritizedFolders
        if (prioritized.remove(oldName)) {
            prioritized.add(newName)
            prefs.prioritizedFolders = prioritized
        }
        val userCreated = prefs.userCreatedFolders
        if (userCreated.remove(oldName)) {
            userCreated.add(newName)
            prefs.userCreatedFolders = userCreated
        }
        if (filterFolder == oldName) {
            filterFolder = newName
            prefs.filterFolder = newName
        }
        if (pendingFilterFolder == oldName) {
            pendingFilterFolder = newName
        }
    }

    fun applyMediaUpdateAcrossCatalogs(updated: MediaItem) {
        // Folder moves update the backing catalog path/bucket metadata, but the active
        // batch state is already driven by handleSwipe/onUndoRestore. Skipping the next
        // catalog sync prevents batch progress from being reset mid-settlement.
        skipNextCatalogSync = true
        allMedia = replaceMediaItemById(allMedia, updated)
    }

    fun releaseKeptForCurrentFilter(onComplete: () -> Unit) {
        val processed = prefs.processedIds
        val liked = prefs.likedIds
        val pendingTrash = prefs.pendingTrashIds
        val releaseCriteria = buildFilterCriteria()
        val processedMatching = selectProcessedMedia(insightsMedia, processed.toSet(), releaseCriteria)

        val toRelease = processedMatching.filter {
            processed.contains(it.id) &&
                !liked.contains(it.id) &&
                !pendingTrash.contains(it.id) &&
                !prefs.movedIds.contains(it.id)
        }

        if (toRelease.isNotEmpty()) {
            val toReleaseIds = buildMediaIdSet(toRelease)
            processed.removeAll(toReleaseIds)
            prefs.processedIds = processed
            removeKeepIds(toReleaseIds)

            val newStats = stats.copy(
                processed = (stats.processed - toRelease.size).coerceAtLeast(0),
                skipped = (stats.skipped - toRelease.size).coerceAtLeast(0)
            )
            stats = newStats
            prefs.stats = newStats
        }

        onComplete()
    }
    

    LaunchedEffect(showDeleteModal, pendingTrashCount) {
        if (showDeleteModal && pendingTrashCount == 0) {
            showDeleteModal = false
        }
    }

    val deleteModalOverHomeSnappingCarousel =
        showDeleteModal &&
            backtrackModeEnabled &&
            !showStartupWelcomeCard &&
            viewingItems == null &&
            fullScreenVideoItem == null &&
            monthlyReviewList.isEmpty() &&
            !showMonthly &&
            !showLanguage &&
            !showSwipePreferences &&
            !showSettings &&
            !showLab &&
            !showLabGlobe &&
            !showLabAlbumSettings &&
            !showLabCopyrightVerify &&
            !showLabLiveSyncTool &&
            !showCampfirePlan &&
            !showHelpCenter &&
            !showElectronicAlbumPlayback &&
            !showFilter &&
            !showLikes &&
            !showKeepGallery &&
            !showWatermarkFrame &&
            !showColorCards &&
            !showColorCardCamera &&
            !showStampCamera &&
            !showFillLight &&
            !showSimilarPhotos &&
            !showLowQuality &&
            !showPhotoWall &&
            !showVideoCompress &&
            !showImageCompress &&
            !showDeepOrganize &&
            !showStatsOverlay &&
            !showShootingStats &&
            !showRecycleBin

    LaunchedEffect(
        showDeleteModal,
        deleteModalOverHomeSnappingCarousel,
        displayMedia.size,
        displayMedia.firstOrNull()?.id,
        displayMedia.lastOrNull()?.id
    ) {
        if (showDeleteModal) {
            if (!homeSnappingCarouselDeleteReturnArmed && deleteModalOverHomeSnappingCarousel) {
                val savedPage = resolveHomeCarouselPage()
                homeSnappingCarouselDeleteReturnPage = savedPage
                homeSnappingCarouselDeleteReturnItemId =
                    resolveHomeFocusedItem(displayMedia)?.id
                homeSnappingCarouselDeleteReturnToSettlement =
                    savedPage == displayMedia.size
                homeSnappingCarouselDeleteReturnArmed = true
            } else if (!deleteModalOverHomeSnappingCarousel) {
                clearHomeSnappingCarouselDeleteReturnState()
            }
            return@LaunchedEffect
        }

        if (!homeSnappingCarouselDeleteReturnArmed) {
            return@LaunchedEffect
        }

        val savedPage = homeSnappingCarouselDeleteReturnPage
        if (savedPage == null) {
            clearHomeSnappingCarouselDeleteReturnState()
            return@LaunchedEffect
        }

        val restoredPage = when {
            homeSnappingCarouselDeleteReturnToSettlement -> displayMedia.size
            else -> {
                val savedItemId = homeSnappingCarouselDeleteReturnItemId
                val restoredIndex = savedItemId?.let { itemId ->
                    displayMedia.indexOfFirst { it.id == itemId }.takeIf { it >= 0 }
                }
                restoredIndex ?: savedPage.coerceIn(0, displayMedia.size)
            }
        }

        if (homeSnappingCarouselCurrentPage != restoredPage) {
            homeSnappingCarouselCurrentPage = restoredPage
        }
        clearHomeSnappingCarouselDeleteReturnState()
    }

    LaunchedEffect(
        pendingStartupCatalogHydration,
        startupCatalogHydrationRunning,
        needsFullCatalogForInsights,
        keepCatalogAnalysisAlive,
        hasLoadedOnce,
        fullCatalogLoaded
    ) {
        if (!pendingStartupCatalogHydration || startupCatalogHydrationRunning) return@LaunchedEffect
        if (!hasLoadedOnce) return@LaunchedEffect
        if (!needsFullCatalogForInsights && !catalogAnalysisDemandActive) {
            return@LaunchedEffect
        }
        if (needsFullCatalogForInsights || keepCatalogAnalysisAlive) {
            fullCatalogLoading = true
            refreshAllMedia(
                playEntryAnimation = false,
                preserveVisibleWindow = true
            )
        }
    }

    fun runIncrementalSync(limit: Int = 3500, refreshUi: Boolean = true) {
        // Baseline storage mode: incremental sync degenerates to a full lightweight refresh.
        if (refreshUi) {
            refreshAllMedia(
                playEntryAnimation = false,
                preserveVisibleWindow = true
            )
        }
    }

    fun scheduleTopCardPlaybackGate(hasNextCard: Boolean) {
        playbackGateJob?.cancel()
        playbackGateJob = null
        if (!hasNextCard) {
            topCardPlaybackArmed = false
            return
        }
        val now = SystemClock.elapsedRealtime()
        val idleGap = if (lastSwipeCommittedAtMs > 0L) now - lastSwipeCommittedAtMs else Long.MAX_VALUE
        if (idleGap > 900L) {
            rapidSwipeStreak = 0
        }
        val rapidBonusMs = when {
            rapidSwipeStreak >= 5 -> 220L
            rapidSwipeStreak >= 3 -> 150L
            rapidSwipeStreak >= 1 -> 90L
            else -> 0L
        }
        val delayMs = (nextPlaybackDelayMs + rapidBonusMs).coerceIn(180L, 420L)
        topCardPlaybackArmed = false
        if (BuildConfig.DEBUG) {
            Log.d("SwipePlaybackGate", "swipe_gate_close delay=$delayMs streak=$rapidSwipeStreak base=$nextPlaybackDelayMs bonus=$rapidBonusMs")
        }
        playbackGateJob = scope.launch {
            delay(delayMs)
            topCardPlaybackArmed = true
            if (BuildConfig.DEBUG) {
                Log.d("SwipePlaybackGate", "swipe_gate_open")
            }
            playbackGateJob = null
        }
    }

    fun scheduleUndoPlaybackGate(nextTopCard: MediaItem?) {
        playbackGateJob?.cancel()
        playbackGateJob = null
        if (nextTopCard == null) {
            topCardPlaybackArmed = false
            return
        }

        val isAutoPlayHeavy = nextTopCard.type == "video" || nextTopCard.isLivePhoto || nextTopCard.isMotionPhoto
        if (!isAutoPlayHeavy) {
            topCardPlaybackArmed = true
            return
        }

        val gateTargetId = nextTopCard.id
        val undoDelayMs = when {
            nextTopCard.type == "video" -> (nextPlaybackDelayMs + 70L).coerceIn(140L, 320L)
            else -> (nextPlaybackDelayMs + 100L).coerceIn(180L, 380L)
        }
        topCardPlaybackArmed = false
        if (BuildConfig.DEBUG) {
            Log.d("SwipePlaybackGate", "undo_gate_close delay=$undoDelayMs itemId=$gateTargetId")
        }
        playbackGateJob = scope.launch {
            delay(undoDelayMs)
            if (resolveHomePlaybackItem()?.id == gateTargetId) {
                topCardPlaybackArmed = true
                if (BuildConfig.DEBUG) {
                    Log.d("SwipePlaybackGate", "undo_gate_open itemId=$gateTargetId")
                }
            }
            playbackGateJob = null
        }
    }

    fun onTopCardStartupSample(itemId: Long, sampleMs: Long) {
        val currentTopId = resolveHomeFocusedItem()?.id
        if (currentTopId != itemId) return
        if (sampleMs <= 0L || sampleMs > 5000L) return

        if (BuildConfig.DEBUG) {
            Log.d("SwipePlaybackGate", "video_startup_sample ms=$sampleMs itemId=$itemId")
        }
        startupEwmaMs = if (startupEwmaMs <= 0f) {
            sampleMs.toFloat()
        } else {
            (startupEwmaMs * 0.7f) + (sampleMs.toFloat() * 0.3f)
        }
        nextPlaybackDelayMs = when {
            startupEwmaMs > 220f -> 260L
            startupEwmaMs > 170f -> 220L
            startupEwmaMs < 120f -> 170L
            else -> nextPlaybackDelayMs
        }
        if (BuildConfig.DEBUG) {
            Log.d(
                "SwipePlaybackGate",
                "ewma_update ms=${startupEwmaMs.toInt()} next_delay=$nextPlaybackDelayMs"
            )
        }
    }
    
    val refreshLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(refreshLifecycleOwner, hasLoadedOnce) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                lastBackgroundAt = System.currentTimeMillis()
                // The process can be killed any time after this; don't leave swipes in the debounce window.
                statePersistenceWriter.flush()
                CatalogScanCoordinator.flush()
            } else if (event == Lifecycle.Event.ON_RESUME && hasLoadedOnce) {
                val now = System.currentTimeMillis()
                val hasValidBackgroundTimestamp = lastBackgroundAt > 0L
                val backgroundGap = if (hasValidBackgroundTimestamp) (now - lastBackgroundAt) else Long.MAX_VALUE
                val quickReturnSkipMs = 2L * 60L * 1000L
                val lightCheckMs = 20L * 60L * 1000L
                val hardRefreshMs = 6L * 60L * 60L * 1000L

                val shouldHardRefresh = allMedia.isEmpty() || backgroundGap >= hardRefreshMs
                if (shouldHardRefresh) {
                    refreshAllMedia(preserveVisibleWindow = true)
                } else if (backgroundGap >= lightCheckMs) {
                    // For medium gaps, only refresh if media total count actually changed.
                    scope.launch(Dispatchers.IO) {
                        val currentTotal = queryMediaTotalCount(context)
                        if (currentTotal != mediaTotalCount) {
                            withContext(Dispatchers.Main) {
                                refreshAllMedia(preserveVisibleWindow = true)
                            }
                        }
                    }
                } else if (backgroundGap < quickReturnSkipMs) {
                    // Skip refresh on quick returns (share cancel/app switch) to avoid card stack reset.
                    Unit
                } else {
                    // Baseline mode has no true incremental path yet; keep current queue untouched.
                    Unit
                }
            }
        }
        refreshLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { refreshLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackGateJob?.cancel()
            playbackGateJob = null
            homeInteractionPerfController.cancel()
            statePersistenceWriter.flush()
            statePersistenceWriter.cancel()
            CatalogScanCoordinator.flush()
            MediaInsightRepository.flush(context)
        }
    }
    val topUndoCandidate = undoStack.lastOrNull()?.item
    LaunchedEffect(topUndoCandidate?.id, topUndoCandidate?.uri, topUndoCandidate?.type) {
        val item = topUndoCandidate ?: return@LaunchedEffect
        prefetchHomeCardMedia(context, item)
    }
    LaunchedEffect(lowQualityUndoAnimationTarget?.token) {
        val token = lowQualityUndoAnimationTarget?.token ?: return@LaunchedEffect
        delay(UNDO_RESTORE_ANIMATION_DURATION_MS)
        if (lowQualityUndoAnimationTarget?.token == token) {
            lowQualityUndoAnimationTarget = null
        }
    }
    LaunchedEffect(pendingHomeUndoCatalogSync?.token) {
        val request = pendingHomeUndoCatalogSync ?: return@LaunchedEffect
        delay(UNDO_CATALOG_SYNC_DEBOUNCE_MS)
        val latestVisibleCatalog = withContext(Dispatchers.Default) {
            selectActiveCatalogMedia(
                allMedia = request.allMediaSnapshot,
                pendingTrashIds = request.pendingTrashIds,
                hiddenFolders = request.hiddenFolders
            )
        }
        val restoredSnapshot = homeFeedCoordinator.onUndoRestore(
            item = request.item,
            criteria = request.criteria,
            batchSize = request.batchSize,
            restoresPendingCount = false,
            catalogOverride = latestVisibleCatalog,
            preserveVisibleOrder = backtrackModeEnabled,
            stableVisibleOrderIds = if (backtrackModeEnabled) homeSnappingCarouselBatchOrderIds else emptyList()
        )
        val snappingCatalogPlacement = if (backtrackModeEnabled && request.snappingCarouselInsertIndex != null) {
            placeSnappingCarouselUndoAtCurrentPosition(
                items = restoredSnapshot.displayMedia,
                restoredItemId = request.item.id,
                preferredInsertIndex = request.snappingCarouselInsertIndex
            )
        } else {
            null
        }
        val isStillShowingRestoredItem = if (backtrackModeEnabled) {
            resolveHomeFocusedItem()?.id == request.item.id
        } else {
            displayMedia.firstOrNull()?.id == request.item.id
        }
        if (
            pendingHomeUndoCatalogSync?.token == request.token &&
            isStillShowingRestoredItem
        ) {
            displayMedia = snappingCatalogPlacement?.display ?: restoredSnapshot.displayMedia
            isFullAlbumDone = restoredSnapshot.isFullAlbumDone
            if (backtrackModeEnabled) {
                snappingCatalogPlacement?.let { resetHomeSnappingCarouselBatchOrder(it.display) }
                val restoredIndex = snappingCatalogPlacement?.restoredIndex
                    ?: displayMedia.indexOfFirst { it.id == request.item.id }
                if (restoredIndex >= 0) {
                    homeSnappingCarouselCurrentPage = restoredIndex
                }
            }
            scheduleUndoPlaybackGate(resolveHomePlaybackItem(restoredSnapshot.displayMedia))
        }
        if (pendingHomeUndoCatalogSync?.token == request.token) {
            pendingHomeUndoCatalogSync = null
        }
    }

    fun resolveHotStatePersistDelayMs(): Long {
        val now = SystemClock.elapsedRealtime()
        val cooldownRemainingMs =
            (homeInteractionPerfController.interactionCooldownUntilMs - now).coerceAtLeast(0L)
        return maxOf(1200L, cooldownRemainingMs + 300L)
    }

    fun scheduleHotStatePersist(
        persistProcessedIds: Boolean = false,
        persistPendingTrashIds: Boolean = false,
        persistLikedIds: Boolean = false,
        persistMovedIds: Boolean = false,
        folderUsage: Map<String, Int>? = null,
        stats: AppStats? = null
    ) {
        if (
            !persistProcessedIds &&
            !persistPendingTrashIds &&
            !persistLikedIds &&
            !persistMovedIds &&
            folderUsage == null &&
            stats == null
        ) {
            return
        }
        statePersistenceWriter.schedule(
            HomeUndoPersistenceSnapshot(
                persistProcessedIds = persistProcessedIds,
                persistPendingTrashIds = persistPendingTrashIds,
                persistLikedIds = persistLikedIds,
                persistMovedIds = persistMovedIds,
                folderUsage = folderUsage,
                stats = stats
            ),
            delayMs = resolveHotStatePersistDelayMs()
        )
    }

    fun handleSwipe(action: UndoAction) {
        val item = action.item
        val nowElapsed = SystemClock.elapsedRealtime()
        val sinceLastSwipe = if (lastSwipeCommittedAtMs > 0L) nowElapsed - lastSwipeCommittedAtMs else Long.MAX_VALUE
        rapidSwipeStreak = if (sinceLastSwipe <= 550L) {
            (rapidSwipeStreak + 1).coerceAtMost(6)
        } else {
            1
        }
        predictedSwipeIntent = swipeIntentFromDirection(action.direction)
        lastSwipeCommittedAtMs = nowElapsed

        // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｉ幇顒佹儓闁搞劌鍊块弻娑㈩敃閿濆棛顦ョ紓浣哄У瑜板啴濡撮幒鎴僵闁挎繂鎳嶆竟鏇熶繆閻愵亜鈧垿宕归悜妯尖攳婵炲樊鈧亜娲獮搴ㄦ嚍閵壯冨箥濠电偞鎸婚懝楣冩倶濠靛洦鍙忛柛宀€鍋為悡鏇㈡煃閸濆嫷鍎戠紒鈾€鍋撻柣搴ゎ潐濞插繘宕濋幋锔衡偓浣割潨閳ь剟骞冮鍫濈劦妞ゆ巻鍋撻崡鍗灻归悡搴ｆ憼闁抽攱甯掗湁闁挎繂鎳忛崯鐐烘煕閻斿搫浠滈柕鍡樺笒椤繈顢樿閻や線鎮楃憴鍕８闁搞劍妞芥俊鍫曟晲婢跺﹦顦ㄩ梺闈浤涚仦鑺ユ珡闂傚倸鍊烽懗鍓佸垝椤栫偐鈧箓宕堕鈧弸渚€鏌涘┑鍕姎妞ゃ儲鑹鹃埞鎴﹀磼濮橆厼鏆堥梺缁樻尵閸犳劗鎹㈠☉銏犲耿婵°倕鍟伴澶愭⒑闂堚晝鎮奸柡鍜佸亰閹偓妞ゅ繐鐗嗙粻姘辨喐鎼淬劌纾婚柕澶嗘櫆閻撴稓鈧厜鍋撻柍褜鍓熷畷浼村冀椤撴壕鍋撴担绯曟瀻闁瑰鍋涢崢褰掓⒑濮瑰洤鈧倝宕规导鏉戠畾闁割偁鍎查埛鎺懨归敐鍫澬撻柕鍡楀暟缁辨帡鍩€椤掍焦濯撮柧蹇撴贡閻撳姊洪崷顓℃闁革綆鍨崇槐鐐哄冀閵娧呯槇闂傚倸鐗婃笟妤呭磿濞戙垺鐓涘ù锝呮啞閸犳﹢鏌″畝瀣埌閾伙綁鎮归崶褎鈻曟俊?5 濠电姷鏁告慨鐑藉极閸涘﹥鍙忛柣鎴ｆ閺嬩線鏌熼梻瀵割槮缁惧墽绮换娑㈠箣閻戝棛鍔┑鐐村灦椤洭寮抽崱娑欑厱闁哄洢鍔屾禍鐐淬亜閿旇骞栭柍瑙勫灴閹晝绱掑Ο濠氭暘闂備胶绮〃鍛涘┑鍡欐殾闁荤喐澹嬮弨浠嬫倵閿濆骸浜為柛娆忔濮婃椽宕崟顒€绐涢梺绋款儐閻╊垶宕洪埀顒併亜閹哄棗浜鹃梺纭呭Г缁酣宕氶幒妤€閱囬柣鏃堫棑缁愮偤姊鸿ぐ鎺戜喊闁告﹢绠栧畷銏ゎ敂閸涱垳鐦堥梺姹囧灲濞佳冪摥缂傚倷鑳剁划顖炲礉濞嗘挸绠氬ù锝呭閻撱儵鏌涢弴銊ュ鐎殿喗瀵х换婵嬫偨闂堟刀銏＄箾鐏炲倸濮傞柟顔藉劤鐓ゆい蹇撴噽閸樻悂鎮楅崗澶婁壕闁诲函缍嗛崜娑㈡晬閻斿摜绠鹃悗鐢殿焾椤庢挸鈹戦垾铏枠妤犵偛绻橀幃鈺冩嫚閼碱剦鍞介梻浣告贡閸庛倝宕洪崼婵愮劷婵浜壕浠嬫煕鐏炴崘澹橀柍褜鍓涢崗姗€骞冮悙鐑樻櫇闁逞屽墴閿濈偠绠涘☉娆忎画闂備緡鍙忔俊鍥礉瀹勬壋鏀介柣鎰级椤ョ偤鏌熼崨濠冨€愰柟顔光偓鏂ユ斀閻庯綆浜為鎰攽閻戝洨绉甸柛鎾寸懄娣囧﹥绺介崨濠勫幗闂婎偄娲﹂弻銊╁煝閺囥垺鐓涢悘鐐插⒔濞插瓨顨ラ悙鍙夊枠妞ゃ垺顨婇崺鈧い鎺戝€诲Λ顖滄喐閺冨牆钃熼柨鐔哄Т楠炪垽鏌熼柇锕€澧┑顕嗙畱閳?
        homeInteractionPerfController.onSwipeCommitted()
        if (false && rapidSwipeStreak % 5 == 0) {
            scope.launch(Dispatchers.IO) {
                val runtime = Runtime.getRuntime()
                val used = runtime.totalMemory() - runtime.freeMemory()
                val headroom = runtime.maxMemory() - used
                // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柛娑橈攻閸欏繘鏌ｉ幋婵愭綗闁逞屽墮閸婂湱绮嬮幒鏂哄亾閿濆簼绨介柛鏃撶畱椤啴濡堕崱妤€娼戦梺绋款儐閹瑰洭寮诲☉銏″亜闂佸灝顑呮禒鎾⒑缁洘鏉归柛瀣尭椤啴濡堕崱妤€娼戦梺绋款儐閹稿墽妲愰幘鎰佸悑闁告粌鍟抽崥顐⑽旈悩闈涗粶闁哥噥鍋夐悘鎺楁煟閻樺弶绌块悘蹇旂懅缁綁鎮欓悜妯锋嫼閻熸粎澧楃敮鎺撶娴煎瓨鐓曢柟鎯ь嚟濞叉挳鏌熼鍡欑瘈妤犵偛顑呴埞鎴﹀箛椤撶偟妲ｉ梻鍌欑窔濞佳囨偋閸℃あ娑樷槈濞嗘劖鐝峰銈嗗笒閸婅崵澹曢崗绗轰簻闁哄啫娲ゆ禍褰掑极閸儲鍊垫繛鍫濈仢閺嬫盯鏌ｉ妶鍛窛缂侇喛顕ч埥澶娢熼柨瀣垫綌婵犵數鍋涘Λ娆撳垂閻㈠摜宓侀柛顐犲劜閳锋帒霉閿濆懏鎲哥紒澶嬫そ閺屾稓鈧綆浜烽煬顒傗偓瑙勬礃缁矂锝炲┑鍫熷磯婵炲樊浜濋敍妤呮煟閻斿摜鐭屽褎顨堥弫顔嘉旈崨顔间簵闂佺粯姊婚崢褏绮婚幎鑺ョ厵闁诡垳澧楅ˉ澶岀磼閳ь剚寰勭仦绋夸壕妤犵偛鐏濋崝姘亜閿旇鐏ｇ紒顔碱煼椤㈡盯鎮欑€电骞堟繝鐢靛仦閸ㄩ潧鐣烽鍕嚑闁绘柨鍚嬮悡鏇㈠箹鏉堝墽鍒伴柡瀣灩缁辨帞绱掑Ο鑲╃杽濠碘槅鍋勯崯顐﹀煡婢跺娼╂い鎴ｆ瑜版碍绻?64MB闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弮鍫熸殰闁稿鎸剧划顓炩槈濡娅ч梺娲诲幗閻熲晠寮婚悢琛″亾閻㈡鐒惧ù鐘欏洦鈷掗柛鏇ㄥ亜椤忣參鏌″畝瀣瘈鐎规洘锕㈡俊鎼佸Ψ閵忕姳澹曢梺姹囧灮椤牏澹曟繝姘厵闁告挆鍛濡炪們鍎遍悧濠勬崲濞戙垹绠ｉ柣鎰嚟濞堛倝姊洪崨濠冾棓闁搞劌鐖奸獮鍐ㄎ旈崘鈺佹瀭闂佸憡娲﹂崜娑⑺囬妸褏纾藉ù锝囩摂閸ゆ瑩鏌ｅΔ鈧敃銉х矚鏉堛劎绡€闁搞儜鍜佸晣濠电偠鎻紞鈧い顐㈩槸閳诲秹宕卞▎鎴狅紳闂佺鏈悷褏鎷归悧鍫㈢瘈闁冲灈鏅涙禍鍓х磽閸屾瑧鍔嶅褍閰ｅ鏌ユ偐瀹割喗缍?
                if (headroom < 64L * 1024L * 1024L) {
                    withContext(Dispatchers.Main) {
                        runCatching { 
                            context.imageLoader.memoryCache?.clear()
                        }
                    }
                    System.gc()
                }
            }
        }

        // Sound moved to DraggableCard to play BEFORE animation
        
        // Combo Logic
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 3000) {
            comboCount++
        } else {
            comboCount = 1
        }
        lastActionTime = now

        // Update Persistence
        val processed = prefs.processedIds
        val processedAdded = processed.add(item.id)

        val criteria = buildFilterCriteria()
        val batchProgressSnapshot = BatchProgressSnapshot(
            total = currentBatchTotal,
            consumed = currentBatchConsumed
        )
        val snapshot = homeFeedCoordinator.onSwipeCommitted(
            item = item,
            criteria = criteria,
            batchSize = batchSize,
            processedIds = processed,
            processedAdded = processedAdded
        )
        val resolvedSnapshot = doExtendFeedWindowIfNeeded(
            baseSnapshot = snapshot,
            criteria = criteria,
            processedIds = processed,
            swipeDirection = action.direction
        )
        displayMedia = resolvedSnapshot.displayMedia.distinctBy { it.id }
        isFullAlbumDone = resolvedSnapshot.isFullAlbumDone
        advanceBatchProgressAfterSwipe(
            remainingPendingCount = resolvedSnapshot.remainingPendingCount
        )
        scheduleTopCardPlaybackGate(resolvedSnapshot.displayMedia.isNotEmpty())

        when (action) {
            is UndoAction.Trash -> {
                val pending = prefs.pendingTrashIds
                val pendingAdded = pending.add(item.id)
                if (pendingAdded) {
                    pendingTrashCount += 1
                }
                removeKeepIds(listOf(item.id))
                
                val newStats = stats.copy(
                    processed = stats.processed + 1,
                    trashed = stats.trashed + 1,
                    savedSize = stats.savedSize + item.size
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedAdded,
                    persistPendingTrashIds = pendingAdded,
                    stats = newStats
                )
            }
            is UndoAction.Like -> {
                val liked = prefs.likedIds
                val likedAdded = liked.add(item.id)
                removeKeepIds(listOf(item.id))

                val newStats = stats.copy(
                    processed = stats.processed + 1,
                    skipped = stats.skipped + 1
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedAdded,
                    persistLikedIds = likedAdded,
                    stats = newStats
                )
            }
            is UndoAction.Keep, is UndoAction.Move -> {
                val movedAdded = if (action is UndoAction.Move) {
                    val moved = prefs.movedIds
                    moved.add(item.id)
                } else {
                    false
                }
                if (action is UndoAction.Move) {
                    removeKeepIds(listOf(item.id))
                } else {
                    addKeepItem(item)
                }
                // Keep or Move (Skipped)
                val newStats = stats.copy(
                    processed = stats.processed + 1,
                    skipped = stats.skipped + 1
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedAdded,
                    persistMovedIds = movedAdded,
                    stats = newStats
                )
            }
        }
        
        undoStack = undoStack + action
        batchProgressUndoStack = batchProgressUndoStack + batchProgressSnapshot
        if (processedAdded) {
            organizedToday = prefs.organizedTodayCount + 1
            prefs.organizedTodayCount = organizedToday
        }
    }

    // Apply pending settings changes when returning to main screen
    LaunchedEffect(showSettings, showStatsOverlay, showShootingStats, showDeepOrganize, showSimilarPhotos,
                   showMonthly, showLikes, showKeepGallery, showWatermarkFrame, showColorCards, showColorCardCamera, showStampCamera, showPromptManager, showFillLight, showLowQuality, showPhotoWall, showVideoCompress, showImageCompress, showSwipePreferences, showRecycleBin) {
        // Only apply when ALL sub-pages and overlays are closed (user is back on main screen)
        val isOnMainScreen = !showSettings && !showStatsOverlay && !showShootingStats && !showDeepOrganize &&
                             !showSimilarPhotos && !showMonthly && !showLikes && !showKeepGallery && !showWatermarkFrame && !showColorCards && !showColorCardCamera && !showStampCamera && !showPromptManager && !showFillLight &&
                            !showLowQuality && !showPhotoWall && !showVideoCompress && !showImageCompress && !showSwipePreferences && !showRecycleBin
        if (isOnMainScreen) {
            if (pendingQueueOrderChange) {
                allMedia = withContext(Dispatchers.Default) {
                    orderMediaForQueue(
                        media = allMedia,
                        queueOrder = queueOrder,
                        randomOrderSeed = queueRandomOrderSeed
                    )
                }
                loadNextBatch(playEntryAnimation = true)
                pendingQueueOrderChange = false
            }
            if (pendingBatchSizeChange) {
                // Update the remember cache with the new batchSize from prefs
                batchSize = prefs.batchSize
                loadNextBatch(playEntryAnimation = false)
                pendingBatchSizeChange = false
            }
        }
    }

    fun handleLowQualitySwipe(item: MediaItem, reason: String, rawAction: SwipeAction, direction: SwipeDirection) {
        suppressStatsHapticsOnce = true
        val action = if (rawAction == SwipeAction.None) SwipeAction.Keep else rawAction
        val processed = prefs.processedIds
        val processedAdded = processed.add(item.id)
        var likeAdded = false
        when (action) {
            SwipeAction.Delete -> {
                val pending = prefs.pendingTrashIds
                val pendingAdded = pending.add(item.id)
                if (pendingAdded) {
                    pendingTrashCount = pending.size
                }
                removeKeepIds(listOf(item.id))
                val newStats = stats.copy(
                    processed = stats.processed + 1,
                    trashed = stats.trashed + 1,
                    savedSize = stats.savedSize + item.size
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedAdded,
                    persistPendingTrashIds = pendingAdded,
                    stats = newStats
                )
            }
            SwipeAction.Like -> {
                val liked = prefs.likedIds
                likeAdded = liked.add(item.id)
                removeKeepIds(listOf(item.id))
                val newStats = stats.copy(
                    processed = stats.processed + 1,
                    skipped = stats.skipped + 1
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedAdded,
                    persistLikedIds = likeAdded,
                    stats = newStats
                )
            }
            SwipeAction.Keep,
            SwipeAction.None -> {
                addKeepItem(item)
                val newStats = stats.copy(
                    processed = stats.processed + 1,
                    skipped = stats.skipped + 1
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedAdded,
                    stats = newStats
                )
            }
        }
        val currentDisplay = displayMedia
        val newDisplay = currentDisplay.filter { it.id != item.id }
        if (newDisplay.size != currentDisplay.size) {
            displayMedia = newDisplay
        }
        lowQualityUndoStack = lowQualityUndoStack + LowQualityUndoEntry(
            item = item,
            reason = reason,
            action = action,
            likeAdded = likeAdded,
            direction = direction
        )
        lowQualityDisplayItems = lowQualityDisplayItems.filter { it.first.id != item.id }
        LowQualityManager.removeResult(item.id)
    }

    fun handleLowQualityUndo() {
        if (lowQualityUndoStack.isEmpty()) return
        suppressStatsHapticsOnce = true
        var undoEntry: LowQualityUndoEntry? = null
        while (lowQualityUndoStack.isNotEmpty()) {
            val candidate = lowQualityUndoStack.last()
            lowQualityUndoStack = lowQualityUndoStack.dropLast(1)
            if (hasLowQualityUndoCandidate(candidate.item.id)) {
                undoEntry = candidate
                break
            }
        }
        val entry = undoEntry ?: return
        val item = entry.item
        val reason = entry.reason
        removeKeepIds(listOf(item.id))
        val processed = prefs.processedIds
        processed.remove(item.id)
        val processedChanged = true
        when (entry.action) {
            SwipeAction.Delete -> {
                val pending = prefs.pendingTrashIds
                pending.remove(item.id)
                if (item.livePhotoVideoId != null) {
                    pending.remove(item.livePhotoVideoId)
                }
                pendingTrashCount = pending.size
                val newStats = stats.copy(
                    processed = stats.processed - 1,
                    trashed = stats.trashed - 1,
                    savedSize = stats.savedSize - item.size
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedChanged,
                    persistPendingTrashIds = true,
                    stats = newStats
                )
            }
            SwipeAction.Like -> {
                val likedChanged = if (entry.likeAdded) {
                    val liked = prefs.likedIds
                    liked.remove(item.id)
                    true
                } else {
                    false
                }
                val newStats = stats.copy(
                    processed = stats.processed - 1,
                    skipped = stats.skipped - 1
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedChanged,
                    persistLikedIds = likedChanged,
                    stats = newStats
                )
            }
            SwipeAction.Keep,
            SwipeAction.None -> {
                val newStats = stats.copy(
                    processed = stats.processed - 1,
                    skipped = stats.skipped - 1
                )
                stats = newStats
                scheduleHotStatePersist(
                    persistProcessedIds = processedChanged,
                    stats = newStats
                )
            }
        }
        lowQualityDisplayItems = listOf(item to reason) + lowQualityDisplayItems
        lowQualityUndoAnimationTarget = UndoAnimationTarget(
            itemId = item.id,
            direction = entry.direction,
            token = nextUndoAnimationToken()
        )
        LowQualityManager.restoreResult(item, reason)
    }

    // A photo thrown off the photo wall: the same bookkeeping as a trash swipe on the sorting deck.
    fun handlePhotoWallTrash(item: MediaItem): Boolean {
        suppressStatsHapticsOnce = true
        val processed = prefs.processedIds
        val processedAdded = processed.add(item.id)
        val pending = prefs.pendingTrashIds
        val pendingAdded = pending.add(item.id)
        if (pendingAdded) {
            pendingTrashCount = pending.size
        }
        removeKeepIds(listOf(item.id))
        val newStats = stats.copy(
            processed = stats.processed + 1,
            trashed = stats.trashed + 1,
            savedSize = stats.savedSize + item.size
        )
        stats = newStats
        scheduleHotStatePersist(
            persistProcessedIds = processedAdded,
            persistPendingTrashIds = pendingAdded,
            stats = newStats
        )
        if (processedAdded) {
            organizedToday = prefs.organizedTodayCount + 1
            prefs.organizedTodayCount = organizedToday
        }
        val currentDisplay = displayMedia
        val newDisplay = currentDisplay.filter { it.id != item.id }
        if (newDisplay.size != currentDisplay.size) {
            displayMedia = newDisplay
        }
        photoWallChanged = true
        return true
    }

    // Undo on the photo wall: the exact reverse of handlePhotoWallTrash.
    fun handlePhotoWallRestore(item: MediaItem) {
        suppressStatsHapticsOnce = true
        val processed = prefs.processedIds
        processed.remove(item.id)
        val pending = prefs.pendingTrashIds
        pending.remove(item.id)
        if (item.livePhotoVideoId != null) {
            pending.remove(item.livePhotoVideoId)
        }
        pendingTrashCount = pending.size
        val newStats = stats.copy(
            processed = stats.processed - 1,
            trashed = stats.trashed - 1,
            savedSize = stats.savedSize - item.size
        )
        stats = newStats
        scheduleHotStatePersist(
            persistProcessedIds = true,
            persistPendingTrashIds = true,
            stats = newStats
        )
        organizedToday = (prefs.organizedTodayCount - 1).coerceAtLeast(0)
        prefs.organizedTodayCount = organizedToday
        photoWallChanged = true
    }

    fun closePhotoWall() {
        showPhotoWall = false
        if (photoWallChanged) {
            photoWallChanged = false
            loadNextBatch(playEntryAnimation = false)
        }
    }
    fun handleUndo() {
        if (undoStack.isEmpty()) return
        if (backtrackModeEnabled && homeSnappingCarouselUndoInFlightToken != null) return
        val carouselPageBeforeUndo = if (backtrackModeEnabled) {
            resolveHomeCarouselPage().coerceIn(0, maxOf(0, displayMedia.lastIndex))
        } else {
            0
        }
        val action = undoStack.last()
        val batchProgressSnapshot = batchProgressUndoStack.lastOrNull()
        undoStack = undoStack.dropLast(1)
        if (batchProgressUndoStack.isNotEmpty()) {
            batchProgressUndoStack = batchProgressUndoStack.dropLast(1)
        }
        val item = action.item
        removeKeepIds(listOf(item.id))
        organizedToday = (prefs.organizedTodayCount - 1).coerceAtLeast(0)
        prefs.organizedTodayCount = organizedToday
        
        // Mark undo animation target; actual list restore is delegated to SwipeWindow.
        val snappingCarouselUndoDirection = if (backtrackModeEnabled && action is UndoAction.Move) {
            SwipeDirection.Up
        } else {
            action.direction
        }
        val undoAnimationToken = enqueueHomeUndoAnimation(
            itemId = item.id,
            direction = snappingCarouselUndoDirection,
            restoreOffsetY = if (action is UndoAction.Trash) action.restoreOffsetY else 0f,
            forSnappingCarousel = backtrackModeEnabled
        )
        if (backtrackModeEnabled) {
            homeSnappingCarouselUndoReleaseJob?.cancel()
            homeSnappingCarouselUndoInFlightToken = undoAnimationToken
            homeSnappingCarouselUndoReleaseJob = scope.launch {
                delay(1200L)
                if (homeSnappingCarouselUndoInFlightToken == undoAnimationToken) {
                    homeSnappingCarouselUndoInFlightToken = null
                    homeSnappingCarouselUndoReleaseJob = null
                }
            }
        }
        
        // Update Persistence
        val processed = prefs.processedIds
        val processedRemoved = processed.remove(item.id)
        var persistPendingTrashIds = false
        var persistLikedIds = false
        var persistMovedIds = false
        var folderUsageSnapshotForPersist: Map<String, Int>? = null
        var statsSnapshotForPersist: AppStats? = null
        

        when (action) {
            is UndoAction.Trash -> {
                val pending = prefs.pendingTrashIds
                pending.remove(item.id)
                if (item.livePhotoVideoId != null) {
                    pending.remove(item.livePhotoVideoId)
                }
                pendingTrashCount = pending.size
                persistPendingTrashIds = true
                
                val newStats = stats.copy(
                    processed = stats.processed - 1,
                    trashed = stats.trashed - 1,
                    savedSize = stats.savedSize - item.size
                )
                stats = newStats
                statsSnapshotForPersist = newStats
            }
            is UndoAction.Like -> {
                val liked = prefs.likedIds
                liked.remove(item.id)
                persistLikedIds = true
                
                val newStats = stats.copy(
                    processed = stats.processed - 1,
                    skipped = stats.skipped - 1
                )
                stats = newStats
                statsSnapshotForPersist = newStats
            }
            is UndoAction.Keep -> {
                val newStats = stats.copy(
                    processed = stats.processed - 1,
                    skipped = stats.skipped - 1
                )
                stats = newStats
                statsSnapshotForPersist = newStats
            }
            is UndoAction.Move -> {
                val moved = prefs.movedIds
                moved.remove(item.id)
                persistMovedIds = true

                // Update Folder Counts
                // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｅΟ娆惧殭缂佺姴鐏氶妵鍕疀閹炬惌妫ょ紓浣插亾濠电姴娲﹂悡鍐喐濠婂牆绀堟慨妯块哺瀹曞弶绻涢幋鐐垫噧缂佸墎鍋ら幃妤呮晲鎼粹€茬敖濡炪倧缂氶崡鎶藉箖?ConcurrentHashMap 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆缂備焦蓱濞呭牓姊绘担鍛婂暈濞撴碍顨婂畷褰掝敂閸繄锛涢梺鍦亾閸撴艾顭囬埡鍌樹簻闁规壋鏅涢悘鈺冪磼娓氬洤娅嶆慨濠呮閸栨牠寮撮悢鍛婄番闂備胶顭堥鍡涘箰閹间讲鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔村姂濮婅櫣绮欓崠鈥冲濡炪値鍘奸悧鎾诲箖濞差亶鏁囬柕蹇婃閹风粯绻涙潏鍓хК妞ゎ偄顦靛畷鎴︽偐缂佹鍘辨繝鐢靛Т閸熸壆绮婚弽顐ょ＜闁稿本姘ㄨ倴闂佸湱鎳撶€氼厾绮悢纰辨晬婵﹩鍙庨崥娆忊攽閿涘嫬浜奸柛濠冪墵瀹曟繆顦寸紒顔碱煼楠炲鎮╅崹顐础闁荤喐绮嶅Λ鍐嚕鐠囨祴妲堥柍鍨涙杹閸嬫捇鏁冮崒姘鳖吅濠电娀娼уú銊︾閹€鏀介柣鎰▕閸ょ喎鈹戦锝呭籍鐎规洩缍佸畷姗€顢欓懖鈺冩毇婵犲痉鏉库偓鏇㈠箠韫囨稑纾归柣鎴ｅГ閻撶喖鏌ｉ弮鍌ょ劸妞ゃ儲鍔欓弻娑氣偓锝庡亝瀹曞瞼鈧鍠楅幐铏叏閳ь剟鏌ｅΟ娲诲晱闁哥喎娲︽穱濠囨倷椤忓嫧鍋撻弽顓炵鐟滃繒鍒掓繝姘闁绘ê鍚€缁楀姊哄Ч鍥х仼闁硅绻濋幃锟犲礃椤旂晫鍘卞┑鐘绘涧濡顢旈鍫熺厱闁挎繂妫欓妵婵嬫煛鐏炲墽銆掗柍褜鍓ㄧ紞鍡樼濠婂牆鐒垫い鎺嗗亾妞ゎ厾鍏橀悰顔藉緞瀹€鈧惌娆撳箹鐎涙ɑ灏伴柡鍌楀亾闂傚倷绀佹竟濠囧磻閸℃稑闂柕澶堝労閻掍粙鏌ｉ弬璺ㄦ闁衡偓娴犲鐓熸俊顖濆亹鐢稒绻涢幊宄板娴滄粓鏌熺€涙绠栨い銉ｅ灲閺岀喖顢欓幆褌娌紓浣芥〃缁瑥鐣烽妸锔剧瘈闁告洦鍋勭粻鐐测攽閿涘嫬浜奸柛濞垮€濆畷銏＄鐎ｎ亜鐎梺姹囧灮椤牓宕欓悩宕囩闁瑰瓨鐟ラ悘顏堟煕鐎ｃ劌濡介柕鍥у瀵粙濡歌濡插牓姊哄畷鍥╁笡婵炲弶绮撻崺鈧い鎺嶇贰閸熷繘鏌涢悩宕囧⒌闁诡喓鍎茬缓鐣岀矙閹稿孩袣闂備焦鏋奸弲娑㈠疮閹殿喖濮柍褜鍓熷铏圭矙閹稿孩鎷卞┑顔角滈崝鎴︺€佸▎鎰瘈闁搞儯鍔夐幏缁樼箾鏉堝墽鍒伴柟璇х節楠炲骞栨担鍦幈闁瑰吋鐣崹瑙勬叏瀹ュ洠鍋撶憴鍕缂侇喗鎹囬獮鍐閿涘嫰妾繝銏ｆ硾閿曘儵顢橀幐搴濈箚闁绘劦浜滈埀顒佺墪鐓ょ€广儱顦壕濠氭煙閹呬邯闁稿鎸鹃幉鎾礋椤掑偆妲扮紓鍌欑贰閸犳牜绮旇ぐ鎺斿祦闊洦绋戝婵嬫煛婢跺鐏︾€规挸绉撮—鍐Χ閸℃ê鏆楅梺鍝ュУ閸旀瑩銆佸▎鎺旂杸闁圭虎鍨遍弬鈧梺鍦劋婵炲﹤鐣烽幇顑╂梹鎷呴悷鏉夸紟闂備胶绮崹鐓幬涢崟顓烆棜濠电姵纰嶉悡鐔镐繆椤栨繂鍚归柕鍫熸尦閺岋紕鈧綆鍋嗘晶顒傜磼缂佹娲存鐐差儔閹瑩宕ｆ径灞剧€繝鐢靛仜閻°劎鍒掑畝鈧槐鐐寸節閸パ嗘憰濠电偞鍨堕惌顔尖柦椤忓牊鐓曢柟鐐仦鐠佹煡宕戦幘璇茬婵°倓鑳堕崢鎾绘⒑閼姐倕鏋戝鐟版閻楀酣姊绘笟鈧褍煤閵堝洠鍋撳顓熺凡妞ゆ洩缍侀、姘跺焵椤掆偓閻ｇ兘宕奸弴妞诲亾閺嶎収鏁婇柤鎭掑劚绾炬娊姊?
                val currentUsage = java.util.concurrent.ConcurrentHashMap(prefs.folderUsage)
                val targetBucketId = File(action.targetFolder).name
                
                if (action.isNewFolder) {
                    currentUsage.remove(targetBucketId)
                } else {
                    val count = currentUsage[targetBucketId] ?: 0
                    if (count > 0) {
                        currentUsage[targetBucketId] = count - 1
                    }
                }
                folderUsageState = currentUsage
                folderUsageSnapshotForPersist = HashMap(currentUsage)

                val newStats = stats.copy(
                    processed = stats.processed - 1,
                    skipped = stats.skipped - 1
                )
                stats = newStats
                statsSnapshotForPersist = newStats

                // Keep both active and full-catalog sources in sync after undo.
                val originalItem = action.item
                applyMediaUpdateAcrossCatalogs(originalItem)
                
                // Silent Move Back
                scope.launch(Dispatchers.IO) {
                    try {
                         val targetDir = File(action.sourceParentPath)
                         if (!targetDir.exists()) targetDir.mkdirs()
                         
                         val fileName = File(item.path).name
                         val currentFile = File(android.os.Environment.getExternalStorageDirectory(), action.targetFolder + "/" + fileName)
                         
                         if (currentFile.exists()) {
                             val destFile = File(targetDir, fileName)
                             
                             // Check if permission available
                             val hasManage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                 android.os.Environment.isExternalStorageManager()
                             } else {
                                 false
                             }

                             if (hasManage) {
                                 val strings = getStrings(prefs.language)
                                 if (currentFile.renameTo(destFile)) {
                                     FileOperationManager.scanFile(context, currentFile.absolutePath)
                                     FileOperationManager.scanFile(context, destFile.absolutePath)
                                     
                                     if (action.isNewFolder) {
                                         val folderToDelete = currentFile.parentFile
                                         if (folderToDelete != null && folderToDelete.exists() && folderToDelete.listFiles()?.isEmpty() == true) {
                                             folderToDelete.delete()
                                         }
                                     }

                                     withContext(Dispatchers.Main) {
                                         Toast.makeText(context, strings.undoSuccess, Toast.LENGTH_SHORT).show()
                                     }
                                 } else {
                                     // Fallback copy
                                     currentFile.inputStream().use { input ->
                                         destFile.outputStream().use { output ->
                                             input.copyTo(output)
                                         }
                                     }
                                     if (destFile.exists() && destFile.length() == currentFile.length()) {
                                         currentFile.delete()
                                         FileOperationManager.scanFile(context, currentFile.absolutePath)
                                         FileOperationManager.scanFile(context, destFile.absolutePath)
                                         
                                         if (action.isNewFolder) {
                                             val folderToDelete = currentFile.parentFile
                                             if (folderToDelete != null && folderToDelete.exists() && folderToDelete.listFiles()?.isEmpty() == true) {
                                                 folderToDelete.delete()
                                             }
                                         }

                                         withContext(Dispatchers.Main) {
                                             Toast.makeText(context, strings.undoSuccess, Toast.LENGTH_SHORT).show()
                                         }
                                     }
                                 }
                             } else {
                                 // No permission (Android 10 or denied)
                                 withContext(Dispatchers.Main) {
                                     Toast.makeText(context, "Cannot undo move: Permission required", Toast.LENGTH_SHORT).show()
                                 }
                             }
                         }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val criteria = buildFilterCriteria()
        val allMediaSnapshot = allMedia
        val pendingTrashSnapshot = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
            addAll(prefs.pendingTrashIds)
        }
        val hiddenFoldersSnapshot = deckHiddenFolders()
        val batchSizeSnapshot = batchSize

        statePersistenceWriter.schedule(
            HomeUndoPersistenceSnapshot(
                persistProcessedIds = true,
                persistPendingTrashIds = persistPendingTrashIds,
                persistLikedIds = persistLikedIds,
                persistMovedIds = persistMovedIds,
                folderUsage = folderUsageSnapshotForPersist,
                stats = statsSnapshotForPersist
            ),
            delayMs = resolveHotStatePersistDelayMs()
        )

        val immediateSnapshot = homeFeedCoordinator.onUndoRestore(
            item = item,
            criteria = criteria,
            batchSize = batchSizeSnapshot,
            restoresPendingCount = processedRemoved,
            preserveVisibleOrder = backtrackModeEnabled,
            stableVisibleOrderIds = if (backtrackModeEnabled) homeSnappingCarouselBatchOrderIds else emptyList()
        )
        val snappingUndoPlacement = if (backtrackModeEnabled) {
            placeSnappingCarouselUndoAtCurrentPosition(
                items = immediateSnapshot.displayMedia,
                restoredItemId = item.id,
                preferredInsertIndex = carouselPageBeforeUndo
            )
        } else {
            null
        }
        displayMedia = snappingUndoPlacement?.display ?: immediateSnapshot.displayMedia
        isFullAlbumDone = immediateSnapshot.isFullAlbumDone
        if (backtrackModeEnabled) {
            if (snappingUndoPlacement != null) {
                resetHomeSnappingCarouselBatchOrder(snappingUndoPlacement.display)
            }
            val restoredIndex = (snappingUndoPlacement?.restoredIndex)
                ?: displayMedia.indexOfFirst { it.id == item.id }
            if (restoredIndex >= 0) {
                homeSnappingCarouselCurrentPage = restoredIndex
                homeSnappingCarouselPendingUndoPage = null
            } else if (homeSnappingCarouselUndoInFlightToken == undoAnimationToken) {
                homeSnappingCarouselUndoReleaseJob?.cancel()
                homeSnappingCarouselUndoReleaseJob = null
                homeSnappingCarouselUndoInFlightToken = null
            }
        } else {
            homeSnappingCarouselPendingUndoPage = null
        }
        currentBatchTotal = batchProgressSnapshot?.total ?: immediateSnapshot.displayMedia.size
        currentBatchConsumed = batchProgressSnapshot?.consumed?.coerceIn(0, currentBatchTotal) ?: 0
        scheduleUndoPlaybackGate(resolveHomePlaybackItem(immediateSnapshot.displayMedia))
        homeInteractionPerfController.onUndoRestored()

        pendingHomeUndoCatalogSync = if (action is UndoAction.Trash || action is UndoAction.Move) {
            HomeUndoCatalogSyncRequest(
                token = nextUndoAnimationToken(),
                item = item,
                criteria = criteria,
                batchSize = batchSizeSnapshot,
                allMediaSnapshot = allMediaSnapshot,
                pendingTrashIds = pendingTrashSnapshot,
                hiddenFolders = hiddenFoldersSnapshot,
                snappingCarouselInsertIndex = if (backtrackModeEnabled) carouselPageBeforeUndo else null
            )
        } else {
            null
        }
    }

    fun handleEmptyTrash(requestedIds: Set<Long>? = null) {
        val currentMode = deletionMode
        val activeProcessingIds = (requestedIds ?: trashProcessingIds ?: prefs.pendingTrashIds.toSet()).toSet()
        if (activeProcessingIds.isEmpty()) {
            trashBatchIds = null
            trashProcessingIds = null
            onTrashBatchComplete = null
            return
        }
        if (requestedIds != null || trashProcessingIds == null) {
            trashProcessingIds = activeProcessingIds
        }

        scope.launch(Dispatchers.IO) {
            val mediaById = allMedia.associateBy { it.id }
            val pendingOrdered = activeProcessingIds.toList()
            val batch = LinkedHashSet<Long>()
            val uriList = mutableListOf<Uri>()
            // Rows MediaStore does not treat as media (RAW such as .cr3/.nrw): the system delete and
            // trash requests reject them, and one of them fails the whole batch. They are handled apart.
            val nonMediaUris = LinkedHashMap<Long, Uri>()
            val filesCollectionPrefix = MediaStore.Files.getContentUri("external").toString() + "/"
            for (id in pendingOrdered) {
                val item = mediaById[id]
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && item != null &&
                    item.uri.toString().startsWith(filesCollectionPrefix)
                ) {
                    nonMediaUris[id] = item.uri
                    continue
                }
                batch.add(id)
                val idUris = mutableListOf<Uri>()
                if (item != null) {
                    // The catalog uri already points at the right collection; RAW files that
                    // MediaStore does not type as images only resolve through it (Files table).
                    idUris.add(item.uri)
                    if (item.isLivePhoto && item.livePhotoVideoUri != null) {
                        idUris.add(item.livePhotoVideoUri)
                    }
                } else {
                    var uri: Uri? = null
                    try {
                        context.contentResolver.query(
                            MediaStore.Files.getContentUri("external"),
                            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE),
                            "${MediaStore.Files.FileColumns._ID}=?",
                            arrayOf(id.toString()),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                uri = when (cursor.getInt(0)) {
                                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                                    // RAW and other untyped rows only exist in the Files table.
                                    else -> ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    val resolvedUri = uri
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        resolvedUri != null && resolvedUri.toString().startsWith(filesCollectionPrefix)
                    ) {
                        batch.remove(id)
                        nonMediaUris[id] = resolvedUri
                        continue
                    }
                    if (resolvedUri != null) {
                        idUris.add(resolvedUri)
                    }
                }
                val nextUriCount = uriList.size + idUris.size
                if (uriList.isNotEmpty() && nextUriCount > MEDIASTORE_URI_BATCH_LIMIT) {
                    batch.remove(id)
                    break
                }
                uriList.addAll(idUris)
            }
            val uris = uriList.distinct()

            // Non-media rows can only be removed directly, which needs all-files access on Android 11+
            // (below that, storage permission covers it). Without it they stay in the delete queue
            // instead of blocking every other photo.
            val nonMediaHandled = LinkedHashSet<Long>()
            val canDeleteNonMedia = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                android.os.Environment.isExternalStorageManager()
            if (nonMediaUris.isNotEmpty() && canDeleteNonMedia) {
                nonMediaUris.forEach { (id, uri) ->
                    val removed = runCatching { context.contentResolver.delete(uri, null, null) > 0 }
                        .getOrDefault(false)
                    if (removed) nonMediaHandled.add(id)
                }
            }
            if (nonMediaUris.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val currentPending = prefs.pendingTrashIds
                    if (nonMediaHandled.isNotEmpty()) {
                        currentPending.removeAll(nonMediaHandled)
                        prefs.pendingTrashIds = currentPending
                        pendingTrashCount = currentPending.size
                        allMedia = removeMediaByIds(allMedia, nonMediaHandled)
                        doSyncCatalogAfterMediaRemoval(nonMediaHandled)
                        displayMedia = removeMediaByIds(displayMedia, nonMediaHandled)
                        homeFeedCoordinator.alignCurrentDisplay(displayMedia)
                        pruneHomeUndoStateForRemoved(nonMediaHandled)
                    }
                    // Nothing else to do for this pass; later passes only look at media rows.
                    trashProcessingIds = (activeProcessingIds - nonMediaUris.keys).takeIf { it.isNotEmpty() }
                }
                if (batch.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        trashBatchIds = null
                        onTrashBatchComplete = null
                        trashProcessingIds = null
                    }
                    return@launch
                }
            }

            if (uris.isEmpty()) {
                withContext(Dispatchers.Main) {
                    val currentPending = prefs.pendingTrashIds
                    currentPending.removeAll(batch)
                    prefs.pendingTrashIds = currentPending
                    pendingTrashCount = currentPending.size

                    val remainingProcessing = activeProcessingIds.filterNot { batch.contains(it) }.toSet()
                    if (currentPending.isEmpty()) {
                        undoStack = emptyList()
                        batchProgressUndoStack = emptyList()
                        clearHomeUndoAnimationTargets()
                        clearHomeSnappingCarouselUndoPage()
                    } else {
                        pruneHomeUndoStateForRemoved(batch)
                    }

                    trashBatchIds = null
                    trashProcessingIds = remainingProcessing.takeIf { it.isNotEmpty() }
                    onTrashBatchComplete = null
                    if (remainingProcessing.isNotEmpty()) handleEmptyTrash()
                }
                return@launch
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Set batch state on Main thread
                withContext(Dispatchers.Main) {
                    trashProcessingIds = activeProcessingIds
                    trashBatchIds = batch
                    trashBatchMode = currentMode
                    onTrashBatchComplete = { handleEmptyTrash() }
                }

                try {
                    val pi = if (currentMode == "recycle_bin") {
                        MediaStore.createTrashRequest(context.contentResolver, uris, true)
                    } else {
                        MediaStore.createDeleteRequest(context.contentResolver, uris)
                    }

                    withContext(Dispatchers.Main) {
                        deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        trashBatchIds = null
                        trashProcessingIds = null
                        onTrashBatchComplete = null
                    }
                }
            } else {
                // Older Android versions
                uris.forEach { try { context.contentResolver.delete(it, null, null) } catch (e: Exception) {} }
                withContext(Dispatchers.Main) {
                    val currentPending = prefs.pendingTrashIds
                    currentPending.removeAll(batch)
                    prefs.pendingTrashIds = currentPending

                    if (batch.isNotEmpty()) {
                        allMedia = removeMediaByIds(allMedia, batch)
                        doSyncCatalogAfterMediaRemoval(batch)
                        pruneLowQualityUndoForRemoved(batch)
                    }

                    pendingTrashCount = currentPending.size

                    val remainingProcessing = activeProcessingIds.filterNot { batch.contains(it) }.toSet()
                    trashBatchIds = null
                    trashProcessingIds = remainingProcessing.takeIf { it.isNotEmpty() }
                    onTrashBatchComplete = null

                    if (remainingProcessing.isNotEmpty()) {
                        pruneHomeUndoStateForRemoved(batch)
                        handleEmptyTrash()
                    } else if (currentPending.isNotEmpty()) {
                        pruneHomeUndoStateForRemoved(batch)
                    } else {
                        val retainedUndoEntries = undoStack.mapIndexed { index, undoAction ->
                            undoAction to batchProgressUndoStack.getOrElse(index) {
                                BatchProgressSnapshot(
                                    total = currentBatchTotal,
                                    consumed = currentBatchConsumed
                                )
                            }
                        }.filter { (undoAction, _) ->
                            undoAction is UndoAction.Keep
                        }
                        undoStack = retainedUndoEntries.map { it.first }
                        batchProgressUndoStack = retainedUndoEntries.map { it.second }
                        clearHomeUndoAnimationTargets()
                        clearHomeSnappingCarouselUndoPage()
                    }
                }
            }
        }
    }

    val returnToModernStartupWelcomeFromHome: () -> Unit = {
        returnToModernStartupWelcomeAfterSubpage = false
        suppressStartupHomeEntryOnce = true
        startupHomeTransitionRunning = false
        showStartupWelcomeCard = true
    }

    PredictiveBackWrapper(
        isVisible = isRootState,
        enableCustomPredictive = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        onBack = {
            if (modernHomeNavigationEnabled && !showStartupWelcomeCard) {
                returnToModernStartupWelcomeFromHome()
            } else {
                (context as? Activity)?.finish()
            }
        }
    ) {
        val currentWelcomeCopy = startupWelcomeCopy
        val dismissStartupWelcomeCard: (Boolean, () -> Unit) -> Unit = { suppressHomeEntry, onDismissed ->
            val copyKey = currentWelcomeCopy.key
            startupLastGreetingKey = copyKey
            context.getSharedPreferences("voicelike_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("startup_last_greeting_key", copyKey)
                .apply()
            isInitialLoad = true
            suppressStartupHomeEntryOnce = suppressHomeEntry
            if (!suppressHomeEntry) {
                entryAnimationToken++
            }
            if (suppressHomeEntry) {
                onDismissed()
            }
            showStartupWelcomeCard = false
            if (!suppressHomeEntry) {
                scope.launch {
                    withFrameNanos { }
                    onDismissed()
                }
            }
        }
        // 闂?闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柤纰卞墻濡茬兘姊绘担鍛婃儓婵炲眰鍨藉畷鍦喆閸曨剙鍔呮繝鐢靛Т濞诧箓鎮″☉銏″€堕柣鎰邦杺閸ゆ瑩鏌涢弮鈧銊ф閹烘鍋愰柛鎰絻閹介潧螖閻橀潧浠﹂柣妤佹礉瑜颁礁顪冮妶鍡楀潑闁稿鎸搁埞鎴︽闁稿鍔楀Σ鎰板箳濡も偓閻掑灚銇勯幒宥囶槮缂佸墎鍋ら弻鐔兼焽閿曗偓楠炴﹢鏌涘鍡曠凹缂佺粯绻堥幃浠嬫濞磋翰鍨荤槐鎺旂磼濡搫顫掗梺璇″灟缁€渚€鈥﹂妸鈺侀唶闁绘柨鎼獮鍫ユ⒑鐠囨彃鍤辩紒鎻掝煼瀹曟繄浠﹂悙顒€寮块梺绋挎湰婢规洟宕戦幘鏂ユ灁闁割煈鍠楅悘鍫ユ⒑缂佹澧柕鍫㈩焾閻ｅ嘲煤椤忓嫮鍔撮梺鍛婂姦娴滅偤骞婂┑瀣拺闂侇偆鍋涢懟顖涙櫠鐎电硶鍋撶憴鍕┛缂傚秳绀侀悾鐑芥偄绾拌鲸鏅┑鐐村灦閼瑰墽鏁ィ鍐┾拻濞达綀娅ｉ妴濠囨煕閹惧绠為柕鍡楀暢缁犳盯骞橀幇鍓佺К闁靛洦鍔欓獮鎺楀箻閺傘儱浜鹃柣銏犳啞閻撶喖鏌曡箛瀣偓鏍р槈瑜旈幃浠嬵敍濮樺崬鐓熼梺纭呮閻栧ジ寮幇顓炵窞閻庯綆鍓欓獮鍫熺節閻㈤潧鈻堟繛浣冲吘娑樜旈崪浣规櫆闂佸啿鎼崯鐘诲磻閹炬枼鏋旈柛顭戝枟閻忓秴顪冮妶鍐ㄥ闁绘绻愬嵄闁圭増婢樼粻濠氭倵濞戞顏堫敁閹剧粯鈷戦柛娑橈功缁犳捇鎮楀鐓庢灕闁靛洦鍔曢埢搴ㄥ箻缁瀚奸梻浣告啞缁嬫垿鏁冮妷锕€绶為柛鏇ㄥ灡閻撴洟鐓崶銊︹拻闁靛棙甯￠弻娑㈠箳閹捐櫕璇炲銈冨灪缁嬫垿鍩ユ径濞㈡棃宕橀鍡樻瘞闂傚倸鍊风欢姘焽瑜旈幃褔宕卞☉妯肩枃闁瑰吋鐣崝宀勬倿閸偁浜滈柟鍝勬娴滃墽绱撻崒姘毙㈤柨鏇ㄤ簻椤曪綁顢曢敃鈧粈鍐┿亜閺囩偞鍣洪柡鍛箞濮婃椽妫冨☉姘暫濠碘槅鍋掗崢濂糕€﹂崶顒佸亜閻忓繋鐒﹂弬鈧梻浣虹帛椤洨鍒掗姘ｆ鐟滃海鎹㈠☉姘勃闁稿本鑹鹃～鍥⒑缁洘鏉洪柛銊ㄦ椤曪綁顢氶埀顒€鐣烽幒鎳虫棃鍩€椤掍胶顩查柣鎰靛墻濞堜粙鏌ｉ幇顖氱毢濞寸姰鍨介弻娑㈠籍閳ь剙鐣濋幖浣歌摕闁挎繂鎲橀弮鈧幏鍛存惞閸︻厼甯庡┑锛勫亼閸娿倝宕㈤悡骞熸椽濡搁埡浣侯唹闂佸憡娲﹂崜娑㈡⒔閸曨垱鐓㈡俊顖欒濡茶櫣绱掓径瀣仢闁哄瞼鍠栭、娆戞喆閸曨剛褰嗙紓鍌欑劍椤ㄥ牓宕伴弽顓溾偓浣糕槈濮楀棙鍍靛銈嗘尵閸犳劙鎯佸鍛＝闁稿本鑹鹃埀顒佹倐瀹曟劙骞栨担鍝ワ紮闂佸憡绺块崕鎶芥儗閹剧粯鍊甸梻鍫熺⊕閹叉悂鏌ｉ鐔稿磳闁哄矉缍侀幃銏㈢矙濞嗙偓顥嬮梻浣告惈濡挳姊介崟顖氱疄闁靛鍎哄〒濠氭偣閸ヮ亜绱﹀瑙勬礋閺岋絾鎯旈妶搴㈢秷濠电偞褰冪换妯虹暦濠靛绠ｉ柨鏃囥€€閸嬫挻鎷呴懖婵堝枎閳诲酣鎮欓顐熸櫊濮婄粯鎷呴崨濠冨創闂佸綊鏀遍崹鍦閻愬瓨濯撮柛鎾冲级缂嶅骸鈹戦悙鍙夆枙濞存粍绻堝畷鎴︽晸閻樺磭鍘靛┑鐐茬墕閻忔繈寮稿▎鎴犵＜妞ゆ梻鍋撻弳顒佹叏婵犲啯銇濈€规洜鍏橀、妯款槾闁告梻顭堥—鍐Χ鎼粹€崇闂佹悶鍨洪悡锟犮€?
        // photoo:// links: the photo wall opens through its home entry, organize starts like the Start
        // button. Without photo access both stop on the Organize home.
        val pendingDeepLink by PhotooDeepLinkCenter.pending.collectAsState()
        LaunchedEffect(pendingDeepLink) {
            val link = pendingDeepLink ?: return@LaunchedEffect
            PhotooDeepLinkCenter.consume(link)
            // A link lands on top of whatever page was open: close the pages above the tabs first.
            showSettings = false
            showStatsOverlay = false
            showShootingStats = false
            showDeepOrganize = false
            showSimilarPhotos = false
            showLowQuality = false
            showLikes = false
            showKeepGallery = false
            showMonthly = false
            showDeleteModal = false
            showRecycleBin = false
            showLanguage = false
            showSwipePreferences = false
            showFilter = false
            showVideoCompress = false
            showImageCompress = false
            showCampfirePlan = false
            showHelpCenter = false
            if (!modernHomeNavigationEnabled) {
                modernHomeNavigationEnabled = true
                prefs.modernHomeNavigationEnabled = true
            }
            modernStartupWelcomeTab = StartupWelcomeTab.Organize.name
            if (!showStartupWelcomeCard) returnToModernStartupWelcomeFromHome()
            if (!hasMediaPermission) return@LaunchedEffect
            when (link) {
                PhotooDeepLink.PhotoWall -> showPhotoWall = true
                PhotooDeepLink.Organize -> {
                    if (showPhotoWall) closePhotoWall()
                    // Consuming the link restarts this effect, so nothing here may suspend.
                    if (!startupHomeTransitionRunning) startupHomeTransitionRunning = true
                }
            }
        }
        // Organizing from the home range panel: close the panel, then start like the Start button.
        LaunchedEffect(startOrganizeFromHomeAfterRangeSignal) {
            if (startOrganizeFromHomeAfterRangeSignal == 0) return@LaunchedEffect
            delay(450L)
            if (showStartupWelcomeCard && !startupHomeTransitionRunning) {
                startupHomeTransitionRunning = true
            }
        }
        LaunchedEffect(startupHomeTransitionRunning, showStartupWelcomeCard) {
            if (!startupHomeTransitionRunning || !showStartupWelcomeCard) return@LaunchedEffect
            delay(520L)
            dismissStartupWelcomeCard(false) {}
            startupHomeTransitionRunning = false
        }
        val homeChromeRevealProgress = if (showStartupWelcomeCard) {
            startupHomeTransitionProgress
        } else {
            1f
        }
        val homeTopRevealProgress = stagedProgress(homeChromeRevealProgress, start = 0.06f, end = 0.52f)
        val homeSideButtonRevealProgress = stagedProgress(homeChromeRevealProgress, start = 0.20f, end = 0.74f)
        val homeUndoRevealProgress = stagedProgress(homeChromeRevealProgress, start = 0.28f, end = 0.82f)
        val homeQuickDrawerRevealProgress = stagedProgress(homeChromeRevealProgress, start = 0.34f, end = 1f)
        val modernWatermarkPresetImagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val presetKey = pendingModernWatermarkPickerPresetKey
            pendingModernWatermarkPickerPresetKey = null
            if (presetKey == null || result.resultCode != Activity.RESULT_OK) {
                return@rememberLauncherForActivityResult
            }
            val uri = result.data?.data
                ?: result.data?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                ?: return@rememberLauncherForActivityResult
            if (modernHomeNavigationEnabled && showStartupWelcomeCard) {
                openWatermarkFramePage(
                    presetKey = presetKey,
                    initialImageUriString = uri.toString()
                )
            } else dismissStartupWelcomeCard(true) {
                openWatermarkFramePage(
                    presetKey = presetKey,
                    initialImageUriString = uri.toString()
                )
            }
        }
        val launchModernWatermarkPresetImagePicker: (String) -> Unit = { presetKey ->
            pendingModernWatermarkPickerPresetKey = presetKey
            modernWatermarkPresetImagePickerLauncher.launch(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "image/*"
                }
            )
        }

        val openSubpageFromStartupWelcome: (() -> Unit) -> Unit = { onOpen ->
            if (modernHomeNavigationEnabled && showStartupWelcomeCard) {
                onOpen()
            } else {
                dismissStartupWelcomeCard(true, onOpen)
            }
        }

        if (showStartupWelcomeCard) {
            val selectedModernStartupTab = runCatching {
                StartupWelcomeTab.valueOf(modernStartupWelcomeTab)
            }.getOrDefault(StartupWelcomeTab.Organize).let { savedTab ->
                if (savedTab == StartupWelcomeTab.Watermark) {
                    StartupWelcomeTab.ColorCard
                } else {
                    savedTab
                }
            }
            val modernMineLikedIdsSize = prefs.likedIds.size
            val modernMineLikedPhotos = remember(insightsMedia, modernMineLikedIdsSize) {
                val likedSet = prefs.likedIds.toSet()
                insightsMedia
                    .asSequence()
                    .filter { it.type == "photo" && likedSet.contains(it.id) }
                    .distinctBy { it.id }
                    .sortedByDescending { it.dateAdded }
                    .toList()
            }
            StartupWelcomeCard(
                copy = currentWelcomeCopy,
                loading = loading,  // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗霉閿濆浜ら柤鏉挎健濮婃椽顢楅埀顒傜矓椤曗偓閸┾偓妞ゆ帒锕﹂悾鐢碘偓瑙勬礀閵堝憡淇婃搴樺亾閿濆簼绨奸柛鐘筹耿閺岀喖顢欓悙顒佺彧缂備緡鍠楅悷褔骞戦崟顖椻偓锕傚箣濠靛洦姣嗛梻鍌氬€搁崐鐑芥倿閿曞倸纾块柛鎰梿閻熼偊娼╅悹楦挎閿涙盯姊洪崨濠冨闁搞劌銈稿顐も偓锝庡枛缁狙囨煕椤愶絿鈽夊┑鈥茬矙閺屾稑鈻庤箛鏇狀唹闂侀€涚┒閸斿矁鐏冮梺閫炲苯澧摶鐐翠繆閵堝懏鍣界€规挷鐒︽穱濠囶敍濞嗘帩鍔呴梺缁樻煥濡繈寮婚敐澶嬪亜缂佸顑欏Λ鍡涙⒑缂佹ê绗氶柟顔煎€垮?loading 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢幘鑼槮闁搞劍绻冮妵鍕冀椤愵澀姹楅梺閫炲苯澧剧紒鐘虫尭閻ｉ攱绺界粙璇俱劑鏌曟径娑㈡鐞氾綁姊婚崒娆戭槮闁圭⒈鍋嗙划娆愮瑹閳ь剙鐣烽姀锛勯檮闁告稑锕ら埀顒€娼￠弻娑⑩€﹂幋婵呯按婵炲瓨绮嶇划鎾诲蓟閻斿憡缍囬柛鎾楀惙鎴犵磼缂併垹骞栭柣鏍с偢瀵鈽夐姀鐘愁棟闁荤姵浜介崜閬嶆晲閸氬啿閰ｅ畷鎯邦檪闂婎剦鍓氶妵鍕閿涘嫧妲堝銈庡亝缁诲牆鐣峰Δ浣哥窞妤犵偛鐏濇俊璺ㄧ磼鏉堛劍灏扮紒妤冨枛瀹曟儼顦抽柣蹇撶墦濮婅櫣绱掑鍡樼暦闂佺懓鎲￠幃鍌炲箖妤ｅ啯鍊荤紒娑橆儐閺呮繈姊洪幐搴㈢５闁稿鎸剧槐鎺楀焵椤掑倵鍋撻敐搴′簴濞存粍绮撻弻鐔稿鐎涙ɑ閿繛瀛樼矋缁诲倿鍩為幋锕€纾兼繛鎴炶壘閳峰苯鈹戦纭锋敾婵＄偠妫勯悾鐑筋敃閿曗偓缁€瀣煕椤垵鈧綁濡搁埡鍌楁嫼缂備礁顑嗛娆撳磿閹存績鏀芥い鏃傚亾閺嗩剚顨ラ悙鑼ⅵ濠碘剝鎮傞崺鈩冩媴閸曟挸鍠氶悢鍡涙偣閾忕懓鐨戠€规悶鍎甸弻锝嗘償椤旂厧绫嶅┑顔硷攻濡炶棄螞閸愵煁褰掑Χ閸℃瑦鍒涢悗瑙勬磸閸旀垿銆佸☉銏犖ч柛銉戝啰浠繝鐢靛Х椤ｎ喚妲愰弴銏犵；闁硅揪绠戠壕褰掓煛瀹ュ骸骞栭柦鍐枛閺屾洘绻涢崹顔嘉╂繛瀛樼矊缂嶅﹪寮婚敐澶婄疀妞ゆ帒鍊风划闈涱渻?
                pendingDeleteCount = pendingTrashCount,
                startTransitionActive = startupHomeTransitionRunning,
                startTransitionProgress = startupHomeTransitionProgress,
                onStartTransitionRequested = {
                    if (!startupHomeTransitionRunning) {
                        startupHomeTransitionRunning = true
                    }
                },
                onStatsRequested = {
                    openSubpageFromStartupWelcome {
                        showStatsOverlay = true
                    }
                },
                onDeleteQueueRequested = if (pendingTrashCount > 0) {
                    {
                        openSubpageFromStartupWelcome {
                            openDeleteQueue()
                        }
                    }
                } else {
                    null
                },
                onLikesRequested = {
                    openSubpageFromStartupWelcome {
                        prefs.likesDisplayMode = LikesDisplayMode.TikTok.toPrefValue()
                        showWatermarkFrame = false
                        pendingModernWatermarkPresetKey = null
                        pendingModernWatermarkInitialImageUri = null
                        pendingModernWatermarkPickerPresetKey = null
                        replacePendingWatermarkBackdropBitmap(null)
                        showLikes = true
                    }
                },
                onColorCardRequested = if (colorCardFeatureEnabled) {
                    {
                        openSubpageFromStartupWelcome {
                            pendingModernColorCardFilterPaletteId = null
                            showColorCards = true
                        }
                    }
                } else {
                    null
                },
                onColorCardFilterRequested = if (colorCardFeatureEnabled) {
                    { paletteId ->
                        openSubpageFromStartupWelcome {
                            pendingModernColorCardFilterPaletteId = paletteId
                            showColorCards = true
                        }
                    }
                } else {
                    null
                },
                onColorCardCameraRequested = if (colorCardFeatureEnabled) {
                    {
                        openSubpageFromStartupWelcome {
                            showColorCardCamera = true
                        }
                    }
                } else {
                    null
                },
                onStampRequested = {
                    openSubpageFromStartupWelcome {
                        showStampCamera = true
                    }
                },
                onElectronicAlbumRequested = {
                    openSubpageFromStartupWelcome {
                        openStandaloneElectronicAlbum()
                    }
                },
                onPromptManagerRequested = {
                    openSubpageFromStartupWelcome {
                        showPromptManager = true
                    }
                },
                onFillLightRequested = {
                    openSubpageFromStartupWelcome {
                        showFillLight = true
                    }
                },
                onWatermarkRequested = {
                    openSubpageFromStartupWelcome {
                        openWatermarkFramePage()
                    }
                },
                onWatermarkPresetRequested = { presetKey ->
                    launchModernWatermarkPresetImagePicker(presetKey)
                },
                modernNavigationEnabled = modernHomeNavigationEnabled,
                modernMineStats = stats,
                modernMineTotalPhotos = maxOf(mediaTotalCount, insightsMedia.size),
                modernMineTotalSizeBytes = mediaTotalSizeBytes,
                modernMineAvatarType = avatarType,
                modernMineCustomAvatarPath = customAvatarPath,
                modernMineLikedPhotos = modernMineLikedPhotos,
                modernMineAllMedia = allMedia,
                modernMineLikedIds = prefs.likedIds.map { it.toLong() }.toSet(),
                selectedModernTab = selectedModernStartupTab,
                onModernTabSelected = { tab ->
                    modernStartupWelcomeTab = tab.name
                },
                onShootingStatsRequested = {
                    openSubpageFromStartupWelcome {
                        openShootingStatsPage()
                    }
                },
                onDeepOrganizeRequested = {
                    openSubpageFromStartupWelcome {
                        showDeepOrganize = true
                    }
                },
                onKeepGalleryRequested = {
                    openSubpageFromStartupWelcome {
                        openKeepGallery()
                    }
                },
                onRecycleBinRequested = {
                    openSubpageFromStartupWelcome {
                        showRecycleBin = true
                    }
                },
                onMineGlobeRequested = {
                    openSubpageFromStartupWelcome {
                        showLabCopyrightVerify = false
                        showLabAlbumSettings = false
                        showLabLiveSyncTool = false
                        showElectronicAlbumPlayback = false
                        showLab = false
                        showLabGlobe = true
                    }
                },
                onLabRequested = {
                    openSubpageFromStartupWelcome {
                        electronicAlbumStandaloneEntry = false
                        showLabCopyrightVerify = false
                        showLabAlbumSettings = false
                        showLab = true
                    }
                },
                onCampfireRequested = {
                    openSubpageFromStartupWelcome {
                        campfireOpenVersion += 1
                        showCampfirePlan = true
                    }
                },
                onHelpCenterRequested = {
                    if (isHelpCenterVisibleForLanguage(currentLanguage)) {
                        openSubpageFromStartupWelcome {
                            showHelpCenter = true
                        }
                    } else {
                        Toast.makeText(
                            context,
                            getHelpCenterUnavailableMessage(currentLanguage),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onSettingsRequested = {
                    openSubpageFromStartupWelcome {
                        settingsDestination = SettingsDestination.Home
                        showSettings = true
                    }
                },
                onMineAvatarTypeChange = { type ->
                    avatarType = type
                    prefs.avatarType = type
                },
                onMineCustomAvatarPathChange = { path ->
                    customAvatarPath = path
                    prefs.customAvatarPath = path
                },
                onStartRequested = {
                    dismissStartupWelcomeCard(false) {}
                },
                organizeScope = StartupOrganizeScope(
                    label = if (organizeRangeCriteria.isActive) {
                        organizeRangeTitle(organizeRangeCriteria, currentLanguage)
                    } else {
                        organizeSortingStrings(currentLanguage).allPhotos
                    },
                    active = organizeRangeCriteria.isActive,
                    remainingCount = organizeRangeRemainingCount,
                    onOpen = { showFilter = true },
                    onClear = {
                        setOrganizeRange(MediaFilterCriteria())
                        isFullAlbumDone = false
                        loadNextBatch(playEntryAnimation = true)
                    }
                ),
                insightsContent = {
                    InsightsTabContent(
                        state = insightsUiState,
                        allMedia = insightsCatalogMedia,
                        likedIds = insightsLikedIds
                    )
                },
                organizeHome = StartupOrganizeHome(
                    deckItems = if (hasMediaPermission) displayMedia.take(3) else emptyList(),
                    hasPermission = hasMediaPermission,
                    pendingDeleteCount = pendingTrashCount,
                    todayOrganized = organizedToday,
                    savedBytes = stats.savedSize,
                    scopeHasPhotos = organizeRangeSummary?.second ?: true,
                    onPendingDelete = { openSubpageFromStartupWelcome { openDeleteQueue() } },
                    onProgress = { openSubpageFromStartupWelcome { showStatsOverlay = true } },
                    onRequestPermission = onRequestMediaPermission,
                    onDeepOrganize = { openSubpageFromStartupWelcome { showDeepOrganize = true } },
                    onSimilar = { openSubpageFromStartupWelcome { showSimilarPhotos = true } },
                    onLowQuality = { openSubpageFromStartupWelcome { showLowQuality = true } },
                    onVideoCompress = { openSubpageFromStartupWelcome { showVideoCompress = true } },
                    onImageCompress = { openSubpageFromStartupWelcome { showImageCompress = true } },
                    onRecycleBin = { openSubpageFromStartupWelcome { showRecycleBin = true } },
                    photoWallPreview = organizeRangeSummary?.third.orEmpty(),
                    onPhotoWall = { openSubpageFromStartupWelcome { showPhotoWall = true } }
                )
            )
        }
        val topCardId = resolveHomeFocusedItem()?.id
        val topCardUiState = rememberHomeTopCardUiState(
            topCardId = topCardId,
            lensyEnabled = effectiveLensyEnabled
        )
        val isFolderDropMode = topCardUiState.isFolderDropModeActive

        if (true) {
            if (true) {
            // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｉ幇顒佹儓闁搞劌鍊块弻娑㈩敃閿濆棛顦ョ紓浣哄У瑜板啴婀侀梺鎸庣箓閹冲繘骞夐幖浣圭厸闁糕槅鍘鹃悾鐢告煛瀹€瀣М濠殿喒鍋撻梺闈涚箚閺呮繈宕濋幖浣光拺婵懓娲ら埀顒侇殜瀹曟垿骞樼拠鍙傦箓鏌涢弴銊ョ仩缂佺姵鐩弻娑㈩敃閿濆棛顦ㄥ銈呴缁夋挳鈥旈崘顔嘉ч柛鎰╁妼婵酣姊虹紒姗嗘畷闁圭懓娲ら锝嗙節濮橆剙宓嗛梺鎸庣☉鐎氼噣寮堕幖浣光拺闁告繂瀚婵嬫煕婵犲骸浜板┑顔瑰亾闂侀潧鐗嗗Λ娑㈠储閸涘﹦绠鹃弶鍫濆⒔閸掍即鏌熼懞銉х煉鐎规洘妞介弫鎰緞鐎ｎ剙骞?
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
    Box(
        modifier = Modifier
            .matchParentSize()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (showStartupWelcomeCard) {
                        Color.Transparent
                    } else if (isDark) {
                        MaterialTheme.colorScheme.background
                    } else {
                        Color(0xFFE5E5E5)
                    }
                )
        )
    }
    val topCardId = resolveHomeFocusedItem()?.id
    val topCardUiState = rememberHomeTopCardUiState(
        topCardId = topCardId,
        lensyEnabled = effectiveLensyEnabled
    )
    LaunchedEffect(topCardUiState.isDragging, isInitialLoad, entryAnimationToken) {
        if (!isInitialLoad || !topCardUiState.isDragging) return@LaunchedEffect
        cardEntryOffsets.forEach { it.snapTo(0f) }
        cardEntryRotations.forEach { it.snapTo(0f) }
        isInitialLoad = false
    }
    // Increased threshold to 300f to prevent accidental triggers (previously 150f)
    // In quick-organize mode, folder move is handled by bottom capsules; disable down-swipe folder-drop mode.
    val isFolderDropMode = topCardUiState.isFolderDropModeActive
    var folderDropTargets by remember { mutableStateOf<List<FolderData>>(emptyList()) }
    var activeDropTargetId by remember { mutableStateOf<String?>(null) }
    val folderDropGestureController = remember { FolderDropGestureController(DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX) }
    var folderDropTargetRects by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var cardResetToken by remember { mutableStateOf(0) }
    val hiddenFoldersForMoveTargets = if (hiddenFoldersAffectMoveTargets) hiddenFolders else emptySet()
    val mediaForMoveTargets = remember(insightsMedia, pendingTrashCount, quickOrganizeMode, isFolderDropMode) {
        if (!quickOrganizeMode && !isFolderDropMode) {
            emptyList()
        } else {
            selectActiveCatalogMedia(
                allMedia = insightsMedia,
                pendingTrashIds = HashSet<Long>().apply {
                    addAll(prefs.pendingTrashIds)
                },
                hiddenFolders = emptySet()
            )
        }
    }

    fun quickMoveToFolder(sourceItem: MediaItem, folder: FolderData): Boolean {
        if (folder.id == "new_folder") return false
        val fallbackBaseDir = if (sourceItem.type == "video") {
            android.os.Environment.DIRECTORY_MOVIES
        } else {
            android.os.Environment.DIRECTORY_PICTURES
        }
        val fallbackName = folder.name.trim().ifBlank { folder.id.trim() }
        val fallbackPath = "$fallbackBaseDir/$fallbackName"
        val targetFolder = (folder.path ?: fallbackPath)
            .trim()
            .trimEnd('/')
            .plus("/")
            .takeIf { it.isNotBlank() } ?: return false
        scope.launch {
            try {
                folderUsageState = markQuickOrganizeFolderUsed(prefs, folder.id)

                val intentSender = FileOperationManager.moveMedia(context, listOf(sourceItem), targetFolder)
                if (intentSender != null) {
                    pendingMoveArgs = Triple(listOf(sourceItem), targetFolder, null)
                    pendingMoveCommitOnGrant = true
                    val parent = sourceItem.path
                        ?.let { File(it).parentFile?.absolutePath }
                        .orEmpty()
                    pendingMoveUiAction = {
                        val moved = buildMovedMediaItem(
                            sourceItem = sourceItem,
                            targetFolderPath = targetFolder,
                            targetBucketName = folder.name
                        )
                        applyMediaUpdateAcrossCatalogs(moved)
                        handleSwipe(UndoAction.Move(sourceItem, parent, targetFolder))
                    }
                    intentSenderLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } else {
                    moveFeedbackMessage = "${strings.moveTo} $targetFolder"
                    val newItem = buildMovedMediaItem(
                        sourceItem = sourceItem,
                        targetFolderPath = targetFolder,
                        targetBucketName = folder.name
                    )
                    applyMediaUpdateAcrossCatalogs(newItem)
                    val parent = sourceItem.path
                        ?.let { File(it).parentFile?.absolutePath }
                        .orEmpty()
                    handleSwipe(UndoAction.Move(sourceItem, parent, targetFolder))
                }
            } catch (e: Exception) {
                AppRuntimeLogStore.logError(context, "QuickMove", "Failed to move item ${sourceItem.id}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Move failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    val homeGlassBackdrop = rememberLiquidGlassBackdrop()
    if (!showStartupWelcomeCard) {
    // Wide landscape windows dock the current card's details beside the deck (OrganizeSplitLayout).
    OrganizeSplitLayout(
        detailItem = resolveHomeFocusedItem(),
        onJumpToDay = { selectedDay ->
            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            setOrganizeRange(MediaFilterCriteria().withTime(day = selectedDay))
            loadNextBatch(playEntryAnimation = true)
        }
    ) {
    val deckRangeActive = buildFilterCriteria().isActive
    val deckRemainingCount = if (displayMedia.isEmpty()) {
        homeFeedCoordinator.remainingCount(buildFilterCriteria(), prefs.processedIds)
    } else {
        0
    }
    // A selected range never stops for a batch-done card: when a capped range deck runs
    // out and more of the range is left, the next deck deals in straight away.
    val rangeAutoAdvancePending = displayMedia.isEmpty() &&
        deckRangeActive &&
        deckRemainingCount > 0 &&
        hasMediaPermission &&
        hasLoadedOnce &&
        !backtrackModeEnabled
    LaunchedEffect(rangeAutoAdvancePending) {
        if (!rangeAutoAdvancePending) return@LaunchedEffect
        delay(260L)
        loadNextBatch(playEntryAnimation = true)
    }
    if (displayMedia.isEmpty() && !rangeAutoAdvancePending) {
        val remainingCount = deckRemainingCount

        EmptyState(
            onRestart = { loadNextBatch(playEntryAnimation = true) },
            hasMediaPermission = hasMediaPermission,
            onRequestPermission = onRequestMediaPermission,
            pendingTrashCount = pendingTrashCount,
            onViewTrash = { openDeleteQueue() },
            filterFolder = filterFolder,
            filterType = filterType,
            filterMonth = filterMonth,
            filterDay = filterDay,
            remainingCount = remainingCount,
            isFullAlbumDone = isFullAlbumDone,
            onResetFilter = {
                setOrganizeRange(MediaFilterCriteria())
                isFullAlbumDone = false
                loadNextBatch(playEntryAnimation = true)
            },
            onReleaseKept = {
                releaseKeptForCurrentFilter {
                    loadNextBatch()
                }
            },
            onViewLikes = { showLikes = true },
            onChangeFilter = {
                showFilter = true
            },
            quickOrganizeMode = quickOrganizeMode,
            isRangeActive = deckRangeActive,
            onBack = { returnToModernStartupWelcomeFromHome() }
        )
    }
        // Card Stack
        
        // Calculate Folder Targets
        // var folderUsageState by remember { mutableStateOf(prefs.folderUsage) } // Moved up to VoiceLikeApp scope
        // var folderLastUsedState by remember { mutableStateOf(prefs.folderLastUsed) } // Need this for reactive sorting?
        // Actually, we can just read prefs.folderLastUsed inside the LaunchedEffect or pass it as state if we want instant UI updates.
        // Let's create a local state for it to trigger sorting updates.
        val folderLastUsedState = remember { mutableStateOf(prefs.folderLastUsed) }
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }
        val folderListState = rememberLazyListState()

if (displayMedia.isNotEmpty()) {
        // Keep the home chrome alive even when the feed is empty; only the card stack itself disappears.

if (showNewFolderDialog) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = { 
            showNewFolderDialog = false 
            cardResetToken++
        },
        title = { Text(strings.createNewFolder) },
        text = {
            OutlinedTextField(
                value = newFolderName,
                onValueChange = { newFolderName = it },
                label = { Text(strings.folderName) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Allow empty input for default name
                    val finalFolderName = if (newFolderName.isBlank()) strings.newFolder else newFolderName
                    val item = resolveHomeFocusedItem()
                    if (quickOrganizeMode) {
                        val baseDir = if (item?.type == "video") {
                            android.os.Environment.DIRECTORY_MOVIES
                        } else {
                            android.os.Environment.DIRECTORY_PICTURES
                        }
                        scope.launch {
                            val created = createFolderOnlyForQuickMode(context, baseDir, finalFolderName)
                            folderUsageState = registerQuickOrganizeFolderMetadata(prefs, finalFolderName)
                            moveFeedbackMessage = if (created) {
                                "${strings.create}: ${finalFolderName.trim()}"
                            } else {
                                "${strings.create}: ${finalFolderName.trim()} (metadata only)"
                            }
                        }
                    } else if (item != null) {
                        scope.launch {
                            val baseDir = if (item.type == "video") android.os.Environment.DIRECTORY_MOVIES else android.os.Environment.DIRECTORY_PICTURES
                            val destFolder = "$baseDir/${finalFolderName.trim()}/"

                            val intentSender = FileOperationManager.moveMedia(context, listOf(item), destFolder)
                            if (intentSender != null) {
                                pendingMoveArgs = Triple(listOf(item), destFolder, null)
                                pendingMoveCommitOnGrant = true
                                val parent = item.path
                                    ?.let { File(it).parentFile?.absolutePath }
                                    .orEmpty()
                                pendingMoveUiAction = {
                                    val moved = buildMovedMediaItem(
                                        sourceItem = item,
                                        targetFolderPath = destFolder,
                                        targetBucketName = finalFolderName
                                    )
                                    applyMediaUpdateAcrossCatalogs(moved)
                                    handleSwipe(UndoAction.Move(item, parent, destFolder))
                                }
                                intentSenderLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            } else {
                                // Allow "sucked in" animation to play
                                delay(50)
                                moveFeedbackMessage = "${strings.moveTo} $destFolder"

                                val currentUsage = prefs.folderUsage
                                val newBucketId = finalFolderName.trim()
                                currentUsage[newBucketId] = 1
                                prefs.folderUsage = currentUsage
                                folderUsageState = currentUsage

                                val currentLastUsed = prefs.folderLastUsed
                                currentLastUsed[newBucketId] = System.currentTimeMillis()
                                prefs.folderLastUsed = currentLastUsed

                                val userCreated = prefs.userCreatedFolders
                                userCreated.add(newBucketId)
                                prefs.userCreatedFolders = userCreated

                                val newItem = buildMovedMediaItem(
                                    sourceItem = item,
                                    targetFolderPath = destFolder,
                                    targetBucketName = finalFolderName
                                )

                                applyMediaUpdateAcrossCatalogs(newItem)

                                val parent = item.path
                                    ?.let { File(it).parentFile?.absolutePath }
                                    .orEmpty()
                                handleSwipe(UndoAction.Move(item, parent, destFolder, isNewFolder = true))
                            }
                        }
                    }
                    showNewFolderDialog = false
                    newFolderName = ""

                }
            ) {
                Text(strings.create)
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                showNewFolderDialog = false
                cardResetToken++
            }) {
                Text(strings.cancel)
            }
        }
    )
}

val carouselSettlementRemainingCount = if (backtrackModeEnabled) {
    homeFeedCoordinator.remainingCount(buildFilterCriteria(), prefs.processedIds)
} else {
    0
}

// Floating chrome on this screen refracts the cards, so the card area is recorded for it.
Box(
    modifier = Modifier
        .fillMaxSize()
        .then(if (homeGlassBackdrop != null) Modifier.layerBackdrop(homeGlassBackdrop) else Modifier)
) {
    if (backtrackModeEnabled) {
        HomeSnappingCarouselArea(
            displayMedia = displayMedia,
            currentPage = resolveHomeCarouselPage(),
            pendingUndoPage = homeSnappingCarouselPendingUndoPage,
            undoAnimationTargets = homeSnappingCarouselUndoAnimationTargets,
            interactionLocked = homeSnappingCarouselUndoInFlightToken != null,
            isInitialLoad = isInitialLoad,
            entryAnimationToken = entryAnimationToken,
            settlementBatchToken = homeSnappingCarouselSettlementBatchToken,
            upSwipeSpeedMultiplier = swipeUpSpeedMultiplier,
            hiddenSourceItemId = sharedDismissHiddenSourceItemId,
            quickOrganizeMode = quickOrganizeMode,
            muteVideos = muteVideos,
            lensyEnabled = effectiveLensyEnabled,
            aiImageResultsEnabled = aiImageResultsEnabled,
            colorCardEnabled = colorCardFeatureEnabled,
            pauseMainVideos = pauseMainVideos,
            topCardPlaybackArmed = topCardPlaybackArmed,
            isFullScreenVideoOpen = fullScreenVideoItem != null,
            onCurrentPageChange = { homeSnappingCarouselCurrentPage = it },
            onSwipeUp = { item, restoreOffsetY ->
                handleSwipe(
                    UndoAction.Trash(
                        item = item,
                        direction = SwipeDirection.Up,
                        restoreOffsetY = restoreOffsetY
                    )
                )
            },
            onDeleteSound = { playDeleteSound() },
            onOpenDetail = { item, sourceBounds ->
                viewingItems = listOf(item)
                viewingInitialIndex = 0
                viewingSourceBounds = sourceBounds
                viewingSourceCardBoundsAtOpen = topCardUiState.topCardBoundsInRoot
                viewingSourceItemId = item.id
                sharedDismissHiddenSourceItemId = null
            },
            onOpenFullScreenVideo = { media, player ->
                fullScreenVideoItem = media
                fullScreenPlayer = player
            },
            onMuteVideosChange = { isMuted ->
                muteVideos = isMuted
                prefs.muteVideos = isMuted
            },
            onJumpToDay = { selectedDay ->
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                setOrganizeRange(MediaFilterCriteria().withTime(day = selectedDay))
                loadNextBatch(playEntryAnimation = true)
            },
            onTopCardStartupSample = { item, sampleMs ->
                onTopCardStartupSample(item.id, sampleMs)
            },
            onCenterCardBoundsChanged = { bounds ->
                topCardUiState.updateBounds(bounds)
            },
            onCenterCardImageBoundsChanged = { bounds ->
                topCardUiState.updateImageBounds(bounds)
            },
            onCenterAiOverlayStateChanged = { text, visible ->
                topCardUiState.updateAiOverlay(
                    text = text,
                    visible = visible,
                    lensyEnabled = effectiveLensyEnabled
                )
            },
            onUndoAnimationFinished = { token ->
                if (homeSnappingCarouselUndoInFlightToken == token) {
                    homeSnappingCarouselUndoReleaseJob?.cancel()
                    homeSnappingCarouselUndoReleaseJob = null
                    homeSnappingCarouselUndoInFlightToken = null
                }
            },
            onEntryAnimationFinished = {
                if (isInitialLoad) {
                    isInitialLoad = false
                }
            },
            onPendingUndoPageConsumed = {
                homeSnappingCarouselPendingUndoPage = null
            },
            settlementPageContent = { settlementCelebrationToken ->
                EmptyState(
                    onRestart = { continueSnappingCarouselBatchByKeepingRemaining() },
                    pendingTrashCount = pendingTrashCount,
                    onViewTrash = { openDeleteQueue() },
                    filterFolder = filterFolder,
                    filterType = filterType,
                    filterMonth = filterMonth,
                    filterDay = filterDay,
                    remainingCount = carouselSettlementRemainingCount,
                    isFullAlbumDone = isFullAlbumDone,
                    onResetFilter = {
                        setOrganizeRange(MediaFilterCriteria())
                        isFullAlbumDone = false
                        loadNextSnappingCarouselBatch()
                    },
                    onReleaseKept = {
                        releaseKeptForCurrentFilter {
                            loadNextSnappingCarouselBatch()
                        }
                    },
                    onViewLikes = { showLikes = true },
                    onChangeFilter = {
                        showFilter = true
                    },
                    celebrationTriggerToken = settlementCelebrationToken,
                    embeddedInCard = true,
                    burstImmediate = true,
                    quickOrganizeMode = quickOrganizeMode,
                    isRangeActive = buildFilterCriteria().isActive,
                    onBack = { returnToModernStartupWelcomeFromHome() }
                )
            }
        )

        if (effectiveLensyEnabled && !topCardUiState.pageAiOverlayText.isNullOrBlank()) {
            val overlayCardBiasY = if (quickOrganizeMode) -0.29f else -0.12f
            val overlayHeightFraction = if (quickOrganizeMode) 0.56f else 0.6f
            val overlayOffsetY = if (quickOrganizeMode) (-28).dp else (-30).dp

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = BiasAlignment(0f, overlayCardBiasY)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(overlayHeightFraction)
                ) {
                    SmartAiTypingOverlay(
                        fullText = topCardUiState.pageAiOverlayText,
                        visible = topCardUiState.pageAiOverlayVisible,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-6).dp, y = overlayOffsetY)
                    )
                }
            }
        }
    } else {
        HomeCardStackArea(
            topCardUiState = topCardUiState,
            quickOrganizeMode = quickOrganizeMode,
            fullCardMode = fullCardMode,
            backtrackModeEnabled = false,
            canSwipeBacktrackPrevious = false,
            displayMedia = displayMedia,
            hiddenSourceItemId = sharedDismissHiddenSourceItemId,
            catalogVerified = hasVerifiedCatalog,
            isInitialLoad = isInitialLoad,
            cardEntryOffsets = cardEntryOffsets,
            cardEntryRotations = cardEntryRotations,
            cardResetToken = cardResetToken,
            isUndoAnimating = isUndoAnimating,
            undoAnimationTargets = homeCardStackUndoAnimationTargets,
            homeInteractionPerfController = homeInteractionPerfController,
            stagedHeavyWorkCards = stagedHeavyWorkCards,
            predictedSwipeIntent = predictedSwipeIntent,
            rapidSwipeStreak = rapidSwipeStreak,
            muteVideos = muteVideos,
            lensyEnabled = effectiveLensyEnabled,
            aiImageResultsEnabled = aiImageResultsEnabled,
            colorCardEnabled = colorCardFeatureEnabled,
            pauseMainVideos = pauseMainVideos,
            topCardPlaybackArmed = topCardPlaybackArmed,
            isFullScreenVideoOpen = fullScreenVideoItem != null,
            swipeLeftAction = swipeLeftAction,
            swipeRightAction = swipeRightAction,
            swipeUpAction = swipeUpAction,
            onSwipeLeft = { item ->
                handleSwipe(resolveUndoAction(item, swipeLeftAction, SwipeDirection.Left))
            },
            onSwipeRight = { item ->
                handleSwipe(resolveUndoAction(item, swipeRightAction, SwipeDirection.Right))
            },
            onSwipeUp = { item ->
                handleSwipe(resolveUndoAction(item, swipeUpAction, SwipeDirection.Up))
            },
            onOpenDetail = { item, sourceBounds ->
                viewingItems = listOf(item)
                viewingInitialIndex = 0
                viewingSourceBounds = sourceBounds
                viewingSourceCardBoundsAtOpen = topCardUiState.topCardBoundsInRoot
                viewingSourceItemId = item.id
                sharedDismissHiddenSourceItemId = null
            },
            onOpenFullScreenVideo = { media, player ->
                fullScreenVideoItem = media
                fullScreenPlayer = player
            },
            onMuteVideosChange = { isMuted ->
                muteVideos = isMuted
                prefs.muteVideos = isMuted
            },
            onJumpToDay = { selectedDay ->
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                setOrganizeRange(MediaFilterCriteria().withTime(day = selectedDay))
                loadNextBatch(playEntryAnimation = true)
            },
            onTopCardStartupSample = { item, sampleMs ->
                onTopCardStartupSample(item.id, sampleMs)
            },
            playSwipeSound = playSwipeSound,
            playDeleteSound = { playDeleteSound() },
            playLikeSound = { playLikeSound() },
            folderDropTargets = folderDropTargets,
            onFolderDropTargetsChange = { folderDropTargets = it },
            folderDropGestureController = folderDropGestureController,
            folderDropTargetRects = folderDropTargetRects,
            onFolderDropTargetRectsChange = { folderDropTargetRects = it },
            activeDropTargetId = activeDropTargetId,
            onActiveDropTargetIdChange = { activeDropTargetId = it },
            folderListState = folderListState,
            mediaForMoveTargets = mediaForMoveTargets,
            folderUsageState = folderUsageState,
            folderLastUsed = folderLastUsedState.value,
            hiddenFoldersForMoveTargets = hiddenFoldersForMoveTargets,
            prioritizedFolders = prefs.prioritizedFolders,
            userCreatedFolders = prefs.userCreatedFolders.toSet(),
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            onRequestNewFolder = { showNewFolderDialog = true },
            onQuickMoveToFolder = { item, folder -> quickMoveToFolder(item, folder) }
        )
    }
}
    }

    val focusedHomeItem = resolveHomeFocusedItem()

    val quickOrganizeTopRailOffset = if (quickOrganizeMode) (-8).dp else 0.dp
    val quickOrganizeFolderLabelSpacing = if (quickOrganizeMode) 2.dp else 4.dp

    // Top Controls
    CompositionLocalProvider(LocalLiquidGlassBackdrop provides homeGlassBackdrop) {
        MainTopActionRail(
            modifier = Modifier
                .offset(y = quickOrganizeTopRailOffset)
                .graphicsLayer {
                    alpha = homeTopRevealProgress
                    translationY = (-18).dp.toPx() * (1f - homeTopRevealProgress)
                }
                .blur(18.dp * (1f - homeTopRevealProgress)),
            startContent = {
                if (modernHomeNavigationEnabled) {
                    MainHomeBackButton(
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            returnToModernStartupWelcomeFromHome()
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                } else {
                    MainStatsEntryButton(
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showStatsOverlay = true
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            },
            centerContent = {
                // Batch Counter
                if (displayMedia.isNotEmpty()) {
                    // Logic:
                    // denominator = batchSize (user preference)
                    // remaining = displayMedia.size
                    // done = batchSize - remaining
                    // If total available < batchSize (e.g. last batch), denominator should probably be the initial load size?
                    // But loadNextBatch always takes batchSize.
                    // However, if we are near the end, displayMedia might be smaller than batchSize initially.
                    // Let's rely on currentBatchTotal which tracks the initial size of the current batch.

                    // If the batch was smaller than the preference (end of list), use that smaller number as denominator.
                    val denominator = if (backtrackModeEnabled) {
                        homeSnappingCarouselBatchOrderIds.size
                            .takeIf { it > 0 }
                            ?: displayMedia.size.coerceAtLeast(1)
                    } else {
                        currentBatchTotal
                            .takeIf { it > 0 }
                            ?: displayMedia.size.coerceAtMost(batchSize).coerceAtLeast(1)
                    }
                    val done = if (backtrackModeEnabled) {
                        val focusedItemId = focusedHomeItem?.id
                        val stableIndex = focusedItemId?.let { itemId ->
                            homeSnappingCarouselBatchOrderIds.indexOf(itemId).takeIf { it >= 0 }
                        }
                        ((stableIndex ?: resolveHomeCarouselPage()).coerceIn(0, denominator - 1) + 1)
                            .coerceIn(1, denominator)
                    } else {
                        currentBatchConsumed.coerceIn(0, denominator)
                    }
                    val progress = if (denominator > 0) {
                        done.toFloat() / denominator.toFloat()
                    } else {
                        0f
                    }
                    val deckRange = buildFilterCriteria()
                    val hasActiveFilter = deckRange.isActive
                    val currentFolder = focusedHomeItem?.bucketName.orEmpty()
                    // A range that names an album already shows it; any other range (type, time,
                    // a collection) spans albums, so the photo's own album is still worth showing.
                    val showCurrentFolder = currentFolder.isNotBlank() && deckRange.folder == MediaFilterCriteria().folder
                    val filterSummary = if (hasActiveFilter) organizeRangeTitle(deckRange, currentLanguage) else null
                    val clearFilter: () -> Unit = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        setOrganizeRange(MediaFilterCriteria())
                        loadNextBatch(playEntryAnimation = true)
                    }

                    if (backtrackModeEnabled) {
                        HomeSnappingCarouselTopProgress(
                            modifier = Modifier.align(Alignment.Center),
                            progress = progress,
                            isDark = isDark,
                            filterSummary = filterSummary,
                            currentFolderLabel = if (showCurrentFolder) "${strings.currentFolder}: $currentFolder" else null,
                            currentFolderName = if (showCurrentFolder) currentFolder else null,
                            currentFolderSpacing = quickOrganizeFolderLabelSpacing,
                            onClearFilter = clearFilter
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val sortingStrings = organizeSortingStrings(currentLanguage)
                            val deckRemaining = (denominator - done).coerceAtLeast(0)
                            val counterColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black.copy(alpha = 0.86f)
                            Text(
                                text = sortingStrings.format(
                                    if (hasActiveFilter) sortingStrings.scopeRemainingFmt else sortingStrings.batchRemainingFmt,
                                    deckRemaining
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = counterColor,
                                maxLines = 1,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HomeBatchProgressBar(
                                done = done,
                                total = denominator,
                                contentDescription = sortingStrings.format(
                                    sortingStrings.batchPositionFmt,
                                    (done + 1).coerceAtMost(denominator),
                                    denominator
                                )
                            )

                            val folderLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
                            if (hasActiveFilter && filterSummary != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HomeRangeTag(
                                        label = filterSummary,
                                        clearLabel = sortingStrings.clearRange,
                                        onOpen = {
                                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showFilter = true
                                        },
                                        onClear = clearFilter,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (showCurrentFolder) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        HomeCurrentFolderInline(
                                            name = currentFolder,
                                            color = folderLabelColor,
                                            contentDescription = strings.currentFolder
                                        )
                                    }
                                }
                            } else if (showCurrentFolder) {
                                Spacer(modifier = Modifier.height(quickOrganizeFolderLabelSpacing))
                                Text(
                                    text = "${strings.currentFolder}: $currentFolder",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            },
            endContent = {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    AnimatedVisibility(visible = pendingTrashCount > 0) {
                        MainDeleteQueueButton(
                            pendingCount = pendingTrashCount,
                            onClick = { openDeleteQueue() }
                        )
                    }
                }
            }
        )
    }

    val quickFolderTargets = remember(
        quickOrganizeMode,
        mediaForMoveTargets,
        folderUsageState,
        prefs.folderLastUsed,
        hiddenFoldersForMoveTargets,
        prefs.prioritizedFolders,
        focusedHomeItem?.bucketName
    ) {
        if (!quickOrganizeMode) {
            emptyList()
        } else {
            buildFolderTargetsForOrganize(
                allMedia = mediaForMoveTargets,
                currentFolder = focusedHomeItem?.bucketName,
                hiddenFolders = hiddenFoldersForMoveTargets,
                prioritizedFolders = prefs.prioritizedFolders,
                folderUsage = folderUsageState,
                folderLastUsed = prefs.folderLastUsed,
                userCreatedFolders = prefs.userCreatedFolders
            )
        }
    }

    val shareTarget = focusedHomeItem
    var stableQuickOrganizeTarget by remember { mutableStateOf<MediaItem?>(null) }
    LaunchedEffect(
        quickOrganizeMode,
        isFolderDropMode,
        shareTarget?.id
    ) {
        when {
            !quickOrganizeMode || isFolderDropMode -> {
                stableQuickOrganizeTarget = null
            }
            shareTarget != null -> {
                stableQuickOrganizeTarget = shareTarget
            }
        }
    }
    val quickOrganizeBottomControlPadding = rememberQuickOrganizeBottomControlPadding()
    val bottomControlPadding = if (quickOrganizeMode) quickOrganizeBottomControlPadding else 60.dp
    val organizeFloatingButtonShape = RoundedCornerShape(30.dp)
    val organizeFloatingContainerColor = when {
        homeGlassBackdrop != null -> Color.Transparent
        isDark -> MaterialTheme.colorScheme.surface
        else -> Color.White.copy(alpha = 0.9f)
    }
    val organizeFloatingButtonElevation = if (homeGlassBackdrop != null) {
        ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    } else {
        ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    }
    val organizeFloatingBorder = if (homeGlassBackdrop != null) {
        null
    } else {
        androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            } else {
                Color.Black.copy(alpha = 0.1f)
            }
        )
    }
    val organizeFloatingGlassModifier = if (homeGlassBackdrop != null) {
        Modifier.liquidGlassControl(
            backdrop = homeGlassBackdrop,
            shape = organizeFloatingButtonShape,
            surfaceColor = liquidGlassSurfaceColor(isLightTheme = !isDark)
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .navigationBarsPadding()
            .padding(bottom = bottomControlPadding, start = 24.dp)
            .graphicsLayer {
                alpha = homeSideButtonRevealProgress
                translationY = 30.dp.toPx() * (1f - homeSideButtonRevealProgress)
            }
            .blur(16.dp * (1f - homeSideButtonRevealProgress))
    ) {
        AnimatedVisibility(
            visible = shareTarget != null && !isFolderDropMode,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Button(
                onClick = { 
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    shareTarget?.let { shareMedia(context, it) } 
                },
                modifier = Modifier
                    .bounceClick()
                    .then(organizeFloatingGlassModifier),
                colors = ButtonDefaults.buttonColors(containerColor = organizeFloatingContainerColor),
                shape = organizeFloatingButtonShape,
                elevation = organizeFloatingButtonElevation,
                border = organizeFloatingBorder,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Filter Button (Bottom Right - Symmetric)
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(bottom = bottomControlPadding, end = 24.dp)
            .graphicsLayer {
                alpha = homeSideButtonRevealProgress
                translationY = 30.dp.toPx() * (1f - homeSideButtonRevealProgress)
            }
            .blur(16.dp * (1f - homeSideButtonRevealProgress))
    ) {
        AnimatedVisibility(
            visible = !isFolderDropMode, // Always visible if permissions granted, unless dragging down
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Button(
                onClick = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    pendingFilterFolder = filterFolder
                    pendingFilterType = filterType
                    pendingFilterMonth = filterMonth
                    pendingFilterDay = filterDay
                    showFilter = true
                },
                modifier = Modifier
                    .bounceClick()
                    .then(organizeFloatingGlassModifier),
                colors = ButtonDefaults.buttonColors(containerColor = organizeFloatingContainerColor),
                shape = organizeFloatingButtonShape,
                elevation = organizeFloatingButtonElevation,
                border = organizeFloatingBorder,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Filter",
                    tint = when {
                        organizeRangeCriteria.isActive -> MaterialTheme.colorScheme.primary
                        isDark -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        else -> Color.Black.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Bottom Undo Button (Centered)
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = bottomControlPadding)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = homeUndoRevealProgress
                translationY = 34.dp.toPx() * (1f - homeUndoRevealProgress)
            }
            .blur(16.dp * (1f - homeUndoRevealProgress)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = undoStack.isNotEmpty() && !isFolderDropMode,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Button(
                onClick = { handleUndo() },
                modifier = Modifier
                    .bounceClick()
                    .then(organizeFloatingGlassModifier),
                enabled = !backtrackModeEnabled || homeSnappingCarouselUndoInFlightToken == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = organizeFloatingContainerColor
                ),
                shape = organizeFloatingButtonShape,
                elevation = organizeFloatingButtonElevation,
                border = organizeFloatingBorder,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Replay,
                    contentDescription = strings.undo,
                    tint = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    strings.undo,
                    color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    CompositionLocalProvider(LocalLiquidGlassBackdrop provides homeGlassBackdrop) {
        QuickOrganizeBar(
            visible = quickOrganizeMode &&
                !isFolderDropMode &&
                stableQuickOrganizeTarget != null,
            entranceProgress = homeQuickDrawerRevealProgress,
            folders = quickFolderTargets,
            onCreateFolderClick = {
                showNewFolderDialog = true
            },
            onFolderClick = { folder ->
                stableQuickOrganizeTarget?.let { sourceItem ->
                    quickMoveToFolder(sourceItem, folder)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }

    }
        }
    }

    // Stats Sub-page
    PageAnimatedVisibility(
        visible = showStatsOverlay,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        val isFolderDropModeState = isFolderDropMode // Capture state
        val showStatsOverlayState = showStatsOverlay
        PredictiveBackWrapper(
            isVisible = showStatsOverlayState &&
                !showDeepOrganize &&
                !showSimilarPhotos &&
                !showLowQuality &&
                !showDeleteModal &&
                !showRecycleBin &&
                !showLab &&
                !showLabGlobe &&
                !showLabAlbumSettings &&
                !showLabCopyrightVerify &&
                !showLabLiveSyncTool &&
                !showCampfirePlan &&
                !showHelpCenter &&
                !showPromptManager &&
                !showFillLight &&
                !showElectronicAlbumPlayback,
            onBack = { 
                showStatsOverlay = false
                hasPlayedStatsAnimation = true 
            }
        ) {
            // Only play the stats count-up animation the first time the overlay is opened.
            val shouldAnimate = !hasPlayedStatsAnimation
            
            // Check if any sub-page is open (Deep Organize, Monthly, Likes, Settings)
            // Note: Some are overlays on top of StatsOverlay, so we check their visibility states
            val isSubPageOpen = showDeepOrganize || showMonthly || showLikes || showKeepGallery || showWatermarkFrame || showColorCards || showColorCardCamera || showStampCamera || showPromptManager || showFillLight || showSettings || showLanguage || showFilter || showVideoCompress || showImageCompress || showShootingStats || showSwipePreferences || showLab || showLabGlobe || showLabAlbumSettings || showLabCopyrightVerify || showLabLiveSyncTool || showCampfirePlan || showHelpCenter || showElectronicAlbumPlayback
            val consumeStatsIntroAnimation = {
                hasPlayedStatsAnimation = true
                suppressStatsHapticsOnce = true
            }

            // Optimization: Calculate totalSize in background to avoid frame drops
            val totalSize by produceState(initialValue = 0L, key1 = allMedia) {
                value = withContext(Dispatchers.Default) {
                    allMedia.sumOf { it.size }
                }
            }
            val displayTotalSize = remember(totalSize, mediaTotalSizeBytes) {
                maxOf(totalSize, mediaTotalSizeBytes)
            }

            StatsOverlay(
                stats = stats, 
                totalPhotos = maxOf(mediaTotalCount, allMedia.size), 
                likedCount = prefs.likedIds.size,
                deletionMode = deletionMode,
                onDismiss = { 
                    showStatsOverlay = false
                    hasPlayedStatsAnimation = true 
                },
                onProcessedClick = {
                    consumeStatsIntroAnimation()
                    openKeepGallery()
                },
                onDeepOrganizeClick = {
                    consumeStatsIntroAnimation()
                    showDeepOrganize = true
                },
                onShootingStatsClick = {
                    consumeStatsIntroAnimation()
                    openShootingStatsPage()
                },
                onMonthlyClick = {
                    consumeStatsIntroAnimation()
                    showMonthly = true
                },
                onLikesClick = {
                    consumeStatsIntroAnimation()
                    showLikes = true
                },
                onWatermarkFrameClick = {
                    consumeStatsIntroAnimation()
                    openWatermarkFramePage()
                },
                onColorCardClick = {
                    consumeStatsIntroAnimation()
                    if (colorCardFeatureEnabled) {
                        pendingModernColorCardFilterPaletteId = null
                        showColorCards = true
                    }
                },
                showColorCardEntry = colorCardFeatureEnabled,
                onSettingsClick = {
                    consumeStatsIntroAnimation()
                    settingsDestination = SettingsDestination.Home
                    showSettings = true
                },
                onRecycleBinClick = {
                    consumeStatsIntroAnimation()
                    showRecycleBin = true
                },
                onPrivacyClick = {
                    consumeStatsIntroAnimation()
                    settingsDestination = SettingsDestination.Privacy
                    showSettings = true
                },
                onLabClick = {
                    consumeStatsIntroAnimation()
                    showLabCopyrightVerify = false
                    showLabAlbumSettings = false
                    showLab = true
                },
                onCampfireClick = {
                    consumeStatsIntroAnimation()
                    campfireOpenVersion += 1
                    showCampfirePlan = true
                },
                onHelpCenterClick = {
                    if (isHelpCenterVisibleForLanguage(currentLanguage)) {
                        consumeStatsIntroAnimation()
                        showHelpCenter = true
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            getHelpCenterUnavailableMessage(currentLanguage),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                totalSize = displayTotalSize,
                shouldAnimate = shouldAnimate,
                isSubPageOpen = isSubPageOpen,
                isFolderDropMode = isFolderDropModeState, // Use captured state
                enableHaptics = !hasPlayedStatsAnimation && !suppressStatsHapticsOnce
            )
        }
    }

    PageAnimatedVisibility(
        visible = showCampfirePlan,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit,
        modifier = Modifier.zIndex(24f)
    ) {
        PredictiveBackWrapper(
            isVisible = showCampfirePlan,
            enableCustomPredictive = false,
            onBack = { showCampfirePlan = false }
        ) {
            CampfirePlanView(
                openVersion = campfireOpenVersion,
                onBack = { showCampfirePlan = false }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showShootingStats,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showShootingStats,
            onBack = {
                showShootingStats = false
                shootingStatsMediaSnapshot = null
                shootingStatsLikedSnapshot = null
            }
        ) {
            val currentPendingTrashIds = remember(pendingTrashCount) { prefs.pendingTrashIds }
            val activeMedia = remember(insightsMedia, currentPendingTrashIds) {
                insightsMedia.filter { !currentPendingTrashIds.contains(it.id) }
            }
            val stableMedia = shootingStatsMediaSnapshot ?: activeMedia
            val stableLikedIds = shootingStatsLikedSnapshot ?: prefs.likedIds

            ShootingStatsPage(
                allMedia = stableMedia,
                likedIds = stableLikedIds,
                onBack = {
                    showShootingStats = false
                    shootingStatsMediaSnapshot = null
                    shootingStatsLikedSnapshot = null
                }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showDeepOrganize,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showDeepOrganize &&
                !showSimilarPhotos &&
                !showLowQuality &&
                !showDeleteModal &&
                !showRecycleBin,
            onBack = { showDeepOrganize = false }
        ) {
            // Filter out pending trash items to ensure counts are accurate
            DeepOrganizeView(
                allMedia = deepOrganizeCatalogMedia,
                onBack = { showDeepOrganize = false },
                onSimilarClick = { showSimilarPhotos = true },
                onLowQualityClick = { showLowQuality = true },
                onVideoCompressClick = { showVideoCompress = true },
                onImageCompressClick = { showImageCompress = true },
                isScanPaused = isDeepOrganizeScanPaused,
                onToggleScanPause = ::toggleDeepOrganizeScanPause,
                onRescanClick = ::restartDeepOrganizeScan,
                totalMediaCount = maxOf(mediaTotalCount, activeCatalogMedia.size)
            )
        }
    }


    
    PageAnimatedVisibility(
        visible = showVideoCompress,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        PredictiveBackWrapper(
            isVisible = showVideoCompress,
            onBack = { showVideoCompress = false }
        ) {
            VideoCompressionView(
                videos = compressionVideos,
                onBack = { showVideoCompress = false },
                deletionMode = deletionMode,
                onMediaDeleted = { deletedIds ->
                    if (deletionMode == "recycle_bin") {
                        val pending = prefs.pendingTrashIds
                        pending.addAll(deletedIds)
                        prefs.pendingTrashIds = pending
                        pendingTrashCount = pending.size
                    } else {
                        val appTrash = prefs.appTrashInfo
                        deletedIds.forEach { appTrash.remove(it) }
                        prefs.appTrashInfo = appTrash
                    }
                    
                    // Update stats
                    val deletedIdSet = deletedIds.toSet()
                    val newStats = stats.copy(
                        trashed = stats.trashed + deletedIds.size,
                        savedSize = stats.savedSize + sumMediaSizeByIds(insightsMedia, deletedIdSet)
                    )
                    stats = newStats
                    prefs.stats = newStats
                    
                    // Update allMedia
                    allMedia = removeMediaByIds(allMedia, deletedIdSet)
                    doSyncCatalogAfterMediaRemoval(deletedIdSet)
                    removeKeepIds(deletedIdSet)
                }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showImageCompress,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showImageCompress,
            onBack = { showImageCompress = false }
        ) {
            ImageCompressionView(
                photos = compressionPhotos,
                onBack = { showImageCompress = false },
                deletionMode = deletionMode,
                onMediaDeleted = { deletedIds ->
                    if (deletionMode == "recycle_bin") {
                        val pending = prefs.pendingTrashIds
                        pending.addAll(deletedIds)
                        prefs.pendingTrashIds = pending
                        pendingTrashCount = pending.size
                    } else {
                        val appTrash = prefs.appTrashInfo
                        deletedIds.forEach { appTrash.remove(it) }
                        prefs.appTrashInfo = appTrash
                    }
                    
                    // Update stats
                    val deletedIdSet = deletedIds.toSet()
                    val newStats = stats.copy(
                        trashed = stats.trashed + deletedIds.size,
                        savedSize = stats.savedSize + sumMediaSizeByIds(insightsMedia, deletedIdSet)
                    )
                    stats = newStats
                    prefs.stats = newStats
                    
                    // Update allMedia
                    allMedia = removeMediaByIds(allMedia, deletedIdSet)
                    doSyncCatalogAfterMediaRemoval(deletedIdSet)
                    removeKeepIds(deletedIdSet)
                }
            )
        }
    }

    // Settings View
    PageAnimatedVisibility(
        visible = showSettings,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showSettings,
            onBack = {
                if (settingsDestination == SettingsDestination.Home) {
                    showSettings = false
                } else {
                    settingsDestination = SettingsDestination.Home
                }
            }
        ) {
            SettingsView(
                batchSize = batchSize,
                queueOrder = queueOrder,
                muteVideos = muteVideos,
                soundEffectsEnabled = soundEffectsEnabled,
                hapticsEnabled = hapticsEnabled,
                lensyEnabled = effectiveLensyEnabled,
                aiImageResultsEnabled = aiImageResultsEnabled,
                autoCheckUpdatesEnabled = autoCheckUpdatesEnabled,
                appThemeMode = appThemeMode,
                avatarType = avatarType,
                modernHomeNavigationEnabled = modernHomeNavigationEnabled,
                liquidGlassEnabled = liquidGlassEnabled,
                startupDefaultTab = prefs.startupDefaultTab,
                backtrackModeEnabled = backtrackModeEnabled,
                quickOrganizeMode = quickOrganizeMode,
                onBatchSizeChange = { newSize ->
                    batchSize = newSize
                    prefs.batchSize = newSize
                    pendingBatchSizeChange = true
                },
                onQueueOrderChange = { newOrder ->
                    val previousOrder = queueOrder
                    queueOrder = newOrder
                    if (newOrder == "random" && previousOrder != newOrder) {
                        queueRandomOrderSeed = SystemClock.elapsedRealtimeNanos()
                    }
                    prefs.queueOrder = newOrder
                    pendingQueueOrderChange = true
                },
                onMuteVideosChange = { isMuted ->
                    muteVideos = isMuted
                    prefs.muteVideos = isMuted
                },
                onSoundEffectsChange = { isEnabled ->
                    soundEffectsEnabled = isEnabled
                    prefs.soundEffectsEnabled = isEnabled
                },
                onHapticsEnabledChange = { isEnabled ->
                    hapticsEnabled = isEnabled
                    prefs.hapticsEnabled = isEnabled
                },
                onLensyEnabledChange = { isEnabled ->
                    lensyEnabled = isEnabled
                    aiHomepageDisplayPrefs.lensyEnabled = isEnabled
                },
                onAiImageResultsEnabledChange = { isEnabled ->
                    aiImageResultsEnabled = isEnabled
                    aiHomepageDisplayPrefs.imageResultsEnabled = isEnabled
                },
                onAutoCheckUpdatesEnabledChange = { isEnabled ->
                    autoCheckUpdatesEnabled = isEnabled
                    prefs.autoCheckUpdatesEnabled = isEnabled
                },
                onModernHomeNavigationEnabledChange = { isEnabled ->
                    modernHomeNavigationEnabled = isEnabled
                    prefs.modernHomeNavigationEnabled = isEnabled
                },
                onLiquidGlassEnabledChange = { isEnabled ->
                    liquidGlassEnabled = isEnabled
                    prefs.liquidGlassEnabled = isEnabled
                },
                onStartupDefaultTabChange = { tab ->
                    modernStartupWelcomeTab = tab
                    prefs.startupDefaultTab = tab
                },
                onBacktrackModeEnabledChange = { enabled ->
                    if (backtrackModeEnabled != enabled) {
                        backtrackModeEnabled = enabled
                        homeSnappingCarouselLabPrefs.enabled = enabled
                        if (!enabled) {
                            homeSnappingCarouselPendingUndoPage = null
                            homeSnappingCarouselUndoReleaseJob?.cancel()
                            homeSnappingCarouselUndoReleaseJob = null
                            homeSnappingCarouselUndoInFlightToken = null
                        }
                        if (enabled && !quickOrganizeMode) {
                            quickOrganizeMode = true
                            prefs.quickOrganizeMode = true
                            pendingQuickOrganizeRefresh = true
                        }
                    }
                },
                onQuickOrganizeModeChange = { enabled ->
                    if (quickOrganizeMode != enabled) {
                        quickOrganizeMode = enabled
                        prefs.quickOrganizeMode = enabled
                        pendingQuickOrganizeRefresh = true
                    }
                },
                fullCardMode = fullCardMode,
                onFullCardModeChange = { enabled ->
                    fullCardMode = enabled
                    prefs.fullCardMode = enabled
                },
                onLensyInfoClick = {
                    if (lensyAvailable) {
                        showLensyInfo = true
                    }
                },
                onThemeModeChange = { mode ->
                    if (appThemeMode != mode) {
                        appThemeMode = mode
                        prefs.themeMode = mode
                        applyAppThemeMode(mode)
                        restartForThemeModeChange(context as? Activity)
                    }
                },
                onAvatarChange = { type ->
                    avatarType = type
                    prefs.avatarType = type
                },
                onLanguageClick = { showLanguage = true },
                onDeletionModeClick = { showDeletionMode = true },
                onSwipePreferencesClick = {
                    showSwipePreferences = true
                },
                onLabClick = {
                    showLabGlobe = false
                    showLabCopyrightVerify = false
                    showLabAlbumSettings = false
                    showLabLiveSyncTool = false
                    showLab = true
                },
                onHelpCenterClick = if (isHelpCenterVisibleForLanguage(currentLanguage)) {
                    { showHelpCenter = true }
                } else {
                    null
                },
                onOpenSourceClick = {
                    showOpenSourceLicenses = true
                },
                currentDestination = settingsDestination,
                onDestinationChange = { settingsDestination = it },
                onReleaseKept = { showReorganize = true },
                onBack = {
                    settingsDestination = SettingsDestination.Home
                    showSettings = false
                }
            )
        }
    }

    // Settings > Organize Again
    PageAnimatedVisibility(
        visible = showReorganize,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showReorganize,
            onBack = { showReorganize = false }
        ) {
            val reorganizeCandidates = remember(insightsMedia, stats.processed, pendingTrashCount, hiddenFolders, showReorganize) {
                if (!showReorganize) {
                    emptyList()
                } else {
                    selectReorganizeCandidates(
                        allMedia = insightsMedia,
                        processedIds = prefs.processedIds,
                        pendingTrashIds = prefs.pendingTrashIds,
                        likedIds = prefs.likedIds,
                        movedIds = prefs.movedIds,
                        hiddenFolders = hiddenFolders
                    )
                }
            }
            ReorganizePage(
                candidates = reorganizeCandidates,
                hiddenFolders = hiddenFolders,
                onBack = { showReorganize = false },
                onConfirm = { ids, range ->
                    val processed = prefs.processedIds
                    val released = ids.filter { processed.contains(it) }.toSet()
                    if (released.isNotEmpty()) {
                        processed.removeAll(released)
                        prefs.processedIds = processed
                        removeKeepIds(released)
                        val newStats = stats.copy(
                            processed = (stats.processed - released.size).coerceAtLeast(0),
                            skipped = (stats.skipped - released.size).coerceAtLeast(0)
                        )
                        stats = newStats
                        prefs.stats = newStats
                    }
                    setOrganizeRange(range)
                    isFullAlbumDone = false
                    loadNextBatch(playEntryAnimation = true)
                    Toast.makeText(
                        context,
                        reorganizeDoneToast(released.size, currentLanguage),
                        Toast.LENGTH_SHORT
                    ).show()
                    showReorganize = false
                }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showHelpCenter,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showHelpCenter,
            enableCustomPredictive = false,
            onBack = { showHelpCenter = false }
        ) {
            HelpCenterPage(
                onBack = { showHelpCenter = false }
            )
        }
    }

    LabFlowHost(
        showLab = showLab,
        showLabGlobe = showLabGlobe,
        showLabAlbumSettings = showLabAlbumSettings,
        showLabCopyrightVerify = showLabCopyrightVerify,
        showLabLiveSyncTool = showLabLiveSyncTool,
        showElectronicAlbumPlayback = showElectronicAlbumPlayback,
        setShowLab = { showLab = it },
        setShowLabGlobe = { showLabGlobe = it },
        setShowLabAlbumSettings = { showLabAlbumSettings = it },
        setShowLabCopyrightVerify = { showLabCopyrightVerify = it },
        setShowLabLiveSyncTool = { showLabLiveSyncTool = it },
        setShowElectronicAlbumPlayback = { showElectronicAlbumPlayback = it },
        activeCatalogMedia = activeCatalogMedia,
        currentLanguage = currentLanguage,
        advancedTextureEnabled = labAdvancedTextureEnabled,
        onAdvancedTextureEnabledChange = { enabled ->
            if (labAdvancedTextureEnabled != enabled) {
                labAdvancedTextureEnabled = enabled
            }
        },
        likedIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
            addAll(prefs.likedIds)
        },
        config = LabSlideshowConfig(
            source = labSlideshowSource,
            playbackMode = labPlaybackMode,
            folderName = labSlideshowFolder,
            intervalSec = labSlideshowIntervalSec,
            fullscreenLandscape = labFullscreenLandscape,
            displayMode = labFullscreenDisplayMode,
            wholeViewBlurBackgroundEnabled = labWholeViewBlurBackgroundEnabled,
            durationEnabled = labSlideshowDurationEnabled,
            durationMin = labSlideshowDurationMin,
            shuffleEnabled = labSlideshowShuffleEnabled
        ),
        onConfigChange = { cfg ->
            labSlideshowSource = cfg.source
            labPlaybackMode = cfg.playbackMode
            labSlideshowFolder = cfg.folderName
            labSlideshowIntervalSec = cfg.intervalSec
            labFullscreenLandscape = cfg.fullscreenLandscape
            labFullscreenDisplayMode = cfg.displayMode
            labWholeViewBlurBackgroundEnabled = cfg.wholeViewBlurBackgroundEnabled
            labSlideshowDurationEnabled = cfg.durationEnabled
            labSlideshowDurationMin = cfg.durationMin
            labSlideshowShuffleEnabled = cfg.shuffleEnabled
            prefs.labSlideshowSource = if (cfg.source == LabSlideshowSource.Folder) "folder" else "favorites"
            prefs.labPlaybackMode = if (cfg.playbackMode == LabPlaybackMode.FullscreenSlideshow) "fullscreen" else "card"
            prefs.labSlideshowFolder = cfg.folderName
            prefs.labSlideshowIntervalSec = cfg.intervalSec
            prefs.labFullscreenLandscape = cfg.fullscreenLandscape
            prefs.labFullscreenDisplayMode = cfg.displayMode.toPreferenceValue()
            prefs.labWholeViewBlurBackgroundEnabled = cfg.wholeViewBlurBackgroundEnabled
            prefs.labSlideshowDurationEnabled = cfg.durationEnabled
            prefs.labSlideshowDurationMin = cfg.durationMin
            prefs.labSlideshowShuffleEnabled = cfg.shuffleEnabled
        },
        playbackQueue = playbackQueue,
        playbackMode = playbackMode,
        playbackForceLandscapeInFullscreen = playbackForceLandscapeInFullscreen,
        playbackDisplayMode = playbackDisplayMode,
        playbackWholeViewBlurBackgroundEnabled = playbackWholeViewBlurBackgroundEnabled,
        playbackIntervalMs = playbackIntervalMs,
        playbackDurationMs = playbackDurationMs,
        albumSettingsStandalone = electronicAlbumStandaloneEntry,
        keepAlbumSettingsMountedDuringPlayback = false,
        suppressStandaloneExitAnimation = electronicAlbumOpenedFromColorCards,
        onAlbumFlowClosed = ::closeElectronicAlbumFlow,
        onPlaybackPrepared = { queue, mode, forceLandscapeInFullscreen, displayMode, wholeViewBlurBackgroundEnabled, intervalMs, durationMs ->
            playbackQueue = queue
            playbackMode = mode
            playbackForceLandscapeInFullscreen = forceLandscapeInFullscreen
            playbackDisplayMode = displayMode
            playbackWholeViewBlurBackgroundEnabled = wholeViewBlurBackgroundEnabled
            playbackIntervalMs = intervalMs
            playbackDurationMs = durationMs
        }
    )

    PageAnimatedVisibility(
        visible = showSwipePreferences,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showSwipePreferences,
            onBack = { showSwipePreferences = false }
        ) {
            SwipePreferencesView(
                leftAction = swipeLeftAction,
                rightAction = swipeRightAction,
                upAction = swipeUpAction,
                horizontalSpeedMultiplier = swipeHorizontalSpeedMultiplier,
                upSpeedMultiplier = swipeUpSpeedMultiplier,
                onLeftActionChange = {
                    swipeLeftAction = it
                    prefs.swipeLeftAction = swipeActionKey(it)
                },
                onRightActionChange = {
                    swipeRightAction = it
                    prefs.swipeRightAction = swipeActionKey(it)
                },
                onUpActionChange = {
                    swipeUpAction = it
                    prefs.swipeUpAction = swipeActionKey(it)
                },
                onHorizontalSpeedMultiplierChange = {
                    val normalized = normalizeSwipeSpeedMultiplier(it)
                    swipeHorizontalSpeedMultiplier = normalized
                    prefs.swipeHorizontalSpeedMultiplier = normalized
                },
                onUpSpeedMultiplierChange = {
                    val normalized = normalizeSwipeSpeedMultiplier(it)
                    swipeUpSpeedMultiplier = normalized
                    prefs.swipeUpSpeedMultiplier = normalized
                },
                onRestoreSwipeSpeedDefaults = {
                    swipeHorizontalSpeedMultiplier = DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER
                    swipeUpSpeedMultiplier = DEFAULT_UP_SWIPE_SPEED_MULTIPLIER
                    prefs.swipeHorizontalSpeedMultiplier =
                        DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER
                    prefs.swipeUpSpeedMultiplier = DEFAULT_UP_SWIPE_SPEED_MULTIPLIER
                },
                onBack = { showSwipePreferences = false }
            )
        }
    }

    // Open Source Licenses View
    PageAnimatedVisibility(
        visible = showOpenSourceLicenses,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showOpenSourceLicenses,
            onBack = { showOpenSourceLicenses = false }
        ) {
            OpenSourceLicensesView(onBack = {
                showOpenSourceLicenses = false
            })
        }
    }

    val rangePanelSource = remember(insightsMedia, pendingTrashCount, showFilter) {
        if (!showFilter) {
            emptyList()
        } else {
            selectActiveCatalogMedia(
                allMedia = insightsMedia,
                pendingTrashIds = HashSet(prefs.pendingTrashIds),
                hiddenFolders = emptySet()
            )
        }
    }
    val rangePanelProcessedIds = remember(showFilter, stats.processed) {
        if (showFilter) HashSet(prefs.processedIds) else emptySet()
    }
    var rangePanelPrioritizedFolders by remember { mutableStateOf<Set<String>>(prefs.prioritizedFolders.toSet()) }

    // Organize range panel
    PageAnimatedVisibility(
        visible = showFilter,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        val showFilterState = showFilter
        PredictiveBackWrapper(
            isVisible = showFilterState,
            onBack = { showFilter = false }
        ) {
            if (fullCatalogLoading && !fullCatalogLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                OrganizeRangePanel(
                    initialCriteria = buildFilterCriteria(),
                    source = rangePanelSource,
                    processedIds = rangePanelProcessedIds,
                    hiddenFolders = hiddenFolders,
                    prioritizedFolders = rangePanelPrioritizedFolders,
                    onBack = { showFilter = false },
                    onOrganize = { range ->
                        setOrganizeRange(range)
                        isFullAlbumDone = false
                        showFilter = false
                        loadNextBatch(playEntryAnimation = true)
                        if (showStartupWelcomeCard) {
                            startOrganizeFromHomeAfterRangeSignal += 1
                        }
                    },
                    onHideFolder = { folder ->
                        val newSet = hiddenFolders.toMutableSet()
                        newSet.add(folder)
                        hiddenFolders = newSet
                        prefs.hiddenFolders = newSet
                        if (filterFolder == folder) {
                            setOrganizeRange(buildFilterCriteria().copy(folder = "All"))
                        }
                        loadNextBatch()
                    },
                    onUnhideFolder = { folder ->
                        val newSet = hiddenFolders.toMutableSet()
                        newSet.remove(folder)
                        hiddenFolders = newSet
                        prefs.hiddenFolders = newSet
                        loadNextBatch()
                    },
                    onTogglePinFolder = { folder ->
                        val current = prefs.prioritizedFolders
                        if (!current.remove(folder)) current.add(folder)
                        prefs.prioritizedFolders = current
                        rangePanelPrioritizedFolders = current.toSet()
                    }
                )
            }
        }
    }
    
    // Daily Calendar View (New)
    PageAnimatedVisibility(
        visible = showDailyCalendar,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showDailyCalendar,
            onBack = { showDailyCalendar = false }
        ) {
            if (fullCatalogLoading && !fullCatalogLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                DailyCalendarView(
                    currentMonth = pendingFilterMonth,
                    allMedia = insightsMedia,
                    onDaySelected = { day ->
                        pendingFilterDay = day
                        // Return to Filter View with Day selected
                        // Do NOT close Filter View
                        showDailyCalendar = false
                    },
                    onBack = { showDailyCalendar = false }
                )
            }
        }
    }
    
    // Monthly Selection View (Reused from old Monthly feature)
    PageAnimatedVisibility(
        visible = showMonthly,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showMonthly,
            onBack = { showMonthly = false }
        ) {
            // If monthlyReviewList is empty, we are in Selection Mode (List of Months)
            // If it's not empty, we are in Review Mode (Old logic). 
            // But now we want Selection Mode to just SELECT the month and return to Filter/Main.
            
            // We need to differentiate: Are we here for "Monthly Organize" (Old) or "Filter Selection" (New)?
            // User said: "Monthly Organize is no longer a separate feature, but a filter way".
            // So when we click a month, we should SET FILTER and GO BACK.
            
            if (fullCatalogLoading && !fullCatalogLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                MonthlyListView(
                    allMedia = insightsMedia,
                    processedIds = prefs.processedIds,
                    currentMonth = pendingFilterMonth,
                    showProgressOverlay = monthlyProgressEnabled,
                    onToggleProgressOverlay = { enabled ->
                        monthlyProgressEnabled = enabled
                        prefs.monthlyProgressEnabled = enabled
                    },
                    onMonthClick = { items ->
                        // New Logic: Extract Month Key from the first item and set as Filter
                        if (items.isNotEmpty()) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = items[0].dateAdded * 1000L
                            val year = cal.get(Calendar.YEAR)
                            val month = cal.get(Calendar.MONTH) + 1
                            val key = "$year-${month.toString().padStart(2, '0')}"

                            pendingFilterMonth = key

                            showMonthly = false
                        }
                    },
                    onAllClick = {
                        pendingFilterMonth = "All"
                        pendingFilterDay = "All"
                        showMonthly = false
                    },
                    onBack = { showMonthly = false }
                )
            }
        }
    }

    // Language Selection View
    PageAnimatedVisibility(
        visible = showLanguage,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showLanguage,
            onBack = { showLanguage = false }
        ) {
            LanguageView(
                currentLanguage = currentLanguage,
                onLanguageSelected = { lang ->
                    currentLanguage = lang
                    prefs.language = lang
                    showLanguage = false // Close language view, revealing Settings below
                },
                onBack = { showLanguage = false } // Just close language view
            )
        }
    }


    // Monthly View
fun handleMonthlyUndo(
    undoStack: List<UndoAction>, 
    updateUndoStack: (List<UndoAction>) -> Unit,
    updateQueue: (MediaItem) -> Unit, // Add back to front of queue
    updateFolderUsageState: (Map<String, Int>) -> Unit
) {
    if (undoStack.isEmpty()) return
    val action = undoStack.last()
    updateUndoStack(undoStack.dropLast(1))
    val item = action.item

    // Restore to Queue (at front)
    updateQueue(item)
    
    // Update Persistence
    val processed = prefs.processedIds
    processed.remove(item.id)
    prefs.processedIds = processed
    removeKeepIds(listOf(item.id))
    

    when (action) {
        is UndoAction.Trash -> {
            val pending = prefs.pendingTrashIds
            pending.remove(item.id)
            if (item.livePhotoVideoId != null) {
                pending.remove(item.livePhotoVideoId)
            }
            prefs.pendingTrashIds = pending
            pendingTrashCount = pending.size
            
            val newStats = stats.copy(
                processed = stats.processed - 1,
                trashed = stats.trashed - 1,
                savedSize = stats.savedSize - item.size
            )
            stats = newStats
            prefs.stats = newStats
        }
        is UndoAction.Like -> {
            val liked = prefs.likedIds
            liked.remove(item.id)
            prefs.likedIds = liked
            
            val newStats = stats.copy(
                processed = stats.processed - 1,
                skipped = stats.skipped - 1
            )
            stats = newStats
            prefs.stats = newStats
        }
        is UndoAction.Keep -> {
            val newStats = stats.copy(
                processed = stats.processed - 1,
                skipped = stats.skipped - 1
            )
            stats = newStats
            prefs.stats = newStats
        }
        is UndoAction.Move -> {
            val newStats = stats.copy(
                processed = stats.processed - 1,
                skipped = stats.skipped - 1
            )
            stats = newStats
            prefs.stats = newStats
            
            // If it was a new folder creation, remove the folder from usage stats
            if (action.isNewFolder) {
                val targetBucketId = File(action.targetFolder).name
                val currentUsage = prefs.folderUsage
                currentUsage.remove(targetBucketId)
                prefs.folderUsage = currentUsage
                updateFolderUsageState(currentUsage) // Trigger UI update
            } else {
                // Decrement count for existing folder
                val targetBucketId = File(action.targetFolder).name
                val currentUsage = prefs.folderUsage
                val count = currentUsage[targetBucketId] ?: 0
                if (count > 0) {
                    currentUsage[targetBucketId] = count - 1
                    prefs.folderUsage = currentUsage
                    updateFolderUsageState(currentUsage) // Trigger UI update
                }
            }
        }
    }
}

// Old Monthly View logic removed as it was duplicate
// We now reuse showMonthly for just the Selection view above
// And MonthlyReviewView is no longer used directly from main flow, 
// instead we filter main flow by month.

// However, we need to keep handleMonthlyUndo if it's used elsewhere?
// It seems it was only used by MonthlyReviewView.
// Since we removed MonthlyReviewView usage, we can remove this function too if not needed.
// Wait, the AnimatedVisibility block for showMonthly was duplicated above in my SearchReplace.
// I need to REMOVE the old block I just read (lines 2227-2300 approx).

// ... Actually, the toolcall result shows I have TWO AnimatedVisibility(visible = showMonthly) blocks now.
// One I added in previous step (correct one), and one existing one (old one).
// I must remove the OLD one.

    // Similar Photos View
    PageAnimatedVisibility(
        visible = showSimilarPhotos,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        val isScanningSimilar by SimilarPhotoManager.isScanning.collectAsState()
        val similarScanProgress by SimilarPhotoManager.scanProgress.collectAsState()
        val activeSimilarGroups by SimilarPhotoManager.foundGroups.collectAsState()
        PredictiveBackWrapper(
            isVisible = showSimilarPhotos && !showDeleteModal && !showRecycleBin,
            onBack = { showSimilarPhotos = false }
        ) {
            SimilarPhotosView(
                similarGroups = activeSimilarGroups,
                muteVideos = muteVideos,
                isScanning = isScanningSimilar,
                progress = similarScanProgress,
                pendingTrashCount = pendingTrashCount,
                canUndo = similarUndoStack.isNotEmpty(),
                onClose = { showSimilarPhotos = false },
                onDecision = { idsToDelete, idsProcessed ->
                    // Calculate deleted size for stats and undo
                    val deletedSize = sumMediaSizeByIds(insightsMedia, idsToDelete)
                    val deletedItems = if (idsToDelete.isNotEmpty()) {
                        allMedia.filter { idsToDelete.contains(it.id) }
                    } else {
                        emptyList()
                    }
                    
                    // Push to Undo Stack
                    similarUndoStack = similarUndoStack + Triple(idsToDelete, idsProcessed, deletedSize)
                    similarUndoMediaStack = similarUndoMediaStack + listOf(deletedItems)

                    // Immediate save to prevent progress loss
                    if (idsToDelete.isNotEmpty()) {
                        // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣椤斿姊洪柅娑樺祮婵炰匠鍐ｆ灁妞ゆ挾濮风壕钘壝归敐鍡楃祷濞存粓绠栧娲礈閹绘帊绨介梺鍝ュТ濡瑧绮嬪鍜佺叆闁割偆鍠撻崢鎼佹倵楠炲灝鍔氶柛鐕佸亝娣囧﹪宕稿Δ浣哄帗濡炪倖鐗楃粙鎺旂矆鐎ｎ喗鐓涚€光偓閳ь剟宕伴弽顓炵畺婵犲﹤鐗嗙粻锝夋煙閻戞ɑ灏柡瀣ㄥ€濆缁樻媴閸涘﹥鍎撻柣鐐村嚬閸嬪﹤鐣峰┑鍡欐殕闁告洦鍋嗛崣鈧┑鐘灱濞夋稖鐧岄梺缁樻煥閸氬宕甸崘顔界厓閺夌偞澹嗛ˇ锔炬偖閳轰讲鏀介柣妯虹仛閺嗏晠鏌涚€ｎ剙浠滄い顓炴搐閳诲酣骞橀弶鎴濆闂備胶绮崹鐓幬涢崟顖涘仾濡わ絽鍟悡鐔镐繆椤栨氨浠㈤柣銊︽そ閺岋繝宕遍銏☆€嶆繛锝呮搐閿曨亝淇婇崼鏇炵＜婵﹩鍋勯ˉ姘攽閿涘嫬浜奸柛濠冪墵楠炴劙骞栨担鑲澭呯磼鐎ｎ厽纭堕柡鍡畵閺屾盯顢曢敐鍡欘槬闁诡垳鍠栭幃宄邦煥閸愵€呪偓瑙勬礈閺佸寮婚崱妤婂悑闁糕€崇箲鐎氳棄鈹戦悙鑸靛涧缂佹彃娼￠幃娲棘濞嗗墽鍔烽梺褰掑亰閸庮亪鎮烽柇锔惧弳闂侀€炲苯澧伴柛鎺撳笒閳诲氦绠涢弴鐔割啎闂備礁鎼ú銊╁窗鎼搭煈鏁嬮梺顒€绉甸悡娑橆熆鐠轰警鍎忛柣蹇婃櫊閺岋綁濡堕崨顓т純濠殿喖锕ュ钘壩涢崘顭嬪綊濡烽妷褍鈪甸悗娈垮枟瑜板啴鍩ユ径濠庢僵閺夊牃鏅槐鏌ユ⒒娴ｇ鏆遍柟纰卞亰楠炲﹨绠涘☉娆忎簵闂佺粯鏌ㄩ崥瀣偂閻斿吋鐓涢柛灞剧☉椤曟粓鏌涢悢鍝勪户缂佽鲸甯￠幊婊堟偨闂堟稑缁╅梻浣告惈閻鎹㈠┑鍡欐殾闁圭儤顨呯粈鍐煏婵炲灝鍔滄繛鍫熺矒濮婅櫣鎮伴垾鍏呭闂備礁鍚嬫禍浠嬪磿椤栨稓顩叉繝濠傜墛閻撴盯鏌涢幇鈺佸闁逞屽墰閺佸鐛箛娑欐櫢闁绘ê纾崢閬嶆煙閸忚偐鏆橀柛銊︽そ閸╂盯骞囬鍓э紲缂傚倷鐒﹂…鍥煀閺囩姭鍋撻崹顐ｇ凡閻庢凹鍓熼崺銉﹀緞婵犲孩鍍甸柡澶婄墑閸斿秹顢欓幘缁樷拻?
                        val pending = prefs.pendingTrashIds
                        val updatedPending = HashSet(pending)
                        updatedPending.addAll(idsToDelete)
                        prefs.pendingTrashIds = updatedPending
                        pendingTrashCount = updatedPending.size
                        
                        // Update stats
                        val keptCount = idsProcessed.size - idsToDelete.size
                        
                        val newStats = stats.copy(
                            processed = stats.processed + idsProcessed.size,
                            trashed = stats.trashed + idsToDelete.size,
                            skipped = stats.skipped + keptCount,
                            savedSize = stats.savedSize + deletedSize
                        )
                        stats = newStats
                        prefs.stats = newStats
                    } else if (idsProcessed.isNotEmpty()) {
                        // Keep All Case
                        val newStats = stats.copy(
                            processed = stats.processed + idsProcessed.size,
                            skipped = stats.skipped + idsProcessed.size
                        )
                        stats = newStats
                        prefs.stats = newStats
                    }
                    
                    if (idsProcessed.isNotEmpty()) {
                        // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣椤斿姊洪柅娑樺祮婵炰匠鍐ｆ灁妞ゆ挾濮风壕钘壝归敐鍡楃祷濞存粓绠栧娲礈閹绘帊绨介梺鍝ュТ濡瑧绮嬪鍜佺叆闁割偆鍠撻崢鎼佹倵楠炲灝鍔氶柛鐕佸亝娣囧﹪宕稿Δ浣哄帗濡炪倖鐗楃粙鎺旂矆鐎ｎ喗鐓涚€光偓閳ь剟宕伴弽顓炵畺婵犲﹤鐗嗙粻锝夋煙閻戞ɑ灏柡瀣ㄥ€濆缁樻媴閸涘﹥鍎撻柣鐐村嚬閸嬪﹤鐣峰┑鍡欐殕闁告洦鍋嗛崣鈧┑鐘灱濞夋稖鐧岄梺缁樻煥閸氬宕甸崘顔界厓閺夌偞澹嗛ˇ锔炬偖閳轰讲鏀介柣妯虹仛閺嗏晠鏌涚€ｎ剙浠滄い顓炴搐閳诲酣骞橀弶鎴濆闂備胶绮崹鐓幬涢崟顖涘仾濡わ絽鍟悡鐔镐繆椤栨氨浠㈤柣銊︽そ閺岋繝宕遍銏☆€嶆繛锝呮搐閿曨亝淇婇崼鏇炵＜婵﹩鍋勯ˉ姘攽閿涘嫬浜奸柛濠冪墵楠炴劙骞栨担鑲澭呯磼鐎ｎ厽纭堕柡鍡畵閺屾盯顢曢敐鍡欘槬闁诡垳鍠栭幃宄邦煥閸愵€呪偓瑙勬礈閺佸寮婚崱妤婂悑闁糕€崇箲鐎氳棄鈹戦悙鑸靛涧缂佹彃娼￠幃娲棘濞嗗墽鍔烽梺褰掑亰閸庮亪鎮烽柇锔惧弳闂侀€炲苯澧伴柛鎺撳笒閳诲氦绠涢弴鐔割啎闂備礁鎼ú銊╁窗鎼搭煈鏁嬮梺顒€绉甸悡娑橆熆鐠轰警鍎忛柣蹇婃櫊閺岋綁濡堕崨顓т純濠殿喖锕ュ钘壩涢崘顭嬪綊濡烽妷褍鈪甸悗娈垮枟瑜板啴鍩ユ径濠庢僵閺夊牃鏅槐鏌ユ⒒娴ｇ鏆遍柟纰卞亰楠炲﹨绠涘☉娆忎簵闂佺粯鏌ㄩ崥瀣偂閻斿吋鐓涢柛灞剧☉椤曟粓鏌涢悢鍝勪户缂佽鲸甯￠幊婊堟偨闂堟稑缁╅梻浣告惈閻鎹㈠┑鍡欐殾闁圭儤顨呯粈鍐煏婵炲灝鍔滄繛鍫熺矒濮婅櫣鎮伴垾鍏呭闂備礁鍚嬫禍浠嬪磿椤栨稓顩叉繝濠傜墛閻撴盯鏌涢幇鈺佸闁逞屽墰閺佸鐛箛娑欐櫢闁绘ê纾崢閬嶆煙閸忚偐鏆橀柛銊︽そ閸╂盯骞囬鍓э紲缂傚倷鐒﹂…鍥煀閺囩姭鍋撻崹顐ｇ凡閻庢凹鍓熼崺銉﹀緞婵犲孩鍍甸柡澶婄墑閸斿秹顢欓幘缁樷拻?
                        val processed = prefs.processedIds
                        val updatedProcessed = HashSet(processed)
                        updatedProcessed.addAll(idsProcessed)
                        prefs.processedIds = updatedProcessed
                    }
                    val keptIds = idsProcessed - idsToDelete
                    addKeepItems(keptIds)
                    removeKeepIds(idsToDelete)
                    
                    // Update SimilarPhotoManager internal ignored list
                    SimilarPhotoManager.addIgnoredIds(idsProcessed + idsToDelete)
                    
                    // Immediately update SimilarPhotoManager state to remove processed items from view
                    SimilarPhotoManager.removeProcessedItems(idsProcessed + idsToDelete)
                    // Keep pending-trash items in catalog until user confirms real deletion.
                    // Otherwise DeleteReviewModal will show count>0 but an empty list.
                },
                onFinishProcessing = { _, _ ->
                    // Just close, as data is saved incrementally
                    showSimilarPhotos = false
                },
                onUndo = {
                    if (similarUndoStack.isNotEmpty()) {
                        val action = similarUndoStack.last()
                        similarUndoStack = similarUndoStack.dropLast(1)
                        
                        val (deletedIds, processedIds, deletedSize) = action
                        val deletedItems = similarUndoMediaStack.lastOrNull() ?: emptyList()
                        similarUndoMediaStack = similarUndoMediaStack.dropLast(1)
                        removeKeepIds(processedIds)
                        
                        // 1. Revert processed status - 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣椤斿姊洪柅娑樺祮婵炰匠鍐ｆ灁妞ゆ挾濮风壕钘壝归敐鍡楃祷濞存粓绠栧娲礈閹绘帊绨介梺鍝ュТ濡瑧绮嬪鍜佺叆闁割偆鍠撻崢鎼佹倵楠炲灝鍔氶柛鐕佸亝娣囧﹪宕稿Δ浣哄帗濡炪倖鐗楃粙鎺旂矆鐎ｎ喗鐓涚€光偓閳ь剟宕伴弽顓炵畺婵犲﹤鐗嗙粻锝夋煙閻戞ɑ灏柡瀣ㄥ€濆缁樻媴閸涘﹥鍎撻柣鐐村嚬閸嬪﹤鐣峰┑鍡欐殕闁告洦鍋嗛崣鈧┑鐘灱濞夋稖鐧岄梺缁樻煥閸氬宕甸崘顔界厓閺夌偞澹嗛ˇ锔炬偖閳轰讲鏀介柣妯虹仛閺嗏晠鏌涚€ｎ剙浠滄い顓炴搐閳诲酣骞橀弶鎴濆闂備胶绮崹鐓幬涢崟顖涘仾濡わ絽鍟悡鐔镐繆椤栨氨浠㈤柣銊︽そ閺岋繝宕遍銏☆€嶆繛锝呮搐閿曨亝淇婇崼鏇炵＜婵﹩鍋勯ˉ姘攽閿涘嫬浜奸柛濠冪墵楠炴劙骞栨担鑲澭呯磼鐎ｎ厽纭堕柡鍡畵閺屾盯顢曢敐鍡欘槬闁诡垳鍠栭幃宄邦煥閸愵€呪偓瑙勬礈閺佸寮婚崱妤婂悑闁糕€崇箲鐎氳棄鈹戦悙鑸靛涧缂佹彃娼￠幃娲棘濞嗗墽鍔烽梺褰掑亰閸庮亪鎮烽柇锔惧弳闂侀€炲苯澧伴柛鎺撳笒閳诲氦绠涢弴鐔割啎闂備礁鎼ú銊╁窗鎼搭煈鏁嬮梺顒€绉甸悡娑橆熆鐠轰警鍎忛柣蹇婃櫊閺岋綁濡堕崨顓т純濠殿喖锕ュ钘壩涢崘顭嬪綊濡烽妷褍鈪甸悗娈垮枟瑜板啴鍩ユ径濠庢僵閺夊牃鏅槐鏌ユ⒒娴ｇ鏆遍柟纰卞亰楠炲﹨绠涘☉娆忎簵闂佺粯鏌ㄩ崥瀣偂閻斿吋鐓涢柛灞剧☉椤曟粓鏌涢悢鍝勪户缂佽鲸甯￠幊婊堟偨闂堟稑缁╅梻浣告惈閻鎹㈠┑鍡欐殾闁圭儤顨呯粈鍐煏婵炲灝鍔滄繛鍫熺矒濮婅櫣鎮伴垾鍏呭闂備礁鍚嬫禍浠嬪磿椤栨稓顩叉繝濠傜墛閻撴盯鏌涢幇鈺佸闁逞屽墰閺佸鐛箛娑欐櫢闁绘ê纾崢閬嶆煙閸忚偐鏆橀柛銊︽そ閸╂盯骞囬鍓э紲缂傚倷鐒﹂…鍥煀閺囩姭鍋撻崹顐ｇ凡閻庢凹鍓熼崺銉﹀緞婵犲孩鍍甸柡澶婄墑閸斿秹顢欓幘缁樷拻?
                        val processed = prefs.processedIds
                        val updatedProcessed = HashSet(processed)
                        updatedProcessed.removeAll(processedIds)
                        prefs.processedIds = updatedProcessed
                        
                        // 2. Revert deleted status - 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣椤斿姊洪柅娑樺祮婵炰匠鍐ｆ灁妞ゆ挾濮风壕钘壝归敐鍡楃祷濞存粓绠栧娲礈閹绘帊绨介梺鍝ュТ濡瑧绮嬪鍜佺叆闁割偆鍠撻崢鎼佹倵楠炲灝鍔氶柛鐕佸亝娣囧﹪宕稿Δ浣哄帗濡炪倖鐗楃粙鎺旂矆鐎ｎ喗鐓涚€光偓閳ь剟宕伴弽顓炵畺婵犲﹤鐗嗙粻锝夋煙閻戞ɑ灏柡瀣ㄥ€濆缁樻媴閸涘﹥鍎撻柣鐐村嚬閸嬪﹤鐣峰┑鍡欐殕闁告洦鍋嗛崣鈧┑鐘灱濞夋稖鐧岄梺缁樻煥閸氬宕甸崘顔界厓閺夌偞澹嗛ˇ锔炬偖閳轰讲鏀介柣妯虹仛閺嗏晠鏌涚€ｎ剙浠滄い顓炴搐閳诲酣骞橀弶鎴濆闂備胶绮崹鐓幬涢崟顖涘仾濡わ絽鍟悡鐔镐繆椤栨氨浠㈤柣銊︽そ閺岋繝宕遍銏☆€嶆繛锝呮搐閿曨亝淇婇崼鏇炵＜婵﹩鍋勯ˉ姘攽閿涘嫬浜奸柛濠冪墵楠炴劙骞栨担鑲澭呯磼鐎ｎ厽纭堕柡鍡畵閺屾盯顢曢敐鍡欘槬闁诡垳鍠栭幃宄邦煥閸愵€呪偓瑙勬礈閺佸寮婚崱妤婂悑闁糕€崇箲鐎氳棄鈹戦悙鑸靛涧缂佹彃娼￠幃娲棘濞嗗墽鍔烽梺褰掑亰閸庮亪鎮烽柇锔惧弳闂侀€炲苯澧伴柛鎺撳笒閳诲氦绠涢弴鐔割啎闂備礁鎼ú銊╁窗鎼搭煈鏁嬮梺顒€绉甸悡娑橆熆鐠轰警鍎忛柣蹇婃櫊閺岋綁濡堕崨顓т純濠殿喖锕ュ钘壩涢崘顭嬪綊濡烽妷褍鈪甸悗娈垮枟瑜板啴鍩ユ径濠庢僵閺夊牃鏅槐鏌ユ⒒娴ｇ鏆遍柟纰卞亰楠炲﹨绠涘☉娆忎簵闂佺粯鏌ㄩ崥瀣偂閻斿吋鐓涢柛灞剧☉椤曟粓鏌涢悢鍝勪户缂佽鲸甯￠幊婊堟偨闂堟稑缁╅梻浣告惈閻鎹㈠┑鍡欐殾闁圭儤顨呯粈鍐煏婵炲灝鍔滄繛鍫熺矒濮婅櫣鎮伴垾鍏呭闂備礁鍚嬫禍浠嬪磿椤栨稓顩叉繝濠傜墛閻撴盯鏌涢幇鈺佸闁逞屽墰閺佸鐛箛娑欐櫢闁绘ê纾崢閬嶆煙閸忚偐鏆橀柛銊︽そ閸╂盯骞囬鍓э紲缂傚倷鐒﹂…鍥煀閺囩姭鍋撻崹顐ｇ凡閻庢凹鍓熼崺銉﹀緞婵犲孩鍍甸柡澶婄墑閸斿秹顢欓幘缁樷拻?
                        if (deletedIds.isNotEmpty()) {
                            val pending = prefs.pendingTrashIds
                            val updatedPending = HashSet(pending)
                            updatedPending.removeAll(deletedIds)
                            prefs.pendingTrashIds = updatedPending
                            pendingTrashCount = updatedPending.size
                        }
                        
                        // 3. Revert Stats
                        val keptCount = processedIds.size - deletedIds.size
                        val newStats = stats.copy(
                            processed = (stats.processed - processedIds.size).coerceAtLeast(0),
                            trashed = (stats.trashed - deletedIds.size).coerceAtLeast(0),
                            skipped = (stats.skipped - keptCount).coerceAtLeast(0),
                            savedSize = (stats.savedSize - deletedSize).coerceAtLeast(0)
                        )
                        stats = newStats
                        prefs.stats = newStats
                        
                        // 4. Restore in SimilarPhotoManager
                        // This will trigger re-calculation of groups, effectively putting the group back
                        val allIds = buildMediaIdSet(allMedia)
                        val hasMissing = deletedItems.any { !allIds.contains(it.id) }
                        if (hasMissing && deletedItems.isNotEmpty()) {
                            allMedia = mergeMediaIntoQueue(
                                currentMedia = allMedia,
                                additions = deletedItems,
                                queueOrder = queueOrder,
                                randomOrderSeed = queueRandomOrderSeed
                            )
                        }
                        SimilarPhotoManager.restoreIgnoredIds(processedIds + deletedIds, allMedia)
                    }
                },
                onViewTrash = { openDeleteQueue() }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showLowQuality,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        val isScanningLowQuality by LowQualityManager.isScanning.collectAsState()
        val lowQualityScanProgress by LowQualityManager.scanProgress.collectAsState()
        val lowQualityItemsFromManager by LowQualityManager.foundItems.collectAsState()
        var lowQualityDisplayInitialized by remember { mutableStateOf(false) }
        val lowQualityIgnoredIds = catalogDisplayIgnoredIds
        val managerVisibleLowQualityItems = remember(
            lowQualityItemsFromManager,
            lowQualityIgnoredIds
        ) {
            lowQualityItemsFromManager.filter { entry ->
                shouldAnalyzeAsStandardPhoto(entry.first) &&
                    !lowQualityIgnoredIds.contains(entry.first.id)
            }
        }
        val lowQualitySeedItems = managerVisibleLowQualityItems
        val visibleLowQualityItems = remember(
            lowQualityDisplayInitialized,
            lowQualityDisplayItems,
            lowQualitySeedItems
        ) {
            if (lowQualityDisplayInitialized) lowQualityDisplayItems else lowQualitySeedItems
        }

        LaunchedEffect(lowQualitySeedItems, lowQualityDisplayInitialized) {
            if (!lowQualityDisplayInitialized && lowQualitySeedItems.isNotEmpty()) {
                lowQualityDisplayItems = lowQualitySeedItems
                lowQualityDisplayInitialized = true
            }
        }

        LaunchedEffect(
            lowQualityDisplayInitialized,
            lowQualitySeedItems,
            lowQualityIgnoredIds
        ) {
            if (!lowQualityDisplayInitialized) {
                return@LaunchedEffect
            }

            val currentVisibleItems = lowQualityDisplayItems.filter { entry ->
                !lowQualityIgnoredIds.contains(entry.first.id)
            }
            val mergedItems = mergeLowQualityDisplayKeepingVisible(
                currentDisplay = currentVisibleItems,
                targetDisplay = lowQualitySeedItems
            )
            if (mergedItems != lowQualityDisplayItems) {
                lowQualityDisplayItems = mergedItems
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                lowQualityDisplayInitialized = false
                lowQualityDisplayItems = emptyList()
            }
        }
        PredictiveBackWrapper(
            isVisible = showLowQuality && !showDeleteModal && !showRecycleBin,
            onBack = { showLowQuality = false }
        ) {
            LowQualityView(
                items = visibleLowQualityItems,
                isScanning = isScanningLowQuality,
                progress = lowQualityScanProgress,
                pendingTrashCount = pendingTrashCount,
                canUndo = lowQualityUndoStack.any { hasLowQualityUndoCandidate(it.item.id) },
                undoAnimationTarget = lowQualityUndoAnimationTarget,
                muteVideos = muteVideos,
                swipeLeftAction = swipeLeftAction,
                swipeRightAction = swipeRightAction,
                swipeUpAction = swipeUpAction,
                onClose = { showLowQuality = false },
                onSwipe = { item, reason, action, direction ->
                    handleLowQualitySwipe(item, reason, action, direction)
                },
                onUndo = { handleLowQualityUndo() },
                onViewTrash = { openDeleteQueue() }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showPhotoWall,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        val photoWallCriteria = organizeRangeCriteria
        // Read again when the library, range or hidden albums change, and after the pending-delete
        // page closes (photos restored there come back to the wall).
        val photoWallItems by produceState<List<MediaItem>?>(
            initialValue = null,
            insightsMedia,
            photoWallCriteria,
            hiddenFolders,
            showDeleteModal
        ) {
            if (showDeleteModal) return@produceState
            val media = insightsMedia
            val pendingSnapshot = HashSet(prefs.pendingTrashIds)
            val processedSnapshot = HashSet(prefs.processedIds)
            val hiddenSnapshot = effectiveHiddenFolders(hiddenFolders.toSet(), photoWallCriteria)
            value = withContext(Dispatchers.Default) {
                selectPhotoWallItems(media, pendingSnapshot, processedSnapshot, hiddenSnapshot, photoWallCriteria)
            }
        }
        val photoWallResultStrings = organizeResultStrings(currentLanguage)
        PredictiveBackWrapper(
            isVisible = showPhotoWall && !showDeleteModal && !showRecycleBin,
            onBack = { closePhotoWall() }
        ) {
            PhotoWallPage(
                items = photoWallItems,
                title = if (photoWallCriteria.isActive) {
                    organizeRangeTitle(photoWallCriteria, currentLanguage)
                } else {
                    photoWallStrings(currentLanguage).title
                },
                emptyMessage = if (photoWallCriteria.isActive) {
                    photoWallResultStrings.rangeDoneTitle
                } else {
                    photoWallResultStrings.allDoneTitle
                },
                pendingCount = pendingTrashCount,
                onBack = { closePhotoWall() },
                onOpenPendingDelete = { openDeleteQueue() },
                onTrash = { handlePhotoWallTrash(it) },
                onRestore = { handlePhotoWallRestore(it) },
                playDeleteSound = { playDeleteSound() },
                playReturnSound = { playSwipeSound() }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showRecycleBin,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showRecycleBin,
            onBack = { showRecycleBin = false }
        ) {
            RecycleBinView(
                items = recycleBinItems,
                onRestore = { selectedIds ->
                    if (selectedIds.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val selectedItems = recycleBinItems.filter { selectedIds.contains(it.media.id) }
                        val batchItems = selectedItems.take(MEDIASTORE_URI_BATCH_LIMIT)
                        val batchUris = batchItems.map { it.media.uri }
                        if (batchUris.isNotEmpty()) {
                            val batchIds = batchItems.map { it.media.id }.toSet()
                            pendingRestoreIds = batchIds
                            pendingRestoreRemainingIds = selectedItems.drop(batchItems.size).map { it.media.id }.toSet()
                            restoreLaunchToken++
                        }
                    }
                },
                onDelete = { selectedIds ->
                    if (selectedIds.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val selectedItems = recycleBinItems.filter { selectedIds.contains(it.media.id) }
                        val batchItems = selectedItems.take(MEDIASTORE_URI_BATCH_LIMIT)
                        val batchItemIds = batchItems.map { it.media.id }.toSet()
                        val batchItemUris = batchItems.map { it.media.uri }
                        if (batchItemUris.isNotEmpty()) {
                            pendingPermanentDeleteIds = batchItemIds
                            pendingPermanentDeleteRemainingIds = selectedItems.drop(batchItems.size).map { it.media.id }.toSet()
                            permanentDeleteLaunchToken++
                        }
                    }
                },
                onBack = { showRecycleBin = false }
            )
        }
    }
    
    PageAnimatedVisibility(
        visible = showKeepGallery,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        PredictiveBackWrapper(
            isVisible = showKeepGallery,
            onBack = { showKeepGallery = false }
        ) {
            KeepGalleryView(
                store = keepGalleryStore,
                onBack = { showKeepGallery = false },
                onShare = { item -> shareMedia(context, item) },
                onLike = { item ->
                    val liked = prefs.likedIds
                    val likedAdded = liked.add(item.id)
                    prefs.likedIds = liked
                    removeKeepIds(listOf(item.id))
                    if (likedAdded) {
                        scheduleHotStatePersist(persistLikedIds = true)
                    }
                },
                onDeleteQueued = { item ->
                    val pending = prefs.pendingTrashIds
                    val pendingAdded = pending.add(item.id)
                    prefs.pendingTrashIds = pending
                    pendingTrashCount = pending.size
                    removeKeepIds(listOf(item.id))
                    if (pendingAdded) {
                        val newStats = stats.copy(
                            trashed = stats.trashed + 1,
                            skipped = (stats.skipped - 1).coerceAtLeast(0),
                            savedSize = stats.savedSize + item.size
                        )
                        stats = newStats
                        scheduleHotStatePersist(
                            persistPendingTrashIds = true,
                            stats = newStats
                        )
                    }
                }
            )
        }
    }

    // Likes View (TikTok Style)
    PageAnimatedVisibility(
        visible = showLikes,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        PredictiveBackWrapper(
            isVisible = showLikes,
            onBack = { showLikes = false }
        ) {
            // Filter for Liked Items: processed AND marked as LIKED (right swipe)
            // Use produceState to offload heavy filtering/shuffling to background thread
            // Add allMedia.size as key to ensure it updates when media loads
            val likedItems by produceState<List<MediaItem>>(initialValue = emptyList(), key1 = showLikes, key2 = insightsMedia.size) {
                if (showLikes) {
                    value = withContext(Dispatchers.Default) {
                        val liked = prefs.likedIds
                        // Convert to Set<Long> explicitly to avoid Gson double/long mismatch issues
                        val likedSet = liked.map { it.toLong() }.toSet()
                        insightsMedia.filter { likedSet.contains(it.id) }.shuffled()
                    }
                }
            }
            
            LikesView(
                likedItems = likedItems,
                onBack = { showLikes = false },
                onShare = { item -> shareMedia(context, item) },
                prefs = prefs, // Pass prefs to handle comments
                allMedia = insightsMedia,
                folderUsage = folderUsageState,
                userCreatedFolders = prefs.userCreatedFolders,
                onLikeRemoved = { item -> addKeepItem(item) }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showWatermarkFrame,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit,
        restartContentOnOpen = false
    ) {
        PredictiveBackWrapper(
            isVisible = showWatermarkFrame,
            onBack = {
                showWatermarkFrame = false
                resetWatermarkEditorForLegacyEntry = false
                pendingModernWatermarkPresetKey = null
                pendingModernWatermarkInitialImageUri = null
                pendingModernWatermarkPickerPresetKey = null
                replacePendingWatermarkBackdropBitmap(null)
            }
        ) {
            WatermarkFrameView(
                onBack = {
                    showWatermarkFrame = false
                    resetWatermarkEditorForLegacyEntry = false
                    pendingModernWatermarkPresetKey = null
                    pendingModernWatermarkInitialImageUri = null
                    pendingModernWatermarkPickerPresetKey = null
                    replacePendingWatermarkBackdropBitmap(null)
                },
                openSessionToken = watermarkFrameOpenSessionToken,
                resetForLegacyEntryOnOpen = resetWatermarkEditorForLegacyEntry,
                initialModernPresetKey = pendingModernWatermarkPresetKey,
                initialModernImageUriString = pendingModernWatermarkInitialImageUri,
                initialBackdropBitmap = pendingWatermarkBackdropBitmap,
                autoLaunchPickerForInitialModernPreset = pendingModernWatermarkPresetKey != null &&
                    pendingModernWatermarkInitialImageUri == null,
                onInitialModernPresetConsumed = {
                    pendingModernWatermarkPresetKey = null
                    pendingModernWatermarkInitialImageUri = null
                }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showColorCards,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showColorCards &&
                !colorCardPageHandlesBackInternally &&
                !showLabAlbumSettings &&
                !showFillLight &&
                !showElectronicAlbumPlayback,
            onBack = {
                colorCardPageHandlesBackInternally = false
                showColorCards = false
                pendingModernColorCardFilterPaletteId = null
            }
        ) {
            ColorCardPage(
                onBack = {
                    colorCardPageHandlesBackInternally = false
                    showColorCards = false
                    pendingModernColorCardFilterPaletteId = null
                },
                onOpenCamera = { showColorCardCamera = true },
                onOpenStampCamera = { showStampCamera = true },
                onOpenElectronicAlbum = {
                    openStandaloneElectronicAlbum(preserveColorCards = true)
                },
                onOpenPromptManager = { showPromptManager = true },
                onOpenFillLight = { showFillLight = true },
                onInternalBackHandlingChanged = { colorCardPageHandlesBackInternally = it },
                initialFilterPaletteId = pendingModernColorCardFilterPaletteId,
                autoLaunchPickerForInitialFilter = pendingModernColorCardFilterPaletteId != null,
                onInitialFilterConsumed = {
                    pendingModernColorCardFilterPaletteId = null
                }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showPromptManager,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showPromptManager,
            onBack = { showPromptManager = false }
        ) {
            PromptManagerPage(
                onBack = { showPromptManager = false }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showFillLight,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        FillLightPage(
            onBack = { showFillLight = false }
        )
    }

    PageAnimatedVisibility(
        visible = showColorCardCamera,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showColorCardCamera,
            onBack = { showColorCardCamera = false }
        ) {
            ColorCardCameraPage(
                onBack = { showColorCardCamera = false }
            )
        }
    }

    PageAnimatedVisibility(
        visible = showStampCamera,
        enter = FluidTransitions.SheetEnter,
        exit = FluidTransitions.SheetExit
    ) {
        PredictiveBackWrapper(
            isVisible = showStampCamera,
            onBack = { showStampCamera = false }
        ) {
            StampCalendarPage(
                onBack = { showStampCamera = false },
                onCampfireRequested = {
                    showStampCamera = false
                    campfireOpenVersion += 1
                    showCampfirePlan = true
                }
            )
        }
    }

    // Insights sub-pages (footprints globe, photo groups) sit above the tabs, below the viewer.
    InsightsOverlayPages(
        state = insightsUiState,
        allMedia = insightsCatalogMedia,
        likedIds = insightsLikedIds,
        onOrganize = { title, ids ->
            insightsUiState.clearRoutes()
            setOrganizeRange(MediaFilterCriteria(collectionTitle = title, collectionIds = ids))
            isFullAlbumDone = false
            loadNextBatch(playEntryAnimation = true)
            modernStartupWelcomeTab = StartupWelcomeTab.Organize.name
            if (showStartupWelcomeCard) {
                startOrganizeFromHomeAfterRangeSignal += 1
            }
        },
        onOpenPhoto = { items, index ->
            viewingItems = items
            viewingInitialIndex = index.coerceIn(0, maxOf(0, items.lastIndex))
            viewingSourceBounds = null
            viewingSourceCardBoundsAtOpen = null
            viewingSourceItemId = null
            sharedDismissHiddenSourceItemId = null
        }
    )

    // Full Screen Viewer
    // Fix: Persist items during exit animation
    var currentViewingItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(viewingItems) {
        viewingItems?.let {
            currentViewingItems = it
        }
    }
    val effectiveViewingItems = viewingItems ?: currentViewingItems
    val adjustedViewingSourceBounds = remember(
        viewingSourceBounds,
        viewingSourceCardBoundsAtOpen,
        topCardUiState.topCardBoundsInRoot,
        topCardUiState.topCardImageBoundsInRoot,
        viewingSourceItemId,
        topCardId
    ) {
        val baseBounds = viewingSourceBounds ?: return@remember null
        val openCardBounds = viewingSourceCardBoundsAtOpen
        val currentCardBounds = topCardUiState.topCardBoundsInRoot
        val currentImageBounds = topCardUiState.topCardImageBoundsInRoot
        val sourceItemMatchesTopCard = when {
            viewingSourceItemId == null -> true
            topCardId == null -> false
            else -> viewingSourceItemId == topCardId
        }
        if (
            sourceItemMatchesTopCard &&
            currentImageBounds != null &&
            currentImageBounds.width > 1f &&
            currentImageBounds.height > 1f
        ) {
            currentImageBounds
        } else if (
            sourceItemMatchesTopCard &&
            openCardBounds != null &&
            currentCardBounds != null &&
            shouldRemapBoundsByCardDelta(openCardBounds, currentCardBounds)
        ) {
            remapBoundsByCardDelta(
                sourceBounds = baseBounds,
                openCardBounds = openCardBounds,
                currentCardBounds = currentCardBounds
            )
        } else {
            baseBounds
        }
    }
    val sharedOpenSourceBounds = remember(viewingSourceBounds, adjustedViewingSourceBounds) {
        viewingSourceBounds ?: adjustedViewingSourceBounds
    }
    val fullScreenPhotoTimeline = remember(insightsMedia) {
        buildFullScreenPhotoTimeline(allMedia = insightsMedia)
    }
    val fullScreenPhotoTimelineIds = remember(fullScreenPhotoTimeline) {
        fullScreenPhotoTimeline.asSequence().map { it.id }.toHashSet()
    }
    val currentFullScreenDayContext = remember(effectiveViewingItems, fullScreenPhotoTimeline, fullScreenPhotoTimelineIds) {
        val currentItem = effectiveViewingItems.firstOrNull() ?: return@remember emptyList()
        if (currentItem.type != "photo" || fullScreenPhotoTimelineIds.contains(currentItem.id)) {
            fullScreenPhotoTimeline
        } else {
            (fullScreenPhotoTimeline + currentItem).sortedBy { it.dateAdded }
        }
    }
    val currentViewingItem = effectiveViewingItems.firstOrNull()
    val shouldUseSharedOpenTransition = remember(currentViewingItem, sharedOpenSourceBounds) {
        val targetItem = currentViewingItem ?: return@remember false
        val sourceBounds = sharedOpenSourceBounds
        val hasSharedBounds = sourceBounds != null &&
            sourceBounds.width > 1f &&
            sourceBounds.height > 1f
        hasSharedBounds && targetItem.type == "photo"
    }

    PageAnimatedVisibility(
        visible = viewingItems != null,
        enter = if (shouldUseSharedOpenTransition) {
            androidx.compose.animation.EnterTransition.None
        } else {
            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300))
        },
        exit = androidx.compose.animation.ExitTransition.None,
        restartContentOnOpen = false
    ) {
        if (effectiveViewingItems.isNotEmpty()) {
            MainFullScreenImageViewer(
                item = effectiveViewingItems.first(),
                dayContextItems = currentFullScreenDayContext,
                sourceBoundsInRoot = sharedOpenSourceBounds,
                dismissSourceBoundsInRoot = adjustedViewingSourceBounds,
                onSharedDismissStart = { sourceItemId ->
                    sharedDismissHiddenSourceItemId = sourceItemId
                },
                isMuted = muteVideos,
                onJumpToDay = { selectedDay ->
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewingItems = null
                    viewingSourceBounds = null
                    viewingSourceCardBoundsAtOpen = null
                    viewingSourceItemId = null
                    sharedDismissHiddenSourceItemId = null
                    scope.launch {
                        delay(220L)
                        setOrganizeRange(MediaFilterCriteria().withTime(day = selectedDay))
                        loadNextBatch(playEntryAnimation = true)
                    }
                },
                onDismiss = {
                    if (sharedDismissHiddenSourceItemId != null) {
                        viewingItems = null
                        scope.launch {
                            withFrameNanos { }
                            sharedDismissHiddenSourceItemId = null
                            if (viewingItems == null) {
                                viewingSourceBounds = null
                                viewingSourceCardBoundsAtOpen = null
                                viewingSourceItemId = null
                            }
                        }
                    } else {
                        viewingItems = null
                        viewingSourceBounds = null
                        viewingSourceCardBoundsAtOpen = null
                        viewingSourceItemId = null
                        sharedDismissHiddenSourceItemId = null
                    }
                }
            )
        }
    }
    
    // Full Screen Video Player
    AnimatedVisibility(
        visible = fullScreenVideoItem != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        fullScreenVideoItem?.let { item ->
            FullScreenVideoPlayer(
                item = item,
                player = fullScreenPlayer,
                isMuted = muteVideos,
                onMuteChange = { isMuted ->
                    muteVideos = isMuted
                    prefs.muteVideos = isMuted
                },
                onDismiss = {
                    fullScreenVideoItem = null
                    fullScreenPlayer = null
                }
            )
        }
    }

    // Delete Review Modal (top layer: must sit above full-screen sub-pages like
    // Similar Photos / Low Quality / Recycle Bin, which are composed after the home Box)
    val deleteQueueItems by produceState(
        initialValue = emptyList<MediaItem>(),
        showDeleteModal,
        pendingTrashCount,
        insightsMedia,
        effectivePendingTrashIds
    ) {
        if (!showDeleteModal) return@produceState
        val pendingTrashSnapshot = effectivePendingTrashIds
        val latestItems = withContext(Dispatchers.Default) {
            if (pendingTrashSnapshot.isEmpty()) {
                emptyList()
            } else {
                insightsMedia.asSequence()
                    .filter { pendingTrashSnapshot.contains(it.id) }
                    .toList()
            }
        }
        value = when {
            latestItems.isEmpty() && pendingTrashSnapshot.isNotEmpty() && value.isNotEmpty() -> value
            latestItems.isEmpty() -> emptyList()
            value.isEmpty() -> latestItems
            else -> mergeDisplayKeepingVisible(
                currentDisplay = value,
                targetDisplay = latestItems,
                limit = latestItems.size
            )
        }
    }

    fun removeIdsFromDeleteQueue(idsToRemove: Set<Long>) {
        val normalizedPending = pendingTrashForCurrentCatalog()
        if (normalizedPending.size != prefs.pendingTrashIds.size) {
            prefs.pendingTrashIds = normalizedPending.toMutableSet()
            pendingTrashCount = normalizedPending.size
        }

        val removableIds = idsToRemove.filter { normalizedPending.contains(it) }.toSet()
        if (removableIds.isEmpty()) {
            pendingTrashCount = normalizedPending.size
            return
        }

        val updatedStats = stats.copy(
            trashed = (stats.trashed - removableIds.size).coerceAtLeast(0),
            skipped = stats.skipped + removableIds.size,
            savedSize = (stats.savedSize - sumMediaSizeByIds(insightsMedia, removableIds)).coerceAtLeast(0)
        )
        stats = updatedStats
        prefs.stats = updatedStats

        val updatedPending = normalizedPending.filterNot { removableIds.contains(it) }.toSet()
        prefs.pendingTrashIds = updatedPending.toMutableSet()
        pendingTrashCount = updatedPending.size
        addKeepItems(removableIds, insightsMedia)
    }

    @Composable
    fun DeleteReviewModalLayer() {
        PageAnimatedVisibility(
            visible = showDeleteModal,
            enter = FluidTransitions.SheetEnter,
            exit = FluidTransitions.SheetExit,
            modifier = Modifier.zIndex(30f)
        ) {
            PredictiveBackWrapper(
                isVisible = showDeleteModal,
                onBack = { showDeleteModal = false }
            ) {
                DeleteReviewModal(
                    pendingTrashIds = effectivePendingTrashIds,
                    trashItems = deleteQueueItems,
                    currentDeletionMode = deletionMode,
                    onSettingsClick = {
                        showDeletionMode = true
                    },
                    onConfirmDelete = { selectedIds ->
                        val normalizedPending = pendingTrashForCurrentCatalog()
                        if (normalizedPending.size != prefs.pendingTrashIds.size) {
                            prefs.pendingTrashIds = normalizedPending.toMutableSet()
                            pendingTrashCount = normalizedPending.size
                        }
                        val normalizedSelected = selectedIds.filter { normalizedPending.contains(it) }.toSet()
                        if (normalizedSelected.isNotEmpty()) {
                            handleEmptyTrash(normalizedSelected)
                        }
                    },
                    onRemoveFromQueue = { ids ->
                        removeIdsFromDeleteQueue(ids)
                    },
                    onRemoveSingleFromQueue = { id ->
                        removeIdsFromDeleteQueue(setOf(id))
                    },
                    onDismiss = { showDeleteModal = false },
                    onNavigateToRecycleBin = {
                        showDeleteModal = false
                        showRecycleBin = true
                    }
                )
            }
        }
    }
    DeleteReviewModalLayer()

    // Deletion Mode Page (Moved to top layer)
    PageAnimatedVisibility(
        visible = showDeletionMode,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.zIndex(31f)
    ) {
        PredictiveBackWrapper(
            isVisible = showDeletionMode,
            onBack = { showDeletionMode = false }
        ) {
            DeletionModeView(
                currentMode = deletionMode,
                onModeSelected = { mode ->
                    deletionMode = mode
                    prefs.deletionMode = mode
                },
                onBack = { showDeletionMode = false }
            )
        }
    }

    // Feedback Overlay
    AnimatedVisibility(
        visible = moveFeedbackMessage != null,
        enter = FluidTransitions.SubtleSlideIn,
        exit = FluidTransitions.SubtleSlideOut,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (quickOrganizeMode) 88.dp else 108.dp)
    ) {
        moveFeedbackMessage?.let { msg ->
            val feedbackBackground = if (isDark) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            } else {
                Color.White.copy(alpha = 0.88f)
            }
            val feedbackBorder = if (isDark) {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
            } else {
                Color.Black.copy(alpha = 0.08f)
            }
            val feedbackTextColor = if (isDark) {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f)
            } else {
                Color.Black.copy(alpha = 0.66f)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(feedbackBackground)
                    .border(
                        width = 1.dp,
                        color = feedbackBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            ) {
                Text(
                    text = msg,
                    color = feedbackTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    LaunchedEffect(moveFeedbackMessage) {
        if (moveFeedbackMessage != null) {
            delay(1500) // 1.5s display time (faster than Toast's 2s+ fade)
            moveFeedbackMessage = null
        }
    }
    }
    if (showLensyInfo && lensyAvailable) {
        AlertDialog(
            onDismissRequest = { showLensyInfo = false },
            title = { Text(text = "Lensy") },
            text = {
                Text(
                    text = lensyInfoCopy.message
                )
            },
            confirmButton = {
                TextButton(onClick = { showLensyInfo = false }) {
                    Text(lensyInfoCopy.closeLabel, color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    }
    }
    }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun shouldRemapBoundsByCardDelta(
    openCardBounds: androidx.compose.ui.geometry.Rect,
    currentCardBounds: androidx.compose.ui.geometry.Rect
): Boolean {
    val openWidth = openCardBounds.width.coerceAtLeast(1f)
    val openHeight = openCardBounds.height.coerceAtLeast(1f)
    val scaleX = currentCardBounds.width / openWidth
    val scaleY = currentCardBounds.height / openHeight
    if (scaleX !in 0.7f..1.35f || scaleY !in 0.7f..1.35f) {
        return false
    }
    val dx = currentCardBounds.center.x - openCardBounds.center.x
    val dy = currentCardBounds.center.y - openCardBounds.center.y
    val centerDistance = kotlin.math.sqrt((dx * dx) + (dy * dy))
    val maxReasonableShift = kotlin.math.max(openWidth, openHeight) * 0.65f + 96f
    return centerDistance <= maxReasonableShift
}

private fun stagedProgress(
    progress: Float,
    start: Float,
    end: Float
): Float {
    if (end <= start) return if (progress >= end) 1f else 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private fun remapBoundsByCardDelta(
    sourceBounds: androidx.compose.ui.geometry.Rect,
    openCardBounds: androidx.compose.ui.geometry.Rect,
    currentCardBounds: androidx.compose.ui.geometry.Rect
): androidx.compose.ui.geometry.Rect {
    val openWidth = openCardBounds.width.coerceAtLeast(1f)
    val openHeight = openCardBounds.height.coerceAtLeast(1f)
    val scaleX = currentCardBounds.width / openWidth
    val scaleY = currentCardBounds.height / openHeight
    val openCenter = openCardBounds.center
    val currentCenter = currentCardBounds.center
    return androidx.compose.ui.geometry.Rect(
        left = currentCenter.x + ((sourceBounds.left - openCenter.x) * scaleX),
        top = currentCenter.y + ((sourceBounds.top - openCenter.y) * scaleY),
        right = currentCenter.x + ((sourceBounds.right - openCenter.x) * scaleX),
        bottom = currentCenter.y + ((sourceBounds.bottom - openCenter.y) * scaleY)
    )
}

/* extracted to StatsOverlay.kt
                val actionItems = buildList<StatsActionItem> {
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.Image,
                            title = "濠电姷鏁告慨鐑藉极閸涘﹥鍙忛柣銏犲閺佸﹪鏌″搴″箹缂佹劖顨嗘穱濠囧Χ閸涱収浠鹃梺鐟板暱閺堫剛鎹㈠┑瀣仺闂傚牊绋愮划鍫曟⒑閸涘﹨澹樻い鎴濐樀楠炲啴鎮滈挊澹┿劑鏌嶉崫鍕偓鍛婄婵傚憡鈷戞慨鐟版搐閻忣喗銇勯鐐靛ⅱ缂侇喖鐗撳畷姗€顢欓悾灞藉笚闂備礁鎲＄换鍌溾偓姘煎墴閸┾偓妞ゆ帒鍟崥鍦磼椤旇姤顥堢€规洖銈稿鎾偄閸濆嫬绠洪梻鍌欒兌缁垶寮婚妸鈺佺婵犲﹤鐗嗙粻鐘绘煕閹伴潧鏋熼柛濠傜仛閹便劌螣閸濆嫯鍩為梺鍛娒顓㈠焵?,
                            onClick = onWatermarkFrameClick
                        )
                    )
                }
*/

private fun deletionQueueMilestone(count: Int): Int {
    if (count < 120) return 0
    return 120 + ((count - 120) / 50) * 50
}
