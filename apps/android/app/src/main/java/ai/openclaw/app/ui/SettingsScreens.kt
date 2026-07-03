package ai.openclaw.app.ui

import ai.openclaw.app.R
import ai.openclaw.app.AppearanceThemeMode
import ai.openclaw.app.BuildConfig
import ai.openclaw.app.GatewayAgentSummary
import ai.openclaw.app.GatewayCronJobSummary
import ai.openclaw.app.GatewayExecApprovalSummary
import ai.openclaw.app.GatewayUsageProviderSummary
import ai.openclaw.app.LocationMode
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.NotificationPackageFilterMode
import ai.openclaw.app.chat.ChatPendingToolCall
import ai.openclaw.app.node.DeviceNotificationListenerService
import ai.openclaw.app.ui.design.ClawDetailRow
import ai.openclaw.app.ui.design.ClawIconBadge
import ai.openclaw.app.ui.design.ClawListPanel
import ai.openclaw.app.ui.design.ClawPanel
import ai.openclaw.app.ui.design.ClawPlainIconButton
import ai.openclaw.app.ui.design.ClawPrimaryButton
import ai.openclaw.app.ui.design.ClawScaffold
import ai.openclaw.app.ui.design.ClawSecondaryButton
import ai.openclaw.app.ui.design.ClawSegmentedControl
import ai.openclaw.app.ui.design.ClawSeparatedColumn
import ai.openclaw.app.ui.design.ClawStatus
import ai.openclaw.app.ui.design.ClawStatusPill
import ai.openclaw.app.ui.design.ClawTextBadge
import ai.openclaw.app.ui.design.ClawTextField
import ai.openclaw.app.ui.design.ClawTheme
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Detail routes reachable from the Android settings home surface.
 */
internal enum class SettingsRoute {
  Home,
  Profile,
  Voice,
  Agents,
  ProvidersModels,
  Approvals,
  CronJobs,
  Usage,
  Skills,
  NodesDevices,
  Channels,
  Dreaming,
  Canvas,
  Notifications,
  PhoneCapabilities,
  Gateway,
  Appearance,
  Health,
  About,
}

/**
 * Dispatches a selected settings route to its detail screen without changing navigation ownership.
 */
@Composable
internal fun SettingsDetailScreen(
  viewModel: MainViewModel,
  route: SettingsRoute,
  onBack: () -> Unit,
) {
  when (route) {
    SettingsRoute.Home -> Unit
    SettingsRoute.Profile -> ProfileSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Voice -> VoiceSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Agents -> AgentsSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.ProvidersModels -> ProvidersModelsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Approvals -> ApprovalsSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.CronJobs -> CronJobsSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Usage -> UsageSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Skills -> SkillsSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.NodesDevices -> NodesDevicesSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Channels -> ChannelsSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Dreaming -> DreamingSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Canvas -> CanvasSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Notifications -> NotificationSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.PhoneCapabilities -> PhoneCapabilitiesScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Gateway -> GatewaySettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Appearance -> AppearanceSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.Health -> HealthLogsSettingsScreen(viewModel = viewModel, onBack = onBack)
    SettingsRoute.About -> AboutSettingsScreen(viewModel = viewModel, onBack = onBack)
  }
}

@Composable
private fun UsageSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val usageSummary by viewModel.usageSummary.collectAsState()
  val usageRefreshing by viewModel.usageRefreshing.collectAsState()
  val usageErrorText by viewModel.usageErrorText.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()
  val providerCount = usageSummary.providers.size
  val issueCount = usageSummary.providers.count { it.error != null }

  LaunchedEffect(isConnected) {
    if (isConnected) {
      viewModel.refreshUsage()
    }
  }

  SettingsDetailFrame(title = stringResource(R.string.usage), subtitle = stringResource(R.string.provider_limits), icon = Icons.Default.Storage, onBack = onBack) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.providers), providerCount.toString()),
          SettingsMetric(stringResource(R.string.issues), issueCount.toString()),
          SettingsMetric(stringResource(R.string.updated), formatUsageUpdated(usageSummary.updatedAtMs)),
        ),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawSecondaryButton(text = if (usageRefreshing) stringResource(R.string.refreshing) else stringResource(R.string.refresh), onClick = viewModel::refreshUsage, enabled = isConnected && !usageRefreshing, modifier = Modifier.weight(1f))
    }
    usageErrorText?.let { errorText ->
      ClawPanel {
        Text(text = errorText, style = ClawTheme.type.body, color = ClawTheme.colors.warning)
      }
    }
    when {
      !isConnected ->
        ClawPanel {
          Text(text = stringResource(R.string.connect_gateway_to_load_usage), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      usageSummary.providers.isEmpty() ->
        ClawPanel {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = stringResource(R.string.no_usage_data_yet), style = ClawTheme.type.section, color = ClawTheme.colors.text)
            Text(text = stringResource(R.string.provider_limits_appear), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
          }
        }
      else -> UsageProvidersPanel(providers = usageSummary.providers)
    }
  }
}

@Composable
private fun CronJobsSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val cronStatus by viewModel.cronStatus.collectAsState()
  val cronJobs by viewModel.cronJobs.collectAsState()
  val cronRefreshing by viewModel.cronRefreshing.collectAsState()
  val cronErrorText by viewModel.cronErrorText.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()

  LaunchedEffect(isConnected) {
    if (isConnected) {
      viewModel.refreshCronJobs()
    }
  }

  SettingsDetailFrame(title = stringResource(R.string.cron_jobs), subtitle = stringResource(R.string.scheduled_openclaw_work), icon = Icons.Default.Bolt, onBack = onBack) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.status), if (cronStatus.enabled) stringResource(R.string.enabled) else stringResource(R.string.off)),
          SettingsMetric(stringResource(R.string.jobs), cronStatus.jobs.toString()),
          SettingsMetric(stringResource(R.string.next_wake), formatCronWake(cronStatus.nextWakeAtMs)),
        ),
    )
    ClawSecondaryButton(text = if (cronRefreshing) stringResource(R.string.refreshing) else stringResource(R.string.refresh), onClick = viewModel::refreshCronJobs, enabled = isConnected && !cronRefreshing, modifier = Modifier.fillMaxWidth())
    ClawPanel {
      Text(text = stringResource(R.string.cron_jobs_hint), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
    }
    cronErrorText?.let { errorText ->
      ClawPanel {
        Text(text = errorText, style = ClawTheme.type.body, color = ClawTheme.colors.warning)
      }
    }
    when {
      !isConnected ->
        ClawPanel {
          Text(text = stringResource(R.string.connect_gateway_to_load_cron_jobs), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      cronJobs.isEmpty() ->
        ClawPanel {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = stringResource(R.string.no_scheduled_jobs), style = ClawTheme.type.section, color = ClawTheme.colors.text)
            Text(text = stringResource(R.string.create_recurring_openclaw_work), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
          }
        }
      else -> CronJobsPanel(jobs = cronJobs)
    }
  }
}

