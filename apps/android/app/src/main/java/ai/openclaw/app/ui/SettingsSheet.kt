package ai.openclaw.app.ui

import ai.openclaw.app.R
import ai.openclaw.app.BuildConfig
import ai.openclaw.app.LocationMode
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.NotificationPackageFilterMode
import ai.openclaw.app.SensitiveFeatureConfig
import ai.openclaw.app.node.DeviceNotificationListenerService
import ai.openclaw.app.normalizeLocalHourMinute
import ai.openclaw.app.update.AppUpdateManager
import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Mobile settings surface for device permissions, forwarding, location, and app preferences. */
@Composable
fun SettingsSheet(viewModel: MainViewModel) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val instanceId by viewModel.instanceId.collectAsState()
  val displayName by viewModel.displayName.collectAsState()
  val cameraEnabled by viewModel.cameraEnabled.collectAsState()
  val locationMode by viewModel.locationMode.collectAsState()
  val locationPreciseEnabled by viewModel.locationPreciseEnabled.collectAsState()
  val preventSleep by viewModel.preventSleep.collectAsState()
  val canvasDebugStatusEnabled by viewModel.canvasDebugStatusEnabled.collectAsState()
  val notificationForwardingEnabled by viewModel.notificationForwardingEnabled.collectAsState()
  val notificationForwardingMode by viewModel.notificationForwardingMode.collectAsState()
  val notificationForwardingPackages by viewModel.notificationForwardingPackages.collectAsState()
  val notificationForwardingQuietHoursEnabled by viewModel.notificationForwardingQuietHoursEnabled.collectAsState()
  val notificationForwardingQuietStart by viewModel.notificationForwardingQuietStart.collectAsState()
  val notificationForwardingQuietEnd by viewModel.notificationForwardingQuietEnd.collectAsState()
  val notificationForwardingMaxEventsPerMinute by viewModel.notificationForwardingMaxEventsPerMinute.collectAsState()
  val notificationForwardingSessionKey by viewModel.notificationForwardingSessionKey.collectAsState()
  val appearanceThemeMode by viewModel.appearanceThemeMode.collectAsState()
  val voiceCaptureMode by viewModel.voiceCaptureMode.collectAsState()
  val appUpdateState by viewModel.appUpdateState.collectAsState()
  val appUpdateCheckResult by viewModel.appUpdateCheckResult.collectAsState()

  var notificationQuietStartDraft by remember(notificationForwardingQuietStart) {
    mutableStateOf(notificationForwardingQuietStart)
  }
  var notificationQuietEndDraft by remember(notificationForwardingQuietEnd) {
    mutableStateOf(notificationForwardingQuietEnd)
  }
  var notificationRateDraft by remember(notificationForwardingMaxEventsPerMinute) {
    mutableStateOf(notificationForwardingMaxEventsPerMinute.toString())
  }
  var notificationSessionKeyDraft by remember(notificationForwardingSessionKey) {
    mutableStateOf(notificationForwardingSessionKey.orEmpty())
  }
  val normalizedQuietStartDraft =
    remember(notificationQuietStartDraft) {
      normalizeLocalHourMinute(notificationQuietStartDraft)
    }
  val normalizedQuietEndDraft =
    remember(notificationQuietEndDraft) {
      normalizeLocalHourMinute(notificationQuietEndDraft)
    }
  val quietHoursDraftValid = normalizedQuietStartDraft != null && normalizedQuietEndDraft != null
  val selectedPackagesSummary =
    remember(notificationForwardingMode, notificationForwardingPackages) {
      when (notificationForwardingMode) {
        NotificationPackageFilterMode.Allowlist ->
          if (notificationForwardingPackages.isEmpty()) {
            context.getString(R.string.allowlist_none_summary)
          } else {
            context.getString(R.string.allowlist_count_summary, notificationForwardingPackages.size)
          }
        NotificationPackageFilterMode.Blocklist ->
          if (notificationForwardingPackages.isEmpty()) {
            context.getString(R.string.blocklist_none_summary)
          } else {
            context.getString(R.string.blocklist_count_summary, notificationForwardingPackages.size)
          }
      }
    }
  val quietHoursCanEnable = notificationForwardingEnabled && quietHoursDraftValid
  // Compare stored values against normalized drafts so equivalent HH:mm input
  // does not keep the save button enabled.
  val quietHoursDraftDirty =
    notificationForwardingQuietStart != (normalizedQuietStartDraft ?: notificationQuietStartDraft.trim()) ||
      notificationForwardingQuietEnd != (normalizedQuietEndDraft ?: notificationQuietEndDraft.trim())
  val quietHoursSaveEnabled = notificationForwardingEnabled && quietHoursDraftValid && quietHoursDraftDirty

  val listState = rememberLazyListState()
  val deviceModel =
    remember {
      listOfNotNull(Build.MANUFACTURER, Build.MODEL)
        .joinToString(" ")
        .trim()
        .ifEmpty { "Android" }
    }
  val appVersion =
    remember {
      val versionName = BuildConfig.VERSION_NAME.trim().ifEmpty { "dev" }
      if (BuildConfig.DEBUG && !versionName.contains("dev", ignoreCase = true)) {
        "$versionName-dev"
      } else {
        versionName
      }
    }
  var assistantRoleAvailable by remember(context) { mutableStateOf(isAssistantRoleAvailable(context)) }
  var assistantRoleHeld by remember(context) { mutableStateOf(isAssistantRoleHeld(context)) }
  val listItemColors =
    ListItemDefaults.colors(
      containerColor = Color.Transparent,
      headlineColor = mobileText,
      supportingColor = mobileTextSecondary,
      trailingIconColor = mobileTextSecondary,
      leadingIconColor = mobileTextSecondary,
    )

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
      val cameraOk = perms[Manifest.permission.CAMERA] == true
      viewModel.setCameraEnabled(cameraOk)
    }

  var pendingLocationRequest by remember { mutableStateOf(false) }
  var pendingPreciseToggle by remember { mutableStateOf(false) }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
      val fineOk = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
      val coarseOk = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      val granted = fineOk || coarseOk

      if (pendingPreciseToggle) {
        pendingPreciseToggle = false
        viewModel.setLocationPreciseEnabled(fineOk)
        return@rememberLauncherForActivityResult
      }

      if (pendingLocationRequest) {
        pendingLocationRequest = false
        viewModel.setLocationMode(if (granted) LocationMode.WhileUsing else LocationMode.Off)
      }
    }

  var micPermissionGranted by
    remember {
      mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
          PackageManager.PERMISSION_GRANTED,
      )
    }
  val audioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      micPermissionGranted = granted
    }

  val smsPermissionAvailable =
    remember {
      SensitiveFeatureConfig.smsEnabled &&
        context.packageManager?.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) == true
    }
  val callLogPermissionAvailable = remember { SensitiveFeatureConfig.callLogEnabled }
  val photosPermissionAvailable = remember { SensitiveFeatureConfig.photosEnabled }
  val photosPermission =
    if (Build.VERSION.SDK_INT >= 33) {
      Manifest.permission.READ_MEDIA_IMAGES
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
  val motionPermissionRequired = true
  val motionAvailable = remember(context) { hasMotionCapabilities(context) }

  var notificationsPermissionGranted by
    remember {
      mutableStateOf(hasNotificationsPermission(context))
    }
  val notificationsPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      notificationsPermissionGranted = granted
    }

  var notificationListenerEnabled by
    remember {
      mutableStateOf(isNotificationListenerEnabled(context))
    }
  val notificationForwardingAvailable = notificationForwardingEnabled && notificationListenerEnabled
  val notificationForwardingControlsAlpha = if (notificationForwardingAvailable) 1f else 0.6f

  var notificationPickerExpanded by remember { mutableStateOf(false) }
  var notificationAppSearch by remember { mutableStateOf("") }
  var notificationShowSystemApps by remember { mutableStateOf(false) }
  var installedNotificationApps by
    remember(context, notificationForwardingPackages) {
      mutableStateOf(queryInstalledApps(context, notificationForwardingPackages))
    }

  var photosPermissionGranted by
    remember {
      mutableStateOf(
        if (photosPermissionAvailable) {
          ContextCompat.checkSelfPermission(context, photosPermission) == PackageManager.PERMISSION_GRANTED
        } else {
          false
        },
      )
    }
  val photosPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      photosPermissionGranted = granted
    }

  var contactsPermissionGranted by
    remember {
      mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
          PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) ==
          PackageManager.PERMISSION_GRANTED,
      )
    }
  val contactsPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
      val readOk = perms[Manifest.permission.READ_CONTACTS] == true
      val writeOk = perms[Manifest.permission.WRITE_CONTACTS] == true
      contactsPermissionGranted = readOk && writeOk
    }

  var calendarPermissionGranted by
    remember {
      mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
          PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
          PackageManager.PERMISSION_GRANTED,
      )
    }
  val calendarPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
      val readOk = perms[Manifest.permission.READ_CALENDAR] == true
      val writeOk = perms[Manifest.permission.WRITE_CALENDAR] == true
      calendarPermissionGranted = readOk && writeOk
    }

  var callLogPermissionGranted by
    remember {
      mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
          PackageManager.PERMISSION_GRANTED,
      )
    }
  val callLogPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      callLogPermissionGranted = granted
    }

  var motionPermissionGranted by
    remember {
      mutableStateOf(
        !motionPermissionRequired ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
          PackageManager.PERMISSION_GRANTED,
      )
    }
  val motionPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      motionPermissionGranted = granted
    }

  var smsPermissionGranted by
    remember {
      mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
          PackageManager.PERMISSION_GRANTED ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
          PackageManager.PERMISSION_GRANTED,
      )
    }
  val smsPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      smsPermissionGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED
      viewModel.refreshGatewayConnection()
    }

  val assistantRoleLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      assistantRoleAvailable = isAssistantRoleAvailable(context)
      assistantRoleHeld = isAssistantRoleHeld(context)
    }

  DisposableEffect(lifecycleOwner, context) {
    val observer =
      LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          // Permission and role screens live outside Compose; refresh all derived
          // toggles whenever Android returns to this settings surface.
          micPermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
          notificationsPermissionGranted = hasNotificationsPermission(context)
          notificationListenerEnabled = isNotificationListenerEnabled(context)
          installedNotificationApps = queryInstalledApps(context, notificationForwardingPackages)
          photosPermissionGranted =
            if (photosPermissionAvailable) {
              ContextCompat.checkSelfPermission(context, photosPermission) == PackageManager.PERMISSION_GRANTED
            } else {
              false
            }
          contactsPermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
          calendarPermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
          callLogPermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
          motionPermissionGranted =
            !motionPermissionRequired ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
          smsPermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
          assistantRoleAvailable = isAssistantRoleAvailable(context)
          assistantRoleHeld = isAssistantRoleHeld(context)
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  fun setCameraEnabledChecked(checked: Boolean) {
    if (!checked) {
      viewModel.setCameraEnabled(false)
      return
    }

    val cameraOk =
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    if (cameraOk) {
      viewModel.setCameraEnabled(true)
    } else {
      permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }
  }

  fun requestLocationPermissions() {
    val fineOk =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarseOk =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (fineOk || coarseOk) {
      viewModel.setLocationMode(LocationMode.WhileUsing)
    } else {
      pendingLocationRequest = true
      locationPermissionLauncher.launch(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
      )
    }
  }

  fun setPreciseLocationChecked(checked: Boolean) {
    if (!checked) {
      viewModel.setLocationPreciseEnabled(false)
      return
    }
    val fineOk =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (fineOk) {
      viewModel.setLocationPreciseEnabled(true)
    } else {
      pendingPreciseToggle = true
      locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }
  }

  val normalizedAppSearch = notificationAppSearch.trim().lowercase()
  val filteredNotificationApps =
    remember(installedNotificationApps, normalizedAppSearch, notificationShowSystemApps) {
      installedNotificationApps
        .asSequence()
        .filter { app -> notificationShowSystemApps || !app.isSystemApp }
        .filter { app ->
          normalizedAppSearch.isEmpty() ||
            app.label.lowercase().contains(normalizedAppSearch) ||
            app.packageName.lowercase().contains(normalizedAppSearch)
        }.toList()
    }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(mobileBackgroundGradient),
  ) {
    LazyColumn(
      state = listState,
      modifier =
        Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .imePadding()
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // ── Node ──
      item {
        Text(
          stringResource(R.string.device),
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = mobileAccent,
        )
      }
      item {
        Column(modifier = Modifier.settingsRowModifier()) {
          OutlinedTextField(
            value = displayName,
            onValueChange = viewModel::setDisplayName,
            label = { Text(stringResource(R.string.name_label), style = mobileCaption1, color = mobileTextSecondary) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            textStyle = mobileBody.copy(color = mobileText),
            colors = settingsTextFieldColors(),
          )
          HorizontalDivider(color = mobileBorder)
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text("$deviceModel · $appVersion", style = mobileCallout, color = mobileTextSecondary)
            Text(
              instanceId.take(8) + "…",
              style = mobileCaption1.copy(fontFamily = FontFamily.Monospace),
              color = mobileTextTertiary,
            )
          }
          if (assistantRoleAvailable) {
            HorizontalDivider(color = mobileBorder)
            ListItem(
              modifier = Modifier.fillMaxWidth(),
              colors = listItemColors,
              headlineContent = { Text(stringResource(R.string.default_assistant), style = mobileHeadline) },
              supportingContent = {
                Text(
                  if (assistantRoleHeld) {
                    stringResource(R.string.openclaw_registered_as_device_assistant)
                  } else {
                    stringResource(R.string.launch_from_assistant_gesture)
                  },
                  style = mobileCallout,
                )
              },
              trailingContent = {
                Button(
                  onClick = {
                    assistantRoleLauncher.launch(
                      context
                        .getSystemService(RoleManager::class.java)
                        .createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                    )
                  },
                  colors = settingsPrimaryButtonColors(),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text(
                    if (assistantRoleHeld) stringResource(R.string.manage) else stringResource(R.string.enable),
                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                  )
                }
              },
            )
          }
        }
      }

      // ── Media ──
      item {
        Text(
          stringResource(R.string.media),
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = mobileAccent,
        )
      }
      item {
        Column(modifier = Modifier.settingsRowModifier()) {
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.microphone), style = mobileHeadline) },
            supportingContent = {
              Text(
                if (micPermissionGranted) stringResource(R.string.granted) else stringResource(R.string.required_for_voice_transcription),
                style = mobileCallout,
              )
            },
            trailingContent = {
              Button(
                onClick = {
                  if (micPermissionGranted) {
                    openAppSettings(context)
                  } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                },
                colors = settingsPrimaryButtonColors(),
                shape = RoundedCornerShape(14.dp),
              ) {
                Text(
                  if (micPermissionGranted) stringResource(R.string.manage) else stringResource(R.string.grant),
                  style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                )
              }
            },
          )
          HorizontalDivider(color = mobileBorder)
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.camera), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.camera_description), style = mobileCallout) },
            trailingContent = { Switch(checked = cameraEnabled, onCheckedChange = ::setCameraEnabledChecked) },
          )
        }
      }

      // ── Notifications & Messaging ──
      item {
        Text(
          stringResource(R.string.notifications),
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = mobileAccent,
        )
      }
      item {
        Column(modifier = Modifier.settingsRowModifier()) {
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.system_notifications), style = mobileHeadline) },
            supportingContent = {
              Text(stringResource(R.string.system_notifications_description), style = mobileCallout)
            },
            trailingContent = {
              Button(
                onClick = {
                  if (notificationsPermissionGranted || Build.VERSION.SDK_INT < 33) {
                    openAppSettings(context)
                  } else {
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                },
                colors = settingsPrimaryButtonColors(),
                shape = RoundedCornerShape(14.dp),
              ) {
                Text(
                  if (notificationsPermissionGranted) stringResource(R.string.manage) else stringResource(R.string.grant),
                  style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                )
              }
            },
          )
          HorizontalDivider(color = mobileBorder)
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.notification_listener_access), style = mobileHeadline) },
            supportingContent = {
              Text(
                stringResource(R.string.notification_listener_required_desc),
                style = mobileCallout,
              )
            },
            trailingContent = {
              Button(
                onClick = { openNotificationListenerSettings(context) },
                colors = settingsPrimaryButtonColors(),
                shape = RoundedCornerShape(14.dp),
              ) {
                Text(
                  if (notificationListenerEnabled) stringResource(R.string.manage) else stringResource(R.string.enable),
                  style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                )
              }
            },
          )
          if (smsPermissionAvailable) {
            HorizontalDivider(color = mobileBorder)
            ListItem(
              modifier = Modifier.fillMaxWidth(),
              colors = listItemColors,
              headlineContent = { Text(stringResource(R.string.sms), style = mobileHeadline) },
              supportingContent = {
                Text(stringResource(R.string.sms_description), style = mobileCallout)
              },
              trailingContent = {
                Button(
                  onClick = {
                    if (smsPermissionGranted) {
                      openAppSettings(context)
                    } else {
                      smsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS),
                      )
                    }
                  },
                  colors = settingsPrimaryButtonColors(),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text(
                    if (smsPermissionGranted) {
                      stringResource(R.string.manage)
                    } else {
                      stringResource(R.string.grant)
                    },
                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                  )
                }
              },
            )
          }
        }
      }
      item {
        ListItem(
          modifier = Modifier.settingsRowModifier(),
          colors = listItemColors,
          headlineContent = { Text(stringResource(R.string.forward_notification_events), style = mobileHeadline) },
          supportingContent = {
            Text(
              if (notificationListenerEnabled) {
                stringResource(R.string.forward_listener_events_hint)
              } else {
                stringResource(R.string.notification_listener_access_off)
              },
              style = mobileCallout,
            )
          },
          trailingContent = {
            Switch(
              checked = notificationForwardingEnabled,
              onCheckedChange = viewModel::setNotificationForwardingEnabled,
              enabled = notificationListenerEnabled,
            )
          },
        )
      }
      item {
        Text(
          if (notificationListenerEnabled) {
            stringResource(R.string.forwarding_available_when_enabled)
          } else {
            stringResource(R.string.forwarding_controls_disabled_until_listener_enabled)
          },
          style = mobileCallout,
          color = mobileTextSecondary,
        )
      }
      item {
        Column(
          modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
          verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.package_filter_allowlist), style = mobileHeadline) },
            supportingContent = {
              Text(stringResource(R.string.package_filter_allowlist_desc), style = mobileCallout)
            },
            trailingContent = {
              RadioButton(
                selected = notificationForwardingMode == NotificationPackageFilterMode.Allowlist,
                onClick = {
                  viewModel.setNotificationForwardingMode(NotificationPackageFilterMode.Allowlist)
                },
                enabled = notificationForwardingAvailable,
              )
            },
          )
          HorizontalDivider(color = mobileBorder)
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.package_filter_blocklist), style = mobileHeadline) },
            supportingContent = {
              Text(stringResource(R.string.package_filter_blocklist_desc), style = mobileCallout)
            },
            trailingContent = {
              RadioButton(
                selected = notificationForwardingMode == NotificationPackageFilterMode.Blocklist,
                onClick = {
                  viewModel.setNotificationForwardingMode(NotificationPackageFilterMode.Blocklist)
                },
                enabled = notificationForwardingAvailable,
              )
            },
          )
        }
      }
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(
            onClick = { notificationPickerExpanded = !notificationPickerExpanded },
            enabled = notificationForwardingAvailable,
            colors = settingsPrimaryButtonColors(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(
              if (notificationPickerExpanded) stringResource(R.string.close_app_picker) else stringResource(R.string.open_app_picker),
              style = mobileCallout.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
      item {
        Text(
          selectedPackagesSummary,
          style = mobileCallout,
          color = mobileTextSecondary,
        )
      }
      if (notificationPickerExpanded) {
        item {
          OutlinedTextField(
            value = notificationAppSearch,
            onValueChange = { notificationAppSearch = it },
            label = {
              Text(stringResource(R.string.open_app_picker), style = mobileCaption1, color = mobileTextSecondary)
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = mobileBody.copy(color = mobileText),
            colors = settingsTextFieldColors(),
            enabled = notificationForwardingAvailable,
          )
        }
        item {
          ListItem(
            modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.show_system_apps), style = mobileHeadline) },
            supportingContent = {
              Text(stringResource(R.string.show_system_apps_desc), style = mobileCallout)
            },
            trailingContent = {
              Switch(
                checked = notificationShowSystemApps,
                onCheckedChange = { notificationShowSystemApps = it },
                enabled = notificationForwardingAvailable,
              )
            },
          )
        }
        items(filteredNotificationApps, key = { it.packageName }) { app ->
          ListItem(
            modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
            colors = listItemColors,
            headlineContent = { Text(app.label, style = mobileHeadline) },
            supportingContent = { Text(app.packageName, style = mobileCallout) },
            trailingContent = {
              Switch(
                checked = notificationForwardingPackages.contains(app.packageName),
                onCheckedChange = { checked ->
                  val next = notificationForwardingPackages.toMutableSet()
                  if (checked) {
                    next.add(app.packageName)
                  } else {
                    next.remove(app.packageName)
                  }
                  viewModel.setNotificationForwardingPackagesCsv(next.sorted().joinToString(","))
                },
                enabled = notificationForwardingAvailable,
              )
            },
          )
        }
      }
      item {
        ListItem(
          modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
          colors = listItemColors,
          headlineContent = { Text(stringResource(R.string.quiet_hours), style = mobileHeadline) },
          supportingContent = {
            Text(stringResource(R.string.quiet_hours_desc), style = mobileCallout)
          },
          trailingContent = {
            Switch(
              checked = notificationForwardingQuietHoursEnabled,
              onCheckedChange = {
                if (!quietHoursCanEnable && it) return@Switch
                viewModel.setNotificationForwardingQuietHours(
                  enabled = it,
                  start = notificationQuietStartDraft,
                  end = notificationQuietEndDraft,
                )
              },
              enabled = if (notificationForwardingQuietHoursEnabled) notificationForwardingAvailable else quietHoursCanEnable,
            )
          },
        )
      }
      item {
        OutlinedTextField(
          value = notificationQuietStartDraft,
          onValueChange = { notificationQuietStartDraft = it },
          label = { Text(stringResource(R.string.quiet_start_label), style = mobileCaption1, color = mobileTextSecondary) },
          modifier = Modifier.fillMaxWidth(),
          textStyle = mobileBody.copy(color = mobileText),
          colors = settingsTextFieldColors(),
          enabled = notificationForwardingAvailable,
          isError = notificationForwardingAvailable && normalizedQuietStartDraft == null,
          supportingText = {
            if (notificationForwardingAvailable && normalizedQuietStartDraft == null) {
              Text(stringResource(R.string.quiet_start_error), style = mobileCaption1, color = mobileDanger)
            }
          },
        )
      }
      item {
        OutlinedTextField(
          value = notificationQuietEndDraft,
          onValueChange = { notificationQuietEndDraft = it },
          label = { Text(stringResource(R.string.quiet_end_label), style = mobileCaption1, color = mobileTextSecondary) },
          modifier = Modifier.fillMaxWidth(),
          textStyle = mobileBody.copy(color = mobileText),
          colors = settingsTextFieldColors(),
          enabled = notificationForwardingAvailable,
          isError = notificationForwardingAvailable && normalizedQuietEndDraft == null,
          supportingText = {
            if (notificationForwardingAvailable && normalizedQuietEndDraft == null) {
              Text(stringResource(R.string.quiet_end_error), style = mobileCaption1, color = mobileDanger)
            }
          },
        )
      }
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(
            onClick = {
              viewModel.setNotificationForwardingQuietHours(
                enabled = notificationForwardingQuietHoursEnabled,
                start = notificationQuietStartDraft,
                end = notificationQuietEndDraft,
              )
            },
            enabled = quietHoursSaveEnabled,
            colors = settingsPrimaryButtonColors(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(stringResource(R.string.save_quiet_hours), style = mobileCallout.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
      item {
        OutlinedTextField(
          value = notificationRateDraft,
          onValueChange = { notificationRateDraft = it.filter { c -> c.isDigit() } },
          label = { Text(stringResource(R.string.max_events_per_minute), style = mobileCaption1, color = mobileTextSecondary) },
          modifier = Modifier.fillMaxWidth(),
          textStyle = mobileBody.copy(color = mobileText),
          colors = settingsTextFieldColors(),
          enabled = notificationForwardingAvailable,
        )
      }
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(
            onClick = {
              val parsed = notificationRateDraft.toIntOrNull() ?: notificationForwardingMaxEventsPerMinute
              viewModel.setNotificationForwardingMaxEventsPerMinute(parsed)
            },
            enabled = notificationForwardingAvailable,
            colors = settingsPrimaryButtonColors(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(stringResource(R.string.save_rate), style = mobileCallout.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
      item {
        OutlinedTextField(
          value = notificationSessionKeyDraft,
          onValueChange = { notificationSessionKeyDraft = it },
          label = {
            Text(
              stringResource(R.string.route_session_key),
              style = mobileCaption1,
              color = mobileTextSecondary,
            )
          },
          placeholder = {
            Text(
              stringResource(R.string.route_session_key_hint),
              style = mobileCaption1,
              color = mobileTextSecondary,
            )
          },
          modifier = Modifier.fillMaxWidth(),
          textStyle = mobileBody.copy(color = mobileText),
          colors = settingsTextFieldColors(),
          enabled = notificationForwardingAvailable,
        )
      }
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(
            onClick = {
              viewModel.setNotificationForwardingSessionKey(notificationSessionKeyDraft.trim().ifEmpty { null })
            },
            enabled = notificationForwardingAvailable,
            colors = settingsPrimaryButtonColors(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(stringResource(R.string.save_session_route), style = mobileCallout.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
      item { HorizontalDivider(color = mobileBorder) }

      // ── Data Access ──
      item {
        Text(
          stringResource(R.string.data_access),
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = mobileAccent,
        )
      }
      item {
        Column(modifier = Modifier.settingsRowModifier()) {
          if (photosPermissionAvailable) {
            ListItem(
              modifier = Modifier.fillMaxWidth(),
              colors = listItemColors,
              headlineContent = { Text(stringResource(R.string.photos), style = mobileHeadline) },
              supportingContent = { Text(stringResource(R.string.photos_desc), style = mobileCallout) },
              trailingContent = {
                Button(
                  onClick = {
                    if (photosPermissionGranted) {
                      openAppSettings(context)
                    } else {
                      photosPermissionLauncher.launch(photosPermission)
                    }
                  },
                  colors = settingsPrimaryButtonColors(),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text(
                    if (photosPermissionGranted) stringResource(R.string.manage) else stringResource(R.string.grant),
                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                  )
                }
              },
            )
            HorizontalDivider(color = mobileBorder)
          }
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.contacts), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.contacts_desc), style = mobileCallout) },
            trailingContent = {
              Button(
                onClick = {
                  if (contactsPermissionGranted) {
                    openAppSettings(context)
                  } else {
                    contactsPermissionLauncher.launch(
                      arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                    )
                  }
                },
                colors = settingsPrimaryButtonColors(),
                shape = RoundedCornerShape(14.dp),
              ) {
                Text(
                  if (contactsPermissionGranted) stringResource(R.string.manage) else stringResource(R.string.grant),
                  style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                )
              }
            },
          )
          HorizontalDivider(color = mobileBorder)
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.calendar), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.calendar_desc), style = mobileCallout) },
            trailingContent = {
              Button(
                onClick = {
                  if (calendarPermissionGranted) {
                    openAppSettings(context)
                  } else {
                    calendarPermissionLauncher.launch(
                      arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                    )
                  }
                },
                colors = settingsPrimaryButtonColors(),
                shape = RoundedCornerShape(14.dp),
              ) {
                Text(
                  if (calendarPermissionGranted) stringResource(R.string.manage) else stringResource(R.string.grant),
                  style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                )
              }
            },
          )
          if (callLogPermissionAvailable) {
            HorizontalDivider(color = mobileBorder)
            ListItem(
              modifier = Modifier.fillMaxWidth(),
              colors = listItemColors,
              headlineContent = { Text(stringResource(R.string.call_log), style = mobileHeadline) },
              supportingContent = { Text(stringResource(R.string.call_log_desc), style = mobileCallout) },
              trailingContent = {
                Button(
                  onClick = {
                    if (callLogPermissionGranted) {
                      openAppSettings(context)
                    } else {
                      callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    }
                  },
                  colors = settingsPrimaryButtonColors(),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text(
                    if (callLogPermissionGranted) stringResource(R.string.manage) else stringResource(R.string.grant),
                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                  )
                }
              },
            )
          }
          if (motionAvailable) {
            HorizontalDivider(color = mobileBorder)
            ListItem(
              modifier = Modifier.fillMaxWidth(),
              colors = listItemColors,
              headlineContent = { Text(stringResource(R.string.motion), style = mobileHeadline) },
              supportingContent = { Text(stringResource(R.string.motion_desc), style = mobileCallout) },
              trailingContent = {
                val motionButtonLabel =
                  when {
                    !motionPermissionRequired -> stringResource(R.string.manage)
                    motionPermissionGranted -> stringResource(R.string.manage)
                    else -> stringResource(R.string.grant)
                  }
                Button(
                  onClick = {
                    if (!motionPermissionRequired || motionPermissionGranted) {
                      openAppSettings(context)
                    } else {
                      motionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                  },
                  colors = settingsPrimaryButtonColors(),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text(motionButtonLabel, style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                }
              },
            )
          }
        }
      }

      // ── Location ──
      item {
        Text(
          stringResource(R.string.location),
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = mobileAccent,
        )
      }
      item {
        Column(modifier = Modifier.settingsRowModifier()) {
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.location_off), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.location_off_desc), style = mobileCallout) },
            trailingContent = {
              RadioButton(
                selected = locationMode == LocationMode.Off,
                onClick = { viewModel.setLocationMode(LocationMode.Off) },
              )
            },
          )
          HorizontalDivider(color = mobileBorder)
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.location_while_using), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.location_while_using_desc), style = mobileCallout) },
            trailingContent = {
              RadioButton(
                selected = locationMode == LocationMode.WhileUsing,
                onClick = { requestLocationPermissions() },
              )
            },
          )
          HorizontalDivider(color = mobileBorder)
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.precise_location), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.precise_location_desc), style = mobileCallout) },
            trailingContent = {
              Switch(
                checked = locationPreciseEnabled,
                onCheckedChange = ::setPreciseLocationChecked,
                enabled = locationMode != LocationMode.Off,
              )
            },
          )
        }
      }

      // ── Preferences ──
      item {
        Text(
          stringResource(R.string.preferences),
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = mobileAccent,
        )
      }
      item {
        Column(modifier = Modifier.settingsRowModifier()) {
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.prevent_sleep), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.prevent_sleep_desc), style = mobileCallout) },
            trailingContent = { Switch(checked = preventSleep, onCheckedChange = viewModel::setPreventSleep) },
          )
          HorizontalDivider(color = mobileBorder)
          ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = listItemColors,
            headlineContent = { Text(stringResource(R.string.debug_canvas), style = mobileHeadline) },
            supportingContent = { Text(stringResource(R.string.debug_canvas_desc), style = mobileCallout) },
            trailingContent = {
              Switch(
                checked = canvasDebugStatusEnabled,
                onCheckedChange = viewModel::setCanvasDebugStatusEnabled,
              )
            },
          )
        }
      }

      // ── Version and update ──
      item {
        Text(
          stringResource(R.string.version_and_update),
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = mobileAccent,
        )
      }
      item {
        AppUpdateRow(
          appVersion = appVersion,
          updateState = appUpdateState,
          checkResult = appUpdateCheckResult,
          onCheck = { viewModel.checkForAppUpdate() },
          onDownload = { viewModel.downloadAppUpdate(context) },
          onInstall = { viewModel.installAppUpdate(context) },
          onOpenRelease = { viewModel.openAppUpdateReleasePage(context) },
          onDismiss = { viewModel.clearAppUpdateState() },
        )
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}

