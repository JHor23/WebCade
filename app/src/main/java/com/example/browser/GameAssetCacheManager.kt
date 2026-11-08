package com.example.browser

import android.content.Context
import android.net.Uri
import android.util.LruCache
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * High-performance, two-tier game asset caching engine for WebCade.
 *
 * Solves the issue where browser games (like PokeFable Online, Phaser, PixiJS, Unity WebGL)
 * re-fetch all textures, audio, spritesheets, wasm, and map tiles on every scene/map transition.
 *
 * Tier 1: 32MB in-memory LRU Cache (Instant 0-1ms retrieval for active sprites, tilesets, and audio)
 * Tier 2: 256MB persistent disk cache with atomic file storage & LRU eviction
 *
 * Features:
 * - Serves cached assets with immutable Cache-Control & CORS headers so games never stall or throw canvas errors
 * - Non-blocking: cache misses immediately fall through to Chromium's native HTTP/2/3 pipeline with zero latency,
 *   while assets are cached in the background for subsequent transitions
 * - Thread-safe: never touches Android Views or WebSettings on worker threads
 * - Safe exclusion of main-frame HTML, dynamic API endpoints, and WebSockets
 */
class GameAssetCacheManager(private val context: Context) {

    companion object {
        private const val MAGIC_HEADER = 0x57434143 // "WCAC"
        private const val MAX_MEMORY_CACHE_BYTES = 32 * 1024 * 1024 // 32 MB
        private const val MAX_DISK_CACHE_BYTES = 256 * 1024 * 1024L // 256 MB
        private const val MAX_SINGLE_ASSET_BYTES = 20 * 1024 * 1024 // 20 MB max per single asset
        private const val MAX_MEM_ASSET_BYTES = 1024 * 1024 // 1 MB max for memory cache entry

        // Recognized static game asset extensions
        private val CACHEABLE_EXTENSIONS = hashSetOf(
            // Sprites, Textures & Artwork
            "png", "jpg", "jpeg", "webp", "gif", "svg", "ico",
            // Audio & Music
            "ogg", "mp3", "wav", "m4a", "aac", "opus", "flac",
            // 3D Models & Data
            "gltf", "glb", "bin", "obj", "mtl",
            // WebAssembly & Bytecode
            "wasm",
            // Game Data & Map Bundles
            "json", "atlas", "data", "pack",
            // Fonts & Styles
            "woff", "woff2", "ttf", "otf", "eot", "css",
            // Scripts & Shaders
            "js", "vert", "frag", "glsl"
        )

        // Sensitive paths that should NEVER be cached as static assets
        private val DYNAMIC_PATH_PATTERNS = listOf(
            "/api/", "/auth/", "/login/", "/token/", "/session/",
            "/socket.io", "/ws/", "/wss/", "/live/", "/status",
            "/sync/", "/heartbeat", "/ping", "/checkout", "/payment"
        )
    }

    private data class MemoryAsset(
        val data: ByteArray,
        val mimeType: String,
        val encoding: String?
    )

    private val cacheDir: File = File(context.cacheDir, "webcade_game_asset_cache").apply {
        if (!exists()) mkdirs()
    }

    // In-memory LRU cache (bounded at 32MB)
    private val memoryCache = object : LruCache<String, MemoryAsset>(MAX_MEMORY_CACHE_BYTES) {
        override fun sizeOf(key: String, value: MemoryAsset): Int {
            return value.data.size
        }
    }

    // Dedicated bounded thread pool for background asset caching without blocking Chromium IO threads
    private val backgroundCacheExecutor = Executors.newFixedThreadPool(2)

    // Set of URLs currently being downloaded to prevent redundant parallel requests
    private val activeFetches = ConcurrentHashMap<String, Boolean>()

    /**
     * Determines whether a web request is for a cacheable static game asset.
     */
    fun isCacheableGameAsset(request: WebResourceRequest): Boolean {
        try {
            // Never intercept main frame HTML navigation
            if (request.isForMainFrame) return false

            // Only cache GET requests
            val method = request.method?.uppercase(Locale.ROOT) ?: "GET"
            if (method != "GET") return false

            val uri = request.url ?: return false
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
            if (scheme != "http" && scheme != "https") return false

            val path = uri.path?.lowercase(Locale.ROOT) ?: ""

            // Exclude dynamic APIs, WebSockets, and authentication endpoints
            for (dynamicPattern in DYNAMIC_PATH_PATTERNS) {
                if (path.contains(dynamicPattern)) return false
            }

            // Check file extension
            val dotIndex = path.lastIndexOf('.')
            if (dotIndex != -1 && dotIndex < path.length - 1) {
                val extension = path.substring(dotIndex + 1)
                if (CACHEABLE_EXTENSIONS.contains(extension)) {
                    return true
                }
            }

            // Check if query string indicates a known game asset type
            val query = uri.query?.lowercase(Locale.ROOT) ?: ""
            if (query.contains("format=png") || query.contains("format=webp") ||
                query.contains("type=texture") || query.contains("type=audio") ||
                query.contains("spritesheet") || query.contains("tileset")
            ) {
                return true
            }

            return false
        } catch (_: Throwable) {
            return false
        }
    }

