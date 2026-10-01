package com.ghostgram.app.utils

actual fun openFileInSystem(filePath: String) {
    try {
        val file = java.io.File(filePath)
        if (file.exists()) {
            java.awt.Desktop.getDesktop().open(file)
        }
    } catch (e: Exception) {
        println("❌ Ошибка открытия файла на Desktop: ${e.message}")
    }
}