package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, isHighPriority DESC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY isHighPriority DESC, createdAt DESC")
    suspend fun getPendingTasksSnapshot(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesSnapshot(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    suspend fun getAllMemoriesSnapshot(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)
}

@Dao
interface VaultInteractionDao {
    @Query("SELECT * FROM vault_interactions ORDER BY isPinned DESC, createdAt DESC")
    fun observeInteractions(): Flow<List<VaultInteractionEntity>>

    @Query("SELECT * FROM vault_interactions WHERE isPinned = 1 ORDER BY createdAt DESC")
    fun observePinnedInteractions(): Flow<List<VaultInteractionEntity>>

    @Query(
        "SELECT * FROM vault_interactions " +
            "WHERE userPrompt LIKE '%' || :query || '%' OR assistantReply LIKE '%' || :query || '%' " +
            "ORDER BY isPinned DESC, createdAt DESC"
    )
    fun searchInteractions(query: String): Flow<List<VaultInteractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: VaultInteractionEntity): Long

    @Query("UPDATE vault_interactions SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinState(id: Long, isPinned: Boolean)

    @Query("DELETE FROM vault_interactions WHERE id = :id")
    suspend fun deleteInteractionById(id: Long)

    @Query("DELETE FROM vault_interactions WHERE isPinned = 0")
    suspend fun clearUnpinnedInteractions()
}

@Dao
interface VoiceClipDao {
    @Query("SELECT * FROM voice_clips ORDER BY createdAt DESC")
    fun observeVoiceClips(): Flow<List<VoiceClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceClip(clip: VoiceClipEntity): Long

    @Query("DELETE FROM voice_clips WHERE id = :id")
    suspend fun deleteVoiceClipById(id: Long)
}
