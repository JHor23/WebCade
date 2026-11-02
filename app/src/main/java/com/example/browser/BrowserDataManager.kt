package com.example.browser

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.model.UrlHistoryManager

/**
 * Handles clearing all cache, cookies, DOM storage, and browser data for WebCade.
 */
object BrowserDataManager {

    /**
     * Clears all cached web files, HTTP cookies, DOM storage, form data, and SSL caches.
     * Optionally clears local URL history.
     */
    fun clearAllBrowserData(
        context: Context,
        webView: WebView? = null,
        clearHistory: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        // 1. Clear WebView cache (disk & memory)
        webView?.post {
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            webView.clearSslPreferences()
        }

        // 2. Clear all Cookies (session & persistent)
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.removeSessionCookies {
                    cookieManager.flush()
                }
            }
        } catch (_: Exception) {}

        // 3. Clear HTML5 DOM Web Storage & Databases
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (_: Exception) {}

        // 4. Clear Geolocation permissions
        try {
            GeolocationPermissions.getInstance().clearAll()
        } catch (_: Exception) {}

        // 5. Clean local disk cache directories
        try {
            context.cacheDir.deleteRecursively()
            context.codeCacheDir.deleteRecursively()
        } catch (_: Exception) {}

        // 6. Optional URL history wipe
        if (clearHistory) {
            try {
                UrlHistoryManager(context).clearHistory()
            } catch (_: Exception) {}
        }

        // 7. Invoke callback on main thread
        Handler(Looper.getMainLooper()).postDelayed({
            onComplete?.invoke()
        }, 150)
    }
}
