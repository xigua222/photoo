package com.voicelike.app

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.ln

private val GlobeScanDispatcher = Dispatchers.IO.limitedParallelism(1)
private const val HEAT_LAT_STEP = 0.5
private const val HEAT_LON_STEP = 0.5
private const val COASTLINE_DECIMATE_STEP = 1
private const val GLOBE_CACHE_VERSION = "v7"
private const val GLOBE_USE_STALE_CACHE_ON_SIGNATURE_MISS = true
private const val SCATTER_TARGET_POINTS = 15000
private const val HEAT_RENDER_MAX_POINTS = 9000
private const val SCATTER_RENDER_MAX_POINTS = 6500
private const val GLOBE_ZOOM_MIN = 0.75f
private const val GLOBE_ZOOM_MAX = 2.25f
private const val GLOBE_TRACK_TAP_ZOOM_DURATION_MS = 320
private const val GLOBE_DRAG_YAW_SENSITIVITY = 0.0065f
private const val GLOBE_DRAG_PITCH_SENSITIVITY = 0.0046f
private const val GLOBE_DRAG_INERTIA_X = 0.11f
private const val GLOBE_DRAG_INERTIA_Y = 0.09f
private const val GLOBE_ENTRY_DURATION_NS = 1_850_000_000L
private const val GLOBE_ENTRY_START_SCALE = 2.35f
private const val GLOBE_ENTRY_START_SPIN_SPEED = 5.40f
private const val HEAT_HALO_SCALE_OUTER = 3.2f
private const val HEAT_HALO_SCALE_MID = 2.0f
private const val HEAT_CORE_SCALE = 0.95f
private const val SCATTER_BASE_RADIUS = 0.55f
private const val SCATTER_RADIUS_GAIN = 1.35f

private data class GlobeDensityPoint(
    val lat: Double,
    val lon: Double,
    val weight: Float
)

private data class GlobeScatterPoint(
    val lat: Double,
    val lon: Double,
    val count: Int = 1
)

private data class ProjectedPoint(
    val x: Float,
    val y: Float,
    val depth: Float
)

private data class UnitVec3(
    val x: Float,
    val y: Float,
    val z: Float
)

private data class HeatPoint3D(
    val v: UnitVec3,
    val weight: Float
)

private data class ScatterPoint3D(
    val v: UnitVec3,
    val count: Int
)

private data class ProjectionBasis(
    val cosYaw: Float,
    val sinYaw: Float,
    val cosPitch: Float,
    val sinPitch: Float
)

private data class GlobeLoadState(
    val isLoading: Boolean = true,
    val scanned: Int = 0,
    val total: Int = 0,
    val points: List<GlobeDensityPoint> = emptyList(),
    val scatterPoints: List<GlobeScatterPoint> = emptyList(),
    val geoTaggedCount: Int = 0
)

private data class GlobeCacheEntry(
    val signature: String,
    val points: List<GlobeDensityPoint>,
    val scatterPoints: List<GlobeScatterPoint> = emptyList(),
    val geoTaggedCount: Int = 0,
    val savedAtMs: Long = System.currentTimeMillis()
)

private data class GlobeCoordRecord(
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null
)

private data class GlobeDensityResult(
    val points: List<GlobeDensityPoint>,
    val scatterPoints: List<GlobeScatterPoint>,
    val geoTaggedCount: Int
)

private enum class GlobeRenderMode { Heat, Scatter }
private data class GlobeLocalizedText(
    val title: String,
    val loadingPrefix: String,
    val backgroundBuilding: String,
    val modeHeat: String,
    val modeScatter: String,
    val zoom: String,
    val privacyHint: String,
    val loadingFacts: List<String>
)

private object GlobeCoordinateCache {
    private val coords = HashMap<Long, Pair<Double, Double>?>()
    private var loaded = false
    private var dirty = false
    private const val INDEX_FILE_NAME = "globe_coord_index_v2.json"
    private const val MAX_INDEX_ENTRIES = 250_000
    private val gson = Gson()

    @Synchronized
    fun ensureLoaded(context: android.content.Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val file = File(context.filesDir, INDEX_FILE_NAME)
            if (!file.exists()) return@runCatching
            val json = file.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<GlobeCoordRecord>>() {}.type
            val records: List<GlobeCoordRecord> = gson.fromJson(json, type) ?: emptyList()
            var droppedLegacyNull = false
            records.forEach { rec ->
                if (rec.lat != null && rec.lon != null) {
                    coords[rec.id] = rec.lat to rec.lon
                } else {
                    droppedLegacyNull = true
                }
            }
            if (droppedLegacyNull) dirty = true
        }
    }

    @Synchronized
    fun getIfKnown(id: Long): Pair<Double, Double>? {
        return if (coords.containsKey(id)) coords[id] else null
    }

    @Synchronized
    fun has(id: Long): Boolean = coords.containsKey(id)

    @Synchronized
    fun put(id: Long, value: Pair<Double, Double>?) {
        if (value == null) {
            if (coords.remove(id) != null) {
                dirty = true
            }
            return
        }
        val current = coords[id]
        if (current != value) {
            coords[id] = value
            dirty = true
        }
    }

    @Synchronized
    fun persistIfDirty(context: android.content.Context) {
        if (!dirty) return
        dirty = false
        runCatching {
            val snapshot = if (coords.size <= MAX_INDEX_ENTRIES) {
                coords.entries.toList()
            } else {
                coords.entries.toList().takeLast(MAX_INDEX_ENTRIES)
            }
            val records = snapshot.map { (id, value) ->
                GlobeCoordRecord(
                    id = id,
                    lat = value?.first,
                    lon = value?.second
                )
            }
            val file = File(context.filesDir, INDEX_FILE_NAME)
            file.writeText(gson.toJson(records), Charsets.UTF_8)
        }.onFailure {
            // Mark dirty again so we can retry on next pass.
            dirty = true
        }
    }
}

private object GlobeHeatmapCacheRepository {
    private const val CACHE_FILE_NAME = "globe_heatmap_cache_v4.json"
    private val gson = Gson()

    fun loadWithMeta(context: android.content.Context, signature: String): GlobeDensityResult? {
        return runCatching {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) return null
            if (file.length() <= 4L) {
                runCatching { file.delete() }
                return null
            }
            val json = file.readText(Charsets.UTF_8)
            parseEntryJson(json, requiredSignature = signature)
        }.getOrElse {
            // Corrupted/legacy cache should not crash rendering.
            runCatching { File(context.filesDir, CACHE_FILE_NAME).delete() }
            null
        }
    }

    fun loadLatest(context: android.content.Context): GlobeDensityResult? {
        return runCatching {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) return null
            if (file.length() <= 4L) {
                runCatching { file.delete() }
                return null
            }
            val json = file.readText(Charsets.UTF_8)
            parseEntryJson(json, requiredSignature = null)
        }.getOrElse {
            runCatching { File(context.filesDir, CACHE_FILE_NAME).delete() }
            null
        }
    }

    fun save(
        context: android.content.Context,
        signature: String,
        points: List<GlobeDensityPoint>,
        scatterPoints: List<GlobeScatterPoint>,
        geoTaggedCount: Int
    ) {
        runCatching {
            val entry = GlobeCacheEntry(
                signature = signature,
                points = points,
                scatterPoints = scatterPoints,
                geoTaggedCount = geoTaggedCount
            )
            val json = gson.toJson(entry)
            val file = File(context.filesDir, CACHE_FILE_NAME)
            val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.writeText(json, Charsets.UTF_8)
                runCatching { tmp.delete() }
            }
        }
    }

    private fun parseEntryJson(
        json: String,
        requiredSignature: String?
    ): GlobeDensityResult? {
        val root = JsonParser.parseString(json).asJsonObject
        val sig = root.get("signature")?.takeIf { !it.isJsonNull }?.asString ?: ""
        if (requiredSignature != null && sig != requiredSignature) return null

        val points = root.getAsJsonArray("points")
            ?.mapNotNull { item ->
                val obj = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val lat = obj.get("lat")?.takeIf { !it.isJsonNull }?.asDouble ?: return@mapNotNull null
                val lon = obj.get("lon")?.takeIf { !it.isJsonNull }?.asDouble ?: return@mapNotNull null
                val weight = obj.get("weight")?.takeIf { !it.isJsonNull }?.asFloat ?: return@mapNotNull null
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@mapNotNull null
                GlobeDensityPoint(lat = lat, lon = lon, weight = weight)
            }
            .orEmpty()

        val scatter = root.getAsJsonArray("scatterPoints")
            ?.mapNotNull { item ->
                val obj = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val lat = obj.get("lat")?.takeIf { !it.isJsonNull }?.asDouble ?: return@mapNotNull null
                val lon = obj.get("lon")?.takeIf { !it.isJsonNull }?.asDouble ?: return@mapNotNull null
                val count = obj.get("count")?.takeIf { !it.isJsonNull }?.asInt ?: 1
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@mapNotNull null
                GlobeScatterPoint(lat = lat, lon = lon, count = count.coerceAtLeast(1))
            }
            .orEmpty()

        val geoTaggedCount = root.get("geoTaggedCount")?.takeIf { !it.isJsonNull }?.asInt
            ?: points.size
        if (points.isEmpty() && scatter.isEmpty()) return null
        return GlobeDensityResult(
            points = points,
            scatterPoints = scatter,
            geoTaggedCount = geoTaggedCount.coerceAtLeast(0)
        )
    }
}

