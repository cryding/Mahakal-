package com.example.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.model.HealthCheckDto
import com.example.backend.model.SecurityDashboardSummaryDto
import com.example.backend.model.SecurityEventDto
import com.example.backend.model.SessionInfoDto
import com.example.data.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityUiState(
    val isLoading: Boolean = false,
    val isActionRunning: Boolean = false,
    val dashboardSummary: SecurityDashboardSummaryDto? = null,
    val securityEvents: List<SecurityEventDto> = emptyList(),
    val totalEventsCount: Int = 0,
    val currentEventsPage: Int = 1,
    val totalEventsPages: Int = 1,
    val selectedFilterType: String? = null,
    val activeSessions: List<SessionInfoDto> = emptyList(),
    val healthCheck: HealthCheckDto? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class SecurityViewModel(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        loadDashboardSummary()
        loadActiveSessions()
        loadHealthCheck()
    }

    fun loadDashboardSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val response = securityRepository.getSecurityDashboardSummary()
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dashboardSummary = response.data,
                        securityEvents = response.data.recentSecurityEvents
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to load security summary."
                    )
                }
            }
        }
    }

    fun loadActiveSessions(targetAccountId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionRunning = true) }
            val response = securityRepository.getActiveSessions(targetAccountId)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isActionRunning = false,
                        activeSessions = response.data
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isActionRunning = false,
                        errorMessage = response.message ?: "Failed to load active sessions."
                    )
                }
            }
        }
    }

    fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionRunning = true) }
            val response = securityRepository.revokeSession(sessionId)
            if (response.success) {
                _uiState.update { it.copy(feedbackMessage = "Session successfully revoked.") }
                loadActiveSessions()
                loadDashboardSummary()
            } else {
                _uiState.update { it.copy(errorMessage = response.message ?: "Failed to revoke session.") }
            }
        }
    }

    fun revokeAllAccountSessions(targetAccountId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionRunning = true) }
            val response = securityRepository.revokeAllAccountSessions(targetAccountId)
            if (response.success) {
                _uiState.update { it.copy(feedbackMessage = "All sessions revoked for account.") }
                loadActiveSessions()
                loadDashboardSummary()
            } else {
                _uiState.update { it.copy(errorMessage = response.message ?: "Failed to revoke sessions.") }
            }
        }
    }

    fun loadHealthCheck() {
        viewModelScope.launch {
            val health = securityRepository.getHealthCheck()
            _uiState.update { it.copy(healthCheck = health) }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }

    class Factory(private val securityRepository: SecurityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecurityViewModel(securityRepository) as T
        }
    }
}
