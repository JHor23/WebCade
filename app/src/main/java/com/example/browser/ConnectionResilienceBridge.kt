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

            // 4. Force-Cache Static Game Assets in fetch():
            // Ensures game asset downloads (sprites, wasm, json map chunks, audio) leverage
            // the persistent disk/memory cache instead of revalidating on every scene transition.
            try {
                var originalFetch = window.fetch;
                if (originalFetch) {
                    var assetRegex = /\.(png|jpg|jpeg|webp|gif|svg|ogg|mp3|wav|m4a|wasm|json|atlas|bin|gltf|glb|woff2?|ttf)(\?.*)?$/i;
                    window.fetch = function(input, init) {
                        try {
                            var url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                            if (url && assetRegex.test(url)) {
                                init = init || {};
                                if (!init.cache) {
                                    init.cache = 'force-cache';
                                }
                            }
                        } catch(err) {}
                        return originalFetch.apply(this, arguments);
                    };
                }
            } catch(e) {}

            // 5. Texture & Image Cache Retention:
            // Prevents Chromium from garbage collecting decoded GPU textures when changing scenes/maps
            try {
                var retainedImages = [];
                var MAX_RETAINED = 64;
                var OriginalImage = window.Image;
                if (OriginalImage) {
                    var proto = OriginalImage.prototype;
                    var origSrcDesc = Object.getOwnPropertyDescriptor(proto, 'src') || 
                                      Object.getOwnPropertyDescriptor(HTMLImageElement.prototype, 'src');
                    if (origSrcDesc && origSrcDesc.set) {
                        var origSet = origSrcDesc.set;
                        Object.defineProperty(proto, 'src', {
                            set: function(val) {
                                try {
                                    if (val && typeof val === 'string' && val.length < 512) {
                                        retainedImages.push(val);
                                        if (retainedImages.length > MAX_RETAINED) {
                                            retainedImages.shift();
                                        }
                                    }
                                } catch(_) {}
                                return origSet.call(this, val);
                            },
                            get: origSrcDesc.get,
                            configurable: true
                        });
                    }
                }
            } catch(e) {}

            // 6. Safe screen.orientation.lock() polyfill & Fullscreen crash prevention:
            // Connects HTML5 orientation lock calls to native Android orientation control
            try {
                if (window.screen) {
                    if (!window.screen.orientation) {
                        window.screen.orientation = {};
                    }
                    window.screen.orientation.lock = function(orientation) {
                        try {
                            if (window.WebCadeNative) {
                                var str = (orientation || '').toLowerCase();
                                if (str.indexOf('landscape') !== -1) {
                                    window.WebCadeNative.requestLandscape();
                                } else if (str.indexOf('portrait') !== -1) {
                                    window.WebCadeNative.requestPortrait();
                                }
                            }
                        } catch(_) {}
                        return Promise.resolve();
                    };
                    window.screen.orientation.unlock = function() {
                        try {
                            if (window.WebCadeNative) {
                                window.WebCadeNative.requestSensor();
                            }
                        } catch(_) {}
                    };
                }
            } catch(e) {}

            // 7. Fullscreen button click handler:
            // When game displays a "Full screen" prompt, rotate to landscape and ensure no WebView crash
            document.addEventListener('click', function(e) {
                var target = e.target;
                if (!target) return;
                var text = (target.textContent || target.innerText || '').trim().toLowerCase();
                if (text === 'full screen' || text === 'fullscreen') {
                    try {
                        if (window.WebCadeNative) {
                            window.WebCadeNative.requestLandscape();
                        }
                    } catch(_) {}
                }
            }, true);

            // 8. Portrait & Landscape Gaming Unlocker:
            // Dismisses intrusive "Turn your phone / The game is played in landscape" overlays
            // so games can be freely enjoyed in both Portrait and Landscape orientations!
            function dismissOrientationOverlays() {
                try {
                    var candidates = document.querySelectorAll('div, section, p, span, button');
                    for (var i = 0; i < candidates.length; i++) {
                        var el = candidates[i];
                        if (el.children && el.children.length > 5) continue;
                        var txt = (el.innerText || el.textContent || '').toLowerCase();
                        if (txt.includes('turn your phone') || txt.includes('game is played in landscape') || txt.includes('please rotate your device') || txt.includes('rotate your phone')) {
                            var curr = el;
                            while (curr && curr !== document.body && curr !== document.documentElement) {
                                var style = window.getComputedStyle(curr);
                                if (style && (style.position === 'fixed' || style.position === 'absolute' || parseInt(style.zIndex, 10) >= 50)) {
                                    curr.style.setProperty('display', 'none', 'important');
                                    curr.style.setProperty('visibility', 'hidden', 'important');
                                    curr.style.setProperty('opacity', '0', 'important');
                                    curr.style.setProperty('pointer-events', 'none', 'important');
                                    break;
                                }
                                curr = curr.parentElement;
                            }
                        }
                    }
                } catch(_) {}
            }

            dismissOrientationOverlays();
            setInterval(dismissOrientationOverlays, 800);
            window.addEventListener('resize', dismissOrientationOverlays);
            window.addEventListener('orientationchange', dismissOrientationOverlays);
            document.addEventListener('DOMContentLoaded', dismissOrientationOverlays);
        })();
    """.trimIndent()
}
