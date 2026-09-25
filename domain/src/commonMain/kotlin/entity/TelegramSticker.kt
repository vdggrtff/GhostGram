package entity

data class TelegramSticker(
    val fileId: Int,             // Внутренний ID файла в TDLib для отправки
    val remoteFileId: String,
    val emoji: String,           // Эмодзи стикера (например, "🔥")
    val thumbnailPath: String?   // Путь к скачанной статичной превьюшке .webp
)