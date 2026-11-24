package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.TimerState
import com.example.ui.theme.LocalLumaColors
import com.example.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val lumaColors = LocalLumaColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lumaColors.cosmicTop,
                        MaterialTheme.colorScheme.background,
                        lumaColors.cosmicBottom
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topGlow = lumaColors.orbHalo.copy(alpha = if (lumaColors.isDark) 0.16f else 0.10f)
            val sideGlow = lumaColors.orbCyan.copy(alpha = if (lumaColors.isDark) 0.12f else 0.08f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(topGlow, Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.12f),
                    radius = size.width * 0.7f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.12f),
                radius = size.width * 0.7f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sideGlow, Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.65f),
                    radius = size.width * 0.65f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.65f),
                radius = size.width * 0.65f
            )
        }
        content()
    }
}

@Composable
fun AnimatedVoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    size: Dp = 210.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lumaColors = LocalLumaColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 900 else 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening || isSpeaking) 4500 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val energyBoost = when {
        isListening -> 1f + (rmsDb / 30f).coerceIn(0f, 0.25f)
        isSpeaking -> 1.08f
        else -> 1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.62f * breathe * energyBoost

            // Outer ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lumaColors.orbHalo.copy(alpha = if (isListening) 0.45f else 0.25f),
                        lumaColors.orbCyan.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                center = center,
                radius = baseRadius * 1.55f
            )

            // Rotating cosmic plasma ring
            val rad = Math.toRadians(rotation.toDouble())
            val offsetA = Offset(
                x = center.x + (cos(rad) * baseRadius * 0.22f).toFloat(),
                y = center.y + (sin(rad) * baseRadius * 0.22f).toFloat()
            )
            val offsetB = Offset(
                x = center.x - (cos(rad) * baseRadius * 0.22f).toFloat(),
                y = center.y - (sin(rad) * baseRadius * 0.22f).toFloat()
            )

            // Main sphere body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lumaColors.orbCyan.copy(alpha = 0.95f),
                        lumaColors.orbHalo.copy(alpha = 0.88f),
                        Color(0xFF1E1352)
                    ),
                    center = offsetA,
                    radius = baseRadius * 1.15f
                ),
                center = center,
                radius = baseRadius
            )

            // Inner nebula highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isListening || isSpeaking) 0.48f else 0.28f),
                        lumaColors.orbCyan.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = offsetB,
                    radius = baseRadius * 0.72f
                ),
                center = center,
                radius = baseRadius * 0.92f
            )

            // Crisp orbital arc
            drawArc(
                color = lumaColors.orbCyan.copy(alpha = 0.65f),
                startAngle = rotation,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 1.16f, center.y - baseRadius * 1.16f),
                size = androidx.compose.ui.geometry.Size(baseRadius * 2.32f, baseRadius * 2.32f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = lumaColors.orbHalo.copy(alpha = 0.55f),
                startAngle = rotation + 180f,
                sweepAngle = 95f,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 1.26f, center.y - baseRadius * 1.26f),
                size = androidx.compose.ui.geometry.Size(baseRadius * 2.52f, baseRadius * 2.52f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun OfflineBadgePill(modifier: Modifier = Modifier) {
    val lumaColors = LocalLumaColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = lumaColors.glassSurface,
        border = BorderStroke(1.dp, lumaColors.glassBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = "Offline mode active",
                tint = SuccessGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Offline Ready",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ActiveTimerCard(
    timerState: TimerState,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!timerState.isActive && !timerState.isFinished) return
    val lumaColors = LocalLumaColors.current
    val progress = if (timerState.totalSeconds > 0) {
        timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
    } else 0f

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = lumaColors.glassSurface,
        border = BorderStroke(1.dp, lumaColors.glassBorder),
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = "Timer",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (timerState.isFinished) "${timerState.label} Complete" else timerState.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatClock(timerState.remainingSeconds),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Row {
                    if (!timerState.isFinished) {
                        IconButton(
                            onClick = onPauseResume,
                            modifier = Modifier.testTag("timer_pause_resume_button")
                        ) {
                            Icon(
                                imageVector = if (timerState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (timerState.isPaused) "Resume timer" else "Pause timer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("timer_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
