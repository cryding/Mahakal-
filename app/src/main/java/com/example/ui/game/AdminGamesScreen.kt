package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.GameDto
import com.example.backend.model.GameEntryDto
import com.example.backend.model.GameOptionCreateRequest
import com.example.backend.model.GameStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminGamesScreen(
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var finalizingGame by remember { mutableStateOf<GameDto?>(null) }
    var cancellingGame by remember { mutableStateOf<GameDto?>(null) }
    var viewingEntriesForGame by remember { mutableStateOf<GameDto?>(null) }

    // Phase 8 Admin Dialog States
    var showReportsDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf(uiState.adminGamesSearchQuery) }

    LaunchedEffect(Unit) {
        gameViewModel.loadAdminSummary()
        gameViewModel.loadAdminGames()
    }

    val bgDark = Color(0xFF0F141C)
    val accentGold = Color(0xFFE5A93C)
    val filterOptions = listOf(
        "ALL" to null,
        "OPEN" to GameStatus.OPEN,
        "CLOSED" to GameStatus.CLOSED,
        "PENDING" to GameStatus.RESULT_PENDING,
        "COMPLETED" to GameStatus.RESULT_FINALIZED,
        "DRAFT" to GameStatus.DRAFT,
        "CANCELLED" to GameStatus.CANCELLED
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(16.dp)
    ) {
        // Operations Summary KPI Banner
        AdminOperationsSummaryHeader(
            summary = uiState.adminSummary,
            isLoading = uiState.isLoadingSummary,
            onRefresh = {
                gameViewModel.loadAdminSummary()
                gameViewModel.loadAdminGames()
            },
            onOpenReports = {
                showReportsDialog = true
                gameViewModel.loadReports()
            },
            onOpenAuditLogs = {
                showAuditLogsDialog = true
                gameViewModel.loadAuditLogs()
            },
            onOpenExport = {
                showExportDialog = true
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Header & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GAMES REPOSITORY",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${uiState.adminGamesTotalCount} Games Registered (Page ${uiState.adminGamesPage}/${uiState.adminGamesTotalPages})",
                    fontSize = 11.sp,
                    color = Color(0xFF7A8B9E)
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentGold, contentColor = Color.Black),
                modifier = Modifier.testTag("admin_create_game_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Game", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search and Filters Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    gameViewModel.loadAdminGames(search = it, page = 1)
                },
                placeholder = { Text("Search games by title or ID...", fontSize = 12.sp, color = Color(0xFF7A8B9E)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF7A8B9E), modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            gameViewModel.loadAdminGames(search = "", page = 1)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF7A8B9E), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                singleLine = true
            )
        }

        // Filter chips row
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { (label, statusVal) ->
                val isSelected = uiState.adminGamesFilterStatus == statusVal
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        gameViewModel.loadAdminGames(status = statusVal, page = 1)
                    },
                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentGold,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF141C28),
                        labelColor = Color(0xFF90A4AE)
                    )
                )
            }
        }

        // Messages
        uiState.actionSuccessMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = Color(0xFF1B3828),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF66BB6A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = msg, color = Color(0xFF81C784), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { gameViewModel.clearMessages() }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        uiState.actionErrorMessage?.let { err ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = Color(0xFF3E1C1C),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = err, color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { gameViewModel.clearMessages() }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFFF8A80), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Games List or Empty/Loading State
        val gamesList = if (uiState.adminGames.isNotEmpty()) uiState.adminGames else uiState.games
        val isLoading = uiState.isLoadingAdminGames || uiState.isLoadingGames

        if (isLoading && gamesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentGold)
            }
        } else if (gamesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No games match the current filter. Click 'New Game' or clear search.", color = Color(0xFF7A8B9E), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(gamesList, key = { it.gameId }) { game ->
                    AdminGameCard(
                        game = game,
                        onOpen = {
                            gameViewModel.openGame(game.gameId)
                            gameViewModel.loadAdminGames()
                            gameViewModel.loadAdminSummary()
                        },
                        onClose = {
                            gameViewModel.closeGame(game.gameId)
                            gameViewModel.loadAdminGames()
                            gameViewModel.loadAdminSummary()
                        },
                        onFinalize = { finalizingGame = game },
                        onCancel = { cancellingGame = game },
                        onViewEntries = {
                            viewingEntriesForGame = game
                            gameViewModel.loadGameEntriesAdmin(game.gameId)
                        },
                        onInspect = {
                            gameViewModel.loadAdminGameDetails(game.gameId)
                        }
                    )
                }
            }

            // Pagination Row
            if (uiState.adminGamesTotalPages > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (uiState.adminGamesPage > 1) {
                                gameViewModel.loadAdminGames(page = uiState.adminGamesPage - 1)
                            }
                        },
                        enabled = uiState.adminGamesPage > 1,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev", fontSize = 11.sp)
                    }

                    Text(
                        text = "Page ${uiState.adminGamesPage} of ${uiState.adminGamesTotalPages}",
                        fontSize = 11.sp,
                        color = Color(0xFF7A8B9E)
                    )

                    OutlinedButton(
                        onClick = {
                            if (uiState.adminGamesPage < uiState.adminGamesTotalPages) {
                                gameViewModel.loadAdminGames(page = uiState.adminGamesPage + 1)
                            }
                        },
                        enabled = uiState.adminGamesPage < uiState.adminGamesTotalPages,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Next", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // Phase 8 Admin Dialogs
    // ==========================================

    // Game Inspector Dialog
    if (uiState.selectedGameDetails != null) {
        AdminGameInspectorDialog(
            details = uiState.selectedGameDetails,
            reconciliationReport = uiState.reconciliationReport,
            isReconciling = uiState.isReconciling,
            isCorrecting = uiState.isCorrectingReconciliation,
            isRetrying = uiState.isRetryingProcessing,
            onDismiss = { gameViewModel.clearSelectedGameDetails() },
            onRetryProcessing = { gId -> gameViewModel.retryProcessing(gId) },
            onReconcile = { gId -> gameViewModel.reconcileGame(gId) },
            onCorrectDiscrepancies = { gId ->
                gameViewModel.correctDiscrepancies(gId) {
                    gameViewModel.loadAdminGames()
                }
            },
            onInspectTransaction = { ref -> gameViewModel.loadTransactionDetail(ref) }
        )
    }

    // Authoritative Ledger Transaction Detail Dialog
    if (uiState.selectedTransactionDetail != null) {
        AdminTransactionDetailDialog(
            tx = uiState.selectedTransactionDetail,
            onDismiss = { gameViewModel.clearTransactionDetail() }
        )
    }

    // Reports Dialog
    if (showReportsDialog) {
        AdminReportsDialog(
            gamesReport = uiState.gamesReport,
            entriesReport = uiState.entriesReport,
            transactionsReport = uiState.transactionsReport,
            isLoading = uiState.isLoadingReports,
            onDismiss = { showReportsDialog = false },
            onRefresh = { gameViewModel.loadReports() }
        )
    }

    // System Audit Logs Dialog
    if (showAuditLogsDialog) {
        AdminAuditLogsDialog(
            auditLogs = uiState.adminAuditLogs,
            isLoading = uiState.isLoadingAuditLogs,
            onDismiss = { showAuditLogsDialog = false },
            onRefresh = { gameViewModel.loadAuditLogs() }
        )
    }

    // Export CSV Dialog
    if (showExportDialog) {
        AdminExportDialog(
            csvData = uiState.exportedCsvData,
            isExporting = uiState.isExportingCsv,
            onDismiss = {
                showExportDialog = false
                gameViewModel.clearExportedCsv()
            },
            onExportType = { type -> gameViewModel.exportCsv(type) }
        )
    }

    // Create Game Dialog
    if (showCreateDialog) {
        AdminCreateGameDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { showCreateDialog = false },
            onSubmit = { request ->
                gameViewModel.createGame(request) {
                    showCreateDialog = false
                }
            }
        )
    }

    // Finalize Result Dialog
    finalizingGame?.let { game ->
        AdminFinalizeResultDialog(
            game = game,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { finalizingGame = null },
            onSubmit = { winningOptionId, reason ->
                gameViewModel.finalizeResult(game.gameId, winningOptionId, reason) {
                    finalizingGame = null
                }
            }
        )
    }

    // Cancel Game Dialog
    cancellingGame?.let { game ->
        AdminCancelGameDialog(
            game = game,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { cancellingGame = null },
            onSubmit = { reason ->
                gameViewModel.cancelGame(game.gameId, reason)
                cancellingGame = null
            }
        )
    }

    // View Entries Dialog
    viewingEntriesForGame?.let { game ->
        AdminGameEntriesDialog(
            game = game,
            entries = uiState.selectedGameEntries,
            isLoading = uiState.isLoadingGameEntries,
            onDismiss = { viewingEntriesForGame = null }
        )
    }
}

