package com.example.engine

import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class VoiceAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    private val _playingFilePath = MutableStateFlow<String?>(null)
    val playingFilePath: StateFlow<String?> = _playingFilePath.asStateFlow()

    fun togglePlayback(filePath: String) {
        if (_playingFilePath.value == filePath) {
            stop()
            return
        }
        play(filePath)
    }

    fun play(filePath: String) {
        stop()
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return

        val player = MediaPlayer()
        try {
            player.setDataSource(filePath)
            player.setOnCompletionListener {
                stop()
            }
            player.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
            _playingFilePath.value = filePath
        } catch (_: IOException) {
            player.release()
            _playingFilePath.value = null
        } catch (_: IllegalStateException) {
            player.release()
            _playingFilePath.value = null
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: IllegalStateException) {
            }
            try {
                player.release()
            } catch (_: RuntimeException) {
            }
        }
        mediaPlayer = null
        _playingFilePath.value = null
    }

    fun release() {
        stop()
    }
}
