/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.browser

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout

/** Intercept remote events before a WebView provider's custom view can swallow them. */
internal class FullscreenHost(
    context: Context,
    custom: View,
    private val routeKey: (KeyEvent) -> Boolean,
) : FrameLayout(context) {
    init {
        isFocusable = true
        isFocusableInTouchMode = true
        addView(custom, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        routeKey(event) || super.dispatchKeyEvent(event)
}
