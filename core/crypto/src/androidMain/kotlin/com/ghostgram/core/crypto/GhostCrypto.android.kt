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

    /**
     * Generates an EC keypair (NIST P-256) for ECDH key agreement.
     * Returns Pair(PrivateKey.encoded, PublicKey.encoded).
     */

    actual fun generateKeyPair(): Pair<ByteArray, ByteArray> {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val keyPair = kpg.generateKeyPair()
        return Pair(keyPair.private.encoded, keyPair.public.encoded)
    }

    /**
     * Derives a symmetric 256-bit AES key using ECDH shared secret hashed with SHA-256.
     */
    actual fun generateSharedSecret(myPrivateKey: ByteArray, otherPublicKey: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("EC")
        val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(myPrivateKey))
        val publicKey = kf.generatePublic(X509EncodedKeySpec(otherPublicKey))
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        val rawSecret = keyAgreement.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawSecret)
    }

    actual fun encryptAES(key: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val secretKeySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)
        val cipherText = cipher.doFinal(plaintext)

        // Prepend IV to ciphertext for recipient decryption
        return iv + cipherText
    }

    actual fun decryptAES(key: ByteArray, ciphertext: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ciphertext.copyOfRange(0, 12)
            val actualCipherText = ciphertext.copyOfRange(12, ciphertext.size)

            val secretKeySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)
            cipher.doFinal(actualCipherText)
        } catch (e: Exception) {
            // AEAD authentication failed (tampered ciphertext or invalid key)
            null
        }
    }
}