/** Settings row that shows current version and handles in-app update flow. */
@Composable
private fun AppUpdateRow(
  appVersion: String,
  updateState: AppUpdateManager.UpdateState,
  checkResult: AppUpdateManager.CheckResult?,
  onCheck: () -> Unit,
  onDownload: () -> Unit,
  onInstall: () -> Unit,
  onOpenRelease: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var showDialog by remember { mutableStateOf(false) }
  val listItemColors =
    ListItemDefaults.colors(
      containerColor = Color.Transparent,
      headlineColor = mobileText,
      supportingColor = mobileTextSecondary,
      trailingIconColor = mobileTextSecondary,
      leadingIconColor = mobileTextSecondary,
    )

  if (showDialog) {
    AppUpdateDialog(
      updateState = updateState,
      checkResult = checkResult,
      onDownload = onDownload,
      onInstall = onInstall,
      onOpenRelease = onOpenRelease,
      onDismiss = {
        showDialog = false
        onDismiss()
      },
    )
  }

  val updateStatusLabel =
    remember(updateState, checkResult) {
      when (updateState) {
        is AppUpdateManager.UpdateState.Checking -> context.getString(R.string.app_update_checking)
        is AppUpdateManager.UpdateState.Downloading -> context.getString(R.string.app_update_downloading)
        is AppUpdateManager.UpdateState.ReadyToInstall -> context.getString(R.string.app_update_ready_to_install)
        is AppUpdateManager.UpdateState.Error -> context.getString(R.string.app_update_error)
        is AppUpdateManager.UpdateState.Idle ->
          when (checkResult) {
            is AppUpdateManager.CheckResult.UpToDate -> context.getString(R.string.app_update_up_to_date)
            is AppUpdateManager.CheckResult.UpdateAvailable ->
              context.getString(R.string.app_update_available_version, checkResult.version)
            is AppUpdateManager.CheckResult.Error -> context.getString(R.string.app_update_check_failed)
            null -> context.getString(R.string.app_update_tap_to_check)
          }
      }
    }

  val isBusy =
    updateState is AppUpdateManager.UpdateState.Checking ||
      updateState is AppUpdateManager.UpdateState.Downloading

  Column(modifier = Modifier.settingsRowModifier()) {
    ListItem(
      modifier = Modifier.fillMaxWidth(),
      colors = listItemColors,
      headlineContent = { Text(stringResource(R.string.app_update_current_version), style = mobileHeadline) },
      supportingContent = { Text(updateStatusLabel, style = mobileCallout) },
      trailingContent = {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = mobileAccent,
            strokeWidth = 2.dp,
          )
        } else {
          Button(
            onClick = {
              if (checkResult is AppUpdateManager.CheckResult.UpdateAvailable ||
                checkResult is AppUpdateManager.CheckResult.Error
              ) {
                showDialog = true
              } else {
                onCheck()
                showDialog = true
              }
            },
            colors = settingsPrimaryButtonColors(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(
              when (checkResult) {
                is AppUpdateManager.CheckResult.UpdateAvailable -> context.getString(R.string.app_update_action)
                is AppUpdateManager.CheckResult.Error -> context.getString(R.string.app_update_retry)
                else -> context.getString(R.string.app_update_check)
              },
              style = mobileCallout.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      },
    )
    if (checkResult is AppUpdateManager.CheckResult.UpdateAvailable) {
      HorizontalDivider(color = mobileBorder)
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          context.getString(R.string.app_update_latest_version, checkResult.version),
          style = mobileCallout,
          color = mobileTextSecondary,
        )
        Text(
          appVersion,
          style = mobileCaption1.copy(fontFamily = FontFamily.Monospace),
          color = mobileTextTertiary,
        )
      }
    }
  }
}

