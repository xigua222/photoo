package com.voicelike.app.motionphoto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import java.io.File
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

internal object MotionPhotoFrameRenderer {
    const val DEFAULT_EFFECT_DURATION_MS = 2400L
    const val EFFECT_LOOP_PAUSE_MS = 360L

    private const val EFFECT_INITIAL_RADIUS_FRACTION = 0.17f
    private const val EFFECT_HOLD_FRACTION = 0.56f

    internal data class RevealAnchor(
        val xFraction: Float,
        val yFraction: Float
    )

    fun decodeSourceBitmap(
        sourceFile: File?,
        fallbackBitmap: Bitmap?,
        maxLongSide: Int
    ): Bitmap? {
        sourceFile
            ?.takeIf(File::exists)
            ?.let { file ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@let null
                val longSide = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                var sampleSize = 1
                while (longSide / sampleSize > maxLongSide * 2) {
                    sampleSize *= 2
                }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                    ?.let { decoded ->
                        val scaled = scaleBitmap(decoded, maxLongSide)
                        if (scaled !== decoded && !decoded.isRecycled) {
                            decoded.recycle()
                        }
                        scaled
                    }
                    ?.let { scaled ->
                        when {
                            scaled.config == Bitmap.Config.ARGB_8888 -> scaled
                            else -> scaled.copy(Bitmap.Config.ARGB_8888, false).also {
                                if (it !== scaled && !scaled.isRecycled) {
                                    scaled.recycle()
                                }
                            }
                        }
                    }
                    ?.let { return it }
            }

        val safeFallback = fallbackBitmap?.takeIf { !it.isRecycled } ?: return null
        return scaleBitmap(safeFallback, maxLongSide)
    }

    fun createGrayscaleBitmap(source: Bitmap): Bitmap {
        val grayscale = Bitmap.createBitmap(
            source.width.coerceAtLeast(1),
            source.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(grayscale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return grayscale
    }

    fun renderFrame(
        sourceBitmap: Bitmap,
        grayscaleBitmap: Bitmap,
        progress: Float,
        revealPattern: MotionPhotoRevealPattern = MotionPhotoRevealPattern.SinglePoint,
        revealSeed: Long = 0L
    ): Bitmap {
        val output = Bitmap.createBitmap(
            sourceBitmap.width.coerceAtLeast(1),
            sourceBitmap.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        canvas.drawBitmap(grayscaleBitmap, 0f, 0f, null)
        val radius = resolveRevealRadius(
            width = sourceBitmap.width.toFloat(),
            height = sourceBitmap.height.toFloat(),
            progress = progress
        )
        val path = Path().apply {
            resolveRevealAnchors(revealPattern, revealSeed).forEach { anchor ->
                addCircle(
                    sourceBitmap.width * anchor.xFraction,
                    sourceBitmap.height * anchor.yFraction,
                    radius,
                    Path.Direction.CW
                )
            }
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
        canvas.restore()
        return output
    }

    fun resolveRevealRadius(
        width: Float,
        height: Float,
        progress: Float
    ): Float {
        val safeProgress = progress.coerceIn(0f, 1f)
        if (safeProgress <= 0f) return 0f
        val fullRadius = (hypot(width.toDouble(), height.toDouble()).toFloat() * 0.5f).coerceAtLeast(1f)
        val initialRadius = (fullRadius * EFFECT_INITIAL_RADIUS_FRACTION).coerceAtLeast(1f)
        if (safeProgress <= EFFECT_HOLD_FRACTION) {
            return initialRadius
        }
        val expansionProgress = ((safeProgress - EFFECT_HOLD_FRACTION) / (1f - EFFECT_HOLD_FRACTION))
            .coerceIn(0f, 1f)
        val easedExpansion = 1f - (1f - expansionProgress).let { remaining ->
            remaining * remaining * remaining
        }
        return initialRadius + ((fullRadius - initialRadius) * easedExpansion)
    }

    fun resolveRevealAnchors(
        revealPattern: MotionPhotoRevealPattern,
        revealSeed: Long = 0L
    ): List<RevealAnchor> {
        val pointCount = when (revealPattern) {
            MotionPhotoRevealPattern.SinglePoint -> 1
            MotionPhotoRevealPattern.DualPoint -> 2
            MotionPhotoRevealPattern.TriplePoint -> 3
        }
        return buildRandomRevealAnchors(pointCount, revealSeed)
    }

    private fun buildRandomRevealAnchors(
        pointCount: Int,
        revealSeed: Long
    ): List<RevealAnchor> {
        val random = Random(revealSeed)
        val anchors = mutableListOf<RevealAnchor>()
        val minimumDistance = when (pointCount) {
            1 -> 0.0f
            2 -> 0.22f
            else -> 0.18f
        }
        repeat(pointCount) {
            var resolved: RevealAnchor? = null
            repeat(24) {
                val candidate = RevealAnchor(
                    xFraction = random.nextFloat() * 0.48f + 0.26f,
                    yFraction = random.nextFloat() * 0.48f + 0.26f
                )
                if (anchors.none { existing ->
                        hypot(
                            (existing.xFraction - candidate.xFraction).toDouble(),
                            (existing.yFraction - candidate.yFraction).toDouble()
                        ) < minimumDistance
                    }
                ) {
                    resolved = candidate
                    return@repeat
                }
            }
            anchors += resolved ?: RevealAnchor(
                xFraction = 0.5f,
                yFraction = 0.5f
            )
        }
        return anchors
    }

    private fun scaleBitmap(
        source: Bitmap,
        maxLongSide: Int
    ): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val longSide = max(width, height)
        val normalizedWidth = width.toEvenDimension()
        val normalizedHeight = height.toEvenDimension()
        if (longSide <= maxLongSide) {
            val normalized = when {
                width == normalizedWidth && height == normalizedHeight -> source
                else -> Bitmap.createScaledBitmap(source, normalizedWidth, normalizedHeight, true)
            }
            return if (normalized.config == Bitmap.Config.ARGB_8888) {
                normalized.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                normalized.copy(Bitmap.Config.ARGB_8888, false).also {
                    if (normalized !== source && !normalized.isRecycled) {
                        normalized.recycle()
                    }
                }
            }
        }
        val scale = maxLongSide.toFloat() / longSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(2).toEvenDimension()
        val targetHeight = (height * scale).toInt().coerceAtLeast(2).toEvenDimension()
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun Int.toEvenDimension(): Int = when {
        this <= 2 -> 2
        this % 2 == 0 -> this
        else -> this - 1
    }
}
