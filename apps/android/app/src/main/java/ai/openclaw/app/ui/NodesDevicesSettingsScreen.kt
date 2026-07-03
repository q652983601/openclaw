package ai.openclaw.app.ui

import ai.openclaw.app.GatewayDeviceTokenSummary
import ai.openclaw.app.GatewayNodeApprovalState
import ai.openclaw.app.GatewayNodeSummary
import ai.openclaw.app.GatewayNodesDevicesSummary
import ai.openclaw.app.GatewayPairedDeviceSummary
import ai.openclaw.app.GatewayPendingDeviceSummary
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.ui.design.ClawDetailRow
import ai.openclaw.app.ui.design.ClawPanel
import ai.openclaw.app.ui.design.ClawSecondaryButton
import ai.openclaw.app.ui.design.ClawStatus
import ai.openclaw.app.ui.design.ClawStatusPill
import ai.openclaw.app.ui.design.ClawTextBadge
import ai.openclaw.app.ui.design.ClawTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import ai.openclaw.app.R

/** Settings screen for gateway nodes, paired devices, and pending pairing requests. */
@Composable
internal fun NodesDevicesSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val summary by viewModel.nodesDevicesSummary.collectAsState()
  val refreshing by viewModel.nodesDevicesRefreshing.collectAsState()
  val errorText by viewModel.nodesDevicesErrorText.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()

  LaunchedEffect(isConnected) {
    if (isConnected) {
      // Refresh once on connection; user-triggered refresh handles later changes
      // so device admin state is not polled from Compose.
      viewModel.refreshNodesDevices()
    }
  }

  SettingsDetailFrame(
    title = stringResource(R.string.nodes_devices_detail_title),
    subtitle = stringResource(R.string.nodes_devices_detail_subtitle),
    icon = Icons.Default.Cloud,
    onBack = onBack,
  ) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.nodes_devices_metric_nodes), summary.nodes.size.toString()),
          SettingsMetric(stringResource(R.string.nodes_devices_metric_online), summary.nodes.count { it.connected }.toString()),
          SettingsMetric(stringResource(R.string.nodes_devices_metric_devices), if (summary.devicePairingAvailable) summary.pairedDevices.size.toString() else stringResource(R.string.nodes_devices_metric_admin)),
          SettingsMetric(stringResource(R.string.nodes_devices_metric_pending), summary.pendingDevices.size.toString()),
        ),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawSecondaryButton(
        text = if (refreshing) stringResource(R.string.nodes_devices_refreshing) else stringResource(R.string.nodes_devices_refresh),
        onClick = viewModel::refreshNodesDevices,
        enabled = isConnected && !refreshing,
        modifier = Modifier.weight(1f),
      )
    }
    errorText?.let {
      ClawPanel {
        Text(text = it, style = ClawTheme.type.body, color = ClawTheme.colors.warning)
      }
    }
    when {
      !isConnected ->
        ClawPanel {
          Text(text = stringResource(R.string.connect_gateway_load_nodes_devices), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      summary.isEmpty() ->
        ClawPanel {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = stringResource(R.string.no_nodes_or_paired_devices), style = ClawTheme.type.section, color = ClawTheme.colors.text)
            Text(text = stringResource(R.string.nodes_devices_pairing_hint), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
          }
        }
      else -> NodesDevicesPanel(summary = summary)
    }
  }
}

@Composable
private fun NodesDevicesPanel(summary: GatewayNodesDevicesSummary) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (!summary.devicePairingAvailable) {
      ClawPanel {
        Text(text = stringResource(R.string.device_pairing_admin), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
      }
    }
    if (summary.pendingDevices.isNotEmpty()) {
      NodesSection(title = stringResource(R.string.pending_requests)) {
        summary.pendingDevices.forEachIndexed { index, device ->
          PendingDeviceRow(device = device)
          if (index != summary.pendingDevices.lastIndex) {
            HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
          }
        }
      }
    }
    if (summary.nodes.isNotEmpty()) {
      NodesSection(title = stringResource(R.string.nodes_devices_metric_nodes)) {
        summary.nodes.forEachIndexed { index, node ->
          NodeRow(node = node)
          if (index != summary.nodes.lastIndex) {
            HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
          }
        }
      }
    }
    if (summary.pairedDevices.isNotEmpty()) {
      NodesSection(title = stringResource(R.string.paired_devices)) {
        summary.pairedDevices.forEachIndexed { index, device ->
          PairedDeviceRow(device = device)
          if (index != summary.pairedDevices.lastIndex) {
            HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
          }
        }
      }
    }
  }
}

@Composable
private fun NodesSection(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(text = title.uppercase(), style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted)
    ClawPanel(contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
      Column {
        content()
      }
    }
  }
}

@Composable
private fun NodeRow(node: GatewayNodeSummary) {
  DeviceListRow(
    badge = nodeBadge(node.displayName ?: node.id),
    title = node.displayName ?: node.id,
    subtitle = nodeSubtitle(node),
    statusText = nodeStatusText(node),
    status = nodeStatus(node),
  )
}

@Composable
private fun PendingDeviceRow(device: GatewayPendingDeviceSummary) {
  DeviceListRow(
    badge = nodeBadge(device.displayName ?: device.deviceId),
    title = device.displayName ?: stringResource(R.string.new_device),
    subtitle = pendingDeviceSubtitle(device),
    statusText = if (device.repair) stringResource(R.string.repair) else stringResource(R.string.review),
    status = ClawStatus.Warning,
  )
}