@Composable
private fun AgentsSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val agents by viewModel.gatewayAgents.collectAsState()
  val defaultAgentId by viewModel.gatewayDefaultAgentId.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()

  LaunchedEffect(isConnected) {
    if (isConnected) {
      viewModel.refreshAgents()
    }
  }

  SettingsDetailFrame(title = stringResource(R.string.agents), subtitle = stringResource(R.string.choose_and_inspect), icon = Icons.Default.Person, onBack = onBack) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.available), agents.size.toString()),
          SettingsMetric(stringResource(R.string.default_), defaultAgentName(agents, defaultAgentId)),
        ),
    )
    when {
      !isConnected ->
        ClawPanel {
          Text(text = stringResource(R.string.connect_gateway_to_load_agents), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      agents.isEmpty() ->
        ClawPanel {
          Text(text = stringResource(R.string.no_agents_loaded_yet), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      else -> AgentsPanel(agents = agents, defaultAgentId = defaultAgentId)
    }
  }
}

@Composable
private fun ApprovalsSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val isConnected by viewModel.isConnected.collectAsState()
  val execApprovals by viewModel.execApprovals.collectAsState()
  val execApprovalsRefreshing by viewModel.execApprovalsRefreshing.collectAsState()
  val execApprovalsErrorText by viewModel.execApprovalsErrorText.collectAsState()
  val pendingToolCalls by viewModel.chatPendingToolCalls.collectAsState()
  val pendingRunCount by viewModel.pendingRunCount.collectAsState()
  val issueCount = execApprovals.count { it.errorText != null } + pendingToolCalls.count { it.isError == true }

  LaunchedEffect(isConnected) {
    if (isConnected) {
      viewModel.refreshExecApprovals()
    }
  }

  SettingsDetailFrame(title = stringResource(R.string.approvals), subtitle = stringResource(R.string.review_actions), icon = Icons.Default.Lock, onBack = onBack) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.gateway_pending), execApprovals.size.toString()),
          SettingsMetric(stringResource(R.string.session_activity), pendingToolCalls.size.toString()),
          SettingsMetric(stringResource(R.string.issues), issueCount.toString()),
          SettingsMetric(stringResource(R.string.active_runs), pendingRunCount.toString()),
        ),
    )
    ClawSecondaryButton(
      text = if (execApprovalsRefreshing) stringResource(R.string.refreshing) else stringResource(R.string.refresh),
      onClick = viewModel::refreshExecApprovals,
      enabled = isConnected && !execApprovalsRefreshing,
      modifier = Modifier.fillMaxWidth(),
    )
    if (execApprovalsErrorText != null) {
      ClawPanel {
        Text(text = execApprovalsErrorText ?: "", style = ClawTheme.type.body, color = ClawTheme.colors.warning)
      }
    }
    if (!isConnected) {
      ClawPanel {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(text = stringResource(R.string.gateway_disconnected), style = ClawTheme.type.section, color = ClawTheme.colors.text)
          Text(text = stringResource(R.string.connect_gateway_to_load_approvals), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      }
    } else if (execApprovals.isEmpty()) {
      ClawPanel {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(text = stringResource(R.string.no_gateway_approvals), style = ClawTheme.type.section, color = ClawTheme.colors.text)
          Text(text = stringResource(R.string.exec_approval_requests_appear), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      }
    } else {
      ExecApprovalsPanel(approvals = execApprovals, onResolve = viewModel::resolveExecApproval)
    }
    if (pendingToolCalls.isNotEmpty()) {
      Text(text = stringResource(R.string.session_activity_title), style = ClawTheme.type.section, color = ClawTheme.colors.text)
      Text(text = stringResource(R.string.chat_tool_calls_waiting), style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted)
      SessionToolCallsPanel(toolCalls = pendingToolCalls)
    }
  }
}

@Composable
private fun ProfileSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val displayName by viewModel.displayName.collectAsState()
  var draft by remember(displayName) { mutableStateOf(displayName.ifBlank { "OpenClaw" }) }

  SettingsDetailFrame(title = stringResource(R.string.profile), subtitle = stringResource(R.string.profile_subtitle), icon = Icons.Default.Person, onBack = onBack) {
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ClawTextField(value = draft, onValueChange = { draft = it }, placeholder = stringResource(R.string.device_name))
        ClawPrimaryButton(text = stringResource(R.string.save_profile), onClick = { viewModel.setDisplayName(draft) }, enabled = draft.isNotBlank())
      }
    }
  }
}

@Composable
private fun VoiceSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val speakerEnabled by viewModel.speakerEnabled.collectAsState()
  val micEnabled by viewModel.micEnabled.collectAsState()
  val talkModeEnabled by viewModel.talkModeEnabled.collectAsState()

  SettingsDetailFrame(title = stringResource(R.string.talk_provider_setup), subtitle = stringResource(R.string.talk_provider_setup_subtitle), icon = Icons.Default.Mic, onBack = onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      VoiceSetupPanel(
        voiceActive = micEnabled || talkModeEnabled,
      )
      Text(text = stringResource(R.string.audio_test), style = ClawTheme.type.section, color = ClawTheme.colors.text)
      Text(text = stringResource(R.string.audio_test_desc), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
      SettingsWaveformPanel(active = speakerEnabled, onClick = ::playVoiceSetupTone)
      VoiceSetupActionRow(
        title = if (speakerEnabled) stringResource(R.string.mute_speaker) else stringResource(R.string.enable_speaker),
        subtitle = if (speakerEnabled) stringResource(R.string.replies_play_aloud) else stringResource(R.string.assistant_speech_muted),
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        statusText = if (speakerEnabled) stringResource(R.string.on) else stringResource(R.string.muted),
        ready = speakerEnabled,
        onClick = { viewModel.setSpeakerEnabled(!speakerEnabled) },
      )
      ClawPrimaryButton(text = stringResource(R.string.done), onClick = onBack, modifier = Modifier.fillMaxWidth(), icon = Icons.Default.GraphicEq)
    }
  }
}

@Composable
private fun VoiceSetupPanel(
  voiceActive: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
    VoiceSetupActionRow(
      title = stringResource(R.string.realtime_provider),
      subtitle = stringResource(R.string.gateway_voice_relay),
      icon = Icons.Default.GraphicEq,
      statusText = if (voiceActive) stringResource(R.string.live) else stringResource(R.string.ready),
      ready = true,
    )
    VoiceSetupActionRow(
      title = stringResource(R.string.voice),
      subtitle = stringResource(R.string.voice_input),
      icon = Icons.Default.Mic,
      statusText = stringResource(R.string.configured),
      ready = true,
    )
    VoiceSetupActionRow(
      title = stringResource(R.string.transport),
      subtitle = stringResource(R.string.socket_relay),
      icon = Icons.Default.Bolt,
      statusText = stringResource(R.string.configured),
      ready = true,
    )
  }
}

