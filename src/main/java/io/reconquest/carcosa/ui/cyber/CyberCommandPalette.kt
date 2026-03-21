package io.reconquest.carcosa.ui.cyber

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class CommandEntry(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val onClick: () -> Unit,
    val actionLabel: String = "RUN",
)

@Composable
fun CyberCommandPalette(
    query: String,
    entries: List<CommandEntry>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCyberPalette.current
    val focusRequester = FocusRequester()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        CyberPanel(
            title = "COMMAND PALETTE",
            accent = colors.cyan,
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 42.dp)
                .fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "/",
                        color = colors.amber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.text,
                            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        ),
                        cursorBrush = SolidColor(colors.acid),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (query.isBlank()) {
                                Text(
                                    text = "type a token, repo, or action",
                                    color = colors.muted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            inner()
                        },
                    )
                    Text(
                        text = "ESC",
                        color = colors.muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.panelBorder),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(entries.take(12)) { entry ->
                        CommandPaletteRow(
                            entry = entry,
                            query = query,
                            onClick = {
                                entry.onClick()
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandPaletteRow(
    entry: CommandEntry,
    query: String,
    onClick: () -> Unit,
) {
    val colors = LocalCyberPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(entry.accent.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            text = "▸",
            color = entry.accent,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = neonHighlight(entry.title, query, colors.text, colors.acid),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            if (entry.subtitle.isNotBlank()) {
                Text(
                    text = entry.subtitle,
                    color = colors.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = entry.actionLabel.uppercase(),
            color = entry.accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
    }
}