/** Dialog that guides the user through download/install/update actions. */
@Composable
private fun AppUpdateDialog(
  updateState: AppUpdateManager.UpdateState,
  checkResult: AppUpdateManager.CheckResult?,
  onDownload: () -> Unit,
  onInstall: () -> Unit,
  onOpenRelease: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = mobileCardSurface,
    title = {
      Text(
        when (checkResult) {
          is AppUpdateManager.CheckResult.UpdateAvailable ->
            context.getString(R.string.app_update_available_title, checkResult.version)
          is AppUpdateManager.CheckResult.Error -> context.getString(R.string.app_update_check_failed_title)
          else -> context.getString(R.string.app_update_no_update_title)
        },
        style = mobileHeadline,
        color = mobileText,
      )
    },
    text = {
      Text(
        when (updateState) {
          is AppUpdateManager.UpdateState.Downloading ->
            context.getString(R.string.app_update_downloading_message)
          is AppUpdateManager.UpdateState.ReadyToInstall ->
            context.getString(R.string.app_update_ready_to_install_message)
          is AppUpdateManager.UpdateState.Error ->
            updateState.reason
          else ->
            when (checkResult) {
              is AppUpdateManager.CheckResult.UpdateAvailable ->
                context.getString(R.string.app_update_available_message, checkResult.version)
              is AppUpdateManager.CheckResult.Error ->
                checkResult.reason
              else -> context.getString(R.string.app_update_up_to_date_message)
            }
        },
        style = mobileCallout,
        color = mobileText,
      )
    },
    confirmButton = {
      when (updateState) {
        is AppUpdateManager.UpdateState.ReadyToInstall ->
          Button(
            onClick = { onInstall(); onDismiss() },
            colors = settingsPrimaryButtonColors(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(context.getString(R.string.app_update_install), style = mobileCallout.copy(fontWeight = FontWeight.Bold))
          }
        is AppUpdateManager.UpdateState.Downloading ->
          Button(
            onClick = {},
            enabled = false,
            colors = settingsPrimaryButtonColors(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(context.getString(R.string.app_update_downloading), style = mobileCallout.copy(fontWeight = FontWeight.Bold))
          }
        else ->
          if (checkResult is AppUpdateManager.CheckResult.UpdateAvailable) {
            Button(
              onClick = onDownload,
              colors = settingsPrimaryButtonColors(),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(context.getString(R.string.app_update_download), style = mobileCallout.copy(fontWeight = FontWeight.Bold))
            }
          } else {
            Button(
              onClick = onDismiss,
              colors = settingsPrimaryButtonColors(),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(context.getString(R.string.ok), style = mobileCallout.copy(fontWeight = FontWeight.Bold))
            }
          }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = mobileTextSecondary),
      ) {
        Text(context.getString(R.string.cancel))
      }
    },
  )
}

