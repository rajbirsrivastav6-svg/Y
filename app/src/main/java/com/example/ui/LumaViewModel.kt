package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessageEntity
import com.example.data.InteractionModality
import com.example.data.LumaDatabase
import com.example.data.LumaRepository
import com.example.data.MemoryEntity
import com.example.data.MessageSender
import com.example.data.NoteEntity
import com.example.data.ResponseCardType
import com.example.data.SettingsRepository
import com.example.data.TaskEntity
import com.example.data.ThemeMode
import com.example.data.UserPreferences
import com.example.data.VaultInteractionEntity
import com.example.engine.OfflineAssistantEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val label: String = "Offline Timer",
    val isFinished: Boolean = false
)

class LumaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LumaDatabase.getInstance(application)
    private val repository = LumaRepository(
        chatDao = database.chatDao(),
        taskDao = database.taskDao(),
        noteDao = database.noteDao(),
        memoryDao = database.memoryDao(),
        vaultInteractionDao = database.vaultInteractionDao()
    )
    private val settingsRepository = SettingsRepository(application)
    private val engine = OfflineAssistantEngine(repository)

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val messages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notes: StateFlow<List<NoteEntity>> = repository.notes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val memories: StateFlow<List<MemoryEntity>> = repository.memories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val vaultInteractions: StateFlow<List<VaultInteractionEntity>> = repository.vaultInteractions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _speakEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val speakEvents: SharedFlow<String> = _speakEvents.asSharedFlow()

    private var timerJob: Job? = null

    fun sendMessage(
        rawText: String,
        speakReply: Boolean = preferences.value.autoSpeak,
        modality: InteractionModality = InteractionModality.TEXT
    ) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            repository.saveMessage(
                ChatMessageEntity(
                    sender = MessageSender.USER.name,
                    text = text
                )
            )
            val reply = engine.processInput(text)

            reply.requestedThemeMode?.let { mode ->
                settingsRepository.setThemeMode(mode)
            }
            reply.startTimerSeconds?.let { seconds ->
                startTimer(seconds, reply.timerLabel ?: "Offline Timer")
            }

            repository.saveMessage(
                ChatMessageEntity(
                    sender = MessageSender.LUMA.name,
                    text = reply.text,
                    cardType = reply.cardType.name,
                    cardTitle = reply.cardTitle,
                    cardSubtitle = reply.cardSubtitle
                )
            )

            repository.saveVaultInteraction(
                userPrompt = text,
                assistantReply = reply.text,
                cardType = reply.cardType,
                cardTitle = reply.cardTitle,
                modality = modality
            )

            if (speakReply) {
                _speakEvents.tryEmit(reply.text)
            }
        }
    }

    fun toggleDarkModeQuick(currentlyDark: Boolean) {
        viewModelScope.launch {
            val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
            settingsRepository.setThemeMode(next)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSpeak(enabled)
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepository.setSpeechRate(rate)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setHandsFreeLoop(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHandsFreeLoop(enabled)
        }
    }

    fun addTask(title: String, isHighPriority: Boolean = false) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTask(title, isHighPriority)
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTask(task)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun addNote(title: String, content: String, tag: String = "Personal") {
        if (content.isBlank() && title.isBlank()) return
        viewModelScope.launch {
            repository.addNote(
                title = title.ifBlank { "Quick Note" },
                content = content.ifBlank { title },
                tag = tag
            )
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun addMemory(key: String, answer: String) {
        if (key.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            repository.addMemory(key, answer)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun toggleInteractionPin(interaction: VaultInteractionEntity) {
        viewModelScope.launch {
            repository.toggleInteractionPin(interaction)
        }
    }

    fun deleteVaultInteraction(id: Long) {
        viewModelScope.launch {
            repository.deleteVaultInteraction(id)
        }
    }

    fun clearUnpinnedInteractions() {
        viewModelScope.launch {
            repository.clearUnpinnedInteractions()
        }
    }

    fun startTimer(seconds: Int, label: String = "Focus Timer") {
        timerJob?.cancel()
        val safeSeconds = seconds.coerceAtLeast(1)
        _timerState.value = TimerState(
            isActive = true,
            isPaused = false,
            totalSeconds = safeSeconds,
            remainingSeconds = safeSeconds,
            label = label,
            isFinished = false
        )
        runTimerLoop()
    }

    fun pauseResumeTimer() {
        val current = _timerState.value
        if (!current.isActive) return
        if (current.isPaused) {
            _timerState.value = current.copy(isPaused = false)
            runTimerLoop()
        } else {
            timerJob?.cancel()
            _timerState.value = current.copy(isPaused = true)
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.isActive && !_timerState.value.isPaused && _timerState.value.remainingSeconds > 0) {
                delay(1000L)
                val next = (_timerState.value.remainingSeconds - 1).coerceAtLeast(0)
                if (next == 0) {
                    val finishedLabel = _timerState.value.label
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = 0,
                        isFinished = true,
                        isActive = false
                    )
                    repository.saveMessage(
                        ChatMessageEntity(
                            sender = MessageSender.LUMA.name,
                            text = "Time's up! Your $finishedLabel has finished.",
                            cardType = ResponseCardType.TIMER_STARTED.name,
                            cardTitle = "Timer Complete",
                            cardSubtitle = finishedLabel
                        )
                    )
                    if (preferences.value.autoSpeak) {
                        _speakEvents.tryEmit("Time is up for $finishedLabel.")
                    }
                } else {
                    _timerState.value = _timerState.value.copy(remainingSeconds = next)
                }
            }
        }
    }
}
