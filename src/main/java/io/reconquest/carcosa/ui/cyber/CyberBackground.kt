package io.reconquest.carcosa.ui.cyber

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CyberBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalCyberPalette.current
    val transition = rememberInfiniteTransition(label = "scanline")
    val flicker by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "grid-flicker",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors.background)

            val grid = 28.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = colors.grid.copy(alpha = 0.26f * flicker),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += grid
            }

            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = colors.grid.copy(alpha = 0.20f * flicker),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += grid
            }

            val scanline = 4.dp.toPx()
            var sy = 0f
            while (sy <= size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.18f),
                    start = Offset(0f, sy),
                    end = Offset(size.width, sy),
                    strokeWidth = 1f,
                )
                sy += scanline
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.cyan.copy(alpha = 0.11f),
                        colors.background.copy(alpha = 0.0f),
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.10f),
                    radius = size.maxDimension * 0.70f,
                ),
            )
        }

        content()
    }
}
