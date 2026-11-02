package com.example.browser

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages DNS-style ad blocking and tracker filtering for WebCade.
 * Intercepts known advertising networks, telemetry endpoints, and intrusive pop-up scripts
 * before network transmission, reducing latency and eliminating game interruptions.
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
        private val BLOCKED_AD_DOMAINS = setOf(
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

        // Ad script path signatures
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

        // Cosmetic script to hide leftover blank ad boxes without breaking game layouts
        const val COSMETIC_AD_BLOCK_SCRIPT = """
            (function() {
                if (window.__webcade_adblock_injected) return;
                window.__webcade_adblock_injected = true;
                const style = document.createElement('style');
                style.id = 'webcade-adblock-cosmetic';
                style.innerHTML = 'ins.adsbygoogle, [class*="ad-banner"], [class*="ad-container"], [id*="ad-container"], [id^="google_ads_"], iframe[src*="doubleclick"], iframe[src*="googlesyndication"] { display: none !important; width: 0 !important; height: 0 !important; }';
                if (document.head) {
                    document.head.appendChild(style);
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        document.head.appendChild(style);
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

    fun toggleAdBlocker(): Boolean {
        val newState = !_isAdBlockerEnabled.value
        setAdBlockerEnabled(newState)
        return newState
    }

    fun setAdBlockerEnabled(enabled: Boolean) {
        _isAdBlockerEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ADBLOCK_ENABLED, enabled).apply()
    }

    fun resetBlockedCounter() {
        _blockedAdsCount.value = 0
    }

    /**
     * Inspects a requested URL against DNS ad filter lists and path signatures.
     * Returns true if the request should be blocked.
     */
    fun shouldBlock(uri: Uri): Boolean {
        if (!_isAdBlockerEnabled.value) return false

        val host = uri.host?.lowercase(Locale.ROOT) ?: return false

        // Check if host belongs to an essential cloud gaming platform
        for (whiteHost in ESSENTIAL_WHITELIST) {
            if (host == whiteHost || host.endsWith(".$whiteHost")) {
                return false
            }
        }

        // 1. Direct host or subdomain match against ad-blocking DNS list
        for (adDomain in BLOCKED_AD_DOMAINS) {
            if (host == adDomain || host.endsWith(".$adDomain")) {
                _blockedAdsCount.value++
                return true
            }
        }

        // 2. Host prefixes that identify standalone ad-servers (e.g. ads.example.com, adserver.net)
        if (host.startsWith("ads.") ||
            host.startsWith("adserver.") ||
            host.startsWith("ad-delivery.") ||
            host.startsWith("tracker.")
        ) {
            _blockedAdsCount.value++
            return true
        }

        // 3. Known ad script paths
        val fullUrl = uri.toString().lowercase(Locale.ROOT)
        for (sig in BLOCKED_PATH_SIGNATURES) {
            if (fullUrl.contains(sig)) {
                _blockedAdsCount.value++
                return true
            }
        }

        return false
    }
}
