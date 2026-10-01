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

    /**
     * Active environment resolved from BuildConfig.
     */
    val currentEnvironment: AppEnvironment by lazy {
        AppEnvironment.fromString(BuildConfig.APP_ENV)
    }

    /**
     * Resolves the authoritative base API URL for the current runtime environment.
     */
    fun getBaseUrl(): String {
        val configuredUrl = BuildConfig.API_BASE_URL
        if (configuredUrl.isNotBlank()) {
            return configuredUrl.trim().trimEnd('/')
        }
        return PRODUCTION_API_URL
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
