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
 * In-memory authentication state. Local preferences are never proof of identity.
 * Only a successful login response or server-validated /v1/auth/me may authenticate.
 */
class SessionManager(private val secureStorage: SecureTokenStorage) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun setAuthenticated(user: UserProfileDto, accessToken: String, refreshToken: String) {
        // This method must only be called after successful server authentication/validation.
        if (user.status != "ACTIVE" || user.role !in setOf("ADMIN", "AGENT", "USER")) {
            secureStorage.clearSession()
            _authState.value = AuthState.Unauthenticated
            return
        }

        secureStorage.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            loginId = user.loginId,
            role = user.role,
            fullName = user.fullName
        )
        _authState.value = if (user.mustChangePassword) {
            AuthState.PasswordChangeRequired(user)
        } else {
            AuthState.Authenticated(user, accessToken)
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
