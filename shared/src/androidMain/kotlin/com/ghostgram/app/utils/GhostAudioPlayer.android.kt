package com.ghostgram.app.utils

import android.media.MediaPlayer
import java.io.File

actual class GhostAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    actual fun play(filePath: String, onFinished: () -> Unit) {
        stop()
        try {
            val file = File(filePath)
            if (!file.exists()) {
                println("❌ [AUDIO-ANDROID] Файл не найден: $filePath")
                onFinished()
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    stop()
                    onFinished() // Когда доиграло, возвращаем иконку ▶
                }
            }
            println("🔊 [AUDIO-ANDROID] Играет звук: $filePath")
        } catch (e: Exception) {
            println("❌ [AUDIO-ANDROID] Ошибка: ${e.message}")
            onFinished()
        }
    }

    actual fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
    }
}