package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.ui.wallet.AdminCoinsDirectoryView
import com.example.ui.wallet.AgentCoinManagementScreen
import com.example.ui.wallet.CoinTransactionFailureDialog
import com.example.ui.wallet.CoinTransactionSuccessDialog
import com.example.ui.wallet.DeductCoinScreen
import com.example.ui.wallet.DeductCoinsDialog
import com.example.ui.wallet.ReverseTransactionDialog
import com.example.ui.wallet.TransactionDetailDialog
import com.example.ui.wallet.TransactionLedgerView
import com.example.ui.wallet.TransferCoinScreen
import com.example.ui.wallet.TransferCoinsDialog
import com.example.ui.wallet.WalletBalanceCard
import com.example.ui.wallet.WalletViewModel
import com.example.ui.game.AdminGamesScreen
import com.example.ui.game.GameViewModel
import com.example.ui.security.AdminSecurityScreen
import com.example.ui.security.SecurityViewModel
import com.example.ui.agent.AgentUserViewModel
import com.example.ui.agent.AgentUsersTab
import com.example.ui.notification.NotificationsScreen
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.backend.model.AgentDetailsDto
import com.example.backend.model.AgentSummaryDto
import com.example.backend.model.UserProfileDto
import com.example.ui.notification.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// Color Palette for Mahakal Platform
private val BgDark = Color(0xFF0A0E17)
private val CardBg = Color(0xFF141C28)
private val CardBorder = Color(0xFF1E2B3E)
private val AccentGold = Color(0xFFE5A93C)
private val AgentBlue = Color(0xFF42A5F5)
private val SuccessGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFEF5350)
private val TextMuted = Color(0xFF8A99AD)