@Composable
private fun VoiceSetupActionRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  statusText: String,
  ready: Boolean,
  onClick: (() -> Unit)? = null,
) {
  val rowModifier = Modifier.fillMaxWidth().heightIn(min = 68.dp)
  Surface(
    onClick = onClick ?: {},
    enabled = onClick != null,
    modifier = rowModifier,
    shape = RoundedCornerShape(ClawTheme.radii.panel),
    color = ClawTheme.colors.surface,
    contentColor = ClawTheme.colors.text,
    border = BorderStroke(1.dp, ClawTheme.colors.border),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
      Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = ClawTheme.colors.canvas,
        contentColor = ClawTheme.colors.text,
        border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(19.dp))
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = ClawTheme.type.section, color = ClawTheme.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = subtitle, style = ClawTheme.type.body, color = ClawTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
          modifier =
            Modifier
              .size(7.dp)
              .background(if (ready) ClawTheme.colors.success else ClawTheme.colors.textSubtle, CircleShape),
        )
        Text(text = statusText, style = ClawTheme.type.body, color = ClawTheme.colors.textMuted, maxLines = 1)
        if (onClick != null) {
          Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = ClawTheme.colors.textMuted)
        }
      }
    }
  }
}

@Composable
private fun SettingsWaveformPanel(
  active: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(76.dp),
    shape = RoundedCornerShape(ClawTheme.radii.panel),
    color = ClawTheme.colors.surface,
    contentColor = ClawTheme.colors.text,
    border = BorderStroke(1.dp, ClawTheme.colors.border),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp), tint = ClawTheme.colors.text)
      Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        listOf(6, 12, 18, 11, 28, 34, 18, 10, 8, 24, 38, 31, 12, 8, 18, 30, 40, 22, 12, 8, 20, 29, 16, 8).forEachIndexed { index, height ->
          Box(
            modifier =
              Modifier
                .size(width = 2.dp, height = (if (active) height else 7 + index % 4 * 4).dp)
                .background(if (active) ClawTheme.colors.text else ClawTheme.colors.textSubtle, RoundedCornerShape(999.dp)),
          )
        }
      }
    }
  }
}

private fun playVoiceSetupTone() {
  val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
  tone.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
  Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 300L)
}

private const val NOTIFICATION_PICKER_RESULT_LIMIT = 40

@Composable
private fun NotificationSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val enabled by viewModel.notificationForwardingEnabled.collectAsState()
  val mode by viewModel.notificationForwardingMode.collectAsState()
  val packages by viewModel.notificationForwardingPackages.collectAsState()
  val quietEnabled by viewModel.notificationForwardingQuietHoursEnabled.collectAsState()
  val quietStart by viewModel.notificationForwardingQuietStart.collectAsState()
  val quietEnd by viewModel.notificationForwardingQuietEnd.collectAsState()
  val maxEventsPerMinute by viewModel.notificationForwardingMaxEventsPerMinute.collectAsState()
  val modeLabel = if (mode == NotificationPackageFilterMode.Blocklist) stringResource(R.string.blocklist) else stringResource(R.string.allowlist)
  val installedApps = remember(context, packages) { queryInstalledApps(context, packages) }
  var notificationPickerExpanded by remember { mutableStateOf(false) }
  var notificationAppSearch by remember { mutableStateOf("") }
  var notificationShowSystemApps by remember { mutableStateOf(false) }
  val filteredApps =
    remember(installedApps, packages, notificationAppSearch, notificationShowSystemApps) {
      filterNotificationAppsForPicker(
        apps = installedApps,
        selectedPackages = packages,
        query = notificationAppSearch,
        showSystemApps = notificationShowSystemApps,
      )
    }
  var listenerEnabled by remember { mutableStateOf(DeviceNotificationListenerService.isAccessEnabled(context)) }
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      viewModel.setNotificationForwardingEnabled(granted)
    }

  fun setForwarding(checked: Boolean) {
    if (!checked) {
      viewModel.setNotificationForwardingEnabled(false)
      return
    }
    if (Build.VERSION.SDK_INT >= 33 && !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      viewModel.setNotificationForwardingEnabled(true)
    }
    listenerEnabled = DeviceNotificationListenerService.isAccessEnabled(context)
  }

  val allowlistLabel = stringResource(R.string.allowlist)
  val blocklistLabel = stringResource(R.string.blocklist)

  SettingsDetailFrame(title = stringResource(R.string.notifications), subtitle = stringResource(R.string.choose_what_reaches), icon = Icons.Default.Notifications, onBack = onBack) {
    SettingsTogglePanel(
      rows =
        listOf(
          SettingsToggleRow(stringResource(R.string.forward_notifications), if (enabled) stringResource(R.string.forward_notifications_on) else stringResource(R.string.forward_notifications_off), Icons.Default.Notifications, enabled, ::setForwarding),
          SettingsToggleRow(stringResource(R.string.quiet_hours), stringResource(R.string.quiet_hours_window, quietStart, quietEnd), Icons.Default.Bolt, quietEnabled) { checked ->
            viewModel.setNotificationForwardingQuietHours(enabled = checked, start = quietStart, end = quietEnd)
          },
        ),
    )
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.policy), modeLabel),
          SettingsMetric(stringResource(R.string.selected_apps), packages.size.toString()),
          SettingsMetric(stringResource(R.string.rate_limit), stringResource(R.string.events_per_minute_format, maxEventsPerMinute)),
          SettingsMetric(stringResource(R.string.access), if (listenerEnabled) stringResource(R.string.granted) else stringResource(R.string.setup)),
        ),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawSecondaryButton(
        text = if (listenerEnabled) stringResource(R.string.check_access) else stringResource(R.string.open_system_access),
        onClick = {
          openNotificationListenerSettings(context)
          listenerEnabled = DeviceNotificationListenerService.isAccessEnabled(context)
        },
        modifier = Modifier.weight(1f),
      )
    }
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.forwarding_mode), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        ClawSegmentedControl(
          options = listOf(blocklistLabel, allowlistLabel),
          selected = modeLabel,
          onSelect = { selected ->
            viewModel.setNotificationForwardingMode(if (selected == allowlistLabel) NotificationPackageFilterMode.Allowlist else NotificationPackageFilterMode.Blocklist)
          },
        )
      }
    }
    NotificationPackagePickerPanel(
      mode = mode,
      selectedPackages = packages,
      apps = filteredApps,
      search = notificationAppSearch,
      showSystemApps = notificationShowSystemApps,
      expanded = notificationPickerExpanded,
      onSearchChange = { notificationAppSearch = it },
      onShowSystemAppsChange = { notificationShowSystemApps = it },
      onExpandedChange = { notificationPickerExpanded = it },
      onPackageSelectionChange = { packageName, selected ->
        val next = packages.toMutableSet()
        if (selected) {
          next.add(packageName)
        } else {
          next.remove(packageName)
        }
        viewModel.setNotificationForwardingPackagesCsv(next.sorted().joinToString(","))
      },
    )
  }
}

