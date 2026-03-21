package io.reconquest.carcosa.ui.cyber

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CyberPanel(
    title: String? = null,
    modifier: Modifier = Modifier,
    accent: Color = LocalCyberPalette.current.cyan,
    selected: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = LocalCyberPalette.current
    val border = if (selected) accent.copy(alpha = 0.92f) else colors.panelBorder.copy(alpha = 0.82f)
    val background = if (selected) colors.panelHot else colors.panel

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background.copy(alpha = 0.94f), RoundedCornerShape(3.dp))
            .border(BorderStroke(1.dp, border), RoundedCornerShape(3.dp))
            .padding(1.dp),
    ) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = if (selected) 0.16f else 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ":: $title",
                    color = accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                trailing()
            }
        }

        Box(Modifier.padding(10.dp)) {
            content()
        }
    }
}

@Composable
fun CyberRule(
    modifier: Modifier = Modifier,
    color: Color = LocalCyberPalette.current.panelBorder,
    width: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(width, color.copy(alpha = 0.55f)),
    )
}
