/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class HomeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun remoteMovesBetweenBookmarksAndSearch() {
        compose.waitUntil(5000) {
            compose.onAllNodes(hasText("回忆录") and isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNode(hasText("百度") and hasClickAction()).assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_UP)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("想去哪里？").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("javascript:alert(1)")
        compose.onNodeWithText("前往").performClick()
        compose.onNodeWithText("请输入关键词或有效的 http / https 网址").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("常用网站").assertIsDisplayed()
    }

    @Test fun homeBackShowsExitConfirmationAndCanResume() {
        compose.waitUntil(5000) { compose.onAllNodesWithText("百度").fetchSemanticsNodes().isNotEmpty() }
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("结束这次探索？").assertIsDisplayed()
        compose.onNodeWithText("继续浏览").performClick()
        compose.onNodeWithText("常用网站").assertIsDisplayed()
    }

    private fun key(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.waitForIdle()
    }
}
