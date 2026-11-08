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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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

    val adBlockManager = remember { AdBlockManager(context) }
    val gameAssetCacheManager = remember { GameAssetCacheManager(context) }
    val isAdBlockerEnabled by adBlockManager.isAdBlockerEnabled.collectAsState()
    val blockedAdsCount by adBlockManager.blockedAdsCount.collectAsState()

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
        Toast.makeText(context, "Press Phone Back button anytime for URL bar & settings", Toast.LENGTH_SHORT).show()
    }

    /**
     * Safely handles back navigation.
     * If the user is viewing a custom fullscreen view, it dismisses the view.
     * If navigating back would land on about:blank, empty document, or root of history,
     * it gracefully exits back to the Home Portal instead of crashing to a blank white screen.
     */
    fun navigateBackOrHome() {
        if (customFullscreenView != null) {
            gamingWebClient.hideCustomView()
            return
        }
        val history = webViewInstance?.copyBackForwardList()
        if (history != null && history.currentIndex > 0) {
            val targetIndex = history.currentIndex - 1
            val targetUrl = history.getItemAtIndex(targetIndex)?.url ?: ""
            if (targetUrl.isBlank() || targetUrl == "about:blank" || targetUrl.startsWith("data:text/html")) {
                dismissUrlBar()
                isAtHomePortal = true
            } else {
                webViewInstance?.goBack()
            }
        } else {
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
                    },
                    onReset = { container ->
                        container.removeAllViews()
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

            // 4. SWIPEABLE URL BAR: Only composed when requested (guarantees 0% touch obstruction in game mode)
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
                    onClearBrowserData = { isClearDataDialogOpen = true },
                    orientationMode = orientationMode,
                    onToggleOrientationMode = { toggleOrientation() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
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
}
