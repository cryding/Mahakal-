package com.example.backend.model

data class CreateUserRequest(
    val userId: String,
    val displayName: String,
    val temporaryPassword: String,
    val notes: String? = null
)

data class UserCreatedDto(
    val id: String,
    val userId: String,
    val displayName: String,
    val agentId: String,
    val role: String = "USER",
    val status: String = "ACTIVE",
    val createdAt: Long,
    val notes: String? = null,
    val temporaryPassword: String? = null
)

data class EditUserRequest(
    val displayName: String? = null,
    val notes: String? = null
)

data class ResetUserPasswordRequest(
    val temporaryPassword: String? = null
)

data class UserSummaryDto(
    val id: String,
    val userId: String,
    val displayName: String,
    val agentId: String,
    val status: String,
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val notes: String? = null
)

data class UserActivityDto(
    val id: String,
    val action: String,
    val actorRole: String,
    val createdAt: Long,
    val metadata: String
)

data class UserDetailsDto(
    val id: String,
    val userId: String,
    val displayName: String,
    val agentId: String,
    val agentName: String? = null,
    val role: String = "USER",
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastLoginAt: Long? = null,
    val mustChangePassword: Boolean = false,
    val virtualCoinBalance: Long = 0L,
    val notes: String? = null,
    val recentActivity: List<UserActivityDto> = emptyList()
)

data class PaginatedUserListResponse(
    val users: List<UserSummaryDto>,
    val totalCount: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)
