package com.ghostgram.app.utils

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object WaveformDecoder {

    /**
     * 💥 Распаковывает 5-битный массив Telegram в нормализованные высоты столбиков (0.0f .. 1.0f)
     * и ресэмплит их ровно в 36 столбиков для идеальной посадки в пузырь!
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decode(base64Waveform: String?, targetBarCount: Int = 36): List<Float> {
        if (base64Waveform.isNullOrBlank()) {
            // Фолбэк на дефолтную легкую волну, если метаданных нет
            return List(targetBarCount) { 0.25f }
        }

        val bytes = try {
            Base64.decode(base64Waveform)
        } catch (e: Exception) {
            return List(targetBarCount) { 0.25f }
        }

        val totalBits = bytes.size * 8
        val totalSamples = totalBits / 5
        if (totalSamples == 0) return List(targetBarCount) { 0.25f }

        val rawSamples = ArrayList<Float>(totalSamples)

        // 💥 Математика побитового сдвига Telegram: читаем по 5 бит
        for (i in 0 until totalSamples) {
            val bitIndex = i * 5
            val byteIndex = bitIndex / 8
            val bitOffset = bitIndex % 8

            var value = (bytes[byteIndex].toInt() and 0xFF) ushr bitOffset
            if (bitOffset > 3 && byteIndex + 1 < bytes.size) {
                val nextByte = (bytes[byteIndex + 1].toInt() and 0xFF)
                value = value or (nextByte shl (8 - bitOffset))
            }

            val sample5Bit = value and 0x1F // Берем младшие 5 бит (0..31)
            rawSamples.add((sample5Bit / 31f).coerceIn(0.1f, 1f))
        }

        // Приводим пачку к нужному количеству столбиков (36 штук)
        val step = rawSamples.size.toFloat() / targetBarCount
        return List(targetBarCount) { index ->
            val sampleIndex = (index * step).toInt().coerceIn(0, rawSamples.size - 1)
            rawSamples[sampleIndex]
        }
    }
}