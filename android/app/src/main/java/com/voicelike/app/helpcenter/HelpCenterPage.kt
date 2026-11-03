package com.voicelike.app.helpcenter

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.voicelike.app.AppLanguage
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.UnifiedSubpageTopBarIconButton

private const val helpCenterUrl = "https://photooapp.com/help/"

fun getHelpCenterActionTitle(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SimplifiedChinese -> "\u5e2e\u52a9\u4e2d\u5fc3"
        AppLanguage.TraditionalChinese -> "\u5e6c\u52a9\u4e2d\u5fc3"
        AppLanguage.Japanese -> "\u30d8\u30eb\u30d7\u30bb\u30f3\u30bf\u30fc"
        AppLanguage.Korean -> "\ub3c4\uc6c0\ub9d0\uc815\uc2e0"
        AppLanguage.French -> "Centre d'aide"
        AppLanguage.German -> "Hilfezentrum"
        AppLanguage.Italian -> "Centro Assistenza"
        AppLanguage.Spanish -> "Centro de Ayuda"
        AppLanguage.Portuguese -> "Central de Ajuda"
        AppLanguage.Russian -> "\u0426\u0435\u043d\u0442\u0440 \u043f\u043e\u043c\u043e\u0449\u0438"
        AppLanguage.Thai -> "\u0e28\u0e39\u0e19\u0e22\u0e4c\u0e04\u0e27\u0e49\u0e32\u0e07\u0e40\u0e01\u0e35\u0e22\u0e23\u0e15\u0e34"
        AppLanguage.Vietnamese -> "Trung tâm Trợ giúp"
        AppLanguage.Indonesian -> "Pusat Bantuan"
        AppLanguage.Arabic -> "\u0645\u0631\u0643\u0632 \u0627\u0644\u0645\u0633\u0627\u0639\u062f\u0629"
        AppLanguage.Ukrainian -> "Довідка"
        AppLanguage.Polish -> "Centrum pomocy"
        AppLanguage.Czech -> "Centrum nápovědy"
        AppLanguage.Dutch -> "Helpcentrum"
        AppLanguage.Swedish -> "Hjälpcenter"
        AppLanguage.Danish -> "Hjælpecenter"
        AppLanguage.Norwegian -> "Hjelpesenter"
        AppLanguage.Finnish -> "Ohjekeskus"
        AppLanguage.Greek -> "Κέντρο βοήθειας"
        AppLanguage.Romanian -> "Centru de ajutor"
        AppLanguage.Hungarian -> "Súgóközpont"
        AppLanguage.Turkish -> "Yardım Merkezi"
        AppLanguage.Malay -> "Pusat Bantuan"
        AppLanguage.Hebrew -> "מרכז העזרה"
        AppLanguage.English -> "Help Center"
    }
}

@Composable
fun HelpCenterPage(
    onBack: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }

    BackHandler {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
            webView = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    configureHelpCenterWebView(
                        onProgressChanged = { progress ->
                            loadingProgress = progress / 100f
                            isLoading = progress < 100
                        },
                        onPageStarted = {
                            isLoading = true
                            if (loadingProgress <= 0f || loadingProgress >= 1f) {
                                loadingProgress = 0.08f
                            }
                        },
                        onPageFinished = {
                            loadingProgress = 1f
                            isLoading = false
                        }
                    )
                    loadUrl(helpCenterUrl)
                    webView = this
                }
            },
            update = { view ->
                webView = view
                if (view.url.isNullOrBlank()) {
                    view.loadUrl(helpCenterUrl)
                }
            }
        )

        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadingProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
        UnifiedSubpageTopBarIconButton(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = LocalAppStrings.current.back,
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
            iconTint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureHelpCenterWebView(
    onProgressChanged: (Int) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        settings.safeBrowsingEnabled = true
    }

    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setAcceptThirdPartyCookies(this@configureHelpCenterWebView, true)
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgressChanged(newProgress)
        }
    }
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageStarted()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished()
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean = false
    }
}
