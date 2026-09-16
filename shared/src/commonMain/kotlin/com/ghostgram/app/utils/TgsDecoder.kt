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
    /**
     * Читает сжатый .tgs файл с диска, распаковывает GZIP и возвращает чистый JSON
     */
    suspend fun decodeTgsToJson(filePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val path = filePath.toPath()
                val fileSystem = FileSystem.SYSTEM

                if (!fileSystem.exists(path)) return@withContext null

                // 💥 Магия Okio: читаем файл, прогоняем через GzipSource и конвертируем в строку!
                fileSystem.source(path).let { GzipSource(it) }.buffer().use {
                    it.readUtf8()
                }
            } catch (e: Exception) {
                println("❌ Ошибка распаковки TGS: ${e.message}")
                null
            }
        }
    }
}