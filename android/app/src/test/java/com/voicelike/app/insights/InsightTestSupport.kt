package com.voicelike.app

import android.net.FakeUri
import java.util.Calendar

/** Epoch seconds of a local wall-clock time in the JVM default zone. */
internal fun localSeconds(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(year, month - 1, day, hour, minute, 0)
    return calendar.timeInMillis / 1000
}

internal fun localMillis(year: Int, month: Int, day: Int, hour: Int = 12): Long = localSeconds(year, month, day, hour) * 1000

internal fun photo(
    id: Long,
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 12,
    minute: Int = 0,
    width: Int = 3000,
    height: Int = 4000,
    path: String = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg",
    bucket: String = "Camera",
    displayName: String = "IMG_$id.jpg",
    mimeType: String = "image/jpeg",
    live: Boolean = false
) = MediaItem(
    id = id,
    uri = FakeUri.INSTANCE,
    type = "photo",
    dateAdded = localSeconds(year, month, day, hour, minute),
    size = 1000,
    width = width,
    height = height,
    mimeType = mimeType,
    displayName = displayName,
    path = path,
    bucketName = bucket,
    isLivePhoto = live
)

internal fun screenshot(id: Long, year: Int, month: Int, day: Int, hour: Int = 12) = photo(
    id, year, month, day, hour,
    path = "/storage/emulated/0/Pictures/Screenshots/Screenshot_$id.png",
    bucket = "Screenshots",
    displayName = "Screenshot_$id.png",
    mimeType = "image/png"
)

internal fun video(id: Long, year: Int, month: Int, day: Int, hour: Int = 12, durationMs: Long = 30_000) = MediaItem(
    id = id,
    uri = FakeUri.INSTANCE,
    type = "video",
    dateAdded = localSeconds(year, month, day, hour),
    size = 1000,
    width = 1920,
    height = 1080,
    mimeType = "video/mp4",
    displayName = "VID_$id.mp4",
    path = "/storage/emulated/0/DCIM/Camera/VID_$id.mp4",
    bucketName = "Camera",
    duration = durationMs
)

internal fun shot(
    focal: Float? = null,
    aperture: Float? = null,
    iso: Int? = null,
    exposure: Double? = null,
    camera: String? = null
) = MediaInsightSnapshot(
    camera = camera,
    focal35 = focal,
    aperture = aperture,
    iso = iso,
    exposure = exposure,
    shootingScanned = true,
    metadataScanned = true
)

internal fun geo(lat: Double, lon: Double, location: String? = null) =
    MediaInsightSnapshot(latitude = lat, longitude = lon, location = location, metadataScanned = true)

/** 2026-10-16 12:00 local. */
internal val NOW = localMillis(2026, 10, 16)
