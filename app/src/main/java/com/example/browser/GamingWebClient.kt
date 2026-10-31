package com.example.browser

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WebState(
    val url: String = "",
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isFullscreenCustomView: Boolean = false,
    val error: String? = null
)

class GamingWebClient(
    private val onCustomViewChanged: (View?, WebChromeClient.CustomViewCallback?) -> Unit
) {
    private val _webState = MutableStateFlow(WebState())
    val webState: StateFlow<WebState> = _webState.asStateFlow()

    val webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            _webState.value = _webState.value.copy(
                url = url ?: "",
                isLoading = true,
                error = null,
                canGoBack = view?.canGoBack() ?: false,
                canGoForward = view?.canGoForward() ?: false
            )
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            _webState.value = _webState.value.copy(
                url = url ?: "",
                title = view?.title ?: "",
                isLoading = false,
                canGoBack = view?.canGoBack() ?: false,
                canGoForward = view?.canGoForward() ?: false
            )
            // Inject virtual gamepad bridge into DOM
            view?.evaluateJavascript(VirtualGamepadBridge.INJECTION_SCRIPT, null)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                _webState.value = _webState.value.copy(
                    isLoading = false,
                    error = error?.description?.toString() ?: "Network connection issue"
                )
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme?.lowercase() ?: ""
            // Allow standard http and https gaming links
            if (scheme == "http" || scheme == "https") {
                return false
            }
            // Block or ignore external non-web schemes to maintain locked-in gaming fullscreen
            return true
        }
    }

    val webChromeClient = object : WebChromeClient() {
        private var customViewCallback: CustomViewCallback? = null

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            _webState.value = _webState.value.copy(
                progress = newProgress,
                isLoading = newProgress < 100
            )
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (!title.isNullOrBlank()) {
                _webState.value = _webState.value.copy(title = title)
            }
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            if (request == null) return
            // Cloud gaming platforms require audio capture for microphone voice chat and video capture
            val requestedResources = request.resources
            request.grant(requestedResources)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            customViewCallback = callback
            _webState.value = _webState.value.copy(isFullscreenCustomView = true)
            onCustomViewChanged(view, callback)
        }

        override fun onHideCustomView() {
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
            _webState.value = _webState.value.copy(isFullscreenCustomView = false)
            onCustomViewChanged(null, null)
        }
    }
}
