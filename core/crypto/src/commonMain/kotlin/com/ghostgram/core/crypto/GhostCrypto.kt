package com.ghostgram.core.crypto

expect class GhostCrypto() {

    /**
     * Генерирует пару ключей (Приватный, Публичный) по эллиптической кривой SECP256R1.
     */
    fun generateKeyPair(): Pair<ByteArray, ByteArray>

    /**
     * Математическая магия ECDH: вычисляет ОБЩИЙ СЕКРЕТНЫЙ КЛЮЧ (AES-256)
     * на основе ТВОЕГО приватного ключа и ПУБЛИЧНОГО ключа собеседника.
     */
    fun generateSharedSecret(myPrivateKey: ByteArray, otherPublicKey: ByteArray): ByteArray

    /**
     * Шифрует текст алгоритмом AES-256-GCM.
     * GCM гарантирует, что сообщение никто не изменил в пути (встроенная проверка целостности).
     */
    fun encryptAES(key: ByteArray, plaintext: ByteArray): ByteArray

    /**
     * Расшифровывает байты. Вернет null, если ключ неверный или данные повредили.
     */
    fun decryptAES(key: ByteArray, ciphertext: ByteArray): ByteArray?
}