private object GlobeRuntimeCache {
    @Volatile
    var signature: String? = null

    @Volatile
    var result: GlobeDensityResult? = null
}

/** Natural Earth coastline resolution; [Fine] is what GlobeViewPage has always drawn. */
internal enum class CoastlineDetail(val assetName: String, val fallbackAssetName: String) {
    Fine("ne_50m_coastline.geojson", "ne_110m_coastline.geojson"),
    Coarse("ne_110m_coastline.geojson", "ne_50m_coastline.geojson")
}

/** Shared coastline cache (GlobeViewPage + InsightGlobe). [load] blocks: call it off the main thread. */
internal object CoastlineRepository {
    private val cachedByDetail = java.util.concurrent.ConcurrentHashMap<CoastlineDetail, List<List<Pair<Double, Double>>>>()

    fun load(context: android.content.Context): List<List<Pair<Double, Double>>> =
        load(context, CoastlineDetail.Fine)

    fun load(context: android.content.Context, detail: CoastlineDetail): List<List<Pair<Double, Double>>> {
        cachedByDetail[detail]?.let { return it }
        synchronized(this) {
            cachedByDetail[detail]?.let { return it }
            val result = runCatching {
                val coastlineFile = runCatching {
                    context.assets.open(detail.assetName).close()
                    detail.assetName
                }.getOrDefault(detail.fallbackAssetName)

                context.assets.open(coastlineFile).use { input ->
                    InputStreamReader(input).use { reader ->
                        val root = JsonParser.parseReader(reader).asJsonObject
                        val features = root.getAsJsonArray("features")
                        val lines = ArrayList<List<Pair<Double, Double>>>()

                        features.forEach { f ->
                            val feature = f.asJsonObject
                            val geometry = feature.getAsJsonObject("geometry")
                            val type = geometry.get("type").asString
                            val coords = geometry.getAsJsonArray("coordinates")

                            when (type) {
                                "LineString" -> {
                                    val line = parseLineString(coords)
                                    if (line.size >= 2) lines.add(line)
                                }
                                "MultiLineString" -> {
                                    coords.forEach { segment ->
                                        val line = parseLineString(segment.asJsonArray)
                                        if (line.size >= 2) lines.add(line)
                                    }
                                }
                            }
                        }
                        lines
                    }
                }
            }.getOrElse { emptyList() }

            cachedByDetail[detail] = result
            return result
        }
    }

    private fun parseLineString(coords: com.google.gson.JsonArray): List<Pair<Double, Double>> {
        val raw = ArrayList<Pair<Double, Double>>(coords.size())
        coords.forEach { point ->
            val arr = point.asJsonArray
            if (arr.size() >= 2) {
                val lon = arr[0].asDouble
                val lat = arr[1].asDouble
                if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                    raw.add(lat to lon)
                }
            }
        }
        if (COASTLINE_DECIMATE_STEP <= 1 || raw.size <= 3) return raw
        val decimated = ArrayList<Pair<Double, Double>>(raw.size / COASTLINE_DECIMATE_STEP + 2)
        raw.forEachIndexed { idx, p ->
            if (idx % COASTLINE_DECIMATE_STEP == 0 || idx == raw.lastIndex) {
                decimated.add(p)
            }
        }
        return decimated
    }
}

