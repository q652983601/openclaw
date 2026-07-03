package ai.openclaw.app.ui

import ai.openclaw.app.MainViewModel
import ai.openclaw.app.R
import ai.openclaw.app.VoiceCaptureMode
import ai.openclaw.app.ui.design.ClawPanel
import ai.openclaw.app.ui.design.ClawPlainIconButton
import ai.openclaw.app.ui.design.ClawPrimaryButton
import ai.openclaw.app.ui.design.ClawSecondaryButton
import ai.openclaw.app.ui.design.ClawStatus
import ai.openclaw.app.ui.design.ClawStatusPill
import ai.openclaw.app.ui.design.ClawTheme
import ai.openclaw.app.voice.VoiceConversationEntry
import ai.openclaw.app.voice.VoiceConversationRole
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/** Voice home screen that routes between talk mode, dictation, and idle setup. */
@Composable
fun VoiceScreen(
  viewModel: MainViewModel,
  onOpenCommand: () -> Unit,
  onOpenGatewaySettings: () -> Unit,
  onOpenVoiceSettings: () -> Unit,
) {
  val context = LocalContext.current
  val gatewayStatus by viewModel.statusText.collectAsState()
  val voiceCaptureMode by viewModel.voiceCaptureMode.collectAsState()
  val micEnabled by viewModel.micEnabled.collectAsState()
  val micCooldown by viewModel.micCooldown.collectAsState()
  val speakerEnabled by viewModel.speakerEnabled.collectAsState()
  val micStatusText by viewModel.micStatusText.collectAsState()
  val micLiveTranscript by viewModel.micLiveTranscript.collectAsState()
  val micQueuedMessages by viewModel.micQueuedMessages.collectAsState()
  val micConversation by viewModel.micConversation.collectAsState()
  val micIsSending by viewModel.micIsSending.collectAsState()
  val talkModeEnabled by viewModel.talkModeEnabled.collectAsState()
  val talkModeListening by viewModel.talkModeListening.collectAsState()
  val talkModeSpeaking by viewModel.talkModeSpeaking.collectAsState()
  val talkModeStatusText by viewModel.talkModeStatusText.collectAsState()
  val talkModeConversation by viewModel.talkModeConversation.collectAsState()

  var pendingAction by remember { mutableStateOf<VoiceAction?>(null) }
  var hasMicPermission by remember { mutableStateOf(context.hasRecordAudioPermission()) }
  val requestMicPermission =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasMicPermission = granted
      if (granted) {
        when (pendingAction) {
          VoiceAction.Talk -> viewModel.setTalkModeEnabled(true)
          VoiceAction.Dictation -> viewModel.setMicEnabled(true)
          null -> Unit
        }
      }
      pendingAction = null
    }

  // Talk mode and dictation use different managers, so choose the transcript
  // from the mode the user is actually seeing.
  val activeConversation = if (voiceCaptureMode == VoiceCaptureMode.TalkMode) talkModeConversation else micConversation
  val voiceActive = micEnabled || micIsSending || talkModeEnabled
  val gatewayReady = gatewayStatus.isVoiceGatewayReady()
  val voiceAttentionStatus =
    voiceAttentionStatus(
      talkModeStatusText = talkModeStatusText,
      voiceCaptureMode = voiceCaptureMode,
      micEnabled = micEnabled,
      micIsSending = micIsSending,
      talkModeEnabled = talkModeEnabled,
      talkModeListening = talkModeListening,
      talkModeSpeaking = talkModeSpeaking,
    )
  val activeStatus =
    voiceStatusLabel(
      gatewayStatus = gatewayStatus,
      voiceCaptureMode = voiceCaptureMode,
      micStatusText = micStatusText,
      micQueuedMessages = micQueuedMessages.size,
      micIsSending = micIsSending,
      talkModeListening = talkModeListening,
      talkModeSpeaking = talkModeSpeaking,
      voiceAttentionStatus = voiceAttentionStatus,
    )

  if (talkModeEnabled) {
    TalkSessionScreen(
      entries = talkModeConversation,
      listening = talkModeListening,
      speaking = talkModeSpeaking,
      speakerEnabled = speakerEnabled,
      onToggleSpeaker = { viewModel.setSpeakerEnabled(!speakerEnabled) },
      onEndTalk = { viewModel.setTalkModeEnabled(false) },
      onOpenVoiceSettings = onOpenVoiceSettings,
    )
    return
  }

  if (voiceCaptureMode == VoiceCaptureMode.ManualMic || micEnabled || micIsSending) {
    // Manual mic mode owns the whole screen while a turn is being captured or
    // delivered, even after the user releases the mic.
    DictationScreen(
      liveTranscript = micLiveTranscript,
      conversation = micConversation,
      listening = micEnabled,
      sending = micIsSending,
      statusText = activeStatus,
      gatewayStatus = gatewayStatus,
      onCancel = { viewModel.cancelMicCapture() },
      onSend = { viewModel.setMicEnabled(false) },
      onOpenVoiceSettings = onOpenVoiceSettings,
    )
    return
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .imePadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    VoiceHeader(
      statusText = voiceAttentionStatus ?: if (voiceActive || !gatewayReady) activeStatus else stringResource(R.string.voice_command_center),
      speakerEnabled = speakerEnabled,
      onToggleSpeaker = { viewModel.setSpeakerEnabled(!speakerEnabled) },
      onOpenCommand = onOpenCommand,
    )

    VoiceHero(
      gatewayStatus = gatewayStatus,
      voiceCaptureMode = voiceCaptureMode,
      micEnabled = micEnabled,
      talkModeEnabled = talkModeEnabled,
      talkModeListening = talkModeListening,
      talkModeSpeaking = talkModeSpeaking,
      micLiveTranscript = micLiveTranscript,
      gatewayReady = gatewayReady,
      voiceAttentionStatus = voiceAttentionStatus,
      onStartTalk = {
        runVoiceAction(
          action = VoiceAction.Talk,
          hasMicPermission = hasMicPermission,
          requestPermission = {
            pendingAction = VoiceAction.Talk
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
          },
          run = { viewModel.setTalkModeEnabled(!talkModeEnabled) },
        )
      },
      onStartDictation = {
        if (micCooldown) return@VoiceHero
        runVoiceAction(
          action = VoiceAction.Dictation,
          hasMicPermission = hasMicPermission,
          requestPermission = {
            pendingAction = VoiceAction.Dictation
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
          },
          run = { viewModel.setMicEnabled(!micEnabled) },
        )
      },
      onConnectGateway = onOpenGatewaySettings,
    )

    if (!hasMicPermission) {
      VoicePermissionPanel(
        onRequestPermission = {
          pendingAction = VoiceAction.Talk
          requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        },
      )
    }

    VoiceTranscript(
      entries = activeConversation,
      showThinking = micIsSending && activeConversation.none { it.role == VoiceConversationRole.Assistant && it.isStreaming },
      modifier = Modifier.weight(1f),
    )
  }
}

