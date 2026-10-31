package com.example.gamepad

import android.content.Context
import android.content.SharedPreferences

enum class ControllerMode(val label: String, val description: String) {
    DEDICATED_GAMEPAD(
        label = "Dedicated Gamepad",
        description = "Native W3C Gamepad API for Xbox Cloud Gaming, GeForce NOW & Luna"
    ),
    KEYBOARD_MOUSE_EMULATION(
        label = "Keyboard & Mouse Emulation",
        description = "Emulates WASD keys and mouse aim/click for browser & HTML5 games"
    )
}

enum class StickEmulation(val label: String) {
    WASD("WASD Keys (Movement)"),
    ARROW_KEYS("Arrow Keys"),
    MOUSE_AIM("Mouse Aim / Look"),
    DISABLED("Disabled")
}

enum class EmulatedKey(val label: String, val code: String, val key: String, val keyCode: Int, val isMouse: Boolean = false, val mouseButton: Int = 0) {
    // Mouse actions
    MOUSE_LEFT_CLICK("Left Mouse Click (Fire)", "MouseLeft", "MouseLeft", 0, isMouse = true, mouseButton = 0),
    MOUSE_RIGHT_CLICK("Right Mouse Click (Aim)", "MouseRight", "MouseRight", 0, isMouse = true, mouseButton = 2),
    MOUSE_MIDDLE_CLICK("Middle Click", "MouseMiddle", "MouseMiddle", 0, isMouse = true, mouseButton = 1),

    // Common Gaming Keys
    KEY_SPACE("Spacebar (Jump)", "Space", " ", 32),
    KEY_SHIFT("Shift (Sprint / Crouch)", "ShiftLeft", "Shift", 16),
    KEY_CTRL("Control (Crouch)", "ControlLeft", "Control", 17),
    KEY_ENTER("Enter / Return", "Enter", "Enter", 13),
    KEY_ESCAPE("Escape (Pause / Menu)", "Escape", "Escape", 27),
    KEY_TAB("Tab (Scoreboard / Map)", "Tab", "Tab", 9),
    KEY_E("E Key (Interact / Use)", "KeyE", "e", 69),
    KEY_R("R Key (Reload)", "KeyR", "r", 82),
    KEY_F("F Key (Use / Flashlight)", "KeyF", "f", 70),
    KEY_Q("Q Key (Ability / Swap)", "KeyQ", "q", 81),
    KEY_C("C Key (Crouch / Slide)", "KeyC", "c", 67),
    KEY_V("V Key (Melee)", "KeyV", "v", 86),
    KEY_1("1 Key (Primary Weapon)", "Digit1", "1", 49),
    KEY_2("2 Key (Secondary Weapon)", "Digit2", "2", 50),
    KEY_3("3 Key (Slot 3)", "Digit3", "3", 51),
    KEY_4("4 Key (Slot 4)", "Digit4", "4", 52),
    KEY_W("W Key", "KeyW", "w", 87),
    KEY_A("A Key", "KeyA", "a", 65),
    KEY_S("S Key", "KeyS", "s", 83),
    KEY_D("D Key", "KeyD", "d", 68),
    KEY_UP("Arrow Up", "ArrowUp", "ArrowUp", 38),
    KEY_DOWN("Arrow Down", "ArrowDown", "ArrowDown", 40),
    KEY_LEFT("Arrow Left", "ArrowLeft", "ArrowLeft", 37),
    KEY_RIGHT("Arrow Right", "ArrowRight", "ArrowRight", 39),
    NONE("None (Unmapped)", "None", "", 0)
}

data class ControllerMapping(
    val buttonA: EmulatedKey = EmulatedKey.KEY_SPACE,
    val buttonB: EmulatedKey = EmulatedKey.KEY_SHIFT,
    val buttonX: EmulatedKey = EmulatedKey.KEY_R,
    val buttonY: EmulatedKey = EmulatedKey.KEY_E,
    val buttonLB: EmulatedKey = EmulatedKey.KEY_Q,
    val buttonRB: EmulatedKey = EmulatedKey.KEY_F,
    val buttonLT: EmulatedKey = EmulatedKey.MOUSE_RIGHT_CLICK,
    val buttonRT: EmulatedKey = EmulatedKey.MOUSE_LEFT_CLICK,
    val buttonStart: EmulatedKey = EmulatedKey.KEY_ESCAPE,
    val buttonSelect: EmulatedKey = EmulatedKey.KEY_TAB,
    val buttonL3: EmulatedKey = EmulatedKey.KEY_SHIFT,
    val buttonR3: EmulatedKey = EmulatedKey.KEY_C,
    val dpadUp: EmulatedKey = EmulatedKey.KEY_1,
    val dpadDown: EmulatedKey = EmulatedKey.KEY_2,
    val dpadLeft: EmulatedKey = EmulatedKey.KEY_3,
    val dpadRight: EmulatedKey = EmulatedKey.KEY_4,
    val leftStick: StickEmulation = StickEmulation.WASD,
    val rightStick: StickEmulation = StickEmulation.MOUSE_AIM,
    val mouseSensitivity: Float = 1.0f,
    val deadzone: Float = 0.15f,
    val showVirtualCursor: Boolean = true
)

class GamepadConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("webcade_gamepad_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MODE = "controller_mode"
        private const val KEY_SENSITIVITY = "mouse_sensitivity"
        private const val KEY_DEADZONE = "deadzone"
        private const val KEY_SHOW_CURSOR = "show_virtual_cursor"

        private const val KEY_BTN_A = "btn_a"
        private const val KEY_BTN_B = "btn_b"
        private const val KEY_BTN_X = "btn_x"
        private const val KEY_BTN_Y = "btn_y"
        private const val KEY_BTN_LB = "btn_lb"
        private const val KEY_BTN_RB = "btn_rb"
        private const val KEY_BTN_LT = "btn_lt"
        private const val KEY_BTN_RT = "btn_rt"
        private const val KEY_BTN_START = "btn_start"
        private const val KEY_BTN_SELECT = "btn_select"
        private const val KEY_BTN_L3 = "btn_l3"
        private const val KEY_BTN_R3 = "btn_r3"
        private const val KEY_DPAD_UP = "dpad_up"
        private const val KEY_DPAD_DOWN = "dpad_down"
        private const val KEY_DPAD_LEFT = "dpad_left"
        private const val KEY_DPAD_RIGHT = "dpad_right"
        private const val KEY_LEFT_STICK = "left_stick"
        private const val KEY_RIGHT_STICK = "right_stick"
    }

    private fun getSavedKey(keyName: String, default: EmulatedKey): EmulatedKey {
        val str = prefs.getString(keyName, default.name) ?: return default
        return try {
            EmulatedKey.valueOf(str)
        } catch (_: Exception) {
            default
        }
    }

    private fun getSavedStick(keyName: String, default: StickEmulation): StickEmulation {
        val str = prefs.getString(keyName, default.name) ?: return default
        return try {
            StickEmulation.valueOf(str)
        } catch (_: Exception) {
            default
        }
    }

    fun getControllerMode(): ControllerMode {
        val name = prefs.getString(KEY_MODE, ControllerMode.DEDICATED_GAMEPAD.name)
        return try {
            ControllerMode.valueOf(name ?: ControllerMode.DEDICATED_GAMEPAD.name)
        } catch (_: Exception) {
            ControllerMode.DEDICATED_GAMEPAD
        }
    }

    fun setControllerMode(mode: ControllerMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getMapping(): ControllerMapping {
        val defaultMapping = ControllerMapping()
        return ControllerMapping(
            buttonA = getSavedKey(KEY_BTN_A, defaultMapping.buttonA),
            buttonB = getSavedKey(KEY_BTN_B, defaultMapping.buttonB),
            buttonX = getSavedKey(KEY_BTN_X, defaultMapping.buttonX),
            buttonY = getSavedKey(KEY_BTN_Y, defaultMapping.buttonY),
            buttonLB = getSavedKey(KEY_BTN_LB, defaultMapping.buttonLB),
            buttonRB = getSavedKey(KEY_BTN_RB, defaultMapping.buttonRB),
            buttonLT = getSavedKey(KEY_BTN_LT, defaultMapping.buttonLT),
            buttonRT = getSavedKey(KEY_BTN_RT, defaultMapping.buttonRT),
            buttonStart = getSavedKey(KEY_BTN_START, defaultMapping.buttonStart),
            buttonSelect = getSavedKey(KEY_BTN_SELECT, defaultMapping.buttonSelect),
            buttonL3 = getSavedKey(KEY_BTN_L3, defaultMapping.buttonL3),
            buttonR3 = getSavedKey(KEY_BTN_R3, defaultMapping.buttonR3),
            dpadUp = getSavedKey(KEY_DPAD_UP, defaultMapping.dpadUp),
            dpadDown = getSavedKey(KEY_DPAD_DOWN, defaultMapping.dpadDown),
            dpadLeft = getSavedKey(KEY_DPAD_LEFT, defaultMapping.dpadLeft),
            dpadRight = getSavedKey(KEY_DPAD_RIGHT, defaultMapping.dpadRight),
            leftStick = getSavedStick(KEY_LEFT_STICK, defaultMapping.leftStick),
            rightStick = getSavedStick(KEY_RIGHT_STICK, defaultMapping.rightStick),
            mouseSensitivity = prefs.getFloat(KEY_SENSITIVITY, defaultMapping.mouseSensitivity),
            deadzone = prefs.getFloat(KEY_DEADZONE, defaultMapping.deadzone),
            showVirtualCursor = prefs.getBoolean(KEY_SHOW_CURSOR, defaultMapping.showVirtualCursor)
        )
    }

    fun saveMapping(mapping: ControllerMapping) {
        prefs.edit()
            .putString(KEY_BTN_A, mapping.buttonA.name)
            .putString(KEY_BTN_B, mapping.buttonB.name)
            .putString(KEY_BTN_X, mapping.buttonX.name)
            .putString(KEY_BTN_Y, mapping.buttonY.name)
            .putString(KEY_BTN_LB, mapping.buttonLB.name)
            .putString(KEY_BTN_RB, mapping.buttonRB.name)
            .putString(KEY_BTN_LT, mapping.buttonLT.name)
            .putString(KEY_BTN_RT, mapping.buttonRT.name)
            .putString(KEY_BTN_START, mapping.buttonStart.name)
            .putString(KEY_BTN_SELECT, mapping.buttonSelect.name)
            .putString(KEY_BTN_L3, mapping.buttonL3.name)
            .putString(KEY_BTN_R3, mapping.buttonR3.name)
            .putString(KEY_DPAD_UP, mapping.dpadUp.name)
            .putString(KEY_DPAD_DOWN, mapping.dpadDown.name)
            .putString(KEY_DPAD_LEFT, mapping.dpadLeft.name)
            .putString(KEY_DPAD_RIGHT, mapping.dpadRight.name)
            .putString(KEY_LEFT_STICK, mapping.leftStick.name)
            .putString(KEY_RIGHT_STICK, mapping.rightStick.name)
            .putFloat(KEY_SENSITIVITY, mapping.mouseSensitivity)
            .putFloat(KEY_DEADZONE, mapping.deadzone)
            .putBoolean(KEY_SHOW_CURSOR, mapping.showVirtualCursor)
            .apply()
    }
}