@Composable
fun GlobeViewPage(
    allMedia: List<MediaItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    fun performHaptic() {
        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    BackHandler(enabled = true) {
        onBack()
    }

    val strings = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    val localized = remember(appLanguage) {
        when (appLanguage) {
            AppLanguage.SimplifiedChinese -> GlobeLocalizedText(
                title = "地球仪",
                loadingPrefix = "正在生成全球热力图…",
                backgroundBuilding = "后台构建中",
                modeHeat = "热力",
                modeScatter = "散点",
                zoom = "缩放",
                privacyHint = "地球仪支持离线使用，数据默认仅保存在本地设备。",
                loadingFacts = listOf(
                    "我们住在一颗被磁风包裹的带电岩石上。",
                    "地表约 71% 是海洋，储存着大部分流动的热。",
                    "你的影像正在被映射进一颗私有的记忆引力场。"
                )
            )
            AppLanguage.TraditionalChinese -> GlobeLocalizedText(
                title = "地球儀",
                loadingPrefix = "正在生成全球熱力圖…",
                backgroundBuilding = "背景建置中",
                modeHeat = "熱力",
                modeScatter = "散點",
                zoom = "縮放",
                privacyHint = "地球儀支援離線使用，資料預設僅保留在本機。",
                loadingFacts = listOf(
                    "我們住在一顆被磁風包裹的帶電岩石上。",
                    "地表約 71% 是海洋，儲存著大部分流動的熱。",
                    "你的影像正被映射進一顆私有的記憶引力場。"
                )
            )
            AppLanguage.Korean -> GlobeLocalizedText(
                title = "지구본",
                loadingPrefix = "전역 히트맵 생성 중…",
                backgroundBuilding = "백그라운드에서 생성 중",
                modeHeat = "히트",
                modeScatter = "산점",
                zoom = "확대/축소",
                privacyHint = "글로브는 오프라인 사용을 지원하며, 데이터는 기본적으로 기기에만 저장됩니다.",
                loadingFacts = listOf(
                    "우리는 자기 바람에 감싸인 전하를 띤 암석 위에 살고 있습니다.",
                    "지표의 약 71%는 바다이며, 이동하는 열의 대부분을 저장합니다.",
                    "당신의 사진은 개인 기억의 중력장으로 매핑되고 있습니다."
                )
            )
            AppLanguage.Japanese -> GlobeLocalizedText(
                title = "地球儀",
                loadingPrefix = "グローバルヒートマップを生成中…",
                backgroundBuilding = "バックグラウンドで構築中",
                modeHeat = "ヒート",
                modeScatter = "散布",
                zoom = "ズーム",
                privacyHint = "グローブはオフライン利用をサポートし、データは既定で端末内に保存されます。",
                loadingFacts = listOf(
                    "私たちは磁気の風に包まれた帯電した岩の上で暮らしています。",
                    "地表の約71%は海で、動く熱の多くを蓄えています。",
                    "あなたの写真は、個人の記憶の重力場へマッピングされています。"
                )
            )
            AppLanguage.French -> GlobeLocalizedText(
                title = "Globe",
                loadingPrefix = "Génération de la carte de chaleur mondiale…",
                backgroundBuilding = "Construction en arrière-plan",
                modeHeat = "Chaleur",
                modeScatter = "Nuage",
                zoom = "Zoom",
                privacyHint = "Le globe prend en charge un usage hors ligne, et les données restent par défaut sur l'appareil.",
                loadingFacts = listOf(
                    "Nous vivons sur une roche chargée enveloppée par des vents magnétiques.",
                    "Environ 71% de la surface terrestre est océanique et stocke la majeure partie de la chaleur mobile.",
                    "Vos images sont cartographiées dans un champ gravitationnel de mémoire privée."
                )
            )
            AppLanguage.German -> GlobeLocalizedText(
                title = "Globus",
                loadingPrefix = "Globale Heatmap wird erstellt…",
                backgroundBuilding = "Wird im Hintergrund aufgebaut",
                modeHeat = "Heat",
                modeScatter = "Streuung",
                zoom = "Zoom",
                privacyHint = "Der Globus unterstützt die Offline-Nutzung; Daten bleiben standardmäßig auf diesem Gerät.",
                loadingFacts = listOf(
                    "Wir leben auf einem geladenen Gestein, umhüllt von magnetischem Wind.",
                    "Rund 71% der Erdoberfläche sind Ozean und speichern den Großteil bewegter Wärme.",
                    "Deine Bilder werden in ein privates Gravitationsfeld der Erinnerung abgebildet."
                )
            )
            AppLanguage.Italian -> GlobeLocalizedText(
                title = "Globo",
                loadingPrefix = "Generazione della mappa di calore globale…",
                backgroundBuilding = "Elaborazione in background",
                modeHeat = "Calore",
                modeScatter = "Dispersione",
                zoom = "Zoom",
                privacyHint = "Il globo supporta l'uso offline e i dati restano per impostazione predefinita sul dispositivo.",
                loadingFacts = listOf(
                    "Viviamo su una roccia carica avvolta da vento magnetico.",
                    "Circa il 71% della superficie terrestre è oceano e immagazzina gran parte del calore in movimento.",
                    "Le tue immagini vengono mappate in un campo gravitazionale privato della memoria."
                )
            )
            AppLanguage.Portuguese -> GlobeLocalizedText(
                title = "Globo",
                loadingPrefix = "Gerando mapa de calor global…",
                backgroundBuilding = "Construindo em segundo plano",
                modeHeat = "Calor",
                modeScatter = "Dispersão",
                zoom = "Zoom",
                privacyHint = "O globo suporta uso offline, e os dados ficam por padrão no seu dispositivo.",
                loadingFacts = listOf(
                    "Vivemos sobre uma rocha carregada envolta por vento magnético.",
                    "Cerca de 71% da superfície da Terra é oceano e armazena a maior parte do calor em movimento.",
                    "Suas imagens estão sendo mapeadas para um campo gravitacional privado de memória."
                )
            )
            AppLanguage.Spanish -> GlobeLocalizedText(
                title = "Globo",
                loadingPrefix = "Generando mapa de calor global…",
                backgroundBuilding = "Construyendo en segundo plano",
                modeHeat = "Calor",
                modeScatter = "Dispersión",
                zoom = "Zoom",
                privacyHint = "El globo admite uso sin conexión y los datos permanecen por defecto en este dispositivo.",
                loadingFacts = listOf(
                    "Vivimos sobre una roca cargada envuelta por viento magnético.",
                    "Aproximadamente el 71% de la superficie terrestre es océano y almacena gran parte del calor en movimiento.",
                    "Tus imágenes se están mapeando en un campo gravitatorio privado de memoria."
                )
            )
            AppLanguage.Russian -> GlobeLocalizedText(
                title = "Глобус",
                loadingPrefix = "Создание глобальной тепловой карты…",
                backgroundBuilding = "Фоновое построение",
                modeHeat = "Тепло",
                modeScatter = "Точки",
                zoom = "Масштаб",
                privacyHint = "Глобус поддерживает работу без интернета, данные по умолчанию остаются на устройстве.",
                loadingFacts = listOf(
                    "Мы живем на заряженной скале, окруженной магнитным ветром.",
                    "Около 71% поверхности Земли покрыто океаном, где хранится большая часть движущегося тепла.",
                    "Ваши снимки проецируются в приватное гравитационное поле памяти."
                )
            )
            AppLanguage.Thai -> GlobeLocalizedText(
                title = "ลูกโลก",
                loadingPrefix = "กำลังสร้างฮีตแมประดับโลก…",
                backgroundBuilding = "กำลังประมวลผลเบื้องหลัง",
                modeHeat = "ฮีตแมป",
                modeScatter = "จุดกระจาย",
                zoom = "ซูม",
                privacyHint = "ลูกโลกรองรับการใช้งานแบบออฟไลน์ และข้อมูลจะเก็บไว้ในเครื่องเป็นค่าเริ่มต้น",
                loadingFacts = listOf(
                    "เรากำลังอาศัยอยู่บนหินที่มีประจุและถูกโอบล้อมด้วยลมแม่เหล็ก",
                    "พื้นผิวโลกประมาณ 71% เป็นมหาสมุทร ซึ่งเก็บความร้อนที่เคลื่อนที่ส่วนใหญ่ไว้",
                    "ภาพของคุณกำลังถูกแมปเข้าสู่แรงโน้มถ่วงความทรงจำแบบส่วนตัว"
                )
            )
            AppLanguage.Vietnamese -> GlobeLocalizedText(
                title = "Quả cầu",
                loadingPrefix = "Đang tạo bản đồ nhiệt toàn cầu…",
                backgroundBuilding = "Đang dựng nền",
                modeHeat = "Nhiệt",
                modeScatter = "Phân tán",
                zoom = "Thu phóng",
                privacyHint = "Quả cầu hỗ trợ dùng ngoại tuyến, dữ liệu mặc định chỉ lưu trên thiết bị.",
                loadingFacts = listOf(
                    "Chúng ta sống trên một khối đá mang điện được bao bọc bởi gió từ trường.",
                    "Khoảng 71% bề mặt Trái Đất là đại dương, lưu trữ phần lớn nhiệt đang chuyển động.",
                    "Ảnh của bạn đang được ánh xạ vào một trường hấp dẫn ký ức riêng tư."
                )
            )
            AppLanguage.Indonesian -> GlobeLocalizedText(
                title = "Globe",
                loadingPrefix = "Membuat heatmap global…",
                backgroundBuilding = "Membangun di latar belakang",
                modeHeat = "Panas",
                modeScatter = "Sebaran",
                zoom = "Zoom",
                privacyHint = "Globe mendukung penggunaan offline, dan data secara default tetap di perangkat ini.",
                loadingFacts = listOf(
                    "Kita hidup di atas batu bermuatan yang diselimuti angin magnetik.",
                    "Sekitar 71% permukaan Bumi adalah lautan dan menyimpan sebagian besar panas yang bergerak.",
                    "Gambar Anda sedang dipetakan ke medan gravitasi memori pribadi."
                )
            )
            AppLanguage.Arabic -> GlobeLocalizedText(
                title = "الكرة الأرضية",
                loadingPrefix = "جارٍ إنشاء خريطة حرارة عالمية…",
                backgroundBuilding = "جارٍ البناء في الخلفية",
                modeHeat = "حرارة",
                modeScatter = "تناثر",
                zoom = "تكبير",
                privacyHint = "تدعم الكرة الأرضية الاستخدام دون اتصال، وتبقى البيانات افتراضيًا على هذا الجهاز.",
                loadingFacts = listOf(
                    "نحن نعيش على صخرة مشحونة يلفّها ريح مغناطيسية.",
                    "حوالي 71٪ من سطح الأرض محيطات تخزّن معظم الحرارة المتحركة.",
                    "يتم إسقاط صورك داخل مجال جاذبية خاص بالذاكرة."
                )
            )
            AppLanguage.Ukrainian -> GlobeLocalizedText(
                title = "Глобус",
                loadingPrefix = "Створення глобальної теплової карти…",
                backgroundBuilding = "Побудова у фоновому режимі",
                modeHeat = "Тепло",
                modeScatter = "Точки",
                zoom = "Масштаб",
                privacyHint = "Глобус дозволяє користуватися основними функціями без інтернету. Дані за замовчуванням залишаються на цьому пристрої.",
                loadingFacts = listOf(
                    "Ми живемо на зарядженому камені, оповитому магнітним вітром.",
                    "71% Землі вкрито океаном, який зберігає більшу частину рухомого тепла.",
                    "Ваші зображення відображаються в приватній гравітації пам’яті."
                )
            )
            AppLanguage.Polish -> GlobeLocalizedText(
                title = "Globus",
                loadingPrefix = "Tworzenie globalnej mapy cieplnej…",
                backgroundBuilding = "Budowanie w tle",
                modeHeat = "Ciepło",
                modeScatter = "Punkty",
                zoom = "Powiększenie",
                privacyHint = "Globus pozwala korzystać z podstawowych funkcji bez internetu. Dane domyślnie zostają na tym urządzeniu.",
                loadingFacts = listOf(
                    "Żyjemy na naładowanym kamieniu otulonym magnetycznym wiatrem.",
                    "71% Ziemi pokrywa ocean, który magazynuje większość ruchomego ciepła.",
                    "Twoje zdjęcia są mapowane w prywatnej grawitacji pamięci."
                )
            )
            AppLanguage.Czech -> GlobeLocalizedText(
                title = "Glóbus",
                loadingPrefix = "Vytváření globální teplotní mapy…",
                backgroundBuilding = "Sestavování na pozadí",
                modeHeat = "Teplo",
                modeScatter = "Body",
                zoom = "Přiblížení",
                privacyHint = "Glóbus umožňuje používat hlavní funkce bez internetu. Data ve výchozím stavu zůstávají v tomto zařízení.",
                loadingFacts = listOf(
                    "Žijeme na nabitém kameni obaleném magnetickým větrem.",
                    "71 % Země pokrývá oceán, který uchovává většinu pohyblivého tepla.",
                    "Vaše obrázky se mapují do soukromé gravitace paměti."
                )
            )
            AppLanguage.Dutch -> GlobeLocalizedText(
                title = "Wereldbol",
                loadingPrefix = "Wereldwijde heatmap maken…",
                backgroundBuilding = "Bouwen op de achtergrond",
                modeHeat = "Warmte",
                modeScatter = "Punten",
                zoom = "Zoom",
                privacyHint = "Met de wereldbol gebruik je de kernfuncties zonder internet. Gegevens blijven standaard op dit apparaat.",
                loadingFacts = listOf(
                    "We leven op een geladen steen, gehuld in magnetische wind.",
                    "71% van de aarde is oceaan, die het meeste bewegende warmte opslaat.",
                    "Je afbeeldingen worden in kaart gebracht in een privézwaartekracht van herinnering."
                )
            )
            AppLanguage.Swedish -> GlobeLocalizedText(
                title = "Jordglob",
                loadingPrefix = "Skapar global värmekarta…",
                backgroundBuilding = "Byggs i bakgrunden",
                modeHeat = "Värme",
                modeScatter = "Punkter",
                zoom = "Zoom",
                privacyHint = "Jordgloben låter dig använda kärnfunktionerna utan internet. Data stannar som standard på den här enheten.",
                loadingFacts = listOf(
                    "Vi lever på en laddad sten, höljd i magnetisk vind.",
                    "71 % av jorden är hav, som lagrar det mesta av den rörliga värmen.",
                    "Dina bilder kartläggs i minnets privata tyngdkraft."
                )
            )
            AppLanguage.Danish -> GlobeLocalizedText(
                title = "Globus",
                loadingPrefix = "Opretter globalt varmekort…",
                backgroundBuilding = "Bygger i baggrunden",
                modeHeat = "Varme",
                modeScatter = "Punkter",
                zoom = "Zoom",
                privacyHint = "Globussen lader dig bruge kernefunktionerne uden internet. Data bliver som standard på denne enhed.",
                loadingFacts = listOf(
                    "Vi lever på en ladet sten, svøbt i magnetisk vind.",
                    "71 % af Jorden er hav, som lagrer det meste af den bevægelige varme.",
                    "Dine billeder bliver kortlagt i erindringens private tyngdekraft."
                )
            )
            AppLanguage.Norwegian -> GlobeLocalizedText(
                title = "Globus",
                loadingPrefix = "Lager globalt varmekart…",
                backgroundBuilding = "Bygger i bakgrunnen",
                modeHeat = "Varme",
                modeScatter = "Punkter",
                zoom = "Zoom",
                privacyHint = "Globusen lar deg bruke kjernefunksjonene uten internett. Data blir som standard på denne enheten.",
                loadingFacts = listOf(
                    "Vi lever på en ladet stein, svøpt i magnetisk vind.",
                    "71 % av jorden er hav, som lagrer mesteparten av den bevegelige varmen.",
                    "Bildene dine kartlegges i minnets private tyngdekraft."
                )
            )
            AppLanguage.Finnish -> GlobeLocalizedText(
                title = "Maapallo",
                loadingPrefix = "Luodaan maailmanlaajuista lämpökarttaa…",
                backgroundBuilding = "Rakennetaan taustalla",
                modeHeat = "Lämpö",
                modeScatter = "Pisteet",
                zoom = "Zoomaus",
                privacyHint = "Maapallolla voi käyttää perustoimintoja ilman internetiä. Tiedot pysyvät oletuksena tällä laitteella.",
                loadingFacts = listOf(
                    "Elämme latautuneella kivellä, jota magneettinen tuuli kietoo.",
                    "71 % maapallosta on valtamerta, joka varastoi suurimman osan liikkuvasta lämmöstä.",
                    "Kuvasi kartoitetaan muistin yksityiseen painovoimaan."
                )
            )
            AppLanguage.Greek -> GlobeLocalizedText(
                title = "Υδρόγειος",
                loadingPrefix = "Δημιουργία παγκόσμιου χάρτη θερμότητας…",
                backgroundBuilding = "Δημιουργία στο παρασκήνιο",
                modeHeat = "Θερμότητα",
                modeScatter = "Σημεία",
                zoom = "Ζουμ",
                privacyHint = "Η υδρόγειος επιτρέπει τη χρήση των βασικών λειτουργιών χωρίς internet. Τα δεδομένα μένουν από προεπιλογή σε αυτή τη συσκευή.",
                loadingFacts = listOf(
                    "Ζούμε σε μια φορτισμένη πέτρα τυλιγμένη από μαγνητικό άνεμο.",
                    "Το 71% της Γης είναι ωκεανός, που αποθηκεύει το μεγαλύτερο μέρος της κινούμενης θερμότητας.",
                    "Οι εικόνες σας χαρτογραφούνται στην ιδιωτική βαρύτητα της μνήμης."
                )
            )
            AppLanguage.Romanian -> GlobeLocalizedText(
                title = "Glob",
                loadingPrefix = "Se creează harta termică globală…",
                backgroundBuilding = "Se construiește în fundal",
                modeHeat = "Căldură",
                modeScatter = "Puncte",
                zoom = "Zoom",
                privacyHint = "Globul permite folosirea funcțiilor de bază fără internet. Datele rămân implicit pe acest dispozitiv.",
                loadingFacts = listOf(
                    "Trăim pe o piatră încărcată, învelită într-un vânt magnetic.",
                    "71% din Pământ este ocean, care stochează cea mai mare parte din căldura în mișcare.",
                    "Imaginile tale sunt cartografiate în gravitația privată a memoriei."
                )
            )
            AppLanguage.Hungarian -> GlobeLocalizedText(
                title = "Földgömb",
                loadingPrefix = "Globális hőtérkép készítése…",
                backgroundBuilding = "Építés a háttérben",
                modeHeat = "Hő",
                modeScatter = "Pontok",
                zoom = "Nagyítás",
                privacyHint = "A földgömb internet nélkül is használható az alapfunkciókhoz. Az adatok alapértelmezés szerint ezen az eszközön maradnak.",
                loadingFacts = listOf(
                    "Egy feltöltött kövön élünk, amelyet mágneses szél borít.",
                    "A Föld 71%-át óceán borítja, amely a mozgó hő nagy részét tárolja.",
                    "A képeid az emlékezet privát gravitációjába kerülnek leképezésre."
                )
            )
            AppLanguage.Turkish -> GlobeLocalizedText(
                title = "Küre",
                loadingPrefix = "Küresel ısı haritası oluşturuluyor…",
                backgroundBuilding = "Arka planda oluşturuluyor",
                modeHeat = "Isı",
                modeScatter = "Nokta",
                zoom = "Yakınlaştırma",
                privacyHint = "Küre, temel özelliklerin internetsiz kullanılmasını destekler. Veriler varsayılan olarak bu cihazda kalır.",
                loadingFacts = listOf(
                    "Manyetik rüzgârla sarılmış yüklü bir taşın üzerinde yaşıyoruz.",
                    "Dünya'nın %71'i okyanustur ve hareketli ısının çoğunu depolar.",
                    "Görsellerin, belleğin özel yerçekimine haritalanıyor."
                )
            )
            AppLanguage.Malay -> GlobeLocalizedText(
                title = "Glob",
                loadingPrefix = "Menjana peta haba global…",
                backgroundBuilding = "Membina di latar belakang",
                modeHeat = "Haba",
                modeScatter = "Titik",
                zoom = "Zum",
                privacyHint = "Glob menyokong penggunaan ciri teras tanpa internet. Data kekal pada peranti ini secara lalai.",
                loadingFacts = listOf(
                    "Kita hidup di atas batu bercas yang diselubungi angin magnet.",
                    "71% daripada Bumi ialah lautan, yang menyimpan kebanyakan haba bergerak.",
                    "Imej anda dipetakan ke dalam graviti peribadi ingatan."
                )
            )
            AppLanguage.Hebrew -> GlobeLocalizedText(
                title = "גלובוס",
                loadingPrefix = "יוצר מפת חום עולמית…",
                backgroundBuilding = "בנייה ברקע",
                modeHeat = "חום",
                modeScatter = "פיזור",
                zoom = "זום",
                privacyHint = "הגלובוס מאפשר להשתמש בתכונות המרכזיות בלי אינטרנט. הנתונים נשארים כברירת מחדל במכשיר הזה.",
                loadingFacts = listOf(
                    "אנחנו חיים על אבן טעונה העטופה ברוח מגנטית.",
                    "71% מכדור הארץ הוא אוקיינוס, שאוגר את רוב החום הנע.",
                    "התמונות שלכם ממופות אל כוח המשיכה הפרטי של הזיכרון."
                )
            )
            AppLanguage.English -> GlobeLocalizedText(
                title = "Globe",
                loadingPrefix = "Generating global heatmap...",
                backgroundBuilding = "Building in background",
                modeHeat = "Heat",
                modeScatter = "Scatter",
                zoom = "Zoom",
                privacyHint = "Globe supports using core features without internet. Data stays on this device by default.",
                loadingFacts = listOf(
                    "We live on a charged stone wrapped by magnetic wind.",
                    "71% of Earth is ocean, storing most of its moving heat.",
                    "Your images are being mapped into a private gravity of memory."
                )
            )
        }
    }
    val signature = remember(allMedia) { buildGlobeSignature(allMedia) }
    val totalPhotos = remember(allMedia) { allMedia.count { it.type == "photo" } }
    val catalogScanState by CatalogScanCoordinator.state.collectAsState()
    val globalScanProgress = catalogScanState.insightProgress
    val isGlobalScanning = catalogScanState.isRunning
    val globalScannedCount = remember(globalScanProgress, totalPhotos) {
        (totalPhotos * globalScanProgress).toInt().coerceIn(0, totalPhotos)
    }
    // Globe depends on EXIF/media insight prewarm, not on similar/low-quality completion.
    val globalScanReady = totalPhotos == 0 || globalScanProgress >= 0.999f
    var zoomTargetScale by remember { mutableFloatStateOf(1.0f) }
    val zoomScaleAnim = remember { Animatable(1.0f) }
    val zoomSliderInteraction = remember { MutableInteractionSource() }
    val isZoomSliderDragged by zoomSliderInteraction.collectIsDraggedAsState()
    val zoomScale = zoomScaleAnim.value
    var renderMode by remember { mutableStateOf(GlobeRenderMode.Heat) }

    LaunchedEffect(zoomTargetScale, isZoomSliderDragged) {
        val clampedTarget = zoomTargetScale.coerceIn(GLOBE_ZOOM_MIN, GLOBE_ZOOM_MAX)
        if (isZoomSliderDragged) {
            zoomScaleAnim.snapTo(clampedTarget)
        } else {
            zoomScaleAnim.animateTo(
                targetValue = clampedTarget,
                animationSpec = tween(
                    durationMillis = GLOBE_TRACK_TAP_ZOOM_DURATION_MS,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    LaunchedEffect(globalScanReady, isGlobalScanning, totalPhotos, signature) {
        if (!globalScanReady && !isGlobalScanning && totalPhotos > 0) {
            CatalogScanCoordinator.ensureForeground()
        }
    }

    val coastlineContours by produceState(initialValue = emptyList<List<Pair<Double, Double>>>()) {
        value = withContext(Dispatchers.IO) {
            CoastlineRepository.load(context)
        }
    }

    val loadState by produceState(initialValue = GlobeLoadState(), signature, globalScanReady, globalScannedCount, totalPhotos) {
        var warmStartResult: GlobeDensityResult? = null
        val runtimeHit = GlobeRuntimeCache.takeIf { it.signature == signature }?.result
        if (runtimeHit != null && (runtimeHit.points.isNotEmpty() || runtimeHit.scatterPoints.isNotEmpty())) {
            value = GlobeLoadState(
                isLoading = false,
                scanned = totalPhotos,
                total = totalPhotos,
                points = runtimeHit.points,
                scatterPoints = runtimeHit.scatterPoints,
                geoTaggedCount = runtimeHit.geoTaggedCount
            )
            return@produceState
        }

        val cached = withContext(Dispatchers.IO) {
            GlobeHeatmapCacheRepository.loadWithMeta(context, signature)
        }
        if (cached != null && (cached.points.isNotEmpty() || cached.scatterPoints.isNotEmpty())) {
            GlobeRuntimeCache.signature = signature
            GlobeRuntimeCache.result = cached
            value = GlobeLoadState(
                isLoading = false,
                scanned = totalPhotos,
                total = totalPhotos,
                points = cached.points,
                scatterPoints = cached.scatterPoints,
                geoTaggedCount = cached.geoTaggedCount
            )
            return@produceState
        }

        if (!globalScanReady) {
            value = GlobeLoadState(
                isLoading = true,
                scanned = globalScannedCount,
                total = totalPhotos,
                points = emptyList(),
                scatterPoints = emptyList(),
                geoTaggedCount = 0
            )
            return@produceState
        }

        val latest = withContext(Dispatchers.IO) {
            GlobeHeatmapCacheRepository.loadLatest(context)
        }
        if (latest != null && (latest.points.isNotEmpty() || latest.scatterPoints.isNotEmpty()) && GLOBE_USE_STALE_CACHE_ON_SIGNATURE_MISS) {
            warmStartResult = latest
            value = GlobeLoadState(
                // Warm start from persisted cache, then refresh incrementally in background.
                isLoading = true,
                scanned = totalPhotos,
                total = totalPhotos,
                points = latest.points,
                scatterPoints = latest.scatterPoints,
                geoTaggedCount = latest.geoTaggedCount
            )
        }

        if (warmStartResult == null) {
            value = GlobeLoadState(isLoading = true, scanned = 0, total = totalPhotos)
        }
        val scannedAtomic = AtomicInteger(value.scanned.coerceIn(0, totalPhotos))
        val geoTaggedAtomic = AtomicInteger(0)
        val progressJob = launch {
            while (isActive) {
                val latest = scannedAtomic.get()
                val latestGeo = geoTaggedAtomic.get()
                if (latest != value.scanned || latestGeo != value.geoTaggedCount) {
                    value = value.copy(
                        scanned = latest,
                        total = totalPhotos,
                        geoTaggedCount = latestGeo
                    )
                }
                delay(16)
            }
        }
        val result: GlobeDensityResult = withContext(GlobeScanDispatcher) {
            buildDensityPoints(
                context = context,
                allMedia = allMedia
            ) { scanned, _, geoTagged ->
                scannedAtomic.set(scanned)
                geoTaggedAtomic.set(geoTagged)
            }
        }
        progressJob.cancel()
        value = value.copy(
            scanned = totalPhotos,
            total = totalPhotos,
            geoTaggedCount = result.geoTaggedCount
        )
        GlobeRuntimeCache.signature = signature
        GlobeRuntimeCache.result = result
        withContext(Dispatchers.IO) {
            GlobeHeatmapCacheRepository.save(
                context = context,
                signature = signature,
                points = result.points,
                scatterPoints = result.scatterPoints,
                geoTaggedCount = result.geoTaggedCount
            )
        }
        value = value.copy(
            isLoading = false,
            points = result.points,
            scatterPoints = result.scatterPoints,
            geoTaggedCount = result.geoTaggedCount
        )
    }

    DisposableEffect(loadState.isLoading) {
        val activity = context as? Activity
        val hasRenderablePoints = loadState.points.isNotEmpty() || loadState.scatterPoints.isNotEmpty()
        if (loadState.isLoading && !hasRenderablePoints) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        UnifiedSubpageTopBar(
            title = localized.title,
            onBack = {
                performHaptic()
                onBack()
            },
            backContentDescription = strings.back,
            titleColor = Color.White,
            backButtonContainerColor = Color.White.copy(alpha = 0.10f),
            backButtonIconColor = Color.White
        )

        val hasRenderablePoints = loadState.points.isNotEmpty() || loadState.scatterPoints.isNotEmpty()
        if (loadState.isLoading && !hasRenderablePoints) {
            val facts = remember(localized) { localized.loadingFacts }
            var factIndex by remember { mutableStateOf(0) }
            LaunchedEffect(facts) {
                while (true) {
                    delay(2400)
                    factIndex = (factIndex + 1) % facts.size.coerceAtLeast(1)
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                MagneticBreathingBackground(
                    modifier = Modifier
                        .fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE5E7EB),
                        trackColor = Color.White.copy(alpha = 0.20f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${localized.loadingPrefix} ${loadState.scanned}/${loadState.total}",
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = facts.getOrElse(factIndex) { "" },
                        color = Color(0xFFB9D6FF).copy(alpha = 0.92f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localized.privacyHint,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            GlobeCanvas(
                points = loadState.points,
                scatterPoints = loadState.scatterPoints,
                contours = coastlineContours,
                zoomScale = zoomScale,
                renderMode = renderMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localized.privacyHint,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            if (loadState.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = localized.backgroundBuilding,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.62f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeChip(
                    text = localized.modeHeat,
                    selected = renderMode == GlobeRenderMode.Heat,
                    onClick = {
                        performHaptic()
                        renderMode = GlobeRenderMode.Heat
                    }
                )
                ModeChip(
                    text = localized.modeScatter,
                    selected = renderMode == GlobeRenderMode.Scatter,
                    onClick = {
                        performHaptic()
                        renderMode = GlobeRenderMode.Scatter
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localized.zoom,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(end = 10.dp)
                )
                Slider(
                    value = zoomScale,
                    onValueChange = {
                        zoomTargetScale = it.coerceIn(GLOBE_ZOOM_MIN, GLOBE_ZOOM_MAX)
                    },
                    valueRange = GLOBE_ZOOM_MIN..GLOBE_ZOOM_MAX,
                    modifier = Modifier.weight(1f),
                    interactionSource = zoomSliderInteraction,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFE5E7EB),
                        activeTrackColor = Color(0xFFE5E7EB),
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )
            }
        }
    }
}

@Composable
private fun MagneticBreathingBackground(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "magnetic-bg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val breath by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height * 0.40f)
        val baseR = size.minDimension * 0.27f * breath

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF5CCBFF).copy(alpha = 0.44f),
                    Color(0xFF5CCBFF).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = center,
                radius = baseR * 2.6f
            ),
            center = center,
            radius = baseR * 2.6f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFC8E6FF).copy(alpha = 0.28f),
                    Color.Transparent
                ),
                center = center,
                radius = baseR * 1.45f
            ),
            center = center,
            radius = baseR * 1.45f
        )

        for (ring in 0 until 5) {
            val path = Path()
            val r = baseR * (0.78f + ring * 0.24f)
            val amp = r * (0.08f + ring * 0.02f)
            val alpha = 0.34f - ring * 0.05f
            var started = false
            val steps = 180
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val a = (t * (2f * Math.PI).toFloat())
                val wobble = sin(a * 3f + phase * (2f * Math.PI).toFloat() + ring) * amp
                val x = center.x + cos(a) * (r + wobble)
                val y = center.y + sin(a) * (r * 0.62f + wobble * 0.45f)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFFE3F4FF).copy(alpha = alpha.coerceAtLeast(0.22f)),
                style = Stroke(width = 2.2f + ring * 0.45f)
            )
        }
    }
}

@Composable
private fun GlobeCanvas(
    points: List<GlobeDensityPoint>,
    scatterPoints: List<GlobeScatterPoint>,
    contours: List<List<Pair<Double, Double>>>,
    zoomScale: Float,
    renderMode: GlobeRenderMode,
    modifier: Modifier = Modifier
) {
    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(-0.25f) }
    var angularVX by remember { mutableFloatStateOf(0f) }
    var angularVY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastInteractionNs by remember { mutableLongStateOf(0L) }
    var entryStartNs by remember { mutableLongStateOf(0L) }
    var entryProgress by remember { mutableFloatStateOf(0f) }

    val sortedPoints = remember(points) { points.sortedByDescending { it.weight } }
    val sortedWeightsAsc = remember(points) { points.map { it.weight }.sorted() }
    val p92Weight = remember(sortedWeightsAsc) {
        if (sortedWeightsAsc.isEmpty()) 1f
        else sortedWeightsAsc[(sortedWeightsAsc.lastIndex * 0.92f).toInt().coerceIn(0, sortedWeightsAsc.lastIndex)]
            .coerceAtLeast(1f)
    }
    val maxScatterCount = remember(scatterPoints) { max(1, scatterPoints.maxOfOrNull { it.count } ?: 1) }
    val heatPoints3D = remember(sortedPoints) {
        sortedPoints.map { p -> HeatPoint3D(v = latLonToUnitVec(p.lat, p.lon), weight = p.weight) }
    }
    val safeScatterPoints = remember(scatterPoints) {
        // Defensively sanitize in case legacy cache polluted runtime types.
        scatterPoints.filter { it.lat in -90.0..90.0 && it.lon in -180.0..180.0 && it.count > 0 }
    }
    val scatterPoints3D = remember(safeScatterPoints) {
        safeScatterPoints.map { p -> ScatterPoint3D(v = latLonToUnitVec(p.lat, p.lon), count = p.count) }
    }
    val contours3D = remember(contours) {
        contours.map { contour -> contour.map { (lat, lon) -> latLonToUnitVec(lat, lon) } }
    }
    val autoSpinSpeed = 2.64f // rad/s
    val zoomedInProgress = ((zoomScale - 1f) / (GLOBE_ZOOM_MAX - 1f)).coerceIn(0f, 1f)
    val dragSensitivityFactor = lerp(1f, 0.58f, easeOutCubic(zoomedInProgress))
    val dragInertiaFactor = lerp(1f, 0.70f, easeOutCubic(zoomedInProgress))

    LaunchedEffect(Unit) {
        var lastFrameNs = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNs == 0L) {
                    lastFrameNs = now
                    entryStartNs = now
                    angularVX = GLOBE_ENTRY_START_SPIN_SPEED
                    return@withFrameNanos
                }
                val dt = ((now - lastFrameNs).coerceAtMost(33_000_000L)).toFloat() / 1_000_000_000f
                lastFrameNs = now
                if (entryProgress < 1f) {
                    val t = ((now - entryStartNs).toDouble() / GLOBE_ENTRY_DURATION_NS.toDouble())
                        .toFloat()
                        .coerceIn(0f, 1f)
                    entryProgress = t
                }
                val entryEase = easeOutCubic(entryProgress)
                val targetAutoSpinSpeed = lerp(GLOBE_ENTRY_START_SPIN_SPEED, autoSpinSpeed, entryEase)

                if (!isDragging) {
                    yaw += angularVX * dt
                    pitch = (pitch + angularVY * dt).coerceIn(-1.2f, 1.2f)

                    // Frame-rate independent damping for stable hand-feel across devices.
                    val damping = exp(-9.0f * dt)
                    angularVX *= damping
                    angularVY *= damping

                    val idleSeconds = if (lastInteractionNs == 0L) 99f else (now - lastInteractionNs).toFloat() / 1_000_000_000f
                    val idleThreshold = if (entryProgress < 1f) 0f else 2.2f
                    val spinLerp = if (entryProgress < 1f) 0.09f else 0.025f
                    if (abs(angularVX) < 0.02f && abs(angularVY) < 0.02f && idleSeconds > idleThreshold) {
                        angularVX = lerp(angularVX, targetAutoSpinSpeed, spinLerp)
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    isDragging = true
                    angularVX = 0f
                    angularVY = 0f
                    lastInteractionNs = System.nanoTime()
                },
                onDragEnd = {
                    isDragging = false
                    lastInteractionNs = System.nanoTime()
                },
                onDragCancel = {
                    isDragging = false
                    lastInteractionNs = System.nanoTime()
                }
            ) { change, dragAmount ->
                change.consume()
                // Direct-manipulation: globe follows finger direction.
                yaw -= dragAmount.x * GLOBE_DRAG_YAW_SENSITIVITY * dragSensitivityFactor
                pitch = (
                    pitch + dragAmount.y * GLOBE_DRAG_PITCH_SENSITIVITY * dragSensitivityFactor
                ).coerceIn(-1.2f, 1.2f)
                angularVX = (-dragAmount.x * GLOBE_DRAG_INERTIA_X * dragInertiaFactor)
                    .coerceIn(-1.2f, 1.2f)
                angularVY = (dragAmount.y * GLOBE_DRAG_INERTIA_Y * dragInertiaFactor)
                    .coerceIn(-1.0f, 1.0f)
            }
        }
    ) {
        val entryScale = lerp(GLOBE_ENTRY_START_SCALE, 1f, easeOutCubic(entryProgress))
        val radius = size.minDimension * 0.42f * zoomScale.coerceIn(GLOBE_ZOOM_MIN, GLOBE_ZOOM_MAX) * entryScale
        val center = Offset(size.width / 2f, size.height / 2f)
        val oceanDark = Color(0xFF02050D)
        val oceanLight = Color(0xFF0F1D33)

        // Stronger globe glow on black background.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF5E9BFF).copy(alpha = 0.24f),
                    Color(0xFF5E9BFF).copy(alpha = 0.09f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.70f
            ),
            radius = radius * 1.70f,
            center = center
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(oceanLight, oceanDark),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        val basis = ProjectionBasis(
            cosYaw = cos(yaw),
            sinYaw = sin(yaw),
            cosPitch = cos(pitch),
            sinPitch = sin(pitch)
        )

        // Formal coastline outlines from Natural Earth dataset.
        contours3D.forEach { contour ->
            val path = Path()
            var started = false
            contour.forEach { unit ->
                val p = projectUnit(unit, basis, center, radius)
                if (p != null && p.depth > 0f) {
                    if (!started) {
                        path.moveTo(p.x, p.y)
                        started = true
                    } else {
                        path.lineTo(p.x, p.y)
                    }
                } else {
                    started = false
                }
            }
            drawPath(
                path = path,
                color = Color(0xFFDDE9FF).copy(alpha = 0.74f),
                style = Stroke(width = 1.10f)
            )
        }

        when (renderMode) {
            GlobeRenderMode.Heat -> {
                var rendered = 0
                val renderLimit = if (zoomScale > 1.35f) HEAT_RENDER_MAX_POINTS + 1800 else HEAT_RENDER_MAX_POINTS
                for (point in heatPoints3D) {
                    if (rendered >= renderLimit) break
                    val p = projectUnit(point.v, basis, center, radius)
                    if (p != null && p.depth > 0f) {
                        val reference = p92Weight.coerceAtLeast(0.25f)
                        val logScaled = (
                            ln(1f + point.weight * 2.6f) /
                                ln(1f + reference * 2.6f).coerceAtLeast(0.01f)
                            ).coerceIn(0f, 1.35f)
                        val baseIntensity = smoothstep(0f, 1f, (logScaled / 1.15f).coerceIn(0f, 1f))
                        // Keep low-density travel footprints visible without flattening hotspots.
                        val intensity = (baseIntensity + 0.08f * (1f - baseIntensity)).coerceIn(0f, 1f)
                        if (intensity < 0.006f && p.depth < 0.75f) continue
                        val depthFactor = 0.40f + 0.60f * p.depth
                        val alpha = (0.12f + 0.72f * intensity) * depthFactor
                        val dotRadius = (1.75f + 10.8f * intensity) * (0.62f + 0.38f * p.depth)
                        val color = heatColor(intensity).copy(alpha = alpha)
                        drawCircle(
                            color = color.copy(alpha = alpha * 0.13f),
                            radius = dotRadius * HEAT_HALO_SCALE_OUTER,
                            center = Offset(p.x, p.y)
                        )
                        drawCircle(
                            color = color.copy(alpha = alpha * 0.24f),
                            radius = dotRadius * HEAT_HALO_SCALE_MID,
                            center = Offset(p.x, p.y)
                        )
                        drawCircle(
                            color = color.copy(alpha = alpha * 0.56f),
                            radius = dotRadius * HEAT_CORE_SCALE,
                            center = Offset(p.x, p.y)
                        )
                        rendered += 1
                    }
                }
            }
            GlobeRenderMode.Scatter -> {
                val neon = Color(0xFF39FF14)
                val renderLimit = if (zoomScale > 1.35f) SCATTER_RENDER_MAX_POINTS + 1300 else SCATTER_RENDER_MAX_POINTS
                val pointsToRender = if (scatterPoints3D.size <= renderLimit) {
                    scatterPoints3D
                } else {
                    // Uniform sampling keeps travel locations visible instead of only dense hotspots.
                    val sampled = ArrayList<ScatterPoint3D>(renderLimit)
                    val step = scatterPoints3D.size.toFloat() / renderLimit.toFloat()
                    var cursor = 0f
                    while (sampled.size < renderLimit && cursor < scatterPoints3D.size) {
                        sampled.add(scatterPoints3D[cursor.toInt().coerceIn(0, scatterPoints3D.lastIndex)])
                        cursor += step
                    }
                    sampled
                }
                for (point in pointsToRender) {
                    val p = projectUnit(point.v, basis, center, radius)
                    if (p != null && p.depth > 0f) {
                        // Use log compression so sparse travel points remain visible even with a very dense home cluster.
                        val normalized = (ln(1f + point.count.toFloat()) / ln(1f + maxScatterCount.toFloat()).coerceAtLeast(0.01f))
                            .coerceIn(0f, 1f)
                        val intensity = normalized.toDouble().pow(0.55).toFloat().coerceIn(0f, 1f)
                        val boosted = (0.22f + 0.78f * intensity).coerceIn(0f, 1f)
                        val alpha = (0.20f + 0.52f * boosted) * (0.58f + 0.42f * p.depth)
                        val r = (SCATTER_BASE_RADIUS + SCATTER_RADIUS_GAIN * intensity) * (0.74f + 0.26f * p.depth)
                        drawCircle(
                            color = neon.copy(alpha = alpha),
                            radius = r,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
            }
        }

        drawCircle(
            color = Color(0xFFC8DBFF).copy(alpha = 0.34f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.9f)
        )
    }
}

@Composable
private fun ModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (selected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                shape = CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.75f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

private fun heatColor(t: Float): Color {
    val x = smoothstep(0f, 1f, t.coerceIn(0f, 1f))
    val stops = arrayOf(
        0.00f to Color(0xFF0B3C8A),
        0.16f to Color(0xFF1D4ED8),
        0.32f to Color(0xFF06B6D4),
        0.50f to Color(0xFF2DD4BF),
        0.66f to Color(0xFF84CC16),
        0.80f to Color(0xFFF59E0B),
        0.92f to Color(0xFFF97316),
        1.00f to Color(0xFFDC2626)
    )
    for (i in 0 until stops.lastIndex) {
        val (p0, c0) = stops[i]
        val (p1, c1) = stops[i + 1]
        if (x <= p1) {
            val localT = ((x - p0) / (p1 - p0)).coerceIn(0f, 1f)
            return lerpColor(c0, c1, localT)
        }
    }
    return stops.last().second
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge1 <= edge0) return if (x >= edge1) 1f else 0f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * x,
        green = a.green + (b.green - a.green) * x,
        blue = a.blue + (b.blue - a.blue) * x,
        alpha = a.alpha + (b.alpha - a.alpha) * x
    )
}

private fun lerp(a: Float, b: Float, t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return a + (b - a) * x
}

private fun easeOutCubic(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val inv = 1f - x
    return 1f - inv * inv * inv
}

private fun latLonToUnitVec(latDeg: Double, lonDeg: Double): UnitVec3 {
    val lat = Math.toRadians(latDeg)
    val lon = Math.toRadians(lonDeg)
    val cosLat = cos(lat)
    return UnitVec3(
        x = (cosLat * cos(lon)).toFloat(),
        y = sin(lat).toFloat(),
        z = (cosLat * sin(lon)).toFloat()
    )
}

private fun projectUnit(
    unit: UnitVec3,
    basis: ProjectionBasis,
    center: Offset,
    radius: Float
): ProjectedPoint? {
    var x = unit.x
    var y = unit.y
    var z = unit.z

    val xYaw = x * basis.cosYaw + z * basis.sinYaw
    val zYaw = -x * basis.sinYaw + z * basis.cosYaw
    x = xYaw
    z = zYaw

    val yPitch = y * basis.cosPitch - z * basis.sinPitch
    val zPitch = y * basis.sinPitch + z * basis.cosPitch
    y = yPitch
    z = zPitch

    if (z <= 0f) return null
    return ProjectedPoint(
        x = center.x - x * radius,
        y = center.y - y * radius,
        depth = z
    )
}

private fun project(
    latDeg: Double,
    lonDeg: Double,
    yaw: Float,
    pitch: Float,
    center: Offset,
    radius: Float
): ProjectedPoint? {
    val lat = Math.toRadians(latDeg)
    val lon = Math.toRadians(lonDeg)
    val cosLat = cos(lat)
    var x = (cosLat * cos(lon)).toFloat()
    var y = sin(lat).toFloat()
    var z = (cosLat * sin(lon)).toFloat()

    val cosYaw = cos(yaw)
    val sinYaw = sin(yaw)
    val xYaw = x * cosYaw + z * sinYaw
    val zYaw = -x * sinYaw + z * cosYaw
    x = xYaw
    z = zYaw

    val cosPitch = cos(pitch)
    val sinPitch = sin(pitch)
    val yPitch = y * cosPitch - z * sinPitch
    val zPitch = y * sinPitch + z * cosPitch
    y = yPitch
    z = zPitch

    if (z <= 0f) return null
    return ProjectedPoint(
        // Flip horizontally so the globe skin orientation matches natural world direction.
        x = center.x - x * radius,
        y = center.y - y * radius,
        depth = z
    )
}

private suspend fun buildDensityPoints(
    context: android.content.Context,
    allMedia: List<MediaItem>,
    onProgress: (Int, Int, Int) -> Unit = { _, _, _ -> }
): GlobeDensityResult {
    GlobeCoordinateCache.ensureLoaded(context)
    val bins = HashMap<Pair<Int, Int>, Float>()
    val rawScatter = ArrayList<GlobeScatterPoint>()
    val latStep = HEAT_LAT_STEP
    val lonStep = HEAT_LON_STEP
    val latBins = (180.0 / latStep).toInt()
    val lonBins = (360.0 / lonStep).toInt()
    val photoItems = allMedia.filter { it.type == "photo" }
    val total = photoItems.size
    var geoTaggedCount = 0
    onProgress(0, total, 0)

    val photoIds = photoItems.map { it.id }
    val insightSnapshot = MediaInsightRepository.getMany(context, photoIds)

    val kernelRadius = 2
    val kernel = ArrayList<Triple<Int, Int, Float>>()
    for (dy in -kernelRadius..kernelRadius) {
        for (dx in -kernelRadius..kernelRadius) {
            val d2 = (dx * dx + dy * dy).toDouble()
            if (d2 <= (kernelRadius * kernelRadius).toDouble()) {
                val w = exp((-d2 / 2.2)).toFloat()
                kernel.add(Triple(dx, dy, w))
            }
        }
    }

    photoItems.forEachIndexed { index, media ->
        val latLon = resolveMediaLatLon(
            media = media,
            insightSnapshot = insightSnapshot
        )
        if (latLon != null) {
            val lat = latLon.first
            val lon = latLon.second
            if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                geoTaggedCount += 1
                rawScatter.add(GlobeScatterPoint(lat = lat, lon = lon))
                val latIdx = floor((lat + 90.0) / latStep).toInt().coerceIn(0, latBins - 1)
                val lonIdx = floor((lon + 180.0) / lonStep).toInt().coerceIn(0, lonBins - 1)

                kernel.forEach { (dx, dy, kw) ->
                    val y = (latIdx + dy).coerceIn(0, latBins - 1)
                    val xWrapped = ((lonIdx + dx) % lonBins + lonBins) % lonBins
                    val key = y to xWrapped
                    bins[key] = (bins[key] ?: 0f) + kw
                }
            }
        }

        onProgress(index + 1, total, geoTaggedCount)
        if ((index + 1) % 120 == 0 || index == photoItems.lastIndex) {
            // Yield periodically to avoid contending with other background jobs.
            delay(10)
        }
    }

    val points = bins
        .filter { it.value >= 0.05f }
        .map { (key, weight) ->
            val latCenter = -90.0 + (key.first + 0.5) * latStep
            val lonCenter = -180.0 + (key.second + 0.5) * lonStep
            GlobeDensityPoint(lat = latCenter, lon = lonCenter, weight = weight)
        }
        .sortedByDescending { it.weight }
        .take(15000)
    val scatterPoints = aggregateScatterPoints(
        rawPoints = rawScatter,
        targetPoints = SCATTER_TARGET_POINTS
    )
    GlobeCoordinateCache.persistIfDirty(context)
    return GlobeDensityResult(
        points = points,
        scatterPoints = scatterPoints,
        geoTaggedCount = geoTaggedCount
    )
}

