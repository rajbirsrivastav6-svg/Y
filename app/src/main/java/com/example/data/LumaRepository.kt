package com.example.data

import kotlinx.coroutines.flow.Flow
import java.io.File

class LumaRepository(
    private val chatDao: ChatDao,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val memoryDao: MemoryDao,
    private val vaultInteractionDao: VaultInteractionDao,
    private val voiceClipDao: VoiceClipDao
) {
    val chatMessages: Flow<List<ChatMessageEntity>> = chatDao.observeMessages()
    val tasks: Flow<List<TaskEntity>> = taskDao.observeTasks()
    val notes: Flow<List<NoteEntity>> = noteDao.observeNotes()
    val memories: Flow<List<MemoryEntity>> = memoryDao.observeMemories()
    val vaultInteractions: Flow<List<VaultInteractionEntity>> = vaultInteractionDao.observeInteractions()
    val voiceClips: Flow<List<VoiceClipEntity>> = voiceClipDao.observeVoiceClips()

    suspend fun saveMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChat() {
        chatDao.clearAllMessages()
    }

    suspend fun saveVaultInteraction(
        userPrompt: String,
        assistantReply: String,
        cardType: ResponseCardType,
        cardTitle: String,
        modality: InteractionModality
    ): VaultInteractionEntity {
        val entity = VaultInteractionEntity(
            userPrompt = userPrompt.trim(),
            assistantReply = assistantReply.trim(),
            cardType = cardType.name,
            cardTitle = cardTitle,
            modality = modality.name
        )
        val id = vaultInteractionDao.insertInteraction(entity)
        return entity.copy(id = id)
    }

    suspend fun toggleInteractionPin(interaction: VaultInteractionEntity) {
        vaultInteractionDao.updatePinState(interaction.id, !interaction.isPinned)
    }

    suspend fun deleteVaultInteraction(id: Long) {
        vaultInteractionDao.deleteInteractionById(id)
    }

    suspend fun clearUnpinnedInteractions() {
        vaultInteractionDao.clearUnpinnedInteractions()
    }

    suspend fun addTask(title: String, isHighPriority: Boolean = false): TaskEntity {
        val entity = TaskEntity(title = title.trim(), isHighPriority = isHighPriority)
        val id = taskDao.insertTask(entity)
        return entity.copy(id = id)
    }

    suspend fun toggleTask(task: TaskEntity) {
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun getPendingTasks(): List<TaskEntity> = taskDao.getPendingTasksSnapshot()

    suspend fun addNote(title: String, content: String, tag: String = "Personal"): NoteEntity {
        val entity = NoteEntity(
            title = title.trim().ifBlank { "Quick Note" },
            content = content.trim(),
            tag = tag
        )
        val id = noteDao.insertNote(entity)
        return entity.copy(id = id)
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNoteById(id)
    }

    suspend fun getAllNotes(): List<NoteEntity> = noteDao.getAllNotesSnapshot()

    suspend fun addMemory(triggerKey: String, answer: String): MemoryEntity {
        val entity = MemoryEntity(triggerKey = triggerKey.trim(), answer = answer.trim())
        val id = memoryDao.insertMemory(entity)
        return entity.copy(id = id)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun getAllMemories(): List<MemoryEntity> = memoryDao.getAllMemoriesSnapshot()

    suspend fun saveVoiceClip(
        title: String,
        filePath: String,
        durationMs: Long,
        createdAt: Long = System.currentTimeMillis()
    ): VoiceClipEntity {
        val entity = VoiceClipEntity(
            title = title.trim(),
            filePath = filePath,
            durationMs = durationMs,
            createdAt = createdAt
        )
        val id = voiceClipDao.insertVoiceClip(entity)
        return entity.copy(id = id)
    }

    suspend fun deleteVoiceClip(clip: VoiceClipEntity) {
        voiceClipDao.deleteVoiceClipById(clip.id)
        runCatching { File(clip.filePath).delete() }
    }
}
