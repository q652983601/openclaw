package ai.openclaw.app.ui

import ai.openclaw.app.GatewayModelProviderSummary
import ai.openclaw.app.GatewayModelSummary
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.R
import ai.openclaw.app.ui.design.ClawEmptyState
import ai.openclaw.app.ui.design.ClawPanel
import ai.openclaw.app.ui.design.ClawPlainIconButton
import ai.openclaw.app.ui.design.ClawScaffold
import ai.openclaw.app.ui.design.ClawSeparatedColumn
import ai.openclaw.app.ui.design.ClawTextField
import ai.openclaw.app.ui.design.ClawTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Full-screen command palette for navigation and recent-session search. */
@Composable
internal fun CommandPalette(
  viewModel: MainViewModel,
  onDismiss: () -> Unit,
  onOpenChat: () -> Unit,
  onOpenVoice: () -> Unit,
  onOpenSessions: () -> Unit,
  onOpenProviders: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenSession: (String) -> Unit,
) {
  val isConnected by viewModel.isConnected.collectAsState()
  val sessions by viewModel.chatSessions.collectAsState()
  val models by viewModel.modelCatalog.collectAsState()
  val providers by viewModel.modelAuthProviders.collectAsState()
  val pendingRunCount by viewModel.pendingRunCount.collectAsState()
  var query by rememberSaveable { mutableStateOf("") }
  val normalizedQuery = query.trim().lowercase()
  val quickActions =
    listOf(
      CommandItem(stringResource(R.string.open_chat), stringResource(R.string.start_or_continue_conversation), Icons.Outlined.ChatBubbleOutline, onOpenChat),
      CommandItem(stringResource(R.string.start_voice), stringResource(R.string.talk_or_dictate_with_openclaw), Icons.Outlined.MicNone, onOpenVoice),
      CommandItem(stringResource(R.string.browse_sessions), stringResource(R.string.find_previous_conversations), Icons.Outlined.AccessTime, onOpenSessions),
      CommandItem(stringResource(R.string.providers_models), providerCommandSubtitle(isConnected, providers, models), Icons.Outlined.Inventory2, onOpenProviders),
      CommandItem(stringResource(R.string.settings), stringResource(R.string.gateway_voice_notifications_privacy), Icons.Outlined.Settings, onOpenSettings),
    )
  val actionRows = quickActions.filter { it.matches(normalizedQuery) }
  val sessionRows =
    sessions
      .filter { session ->
        val title = commandSessionTitle(session.displayName)
        normalizedQuery.isEmpty() || title.lowercase().contains(normalizedQuery)
      }.take(5)

  Surface(modifier = Modifier.fillMaxSize(), color = ClawTheme.colors.canvas, contentColor = ClawTheme.colors.text) {
    ClawScaffold(contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 20.dp)) {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            ClawPlainIconButton(
              icon = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.close_search),
              onClick = onDismiss,
            )
            Text(text = stringResource(R.string.search), style = ClawTheme.type.title, color = ClawTheme.colors.text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            CommandAvatar(text = stringResource(R.string.openclaw_initials))
          }
        }

        item {
          ClawTextField(value = query, onValueChange = { query = it }, placeholder = stringResource(R.string.search_openclaw))
        }

        item {
          CommandSectionLabel(title = stringResource(R.string.quick_actions))
        }

        if (actionRows.isEmpty()) {
          item {
            ClawEmptyState(title = stringResource(R.string.no_actions_found), body = stringResource(R.string.try_chat_voice_sessions_providers_settings))
          }
        } else {
          item {
            CommandActionList(rows = actionRows)
          }
        }

        item {
          CommandSectionLabel(title = stringResource(R.string.sessions))
        }

        if (sessionRows.isEmpty()) {
          item {
            ClawPanel {
              Text(
                text = if (isConnected) stringResource(R.string.no_matching_sessions_yet) else stringResource(R.string.connect_gateway_to_search_sessions),
                style = ClawTheme.type.body,
                color = ClawTheme.colors.textMuted,
              )
            }
          }
        } else {
          item {
            CommandSessionList(
              rows =
                sessionRows.map { session ->
                  CommandSessionRow(
                    key = session.key,
                    title = commandSessionTitle(session.displayName),
                    subtitle = if (pendingRunCount > 0) stringResource(R.string.assistant_working) else stringResource(R.string.openclaw_session),
                    metadata = session.updatedAtMs?.let { commandRelativeTime(it) } ?: stringResource(R.string.now),
                  )
                },
              onOpen = onOpenSession,
            )
          }
        }
      }
    }
  }
}

