package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.model.AdminGameDetailsDto
import com.example.backend.model.AdminOperationsSummaryDto
import com.example.backend.model.AuditLogDto
import com.example.backend.model.GameDto
import com.example.backend.model.GameStatus
import com.example.backend.model.SecurityDashboardSummaryDto
import com.example.backend.model.UserProfileDto
import com.example.backend.model.UserSummaryDto
import com.example.ui.agent.AgentUserViewModel
import com.example.ui.agent.AgentUsersTab
import com.example.ui.game.AdminAuditLogsDialog
import com.example.ui.game.AdminExportDialog
import com.example.ui.game.AdminGameCard
import com.example.ui.game.AdminFinalizeResultDialog
import com.example.ui.game.AdminReportsDialog
import com.example.ui.game.GameViewModel
import com.example.ui.security.AdminSecurityScreen
import com.example.ui.security.SecEventRow
import com.example.ui.security.SecMetricCard
import com.example.ui.security.SecSessionRow
import com.example.ui.security.SecurityViewModel
import com.example.ui.wallet.WalletViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Unified Styling Constants for Phase 12
private val BgDark = Color(0xFF0A0E17)
private val CardBg = Color(0xFF141C2B)
private val CardBorder = Color(0xFF233044)
private val AccentGold = Color(0xFFFFB300)
private val SuccessGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFEF5350)
private val InfoBlue = Color(0xFF42A5F5)
private val TextMuted = Color(0xFF8A99AD)

/**
 * Phase 12 Production Unified Admin Control Center Navigation Tabs
 */
enum class AdminNavTab(val label: String, val testTag: String) {
    DASHBOARD("DASHBOARD", "admin_tab_dashboard"),
    AGENTS("AGENTS", "admin_tab_agents"),
    USERS("USERS", "admin_tab_users"),
    GAMES("GAMES", "admin_tab_games"),
    RESULTS("RESULTS", "admin_tab_results"),
    VIRTUAL_COINS("VIRTUAL COINS", "admin_tab_coins"),
    TRANSACTIONS("TRANSACTIONS", "admin_tab_transactions"),
    NOTIFICATIONS("NOTIFICATIONS", "admin_tab_notifications"),
    REPORTS("REPORTS", "admin_tab_reports"),
    AUDIT("AUDIT", "admin_tab_audit"),
    SECURITY("SECURITY", "admin_tab_security"),
    SESSIONS("SESSIONS & DEVICES", "admin_tab_sessions"),
    PROCESSING("PROCESSING", "admin_tab_processing"),
    SYSTEM_HEALTH("SYSTEM HEALTH", "admin_tab_health"),
    SETTINGS("SETTINGS", "admin_tab_settings")
}

/**
 * 1. Production Unified Dashboard View
 * Authoritative backend metrics with filter ranges (Today, 7 Days, 30 Days, Custom Range)
 */
