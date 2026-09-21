/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.home

import androidx.lifecycle.SavedStateHandle
import io.github.imdlxiao.tvbrowser.data.Bookmark
import io.github.imdlxiao.tvbrowser.data.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sites = listOf(Bookmark("sample", "示例", "https://example.com", "示例站点"))
    private val repository = object : BookmarkRepository {
        override suspend fun load() = sites
        override suspend fun save(bookmarks: List<Bookmark>) = Unit
    }
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `bookmarks load from repository`() = runTest(dispatcher) {
        val model = HomeViewModel(repository, SavedStateHandle())
        advanceUntilIdle()
        assertEquals(sites, model.uiState.value.bookmarks)
    }

    @Test fun `bookmark correction persists and updates current saved destination`() = runTest(dispatcher) {
        var stored = sites
        val writable = object : BookmarkRepository {
            override suspend fun load() = stored
            override suspend fun save(bookmarks: List<Bookmark>) { stored=bookmarks }
        }
        val handle = SavedStateHandle()
        val model = HomeViewModel(writable, handle)
        advanceUntilIdle()
        model.open(sites.first().url)
        model.updateBookmarkAddress("sample", "http://192.168.1.12:8765/")
        advanceUntilIdle()
        assertEquals("http://192.168.1.12:8765/", stored.first().url)
        assertEquals(stored.first().url, handle.get<String>("browserUrl"))
        model.updateBookmarkAddress("sample", "javascript:alert(1)")
        advanceUntilIdle()
        assertEquals("http://192.168.1.12:8765/", model.uiState.value.bookmarks.first().url)
    }

    @Test fun `invalid search stays on home and exposes error`() = runTest(dispatcher) {
        val model = HomeViewModel(repository, SavedStateHandle())
        model.updateQuery("javascript:alert(1)")
        assertFalse(model.search())
        assertNull(model.uiState.value.browserUrl)
        assertNotNull(model.uiState.value.error)
        model.updateQuery("电视")
        assertNull(model.uiState.value.error)
    }

    @Test fun `navigation survives recreation and home clears destination`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val model = HomeViewModel(repository, handle)
        model.updateQuery("example.com")
        assertTrue(model.search())
        val restored = HomeViewModel(repository, handle)
        assertEquals("example.com", restored.uiState.value.query)
        assertEquals("https://example.com", restored.uiState.value.browserUrl)
        restored.home()
        assertNull(handle.get<String>("browserUrl"))
        assertNull(restored.uiState.value.browserUrl)
    }
}
