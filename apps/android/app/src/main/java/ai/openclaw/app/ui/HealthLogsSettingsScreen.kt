package ai.openclaw.app.ui

import ai.openclaw.app.GatewayHealthLogsSummary
import ai.openclaw.app.GatewayLogEntry
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.ui.design.ClawPanel
import ai.openclaw.app.ui.design.ClawSecondaryButton
import ai.openclaw.app.ui.design.ClawStatus
import ai.openclaw.app.ui.design.ClawStatusPill
import ai.openclaw.app.ui.design.ClawStatusRow
import ai.openclaw.app.ui.design.ClawTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import ai.openclaw.app.R

/** Settings health screen for gateway/node status and recent gateway logs. */
@Composable
internal fun HealthLogsSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val isConnected by viewModel.isConnected.collectAsState()
  val isNodeConnected by viewModel.isNodeConnected.collectAsState()
  val chatHealthOk by viewModel.chatHealthOk.collectAsState()
  val statusText by viewModel.statusText.collectAsState()
  val modelCount by viewModel.modelCatalog.collectAsState()
  val pendingRunCount by viewModel.pendingRunCount.collectAsState()
  val talkStatus by viewModel.talkModeStatusText.collectAsState()
  val logsSummary by viewModel.healthLogsSummary.collectAsState()
  val logsRefreshing by viewModel.healthLogsRefreshing.collectAsState()
  val logsErrorText by viewModel.healthLogsErrorText.collectAsState()
  var selectedLogEntry by remember { mutableStateOf<GatewayLogEntry?>(null) }

  LaunchedEffect(isConnected) {
    if (isConnected) {
      // Load logs when the gateway becomes available; manual refresh covers
      // later updates so this screen does not poll.
      viewModel.refreshHealthLogs()
    }
  }

  selectedLogEntry?.let { entry ->
    GatewayLogDetailSettingsScreen(entry = entry, onBack = { selectedLogEntry = null })
    return
  }

  SettingsDetailFrame(
    title = stringResource(R.string.health_detail_title),
    subtitle = stringResource(R.string.health_detail_subtitle),
    icon = Icons.Default.Settings,
    onBack = onBack,
  ) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.health_metric_gateway), if (isConnected) stringResource(R.string.health_metric_online) else stringResource(R.string.health_metric_offline)),
          SettingsMetric(stringResource(R.string.health_metric_node), if (isNodeConnected) stringResource(R.string.health_metric_online) else stringResource(R.string.health_metric_waiting)),
          SettingsMetric(stringResource(R.string.health_metric_models), modelCount.size.toString()),
          SettingsMetric(stringResource(R.string.health_metric_logs), logsSummary.entries.size.toString()),
        ),
    )
    HealthStatusPanel(
      gateway = statusText,
      node = if (isNodeConnected) stringResource(R.string.health_metric_online) else stringResource(R.string.health_metric_waiting),
      chat = if (chatHealthOk) stringResource(R.string.health_status_ready) else stringResource(R.string.health_status_needs_connection),
      models = stringResource(R.string.health_models_available, modelCount.size),
      voice = talkStatus,
      runs = if (pendingRunCount > 0) stringResource(R.string.x_runs_active, pendingRunCount) else stringResource(R.string.idle),
      isConnected = isConnected,
      isNodeConnected = isNodeConnected,
      chatHealthOk = chatHealthOk,
      modelsReady = modelCount.isNotEmpty(),
      voiceReady = talkStatus.lowercase() != "off",
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawSecondaryButton(
        text = if (logsRefreshing) stringResource(R.string.refreshing) else stringResource(R.string.refresh_logs),
        onClick = viewModel::refreshHealthLogs,
        enabled = isConnected && !logsRefreshing,
        modifier = Modifier.weight(1f),
      )
    }
    logsErrorText?.let { error ->
      ClawPanel {
        Text(text = error, style = ClawTheme.type.body, color = ClawTheme.colors.warning)
      }
    }
    GatewayLogsPanel(isConnected = isConnected, summary = logsSummary, onLogClick = { selectedLogEntry = it })
  }
}

