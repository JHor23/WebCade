package com.example.ui

import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
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
import com.example.browser.GamingWebClient
import com.example.browser.GamingWebViewContainer
import com.example.browser.VirtualGamepadBridge
import com.example.gamepad.ControllerMode
import com.example.gamepad.GamepadManager
import com.example.model.GamingPresets
import com.example.model.UrlHistoryManager
import com.example.model.UserAgentMode
import com.example.ui.theme.CyberCyan

@Composable
fun GamingBrowserScreen(
    gamepadManager: GamepadManager,
    isMenuRequestedFromGamepad: Boolean,
    onMenuDismissed: () -> Unit,
    isUrlBarRequestedFromSwipe: Boolean = false,
    onUrlBarDismissed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val urlHistoryManager = remember { UrlHistoryManager(context) }

    var recentUrls by remember { mutableStateOf(urlHistoryManager.getRecentUrls()) }
    var currentUrl by remember {
        mutableStateOf(urlHistoryManager.getLastUrl() ?: GamingPresets.platforms[0].url)
    }
    var isAtHomePortal by remember { mutableStateOf(false) }
    var userAgentMode by remember { mutableStateOf(UserAgentMode.DESKTOP_CHROME) }
    var isUrlBarVisible by remember { mutableStateOf(false) }
    var isHubOpen by remember { mutableStateOf(false) }
    var isVirtualGamepadEnabled by remember { mutableStateOf(false) }
    var isPerformanceHudEnabled by remember { mutableStateOf(false) }
    var isMappingDialogOpen by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }

    val gamepadStatus by gamepadManager.gamepadStatus.collectAsState()
    val controllerMode by gamepadManager.controllerMode.collectAsState()
    val controllerMapping by gamepadManager.controllerMapping.collectAsState()
    val cursorPosition by gamepadManager.emulationEngine.cursorPosition.collectAsState()

    val gamingWebClient = remember {
        GamingWebClient(
            onCustomViewChanged = { view, _ ->
                customFullscreenView = view
            }
        )
    }

    val webState by gamingWebClient.webState.collectAsState()

    // Listen for top swipe-down from MainActivity (non-blocking)
    LaunchedEffect(isUrlBarRequestedFromSwipe) {
        if (isUrlBarRequestedFromSwipe) {
            isUrlBarVisible = true
        }
    }

    fun dismissUrlBar() {
        isUrlBarVisible = false
        onUrlBarDismissed()
    }

    fun launchUrl(url: String, ua: UserAgentMode) {
        val formatted = urlHistoryManager.formatUrl(url)
        urlHistoryManager.addUrl(formatted)
        recentUrls = urlHistoryManager.getRecentUrls()
        currentUrl = formatted
        userAgentMode = ua
        isAtHomePortal = false
        dismissUrlBar()
        webViewInstance?.loadUrl(formatted)
    }

    // Handle gamepad hotkey to toggle hub
    if (isMenuRequestedFromGamepad && !isHubOpen) {
        isHubOpen = true
        onMenuDismissed()
    }

    // Intercept back: dismiss URL Bar first, then Hub, then history
    BackHandler(enabled = true) {
        if (isUrlBarVisible) {
            dismissUrlBar()
        } else if (isHubOpen) {
            isHubOpen = false
        } else if (isAtHomePortal) {
            if (currentUrl.isNotEmpty()) {
                isAtHomePortal = false
            }
        } else if (webState.canGoBack) {
            webViewInstance?.goBack()
        } else {
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
            if (customFullscreenView != null) {
                AndroidView(
                    factory = { customFullscreenView!! },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                GamingWebViewContainer(
                    currentUrl = currentUrl,
                    userAgentMode = userAgentMode,
                    gamingWebClient = gamingWebClient,
                    onWebViewReady = { webView ->
                        webViewInstance = webView
                        gamepadManager.attachWebView(webView)
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

            // 3. Discreet Notch Indicator (Tiny 36dp pill; does NOT block clicks on the rest of the top)
            if (!isUrlBarVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 3.dp)
                        .width(36.dp)
                        .height(5.dp)
                        .alpha(0.3f)
                        .background(Color(0x99161E2E), RoundedCornerShape(3.dp))
                        .border(0.5.dp, Color(0x6600F2FE), RoundedCornerShape(3.dp))
                        .clickable { isUrlBarVisible = true }
                        .testTag("stealth_top_notch")
                )
            }

            // 4. SWIPEABLE URL BAR: Swiping UP on this URL bar causes it to disappear
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
                    if (webViewInstance?.canGoBack() == true) {
                        webViewInstance?.goBack()
                    }
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
                modifier = Modifier.align(Alignment.TopCenter)
            )

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
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        }
                    },
                    onGoForward = {
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    },
                    onClearCache = {
                        webViewInstance?.clearCache(true)
                        CookieManager.getInstance().removeAllCookies(null)
                        webViewInstance?.reload()
                    },
                    onRequestPointerCapture = {
                        webViewInstance?.requestPointerCapture()
                    },
                    onGoToHome = {
                        isAtHomePortal = true
                    }
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
}
