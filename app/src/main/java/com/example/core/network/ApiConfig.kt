package com.example.core.network

import com.example.BuildConfig

/**
 * Target server deployment environments.
 */
enum class AppEnvironment(val envName: String) {
    DEVELOPMENT("development"),
    STAGING("staging"),
    PRODUCTION("production");

    companion object {
        fun fromString(value: String?): AppEnvironment {
            return entries.find { it.envName.equals(value, ignoreCase = true) } ?: DEVELOPMENT
        }
    }
}

/**
 * Centralized, environment-aware API and server configuration.
 *
 * Guarantees strict environment isolation between DEVELOPMENT, STAGING, and PRODUCTION.
 * Never allows a production build to inadvertently connect to development infrastructure.
 */
object ApiConfig {

    /**
     * Active environment resolved from build flavor / BuildConfig or default.
     */
    val currentEnvironment: AppEnvironment by lazy {
        if (BuildConfig.DEBUG) {
            AppEnvironment.DEVELOPMENT
        } else {
            // When release signing / release build is engaged, check if explicitly configured as staging or production
            AppEnvironment.PRODUCTION
        }
    }

    /**
     * Resolves the authoritative base API URL for the current runtime environment.
     */
    fun getBaseUrl(): String {
        return when (currentEnvironment) {
            AppEnvironment.DEVELOPMENT -> "https://dev-api.mahakal.internal/v1"
            AppEnvironment.STAGING -> "https://staging-api.mahakal.internal/v1"
            AppEnvironment.PRODUCTION -> "https://api.mahakal.internal/v1"
        }
    }

    /**
     * Resolves the appropriate database name for local caching / persistence per environment.
     */
    fun getDatabaseName(): String {
        return when (currentEnvironment) {
            AppEnvironment.DEVELOPMENT -> "mahakal_server_ledger_dev.db"
            AppEnvironment.STAGING -> "mahakal_server_ledger_staging.db"
            AppEnvironment.PRODUCTION -> "mahakal_server_ledger.db"
        }
    }

    /**
     * Whether verbose logging and non-production diagnostics are allowed.
     */
    fun isDiagnosticsEnabled(): Boolean {
        return currentEnvironment != AppEnvironment.PRODUCTION
    }

    /**
     * API request timeout in milliseconds per environment.
     */
    val connectTimeoutMs: Long = 15_000L
    val readTimeoutMs: Long = 30_000L

    /**
     * Production Cryptographic Secrets and Database Configuration injected via Secrets Gradle Plugin at build/runtime.
     */
    val databaseUrl: String
        get() = BuildConfig.DATABASE_URL

    val sessionSecret: String
        get() = BuildConfig.SESSION_SECRET

    val tokenSigningSecret: String
        get() = BuildConfig.TOKEN_SIGNING_SECRET

    val encryptionKey: String
        get() = BuildConfig.ENCRYPTION_KEY
}
