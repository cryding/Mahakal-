package com.example

import com.example.backend.security.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun salt_generation_is_unique() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()
        assertNotEquals(salt1, salt2)
        assertTrue(salt1.length >= 32)
    }

    @Test
    fun password_hashing_and_verification_succeeds() {
        val password = "SecureAdminPassword!#2026"
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)

        // Valid verification
        assertTrue(PasswordHasher.verifyPassword(password, salt, hash))

        // Invalid verification
        assertFalse(PasswordHasher.verifyPassword("WrongPassword!#2026", salt, hash))
        assertFalse(PasswordHasher.verifyPassword(password, PasswordHasher.generateSalt(), hash))
    }

    @Test
    fun same_password_with_different_salts_produces_distinct_hashes() {
        val password = "CommonPassword123"
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()

        val hash1 = PasswordHasher.hashPassword(password, salt1)
        val hash2 = PasswordHasher.hashPassword(password, salt2)

        assertNotEquals(hash1, hash2)
    }
}
