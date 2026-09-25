package com.ghostgram.app.utils

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

    private val memoryCache = mutableMapOf<String, String>()

    /**
     * Читает сжатый .tgs файл с диска, распаковывает GZIP и возвращает чистый JSON
     */
    suspend fun decodeTgsToJson(filePath: String): String? {
        memoryCache[filePath]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val path = filePath.toPath()
                val fileSystem = FileSystem.SYSTEM

                if (!fileSystem.exists(path)) return@withContext null

                // Магия Okio: читаем файл, прогоняем через GzipSource и конвертируем в строку!
                /*fileSystem.source(path).let { GzipSource(it) }.buffer().use {
                    it.readUtf8()
                }*/
                val json = fileSystem.source(path).let { GzipSource(it) }.buffer().use {
                    it.readUtf8()
                }

                if (json.isNotBlank()) {
                    if (memoryCache.size > 100) memoryCache.clear() // Защита от переполнения RAM
                    memoryCache[filePath] = json
                }
                json
            } catch (e: Exception) {
                println("❌ Ошибка распаковки TGS: ${e.message}")
                null
            }
        }
    }
}