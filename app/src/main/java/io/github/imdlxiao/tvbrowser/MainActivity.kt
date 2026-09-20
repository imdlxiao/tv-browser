/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.imdlxiao.tvbrowser.ui.BrowserApp
import io.github.imdlxiao.tvbrowser.ui.home.HomeViewModel
import io.github.imdlxiao.tvbrowser.ui.theme.TvBrowserTheme

/** Android window configuration and application composition root only. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val repository = (application as TvBrowserApplication).bookmarks
        val model = ViewModelProvider(this, viewModelFactory {
            initializer { HomeViewModel(repository, createSavedStateHandle()) }
        })[HomeViewModel::class.java]
        setContent { TvBrowserTheme { BrowserApp(model) } }
    }
}