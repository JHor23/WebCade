package com.example.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast
import com.example.browser.AdBlockManager
import com.example.browser.BrowserDataManager
import com.example.browser.EncryptedTunnelManager
import com.example.browser.GameAssetCacheManager
import com.example.browser.GamingWebClient
import com.example.browser.GamingWebViewContainer
import com.example.browser.VirtualGamepadBridge
import com.example.gamepad.ControllerMode
import com.example.gamepad.GamepadManager
import com.example.model.GamingPresets
import com.example.model.ScreenOrientationMode
import com.example.model.UrlHistoryManager
import com.example.model.UserAgentMode
import com.example.ui.theme.CyberCyan

@Composable
fun GamingBrowserScreen(
    gamepadManager: GamepadManager,
    isMenuRequestedFromGamepad: Boolean,
    onMenuDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val urlHistoryManager = remember { UrlHistoryManager(context) }

    var recentUrls by remember { mutableStateOf(urlHistoryManager.getRecentUrls()) }
    var currentUrl by rememberSaveable {
        mutableStateOf(urlHistoryManager.getLastUrl() ?: GamingPresets.platforms[0].url)
    }
    var isAtHomePortal by rememberSaveable { mutableStateOf(true) }
    var userAgentMode by remember { mutableStateOf(UserAgentMode.DESKTOP_CHROME) }
    var orientationMode by rememberSaveable { mutableStateOf(ScreenOrientationMode.AUTO) }
    var isUrlBarVisible by remember { mutableStateOf(false) }
    var isHubOpen by remember { mutableStateOf(false) }
    var isVirtualGamepadEnabled by remember { mutableStateOf(false) }
    var isPerformanceHudEnabled by remember { mutableStateOf(false) }
    var isMappingDialogOpen by remember { mutableStateOf(false) }
    var isClearDataDialogOpen by remember { mutableStateOf(false) }
    var showNavigationHint by remember { mutableStateOf(false) }

    LaunchedEffect(showNavigationHint) {
        if (showNavigationHint) {
            delay(5000L) // 5 seconds (extra 3s for easy, comfortable reading)
            showNavigationHint = false
        }
    }

    val adBlockManager = remember { AdBlockManager(context) }
    val gameAssetCacheManager = remember { GameAssetCacheManager(context) }
    val isAdBlockerEnabled by adBlockManager.isAdBlockerEnabled.collectAsState()
    val blockedAdsCount by adBlockManager.blockedAdsCount.collectAsState()

    val encryptedTunnelManager = remember { EncryptedTunnelManager(context) }
    val isTunnelEnabled by encryptedTunnelManager.isTunnelEnabled.collectAsState()
    var isTunnelConfigDialogOpen by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }

    fun setOrientation(newMode: ScreenOrientationMode) {
        orientationMode = newMode
        activity?.requestedOrientation = when (newMode) {
            ScreenOrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    fun toggleOrientation() {
        val nextMode = when (orientationMode) {
            ScreenOrientationMode.AUTO -> ScreenOrientationMode.LANDSCAPE
            ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.PORTRAIT
            ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.AUTO
        }
        setOrientation(nextMode)
        val msg = when (nextMode) {
            ScreenOrientationMode.AUTO -> "Screen Orientation: Auto-Rotate (Sensor)"
            ScreenOrientationMode.LANDSCAPE -> "Screen Orientation: Locked to Landscape"
            ScreenOrientationMode.PORTRAIT -> "Screen Orientation: Locked to Portrait (Portrait Gaming)"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    val gamepadStatus by gamepadManager.gamepadStatus.collectAsState()
    val controllerMode by gamepadManager.controllerMode.collectAsState()
    val controllerMapping by gamepadManager.controllerMapping.collectAsState()
    val cursorPosition by gamepadManager.emulationEngine.cursorPosition.collectAsState()

    val gamingWebClient = remember {
        GamingWebClient(
            adBlockManager = adBlockManager,
            gameAssetCacheManager = gameAssetCacheManager,
            onCustomViewChanged = { view, _ ->
                customFullscreenView = view
                if (view != null) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    activity?.requestedOrientation = when (orientationMode) {
                        ScreenOrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                }
            }
        )
    }

    val webState by gamingWebClient.webState.collectAsState()

    fun toggleAdBlocker() {
        val enabled = adBlockManager.toggleAdBlocker()
        val msg = if (enabled) {
            "Ad-Blocker DNS Filtering: ON (Blocking ads & trackers)"
        } else {
            "Ad-Blocker DNS Filtering: OFF"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        if (!isAtHomePortal) {
            webViewInstance?.reload()
        }
    }

    fun toggleTunnel() {
        val enabled = encryptedTunnelManager.toggleTunnel(webViewInstance)
        val msg = if (enabled) {
            "Anti-DPI Encrypted Tunnel: ON (ISP Throttling & DPI Bypass Active)"
        } else {
            "Anti-DPI Encrypted Tunnel: OFF (Direct Connection)"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun handleClearBrowserData(clearHistory: Boolean) {
        BrowserDataManager.clearAllBrowserData(
            context = context,
            webView = webViewInstance,
            clearHistory = clearHistory,
            onComplete = {
                if (clearHistory) {
                    recentUrls = urlHistoryManager.getRecentUrls()
                }
                Toast.makeText(
                    context,
                    "All cache, cookies & browser data cleared",
                    Toast.LENGTH_SHORT
                ).show()
                if (!isAtHomePortal) {
                    webViewInstance?.reload()
                }
            }
        )
    }

    fun dismissUrlBar() {
        isUrlBarVisible = false
    }

    fun launchUrl(url: String, ua: UserAgentMode) {
        val formatted = urlHistoryManager.formatUrl(url)
        urlHistoryManager.addUrl(formatted)
        recentUrls = urlHistoryManager.getRecentUrls()
        currentUrl = formatted
        userAgentMode = ua
        adBlockManager.setCurrentGameHost(formatted)
        isAtHomePortal = false
        dismissUrlBar()
        if (webViewInstance != null && webViewInstance?.url != formatted) {
            webViewInstance?.loadUrl(formatted)
        }
        showNavigationHint = true
    }

    /**
     * Safely handles back navigation.
     * If the user is viewing a custom fullscreen view, it dismisses the view.
     * If navigating back would land on about:blank, empty document, or root of history,
     * it gracefully exits back to the Home Portal instead of crashing to a blank white screen.
     */
    fun navigateBackOrHome() {
        try {
            if (customFullscreenView != null) {
                gamingWebClient.hideCustomView()
                return
            }
            val history = try { webViewInstance?.copyBackForwardList() } catch (_: Throwable) { null }
            if (history != null && history.currentIndex > 0) {
                val targetIndex = history.currentIndex - 1
                val targetUrl = history.getItemAtIndex(targetIndex)?.url ?: ""
                if (targetUrl.isBlank() || targetUrl == "about:blank" || targetUrl.startsWith("data:text/html")) {
                    dismissUrlBar()
                    isAtHomePortal = true
                } else {
                    try {
                        webViewInstance?.goBack()
                    } catch (_: Throwable) {
                        dismissUrlBar()
                        isAtHomePortal = true
                    }
                }
            } else {
                dismissUrlBar()
                isAtHomePortal = true
            }
        } catch (_: Throwable) {
            dismissUrlBar()
            isAtHomePortal = true
        }
    }

    // Handle gamepad hotkey to toggle hub
    if (isMenuRequestedFromGamepad && !isHubOpen) {
        isHubOpen = true
        onMenuDismissed()
    }

    // Intercept back button:
    // When playing in fullscreen game mode: pressing phone's back button OPENS the URL bar & settings!
    // If the URL bar or any dialog is already open: pressing back DISMISSES it to return to clean full-screen game.
    BackHandler(enabled = isClearDataDialogOpen || isUrlBarVisible || isHubOpen || !isAtHomePortal) {
        if (isClearDataDialogOpen) {
            isClearDataDialogOpen = false
        } else if (isUrlBarVisible) {
            dismissUrlBar()
        } else if (isHubOpen) {
            isHubOpen = false
        } else {
            // User pressed Phone's back button during gameplay -> open URL bar & settings screen
            isUrlBarVisible = true
        }
    }

    if (isAtHomePortal) {
        // Full Portal Home with dedicated URL input and presets
        GamingPortalHome(
            recentUrls = recentUrls,
            gamepadStatus = gamepadStatus,
            selectedUserAgent = userAgentMode,
            onUserAgentChanged = { userAgentMode = it },
            onLaunchUrl = { url, ua ->
                launchUrl(url, ua)
            },
            controllerMode = controllerMode,
            onOpenControllerMapping = { isMappingDialogOpen = true },
            isAdBlockerEnabled = isAdBlockerEnabled,
            blockedAdsCount = blockedAdsCount,
            onToggleAdBlocker = { toggleAdBlocker() },
            isTunnelEnabled = isTunnelEnabled,
            onToggleTunnel = { toggleTunnel() },
            onOpenTunnelConfig = { isTunnelConfigDialogOpen = true },
            onClearBrowserData = { isClearDataDialogOpen = true },
            modifier = modifier
        )
    } else {
        // Fullscreen Gaming Mode (Zero GUI, completely unblocked touch)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .onGloballyPositioned { coordinates ->
                    gamepadManager.updateScreenDimensions(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                }
        ) {
            // 1. Primary Fullscreen Gaming Canvas (100% Zero GUI, direct touch handling)
            GamingWebViewContainer(
                currentUrl = currentUrl,
                userAgentMode = userAgentMode,
                gamingWebClient = gamingWebClient,
                onWebViewReady = { webView ->
                    webViewInstance = webView
                    gamepadManager.attachWebView(webView)
                },
                onOrientationChange = { mode ->
                    activity?.runOnUiThread {
                        setOrientation(mode)
                    }
                },
                onBackToHome = {
                    isAtHomePortal = true
                },
                modifier = Modifier.fillMaxSize()
            )

            if (customFullscreenView != null) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { container ->
                        try {
                            val targetView = customFullscreenView
                            if (targetView != null) {
                                if (targetView.parent !== container) {
                                    (targetView.parent as? ViewGroup)?.removeView(targetView)
                                    container.removeAllViews()
                                    container.addView(
                                        targetView,
                                        ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                }
                            } else {
                                container.removeAllViews()
                            }
                        } catch (_: Throwable) {}
                    },
                    onReset = { container ->
                        try {
                            container.removeAllViews()
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Loading Indicator (Minimalist 2.5dp line at top, non-interactive)
            AnimatedVisibility(
                visible = webState.isLoading && webState.progress < 100,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    progress = { webState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = CyberCyan,
                    trackColor = Color.Transparent
                )
            }

            // 4. FULL-SCREEN QUICK SETTINGS & URL BAR: Only composed when requested via Phone Back button
            if (isUrlBarVisible) {
                SwipeableGamingUrlBar(
                    isVisible = isUrlBarVisible,
                    currentUrl = currentUrl,
                    recentUrls = recentUrls,
                    userAgentMode = userAgentMode,
                    onDismissUrlBar = { dismissUrlBar() },
                    onNavigateToUrl = { url, ua ->
                        launchUrl(url, ua)
                    },
                    onUserAgentChanged = { userAgentMode = it },
                    onReload = { webViewInstance?.reload() },
                    onGoBack = {
                        navigateBackOrHome()
                    },
                    onGoForward = {
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    },
                    onOpenFullHub = {
                        dismissUrlBar()
                        isHubOpen = true
                    },
                    onGoToHome = {
                        dismissUrlBar()
                        isAtHomePortal = true
                    },
                    isAdBlockerEnabled = isAdBlockerEnabled,
                    onToggleAdBlocker = { toggleAdBlocker() },
                    isTunnelEnabled = isTunnelEnabled,
                    onToggleTunnel = { toggleTunnel() },
                    onOpenTunnelConfig = { isTunnelConfigDialogOpen = true },
                    onClearBrowserData = { isClearDataDialogOpen = true },
                    orientationMode = orientationMode,
                    onToggleOrientationMode = { toggleOrientation() },
                    onSetOrientationMode = { setOrientation(it) },
                    pageTitle = webState.title,
                    canGoBack = webState.canGoBack,
                    canGoForward = webState.canGoForward,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 5. Centered Navigation Hint Notification (stays for 5s, centered on screen, centered text in bubble)
            AnimatedVisibility(
                visible = showNavigationHint && !isUrlBarVisible && !isHubOpen,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .widthIn(max = 420.dp)
                        .clickable { showNavigationHint = false }
                        .testTag("nav_hint_bubble"),
                    shape = RoundedCornerShape(22.dp),
                    color = DarkSurface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.85f)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(DarkSurfaceElevated, CircleShape)
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Press Back at any time to open Game Controls & Settings",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tap to dismiss",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 5. Performance & Telemetry HUD (Optional)
            if (isPerformanceHudEnabled) {
                PerformanceHud(
                    gamepadStatus = gamepadStatus,
                    userAgentMode = userAgentMode,
                    controllerMode = controllerMode,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = if (isUrlBarVisible) 130.dp else 24.dp, end = 16.dp)
                )
            }

            // 6. Virtual On-Screen Gamepad Overlay (When enabled)
            if (isVirtualGamepadEnabled) {
                VirtualGamepadOverlay(
                    onGamepadUpdate = { lx, ly, rx, ry, btnMask, lt, rt ->
                        VirtualGamepadBridge.updateGamepadState(
                            webViewInstance,
                            lx, ly, rx, ry, btnMask, lt, rt
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 7. Virtual Mouse Cursor Reticle (When in Keyboard & Mouse Emulation mode)
            if (controllerMode == ControllerMode.KEYBOARD_MOUSE_EMULATION && controllerMapping.showVirtualCursor) {
                VirtualCursorReticle(cursorPosition = cursorPosition)
            }

            // 8. Full Game Hub Dialog (Settings, controller diagnostic, user agent)
            if (isHubOpen) {
                GameHubDialog(
                    currentUrl = currentUrl,
                    recentUrls = recentUrls,
                    userAgentMode = userAgentMode,
                    isVirtualGamepadEnabled = isVirtualGamepadEnabled,
                    isPerformanceHudEnabled = isPerformanceHudEnabled,
                    gamepadStatus = gamepadStatus,
                    controllerMode = controllerMode,
                    onOpenControllerMapping = {
                        isHubOpen = false
                        isMappingDialogOpen = true
                    },
                    onDismiss = { isHubOpen = false },
                    onLoadUrl = { url, recommendedUa ->
                        launchUrl(url, recommendedUa)
                    },
                    onUserAgentChanged = { newUa ->
                        userAgentMode = newUa
                    },
                    onToggleVirtualGamepad = { enabled ->
                        isVirtualGamepadEnabled = enabled
                    },
                    onTogglePerformanceHud = { enabled ->
                        isPerformanceHudEnabled = enabled
                    },
                    onReload = {
                        webViewInstance?.reload()
                    },
                    onGoBack = {
                        navigateBackOrHome()
                    },
                    onGoForward = {
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    },
                    onClearCache = {
                        isClearDataDialogOpen = true
                    },
                    onRequestPointerCapture = {
                        webViewInstance?.requestPointerCapture()
                    },
                    onGoToHome = {
                        isAtHomePortal = true
                    },
                    isAdBlockerEnabled = isAdBlockerEnabled,
                    blockedAdsCount = blockedAdsCount,
                    onToggleAdBlocker = { toggleAdBlocker() },
                    orientationMode = orientationMode,
                    onOrientationModeChanged = { setOrientation(it) }
                )
            }
        }
    }

    // 9. Controller Mapping & Emulation Configuration Dialog
    if (isMappingDialogOpen) {
        GamepadMappingDialog(
            currentMode = controllerMode,
            currentMapping = controllerMapping,
            gamepadStatus = gamepadStatus,
            onDismiss = { isMappingDialogOpen = false },
            onModeChanged = { newMode ->
                gamepadManager.setControllerMode(newMode)
            },
            onMappingChanged = { newMapping ->
                gamepadManager.setMapping(newMapping)
            }
        )
    }

    // 10. Clear Cache, Cookies & Browser Data Dialog
    if (isClearDataDialogOpen) {
        ClearBrowserDataDialog(
            onDismiss = { isClearDataDialogOpen = false },
            onConfirmClear = { clearHistory ->
                handleClearBrowserData(clearHistory)
            }
        )
    }

    // 11. Anti-DPI Encrypted Tunnel Configuration Dialog
    if (isTunnelConfigDialogOpen) {
        TunnelConfigDialog(
            tunnelManager = encryptedTunnelManager,
            onDismiss = { isTunnelConfigDialogOpen = false }
        )
    }
}
