package com.ghostgram.app.utils

import android.media.MediaRecorder
import android.os.Build


actual class GhostAudioRecorder {
    private var recorder: MediaRecorder? = null

    actual fun startRecording(filePath: String) {
        try {
            val context = AndroidContextProvider.context
            // Используем старый API для совместимости или новый для Android 12+
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context) // Твой контекст из KoinComponent
            } else {
                @Suppress("DEPRECATION")
                (MediaRecorder())
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // Формат OGG/MPEG4 лучше всего подходит для голосовых
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
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