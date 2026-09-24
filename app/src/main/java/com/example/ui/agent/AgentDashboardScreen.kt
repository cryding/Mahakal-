package com.example.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.ui.wallet.AgentCoinsDirectoryView
import com.example.ui.wallet.CoinTransactionFailureDialog
import com.example.ui.wallet.CoinTransactionSuccessDialog
import com.example.ui.wallet.DeductCoinScreen
import com.example.ui.wallet.DeductCoinsDialog
import com.example.ui.wallet.TransactionDetailDialog
import com.example.ui.wallet.TransactionLedgerView
import com.example.ui.wallet.TransferCoinScreen
import com.example.ui.wallet.TransferCoinsDialog
import com.example.ui.wallet.UserCoinManagementScreen
import com.example.ui.wallet.WalletBalanceCard
import com.example.ui.wallet.WalletViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
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
import com.example.backend.model.UserCreatedDto
import com.example.backend.model.UserDetailsDto
import com.example.backend.model.UserProfileDto
import com.example.backend.model.UserSummaryDto
import com.example.ui.notification.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val AgentThemeColor = Color(0xFF42A5F5)
private val DarkBg = Color(0xFF0F141C)
private val CardBg = Color(0xFF18202C)
private val CardInnerBg = Color(0xFF222C3D)
private val TextMuted = Color(0xFF7A8B9E)

