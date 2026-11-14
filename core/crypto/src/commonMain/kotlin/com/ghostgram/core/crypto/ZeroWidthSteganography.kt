package com.ghostgram.core.crypto

object ZeroWidthSteganography {

    // 💥 4 НЕВИДИМЫХ СИМВОЛА UNICODE (занимают ровно 0 пикселей на экране):
    private const val ZW_00 = '\u200B' // Zero-Width Space
    private const val ZW_01 = '\u200C' // Zero-Width Non-Joiner
    private const val ZW_10 = '\u200D' // Zero-Width Joiner
    private const val ZW_11 = '\u2060' // Word Joiner

    // 💥 НЕВИДИМЫЙ СЕКРЕТНЫЙ МАРКЕР GHOSTGRAM:
    private const val MAGIC_HEADER = "\u200D\u200B\u200D\u2060"
    private const val MAGIC_FOOTER = "\u2060\u200D\u200B\u200D"

    // Обычные человеческие фразы-прикрытия (чтобы в официальном Telegram не было видно шифра):
    private val CASUAL_COVER_PHRASES = listOf(
        "Договорились, буду вовремя!",
        "Хорошо, на связи.",
        "Окей, без проблем!",
        "Понял, сделаю.",
        "Да, всё в силе.",
        "Привет! Как дела?",
        "Отлично, тогда до встречи!"
    )

    /**
     * Прячет зашифрованные байты внутрь текста.
     * В официальном Telegram отобразится только обычная фраза!
     */
    fun hide(bytes: ByteArray, customCoverText: String? = null): String {
        val cover = customCoverText?.trim()?.ifBlank { null } ?: CASUAL_COVER_PHRASES.random()

        val invisiblePayload = buildString {
            append(MAGIC_HEADER)
            for (byte in bytes) {
                val b = byte.toInt() and 0xFF
                append(bitsToChar((b shr 6) and 0x03))
                append(bitsToChar((b shr 4) and 0x03))
                append(bitsToChar((b shr 2) and 0x03))
                append(bitsToChar(b and 0x03))
            }
            append(MAGIC_FOOTER)
        }

        // Вставляем невидимый шифр перед точкой/знаком в конце
        return if (cover.length > 2) {
            cover.substring(0, cover.length - 1) + invisiblePayload + cover.last()
        } else {
            cover + invisiblePayload
        }
    }

    /**
     * Проверяет, есть ли внутри сообщения скрытый невидимый шифр GhostGRAM.
     */
    fun containsHiddenPayload(text: String): Boolean {
        return text.contains(MAGIC_HEADER) && text.contains(MAGIC_FOOTER)
    }

    /**
     * Извлекает спрятанные байты шифра обратно.
     */
    fun reveal(text: String): ByteArray? {
        val startIndex = text.indexOf(MAGIC_HEADER)
        val endIndex = text.indexOf(MAGIC_FOOTER)
        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) return null

        val payloadChars = text.substring(startIndex + MAGIC_HEADER.length, endIndex)
        if (payloadChars.length % 4 != 0) return null

        val bytes = ByteArray(payloadChars.length / 4)
        var byteIndex = 0

        for (i in payloadChars.indices step 4) {
            val b0 = charToBits(payloadChars[i]) ?: return null
            val b1 = charToBits(payloadChars[i + 1]) ?: return null
            val b2 = charToBits(payloadChars[i + 2]) ?: return null
            val b3 = charToBits(payloadChars[i + 3]) ?: return null

            bytes[byteIndex++] = ((b0 shl 6) or (b1 shl 4) or (b2 shl 2) or b3).toByte()
        }

        return bytes
    }

    private fun bitsToChar(bits: Int): Char = when (bits) {
        0 -> ZW_00
        1 -> ZW_01
        2 -> ZW_10
        3 -> ZW_11
        else -> ZW_00
    }

    private fun charToBits(c: Char): Int? = when (c) {
        ZW_00 -> 0
        ZW_01 -> 1
        ZW_10 -> 2
        ZW_11 -> 3
        else -> null
    }
}