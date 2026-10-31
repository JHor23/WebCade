package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.UserAgentMode

@SuppressLint("SetJavaScriptEnabled")
fun setupGamingWebSettings(webView: WebView, userAgentMode: UserAgentMode) {
    // Enable hardware acceleration at the view layer
    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
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

    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = gamingWebClient.webViewClient
            webChromeClient = gamingWebClient.webChromeClient
            setupGamingWebSettings(this, userAgentMode)
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

    // React to URL changes
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank() && webView.url != currentUrl) {
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
        modifier = modifier.fillMaxSize()
    )
}
