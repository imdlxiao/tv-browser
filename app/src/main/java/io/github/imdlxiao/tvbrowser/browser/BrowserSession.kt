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

data class BrowserUiState(
    val url: String = "",
    val title: String = "正在打开网页",
    val progress: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val rendererLost: Boolean = false,
)

/**
 * UI-scoped WebView owner. Holds engine history and lifecycle, never retained by a ViewModel.
 * WebView callbacks are observations; they must never cause another loadUrl call.
 */
class BrowserSession(private var restoredState: Bundle? = null) {
    var state by mutableStateOf(BrowserUiState())
        private set
    private var view: WebView? = null
    var requestToolbarFocus: () -> Unit = {}

    @SuppressLint("SetJavaScriptEnabled")
    fun createView(context: Context, initialUrl: String): WebView {
        state = state.copy(url=initialUrl, error=null, rendererLost=false)
        return WebView(context).apply {
            view = this
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
                    keyCode == KeyEvent.KEYCODE_MENU -> {
                        if (event.action == KeyEvent.ACTION_UP) requestToolbarFocus()
                        true
                    }
                    keyCode == KeyEvent.KEYCODE_DPAD_UP && !canScrollVertically(-1) && !hasEditableFocus() -> {
                        if (event.action == KeyEvent.ACTION_DOWN) requestToolbarFocus()
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
                    state = state.copy(url=url, loading=true, progress=0, error=null, notice=null)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    state = state.copy(url=url, title=view.title.orEmpty().ifBlank { "网页" },
                        loading=false, progress=100)
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                    state = state.copy(url=url)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) fail("网页暂时无法打开，请检查网络后重试。")
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame) fail("网站暂时无法提供此页面（" + response.statusCode + "）。")
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    if (error.url == state.url) fail("网站安全证书异常，已停止连接。")
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

    private fun WebView.hasEditableFocus(): Boolean =
        hitTestResult?.type == WebView.HitTestResult.EDIT_TEXT_TYPE

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

    fun focusPage() { view?.requestFocus() }
    fun resume() { view?.onResume() }
    fun pause() { view?.onPause() }
    fun dismissNotice() { state = state.copy(notice=null) }

    fun saveState(): Bundle = Bundle().also { view?.saveState(it) }

    fun release(target: WebView) {
        if (view !== target) return
        view = null
        target.stopLoading()
        target.setOnKeyListener(null)
        target.webChromeClient = null
        target.webViewClient = WebViewClient()
        target.destroy()
    }
}