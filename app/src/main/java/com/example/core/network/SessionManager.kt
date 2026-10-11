package com.example.core.network

import com.example.backend.model.UserProfileDto
import com.example.core.security.SecureTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(
        val user: UserProfileDto,
        val token: String
    ) : AuthState()
    object Unauthenticated : AuthState()
    data class AccountSuspended(val message: String) : AuthState()
    data class PasswordChangeRequired(val user: UserProfileDto) : AuthState()
    data class Error(val message: String, val statusCode: Int? = null) : AuthState()
}

/**
 * In-memory authentication state.
 *
 * Stored session metadata is only a hint for locating a token; it is never proof
 * of authentication. Only a successful login response or a server-validated
 * /v1/auth/me response may call setAuthenticated().
 */
class SessionManager(private val secureStorage: SecureTokenStorage) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Deliberately does not restore AuthState from local preferences.
     * The repository must validate any persisted access token with the backend
     * before an authenticated state can be established.
     */
    private fun restoreSession() {
        _authState.value = AuthState.Unauthenticated
    }

    fun setAuthenticated(user: UserProfileDto, accessToken: String, refreshToken: String) {
        // Call only after a successful login or successful server profile validation.
        secureStorage.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            loginId = user.loginId,
            role = user.role,
            fullName = user.fullName
        )
        if (user.status != "ACTIVE") {
            secureStorage.clearSession()
            _authState.value = AuthState.Unauthenticated
        } else if (user.mustChangePassword) {
            _authState.value = AuthState.PasswordChangeRequired(user)
        } else {
            _authState.value = AuthState.Authenticated(user, accessToken)
        }
    }

    fun setSuspended(message: String) {
        secureStorage.clearSession()
        _authState.value = AuthState.AccountSuspended(message)
    }

    fun setError(message: String, statusCode: Int? = null) {
        _authState.value = AuthState.Error(message, statusCode)
    }

    fun clearSession() {
        secureStorage.clearSession()
        _authState.value = AuthState.Unauthenticated
    }
}