private fun aggregateScatterPoints(
    rawPoints: List<GlobeScatterPoint>,
    targetPoints: Int
): List<GlobeScatterPoint> {
    if (rawPoints.isEmpty() || rawPoints.size <= targetPoints) return rawPoints

    val boundedTarget = targetPoints.coerceAtLeast(3000)
    val worldArea = 180.0 * 360.0
    var step = sqrt(worldArea / boundedTarget.toDouble()).coerceIn(0.22, 3.0)

    repeat(4) {
        val buckets = HashMap<Long, Triple<Double, Double, Int>>()
        rawPoints.forEach { p ->
            val y = floor((p.lat + 90.0) / step).toInt()
            val x = floor((p.lon + 180.0) / step).toInt()
            val key = (y.toLong() shl 32) xor (x.toLong() and 0xffffffffL)
            val prev = buckets[key]
            if (prev == null) {
                buckets[key] = Triple(p.lat, p.lon, p.count)
            } else {
                buckets[key] = Triple(
                    prev.first + p.lat,
                    prev.second + p.lon,
                    prev.third + p.count
                )
            }
        }

        if (buckets.size <= boundedTarget || it == 3) {
            return buckets.values.map { (latSum, lonSum, count) ->
                GlobeScatterPoint(
                    lat = latSum / count,
                    lon = lonSum / count,
                    count = count
                )
            }
        }

        // Too many points: increase cell size and re-aggregate.
        step = (step * 1.42).coerceAtMost(8.0)
    }

    return rawPoints.take(boundedTarget)
}

