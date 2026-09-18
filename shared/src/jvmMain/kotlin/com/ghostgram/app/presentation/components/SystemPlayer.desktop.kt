package com.ghostgram.app.presentation.components

import java.awt.Desktop
import java.io.File

actual fun openVideoInSystemPlayer(filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            // 💥 Открывает файл в стандартном плеере Windows/Linux/Mac!
            Desktop.getDesktop().open(file)
        }
    } catch (e: Exception) {
        println("❌ Ошибка открытия видео: ${e.message}")
    }
}