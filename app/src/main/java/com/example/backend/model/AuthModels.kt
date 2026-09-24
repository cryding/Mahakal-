package com.example.backend.model

data class LoginRequest(
    val loginId: String,
    val password: String
)

data class UserProfileDto(
    val id: String,
    val loginId: String,
    val role: String,
    val status: String,
    val fullName: String,
    val mustChangePassword: Boolean
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserProfileDto
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ServerResponse<T>(
    val success: Boolean,
    val statusCode: Int,
    val data: T? = null,
    val errorCode: String? = null,
    val message: String? = null,
    val requestId: String = ""
)
