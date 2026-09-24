package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.model.AgentCreatedDto
import com.example.backend.model.AgentDetailsDto
import com.example.backend.model.AgentSummaryDto
import com.example.backend.model.CreateAgentRequest
import com.example.backend.model.EditAgentRequest
import com.example.backend.model.ResetAgentPasswordRequest
import com.example.data.repository.AgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentListUiState(
    val isLoading: Boolean = false,
    val agents: List<AgentSummaryDto> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val selectedFilter: String = "ALL", // ALL, ACTIVE, SUSPENDED
    val searchQuery: String = "",
    val errorMessage: String? = null
)

data class CreatedCredentials(
    val agentId: String,
    val agentName: String,
    val temporaryPassword: String
)

data class ResetCredentials(
    val agentId: String,
    val temporaryPassword: String
)

class AdminAgentViewModel(
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(AgentListUiState())
    val listState: StateFlow<AgentListUiState> = _listState.asStateFlow()

    private val _selectedAgentDetails = MutableStateFlow<AgentDetailsDto?>(null)
    val selectedAgentDetails: StateFlow<AgentDetailsDto?> = _selectedAgentDetails.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createdCredentials = MutableStateFlow<CreatedCredentials?>(null)
    val createdCredentials: StateFlow<CreatedCredentials?> = _createdCredentials.asStateFlow()

    private val _resetCredentials = MutableStateFlow<ResetCredentials?>(null)
    val resetCredentials: StateFlow<ResetCredentials?> = _resetCredentials.asStateFlow()

    init {
        loadAgents()
    }

    fun loadAgents(
        page: Int = _listState.value.currentPage,
        statusFilter: String = _listState.value.selectedFilter,
        searchQuery: String = _listState.value.searchQuery
    ) {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, errorMessage = null, selectedFilter = statusFilter, searchQuery = searchQuery) }
            val response = agentRepository.getAgents(
                page = page,
                limit = 20,
                status = if (statusFilter == "ALL") null else statusFilter,
                search = searchQuery.ifBlank { null }
            )
            if (response.success && response.data != null) {
                _listState.update {
                    it.copy(
                        isLoading = false,
                        agents = response.data.agents,
                        totalCount = response.data.totalCount,
                        currentPage = response.data.page,
                        totalPages = response.data.totalPages,
                        errorMessage = null
                    )
                }
            } else {
                _listState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to retrieve agents list."
                    )
                }
            }
        }
    }

    fun searchAgents(query: String) {
        loadAgents(page = 1, searchQuery = query)
    }

    fun setFilter(filter: String) {
        loadAgents(page = 1, statusFilter = filter)
    }

    fun createAgent(
        agentId: String,
        agentName: String,
        tempPassword: String,
        confirmPassword: String,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        val trimmedId = agentId.trim().uppercase()
        val trimmedName = agentName.trim()

        if (trimmedId.length < 3 || trimmedId.length > 20) {
            _errorMessage.value = "Agent ID must be 3 to 20 characters."
            return
        }
        if (!trimmedId.matches(Regex("^[A-Z0-9_]+$"))) {
            _errorMessage.value = "Agent ID must contain only uppercase letters, numbers, and underscores."
            return
        }
        if (trimmedName.length < 2 || trimmedName.length > 50) {
            _errorMessage.value = "Agent Name must be between 2 and 50 characters."
            return
        }
        if (tempPassword.length < 8 || !tempPassword.any { it.isDigit() } || !tempPassword.any { it.isLetter() }) {
            _errorMessage.value = "Temporary password must be at least 8 characters with letters and numbers."
            return
        }
        if (tempPassword != confirmPassword) {
            _errorMessage.value = "Temporary passwords do not match."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val response = agentRepository.createAgent(
                CreateAgentRequest(
                    agentId = trimmedId,
                    agentName = trimmedName,
                    temporaryPassword = tempPassword,
                    notes = notes?.ifBlank { null }
                )
            )

            _isSubmitting.value = false
            if (response.success && response.data != null) {
                _createdCredentials.value = CreatedCredentials(
                    agentId = response.data.agentId,
                    agentName = response.data.agentName,
                    temporaryPassword = tempPassword
                )
                _actionFeedback.value = "Agent '${response.data.agentId}' created successfully."
                loadAgents(page = 1)
                onSuccess()
            } else {
                _errorMessage.value = response.message ?: "Failed to create agent."
            }
        }
    }

    fun loadAgentDetails(agentId: String) {
        viewModelScope.launch {
            _isLoadingDetails.value = true
            val response = agentRepository.getAgentDetails(agentId)
            _isLoadingDetails.value = false
            if (response.success && response.data != null) {
                _selectedAgentDetails.value = response.data
            } else {
                _errorMessage.value = response.message ?: "Failed to load agent details."
            }
        }
    }

    fun clearSelectedAgent() {
        _selectedAgentDetails.value = null
    }

    fun updateAgent(agentId: String, name: String, notes: String?, onComplete: () -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.length < 2 || trimmedName.length > 50) {
            _errorMessage.value = "Agent Name must be between 2 and 50 characters."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val response = agentRepository.updateAgent(
                agentId = agentId,
                request = EditAgentRequest(agentName = trimmedName, notes = notes?.ifBlank { null })
            )
            _isSubmitting.value = false
            if (response.success) {
                _actionFeedback.value = "Agent profile updated."
                loadAgentDetails(agentId)
                loadAgents()
                onComplete()
            } else {
                _errorMessage.value = response.message ?: "Failed to update agent profile."
            }
        }
    }

    fun suspendAgent(agentId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val response = agentRepository.suspendAgent(agentId)
            _isSubmitting.value = false
            if (response.success) {
                _actionFeedback.value = "Agent successfully suspended."
                loadAgentDetails(agentId)
                loadAgents()
            } else {
                _errorMessage.value = response.message ?: "Failed to suspend agent."
            }
        }
    }

    fun activateAgent(agentId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val response = agentRepository.activateAgent(agentId)
            _isSubmitting.value = false
            if (response.success) {
                _actionFeedback.value = "Agent status restored to ACTIVE."
                loadAgentDetails(agentId)
                loadAgents()
            } else {
                _errorMessage.value = response.message ?: "Failed to activate agent."
            }
        }
    }

    fun resetPassword(agentId: String, customTempPassword: String?, onComplete: () -> Unit) {
        if (!customTempPassword.isNullOrBlank()) {
            val trimmed = customTempPassword.trim()
            if (trimmed.length < 8 || !trimmed.any { it.isDigit() } || !trimmed.any { it.isLetter() }) {
                _errorMessage.value = "Temporary password must be at least 8 characters with letters and digits."
                return
            }
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val response = agentRepository.resetPassword(
                agentId = agentId,
                request = ResetAgentPasswordRequest(temporaryPassword = customTempPassword?.ifBlank { null })
            )
            _isSubmitting.value = false
            if (response.success && response.data != null) {
                val agent = _selectedAgentDetails.value
                _resetCredentials.value = ResetCredentials(
                    agentId = agent?.agentId ?: agentId,
                    temporaryPassword = response.data.temporaryPassword
                )
                _actionFeedback.value = "Password successfully reset."
                loadAgentDetails(agentId)
                onComplete()
            } else {
                _errorMessage.value = response.message ?: "Failed to reset password."
            }
        }
    }

    fun dismissCreatedDialog() {
        _createdCredentials.value = null
    }

    fun dismissResetDialog() {
        _resetCredentials.value = null
    }

    fun clearFeedback() {
        _actionFeedback.value = null
        _errorMessage.value = null
    }

    class Factory(private val repository: AgentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminAgentViewModel(repository) as T
        }
    }
}
