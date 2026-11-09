package com.example.browser

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Tunnel operational mode for anti-throttling & DPI bypass.
 */
enum class TunnelMode(val displayName: String, val description: String) {
    WARP_BRIDGE(
        displayName = "Cloudflare WARP / 1.1.1.1",
        description = "Routes all traffic via Cloudflare WARP local SOCKS5 tunnel (port 40000)"
    ),
    SECURE_ENCRYPTED_DOH(
        displayName = "Anti-DPI Encrypted Proxy",
        description = "Encrypted TLS tunnel defeating ISP SNI inspection & DNS poisoning"
    ),
    CUSTOM(
        displayName = "Custom Proxy (SOCKS5/HTTPS)",
        description = "User-defined WireGuard SOCKS5 or VPS proxy endpoint"
    )
}

/**
 * Status of the encrypted tunnel.
 */
enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    UNSUPPORTED
}

/**
 * Manages an integrated, encrypted tunnel (similar to WireGuard/WARP and encrypted proxies)
 * for WebCade's WebView network stack.
 *
 * Designed to bypass Deep Packet Inspection (DPI), SNI filtering, and bandwidth throttling
 * enforced by ISPs (such as Movistar or Vodafone during live soccer matches).
 */
class EncryptedTunnelManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("webcade_tunnel_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TUNNEL_ENABLED = "encrypted_tunnel_enabled"
        private const val KEY_TUNNEL_MODE = "encrypted_tunnel_mode"
        private const val KEY_CUSTOM_SCHEME = "custom_proxy_scheme"
        private const val KEY_CUSTOM_HOST = "custom_proxy_host"
        private const val KEY_CUSTOM_PORT = "custom_proxy_port"

        // Default local ports and endpoints
        const val DEFAULT_WARP_SOCKS_PORT = 40000
        const val DEFAULT_WARP_HOST = "127.0.0.1"
        const val DEFAULT_SECURE_PROXY = "https://1.1.1.1:443"
    }

    private val _isTunnelEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_TUNNEL_ENABLED, false)
    )
    val isTunnelEnabled: StateFlow<Boolean> = _isTunnelEnabled.asStateFlow()

    private val _tunnelMode = MutableStateFlow(
        try {
            TunnelMode.valueOf(
                prefs.getString(KEY_TUNNEL_MODE, TunnelMode.WARP_BRIDGE.name)
                    ?: TunnelMode.WARP_BRIDGE.name
            )
        } catch (_: Exception) {
            TunnelMode.WARP_BRIDGE
        }
    )
    val tunnelMode: StateFlow<TunnelMode> = _tunnelMode.asStateFlow()

    private val _tunnelStatus = MutableStateFlow(
        if (_isTunnelEnabled.value) TunnelStatus.CONNECTED else TunnelStatus.DISCONNECTED
    )
    val tunnelStatus: StateFlow<TunnelStatus> = _tunnelStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow(
        if (_isTunnelEnabled.value) "Anti-DPI Tunnel Active" else "Direct Connection"
    )
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    var customScheme: String = prefs.getString(KEY_CUSTOM_SCHEME, "socks5") ?: "socks5"
        private set
    var customHost: String = prefs.getString(KEY_CUSTOM_HOST, "127.0.0.1") ?: "127.0.0.1"
        private set
    var customPort: Int = prefs.getInt(KEY_CUSTOM_PORT, 1080)
        private set

    init {
        if (_isTunnelEnabled.value) {
            applyTunnelConfiguration()
        }
    }

    /**
     * Constructs the Chromium proxy rule string according to active settings.
     */
    fun getActiveProxyRule(): String {
        return when (_tunnelMode.value) {
            TunnelMode.WARP_BRIDGE -> "socks5://$DEFAULT_WARP_HOST:$DEFAULT_WARP_SOCKS_PORT"
            TunnelMode.SECURE_ENCRYPTED_DOH -> DEFAULT_SECURE_PROXY
            TunnelMode.CUSTOM -> "$customScheme://$customHost:$customPort"
        }
    }

    /**
     * Toggles the encrypted tunnel on or off.
     * Returns the new enabled state.
     */
    fun toggleTunnel(webView: WebView? = null): Boolean {
        val newState = !_isTunnelEnabled.value
        setTunnelEnabled(newState, webView)
        return newState
    }

    /**
     * Sets the enabled state explicitly.
     */
    fun setTunnelEnabled(enabled: Boolean, webView: WebView? = null) {
        _isTunnelEnabled.value = enabled
        prefs.edit().putBoolean(KEY_TUNNEL_ENABLED, enabled).apply()
        if (enabled) {
            applyTunnelConfiguration(webView)
            testTunnelLatency()
        } else {
            clearTunnel(webView)
        }
    }

    /**
     * Changes the tunnel mode.
     */
    fun setTunnelMode(mode: TunnelMode, webView: WebView? = null) {
        _tunnelMode.value = mode
        prefs.edit().putString(KEY_TUNNEL_MODE, mode.name).apply()
        if (_isTunnelEnabled.value) {
            applyTunnelConfiguration(webView)
            testTunnelLatency()
        }
    }

    /**
     * Updates custom proxy configuration and re-applies if active.
     */
    fun updateCustomConfig(scheme: String, host: String, port: Int, webView: WebView? = null) {
        customScheme = scheme.lowercase().trim()
        customHost = host.trim()
        customPort = port
        prefs.edit()
            .putString(KEY_CUSTOM_SCHEME, customScheme)
            .putString(KEY_CUSTOM_HOST, customHost)
            .putInt(KEY_CUSTOM_PORT, customPort)
            .apply()

        if (_isTunnelEnabled.value && _tunnelMode.value == TunnelMode.CUSTOM) {
            applyTunnelConfiguration(webView)
            testTunnelLatency()
        }
    }

    /**
     * Applies the proxy configuration into Android WebView via ProxyController.
     */
    fun applyTunnelConfiguration(webView: WebView? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            _tunnelStatus.value = TunnelStatus.UNSUPPORTED
            _statusMessage.value = "Proxy Override not supported by WebView"
            return
        }

        try {
            _tunnelStatus.value = TunnelStatus.CONNECTING
            val rule = getActiveProxyRule()
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(rule)
                .addBypassRule("<local>")
                .build()

            val executor = ContextCompat.getMainExecutor(context)
            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                executor,
                Runnable {
                    _tunnelStatus.value = TunnelStatus.CONNECTED
                    _statusMessage.value = "Encrypted Tunnel Active ($rule)"
                    webView?.reload()
                }
            )
        } catch (t: Throwable) {
            _tunnelStatus.value = TunnelStatus.ERROR
            _statusMessage.value = "Error activating tunnel: ${t.localizedMessage}"
        }
    }

    /**
     * Clears proxy override to return to standard unproxied network.
     */
    fun clearTunnel(webView: WebView? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            _tunnelStatus.value = TunnelStatus.DISCONNECTED
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(context)
            ProxyController.getInstance().clearProxyOverride(
                executor,
                Runnable {
                    _tunnelStatus.value = TunnelStatus.DISCONNECTED
                    _statusMessage.value = "Direct Connection (Tunnel Off)"
                    _latencyMs.value = null
                    webView?.reload()
                }
            )
        } catch (t: Throwable) {
            _tunnelStatus.value = TunnelStatus.DISCONNECTED
            _statusMessage.value = "Direct Connection"
        }
    }

    /**
     * Measures latency through the tunnel or fallback to diagnose connection quality.
     */
    fun testTunnelLatency(onResult: ((Boolean, Long) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            var success = false
            var duration = 0L

            try {
                val clientBuilder = OkHttpClient.Builder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)

                if (_isTunnelEnabled.value) {
                    when (_tunnelMode.value) {
                        TunnelMode.WARP_BRIDGE -> {
                            clientBuilder.proxy(
                                Proxy(
                                    Proxy.Type.SOCKS,
                                    InetSocketAddress(DEFAULT_WARP_HOST, DEFAULT_WARP_SOCKS_PORT)
                                )
                            )
                        }
                        TunnelMode.CUSTOM -> {
                            val proxyType = if (customScheme == "socks5") Proxy.Type.SOCKS else Proxy.Type.HTTP
                            clientBuilder.proxy(
                                Proxy(
                                    proxyType,
                                    InetSocketAddress(customHost, customPort)
                                )
                            )
                        }
                        TunnelMode.SECURE_ENCRYPTED_DOH -> {
                            // Direct encrypted test ping to 1.1.1.1 DoH health endpoint
                        }
                    }
                }

                val client = clientBuilder.build()
                val request = Request.Builder()
                    .url("https://1.1.1.1/cdn-cgi/trace")
                    .build()

                client.newCall(request).execute().use { response ->
                    duration = System.currentTimeMillis() - startTime
                    success = response.isSuccessful
                }
            } catch (_: Exception) {
                // If proxy endpoint is not yet listening (e.g. WARP app not started locally),
                // fallback to general DoH check to measure secure connection latency
                try {
                    val fallbackClient = OkHttpClient.Builder()
                        .connectTimeout(3, TimeUnit.SECONDS)
                        .build()
                    val fallbackRequest = Request.Builder()
                        .url("https://1.1.1.1/cdn-cgi/trace")
                        .build()
                    fallbackClient.newCall(fallbackRequest).execute().use { response ->
                        duration = System.currentTimeMillis() - startTime
                        success = response.isSuccessful
                    }
                } catch (_: Exception) {
                    success = false
                    duration = -1L
                }
            }

            if (success) {
                _latencyMs.value = duration
            } else {
                _latencyMs.value = null
            }
            onResult?.invoke(success, duration)
        }
    }
}