/** Full-screen dictation capture and send state. */
@Composable
private fun DictationScreen(
  liveTranscript: String?,
  conversation: List<VoiceConversationEntry>,
  listening: Boolean,
  sending: Boolean,
  statusText: String,
  gatewayStatus: String,
  onCancel: () -> Unit,
  onSend: () -> Unit,
  onOpenVoiceSettings: () -> Unit,
) {
  val lastUserText = conversation.lastOrNull { it.role == VoiceConversationRole.User }?.text
  val draftText = liveTranscript?.takeIf { it.isNotBlank() } ?: lastUserText.orEmpty()
  val providerAttentionStatus = voiceRuntimeAttentionStatus(statusText)
  val displayStatusText = providerAttentionStatus ?: statusText
  val speechProviderReady = providerAttentionStatus == null && gatewayStatus.isVoiceGatewayReady()
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .imePadding()
        .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
      ClawPlainIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_voice), onClick = onCancel)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(R.string.dictation), style = ClawTheme.type.title.copy(fontSize = 16.sp, lineHeight = 20.sp), color = ClawTheme.colors.text)
        Text(text = stringResource(R.string.transcribe_then_send), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
      }
      ClawPlainIconButton(icon = Icons.Default.Settings, contentDescription = stringResource(R.string.dictation_settings), onClick = onOpenVoiceSettings)
    }

    Surface(
      modifier = Modifier.fillMaxWidth().aspectRatio(0.82f),
      shape = RoundedCornerShape(ClawTheme.radii.panel),
      color = ClawTheme.colors.canvas,
      border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = draftText.ifBlank { if (sending) stringResource(R.string.sending_to_chat) else stringResource(R.string.start_speaking) },
          style = ClawTheme.type.title.copy(fontSize = 15.sp, lineHeight = 19.sp),
          color = if (draftText.isBlank()) ClawTheme.colors.textSubtle else ClawTheme.colors.text,
          maxLines = 7,
          overflow = TextOverflow.Ellipsis,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
          DictationWaveform(active = listening || sending)
          Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (listening) ClawTheme.colors.success else ClawTheme.colors.textMuted)
            Text(text = displayStatusText, style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
          }
        }
      }
    }

    ClawPanel(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          modifier = Modifier.size(30.dp),
          shape = CircleShape,
          color = ClawTheme.colors.surfacePressed,
          border = BorderStroke(1.dp, ClawTheme.colors.border),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClawTheme.colors.text)
          }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(text = stringResource(R.string.speech_provider), style = ClawTheme.type.section, color = ClawTheme.colors.text)
          Text(
            text = providerAttentionStatus ?: gatewayStatus.voiceGatewayLabel(),
            style = ClawTheme.type.body,
            color = ClawTheme.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text =
              when {
                sending -> stringResource(R.string.status_sending)
                providerAttentionStatus != null -> stringResource(R.string.attention)
                speechProviderReady -> stringResource(R.string.status_ready_short)
                else -> stringResource(R.string.status_offline_short)
              },
            style = ClawTheme.type.caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
            color =
              when {
                sending -> ClawTheme.colors.warning
                providerAttentionStatus != null -> ClawTheme.colors.warning
                speechProviderReady -> ClawTheme.colors.success
                else -> ClawTheme.colors.textMuted
              },
          )
          Box(
            modifier =
              Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                  when {
                    sending -> ClawTheme.colors.warning
                    providerAttentionStatus != null -> ClawTheme.colors.warning
                    speechProviderReady -> ClawTheme.colors.success
                    else -> ClawTheme.colors.textSubtle
                  },
                ),
          )
        }
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClawTheme.colors.textMuted)
      Text(text = stringResource(R.string.dictation_tip), style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      ClawSecondaryButton(text = stringResource(R.string.cancel), icon = Icons.Default.Close, onClick = onCancel, modifier = Modifier.weight(0.95f))
      ClawPrimaryButton(text = if (sending) stringResource(R.string.status_sending) else stringResource(R.string.send_to_chat), icon = Icons.AutoMirrored.Filled.Send, onClick = onSend, enabled = !sending, modifier = Modifier.weight(1.25f))
    }
  }
}

