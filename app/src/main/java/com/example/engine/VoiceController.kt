package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data class Listening(val partialText: String, val rmsDb: Float) : VoiceUiState
    data object Speaking : VoiceUiState
    data class Info(val message: String) : VoiceUiState
}

class VoiceController(
    private val context: Context,
    private val onRecognizedText: (String) -> Unit
) {
    private val _voiceState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val voiceState: StateFlow<VoiceUiState> = _voiceState.asStateFlow()

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var speechRate: Float = 1.0f

    val isSpeechRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(speechRate)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceUiState.Speaking
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_voiceState.value is VoiceUiState.Speaking) {
                            _voiceState.value = VoiceUiState.Idle
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_voiceState.value is VoiceUiState.Speaking) {
                            _voiceState.value = VoiceUiState.Idle
                        }
                    }
                })
                _ttsReady.value = true
            }
        }
    }

    fun updateSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.6f, 1.6f)
        tts?.setSpeechRate(speechRate)
    }

    fun speak(text: String) {
        if (!_ttsReady.value || text.isBlank()) return
        stopListening()
        val id = "luma_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_voiceState.value is VoiceUiState.Speaking) {
            _voiceState.value = VoiceUiState.Idle
        }
    }

    fun startListening() {
        if (!isSpeechRecognitionAvailable) {
            _voiceState.value = VoiceUiState.Info(
                "Offline speech service is unavailable on this device. Use quick voice chips or type below."
            )
            return
        }
        stopSpeaking()
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(createListener())
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        _voiceState.value = VoiceUiState.Listening(partialText = "", rmsDb = 0f)
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_voiceState.value is VoiceUiState.Listening) {
            _voiceState.value = VoiceUiState.Idle
        }
    }

    fun clearMessage() {
        if (_voiceState.value is VoiceUiState.Info) {
            _voiceState.value = VoiceUiState.Idle
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceUiState.Listening(partialText = "", rmsDb = 1f)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            val current = _voiceState.value
            if (current is VoiceUiState.Listening) {
                _voiceState.value = current.copy(rmsDb = rmsdB.coerceIn(0f, 12f))
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap the orb to speak again."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap the orb when ready."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice."
                else -> "Offline speech service returned code $error. You can type instead."
            }
            _voiceState.value = VoiceUiState.Info(msg)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognized = matches?.firstOrNull()?.trim().orEmpty()
            _voiceState.value = VoiceUiState.Idle
            if (recognized.isNotBlank()) {
                onRecognizedText(recognized)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty()
            val current = _voiceState.value
            if (current is VoiceUiState.Listening && text.isNotBlank()) {
                _voiceState.value = current.copy(partialText = text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