@Composable
fun AgentDashboardScreen(
    agentUser: UserProfileDto,
    viewModel: AgentUserViewModel,
    walletViewModel: WalletViewModel? = null,
    notificationViewModel: NotificationViewModel? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val walletState = walletViewModel?.uiState?.collectAsState()?.value
    val unreadNotifCount = notificationViewModel?.unreadCount?.collectAsState()?.value ?: 0

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Overview, 1: Users, 2: Coins, 3: Coin Ledger

    // Phase 6 Coin Management States
    var transferTargetUser by remember { mutableStateOf<UserSummaryDto?>(null) }
    var deductTargetUser by remember { mutableStateOf<UserSummaryDto?>(null) }
    var viewTransactionsUser by remember { mutableStateOf<UserSummaryDto?>(null) }

    // Wallet Action Dialog States
    var showTransferCoinsDialog by remember { mutableStateOf(false) }
    var showDeductCoinsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    // Authoritatively sync user balances when users are loaded
    LaunchedEffect(uiState.users) {
        if (uiState.users.isNotEmpty()) {
            walletViewModel?.loadAccountBalances(uiState.users.map { it.id })
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar("Error: $msg")
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(walletState?.actionSuccessMessage) {
        walletState?.actionSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            walletViewModel?.clearFeedback()
        }
    }

    LaunchedEffect(walletState?.actionErrorMessage) {
        walletState?.actionErrorMessage?.let { msg ->
            snackbarHostState.showSnackbar("Wallet Error: $msg")
            walletViewModel?.clearFeedback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF0A0D14))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            AgentTopBar(
                agentUser = agentUser,
                unreadCount = unreadNotifCount,
                onOpenNotifications = onOpenNotifications,
                onOpenProfile = { showProfileDialog = true },
                onOpenSecurity = { showSecurityDialog = true },
                onLogout = onLogout
            )

            // Tabs: Overview vs Users vs Coins vs Ledger
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkBg,
                contentColor = AgentThemeColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AgentThemeColor,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "OVERVIEW",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    selectedContentColor = AgentThemeColor,
                    unselectedContentColor = TextMuted,
                    modifier = Modifier.testTag("tab_overview")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "USERS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    selectedContentColor = AgentThemeColor,
                    unselectedContentColor = TextMuted,
                    modifier = Modifier.testTag("tab_users")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        walletViewModel?.loadBalance()
                        if (uiState.users.isNotEmpty()) {
                            walletViewModel?.loadAccountBalances(uiState.users.map { it.id })
                        }
                    },
                    text = {
                        Text(
                            text = "COINS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    selectedContentColor = AgentThemeColor,
                    unselectedContentColor = TextMuted,
                    modifier = Modifier.testTag("tab_coins")
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = {
                        selectedTabIndex = 3
                        walletViewModel?.loadTransactions()
                    },
                    text = {
                        Text(
                            text = "LEDGER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    selectedContentColor = AgentThemeColor,
                    unselectedContentColor = TextMuted,
                    modifier = Modifier.testTag("tab_ledger")
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> AgentOverviewTab(
                        agentUser = agentUser,
                        uiState = uiState,
                        walletBalance = walletState?.myBalance?.balance ?: 0L,
                        isLoadingWallet = walletState?.isLoadingBalance ?: false,
                        onRefreshWallet = { walletViewModel?.loadBalance() },
                        onTransferCoins = { showTransferCoinsDialog = true },
                        onDeductCoins = { showDeductCoinsDialog = true },
                        onNavigateToUsers = { selectedTabIndex = 1 },
                        onNavigateToLedger = {
                            selectedTabIndex = 3
                            walletViewModel?.loadTransactions()
                        },
                        onOpenCreate = { viewModel.openCreateDialog() }
                    )
                    1 -> AgentUsersTab(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    2 -> AgentCoinsDirectoryView(
                        agentBalance = walletState?.myBalance?.balance ?: 0L,
                        users = uiState.users,
                        accountBalances = walletState?.accountBalances ?: emptyMap(),
                        isLoading = uiState.isLoading || (walletState?.isLoadingBalances ?: false),
                        onRefresh = {
                            viewModel.loadUsers()
                            walletViewModel?.loadBalance()
                            if (uiState.users.isNotEmpty()) {
                                walletViewModel?.loadAccountBalances(uiState.users.map { it.id })
                            }
                        },
                        onTransferCoins = { user ->
                            transferTargetUser = user
                        },
                        onDeductCoins = { user ->
                            deductTargetUser = user
                        },
                        onViewTransactions = { user ->
                            viewTransactionsUser = user
                            walletViewModel?.loadAccountSummary(user.id)
                        }
                    )
                    3 -> TransactionLedgerView(
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
            }
        }

        // Floating Action Button to Create User
        FloatingActionButton(
            onClick = { viewModel.openCreateDialog() },
            containerColor = AgentThemeColor,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_user_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create New User")
        }

        // Dialogs
        if (uiState.isCreateDialogOpen) {
            CreateUserDialog(
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.closeCreateDialog() },
                onSubmit = { uid, name, pwd, notes ->
                    viewModel.createUser(uid, name, pwd, notes)
                }
            )
        }

        // Secure Confirmation Dialog (One-Time Credentials Delivery)
        uiState.createdCredentials?.let { creds ->
            SecureConfirmationDialog(
                credentials = creds,
                onDismiss = { viewModel.dismissCreatedCredentials() }
            )
        }

        // User Details Dialog
        uiState.selectedUserDetails?.let { details ->
            UserDetailsDialog(
                details = details,
                onDismiss = { viewModel.closeUserDetails() },
                onEdit = {
                    viewModel.closeUserDetails()
                    viewModel.openEditDialog(
                        UserSummaryDto(
                            id = details.id,
                            userId = details.userId,
                            displayName = details.displayName,
                            agentId = details.agentId,
                            status = details.status,
                            createdAt = details.createdAt,
                            lastLoginAt = details.lastLoginAt,
                            notes = details.notes
                        )
                    )
                },
                onSuspend = {
                    viewModel.closeUserDetails()
                    viewModel.openSuspendDialog(
                        UserSummaryDto(
                            id = details.id,
                            userId = details.userId,
                            displayName = details.displayName,
                            agentId = details.agentId,
                            status = details.status,
                            createdAt = details.createdAt,
                            lastLoginAt = details.lastLoginAt,
                            notes = details.notes
                        )
                    )
                },
                onActivate = {
                    viewModel.closeUserDetails()
                    viewModel.openActivateDialog(
                        UserSummaryDto(
                            id = details.id,
                            userId = details.userId,
                            displayName = details.displayName,
                            agentId = details.agentId,
                            status = details.status,
                            createdAt = details.createdAt,
                            lastLoginAt = details.lastLoginAt,
                            notes = details.notes
                        )
                    )
                },
                onResetPassword = {
                    viewModel.closeUserDetails()
                    viewModel.openResetPasswordDialog(
                        UserSummaryDto(
                            id = details.id,
                            userId = details.userId,
                            displayName = details.displayName,
                            agentId = details.agentId,
                            status = details.status,
                            createdAt = details.createdAt,
                            lastLoginAt = details.lastLoginAt,
                            notes = details.notes
                        )
                    )
                }
            )
        }

        // Edit Dialog
        uiState.userToEdit?.let { user ->
            EditUserDialog(
                user = user,
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.closeEditDialog() },
                onConfirm = { name, notes ->
                    viewModel.updateUser(user.id, name, notes)
                }
            )
        }

        // Suspend Confirmation Dialog
        uiState.userToSuspend?.let { user ->
            SuspendUserConfirmationDialog(
                user = user,
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.closeSuspendDialog() },
                onConfirm = { viewModel.suspendUser(user.id) }
            )
        }

        // Activate Confirmation Dialog
        uiState.userToActivate?.let { user ->
            ActivateUserConfirmationDialog(
                user = user,
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.closeActivateDialog() },
                onConfirm = { viewModel.activateUser(user.id) }
            )
        }

        // Reset Password Dialog
        uiState.userToResetPassword?.let { user ->
            ResetUserPasswordDialog(
                user = user,
                isLoading = uiState.isLoading,
                onDismiss = { viewModel.closeResetPasswordDialog() },
                onConfirm = { customPwd ->
                    viewModel.resetPassword(user.id, customPwd)
                }
            )
        }

        // Phase 5: Transfer Coins Dialog (Agent to User)
        if (showTransferCoinsDialog) {
            val userCandidates = uiState.users
                .filter { it.status == "ACTIVE" }
                .map { it.id to "${it.userId} (${it.displayName})" }

            TransferCoinsDialog(
                role = "AGENT",
                availableBalance = walletState?.myBalance?.balance ?: 0L,
                candidates = userCandidates,
                isLoadingCandidates = uiState.isLoading,
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

        // Phase 5: Deduct Coins Dialog (Agent from User)
        if (showDeductCoinsDialog) {
            val userCandidates = uiState.users
                .filter { it.status == "ACTIVE" }
                .map { it.id to "${it.userId} (${it.displayName})" }

            DeductCoinsDialog(
                role = "AGENT",
                candidates = userCandidates,
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

        // Phase 5: Transaction Detail Inspector Dialog
        walletState?.selectedTransaction?.let { tx ->
            TransactionDetailDialog(
                transaction = tx,
                currentUserRole = "AGENT",
                onDismiss = { walletViewModel?.selectTransaction(null) }
            )
        }

        // Phase 6: Transfer Coin Screen Overlay (Agent to User)
        transferTargetUser?.let { user ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                TransferCoinScreen(
                    role = "AGENT",
                    senderName = "${agentUser.fullName} (Agent)",
                    availableBalance = walletState?.myBalance?.balance ?: 0L,
                    recipientAccountId = user.id,
                    recipientDisplayName = "${user.displayName} (${user.userId})",
                    isSubmitting = walletState?.isSubmittingAction ?: false,
                    onBack = { transferTargetUser = null },
                    onSubmitTransfer = { amount, reason ->
                        walletViewModel?.transferCoins(
                            destinationAccountId = user.id,
                            amount = amount,
                            reason = reason
                        ) {
                            transferTargetUser = null
                            if (uiState.users.isNotEmpty()) {
                                walletViewModel.loadAccountBalances(uiState.users.map { it.id })
                            }
                        }
                    }
                )
            }
        }

        // Phase 6: Deduct Coin Screen Overlay (Agent from User)
        deductTargetUser?.let { user ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                val currentTargetBal = walletState?.accountBalances?.get(user.id) ?: 0L
                DeductCoinScreen(
                    role = "AGENT",
                    operatorName = "${agentUser.fullName} (Agent)",
                    targetAccountId = user.id,
                    targetDisplayName = "${user.displayName} (${user.userId})",
                    targetCurrentBalance = currentTargetBal,
                    isSubmitting = walletState?.isSubmittingAction ?: false,
                    onBack = { deductTargetUser = null },
                    onSubmitDeduct = { amount, reason ->
                        walletViewModel?.deductCoins(
                            targetAccountId = user.id,
                            amount = amount,
                            reason = reason
                        ) {
                            deductTargetUser = null
                            if (uiState.users.isNotEmpty()) {
                                walletViewModel.loadAccountBalances(uiState.users.map { it.id })
                            }
                        }
                    }
                )
            }
        }

        // Phase 6: User Coin Management Screen Overlay
        viewTransactionsUser?.let { user ->
            walletState?.selectedAccountSummary?.let { summary ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                ) {
                    UserCoinManagementScreen(
                        userSummary = summary,
                        isLoading = walletState.isLoadingAccountSummary,
                        onBack = {
                            viewTransactionsUser = null
                            walletViewModel?.clearAccountSummary()
                        },
                        onTransferClick = {
                            transferTargetUser = user
                        },
                        onDeductClick = {
                            deductTargetUser = user
                        },
                        onRefresh = {
                            walletViewModel?.loadAccountSummary(user.id)
                        },
                        onSelectTransaction = { tx ->
                            walletViewModel?.selectTransaction(tx)
                        }
                    )
                }
            }
        }

        // Phase 6: Authoritative Success Dialog
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

        // Phase 6: Authoritative Failure Dialog
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

        // Agent Profile Dialog
        if (showProfileDialog) {
            AgentProfileDialog(
                agentUser = agentUser,
                onDismiss = { showProfileDialog = false },
                onLogout = {
                    showProfileDialog = false
                    onLogout()
                }
            )
        }

        // Agent Security Dialog
        if (showSecurityDialog) {
            AgentSecurityDialog(
                agentUser = agentUser,
                onDismiss = { showSecurityDialog = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AgentTopBar(
    agentUser: UserProfileDto,
    unreadCount: Int = 0,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenProfile: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onLogout: () -> Unit
) {
    Surface(
        color = CardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenProfile() }
            ) {
                Surface(
                    shape = CircleShape,
                    color = AgentThemeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = "Agent Icon",
                            tint = AgentThemeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AGENT PORTAL",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${agentUser.fullName} (${agentUser.loginId})",
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
                                .testTag("agent_notifications_btn")
                        ) {
                            Icon(
                                imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadCount > 0) Color(0xFFE5A93C) else Color(0xFF7A8B9E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = onOpenSecurity,
                    modifier = Modifier.size(36.dp).testTag("agent_security_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Details",
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.size(36.dp).testTag("agent_profile_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Details",
                        tint = AgentThemeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(36.dp).testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentOverviewTab(
    agentUser: UserProfileDto,
    uiState: AgentUserUiState,
    walletBalance: Long,
    isLoadingWallet: Boolean,
    onRefreshWallet: () -> Unit,
    onTransferCoins: () -> Unit,
    onDeductCoins: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onOpenCreate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Phase 5: Virtual Coin Balance Card for Agent
        WalletBalanceCard(
            balance = walletBalance,
            isLoading = isLoadingWallet,
            role = "AGENT",
            onRefresh = onRefreshWallet,
            onTransferClick = onTransferCoins,
            onDeductClick = onDeductCoins,
            onViewLedgerClick = onNavigateToLedger
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Metrics
        Text(
            text = "SUBORDINATE USER METRICS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "TOTAL USERS",
                value = uiState.totalCount.toString(),
                color = AgentThemeColor,
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f)
            )
            val activeCount = uiState.users.count { it.status == "ACTIVE" }
            MetricCard(
                title = "ACTIVE",
                value = activeCount.toString(),
                color = Color(0xFF66BB6A),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            val suspendedCount = uiState.users.count { it.status == "SUSPENDED" }
            MetricCard(
                title = "SUSPENDED",
                value = suspendedCount.toString(),
                color = Color(0xFFEF5350),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Agent Account Controls",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage player user accounts subordinated strictly to your Agent ID (${agentUser.loginId}).",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onOpenCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = AgentThemeColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overview_create_user_button")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create User", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToUsers,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overview_view_users_button")
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Directory")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase 4 Architecture Guard Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardInnerBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = AgentThemeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phase 4 Security Architecture Enforced",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Ownership Derivation: User.agentId is derived strictly from your authenticated session.\n" +
                            "• Zero Credential Exposure: Password hashes, salts, and session tokens are strictly excluded from all read operations.\n" +
                            "• Session Revocation: Suspending or resetting password automatically invalidates active user sessions.\n" +
                            "• Isolated Ledger: Cross-agent access is blocked with strict RBAC ownership validation.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun AgentUsersTab(
    uiState: AgentUserUiState,
    viewModel: AgentUserViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Filters Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Search TextField
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by User ID or Display Name...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AgentThemeColor,
                    unfocusedBorderColor = CardInnerBg,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips: ALL, ACTIVE, SUSPENDED
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "ACTIVE", "SUSPENDED").forEach { filter ->
                    FilterChip(
                        selected = uiState.statusFilter == filter,
                        onClick = { viewModel.setStatusFilter(filter) },
                        label = {
                            Text(
                                text = filter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AgentThemeColor.copy(alpha = 0.2f),
                            selectedLabelColor = AgentThemeColor,
                            containerColor = CardBg,
                            labelColor = TextMuted
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { viewModel.loadUsers() },
                    modifier = Modifier.size(32.dp).testTag("refresh_users_button")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Users List
        if (uiState.isLoading && uiState.users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AgentThemeColor)
            }
        } else if (uiState.users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) "No users found matching query." else "No subordinate users yet.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.openCreateDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = AgentThemeColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("empty_state_create_button")
                    ) {
                        Text("Create First User", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("users_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        onClick = { viewModel.openUserDetails(user.id) },
                        onEdit = { viewModel.openEditDialog(user) },
                        onSuspend = { viewModel.openSuspendDialog(user) },
                        onActivate = { viewModel.openActivateDialog(user) },
                        onResetPassword = { viewModel.openResetPasswordDialog(user) }
                    )
                }

                item {
                    // Pagination Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setPage(uiState.currentPage - 1) },
                            enabled = uiState.currentPage > 1,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("prev_page_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }

                        Text(
                            text = "Page ${uiState.currentPage} of ${uiState.totalPages} (${uiState.totalCount} users)",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("pagination_label")
                        )

                        OutlinedButton(
                            onClick = { viewModel.setPage(uiState.currentPage + 1) },
                            enabled = uiState.currentPage < uiState.totalPages,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("next_page_button")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: UserSummaryDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSuspend: () -> Unit,
    onActivate: () -> Unit,
    onResetPassword: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("user_card_${user.userId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AgentThemeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.userId.take(2).uppercase(),
                            color = AgentThemeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.userId,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(status = user.status)
                    }
                    Text(
                        text = user.displayName,
                        color = Color(0xFFCFD8DC),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Created: ${formatTimestamp(user.createdAt)}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.testTag("user_menu_${user.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "User Actions",
                        tint = TextMuted
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    modifier = Modifier.background(CardInnerBg)
                ) {
                    DropdownMenuItem(
                        text = { Text("View Details", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AgentThemeColor) },
                        onClick = {
                            isMenuOpen = false
                            onClick()
                        },
                        modifier = Modifier.testTag("menu_view_details")
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Profile", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) },
                        onClick = {
                            isMenuOpen = false
                            onEdit()
                        },
                        modifier = Modifier.testTag("menu_edit_user")
                    )
                    if (user.status == "ACTIVE") {
                        DropdownMenuItem(
                            text = { Text("Suspend Account", color = Color(0xFFFF8A80)) },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF8A80)) },
                            onClick = {
                                isMenuOpen = false
                                onSuspend()
                            },
                            modifier = Modifier.testTag("menu_suspend_user")
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Activate Account", color = Color(0xFF81C784)) },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784)) },
                            onClick = {
                                isMenuOpen = false
                                onActivate()
                            },
                            modifier = Modifier.testTag("menu_activate_user")
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Reset Password", color = Color(0xFFFFD54F)) },
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFFFFD54F)) },
                        onClick = {
                            isMenuOpen = false
                            onResetPassword()
                        },
                        modifier = Modifier.testTag("menu_reset_password")
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "ACTIVE" -> Pair(Color(0xFF2E7D32).copy(alpha = 0.25f), Color(0xFF81C784))
        "SUSPENDED" -> Pair(Color(0xFFC62828).copy(alpha = 0.25f), Color(0xFFFF8A80))
        else -> Pair(Color(0xFF455A64).copy(alpha = 0.25f), Color(0xFFB0BEC5))
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = status.uppercase(),
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Dialog: Create New User
 */
@Composable
private fun CreateUserDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (userId: String, displayName: String, password: String, notes: String?) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_user_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Subordinate User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Provision a player account strictly owned by your Agent ID.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // User ID Input
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it.uppercase().trim() },
                    label = { Text("User ID (e.g. USR10001)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgentThemeColor,
                        unfocusedBorderColor = CardInnerBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_id_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Display Name Input
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name / Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgentThemeColor,
                        unfocusedBorderColor = CardInnerBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Temporary Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Temporary Password") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgentThemeColor,
                        unfocusedBorderColor = CardInnerBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("temp_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgentThemeColor,
                        unfocusedBorderColor = CardInnerBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes Input (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional, max 500 chars)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgentThemeColor,
                        unfocusedBorderColor = CardInnerBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input")
                )

                validationError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        modifier = Modifier.testTag("validation_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmedId = userId.trim().uppercase()
                            if (trimmedId.length < 3 || trimmedId.length > 20) {
                                validationError = "User ID must be 3 to 20 characters."
                                return@Button
                            }
                            if (displayName.trim().length < 2) {
                                validationError = "Display name must be at least 2 characters."
                                return@Button
                            }
                            if (password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isLetter() }) {
                                validationError = "Password must be at least 8 chars and contain letters and numbers."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                validationError = "Passwords do not match."
                                return@Button
                            }
                            validationError = null
                            onSubmit(trimmedId, displayName.trim(), password, notes.ifBlank { null })
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AgentThemeColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("submit_create_user_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Create User", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Secure Confirmation Dialog: One-Time Temporary Credentials Display
 */
@Composable
private fun SecureConfirmationDialog(
    credentials: UserCreatedDto,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isPasswordVisible by remember { mutableStateOf(false) }
    var copiedId by remember { mutableStateOf(false) }
    var copiedPwd by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("secure_credentials_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "User Credentials Generated",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Temporary credentials are ready for delivery. Provide these securely to the user.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Credentials Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardInnerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // User ID Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("USER ID", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Text(
                                    text = credentials.userId,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("confirmed_user_id")
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(credentials.userId))
                                    copiedId = true
                                },
                                modifier = Modifier.testTag("copy_user_id_button")
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy ID",
                                    tint = if (copiedId) Color(0xFF81C784) else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Temporary Password Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TEMPORARY PASSWORD", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                val pwd = credentials.temporaryPassword ?: "********"
                                Text(
                                    text = if (isPasswordVisible) pwd else "•".repeat(pwd.length.coerceAtLeast(8)),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("confirmed_password_text")
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible },
                                    modifier = Modifier.testTag("show_hide_password_button")
                                ) {
                                    Icon(
                                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                credentials.temporaryPassword?.let { rawPwd ->
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(rawPwd))
                                            copiedPwd = true
                                        },
                                        modifier = Modifier.testTag("copy_password_button")
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy Password",
                                            tint = if (copiedPwd) Color(0xFF81C784) else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Warning Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Once you close this dialog, the temporary password CANNOT be retrieved again through normal APIs. The user must change it upon their first login.",
                        color = Color(0xFFFFB74D),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AgentThemeColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_credentials_button")
                ) {
                    Text("Done / Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Dialog: User Details Inspector
 */
@Composable
private fun UserDetailsDialog(
    details: UserDetailsDto,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSuspend: () -> Unit,
    onActivate: () -> Unit,
    onResetPassword: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_details_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detail Attributes
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardInnerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        DetailItem("User ID", details.userId)
                        DetailItem("Display Name", details.displayName)
                        DetailItem("Account Status", details.status)
                        DetailItem("Parent Agent ID", details.agentId)
                        details.agentName?.let { DetailItem("Parent Agent Name", it) }
                        DetailItem("Created At", formatTimestamp(details.createdAt))
                        details.lastLoginAt?.let { DetailItem("Last Login", formatTimestamp(it)) }
                        DetailItem("Must Change Password", if (details.mustChangePassword) "YES (Temporary active)" else "NO")
                        details.notes?.let { DetailItem("Internal Notes", it) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls
                Text("Lifecycle Actions", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("dialog_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }

                    if (details.status == "ACTIVE") {
                        OutlinedButton(
                            onClick = onSuspend,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                            modifier = Modifier.weight(1f).testTag("dialog_suspend_button")
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suspend", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onActivate,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81C784)),
                            modifier = Modifier.weight(1f).testTag("dialog_activate_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Activate", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onResetPassword,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F)),
                        modifier = Modifier.weight(1f).testTag("dialog_reset_pwd_button")
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Pwd", fontSize = 12.sp)
                    }
                }

                // Recent Audit Activity Log
                if (details.recentActivity.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recent Audit History", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    details.recentActivity.forEach { act ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = AgentThemeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "${act.action} (${act.actorRole})",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatTimestamp(act.createdAt),
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Dialog: Edit User Profile
 */
@Composable
private fun EditUserDialog(
    user: UserSummaryDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (displayName: String, notes: String?) -> Unit
) {
    var displayName by remember { mutableStateOf(user.displayName) }
    var notes by remember { mutableStateOf(user.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Edit User Profile (${user.userId})", color = Color.White, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgentThemeColor,
                        unfocusedBorderColor = CardInnerBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Notes") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgentThemeColor,
                        unfocusedBorderColor = CardInnerBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_notes_input")
                )
                errorMsg?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.trim().length < 2) {
                        errorMsg = "Display Name must be at least 2 characters."
                        return@Button
                    }
                    onConfirm(displayName.trim(), notes.ifBlank { null })
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AgentThemeColor),
                modifier = Modifier.testTag("confirm_edit_user_button")
            ) {
                Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

/**
 * Dialog: Suspend User Confirmation
 */
@Composable
private fun SuspendUserConfirmationDialog(
    user: UserSummaryDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Suspend User ${user.userId}?", color = Color(0xFFFF8A80), fontSize = 16.sp) },
        text = {
            Text(
                "Suspending this user account will immediately revoke all active sessions and block further logins. " +
                        "This action is logged to the immutable audit trail.",
                color = TextMuted,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.testTag("confirm_suspend_user_button")
            ) {
                Text("Suspend User", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

/**
 * Dialog: Activate User Confirmation
 */
@Composable
private fun ActivateUserConfirmationDialog(
    user: UserSummaryDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Activate User ${user.userId}?", color = Color.White, fontSize = 16.sp) },
        text = {
            Text(
                "Activating this user account will restore their login eligibility.",
                color = TextMuted,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                modifier = Modifier.testTag("confirm_activate_user_button")
            ) {
                Text("Activate User", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

/**
 * Dialog: Reset User Password
 */
@Composable
private fun ResetUserPasswordDialog(
    user: UserSummaryDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (customTempPassword: String?) -> Unit
) {
    var customPwd by remember { mutableStateOf("") }
    var useAutoGenerate by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Reset Password for ${user.userId}", color = Color.White, fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    "Resetting password will immediately invalidate any current active sessions for this user. " +
                            "A temporary password will be issued.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    FilterChip(
                        selected = useAutoGenerate,
                        onClick = { useAutoGenerate = true },
                        label = { Text("Auto-Generate", fontSize = 11.sp) },
                        modifier = Modifier.testTag("chip_auto_generate")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !useAutoGenerate,
                        onClick = { useAutoGenerate = false },
                        label = { Text("Custom Temp Pwd", fontSize = 11.sp) },
                        modifier = Modifier.testTag("chip_custom_pwd")
                    )
                }

                if (!useAutoGenerate) {
                    OutlinedTextField(
                        value = customPwd,
                        onValueChange = { customPwd = it },
                        label = { Text("Custom Temporary Password") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgentThemeColor,
                            unfocusedBorderColor = CardInnerBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_temp_pwd_input")
                    )
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!useAutoGenerate) {
                        if (customPwd.length < 8 || !customPwd.any { it.isDigit() } || !customPwd.any { it.isLetter() }) {
                            errorMsg = "Password must be at least 8 chars and contain letters and numbers."
                            return@Button
                        }
                        onConfirm(customPwd)
                    } else {
                        onConfirm(null)
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                modifier = Modifier.testTag("confirm_reset_pwd_button")
            ) {
                Text("Reset Password", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun AgentProfileDialog(
    agentUser: UserProfileDto,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131A24),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agent Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AgentThemeColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgentThemeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "AGENT",
                        color = AgentThemeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1C2433),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileRow(label = "Full Name", value = agentUser.fullName)
                        ProfileRow(label = "Login ID", value = agentUser.loginId)
                        ProfileRow(label = "Account ID", value = agentUser.id)
                        ProfileRow(label = "Assigned Role", value = agentUser.role)
                        ProfileRow(label = "Account Status", value = agentUser.status, valueColor = Color(0xFF81C784))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF18202C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Agent Responsibilities",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "You are authorized to manage users, distribute virtual game coins, and oversee game participation exclusively for accounts registered under your agent umbrella.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("agent_profile_logout_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Logout", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted)
            }
        }
    )
}

@Composable
fun AgentSecurityDialog(
    agentUser: UserProfileDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131A24),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security & Governance",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1C2433),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileRow(label = "Session Protocol", value = "Encrypted JWT Bearer")
                        ProfileRow(label = "Hash Algorithm", value = "Argon2id (RFC 9106)")
                        ProfileRow(label = "Agent Scope Boundary", value = "Restricted to Agent-Owned Users Only")
                        ProfileRow(label = "Authoritative Balance", value = "Server-Enforced (Zero Local Math)")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222C3D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Anti-Tamper & Isolation Compliance",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Agents cannot query, transfer to, or view ledger activities of users belonging to other agents. Every transaction is authoritatively validated and signed with double-entry idempotency keys.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AgentThemeColor, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ProfileRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
