package com.voicelike.app

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// photoo:// links from store promotions (iOS ContentView pending routes):
// photoo://photo-wall opens the photo wall on the Organize tab, photoo://organize starts card sorting.
internal enum class PhotooDeepLink { PhotoWall, Organize }

// Holds a link until the main interface is up, so a cold launch (or one that first goes through
// onboarding) still lands where the link points.
internal object PhotooDeepLinkCenter {
    private val _pending = MutableStateFlow<PhotooDeepLink?>(null)
    val pending: StateFlow<PhotooDeepLink?> = _pending.asStateFlow()

    fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "photoo") return
        _pending.value =
            when (data.host) {
                "photo-wall" -> PhotooDeepLink.PhotoWall
                "organize" -> PhotooDeepLink.Organize
                else -> return
            }
    }

    fun consume(link: PhotooDeepLink) {
        _pending.compareAndSet(link, null)
    }
}
