package ai.openclaw.app.ui

import ai.openclaw.app.R
import ai.openclaw.app.BuildConfig
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast

/** App version label shared by diagnostics and gateway-facing Android metadata. */
internal fun openClawAndroidVersionLabel(): String {
  val versionName = BuildConfig.VERSION_NAME.trim().ifEmpty { "dev" }
  return if (BuildConfig.DEBUG && !versionName.contains("dev", ignoreCase = true)) {
    "$versionName-dev"
  } else {
    versionName
  }
}

/** Normalizes blank gateway status text for display and diagnostics copy. */
internal fun gatewayStatusForDisplay(statusText: String): String = statusText.trim().ifEmpty { "Offline" }

/** Returns true when the status has enough signal to show diagnostics affordances. */
internal fun gatewayStatusHasDiagnostics(statusText: String): Boolean {
  val lower = statusText.trim().lowercase()
  return lower != "offline" && lower != "离线" && !lower.contains("connecting") && !lower.contains("连接中")
}

/** Detects pairing/approval status text so UI can offer pairing-specific actions. */
internal fun gatewayStatusLooksLikePairing(statusText: String): Boolean {
  val lower = statusText.trim().lowercase()
  return lower.contains("pair") || lower.contains("approve") ||
    lower.contains("配对") || lower.contains("批准") || lower.contains("待审批")
}

/** Builds the copyable support prompt with device, endpoint, and exact status context. */
internal fun buildGatewayDiagnosticsReport(
  screen: String,
  gatewayAddress: String,
  statusText: String,
): String {
  val device =
    listOfNotNull(Build.MANUFACTURER, Build.MODEL)
      .joinToString(" ")
      .trim()
      .ifEmpty { "Android" }
  val androidVersion =
    Build.VERSION.RELEASE
      ?.trim()
      .orEmpty()
      .ifEmpty { Build.VERSION.SDK_INT.toString() }
  val endpoint = gatewayAddress.trim().ifEmpty { "unknown" }
  val status = gatewayStatusForDisplay(statusText)
  return """
    Help diagnose this OpenClaw Android gateway connection failure.

    Please:
    - pick one route only: same machine, same LAN, Tailscale, or public URL
    - classify this as pairing/auth, TLS trust, wrong advertised route, wrong address/port, or gateway down
    - remember: public routes require wss:// or Tailscale Serve; ws:// is allowed for localhost, the Android emulator, and private LAN IPs
    - quote the exact app status/error below
    - tell me whether `openclaw devices list` should show a pending pairing request
    - if more signal is needed, ask for `openclaw qr --json`, `openclaw devices list`, and `openclaw nodes status`
    - give the next exact command or tap

    Debug info:
    - screen: $screen
    - app version: ${openClawAndroidVersionLabel()}
    - device: $device
    - android: $androidVersion (SDK ${Build.VERSION.SDK_INT})
    - gateway address: $endpoint
    - status/error: $status
    """.trimIndent()
}

/** Copies the diagnostics report to Android clipboard and shows a short confirmation toast. */
internal fun copyGatewayDiagnosticsReport(
  context: Context,
  screen: String,
  gatewayAddress: String,
  statusText: String,
) {
  val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
  val report = buildGatewayDiagnosticsReport(screen = screen, gatewayAddress = gatewayAddress, statusText = statusText)
  clipboard.setPrimaryClip(ClipData.newPlainText("OpenClaw gateway diagnostics", report))
  Toast.makeText(context, context.getString(R.string.copied_gateway_diagnostics), Toast.LENGTH_SHORT).show()
}
