package com.example.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.backend.model.AdminGameDetailsDto
import com.example.backend.model.AdminOperationsSummaryDto
import com.example.backend.model.AdminReconciliationReportDto
import com.example.backend.model.AuditLogDto
import com.example.backend.model.EntriesReportDto
import com.example.backend.model.GameEntryDto
import com.example.backend.model.GameProcessingDto
import com.example.backend.model.GamesReportDto
import com.example.backend.model.TransactionDetailDto
import com.example.backend.model.TransactionsReportDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkCardBg = Color(0xFF141C28)
private val DarkCardBorder = Color(0xFF1E2B3E)
private val GoldAccent = Color(0xFFE5A93C)
private val SuccessGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFEF5350)
private val InfoBlue = Color(0xFF42A5F5)
private val MutedText = Color(0xFF8A99AD)

/**
 * KPI Summary Banner for Admin Game Operations
 */
@Composable
fun AdminOperationsSummaryHeader(
    summary: AdminOperationsSummaryDto?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAuditLogs: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_operations_summary_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPERATIONS CONTROL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenReports,
                        modifier = Modifier.size(32.dp).testTag("admin_open_reports_btn")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = "Reports", tint = InfoBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onOpenAuditLogs,
                        modifier = Modifier.size(32.dp).testTag("admin_open_audit_btn")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Audit Logs", tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onOpenExport,
                        modifier = Modifier.size(32.dp).testTag("admin_open_export_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp).testTag("admin_refresh_summary_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GoldAccent, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MutedText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStatBadge(
                        label = "Total Games",
                        value = "${summary.totalGames}",
                        subtext = "${summary.activeGames} Active",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatBadge(
                        label = "Coins Volume",
                        value = "${summary.totalCoinsEntered}",
                        subtext = "${summary.totalEntries} Entries",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatBadge(
                        label = "Distributed",
                        value = "${summary.totalCoinsDistributed}",
                        subtext = "${summary.completedGames} Finalized",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatBadge(
                        label = "Health",
                        value = if (summary.discrepanciesCount == 0 && summary.pendingProcessingCount == 0) "SYNCED" else "${summary.pendingProcessingCount} Pend",
                        subtext = if (summary.discrepanciesCount > 0) "${summary.discrepanciesCount} Discrep" else "No Issues",
                        valueColor = if (summary.discrepanciesCount > 0) DangerRed else SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBadge(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0C121B),
        border = BorderStroke(0.8.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, fontSize = 9.sp, color = MutedText, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, fontSize = 9.sp, color = MutedText.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

/**
 * Full Administration Game Inspector Dialog
 */
@Composable
fun AdminGameInspectorDialog(
    details: AdminGameDetailsDto?,
    reconciliationReport: AdminReconciliationReportDto?,
    isReconciling: Boolean,
    isCorrecting: Boolean,
    isRetrying: Boolean,
    onDismiss: () -> Unit,
    onRetryProcessing: (gameId: String) -> Unit,
    onReconcile: (gameId: String) -> Unit,
    onCorrectDiscrepancies: (gameId: String) -> Unit,
    onInspectTransaction: (reference: String) -> Unit
) {
    if (details == null) return
    val game = details.game
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview & Pool, 1: Reconciliation, 2: Entries, 3: Audit Trail
    var showConfirmCorrectionDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E1520),
            border = BorderStroke(1.dp, Color(0xFF263345))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141C28))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = game.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Game ID: ${game.gameId} | Status: ${game.status}",
                            fontSize = 11.sp,
                            color = GoldAccent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                    }
                }

                // Sub-tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF141C28),
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pool & Rules", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            if (reconciliationReport == null) {
                                onReconcile(game.gameId)
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Reconciliation", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                if (reconciliationReport?.hasDiscrepancies == true) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(shape = CircleShape, color = DangerRed, modifier = Modifier.size(8.dp)) {}
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Entries (${details.statistics.totalEntries})", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Audit Trail (${details.auditEvents.size})", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                // Body content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> InspectorOverviewTab(
                            details = details,
                            isRetrying = isRetrying,
                            onRetryProcessing = { onRetryProcessing(game.gameId) }
                        )
                        1 -> InspectorReconciliationTab(
                            gameId = game.gameId,
                            report = reconciliationReport,
                            isReconciling = isReconciling,
                            isCorrecting = isCorrecting,
                            onReconcile = { onReconcile(game.gameId) },
                            onRequestCorrection = { showConfirmCorrectionDialog = true }
                        )
                        2 -> InspectorEntriesTab(
                            details = details,
                            onInspectTransaction = onInspectTransaction
                        )
                        3 -> InspectorAuditTab(auditEvents = details.auditEvents)
                    }
                }
            }
        }
    }

    // Confirmation dialog for authoritative ledger correction
    if (showConfirmCorrectionDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCorrectionDialog = false },
            containerColor = Color(0xFF18202C),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Ledger Correction", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "This will execute an authoritative correction on player wallets for game '${game.gameId}'. All missing reward disbursements will be deposited with idempotency protections. Do you want to proceed?",
                    color = Color(0xFFCFD8DC),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCorrectionDialog = false
                        onCorrectDiscrepancies(game.gameId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Execute Correction", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCorrectionDialog = false }) {
                    Text("Cancel", color = MutedText)
                }
            }
        )
    }
}

