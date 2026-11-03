package com.voicelike.app.motionphoto

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

internal object MotionPhotoVideoEncoder {

    suspend fun encodeFrameSequenceToMp4(
        frameCount: Int,
        frameProvider: suspend (Int) -> Bitmap,
        outputFile: File,
        spec: MotionPhotoRenderSpec,
        preferredVideoMimeTypes: List<String> = listOf(MediaFormat.MIMETYPE_VIDEO_AVC)
    ) {
        require(frameCount > 0) { "No frames to encode" }
        val initialFrame = frameProvider(0)
        var firstFrame: Bitmap? = initialFrame
        val width = initialFrame.width.even()
        val height = initialFrame.height.even()
        val videoMimeType = resolveSupportedVideoMimeType(preferredVideoMimeTypes)
        val colorFormat = resolveSupportedColorFormat(videoMimeType)
        val frameConverter = YuvFrameConverter(width = width, height = height, colorFormat = colorFormat)
        val format = MediaFormat.createVideoFormat(videoMimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, spec.videoBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, spec.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(videoMimeType)
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        var muxerStarted = false

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        try {
            for (index in 0 until frameCount) {
                val bitmap = if (index == 0) {
                    firstFrame!!.also { firstFrame = null }
                } else {
                    frameProvider(index)
                }
                val frame = if (bitmap.width == width && bitmap.height == height) {
                    bitmap
                } else {
                    Bitmap.createScaledBitmap(bitmap, width, height, true)
                }
                try {
                    val inputBufferIndex = waitForInputBuffer(codec)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                            ?: error("Missing input buffer")
                        inputBuffer.clear()
                        val yuv = frameConverter.convert(frame)
                        inputBuffer.put(yuv)
                        val presentationTimeUs = (index * 1_000_000L) / spec.frameRate
                        codec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            yuv.size,
                            presentationTimeUs,
                            0
                        )
                    }
                    drainEncoder(codec, muxer, bufferInfo) { formatChanged ->
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(formatChanged)
                            muxer.start()
                            muxerStarted = true
                        }
                        trackIndex
                    }
                } finally {
                    if (frame !== bitmap && !frame.isRecycled) {
                        frame.recycle()
                    }
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }

            val eosIndex = waitForInputBuffer(codec)
            if (eosIndex >= 0) {
                codec.queueInputBuffer(
                    eosIndex,
                    0,
                    0,
                    (frameCount * 1_000_000L) / spec.frameRate,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            var outputDone = false
            while (!outputDone) {
                outputDone = drainEncoder(codec, muxer, bufferInfo) { formatChanged ->
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(formatChanged)
                        muxer.start()
                        muxerStarted = true
                    }
                    trackIndex
                }
            }
        } finally {
            firstFrame?.takeIf { !it.isRecycled }?.recycle()
            runCatching { codec.stop() }
            runCatching { codec.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private fun waitForInputBuffer(codec: MediaCodec): Int {
        while (true) {
            val index = codec.dequeueInputBuffer(10_000)
            if (index >= 0) return index
        }
    }

    private fun resolveSupportedVideoMimeType(preferredVideoMimeTypes: List<String>): String {
        val normalizedPreferences = preferredVideoMimeTypes
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(MediaFormat.MIMETYPE_VIDEO_AVC) }
        val encoders = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { it.isEncoder }
        return normalizedPreferences.firstOrNull { preferredMimeType ->
            encoders.any { info ->
                info.supportedTypes.any { type ->
                    type.equals(preferredMimeType, ignoreCase = true)
                }
            }
        } ?: MediaFormat.MIMETYPE_VIDEO_AVC
    }

    private fun resolveSupportedColorFormat(videoMimeType: String): Int {
        val codecName = MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .firstOrNull { info ->
                info.isEncoder && info.supportedTypes.any { type ->
                    type.equals(videoMimeType, ignoreCase = true)
                }
            }?.name
            ?: return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        val codecInfo = MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .firstOrNull { it.name == codecName }
            ?: return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        val capabilities = codecInfo.getCapabilitiesForType(videoMimeType)
        val preferredFormats = listOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        return preferredFormats.firstOrNull { it in capabilities.colorFormats.toSet() }
            ?: MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        ensureTrack: (MediaFormat) -> Int
    ): Boolean {
        var sawEos = false
        while (true) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return sawEos
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    ensureTrack(codec.outputFormat)
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        ?: error("Missing output buffer")
                    if (bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(ensureTrack(codec.outputFormat), outputBuffer, bufferInfo)
                    }
                    sawEos = sawEos || (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if (sawEos) return true
                }
            }
        }
    }

    private class YuvFrameConverter(
        private val width: Int,
        private val height: Int,
        private val colorFormat: Int
    ) {
        private val argbBuffer = IntArray(width * height)
        private val yuvBuffer = ByteArray(width * height * 3 / 2)

        fun convert(bitmap: Bitmap): ByteArray {
            bitmap.getPixels(argbBuffer, 0, width, 0, 0, width, height)
            return when (colorFormat) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> fillI420()
                else -> fillNv12()
            }
        }

        private fun fillNv12(): ByteArray {
            var yIndex = 0
            var uvIndex = width * height
            var index = 0

            for (j in 0 until height) {
                for (i in 0 until width) {
                    val color = argbBuffer[index++]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                    yuvBuffer[yIndex++] = y.coerceIn(0, 255).toByte()
                    if (j % 2 == 0 && i % 2 == 0) {
                        yuvBuffer[uvIndex++] = u.coerceIn(0, 255).toByte()
                        yuvBuffer[uvIndex++] = v.coerceIn(0, 255).toByte()
                    }
                }
            }
            return yuvBuffer
        }

        private fun fillI420(): ByteArray {
            val frameSize = width * height
            var yIndex = 0
            var uIndex = frameSize
            var vIndex = frameSize + (frameSize / 4)
            var index = 0

            for (j in 0 until height) {
                for (i in 0 until width) {
                    val color = argbBuffer[index++]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    yuvBuffer[yIndex++] = y.coerceIn(0, 255).toByte()
                    if (j % 2 == 0 && i % 2 == 0) {
                        yuvBuffer[uIndex++] = u.coerceIn(0, 255).toByte()
                        yuvBuffer[vIndex++] = v.coerceIn(0, 255).toByte()
                    }
                }
            }
            return yuvBuffer
        }
    }

    private fun Int.even(): Int = if (this % 2 == 0) this else this - 1
}
