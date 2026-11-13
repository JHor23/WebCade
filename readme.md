# 🕹️ WebCade

**WebCade** is a specialized full-screen gaming browser and launcher designed to seamlessly bridge web-based games, cloud streaming platforms, and native gamepad inputs without clutter or distracting UI elements.

Whether you're streaming AAA titles, jumping into browser-based MMORPGs, or running lightweight web games, WebCade optimizes performance, latency, and input mapping for an elevated handheld and mobile experience.

---

## ✨ Key Features

*   **🌐 Gaming URL Bar & Quick Launch:** Instantly jump into your favorite web games, browser-based MMORPGs (like *PokéFable Online*), and web tools with dedicated user-agent toggles (Desktop Win 11, ChromeOS, Mobile).
*   **🔒 Anti-DPI Encrypted Tunnel:** Bypass ISP throttling, Deep Packet Inspection (DPI), and stream blocks using built-in encrypted tunneling modes, complete with live latency testing.
*   **🛡️ DNS Ad-Blocking & Privacy:** Built-in DNS filtering toggle to block invasive ads, pop-ups, and tracking scripts, ensuring faster page load times and uninterrupted gameplay.
*   **☁️ Cloud Gaming Hub:** Integrated quick-access dashboards optimized for major cloud platforms:
    *   Xbox Cloud Gaming (xCloud)
    *   NVIDIA GeForce NOW
    *   Amazon Luna
    *   Boosteroid
    *   Web & 3D WebGL Games (e.g., *Krunker.io*)
*   **🎮 Advanced Controller Mode & Mapping:** Native W3C Gamepad API passthrough that routes physical controller inputs straight to HTML5 and WebRTC layers, offering dedicated gamepad support or keyboard/mouse emulation for games without native controller compatibility.
*   **📱 Immersive Full-Screen Layout:** Clean, zero-GUI viewport during gameplay with a quick swipe-to-hide navigation bar for total visual immersion.

---

## 🔒 Anti-DPI Encrypted Tunnel (ISP Throttling Bypass)

WebCade includes a built-in **Anti-DPI Encrypted Tunnel** designed to route game and web traffic safely around ISP Deep Packet Inspection (DPI) throttling or live-stream blocks:
*   **Cloudflare WARP / 1.1.1.1:** Routes all traffic through a local Cloudflare WARP SOCKS5 tunnel (port 40000) for fast, low-overhead encryption.
*   **Anti-DPI Encrypted Proxy:** Utilizes an encrypted TLS tunnel engineered to defeat ISP SNI inspection and DNS poisoning.
*   **Custom Proxy (SOCKS5/HTTPS):** Supports user-defined WireGuard SOCKS5 or private VPS proxy endpoints for full routing control.
*   **Built-in Latency Diagnostics:** Features a one-tap **Test Tunnel** tool to measure active tunnel latency in real time before launching a session.

---

## 🛡️ DNS Ad-Blocking & Privacy

WebCade features an easily accessible **DNS Protection Toggle** right in the URL controls.
*   **Ad & Tracker Filtering:** Blocks third-party ad networks and tracking telemetry before they render, saving bandwidth and reducing system overhead.
*   **One-Tap Toggle:** Easily turn DNS protection ON or OFF directly from the URL bar to resolve any site-compatibility issues on the fly.
*   **Data Cleanup:** Includes a one-click **Clear Data** utility to wipe session cookies, cache, and local storage between gaming sessions.

---

## 🚀 Quick Start / Usage

1. Clone or download the repository.
2. Build and run the application via your preferred Android/web development environment.
3. Enter your target game URL or pick from the pre-configured cloud gaming shortcuts.
4. Set your preferred **UA Mode** (Desktop Win 11, ChromeOS, or Mobile) and toggle **DNS Filtering** as needed.
5. Configure your controller mapping mode (Dedicated Gamepad vs. Key & Mouse Emulation) and dive in!

---

## 🛠️ Built With
*   Modern Web Technologies & Native Wrappers
*   W3C Gamepad API
*   WebRT
*   This was created with AI

---

## IMPORTANT
*   For the time being, you will have to uninstall previous versions before installing the latest releases.  I do not have these set to update over themselves.
*   If you investigate the package name, you'll see it listed as "ZeroPlay".  Google AI Studio gave this application that name, and I'm unable to change that at a package level.

## 📄 License
Created by Google A.I. Studio and it falls under their licensing.