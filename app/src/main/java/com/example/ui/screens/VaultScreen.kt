package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.InteractionModality
import com.example.data.MemoryEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.VaultInteractionEntity
import com.example.data.VoiceClipEntity
import com.example.ui.theme.LocalLumaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    tasks: List<TaskEntity>,
    notes: List<NoteEntity>,
    memories: List<MemoryEntity>,
    interactions: List<VaultInteractionEntity>,
    voiceClips: List<VoiceClipEntity>,
    playingFilePath: String?,
    onAddTask: (String, Boolean) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onToggleInteractionPin: (VaultInteractionEntity) -> Unit,
    onDeleteInteraction: (Long) -> Unit,
    onToggleClipPlayback: (String) -> Unit,
    onDeleteVoiceClip: (VoiceClipEntity) -> Unit,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lumaColors = LocalLumaColors.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val tabs = listOf("History", "Voice", "Tasks", "Notes", "Facts")

    val filteredInteractions = remember(interactions, searchQuery) {
        if (searchQuery.isBlank()) interactions
        else interactions.filter {
            it.userPrompt.contains(searchQuery, ignoreCase = true) ||
                it.assistantReply.contains(searchQuery, ignoreCase = true) ||
                it.cardTitle.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredVoiceClips = remember(voiceClips, searchQuery) {
        if (searchQuery.isBlank()) voiceClips
        else voiceClips.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    val filteredTasks = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) tasks
        else tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredMemories = remember(memories, searchQuery) {
        if (searchQuery.isBlank()) memories
        else memories.filter {
            it.triggerKey.contains(searchQuery, ignoreCase = true) ||
                it.answer.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = lumaColors.glassSurface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedTab == index,
                            borderColor = lumaColors.glassBorder,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("vault_tab_${label.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search offline ${tabs[selectedTab].lowercase()}…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search vault"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = lumaColors.glassSurface,
                    unfocusedContainerColor = lumaColors.glassSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = lumaColors.glassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_input")
            )

            when (selectedTab) {
                0 -> {
                    if (filteredInteractions.isEmpty()) {
                        VaultEmptyState(
                            icon = Icons.Rounded.History,
                            title = "No saved interactions yet",
                            subtitle = "Every voice or text interaction with Luma is automatically stored here offline so you can search, pin, or replay it anytime.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredInteractions, key = { it.id }) { interaction ->
                                InteractionHistoryCard(
                                    interaction = interaction,
                                    onTogglePin = { onToggleInteractionPin(interaction) },
                                    onSpeak = { onSpeakText(interaction.assistantReply) },
                                    onDelete = { onDeleteInteraction(interaction.id) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    if (filteredVoiceClips.isEmpty()) {
                        VaultEmptyState(
                            icon = Icons.Rounded.GraphicEq,
                            title = "No voice recordings yet",
                            subtitle = "Use Voice Capture on the Voice Orb screen to record offline audio inputs with MediaRecorder.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredVoiceClips, key = { it.id }) { clip ->
                                val isPlaying = playingFilePath == clip.filePath
                                val secs = (clip.durationMs / 1000L).coerceAtLeast(1L)
                                val durationLabel = String.format(Locale.getDefault(), "%02d:%02d", secs / 60L, secs % 60L)
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
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onToggleClipPlayback(clip.filePath) },
                                            modifier = Modifier.testTag("vault_voice_play_${clip.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isPlaying) "Stop playback" else "Play voice recording",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = clip.title,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Recorded Audio • $durationLabel",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteVoiceClip(clip) },
                                            modifier = Modifier.testTag("vault_voice_delete_${clip.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete voice recording",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (filteredTasks.isEmpty()) {
                        VaultEmptyState(
                            icon = Icons.Rounded.TaskAlt,
                            title = "No offline tasks yet",
                            subtitle = "Tap + below or tell Luma \"Add task: Revise notes\" to save tasks locally.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredTasks, key = { it.id }) { task ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = lumaColors.glassSurface,
                                    border = BorderStroke(1.dp, lumaColors.glassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { onToggleTask(task) }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    textDecoration = if (task.isCompleted) {
                                                        TextDecoration.LineThrough
                                                    } else TextDecoration.None
                                                ),
                                                color = if (task.isCompleted) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (task.isHighPriority && !task.isCompleted) {
                                                Text(
                                                    text = "High Priority",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onDeleteTask(task.id) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete task",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    if (filteredNotes.isEmpty()) {
                        VaultEmptyState(
                            icon = Icons.Rounded.EditNote,
                            title = "No offline notes saved",
                            subtitle = "Tap + to jot down an offline note or say \"Note: Grocery list\" in Chat.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = lumaColors.glassSurface,
                                    border = BorderStroke(1.dp, lumaColors.glassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = note.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = note.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeleteNote(note.id) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete note",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (filteredMemories.isEmpty()) {
                        VaultEmptyState(
                            icon = Icons.Rounded.Psychology,
                            title = "Teach Luma offline facts",
                            subtitle = "Save personal facts like bike slot, locker code, or study definitions so Luma recalls them offline.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredMemories, key = { it.id }) { mem ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = lumaColors.glassSurface,
                                    border = BorderStroke(1.dp, lumaColors.glassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mem.triggerKey,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = mem.answer,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(onClick = { onDeleteMemory(mem.id) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete memory",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab >= 2) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("vault_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add item to vault"
                )
            }
        }
    }

    if (showAddDialog) {
        AddVaultItemDialog(
            tabIndex = (selectedTab - 2).coerceAtLeast(0),
            onDismiss = { showAddDialog = false },
            onSaveTask = { title, priority ->
                onAddTask(title, priority)
                showAddDialog = false
            },
            onSaveNote = { title, content ->
                onAddNote(title, content)
                showAddDialog = false
            },
            onSaveMemory = { key, answer ->
                onAddMemory(key, answer)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun InteractionHistoryCard(
    interaction: VaultInteractionEntity,
    onTogglePin: () -> Unit,
    onSpeak: () -> Unit,
    onDelete: () -> Unit
) {
    val lumaColors = LocalLumaColors.current
    val formattedTime = remember(interaction.createdAt) {
        SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(interaction.createdAt))
    }
    val isVoice = interaction.modality == InteractionModality.VOICE.name

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = lumaColors.glassSurface,
        border = BorderStroke(
            width = if (interaction.isPinned) 1.5.dp else 1.dp,
            color = if (interaction.isPinned) MaterialTheme.colorScheme.primary else lumaColors.glassBorder
        ),
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
                    if (isVoice) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Voice interaction",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (interaction.cardTitle.isNotBlank()) interaction.cardTitle else formattedTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (interaction.cardTitle.isNotBlank()) {
                        Text(
                            text = "• $formattedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (interaction.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (interaction.isPinned) "Unpin interaction" else "Pin interaction",
                            tint = if (interaction.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Speak reply aloud",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete interaction",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = "You: ${interaction.userPrompt}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = interaction.assistantReply,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VaultEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(54.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddVaultItemDialog(
    tabIndex: Int,
    onDismiss: () -> Unit,
    onSaveTask: (String, Boolean) -> Unit,
    onSaveNote: (String, String) -> Unit,
    onSaveMemory: (String, String) -> Unit
) {
    var fieldOne by remember { mutableStateOf("") }
    var fieldTwo by remember { mutableStateOf("") }
    var highPriority by remember { mutableStateOf(false) }

    val dialogTitle = when (tabIndex) {
        0 -> "New Offline Task"
        1 -> "New Offline Note"
        else -> "Teach Luma a Fact"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fieldOne,
                    onValueChange = { fieldOne = it },
                    label = {
                        Text(
                            when (tabIndex) {
                                0 -> "Task description"
                                1 -> "Note title"
                                else -> "Topic / Keyword (e.g., Bike number)"
                            }
                        )
                    },
                    singleLine = tabIndex != 0,
                    modifier = Modifier.fillMaxWidth()
                )
                if (tabIndex == 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark as High Priority", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = highPriority,
                            onCheckedChange = { highPriority = it }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = fieldTwo,
                        onValueChange = { fieldTwo = it },
                        label = {
                            Text(if (tabIndex == 1) "Note content" else "Fact to remember")
                        },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (tabIndex) {
                        0 -> if (fieldOne.isNotBlank()) onSaveTask(fieldOne, highPriority)
                        1 -> if (fieldOne.isNotBlank() || fieldTwo.isNotBlank()) onSaveNote(fieldOne, fieldTwo)
                        else -> if (fieldOne.isNotBlank() && fieldTwo.isNotBlank()) onSaveMemory(fieldOne, fieldTwo)
                    }
                }
            ) {
                Text("Save Offline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