@Composable
private fun NotificationPackagePickerPanel(
  mode: NotificationPackageFilterMode,
  selectedPackages: Set<String>,
  apps: List<InstalledApp>,
  search: String,
  showSystemApps: Boolean,
  expanded: Boolean,
  onSearchChange: (String) -> Unit,
  onShowSystemAppsChange: (Boolean) -> Unit,
  onExpandedChange: (Boolean) -> Unit,
  onPackageSelectionChange: (String, Boolean) -> Unit,
) {
  val visibleApps = apps.take(NOTIFICATION_PICKER_RESULT_LIMIT)
  ClawPanel {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(text = stringResource(R.string.app_filter), style = ClawTheme.type.section, color = ClawTheme.colors.text)
      Text(
        text = notificationPackageSelectionSummary(mode = mode, selectedCount = selectedPackages.size),
        style = ClawTheme.type.body,
        color = ClawTheme.colors.textMuted,
      )
      ClawSecondaryButton(
        text = if (expanded) stringResource(R.string.close_app_picker) else stringResource(R.string.open_app_picker),
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth(),
      )
      if (expanded) {
        ClawTextField(value = search, onValueChange = onSearchChange, placeholder = stringResource(R.string.search_apps))
        SettingsToggleListRow(
          SettingsToggleRow(
            title = stringResource(R.string.show_system_apps),
            subtitle = stringResource(R.string.include_android_background_packages),
            icon = Icons.Default.Storage,
            checked = showSystemApps,
            onCheckedChange = onShowSystemAppsChange,
          ),
        )
        if (visibleApps.isEmpty()) {
          Text(text = stringResource(R.string.no_matching_apps), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        } else {
          ClawSeparatedColumn(items = visibleApps) { app ->
            NotificationPackageAppRow(
              app = app,
              selected = selectedPackages.contains(app.packageName),
              onSelectedChange = { selected -> onPackageSelectionChange(app.packageName, selected) },
            )
          }
          if (apps.size > visibleApps.size) {
            Text(
              text = stringResource(R.string.showing_apps_format, visibleApps.size, apps.size),
              style = ClawTheme.type.caption,
              color = ClawTheme.colors.textMuted,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationPackageAppRow(
  app: InstalledApp,
  selected: Boolean,
  onSelectedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 58.dp)
        .clickable { onSelectedChange(!selected) }
        .padding(vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    ClawTextBadge(text = notificationAppBadge(app.label))
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
      Text(
        text = app.label,
        style = ClawTheme.type.body,
        color = ClawTheme.colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = app.packageName,
        style = ClawTheme.type.caption,
        color = ClawTheme.colors.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Switch(checked = selected, onCheckedChange = onSelectedChange)
  }
}

@Composable
private fun PhoneCapabilitiesScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val cameraEnabled by viewModel.cameraEnabled.collectAsState()
  val locationMode by viewModel.locationMode.collectAsState()
  val locationPreciseEnabled by viewModel.locationPreciseEnabled.collectAsState()
  val preventSleep by viewModel.preventSleep.collectAsState()
  val canvasDebugStatusEnabled by viewModel.canvasDebugStatusEnabled.collectAsState()
  val installedAppsSharingEnabled by viewModel.installedAppsSharingEnabled.collectAsState()
  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      viewModel.setCameraEnabled(granted)
    }
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
      val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      viewModel.setLocationMode(if (granted) LocationMode.WhileUsing else LocationMode.Off)
      viewModel.setLocationPreciseEnabled(grants[Manifest.permission.ACCESS_FINE_LOCATION] == true)
    }

  fun setCameraAccess(checked: Boolean) {
    if (!checked) {
      viewModel.setCameraEnabled(false)
      return
    }
    if (hasPermission(context, Manifest.permission.CAMERA)) {
      viewModel.setCameraEnabled(true)
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  fun setLocationAccess(mode: LocationMode) {
    if (mode == LocationMode.Off) {
      viewModel.setLocationMode(LocationMode.Off)
      return
    }
    if (hasLocationPermission(context)) {
      viewModel.setLocationMode(LocationMode.WhileUsing)
    } else {
      locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
  }

  fun setPreciseLocation(checked: Boolean) {
    if (!checked) {
      viewModel.setLocationPreciseEnabled(false)
      return
    }
    if (hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
      viewModel.setLocationPreciseEnabled(true)
      viewModel.setLocationMode(LocationMode.WhileUsing)
    } else {
      locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
  }

  SettingsDetailFrame(title = stringResource(R.string.phone_capabilities), subtitle = stringResource(R.string.choose_what_share), icon = Icons.AutoMirrored.Filled.ScreenShare, onBack = onBack) {
    SettingsTogglePanel(
      rows =
        listOf(
          SettingsToggleRow(stringResource(R.string.camera), stringResource(R.string.allow_camera), Icons.Default.CameraAlt, cameraEnabled, ::setCameraAccess),
          SettingsToggleRow(stringResource(R.string.precise_location), stringResource(R.string.precise_location_desc), Icons.Default.LocationOn, locationPreciseEnabled, ::setPreciseLocation),
          SettingsToggleRow(
            stringResource(R.string.installed_apps),
            if (installedAppsSharingEnabled) stringResource(R.string.installed_apps_on) else stringResource(R.string.installed_apps_off),
            Icons.Default.Storage,
            installedAppsSharingEnabled,
            viewModel::setInstalledAppsSharingEnabled,
          ),
          SettingsToggleRow(stringResource(R.string.keep_awake), stringResource(R.string.keep_awake_desc), Icons.Default.Bolt, preventSleep, viewModel::setPreventSleep),
          SettingsToggleRow(stringResource(R.string.canvas_status), stringResource(R.string.canvas_status_desc), Icons.AutoMirrored.Filled.ScreenShare, canvasDebugStatusEnabled, viewModel::setCanvasDebugStatusEnabled),
        ),
    )
    val offLabel = stringResource(R.string.off)
    val whileUsingLabel = stringResource(R.string.while_using)

    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.location), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        ClawSegmentedControl(
          options = listOf(offLabel, whileUsingLabel),
          selected = if (locationMode == LocationMode.WhileUsing) whileUsingLabel else offLabel,
          onSelect = { selected -> setLocationAccess(if (selected == whileUsingLabel) LocationMode.WhileUsing else LocationMode.Off) },
        )
      }
    }
  }
}

@Composable
private fun GatewaySettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val isConnected by viewModel.isConnected.collectAsState()
  val isNodeConnected by viewModel.isNodeConnected.collectAsState()
  val statusText by viewModel.statusText.collectAsState()
  val serverName by viewModel.serverName.collectAsState()
  val remoteAddress by viewModel.remoteAddress.collectAsState()
  val manualHost by viewModel.manualHost.collectAsState()
  val manualPort by viewModel.manualPort.collectAsState()
  val manualTls by viewModel.manualTls.collectAsState()
  val savedBootstrapToken by viewModel.gatewayBootstrapToken.collectAsState()
  val savedGatewayToken by viewModel.gatewayToken.collectAsState()
  var setupCode by remember { mutableStateOf("") }
  var hostInput by remember(manualHost) { mutableStateOf(manualHost.ifBlank { "127.0.0.1" }) }
  var portInput by remember(manualPort) { mutableStateOf(manualPort.toString()) }
  var tlsInput by remember(manualTls) { mutableStateOf(manualTls) }
  var tokenInput by remember { mutableStateOf("") }
  var bootstrapTokenInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var validationText by remember { mutableStateOf<String?>(null) }
  var showSetupCodeHelp by remember { mutableStateOf(false) }

  SettingsDetailFrame(title = stringResource(R.string.gateway), subtitle = stringResource(R.string.connection_between), icon = Icons.Default.Cloud, onBack = onBack) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.connection), if (isConnected) stringResource(R.string.connected) else stringResource(R.string.offline)),
          SettingsMetric(stringResource(R.string.node), if (isNodeConnected) stringResource(R.string.online) else stringResource(R.string.not_paired)),
          SettingsMetric(stringResource(R.string.gateway_label), serverName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_gateway)),
          SettingsMetric(stringResource(R.string.address), remoteAddress?.takeIf { it.isNotBlank() } ?: stringResource(R.string.not_available)),
          SettingsMetric(stringResource(R.string.status), gatewayStatusLabel(statusText = statusText, isConnected = isConnected)),
        ),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawPrimaryButton(text = stringResource(R.string.reconnect), onClick = viewModel::refreshGatewayConnection, modifier = Modifier.weight(1f))
      ClawSecondaryButton(text = stringResource(R.string.disconnect), onClick = viewModel::disconnect, modifier = Modifier.weight(1f))
    }
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = stringResource(R.string.pair_new_gateway), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        Text(text = stringResource(R.string.clear_gateway_access), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ClawSecondaryButton(text = stringResource(R.string.pair_new_gateway), onClick = viewModel::pairNewGateway, modifier = Modifier.weight(1f), icon = Icons.Default.QrCode2)
          ClawSecondaryButton(text = stringResource(R.string.setup_code_button), onClick = { showSetupCodeHelp = !showSetupCodeHelp }, modifier = Modifier.weight(1f), icon = Icons.Default.Info)
        }
        if (showSetupCodeHelp) {
          Text(
            text = stringResource(R.string.setup_code_generation_hint),
            style = ClawTheme.type.caption,
            color = ClawTheme.colors.textMuted,
          )
        }
      }
    }
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.connection_setup), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        ClawTextField(value = setupCode, onValueChange = { setupCode = it }, placeholder = stringResource(R.string.setup_code))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ClawTextField(value = hostInput, onValueChange = { hostInput = it }, placeholder = stringResource(R.string.host_label), modifier = Modifier.weight(1f))
          ClawTextField(value = portInput, onValueChange = { portInput = it }, placeholder = stringResource(R.string.port_label), modifier = Modifier.weight(0.55f))
        }
        val localLabel = stringResource(R.string.local)
        val tlsLabel = stringResource(R.string.tls)
        ClawSegmentedControl(
          options = listOf(localLabel, tlsLabel),
          selected = if (tlsInput) tlsLabel else localLabel,
          onSelect = { selected -> tlsInput = selected == tlsLabel },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ClawTextField(value = tokenInput, onValueChange = { tokenInput = it }, placeholder = stringResource(R.string.token), modifier = Modifier.weight(1f))
          ClawTextField(value = bootstrapTokenInput, onValueChange = { bootstrapTokenInput = it }, placeholder = stringResource(R.string.bootstrap_token), modifier = Modifier.weight(1f))
        }
        ClawTextField(value = passwordInput, onValueChange = { passwordInput = it }, placeholder = stringResource(R.string.password_optional))
        validationText?.let {
          Text(text = it, style = ClawTheme.type.caption, color = ClawTheme.colors.warning)
        }
        val enterValidSetupCode = stringResource(R.string.enter_valid_setup_code)
        ClawPrimaryButton(
          text = stringResource(R.string.save_connect),
          onClick = {
            val setup = setupCode.trim().takeIf { it.isNotEmpty() }?.let(::decodeGatewaySetupCode)
            val endpointConfig =
              if (setup != null) {
                parseGatewayEndpointResult(setup.url).config
              } else {
                composeGatewayManualUrl(hostInput, portInput, tlsInput)?.let { parseGatewayEndpointResult(it).config }
              }
            if (endpointConfig == null) {
              validationText = enterValidSetupCode
              return@ClawPrimaryButton
            }
            val bootstrapToken =
              setup
                ?.bootstrapToken
                ?.trim()
                .orEmpty()
                .ifEmpty { bootstrapTokenInput.trim().ifEmpty { savedBootstrapToken } }
            val token =
              setup
                ?.token
                ?.trim()
                .orEmpty()
                .ifEmpty { tokenInput.trim().ifEmpty { if (bootstrapToken.isBlank()) savedGatewayToken else "" } }
            val password =
              setup
                ?.password
                ?.trim()
                .orEmpty()
                .ifEmpty { passwordInput.trim() }
            validationText = null
            viewModel.saveGatewayConfigAndConnect(
              host = endpointConfig.host,
              port = endpointConfig.port,
              tls = endpointConfig.tls,
              token = token,
              bootstrapToken = bootstrapToken,
              password = password,
              resetSetupAuth = setup != null,
            )
          },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun AppearanceSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val themeMode by viewModel.appearanceThemeMode.collectAsState()

  SettingsDetailFrame(title = stringResource(R.string.appearance), subtitle = stringResource(R.string.appearance_subtitle), icon = Icons.Default.Palette, onBack = onBack) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.theme), appearanceThemeSummary(themeMode)),
          SettingsMetric(stringResource(R.string.contrast), stringResource(R.string.high)),
          SettingsMetric(stringResource(R.string.typography), stringResource(R.string.readable)),
        ),
    )
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = stringResource(R.string.theme), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        ClawSegmentedControl(
          options = appearanceThemeOptions(),
          selected = appearanceThemeSummary(themeMode),
          onSelect = { selected -> viewModel.setAppearanceThemeMode(appearanceThemeModeForLabel(selected)) },
        )
      }
    }
  }
}

