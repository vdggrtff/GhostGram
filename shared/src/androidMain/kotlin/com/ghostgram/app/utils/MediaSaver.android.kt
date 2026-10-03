package com.ghostgram.app.utils

import android.os.Environment
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File

actual fun saveMediaToDownloads(sourcePath: String, fileName: String): String? {
    return try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val targetFile = File(downloadsDir, fileName)
        val fs = FileSystem.SYSTEM
        fs.copy(sourcePath.toPath(), targetFile.absolutePath.toPath())
        println("💾 [SAVER-ANDROID] Файл сохранен в: ${targetFile.absolutePath}")
        targetFile.absolutePath
    } catch (e: Exception) {
        println("❌ [SAVER-ANDROID] Ошибка сохранения: ${e.message}")
        null
    }
}