package com.ghostgram.data.local

import config.AppStorageConfig
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import repository.SettingsRepository

class LocalSettingsManager(
    private val appStorage: AppStorageConfig
): SettingsRepository {

    private val fs = FileSystem.SYSTEM
    private val settingsFile = "${appStorage.basePath}/ghostgram_settings.txt".toPath()

    override fun getGeminiApiKey(): String {
        if (!fs.exists(settingsFile)) return ""
        return try {
            fs.read(settingsFile) { readUtf8() }.trim()
        } catch (e: Exception) {
            ""
        }
    }

    override fun saveGeminiApiKey(key: String) {
        try {
            // Если родительской папки нет — создаем
            settingsFile.parent?.let { if (!fs.exists(it)) fs.createDirectories(it) }
            fs.write(settingsFile) { writeUtf8(key.trim()) }
            println("🔑 [SETTINGS] Ключ Gemini успешно записан: ${settingsFile}")
        } catch (e: Exception) {
            println("❌ [SETTINGS] Ошибка записи ключа: ${e.message}")
        }
    }

    override fun clearSettings() {
        try {
            if (fs.exists(settingsFile)) fs.delete(settingsFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/*
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
}*/
