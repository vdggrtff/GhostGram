package com.ghostgram.app.utils

import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.GzipSource
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

object TgsDecoder {
    // 💥 КЭШ В ОПЕРАТИВКЕ: путь к файлу -> разархивированный JSON
    private val jsonCache = mutableMapOf<String, String>()

    fun getJsonFromCache(filePath: String): String? = jsonCache[filePath]

    /**
     * Распаковывает GZIP-архив .tgs в чистую JSON-строку
     */
    suspend fun decodeTgsToJson(filePath: String): String? {
        // Если уже распаковывали — отдаем из памяти за 0 мс!
        jsonCache[filePath]?.let { return it }

        return withContext(Dispatchers.Default) {
            try {
                val path = filePath.toPath()
                val fileSystem = FileSystem.SYSTEM
                if (!fileSystem.exists(path)) return@withContext null

                // Распаковываем GZIP поток
                val json = fileSystem.source(path).let { GzipSource(it) }.buffer().use {
                    it.readUtf8()
                }

                if (json.isNotBlank()) {
                    if (jsonCache.size > 100) jsonCache.clear() // защита от переполнения RAM
                    jsonCache[filePath] = json
                    json
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}