@Composable
private fun DictationWaveform(active: Boolean) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    List(48) { index ->
      val height = if (active) 3 + ((index * 7) % 16) else 3 + (index % 3) * 2
      Box(
        modifier =
          Modifier
            .size(width = 2.dp, height = height.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) ClawTheme.colors.text else ClawTheme.colors.textSubtle),
      )
    }
  }
}

@Composable
private fun TalkSessionScreen(
  entries: List<VoiceConversationEntry>,
  listening: Boolean,
  speaking: Boolean,
  speakerEnabled: Boolean,
  onToggleSpeaker: () -> Unit,
  onEndTalk: () -> Unit,
  onOpenVoiceSettings: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .imePadding()
        .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      ClawPlainIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_voice), onClick = onEndTalk)
      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = stringResource(R.string.realtime_talk), style = ClawTheme.type.title.copy(fontSize = 16.sp, lineHeight = 20.sp), color = ClawTheme.colors.text)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          Box(modifier = Modifier.size(4.5.dp).clip(CircleShape).background(if (speaking || listening) ClawTheme.colors.success else ClawTheme.colors.textSubtle))
          Text(
            text =
              if (speaking) {
                stringResource(R.string.openclaw_speaking)
              } else if (listening) {
                stringResource(R.string.realtime_voice)
              } else {
                stringResource(R.string.status_connected)
              },
            style = ClawTheme.type.body,
            color = ClawTheme.colors.textMuted,
          )
        }
      }
      ClawPlainIconButton(icon = Icons.Default.Info, contentDescription = stringResource(R.string.talk_settings), onClick = onOpenVoiceSettings)
    }

    Surface(
      modifier = Modifier.fillMaxWidth().height(52.dp),
      shape = RoundedCornerShape(ClawTheme.radii.panel),
      color = ClawTheme.colors.canvas,
      border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
    ) {
      Box(contentAlignment = Alignment.Center) {
        TalkWaveform(active = listening || speaking)
      }
    }

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = stringResource(R.string.live_transcript), style = ClawTheme.type.caption, color = ClawTheme.colors.textMuted)
      TalkTranscript(entries = entries, modifier = Modifier.weight(1f))
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TalkControl(icon = if (speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, label = if (speakerEnabled) stringResource(R.string.mute) else stringResource(R.string.unmute), onClick = onToggleSpeaker)
      TalkControl(icon = Icons.Default.PhoneDisabled, label = stringResource(R.string.end), primary = true, onClick = onEndTalk)
      TalkControl(icon = Icons.Default.GraphicEq, label = stringResource(R.string.voice_title), onClick = onOpenVoiceSettings)
    }
  }
}