@Composable
private fun InspectorOverviewTab(
    details: AdminGameDetailsDto,
    isRetrying: Boolean,
    onRetryProcessing: () -> Unit
) {
    val game = details.game
    val stats = details.statistics
    val processing = details.processing
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ENTRY & REWARD POOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Coins Entered:", fontSize = 12.sp, color = MutedText)
                    Text("${stats.totalCoinsEntered} Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Rewards Distributed:", fontSize = 12.sp, color = MutedText)
                    Text("${stats.totalRewardsDistributed} Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Confirmed / Winning / Losing:", fontSize = 12.sp, color = MutedText)
                    Text("${stats.confirmedEntries} / ${stats.winningEntries} / ${stats.losingEntries}", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Processing State Card
        if (processing != null) {
            val statusColor = when (processing.status) {
                "COMPLETED" -> SuccessGreen
                "FAILED" -> DangerRed
                "PROCESSING" -> InfoBlue
                else -> GoldAccent
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SETTLEMENT PROCESSING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, statusColor)
                        ) {
                            Text(
                                text = processing.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Attempts: ${processing.attemptCount} | Processed: ${processing.processedEntries}/${processing.totalEntries} | Failed: ${processing.failedEntries}", fontSize = 11.sp, color = MutedText)
                    if (processing.lastErrorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Error [${processing.lastErrorCode}]: ${processing.lastErrorMessage}", fontSize = 11.sp, color = DangerRed)
                    }

                    if (processing.status == "FAILED" || processing.status == "PROCESSING") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRetryProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (isRetrying) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black)
                            } else {
                                Text("Retry Settlement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Options Breakdown
        Text("OPTIONS DISTRIBUTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        stats.optionsBreakdown.forEach { opt ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkCardBg,
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(opt.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${opt.entriesCount} Entries | ${opt.totalCoins} Coins", fontSize = 10.sp, color = MutedText)
                    }
                    Text(
                        String.format(Locale.US, "%.1f%%", opt.percentageOfTotalCoins),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectorReconciliationTab(
    gameId: String,
    report: AdminReconciliationReportDto?,
    isReconciling: Boolean,
    isCorrecting: Boolean,
    onReconcile: () -> Unit,
    onRequestCorrection: () -> Unit
) {
    if (isReconciling) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = GoldAccent)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Running Authoritative Ledger Reconciliation...", color = MutedText, fontSize = 12.sp)
            }
        }
        return
    }

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = onReconcile,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("Run Reconciliation")
            }
        }
        return
    }

    val isMatch = report.reconciliationStatus == "MATCH"
    val statusColor = if (isMatch) SuccessGreen else DangerRed
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = statusColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, statusColor)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isMatch) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMatch) "LEDGER FULLY MATCHED" else "DISCREPANCIES DETECTED",
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = onReconcile, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Re-check", tint = statusColor)
                }
            }
        }

        // Ledger Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AUTHORITATIVE RECONCILIATION SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Winners Claimed:", fontSize = 12.sp, color = MutedText)
                    Text("${report.totalWinnersClaimed}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Expected Rewards:", fontSize = 12.sp, color = MutedText)
                    Text("${report.totalExpectedRewards} Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Authoritative Paid in Ledger:", fontSize = 12.sp, color = MutedText)
                    Text("${report.totalPaidInLedger} Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Reconciled At:", fontSize = 11.sp, color = MutedText)
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(report.reconciledAt))
                    Text(dateStr, fontSize = 11.sp, color = MutedText)
                }
            }
        }

        // Discrepancy details
        if (report.hasDiscrepancies) {
            Text("DISCREPANCY ENTRIES (${report.discrepancies.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
            for (disc in report.discrepancies) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF261618),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Entry ID: ${disc.entryId}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Player: ${disc.userId}", fontSize = 10.sp, color = MutedText)
                        Text("Expected: ${disc.expectedAmount} | Ledger: ${disc.ledgerAmount}", fontSize = 10.sp, color = DangerRed)
                        Text("Reason: ${disc.reason}", fontSize = 10.sp, color = Color(0xFFFF8A80))
                    }
                }
            }

            // Correction CTA
            Button(
                onClick = onRequestCorrection,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCorrecting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("Correct All Discrepancies Authoritatively", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InspectorEntriesTab(
    details: AdminGameDetailsDto,
    onInspectTransaction: (reference: String) -> Unit
) {
    val gameId = details.game.gameId
    val entries = details.statistics.optionsBreakdown
    // Note: details includes aggregate breakdown, but entries list can be loaded from ViewModel
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "Game Entries Overview: ${details.statistics.totalEntries} Total",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
        }
        items(details.statistics.optionsBreakdown) { opt ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkCardBg,
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(opt.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Option ID: ${opt.optionId}", fontSize = 10.sp, color = MutedText)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${opt.entriesCount} Entries", fontSize = 11.sp, color = Color.White)
                        Text("${opt.totalCoins} Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorAuditTab(
    auditEvents: List<com.example.backend.model.GameEventDto>
) {
    if (auditEvents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audit events recorded for this game.", color = MutedText, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(auditEvents) { event ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkCardBg,
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(event.eventType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date(event.timestamp))
                        Text(dateStr, fontSize = 10.sp, color = MutedText)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Actor: ${event.actorId} (${event.actorRole})", fontSize = 10.sp, color = Color.White)
                    if (event.metadataJson != null && event.metadataJson != "{}") {
                        Text("Metadata: ${event.metadataJson}", fontSize = 9.sp, color = MutedText)
                    }
                }
            }
        }
    }
}

