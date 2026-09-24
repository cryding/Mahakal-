package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.rbac.AccountRole
import com.example.core.network.AuthState
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val loginId: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuspended: Boolean = false
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.authState

    fun onLoginIdChanged(newId: String) {
        _uiState.value = _uiState.value.copy(
            loginId = newId,
            errorMessage = null,
            isSuspended = false
        )
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.value = _uiState.value.copy(
            password = newPassword,
            errorMessage = null,
            isSuspended = false
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.loginId.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Both ID / Username and Password are required."
            )
            return
        }

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null, isSuspended = false)

        viewModelScope.launch {
            val response = authRepository.login(
                loginId = currentState.loginId.trim(),
                password = currentState.password
            )
            if (response.success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    password = "", // Clear password from memory
                    errorMessage = null
                )
            } else {
                val isSuspended = response.errorCode == "ACCOUNT_SUSPENDED"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = response.message ?: "Invalid credentials. Please verify your ID and password.",
                    isSuspended = isSuspended
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(authRepository) as T
        }
    }
}
