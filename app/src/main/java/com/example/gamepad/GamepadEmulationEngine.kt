package com.example.gamepad

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CursorPosition(
    val x: Float = 500f,
    val y: Float = 500f,
    val isVisible: Boolean = false
)

class GamepadEmulationEngine {

    companion object {
        val KBM_INJECTION_SCRIPT = """
            (function() {
                if (window.__webCadeKbmInstalled) return;
                window.__webCadeKbmInstalled = true;
                window.__webCadeKbm = {
                    cursorX: window.innerWidth / 2,
                    cursorY: window.innerHeight / 2,
                    activeKeys: {},
                    sendKey: function(code, key, keyCode, isDown) {
                        if (isDown) {
                            if (this.activeKeys[code]) return;
                            this.activeKeys[code] = true;
                        } else {
                            delete this.activeKeys[code];
                        }
                        var eventType = isDown ? 'keydown' : 'keyup';
                        var event = new KeyboardEvent(eventType, {
                            code: code,
                            key: key,
                            keyCode: keyCode,
                            which: keyCode,
                            bubbles: true,
                            cancelable: true,
                            view: window
                        });
                        var target = document.activeElement || document.body || document.documentElement;
                        target.dispatchEvent(event);
                        document.dispatchEvent(event);
                    },
                    sendMouseMove: function(x, y) {
                        this.cursorX = x;
                        this.cursorY = y;
                        var elem = document.elementFromPoint(x, y) || document.body;
                        if (!elem) return;
                        var event = new MouseEvent('mousemove', {
                            clientX: x,
                            clientY: y,
                            screenX: x,
                            screenY: y,
                            bubbles: true,
                            cancelable: true,
                            view: window
                        });
                        elem.dispatchEvent(event);
                    },
                    sendMouseDown: function(button) {
                        var elem = document.elementFromPoint(this.cursorX, this.cursorY) || document.body;
                        if (!elem) return;
                        var event = new MouseEvent('mousedown', {
                            button: button,
                            buttons: button === 0 ? 1 : 2,
                            clientX: this.cursorX,
                            clientY: this.cursorY,
                            bubbles: true,
                            cancelable: true,
                            view: window
                        });
                        elem.dispatchEvent(event);
                        if (elem.focus) elem.focus();
                    },
                    sendMouseUp: function(button) {
                        var elem = document.elementFromPoint(this.cursorX, this.cursorY) || document.body;
                        if (!elem) return;
                        var upEvent = new MouseEvent('mouseup', {
                            button: button,
                            buttons: 0,
                            clientX: this.cursorX,
                            clientY: this.cursorY,
                            bubbles: true,
                            cancelable: true,
                            view: window
                        });
                        elem.dispatchEvent(upEvent);
                        if (button === 0) {
                            var clickEvent = new MouseEvent('click', {
                                button: 0,
                                buttons: 1,
                                clientX: this.cursorX,
                                clientY: this.cursorY,
                                bubbles: true,
                                cancelable: true,
                                view: window
                            });
                            elem.dispatchEvent(clickEvent);
                        }
                    }
                };
                console.log("WebCade: Keyboard & Mouse Emulation initialized.");
            })();
        """.trimIndent()
    }

    private var targetWebView: WebView? = null
    var mapping: ControllerMapping = ControllerMapping()
    var controllerMode: ControllerMode = ControllerMode.DEDICATED_GAMEPAD

    private val _cursorPosition = MutableStateFlow(CursorPosition())
    val cursorPosition: StateFlow<CursorPosition> = _cursorPosition.asStateFlow()

    private var screenWidth = 1920f
    private var screenHeight = 1080f

    private var currentRx = 0f
    private var currentRy = 0f
    private var currentLx = 0f
    private var currentLy = 0f

