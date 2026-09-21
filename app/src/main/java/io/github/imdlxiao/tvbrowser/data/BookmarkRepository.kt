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

/** Storage boundary; malformed local data falls back to built-in sites. */
class LocalBookmarkRepository(private val preferences: SharedPreferences) : BookmarkRepository {
    override suspend fun load(): List<Bookmark> = withContext(Dispatchers.IO) {
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
        }.getOrDefault(defaults).withPinnedBookmark()
    }

    override suspend fun save(bookmarks: List<Bookmark>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        bookmarks.withPinnedBookmark().forEach { bookmark ->
            array.put(JSONObject().put("id", bookmark.id).put("title", bookmark.title)
                .put("url", bookmark.url).put("caption", bookmark.caption))
        }
        check(preferences.edit().putString("items", array.toString()).commit())
    }

    companion object {
        private val pinnedBookmark = Bookmark(
            "memoir", "回忆录", "http://Abyss.local:8765/", "重温时光，珍藏每一刻",
        )

        /** Keep the pinned entry first even when upgrading an existing saved collection. */
        private fun List<Bookmark>.withPinnedBookmark(): List<Bookmark> =
            listOf(firstOrNull { it.id == pinnedBookmark.id && AddressResolver.isWebUrl(it.url) } ?: pinnedBookmark) + filterNot {
                it.id == pinnedBookmark.id ||
                    it.url.trimEnd('/').equals(pinnedBookmark.url.trimEnd('/'), ignoreCase = true)
            }

        val defaults = listOf(
            pinnedBookmark,
            Bookmark("baidu", "百度", "https://www.baidu.com", "搜索世界，发现答案"),
            Bookmark("bilibili", "哔哩哔哩", "https://www.bilibili.com", "让兴趣，在大屏相遇"),
            Bookmark("iqiyi", "爱奇艺", "https://www.iqiyi.com", "好故事，值得看见"),
            Bookmark("tencent", "腾讯视频", "https://v.qq.com", "把精彩留给此刻"),
            Bookmark("youku", "优酷", "https://www.youku.com", "你的下一部好片"),
            Bookmark("zhihu", "知乎", "https://www.zhihu.com", "每一个好奇，都有回响"),
        )
    }
}
