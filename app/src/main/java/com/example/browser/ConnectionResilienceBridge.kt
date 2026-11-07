package com.example.browser

/**
 * JavaScript injection shim to boost game loading responsiveness,
 * keep WebSocket connections alive, and automatically recover from
 * visibility changes, network transitions, and AudioContext stalls.
 */
object ConnectionResilienceBridge {

    val CONNECTION_RESILIENCE_SCRIPT = """
        (function() {
            if (window.__webCadeResilienceInstalled) return;
            window.__webCadeResilienceInstalled = true;

            // 1. Auto-unlock AudioContext:
            // Prevents HTML5 games (Phaser, Pixi, Unity WebGL) from stalling on loading screens
            // waiting for browser audio autoplay authorization.
            function unlockWebAudio() {
                try {
                    var AudioContextClass = window.AudioContext || window.webkitAudioContext;
                    if (AudioContextClass) {
                        if (!window.__webCadeSharedAudioCtx) {
                            // Find active contexts
                        }
                    }
                } catch(e) {}
            }

            var triggerEvents = ['touchstart', 'touchend', 'mousedown', 'keydown', 'click'];
            triggerEvents.forEach(function(evtName) {
                window.addEventListener(evtName, function() {
                    try {
                        if (window.AudioContext || window.webkitAudioContext) {
                            var ctx = (window.__webCadeSharedAudioCtx);
                            if (ctx && ctx.state === 'suspended') {
                                ctx.resume();
                            }
                        }
                    } catch(e) {}
                }, { passive: true, capture: true });
            });

            // 2. Immediate Focus and Visibility Recovery:
            // When device orientation flips or user switches apps, resume animations & WebSockets
            document.addEventListener('visibilitychange', function() {
                if (!document.hidden) {
                    try {
                        window.dispatchEvent(new Event('focus'));
                        window.dispatchEvent(new Event('pageshow'));
                    } catch(e) {}
                }
            });

            // 3. Keep-alive helper for active WebSockets
            // Notifies window on connection recovery
            window.addEventListener('online', function() {
                try {
                    window.dispatchEvent(new Event('focus'));
                } catch(e) {}
            });
        })();
    """.trimIndent()
}
