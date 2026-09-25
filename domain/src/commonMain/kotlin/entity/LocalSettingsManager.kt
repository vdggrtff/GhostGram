package entity

import AppStorageConfig
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class LocalSettingsManager(
    private val appStorage: AppStorageConfig
) {
    private val settingsFile = "${appStorage.basePath}/ghostgram_settings.txt".toPath()

    // Сохраняем ключ
    fun saveGeminiKey(key: String) {
        val fs = FileSystem.SYSTEM
        fs.write(settingsFile) { writeUtf8(key) }
    }

    // Читаем ключ (если файла нет - вернет пустую строку)
    fun getGeminiKey(): String {
        val fs = FileSystem.SYSTEM
        if (!fs.exists(settingsFile)) return ""
        return try {
            fs.read(settingsFile) { readUtf8() }
        } catch (e: Exception) {
            ""
        }
    }
}