/**
 * Authoritative Transaction Detail Inspector Dialog
 */
@Composable
fun AdminTransactionDetailDialog(
    tx: TransactionDetailDto?,
    onDismiss: () -> Unit
) {
    if (tx == null) return
    val t = tx.transaction

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141C28),
        title = {
            Text("Ledger Transaction Details", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Transaction ID: ${t.transactionId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = GoldAccent)
                Text("Type: ${t.transactionType} | Status: ${t.status}", fontSize = 11.sp, color = Color.White)
                Text("Amount: ${t.amount} Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)
                Text("Source: ${tx.sourceAccountName ?: t.sourceWalletId ?: "SYSTEM"}", fontSize = 11.sp, color = MutedText)
                Text("Destination: ${tx.destinationAccountName ?: t.destinationWalletId ?: "SYSTEM"}", fontSize = 11.sp, color = MutedText)
                if (t.referenceId != null) {
                    Text("Reference ID: ${t.referenceId}", fontSize = 10.sp, color = InfoBlue)
                }
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(t.createdAt))
                Text("Timestamp: $dateStr", fontSize = 10.sp, color = MutedText)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GoldAccent)
            }
        }
    )
}

/**
 * Operational Reports & Analytics Dialog
 */
@Composable
fun AdminReportsDialog(
    gamesReport: GamesReportDto?,
    entriesReport: EntriesReportDto?,
    transactionsReport: TransactionsReportDto?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E1520),
            border = BorderStroke(1.dp, Color(0xFF263345))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OPERATIONAL REPORTS & METRICS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldAccent)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldAccent)
                    }
                } else {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Games Section
                        if (gamesReport != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("GAMES AGGREGATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Total Games Created: ${gamesReport.totalGames}", fontSize = 12.sp, color = Color.White)
                                    Text("Active Games: ${gamesReport.activeGames} | Completed: ${gamesReport.completedGames} | Cancelled: ${gamesReport.cancelledGames}", fontSize = 11.sp, color = MutedText)
                                    Text("Total Virtual Coins Entered: ${gamesReport.totalCoinsEntered}", fontSize = 11.sp, color = Color.White)
                                    Text("Total Virtual Coins Distributed: ${gamesReport.totalCoinsDistributed}", fontSize = 11.sp, color = SuccessGreen)
                                }
                            }
                        }

                        // Entries Section
                        if (entriesReport != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("ENTRIES PERFORMANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Total Player Entries: ${entriesReport.totalEntries}", fontSize = 12.sp, color = Color.White)
                                    Text("Confirmed: ${entriesReport.confirmedEntries} | Won: ${entriesReport.winningEntries} | Lost: ${entriesReport.losingEntries}", fontSize = 11.sp, color = MutedText)
                                    Text("Total Winnings Paid: ${entriesReport.totalCoinsWon} Coins", fontSize = 11.sp, color = SuccessGreen)
                                }
                            }
                        }

                        // Transactions Section
                        if (transactionsReport != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("LEDGER TRANSACTIONS METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Total Ledger Transactions: ${transactionsReport.totalTransactions}", fontSize = 12.sp, color = Color.White)
                                    Text("Total Volume: ${transactionsReport.totalVolume} Coins", fontSize = 11.sp, color = GoldAccent)
                                    Text("Entry Deductions: ${transactionsReport.entryDeductionsCount} | Reward Payouts: ${transactionsReport.rewardPayoutsCount} | Refunds: ${transactionsReport.refundsCount}", fontSize = 10.sp, color = MutedText)
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
 * Data Export Dialog with Clipboard Copy & Live View
 */
@Composable
fun AdminExportDialog(
    csvData: String?,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExportType: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141C28),
        title = {
            Text("Export Operations Data (CSV)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select export dataset:", fontSize = 12.sp, color = MutedText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onExportType("games") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3E))
                    ) {
                        Text("Games", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onExportType("entries") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3E))
                    ) {
                        Text("Entries", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onExportType("audit") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3E))
                    ) {
                        Text("Audit", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onExportType("transactions") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3E))
                    ) {
                        Text("Ledger", fontSize = 10.sp)
                    }
                }

                if (isExporting) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldAccent)
                    }
                } else if (csvData != null) {
                    val lineCount = csvData.lines().size
                    Text("Generated CSV ($lineCount lines):", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0A0F16),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = csvData.take(500) + if (csvData.length > 500) "\n... [truncated for display]" else "",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFCFD8DC),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(csvData))
                            copied = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copied) "Copied to Clipboard!" else "Copy Entire CSV to Clipboard", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MutedText)
            }
        }
    )
}

/**
 * System Audit Trail Dialog
 */
@Composable
fun AdminAuditLogsDialog(
    auditLogs: List<AuditLogDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E1520),
            border = BorderStroke(1.dp, Color(0xFF263345))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SYSTEM AUDIT EVENT LOGS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldAccent)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldAccent)
                    }
                } else if (auditLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No audit logs found.", color = MutedText)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(auditLogs) { log ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = DarkCardBg,
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                        val dateStr = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date(log.createdAt))
                                        Text(dateStr, fontSize = 10.sp, color = MutedText)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Actor: ${log.actorId} [${log.actorRole}]", fontSize = 10.sp, color = Color.White)
                                    if (log.targetId != null) {
                                        Text("Target: ${log.targetId} (${log.targetType ?: ""})", fontSize = 10.sp, color = InfoBlue)
                                    }
                                    if (log.metadataJson != null && log.metadataJson != "{}") {
                                        Text("Metadata: ${log.metadataJson}", fontSize = 9.sp, color = MutedText)
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
