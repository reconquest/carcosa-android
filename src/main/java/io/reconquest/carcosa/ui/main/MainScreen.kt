package io.reconquest.carcosa.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reconquest.carcosa.lib.Repo
import io.reconquest.carcosa.lib.Token
import io.reconquest.carcosa.ui.cyber.CommandEntry
import io.reconquest.carcosa.ui.cyber.CyberBackground
import io.reconquest.carcosa.ui.cyber.CyberPanel
import io.reconquest.carcosa.ui.cyber.CyberCommandPalette
import io.reconquest.carcosa.ui.cyber.CyberTheme
import io.reconquest.carcosa.ui.cyber.LocalCyberPalette
import io.reconquest.carcosa.ui.cyber.SignalBars
import io.reconquest.carcosa.ui.cyber.StatusChip
import io.reconquest.carcosa.ui.cyber.TerminalSpinner
import io.reconquest.carcosa.ui.cyber.fuzzyMatchPositions
import io.reconquest.carcosa.ui.cyber.fuzzyScore
import io.reconquest.carcosa.ui.cyber.neonHighlight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuditLevel {
    Info,
    Success,
    Warning,
    Error,
}

data class AuditEvent(
    val type: String,
    val message: String,
    val level: AuditLevel = AuditLevel.Info,
    val timestamp: Long = System.currentTimeMillis(),
)

@Composable
fun MainScreen(
    repos: List<Repo>,
    loading: Boolean,
    syncing: Boolean,
    error: String?,
    auditEvents: List<AuditEvent>,
    onSync: () -> Unit,
    onAddRepo: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onUnlockRepo: (Repo) -> Unit,
    onViewToken: (Token) -> Unit,
    onCopyToken: (Token) -> Unit,
) {
    CyberTheme {
        CyberMainSurface(
            repos = repos,
            loading = loading,
            syncing = syncing,
            error = error,
            auditEvents = auditEvents,
            onSync = onSync,
            onAddRepo = onAddRepo,
            onSettings = onSettings,
            onAbout = onAbout,
            onUnlockRepo = onUnlockRepo,
            onViewToken = onViewToken,
            onCopyToken = onCopyToken,
        )
    }
}

@Composable
private fun CyberMainSurface(
    repos: List<Repo>,
    loading: Boolean,
    syncing: Boolean,
    error: String?,
    auditEvents: List<AuditEvent>,
    onSync: () -> Unit,
    onAddRepo: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onUnlockRepo: (Repo) -> Unit,
    onViewToken: (Token) -> Unit,
    onCopyToken: (Token) -> Unit,
) {
    val colors = LocalCyberPalette.current
    var paletteVisible by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedRepoId by rememberSaveable { mutableStateOf<String?>(null) }
    var visibleSecret by remember { mutableStateOf<Token?>(null) }

    LaunchedEffect(repos.map { it.id }) {
        if (selectedRepoId == null || repos.none { it.id == selectedRepoId }) {
            selectedRepoId = repos.firstOrNull()?.id
        }
    }

    BackHandler(enabled = paletteVisible) {
        paletteVisible = false
    }

    CyberBackground(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Slash) {
                    paletteVisible = true
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TerminalTopBar(
                syncing = syncing,
                onSync = onSync,
                onAddRepo = onAddRepo,
                onSettings = onSettings,
                onAbout = onAbout,
            )

            SearchPrompt(
                query = query,
                onClick = { paletteVisible = true },
            )

            if (error != null) {
                ErrorPanel(error)
            }

            BoxWithConstraints(Modifier.weight(1f)) {
                if (maxWidth >= 840.dp) {
                    SplitVaultLayout(
                        repos = repos,
                        loading = loading,
                        syncing = syncing,
                        selectedRepoId = selectedRepoId,
                        query = query,
                        auditEvents = auditEvents,
                        onSelectRepo = { selectedRepoId = it.id },
                        onUnlockRepo = onUnlockRepo,
                        onViewToken = { visibleSecret = it; onViewToken(it) },
                        onCopyToken = onCopyToken,
                        onAddRepo = onAddRepo,
                    )
                } else {
                    CompactVaultLayout(
                        repos = repos,
                        loading = loading,
                        syncing = syncing,
                        query = query,
                        auditEvents = auditEvents,
                        onSelectRepo = { selectedRepoId = it.id },
                        onUnlockRepo = onUnlockRepo,
                        onViewToken = { visibleSecret = it; onViewToken(it) },
                        onCopyToken = onCopyToken,
                        onAddRepo = onAddRepo,
                    )
                }
            }
        }

        if (paletteVisible) {
            CyberCommandPalette(
                query = query,
                entries = commandEntries(
                    repos = repos,
                    query = query,
                    onSync = onSync,
                    onAddRepo = onAddRepo,
                    onSettings = onSettings,
                    onAbout = onAbout,
                    onUnlockRepo = onUnlockRepo,
                    onViewToken = { visibleSecret = it; onViewToken(it) },
                    onCopyToken = onCopyToken,
                ),
                onQueryChange = { query = it },
                onDismiss = { paletteVisible = false },
            )
        }

        visibleSecret?.let { token ->
            SecretDialog(
                token = token,
                onDismiss = { visibleSecret = null },
                onCopy = {
                    onCopyToken(token)
                    visibleSecret = null
                },
            )
        }
    }
}

