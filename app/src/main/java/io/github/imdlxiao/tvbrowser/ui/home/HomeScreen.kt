/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.imdlxiao.tvbrowser.data.Bookmark
import io.github.imdlxiao.tvbrowser.ui.components.*
import io.github.imdlxiao.tvbrowser.ui.theme.*
import kotlinx.coroutines.flow.first

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(state: HomeUiState, onQuery: (String) -> Unit, onSearch: () -> Boolean, onOpen: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf("baidu") }
    val cardFocus = remember(state.bookmarks) { state.bookmarks.associate { it.id to FocusRequester() } }
    val searchFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val inputMode = LocalInputModeManager.current
    LaunchedEffect(state.bookmarks.isNotEmpty()) {
        if (state.bookmarks.isNotEmpty()) {
            inputMode.requestInputMode(InputMode.Keyboard)
            val selected = state.bookmarks.indexOfFirst { it.id == selectedId }
            if (selected >= 0) {
                if (selected >= 3) gridState.scrollToItem(selected + 3)
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.key } }
                    .first { selectedId in it }
                withFrameNanos { }
                cardFocus.getValue(selectedId).requestFocus()
            } else searchFocus.requestFocus()
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(
        listOf(Night, Color(0xFF102436), Night),
    ))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), state = gridState,
            contentPadding = PaddingValues(horizontal=40.dp, vertical=28.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            LineIcon(Glyph.Globe, Modifier.size(20.dp))
                            Text("TV BROWSER", color=Mint, fontSize=13.sp, letterSpacing=3.sp, fontWeight=FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("大屏之上，自在探索。", style=MaterialTheme.typography.headlineLarge)
                    }
                    Column(horizontalAlignment=Alignment.End) {
                        Text("放松坐好，让好奇心出发", color=Muted, fontSize=14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("简洁  /  专注  /  为电视而生", color=Muted.copy(alpha=.65f), fontSize=12.sp)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchEntry(state.query, Modifier.focusRequester(searchFocus)) { editing = true }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Text("常用网站", fontSize=20.sp, fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    Text("发现你的下一份精彩", color=Muted, fontSize=13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("精选导航  ·  06", color=Muted, fontSize=12.sp)
                }
            }
            itemsIndexed(state.bookmarks, key={ _, item -> item.id }) { index, bookmark ->
                BookmarkCard(bookmark, index,
                    Modifier.focusRequester(cardFocus.getValue(bookmark.id)),
                    onFocused = { selectedId = bookmark.id },
                    onClick = { onOpen(bookmark.url) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().padding(top=4.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("方向键  移动     ·     确认键  打开     ·     返回键  返回", color=Muted, fontSize=12.sp)
                    Text("让浏览回归简单", color=Mint.copy(alpha=.7f), fontSize=12.sp)
                }
            }
        }
    }
    if (editing) {
        SearchDialog(state, onQuery, onSearch, onDismiss = { editing = false })
    }
}

@Composable
private fun SearchEntry(query: String, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().height(62.dp).onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0xFF1A3C48) else Color(0xFF162638))
            .border(if (focused) 2.dp else 1.dp, if (focused) Mint else Color.White.copy(alpha=.1f), RoundedCornerShape(16.dp))
            .clickable(role=Role.Button, onClick=onClick).padding(horizontal=22.dp),
        verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(14.dp),
    ) {
        LineIcon(Glyph.Search)
        Text(query.ifBlank { "搜索你喜欢的内容，或输入网址" },
            Modifier.weight(1f), color=if (query.isBlank()) Muted else Color.White,
            fontSize=19.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
        Text("开始探索", color=Mint, fontSize=15.sp)
        LineIcon(Glyph.Arrow, Modifier.size(18.dp))
    }
}

@Composable
private fun BookmarkCard(bookmark: Bookmark, index: Int, modifier: Modifier, onFocused: () -> Unit, onClick: () -> Unit) {
    val accents = listOf(Color(0xFF80AAFF), Color(0xFFFF99BD), Color(0xFF8DE0AC),
        Color(0xFF8ACFFF), Color(0xFFFFB58D), Color(0xFFAFA4FF))
    val accent = accents[index % accents.size]
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, label="bookmarkScale")
    val background by animateColorAsState(if (focused) Color(0xFF233C51) else Panel, label="bookmarkColor")
    Column(
        modifier.fillMaxWidth().height(126.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
            .clip(RoundedCornerShape(18.dp)).background(background)
            .border(if (focused) 2.dp else 1.dp, if (focused) Mint else Color.White.copy(alpha=.065f), RoundedCornerShape(18.dp))
            .clickable(role=Role.Button, onClick=onClick).padding(18.dp),
        verticalArrangement=Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha=.14f)),
                contentAlignment=Alignment.Center) {
                Text(bookmark.title.take(1), color=accent, fontSize=24.sp, fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(bookmark.title, fontSize=23.sp, fontWeight=FontWeight.SemiBold, modifier=Modifier.weight(1f))
            LineIcon(Glyph.Arrow, Modifier.size(18.dp), if (focused) Mint else Muted.copy(alpha=.4f))
        }
        Text(bookmark.caption, color=Muted, fontSize=14.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchDialog(state: HomeUiState, onQuery: (String) -> Unit, onSearch: () -> Boolean, onDismiss: () -> Unit) {
    val inputFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() {
        keyboard?.hide()
        if (onSearch()) onDismiss()
    }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={ Text("想去哪里？") },
        text={
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("输入网址，或用百度搜索关键词。", color=Muted)
                OutlinedTextField(
                    value=state.query, onValueChange=onQuery, singleLine=true,
                    modifier=Modifier.fillMaxWidth().focusRequester(inputFocus),
                    placeholder={ Text("网址 / 搜索关键词") },
                    isError=state.error != null,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri, imeAction=ImeAction.Search),
                    keyboardActions=KeyboardActions(onSearch={ submit() }),
                )
                LaunchedEffect(Unit) { inputFocus.requestFocus(); keyboard?.show() }
                state.error?.let { Text(it, color=MaterialTheme.colorScheme.error, fontSize=14.sp) }
            }
        },
        confirmButton={ TvAction("前往", Glyph.Arrow) { submit() } },
        dismissButton={ TvAction("取消", Glyph.Back, onClick=onDismiss) },
    )
}
