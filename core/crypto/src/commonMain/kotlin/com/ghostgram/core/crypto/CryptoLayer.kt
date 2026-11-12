package com.ghostgram.core.crypto

import entity.SafetyFingerprint
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

fun ByteArray.toHex(): String = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
fun String.decodeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

// 💥 2. СТАТУС ПРОВЕРКИ КЛЮЧА
enum class KeyVerificationStatus {
    TRUSTED,      // Ключ подтвержден
    NEW_KEY,      // Первый обмен ключами
    MITM_WARNING  // 🚨 ТРЕВОГА! Ключ подменен хакером!
}

class CryptoLayer(databasePath: String) {
    private val cryptoEngine = GhostCrypto()
    private val fallbackKey = ByteArray(32) { 42 }

    // Словарь 64 эмодзи для отпечатка безопасности
    private val FINGERPRINT_EMOJIS = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
        "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
        "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺",
        "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞",
        "🐢", "🐍", "🦎", "🐙", "🦑", "🦐", "🦞", "🦀",
        "🐡", "🐠", "🐟", "🐬", "🐳", "🦈", "🐊", "🐅",
        "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏", "🦒",
        "🚀", "🛸", "💎", "🛡️", "🔑", "⚡", "🔥", "🪐"
    )

    val myKeyPair: Pair<ByteArray, ByteArray> = try {
        loadOrGenerateKeys(databasePath)
    } catch (e: Exception) {
        println("CryptoLayer: Failed to load keys at startup: ${e.message}")
        e.printStackTrace()
        Pair(fallbackKey, fallbackKey)
    }

    private val activeSecureChats = mutableMapOf<Long, ByteArray>()

    // 💥 ЗАПОМИНАЕМ ПУБЛИЧНЫЕ КЛЮЧИ СОБЕСЕДНИКОВ (ДЛЯ ЗАЩИТЫ ОТ ПОДМЕНЫ):
    private val pinnedPeerKeys = mutableMapOf<Long, String>()

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

    // 💥 3. УМНЫЙ ОБМЕН КЛЮЧАМИ С ПРОВЕРКОЙ НА АТАКУ «ЧЕЛОВЕК ПОСЕРЕДИНЕ» (MITM):
    fun verifyAndEstablishSecret(chatId: Long, otherPublicKeyHex: String): KeyVerificationStatus {
        val cleanPeerKey = otherPublicKeyHex.trim().lowercase()
        val previousPinnedKey = pinnedPeerKeys[chatId]

        return when {
            // Ключ внезапно изменился без сброса — атака!
            previousPinnedKey != null && previousPinnedKey != cleanPeerKey -> {
                println("🚨 [MITM ALERT] Внимание! Ключ чата $chatId изменился!")
                KeyVerificationStatus.MITM_WARNING
            }
            // Первый обмен
            previousPinnedKey == null -> {
                pinnedPeerKeys[chatId] = cleanPeerKey
                establishSecretInternal(chatId, cleanPeerKey)
                KeyVerificationStatus.NEW_KEY
            }
            // Ключ совпал с сохраненным
            else -> {
                establishSecretInternal(chatId, cleanPeerKey)
                KeyVerificationStatus.TRUSTED
            }
        }
    }

    // Оставляем старый метод для обратной совместимости
    fun establishSecret(chatId: Long, otherPublicKeyHex: String) {
        verifyAndEstablishSecret(chatId, otherPublicKeyHex)
    }

    private fun establishSecretInternal(chatId: Long, otherPublicKeyHex: String) {
        try {
            val otherPubKey = otherPublicKeyHex.decodeHex()
            val sharedSecret = cryptoEngine.generateSharedSecret(myKeyPair.first, otherPubKey)
            activeSecureChats[chatId] = sharedSecret
            println("🔐 E2EE УСТАНОВЛЕН ДЛЯ ЧАТА $chatId!")
        } catch (e: Exception) {
            println("❌ E2EE ОШИБКА: Неверный публичный ключ")
        }
    }

    // 💥 4. ВЫЧИСЛЕНИЕ 4 ЭМОДЗИ БЕЗОПАСНОСТИ (SAFETY FINGERPRINT):
    fun getSafetyFingerprint(chatId: Long): SafetyFingerprint? {
        val peerKeyHex = pinnedPeerKeys[chatId] ?: return null
        val myKeyHex = myKeyPair.second.toHex().lowercase()

        // Детерминированная сортировка: и у тебя, и у друга хэш будет идентичным!
        val combined = if (myKeyHex < peerKeyHex) myKeyHex + peerKeyHex else peerKeyHex + myKeyHex

        // Считаем SHA-256 через Okio
        val hash = combined.encodeUtf8().sha256()
        val hashBytes = hash.toByteArray()

        // Достаем 4 эмодзи
        val emojiList = List(4) { i ->
            val byteVal = hashBytes[i].toInt() and 0xFF
            FINGERPRINT_EMOJIS[byteVal % FINGERPRINT_EMOJIS.size]
        }
        val formattedHex = hash.hex().chunked(4).joinToString(" ")

        return SafetyFingerprint(
            emojis = emojiList,
            formattedHex = formattedHex
        )
    }

    fun encryptAndHide(chatId: Long, text: String): String {
        val key = activeSecureChats[chatId] ?: return text
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