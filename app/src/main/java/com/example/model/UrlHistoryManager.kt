package com.example.model

import android.content.Context
import android.content.SharedPreferences
import java.net.URLEncoder

class UrlHistoryManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("webcade_urls_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "recent_urls"
        private const val KEY_LAST_URL = "last_url"
        private const val MAX_HISTORY = 10
        private const val GOOGLE_SEARCH_BASE = "https://www.google.com/search?q="

        fun buildGoogleSearchUrl(query: String): String {
            val trimmed = query.trim()
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            return "$GOOGLE_SEARCH_BASE$encoded"
        }

        fun isValidWebUrl(input: String): Boolean {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return false
            if (trimmed.any { it.isWhitespace() }) return false

            val authority: String
            val schemeIndex = trimmed.indexOf("://")
            if (schemeIndex >= 0) {
                val scheme = trimmed.substring(0, schemeIndex).lowercase()
                if (scheme != "http" && scheme != "https") return false
                val afterScheme = trimmed.substring(schemeIndex + 3)
                if (afterScheme.isEmpty()) return false
                authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            } else {
                // Reject malformed schemes like "http:/site.com" or "https:site.com"
                if (trimmed.startsWith("http:", ignoreCase = true) ||
                    trimmed.startsWith("https:", ignoreCase = true)
                ) {
                    return false
                }
                authority = trimmed.substringBefore('/').substringBefore('?').substringBefore('#')
            }

            return isValidAuthority(authority)
        }

        private fun isValidAuthority(authority: String): Boolean {
            if (authority.isEmpty()) return false
            if (authority.contains('@')) return false

            val colonCount = authority.count { it == ':' }
            if (colonCount > 1) return false

            val host: String
            if (colonCount == 1) {
                host = authority.substringBefore(':')
                val portStr = authority.substringAfter(':')
                val port = portStr.toIntOrNull() ?: return false
                if (port !in 1..65535) return false
            } else {
                host = authority
            }

            if (host.isEmpty()) return false
            if (host.equals("localhost", ignoreCase = true)) return true

            val labels = host.split('.')
            // Check IPv4 first if all labels are strictly numeric
            if (labels.all { label -> label.isNotEmpty() && label.all { it.isDigit() } }) {
                return labels.size == 4 && labels.all { part ->
                    val octet = part.toIntOrNull()
                    octet != null && octet in 0..255 && part == octet.toString()
                }
            }

            // Must have at least domain + TLD (e.g., "xbox.com")
            if (labels.size < 2) return false
            if (host.startsWith('.') || host.endsWith('.')) return false

            for (label in labels) {
                if (label.isEmpty() || label.length > 63) return false
                if (label.startsWith('-') || label.endsWith('-')) return false
                if (!label.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' }) {
                    return false
                }
            }

            val tld = labels.last()
            val isAlphaTld = tld.length >= 2 && tld.all { it in 'a'..'z' || it in 'A'..'Z' }
            val isPunycodeTld = tld.startsWith("xn--", ignoreCase = true) && tld.length > 4
            return isAlphaTld || isPunycodeTld
        }

        fun resolveUrlOrGoogleSearch(input: String): String {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return ""
            if (!isValidWebUrl(trimmed)) {
                return buildGoogleSearchUrl(trimmed)
            }
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("https://") -> "https://" + trimmed.substring(8)
                lower.startsWith("http://") -> "http://" + trimmed.substring(7)
                else -> "https://$trimmed"
            }
        }
    }

    fun getRecentUrls(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").filter { it.isNotBlank() }
    }

    fun addUrl(url: String) {
        val cleanUrl = formatUrl(url)
        if (cleanUrl.isEmpty()) return
        val current = getRecentUrls().toMutableList()
        current.remove(cleanUrl)
        current.add(0, cleanUrl)
        if (current.size > MAX_HISTORY) {
            current.removeAt(current.size - 1)
        }
        prefs.edit()
            .putString(KEY_HISTORY, current.joinToString(";"))
            .putString(KEY_LAST_URL, cleanUrl)
            .apply()
    }

    fun removeUrl(url: String) {
        val current = getRecentUrls().toMutableList()
        if (current.remove(url)) {
            prefs.edit()
                .putString(KEY_HISTORY, current.joinToString(";"))
                .apply()
        }
    }

    fun getLastUrl(): String? {
        return prefs.getString(KEY_LAST_URL, null)
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun formatUrl(input: String): String {
        return resolveUrlOrGoogleSearch(input)
    }
}
