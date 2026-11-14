package com.ghostgram.core.crypto

import entity.SafetyFingerprint
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

fun ByteArray.toHex(): String = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
fun String.decodeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

enum class KeyVerificationStatus {
    TRUSTED,      // Ключ подтвержден
    NEW_KEY,      // Первый обмен ключами
    MITM_WARNING  // 🚨 ТРЕВОГА! Ключ подменен хакером!
}

class CryptoLayer(databasePath: String) {

    private val fs = FileSystem.SYSTEM
    private val dir = databasePath.toPath()
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
        loadOrGenerateKeys()
    } catch (e: Exception) {
        println("CryptoLayer: Failed to load keys at startup: ${e.message}")
        Pair(fallbackKey, fallbackKey)
    }

    private val chatKeyPairs = mutableMapOf<Long, Pair<ByteArray, ByteArray>>()
    private val activeSecureChats = mutableMapOf<Long, ByteArray>()
    private val pinnedPeerKeys = mutableMapOf<Long, String>()

    // 💥 Теперь проверяет и в памяти, и на диске!
    fun isChatSecure(chatId: Long): Boolean = getSecretKey(chatId) != null

    private fun loadOrGenerateKeys(): Pair<ByteArray, ByteArray> {
        val privFile = dir / "ghost_priv.key"
        val pubFile = dir / "ghost_pub.key"
        if (!fs.exists(dir)) fs.createDirectories(dir)

        if (fs.exists(privFile) && fs.exists(pubFile)) {
            return Pair(fs.read(privFile) { readByteArray() }, fs.read(pubFile) { readByteArray() })
        }

        val newKeys = cryptoEngine.generateKeyPair()
        fs.write(privFile) { write(newKeys.first) }
        fs.write(pubFile) { write(newKeys.second) }
        return newKeys
    }

    // -------------------------------------------------------------------------
    // 1. ЭФЕМЕРНЫЕ КЛЮЧИ КОНКРЕТНОГО ЧАТА (Forward Secrecy)
    // -------------------------------------------------------------------------
    fun getOrGenerateChatKeyPair(chatId: Long): Pair<ByteArray, ByteArray> {
        chatKeyPairs[chatId]?.let { return it }

        val privFile = dir / "chat_priv_${chatId}.key"
        val pubFile = dir / "chat_pub_${chatId}.pub"

        if (fs.exists(privFile) && fs.exists(pubFile)) {
            val pair = Pair(fs.read(privFile) { readByteArray() }, fs.read(pubFile) { readByteArray() })
            chatKeyPairs[chatId] = pair
            return pair
        }

        return generateFreshChatKeyPair(chatId)
    }

    fun generateFreshChatKeyPair(chatId: Long): Pair<ByteArray, ByteArray> {
        val newPair = cryptoEngine.generateKeyPair()
        chatKeyPairs[chatId] = newPair
        fs.write(dir / "chat_priv_${chatId}.key") { write(newPair.first) }
        fs.write(dir / "chat_pub_${chatId}.pub") { write(newPair.second) }
        println("🎲 [E2EE] Сгенерированы новые эфемерные ключи для чата $chatId")
        return newPair
    }

    fun shouldProcessReset(chatId: Long, resetMsgId: Long): Boolean {
        val file = dir / "last_reset_${chatId}.txt"
        val lastId = if (fs.exists(file)) {
            fs.read(file) { readUtf8() }.trim().toLongOrNull() ?: 0L
        } else 0L

        if (resetMsgId <= lastId) return false
        fs.write(file) { writeUtf8(resetMsgId.toString()) }
        return true
    }

    // -------------------------------------------------------------------------
    // 2. ОБМЕН КЛЮЧАМИ И ЗАЩИТА ОТ MITM
    // -------------------------------------------------------------------------
    fun verifyAndEstablishSecret(chatId: Long, otherPublicKeyHex: String): KeyVerificationStatus {
        val cleanPeerKey = otherPublicKeyHex.trim().lowercase()
        val previousPinnedKey = getPinnedPeerKey(chatId) // 💥 Читаем с диска!

        return when {
            previousPinnedKey != null && previousPinnedKey != cleanPeerKey -> {
                println("🚨 [MITM ALERT] Внимание! Ключ чата $chatId изменился!")
                KeyVerificationStatus.MITM_WARNING
            }
            previousPinnedKey == null -> {
                establishSecretInternal(chatId, cleanPeerKey)
                KeyVerificationStatus.NEW_KEY
            }
            else -> {
                establishSecretInternal(chatId, cleanPeerKey)
                KeyVerificationStatus.TRUSTED
            }
        }
    }

    fun establishSecret(chatId: Long, otherPublicKeyHex: String) {
        verifyAndEstablishSecret(chatId, otherPublicKeyHex)
    }

    private fun establishSecretInternal(chatId: Long, otherPublicKeyHex: String) {
        try {
            val otherPubKey = otherPublicKeyHex.decodeHex()
            val (myChatPriv, _) = getOrGenerateChatKeyPair(chatId)
            val sharedSecret = cryptoEngine.generateSharedSecret(myChatPriv, otherPubKey)

            activeSecureChats[chatId] = sharedSecret
            pinnedPeerKeys[chatId] = otherPublicKeyHex

            fs.write(dir / "sec_${chatId}.key") { write(sharedSecret) }
            fs.write(dir / "peer_${chatId}.pub") { writeUtf8(otherPublicKeyHex) }
            println("🔐 [E2EE] Секретный ключ установлен и сохранен на диск для чата $chatId!")
        } catch (e: Exception) {
            println("❌ E2EE ОШИБКА: Неверный публичный ключ")
        }
    }

    // -------------------------------------------------------------------------
    // 3. ЧТЕНИЕ КЛЮЧЕЙ С ДИСКА И СБРОС СЕАНСА
    // -------------------------------------------------------------------------
    private fun getSecretKey(chatId: Long): ByteArray? {
        activeSecureChats[chatId]?.let { return it }
        val file = dir / "sec_${chatId}.key"
        if (fs.exists(file)) {
            val key = fs.read(file) { readByteArray() }
            activeSecureChats[chatId] = key
            return key
        }
        return null
    }

    private fun getPinnedPeerKey(chatId: Long): String? {
        pinnedPeerKeys[chatId]?.let { return it }
        val file = dir / "peer_${chatId}.pub"
        if (fs.exists(file)) {
            val pub = fs.read(file) { readUtf8() }.trim()
            pinnedPeerKeys[chatId] = pub
            return pub
        }
        return null
    }

    fun getSafetyFingerprint(chatId: Long): SafetyFingerprint? {
        val secretKey = getSecretKey(chatId) ?: return null
        val hash = secretKey.toByteString().sha256()
        val hashBytes = hash.toByteArray()

        val emojiList = List(4) { i ->
            val byteVal = hashBytes[i].toInt() and 0xFF
            FINGERPRINT_EMOJIS[byteVal % FINGERPRINT_EMOJIS.size]
        }
        val formattedHex = hash.hex().chunked(4).joinToString(" ")

        return SafetyFingerprint(emojis = emojiList, formattedHex = formattedHex)
    }

    fun resetChatSecurity(chatId: Long) {
        activeSecureChats.remove(chatId)
        pinnedPeerKeys.remove(chatId)
        chatKeyPairs.remove(chatId)

        listOf("sec_${chatId}.key", "peer_${chatId}.pub", "chat_priv_${chatId}.key", "chat_pub_${chatId}.pub").forEach { name ->
            val file = dir / name
            if (fs.exists(file)) runCatching { fs.delete(file) }
        }
        println("🔥 [E2EE RESET] Все ключи чата $chatId уничтожены с диска!")
    }

    // -------------------------------------------------------------------------
    // 4. НЕВИДИМАЯ СТЕГАНОГРАФИЯ (ZERO-WIDTH)
    // -------------------------------------------------------------------------
    fun encryptAndHide(chatId: Long, text: String): String {
        val key = getSecretKey(chatId) ?: return text // 💥 Берем ключ с диска!
        val rawBytes = text.encodeToByteArray()
        val encryptedBytes = cryptoEngine.encryptAES(key, rawBytes)

        // 💥 Никакой рекламы и слов "гриб киоск"! Только невидимый шифр внутри обычной фразы!
        return ZeroWidthSteganography.hide(encryptedBytes)
    }

    fun revealAndDecrypt(chatId: Long, hiddenText: String): String? {
        val key = getSecretKey(chatId) ?: return null // 💥 Берем ключ с диска!

        // 💥 Достаем невидимые байты (и поддержка старых сообщений через WordCoder, если остались)
        val encryptedBytes = ZeroWidthSteganography.reveal(hiddenText)
            ?: WordCoder.decode(hiddenText)
            ?: return null

        val decryptedBytes = cryptoEngine.decryptAES(key, encryptedBytes) ?: return null
        return decryptedBytes.decodeToString()
    }
}