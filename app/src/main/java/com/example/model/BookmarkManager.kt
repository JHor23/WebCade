package com.example.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

data class SiteBookmark(
    val id: String,
    val title: String,
    val url: String,
    val preferredUa: UserAgentMode = UserAgentMode.DESKTOP_CHROME,
    val createdAt: Long = System.currentTimeMillis()
)

class BookmarkManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("webcade_bookmarks_pref", Context.MODE_PRIVATE)

    private val _bookmarks = MutableStateFlow(loadBookmarks())
    val bookmarks: StateFlow<List<SiteBookmark>> = _bookmarks.asStateFlow()

    companion object {
        private const val KEY_BOOKMARKS_JSON = "saved_bookmarks_v1"

        fun normalizeUrlKey(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return ""
            return try {
                val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
                val uri = Uri.parse(withScheme)
                val scheme = (uri.scheme ?: "https").lowercase(Locale.ROOT)
                val host = (uri.host ?: "").lowercase(Locale.ROOT).removePrefix("www.")
                val port = if (uri.port != -1 &&
                    !((scheme == "http" && uri.port == 80) || (scheme == "https" && uri.port == 443))
                ) ":${uri.port}" else ""
                val path = (uri.encodedPath ?: "").trimEnd('/')
                val query = uri.encodedQuery?.let { "?$it" } ?: ""
                if (host.isNotEmpty()) {
                    "$scheme://$host$port$path$query"
                } else {
                    trimmed.lowercase(Locale.ROOT).trimEnd('/')
                }
            } catch (_: Throwable) {
                trimmed.lowercase(Locale.ROOT).trimEnd('/')
            }
        }

        fun deriveTitleFromUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return "Bookmark"
            return try {
                val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
                val host = Uri.parse(withScheme).host?.removePrefix("www.")
                if (!host.isNullOrBlank()) host else trimmed
            } catch (_: Throwable) {
                trimmed
            }
        }

        fun formatBookmarkUrl(rawInput: String): String {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return ""
            val resolved = UrlHistoryManager.resolveUrlOrGoogleSearch(trimmed)
            return if (resolved.isNotBlank()) resolved else trimmed
        }
    }

    private fun loadBookmarks(): List<SiteBookmark> {
        val rawJson = prefs.getString(KEY_BOOKMARKS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                    val url = obj.optString("url")
                    if (url.isBlank()) continue
                    val title = obj.optString("title").ifBlank { deriveTitleFromUrl(url) }
                    val uaName = obj.optString("preferredUa")
                    val ua = UserAgentMode.entries.firstOrNull { it.name == uaName }
                        ?: UserAgentMode.DESKTOP_CHROME
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    add(
                        SiteBookmark(
                            id = id,
                            title = title,
                            url = url,
                            preferredUa = ua,
                            createdAt = createdAt
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun saveBookmarks(list: List<SiteBookmark>) {
        val array = JSONArray()
        list.forEach { bookmark ->
            val obj = JSONObject().apply {
                put("id", bookmark.id)
                put("title", bookmark.title)
                put("url", bookmark.url)
                put("preferredUa", bookmark.preferredUa.name)
                put("createdAt", bookmark.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_BOOKMARKS_JSON, array.toString()).apply()
        _bookmarks.value = list
    }

    fun isBookmarked(url: String): Boolean {
        val key = normalizeUrlKey(url)
        if (key.isEmpty()) return false
        return _bookmarks.value.any { normalizeUrlKey(it.url) == key }
    }

    fun findByUrl(url: String): SiteBookmark? {
        val key = normalizeUrlKey(url)
        if (key.isEmpty()) return null
        return _bookmarks.value.firstOrNull { normalizeUrlKey(it.url) == key }
    }

    fun addBookmark(
        url: String,
        title: String = "",
        preferredUa: UserAgentMode = UserAgentMode.DESKTOP_CHROME
    ): SiteBookmark? {
        val formattedUrl = formatBookmarkUrl(url)
        if (formattedUrl.isBlank()) return null

        val cleanTitle = title.trim().ifBlank { deriveTitleFromUrl(formattedUrl) }
        val key = normalizeUrlKey(formattedUrl)
        val current = _bookmarks.value.toMutableList()
        val existingIndex = current.indexOfFirst { normalizeUrlKey(it.url) == key }

        val saved = if (existingIndex >= 0) {
            val updated = current[existingIndex].copy(
                title = cleanTitle,
                url = formattedUrl,
                preferredUa = preferredUa
            )
            current[existingIndex] = updated
            updated
        } else {
            val created = SiteBookmark(
                id = UUID.randomUUID().toString(),
                title = cleanTitle,
                url = formattedUrl,
                preferredUa = preferredUa
            )
            current.add(0, created)
            created
        }

        saveBookmarks(current)
        return saved
    }

    fun updateBookmark(
        id: String,
        newTitle: String,
        newUrl: String,
        newUa: UserAgentMode
    ): SiteBookmark? {
        val formattedUrl = formatBookmarkUrl(newUrl)
        if (formattedUrl.isBlank()) return null
        val cleanTitle = newTitle.trim().ifBlank { deriveTitleFromUrl(formattedUrl) }
        val current = _bookmarks.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return addBookmark(formattedUrl, cleanTitle, newUa)

        val updated = current[idx].copy(
            title = cleanTitle,
            url = formattedUrl,
            preferredUa = newUa
        )
        current[idx] = updated
        saveBookmarks(current)
        return updated
    }

    fun removeBookmark(urlOrId: String): Boolean {
        val trimmed = urlOrId.trim()
        if (trimmed.isEmpty()) return false
        val key = normalizeUrlKey(trimmed)
        val current = _bookmarks.value.toMutableList()
        val removed = current.removeAll { it.id == trimmed || normalizeUrlKey(it.url) == key }
        if (removed) {
            saveBookmarks(current)
        }
        return removed
    }

    fun toggleBookmark(
        url: String,
        title: String = "",
        preferredUa: UserAgentMode = UserAgentMode.DESKTOP_CHROME
    ): Boolean {
        return if (isBookmarked(url)) {
            removeBookmark(url)
            false
        } else {
            addBookmark(url, title, preferredUa) != null
        }
    }

    fun clearAllBookmarks() {
        saveBookmarks(emptyList())
    }
}