@Composable
fun AdminDashboardScreen(
    adminUser: UserProfileDto,
    viewModel: AdminAgentViewModel,
    agentUserViewModel: AgentUserViewModel? = null,
    walletViewModel: WalletViewModel? = null,
    gameViewModel: GameViewModel? = null,
    notificationViewModel: NotificationViewModel? = null,
    securityViewModel: SecurityViewModel? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    val listState by viewModel.listState.collectAsState()
    val selectedAgentDetails by viewModel.selectedAgentDetails.collectAsState()
    val isLoadingDetails by viewModel.isLoadingDetails.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val createdCredentials by viewModel.createdCredentials.collectAsState()
    val resetCredentials by viewModel.resetCredentials.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Phase 5 Wallet State
    val walletState = walletViewModel?.uiState?.collectAsState()?.value
    val unreadNotifCount = notificationViewModel?.unreadCount?.collectAsState()?.value ?: 0

    // Phase 12 Production Unified Admin Navigation: 15 Tabs
    var selectedNavTab by remember { mutableStateOf(AdminNavTab.DASHBOARD) }
    val tabsList = remember { AdminNavTab.values() }
    val selectedTabIndex = remember(selectedNavTab) { tabsList.indexOf(selectedNavTab).coerceAtLeast(0) }

    val gameUiState = gameViewModel?.uiState?.collectAsState()?.value
    val securityUiState = securityViewModel?.uiState?.collectAsState()?.value
    val agentUserUiState = agentUserViewModel?.uiState?.collectAsState()?.value

    var showCreateDialog by remember { mutableStateOf(false) }
    var agentToEdit by remember { mutableStateOf<AgentSummaryDto?>(null) }
    var agentToReset by remember { mutableStateOf<AgentSummaryDto?>(null) }
    var agentToToggleStatus by remember { mutableStateOf<AgentSummaryDto?>(null) }

    // Phase 6 Coin Management States
    var transferTargetAgent by remember { mutableStateOf<AgentSummaryDto?>(null) }
    var deductTargetAgent by remember { mutableStateOf<AgentSummaryDto?>(null) }
    var viewTransactionsAgent by remember { mutableStateOf<AgentSummaryDto?>(null) }

    // Coin Action Dialog States
    var showTransferCoinsDialog by remember { mutableStateOf(false) }
    var showDeductCoinsDialog by remember { mutableStateOf(false) }
    var txToReverse by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Sync authoritative account balances for all loaded agents
    LaunchedEffect(listState.agents) {
        if (listState.agents.isNotEmpty()) {
            walletViewModel?.loadAccountBalances(listState.agents.map { it.id })
        }
    }

    LaunchedEffect(actionFeedback) {
        actionFeedback?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(walletState?.actionSuccessMessage) {
        walletState?.actionSuccessMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            walletViewModel?.clearFeedback()
        }
    }

    LaunchedEffect(walletState?.actionErrorMessage) {
        walletState?.actionErrorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            walletViewModel?.clearFeedback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Admin Top App Bar
            AdminHeader(
                adminUser = adminUser,
                unreadCount = unreadNotifCount,
                onOpenNotifications = onOpenNotifications,
                onLogout = onLogout
            )

            // Navigation Tabs - Full 15 Tab Scrollable Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF0F1622),
                contentColor = Color.White,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = AccentGold
                        )
                    }
                }
            ) {
                tabsList.forEach { tab ->
                    val isSelected = selectedNavTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = {
                            selectedNavTab = tab
                            when (tab) {
                                AdminNavTab.DASHBOARD -> {
                                    gameViewModel?.loadAdminSummary()
                                    securityViewModel?.loadDashboardSummary()
                                    walletViewModel?.loadBalance()
                                }
                                AdminNavTab.AGENTS -> {
                                    viewModel.loadAgents()
                                }
                                AdminNavTab.USERS -> {
                                    agentUserViewModel?.loadUsers()
                                }
                                AdminNavTab.GAMES -> {
                                    gameViewModel?.loadGames()
                                    gameViewModel?.loadAdminGames()
                                }
                                AdminNavTab.RESULTS -> {
                                    gameViewModel?.loadAdminGames(status = com.example.backend.model.GameStatus.RESULT_PENDING, page = 1)
                                    gameViewModel?.loadAdminSummary()
                                }
                                AdminNavTab.VIRTUAL_COINS -> {
                                    walletViewModel?.loadBalance()
                                    if (listState.agents.isNotEmpty()) {
                                        walletViewModel?.loadAccountBalances(listState.agents.map { it.id })
                                    }
                                }
                                AdminNavTab.TRANSACTIONS -> {
                                    walletViewModel?.loadTransactions()
                                }
                                AdminNavTab.NOTIFICATIONS -> {
                                    notificationViewModel?.loadNotifications()
                                }
                                AdminNavTab.REPORTS -> {
                                    gameViewModel?.loadReports()
                                }
                                AdminNavTab.AUDIT -> {
                                    gameViewModel?.loadAuditLogs()
                                }
                                AdminNavTab.SECURITY -> {
                                    securityViewModel?.loadDashboardSummary()
                                    securityViewModel?.loadActiveSessions()
                                }
                                AdminNavTab.SESSIONS -> {
                                    securityViewModel?.loadActiveSessions()
                                }
                                AdminNavTab.PROCESSING -> {
                                    gameViewModel?.loadAdminSummary()
                                    gameViewModel?.loadAdminGames()
                                }
                                AdminNavTab.SYSTEM_HEALTH -> {
                                    securityViewModel?.loadHealthCheck()
                                    securityViewModel?.loadDashboardSummary()
                                }
                                AdminNavTab.SETTINGS -> {
                                    // Settings view is local
                                }
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (isSelected) AccentGold else TextMuted
                                )
                                if (tab == AdminNavTab.AGENTS && listState.totalCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) AccentGold else CardBorder,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${listState.totalCount}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }

            // Body Content
            when (selectedNavTab) {
                AdminNavTab.DASHBOARD -> {
                    AdminControlCenterDashboardView(
                        adminUser = adminUser,
                        totalAgents = listState.totalCount,
                        walletBalance = walletState?.myBalance?.balance ?: 0L,
                        isLoadingWallet = walletState?.isLoadingBalance ?: false,
                        operationsSummary = gameUiState?.adminSummary,
                        securitySummary = securityUiState?.dashboardSummary,
                        isLoadingSummary = (gameUiState?.isLoadingSummary ?: false) || (securityUiState?.isLoading ?: false),
                        onRefresh = {
                            gameViewModel?.loadAdminSummary()
                            securityViewModel?.loadDashboardSummary()
                            walletViewModel?.loadBalance()
                            viewModel.loadAgents()
                        },
                        onNavigateTab = { navTab ->
                            selectedNavTab = navTab
                        }
                    )
                }
                AdminNavTab.AGENTS -> {
                    AgentManagementView(
                        listState = listState,
                        onSearch = { viewModel.searchAgents(it) },
                        onFilterSelect = { viewModel.setFilter(it) },
                        onPageChange = { viewModel.loadAgents(page = it) },
                        onRefresh = { viewModel.loadAgents() },
                        onCreateClick = { showCreateDialog = true },
                        onViewAgent = { viewModel.loadAgentDetails(it.id) },
                        onEditAgent = { agentToEdit = it },
                        onResetPassword = { agentToReset = it },
                        onToggleStatus = { agentToToggleStatus = it }
                    )
                }
                AdminNavTab.USERS -> {
                    if (agentUserViewModel != null && agentUserUiState != null) {
                        AgentUsersTab(
                            uiState = agentUserUiState,
                            viewModel = agentUserViewModel
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("User management module is initializing...", color = TextMuted)
                        }
                    }
                }
                AdminNavTab.GAMES -> {
                    if (gameViewModel != null) {
                        AdminGamesScreen(gameViewModel = gameViewModel)
                    }
                }
                AdminNavTab.RESULTS -> {
                    if (gameViewModel != null) {
                        AdminResultsScreen(gameViewModel = gameViewModel)
                    }
                }
                AdminNavTab.VIRTUAL_COINS -> {
                    AdminCoinsDirectoryView(
                        adminBalance = walletState?.myBalance?.balance ?: 0L,
                        agents = listState.agents,
                        accountBalances = walletState?.accountBalances ?: emptyMap(),
                        isLoading = listState.isLoading || (walletState?.isLoadingBalances ?: false),
                        onRefresh = {
                            walletViewModel?.loadBalance()
                            if (listState.agents.isNotEmpty()) {
                                walletViewModel?.loadAccountBalances(listState.agents.map { it.id })
                            }
                        },
                        onTransferCoins = { agent ->
                            transferTargetAgent = agent
                        },
                        onDeductCoins = { agent ->
                            deductTargetAgent = agent
                        },
                        onViewTransactions = { agent ->
                            viewTransactionsAgent = agent
                            walletViewModel?.loadAccountSummary(agent.id)
                        }
                    )
                }
                AdminNavTab.TRANSACTIONS -> {
                    TransactionLedgerView(
                        transactions = walletState?.transactions ?: emptyList(),
                        isLoading = walletState?.isLoadingTransactions ?: false,
                        totalCount = walletState?.totalTransactionsCount ?: 0,
                        currentPage = walletState?.currentTransactionsPage ?: 1,
                        totalPages = walletState?.totalTransactionsPages ?: 1,
                        filterType = walletState?.filterType,
                        searchQuery = walletState?.searchQuery ?: "",
                        onFilterChange = { walletViewModel?.loadTransactions(type = it) },
                        onSearchChange = { walletViewModel?.loadTransactions(search = it) },
                        onPageChange = { walletViewModel?.loadTransactions(page = it) },
                        onRefresh = {
                            walletViewModel?.loadBalance()
                            walletViewModel?.loadTransactions()
                        },
                        onSelectTransaction = { walletViewModel?.selectTransaction(it) }
                    )
                }
                AdminNavTab.NOTIFICATIONS -> {
                    if (notificationViewModel != null) {
                        NotificationsScreen(
                            viewModel = notificationViewModel,
                            onBack = { selectedNavTab = AdminNavTab.DASHBOARD }
                        )
                    }
                }
                AdminNavTab.REPORTS -> {
                    if (gameViewModel != null) {
                        AdminReportsScreen(gameViewModel = gameViewModel)
                    }
                }
                AdminNavTab.AUDIT -> {
                    if (gameViewModel != null) {
                        AdminAuditScreen(gameViewModel = gameViewModel)
                    }
                }
                AdminNavTab.SECURITY -> {
                    if (securityViewModel != null) {
                        AdminSecurityScreen(securityViewModel = securityViewModel)
                    }
                }
                AdminNavTab.SESSIONS -> {
                    if (securityViewModel != null) {
                        AdminSecurityScreen(securityViewModel = securityViewModel)
                    }
                }
                AdminNavTab.PROCESSING -> {
                    if (gameViewModel != null) {
                        AdminProcessingScreen(gameViewModel = gameViewModel)
                    }
                }
                AdminNavTab.SYSTEM_HEALTH -> {
                    if (securityViewModel != null) {
                        AdminSystemHealthScreen(securityViewModel = securityViewModel)
                    }
                }
                AdminNavTab.SETTINGS -> {
                    AdminSettingsScreen(
                        adminUser = adminUser,
                        onLogout = onLogout
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Floating Action Button for Creating Agent on Agent tab
        if (selectedNavTab == AdminNavTab.AGENTS) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AccentGold,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("create_agent_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Agent")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "New Agent", fontWeight = FontWeight.Bold)
                }
            }
        }

        // =====================================================================
        // DIALOGS & MODALS
        // =====================================================================

        // 1. Create Agent Dialog
        if (showCreateDialog) {
            CreateAgentDialog(
                isSubmitting = isSubmitting,
                onDismiss = { showCreateDialog = false },
                onSubmit = { id, name, pwd, confirm, notes ->
                    viewModel.createAgent(id, name, pwd, confirm, notes) {
                        showCreateDialog = false
                    }
                }
            )
        }

        // 2. Created Credentials One-Time Display
        createdCredentials?.let { creds ->
            CreatedCredentialsDialog(
                credentials = creds,
                onDismiss = { viewModel.dismissCreatedDialog() }
            )
        }

        // 3. Reset Password Dialog
        agentToReset?.let { agent ->
            ResetPasswordDialog(
                agent = agent,
                isSubmitting = isSubmitting,
                onDismiss = { agentToReset = null },
                onConfirm = { customPwd ->
                    viewModel.resetPassword(agent.id, customPwd) {
                        agentToReset = null
                    }
                }
            )
        }

        // 4. Reset Credentials One-Time Display
        resetCredentials?.let { creds ->
            ResetCredentialsDialog(
                credentials = creds,
                onDismiss = { viewModel.dismissResetDialog() }
            )
        }

        // 5. Edit Agent Dialog
        agentToEdit?.let { agent ->
            EditAgentDialog(
                agent = agent,
                isSubmitting = isSubmitting,
                onDismiss = { agentToEdit = null },
                onSave = { name, notes ->
                    viewModel.updateAgent(agent.id, name, notes) {
                        agentToEdit = null
                    }
                }
            )
        }

        // 6. Suspend / Activate Confirmation Dialog
        agentToToggleStatus?.let { agent ->
            val isCurrentlyActive = agent.status == "ACTIVE"
            val actionTitle = if (isCurrentlyActive) "Suspend Agent" else "Activate Agent"
            val actionMessage = if (isCurrentlyActive) {
                "Are you sure you want to suspend '${agent.agentId}'? All active sessions for this agent will be revoked immediately and login access will be blocked."
            } else {
                "Restore login access for agent '${agent.agentId}'?"
            }

            AlertDialog(
                onDismissRequest = { agentToToggleStatus = null },
                title = { Text(text = actionTitle, color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text(text = actionMessage, color = TextMuted) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isCurrentlyActive) {
                                viewModel.suspendAgent(agent.id)
                            } else {
                                viewModel.activateAgent(agent.id)
                            }
                            agentToToggleStatus = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentlyActive) DangerRed else SuccessGreen
                        )
                    ) {
                        Text(if (isCurrentlyActive) "Suspend" else "Activate", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { agentToToggleStatus = null }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = CardBg
            )
        }

        // 7. Full Agent Details Dialog
        if (selectedAgentDetails != null || isLoadingDetails) {
            AgentDetailsDialog(
                details = selectedAgentDetails,
                isLoading = isLoadingDetails,
                onDismiss = { viewModel.clearSelectedAgent() },
                onEdit = {
                    val d = selectedAgentDetails
                    if (d != null) {
                        agentToEdit = AgentSummaryDto(
                            id = d.id,
                            agentId = d.agentId,
                            agentName = d.agentName,
                            status = d.status,
                            userCount = d.userCount,
                            createdAt = d.createdAt,
                            lastLoginAt = d.lastLoginAt,
                            notes = d.notes
                        )
                    }
                },
                onResetPassword = {
                    val d = selectedAgentDetails
                    if (d != null) {
                        agentToReset = AgentSummaryDto(
                            id = d.id,
                            agentId = d.agentId,
                            agentName = d.agentName,
                            status = d.status,
                            userCount = d.userCount,
                            createdAt = d.createdAt,
                            lastLoginAt = d.lastLoginAt,
                            notes = d.notes
                        )
                    }
                },
                onToggleStatus = {
                    val d = selectedAgentDetails
                    if (d != null) {
                        agentToToggleStatus = AgentSummaryDto(
                            id = d.id,
                            agentId = d.agentId,
                            agentName = d.agentName,
                            status = d.status,
                            userCount = d.userCount,
                            createdAt = d.createdAt,
                            lastLoginAt = d.lastLoginAt,
                            notes = d.notes
                        )
                    }
                }
            )
        }

        // 8. Phase 5: Transfer Coins Dialog
        if (showTransferCoinsDialog) {
            val agentCandidates = listState.agents
                .filter { it.status == "ACTIVE" }
                .map { it.id to "${it.agentId} (${it.agentName})" }

            TransferCoinsDialog(
                role = "ADMIN",
                availableBalance = walletState?.myBalance?.balance ?: 0L,
                candidates = agentCandidates,
                isLoadingCandidates = listState.isLoading,
                isSubmitting = walletState?.isSubmittingAction ?: false,
                onDismiss = { showTransferCoinsDialog = false },
                onConfirmTransfer = { destId, amount, reason ->
                    walletViewModel?.transferCoins(
                        destinationAccountId = destId,
                        amount = amount,
                        reason = reason
                    ) {
                        showTransferCoinsDialog = false
                    }
                }
            )
        }

        // 9. Phase 5: Deduct Coins Dialog
        if (showDeductCoinsDialog) {
            val agentCandidates = listState.agents
                .filter { it.status == "ACTIVE" }
                .map { it.id to "${it.agentId} (${it.agentName})" }

            DeductCoinsDialog(
                role = "ADMIN",
                candidates = agentCandidates,
                isSubmitting = walletState?.isSubmittingAction ?: false,
                onDismiss = { showDeductCoinsDialog = false },
                onConfirmDeduct = { targetId, amount, reason ->
                    walletViewModel?.deductCoins(
                        targetAccountId = targetId,
                        amount = amount,
                        reason = reason
                    ) {
                        showDeductCoinsDialog = false
                    }
                }
            )
        }

        // 10. Phase 5: Transaction Detail Inspector Dialog
        walletState?.selectedTransaction?.let { tx ->
            TransactionDetailDialog(
                transaction = tx,
                currentUserRole = "ADMIN",
                onDismiss = { walletViewModel?.selectTransaction(null) },
                onReverseClick = {
                    txToReverse = tx.transactionId
                    walletViewModel?.selectTransaction(null)
                }
            )
        }

        // 11. Phase 5: Reversal Confirmation Dialog
        txToReverse?.let { txId ->
            ReverseTransactionDialog(
                transactionId = txId,
                isSubmitting = walletState?.isSubmittingAction ?: false,
                onDismiss = { txToReverse = null },
                onConfirm = { reason ->
                    walletViewModel?.reverseTransaction(
                        transactionId = txId,
                        reason = reason
                    ) {
                        txToReverse = null
                    }
                }
            )
        }

        // 12. Phase 6: Transfer Coin Screen Overlay
        transferTargetAgent?.let { agent ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark)
            ) {
                TransferCoinScreen(
                    role = "ADMIN",
                    senderName = "Super Admin (Treasury)",
                    availableBalance = walletState?.myBalance?.balance ?: 0L,
                    recipientAccountId = agent.id,
                    recipientDisplayName = "${agent.agentName} (${agent.agentId})",
                    isSubmitting = walletState?.isSubmittingAction ?: false,
                    onBack = { transferTargetAgent = null },
                    onSubmitTransfer = { amount, reason ->
                        walletViewModel?.transferCoins(
                            destinationAccountId = agent.id,
                            amount = amount,
                            reason = reason
                        ) {
                            transferTargetAgent = null
                            if (listState.agents.isNotEmpty()) {
                                walletViewModel.loadAccountBalances(listState.agents.map { it.id })
                            }
                        }
                    }
                )
            }
        }

        // 13. Phase 6: Deduct Coin Screen Overlay
        deductTargetAgent?.let { agent ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark)
            ) {
                val currentTargetBal = walletState?.accountBalances?.get(agent.id) ?: 0L
                DeductCoinScreen(
                    role = "ADMIN",
                    operatorName = "Super Admin",
                    targetAccountId = agent.id,
                    targetDisplayName = "${agent.agentName} (${agent.agentId})",
                    targetCurrentBalance = currentTargetBal,
                    isSubmitting = walletState?.isSubmittingAction ?: false,
                    onBack = { deductTargetAgent = null },
                    onSubmitDeduct = { amount, reason ->
                        walletViewModel?.deductCoins(
                            targetAccountId = agent.id,
                            amount = amount,
                            reason = reason
                        ) {
                            deductTargetAgent = null
                            if (listState.agents.isNotEmpty()) {
                                walletViewModel.loadAccountBalances(listState.agents.map { it.id })
                            }
                        }
                    }
                )
            }
        }

        // 14. Phase 6: Agent Coin Management Detail Screen Overlay
        viewTransactionsAgent?.let { agent ->
            walletState?.selectedAccountSummary?.let { summary ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgDark)
                ) {
                    AgentCoinManagementScreen(
                        agentSummary = summary,
                        isLoading = walletState.isLoadingAccountSummary,
                        onBack = {
                            viewTransactionsAgent = null
                            walletViewModel?.clearAccountSummary()
                        },
                        onTransferClick = {
                            transferTargetAgent = agent
                        },
                        onDeductClick = {
                            deductTargetAgent = agent
                        },
                        onRefresh = {
                            walletViewModel?.loadAccountSummary(agent.id)
                        },
                        onSelectTransaction = { tx ->
                            walletViewModel?.selectTransaction(tx)
                        }
                    )
                }
            }
        }

        // 15. Phase 6: Authoritative Success Dialog
        walletState?.activeCompletedTransaction?.let { tx ->
            CoinTransactionSuccessDialog(
                transaction = tx,
                onViewTransaction = {
                    walletViewModel?.selectTransaction(tx)
                    walletViewModel?.clearActiveResultScreens()
                },
                onDone = {
                    walletViewModel?.clearActiveResultScreens()
                }
            )
        }

        // 16. Phase 6: Authoritative Failure Dialog
        walletState?.activeFailureDetails?.let { failure ->
            CoinTransactionFailureDialog(
                errorCode = failure.first,
                friendlyMessage = failure.second,
                onRetry = {
                    walletViewModel?.clearActiveResultScreens()
                },
                onClose = {
                    walletViewModel?.clearActiveResultScreens()
                }
            )
        }
    }
}

