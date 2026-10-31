package com.example

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.gamepad.GamepadManager
import com.example.ui.GamingBrowserScreen
import com.example.ui.theme.WebCadeTheme

class MainActivity : ComponentActivity() {

    private lateinit var gamepadManager: GamepadManager
    private var isMenuRequestedFromGamepad by mutableStateOf(false)
    private var isUrlBarRequestedFromSwipe by mutableStateOf(false)

    private var touchDownY = -1f
    private var isPotentialTopSwipe = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configure Hardware Acceleration and Keep Screen On
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. High Refresh Rate optimization (90Hz / 120Hz / 144Hz)
        setupHighRefreshRate()

        // 3. Cutout mode short edges (use 100% of the display including camera notch area)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }

        // 4. Immersive Sticky Fullscreen
        enableImmersiveStickyFullscreen()

        // 5. Initialize Gamepad Input Manager
        gamepadManager = GamepadManager(this)

        setContent {
            WebCadeTheme {
                GamingBrowserScreen(
                    gamepadManager = gamepadManager,
                    isMenuRequestedFromGamepad = isMenuRequestedFromGamepad,
                    onMenuDismissed = { isMenuRequestedFromGamepad = false },
                    isUrlBarRequestedFromSwipe = isUrlBarRequestedFromSwipe,
                    onUrlBarDismissed = { isUrlBarRequestedFromSwipe = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveStickyFullscreen()
        }
    }

    /**
     * Non-blocking gesture listener: observes downward swipes from the top edge
     * to reveal the URL bar WITHOUT consuming taps or blocking clicks on the web page.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // If finger touched within the top edge region (top 140px)
                if (ev.y < 140f) {
                    touchDownY = ev.y
                    isPotentialTopSwipe = true
                } else {
                    isPotentialTopSwipe = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPotentialTopSwipe && touchDownY >= 0f) {
                    val deltaY = ev.y - touchDownY
                    // Swiping down from top edge brings back URL bar
                    if (deltaY > 60f) {
                        isPotentialTopSwipe = false
                        touchDownY = -1f
                        isUrlBarRequestedFromSwipe = true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchDownY = -1f
                isPotentialTopSwipe = false
            }
        }
        // Always pass event down so web clicks and in-game controls work 100% unimpeded
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Check for controller hotkey (Select + Start) or input logging
        val consumed = gamepadManager.handleKeyEvent(event) {
            isMenuRequestedFromGamepad = true
        }
        if (consumed) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        gamepadManager.handleMotionEvent(event)
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        gamepadManager.cleanup()
    }

    private fun enableImmersiveStickyFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @Suppress("DEPRECATION")
    private fun setupHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val displayObj = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                windowManager.defaultDisplay
            }
            val modes = displayObj?.supportedModes ?: emptyArray()
            val highestMode = modes.maxByOrNull { it.refreshRate }
            if (highestMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = highestMode.modeId
                window.attributes = params
            }
        }
    }
}
