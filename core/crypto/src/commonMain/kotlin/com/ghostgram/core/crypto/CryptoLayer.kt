package com.ghostgram.core.crypto

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

fun ByteArray.toHex(): String = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
fun String.decodeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

class CryptoLayer(databasePath: String) {
    private val cryptoEngine = GhostCrypto()

    private val fallbackKey = ByteArray(32) { 42 }

    // 💥 1. Наша личная пара ключей (в идеале потом сохраним в зашифрованный DataStore, пока держим в памяти)
    val myKeyPair: Pair<ByteArray, ByteArray> = try {
        loadOrGenerateKeys(databasePath)
    } catch (e: Exception) {
        println("❌ ФАТАЛЬНАЯ ОШИБКА КРИПТОГРАФИИ ПРИ СТАРТЕ: ${e.message}")
        e.printStackTrace()
        // Отдаем фейковую пару ключей, чтобы Koin не крашнул приложение!
        Pair(fallbackKey, fallbackKey)
    }


    // 💥 2. Храним вычисленные AES-секреты для каждого чата: ChatID -> Секретный Ключ
    private val activeSecureChats = mutableMapOf<Long, ByteArray>()

    // Проверяем, есть ли у нас ключ с этим чатом
    fun isChatSecure(chatId: Long): Boolean = activeSecureChats.containsKey(chatId)

    private fun loadOrGenerateKeys(pathStr: String): Pair<ByteArray, ByteArray> {
        val fs = FileSystem.SYSTEM
        val dir = pathStr.toPath()
        val privFile = dir / "ghost_priv.key"
        val pubFile = dir / "ghost_pub.key"

        println("🔐 Пытаемся загрузить ключи из: $dir")

        if (!fs.exists(dir)) {
            fs.createDirectories(dir)
        }

        if (fs.exists(privFile) && fs.exists(pubFile)) {
            println("🔐 Ключи E2EE успешно загружены с диска!")
            val priv = fs.read(privFile) { readByteArray() }
            val pub = fs.read(pubFile) { readByteArray() }
            return Pair(priv, pub)
        }

        println("🔐 Генерация новых ключей E2EE...")
        val newKeys = cryptoEngine.generateKeyPair()

        fs.write(privFile) { write(newKeys.first) }
        fs.write(pubFile) { write(newKeys.second) }

        println("✅ Ключи успешно сгенерированы и сохранены!")
        return newKeys
    }

    fun establishSecret(chatId: Long, otherPublicKeyHex: String) {
        try {
            val otherPubKey = otherPublicKeyHex.decodeHex()
            val sharedSecret = cryptoEngine.generateSharedSecret(myKeyPair.first, otherPubKey)
            activeSecureChats[chatId] = sharedSecret
            println("🔐 E2EE УСТАНОВЛЕН ДЛЯ ЧАТА $chatId!")
        } catch (e: Exception) {
            println("❌ E2EE ОШИБКА: Неверный публичный ключ")
        }
    }

    // 💥 4. Шифруем и расшифровываем с использованием УНИКАЛЬНОГО ключа чата
    fun encryptAndHide(chatId: Long, text: String): String {
        val key = activeSecureChats[chatId] ?: return text // Если нет ключа - шлем как есть
        val rawBytes = text.encodeToByteArray()
        val encryptedBytes = cryptoEngine.encryptAES(key, rawBytes)

        val promo = "\n\n🔒 GhostGRAM E2EE\n👉 github.com/ghostgram"
        return WordCoder.encode(encryptedBytes) + promo
    }

    fun revealAndDecrypt(chatId: Long, hiddenText: String): String? {
        val key = activeSecureChats[chatId] ?: return null
        val encryptedBytes = WordCoder.decode(hiddenText) ?: return null
        val decryptedBytes = cryptoEngine.decryptAES(key, encryptedBytes) ?: return null
        return decryptedBytes.decodeToString()
    }
/*
    *//**
     * Зашифровывает текст и маскирует его в слова
     *//*
    fun encryptAndHide(text: String, sharedKey: ByteArray): String {
        val rawBytes = text.encodeToByteArray()
        val encryptedBytes = cryptoEngine.encryptAES(sharedKey, rawBytes)
        val hiddenWords = WordCoder.encode(encryptedBytes)

        // 💥 НАША ВИРУСНАЯ РЕКЛАМА ДЛЯ ОФИЦИАЛЬНОГО ТЕЛЕГРАМА:
        val promoFooter = """
            
            
            🔒 Зашифровано в GhostGRAM
            🛡 End-to-End Steganography E2EE
            👉 github.com/ghostgram (или твой канал)
        """.trimIndent()

        return "$hiddenWords$promoFooter"
    }


    *//**
     * Достает слова, превращает в байты и расшифровывает
     *//*
    fun revealAndDecrypt(hiddenText: String, sharedKey: ByteArray): String? {
        val encryptedBytes = WordCoder.decode(hiddenText) ?: return null
        val decryptedBytes = cryptoEngine.decryptAES(sharedKey, encryptedBytes) ?: return null
        return decryptedBytes.decodeToString()
    }*/
}