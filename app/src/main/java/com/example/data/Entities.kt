package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageSender {
    USER,
    LUMA
}

enum class ResponseCardType {
    NONE,
    MATH,
    CONVERSION,
    TASK_SAVED,
    NOTE_SAVED,
    MEMORY_SAVED,
    TIMER_STARTED,
    THEME_CHANGED,
    BREATHING_GUIDE
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val text: String,
    val cardType: String = ResponseCardType.NONE.name,
    val cardTitle: String = "",
    val cardSubtitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val isHighPriority: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tag: String = "Personal",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerKey: String,
    val answer: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InteractionModality {
    TEXT,
    VOICE
}

@Entity(tableName = "vault_interactions")
data class VaultInteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userPrompt: String,
    val assistantReply: String,
    val cardType: String = ResponseCardType.NONE.name,
    val cardTitle: String = "",
    val modality: String = InteractionModality.TEXT.name,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
