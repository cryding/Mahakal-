package com.example.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.EditUserRequest
import com.example.backend.model.ResetPasswordResponse
import com.example.backend.model.ResetUserPasswordRequest
import com.example.backend.model.UserCreatedDto
import com.example.backend.model.UserDetailsDto
import com.example.backend.model.UserSummaryDto
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentUserUiState(
    val users: List<UserSummaryDto> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val limit: Int = 10,
    val statusFilter: String = "ALL",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Dialog & Detail states
    val isCreateDialogOpen: Boolean = false,
    val createdCredentials: UserCreatedDto? = null,
    val resetPasswordResult: ResetPasswordResponse? = null,
    val selectedUserDetails: UserDetailsDto? = null,
    val isLoadingDetails: Boolean = false,
    val userToEdit: UserSummaryDto? = null,
    val userToSuspend: UserSummaryDto? = null,
    val userToActivate: UserSummaryDto? = null,
    val userToResetPassword: UserSummaryDto? = null
)

class AgentUserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUserUiState())
    val uiState: StateFlow<AgentUserUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value
            val response = userRepository.getUsers(
                page = state.currentPage,
                limit = state.limit,
                status = if (state.statusFilter == "ALL") null else state.statusFilter,
                search = if (state.searchQuery.isBlank()) null else state.searchQuery
            )

            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        users = response.data.users,
                        totalCount = response.data.totalCount,
                        currentPage = response.data.page,
                        totalPages = response.data.totalPages,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to load users."
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, currentPage = 1) }
        loadUsers()
    }

    fun setStatusFilter(filter: String) {
        _uiState.update { it.copy(statusFilter = filter, currentPage = 1) }
        loadUsers()
    }

    fun setPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            _uiState.update { it.copy(currentPage = page) }
            loadUsers()
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = true, errorMessage = null) }
    }

    fun closeCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false) }
    }

    fun createUser(
        userId: String,
        displayName: String,
        password: String,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val request = CreateUserRequest(
                userId = userId,
                displayName = displayName,
                temporaryPassword = password,
                notes = notes
            )
            val response = userRepository.createUser(request)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreateDialogOpen = false,
                        createdCredentials = response.data,
                        successMessage = "User ${response.data.userId} created successfully."
                    )
                }
                loadUsers()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to create user."
                    )
                }
            }
        }
    }

    fun dismissCreatedCredentials() {
        _uiState.update { it.copy(createdCredentials = null, resetPasswordResult = null) }
    }

    fun openUserDetails(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true, errorMessage = null) }
            val response = userRepository.getUserDetails(userId)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        selectedUserDetails = response.data,
                        isLoadingDetails = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        errorMessage = response.message ?: "Failed to load user details."
                    )
                }
            }
        }
    }

    fun closeUserDetails() {
        _uiState.update { it.copy(selectedUserDetails = null) }
    }

    fun openEditDialog(user: UserSummaryDto) {
        _uiState.update { it.copy(userToEdit = user, errorMessage = null) }
    }

    fun closeEditDialog() {
        _uiState.update { it.copy(userToEdit = null) }
    }

    fun updateUser(userId: String, displayName: String, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val response = userRepository.updateUser(
                userId,
                EditUserRequest(displayName = displayName, notes = notes)
            )
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userToEdit = null,
                        successMessage = "User profile updated successfully."
                    )
                }
                loadUsers()
                // Refresh detail if open
                if (_uiState.value.selectedUserDetails?.id == userId ||
                    _uiState.value.selectedUserDetails?.userId == userId) {
                    openUserDetails(userId)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to update user."
                    )
                }
            }
        }
    }

    fun openSuspendDialog(user: UserSummaryDto) {
        _uiState.update { it.copy(userToSuspend = user) }
    }

    fun closeSuspendDialog() {
        _uiState.update { it.copy(userToSuspend = null) }
    }

    fun suspendUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val response = userRepository.suspendUser(userId)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userToSuspend = null,
                        successMessage = "User suspended and sessions revoked."
                    )
                }
                loadUsers()
                if (_uiState.value.selectedUserDetails?.id == userId) {
                    openUserDetails(userId)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to suspend user."
                    )
                }
            }
        }
    }

    fun openActivateDialog(user: UserSummaryDto) {
        _uiState.update { it.copy(userToActivate = user) }
    }

    fun closeActivateDialog() {
        _uiState.update { it.copy(userToActivate = null) }
    }

    fun activateUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val response = userRepository.activateUser(userId)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userToActivate = null,
                        successMessage = "User status restored to ACTIVE."
                    )
                }
                loadUsers()
                if (_uiState.value.selectedUserDetails?.id == userId) {
                    openUserDetails(userId)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to activate user."
                    )
                }
            }
        }
    }

    fun openResetPasswordDialog(user: UserSummaryDto) {
        _uiState.update { it.copy(userToResetPassword = user) }
    }

    fun closeResetPasswordDialog() {
        _uiState.update { it.copy(userToResetPassword = null) }
    }

    fun resetPassword(userId: String, customTempPassword: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val response = userRepository.resetPassword(
                userId,
                ResetUserPasswordRequest(temporaryPassword = customTempPassword)
            )
            if (response.success && response.data != null) {
                val user = _uiState.value.userToResetPassword
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userToResetPassword = null,
                        resetPasswordResult = response.data,
                        createdCredentials = UserCreatedDto(
                            id = user?.id ?: userId,
                            userId = user?.userId ?: userId,
                            displayName = user?.displayName ?: "User",
                            agentId = user?.agentId ?: "",
                            role = "USER",
                            status = user?.status ?: "ACTIVE",
                            createdAt = System.currentTimeMillis(),
                            temporaryPassword = response.data.temporaryPassword
                        ),
                        successMessage = "Password reset successfully. Credentials ready for delivery."
                    )
                }
                loadUsers()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to reset user password."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AgentUserViewModel(userRepository) as T
        }
    }
}
