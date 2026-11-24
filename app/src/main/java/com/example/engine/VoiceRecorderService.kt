package com.example.engine

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.math.sqrt

data class VoiceRecorderState(
    val isRecording: Boolean = false,
    val elapsedMillis: Long = 0L,
    val amplitudeNormalized: Float = 0f,
    val activeFilePath: String? = null,
    val errorMessage: String? = null
)

data class RecordedVoiceClip(
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long
)

object VoiceRecorderBus {
    private val _state = MutableStateFlow(VoiceRecorderState())
    val state: StateFlow<VoiceRecorderState> = _state.asStateFlow()

    private val _completedClips = MutableSharedFlow<RecordedVoiceClip>(extraBufferCapacity = 8)
    val completedClips: SharedFlow<RecordedVoiceClip> = _completedClips.asSharedFlow()

    internal fun updateState(transform: (VoiceRecorderState) -> VoiceRecorderState) {
        _state.value = transform(_state.value)
    }

    internal fun emitCompletedClip(clip: RecordedVoiceClip) {
        _completedClips.tryEmit(clip)
    }

    fun clearError() {
        if (_state.value.errorMessage != null) {
            _state.value = _state.value.copy(errorMessage = null)
        }
    }
}

class VoiceRecorderService : LifecycleService() {

    inner class LocalBinder : Binder() {
        fun getService(): VoiceRecorderService = this@VoiceRecorderService
    }

