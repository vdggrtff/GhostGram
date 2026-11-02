package com.ghostgram.app.presentation.components.player

actual class GhostAudioPlayer {
    actual fun play(filePath: String, onProgress: (currentMs: Int, totalMs: Int) -> Unit, onFinished: () -> Unit) {
        TODO()
    }

    actual fun stop() {TODO()}

    actual fun seekTo(progress: Float) {
        TODO()
    }
}