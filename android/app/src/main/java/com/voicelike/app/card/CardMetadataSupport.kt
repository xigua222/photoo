package com.voicelike.app

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun parseVideoLocationMetadata(raw: String?): Pair<Double, Double>? {
    if (raw.isNullOrBlank()) return null
    val text = raw.trim()
    val match = Regex("([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?).*").find(text) ?: return null
    val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return null
    val lon = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return lat to lon
}

internal fun parseLatLonTextLocal(location: String?): Pair<Double, Double>? {
    return LocationLabelResolver.parseLatLonText(location)
}

internal fun resolveLocationLabel(context: Context, lat: Double?, lon: Double?): String {
    return LocationLabelResolver.resolve(context, lat, lon, fallbackToCoordinates = true)
}

internal fun formatVideoBitrate(rawBps: String?): String {
    val bps = rawBps?.toLongOrNull() ?: return "--"
    if (bps <= 0L) return "--"
    val mbps = bps / 1_000_000.0
    return if (mbps >= 1.0) {
        String.format(Locale.US, "%.1f Mbps", mbps)
    } else {
        String.format(Locale.US, "%.0f Kbps", bps / 1000.0)
    }
}

internal fun formatVideoFps(captureFps: String?, frameCount: String?, durationMs: String?): String {
    val direct = captureFps?.toFloatOrNull()?.takeIf { it > 0f }
    if (direct != null) return String.format(Locale.US, "%.1f fps", direct)
    val frames = frameCount?.toLongOrNull()?.takeIf { it > 0L }
    val duration = durationMs?.toLongOrNull()?.takeIf { it > 0L }
    if (frames != null && duration != null) {
        val fps = (frames * 1000.0) / duration.toDouble()
        if (fps > 0.0) return String.format(Locale.US, "%.1f fps", fps)
    }
    return "--"
}

internal fun formatVideoDuration(durationMs: String?): String {
    val ms = durationMs?.toLongOrNull() ?: return "--"
    if (ms <= 0L) return "--"
    val totalSec = ms / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

internal fun formatVideoCodec(mime: String?): String {
    if (mime.isNullOrBlank()) return "--"
    return when (mime.lowercase(Locale.US)) {
        "video/avc", "video/h264", "video/x-h264" -> "H.264"
        "video/hevc", "video/h265", "video/x-h265" -> "H.265"
        "video/mp4v-es" -> "MPEG-4"
        "video/3gpp" -> "3GPP"
        "video/webm" -> "WebM"
        else -> mime.substringAfter('/').uppercase(Locale.US)
    }
}

internal fun formatVideoAudioTrack(hasAudio: String?): String {
    return when (hasAudio?.trim()) {
        "yes", "1", "true", "YES", "TRUE" -> "Yes"
        "no", "0", "false", "NO", "FALSE" -> "No"
        else -> "--"
    }
}

internal fun resolvePreferredCardExifFocalLength(exif: ExifInterface): String {
    val focalLength35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)
        ?.trim()
        .orEmpty()
    if (focalLength35mm.isNotBlank() && focalLength35mm != "0") {
        return focalLength35mm
    }
    return exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        ?.trim()
        .takeUnless { it.isNullOrBlank() || it == "0" }
        ?: "--"
}

internal fun formatCardFocalLength(raw: String?): String {
    val value = raw?.trim().orEmpty()
    return when {
        value.isBlank() || value == "--" -> "--"
        value.contains("/") -> {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].toDoubleOrNull()
                val denominator = parts[1].toDoubleOrNull()
                if (numerator != null && denominator != null && denominator != 0.0) {
                    "${(numerator / denominator).roundToInt()}mm"
                } else {
                    value
                }
            } else {
                value
            }
        }
        else -> {
            val asNumber = value.toDoubleOrNull()
            if (asNumber != null) {
                "${asNumber.roundToInt()}mm"
            } else if (value.endsWith("mm", ignoreCase = true)) {
                value
            } else {
                "${value}mm"
            }
        }
    }
}