// =============================================================================
// SUB-VIEWS
// =============================================================================

@Composable
private fun AdminHeader(
    adminUser: UserProfileDto,
    unreadCount: Int = 0,
    onOpenNotifications: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    Surface(
        color = Color(0xFF0C121D),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = AccentGold.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MAHAKAL ADMIN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${adminUser.fullName} (${adminUser.loginId})",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenNotifications != null) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("admin_notifications_btn")
                        ) {
                            Icon(
                                imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadCount > 0) AccentGold else Color(0xFF8A99AD),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("admin_logout_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = DangerRed
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewView(
    adminUser: UserProfileDto,
    totalAgents: Int,
    walletBalance: Long,
    isLoadingWallet: Boolean,
    onRefreshWallet: () -> Unit,
    onTransferCoins: () -> Unit,
    onDeductCoins: () -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToLedger: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Phase 5: Virtual Coin Treasury / Wallet Card
        WalletBalanceCard(
            balance = walletBalance,
            isLoading = isLoadingWallet,
            role = "ADMIN",
            onRefresh = onRefreshWallet,
            onTransferClick = onTransferCoins,
            onDeductClick = onDeductCoins,
            onViewLedgerClick = onNavigateToLedger
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SYSTEM METRICS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Subordinated Agents",
                value = "$totalAgents",
                subtitle = "Active Hierarchy",
                icon = Icons.Default.SupportAgent,
                accentColor = AgentBlue,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToAgents() }
            )
            MetricCard(
                title = "Security Level",
                value = "RBAC L3",
                subtitle = "Atomic Coin Ledger",
                icon = Icons.Default.Security,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Architecture Hierarchy Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phase 5: Production Virtual Coin Ledger Active",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Production-grade atomic virtual coin engine is online. All coin transfers and deductions conserve total balance, use idempotency keys, write append-only audit entries, and enforce strict RBAC ownership. Internal non-monetary units only.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToAgents,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Manage Agents", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToLedger,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "View Global Ledger", color = AccentGold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 10.sp, color = accentColor)
        }
    }
}

