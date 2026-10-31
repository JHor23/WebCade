package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.gamepad.CursorPosition
import com.example.ui.theme.CyberCyan

@Composable
fun VirtualCursorReticle(
    cursorPosition: CursorPosition,
    modifier: Modifier = Modifier
) {
    if (!cursorPosition.isVisible) return

    val size = 24.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("virtual_mouse_reticle")
    ) {
        Canvas(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (cursorPosition.x - 12.dp.toPx()).toInt(),
                        (cursorPosition.y - 12.dp.toPx()).toInt()
                    )
                }
                .size(size)
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            // Outer ring
            drawCircle(
                color = CyberCyan,
                radius = this.size.minDimension / 2f - 2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            // Inner dot
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx()
            )
            // Crosshairs
            drawLine(
                color = CyberCyan,
                start = Offset(center.x, 0f),
                end = Offset(center.x, 6.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = CyberCyan,
                start = Offset(center.x, this.size.height - 6.dp.toPx()),
                end = Offset(center.x, this.size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = CyberCyan,
                start = Offset(0f, center.y),
                end = Offset(6.dp.toPx(), center.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = CyberCyan,
                start = Offset(this.size.width - 6.dp.toPx(), center.y),
                end = Offset(this.size.width, center.y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