@Composable
private fun TalkTranscript(
  entries: List<VoiceConversationEntry>,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (entries.isEmpty()) {
      item {
        TalkTranscriptCard(label = stringResource(R.string.openclaw_brand), text = stringResource(R.string.listening_for_next_turn), muted = true)
      }
    } else {
      items(entries.takeLast(6), key = { it.id }) { entry ->
        TalkTranscriptCard(
          label = if (entry.role == VoiceConversationRole.User) stringResource(R.string.you) else stringResource(R.string.openclaw_brand),
          text = if (entry.isStreaming && entry.text.isBlank()) stringResource(R.string.listening_response) else entry.text,
          muted = entry.isStreaming,
        )
      }
    }
  }
}

@Composable
private fun TalkTranscriptCard(
  label: String,
  text: String,
  muted: Boolean = false,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(ClawTheme.radii.panel),
    color = ClawTheme.colors.surface,
    border = BorderStroke(1.dp, ClawTheme.colors.border),
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Text(text = label, style = ClawTheme.type.section, color = ClawTheme.colors.text)
      Text(text = text, style = ClawTheme.type.body, color = if (muted) ClawTheme.colors.textMuted else ClawTheme.colors.text)
    }
  }
}

@Composable
private fun TalkControl(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  primary: Boolean = false,
  onClick: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
    Surface(
      onClick = onClick,
      modifier = Modifier.size(ClawTheme.spacing.touchTarget),
      shape = RoundedCornerShape(ClawTheme.radii.button),
      color = if (primary) ClawTheme.colors.primary else ClawTheme.colors.canvas,
      contentColor = if (primary) ClawTheme.colors.primaryText else ClawTheme.colors.text,
      border = BorderStroke(1.dp, if (primary) ClawTheme.colors.primary else ClawTheme.colors.border),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(if (primary) 20.dp else 18.dp))
      }
    }
    Text(text = label, style = ClawTheme.type.caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp), color = ClawTheme.colors.textMuted)
  }
}

@Composable
private fun TalkWaveform(active: Boolean) {
  Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
    listOf(4, 12, 24, 34, 46, 28, 12, 38, 44, 24, 12, 30, 42, 18, 6).forEachIndexed { index, height ->
      Box(
        modifier =
          Modifier
            .size(width = 3.dp, height = (if (active) height else 6 + index % 4 * 5).dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) ClawTheme.colors.text else ClawTheme.colors.textSubtle),
      )
    }
  }
}

