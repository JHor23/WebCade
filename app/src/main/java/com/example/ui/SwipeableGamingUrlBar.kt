package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwipeableGamingUrlBar(
    isVisible: Boolean,
    currentUrl: String,
    recentUrls: List<String>,
    userAgentMode: UserAgentMode,
    onDismissUrlBar: () -> Unit,
    onNavigateToUrl: (String, UserAgentMode) -> Unit,
    onUserAgentChanged: (UserAgentMode) -> Unit,
    onReload: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onOpenFullHub: () -> Unit,
    onGoToHome: () -> Unit = {},
    isAdBlockerEnabled: Boolean = true,
    onToggleAdBlocker: () -> Unit = {},
    onClearBrowserData: () -> Unit = {},
    orientationMode: ScreenOrientationMode = ScreenOrientationMode.AUTO,
    onToggleOrientationMode: () -> Unit = {},
    onSetOrientationMode: (ScreenOrientationMode) -> Unit = {},
    pageTitle: String = "",
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    modifier: Modifier = Modifier
) {
    var urlInputText by remember { mutableStateOf(currentUrl) }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(currentUrl) {
        urlInputText = currentUrl
    }

    fun submit() {
        val trimmed = urlInputText.trim()
        if (trimmed.isNotEmpty()) {
            val finalUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            onNavigateToUrl(finalUrl, userAgentMode)
            onDismissUrlBar()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 }),
        modifier = modifier
            .fillMaxSize()
            .testTag("full_screen_quick_settings_container")
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row: Title, Current Site Status, and Resume Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Text(
                                text = "QUICK SETTINGS & NAVIGATION",
                                color = CyberCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (pageTitle.isNotBlank() || currentUrl.isNotBlank()) {
                            Text(
                                text = pageTitle.ifBlank { currentUrl },
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Prominent Resume Button (X / Close)
                    IconButton(
                        onClick = onDismissUrlBar,
                        modifier = Modifier
                            .size(48.dp)
                            .background(DarkSurfaceElevated, CircleShape)
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                            .testTag("quick_settings_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Resume Game",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Scrollable Content Body with Ample Space
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 720.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. URL Input & Primary Navigation Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "GAME URL / WEB ADDRESS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            // Main Full-Width URL Input
                            OutlinedTextField(
                                value = urlInputText,
                                onValueChange = { urlInputText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("url_bar_text_field"),
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        "Enter game URL (e.g. xbox.com/play, krunker.io)...",
                                        color = TextMuted,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "URL",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (urlInputText.isNotEmpty()) {
                                        IconButton(onClick = { urlInputText = "" }) {
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
                                    onGo = { submit() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkBackground,
                                    unfocusedContainerColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Primary Action Buttons Row: PASTE & GO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            urlInputText = clip.trim()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("url_bar_paste_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "PASTE URL",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { submit() },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(50.dp)
                                        .testTag("url_bar_go_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Go",
                                        tint = DarkBackground,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LAUNCH GAME",
                                        color = DarkBackground,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Browser Navigation Controls Row (Back, Forward, Reload, Exit to Home)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onGoBack,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("url_bar_back_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = if (canGoBack) CyberCyan else TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back", color = TextPrimary, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = onGoForward,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("url_bar_forward_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Forward",
                                        tint = if (canGoForward) CyberCyan else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Forward", color = TextPrimary, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        onReload()
                                        onDismissUrlBar()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("url_bar_reload_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reload", color = TextPrimary, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        onDismissUrlBar()
                                        onGoToHome()
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp)
                                        .testTag("url_bar_home_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home",
                                        tint = NeonPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Portal Home", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. Display & Screen Orientation Settings Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Screen Orientation",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "SCREEN ORIENTATION",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = when (orientationMode) {
                                        ScreenOrientationMode.AUTO -> "Auto-Rotate"
                                        ScreenOrientationMode.LANDSCAPE -> "Landscape"
                                        ScreenOrientationMode.PORTRAIT -> "Portrait"
                                    },
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Orientation Segmented Buttons with 48dp Touch Targets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val modes = listOf(
                                    Triple(ScreenOrientationMode.AUTO, "Auto (Sensor)", "🔄"),
                                    Triple(ScreenOrientationMode.LANDSCAPE, "Landscape", "🖥️"),
                                    Triple(ScreenOrientationMode.PORTRAIT, "Portrait", "📱")
                                )

                                modes.forEach { (mode, label, emoji) ->
                                    val isSelected = orientationMode == mode
                                    Button(
                                        onClick = {
                                            onSetOrientationMode(mode)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("orientation_btn_${mode.name.lowercase()}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan) else null
                                    ) {
                                        Text(text = emoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            color = if (isSelected) CyberCyan else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Ad-Blocker & Privacy Controls Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "PRIVACY, ADS & STORAGE",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            // Ad-Blocker DNS Filtering Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                                    .clickable { onToggleAdBlocker() }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Ad Blocker",
                                        tint = if (isAdBlockerEnabled) CyberCyan else TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Ad-Blocker DNS Filtering",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isAdBlockerEnabled) "Active • Blocking video ads, trackers & malware" else "Disabled • Tap to enable DNS filtering",
                                            color = if (isAdBlockerEnabled) CyberCyan else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = isAdBlockerEnabled,
                                    onCheckedChange = { onToggleAdBlocker() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberCyan,
                                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBackground
                                    )
                                )
                            }

                            // Clear Cache & Cookies Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                                    .clickable {
                                        onDismissUrlBar()
                                        onClearBrowserData()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear Browser Data",
                                        tint = NeonPink,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Clear Cache & Cookies",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Flushes browser cache, cookies, local storage & WebGL buffers",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onDismissUrlBar()
                                        onClearBrowserData()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.15f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("url_bar_clear_data_btn")
                                ) {
                                    Text("CLEAR", color = NeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 4. Browser User-Agent Identity Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "BROWSER IDENTITY (USER AGENT)",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UserAgentMode.entries.forEach { mode ->
                                    val isSelected = userAgentMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUserAgentChanged(mode) },
                                        label = {
                                            Text(
                                                text = "${mode.iconEmoji} ${mode.label}",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                            selectedLabelColor = CyberCyan,
                                            containerColor = DarkSurfaceElevated,
                                            labelColor = TextSecondary
                                        ),
                                        border = if (isSelected) {
                                            FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = true,
                                                borderColor = CyberCyan,
                                                borderWidth = 1.dp
                                            )
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // 5. Cloud Gaming Presets & Quick Shortcuts Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CLOUD GAMING PRESETS & QUICK SHORTCUTS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GamingPresets.platforms.forEach { preset ->
                                    Surface(
                                        modifier = Modifier
                                            .clickable {
                                                urlInputText = preset.url
                                                onNavigateToUrl(preset.url, preset.recommendedUa)
                                                onDismissUrlBar()
                                            }
                                            .testTag("quick_preset_${preset.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = DarkSurfaceElevated,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = preset.iconEmoji, fontSize = 14.sp)
                                            Text(
                                                text = preset.name,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            if (recentUrls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "RECENT WEBSITES",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    recentUrls.take(3).forEach { recentUrl ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    urlInputText = recentUrl
                                                    onNavigateToUrl(recentUrl, userAgentMode)
                                                    onDismissUrlBar()
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = DarkBackground,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Public,
                                                    contentDescription = "Recent URL",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = recentUrl,
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Gamepad Diagnostic & Full Hub Launcher Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismissUrlBar()
                                    onOpenFullHub()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "Controller Hub",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Gamepad & Key Mapping Hub",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Hardware diagnostics, custom button mapping & mouse reticle",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Open Hub",
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Bottom Sticky Resume Game Action Button
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismissUrlBar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .height(56.dp)
                        .testTag("resume_game_bottom_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume Playing",
                        tint = DarkBackground,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RESUME GAME",
                            color = DarkBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Tap here or press Phone Back button to resume",
                            color = DarkBackground.copy(alpha = 0.75f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
