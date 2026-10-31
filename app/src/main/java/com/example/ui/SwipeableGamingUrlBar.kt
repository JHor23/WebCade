package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamingPresets
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
    modifier: Modifier = Modifier
) {
    var urlInputText by remember { mutableStateOf(currentUrl) }
    val clipboardManager = LocalClipboardManager.current
    var totalVerticalDrag by remember { mutableFloatStateOf(0f) }

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
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("swipeable_url_bar_container")
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                // SWIPE UP TO DISMISS GESTURE DETECTOR
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            totalVerticalDrag += dragAmount
                            // If dragged upward by more than 20 pixels, dismiss the URL bar
                            if (totalVerticalDrag < -25f) {
                                change.consume()
                                totalVerticalDrag = 0f
                                onDismissUrlBar()
                            }
                        },
                        onDragEnd = { totalVerticalDrag = 0f },
                        onDragCancel = { totalVerticalDrag = 0f }
                    )
                }
                .testTag("swipeable_url_bar_card"),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: URL Input, Paste, Clear, GO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Back & Forward navigation
                    IconButton(
                        onClick = onGoBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                            .testTag("url_bar_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Main URL TextField
                    OutlinedTextField(
                        value = urlInputText,
                        onValueChange = { urlInputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("url_bar_text_field"),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Enter game URL (krunker.io, xbox.com/play...)",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "URL",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (urlInputText.isNotEmpty()) {
                                IconButton(onClick = { urlInputText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
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
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Paste Button
                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                urlInputText = clip.trim()
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("url_bar_paste_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PASTE", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Launch / Go Button
                    Button(
                        onClick = { submit() },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("url_bar_go_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Go",
                            tint = DarkBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "GO",
                            color = DarkBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }

                    // Full Hub icon button
                    IconButton(
                        onClick = onOpenFullHub,
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                            .testTag("url_bar_hub_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Full Hub",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Second Row: Recent URLs & Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick:",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Show recent URLs first
                        items(recentUrls.take(4)) { recentUrl ->
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        urlInputText = recentUrl
                                        onNavigateToUrl(recentUrl, userAgentMode)
                                        onDismissUrlBar()
                                    }
                                    .testTag("quick_recent_url"),
                                shape = RoundedCornerShape(6.dp),
                                color = DarkBackground,
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
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = recentUrl.removePrefix("https://").removePrefix("http://"),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Preset shortcuts
                        items(GamingPresets.platforms.take(5)) { preset ->
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        urlInputText = preset.url
                                        onNavigateToUrl(preset.url, preset.recommendedUa)
                                        onDismissUrlBar()
                                    }
                                    .testTag("quick_preset_${preset.id}"),
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                            ) {
                                Text(
                                    text = "${preset.iconEmoji} ${preset.tag}",
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Reload
                    IconButton(
                        onClick = onReload,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("url_bar_reload_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Bottom Dismiss Handle: "Swipe up to hide"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissUrlBar() }
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Swipe Up to Hide",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SWIPE UP TO HIDE (FULL SCREEN)",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Swipe Up to Hide",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