@Composable
private fun GatewayLogDetailSettingsScreen(
  entry: GatewayLogEntry,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  SettingsDetailFrame(
    title = stringResource(R.string.log_entry_title),
    subtitle = stringResource(R.string.log_entry_subtitle),
    icon = Icons.Default.Settings,
    onBack = onBack,
  ) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.log_metric_time), compactLogTime(entry.time)),
          SettingsMetric(stringResource(R.string.log_metric_level), entry.level?.uppercase() ?: stringResource(R.string.unknown_log_level)),
          SettingsMetric(stringResource(R.string.log_metric_subsystem), entry.subsystem ?: stringResource(R.string.unknown_subsystem)),
        ),
    )
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(R.string.log_message), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        Text(text = entry.message, style = ClawTheme.type.body, color = ClawTheme.colors.text)
      }
    }
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(R.string.log_raw), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        Text(
          text = entry.raw.take(4_000),
          style = ClawTheme.type.caption,
          color = ClawTheme.colors.textMuted,
        )
      }
    }
  }
}

@Composable
private fun HealthStatusPanel(
  gateway: String,
  node: String,
  chat: String,
  models: String,
  voice: String,
  runs: String,
  isConnected: Boolean,
  isNodeConnected: Boolean,
  chatHealthOk: Boolean,
  modelsReady: Boolean,
  voiceReady: Boolean,
) {
  ClawPanel(contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
    Column {
      ClawStatusRow(title = stringResource(R.string.health_metric_gateway), value = gateway, healthy = isConnected)
      HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
      ClawStatusRow(title = stringResource(R.string.health_status_phone_node), value = node, healthy = isNodeConnected)
      HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
      ClawStatusRow(title = stringResource(R.string.health_status_chat), value = chat, healthy = chatHealthOk)
      HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
      ClawStatusRow(title = stringResource(R.string.health_metric_models), value = models, healthy = modelsReady)
      HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
      ClawStatusRow(title = stringResource(R.string.health_voice_status), value = voice, healthy = voiceReady)
      HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
      ClawStatusRow(title = stringResource(R.string.health_runs_status), value = runs, healthy = true)
    }
  }
}

@Composable
private fun GatewayLogsPanel(
  isConnected: Boolean,
  summary: GatewayHealthLogsSummary,
  onLogClick: (GatewayLogEntry) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(text = stringResource(R.string.recent_logs), style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted)
      summary.fileName?.let { fileName ->
        Text(text = fileName, style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
    when {
      !isConnected ->
        ClawPanel {
          Text(text = stringResource(R.string.connect_gateway_load_logs), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      summary.entries.isEmpty() ->
        ClawPanel {
          Text(text = stringResource(R.string.no_recent_log_entries), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      else ->
        ClawPanel(contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
          val entries = summary.entries.takeLast(12)
          Column {
            entries.forEachIndexed { index, entry ->
              GatewayLogRow(entry = entry, onClick = { onLogClick(entry) })
              if (index != entries.lastIndex) {
                HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
              }
            }
          }
        }
    }
    if (summary.truncated) {
      Text(text = stringResource(R.string.showing_latest_log_chunk), style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle)
    }
  }
}

@Composable
private fun GatewayLogRow(
  entry: GatewayLogEntry,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClickLabel = stringResource(R.string.open_log_entry), onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    Text(text = compactLogTime(entry.time), style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle, modifier = Modifier.weight(0.72f), maxLines = 1)
    Column(modifier = Modifier.weight(2.7f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
      Text(text = entry.message, style = ClawTheme.type.caption, color = ClawTheme.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
      entry.subsystem?.let { subsystem ->
        Text(text = subsystem, style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
    ClawStatusPill(text = entry.level?.uppercase() ?: stringResource(R.string.unknown_log_level), status = logLevelStatus(entry.level))
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = ClawTheme.colors.textSubtle,
    )
  }
}

@Composable
private fun compactLogTime(value: String?): String {
  val raw = value?.trim().orEmpty()
  if (raw.isEmpty()) return stringResource(R.string.log_time_placeholder)
  // Gateway log timestamps may be ISO strings or already-compact fragments;
  // keep only the HH:mm portion when present.
  val time =
    raw
      .substringAfter('T', raw)
      .substringBefore('.')
      .substringBefore('+')
      .substringBefore('Z')
  return time.takeIf { it.length >= 5 }?.take(5) ?: raw.take(5)
}

private fun logLevelStatus(level: String?): ClawStatus =
  when (level?.lowercase()) {
    "error", "fatal" -> ClawStatus.Danger
    "warn" -> ClawStatus.Warning
    "info" -> ClawStatus.Success
    else -> ClawStatus.Neutral
  }
