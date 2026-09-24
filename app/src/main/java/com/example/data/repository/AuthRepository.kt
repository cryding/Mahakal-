package com.example.data.repository

import com.example.backend.api.ServerApiRouter
import com.example.backend.model.ChangePasswordRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.LoginResponse
import com.example.backend.model.ServerResponse
import com.example.backend.model.UserProfileDto
import com.example.core.network.AuthState
import com.example.core.network.SessionManager
import com.example.core.security.SecureTokenStorage
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun login(loginId: String, password: String): ServerResponse<LoginResponse>
    suspend fun logout(): ServerResponse<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): ServerResponse<Unit>
    suspend fun refreshProfile(): ServerResponse<UserProfileDto>
    fun getStoredAccessToken(): String?
}

class AuthRepositoryImpl(
    private val apiRouter: ServerApiRouter,
    private val sessionManager: SessionManager,
    private val secureStorage: SecureTokenStorage
) : AuthRepository {

    override val authState: StateFlow<AuthState> = sessionManager.authState

    override suspend fun login(loginId: String, password: String): ServerResponse<LoginResponse> {
        val response = apiRouter.handleLogin(LoginRequest(loginId = loginId, password = password))
        @Suppress("UNCHECKED_CAST")
        val typedResponse = response as ServerResponse<LoginResponse>

        if (typedResponse.success && typedResponse.data != null) {
            sessionManager.setAuthenticated(
                user = typedResponse.data.user,
                accessToken = typedResponse.data.accessToken,
                refreshToken = typedResponse.data.refreshToken
            )
        } else {
            if (typedResponse.statusCode == 403 && typedResponse.errorCode == "ACCOUNT_SUSPENDED") {
                sessionManager.setSuspended(typedResponse.message ?: "Account suspended.")
            } else {
                sessionManager.setError(
                    typedResponse.message ?: "Authentication failed.",
                    typedResponse.statusCode
                )
            }
        }
        return typedResponse
    }

    override suspend fun logout(): ServerResponse<Unit> {
        val token = secureStorage.getAccessToken()
        val response = if (token != null) {
            apiRouter.handleLogout("Bearer $token")
        } else {
            ServerResponse(true, 200, Unit, null, "Logged out", "")
        }
        sessionManager.clearSession()
        return response
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): ServerResponse<Unit> {
        val token = secureStorage.getAccessToken()
            ?: return ServerResponse(false, 401, null, "UNAUTHORIZED", "Not logged in", "")
        val response = apiRouter.handleChangePassword(
            "Bearer $token",
            ChangePasswordRequest(currentPassword, newPassword)
        )
        if (response.success) {
            // Refresh profile
            refreshProfile()
        }
        return response
    }

    override suspend fun refreshProfile(): ServerResponse<UserProfileDto> {
        val token = secureStorage.getAccessToken()
            ?: return ServerResponse(false, 401, null, "UNAUTHORIZED", "Not logged in", "")
        val response = apiRouter.handleGetMe("Bearer $token")
        @Suppress("UNCHECKED_CAST")
        val typed = response as ServerResponse<UserProfileDto>
        if (typed.success && typed.data != null) {
            sessionManager.setAuthenticated(
                user = typed.data,
                accessToken = token,
                refreshToken = secureStorage.getRefreshToken() ?: ""
            )
        } else if (typed.statusCode == 401 || typed.statusCode == 403) {
            sessionManager.clearSession()
        }
        return typed
    }

    override fun getStoredAccessToken(): String? {
        return secureStorage.getAccessToken()
    }
}
