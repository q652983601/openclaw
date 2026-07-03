package ai.openclaw.app.ui

import ai.openclaw.app.MainViewModel
import ai.openclaw.app.ui.design.ClawPanel
import ai.openclaw.app.ui.design.ClawPrimaryButton
import ai.openclaw.app.ui.design.ClawSecondaryButton
import ai.openclaw.app.ui.design.ClawTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import ai.openclaw.app.R

/** Settings detail surface for live canvas status, refresh, and embedded preview. */
@Composable
internal fun CanvasSettingsScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  val isConnected by viewModel.isConnected.collectAsState()
  val currentUrl by viewModel.canvasCurrentUrl.collectAsState()
  val hydrated by viewModel.canvasA2uiHydrated.collectAsState()
  val rehydratePending by viewModel.canvasRehydratePending.collectAsState()
  val rehydrateErrorText by viewModel.canvasRehydrateErrorText.collectAsState()
  val hasLivePage = currentUrl?.isNotBlank() == true
  val showCanvasSurface = isConnected
  val canvasLabel = if (hasLivePage) stringResource(R.string.canvas_live_page) else stringResource(R.string.canvas_home_canvas)

  LaunchedEffect(isConnected) {
    if (isConnected) {
      // Refresh once when the gateway comes online so the settings preview is
      // populated before the user manually asks for a rehydrate.
      viewModel.refreshHomeCanvasOverviewIfConnected()
    }
  }

  SettingsDetailFrame(
    title = stringResource(R.string.canvas_detail_title),
    subtitle = stringResource(R.string.canvas_detail_subtitle),
    icon = Icons.AutoMirrored.Filled.ScreenShare,
    onBack = onBack,
  ) {
    SettingsMetricPanel(
      rows =
        listOf(
          SettingsMetric(stringResource(R.string.canvas_metric_connection), if (isConnected) stringResource(R.string.canvas_bridge_ready) else stringResource(R.string.health_metric_offline)),
          SettingsMetric(stringResource(R.string.canvas_metric_surface), canvasLabel),
          SettingsMetric(stringResource(R.string.canvas_metric_bridge), if (hasLivePage && hydrated) stringResource(R.string.canvas_bridge_ready) else stringResource(R.string.canvas_bridge_standby)),
        ),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawPrimaryButton(
        text = if (rehydratePending) stringResource(R.string.canvas_refreshing) else stringResource(R.string.canvas_refresh_screen),
        onClick = { viewModel.requestCanvasRehydrate(source = "settings_canvas") },
        enabled = isConnected && !rehydratePending,
        modifier = Modifier.weight(1f),
      )
      ClawSecondaryButton(
        text = stringResource(R.string.canvas_reconnect),
        onClick = viewModel::refreshGatewayConnection,
        modifier = Modifier.weight(1f),
      )
    }
    rehydrateErrorText?.let {
      ClawPanel {
        Text(text = it, style = ClawTheme.type.body, color = ClawTheme.colors.warning)
      }
    }
    ClawPanel(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = canvasLabel, style = ClawTheme.type.section, color = ClawTheme.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Surface(
          modifier = Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(ClawTheme.radii.panel)),
          shape = RoundedCornerShape(ClawTheme.radii.panel),
          color = ClawTheme.colors.canvas,
          border = BorderStroke(1.dp, ClawTheme.colors.border),
        ) {
          Box {
            if (showCanvasSurface) {
              CanvasScreen(viewModel = viewModel, visible = true, modifier = Modifier.fillMaxWidth().height(520.dp))
            } else {
              CanvasStandbyPanel(isConnected = isConnected)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CanvasStandbyPanel(isConnected: Boolean) {
  Column(
    modifier = Modifier.fillMaxWidth().height(520.dp).padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      modifier = Modifier.size(54.dp),
      shape = RoundedCornerShape(ClawTheme.radii.panel),
      color = ClawTheme.colors.surfacePressed,
      border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
      contentColor = ClawTheme.colors.text,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ScreenShare, contentDescription = null, modifier = Modifier.size(26.dp))
      }
    }
    Text(
      text = if (isConnected) stringResource(R.string.screen_surface_ready) else stringResource(R.string.connect_gateway_for_canvas),
      style = ClawTheme.type.title,
      color = ClawTheme.colors.text,
      modifier = Modifier.padding(top = 18.dp),
    )
    Text(
      text = if (isConnected) stringResource(R.string.canvas_surface_ready_hint) else stringResource(R.string.canvas_needs_gateway),
      style = ClawTheme.type.body,
      color = ClawTheme.colors.textMuted,
      modifier = Modifier.padding(top = 6.dp),
    )
  }
}