@Composable
private fun VoiceHeader(
  statusText: String,
  speakerEnabled: Boolean,
  onToggleSpeaker: () -> Unit,
  onOpenCommand: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        painter = painterResource(id = R.drawable.openclaw_logo),
        contentDescription = null,
        modifier = Modifier.size(25.dp),
        tint = ClawTheme.colors.text,
      )
      Text(
        text = stringResource(R.string.openclaw_brand),
        style = ClawTheme.type.title.copy(fontSize = 17.sp, lineHeight = 21.sp),
        color = ClawTheme.colors.text,
        modifier = Modifier.weight(1f),
      )
      ClawPlainIconButton(icon = Icons.Default.Search, contentDescription = stringResource(R.string.search_voice), onClick = onOpenCommand)
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = stringResource(R.string.voice_title), style = ClawTheme.type.display.copy(fontSize = 24.sp, lineHeight = 28.sp), color = ClawTheme.colors.text)
        Text(
          text = statusText,
          style = ClawTheme.type.body,
          color = ClawTheme.colors.textMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      ClawPlainIconButton(
        icon = if (speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
        contentDescription = if (speakerEnabled) stringResource(R.string.mute_speaker) else stringResource(R.string.unmute_speaker),
        onClick = onToggleSpeaker,
      )
    }
  }
}

@Composable
private fun VoiceHero(
  gatewayStatus: String,
  voiceCaptureMode: VoiceCaptureMode,
  micEnabled: Boolean,
  talkModeEnabled: Boolean,
  talkModeListening: Boolean,
  talkModeSpeaking: Boolean,
  micLiveTranscript: String?,
  gatewayReady: Boolean,
  voiceAttentionStatus: String?,
  onStartTalk: () -> Unit,
  onStartDictation: () -> Unit,
  onConnectGateway: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
    VoiceOrb(
      active = micEnabled || talkModeEnabled,
      listening = talkModeListening || voiceCaptureMode == VoiceCaptureMode.ManualMic,
      speaking = talkModeSpeaking,
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier =
          Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(if (micEnabled || talkModeEnabled) ClawTheme.colors.success else ClawTheme.colors.textSubtle),
      )
      Text(
        text =
          when {
            voiceAttentionStatus != null -> voiceAttentionStatus
            talkModeSpeaking -> stringResource(R.string.openclaw_is_replying)
            talkModeListening -> stringResource(R.string.status_listening)
            talkModeEnabled -> stringResource(R.string.talk_is_live)
            micEnabled -> stringResource(R.string.dictation_is_listening)
            !gatewayReady -> stringResource(R.string.gateway_offline)
            else -> stringResource(R.string.ready_to_talk)
          },
        style = ClawTheme.type.body,
        color = ClawTheme.colors.textMuted,
        textAlign = TextAlign.Center,
      )
    }

    if (!micLiveTranscript.isNullOrBlank()) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ClawTheme.radii.panel),
        color = ClawTheme.colors.surface,
        border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
      ) {
        Text(
          text = micLiveTranscript.trim(),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
          style = ClawTheme.type.body,
          color = ClawTheme.colors.text,
        )
      }
    }

    ClawPanel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
      VoiceModeRow(
        title = if (talkModeEnabled) stringResource(R.string.end_talk) else stringResource(R.string.realtime_talk),
        subtitle =
          when {
            talkModeEnabled -> stringResource(R.string.conversation_is_live)
            gatewayReady -> stringResource(R.string.natural_conversation_realtime)
            else -> stringResource(R.string.connect_gateway_to_start)
          },
        icon = if (talkModeEnabled) Icons.Default.PhoneDisabled else Icons.Default.RecordVoiceOver,
        onClick = onStartTalk,
        enabled = gatewayReady || talkModeEnabled,
      )
      VoiceModeRow(
        title = if (micEnabled) stringResource(R.string.stop_dictation) else stringResource(R.string.dictation),
        subtitle =
          when {
            micEnabled -> stringResource(R.string.listening_for_one_turn)
            gatewayReady -> stringResource(R.string.convert_speech_to_text)
            else -> stringResource(R.string.connect_gateway_to_start)
          },
        icon = if (micEnabled) Icons.Default.MicOff else Icons.Default.TextFields,
        onClick = onStartDictation,
        enabled = gatewayReady || micEnabled,
      )
    }

    VoiceProviderCard(gatewayStatus = gatewayStatus, voiceAttentionStatus = voiceAttentionStatus)

    VoicePrimaryAction(
      text =
        when {
          talkModeEnabled -> stringResource(R.string.end_talk)
          gatewayReady -> stringResource(R.string.start_talk)
          else -> stringResource(R.string.connect_gateway)
        },
      icon =
        when {
          talkModeEnabled -> Icons.Default.PhoneDisabled
          gatewayReady -> Icons.Default.Phone
          else -> Icons.Default.Cloud
        },
      onClick = if (gatewayReady || talkModeEnabled) onStartTalk else onConnectGateway,
    )
  }
}

