package com.example.data.repository

import com.example.backend.api.ServerApiRouter
import com.example.backend.model.AgentCreatedDto
import com.example.backend.model.AgentDetailsDto
import com.example.backend.model.AgentSummaryDto
import com.example.backend.model.CreateAgentRequest
import com.example.backend.model.EditAgentRequest
import com.example.backend.model.PaginatedAgentListResponse
import com.example.backend.model.ResetAgentPasswordRequest
import com.example.backend.model.ResetPasswordResponse
import com.example.backend.model.ServerResponse
import com.example.core.security.SecureTokenStorage

interface AgentRepository {
    suspend fun createAgent(request: CreateAgentRequest): ServerResponse<AgentCreatedDto>
    suspend fun getAgents(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        search: String? = null
    ): ServerResponse<PaginatedAgentListResponse>
    suspend fun getAgentDetails(agentId: String): ServerResponse<AgentDetailsDto>
    suspend fun updateAgent(agentId: String, request: EditAgentRequest): ServerResponse<AgentSummaryDto>
    suspend fun suspendAgent(agentId: String): ServerResponse<Unit>
    suspend fun activateAgent(agentId: String): ServerResponse<Unit>
    suspend fun resetPassword(agentId: String, request: ResetAgentPasswordRequest): ServerResponse<ResetPasswordResponse>
}

class AgentRepositoryImpl(
    private val apiRouter: ServerApiRouter,
    private val secureStorage: SecureTokenStorage
) : AgentRepository {

    private fun getAuthHeader(): String? {
        val token = secureStorage.getAccessToken() ?: return null
        return "Bearer $token"
    }

    override suspend fun createAgent(request: CreateAgentRequest): ServerResponse<AgentCreatedDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleCreateAgent(authHeader, request)
    }

    override suspend fun getAgents(
        page: Int,
        limit: Int,
        status: String?,
        search: String?
    ): ServerResponse<PaginatedAgentListResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAgents(authHeader, page, limit, status, search)
    }

    override suspend fun getAgentDetails(agentId: String): ServerResponse<AgentDetailsDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAgentDetails(authHeader, agentId)
    }

    override suspend fun updateAgent(agentId: String, request: EditAgentRequest): ServerResponse<AgentSummaryDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleUpdateAgent(authHeader, agentId, request)
    }

    override suspend fun suspendAgent(agentId: String): ServerResponse<Unit> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleSuspendAgent(authHeader, agentId)
    }

    override suspend fun activateAgent(agentId: String): ServerResponse<Unit> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleActivateAgent(authHeader, agentId)
    }

    override suspend fun resetPassword(
        agentId: String,
        request: ResetAgentPasswordRequest
    ): ServerResponse<ResetPasswordResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleResetAgentPassword(authHeader, agentId, request)
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
