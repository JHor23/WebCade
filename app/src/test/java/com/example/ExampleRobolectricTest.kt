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
@Config(sdk = [34])
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

    // Re-enable and test dynamic first-party game whitelisting
    adBlockManager.setAdBlockerEnabled(true)
    adBlockManager.setCurrentGameHost("https://www.pokefableonline.com/game")
    val gameAssetUri = android.net.Uri.parse("https://cdn.pokefableonline.com/assets/sprites/charizard.png")
    val gameWsUri = android.net.Uri.parse("https://ws.pokefableonline.com/socket.io")
    assertFalse(adBlockManager.shouldBlock(gameAssetUri))
    assertFalse(adBlockManager.shouldBlock(gameWsUri))
  }

  @Test
  fun `test GameAssetCacheManager caches and clears cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetManager = com.example.browser.GameAssetCacheManager(context)
    assertNotNull(assetManager)
    assetManager.clearCache()
  }

  @Test
  fun `test GameAssetCacheManager recognizes cacheable static assets and excludes dynamic endpoints`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetManager = com.example.browser.GameAssetCacheManager(context)

    // Helper mock request
    fun createMockRequest(url: String, isMainFrame: Boolean = false, method: String = "GET"): android.webkit.WebResourceRequest {
      return object : android.webkit.WebResourceRequest {
        override fun getUrl(): android.net.Uri = android.net.Uri.parse(url)
        override fun isForMainFrame(): Boolean = isMainFrame
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = false
        override fun getMethod(): String = method
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
      }
    }

    // Static assets should be cacheable
    assertTrue(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/sprites/player.png")))
    assertTrue(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/audio/bgm.ogg")))
    assertTrue(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/engine/game.wasm")))
    assertTrue(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/maps/level1.json")))

    // Main frame, non-GET, dynamic APIs, and WebSockets should NOT be cached
    assertFalse(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/index.html", isMainFrame = true)))
    assertFalse(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/api/user/save", method = "POST")))
    assertFalse(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/auth/login")))
    assertFalse(assetManager.isCacheableGameAsset(createMockRequest("https://game.cdn.com/socket.io/?EIO=4")))
  }

  @Test
  fun `test EncryptedTunnelManager toggling, mode switching, and proxy rule generation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tunnelManager = com.example.browser.EncryptedTunnelManager(context)

    // Test default initial state
    assertNotNull(tunnelManager)
    val initialEnabled = tunnelManager.isTunnelEnabled.value

    // Test toggle
    val toggled = tunnelManager.toggleTunnel()
    assertEquals(!initialEnabled, toggled)
    assertEquals(toggled, tunnelManager.isTunnelEnabled.value)

    // Test mode switching to WARP_BRIDGE
    tunnelManager.setTunnelMode(com.example.browser.TunnelMode.WARP_BRIDGE)
    assertEquals(com.example.browser.TunnelMode.WARP_BRIDGE, tunnelManager.tunnelMode.value)
    assertEquals("socks5://127.0.0.1:40000", tunnelManager.getActiveProxyRule())

    // Test mode switching to SECURE_ENCRYPTED_DOH
    tunnelManager.setTunnelMode(com.example.browser.TunnelMode.SECURE_ENCRYPTED_DOH)
    assertEquals(com.example.browser.TunnelMode.SECURE_ENCRYPTED_DOH, tunnelManager.tunnelMode.value)
    assertEquals("https://1.1.1.1:443", tunnelManager.getActiveProxyRule())

    // Test custom endpoint configuration
    tunnelManager.updateCustomConfig("socks5", "192.168.1.100", 1080)
    tunnelManager.setTunnelMode(com.example.browser.TunnelMode.CUSTOM)
    assertEquals(com.example.browser.TunnelMode.CUSTOM, tunnelManager.tunnelMode.value)
    assertEquals("socks5://192.168.1.100:1080", tunnelManager.getActiveProxyRule())

    // Test custom HTTPS endpoint
    tunnelManager.updateCustomConfig("https", "myvpn.proxy.net", 8443)
    assertEquals("https://myvpn.proxy.net:8443", tunnelManager.getActiveProxyRule())

    // Reset back to disabled
    tunnelManager.setTunnelEnabled(false)
    assertFalse(tunnelManager.isTunnelEnabled.value)
  }

  @Test
  fun `test UrlHistoryManager resolves valid URLs directly and incorrect URLs to Google search`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val urlHistoryManager = com.example.model.UrlHistoryManager(context)

    // Valid URLs should resolve directly
    assertEquals("https://xbox.com/play", urlHistoryManager.formatUrl("xbox.com/play"))
    assertEquals("https://krunker.io", urlHistoryManager.formatUrl("  krunker.io  "))
    assertEquals("https://www.geforcenow.com", urlHistoryManager.formatUrl("HTTPS://www.geforcenow.com"))
    assertEquals("http://localhost:8080/game", urlHistoryManager.formatUrl("http://localhost:8080/game"))
    assertEquals("https://192.168.1.10:3000", urlHistoryManager.formatUrl("192.168.1.10:3000"))

    // Incorrect / malformed URLs and plain search terms should open Google search results
    assertEquals(
      "https://www.google.com/search?q=xbox+cloud+gaming",
      urlHistoryManager.formatUrl("xbox cloud gaming")
    )
    assertEquals(
      "https://www.google.com/search?q=krunker",
      urlHistoryManager.formatUrl("krunker")
    )
    assertEquals(
      "https://www.google.com/search?q=www.xbox%2Ccom",
      urlHistoryManager.formatUrl("www.xbox,com")
    )
    assertEquals(
      "https://www.google.com/search?q=htp%3A%2F%2Fxbox.com",
      urlHistoryManager.formatUrl("htp://xbox.com")
    )
    assertEquals(
      "https://www.google.com/search?q=https%3A%2F%2Fbad..url.com",
      urlHistoryManager.formatUrl("https://bad..url.com")
    )
    assertEquals(
      "https://www.google.com/search?q=xbox.c",
      urlHistoryManager.formatUrl("xbox.c")
    )
    assertEquals(
      "https://www.google.com/search?q=xbox.",
      urlHistoryManager.formatUrl("xbox.")
    )
    assertEquals(
      "https://www.google.com/search?q=123.456",
      urlHistoryManager.formatUrl("123.456")
    )
    assertEquals("", urlHistoryManager.formatUrl("   "))
  }

  @Test
  fun `test UrlHistoryManager addUrl and removeUrl manage recent history`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val urlHistoryManager = com.example.model.UrlHistoryManager(context)
    urlHistoryManager.clearHistory()

    urlHistoryManager.addUrl("https://nonexistent12345.com")
    assertTrue(urlHistoryManager.getRecentUrls().contains("https://nonexistent12345.com"))

    urlHistoryManager.removeUrl("https://nonexistent12345.com")
    assertFalse(urlHistoryManager.getRecentUrls().contains("https://nonexistent12345.com"))
  }

  @Test
  fun `test GamingWebClient identifies incorrect or unresolvable URL errors`() {
    assertTrue(
      com.example.browser.GamingWebClient.isIncorrectUrlError(
        android.webkit.WebViewClient.ERROR_HOST_LOOKUP,
        "net::ERR_NAME_NOT_RESOLVED"
      )
    )
    assertTrue(
      com.example.browser.GamingWebClient.isIncorrectUrlError(
        android.webkit.WebViewClient.ERROR_BAD_URL,
        "net::ERR_INVALID_URL"
      )
    )
    assertTrue(
      com.example.browser.GamingWebClient.isIncorrectUrlError(
        null,
        "net::ERR_NAME_RESOLUTION_FAILED"
      )
    )
    assertFalse(
      com.example.browser.GamingWebClient.isIncorrectUrlError(
        android.webkit.WebViewClient.ERROR_TIMEOUT,
        "net::ERR_CONNECTION_TIMED_OUT"
      )
    )
  }
}