@Composable
private fun VoiceModeRow(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  enabled: Boolean = true,
) {
  Surface(onClick = onClick, enabled = enabled, color = Color.Transparent, contentColor = ClawTheme.colors.text) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 0.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        modifier = Modifier.size(30.dp),
        shape = RoundedCornerShape(ClawTheme.radii.control),
        color = if (enabled) ClawTheme.colors.surface else ClawTheme.colors.canvas,
        contentColor = if (enabled) ClawTheme.colors.text else ClawTheme.colors.textSubtle,
        border = BorderStroke(1.dp, ClawTheme.colors.border),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = ClawTheme.type.body, color = if (enabled) ClawTheme.colors.text else ClawTheme.colors.textMuted, maxLines = 1)
        Text(text = subtitle, style = ClawTheme.type.caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp), color = ClawTheme.colors.textMuted, maxLines = 1)
      }
      if (enabled) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = ClawTheme.colors.textMuted,
        )
      }
    }
  }
}

@Composable
private fun VoiceProviderCard(
  gatewayStatus: String,
  voiceAttentionStatus: String?,
) {
  val ready = voiceAttentionStatus == null && gatewayStatus.isVoiceGatewayReady()
  Surface(
    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
    shape = RoundedCornerShape(ClawTheme.radii.panel),
    color = ClawTheme.colors.surface,
    contentColor = ClawTheme.colors.text,
    border = BorderStroke(1.dp, ClawTheme.colors.border),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        modifier = Modifier.size(30.dp),
        shape = RoundedCornerShape(ClawTheme.radii.control),
        color = ClawTheme.colors.canvas,
        contentColor = ClawTheme.colors.text,
        border = BorderStroke(1.dp, ClawTheme.colors.borderStrong),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(15.dp))
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(R.string.provider), style = ClawTheme.type.body, color = ClawTheme.colors.text, maxLines = 1)
        Text(
          text = voiceAttentionStatus ?: gatewayStatus.voiceGatewayLabel(),
          style = ClawTheme.type.caption,
          color = ClawTheme.colors.textMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
          modifier =
            Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(
                when {
                  ready -> ClawTheme.colors.success
                  voiceAttentionStatus != null -> ClawTheme.colors.warning
                  else -> ClawTheme.colors.textSubtle
                },
              ),
        )
        Text(
          text =
            when {
              ready -> stringResource(R.string.status_ready_short)
              voiceAttentionStatus != null -> stringResource(R.string.attention)
              else -> stringResource(R.string.status_offline_short)
            },
          style = ClawTheme.type.caption,
          color = ClawTheme.colors.textMuted,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun VoicePrimaryAction(
  text: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(ClawTheme.spacing.touchTarget),
    shape = RoundedCornerShape(ClawTheme.radii.button),
    color = ClawTheme.colors.primary,
    contentColor = ClawTheme.colors.primaryText,
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(17.dp))
      Text(text = text, modifier = Modifier.padding(start = 8.dp), style = ClawTheme.type.label)
    }
  }
}

