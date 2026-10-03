package com.ghostgram.app.utils

import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File

actual fun saveMediaToDownloads(sourcePath: String, fileName: String): String? {
    return try {
        val userHome = System.getProperty("user.home")
        val downloadsDir = listOf(
            File(userHome, "Загрузки"),
            File(userHome, "Downloads")
        ).firstOrNull { it.exists() && it.isDirectory } ?: File(userHome, "Загрузки").apply { mkdirs() }
        val targetFile = File(downloadsDir, fileName)

        val fs = FileSystem.SYSTEM
        fs.copy(sourcePath.toPath(), targetFile.absolutePath.toPath())
        println("💾 [SAVER-PC] Файл сохранен в: ${targetFile.absolutePath}")
        targetFile.absolutePath
    } catch (e: Exception) {
        println("❌ [SAVER-PC] Ошибка сохранения: ${e.message}")
        null
    }
}