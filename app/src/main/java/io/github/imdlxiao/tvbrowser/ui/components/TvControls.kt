/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.imdlxiao.tvbrowser.ui.theme.*

enum class Glyph { Search, Back, Home, Reload, Arrow, Globe, Page }

/** Original vector strokes, shared by navigation and branding; no downloaded assets. */
@Composable
fun LineIcon(glyph: Glyph, modifier: Modifier = Modifier, color: Color = Mint) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(w*x1, h*y1), Offset(w*x2, h*y2), strokeWidth = stroke)
        when (glyph) {
            Glyph.Search -> {
                drawCircle(color, w*.28f, Offset(w*.42f, h*.42f), style = Stroke(stroke))
                line(.64f,.64f,.9f,.9f)
            }
            Glyph.Back -> { line(.2f,.5f,.85f,.5f); line(.2f,.5f,.48f,.22f); line(.2f,.5f,.48f,.78f) }
            Glyph.Arrow -> { line(.15f,.5f,.8f,.5f); line(.8f,.5f,.52f,.22f); line(.8f,.5f,.52f,.78f) }
            Glyph.Home -> {
                val p = Path().apply {
                    moveTo(w*.1f,h*.45f); lineTo(w*.5f,h*.1f); lineTo(w*.9f,h*.45f)
                    moveTo(w*.23f,h*.35f); lineTo(w*.23f,h*.87f); lineTo(w*.77f,h*.87f); lineTo(w*.77f,h*.35f)
                }
                drawPath(p,color,style=Stroke(stroke))
            }
            Glyph.Reload -> {
                drawArc(color, 40f, 295f, false, Offset(w*.15f,h*.15f),
                    androidx.compose.ui.geometry.Size(w*.7f,h*.7f), style=Stroke(stroke))
                line(.85f,.12f,.85f,.4f); line(.85f,.4f,.6f,.4f)
            }
            Glyph.Globe -> {
                drawCircle(color,w*.4f,style=Stroke(stroke))
                drawOval(color,Offset(w*.32f,h*.1f),androidx.compose.ui.geometry.Size(w*.36f,h*.8f),style=Stroke(stroke))
                line(.1f,.5f,.9f,.5f)
            }
            Glyph.Page -> {
                drawRoundRect(color,Offset(w*.15f,h*.12f),
                    androidx.compose.ui.geometry.Size(w*.7f,h*.76f),
                    androidx.compose.ui.geometry.CornerRadius(w*.08f),style=Stroke(stroke))
                line(.3f,.4f,.7f,.4f); line(.3f,.6f,.6f,.6f)
            }
        }
    }
}

/** A single focus target with an animated, high-contrast TV focus ring. */
@Composable
fun TvAction(label: String, glyph: Glyph, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val background by animateColorAsState(if (focused) Mint else Panel, label = "actionBackground")
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, label = "actionScale")
    val foreground = if (focused) Night else Color.White
    Row(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp)).background(background)
            .border(1.dp, if (focused) Color.White else Color.White.copy(alpha=.12f), RoundedCornerShape(12.dp))
            .clickable(role=Role.Button, onClick=onClick)
            .padding(horizontal=16.dp, vertical=12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LineIcon(glyph, Modifier.size(20.dp), foreground)
        Text(label, color=foreground, fontSize=16.sp)
    }
}