@Composable
private fun PairedDeviceRow(device: GatewayPairedDeviceSummary) {
  DeviceListRow(
    badge = nodeBadge(device.displayName ?: device.deviceId),
    title = device.displayName ?: stringResource(R.string.paired_device),
    subtitle = pairedDeviceSubtitle(device),
    statusText = pairedDeviceStatusText(device.tokens),
    status = pairedDeviceStatus(device.tokens),
  )
}

@Composable
private fun DeviceListRow(
  badge: String,
  title: String,
  subtitle: String,
  statusText: String,
  status: ClawStatus,
) {
  ClawDetailRow(
    title = title,
    subtitle = subtitle,
    leading = { ClawTextBadge(text = badge) },
    trailing = { ClawStatusPill(text = statusText, status = status) },
  )
}

/** True when the gateway returned no node or device rows to render. */
private fun GatewayNodesDevicesSummary.isEmpty(): Boolean = nodes.isEmpty() && pendingDevices.isEmpty() && pairedDevices.isEmpty()

@Composable
private fun nodeSubtitle(node: GatewayNodeSummary): String {
  val kind = node.deviceFamily ?: stringResource(R.string.node_host)
  val version = node.version?.let { stringResource(R.string.openclaw_version_label, it) }
  val status = if (node.paired) stringResource(R.string.paired) else stringResource(R.string.unpaired)
  val approval = nodeApprovalSubtitle(node.approvalState)
  val commands =
    node.commands
      .take(2)
      .joinToString(", ")
      .takeIf { it.isNotBlank() }
  return listOfNotNull(kind, version, status, approval, commands).joinToString(" · ")
}

@Composable
private fun nodeStatusText(node: GatewayNodeSummary): String =
  when (node.approvalState) {
    GatewayNodeApprovalState.PendingApproval -> stringResource(R.string.needs_approval)
    GatewayNodeApprovalState.PendingReapproval -> stringResource(R.string.needs_reapproval)
    GatewayNodeApprovalState.Unapproved -> stringResource(R.string.unapproved)
    else -> if (node.connected) stringResource(R.string.health_metric_online) else stringResource(R.string.health_metric_offline)
  }

private fun nodeStatus(node: GatewayNodeSummary): ClawStatus =
  when (node.approvalState) {
    GatewayNodeApprovalState.Approved -> if (node.connected) ClawStatus.Success else ClawStatus.Warning
    GatewayNodeApprovalState.PendingApproval,
    GatewayNodeApprovalState.PendingReapproval,
    GatewayNodeApprovalState.Unapproved,
    -> ClawStatus.Warning
    GatewayNodeApprovalState.Loading,
    GatewayNodeApprovalState.Unsupported,
    -> if (node.connected) ClawStatus.Neutral else ClawStatus.Warning
  }

@Composable
private fun nodeApprovalSubtitle(approvalState: GatewayNodeApprovalState): String? =
  when (approvalState) {
    GatewayNodeApprovalState.Approved -> stringResource(R.string.approved)
    GatewayNodeApprovalState.PendingApproval -> stringResource(R.string.capability_approval_pending)
    GatewayNodeApprovalState.PendingReapproval -> stringResource(R.string.capability_reapproval_pending)
    GatewayNodeApprovalState.Unapproved -> stringResource(R.string.capability_unapproved)
    GatewayNodeApprovalState.Loading,
    GatewayNodeApprovalState.Unsupported,
    -> null
  }

@Composable
private fun pendingDeviceSubtitle(device: GatewayPendingDeviceSummary): String {
  val roles = formatDeviceList(device.roles, stringResource(R.string.role))
  val scopes = formatDeviceList(device.scopes, stringResource(R.string.scope))
  val requested = device.requestedAtMs?.let { "requested ${relativeDeviceTime(it)}" }
  return listOfNotNull(roles, scopes, requested, device.remoteIp).joinToString(" · ")
}

@Composable
private fun pairedDeviceSubtitle(device: GatewayPairedDeviceSummary): String {
  val roles = formatDeviceList(device.roles, stringResource(R.string.role))
  val scopes = formatDeviceList(device.scopes, stringResource(R.string.scope))
  val tokens = stringResource(R.string.active_tokens, device.tokens.count { !it.revoked }, device.tokens.size)
  return listOfNotNull(roles, scopes, tokens, device.remoteIp).joinToString(" · ")
}

@Composable
private fun pairedDeviceStatusText(tokens: List<GatewayDeviceTokenSummary>): String =
  when {
    tokens.isEmpty() -> stringResource(R.string.paired)
    tokens.any { !it.revoked } -> stringResource(R.string.active)
    else -> stringResource(R.string.needs_token)
  }

private fun pairedDeviceStatus(tokens: List<GatewayDeviceTokenSummary>): ClawStatus =
  when {
    tokens.isEmpty() -> ClawStatus.Neutral
    tokens.any { !it.revoked } -> ClawStatus.Success
    else -> ClawStatus.Warning
  }

private fun nodeBadge(value: String): String =
  value
    .split(' ', '-', '_')
    .filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
    .joinToString("")
    .ifBlank { "N" }

@Composable
private fun formatDeviceList(
  values: List<String>,
  fallback: String,
): String? =
  when (values.size) {
    0 -> null
    1 -> values.first()
    else -> stringResource(R.string.plural_format, values.size, fallback)
  }

@Composable
private fun relativeDeviceTime(timeMs: Long): String {
  val minutes = ((System.currentTimeMillis() - timeMs).coerceAtLeast(0L)) / 60_000L
  if (minutes < 1) return stringResource(R.string.now)
  if (minutes < 60) return stringResource(R.string.minutes_ago_long, minutes)
  val hours = minutes / 60L
  if (hours < 24) return stringResource(R.string.hours_ago_long, hours)
  return stringResource(R.string.days_ago_long, hours / 24L)
}
