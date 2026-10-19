package com.ghostgram.core.crypto

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

fun ByteArray.toHex(): String = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
fun String.decodeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

class CryptoLayer(databasePath: String) {
    private val cryptoEngine = GhostCrypto()

    private val fallbackKey = ByteArray(32) { 42 }

    // TODO: Migrate key storage from filesystem to encrypted platform keystore
    val myKeyPair: Pair<ByteArray, ByteArray> = try {
        loadOrGenerateKeys(databasePath)
    } catch (e: Exception) {
        println("CryptoLayer: Failed to load keys at startup: ${e.message}")
        e.printStackTrace()
        // Fallback dummy keys to prevent DI crash on unrecoverable keystore failure
        Pair(fallbackKey, fallbackKey)
    }

    private val activeSecureChats = mutableMapOf<Long, ByteArray>()

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
}