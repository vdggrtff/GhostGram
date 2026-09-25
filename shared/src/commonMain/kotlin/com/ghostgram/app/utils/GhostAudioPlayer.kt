package com.ghostgram.app.utils

expect class GhostAudioPlayer() {
    fun play(filePath: String, onFinished: () -> Unit)
    fun stop()
}