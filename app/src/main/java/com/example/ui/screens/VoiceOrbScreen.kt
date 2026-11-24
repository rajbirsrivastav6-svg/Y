package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.ChatMessageEntity
import com.example.data.MessageSender
import com.example.engine.VoiceUiState
import com.example.ui.components.AnimatedVoiceOrb
import com.example.ui.theme.LocalLumaColors

@Composable
fun VoiceOrbScreen(
    voiceState: VoiceUiState,
    autoSpeak: Boolean,
    latestAssistantMessage: ChatMessageEntity?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onSendVoiceFallbackCommand: (String) -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lumaColors = LocalLumaColors.current
    var permissionDenied by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            onStartListening()
        } else {
            permissionDenied = true
        }
    }

    val handleOrbTap = {
        when (voiceState) {
            is VoiceUiState.Listening -> onStopListening()
            is VoiceUiState.Speaking -> onStopSpeaking()
            else -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    onStartListening()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    val isListening = voiceState is VoiceUiState.Listening
    val isSpeaking = voiceState is VoiceUiState.Speaking
    val rmsDb = (voiceState as? VoiceUiState.Listening)?.rmsDb ?: 0f

    val statusTitle = when (voiceState) {
        is VoiceUiState.Listening -> "Luma is listening…"
        is VoiceUiState.Speaking -> "Luma is speaking…"
        is VoiceUiState.Info -> voiceState.message
        VoiceUiState.Idle -> if (permissionDenied) {
            "Microphone permission denied. Grant access or tap a quick command below."
        } else {
            "Tap the orb to talk offline"
        }
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
            Spacer(modifier = Modifier.height(6.dp))

            AnimatedVoiceOrb(
                isListening = isListening,
                isSpeaking = isSpeaking,
                rmsDb = rmsDb,
                size = 228.dp,
                onClick = handleOrbTap,
                modifier = Modifier.testTag("voice_orb_button")
            )

            Text(
                text = statusTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            isListening -> "Stop Listening"
                            isSpeaking -> "Stop Voice"
                            else -> "Speak Now"
                        }
                    )
                }

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
