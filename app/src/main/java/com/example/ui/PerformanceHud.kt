package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamepad.ControllerMode
import com.example.gamepad.GamepadStatus
import com.example.model.UserAgentMode
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple

@Composable
fun PerformanceHud(
    gamepadStatus: GamepadStatus,
    userAgentMode: UserAgentMode,
    controllerMode: ControllerMode = ControllerMode.DEDICATED_GAMEPAD,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("performance_hud")
            .background(Color(0xBB080B10), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x4400F2FE), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hardware Acceleration status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NeonGreen, CircleShape)
                )
                Text(
                    text = "HW ON",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Controller & Mode
            Text(
                text = if (gamepadStatus.isConnected) {
                    if (controllerMode == ControllerMode.DEDICATED_GAMEPAD) "🎮 Pad: Direct" else "⌨️🖱️ Pad: KBM Emulation"
                } else {
                    "🕹️ Touch Virtual Pad"
                },
                color = if (controllerMode == ControllerMode.KEYBOARD_MOUSE_EMULATION) NeonPurple else Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // Last Pressed or Stick
            if (gamepadStatus.lastButtonPressed.isNotEmpty()) {
                Text(
                    text = "[${gamepadStatus.lastButtonPressed}]",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
