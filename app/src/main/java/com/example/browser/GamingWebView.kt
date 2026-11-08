package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ScreenOrientationMode
import com.example.model.UserAgentMode

@SuppressLint("SetJavaScriptEnabled")
fun setupGamingWebSettings(
    webView: WebView,
    userAgentMode: UserAgentMode,
    gamingWebClient: GamingWebClient? = null
) {
    // 1. Direct GPU composition: LAYER_TYPE_NONE allows Chromium's native compositor to render WebGL directly
    // without forcing an expensive, memory-heavy off-screen View texture layer.
    webView.setLayerType(View.LAYER_TYPE_NONE, null)
    webView.setBackgroundColor(android.graphics.Color.BLACK)
    webView.isFocusable = true
    webView.isFocusableInTouchMode = true
    webView.overScrollMode = View.OVER_SCROLL_NEVER
    webView.keepScreenOn = true

    val settings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.loadWithOverviewMode = true
    settings.useWideViewPort = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.allowFileAccess = false
    settings.allowContentAccess = true

    // Disable Safe Browsing cloud lookups on game assets to cut loading screen times
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        settings.safeBrowsingEnabled = false
    }

    // Enable mixed content for cloud streaming servers and game websockets
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

    // Performance and responsiveness optimizations
    settings.offscreenPreRaster = true
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.loadsImagesAutomatically = true
    settings.blockNetworkImage = false
    settings.blockNetworkLoads = false
    settings.javaScriptCanOpenWindowsAutomatically = true

    // Configure ServiceWorker caching for offline asset bundles, shaders, and web workers
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val swController = android.webkit.ServiceWorkerController.getInstance()
            swController.serviceWorkerWebSettings.cacheMode = WebSettings.LOAD_DEFAULT
            swController.serviceWorkerWebSettings.allowContentAccess = true
            if (gamingWebClient != null) {
                swController.setServiceWorkerClient(object : android.webkit.ServiceWorkerClient() {
                    override fun shouldInterceptRequest(
                        request: android.webkit.WebResourceRequest
                    ): android.webkit.WebResourceResponse? {
                        val uri = request.url ?: return null
                        if (gamingWebClient.adBlockManager.shouldBlock(uri)) {
                            return android.webkit.WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        val cached = gamingWebClient.gameAssetCacheManager.shouldInterceptRequest(
                            request = request,
                            userAgent = webView.settings.userAgentString
                        )
                        if (cached != null) return cached
                        return super.shouldInterceptRequest(request)
                    }
                })
            }
        } catch (_: Exception) {}
    }

    // Apply User Agent
    if (userAgentMode.value.isNotEmpty()) {
        settings.userAgentString = userAgentMode.value
    } else {
        settings.userAgentString = null // Reset to default
    }

    // Enable third-party cookies for cross-origin cloud gaming authentication
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)
}

@Composable
fun GamingWebViewContainer(
    currentUrl: String,
    userAgentMode: UserAgentMode,
    gamingWebClient: GamingWebClient,
    onWebViewReady: (WebView) -> Unit,
    onOrientationChange: (ScreenOrientationMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastLoadedUrl by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    // Retain WebView instance across orientation and configuration changes
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            webViewClient = gamingWebClient.webViewClient
            webChromeClient = gamingWebClient.webChromeClient
            setupGamingWebSettings(this, userAgentMode, gamingWebClient)

            // Register native bridge for HTML5 games to request orientation or handle full screen
            addJavascriptInterface(
                object {
                    @android.webkit.JavascriptInterface
                    fun requestLandscape() {
                        onOrientationChange(ScreenOrientationMode.LANDSCAPE)
                    }

                    @android.webkit.JavascriptInterface
                    fun requestPortrait() {
                        onOrientationChange(ScreenOrientationMode.PORTRAIT)
                    }

                    @android.webkit.JavascriptInterface
                    fun requestSensor() {
                        onOrientationChange(ScreenOrientationMode.AUTO)
                    }
                },
                "WebCadeNative"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
                    val w = right - left
                    val h = bottom - top
                    if (w > 0 && h > 0) {
                        val density = v.context.resources.displayMetrics.density
                        val exclusionHeight = (56 * density).toInt()
                        v.systemGestureExclusionRects = listOf(
                            android.graphics.Rect(0, 0, w, exclusionHeight)
                        )
                    }
                }
            }
            if (currentUrl.isNotBlank() && lastLoadedUrl == null) {
                lastLoadedUrl = currentUrl
                loadUrl(currentUrl)
            }
            onWebViewReady(this)
        }
    }

    // React to user agent changes dynamically
    LaunchedEffect(userAgentMode) {
        if (userAgentMode.value.isNotEmpty()) {
            webView.settings.userAgentString = userAgentMode.value
        } else {
            webView.settings.userAgentString = null
        }
        if (webView.url != null) {
            webView.reload()
        }
    }

    // React to URL changes: only re-navigate when currentUrl changes to a new target
    // and never re-trigger on internal in-game SPA route or map changes
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank() && lastLoadedUrl != currentUrl) {
            lastLoadedUrl = currentUrl
            webView.loadUrl(currentUrl)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
