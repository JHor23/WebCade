package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BookmarkManager
import com.example.model.SiteBookmark
import com.example.model.UserAgentMode
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BookmarkDialog(
    initialUrl: String,
    initialTitle: String,
    initialUa: UserAgentMode,
    existingBookmark: SiteBookmark? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String, preferredUa: UserAgentMode) -> Unit,
    onDelete: ((SiteBookmark) -> Unit)? = null
) {
    val isEditing = existingBookmark != null
    val clipboardManager = LocalClipboardManager.current

    var urlText by remember(existingBookmark, initialUrl) {
        mutableStateOf(existingBookmark?.url ?: initialUrl)
    }
    var titleText by remember(existingBookmark, initialTitle, initialUrl) {
        mutableStateOf(
            existingBookmark?.title
                ?: initialTitle.takeIf { it.isNotBlank() }
                ?: BookmarkManager.deriveTitleFromUrl(initialUrl).takeIf { initialUrl.isNotBlank() }
                ?: ""
        )
    }
    var selectedUa by remember(existingBookmark, initialUa) {
        mutableStateOf(existingBookmark?.preferredUa ?: initialUa)
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
                .background(Color(0xD804070D))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .clickable(enabled = false) {}
                    .testTag("bookmark_editor_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = BorderStroke(1.5.dp, CyberCyan),
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CyberCyan.copy(alpha = 0.16f), CircleShape)
                                    .border(1.dp, CyberCyan.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Bookmark",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isEditing) "EDIT BOOKMARK" else "SAVE BOOKMARK",
                                    color = CyberCyan,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = "Quick-launch website with preferred browser mode",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("bookmark_dialog_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Bookmark Dialog",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Bookmark Title
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Bookmark Name", fontSize = 12.sp) },
                        placeholder = {
                            Text(
                                "e.g. PokéFable, Xbox Cloud, YouTube",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        trailingIcon = {
                            if (titleText.isNotEmpty()) {
                                IconButton(onClick = { titleText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Name",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bookmark_title_input")
                    )

                    // Bookmark URL + Paste button
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("Website URL", fontSize = 12.sp) },
                        placeholder = {
                            Text(
                                "https://example.com",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            urlText = clip.trim()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste URL",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (urlText.isNotEmpty()) {
                                    IconButton(onClick = { urlText = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bookmark_url_input")
                    )

                    // Preferred User Agent selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PREFERRED USER AGENT",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            UserAgentMode.entries.forEach { mode ->
                                val isSelected = selectedUa == mode
                                val shortLabel = when (mode) {
                                    UserAgentMode.DESKTOP_CHROME -> "Desktop"
                                    UserAgentMode.CHROME_OS -> "ChromeOS"
                                    UserAgentMode.MOBILE -> "Mobile"
                                }
                                Surface(
                                    onClick = { selectedUa = mode },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("bookmark_ua_${mode.name.lowercase()}"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NeonPurple else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = mode.iconEmoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = shortLabel,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing && existingBookmark != null && onDelete != null) {
                            OutlinedButton(
                                onClick = {
                                    onDelete(existingBookmark)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("bookmark_delete_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Bookmark",
                                    tint = NeonPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (urlText.isNotBlank()) {
                                    onSave(titleText.trim(), urlText.trim(), selectedUa)
                                    onDismiss()
                                }
                            },
                            enabled = urlText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                disabledContainerColor = CyberCyan.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("bookmark_save_confirm_btn")
                        ) {
                            Text(
                                text = if (isEditing) "Save Changes" else "Save Bookmark",
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
}
