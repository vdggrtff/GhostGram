package com.ghostgram.app.utils

actual class GhostAudioRecorder {
    actual fun startRecording(filePath: String) {
        println("🎤 [RECORDER-PC] Запись звука на ПК пока в разработке. Путь: $filePath")
    }
    actual fun stopRecording() {
        println("🛑 [RECORDER-PC] Остановка записи ПК.")
    }
}