    private val binder = LocalBinder()
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtElapsedRealtime: Long = 0L
    private var meteringJob: Job? = null

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording(discard = false)
            ACTION_CANCEL_RECORDING -> stopRecording(discard = true)
        }
        return START_NOT_STICKY
    }

    fun startRecording() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            VoiceRecorderBus.updateState {
                VoiceRecorderState(errorMessage = "Microphone permission is required to record voice input.")
            }
            stopSelf()
            return
        }

        if (mediaRecorder != null) {
            return
        }

        val voiceDir = File(filesDir, VOICE_DIR_NAME).apply { mkdirs() }
        val targetFile = File(voiceDir, "luma_voice_${System.currentTimeMillis()}.m4a")
        outputFile = targetFile

        val recorder = createMediaRecorder()
        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(targetFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            startedAtElapsedRealtime = SystemClock.elapsedRealtime()

            promoteToForeground()

            VoiceRecorderBus.updateState {
                VoiceRecorderState(
                    isRecording = true,
                    elapsedMillis = 0L,
                    amplitudeNormalized = 0.08f,
                    activeFilePath = targetFile.absolutePath,
                    errorMessage = null
                )
            }
            startAmplitudeMetering()
        } catch (e: IOException) {
            cleanupFailedStart(recorder, targetFile, "Could not prepare audio file for voice capture.")
        } catch (e: IllegalStateException) {
            cleanupFailedStart(recorder, targetFile, "Microphone is busy or unavailable right now.")
        } catch (e: RuntimeException) {
            cleanupFailedStart(recorder, targetFile, "Unable to start voice recording on this device.")
        }
    }

    fun stopRecording(discard: Boolean = false) {
        meteringJob?.cancel()
        meteringJob = null

        val recorder = mediaRecorder
        val file = outputFile
        mediaRecorder = null
        outputFile = null

        val durationMs = if (startedAtElapsedRealtime > 0L) {
            (SystemClock.elapsedRealtime() - startedAtElapsedRealtime).coerceAtLeast(0L)
        } else {
            0L
        }
        startedAtElapsedRealtime = 0L

        var stopSucceeded = false
        if (recorder != null) {
            try {
                recorder.stop()
                stopSucceeded = true
            } catch (_: RuntimeException) {
                stopSucceeded = false
            } finally {
                try {
                    recorder.reset()
                } catch (_: RuntimeException) {
                }
                try {
                    recorder.release()
                } catch (_: RuntimeException) {
                }
            }
        }

        if (discard || !stopSucceeded || file == null || !file.exists() || file.length() == 0L || durationMs < MIN_VALID_DURATION_MS) {
            file?.delete()
            val message = when {
                discard -> null
                durationMs in 1 until MIN_VALID_DURATION_MS -> "Voice clip was too short. Hold or speak a little longer."
                !stopSucceeded && recorder != null -> "Voice recording stopped before audio could be captured."
                else -> null
            }
            VoiceRecorderBus.updateState {
                VoiceRecorderState(
                    isRecording = false,
                    elapsedMillis = 0L,
                    amplitudeNormalized = 0f,
                    activeFilePath = null,
                    errorMessage = message
                )
            }
        } else {
            VoiceRecorderBus.updateState {
                VoiceRecorderState(
                    isRecording = false,
                    elapsedMillis = 0L,
                    amplitudeNormalized = 0f,
                    activeFilePath = null,
                    errorMessage = null
                )
            }
            VoiceRecorderBus.emitCompletedClip(
                RecordedVoiceClip(
                    filePath = file.absolutePath,
                    durationMs = durationMs,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAmplitudeMetering() {
        meteringJob?.cancel()
        meteringJob = lifecycleScope.launch {
            while (isActive && mediaRecorder != null) {
                val rawAmplitude = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (_: RuntimeException) {
                    0
                }
                val linear = (rawAmplitude / 32767f).coerceIn(0f, 1f)
                val boosted = sqrt(linear).coerceIn(0.05f, 1f)
                val elapsed = (SystemClock.elapsedRealtime() - startedAtElapsedRealtime).coerceAtLeast(0L)

                VoiceRecorderBus.updateState { current ->
                    if (current.isRecording) {
                        current.copy(
                            elapsedMillis = elapsed,
                            amplitudeNormalized = boosted
                        )
                    } else {
                        current
                    }
                }
                delay(METERING_INTERVAL_MS)
            }
        }
    }

    private fun promoteToForeground() {
        val notification = buildRecordingNotification()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun buildRecordingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceRecorderService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.voice_service_notification_title))
            .setContentText(getString(R.string.voice_service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.voice_service_stop_action),
                stopPendingIntent
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.voice_service_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.voice_service_channel_description)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun cleanupFailedStart(recorder: MediaRecorder, file: File, errorMsg: String) {
        try {
            recorder.reset()
        } catch (_: RuntimeException) {
        }
        try {
            recorder.release()
        } catch (_: RuntimeException) {
        }
        file.delete()
        mediaRecorder = null
        outputFile = null
        startedAtElapsedRealtime = 0L
        VoiceRecorderBus.updateState {
            VoiceRecorderState(errorMessage = errorMsg)
        }
        stopSelf()
    }

    override fun onDestroy() {
        meteringJob?.cancel()
        meteringJob = null
        mediaRecorder?.let { recorder ->
            try {
                recorder.stop()
            } catch (_: RuntimeException) {
            }
            try {
                recorder.release()
            } catch (_: RuntimeException) {
            }
        }
        mediaRecorder = null
        if (VoiceRecorderBus.state.value.isRecording) {
            VoiceRecorderBus.updateState { VoiceRecorderState() }
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_RECORDING = "com.example.engine.action.START_VOICE_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.engine.action.STOP_VOICE_RECORDING"
        const val ACTION_CANCEL_RECORDING = "com.example.engine.action.CANCEL_VOICE_RECORDING"

        private const val CHANNEL_ID = "luma_voice_capture"
        private const val NOTIFICATION_ID = 4102
        private const val VOICE_DIR_NAME = "voice_notes"
        private const val METERING_INTERVAL_MS = 80L
        private const val MIN_VALID_DURATION_MS = 350L

        fun startRecording(context: Context) {
            val intent = Intent(context, VoiceRecorderService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, VoiceRecorderService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }

        fun cancelRecording(context: Context) {
            val intent = Intent(context, VoiceRecorderService::class.java).apply {
                action = ACTION_CANCEL_RECORDING
            }
            context.startService(intent)
        }
    }
}
