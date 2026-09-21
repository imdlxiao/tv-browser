/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.github.imdlxiao.tvbrowser.data.LocalBookmarkRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkStorageTest {
    @Test fun updatedPinnedAddressSurvivesRepositoryReload() = runBlocking {
        val preferences = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("isolated-bookmark-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val repository = LocalBookmarkRepository(preferences)
            val original = repository.load()
            val edited = original.map { if (it.id == "memoir") it.copy(url="http://192.168.1.12:8765/") else it }
            repository.save(edited)
            val reloaded = LocalBookmarkRepository(preferences).load()
            assertEquals("memoir", reloaded.first().id)
            assertEquals("http://192.168.1.12:8765/", reloaded.first().url)
            assertEquals(original.size, reloaded.size)
        } finally { preferences.edit().clear().commit() }
    }
}