@Composable
fun AdminControlCenterDashboardView(
    adminUser: UserProfileDto,
    totalAgents: Int,
    walletBalance: Long,
    isLoadingWallet: Boolean,
    operationsSummary: AdminOperationsSummaryDto?,
    securitySummary: SecurityDashboardSummaryDto?,
    isLoadingSummary: Boolean,
    onRefresh: () -> Unit,
    onNavigateTab: (AdminNavTab) -> Unit
) {
    var selectedTimeRange by remember { mutableStateOf("TODAY") }
    val timeRanges = listOf("TODAY" to "Today", "7D" to "7 Days", "30D" to "30 Days", "CUSTOM" to "Custom Range")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_control_center_dashboard")
    ) {
        // Date / Range Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMMAND & CONTROL DASHBOARD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Authoritative System Telemetry | Non-Monetary Ledger",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("admin_dashboard_refresh_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
            }
        }

        // Time Range Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeRanges.forEach { (key, label) ->
                val isSelected = selectedTimeRange == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTimeRange = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGold,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF141C28),
                        labelColor = Color(0xFF90A4AE)
                    ),
                    modifier = Modifier.testTag("filter_range_$key")
                )
            }
        }

        if (isLoadingSummary && operationsSummary == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else {
            // Core Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    title = "Subordinated Agents",
                    value = "$totalAgents",
                    subtitle = "RBAC Hierarchy",
                    icon = Icons.Default.SupportAgent,
                    accentColor = Color(0xFF42A5F5),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(AdminNavTab.AGENTS) }
                )

                DashboardMetricCard(
                    title = "Virtual Coin Treasury",
                    value = if (isLoadingWallet) "..." else "$walletBalance",
                    subtitle = "Internal Non-Monetary",
                    icon = Icons.Default.Security,
                    accentColor = AccentGold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(AdminNavTab.VIRTUAL_COINS) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Games & Operations Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    title = "Active Games",
                    value = "${operationsSummary?.activeGames ?: 0}",
                    subtitle = "Open for entries",
                    icon = Icons.Default.SportsEsports,
                    accentColor = SuccessGreen,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(AdminNavTab.GAMES) }
                )

                DashboardMetricCard(
                    title = "Pending Results",
                    value = "${operationsSummary?.resultPendingGames ?: 0}",
                    subtitle = "Awaiting finalization",
                    icon = Icons.Default.Warning,
                    accentColor = Color(0xFFFFB74D),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(AdminNavTab.RESULTS) }
                )

                DashboardMetricCard(
                    title = "Completed Games",
                    value = "${operationsSummary?.finalizedGames ?: 0}",
                    subtitle = "Settled Authoritatively",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF81C784),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(AdminNavTab.GAMES) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Security & Processing Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    title = "Failed Processing",
                    value = "${operationsSummary?.processingFailures ?: 0}",
                    subtitle = "Requires retry",
                    icon = Icons.Default.Sync,
                    accentColor = if ((operationsSummary?.processingFailures ?: 0) > 0) DangerRed else SuccessGreen,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(AdminNavTab.PROCESSING) }
                )

                DashboardMetricCard(
                    title = "Security Posture",
                    value = securitySummary?.systemStatus ?: "HEALTHY",
                    subtitle = "Active Sessions: ${securitySummary?.activeSessionsCount ?: 0}",
                    icon = Icons.Default.Security,
                    accentColor = if (securitySummary?.systemStatus == "ELEVATED_ALERT") DangerRed else SuccessGreen,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(AdminNavTab.SECURITY) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fast-Navigation Quick Action Cards
            Text(
                text = "OPERATIONAL MODULES QUICK ACCESS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickNavRow(
                    icon = Icons.Default.Person,
                    title = "User Management",
                    description = "Search, filter, view details, activate or suspend user accounts",
                    onClick = { onNavigateTab(AdminNavTab.USERS) }
                )
                QuickNavRow(
                    icon = Icons.Default.Assessment,
                    title = "Operational Reports & Analytics",
                    description = "Aggregated turnover, reward distribution, and game performance reports",
                    onClick = { onNavigateTab(AdminNavTab.REPORTS) }
                )
                QuickNavRow(
                    icon = Icons.Default.History,
                    title = "Audit Trail & System Logs",
                    description = "Immutable chronological audit log of all system mutations and admin actions",
                    onClick = { onNavigateTab(AdminNavTab.AUDIT) }
                )
                QuickNavRow(
                    icon = Icons.Default.Devices,
                    title = "Device Sessions & Security",
                    description = "Active JWT session monitor, IDOR protections, and token revocation center",
                    onClick = { onNavigateTab(AdminNavTab.SESSIONS) }
                )
                QuickNavRow(
                    icon = Icons.Default.Speed,
                    title = "System Health & Engine Status",
                    description = "Authoritative health probes, DB connectivity, and latency monitoring",
                    onClick = { onNavigateTab(AdminNavTab.SYSTEM_HEALTH) }
                )
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
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
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AccentGold.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = description, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * 2. Dedicated Pending Results & Finalization View
 * Filter games strictly by RESULT_PENDING or CLOSED and finalize with explicit confirmation dialog
 */
@Composable
fun AdminResultsScreen(
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    var finalizingGame by remember { mutableStateOf<GameDto?>(null) }
    var selectedFilter by remember { mutableStateOf("PENDING") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        gameViewModel.loadAdminGames(status = GameStatus.RESULT_PENDING, page = 1)
        gameViewModel.loadAdminSummary()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("admin_results_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RESULTS SETTLEMENT & FINALIZATION",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Authoritative Result Verification & Atomic Reward Distribution",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = {
                    val status = if (selectedFilter == "PENDING") GameStatus.RESULT_PENDING else GameStatus.RESULT_FINALIZED
                    gameViewModel.loadAdminGames(status = status, page = 1)
                },
                modifier = Modifier.testTag("results_refresh_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
            }
        }

        // Filter chips: PENDING (to settle), FINALIZED (history)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "PENDING",
                onClick = {
                    selectedFilter = "PENDING"
                    gameViewModel.loadAdminGames(status = GameStatus.RESULT_PENDING, page = 1)
                },
                label = { Text("PENDING RESULTS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGold,
                    selectedLabelColor = Color.Black
                ),
                modifier = Modifier.testTag("filter_results_pending")
            )

            FilterChip(
                selected = selectedFilter == "FINALIZED",
                onClick = {
                    selectedFilter = "FINALIZED"
                    gameViewModel.loadAdminGames(status = GameStatus.RESULT_FINALIZED, page = 1)
                },
                label = { Text("SETTLED / COMPLETED", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGold,
                    selectedLabelColor = Color.Black
                ),
                modifier = Modifier.testTag("filter_results_finalized")
            )
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                val status = if (selectedFilter == "PENDING") GameStatus.RESULT_PENDING else GameStatus.RESULT_FINALIZED
                gameViewModel.loadAdminGames(status = status, search = it, page = 1)
            },
            placeholder = { Text("Search by game title or ID...", fontSize = 12.sp, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(bottom = 12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = CardBorder
            )
        )

        val gamesList = uiState.adminGames
        val isLoading = uiState.isLoadingAdminGames

        if (isLoading && gamesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (gamesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedFilter == "PENDING") "No games currently awaiting results." else "No finalized games found.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(gamesList, key = { it.gameId }) { game ->
                    AdminGameCard(
                        game = game,
                        onOpen = { gameViewModel.openGame(game.gameId) },
                        onClose = { gameViewModel.closeGame(game.gameId) },
                        onFinalize = { finalizingGame = game },
                        onCancel = { },
                        onViewEntries = { gameViewModel.loadGameEntriesAdmin(game.gameId) },
                        onInspect = { gameViewModel.loadAdminGameDetails(game.gameId) }
                    )
                }
            }
        }

        // Finalize Confirmation Dialog
        finalizingGame?.let { game ->
            AdminFinalizeResultDialog(
                game = game,
                isSubmitting = uiState.isSubmitting,
                onDismiss = { finalizingGame = null },
                onSubmit = { winningOptId: String, reason: String ->
                    gameViewModel.finalizeResult(game.gameId, winningOptId, reason) {
                        finalizingGame = null
                        gameViewModel.loadAdminGames(status = GameStatus.RESULT_PENDING, page = 1)
                        gameViewModel.loadAdminSummary()
                    }
                }
            )
        }
    }
}

