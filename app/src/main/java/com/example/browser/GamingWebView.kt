package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
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

private val CHROME_VERSION_REGEX = Regex("""Chrome/(\d+)(?:\.(\d+\.\d+\.\d+))?""")
private const val DESKTOP_WIN_UA_TEMPLATE =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s.0.0.0 Safari/537.36"
private const val CHROME_OS_UA_TEMPLATE =
    "Mozilla/5.0 (X11; CrOS x86_64 15886.44.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s.0.0.0 Safari/537.36"
private const val FALLBACK_MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

internal fun extractChromeVersions(defaultUa: String): Pair<String, String> {
    val match = CHROME_VERSION_REGEX.find(defaultUa)
    val major = match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "131"
    val suffix = match?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "0.0.0"
    return major to "$major.$suffix"
}

internal fun sanitizeMobileUserAgent(defaultUa: String): String {
    if (defaultUa.isBlank()) return FALLBACK_MOBILE_UA
    return defaultUa
        .replace("; wv)", ")")
        .replace("; wv;", ";")
        .replace(Regex("""\bVersion/\S+\s*"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
}

internal fun resolveUserAgentString(defaultUa: String, mode: UserAgentMode): String {
    val (majorVersion, _) = extractChromeVersions(defaultUa)
    return when (mode) {
        UserAgentMode.DESKTOP_CHROME -> String.format(java.util.Locale.US, DESKTOP_WIN_UA_TEMPLATE, majorVersion)
        UserAgentMode.CHROME_OS -> String.format(java.util.Locale.US, CHROME_OS_UA_TEMPLATE, majorVersion)
        UserAgentMode.MOBILE -> sanitizeMobileUserAgent(defaultUa)
    }
}

internal fun isGoogleAuthHost(host: String?): Boolean {
    val normalized = host?.lowercase(java.util.Locale.ROOT)?.trimEnd('.') ?: return false
    return normalized == "accounts.google.com" ||
        normalized.startsWith("accounts.google.") ||
        normalized.endsWith(".accounts.google.com") ||
        normalized == "accounts.youtube.com" ||
        normalized == "myaccount.google.com" ||
        normalized == "consent.google.com" ||
        normalized.startsWith("consent.google.") ||
        normalized == "consent.youtube.com" ||
        normalized == "gds.google.com" ||
        normalized == "signin.google.com"
}

internal fun isGoogleAuthUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return try {
        isGoogleAuthHost(android.net.Uri.parse(url).host)
    } catch (_: Throwable) {
        false
    }
}

internal fun resolveEffectiveUserAgentForUrl(
    url: String?,
    desiredMode: UserAgentMode
): UserAgentMode {
    return if (isGoogleAuthUrl(url)) UserAgentMode.MOBILE else desiredMode
}

fun applyBrowserIdentityAndAuthSettings(
    context: Context,
    settings: WebSettings,
    userAgentMode: UserAgentMode
) {
    val defaultUa = try {
        WebSettings.getDefaultUserAgent(context)
    } catch (_: Throwable) {
        ""
    }
    val (majorVersion, fullVersion) = extractChromeVersions(defaultUa)
    val resolvedUa = resolveUserAgentString(defaultUa, userAgentMode)

    try {
        settings.userAgentString = resolvedUa
    } catch (_: Throwable) {
        settings.userAgentString = if (userAgentMode.value.isNotEmpty()) userAgentMode.value else null
    }

    // Suppress X-Requested-With header so Google Sign-In does not reject WebCade as an embedded WebView
    try {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
        }
    } catch (_: Throwable) {}

    // Align Client Hints (Sec-CH-UA / navigator.userAgentData) with Google Chrome instead of "Android WebView"
    try {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            val brands = listOf(
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand("Chromium")
                    .setMajorVersion(majorVersion)
                    .setFullVersion(fullVersion)
                    .build(),
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand("Google Chrome")
                    .setMajorVersion(majorVersion)
                    .setFullVersion(fullVersion)
                    .build(),
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand("Not_A Brand")
                    .setMajorVersion("24")
                    .setFullVersion("24.0.0.0")
                    .build()
            )
            val metadataBuilder = UserAgentMetadata.Builder()
                .setBrandVersionList(brands)
                .setFullVersion(fullVersion)
                .setWow64(false)

            when (userAgentMode) {
                UserAgentMode.DESKTOP_CHROME -> {
                    metadataBuilder
                        .setPlatform("Windows")
                        .setPlatformVersion("10.0.0")
                        .setArchitecture("x86")
                        .setModel("")
                        .setMobile(false)
                        .setBitness(64)
                }
                UserAgentMode.CHROME_OS -> {
                    metadataBuilder
                        .setPlatform("Chrome OS")
                        .setPlatformVersion("15886.44.0")
                        .setArchitecture("x86")
                        .setModel("")
                        .setMobile(false)
                        .setBitness(64)
                }
                UserAgentMode.MOBILE -> {
                    val isArm64 = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
                    metadataBuilder
                        .setPlatform("Android")
                        .setPlatformVersion(Build.VERSION.RELEASE ?: "14")
                        .setArchitecture(if (isArm64) "arm" else "")
                        .setModel(Build.MODEL ?: "")
                        .setMobile(true)
                        .setBitness(if (isArm64) 64 else UserAgentMetadata.BITNESS_DEFAULT)
                }
            }
            WebSettingsCompat.setUserAgentMetadata(settings, metadataBuilder.build())
        }
    } catch (_: Throwable) {}

    // Enable WebAuthn / Passkey support for modern Google Sign-In verification flows
    try {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
            WebSettingsCompat.setWebAuthenticationSupport(
                settings,
                WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER
            )
        }
    } catch (_: Throwable) {}
}

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

        // Apply sanitized User-Agent, Client Hints, X-Requested-With suppression, and WebAuthn support
        applyBrowserIdentityAndAuthSettings(webView.context, settings, userAgentMode)

        // Enable third-party cookies for cross-origin cloud gaming and Google authentication
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
                val initialEffectiveUa = resolveEffectiveUserAgentForUrl(currentUrl, userAgentMode)
                gamingWebClient.syncUserAgentState(userAgentMode, initialEffectiveUa)
                setupGamingWebSettings(this, initialEffectiveUa)

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

    var appliedUserAgentMode by remember(webView) { mutableStateOf(userAgentMode) }

    // React to user agent changes dynamically without double-reloading on initial launch
    LaunchedEffect(userAgentMode, webView) {
        if (userAgentMode != appliedUserAgentMode) {
            appliedUserAgentMode = userAgentMode
            try {
                val effectiveMode = resolveEffectiveUserAgentForUrl(webView.url, userAgentMode)
                gamingWebClient.syncUserAgentState(userAgentMode, effectiveMode)
                applyBrowserIdentityAndAuthSettings(context, webView.settings, effectiveMode)
                if (webView.url != null) {
                    webView.reload()
                }
            } catch (_: Throwable) {}
        }
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
