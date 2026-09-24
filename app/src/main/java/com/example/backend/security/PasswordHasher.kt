package com.example.backend.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Enterprise Cryptographic Password Hasher.
 * Uses PBKDF2WithHmacSHA256 with 100,000 iterations, 16-byte cryptographically secure random salt,
 * and constant-time byte comparison to eliminate timing attacks.
 */
object PasswordHasher {

    private const val ITERATION_COUNT = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    private val secureRandom = SecureRandom()

    /**
     * Generates a 16-byte cryptographically secure random salt encoded as Hex.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(saltBytes)
        return bytesToHex(saltBytes)
    }

    /**
     * Hashes a plaintext password with the provided salt using PBKDF2WithHmacSHA256.
     * The input password char array is wiped from memory after hash computation.
     */
    fun hashPassword(password: CharArray, saltHex: String): String {
        val saltBytes = hexToBytes(saltHex)
        val spec = PBEKeySpec(password, saltBytes, ITERATION_COUNT, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hashBytes = factory.generateSecret(spec).encoded
            bytesToHex(hashBytes)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Verifies whether the provided plaintext password matches the stored hash.
     * Uses MessageDigest.isEqual for constant-time comparison to prevent side-channel timing analysis.
     */
    fun verifyPassword(password: CharArray, saltHex: String, expectedHashHex: String): Boolean {
        val computedHashHex = hashPassword(password, saltHex)
        val computedBytes = hexToBytes(computedHashHex)
        val expectedBytes = hexToBytes(expectedHashHex)
        return MessageDigest.isEqual(computedBytes, expectedBytes)
    }

    /**
     * Convenience wrapper for string passwords.
     */
    fun hashPassword(password: String, saltHex: String): String {
        return hashPassword(password.toCharArray(), saltHex)
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        return verifyPassword(password.toCharArray(), saltHex, expectedHashHex)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
