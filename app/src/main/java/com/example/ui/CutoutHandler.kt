package com.example.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.view.ViewCompat

/**
 * Encapsulates display cutout and front-facing camera obstacle detection.
 * Allows menus and navigation headers to adjust their padding away from
 * the camera punch-hole or notch, while leaving active gameplay in 100% fullscreen.
 */
data class MenuCutoutInsets(
    val hasCutout: Boolean,
    val top: Dp,
    val start: Dp,
    val end: Dp,
    val bottom: Dp
)

@Composable
fun rememberMenuCutoutInsets(): MenuCutoutInsets {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // 1. Compose native displayCutout insets
    val composeCutout = WindowInsets.displayCutout.asPaddingValues()
    val composeTop = composeCutout.calculateTopPadding()
    val composeStart = composeCutout.calculateStartPadding(layoutDirection)
    val composeEnd = composeCutout.calculateEndPadding(layoutDirection)
    val composeBottom = composeCutout.calculateBottomPadding()

    // 2. Direct WindowInsets inspection from ViewCompat (gets raw hardware cutout bounding rects)
    val rootInsets = ViewCompat.getRootWindowInsets(view)
    val displayCutout = rootInsets?.displayCutout

    var hardwareCutoutDetected = false
    var hardwareTopPx = 0
    var hardwareLeftPx = 0
    var hardwareRightPx = 0

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && displayCutout != null) {
        val safeTop = displayCutout.safeInsetTop
        val safeLeft = displayCutout.safeInsetLeft
        val safeRight = displayCutout.safeInsetRight
        val rects = displayCutout.boundingRects

        if (rects.isNotEmpty() || safeTop > 0 || safeLeft > 0 || safeRight > 0) {
            hardwareCutoutDetected = true
            // Find max depth of cutout at the top edge (within top 160px)
            val topRectDepth = rects.filter { it.top <= 20 }.maxOfOrNull { it.bottom } ?: 0
            hardwareTopPx = maxOf(safeTop, topRectDepth)
            hardwareLeftPx = safeLeft
            hardwareRightPx = safeRight
        }
    }

    // 3. Status bar height reference (on notched/hole-punch devices, status bar height matches or exceeds camera cutout)
    val statusBarResId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    val statusBarHeightPx = if (statusBarResId > 0) {
        try { context.resources.getDimensionPixelSize(statusBarResId) } catch (_: Throwable) { 0 }
    } else 0
    val statusBarHeightDp = with(density) { statusBarHeightPx.toDp() }

    val hardwareTopDp = with(density) { hardwareTopPx.toDp() }
    val hardwareLeftDp = with(density) { hardwareLeftPx.toDp() }
    val hardwareRightDp = with(density) { hardwareRightPx.toDp() }

    val hasFrontCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

    // Check if camera cutout is physically present or reported
    val hasCutout = hardwareCutoutDetected ||
            composeTop > 0.dp ||
            hardwareTopDp > 0.dp ||
            (hasFrontCamera && statusBarHeightDp >= 28.dp)

    // When cutout is detected, ensure the top inset clears the camera hole with a safe margin (at least 38dp)
    val effectiveTop = if (hasCutout) {
        max(max(composeTop, hardwareTopDp), max(statusBarHeightDp, 38.dp))
    } else {
        0.dp
    }

    val effectiveStart = if (hasCutout) {
        max(composeStart, hardwareLeftDp)
    } else {
        0.dp
    }

    val effectiveEnd = if (hasCutout) {
        max(composeEnd, hardwareRightDp)
    } else {
        0.dp
    }

    return MenuCutoutInsets(
        hasCutout = hasCutout,
        top = effectiveTop,
        start = effectiveStart,
        end = effectiveEnd,
        bottom = composeBottom
    )
}