@Composable
private fun TerminalTopBar(
    syncing: Boolean,
    onSync: () -> Unit,
    onAddRepo: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val colors = LocalCyberPalette.current

    CyberPanel(accent = colors.amber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TerminalSpinner(active = syncing, color = if (syncing) colors.amber else colors.acid)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "CARCOSA :: VAULT",
                    color = colors.amber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (syncing) "sync bus active / native core busy" else "auth session online / native core armed",
                    color = colors.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CyberButton("+REPO", colors.cyan, onAddRepo)
            CyberButton(if (syncing) "SYNC…" else "SYNC", colors.amber, onSync, enabled = !syncing)
            CyberButton("CFG", colors.muted, onSettings)
            CyberButton("?", colors.muted, onAbout)
        }
    }
}

@Composable
private fun SearchPrompt(query: String, onClick: () -> Unit) {
    val colors = LocalCyberPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
            .border(1.dp, colors.panelBorder.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("/", color = colors.amber, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(8.dp))
        Text(
            text = query.ifBlank { "open command palette: search tokens, repos, actions" },
            color = if (query.isBlank()) colors.muted else colors.text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("TAP OR /", color = colors.cyan, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ErrorPanel(error: String) {
    val colors = LocalCyberPalette.current
    CyberPanel(title = "ERROR BUS", accent = colors.red) {
        Text(error, color = colors.red, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CompactVaultLayout(
    repos: List<Repo>,
    loading: Boolean,
    syncing: Boolean,
    query: String,
    auditEvents: List<AuditEvent>,
    onSelectRepo: (Repo) -> Unit,
    onUnlockRepo: (Repo) -> Unit,
    onViewToken: (Token) -> Unit,
    onCopyToken: (Token) -> Unit,
    onAddRepo: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) {
            item { LoadingPanel(syncing = syncing) }
        } else if (repos.isEmpty()) {
            item { EmptyVaultPanel(onAddRepo) }
        } else {
            repos.forEach { repo ->
                item(key = "repo-${repo.id}") {
                    RepoDataPanel(
                        repo = repo,
                        selected = false,
                        syncing = syncing,
                        onSelect = { onSelectRepo(repo) },
                        onUnlock = { onUnlockRepo(repo) },
                    )
                }
                items(
                    items = filteredTokens(repo, query),
                    key = { token -> "${repo.id}-${token.name}" },
                ) { token ->
                    TokenTerminalRow(
                        token = token,
                        query = query,
                        onView = { onViewToken(token) },
                        onCopy = { onCopyToken(token) },
                    )
                }
            }
            item { AuditLogPane(auditEvents = auditEvents.takeLast(8), compact = true) }
        }
    }
}

@Composable
private fun SplitVaultLayout(
    repos: List<Repo>,
    loading: Boolean,
    syncing: Boolean,
    selectedRepoId: String?,
    query: String,
    auditEvents: List<AuditEvent>,
    onSelectRepo: (Repo) -> Unit,
    onUnlockRepo: (Repo) -> Unit,
    onViewToken: (Token) -> Unit,
    onCopyToken: (Token) -> Unit,
    onAddRepo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CyberPanel(
            title = "REPOSITORIES",
            accent = LocalCyberPalette.current.cyan,
            modifier = Modifier
                .width(330.dp)
                .fillMaxHeight(),
        ) {
            if (loading) {
                LoadingPanel(syncing = syncing)
            } else if (repos.isEmpty()) {
                EmptyVaultPanel(onAddRepo)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(repos, key = { repo -> repo.id ?: repoTitle(repo) }) { repo ->
                        RepoDataPanel(
                            repo = repo,
                            selected = repo.id == selectedRepoId,
                            syncing = syncing,
                            onSelect = { onSelectRepo(repo) },
                            onUnlock = { onUnlockRepo(repo) },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val selectedRepo = repos.firstOrNull { it.id == selectedRepoId } ?: repos.firstOrNull()
            CyberPanel(
                title = selectedRepo?.let { "TOKENS :: ${repoTitle(it)}" } ?: "TOKENS",
                accent = LocalCyberPalette.current.acid,
                modifier = Modifier.weight(1f),
            ) {
                if (selectedRepo == null) {
                    EmptyVaultPanel(onAddRepo)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(
                            items = filteredTokens(selectedRepo, query),
                            key = { token -> "${selectedRepo.id}-${token.name}" },
                        ) { token ->
                            TokenTerminalRow(
                                token = token,
                                query = query,
                                onView = { onViewToken(token) },
                                onCopy = { onCopyToken(token) },
                            )
                        }
                    }
                }
            }

            AuditLogPane(
                auditEvents = auditEvents.takeLast(8),
                compact = false,
                modifier = Modifier.height(170.dp),
            )
        }
    }
}

@Composable
private fun LoadingPanel(syncing: Boolean) {
    val colors = LocalCyberPalette.current
    CyberPanel(title = "SCAN", accent = colors.amber) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TerminalSpinner(active = true, color = colors.amber)
            Text(
                text = if (syncing) "syncing repositories…" else "scanning native vault state…",
                color = colors.text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EmptyVaultPanel(onAddRepo: () -> Unit) {
    val colors = LocalCyberPalette.current
    CyberPanel(title = "NO REPOSITORIES", accent = colors.amber) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "vault index is empty; connect a carcosa git repository to begin",
                color = colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            CyberButton("CONNECT REPOSITORY", colors.cyan, onAddRepo)
        }
    }
}

@Composable
private fun RepoDataPanel(
    repo: Repo,
    selected: Boolean,
    syncing: Boolean,
    onSelect: () -> Unit,
    onUnlock: () -> Unit,
) {
    val colors = LocalCyberPalette.current
    val locked = repo.isLocked
    val accent = when {
        locked -> colors.red
        selected -> colors.cyan
        else -> colors.amber
    }

    CyberPanel(
        title = repoTitle(repo),
        accent = accent,
        selected = selected,
        modifier = Modifier.clickable(onClick = onSelect),
        trailing = {
            StatusChip(
                label = if (locked) "locked" else "ready",
                color = if (locked) colors.red else colors.acid,
                pulsing = syncing,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SignalBars(active = syncing, color = accent)
                Text(
                    text = repoAddress(repo),
                    color = colors.text,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Metadata("sync", repo.syncStat?.date ?: "never", colors.cyan)
                Metadata("+", (repo.syncStat?.added ?: 0).toString(), colors.acid)
                Metadata("-", (repo.syncStat?.deleted ?: 0).toString(), colors.red)
                Metadata("tokens", repo.tokens?.size?.toString() ?: "0", colors.amber)
            }

            if (locked) {
                CyberButton("UNLOCK", colors.red, onUnlock)
            }
        }
    }
}

@Composable
private fun TokenTerminalRow(
    token: Token,
    query: String,
    onView: () -> Unit,
    onCopy: () -> Unit,
) {
    val colors = LocalCyberPalette.current
    val name = token.name ?: "unnamed-token"
    val resource = token.resource.orEmpty()
    val login = token.login.orEmpty()
    val hasMetadata = resource.isNotBlank() || login.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.82f), RoundedCornerShape(2.dp))
            .border(1.dp, colors.panelBorder.copy(alpha = 0.52f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("TOK", color = colors.cyan, style = MaterialTheme.typography.labelSmall)
        Column(Modifier.weight(1f)) {
            Text(
                text = neonHighlight(name, query, colors.text, colors.acid),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasMetadata) {
                Text(
                    text = listOfNotNull(
                        resource.takeIf { it.isNotBlank() }?.let { "resource=$it" },
                        login.takeIf { it.isNotBlank() }?.let { "login=$it" },
                    ).joinToString("  "),
                    color = colors.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CyberButton("VIEW", colors.amber, onView)
        CyberButton("COPY", colors.acid, onCopy)
    }
}

@Composable
private fun AuditLogPane(
    auditEvents: List<AuditEvent>,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCyberPalette.current
    CyberPanel(
        title = "AUDIT LOG",
        accent = colors.cyan,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (auditEvents.isEmpty()) {
            Text("no events captured in this session", color = colors.muted, style = MaterialTheme.typography.bodySmall)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)) {
                auditEvents.reversed().forEach { event ->
                    AuditLogRow(event)
                }
            }
        }
    }
}

@Composable
private fun AuditLogRow(event: AuditEvent) {
    val colors = LocalCyberPalette.current
    val accent = when (event.level) {
        AuditLevel.Info -> colors.cyan
        AuditLevel.Success -> colors.acid
        AuditLevel.Warning -> colors.amber
        AuditLevel.Error -> colors.red
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(formatTime(event.timestamp), color = colors.muted, style = MaterialTheme.typography.labelSmall)
        Text(event.type.uppercase(), color = accent, style = MaterialTheme.typography.labelSmall)
        Text(
            event.message,
            color = colors.text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SecretDialog(
    token: Token,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
) {
    val colors = LocalCyberPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        titleContentColor = colors.amber,
        textContentColor = colors.text,
        title = {
            Text(token.name ?: "secret", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(
                text = token.payload ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
            )
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text("COPY", color = colors.acid)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = colors.muted)
            }
        },
    )
}

@Composable
private fun CyberButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = LocalCyberPalette.current
    val alpha = if (enabled) 1f else 0.36f
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f * alpha), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.75f * alpha), RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) color else colors.muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun Metadata(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label=", color = color, style = MaterialTheme.typography.labelSmall)
        Text(value, color = LocalCyberPalette.current.text, style = MaterialTheme.typography.labelSmall)
    }
}

private fun commandEntries(
    repos: List<Repo>,
    query: String,
    onSync: () -> Unit,
    onAddRepo: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onUnlockRepo: (Repo) -> Unit,
    onViewToken: (Token) -> Unit,
    onCopyToken: (Token) -> Unit,
): List<CommandEntry> {
    val intent = parseCommandIntent(query)
    val palette = mutableListOf<CommandEntry>()

    if (intent.verb == CommandVerb.General) {
        palette += CommandEntry("sync repositories", "action", Color(0xFFFFB000), onSync)
        palette += CommandEntry("connect repository", "action", Color(0xFF00E5FF), onAddRepo)
        palette += CommandEntry("open settings", "action", Color(0xFF6E7D86), onSettings)
        palette += CommandEntry("open about", "action", Color(0xFF6E7D86), onAbout)
    }

    repos.forEach { repo ->
        if (intent.verb == CommandVerb.General && repo.isLocked) {
            palette += CommandEntry(
                title = "unlock ${repoTitle(repo)}",
                subtitle = repoAddress(repo),
                accent = Color(0xFFFF355E),
                onClick = { onUnlockRepo(repo) },
                actionLabel = "UNLOCK",
            )
        }

        repo.tokens?.forEach { token ->
            val title = token.name ?: "unnamed-token"
            val context = listOfNotNull(
                repoTitle(repo),
                token.resource?.takeIf { it.isNotBlank() },
                token.login?.takeIf { it.isNotBlank() },
            ).joinToString(" / ")

            val entry = when (intent.verb) {
                CommandVerb.General -> CommandEntry(
                    title = title,
                    subtitle = "token / enter copies / $context",
                    accent = Color(0xFFB6FF00),
                    onClick = { onCopyToken(token) },
                    actionLabel = "COPY",
                )
                CommandVerb.Copy -> CommandEntry(
                    title = "copy $title",
                    subtitle = context,
                    accent = Color(0xFFB6FF00),
                    onClick = { onCopyToken(token) },
                    actionLabel = "COPY",
                )
                CommandVerb.View -> CommandEntry(
                    title = "view $title",
                    subtitle = context,
                    accent = Color(0xFFFFB000),
                    onClick = { onViewToken(token) },
                    actionLabel = "VIEW",
                )
            }
            palette += entry
        }
    }

    if (intent.matchQuery.isBlank()) {
        return palette.take(20)
    }

    return palette
        .filter {
            fuzzyMatchPositions(intent.matchQuery, it.title) != null ||
                fuzzyMatchPositions(intent.matchQuery, it.subtitle) != null
        }
        .sortedBy { minOf(fuzzyScore(intent.matchQuery, it.title), fuzzyScore(intent.matchQuery, it.subtitle)) }
}

private enum class CommandVerb {
    General,
    Copy,
    View,
}

private data class ParsedCommandIntent(
    val verb: CommandVerb,
    val matchQuery: String,
)

private fun parseCommandIntent(query: String): ParsedCommandIntent {
    val trimmed = query.trimStart()
    val lower = trimmed.lowercase(Locale.getDefault())

    return when {
        lower == "copy" -> ParsedCommandIntent(CommandVerb.Copy, "")
        lower.startsWith("copy ") -> ParsedCommandIntent(CommandVerb.Copy, trimmed.drop(5).trimStart())
        lower == "cp" -> ParsedCommandIntent(CommandVerb.Copy, "")
        lower.startsWith("cp ") -> ParsedCommandIntent(CommandVerb.Copy, trimmed.drop(3).trimStart())
        lower == "view" -> ParsedCommandIntent(CommandVerb.View, "")
        lower.startsWith("view ") -> ParsedCommandIntent(CommandVerb.View, trimmed.drop(5).trimStart())
        lower == "show" -> ParsedCommandIntent(CommandVerb.View, "")
        lower.startsWith("show ") -> ParsedCommandIntent(CommandVerb.View, trimmed.drop(5).trimStart())
        else -> ParsedCommandIntent(CommandVerb.General, query)
    }
}

private fun filteredTokens(repo: Repo, query: String): List<Token> {
    val tokens = repo.tokens?.toList().orEmpty()
    if (query.isBlank()) {
        return tokens
    }

    return tokens
        .filter { token ->
            val fields = listOf(token.name, token.resource, token.login).filterNotNull()
            fields.any { fuzzyMatchPositions(query, it) != null }
        }
        .sortedBy { token -> fuzzyScore(query, token.name ?: "") }
}

private fun repoTitle(repo: Repo): String {
    return repo.name?.takeIf { it.isNotBlank() }
        ?: repo.config?.address?.takeIf { it.isNotBlank() }
        ?: repo.id?.takeIf { it.isNotBlank() }
        ?: "unknown-repo"
}

private fun repoAddress(repo: Repo): String {
    val config = repo.config
    val protocol = config?.protocol?.takeIf { it.isNotBlank() } ?: "git"
    val address = config?.address?.takeIf { it.isNotBlank() } ?: repoTitle(repo)
    return "$protocol://$address"
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