private fun resolveMediaLatLon(
    media: MediaItem,
    insightSnapshot: Map<Long, MediaInsightSnapshot>
): Pair<Double, Double>? {
    GlobeCoordinateCache.getIfKnown(media.id)?.let { return it }

    val byCachedCoords = snapshotLatLon(insightSnapshot[media.id])
    if (byCachedCoords != null) {
        GlobeCoordinateCache.put(media.id, byCachedCoords)
        return byCachedCoords
    }

    val byCachedText = parseLatLonText(insightSnapshot[media.id]?.location ?: "")
    if (byCachedText != null) {
        GlobeCoordinateCache.put(media.id, byCachedText)
        return byCachedText
    }

    val byMediaLocation = parseLatLonText(media.location)
    if (byMediaLocation != null) {
        GlobeCoordinateCache.put(media.id, byMediaLocation)
        return byMediaLocation
    }

    return null
}

private fun snapshotLatLon(snapshot: MediaInsightSnapshot?): Pair<Double, Double>? {
    val lat = snapshot?.latitude ?: return null
    val lon = snapshot.longitude ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return lat to lon
}

private fun parseLatLonText(location: String): Pair<Double, Double>? {
    val match = Regex("""\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*""").find(location) ?: return null
    val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return null
    val lon = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return lat to lon
}

private fun buildGlobeSignature(allMedia: List<MediaItem>): String {
    var count = 0
    var maxDate = 0L
    var sumIds = 0L
    var sumDates = 0L
    var xorMix = 0L

    allMedia.forEach { item ->
        if (item.type != "photo") return@forEach
        count += 1
        if (item.dateAdded > maxDate) maxDate = item.dateAdded
        sumIds += item.id
        sumDates += item.dateAdded
        xorMix = xorMix xor (item.id * 31L + item.dateAdded * 17L)
    }

    return "${GLOBE_CACHE_VERSION}_${HEAT_LAT_STEP}_${HEAT_LON_STEP}_${count}_${maxDate}_${sumIds}_${sumDates}_${xorMix}"
}




