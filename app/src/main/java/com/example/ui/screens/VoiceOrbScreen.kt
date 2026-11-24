package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.ChatMessageEntity
import com.example.data.MessageSender
import com.example.data.VoiceClipEntity
import com.example.engine.VoiceRecorderState
import com.example.engine.VoiceUiState
import com.example.ui.components.AnimatedVoiceOrb
import com.example.ui.theme.LocalLumaColors
import java.util.Locale

@Composable
fun VoiceOrbScreen(
    voiceState: VoiceUiState,
    recorderState: VoiceRecorderState,
    voiceClips: List<VoiceClipEntity>,
    playingFilePath: String?,
    autoSpeak: Boolean,
    latestAssistantMessage: ChatMessageEntity?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onClearRecorderError: () -> Unit,
    onToggleClipPlayback: (String) -> Unit,
    onDeleteVoiceClip: (VoiceClipEntity) -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onSendVoiceFallbackCommand: (String) -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lumaColors = LocalLumaColors.current
    var permissionDenied by remember { mutableStateOf(false) }
    // 0 = MediaRecorder Service capture, 1 = Live SpeechRecognizer command
    var voiceMode by rememberSaveable { mutableIntStateOf(0) }

    val startSelectedVoiceAction = {
        onClearRecorderError()
        if (voiceMode == 0) {
            onStartRecording()
        } else {
            onStartListening()
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (audioGranted) {
            permissionDenied = false
            startSelectedVoiceAction()
        } else {
            permissionDenied = true
        }
    }

    val requestMicAndRun = {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val needsNotificationPermission = voiceMode == 0 &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission && !needsNotificationPermission) {
            permissionDenied = false
            startSelectedVoiceAction()
        } else {
            val toRequest = buildList {
                if (!hasAudioPermission) add(Manifest.permission.RECORD_AUDIO)
                if (needsNotificationPermission) add(Manifest.permission.POST_NOTIFICATIONS)
            }.toTypedArray()
            permissionsLauncher.launch(toRequest)
        }
    }

    val isRecording = recorderState.isRecording
    val isListening = voiceState is VoiceUiState.Listening || isRecording
    val isSpeaking = voiceState is VoiceUiState.Speaking && !isRecording
    val orbRmsDb = when {
        isRecording -> (recorderState.amplitudeNormalized * 12f).coerceIn(0f, 12f)
        voiceState is VoiceUiState.Listening -> voiceState.rmsDb
        else -> 0f
    }

    val handleOrbTap = {
        when {
            isRecording -> onStopRecording()
            voiceState is VoiceUiState.Listening -> onStopListening()
            voiceState is VoiceUiState.Speaking -> onStopSpeaking()
            else -> requestMicAndRun()
        }
    }

    val statusTitle = when {
        isRecording -> "Capturing voice input… ${formatDurationClock(recorderState.elapsedMillis)}"
        recorderState.errorMessage != null -> recorderState.errorMessage
        voiceState is VoiceUiState.Listening -> "Luma is listening…"
        voiceState is VoiceUiState.Speaking -> "Luma is speaking…"
        voiceState is VoiceUiState.Info -> voiceState.message
        permissionDenied -> "Microphone permission denied. Grant access or tap a quick command below."
        voiceMode == 0 -> "Tap the orb to record voice input"
        else -> "Tap the orb to speak a command"
    }

    val quickVoiceChips = listOf(
        "What time is it?",
        "Switch to dark mode",
        "Switch to light mode",
        "Start 5 minute timer",
        "Give me a motivation quote",
        "Help me relax"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = voiceMode == 0,
                    onClick = {
                        if (!isRecording) voiceMode = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("voice_mode_capture")
                ) {
                    Text("Voice Capture", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SegmentedButton(
                    selected = voiceMode == 1,
                    onClick = {
                        if (!isRecording) voiceMode = 1
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("voice_mode_command")
                ) {
                    Text("Voice Command", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            AnimatedVoiceOrb(
                isListening = isListening,
                isSpeaking = isSpeaking,
                rmsDb = orbRmsDb,
                size = 216.dp,
                onClick = handleOrbTap,
                modifier = Modifier.testTag("voice_orb_button")
            )

            Text(
                text = statusTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            AnimatedVisibility(visible = isRecording) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = lumaColors.glassSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FiberManualRecord,
                                    contentDescription = "Recording active",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "MediaRecorder Service Active",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = formatDurationClock(recorderState.elapsedMillis),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { recorderState.amplitudeNormalized.coerceIn(0.05f, 1f) },
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            if (voiceState is VoiceUiState.Listening && voiceState.partialText.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = lumaColors.glassSurface,
                    border = BorderStroke(1.dp, lumaColors.glassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${voiceState.partialText}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (latestAssistantMessage != null && latestAssistantMessage.sender == MessageSender.LUMA.name) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = lumaColors.glassSurface,
                    border = BorderStroke(1.dp, lumaColors.glassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Luma's Reply",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = latestAssistantMessage.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Primary Voice Control Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = handleOrbTap,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isListening || isSpeaking) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("voice_primary_action_button")
                ) {
                    Icon(
                        imageVector = if (isListening || isSpeaking) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isRecording -> "Save Voice Input"
                            voiceState is VoiceUiState.Listening -> "Stop Listening"
                            isSpeaking -> "Stop Voice"
                            voiceMode == 0 -> "Record Input"
                            else -> "Speak Now"
                        }
                    )
                }

                if (isRecording) {
                    OutlinedButton(
                        onClick = onCancelRecording,
                        shape = CircleShape,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("voice_cancel_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Discard recording",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Discard")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onToggleAutoSpeak(!autoSpeak) },
                        shape = CircleShape,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("voice_mute_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (autoSpeak) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (autoSpeak) "Voice On" else "Muted")
                    }
                }
            }

            if (voiceClips.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Captured Voice Inputs (${voiceClips.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    voiceClips.take(3).forEach { clip ->
                        val isPlaying = playingFilePath == clip.filePath
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = lumaColors.glassSurface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isPlaying) MaterialTheme.colorScheme.secondary else lumaColors.glassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                IconButton(
                                    onClick = { onToggleClipPlayback(clip.filePath) },
                                    modifier = Modifier.testTag("voice_clip_play_${clip.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Stop playback" else "Play voice clip",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = clip.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Offline Audio • ${formatDurationClock(clip.durationMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteVoiceClip(clip) },
                                    modifier = Modifier.testTag("voice_clip_delete_${clip.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete voice clip",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tap-to-speak offline quick commands
            Text(
                text = "Quick Offline Voice Prompts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                quickVoiceChips.forEach { cmd ->
                    AssistChip(
                        onClick = { onSendVoiceFallbackCommand(cmd) },
                        label = { Text(cmd) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = lumaColors.glassSurface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, lumaColors.glassBorder)
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenChat,
                shape = CircleShape,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("voice_type_instead_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch to Text Chat")
            }
        }
    }
}

private fun formatDurationClock(durationMs: Long): String {
    val totalSecs = (durationMs / 1000L).coerceAtLeast(0L)
    val mins = totalSecs / 60L
    val secs = totalSecs % 60L
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