@Composable
private fun VoiceOrb(
  active: Boolean,
  listening: Boolean,
  speaking: Boolean,
) {
  Surface(
    modifier = Modifier.size(112.dp),
    shape = CircleShape,
    color = if (active || listening || speaking) Color(0xFF1976D2) else Color(0xFF123B63),
    contentColor = Color.White,
    tonalElevation = 3.dp,
    shadowElevation = 7.dp,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
          imageVector =
            when {
              speaking -> Icons.Default.RecordVoiceOver
              listening -> Icons.Default.GraphicEq
              else -> Icons.Default.Mic
            },
          contentDescription = null,
          modifier = Modifier.size(32.dp),
          tint = Color.White,
        )
        Waveform(active = active)
      }
    }
  }
}

@Composable
private fun Waveform(active: Boolean) {
  Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
    listOf(6, 11, 17, 23, 14, 9, 20, 14, 7).forEachIndexed { index, height ->
      Box(
        modifier =
          Modifier
            .size(width = 2.dp, height = (if (active) height else 6 + index % 3 * 3).dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Color.White else Color.White.copy(alpha = 0.52f)),
      )
    }
  }
}

@Composable
private fun VoiceTranscript(
  entries: List<VoiceConversationEntry>,
  showThinking: Boolean,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  LaunchedEffect(entries.size, showThinking) {
    if (entries.isNotEmpty() || showThinking) {
      listState.animateScrollToItem(0)
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxWidth(),
    state = listState,
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 8.dp),
  ) {
    if (showThinking) {
      item(key = "thinking") {
        VoiceThinkingCard()
      }
    }

    items(entries.asReversed(), key = { it.id }) { entry ->
      VoiceTurnCard(entry = entry)
    }

    if (entries.isEmpty() && !showThinking) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = stringResource(R.string.live_transcript), style = ClawTheme.type.caption, color = ClawTheme.colors.textSubtle)
          ClawPanel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = stringResource(R.string.no_transcript_yet), style = ClawTheme.type.section, color = ClawTheme.colors.text)
              Text(
                text = stringResource(R.string.words_and_replies_appear),
                style = ClawTheme.type.body,
                color = ClawTheme.colors.textMuted,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VoiceTurnCard(entry: VoiceConversationEntry) {
  val isUser = entry.role == VoiceConversationRole.User
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
    Surface(
      modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.92f),
      shape = RoundedCornerShape(ClawTheme.radii.panel),
      color = if (isUser) ClawTheme.colors.surfacePressed else ClawTheme.colors.surfaceRaised,
      contentColor = ClawTheme.colors.text,
      border = BorderStroke(1.dp, if (entry.isStreaming) ClawTheme.colors.borderStrong else ClawTheme.colors.border),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
          text = if (isUser) stringResource(R.string.you) else stringResource(R.string.openclaw_brand),
          style = ClawTheme.type.caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
          color = ClawTheme.colors.textSubtle,
        )
        Text(
          text = if (entry.isStreaming && entry.text.isBlank()) stringResource(R.string.listening_dots) else entry.text,
          style = ClawTheme.type.body,
          color = ClawTheme.colors.text,
        )
      }
    }
  }
}

@Composable
private fun VoiceThinkingCard() {
  ClawPanel {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      ClawStatusPill(text = stringResource(R.string.status_sending), status = ClawStatus.Warning)
      Text(text = stringResource(R.string.openclaw_preparing_response), style = ClawTheme.type.body, color = ClawTheme.colors.textMuted)
    }
  }
}

@Composable
private fun VoicePermissionPanel(onRequestPermission: () -> Unit) {
  ClawPanel {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ClawStatusPill(text = stringResource(R.string.permission_needed), status = ClawStatus.Warning)
      Text(text = stringResource(R.string.microphone_access_needed), style = ClawTheme.type.section, color = ClawTheme.colors.text)
      Text(
        text = stringResource(R.string.openclaw_listens_only_when_started),
        style = ClawTheme.type.body,
        color = ClawTheme.colors.textMuted,
      )
      ClawSecondaryButton(text = stringResource(R.string.enable_microphone), icon = Icons.Default.Mic, onClick = onRequestPermission)
    }
  }
}

private enum class VoiceAction {
  Talk,
  Dictation,
}

