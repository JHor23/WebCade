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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.UserAgentMode

@SuppressLint("SetJavaScriptEnabled")
fun setupGamingWebSettings(webView: WebView, userAgentMode: UserAgentMode) {
    // Enable hardware acceleration at the view layer
    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    webView.setBackgroundColor(android.graphics.Color.BLACK)
    webView.isFocusable = true
    webView.isFocusableInTouchMode = true
    webView.overScrollMode = View.OVER_SCROLL_NEVER

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

    // Enable mixed content for cloud streaming servers that negotiate hybrid assets
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

    // Performance and responsiveness optimizations
    settings.offscreenPreRaster = true
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            setupGamingWebSettings(this, userAgentMode)
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
            if (currentUrl.isNotBlank()) {
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

    // React to URL changes (ensures page loads even if recreated or after back navigation)
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank() && (webView.url == null || webView.url != currentUrl)) {
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
