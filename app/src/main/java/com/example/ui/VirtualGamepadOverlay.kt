package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Button bit flags
const val BTN_A = 1 shl 0
const val BTN_B = 1 shl 1
const val BTN_X = 1 shl 2
const val BTN_Y = 1 shl 3
const val BTN_LB = 1 shl 4
const val BTN_RB = 1 shl 5
const val BTN_LT = 1 shl 6
const val BTN_RT = 1 shl 7
const val BTN_SELECT = 1 shl 8
const val BTN_START = 1 shl 9
const val BTN_DPAD_UP = 1 shl 12
const val BTN_DPAD_DOWN = 1 shl 13
const val BTN_DPAD_LEFT = 1 shl 14
const val BTN_DPAD_RIGHT = 1 shl 15

@Composable
fun VirtualGamepadOverlay(
    onGamepadUpdate: (lx: Float, ly: Float, rx: Float, ry: Float, btnMask: Int, lt: Float, rt: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var lx by remember { mutableFloatStateOf(0f) }
    var ly by remember { mutableFloatStateOf(0f) }
    var rx by remember { mutableFloatStateOf(0f) }
    var ry by remember { mutableFloatStateOf(0f) }
    var btnMask by remember { mutableIntStateOf(0) }
    var lt by remember { mutableFloatStateOf(0f) }
    var rt by remember { mutableFloatStateOf(0f) }

    fun sendUpdate() {
        onGamepadUpdate(lx, ly, rx, ry, btnMask, lt, rt)
    }

    fun setButtonPressed(bit: Int, pressed: Boolean) {
        btnMask = if (pressed) btnMask or bit else btnMask and bit.inv()
        sendUpdate()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("virtual_gamepad_overlay")
    ) {
        // Top Shoulders (LB, LT, RB, RT) & Select/Start
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Shoulders
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamepadShoulderButton(
                    label = "LT",
                    testTag = "btn_lt",
                    isPressed = (btnMask and BTN_LT) != 0,
                    onPressChanged = { pressed ->
                        lt = if (pressed) 1.0f else 0.0f
                        setButtonPressed(BTN_LT, pressed)
                    }
                )
                GamepadShoulderButton(
                    label = "LB",
                    testTag = "btn_lb",
                    isPressed = (btnMask and BTN_LB) != 0,
                    onPressChanged = { pressed -> setButtonPressed(BTN_LB, pressed) }
                )
            }

            // Center View / Menu buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GamepadPillButton(
                    label = "VIEW",
                    testTag = "btn_select",
                    isPressed = (btnMask and BTN_SELECT) != 0,
                    onPressChanged = { pressed -> setButtonPressed(BTN_SELECT, pressed) }
                )
                GamepadPillButton(
                    label = "MENU",
                    testTag = "btn_start",
                    isPressed = (btnMask and BTN_START) != 0,
                    onPressChanged = { pressed -> setButtonPressed(BTN_START, pressed) }
                )
            }

            // Right Shoulders
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamepadShoulderButton(
                    label = "RB",
                    testTag = "btn_rb",
                    isPressed = (btnMask and BTN_RB) != 0,
                    onPressChanged = { pressed -> setButtonPressed(BTN_RB, pressed) }
                )
                GamepadShoulderButton(
                    label = "RT",
                    testTag = "btn_rt",
                    isPressed = (btnMask and BTN_RT) != 0,
                    onPressChanged = { pressed ->
                        rt = if (pressed) 1.0f else 0.0f
                        setButtonPressed(BTN_RT, pressed)
                    }
                )
            }
        }

        // Bottom Left: Left Joystick & D-Pad
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
        ) {
            VirtualJoystick(
                testTag = "left_joystick",
                onStickMove = { x, y ->
                    lx = x
                    ly = y
                    sendUpdate()
                }
            )
        }

        // Bottom Right: ABXY Buttons
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            AbxyCluster(
                btnMask = btnMask,
                onButtonPress = { bit, pressed -> setButtonPressed(bit, pressed) }
            )
        }
    }
}