@Composable
fun AdminGameCard(
    game: GameDto,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onFinalize: () -> Unit,
    onCancel: () -> Unit,
    onViewEntries: () -> Unit,
    onInspect: () -> Unit
) {
    val cardBg = Color(0xFF18202C)
    val accentGold = Color(0xFFE5A93C)
    val statusColor = when (game.status) {
        GameStatus.OPEN -> Color(0xFF66BB6A)
        GameStatus.SCHEDULED -> Color(0xFF42A5F5)
        GameStatus.CLOSED -> Color(0xFFFFA726)
        GameStatus.RESULT_FINALIZED -> Color(0xFFAB47BC)
        GameStatus.CANCELLED -> Color(0xFFEF5350)
        else -> Color(0xFF78909C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_game_card_${game.gameId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263345))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = game.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "${game.entryCount} Entries Placed",
                    fontSize = 12.sp,
                    color = Color(0xFF7A8B9E),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = game.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Type: ${game.gameType} | Multiplier: ${game.rewardMultiplier}x | Limits: ${game.minCoins} - ${game.maxCoins} coins",
                fontSize = 11.sp,
                color = Color(0xFF90A4AE)
            )

            val deadlineStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(game.entryDeadline))
            Text(
                text = "Entry Deadline: $deadlineStr",
                fontSize = 11.sp,
                color = Color(0xFFB0BEC5)
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF263345), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onInspect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3E), contentColor = accentGold),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("admin_inspect_game_${game.gameId}")
                ) {
                    Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (game.status == GameStatus.DRAFT || game.status == GameStatus.SCHEDULED) {
                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("admin_open_game_${game.gameId}")
                    ) {
                        Text("Open", fontSize = 11.sp)
                    }
                }

                if (game.status == GameStatus.OPEN) {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("admin_close_game_${game.gameId}")
                    ) {
                        Text("Close", fontSize = 11.sp)
                    }
                }

                if (game.status == GameStatus.OPEN || game.status == GameStatus.CLOSED || game.status == GameStatus.RESULT_PENDING) {
                    Button(
                        onClick = onFinalize,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("admin_finalize_game_${game.gameId}")
                    ) {
                        Text("Finalize", fontSize = 11.sp)
                    }
                }

                if (game.status != GameStatus.CANCELLED && game.status != GameStatus.RESULT_FINALIZED && game.status != GameStatus.ARCHIVED) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("admin_cancel_game_${game.gameId}")
                    ) {
                        Text("Cancel", fontSize = 11.sp, color = Color(0xFFEF5350))
                    }
                }

                OutlinedButton(
                    onClick = onViewEntries,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("admin_view_entries_${game.gameId}")
                ) {
                    Text("Entries", fontSize = 11.sp, color = Color(0xFFB0BEC5))
                }
            }
        }
    }
}

