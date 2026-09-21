/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.concurrent.thread

data class BrowserUiState(
    val url: String = "",
    val title: String = "正在打开网页",
    val progress: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val rendererLost: Boolean = false,
    val diagnostics: String? = null,
    val diagnosing: Boolean = false,
    val engine: String = "",
)

/**
 * UI-scoped WebView owner. Holds engine history and lifecycle, never retained by a ViewModel.
 * WebView callbacks are observations; they must never cause another loadUrl call.
 */
class BrowserSession(private var restoredState: Bundle? = null) {
    var state by mutableStateOf(BrowserUiState())
        private set
    private var view: WebView? = null
    var fullscreenView by mutableStateOf<android.view.View?>(null)
        private set
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var replayingKey = false
    private var navigationScript = ""
    var requestToolbarFocus: () -> Unit = {}

    @SuppressLint("SetJavaScriptEnabled")
    fun createView(context: Context, initialUrl: String): WebView {
        state = state.copy(url=initialUrl, error=null, rendererLost=false)
        return WebView(context).apply {
            view = this
            navigationScript = context.assets.open("remote-navigation.js").bufferedReader().use { it.readText() }
            val provider = if (android.os.Build.VERSION.SDK_INT >= 26) WebView.getCurrentWebViewPackage()?.let { "${it.packageName} ${it.versionName}" } else null
            state = state.copy(engine="Android ${android.os.Build.VERSION.RELEASE} · WebView ${provider ?: settings.userAgentString}")
            setBackgroundColor(android.graphics.Color.WHITE)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = true
                setSupportMultipleWindows(false)
                userAgentString += "; TVBrowser"
            }
            setDownloadListener { _, _, _, _, _ ->
                state = state.copy(notice="当前版本暂不支持下载")
            }
            setOnKeyListener { _, keyCode, event ->
                when {
                    replayingKey -> false
                    keyCode == KeyEvent.KEYCODE_MENU -> {
                        if (event.action == KeyEvent.ACTION_UP) requestToolbarFocus()
                        true
                    }
                    keyCode in directionKeys -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            val saved = KeyEvent(event)
                            navigate(directionKeys.getValue(keyCode)) { result ->
                                if (result == "toolbar") requestToolbarFocus()
                                else if (result != "handled") {
                                    replayingKey = true
                                    try { dispatchKeyEvent(saved); dispatchKeyEvent(KeyEvent.changeAction(saved, KeyEvent.ACTION_UP)) }
                                    finally { replayingKey = false }
                                }
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    blockUnsupported(request.url.toString())

                @Deprecated("Compatibility for old TV WebView providers")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    blockUnsupported(url)

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    state = state.copy(url=url, loading=true, progress=0, error=null, notice=null, diagnostics=null, diagnosing=false)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    state = state.copy(url=url, title=view.title.orEmpty().ifBlank { "网页" },
                        loading=false, progress=100)
                    view.evaluateJavascript(navigationScript, null)
                    CookieManager.getInstance().flush()
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                    state = state.copy(url=url)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) fail(NavigationFailure.web(error.errorCode, error.description.toString(), request.url.toString()).display())
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame) fail("HTTP ${response.statusCode} · ${response.reasonPhrase}\n${NavigationFailure.safeUrl(request.url.toString())}\n已连接网站，但服务器拒绝或无法处理此页面；401/403 表示登录或权限问题。")
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    if (error.url == state.url) fail("SSL_CERTIFICATE_ERROR (${error.primaryError})\n${NavigationFailure.safeUrl(error.url)}\n网站证书异常，连接已停止。请检查电视日期和网站证书。")
                    else state = state.copy(notice="已拦截证书异常的网页资源")
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    // Detach the unusable renderer immediately. Retry creates a fresh WebView.
                    (view.parent as? android.view.ViewGroup)?.removeView(view)
                    release(view)
                    state = state.copy(rendererLost=true, loading=false, error="网页进程已结束，请重新打开。")
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(custom: android.view.View, callback: CustomViewCallback) {
                    if (fullscreenView != null) { callback.onCustomViewHidden(); return }
                    fullscreenCallback = callback
                    fullscreenView = custom
                    custom.isFocusableInTouchMode = true
                    custom.setOnKeyListener { _, code, event ->
                        if (code in directionKeys || code == KeyEvent.KEYCODE_DPAD_CENTER ||
                            code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_MENU ||
                            code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) this@apply.dispatchKeyEvent(event) else false
                    }
                    custom.requestFocus()
                }
                override fun onHideCustomView() { exitFullscreen() }
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    state = state.copy(progress=newProgress)
                }
                override fun onReceivedTitle(view: WebView, title: String?) {
                    state = state.copy(title=title.orEmpty().ifBlank { "网页" })
                }
            }
            val restored = restoredState?.let { restoreState(it) }
            restoredState = null
            if (restored == null) loadUrl(initialUrl)
            else state = state.copy(url=url ?: initialUrl, title=title ?: "网页", loading=false, progress=100)
        }
    }

    private val directionKeys = mapOf(KeyEvent.KEYCODE_DPAD_UP to "ArrowUp", KeyEvent.KEYCODE_DPAD_DOWN to "ArrowDown",
        KeyEvent.KEYCODE_DPAD_LEFT to "ArrowLeft", KeyEvent.KEYCODE_DPAD_RIGHT to "ArrowRight",
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "MediaPlayPause", KeyEvent.KEYCODE_MEDIA_PLAY to "MediaPlay",
        KeyEvent.KEYCODE_MEDIA_PAUSE to "MediaPause", KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to "MediaFastForward",
        KeyEvent.KEYCODE_MEDIA_REWIND to "MediaRewind")

    private fun navigate(key: String, done: (String) -> Unit = {}) {
        val current = view ?: return done("unhandled")
        current.evaluateJavascript(navigationScript + "\nwindow.__tvBrowserNavigate(${org.json.JSONObject.quote(key)});") { value ->
            if (view === current) done(value.trim('"'))
        }
    }

    fun back(onHome: () -> Unit) {
        if (fullscreenView != null) { exitFullscreen(); return }
        if (state.error != null) { if (!goBack()) onHome(); return }
        navigate("Back") { if (it != "handled" && !goBack()) onHome() }
    }

    fun load(url: String) { if (AddressResolver.isWebUrl(url)) { state=state.copy(url=url, error=null); view?.loadUrl(url) } }

    fun diagnose() {
        if (state.diagnosing) return
        val url = state.url
        val current = view ?: return
        state = state.copy(diagnosing=true, diagnostics="正在检查 DNS 与目标端口…")
        thread(isDaemon=true, name="tv-network-diagnostics") {
            val result = NetworkDiagnostics.check(url)
            current.post { if (view === current && state.url == url) state=state.copy(diagnosing=false, diagnostics=result) }
        }
    }

    private fun blockUnsupported(url: String): Boolean {
        if (AddressResolver.isWebUrl(url)) return false
        state = state.copy(notice="此链接需要外部应用，当前页面已保留")
        return true
    }

    private fun fail(message: String) {
        state = state.copy(error=message, loading=false)
    }

    fun goBack(): Boolean {
        val current = view ?: return false
        if (!current.canGoBack()) return false
        current.goBack()
        return true
    }

    fun reload() {
        state = state.copy(error=null, notice=null, loading=true)
        view?.reload()
    }

    fun focusPage() { if (state.error == null) { view?.requestFocus(); navigate("Focus") } }
    fun exitFullscreen() {
        val callback = fullscreenCallback
        fullscreenCallback = null
        fullscreenView?.setOnKeyListener(null)
        fullscreenView = null
        callback?.onCustomViewHidden()
        view?.requestFocus()
    }
    fun resume() { view?.onResume() }
    fun pause() { view?.onPause() }
    fun dismissNotice() { state = state.copy(notice=null) }

    fun saveState(): Bundle = Bundle().also { view?.saveState(it) }

    fun release(target: WebView) {
        if (view !== target) return
        exitFullscreen()
        view = null
        target.stopLoading()
        target.setOnKeyListener(null)
        target.webChromeClient = null
        target.webViewClient = WebViewClient()
        target.destroy()
    }
}
