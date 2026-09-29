package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameEntryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.MahakalRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MahakalViewModel(private val repository: MahakalRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _activeTab = MutableStateFlow("HOME")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    init {
        viewModelScope.launch {
            repository.bootstrapInitialData()
            // Default auto-login to Admin for instant preview inspection, or user can switch freely
            val admin = repository.getUserById("admin_master")
            _currentUser.value = admin
        }
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    fun switchAccount(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
                _activeTab.value = "HOME"
                _snackbarMessage.emit("Switched profile to ${user.fullName} (${user.role})")
            }
        }
    }

    fun login(username: String, pass: String) {
        viewModelScope.launch {
            val user = repository.authenticate(username, pass)
            if (user != null) {
                _currentUser.value = user
                _activeTab.value = "HOME"
                _snackbarMessage.emit("Welcome back, ${user.fullName}")
            } else {
                _snackbarMessage.emit("Invalid credentials or account frozen")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeTab.value = "HOME"
    }

    // Refresh current user data (balance updates)
    fun refreshCurrentUser() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = repository.getUserById(user.id)
            if (updated != null) {
                _currentUser.value = updated
            }
        }
    }

    // Admin streams
    val allAgents: StateFlow<List<UserEntity>> = repository.getAllAgents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<UserEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Agent Downline users stream
    val agentUsers: StateFlow<List<UserEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == "AGENT") {
            repository.getUsersByAgent(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Games streams
    val allGames: StateFlow<List<GameEntity>> = repository.getAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGames: StateFlow<List<GameEntity>> = repository.getActiveGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User entries
    val userEntries: StateFlow<List<GameEntryEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getEntriesByUser(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEntries: StateFlow<List<GameEntryEntity>> = repository.getAllRecentEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ledger / Transactions
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userTransactions: StateFlow<List<TransactionEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            if (user.role == "ADMIN") {
                repository.getAllTransactions()
            } else if (user.role == "AGENT") {
                repository.getTransactionsForAgent(user.id)
            } else {
                repository.getTransactionsForUser(user.id)
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotifications: StateFlow<List<NotificationEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getNotificationsForUser(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin / Agent Actions
    fun createAgent(username: String, fullName: String, initialCoins: Long) {
        viewModelScope.launch {
            val res = repository.createAgent(username, fullName, initialCoins)
            if (res.isSuccess) {
                _snackbarMessage.emit("Agent $fullName appointed with $initialCoins coins")
                refreshCurrentUser()
            } else {
                _snackbarMessage.emit(res.exceptionOrNull()?.message ?: "Failed to create agent")
            }
        }
    }

    fun createUserUnderAgent(username: String, fullName: String, initialCoins: Long) {
        val agent = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.createUserUnderAgent(agent.id, username, fullName, initialCoins)
            if (res.isSuccess) {
                _snackbarMessage.emit("Player $fullName onboarded successfully")
                refreshCurrentUser()
            } else {
                _snackbarMessage.emit(res.exceptionOrNull()?.message ?: "Failed to onboard player")
            }
        }
    }

    fun transferCoins(recipientId: String, amount: Long, notes: String) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.transferCoins(actor, recipientId, amount, notes)
            if (res.isSuccess) {
                _snackbarMessage.emit("Successfully transferred $amount coins")
                refreshCurrentUser()
            } else {
                _snackbarMessage.emit(res.exceptionOrNull()?.message ?: "Transfer failed")
            }
        }
    }

    fun mintTreasuryCoins(amount: Long) {
        val admin = _currentUser.value ?: return
        if (admin.role != "ADMIN") return
        viewModelScope.launch {
            val res = repository.mintTreasuryCoins(admin, amount)
            if (res.isSuccess) {
                _snackbarMessage.emit("Minted $amount coins into platform treasury")
                refreshCurrentUser()
            }
        }
    }

    fun toggleUserStatus(targetUserId: String, newStatus: String) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.toggleUserStatus(actor, targetUserId, newStatus)
            if (res.isSuccess) {
                _snackbarMessage.emit("Status updated to $newStatus")
            }
        }
    }

    // Game Actions
    fun createGame(title: String, category: String, minCoins: Long, maxCoins: Long, multiplier: Double) {
        val admin = _currentUser.value ?: return
        if (admin.role != "ADMIN") return
        viewModelScope.launch {
            val res = repository.createGame(admin, title, category, minCoins, maxCoins, multiplier)
            if (res.isSuccess) {
                _snackbarMessage.emit("Game '${title}' published successfully")
            }
        }
    }

    fun placeGameEntry(gameId: String, option: String, coins: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.placeGameEntry(user, gameId, option, coins)
            if (res.isSuccess) {
                _snackbarMessage.emit("Entry of $coins coins placed on [$option]!")
                refreshCurrentUser()
            } else {
                _snackbarMessage.emit(res.exceptionOrNull()?.message ?: "Entry failed")
            }
        }
    }

    fun finalizeGameResult(gameId: String, winningOption: String) {
        val admin = _currentUser.value ?: return
        if (admin.role != "ADMIN") return
        viewModelScope.launch {
            val res = repository.finalizeGameResult(admin, gameId, winningOption)
            if (res.isSuccess) {
                _snackbarMessage.emit("Game settled! Winner: [$winningOption]. Rewards distributed.")
                refreshCurrentUser()
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    class Factory(private val repository: MahakalRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MahakalViewModel(repository) as T
        }
    }
}
