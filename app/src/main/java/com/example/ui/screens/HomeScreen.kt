package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChatMessageEntity
import com.example.data.ThemeMode
import com.example.ui.TimerState
import com.example.ui.components.ActiveTimerCard
import com.example.ui.theme.LocalLumaColors

@Composable
fun HomeScreen(
    themeMode: ThemeMode,
    timerState: TimerState,
    recentMessages: List<ChatMessageEntity>,
    pendingTaskCount: Int,
    noteCount: Int,
    onOpenVoice: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenVault: () -> Unit,
    onToggleTheme: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onStartQuickTimer: () -> Unit,
    onPauseResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lumaColors = LocalLumaColors.current
    var promptText by rememberSaveable { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Companion Hero Card with generated avatar + glowing "Tap to Talk" center button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color(0xFF130E2E),
                    border = BorderStroke(1.dp, lumaColors.glassBorder),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_luma_avatar_1795531184860),
                            contentDescription = "Luma offline companion illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Atmospheric top & bottom vignette so text is always crisp
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xB80E0A24),
                                            Color.Transparent,
                                            Color(0xE60B081B)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Hi, I'm Luma",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "100% Offline • Always Ready",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFA5F3FC)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0x66000000),
                            border = BorderStroke(1.dp, Color(0x4DFFFFFF)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "${themeMode.label} Theme",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Floating Tap to Talk Orb Button overlapping the bottom edge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 26.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.secondary),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenVoice)
                            .testTag("home_tap_to_talk_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            lumaColors.orbHalo.copy(alpha = 0.85f),
                                            Color(0xFF1F164D)
                                        )
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Tap to talk with Luma",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap to Talk",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            ActiveTimerCard(
                timerState = timerState,
                onPauseResume = onPauseResumeTimer,
                onCancel = onCancelTimer
            )

            // Quick Offline Tools Grid
            Text(
                text = "Offline Capabilities",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickCapabilityCard(
                    title = if (lumaColors.isDark) "Dark Mode On" else "Light Mode On",
                    subtitle = "Tap to switch theme",
                    icon = if (lumaColors.isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_theme_card")
                )
                QuickCapabilityCard(
                    title = "Math & Units",
                    subtitle = "Solve & convert offline",
                    icon = Icons.Rounded.Calculate,
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        onSendPrompt("25% of 480")
                        onOpenChat()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_math_card")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickCapabilityCard(
                    title = "Offline Vault",
                    subtitle = "$pendingTaskCount tasks • $noteCount notes",
                    icon = Icons.Rounded.EditNote,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onClick = onOpenVault,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_vault_card")
                )
                QuickCapabilityCard(
                    title = "Focus Timer",
                    subtitle = "Start 25 min session",
                    icon = Icons.Rounded.Timer,
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = onStartQuickTimer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_timer_card")
                )
            }

            // Recent conversation preview card if any messages exist
            if (recentMessages.isNotEmpty()) {
                val latest = recentMessages.last()
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = lumaColors.glassSurface,
                    border = BorderStroke(1.dp, lumaColors.glassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onOpenChat)
                        .testTag("home_recent_chat_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Recent chat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recent Offline Reply",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = latest.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick "Ask me anything offline..." input bar
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = {
                    Text(
                        text = "Ask anything offline…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = lumaColors.glassSurface,
                    unfocusedContainerColor = lumaColors.glassSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = lumaColors.glassBorder
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (promptText.isNotBlank()) {
                            onSendPrompt(promptText)
                            promptText = ""
                            onOpenChat()
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (promptText.isNotBlank()) {
                                onSendPrompt(promptText)
                                promptText = ""
                                onOpenChat()
                            } else {
                                onOpenChat()
                            }
                        },
                        modifier = Modifier.testTag("home_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send offline query",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_prompt_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickCapabilityCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lumaColors = LocalLumaColors.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = lumaColors.glassSurface,
        border = BorderStroke(1.dp, lumaColors.glassBorder),
        tonalElevation = 2.dp,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
