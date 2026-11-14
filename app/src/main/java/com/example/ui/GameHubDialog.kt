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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gamepad.ControllerMode
import com.example.gamepad.GamepadStatus
import com.example.model.GamingPlatform
import com.example.model.GamingPresets
import com.example.model.ScreenOrientationMode
import com.example.model.UserAgentMode
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameHubDialog(
    currentUrl: String,
    recentUrls: List<String> = emptyList(),
    userAgentMode: UserAgentMode,
    isVirtualGamepadEnabled: Boolean,
    isPerformanceHudEnabled: Boolean,
    gamepadStatus: GamepadStatus,
    onDismiss: () -> Unit,
    onLoadUrl: (String, UserAgentMode) -> Unit,
    onUserAgentChanged: (UserAgentMode) -> Unit,
    onToggleVirtualGamepad: (Boolean) -> Unit,
    onTogglePerformanceHud: (Boolean) -> Unit,
    onReload: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onClearCache: () -> Unit,
    onRequestPointerCapture: () -> Unit,
    onGoToHome: () -> Unit = {},
    controllerMode: ControllerMode = ControllerMode.DEDICATED_GAMEPAD,
    onOpenControllerMapping: () -> Unit = {},
    isAdBlockerEnabled: Boolean = true,
    blockedAdsCount: Int = 0,
    onToggleAdBlocker: (Boolean) -> Unit = {},
    orientationMode: ScreenOrientationMode = ScreenOrientationMode.AUTO,
    onOrientationModeChanged: (ScreenOrientationMode) -> Unit = {}
) {
    var customUrlInput by remember { mutableStateOf(currentUrl) }
    val clipboardManager = LocalClipboardManager.current

    fun submitUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        onLoadUrl(trimmed, userAgentMode)
        onDismiss()
    }

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
                    .heightIn(max = 700.dp)
                    .testTag("game_hub_card"),
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
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "WebCade Icon",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Game URL & Control Hub",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Input URL or switch platform",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Home / Portal button
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onGoToHome()
                                },
                                modifier = Modifier
                                    .testTag("hub_home_btn")
                                    .size(38.dp)
                                    .background(DarkSurfaceElevated, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Go to Game Portal Home",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Close button to resume full screen
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .testTag("close_hub_btn")
                                    .size(38.dp)
                                    .background(DarkSurfaceElevated, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Resume Fullscreen Game",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. DEDICATED URL INPUT SECTION
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = "URL",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "INPUT GAMING WEBSITE URL",
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = customUrlInput,
                                            onValueChange = { customUrlInput = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("custom_url_input"),
                                            singleLine = true,
                                            placeholder = {
                                                Text(
                                                    "e.g. krunker.io, crazygames.com, xbox.com/play",
                                                    color = TextMuted,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            trailingIcon = {
                                                if (customUrlInput.isNotEmpty()) {
                                                    IconButton(onClick = { customUrlInput = "" }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Clear,
                                                            contentDescription = "Clear",
                                                            tint = TextSecondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Uri,
                                                imeAction = ImeAction.Go
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onGo = { submitUrl(customUrlInput) }
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyberCyan,
                                                unfocusedBorderColor = DarkBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedContainerColor = DarkBackground,
                                                unfocusedContainerColor = DarkBackground
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        // Paste Button
                                        Button(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    customUrlInput = clip.trim()
                                                }
                                            },
                                            modifier = Modifier
                                                .height(52.dp)
                                                .testTag("dialog_paste_btn"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Go / Play Button
                                        Button(
                                            onClick = { submitUrl(customUrlInput) },
                                            modifier = Modifier
                                                .height(52.dp)
                                                .testTag("launch_url_btn"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Launch",
                                                tint = DarkBackground
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "GO",
                                                color = DarkBackground,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    // Recent URLs quick chips
                                    if (recentUrls.isNotEmpty()) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(recentUrls.take(6)) { url ->
                                                Surface(
                                                    modifier = Modifier
                                                        .clickable {
                                                            customUrlInput = url
                                                            submitUrl(url)
                                                        }
                                                        .testTag("dialog_recent_chip"),
                                                    color = DarkBackground,
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Public,
                                                            contentDescription = "Recent",
                                                            tint = CyberCyan,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = url.removePrefix("https://").removePrefix("http://"),
                                                            color = TextSecondary,
                                                            fontSize = 11.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Preset Cloud Gaming Platforms & Web Games
                        item {
                            Text(
                                text = "QUICK PRESET PLATFORMS",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(GamingPresets.platforms) { platform ->
                                    GamingPlatformCard(
                                        platform = platform,
                                        isActive = currentUrl == platform.url,
                                        onClick = {
                                            onLoadUrl(platform.url, platform.recommendedUa)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }

                        // 3. User Agent Presets
                        item {
                            Text(
                                text = "USER AGENT SPOOFING (AVOIDS MOBILE BLOCKING)",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(UserAgentMode.values()) { mode ->
                                    val isSelected = userAgentMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUserAgentChanged(mode) },
                                        label = {
                                            Text(
                                                text = when (mode) {
                                                    UserAgentMode.DESKTOP_CHROME -> "Desktop Windows 11"
                                                    UserAgentMode.CHROME_OS -> "ChromeOS"
                                                    UserAgentMode.MOBILE -> "Mobile"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonPurple,
                                            selectedLabelColor = Color.White,
                                            containerColor = DarkSurfaceElevated,
                                            labelColor = TextSecondary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("ua_chip_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Screen Orientation Mode (Auto, Landscape, Portrait)
                        item {
                            Text(
                                text = "SCREEN ORIENTATION (PORTRAIT OR LANDSCAPE)",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(ScreenOrientationMode.values()) { mode ->
                                    val isSelected = orientationMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onOrientationModeChanged(mode) },
                                        label = {
                                            Text(
                                                text = "${mode.iconEmoji} ${mode.label}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberCyan,
                                            selectedLabelColor = DarkBackground,
                                            containerColor = DarkSurfaceElevated,
                                            labelColor = TextSecondary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("orientation_chip_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // 4. Quick Browser Navigation Controls
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = onGoBack,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                        .testTag("nav_back_btn")
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                                }
                                IconButton(
                                    onClick = onGoForward,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                        .testTag("nav_forward_btn")
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = TextPrimary)
                                }
                                IconButton(
                                    onClick = onReload,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                        .testTag("nav_reload_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = CyberCyan)
                                }
                                IconButton(
                                    onClick = onClearCache,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                        .testTag("clear_cache_btn")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Cache", tint = NeonPink)
                                }
                                Button(
                                    onClick = {
                                        onRequestPointerCapture()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("mouse_lock_btn")
                                ) {
                                    Icon(Icons.Default.Mouse, contentDescription = "Lock Mouse", tint = CyberCyan)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LOCK MOUSE", color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }

                        // 5. Virtual Gamepad & Telemetry Toggles
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Virtual On-Screen Gamepad",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "On-screen analog sticks & ABXY buttons",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = isVirtualGamepadEnabled,
                                        onCheckedChange = onToggleVirtualGamepad,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberCyan,
                                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkBackground
                                        ),
                                        modifier = Modifier.testTag("toggle_virtual_gamepad")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Telemetry & Performance HUD",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Show FPS, HW accel & controller state",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = isPerformanceHudEnabled,
                                        onCheckedChange = onTogglePerformanceHud,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeonGreen,
                                            checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkBackground
                                        ),
                                        modifier = Modifier.testTag("toggle_performance_hud")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Ad-Blocker DNS Filtering",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (blockedAdsCount > 0) {
                                                Surface(
                                                    color = CyberCyan.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "$blockedAdsCount blocked",
                                                        color = CyberCyan,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "DNS sinkhole & popup blocker for gaming",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = isAdBlockerEnabled,
                                        onCheckedChange = onToggleAdBlocker,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberCyan,
                                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkBackground
                                        ),
                                        modifier = Modifier.testTag("toggle_adblocker_dns")
                                    )
                                }
                            }
                        }

                        // 6. Connected Controller Status
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        if (gamepadStatus.isConnected) NeonGreen else TextMuted,
                                                        CircleShape
                                                    )
                                            )
                                            Text(
                                                text = if (gamepadStatus.isConnected) {
                                                    "Controller: ${gamepadStatus.deviceName}"
                                                } else {
                                                    "No Physical Controller Connected"
                                                },
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Text(
                                            text = if (controllerMode == ControllerMode.DEDICATED_GAMEPAD) "Dedicated Gamepad" else "KBM Emulation",
                                            color = if (controllerMode == ControllerMode.KEYBOARD_MOUSE_EMULATION) NeonPurple else CyberCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Button(
                                        onClick = onOpenControllerMapping,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .testTag("open_controller_mapping_btn"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Configure",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CONFIGURE KEY & MOUSE MAPPING / MODE",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Resume Fullscreen Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("resume_fullscreen_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "RESUME FULL SCREEN (ZERO GUI)",
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
fun GamingPlatformCard(
    platform: GamingPlatform,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick)
            .testTag("platform_card_${platform.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) DarkSurfaceElevated else DarkBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) CyberCyan else DarkBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = platform.iconEmoji, fontSize = 22.sp)
                Column {
                    Text(
                        text = platform.tag,
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = platform.name,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = platform.description,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                lineHeight = 14.sp
            )
        }
    }
}
