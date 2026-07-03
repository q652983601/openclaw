package ai.openclaw.app.ui

import ai.openclaw.app.MainViewModel
import ai.openclaw.app.R
import ai.openclaw.app.ui.design.ClawEmptyState
import ai.openclaw.app.ui.design.ClawPlainIconButton
import ai.openclaw.app.ui.design.ClawPrimaryButton
import ai.openclaw.app.ui.design.ClawScaffold
import ai.openclaw.app.ui.design.ClawTheme
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Session browser for recent and current chat sessions. */
@Composable
internal fun SessionsScreen(
  viewModel: MainViewModel,
  onOpenCommand: () -> Unit,
  onOpenChat: () -> Unit,
) {
  val sessions by viewModel.chatSessions.collectAsState()
  val chatSessionKey by viewModel.chatSessionKey.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()
  var filter by rememberSaveable { mutableStateOf(SessionFilter.Recent) }
  var compactLayout by rememberSaveable { mutableStateOf(false) }
  var recentFirst by rememberSaveable { mutableStateOf(true) }
  val visibleSessions =
    sessions
      .let { rows ->
        when (filter) {
          SessionFilter.Recent -> rows
          SessionFilter.Current -> rows.filter { it.key == chatSessionKey }
        }
      }.let { rows ->
        if (recentFirst) {
          rows.sortedByDescending { it.updatedAtMs ?: 0L }
        } else {
          rows.sortedBy { it.updatedAtMs ?: 0L }
        }
      }

  LaunchedEffect(isConnected) {
    if (isConnected) {
      // Sessions are cheap to refresh on entry; subsequent sorting/filtering is
      // local to avoid re-querying while the user explores the list.
      viewModel.refreshChatSessions(limit = 200)
    }
  }

  val context = LocalContext.current

  ClawScaffold(
    contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp),
    contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(9.dp),
      contentPadding = PaddingValues(bottom = 4.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(text = stringResource(R.string.sessions_title), style = ClawTheme.type.display.copy(fontSize = 24.sp, lineHeight = 28.sp), color = ClawTheme.colors.text, modifier = Modifier.weight(1f))
          ClawPlainIconButton(icon = Icons.Default.Search, contentDescription = stringResource(R.string.search_sessions), onClick = onOpenCommand)
          ClawPlainIconButton(icon = Icons.Default.SwapVert, contentDescription = stringResource(R.string.reverse_session_sort), onClick = { recentFirst = !recentFirst })
        }
      }

      item {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          FilterPill(text = stringResource(R.string.recent_filter), icon = Icons.Outlined.AccessTime, active = filter == SessionFilter.Recent, onClick = { filter = SessionFilter.Recent })
          FilterPill(text = stringResource(R.string.current_filter), icon = Icons.Outlined.MicNone, active = filter == SessionFilter.Current, showDot = sessions.any { it.key == chatSessionKey }, onClick = { filter = SessionFilter.Current })
        }
      }

      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Row(
            modifier =
              Modifier
                .clip(RoundedCornerShape(ClawTheme.radii.row))
                .clickable { recentFirst = !recentFirst }
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(text = stringResource(R.string.sort_label, if (recentFirst) stringResource(R.string.sort_newest) else stringResource(R.string.sort_oldest)), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(11.dp), tint = ClawTheme.colors.textMuted)
          }
          SessionOutlineIconButton(icon = Icons.Default.Storage, contentDescription = stringResource(R.string.toggle_session_layout), onClick = { compactLayout = !compactLayout })
        }
      }

      item {
        Text(text = if (compactLayout) stringResource(R.string.layout_compact) else stringResource(R.string.layout_detailed), style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle)
      }

      if (visibleSessions.isEmpty()) {
        item {
          Box(
            modifier = Modifier.fillParentMaxHeight(0.56f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            ClawEmptyState(
              title = emptySessionTitle(context, filter),
              body = emptySessionBody(context, filter),
              action = { ClawPrimaryButton(text = stringResource(R.string.start_chat), onClick = onOpenChat) },
            )
          }
        }
      } else {
        items(visibleSessions, key = { it.key }) { session ->
          val active = session.key == chatSessionKey
          SessionRow(
            title = displaySessionTitle(context, session.displayName),
            subtitle = if (active) stringResource(R.string.current_session) else stringResource(R.string.openclaw_session),
            metadata = session.updatedAtMs?.let { relativeSessionTime(context, it) } ?: stringResource(R.string.now),
            active = active,
            compact = compactLayout,
            onClick = {
              viewModel.switchChatSession(session.key)
              onOpenChat()
            },
          )
        }
      }
    }
  }
}

