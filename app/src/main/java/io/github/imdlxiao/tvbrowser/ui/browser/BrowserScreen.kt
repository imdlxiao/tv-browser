/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.imdlxiao.tvbrowser.browser.BrowserSession
import io.github.imdlxiao.tvbrowser.ui.components.*
import io.github.imdlxiao.tvbrowser.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun BrowserScreen(initialUrl: String, onHome: () -> Unit) {
    val session = rememberSaveable(saver=Saver(
        save={ value: BrowserSession -> value.saveState() },
        restore={ BrowserSession(it) },
    )) { BrowserSession() }
    val state = session.state
    var rendererGeneration by remember { mutableIntStateOf(0) }
    val toolbarFocus = remember { FocusRequester() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    session.requestToolbarFocus = { toolbarFocus.requestFocus() }
    DisposableEffect(session, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> session.resume()
                Lifecycle.Event.ON_PAUSE -> session.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            session.requestToolbarFocus = {}
        }
    }
    LaunchedEffect(Unit) { toolbarFocus.requestFocus() }
    LaunchedEffect(state.notice) {
        if (state.notice != null) { delay(4000); session.dismissNotice() }
    }
    BackHandler { if (!session.goBack()) onHome() }
    fun reload() {
        if (state.rendererLost) rendererGeneration++ else session.reload()
    }
    Column(Modifier.fillMaxSize().background(Night)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp)
                .onPreviewKeyEvent {
                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown && state.error == null) {
                        session.focusPage(); true
                    } else false
                },
            horizontalArrangement=Arrangement.spacedBy(10.dp),
            verticalAlignment=Alignment.CenterVertically,
        ) {
            TvAction("返回", Glyph.Back, Modifier.focusRequester(toolbarFocus)) {
                if (!session.goBack()) onHome()
            }
            TvAction("首页", Glyph.Home, onClick=onHome)
            Column(Modifier.weight(1f).padding(horizontal=8.dp)) {
                Text(state.title, maxLines=1, overflow=TextOverflow.Ellipsis, fontSize=16.sp)
                Text(state.url, color=Muted, maxLines=1, overflow=TextOverflow.Ellipsis, fontSize=12.sp)
            }
            TvAction("刷新", Glyph.Reload) { reload() }
            TvAction("浏览网页", Glyph.Page) { session.focusPage() }
        }
        if (state.loading) LinearProgressIndicator(
            progress={ state.progress / 100f }, modifier=Modifier.fillMaxWidth().height(3.dp),
            color=Mint, trackColor=Panel,
        ) else Spacer(Modifier.height(3.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(rendererGeneration) {
                AndroidView(
                    factory={ session.createView(it, state.url.ifBlank { initialUrl }) },
                    modifier=Modifier.fillMaxSize(),
                    onRelease={ session.release(it) },
                )
            }
            if (state.error != null) {
                Column(
                    Modifier.fillMaxSize().background(Night).padding(32.dp),
                    verticalArrangement=Arrangement.Center,
                    horizontalAlignment=Alignment.CenterHorizontally,
                ) {
                    LineIcon(Glyph.Globe, Modifier.size(52.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("暂时没能抵达", style=MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(state.error, color=Muted)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        TvAction("重新打开", Glyph.Reload) { reload() }
                        TvAction("回到首页", Glyph.Home, onClick=onHome)
                    }
                }
            }
            state.notice?.let { notice ->
                Surface(Modifier.align(Alignment.BottomCenter).padding(20.dp),
                    color=Panel, shape=MaterialTheme.shapes.medium) {
                    Text(notice, Modifier.padding(horizontal=20.dp, vertical=14.dp), fontSize=16.sp)
                }
            }
        }
        Text("方向键浏览网页  ·  网页顶部按 ↑ 或菜单键回到工具栏  ·  返回键回到上一页",
            Modifier.padding(horizontal=16.dp, vertical=6.dp), color=Muted, fontSize=12.sp)
    }
}