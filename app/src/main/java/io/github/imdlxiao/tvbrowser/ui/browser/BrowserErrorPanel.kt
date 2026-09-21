/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.imdlxiao.tvbrowser.browser.BrowserUiState
import io.github.imdlxiao.tvbrowser.ui.components.*
import io.github.imdlxiao.tvbrowser.ui.theme.*

@Composable
fun BrowserErrorPanel(state: BrowserUiState, focus: FocusRequester, retry: () -> Unit,
                      home: () -> Unit, diagnose: () -> Unit, edit: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Night).verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("网页未能打开", style=MaterialTheme.typography.headlineMedium)
        Text(state.error.orEmpty(), fontSize=17.sp, color=MaterialTheme.colorScheme.onSurface)
        Text(state.engine, fontSize=12.sp, color=Muted)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            TvAction("重新打开", Glyph.Reload, Modifier.focusRequester(focus), retry)
            TvAction("连接诊断", Glyph.Globe, onClick=diagnose)
            TvAction("修改地址", Glyph.Search, onClick=edit)
            TvAction("回到首页", Glyph.Home, onClick=home)
        }
        state.diagnostics?.let { Text(it, fontSize=15.sp, color=Mint) }
    }
}
