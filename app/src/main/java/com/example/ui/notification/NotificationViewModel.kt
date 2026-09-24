package com.example.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.model.NotificationDto
import com.example.backend.model.NotificationPreferenceDto
import com.example.backend.model.UpdateNotificationPreferenceRequest
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationListUiState {
    object Loading : NotificationListUiState
    data class Success(
        val notifications: List<NotificationDto>,
        val unreadCount: Int,
        val totalCount: Int,
        val page: Int,
        val pageSize: Int
    ) : NotificationListUiState
    data class Error(val message: String) : NotificationListUiState
}

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationListUiState>(NotificationListUiState.Loading)
    val uiState: StateFlow<NotificationListUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _selectedNotification = MutableStateFlow<NotificationDto?>(null)
    val selectedNotification: StateFlow<NotificationDto?> = _selectedNotification.asStateFlow()

    private val _preferences = MutableStateFlow<NotificationPreferenceDto?>(null)
    val preferences: StateFlow<NotificationPreferenceDto?> = _preferences.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null) // null = ALL, "UNREAD", "READ", "ARCHIVED"
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow<String?>(null) // null = ALL
    val typeFilter: StateFlow<String?> = _typeFilter.asStateFlow()

    private val _inAppAlert = MutableSharedFlow<NotificationDto>(extraBufferCapacity = 5)
    val inAppAlert: SharedFlow<NotificationDto> = _inAppAlert.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _operationMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val operationMessage: SharedFlow<String> = _operationMessage.asSharedFlow()

    init {
        loadNotifications()
        loadUnreadCount()
        loadPreferences()
        observeRealtimeNotifications()
    }

    private fun observeRealtimeNotifications() {
        viewModelScope.launch {
            repository.observeRealtimeNotifications().collect { notif ->
                _inAppAlert.tryEmit(notif)
                loadUnreadCount()
                // Refresh list if user is currently viewing
                loadNotifications(silent = true)
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
        loadNotifications()
    }

    fun setTypeFilter(type: String?) {
        _typeFilter.value = type
        loadNotifications()
    }

    fun selectNotification(notification: NotificationDto?) {
        _selectedNotification.value = notification
        if (notification != null && notification.status == "UNREAD") {
            markAsRead(notification.notificationId)
        }
    }

    fun loadNotifications(page: Int = 1, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _isRefreshing.value = true
            }
            val response = repository.getNotifications(
                page = page,
                limit = 25,
                status = _statusFilter.value,
                type = _typeFilter.value
            )
            _isRefreshing.value = false
            if (response.success && response.data != null) {
                _uiState.value = NotificationListUiState.Success(
                    notifications = response.data.notifications,
                    unreadCount = response.data.unreadCount,
                    totalCount = response.data.totalCount,
                    page = response.data.page,
                    pageSize = response.data.pageSize
                )
                _unreadCount.value = response.data.unreadCount
            } else {
                if (!silent) {
                    _uiState.value = NotificationListUiState.Error(response.message ?: "Failed to load notifications.")
                }
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            val response = repository.getUnreadCount()
            if (response.success && response.data != null) {
                _unreadCount.value = response.data.unreadCount
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val response = repository.markAsRead(notificationId)
            if (response.success) {
                loadNotifications(silent = true)
                loadUnreadCount()
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val response = repository.markAllAsRead()
            if (response.success) {
                _operationMessage.tryEmit("All notifications marked as read.")
                loadNotifications(silent = true)
                loadUnreadCount()
            } else {
                _operationMessage.tryEmit(response.message ?: "Failed to mark all as read.")
            }
        }
    }

    fun archiveNotification(notificationId: String) {
        viewModelScope.launch {
            val response = repository.archiveNotification(notificationId)
            if (response.success) {
                _operationMessage.tryEmit("Notification archived.")
                if (_selectedNotification.value?.notificationId == notificationId) {
                    _selectedNotification.value = null
                }
                loadNotifications(silent = true)
                loadUnreadCount()
            } else {
                _operationMessage.tryEmit(response.message ?: "Failed to archive notification.")
            }
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            val response = repository.getPreferences()
            if (response.success && response.data != null) {
                _preferences.value = response.data
            }
        }
    }

    fun updatePreferences(request: UpdateNotificationPreferenceRequest) {
        viewModelScope.launch {
            val response = repository.updatePreferences(request)
            if (response.success && response.data != null) {
                _preferences.value = response.data
                _operationMessage.tryEmit("Notification preferences saved.")
            } else {
                _operationMessage.tryEmit(response.message ?: "Failed to save preferences.")
            }
        }
    }

    fun refreshAll() {
        loadNotifications()
        loadUnreadCount()
        loadPreferences()
    }

    class Factory(private val repository: NotificationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                return NotificationViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
