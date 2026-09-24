package com.example.backend.security

import com.example.backend.database.dao.RateLimitDao
import com.example.backend.database.entity.RateLimitEntity

class RateLimiter(private val rateLimitDao: RateLimitDao) {

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val WINDOW_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes lockout
    }

    /**
     * Checks whether the given key (e.g. "login:loginId" or "ip:xxx") is currently locked out.
     * Returns true if locked (too many requests), false if allowed.
     */
    suspend fun isLocked(key: String, now: Long = System.currentTimeMillis()): Boolean {
        val record = rateLimitDao.findByKey(key) ?: return false
        val lockedUntil = record.lockedUntil
        if (lockedUntil != null && lockedUntil > now) {
            return true
        }
        return false
    }

    /**
     * Records a failed attempt for the key. If threshold exceeded, triggers temporary lockout.
     */
    suspend fun recordFailure(key: String, now: Long = System.currentTimeMillis()): Boolean {
        val existing = rateLimitDao.findByKey(key)
        if (existing == null || now - existing.windowStart > WINDOW_DURATION_MS) {
            rateLimitDao.save(
                RateLimitEntity(
                    rateKey = key,
                    attemptCount = 1,
                    windowStart = now,
                    lockedUntil = null
                )
            )
            return false
        } else {
            val newCount = existing.attemptCount + 1
            val lockUntil = if (newCount >= MAX_FAILED_ATTEMPTS) now + LOCKOUT_DURATION_MS else null
            rateLimitDao.save(
                RateLimitEntity(
                    rateKey = key,
                    attemptCount = newCount,
                    windowStart = existing.windowStart,
                    lockedUntil = lockUntil
                )
            )
            return lockUntil != null
        }
    }

    /**
     * Clears rate limit state upon successful authentication.
     */
    suspend fun recordSuccess(key: String) {
        rateLimitDao.delete(key)
    }

    /**
     * Generic rate limiter for high-value administrative endpoints.
     * Returns true if request limit exceeded within sliding window, false if allowed.
     */
    suspend fun isActionRateLimited(
        key: String,
        maxRequests: Int = 10,
        windowMs: Long = 60_000L,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val record = rateLimitDao.findByKey(key)
        if (record == null || now - record.windowStart > windowMs) {
            rateLimitDao.save(
                RateLimitEntity(
                    rateKey = key,
                    attemptCount = 1,
                    windowStart = now,
                    lockedUntil = null
                )
            )
            return false
        } else {
            val newCount = record.attemptCount + 1
            if (newCount > maxRequests) {
                val lockout = now + windowMs
                rateLimitDao.save(
                    RateLimitEntity(
                        rateKey = key,
                        attemptCount = newCount,
                        windowStart = record.windowStart,
                        lockedUntil = lockout
                    )
                )
                return true
            } else {
                rateLimitDao.save(
                    RateLimitEntity(
                        rateKey = key,
                        attemptCount = newCount,
                        windowStart = record.windowStart,
                        lockedUntil = null
                    )
                )
                return false
            }
        }
    }
}