internal fun appearanceThemeSummary(mode: AppearanceThemeMode): String = mode.displayLabel

internal fun appearanceThemeOptions(): List<String> = AppearanceThemeMode.entries.map { it.displayLabel }

internal fun appearanceThemeModeForLabel(label: String): AppearanceThemeMode = AppearanceThemeMode.fromDisplayLabel(label)

/** Converts raw gateway connection text into stable settings metric labels. */
@Composable
private fun gatewayStatusLabel(
  statusText: String,
  isConnected: Boolean,
): String {
  if (isConnected) return stringResource(R.string.status_ready)
  val status = statusText.trim().lowercase()
  return when {
    status.contains("connecting") || status.contains("reconnecting") -> stringResource(R.string.status_connecting_progress)
    status.contains("pair") -> stringResource(R.string.status_pairing_needed)
    status.contains("auth") -> stringResource(R.string.status_authentication_needed)
    status.contains("certificate") || status.contains("tls") -> stringResource(R.string.status_certificate_review_needed)
    status.contains("failed") || status.contains("error") || status.contains("offline") || status.contains("not connected") -> stringResource(R.string.status_cannot_reach_gateway)
    status.isBlank() -> stringResource(R.string.status_not_connected)
    else -> stringResource(R.string.status_not_connected)
  }
}

