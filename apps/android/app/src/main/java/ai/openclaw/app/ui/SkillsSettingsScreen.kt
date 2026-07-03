package ai.openclaw.app.ui

import ai.openclaw.app.GatewaySkillSummary
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.ui.design.ClawDetailRow
import ai.openclaw.app.ui.design.ClawListPanel
import ai.openclaw.app.ui.design.ClawPanel
import ai.openclaw.app.ui.design.ClawSecondaryButton
import ai.openclaw.app.ui.design.ClawStatus
import ai.openclaw.app.ui.design.ClawStatusPill
import ai.openclaw.app.ui.design.ClawTextBadge
import ai.openclaw.app.ui.design.ClawTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import ai.openclaw.app.R

/** Settings screen for gateway skills and their readiness state. */
@Composable
internal fun SkillsSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val skillsSummary by viewModel.skillsSummary.collectAsState()
  val skillsRefreshing by viewModel.skillsRefreshing.collectAsState()
  val skillsErrorText by viewModel.skillsErrorText.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()
  val skills = skillsSummary.skills
  val readyCount = skills.count { skillReady(it) }
  val needsSetupCount = skills.count { skillNeedsSetup(it) }
  var selectedSkillKey by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(isConnected) {
    if (isConnected) {
      viewModel.refreshSkills()
    }
  }

  selectedSkillKey?.let { skillKey ->
    val selectedSkill = skills.firstOrNull { it.skillKey == skillKey }
    SkillDetailSettingsScreen(
      skill = selectedSkill,
      skillKey = skillKey,
      isConnected = isConnected,
      onBack = { selectedSkillKey = null },
    )
    return
  }

  SettingsDetailFrame(
    title = stringResource(R.string.skills_detail_title),
    subtitle = stringResource(R.string.skills_detail_subtitle),
    icon = Icons.Default.Settings,
    onBack = onBack,
  ) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.skills_metric_installed), skills.size.toString()),
          SettingsMetric(stringResource(R.string.skills_metric_ready), readyCount.toString()),
          SettingsMetric(stringResource(R.string.skills_metric_needs_setup), needsSetupCount.toString()),
        ),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawSecondaryButton(
        text = if (skillsRefreshing) stringResource(R.string.skills_refreshing) else stringResource(R.string.skills_refresh),
        onClick = viewModel::refreshSkills,
        enabled = isConnected && !skillsRefreshing,
        modifier = Modifier.weight(1f),
      )
    }
    skillsErrorText?.let { errorText ->
      ClawPanel {
        Text(text = errorText, style = ClawTheme.type.body, color = ClawTheme.colors.warning)
      }
    }
    when {
      !isConnected ->
        ClawPanel {
          Text(text = stringResource(R.string.connect_gateway_load_skills), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
        }
      skills.isEmpty() ->
        ClawPanel {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = stringResource(R.string.no_skills_installed), style = ClawTheme.type.section, color = ClawTheme.colors.text)
            Text(text = stringResource(R.string.skills_install_hint), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
          }
        }
      else -> SkillsPanel(skills = skills, onSkillClick = { selectedSkillKey = it.skillKey })
    }
  }
}

@Composable
private fun SkillDetailSettingsScreen(
  skill: GatewaySkillSummary?,
  skillKey: String,
  isConnected: Boolean,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  SettingsDetailFrame(
    title = skill?.name ?: skillKey,
    subtitle = stringResource(R.string.skill_detail_subtitle),
    icon = Icons.Default.Settings,
    onBack = onBack,
  ) {
    skill?.let { summary ->
      SettingsMetricPanel(
        rows =
          listOf(
            SettingsMetric(stringResource(R.string.skill_metric_status), skillStatusText(summary)),
            SettingsMetric(stringResource(R.string.skill_metric_source), skillSourceLabel(summary)),
            SettingsMetric(stringResource(R.string.skill_metric_missing), summary.missingCount.toString()),
          ),
      )
      SkillSetupPanel(summary)
    }
    SkillDetailPanel(skill = skill, isConnected = isConnected)
  }
}

@Composable
private fun SkillSetupPanel(skill: GatewaySkillSummary) {
  ClawPanel {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(text = stringResource(R.string.skill_setup), style = ClawTheme.type.section, color = ClawTheme.colors.text)
      Text(text = skillConfigurationText(skill), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
    }
  }
}