@Composable
private fun FilterPill(
  text: String,
  icon: ImageVector? = null,
  active: Boolean = false,
  showDot: Boolean = false,
  dropdown: Boolean = false,
  onClick: (() -> Unit)? = null,
) {
  Surface(
    onClick = onClick ?: {},
    enabled = onClick != null,
    shape = RoundedCornerShape(7.dp),
    color = if (active) ClawTheme.colors.surfaceRaised else Color.Transparent,
    contentColor = ClawTheme.colors.text,
    border = BorderStroke(1.dp, if (active) ClawTheme.colors.borderStrong else ClawTheme.colors.border),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      icon?.let { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(12.dp), tint = ClawTheme.colors.text) }
      Text(text = text, style = ClawTheme.type.label, color = ClawTheme.colors.text, maxLines = 1)
      if (showDot) {
        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ClawTheme.colors.success))
      }
      if (dropdown) {
        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(11.dp), tint = ClawTheme.colors.textMuted)
      }
    }
  }
}

@Composable
private fun SessionRow(
  title: String,
  subtitle: String,
  metadata: String,
  active: Boolean,
  compact: Boolean,
  onClick: () -> Unit,
) {
  Surface(onClick = onClick, color = Color.Transparent, contentColor = ClawTheme.colors.text) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Surface(
          modifier = Modifier.size(30.dp),
          shape = CircleShape,
          color = Color.Transparent,
          border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (active) Icons.Default.StarBorder else Icons.Outlined.ChatBubbleOutline,
              contentDescription = null,
              modifier = Modifier.size(15.dp),
              tint = ClawTheme.colors.text,
            )
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = title,
              style = ClawTheme.type.body,
              color = ClawTheme.colors.text,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (active) {
              Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(ClawTheme.colors.success))
            }
          }
          if (!compact) {
            Text(text = subtitle, style = ClawTheme.type.caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp), color = ClawTheme.colors.textMuted, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              SessionMiniTag(text = stringResource(R.string.workspace_tag))
              SessionMiniTag(text = if (active) stringResource(R.string.current_tag) else stringResource(R.string.openclaw_brand))
            }
          }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(13.dp), tint = ClawTheme.colors.textMuted)
          Text(text = metadata, style = ClawTheme.type.caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp), color = ClawTheme.colors.textMuted, maxLines = 1)
        }
      }
      HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
    }
  }
}

@Composable
private fun SessionOutlineIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier.size(ClawTheme.spacing.touchTarget),
    shape = RoundedCornerShape(7.dp),
    color = Color.Transparent,
    contentColor = ClawTheme.colors.text,
    border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(14.dp))
    }
  }
}

@Composable
private fun SessionMiniTag(text: String) {
  Surface(
    shape = RoundedCornerShape(5.dp),
    color = Color.Transparent,
    border = BorderStroke(1.dp, ClawTheme.colors.border),
    contentColor = ClawTheme.colors.textMuted,
  ) {
    Text(text = text, modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp), style = ClawTheme.type.caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp), maxLines = 1)
  }
}

private enum class SessionFilter {
  Recent,
  Current,
}

/** Empty-state title selected by the active session browser filter. */
private fun emptySessionTitle(context: Context, filter: SessionFilter): String =
  when (filter) {
    SessionFilter.Recent -> context.getString(R.string.no_sessions_yet)
    SessionFilter.Current -> context.getString(R.string.no_current_session)
  }

/** Empty-state body selected by the active session browser filter. */
private fun emptySessionBody(context: Context, filter: SessionFilter): String =
  when (filter) {
    SessionFilter.Recent -> context.getString(R.string.start_new_conversation_hint)
    SessionFilter.Current -> context.getString(R.string.open_chat_current_hint)
  }

/** Formats session timestamps for compact mobile metadata. */
private fun relativeSessionTime(context: Context, updatedAtMs: Long): String {
  val deltaMs = (System.currentTimeMillis() - updatedAtMs).coerceAtLeast(0L)
  val minutes = deltaMs / 60_000L
  if (minutes < 1) return context.getString(R.string.now)
  if (minutes < 60) return context.getString(R.string.minutes_ago_format, minutes)
  val hours = minutes / 60
  if (hours < 24) return context.getString(R.string.hours_ago_format, hours)
  return context.getString(R.string.days_ago_format, hours / 24)
}

/** Falls back to the canonical main-session label when gateway display names are blank. */
private fun displaySessionTitle(context: Context, displayName: String?): String = displayName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.main_session)
