package com.example.backend.security

import java.security.SecureRandom
import java.util.UUID

/**
 * Token generation and metadata provider.
 */
object TokenProvider {

    private val secureRandom = SecureRandom()

    // Access tokens valid for 1 hour (3600 seconds)
    const val ACCESS_TOKEN_VALIDITY_MS: Long = 3600 * 1000L

    // Refresh tokens valid for 7 days
    const val REFRESH_TOKEN_VALIDITY_MS: Long = 7 * 24 * 3600 * 1000L

    fun generateAccessToken(): String {
        return "mhk_acc_" + UUID.randomUUID().toString().replace("-", "") + generateRandomEntropy()
    }

    fun generateRefreshToken(): String {
        return "mhk_ref_" + UUID.randomUUID().toString().replace("-", "") + generateRandomEntropy()
    }

    private fun generateRandomEntropy(): String {
        val bytes = ByteArray(12)
        secureRandom.nextBytes(bytes)
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
