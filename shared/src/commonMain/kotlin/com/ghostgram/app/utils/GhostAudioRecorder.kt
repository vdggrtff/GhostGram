package com.ghostgram.app.utils

expect class GhostAudioRecorder() {
    fun startRecording(filePath: String)
    fun stopRecording()
}