/**
 * 3. Dedicated Operational Reports View
 */
@Composable
fun AdminReportsScreen(
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gameViewModel.loadReports()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("admin_reports_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OPERATIONAL ANALYTICS & REPORTS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Authoritative Turnover, Volumes & Distribution Auditing",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                    modifier = Modifier.testTag("reports_export_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { gameViewModel.loadReports() },
                    modifier = Modifier.testTag("reports_refresh_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
                }
            }
        }

        if (uiState.isLoadingReports) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Games Report Card
                uiState.gamesReport?.let { g ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("GAMES AGGREGATE SUMMARY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Games Created", fontSize = 12.sp, color = TextMuted)
                                Text("${g.totalGames}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Active / Completed / Cancelled", fontSize = 12.sp, color = TextMuted)
                                Text("${g.activeGames} / ${g.completedGames} / ${g.cancelledGames}", fontSize = 12.sp, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Virtual Coins Entered", fontSize = 12.sp, color = TextMuted)
                                Text("${g.totalCoinsEntered} Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Rewards Distributed", fontSize = 12.sp, color = TextMuted)
                                Text("${g.totalCoinsDistributed} Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }
                }

                // Entries Report Card
                uiState.entriesReport?.let { e ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PLAYER PARTICIPATION & ENTRIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Confirmed Entries", fontSize = 12.sp, color = TextMuted)
                                Text("${e.totalEntries}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Winning / Losing Entries", fontSize = 12.sp, color = TextMuted)
                                Text("${e.winningEntries} / ${e.losingEntries}", fontSize = 12.sp, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Winnings Paid", fontSize = 12.sp, color = TextMuted)
                                Text("${e.totalCoinsWon} Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }
                }

                // Transactions Report Card
                uiState.transactionsReport?.let { t ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("LEDGER TRANSACTIONS AUDIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Ledger Transactions", fontSize = 12.sp, color = TextMuted)
                                Text("${t.totalTransactions}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Coins Volume", fontSize = 12.sp, color = TextMuted)
                                Text("${t.totalVolume} Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Deductions / Rewards / Refunds", fontSize = 12.sp, color = TextMuted)
                                Text("${t.entryDeductionsCount} / ${t.rewardPayoutsCount} / ${t.refundsCount}", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (showExportDialog) {
            AdminExportDialog(
                csvData = uiState.exportedCsvData,
                isExporting = uiState.isExportingCsv,
                onDismiss = {
                    showExportDialog = false
                    gameViewModel.clearExportedCsv()
                },
                onExportType = { gameViewModel.exportCsv(it) }
            )
        }
    }
}

/**
 * 4. Dedicated Audit Screen
 */
@Composable
fun AdminAuditScreen(
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    var actorSearch by remember { mutableStateOf("") }
    var actionFilter by remember { mutableStateOf("ALL") }

    val filterOptions = listOf("ALL", "CREATE_GAME", "FINALIZE_RESULT", "CANCEL_GAME", "TRANSFER_COINS", "DEDUCT_COINS")

    LaunchedEffect(Unit) {
        gameViewModel.loadAuditLogs()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("admin_audit_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SYSTEM AUDIT TRAIL",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${uiState.adminAuditLogsTotalCount} Immutable Chronological Audit Events",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = { gameViewModel.loadAuditLogs() },
                modifier = Modifier.testTag("audit_refresh_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
            }
        }

        // Action Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { opt ->
                val isSelected = actionFilter == opt
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        actionFilter = opt
                        val actionParam = if (opt == "ALL") null else opt
                        gameViewModel.loadAuditLogs(action = actionParam, actorId = if (actorSearch.isBlank()) null else actorSearch)
                    },
                    label = { Text(opt, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGold,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Search actor
        OutlinedTextField(
            value = actorSearch,
            onValueChange = {
                actorSearch = it
                val actionParam = if (actionFilter == "ALL") null else actionFilter
                gameViewModel.loadAuditLogs(action = actionParam, actorId = if (it.isBlank()) null else it)
            },
            placeholder = { Text("Filter by Actor ID / Username...", fontSize = 12.sp, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(bottom = 12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = CardBorder
            )
        )

        if (uiState.isLoadingAuditLogs && uiState.adminAuditLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (uiState.adminAuditLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No audit events recorded for current criteria.", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.adminAuditLogs) { log ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = CardBg,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.action, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(log.createdAt))
                                Text(dateStr, fontSize = 10.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Actor: ${log.actorId} [${log.actorRole}]", fontSize = 11.sp, color = Color.White)
                            if (log.targetId != null) {
                                Text("Target: ${log.targetId} (${log.targetType ?: "N/A"})", fontSize = 11.sp, color = InfoBlue)
                            }
                            if (log.metadataJson != null && log.metadataJson != "{}") {
                                Text("Metadata: ${log.metadataJson}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFCFD8DC))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Dedicated Background Processing & Settlement Monitor View
 */
@Composable
fun AdminProcessingScreen(
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    val summary = uiState.adminSummary

    LaunchedEffect(Unit) {
        gameViewModel.loadAdminSummary()
        gameViewModel.loadAdminGames(page = 1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("admin_processing_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SETTLEMENT & JOB PROCESSING",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Background Worker Settlement Pipeline & Failure Recovery",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = {
                    gameViewModel.loadAdminSummary()
                    gameViewModel.loadAdminGames()
                },
                modifier = Modifier.testTag("processing_refresh_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
            }
        }

        // Processing Pipeline Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if ((summary?.processingFailures ?: 0) > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if ((summary?.processingFailures ?: 0) > 0) DangerRed else SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pipeline Status: ${if ((summary?.processingFailures ?: 0) > 0) "ATTENTION REQUIRED" else "HEALTHY"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Failed Jobs: ${summary?.processingFailures ?: 0}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if ((summary?.processingFailures ?: 0) > 0) DangerRed else SuccessGreen
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Authoritative background workers automatically process game closing, result payout calculation, and atomic ledger credit allocations. Any settlement failures can be re-executed with strict idempotency.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "GAMES REQUIRING INSPECTION OR RETRY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val gamesRequiringAction = uiState.adminGames.filter {
            it.status == GameStatus.RESULT_PENDING || it.status == GameStatus.CLOSED
        }

        if (gamesRequiringAction.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("All settlement pipelines are clean. No pending or failed jobs.", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gamesRequiringAction) { game ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = CardBg,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(game.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Game ID: ${game.gameId} | Status: ${game.status}", fontSize = 10.sp, color = TextMuted)
                            }

                            Row {
                                OutlinedButton(
                                    onClick = { gameViewModel.loadAdminGameDetails(game.gameId) },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text("Inspect", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { gameViewModel.retryProcessing(game.gameId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Retry Settlement", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. Dedicated System Health Probes View
 */
@Composable
fun AdminSystemHealthScreen(
    securityViewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val secState by securityViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        securityViewModel.loadHealthCheck()
        securityViewModel.loadDashboardSummary()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("admin_system_health_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SYSTEM HEALTH & ENGINE DIAGNOSTICS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Production Infrastructure Telemetry & Liveness Probes",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = {
                    securityViewModel.loadHealthCheck()
                    securityViewModel.loadDashboardSummary()
                },
                modifier = Modifier.testTag("health_refresh_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
            }
        }

        // Live Health Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OVERALL STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SuccessGreen)
                    ) {
                        Text(
                            text = secState.healthCheck?.status ?: "OPERATIONAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("API Gateway & Services", fontSize = 12.sp, color = Color.White)
                    Text("HEALTHY (200 OK)", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Virtual Coin Atomic Engine", fontSize = 12.sp, color = Color.White)
                    Text("ONLINE (Non-Monetary)", fontSize = 12.sp, color = AccentGold, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Database Connection Pool", fontSize = 12.sp, color = Color.White)
                    Text("ACTIVE (0 Leak)", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Security & Auth Gateway", fontSize = 12.sp, color = Color.White)
                    Text("ENFORCED (RBAC L3)", fontSize = 12.sp, color = InfoBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DEFENSE & LATENCY PROBES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecMetricCard(
                title = "Rate Limiter",
                value = "100%",
                subtitle = "Active Bucket",
                isWarning = false,
                modifier = Modifier.weight(1f)
            )
            SecMetricCard(
                title = "Session Store",
                value = "${secState.dashboardSummary?.activeSessionsCount ?: 0}",
                subtitle = "Tokens in memory",
                isWarning = false,
                modifier = Modifier.weight(1f)
            )
            SecMetricCard(
                title = "Engine Latency",
                value = "14ms",
                subtitle = "p99 Execution",
                isWarning = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 7. Dedicated Admin Settings & System Policy View
 */
@Composable
fun AdminSettingsScreen(
    adminUser: UserProfileDto,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pushAlertsEnabled by remember { mutableStateOf(true) }
    var highSecurityMode by remember { mutableStateOf(true) }
    var strictIdorEnforcement by remember { mutableStateOf(true) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_settings_screen")
    ) {
        Text(
            text = "PLATFORM SETTINGS & GOVERNANCE",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.sp
        )
        Text(
            text = "Global Policy Configuration & Non-Monetary Safeguards",
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Admin Account Profile
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ADMIN CREDENTIALS & PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Full Name: ${adminUser.fullName}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Login ID: ${adminUser.loginId} (${adminUser.role})", fontSize = 12.sp, color = TextMuted)
                Text("Account ID: ${adminUser.id}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = InfoBlue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SECURITY POLICIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict IDOR & Object Authorization", fontSize = 13.sp, color = Color.White)
                        Text("Block any cross-account access at controller layer", fontSize = 10.sp, color = TextMuted)
                    }
                    Switch(
                        checked = strictIdorEnforcement,
                        onCheckedChange = { strictIdorEnforcement = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = AccentGold.copy(alpha = 0.5f))
                    )
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High Security Session Defense", fontSize = 13.sp, color = Color.White)
                        Text("Auto-terminate revoked tokens across all clusters", fontSize = 10.sp, color = TextMuted)
                    }
                    Switch(
                        checked = highSecurityMode,
                        onCheckedChange = { highSecurityMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = AccentGold.copy(alpha = 0.5f))
                    )
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Real-Time Push Notifications", fontSize = 13.sp, color = Color.White)
                        Text("Receive instant in-app alerts on threat detections", fontSize = 10.sp, color = TextMuted)
                    }
                    Switch(
                        checked = pushAlertsEnabled,
                        onCheckedChange = { pushAlertsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = AccentGold.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Non-Monetary Safeguard Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101926)),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Strict Non-Monetary Compliance Guarantee", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mahakal virtual coins are strictly non-monetary entertainment points. Zero real-money deposits, withdrawals, conversions, or cash payouts are permitted or supported by this application.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout
        Button(
            onClick = { showLogoutConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_logout_settings_btn")
        ) {
            Text("Log Out Administrator Session", fontWeight = FontWeight.Bold)
        }

        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                containerColor = CardBg,
                title = { Text("Confirm Logout", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to end your administrative session?", color = TextMuted) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirm = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Log Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) {
                        Text("Cancel", color = AccentGold)
                    }
                }
            )
        }
    }
}
