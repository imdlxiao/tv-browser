/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Optional cross-project integration; requires the disposable memoir-tv tests/tv_fixture.py server. */
class MemoirRemoteTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun oldWebViewLoginPlaybackAndRemoteBack() {
        val url = InstrumentationRegistry.getArguments().getString("memoirUrl")
        assumeTrue("Pass memoirUrl pointing to the synthetic test library", url != null)
        val cleared = CountDownLatch(1)
        compose.runOnUiThread { android.webkit.CookieManager.getInstance().setCookie(url, "memoir_session=; Max-Age=0; Path=/") { cleared.countDown() } }
        assertTrue(cleared.await(5, TimeUnit.SECONDS))
        compose.onNodeWithText("搜索你喜欢的内容，或输入网址").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(url!!)
        compose.onNodeWithText("前往").performClick()
        awaitJs("window.memoirReady === true && !!document.querySelector('[name=username]')")
        val needsLegacy = js("(function(){try{new Function('return a?.b ?? 1');return false;}catch(e){return true;}})()")
        assertEquals(needsLegacy, js("!!document.querySelector('script[src*=compat]')"))
        compose.onNodeWithText("浏览网页").performClick()
        awaitJs("document.activeElement.name === 'username'")
        // Credentials are public fixtures. Navigation and activation use real Android keys.
        js("document.activeElement.value='test-owner'")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        awaitJs("document.activeElement.name === 'password'")
        js("document.activeElement.value='Fixture-Password-2026'")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        awaitJs("document.activeElement.type === 'submit'")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("document.querySelectorAll('.memory-card').length === 2")
        assertEquals("true", js("document.body.classList.contains('tv-mode')"))
        awaitJs("document.querySelector('.media-frame').getBoundingClientRect().height >= 120")
        capture("tv-home")
        compose.onNodeWithText("浏览网页").performClick()
        // Direction keys can move away from the first focused control.
        val before = js("document.activeElement.outerHTML")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.waitUntil(5000) { js("document.activeElement.outerHTML") != before }
        js("document.querySelector('[data-action=open]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!!document.querySelector('#viewer-dialog[open] video')")
        awaitJs("document.querySelector('video').readyState >= 2")
        awaitJs("document.querySelector('video').getBoundingClientRect().height > 120")
        awaitJs("(function(){var r=document.querySelector('#viewer-dialog').getBoundingClientRect();return r.height > 240 && r.top >= 0 && r.bottom <= innerHeight+1;})()")
        capture("tv-player")
        js("document.querySelector('video').pause();document.querySelector('[data-remote-play]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!document.querySelector('video').paused")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("document.querySelector('video').paused")
        js("document.querySelector('[data-remote-forward]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("document.querySelector('video').currentTime >= 10")
        js("document.querySelector('[data-fullscreen]').focus()")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        awaitJs("!!(document.fullscreenElement || document.webkitFullscreenElement)")
        key(KeyEvent.KEYCODE_BACK)
        awaitJs("!(document.fullscreenElement || document.webkitFullscreenElement)")
        awaitJs("document.querySelector('#viewer-dialog').open")
        key(KeyEvent.KEYCODE_BACK)
        awaitJs("!document.querySelector('#viewer-dialog').open")
        assertEquals("true", js("!!document.querySelector('.memory-card')"))
        key(KeyEvent.KEYCODE_MENU)
        compose.waitUntil(5000) { compose.onAllNodes(hasText("返回") and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("刷新").performClick()
        awaitJs("document.querySelectorAll('.memory-card').length === 2 && window.memoirReady === true")
        assertEquals("false", js("!!document.querySelector('#compat-error')"))
    }

    private fun key(code: Int) {
        try { compose.waitUntil(5000) { compose.runOnUiThread { compose.activity.hasWindowFocus() } } }
        catch (failure: Throwable) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val output = instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/remote-test.png")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
            throw failure
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.waitForIdle()
    }
    private fun awaitJs(expression: String) {
        try { compose.waitUntil(20000) { js(expression) == "true" } }
        catch (failure: Throwable) {
            android.util.Log.e("TVLayout", js("(function(){var d=document.querySelector('#viewer-dialog'),s=document.querySelector('.viewer-stage');return JSON.stringify({viewport:[innerWidth,innerHeight],rect:d && d.getBoundingClientRect(),maxHeight:d && getComputedStyle(d).maxHeight,height:d && getComputedStyle(d).height,overflow:d && getComputedStyle(d).overflow,stage:s && s.getBoundingClientRect(),stageDisplay:s && getComputedStyle(s).display,stageHeight:s && getComputedStyle(s).height});})()"))
            throw AssertionError("Expected: $expression; DOM: " + js("JSON.stringify({url:location.href,active:document.activeElement.outerHTML,ready:window.memoirReady,error:document.querySelector('#compat-error') && document.querySelector('#compat-error').textContent})"), failure)
        }
    }
    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("screenshots") != "true") return
        js("window.__captureReady=false;requestAnimationFrame(function(){requestAnimationFrame(function(){window.__captureReady=true;});});")
        awaitJs("window.__captureReady === true")
        Thread.sleep(250) // Let the emulator compositor present the already laid-out test frame.
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/Download/$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }
    private fun js(expression: String): String {
        val latch = CountDownLatch(1)
        var result = "null"
        compose.runOnUiThread {
            findWeb(compose.activity.window.decorView)?.evaluateJavascript(expression) {
                result=it; latch.countDown()
            } ?: latch.countDown()
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
