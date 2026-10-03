/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real remote-key regression; memoirUrl must point to tests/tv_fixture.py, never the family server. */
class CinemaRemoteTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun fullscreenKeepsRemotePlaybackFocusAndPanelBack() {
        val url = InstrumentationRegistry.getArguments().getString("memoirUrl")
        assumeTrue("Use the disposable synthetic memoir-tv fixture", url != null)
        require(url!!.contains(":18765") || url.contains(":18766")) { "Only synthetic fixture ports are allowed" }
        val cleared = CountDownLatch(1)
        compose.runOnUiThread {
            android.webkit.CookieManager.getInstance().setCookie(url, "memoir_session=; Max-Age=0; Path=/") { cleared.countDown() }
        }
        assertTrue(cleared.await(5, TimeUnit.SECONDS))
        compose.onNodeWithText("搜索你喜欢的内容，或输入网址").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(url)
        compose.onNodeWithText("前往").performClick()
        awaitJs("window.memoirReady === true && !!document.querySelector('[name=username]')")
        compose.onNodeWithText("浏览网页").performClick()
        js("document.querySelector('[name=username]').value='test-owner';document.querySelector('[name=password]').value='Fixture-Password-2026';document.querySelector('[type=submit]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("document.querySelectorAll('.memory-card').length === 2")
        js("document.querySelector('[data-action=open]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!!document.querySelector('#viewer-dialog[open] .cinema') && document.querySelector('video').readyState >= 2")
        js("document.querySelector('video').pause();document.querySelector('video').currentTime=0;document.querySelector('[data-fullscreen]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!!document.fullscreenElement")
        awaitJs("document.activeElement.hasAttribute('data-remote-play')")
        val started = js("document.querySelector('video').currentTime").toDouble()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!document.querySelector('video').paused && document.querySelector('video').currentTime > ${started + 0.2}")
        // First confirmation wakes an idle controller; second actually pauses.
        awaitJs("document.querySelector('.cinema').classList.contains('cinema-idle')")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!document.querySelector('.cinema').classList.contains('cinema-idle') && !document.querySelector('video').paused")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("document.querySelector('video').paused")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        awaitJs("document.activeElement.hasAttribute('data-remote-back')")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        awaitJs("document.activeElement.hasAttribute('data-remote-forward')")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("document.querySelector('video').currentTime >= 10")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        awaitJs("document.activeElement.hasAttribute('data-next')")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!!document.fullscreenElement && document.querySelector('.cinema-count').textContent.indexOf('2 / 2') >= 0")
        key(KeyEvent.KEYCODE_MEDIA_PAUSE)
        awaitJs("document.querySelector('video').paused")
        js("document.querySelector('[data-player-settings]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!document.querySelector('.cinema-panel').hidden")
        key(KeyEvent.KEYCODE_BACK)
        awaitJs("document.querySelector('.cinema-panel').hidden && !!document.fullscreenElement")
        key(KeyEvent.KEYCODE_BACK)
        awaitJs("!document.fullscreenElement && document.querySelector('#viewer-dialog').open")
        key(KeyEvent.KEYCODE_BACK)
        awaitJs("!document.querySelector('#viewer-dialog').open")
    }

    @Test fun genericFullscreenPageKeepsButtonsAndSelectionOperable() {
        val url = InstrumentationRegistry.getArguments().getString("memoirUrl")
        assumeTrue("Use the disposable synthetic memoir-tv fixture", url != null)
        require(url!!.contains(":18765") || url.contains(":18766"))
        compose.onNodeWithText("搜索你喜欢的内容，或输入网址").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(url)
        compose.onNodeWithText("前往").performClick()
        compose.waitUntil(10000) { compose.runOnUiThread { findWeb(compose.activity.window.decorView)?.progress == 100 } }
        val html = """
            <html><head><title>Fullscreen fixture</title><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>body{background:#111;color:white}button,select{font-size:28px;padding:20px;margin:10px}#stage{background:#111}</style></head>
            <body><div id="stage"><button id="full" onclick="document.getElementById('stage').requestFullscreen()">Fullscreen</button>
            <button id="action" onclick="window.activations=(window.activations||0)+1">Action</button>
            <select id="choice"><option>A</option><option>B</option></select></div></body></html>
        """.trimIndent()
        compose.runOnUiThread { findWeb(compose.activity.window.decorView)!!.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null) }
        awaitJs("document.title === 'Fullscreen fixture' && typeof window.__tvBrowserNavigate === 'function'")
        compose.onNodeWithText("浏览网页").performClick()
        js("document.querySelector('#full').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!!document.fullscreenElement")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        awaitJs("document.activeElement.id === 'action'")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("window.activations === 1")
        val downTime = android.os.SystemClock.uptimeMillis()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeySync(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        instrumentation.sendKeySync(KeyEvent(downTime, downTime + 100, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1))
        instrumentation.sendKeySync(KeyEvent(downTime, downTime + 150, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        awaitJs("window.activations === 2")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        awaitJs("document.activeElement.id === 'choice'")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        awaitJs("document.querySelector('#choice').selectedIndex === 1")
        js("document.querySelector('#action').focus();document.querySelector('#action').setAttribute('aria-disabled','true')")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals("2", js("window.activations"))
        key(KeyEvent.KEYCODE_BACK)
        awaitJs("!document.fullscreenElement")
        assertEquals("2", js("window.activations"))
    }

    private fun key(code: Int) {
        compose.waitUntil(5000) { compose.runOnUiThread { compose.activity.hasWindowFocus() } }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    private fun awaitJs(expression: String) {
        try { compose.waitUntil(20000) { js(expression) == "true" } }
        catch (failure: Throwable) {
            throw AssertionError("Expected $expression; state=" + js("JSON.stringify({active:document.activeElement.outerHTML,full:!!document.fullscreenElement,dialog:document.querySelector('#viewer-dialog')?.open,paused:document.querySelector('video')?.paused,time:document.querySelector('video')?.currentTime})"), failure)
        }
    }

    private fun js(expression: String): String {
        val latch = CountDownLatch(1)
        var result = "null"
        compose.runOnUiThread {
            findWeb(compose.activity.window.decorView)?.evaluateJavascript(expression) { result = it; latch.countDown() }
                ?: latch.countDown()
        }
        assertTrue("JavaScript callback timeout", latch.await(5, TimeUnit.SECONDS))
        return result
    }

    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWeb(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
