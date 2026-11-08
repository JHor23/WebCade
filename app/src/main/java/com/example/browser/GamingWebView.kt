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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ScreenOrientationMode
import com.example.model.UserAgentMode
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
fun setupGamingWebSettings(
    webView: WebView,
    userAgentMode: UserAgentMode
) {
    try {
        // Direct GPU composition: LAYER_TYPE_NONE allows Chromium's native compositor to render WebGL directly
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
            try {
                settings.safeBrowsingEnabled = false
            } catch (_: Throwable) {}
        }

        // Enable mixed content for cloud streaming servers and game websockets
        try {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        } catch (_: Throwable) {}

        // Performance and responsiveness optimizations
        try {
            settings.offscreenPreRaster = true
        } catch (_: Throwable) {}
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        settings.javaScriptCanOpenWindowsAutomatically = true

        // Apply User Agent
        if (userAgentMode.value.isNotEmpty()) {
            settings.userAgentString = userAgentMode.value
        } else {
            settings.userAgentString = null // Reset to default
        }

        // Enable third-party cookies for cross-origin cloud gaming authentication
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        } catch (_: Throwable) {}
    } catch (_: Throwable) {}
}

@Composable
fun GamingWebViewContainer(
    currentUrl: String,
    userAgentMode: UserAgentMode,
    gamingWebClient: GamingWebClient,
    onWebViewReady: (WebView) -> Unit,
    onOrientationChange: (ScreenOrientationMode) -> Unit = {},
    onBackToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }
    var webViewInitError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    // Safely create and retain WebView instance, guarding against Android System WebView updates
    val webView = remember(retryTrigger) {
        try {
            webViewInitError = null
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                webViewClient = gamingWebClient.webViewClient
                webChromeClient = gamingWebClient.webChromeClient
                setupGamingWebSettings(this, userAgentMode)

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
        } catch (t: Throwable) {
            val rawMsg = t.localizedMessage ?: t.message ?: ""
            webViewInitError = if (rawMsg.contains("updating", ignoreCase = true) ||
                rawMsg.contains("package", ignoreCase = true)
            ) {
                "Android System WebView is currently being updated by Google Play. Once the update completes in 1-2 minutes, tap Retry."
            } else {
                "Android System WebView encountered an unexpected initialization error: ${t.javaClass.simpleName}"
            }
            null
        }
    }

    if (webView == null || webViewInitError != null) {
        // Fallback UI to prevent application crash during system WebView updates
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 480.dp)
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "WebView Update Notice",
                    tint = CyberCyan,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "System WebView Updating",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = webViewInitError
                        ?: "Android System WebView is currently being updated in the background. Please wait a moment and tap Retry.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { retryTrigger++ },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("retry_webview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(
                        text = "  Retry Loading",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBackToHome,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("back_to_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Return Home",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "  Return to Home Portal",
                        color = TextSecondary
                    )
                }
            }
        }
        return
    }

    // React to user agent changes dynamically
    LaunchedEffect(userAgentMode, webView) {
        try {
            if (userAgentMode.value.isNotEmpty()) {
                webView.settings.userAgentString = userAgentMode.value
            } else {
                webView.settings.userAgentString = null
            }
            if (webView.url != null) {
                webView.reload()
            }
        } catch (_: Throwable) {}
    }

    // React to URL changes: only re-navigate when currentUrl changes to a new target
    // and never re-trigger on internal in-game SPA route or map changes
    LaunchedEffect(currentUrl, webView) {
        try {
            if (currentUrl.isNotBlank() && lastLoadedUrl != currentUrl) {
                lastLoadedUrl = currentUrl
                webView.loadUrl(currentUrl)
            }
        } catch (_: Throwable) {}
    }

    DisposableEffect(webView) {
        onDispose {
            try {
                webView.stopLoading()
                webView.destroy()
            } catch (_: Throwable) {}
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
