package com.example.core.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {

    private val MAGIC = byteArrayOf('E'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte(), 1)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000

    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC.size) return false
        for (i in MAGIC.indices) {
            if (bytes[i] != MAGIC[i]) return false
        }
        return true
    }

    fun encrypt(plainText: String, passphrase: String): ByteArray {
        require(passphrase.length >= 4) { "Passphrase must be at least 4 characters." }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).apply { random.nextBytes(this) }
        val iv = ByteArray(IV_BYTES).apply { random.nextBytes(this) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val output = ByteArray(MAGIC.size + SALT_BYTES + IV_BYTES + cipherBytes.size)
        var offset = 0
        System.arraycopy(MAGIC, 0, output, offset, MAGIC.size)
        offset += MAGIC.size
        System.arraycopy(salt, 0, output, offset, SALT_BYTES)
        offset += SALT_BYTES
        System.arraycopy(iv, 0, output, offset, IV_BYTES)
        offset += IV_BYTES
        System.arraycopy(cipherBytes, 0, output, offset, cipherBytes.size)
        return output
    }

    fun decrypt(encryptedBytes: ByteArray, passphrase: String): String {
        val minSize = MAGIC.size + SALT_BYTES + IV_BYTES + 16
        require(encryptedBytes.size >= minSize) { "Invalid or corrupted backup file." }
        for (i in MAGIC.indices) {
            require(encryptedBytes[i] == MAGIC[i]) { "Unsupported backup file format." }
        }
        var offset = MAGIC.size
        val salt = encryptedBytes.copyOfRange(offset, offset + SALT_BYTES)
        offset += SALT_BYTES
        val iv = encryptedBytes.copyOfRange(offset, offset + IV_BYTES)
        offset += IV_BYTES
        val cipherBytes = encryptedBytes.copyOfRange(offset, encryptedBytes.size)

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val plainBytes = cipher.doFinal(cipherBytes)
        return plainBytes.toString(Charsets.UTF_8)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val encoded = factory.generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }
}
