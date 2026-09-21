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
import androidx.compose.ui.platform.LocalWindowInfo
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
fun BrowserScreen(initialUrl: String, onHome: () -> Unit, saveBookmark: ((String) -> Unit)? = null) {
    val session = rememberSaveable(saver=Saver(
        save={ value: BrowserSession -> value.saveState() },
        restore={ BrowserSession(it) },
    )) { BrowserSession() }
    val state = session.state
    var rendererGeneration by remember { mutableIntStateOf(0) }
    val toolbarFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    var editingAddress by remember { mutableStateOf(false) }
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
    LaunchedEffect(state.error, windowFocused) {
        if (windowFocused) {
            withFrameNanos { }
            if (state.error != null) retryFocus.requestFocus() else toolbarFocus.requestFocus()
        }
    }
    LaunchedEffect(state.notice) {
        if (state.notice != null) { delay(4000); session.dismissNotice() }
    }
    BackHandler { session.back(onHome) }
    fun reload() {
        if (state.rendererLost) rendererGeneration++ else session.reload()
    }
    if (editingAddress) AddressDialog(state.url, { editingAddress=false }, saveBookmark != null) { url, save ->
        editingAddress=false
        if (save) saveBookmark?.invoke(url)
        if (state.rendererLost) { session.load(url); rendererGeneration++ } else session.load(url)
    }
    Box(Modifier.fillMaxSize().background(Night)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp)
                    .onPreviewKeyEvent {
                        if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                            if (state.error == null) session.focusPage() else retryFocus.requestFocus()
                            true
                        } else false
                    },
                horizontalArrangement=Arrangement.spacedBy(10.dp),
                verticalAlignment=Alignment.CenterVertically,
            ) {
                TvAction("返回", Glyph.Back, Modifier.focusRequester(toolbarFocus)) {
                    session.back(onHome)
                }
                TvAction("首页", Glyph.Home, onClick=onHome)
                Column(Modifier.weight(1f).padding(horizontal=8.dp)) {
                    Text(state.title, maxLines=1, overflow=TextOverflow.Ellipsis, fontSize=16.sp)
                    Text(state.url, color=Muted, maxLines=1, overflow=TextOverflow.Ellipsis, fontSize=12.sp)
                }
                TvAction("刷新", Glyph.Reload) { reload() }
                TvAction("地址", Glyph.Search) { editingAddress=true }
                TvAction("浏览网页", Glyph.Page) { if (state.error == null) session.focusPage() else retryFocus.requestFocus() }
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
                        update={ web ->
                            web.visibility = if (state.error == null) android.view.View.VISIBLE else android.view.View.INVISIBLE
                            web.isFocusable = state.error == null
                            web.isFocusableInTouchMode = state.error == null
                        },
                        onRelease={ session.release(it) },
                    )
                }
                if (state.error != null) {
                    BrowserErrorPanel(state, retryFocus, { reload() }, onHome, { session.diagnose() }, { editingAddress=true })
                }
                state.notice?.let { notice ->
                    Surface(Modifier.align(Alignment.BottomCenter).padding(20.dp),
                        color=Panel, shape=MaterialTheme.shapes.medium) {
                        Text(notice, Modifier.padding(horizontal=20.dp, vertical=14.dp), fontSize=16.sp)
                    }
                }
            }
            Text("方向键选中 · 确认键操作 · 输入框上下换项/左右移动光标 · 菜单键回工具栏 · 返回优先关闭网页弹窗",
                Modifier.padding(horizontal=16.dp, vertical=6.dp), color=Muted, fontSize=12.sp)
        }
        session.fullscreenView?.let { custom ->
            AndroidView(factory={ custom }, modifier=Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black),
                update={ it.requestFocus() })
        }
    }
}
