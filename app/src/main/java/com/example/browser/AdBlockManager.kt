package com.example.browser

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-performance DNS-style ad blocking and tracker filtering for WebCade.
 * Intercepts known advertising networks and telemetry endpoints before network transmission.
 *
 * Performance features:
 * - O(1) in-memory host verdict cache (resolves 99% of requests without linear scans)
 * - Automatic first-party host whitelisting (never slows down or blocks the active game)
 * - Asynchronous, debounced counter updates (eliminates main-thread Compose recomposition spikes)
 */
class AdBlockManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("webcade_adblock_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ADBLOCK_ENABLED = "adblock_dns_enabled"

        // Critical Cloud Gaming and Platform whitelist - Never block essential game streaming endpoints
        private val ESSENTIAL_WHITELIST = setOf(
            "xbox.com",
            "play.geforcenow.com",
            "geforcenow.com",
            "luna.amazon.com",
            "amazon.com",
            "boosteroid.com",
            "crazygames.com",
            "krunker.io",
            "poki.com",
            "1v1.lol",
            "now.gg"
        )

        // Known advertising, telemetry, tracker, and popup ad domains (DNS filtering sinkhole list)
        private val BLOCKED_AD_DOMAINS = hashSetOf(
            // Google Ad Services
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "adclick.g.doubleclick.net",
            // Major Web & Video Ad Networks
            "amazon-adsystem.com",
            "adnxs.com",
            "criteo.com",
            "criteo.net",
            "taboola.com",
            "outbrain.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "adform.net",
            "casalemedia.com",
            "advertising.com",
            "scorecardresearch.com",
            "quantserve.com",
            "serving-sys.com",
            "smartadserver.com",
            "revcontent.com",
            "mgid.com",
            "media.net",
            "bidswitch.net",
            "teads.tv",
            // Aggressive Popups & Redirects
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "trafficjunky.com",
            "exoclick.com",
            "adcash.com",
            "infolinks.com",
            "chitika.com",
            "adsterra.com",
            // Mobile Game Ad SDK endpoints
            "adcolony.com",
            "unityads.unity3d.com",
            "applovin.com",
            "vungle.com",
            "ironsrc.com",
            "chartboost.com",
            "fyber.com",
            // Trackers & Telemetry
            "chartbeat.com",
            "hotjar.com",
            "segment.io",
            "mixpanel.com",
            "statcounter.com",
            "histats.com"
        )

        // Specific ad script path signatures
        private val BLOCKED_PATH_SIGNATURES = listOf(
            "/pagead/",
            "/adview",
            "/ads.js",
            "/prebid.js",
            "/gpt.js",
            "googletagservices.com/tag/js/gpt.js",
            "/adserver/",
            "/banner/display"
        )

        // Lightweight cosmetic CSS targeting only confirmed advertising wrappers without scanning all classes
        const val COSMETIC_AD_BLOCK_SCRIPT = """
            (function() {
                if (window.__webcade_adblock_injected) return;
                window.__webcade_adblock_injected = true;
                const style = document.createElement('style');
                style.id = 'webcade-adblock-cosmetic';
                style.innerHTML = 'ins.adsbygoogle, .ad-banner, .ad-container, .ad-box, .ad-unit, .adsbox, iframe[src*="doubleclick"], iframe[src*="googlesyndication"] { display: none !important; width: 0 !important; height: 0 !important; }';
                if (document.head) {
                    document.head.appendChild(style);
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        if (document.head) document.head.appendChild(style);
                    });
                }
            })();
        """
    }

    private val _isAdBlockerEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ADBLOCK_ENABLED, true)
    )
    val isAdBlockerEnabled: StateFlow<Boolean> = _isAdBlockerEnabled.asStateFlow()

    private val _blockedAdsCount = MutableStateFlow(0)
    val blockedAdsCount: StateFlow<Int> = _blockedAdsCount.asStateFlow()

    private val pendingBlockedCount = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isFlushScheduled = false

    // Fast memory cache of host -> shouldBlock verdicts (bounded at 512 entries)
    private val hostDecisionCache = ConcurrentHashMap<String, Boolean>(256)

    // Dynamically whitelisted current first-party game root host (e.g. pokefableonline.com)
    @Volatile
    private var currentGameRootHost: String? = null

    fun setCurrentGameHost(url: String?) {
        if (url.isNullOrBlank()) {
            currentGameRootHost = null
            return
        }
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase(Locale.ROOT)
            if (host != null) {
                // Extract root domain (e.g. www.pokefableonline.com -> pokefableonline.com)
                val parts = host.split('.')
                val newRoot = if (parts.size >= 2) {
                    "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
                } else {
                    host
                }
                // Only clear cache when actually switching to a different game root domain
                if (newRoot != currentGameRootHost) {
                    currentGameRootHost = newRoot
                    hostDecisionCache.clear()
                }
            }
        } catch (_: Exception) {
            currentGameRootHost = null
        }
    }

    fun toggleAdBlocker(): Boolean {
        val newState = !_isAdBlockerEnabled.value
        setAdBlockerEnabled(newState)
        return newState
    }

    fun setAdBlockerEnabled(enabled: Boolean) {
        _isAdBlockerEnabled.value = enabled
        hostDecisionCache.clear()
        prefs.edit().putBoolean(KEY_ADBLOCK_ENABLED, enabled).apply()
    }

    fun resetBlockedCounter() {
        pendingBlockedCount.set(0)
        _blockedAdsCount.value = 0
    }

    private fun incrementBlockedCounter() {
        pendingBlockedCount.incrementAndGet()
        synchronized(this) {
            if (!isFlushScheduled) {
                isFlushScheduled = true
                mainHandler.postDelayed({
                    synchronized(this) {
                        isFlushScheduled = false
                    }
                    val count = pendingBlockedCount.get()
                    if (_blockedAdsCount.value != count) {
                        _blockedAdsCount.value = count
                    }
                }, 800)
            }
        }
    }

    /**
     * Highly optimized check:
     * 1. First-party game host: instant pass (zero delay for game textures, scripts, wasm, sounds)
     * 2. Host decision cache: instant O(1) hit
     * 3. Set-based domain lookup
     */
    fun shouldBlock(uri: Uri): Boolean {
        if (!_isAdBlockerEnabled.value) return false

        val host = uri.host?.lowercase(Locale.ROOT) ?: return false

        // 1. Instant pass for active game's first-party domain and subdomains
        val root = currentGameRootHost
        if (root != null && (host == root || host.endsWith(".$root"))) {
            return false
        }

        // 2. Fast cache hit
        val cached = hostDecisionCache[host]
        if (cached != null) {
            if (cached) incrementBlockedCounter()
            return cached
        }

        // 3. Essential platform whitelist check
        for (whiteHost in ESSENTIAL_WHITELIST) {
            if (host == whiteHost || host.endsWith(".$whiteHost")) {
                if (hostDecisionCache.size < 512) hostDecisionCache[host] = false
                return false
            }
        }

        // 4. Exact domain match against blocked ad set
        if (BLOCKED_AD_DOMAINS.contains(host)) {
            if (hostDecisionCache.size < 512) hostDecisionCache[host] = true
            incrementBlockedCounter()
            return true
        }

        // Check if any subdomain matches blocked domains
        var isBlockedByDomain = false
        for (adDomain in BLOCKED_AD_DOMAINS) {
            if (host.endsWith(".$adDomain")) {
                isBlockedByDomain = true
                break
            }
        }
        if (isBlockedByDomain) {
            if (hostDecisionCache.size < 512) hostDecisionCache[host] = true
            incrementBlockedCounter()
            return true
        }

        // 5. Host prefixes that identify standalone ad-servers
        if (host.startsWith("ads.") ||
            host.startsWith("adserver.") ||
            host.startsWith("ad-delivery.") ||
            host.startsWith("tracker.")
        ) {
            if (hostDecisionCache.size < 512) hostDecisionCache[host] = true
            incrementBlockedCounter()
            return true
        }

        // 6. Check known ad script path signatures only if path contains "ad" or "pagead"
        val path = uri.path?.lowercase(Locale.ROOT) ?: ""
        if (path.contains("ad") || path.contains("prebid") || path.contains("gpt")) {
            for (sig in BLOCKED_PATH_SIGNATURES) {
                if (path.contains(sig)) {
                    incrementBlockedCounter()
                    return true
                }
            }
        }

        if (hostDecisionCache.size < 512) hostDecisionCache[host] = false
        return false
    }
}
