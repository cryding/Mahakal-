package com.example.data.repository

import com.example.backend.api.ServerApiRouter
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.EditUserRequest
import com.example.backend.model.PaginatedUserListResponse
import com.example.backend.model.ResetPasswordResponse
import com.example.backend.model.ResetUserPasswordRequest
import com.example.backend.model.ServerResponse
import com.example.backend.model.UserCreatedDto
import com.example.backend.model.UserDetailsDto
import com.example.backend.model.UserSummaryDto
import com.example.core.security.SecureTokenStorage

interface UserRepository {
    suspend fun createUser(request: CreateUserRequest): ServerResponse<UserCreatedDto>
    suspend fun getUsers(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        search: String? = null
    ): ServerResponse<PaginatedUserListResponse>
    suspend fun getUserDetails(userId: String): ServerResponse<UserDetailsDto>
    suspend fun updateUser(userId: String, request: EditUserRequest): ServerResponse<UserSummaryDto>
    suspend fun suspendUser(userId: String): ServerResponse<Unit>
    suspend fun activateUser(userId: String): ServerResponse<Unit>
    suspend fun resetPassword(userId: String, request: ResetUserPasswordRequest): ServerResponse<ResetPasswordResponse>

    // Admin Supervisory
    suspend fun adminGetUsers(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        search: String? = null
    ): ServerResponse<PaginatedUserListResponse>
    suspend fun adminGetUserDetails(userId: String): ServerResponse<UserDetailsDto>
    suspend fun adminUpdateUser(userId: String, request: EditUserRequest): ServerResponse<UserSummaryDto>
    suspend fun adminSuspendUser(userId: String): ServerResponse<Unit>
    suspend fun adminActivateUser(userId: String): ServerResponse<Unit>
    suspend fun adminResetPassword(userId: String, request: ResetUserPasswordRequest): ServerResponse<ResetPasswordResponse>
}

class UserRepositoryImpl(
    private val apiRouter: ServerApiRouter,
    private val secureStorage: SecureTokenStorage
) : UserRepository {

    private fun getAuthHeader(): String? {
        val token = secureStorage.getAccessToken() ?: return null
        return "Bearer $token"
    }

    override suspend fun createUser(request: CreateUserRequest): ServerResponse<UserCreatedDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleCreateUser(authHeader, request)
    }

    override suspend fun getUsers(
        page: Int,
        limit: Int,
        status: String?,
        search: String?
    ): ServerResponse<PaginatedUserListResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetUsers(authHeader, page, limit, status, search)
    }

    override suspend fun getUserDetails(userId: String): ServerResponse<UserDetailsDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetUserDetails(authHeader, userId)
    }

    override suspend fun updateUser(userId: String, request: EditUserRequest): ServerResponse<UserSummaryDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleUpdateUser(authHeader, userId, request)
    }

    override suspend fun suspendUser(userId: String): ServerResponse<Unit> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleSuspendUser(authHeader, userId)
    }

    override suspend fun activateUser(userId: String): ServerResponse<Unit> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleActivateUser(authHeader, userId)
    }

    override suspend fun resetPassword(
        userId: String,
        request: ResetUserPasswordRequest
    ): ServerResponse<ResetPasswordResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleResetUserPassword(authHeader, userId, request)
    }

    override suspend fun adminGetUsers(
        page: Int,
        limit: Int,
        status: String?,
        search: String?
    ): ServerResponse<PaginatedUserListResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleAdminGetUsers(authHeader, page, limit, status, search)
    }

    override suspend fun adminGetUserDetails(userId: String): ServerResponse<UserDetailsDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleAdminGetUserDetails(authHeader, userId)
    }

    override suspend fun adminUpdateUser(userId: String, request: EditUserRequest): ServerResponse<UserSummaryDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleAdminUpdateUser(authHeader, userId, request)
    }

    override suspend fun adminSuspendUser(userId: String): ServerResponse<Unit> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleAdminSuspendUser(authHeader, userId)
    }

    override suspend fun adminActivateUser(userId: String): ServerResponse<Unit> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleAdminActivateUser(authHeader, userId)
    }

    override suspend fun adminResetPassword(
        userId: String,
        request: ResetUserPasswordRequest
    ): ServerResponse<ResetPasswordResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleAdminResetUserPassword(authHeader, userId, request)
    }

    private fun <T> unauthorized(): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 401,
            data = null,
            errorCode = "UNAUTHORIZED",
            message = "Authentication required."
        )
    }
}
