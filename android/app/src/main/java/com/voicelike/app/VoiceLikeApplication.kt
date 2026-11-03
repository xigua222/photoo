package com.voicelike.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import android.os.Process
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import kotlin.system.exitProcess

class VoiceLikeApplication : Application(), ImageLoaderFactory {
    companion object {
        private const val ENABLE_JANK_MONITOR = false
    }

    override fun onCreate() {
        super.onCreate()
        CrashDebugProbe.onProcessCreate(this)
        // Explains the previous process's end (native crash, ANR, system kill) off the main thread.
        Thread({ AppExitReasonReporter.reportPreviousExits(this) }, "exit-reason-report").start()
        LowQualityStrategyGate.registerAppLaunch(this, BuildConfig.VERSION_CODE)
        MemorySafetyMode.registerAppLaunch(this, BuildConfig.VERSION_CODE)
        ScanCacheMigrationManager.migrateIfNeeded(this)
        installCrashHandler()
        if (ENABLE_JANK_MONITOR) {
            AppJankMonitor.start(this)
        }
        AppPerformanceGovernor.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        val conservativeMemoryMode = MemorySafetyMode.shouldUseConservativeMemory(this)
        val oomFallback = MemorySafetyMode.isOomFallbackActive(this)
        val memoryCachePercent = when {
            oomFallback -> 0.08
            conservativeMemoryMode -> 0.10
            else -> 0.15
        }
        val maxMemoryCacheBytes = when {
            oomFallback -> 24 * 1024 * 1024
            conservativeMemoryMode -> 28 * 1024 * 1024
            else -> 40 * 1024 * 1024
        }

        return ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .maxSizeBytes(maxMemoryCacheBytes)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.04)
                    .maxSizeBytes(120 * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(!conservativeMemoryMode)
            .components {
                add(VideoFrameDecoder.Factory())
                add(SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(150)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        AppPerformanceGovernor.onTrimMemory(level)
        when {
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                runCatching { imageLoader.memoryCache?.clear() }
                runCatching { MediaAnalysisManager.trimTransientCaches() }
                runCatching { SimilarPhotoManager.trimTransientCaches() }
                runCatching { MotionPhotoHelper.trimTransientCaches() }
                runCatching { VideoPlayerCache.trimToMinimum() }
            }

            AppPerformanceGovernor.isRunningCriticalTrimLevel(level) -> {
                runCatching { imageLoader.memoryCache?.clear() }
                runCatching { MediaAnalysisManager.trimTransientCaches() }
                runCatching { SimilarPhotoManager.trimTransientCaches() }
                runCatching { MotionPhotoHelper.trimTransientCaches() }
                runCatching { VideoPlayerCache.trimIdlePlayers() }
            }

            AppPerformanceGovernor.isRunningLowTrimLevel(level) -> {
                runCatching { MediaAnalysisManager.trimTransientCaches() }
                runCatching { SimilarPhotoManager.trimTransientCaches() }
                runCatching { MotionPhotoHelper.trimTransientCaches() }
                runCatching { VideoPlayerCache.trimIdlePlayers() }
            }

            AppPerformanceGovernor.isRunningModerateTrimLevel(level) -> {
                runCatching { MediaAnalysisManager.trimTransientCaches() }
                runCatching { SimilarPhotoManager.trimTransientCaches() }
                runCatching { MotionPhotoHelper.trimTransientCaches() }
                runCatching { VideoPlayerCache.trimIdlePlayers() }
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AppPerformanceGovernor.onLowMemory()
        runCatching { imageLoader.memoryCache?.clear() }
        runCatching { MediaAnalysisManager.trimTransientCaches() }
        runCatching { SimilarPhotoManager.trimTransientCaches() }
        runCatching { MotionPhotoHelper.trimTransientCaches() }
        runCatching { VideoPlayerCache.trimToMinimum() }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            kotlin.runCatching {
                val isOom = containsOutOfMemory(throwable)
                if (isOom) {
                    MemorySafetyMode.markOomCrash(this)
                    AppRuntimeLogStore.logCrashLowOverhead(this, "uncaught:${thread.name}", throwable)
                } else {
                    AppRuntimeLogStore.logCrash(this, "uncaught:${thread.name}", throwable)
                }
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun containsOutOfMemory(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 8) {
            if (current is OutOfMemoryError) return true
            current = current.cause
            depth++
        }
        return false
    }
}
