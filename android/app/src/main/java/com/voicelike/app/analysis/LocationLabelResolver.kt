package com.voicelike.app

import android.content.Context
import android.location.Geocoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object LocationLabelResolver {
    private val cache = ConcurrentHashMap<String, String>()

    fun parseLatLonText(location: String?): Pair<Double, Double>? {
        if (location.isNullOrBlank()) return null
        val match = Regex("""\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*""").find(location) ?: return null
        val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return null
        val lon = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    fun resolve(
        context: Context,
        lat: Double?,
        lon: Double?,
        fallbackToCoordinates: Boolean
    ): String {
        if (lat == null || lon == null) return unknownOrCoordinates(lat, lon, fallbackToCoordinates)
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return unknownOrCoordinates(lat, lon, fallbackToCoordinates)

        val key = buildKey(lat, lon, fallbackToCoordinates)
        cache[key]?.let { return it }

        val resolved = try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea
                val country = address.countryName
                when {
                    !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                    !city.isNullOrBlank() -> city
                    !country.isNullOrBlank() -> country
                    else -> unknownOrCoordinates(lat, lon, fallbackToCoordinates)
                }
            } else {
                unknownOrCoordinates(lat, lon, fallbackToCoordinates)
            }
        } catch (_: Exception) {
            unknownOrCoordinates(lat, lon, fallbackToCoordinates)
        }

        cache.putIfAbsent(key, resolved)
        return resolved
    }

    private fun buildKey(lat: Double, lon: Double, fallbackToCoordinates: Boolean): String {
        return "${String.format(Locale.US, "%.4f", lat)},${String.format(Locale.US, "%.4f", lon)}|$fallbackToCoordinates"
    }

    private fun unknownOrCoordinates(lat: Double?, lon: Double?, fallbackToCoordinates: Boolean): String {
        if (!fallbackToCoordinates || lat == null || lon == null) return "Unknown Location"
        return "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lon)}"
    }
}