@Composable
private fun AboutSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val isConnected by viewModel.isConnected.collectAsState()
  val serverName by viewModel.serverName.collectAsState()
  val gatewayVersion by viewModel.gatewayVersion.collectAsState()
  val updateAvailable by viewModel.gatewayUpdateAvailable.collectAsState()
  val latestVersion = updateAvailable?.latestVersion?.takeIf { it.isNotBlank() }
  val currentGatewayVersion = updateAvailable?.currentVersion?.takeIf { it.isNotBlank() } ?: gatewayVersion

  SettingsDetailFrame(title = stringResource(R.string.about), subtitle = stringResource(R.string.about_subtitle), icon = Icons.Default.Info, onBack = onBack) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.android_app), BuildConfig.VERSION_NAME),
          SettingsMetric(stringResource(R.string.build), BuildConfig.VERSION_CODE.toString()),
          SettingsMetric(stringResource(R.string.channel), androidDistributionChannel()),
          SettingsMetric(stringResource(R.string.gateway_label), currentGatewayVersion ?: stringResource(R.string.not_connected)),
        ),
    )
    ClawPanel(contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
      Column {
        AboutStatusRow(title = stringResource(R.string.gateway_label), value = serverName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_gateway), healthy = isConnected)
        HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
        AboutStatusRow(title = stringResource(R.string.runtime), value = currentGatewayVersion ?: stringResource(R.string.waiting), healthy = currentGatewayVersion != null)
        HorizontalDivider(color = ClawTheme.colors.border, thickness = 1.dp)
        AboutStatusRow(
          title = stringResource(R.string.update),
          value = latestVersion?.let { stringResource(R.string.version_available, it) } ?: stringResource(R.string.up_to_date),
          healthy = latestVersion == null,
        )
      }
    }
    ClawPanel {
      Text(text = aboutUpdateText(latestVersion = latestVersion), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
    }
  }
}

internal fun androidDistributionChannel(flavor: String = BuildConfig.FLAVOR): String =
  when (flavor.trim()) {
    "play" -> "Play"
    "thirdParty" -> "Third-party"
    "" -> "Unknown"
    else -> flavor.trim()
  }

@Composable
private fun AboutStatusRow(
  title: String,
  value: String,
  healthy: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
      Text(text = title, style = ClawTheme.type.body, color = ClawTheme.colors.text, maxLines = 1)
      Text(text = value, style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    ClawStatusPill(text = if (healthy) stringResource(R.string.ok) else stringResource(R.string.check), status = if (healthy) ClawStatus.Success else ClawStatus.Warning)
  }
}

/** Chooses about-screen copy based on whether the gateway advertises an update. */
@Composable
private fun aboutUpdateText(latestVersion: String?): String =
  if (latestVersion == null) {
    stringResource(R.string.about_body)
  } else {
    stringResource(R.string.update_available)
  }

/**
 * Shared settings detail shell with back navigation, title, subtitle, and section content.
 */
@Composable
internal fun SettingsDetailFrame(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onBack: () -> Unit,
  content: @Composable () -> Unit,
) {
  ClawScaffold(
    contentPadding = PaddingValues(start = ClawTheme.spacing.lg, top = 14.dp, end = ClawTheme.spacing.lg, bottom = 6.dp),
    contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
  ) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
      item {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
          ClawPlainIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
          )
          Text(text = title, style = ClawTheme.type.title, color = ClawTheme.colors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
          SettingsIconMark(icon = icon)
        }
      }
      item {
        Text(text = subtitle, style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
      }
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          content()
        }
      }
    }
  }
}

/**
 * Toggle row model reused by settings sections that render simple on/off controls.
 */
private data class SettingsToggleRow(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val checked: Boolean,
  val onCheckedChange: (Boolean) -> Unit,
)

/**
 * Compact metric row model for connected gateway summaries.
 */
internal data class SettingsMetric(
  val title: String,
  val value: String,
)

@Composable
private fun ExecApprovalsPanel(
  approvals: List<GatewayExecApprovalSummary>,
  onResolve: (String, String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    approvals.forEach { approval ->
      ExecApprovalCard(approval = approval, onResolve = onResolve)
    }
  }
}

