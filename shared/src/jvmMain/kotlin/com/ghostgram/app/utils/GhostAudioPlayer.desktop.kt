package com.ghostgram.app.utils

import java.awt.Desktop
import java.io.File

actual class GhostAudioPlayer {
    actual fun play(filePath: String, onProgress: (currentMs: Int, totalMs: Int) -> Unit, onFinished: () -> Unit) {
        try {
            val file = File(filePath)
            if (file.exists()) Desktop.getDesktop().open(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onFinished()
    }
    actual fun stop() {}
    actual fun seekTo(progress: Float) {}
}