@Composable
private fun SkillDetailPanel(
  skill: GatewaySkillSummary?,
  isConnected: Boolean,
) {
  if (!isConnected) {
    ClawPanel {
      Text(text = stringResource(R.string.connect_gateway_load_skills), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
    }
    return
  }
  if (skill == null) {
    ClawPanel {
      Text(text = stringResource(R.string.skill_detail_unavailable), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
    }
    return
  }
  SettingsMetricPanel(
    rows =
      listOf(
        SettingsMetric(stringResource(R.string.skill_metric_skill_key), skill.skillKey),
        SettingsMetric(stringResource(R.string.skill_metric_display), skill.name),
        SettingsMetric(stringResource(R.string.skill_metric_source), skillSourceLabel(skill)),
        SettingsMetric(stringResource(R.string.skill_metric_install_options), skill.installCount.toString()),
      ),
  )
  skill.description?.let { description ->
    ClawPanel {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(R.string.skill_description), style = ClawTheme.type.section, color = ClawTheme.colors.text)
        Text(text = description, style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
      }
    }
  }
}

@Composable
private fun SkillsPanel(
  skills: List<GatewaySkillSummary>,
  onSkillClick: (GatewaySkillSummary) -> Unit,
) {
  ClawListPanel(items = skills) { skill ->
    SkillListRow(skill = skill, onClick = { onSkillClick(skill) })
  }
}

@Composable
private fun SkillListRow(
  skill: GatewaySkillSummary,
  onClick: () -> Unit,
) {
  ClawDetailRow(
    title = skill.name,
    subtitle = skillSubtitle(skill),
    modifier = Modifier.clickable(onClickLabel = stringResource(R.string.open_skill_detail), onClick = onClick),
    leading = { ClawTextBadge(text = skillBadge(skill)) },
    trailing = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ClawStatusPill(text = skillStatusText(skill), status = skillStatus(skill))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = ClawTheme.colors.textSubtle,
        )
      }
    },
  )
}

private fun skillReady(skill: GatewaySkillSummary): Boolean = !skill.disabled && skill.eligible && skill.missingCount == 0

private fun skillNeedsSetup(skill: GatewaySkillSummary): Boolean = !skill.disabled && (skill.blockedByAllowlist || !skill.eligible || skill.missingCount > 0)

@Composable
private fun skillStatusText(skill: GatewaySkillSummary): String =
  when {
    skill.disabled -> stringResource(R.string.skill_status_off)
    skillNeedsSetup(skill) -> stringResource(R.string.skill_status_setup)
    else -> stringResource(R.string.skill_status_ready)
  }

private fun skillStatus(skill: GatewaySkillSummary): ClawStatus =
  when {
    skill.disabled -> ClawStatus.Neutral
    skillNeedsSetup(skill) -> ClawStatus.Warning
    else -> ClawStatus.Success
  }

@Composable
private fun skillSubtitle(skill: GatewaySkillSummary): String {
  val issue =
    when {
      skill.disabled -> stringResource(R.string.skill_disabled)
      skill.blockedByAllowlist -> stringResource(R.string.skill_blocked)
      skill.missingCount > 0 -> stringResource(R.string.skill_missing_plural, skill.missingCount)
      !skill.eligible -> stringResource(R.string.skill_needs_setup)
      else -> null
    }
  return listOfNotNull(skill.description, skillSourceLabel(skill), issue).joinToString(" · ")
}

@Composable
private fun skillConfigurationText(skill: GatewaySkillSummary): String =
  when {
    skill.disabled -> stringResource(R.string.skill_disabled_detail)
    skill.blockedByAllowlist -> stringResource(R.string.skill_blocked_detail)
    skill.missingCount > 0 -> stringResource(R.string.skill_missing_detail, skill.missingCount)
    !skill.eligible -> stringResource(R.string.skill_not_eligible_detail)
    else -> stringResource(R.string.skill_ready_detail)
  }

@Composable
private fun skillSourceLabel(skill: GatewaySkillSummary): String =
  when (skill.source) {
    "openclaw-bundled" -> if (skill.bundled) stringResource(R.string.skill_source_built_in) else stringResource(R.string.skill_source_bundled)
    "openclaw-managed" -> stringResource(R.string.skill_source_installed)
    "openclaw-workspace" -> stringResource(R.string.skill_source_workspace)
    "openclaw-extra" -> stringResource(R.string.skill_source_extra)
    else -> stringResource(R.string.skill_source_default)
  }

private fun skillBadge(skill: GatewaySkillSummary): String {
  skill.emoji?.let { return it }
  return skill.name
    .split(' ', '-', '_')
    .filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
    .joinToString("")
    .ifBlank { "S" }
}
