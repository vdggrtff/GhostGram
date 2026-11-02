package repository

interface SettingsRepository {
    fun getGeminiApiKey(): String
    fun saveGeminiApiKey(key: String)
    fun clearSettings()
}