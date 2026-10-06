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

/*object TgsDecoder {

    private val memoryCache = mutableMapOf<String, String>()
    // 💥 КЭШ РАСПАРСЕННЫХ LOTTIE: путь -> готовый объект LottieComposition!
    // Парсинг JSON будет происходить РОВНО 1 РАЗ!
    fun getJsonFromCache(filePath: String): String? = memoryCache[filePath]

    *//**
     * Читает сжатый .tgs файл с диска, распаковывает GZIP и возвращает чистый JSON
     *//*
    suspend fun decodeTgsToJson(filePath: String): String? {
        if (filePath.isBlank() || filePath == "." || filePath == "/") return null
        memoryCache[filePath]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val path = filePath.toPath()
                val fileSystem = FileSystem.SYSTEM
                if (!fileSystem.exists(path)) return@withContext null

                val json = fileSystem.source(path).let { GzipSource(it) }.buffer().use {
                    it.readUtf8()
                }

                if (json.isNotBlank()) {
                    if (memoryCache.size > 100) memoryCache.clear()
                    memoryCache[filePath] = json
                }
                json
            } catch (e: Exception) {
                null
            }
        }
    }*/
    /*suspend fun decodeTgsToJson(filePath: String): String? {
        if (filePath.isBlank() || filePath == "." || filePath == "/") return null

        memoryCache[filePath]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val path = filePath.toPath()
                val fileSystem = FileSystem.SYSTEM

                //if (!fileSystem.exists(path)) return@withContext null
                if (!fileSystem.exists(path)) return@withContext null
                val metadata = fileSystem.metadataOrNull(path)
                if (metadata?.isDirectory == true) return@withContext null

                // Магия Okio: читаем файл, прогоняем через GzipSource и конвертируем в строку!
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
    }*/
//}