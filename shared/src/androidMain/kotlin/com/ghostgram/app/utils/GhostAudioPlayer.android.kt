package com.ghostgram.app.utils

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

actual class GhostAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    actual fun play(
        filePath: String,
        onProgress: (currentMs: Int, totalMs: Int) -> Unit,
        onFinished: () -> Unit
    ) {
        stop()
        try {
            val file = File(filePath)
            if (!file.exists()) {
                onFinished()
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    stop()
                    onFinished()
                }
            }

            val totalDuration = mediaPlayer?.duration ?: 0

            // 💥 Запускаем таймер с тиком каждые 50 мс
            progressJob = CoroutineScope(Dispatchers.Main).launch {
                while (mediaPlayer?.isPlaying == true) {
                    val current = mediaPlayer?.currentPosition ?: 0
                    onProgress(current, totalDuration)
                    delay(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onFinished()
        }
    }

    actual fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
    }

    actual fun seekTo(progress: Float) {
        mediaPlayer?.let { player ->
            val targetMs = (player.duration * progress).toInt()
            player.seekTo(targetMs)
        }
    }
}