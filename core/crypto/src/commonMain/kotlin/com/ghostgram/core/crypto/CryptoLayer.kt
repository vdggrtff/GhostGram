package com.ghostgram.core.crypto

class CryptoLayer {
    private val cryptoEngine = GhostCrypto()

    // 1. Генерация ключей при старте секретного чата
    fun createKeyPair() = cryptoEngine.generateKeyPair()

    // 2. Генерация общего секрета
    fun getSharedSecret(myPriv: ByteArray, otherPub: ByteArray) =
        cryptoEngine.generateSharedSecret(myPriv, otherPub)

    /**
     * Зашифровывает текст и маскирует его в слова
     */
    fun encryptAndHide(text: String, sharedKey: ByteArray): String {
        val rawBytes = text.encodeToByteArray()
        val encryptedBytes = cryptoEngine.encryptAES(sharedKey, rawBytes)
        return WordCoder.encode(encryptedBytes)
    }

    /**
     * Достает слова, превращает в байты и расшифровывает
     */
    fun revealAndDecrypt(hiddenText: String, sharedKey: ByteArray): String? {
        val encryptedBytes = WordCoder.decode(hiddenText) ?: return null
        val decryptedBytes = cryptoEngine.decryptAES(sharedKey, encryptedBytes) ?: return null
        return decryptedBytes.decodeToString()
    }
}