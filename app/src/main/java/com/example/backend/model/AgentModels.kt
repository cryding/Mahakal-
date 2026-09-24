package com.example.backend.model

data class CreateAgentRequest(
    val agentId: String,
    val agentName: String,
    val temporaryPassword: String,
    val notes: String? = null
)

data class AgentCreatedDto(
    val id: String,
    val agentId: String,
    val agentName: String,
    val role: String = "AGENT",
    val status: String = "ACTIVE",
    val createdAt: Long,
    val notes: String? = null
)

data class CreateAgentResponse(
    val agent: AgentCreatedDto
)

data class EditAgentRequest(
    val agentName: String? = null,
    val notes: String? = null
)

data class ResetAgentPasswordRequest(
    val temporaryPassword: String? = null
)

data class ResetPasswordResponse(
    val temporaryPassword: String,
    val message: String
)

data class AgentSummaryDto(
    val id: String,
    val agentId: String,
    val agentName: String,
    val status: String,
    val userCount: Int = 0,
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val notes: String? = null
)

data class AgentActivityDto(
    val id: String,
    val action: String,
    val actorRole: String,
    val createdAt: Long,
    val metadata: String
)

data class AgentDetailsDto(
    val id: String,
    val agentId: String,
    val agentName: String,
    val status: String,
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val userCount: Int = 0,
    val virtualCoinBalance: Long = 0L,
    val notes: String? = null,
    val recentActivity: List<AgentActivityDto> = emptyList()
)

data class PaginatedAgentListResponse(
    val agents: List<AgentSummaryDto>,
    val totalCount: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)
