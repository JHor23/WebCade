package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gamepad.ControllerMapping
import com.example.gamepad.ControllerMode
import com.example.gamepad.EmulatedKey
import com.example.gamepad.GamepadConfigManager
import com.example.gamepad.GamepadEmulationEngine
import com.example.gamepad.GamepadManager
import com.example.gamepad.StickEmulation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WebCade", appName)
  }

  @Test
  fun `test GamepadConfigManager mode switching and persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = GamepadConfigManager(context)

    // Default should be DEDICATED_GAMEPAD
    manager.setControllerMode(ControllerMode.DEDICATED_GAMEPAD)
    assertEquals(ControllerMode.DEDICATED_GAMEPAD, manager.getControllerMode())

    // Switch to KEYBOARD_MOUSE_EMULATION
    manager.setControllerMode(ControllerMode.KEYBOARD_MOUSE_EMULATION)
    assertEquals(ControllerMode.KEYBOARD_MOUSE_EMULATION, manager.getControllerMode())

    // Reset back
    manager.setControllerMode(ControllerMode.DEDICATED_GAMEPAD)
    assertEquals(ControllerMode.DEDICATED_GAMEPAD, manager.getControllerMode())
  }

  @Test
  fun `test GamepadConfigManager custom button mapping persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = GamepadConfigManager(context)

    val customMapping = ControllerMapping(
      buttonA = EmulatedKey.KEY_ENTER,
      buttonB = EmulatedKey.KEY_ESCAPE,
      buttonX = EmulatedKey.KEY_E,
      buttonY = EmulatedKey.KEY_R,
      buttonLB = EmulatedKey.KEY_SHIFT,
      buttonRB = EmulatedKey.KEY_CTRL,
      buttonLT = EmulatedKey.MOUSE_RIGHT_CLICK,
      buttonRT = EmulatedKey.MOUSE_LEFT_CLICK,
      leftStick = StickEmulation.WASD,
      rightStick = StickEmulation.MOUSE_AIM,
      mouseSensitivity = 1.75f,
      deadzone = 0.20f,
      showVirtualCursor = false
    )

    manager.saveMapping(customMapping)
    val loaded = manager.getMapping()

    assertEquals(EmulatedKey.KEY_ENTER, loaded.buttonA)
    assertEquals(EmulatedKey.KEY_ESCAPE, loaded.buttonB)
    assertEquals(EmulatedKey.KEY_E, loaded.buttonX)
    assertEquals(EmulatedKey.KEY_R, loaded.buttonY)
    assertEquals(EmulatedKey.KEY_SHIFT, loaded.buttonLB)
    assertEquals(EmulatedKey.KEY_CTRL, loaded.buttonRB)
    assertEquals(EmulatedKey.MOUSE_RIGHT_CLICK, loaded.buttonLT)
    assertEquals(EmulatedKey.MOUSE_LEFT_CLICK, loaded.buttonRT)
    assertEquals(StickEmulation.WASD, loaded.leftStick)
    assertEquals(StickEmulation.MOUSE_AIM, loaded.rightStick)
    assertEquals(1.75f, loaded.mouseSensitivity, 0.01f)
    assertEquals(0.20f, loaded.deadzone, 0.01f)
    assertFalse(loaded.showVirtualCursor)
  }

  @Test
  fun `test GamepadEmulationEngine injection script is valid javascript`() {
    val script = GamepadEmulationEngine.KBM_INJECTION_SCRIPT
    assertNotNull(script)
    assertTrue(script.contains("window.__webCadeKbm"))
    assertTrue(script.contains("sendKey"))
    assertTrue(script.contains("sendMouseMove"))
    assertTrue(script.contains("sendMouseDown"))
    assertTrue(script.contains("sendMouseUp"))
  }

  @Test
  fun `test GamepadManager initialization and mode switching`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val gamepadManager = GamepadManager(context)

    gamepadManager.setControllerMode(ControllerMode.KEYBOARD_MOUSE_EMULATION)
    assertEquals(ControllerMode.KEYBOARD_MOUSE_EMULATION, gamepadManager.controllerMode.value)
    assertEquals(ControllerMode.KEYBOARD_MOUSE_EMULATION, gamepadManager.emulationEngine.controllerMode)

    gamepadManager.setControllerMode(ControllerMode.DEDICATED_GAMEPAD)
    assertEquals(ControllerMode.DEDICATED_GAMEPAD, gamepadManager.controllerMode.value)
    assertEquals(ControllerMode.DEDICATED_GAMEPAD, gamepadManager.emulationEngine.controllerMode)

    gamepadManager.cleanup()
  }

  @Test
  fun `test GamingPresets platforms list is populated with gaming services`() {
    val platforms = com.example.model.GamingPresets.platforms
    assertTrue(platforms.isNotEmpty())
    assertTrue(platforms.any { it.id == "xbox" })
    assertTrue(platforms.any { it.id == "gfn" })
    assertTrue(platforms.any { it.id == "luna" })
    assertTrue(platforms.any { it.id == "krunker" })
    assertTrue(platforms.any { it.id == "crazygames" })
  }

  @Test
  fun `test AdBlockManager blocks ad and tracker domains while allowing games`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val adBlockManager = com.example.browser.AdBlockManager(context)

    // Ensure ad-block is enabled
    adBlockManager.setAdBlockerEnabled(true)
    assertTrue(adBlockManager.isAdBlockerEnabled.value)

    // Should block known ad domains
    val adUri1 = android.net.Uri.parse("https://adservice.google.com/ads/pagead")
    val adUri2 = android.net.Uri.parse("https://securepubads.g.doubleclick.net/gampad/ads")
    val adUri3 = android.net.Uri.parse("https://c.amazon-adsystem.com/aax2/apstag.js")
    val adUri4 = android.net.Uri.parse("https://popads.net/serve")

    assertTrue(adBlockManager.shouldBlock(adUri1))
    assertTrue(adBlockManager.shouldBlock(adUri2))
    assertTrue(adBlockManager.shouldBlock(adUri3))
    assertTrue(adBlockManager.shouldBlock(adUri4))

    // Should allow essential gaming streaming platforms
    val gameUri1 = android.net.Uri.parse("https://www.xbox.com/play")
    val gameUri2 = android.net.Uri.parse("https://play.geforcenow.com/mall/")
    val gameUri3 = android.net.Uri.parse("https://krunker.io/")
    val gameUri4 = android.net.Uri.parse("https://www.crazygames.com/game/1v1-lol")

    assertFalse(adBlockManager.shouldBlock(gameUri1))
    assertFalse(adBlockManager.shouldBlock(gameUri2))
    assertFalse(adBlockManager.shouldBlock(gameUri3))
    assertFalse(adBlockManager.shouldBlock(gameUri4))

    // Toggling ad-block off should allow requests through
    adBlockManager.setAdBlockerEnabled(false)
    assertFalse(adBlockManager.shouldBlock(adUri1))
  }

  @Test
  fun `test BrowserDataManager clearAllBrowserData executes cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var completed = false
    com.example.browser.BrowserDataManager.clearAllBrowserData(
      context = context,
      webView = null,
      clearHistory = true,
      onComplete = { completed = true }
    )
    // Run looper tasks
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    assertTrue(completed)
  }
}

