package com.ghostgram.app.utils

import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread

actual class GhostAudioRecorder {
    private var targetLine: TargetDataLine? = null
    private var recordingThread: Thread? = null

    actual fun startRecording(filePath: String) {
        try {
            // Стандартный формат для микрофона: 44.1 кГц, 16 бит, моно
            val format = AudioFormat(44100f, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)

            if (!AudioSystem.isLineSupported(info)) {
                println("❌ [RECORDER-PC] Микрофон не поддерживается системой!")
                return
            }

            // Открываем канал микрофона
            targetLine = AudioSystem.getLine(info) as TargetDataLine
            targetLine?.open(format)
            targetLine?.start()

            println("🎤 [RECORDER-PC] Запись звука началась: $filePath")

            // Запускаем запись в фоновом потоке, чтобы не повесить UI!
            recordingThread = thread(start = true) {
                try {
                    val audioStream = AudioInputStream(targetLine)
                    val outFile = File(filePath)
                    // Пишем аудиопоток в файл формата WAV
                    AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outFile)
                } catch (e: Exception) {
                    println("❌ [RECORDER-PC] Ошибка сохранения аудиофайла: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ [RECORDER-PC] Ошибка старта микрофона: ${e.message}")
        }
    }

    actual fun stopRecording() {
        println("🛑 [RECORDER-PC] Остановка записи...")
        targetLine?.stop()
        targetLine?.close()
        targetLine = null
        recordingThread = null
    }
}