@Composable
private fun ExecApprovalCard(
  approval: GatewayExecApprovalSummary,
  onResolve: (String, String) -> Unit,
) {
  val resolving = approval.resolvingDecision != null
  ClawPanel {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(text = approval.commandText, style = ClawTheme.type.body, color = ClawTheme.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
          approval.commandPreview?.let { preview ->
            Text(text = preview, style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
          }
        }
        ClawStatusPill(text = if (resolving) stringResource(R.string.sending) else stringResource(R.string.review), status = if (resolving) ClawStatus.Warning else ClawStatus.Success)
      }
      Text(text = execApprovalMetadata(approval), style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle, maxLines = 2, overflow = TextOverflow.Ellipsis)
      approval.errorText?.let { errorText ->
        Text(text = errorText, style = ClawTheme.type.caption, color = ClawTheme.colors.warning)
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if ("allow-once" in approval.allowedDecisions) {
          ClawPrimaryButton(
            text = if (approval.resolvingDecision == "allow-once") stringResource(R.string.allowing) else stringResource(R.string.allow_once),
            onClick = { onResolve(approval.id, "allow-once") },
            enabled = !resolving,
            modifier = Modifier.weight(1f),
          )
        }
        if ("allow-always" in approval.allowedDecisions) {
          ClawSecondaryButton(
            text = if (approval.resolvingDecision == "allow-always") stringResource(R.string.saving) else stringResource(R.string.always),
            onClick = { onResolve(approval.id, "allow-always") },
            enabled = !resolving,
            modifier = Modifier.weight(1f),
          )
        }
        if ("deny" in approval.allowedDecisions) {
          ClawSecondaryButton(
            text = if (approval.resolvingDecision == "deny") stringResource(R.string.denying) else stringResource(R.string.deny),
            onClick = { onResolve(approval.id, "deny") },
            enabled = !resolving,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun SessionToolCallsPanel(toolCalls: List<ChatPendingToolCall>) {
  ClawListPanel(items = toolCalls) { toolCall ->
    ApprovalListRow(toolCall = toolCall)
  }
}

@Composable
private fun ApprovalListRow(toolCall: ChatPendingToolCall) {
  val hasIssue = toolCall.isError == true
  ClawDetailRow(
    title = approvalActionName(toolCall.name),
    subtitle = approvalSubtitle(toolCall, hasIssue),
    leading = { ClawIconBadge(icon = Icons.Default.Lock) },
    trailing = { ClawStatusPill(text = if (hasIssue) stringResource(R.string.issue) else stringResource(R.string.review), status = if (hasIssue) ClawStatus.Warning else ClawStatus.Success) },
  )
}

@Composable
private fun CronJobsPanel(jobs: List<GatewayCronJobSummary>) {
  ClawListPanel(items = jobs) { job ->
    CronJobListRow(job = job)
  }
}

@Composable
private fun UsageProvidersPanel(providers: List<GatewayUsageProviderSummary>) {
  ClawListPanel(items = providers) { provider ->
    UsageProviderListRow(provider = provider)
  }
}

@Composable
private fun UsageProviderListRow(provider: GatewayUsageProviderSummary) {
  val hasIssue = provider.error != null
  ClawDetailRow(
    title = provider.displayName,
    subtitle = usageProviderSubtitle(provider),
    leading = { ClawTextBadge(text = provider.displayName.firstOrNull()?.uppercase() ?: "U") },
    trailing = { ClawStatusPill(text = if (hasIssue) stringResource(R.string.issue) else stringResource(R.string.ok), status = if (hasIssue) ClawStatus.Warning else ClawStatus.Success) },
  )
}

@Composable
private fun CronJobListRow(job: GatewayCronJobSummary) {
  ClawDetailRow(
    title = job.name,
    subtitle = cronJobSubtitle(job),
    leading = { ClawIconBadge(icon = Icons.Default.Bolt) },
    trailing = { ClawStatusPill(text = cronJobStatusText(job), status = cronJobStatus(job)) },
  )
}

@Composable
private fun AgentsPanel(
  agents: List<GatewayAgentSummary>,
  defaultAgentId: String?,
) {
  ClawListPanel(items = agents) { agent ->
    AgentListRow(agent = agent, isDefault = agent.id == defaultAgentId)
  }
}

@Composable
private fun AgentListRow(
  agent: GatewayAgentSummary,
  isDefault: Boolean,
) {
  ClawDetailRow(
    title = agent.name?.takeIf { it.isNotBlank() } ?: agent.id,
    subtitle = if (isDefault) stringResource(R.string.default_assistant) else stringResource(R.string.ready),
    leading = { ClawTextBadge(text = agentBadge(agent)) },
    trailing = { ClawStatusPill(text = if (isDefault) stringResource(R.string.default_) else stringResource(R.string.ready), status = ClawStatus.Success) },
  )
}

/**
 * Chooses a display name for the configured default agent, falling back to any available agent.
 */
@Composable
private fun defaultAgentName(
  agents: List<GatewayAgentSummary>,
  defaultAgentId: String?,
): String {
  val defaultId = defaultAgentId?.trim().orEmpty()
  val agent = agents.firstOrNull { it.id == defaultId } ?: agents.firstOrNull()
  return agent?.name?.takeIf { it.isNotBlank() } ?: agent?.id ?: stringResource(R.string.none)
}

/**
 * Builds a short stable badge from agent emoji/name/id for dense lists.
 */
private fun agentBadge(agent: GatewayAgentSummary): String {
  agent.emoji
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let { return it }
  val source = agent.name?.takeIf { it.isNotBlank() } ?: agent.id
  return source
    .split(' ', '-', '_')
    .filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
    .joinToString("")
    .ifBlank { "A" }
}

/**
 * Normalizes tool-call names into readable approval action labels.
 */
@Composable
private fun approvalActionName(name: String): String {
  val cleaned =
    name
      .replace('.', ' ')
      .replace('_', ' ')
      .replace('-', ' ')
      .trim()
  return cleaned
    .split(' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
    .ifBlank { stringResource(R.string.action_request) }
}

/** Builds approval row age/error copy without exposing raw tool arguments. */
@Composable
private fun approvalSubtitle(
  toolCall: ChatPendingToolCall,
  hasIssue: Boolean,
): String {
  if (hasIssue) return stringResource(R.string.status_needs_attention)
  val ageMs = (System.currentTimeMillis() - toolCall.startedAtMs).coerceAtLeast(0L)
  val minutes = ageMs / 60_000L
  return if (minutes < 1) stringResource(R.string.waiting_for_review) else stringResource(R.string.waiting_minutes, minutes)
}

@Composable
private fun execApprovalMetadata(approval: GatewayExecApprovalSummary): String {
  val target =
    when {
      approval.host == "node" && approval.nodeId != null -> stringResource(R.string.node_id, approval.nodeId.take(8))
      approval.host != null -> approval.host.replaceFirstChar { it.uppercaseChar() }
      else -> stringResource(R.string.gateway)
    }
  val agent = approval.agentId?.let { stringResource(R.string.agent_id, it.take(8)) }
  val age = approval.createdAtMs?.let { stringResource(R.string.waiting_duration, formatApprovalDuration(System.currentTimeMillis() - it)) }
  val expires = approval.expiresAtMs?.let { stringResource(R.string.expires_duration, formatApprovalDuration(it - System.currentTimeMillis())) }
  return listOfNotNull(target, agent, age, expires).joinToString(" · ")
}

@Composable
private fun formatApprovalDuration(deltaMs: Long): String {
  val safeDelta = deltaMs.coerceAtLeast(0L)
  val minutes = safeDelta / 60_000L
  val hours = minutes / 60L
  return when {
    minutes < 1 -> stringResource(R.string.soon)
    hours < 1 -> "${minutes}m"
    else -> "${hours}h"
  }
}

/** Builds the dense cron-job subtitle from schedule, next wake, and prompt preview. */
@Composable
private fun cronJobSubtitle(job: GatewayCronJobSummary): String = "${job.scheduleLabel} · ${formatCronWake(job.nextRunAtMs)} · ${job.promptPreview}"

/** Summarizes a provider plan and most-used quota window for usage rows. */
@Composable
private fun usageProviderSubtitle(provider: GatewayUsageProviderSummary): String {
  provider.error?.let { return it }
  val window = provider.windows.maxByOrNull { it.usedPercent }
  val quota = window?.let { stringResource(R.string.percent_left, (100.0 - it.usedPercent).coerceIn(0.0, 100.0).toInt(), it.label) }
  return listOfNotNull(provider.plan, quota).joinToString(" · ").ifBlank { stringResource(R.string.no_limits_reported) }
}

/**
 * Converts usage timestamps into short relative labels for metric panels.
 */
@Composable
private fun formatUsageUpdated(updatedAtMs: Long?): String {
  val updated = updatedAtMs ?: return stringResource(R.string.never)
  val deltaMs = (System.currentTimeMillis() - updated).coerceAtLeast(0L)
  val minutes = deltaMs / 60_000L
  val hours = minutes / 60L
  return when {
    minutes < 1 -> stringResource(R.string.now)
    hours < 1 -> "${minutes}m"
    hours < 24 -> "${hours}h"
    else -> "${hours / 24L}d"
  }
}

/** Converts gateway cron status text into the short row badge label. */
@Composable
private fun cronJobStatusText(job: GatewayCronJobSummary): String {
  if (!job.enabled) return stringResource(R.string.off)
  return when (job.lastRunStatus?.lowercase()) {
    "error" -> stringResource(R.string.issue)
    "ok" -> stringResource(R.string.ok)
    "skipped" -> stringResource(R.string.skipped)
    else -> stringResource(R.string.ready)
  }
}

/** Maps gateway cron status text to app status colors. */
private fun cronJobStatus(job: GatewayCronJobSummary): ClawStatus {
  if (!job.enabled) return ClawStatus.Neutral
  return when (job.lastRunStatus?.lowercase()) {
    "error" -> ClawStatus.Danger
    "skipped" -> ClawStatus.Warning
    else -> ClawStatus.Success
  }
}

/** Applies query/system visibility rules while always preserving selected packages. */
internal fun filterNotificationAppsForPicker(
  apps: List<InstalledApp>,
  selectedPackages: Set<String>,
  query: String,
  showSystemApps: Boolean,
): List<InstalledApp> {
  val normalizedQuery = query.trim().lowercase()
  return apps.filter { app ->
    val selected = app.packageName in selectedPackages
    val visibleByType = showSystemApps || !app.isSystemApp || selected
    val visibleBySearch =
      normalizedQuery.isEmpty() ||
        app.label.lowercase().contains(normalizedQuery) ||
        app.packageName.lowercase().contains(normalizedQuery)
    visibleByType && visibleBySearch
  }
}

/** Summarizes allowlist/blocklist mode with an empty-state warning when needed. */
@Composable
private fun notificationPackageSelectionSummary(
  mode: NotificationPackageFilterMode,
  selectedCount: Int,
): String =
  when (mode) {
    NotificationPackageFilterMode.Allowlist ->
      if (selectedCount == 0) {
        stringResource(R.string.no_apps_selected_nothing_forwards)
      } else {
        if (selectedCount == 1) stringResource(R.string.one_app_allowed_to_forward) else stringResource(R.string.apps_allowed_to_forward, selectedCount)
      }
    NotificationPackageFilterMode.Blocklist ->
      if (selectedCount == 0) {
        stringResource(R.string.no_apps_blocked)
      } else {
        if (selectedCount == 1) stringResource(R.string.one_app_blocked) else stringResource(R.string.apps_blocked_from_forwarding, selectedCount)
      }
  }

/** Builds compact two-letter app badges from package-picker labels. */
private fun notificationAppBadge(label: String): String {
  val initials =
    label
      .split(' ', '-', '_', '.')
      .asSequence()
      .filter { it.isNotBlank() }
      .take(2)
      .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
      .joinToString("")
  return initials.ifBlank { "A" }
}

/**
 * Converts cron wake times into short relative labels for scheduled-work rows.
 */
@Composable
private fun formatCronWake(timeMs: Long?): String {
  val target = timeMs ?: return stringResource(R.string.none)
  val deltaMs = target - System.currentTimeMillis()
  if (deltaMs <= 0) return stringResource(R.string.due)
  val minutes = deltaMs / 60_000L
  val hours = minutes / 60L
  val days = hours / 24L
  return when {
    days > 0 -> "${days}d"
    hours > 0 -> "${hours}h"
    minutes > 0 -> "${minutes}m"
    else -> stringResource(R.string.soon_label)
  }
}

@Composable
private fun SettingsTogglePanel(rows: List<SettingsToggleRow>) {
  ClawPanel(contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
    ClawSeparatedColumn(items = rows) { row ->
      SettingsToggleListRow(row)
    }
  }
}

@Composable
private fun SettingsToggleListRow(row: SettingsToggleRow) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .clickable { row.onCheckedChange(!row.checked) }
        .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    Icon(imageVector = row.icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = ClawTheme.colors.text)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
      Text(text = row.title, style = ClawTheme.type.body, color = ClawTheme.colors.text, maxLines = 1)
      Text(text = row.subtitle, style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    Switch(checked = row.checked, onCheckedChange = row.onCheckedChange)
  }
}

/**
 * Reusable metric panel for settings screens with compact title/value rows.
 */
@Composable
internal fun SettingsMetricPanel(rows: List<SettingsMetric>) {
  ClawPanel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
    ClawSeparatedColumn(items = rows) { row ->
      Row(modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 0.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = row.title, style = ClawTheme.type.body, color = ClawTheme.colors.text, modifier = Modifier.weight(1f), maxLines = 1)
        Text(text = row.value, style = ClawTheme.type.caption.copy(fontSize = 13.sp, lineHeight = 17.sp), color = ClawTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
private fun SettingsIconMark(icon: ImageVector) {
  Surface(
    modifier = Modifier.size(30.dp),
    shape = CircleShape,
    color = ClawTheme.colors.surfaceRaised,
    border = BorderStroke(1.dp, ClawTheme.colors.border),
    contentColor = ClawTheme.colors.text,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
    }
  }
}

/**
 * Checks an exact Android runtime permission for settings enablement.
 */
private fun hasPermission(
  context: Context,
  permission: String,
): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Returns true when either fine or coarse location is available to settings callers. */
private fun hasLocationPermission(context: Context): Boolean =
  hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
    hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

private fun openNotificationListenerSettings(context: Context) {
  val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  context.startActivity(intent)
}
