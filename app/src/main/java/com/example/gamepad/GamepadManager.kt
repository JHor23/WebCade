package com.example.gamepad

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GamepadStatus(
    val isConnected: Boolean = false,
    val deviceName: String = "No Controller Connected",
    val deviceId: Int = -1,
    val lastButtonPressed: String = "",
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f
)

class GamepadManager(private val context: Context) : InputManager.InputDeviceListener {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val _gamepadStatus = MutableStateFlow(GamepadStatus())
    val gamepadStatus: StateFlow<GamepadStatus> = _gamepadStatus.asStateFlow()

    private var isSelectPressed = false
    private var isStartPressed = false
    private var isMenuHotkeyTriggered = false

    init {
        inputManager?.registerInputDeviceListener(this, null)
        refreshConnectedControllers()
    }

    fun cleanup() {
        inputManager?.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        refreshConnectedControllers()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        refreshConnectedControllers()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshConnectedControllers()
    }

    fun refreshConnectedControllers() {
        val deviceIds = InputDevice.getDeviceIds()
        var foundGamepad: InputDevice? = null

        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            if ((sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
            ) {
                foundGamepad = device
                break
            }
        }

        if (foundGamepad != null) {
            _gamepadStatus.value = _gamepadStatus.value.copy(
                isConnected = true,
                deviceName = foundGamepad.name,
                deviceId = foundGamepad.id
            )
        } else {
            _gamepadStatus.value = _gamepadStatus.value.copy(
                isConnected = false,
                deviceName = "No Controller Connected",
                deviceId = -1
            )
        }
    }

    /**
     * Inspects KeyEvent from the controller.
     * Returns true if the event was consumed (e.g., hotkey pressed).
     */
    fun handleKeyEvent(event: KeyEvent, onToggleGameMenu: () -> Unit): Boolean {
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN

        // Track Select and Start for hotkey combination
        if (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT || keyCode == KeyEvent.KEYCODE_BACK) {
            isSelectPressed = isDown
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_START || keyCode == KeyEvent.KEYCODE_MENU) {
            isStartPressed = isDown
        }

        // Combo: Select + Start together toggles Game Menu
        if (isSelectPressed && isStartPressed) {
            if (!isMenuHotkeyTriggered && isDown) {
                isMenuHotkeyTriggered = true
                onToggleGameMenu()
                return true
            }
        } else {
            isMenuHotkeyTriggered = false
        }

        if (isDown) {
            val buttonName = getButtonName(keyCode)
            if (buttonName.isNotEmpty()) {
                _gamepadStatus.value = _gamepadStatus.value.copy(lastButtonPressed = buttonName)
            }
        }

        return false
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val lx = event.getAxisValue(MotionEvent.AXIS_X)
            val ly = event.getAxisValue(MotionEvent.AXIS_Y)
            val rx = event.getAxisValue(MotionEvent.AXIS_Z)
            val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
            val lt = event.getAxisValue(MotionEvent.AXIS_BRAKE).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_LTRIGGER))
            val rt = event.getAxisValue(MotionEvent.AXIS_GAS).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_RTRIGGER))

            _gamepadStatus.value = _gamepadStatus.value.copy(
                leftStickX = lx,
                leftStickY = ly,
                rightStickX = rx,
                rightStickY = ry,
                leftTrigger = lt,
                rightTrigger = rt
            )
        }
        return false
    }

    private fun getButtonName(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> "A / Cross"
            KeyEvent.KEYCODE_BUTTON_B -> "B / Circle"
            KeyEvent.KEYCODE_BUTTON_X -> "X / Square"
            KeyEvent.KEYCODE_BUTTON_Y -> "Y / Triangle"
            KeyEvent.KEYCODE_BUTTON_L1 -> "LB / L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> "RB / R1"
            KeyEvent.KEYCODE_BUTTON_L2 -> "LT / L2"
            KeyEvent.KEYCODE_BUTTON_R2 -> "RT / R2"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3 / Left Stick"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3 / Right Stick"
            KeyEvent.KEYCODE_BUTTON_START -> "Start / Menu"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "Select / View"
            KeyEvent.KEYCODE_DPAD_UP -> "D-Pad Up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "D-Pad Down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "D-Pad Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "D-Pad Right"
            else -> ""
        }
    }
}