@Composable
fun VirtualJoystick(
    testTag: String,
    onStickMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseRadius = 65.dp
    val stickRadius = 28.dp

    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(baseRadius * 2)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = offset - center
                        val maxDistance = size.width / 2f - 20f
                        val dist = delta.getDistance()
                        val clamped = if (dist > maxDistance) delta * (maxDistance / dist) else delta
                        dragOffset = clamped
                        onStickMove(clamped.x / maxDistance, clamped.y / maxDistance)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val current = dragOffset + dragAmount
                        val maxDistance = size.width / 2f - 20f
                        val dist = current.getDistance()
                        val clamped = if (dist > maxDistance) current * (maxDistance / dist) else current
                        dragOffset = clamped
                        onStickMove(clamped.x / maxDistance, clamped.y / maxDistance)
                    },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        onStickMove(0f, 0f)
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        onStickMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Base Circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x33000000),
                radius = size.minDimension / 2f
            )
            drawCircle(
                color = Color(0x6600F2FE),
                radius = size.minDimension / 2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            // Center crosshair
            drawLine(
                color = Color(0x33FFFFFF),
                start = Offset(size.width / 2f, size.height * 0.25f),
                end = Offset(size.width / 2f, size.height * 0.75f)
            )
            drawLine(
                color = Color(0x33FFFFFF),
                start = Offset(size.width * 0.25f, size.height / 2f),
                end = Offset(size.width * 0.75f, size.height / 2f)
            )
        }

        // Floating Stick Thumb
        Surface(
            modifier = Modifier
                .offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }
                .size(stickRadius * 2),
            shape = CircleShape,
            color = Color(0xCC161E2E),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, CyberCyan)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(CyberCyan, CircleShape)
                )
            }
        }
    }
}

@Composable
fun AbxyCluster(
    btnMask: Int,
    onButtonPress: (bit: Int, pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val clusterSize = 140.dp
    val buttonSize = 46.dp

    Box(
        modifier = modifier
            .size(clusterSize)
            .testTag("abxy_cluster"),
        contentAlignment = Alignment.Center
    ) {
        // Y - Top
        GamepadActionButton(
            label = "Y",
            color = Color(0xFFFBBF24),
            isPressed = (btnMask and BTN_Y) != 0,
            onPressChanged = { onButtonPress(BTN_Y, it) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize)
        )
        // A - Bottom
        GamepadActionButton(
            label = "A",
            color = Color(0xFF34D399),
            isPressed = (btnMask and BTN_A) != 0,
            onPressChanged = { onButtonPress(BTN_A, it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(buttonSize)
        )
        // X - Left
        GamepadActionButton(
            label = "X",
            color = Color(0xFF60A5FA),
            isPressed = (btnMask and BTN_X) != 0,
            onPressChanged = { onButtonPress(BTN_X, it) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(buttonSize)
        )
        // B - Right
        GamepadActionButton(
            label = "B",
            color = Color(0xFFF87171),
            isPressed = (btnMask and BTN_B) != 0,
            onPressChanged = { onButtonPress(BTN_B, it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(buttonSize)
        )
    }
}

@Composable
fun GamepadActionButton(
    label: String,
    color: Color,
    isPressed: Boolean,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPressed) color else Color(0xCC0F141C)
    val textColor = if (isPressed) Color.Black else color

    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChanged(true)
                        tryAwaitRelease()
                        onPressChanged(false)
                    }
                )
            },
        shape = CircleShape,
        color = bgColor,
        shadowElevation = if (isPressed) 2.dp else 4.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, color)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun GamepadShoulderButton(
    label: String,
    testTag: String,
    isPressed: Boolean,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(width = 64.dp, height = 40.dp)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChanged(true)
                        tryAwaitRelease()
                        onPressChanged(false)
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = if (isPressed) CyberCyan else Color(0xCC0F141C),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isPressed) Color.Black else CyberCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GamepadPillButton(
    label: String,
    testTag: String,
    isPressed: Boolean,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(width = 60.dp, height = 32.dp)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChanged(true)
                        tryAwaitRelease()
                        onPressChanged(false)
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isPressed) NeonPurple else Color(0x990F141C),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isPressed) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