    /**
     * Intercepts and fulfills game asset requests from memory or disk cache.
     * Returns an immediate WebResourceResponse if cached, or null so Chromium's native
     * network stack fetches the resource with zero thread blocking.
     */
    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        try {
            if (!isCacheableGameAsset(request)) return null

            val uri = request.url ?: return null
            val urlString = uri.toString()
            val cacheKey = hashUrl(urlString)

            // 1. Tier 1: Check in-memory LRU cache (instant 0-1ms hit)
            val memHit = memoryCache.get(cacheKey)
            if (memHit != null) {
                return buildWebResourceResponse(
                    data = ByteArrayInputStream(memHit.data),
                    mimeType = memHit.mimeType,
                    encoding = memHit.encoding,
                    cacheSource = "HIT-MEMORY"
                )
            }

            // 2. Tier 2: Check persistent disk cache
            val diskFile = File(cacheDir, "$cacheKey.asset")
            if (diskFile.exists() && diskFile.length() > 0) {
                val diskAsset = readDiskAsset(diskFile)
                if (diskAsset != null) {
                    if (diskAsset.data.size <= MAX_MEM_ASSET_BYTES) {
                        memoryCache.put(cacheKey, diskAsset)
                    }
                    return buildWebResourceResponse(
                        data = ByteArrayInputStream(diskAsset.data),
                        mimeType = diskAsset.mimeType,
                        encoding = diskAsset.encoding,
                        cacheSource = "HIT-DISK"
                    )
                }
            }

            // 3. Cache Miss:
            // Never block the Chromium worker thread with a synchronous HttpURLConnection!
            // Instead, enqueue background caching for future scene transitions, and return null
            // so Chromium's optimized HTTP/2/3 pipeline loads the asset instantly without stall.
            enqueueBackgroundCache(request, urlString, cacheKey)
            return null
        } catch (_: Throwable) {
            return null
        }
    }

    private fun enqueueBackgroundCache(
        request: WebResourceRequest,
        urlString: String,
        cacheKey: String
    ) {
        try {
            if (activeFetches.containsKey(cacheKey)) return
            val diskFile = File(cacheDir, "$cacheKey.asset")
            if (diskFile.exists() && diskFile.length() > 0) return

            val headers = request.requestHeaders?.toMap()
            backgroundCacheExecutor.execute {
                if (activeFetches.putIfAbsent(cacheKey, true) != null) return@execute
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(urlString)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 4000
                        readTimeout = 6000
                        instanceFollowRedirects = true
                        useCaches = true

                        if (headers != null) {
                            for ((k, v) in headers) {
                                if (!k.equals("Host", ignoreCase = true) &&
                                    !k.equals("Connection", ignoreCase = true)
                                ) {
                                    setRequestProperty(k, v)
                                }
                            }
                        }

                        val cookies = try {
                            CookieManager.getInstance().getCookie(urlString)
                        } catch (_: Throwable) {
                            null
                        }
                        if (!cookies.isNullOrBlank()) {
                            setRequestProperty("Cookie", cookies)
                        }
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val rawContentType = connection.contentType ?: ""
                        val mimeType = extractMimeType(rawContentType, urlString)
                        val encoding = extractEncoding(rawContentType)

                        val inputStream = connection.inputStream
                        val bytes = readBytesWithLimit(inputStream, MAX_SINGLE_ASSET_BYTES)

                        if (bytes != null && bytes.isNotEmpty()) {
                            val asset = MemoryAsset(bytes, mimeType, encoding)
                            saveToDiskCache(cacheKey, asset)
                            if (bytes.size <= MAX_MEM_ASSET_BYTES) {
                                memoryCache.put(cacheKey, asset)
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Silently ignore background caching errors
                } finally {
                    try {
                        connection?.disconnect()
                    } catch (_: Throwable) {}
                    activeFetches.remove(cacheKey)
                }
            }
        } catch (_: Throwable) {}
    }

    private fun buildWebResourceResponse(
        data: InputStream,
        mimeType: String,
        encoding: String?,
        cacheSource: String
    ): WebResourceResponse {
        val responseHeaders = mutableMapOf(
            "Cache-Control" to "public, max-age=31536000, immutable",
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, HEAD, OPTIONS",
            "Access-Control-Allow-Headers" to "*",
            "X-WebCade-Asset-Cache" to cacheSource
        )
        return WebResourceResponse(
            mimeType,
            encoding ?: "UTF-8",
            200,
            "OK",
            responseHeaders,
            data
        )
    }

    private fun readBytesWithLimit(input: InputStream, limit: Int): ByteArray? {
        return try {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var totalRead = 0
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                if (totalRead > limit) {
                    return null
                }
                output.write(buffer, 0, bytesRead)
            }
            output.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveToDiskCache(cacheKey: String, asset: MemoryAsset) {
        try {
            pruneDiskCacheIfNeeded()

            val tempFile = File(cacheDir, "$cacheKey.tmp")
            val targetFile = File(cacheDir, "$cacheKey.asset")

            FileOutputStream(tempFile).use { fos ->
                DataOutputStream(fos).use { dos ->
                    dos.writeInt(MAGIC_HEADER)
                    dos.writeUTF(asset.mimeType)
                    dos.writeUTF(asset.encoding ?: "")
                    dos.writeLong(System.currentTimeMillis())
                    dos.writeInt(asset.data.size)
                    dos.write(asset.data)
                    dos.flush()
                }
            }

            if (tempFile.exists()) {
                tempFile.renameTo(targetFile)
            }
        } catch (_: Throwable) {}
    }

    private fun readDiskAsset(file: File): MemoryAsset? {
        return try {
            FileInputStream(file).use { fis ->
                DataInputStream(fis).use { dis ->
                    val magic = dis.readInt()
                    if (magic != MAGIC_HEADER) return null
                    val mimeType = dis.readUTF()
                    val encodingStr = dis.readUTF()
                    val encoding = if (encodingStr.isEmpty()) null else encodingStr
                    val timestamp = dis.readLong()
                    val size = dis.readInt()
                    if (size > MAX_SINGLE_ASSET_BYTES) return null

                    val data = ByteArray(size)
                    dis.readFully(data)
                    file.setLastModified(System.currentTimeMillis())
                    MemoryAsset(data, mimeType, encoding)
                }
            }
        } catch (_: Throwable) {
            try { file.delete() } catch (_: Throwable) {}
            null
        }
    }

    private fun pruneDiskCacheIfNeeded() {
        try {
            val files = cacheDir.listFiles() ?: return
            var totalSize = 0L
            for (f in files) {
                totalSize += f.length()
            }
            if (totalSize > MAX_DISK_CACHE_BYTES) {
                val sorted = files.sortedBy { it.lastModified() }
                val targetDeleteCount = (sorted.size * 0.3).toInt().coerceAtLeast(1)
                for (i in 0 until targetDeleteCount) {
                    sorted[i].delete()
                }
            }
        } catch (_: Throwable) {}
    }

    fun clearCache() {
        try {
            memoryCache.evictAll()
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        } catch (_: Throwable) {}
    }

    private fun hashUrl(url: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(url.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            url.hashCode().toString()
        }
    }

    private fun extractMimeType(contentTypeHeader: String, url: String): String {
        if (contentTypeHeader.isNotBlank()) {
            val clean = contentTypeHeader.substringBefore(';').trim().lowercase(Locale.ROOT)
            if (clean.isNotEmpty() && clean != "application/octet-stream" && clean != "text/plain") {
                return clean
            }
        }

        val lowerUrl = url.lowercase(Locale.ROOT).substringBefore('?')
        return when {
            lowerUrl.endsWith(".png") -> "image/png"
            lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") -> "image/jpeg"
            lowerUrl.endsWith(".webp") -> "image/webp"
            lowerUrl.endsWith(".gif") -> "image/gif"
            lowerUrl.endsWith(".svg") -> "image/svg+xml"
            lowerUrl.endsWith(".ico") -> "image/x-icon"
            lowerUrl.endsWith(".ogg") -> "audio/ogg"
            lowerUrl.endsWith(".mp3") -> "audio/mpeg"
            lowerUrl.endsWith(".wav") -> "audio/wav"
            lowerUrl.endsWith(".m4a") -> "audio/mp4"
            lowerUrl.endsWith(".opus") -> "audio/opus"
            lowerUrl.endsWith(".wasm") -> "application/wasm"
            lowerUrl.endsWith(".json") -> "application/json"
            lowerUrl.endsWith(".gltf") -> "model/gltf+json"
            lowerUrl.endsWith(".glb") -> "model/gltf-binary"
            lowerUrl.endsWith(".woff") -> "font/woff"
            lowerUrl.endsWith(".woff2") -> "font/woff2"
            lowerUrl.endsWith(".ttf") -> "font/ttf"
            lowerUrl.endsWith(".otf") -> "font/otf"
            lowerUrl.endsWith(".css") -> "text/css"
            lowerUrl.endsWith(".js") -> "application/javascript"
            else -> "application/octet-stream"
        }
    }

    private fun extractEncoding(contentTypeHeader: String): String? {
        val charsetIndex = contentTypeHeader.indexOf("charset=", ignoreCase = true)
        if (charsetIndex != -1) {
            return contentTypeHeader.substring(charsetIndex + 8).substringBefore(';').trim()
        }
        return null
    }
}