    // Active key states for WASD to avoid spamming
    private var isWDown = false
    private var isSDown = false
    private var isADown = false
    private var isDDown = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTickerRunning = false

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (controllerMode == ControllerMode.KEYBOARD_MOUSE_EMULATION) {
                tickMouseAim()
                tickWasdMovement()
            }
            if (isTickerRunning) {
                mainHandler.postDelayed(this, 16) // ~60fps smooth loop
            }
        }
    }

    fun attachWebView(webView: WebView) {
        this.targetWebView = webView
        screenWidth = webView.width.takeIf { it > 0 }?.toFloat() ?: 1920f
        screenHeight = webView.height.takeIf { it > 0 }?.toFloat() ?: 1080f
        _cursorPosition.value = _cursorPosition.value.copy(
            x = screenWidth / 2f,
            y = screenHeight / 2f
        )
        startTicker()
    }

    fun updateScreenDimensions(width: Float, height: Float) {
        if (width > 0 && height > 0) {
            screenWidth = width
            screenHeight = height
        }
    }

    fun startTicker() {
        if (!isTickerRunning) {
            isTickerRunning = true
            mainHandler.post(tickerRunnable)
        }
    }

    fun stopTicker() {
        isTickerRunning = false
        mainHandler.removeCallbacks(tickerRunnable)
    }

    fun handleKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        if (controllerMode != ControllerMode.KEYBOARD_MOUSE_EMULATION) {
            return false // Let dedicated gamepad handle it
        }

        val emulatedAction = getEmulatedActionForKeyCode(keyCode)
        if (emulatedAction == EmulatedKey.NONE) {
            return false
        }

        dispatchEmulatedAction(emulatedAction, isDown)
        return true
    }

    fun handleTriggerChange(leftTrigger: Float, rightTrigger: Float) {
        if (controllerMode != ControllerMode.KEYBOARD_MOUSE_EMULATION) return

        val deadzone = mapping.deadzone
        val isLtPressed = leftTrigger > deadzone
        val isRtPressed = rightTrigger > deadzone

        dispatchEmulatedAction(mapping.buttonLT, isLtPressed)
        dispatchEmulatedAction(mapping.buttonRT, isRtPressed)
    }

    fun handleThumbsticks(lx: Float, ly: Float, rx: Float, ry: Float) {
        currentLx = lx
        currentLy = ly
        currentRx = rx
        currentRy = ry
    }

    private fun tickMouseAim() {
        val deadzone = mapping.deadzone
        val rx = currentRx
        val ry = currentRy

        if (abs(rx) > deadzone || abs(ry) > deadzone) {
            val speed = 18f * mapping.mouseSensitivity
            val dx = if (abs(rx) > deadzone) rx * speed else 0f
            val dy = if (abs(ry) > deadzone) ry * speed else 0f

            val newX = (_cursorPosition.value.x + dx).coerceIn(0f, screenWidth)
            val newY = (_cursorPosition.value.y + dy).coerceIn(0f, screenHeight)

            _cursorPosition.value = _cursorPosition.value.copy(
                x = newX,
                y = newY,
                isVisible = mapping.showVirtualCursor
            )

            // Inject mousemove event into webview
            val js = "if (window.__webCadeKbm) { window.__webCadeKbm.sendMouseMove($newX, $newY); }"
            targetWebView?.post { targetWebView?.evaluateJavascript(js, null) }
        }
    }

    private fun tickWasdMovement() {
        val deadzone = mapping.deadzone
        val lx = currentLx
        val ly = currentLy

        val wantW = ly < -deadzone
        val wantS = ly > deadzone
        val wantA = lx < -deadzone
        val wantD = lx > deadzone

        if (wantW != isWDown) {
            isWDown = wantW
            sendKey(EmulatedKey.KEY_W, isWDown)
        }
        if (wantS != isSDown) {
            isSDown = wantS
            sendKey(EmulatedKey.KEY_S, isSDown)
        }
        if (wantA != isADown) {
            isADown = wantA
            sendKey(EmulatedKey.KEY_A, isADown)
        }
        if (wantD != isDDown) {
            isDDown = wantD
            sendKey(EmulatedKey.KEY_D, isDDown)
        }
    }

    private fun dispatchEmulatedAction(action: EmulatedKey, isDown: Boolean) {
        if (action.isMouse) {
            val js = if (isDown) {
                "if (window.__webCadeKbm) { window.__webCadeKbm.sendMouseDown(${action.mouseButton}); }"
            } else {
                "if (window.__webCadeKbm) { window.__webCadeKbm.sendMouseUp(${action.mouseButton}); }"
            }
            targetWebView?.post { targetWebView?.evaluateJavascript(js, null) }
        } else {
            sendKey(action, isDown)
        }
    }

    private fun sendKey(action: EmulatedKey, isDown: Boolean) {
        val js = "if (window.__webCadeKbm) { window.__webCadeKbm.sendKey('${action.code}', '${action.key}', ${action.keyCode}, $isDown); }"
        targetWebView?.post { targetWebView?.evaluateJavascript(js, null) }
    }

    private fun getEmulatedActionForKeyCode(keyCode: Int): EmulatedKey {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> mapping.buttonA
            KeyEvent.KEYCODE_BUTTON_B -> mapping.buttonB
            KeyEvent.KEYCODE_BUTTON_X -> mapping.buttonX
            KeyEvent.KEYCODE_BUTTON_Y -> mapping.buttonY
            KeyEvent.KEYCODE_BUTTON_L1 -> mapping.buttonLB
            KeyEvent.KEYCODE_BUTTON_R1 -> mapping.buttonRB
            KeyEvent.KEYCODE_BUTTON_L2 -> mapping.buttonLT
            KeyEvent.KEYCODE_BUTTON_R2 -> mapping.buttonRT
            KeyEvent.KEYCODE_BUTTON_THUMBL -> mapping.buttonL3
            KeyEvent.KEYCODE_BUTTON_THUMBR -> mapping.buttonR3
            KeyEvent.KEYCODE_BUTTON_START -> mapping.buttonStart
            KeyEvent.KEYCODE_BUTTON_SELECT -> mapping.buttonSelect
            KeyEvent.KEYCODE_DPAD_UP -> mapping.dpadUp
            KeyEvent.KEYCODE_DPAD_DOWN -> mapping.dpadDown
            KeyEvent.KEYCODE_DPAD_LEFT -> mapping.dpadLeft
            KeyEvent.KEYCODE_DPAD_RIGHT -> mapping.dpadRight
            else -> EmulatedKey.NONE
        }
    }
}
