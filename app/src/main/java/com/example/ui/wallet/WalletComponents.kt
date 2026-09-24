package com.example.ui.wallet

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.model.TransactionDto
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Design System Colors
private val BgDark = Color(0xFF0A0E17)
private val CardBg = Color(0xFF141C28)
private val CardBorder = Color(0xFF1E2B3E)
private val AccentGold = Color(0xFFE5A93C)
private val AgentBlue = Color(0xFF42A5F5)
private val SuccessGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFEF5350)
private val TextMuted = Color(0xFF8A99AD)

fun formatCoins(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

fun formatLedgerDate(timestamp: Long): String {
    if (timestamp <= 0) return "—"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Clean, prominent Material 3 Wallet Balance Card.
 * Displays authoritative virtual-coin balance and role-appropriate actions.
 */
@Composable
fun WalletBalanceCard(
    balance: Long,
    currencyType: String = "VIRTUAL_COIN",
    isLoading: Boolean = false,
    role: String,
    onRefresh: () -> Unit,
    onTransferClick: (() -> Unit)? = null,
    onDeductClick: (() -> Unit)? = null,
    onViewLedgerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wallet_balance_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, AccentGold.copy(alpha = 0.4f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Label & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Virtual Coins",
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VIRTUAL COIN BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Non-monetary internal accounting units",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.testTag("refresh_wallet_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AccentGold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Balance",
                            tint = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Balance Number
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatCoins(balance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("wallet_balance_text")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COINS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onTransferClick != null) {
                    Button(
                        onClick = onTransferClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_coins_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (role.uppercase() == "ADMIN") "Allocate" else "Transfer",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (onDeductClick != null) {
                    OutlinedButton(
                        onClick = onDeductClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("deduct_coins_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deduct",
                            color = DangerRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (onViewLedgerClick != null) {
                    OutlinedButton(
                        onClick = onViewLedgerClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_ledger_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ledger",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Transfer Coins Dialog with Step 1 (Input) and Step 2 (Confirmation).
 */
@Composable
fun TransferCoinsDialog(
    role: String,
    availableBalance: Long,
    candidates: List<Pair<String, String>>, // List of (Account ID, Display Label)
    isLoadingCandidates: Boolean = false,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmTransfer: (destinationAccountId: String, amount: Long, reason: String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(candidates.firstOrNull()?.first ?: "") }
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1: Input, 2: Confirmation
    var validationError by remember { mutableStateOf<String?>(null) }

    val amountLong = amountText.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = AccentGold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (step == 1) {
                        if (role.uppercase() == "ADMIN") "Allocate Coins to Agent" else "Transfer Coins to User"
                    } else "Confirm Coin Transfer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            if (step == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Available Balance Banner
                    Surface(
                        color = CardBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Available Balance:", fontSize = 12.sp, color = TextMuted)
                            Text(
                                "${formatCoins(availableBalance)} Coins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                    }

                    // Recipient Selector
                    Text(
                        text = if (role.uppercase() == "ADMIN") "Select Recipient Agent" else "Select Recipient User",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )

                    if (candidates.isEmpty()) {
                        Text(
                            text = if (isLoadingCandidates) "Loading eligible recipients..." else "No active recipients found.",
                            fontSize = 13.sp,
                            color = DangerRed
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            candidates.take(5).forEach { (id, label) ->
                                val isSelected = selectedAccountId == id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AccentGold.copy(alpha = 0.2f) else CardBg,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(
                                            if (isSelected) listOf(AccentGold, AccentGold) else listOf(CardBorder, CardBorder)
                                        )
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAccountId = id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isSelected) AccentGold else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                amountText = input
                                validationError = null
                            }
                        },
                        label = { Text("Transfer Amount (Coins)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = CardBorder
                        )
                    )

                    // Quick percentage chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100L, 500L, 1000L, 5000L).forEach { quickAmount ->
                            FilterChip(
                                selected = amountText == quickAmount.toString(),
                                onClick = {
                                    amountText = quickAmount.toString()
                                    validationError = null
                                },
                                label = { Text("+$quickAmount", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentGold
                                )
                            )
                        }
                    }

                    // Reason Field
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = {
                            reasonText = it
                            validationError = null
                        },
                        label = { Text("Reason / Purpose") },
                        placeholder = { Text("e.g., Weekly agent coin allocation") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_reason_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = CardBorder
                        )
                    )

                    validationError?.let { err ->
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = DangerRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // Confirmation Step
                val recipientName = candidates.find { it.first == selectedAccountId }?.second ?: selectedAccountId
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = CardBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Recipient:", fontSize = 12.sp, color = TextMuted)
                                Text(recipientName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount to Transfer:", fontSize = 12.sp, color = TextMuted)
                                Text("${formatCoins(amountLong)} Coins", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            }
                            HorizontalDivider(color = CardBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Balance After Transfer:", fontSize = 12.sp, color = TextMuted)
                                Text("${formatCoins(availableBalance - amountLong)} Coins", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                            HorizontalDivider(color = CardBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Reason:", fontSize = 12.sp, color = TextMuted)
                                Text(reasonText.ifBlank { "Coin transfer" }, fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }

                    // Warning notice
                    Surface(
                        color = DangerRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Non-monetary virtual coin transfer. Operation is recorded permanently in the atomic ledger.",
                                fontSize = 11.sp,
                                color = DangerRed
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = {
                        if (selectedAccountId.isBlank()) {
                            validationError = "Please select a valid recipient."
                            return@Button
                        }
                        if (amountLong <= 0L) {
                            validationError = "Transfer amount must be greater than 0."
                            return@Button
                        }
                        if (amountLong > availableBalance) {
                            validationError = "Insufficient balance. Available: ${formatCoins(availableBalance)} Coins."
                            return@Button
                        }
                        step = 2
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    modifier = Modifier.testTag("transfer_next_button")
                ) {
                    Text("Review Transfer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onConfirmTransfer(
                            selectedAccountId,
                            amountLong,
                            reasonText.ifBlank { "Transfer from $role" }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    modifier = Modifier.testTag("transfer_confirm_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Confirm & Execute", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (step == 2) {
                OutlinedButton(
                    onClick = { step = 1 },
                    enabled = !isSubmitting
                ) {
                    Text("Back", color = TextMuted)
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        },
        containerColor = BgDark
    )
}

/**
 * Deduct Coins Dialog with strict Reason requirement.
 */
@Composable
fun DeductCoinsDialog(
    role: String,
    candidates: List<Pair<String, String>>, // List of (Account ID, Display Label)
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmDeduct: (targetAccountId: String, amount: Long, reason: String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(candidates.firstOrNull()?.first ?: "") }
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val amountLong = amountText.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (role.uppercase() == "ADMIN") "Deduct Coins from Agent" else "Deduct Coins from User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select Account for Deduction",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )

                if (candidates.isEmpty()) {
                    Text("No accounts available for deduction.", fontSize = 13.sp, color = DangerRed)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        candidates.take(5).forEach { (id, label) ->
                            val isSelected = selectedAccountId == id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DangerRed.copy(alpha = 0.15f) else CardBg,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(
                                        if (isSelected) listOf(DangerRed, DangerRed) else listOf(CardBorder, CardBorder)
                                    )
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAccountId = id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isSelected) DangerRed else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            amountText = input
                            validationError = null
                        }
                    },
                    label = { Text("Deduction Amount (Coins)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deduct_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DangerRed,
                        unfocusedBorderColor = CardBorder
                    )
                )

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = {
                        reasonText = it
                        validationError = null
                    },
                    label = { Text("Mandatory Reason") },
                    placeholder = { Text("e.g., Audit adjustment / Correction") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deduct_reason_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DangerRed,
                        unfocusedBorderColor = CardBorder
                    )
                )

                validationError?.let { err ->
                    Text(text = err, fontSize = 12.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAccountId.isBlank()) {
                        validationError = "Please select an account."
                        return@Button
                    }
                    if (amountLong <= 0L) {
                        validationError = "Deduction amount must be greater than 0."
                        return@Button
                    }
                    if (reasonText.trim().isEmpty()) {
                        validationError = "Reason is mandatory for deductions."
                        return@Button
                    }
                    onConfirmDeduct(selectedAccountId, amountLong, reasonText.trim())
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                modifier = Modifier.testTag("deduct_confirm_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Execute Deduction", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = BgDark
    )
}

/**
 * Transaction Ledger Screen / View with filters, search, and detailed item inspection.
 */
@Composable
fun TransactionLedgerView(
    transactions: List<TransactionDto>,
    isLoading: Boolean,
    totalCount: Int,
    currentPage: Int,
    totalPages: Int,
    filterType: String?,
    searchQuery: String,
    onFilterChange: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSelectTransaction: (TransactionDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transaction_ledger_view")
    ) {
        // Search & Refresh Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by ID or reason...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ledger_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = CardBorder
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier.testTag("ledger_refresh_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentGold, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Ledger", tint = TextMuted)
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                null to "All",
                "ADMIN_TO_AGENT" to "Admin → Agent",
                "AGENT_TO_USER" to "Agent → User",
                "ADMIN_DEDUCTION" to "Admin Deductions",
                "AGENT_DEDUCTION" to "Agent Deductions",
                "SYSTEM_ADJUSTMENT" to "Reversals"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = filterType == type,
                    onClick = { onFilterChange(type) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGold.copy(alpha = 0.2f),
                        selectedLabelColor = AccentGold
                    )
                )
            }
        }

        // Total count header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LEDGER ENTRIES ($totalCount)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = "Page $currentPage of $totalPages",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        // Transactions List
        if (transactions.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transactions found in ledger.", color = TextMuted, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.transactionId }) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }
        }

        // Pagination Controls
        if (totalPages > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onPageChange(currentPage - 1) },
                    enabled = currentPage > 1 && !isLoading
                ) {
                    Text("Previous", fontSize = 12.sp)
                }

                Text("Page $currentPage / $totalPages", fontSize = 12.sp, color = TextMuted)

                OutlinedButton(
                    onClick = { onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages && !isLoading
                ) {
                    Text("Next", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Individual Transaction Row in Ledger.
 */
@Composable
fun TransactionItemRow(
    transaction: TransactionDto,
    onClick: () -> Unit
) {
    val isDeduction = transaction.transactionType.contains("DEDUCTION")
    val isReversal = transaction.transactionType == "SYSTEM_ADJUSTMENT"
    val isReward = transaction.transactionType == "GAME_REWARD"

    val (badgeColor, typeLabel) = when (transaction.transactionType) {
        "ADMIN_TO_AGENT" -> Pair(AccentGold, "Admin → Agent")
        "AGENT_TO_USER" -> Pair(AgentBlue, "Agent → User")
        "ADMIN_DEDUCTION" -> Pair(DangerRed, "Admin Deduction")
        "AGENT_DEDUCTION" -> Pair(DangerRed, "Agent Deduction")
        "GAME_REWARD" -> Pair(SuccessGreen, "Game Reward")
        "GAME_DEDUCTION" -> Pair(DangerRed, "Game Deduction")
        "SYSTEM_ADJUSTMENT" -> Pair(Color(0xFFCE93D8), "Compensating Reversal")
        else -> Pair(TextMuted, transaction.transactionType)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_item_${transaction.transactionId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Pill
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDeduction) Icons.Default.ArrowDownward
                        else if (isReversal) Icons.Default.Undo
                        else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                    Text(
                        text = "${if (isDeduction) "-" else "+"}${formatCoins(transaction.amount)} Coins",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDeduction) DangerRed else SuccessGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = transaction.reason,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatLedgerDate(transaction.timestamp),
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (transaction.status == "COMPLETED") SuccessGreen.copy(alpha = 0.15f)
                        else if (transaction.status == "REVERSED") DangerRed.copy(alpha = 0.15f)
                        else TextMuted.copy(alpha = 0.15f),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = transaction.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.status == "COMPLETED") SuccessGreen
                            else if (transaction.status == "REVERSED") DangerRed
                            else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed Inspector Dialog for a Transaction.
 * Allows Admin to execute transaction reversal.
 */
@Composable
fun TransactionDetailDialog(
    transaction: TransactionDto,
    currentUserRole: String,
    onDismiss: () -> Unit,
    onReverseClick: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AccentGold)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Ledger Entry Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Transaction ID", transaction.transactionId)
                DetailRow("Idempotency Key", transaction.idempotencyKey)
                DetailRow("Type", transaction.transactionType)
                DetailRow("Status", transaction.status)
                DetailRow("Amount", "${formatCoins(transaction.amount)} Coins")
                DetailRow("Actor ID", transaction.actorId)
                DetailRow("Actor Role", transaction.actorRole)
                DetailRow("Source Wallet", transaction.sourceWalletId ?: "SYSTEM / REWARD")
                if (transaction.balanceBeforeSource != null) {
                    DetailRow("Source Balance", "${formatCoins(transaction.balanceBeforeSource ?: 0)} → ${formatCoins(transaction.balanceAfterSource ?: 0)}")
                }
                DetailRow("Destination Wallet", transaction.destinationWalletId ?: "N/A (DEDUCTION)")
                if (transaction.balanceBeforeDestination != null) {
                    DetailRow("Dest Balance", "${formatCoins(transaction.balanceBeforeDestination ?: 0)} → ${formatCoins(transaction.balanceAfterDestination ?: 0)}")
                }
                DetailRow("Timestamp", formatLedgerDate(transaction.timestamp))
                DetailRow("Reason", transaction.reason)
                if (transaction.referenceId != null) {
                    DetailRow("Reference ID", transaction.referenceId)
                }

                if (currentUserRole.uppercase() == "ADMIN" && transaction.status == "COMPLETED" && onReverseClick != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onReverseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reverse_tx_button")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reverse This Transaction", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentGold)
            }
        },
        containerColor = BgDark
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
        HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * Reversal Confirmation Dialog for Admin.
 */
@Composable
fun ReverseTransactionDialog(
    transactionId: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Confirm Transaction Reversal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You are reversing transaction:\n$transactionId",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "This will create an atomic compensating SYSTEM_ADJUSTMENT transaction and update the original status to REVERSED. Total balance conservation will be enforced.",
                    fontSize = 12.sp,
                    color = DangerRed
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        error = null
                    },
                    label = { Text("Reversal Reason (Mandatory)") },
                    placeholder = { Text("e.g., Wrong recipient allocation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(text = it, fontSize = 12.sp, color = DangerRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.trim().isEmpty()) {
                        error = "Reversal reason is mandatory."
                        return@Button
                    }
                    onConfirm(reason.trim())
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Confirm Reversal", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = BgDark
    )
}
