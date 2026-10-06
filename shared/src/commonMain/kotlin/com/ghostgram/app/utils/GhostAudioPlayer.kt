package com.ghostgram.app.utils

expect class GhostAudioPlayer() {
    fun play(filePath: String,  onProgress: (currentMs: Int, totalMs: Int) -> Unit, onFinished: () -> Unit)
    fun stop()
    fun seekTo(progress: Float)
}