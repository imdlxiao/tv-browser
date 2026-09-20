/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/** Deterministic real-WebView regression test; no dependency on external websites. */
class BrowserNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigationCallbacksDoNotReloadAndBackUsesWebHistory() {
        FixtureServer().use { server ->
            compose.waitUntil(5000) { compose.onAllNodesWithText("百度").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("搜索你喜欢的内容，或输入网址").performClick()
            compose.onNode(hasSetTextAction()).performTextInput(server.url)
            compose.onNodeWithText("前往").performClick()
            awaitTitle("First page")
            assertEquals(1, server.firstPageRequests.get())

            compose.onNodeWithText("浏览网页").performClick()
            compose.runOnUiThread {
                val web = findWebView(compose.activity.window.decorView)!!
                assertTrue(web.hasFocus())
                web.evaluateJavascript("document.getElementById('next').focus()", null)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
            awaitTitle("Second page")
            compose.runOnUiThread {
                val web = findWebView(compose.activity.window.decorView)!!
                assertTrue("History entries: ${web.copyBackForwardList().size}", web.canGoBack())
            }
            compose.onNodeWithText("返回").performClick()
            awaitTitle("First page")

            // Activity recreation must restore WebView state without reopening the search dialog.
            compose.activityRule.scenario.recreate()
            awaitTitle("First page")
            compose.onNodeWithText("首页").performClick()
            compose.onNodeWithText("常用网站").assertIsDisplayed()
            compose.onNodeWithText("想去哪里？").assertDoesNotExist()
        }
    }

    private fun awaitTitle(title: String) {
        try {
            compose.waitUntil(15000) { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(5000) {
                compose.runOnUiThread { findWebView(compose.activity.window.decorView)?.progress == 100 }
            }
            compose.waitForIdle()
        } catch (failure: Throwable) {
            compose.onRoot().printToLog("BrowserTest")
            compose.runOnUiThread {
                val view = findWebView(compose.activity.window.decorView)
                android.util.Log.e("BrowserTest", "Expected=$title actual=${view?.title} url=${view?.url} back=${view?.canGoBack()}")
            }
            throw failure
        }
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            findWebView(view.getChildAt(i))?.let { return it }
        }
        return null
    }

    private class FixtureServer : AutoCloseable {
        private val socket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        val firstPageRequests = AtomicInteger()
        val url = "http://127.0.0.1:${socket.localPort}/first"
        private val worker = thread(isDaemon=true, name="browser-test-http") {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { client ->
                        client.soTimeout = 3000
                        val reader = client.getInputStream().bufferedReader()
                        val path = reader.readLine()?.split(' ')?.getOrNull(1) ?: "/"
                        while (!reader.readLine().isNullOrEmpty()) { /* Consume headers. */ }
                        val second = path == "/second"
                        if (path == "/first") firstPageRequests.incrementAndGet()
                        val title = if (second) "Second page" else "First page"
                        val body = "<html><head><title>$title</title></head><body><h1>$title</h1><a id='next' href='/second'>Next page</a></body></html>".toByteArray()
                        client.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(body)
                            flush()
                        }
                    }
                } catch (_: java.io.IOException) { if (socket.isClosed) break }
            }
        }
        override fun close() { socket.close(); worker.join(1000) }
    }
}