@Composable
fun AdminCreateGameDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateGameRequest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var gameType by remember { mutableStateOf("PREDICTION") }
    var minCoinsText by remember { mutableStateOf("10") }
    var maxCoinsText by remember { mutableStateOf("1000") }
    var multiplierText by remember { mutableStateOf("2.0") }
    var option1 by remember { mutableStateOf("Option A") }
    var option2 by remember { mutableStateOf("Option B") }
    var option3 by remember { mutableStateOf("") }

    val accentGold = Color(0xFFE5A93C)
    val now = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18202C),
        title = { Text("Create New Game", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_game_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().testTag("new_game_desc_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minCoinsText,
                        onValueChange = { minCoinsText = it },
                        label = { Text("Min Coins") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = maxCoinsText,
                        onValueChange = { maxCoinsText = it },
                        label = { Text("Max Coins") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = multiplierText,
                        onValueChange = { multiplierText = it },
                        label = { Text("Multiplier") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Text("Options (At least 2 required):", fontSize = 12.sp, color = Color(0xFFCFD8DC), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = option1,
                    onValueChange = { option1 = it },
                    label = { Text("Option 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option2,
                    onValueChange = { option2 = it },
                    label = { Text("Option 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option3,
                    onValueChange = { option3 = it },
                    label = { Text("Option 3 (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val options = mutableListOf(
                        GameOptionCreateRequest("OPT_1", option1.trim()),
                        GameOptionCreateRequest("OPT_2", option2.trim())
                    )
                    if (option3.trim().isNotEmpty()) {
                        options.add(GameOptionCreateRequest("OPT_3", option3.trim()))
                    }
                    val req = CreateGameRequest(
                        gameType = gameType,
                        title = title.trim(),
                        description = description.trim(),
                        options = options,
                        startTime = now,
                        entryDeadline = now + 3600_000L, // 1 hour
                        resultTime = now + 4200_000L,
                        minCoins = minCoinsText.toLongOrNull() ?: 10L,
                        maxCoins = maxCoinsText.toLongOrNull() ?: 1000L,
                        rewardMultiplier = multiplierText.toDoubleOrNull() ?: 2.0
                    )
                    onSubmit(req)
                },
                enabled = title.isNotBlank() && option1.isNotBlank() && option2.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = accentGold, contentColor = Color.Black),
                modifier = Modifier.testTag("admin_confirm_create_game_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Create Game", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = Color(0xFF7A8B9E))
            }
        }
    )
}

@Composable
fun AdminFinalizeResultDialog(
    game: GameDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (winningOptionId: String, reason: String) -> Unit
) {
    var selectedOptionId by remember { mutableStateOf(game.options.firstOrNull()?.optionId ?: "") }
    var reason by remember { mutableStateOf("Administrator confirmed result") }
    val accentGold = Color(0xFFE5A93C)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18202C),
        title = {
            Text(
                text = "Finalize Result & Distribute Rewards",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Select Winning Outcome for '${game.title}':",
                    fontSize = 12.sp,
                    color = Color(0xFFCFD8DC)
                )
                Spacer(modifier = Modifier.height(10.dp))

                game.options.forEach { opt ->
                    val isSelected = opt.optionId == selectedOptionId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOptionId = opt.optionId }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedOptionId = opt.optionId },
                            colors = RadioButtonDefaults.colors(selectedColor = accentGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = opt.displayName,
                            color = if (isSelected) Color.White else Color(0xFFB0BEC5),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Audit Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedOptionId, reason) },
                enabled = selectedOptionId.isNotEmpty() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                modifier = Modifier.testTag("admin_confirm_finalize_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("Finalize & Reward", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = Color(0xFF7A8B9E))
            }
        }
    )
}

@Composable
fun AdminCancelGameDialog(
    game: GameDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("Cancelled due to schedule change") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18202C),
        title = {
            Text("Cancel Game & Refund Players", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to cancel '${game.title}'? All ${game.entryCount} confirmed entries will be automatically refunded to player wallets.",
                    fontSize = 13.sp,
                    color = Color(0xFFCFD8DC)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Cancellation Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) {
                Text("Cancel Game", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Back", color = Color(0xFF7A8B9E))
            }
        }
    )
}

@Composable
fun AdminGameEntriesDialog(
    game: GameDto,
    entries: List<GameEntryDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18202C),
        title = {
            Text(
                text = "Entries for '${game.title}' (${entries.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE5A93C))
                }
            } else if (entries.isEmpty()) {
                Text("No entries placed for this game yet.", color = Color(0xFF7A8B9E))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF131A24),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Player: @${entry.userId.take(8)}", fontSize = 11.sp, color = Color.White)
                                    Text("Choice: ${entry.selectedOptionName ?: entry.selectedOptionId}", fontSize = 11.sp, color = Color(0xFFE5A93C))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${entry.virtualCoinAmount} Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(entry.status, fontSize = 10.sp, color = Color(0xFF90A4AE))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFE5A93C))
            }
        }
    )
}