private fun runVoiceAction(
  action: VoiceAction,
  hasMicPermission: Boolean,
  requestPermission: () -> Unit,
  run: () -> Unit,
) {
  if (hasMicPermission) {
    run()
  } else {
    requestPermission()
  }
}

internal fun voiceStatusLabel(
  gatewayStatus: String,
  voiceCaptureMode: VoiceCaptureMode,
  micStatusText: String,
  micQueuedMessages: Int,
  micIsSending: Boolean,
  talkModeListening: Boolean,
  talkModeSpeaking: Boolean,
  voiceAttentionStatus: String?,
): String =
  when {
    voiceAttentionStatus != null -> voiceAttentionStatus
    voiceCaptureMode == VoiceCaptureMode.TalkMode && talkModeSpeaking -> "OpenClaw is speaking"
    voiceCaptureMode == VoiceCaptureMode.TalkMode && talkModeListening -> "Listening"
    voiceCaptureMode == VoiceCaptureMode.TalkMode -> "Talk is live"
    micIsSending -> "Sending dictation..."
    voiceCaptureMode == VoiceCaptureMode.ManualMic -> micStatusText.ifBlank { "Listening" }
    micQueuedMessages > 0 -> "$micQueuedMessages queued"
    !gatewayStatus.isVoiceGatewayReady() -> "Gateway offline"
    else -> "Ready to talk"
  }

internal fun voiceAttentionStatus(
  talkModeStatusText: String,
  voiceCaptureMode: VoiceCaptureMode,
  micEnabled: Boolean,
  micIsSending: Boolean,
  talkModeEnabled: Boolean,
  talkModeListening: Boolean,
  talkModeSpeaking: Boolean,
): String? {
  if (voiceCaptureMode != VoiceCaptureMode.Off || micEnabled || micIsSending) return null
  if (talkModeEnabled || talkModeListening || talkModeSpeaking) return null
  val status = talkModeStatusText.trim()
  if (status.isBlank()) return null
  val lower = status.lowercase()
  if (lower == "off" || lower == "ready" || lower == "listening" || lower == "connecting…") return null
  return status
    .takeIf {
      lower.contains("failed") ||
        lower.contains("unavailable") ||
        lower.contains("permission required") ||
        lower.contains("not connected") ||
        lower.contains("error")
    }?.let { userFacingVoiceAttentionStatus(it) }
}

internal fun voiceRuntimeAttentionStatus(statusText: String): String? {
  val status = statusText.trim()
  if (status.isBlank()) return null
  val lower = status.lowercase()
  return status
    .takeIf {
      lower.contains("transcription unavailable") ||
        lower.contains("provider unavailable") ||
        (lower.contains("provider") && lower.contains("not configured")) ||
        lower.contains("no realtime transcription provider") ||
        lower.contains("failed")
    }?.let { userFacingVoiceAttentionStatus(it) }
}

private fun userFacingVoiceAttentionStatus(status: String): String {
  val normalized =
    status
      .removePrefix("Start failed:")
      .trim()
      .removePrefix("Transcription unavailable:")
      .trim()
      .removePrefix("UNAVAILABLE:")
      .trim()
      .removePrefix("Error:")
      .trim()
  val lower = normalized.lowercase()
  if (lower.contains("realtime voice provider") && lower.contains("not configured")) {
    return "Realtime voice provider is not configured."
  }
  if (lower.contains("no realtime transcription provider")) {
    return "Realtime transcription provider is not configured."
  }
  if (lower.contains("microphone permission required")) {
    return "Microphone permission required"
  }
  return if (normalized.length <= 90) normalized else "${normalized.take(87)}..."
}

private fun String.isVoiceGatewayReady(): Boolean {
  val status = lowercase()
  return !status.contains("offline") && !status.contains("not connected") && !status.contains("failed") && !status.contains("error")
}

@Composable
private fun String.voiceGatewayLabel(): String =
  if (isVoiceGatewayReady()) stringResource(R.string.connected_and_ready) else stringResource(R.string.gateway_not_connected)

private fun Context.hasRecordAudioPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