@Composable
private fun AgentManagementView(
    listState: AgentListUiState,
    onSearch: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCreateClick: () -> Unit,
    onViewAgent: (AgentSummaryDto) -> Unit,
    onEditAgent: (AgentSummaryDto) -> Unit,
    onResetPassword: (AgentSummaryDto) -> Unit,
    onToggleStatus: (AgentSummaryDto) -> Unit
) {
    var searchText by remember { mutableStateOf(listState.searchQuery) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Search & Refresh Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    onSearch(it)
                },
                placeholder = { Text("Search Agent ID or Name...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            onSearch("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("agent_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .size(52.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("ALL" to "All Agents", "ACTIVE" to "Active Only", "SUSPENDED" to "Suspended").forEach { (key, label) ->
                val isSelected = listState.selectedFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelect(key) },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                        selectedLabelColor = AccentGold,
                        containerColor = CardBg,
                        labelColor = TextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) AccentGold else CardBorder
                    ),
                    modifier = Modifier.testTag("filter_chip_$key")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Agent List
        if (listState.isLoading && listState.agents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (listState.agents.isEmpty()) {
            EmptyAgentsView(
                searchActive = searchText.isNotEmpty() || listState.selectedFilter != "ALL",
                onCreateClick = onCreateClick
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(listState.agents, key = { it.id }) { agent ->
                    AgentListItem(
                        agent = agent,
                        onClick = { onViewAgent(agent) },
                        onEdit = { onEditAgent(agent) },
                        onResetPassword = { onResetPassword(agent) },
                        onToggleStatus = { onToggleStatus(agent) }
                    )
                }

                item {
                    // Pagination Bar
                    if (listState.totalPages > 1) {
                        PaginationControls(
                            currentPage = listState.currentPage,
                            totalPages = listState.totalPages,
                            onPageChange = onPageChange
                        )
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun AgentListItem(
    agent: AgentSummaryDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isActive = agent.status == "ACTIVE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("agent_item_${agent.agentId}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Agent Icon Badge
                    Surface(
                        shape = CircleShape,
                        color = AgentBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = AgentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = agent.agentId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = agent.status)
                        }
                        Text(
                            text = agent.agentName,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted)
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E2838))
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AgentBlue) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Profile", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentGold) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isActive) "Suspend Agent" else "Activate Agent", color = if (isActive) DangerRed else SuccessGreen) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isActive) DangerRed else SuccessGreen
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleStatus()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset Password", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = AccentGold) },
                            onClick = {
                                menuExpanded = false
                                onResetPassword()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subordinated Users count + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Users",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${agent.userCount} Subordinated Users",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Text(
                    text = "Created: ${formatDate(agent.createdAt)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val isActive = status == "ACTIVE"
    val badgeBg = if (isActive) SuccessGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f)
    val badgeColor = if (isActive) SuccessGreen else DangerRed

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeBg
    ) {
        Text(
            text = status,
            color = badgeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyAgentsView(
    searchActive: Boolean,
    onCreateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = CardBorder,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchActive) "No Matching Agents Found" else "No Subordinated Agents Yet",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (searchActive) "Try clearing search filters or search terms." else "Create your first Agent to begin delegating player user management.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            if (!searchActive) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Create Agent", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onPageChange(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Prev",
                tint = if (currentPage > 1) AccentGold else TextMuted
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Page $currentPage of $totalPages",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(
            onClick = { onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next",
                tint = if (currentPage < totalPages) AccentGold else TextMuted
            )
        }
    }
}

// =============================================================================
// MODAL DIALOGS
// =============================================================================

@Composable
private fun CreateAgentDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (agentId: String, name: String, pwd: String, confirm: String, notes: String?) -> Unit
) {
    var agentId by remember { mutableStateOf("") }
    var agentName by remember { mutableStateOf("") }
    var tempPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Agent",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = agentId,
                    onValueChange = { agentId = it.uppercase() },
                    label = { Text("Agent ID (e.g. AGENT_001)") },
                    supportingText = { Text("3-20 characters: UPPERCASE, digits, underscore") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_agent_id_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = agentName,
                    onValueChange = { agentName = it },
                    label = { Text("Agent Full Name / Title") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_agent_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = { tempPassword = it },
                    label = { Text("Temporary Password") },
                    supportingText = { Text("Min 8 chars with letters & numbers") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Password",
                                tint = TextMuted
                            )
                        }
                    },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_agent_temp_pwd_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Temporary Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_agent_confirm_pwd_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 3,
                    colors = customTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_agent_notes_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(agentId, agentName, tempPassword, confirmPassword, notes) },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_create_agent_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Create Agent", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatedCredentialsDialog(
    credentials: CreatedCredentials,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showPassword by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = SuccessGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Agent Account Created",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Text(
                    text = "Temporary credentials must be securely delivered to the Agent. This temporary password will NOT be shown again.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Credential Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0D1420),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Agent ID", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = credentials.agentId,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(credentials.agentId))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = AccentGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Temporary Password", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = if (showPassword) credentials.temporaryPassword else "••••••••••••",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle",
                                        tint = TextMuted
                                    )
                                }
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(credentials.temporaryPassword))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = AccentGold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Have Secured These Credentials", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ResetCredentialsDialog(
    credentials: ResetCredentials,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showPassword by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = AccentGold.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Password Reset Successful",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Text(
                    text = "All prior sessions for '${credentials.agentId}' have been revoked. Securely share this new temporary password.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0D1420),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("New Temporary Password", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = if (showPassword) credentials.temporaryPassword else "••••••••••••",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle",
                                    tint = TextMuted
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(credentials.temporaryPassword))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = AccentGold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AgentDetailsDialog(
    details: AgentDetailsDto?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading || details == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentGold)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agent Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("Agent ID", details.agentId, isMonospace = true)
                    DetailRow("Full Name", details.agentName)
                    DetailRow("Status", details.status, valueColor = if (details.status == "ACTIVE") SuccessGreen else DangerRed)
                    DetailRow("Subordinated Users", "${details.userCount}")
                    DetailRow("Virtual Coin Balance", "${details.virtualCoinBalance} Coins")
                    DetailRow("Created At", formatDate(details.createdAt))
                    DetailRow("Last Login", details.lastLoginAt?.let { formatDate(it) } ?: "Never")
                    details.notes?.let { DetailRow("Notes", it) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEdit()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Edit", fontSize = 12.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onResetPassword()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset Pwd", fontSize = 12.sp, color = AccentGold)
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onToggleStatus()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (details.status == "ACTIVE") DangerRed else SuccessGreen
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (details.status == "ACTIVE") "Suspend" else "Activate", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Recent Activity Audit Ledger
                    if (details.recentActivity.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "RECENT AUDIT LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        details.recentActivity.take(5).forEach { act ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F1521),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = act.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = formatDate(act.createdAt), fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
private fun EditAgentDialog(
    agent: AgentSummaryDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(agent.agentName) }
    var notes by remember { mutableStateOf(agent.notes ?: "") }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Agent Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent Name") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    colors = customTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, notes) },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetPasswordDialog(
    agent: AgentSummaryDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (customTempPassword: String?) -> Unit
) {
    var customPassword by remember { mutableStateOf("") }
    var autoGenerate by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Reset Agent Password",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resetting the password for '${agent.agentId}' will revoke all active sessions immediately.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = autoGenerate,
                        onClick = { autoGenerate = true },
                        label = { Text("Auto-Generate Strong Pwd", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !autoGenerate,
                        onClick = { autoGenerate = false },
                        label = { Text("Custom Pwd", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGold
                        )
                    )
                }

                if (!autoGenerate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customPassword,
                        onValueChange = { customPassword = it },
                        label = { Text("New Temporary Password") },
                        supportingText = { Text("Min 8 chars, letters & numbers") },
                        singleLine = true,
                        colors = customTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pwd = if (autoGenerate) null else customPassword
                            onConfirm(pwd)
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Reset Password", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = CardBorder,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = AccentGold,
    unfocusedLabelColor = TextMuted,
    focusedContainerColor = Color(0xFF0F1521),
    unfocusedContainerColor = Color(0xFF0F1521)
)

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