/** Shared Material text-field colors for the legacy mobile settings sheet. */
@Composable
private fun settingsTextFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedContainerColor = mobileSurface,
    unfocusedContainerColor = mobileSurface,
    focusedBorderColor = mobileAccent,
    unfocusedBorderColor = mobileBorder,
    focusedTextColor = mobileText,
    unfocusedTextColor = mobileText,
    cursorColor = mobileAccent,
  )

/** Applies the legacy mobile card border/background used by settings rows. */
@Composable
private fun Modifier.settingsRowModifier() =
  this
    .fillMaxWidth()
    .border(width = 1.dp, color = mobileBorder, shape = RoundedCornerShape(14.dp))
    .background(mobileCardSurface, RoundedCornerShape(14.dp))

/** Primary button colors for the legacy mobile settings sheet. */
@Composable
private fun settingsPrimaryButtonColors() =
  ButtonDefaults.buttonColors(
    containerColor = mobileAccent,
    contentColor = Color.White,
    disabledContainerColor = mobileAccent.copy(alpha = 0.45f),
    disabledContentColor = Color.White.copy(alpha = 0.9f),
  )

/** Opens this app's Android settings page for permissions that require system UI. */
private fun openAppSettings(context: Context) {
  val intent =
    Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      Uri.fromParts("package", context.packageName, null),
    )
  context.startActivity(intent)
}

/** Opens notification-listener settings, falling back to app settings if the intent is unavailable. */
private fun openNotificationListenerSettings(context: Context) {
  val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
  runCatching {
    context.startActivity(intent)
  }.getOrElse {
    openAppSettings(context)
  }
}

/** Android 13+ notification permission check; earlier versions grant posting at install time. */
private fun hasNotificationsPermission(context: Context): Boolean {
  if (Build.VERSION.SDK_INT < 33) return true
  return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED
}

/** Mirrors the notification listener service access check for UI enablement. */
private fun isNotificationListenerEnabled(context: Context): Boolean = DeviceNotificationListenerService.isAccessEnabled(context)

/** Checks whether the device exposes motion sensors needed by motion-related capabilities. */
private fun hasMotionCapabilities(context: Context): Boolean {
  val sensorManager = context.getSystemService(SensorManager::class.java) ?: return false
  return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ||
    sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
}

private fun isAssistantRoleAvailable(context: Context): Boolean = context.getSystemService(RoleManager::class.java).isRoleAvailable(RoleManager.ROLE_ASSISTANT)

private fun isAssistantRoleHeld(context: Context): Boolean = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
