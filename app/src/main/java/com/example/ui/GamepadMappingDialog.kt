package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gamepad.ControllerMapping
import com.example.gamepad.ControllerMode
import com.example.gamepad.EmulatedKey
import com.example.gamepad.GamepadStatus
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GamepadMappingDialog(
    currentMode: ControllerMode,
    currentMapping: ControllerMapping,
    gamepadStatus: GamepadStatus,
    onDismiss: () -> Unit,
    onModeChanged: (ControllerMode) -> Unit,
    onMappingChanged: (ControllerMapping) -> Unit
) {
    var mode by remember { mutableStateOf(currentMode) }
    var mapping by remember { mutableStateOf(currentMapping) }
    var sensitivity by remember { mutableFloatStateOf(currentMapping.mouseSensitivity) }
    var deadzone by remember { mutableFloatStateOf(currentMapping.deadzone) }
    var showCursor by remember { mutableStateOf(currentMapping.showVirtualCursor) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE080B10))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .heightIn(max = 720.dp)
                    .testTag("gamepad_mapping_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CyberCyan.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, CyberCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Controller Config",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Controller Mode & Mapping",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (gamepadStatus.isConnected) "Connected: ${gamepadStatus.deviceName}" else "No physical controller connected",
                                    color = if (gamepadStatus.isConnected) NeonGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkSurfaceElevated, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // MODE SELECTION TABS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ControllerMode.values().forEach { m ->
                            val isSelected = mode == m
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        mode = m
                                        onModeChanged(m)
                                    }
                                    .testTag("mode_tab_${m.name.lowercase()}"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberCyan else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (m == ControllerMode.DEDICATED_GAMEPAD) Icons.Default.SportsEsports else Icons.Default.Keyboard,
                                        contentDescription = m.label,
                                        tint = if (isSelected) DarkBackground else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (m == ControllerMode.DEDICATED_GAMEPAD) "Dedicated Gamepad" else "Key & Mouse Emulation",
                                        color = if (isSelected) DarkBackground else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (mode == ControllerMode.DEDICATED_GAMEPAD) {
                            // Dedicated Gamepad Mode Info Card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "NATIVE W3C GAMEPAD MODE",
                                            color = CyberCyan,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Controller inputs are passed directly to HTML5 and WebRTC Gamepad APIs. Recommended for:",
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "• Xbox Cloud Gaming (xCloud)\n• NVIDIA GeForce NOW\n• Amazon Luna\n• Boosteroid\n• Gamepad Tester",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Keyboard & Mouse Emulation Mode
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "KEYBOARD & MOUSE EMULATION ACTIVE",
                                            color = NeonPurple,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "• Left Stick: Moves WASD\n• Right Stick: Aims Mouse Cursor\n• Buttons emulate configured keystrokes & mouse clicks",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            // Presets Row
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Presets:",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Button(
                                        onClick = {
                                            mapping = ControllerMapping(
                                                buttonA = EmulatedKey.KEY_SPACE,
                                                buttonB = EmulatedKey.KEY_SHIFT,
                                                buttonX = EmulatedKey.KEY_R,
                                                buttonY = EmulatedKey.KEY_E,
                                                buttonLT = EmulatedKey.MOUSE_RIGHT_CLICK,
                                                buttonRT = EmulatedKey.MOUSE_LEFT_CLICK,
                                                buttonLB = EmulatedKey.KEY_Q,
                                                buttonRB = EmulatedKey.KEY_F
                                            )
                                            onMappingChanged(mapping)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("FPS / WASD", fontSize = 11.sp, color = CyberCyan)
                                    }

                                    Button(
                                        onClick = {
                                            mapping = ControllerMapping(
                                                buttonA = EmulatedKey.KEY_SPACE,
                                                buttonB = EmulatedKey.KEY_ENTER,
                                                buttonX = EmulatedKey.KEY_C,
                                                buttonY = EmulatedKey.KEY_V,
                                                buttonLT = EmulatedKey.NONE,
                                                buttonRT = EmulatedKey.KEY_SPACE
                                            )
                                            onMappingChanged(mapping)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Arcade / Platformer", fontSize = 11.sp, color = NeonPurple)
                                    }
                                }
                            }

                            // Mouse Sensitivity & Deadzone
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBackground, RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Mouse Aim Sensitivity: ${(sensitivity * 100).toInt()}%",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = sensitivity,
                                        onValueChange = {
                                            sensitivity = it
                                            mapping = mapping.copy(mouseSensitivity = it)
                                            onMappingChanged(mapping)
                                        },
                                        valueRange = 0.2f..3.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = CyberCyan,
                                            activeTrackColor = CyberCyan
                                        ),
                                        modifier = Modifier.testTag("mouse_sensitivity_slider")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Show Virtual Mouse Reticle",
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Switch(
                                            checked = showCursor,
                                            onCheckedChange = {
                                                showCursor = it
                                                mapping = mapping.copy(showVirtualCursor = it)
                                                onMappingChanged(mapping)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = CyberCyan,
                                                checkedTrackColor = CyberCyan.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                }
                            }

                            // Button Remapping Items
                            item {
                                Text(
                                    text = "BUTTON REMAPPING",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val buttonRows = listOf(
                                Triple("Button A (Cross)", mapping.buttonA) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonA = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Button B (Circle)", mapping.buttonB) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonB = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Button X (Square)", mapping.buttonX) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonX = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Button Y (Triangle)", mapping.buttonY) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonY = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Right Trigger (RT / R2)", mapping.buttonRT) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonRT = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Left Trigger (LT / L2)", mapping.buttonLT) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonLT = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Right Bumper (RB / R1)", mapping.buttonRB) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonRB = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Left Bumper (LB / L1)", mapping.buttonLB) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonLB = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Left Stick Click (L3)", mapping.buttonL3) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonL3 = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Right Stick Click (R3)", mapping.buttonR3) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonR3 = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Start / Menu Button", mapping.buttonStart) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonStart = key)
                                    onMappingChanged(mapping)
                                },
                                Triple("Select / View Button", mapping.buttonSelect) { key: EmulatedKey ->
                                    mapping = mapping.copy(buttonSelect = key)
                                    onMappingChanged(mapping)
                                }
                            )

                            items(buttonRows) { (label, currentVal, onSelect) ->
                                ButtonMappingRow(
                                    label = label,
                                    currentValue = currentVal,
                                    onActionSelected = onSelect
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_mapping_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "APPLY & RESUME GAME",
                            color = DarkBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonMappingRow(
    label: String,
    currentValue: EmulatedKey,
    onActionSelected: (EmulatedKey) -> Unit
) {
    var isDropdownOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Box {
            Surface(
                modifier = Modifier
                    .clickable { isDropdownOpen = true }
                    .testTag("rebind_${label.take(8).trim()}"),
                shape = RoundedCornerShape(6.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
            ) {
                Text(
                    text = currentValue.label,
                    color = if (currentValue.isMouse) NeonGreen else CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            DropdownMenu(
                expanded = isDropdownOpen,
                onDismissRequest = { isDropdownOpen = false },
                modifier = Modifier
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder)
            ) {
                EmulatedKey.values().forEach { keyOption ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = keyOption.label,
                                color = if (keyOption == currentValue) CyberCyan else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (keyOption == currentValue) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onActionSelected(keyOption)
                            isDropdownOpen = false
                        }
                    )
                }
            }
        }
    }
}
