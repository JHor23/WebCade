package com.example.model

data class GamingPlatform(
    val id: String,
    val name: String,
    val tag: String,
    val url: String,
    val description: String,
    val recommendedUa: UserAgentMode,
    val iconEmoji: String,
    val category: String = "Cloud Gaming"
)

enum class UserAgentMode(val label: String, val value: String) {
    DESKTOP_CHROME(
        label = "Desktop Windows (Chrome 131)",
        value = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    ),
    CHROME_OS(
        label = "ChromeOS / Chromebook",
        value = "Mozilla/5.0 (X11; CrOS x86_64 15886.44.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    ),
    MOBILE(
        label = "Default Mobile",
        value = ""
    )
}

enum class ScreenOrientationMode(val label: String, val iconEmoji: String) {
    AUTO(
        label = "Auto (Sensor)",
        iconEmoji = "🔄"
    ),
    LANDSCAPE(
        label = "Landscape (Widescreen)",
        iconEmoji = "🖥️"
    ),
    PORTRAIT(
        label = "Portrait (Handheld)",
        iconEmoji = "📱"
    )
}

object GamingPresets {
    val platforms = listOf(
        GamingPlatform(
            id = "xbox",
            name = "Xbox Cloud Gaming",
            tag = "xCloud",
            url = "https://www.xbox.com/play",
            description = "Stream console titles with Game Pass Ultimate",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🎮",
            category = "Cloud Streaming"
        ),
        GamingPlatform(
            id = "gfn",
            name = "GeForce NOW",
            tag = "NVIDIA GFN",
            url = "https://play.geforcenow.com",
            description = "Stream high-end RTX PC titles at ultra low latency",
            recommendedUa = UserAgentMode.CHROME_OS,
            iconEmoji = "⚡",
            category = "Cloud Streaming"
        ),
        GamingPlatform(
            id = "luna",
            name = "Amazon Luna",
            tag = "Luna",
            url = "https://luna.amazon.com",
            description = "Instant cloud gaming with Prime & Ubisoft+",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🌙",
            category = "Cloud Streaming"
        ),
        GamingPlatform(
            id = "boosteroid",
            name = "Boosteroid",
            tag = "Boosteroid",
            url = "https://cloud.boosteroid.com",
            description = "Stream modern PC titles through browser WebRTC",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🚀",
            category = "Cloud Streaming"
        ),
        GamingPlatform(
            id = "crazygames",
            name = "CrazyGames",
            tag = "Web Games",
            url = "https://www.crazygames.com",
            description = "Thousands of fast-loading browser 3D and multiplayer games",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🕹️",
            category = "Browser Games"
        ),
        GamingPlatform(
            id = "krunker",
            name = "Krunker.io",
            tag = "3D WebGL FPS",
            url = "https://krunker.io",
            description = "Fast-paced competitive 3D multiplayer shooter in browser",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🔫",
            category = "Browser Games"
        ),
        GamingPlatform(
            id = "poki",
            name = "Poki Games",
            tag = "HTML5 Portal",
            url = "https://poki.com",
            description = "Instant action, racing, and arcade browser games",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🏆",
            category = "Browser Games"
        ),
        GamingPlatform(
            id = "itch",
            name = "Itch.io Games",
            tag = "Indie Web Games",
            url = "https://itch.io/games/html5",
            description = "Explore thousands of indie games playable in browser",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "👾",
            category = "Browser Games"
        ),
        GamingPlatform(
            id = "gamepad_test",
            name = "Gamepad Tester",
            tag = "Input Diagnostic",
            url = "https://gamepad-tester.com",
            description = "Test physical & virtual controller latency and button mappings",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🎯",
            category = "Diagnostics"
        ),
        GamingPlatform(
            id = "webgl_benchmark",
            name = "WebGL 3D Aquarium",
            tag = "GPU Benchmark",
            url = "https://webglsamples.org/aquarium/aquarium.html",
            description = "Hardware acceleration stress-test at 60/120 FPS",
            recommendedUa = UserAgentMode.DESKTOP_CHROME,
            iconEmoji = "🐠",
            category = "Diagnostics"
        )
    )
}
