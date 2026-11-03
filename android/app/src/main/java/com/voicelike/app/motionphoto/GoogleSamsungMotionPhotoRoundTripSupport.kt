package com.voicelike.app.motionphoto

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.voicelike.app.MediaItem

internal data class GoogleSamsungMotionPhotoRoundTripPayload(
    val sourceXmp: String?,
    val presentationTimestampUs: Long?,
    val samsungMotionPhotoVersion: String?
)

internal object GoogleSamsungMotionPhotoRoundTripSupport {
    private val motionPresentationTimestampRegex =
        Regex("""GCamera:MotionPhotoPresentationTimestampUs\s*=\s*['"](-?\d+)['"]""")
    private val microVideoPresentationTimestampRegex =
        Regex("""GCamera:MicroVideoPresentationTimestampUs\s*=\s*['"](-?\d+)['"]""")
    private val samsungMotionPhotoVersionRegex =
        Regex("""MotionPhoto_Version(mpv[\w.\-]{1,16})""")

    fun extract(
        context: Context,
        sourceItem: MediaItem
    ): GoogleSamsungMotionPhotoRoundTripPayload? {
        if (!sourceItem.isMotionPhoto) return null
        val fileBytes = context.contentResolver.openInputStream(sourceItem.uri)?.use { it.readBytes() }
            ?: return null
        val xmp = runCatching {
            ExifInterface(fileBytes.inputStream()).run {
                getAttribute(ExifInterface.TAG_XMP)
                    ?: getAttributeBytes(ExifInterface.TAG_XMP)?.toString(Charsets.UTF_8)
            }
        }.getOrNull()

        val presentationTimestampUs = motionPresentationTimestampRegex
            .find(xmp.orEmpty())
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?: microVideoPresentationTimestampRegex
                .find(xmp.orEmpty())
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        val samsungMotionPhotoVersion = extractSamsungMotionPhotoVersion(fileBytes)

        if (xmp.isNullOrBlank() && presentationTimestampUs == null && samsungMotionPhotoVersion == null) {
            return null
        }
        return GoogleSamsungMotionPhotoRoundTripPayload(
            sourceXmp = xmp,
            presentationTimestampUs = presentationTimestampUs,
            samsungMotionPhotoVersion = samsungMotionPhotoVersion
        )
    }

    private fun extractSamsungMotionPhotoVersion(fileBytes: ByteArray): String? {
        val tailWindow = fileBytes.copyOfRange(
            fromIndex = (fileBytes.size - 4096).coerceAtLeast(0),
            toIndex = fileBytes.size
        )
        val tailText = tailWindow.toString(Charsets.ISO_8859_1)
        return samsungMotionPhotoVersionRegex
            .find(tailText)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.startsWith("mpv", ignoreCase = true) }
    }
}
