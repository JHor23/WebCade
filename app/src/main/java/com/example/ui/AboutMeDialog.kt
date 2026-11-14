package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonPurple

@Composable
fun AboutMeDialog(
    onDismiss: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun openExternalOrInApp(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Throwable) {
            onDismiss()
            onOpenUrlInBrowser(url)
        }
    }

    val neonBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(CyberCyan, NeonPurple, Color(0xFF00D4FF), NeonPurple)
        )
    }

    val cardLightBackground = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF3F7FF),
                Color(0xFFE7EFFC),
                Color(0xFFF5F8FF)
            )
        )
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
                .background(Color(0xDC04070D))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .clickable(enabled = false) {}
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = CyberCyan,
                        spotColor = NeonPurple
                    )
                    .testTag("about_me_dialog_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, neonBorderBrush),
                color = Color(0xFF080E1E)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardLightBackground)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Dark Cyberpunk Geometric Top Banner + Overlapping Avatar
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Dark cyber header backdrop with angled geometric cutout and neon wings
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .drawBehind {
                                    val w = size.width
                                    val h = size.height

                                    // Dark cyber sky
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF050A17),
                                                Color(0xFF0B162C),
                                                Color(0xFF101D38)
                                            )
                                        )
                                    )

                                    // Angled bright lower shoulder notch (left and right trapezoid edges)
                                    val shoulderPath = Path().apply {
                                        moveTo(0f, h * 0.78f)
                                        lineTo(w * 0.16f, h)
                                        lineTo(w * 0.84f, h)
                                        lineTo(w, h * 0.78f)
                                        lineTo(w, h)
                                        lineTo(0f, h)
                                        close()
                                    }
                                    drawPath(shoulderPath, color = Color(0xFFF3F7FF))

                                    // Left cyan diagonal light streaks
                                    drawLine(
                                        color = CyberCyan.copy(alpha = 0.85f),
                                        start = Offset(0f, h * 0.22f),
                                        end = Offset(w * 0.26f, h * 0.68f),
                                        strokeWidth = 6f
                                    )
                                    drawLine(
                                        color = CyberCyan.copy(alpha = 0.45f),
                                        start = Offset(0f, h * 0.40f),
                                        end = Offset(w * 0.20f, h * 0.76f),
                                        strokeWidth = 10f
                                    )
                                    drawLine(
                                        color = CyberCyan.copy(alpha = 0.25f),
                                        start = Offset(w * 0.08f, 0f),
                                        end = Offset(w * 0.30f, h * 0.45f),
                                        strokeWidth = 4f
                                    )

                                    // Right neon purple diagonal light streaks
                                    drawLine(
                                        color = NeonPurple.copy(alpha = 0.85f),
                                        start = Offset(w, h * 0.22f),
                                        end = Offset(w * 0.74f, h * 0.68f),
                                        strokeWidth = 6f
                                    )
                                    drawLine(
                                        color = Color(0xFFD500F9).copy(alpha = 0.45f),
                                        start = Offset(w, h * 0.40f),
                                        end = Offset(w * 0.80f, h * 0.76f),
                                        strokeWidth = 10f
                                    )
                                    drawLine(
                                        color = NeonPurple.copy(alpha = 0.28f),
                                        start = Offset(w * 0.92f, 0f),
                                        end = Offset(w * 0.70f, h * 0.45f),
                                        strokeWidth = 4f
                                    )

                                    // Crisp cyan & purple border accents along shoulder angle
                                    drawLine(
                                        color = CyberCyan,
                                        start = Offset(0f, h * 0.78f),
                                        end = Offset(w * 0.16f, h),
                                        strokeWidth = 3.5f
                                    )
                                    drawLine(
                                        color = NeonPurple,
                                        start = Offset(w, h * 0.78f),
                                        end = Offset(w * 0.84f, h),
                                        strokeWidth = 3.5f
                                    )
                                }
                        ) {
                            // Close button in top-right corner of the About Me card
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(48.dp)
                                    .testTag("about_me_close_btn")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x990A1328))
                                        .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close About Me",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Glowing Circular Tron / Daft Punk Avatar overlapping banner seam
                        Box(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .size(146.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    ambientColor = CyberCyan,
                                    spotColor = NeonPurple
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CyberCyan, Color.White, NeonPurple)
                                    )
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_jhor_avatar_1794639141792),
                                contentDescription = "JHor Tron / Daft Punk Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. "JHor" Title flanked by glowing neon bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, CyberCyan)
                                    )
                                )
                        )
                        Text(
                            text = "JHor",
                            color = Color(0xFF0A1A38),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(NeonPurple, Color.Transparent)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Neon circuit divider with center ring
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, CyberCyan)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFF102A6B), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(NeonPurple, Color.Transparent)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // "ABOUT ME" Subheader
                    Text(
                        text = "ABOUT ME",
                        color = Color(0xFF0D234D),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Bio Text matching screenshot
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hey there! I’m an independent developer and server admin who loves vibe coding, useful mobile tools, and media applications.",
                            color = Color(0xFF1A2438),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Whether I’m building programs, vibing to music, or trolling the community, I’m always looking for ways to improve our quality of life.",
                            color = Color(0xFF1A2438),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Action Pill Buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pill 1: SUPPORT ON KO-FI
                        Button(
                            onClick = { openExternalOrInApp("https://ko-fi.com/jhor23") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("about_me_kofi_btn"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A89B)),
                            border = BorderStroke(1.5.dp, Color(0xFF5CF6EA)),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "SUPPORT ON KO-FI",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Ko-fi Heart",
                                        tint = Color(0xFFFF4D5A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Pill 2: VIEW SOURCE (GITHUB)
                        Button(
                            onClick = { openExternalOrInApp("https://github.com/JHor23/WebCade") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("about_me_github_btn"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF091B3A)),
                            border = BorderStroke(1.5.dp, CyberCyan),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "VIEW SOURCE (GITHUB)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "GitHub Source",
                                        tint = Color(0xFF091B3A),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        // Pill 3: CHECK OUT MY PROJECTS
                        Button(
                            onClick = { openExternalOrInApp("https://github.com/JHor23") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("about_me_projects_btn"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7200CA)),
                            border = BorderStroke(1.5.dp, Color(0xFFD68BFF)),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "CHECK OUT MY PROJECTS",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Projects",
                                        tint = Color(0xFF7200CA),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Footer: Version & Thank You
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, CyberCyan)
                                    )
                                )
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} | Thanks for the support!",
                            color = Color(0xFF3A4963),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(NeonPurple, Color.Transparent)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
