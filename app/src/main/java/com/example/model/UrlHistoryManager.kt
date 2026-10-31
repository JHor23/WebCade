package com.example.model

import android.content.Context
import android.content.SharedPreferences

class UrlHistoryManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("webcade_urls_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "recent_urls"
        private const val KEY_LAST_URL = "last_url"
        private const val MAX_HISTORY = 10
    }

    fun getRecentUrls(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").filter { it.isNotBlank() }
    }

    fun addUrl(url: String) {
        val cleanUrl = formatUrl(url)
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

    fun getLastUrl(): String? {
        return prefs.getString(KEY_LAST_URL, null)
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun formatUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }
}
