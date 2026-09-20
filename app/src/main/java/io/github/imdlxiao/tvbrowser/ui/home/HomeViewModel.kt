/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.imdlxiao.tvbrowser.browser.AddressResolver
import io.github.imdlxiao.tvbrowser.data.Bookmark
import io.github.imdlxiao.tvbrowser.data.BookmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val query: String = "",
    val bookmarks: List<Bookmark> = emptyList(),
    val error: String? = null,
    val browserUrl: String? = null,
)

/** Search and durable navigation state; never owns Android View references. */
class HomeViewModel(repository: BookmarkRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState(
        query = savedState["query"] ?: "", browserUrl = savedState["browserUrl"],
    ))
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val bookmarks = repository.load()
            mutableState.update { it.copy(bookmarks = bookmarks) }
        }
    }

    fun updateQuery(query: String) {
        savedState["query"] = query
        mutableState.update { it.copy(query = query, error = null) }
    }

    fun search(): Boolean {
        val url = AddressResolver.resolve(uiState.value.query)
        if (url == null) {
            mutableState.update { it.copy(error = "请输入关键词或有效的 http / https 网址") }
        } else open(url)
        return url != null
    }

    fun open(url: String) {
        if (!AddressResolver.isWebUrl(url)) return
        savedState["browserUrl"] = url
        mutableState.update { it.copy(browserUrl = url, error = null) }
    }

    fun home() {
        savedState["browserUrl"] = null
        mutableState.update { it.copy(browserUrl = null) }
    }
}
