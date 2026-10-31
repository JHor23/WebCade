package com.example.browser

import android.webkit.WebView

object VirtualGamepadBridge {

    /**
     * JavaScript shim that polyfills or exposes standard W3C Gamepad API
     * so cloud gaming platforms like xCloud, GeForce NOW, and HTML5 games
     * instantly recognize the virtual controller as an Xbox 360 standard gamepad.
     */
    val INJECTION_SCRIPT = """
        (function() {
            if (window.__zeroPlayGamepadInstalled) return;
            window.__zeroPlayGamepadInstalled = true;
            
            var padState = {
                buttons: new Array(17).fill(0).map(() => ({ pressed: false, touched: false, value: 0.0 })),
                axes: [0.0, 0.0, 0.0, 0.0],
                timestamp: performance.now()
            };

            var fakeGamepad = {
                id: "Xbox 360 Controller (XInput STANDARD GAMEPAD)",
                index: 0,
                connected: true,
                timestamp: performance.now(),
                mapping: "standard",
                axes: [0, 0, 0, 0],
                buttons: padState.buttons,
                vibrationActuator: null
            };

            window.__updateZeroPlayGamepad = function(lx, ly, rx, ry, btnMask, lt, rt) {
                fakeGamepad.timestamp = performance.now();
                fakeGamepad.axes[0] = lx;
                fakeGamepad.axes[1] = ly;
                fakeGamepad.axes[2] = rx;
                fakeGamepad.axes[3] = ry;

                // 17 standard gamepad buttons:
                // 0: A, 1: B, 2: X, 3: Y, 4: LB, 5: RB, 6: LT, 7: RT,
                // 8: Back/Select, 9: Start, 10: L3, 11: R3, 12: Up, 13: Down, 14: Left, 15: Right, 16: Guide
                for (var i = 0; i < 17; i++) {
                    var pressed = ((btnMask & (1 << i)) !== 0);
                    var val = pressed ? 1.0 : 0.0;
                    if (i === 6) val = lt; // LT analog
                    if (i === 7) val = rt; // RT analog
                    fakeGamepad.buttons[i] = {
                        pressed: pressed || val > 0.1,
                        touched: pressed || val > 0.0,
                        value: val
                    };
                }

                // Dispatch gamepadconnected if not already fired
                if (!window.__gamepadEventFired) {
                    window.__gamepadEventFired = true;
                    var evt = new Event('gamepadconnected');
                    evt.gamepad = fakeGamepad;
                    window.dispatchEvent(evt);
                }
            };

            var originalGetGamepads = navigator.getGamepads ? navigator.getGamepads.bind(navigator) : null;
            navigator.getGamepads = function() {
                var real = originalGetGamepads ? originalGetGamepads() : [];
                var list = Array.from(real || []);
                if (!list[0]) {
                    list[0] = fakeGamepad;
                }
                return list;
            };

            console.log("ZeroPlay: Virtual Gamepad shim initialized.");
        })();
    """.trimIndent()

    fun updateGamepadState(
        webView: WebView?,
        lx: Float,
        ly: Float,
        rx: Float,
        ry: Float,
        btnMask: Int,
        lt: Float,
        rt: Float
    ) {
        webView?.post {
            webView.evaluateJavascript(
                "if (window.__updateZeroPlayGamepad) { window.__updateZeroPlayGamepad($lx, $ly, $rx, $ry, $btnMask, $lt, $rt); }",
                null
            )
        }
    }
}