private data class CommandItem(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
) {
  /** Matches palette queries against both action title and explanatory subtitle. */
  fun matches(query: String): Boolean = query.isEmpty() || title.lowercase().contains(query) || subtitle.lowercase().contains(query)
}

private data class CommandSessionRow(
  val key: String,
  val title: String,
  val subtitle: String,
  val metadata: String,
)

@Composable
private fun CommandActionList(rows: List<CommandItem>) {
  ClawPanel(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
    ClawSeparatedColumn(items = rows) { row ->
      CommandActionRow(row = row)
    }
  }
}

@Composable
private fun CommandActionRow(row: CommandItem) {
  Surface(color = Color.Transparent, contentColor = ClawTheme.colors.text) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 52.dp)
          .clip(RoundedCornerShape(ClawTheme.radii.row))
          .clickable(onClick = row.onClick)
          .padding(horizontal = 2.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      Icon(imageVector = row.icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = ClawTheme.colors.text)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(text = row.title, style = ClawTheme.type.body, color = ClawTheme.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = row.subtitle, style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = stringResource(R.string.open_x, row.title),
        modifier = Modifier.size(17.dp),
        tint = ClawTheme.colors.textMuted,
      )
    }
  }
}

@Composable
private fun CommandSessionList(
  rows: List<CommandSessionRow>,
  onOpen: (String) -> Unit,
) {
  ClawPanel(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
    ClawSeparatedColumn(items = rows) { row ->
      CommandSessionListRow(row = row, onClick = { onOpen(row.key) })
    }
  }
}

@Composable
private fun CommandSessionListRow(
  row: CommandSessionRow,
  onClick: () -> Unit,
) {
  Surface(color = ClawTheme.colors.canvas, contentColor = ClawTheme.colors.text) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 58.dp)
          .clip(RoundedCornerShape(ClawTheme.radii.row))
          .clickable(onClick = onClick)
          .padding(horizontal = 2.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        modifier = Modifier.size(30.dp),
        shape = CircleShape,
        color = ClawTheme.colors.canvas,
        border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = ClawTheme.colors.text)
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(text = row.title, style = ClawTheme.type.body, color = ClawTheme.colors.text, maxLines = 1)
        Text(text = row.subtitle, style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle, maxLines = 1)
      }
      Text(text = row.metadata, style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted)
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = stringResource(R.string.open_session),
        modifier = Modifier.size(17.dp),
        tint = ClawTheme.colors.textMuted,
      )
    }
  }
}

@Composable
private fun CommandAvatar(text: String) {
  Surface(
    modifier = Modifier.size(34.dp),
    shape = CircleShape,
    color = ClawTheme.colors.surfaceRaised,
    contentColor = ClawTheme.colors.text,
    border = BorderStroke(1.dp, ClawTheme.colors.border),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(text = text.take(2).uppercase(), style = ClawTheme.type.label)
    }
  }
}

@Composable
private fun CommandSectionLabel(title: String) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Text(text = title.uppercase(), style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted)
  }
}

@Composable
internal fun providerCommandSubtitle(
  isConnected: Boolean,
  providers: List<GatewayModelProviderSummary>,
  models: List<GatewayModelSummary>,
): String {
  if (!isConnected) return stringResource(R.string.connect_gateway_to_view_providers)
  val readyProviderCount = providerRows(providers = providers, models = models).count { it.ready }
  if (readyProviderCount > 0) return stringResource(R.string.providers_ready, readyProviderCount)
  return stringResource(R.string.no_ready_providers)
}

/** Falls back to the canonical main-session label when gateway display names are blank. */
@Composable
private fun commandSessionTitle(displayName: String?): String = displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.main_session)

/** Formats command-palette session timestamps for compact rows. */
@Composable
private fun commandRelativeTime(updatedAtMs: Long): String {
  val deltaMs = (System.currentTimeMillis() - updatedAtMs).coerceAtLeast(0L)
  val minutes = deltaMs / 60_000L
  if (minutes < 1) return stringResource(R.string.now)
  if (minutes < 60) return stringResource(R.string.in_minutes, minutes)
  val hours = minutes / 60
  if (hours < 24) return stringResource(R.string.in_hours, hours)
  return stringResource(R.string.in_days, hours / 24)
}
