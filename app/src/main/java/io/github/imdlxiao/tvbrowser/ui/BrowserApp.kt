/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.imdlxiao.tvbrowser.ui.browser.BrowserScreen
import io.github.imdlxiao.tvbrowser.ui.components.Glyph
import io.github.imdlxiao.tvbrowser.ui.components.TvAction
import io.github.imdlxiao.tvbrowser.ui.home.HomeScreen
import io.github.imdlxiao.tvbrowser.ui.home.HomeViewModel

/** Two destinations, with the home scroll/focus state retained while browsing. */
@Composable
fun BrowserApp(model: HomeViewModel) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val savedPages = rememberSaveableStateHolder()
    var showExit by remember { mutableStateOf(false) }
    val activity = LocalActivity.current
    val url = state.browserUrl
    if (url == null) {
        savedPages.SaveableStateProvider("home") {
            HomeScreen(state, model::updateQuery, model::search, model::open)
        }
        BackHandler { showExit = true }
    } else {
        BrowserScreen(url, model::home)
    }
    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text("结束这次探索？") },
            text = { Text("退出浏览器，返回电视桌面。") },
            confirmButton = { TvAction("退出", Glyph.Arrow) { activity?.finish() } },
            dismissButton = { TvAction("继续浏览", Glyph.Back) { showExit = false } },
        )
    }
}
