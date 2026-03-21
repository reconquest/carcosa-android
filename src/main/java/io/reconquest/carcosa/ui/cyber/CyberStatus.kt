package io.reconquest.carcosa.ui.cyber

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "status-chip")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "status-alpha",
    )
    val dotAlpha = if (pulsing) alpha else 0.95f

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(2.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(dotAlpha)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun TerminalSpinner(
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = LocalCyberPalette.current.amber,
) {
    val transition = rememberInfiniteTransition(label = "terminal-spinner")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(800)),
        label = "spinner-phase",
    )
    val frames = listOf("◐", "◓", "◑", "◒")
    val frame = if (active) frames[phase.toInt().coerceIn(0, 3)] else "●"

    Text(
        text = frame,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

@Composable
fun SignalBars(
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "signal-bars")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "signal-pulse",
    )

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        repeat(3) { index ->
            val alpha = if (active) (pulse - index * 0.18f).coerceIn(0.18f, 1f) else 0.45f
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .width(3.dp)
                    .height((6 + index * 4).dp)
                    .alpha(alpha)
                    .background(color, RoundedCornerShape(1.dp)),
            )
        }
    }
}
