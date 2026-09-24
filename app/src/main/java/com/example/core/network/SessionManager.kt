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

class SessionManager(private val secureStorage: SecureTokenStorage) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val token = secureStorage.getAccessToken()
        val userId = secureStorage.getSessionUserId()
        val loginId = secureStorage.getSessionLoginId()
        val role = secureStorage.getSessionRole()
        if (token != null && userId != null && loginId != null && role != null) {
            val fullName = secureStorage.getSessionFullName() ?: loginId
            _authState.value = AuthState.Authenticated(
                user = UserProfileDto(
                    id = userId,
                    loginId = loginId,
                    role = role,
                    status = "ACTIVE",
                    fullName = fullName,
                    mustChangePassword = false
                ),
                token = token
            )
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun setAuthenticated(user: UserProfileDto, accessToken: String, refreshToken: String) {
        secureStorage.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            loginId = user.loginId,
            role = user.role,
            fullName = user.fullName
        )
        if (user.mustChangePassword) {
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
