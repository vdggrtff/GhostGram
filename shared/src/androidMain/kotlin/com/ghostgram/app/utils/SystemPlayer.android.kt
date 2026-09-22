package com.ghostgram.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

object AndroidContextProvider : KoinComponent {
    val context: Context by inject()
}

actual fun openVideoInSystemPlayer(filePath: String) {
    try {
        val context = AndroidContextProvider.context
        val file = File(filePath)

        if (!file.exists()) {
            println("❌ [ПЛЕЕР] Файл не найден: $filePath")
            return
        }

        // 💥 1. Генерируем безопасный content:// URI через FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // 💥 2. Создаем Intent для открытия видео
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Даем плееру право на чтение!
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Обязательно, т.к. запускаем не из Activity
        }

        // 💥 3. Запускаем стандартный плеер Android
        context.startActivity(intent)
        println("✅ [ПЛЕЕР] Видео успешно передано системному плееру Android!")

    } catch (e: Exception) {
        println("❌ [ПЛЕЕР] Ошибка запуска плеера на Android: ${e.message}")
        e.printStackTrace()
    }
}