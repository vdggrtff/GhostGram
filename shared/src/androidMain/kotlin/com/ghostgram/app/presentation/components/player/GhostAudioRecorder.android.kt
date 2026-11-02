package com.ghostgram.app.presentation.components.player

import android.media.MediaRecorder
import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.AudioSource
import android.media.MediaRecorder.OutputFormat
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.ghostgram.app.utils.AndroidContextProvider


actual class GhostAudioRecorder {
    private var recorder: MediaRecorder? = null

    actual fun startRecording(filePath: String) {
        try {
            val context = AndroidContextProvider.context
            // Используем старый API для совместимости или новый для Android 12+
            recorder = if (VERSION.SDK_INT >= VERSION_CODES.S) {
                MediaRecorder(context) // Твой контекст из KoinComponent
            } else {
                @Suppress("DEPRECATION")
                (MediaRecorder())
            }

            recorder?.apply {
                setAudioSource(AudioSource.MIC)
                // Формат OGG/MPEG4 лучше всего подходит для голосовых
                setOutputFormat(OutputFormat.MPEG_4)
                setAudioEncoder(AudioEncoder.AAC)
                setOutputFile(filePath)
                prepare()
                start()
            }
            println("🎤 [RECORDER] Запись пошла: $filePath")
        } catch (e: Exception) {
            println("❌ [RECORDER] Ошибка микрофона: ${e.message}")
        }
    }

    actual fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            println("🛑 [RECORDER] Запись остановлена!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}