internal fun loadPhotoCardMetadata(context: Context, item: MediaItem): MediaItem? {
    return try {
        openExifInputStream(context, item.uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val shooting = readExifShootingValues(exif)
            val cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
            val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
            val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "--"
            val shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "--"
            val focalLength = resolvePreferredCardExifFocalLength(exif)
            val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "--"
            val latLong = exif.latLong
            val cachedLocationRaw = MediaInsightRepository.get(context, item.id)?.location
                ?.takeIf { it.isNotBlank() && !it.equals("Unknown Location", ignoreCase = true) }
            var cachedLocation = cachedLocationRaw
            val cachedLatLon = cachedLocationRaw?.let { parseLatLonTextLocal(it) }
            if (cachedLatLon != null) {
                val upgraded = resolveLocationLabel(context, cachedLatLon.first, cachedLatLon.second)
                if (!upgraded.equals("Unknown Location", ignoreCase = true)) {
                    cachedLocation = upgraded
                    MediaInsightRepository.upsertLocation(
                        context = context,
                        mediaId = item.id,
                        location = upgraded,
                        latitude = cachedLatLon.first,
                        longitude = cachedLatLon.second
                    )
                }
            }

            val isValidLocation = latLong != null && (abs(latLong[0]) > 0.0001 || abs(latLong[1]) > 0.0001)
            val location = cachedLocation ?: if (isValidLocation) {
                resolveLocationLabel(context, latLong!![0], latLong[1])
            } else {
                "Unknown Location"
            }

            val cameraRaw = if (cameraMake.isNotEmpty() || cameraModel.isNotEmpty()) {
                "$cameraMake $cameraModel".trim()
            } else {
                "--"
            }
            val camera = DeviceModelNameResolver.resolveCameraLabel(context, cameraRaw)
            val fNumber = if (aperture != "--") "f/$aperture" else "--"
            val focal = formatCardFocalLength(focalLength)

            MediaInsightRepository.upsertExif(
                context = context,
                mediaId = item.id,
                camera = camera,
                location = location,
                latitude = if (isValidLocation) latLong?.get(0) else null,
                longitude = if (isValidLocation) latLong?.get(1) else null,
                focal35 = shooting.focal35,
                aperture = shooting.aperture,
                iso = shooting.iso,
                exposure = shooting.exposure,
                shootingScanned = true,
                metadataScanned = true
            )

            val motionInfo = MotionPhotoHelper.detectAndPersist(context, item)
            item.copy(
                camera = camera,
                iso = iso,
                shutter = formatShutterSpeed(shutter),
                focalLength = focal,
                aperture = fNumber,
                location = location,
                isMotionPhoto = motionInfo.isMotionPhoto,
                motionPhotoVideoOffset = motionInfo.videoOffset,
                motionPhotoVideoLength = motionInfo.videoLength
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

internal fun loadVideoCardMetadata(context: Context, item: MediaItem): MediaItem? {
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            val latLon = parseVideoLocationMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            )
            val location = resolveLocationLabel(context, latLon?.first, latLon?.second)
            val bitrate = formatVideoBitrate(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            )
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val fps = formatVideoFps(
                captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE),
                frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT),
                durationMs = duration
            )
            val codec = formatVideoCodec(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: item.mimeType
            )
            val audioTrack = formatVideoAudioTrack(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            )
            val durationText = formatVideoDuration(duration)

            MediaInsightRepository.upsertExif(
                context = context,
                mediaId = item.id,
                camera = null,
                location = location,
                latitude = latLon?.first,
                longitude = latLon?.second
            )

            item.copy(
                camera = codec,
                iso = bitrate,
                shutter = fps,
                focalLength = durationText,
                aperture = audioTrack,
                location = location
            )
        } finally {
            runCatching { retriever.release() }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
