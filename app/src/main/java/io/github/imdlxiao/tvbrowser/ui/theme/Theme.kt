/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TvBrowserTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint, onPrimary = Night, secondary = Color(0xFF93B8FF),
            background = Night, surface = Panel, onSurface = Color(0xFFF2F6FC),
            onBackground = Color(0xFFF2F6FC), onSurfaceVariant = Muted,
            error = Color(0xFFFFACA7),
        ),
        typography = Typography,
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Night, content = content)
    }
}
