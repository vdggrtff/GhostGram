package com.ghostgram.core.crypto

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual class GhostCrypto actual constructor() {

    actual fun generateKeyPair(): Pair<ByteArray, ByteArray> {
        val kpg = KeyPairGenerator.getInstance("EC")
        // Используем самую надежную и быструю кривую SECP256R1
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val keyPair = kpg.generateKeyPair()
        return Pair(keyPair.private.encoded, keyPair.public.encoded)
    }

    actual fun generateSharedSecret(myPrivateKey: ByteArray, otherPublicKey: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("EC")
        val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(myPrivateKey))
        val publicKey = kf.generatePublic(X509EncodedKeySpec(otherPublicKey))

        // Протокол Диффи-Хеллмана на эллиптических кривых (ECDH)
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        val rawSecret = keyAgreement.generateSecret()

        // Хэшируем секрет через SHA-256, чтобы получить идеальный 32-байтный (256-битный) ключ для AES
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawSecret)
    }

    actual fun encryptAES(key: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Генерируем уникальный вектор инициализации (IV) для каждого сообщения
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val secretKeySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv) // 128 бит (16 байт) - длина тега авторизации

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)
        val cipherText = cipher.doFinal(plaintext)

        // Склеиваем IV (первые 12 байт) и сам зашифрованный текст, чтобы собеседник мог расшифровать
        return iv + cipherText
    }

    actual fun decryptAES(key: ByteArray, ciphertext: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            // Достаем IV (первые 12 байт)
            val iv = ciphertext.copyOfRange(0, 12)
            // Достаем само зашифрованное сообщение
            val actualCipherText = ciphertext.copyOfRange(12, ciphertext.size)

            val secretKeySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)
            cipher.doFinal(actualCipherText)
        } catch (e: Exception) {
            // Если кто-то изменил хоть один байт или ключ неверный, GCM выбросит AEADBadTagException
            null
        }
    }
}