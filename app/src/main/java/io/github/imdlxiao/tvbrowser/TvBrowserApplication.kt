/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser

import android.app.Application
import io.github.imdlxiao.tvbrowser.data.BookmarkRepository
import io.github.imdlxiao.tvbrowser.data.LocalBookmarkRepository

/** Small dependency container; only the data layer receives application context. */
class TvBrowserApplication : Application() {
    val bookmarks: BookmarkRepository by lazy {
        LocalBookmarkRepository(getSharedPreferences("bookmarks", MODE_PRIVATE))
    }
}