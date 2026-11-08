package com.ghostgram.app.presentation.components.player

expect class GhostAudioRecorder() {
    fun startRecording(filePath: String)
    fun stopRecording()
}