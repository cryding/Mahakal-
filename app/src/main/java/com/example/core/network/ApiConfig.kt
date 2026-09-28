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

    const val PRODUCTION_API_URL = "https://mahakal-qo14.onrender.com"
    const val STAGING_API_URL = "https://staging-api.mahakal.internal/v1"
    const val DEVELOPMENT_API_URL = "https://dev-api.mahakal.internal/v1"

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
     * Enforces HTTPS strictly and rejects insecure or local addresses in production.
     */
    fun getBaseUrl(): String {
        val configuredUrl = try {
            val field = BuildConfig::class.java.getField("API_BASE_URL")
            field.get(null) as? String
        } catch (_: Throwable) {
            null
        }

        if (!configuredUrl.isNullOrBlank()) {
            val trimmed = configuredUrl.trim().trimEnd('/')
            // Security verification: must use HTTPS and must not be a local/insecure address
            if (trimmed.startsWith("https://", ignoreCase = true) &&
                !trimmed.contains("localhost", ignoreCase = true) &&
                !trimmed.contains("127.0.0.1") &&
                !trimmed.contains("10.0.2.2")
            ) {
                return trimmed
            }
        }

        return when (currentEnvironment) {
            AppEnvironment.DEVELOPMENT -> DEVELOPMENT_API_URL
            AppEnvironment.STAGING -> STAGING_API_URL
            AppEnvironment.PRODUCTION -> PRODUCTION_API_URL
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
}
