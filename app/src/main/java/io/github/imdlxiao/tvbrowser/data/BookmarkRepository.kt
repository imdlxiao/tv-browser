/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.data

import android.content.SharedPreferences
import io.github.imdlxiao.tvbrowser.browser.AddressResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val id: String, val title: String, val url: String, val caption: String)

interface BookmarkRepository {
    suspend fun load(): List<Bookmark>
    suspend fun save(bookmarks: List<Bookmark>)
}

/** Single-site collection; upgrades replace legacy entries once, then retain address edits. */
class LocalBookmarkRepository(private val preferences: SharedPreferences) : BookmarkRepository {
    override suspend fun load(): List<Bookmark> = withContext(Dispatchers.IO) {
        if (preferences.getInt("collection_version", 0) < COLLECTION_VERSION) {
            persist(defaults)
            return@withContext defaults
        }
        val stored = preferences.getString("items", null) ?: return@withContext defaults
        runCatching {
            val array = JSONArray(stored)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                Bookmark(item.getString("id"), item.getString("title"),
                    item.getString("url"), item.optString("caption"))
            }.also { items ->
                require(items.map { it.id }.distinct().size == items.size)
                require(items.all { it.id.isNotBlank() && AddressResolver.isWebUrl(it.url) })
            }
        }.getOrDefault(defaults).onlyMemoir()
    }

    override suspend fun save(bookmarks: List<Bookmark>) = withContext(Dispatchers.IO) {
        persist(bookmarks.onlyMemoir())
    }

    private fun persist(bookmarks: List<Bookmark>) {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(JSONObject().put("id", bookmark.id).put("title", bookmark.title)
                .put("url", bookmark.url).put("caption", bookmark.caption))
        }
        check(preferences.edit().putString("items", array.toString())
            .putInt("collection_version", COLLECTION_VERSION).commit())
    }

    companion object {
        private const val COLLECTION_VERSION = 2
        private val pinnedBookmark = Bookmark(
            "memoir", "回忆录", "http://192.168.5.12:8765/", "重温时光，珍藏每一刻",
        )

        private fun List<Bookmark>.onlyMemoir(): List<Bookmark> =
            listOf(firstOrNull { it.id == pinnedBookmark.id && AddressResolver.isWebUrl(it.url) } ?: pinnedBookmark)

        val defaults = listOf(pinnedBookmark)
    }
}
