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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.gamepad.GamepadManager
import com.example.ui.GamingBrowserScreen
import com.example.ui.theme.WebCadeTheme

class MainActivity : ComponentActivity() {

    private lateinit var gamepadManager: GamepadManager
    private var isMenuRequestedFromGamepad by mutableStateOf(false)

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

        // 3. Cutout mode ALWAYS (use 100% of the display including camera notch area on all edges)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            window.attributes = params
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }

        // 4. Immersive Sticky Fullscreen
        enableImmersiveStickyFullscreen()

        // 5. Apply System Gesture Exclusions along the top edge so game controls receive 100% of touches
        applySystemGestureExclusions()

        // 6. Initialize Gamepad Input Manager
        gamepadManager = GamepadManager(this)

        setContent {
            WebCadeTheme {
                GamingBrowserScreen(
                    gamepadManager = gamepadManager,
                    isMenuRequestedFromGamepad = isMenuRequestedFromGamepad,
                    onMenuDismissed = { isMenuRequestedFromGamepad = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveStickyFullscreen()
            applySystemGestureExclusions()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        enableImmersiveStickyFullscreen()
        applySystemGestureExclusions()
    }


    /**
     * Informs the Android OS that the top edge contains active game controls and buttons.
     * Prevents SystemUI on Google Pixel from capturing, delaying, or filtering touches as
     * notification shade pull-downs or accidental palm contacts.
     */
    private fun applySystemGestureExclusions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.post {
                val decor = window.decorView
                val width = decor.width
                val height = decor.height
                if (width > 0 && height > 0) {
                    val density = resources.displayMetrics.density
                    // 56dp exclusion band along the top edge
                    val exclusionHeight = (56 * density).toInt()
                    val rects = listOf(
                        android.graphics.Rect(0, 0, width, exclusionHeight)
                    )
                    ViewCompat.setSystemGestureExclusionRects(decor, rects)
                }
            }
        }
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
