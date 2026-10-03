package com.ghostgram.app.utils

actual fun openFileInSystem(filePath: String) {
    try {
        // Достаем контекст из Koin, как мы делали для видео
        val context = AndroidContextProvider.context
        val file = java.io.File(filePath)
        if (!file.exists()) return

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // Автоматически определяем MIME-тип по расширению (apk, pdf, docx...)
        val extension = file.extension
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        println("❌ Ошибка открытия файла на Android: ${e.message}")
    }
}