package com.ghostgram.app.utils

actual class GhostAudioPlayer {
    actual fun play(filePath: String, onProgress: (currentMs: Int, totalMs: Int) -> Unit, onFinished: () -> Unit) {
        TODO()
    }

    actual fun stop() {TODO()}

    actual fun seekTo(progress: Float) {
        TODO()
    }
}